package com.wynnchayuan.capture;

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

    /**
     * 地名照<b>第一個字元</b>分組，給 {@link #mayContain} 先篩用。
     *
     * <h2>為什麼需要</h2>
     * 比對式是四百多個地名串成的一長串「或」，Java 的正則不會把它整理成字典樹：
     * 每一個起點都要把四百多個分支試一遍。{@link LineParts#of} 每一段字都會掃一次，
     * tooltip 整段查表的時候同一行還會被掃上幾十次——離線量測冷 tooltip 有一半的時間
     * 花在這裡（51 行的裝備說明 11.6ms，其中約 7ms 是這條比對式）。
     *
     * <p>絕大多數的字裡根本沒有地名。先用這張表問「有沒有任何一個地名出現在這裡」，
     * 沒有就不必跑比對式。這只是<b>必要條件</b>：有出現才交給原本的比對式，邊界規則、
     * 長的優先，全部照舊，所以結果跟以前逐字相同（見 {@code PlaceNamesIndexTest}）。
     */
    private static final java.util.Map<Character, String[]> BY_FIRST;

    static {
        List<String> names = load();
        NAMES = List.copyOf(names);
        PATTERN = names.isEmpty() ? null : compile(names);
        java.util.Map<Character, List<String>> grouped = new java.util.HashMap<>();
        for (String name : NAMES) {
            grouped.computeIfAbsent(name.charAt(0), k -> new ArrayList<>()).add(name);
        }
        java.util.Map<Character, String[]> index = new java.util.HashMap<>();
        grouped.forEach((first, list) -> index.put(first, list.toArray(new String[0])));
        BY_FIRST = java.util.Map.copyOf(index);
    }

    private PlaceNames() {}

    private static List<String> load() {
        List<String> names = new ArrayList<>();
        try (InputStream in = PlaceNames.class.getResourceAsStream(
                "/assets/wynnchayuan/places.json")) {
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
            System.err.println("[WynnChaYuan] 地名清單載入失敗，將不做地名參數化: " + e.getMessage());
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
        if (PATTERN == null) {
            return null;
        }
        // 沒有任何地名出現在這段字裡：比對式一定找不到，給它一段空字串就好——
        // 呼叫端照樣拿到一個 matcher，find() 一次都不會成立，跟以前掃完整段的結果一樣。
        return PATTERN.matcher(text == null || mayContain(text) ? text : "");
    }

    /**
     * 這段字裡有沒有<b>可能</b>出現地名：有沒有任何一個位置剛好接著某個地名的全文。
     * 不看邊界——邊界交給比對式判斷；這裡回答「否」時比對式一定也找不到。
     */
    static boolean mayContain(String text) {
        for (int i = 0; i < text.length(); i++) {
            String[] names = BY_FIRST.get(text.charAt(i));
            if (names == null) {
                continue;
            }
            for (String name : names) {
                if (text.startsWith(name, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 是否剛好等於某個地名（用於 NPC 名牌那種整段就是地名的情況）。 */
    public static boolean isPlace(String text) {
        return text != null && NAMES.contains(text.strip());
    }

    /**
     * 有沒有哪個地名以這幾個字開頭——地名打到一半時，判斷「這裡可能是一個 {@code {p}}」。
     */
    public static boolean anyStartsWith(String partial) {
        if (partial == null || partial.isEmpty()) {
            return true;
        }
        for (String name : NAMES) {
            if (name.startsWith(partial)) {
                return true;
            }
        }
        return false;
    }

    public static int size() {
        return NAMES.size();
    }
}
