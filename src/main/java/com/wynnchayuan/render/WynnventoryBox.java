package com.wynnchayuan.render;

import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.joml.Vector2i;

import java.util.List;

public final class WynnventoryBox {

    private WynnventoryBox() {}

    private static final long FRESH_NANOS = 120_000_000L;

    private static final int PAD = 4;

    private static int[] box;
    private static long notedAt;

    public static void note(Vector2i at, List<ClientTooltipComponent> lines) {
        try {
            if (at == null || lines == null || lines.isEmpty()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            int w = 0;
            int h = 0;
            for (ClientTooltipComponent c : lines) {
                w = Math.max(w, c.getWidth(font));
                h += c.getHeight(font);
            }
            int screenH = mc.getWindow().getGuiScaledHeight();
            float scale = h + 10 > screenH ? screenH / (float) (h + 10) : 1f;
            note(Math.round(at.x * scale) - PAD, Math.round(at.y * scale) - PAD,
                    Math.round(w * scale) + PAD * 2, Math.round(h * scale) + PAD * 2,
                    System.nanoTime());
        } catch (Throwable t) {
            ErrorDebug.note("WynnventoryBox#note", null, t);
        }
    }

    static void note(int x, int y, int w, int h, long now) {
        box = new int[] {x, y, w, h};
        notedAt = now;
    }

    static void forget() {
        box = null;
    }

    public static int place(int x, int y, int w, int h, int leftSide, int rightSide,
                            int screenW, int gap) {
        return place(x, y, w, h, leftSide, rightSide, screenW, gap, System.nanoTime());
    }

    static int place(int x, int y, int w, int h, int leftSide, int rightSide,
                     int screenW, int gap, long now) {
        int[] b = box;
        if (b == null || now - notedAt > FRESH_NANOS || !hits(b, x, y, w, h)) {
            return x;
        }
        int tipLeft = leftSide + w + gap;
        int tipRight = rightSide - gap;
        boolean wasRight = Math.abs(x - rightSide) <= Math.abs(x - leftSide);
        int other = wasRight ? leftSide : rightSide;
        int beyond = b[0] >= tipRight ? b[0] + b[2] + gap
                : b[0] + b[2] <= tipLeft ? b[0] - gap - w
                : Integer.MIN_VALUE;
        for (int candidate : new int[] {other, beyond}) {
            if (candidate >= 0 && candidate + w <= screenW && !hits(b, candidate, y, w, h)) {
                return candidate;
            }
        }
        return x;
    }

    public static int[] current() {
        int[] b = box;
        return b == null || System.nanoTime() - notedAt > FRESH_NANOS ? null : b;
    }

    private static boolean hits(int[] b, int x, int y, int w, int h) {
        return x < b[0] + b[2] && b[0] < x + w && y < b[1] + b[3] && b[1] < y + h;
    }
}
