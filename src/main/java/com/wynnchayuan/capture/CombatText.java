package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;

import java.util.Set;

/**
 * 認出戰鬥中的浮動提示（傷害數字、閃避、格擋…）。
 *
 * <h2>為什麼要特別擋掉</h2>
 * 這些提示在遊戲裡是<b>用名牌實作的</b>——跟 NPC 頭頂那塊是同一種東西，
 * 事件層完全分不出來。結果是打怪時每被打一下，準心前面就冒出一個
 * 「閃避」的翻譯小框，而那正是玩家最不需要被打擾的時候。
 *
 * <p>而且它們也不該被收集：傷害數字每次都不一樣，收進 captured.json 只會
 * 累積成幾千條永遠不會有人翻的垃圾。
 *
 * <p>{@code Dodged} 就是這樣混進 {@code npc.json} 的——它從 captured.json 被
 * 當成 NPC 名稱收進去，於是傷害提示真的查得到譯文，框就跳出來了。
 *
 * <h2>判斷方式</h2>
 * 只看文字內容，因為實體型別分不出來。兩條規則：
 *
 * <ol>
 *   <li>剝掉圖示後<b>只剩數字與符號</b>——傷害、治療、魔力都長這樣</li>
 *   <li>是已知的戰鬥字眼——那是固定的一小組詞</li>
 * </ol>
 *
 * <p>刻意不用「包含」比對：NPC 名稱裡出現這些字的話（例如某個叫
 * {@code Blocked Passage} 的地標）不該被誤殺。
 */
public final class CombatText {

    /**
     * 固定的戰鬥字眼。
     *
     * <p>小寫比對，前後可能帶驚嘆號。清單短是因為 Wynncraft 的這類提示就這幾種；
     * 有漏的話症狀很明顯（打怪時跳框），補上即可。
     */
    private static final Set<String> WORDS = Set.of(
            "dodged", "blocked", "missed", "immune", "absorbed",
            "critical", "crit", "resisted", "evaded");

    private CombatText() {}

    public static boolean isIndicator(StyledText text) {
        return text != null && isIndicator(text.getStringWithoutFormatting());
    }

    public static boolean isIndicator(String raw) {
        if (raw == null) {
            return false;
        }
        String core = strip(raw);
        if (core.isEmpty()) {
            return false;                      // 純圖示由 GlyphSplitter 處理
        }
        if (WORDS.contains(core.toLowerCase())) {
            return true;
        }
        return isNumericOnly(core);
    }

    /** 去掉圖示、空白，以及提示常帶的驚嘆號與正負號。 */
    private static String strip(String raw) {
        StringBuilder sb = new StringBuilder();
        raw.codePoints().forEach(cp -> {
            if (!GlyphSplitter.isGlyphCodePoint(cp)) {
                sb.appendCodePoint(cp);
            }
        });
        String core = sb.toString().strip();
        while (core.endsWith("!")) {
            core = core.substring(0, core.length() - 1).strip();
        }
        return core;
    }

    /**
     * 只有數字與伴隨的符號，沒有任何字母。
     *
     * <p>例如 {@code -1,234}、{@code +560/3s}、{@code 98%}。有字母就代表
     * 是有意義的文字，不能當成傷害數字丟掉。
     */
    private static boolean isNumericOnly(String core) {
        boolean hasDigit = false;
        for (int i = 0; i < core.length(); i++) {
            char c = core.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLetter(c)) {
                return false;
            }
        }
        return hasDigit;
    }
}
