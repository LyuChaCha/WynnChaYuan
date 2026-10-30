package com.wynnchayuan.render;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class OriginalTooltipBox {

    private OriginalTooltipBox() {
    }

    // 原版內距 3＋外圈 9，Wynncraft 資源包把外圈畫滿稀有度框
    public static final int FRAME = 12;

    public record Box(float x, float y, float w, float h, float scale, int rawW, int rawH) {
    }

    private static final int KEEP = 8;

    private static long frame;
    private static final Box[] now = new Box[KEEP];
    private static int nowCount;
    private static final Box[] last = new Box[KEEP];
    private static int lastCount;
    private static int ownDepth;

    public static void nextFrame() {
        frame++;
        System.arraycopy(now, 0, last, 0, nowCount);
        lastCount = nowCount;
        nowCount = 0;
    }

    public static void beginOwn() {
        ownDepth++;
    }

    public static void endOwn() {
        ownDepth = Math.max(0, ownDepth - 1);
    }

    public static boolean drawingOwn() {
        return ownDepth > 0;
    }

    public static void note(GuiGraphics graphics, int x, int y, int w, int h) {
        try {
            if (ownDepth > 0 || WynnventoryLayout.drawing() || graphics == null || nowCount >= KEEP) {
                return;
            }
            Matrix3x2f m = graphics.pose();
            Vector2f a = m.transformPosition(x, y, new Vector2f());
            Vector2f b = m.transformPosition(x + w, y + h, new Vector2f());
            float sw = b.x - a.x;
            now[nowCount++] = new Box(a.x, a.y, sw, b.y - a.y, w == 0 ? 1f : sw / w, w, h);
        } catch (Throwable t) {
        }
    }

    public static Box find(int textW, int textH) {
        Box hit = find(now, nowCount, textW, textH);
        return hit != null ? hit : find(last, lastCount, textW, textH);
    }

    private static Box find(Box[] list, int count, int textW, int textH) {
        for (int i = 0; i < count; i++) {
            Box b = list[i];
            if (b != null && b.rawW() == textW && b.rawH() == textH) {
                return b;
            }
        }
        return null;
    }

    static void add(Box b) {
        if (nowCount < KEEP) {
            now[nowCount++] = b;
        }
    }

    static void forget() {
        nowCount = 0;
        lastCount = 0;
    }
}
