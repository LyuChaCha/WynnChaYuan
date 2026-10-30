package com.wynnchayuan.translate;

import com.wynnchayuan.ModLog;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ErrorDebug {

    private static final String FILE = "error-debug.txt";

    private static final int LIMIT = 40;

    private static final Set<String> seen = ConcurrentHashMap.newKeySet();

    private static volatile Path dir;

    private ErrorDebug() {}

    public static void into(Path configDir) {
        dir = configDir;
    }

    public static void note(String where, String note, Throwable t) {
        if (t == null) {
            return;
        }
        ModLog.error(where, t);
        Path target = dir;
        if (target == null || seen.size() >= LIMIT
                || !seen.add(where + '|' + t.getClass().getName())) {
            return;
        }
        try {
            StringWriter trace = new StringWriter();
            t.printStackTrace(new PrintWriter(trace));
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(where).append(" ===").append(System.lineSeparator());
            if (note != null && !note.isBlank()) {
                sb.append("處理中：").append(note).append(System.lineSeparator());
            }
            sb.append(trace).append(System.lineSeparator());
            Files.writeString(target.resolve(FILE), sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ignored) {
        }
    }
}
