package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.PanelGap;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PanelPreview {

    public record Sample(List<Component> lines, List<List<Component>> options, int picked) {

        public static Sample lines(List<Component> lines) {
            return new Sample(List.copyOf(lines), List.of(), 0);
        }

        public static Sample options(List<List<Component>> options, int picked) {
            return new Sample(List.of(), List.copyOf(options), picked);
        }
    }

    private static final int TOOLTIP_X = 20;
    private static final int TOOLTIP_Y = 20;

    private static volatile boolean editing;

    private PanelPreview() {}

    public static boolean editing() {
        return editing;
    }

    public static void editing(boolean on) {
        editing = on;
    }

    public static PanelBox draw(GuiGraphics graphics, ModConfig.Overlay which, Sample sample,
                                PanelPlace place) {
        if (graphics == null || which == null || sample == null) {
            return PanelBox.NONE;
        }
        PanelPlace at = place == null ? PanelPlace.AUTO : place;
        try {
            return switch (which) {
                case TOOLTIP -> TooltipPanel.drawPinned(graphics, sample.lines(),
                        at.x() == null ? TOOLTIP_X : at.x(), at.y() == null ? TOOLTIP_Y : at.y());
                case TRACKER -> TrackerOverlay.draw(graphics, sample.lines(), at);
                case DIALOGUE -> DialoguePanelRenderer.drawDialogue(graphics, sample.lines(), at, 1f);
                case CHOICES -> DialoguePanelRenderer.drawChoices(graphics, sample.options(),
                        sample.picked(), at, 1f);
                case NAMETAG -> LookAtTranslator.drawRow(graphics, sample.lines(), 1f, at);
            };
        } catch (Throwable t) {
            ErrorDebug.note("PanelPreview#draw", which.name(), t);
            return PanelBox.NONE;
        }
    }

    public record Following(PanelBox panel, PanelBox item) {

        public static final Following NONE = new Following(PanelBox.NONE, PanelBox.NONE);
    }

    public static Following drawFollowing(GuiGraphics graphics, Sample original, Sample panel,
                                          int mouseX, int mouseY, int gap,
                                          ModConfig.PanelSide side) {
        if (graphics == null || original == null || panel == null) {
            return Following.NONE;
        }
        try {
            TooltipPanel.Beside drawn = TooltipPanel.drawBeside(graphics, original.lines(),
                    panel.lines(), mouseX, mouseY, PanelGap.layout(gap),
                    side == null ? ModConfig.PanelSide.AUTO : side, true);
            return new Following(drawn.panel(), drawn.original());
        } catch (Throwable t) {
            ErrorDebug.note("PanelPreview#drawFollowing", null, t);
            return Following.NONE;
        }
    }

    public static Component speaker(String name) {
        return DialoguePanelRenderer.speakerLine(name);
    }

    public static String trackerHeading(String type) {
        return TrackerOverlay.heading(type);
    }
}
