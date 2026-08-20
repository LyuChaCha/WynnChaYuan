package com.wynnchayuan;

import com.wynnchayuan.render.Colors;
import com.wynnchayuan.render.Fade;

/**
 * 驗證淡出的時間計算。
 *
 * <p>純算術，但錯了很難查：淡出只有幾百毫秒，畫面上只會覺得「怪怪的」，
 * 說不出哪裡怪。例如少了「淡完才回 0」這條，框會在還看得見的時候被清掉，
 * 看起來就是又閃了一下——跟原本要修的問題一模一樣。
 */
public final class FadeTest {

    private static int failures = 0;

    public static void main(String[] args) {
        long now = System.currentTimeMillis();

        // --- 停留期間 -------------------------------------------------------
        check("剛更新完是全不透明", Fade.alphaFor(now, 3000) == 1.0f);
        check("停留時間內維持全不透明", Fade.alphaFor(now - 2900, 3000) == 1.0f);

        // --- 淡出期間 -------------------------------------------------------
        float mid = Fade.alphaFor(now - 3175, 3000);
        check("停留結束後開始變淡（實際 " + mid + "）", mid > 0.0f && mid < 1.0f);
        check("越晚越淡",
                Fade.alphaFor(now - 3250, 3000) < Fade.alphaFor(now - 3050, 3000));

        // --- 淡完 -----------------------------------------------------------
        check("淡完之後才回 0", Fade.alphaFor(now - 4000, 3000) == 0.0f);
        check("淡出還沒完就不能回 0 —— 否則框會在看得見的時候被清掉",
                Fade.alphaFor(now - 3100, 3000) > 0.0f);

        // --- 持續顯示 -------------------------------------------------------
        check("設成持續顯示就永遠不淡",
                Fade.alphaFor(now - 999_999, Integer.MAX_VALUE) == 1.0f);

        // --- 換內容 ---------------------------------------------------------
        // NPC 講下一句時 lastUpdate 會被推後，透明度自然回到全滿。
        // 這是「換內容不淡出」唯一的機制，沒有額外狀態。
        check("更新時間一被推後就回到全不透明",
                Fade.alphaFor(now, 3000) == 1.0f);

        // --- 顏色縮放 -------------------------------------------------------
        check("alpha 減半", Colors.fade(0xFF203040, 0.5f) == 0x80203040);
        check("RGB 不受影響", (Colors.fade(0xFF203040, 0.25f) & 0xFFFFFF) == 0x203040);
        check("0 就是全透明", (Colors.fade(0xFFFFFFFF, 0f) >>> 24) == 0);
        check("超過 1 會夾回來", Colors.fade(0xFF203040, 5f) == 0xFF203040);

        System.out.println(failures == 0 ? "Fade: 全部通過" : "Fade: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
