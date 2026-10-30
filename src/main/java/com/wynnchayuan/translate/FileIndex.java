package com.wynnchayuan.translate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FileIndex {

    private static String resource(String lang) {
        return Languages.resource(lang) + "_index.json";
    }

    private static final List<String> FALLBACK = List.of("ui-labels.json", "npc.json", "quest.json");

    private static final java.util.Map<String, List<String>> cached =
            new java.util.concurrent.ConcurrentHashMap<>();

    private FileIndex() {}

    public static final List<String> SCOPED = List.of("scoped/label.json", "scoped/bossbar.json",
            "scoped/name.json", "scoped/archetype.json", "scoped/gear.json",
            "scoped/wynnventory.json");

    public static List<String> bundled() {
        return bundled(Languages.DEFAULT);
    }

    public static List<String> bundled(String lang) {
        List<String> hit = cached.get(lang);
        if (hit != null) {
            return hit;
        }
        List<String> found = read(() -> FileIndex.class.getResourceAsStream(resource(lang)));
        cached.put(lang, found);
        return found;
    }

    public static List<String> forDirectory(Path dir) {
        Path local = dir.resolve("_index.json");
        if (Files.isRegularFile(local)) {
            List<String> found = read(() -> Files.newInputStream(local));
            if (!found.isEmpty()) {
                return found;
            }
        }
        return bundled();
    }

    public static List<String> inDirectory(Path dir) {
        Path local = dir.resolve("_index.json");
        if (!Files.isRegularFile(local)) {
            return List.of();
        }
        return read(() -> Files.newInputStream(local));
    }

    private static List<String> read(StreamSource source) {
        List<String> names = new ArrayList<>();
        try (InputStream in = source.open()) {
            if (in == null) {
                return FALLBACK;
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("files");
            if (arr == null) {
                return FALLBACK;
            }
            for (JsonElement el : arr) {
                String name = el.getAsString().strip();
                if (name.endsWith(".json") && !name.startsWith("_") && safeName(name)) {
                    names.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 譯文清單讀取失敗，改用內建集合: " + e.getMessage());
            return FALLBACK;
        }
        return names.isEmpty() ? FALLBACK : List.copyOf(names);
    }

    static boolean safeName(String name) {
        return !name.isEmpty() && !name.startsWith("/") && !name.contains("..")
                && !name.contains("\\") && !name.contains(":") && !name.contains("//")
                && !name.contains("?") && !name.contains("#");
    }

    @FunctionalInterface
    private interface StreamSource {
        InputStream open() throws Exception;
    }
}
