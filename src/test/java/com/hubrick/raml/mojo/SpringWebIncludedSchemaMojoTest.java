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
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static io.takari.maven.testing.TestResources.assertFilesPresent;

/**
 * @author ahanin
 * @since 1.0.0
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions("3.3.3")
public class SpringWebIncludedSchemaMojoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringWebIncludedSchemaMojoTest.class);

    @Rule
    public final TestResources resources = new TestResources();

    @Rule
    public final TestMavenRuntime maven = new TestMavenRuntime();

    private final MavenRuntimeBuilder mavenRuntimeBuilder;

    public SpringWebIncludedSchemaMojoTest(MavenRuntimeBuilder mavenRuntimeBuilder) {
        this.mavenRuntimeBuilder = mavenRuntimeBuilder;
    }

    @Test
    public void testGenerateModelClassDefinedInIncludedSchema() throws Exception {
        final File basedir = resources.getBasedir("spring-web-included-schema");

        maven.executeMojo(basedir, "spring-web");

        assertFilesPresent(basedir, "target/generated-sources/raml/tld/example/resources/model/User.java");
    }

}
