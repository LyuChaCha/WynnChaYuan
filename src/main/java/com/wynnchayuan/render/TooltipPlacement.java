package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.PanelGap;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

import java.util.List;

final class TooltipPlacement {

    private TooltipPlacement() {}

    static int[] box(Minecraft mc, List<Component> lines) {
        List<ClientTooltipComponent> parts = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
        int w = 0;
        for (ClientTooltipComponent part : parts) {
            w = Math.max(w, part.getWidth(mc.font));
        }
        return new int[] {w, (parts.size() == 1 ? -2 : 0) + height(parts)};
    }

    static int height(List<ClientTooltipComponent> components) {
        int h = 0;
        for (ClientTooltipComponent c : components) {
            h += c.getHeight(Minecraft.getInstance().font);
        }
        return h;
    }

    record Place(int panelX, int panelY, int originalX, int originalY, boolean overlaps,
                 int leftSide, int rightSide) {}

    // 仿原版 DefaultTooltipPositioner：游標右下 12/-12
    static int originalLeftFor(int mouseX, int screenW, int textWidth) {
        int left = mouseX + 12;
        if (left + textWidth > screenW) {
            left = Math.max(left - 24 - textWidth, 4);
        }
        return left;
    }

    static Place place(Minecraft mc, List<Component> lines, List<Component> tooltip,
                       int mouseX, int mouseY, int screenW, int screenH, int gap) {
        int[] panelBox = box(mc, lines);
        int[] originalBox = box(mc, tooltip);
        OriginalTooltipBox.Box drawn = OriginalTooltipBox.find(originalBox[0], originalBox[1]);
        return layout(panelBox[0], panelBox[1], originalBox[0], originalBox[1], drawn,
                mouseX, mouseY, screenW, screenH, gap, WynnChaYuan.config().panelSide());
    }

    static Place layout(int panelTextW, int panelTextH, int originalTextW, int originalTextH,
                        OriginalTooltipBox.Box drawn, int mouseX, int mouseY,
                        int screenW, int screenH, int gap, CollectorConfig.PanelSide side) {
        int panelW = panelTextW + 8;
        int panelH = panelTextH + 8;

        int originalLeft;
        int originalTop;
        float scale;
        if (drawn != null) {
            originalLeft = Math.round(drawn.x());
            originalTop = Math.round(drawn.y());
            scale = drawn.scale();
        } else {
            originalLeft = originalLeftFor(mouseX, screenW, originalTextW);
            originalTop = mouseY - 12;
            if (originalTop + originalTextH + 3 > screenH) {
                originalTop = screenH - originalTextH - 3;
            }
            scale = 1f;
        }
        int frame = OriginalTooltipBox.FRAME;
        int originalFrame = Math.round(frame * scale);
        int shownW = Math.round(originalTextW * scale);
        int shownH = Math.round(originalTextH * scale);

        boolean originalOnRight = originalLeft >= mouseX;
        int rightSide = originalLeft + shownW + originalFrame + gap + frame;
        int leftSide = originalLeft - originalFrame - gap - frame - panelTextW;

        int x = switch (side) {
            case RIGHT -> rightSide;
            case LEFT -> leftSide;
            default -> originalOnRight ? leftSide : rightSide;
        };
        int y = originalTop;

        x = Math.max(0, Math.min(x, Math.max(0, screenW - panelW)));
        y = Math.max(0, Math.min(y, Math.max(0, screenH - panelH)));

        int textLeft = originalLeft - 4;
        int textTop = originalTop - 4;
        boolean overlaps = x < textLeft + shownW + 8 && x + panelW > textLeft
                && y < textTop + shownH + 8 && y + panelH > textTop;

        int originalX = originalLeft;
        int originalY = originalTop;
        if (overlaps) {
            originalX = originalOnRight ? x + panelTextW + frame + gap + frame
                    : x - frame - gap - frame - originalTextW;
            originalY = y;
        }
        return new Place(x, y, originalX, originalY, overlaps, leftSide, rightSide);
    }

    static boolean wouldPush(List<Component> tooltip, int mouseX, int mouseY,
                             TranslationStore store) {
        try {
            if (WynnChaYuan.config() == null
                    || WynnChaYuan.config().panelAnchor() == CollectorConfig.PanelAnchor.FIXED
                    || tooltip == null || tooltip.isEmpty()) {
                return false;
            }
            List<Component> stripped = ThirdPartySections.strip(tooltip);
            List<Component> lines = TooltipTranslator.translateLinesCached(stripped, store);
            if (lines.isEmpty()) {
                return false;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) {
                return false;
            }
            return place(mc, lines, stripped, mouseX, mouseY,
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                    PanelGap.layout(WynnChaYuan.config().panelGap())).overlaps();
        } catch (Throwable t) {
            ErrorDebug.note("TooltipPlacement#wouldPush", null, t);
            return false;
        }
    }

    static void drawAt(GuiGraphics graphics, Minecraft mc,
                       List<Component> lines, int x, int y) {
        List<ClientTooltipComponent> parts = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
        OriginalTooltipBox.beginOwn();
        try {
            graphics.renderTooltip(mc.font, parts, x, y, TooltipPanel.EXACT, null);
        } finally {
            OriginalTooltipBox.endOwn();
        }
    }
}
