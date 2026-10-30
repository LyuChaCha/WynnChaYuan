package com.wynnchayuan.golden;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

final class GoldenCorpus {

    static final String NUMBER = "3";
    static final String PLAYER = "Steve";
    static final String PLACE = "Ragni";
    static final String GLYPH = "";

    record DialogueLine(String key, String shown, String quest) {}

    private GoldenCorpus() {}

    static List<String> languages(Path root) throws IOException {
        try (Stream<Path> dirs = Files.list(root)) {
            return dirs.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    static SortedSet<String> sources(Path langDir) throws IOException {
        SortedSet<String> out = new TreeSet<>();
        for (Path file : jsonFiles(langDir)) {
            JsonObject root = parse(file);
            if (root == null) {
                continue;
            }
            if (root.get("entries") instanceof JsonObject entries) {
                for (Map.Entry<String, JsonElement> e : entries.entrySet()) {
                    if (e.getValue() instanceof JsonObject entry && entry.has("src")
                            && entry.get("src").isJsonPrimitive()) {
                        out.add(entry.get("src").getAsString());
                    }
                }
                continue;
            }
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                if (!e.getKey().startsWith("_") && e.getValue().isJsonPrimitive()) {
                    out.add(e.getKey());
                }
            }
        }
        out.remove("");
        return out;
    }

    static List<DialogueLine> dialogue(Path langDir, int limit) throws IOException {
        JsonObject root = parse(langDir.resolve("quest-dialogue.json"));
        if (root == null || !(root.get("entries") instanceof JsonObject entries)) {
            return List.of();
        }
        SortedMap<String, DialogueLine> eligible = new TreeMap<>();
        for (Map.Entry<String, JsonElement> e : entries.entrySet()) {
            if (!(e.getValue() instanceof JsonObject entry) || !entry.has("src")) {
                continue;
            }
            String src = entry.get("src").getAsString().strip();
            if (src.length() < 8 || src.contains("{#}") || src.contains("\n")) {
                continue;
            }
            String quest = entry.has("quest") ? entry.get("quest").getAsString() : null;
            eligible.put(e.getKey(), new DialogueLine(e.getKey(), fill(src), quest));
        }
        int stride = Math.max(1, eligible.size() / Math.max(1, limit));
        List<DialogueLine> out = new ArrayList<>();
        int at = 0;
        for (DialogueLine line : eligible.values()) {
            if (at++ % stride == 0 && out.size() < limit) {
                out.add(line);
            }
        }
        return out;
    }

    static String fill(String src) {
        return src.replaceAll("\\{~\\d*}", NUMBER)
                .replace("{u}", PLAYER)
                .replace("{p}", PLACE)
                .replace("{#}", GLYPH);
    }

    static StyledText styled(String src) {
        String shown = src.replaceAll("\\{~\\d*}", NUMBER)
                .replace("{u}", PLAYER)
                .replace("{p}", PLACE);
        MutableComponent root = Component.empty();
        int from = 0;
        for (int at = shown.indexOf("{#}"); at >= 0; at = shown.indexOf("{#}", from)) {
            if (at > from) {
                root.append(Component.literal(shown.substring(from, at)));
            }
            root.append(Component.literal(GLYPH));
            from = at + 3;
        }
        if (from < shown.length()) {
            root.append(Component.literal(shown.substring(from)));
        }
        return StyledText.fromComponent(root);
    }

    private static List<Path> jsonFiles(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().startsWith("_"))
                    .sorted()
                    .toList();
        }
    }

    private static JsonObject parse(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        return root instanceof JsonObject object ? object : null;
    }
}
