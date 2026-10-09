package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 翻出來了、但<b>譯文比原本那一格大</b>的句子，連同「大多少」記下來。
 *
 * <h2>為什麼要有</h2>
 * 譯文太長時每個介面各有各的退路：對話框塞不下就改由小框接手（issue #864，
 * 先前是整句退回英文）、物品說明整份撐寬。退路讓畫面不壞，但也讓問題<b>看不見</b>
 * ——譯者不知道哪一句害玩家多看了一個小框、哪一行把整份說明撐寬了三十個像素。
 * 西文近兩成的台詞塞不進對話框，這個數字是離線量出來的；實機上到底是哪幾句
 * 被玩家看到，沒有任何地方記著。
 *
 * <p>這裡把那些句子去重之後放進 {@code captured.json} 的 {@code overflow}：
 * 玩家照平常那樣把檔案交出來，譯者就拿得到「哪一句、在哪個介面、原本容得下多少、
 * 譯文要多少」，可以直接把那一句改短——或者看出某個介面整類都超出，再回頭改程式。
 *
 * <h2>記什麼</h2>
 * <ul>
 *   <li>{@code dialogue}：就地取代的對話，單位是<b>列</b>。{@code room} 是原文佔的
 *       列數，{@code need} 是譯文要的列數。</li>
 *   <li>{@code tooltip}：物品說明被撐寬，單位是<b>像素</b>。{@code room} 是原文
 *       最寬那一行，{@code need} 是撐寬之後的寬度；記的是譯文裡最寬的那一行。</li>
 * </ul>
 *
 * <h2>不收什麼</h2>
 * 帶著玩家名字或別的玩家資料的句子整條不收，跟 capture 的其他部分同一套濾網。
 * 兩個介面交進來的都是<b>模板</b>（玩家名字已經是 {@code {u}}），所以帶 {@code {u}}
 * 的台詞照收——對話裡叫玩家名字的句子很多，整類不收就少掉一大塊。
 * 同一句只記一次；後來塞得下了（譯文改短、語料同步過）再看到一次就自己消失。
 */
public final class OverflowAudit {

    private OverflowAudit() {}

    /** 最多留幾句。 */
    static final int LIMIT = 300;

    public static final String DIALOGUE = "dialogue";
    public static final String TOOLTIP = "tooltip";

    public static final String ROWS_UNIT = "rows";
    public static final String PX_UNIT = "px";

    /** 物品說明撐寬不到這麼多像素的不記：一兩個像素是字型的進位，不是譯文太長。 */
    static final int MIN_PX = 4;

    /**
     * @param where 哪個介面（{@link #DIALOGUE}、{@link #TOOLTIP}）
     * @param src   原文的模板（跟語料的鍵同一種寫法）
     * @param dst   當時的譯文
     * @param room  原本容得下多少
     * @param need  譯文要多少
     * @param unit  {@link #ROWS_UNIT} 或 {@link #PX_UNIT}
     */
    public record Row(String where, String src, String dst, int room, int need, String unit) {}

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();
    private static volatile boolean dirty;

    /**
     * 譯文量完之後叫一次；塞得下的也要叫，記過的那一條才會被拿掉。
     */
    public static void note(String where, String src, String dst, int room, int need,
                            String unit) {
        try {
            // 塞得下、而且清單是空的：絕大多數的呼叫都是這一種，每一幀都會來，直接放過。
            if (need <= room && size() == 0) {
                return;
            }
            String id = id(where, src);
            Row row = audit(where, src, dst, room, need, unit);
            synchronized (ROWS) {
                if (row == null) {
                    if (id != null && !ROWS.isEmpty() && ROWS.remove(id) != null) {
                        dirty = true;
                    }
                    return;
                }
                Row was = ROWS.get(id);
                if (row.equals(was)) {
                    return;                    // 每一幀都會來，一樣的不必再寫一次
                }
                if (was == null && ROWS.size() >= LIMIT) {
                    return;
                }
                ROWS.put(id, row);
            }
            dirty = true;
        } catch (Throwable t) {
            // 這是診斷，絕不能反過來弄壞畫面
        }
    }

    /** 見 {@link #note}；抽出來是為了測得到。塞得下或不該收的回 {@code null}。 */
    static Row audit(String where, String src, String dst, int room, int need, String unit) {
        if (where == null || src == null || dst == null || unit == null) {
            return null;
        }
        String key = clean(src);
        if (key.isBlank() || dst.isBlank() || room <= 0 || need <= room) {
            return null;
        }
        if (PX_UNIT.equals(unit) && need - room < MIN_PX) {
            return null;
        }
        // 只問原文。那支濾網把「出現中日文」也當成玩家資料（遊戲自己的原文不會有），
        // 拿去問譯文的話，中文、日文的譯文會一條都收不到。
        if (PlayerDataFilter.carriesPlayerData(key)) {
            return null;
        }
        return new Row(where, key, clean(dst), room, need, unit);
    }

    private static String id(String where, String src) {
        return where == null || src == null ? null : where + '\u0000' + clean(src);
    }

    private static String clean(String text) {
        return GlyphSplitter.parametrizeNumbers(GlyphSplitter.stripGlyphChars(text)).strip();
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
                o.addProperty("where", row.where());
                o.addProperty("src", row.src());
                o.addProperty("dst", row.dst());
                o.addProperty("room", row.room());
                o.addProperty("need", row.need());
                o.addProperty("unit", row.unit());
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
                    Row row = new Row(o.get("where").getAsString(), o.get("src").getAsString(),
                            o.get("dst").getAsString(), o.get("room").getAsInt(),
                            o.get("need").getAsInt(), o.get("unit").getAsString());
                    if (!row.src().isBlank() && ROWS.size() < LIMIT) {
                        ROWS.putIfAbsent(row.where() + '\u0000' + row.src(), row);
                    }
                } catch (RuntimeException e) {
                    // 舊格式或被手改壞的一條，跳過
                }
            }
        }
    }

    /** 測試用：整份倒掉。 */
    public static void clear() {
        synchronized (ROWS) {
            if (!ROWS.isEmpty()) {
                ROWS.clear();
                dirty = true;
            }
        }
    }
}
