package com.wynnchayuan;

import com.wynnchayuan.render.Colors;
import com.wynnchayuan.render.Fade;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class FadeTest {

    public static void main(String[] args) {
        long now = System.currentTimeMillis();

        check("剛更新完是全不透明", Fade.alphaFor(now, 3000) == 1.0f);
        check("停留時間內維持全不透明", Fade.alphaFor(now - 2900, 3000) == 1.0f);

        float mid = Fade.alphaFor(now - 3175, 3000);
        check("停留結束後開始變淡（實際 " + mid + "）", mid > 0.0f && mid < 1.0f);
        check("越晚越淡",
                Fade.alphaFor(now - 3250, 3000) < Fade.alphaFor(now - 3050, 3000));

        check("淡完之後才回 0", Fade.alphaFor(now - 4000, 3000) == 0.0f);
        check("淡出還沒完就不能回 0 —— 否則框會在看得見的時候被清掉",
                Fade.alphaFor(now - 3100, 3000) > 0.0f);

        check("設成持續顯示就永遠不淡",
                Fade.alphaFor(now - 999_999, Integer.MAX_VALUE) == 1.0f);

        check("更新時間一被推後就回到全不透明",
                Fade.alphaFor(now, 3000) == 1.0f);

        check("alpha 減半", Colors.fade(0xFF203040, 0.5f) == 0x80203040);
        check("RGB 不受影響", (Colors.fade(0xFF203040, 0.25f) & 0xFFFFFF) == 0x203040);
        check("0 就是全透明", (Colors.fade(0xFFFFFFFF, 0f) >>> 24) == 0);
        check("超過 1 會夾回來", Colors.fade(0xFF203040, 5f) == 0xFF203040);

        System.out.println(failures == 0 ? "Fade: 全部通過" : "Fade: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

}
