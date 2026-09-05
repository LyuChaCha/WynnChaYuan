package com.wynnchayuan.client;

/**
 * F6 設定畫面的版面，在任何畫面尺寸下都不能有東西凸出去或疊在一起。
 *
 * <h2>為什麼要有這條</h2>
 * 使用者回報「排版與方框不相同」，而舊版那個錯<b>看不出來也測不到</b>——
 * 版面是三條各自獨立的手算公式散在兩個畫圖的方法裡，說明早就印在錯的列底下、
 * 最後四列根本沒有說明，卻沒有任何東西會叫。
 *
 * <p>版面算式抽成 {@link SettingsLayout} 之後全部是純算術，可以把畫面尺寸
 * 從 Minecraft 的下限 320×240 一路掃到 4K，逐一驗每一個邊界。
 *
 * <h2>驗什麼</h2>
 * <ul>
 *   <li>名稱那一欄放得下最長的名稱（七個中文字）。</li>
 *   <li>控制項不會被縮到塞不下「原文加譯文」。</li>
 *   <li>兩張卡片、捲動提示、說明條、按鈕列由上到下<b>不重疊</b>。</li>
 *   <li>整個版面在畫面內，左右都不出界。</li>
 * </ul>
 */
public final class SettingsLayoutTest {

    private static int failures = 0;

    /** 最長的列名「對話／追蹤小框」：七個中文字，一個九像素。 */
    private static final int LONGEST_NAME = 7 * 9;

    /** 控制項上最長的字「原文加譯文」五個中文字，加左右內距。 */
    private static final int LONGEST_VALUE = 5 * 9 + 8;

    /** 一個分類最多幾列。現在最多是五列（面板、資料）。 */
    private static final int MAX_ROWS = 5;

    public static void main(String[] args) {
        // Minecraft 的下限是 320×240；上面掃到 4K。中間挑幾個常見的。
        int[] widths = {320, 400, 480, 640, 854, 1024, 1280, 1920, 3840};
        int[] heights = {240, 300, 360, 480, 540, 720, 1080, 2160};

        int checked = 0;
        for (int w : widths) {
            for (int h : heights) {
                // 一列到二十列都試——以後加設定不會忽然爆掉
                for (int rows = 1; rows <= 20; rows++) {
                    checked++;
                    verify(w, h, rows);
                }
            }
        }
        report("掃過 " + checked + " 種組合都沒有凸出或重疊", failures == 0);

        // 下限那一組特別列出來，數字看得見才知道有多緊
        SettingsLayout floor = new SettingsLayout(320, 240, MAX_ROWS);
        System.out.println("  320×240：名稱欄 " + floor.nameW() + "px、控制項 "
                + floor.ctrlW() + "px、列距 " + floor.rowH() + "px、放得下 "
                + floor.perPage() + " 列、卡片間距 " + floor.cardGap() + "px、"
                + (floor.scrolls() ? "要捲動" : "不用捲"));
        SettingsLayout normal = new SettingsLayout(854, 480, MAX_ROWS);
        System.out.println("  854×480：名稱欄 " + normal.nameW() + "px、控制項 "
                + normal.ctrlW() + "px、列距 " + normal.rowH() + "px、放得下 "
                + normal.perPage() + " 列、卡片間距 " + normal.cardGap() + "px");
        report("下限畫面放得下最多的那一類（" + floor.perPage() + " 列）",
                floor.perPage() >= MAX_ROWS);

        System.out.println(failures == 0
                ? "設定版面：全部通過" : "設定版面：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void verify(int w, int h, int rows) {
        SettingsLayout box = new SettingsLayout(w, h, rows);
        String at = w + "×" + h + " " + rows + " 列";

        // ---- 橫向 ----
        must(at + "：名稱欄放得下最長的名稱", box.nameW() >= LONGEST_NAME);
        must(at + "：控制項放得下最長的值", box.ctrlW() >= LONGEST_VALUE);
        must(at + "：左緣不出界", box.originX() >= 0);
        must(at + "：控制項在清單裡", box.ctrlX() + box.ctrlW()
                <= box.paneX() + box.paneW());
        must(at + "：說明條不出界",
                box.tabsCardX() >= 0 && box.tabsCardX() + box.footerW() <= w);

        // 兩張卡片不能疊在一起——使用者一眼看出來的就是這個。
        must(at + "：兩張卡片之間有空隙（實際 " + box.cardGap() + "px）",
                box.cardGap() >= 4);
        must(at + "：清單卡片不出界",
                box.listCardX() + box.listCardW() <= w);

        // ---- 縱向：由上到下不重疊 ----
        must(at + "：清單在標題列底下", box.tabsY() >= SettingsLayout.HEADER_H);
        must(at + "：清單卡片沒壓到說明條", box.listBottom() <= box.footerY());
        must(at + "：分類卡片沒壓到說明條", box.tabsBottom() <= box.footerY());
        must(at + "：說明條沒壓到按鈕列", box.footerY() + 20 <= box.buttonsY());
        must(at + "：按鈕列在畫面內", box.buttonsY() + 20 <= h);

        // 要捲動時，那行提示也要有位置
        if (box.scrolls()) {
            must(at + "：捲動提示沒壓到說明條", box.captionY() + 9 <= box.footerY());
        }

        // 最後一列整列都在卡片內
        int last = box.rowY(box.shown() - 1);
        must(at + "：最後一列在卡片內", last + 20 <= box.listBottom());
    }

    /** 只在第一次失敗時印出來——掃幾萬組，全部印會把真正的訊息淹掉。 */
    private static void must(String what, boolean ok) {
        if (!ok) {
            if (failures < 5) {
                System.out.println("  [FAIL] " + what);
            }
            failures++;
        }
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok && failures == 0) {
            failures++;
        }
    }
}
