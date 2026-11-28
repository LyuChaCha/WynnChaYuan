package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.client.T;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
        ModConfig cfg = WynnChaYuan.config();
        PanelPlace choicePlace = PanelPlace.saved(cfg, ModConfig.Overlay.CHOICES);
        if (cfg.dialogueMode() == ModConfig.DialogueMode.REPLACE
                && !DialogueRewriter.bodyTooLong()) {
            if (!options.isEmpty() && choicesInPanel()) {
                drawChoices(graphics, options, DialogueOverlay.picked, choicePlace, alpha);
            }
            return;
        }
        if (DialogueOverlay.speaker != null && !lines.isEmpty()) {
            List<Component> withName = new ArrayList<>(lines.size() + 1);
            withName.add(speakerLine(DialogueOverlay.speaker));
            withName.addAll(lines);
            lines = withName;
        }
        drawDialogue(graphics, lines, PanelPlace.saved(cfg, ModConfig.Overlay.DIALOGUE), alpha);
        if (!options.isEmpty() && choicesInPanel()) {
            drawChoices(graphics, options, DialogueOverlay.picked, choicePlace, alpha);
        }
    }

    static PanelBox drawDialogue(GuiGraphics graphics, List<Component> lines, PanelPlace place,
                                 float alpha) {
        if (lines.isEmpty()) {
            return PanelBox.NONE;
        }
        Minecraft mc = Minecraft.getInstance();
        int lineHeight = mc.font.lineHeight + 1;
        boolean sized = place.sized();
        int cap = Math.min(MAX_BOX_W, graphics.guiWidth() - CHOICE_EDGE * 2);
        int natural = sized ? 0 : widthOf(mc, lines);
        boolean folded = !sized && natural > cap;
        int boxW = sized ? place.w() : folded ? cap : natural;
        int rowCount = folded ? wrapAll(mc, lines, boxW - DialogueOverlay.PADDING * 2).size() : lines.size();
        int boxH = atLeastTall(rowCount * lineHeight + DialogueOverlay.PADDING * 2, place);

        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - DialogueOverlay.BOTTOM_MARGIN - boxH;
        if (place.placed()) {
            x = place.x() - boxW / 2;
            y = place.y();
        }
        int[] at = Boxes.intoScreen(x, y, boxW, boxH, graphics.guiWidth(), graphics.guiHeight());
        x = at[0];
        y = at[1];

        if (sized || folded) {
            boxH = drawSized(graphics, mc, lines, x, y, boxW, alpha, place);
        } else {
            draw(graphics, mc, lines, x + boxW / 2, y, alpha, true);
        }
        return new PanelBox(x, y, boxW, boxH);
    }

    private static boolean choicesInPanel() {
        return WynnChaYuan.config().choiceMode() == ModConfig.DialogueMode.PANEL;
    }

    static PanelBox drawChoices(GuiGraphics graphics, List<List<Component>> options, int picked,
                                PanelPlace place, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        int boxW = place.sized()
                ? place.w()
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
            return PanelBox.NONE;
        }
        int boxH = textLines * lineHeight + DialogueOverlay.PADDING * 2
                + (rows.size() - 1) * (ROW_GAP * 2 + 1);

        int x = graphics.guiWidth() - boxW - CHOICE_EDGE;
        int y = graphics.guiHeight() * 5 / 8;
        if (place.placed()) {
            x = place.x() - boxW / 2;
            y = place.y();
        }
        x = Math.max(2, Math.min(graphics.guiWidth() - boxW - 2, x));
        y = Math.max(2, Math.min(graphics.guiHeight() - boxH - 2, y));

        Boxes.draw(graphics, x, y, boxW, boxH, alpha);

        int textY = y + DialogueOverlay.PADDING;
        for (int i = 0; i < rows.size(); i++) {
            List<FormattedCharSequence> option = rows.get(i);
            int rowH = option.size() * lineHeight;
            boolean here = picked == i + 1;

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
        return new PanelBox(x, y, boxW, boxH);
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
        List<LineParts.Piece> pieces = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        line.visit((style, text) -> {
            pieces.add(new LineParts.Piece(text, style));
            plain.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        List<int[]> rows = DialogueLayout.flow(plain.toString(), null, width);
        if (rows.size() <= 1) {
            return List.of(line);
        }
        List<Component> out = new ArrayList<>(rows.size());
        for (int[] row : rows) {
            MutableComponent part = Component.empty();
            for (var piece : DialogueAccents.cut(pieces, row[0], row[1])) {
                part.append(Component.literal(piece.text()).withStyle(piece.style()));
            }
            out.add(part);
        }
        return out;
    }

    static Component speakerLine(String said) {
        return Component.literal(T.s("dialogue.speaker", said)).withStyle(Style.EMPTY
                        .withColor(DialogueOverlay.SPEAKER_COLOUR).withBold(true));
    }

    private static final int MAX_BOX_W = 420;

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

    private static int atLeastTall(int auto, PanelPlace place) {
        return place.sized() ? Math.max(auto, place.h()) : auto;
    }

    private static int drawSized(GuiGraphics graphics, Minecraft mc,
                                 List<Component> lines, int x, int y,
                                 int boxW, float alpha, PanelPlace place) {
        int lineHeight = mc.font.lineHeight + 1;
        List<FormattedCharSequence> rows = wrapAll(mc, lines, boxW - DialogueOverlay.PADDING * 2);
        int boxH = atLeastTall(rows.size() * lineHeight + DialogueOverlay.PADDING * 2, place);
        Boxes.draw(graphics, x, y, boxW, boxH, alpha);
        int textY = y + DialogueOverlay.PADDING;
        for (FormattedCharSequence row : rows) {
            graphics.drawString(mc.font, row, x + DialogueOverlay.PADDING, textY,
                    Colors.fade(Colors.TEXT, alpha));
            textY += lineHeight;
        }
        return boxH;
    }

}
