package com.wynnchayuan.render;

public final class PanelsHidden {

    private static volatile boolean hidden = false;

    private PanelsHidden() {}

    public static boolean hidden() {
        return hidden;
    }

    public static synchronized boolean toggle() {
        hidden = !hidden;
        return hidden;
    }
}
