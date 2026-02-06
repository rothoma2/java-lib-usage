package com.example.libusage;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ListImportsExample {
    
    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.err.println("Usage: java ListImportsExample <path/to/File.java>");
            System.exit(2);
        }

        Path file = Path.of(args[0]).toAbsolutePath().normalize();

        CompilationUnit cu = StaticJavaParser.parse(file);

        List<String> imports = cu.getImports()
                .stream()
                .map(imp -> imp.getNameAsString()
                        + (imp.isAsterisk() ? ".*" : "")
                        + (imp.isStatic() ? " (static)" : ""))
                .sorted()
                .collect(Collectors.toList());

        imports.forEach(System.out::println);
    }
}
