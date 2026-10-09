package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 翻出來了、但<b>原文的某個顏色沒有回到譯文上</b>的句子，連同原文的顏色分段記下來。
 *
 * <h2>為什麼要有</h2>
 * 語料裡看不到顏色：鍵是純文字，顏色是畫的時候才從實機那一行的樣式讀出來的。
 * 上色靠的是「原文這一段的中文說法，在譯文裡找得到」——中文語序不同、同一個詞
 * 兩種譯法、或是敘述被重新折行，就會有一段顏色貼不回去。Lootrun 的使命敘述、
 * 討伐戰增益卡的說明、任務說明是重災區，而且只有使用者在遊戲裡一句一句看才
 * 看得出來（2026-10-08：「顏色還是會有錯誤，需要人工對」）。
 *
 * <p>先前的診斷（{@code majorid-debug.txt} 的「可用的顏色」「重點段」）有這些
 * 資料，但那個檔有額度、開遊戲沒多久就滿了，而且只有本機看得到。這裡把同一份
 * 資料<b>只留有問題的</b>、去重之後放進 {@code captured.json}——玩家照平常那樣
 * 把檔案交出來，譯者就拿得到「哪一句、原文哪幾段是什麼顏色、哪個顏色掉了」，
 * 可以直接寫 {@code {c1}}／{@code {c2}}，不必再進遊戲對。
 *
 * <h2>什麼算「有問題」</h2>
 * 原文至少兩種顏色，而其中一種<b>在譯文上一個字都沒有</b>。譯者已經自己寫了
 * 色碼的不記（那是他決定的）。顏色貼到了錯的詞上這種，機器分不出來，不在這裡。
 *
 * <h2>不收什麼</h2>
 * 帶著玩家名字或別的玩家資料的句子整條不收；數字一律換成佔位符。這一段跟
 * capture 的其他部分用同一套濾網。
 */
public final class ColourAudit {

    private ColourAudit() {}

    /** 最多留幾句。一句大約兩三百個位元組，再多檔案就胖得不像話了。 */
    static final int LIMIT = 400;

    /** 原文裡的一段：顏色（{@code #RRGGBB}，粗體加 {@code b}、底線加 {@code u}）與那一段字。 */
    public record Run(String colour, String text) {}

    /**
     * @param src    原文的模板（跟語料的鍵同一種寫法），多行用換行接
     * @param dst    當時查到的譯文，一行一個
     * @param runs   原文的顏色分段，照先後
     * @param missed 譯文上完全沒出現的那幾個顏色
     */
    public record Row(String src, List<String> dst, List<Run> runs, List<String> missed) {}

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();

    /** 看過的「原文＋譯文」。見 {@link #note}。 */
    private static final Set<String> SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final int SEEN_LIMIT = 4096;
    private static volatile boolean dirty;

    private static final Pattern MARKER = Pattern.compile("\\{(?:c[^}]*|w\\d|/)}");

    /**
     * 一段譯文貼完顏色之後叫一次。
     *
     * @param src      原文每一行的模板
     * @param dst      譯文每一行（還帶著佔位符）
     * @param runs     原文所有的樣式片段，照先後
     * @param accents  拿去譯文裡比對的重點段
     * @param used     每個重點段有沒有真的貼上
     * @param dominant 這一段的主樣式（譯文的底色）——它不算「掉了」
     */
    public static void note(List<String> src, String[] dst, List<LineParts.Piece> runs,
                            List<LineParts.Piece> accents, boolean[] used, Style dominant) {
        try {
            if (src == null || dst == null) {
                return;
            }
            // 同一句每一幀都會來（名牌、追蹤欄不走快取）。看過的「原文＋當時的譯文」
            // 直接放過；譯文換了（語料同步、有人修好了）才再看一次。
            String id = String.join("\n", src) + '\u0000' + String.join("\n", dst);
            if (!SEEN.add(id)) {
                return;
            }
            if (SEEN.size() > SEEN_LIMIT) {
                SEEN.clear();
            }
            Row row = audit(src, dst, runs, accents, used, dominant);
            if (row == null) {
                // 這一句現在沒問題。之前記過的話拿掉——譯文修好之後，
                // 玩家再看到一次它就自己從清單上消失，不必有人去刪。
                if (src != null) {
                    synchronized (ROWS) {
                        if (!ROWS.isEmpty() && ROWS.remove(String.join("\n", src)) != null) {
                            dirty = true;
                        }
                    }
                }
                return;
            }
            synchronized (ROWS) {
                if (ROWS.containsKey(row.src()) || ROWS.size() >= LIMIT) {
                    return;
                }
                ROWS.put(row.src(), row);
            }
            dirty = true;
        } catch (Throwable t) {
            // 這是診斷，絕不能反過來弄壞畫面
        }
    }

    /** 見 {@link #note}；抽出來是為了測得到。沒問題的回 {@code null}。 */
    static Row audit(List<String> src, String[] dst, List<LineParts.Piece> runs,
                     List<LineParts.Piece> accents, boolean[] used, Style dominant) {
        if (src == null || dst == null || runs == null || accents == null) {
            return null;
        }
        String key = String.join("\n", src);
        if (key.isBlank() || key.contains(GlyphSplitter.PLAYER_PLACEHOLDER)
                || PlayerDataFilter.carriesPlayerData(key)) {
            return null;
        }
        for (String line : dst) {
            if (line != null && MARKER.matcher(line).find()) {
                return null;                   // 譯者自己指定了顏色
            }
        }
        // 原文有哪幾種顏色（只算有字的片段；圖示與空白的樣式不是「顏色」）
        Set<String> present = new LinkedHashSet<>();
        List<Run> shown = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            String text = clean(run.text());
            if (text.isBlank()) {
                continue;
            }
            String colour = describe(run.style());
            shown.add(new Run(colour, text));
            if (hasLetter(text)) {
                present.add(colour);
            }
        }
        if (present.size() < 2) {
            return null;
        }
        Set<String> placed = new LinkedHashSet<>();
        placed.add(describe(dominant));
        for (int i = 0; i < accents.size(); i++) {
            if (used != null && i < used.length && used[i]) {
                placed.add(describe(accents.get(i).style()));
            }
        }
        List<String> missed = new ArrayList<>();
        for (String colour : present) {
            if (!placed.contains(colour)) {
                missed.add(colour);
            }
        }
        if (missed.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>(dst.length);
        for (String line : dst) {
            lines.add(line == null ? "" : line);
        }
        return new Row(key, lines, merge(shown), missed);
    }

    /** 相鄰同色的片段併成一段：原文常把一句話拆成好幾個同色的小片段。 */
    private static List<Run> merge(List<Run> runs) {
        List<Run> out = new ArrayList<>();
        for (Run run : runs) {
            if (!out.isEmpty() && out.get(out.size() - 1).colour().equals(run.colour())) {
                Run last = out.remove(out.size() - 1);
                out.add(new Run(last.colour(), last.text() + run.text()));
            } else {
                out.add(run);
            }
        }
        return out;
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return GlyphSplitter.parametrizeNumbers(GlyphSplitter.stripGlyphChars(text));
    }

    private static boolean hasLetter(String text) {
        return text.codePoints().anyMatch(Character::isLetter);
    }

    static String describe(Style style) {
        if (style == null) {
            return "inherit";
        }
        TextColor colour = style.getColor();
        String base = colour == null ? "inherit"
                : String.format("#%06X", colour.getValue() & 0xFFFFFF);
        return base + (style.isBold() ? "b" : "") + (style.isUnderlined() ? "u" : "");
    }

    // ------------------------------------------------------------ 存檔

    /** 有沒有新的東西還沒寫進檔案；問完就歸零。 */
    public static boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    public static int size() {
        synchronized (ROWS) {
            return ROWS.size();
        }
    }

    public static JsonArray toJson() {
        JsonArray out = new JsonArray();
        synchronized (ROWS) {
            for (Row row : ROWS.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("src", row.src());
                JsonArray dst = new JsonArray();
                row.dst().forEach(dst::add);
                o.add("dst", dst);
                JsonArray runs = new JsonArray();
                for (Run run : row.runs()) {
                    JsonArray one = new JsonArray();
                    one.add(run.colour());
                    one.add(run.text());
                    runs.add(one);
                }
                o.add("runs", runs);
                JsonArray missed = new JsonArray();
                row.missed().forEach(missed::add);
                o.add("missed", missed);
                out.add(o);
            }
        }
        return out;
    }

    /** 讀回上次存的那一份；格式不對的那幾條跳過。 */
    public static void load(JsonElement saved) {
        if (saved == null || !saved.isJsonArray()) {
            return;
        }
        synchronized (ROWS) {
            for (JsonElement el : saved.getAsJsonArray()) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    String src = o.get("src").getAsString();
                    List<String> dst = new ArrayList<>();
                    o.getAsJsonArray("dst").forEach(e -> dst.add(e.getAsString()));
                    List<Run> runs = new ArrayList<>();
                    for (JsonElement r : o.getAsJsonArray("runs")) {
                        JsonArray one = r.getAsJsonArray();
                        runs.add(new Run(one.get(0).getAsString(), one.get(1).getAsString()));
                    }
                    List<String> missed = new ArrayList<>();
                    o.getAsJsonArray("missed").forEach(e -> missed.add(e.getAsString()));
                    if (!src.isBlank() && ROWS.size() < LIMIT) {
                        ROWS.putIfAbsent(src, new Row(src, dst, runs, missed));
                    }
                } catch (RuntimeException e) {
                    // 舊格式或被手改壞的一條，跳過
                }
            }
        }
    }

    /**
     * 語料換過之後（同步、換語言）整份倒掉：記的是「當時那份譯文」掉了什麼顏色，
     * 譯文一改就不算數了，留著只會讓人去修已經修好的句子。
     */
    public static void clear() {
        SEEN.clear();
        synchronized (ROWS) {
            if (!ROWS.isEmpty()) {
                ROWS.clear();
                dirty = true;
            }
        }
    }
}
