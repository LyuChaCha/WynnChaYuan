package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.T;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TrackerOverlay {

    private static final int LEFT_MARGIN = 6;
    private static final int TOP_OFFSET = 90;
    private static final int PADDING = 3;
    private static final int BACKGROUND = 0xB0100010;
    private static final int BORDER = 0xFF3A1E5C;

    private static volatile List<Component> current = List.of();

    private TrackerOverlay() {}

    public static void setCurrent(String name, StyledText task,
                                  TranslationStore store) {
        setCurrent(null, name, task, store);
    }

    public static void setCurrent(Object type, String name, StyledText task,
                                  TranslationStore store) {
        List<Component> lines = new ArrayList<>();
        boolean any = false;

        if (name != null && !name.isBlank()) {
            StyledText styled = StyledText.fromString(name);
            Component translated = LineTranslator.translate(styled, store);
            Component shown = translated != null
                    ? translated : LineTranslator.untranslated(styled);
            if (translated != null) {
                any = true;
            }
            lines.add(Component.literal(heading(type)).append(shown));
        }
        if (task != null) {
            Component translated = LineTranslator.translate(task, store);
            if (translated != null) {
                any = true;
                lines.addAll(Boxes.toLines(translated));
            } else {
                lines.add(LineTranslator.untranslated(task));
            }
        }
        WynnChaYuan.store().noteEvent(any ? "tracker.shown" : "tracker.noMatch");
        current = any ? List.copyOf(lines) : List.of();
    }

    public static void clear() {
        current = List.of();
    }

    private static boolean stillTracking() {
        try {
            String name = Models.Activity.getTrackedName();
            return name != null && !name.isBlank();
        } catch (Throwable t) {
            ErrorDebug.note("TrackerOverlay#stillTracking", null, t);
            return true;
        }
    }

    private static volatile List<Component> extras = List.of();

    public static void setExtras(List<Component> lines) {
        extras = lines == null ? List.of() : List.copyOf(lines);
    }

    public static boolean showsExtras(CollectorConfig config) {
        return config != null && !config.wynntilsUi();
    }

    public static void render(GuiGraphics graphics) {
        List<Component> lines = current;
        boolean panel = WynnChaYuan.config().trackerMode()
                == CollectorConfig.DialogueMode.PANEL;
        if (!panel) {
            return;
        }
        List<Component> more = showsExtras(WynnChaYuan.config()) ? extras : List.of();
        if (lines.isEmpty() && more.isEmpty()) {
            return;
        }
        if (!stillTracking()) {
            current = List.of();
            lines = List.of();
            if (more.isEmpty()) {
                return;
            }
        }
        if (!more.isEmpty()) {
            List<Component> both = new ArrayList<>(lines.size() + more.size());
            both.addAll(lines);
            both.addAll(more);
            lines = both;
        }
        Minecraft mc = Minecraft.getInstance();
        int lineHeight = mc.font.lineHeight + 1;
        int x = LEFT_MARGIN;
        int y = TOP_OFFSET;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.TRACKER)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.TRACKER);
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.TRACKER);
        }
        CollectorConfig cfg = WynnChaYuan.config();
        boolean sized = cfg.hasOverlaySize(CollectorConfig.Overlay.TRACKER);
        int fixedW = sized ? cfg.overlayW(CollectorConfig.Overlay.TRACKER) : 0;
        int wrapAt = sized
                ? Math.max(MIN_WRAP, fixedW - PADDING * 2)
                : wrapWidth(x, graphics.guiWidth());
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        int firstRows = 0;
        for (int i = 0; i < lines.size(); i++) {
            int room = i == 0 ? Math.round(wrapAt / NAME_SCALE) : wrapAt;
            List<FormattedCharSequence> parts = mc.font.split(lines.get(i), room);
            if (parts.isEmpty()) {
                parts = List.of(lines.get(i).getVisualOrderText());
            }
            if (i == 0) {
                firstRows = parts.size();
            }
            wrapped.addAll(parts);
        }
        int nameHeight = Math.round(lineHeight * NAME_SCALE);
        int boxH = firstRows * nameHeight
                + (wrapped.size() - firstRows) * lineHeight + PADDING * 2;
        int boxW = 0;
        for (int i = 0; i < wrapped.size(); i++) {
            int w = mc.font.width(wrapped.get(i));
            boxW = Math.max(boxW, i < firstRows ? Math.round(w * NAME_SCALE) : w);
        }
        boxW += PADDING * 2;
        if (sized) {
            boxW = Math.max(boxW, fixedW);
            boxH = Math.max(boxH, cfg.overlayH(CollectorConfig.Overlay.TRACKER));
        }
        x = Math.max(2, Math.min(graphics.guiWidth() - boxW - 2, x));
        y = Math.max(2, Math.min(graphics.guiHeight() - boxH - 2, y));

        Boxes.draw(graphics, x, y, boxW, boxH);

        int textY = y + PADDING;
        for (int i = 0; i < firstRows; i++) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(NAME_SCALE, NAME_SCALE);
            graphics.drawString(mc.font, wrapped.get(i),
                    Math.round((x + PADDING) / NAME_SCALE),
                    Math.round(textY / NAME_SCALE),
                    NAME_COLOR);
            graphics.pose().popMatrix();
            textY += nameHeight;
        }
        for (int i = firstRows; i < wrapped.size(); i++) {
            graphics.drawString(mc.font, wrapped.get(i), x + PADDING, textY,
                    Colors.TEXT);
            textY += lineHeight;
        }
    }

    private static final float NAME_SCALE = 1.25f;

    private static final int NAME_COLOR = 0xFF40E0C0;

    private static String heading(Object type) {
        String key = type == null ? null : String.valueOf(type);
        String suffix = key == null ? "default"
                : key.toLowerCase(Locale.ROOT);
        String text = T.s("tracker.heading." + suffix);
        if (text.startsWith("wynnchayuan.")) {
            text = T.s("tracker.heading.default");
        }
        return text + " - ";
    }

    private static int wrapWidth(int x, int screenWidth) {
        int room = screenWidth - x - LEFT_MARGIN - PADDING * 2;
        return Math.max(MIN_WRAP, Math.min(room, screenWidth / 2));
    }

    private static final int MIN_WRAP = 80;
}
