package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

final class DialoguePanelRenderer {

    private DialoguePanelRenderer() {}

    static void render(GuiGraphics graphics) {
        List<Component> lines = DialogueOverlay.current;
        List<List<Component>> options = DialogueOverlay.choices;
        if (lines.isEmpty() && options.isEmpty()) {
            return;
        }
        float alpha = Fade.alphaFor(DialogueOverlay.lastUpdate, WynnChaYuan.config().dialogueHoldMs());
        if (alpha <= 0f) {
            DialogueOverlay.current = List.of();
            DialogueOverlay.choices = List.of();
            DialogueOverlay.rawChoices = List.of();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (WynnChaYuan.config().dialogueMode() == CollectorConfig.DialogueMode.REPLACE
                && !DialogueRewriter.bodyTooLong()) {
            if (!options.isEmpty() && choicesInPanel()) {
                drawChoices(graphics, mc, options, alpha);
            }
            return;
        }
        if (DialogueOverlay.speaker != null && !lines.isEmpty()) {
            List<Component> withName = new ArrayList<>(lines.size() + 1);
            withName.add(speakerLine(DialogueOverlay.speaker));
            withName.addAll(lines);
            lines = withName;
        }
        int lineHeight = mc.font.lineHeight + 1;
        boolean sized = WynnChaYuan.config()
                .hasOverlaySize(CollectorConfig.Overlay.DIALOGUE);
        int cap = Math.min(MAX_BOX_W, graphics.guiWidth() - CHOICE_EDGE * 2);
        int natural = sized ? 0 : widthOf(mc, lines);
        boolean folded = !sized && natural > cap;
        int boxW = sized ? dialogueWidth(0) : folded ? cap : natural;
        int rowCount = folded ? wrapAll(mc, lines, boxW - DialogueOverlay.PADDING * 2).size() : lines.size();
        int boxH = lines.isEmpty() ? 0
                : atLeastTall(rowCount * lineHeight + DialogueOverlay.PADDING * 2);

        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - DialogueOverlay.BOTTOM_MARGIN - boxH;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.DIALOGUE)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.DIALOGUE) - boxW / 2;
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.DIALOGUE);
        }
        int[] at = Boxes.intoScreen(x, y, boxW, boxH, graphics.guiWidth(), graphics.guiHeight());
        x = at[0];
        y = at[1];

        if (!lines.isEmpty()) {
            if (sized || folded) {
                drawSized(graphics, mc, lines, x, y, boxW, alpha);
            } else {
                draw(graphics, mc, lines, x + boxW / 2, y, alpha, true);
            }
        }
        if (!options.isEmpty() && choicesInPanel()) {
            drawChoices(graphics, mc, options, alpha);
        }
    }

    private static boolean choicesInPanel() {
        return WynnChaYuan.config().choiceMode() == CollectorConfig.DialogueMode.PANEL;
    }

    private static void drawInPlace(GuiGraphics graphics, Minecraft mc,
                                    List<Component> lines, List<Component> options,
                                    float alpha) {
        int boxW = dialogueWidth(Math.max(MIN_BOX_W, Math.min(MAX_BOX_W,
                graphics.guiWidth() * 5 / 8)));
        int inner = boxW - DialogueOverlay.PADDING * 2 - 2;
        int lineHeight = mc.font.lineHeight + 1;

        List<FormattedCharSequence> body = wrapAll(mc, lines, inner);
        List<FormattedCharSequence> picks = wrapAll(mc, options, inner);

        int bodyH = body.isEmpty() ? 0
                : atLeastTall(body.size() * lineHeight + DialogueOverlay.PADDING * 2);
        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - IN_PLACE_MARGIN - bodyH;

        if (!body.isEmpty()) {
            Boxes.draw(graphics, x, y, boxW, bodyH, alpha);
            int textY = y + DialogueOverlay.PADDING;
            for (FormattedCharSequence line : body) {
                graphics.drawString(mc.font, line, x + DialogueOverlay.PADDING + 1, textY,
                        Colors.fade(Colors.TEXT, alpha));
                textY += lineHeight;
            }
            if (DialogueOverlay.speaker != null) {
                int nameW = mc.font.width(DialogueOverlay.speaker) + DialogueOverlay.PADDING * 2 + 2;
                int nameH = mc.font.lineHeight + DialogueOverlay.PADDING;
                Boxes.draw(graphics, x, y - nameH + 1, nameW, nameH, alpha);
                graphics.drawString(mc.font, DialogueOverlay.speaker, x + DialogueOverlay.PADDING + 1,
                        y - nameH + DialogueOverlay.PADDING / 2 + 2,
                        Colors.fade(DialogueOverlay.SPEAKER_COLOUR.getValue() | 0xFF000000, alpha));
            }
            if (DialogueOverlay.needsShift) {
                String hint = SHIFT_HINT.get();
                int hintW = mc.font.width(hint);
                graphics.drawString(mc.font, hint,
                        x + (boxW - hintW) / 2, y + bodyH + 3,
                        Colors.fade(Colors.TEXT, alpha * 0.75f));
            }
        }

        if (!picks.isEmpty()) {
            int pickH = picks.size() * lineHeight + DialogueOverlay.PADDING * 2;
            int pickY = y - pickH - CHOICE_GAP - (DialogueOverlay.speaker == null ? 0 : mc.font.lineHeight);
            Boxes.draw(graphics, x, pickY, boxW, pickH, alpha);
            int textY = pickY + DialogueOverlay.PADDING;
            for (FormattedCharSequence line : picks) {
                graphics.drawString(mc.font, line, x + DialogueOverlay.PADDING + 1, textY,
                        Colors.fade(Colors.TEXT, alpha));
                textY += lineHeight;
            }
        }
    }

    private static void drawChoices(GuiGraphics graphics, Minecraft mc,
                                    List<List<Component>> options, float alpha) {
        CollectorConfig cfg = WynnChaYuan.config();
        int boxW = cfg.hasOverlaySize(CollectorConfig.Overlay.CHOICES)
                ? cfg.overlayW(CollectorConfig.Overlay.CHOICES)
                : Math.max(MIN_CHOICE_W, Math.min(MAX_CHOICE_W,
                        graphics.guiWidth() / 4));
        int inner = boxW - DialogueOverlay.PADDING * 2 - MARKER_W - 2;
        int lineHeight = mc.font.lineHeight + 1;

        List<List<FormattedCharSequence>> rows = new ArrayList<>(options.size());
        int textLines = 0;
        for (List<Component> option : options) {
            List<FormattedCharSequence> wrapped = wrapAll(mc, option, inner);
            if (wrapped.isEmpty()) {
                continue;
            }
            rows.add(wrapped);
            textLines += wrapped.size();
        }
        if (rows.isEmpty()) {
            return;
        }
        int boxH = textLines * lineHeight + DialogueOverlay.PADDING * 2
                + (rows.size() - 1) * (ROW_GAP * 2 + 1);

        int x = graphics.guiWidth() - boxW - CHOICE_EDGE;
        int y = graphics.guiHeight() * 5 / 8;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.CHOICES)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.CHOICES)
                    - boxW / 2;
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.CHOICES);
        }
        x = Math.max(2, Math.min(graphics.guiWidth() - boxW - 2, x));
        y = Math.max(2, Math.min(graphics.guiHeight() - boxH - 2, y));

        Boxes.draw(graphics, x, y, boxW, boxH, alpha);

        int textY = y + DialogueOverlay.PADDING;
        for (int i = 0; i < rows.size(); i++) {
            List<FormattedCharSequence> option = rows.get(i);
            int rowH = option.size() * lineHeight;
            boolean here = DialogueOverlay.picked == i + 1;

            if (here) {
                graphics.fill(x + 1, textY - ROW_GAP + 1, x + boxW - 1,
                              textY + rowH + ROW_GAP - 1,
                              Colors.fade(SELECTED_BG, alpha));
                graphics.drawString(mc.font, MARKER, x + DialogueOverlay.PADDING,
                        textY + (rowH - mc.font.lineHeight) / 2,
                        Colors.fade(Colors.HIGHLIGHT, alpha));
            }
            int colour = here ? Colors.HIGHLIGHT : Colors.TEXT;
            int lineY = textY;
            for (FormattedCharSequence line : option) {
                graphics.drawString(mc.font, line, x + DialogueOverlay.PADDING + MARKER_W, lineY,
                        Colors.fade(colour, alpha));
                lineY += lineHeight;
            }
            textY += rowH;

            if (i < rows.size() - 1) {
                int sepY = textY + ROW_GAP;
                for (int dx = x + DialogueOverlay.PADDING; dx < x + boxW - DialogueOverlay.PADDING;
                        dx += DASH + DASH_GAP) {
                    int end = Math.min(dx + DASH, x + boxW - DialogueOverlay.PADDING);
                    graphics.fill(dx, sepY, end, sepY + 1,
                                  Colors.fade(Colors.FAINT, alpha));
                }
                textY += ROW_GAP * 2 + 1;
            }
        }
    }

    private static final int SELECTED_BG = 0x40FFD24A;

    private static final String MARKER = "\u25B6";

    private static final int MARKER_W = 8;

    private static final int ROW_GAP = 2;

    private static final int DASH = 3;

    private static final int DASH_GAP = 2;

    private static final int MIN_CHOICE_W = 120;

    private static final int MAX_CHOICE_W = 260;

    private static final int CHOICE_EDGE = 8;

    // MC 的 font.split 只在空白斷行，中文標點會落到行首
    private static List<FormattedCharSequence> wrapAll(Minecraft mc,
                                                       List<Component> lines, int width) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component line : lines) {
            for (Component row : fold(line, Math.max(16, width))) {
                out.add(row.getVisualOrderText());
            }
        }
        return out;
    }

    static List<Component> fold(Component line, int width) {
        List<com.wynnchayuan.capture.LineParts.Piece> pieces = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        line.visit((style, text) -> {
            pieces.add(new com.wynnchayuan.capture.LineParts.Piece(text, style));
            plain.append(text);
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        List<int[]> rows = DialogueLayout.flow(plain.toString(), null, width);
        if (rows.size() <= 1) {
            return List.of(line);
        }
        List<Component> out = new ArrayList<>(rows.size());
        for (int[] row : rows) {
            net.minecraft.network.chat.MutableComponent part = Component.empty();
            for (var piece : DialogueAccents.cut(pieces, row[0], row[1])) {
                part.append(Component.literal(piece.text()).withStyle(piece.style()));
            }
            out.add(part);
        }
        return out;
    }

    private static Component speakerLine(String said) {
        return Component.literal(com.wynnchayuan.client.T.s("dialogue.speaker", said)).withStyle(
                net.minecraft.network.chat.Style.EMPTY
                        .withColor(DialogueOverlay.SPEAKER_COLOUR).withBold(true));
    }

    private static final int MIN_BOX_W = 240;

    private static final int MAX_BOX_W = 420;

    private static final java.util.function.Supplier<String> SHIFT_HINT =
            () -> com.wynnchayuan.client.T.s("dialogue.shift_hint");

    private static final int IN_PLACE_MARGIN = 55;

    private static void draw(GuiGraphics graphics, Minecraft mc, List<Component> lines,
                             int centerX, int y, float alpha, boolean withBox) {
        int lineHeight = mc.font.lineHeight + 1;
        int boxW = widthOf(mc, lines);
        int boxH = lines.size() * lineHeight + DialogueOverlay.PADDING * 2;
        int x = centerX - boxW / 2;
        if (withBox) {
            Boxes.draw(graphics, x, y, boxW, boxH, alpha);
        }
        int textY = y + DialogueOverlay.PADDING;
        for (Component line : lines) {
            int lineX = withBox
                    ? x + DialogueOverlay.PADDING
                    : centerX - mc.font.width(line) / 2;
            graphics.drawString(mc.font, line, lineX, textY,
                    Colors.fade(Colors.TEXT, alpha));
            textY += lineHeight;
        }
    }

    private static int widthOf(Minecraft mc, List<Component> lines) {
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, mc.font.width(line));
        }
        return width + DialogueOverlay.PADDING * 2;
    }

    private static int dialogueWidth(int auto) {
        CollectorConfig cfg = WynnChaYuan.config();
        return cfg.hasOverlaySize(CollectorConfig.Overlay.DIALOGUE)
                ? cfg.overlayW(CollectorConfig.Overlay.DIALOGUE)
                : auto;
    }

    private static int atLeastTall(int auto) {
        CollectorConfig cfg = WynnChaYuan.config();
        return cfg.hasOverlaySize(CollectorConfig.Overlay.DIALOGUE)
                ? Math.max(auto, cfg.overlayH(CollectorConfig.Overlay.DIALOGUE))
                : auto;
    }

    private static void drawSized(GuiGraphics graphics, Minecraft mc,
                                  List<Component> lines, int x, int y,
                                  int boxW, float alpha) {
        int lineHeight = mc.font.lineHeight + 1;
        List<FormattedCharSequence> rows = wrapAll(mc, lines, boxW - DialogueOverlay.PADDING * 2);
        int boxH = atLeastTall(rows.size() * lineHeight + DialogueOverlay.PADDING * 2);
        Boxes.draw(graphics, x, y, boxW, boxH, alpha);
        int textY = y + DialogueOverlay.PADDING;
        for (FormattedCharSequence row : rows) {
            graphics.drawString(mc.font, row, x + DialogueOverlay.PADDING, textY,
                    Colors.fade(Colors.TEXT, alpha));
            textY += lineHeight;
        }
    }

    private static final int CHOICE_GAP = 3;
}
