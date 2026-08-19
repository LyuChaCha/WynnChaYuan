package com.wynnchayuan.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.render.Colors;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 貢獻者名單，讀自 {@code assets/wynnchayuan/credits.json}。
 *
 * <p>做成資料檔而不是寫死在程式裡：加一位譯者不該需要改 Java、重編譯，
 * 送 PR 的人只要動一個 json 就好。
 *
 * <p>每個分區帶自己的顏色，因為「開發者／贊助者／貢獻者」是不同性質的貢獻，
 * 顏色都一樣的話得逐行讀標題才分得出來。
 */
public final class Credits {

    /** 一位貢獻者。{@code mc} 有填就會顯示 Minecraft 頭像。 */
    public record Member(String name, String mc) {
        public boolean hasHead() {
            return mc != null && !mc.isBlank();
        }
    }

    /** 一個分區，例如「開發者」。 */
    public record Section(String role, int color, List<Member> members) {}

    private static List<Section> cached;

    private Credits() {}

    public static List<Section> sections() {
        if (cached != null) {
            return cached;
        }
        List<Section> out = new ArrayList<>();
        try (InputStream in = Credits.class.getResourceAsStream(
                "/assets/wynnchayuan/credits.json")) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray sections = root.getAsJsonArray("sections");
                if (sections != null) {
                    for (JsonElement el : sections) {
                        Section section = readSection(el.getAsJsonObject());
                        if (section != null) {
                            out.add(section);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 貢獻者名單讀取失敗: " + e.getMessage());
        }
        cached = List.copyOf(out);
        return cached;
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
                        m.has("mc") ? m.get("mc").getAsString() : null));
            }
        }
        // 空分區照樣留著：「貢獻者」欄位空著本身就是在邀請別人來填
        return new Section(role, color, List.copyOf(members));
    }

    private static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", "").strip(), 16);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }
}
