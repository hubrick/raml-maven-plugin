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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;

import java.util.LinkedList;

import static java.util.stream.Collectors.joining;

/**
 * @author ahanin
 * @since 1.0.0
 */
public class ASTUtils {

    private ASTUtils() {
    }

    public static ClassOrInterfaceDeclaration findClassOrInterface(CompilationUnit cu, String simpleClassName) {
        return cu.getTypes().stream()
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .filter(t -> t.getName().equals(simpleClassName)).findFirst().orElse(null);
    }

    public static String toQualifiedNameString(final NameExpr nameExpr) {
        NameExpr pointer = nameExpr;
        final LinkedList<NameExpr> elements = new LinkedList<>();

        do {
            elements.push(pointer);
            if (pointer instanceof QualifiedNameExpr) {
                pointer = ((QualifiedNameExpr) pointer).getQualifier();
            } else {
                pointer = null;
            }
        } while (pointer != null);

        return elements.stream().map(NameExpr::getName).collect(joining("."));
    }
}
