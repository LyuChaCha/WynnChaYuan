package com.wynnchayuan.translate;

import com.wynnchayuan.ModNetwork;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class TranslationUpdate {

    private TranslationUpdate() {}

    private static volatile String pending;

    private static volatile boolean told;

    public static void found(String version) {
        pending = version;
    }

    public static boolean available() {
        return pending != null;
    }

    public static void checked(String remote) {
        seen = remote;
        asked = remote != null;
        adoptDate(remote);
    }

    private static void adoptDate(String remote) {
        if (remote == null || remote.isBlank()) {
            return;
        }
        com.wynnchayuan.CollectorConfig config = WynnChaYuan.config();
        if (!config.syncedTranslationsDate().isBlank()
                || !remote.equals(config.syncedTranslations())) {
            return;
        }
        String date = RemoteSync.lastRemoteDate();
        if (date != null && !date.isBlank()) {
            config.syncedTranslationsDate(date);
        }
    }

    public static String label(String sha, String date, String bundled) {
        if (sha == null || sha.isBlank()) {
            return bundled;
        }
        if (date != null && !date.isBlank()) {
            return date;
        }
        return sha.length() > 7 ? sha.substring(0, 7) : sha;
    }

    public static boolean asked() {
        return asked;
    }

    public static String seen() {
        return seen;
    }

    private static volatile boolean asked;

    private static volatile String seen;

    public enum State {
        LOCAL,
        CHECKING,
        LATEST,
        BEHIND,
        UNKNOWN
    }

    public static State verdict(boolean github, boolean checking, boolean asked,
                                String local, String remote) {
        if (!github) {
            return State.LOCAL;
        }
        if (checking) {
            return State.CHECKING;
        }
        if (!asked || remote == null || remote.isBlank()) {
            return State.UNKNOWN;
        }
        if (local == null || local.isBlank()) {
            return State.UNKNOWN;
        }
        return remote.equals(local) ? State.LATEST : State.BEHIND;
    }

    public static void done(String version) {
        pending = null;
        told = true;
        if (version != null && !version.isBlank()) {
            WynnChaYuan.config().syncedTranslations(version);
            String date = RemoteSync.lastRemoteDate();
            WynnChaYuan.config().syncedTranslationsDate(date == null ? "" : date);
            checked(version);
        }
    }

    public static final String COMMAND = "wynnchayuan-update";

    private static volatile boolean fetching;

    public static void registerCommand() {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registry) -> dispatcher.register(
                        net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                .literal(COMMAND)
                                .executes(ctx -> {
                                    update(Minecraft.getInstance());
                                    return 1;
                                })));
    }

    private static void update(Minecraft client) {
        if (client == null || client.player == null || fetching) {
            return;
        }
        fetching = true;
        tell(client, com.wynnchayuan.client.T.c("status.fetching")
                .withStyle(ChatFormatting.GRAY));
        ModNetwork.resyncTranslations(result -> {
            fetching = false;
            if (client.player != null) {
                tell(client, com.wynnchayuan.client.T.c("chat.translations.done", result)
                        .withStyle(ChatFormatting.GREEN));
            }
        });
    }

    public static void tellOnce(Minecraft client) {
        String version = pending;
        if (told || version == null || client == null || client.player == null) {
            return;
        }
        told = true;
        tell(client, com.wynnchayuan.client.T.c("chat.translations.line1")
                .withStyle(ChatFormatting.AQUA));
        tell(client, button());
        tell(client, com.wynnchayuan.client.T.c("chat.translations.where")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    static Component button() {
        return com.wynnchayuan.client.T.c("chat.translations.button")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand(
                                "/" + COMMAND))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                                com.wynnchayuan.client.T.c("chat.translations.button.hover"))));
    }

    private static void tell(Minecraft client, Component line) {
        com.wynnchayuan.capture.OwnOutputs.note(line);
        client.player.displayClientMessage(line, false);
    }
}
