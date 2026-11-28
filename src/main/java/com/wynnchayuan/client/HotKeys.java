package com.wynnchayuan.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wynnchayuan.translate.ErrorDebug;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class HotKeys {

    private record Hotkey(KeyMapping mapping, Runnable action, KeyLatch latch) {}

    private static final List<Hotkey> KEYS = new CopyOnWriteArrayList<>();

    private HotKeys() {}

    public static synchronized void add(KeyMapping mapping, Runnable action) {
        if (mapping == null || action == null) {
            return;
        }
        if (KEYS.isEmpty()) {
            listenToScreens();
        }
        KEYS.add(new Hotkey(mapping, action, new KeyLatch()));
    }

    public static void tick(Minecraft client) {
        for (Hotkey key : KEYS) {
            boolean clicked = false;
            while (key.mapping().consumeClick()) {
                clicked = true;
            }
            if (clicked) {
                fire(key);
            }
            key.latch().settle(held(client, key.mapping()));
        }
    }

    private static void listenToScreens() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            // afterKeyPress 只在畫面沒吃鍵時跑，背包與 Wynntils 會吃鍵
            ScreenKeyboardEvents.beforeKeyPress(screen).register(HotKeys::pressed);
            ScreenKeyboardEvents.afterKeyPress(screen).register(HotKeys::pressed);
        });
    }

    private static void pressed(Screen screen, KeyEvent event) {
        for (Hotkey key : KEYS) {
            try {
                if (key.mapping().matches(event) && !(typing(screen) && printable(event))) {
                    fire(key);
                }
            } catch (Throwable t) {
                ErrorDebug.note("HotKeys#pressed", key.mapping().getName(), t);
            }
        }
    }

    private static void fire(Hotkey key) {
        if (!key.latch().press()) {
            return;
        }
        try {
            key.action().run();
        } catch (Throwable t) {
            ErrorDebug.note("HotKeys#fire", key.mapping().getName(), t);
        }
    }

    private static boolean typing(Screen screen) {
        return screen instanceof ChatScreen
                || screen instanceof AbstractSignEditScreen
                || screen instanceof BookEditScreen
                || screen.getFocused() instanceof EditBox;
    }

    private static boolean printable(KeyEvent event) {
        return event.key() >= 0 && event.key() < GLFW.GLFW_KEY_ESCAPE;
    }

    private static boolean held(Minecraft client, KeyMapping mapping) {
        try {
            InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(mapping);
            if (client == null || client.getWindow() == null || bound == null
                    || bound.getType() != InputConstants.Type.KEYSYM || bound.getValue() < 0) {
                return false;
            }
            return InputConstants.isKeyDown(client.getWindow(), bound.getValue());
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasKey(KeyMapping mapping) {
        if (mapping == null) {
            return false;
        }
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mapping);
        return key != null && key.getValue() != InputConstants.UNKNOWN.getValue();
    }

    public static String keyName(KeyMapping mapping) {
        return mapping == null ? "" : mapping.getTranslatedKeyMessage().getString();
    }

    public static String clash(KeyMapping mapping) {
        if (!hasKey(mapping)) {
            return null;
        }
        InputConstants.Key mine = KeyBindingHelper.getBoundKeyOf(mapping);
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return null;
        }
        for (KeyMapping other : mc.options.keyMappings) {
            if (other == mapping) {
                continue;
            }
            InputConstants.Key theirs = KeyBindingHelper.getBoundKeyOf(other);
            if (theirs != null && theirs.getType() == mine.getType()
                    && theirs.getValue() == mine.getValue()) {
                return Component.translatable(other.getName()).getString();
            }
        }
        return null;
    }
}
