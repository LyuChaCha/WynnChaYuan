package com.wynnchayuan.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.ModLog;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Badges;
import com.wynnchayuan.render.Colors;
import com.wynnchayuan.translate.ErrorDebug;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Credits {

    public record Member(String label, String key, String mc,
                         int colour, boolean rainbow, boolean hidden, String cape) {
        public boolean hasHead() {
            return mc != null && !mc.isBlank();
        }

        public String name() {
            return key == null ? label : T.s(key);
        }
    }

    public record Section(String label, String key, int color, List<Member> members) {
        public String role() {
            return key == null ? label : T.s(key);
        }

        public String id() {
            return key == null ? label : key.substring(key.lastIndexOf('.') + 1);
        }
    }

    private static volatile List<Section> cached;

    private Credits() {}

    public static final String FILE = "credits.json";

    public static List<Section> sections() {
        List<Section> have = cached;
        if (have != null) {
            return have;
        }
        List<Section> out = fromSynced();
        if (out.isEmpty()) {
            try (InputStream in = Credits.class.getResourceAsStream(
                    "/assets/wynnchayuan/credits.json")) {
                if (in != null) {
                    out = read(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                ModLog.warn("貢獻者名單讀取失敗: " + e.getMessage());
            }
        }
        have = List.copyOf(out);
        cached = have;
        return have;
    }

    public static void reload() {
        cached = null;
        Badges.reload();
    }

    private static List<Section> fromSynced() {
        try {
            Path dir = WynnChaYuan.configDir();
            Path file = dir == null ? null : dir.resolve(FILE);
            if (file == null || !Files.isRegularFile(file)) {
                return List.of();
            }
            try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return read(in);
            }
        } catch (Throwable t) {
            ErrorDebug.note("Credits#fromSynced", null, t);
            return List.of();
        }
    }

    public static List<Section> read(Reader in) {
        List<Section> out = new ArrayList<>();
        JsonObject root = JsonParser.parseReader(in).getAsJsonObject();
        JsonArray sections = root.getAsJsonArray("sections");
        if (sections != null) {
            for (JsonElement el : sections) {
                Section section = readSection(el.getAsJsonObject());
                if (section != null) {
                    out.add(section);
                }
            }
        }
        return out;
    }

    private static Section readSection(JsonObject obj) {
        String role = obj.has("role") ? obj.get("role").getAsString() : null;
        if (role == null || role.isBlank()) {
            return null;
        }
        int color = obj.has("color")
                ? Colors.opaque(parseHex(obj.get("color").getAsString()))
                : Colors.TEXT;

        List<Member> members = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray("members");
        if (arr != null) {
            for (JsonElement el : arr) {
                JsonObject m = el.getAsJsonObject();
                String name = m.has("name") ? m.get("name").getAsString() : null;
                if (name == null || name.isBlank()) {
                    continue;
                }
                members.add(new Member(name,
                        m.has("key") ? m.get("key").getAsString() : null,
                        m.has("mc") ? m.get("mc").getAsString() : null,
                        m.has("color") ? Colors.opaque(parseHex(m.get("color").getAsString())) : 0,
                        flag(m, "rainbow"), flag(m, "hide"),
                        m.has("cape") && m.get("cape").isJsonPrimitive()
                                ? m.get("cape").getAsString() : null));
            }
        }
        return new Section(role, obj.has("key") ? obj.get("key").getAsString() : null,
                color, List.copyOf(members));
    }

    private static boolean flag(JsonObject m, String name) {
        try {
            return m.has(name) && m.get(name).getAsBoolean();
        } catch (Exception e) {
            ErrorDebug.note("Credits#flag", null, e);
            return false;
        }
    }

    private static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", "").strip(), 16);
        } catch (Exception e) {
            ErrorDebug.note("Credits#parseHex", null, e);
            return 0xFFFFFF;
        }
    }
}
