package com.example.libusage;

import java.util.*;

public final class Output {
    private final Map<String, SortedSet<String>> usage;

    public Output(Map<String, SortedSet<String>> usage) {
        this.usage = usage;
    }

    public String toPrettyJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        List<String> classes = new ArrayList<>(usage.keySet());
        Collections.sort(classes);

        for (int i = 0; i < classes.size(); i++) {
            String cls = classes.get(i);
            sb.append("  ").append(jsonStr(cls)).append(": [\n");

            List<String> sigs = new ArrayList<>(usage.getOrDefault(cls, new TreeSet<>()));
            for (int j = 0; j < sigs.size(); j++) {
                sb.append("    ").append(jsonStr(sigs.get(j)));
                if (j < sigs.size() - 1) sb.append(",");
                sb.append("\n");
            }

            sb.append("  ]");
            if (i < classes.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String jsonStr(String s) {
        String esc = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + esc + "\"";
    }
}

