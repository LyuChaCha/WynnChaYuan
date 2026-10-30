package com.wynnchayuan.render;

import net.minecraft.network.chat.Component;

import java.util.List;

// Wynntils 的 Pre 早於原版 tooltip、Post 從未發送
public final class PendingTooltip {

    private static volatile List<Component> lines = List.of();
    private static volatile int mouseX;
    private static volatile int mouseY;
    private static volatile boolean pushed;

    private PendingTooltip() {}

    public static void set(List<Component> tooltip, int x, int y) {
        set(tooltip, x, y, false);
    }

    public static void set(List<Component> tooltip, int x, int y, boolean pushedOriginal) {
        lines = tooltip == null ? List.of() : List.copyOf(tooltip);
        mouseX = x;
        mouseY = y;
        pushed = pushedOriginal;
    }

    public static boolean pushed() {
        return pushed;
    }

    public static List<Component> take() {
        List<Component> current = lines;
        lines = List.of();
        return current;
    }

    public static int mouseX() {
        return mouseX;
    }

    public static int mouseY() {
        return mouseY;
    }
}
