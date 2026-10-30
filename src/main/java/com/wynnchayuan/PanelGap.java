package com.wynnchayuan;

public final class PanelGap {

    private PanelGap() {
    }

    public static final int MIN = 12;
    public static final int MAX = 200;
    public static final int DEFAULT = 12;
    public static final int FRAME_ALLOWANCE = 12;

    public static int clamp(int visible) {
        return Math.max(MIN, Math.min(MAX, visible));
    }

    public static int layout(int visible) {
        return clamp(visible) - FRAME_ALLOWANCE;
    }

    public static int visible(int layout) {
        return layout + FRAME_ALLOWANCE;
    }

    public static int preview(float visible, int budget) {
        return Math.max(0, Math.min(Math.round(visible), budget));
    }
}
