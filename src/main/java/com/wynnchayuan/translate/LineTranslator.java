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
            String swapped = TermSubstitution.termsWithin(translated, store);
            if (swapped != null) {
                translated = swapped;
            }
            translated = wrapToBlock(translated, run);
            rewrapped = true;
        }
        translated = TermSubstitution.breakBeforeOriginal(translated, widestOf(run), store,
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
                if (!body.isEmpty() && TemplateLookup.isConnectorSegment(body)) {
                    connectorAt.add(pieces.size());
                }
                pieces.add(Piece.text(raw.substring(lead.length()), style));
                continue;
            }
            any = true;
            Component orig = DisplayStyles.literal(body, style);
            pieces.add(Piece.translated(
                    com.wynnchayuan.render.PairedFont.absolutelyPositioned(style)
                            ? TemplateLookup.padSlot(replaced, orig, style) : replaced,
                    orig));
            if (!tail.isEmpty()) {
                pieces.add(Piece.space(SpaceOffset.decode(tail),
                        SpaceOffset.styleFor(style)));
            }
        }
        if (!any) {
            return null;
        }
        TemplateLookup.translateConnectors(pieces, connectorAt, store);

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
        String hit = TemplateLookup.lookup(parts.template(), store, percent);
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
        String tail = TemplateLookup.reattach(rebuilt.getString(), suffix);
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
        String translated = TemplateLookup.lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            LineParts framed = ChatTranslator.framedOnMiss(line, parts, store);
            translated = framed == null ? null : TemplateLookup.lookup(framed.template(), store);
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
            String hit = GlyphSplitter.hasLetter(row) ? TemplateLookup.lookup(row, store) : null;
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
        accents = TermSubstitution.withTranslations(accents, String.join(String.valueOf(NEWLINE), translated), store);
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
        flowed = TermSubstitution.forcedTerms(flowed, store);

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
        while (mid < tail && TermSubstitution.isPictograph(text.charAt(mid))) {
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
            while (start > lead && TermSubstitution.isPictograph(text.charAt(start - 1))) {
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
            return TermSubstitution.isPictograph(text.charAt(0)) ? run.style() : null;
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
        if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || TermSubstitution.isHan(c)) {
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
            TranslationStore.Term term = store == null ? null : TermSubstitution.termOutside(store, text, from, kept);
            boolean longer = term != null && at >= 0 && term.start() == at
                    && term.end() - term.start() >= accents.get(which).text().length();
            if (term != null && (at < 0 || term.start() < at || longer)) {
                if (term.start() > from) {
                    String before = text.substring(from, term.start());
                    if (TermSubstitution.dropsSpaceBefore(before, term.translation())) {
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
                if (TermSubstitution.dropsSpaceAfter(term.translation(), text, from)) {
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
            boolean keep = TermSubstitution.overlapping(kept, at, at + accent.text().length()) != null;
            String shown = store == null || keep ? null : store.lookupTerm(accent.text());
            if (shown == null && store != null && !keep) {
                shown = TermSubstitution.termsWithin(accent.text(), store);
            }
            out.append(DisplayStyles.literal(shown != null ? shown : accent.text(),
                               DisplayStyles.forDisplay(accent.style())));
            used[which] = true;
            from = at + accent.text().length();
        }
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

    public static Component translateFloating(StyledText label, TranslationStore store) {
        return LabelTranslator.translateFloating(label, store);
    }

    public static Component translateLabel(StyledText label, TranslationStore store) {
        return LabelTranslator.translateLabel(label, store);
    }
}
