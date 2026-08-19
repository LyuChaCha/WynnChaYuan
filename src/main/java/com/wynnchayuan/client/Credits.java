package com.wynnchayuan.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 */
public final class Credits {

    private static List<CreditsScreen.Section> cached;

    private Credits() {}

    public static List<CreditsScreen.Section> sections() {
        if (cached != null) {
            return cached;
        }
        List<CreditsScreen.Section> out = new ArrayList<>();
        try (InputStream in = Credits.class.getResourceAsStream("/assets/wynnchayuan/credits.json")) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String title : root.keySet()) {
                    if (title.startsWith("_")) {
                        continue;
                    }
                    List<String> names = new ArrayList<>();
                    JsonArray arr = root.getAsJsonArray(title);
                    for (JsonElement el : arr) {
                        names.add(el.getAsString());
                    }
                    out.add(new CreditsScreen.Section(title, names));
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 貢獻者名單讀取失敗: " + e.getMessage());
        }
        cached = List.copyOf(out);
        return cached;
    }
}
