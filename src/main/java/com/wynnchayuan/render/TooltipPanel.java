package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.PanelGap;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

public final class TooltipPanel {

    private static List<Component> lastDiagnosed = List.of();

    private TooltipPanel() {}

    public static void render(GuiGraphics graphics, List<Component> tooltip,
                              int mouseX, int mouseY, TranslationStore store) {
        if (PanelsHidden.hidden() || tooltip == null || tooltip.isEmpty()) {
            return;
        }
        tooltip = ThirdPartySections.strip(tooltip);
        List<Component> lines = TooltipTranslator.translateLinesCached(tooltip, store);
        if (!tooltip.equals(lastDiagnosed)) {
            lastDiagnosed = List.copyOf(tooltip);
            WynnChaYuan.store().noteEvent(lines.isEmpty() ? "panel.noMatch" : "panel.shown");
            if (lines.isEmpty()) {
                TooltipDebug.dump(tooltip);
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        ModConfig cfg = WynnChaYuan.config();
        PanelBox drawn = cfg.panelAnchor() == ModConfig.PanelAnchor.FIXED
                ? drawPinned(graphics, lines, cfg.fixedX(), cfg.fixedY())
                : drawBeside(graphics, tooltip, lines, mouseX, mouseY,
                        PanelGap.layout(cfg.panelGap()), cfg.panelSide(), PendingTooltip.pushed())
                        .panel();
        PanelShot.note(drawn.x(), drawn.y(), drawn.w(), drawn.h(), lines.get(0).getString());
    }

    record Beside(PanelBox panel, PanelBox original) {

        static final Beside NONE = new Beside(PanelBox.NONE, PanelBox.NONE);
    }

    static Beside drawBeside(GuiGraphics graphics, List<Component> tooltip, List<Component> lines,
                             int mouseX, int mouseY, int gap, ModConfig.PanelSide side,
                             boolean withOriginal) {
        if (tooltip == null || tooltip.isEmpty() || lines == null || lines.isEmpty()) {
            return Beside.NONE;
        }
        Minecraft mc = Minecraft.getInstance();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int[] panelBox = TooltipPlacement.box(mc, lines);
        TooltipPlacement.Place spot = TooltipPlacement.place(mc, lines, tooltip, mouseX, mouseY,
                screenW, screenH, gap, side);
        int[] at = keepOnScreen(spot.panelX(), spot.panelY(), panelBox, screenW, screenH);
        int x = WynnventoryBox.place(at[0], at[1], panelBox[0] + 8, panelBox[1] + 8,
                spot.leftSide(), spot.rightSide(), screenW, gap);
        PanelBox original = PanelBox.NONE;
        if (withOriginal) {
            TooltipPlacement.drawAt(graphics, mc, tooltip, spot.originalX(), spot.originalY());
            int[] originalBox = TooltipPlacement.box(mc, tooltip);
            original = new PanelBox(spot.originalX() - SHOT_MARGIN, spot.originalY() - SHOT_MARGIN,
                    originalBox[0] + SHOT_MARGIN * 2, originalBox[1] + SHOT_MARGIN * 2);
        }
        return new Beside(paint(graphics, mc, lines, x, at[1], panelBox), original);
    }

    static PanelBox drawPinned(GuiGraphics graphics, List<Component> lines, int x, int y) {
        if (lines == null || lines.isEmpty()) {
            return PanelBox.NONE;
        }
        Minecraft mc = Minecraft.getInstance();
        int[] panelBox = TooltipPlacement.box(mc, lines);
        int[] at = keepOnScreen(x, y, panelBox, graphics.guiWidth(), graphics.guiHeight());
        return paint(graphics, mc, lines, at[0], at[1], panelBox);
    }

    private static int[] keepOnScreen(int x, int y, int[] panelBox, int screenW, int screenH) {
        int panelW = panelBox[0] + 8;
        int panelH = panelBox[1] + 8;
        return new int[] {Math.max(0, Math.min(x, Math.max(0, screenW - panelW))),
                Math.max(0, Math.min(y, Math.max(0, screenH - panelH)))};
    }

    private static PanelBox paint(GuiGraphics graphics, Minecraft mc, List<Component> lines,
                                  int x, int y, int[] panelBox) {
        List<ClientTooltipComponent> components = new ArrayList<>(lines.size());
        for (Component line : lines) {
            components.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        }
        OriginalTooltipBox.beginOwn();
        try {
            graphics.renderTooltip(mc.font, components, x, y, EXACT, null);
        } finally {
            OriginalTooltipBox.endOwn();
        }
        return new PanelBox(x - SHOT_MARGIN, y - SHOT_MARGIN,
                panelBox[0] + SHOT_MARGIN * 2, panelBox[1] + SHOT_MARGIN * 2);
    }

    private static final int SHOT_MARGIN = 4;

    static final ClientTooltipPositioner EXACT =
            (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) -> new Vector2i(x, y);

    public static void noteShot(GuiGraphics graphics, List<Component> tooltip,
                                int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || tooltip == null || tooltip.isEmpty()) {
            return;
        }
        int[] shotBox = TooltipPlacement.box(mc, tooltip);
        int w = shotBox[0];
        int h = shotBox[1];

        Vector2ic at = net.minecraft.client.gui.screens.inventory.tooltip
                .DefaultTooltipPositioner.INSTANCE.positionTooltip(
                        graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY, w, h);
        int x = at.x();
        int y = at.y();
        PanelShot.note(x - SHOT_MARGIN, y - SHOT_MARGIN,
                       w + SHOT_MARGIN * 2, h + SHOT_MARGIN * 2,
                       tooltip.get(0).getString());
    }

    public static boolean wouldPush(List<Component> tooltip, int mouseX, int mouseY,
                                    TranslationStore store) {
        if (PanelsHidden.hidden()) {
            return false;
        }
        return TooltipPlacement.wouldPush(tooltip, mouseX, mouseY, store);
    }

    public static List<Component> translateLines(List<Component> tooltip, TranslationStore store) {
        return TooltipTranslator.translateLines(tooltip, store);
    }

    public static List<Component> translateInPlace(List<Component> tooltip, TranslationStore store) {
        return TooltipTranslator.translateInPlace(tooltip, store);
    }

    public static String bareName(String template) {
        return TooltipShape.bareName(template);
    }
}
