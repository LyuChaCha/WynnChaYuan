package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ColumnTemplates {

    private ColumnTemplates() {}

    static String byColumn(String template, TranslationStore store) {
        if (template.indexOf(LineTranslator.NEWLINE) >= 0) {
            return null;
        }
        List<String> parts = splitColumns(template);
        if (parts.size() < 2) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int hits = 0;
        int colours = 0;
        int values = 0;
        for (String part : parts) {
            String hit = GlyphSplitter.hasLetter(part) ? TemplateLookup.lookup(part, store) : null;
            if (hit != null && !hit.isBlank()) {
                String shifted = renumber(ColourTemplate.shiftColours(hit, colours), values);
                if (shifted == null) {
                    return null;
                }
                colours += ColourTemplate.distinctColours(hit);
                out.append(shifted);
                hits++;
            } else {
                out.append(part);
            }
            values += countNumbers(part);
        }
        return hits > 0 ? out.toString() : null;
    }

    private static final String WELCOME_LINE =
            GlyphSplitter.GLYPH_PLACEHOLDER + "Welcome to Wynncraft!" + LineTranslator.NEWLINE;

    static String welcomeBanner(String template, TranslationStore store) {
        if (template == null || store == null) {
            return null;
        }
        int start = 0;
        while (start < template.length() && template.charAt(start) == LineTranslator.NEWLINE) {
            start++;
        }
        if (!template.startsWith(WELCOME_LINE, start)) {
            return null;
        }
        String[] lines = template.split(String.valueOf(LineTranslator.NEWLINE), -1);
        StringBuilder out = new StringBuilder();
        int values = 0;
        int segments = 0;
        boolean marked = false;
        int i = 0;
        while (i < lines.length) {
            if (i > 0) {
                out.append(LineTranslator.NEWLINE);
            }
            if (!GlyphSplitter.hasLetter(lines[i])) {
                out.append(lines[i]);
                values += countNumbers(lines[i]);
                i++;
                continue;
            }
            String hit = null;
            int end = -1;
            for (int j = lines.length; j > i && hit == null; j--) {
                if (!GlyphSplitter.hasLetter(lines[j - 1])
                        || (i == 0 && j == lines.length)) {
                    continue;
                }
                String run = String.join(String.valueOf(LineTranslator.NEWLINE),
                                         Arrays.copyOfRange(lines, i, j));
                String found = TemplateLookup.lookup(run, store);
                if (found != null && !found.isBlank()
                        && found.split(String.valueOf(LineTranslator.NEWLINE), -1).length == j - i) {
                    hit = found;
                    end = j;
                }
            }
            if (hit == null) {
                return null;
            }
            String shifted = renumber(hit, values);
            if (shifted == null) {
                return null;
            }
            marked |= ColourTemplate.distinctColours(hit) > 0 || hit.contains(ColourTemplate.COLOR_END);
            segments++;
            for (int k = i; k < end; k++) {
                values += countNumbers(lines[k]);
            }
            out.append(shifted);
            i = end;
        }
        return segments > 1 && marked ? ColourTemplate.stripColourTokens(out.toString()) : out.toString();
    }

    static int countNumbers(String template) {
        String mark = GlyphSplitter.NUMBER_PLACEHOLDER;
        int n = 0;
        for (int at = template.indexOf(mark); at >= 0;
                at = template.indexOf(mark, at + mark.length())) {
            n++;
        }
        return n;
    }

    private static final Pattern NUMBER_ANY = Pattern.compile("\\{~([1-9])?}");

    static String renumber(String text, int offset) {
        if (offset == 0 || text == null || text.indexOf('{') < 0) {
            return text;
        }
        Matcher m = NUMBER_ANY.matcher(text);
        StringBuilder out = new StringBuilder();
        int at = 0;
        int plain = 0;
        while (m.find()) {
            int want = m.group(1) != null
                    ? offset + Integer.parseInt(m.group(1))
                    : offset + ++plain;
            if (want > 9) {
                return null;
            }
            out.append(text, at, m.start()).append("{~").append(want).append('}');
            at = m.end();
        }
        return at == 0 ? text : out.append(text.substring(at)).toString();
    }

    private static List<String> splitColumns(String template) {
        String mark = GlyphSplitter.GLYPH_PLACEHOLDER;
        List<String> out = new ArrayList<>();
        int at = template.indexOf(mark);
        if (at > 0) {
            out.add(template.substring(0, at));
        }
        while (at >= 0) {
            int next = template.indexOf(mark, at + mark.length());
            out.add(next < 0 ? template.substring(at) : template.substring(at, next));
            at = next;
        }
        return out;
    }
}
