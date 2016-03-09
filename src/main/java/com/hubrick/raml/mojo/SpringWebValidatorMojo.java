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

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BaseParameter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.Sets;
import com.hubrick.raml.mojo.util.ASTUtils;
import com.hubrick.raml.mojo.util.JavaNames;
import com.hubrick.raml.util.Memoized;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.AbstractScanner;
import org.codehaus.plexus.util.DirectoryScanner;
import org.javatuples.Pair;
import org.javatuples.Quintet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Mojo(name = "spring-web-validate", defaultPhase = LifecyclePhase.VERIFY)
public class SpringWebValidatorMojo extends AbstractSpringWebMojo {

    @org.apache.maven.plugins.annotations.Parameter(property = "raml.validate.include")
    private List<String> includes;

    @org.apache.maven.plugins.annotations.Parameter(property = "raml.validate.exclude")
    private List<String> excludes;

    private static class ValidationUnit {

        private JDefinedClass model;
        private Memoized<File> generatedSourceFile = Memoized.of(() -> new File(JavaNames.toFilePathString(model.fullName())));
        private Optional<File> sourceFile;

        public ValidationUnit(JDefinedClass model, Optional<File> sourceFile) {
            this.model = model;
            this.sourceFile = sourceFile;
        }

        public JDefinedClass getModel() {
            return model;
        }

        public File getGeneratedSourceFile() {
            return generatedSourceFile.get();
        }

        public Optional<File> getSourceFile() {
            return sourceFile;
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final JCodeModel codeModel = generateCodeModel();

        final Map<String, File> classFileIndex = mavenProject.getCompileSourceRoots().stream()
                .flatMap(sourceRoot -> {
                    final DirectoryScanner directoryScanner = new DirectoryScanner();
                    directoryScanner.setBasedir(sourceRoot);
                    directoryScanner.setIncludes(new String[]{"**/*.java"});
                    directoryScanner.scan();
                    return Arrays.stream(directoryScanner.getIncludedFiles())
                            .map(filePath -> {
                                final String className = JavaNames.classNameOfPath(filePath);
                                return Pair.with(className, new File(sourceRoot, filePath));
                            });
                })
                .collect(toMap(Pair::getValue0, Pair::getValue1));

        final Collection<ValidationUnit> validationUnits = StreamSupport.stream(Spliterators.spliteratorUnknownSize(codeModel.packages(), Spliterator.DISTINCT | Spliterator.NONNULL), false)
                .flatMap(pkg -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(pkg.classes(), Spliterator.DISTINCT | Spliterator.NONNULL), false))
                .map(klass -> new ValidationUnit(klass, Optional.ofNullable(classFileIndex.get(klass.fullName()))))
                .collect(toList());

        if (!validationUnits.isEmpty()) {

            final GenericScanner generatedSourceScanner = new GenericScanner(() -> {
                final Iterator<String> i = validationUnits.stream()
                        .map(validationUnit -> validationUnit.getGeneratedSourceFile().getPath())
                        .iterator();

                return () -> i;
            });

            final List<String> includes = Optional.of(this.includes).filter(i -> !i.isEmpty()).orElse(Collections.singletonList("**/*"));
            generatedSourceScanner.setIncludes(includes.toArray(new String[includes.size()]));
            generatedSourceScanner.setExcludes(excludes.toArray(new String[excludes.size()]));
            generatedSourceScanner.scan();

            final Set<String> generatedSourceFiles = Sets.newHashSet(generatedSourceScanner.getIncludedFiles());

            getLog().info("Generated source files: " + generatedSourceFiles.stream().collect(joining(", ", "[", "]")));

            final List<ValidationUnit> includedValidationUnits = validationUnits.stream()
                    .filter(validationUnit -> generatedSourceFiles.contains(validationUnit.getGeneratedSourceFile().getPath()))
                    .collect(toList());

            validateMissingSources(includedValidationUnits);

            if (!includedValidationUnits.isEmpty()) {

                final InMemoryCodeWriter inMemoryCodeWriter = new InMemoryCodeWriter();
                try {
                    codeModel.build(inMemoryCodeWriter);
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't build generated sources", e);
                }

                final Map<String, List<String>> fileValidationMessages = includedValidationUnits.stream()
                        .map(t -> {
                            checkState(inMemoryCodeWriter.countainsFile(t.getGeneratedSourceFile()), "%s is not part of generated code", t.getGeneratedSourceFile());

                            final String sourceFilePath = t.getGeneratedSourceFile().getPath();
                            final byte[] sourceFileContent;
                            try {
                                sourceFileContent = Files.readAllBytes(t.getSourceFile().get().toPath());
                            } catch (IOException e) {
                                throw new RuntimeException("Failed reading source file: " + t.getSourceFile().get().getParent());
                            }
                            final List<String> messages = validateFile(sourceFileContent, inMemoryCodeWriter.getSource(new File(sourceFilePath)));
                            return Pair.with(sourceFilePath, messages);
                        })
                        .filter(e -> !e.getValue1().isEmpty())
                        .collect(toMap(Pair::getValue0, Pair::getValue1));

                if (!fileValidationMessages.isEmpty()) {
                    final List<String> longMessage = Stream.concat(
                            Stream.of("REST controller source code validation failed"),
                            fileValidationMessages.entrySet().stream().flatMap(e -> Stream.concat(Stream.of(e.getKey() + ":"), e.getValue().stream()))
                    ).collect(toList());

                    final Log log = getLog();
                    if (log.isErrorEnabled()) {
                        longMessage.forEach(log::error);
                    }

                    throw new MojoFailureException(null, "REST controller source code validation failed", longMessage.stream().collect(joining(System.lineSeparator())));
                }
            } else {
                getLog().info("Nothing to validate");
            }
        }
    }

    private void validateMissingSources(Collection<ValidationUnit> validationUnits) throws MojoFailureException {
        final Collection<String> missingSourceClasses = validationUnits.stream()
                .filter(t -> !t.getSourceFile().isPresent())
                .map(t -> t.getModel().fullName())
                .collect(toList());

        if (!missingSourceClasses.isEmpty()) {
            throw new MojoFailureException("No sources found for classes: " + missingSourceClasses.stream().collect(joining(", ")));
        }
    }

    private List<String> validateFile(byte[] sourceFileContent, byte[] generatedSourceFileContent) {
        final CompilationUnit sourceCu;
        try {
            sourceCu = JavaParser.parse(new ByteArrayInputStream(sourceFileContent));
        } catch (ParseException e) {
            throw new RuntimeException("Couldn't parse source file: " + sourceFileContent, e);
        }

        final List<String> messages = new ArrayList<>();
        for (TypeDeclaration typeDeclaration : sourceCu.getTypes()) {

            final String className = sourceCu.getPackage().getName() + "." + typeDeclaration.getName();

            final CompilationUnit targetCu;
            try (final InputStreamReader generatedSourceReader = new InputStreamReader(new ByteArrayInputStream(generatedSourceFileContent))) {
                try {
                    targetCu = JavaParser.parse(generatedSourceReader, false);
                } catch (ParseException e) {
                    throw new RuntimeException("Can't parse generated source: " + className, e);
                }
            } catch (IOException e) {
                throw new RuntimeException("Couldn't read generated source: " + className, e);
            }

            final Optional<TypeDeclaration> generatedTypeDeclaration = targetCu.getTypes().stream()
                    .filter(td -> className.equals(targetCu.getPackage().getName() + "." + td.getName()))
                    .findFirst();

            checkState(generatedTypeDeclaration.isPresent(), "Type declaration not present in generated source: " + className);

            messages.addAll(validateTypeDeclaration(sourceCu, targetCu, className));
        }

        return messages;
    }

    private List<String> validateTypeDeclaration(CompilationUnit sourceCu, CompilationUnit targetCu, String className) {
        return new TypeDeclarationValidator(sourceCu, targetCu, className).validate();
    }

    private static class InMemoryCodeWriter extends CodeWriter {

        private Map<File, ByteArrayOutputStream> sourceIndex = new HashMap<>();

        @Override
        public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
            final File sourceFile = new File(pkg.name().replaceAll("\\.", File.separator), fileName);
            checkState(!sourceIndex.containsKey(sourceFile), "Output stream has already been opened for " + sourceFile.getPath());
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);
            sourceIndex.put(sourceFile, outputStream);
            return outputStream;
        }

        @Override
        public void close() throws IOException {
            // noop
        }

        public byte[] getSource(File sourceFile) {
            if (!sourceIndex.containsKey(sourceFile)) {
                return null;
            } else {
                return sourceIndex.get(sourceFile).toByteArray();
            }
        }

        public boolean countainsFile(File file) {
            return sourceIndex.containsKey(file);
        }
    }

    private static class GenericScanner extends AbstractScanner {

        private Logger logger = LoggerFactory.getLogger(getClass());

        private Set<String> includedFiles = new HashSet<>();
        private Set<String> excludedFiles = new HashSet<>();

        private Supplier<Iterable<String>> files;

        private GenericScanner(Supplier<Iterable<String>> files) {
            this.files = files;
        }

        @Override
        public void scan() {
            if (this.includes == null) {
                this.includes = new String[]{"**/*"};
            }

            if (this.excludes == null) {
                this.excludes = new String[0];
            }

            this.includedFiles.clear();

            logger.info("Scanning generated sources (includes={}, excludes={})", Arrays.stream(this.includes).collect(joining(", ")), Arrays.stream(this.excludes).collect(joining(", ")));

            setupMatchPatterns();

            for (String file : files.get()) {
                if (isIncluded(file)) {
                    this.includedFiles.add(file);
                }
                if (isExcluded(file)) {
                    this.excludedFiles.add(file);
                }
            }
        }

        @Override
        public String[] getIncludedFiles() {
            return this.includedFiles.toArray(new String[this.includedFiles.size()]);
        }

        @Override
        public String[] getIncludedDirectories() {
            throw new UnsupportedOperationException();
        }

        @Override
        public File getBasedir() {
            throw new UnsupportedOperationException();
        }
    }

    private class TypeDeclarationValidator {

        private final CompilationUnit sourceCu;
        private final CompilationUnit targetCu;
        private final String typeName;

        private final TypeSolverVisitor sourceCuTypeSolver = new TypeSolverVisitor();
        private final TypeSolverVisitor targetCuTypeSolver = new TypeSolverVisitor();

        public TypeDeclarationValidator(CompilationUnit sourceCu, CompilationUnit targetCu, String typeName) {
            this.sourceCu = sourceCu;
            this.targetCu = targetCu;
            this.typeName = typeName;
        }

        public List<String> validate() {
            sourceCuTypeSolver.visit(sourceCu, null);
            targetCuTypeSolver.visit(targetCu, null);

            final ClassOrInterfaceDeclaration targetDeclaration = ASTUtils.findClassOrInterface(targetCu, JavaNames.getSimpleClassName(typeName));
            checkState(targetDeclaration != null, "Type is not declared in generated source: %s", typeName);

            final Collection<MethodDeclaration> targetWebMethods = ASTHelper.getNodesByType(targetDeclaration, MethodDeclaration.class).stream()
                    .filter(this::isWebMethod)
                    .collect(toList());


            final Collection<Quintet<Node, Node, Node, Node, DiffStatus>> diffEntries = new ArrayList<>();

            if (!targetWebMethods.isEmpty()) {

                final ClassOrInterfaceDeclaration sourceDeclaration = ASTUtils.findClassOrInterface(sourceCu, JavaNames.getSimpleClassName(typeName));
                checkState(sourceDeclaration != null, "Type is not declared in source: %s", typeName);

                for (MethodDeclaration targetMethod : targetWebMethods) {
                    final Optional<MethodDeclaration> sourceMethod = sourceDeclaration.getMembers().stream()
                            .filter(MethodDeclaration.class::isInstance)
                            .map(MethodDeclaration.class::cast)
                            .filter(m -> m.getName().equals(targetMethod.getName()))
                            .filter(m -> isSignatureMatched(targetMethod, m))
                            .findFirst();

                    diffNode(targetMethod, null, sourceMethod.orElse(null), null, diffEntries::add);
                }
            }

            return diffEntries.stream()
                    .map(this::formatValidationMessage)
                    .collect(toList());
        }

        private String formatValidationMessage(Quintet<Node, Node, Node, Node, DiffStatus> diffEntry) {
            final Node target = diffEntry.getValue0();
            final Node source = diffEntry.getValue2();
            final Node sourceParent = diffEntry.getValue3();
            final DiffStatus diffStatus = diffEntry.getValue4();

            final String message;
            switch (diffStatus) {
                case MISSING:
                    message = String.format("%s %s at %d:%d %s",
                            diffStatus.name(),
                            target.toStringWithoutComments(),
                            sourceParent != null ? sourceParent.getBeginLine() : null,
                            sourceParent != null ? sourceParent.getBeginColumn() : null,
                            sourceParent != null ? sourceParent.toStringWithoutComments() : null);
                    break;
                case MISMATCH:
                    message = String.format("%s %s at %d:%d, expected %s",
                            diffStatus.name(),
                            target.toStringWithoutComments(),
                            source.getBeginLine(),
                            source.getBeginColumn(),
                            source.toStringWithoutComments());
                    break;
                default:
                    throw new IllegalStateException("Unexpected diff status: " + diffStatus);
            }

            return message;
        }

        private boolean isSignatureMatched(MethodDeclaration targetMethod, MethodDeclaration sourceMethod) {
            final List<Parameter> targetParams = targetMethod.getParameters();
            final List<Parameter> sourceParams = sourceMethod.getParameters();
            if (targetParams.size() != sourceParams.size()) {
                return false;
            }

            final List<Type> targetParameterTypes = targetParams.stream()
                    .map(Parameter::getType)
                    .collect(toList());

            final List<Type> sourceParameterTypes = sourceParams.stream()
                    .map(Parameter::getType)
                    .collect(toList());

            return targetParameterTypes.equals(sourceParameterTypes);
        }

        private boolean isWebMethod(MethodDeclaration method) {
            return method.getAnnotations().stream()
                    .map(a -> a.getName().getName())
                    .filter("RequestMapping"::equals)
                    .findFirst()
                    .isPresent();
        }

    }

    private void diffNode(final Node a, final Node aParent,
                          final Node b, final Node bParent,
                          final Consumer<Quintet<Node, Node, Node, Node, DiffStatus>> diffCallback) {
        if (b == null) {
            diffCallback.accept(Quintet.with(a, aParent, b, bParent, DiffStatus.MISSING));
        } else if (a instanceof NameExpr && !Objects.equals(((NameExpr) a).getName(), ((NameExpr) b).getName())) {
            diffCallback.accept(Quintet.with(a, aParent, b, bParent, DiffStatus.MISMATCH));
        } else if (a instanceof FieldAccessExpr && !Objects.equals(((FieldAccessExpr) a).getFieldExpr(), ((FieldAccessExpr) b).getFieldExpr())) {
            diffCallback.accept(Quintet.with(a, aParent, b, bParent, DiffStatus.MISMATCH));
        } else if (a instanceof LiteralExpr && !Objects.equals(a, b)) {
            diffCallback.accept(Quintet.with(a, aParent, b, bParent, DiffStatus.MISMATCH));
        } else if (a instanceof AnnotationExpr && !Objects.equals(((AnnotationExpr) a).getName().getName(), ((AnnotationExpr) b).getName().getName())) {
            diffCallback.accept(Quintet.with(a, aParent, b, bParent, DiffStatus.MISMATCH));
        } else if (a instanceof Parameter && !Objects.equals(((Parameter) a).getType(), ((Parameter) b).getType())) {
            diffCallback.accept(Quintet.with(a, aParent, b, bParent, DiffStatus.MISMATCH));
        } else if (a instanceof MethodDeclaration) {
            final Iterator<Parameter> iSourceParameters = ((MethodDeclaration) b).getParameters().iterator();
            for (Parameter parameter : ((MethodDeclaration) a).getParameters()) {
                diffNode(parameter, a, iSourceParameters.hasNext() ? iSourceParameters.next() : null, b, diffCallback);
            }
            introspect(a, b, diffCallback);
        } else {
            introspect(a, b, diffCallback);
        }
    }

    private void introspect(Node a, Node b, Consumer<Quintet<Node, Node, Node, Node, DiffStatus>> diffCallback) {
        a.getChildrenNodes().stream()
                .filter(this::isIntrospectionRelevantNode)
                .forEach(node -> diffNode(node, a, findMatch(node, b.getChildrenNodes()), b, diffCallback));
    }

    private boolean isIntrospectionRelevantNode(Node node) {
        if (node instanceof Type && node.getParentNode() instanceof MethodDeclaration) {
            return false;
        }

        if (node instanceof Parameter && node.getParentNode() instanceof MethodDeclaration) {
            return false;
        }

        if (!(node instanceof AnnotationExpr) && node.getParentNode() instanceof BaseParameter) {
            return false;
        }

        if (node instanceof NameExpr && node.getParentNode() instanceof AnnotationExpr) {
            return false;
        }

        if (node.getParentNode() instanceof FieldAccessExpr) {
            return false;
        }

        return true;
    }

    private Node findMatch(Node node, List<? extends Node> candidates) {
        return candidates.stream()
                .filter(c -> matchNode(node, c))
                .findFirst()
                .orElse(null);
    }

    private boolean matchNode(Node node, Node candidate) {
        if (node.getClass() != candidate.getClass()) {
            return false;
        }

        if (node instanceof AnnotationExpr && AnnotationExpr.class.cast(node).getName().equals(AnnotationExpr.class.cast(candidate).getName())) {
            return true;
        }

        if (node instanceof MemberValuePair && MemberValuePair.class.cast(node).getName().equals(MemberValuePair.class.cast(candidate).getName())) {
            return true;
        }

        if (node instanceof Expression) {
            return true;
        }

        return false;
    }

    private interface TypeSolver {

        String getQualifiedName(String type);

        NameExpr asQualifiedNameExpr(NameExpr nameExpr);

    }

    private static class TypeSolverVisitor extends VoidVisitorAdapter<Object> implements TypeSolver {

        private final Set<ImportDeclaration> importDeclarations = new HashSet<>();

        @Override
        public void visit(ImportDeclaration importDeclaration, Object arg) {
            importDeclarations.add(importDeclaration);
        }

        @Override
        public String getQualifiedName(String type) {
            return importDeclarations.stream()
                    .map(ImportDeclaration::getName)
                    .filter(i -> i.getName().equals(type))
                    .map(ASTUtils::toQualifiedNameString)
                    .findFirst()
                    .orElse(type);
        }

        @Override
        public NameExpr asQualifiedNameExpr(NameExpr nameExpr) {
            if (nameExpr instanceof QualifiedNameExpr) {
                return nameExpr;
            } else return importDeclarations.stream()
                    .filter(i -> i.getName().getName().equals(nameExpr.getName()))
                    .map(i -> new QualifiedNameExpr(((QualifiedNameExpr) i.getName()).getQualifier(), nameExpr.getName()))
                    .findFirst()
                    .map(NameExpr.class::cast)
                    .orElse(nameExpr);
        }

    }

    private enum DiffStatus {
        MISMATCH, MISSING
    }

}
