package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.ColumnAligner.Piece;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

final class StyledRebuilder {

    private StyledRebuilder() {}

    static List<LineParts.Piece> bracketAccents(
            List<LineParts.Piece> allRuns, String[] translated, Style blockStyle) {
        List<Style> source = new ArrayList<>();
        List<String> sourceText = new ArrayList<>();
        StringBuilder inside = new StringBuilder();
        Style open = null;
        boolean mixed = false;
        int depth = 0;
        for (LineParts.Piece run : allRuns) {
            String text = run.text();
            if (depth > 0 && !sameColour(open, run.style())) {
                mixed = true;
            }
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '[') {
                    if (depth == 0) {
                        open = run.style();
                        mixed = false;
                        inside.setLength(0);
                    }
                    depth++;
                } else if (c == ']' && depth > 0) {
                    depth--;
                    if (depth == 0) {
                        if (mixed) {
                            return List.of();
                        }
                        source.add(open);
                        sourceText.add(inside.toString());
                        open = null;
                    }
                } else if (depth > 0) {
                    inside.append(c);
                }
            }
        }
        if (source.isEmpty() || depth != 0) {
            return List.of();
        }
        List<String> spans = squareSpans(String.join(LineTranslator.NL, translated));
        if (spans.size() != source.size()) {
            return List.of();
        }
        int[] from = pairBrackets(sourceText, spans);
        List<LineParts.Piece> out = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            Style style = source.get(from[i]);
            if (style == null || sameColour(style, blockStyle) || spans.get(i).isBlank()) {
                continue;
            }
            out.add(new LineParts.Piece(spans.get(i), style));
            for (String piece : PLACEHOLDER.split(spans.get(i), -1)) {
                if (piece.length() >= 2 && hasLetter(piece)) {
                    out.add(new LineParts.Piece(piece, style));
                    String bare = piece.strip();
                    if (piece.endsWith("]") && !bare.equals(piece)
                            && bare.length() >= 2 && hasLetter(bare)) {
                        out.add(new LineParts.Piece(bare, style));
                    }
                }
            }
        }
        return out;
    }

    static boolean hasLetter(String text) {
        return text.codePoints().anyMatch(Character::isLetter);
    }

    static int[] pairBrackets(List<String> source, List<String> spans) {
        int n = spans.size();
        int[] same = new int[n];
        boolean[] srcWordy = new boolean[n];
        boolean[] dstWordy = new boolean[n];
        boolean identical = true;
        int srcCount = 0;
        int dstCount = 0;
        for (int i = 0; i < n; i++) {
            same[i] = i;
            srcWordy[i] = hasLetter(source.get(i));
            dstWordy[i] = hasLetter(PLACEHOLDER.matcher(spans.get(i)).replaceAll(""));
            identical &= srcWordy[i] == dstWordy[i];
            srcCount += srcWordy[i] ? 1 : 0;
            dstCount += dstWordy[i] ? 1 : 0;
        }
        if (identical || srcCount != dstCount) {
            return same;
        }
        int[] out = new int[n];
        int wordy = 0;
        int plain = 0;
        for (int i = 0; i < n; i++) {
            if (dstWordy[i]) {
                while (!srcWordy[wordy]) {
                    wordy++;
                }
                out[i] = wordy++;
            } else {
                while (srcWordy[plain]) {
                    plain++;
                }
                out[i] = plain++;
            }
        }
        return out;
    }

    private static List<String> squareSpans(String text) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == ']' && depth > 0) {
                depth--;
                if (depth == 0) {
                    out.add(text.substring(start, i + 1)
                                .replace(String.valueOf(LineTranslator.NEWLINE), ""));
                }
            }
        }
        return depth == 0 ? out : List.of();
    }

    static boolean sameColour(Style one, Style other) {
        if (one == null || other == null) {
            return one == other;
        }
        return BlockTranslator.undecorated(one).equals(BlockTranslator.undecorated(other));
    }

    static List<LineParts.Piece> wholeLineAccents(
            List<LineParts> parts, List<LineParts.Piece> allRuns,
            String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        String template = parts.size() == 1 ? parts.get(0).template() : null;
        List<RowStyle> source = template == null
                ? perPartStyles(parts) : uniformStyles(allRuns, template);
        String[] dst = String.join(LineTranslator.NL, translated).split(LineTranslator.NL, -1);
        if (source.size() != dst.length) {
            return List.of();
        }
        List<LineParts.Piece> out = new ArrayList<>();
        for (int i = 0; i < dst.length; i++) {
            String text = PLACEHOLDER.matcher(dst[i]).replaceAll("").strip();
            if (!LineTranslator.hasContent(text)) {
                continue;
            }
            Style only = source.get(i).only();
            if (only == null) {
                only = fallback(source.get(i).dominant(), known, dst[i]);
            }
            if (only == null || java.util.Objects.equals(only, blockStyle)
                    || covered(known, text, only)) {
                continue;
            }
            out.add(new LineParts.Piece(text, only));
        }
        return out;
    }

    static List<LineParts.Piece> labelValueAccents(
            List<LineParts> parts, String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        if (parts.size() != 1 || translated.length != 1) {
            return List.of();
        }
        if (translated[0].indexOf(LineTranslator.NEWLINE) >= 0
                || parts.get(0).template().indexOf(LineTranslator.NEWLINE) >= 0) {
            return List.of();
        }
        Style label = null;
        Style value = null;
        for (LineParts.Piece run : parts.get(0).runs()) {
            if (BlockTranslator.isNote(run.text())) {
                continue;
            }
            if (label == null) {
                int colon = run.text().indexOf(':');
                if (colon < 0) {
                    continue;
                }
                if (LineTranslator.hasContent(run.text().substring(colon + 1))) {
                    return List.of();
                }
                label = BlockTranslator.undecorated(run.style());
            } else if (LineTranslator.hasContent(run.text())) {
                value = BlockTranslator.undecorated(run.style());
                break;
            }
        }
        if (label == null || value == null || label.equals(value)) {
            return List.of();
        }
        int at = translated[0].indexOf(':');
        int wide = translated[0].indexOf('：');
        if (at < 0 || (wide >= 0 && wide < at)) {
            at = wide;
        }
        if (at < 0) {
            return List.of();
        }
        List<LineParts.Piece> out = new ArrayList<>();
        add(out, translated[0].substring(0, at + 1), label, blockStyle, known);
        add(out, translated[0].substring(at + 1), value, blockStyle, known);
        return out;
    }

    static List<LineParts.Piece> segmentAccents(
            List<LineParts> parts, String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        String[] dst = String.join(LineTranslator.NL, translated).split(LineTranslator.NL, -1);
        if (dst.length != parts.size()) {
            return null;
        }
        List<LineParts.Piece> out = new ArrayList<>();
        boolean applied = false;
        StringBuilder earlier = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            List<Segment> source = segments(parts.get(i).template());
            List<Segment> target = segments(dst[i]);
            List<List<LineParts.Piece>> styles = source == null
                    ? null : segmentStyles(source, parts.get(i).runs());
            if (styles == null || target == null || !sameShape(source, target)
                    || usableStyles(source, target, styles) < 2) {
                earlier.append(dst[i]).append(LineTranslator.NL);
                continue;
            }
            applied = true;
            for (int k = 0; k < target.size(); k++) {
                String core = target.get(k).text().strip();
                StringBuilder before = new StringBuilder(earlier);
                String name = nameInside(source.get(k).text().strip(), styles.get(k),
                                         core, known);
                List<LineParts.Piece> pieces = name != null
                        ? List.of(new LineParts.Piece(name, styles.get(k).get(0).style()))
                        : assign(source.get(k).text().strip(), styles.get(k), core);
                for (LineParts.Piece piece : pieces) {
                    if (LineTranslator.hasContent(piece.text()) && before.indexOf(piece.text()) < 0) {
                        add(out, piece.text(), piece.style(), blockStyle, known);
                    }
                    before.append(piece.text());
                }
                earlier.append(target.get(k).text());
            }
            earlier.append(LineTranslator.NL);
        }
        return applied ? out : null;
    }

    private static String nameInside(String source, List<LineParts.Piece> runs, String target,
                                     List<LineParts.Piece> known) {
        if (runs.size() != 1 || target.equals(source)) {
            return null;
        }
        Style style = runs.get(0).style();
        List<String> names = new ArrayList<>();
        names.add(source);
        for (LineParts.Piece piece : known) {
            if (java.util.Objects.equals(piece.style(), style)) {
                names.add(piece.text().strip());
            }
        }
        String best = null;
        for (String name : names) {
            if (LineTranslator.hasContent(name) && !target.equals(name) && target.contains(name)
                    && (best == null || name.length() > best.length())) {
                best = name;
            }
        }
        return best;
    }

    private record Segment(String text, String anchor) {}

    private static List<Segment> segments(String line) {
        List<Segment> out = new ArrayList<>();
        java.util.regex.Matcher m = PLACEHOLDER.matcher(line);
        int from = 0;
        int plainNumbers = 0;
        int nextIndex = 1;
        while (m.find()) {
            String token = m.group();
            String anchor;
            if (token.equals(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                anchor = "#";
            } else if (token.equals(GlyphSplitter.PLACE_PLACEHOLDER)) {
                anchor = "p";
            } else if (token.equals(GlyphSplitter.PLAYER_PLACEHOLDER)) {
                anchor = "u";
            } else if (token.equals("{~}")) {
                anchor = "~";
                plainNumbers++;
            } else if (token.matches("\\{~\\d+\\}")) {
                if (Integer.parseInt(token.substring(2, token.length() - 1)) != nextIndex++) {
                    return null;
                }
                anchor = "~";
            } else {
                return null;
            }
            out.add(new Segment(line.substring(from, m.start()), anchor));
            from = m.end();
        }
        if (plainNumbers > 0 && nextIndex > 1) {
            return null;
        }
        out.add(new Segment(line.substring(from), null));
        return out;
    }

    private static boolean sameShape(List<Segment> source, List<Segment> target) {
        if (source.size() != target.size()) {
            return false;
        }
        for (int k = 0; k < source.size(); k++) {
            if (!java.util.Objects.equals(source.get(k).anchor(), target.get(k).anchor())
                    || LineTranslator.hasContent(source.get(k).text()) != LineTranslator.hasContent(target.get(k).text())) {
                return false;
            }
        }
        return true;
    }

    private static List<List<LineParts.Piece>> segmentStyles(List<Segment> source,
                                                              List<LineParts.Piece> runs) {
        StringBuilder plain = new StringBuilder();
        List<Style> styleAt = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            for (int c = 0; c < run.text().length(); c++) {
                plain.append(run.text().charAt(c));
                styleAt.add(run.style());
            }
        }
        List<List<LineParts.Piece>> out = new ArrayList<>(source.size());
        int cursor = 0;
        for (Segment segment : source) {
            String core = segment.text().strip();
            if (core.isEmpty()) {
                out.add(List.of());
                continue;
            }
            int at = plain.indexOf(core, cursor);
            if (at < 0) {
                return null;
            }
            List<LineParts.Piece> pieces = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            Style current = null;
            for (int c = at; c < at + core.length(); c++) {
                char ch = plain.charAt(c);
                if (!Character.isWhitespace(ch)) {
                    Style style = styleAt.get(c);
                    if (current != null && !java.util.Objects.equals(current, style)) {
                        pieces.add(new LineParts.Piece(text.toString().strip(), current));
                        text.setLength(0);
                    }
                    current = style;
                }
                text.append(ch);
            }
            pieces.add(new LineParts.Piece(text.toString().strip(), current));
            out.add(pieces);
            cursor = at + core.length();
        }
        return out;
    }

    private static int usableStyles(List<Segment> source, List<Segment> target,
                                    List<List<LineParts.Piece>> styles) {
        java.util.Set<Style> distinct = new java.util.HashSet<>();
        for (int k = 0; k < target.size(); k++) {
            for (LineParts.Piece piece : assign(source.get(k).text().strip(), styles.get(k),
                                                target.get(k).text().strip())) {
                distinct.add(piece.style());
            }
        }
        return distinct.size();
    }

    private static List<LineParts.Piece> assign(String source, List<LineParts.Piece> runs,
                                                String target) {
        if (runs.size() == 1) {
            return LineTranslator.hasContent(target) ? List.of(new LineParts.Piece(target, runs.get(0).style()))
                                      : List.of();
        }
        if (runs.size() < 2) {
            return List.of();
        }
        if (target.equals(source)) {
            return runs;
        }
        return punctuationSplit(source, runs, target);
    }

    private static List<LineParts.Piece> punctuationSplit(String source,
                                                          List<LineParts.Piece> runs,
                                                          String target) {
        Style[] at = new Style[source.length()];
        int cursor = 0;
        for (LineParts.Piece run : runs) {
            int found = source.indexOf(run.text(), cursor);
            if (found < 0) {
                return List.of();
            }
            java.util.Arrays.fill(at, found, found + run.text().length(), run.style());
            cursor = found + run.text().length();
        }
        List<int[]> ours = punctuationSpans(source);
        List<int[]> theirs = punctuationSpans(target);
        if (!samePunctuation(source, ours, target, theirs)) {
            ours = bracketSpans(source);
            theirs = bracketSpans(target);
            if (!samePunctuation(source, ours, target, theirs)) {
                return List.of();
            }
        }
        List<LineParts.Piece> out = new ArrayList<>();
        int from = 0;
        int dstFrom = 0;
        for (int i = 0; i <= ours.size(); i++) {
            int to = i < ours.size() ? ours.get(i)[0] : source.length();
            int dstTo = i < theirs.size() ? theirs.get(i)[0] : target.length();
            String piece = target.substring(dstFrom, dstTo).strip();
            if (LineTranslator.hasContent(source.substring(from, to)) != LineTranslator.hasContent(piece)) {
                return List.of();
            }
            Style style = uniformStyle(source, at, from, to);
            if (style != null && LineTranslator.hasContent(piece)) {
                out.add(new LineParts.Piece(piece, style));
            }
            if (i < ours.size()) {
                Style mark = uniformStyle(source, at, ours.get(i)[0], ours.get(i)[1]);
                if (mark != null) {
                    out.add(new LineParts.Piece(
                            source.substring(ours.get(i)[0], ours.get(i)[1]), mark));
                } else {
                    out.addAll(mixedMark(source, at, ours.get(i), target, theirs.get(i)));
                }
                from = ours.get(i)[1];
                dstFrom = theirs.get(i)[1];
            }
        }
        return out;
    }

    private static List<LineParts.Piece> mixedMark(String source, Style[] at, int[] span,
                                                   String target, int[] dstSpan) {
        String mark = source.substring(span[0], span[1]);
        if (!halfWidthBrackets(mark).equals(
                halfWidthBrackets(target.substring(dstSpan[0], dstSpan[1])))) {
            return List.of();
        }
        List<LineParts.Piece> out = new ArrayList<>();
        int from = span[0];
        while (from < span[1]) {
            Style style = at[from];
            int to = from + 1;
            while (to < span[1] && java.util.Objects.equals(at[to], style)) {
                to++;
            }
            if (style == null) {
                return List.of();
            }
            out.add(new LineParts.Piece(source.substring(from, to), style));
            from = to;
        }
        return out;
    }

    private static List<int[]> punctuationSpans(String text) {
        List<int[]> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int start = i;
            while (i < text.length() && !Character.isLetterOrDigit(text.charAt(i))
                    && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            out.add(new int[] {start, i});
        }
        return out;
    }

    private static boolean samePunctuation(String source, List<int[]> ours,
                                           String target, List<int[]> theirs) {
        if (ours.isEmpty() || ours.size() != theirs.size()) {
            return false;
        }
        for (int i = 0; i < ours.size(); i++) {
            String a = halfWidthBrackets(source.substring(ours.get(i)[0], ours.get(i)[1]));
            String b = halfWidthBrackets(target.substring(theirs.get(i)[0], theirs.get(i)[1]));
            if (!a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    private static List<int[]> bracketSpans(String text) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(' || c == ')' || c == '（' || c == '）') {
                out.add(new int[] {i, i + 1});
            }
        }
        return out;
    }

    private static String halfWidthBrackets(String text) {
        return text.replace('（', '(').replace('）', ')');
    }

    private static Style uniformStyle(String text, Style[] at, int from, int to) {
        Style only = null;
        for (int c = from; c < to; c++) {
            if (Character.isWhitespace(text.charAt(c))) {
                continue;
            }
            if (at[c] == null || (only != null && !java.util.Objects.equals(only, at[c]))) {
                return null;
            }
            only = at[c];
        }
        return only;
    }

    private static void add(List<LineParts.Piece> out, String text, Style style,
                            Style blockStyle, List<LineParts.Piece> known) {
        String bare = PLACEHOLDER.matcher(text).replaceAll("").strip();
        if (!LineTranslator.hasContent(bare) || java.util.Objects.equals(style, blockStyle)
                || covered(known, bare, style)) {
            return;
        }
        out.add(new LineParts.Piece(bare, style));
    }

    private static Style fallback(Style dominant, List<LineParts.Piece> known, String row) {
        if (dominant == null) {
            return null;
        }
        for (LineParts.Piece piece : known) {
            if (!piece.text().isBlank() && row.contains(piece.text())) {
                return null;
            }
        }
        return dominant;
    }

    private record RowStyle(Style only, Style dominant) {}

    private static final java.util.regex.Pattern PLACEHOLDER =
            java.util.regex.Pattern.compile("\\{[^}]*\\}");

    private static List<RowStyle> uniformStyles(List<LineParts.Piece> runs, String template) {
        List<RowStyle> out = new ArrayList<>();
        int at = 0;
        int eaten = 0;
        int[] depth = {0};
        for (String line : template.split(LineTranslator.NL, -1)) {
            int need = solidCount(PLACEHOLDER.matcher(line).replaceAll(""));
            Style only = null;
            boolean mixed = false;
            boolean seen = false;
            Tally tally = new Tally();
            while (need > 0 && at < runs.size()) {
                LineParts.Piece run = runs.get(at);
                int have = solidCount(run.text()) - eaten;
                if (have <= 0) {
                    at++;
                    eaten = 0;
                    continue;
                }
                if (!seen) {
                    only = run.style();
                    seen = true;
                } else if (!java.util.Objects.equals(only, run.style())) {
                    mixed = true;
                }
                int take = Math.min(have, need);
                int[] local = {depth[0]};
                tally.add(run.style(), BlockTranslator.proseAmong(run.text(), eaten, take, local));
                need -= take;
                if (take == have) {
                    depth[0] = local[0];
                    at++;
                    eaten = 0;
                } else {
                    eaten += take;
                }
            }
            out.add(new RowStyle(mixed || !seen ? null : only, tally.top()));
        }
        return out;
    }

    static final class Tally {
        private final java.util.Map<Style, Integer> counts = new java.util.LinkedHashMap<>();
        private final java.util.Map<Style, Style> shown = new java.util.HashMap<>();

        void add(Style style, int solid) {
            if (style != null && solid > 0) {
                Style key = BlockTranslator.undecorated(style);
                counts.merge(key, solid, Integer::sum);
                Style seen = shown.get(key);
                if (seen == null || (hasDecoration(seen) && !hasDecoration(style))) {
                    shown.put(key, style);
                }
            }
        }

        void remove(Style style, int solid) {
            if (style == null || solid <= 0) {
                return;
            }
            Style key = BlockTranslator.undecorated(style);
            Integer have = counts.get(key);
            if (have == null) {
                return;
            }
            if (have <= solid) {
                counts.remove(key);
                shown.remove(key);
            } else {
                counts.put(key, have - solid);
            }
        }

        Style top() {
            Style best = null;
            int most = 0;
            for (java.util.Map.Entry<Style, Integer> e : counts.entrySet()) {
                if (e.getValue() > most) {
                    most = e.getValue();
                    best = e.getKey();
                }
            }
            return best == null ? null : shown.get(best);
        }

        private static boolean hasDecoration(Style style) {
            return style.isUnderlined() || style.isBold() || style.isItalic()
                    || style.isStrikethrough() || style.isObfuscated();
        }
    }

    static int solidCount(String text) {
        return (int) text.codePoints().filter(cp -> !Character.isWhitespace(cp)
                && !com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)).count();
    }

    private static List<RowStyle> perPartStyles(List<LineParts> parts) {
        List<RowStyle> out = new ArrayList<>();
        for (LineParts part : parts) {
            Style only = null;
            boolean mixed = false;
            boolean seen = false;
            Tally tally = new Tally();
            for (LineParts.Piece run : part.runs()) {
                if (!LineTranslator.hasContent(run.text())) {
                    continue;
                }
                tally.add(run.style(), solidCount(run.text()));
                if (!seen) {
                    only = run.style();
                    seen = true;
                } else if (!java.util.Objects.equals(only, run.style())) {
                    mixed = true;
                }
            }
            for (List<LineParts.Piece> pool
                    : List.of(part.numbers(), part.places(), part.users())) {
                for (LineParts.Piece piece : pool) {
                    tally.remove(piece.style(), solidCount(piece.text()));
                }
            }
            out.add(new RowStyle(mixed || !seen ? null : only, tally.top()));
        }
        return out;
    }

    private static boolean covered(List<LineParts.Piece> known, String text, Style style) {
        for (LineParts.Piece piece : known) {
            if (piece.text().equals(text)
                    && java.util.Objects.equals(piece.style(), style)) {
                return true;
            }
        }
        return false;
    }
}
