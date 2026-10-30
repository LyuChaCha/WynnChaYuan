package com.wynnchayuan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class CorpusGap {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private CorpusGap() {}

    public static Path without(Path dir, String key) throws IOException {
        Path out = Files.createTempDirectory("wynnchayuan-gap");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        for (Path file : files) {
            Path target = out.resolve(dir.relativize(file).toString());
            Files.createDirectories(target.getParent());
            if (!file.toString().endsWith(".json")) {
                Files.copy(file, target);
                continue;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (!text.contains(jsonText(key))) {
                Files.writeString(target, text, StandardCharsets.UTF_8);
                continue;
            }
            JsonElement root = JsonParser.parseString(text);
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("entries") && obj.get("entries").isJsonObject()) {
                    for (var e : obj.getAsJsonObject("entries").entrySet()) {
                        JsonObject entry = e.getValue().getAsJsonObject();
                        if (entry.has("src") && key.equals(entry.get("src").getAsString())) {
                            entry.addProperty("dst", "");
                        }
                    }
                } else if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                    obj.addProperty(key, "");
                }
            }
            Files.writeString(target, GSON.toJson(root), StandardCharsets.UTF_8);
        }
        return out;
    }

    private static String jsonText(String key) {
        String quoted = GSON.toJson(key);
        return quoted.substring(1, quoted.length() - 1);
    }
}
