package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 語料裡<b>有譯文、卻套不上去</b>的句子。
 *
 * <h2>實機回報（issue #1158）</h2>
 * 譯者替一句聊天補了譯文，畫面上還是英文；而 {@code captured.json} 裡那一句也不見了。
 * 兩邊問的是不同的事：收集端問「語料有沒有這一條」（有，所以不是缺口、不記），
 * 顯示端要把原文的圖示、數字、地名、名字一個一個填回譯文——譯文的佔位符跟原文
 * 對不上，就整句放棄、留原文。結果是這一句<b>哪裡都看不到</b>：畫面上沒翻，
 * 缺口清單裡也沒有，譯者只能猜。
 *
 * <p>所以放棄的那一刻記下來，寫進 {@code captured.json} 的 {@code unapplied}：
 * 原文、譯文、哪一種佔位符差了幾個。
 *
 * <h2>只記確定是語料的問題</h2>
 * 重建譯文的那支程式有很多呼叫端，有些是「試試看這樣拼行不行」——那種失敗是正常的，
 * 記下來會是一堆假警報。這裡只認一種：<b>拿這句原文去語料查到的那一條，佔位符本身
 * 就跟原文對不上</b>。那一條不管走哪條路都套不上。
 *
 * <h2>不從檔案讀回來</h2>
 * 每次開遊戲從空的開始、遇到才記。譯者改好之後重開（或重新載入譯文），它就不在了；
 * 讀回來的話改好的那幾條會一直留在清單上。
 */
public final class UnappliedAudit {

    private UnappliedAudit() {}

    /** 最多記幾條。這是診斷，不能讓一份壞掉的語料把檔案灌爆。 */
    static final int LIMIT = 300;

    /** 問過的原文記幾條。同一行每一幀都會來，問過就不再問。 */
    private static final int ASKED_LIMIT = 4000;

    /** @param why 哪一種佔位符差了幾個，例如 {@code {u}: src 1, dst 2} */
    public record Row(String src, String dst, String why) {}

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();
    private static final Set<String> ASKED = ConcurrentHashMap.newKeySet();
    private static volatile boolean dirty;

    /**
     * 這句原文問過了嗎。問過就不必再查一次語料——物品說明每一幀都會重畫。
     *
     * @return 第一次問回 {@code false}，之後回 {@code true}
     */
    public static boolean asked(String src) {
        if (src == null) {
            return true;
        }
        if (ASKED.size() >= ASKED_LIMIT) {
            ASKED.clear();
        }
        return !ASKED.add(src);
    }

    /**
     * 語料的這一條套不上去的話記下來。
     *
     * @param src 原文的模板
     * @param dst 語料裡這一條的譯文
     */
    public static void note(String src, String dst) {
        try {
            Row row = audit(src, dst);
            if (row == null) {
                return;
            }
            synchronized (ROWS) {
                if (row.equals(ROWS.get(row.src())) || (!ROWS.containsKey(row.src())
                        && ROWS.size() >= LIMIT)) {
                    return;
                }
                ROWS.put(row.src(), row);
            }
            dirty = true;
        } catch (Throwable t) {
            // 這是診斷，絕不能反過來弄壞畫面
        }
    }

    /** 判準本身，不碰清單。測試直接問這一支。 */
    public static Row audit(String src, String dst) {
        if (src == null || dst == null || src.isBlank() || dst.isBlank()) {
            return null;
        }
        String why = why(src, dst);
        if (why == null) {
            return null;
        }
        // 這份檔案是要交出去的：帶著別人名字的不記。只問原文，理由同 OverflowAudit。
        if (PlayerDataFilter.carriesPlayerData(src)) {
            return null;
        }
        return new Row(src.strip(), dst.strip(), why);
    }

    private static final Pattern NUMBER = Pattern.compile("\\{~(\\d*)}");

    /**
     * 譯文的佔位符跟原文差在哪。跟 {@code LineTranslator#rebuildAll} 的判準同一套：
     * <ul>
     *   <li>{@code {#}} 圖示、{@code {p}} 地名：要一樣多；</li>
     *   <li>{@code {u}} 玩家名字：譯文可以少（不提名字是譯法），不能多；</li>
     *   <li>{@code {~}} 數值：照順序取的要一樣多；用了 {@code {~1}} 這種指名寫法的，
     *       指到的號碼要存在。</li>
     * </ul>
     *
     * @return 對得上回 {@code null}
     */
    public static String why(String src, String dst) {
        StringBuilder out = new StringBuilder();
        for (String token : new String[] {GlyphSplitter.GLYPH_PLACEHOLDER,
                GlyphSplitter.PLACE_PLACEHOLDER}) {
            int want = count(src, token);
            int have = count(dst, token);
            if (want != have) {
                add(out, token, want, have);
            }
        }
        int wantUsers = count(src, GlyphSplitter.PLAYER_PLACEHOLDER);
        int haveUsers = count(dst, GlyphSplitter.PLAYER_PLACEHOLDER);
        if (haveUsers > wantUsers) {
            add(out, GlyphSplitter.PLAYER_PLACEHOLDER, wantUsers, haveUsers);
        }
        int wantNumbers = count(src, GlyphSplitter.NUMBER_PLACEHOLDER);
        int plain = 0;
        int highest = 0;
        Matcher m = NUMBER.matcher(dst);
        while (m.find()) {
            if (m.group(1).isEmpty() || "0".equals(m.group(1))) {
                plain++;
            } else {
                highest = Math.max(highest, parse(m.group(1)));
            }
        }
        boolean ok = highest == 0 ? plain == wantNumbers
                : plain <= wantNumbers && highest <= wantNumbers;
        if (!ok) {
            out.append(out.length() == 0 ? "" : "; ").append(GlyphSplitter.NUMBER_PLACEHOLDER)
               .append(": src ").append(wantNumbers).append(", dst ").append(plain);
            if (highest > 0) {
                out.append(" (max {~").append(highest).append("})");
            }
        }
        return out.length() == 0 ? null : out.toString();
    }

    private static void add(StringBuilder out, String token, int want, int have) {
        out.append(out.length() == 0 ? "" : "; ").append(token)
           .append(": src ").append(want).append(", dst ").append(have);
    }

    private static int parse(String digits) {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;          // 長到爆掉的編號一定指不到東西
        }
    }

    private static int count(String text, String token) {
        int n = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + token.length())) {
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------ 存檔

    /** 上次問過之後有沒有變。問完就歸零。 */
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
                o.addProperty("dst", row.dst());
                o.addProperty("why", row.why());
                out.add(o);
            }
        }
        return out;
    }

    /** 譯文重新載入了：改好的那幾條不該留著，全部重問。 */
    public static void clear() {
        ASKED.clear();
        synchronized (ROWS) {
            if (!ROWS.isEmpty()) {
                ROWS.clear();
                dirty = true;
            }
        }
    }
}
