package com.wynnchayuan.client;

/**
 * F6 設定畫面的版面算式。
 *
 * <h2>為什麼要抽出來</h2>
 * 舊版的版面是三條各自獨立的手算公式散在兩個方法裡（按鈕一條、說明一條、
 * 卡片高度一條），結果早就漂掉了——說明印在錯的列底下，最後幾列根本沒有說明。
 * 那種錯<b>看不出來也測不到</b>，因為它藏在畫圖的程式碼裡。
 *
 * <p>抽成一個只吃「畫面多寬多高、有幾列」的類別之後，每一個邊界都是純算術，
 * 可以拿各種畫面尺寸掃一遍，證明沒有東西會凸出去。見 {@code SettingsLayoutTest}。
 *
 * <p>所有數字都是<b>畫面座標</b>，不是相對座標——相對座標要在腦子裡加總才知道
 * 會不會撞到，那正是舊版出事的地方。
 */
final class SettingsLayout {

    /** 左邊分類欄的寬度。 */
    static final int TAB_W = 96;

    /** 右邊清單想要多寬。畫面窄的時候會自己縮。 */
    static final int PANE_W = 330;

    /** 分類欄與清單之間、以及清單左右內距。 */
    static final int GAP = 8;

    /** 一列的間距：控制項 20 + 呼吸空間 4。 */
    static final int ROW_H = 24;

    /** 控制項那一半想要多寬。畫面窄的時候會讓給名稱。 */
    static final int CTRL_W = 156;

    /** 名稱那一半至少要留多寬（約八個中文字）。 */
    static final int NAME_MIN = 76;

    /** 控制項最窄縮到多少。再窄下去「原文加譯文」那種字就塞不進去了。 */
    static final int CTRL_MIN = 96;

    /** 標題列高度，見 {@link Cards#header}。 */
    static final int HEADER_H = 47;

    /** 清單從哪裡開始。 */
    static final int TOP = HEADER_H + 12;

    /** 底下留給說明條與按鈕列的高度。 */
    static final int FOOT_H = 56;

    /** 要捲動時，清單底下那行「滾輪捲動 1–5 / 8」佔的高度。 */
    static final int CAPTION_H = 12;

    /** 分類有幾個。卡片高度要照這個算。 */
    static final int TABS = 5;

    private final int width;
    private final int height;
    private final int rows;

    SettingsLayout(int width, int height, int rows) {
        this.width = width;
        this.height = height;
        this.rows = rows;
    }

    // ------------------------------------------------------------ 橫向

    int paneW() {
        return Math.min(PANE_W, width - TAB_W - GAP * 3);
    }

    int originX() {
        return (width - (TAB_W + GAP + paneW())) / 2;
    }

    int paneX() {
        return originX() + TAB_W + GAP;
    }

    /**
     * 控制項那一半實際上有多寬。
     *
     * <p>畫面窄的時候先縮控制項，把寬度讓給名稱——名稱是中文，一個字九像素，
     * 擠不下就會凸出格子外。
     */
    int ctrlW() {
        return Math.max(CTRL_MIN, Math.min(CTRL_W, paneW() - NAME_MIN));
    }

    /** 名稱那一半有多寬。 */
    int nameW() {
        return paneW() - ctrlW();
    }

    /** 控制項的左緣（畫面座標）。 */
    int ctrlX() {
        return paneX() + paneW() - ctrlW();
    }

    // ------------------------------------------------------------ 縱向

    /**
     * 清單放得下幾列。
     *
     * <p>要捲動的時候底下會多印一行提示，那一行也要有位置，不然它會壓到說明條。
     * <b>但只有真的要捲時才讓</b>——不然放得下的分類會平白少一列。
     */
    int perPage() {
        int room = Math.max(1, (height - FOOT_H - TOP) / ROW_H);
        if (rows <= room) {
            return room;
        }
        return Math.max(1, (height - FOOT_H - TOP - CAPTION_H) / ROW_H);
    }

    /** 需不需要捲動。 */
    boolean scrolls() {
        return rows > perPage();
    }

    /** 清單裡看得到幾列。 */
    int shown() {
        return Math.min(rows, perPage());
    }

    /** 第 {@code at} 個看得到的位置在哪一條 y（畫面座標）。 */
    int rowY(int at) {
        return TOP + at * ROW_H;
    }

    /** 清單卡片的高度。 */
    int listH() {
        return shown() * ROW_H;
    }

    /** 清單卡片的底邊。 */
    int listBottom() {
        return TOP - 6 + listH() + 4;
    }

    /** 分類卡片的底邊。 */
    int tabsBottom() {
        return TOP - 6 + TABS * ROW_H + 4;
    }

    /** 捲動提示那一行的 y。 */
    int captionY() {
        return TOP + listH() + 2;
    }

    /** 底部說明條的 y。 */
    int footerY() {
        return height - FOOT_H + 8;
    }

    int footerW() {
        return TAB_W + GAP + paneW() + 8;
    }

    /** 最底下那兩顆按鈕的 y。 */
    int buttonsY() {
        return height - 26;
    }
}
