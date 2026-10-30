package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig.PanelSide;

public final class PanelLayoutTest {

    private static int failures = 0;

    private static final int FRAME = OriginalTooltipBox.FRAME;

    public static void main(String[] args) {
        TooltipPanel.Place p = layout(null, 200, 12);
        int gap = (212 - FRAME) - (p.panelX() + 150 + FRAME);
        check("★ 兩個框最外圈之間剛好是設定的 12px（實際 " + gap + "）", gap == 12);
        check("沒有壓到", !p.overlaps());

        OriginalTooltipBox.Box moved = new OriginalTooltipBox.Box(260, 40, 180, 260, 1f, 180, 260);
        p = layout(moved, 200, 12);
        check("★ 第一列對齊原文實際畫的位置（實際 y=" + p.panelY() + "）", p.panelY() == 40);
        check("★ 左右照原文實際畫的位置（實際 x=" + p.panelX() + "）",
                p.panelX() == 260 - FRAME - 12 - FRAME - 150);

        OriginalTooltipBox.Box half = new OriginalTooltipBox.Box(100, 20, 90, 130, 0.5f, 180, 260);
        p = layout(half, 300, 12);
        check("縮小的原文：框線跟著縮（實際 x=" + p.panelX() + "）",
                p.panelX() == 100 + 90 + 6 + 12 + FRAME);

        p = layout(null, 200, 0);
        check("框線碰到不算壓到", !p.overlaps());

        for (int value : new int[] {0, 4, 12, 16, 24, 40, 100, 200, 500}) {
            int shown = com.wynnchayuan.PanelGap.clamp(value);
            p = TooltipPanel.layout(150, 200, 180, 260, null, 600, 150, 2000, 720,
                    com.wynnchayuan.PanelGap.layout(value), PanelSide.AUTO);
            int outer = (612 - FRAME) - (p.panelX() + 150 + FRAME);
            int visible = com.wynnchayuan.PanelGap.visible(outer);
            check("★ 設定 " + value + "：遊戲裡的間距 " + visible + " 等於滑桿上的 " + shown,
                    visible == shown);
            check("★ 設定 " + value + "：預覽畫的間距等於遊戲裡的 "
                            + com.wynnchayuan.PanelGap.preview(shown, 1000),
                    com.wynnchayuan.PanelGap.preview(shown, 1000) == visible);
        }
        check("最小 12、預設 12", com.wynnchayuan.PanelGap.MIN == 12
                && com.wynnchayuan.PanelGap.DEFAULT == 12);

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("PanelLayoutTest 全部通過");
    }

    private static TooltipPanel.Place layout(OriginalTooltipBox.Box drawn, int mouseX, int gap) {
        return TooltipPanel.layout(150, 200, 180, 260, drawn, mouseX, 150, 640, 360, gap, PanelSide.AUTO);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
