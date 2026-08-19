package com.wynnchayuan.translate;

import com.wynntils.core.text.PartStyle;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

/**
 * Wynncraft 用來對齊的「空白字元」編解碼。
 *
 * <h2>對齊是怎麼做的</h2>
 * tooltip 裡「標籤靠左、數值靠右」的排版不是用空格湊出來的，而是插入一個
 * <b>帶寬度的不可見字元</b>：{@code minecraft:space} 字型把碼位
 * {@code U+D0000 + n} 對應成寬度 {@code n} 像素的空白。
 *
 * <p>所以 {@code "Health Regen" + <U+D0000+37> + "-171%"} 的意思是
 * 「標籤之後空 37 像素再接數值」。
 *
 * <h2>為什麼翻譯會讓版面跑掉</h2>
 * 那個 37 是<b>按英文標籤的寬度算好的</b>。把 {@code Health Regen}（約 62px）
 * 換成「生命回復」（約 40px）之後，標籤短了 22px，但空白仍然是 37px，
 * 數值就整個往左移了 22px——這就是欄位對不齊的原因。
 *
 * <p>解法是把差額加回空白字元：{@code 37 + 22 = 59}，重新編碼成
 * {@code U+D0000+59}，數值就回到原位。
 */
public final class SpaceOffset {

    /** 寬度 0 的基準碼位。{@code U+D0000 + n} 即為 n 像素寬。 */
    private static final int ZERO = 0xD0000;

    private SpaceOffset() {}

    /** 這個樣式是不是對齊用的空白字型。 */
    public static boolean isSpaceFont(PartStyle style) {
        if (style == null) {
            return false;
        }
        FontDescription font = style.getFont();
        if (!(font instanceof FontDescription.Resource resource) || resource.id() == null) {
            return false;
        }
        return "space".equals(resource.id().getPath());
    }

    /** 同上，給已經拿到 {@link Style} 的呼叫端用。 */
    public static boolean isSpaceFont(Style style) {
        if (style == null) {
            return false;
        }
        FontDescription font = style.getFont();
        if (!(font instanceof FontDescription.Resource resource) || resource.id() == null) {
            return false;
        }
        return "space".equals(resource.id().getPath());
    }

    /**
     * 給「自己插入」的對齊字元用的樣式。
     *
     * <p>{@code U+D0000+n} 只有在 {@code minecraft:space} 字型底下才畫得出來。
     * 沿用旁邊文字片段的樣式（通常是 {@code wynntils:language}）會找不到字形，
     * 畫面上就是一個方框。
     */
    public static Style styleFor(Style base) {
        return (base == null ? Style.EMPTY : base)
                .withFont(new FontDescription.Resource(
                        net.minecraft.resources.Identifier.withDefaultNamespace("space")));
    }

    /** 解出這段空白字元代表的總寬度（像素）。 */
    public static int decode(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            int px = cp - ZERO;
            if (px >= 0 && px <= 0xFFFF) {      // 落在這個字型的範圍內才算
                total += px;
            }
        }
        return total;
    }

    /**
     * 編回一個指定寬度的空白字元。
     *
     * @return 寬度 &lt;= 0 時回傳空字串（負寬度沒辦法用這個字型表達）
     */
    public static String encode(int pixels) {
        if (pixels <= 0) {
            return "";
        }
        return new String(Character.toChars(ZERO + Math.min(pixels, 0xFFFF)));
    }
}
