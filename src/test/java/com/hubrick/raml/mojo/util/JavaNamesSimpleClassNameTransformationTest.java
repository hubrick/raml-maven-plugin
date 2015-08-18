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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author ahanin
 * @since 1.0.0
 */
@RunWith(Parameterized.class)
public class JavaNamesSimpleClassNameTransformationTest {

    @Parameterized.Parameters(name = "{index}: toJavaName(\"{0}\").equals(\"{1}\")")
    public static String[][] getParameters() {
        return new String[][]{
                {"user-id", "UserId"},
                {"userId", "UserId"},
                {"user", "User"},
        };
    }

    private String input;
    private String expectation;

    public JavaNamesSimpleClassNameTransformationTest(String input, String expectation) {
        this.input = input;
        this.expectation = expectation;
    }

    @Test
    public void testShouldTransformToJavaName() throws Exception {
        assertThat(JavaNames.toSimpleClassName(input), equalTo(expectation));
    }

}