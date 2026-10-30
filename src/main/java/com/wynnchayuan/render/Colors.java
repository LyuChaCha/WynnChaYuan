package com.wynnchayuan.render;

public final class Colors {

    public static final int TEXT = 0xFFFFFFFF;

    public static final int HINT = 0xFF7A8794;

    public static final int DIM = 0xFF808080;

    public static final int SUBTLE = 0xFFA0A0A0;

    public static final int FAINT = 0xFF707070;

    public static final int HIGHLIGHT = 0xFFFFD24A;

    private Colors() {}

    public static int fade(int argb, float fraction) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * clamp(fraction));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
