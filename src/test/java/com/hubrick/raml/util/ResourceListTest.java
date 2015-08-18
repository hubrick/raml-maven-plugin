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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.raml.model.Raml;
import org.raml.model.Resource;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class ResourceListTest {

    @Test
    public void shouldFlattenResourceList() throws Exception {
        final Raml raml = new Raml();

        final Resource usersResource = new Resource();
        final Resource userResource = new Resource();
        final Resource userSettingsResource = new Resource();

        usersResource.getResources().put("/{userId}", userResource);
        userResource.getResources().put("/settings", userSettingsResource);

        raml.getResources().put("/users", usersResource);

        final List<Resource> flattenned = ResourceList.of(Lists.newArrayList(usersResource))
                .flatten()
                .collect(toList());

        assertThat(flattenned, containsInAnyOrder(usersResource, userResource, userSettingsResource));
    }

}