package com.wynnchayuan.translate;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ErrorDebug {

    private static final String FILE = "error-debug.txt";

    private static final int LIMIT = 20;

    private static final Set<String> seen = new LinkedHashSet<>();

    private static Path dir;

    private ErrorDebug() {}

    public static void into(Path configDir) {
        dir = configDir;
    }

    public static void note(String where, String note, Throwable t) {
        if (dir == null || t == null) {
            return;
        }
        try {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            String key = where + "|" + fingerprint(trace);
            synchronized (seen) {
                if (seen.size() >= LIMIT || !seen.add(key)) {
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(where).append(" ===").append(System.lineSeparator());
            if (note != null && !note.isBlank()) {
                sb.append("處理中：").append(note).append(System.lineSeparator());
            }
            sb.append(trace).append(System.lineSeparator());
            Files.writeString(dir.resolve(FILE), sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static String fingerprint(String trace) {
        String[] lines = trace.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length && i < 4; i++) {
            sb.append(lines[i].strip()).append('\n');
        }
        return sb.toString();
    }
}
