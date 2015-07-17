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

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static io.takari.maven.testing.TestResources.assertFilesPresent;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class SpringWebResourceMojoTest {

    @Rule
    public final TestResources resources = new TestResources();

    @Rule
    public final TestMavenRuntime maven = new TestMavenRuntime();

    @Test
    public void testShouldGenerateResourceInterface() throws Exception {
        final File basedir = resources.getBasedir("raml");

        maven.executeMojo(basedir, "spring-web");

        assertFilesPresent(basedir, "target/generated-sources/tld/example/resources/UsersResource.java");
    }

}