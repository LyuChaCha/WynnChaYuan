package com.wynnchayuan.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 對話選項的跑馬燈：太長的選項會一格一格往左捲。
 *
 * <h2>實機回報 2026-09-18</h2>
 * 選項「Do you remember anything from before…」太長，Wynncraft 每個 tick 只送
 * 一個固定寬度的視窗過來：
 *
 * <pre>
 *   Do you remember anything fro
 *   o you remember anything from
 *    you remember anything from b
 *   …
 * </pre>
 *
 * <b>整句從來不會完整出現</b>。語料只能收第一格（大寫開頭那一個），所以就地取代
 * 那條路第一格翻得出來，往後每一格都查不到、變回英文，捲回開頭又變中文——
 * 畫面上就是選項在中英之間閃。
 *
 * <h2>做法</h2>
 * 記住每一列（每一列有自己的字型 {@code choice_0}～{@code choice_3}）上一格的原文
 * 與譯文。新的一格如果剛好是上一格往左捲了一到三個字，就是同一個選項，
 * 沿用那一份譯文。捲回開頭時第一格本來就查得到，會自己接回來。
 *
 * <p>小框那條路本來就只認第一格（捲到一半的視窗直接略過，見
 * {@code DialogueOverlay#scrolling}），不需要這個。
 */
final class Marquee {

    /** 每一列（字型）上一格的原文與譯文。 */
    private static final Map<String, String[]> last = new ConcurrentHashMap<>();

    /** 一個 tick 最多捲幾個字。實機是一個，留一點餘裕給掉幀。 */
    private static final int MAX_STEP = 3;

    /** 重疊太短就不算——幾個字母的巧合接得上任何東西。 */
    private static final int MIN_OVERLAP = 8;

    private Marquee() {}

    /** 這一列查到了譯文：記下來，後面捲動的幾格要沿用。 */
    static void remember(String row, String raw, String translated) {
        if (row != null && raw != null && translated != null) {
            last.put(row, new String[] {raw, translated});
        }
    }

    /**
     * 這一格接得上這一列上一格的話，回傳同一份譯文；否則 {@code null}。
     */
    static String follow(String row, String raw) {
        if (row == null || raw == null) {
            return null;
        }
        String[] before = last.get(row);
        if (before == null || !continues(before[0], raw)) {
            return null;
        }
        last.put(row, new String[] {raw, before[1]});
        return before[1];
    }

    /** {@code next} 是不是 {@code prev} 往左捲了一到 {@value #MAX_STEP} 個字。 */
    static boolean continues(String prev, String next) {
        // 空白不算：視窗的頭尾會被 strip 掉，捲到空格那一格就會少一個字，
        // 照字面比對會在每個空格處斷掉。
        String p = prev.replace(" ", "");
        String n = next.replace(" ", "");
        for (int d = 0; d <= MAX_STEP && d < p.length(); d++) {
            String tail = p.substring(d);
            if (tail.length() >= MIN_OVERLAP && n.startsWith(tail)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 內文的跑馬燈：就地取代時，塞不進對話框的那一列自己捲
    // ------------------------------------------------------------------
    //
    // 上面那一半是<b>遊戲</b>在捲（選項太長，伺服器一格一格送視窗過來），我們只是
    // 沿用譯文。內文沒有人替我們捲：譯文比原文多出一截時，對話框的列數是伺服器
    // 定的，多不出一列。先前那一句只好整句留英文（後來改成讓小框接手）。
    //
    // 現在照選項跑馬燈的樣子自己捲：每一幀從那一列裡取一段剛好放得下的字，
    // 開頭固定在原來的左緣，一次往左移一個字——跟遊戲自己的選項跑馬燈一樣是
    // 整字移動，不會有半個字被切在框邊。
    //
    // 節奏（一輪）：停在開頭 HOLD_START → 以 SPEED 像素／秒往左捲 → 停在結尾
    // HOLD_END → 跳回開頭重來。速度照像素算，不照字數，所以中日韓（一字 10px）
    // 與拉丁字母（約 6px）捲起來一樣快。

    /** 停在開頭多久才開始捲。要夠讀完前面幾列再看這一列。 */
    static final long HOLD_START = 2000;

    /** 捲到結尾停多久才跳回開頭。 */
    static final long HOLD_END = 1500;

    /** 每秒往左捲幾像素：中文約一秒四個字。 */
    static final int SPEED = 40;

    /**
     * 每一條在捲的列：{@code {第一次出現, 最後一次出現}}。
     *
     * <p>要分開記：內文最後一列與選項那幾列可能同時在捲，共用一個時鐘的話，
     * 每一幀都會被另一列當成「換了一列」重新歸零，誰都捲不動。
     */
    private static final Map<String, long[]> scrolls = new java.util.HashMap<>();

    /**
     * 一列多久沒出現就當成已經下去了。action bar 每 tick 都重送，一秒沒看到
     * 就是對話換了；同一句再出現時從「停在開頭」重新開始。
     */
    private static final long FORGET_AFTER = 1000;

    /**
     * 這一列從第一次出現到現在過了多久（毫秒）。換了一列（鍵不同）就從 0 算——
     * 每一句都從「停在開頭」開始。
     */
    static synchronized long since(String key, long now) {
        scrolls.values().removeIf(seen -> now - seen[1] > FORGET_AFTER || now < seen[0]);
        long[] seen = scrolls.computeIfAbsent(key, k -> new long[] {now, now});
        seen[1] = now;
        return Math.max(0, now - seen[0]);
    }

    /**
     * 實機一格選項跑馬燈的原文（2026-09-18 回報的那一句，28 個字元）。遊戲要到比這更長
     * 才開始捲，所以選項框的字區至少有這麼寬。
     */
    static final String CHOICE_SAMPLE = "Do you remember anything fro";

    /**
     * 選項那一列放得下多寬。
     *
     * <p>原文正在捲的時候，那一格就是框寬（{@code was}）；原文很短的選項（「...What?」）
     * 框照樣是那麼寬，不能拿原文的寬度當上限——不然譯文只比原文長一點點就捲起來。
     * 所以取「這一格的寬度」與「一整格的寬度」比較大的那個。
     */
    static int choiceRoom(int was, java.util.function.ToIntFunction<String> measure) {
        return Math.max(was, measure.applyAsInt(CHOICE_SAMPLE));
    }

    /**
     * 照 {@code key} 那一列的時鐘取這一幀的視窗；放得下就是整段。
     */
    static int[] scroll(String key, String text, int room,
                        java.util.function.ToIntFunction<String> measure, long now) {
        if (measure.applyAsInt(text) <= room) {
            return new int[] {0, text.length()};
        }
        return window(text, room, measure, since(key, now));
    }

    /**
     * 這一幀那一列要畫哪一段。
     *
     * @param row     那一列的完整內容（比框寬）
     * @param room    框裡放得下多寬
     * @param measure 量一段字有多寬
     * @param elapsed 這一列出現多久了，見 {@link #since}
     * @return {@code {起, 迄}}：畫 {@code row.substring(起, 迄)}；放得下時就是整列
     */
    static int[] window(String row, int room, java.util.function.ToIntFunction<String> measure,
                        long elapsed) {
        int length = row.length();
        int total = measure.applyAsInt(row);
        if (total <= room || length == 0) {
            return new int[] {0, length};
        }
        int over = total - room;
        long scroll = over * 1000L / SPEED;
        long cycle = HOLD_START + scroll + HOLD_END;
        long t = Math.max(0, elapsed) % cycle;
        int px;
        if (t < HOLD_START) {
            px = 0;
        } else if (t < HOLD_START + scroll) {
            px = (int) Math.min(over, (t - HOLD_START) * SPEED / 1000);
        } else {
            px = over;
        }
        // 第一個「左緣已經捲過 px」的字。整字移動：框邊不會有半個字。
        int from = 0;
        while (from < length && measure.applyAsInt(row.substring(0, from)) < px) {
            from += Character.charCount(row.codePointAt(from));
        }
        // 開頭剛好是空白就跳過，不然那一格整列會往右縮一下
        if (from < length && row.charAt(from) == ' ') {
            from++;
        }
        int to = from;
        while (to < length) {
            int next = to + Character.charCount(row.codePointAt(to));
            if (measure.applyAsInt(row.substring(from, next)) > room) {
                break;
            }
            to = next;
        }
        return new int[] {from, to};
    }

    /** 給測試用。 */
    static synchronized void clear() {
        last.clear();
        scrolls.clear();
    }
}
