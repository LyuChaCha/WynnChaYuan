package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.PlaceNames;
import net.minecraft.client.Minecraft;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import com.wynnchayuan.translate.ColumnAligner.Piece;
import com.wynnchayuan.translate.SpaceRuns.Run;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

public final class LineTranslator {

    private LineTranslator() {}

    public static List<Component> translateBlock(List<StyledText> run,
                                                 TranslationStore store,
                                                 boolean[] centered) {
        return translateBlock(run, store, centered, LineParts::of);
    }

    public static List<Component> translateBlock(List<StyledText> run,
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
            String swapped = termsWithin(translated, store);
            if (swapped != null) {
                translated = swapped;
            }
            translated = wrapToBlock(translated, run);
            rewrapped = true;
        }
        translated = breakBeforeOriginal(translated, widestOf(run), store,
                                         piece -> widthOf(Component.literal(piece)));
        String[] dst = translated.split("\n", -1);
        List<Component> built = rebuildAll(dst, parts, extra, glyphs, places, store);
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
            int w = widthOf(line);
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
        int content = widthOf(row) - lead;
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

    public static void noteBlockMiss(String template, TranslationStore store) {
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
        String hit = lookup(label, store);
        if (hit == null || hit.isBlank()) {
            hit = lookup(label.substring(0, label.length() - 1), store);
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
        Tally tally = new Tally();
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
                    tally.remove(piece.style(), solidCount(piece.text()));
                }
            }
        }
        Style top = tally.top();
        return undecorated(top == null ? parts.get(0).textStyle() : top);
    }

    private static int proseAmong(String text, int skip, int take, int[] depth) {
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

    private static void appendNoting(MutableComponent out, String text,
                                     Style base, Style note, boolean[] depth,
                                     List<LineParts.Piece> accents, boolean[] used,
                                     TranslationStore store) {
        if (note == null) {
            appendText(out, text, base, accents, used, store);
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
                appendText(out, text.substring(from, cut),
                           depth[0] ? note : base, accents, used, store);
            }
            from = cut;
            depth[0] = opens;
        }
        if (from < text.length()) {
            appendText(out, text.substring(from), depth[0] ? note : base,
                       accents, used, store);
        }
    }

    private static boolean isNote(String text) {
        String core = text.strip();
        return core.startsWith("(") || core.startsWith("（");
    }

    private static Style noteStyleOf(List<LineParts.Piece> runs, Style base) {
        for (LineParts.Piece run : runs) {
            if (isNote(run.text())) {
                Style style = undecorated(run.style());
                return style.equals(base) ? null : DisplayStyles.forDisplay(run.style());
            }
        }
        return null;
    }

    private static Style undecorated(Style style) {
        return (style == null ? Style.EMPTY : style)
                .withUnderlined(false)
                .withBold(false)
                .withItalic(false)
                .withStrikethrough(false)
                .withObfuscated(false);
    }

    private static void addStem(List<LineParts.Piece> out, String core,
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
                    || isDecoration(core.codePointAt(0))) {
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
            widest = Math.max(widest, widthOf(line.getComponent()));
        }
        return widest;
    }

    private static String wrapToBlock(String text, List<StyledText> run) {
        int width = widestOf(run);
        if (width <= 0) {
            return text;
        }
        return TextWrapper.wrapBalanced(text, width, run.size(),
                piece -> widthOf(Component.literal(piece)));
    }

    private static String wrapToWidth(String text, int maxPx) {
        return TextWrapper.wrapToWidth(text, maxPx, piece -> widthOf(Component.literal(piece)));
    }

    static final char NEWLINE = '\n';

    public static Component translate(StyledText line, TranslationStore store) {
        return translate(line, store, true);
    }

    public static Component translate(StyledText line, TranslationStore store,
                                      boolean centered) {
        return translate(line, store, centered, false);
    }

    public static Component translate(StyledText line, TranslationStore store,
                                      boolean centered, boolean leftAligned) {
        Component whole = translateWholeLine(line, store, centered, leftAligned);
        if (whole != null) {
            return sameTextKeepsStyle(line, keepClick(line, DisplayStyles.unslant(dropIconSpaces(whole))));
        }
        Component perLine = translatePerLine(line, store, centered);
        return sameTextKeepsStyle(line, keepClick(line, DisplayStyles.unslant(dropIconSpaces(
                perLine != null ? perLine
                        : translateSegments(line, store, centered, leftAligned)))));
    }

    private static Component sameTextKeepsStyle(StyledText line, Component out) {
        if (out == null) {
            return null;
        }
        Component before = line.getComponent();
        return out.getString().equals(before.getString()) ? before : out;
    }

    static Component keepClick(StyledText original, Component translated) {
        if (original == null || translated == null) {
            return translated;
        }
        ClickEvent only = soleClick(original.getComponent());
        if (only == null || hasClick(translated)) {
            return translated;
        }
        return translated.copy().withStyle(s -> s.withClickEvent(only));
    }

    private static ClickEvent soleClick(Component line) {
        java.util.List<ClickEvent> found = new java.util.ArrayList<>();
        line.visit((style, text) -> {
            ClickEvent click = style.getClickEvent();
            if (click != null && !found.contains(click)) {
                found.add(click);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found.size() == 1 ? found.get(0) : null;
    }

    private static boolean hasClick(Component line) {
        Boolean[] any = {Boolean.FALSE};
        line.visit((style, text) -> {
            if (style.getClickEvent() != null) {
                any[0] = Boolean.TRUE;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return any[0];
    }

    static Component dropIconSpaces(Component line) {
        if (line == null) {
            return null;
        }
        boolean[] dropped = {false};
        boolean[] seenText = {false};
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            if (text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (!seenText[0] && text.isBlank() && iconFont(style)) {
                dropped[0] = true;
                return java.util.Optional.empty();
            }
            if (text.codePoints().anyMatch(Character::isLetterOrDigit)
                    && !SpaceOffset.isSpaceFont(style) && !iconFont(style)) {
                seenText[0] = true;
            }
            out.append(Component.literal(text).withStyle(style));
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return dropped[0] ? out : line;
    }

    private static boolean iconFont(Style style) {
        if (!(style.getFont() instanceof net.minecraft.network.chat.FontDescription.Resource r)
                || r.id() == null) {
            return false;
        }
        String path = r.id().getPath();
        return !path.equals("default") && !path.equals("uniform") && !path.equals("space")
                && !path.equals("language/wynncraft");
    }

    public static Component translateKnown(StyledText line, String translated,
                                           TranslationStore store) {
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuild(translated, LineParts.of(line), store);
        return rebuilt == null ? null : DisplayStyles.unslant(RowRealigner.realign(line, rebuilt, true));
    }

    private static Component translateSegments(StyledText line, TranslationStore store,
                                               boolean centered, boolean leftAligned) {
        List<Piece> pieces = new ArrayList<>();
        boolean any = false;
        boolean percent = hasPercentValue(line);
        List<Integer> connectorAt = new ArrayList<>();

        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();

            if (SpaceRuns.isAdjustableSpace(style, raw)) {
                pieces.add(Piece.space(SpaceOffset.decode(raw), style));
                continue;
            }
            if (GlyphSplitter.isGlyphPart(part) || raw.isBlank()) {
                pieces.add(Piece.text(raw, style));
                continue;
            }
            String tail = SpaceOffset.trailingOffsets(raw);
            String body = raw.substring(0, raw.length() - tail.length());
            String lead = SpaceOffset.leadingOffsets(body);
            if (lead.length() == body.length() || !SpaceRuns.isAdjustableSpace(style, lead)) {
                lead = "";
            }
            body = body.substring(lead.length());
            if (!lead.isEmpty()) {
                pieces.add(Piece.space(SpaceOffset.decode(lead), SpaceOffset.styleFor(style)));
            }

            Component replaced = body.isEmpty()
                    ? null : translateOneSegment(body, style, store, percent);
            if (replaced == null) {
                if (!body.isEmpty() && isConnectorSegment(body)) {
                    connectorAt.add(pieces.size());
                }
                pieces.add(Piece.text(raw.substring(lead.length()), style));
                continue;
            }
            any = true;
            Component orig = DisplayStyles.literal(body, style);
            pieces.add(Piece.translated(
                    com.wynnchayuan.render.PairedFont.absolutelyPositioned(style)
                            ? padSlot(replaced, orig, style) : replaced,
                    orig));
            if (!tail.isEmpty()) {
                pieces.add(Piece.space(SpaceOffset.decode(tail),
                        SpaceOffset.styleFor(style)));
            }
        }
        if (!any) {
            return null;
        }
        translateConnectors(pieces, connectorAt, store);

        int boundary = ColumnAligner.findAlignPoint(pieces);
        if (boundary >= 0 && !pieces.get(boundary).isSpace()
                && ColumnAligner.hasColumnGap(pieces, boundary)) {
            pieces.add(boundary, Piece.space(0,
                    SpaceOffset.styleFor(pieces.get(boundary).style())));
        }

        List<Piece> columns = ColumnAligner.alignColumns(pieces, leftAligned);
        List<Piece> aligned = leftAligned ? columns : ColumnAligner.settle(columns, line);
        LineDebug.pieces("逐片段 " + line.getStringWithoutFormatting(),
                ColumnAligner.describe(pieces, aligned, line));

        if (centered && ColumnAligner.textGroups(pieces).size() >= 2) {
            return ColumnAligner.assemble(ColumnAligner.recenterColumns(pieces), -1, 0);
        }

        int lead = centered ? ColumnAligner.leadingSpace(aligned) : -1;
        if (lead >= 0) {
            int delta = widthOf(line) - widthOf(ColumnAligner.assemble(aligned, -1, 0));
            return ColumnAligner.assemble(aligned, lead, delta / 2);
        }
        return ColumnAligner.assemble(aligned, -1, 0);
    }

    public static int width(Component component) {
        return widthOf(component);
    }

    static int widthOf(Component component) {
        if (measureForTest != null) {
            return measureForTest.applyAsInt(component);
        }
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.font == null ? 0 : mc.font.width(component);
    }

    static ToIntFunction<Component> measureForTest;

    static int widthOf(StyledText line) {
        return widthOf(line.getComponent());
    }

    private static boolean hasPercentValue(StyledText line) {
        for (StyledTextPart part : line) {
            String text = part.getString(null, StyleType.NONE).strip();
            if (text.isEmpty() || !VALUE.matcher(text).matches()) {
                continue;
            }
            if (text.endsWith("%")) {
                return true;
            }
        }
        return false;
    }

    private static final java.util.regex.Pattern VALUE =
            java.util.regex.Pattern.compile(
                    "[+-]?\\d[\\d,.]*%?(?:\\s+to\\s+[+-]?\\d[\\d,.]*%?)?");

    public static Component translateChunk(String raw, Style style,
                                           TranslationStore store) {
        return translateOneSegment(raw, style, store, false);
    }

    private static Component translateOneSegment(String raw, Style style,
                                                 TranslationStore store,
                                                 boolean percent) {
        String core = raw.strip();
        if (core.isEmpty() || !GlyphSplitter.hasLetter(core)) {
            return null;
        }
        if (TranslationStore.indentOf(core) == core.length()) {
            return null;
        }
        int lead = raw.indexOf(core.charAt(0));
        String prefix = raw.substring(0, Math.max(0, lead));
        String suffix = raw.substring(prefix.length() + core.length());

        StyledText one = StyledText.fromComponent(Component.literal(core).withStyle(style));
        LineParts parts = LineParts.of(one);
        String hit = lookup(parts.template(), store, percent);
        if (hit == null) {
            return null;
        }
        Component rebuilt = rebuild(hit, parts, store);
        if (rebuilt == null) {
            return null;
        }
        MutableComponent out = Component.empty();
        if (!prefix.isEmpty()) {
            out.append(DisplayStyles.literal(prefix, style));
        }
        out.append(rebuilt);
        String tail = reattach(rebuilt.getString(), suffix);
        if (!tail.isEmpty()) {
            out.append(DisplayStyles.literal(tail, style));
        }
        return out;
    }

    private static Component translateWholeLine(StyledText line, TranslationStore store,
                                                boolean centered, boolean leftAligned) {
        LineParts parts = LineParts.of(line);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        String translated = lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            LineParts framed = ChatTranslator.framedOnMiss(line, parts, store);
            translated = framed == null ? null : lookup(framed.template(), store);
            if (translated == null || translated.isBlank()) {
                return null;
            }
            parts = framed;
        }
        Component rebuilt = rebuild(translated, parts, store);
        if (rebuilt == null) {
            return null;
        }
        Component result = RowRealigner.realign(line, rebuilt, centered, leftAligned);
        LineDebug.record(line, result);
        return result;
    }

    private static Component translatePerLine(StyledText line, TranslationStore store,
                                              boolean centered) {
        LineParts parts = LineParts.of(line);
        String template = parts.template();
        if (template.indexOf(NEWLINE) < 0) {
            return null;
        }
        String[] rows = template.split("\n", -1);
        StringBuilder joined = new StringBuilder(template.length());
        boolean any = false;
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                joined.append(NEWLINE);
            }
            String row = rows[i];
            String hit = GlyphSplitter.hasLetter(row) ? lookup(row, store) : null;
            if (hit != null && !hit.isBlank()) {
                joined.append(hit);
                any = true;
            } else {
                joined.append(row);
            }
        }
        if (!any) {
            return null;
        }
        Component rebuilt = rebuild(joined.toString(), parts, store);
        if (rebuilt == null) {
            return null;
        }
        Component result = RowRealigner.realign(line, rebuilt, centered);
        LineDebug.record(line, result);
        return result;
    }

    public static Component translateFloating(StyledText label, TranslationStore store) {
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
            return DisplayStyles.unslant(dropIconSpaces(whole));
        }
        return translate(label, store);
    }

    private static Component floatingWhole(StyledText label, LineParts parts,
                                           TranslationStore store) {
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        String translated = lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            translated = withoutSpacerRows(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = boardColours(label, parts, countdownBoard(parts.template(), store), store);
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
        Component rebuilt = rebuild(coloured, parts, store);
        if (rebuilt == null && !coloured.equals(translated)) {
            rebuilt = rebuild(translated, parts, store);
        }
        return rebuilt;
    }

    public static Component translateLabel(StyledText label, TranslationStore store) {
        LineParts parts = LineParts.of(label);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        String translated = store.labelLookup(parts.template());
        boolean rowTail = trimsWholeRows(parts.template());
        if (translated == null || translated.isBlank()) {
            translated = rowTail ? withPercent(parts.template(), store, false)
                                 : lookup(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = withoutSpacerRows(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = boardColours(label, parts, countdownBoard(parts.template(), store), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = labelByLine(parts.template(), store, label);
        }
        if ((translated == null || translated.isBlank()) && rowTail) {
            translated = lookup(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            LineParts framed = ChatTranslator.framedOnMiss(label, parts, store);
            if (framed != null) {
                translated = store.labelLookup(framed.template());
                if (translated == null || translated.isBlank()) {
                    translated = lookup(framed.template(), store);
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
        String[] dst = translated.split(NL, -1);
        if (rows.size() != dst.length) {
            return translated;
        }
        List<Style> palette = ColourTemplate.palette(parts.runs());
        StringBuilder out = new StringBuilder(translated.length() + 16);
        boolean any = false;
        for (int i = 0; i < dst.length; i++) {
            if (i > 0) {
                out.append(NL);
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
        String[] dst = coloured.split(NL, -1);
        if (rows.size() != dst.length) {
            return coloured;
        }
        List<Style> palette = ColourTemplate.palette(parts.runs());
        StringBuilder out = new StringBuilder(coloured.length() + 32);
        for (int i = 0; i < dst.length; i++) {
            if (i > 0) {
                out.append(NL);
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

    private static final java.util.regex.Pattern LABEL_COLOUR_TOKEN =
            java.util.regex.Pattern.compile("\\{(?:c[^}]*|w\\d|/)}");

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
            String[] pieces = raw.split(NL, -1);
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
                if (last != null && (!hasContent(text)
                        || java.util.Objects.equals(last.style(), style))) {
                    row.set(row.size() - 1, new LabelRun(last.text() + text, last.style()));
                } else if (hasContent(text)) {
                    row.add(new LabelRun(text, style));
                }
            }
        }
        rows.add(row);
        return rows;
    }

    private static int distinctStyles(List<LabelRun> row) {
        java.util.Set<Style> seen = new java.util.HashSet<>();
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
        if (!hasContent(core)) {
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
        String hit = lookup(template, store);
        if (hit == null || hit.isBlank()) {
            hit = store.lookupTerm(template);
        }
        if (hit == null || hit.isBlank()) {
            hit = lookupWordCore(template, store);
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
        return hasContent(hit) ? hit : null;
    }

    private static String paint(String row, List<int[]> regions, List<Style> styles,
                                List<Style> palette) {
        Integer[] order = new Integer[regions.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, java.util.Comparator.comparingInt(i -> regions.get(i)[0]));
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
                if (current != 0 && hasContent(gap)) {
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
            if (current != 0 && hasContent(rest)) {
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

    private static boolean hasWord(String text) {
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c) && !isHan(c)) {
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
        if (template.indexOf(NEWLINE) < 0) {
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
            String hit = lookup(row, store);
            if (hit != null && !hit.isBlank()) {
                if (withOwner(row, store) != null) {
                    owned = true;
                }
                out[i] = hit;
                any = true;
                continue;
            }
            if (i >= status || !com.wynnchayuan.capture.GlyphSplitter.hasWordBesidesStatus(row)) {
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
        return String.join(String.valueOf(NEWLINE), out);
    }

    private static String framedRow(StyledText row, String template, TranslationStore store) {
        if (row == null || !LineParts.of(row).template().equals(template)) {
            return null;
        }
        com.wynnchayuan.capture.Broadcasts.Spans spans = store.nameFrames().find(row, template);
        if (spans == null) {
            return null;
        }
        String name = LineParts.flat(row).substring(spans.userStart(), spans.userEnd());
        if (name.chars().anyMatch(Character::isDigit)) {
            return null;
        }
        String hit = lookup(LineParts.of(row, spans).template(), store);
        String user = GlyphSplitter.PLAYER_PLACEHOLDER;
        if (hit == null || hit.isBlank() || hit.indexOf(user) != hit.lastIndexOf(user)
                || !hit.contains(user)) {
            return null;
        }
        return hit.replace(user, name);
    }

    private static String statusRow(String row, TranslationStore store) {
        String out = row;
        for (String word : com.wynnchayuan.capture.GlyphSplitter.STATUS_WORDS) {
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
                || !rows[bar].contains(com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER)) {
            return rows.length;
        }
        return bar + 1;
    }

    private static final java.util.regex.Pattern MOB_NAME_ROW =
            java.util.regex.Pattern.compile(".*\\S\\s+(?:\\{#\\}){2,}");

    static boolean trimsWholeRows(String template) {
        int nl = template.lastIndexOf(NEWLINE);
        if (nl < 0) {
            return false;
        }
        return trimEnd(template, 0, com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER, true)
                <= nl;
    }

    static String withoutSpacerRows(String template, TranslationStore store) {
        if (template == null) {
            return null;
        }
        java.util.regex.Matcher m = SPACER_ROWS.matcher(template);
        if (!m.matches()) {
            return null;
        }
        String body = m.group(1);
        String hit = store.labelLookup(body);
        if (hit == null || hit.isBlank()) {
            hit = lookup(body, store);
        }
        return hit == null || hit.isBlank() ? null : hit + m.group(2);
    }

    private static final java.util.regex.Pattern SPACER_ROWS = java.util.regex.Pattern.compile(
            "(?s)(.*\\S)((?:\n[ \t]*À[ \tÀ]*)+)");

    static String lookup(String template, TranslationStore store) {
        return lookupTrimmed(template, store, false);
    }

    static String lookup(String template, TranslationStore store, boolean percent) {
        String hit = lookupTrimmed(template, store, percent);
        if (hit == null) {
            hit = statRow(template, store, percent);
        }
        return hit != null ? hit : nameRow(template, store);
    }

    private static String nameRow(String template, TranslationStore store) {
        int start = 0;
        while (start < template.length()
                && Character.isWhitespace(template.charAt(start))) {
            start++;
        }
        int body = start;
        for (int next = bulletEnd(template, body); next > body;
                next = bulletEnd(template, body)) {
            body = next;
        }
        String rest = template.substring(body);
        String head = template.substring(0, body);

        int colon = labelEnd(rest);
        if (colon > 0) {
            String label = rest.substring(0, colon);
            String zhLabel = lookupTrimmed(label, store, false);
            int name = colon;
            while (name < rest.length() && rest.charAt(name) == ' ') {
                name++;
            }
            String tail = rest.substring(name);
            if (zhLabel != null && !zhLabel.isBlank() && !tail.isBlank()) {
                String zhName = TYPED_LABELS.contains(label.strip())
                        ? tail : lookupTrimmed(tail, store, false);
                if ((zhName == null || zhName.isBlank())
                        && NAME_LABELS.contains(label.strip())) {
                    zhName = tail;
                }
                if (zhName != null && !zhName.isBlank()) {
                    return head + zhLabel
                            + reattach(zhLabel, rest.substring(colon, name)) + zhName;
                }
            }
        }
        if (body == start) {
            return null;
        }
        String zh = lookupTrimmed(rest, store, false);
        return zh == null || zh.isBlank() ? null : head + zh;
    }

    private static final java.util.Set<String> NAME_LABELS = java.util.Set.of(
            "Required Ability:", "Unlocked Ability:",
            "Cave:",
            "Name Contains:");

    private static final java.util.Set<String> TYPED_LABELS = java.util.Set.of(
            "Name Contains:");

    private static int bulletEnd(String template, int at) {
        if (at >= template.length()) {
            return at;
        }
        int cp = template.codePointAt(at);
        boolean bullet = BULLETS.indexOf(cp) >= 0;
        if (!bullet && !isDecoration(cp)) {
            return at;
        }
        int next = at + Character.charCount(cp);
        int spaced = next;
        while (spaced < template.length() && template.charAt(spaced) == ' ') {
            spaced++;
        }
        if (bullet && spaced == next) {
            return at;
        }
        return spaced;
    }

    private static int labelEnd(String rest) {
        int at = rest.indexOf(':');
        return at > 0 && at + 1 < rest.length() && rest.charAt(at + 1) == ' '
                ? at + 1 : -1;
    }

    private static final String BULLETS = "-–—•*";

    private static String statRow(String template, TranslationStore store, boolean percent) {
        int from = valueTailStart(template);
        if (from <= 0) {
            return null;
        }
        String label = template.substring(0, from);
        if (!GlyphSplitter.hasLetter(label)) {
            return null;
        }
        String tail = template.substring(from);
        String whole = untruncated(label, tail, store);
        if (whole != null) {
            tail = tail.substring(tail.indexOf(TRUNCATED) + TRUNCATED.length());
            label = whole;
            if (tail.isBlank()) {
                String zh = lookupTrimmed(label, store, percent);
                return zh == null || zh.isBlank() ? null : zh + tail;
            }
        }
        if (store.itemNameOnly(label)) {
            if (colonFirst(tail)) {
                return itemAsLabel(label, tail, store);
            }
            if (!countFirst(tail)) {
                return null;
            }
        }
        if (!tail.isEmpty()) {
            boolean was = TranslationStore.itemNamesBarred();
            TranslationStore.barItemNames(was || !countFirst(tail));
            String zh;
            try {
                zh = statLabel(label, store, percent);
            } finally {
                TranslationStore.barItemNames(was);
            }
            if (zh != null) {
                return zh + translateTail(tail, store);
            }
            if (colonFirst(tail)) {
                String own = itemAsLabel(label, tail, store);
                if (own != null) {
                    return own;
                }
            }
        }
        int head = valueHeadEnd(label);
        String lead = label.substring(0, head);
        if (lead.indexOf(GlyphSplitter.NUMBER_PLACEHOLDER) < 0) {
            return null;
        }
        String name = label.substring(head);
        if (tail.isEmpty() && !store.isUiLabel(name.strip())) {
            return null;
        }
        String zh = statLabel(name, store, percent);
        return zh == null ? null : translateTail(lead, store) + zh + translateTail(tail, store);
    }

    // WynnMod 標籤放不下時會砍尾並接上 ..
    private static final String TRUNCATED = "..";

    private static final int MIN_BARE_TRUNCATED = 10;

    private static String untruncated(String label, String tail, TranslationStore store) {
        int dots = 0;
        while (dots < tail.length() && Character.isWhitespace(tail.charAt(dots))) {
            dots++;
        }
        if (!tail.startsWith(TRUNCATED, dots)) {
            return null;
        }
        String after = tail.substring(dots + TRUNCATED.length());
        boolean bare = after.isBlank();
        if (!bare && after.indexOf(GlyphSplitter.NUMBER_PLACEHOLDER) < 0) {
            return null;
        }
        int start = 0;
        while (start < label.length()) {
            if (label.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER, start)) {
                start += GlyphSplitter.GLYPH_PLACEHOLDER.length();
            } else if (Character.isWhitespace(label.charAt(start))) {
                start++;
            } else {
                break;
            }
        }
        String head = label.substring(start) + tail.substring(0, dots);
        if (bare && head.strip().length() < MIN_BARE_TRUNCATED) {
            return null;
        }
        String whole = store.completeUiLabel(head);
        return whole == null ? null : label.substring(0, start) + whole;
    }

    private static boolean countFirst(String tail) {
        String t = tail.stripLeading();
        return !t.isEmpty() && (t.charAt(0) == '[' || t.charAt(0) == '(');
    }

    private static boolean colonFirst(String tail) {
        String t = tail.stripLeading();
        return !t.isEmpty() && ColumnAligner.isTrailingColon(t.charAt(0));
    }

    private static String itemAsLabel(String label, String tail, TranslationStore store) {
        String word = label.strip();
        String own = store.lookupExact(word + ":");
        if (own == null) {
            return null;
        }
        String lead = label.substring(0, label.indexOf(word));
        String t = tail.stripLeading();
        String rest = t.substring(1);
        return lead + own + translateTail(reattach(own, rest), store);
    }

    private static int valueHeadEnd(String label) {
        int at = 0;
        while (at < label.length()) {
            char c = label.charAt(at);
            if (c == '{') {
                int close = label.indexOf('}', at);
                if (close < 0) {
                    break;
                }
                at = close + 1;
            } else if (c != '}' && isValueChar(c)) {
                at++;
            } else {
                break;
            }
        }
        return at;
    }

    private static String statLabel(String label, TranslationStore store, boolean percent) {
        String zh = lookupTrimmed(label + (percent ? "%" : " Raw"), store, false);
        if (zh == null || zh.isBlank()) {
            zh = lookupTrimmed(label, store, percent);
        }
        return zh == null || zh.isBlank() ? null : zh;
    }

    private static int valueTailStart(String template) {
        int at = template.length();
        while (at > 0) {
            char last = template.charAt(at - 1);
            if (last == '}') {
                int open = template.lastIndexOf('{', at - 1);
                if (open < 0) {
                    break;
                }
                at = open;
            } else if (isValueChar(last)) {
                at--;
            } else {
                int word = tailWordStart(template, at);
                if (word < 0) {
                    break;
                }
                at = word;
            }
        }
        return at;
    }

    private static int tailWordStart(String template, int at) {
        int start = at;
        while (start > 0 && Character.isLetter(template.charAt(start - 1))) {
            start--;
        }
        return start < at && CONNECTOR_KEYS.containsKey(template.substring(start, at))
                ? start : -1;
    }

    private static String translateTail(String tail, TranslationStore store) {
        StringBuilder out = new StringBuilder(tail.length());
        int at = 0;
        while (at < tail.length()) {
            if (!Character.isLetter(tail.charAt(at))) {
                out.append(tail.charAt(at));
                at++;
                continue;
            }
            int end = at;
            while (end < tail.length() && Character.isLetter(tail.charAt(end))) {
                end++;
            }
            String word = tail.substring(at, end);
            String local = connector(word, store);
            out.append(local != null ? local : word);
            at = end;
        }
        return out.toString();
    }

    private static boolean isValueChar(char c) {
        return Character.isDigit(c) || Character.isWhitespace(c)
                || "+-±%/[](){}<>,.:~*★⇧⇩⇨❤✦✤✹✽✧'\"".indexOf(c) >= 0;
    }

    private static final java.util.Map<String, String> CONNECTOR_KEYS = java.util.Map.of(
            "to", "{~} to {~}", "tier", "{~} tier", "tiers", "{~} tier");

    private static final java.util.regex.Pattern CONNECTOR_SLOT =
            java.util.regex.Pattern.compile("\\{~\\d*}");

    static String connector(String word, TranslationStore store) {
        String key = CONNECTOR_KEYS.get(word);
        if (key == null || store == null) {
            return null;
        }
        String dst = store.lookup(key);
        if (dst == null || dst.isBlank()) {
            return null;
        }
        String local = CONNECTOR_SLOT.matcher(dst).replaceAll("").strip();
        return local.isEmpty() ? null : local;
    }

    private static boolean isConnectorSegment(String body) {
        boolean word = false;
        int at = 0;
        while (at < body.length()) {
            char c = body.charAt(at);
            if (Character.isLetter(c)) {
                int end = at;
                while (end < body.length() && Character.isLetter(body.charAt(end))) {
                    end++;
                }
                if (!CONNECTOR_KEYS.containsKey(body.substring(at, end))) {
                    return false;
                }
                word = true;
                at = end;
            } else if (isValueChar(c)) {
                at++;
            } else {
                return false;
            }
        }
        return word;
    }

    private static void translateConnectors(List<Piece> pieces, List<Integer> at,
                                            TranslationStore store) {
        for (int k = at.size() - 1; k >= 0; k--) {
            int index = at.get(k);
            Piece piece = pieces.get(index);
            String raw = piece.text();
            String tail = SpaceOffset.trailingOffsets(raw);
            String body = raw.substring(0, raw.length() - tail.length());
            String local = translateTail(body, store);
            if (local.equals(body)) {
                continue;
            }
            pieces.set(index, Piece.translated(DisplayStyles.literal(local, DisplayStyles.forDisplay(piece.style())),
                                               DisplayStyles.literal(body, piece.style())));
            if (!tail.isEmpty()) {
                pieces.add(index + 1, Piece.space(SpaceOffset.decode(tail),
                        SpaceOffset.styleFor(piece.style())));
            }
        }
    }

    private static Component padSlot(Component translated, Component original, Style style) {
        try {
            int want = widthOf(original);
            int got = widthOf(translated);
            if (want <= 0 || got <= 0 || want == got) {
                return translated;
            }
            String fill = SpaceOffset.encode(want - got);
            if (fill.isEmpty()) {
                return translated;
            }
            return Component.empty().append(translated).append(
                    Component.literal(fill).setStyle(SpaceOffset.styleFor(style)));
        } catch (Throwable t) {
            return translated;
        }
    }

    private static String lookupTrimmed(String template, TranslationStore store,
                                        boolean percent) {
        String exact = withPercent(template, store, percent);
        if (exact != null) {
            int head = 0;
            while (head < template.length()
                    && Character.isWhitespace(template.charAt(head))) {
                head++;
            }
            int tail = template.length();
            while (tail > head && Character.isWhitespace(template.charAt(tail - 1))) {
                tail--;
            }
            return template.substring(0, head) + exact
                    + reattach(exact, template.substring(tail));
        }
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;

        int start = 0;
        while (start < template.length()) {
            if (template.startsWith(glyph, start)) {
                start += glyph.length();
            } else if (Character.isWhitespace(template.charAt(start))) {
                start++;
            } else if (isDecoration(template.codePointAt(start))) {
                start += Character.charCount(template.codePointAt(start));
            } else {
                break;
            }
        }
        int keepColon = trimEnd(template, start, glyph, false);
        int end = trimEnd(template, start, glyph, true);
        if (start == 0 && end == template.length()) {
            return null;
        }
        boolean plate = start == 0 && !template.substring(end).isBlank();
        boolean was = TranslationStore.itemNamesBarred();
        TranslationStore.barItemNames(was || plate);
        try {
            String hit = null;
            int used = keepColon;
            if (keepColon > end) {
                String withColon = template.substring(start, keepColon);
                hit = withColon.isBlank() ? null : withPercent(withColon, store, percent);
            }
            if (hit == null) {
                used = end;
                String core = template.substring(start, end);
                if (core.isBlank()) {
                    return null;
                }
                hit = withPercent(core, store, percent);
            }
            if (hit == null) {
                return null;
            }
            return template.substring(0, start) + hit + reattach(hit, template.substring(used));
        } finally {
            TranslationStore.barItemNames(was);
        }
    }

    private static int trimEnd(String template, int start, String glyph, boolean colons) {
        int end = template.length();
        while (end > start) {
            if (end >= start + glyph.length() && template.startsWith(glyph, end - glyph.length())) {
                end -= glyph.length();
            } else if (Character.isWhitespace(template.charAt(end - 1))
                    || (colons && ColumnAligner.isTrailingColon(template.charAt(end - 1)))) {
                end--;
            } else {
                break;
            }
        }
        return end;
    }

    private static boolean isDecoration(int codePoint) {
        return Character.getType(codePoint) == Character.OTHER_SYMBOL;
    }

    private static String reattach(String translated, String suffix) {
        if (translated.isEmpty()) {
            return suffix;
        }
        char last = translated.charAt(translated.length() - 1);
        if (suffix.isBlank()) {
            return isFullWidthPunctuation(last) ? "" : suffix;
        }
        if (ColumnAligner.isTrailingColon(suffix.charAt(0)) && suffix.substring(1).isBlank()
                && ColumnAligner.isTrailingColon(last)) {
            return suffix.substring(1);
        }
        return suffix;
    }

    private static boolean isFullWidthPunctuation(char c) {
        return "：，。、！？；）」』】".indexOf(c) >= 0;
    }

    private static String withPercent(String key, TranslationStore store, boolean percent) {
        if (percent) {
            String hit = store.lookup(key.strip() + "%");
            if (hit != null) {
                return hit;
            }
        }
        String hit = store.lookup(key);
        if (hit != null) {
            return hit;
        }
        hit = withQuality(key, store, percent);
        if (hit != null) {
            return hit;
        }
        hit = withMaker(key, store);
        if (hit != null) {
            return hit;
        }
        hit = withOwner(key, store);
        if (hit == null) {
            hit = eventFrame(key, store);
        }
        if (hit == null) {
            hit = iconlessReward(key, store);
        }
        return hit != null ? hit : npcChatLine(key, store);
    }

    private static final String REWARD_ROW = "- +";

    static String iconlessReward(String key, TranslationStore store) {
        if (key == null || store == null) {
            return null;
        }
        String core = key.strip();
        if (!core.startsWith(REWARD_ROW) || core.indexOf('\n') >= 0) {
            return null;
        }
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        String hit = store.lookup(glyph + core);
        if (hit == null || hit.isBlank() || !hit.startsWith(glyph)) {
            return null;
        }
        String rest = hit.substring(glyph.length());
        return rest.isBlank() ? null : rest;
    }

    static String npcChatLine(String template, TranslationStore store) {
        if (template == null || store == null || !template.startsWith(CHAT_COUNTER)) {
            return null;
        }
        java.util.regex.Matcher m = TranslationStore.CHAT_LINE.matcher(template.strip());
        if (!m.matches()) {
            return null;
        }
        String speaker = m.group("speaker").strip();
        String line = m.group("line").strip();
        if (speaker.isEmpty() || !hasWord(line)
                || speaker.contains(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            return null;
        }
        String said = lookup(line, store);
        if (said == null || said.isBlank()) {
            return null;
        }
        said = oneLine(said.strip());
        String name = speakerName(speaker, store);
        String numberedName = ColumnTemplates.renumber(name, 2);
        String numberedLine = ColumnTemplates.renumber(said, 2 + ColumnTemplates.countNumbers(speaker));
        if (numberedName == null || numberedLine == null) {
            return null;
        }
        return "[{~1}/{~2}] " + numberedName + ": " + numberedLine;
    }

    static final String CHAT_COUNTER = "[{~}/{~}] ";

    private static String speakerName(String speaker, TranslationStore store) {
        String learned = store.chatSpeaker(speaker);
        if (learned == null && !store.isItemName(speaker)) {
            learned = store.lookupExact(speaker);
        }
        if (learned == null || learned.isBlank() || learned.indexOf('\n') >= 0
                || !samePlaceholders(speaker, learned)) {
            return speaker;
        }
        return learned.strip();
    }

    private static boolean samePlaceholders(String a, String b) {
        return ColumnTemplates.countNumbers(a) == ColumnTemplates.countNumbers(ColourTemplate.stripColourTokens(b).replaceAll("\\{~\\d}", "{~}"))
                && count(a, GlyphSplitter.PLACE_PLACEHOLDER) == count(b, GlyphSplitter.PLACE_PLACEHOLDER)
                && count(a, GlyphSplitter.PLAYER_PLACEHOLDER) == count(b, GlyphSplitter.PLAYER_PLACEHOLDER)
                && count(a, GlyphSplitter.GLYPH_PLACEHOLDER) == count(b, GlyphSplitter.GLYPH_PLACEHOLDER);
    }

    private static String oneLine(String text) {
        if (text.indexOf('\n') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        String[] parts = text.split("\n");
        for (String part : parts) {
            String piece = part.strip();
            if (piece.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                char before = out.charAt(out.length() - 1);
                char after = piece.charAt(0);
                if (!isWide(before) && !isWide(after)) {
                    out.append(' ');
                }
            }
            out.append(piece);
        }
        return out.toString();
    }

    private static boolean isWide(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN
                || Character.UnicodeScript.of(c) == Character.UnicodeScript.HIRAGANA
                || Character.UnicodeScript.of(c) == Character.UnicodeScript.KATAKANA
                || (c >= '　' && c <= '〿') || (c >= '＀' && c <= '￯');
    }

    static String countdownBoard(String template, TranslationStore store) {
        if (template == null || store == null || template.indexOf('\n') < 0) {
            return null;
        }
        String[] rows = template.split("\n", -1);
        boolean timer = false;
        boolean words = false;
        for (String row : rows) {
            timer |= TIMER_ROW.matcher(row.strip()).matches();
            words |= hasWord(row);
        }
        if (!timer || !words) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int values = 0;
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                out.append(NEWLINE);
            }
            String row = rows[i];
            String hit;
            if (TIMER_ROW.matcher(row.strip()).matches()) {
                hit = store.labelLookup(row);
                if (hit == null || hit.isBlank()) {
                    hit = row;
                }
            } else if (!hasWord(row)) {
                hit = row;
            } else {
                hit = store.labelLookup(row);
                if (hit == null || hit.isBlank()) {
                    hit = lookup(row, store);
                }
                if (hit == null || hit.isBlank() || hit.indexOf('\n') >= 0) {
                    return null;
                }
            }
            String shifted = ColumnTemplates.renumber(ColourTemplate.stripColourTokens(hit), values);
            if (shifted == null) {
                return null;
            }
            values += ColumnTemplates.countNumbers(row);
            out.append(shifted);
        }
        return out.toString();
    }

    private static final java.util.regex.Pattern TIMER_ROW =
            java.util.regex.Pattern.compile("\\{~}[dhms](?: \\{~}[dhms])*");

    static String eventFrame(String template, TranslationStore store) {
        if (template == null || store == null || template.indexOf('\n') >= 0
                || !template.contains(EVENT_MARK)) {
            return null;
        }
        java.util.regex.Matcher m = EVENT_LINE.matcher(template.strip());
        if (!m.matches()) {
            return null;
        }
        String name = m.group("name").strip();
        if (name.isEmpty() || name.contains(GlyphSplitter.NUMBER_PLACEHOLDER)
                || name.contains(GlyphSplitter.GLYPH_PLACEHOLDER)
                || name.contains(GlyphSplitter.PLAYER_PLACEHOLDER)) {
            return null;
        }
        int lead = count(m.group("lead"), GlyphSplitter.GLYPH_PLACEHOLDER);
        if (lead > 1) {
            return null;
        }
        String key = GlyphSplitter.GLYPH_PLACEHOLDER + " The " + GlyphSplitter.PLACE_PLACEHOLDER
                + " World Event starts in "
                + (m.group("timer").contains("m") ? "{~}m {~}s" : "{~}s")
                + "! (" + GlyphSplitter.NUMBER_PLACEHOLDER + " blocks away) Click to track";
        String frame = store.lookup(key);
        if (frame == null || frame.isBlank()
                || count(frame, GlyphSplitter.PLACE_PLACEHOLDER) != 1) {
            return null;
        }
        if (lead == 0) {
            if (!frame.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                return null;
            }
            frame = frame.substring(GlyphSplitter.GLYPH_PLACEHOLDER.length()).stripLeading();
        }
        return frame.replace(GlyphSplitter.PLACE_PLACEHOLDER, eventName(name, store));
    }

    private static String eventName(String name, TranslationStore store) {
        String hit = store.lookup(name + EVENT_SUFFIX);
        if (hit == null || hit.isBlank()) {
            return name;
        }
        String bare = TRAILING_BRACKET.matcher(hit.strip()).replaceFirst("").strip();
        return bare.isEmpty() || bare.contains("{") ? name : bare;
    }

    private static int count(String text, String token) {
        int n = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + token.length())) {
            n++;
        }
        return n;
    }

    private static final String EVENT_MARK = " World Event starts in ";

    private static final String EVENT_SUFFIX = " [World Event]";

    private static final java.util.regex.Pattern TRAILING_BRACKET =
            java.util.regex.Pattern.compile("\\s*[\\[［][^\\[\\]［］]*[\\]］]$");

    private static final java.util.regex.Pattern EVENT_LINE = java.util.regex.Pattern.compile(
            "(?<lead>(?:\\{#}\\s*)*)The (?<name>.+?) World Event starts in "
                    + "(?<timer>\\{~}m\\s*\\{~}s|\\{~}s)!\\s*\\(\\{~} blocks away\\)\\s*Click to track");

    private static String withMaker(String key, TranslationStore store) {
        String core = key.strip();
        for (String prefix : NAME_PREFIXES) {
            if (!core.startsWith(prefix)) {
                continue;
            }
            String name = core.substring(prefix.length()).strip();
            if (!isName(name)) {
                continue;
            }
            String word = store.lookup(prefix.strip());
            if (word != null && !word.isBlank()) {
                return word + " " + name;
            }
        }
        return null;
    }

    private static String withOwner(String key, TranslationStore store) {
        String core = key.strip();
        int at = -1;
        for (String mark : OWNER_MARKS) {
            int found = core.indexOf(mark);
            if (found > 0 && (at < 0 || found < at)) {
                at = found;
            }
        }
        if (at <= 0) {
            return null;
        }
        String name = core.substring(0, at);
        if (!isName(name)) {
            return null;
        }
        String whole = core;
        while (whole.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            whole = whole.substring(GlyphSplitter.GLYPH_PLACEHOLDER.length()).strip();
        }
        if (store.lookupExact(whole) != null || store.isItemName(whole)) {
            return null;
        }
        String thing = core.substring(at + 3).strip();
        boolean was = TranslationStore.itemNamesBarred();
        TranslationStore.barItemNames(true);
        String zh;
        try {
            zh = thing.isEmpty() ? null : store.lookup(thing);
        } finally {
            TranslationStore.barItemNames(was);
        }
        if (zh == null || zh.isBlank()) {
            return null;
        }
        String form = store.lookup(OWNER_FORM);
        if (form == null || !form.contains("{owner}") || !form.contains("{thing}")) {
            return null;
        }
        return form.replace("{owner}", name).replace("{thing}", zh);
    }

    private static final String OWNER_FORM = "{owner}'s {thing}";

    private static final String[] OWNER_MARKS = {"'s ", "’s "};

    static boolean isName(String text) {
        if (text.isEmpty() || text.length() > NAME_ROOM) {
            return false;
        }
        String bare = text.replaceAll("\\{[^}]*\\}", "");
        return bare.chars().allMatch(c ->
                (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9') || c == '_');
    }

    private static final int NAME_ROOM = 40;

    private static final String[] NAME_PREFIXES = {"Crafted by "};

    private static String withQuality(String key, TranslationStore store, boolean percent) {
        String core = key.strip();
        for (String prefix : QUALITY_PREFIXES) {
            if (!core.startsWith(prefix)) {
                continue;
            }
            String name = withPercent(core.substring(prefix.length()), store, percent);
            String word = store.lookup(prefix.strip());
            if (name != null && word != null) {
                return word + name;
            }
        }
        return null;
    }

    private static final String[] QUALITY_PREFIXES = {"Perfect ", "Defective "};

    public static Component untranslated(StyledText line) {
        MutableComponent out = Component.empty();
        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            out.append(DisplayStyles.literal(raw, ps == null ? Style.EMPTY : ps.getStyle()));
        }
        return out;
    }

    static Component rebuild(String translated, LineParts parts,
                                     TranslationStore store) {
        List<Component> one = rebuildAll(new String[] {translated}, List.of(parts), List.of(), store);
        return one == null ? null : one.get(0);
    }

    private static void noteUnapplied(List<LineParts> parts, TranslationStore store) {
        try {
            if (store == null || parts.isEmpty()) {
                return;
            }
            StringBuilder src = new StringBuilder();
            for (LineParts part : parts) {
                if (src.length() > 0) {
                    src.append(NEWLINE);
                }
                src.append(part.template());
            }
            String key = src.toString().strip();
            if (com.wynnchayuan.capture.UnappliedAudit.asked(key)) {
                return;
            }
            String corpus = store.lookup(key);
            if (corpus != null && !corpus.isBlank()) {
                com.wynnchayuan.capture.UnappliedAudit.note(key, corpus);
            }
        } catch (Throwable t) {
        }
    }

    static Component rebuild(String translated, LineParts parts,
                                     List<LineParts.Piece> glyphs, TranslationStore store) {
        List<Component> one = rebuildAll(new String[] {translated}, List.of(parts),
                                         List.of(), glyphs, null, store);
        return one == null ? null : one.get(0);
    }

    static List<LineParts.Piece> unwrappedGlyphs(
            String template, List<LineParts.Piece> glyphs) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        List<LineParts.Piece> kept = new ArrayList<>();
        int at = 0;
        int index = 0;
        boolean lineStart = false;
        while (at < template.length() && index < glyphs.size()) {
            if (template.startsWith(glyph, at)) {
                if (!lineStart) {
                    kept.add(glyphs.get(index));
                }
                index++;
                at += glyph.length();
                continue;
            }
            char c = template.charAt(at);
            if (c == '\n') {
                lineStart = true;
            } else if (!Character.isWhitespace(c)) {
                lineStart = false;
            }
            at++;
        }
        while (index < glyphs.size()) {
            kept.add(glyphs.get(index++));
        }
        return kept;
    }

    private static List<Component> rebuildAll(String[] translated, List<LineParts> parts,
                                              List<LineParts.Piece> extraAccents,
                                              TranslationStore store) {
        return rebuildAll(translated, parts, extraAccents, null, null, store);
    }

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
        List<String> spans = squareSpans(String.join(NL, translated));
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

    private static boolean hasLetter(String text) {
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
                                .replace(String.valueOf(NEWLINE), ""));
                }
            }
        }
        return depth == 0 ? out : List.of();
    }

    private static boolean sameColour(Style one, Style other) {
        if (one == null || other == null) {
            return one == other;
        }
        return undecorated(one).equals(undecorated(other));
    }

    private static List<LineParts.Piece> wholeLineAccents(
            List<LineParts> parts, List<LineParts.Piece> allRuns,
            String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        String template = parts.size() == 1 ? parts.get(0).template() : null;
        List<RowStyle> source = template == null
                ? perPartStyles(parts) : uniformStyles(allRuns, template);
        String[] dst = String.join(NL, translated).split(NL, -1);
        if (source.size() != dst.length) {
            return List.of();
        }
        List<LineParts.Piece> out = new ArrayList<>();
        for (int i = 0; i < dst.length; i++) {
            String text = PLACEHOLDER.matcher(dst[i]).replaceAll("").strip();
            if (!hasContent(text)) {
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

    private static List<LineParts.Piece> labelValueAccents(
            List<LineParts> parts, String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        if (parts.size() != 1 || translated.length != 1) {
            return List.of();
        }
        if (translated[0].indexOf(NEWLINE) >= 0
                || parts.get(0).template().indexOf(NEWLINE) >= 0) {
            return List.of();
        }
        Style label = null;
        Style value = null;
        for (LineParts.Piece run : parts.get(0).runs()) {
            if (isNote(run.text())) {
                continue;
            }
            if (label == null) {
                int colon = run.text().indexOf(':');
                if (colon < 0) {
                    continue;
                }
                if (hasContent(run.text().substring(colon + 1))) {
                    return List.of();
                }
                label = undecorated(run.style());
            } else if (hasContent(run.text())) {
                value = undecorated(run.style());
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

    private static List<LineParts.Piece> segmentAccents(
            List<LineParts> parts, String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        String[] dst = String.join(NL, translated).split(NL, -1);
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
                earlier.append(dst[i]).append(NL);
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
                    if (hasContent(piece.text()) && before.indexOf(piece.text()) < 0) {
                        add(out, piece.text(), piece.style(), blockStyle, known);
                    }
                    before.append(piece.text());
                }
                earlier.append(target.get(k).text());
            }
            earlier.append(NL);
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
            if (hasContent(name) && !target.equals(name) && target.contains(name)
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
                    || hasContent(source.get(k).text()) != hasContent(target.get(k).text())) {
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
            return hasContent(target) ? List.of(new LineParts.Piece(target, runs.get(0).style()))
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
            if (hasContent(source.substring(from, to)) != hasContent(piece)) {
                return List.of();
            }
            Style style = uniformStyle(source, at, from, to);
            if (style != null && hasContent(piece)) {
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
        if (!hasContent(bare) || java.util.Objects.equals(style, blockStyle)
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

    static final String NL = "\n";

    private static final java.util.regex.Pattern PLACEHOLDER =
            java.util.regex.Pattern.compile("\\{[^}]*\\}");

    private static List<RowStyle> uniformStyles(List<LineParts.Piece> runs, String template) {
        List<RowStyle> out = new ArrayList<>();
        int at = 0;
        int eaten = 0;
        int[] depth = {0};
        for (String line : template.split(NL, -1)) {
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
                tally.add(run.style(), proseAmong(run.text(), eaten, take, local));
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

    private static final class Tally {
        private final java.util.Map<Style, Integer> counts = new java.util.LinkedHashMap<>();
        private final java.util.Map<Style, Style> shown = new java.util.HashMap<>();

        void add(Style style, int solid) {
            if (style != null && solid > 0) {
                Style key = undecorated(style);
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
            Style key = undecorated(style);
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

    private static int solidCount(String text) {
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
                if (!hasContent(run.text())) {
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

    static boolean hasContent(String text) {
        return text.codePoints().anyMatch(cp -> !Character.isWhitespace(cp)
                && !com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp));
    }

    static List<Component> rebuildAll(String[] translated, List<LineParts> parts,
                                              List<LineParts.Piece> extraAccents,
                                              List<LineParts.Piece> overrideGlyphs,
                                              List<LineParts.Piece> overridePlaces,
                                              TranslationStore store) {
        List<LineParts.Piece> glyphs = new ArrayList<>();
        List<LineParts.Piece> places = new ArrayList<>();
        List<LineParts.Piece> numbers = new ArrayList<>();
        List<LineParts.Piece> users = new ArrayList<>();
        List<LineParts.Piece> accents = new ArrayList<>(extraAccents);
        Style blockStyle = dominantStyle(parts);
        List<LineParts.Piece> allRuns = new ArrayList<>();
        for (LineParts part : parts) {
            glyphs.addAll(part.glyphs());
            places.addAll(part.places());
            numbers.addAll(part.numbers());
            users.addAll(part.users());
            allRuns.addAll(part.runs());
        }
        if (overridePlaces != null) {
            places = new ArrayList<>(overridePlaces);
        }
        if (overrideGlyphs != null) {
            glyphs = new ArrayList<>(overrideGlyphs);
        }
        accents.addAll(LineParts.accentsAgainst(allRuns, blockStyle));
        accents = withTranslations(accents, String.join(String.valueOf(NEWLINE), translated), store);
        List<LineParts.Piece> segmented = segmentAccents(parts, translated, blockStyle, accents);
        if (segmented != null) {
            accents.addAll(segmented);
        } else {
            accents.addAll(labelValueAccents(parts, translated, blockStyle, accents));
        }
        accents.addAll(wholeLineAccents(parts, allRuns, translated, blockStyle, accents));
        accents.addAll(bracketAccents(allRuns, translated, blockStyle));

        String[] flowed = keepAccentsWhole(translated, accents);
        accents.addAll(halvesAcrossBreaks(flowed, accents));
        flowed = forcedTerms(flowed, store);

        List<List<ColourTemplate.Token>> lines = new ArrayList<>(flowed.length);
        long wantGlyphs = 0;
        long wantPlaces = 0;
        long wantNumbers = 0;
        long wantUsers = 0;
        int numbered = 0;
        for (String line : flowed) {
            List<ColourTemplate.Token> tokens = ColourTemplate.tokenize(line);
            lines.add(tokens);
            for (ColourTemplate.Token token : tokens) {
                switch (token.kind()) {
                    case GLYPH -> wantGlyphs++;
                    case PLACE -> wantPlaces++;
                    case NUMBER -> {
                        if (token.index() == 0) {
                            wantNumbers++;
                        } else {
                            numbered = Math.max(numbered, token.index());
                        }
                    }
                    case USER -> wantUsers++;
                    default -> { }
                }
            }
        }
        boolean numbersOk = numbered == 0
                ? wantNumbers == numbers.size()
                : wantNumbers <= numbers.size() && numbered <= numbers.size();
        if (wantGlyphs != glyphs.size() || wantPlaces != places.size()
                || !numbersOk || wantUsers > users.size()) {
            noteUnapplied(parts, store);
            return null;
        }

        Style textStyle = DisplayStyles.forDisplay(blockStyle);
        Style symbolStyle = leadingSymbolStyle(allRuns);
        if (symbolStyle == null) {
            symbolStyle = SpaceOffset.isSpaceFont(blockStyle) ? textStyle : blockStyle;
        }
        Style noteStyle = noteStyleOf(allRuns, blockStyle);
        boolean[] inNote = {false};
        boolean[] usedAccent = new boolean[accents.size()];
        int glyph = 0;
        int place = 0;
        int number = 0;
        int user = 0;

        List<Style> palette = ColourTemplate.palette(allRuns);
        List<Style> words = ColourTemplate.wordPalette(allRuns);
        List<ColourTemplate.Bar> bars = ColourTemplate.bars(allRuns);
        boolean[] usedBar = new boolean[bars.size()];
        FlowedDebug.palette(palette, allRuns, flowed);

        Style forced = null;
        List<Component> out = new ArrayList<>(lines.size());
        for (List<ColourTemplate.Token> tokens : lines) {
            MutableComponent line = Component.empty();
            Style justFilled = null;
            boolean afterNumber = false;
            for (int i = 0; i < tokens.size(); i++) {
                ColourTemplate.Token token = tokens.get(i);
                switch (token.kind()) {
                    case COLOR -> forced = ColourTemplate.colourOf(token, palette, words, textStyle);
                    case GLYPH -> {
                        LineParts.Piece piece = glyphs.get(glyph++);
                        line.append(Component.literal(piece.text()).withStyle(piece.style()));
                        justFilled = null;
                        afterNumber = false;
                    }
                    case PLACE -> {
                        LineParts.Piece piece = places.get(place++);
                        line.append(DisplayStyles.literal(piece.text(), DisplayStyles.forDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = false;
                    }
                    case NUMBER -> {
                        int at = token.index() > 0 ? token.index() - 1 : number++;
                        if (at < 0 || at >= numbers.size()) {
                            return null;
                        }
                        LineParts.Piece piece = numbers.get(at);
                        line.append(DisplayStyles.literal(piece.text(), DisplayStyles.numberDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = true;
                    }
                    case USER -> {
                        LineParts.Piece piece = users.get(user++);
                        line.append(DisplayStyles.literal(piece.text(), DisplayStyles.forDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = false;
                    }
                    case TEXT -> {
                        String text = token.text();
                        Style bigNext = i + 1 < tokens.size()
                                && tokens.get(i + 1).kind() == ColourTemplate.Kind.NUMBER
                                ? DisplayStyles.displayFont(peekStyle(tokens.get(i + 1), glyphs, places,
                                              numbers, users, glyph, place, number, user))
                                : null;
                        if (bigNext != null && !text.isEmpty()
                                && (text.endsWith("+") || text.endsWith("-"))) {
                            String head = text.substring(0, text.length() - 1);
                            if (!head.isEmpty()) {
                                appendHugging(line, head, textStyle, symbolStyle,
                                              noteStyle, inNote, accents, usedAccent,
                                              store, i > 0 ? justFilled : null, null,
                                              afterNumber);
                            }
                            line.append(DisplayStyles.literal(text.substring(text.length() - 1), bigNext));
                            justFilled = null;
                            afterNumber = false;
                            break;
                        }
                        if (forced != null) {
                            line.append(DisplayStyles.literal(token.text(), forced));
                        } else {
                            Style before = i > 0 ? justFilled : null;
                            Style after = i + 1 < tokens.size()
                                    ? peekStyle(tokens.get(i + 1), glyphs, places,
                                                numbers, users, glyph, place, number, user)
                                    : null;
                            int[] span = ColourTemplate.barSpan(token.text());
                            ColourTemplate.Bar bar = span == null ? null
                                    : ColourTemplate.takeBar(bars, usedBar, token.text().charAt(span[0]),
                                              span[1] - span[0]);
                            if (bar == null) {
                                appendHugging(line, token.text(), textStyle, symbolStyle,
                                              noteStyle, inNote, accents, usedAccent,
                                              store, before, after, afterNumber);
                            } else {
                                String head = token.text().substring(0, span[0]);
                                String tail = token.text().substring(span[1]);
                                if (!head.isEmpty()) {
                                    appendHugging(line, head, textStyle, symbolStyle,
                                                  noteStyle, inNote, accents, usedAccent,
                                                  store, before, null, afterNumber);
                                }
                                ColourTemplate.appendBar(line, bar);
                                if (!tail.isEmpty()) {
                                    appendHugging(line, tail, textStyle, symbolStyle,
                                                  noteStyle, inNote, accents, usedAccent,
                                                  store, null, after, false);
                                }
                            }
                        }
                        justFilled = null;
                        afterNumber = false;
                    }
                }
            }
            out.add(line);
        }
        List<String> texts = new ArrayList<>(accents.size());
        List<Style> styles = new ArrayList<>(accents.size());
        for (LineParts.Piece accent : accents) {
            texts.add(accent.text());
            styles.add(accent.style());
        }
        FlowedDebug.accents(texts, styles, usedAccent, flowed);
        List<String> templates = new ArrayList<>(parts.size());
        for (LineParts part : parts) {
            templates.add(part.template());
        }
        com.wynnchayuan.capture.ColourAudit.note(templates, flowed, allRuns, accents,
                usedAccent, blockStyle);
        FlowedDebug.glyphs(glyphs, flowed);
        return out;
    }

    private static Style peekStyle(ColourTemplate.Token next,
                                   List<LineParts.Piece> glyphs, List<LineParts.Piece> places,
                                   List<LineParts.Piece> numbers, List<LineParts.Piece> users,
                                   int glyph, int place, int number, int user) {
        return switch (next.kind()) {
            case GLYPH -> null;
            case PLACE -> at(places, place);
            case NUMBER -> at(numbers, next.index() > 0 ? next.index() - 1 : number);
            case USER -> at(users, user);
            case TEXT, COLOR -> null;
        };
    }

    private static Style at(List<LineParts.Piece> pool, int index) {
        return index >= 0 && index < pool.size() ? pool.get(index).style() : null;
    }

    static boolean leadTakesNext(String text, int lead, Style after) {
        return after != null && lead == text.length();
    }

    private static void appendHugging(MutableComponent out, String text,
                                      Style base, Style symbol, Style note, boolean[] depth,
                                      List<LineParts.Piece> accents, boolean[] used,
                                      TranslationStore store, Style before, Style after,
                                      boolean afterNumber) {
        if (before != null && after != null && sameColour(before, after)
                && !hasLetter(text)) {
            out.append(DisplayStyles.literal(text, DisplayStyles.forDisplay(before)));
            return;
        }
        int lead = 0;
        if (before != null) {
            while (lead < text.length() && hugs(text.charAt(lead))
                    && !hasOwnColour(text, lead, accents, used, before)) {
                lead++;
            }
        }
        int tail = text.length();
        if (after != null) {
            while (tail > lead && hugs(text.charAt(tail - 1))
                    && !hasOwnColour(text, tail - 1, accents, used, after)) {
                tail--;
            }
        }
        if (lead > 0) {
            Style owner = leadTakesNext(text, lead, after) ? after : before;
            out.append(DisplayStyles.literal(text.substring(0, lead), DisplayStyles.forDisplay(owner)));
        }
        int unit = before == null ? 0 : unitLength(text, afterNumber, lead, tail);
        if (unit > 0) {
            out.append(DisplayStyles.literal(text.substring(0, unit), DisplayStyles.forDisplay(before)));
            lead = unit;
        }
        int mid = lead;
        while (mid < tail && isPictograph(text.charAt(mid))) {
            mid++;
        }
        if (mid > lead) {
            Style paint = symbol;
            int own = exactAccent(text.substring(lead, mid), accents, used);
            if (own >= 0) {
                used[own] = true;
                Style mine = accents.get(own).style();
                paint = mine == null || paint == null ? DisplayStyles.forDisplay(mine)
                        : paint.withColor(mine.getColor());
            }
            out.append(DisplayStyles.literal(text.substring(lead, mid), paint));
            lead = mid;
        }
        int back = tail;
        if (after != null) {
            int end = tail;
            while (end > lead && text.charAt(end - 1) == ' ') {
                end--;
            }
            int start = end;
            while (start > lead && isPictograph(text.charAt(start - 1))) {
                start--;
            }
            if (start < end && exactAccent(text.substring(start, end), accents, used) < 0) {
                back = start;
            }
        }
        if (back > lead) {
            appendNoting(out, text.substring(lead, back), base, note,
                         depth, accents, used, store);
        }
        if (back < tail) {
            out.append(DisplayStyles.literal(text.substring(back, tail), DisplayStyles.forDisplay(after)));
        }
        if (tail < text.length()) {
            out.append(DisplayStyles.literal(text.substring(tail), DisplayStyles.forDisplay(after)));
        }
    }

    private static Style leadingSymbolStyle(List<LineParts.Piece> runs) {
        for (LineParts.Piece run : runs) {
            String text = run.text();
            if (text == null || text.isEmpty()) {
                continue;
            }
            return isPictograph(text.charAt(0)) ? run.style() : null;
        }
        return null;
    }

    private static int exactAccent(String word, List<LineParts.Piece> accents,
                                   boolean[] used) {
        for (int k = 0; k < accents.size(); k++) {
            if (!used[k] && accents.get(k).text().equals(word)) {
                return k;
            }
        }
        return -1;
    }

    private static boolean hasOwnColour(String text, int at,
                                        List<LineParts.Piece> accents, boolean[] used,
                                        Style side) {
        for (int k = 0; k < accents.size(); k++) {
            if (used[k] || isLoneBracket(accents.get(k).text())) {
                continue;
            }
            String word = accents.get(k).text();
            if (!word.isEmpty() && text.startsWith(word, at)
                    && !sameColour(accents.get(k).style(), side)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLoneBracket(String word) {
        if (word.length() != 1) {
            return false;
        }
        char c = word.charAt(0);
        return c == '[' || c == ']' || c == '【' || c == '】';
    }

    static int unitLength(String text, boolean afterNumber, int lead, int tail) {
        if (!afterNumber || lead != 0 || text.isEmpty()) {
            return 0;
        }
        int unit = 0;
        while (unit < tail && unit < TextWrapper.MAX_UNIT && TextWrapper.isUnitLetter(text.charAt(unit))) {
            unit++;
        }
        if (unit == 0) {
            return 0;
        }
        return unit >= text.length() || !Character.isLetter(text.charAt(unit)) ? unit : 0;
    }

    static boolean hugs(char c) {
        if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || isHan(c)) {
            return false;
        }
        return c != '(' && c != ')' && c != '（' && c != '）';
    }

    static String[] keepAccentsWhole(String[] lines, List<LineParts.Piece> accents) {
        if (lines.length < 2 || accents.isEmpty()) {
            return lines;
        }
        String[] out = lines.clone();
        for (int i = 0; i + 1 < out.length; i++) {
            int move = splitAcross(out[i], out[i + 1], accents);
            if (move > 0) {
                move += trailingGlyph(out[i], out[i].length() - move);
                move = out[i].length()
                        - TextWrapper.keepValueWithGlyph(out[i], out[i].length() - move, 0);
                String moved = out[i].substring(out[i].length() - move);
                out[i] = out[i].substring(0, out[i].length() - move);
                out[i + 1] = moved + out[i + 1];
            }
        }
        return out;
    }

    private static int trailingGlyph(String head, int from) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = from;
        while (at > 0 && head.charAt(at - 1) == ' ') {
            at--;
        }
        if (at - glyph.length() <= 0 || !head.startsWith(glyph, at - glyph.length())) {
            return 0;
        }
        return from - (at - glyph.length());
    }

    static List<LineParts.Piece> halvesAcrossBreaks(
            String[] flowed, List<LineParts.Piece> accents) {
        List<LineParts.Piece> out = new ArrayList<>();
        if (flowed.length < 2) {
            return out;
        }
        for (LineParts.Piece accent : accents) {
            String word = accent.text();
            if (word.length() < 4) {
                continue;
            }
            for (int i = 0; i + 1 < flowed.length; i++) {
                int cut = cutBetween(flowed[i], flowed[i + 1], word);
                if (cut <= 0) {
                    continue;
                }
                addHalf(out, word.substring(0, cut), accent.style());
                addHalf(out, word.substring(cut), accent.style());
                break;
            }
        }
        return out;
    }

    private static int cutBetween(String head, String tail, String word) {
        for (int cut = 1; cut < word.length(); cut++) {
            if (head.endsWith(word.substring(0, cut))
                    && tail.startsWith(word.substring(cut))) {
                return cut;
            }
        }
        return 0;
    }

    private static void addHalf(List<LineParts.Piece> out, String half, Style style) {
        if (half.length() >= 2 && half.indexOf('{') < 0 && half.indexOf('}') < 0
                && hasLetter(half)) {
            out.add(new LineParts.Piece(half, style));
        }
    }

    private static int splitAcross(String head, String tail,
                                   List<LineParts.Piece> accents) {
        if (head.isEmpty() || tail.isEmpty()) {
            return 0;
        }
        for (LineParts.Piece accent : accents) {
            String word = accent.text();
            if (word.length() < 2 || word.length() > MAX_REFLOW) {
                continue;
            }
            for (int cut = 1; cut < word.length(); cut++) {
                String left = word.substring(0, cut);
                if (!head.endsWith(left) || !tail.startsWith(word.substring(cut))) {
                    continue;
                }
                if (left.indexOf('{') >= 0 || left.indexOf('}') >= 0) {
                    break;
                }
                return cut;
            }
        }
        return 0;
    }

    private static final int MAX_REFLOW = 12;

    private static int indexOfWhole(String text, String word, int from) {
        if (word.isEmpty()) {
            return -1;
        }
        for (int at = text.indexOf(word, from); at >= 0;
                at = text.indexOf(word, at + 1)) {
            if (!splitsWord(text, at, word)) {
                return at;
            }
        }
        return -1;
    }

    private static boolean splitsWord(String text, int at, String word) {
        int end = at + word.length();
        boolean headCuts = TranslationStore.wordChar(word.charAt(0))
                && at > 0 && TranslationStore.wordChar(text.charAt(at - 1));
        boolean tailCuts = TranslationStore.wordChar(word.charAt(word.length() - 1))
                && end < text.length() && TranslationStore.wordChar(text.charAt(end));
        return headCuts || tailCuts;
    }

    private static void appendText(MutableComponent out, String text, Style base,
                                   List<LineParts.Piece> accents, boolean[] used,
                                   TranslationStore store) {
        List<int[]> kept = store == null ? List.of() : store.keptOriginals(text);
        int from = 0;
        while (from < text.length()) {
            int at = -1;
            int which = -1;
            for (int k = 0; k < accents.size(); k++) {
                if (used[k] || isLoneBracket(accents.get(k).text())) {
                    continue;
                }
                int found = indexOfWhole(text, accents.get(k).text(), from);
                if (found < 0) {
                    continue;
                }
                boolean better = at < 0 || found < at
                        || (found == at && accents.get(k).text().length()
                                         > accents.get(which).text().length());
                if (better) {
                    at = found;
                    which = k;
                }
            }
            TranslationStore.Term term = store == null ? null : termOutside(store, text, from, kept);
            boolean longer = term != null && at >= 0 && term.start() == at
                    && term.end() - term.start() >= accents.get(which).text().length();
            if (term != null && (at < 0 || term.start() < at || longer)) {
                if (term.start() > from) {
                    String before = text.substring(from, term.start());
                    if (dropsSpaceBefore(before, term.translation())) {
                        before = before.substring(0, before.length() - 1);
                    }
                    out.append(DisplayStyles.literal(before, base));
                }
                Style style = base;
                for (int k = 0; k < accents.size(); k++) {
                    if (used[k]) {
                        continue;
                    }
                    String accentText = accents.get(k).text();
                    if (text.startsWith(accentText, term.start())
                            && term.start() + accentText.length() <= term.end()) {
                        style = DisplayStyles.forDisplay(accents.get(k).style());
                        used[k] = true;
                        break;
                    }
                }
                out.append(DisplayStyles.literal(term.translation(), style));
                from = term.end();
                if (dropsSpaceAfter(term.translation(), text, from)) {
                    from++;
                }
                continue;
            }
            if (at < 0) {
                out.append(DisplayStyles.literal(text.substring(from), base));
                return;
            }
            if (at > from) {
                out.append(DisplayStyles.literal(text.substring(from, at), base));
            }
            LineParts.Piece accent = accents.get(which);
            boolean keep = overlapping(kept, at, at + accent.text().length()) != null;
            String shown = store == null || keep ? null : store.lookupTerm(accent.text());
            if (shown == null && store != null && !keep) {
                shown = termsWithin(accent.text(), store);
            }
            out.append(DisplayStyles.literal(shown != null ? shown : accent.text(),
                               DisplayStyles.forDisplay(accent.style())));
            used[which] = true;
            from = at + accent.text().length();
        }
    }

    static String breakBeforeOriginal(String text, int maxPx, TranslationStore store,
                                      ToIntFunction<String> measure) {
        if (store == null || maxPx <= 0 || text.indexOf('\n') >= 0) {
            return text;
        }
        int at = store.appendedOriginalAt(text);
        if (at < 0 || measure.applyAsInt(text) <= maxPx) {
            return text;
        }
        int close = text.lastIndexOf(')') + 1;
        return text.substring(0, at) + text.substring(close) + "\n"
                + text.substring(at + 1, close);
    }

    static String termsWithin(String text, TranslationStore store) {
        List<int[]> kept = store == null ? List.of() : store.keptOriginals(text);

        StringBuilder out = new StringBuilder();
        int from = 0;
        boolean any = false;
        TranslationStore.Term term;
        while (from < text.length() && (term = termOutside(store, text, from, kept)) != null) {
            String before = text.substring(from, term.start());
            if (dropsSpaceBefore(before, term.translation())) {
                before = before.substring(0, before.length() - 1);
            }
            out.append(before).append(term.translation());
            from = term.end();
            if (dropsSpaceAfter(term.translation(), text, from)) {
                from++;
            }
            any = true;
        }
        return any ? out.append(text.substring(Math.min(from, text.length()))).toString()
                   : null;
    }

    static String[] forcedTerms(String[] lines, TranslationStore store) {
        if (store == null || lines.length == 0) {
            return lines;
        }
        String joined = String.join(String.valueOf(NEWLINE), lines);
        if (joined.indexOf("{c") < 0 && joined.indexOf("{w") < 0) {
            return lines;
        }
        StringBuilder plain = new StringBuilder(joined.length());
        int[] origin = new int[joined.length()];
        boolean[] inSpan = new boolean[joined.length()];
        boolean forced = false;
        for (int i = 0; i < joined.length(); ) {
            ColourTemplate.Token colour = ColourTemplate.colourToken(joined, i);
            if (colour != null) {
                forced = colour.index() != 0;
                i += colour.text().length();
                continue;
            }
            origin[plain.length()] = i;
            inSpan[plain.length()] = forced;
            plain.append(joined.charAt(i++));
        }
        String text = plain.toString();
        List<int[]> kept = store.keptOriginals(text);
        List<int[]> cuts = new ArrayList<>();
        List<String> with = new ArrayList<>();
        int from = 0;
        TranslationStore.Term term;
        while (from < text.length() && (term = termOutside(store, text, from, kept)) != null) {
            from = term.end();
            int start = term.start();
            int end = term.end();
            boolean whole = true;
            for (int k = start; k < end && whole; k++) {
                whole = inSpan[k];
            }
            if (!whole || origin[end - 1] - origin[start] != end - 1 - start) {
                continue;
            }
            if (dropsSpaceBefore(text.substring(0, start), term.translation())) {
                cuts.add(new int[] {origin[start - 1], origin[start - 1] + 1});
                with.add("");
            }
            cuts.add(new int[] {origin[start], origin[end - 1] + 1});
            with.add(term.translation());
            if (dropsSpaceAfter(term.translation(), text, end)) {
                cuts.add(new int[] {origin[end], origin[end] + 1});
                with.add("");
                from = end + 1;
            }
        }
        if (cuts.isEmpty()) {
            return lines;
        }
        StringBuilder out = new StringBuilder(joined);
        for (int k = cuts.size() - 1; k >= 0; k--) {
            out.replace(cuts.get(k)[0], cuts.get(k)[1], with.get(k));
        }
        return out.toString().split(String.valueOf(NEWLINE), -1);
    }

    private static TranslationStore.Term termOutside(TranslationStore store, String text,
                                                     int from, List<int[]> kept) {
        int at = from;
        while (at < text.length()) {
            TranslationStore.Term term = store.findTerm(text, at);
            if (term == null) {
                return null;
            }
            int[] in = overlapping(kept, term.start(), term.end());
            if (in == null) {
                return term;
            }
            at = Math.max(term.start() + 1, in[1]);
        }
        return null;
    }

    private static int[] overlapping(List<int[]> spans, int start, int end) {
        for (int[] span : spans) {
            if (start < span[1] && end > span[0]) {
                return span;
            }
        }
        return null;
    }

    private static List<LineParts.Piece> withTranslations(List<LineParts.Piece> accents,
                                                          String translated,
                                                          TranslationStore store) {
        if (store == null || accents.isEmpty()) {
            return accents;
        }
        List<LineParts.Piece> out = new ArrayList<>(accents);
        for (LineParts.Piece accent : accents) {
            String core = accent.text().strip();
            if (core.isEmpty()) {
                continue;
            }
            addStem(out, core, accent.style(), translated);
            String zh = store.lookup(core);
            if (zh == null || zh.isBlank()) {
                zh = store.lookupTerm(core);
            }
            if (zh == null || zh.isBlank()) {
                zh = lookupWordCore(core, store);
            }
            if (zh != null && !zh.isBlank() && !zh.equals(core)) {
                String wrapped = wrapLikeSource(core, zh, translated);
                out.add(new LineParts.Piece(wrapped != null ? wrapped : zh, accent.style()));
            }
        }
        return out;
    }

    static String wrapLikeSource(String core, String zh, String translated) {
        if (core == null || zh == null || translated == null
                || core.length() < 3 || zh.isBlank()) {
            return null;
        }
        if (core.charAt(0) != '[' || core.charAt(core.length() - 1) != ']') {
            return null;
        }
        String inner = core.substring(1, core.length() - 1);
        if (inner.isBlank() || inner.indexOf('[') >= 0 || inner.indexOf(']') >= 0) {
            return null;
        }
        if (zh.indexOf('[') >= 0 || zh.indexOf(']') >= 0) {
            return null;
        }
        String wrapped = "[" + zh + "]";
        if (translated.contains(wrapped)) {
            return wrapped;
        }
        return countedWrap(zh, translated);
    }

    private static String countedWrap(String zh, String translated) {
        int at = 0;
        while ((at = translated.indexOf('[', at)) >= 0) {
            int close = translated.indexOf(']', at + 1);
            if (close < 0) {
                return null;
            }
            String inner = translated.substring(at + 1, close);
            if (inner.endsWith(zh)
                    && onlyCount(inner.substring(0, inner.length() - zh.length()))) {
                return translated.substring(at, close + 1);
            }
            at = close + 1;
        }
        return null;
    }

    private static boolean onlyCount(String lead) {
        if (lead.isBlank()) {
            return false;
        }
        String bare = lead.replace(com.wynnchayuan.capture.GlyphSplitter.NUMBER_PLACEHOLDER, "")
                          .replace(" ", "");
        for (int i = 0; i < bare.length(); i++) {
            if (!Character.isDigit(bare.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static String lookupWordCore(String text, TranslationStore store) {
        int from = 0;
        int to = text.length();
        while (from < to && !Character.isLetterOrDigit(text.charAt(from))) {
            from++;
        }
        while (to > from && !Character.isLetterOrDigit(text.charAt(to - 1))) {
            to--;
        }
        if (from == 0 && to == text.length()) {
            return null;
        }
        String core = text.substring(from, to).strip();
        if (core.isEmpty()) {
            return null;
        }
        String zh = store.lookup(core);
        if (zh == null || zh.isBlank()) {
            zh = store.lookupTerm(core);
        }
        return zh == null || zh.isBlank() ? withoutCount(core, store) : zh;
    }

    private static String withoutCount(String core, TranslationStore store) {
        int at = 0;
        while (at < core.length() && Character.isDigit(core.charAt(at))) {
            at++;
        }
        if (at > 0) {
            while (at + 1 < core.length() && (core.charAt(at) == '.' || core.charAt(at) == ',')
                    && Character.isDigit(core.charAt(at + 1))) {
                at++;
                while (at < core.length() && Character.isDigit(core.charAt(at))) {
                    at++;
                }
            }
            if (at < core.length() && core.charAt(at) == '%') {
                at++;
            }
        }
        if (at == 0 || at >= core.length() || core.charAt(at) != ' ') {
            return null;
        }
        String rest = core.substring(at + 1).strip();
        if (rest.isEmpty() || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(rest)) {
            return null;
        }
        String zh = store.lookup(rest);
        if (zh == null || zh.isBlank()) {
            zh = store.lookupTerm(rest);
        }
        return zh == null || zh.isBlank() ? asSingular(rest, store) : zh;
    }

    private static String asSingular(String plural, TranslationStore store) {
        List<String> tries = new ArrayList<>(2);
        if (plural.endsWith("ies") && plural.length() > 3) {
            tries.add(plural.substring(0, plural.length() - 3) + "y");
        } else if (plural.endsWith("es") && plural.length() > 2) {
            tries.add(plural.substring(0, plural.length() - 2));
            tries.add(plural.substring(0, plural.length() - 1));
        } else if (plural.endsWith("s") && !plural.endsWith("ss")
                && plural.length() > 1) {
            tries.add(plural.substring(0, plural.length() - 1));
        }
        for (String one : tries) {
            String zh = store.lookup(one);
            if (zh == null || zh.isBlank()) {
                zh = store.lookupTerm(one);
            }
            if (zh != null && !zh.isBlank()) {
                return zh;
            }
        }
        return null;
    }

    static boolean dropsSpaceBefore(String before, String translation) {
        return before.length() >= 2
                && before.charAt(before.length() - 1) == ' '
                && isHan(before.charAt(before.length() - 2))
                && !translation.isEmpty() && isHan(translation.charAt(0));
    }

    static boolean dropsSpaceAfter(String translation, String text, int at) {
        return !translation.isEmpty() && isHan(translation.charAt(translation.length() - 1))
                && at + 1 < text.length()
                && text.charAt(at) == ' '
                && isHan(text.charAt(at + 1));
    }

    private static boolean isPictograph(char c) {
        return c > 0x7F && !isHan(c) && !Character.isLetterOrDigit(c)
                && !Character.isWhitespace(c);
    }

    private static boolean isHan(char c) {
        Character.UnicodeScript script = Character.UnicodeScript.of(c);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || (c >= '＀' && c <= '￯')
                || (c >= '　' && c <= '〿');
    }

    public static Component unslantAll(Component line) {
        return DisplayStyles.unslantAll(line);
    }

    public static boolean columnsAreLeftAligned(List<StyledText> lines) {
        return ColumnAligner.columnsAreLeftAligned(lines);
    }

    public static Component translateChat(StyledText message, TranslationStore store) {
        return ChatTranslator.translateChat(message, store);
    }

    public static Component translateChat(StyledText message, TranslationStore store, Boolean centred) {
        return ChatTranslator.translateChat(message, store, centred);
    }

    public static Component translateChat(StyledText message, TranslationStore store, Boolean centred,
                                          boolean inPanel) {
        return ChatTranslator.translateChat(message, store, centred, inPanel);
    }

    public static LineParts nameFramed(StyledText line, TranslationStore store) {
        return ChatTranslator.nameFramed(line, store);
    }

    public static Component translateFramed(StyledText line, LineParts framed, TranslationStore store) {
        return ChatTranslator.translateFramed(line, framed, store);
    }

    public static boolean[] chatCentred(List<StyledText> rows) {
        return ChatRealigner.chatCentred(rows);
    }

    public static boolean chatPanel(List<StyledText> rows) {
        return ChatPanelFlow.chatPanel(rows);
    }

    public static boolean panelRow(StyledText row) {
        return ChatPanelFlow.panelRow(row);
    }

    public static Component[] flowPanel(List<StyledText> rows, TranslationStore store, boolean[] centred) {
        return ChatPanelFlow.flowPanel(rows, store, centred);
    }

    public static Component[] flowColumn(List<StyledText> rows, TranslationStore store, boolean[] centred) {
        return ChatPanelFlow.flowColumn(rows, store, centred);
    }
}
