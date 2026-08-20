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

    /**
     * 允許的偏移範圍（像素）。
     *
     * <p>Wynncraft <b>也會用負偏移</b>——實際 tooltip 裡出現過 {@code U+CFFE7}
     * （比基準低 25，也就是往回 25 像素）。原本這裡只認 {@code >= 0}，
     * 負的一律當成 0，於是那些字元既解不出寬度、重新編碼又變成空字串。
     *
     * <p>範圍抓 ±1024：實際看到的都在 ±256 以內，留餘裕但不至於把
     * 隨機的未指派碼位誤認成偏移。
     */
    private static final int LIMIT = 1024;

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

    /**
     * 這一整段是不是純粹的寬度偏移，可以安全地重新編碼。
     *
     * <h2>為什麼不用「編解碼繞得回來」判斷</h2>
     * 先前是拿 {@code encode(decode(text)).equals(text)} 來判斷，那有兩個漏洞：
     *
     * <ul>
     *   <li><b>0 像素的偏移永遠判定失敗</b>——{@code encode(0)} 回傳空字串。
     *       未鑑定物品那三行置中就是被這個擋掉的：第一行的前導偏移剛好是 0，
     *       於是整個空白沒被認出來，置中補償完全沒跑。</li>
     *   <li><b>多個字元組成的偏移也失敗</b>——{@code decode} 會加總，
     *       {@code encode} 只吐一個字元，字串當然對不起來。配方那種
     *       「一行兩欄」的間隔就是多字元的，所以第二欄一直沒對齊。</li>
     * </ul>
     *
     * <p>改成看<b>每個碼位是不是都落在偏移範圍內</b>。字型已經是
     * {@code minecraft:space} 了，範圍內的碼位依定義就是寬度偏移，
     * 不會是有意義的圖示。重新編碼只需要總寬度相同，字元數不必相同。
     */
    public static boolean isOffsetRun(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            int px = cp - ZERO;
            if (px < -LIMIT || px > LIMIT) {
                return false;
            }
        }
        return true;
    }

    /**
     * 這段文字尾端連續的寬度偏移。
     *
     * <p>Wynncraft 有時把欄位間隔<b>接在標籤後面同一個片段裡</b>，例如
     * {@code "Durability" + U+CFFD7 + U+D0091}（往回 41 再往前 145）。
     * 那是不折不扣的欄位間隔，只是沒有獨立成一個片段——不拆出來的話
     * 補償程式碰不到它，譯文變短時數值就會往左跑。
     *
     * @return 尾端的偏移字元；沒有就回傳空字串
     */
    public static String trailingOffsets(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int end = text.length();
        while (end > 0) {
            int cp = text.codePointBefore(end);
            int px = cp - ZERO;
            if (px < -LIMIT || px > LIMIT) {
                break;
            }
            end -= Character.charCount(cp);
        }
        return text.substring(end);
    }

    /** 解出這段空白字元代表的總寬度（像素）。負的也算，見 {@link #LIMIT}。 */
    public static int decode(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            int px = cp - ZERO;
            if (px >= -LIMIT && px <= LIMIT) {  // 落在這個字型的範圍內才算
                total += px;
            }
        }
        return total;
    }

    /**
     * 編回一個指定寬度的空白字元。
     *
     * <p>負寬度也編得出來（碼位落在基準之下），因為 Wynncraft 本來就在用。
     * 先前這裡對 {@code <= 0} 一律回傳空字串——那等於<b>把整個對齊空白刪掉</b>，
     * 畫面上就是數值突然黏到標籤旁邊，而且完全看不出是被刪掉的。
     *
     * @return 寬度剛好是 0 時回傳空字串（本來就不必畫）
     */
    public static String encode(int pixels) {
        if (pixels == 0) {
            return "";
        }
        int clamped = Math.max(-LIMIT, Math.min(pixels, LIMIT));
        return new String(Character.toChars(ZERO + clamped));
    }
}
