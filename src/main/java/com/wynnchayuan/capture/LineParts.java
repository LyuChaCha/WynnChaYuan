package com.wynnchayuan.capture;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public record LineParts(
        String template,
        List<Piece> glyphs,
        List<Piece> places,
        List<Piece> numbers,
        List<Piece> users,
        List<Piece> accents,
        Style textStyle,

        List<Piece> runs) {

    public record Piece(String text, Style style) {}

    public LineParts namingPlayer() {
        if (!users.isEmpty() || !LITERAL_PLAYER.matcher(template).find()) {
            return this;
        }
        Style style = runs.isEmpty() || runs.get(0).style() == null
                ? Style.EMPTY : runs.get(0).style();
        List<Piece> named = new ArrayList<>(users);
        StringBuilder out = new StringBuilder();
        Matcher m = LITERAL_PLAYER.matcher(template);
        int from = 0;
        while (m.find()) {
            out.append(template, from, m.start())
               .append(GlyphSplitter.PLAYER_PLACEHOLDER);
            named.add(new Piece(LITERAL_NAME, style));
            from = m.end();
        }
        out.append(template.substring(from));
        return new LineParts(out.toString(), glyphs, places, numbers,
                             named, accents, textStyle, runs);
    }

    private static final java.util.regex.Pattern LITERAL_PLAYER =
            java.util.regex.Pattern.compile("\\bPlayer\\b");

    private static final String LITERAL_NAME = "Player";

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
        return of(line, null);
    }

    public static LineParts of(StyledText line, Broadcasts.Spans spans) {
        int flatAt = 0;
        int[] slot = {-1, -1};
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

            String lead = leadingGlyphs(raw);
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
            if (spans == null) {
                appendParametrized(text, style, tmpl, places, numbers, users);
            } else {
                appendSpanned(text, flatAt, style, spans, slot, tmpl, places, numbers, users);
            }
            flatAt += text.length();
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
                textStyle,
                List.copyOf(runs));
    }

    public static String flat(StyledText line) {
        StringBuilder out = new StringBuilder();
        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty() || GlyphSplitter.isGlyphPart(part)) {
                continue;
            }
            String lead = leadingGlyphs(raw);
            String tail = trailingGlyphs(raw, lead.length());
            out.append(GlyphSplitter.stripGlyphChars(
                    raw.substring(lead.length(), raw.length() - tail.length())));
        }
        return out.toString();
    }

    private static void appendSpanned(String text, int at, Style style, Broadcasts.Spans spans,
                                      int[] slot, StringBuilder tmpl, List<Piece> places,
                                      List<Piece> numbers, List<Piece> users) {
        int end = at + text.length();
        int pos = at;
        while (pos < end) {
            int next;
            if (pos >= spans.userStart() && pos < spans.userEnd()) {
                next = Math.min(end, spans.userEnd());
                slot[0] = extend(users, slot[0], text.substring(pos - at, next - at), style,
                                 tmpl, GlyphSplitter.PLAYER_PLACEHOLDER);
            } else if (pos >= spans.worldStart() && pos < spans.worldEnd()) {
                next = Math.min(end, spans.worldEnd());
                slot[1] = extend(numbers, slot[1], text.substring(pos - at, next - at), style,
                                 tmpl, GlyphSplitter.NUMBER_PLACEHOLDER);
            } else {
                next = end;
                if (spans.userStart() > pos) {
                    next = Math.min(next, spans.userStart());
                }
                if (spans.worldStart() > pos) {
                    next = Math.min(next, spans.worldStart());
                }
                appendParametrized(text.substring(pos - at, next - at), style,
                                   tmpl, places, numbers, users);
            }
            pos = next;
        }
    }

    private static int extend(List<Piece> pool, int index, String piece, Style style,
                              StringBuilder tmpl, String placeholder) {
        if (index < 0) {
            tmpl.append(placeholder);
            pool.add(new Piece(piece, style));
            return pool.size() - 1;
        }
        Piece open = pool.get(index);
        pool.set(index, new Piece(open.text() + piece, open.style()));
        return index;
    }

    public static List<Piece> accentsAgainst(List<Piece> runs, Style textStyle) {
        return accents(runs, textStyle);
    }

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

    private static List<Piece> accents(List<Piece> runs, Style textStyle) {
        List<Piece> out = new ArrayList<>();
        for (Piece run : runs) {
            String text = run.text().strip();
            if (java.util.Objects.equals(run.style(), textStyle) || text.isEmpty()) {
                continue;
            }
            boolean word = text.length() >= MIN_ACCENT_LENGTH && GlyphSplitter.hasLetter(text);
            if (word || isSymbolRun(text)) {
                out.add(new Piece(text, run.style()));
            }
        }
        return List.copyOf(out);
    }

    private static boolean isSymbolRun(String text) {
        boolean meaningful = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                return false;
            }
            if (BRACKETS.indexOf(c) < 0) {
                meaningful = true;
            }
        }
        return meaningful;
    }

    private static final String BRACKETS = "[]()<>{}【】（）「」";

    private static final int MIN_ACCENT_LENGTH = 3;

    private static void appendParametrized(String text, Style style, StringBuilder tmpl,
                                           List<Piece> places, List<Piece> numbers,
                                           List<Piece> users) {
        String self = SelfNames.find(text);
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
