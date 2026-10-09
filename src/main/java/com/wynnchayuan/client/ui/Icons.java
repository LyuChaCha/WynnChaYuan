package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 線條圖示與圓角的遮罩。
 *
 * <h2>為什麼自己算</h2>
 * 遊戲的畫布只會畫矩形與貼圖。圓角、圓形開關、斜線都得先算成一張「每個點蓋住
 * 幾成」的遮罩，再當貼圖染色貼上去。這裡只做算的那一半——純 Java、不碰遊戲，
 * 所以測試用的畫布也拿同一份遮罩來畫，兩邊看到的形狀一模一樣。
 *
 * <h2>圖示</h2>
 * 每個圖示是 24×24 格子裡的幾條折線，線寬 2 格。題材是 Wynncraft 的東西：
 * 弓（物品）、卷軸（面板）、對話框、羅盤（世界）、任務書（資料）、任務標記（須知）。
 * 遮罩是照「這個點離最近的線多遠」算的，所以放大縮小都是平滑的邊，不是把小圖拉大。
 */
public final class Icons {

    private Icons() {}

    /** 圖示的格子邊長與線寬（格）。 */
    private static final float BOX = 24f;
    private static final float STROKE = 2f;

    public enum Icon {
        BOW(cubic(7, 3, 15, 5, 15, 19, 7, 21), line(7, 3, 7, 21), line(2, 12, 20, 12),
            line(16, 8, 20, 12, 16, 16)),
        SCROLL(rect(4, 4, 16, 4), rect(4, 16, 16, 4), line(6, 8, 6, 16), line(18, 8, 18, 16),
               line(9.5f, 12, 14.5f, 12)),
        BUBBLE(closed(3, 4, 21, 4, 21, 16, 12, 16, 7, 21, 7, 16, 3, 16), dot(8, 10), dot(12, 10),
               dot(16, 10)),
        COMPASS(circle(12, 12, 9), closed(16, 8, 13.5f, 13.5f, 8, 16, 10.5f, 10.5f)),
        BOOK(rect(5, 3, 14, 18), line(9, 3, 9, 21), line(12.5f, 3, 12.5f, 10, 14.5f, 8.5f, 16.5f, 10,
             16.5f, 3)),
        QUEST(closed(12, 2, 22, 12, 12, 22, 2, 12), line(12, 7.5f, 12, 13), dot(12, 16.5f)),
        SEARCH(circle(10.5f, 10.5f, 6.5f), line(15.5f, 15.5f, 21, 21)),
        RESET(arc(12, 12, 8, 180, 500), line(4, 6, 4, 12, 10, 12)),
        DOWN(line(6, 9, 12, 15, 18, 9)),
        UP(line(6, 15, 12, 9, 18, 15)),
        RIGHT(line(9, 6, 15, 12, 9, 18)),
        CHECK(line(5, 12.5f, 10, 17.5f, 19, 7)),
        CLOSE(line(6, 6, 18, 18), line(18, 6, 6, 18));

        final float[][] paths;

        Icon(float[]... paths) {
            this.paths = paths;
        }
    }

    private static float[] line(float... xy) {
        return xy;
    }

    private static float[] closed(float... xy) {
        float[] out = new float[xy.length + 2];
        System.arraycopy(xy, 0, out, 0, xy.length);
        out[xy.length] = xy[0];
        out[xy.length + 1] = xy[1];
        return out;
    }

    private static float[] rect(float x, float y, float w, float h) {
        return closed(x, y, x + w, y, x + w, y + h, x, y + h);
    }

    /** 一個點：長度是零的線段，畫出來是線寬那麼大的圓點。 */
    private static float[] dot(float x, float y) {
        return new float[] {x, y, x, y};
    }

    private static float[] circle(float cx, float cy, float r) {
        return arc(cx, cy, r, 0, 360);
    }

    /** 角度是螢幕座標（y 往下），從 {@code from} 順時針走到 {@code to}。 */
    private static float[] arc(float cx, float cy, float r, float from, float to) {
        int steps = Math.max(8, Math.round(Math.abs(to - from) / 12f));
        float[] out = new float[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(from + (to - from) * i / steps);
            out[i * 2] = cx + r * (float) Math.cos(a);
            out[i * 2 + 1] = cy + r * (float) Math.sin(a);
        }
        return out;
    }

    private static float[] cubic(float x0, float y0, float x1, float y1, float x2, float y2,
                                 float x3, float y3) {
        int steps = 16;
        float[] out = new float[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float u = 1 - t;
            out[i * 2] = u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3;
            out[i * 2 + 1] = u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2
                    + t * t * t * y3;
        }
        return out;
    }

    /**
     * 圖示的遮罩。
     *
     * @param size 邊長（螢幕上的點）
     * @return {@code size × size} 個值，0 是沒蓋到、255 是整個蓋住
     */
    public static byte[] mask(Icon icon, int size) {
        int n = Math.max(1, size);
        float k = n / BOX;
        // 線寬跟著縮放走，但不讓它細過 1.25 個點——再細就斷斷續續了
        float half = Math.max(0.625f, STROKE * k / 2f);
        List<float[]> segs = new ArrayList<>();
        for (float[] path : icon.paths) {
            for (int i = 0; i + 3 < path.length; i += 2) {
                segs.add(new float[] {path[i] * k, path[i + 1] * k, path[i + 2] * k,
                                      path[i + 3] * k});
            }
        }
        byte[] out = new byte[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                float best = Float.MAX_VALUE;
                for (float[] s : segs) {
                    best = Math.min(best, distance(px, py, s[0], s[1], s[2], s[3]));
                }
                float cover = Math.max(0f, Math.min(1f, half + 0.5f - best));
                out[y * n + x] = (byte) Math.round(cover * 255f);
            }
        }
        return out;
    }

    private static float distance(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float len = dx * dx + dy * dy;
        float t = len == 0 ? 0 : Math.max(0f, Math.min(1f, ((px - ax) * dx + (py - ay) * dy) / len));
        float cx = ax + dx * t - px;
        float cy = ay + dy * t - py;
        return (float) Math.sqrt(cx * cx + cy * cy);
    }

    /**
     * 一個圓的遮罩，邊長 {@code 2r}。圓角矩形的四個角就是它的四個象限。
     *
     * @param inner 大於零的話挖掉裡面這個半徑的圓，剩一圈——畫框線用
     */
    public static byte[] disc(int r, float inner) {
        int n = Math.max(1, r) * 2;
        byte[] out = new byte[n * n];
        float c = n / 2f;
        float outer = n / 2f;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float dx = x + 0.5f - c;
                float dy = y + 0.5f - c;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float cover = Math.max(0f, Math.min(1f, outer - d + 0.5f));
                if (inner > 0) {
                    cover = Math.min(cover, Math.max(0f, Math.min(1f, d - inner + 0.5f)));
                }
                out[y * n + x] = (byte) Math.round(cover * 255f);
            }
        }
        return out;
    }
}
