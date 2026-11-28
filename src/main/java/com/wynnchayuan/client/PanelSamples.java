package com.wynnchayuan.client;

import com.wynnchayuan.ModConfig.Overlay;
import com.wynnchayuan.render.PanelPreview;
import com.wynnchayuan.render.PanelPreview.Sample;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PanelSamples {

    private PanelSamples() {}

    static Map<Overlay, Sample> all() {
        Map<Overlay, Sample> out = new EnumMap<>(Overlay.class);
        out.put(Overlay.TOOLTIP, Sample.lines(List.of(
                line("pos.demo.bow", ChatFormatting.DARK_PURPLE),
                line("pos.demo.dps", ChatFormatting.GRAY),
                line("pos.demo.level", ChatFormatting.GRAY),
                Component.empty(),
                line("pos.demo.steal", ChatFormatting.GREEN))));
        out.put(Overlay.TRACKER, Sample.lines(List.of(
                Component.literal(PanelPreview.trackerHeading("QUEST")).append(T.s("pos.demo.quest")),
                T.c("pos.demo.goto"),
                T.c("pos.demo.slay"))));
        out.put(Overlay.DIALOGUE, Sample.lines(List.of(
                PanelPreview.speaker(T.s("pos.demo.cook")),
                T.c("pos.demo.line"))));
        out.put(Overlay.CHOICES, Sample.options(List.of(
                List.of(T.c("pos.demo.choice1")),
                List.of(T.c("pos.demo.choice2")),
                List.of(T.c("pos.demo.choice3"))), 1));
        out.put(Overlay.NAMETAG, Sample.lines(List.of(line("pos.demo.cook", ChatFormatting.GREEN))));
        return out;
    }

    static Sample original() {
        return Sample.lines(List.of(
                Component.literal("Mythic Bow").withStyle(ChatFormatting.DARK_PURPLE),
                Component.literal("DPS 897").withStyle(ChatFormatting.GRAY),
                Component.literal("Combat Lv. Min: 114").withStyle(ChatFormatting.GRAY),
                Component.empty(),
                Component.literal("+978/3s Life Steal").withStyle(ChatFormatting.GREEN)));
    }

    private static Component line(String key, ChatFormatting colour) {
        return Component.literal(T.s(key)).withStyle(colour);
    }
}
