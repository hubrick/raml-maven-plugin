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
package com.hubrick.raml.util;

import org.raml.model.Raml;
import org.raml.model.Resource;

import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class ResourceList {

    private final Iterable<Resource> resources;

    public ResourceList(Iterable<Resource> resources) {
        this.resources = resources;
    }

    public static ResourceList of(Iterable<Resource> resources) {
        return new ResourceList(resources);
    }

    public static ResourceList of(Raml raml) {
        return new ResourceList(raml.getResources().values());
    }

    public Stream<Resource> flatten() {
        return concat(
                stream(resources.spliterator(), false),
                stream(resources.spliterator(), false)
                        .flatMap(r -> ResourceList.of(r.getResources().values()).flatten())
        );
    }

}
