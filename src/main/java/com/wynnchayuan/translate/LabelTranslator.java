package com.wynnchayuan.translate;

import com.wynnchayuan.capture.Broadcasts;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

final class LabelTranslator {

    private LabelTranslator() {}

    static Component translateFloating(StyledText label, TranslationStore store) {
        LineParts parts = LineParts.of(label);
        String scoped = store.labelLookup(parts.template());
        if (scoped != null && !scoped.isBlank()) {
            Component rebuilt = rebuildLabel(label, scoped, parts, store);
            if (rebuilt != null) {
                return DisplayStyles.unslant(rebuilt);
            }
        }
        Component whole = floatingWhole(label, parts, store);
        if (whole != null) {
            return DisplayStyles.unslant(LineTranslator.dropIconSpaces(whole));
        }
        return LineTranslator.translate(label, store);
    }

    private static Component floatingWhole(StyledText label, LineParts parts,
                                           TranslationStore store) {
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        String translated = TemplateLookup.lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            translated = TemplateLookup.withoutSpacerRows(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = boardColours(label, parts, TemplateLookup.countdownBoard(parts.template(), store), store);
        }
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuildLabel(label, translated, parts, store);
        if (rebuilt == null) {
            return null;
        }
        Component result = RowRealigner.realign(label, rebuilt, true, false);
        LineDebug.record(label, result);
        return result;
    }

    private static Component rebuildLabel(StyledText label, String translated,
                                          LineParts parts, TranslationStore store) {
        String coloured = labelColours(label, parts, translated, store);
        Component rebuilt = TemplateRenderer.rebuild(coloured, parts, store);
        if (rebuilt == null && !coloured.equals(translated)) {
            rebuilt = TemplateRenderer.rebuild(translated, parts, store);
        }
        return rebuilt;
    }

    static Component translateLabel(StyledText label, TranslationStore store) {
        LineParts parts = LineParts.of(label);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        String translated = store.labelLookup(parts.template());
        boolean rowTail = trimsWholeRows(parts.template());
        if (translated == null || translated.isBlank()) {
            translated = rowTail ? TemplateLookup.withPercent(parts.template(), store, false)
                                 : TemplateLookup.lookup(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = TemplateLookup.withoutSpacerRows(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = boardColours(label, parts, TemplateLookup.countdownBoard(parts.template(), store), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = labelByLine(parts.template(), store, label);
        }
        if ((translated == null || translated.isBlank()) && rowTail) {
            translated = TemplateLookup.lookup(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            LineParts framed = ChatTranslator.framedOnMiss(label, parts, store);
            if (framed != null) {
                translated = store.labelLookup(framed.template());
                if (translated == null || translated.isBlank()) {
                    translated = TemplateLookup.lookup(framed.template(), store);
                }
                if (translated != null && !translated.isBlank()) {
                    parts = framed;
                }
            }
        }
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuildLabel(label, translated, parts, store);
        return rebuilt == null ? null : DisplayStyles.unslant(rebuilt);
    }

    static String labelColours(StyledText label, LineParts parts, String translated,
                               TranslationStore store) {
        if (label == null || translated == null || store == null
                || LABEL_COLOUR_TOKEN.matcher(translated).find()) {
            return translated;
        }
        List<List<LabelRun>> rows = labelRows(label);
        String[] dst = translated.split(LineTranslator.NL, -1);
        if (rows.size() != dst.length) {
            return translated;
        }
        List<Style> palette = ColourTemplate.palette(parts.runs());
        StringBuilder out = new StringBuilder(translated.length() + 16);
        boolean any = false;
        for (int i = 0; i < dst.length; i++) {
            if (i > 0) {
                out.append(LineTranslator.NL);
            }
            List<LabelRun> row = rows.get(i);
            if (distinctStyles(row) < 2) {
                out.append(dst[i]);
                continue;
            }
            List<int[]> regions = new ArrayList<>();
            List<Style> styles = new ArrayList<>();
            alignLabel(row, 0, row.size(), dst[i], 0, dst[i].length(), regions, styles, store);
            String coloured = paint(dst[i], regions, styles, palette);
            any |= !coloured.equals(dst[i]);
            out.append(coloured);
        }
        return any ? out.toString() : translated;
    }

    static String boardColours(StyledText label, LineParts parts, String composed,
                               TranslationStore store) {
        if (composed == null || label == null) {
            return composed;
        }
        String coloured = labelColours(label, parts, composed, store);
        List<List<LabelRun>> rows = labelRows(label);
        String[] dst = coloured.split(LineTranslator.NL, -1);
        if (rows.size() != dst.length) {
            return coloured;
        }
        List<Style> palette = ColourTemplate.palette(parts.runs());
        StringBuilder out = new StringBuilder(coloured.length() + 32);
        for (int i = 0; i < dst.length; i++) {
            if (i > 0) {
                out.append(LineTranslator.NL);
            }
            String row = dst[i];
            List<LabelRun> source = rows.get(i);
            if (!row.isBlank() && source.size() > 0 && distinctStyles(source) == 1
                    && !LABEL_COLOUR_TOKEN.matcher(row).find()) {
                row = paint(row, List.of(new int[] {0, row.length()}),
                            List.of(source.get(0).style()), palette);
            }
            out.append(row);
        }
        return out.toString();
    }

    private static final Pattern LABEL_COLOUR_TOKEN = Pattern.compile("\\{(?:c[^}]*|w\\d|/)}");

    private record LabelRun(String text, Style style) {}

    private static List<List<LabelRun>> labelRows(StyledText label) {
        List<List<LabelRun>> rows = new ArrayList<>();
        List<LabelRun> row = new ArrayList<>();
        for (StyledTextPart part : label) {
            if (GlyphSplitter.isGlyphPart(part)) {
                continue;
            }
            String raw = part.getString(null, StyleType.NONE);
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();
            String[] pieces = raw.split(LineTranslator.NL, -1);
            for (int p = 0; p < pieces.length; p++) {
                if (p > 0) {
                    rows.add(row);
                    row = new ArrayList<>();
                }
                String text = GlyphSplitter.stripGlyphChars(pieces[p]);
                if (text.isEmpty()) {
                    continue;
                }
                LabelRun last = row.isEmpty() ? null : row.get(row.size() - 1);
                if (last != null && (!LineTranslator.hasContent(text)
                        || Objects.equals(last.style(), style))) {
                    row.set(row.size() - 1, new LabelRun(last.text() + text, last.style()));
                } else if (LineTranslator.hasContent(text)) {
                    row.add(new LabelRun(text, style));
                }
            }
        }
        rows.add(row);
        return rows;
    }

    private static int distinctStyles(List<LabelRun> row) {
        Set<Style> seen = new HashSet<>();
        for (LabelRun run : row) {
            seen.add(run.style());
        }
        return seen.size();
    }

    private static void alignLabel(List<LabelRun> runs, int lo, int hi, String dst, int x, int y,
                                   List<int[]> regions, List<Style> styles,
                                   TranslationStore store) {
        while (x < y && Character.isWhitespace(dst.charAt(x))) {
            x++;
        }
        while (y > x && Character.isWhitespace(dst.charAt(y - 1))) {
            y--;
        }
        if (lo >= hi || x >= y) {
            return;
        }
        if (hi - lo == 1) {
            regions.add(new int[] {x, y});
            styles.add(runs.get(lo).style());
            return;
        }
        String window = dst.substring(x, y);
        for (int k = lo + 1; k < hi; k++) {
            String hit = labelLookup(joined(runs, k, hi), store);
            if (hit != null && hit.length() < window.length() && window.endsWith(hit)
                    && coversEnds(runs, k, hi, hit)) {
                int cut = claimHead(runs.get(k).text(), hit, dst, x, y - hit.length());
                alignLabel(runs, lo, k, dst, x, cut, regions, styles, store);
                alignLabel(runs, k, hi, dst, cut, y, regions, styles, store);
                return;
            }
        }
        for (int k = hi - 1; k > lo; k--) {
            String hit = labelLookup(joined(runs, lo, k), store);
            if (hit != null && hit.length() < window.length() && window.startsWith(hit)
                    && coversEnds(runs, lo, k, hit)) {
                int cut = claimTail(runs.get(k - 1).text(), hit, dst, x + hit.length(), y);
                alignLabel(runs, lo, k, dst, x, cut, regions, styles, store);
                alignLabel(runs, k, hi, dst, cut, y, regions, styles, store);
                return;
            }
        }
        List<int[]> anchors = new ArrayList<>();
        int cursor = x;
        for (int j = lo; j < hi; j++) {
            int[] at = findRun(runs.get(j).text().strip(), dst, cursor, y, store);
            if (at != null) {
                anchors.add(new int[] {j, at[0], at[1]});
                cursor = at[1];
            }
        }
        if (anchors.isEmpty()) {
            List<int[]> chunks = new ArrayList<>();
            int i = x;
            while (i < y) {
                while (i < y && Character.isWhitespace(dst.charAt(i))) {
                    i++;
                }
                int start = i;
                while (i < y && !Character.isWhitespace(dst.charAt(i))) {
                    i++;
                }
                if (i > start) {
                    chunks.add(new int[] {start, i});
                }
            }
            if (chunks.size() == hi - lo) {
                for (int k = 0; k < chunks.size(); k++) {
                    regions.add(chunks.get(k));
                    styles.add(runs.get(lo + k).style());
                }
            }
            return;
        }
        int prevRun = lo;
        int prevEnd = x;
        for (int[] anchor : anchors) {
            alignLabel(runs, prevRun, anchor[0], dst, prevEnd, anchor[1], regions, styles, store);
            regions.add(new int[] {anchor[1], anchor[2]});
            styles.add(runs.get(anchor[0]).style());
            prevRun = anchor[0] + 1;
            prevEnd = anchor[2];
        }
        alignLabel(runs, prevRun, hi, dst, prevEnd, y, regions, styles, store);
    }

    private static boolean coversEnds(List<LabelRun> runs, int from, int to, String hit) {
        if (to - from < 2) {
            return true;
        }
        for (int i : new int[] {from, to - 1}) {
            String core = runs.get(i).text().strip();
            if (!core.isEmpty() && core.codePoints().noneMatch(Character::isLetterOrDigit)
                    && !hit.contains(core)) {
                return false;
            }
        }
        return true;
    }

    private static String joined(List<LabelRun> runs, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(runs.get(i).text());
        }
        return sb.toString().strip();
    }

    private static int[] findRun(String core, String dst, int from, int to,
                                 TranslationStore store) {
        if (!LineTranslator.hasContent(core)) {
            return null;
        }
        int at = dst.indexOf(core, from);
        while (at >= 0 && at + core.length() <= to) {
            boolean letters = core.codePoints().anyMatch(Character::isLetter);
            int after = at + core.length();
            boolean whole = !letters
                    || ((at == 0 || !TextWrapper.isWordChar(dst.charAt(at - 1)))
                        && (after >= dst.length() || !TextWrapper.isWordChar(dst.charAt(after))));
            if (whole) {
                return new int[] {at, after};
            }
            at = dst.indexOf(core, at + 1);
        }
        String hit = labelLookup(core, store);
        if (hit != null) {
            int found = dst.indexOf(hit, from);
            if (found >= 0 && found + hit.length() <= to) {
                return new int[] {claimHead(core, hit, dst, from, found),
                                  claimTail(core, hit, dst, found + hit.length(), to)};
            }
        }
        return null;
    }

    private static int claimTail(String run, String hit, String dst, int end, int limit) {
        String core = run.strip();
        int i = core.length();
        while (i > 0 && isLabelSymbol(core.charAt(i - 1))) {
            i--;
        }
        String symbols = core.substring(i);
        if (symbols.isEmpty() || i == 0 || hit.endsWith(symbols)) {
            return end;
        }
        int at = end;
        while (at < limit && Character.isWhitespace(dst.charAt(at))) {
            at++;
        }
        return at + symbols.length() <= limit && dst.startsWith(symbols, at)
                ? at + symbols.length() : end;
    }

    private static int claimHead(String run, String hit, String dst, int limit, int start) {
        String core = run.strip();
        int i = 0;
        while (i < core.length() && isLabelSymbol(core.charAt(i))) {
            i++;
        }
        String symbols = core.substring(0, i);
        if (symbols.isEmpty() || i == core.length() || hit.startsWith(symbols)) {
            return start;
        }
        int at = start;
        while (at > limit && Character.isWhitespace(dst.charAt(at - 1))) {
            at--;
        }
        return at - symbols.length() >= limit && dst.startsWith(symbols, at - symbols.length())
                ? at - symbols.length() : start;
    }

    private static boolean isLabelSymbol(char c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
    }

    private static String labelLookup(String text, TranslationStore store) {
        if (!GlyphSplitter.hasLetter(text)) {
            return null;
        }
        String template = GlyphSplitter.toTemplate(StyledText.fromString(text));
        String hit = TemplateLookup.lookup(template, store);
        if (hit == null || hit.isBlank()) {
            hit = store.lookupTerm(template);
        }
        if (hit == null || hit.isBlank()) {
            hit = TermSubstitution.lookupWordCore(template, store);
        }
        if (hit == null || hit.isBlank()) {
            String plate = store.lookup(template + " " + GlyphSplitter.GLYPH_PLACEHOLDER
                                        + GlyphSplitter.GLYPH_PLACEHOLDER);
            String tail = GlyphSplitter.GLYPH_PLACEHOLDER + GlyphSplitter.GLYPH_PLACEHOLDER;
            if (plate != null && plate.strip().endsWith(tail)) {
                hit = plate.strip();
                hit = hit.substring(0, hit.length() - tail.length());
            }
        }
        if (hit == null) {
            return null;
        }
        hit = LABEL_COLOUR_TOKEN.matcher(hit).replaceAll("").strip();
        return LineTranslator.hasContent(hit) ? hit : null;
    }

    private static String paint(String row, List<int[]> regions, List<Style> styles,
                                List<Style> palette) {
        Integer[] order = new Integer[regions.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingInt(i -> regions.get(i)[0]));
        StringBuilder out = new StringBuilder(row.length() + 16);
        int at = 0;
        int current = 0;
        for (int i : order) {
            int[] region = regions.get(i);
            int slot = palette.indexOf(styles.get(i)) + 1;
            if (region[0] < at || slot < 1 || slot > 9) {
                continue;
            }
            if (region[0] > at) {
                String gap = row.substring(at, region[0]);
                if (current != 0 && LineTranslator.hasContent(gap)) {
                    out.append(ColourTemplate.COLOR_END);
                    current = 0;
                }
                out.append(gap);
            }
            if (slot != current) {
                out.append("{c").append(slot).append('}');
                current = slot;
            }
            out.append(row, region[0], region[1]);
            at = region[1];
        }
        if (at < row.length()) {
            String rest = row.substring(at);
            if (current != 0 && LineTranslator.hasContent(rest)) {
                out.append(ColourTemplate.COLOR_END);
                current = 0;
            }
            out.append(rest);
        }
        if (current != 0) {
            out.append(ColourTemplate.COLOR_END);
        }
        return out.toString();
    }

    static boolean hasWord(String text) {
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c) && !TermSubstitution.isHan(c)) {
                if (++run >= 2) {
                    return true;
                }
            } else {
                run = 0;
            }
        }
        return false;
    }

    private static String labelByLine(String template, TranslationStore store) {
        return labelByLine(template, store, null);
    }

    private static String labelByLine(String template, TranslationStore store, StyledText label) {
        if (template.indexOf(LineTranslator.NEWLINE) < 0) {
            return null;
        }
        String[] rows = template.split("\n", -1);
        StyledText[] styled = label == null ? null : label.split("\n");
        if (styled != null && styled.length != rows.length) {
            styled = null;
        }
        String[] out = new String[rows.length];
        boolean any = false;
        boolean owned = false;
        boolean missed = false;
        int status = statusFrom(rows);
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            if (!hasWord(row)) {
                out[i] = row;
                continue;
            }
            String hit = TemplateLookup.lookup(row, store);
            if (hit != null && !hit.isBlank()) {
                if (TemplateLookup.withOwner(row, store) != null) {
                    owned = true;
                }
                out[i] = hit;
                any = true;
                continue;
            }
            if (i >= status || !GlyphSplitter.hasWordBesidesStatus(row)) {
                out[i] = statusRow(row, store);
                continue;
            }
            String framed = styled == null ? null : framedRow(styled[i], row, store);
            if (framed != null) {
                owned = true;
                out[i] = framed;
                any = true;
                continue;
            }
            out[i] = row;
            missed = true;
        }
        if (!any || (missed && !owned)) {
            return null;
        }
        return String.join(String.valueOf(LineTranslator.NEWLINE), out);
    }

    private static String framedRow(StyledText row, String template, TranslationStore store) {
        if (row == null || !LineParts.of(row).template().equals(template)) {
            return null;
        }
        Broadcasts.Spans spans = store.nameFrames().find(row, template);
        if (spans == null) {
            return null;
        }
        String name = LineParts.flat(row).substring(spans.userStart(), spans.userEnd());
        if (name.chars().anyMatch(Character::isDigit)) {
            return null;
        }
        String hit = TemplateLookup.lookup(LineParts.of(row, spans).template(), store);
        String user = GlyphSplitter.PLAYER_PLACEHOLDER;
        if (hit == null || hit.isBlank() || hit.indexOf(user) != hit.lastIndexOf(user)
                || !hit.contains(user)) {
            return null;
        }
        return hit.replace(user, name);
    }

    private static String statusRow(String row, TranslationStore store) {
        String out = row;
        for (String word : GlyphSplitter.STATUS_WORDS) {
            if (!out.contains(word)) {
                continue;
            }
            String shown = store.lookup(word);
            if (shown != null && !shown.isBlank() && shown.indexOf('{') < 0) {
                out = out.replace(word, shown.strip());
            }
        }
        return out;
    }

    static int statusFrom(String[] rows) {
        int name = 0;
        while (name < rows.length && !hasWord(rows[name])) {
            name++;
        }
        int bar = name + 1;
        if (bar + 1 >= rows.length || !MOB_NAME_ROW.matcher(rows[name]).matches()
                || hasWord(rows[bar])
                || !rows[bar].contains(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            return rows.length;
        }
        return bar + 1;
    }

    private static final Pattern MOB_NAME_ROW = Pattern.compile(".*\\S\\s+(?:\\{#\\}){2,}");

    static boolean trimsWholeRows(String template) {
        int nl = template.lastIndexOf(LineTranslator.NEWLINE);
        if (nl < 0) {
            return false;
        }
        return TemplateLookup.trimEnd(template, 0, GlyphSplitter.GLYPH_PLACEHOLDER, true)
                <= nl;
    }
}
