package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig.PanelSide;

/**
 * 「另開面板」跟原文的相對位置（使用者 2026-10-24：兩個框疊了一截、上下沒對齊）。
 *
 * <p>間距照兩個框<b>畫出來的最外圈</b>算——Wynncraft 的資源包把框線畫到文字區外 12px；
 * 原文被別的模組挪過時，照它實際畫的位置。
 *
 * <p>面板文字區 150×200、原文文字區 180×260、畫面 640×360、游標 (200, 150)。
 */
public final class PanelLayoutTest {

    private static int failures = 0;

    private static final int FRAME = OriginalTooltipBox.FRAME;

    public static void main(String[] args) {
        // 原文照原版公式畫在游標右邊 212 → 面板放左邊
        TooltipPanel.Place p = layout(null, 200, 12);
        int gap = (212 - FRAME) - (p.panelX() + 150 + FRAME);
        check("★ 兩個框最外圈之間剛好是設定的 12px（實際 " + gap + "）", gap == 12);
        check("沒有壓到", !p.overlaps());

        // 原文被挪過（Wynntils 縮放、Wynnventory 讓位）：照它真正畫的位置
        OriginalTooltipBox.Box moved = new OriginalTooltipBox.Box(260, 40, 180, 260, 1f, 180, 260);
        p = layout(moved, 200, 12);
        check("★ 第一列對齊原文實際畫的位置（實際 y=" + p.panelY() + "）", p.panelY() == 40);
        check("★ 左右照原文實際畫的位置（實際 x=" + p.panelX() + "）",
                p.panelX() == 260 - FRAME - 12 - FRAME - 150);

        // 原文被縮成一半、畫在游標左邊（100–190，游標 300）→ 面板放右邊，原文的框線也跟著縮
        OriginalTooltipBox.Box half = new OriginalTooltipBox.Box(100, 20, 90, 130, 0.5f, 180, 260);
        p = layout(half, 300, 12);
        check("縮小的原文：框線跟著縮（實際 x=" + p.panelX() + "）",
                p.panelX() == 100 + 90 + 6 + 12 + FRAME);

        // 間距 0：只是框線碰在一起，不算壓到（推開原文會丟掉它的稀有度框）
        p = layout(null, 200, 0);
        check("框線碰到不算壓到", !p.overlaps());

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
