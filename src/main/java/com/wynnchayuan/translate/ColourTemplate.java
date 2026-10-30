package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

final class ColourTemplate {

    private ColourTemplate() {}

    static String shiftColours(String text, int offset) {
        if (offset == 0) {
            return text;
        }
        java.util.regex.Matcher m = COLOUR_TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (m.find()) {
            out.append(text, at, m.start())
               .append("{c").append(Integer.parseInt(m.group(1)) + offset).append('}');
            at = m.end();
        }
        return at == 0 ? text : out.append(text.substring(at)).toString();
    }

    static int distinctColours(String text) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.regex.Matcher m = COLOUR_TOKEN.matcher(text);
        while (m.find()) {
            seen.add(m.group(1));
        }
        return seen.size();
    }

    private static final java.util.regex.Pattern COLOUR_TOKEN =
            java.util.regex.Pattern.compile("\\{c(\\d+)}");

    static String stripColourTokens(String text) {
        if (text == null || text.indexOf('{') < 0) {
            return text;
        }
        String out = text;
        if (out.contains(COLOR_END)) {
            out = out.replace(COLOR_END, "");
        }
        return COLOUR_ANY.matcher(out).replaceAll("");
    }

    private static final java.util.regex.Pattern COLOUR_ANY =
            java.util.regex.Pattern.compile("\\{c[^}]*}");

    enum Kind { TEXT, GLYPH, PLACE, NUMBER, USER, COLOR }

    record Token(Kind kind, String text, int index) {
        Token(Kind kind, String text) {
            this(kind, text, 0);
        }
    }

    static final String COLOR_END = "{/}";

    static Token colourToken(String template, int at) {
        if (template.startsWith(COLOR_END, at)) {
            return new Token(Kind.COLOR, COLOR_END, 0);
        }
        boolean word = at + 3 <= template.length()
                && template.charAt(at) == '{' && template.charAt(at + 1) == 'w';
        if (!word && (at + 3 > template.length()
                || template.charAt(at) != '{' || template.charAt(at + 1) != 'c')) {
            return null;
        }
        int end = template.indexOf('}', at + 2);
        if (end < 0) {
            return null;
        }
        String body = template.substring(at + 2, end);
        String whole = template.substring(at, end + 1);
        if (body.length() == 1 && body.charAt(0) >= '1' && body.charAt(0) <= '9') {
            int slot = body.charAt(0) - '0';
            return new Token(Kind.COLOR, whole, word ? slot + WORD_SLOT : slot);
        }
        if (word) {
            return null;
        }
        if (body.length() > 1 && body.charAt(0) == ':') {
            return new Token(Kind.COLOR, whole, -1);
        }
        return null;
    }

    record Bar(char sign, List<Style> styles) { }

    private static final int MIN_BAR = 3;

    static List<Bar> bars(List<LineParts.Piece> runs) {
        List<Bar> out = new ArrayList<>();
        char sign = 0;
        List<Style> styles = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            Style style = run.style() == null ? Style.EMPTY : run.style();
            for (int i = 0; i < run.text().length(); i++) {
                char c = run.text().charAt(i);
                if (c == sign) {
                    styles.add(style);
                    continue;
                }
                if (styles.size() >= MIN_BAR) {
                    out.add(new Bar(sign, List.copyOf(styles)));
                }
                styles.clear();
                sign = isBarSign(c) ? c : 0;
                if (sign != 0) {
                    styles.add(style);
                }
            }
        }
        if (styles.size() >= MIN_BAR) {
            out.add(new Bar(sign, List.copyOf(styles)));
        }
        return out;
    }

    private static boolean isBarSign(char c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)
                && !Character.isSurrogate(c);
    }

    static int[] barSpan(String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (!isBarSign(c)) {
                i++;
                continue;
            }
            int j = i;
            while (j < text.length() && text.charAt(j) == c) {
                j++;
            }
            if (j - i >= MIN_BAR) {
                return new int[] {i, j};
            }
            i = j;
        }
        return null;
    }

    static Bar takeBar(List<Bar> bars, boolean[] used, char sign, int length) {
        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            if (!used[i] && bar.sign() == sign && bar.styles().size() == length) {
                used[i] = true;
                return bar;
            }
        }
        return null;
    }

    static void appendBar(MutableComponent line, Bar bar) {
        int at = 0;
        while (at < bar.styles().size()) {
            Style style = bar.styles().get(at);
            int end = at;
            while (end < bar.styles().size() && bar.styles().get(end).equals(style)) {
                end++;
            }
            line.append(DisplayStyles.literal(String.valueOf(bar.sign()).repeat(end - at),
                                DisplayStyles.forDisplay(style)));
            at = end;
        }
    }

    static List<Style> palette(List<LineParts.Piece> runs) {
        List<Style> out = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            if (!LineTranslator.hasContent(run.text())) {
                continue;
            }
            Style style = run.style() == null ? Style.EMPTY : run.style();
            if (!out.contains(style)) {
                out.add(style);
            }
        }
        return out;
    }

    static List<Style> wordPalette(List<LineParts.Piece> runs) {
        List<Style> out = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            if (!GlyphSplitter.hasLetter(run.text())) {
                continue;
            }
            Style style = run.style() == null ? Style.EMPTY : run.style();
            if (!out.contains(style)) {
                out.add(style);
            }
        }
        return out;
    }

    private static final int WORD_SLOT = 100;

    static Style colourOf(Token token, List<Style> palette,
                                  List<Style> words, Style base) {
        if (token.index() == 0) {
            return null;
        }
        if (token.index() > WORD_SLOT) {
            int slot = token.index() - WORD_SLOT;
            return slot <= words.size() ? DisplayStyles.forDisplay(words.get(slot - 1)) : null;
        }
        if (token.index() > 0) {
            return token.index() <= palette.size()
                    ? DisplayStyles.forDisplay(palette.get(token.index() - 1)) : null;
        }
        String spec = token.text().substring(3, token.text().length() - 1).strip();
        if (spec.startsWith("#")) {
            try {
                return base.withColor(TextColor.fromRgb(
                        Integer.parseInt(spec.substring(1), 16)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        ChatFormatting named = ChatFormatting.getByName(spec);
        return named == null ? null : base.applyFormat(named);
    }

    private static final int NUMBERED_LENGTH = 4;

    private static int numberedAt(String template, int at) {
        if (at + NUMBERED_LENGTH > template.length()
                || template.charAt(at) != '{' || template.charAt(at + 1) != '~'
                || template.charAt(at + 3) != '}') {
            return 0;
        }
        char digit = template.charAt(at + 2);
        return digit >= '1' && digit <= '9' ? digit - '0' : 0;
    }

    static List<Token> tokenize(String template) {
        List<Token> out = new java.util.ArrayList<>();
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        String place = GlyphSplitter.PLACE_PLACEHOLDER;
        String number = GlyphSplitter.NUMBER_PLACEHOLDER;
        String user = GlyphSplitter.PLAYER_PLACEHOLDER;
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
            } else if (numberedAt(template, i) > 0) {
                flush(text, out);
                out.add(new Token(Kind.NUMBER, "", numberedAt(template, i)));
                i += NUMBERED_LENGTH;
            } else if (template.startsWith(user, i)) {
                flush(text, out);
                out.add(new Token(Kind.USER, user));
                i += user.length();
            } else if (colourToken(template, i) != null) {
                Token colour = colourToken(template, i);
                flush(text, out);
                out.add(colour);
                i += colour.text().length();
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
