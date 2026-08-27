package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
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
            // 掛上「進行中的任務」這個抬頭。
            //
            // 光一個任務名擺在畫面角落，看起來像隨便一段文字；原文那一欄是靠
            // Wynntils 自己的框線與位置在說明「這是任務追蹤」，我們的面板沒有
            // 那些線索，所以把身分直接寫出來。
            Component shown = translated != null
                    ? translated : LineTranslator.untranslated(styled);
            if (translated != null) {
                any = true;
            }
            lines.add(Component.literal(HEADING).append(shown));
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

    public static void render(GuiGraphics graphics) {
        List<Component> lines = current;
        if (lines.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int lineHeight = mc.font.lineHeight + 1;
        // 第一行是任務名，其餘是目標。原文那一欄也是這樣分主次的，
        // 全部畫成同一個大小的話，一眼看不出「現在在做哪個任務」。
        int nameHeight = Math.round(lineHeight * NAME_SCALE);
        int boxH = nameHeight + (lines.size() - 1) * lineHeight + PADDING * 2;
        int boxW = Math.round(mc.font.width(lines.get(0)) * NAME_SCALE);
        for (int i = 1; i < lines.size(); i++) {
            boxW = Math.max(boxW, mc.font.width(lines.get(i)));
        }
        boxW += PADDING * 2;

        int x = LEFT_MARGIN;
        int y = TOP_OFFSET;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.TRACKER)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.TRACKER);
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.TRACKER);
        }

        Boxes.draw(graphics, x, y, boxW, boxH);

        int textY = y + PADDING;
        graphics.pose().pushMatrix();
        graphics.pose().scale(NAME_SCALE, NAME_SCALE);
        graphics.drawString(mc.font, lines.get(0),
                Math.round((x + PADDING) / NAME_SCALE),
                Math.round(textY / NAME_SCALE),
                NAME_COLOR);
        graphics.pose().popMatrix();
        textY += nameHeight;
        for (int i = 1; i < lines.size(); i++) {
            graphics.drawString(mc.font, lines.get(i), x + PADDING, textY, Colors.TEXT);
            textY += lineHeight;
        }
    }

    /** 任務名比目標大多少。1.25 倍看得出主次，又不會壓過下面的目標。 */
    private static final float NAME_SCALE = 1.25f;

    /**
     * 任務名的顏色：青綠。
     *
     * <p>配合原文那一欄的色調。先前跟著強調色走，但強調色同時也是截圖邊框與
     * 面板外框的顏色，改一個就得三個一起變——任務名該有自己的顏色。
     */
    private static final int NAME_COLOR = 0xFF40E0C0;

    /** 任務名前面的抬頭。 */
    private static final String HEADING = "進行中的任務 - ";
}
