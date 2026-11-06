package com.wynnchayuan.client.ui;

final class SceneText implements Canvas {

    private Canvas base;
    boolean dialogue;
    boolean shadow = true;
    float size = 1f;

    SceneText on(Canvas canvas) {
        this.base = canvas;
        this.dialogue = false;
        this.shadow = true;
        this.size = 1f;
        return this;
    }

    @Override
    public void text(String text, float x, float y, int argb) {
        base.gameText(text, x, y, argb, size, dialogue, shadow);
    }

    @Override
    public void text(String text, float x, float y, int argb, float scale) {
        base.gameText(text, x, y, argb, size * scale, dialogue, shadow);
    }

    @Override
    public int width(String text) {
        return Math.round(base.gameWidth(text, dialogue) * size);
    }

    @Override
    public float scale(float wanted) {
        return base.scale(wanted);
    }

    @Override
    public void fill(float x0, float y0, float x1, float y1, int argb) {
        base.fill(x0, y0, x1, y1, argb);
    }

    @Override
    public void round(float x, float y, float w, float h, float r, int argb) {
        base.round(x, y, w, h, r, argb);
    }

    @Override
    public void ring(float x, float y, float w, float h, float r, float t, int argb) {
        base.ring(x, y, w, h, r, t, argb);
    }

    @Override
    public void language(String lang) {
        base.language(lang);
    }

    @Override
    public void head(String minecraftName, float x, float y, float size) {
        base.head(minecraftName, x, y, size);
    }

    @Override
    public void clip(float x0, float y0, float x1, float y1) {
        base.clip(x0, y0, x1, y1);
    }

    @Override
    public void unclip() {
        base.unclip();
    }

    @Override
    public void logo(float x, float y, float size) {
        base.logo(x, y, size);
    }

    @Override
    public void icon(Icons.Icon icon, float x, float y, float size, int argb) {
        base.icon(icon, x, y, size, argb);
    }

    @Override
    public void svSquare(float x, float y, float w, float h, float hue) {
        base.svSquare(x, y, w, h, hue);
    }

    @Override
    public void hueBar(float x, float y, float w, float h) {
        base.hueBar(x, y, w, h);
    }

    @Override
    public float px() {
        return base.px();
    }

    @Override
    public void fade(float x, float y, float w, float h, int argb, float from, float to) {
        base.fade(x, y, w, h, argb, from, to);
    }

    @Override
    public int textureWidth(String id) {
        return base.textureWidth(id);
    }

    @Override
    public int textureHeight(String id) {
        return base.textureHeight(id);
    }

    @Override
    public void image(String id, float x, float y, float w, float h, float u, float v, float uw,
                      float vh, int argb) {
        base.image(id, x, y, w, h, u, v, uw, vh, argb);
    }

    @Override
    public void gameText(String text, float x, float y, int argb, float scale, boolean dialogueFont,
                         boolean withShadow) {
        base.gameText(text, x, y, argb, scale, dialogueFont, withShadow);
    }

    @Override
    public float gameWidth(String text, boolean dialogueFont) {
        return base.gameWidth(text, dialogueFont);
    }

    @Override
    public void raise() {
        base.raise();
    }
}
