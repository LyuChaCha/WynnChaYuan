package com.wynnchayuan.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.SafeFiles;
import com.wynnchayuan.capture.LineParts;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DialogueTint {

    public static final String FILE = "dialogue-colours.json";

    private static final int KEEP = 2048;

    private static final int PREFIX_FLOOR = 4;

    private static final Map<String, List<LineParts.Piece>> SEEN =
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, List<LineParts.Piece>> eldest) {
                    return size() > KEEP;
                }
            };

    private static final AtomicBoolean dirty = new AtomicBoolean(false);

    private static volatile Path file;

    private DialogueTint() {}

    public static void init(Path target, String lang) {
        file = target;
        merge(bundled(lang));
        merge(SafeFiles.readObject(target, 4L * 1024 * 1024));
    }

    private static void merge(JsonObject root) {
        if (root == null || !root.has("lines")
                || !root.get("lines").isJsonObject()) {
            return;
        }
        synchronized (SEEN) {
            for (Map.Entry<String, JsonElement> each
                    : root.getAsJsonObject("lines").entrySet()) {
                List<LineParts.Piece> pieces = read(each.getValue());
                if (!pieces.isEmpty()) {
                    SEEN.put(each.getKey(), pieces);
                }
            }
        }
    }

    private static JsonObject bundled(String lang) {
        try (InputStream in = DialogueTint.class.getResourceAsStream(
                "/assets/wynnchayuan/colours/" + lang + ".json")) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    static void forTest() {
        file = null;
        synchronized (SEEN) {
            SEEN.clear();
        }
        dirty.set(false);
    }

    public static List<LineParts.Piece> of(String line) {
        if (line == null || line.isEmpty()) {
            return List.of();
        }
        synchronized (SEEN) {
            List<LineParts.Piece> exact = SEEN.get(line);
            if (exact != null) {
                return exact;
            }
            if (line.length() < PREFIX_FLOOR) {
                return List.of();
            }
            String best = null;
            for (String each : SEEN.keySet()) {
                if (each.startsWith(line)
                        && (best == null || each.length() < best.length())) {
                    best = each;
                }
            }
            if (best != null) {
                return SEEN.get(best);
            }
        }
        return List.of();
    }

    public static void learn(String line, List<LineParts.Piece> pieces) {
        if (file == null || line == null || line.isEmpty() || pieces.isEmpty()) {
            return;
        }
        synchronized (SEEN) {
            List<LineParts.Piece> had = SEEN.get(line);
            if (had != null && had.equals(pieces)) {
                return;
            }
            SEEN.keySet().removeIf(each ->
                    !each.equals(line) && line.startsWith(each));
            for (String each : SEEN.keySet()) {
                if (!each.equals(line) && each.startsWith(line)) {
                    return;
                }
            }
            SEEN.put(line, List.copyOf(pieces));
        }
        dirty.set(true);
    }

    public static void flush() {
        Path target = file;
        if (target == null || !dirty.getAndSet(false)) {
            return;
        }
        JsonObject lines = new JsonObject();
        synchronized (SEEN) {
            for (Map.Entry<String, List<LineParts.Piece>> each : SEEN.entrySet()) {
                lines.add(each.getKey(), write(each.getValue()));
            }
        }
        JsonObject root = new JsonObject();
        root.addProperty("_note", "對話裡哪幾段是強調色。純粹是畫面上量到的快取，"
                + "刪掉只會讓每一句第一次讀的時候，物品名先白一下再變色。");
        root.add("lines", lines);
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .disableHtmlEscaping().create();
        try {
            SafeFiles.writeAtomically(target, gson.toJson(root));
        } catch (Exception e) {
            dirty.set(true);
            throw new RuntimeException(e);
        }
    }

    private static List<LineParts.Piece> read(JsonElement value) {
        List<LineParts.Piece> out = new ArrayList<>();
        if (!value.isJsonArray()) {
            return out;
        }
        for (JsonElement each : value.getAsJsonArray()) {
            if (!each.isJsonObject()) {
                continue;
            }
            JsonObject piece = each.getAsJsonObject();
            JsonElement text = piece.get("text");
            JsonElement colour = piece.get("colour");
            if (text == null || !text.isJsonPrimitive()) {
                continue;
            }
            TextColor tone = colour == null || !colour.isJsonPrimitive()
                    ? null
                    : TextColor.parseColor(colour.getAsString()).result().orElse(null);
            out.add(new LineParts.Piece(text.getAsString(),
                    Style.EMPTY.withColor(tone)));
        }
        return out;
    }

    private static JsonArray write(List<LineParts.Piece> pieces) {
        JsonArray out = new JsonArray();
        for (LineParts.Piece piece : pieces) {
            JsonObject each = new JsonObject();
            each.addProperty("text", piece.text());
            TextColor tone = piece.style().getColor();
            if (tone != null) {
                each.addProperty("colour", tone.serialize());
            }
            out.add(each);
        }
        return out;
    }
}
