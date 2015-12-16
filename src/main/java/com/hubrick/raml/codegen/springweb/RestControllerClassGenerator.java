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
package com.hubrick.raml.codegen.springweb;

import com.hubrick.raml.codegen.ActionMetaInfo;
import com.hubrick.raml.codegen.InlineSchemaReference;
import com.hubrick.raml.codegen.SchemaMetaInfo;
import com.hubrick.raml.mojo.util.JavaNames;
import com.hubrick.raml.mojo.util.RamlUtil;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import org.raml.model.Raml;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class RestControllerClassGenerator {

    private JCodeModel codeModel;
    private String file;
    private Raml raml;
    private Map<InlineSchemaReference, SchemaMetaInfo> schemaIndex;
    private List<ActionMetaInfo> actionDefinitions;
    private String basePackage;

    private RestControllerClassGenerator(JCodeModel codeModel,
                                         String file,
                                         Raml raml,
                                         Map<InlineSchemaReference, SchemaMetaInfo> schemaIndex,
                                         List<ActionMetaInfo> actionDefinitions,
                                         String basePackage) {
        this.codeModel = codeModel;
        this.file = file;
        this.raml = raml;
        this.schemaIndex = schemaIndex;
        this.actionDefinitions = actionDefinitions;
        this.basePackage = basePackage;
    }

    public JType generate() {
        final String subPackageName = emptyToNull(nullToEmpty(new File(file).getParent()).replace(File.separatorChar, '.'));
        final String packageName = JavaNames.joinPackageMembers(basePackage, subPackageName);

        int extPos = file.lastIndexOf('.');
        final String baseName = extPos < 0 ? file : file.substring(0, extPos);
        final String resourceSimpleClassName = JavaNames.toSimpleClassName(baseName) + "Resource";

        final JPackage aPackage = codeModel._package(packageName);
        final JDefinedClass resourceClass;
        try {
            resourceClass = aPackage._interface(JMod.PUBLIC, resourceSimpleClassName);
        } catch (JClassAlreadyExistsException e) {
            throw new IllegalStateException("Resource interface already exists: " + e.getExistingClass().fullName(), e);
        }

        annotateControllerClass(resourceClass);

        // action methods
        for (ActionMetaInfo a : actionDefinitions) {
            final JType returnType;
            if (a.getResponseBodySchema() != null) {
                final InlineSchemaReference schemaRef = InlineSchemaReference.of(file, a.getResponseBodySchema());
                checkState(schemaIndex.containsKey(schemaRef), "Schema <%s> is not defined in the file: %s", schemaRef.getSchema(), file);

                final SchemaMetaInfo schemaMetaInfo = schemaIndex.get(schemaRef);
                returnType = schemaMetaInfo.getJType();
            } else {
                returnType = JType.parse(codeModel, "void");
            }

            final JMethod actionMethod = resourceClass.method(JMod.PUBLIC, returnType, RamlUtil.toJavaMethodName(a.getAction()));
            annotateActionMethod(actionMethod, a);

            // path parameters
            a.getUriParameterDefinitions().stream().forEach(p -> {
                final JVar param = actionMethod.param(JMod.FINAL, codeModel.ref(p.getJavaType()), p.getName());
                param.annotate(codeModel.ref("org.springframework.web.bind.annotation.PathVariable"))
                        .param("value", p.getName());
            });

            // query parameters
            a.getQueryParameterDefinitions().stream().forEach(p -> {
                final JVar param = actionMethod.param(JMod.FINAL, codeModel.ref(p.getJavaType()), p.getName());
                param.annotate(codeModel.ref("org.springframework.web.bind.annotation.RequestParam"))
                        .param("value", p.getName());
            });

            // request body
            if (a.getRequestBodySchema() != null) {
                final InlineSchemaReference schemaRef = InlineSchemaReference.of(file, a.getRequestBodySchema());
                checkState(schemaIndex.containsKey(schemaRef), "Schema <%s> is not defined in the file: %s", schemaRef.getSchema(), file);

                final SchemaMetaInfo schemaMetaInfo = schemaIndex.get(schemaRef);
                actionMethod.param(JMod.FINAL, schemaMetaInfo.getJType(), "body")
                        .annotate(codeModel.ref("org.springframework.web.bind.annotation.RequestBody"));
            }

        }

        return resourceClass;
    }

    private void annotateControllerClass(JDefinedClass resourceClass) {
        resourceClass.annotate(codeModel.ref("org.springframework.web.bind.annotation.RestController"));
        if (raml.getBaseUri() != null) {
            resourceClass.annotate(codeModel.ref("org.springframework.web.bind.annotation.RequestMapping")).param("value", raml.getBaseUri());
        }
    }

    private void annotateActionMethod(JMethod actionMethod, ActionMetaInfo actionMetaInfo) {
        final JAnnotationUse requestMapping = actionMethod.annotate(codeModel.ref("org.springframework.web.bind.annotation.RequestMapping"));
        requestMapping.param("method", codeModel.ref("org.springframework.web.bind.annotation.RequestMethod").staticRef(actionMetaInfo.getAction().getType().name()));
        final String uri = actionMetaInfo.getAction().getResource().getUri();
        if (uri != null) {
            requestMapping.param("value", uri);
        }
    }

    public static RestControllerClassGeneratorBuilder builder() {
        return new RestControllerClassGeneratorBuilder();
    }

    public static class RestControllerClassGeneratorBuilder {

        private JCodeModel codeModel;
        private String file;
        private Raml raml;
        private Map<InlineSchemaReference, SchemaMetaInfo> schemaIndex;
        private List<ActionMetaInfo> actionDefinitions;
        private String basePackage;

        public RestControllerClassGeneratorBuilder setCodeModel(JCodeModel codeModel) {
            this.codeModel = codeModel;
            return this;
        }

        public RestControllerClassGeneratorBuilder setFile(String file) {
            this.file = file;
            return this;
        }

        public RestControllerClassGeneratorBuilder setRaml(Raml raml) {
            this.raml = raml;
            return this;
        }

        public RestControllerClassGeneratorBuilder setSchemaIndex(Map<InlineSchemaReference, SchemaMetaInfo> schemaIndex) {
            this.schemaIndex = schemaIndex;
            return this;
        }

        public RestControllerClassGeneratorBuilder setActionDefinitions(List<ActionMetaInfo> actionDefinitions) {
            this.actionDefinitions = actionDefinitions;
            return this;
        }

        public RestControllerClassGeneratorBuilder setBasePackage(String basePackage) {
            this.basePackage = basePackage;
            return this;
        }

        public RestControllerClassGenerator build() {
            return new RestControllerClassGenerator(codeModel, file, raml, schemaIndex, actionDefinitions, basePackage);
        }
    }
}
