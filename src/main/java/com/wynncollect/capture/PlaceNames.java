package com.wynncollect.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wynncraft 的地名，用來參數化成 {@value GlyphSplitter#PLACE_PLACEHOLDER}。
 *
 * <h2>為什麼要參數化</h2>
 * 地名是專有名詞，翻譯時通常原樣保留。但它會出現在大量句型裡：
 *
 * <pre>
 *   Troms Citizen / Detlas Citizen / Ragni Citizen ...        137 個地名 × 每種身分
 *   [You are now entering Ragni] / [... entering Detlas] ...  137 條幾乎一樣的句子
 * </pre>
 *
 * 逐條記錄會讓譯者把同一句話翻上百次。換成 {@code {p}} 之後
 * {@code "{p} Citizen"} 翻一次就到處適用，還原時再把原本的地名填回去——
 * <b>地名本身永遠不會被翻譯</b>，正好符合專有名詞該保留的慣例。
 *
 * <p>資料取自 Wynntils 用的官方來源，隨模組打包，不需要連線。
 */
public final class PlaceNames {

    /** 依長度排序後的比對式，長的優先——否則「Nemract Docks」會被切成「Nemract」+「Docks」。 */
    private static final Pattern PATTERN;

    private static final List<String> NAMES;

    static {
        List<String> names = load();
        NAMES = List.copyOf(names);
        PATTERN = names.isEmpty() ? null : compile(names);
    }

    private PlaceNames() {}

    private static List<String> load() {
        List<String> names = new ArrayList<>();
        try (InputStream in = PlaceNames.class.getResourceAsStream(
                "/assets/wynncollect/places.json")) {
            if (in == null) {
                return names;
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("places");
            for (JsonElement el : arr) {
                String name = el.getAsString().strip();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnCollect] 地名清單載入失敗，將不做地名參數化: " + e.getMessage());
        }
        return names;
    }

    private static Pattern compile(List<String> names) {
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(Comparator.comparingInt(String::length).reversed());

        StringBuilder sb = new StringBuilder();
        for (String name : sorted) {
            if (!sb.isEmpty()) {
                sb.append('|');
            }
            sb.append(Pattern.quote(name));
        }
        // 前後用邊界限制，避免 "Ragni" 命中 "Ragnite"。
        // 不加 CASE_INSENSITIVE：地名是專有名詞一定大寫開頭，
        // 區分大小寫才不會把 "swamp"（普通名詞）誤判成地名 "Swamp"。
        return Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + sb + ")(?![\\p{L}\\p{N}])");
    }

    /** 這段文字裡的地名。回傳的 matcher 已經套好邊界規則。 */
    public static Matcher matcher(String text) {
        return PATTERN == null ? null : PATTERN.matcher(text);
    }

    /** 是否剛好等於某個地名（用於 NPC 名牌那種整段就是地名的情況）。 */
    public static boolean isPlace(String text) {
        return text != null && NAMES.contains(text.strip());
    }

    public static int size() {
        return NAMES.size();
    }
}
