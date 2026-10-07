package com.wynnchayuan.client.ui;

/**
 * 設定畫面用到的繪製動作，全部就這幾個。
 *
 * <h2>為什麼要隔一層</h2>
 * 設定畫面的版面、命中判定與動畫全部寫在 {@link SettingsView} 裡，而它只認得這個
 * 介面，不認得 {@code GuiGraphics}。這樣同一份程式可以接兩種畫布：
 *
 * <ul>
 *   <li>遊戲裡接 {@link GuiCanvas}，畫到螢幕上；</li>
 *   <li>測試裡接一張記憶體裡的圖，<b>不開遊戲</b>就能把整個畫面算出來存成 PNG，
 *       也能直接量每個控制項有沒有疊在一起、有沒有跑出框外。</li>
 * </ul>
 *
 * 舊版的設定畫面漂過好幾次（按鈕、說明、卡片各算各的座標），每次都是使用者截圖
 * 回報才知道。畫面算得出來，這類問題在送出去之前就看得到。
 *
 * <p>座標一律是 GUI 像素，顏色一律是 <b>ARGB</b>——alpha 是 0 的話字會照常排版
 * 但完全看不到，見 {@code render.Colors} 的說明。
 */
public interface Canvas {

    /** 實心矩形，{@code x1}／{@code y1} 不含。 */
    void fill(int x0, int y0, int x1, int y1, int argb);

    /** 一行字，不帶陰影。 */
    void text(String text, int x, int y, int argb);

    /** 這行字畫出來多寬。 */
    int width(String text);

    /** 之後的繪製只留在這個範圍裡；可以巢狀，記得成對呼叫 {@link #unclip}。 */
    void clip(int x0, int y0, int x1, int y1);

    void unclip();

    /** 模組圖示，畫成 {@code size} 見方。 */
    void logo(int x, int y, int size);
}
