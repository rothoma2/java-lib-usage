package com.example.libusage;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ListMethodInvocationsExample {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java -jar java-lib-usage-1.0.0-shaded.jar <path/to/File.java>");
            System.exit(2);
        }

        Path file = Path.of(args[0]).toAbsolutePath().normalize();
        CompilationUnit cu = StaticJavaParser.parse(file);

        List<String> calls = cu.findAll(MethodCallExpr.class).stream()
                .sorted(Comparator.comparingInt(m -> m.getRange().map(r -> r.begin.line).orElse(Integer.MAX_VALUE)))
                .map(m -> {
                    String scope = m.getScope().map(Object::toString).orElse("<no-scope>");
                    String name = m.getNameAsString();
                    String argsList = m.getArguments().stream().map(Object::toString).collect(Collectors.joining(", "));
                    String line = m.getRange().map(r -> String.valueOf(r.begin.line)).orElse("?");
                    return line + ": " + scope + "." + name + "(" + argsList + ")";
                })
                .collect(Collectors.toList());

        calls.forEach(System.out::println);
    }
}
