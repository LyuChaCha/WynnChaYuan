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
 * <h2>三道防線</h2>
 * <ol>
 *   <li><b>穩定判斷</b>（{@code ActionBarListener#collect}）：選項要連續穩定
 *       700 毫秒才收。捲動中的那幾格永遠不會停，所以不會從那裡被收。</li>
 *   <li><b>殘句判斷</b>（這裡測的）：漏網的殘句幾乎都是從單字中間切開的。
 *       Wynncraft 的選項都寫成句子，一律大寫或符號開頭。</li>
 *   <li><b>捲動狀態</b>（{@code com.wynnchayuan.capture.ChoiceScroll}）：接得上
 *       前面累積的視窗就是捲到一半的那一格。</li>
 * </ol>
 *
 * <p>第二道刻意做得簡單：它只是保險，判準複雜了反而會自己出錯。
 *
 * <h2>第一格要留</h2>
 * 2026-09-30 改過一次：<b>第一格不丟</b>。算繪端手上也只有視窗，查表用的鍵就是
 * 第一格（見 {@code render.Marquee}）——丟掉就等於讓長選項永遠不可能被翻。
 * 整句由 {@code ChoiceScroll} 接回來，放進條目的 {@code full} 欄位給譯者看。
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

        // 切在撇號、逗號上的視窗：不是小寫開頭，先前一路過關
        for (String clipped : new String[] {
                "'m not going to help you wit",
                ", but I never saw him again.",
                ". Nobody told me about that."}) {
            check("符號開頭的殘句也擋得下：" + clipped,
                  ActionBarListener.looksClipped(clipped));
        }

        // 這幾個是真的選項，符號開頭但不是切出來的
        for (String real : new String[] {"…nothing.", "[Leave]", "(Say nothing)"}) {
            check("不誤擋符號開頭的真選項：" + real,
                  !ActionBarListener.looksClipped(real));
        }

        check("空字串不當成殘句", !ActionBarListener.looksClipped(""));
        check("null 不當成殘句", !ActionBarListener.looksClipped(null));

        if (failures > 0) {
            System.out.println("對話選項：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("對話選項：全部通過");
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
