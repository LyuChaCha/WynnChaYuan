package com.wynnchayuan;

import com.mojang.blaze3d.platform.InputConstants;
import com.wynnchayuan.capture.OwnOutputs;
import com.wynnchayuan.client.ChatCopyScreen;
import com.wynnchayuan.client.FirstRunScreen;
import com.wynnchayuan.client.NoticeScreen;
import com.wynnchayuan.client.SettingsScreen;
import com.wynnchayuan.client.T;
import com.wynnchayuan.listener.RenderListener;
import com.wynnchayuan.render.Badges;
import com.wynnchayuan.render.FpsLog;
import com.wynnchayuan.render.PanelShot;
import com.wynnchayuan.translate.TranslationStore;
import com.wynnchayuan.translate.TranslationUpdate;
import com.wynnchayuan.translate.WynnventoryBridge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

final class ModKeys {

    private ModKeys() {}

    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID, "main"));

    static void registerKeyBind() {
        WynnChaYuan.openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.openSettings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY));
        WynnChaYuan.screenshotKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.screenshot",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY));
        WynnChaYuan.copyChatKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.copyChat",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY));
        PanelShot.bind(WynnChaYuan.screenshotKey);
        PanelShot.listen();

        if (WynnventoryBridge.installed()) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                ModConfig c = WynnChaYuan.config();
                TranslationStore store = WynnChaYuan.translations();
                if (c == null || store == null) {
                    return;
                }
                WynnventoryBridge.tick(c.wynnventory(),
                        c.shiftPeekNames() && RenderListener.shiftHeld(), store.generation());
            });
        }

        ClientTickEvents.END_CLIENT_TICK.register(FpsLog::tick);

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> FirstRunScreen.clientTick());
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> NoticeScreen.clientTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (WynnChaYuan.openSettingsKey.consumeClick()) {
                client.setScreen(new SettingsScreen());
            }
            if (tellKeysReleased && client.player != null) {
                tellKeysReleased = false;
                Component line = T.c("chat.keys.released").withStyle(ChatFormatting.AQUA);
                OwnOutputs.note(line);
                client.player.displayClientMessage(line, false);
            }
            if (ModCommands.openSettingsNextTick) {
                ModCommands.openSettingsNextTick = false;
                client.setScreen(new SettingsScreen());
            }
            while (WynnChaYuan.copyChatKey.consumeClick()) {
                client.setScreen(new ChatCopyScreen());
            }
            while (WynnChaYuan.screenshotKey.consumeClick()) {
                PanelShot.request();
            }
            PanelShot.tick();
            Releases.tellOnce(client);
            if (client.player != null && client.getCurrentServer() != null
                    && client.getCurrentServer().ip.toLowerCase(Locale.ROOT)
                            .contains("wynncraft")) {
                Presence.tick(true, WynnChaYuan.config.ownCape(),
                        Badges.ownRainbow(WynnChaYuan.config), Badges.ownShown(WynnChaYuan.config));
            }
            SelfUpdate.tellLeftover(client);
            SelfUpdate.announceProgress(client);
            TranslationUpdate.tellOnce(client);
        });
    }

    private static volatile boolean tellKeysReleased = false;

    static void releaseOldDefaultKeys(Minecraft client) {
        try {
            if (WynnChaYuan.config == null || WynnChaYuan.config.oldKeysReleased() || WynnChaYuan.openSettingsKey == null
                    || WynnChaYuan.screenshotKey == null) {
                return;
            }
            boolean cleared = unbindIf(WynnChaYuan.openSettingsKey, GLFW.GLFW_KEY_F6);
            cleared |= unbindIf(WynnChaYuan.screenshotKey, GLFW.GLFW_KEY_F9);
            cleared |= unbindIf(WynnChaYuan.screenshotKey, GLFW.GLFW_KEY_F8);
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
