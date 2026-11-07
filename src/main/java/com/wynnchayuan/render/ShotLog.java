package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.translate.ErrorDebug;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;

final class ShotLog {

    static final String FILE = "shot-debug.txt";

    private ShotLog() {}

    static boolean write(Path debugDir, ModConfig config, String stage) {
        if (debugDir == null || stage == null || config == null || !config.debugDumps()) {
            return false;
        }
        try {
            Files.writeString(debugDir.resolve(FILE),
                    LocalTime.now().withNano(0) + "  " + stage + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (Throwable t) {
            ErrorDebug.note("ShotLog#write", null, t);
            return false;
        }
    }
}
