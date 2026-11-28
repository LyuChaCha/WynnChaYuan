package com.wynnchayuan.client.ui;

public final class PanelDrag {

    public static final int SNAP = 5;

    public record Moved(int x, int y, boolean snapX, boolean snapY) {}

    private PanelDrag() {}

    public static Moved move(int left, int top, int w, int h, int screenW, int screenH, int edge) {
        int centreX = (screenW - w) / 2;
        int centreY = (screenH - h) / 2;
        boolean nearX = Math.abs(left - centreX) <= SNAP;
        boolean nearY = Math.abs(top - centreY) <= SNAP;
        int x = clamp(nearX ? centreX : left, edge, screenW - w - edge);
        int y = clamp(nearY ? centreY : top, edge, screenH - h - edge);
        return new Moved(x, y, nearX && x == centreX, nearY && y == centreY);
    }

    public static int[] resize(int w, int h, int left, int top, PositionView.Size limits,
                               int screenW, int screenH, int edge) {
        int width = clamp(w, limits.minW(), Math.min(limits.maxW(), screenW - left - edge));
        int height = limits.tall()
                ? clamp(h, limits.minH(), Math.min(limits.maxH(), screenH - top - edge))
                : h;
        return new int[] {width, height};
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, Math.max(lo, hi)));
    }
}
