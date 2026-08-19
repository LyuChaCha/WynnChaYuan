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
 * 任務對話的翻譯小框。
 *
 * <p>畫在畫面下緣、原對話文字的<b>上方</b>，兩邊都看得到——原文不動，
 * 譯文另外一塊，不影響原本的閱讀。
 *
 * <p>目前的內容由 {@link #setCurrent} 從對話事件餵進來，
 * 因為對話文字是走 action bar 的，不像 tooltip 那樣每幀都拿得到。
 */
public final class DialogueOverlay {

    /** 距畫面底部的高度，避開原本的對話文字與快捷列。 */
    private static final int BOTTOM_MARGIN = 70;

    private static final int PADDING = 4;

    private static volatile List<Component> current = List.of();

    /**
     * 最後一次收到對話內容的時間。
     *
     * <p>沒有這個的話框會永遠留在畫面上——對話事件只在對話進行中發，
     * 走開之後就再也沒有東西來清它。改用「多久沒更新就隱藏」，
     * 不必依賴一個其實不可靠的「對話結束」事件。
     */
    private static volatile long lastUpdate = 0;

    private DialogueOverlay() {}

    /** 對話內容變了就更新這裡；傳 null 或空的代表對話結束。 */
    public static void setCurrent(StyledText dialogue, TranslationStore store) {
        if (dialogue == null) {
            current = List.of();
            return;
        }
        List<Component> lines = new ArrayList<>();
        boolean any = false;
        for (String raw : dialogue.getString().split("\n")) {
            StyledText line = StyledText.fromString(raw);
            Component translated = LineTranslator.translate(line, store);
            if (translated != null) {
                any = true;
                lines.addAll(Boxes.toLines(translated));
            } else {
                lines.addAll(Boxes.toLines(LineTranslator.untranslated(line)));
            }
        }
        // 診斷：分得出「沒讀到對話」與「讀到了但沒譯文」——
        // 少了這個，畫面上沒東西時完全不知道卡在哪一步
        WynnChaYuan.store().noteEvent(any ? "dialogue.shown" : "dialogue.noMatch");
        current = any ? List.copyOf(lines) : List.of();
        if (any) {
            lastUpdate = System.currentTimeMillis();
        }
    }

    public static void clear() {
        current = List.of();
        lastUpdate = 0;
    }

    /** 每幀呼叫。沒有內容時什麼都不畫。 */
    public static void render(GuiGraphics graphics) {
        List<Component> lines = current;
        if (lines.isEmpty()) {
            return;
        }
        int hold = WynnChaYuan.config().dialogueHoldMs();
        if (hold != Integer.MAX_VALUE && System.currentTimeMillis() - lastUpdate > hold) {
            current = List.of();               // 停留夠久就收起來
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

        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - BOTTOM_MARGIN - boxH;

        Boxes.draw(graphics, x, y, boxW, boxH);

        int textY = y + PADDING;
        for (Component line : lines) {
            graphics.drawString(mc.font, line, x + PADDING, textY, Colors.TEXT);
            textY += lineHeight;
        }
    }
}
