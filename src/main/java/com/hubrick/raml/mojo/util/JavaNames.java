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

import com.google.common.base.Strings;
import com.hubrick.raml.mojo.util.impl.BasicIndexed;
import freemarker.template.utility.StringUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.spliterator;
import static java.util.stream.StreamSupport.stream;

/**
 * @author ahanin
 * @since 1.0.0
 */
public final class JavaNames {

    private JavaNames() {
    }

    public static String toJavaName(String string) {
        checkArgument(!Strings.isNullOrEmpty(string), "Input string cannot be null or empty");
        final String[] elements = string.split("(?i:[^a-z0-9]+)");
        return stream(spliterator(elements), false)
                .map(indexed())
                .map(element -> element.index() > 0 ? StringUtil.capitalize(element.value()) : element.value())
                .collect(Collectors.joining());
    }

    private static <T> Function<T, Indexed<T>> indexed() {
        final AtomicInteger index = new AtomicInteger();
        return (value) -> new BasicIndexed<>(index.getAndIncrement(), value);
    }

}
