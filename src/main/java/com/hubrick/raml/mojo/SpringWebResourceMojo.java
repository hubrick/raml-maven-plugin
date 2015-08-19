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
import com.hubrick.raml.codegen.InlineSchemaReference;
import com.hubrick.raml.codegen.QueryParameterDefinition;
import com.hubrick.raml.codegen.SchemaMetaInfo;
import com.hubrick.raml.codegen.UriParameterDefinition;
import com.hubrick.raml.mojo.freemarker.ClassPathTemplateLoader;
import com.hubrick.raml.mojo.util.JavaNames;
import com.hubrick.raml.mojo.util.RamlTypes;
import com.hubrick.raml.mojo.util.RamlUtil;
import com.hubrick.raml.util.ResourceList;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;
import org.raml.model.Action;
import org.raml.model.MimeType;
import org.raml.model.ParamType;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.parser.loader.FileResourceLoader;
import org.raml.parser.visitor.RamlDocumentBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Generates Spring Web resources out of RAML specifications.
 */
@Mojo(name = "spring-web", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SpringWebResourceMojo extends AbstractMojo {

    public static final String APPLICATION_JSON = "application/json";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/raml", required = true)
    private File outputDirectory;

    @Parameter(property = "raml.basePackage", defaultValue = "", required = true)
    private String basePackage;

    @Parameter(property = "raml.modelPackage", defaultValue = "", required = true)
    private String modelPackage;

    /**
     * RAML files to in
     */
    @Parameter
    private FileSet fileset;

    public void setFileset(FileSet fileset) {
        this.fileset = fileset;
    }

    public void execute() throws MojoExecutionException {
        final FileSetManager fileSetManager = new FileSetManager();
        final List<String> includedFiles = Arrays.asList(fileSetManager.getIncludedFiles(fileset));
        final List<String> excludedFiles = Arrays.asList(fileSetManager.getExcludedFiles(fileset));

        includedFiles.removeAll(excludedFiles);

        checkState(outputDirectory.mkdirs(), "Unable to create output directory: %s", outputDirectory.getPath());

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

        schemaIndex.values().forEach(schemaMetaInfo -> {
            if (getLog().isInfoEnabled()) {
                getLog().info("Generating: " + JavaNames.joinPackageMembers(schemaMetaInfo.getPackageName(), schemaMetaInfo.getClassName()));
            }
            final JCodeModel jCodeModel = new JCodeModel();
            try {
                final RuleFactory ruleFactory = new RuleFactory(new DefaultGenerationConfig() {
                    @Override
                    public boolean isIncludeToString() {
                        return false;
                    }

                    @Override
                    public boolean isIncludeHashcodeAndEquals() {
                        return false;
                    }
                }, new Jackson2Annotator(), new SchemaStore());

                final SchemaGenerator schemaGenerator = new SchemaGenerator();

                final SchemaMapper schemaMapper = new SchemaMapper(ruleFactory, schemaGenerator);

                final JType jType = schemaMapper.generate(jCodeModel,
                        schemaMetaInfo.getClassName(),
                        schemaMetaInfo.getPackageName(),
                        schemaMetaInfo.getSchema(),
                        URI.create("file://" + new File(fileset.getDirectory(), schemaMetaInfo.getRamlFile())));
                schemaMetaInfo.setJType(jType);
            } catch (IOException e) {
                throw new RuntimeException("Error while generating model class: " + schemaMetaInfo.getPackageName() + "." + schemaMetaInfo.getClassName(), e);
            }
            try {
                jCodeModel.build(outputDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // generate resources
        ramlFileIndex.entrySet().stream()
                .forEach(ramlEntry -> {
                    final String file = ramlEntry.getKey();
                    final Raml raml = ramlEntry.getValue();

                    final List<Resource> resources = ResourceList.of(raml).flatten().collect(toList());

                    // build action index
                    final List<ActionMetaInfo> actions = resources.stream()
                            .map(resource -> resource.getActions().values())
                            .flatMap(actionList -> actionList.stream())
                            .map(action -> createActionMetaInfo(action))
                            .collect(toList());

                    // build imports
                    final Collection<String> imports = Stream.of(
                            actions.stream()
                                    .flatMap(actionMetaInfo -> {
                                        checkState(actionMetaInfo.getRequestBodySchema() == null || schemaIndex.containsKey(InlineSchemaReference.of(file, actionMetaInfo.getRequestBodySchema())),
                                                "Schema doesn't exist: %s", actionMetaInfo.getRequestBodySchema());
                                        checkState(actionMetaInfo.getResponseBodySchema() == null || schemaIndex.containsKey(InlineSchemaReference.of(file, actionMetaInfo.getResponseBodySchema())),
                                                "Schema doesn't exist: %s", actionMetaInfo.getRequestBodySchema());

                                        final List<String> classNames = new ArrayList<>();
                                        if (actionMetaInfo.getRequestBodySchema() != null) {
                                            classNames.add(schemaIndex.get(InlineSchemaReference.of(file, actionMetaInfo.getRequestBodySchema())).getFullyQualifiedClassName());
                                        }

                                        if (actionMetaInfo.getResponseBodySchema() != null) {
                                            classNames.add(schemaIndex.get(InlineSchemaReference.of(file, actionMetaInfo.getResponseBodySchema())).getFullyQualifiedClassName());
                                        }

                                        final List<String> parameterTypes = Stream.of(actionMetaInfo.getUriParameterDefinitions(), actionMetaInfo.getQueryParameterDefinitions(), actionMetaInfo.getHeaderDefinitions())
                                                .flatMap(list -> list.stream())
                                                .map(paramDef -> paramDef.getJavaType())
                                                .collect(toList());

                                        classNames.addAll(parameterTypes);

                                        return classNames.stream();
                                    }),
                            Stream.of(
                                    actions.stream().filter(a -> a.getRequestBodySchema() != null).findAny().isPresent() ? "org.springframework.web.bind.annotation.RequestBody" : null,
                                    actions.stream().filter(a -> !a.getUriParameterDefinitions().isEmpty()).findAny().isPresent() ? "org.springframework.web.bind.annotation.PathVariable" : null,
                                    actions.stream().filter(a -> !a.getQueryParameterDefinitions().isEmpty()).findAny().isPresent() ? "org.springframework.web.bind.annotation.RequestParam" : null,
                                    actions.stream().filter(a -> !a.getHeaderDefinitions().isEmpty()).findAny().isPresent() ? "org.springframework.web.bind.annotation.RequestHeader" : null),
                            Stream.of("org.springframework.web.bind.annotation.RequestMapping", "org.springframework.web.bind.annotation.RestController", "org.springframework.web.bind.annotation.RequestMethod"))
                            .flatMap(c -> c)
                            .filter(c -> c != null)
                            .sorted()
                            .distinct()
                            .collect(toList());

                    resources.stream().forEach(generateResourceClass(file, raml, imports, schemaIndex, actions));
                });

    }

    private ActionMetaInfo createActionMetaInfo(Action action) {
        final Map<String, MimeType> requestBody = action.getBody();

        final Optional<Map<String, MimeType>> responseMap = action.getResponses().entrySet().stream()
                .filter(e -> e.getKey().startsWith("2"))
                .map(e -> e.getValue().getBody())
                .findFirst();

        final Collection<UriParameterDefinition> uriParameterDefinitions = action.getResource().getResolvedUriParameters().entrySet().stream()
                .map(paramEntry -> new UriParameterDefinition(paramEntry.getKey(),
                        RamlTypes.asJavaTypeName(paramEntry.getValue().getType()),
                        paramEntry.getValue()))
                .collect(toList());

        final Collection<QueryParameterDefinition> queryParameterDefinitions = action.getQueryParameters().entrySet().stream()
                .map(paramEntry -> new QueryParameterDefinition(paramEntry.getKey(),
                        RamlTypes.asJavaTypeName(paramEntry.getValue().getType()),
                        paramEntry.getValue()))
                .collect(toList());

        final Collection<HeaderDefinition> headerDefinitions = action.getHeaders().entrySet().stream()
                .map(paramEntry -> new HeaderDefinition(paramEntry.getKey(),
                        RamlTypes.asJavaTypeName(paramEntry.getValue().getType()),
                        paramEntry.getValue()))
                .collect(toList());

        final ActionMetaInfo actionMetaInfo = new ActionMetaInfo();
        actionMetaInfo.setAction(action);
        actionMetaInfo.setRequestBodySchema(getBodySchema(requestBody, APPLICATION_JSON));
        actionMetaInfo.setResponseBodySchema(responseMap.isPresent() ? getBodySchema(responseMap.get(), APPLICATION_JSON) : null);
        actionMetaInfo.setUriParameterDefinitions(uriParameterDefinitions);
        actionMetaInfo.setQueryParameterDefinitions(queryParameterDefinitions);
        actionMetaInfo.setHeaderDefinitions(headerDefinitions);
        // TODO support form parameters

        return actionMetaInfo;
    }

    private String getBodySchema(Map<String, MimeType> requestBody, String contentType) {
        return requestBody != null && requestBody.containsKey(contentType) ? requestBody.get(APPLICATION_JSON).getSchema() : null;
    }

    private Consumer<Resource> generateResourceClass(final String file, final Raml raml, Collection<String> imports, Map<InlineSchemaReference, SchemaMetaInfo> schemaIndex, final List<ActionMetaInfo> actionDefinitions) {
        return entry -> {
            final File ramlFile = new File(fileset.getDirectory(), file);
            final String subPackageName = emptyToNull(nullToEmpty(new File(file).getParent()).replace(File.separatorChar, '.'));
            final String packageName = JavaNames.joinPackageMembers(basePackage, subPackageName);
            final File packageDirectory = new File(outputDirectory, asPackageDirectory(packageName));

            int extPos = ramlFile.getName().lastIndexOf('.');
            final String baseName = extPos < 0 ? ramlFile.getName() : ramlFile.getName().substring(0, extPos);
            final String resourceSimpleClassName = JavaNames.toSimpleClassName(baseName) + "Resource";
            final String resourceClassName = JavaNames.joinPackageMembers(packageName, resourceSimpleClassName);

            final LinkedHashMap<String, String> classReferenceIndex = new LinkedHashMap<>();
            Stream.concat(
                    imports.stream(),
                    Stream.of(resourceClassName))
                    .forEach(className -> {
                        final String simpleClassName = JavaNames.getSimpleClassName(className);
                        classReferenceIndex.put(className, classReferenceIndex.containsValue(simpleClassName) ? className : simpleClassName);
                    });

            final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                    .put("package", packageName)
                    .put("resourceClassName", resourceClassName)
                    .put("actionDefinitions", actionDefinitions)
                    .put("imports", classReferenceIndex.entrySet().stream()
                            .filter(e -> !e.getKey().equals(e.getValue()))
                            .map(e -> e.getKey())
                            .filter(e -> !e.startsWith("java.lang.") && !e.equals(resourceClassName))
                            .collect(toList()))
                    .put("raml", raml)
                    .build();

            final Configuration config = new Configuration();
            config.setTemplateLoader(new ClassPathTemplateLoader(SpringWebResourceMojo.this.getClass().getClassLoader()));
            config.setWhitespaceStripping(true);
            config.setSharedVariable("actionMethodName", new ActionMethodNameFunction());
            config.setSharedVariable("javaType", new JavaTypeFunction());
            config.setSharedVariable("javaName", new JavaNameFunction());
            config.setSharedVariable("classRef", new ClassReferenceFunction(classReferenceIndex));
            config.setSharedVariable("schemaClassRef", new SchemaClassReferenceFunction(file, schemaIndex, classReferenceIndex));

            final Template template;
            try {
                template = config.getTemplate("templates/resource.ftl");
            } catch (IOException e) {
                throw new RuntimeException("Error while acquiring template instance", e);
            }

            if (!packageDirectory.exists()) {
                checkState(packageDirectory.mkdirs(), "Couldn't create package directory: %s", packageDirectory.getAbsolutePath());
            }

            final FileWriter writer;
            final File outputFile = new File(packageDirectory, resourceSimpleClassName + ".java");
            try {
                writer = new FileWriter(outputFile);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create file writer: " + outputFile);
            }

            try {
                template.process(model, writer);
            } catch (TemplateException | IOException e) {
                throw new RuntimeException("Error while generating resource interface", e);
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    SpringWebResourceMojo.this.getLog().error("Error while closing output stream", e);
                }
            }
        };
    }

    private static String asPackageDirectory(String packageName) {
        return packageName.replace('.', File.separatorChar);
    }

    private static class ActionMethodNameFunction implements TemplateMethodModelEx {
        @Override
        public Object exec(List arguments) throws TemplateModelException {
            checkArgument(arguments.size() == 1, "Exactly one argument expected");
            checkArgument(arguments.get(0) instanceof AdapterTemplateModel, "Argument model of type AdapterTemplateModel expected");
            final Action action = (Action) ((AdapterTemplateModel) arguments.get(0)).getAdaptedObject(Action.class);
            return RamlUtil.toJavaMethodName(action);
        }
    }

    private static class JavaTypeFunction implements TemplateMethodModelEx {
        @Override
        public Object exec(List arguments) throws TemplateModelException {
            checkArgument(arguments.size() == 1, "Exactly one argument expected");
            checkArgument(arguments.get(0) instanceof AdapterTemplateModel, "Argument model of type AdapterTemplateModel expected");
            final ParamType paramType = (ParamType) ((AdapterTemplateModel) arguments.get(0)).getAdaptedObject(ParamType.class);
            return RamlTypes.asJavaTypeName(paramType);
        }
    }

    private static class JavaNameFunction implements TemplateMethodModelEx {
        @Override
        public Object exec(List arguments) throws TemplateModelException {
            checkArgument(arguments.size() == 1, "Exactly one argument expected");
            return JavaNames.toJavaName(getModelObject((TemplateModel) arguments.get(0), String.class));
        }
    }

    private static class ClassReferenceFunction implements TemplateMethodModelEx {

        private final Map<String, String> classReferenceIndex;

        public ClassReferenceFunction(Map<String, String> classReferenceIndex) {
            this.classReferenceIndex = classReferenceIndex;
        }

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            checkArgument(arguments.size() == 1, "Exactly one argument expected");
            final String fullyQualifiedClassReference = getModelObject((TemplateModel) arguments.get(0), String.class);
            checkState(classReferenceIndex.containsKey(fullyQualifiedClassReference), "Class reference not found: %s", fullyQualifiedClassReference);
            return classReferenceIndex.get(fullyQualifiedClassReference);
        }

    }

    private static class SchemaClassReferenceFunction implements TemplateMethodModelEx {

        private final String ramlFile;
        private final Map<InlineSchemaReference, SchemaMetaInfo> schemaIndex;
        private final Map<String, String> classRefIndex;

        public SchemaClassReferenceFunction(String ramlFile, Map<InlineSchemaReference, SchemaMetaInfo> schemaIndex, Map<String, String> classRefIndex) {
            this.ramlFile = ramlFile;
            this.schemaIndex = schemaIndex;
            this.classRefIndex = classRefIndex;
        }

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            checkArgument(arguments.size() == 1, "Exactly one argument expected");
            final String schemaName = getModelObject((TemplateModel) arguments.get(0), String.class);

            final InlineSchemaReference schemaReference = InlineSchemaReference.of(ramlFile, schemaName);
            checkState(schemaIndex.containsKey(schemaReference), "Schema <%s> is not defined in the file: %s", schemaName, ramlFile);

            final SchemaMetaInfo schemaMetaInfo = schemaIndex.get(schemaReference);
            checkState(classRefIndex.containsKey(schemaMetaInfo.getFullyQualifiedClassName()), "Class reference not found: %s", schemaName);

            return classRefIndex.get(schemaMetaInfo.getFullyQualifiedClassName());
        }

    }

    private static <T> T getModelObject(TemplateModel model, Class<T> klass) {
        if (model instanceof SimpleScalar) {
            checkState(String.class.isAssignableFrom(klass), "%s is not supported by %s model", klass, SimpleScalar.class.getSimpleName());
            return (T) ((SimpleScalar) model).getAsString();
        } else if (model instanceof AdapterTemplateModel) {
            return (T) ((AdapterTemplateModel) model).getAdaptedObject(klass);
        } else {
            throw new IllegalStateException("Unsupported argument model");
        }
    }

}
