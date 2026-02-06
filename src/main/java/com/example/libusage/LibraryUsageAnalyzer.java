package com.example.libusage;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class LibraryUsageAnalyzer {

    public static Output analyze(Path projectRoot) throws IOException {
        Path srcRoot = guessSourceRoot(projectRoot);

        Set<String> projectPackagePrefixes = collectProjectPackagePrefixes(srcRoot);

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(srcRoot.toFile()));

        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        JavaParser parser = new JavaParser(config);

        Map<String, SortedSet<String>> usage = new HashMap<>();

        List<Path> javaFiles = Files.walk(srcRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        System.out.println("Found " + javaFiles.size() + " Java files:");
        javaFiles.forEach(f -> System.out.println("  - " + f));

        for (Path jf : javaFiles) {
            ParseResult<CompilationUnit> r = parser.parse(jf);
            System.out.println("ParseResult: " + r);
            if (r.isSuccessful()) {
            CompilationUnit cu = r.getResult().get();
            cu.accept(new Visitor(usage, projectPackagePrefixes), null);
            } else {
            System.err.println("Parse error in " + jf + ": " + r.getProblems());
            }
        }

        return new Output(usage);
    }

    private static Path guessSourceRoot(Path projectRoot) {
        Path maven = projectRoot.resolve("src").resolve("main").resolve("java");
        if (Files.isDirectory(maven)) return maven;
        Path src = projectRoot.resolve("src");
        if (Files.isDirectory(src)) return src;
        return projectRoot;
    }

    private static Set<String> collectProjectPackagePrefixes(Path srcRoot) throws IOException {
        Set<String> pkgs = new HashSet<>();

        List<Path> javaFiles = Files.walk(srcRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        for (Path jf : javaFiles) {
            String text = Files.readString(jf);
            ParseResult<CompilationUnit> r = new JavaParser().parse(text);
            r.getResult().flatMap(CompilationUnit::getPackageDeclaration).ifPresent(pd -> {
                String pkg = pd.getNameAsString();
                String[] segs = pkg.split("\\.");
                if (segs.length >= 1) pkgs.add(segs[0]);
                if (segs.length >= 2) pkgs.add(segs[0] + "." + segs[1]);
            });
        }

        return pkgs;
    }

    private static boolean isExternalType(String qualifiedName, Set<String> projectPrefixes) {
        if (qualifiedName == null || qualifiedName.isBlank()) return false;

        if (qualifiedName.startsWith("java.")
                || qualifiedName.startsWith("javax.")
                || qualifiedName.startsWith("jakarta.")
                || qualifiedName.startsWith("sun.")
                || qualifiedName.startsWith("com.sun.")) {
            return false;
        }

        for (String p : projectPrefixes) {
            if (qualifiedName.startsWith(p + ".")) return false;
        }
        return true;
    }

    private static void add(Map<String, SortedSet<String>> usage, String classFqn, String signature) {
        usage.computeIfAbsent(classFqn, k -> new TreeSet<>()).add(signature);
    }

    private static final class Visitor extends VoidVisitorAdapter<Void> {
        private final Map<String, SortedSet<String>> usage;
        private final Set<String> projectPrefixes;

        private Visitor(Map<String, SortedSet<String>> usage, Set<String> projectPrefixes) {
            this.usage = usage;
            this.projectPrefixes = projectPrefixes;
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);

            try {
                ResolvedMethodDeclaration decl = n.resolve();
                String owner = decl.declaringType().getQualifiedName();
                if (!isExternalType(owner, projectPrefixes)) return;

                String sig = decl.getQualifiedSignature();
                add(usage, owner, sig);
            } catch (Throwable ignored) {
            }
        }

        @Override
        public void visit(ObjectCreationExpr n, Void arg) {
            super.visit(n, arg);

            try {
                ResolvedConstructorDeclaration decl = n.resolve();
                String owner = decl.declaringType().getQualifiedName();
                if (!isExternalType(owner, projectPrefixes)) return;

                String sig = owner + "." + decl.getName() + decl.getSignature();
                add(usage, owner, sig);
            } catch (Throwable ignored) {
            }
        }

        @Override
        public void visit(MethodReferenceExpr n, Void arg) {
            super.visit(n, arg);

            try {
                ResolvedMethodDeclaration decl = n.resolve();
                String owner = decl.declaringType().getQualifiedName();
                if (!isExternalType(owner, projectPrefixes)) return;

                String sig = decl.getQualifiedSignature();
                add(usage, owner, sig);
            } catch (Throwable ignored) {
            }
        }

        @Override
        public void visit(FieldAccessExpr n, Void arg) {
            super.visit(n, arg);

            try {
                ResolvedValueDeclaration decl = n.resolve();
                ResolvedType type = decl.getType();

                if (!type.isReferenceType()) return;
                String owner = type.asReferenceType().getQualifiedName();
                if (!isExternalType(owner, projectPrefixes)) return;

                add(usage, owner, owner + "." + decl.getName());
            } catch (Throwable ignored) {
            }
        }
    }
}

