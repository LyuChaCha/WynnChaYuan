package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.render.PairedFont;
import com.wynnchayuan.translate.ColumnAligner.Piece;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

public final class LineTranslator {

    private LineTranslator() {}

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
        LineParts parts = LineParts.of(line);
        Component whole = translateWholeLine(line, parts, store, centered, leftAligned);
        if (whole != null) {
            return sameTextKeepsStyle(line, keepClick(line, DisplayStyles.unslant(dropIconSpaces(whole))));
        }
        Component perLine = translatePerLine(line, parts, store, centered);
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
        List<ClickEvent> found = new ArrayList<>();
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
                return Optional.empty();
            }
            if (!seenText[0] && text.isBlank() && iconFont(style)) {
                dropped[0] = true;
                return Optional.empty();
            }
            if (hasLetterOrDigit(text) && !SpaceOffset.isSpaceFont(style) && !iconFont(style)) {
                seenText[0] = true;
            }
            out.append(Component.literal(text).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return dropped[0] ? out : line;
    }

    private static boolean hasLetterOrDigit(String text) {
        for (int at = 0; at < text.length(); ) {
            int cp = text.codePointAt(at);
            at += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) {
                return true;
            }
        }
        return false;
    }

    private static boolean iconFont(Style style) {
        if (!(style.getFont() instanceof FontDescription.Resource r)
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
        Component rebuilt = TemplateRenderer.rebuild(translated, LineParts.of(line), store);
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
                    PairedFont.absolutelyPositioned(style)
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

    private static final Pattern VALUE = Pattern.compile(
                    "[+-]?\\d[\\d,.]*%?(?:\\s+to\\s+[+-]?\\d[\\d,.]*%?)?");

    public static Component translateChunk(String raw, Style style,
                                           TranslationStore store) {
        return DisplayStyles.unslant(translateOneSegment(raw, style, store, false));
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
        Component rebuilt = TemplateRenderer.rebuild(hit, parts, store);
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

    private static Component translateWholeLine(StyledText line, LineParts plain, TranslationStore store,
                                                boolean centered, boolean leftAligned) {
        LineParts parts = plain;
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
        Component rebuilt = TemplateRenderer.rebuild(translated, parts, store);
        if (rebuilt == null) {
            return null;
        }
        Component result = RowRealigner.realign(line, rebuilt, centered, leftAligned);
        LineDebug.record(line, result);
        return result;
    }

    private static Component translatePerLine(StyledText line, LineParts parts, TranslationStore store,
                                              boolean centered) {
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
        Component rebuilt = TemplateRenderer.rebuild(joined.toString(), parts, store);
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

    static final String NL = "\n";

    static boolean hasContent(String text) {
        for (int at = 0; at < text.length(); ) {
            int cp = text.codePointAt(at);
            at += Character.charCount(cp);
            if (!Character.isWhitespace(cp) && !GlyphSplitter.isGlyphCodePoint(cp)) {
                return true;
            }
        }
        return false;
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

    public static List<Component> translateBlock(List<StyledText> run, TranslationStore store, boolean[] centered) {
        return BlockTranslator.translateBlock(run, store, centered);
    }

    public static List<Component> translateBlock(List<StyledText> run, TranslationStore store, boolean[] centered,
                                                 Function<StyledText, LineParts> partsOf) {
        return BlockTranslator.translateBlock(run, store, centered, partsOf);
    }

    public static void noteBlockMiss(String template, TranslationStore store) {
        BlockTranslator.noteBlockMiss(template, store);
    }
}
