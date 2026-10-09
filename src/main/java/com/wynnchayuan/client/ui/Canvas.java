package com.wynnchayuan.client.ui;

/**
 * 設定畫面用到的繪製動作，全部就這幾個。
 *
 * <h2>為什麼要隔一層</h2>
 * 設定畫面的版面、命中判定與動畫全部寫在 {@link SettingsView} 裡，而它只認得這個
 * 介面，不認得 {@code GuiGraphics}。這樣同一份程式可以接兩種畫布：
 *
 * <ul>
 *   <li>遊戲裡接 {@code client.GuiCanvas}，畫到螢幕上；</li>
 *   <li>測試裡接一張記憶體裡的圖，<b>不開遊戲</b>就能把整個畫面算出來存成 PNG，
 *       也能直接量每個控制項有沒有疊在一起、有沒有跑出框外。</li>
 * </ul>
 *
 * 舊版的設定畫面漂過好幾次（按鈕、說明、卡片各算各的座標），每次都是使用者截圖
 * 回報才知道。畫面算得出來，這類問題在送出去之前就看得到。
 *
 * <h2>單位</h2>
 * 座標是 <b>GUI 像素</b>，但可以帶小數：介面縮放是 3 的時候，一個 GUI 像素是螢幕上
 * 三個點，圓角與細線要畫到那三個點的精度才不會一階一階的。{@link #px} 回傳螢幕上
 * 一個點是多少 GUI 像素，畫髮絲線的時候用它當粗細。
 *
 * <p>顏色一律是 <b>ARGB</b>——alpha 是 0 的話字會照常排版但完全看不到，
 * 見 {@code render.Colors} 的說明。
 */
public interface Canvas {

    /** 實心矩形，{@code x1}／{@code y1} 不含。 */
    void fill(float x0, float y0, float x1, float y1, int argb);

    /** 實心圓角矩形。{@code r} 比短邊的一半還大時自動縮成膠囊。 */
    void round(float x, float y, float w, float h, float r, int argb);

    /** 圓角矩形的框線，{@code t} 是線寬，往<b>裡面</b>長。 */
    void ring(float x, float y, float w, float h, float r, float t, int argb);

    /** 一行字，不帶陰影。 */
    void text(String text, float x, float y, int argb);

    /**
     * 放大的字。實際倍率會被貼齊成「螢幕上每個字點是整數個點」——點陣字放大到
     * 一點幾倍，筆畫會有的粗有的細。量寬度要用 {@link #scale} 問到的那個倍率。
     */
    void text(String text, float x, float y, int argb, float scale);

    /** 想要的倍率實際會畫成多少。 */
    float scale(float wanted);

    /** 這行字（一倍大）畫出來多寬。 */
    int width(String text);

    /**
     * 之後的字用哪一種語言的字型畫（{@code zh_tw}、{@code ja_jp}…）。
     *
     * <p>中日韓共用同一批漢字碼位，但各地的寫法不一樣（「骨」「直」「角」），
     * 所以字型是照語言分的。平常整個畫面一種就好；更新說明可以單獨切成別的語言，
     * 那一塊要換成那個語言的字型，不然簡體的說明會缺字。
     */
    void language(String lang);

    /** 玩家的頭像，畫成 {@code size} 見方。還沒抓到皮膚時是預設的那張臉。 */
    void head(String minecraftName, float x, float y, float size);

    /** 之後的繪製只留在這個範圍裡；可以巢狀，記得成對呼叫 {@link #unclip}。 */
    void clip(float x0, float y0, float x1, float y1);

    void unclip();

    /** 模組圖示，畫成 {@code size} 見方。 */
    void logo(float x, float y, float size);

    /** 線條圖示，畫成 {@code size} 見方。 */
    void icon(Icons.Icon icon, float x, float y, float size, int argb);

    /** 色盤的那一塊：左右是飽和、上下是明度。 */
    void svSquare(float x, float y, float w, float h, float hue);

    /** 色盤的色相條。 */
    void hueBar(float x, float y, float w, float h);

    /** 螢幕上一個點是多少 GUI 像素（介面縮放的倒數）。 */
    float px();
}
