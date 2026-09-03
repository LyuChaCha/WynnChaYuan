package com.wynnchayuan.translate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Major ID 的敘述裡引用到的技能名稱，畫面上要換得成中文。
 *
 * <h2>為什麼語料裡看起來是半中半英</h2>
 * 技能與狀態的名稱在敘述裡是<b>刻意留英文</b>的——它們在原文裡各自有顏色，
 * 而顏色是拿原文的字面到譯文裡找的（見 {@code LineTranslator#appendText}）。
 * 語料裡先把名稱換成中文的話，顏色就貼不回去了。
 *
 * <p>真正換掉名稱的是<b>詞表</b>：{@link TranslationStore#findTerm} 在畫的時候
 * 逐個名稱替換。所以「語料裡是英文」是對的，「畫面上還是英文」才是壞的。
 *
 * <pre>
 *   語料  Serpent's Garden 改以你使用 Swan Dive 的落地位置為中心。
 *   畫面  秘技·蛇華苑 改以你使用 飛燕落 的落地位置為中心。
 * </pre>
 *
 * <h2>這條測試在盯什麼</h2>
 * 詞表有兩道門檻會讓名稱靜靜地掉出去：名稱短於
 * {@code MIN_TERM_LENGTH}（四個字元），或是收名稱的檔案沒有標
 * {@code itemNames:false}。掉出去之後畫面上就是英文，而且<b>不會有任何診斷
 * 跳出來</b>——語料是滿的、validate 也是綠的。
 *
 * <p>所以這裡拿真正的 {@code findTerm} 把每一條敘述跑一遍，剩下的英文
 * 必須都在 {@link #PROPER_NOUNS} 裡。要新增例外就得在這裡寫下來，
 * 那正是希望有人看一眼的時候。
 */
public final class MajorIdTermsTest {

    private static int failures = 0;

    /**
     * 刻意留原文的專有名詞。
     *
     * <p>人名、教派名、傳說武器名——整份語料的慣例都是留原文
     * （{@code Grootslang 之巢}、{@code Corkian 強化道具}），這裡跟著。
     */
    private static final Set<String> PROPER_NOUNS = Set.of(
            "Twain",        // Twain's Arc  -> Twain 之弧
            "Aubri",        // Aubri's Tears -> Aubri 之淚
            "Flain",        // Flain Remnants -> Flain 殘影
            "Bakal",        // Bakal's Grasp -> Bakal 之握
            "Orphion",      // Orphion's Pulse -> Orphion 之脈
            // 技能在遊戲裡的正式名稱是複數的 Mantle of the Bovemists，
            // 而 Major ID 的句子用的是單數所有格。譯名跟著技能走。
            "Bovemists");   // Mantle of the Bovemists -> Bovemists 之庇

    /** 鍵盤按鍵不翻。 */
    private static final Set<String> KEYS = Set.of("Shift", "Ctrl", "Alt");

    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z'-]+");

    private static final Path MAJOR_ID = Path.of(
            "src/main/resources/assets/wynnchayuan/translations",
            Languages.DEFAULT, "major-id.json");

    public static void main(String[] args) throws IOException {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        JsonObject root = JsonParser.parseString(
                Files.readString(MAJOR_ID, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject entries = root.getAsJsonObject("entries");

        int descs = 0;
        int swapped = 0;
        // 用 TreeMap 讓輸出穩定：同一個字每次都印在同一個地方，diff 才看得懂。
        TreeMap<String, String> leftover = new TreeMap<>();
        List<String> blank = new ArrayList<>();

        for (String key : entries.keySet()) {
            JsonElement el = entries.get(key);
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject e = el.getAsJsonObject();
            String dst = e.has("dst") ? e.get("dst").getAsString() : "";
            if (dst.isBlank()) {
                blank.add(key);
                continue;
            }
            if (!"desc".equals(e.has("role") ? e.get("role").getAsString() : "")) {
                continue;
            }
            descs++;
            Swap swap = render(dst, store);
            swapped += swap.count();
            Matcher m = WORD.matcher(swap.text());
            while (m.find()) {
                String word = m.group();
                String bare = word.endsWith("'s") ? word.substring(0, word.length() - 2) : word;
                if (PROPER_NOUNS.contains(bare) || KEYS.contains(bare)) {
                    continue;
                }
                leftover.putIfAbsent(word, key);
            }
        }

        check("每一條 Major ID 都有譯文（沒有的：" + blank + "）", blank.isEmpty());
        check("敘述有被掃到（實際 " + descs + " 條）", descs >= 100);
        check("詞表真的有在換（換掉 " + swapped + " 處）", swapped >= descs);
        for (var entry : leftover.entrySet()) {
            check("畫面上不該留著「" + entry.getKey() + "」（" + entry.getValue() + "）", false);
        }
        if (leftover.isEmpty()) {
            System.out.println("  [PASS] 替換後沒有多餘的英文");
        }

        System.out.println(failures == 0
                ? "MajorIdTerms: 全部通過" : "MajorIdTerms: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** @param count 換掉了幾個名稱 */
    private record Swap(String text, int count) {}

    /**
     * 照畫面上的做法把名稱換成中文。
     *
     * <p>與 {@code LineTranslator#appendText} 同一條路：從左到右找下一個詞表
     * 命中，命中就換、沒有就把剩下的原樣接上。
     */
    private static Swap render(String text, TranslationStore store) {
        StringBuilder out = new StringBuilder(text.length());
        int at = 0;
        int count = 0;
        while (at < text.length()) {
            TranslationStore.Term term = store.findTerm(text, at);
            if (term == null) {
                out.append(text, at, text.length());
                break;
            }
            out.append(text, at, term.start()).append(term.translation());
            at = term.end();
            count++;
        }
        return new Swap(out.toString(), count);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
