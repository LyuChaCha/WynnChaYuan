package com.wynnchayuan;

import com.mojang.blaze3d.platform.InputConstants;
import com.wynnchayuan.client.SettingsScreen;
import com.wynnchayuan.listener.RenderListener;
import com.wynnchayuan.translate.TranslationStore;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

final class ModKeys {

    private ModKeys() {}

    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID, "main"));

    static void registerKeyBind() {
        WynnChaYuan.openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.openSettings",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));
        WynnChaYuan.screenshotKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.screenshot",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));
        WynnChaYuan.copyChatKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.copyChat",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));
        com.wynnchayuan.render.PanelShot.bind(WynnChaYuan.screenshotKey);
        com.wynnchayuan.render.PanelShot.listen();

        if (com.wynnchayuan.translate.WynnventoryBridge.installed()) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                CollectorConfig c = WynnChaYuan.config();
                TranslationStore store = WynnChaYuan.translations();
                if (c == null || store == null) {
                    return;
                }
                com.wynnchayuan.translate.WynnventoryBridge.tick(c.wynnventory(),
                        c.shiftPeekNames() && com.wynnchayuan.listener.RenderListener.shiftHeld(),
                        store.generation());
            });
        }

        ClientTickEvents.END_CLIENT_TICK.register(com.wynnchayuan.render.FpsLog::tick);

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> com.wynnchayuan.client.FirstRunScreen.clientTick());
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> com.wynnchayuan.client.NoticeScreen.clientTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (WynnChaYuan.openSettingsKey.consumeClick()) {
                client.setScreen(new SettingsScreen());
            }
            if (tellKeysReleased && client.player != null) {
                tellKeysReleased = false;
                Component line = com.wynnchayuan.client.T.c("chat.keys.released")
                        .withStyle(net.minecraft.ChatFormatting.AQUA);
                com.wynnchayuan.capture.OwnOutputs.note(line);
                client.player.displayClientMessage(line, false);
            }
            if (ModCommands.openSettingsNextTick) {
                ModCommands.openSettingsNextTick = false;
                client.setScreen(new SettingsScreen());
            }
            while (WynnChaYuan.copyChatKey.consumeClick()) {
                client.setScreen(new com.wynnchayuan.client.ChatCopyScreen());
            }
            while (WynnChaYuan.screenshotKey.consumeClick()) {
                com.wynnchayuan.render.PanelShot.request();
            }
            com.wynnchayuan.render.PanelShot.tick();
            Releases.tellOnce(client);
            if (client.player != null && client.getCurrentServer() != null
                    && client.getCurrentServer().ip.toLowerCase(java.util.Locale.ROOT)
                            .contains("wynncraft")) {
                Presence.tick(true, WynnChaYuan.config.ownCape(),
                        com.wynnchayuan.render.Badges.ownRainbow(WynnChaYuan.config),
                        com.wynnchayuan.render.Badges.ownShown(WynnChaYuan.config));
            }
            SelfUpdate.tellLeftover(client);
            SelfUpdate.announceProgress(client);
            com.wynnchayuan.translate.TranslationUpdate.tellOnce(client);
        });
    }

    private static volatile boolean tellKeysReleased = false;

    static void releaseOldDefaultKeys(net.minecraft.client.Minecraft client) {
        try {
            if (WynnChaYuan.config == null || WynnChaYuan.config.oldKeysReleased() || WynnChaYuan.openSettingsKey == null
                    || WynnChaYuan.screenshotKey == null) {
                return;
            }
            boolean cleared = unbindIf(WynnChaYuan.openSettingsKey, org.lwjgl.glfw.GLFW.GLFW_KEY_F6);
            cleared |= unbindIf(WynnChaYuan.screenshotKey, org.lwjgl.glfw.GLFW.GLFW_KEY_F9);
            cleared |= unbindIf(WynnChaYuan.screenshotKey, org.lwjgl.glfw.GLFW.GLFW_KEY_F8);
            if (cleared) {
                KeyMapping.resetMapping();
                client.options.save();
                tellKeysReleased = true;
            }
            WynnChaYuan.config.markOldKeysReleased();
        } catch (Throwable t) {
            ModLog.info("清舊的預設鍵時出錯，按鍵維持原樣：" + t);
        }
    }

    private static boolean unbindIf(KeyMapping mapping, int key) {
        InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(mapping);
        if (bound == null || bound.getType() != InputConstants.Type.KEYSYM
                || bound.getValue() != key) {
            return false;
        }
        mapping.setKey(InputConstants.UNKNOWN);
        return true;
    }
}
