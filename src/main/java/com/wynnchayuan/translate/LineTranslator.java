package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import net.minecraft.client.Minecraft;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * 把一行原文換成譯文，同時保住顏色與符號。
 *
 * <h2>顏色可以抄，字型不能</h2>
 * 這是整個顯示層最重要的一條規則。自訂字型（{@code common} 那些）的材質裡<b>只有那些
 * 符號的圖，沒有中文字</b>；把中文塞進帶自訂字型的樣式，畫出來就是缺字方框。
 *
 * <p>但顏色、粗體、斜體是渲染屬性，跟字型無關，抄過去完全安全——而且 Wynncraft 的
 * 顏色是有意義的（元素色、稀有度色），丟掉很可惜。所以：
 *
 * <ul>
 *   <li><b>文字</b>：抄顏色與粗斜體，字型強制 {@link FontDescription#DEFAULT}</li>
 *   <li><b>符號</b>：整段原封不動搬回來，<b>連原本的字型一起</b>——不重建，
 *       所以材質包怎麼畫就怎麼畫，不可能破圖</li>
 *   <li><b>數值</b>：填回原本的數字，沿用它自己的樣式（同樣強制預設字型）</li>
 * </ul>
 *
 * <p>佔位符數量對不上就整行放棄，回傳 {@code null} 讓呼叫端顯示灰色原文。
 * 寧可不翻，也不要畫出錯位的東西。
 */
public final class LineTranslator {

    private LineTranslator() {}

    /**
     * @return 譯好的一行；查不到翻譯或佔位符對不上時回傳 {@code null}
     */
    public static Component translate(StyledText line, TranslationStore store) {
        Component whole = translateWholeLine(line, store);
        return whole != null ? whole : translateSegments(line, store);
    }

    /**
     * 逐片段替換：保留原本的元件結構，只換掉查得到的文字片段。
     *
     * <h2>為什麼這才是保真的做法</h2>
     * 另一條路（{@link #translateWholeLine}）是把整行拆成模板再重建，格式保不保得住
     * 完全取決於重建得多完美——而實際的 tooltip 一行裡可能混用五、六種字型
     * （{@code space} 排版、{@code banner/box} 外框、{@code tooltip/divider} 分隔線、
     * {@code language/wynncraft} 文字…），任何一點還原不精確就會變成方框或錯位。
     *
     * <p>逐片段替換不重建任何東西：不是文字的片段<b>原封不動抄過去</b>，
     * 連字型、顏色、負寬度空白都保持原樣，所以版面天然就是對的。
     * 只有查得到譯文的那些片段會被換掉，而且只有那些片段需要改成預設字型。
     *
     * <p>代價是無法跨片段調整語序（例如把「Combat Experience +6%」整句重排）。
     * 對「標籤 + 數值」這種結構沒有影響，也正是物品 tooltip 的主要形態。
     */
    private static Component translateSegments(StyledText line, TranslationStore store) {
        // 先把每個片段翻好，不急著組裝——寬度要等整行都翻完才量得準。
        List<Piece> pieces = new ArrayList<>();
        boolean any = false;

        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();

            if (SpaceOffset.isSpaceFont(ps)) {
                pieces.add(Piece.space(SpaceOffset.decode(raw), style));
                continue;
            }
            // 符號、純空白、查不到的文字 —— 一律原封不動，含字型
            if (GlyphSplitter.isGlyphPart(part) || raw.isBlank()) {
                pieces.add(Piece.text(raw, style));
                continue;
            }
            String replaced = translateOneSegment(raw, style, store);
            if (replaced == null) {
                pieces.add(Piece.text(raw, style));
                continue;
            }
            any = true;
            Style display = forDisplay(style);
            pieces.add(Piece.text(replaced, display));
        }
        if (!any) {
            return null;
        }

        int alignAt = findAlignPoint(pieces);
        if (alignAt < 0) {
            return assemble(pieces, -1, 0);    // 沒有欄位結構，不必調
        }
        if (!pieces.get(alignAt).isSpace()) {
            // 這種行原本靠標籤夠長就自然排到欄位位置，伺服器沒插對齊字元。
            // 翻譯後標籤變短就沒東西撐開，數值會整個貼到標籤旁邊 ——
            // 所以就地補一個寬度 0 的空白，下面再把差額加進去。
            pieces.add(alignAt, Piece.space(0, pieces.get(alignAt).style()));
        }

        // 先照原樣組一次，然後「量」出整行差多少，而不是把各片段的差額「算」總和。
        //
        // 算的方式只要漏掉任何一項（片段間的空白、字型不同造成的字距差、
        // 編碼後空白字元的實際寬度）就會差個幾像素，畫面上看得出來。
        // 量整行則是把所有因素一次涵蓋進去，而且 U+D0000+n 的寬度精確等於 n 像素，
        // 所以補一次就會準。
        int originalWidth = widthOf(line);
        Component draft = assemble(pieces, -1, 0);
        int delta = originalWidth - widthOf(draft);

        return assemble(pieces, alignAt, delta);
    }

    /**
     * 找出這一行該在哪裡撐開寬度，也就是「標籤」與「數值」的交界。
     *
     * <p>tooltip 的欄位對齊有兩種情形，要分開處理：
     *
     * <ol>
     *   <li><b>有對齊空白字元</b>：伺服器已經插好了，調整它即可</li>
     *   <li><b>沒有</b>：原文標籤剛好夠長，自然就排到欄位位置。
     *       翻譯後標籤變短，就得自己補一個</li>
     * </ol>
     *
     * <p>第二種的交界怎麼找：從尾端往回走，把「看起來像數值」的片段
     * （{@code +372}、{@code [94.0%]}、純空白）都算進數值區，
     * 停在第一個真正的文字片段——那就是交界。
     *
     * @return 要撐開的位置索引；沒有欄位結構時回傳 -1
     */
    private static int findAlignPoint(List<Piece> pieces) {
        for (int i = pieces.size() - 1; i >= 0; i--) {
            if (pieces.get(i).isSpace()) {
                return i;                      // 情形一：用現成的
            }
        }
        int i = pieces.size() - 1;
        while (i >= 0 && looksLikeValue(pieces.get(i).text())) {
            i--;
        }
        // i 是最後一個文字片段；數值區從 i+1 開始。
        // 兩邊都要有東西才算「標籤 + 數值」，否則就是一般句子。
        int valueStart = i + 1;
        return (i >= 0 && valueStart < pieces.size()) ? valueStart : -1;
    }

    /** 這段文字看起來是數值而不是文案。 */
    private static boolean looksLikeValue(String text) {
        String t = text.strip();
        if (t.isEmpty()) {
            return true;                       // 純空白，算在數值區
        }
        char c = t.charAt(0);
        if (c == '+' || c == '-' || c == '[' || c == '(') {
            return true;
        }
        // 純數字、百分比、分數這類
        return t.chars().noneMatch(Character::isLetter);
    }

    /**
     * 把片段組回一個 Component。
     *
     * @param adjustIndex 要調整寬度的空白字元索引，{@code -1} 表示都不調
     * @param adjustPx    調整量（像素）
     */
    private static Component assemble(List<Piece> pieces, int adjustIndex, int adjustPx) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (!p.isSpace()) {
                out.append(literal(p.text(), p.style()));
                continue;
            }
            int px = p.spacePx() + (i == adjustIndex ? adjustPx : 0);
            String encoded = SpaceOffset.encode(px);
            if (!encoded.isEmpty()) {
                out.append(literal(encoded, p.style()));
            }
        }
        return out;
    }

    /** 整段文字畫出來有多寬（像素）。 */
    private static int widthOf(Component component) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.font == null ? 0 : mc.font.width(component);
    }

    /** 原始行的寬度。 */
    private static int widthOf(StyledText line) {
        return widthOf(line.getComponent());
    }

    /** 組裝前的一個片段：一般文字，或一個帶寬度的對齊空白。 */
    private record Piece(String text, Style style, int spacePx, boolean isSpace) {
        static Piece text(String t, Style s) {
            return new Piece(t, s, 0, false);
        }

        static Piece space(int px, Style s) {
            return new Piece("", s, px, true);
        }
    }

    /**
     * 翻一個文字片段，並保留它原本的前後空白。
     *
     * <p>空白會影響版面（Wynncraft 用它對齊），所以只拿中間的實際內容去查，
     * 查到之後把空白原樣接回。
     */
    private static String translateOneSegment(String raw, Style style, TranslationStore store) {
        String core = raw.strip();
        if (core.isEmpty() || !GlyphSplitter.hasLetter(core)) {
            return null;
        }
        int lead = raw.indexOf(core.charAt(0));
        String prefix = raw.substring(0, Math.max(0, lead));
        String suffix = raw.substring(prefix.length() + core.length());

        // 借用整行的機制處理片段內的數值與地名參數化
        StyledText one = StyledText.fromComponent(Component.literal(core).withStyle(style));
        LineParts parts = LineParts.of(one);
        String hit = lookup(parts.template(), store);
        if (hit == null) {
            return null;
        }
        Component rebuilt = rebuild(hit, parts);
        return rebuilt == null ? null : prefix + rebuilt.getString() + suffix;
    }

    /** 整行查表。散文與對話用這條路，因為它們需要跨片段重排語序。 */
    private static Component translateWholeLine(StyledText line, TranslationStore store) {
        LineParts parts = LineParts.of(line);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;                       // 純符號或純數值的行，沒東西可翻
        }
        String translated = lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            return null;
        }
        return rebuild(translated, parts);
    }

    /**
     * 查字典，查不到就把首尾的排版符號拿掉再查一次。
     *
     * <p>Wynncraft 會在每一行前後塞排版用的空白字元（{@code minecraft:space}、
     * 外框圖示等），所以實際模板長這樣：
     *
     * <pre>
     *   實際模板  {#}Doom Stone{#}
     *   字典的鍵  Doom Stone          ← 語料來自 CDN 純文字，不含排版符號
     * </pre>
     *
     * 兩者永遠對不上。把首尾的 {@code {#}} 與空白剝掉再查就能命中，
     * 命中後再把剝掉的部分原樣接回去——佔位符數量與位置都不變，
     * 所以 {@link #rebuild} 仍然填得回原本的符號。
     *
     * <p>只剝<b>首尾</b>：句子中間的符號是內容的一部分（例如
     * {@code an {#}Item Identifier can unlock}），剝掉會改變語意。
     */
    private static String lookup(String template, TranslationStore store) {
        String exact = store.lookup(template);
        if (exact != null) {
            return exact;
        }
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;

        int start = 0;
        while (true) {
            if (template.startsWith(glyph, start)) {
                start += glyph.length();
            } else if (start < template.length() && Character.isWhitespace(template.charAt(start))) {
                start++;
            } else {
                break;
            }
        }
        int end = template.length();
        while (end > start) {
            if (end >= start + glyph.length() && template.startsWith(glyph, end - glyph.length())) {
                end -= glyph.length();
            } else if (Character.isWhitespace(template.charAt(end - 1))) {
                end--;
            } else {
                break;
            }
        }
        if (start == 0 && end == template.length()) {
            return null;                       // 首尾沒有可剝的，不必重查
        }
        String core = template.substring(start, end);
        if (core.isBlank()) {
            return null;
        }
        String hit = store.lookup(core);
        return hit == null ? null : template.substring(0, start) + hit + template.substring(end);
    }

    /**
     * 還沒翻譯的行 —— <b>原封不動抄過去</b>。
     *
     * <p>先前這裡會把原文轉成灰色來標示進度，但那等於重建整行，
     * 排版符號的字型與負寬度空白都會跑掉，整個面板的版面就壞了。
     * 格式保真比「看得出哪行沒翻」重要，所以改成原樣複製。
     */
    public static Component untranslated(StyledText line) {
        MutableComponent out = Component.empty();
        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            out.append(literal(raw, ps == null ? Style.EMPTY : ps.getStyle()));
        }
        return out;
    }

    // ------------------------------------------------------------ 內部

    private static Component rebuild(String translated, LineParts parts) {
        List<Token> tokens = tokenize(translated);
        long wantGlyphs = tokens.stream().filter(t -> t.kind() == Kind.GLYPH).count();
        long wantPlaces = tokens.stream().filter(t -> t.kind() == Kind.PLACE).count();
        long wantNumbers = tokens.stream().filter(t -> t.kind() == Kind.NUMBER).count();
        if (wantGlyphs != parts.glyphs().size()
                || wantPlaces != parts.places().size()
                || wantNumbers != parts.numbers().size()) {
            return null;              // 譯者刪了或多加了佔位符，放棄這行
        }

        MutableComponent out = Component.empty();
        Style textStyle = forDisplay(parts.textStyle());
        int glyph = 0;
        int place = 0;
        int number = 0;

        for (Token token : tokens) {
            switch (token.kind()) {
                case GLYPH -> {
                    // 符號連同原樣式（含自訂字型）整段搬回，這是它顯示得出來的唯一方式
                    LineParts.Piece p = parts.glyphs().get(glyph++);
                    out.append(Component.literal(p.text()).withStyle(p.style()));
                }
                case PLACE -> {
                    // 地名是專有名詞，原樣填回，永遠不翻譯
                    LineParts.Piece p = parts.places().get(place++);
                    out.append(literal(p.text(), forDisplay(p.style())));
                }
                case NUMBER -> {
                    LineParts.Piece p = parts.numbers().get(number++);
                    out.append(literal(p.text(), forDisplay(p.style())));
                }
                case TEXT -> out.append(literal(token.text(), textStyle));
            }
        }
        return out;
    }

    /** 保留顏色與粗斜體，但把字型換成預設，中文才畫得出來。 */
    private static Style forDisplay(Style style) {
        return (style == null ? Style.EMPTY : style).withFont(FontDescription.DEFAULT);
    }

    private static Style greyed() {
        return Style.EMPTY
                .withColor(ChatFormatting.DARK_GRAY)
                .withFont(FontDescription.DEFAULT);
    }

    private static Component literal(String text, Style style) {
        return Component.literal(text).withStyle(style);
    }

    // ------------------------------------------------------------ 模板切詞

    private enum Kind { TEXT, GLYPH, PLACE, NUMBER }

    private record Token(Kind kind, String text) {}

    /** 把模板切成「文字 / 符號佔位 / 數值佔位」的序列。 */
    private static List<Token> tokenize(String template) {
        List<Token> out = new java.util.ArrayList<>();
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        String place = GlyphSplitter.PLACE_PLACEHOLDER;
        String number = GlyphSplitter.NUMBER_PLACEHOLDER;
        int i = 0;
        StringBuilder text = new StringBuilder();

        while (i < template.length()) {
            if (template.startsWith(glyph, i)) {
                flush(text, out);
                out.add(new Token(Kind.GLYPH, glyph));
                i += glyph.length();
            } else if (template.startsWith(place, i)) {
                flush(text, out);
                out.add(new Token(Kind.PLACE, place));
                i += place.length();
            } else if (template.startsWith(number, i)) {
                flush(text, out);
                out.add(new Token(Kind.NUMBER, number));
                i += number.length();
            } else {
                text.append(template.charAt(i++));
            }
        }
        flush(text, out);
        return out;
    }

    private static void flush(StringBuilder text, List<Token> out) {
        if (!text.isEmpty()) {
            out.add(new Token(Kind.TEXT, text.toString()));
            text.setLength(0);
        }
    }
}
