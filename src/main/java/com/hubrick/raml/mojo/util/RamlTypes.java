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
package com.hubrick.raml.mojo.util;

import com.google.common.collect.ImmutableMap;
import org.raml.model.ParamType;

import java.math.BigDecimal;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author ahanin
 * @since 1.0.0
 */
public final class RamlTypes {

    private RamlTypes() {
    }

    public static final ImmutableMap<ParamType, Class> JAVA_TYPE_MAP = ImmutableMap.<ParamType, Class>builder()
            .put(ParamType.STRING, String.class)
            .put(ParamType.NUMBER, BigDecimal.class)
            .put(ParamType.INTEGER, Integer.class)
            .put(ParamType.DATE, Instant.class)
            .build();

    public static String asJavaTypeName(ParamType paramType) {
        checkState(JAVA_TYPE_MAP.containsKey(paramType), "%s is not defined in java type mapping", paramType);
        return JAVA_TYPE_MAP.get(paramType).getName();
    }

}
