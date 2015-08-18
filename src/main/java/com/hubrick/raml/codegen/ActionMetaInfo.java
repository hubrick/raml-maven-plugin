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

import org.raml.model.Action;

import java.util.Collection;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class ActionMetaInfo {

    private Action action;
    private String requestBodySchema;
    private String responseBodySchema;
    private Collection<UriParameterDefinition> uriParameterDefinitions;
    private Collection<QueryParameterDefinition> queryParameterDefinitions;
    private Collection<HeaderDefinition> headerDefinitions;

    public void setAction(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public void setRequestBodySchema(String requestBodySchema) {
        this.requestBodySchema = requestBodySchema;
    }

    public String getRequestBodySchema() {
        return requestBodySchema;
    }

    public void setResponseBodySchema(String responseBodySchema) {
        this.responseBodySchema = responseBodySchema;
    }

    public String getResponseBodySchema() {
        return responseBodySchema;
    }

    public void setUriParameterDefinitions(Collection<UriParameterDefinition> uriParameterDefinitions) {
        this.uriParameterDefinitions = uriParameterDefinitions;
    }

    public Collection<UriParameterDefinition> getUriParameterDefinitions() {
        return uriParameterDefinitions;
    }


    public void setQueryParameterDefinitions(Collection<QueryParameterDefinition> queryParameterDefinitions) {
        this.queryParameterDefinitions = queryParameterDefinitions;
    }

    public Collection<QueryParameterDefinition> getQueryParameterDefinitions() {
        return queryParameterDefinitions;
    }

    public void setHeaderDefinitions(Collection<HeaderDefinition> headerDefinitions) {
        this.headerDefinitions = headerDefinitions;
    }

    public Collection<HeaderDefinition> getHeaderDefinitions() {
        return headerDefinitions;
    }
}
