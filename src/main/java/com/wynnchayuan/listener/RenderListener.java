package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.PendingTooltip;
import com.wynnchayuan.render.TooltipPanel;
import com.wynnchayuan.render.LookAtTranslator;
import com.wynnchayuan.render.TrackerOverlay;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class RenderListener {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(ItemTooltipRenderEvent.Pre event) {
        try {
            GuiTextCapture.record(event.getTooltips(), event.getItemStack());
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("capture.guiError");
        }
        CollectorConfig.TooltipMode mode = WynnChaYuan.config().tooltipMode();
        if (mode == CollectorConfig.TooltipMode.OFF) {
            return;
        }
        if (mode == CollectorConfig.TooltipMode.REPLACE) {
            replaceInPlace(event);
            return;
        }
        boolean push = false;
        try {
            push = TooltipPanel.wouldPush(event.getTooltips(),
                    event.getMouseX(), event.getMouseY(), WynnChaYuan.translations());
        } catch (Throwable t) {
            push = false;
        }
        if (push) {
            event.setCanceled(true);
        }
        PendingTooltip.set(event.getTooltips(), event.getMouseX(), event.getMouseY(), push);
    }



    public static boolean shiftHeld() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        mc.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        mc.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static CollectorConfig.ItemNames peeked(CollectorConfig config) {
        CollectorConfig.ItemNames mode = config.itemNames();
        if (!config.shiftPeekNames()
                || mode == CollectorConfig.ItemNames.BOTH
                || !shiftHeld()) {
            return mode;
        }
        return mode == CollectorConfig.ItemNames.ON
                ? CollectorConfig.ItemNames.OFF
                : CollectorConfig.ItemNames.ON;
    }

    private static void withPeek(Runnable body) {
        withPeek(() -> {
            body.run();
            return null;
        });
    }

    private static <T> T withPeek(java.util.function.Supplier<T> body) {
        CollectorConfig config = WynnChaYuan.config();
        WynnChaYuan.translations().setNameMode(peeked(config));
        WynnChaYuan.translations().setPeekPlainNames(
                config.shiftPeekNames() && shiftHeld());
        try {
            return body.get();
        } finally {
            WynnChaYuan.translations().setNameMode(config.itemNames());
            WynnChaYuan.translations().setPeekPlainNames(false);
        }
    }

    public static List<Component> translateForeign(List<Component> lines) {
        return withPeek(() -> com.wynnchayuan.render.TooltipPanel
                .translateInPlace(lines, WynnChaYuan.translations()));
    }

    private void replaceInPlace(ItemTooltipRenderEvent.Pre event) {
        try {
            List<Component> original = event.getTooltips();
            if (original == null || original.isEmpty()) {
                return;
            }
            List<Component> out = withPeek(() -> com.wynnchayuan.render.TooltipPanel
                    .translateInPlace(original, WynnChaYuan.translations()));
            if (!out.isEmpty()) {
                event.setTooltips(out);
                WynnChaYuan.store().noteEvent("tooltip.replaced");
                PendingTooltip.set(out, event.getMouseX(), event.getMouseY());
            }
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("render.replaceError");
            com.wynnchayuan.translate.ErrorDebug.note("tooltip.replace", firstLine(event), t);
        }
    }

    public static List<Component> inPlaceLines(List<Component> original) {
        try {
            List<Component> out = withPeek(() -> com.wynnchayuan.render.TooltipPanel
                    .translateInPlace(original, WynnChaYuan.translations()));
            return out == null || out.isEmpty() ? original : out;
        } catch (Throwable t) {
            return original;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltipPosition(com.wynntils.mc.event.TooltipRenderEvent.Position event) {
        try {
            if (com.wynnchayuan.render.TooltipScaleProbe.fittingEnabled()) {
                return;
            }
            var moved = com.wynnchayuan.render.WynnventoryLayout.shift(event.getPositioner(), 1f);
            if (moved != null) {
                event.setPositioner(moved);
            }
        } catch (Throwable t) {
        }
    }

    private static String firstLine(ItemTooltipRenderEvent.Pre event) {
        List<Component> lines = event.getTooltips();
        return lines == null || lines.isEmpty() ? null : lines.get(0).getString();
    }

    public static void renderAfterScreen(GuiGraphics graphics, int mouseX, int mouseY) {
        CollectorConfig.TooltipMode mode = WynnChaYuan.config().tooltipMode();
        if (mode == CollectorConfig.TooltipMode.REPLACE) {
            List<Component> replaced = PendingTooltip.take();
            if (!replaced.isEmpty()) {
                TooltipPanel.noteShot(graphics, replaced,
                        PendingTooltip.mouseX(), PendingTooltip.mouseY());
            }
            return;
        }
        if (mode != CollectorConfig.TooltipMode.PANEL) {
            PendingTooltip.take();
            return;
        }
        List<Component> tooltip = PendingTooltip.take();
        if (tooltip.isEmpty()) {
            return;
        }
        try {
            List<Component> shown = tooltip;
            withPeek(() -> TooltipPanel.render(graphics, shown,
                    PendingTooltip.mouseX(), PendingTooltip.mouseY(),
                    WynnChaYuan.translations()));
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("render.tooltipError");
            com.wynnchayuan.translate.ErrorDebug.note("tooltip.panel",
                    tooltip.isEmpty() ? null : tooltip.get(0).getString(), t);
        }
    }

    public static void renderHud(GuiGraphics graphics) {
        com.wynnchayuan.listener.ChatBlock.tick();
        boolean inPlace = WynnChaYuan.config().dialogueMode()
                        == CollectorConfig.DialogueMode.REPLACE
                || WynnChaYuan.config().choiceMode()
                        == CollectorConfig.DialogueMode.PANEL;
        if (!WynnChaYuan.config().showOverlays() && !inPlace) {
            return;
        }
        try {
            DialogueOverlay.render(graphics);
            if (!WynnChaYuan.config().showOverlays()) {
                return;
            }
            TrackerOverlay.render(graphics);
            LookAtTranslator.render(graphics);
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("render.dialogueError");
        }
    }
}
