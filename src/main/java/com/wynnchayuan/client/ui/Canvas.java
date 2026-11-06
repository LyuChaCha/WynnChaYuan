package com.wynnchayuan.client.ui;

public interface Canvas {

    void fill(float x0, float y0, float x1, float y1, int argb);

    void round(float x, float y, float w, float h, float r, int argb);

    void ring(float x, float y, float w, float h, float r, float t, int argb);

    void text(String text, float x, float y, int argb);

    void text(String text, float x, float y, int argb, float scale);

    float scale(float wanted);

    int width(String text);

    void language(String lang);

    void head(String minecraftName, float x, float y, float size);

    void clip(float x0, float y0, float x1, float y1);

    void unclip();

    void logo(float x, float y, float size);

    void icon(Icons.Icon icon, float x, float y, float size, int argb);

    void svSquare(float x, float y, float w, float h, float hue);

    void hueBar(float x, float y, float w, float h);

    float px();

    default void fade(float x, float y, float w, float h, int argb, float from, float to) {
        fill(x, y, x + w, y + h, Ui.fade(argb, (from + to) / 2f));
    }

    default void fadeRound(float x, float y, float w, float h, float r, int argb, float from,
                           float to) {
        Ui.fadeBands(this, x, y, w, h, r, argb, from, to);
    }

    default void glow(float x0, float y0, float x1, float y1, float cx, float cy, float rx,
                      float ry, float r, int argb) {
        Ui.glowRings(this, cx, cy, rx, ry, x0, y0, x1, y1, r, argb);
    }

    default void gameText(String text, float x, float y, int argb, float scale, boolean dialogue,
                          boolean shadow) {
        text(text, x, y, argb, scale);
    }

    default float gameWidth(String text, boolean dialogue) {
        return width(text);
    }

    default int textureWidth(String id) {
        return 0;
    }

    default int textureHeight(String id) {
        return 0;
    }

    default void image(String id, float x, float y, float w, float h, float u, float v, float uw,
                       float vh, int argb) {
    }

    default void raise() {
    }
}
