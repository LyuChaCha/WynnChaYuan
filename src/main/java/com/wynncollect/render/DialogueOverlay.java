package com.wynncollect.render;

import com.wynncollect.translate.LineTranslator;
import com.wynncollect.translate.TranslationStore;
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
    private static final int BACKGROUND = 0xC0100010;   // 與原版 tooltip 同色系
    private static final int BORDER = 0xFF3A1E5C;

    private static volatile List<Component> current = List.of();

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
                lines.add(translated);
            } else {
                lines.add(LineTranslator.untranslated(line));
            }
        }
        current = any ? List.copyOf(lines) : List.of();
    }

    public static void clear() {
        current = List.of();
    }

    /** 每幀呼叫。沒有內容時什麼都不畫。 */
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

        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - BOTTOM_MARGIN - boxH;

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
