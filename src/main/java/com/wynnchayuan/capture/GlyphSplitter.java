package com.wynnchayuan.capture;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 把一段遊戲文字拆成「可翻譯的文字」與「必須原樣保留的材質包符號」。
 *
 * <h2>為什麼不用逐字元判斷</h2>
 * 直覺做法是掃過每個字元、看它是不是私用區（PUA）字元就跳過。這樣做會壞，因為
 * 材質包符號和文字的界線<b>不在字元層級，而在樣式層級</b>：同一個碼位在不同字型
 * 下意義完全不同。
 *
 * <p>Wynncraft 送來的文字本來就已經分好了——符號一定帶著自己的 {@code font}
 * （{@code common}、{@code language/wynncraft} 等），文字則沒有。Wynntils 的
 * {@link StyledText} 把這個結構完整保留成一串 {@link StyledTextPart}，所以正確做法是
 * <b>整個 part 一起跳過</b>，而不是拆開裡面的字元。
 *
 * <p>這個假設是驗證過的：對照官方技能資料庫的 10,877 個片段，1,325 個帶 font 的片段
 * 含 0 個非 PUA 字元，9,552 個不帶 font 的片段含 0 個 PUA 字元。零重疊。
 *
 * <h2>三層判斷</h2>
 * <ol>
 *   <li>{@link #isCustomFont} —— 帶非預設字型的 part 直接整段跳過（主要機制）</li>
 *   <li>{@link #isGlyphPart} —— 沒有 font 但整段都是 PUA，以防伺服器改變送出方式</li>
 *   <li>{@link #stripGlyphChars} —— 清掉混在一般文字裡的零星 PUA
 *       （打字動畫的游標圖示就是這樣塞進來的）</li>
 * </ol>
 */
public final class GlyphSplitter {

    /** 抽取結果裡代表一個被略過的符號片段。 */
    public static final String GLYPH_PLACEHOLDER = "{#}";

    /** 代表一個執行期才決定的數值。 */
    public static final String NUMBER_PLACEHOLDER = "{~}";

    /** 代表一個地名。地名是專有名詞，永遠不翻譯，還原時原樣填回。 */
    public static final String PLACE_PLACEHOLDER = "{p}";

    /**
     * 代表<b>玩家自己的名字</b>。
     *
     * <h2>為什麼需要</h2>
     * Wynncraft 的任務對話會直接叫玩家的名字（{@code "Great job, Steve!"}）。
     * 那有兩個問題：收集起來會把名字寫進共享檔案，而且那條譯文只對那個人有用，
     * 別人永遠對不上。
     *
     * <p>先前的做法是<b>整行丟掉</b>——連帶把一大批任務對話一起丟了。
     * 改成參數化之後，一條譯文所有人通用，個資也不會外流。
     */
    public static final String PLAYER_PLACEHOLDER = "{u}";

    /** 數字（含小數、千分位、百分比），例如 {@code 28}、{@code 1,250}、{@code 0%}。 */
    private static final java.util.regex.Pattern NUMBERS =
            java.util.regex.Pattern.compile("\\d+(?:[.,]\\d+)*%?");

    /** 一般文字用的預設字型，帶這個不算符號。 */
    private static final Identifier DEFAULT_FONT = Identifier.withDefaultNamespace("default");

    private GlyphSplitter() {}

    /**
     * 單一片段是否為材質包符號（不可翻譯）。
     *
     * <h2>不能只看字型</h2>
     * 原本的規則是「帶自訂字型就當圖示」。那條規則對照官方技能 JSON 時是成立的
     * （那裡 {@code font: common} 的片段確實只含 PUA），但<b>對實際遊戲畫面不成立</b>：
     * Wynncraft 對<b>一般可讀文字</b>也套用自訂字型（語料的 lore HTML 裡就有
     * {@code font-ascii} 這種文字字型）。結果整個 tooltip 每一行都被當成圖示丟掉，
     * 翻譯一條也對不上。
     *
     * <p>所以改成同時看<b>字型與內容</b>——真正的圖示不會含有可讀字母：
     * <ol>
     *   <li>整段都是 PUA／未分配碼位 → 一定是圖示</li>
     *   <li>帶自訂字型<b>且</b>不含任何字母 → 圖示（涵蓋用 ASCII 碼位畫圖的字型）</li>
     *   <li>其餘一律視為可翻譯文字，即使它帶著自訂字型</li>
     * </ol>
     */
    public static boolean isGlyphPart(StyledTextPart part) {
        String text = part.getString(null, StyleType.NONE);
        if (text.isBlank()) {
            return false;                     // 純空白就是空白，不是圖示
        }
        if (isAllGlyphChars(text)) {
            return true;                      // PUA／未分配碼位
        }
        PartStyle style = part.getPartStyle();
        if (style == null || !isCustomFont(style.getFont())) {
            return false;
        }
        // 自訂字型底下，只有「連數字都沒有」才算圖示。
        // Wynncraft 的數值也用文字字型渲染（例如 +2 帶 wynntils:language），
        // 只看「有沒有字母」會把數值當成圖示，變成 {#} 而不是 {~}。
        return !hasReadable(text);
    }

    /** 是否含有可讀內容（字母或數字）。圖示兩者都不會有。 */
    private static boolean hasReadable(String s) {
        return s.codePoints().anyMatch(cp ->
                !isGlyphCodePoint(cp) && (Character.isLetter(cp) || Character.isDigit(cp)));
    }

    /**
     * 這個字型是不是「真的自訂字型」。
     *
     * <p>注意不能只寫 {@code font != null}。1.21.11 的一般文字帶的是
     * {@link FontDescription#DEFAULT}（非 null 的預設字型），寫成 null 判斷會把
     * <b>每一段普通文字都當成符號</b>，結果整段對話被過濾掉、一個字也收不到。
     *
     * <p>Wynntils 自己也是這樣判斷的——它在序列化樣式時明確排除
     * {@code minecraft:default}（見 {@code PartStyle#getString}）。
     */
    public static boolean isCustomFont(FontDescription font) {
        if (font == null) {
            return false;
        }
        if (font instanceof FontDescription.Resource resource) {
            Identifier id = resource.id();
            return id != null && !DEFAULT_FONT.equals(id);
        }
        // AtlasSprite / PlayerSprite 一定是圖示，不是文字
        return true;
    }

    /** 整段文字是否只由符號構成（完全沒有可翻譯內容）。 */
    public static boolean isGlyphOnly(StyledText text) {
        for (StyledTextPart part : text) {
            if (!isGlyphPart(part) && hasLetter(part.getString(null, StyleType.NONE))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 產生給譯者看的模板：符號換成 {@value #GLYPH_PLACEHOLDER}，文字原樣保留。
     *
     * <p>譯者永遠不會在工作檔裡看到 PUA 字元，也就不可能誤刪或誤改，
     * 而 {@code {#}} 的位置可以依中文語序自由調整。
     */
    public static String toTemplate(StyledText text) {
        // 轉呼叫 LineParts，讓收集端與顯示端保證算出同一個模板。
        // 兩邊各寫一份的話，只要有一點點差異（例如跨片段的數字切法不同），
        // 查表就會靜默落空，而且極難察覺。
        return LineParts.of(text).template();
    }

    /**
     * 把數字換成 {@value #NUMBER_PLACEHOLDER}。
     *
     * <p>{@code "- +28 Emeralds"} 與 {@code "- +30 Emeralds"} 是同一句文案的兩個實例，
     * 分開記錄會讓譯者翻同一句話翻好幾次。參數化之後兩者收斂成一條。
     */
    public static String parametrizeNumbers(String s) {
        return s.isEmpty() ? s : NUMBERS.matcher(s).replaceAll(NUMBER_PLACEHOLDER);
    }

    /** 給需要逐一取出數字（而非直接替換）的呼叫端用，例如要連同樣式一起記下來時。 */
    public static java.util.regex.Matcher numberMatcher(String s) {
        return NUMBERS.matcher(s);
    }

    /**
     * 移除混在一般文字片段裡的 PUA 字元。
     *
     * <p>大部分符號都自己帶 font、獨立成一個 part，由上面的 part 層級判斷處理掉。
     * 但打字動畫的游標圖示是<b>直接塞在文字片段裡</b>的（例如 {@code "H-hey"}），
     * part 層級抓不到，只能在字元層級清掉。
     *
     * <p>這裡用字元判斷是安全的：PUA 碼位在任何字型下都是圖示，永遠不會是要翻譯的內容。
     * （決定「一個 part 是不是符號」仍然要看樣式，不能只看字元——那是兩件事。）
     */
    public static String stripGlyphChars(String s) {
        if (s.codePoints().noneMatch(GlyphSplitter::isGlyphCodePoint)) {
            return s;                          // 常見情況，不用重建字串
        }
        StringBuilder out = new StringBuilder(s.length());
        s.codePoints()
         .filter(cp -> !isGlyphCodePoint(cp))
         .forEach(out::appendCodePoint);
        return out.toString();
    }

    /** 依序取出被略過的符號原文，供還原時填回。 */
    public static List<String> extractGlyphs(StyledText text) {
        List<String> glyphs = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        for (StyledTextPart part : text) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            if (isGlyphPart(part)) {
                run.append(raw);
            } else if (run.length() > 0) {
                glyphs.add(run.toString());
                run.setLength(0);
            }
        }
        if (run.length() > 0) {
            glyphs.add(run.toString());
        }
        return glyphs;
    }

    // ------------------------------------------------------------ 字元判斷

    /**
     * 這個碼位是不是材質包圖示。
     *
     * <p>不要用寫死的碼位範圍。原本以為圖示都放在私用區（PUA），實測發現
     * Wynncraft 的對話游標用的是 <b>U+D0000–U+D0074</b>——那是 Unicode 的第 13 平面，
     * <b>完全未分配</b>，不屬於任何 PUA 範圍。寫死範圍會整批漏掉。
     *
     * <p>改用 Unicode 屬性判斷就同時涵蓋兩者，也不必猜下次他們會用哪一段：
     * 真正的文字一定是「已分配」的字元，圖示則不是。
     */
    public static boolean isGlyphCodePoint(int cp) {
        int type = Character.getType(cp);
        return type == Character.PRIVATE_USE      // U+E000.. 等私用區
            || type == Character.UNASSIGNED;      // 第 13 平面等未分配碼位
    }

    /** 嚴格意義的私用區字元。保留給需要區分兩者的地方。 */
    public static boolean isPrivateUse(int cp) {
        return Character.getType(cp) == Character.PRIVATE_USE;
    }

    private static boolean isAllGlyphChars(String s) {
        if (s.isBlank()) {
            return false;
        }
        return s.codePoints()
                .filter(cp -> !Character.isWhitespace(cp))
                .allMatch(GlyphSplitter::isGlyphCodePoint);
    }

    /** 是否含有任何字母（拉丁與中日韓皆算），用來判斷「這段有沒有真的要翻的東西」。 */
    public static boolean hasLetter(String s) {
        return s.codePoints().anyMatch(cp -> Character.isLetter(cp) && !isGlyphCodePoint(cp));
    }
}
