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

import org.apache.maven.plugins.annotations.Parameter;
import org.jsonschema2pojo.AnnotationStyle;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class SchemaGeneratorConfig {

    @Parameter(property = "raml.schemaGen.generateBuilders", defaultValue = "false")
    private Boolean generateBuilders;

    @Parameter(property = "raml.schemaGen.usePrimitives", defaultValue = "false")
    private Boolean usePrimitives;

    @Parameter(property = "raml.schemaGen.propertyWordDelimiters")
    private char[] propertyWordDelimiters;

    @Parameter(property = "raml.schemaGen.useLongIntegers", defaultValue = "false")
    private Boolean useLongIntegers;

    @Parameter(property = "raml.schemaGen.useDoubleNumbers", defaultValue = "true")
    private Boolean useDoubleNumbers;

    @Parameter(property = "raml.schemaGen.includeHashcodeAndEquals", defaultValue = "true")
    private Boolean includeHashcodeAndEquals;

    @Parameter(property = "raml.schemaGen.includeToString", defaultValue = "true")
    private Boolean includeToString;

    @Parameter(property = "raml.schemaGen.annotationStyle", defaultValue = "JACKSON2")
    private AnnotationStyle annotationStyle;

    @Parameter(property = "raml.schemaGen.includeJsr303Annotations", defaultValue = "false")
    private Boolean includeJsr303Annotations;

    @Parameter(property = "raml.schemaGen.useJodaDates", defaultValue = "false")
    private Boolean useJodaDates;

    @Parameter(property = "raml.schemaGen.useJodaLocalDates", defaultValue = "false")
    private Boolean useJodaLocalDates;

    @Parameter(property = "raml.schemaGen.useJodaLocalTimes", defaultValue = "false")
    private Boolean useJodaLocalTimes;

    @Parameter(property = "raml.schemaGen.useCommonsLang3", defaultValue = "false")
    private Boolean useCommonsLang3;

    @Parameter(property = "raml.schemaGen.parcelable", defaultValue = "false")
    private Boolean parcelable;

    @Parameter(property = "raml.schemaGen.initializeCollections", defaultValue = "true")
    private Boolean initializeCollections;

    @Parameter(property = "raml.schemaGen.classNamePrefix", defaultValue = "")
    private String classNamePrefix;

    @Parameter(property = "raml.schemaGen.classNameSuffix", defaultValue = "")
    private String classNameSuffix;

    @Parameter(property = "raml.schemaGen.includeConstructors", defaultValue = "false")
    private Boolean includeConstructors;

    @Parameter(property = "raml.schemaGen.constructorsRequiredPropertiesOnly", defaultValue = "false")
    private Boolean constructorsRequiredPropertiesOnly;

    public Boolean getGenerateBuilders() {
        return generateBuilders;
    }

    public Boolean getUsePrimitives() {
        return usePrimitives;
    }

    public char[] getPropertyWordDelimiters() {
        return propertyWordDelimiters;
    }

    public Boolean getUseLongIntegers() {
        return useLongIntegers;
    }

    public Boolean getUseDoubleNumbers() {
        return useDoubleNumbers;
    }

    public Boolean getIncludeHashcodeAndEquals() {
        return includeHashcodeAndEquals;
    }

    public Boolean getIncludeToString() {
        return includeToString;
    }

    public AnnotationStyle getAnnotationStyle() {
        return annotationStyle;
    }

    public Boolean getIncludeJsr303Annotations() {
        return includeJsr303Annotations;
    }

    public Boolean getUseJodaDates() {
        return useJodaDates;
    }

    public Boolean getUseJodaLocalDates() {
        return useJodaLocalDates;
    }

    public Boolean getUseJodaLocalTimes() {
        return useJodaLocalTimes;
    }

    public Boolean getUseCommonsLang3() {
        return useCommonsLang3;
    }

    public Boolean getParcelable() {
        return parcelable;
    }

    public Boolean getInitializeCollections() {
        return initializeCollections;
    }

    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    public String getClassNameSuffix() {
        return classNameSuffix;
    }

    public Boolean getIncludeConstructors() {
        return includeConstructors;
    }

    public Boolean getConstructorsRequiredPropertiesOnly() {
        return constructorsRequiredPropertiesOnly;
    }
}