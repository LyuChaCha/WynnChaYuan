package com.wynnchayuan.render;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 對話句子<b>中間</b>夾著的圖示：{@code The elements are ✹ Fire, ❋ Air, …}。
 *
 * <h2>這種句子長什麼樣子</h2>
 * Detlas 士兵講元素的那幾句（PR #1435），每個元素詞前面有一個元素圖示，而且
 * 「圖示＋詞」各自是那個元素的顏色。圖示在對話框裡是另一套字型
 * （{@code hud/dialogue/text/common/body_N}）的<b>私用區字元</b> U+E000–U+E00A，
 * 不是聊天裡的 {@code ✹❋❉✤✦}：
 *
 * <pre>
 *   E000 風   E001 地   E002 火   E003 雷   E004 水
 * </pre>
 *
 * <h2>先前的樣子</h2>
 * {@link LineParts} 只把頭尾的圖示收成 {@code {#}}，夾在中間的直接丟掉——鍵於是
 * 長成 {@code The elements are  Fire,  Air, …}（圖示的位置剩兩個空格），譯文沒有
 * 辦法指出圖示該擺哪裡。貢獻者只好在譯文裡手寫 {@code ✹}，結果：
 * <ul>
 *   <li>就地取代整句不翻：{@code ✹} 不在對話字型的覆蓋範圍裡，守門把整句擋下
 *       （見 {@link DialogueRewriter#renderable}）。</li>
 *   <li>小框翻得出來，但整句白字：Wynntils 給的對話文字不帶顏色。</li>
 *   <li>{@code check-glyphs.py} 擋下：譯文用了被挪用成圖示的字元。</li>
 * </ul>
 *
 * <h2>現在的做法</h2>
 * 對話這三條路（收集、就地取代、小框）在算模板之前，先把句中的私用區圖示拆成
 * <b>自己一段</b>，{@link LineParts} 就會照常把它收成 {@code {#}}：
 *
 * <pre>
 *   鍵    The elements are {#} Fire, {#} Air, {#} Water, {#} Earth , and {#} Thunder.
 *   譯文  属性は {#}火{/}、{#}風{/}、{#}水{/}、{#}土{/}、{#}雷{/}があります。
 * </pre>
 *
 * {@code {#}} 填回原本那個圖示（就地取代用遊戲自己的字型畫，小框換成聊天那套
 * {@code ✹❋❉✤✦}），顏色是圖示自己的。{@code {#}} 後面到 {@code {/}} 為止的字
 * <b>跟著圖示上色</b>——原文的「圖示＋詞」本來就是同一個顏色，譯者不必知道
 * 那是哪個色碼。沒寫 {@code {/}} 就只有圖示自己有顏色。
 *
 * <h2>舊的鍵照樣查得到</h2>
 * 語料裡已經有幾十條是照舊格式（兩個空格）翻好的——King's Recruit 的施法教學、
 * 屬性點的說明。新格式查不到、舊格式查得到的時候就退回舊格式（見 {@link #parts}），
 * 那些句子跟以前一樣翻出來，只是沒有圖示。
 */
public final class DialogueIcons {

    private DialogueIcons() {}

    /**
     * 這個碼位是不是對話框裡的圖示：BMP 的私用區。
     *
     * <p>不收第 13 平面那一段（U+CF000–U+D1000）：那是排版位移與打字游標，
     * 照舊由 {@link LineParts} 頭尾的規則處理，收進來的話既有的鍵全部要改。
     */
    public static boolean isIcon(int cp) {
        return cp >= 0xE000 && cp <= 0xF8FF;
    }

    public static boolean hasIcon(String text) {
        return text != null && text.codePoints().anyMatch(DialogueIcons::isIcon);
    }

    /** 拿掉圖示字元，給「字型畫不畫得出來」那類只看字的檢查用。 */
    public static String strip(String text) {
        if (!hasIcon(text)) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints().filter(cp -> !isIcon(cp)).forEach(out::appendCodePoint);
        return out.toString();
    }

    // ---------------------------------------------------------- 圖示的樣式

    /**
     * 最近一幀對話裡每個圖示的樣式（字型與顏色）。
     *
     * <p>小框那條路拿到的是 Wynntils 攤平過的純文字，顏色與字型都沒了，只能從
     * action bar 的原始訊息讀——所以 {@code ActionBarListener} 在最高優先權就先
     * 記一份，Wynntils 解析對話、發出事件的時候這裡已經是同一幀的了。
     *
     * <p>同一個碼位在不同字型裡是不同的圖（common 與 currency 都從 E000 起），
     * 所以一幀有圖示就<b>整份換掉</b>，不跟舊的混在一起。
     */
    private static volatile Map<Integer, Style> seen = Map.of();

    /** 從 action bar 的原始訊息記下對話內文裡的圖示樣式。 */
    public static void note(Component message) {
        if (message == null) {
            return;
        }
        Map<Integer, Style> found = new HashMap<>();
        List<TextColor> colours = new ArrayList<>();
        List<TextColor> words = new ArrayList<>();
        boolean[] body = {false};
        message.visit((style, text) -> {
            if (!bodyFont(style)) {
                return Optional.empty();
            }
            body[0] = true;
            if (hasIcon(text)) {
                text.codePoints().filter(DialogueIcons::isIcon)
                    .forEach(cp -> found.putIfAbsent(cp, style));
            }
            // 顏色編號：照物品說明那邊的規則（LineTranslator#palette／wordPalette），
            // 只是比的是顏色不是整個樣式——對話每一列的字型都不同，比樣式的話同一個
            // 顏色在第二列會變成另一個編號。
            if (hasContent(text) && !colours.contains(style.getColor())) {
                colours.add(style.getColor());
            }
            if (GlyphSplitter.hasLetter(text) && !words.contains(style.getColor())) {
                words.add(style.getColor());
            }
            return Optional.empty();
        }, Style.EMPTY);
        boolean changed = false;
        if (!found.isEmpty() && !found.equals(seen)) {
            seen = Map.copyOf(found);
            changed = true;
        }
        if (body[0] && (!colours.equals(palette) || !words.equals(wordPalette))) {
            palette = java.util.Collections.unmodifiableList(colours);
            wordPalette = java.util.Collections.unmodifiableList(words);
            changed = true;
        }
        if (changed) {
            generation++;
        }
    }

    /** 有實字的一段：不全是空白或排版／圖示字元。跟 LineTranslator#hasContent 同一條。 */
    private static boolean hasContent(String text) {
        return text.codePoints().anyMatch(cp -> !Character.isWhitespace(cp)
                && !GlyphSplitter.isGlyphCodePoint(cp));
    }

    /**
     * 最近一幀對話內文用到的顏色，依第一次出現的順序。{@code {c1}} 是第一個。
     * 沒有顏色的那一段記成 {@code null}（{@code {cN}} 指到它就是「不指定」）。
     */
    private static volatile List<TextColor> palette = List.of();

    /** 同 {@link #palette}，但只數有字母的段。{@code {w1}} 是第一個。 */
    private static volatile List<TextColor> wordPalette = List.of();

    /**
     * 記下來的圖示或顏色變了幾次。小框把譯文快取起來重用（見
     * {@code DialogueOverlay#settledSource}），而顏色是跟著打字一格一格冒出來的——
     * 第一次算的時候 {@code {c4}} 可能還沒有對象；這個數字變了就該重算一次。
     */
    private static volatile int generation = 0;

    public static int generation() {
        return generation;
    }

    /** 這一句原文的顏色編號（{@code {cN}} 用）。 */
    static List<TextColor> palette() {
        return palette;
    }

    /**
     * 譯文的 {@code at} 位置是不是顏色標記。
     *
     * @return {@code {長度, 種類}}：種類 0 是 {@code {/}}，1 是 {@code {cN}}／
     *         {@code {wN}}／{@code {c:…}}；不是標記時回 {@code null}
     */
    public static int[] colourToken(String text, int at) {
        if (text.startsWith("{/}", at)) {
            return new int[] {3, 0};
        }
        if (at + 3 > text.length() || text.charAt(at) != '{') {
            return null;
        }
        char kind = text.charAt(at + 1);
        if (kind != 'c' && kind != 'w') {
            return null;
        }
        int end = text.indexOf('}', at + 2);
        if (end < 0) {
            return null;
        }
        String body = text.substring(at + 2, end);
        if (body.length() == 1 && body.charAt(0) >= '1' && body.charAt(0) <= '9') {
            return new int[] {end + 1 - at, 1};
        }
        if (kind == 'c' && body.length() > 1 && body.charAt(0) == ':') {
            return new int[] {end + 1 - at, 1};
        }
        return null;
    }

    /**
     * 一個顏色標記（{@code {c2}}、{@code {w1}}、{@code {c:#FF55FF}}、{@code {c:gold}}）
     * 對到的顏色；對不到（編號超出範圍、寫錯）回 {@code null}，當作沒寫。
     */
    public static TextColor resolve(String token) {
        if (token == null || token.length() < 4) {
            return null;
        }
        String body = token.substring(2, token.length() - 1);
        if (body.length() == 1 && Character.isDigit(body.charAt(0))) {
            List<TextColor> from = token.charAt(1) == 'w' ? wordPalette : palette;
            int slot = body.charAt(0) - '1';
            return slot >= 0 && slot < from.size() ? from.get(slot) : null;
        }
        if (!body.startsWith(":")) {
            return null;
        }
        String spec = body.substring(1).strip();
        if (spec.startsWith("#")) {
            try {
                return TextColor.fromRgb(Integer.parseInt(spec.substring(1), 16));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        net.minecraft.ChatFormatting named = net.minecraft.ChatFormatting.getByName(spec);
        return named == null || named.getColor() == null ? null : TextColor.fromLegacyFormat(named);
    }

    /**
     * 小框那條路的譯文前處理：{@code {#}…{/}} 換成圖示的色碼（見 {@link #spans}），
     * {@code {cN}}／{@code {wN}} 換成這一句原文的色碼。
     *
     * <p>Wynntils 給小框的文字沒有顏色，{@link LineParts} 從那裡收的調色盤是空的，
     * {@code {c1}} 交給 {@code LineTranslator} 會對不到任何顏色——實機回報「認得是
     * 另一個框，但顏色沒變」（PR #1436）。所以先照 action bar 記下來的顏色換成
     * {@code {c:#RRGGBB}}；對不到的原樣留著，LineTranslator 會當作沒寫並把它吃掉。
     */
    public static String prepare(String dst, List<LineParts.Piece> glyphs) {
        if (dst == null) {
            return null;
        }
        String out = spans(dst, glyphs);
        if (out.indexOf('{') < 0) {
            return out;
        }
        StringBuilder done = new StringBuilder(out.length());
        int i = 0;
        while (i < out.length()) {
            int[] token = colourToken(out, i);
            if (token == null || token[1] == 0 || out.startsWith("{c:", i)) {
                int step = token == null ? 1 : token[0];
                done.append(out, i, i + step);
                i += step;
                continue;
            }
            String whole = out.substring(i, i + token[0]);
            TextColor colour = resolve(whole);
            done.append(colour == null ? whole
                    : String.format("{c:#%06X}", colour.getValue() & 0xFFFFFF));
            i += token[0];
        }
        return done.toString();
    }

    /**
     * 拿掉顏色標記。就地取代的選項列與 SHIFT 提示列不上色，但標記也絕不能原樣
     * 畫出來（PR #1436：畫面上直接印出「{c1}」）。
     */
    public static String stripMarks(String text) {
        if (!hasColourMarks(text)) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int[] token = colourToken(text, i);
            if (token != null) {
                i += token[0];
            } else {
                out.append(text.charAt(i++));
            }
        }
        return out.toString();
    }

    /** 譯文裡有沒有顏色標記（{@code {cN}}、{@code {wN}}、{@code {c:…}}、{@code {/}}）。 */
    public static boolean hasColourMarks(String dst) {
        if (dst == null || dst.indexOf('{') < 0) {
            return false;
        }
        for (int i = dst.indexOf('{'); i >= 0; i = dst.indexOf('{', i + 1)) {
            if (colourToken(dst, i) != null) {
                return true;
            }
        }
        return false;
    }

    /** 這幾段文字裡有沒有句中圖示收成的碎片——小框要不要換譯文寫法的依據。 */
    public static boolean iconic(LineParts parts) {
        for (LineParts.Piece glyph : parts.glyphs()) {
            if (hasIcon(glyph.text())) {
                return true;
            }
        }
        return false;
    }

    /** 測試用：直接指定某個圖示的樣式。 */
    static void noteForTest(int cp, Style style) {
        Map<Integer, Style> next = new HashMap<>(seen);
        next.put(cp, style);
        seen = Map.copyOf(next);
    }

    /** 測試用：清掉記下來的樣式。 */
    static void forget() {
        seen = Map.of();
        palette = List.of();
        wordPalette = List.of();
        generation++;
    }

    /** 這個圖示在遊戲裡的樣式；沒記到就回 {@code null}。 */
    static Style styleOf(int cp) {
        return seen.get(cp);
    }

    static TextColor colourOf(int cp) {
        Style style = seen.get(cp);
        return style == null ? null : style.getColor();
    }

    private static boolean bodyFont(Style style) {
        Identifier id = fontId(style);
        return id != null && id.getPath().startsWith("hud/dialogue/text/")
                && id.getPath().contains("/body_");
    }

    private static Identifier fontId(Style style) {
        return style != null && style.getFont() instanceof FontDescription.Resource r
                ? r.id() : null;
    }

    /**
     * 就地取代時，這個圖示畫在第幾列要用的樣式。
     *
     * <p>對話字型把列號烘進了 ascent（{@code body_0} 是 35、{@code body_1} 是 23…），
     * 中文重新折行之後圖示可能換了一列，字型就得跟著換成那一列的
     * {@code common/body_N}，不然圖示會留在原本那一列的高度。
     *
     * @param row 這一列的文字原本的樣式（{@code wynncraft/body_N}）
     * @return 樣式；沒記到這個圖示時回 {@code null}（呼叫端就不畫它）
     */
    static Style forRow(int cp, Style row) {
        Style icon = seen.get(cp);
        if (icon == null) {
            return null;
        }
        Identifier mine = fontId(icon);
        Identifier theirs = fontId(row);
        if (mine == null || theirs == null) {
            return icon;
        }
        String path = mine.getPath();
        int at = path.lastIndexOf("/body_");
        int rowAt = theirs.getPath().lastIndexOf("/body_");
        if (at < 0 || rowAt < 0) {
            return icon;
        }
        String moved = path.substring(0, at) + theirs.getPath().substring(rowAt);
        return icon.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath(mine.getNamespace(), moved)));
    }

    // ---------------------------------------------------------- 模板

    /**
     * 拆圖示時給圖示那一段的記號字型——只為了讓它跟旁邊的字<b>不同樣式</b>，
     * 不然 {@link StyledText} 會把兩段併回一段。沒記到真正的樣式時才用。
     */
    private static final FontDescription MARK = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("wynnchayuan", "dialogue_icon"));

    /**
     * 把句中的圖示拆成自己一段，{@link LineParts} 才會把它收成 {@code {#}}。
     *
     * <p>圖示那一段帶著遊戲裡的樣式（記得到的話），小框填回去時就有顏色。
     */
    static StyledText split(String raw) {
        MutableComponent out = Component.empty();
        StringBuilder run = new StringBuilder();
        boolean icons = false;
        int i = 0;
        while (i < raw.length()) {
            int cp = raw.codePointAt(i);
            boolean icon = isIcon(cp);
            if (icon != icons || (icon && !run.isEmpty()
                    && !java.util.Objects.equals(styleOf(run.codePointAt(0)), styleOf(cp)))) {
                emit(out, run, icons);
                icons = icon;
            }
            run.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        emit(out, run, icons);
        return StyledText.fromComponent(out);
    }

    private static void emit(MutableComponent out, StringBuilder run, boolean icons) {
        if (run.isEmpty()) {
            return;
        }
        Style style = Style.EMPTY;
        if (icons) {
            Style known = styleOf(run.codePointAt(0));
            style = known != null ? known : Style.EMPTY.withFont(MARK);
        }
        out.append(Component.literal(run.toString()).withStyle(style));
        run.setLength(0);
    }

    /**
     * 對話這一句要拿哪一份模板去查。
     *
     * <p>句中有圖示時先用新格式（圖示收成 {@code {#}}）。語料不認得新格式、卻認得
     * 舊格式（圖示丟掉、留兩個空格）的話，退回舊格式——那幾十條既有的譯文才
     * 不會因為這次改動消失。兩份都不認得時用新格式：收集端記下來的就是新的。
     *
     * <p>「認得」包含「是某一條的開頭」：打字打到一半的時候整句還查不到，
     * 要靠前綴才分得出該走哪一份。
     *
     * @param store {@code null} 時一律用新格式
     */
    public static LineParts parts(String raw, TranslationStore store) {
        return LineParts.of(line(raw, store));
    }

    /** 同 {@link #parts}，但回傳拿來算模板的那一份文字。 */
    public static StyledText line(String raw, TranslationStore store) {
        StyledText legacy = StyledText.fromString(raw);
        if (!hasIcon(raw)) {
            return legacy;
        }
        StyledText icons = split(raw);
        if (store == null) {
            return icons;
        }
        String iconsT = LineParts.of(icons).template();
        String legacyT = LineParts.of(legacy).template();
        if (iconsT.equals(legacyT) || known(store, iconsT)) {
            return icons;
        }
        return known(store, legacyT) ? legacy : icons;
    }

    private static boolean known(TranslationStore store, String template) {
        String bare = template.strip();
        return store.lookup(bare) != null || store.hasLonger(bare);
    }

    // ---------------------------------------------------------- 譯文

    /**
     * 小框那條路：把「{@code {#}} 之後到 {@code {/}}」換成 {@link LineTranslator}
     * 認得的 {@code {c:#色碼}}，顏色取那個圖示自己的。
     *
     * <p>{@code {#}} 跟 {@code {/}} 中間又出現 {@code {#}} 或別的顏色佔位符時不動——
     * 那不是這種寫法。
     *
     * @param glyphs 這一行的 {@code {#}} 依序對應的碎片（{@link LineParts#glyphs}）
     */
    public static String spans(String dst, List<LineParts.Piece> glyphs) {
        if (dst == null || !dst.contains(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            return dst;
        }
        StringBuilder out = new StringBuilder(dst.length() + 32);
        int glyph = 0;
        int i = 0;
        while (i < dst.length()) {
            if (!dst.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER, i)) {
                out.append(dst.charAt(i++));
                continue;
            }
            i += GlyphSplitter.GLYPH_PLACEHOLDER.length();
            out.append(GlyphSplitter.GLYPH_PLACEHOLDER);
            LineParts.Piece piece = glyph < glyphs.size() ? glyphs.get(glyph) : null;
            glyph++;
            TextColor colour = piece == null || !hasIcon(piece.text()) || piece.style() == null
                    ? null : piece.style().getColor();
            if (colour != null && closes(dst, i) > 0) {
                out.append(String.format("{c:#%06X}", colour.getValue() & 0xFFFFFF));
            }
        }
        return out.toString();
    }

    /**
     * 從 {@code from} 起，這一段跟著圖示上色的字到哪裡結束：下一個 {@code {/}} 的位置。
     * 中間先遇到 {@code {#}}、別的顏色佔位符、或換行時不算（回 -1）；後面緊接著就是
     * {@code {/}}（沒有字可上色）也回 -1。
     */
    static int closes(String dst, int from) {
        int end = dst.indexOf("{/}", from);
        if (end <= from) {
            return -1;
        }
        String between = dst.substring(from, end);
        if (between.contains(GlyphSplitter.GLYPH_PLACEHOLDER) || between.contains("{c")
                || between.contains("{w") || between.indexOf('\n') >= 0) {
            return -1;
        }
        return end;
    }

    /**
     * 小框畫的是預設字型，對話字型的私用區圖示在那裡是別的圖。換成聊天那套
     * 被 Wynncraft 挪用成同一張圖的字元；不認得的圖示拿掉（跟以前一樣不畫）。
     *
     * <p>只動<b>我們拆出來的</b>那幾段（帶對話字型或 {@link #MARK} 的），
     * 別的碎片原樣保留。
     */
    public static Component forPanel(Component line) {
        if (line == null) {
            return null;
        }
        boolean[] touched = {false};
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            if (!hasIcon(text) || !ours(style)) {
                out.append(Component.literal(text).withStyle(style));
                return Optional.empty();
            }
            touched[0] = true;
            StringBuilder keep = new StringBuilder();
            Style plain = style.withFont(FontDescription.DEFAULT);
            text.codePoints().forEach(cp -> {
                if (!isIcon(cp)) {
                    keep.appendCodePoint(cp);
                    return;
                }
                String shown = chatGlyph(cp, style);
                if (shown != null) {
                    keep.append(shown);
                }
            });
            if (!keep.isEmpty()) {
                out.append(Component.literal(keep.toString()).withStyle(plain));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return touched[0] ? out : line;
    }

    private static boolean ours(Style style) {
        Identifier id = fontId(style);
        return id != null && (id.equals(((FontDescription.Resource) MARK).id())
                || id.getPath().startsWith("hud/dialogue/text/"));
    }

    /**
     * 對話字型 {@code common} 的五個元素圖示，在預設字型裡對應的字元。
     *
     * <p>對照是照資源包的 {@code font/hud/dialogue/text/common.png} 一格一格看的；
     * 預設字型那邊 {@code ✹❋❉✤✦} 被 Wynncraft 指到同一套元素圖上（小框的實機截圖
     * 就是這樣畫出來的）。其他幾格（心、劍、盾…）沒有把握對到哪個字元，先不畫。
     */
    private static final Map<Integer, String> CHAT = Map.of(
            0xE000, "❋",   // ❋ 風
            0xE001, "✤",   // ✤ 地
            0xE002, "✹",   // ✹ 火
            0xE003, "✦",   // ✦ 雷
            0xE004, "❉");  // ❉ 水

    static String chatGlyph(int cp, Style style) {
        Identifier id = fontId(style);
        boolean common = id == null || id.getPath().contains("/common/")
                || id.equals(((FontDescription.Resource) MARK).id());
        return common ? CHAT.get(cp) : null;
    }
}
