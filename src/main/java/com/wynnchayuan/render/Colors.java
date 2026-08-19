package com.wynnchayuan.render;

/**
 * 自己畫文字時用的色票。
 *
 * <h2>為什麼要有這個類別，而不是直接寫 0xFFFFFF</h2>
 * {@code GuiGraphics.drawString} 收的是 <b>ARGB</b>，不是 RGB。寫成
 * {@code 0xFFFFFF} 的 alpha 是 {@code 0x00}——文字會照常排版、照常佔位，
 * 但<b>完全透明</b>。畫面上看起來就是「框有出現，裡面是空的」。
 *
 * <p>更麻煩的是，這連帶影響有自己顏色的文字：{@code Font} 取樣式顏色時
 * 只拿 RGB，alpha 一律沿用這個參數。所以就算 {@code Component} 上掛了綠色，
 * 傳進來的 alpha 是 0，還是看不到。
 *
 * <p>{@link TooltipPanel} 沒踩到是因為它走原版 {@code renderTooltip}，
 * alpha 由原版自己補；只有我們自己 {@code drawString} 的地方會中招。
 *
 * <p>把顏色集中成常數之後，alpha 只有這裡要記得，新增繪製程式碼也不會再漏。
 */
public final class Colors {

    /** 一般內文。 */
    public static final int TEXT = 0xFFFFFFFF;

    /** 次要說明文字。 */
    public static final int HINT = 0xFF7A8794;

    /** 比 HINT 再淡一階，用於版本號、註腳。 */
    public static final int DIM = 0xFF808080;

    /** 副標題。 */
    public static final int SUBTLE = 0xFFA0A0A0;

    /** 最淡的註腳。 */
    public static final int FAINT = 0xFF707070;

    /** 分區標題的強調色（貢獻者頁）。 */
    public static final int HIGHLIGHT = 0xFFFFD24A;

    private Colors() {}

    /**
     * 補上不透明的 alpha。
     *
     * <p>給執行期算出來的顏色用——設定檔裡的色碼只有 RGB 六位。
     */
    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
