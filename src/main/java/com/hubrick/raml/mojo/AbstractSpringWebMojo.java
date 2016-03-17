/*
 * Copyright 2015 Hubrick
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubrick.raml.mojo;

import com.google.common.collect.ImmutableMap;
import com.hubrick.raml.codegen.ActionMetaInfo;
import com.hubrick.raml.codegen.HeaderDefinition;
import com.hubrick.raml.codegen.InlinePatternDefinition;
import com.hubrick.raml.codegen.InlineSchemaReference;
import com.hubrick.raml.codegen.PatternDefinition;
import com.hubrick.raml.codegen.QueryParameterDefinition;
import com.hubrick.raml.codegen.ReferencedPatternDefinition;
import com.hubrick.raml.codegen.SchemaMetaInfo;
import com.hubrick.raml.codegen.UriParameterDefinition;
import com.hubrick.raml.codegen.springweb.RestControllerClassGenerator;
import com.hubrick.raml.mojo.antlr.ExtensionTags;
import com.hubrick.raml.mojo.util.JavaNames;
import com.hubrick.raml.mojo.util.RamlTypes;
import com.hubrick.raml.util.ResourceList;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.jsonschema2pojo.AnnotationStyle;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GsonAnnotator;
import org.jsonschema2pojo.Jackson1Annotator;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.NoopAnnotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.rules.RuleFactory;
import org.raml.model.Action;
import org.raml.model.MimeType;
import org.raml.model.Raml;
import org.raml.parser.loader.FileResourceLoader;
import org.raml.parser.visitor.RamlDocumentBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.nullToEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author ahanin
 * @since 1.0.0
 */
public abstract class AbstractSpringWebMojo extends AbstractMojo {

    public static final String APPLICATION_JSON = "application/json";

    private static final Map<AnnotationStyle, Supplier<Annotator>> ANNOTATOR_SUPPLIER_INDEX = ImmutableMap.of(
            AnnotationStyle.JACKSON, Jackson2Annotator::new,
            AnnotationStyle.JACKSON2, Jackson2Annotator::new,
            AnnotationStyle.JACKSON1, Jackson1Annotator::new,
            AnnotationStyle.GSON, GsonAnnotator::new,
            AnnotationStyle.NONE, NoopAnnotator::new
    );

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject mavenProject;

    @Parameter(property = "raml.basePackage", defaultValue = "", required = true)
    protected String basePackage;

    @Parameter(property = "raml.modelPackage", defaultValue = "model", required = true)
    protected String modelPackage;

    @Parameter(property = "raml.schemaGenerator", defaultValue = "${raml.schemaGenerator}", required = true)
    protected SchemaGeneratorConfig schemaGenerator;

    /**
     * RAML files to include in execution.
     */
    @Parameter(required = true)
    private FileSet fileset;

    private static Annotator requireAnnotator(AnnotationStyle annotationStyle) {
        checkState(ANNOTATOR_SUPPLIER_INDEX.containsKey(annotationStyle), "Illegal annotation style: %s", annotationStyle);
        return ANNOTATOR_SUPPLIER_INDEX.get(annotationStyle).get();
    }

    @Override
    public abstract void execute() throws MojoExecutionException, MojoFailureException;

    private static String parseJavaType(String description) {
        final ExtensionTags extensionTags = new ExtensionTags(new ANTLRStringStream(description));
        Token token;
        do {
            token = extensionTags.nextToken();
        } while (token.getType() != ExtensionTags.EOF &&
                token.getType() != ExtensionTags.JAVA_TYPE_TAG);

        if (token.getType() == ExtensionTags.JAVA_TYPE_TAG) {
            final String[] chunks = token.getText().split("\\s+");
            return chunks[1];
        }

        return null;
    }

    protected JCodeModel generateCodeModel() {
        final FileSetManager fileSetManager = new FileSetManager();
        final List<String> includedFiles = Arrays.asList(fileSetManager.getIncludedFiles(fileset));
        final List<String> excludedFiles = Arrays.asList(fileSetManager.getExcludedFiles(fileset));

        includedFiles.removeAll(excludedFiles);

        // parse RAML files
        final Map<String, Raml> ramlFileIndex = includedFiles.stream()
                .collect(toMap(file -> file, file -> {
                    getLog().info("Parsing " + file);
                    final RamlDocumentBuilder ramlDocumentBuilder = new RamlDocumentBuilder(new FileResourceLoader(fileset.getDirectory()));
                    return ramlDocumentBuilder.build(file);
                }));

        // generate model classes out of schemas
        getLog().info("Generating model classes");
        final Map<InlineSchemaReference, SchemaMetaInfo> schemaIndex = ramlFileIndex.entrySet().stream()
                .flatMap(ramlEntry -> ramlEntry.getValue().getSchemas().stream()
                        .flatMap(i -> i.entrySet().stream()
                                .map(schemaEntry -> new SchemaMetaInfo(schemaEntry.getKey(),
                                        schemaEntry.getValue(),
                                        JavaNames.joinPackageMembers(basePackage, modelPackage),
                                        JavaNames.toSimpleClassName(schemaEntry.getKey()),
                                        ramlEntry.getKey()))))
                .collect(toMap(schemaMetaInfo -> InlineSchemaReference.of(schemaMetaInfo.getRamlFile(), schemaMetaInfo.getName()),
                        schemaMetaInfo -> schemaMetaInfo));

        final JCodeModel codeModel = new JCodeModel();

        schemaIndex.values().forEach(schemaMetaInfo -> {
            try {
                final RuleFactory ruleFactory = new RuleFactory();
                final GenerationConfigAdapter generationConfig = new GenerationConfigAdapter(schemaGenerator);
                ruleFactory.setGenerationConfig(generationConfig);
                ruleFactory.setAnnotator(requireAnnotator(generationConfig.getAnnotationStyle()));

                final SchemaGenerator schemaGenerator = new SchemaGenerator();

                final SchemaMapper schemaMapper = new SchemaMapper(ruleFactory, schemaGenerator);

                final JType jType = schemaMapper.generate(codeModel,
                        schemaMetaInfo.getClassName(),
                        schemaMetaInfo.getPackageName(),
                        schemaMetaInfo.getSchema(),
                        new File(fileset.getDirectory(), schemaMetaInfo.getRamlFile()).toURI());
                schemaMetaInfo.setJType(jType);

                if (getLog().isInfoEnabled()) {
                    getLog().info("Completed " + jType.fullName());
                }
            } catch (IOException e) {
                if (getLog().isErrorEnabled()) {
                    getLog().info("Error when generating " + JavaNames.joinPackageMembers(schemaMetaInfo.getPackageName(), schemaMetaInfo.getClassName()));
                }
                throw new RuntimeException("Error while generating model class: " + schemaMetaInfo.getPackageName() + "." + schemaMetaInfo.getClassName(), e);
            }
        });

        if (!ramlFileIndex.isEmpty()) {
            getLog().info("Generating controller classes");

            // generate resources
            ramlFileIndex.entrySet().stream()
                    .forEach(ramlEntry -> {
                        final String file = ramlEntry.getKey();
                        final Raml raml = ramlEntry.getValue();

                        // build action index
                        final List<ActionMetaInfo> actions = ResourceList.of(raml).flatten()
                                .map(resource -> resource.getActions().values())
                                .flatMap(actionList -> actionList.stream())
                                .map(action -> createActionMetaInfo(action))
                                .collect(toList());

                        final JType jType = RestControllerClassGenerator.builder()
                                .setCodeModel(codeModel)
                                .setFile(file)
                                .setRaml(raml)
                                .setSchemaIndex(schemaIndex)
                                .setActionDefinitions(actions)
                                .setBasePackage(basePackage)
                                .build()
                                .generate();

                        if (getLog().isInfoEnabled()) {
                            getLog().info("Completed " + jType.fullName());
                        }
                    });
        }
        return codeModel;
    }

    private ActionMetaInfo createActionMetaInfo(Action action) {
        final Optional<Map<String, MimeType>> requestBody = Optional.ofNullable(action.getBody());

        final Optional<Map<String, MimeType>> responseMap = action.getResponses().entrySet().stream()
                .filter(e -> e.getKey().startsWith("2"))
                .map(e -> e.getValue().getBody())
                .findFirst();

        final Collection<UriParameterDefinition> uriParameterDefinitions = action.getResource().getResolvedUriParameters().entrySet().stream()
                .map(paramEntry ->
                        new UriParameterDefinition(
                                paramEntry.getKey(),
                                firstNonNull(parseJavaType(nullToEmpty(paramEntry.getValue().getDescription())), RamlTypes.asJavaTypeName(paramEntry.getValue().getType())),
                                paramEntry.getValue().getPattern() != null ? parsePatternDefinition(paramEntry.getValue().getPattern()) : null,
                                paramEntry.getValue()))
                .collect(toList());

        final Collection<QueryParameterDefinition> queryParameterDefinitions = action.getQueryParameters().entrySet().stream()
                .map(paramEntry ->
                        new QueryParameterDefinition(paramEntry.getKey(),
                                firstNonNull(parseJavaType(nullToEmpty(paramEntry.getValue().getDescription())), RamlTypes.asJavaTypeName(paramEntry.getValue().getType())),
                                paramEntry.getValue().getPattern() != null ? parsePatternDefinition(paramEntry.getValue().getPattern()) : null,
                                paramEntry.getValue()))
                .collect(toList());

        final Collection<HeaderDefinition> headerDefinitions = action.getHeaders().entrySet().stream()
                .map(paramEntry ->
                        new HeaderDefinition(paramEntry.getKey(),
                                firstNonNull(parseJavaType(nullToEmpty(paramEntry.getValue().getDescription())), RamlTypes.asJavaTypeName(paramEntry.getValue().getType())),
                                paramEntry.getValue().getPattern() != null ? parsePatternDefinition(paramEntry.getValue().getPattern()) : null,
                                paramEntry.getValue()))
                .collect(toList());

        final ActionMetaInfo actionMetaInfo = new ActionMetaInfo();
        actionMetaInfo.setAction(action);
        actionMetaInfo.setRequestBodySchema(requestBody.isPresent() ? getBodySchema(requestBody.get(), APPLICATION_JSON) : null);
        actionMetaInfo.setResponseBodySchema(responseMap.isPresent() ? getBodySchema(responseMap.get(), APPLICATION_JSON) : null);
        actionMetaInfo.setUriParameterDefinitions(uriParameterDefinitions);
        actionMetaInfo.setQueryParameterDefinitions(queryParameterDefinitions);
        actionMetaInfo.setHeaderDefinitions(headerDefinitions);
        // TODO support form parameters

        return actionMetaInfo;
    }

    private PatternDefinition parsePatternDefinition(String pattern) {
        final ExtensionTags extensionTags = new ExtensionTags(new ANTLRStringStream(pattern));
        Token token;
        do {
            token = extensionTags.nextToken();
        } while (token.getType() != ExtensionTags.EOF &&
                token.getType() != ExtensionTags.REF_TAG);

        if (token.getType() == ExtensionTags.REF_TAG) {
            final String[] chunks = token.getText().split("\\s+");
            return new ReferencedPatternDefinition(chunks[1]);
        }

        return new InlinePatternDefinition(pattern);
    }

    private String getBodySchema(Map<String, MimeType> requestBody, String contentType) {
        if (requestBody == null) {
            return null;
        }

        final String[] split = contentType.split("/", 2);
        if (split.length != 2) {
            return null;
        }

        final String extensionReplacementRegexp = String.format("(?<=%s/)(.*\\+)(?=%s)",
                Pattern.quote(split[0]),
                Pattern.quote(split[1]));

        return requestBody.entrySet().stream()
                .filter(e -> e.getKey().replaceAll(extensionReplacementRegexp, "").equals(contentType))
                .map(e -> e.getValue())
                .map(e -> e.getSchema())
                .findFirst().orElse(null);
    }

    private static class GenerationConfigAdapter extends DefaultGenerationConfig {

        private SchemaGeneratorConfig config;

        public GenerationConfigAdapter(SchemaGeneratorConfig config) {
            this.config = config;
        }

        @Override
        public boolean isGenerateBuilders() {
            return firstNonNull(config.getGenerateBuilders(), super.isGenerateBuilders());
        }

        @Override
        public boolean isIncludeToString() {
            return firstNonNull(config.getIncludeToString(), super.isIncludeToString());
        }

        @Override
        public char[] getPropertyWordDelimiters() {
            return super.getPropertyWordDelimiters();
        }

        @Override
        public boolean isUseLongIntegers() {
            return firstNonNull(config.getUseLongIntegers(), super.isUseLongIntegers());
        }

        @Override
        public boolean isUseDoubleNumbers() {
            return firstNonNull(config.getUseDoubleNumbers(), super.isUseDoubleNumbers());
        }

        @Override
        public boolean isIncludeHashcodeAndEquals() {
            return firstNonNull(config.getIncludeHashcodeAndEquals(), super.isIncludeHashcodeAndEquals());
        }

        @Override
        public AnnotationStyle getAnnotationStyle() {
            return firstNonNull(config.getAnnotationStyle(), super.getAnnotationStyle());
        }

        @Override
        public boolean isIncludeJsr303Annotations() {
            return firstNonNull(config.getIncludeJsr303Annotations(), super.isIncludeJsr303Annotations());
        }

        @Override
        public boolean isUseJodaDates() {
            return firstNonNull(config.getUseJodaDates(), super.isUseJodaDates());
        }

        @Override
        public boolean isUseJodaLocalDates() {
            return firstNonNull(config.getUseJodaLocalDates(), super.isUseJodaLocalDates());
        }

        @Override
        public boolean isUseJodaLocalTimes() {
            return firstNonNull(config.getUseJodaLocalTimes(), super.isUseJodaLocalTimes());
        }

        @Override
        public boolean isUseCommonsLang3() {
            return firstNonNull(config.getUseCommonsLang3(), super.isUseCommonsLang3());
        }

        @Override
        public boolean isParcelable() {
            return firstNonNull(config.getParcelable(), super.isParcelable());
        }

        @Override
        public boolean isInitializeCollections() {
            return firstNonNull(config.getInitializeCollections(), super.isInitializeCollections());
        }

        @Override
        public String getClassNamePrefix() {
            return firstNonNull(config.getClassNamePrefix(), super.getClassNamePrefix());
        }

        @Override
        public String getClassNameSuffix() {
            return firstNonNull(config.getClassNameSuffix(), super.getClassNameSuffix());
        }

        @Override
        public boolean isIncludeConstructors() {
            return firstNonNull(config.getIncludeConstructors(), super.isIncludeConstructors());
        }

        @Override
        public boolean isConstructorsRequiredPropertiesOnly() {
            return firstNonNull(config.getConstructorsRequiredPropertiesOnly(), super.isConstructorsRequiredPropertiesOnly());
        }
    }
}
