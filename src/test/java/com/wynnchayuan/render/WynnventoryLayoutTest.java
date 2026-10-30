package com.wynnchayuan.render;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public final class WynnventoryLayoutTest {

    private static int failures = 0;

    public static void main(String[] args) {
        int ph = 49 * 10;
        float[] anchor = WynnventoryLayout.anchoredSpot(492, 170, ph);
        float s = anchor[2];
        float top = anchor[1] - 4 * s;
        float bottom = anchor[1] + (ph + 6) * s;
        check("★ 固定在左側：貼左邊（實際 x=" + anchor[0] + "）", anchor[0] < 12);
        check("★ 整個框在畫面裡（上 " + top + "、下 " + bottom + "）", top >= 0 && bottom <= 492);
        check("太高就縮（實際 " + s + "）", s < 1f);

        int[] box = {4, 10, 180, 470};
        float[] moved = WynnventoryLayout.moved(50, 190, 310, 220, box, 556, 1f);
        check("★ 說明框挪到價格框右邊（實際 " + (moved == null ? "null" : moved[0]) + "）",
              moved != null && moved[0] - 4 >= 4 + 180);
        check("沒蓋到就不挪", WynnventoryLayout.moved(300, 190, 200, 100, box, 556, 1f) == null);
        check("挪了會出畫面就不挪", WynnventoryLayout.moved(50, 190, 400, 100, box, 556, 1f) == null);

        float[] spot = WynnventoryLayout.place(1100, 984, 105, 380, 620, 310, 900);
        check("照實際寬度擺得下（實際 " + (spot == null ? "null" : "x=" + spot[0]) + "）", spot != null);
        if (spot != null) {
            check("不壓在說明框上", spot[0] - 4 * spot[2] >= 725);
        }
        float[] squeezed = WynnventoryLayout.place(1000, 984, 105, 380, 620, 310, 400);
        check("兩邊都放不下時縮小（實際 " + (squeezed == null ? "null" : squeezed[2]) + "）",
              squeezed != null && squeezed[2] < 1f && squeezed[0] - 4 * squeezed[2] >= 725
                      && squeezed[0] + (310 + 4) * squeezed[2] <= 1000);
        float[] roomy = WynnventoryLayout.place(1000, 600, 100, 50, 300, 200, 200);
        check("右邊放得下就放右邊、原尺寸（實際 x=" + roomy[0] + " 縮放 " + roomy[2] + "）",
              roomy[0] == 100 + 300 + 18 + 4 && roomy[2] == 1f);
        float[] lefty = WynnventoryLayout.place(1000, 600, 500, 50, 400, 200, 200);
        check("右邊放不下就放左邊（實際 x=" + lefty[0] + "）", lefty[0] + 200 + 4 <= 500 - 18 + 0.01f);
        float[] tall = WynnventoryLayout.place(1000, 300, 100, 50, 300, 200, 600);
        check("太高就縮，整個框在畫面裡",
              tall[2] < 1f && tall[1] - 4 * tall[2] >= 0 && tall[1] + 606 * tall[2] <= 300.01f);
        check("空間太小就交回它原本的方法",
              WynnventoryLayout.place(400, 600, 30, 50, 340, 300, 100) == null);

        ownPanelKeepsItsSpot();

        if (failures > 0) {
            System.out.println("Wynnventory 側邊框：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("Wynnventory 側邊框：全部通過");
    }

    private static void ownPanelKeepsItsSpot() {
        int screenW = 960;
        int screenH = 508;
        OriginalTooltipBox.Box drawn = new OriginalTooltipBox.Box(420, 120, 170, 202, 1f, 170, 202);
        TooltipPanel.Place p = TooltipPanel.layout(160, 212, 170, 202, drawn, 400, 132,
                screenW, screenH, 12, com.wynnchayuan.CollectorConfig.PanelSide.AUTO);
        WynnventoryBox.note(4, 150, 150, 200, System.nanoTime());
        try {
            OriginalTooltipBox.beginOwn();
            int[] at;
            try {
                at = drawnAt(p.panelX(), p.panelY(), screenW, screenH, 160, 212);
            } finally {
                OriginalTooltipBox.endOwn();
            }
            check("★ 有價格框時面板的第一列仍對齊原文（原文 y=120，面板 y=" + at[1] + "）",
                    at[1] == 120);
            check("★ 面板的 x 照算好的位置（算好 " + p.panelX() + "，畫在 " + at[0] + "）",
                    at[0] == p.panelX());

            int[] item = drawnAt(400, 132, screenW, screenH, 170, 202);
            Vector2ic plain = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                    screenW, screenH, 400, 132, 170, 202);
            check("物品說明照舊走原版定位再讓位（y=" + item[1] + "）", item[1] == plain.y());
        } finally {
            WynnventoryBox.forget();
        }
    }

    private static int[] drawnAt(int x, int y, int screenW, int screenH, int w, int h) {
        ClientTooltipPositioner requested = (sw, sh, mx, my, tw, th) -> new Vector2i(mx, my);
        ClientTooltipPositioner moved = WynnventoryLayout.shift(null, 1f);
        ClientTooltipPositioner used = moved != null ? moved : requested;
        Vector2ic at = used.positionTooltip(screenW, screenH, x, y, w, h);
        return new int[] {at.x(), at.y()};
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            System.out.println("  [ok] " + what);
        } else {
            System.out.println("  [FAIL] " + what);
            failures++;
        }
    }
}
