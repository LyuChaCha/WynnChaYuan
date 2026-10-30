package com.wynnchayuan.client;

public final class ModeColours {

    private ModeColours() {
    }

    public static final int OFF = 0xFFA0A0A0;

    static final int SOFT = 0xFF8FC3E6;

    public static int separate(int accent) {
        return soften(readable(accent) ? accent : SOFT);
    }

    public static int replace(int accent) {
        float[] hsv = hsv(separate(accent));
        return soften(rgb((hsv[0] + 0.5f) % 1f, hsv[1], hsv[2]));
    }

    public static int both(int accent) {
        return mix(separate(accent), replace(accent));
    }

    static boolean readable(int argb) {
        return luminance(argb) >= 96;
    }

    static int luminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    static int soften(int argb) {
        float[] hsv = hsv(argb);
        return rgb(hsv[0], Math.min(hsv[1], 0.55f), Math.max(hsv[2], 0.82f));
    }

    static int mix(int a, int b) {
        int r = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF)) / 2;
        int g = (((a >> 8) & 0xFF) + ((b >> 8) & 0xFF)) / 2;
        int bl = ((a & 0xFF) + (b & 0xFF)) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
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
