package com.example.libusage;

import java.nio.file.Path;

public final class App {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java -jar java-lib-usage.jar <projectRoot>");
            System.exit(2);
        }

        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        System.out.println("Analyzing project at: " + root);
        Output out = LibraryUsageAnalyzer.analyze(root);

        System.out.println(out.toPrettyJson());
    }
}

