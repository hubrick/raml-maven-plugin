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
package com.hubrick.raml.mojo.freemarker;

import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class ClassPathTemplateLoader implements TemplateLoader {

    private ClassLoader classLoader;

    public ClassPathTemplateLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        return classLoader.getResourceAsStream(name);
    }

    @Override
    public long getLastModified(Object templateSource) {
        return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        return new InputStreamReader((InputStream) templateSource);
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        ((InputStream) templateSource).close();
    }
}
