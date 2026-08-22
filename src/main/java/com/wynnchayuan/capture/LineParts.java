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
        /** 玩家自己的名字。參數化之後一條譯文所有人通用，見 GlyphSplitter#PLAYER_PLACEHOLDER。 */
        List<Piece> users,
        /**
         * 樣式與整行不同的文字片段，例如被畫上底線、單獨換色的技能名。
         *
         * <p>重建譯文時整行套用同一個 {@code textStyle}，行內的樣式變化會被抹平——
         * {@code Reduce the Mana cost of Bash.} 裡的 {@code Bash} 原本帶底線與專屬
         * 顏色，譯文出來就成了一片平的。
         *
         * <p>技能名、地名這類詞照慣例<b>保持英文</b>，也就是原樣出現在譯文裡，
         * 所以不必請譯者標記——比對得到就把原本的 {@link Style} 貼回去。
         * 這樣連非原版的自訂顏色都對，因為搬的是樣式物件本身而不是某個顏色碼。
         */
        List<Piece> accents,
        Style textStyle) {

    /**
     * 一個要填回去的碎片：文字加上它原本的樣式。
     *
     * @param ramp 這一段在原文裡是<b>一個字一個顏色</b>畫出來的（彩虹字），
     *             這裡就是那串顏色，順序即左到右。譯文的字數跟原文對不上，
     *             所以不能一個字配一個顏色，得把整串顏色<b>攤到譯文的長度上</b>——
     *             見 {@code LineTranslator.paint}。平常是 {@code null}。
     */
    public record Piece(String text, Style style, List<Style> ramp) {

        public Piece(String text, Style style) {
            this(text, style, null);
        }
    }

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
    /** 本機玩家的帳號名稱；取不到就回傳 null。 */
    private static String localPlayerName() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            String name = mc == null || mc.getUser() == null ? null : mc.getUser().getName();
            return name == null || name.isBlank() ? null : name;
        } catch (Throwable t) {
            return null;
        }
    }

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
        List<Piece> users = new ArrayList<>();
        List<Piece> runs = new ArrayList<>();

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
            if (!text.isBlank()) {
                runs.add(new Piece(text, style));
            }
            appendParametrized(text, style, tmpl, places, numbers, users);
            if (!tail.isEmpty()) {
                tmpl.append(GlyphSplitter.GLYPH_PLACEHOLDER);
                glyphs.add(new Piece(tail, style));
            }
        }
        if (!glyphRun.isEmpty()) {
            glyphs.add(new Piece(glyphRun.toString(), glyphRunStyle));
        }

        Style textStyle = mainStyle(runs);
        return new LineParts(
                tmpl.toString().strip(),
                List.copyOf(glyphs),
                List.copyOf(places),
                List.copyOf(numbers),
                List.copyOf(users),
                accents(runs, textStyle),
                textStyle);
    }

    /**
     * 整行文字的主樣式：<b>涵蓋字數最多</b>的那一個。
     *
     * <p>先前取的是「第一段真正文字」。句子以帶樣式的詞開頭時那就整個反了——
     * {@code Bash deals damage} 會讓整行都被畫上底線，剩下的才是例外。
     */
    private static Style mainStyle(List<Piece> runs) {
        Style best = null;
        int bestLen = -1;
        for (Piece candidate : runs) {
            int len = 0;
            for (Piece other : runs) {
                if (java.util.Objects.equals(candidate.style(), other.style())) {
                    len += other.text().length();
                }
            }
            if (len > bestLen) {
                bestLen = len;
                best = candidate.style();
            }
        }
        return best == null ? Style.EMPTY : best;
    }

    /**
     * 樣式與主樣式不同、而且<b>認得出來</b>的片段。
     *
     * <p>太短的不收：一兩個字元在譯文裡到處都比對得到，貼錯位置比沒有樣式更糟。
     * 純標點、純數字也不收——數字走 {@code {~}} 那條路，本來就會連樣式一起填回。
     */
    private static List<Piece> accents(List<Piece> runs, Style textStyle) {
        List<Piece> out = new ArrayList<>();
        for (Piece run : runs) {
            String text = run.text().strip();
            if (java.util.Objects.equals(run.style(), textStyle) || text.isEmpty()) {
                continue;
            }
            // 一般的詞要夠長才收——短的在譯文裡到處都比對得到，貼錯位置更糟。
            // 但<b>符號</b>不一樣：「✖」只有一個字元，卻是整行唯一的紅色，
            // 而且它原樣留在譯文裡，比對不會出錯。先前被長度擋掉，
            // 於是 ✖ 跟後面的標籤併成一段，紅色就沒了。
            boolean word = text.length() >= MIN_ACCENT_LENGTH && GlyphSplitter.hasLetter(text);
            if (word || isSymbolRun(text)) {
                out.add(new Piece(text, run.style()));
            }
        }
        return List.copyOf(out);
    }

    /** 整段都是符號（沒有字母也沒有數字），例如 {@code ✖}、{@code ✦}、{@code ⚔}。 */
    private static boolean isSymbolRun(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    /** 見 {@link #accents}。三個字元大約是最短的有意義單字。 */
    private static final int MIN_ACCENT_LENGTH = 3;

    /**
     * 把一段文字裡的地名與數字換成佔位符，同時記下原文與樣式。
     *
     * <p>地名先比對：它可能含數字（少見但存在），先換成 {@code {p}} 就不會被拆開。
     */
    private static void appendParametrized(String text, Style style, StringBuilder tmpl,
                                           List<Piece> places, List<Piece> numbers,
                                           List<Piece> users) {
        // 玩家名字最先抽掉。它可能長得像地名或含數字，先處理才不會被拆散。
        String self = localPlayerName();
        if (self != null) {
            int at = text.indexOf(self);
            if (at >= 0) {
                appendParametrized(text.substring(0, at), style, tmpl, places, numbers, users);
                tmpl.append(GlyphSplitter.PLAYER_PLACEHOLDER);
                users.add(new Piece(self, style));
                appendParametrized(text.substring(at + self.length()),
                        style, tmpl, places, numbers, users);
                return;
            }
        }
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
