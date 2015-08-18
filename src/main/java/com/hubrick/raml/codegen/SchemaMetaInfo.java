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
package com.hubrick.raml.codegen;

import com.hubrick.raml.mojo.util.JavaNames;
import com.sun.codemodel.JType;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class SchemaMetaInfo {

    private String name;
    private String schema;
    private String packageName;
    private String className;
    private String ramlFile;
    private JType jType;

    public SchemaMetaInfo(String name, String schema, String packageName, String className, String ramlFile) {
        this.name = name;
        this.schema = schema;
        this.packageName = packageName;
        this.className = className;
        this.ramlFile = ramlFile;
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schema;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getRamlFile() {
        return ramlFile;
    }

    public String getFullyQualifiedClassName() {
        return JavaNames.joinPackageMembers(packageName, className);
    }

    public void setJType(JType jType) {
        this.jType = jType;
    }

    public JType getJType() {
        return jType;
    }

}
