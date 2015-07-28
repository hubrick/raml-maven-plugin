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
package com.hubrick.raml.mojo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class Action {

    private String uri;
    private List<Parameter> uriParameters = new ArrayList<>();
    private List<Parameter> queryParameters = new ArrayList<>();
    private List<Parameter> additionalParameters = new ArrayList<>();
    private String method;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<Parameter> getUriParameters() {
        return uriParameters;
    }

    public void setUriParameters(List<Parameter> uriParameters) {
        this.uriParameters = uriParameters;
    }

    public List<Parameter> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(List<Parameter> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public List<Parameter> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(List<Parameter> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

}
