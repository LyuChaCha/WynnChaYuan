package com.wynnchayuan.client;

import com.wynnchayuan.ModChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class ChatButtons {

    private ChatButtons() {}

    private static final Component GAP = Component.literal("  ");

    public static MutableComponent button(Component label) {
        return Component.empty().append("[ ").append(label).append(" ]");
    }

    public static List<Component> rows(Minecraft client, List<Component> buttons) {
        int width = Mth.floor(ChatComponent.getWidth(client.options.chatWidth().get())
                / client.options.chatScale().get()) - client.font.width(ModChat.INDENT);
        return rows(buttons, width, part -> client.font.width(part));
    }

    static List<Component> rows(List<Component> buttons, int width,
                                ToIntFunction<Component> measure) {
        List<Component> out = new ArrayList<>();
        MutableComponent row = null;
        int used = 0;
        int gap = measure.applyAsInt(GAP);
        for (Component button : buttons) {
            int size = measure.applyAsInt(button);
            if (row != null && used + gap + size > width) {
                out.add(row);
                row = null;
            }
            if (row == null) {
                row = Component.empty().append(button);
                used = size;
            } else {
                row.append(GAP).append(button);
                used += gap + size;
            }
        }
        if (row != null) {
            out.add(row);
        }
        return out;
    }
}
