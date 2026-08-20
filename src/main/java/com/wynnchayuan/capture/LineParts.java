package com.wynnchayuan.capture;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 一行遊戲文字拆解後的樣子：查詢用的模板，加上還原時要填回去的碎片。
 *
 * <p>翻譯的來回是這樣走的：
 *
 * <pre>
 *   原文  ◆Troms Citizen — Total Damage: 400%
 *   模板  {#}{p} Citizen — Total Damage: {~}      ← 拿這個去查字典
 *   查到  {#}{p} 市民 — 總傷害: {~}
 *   還原  ◆Troms 市民 — 總傷害: 400%              ← 符號、地名、數值填回原位
 * </pre>
 *
 * <p>三種佔位符都<b>連同各自的樣式一起記下來</b>：符號要帶原本的自訂字型才顯示得出來，
 * 地名與數值常常是另一個顏色。
 *
 * <h2>這是模板的唯一產生處</h2>
 * 收集端與顯示端<b>必須</b>算出一模一樣的模板，否則查表會靜默落空。
 * 所以 {@link GlyphSplitter#toTemplate} 也是轉呼叫這裡，不另外實作一份。
 */
public record LineParts(
        String template,
        List<Piece> glyphs,
        List<Piece> places,
        List<Piece> numbers,
        Style textStyle) {

    /** 一個要填回去的碎片：文字加上它原本的樣式。 */
    public record Piece(String text, Style style) {}

    /**
     * 拆解一行。
     *
     * <p>逐個 part 走，才能把每個碎片的樣式對應到它所在的片段——
     * 先把整行拼成字串再套正規表示式的話，樣式資訊就丟掉了。
     */
    /**
     * 這段文字開頭（或結尾）連續的符號字元。
     *
     * <p>只認符號碼位，空白不算——空白是內容的一部分，抽掉會讓
     * {@code "Tailoring "} 變成 {@code "Tailoring"}，兩者的語料鍵不同。
     */
    /**
     * 這段文字開頭連續的符號字元。
     *
     * <p>只認符號碼位，空白不算——空白是內容的一部分，抽掉會讓
     * {@code "Tailoring "} 變成 {@code "Tailoring"}，兩者的語料鍵不同。
     */
    private static String leadingGlyphs(String raw) {
        int n = 0;
        while (n < raw.length()) {
            int cp = raw.codePointAt(n);
            if (!GlyphSplitter.isGlyphCodePoint(cp)) {
                break;
            }
            n += Character.charCount(cp);
        }
        return raw.substring(0, n);
    }

    /**
     * 結尾連續的符號字元。
     *
     * @param floor 不要越過這個位置——開頭已經抽走的部分不能再被抽一次
     */
    private static String trailingGlyphs(String raw, int floor) {
        int end = raw.length();
        while (end > floor) {
            int cp = raw.codePointBefore(end);
            if (!GlyphSplitter.isGlyphCodePoint(cp)) {
                break;
            }
            end -= Character.charCount(cp);
        }
        return raw.substring(end);
    }

    public static LineParts of(StyledText line) {
        StringBuilder tmpl = new StringBuilder();
        List<Piece> glyphs = new ArrayList<>();
        List<Piece> places = new ArrayList<>();
        List<Piece> numbers = new ArrayList<>();
        Style textStyle = null;

        StringBuilder glyphRun = new StringBuilder();
        Style glyphRunStyle = null;

        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();

            if (GlyphSplitter.isGlyphPart(part)) {
                // 只合併「樣式相同」的連續符號。
                // 一行裡常常混用多種字型（space 排版 / banner/box 外框 /
                // tooltip/divider 分隔線…），全部併成一段只會套用第一個字型，
                // 其餘字元找不到對應字形就變成方框。
                if (!glyphRun.isEmpty() && !java.util.Objects.equals(glyphRunStyle, style)) {
                    glyphs.add(new Piece(glyphRun.toString(), glyphRunStyle));
                    glyphRun.setLength(0);
                }
                if (glyphRun.isEmpty()) {
                    glyphRunStyle = style;
                    tmpl.append(GlyphSplitter.GLYPH_PLACEHOLDER);
                }
                glyphRun.append(raw);
                continue;
            }
            if (!glyphRun.isEmpty()) {
                glyphs.add(new Piece(glyphRun.toString(), glyphRunStyle));
                glyphRun.setLength(0);
            }

            // 文字片段的<b>頭尾</b>可能黏著排版偏移字元，例如
            // {@code "Jeweling" + U+D0004 + U+D0034}。那些是排版寬度，
            // 先前被 stripGlyphChars 直接丟掉——譯文一換上去，間隔就整個消失。
            //
            // 抽成 {@code {#}} 交給 glyphs 保管，還原時才填得回去。
            // 只處理頭尾：夾在句子中間的圖示是內容的一部分，本來就不該進語料的鍵，
            // 而且抽出來會讓 {@code {#}} 卡在模板中間，既有的譯文全部對不上。
            String lead = leadingGlyphs(raw);
            // 從 lead 之後才往回找，整段都是符號時兩邊才不會重疊
            String tail = trailingGlyphs(raw, lead.length());
            String body = raw.substring(lead.length(), raw.length() - tail.length());

            if (!lead.isEmpty()) {
                tmpl.append(GlyphSplitter.GLYPH_PLACEHOLDER);
                glyphs.add(new Piece(lead, style));
            }
            String text = GlyphSplitter.stripGlyphChars(body);
            if (textStyle == null && !text.isBlank()) {
                textStyle = style;              // 譯文文字沿用第一段真正文字的樣式
            }
            appendParametrized(text, style, tmpl, places, numbers);
            if (!tail.isEmpty()) {
                tmpl.append(GlyphSplitter.GLYPH_PLACEHOLDER);
                glyphs.add(new Piece(tail, style));
            }
        }
        if (!glyphRun.isEmpty()) {
            glyphs.add(new Piece(glyphRun.toString(), glyphRunStyle));
        }

        return new LineParts(
                tmpl.toString().strip(),
                List.copyOf(glyphs),
                List.copyOf(places),
                List.copyOf(numbers),
                textStyle == null ? Style.EMPTY : textStyle);
    }

    /**
     * 把一段文字裡的地名與數字換成佔位符，同時記下原文與樣式。
     *
     * <p>地名先比對：它可能含數字（少見但存在），先換成 {@code {p}} 就不會被拆開。
     */
    private static void appendParametrized(String text, Style style, StringBuilder tmpl,
                                           List<Piece> places, List<Piece> numbers) {
        Matcher place = PlaceNames.matcher(text);
        int from = 0;
        if (place != null) {
            while (place.find()) {
                appendNumbers(text.substring(from, place.start()), style, tmpl, numbers);
                tmpl.append(GlyphSplitter.PLACE_PLACEHOLDER);
                places.add(new Piece(place.group(), style));
                from = place.end();
            }
        }
        appendNumbers(text.substring(from), style, tmpl, numbers);
    }

    private static void appendNumbers(String text, Style style,
                                      StringBuilder tmpl, List<Piece> numbers) {
        Matcher m = GlyphSplitter.numberMatcher(text);
        int from = 0;
        while (m.find()) {
            tmpl.append(text, from, m.start());
            tmpl.append(GlyphSplitter.NUMBER_PLACEHOLDER);
            numbers.add(new Piece(m.group(), style));
            from = m.end();
        }
        tmpl.append(text, from, text.length());
    }
}
