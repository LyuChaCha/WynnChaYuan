package com.wynnchayuan;

import com.wynnchayuan.capture.OwnOutputs;
import com.wynnchayuan.client.T;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class ModCommands {

    private ModCommands() {}

    static volatile boolean openSettingsNextTick = false;

    static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                    for (String name : new String[] {"wynnchayuan", "wcy"}) {
                        dispatcher.register(
                                ClientCommandManager.literal(name).executes(context -> {
                                            openSettingsNextTick = true;
                                            return 1;
                                        })
                                        .then(ClientCommandManager.literal("update")
                                                .executes(context -> {
                                                    SelfUpdate.startFromChat(
                                                            Minecraft.getInstance());
                                                    return 1;
                                                }))
                                        .then(ClientCommandManager.literal("link")
                                                .executes(context -> {
                                                    say("chat.link.usage");
                                                    return 1;
                                                })
                                                .then(ClientCommandManager
                                                        .argument("code", com.mojang.brigadier.arguments
                                                                .StringArgumentType.word())
                                                        .executes(context -> {
                                                            linkAccount(com.mojang.brigadier.arguments
                                                                    .StringArgumentType.getString(context, "code"));
                                                            return 1;
                                                        }))));
                    }
                });
    }

    private static void linkAccount(String code) {
        say("chat.link.working");
        PresenceAccount.link(code, result -> Minecraft.getInstance().execute(() -> say(switch (result) {
            case DONE -> "chat.link.done";
            case BAD_CODE -> "chat.link.bad";
            case UNKNOWN_CODE -> "chat.link.unknown";
            case LOGIN_FAILED -> "chat.link.login";
            case FAILED -> "chat.link.failed";
            case OFF -> "chat.link.off";
        })));
    }

    private static void say(String key) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        Component line = T.c(key);
        OwnOutputs.note(line);
        client.player.displayClientMessage(line, false);
    }
}
