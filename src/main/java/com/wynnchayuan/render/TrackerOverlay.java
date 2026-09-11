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
    public static void setCurrent(String name, StyledText task,
                                  TranslationStore store) {
        setCurrent(null, name, task, store);
    }

    /**
     * @param type Wynntils 說這是哪一種活動；不知道時傳 {@code null}
     */
    public static void setCurrent(Object type, String name, StyledText task,
                                  TranslationStore store) {
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

    /**
     * Wynntils 現在還在追蹤東西嗎。
     *
     * <h2>為什麼要在繪製當下問，而不是等事件</h2>
     * 這塊疊層原本純粹由 {@code ActivityTrackerUpdatedEvent} 推動，追蹤欄空掉時
     * 靠「事件補送一則空的」來收掉。問題是<b>任務完成的那一刻，那則空事件不一定會來</b>
     * ——Wynncraft 直接把追蹤欄收掉，Wynntils 就不再發事件了。
     *
     * <p>結果是原文那一欄已經消失，我們的譯文還孤零零地留在畫面上，而且停在一個
     * <b>已經完成</b>的任務上，看起來像沒做完。使用者回報的正是這個畫面。
     *
     * <p>所以改成在畫之前直接問 Wynntils 的權威狀態：它沒在追蹤，我們就不畫。
     * 這比任何「多久沒更新就清掉」的計時器都可靠——追蹤內容本來就可能好幾分鐘不變，
     * 計時器會把正常顯示中的譯文誤清掉。
     *
     * <p>拿不到狀態（Wynntils 還沒初始化、之後 API 有變動）時回傳 {@code true}，
     * 維持原本的行為：寧可多顯示一下，也不要讓正常的譯文憑空消失。
     */
    private static boolean stillTracking() {
        try {
            String name = com.wynntils.core.components.Models.Activity.getTrackedName();
            return name != null && !name.isBlank();
        } catch (Throwable t) {
            return true;
        }
    }

    public static void render(GuiGraphics graphics) {
        List<Component> lines = current;
        if (lines.isEmpty()) {
            return;
        }
        if (!stillTracking()) {
            current = List.of();      // 原文那一欄沒了，譯文也跟著收
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int lineHeight = mc.font.lineHeight + 1;
        int x = LEFT_MARGIN;
        int y = TOP_OFFSET;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.TRACKER)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.TRACKER);
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.TRACKER);
        }
        // 第一行是放大的抬頭，量寬度要照放大後的算，所以換行寬度也要先縮回去。
        int wrapAt = wrapWidth(x, graphics.guiWidth());
        List<net.minecraft.util.FormattedCharSequence> wrapped =
                new ArrayList<>();
        int firstRows = 0;
        for (int i = 0; i < lines.size(); i++) {
            int room = i == 0 ? Math.round(wrapAt / NAME_SCALE) : wrapAt;
            List<net.minecraft.util.FormattedCharSequence> parts =
                    mc.font.split(lines.get(i), room);
            if (parts.isEmpty()) {
                parts = List.of(lines.get(i).getVisualOrderText());
            }
            if (i == 0) {
                firstRows = parts.size();
            }
            wrapped.addAll(parts);
        }
        // 第一行是任務名，其餘是目標。原文那一欄也是這樣分主次的，
        // 全部畫成同一個大小的話，一眼看不出「現在在做哪個任務」。
        int nameHeight = Math.round(lineHeight * NAME_SCALE);
        int boxH = firstRows * nameHeight
                + (wrapped.size() - firstRows) * lineHeight + PADDING * 2;
        int boxW = 0;
        for (int i = 0; i < wrapped.size(); i++) {
            int w = mc.font.width(wrapped.get(i));
            boxW = Math.max(boxW, i < firstRows ? Math.round(w * NAME_SCALE) : w);
        }
        boxW += PADDING * 2;

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

    /** 任務名比目標大多少。1.25 倍看得出主次，又不會壓過下面的目標。 */
    private static final float NAME_SCALE = 1.25f;

    /**
     * 任務名的顏色：青綠。
     *
     * <p>配合原文那一欄的色調。先前跟著強調色走，但強調色同時也是截圖邊框與
     * 面板外框的顏色，改一個就得三個一起變——任務名該有自己的顏色。
     */
    private static final int NAME_COLOR = 0xFF40E0C0;

    /**
     * 任務名前面的抬頭。
     *
     * <h2>為什麼不能寫死</h2>
     * 追蹤欄不只追任務——世界事件、洞穴、地城、Raid、祕密發現都走同一個欄位。
     * 先前抬頭寫死成「進行中的任務」，於是世界事件也被說成任務，
     * 而原文那一欄明明白白寫著 {@code World Event}。一眼就對不起來。
     *
     * <p>Wynntils 的事件本來就帶著類型（{@code ActivityType}），拿它就好，
     * 不必猜。認不出來的類型退回通用的「進行中」。
     */
    private static String heading(Object type) {
        String key = type == null ? null : String.valueOf(type);
        String suffix = key == null ? "default"
                : key.toLowerCase(java.util.Locale.ROOT);
        String text = com.wynnchayuan.client.T.s("tracker.heading." + suffix);
        if (text.startsWith("wynnchayuan.")) {
            text = com.wynnchayuan.client.T.s("tracker.heading.default");
        }
        return text + " - ";
    }

    /**
     * 一行最寬畫到哪裡。
     *
     * <h2>踩到什麼</h2>
     * 世界事件的說明是一整句話（「Though these pirates are long past their
     * prime…」）。原文那一欄由 Wynntils 自己換行，我們這一塊沒有——
     * 於是整句往右畫出去，直接衝出螢幕外。實機截圖裡右半句就這樣不見了。
     *
     * <p>所以量一次：從框的左緣到螢幕右緣，再留一點邊。上限是螢幕的一半，
     * 不然一句長話會橫跨整個畫面，比截掉還難讀。
     */
    private static int wrapWidth(int x, int screenWidth) {
        int room = screenWidth - x - LEFT_MARGIN - PADDING * 2;
        return Math.max(MIN_WRAP, Math.min(room, screenWidth / 2));
    }

    /** 再窄就一行擠不下幾個字了，寧可讓它超出去。 */
    private static final int MIN_WRAP = 80;
}
