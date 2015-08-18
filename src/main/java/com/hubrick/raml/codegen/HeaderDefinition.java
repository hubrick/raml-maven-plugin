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

import org.raml.model.parameter.Header;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class HeaderDefinition extends AbstractParameterDefinition<Header> {

    public HeaderDefinition(String name, String javaType, Header uriParameter) {
        super(name, javaType, uriParameter);
    }

}
