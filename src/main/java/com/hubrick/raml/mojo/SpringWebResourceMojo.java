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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.hubrick.raml.mojo.freemarker.ClassPathTemplateLoader;
import com.hubrick.raml.mojo.util.JavaNames;
import com.hubrick.raml.mojo.util.RamlTypes;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.raml.model.Action;
import org.raml.model.ParamType;
import org.raml.model.Raml;
import org.raml.parser.loader.FileResourceLoader;
import org.raml.parser.visitor.RamlDocumentBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.spliterator;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;

/**
 * Generates Spring Web resources out of RAML specifications.
 */
@Mojo(name = "spring-web", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SpringWebResourceMojo extends AbstractMojo {
    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "", required = true)
    private String basePackage;

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

        includedFiles.stream()
                .forEach(file -> {
                    getLog().info("Parsing " + file);
                    final RamlDocumentBuilder ramlDocumentBuilder = new RamlDocumentBuilder(new FileResourceLoader(fileset.getDirectory()));
                    final Raml raml = ramlDocumentBuilder.build(file);

                    final List<Action> actions = raml.getResources().entrySet().stream()
                            .map(resource -> resource.getValue().getActions().values())
                            .flatMap(actionList -> actionList.stream())
                            .collect(toList());

                    raml.getResources().entrySet().stream()
                            .forEach(entry -> {
                                final Configuration config = new Configuration();
                                config.setTemplateLoader(new ClassPathTemplateLoader(SpringWebResourceMojo.this.getClass().getClassLoader()));
                                config.setWhitespaceStripping(true);
                                config.setSharedVariable("actionMethodName", (TemplateMethodModelEx) arguments -> {
                                    checkArgument(arguments.size() == 1, "Exactly one argument expected");
                                    checkArgument(arguments.get(0) instanceof AdapterTemplateModel, "Argument model of type AdapterTemplateModel expected");
                                    final Action action = (Action) ((AdapterTemplateModel) arguments.get(0)).getAdaptedObject(Action.class);

                                    final List<String> nameElements =
                                            stream(spliterator(action.getResource().getUri().split("\\/")), false)
                                                    .filter(path -> !path.matches(".*\\{[^\\}]*\\}.*"))
                                                    .map(path -> stream(spliterator(path.split("(?i:[^a-z0-9])")), false)
                                                            .map(StringUtils::capitalize)
                                                            .collect(toList()))
                                                    .flatMap(tokens -> tokens.stream())
                                                    .collect(toList());

                                    return Joiner.on("").join(Iterables.concat(Collections.singleton(action.getType().name().toLowerCase()), nameElements));
                                });

                                config.setSharedVariable("javaType", (TemplateMethodModelEx) arguments -> {
                                    checkArgument(arguments.size() == 1, "Exactly one argument expected");
                                    checkArgument(arguments.get(0) instanceof AdapterTemplateModel, "Argument model of type AdapterTemplateModel expected");
                                    final ParamType paramType = (ParamType) ((AdapterTemplateModel) arguments.get(0)).getAdaptedObject(ParamType.class);
                                    return RamlTypes.asJavaTypeName(paramType);
                                });

                                final Template template;
                                try {
                                    template = config.getTemplate("templates/resource.ftl");
                                } catch (IOException e) {
                                    throw new RuntimeException("Error while acquiring template instance", e);
                                }

                                final File ramlFile = new File(fileset.getDirectory(), file);

                                final String packageName = concat(
                                        stream(spliterator(Strings.nullToEmpty(basePackage).split("\\.")), false),
                                        stream(spliterator(Strings.nullToEmpty(new File(file).getParent()).split(File.separator)), false)
                                )
                                        .filter(element -> !Strings.isNullOrEmpty(element))
                                        .collect(joining("."));

                                final File packageDirectory = new File(outputDirectory, packageName.replace('.', File.separatorChar));

                                int extPos = ramlFile.getName().lastIndexOf('.');
                                final String baseName = extPos < 0 ? ramlFile.getName() : ramlFile.getName().substring(0, extPos);
                                final String className = StringUtils.capitalize(JavaNames.toJavaName(baseName)) + "Resource";
                                final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                                        .put("package", packageName)
                                        .put("className", className)
                                        .put("actions", actions)
                                        .put("raml", raml)
                                        .build();

                                if (!packageDirectory.exists()) {
                                    checkState(packageDirectory.mkdirs(), "Couldn't create package directory: %s", packageDirectory.getAbsolutePath());
                                }

                                final FileWriter writer;
                                final File outputFile = new File(packageDirectory, className + ".java");
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
                                        getLog().error("Error while closing output stream", e);
                                    }
                                }
                            });
                });
    }

}
