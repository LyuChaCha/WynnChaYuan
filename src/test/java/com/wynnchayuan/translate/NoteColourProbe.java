package com.wynnchayuan.translate;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.List;

/**
 * 括號註解在譯文裡有沒有拿到它自己的顏色。
 *
 * <h2>回報</h2>
 * 「上面 {@code ()} 裡面原為是灰色，但實際上是白色」。
 *
 * <p>原文那一行是三種顏色：標籤一種、數值一種、括號註解暗灰。
 * 譯文出來之後括號那一段變成正文的亮色。
 *
 * <p>這支把那一行照原文的樣子組回去（三段各自的顏色），送進翻譯，
 * 再把每一段的顏色印出來——看得見顏色，才知道是哪一段沒分開。
 */
public final class NoteColourProbe {

    /** 原文那一行：標籤灰、數值白、括號註解暗灰。 */
    private static Component line() {
        MutableComponent out = Component.empty();
        out.append(colour("", 0xFF5555));          // ♥ 的位置，紅
        out.append(colour(" Total Damage: ", 0xAAAAAA)); // 標籤，灰
        out.append(colour("300%", 0xFFFFFF));            // 數值，白
        out.append(colour(" (of your DPS, per Wisp)", 0x555555));  // 註解，暗灰
        return out;
    }

    private static Component colour(String text, int rgb) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    public static void main(String[] args) {
        for (String lang : new String[] {"zh_tw", "zh_cn"}) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of(
                    "src/main/resources/assets/wynnchayuan/translations", lang));
            System.out.println("=== " + lang);
            show("原文", line());
            List<Component> out = com.wynnchayuan.render.TooltipPanel
                    .translateLines(List.of(line()), store);
            for (Component c : out) {
                show("譯文", c);
            }
        }
    }

    private static void show(String what, Component c) {
        StringBuilder sb = new StringBuilder("  " + what + "：");
        c.visit((style, text) -> {
            TextColor colour = style.getColor();
            sb.append("[")
              .append(colour == null ? "（無）" : colour.serialize())
              .append(" ")
              .append(text.replace("", "♥"))
              .append("]");
            return java.util.Optional.empty();
        }, Style.EMPTY);
        System.out.println(sb);
    }

    private NoteColourProbe() {}
}
