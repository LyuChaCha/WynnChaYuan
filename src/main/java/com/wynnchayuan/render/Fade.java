package com.wynnchayuan.render;

public final class Fade {

    private static final long DURATION_MS = 350;

    private Fade() {}

    public static float alphaFor(long lastUpdate, int holdMs) {
        if (holdMs == Integer.MAX_VALUE) {
            return 1.0f;
        }
        long elapsed = System.currentTimeMillis() - lastUpdate;
        if (elapsed <= holdMs) {
            return 1.0f;
        }
        long into = elapsed - holdMs;
        if (into >= DURATION_MS) {
            return 0.0f;
        }
        return 1.0f - (float) into / DURATION_MS;
    }
}
