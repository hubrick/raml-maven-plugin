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

import java.util.function.Supplier;

public class Memoized<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private Supplier<T> current;

    private Memoized(Supplier<T> supplier) {
        this.supplier = supplier;
        this.current = () -> swapper();
    }

    private T swapper() {
        T obj = supplier.get();
        current = () -> obj;
        return obj;
    }

    public static <T> Memoized<T> of(final Supplier<T> supplier) {
        return new Memoized<>(supplier);
    }

    @Override
    public synchronized T get() {
        return current.get();
    }

}
