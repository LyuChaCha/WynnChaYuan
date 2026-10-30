package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.PlaceNames;
import com.wynnchayuan.translate.ColumnAligner.Piece;
import com.wynnchayuan.translate.SpaceRuns.Run;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

final class BlockTranslator {

    private BlockTranslator() {}

    static List<Component> translateBlock(List<StyledText> run,
                                          TranslationStore store,
                                          boolean[] centered) {
        return translateBlock(run, store, centered, LineParts::of);
    }

    static List<Component> translateBlock(List<StyledText> run,
                                          TranslationStore store,
                                          boolean[] centered,
                                          java.util.function.Function<StyledText, LineParts> partsOf) {
        if (run.size() < 2) {
            return null;
        }
        List<LineParts> parts = new ArrayList<>(run.size());
        StringBuilder key = new StringBuilder();
        boolean layoutRow = false;
        for (StyledText line : run) {
            LineParts p = partsOf.apply(line);
            if (p.template().isBlank()) {
                return null;
            }
            layoutRow |= p.template()
                    .replace(GlyphSplitter.GLYPH_PLACEHOLDER, "").isBlank();
            parts.add(p);
            if (key.length() > 0) {
                key.append('\n');
            }
            key.append(p.template());
        }
        String template = key.toString();
        if (!GlyphSplitter.hasLetter(template)) {
            return null;
        }
        String translated = store.lookup(template);
        List<LineParts.Piece> extra = List.of();
        List<LineParts.Piece> places = null;
        List<LineParts.Piece> glyphs = null;
        boolean flowed = false;
        boolean rewrapped = false;
        if ((translated == null || translated.isBlank()) && layoutRow) {
            return null;
        }
        if (translated == null || translated.isBlank()) {
            Flowed hit = lookupFlowedParts(template, store);
            if (hit == null) {
                Rejoined rejoined = rejoin(parts, dominantStyle(parts));
                if (rejoined != null) {
                    hit = lookupFlowedParts(rejoined.template(), store);
                    if (hit != null) {
                        places = rejoined.places();
                        glyphs = rejoined.glyphs();
                    }
                }
            }
            translated = hit == null ? null : hit.text();
            flowed = hit != null;
            if (hit != null && hit.label() != null) {
                extra = labelAccent(hit.label(), run.get(0));
            }
        }
        if (translated == null || translated.isBlank()) {
            return null;
        }
        FlowedDebug.note(run, extra.isEmpty() ? null : extra.get(0).text(),
                         labelStyleOf(run.get(0)), dominantStyle(parts));
        if (TextWrapper.needsWrap(translated, run.size(), flowed)) {
            String swapped = TermSubstitution.termsWithin(translated, store);
            if (swapped != null) {
                translated = swapped;
            }
            translated = wrapToBlock(translated, run);
            rewrapped = true;
        }
        translated = TermSubstitution.breakBeforeOriginal(translated, widestOf(run), store,
                                         piece -> LineTranslator.widthOf(Component.literal(piece)));
        String[] dst = translated.split("\n", -1);
        List<Component> built = TemplateRenderer.rebuildAll(dst, parts, extra, glyphs, places, store);
        if (built == null) {
            return null;
        }
        double centre = rewrapped || dst.length != run.size()
                ? centreLine(run, centered) : Double.NaN;
        if (!Double.isNaN(centre)) {
            List<Component> recentred = new ArrayList<>(built.size());
            for (Component one : built) {
                recentred.add(DisplayStyles.unslant(centreOn(one, centre)));
            }
            return recentred;
        }
        if (dst.length != run.size()) {
            List<Component> plain = new ArrayList<>(built.size());
            for (Component one : built) {
                plain.add(DisplayStyles.unslant(one));
            }
            return plain;
        }
        List<Component> out = new ArrayList<>(run.size());
        for (int i = 0; i < run.size(); i++) {
            Component row = built.get(i);
            out.add(DisplayStyles.unslant(rewrapped && hasInnerGap(row)
                    ? row : RowRealigner.realign(run.get(i), row, centered[i])));
        }
        return out;
    }

    private static double centreLine(List<StyledText> run, boolean[] centered) {
        if (centered == null || centered.length < run.size()) {
            return Double.NaN;
        }
        double sum = 0;
        boolean indented = false;
        for (int i = 0; i < run.size(); i++) {
            if (!centered[i]) {
                return Double.NaN;
            }
            Component line = run.get(i).getComponent();
            int lead = leadingPx(line);
            int w = LineTranslator.widthOf(line);
            if (lead < 0 || w - lead <= 0) {
                return Double.NaN;
            }
            indented |= lead > 0;
            sum += lead + (w - lead) / 2.0;
        }
        return indented ? sum / run.size() : Double.NaN;
    }

    private static Component centreOn(Component row, double centre) {
        List<Run> parts = SpaceRuns.runs(row);
        int lead = 0;
        int first = 0;
        while (first < parts.size() && parts.get(first).space()) {
            lead += parts.get(first).px();
            first++;
        }
        int content = LineTranslator.widthOf(row) - lead;
        if (content <= 0 || first == parts.size()) {
            return row;
        }
        int px = Math.max(0, (int) Math.round(centre - content / 2.0));
        MutableComponent out = Component.empty();
        String encoded = SpaceOffset.encode(px);
        if (!encoded.isEmpty()) {
            Style base = first > 0 ? parts.get(0).style() : parts.get(first).style();
            out.append(DisplayStyles.literal(encoded, SpaceOffset.styleFor(base)));
        }
        for (int i = first; i < parts.size(); i++) {
            out.append(DisplayStyles.literal(parts.get(i).text(), parts.get(i).style()));
        }
        return out;
    }

    private static int leadingPx(Component line) {
        int px = 0;
        for (Run r : SpaceRuns.runs(line)) {
            if (!r.space()) {
                break;
            }
            px += r.px();
        }
        return px;
    }

    private static boolean hasInnerGap(Component row) {
        boolean textBefore = false;
        boolean gapPending = false;
        for (Run r : RowRealigner.mergeSpaces(SpaceRuns.splitGaps(SpaceRuns.runs(row)))) {
            if (r.space()) {
                gapPending = textBefore;
            } else if (!r.text().isBlank()) {
                if (gapPending) {
                    return true;
                }
                textBefore = true;
            }
        }
        return false;
    }

    static void noteBlockMiss(String template, TranslationStore store) {
        FlowedDebug.miss(template, store);
    }

    record Flowed(String text, String label) {}

    record Rejoined(String template, List<LineParts.Piece> glyphs,
                    List<LineParts.Piece> places) {}

    private static Rejoined rejoin(List<LineParts> parts, Style style) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        StringBuilder joined = new StringBuilder();
        List<LineParts.Piece> kept = new ArrayList<>();
        List<LineParts.Piece> named = new ArrayList<>();
        boolean trimmed = false;
        for (LineParts part : parts) {
            String line = part.template();
            int at = 0;
            int lead = 0;
            while (line.startsWith(glyph, at)) {
                at += glyph.length();
                lead++;
            }
            if (at >= line.length()) {
                at = 0;
                lead = 0;
            }
            if (lead > 0) {
                trimmed = true;
            }
            int indent = TranslationStore.indentOf(line.substring(at));
            if (indent > 0 && at + indent < line.length()) {
                at += indent;
                trimmed = true;
            }
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(line, at, line.length());
            List<LineParts.Piece> own = part.glyphs();
            kept.addAll(own.subList(Math.min(lead, own.size()), own.size()));
            named.addAll(part.places());
        }

        String text = joined.toString();
        List<LineParts.Piece> merged = new ArrayList<>();
        boolean crossed = false;
        java.util.regex.Matcher place = PlaceNames.matcher(text);
        if (place != null) {
            StringBuilder out = new StringBuilder();
            int from = 0;
            int taken = 0;
            while (place.find()) {
                taken = carry(text, from, place.start(), named, taken, merged);
                out.append(text, from, place.start())
                   .append(GlyphSplitter.PLACE_PLACEHOLDER);
                merged.add(new LineParts.Piece(place.group(), style));
                crossed = true;
                from = place.end();
            }
            if (crossed) {
                carry(text, from, text.length(), named, taken, merged);
                text = out.append(text.substring(from)).toString();
            }
        }
        if (!crossed) {
            merged = named;
        }
        return trimmed || crossed
                ? new Rejoined(text, List.copyOf(kept), List.copyOf(merged)) : null;
    }

    private static int carry(String text, int from, int to,
                             List<LineParts.Piece> named, int taken,
                             List<LineParts.Piece> merged) {
        String mark = GlyphSplitter.PLACE_PLACEHOLDER;
        int at = text.indexOf(mark, from);
        while (at >= 0 && at < to) {
            if (taken < named.size()) {
                merged.add(named.get(taken++));
            }
            at = text.indexOf(mark, at + mark.length());
        }
        return taken;
    }

    static String lookupFlowed(String template, TranslationStore store) {
        Flowed flowed = lookupFlowedParts(template, store);
        return flowed == null ? null : flowed.text();
    }

    static Flowed lookupFlowedParts(String template, TranslationStore store) {
        String whole = store.lookupFlat(template);
        if (whole != null) {
            return new Flowed(whole, null);
        }
        int colon = template.indexOf(": ");
        if (colon <= 0 || colon > TextWrapper.MAX_LABEL_LENGTH) {
            return null;
        }
        String head = labelHead(template.substring(0, colon + 1), store);
        if (head == null) {
            return null;
        }
        String rest = template.substring(colon + 2);
        String tail = store.lookupFlat(rest);
        if (tail == null) {
            tail = store.lookup(rest);
        }
        return tail == null || tail.isBlank()
                ? null : new Flowed(joinLabel(head, tail), head);
    }

    static String labelHead(String label, TranslationStore store) {
        String hit = TemplateLookup.lookup(label, store);
        if (hit == null || hit.isBlank()) {
            hit = TemplateLookup.lookup(label.substring(0, label.length() - 1), store);
        }
        return hit == null || hit.isBlank() ? null : hit;
    }

    private static String joinLabel(String head, String tail) {
        String core = head.stripTrailing();
        return endsWithColon(core) ? core + " " + tail : head + ": " + tail;
    }

    private static boolean endsWithColon(String text) {
        return !text.isEmpty() && ColumnAligner.isTrailingColon(text.charAt(text.length() - 1));
    }

    static Style dominantStyle(List<LineParts> parts) {
        StyledRebuilder.Tally tally = new StyledRebuilder.Tally();
        int[] depth = {0};
        for (LineParts part : parts) {
            for (LineParts.Piece run : part.runs()) {
                if (isNote(run.text())) {
                    continue;
                }
                tally.add(run.style(), proseCount(run.text(), depth));
            }
        }
        for (LineParts part : parts) {
            for (List<LineParts.Piece> pool
                    : List.of(part.numbers(), part.places(), part.users())) {
                for (LineParts.Piece piece : pool) {
                    tally.remove(piece.style(), StyledRebuilder.solidCount(piece.text()));
                }
            }
        }
        Style top = tally.top();
        return undecorated(top == null ? parts.get(0).textStyle() : top);
    }

    static int proseAmong(String text, int skip, int take, int[] depth) {
        int seen = 0;
        int kept = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            boolean bracket = cp == '[' || cp == ']';
            if (cp == '[') {
                depth[0]++;
            } else if (cp == ']' && depth[0] > 0) {
                depth[0]--;
            }
            if (Character.isWhitespace(cp)
                    || com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)) {
                continue;
            }
            seen++;
            if (seen <= skip) {
                continue;
            }
            if (seen > skip + take) {
                break;
            }
            if (!bracket && depth[0] == 0) {
                kept++;
            }
        }
        return kept;
    }

    private static int proseCount(String text, int[] depth) {
        int solid = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '[') {
                depth[0]++;
                continue;
            }
            if (cp == ']') {
                if (depth[0] > 0) {
                    depth[0]--;
                }
                continue;
            }
            if (depth[0] > 0 || Character.isWhitespace(cp)
                    || com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)) {
                continue;
            }
            solid++;
        }
        return solid;
    }

    static void appendNoting(MutableComponent out, String text,
                             Style base, Style note, boolean[] depth,
                             List<LineParts.Piece> accents, boolean[] used,
                             TranslationStore store) {
        if (note == null) {
            TemplateRenderer.appendText(out, text, base, accents, used, store);
            return;
        }
        int from = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean opens = !depth[0] && (c == '(' || c == '（');
            boolean closes = depth[0] && (c == ')' || c == '）');
            if (!opens && !closes) {
                continue;
            }
            int cut = opens ? i : i + 1;
            if (cut > from) {
                TemplateRenderer.appendText(out, text.substring(from, cut),
                           depth[0] ? note : base, accents, used, store);
            }
            from = cut;
            depth[0] = opens;
        }
        if (from < text.length()) {
            TemplateRenderer.appendText(out, text.substring(from), depth[0] ? note : base,
                       accents, used, store);
        }
    }

    static boolean isNote(String text) {
        String core = text.strip();
        return core.startsWith("(") || core.startsWith("（");
    }

    static Style noteStyleOf(List<LineParts.Piece> runs, Style base) {
        for (LineParts.Piece run : runs) {
            if (isNote(run.text())) {
                Style style = undecorated(run.style());
                return style.equals(base) ? null : DisplayStyles.forDisplay(run.style());
            }
        }
        return null;
    }

    static Style undecorated(Style style) {
        return (style == null ? Style.EMPTY : style)
                .withUnderlined(false)
                .withBold(false)
                .withItalic(false)
                .withStrikethrough(false)
                .withObfuscated(false);
    }

    static void addStem(List<LineParts.Piece> out, String core,
                        Style style, String translated) {
        String stem = stemOf(core);
        if (stem == null || translated == null || translated.contains(core)
                || !standsAlone(translated, stem)) {
            return;
        }
        out.add(new LineParts.Piece(stem, style));
    }

    static String stemOf(String text) {
        for (String tail : new String[] {"’s", "'s"}) {
            if (text.endsWith(tail) && text.length() > tail.length() + 1) {
                return text.substring(0, text.length() - tail.length());
            }
        }
        return text.length() > MIN_STEM && text.endsWith("s") && !text.endsWith("ss")
                ? text.substring(0, text.length() - 1) : null;
    }

    static boolean standsAlone(String text, String word) {
        for (int at = text.indexOf(word); at >= 0; at = text.indexOf(word, at + 1)) {
            int after = at + word.length();
            boolean left = at == 0 || !TextWrapper.isWordChar(text.charAt(at - 1));
            boolean right = after >= text.length() || !TextWrapper.isWordChar(text.charAt(after));
            if (left && right) {
                return true;
            }
        }
        return false;
    }

    private static final int MIN_STEM = 4;

    private static List<LineParts.Piece> labelAccent(String label, StyledText firstLine) {
        String core = label;
        while (!core.isEmpty()) {
            if (core.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                core = core.substring(GlyphSplitter.GLYPH_PLACEHOLDER.length());
            } else if (Character.isWhitespace(core.charAt(0))
                    || TemplateLookup.isDecoration(core.codePointAt(0))) {
                core = core.substring(Character.charCount(core.codePointAt(0)));
            } else {
                break;
            }
        }
        if (core.isBlank()) {
            return List.of();
        }
        Style style = labelStyleOf(firstLine);
        if (style == null) {
            return List.of();
        }
        String bare = endsWithColon(core) ? core.substring(0, core.length() - 1) : core;
        return List.of(new LineParts.Piece(bare + ":", style),
                       new LineParts.Piece(core, style));
    }

    static Style labelStyleOf(StyledText firstLine) {
        for (StyledTextPart part : firstLine) {
            String raw = part.getString(null, StyleType.NONE);
            if (!GlyphSplitter.hasLetter(raw)) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            return ps == null ? Style.EMPTY : ps.getStyle();
        }
        return null;
    }

    private static int widestOf(List<StyledText> run) {
        int widest = 0;
        for (StyledText line : run) {
            widest = Math.max(widest, LineTranslator.widthOf(line.getComponent()));
        }
        return widest;
    }

    private static String wrapToBlock(String text, List<StyledText> run) {
        int width = widestOf(run);
        if (width <= 0) {
            return text;
        }
        return TextWrapper.wrapBalanced(text, width, run.size(),
                piece -> LineTranslator.widthOf(Component.literal(piece)));
    }

    private static String wrapToWidth(String text, int maxPx) {
        return TextWrapper.wrapToWidth(text, maxPx, piece -> LineTranslator.widthOf(Component.literal(piece)));
    }
}
