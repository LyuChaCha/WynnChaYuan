package com.wynnchayuan.render;

public record PanelBox(int x, int y, int w, int h) {

    public static final PanelBox NONE = new PanelBox(0, 0, 0, 0);

    public boolean empty() {
        return w <= 0 || h <= 0;
    }
}
