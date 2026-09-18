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

    /** 給測試用。 */
    static void clear() {
        last.clear();
    }
}
