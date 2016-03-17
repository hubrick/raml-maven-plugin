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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static io.takari.maven.testing.TestResources.assertFilesPresent;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author ahanin
 * @since 1.0.0
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions("3.3.3")
public class SpringWebGeneratorMojoTest {

    @Rule
    public final TestResources resources = new TestResources();

    @Rule
    public final TestMavenRuntime maven = new TestMavenRuntime();

    private final MavenRuntime.MavenRuntimeBuilder mavenRuntimeBuilder;

    public SpringWebGeneratorMojoTest(MavenRuntime.MavenRuntimeBuilder mavenRuntimeBuilder) {
        this.mavenRuntimeBuilder = mavenRuntimeBuilder;
    }

    @Test
    public void generateResourceInterface() throws Exception {
        final File basedir = resources.getBasedir("spring-web-generator");

        maven.executeMojo(basedir, "spring-web");

        assertFilesPresent(basedir, "target/generated-sources/raml/tld/example/resources/UsersResource.java");
    }

    @Test
    public void generateModelClassDefinedInIncludedSchema() throws Exception {
        final File basedir = resources.getBasedir("spring-web-generator_included-schema");

        maven.executeMojo(basedir, "spring-web");

        assertFilesPresent(basedir, "target/generated-sources/raml/tld/example/resources/model/User.java");
    }

    @Test
    public void defineUriParameterWithValueReference() throws Exception {
        final File basedir = resources.getBasedir("spring-web-generator_uri-parameter-pattern-value-ref");

        maven.executeMojo(basedir, "spring-web");

        assertFilesPresent(basedir, "target/generated-sources/raml/tld/example/resources/UsersResource.java");

        final File file = new File(basedir, "target/generated-sources/raml/tld/example/resources/UsersResource.java");

        final MethodDeclaration methodDeclaration = getMethodDeclaration(getClassDeclaration(file), "getUsers");

        final Optional<NormalAnnotationExpr> requestMapping = methodDeclaration.getAnnotations().stream()
                .filter(NormalAnnotationExpr.class::isInstance)
                .map(NormalAnnotationExpr.class::cast)
                .filter(a -> a.getName().getName().equals("RequestMapping"))
                .findFirst();

        assertThat("@RequestMapping annotation with 'value' parameter generated", requestMapping.isPresent(), is(true));

        final Optional<MemberValuePair> valueAttribute = requestMapping.get().getPairs().stream()
                .filter(pair -> pair.getName().equals("value"))
                .findFirst();

        assertThat("@RequestMapping.value generated", requestMapping.isPresent(), is(true));

        assertThat("@RequestMapping value contains pattern reference",
                valueAttribute.get().getValue().toStringWithoutComments(),
                is("\"/users/{userId:\" + RegexPatterns.UUID_REGEX + \"}\""));
    }

    @Test
    public void defineContentTypeWithExtension() throws Exception {
        final File basedir = resources.getBasedir("spring-web-generator_json-extension-included-schema");

        maven.executeMojo(basedir, "spring-web");

        assertFilesPresent(basedir, "target/generated-sources/raml/tld/example/resources/UsersResource.java");

        final File file = new File(basedir, "target/generated-sources/raml/tld/example/resources/UsersResource.java");

        final MethodDeclaration methodDeclaration = getMethodDeclaration(getClassDeclaration(file), "postUsers");
        final List<Parameter> parameters = methodDeclaration.getParameters();

        assertThat("Parameters in generated method", parameters.size(), is(1));
    }

    private static MethodDeclaration getMethodDeclaration(ClassOrInterfaceDeclaration controllerDeclaration, String methodName) {
        Optional<MethodDeclaration> getUsersMethod = controllerDeclaration.getMembers().stream()
                .filter(MethodDeclaration.class::isInstance)
                .map(MethodDeclaration.class::cast)
                .filter(method -> method.getName().equals(methodName))
                .findFirst();

        assertThat("'" + methodName + "' method generated", getUsersMethod.isPresent(), is(true));

        return getUsersMethod.get();
    }

    private static ClassOrInterfaceDeclaration getClassDeclaration(File file) throws ParseException, IOException {
        final CompilationUnit compilationUnit = JavaParser.parse(file);
        final String filename = file.getName();
        final String className = filename.replaceAll("(.*)\\.java$", "$1");

        if (className.length() == file.getName().length()) {
            throw new IllegalStateException("Couldn't extract [Java] class name from filename: " + filename);
        }

        Optional<ClassOrInterfaceDeclaration> classDeclaration = compilationUnit.getTypes().stream()
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .filter(declaration -> declaration.getName().equals(className))
                .findFirst();

        assertThat("class " + className + " generated", classDeclaration.isPresent(), is(true));

        return classDeclaration.get();
    }

}
