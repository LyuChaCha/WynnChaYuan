package com.wynnchayuan.listener;

/**
 * 對話選項的跑馬燈不能被逐格收進語料。
 *
 * <h2>畫面上發生了什麼</h2>
 * 太長的選項 Wynncraft 會做成跑馬燈，一格一格往左捲。收集端先前只擋
 * 「跟上一次完全相同」，於是<b>每一格都是新字串、每一格都收一條</b>。
 *
 * <p>實機回報：四個選項的一段對話收出了 <b>56 條</b>，而且每一條都是切一半的
 * 視窗——{@code mber anything from before yo}、
 * {@code ber anything from before you}、{@code er anything from before you}。
 * 那句話從來沒有完整出現在畫面上，收進來的每一格都是殘句，翻了也對不上。
 *
 * <h2>兩道防線</h2>
 * <ol>
 *   <li><b>穩定判斷</b>（{@code ActionBarListener#collect}）：選項要連續穩定
 *       700 毫秒才收。跑馬燈永遠不會停，所以永遠不會被收——這是對的。</li>
 *   <li><b>殘句判斷</b>（這裡測的）：漏網的殘句幾乎都是從單字中間切開的。
 *       Wynncraft 的選項都寫成句子，一律大寫或符號開頭。</li>
 * </ol>
 *
 * <p>第二道刻意做得簡單：它只是保險，判準複雜了反而會自己出錯。
 */
public final class DialogueChoiceTest {

    private static int failures = 0;

    public static void main(String[] args) {
        // 真正的選項：一律大寫或符號開頭
        for (String real : new String[] {
                "Just saying hello",
                "Who are you?",
                "What are you looking at?",
                "I don't remember anything from before you.",
                "[Leave]",
                "…nothing."}) {
            check("不誤擋真選項：" + real,
                  !ActionBarListener.looksClipped(real));
        }

        // 跑馬燈切出來的視窗：從單字中間開始
        for (String clipped : new String[] {
                "mber anything from before yo",
                "ber anything from before you",
                "er anything from before you",
                "so optimistic about being a"}) {
            check("擋得下殘句：" + clipped,
                  ActionBarListener.looksClipped(clipped));
        }

        check("空字串不當成殘句", !ActionBarListener.looksClipped(""));
        check("null 不當成殘句", !ActionBarListener.looksClipped(null));

        firstWindow();

        if (failures > 0) {
            System.out.println("對話選項：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("對話選項：全部通過");
    }

    /**
     * 跑馬燈的<b>第一格</b>也要擋。
     *
     * <h2>實機回報 2026-09-30</h2>
     * 使用者翻自己的 captured.json 時發現「太長、會動的句子讀不到完整的」。
     * 那一份裡 120 條選項有 <b>40 條</b>是切一半的，而且全部都是第一格——
     * 開頭好好的、尾巴被框寬切掉。
     *
     * <p>前面兩道都擋不住它：跑馬燈開始捲之前會停在第一格，停得比
     * 「停穩才收」那 700 毫秒還久；而原本的 looksClipped 只看開頭，
     * 第一格是大寫開頭。
     *
     * <p>下面的樣本全部來自那一份 captured.json，不是我編的。
     */
    private static void firstWindow() {
        // ★ 第一格：長度卡在框寬（27～28），從單字中間切開
        for (String window : new String[] {
                "Don't underestimate our stre",      // 28
                "How come one of the pedestal",      // 28
                "Do you know anything further",      // 28
                "Is this seriously the only w",      // 28
                "It improves mood and mental",       // 27
                "Why do you live in this town"}) {   // 28
            check("★ 擋得下第一格（" + window.length() + " 字元）：" + window,
                  ActionBarListener.looksClipped(window));
        }

        // 不可以誤擋：同一份 capture 裡真正的長選項，結尾一律是標點
        for (String real : new String[] {
                "Should you tell Gana the truth?",                   // 31
                "Give me a moment, actually.",                       // 27
                "Mrs. Fluffles doesn't look to be in the best of shape...",
                "Pick the first letter of the passcode:",
                "...Should you tell him about the prison?"}) {
            check("不誤擋真的長選項（" + real.length() + " 字元）：" + real,
                  !ActionBarListener.looksClipped(real));
        }

        // 短選項不管結尾是什麼都不該被當成視窗——框裝得下就不會捲
        for (String real : new String[] {
                "Leave her be", "She's dead", "General Information",
                "Mount Stats", "Close Book", "Amber for more information."}) {
            check("不誤擋短選項：" + real, !ActionBarListener.looksClipped(real));
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
