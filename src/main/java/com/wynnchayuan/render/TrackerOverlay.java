package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 追蹤任務的譯文小框。
 *
 * <p>畫在畫面左側、Wynntils 自己的追蹤器<b>下方</b>，兩份並存——
 * 原本的追蹤器不動，譯文另外一塊。
 *
 * <p>內容由 {@link com.wynnchayuan.listener.TrackerListener} 在
 * {@code ActivityTrackerUpdatedEvent} 觸發時餵進來，不是每幀重算。
 */
public final class TrackerOverlay {

    private static final int LEFT_MARGIN = 6;
    private static final int TOP_OFFSET = 90;   // 避開 Wynntils 自己的追蹤器
    private static final int PADDING = 3;
    private static final int BACKGROUND = 0xB0100010;
    private static final int BORDER = 0xFF3A1E5C;

    private static volatile List<Component> current = List.of();

    private TrackerOverlay() {}

    /** 追蹤內容變了就更新；名稱與目標都翻不到時整塊不顯示。 */
    public static void setCurrent(String name, StyledText task, TranslationStore store) {
        List<Component> lines = new ArrayList<>();
        boolean any = false;

        if (name != null && !name.isBlank()) {
            StyledText styled = StyledText.fromString(name);
            Component translated = LineTranslator.translate(styled, store);
            if (translated != null) {
                any = true;
                lines.add(translated);
            } else {
                lines.add(LineTranslator.untranslated(styled));
            }
        }
        if (task != null) {
            Component translated = LineTranslator.translate(task, store);
            if (translated != null) {
                any = true;
                lines.add(translated);
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

    public static void render(GuiGraphics graphics) {
        List<Component> lines = current;
        if (lines.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int lineHeight = mc.font.lineHeight + 1;
        int boxH = lines.size() * lineHeight + PADDING * 2;
        int boxW = 0;
        for (Component line : lines) {
            boxW = Math.max(boxW, mc.font.width(line));
        }
        boxW += PADDING * 2;

        int x = LEFT_MARGIN;
        int y = TOP_OFFSET;

        graphics.fill(x, y, x + boxW, y + boxH, BACKGROUND);
        graphics.fill(x, y, x + boxW, y + 1, BORDER);
        graphics.fill(x, y + boxH - 1, x + boxW, y + boxH, BORDER);
        graphics.fill(x, y, x + 1, y + boxH, BORDER);
        graphics.fill(x + boxW - 1, y, x + boxW, y + boxH, BORDER);

        int textY = y + PADDING;
        for (Component line : lines) {
            graphics.drawString(mc.font, line, x + PADDING, textY, 0xFFFFFF);
            textY += lineHeight;
        }
    }
}
