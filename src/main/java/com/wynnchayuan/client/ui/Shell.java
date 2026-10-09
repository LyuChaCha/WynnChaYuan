package com.wynnchayuan.client.ui;

/**
 * 每個畫面都要跟外面問的那幾件事：顏色、字、聲音。
 *
 * <p>畫面本身不認得遊戲（見 {@link Surface}），這些由遊戲那一側實作一份、
 * 測試那一側實作一份。
 */
public interface Shell {

    /** 風格顏色（ARGB）：畫面自己的重點色。 */
    int accent();

    /** 框線顏色（ARGB）：遊戲裡譯文小框的框。 */
    int frame();

    /** 視窗底下有沒有模糊。沒有的話玻璃要不透明一點，不然字讀不清楚。 */
    boolean blurred();

    /** 介面字串。鍵不含 {@code wynnchayuan.} 前綴。 */
    String tr(String key, Object... args);

    /** 介面現在是哪一種語言——字型照這個挑。 */
    String language();

    /** 按下去的那一聲。 */
    void click();
}
