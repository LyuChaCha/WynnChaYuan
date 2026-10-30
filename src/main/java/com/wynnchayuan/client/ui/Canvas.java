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
}
