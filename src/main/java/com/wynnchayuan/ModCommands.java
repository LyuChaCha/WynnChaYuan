package com.wynnchayuan;

import net.minecraft.network.chat.Component;

final class ModCommands {

    private ModCommands() {}

    static volatile boolean openSettingsNextTick = false;

    static void registerCommand() {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess) -> {
                    for (String name : new String[] {"wynnchayuan", "wcy"}) {
                        dispatcher.register(
                                net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                        .literal(name)
                                        .executes(context -> {
                                            openSettingsNextTick = true;
                                            return 1;
                                        })
                                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                .literal("update")
                                                .executes(context -> {
                                                    SelfUpdate.startFromChat(
                                                            net.minecraft.client.Minecraft.getInstance());
                                                    return 1;
                                                }))
                                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                .literal("link")
                                                .executes(context -> {
                                                    say("chat.link.usage");
                                                    return 1;
                                                })
                                                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
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
        Presence.link(code, result -> net.minecraft.client.Minecraft.getInstance().execute(() -> say(switch (result) {
            case DONE -> "chat.link.done";
            case BAD_CODE -> "chat.link.bad";
            case UNKNOWN_CODE -> "chat.link.unknown";
            case LOGIN_FAILED -> "chat.link.login";
            case FAILED -> "chat.link.failed";
            case OFF -> "chat.link.off";
        })));
    }

    private static void say(String key) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        net.minecraft.network.chat.Component line = com.wynnchayuan.client.T.c(key);
        com.wynnchayuan.capture.OwnOutputs.note(line);
        client.player.displayClientMessage(line, false);
    }
}
