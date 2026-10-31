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
        if (tooltip == null || tooltip.isEmpty()) {
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

        Minecraft mc = Minecraft.getInstance();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int gap = PanelGap.layout(WynnChaYuan.config().panelGap());

        int[] panelBox = TooltipPlacement.box(mc, lines);
        int panelW = panelBox[0] + 8;
        int panelH = panelBox[1] + 8;

        TooltipPlacement.Place spot;
        if (WynnChaYuan.config().panelAnchor() == ModConfig.PanelAnchor.FIXED) {
            spot = new TooltipPlacement.Place(WynnChaYuan.config().fixedX(), WynnChaYuan.config().fixedY(),
                    0, 0, false, 0, 0);
        } else {
            spot = TooltipPlacement.place(mc, lines, tooltip, mouseX, mouseY, screenW, screenH, gap);
        }
        int x = Math.max(0, Math.min(spot.panelX(), Math.max(0, screenW - panelW)));
        int y = Math.max(0, Math.min(spot.panelY(), Math.max(0, screenH - panelH)));
        if (WynnChaYuan.config().panelAnchor() != ModConfig.PanelAnchor.FIXED) {
            x = WynnventoryBox.place(x, y, panelW, panelH,
                    spot.leftSide(), spot.rightSide(), screenW, gap);
        }

        if (PendingTooltip.pushed()) {
            TooltipPlacement.drawAt(graphics, mc, tooltip, spot.originalX(), spot.originalY());
        }

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
        String title = lines.isEmpty() ? null : lines.get(0).getString();
        PanelShot.note(x - SHOT_MARGIN, y - SHOT_MARGIN,
                panelBox[0] + SHOT_MARGIN * 2, panelBox[1] + SHOT_MARGIN * 2, title);
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
