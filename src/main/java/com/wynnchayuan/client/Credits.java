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

    /**
     * 一位貢獻者。{@code mc} 有填就會顯示 Minecraft 頭像。
     *
     * @param colour  登記的徽章顏色（{@code "color": "#RRGGBB"}）；0 表示沒登記，用分區的
     * @param rainbow 登記成彩色（{@code "rainbow": true}）
     * @param hidden  這個身分不放在名牌上（{@code "hide": true}）——多重身分的人挑著放
     */
    public record Member(String label, String key, String mc,
                         int colour, boolean rainbow, boolean hidden) {
        public boolean hasHead() {
            return mc != null && !mc.isBlank();
        }

        /** 畫面上的稱呼。資料來源那類有 {@code key}，照 F6 介面語言翻。 */
        public String name() {
            return key == null ? label : T.s(key);
        }
    }

    /** 一個分區，例如「開發者」。 */
    public record Section(String label, String key, int color, List<Member> members) {
        /**
         * 分區標題，照 F6 介面語言翻（{@code credits.json} 的 {@code key}）。
         *
         * <p>先前直接印檔案裡的中文：介面切成英文或西班牙文，標題還是「開發者」。
         */
        public String role() {
            return key == null ? label : T.s(key);
        }

        /**
         * 分區的代號：{@code credits.role.developers} 的 {@code developers}。
         *
         * <p>設定檔與程式拿這個認身分，不拿標題——標題會跟著介面語言變。
         */
        public String id() {
            return key == null ? label : key.substring(key.lastIndexOf('.') + 1);
        }
    }

    private static volatile List<Section> cached;

    private Credits() {}

    /** 同步抓回來的那一份放在設定資料夾底下的檔名。 */
    public static final String FILE = "credits.json";

    public static List<Section> sections() {
        List<Section> have = cached;
        if (have != null) {
            return have;
        }
        // 同步抓到的比 jar 裡的新：加人、登記顏色都不必等發版。
        // 那一份壞了或是空的就退回 jar 裡的，名單不會因為一次抓壞就整個不見。
        List<Section> out = fromSynced();
        if (out.isEmpty()) {
            try (InputStream in = Credits.class.getResourceAsStream(
                    "/assets/wynnchayuan/credits.json")) {
                if (in != null) {
                    out = read(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                System.err.println("[WynnChaYuan] 貢獻者名單讀取失敗: " + e.getMessage());
            }
        }
        have = List.copyOf(out);
        cached = have;
        return have;
    }

    /**
     * 名單檔換了：下次用到時重讀，名牌上的徽章也跟著重建。
     *
     * <p>這是同步的背景執行緒叫的，所以不上鎖——兩條執行緒各讀一次沒關係，
     * 讀到的是同一個檔。
     */
    public static void reload() {
        cached = null;
        com.wynnchayuan.render.Badges.reload();
    }

    private static List<Section> fromSynced() {
        try {
            java.nio.file.Path dir = com.wynnchayuan.WynnChaYuan.configDir();
            java.nio.file.Path file = dir == null ? null : dir.resolve(FILE);
            if (file == null || !java.nio.file.Files.isRegularFile(file)) {
                return List.of();
            }
            try (java.io.Reader in = java.nio.file.Files.newBufferedReader(
                    file, StandardCharsets.UTF_8)) {
                return read(in);
            }
        } catch (Throwable t) {
            return List.of();
        }
    }

    /** 讀一份名單。壞掉的 JSON 會丟例外，由呼叫端決定退回哪一份。 */
    public static List<Section> read(java.io.Reader in) {
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
                        flag(m, "rainbow"), flag(m, "hide")));
            }
        }
        // 空分區照樣留著：「貢獻者」欄位空著本身就是在邀請別人來填
        return new Section(role, obj.has("key") ? obj.get("key").getAsString() : null,
                color, List.copyOf(members));
    }

    /** 名單是人手寫的：寫成字串的 "true" 也認，寫壞了就當沒寫。 */
    private static boolean flag(JsonObject m, String name) {
        try {
            return m.has(name) && m.get(name).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", "").strip(), 16);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }
}
