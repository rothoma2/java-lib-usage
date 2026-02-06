package com.example.libusage;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

public final class ListResolvedMethodInvocationsExample {

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 3) {
            System.err.println("Usage:");
            System.err.println("  java -jar ... ListResolvedMethodInvocationsExample <File.java> [<sourceRoot>] [<classpathJars>]");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  java -jar target/java-lib-usage-1.0.0.jar /path/App.java");
            System.err.println("  java -jar target/java-lib-usage-1.0.0.jar /path/App.java /path/project/src/main/java");
            System.err.println("  java -jar target/java-lib-usage-1.0.0.jar /path/App.java /path/src/main/java \"/path/a.jar:/path/b.jar\"");
            System.exit(2);
        }

        Path file = Path.of(args[0]).toAbsolutePath().normalize();
        Path sourceRoot = args.length >= 2 ? Path.of(args[1]).toAbsolutePath().normalize() : guessSourceRoot(file);
        String classpathJars = args.length == 3 ? args[2] : "";

        if (!Files.isRegularFile(file)) {
            System.err.println("Not a file: " + file);
            System.exit(2);
        }

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        if (Files.isDirectory(sourceRoot)) {
            typeSolver.add(new JavaParserTypeSolver(sourceRoot));
        }

        if (!classpathJars.isBlank()) {
            for (String jar : classpathJars.split(":")) {
                if (jar == null || jar.isBlank()) continue;
                Path jarPath = Path.of(jar).toAbsolutePath().normalize();
                if (Files.isRegularFile(jarPath)) {
                    typeSolver.add(new JarTypeSolver(jarPath));
                } else {
                    System.err.println("[WARN] classpath entry not found (skipped): " + jarPath);
                }
            }
        }

        ParserConfiguration cfg = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        JavaParser parser = new JavaParser(cfg);
        ParseResult<CompilationUnit> parsed = parser.parse(file);

        if (parsed.getResult().isEmpty()) {
            System.err.println("Parse failed: " + file);
            parsed.getProblems().forEach(p -> System.err.println("  " + p));
            System.exit(1);
        }

        CompilationUnit cu = parsed.getResult().get();

        cu.findAll(MethodCallExpr.class).stream()
                .sorted(Comparator.comparingInt(m -> m.getRange().map(r -> r.begin.line).orElse(Integer.MAX_VALUE)))
                .forEach(m -> {
                    String line = m.getRange().map(r -> String.valueOf(r.begin.line)).orElse("?");
                    String expr = m.toString();

                    try {
                        ResolvedMethodDeclaration decl = m.resolve();
                        String ownerFqn = decl.declaringType().getQualifiedName();
                        //String sig = decl.getSignature(); // e.g. println(java.lang.String)
                        //System.out.println(line + ": " + ownerFqn + "." + decl.getName()  + "    <-- " + expr);
                        System.out.println(ownerFqn + "." + decl.getSignature());
                    } catch (Throwable t) {
                        // Fallback: show scope if present; this helps you debug missing classpath/solver
                        String scope = m.getScope().map(Object::toString).orElse("<no-scope>");
                        System.out.println(line + ": <UNRESOLVED> " + scope + "." + m.getNameAsString() + "(...)    <-- " + expr);
                    }
                });
    }

    private static Path guessSourceRoot(Path file) {
        // Walk upwards looking for src/main/java, otherwise fall back to parent.
        Path p = file.toAbsolutePath().normalize().getParent();
        while (p != null) {
            Path mavenSrc = p.resolve("src").resolve("main").resolve("java");
            if (Files.isDirectory(mavenSrc)) return mavenSrc;
            p = p.getParent();
        }
        return file.toAbsolutePath().normalize().getParent();
    }
}
