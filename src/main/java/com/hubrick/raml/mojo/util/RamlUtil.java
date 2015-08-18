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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.raml.model.Action;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.spliterator;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class RamlUtil {

    public static String toJavaMethodName(Action action) {
        final List<String> nameElements =
                stream(spliterator(action.getResource().getUri().split("\\/")), false)
                        .filter(path -> !path.matches(".*\\{[^\\}]*\\}.*"))
                        .map(path -> stream(spliterator(path.split("(?i:[^a-z0-9])")), false)
                                .map(StringUtils::capitalize)
                                .collect(toList()))
                        .flatMap(tokens -> tokens.stream())
                        .collect(toList());

        return Joiner.on("").join(Iterables.concat(Collections.singleton(action.getType().name().toLowerCase()), nameElements));
    }

}
