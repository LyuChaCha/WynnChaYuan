package com.wynnchayuan.translate;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.List;

public final class NoteColourProbe {

    private static Component line() {
        MutableComponent out = Component.empty();
        out.append(colour("", 0xFF5555));
        out.append(colour(" Total Damage: ", 0xAAAAAA));
        out.append(colour("300%", 0xFFFFFF));
        out.append(colour(" (of your DPS, per Wisp)", 0x555555));
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
