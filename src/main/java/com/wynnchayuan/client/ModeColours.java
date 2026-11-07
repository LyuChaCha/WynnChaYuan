package com.wynnchayuan.client;

public final class ModeColours {

    private ModeColours() {
    }

    public static final int OFF = 0xFF8A9A8E;
    public static final int OFF_DEEP = 0xFF55645A;

    public static final int LEAF = 0xFF7FD08A;
    static final int LEAF_DEEP = 0xFF3A9F4C;
    static final int GOLD = 0xFFE8C876;
    static final int GOLD_DEEP = 0xFFB98A3C;
    static final int CYAN = 0xFF6FE0EC;
    static final int CYAN_DEEP = 0xFF2F9FB4;

    public static int separate(int accent) {
        return turn(LEAF, accent);
    }

    public static int replace(int accent) {
        return turn(GOLD, accent);
    }

    public static int both(int accent) {
        return turn(CYAN, accent);
    }

    public static int separateDeep(int accent) {
        return turn(LEAF_DEEP, accent);
    }

    public static int replaceDeep(int accent) {
        return turn(GOLD_DEEP, accent);
    }

    public static int bothDeep(int accent) {
        return turn(CYAN_DEEP, accent);
    }

    static int turn(int base, int accent) {
        float[] theme = hsv(accent);
        if (theme[1] < 0.12f || theme[2] < 0.08f) {
            return base;
        }
        float shift = theme[0] - hsv(LEAF)[0];
        if (Math.abs(shift) < 0.004f) {
            return base;
        }
        float[] own = hsv(base);
        return rgb(((own[0] + shift) % 1f + 1f) % 1f, own[1], own[2]);
    }

    static int luminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    static float[] hsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) {
            h = 0;
        } else if (max == r) {
            h = ((g - b) / d) % 6f;
        } else if (max == g) {
            h = (b - r) / d + 2f;
        } else {
            h = (r - g) / d + 4f;
        }
        h /= 6f;
        if (h < 0) {
            h += 1f;
        }
        return new float[] {h, max == 0 ? 0 : d / max, max};
    }

    static int rgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6f) % 2f - 1));
        float m = v - c;
        float r;
        float g;
        float b;
        switch ((int) (h * 6f) % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return 0xFF000000
                | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8)
                | Math.round((b + m) * 255);
    }
}
