package com.wynnchayuan.client.ui;

import java.util.Random;

final class Leaves {

    private static final float PX = 2f;
    private static final int COUNT = 6;
    private static final int OPACITY = 0x3D;
    private static final int WISP = 0xFFE8F8DE;

    private static final String[][] UP = {
        {"..o..", ".oLo.", "oLLGo", "oLVGo", "oGVGo", ".oVo.", "..s.."},
        {"..o..", ".oLo.", ".oVo.", ".oVo.", ".oVo.", "..o..", "..s.."},
        {".....", "..o..", "..o..", "..o..", "..o..", "..o..", "..s.."},
    };
    private static final String[][] DIAG = {
        {"....oo", "...oLo", "..oLVo", ".oLVGo", ".oVGo.", "so...."},
        {".....o", "....oo", "...oVo", "..oVo.", ".oVo..", "so...."},
        {"......", "....o.", "...o..", "..o...", ".o....", "s....."},
    };
    private static final int[][] SETS = {
        {0x3E6828, 0x6EA642, 0xA0CE60, 0x508232, 0x705636},
        {0x2C6E40, 0x4FA060, 0x7FD08A, 0x347C48, 0x705636},
        {0x286246, 0x469664, 0x76C48C, 0x327650, 0x705636},
    };
    private static final int[] FLIP_SHAPE = {0, 1, 2, 1, 0};
    private static final boolean[] FLIP_BACK = {false, false, false, true, true};

    private static final Sprite[] SPRITES = bake();

    private record Sprite(int w, int h, int[] xs, int[] ys, int[] colours) {}

    private static final class Leaf {
        float x;
        float y0;
        float base;
        float fall;
        float amp;
        float f;
        float ph;
        float t;
        int set;
        int dir;
        int o0;
        float off;
        float flip;
        float gust;
        boolean live;
    }

    private static final class Gust {
        float x;
        float v;
        float k;
        boolean live;
        final float[] dy = new float[2];
        final float[] lag = new float[2];
        final float[] len = new float[2];
        final float[] wave = new float[2];
        final float[] phase = new float[2];
    }

    private final Leaf[] leaves = new Leaf[COUNT];
    private final Gust[] gusts = {new Gust(), new Gust()};
    private final Random random = new Random();
    private float untilGust = rand(3000, 6000);
    private boolean seeded;

    Leaves() {
        for (int i = 0; i < COUNT; i++) {
            leaves[i] = new Leaf();
        }
    }

    private static Sprite[] bake() {
        Sprite[] out = new Sprite[SETS.length * 3 * 2 * 8];
        for (int set = 0; set < SETS.length; set++) {
            for (int shape = 0; shape < 3; shape++) {
                for (int back = 0; back < 2; back++) {
                    for (int idx = 0; idx < 8; idx++) {
                        out[index(set, shape, back == 1, idx)] = sprite(set, shape, back == 1, idx);
                    }
                }
            }
        }
        return out;
    }

    private static int index(int set, int shape, boolean back, int idx) {
        return ((set * 3 + shape) * 2 + (back ? 1 : 0)) * 8 + idx;
    }

    private static Sprite sprite(int set, int shape, boolean back, int idx) {
        String[] rows = (idx % 2 == 1 ? DIAG : UP)[shape];
        int h = rows.length;
        int w = rows[0].length();
        int n = 0;
        for (String row : rows) {
            for (int x = 0; x < row.length(); x++) {
                n += row.charAt(x) != '.' ? 1 : 0;
            }
        }
        int[] xs = new int[n];
        int[] ys = new int[n];
        int[] colours = new int[n];
        int at = 0;
        int[] c = SETS[set];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char ch = rows[y].charAt(x);
                if (ch == '.') {
                    continue;
                }
                int colour = switch (ch) {
                    case 'o' -> c[0];
                    case 'G' -> back ? dim(c[1]) : c[1];
                    case 'L' -> back ? dim(c[1]) : c[2];
                    case 'V' -> back ? dim(c[3]) : c[3];
                    default -> c[4];
                };
                int px = x;
                int py = y;
                if (back) {
                    if (idx % 2 == 1) {
                        px = w - 1 - y;
                        py = h - 1 - x;
                    } else {
                        px = w - 1 - x;
                    }
                }
                xs[at] = px;
                ys[at] = py;
                colours[at] = colour;
                at++;
            }
        }
        for (int turn = 0; turn < idx >> 1; turn++) {
            for (int i = 0; i < n; i++) {
                int px = h - 1 - ys[i];
                ys[i] = xs[i];
                xs[i] = px;
            }
            int was = h;
            h = w;
            w = was;
        }
        return new Sprite(w, h, xs, ys, colours);
    }

    private static int dim(int rgb) {
        int r = Math.round(((rgb >> 16) & 0xFF) * 0.7f);
        int g = Math.round(((rgb >> 8) & 0xFF) * 0.7f);
        int b = Math.round((rgb & 0xFF) * 0.7f);
        return (r << 16) | (g << 8) | b;
    }

    private float rand(float lo, float hi) {
        return lo + random.nextFloat() * (hi - lo);
    }

    private void spawn(Leaf l, float w, float h, boolean anywhere) {
        l.x = anywhere ? rand(0, w) : rand(-20, -6);
        l.y0 = rand(0, Math.max(1, h - 4));
        l.base = rand(8, 15);
        l.fall = rand(0.6f, 2.4f);
        l.amp = rand(1.2f, 2.6f);
        l.f = rand(0.35f, 0.6f);
        l.ph = rand(0, 6.3f);
        l.t = 0;
        l.set = random.nextInt(SETS.length);
        l.dir = random.nextBoolean() ? 1 : -1;
        l.o0 = random.nextInt(8);
        l.off = random.nextFloat();
        l.flip = 0;
        l.gust = 0;
        l.live = true;
    }

    private void blow(float h) {
        for (Gust g : gusts) {
            if (g.live) {
                continue;
            }
            g.live = true;
            g.x = -30;
            g.v = rand(150, 190);
            g.k = rand(0.65f, 1f);
            for (int i = 0; i < 2; i++) {
                g.dy[i] = rand(4, Math.max(5, h - 4));
                g.lag[i] = rand(0, 26);
                g.len[i] = rand(28, 52);
                g.wave[i] = rand(0.8f, 1.8f);
                g.phase[i] = rand(0, 6.3f);
            }
            return;
        }
    }

    private float wind(float x) {
        float total = 0;
        for (Gust g : gusts) {
            if (!g.live) {
                continue;
            }
            float d = g.x - x;
            if (d < -12) {
                continue;
            }
            float rise = Math.min(1f, (d + 12) / 20f);
            float fall = (float) Math.exp(-Math.max(0, d - 8) / 70f);
            total = Math.max(total, rise * fall * g.k);
        }
        return total;
    }

    void draw(Canvas c, float x, float y, float width, float height, float dtMs) {
        float w = width / PX;
        float h = height / PX;
        if (w < 10 || h < 10) {
            return;
        }
        if (!seeded) {
            for (Leaf l : leaves) {
                spawn(l, w, h, true);
            }
            seeded = true;
        }
        float dt = Math.min(0.05f, Math.max(0f, dtMs / 1000f));
        untilGust -= dtMs;
        if (untilGust <= 0) {
            blow(h);
            untilGust = rand(9000, 15000);
        }
        for (Gust g : gusts) {
            if (g.live) {
                g.x += g.v * dt;
                g.live = g.x - 90 < w + 40;
            }
        }
        for (Leaf l : leaves) {
            l.gust = wind(l.x);
            l.t += dt;
            l.x += (l.base + 52 * l.gust) * dt;
            l.flip += (0.5f + 3.6f * l.gust) * dt;
            if (l.x > w + 12 || l.y0 + l.fall * l.t > h + 12) {
                spawn(l, w, h, false);
            }
        }

        c.clip(x, y, x + width, y + height);
        for (Gust g : gusts) {
            if (g.live) {
                wisps(c, x, y, w, g);
            }
        }
        for (Leaf l : leaves) {
            leaf(c, x, y, l);
        }
        c.unclip();
    }

    private void wisps(Canvas c, float ox, float oy, float w, Gust g) {
        float k = (g.x + 30) / (w + 160);
        float fade = Math.max(0, Math.min(1, Math.min(k / 0.12f, (1 - k) / 0.18f)));
        for (int i = 0; i < 2; i++) {
            float head = g.x - g.lag[i];
            for (int s = 0; s < g.len[i]; s++) {
                float a = 0.2f * fade * (float) Math.pow(1 - s / g.len[i], 1.5);
                int alpha = Math.round(a * OPACITY);
                if (alpha < 1) {
                    continue;
                }
                float px = (float) Math.floor(head - s);
                float py = (float) Math.floor(g.dy[i] + g.wave[i] * Math.sin((head - s) / 13f + g.phase[i]));
                c.fill(ox + px * PX, oy + py * PX, ox + (px + 1) * PX, oy + (py + 1) * PX,
                       Ui.alpha(WISP, alpha));
            }
        }
    }

    private void leaf(Canvas c, float ox, float oy, Leaf l) {
        float spin = l.flip + l.off;
        int turn = (int) Math.floor(spin);
        float frac = spin - turn;
        int stage = frac < 0.66f ? 0 : Math.min(4, (int) Math.floor((frac - 0.66f) / 0.34f * 5));
        boolean back = (turn % 2 == 1) != FLIP_BACK[stage];
        int idx = Math.floorMod(l.o0 + l.dir * turn, 8);
        Sprite s = SPRITES[index(l.set, FLIP_SHAPE[stage], back, idx)];
        float swing = (float) Math.sin(2 * Math.PI * 2 * l.f * l.t + l.ph);
        float px = l.x + 2 * swing;
        float py = l.y0 + l.fall * l.t + l.amp * (float) Math.sin(2 * Math.PI * l.f * l.t + l.ph)
                + Math.abs(swing) - 3 * l.gust;
        float left = Math.round(px - s.w() / 2f);
        float top = Math.round(py - s.h() / 2f);
        for (int i = 0; i < s.xs().length; i++) {
            float ax = ox + (left + s.xs()[i]) * PX;
            float ay = oy + (top + s.ys()[i]) * PX;
            c.fill(ax, ay, ax + PX, ay + PX, (OPACITY << 24) | s.colours()[i]);
        }
    }
}
