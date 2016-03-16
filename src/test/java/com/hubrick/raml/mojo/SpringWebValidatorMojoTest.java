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
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.fail;

/**
 * @author ahanin
 * @since 1.0.0
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions("3.3.3")
public class SpringWebValidatorMojoTest {

    @Rule
    public final TestResources resources = new TestResources();

    @Rule
    public final TestMavenRuntime maven = new TestMavenRuntime();

    private final MavenRuntimeBuilder mavenRuntimeBuilder;

    public SpringWebValidatorMojoTest(MavenRuntimeBuilder mavenRuntimeBuilder) {
        this.mavenRuntimeBuilder = mavenRuntimeBuilder;
    }

    @Test
    public void succeedOnValidControllerInterface() throws Exception {
        final File basedir = resources.getBasedir("spring-web-validator");
        maven.executeMojo(basedir, "spring-web-validate");
    }

    @Test
    public void failOnInvalidControllerInterface() throws Exception {
        final File basedir = resources.getBasedir("spring-web-validator_invalid-interface");
        try {
            maven.executeMojo(basedir, "spring-web-validate");
            fail("Should throw " + MojoFailureException.class.getName());
        } catch (MojoFailureException e) {
            // expected behaviour
        }
    }

    @Test
    public void failOnMissingSourceFiles() throws Exception {
        final File basedir = resources.getBasedir("spring-web-validator_missing-source-files");

        try {
            maven.executeMojo(basedir, "spring-web-validate");
            fail("Should throw " + MojoFailureException.class.getName());
        } catch (MojoFailureException ex) {
            // expected behaviour
        }
    }

    @Test
    public void succeedOnValidRequestMappingPattern() throws Exception {
        final File basedir = resources.getBasedir("spring-web-validator_valid-path-pattern");
        maven.executeMojo(basedir, "spring-web-validate");
    }

    @Test
    public void failOnInvalidRequestMappingPattern() throws Exception {
        final File basedir = resources.getBasedir("spring-web-validator_invalid-path-pattern");
        try {
            maven.executeMojo(basedir, "spring-web-validate");
            fail("Should throw " + MojoFailureException.class.getName());
        } catch (MojoFailureException ex) {
            // expected behaviour
        }
    }

}
