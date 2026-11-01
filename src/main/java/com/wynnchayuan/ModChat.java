package com.wynnchayuan;

import com.wynnchayuan.capture.OwnOutputs;
import com.wynnchayuan.client.ChatPill;
import com.wynnchayuan.listener.ChatBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ModChat {

    public static final String INDENT = "  ";

    private ModChat() {}

    public static void say(Minecraft client, Component line) {
        if (client != null) {
            send(client, ChatPill.prefixed(client.font, line), line);
        }
    }

    public static void more(Minecraft client, Component line) {
        send(client, line, line);
    }

    public static void panel(Minecraft client, Component head, List<Component> rows) {
        more(client, Component.empty());
        say(client, head);
        for (Component row : rows) {
            more(client, Component.literal(INDENT).append(row));
        }
        more(client, Component.empty());
    }

    private static void send(Minecraft client, Component shown, Component bare) {
        if (client == null || client.player == null) {
            return;
        }
        OwnOutputs.note(bare);
        ChatBlock.sendOurs(shown, () -> client.player.displayClientMessage(shown, false));
    }
}
