package com.wynnchayuan;

import com.wynnchayuan.capture.OwnOutputs;
import com.wynnchayuan.client.ChatPill;
import com.wynnchayuan.listener.ChatBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ModChat {

    private ModChat() {}

    public static void say(Minecraft client, Component line) {
        if (client != null) {
            send(client, ChatPill.prefixed(client.font, line), line);
        }
    }

    public static void more(Minecraft client, Component line) {
        send(client, line, line);
    }

    private static void send(Minecraft client, Component shown, Component bare) {
        if (client == null || client.player == null) {
            return;
        }
        OwnOutputs.note(bare);
        ChatBlock.sendOurs(shown, () -> client.player.displayClientMessage(shown, false));
    }
}
