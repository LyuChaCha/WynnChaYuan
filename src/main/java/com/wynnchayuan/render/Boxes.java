package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 所有小框共用的外觀。
 *
 * <p>集中在一處，改主題色時三個小框（對話、追蹤、名牌）會一起跟著變，
 * 不必記得三個地方都要改。
 */
public final class Boxes {

    private Boxes() {}

    public static void draw(GuiGraphics g, int x, int y, int w, int h) {
        int bg = WynnChaYuan.config().backgroundARGB();
        int border = WynnChaYuan.config().accentARGB();
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    /**
     * 把一段譯文整理成可以放進小框的幾行。
     *
     * <h2>為什麼要丟掉排版字元</h2>
     * 遊戲原本的文字裡夾著大量 {@code minecraft:space} 的對齊偏移，那是為了
     * <b>原本的版面</b>算好的——3D 名牌置中、tooltip 的欄位對齊。
     *
     * <p>但這裡是我們自己畫的框，版面完全不同。把偏移原樣搬過來會把文字推到框外，
     * 看起來就是「框有出現但裡面是空的」。
     *
     * <p>tooltip 面板則相反：它在鏡像原本的版面，那邊必須保留偏移。
     * 兩種情境不能共用同一套處理。
     *
     * <p>換行也在這裡拆開——{@code drawString} 不會自己斷行，
     * 留著只會讓好幾行擠成一團。
     */
    public static List<Component> toLines(Component source) {
        if (source == null) {
            return List.of();
        }
        List<Style> styles = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        source.visit((style, text) -> {
            styles.add(style);
            texts.add(text);
            return Optional.empty();
        }, Style.EMPTY);

        List<Component> lines = new ArrayList<>();
        MutableComponent current = Component.empty();
        boolean hasContent = false;

        for (int i = 0; i < texts.size(); i++) {
            Style style = styles.get(i);
            String text = texts.get(i);
            if (text.isEmpty() || SpaceOffset.isSpaceFont(style)) {
                continue;                      // 排版偏移，這個框不需要
            }
            String[] chunks = text.split("\n", -1);
            for (int c = 0; c < chunks.length; c++) {
                if (c > 0) {
                    if (hasContent) {
                        lines.add(current);
                    }
                    current = Component.empty();
                    hasContent = false;
                }
                if (!chunks[c].isEmpty()) {
                    current.append(Component.literal(chunks[c]).withStyle(style));
                    if (!chunks[c].isBlank()) {
                        hasContent = true;
                    }
                }
            }
        }
        if (hasContent) {
            lines.add(current);
        }
        return lines;
    }
}
