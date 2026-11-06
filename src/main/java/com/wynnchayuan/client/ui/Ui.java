package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class Ui {

    private Ui() {}

    public static final int WINDOW = 0xC4161E19;
    public static final int WINDOW_SOLID = 0xF4161E19;
    public static final int WIN_EDGE = 0xFF2F4A39;
    public static final int GLOW_TOP = 0xFF24402E;
    public static final int GLOW_BOTTOM = 0xFF17352C;
    public static final int VEIL = 0xC8161E19;
    public static final int RAIL = 0x5522372A;
    public static final int RAIL_ON = 0xFF2A6A42;
    public static final int RAIL_ON_EDGE = 0xFF4A9A60;
    public static final int RAIL_ON_INK = 0xFFD9F5DD;
    public static final int CARD = 0x0AFFFFFF;
    public static final int FIELD = 0xB30F1512;
    public static final int RAISED = 0x14FFFFFF;
    public static final int BORDER = 0xFF34473B;
    public static final int LINE = 0xFF2A382F;
    public static final int GROUP = 0xFF9FC7A6;
    public static final int GROUP_RULE = 0xFF3F6B4C;
    public static final int POP = 0xFC18221C;
    public static final int SCENE = 0xFF1C2620;
    public static final int KNOB_OFF = 0xFF8A9A8E;
    public static final int TRACK_OFF = 0xFF323D36;

    public static final int TEXT = 0xFFE9EFE9;
    public static final int TEXT_2 = 0xFFC9D5CC;
    public static final int TEXT_3 = 0xFFA3B3A7;
    public static final int HINT = 0xFF8A9A8E;
    public static final int FAINT = 0xFF76867A;
    public static final int ON_ACCENT = 0xFF0C1A10;

    public static final int TITLE_FROM = 0xFF5FC063;
    public static final int TITLE_TO = 0xFFA2DF86;
    public static final int[] LEAF_STOPS = {0xFF3A9F4C, 0xFF5FC063, 0xFFA2DF86};

    public static final int MODE_OFF = 0xFF8A9A8E;
    public static final int OPTION_BOX = 0xE6111A15;
    public static final int RADIO = 0xFF5F7065;

    public static final int GOLD = 0xFFE0BC6E;
    public static final int GREEN = 0xFF7BC47F;
    public static final int AMBER = 0xFFE0B354;
    public static final int RED = 0xFFE0706B;

    public static final float R1 = 4.5f;
    public static final float R2 = 7f;
    public static final float R3 = 9f;

    public static int alpha(int argb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (argb & 0xFFFFFF);
    }

    public static int fade(int argb, float k) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, k)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            out |= (Math.round(ca + (cb - ca) * k) & 0xFF) << shift;
        }
        return out;
    }

    public static int stops(int[] stops, float t) {
        float at = Math.max(0f, Math.min(1f, t)) * (stops.length - 1);
        int i = Math.min((int) at, stops.length - 2);
        return mix(stops[i], stops[i + 1], at - i);
    }

    public static int deep(int argb) {
        float[] hsv = toHsv(argb);
        return (argb & 0xFF000000) | (hsv(hsv[0], Math.min(1f, hsv[1] * 1.65f),
                hsv[2] * 0.76f) & 0xFFFFFF);
    }

    public static void fadePane(Canvas c, float x, float y, float w, float h, float r, int argb,
                                float from, float to) {
        if (w <= 0 || h <= 0) {
            return;
        }
        c.fadeRound(x, y, w, h, Math.max(0, Math.min(r, Math.min(w, h) / 2f)), argb, from, to);
    }

    static void fadeBands(Canvas c, float x, float y, float w, float h, float r, int argb,
                          float from, float to) {
        float rr = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float step = Math.max(c.px(), 0.25f);
        for (float dy = 0; dy < rr; dy += step) {
            float d = rr - dy - step / 2f;
            float in = rr - (float) Math.sqrt(Math.max(0f, rr * rr - d * d));
            float a = from + (to - from) * in / w;
            float b = from + (to - from) * (w - in) / w;
            float band = Math.min(step, rr - dy);
            c.fade(x + in, y + dy, w - in * 2, band, argb, a, b);
            c.fade(x + in, y + h - dy - band, w - in * 2, band, argb, a, b);
        }
        if (h > rr * 2) {
            c.fade(x, y + rr, w, h - rr * 2, argb, from, to);
        }
    }

    public static void triangle(Canvas c, float cx, float cy, float size, float degrees,
                                int argb) {
        double a = Math.toRadians(degrees);
        float cos = (float) Math.cos(a);
        float sin = (float) Math.sin(a);
        float x0 = cx + (-0.55f * size) * cos - (-size) * sin;
        float y0 = cy + (-0.55f * size) * sin + (-size) * cos;
        float x1 = cx + (-0.55f * size) * cos - size * sin;
        float y1 = cy + (-0.55f * size) * sin + size * cos;
        float x2 = cx + 0.9f * size * cos;
        float y2 = cy + 0.9f * size * sin;
        float top = Math.min(y0, Math.min(y1, y2));
        float bottom = Math.max(y0, Math.max(y1, y2));
        float step = Math.max(c.px(), 0.25f);
        for (float y = top; y < bottom; y += step) {
            float mid = y + step / 2f;
            float a1 = cross(x0, y0, x1, y1, mid);
            float a2 = cross(x1, y1, x2, y2, mid);
            float a3 = cross(x2, y2, x0, y0, mid);
            float lo = Math.min(nanHigh(a1), Math.min(nanHigh(a2), nanHigh(a3)));
            float hi = Math.max(nanLow(a1), Math.max(nanLow(a2), nanLow(a3)));
            if (hi > lo) {
                c.fill(lo, y, hi, y + step, argb);
            }
        }
    }

    private static float nanHigh(float v) {
        return Float.isNaN(v) ? Float.MAX_VALUE : v;
    }

    private static float nanLow(float v) {
        return Float.isNaN(v) ? -Float.MAX_VALUE : v;
    }

    private static float cross(float ax, float ay, float bx, float by, float y) {
        if ((y < ay && y < by) || (y > ay && y > by) || ay == by) {
            return Float.NaN;
        }
        return ax + (bx - ax) * (y - ay) / (by - ay);
    }

    public static final int GLOW_PEAK = 0x4A;
    public static final float DITHER = 6f;

    public static void glowSpot(Canvas c, float cx, float cy, float rx, float ry, float x0,
                                float y0, float x1, float y1, float corner, int argb) {
        c.glow(x0, y0, x1, y1, cx, cy, rx, ry, corner, alpha(argb, GLOW_PEAK));
    }

    static void glowRings(Canvas c, float cx, float cy, float rx, float ry, float x0, float y0,
                          float x1, float y1, float corner, int argb) {
        for (int i = 0; i < 6; i++) {
            float k = 1f - i / 6f;
            float ax = Math.max(x0, cx - rx * k);
            float ay = Math.max(y0, cy - ry * k);
            float bx = Math.min(x1, cx + rx * k);
            float by = Math.min(y1, cy + ry * k);
            if (bx > ax && by > ay) {
                c.round(ax, ay, bx - ax, by - ay, Math.max(corner, ry * k), alpha(argb, 0x0E));
            }
        }
    }

    public static float gradientText(Canvas c, String text, float x, float y, float scale,
                                     int from, int to) {
        int n = text.codePointCount(0, text.length());
        float at = x;
        int i = 0;
        for (int k = 0; k < text.length(); ) {
            int cp = text.codePointAt(k);
            int end = k + Character.charCount(cp);
            String one = text.substring(k, end);
            c.text(one, at, y, mix(from, to, n <= 1 ? 0f : i / (float) (n - 1)), scale);
            at += c.width(one) * scale;
            k = end;
            i++;
        }
        return at;
    }

    public static int parseHex(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String v = hex.strip();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }
        if (!v.matches("[0-9a-fA-F]{6}")) {
            return fallback;
        }
        return 0xFF000000 | Integer.parseInt(v, 16);
    }

    public static String toHex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    public static int hsv(float h, float s, float v) {
        float hh = ((h % 360f) + 360f) % 360f;
        float c = v * s;
        float x = c * (1 - Math.abs((hh / 60f) % 2 - 1));
        float m = v - c;
        float r = 0;
        float g = 0;
        float b = 0;
        if (hh < 60) {
            r = c;
            g = x;
        } else if (hh < 120) {
            r = x;
            g = c;
        } else if (hh < 180) {
            g = c;
            b = x;
        } else if (hh < 240) {
            g = x;
            b = c;
        } else if (hh < 300) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        return 0xFF000000 | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    public static float[] toHsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) {
                h = ((g - b) / d) % 6;
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h *= 60;
            if (h < 0) {
                h += 360;
            }
        }
        return new float[] {h, max == 0 ? 0 : d / max, max};
    }

    public static float ease(float current, float target, float dtMs, float tauMs) {
        if (Math.abs(target - current) < 0.002f) {
            return target;
        }
        float k = 1f - (float) Math.exp(-Math.max(0f, dtMs) / tauMs);
        return current + (target - current) * k;
    }

    public static void box(Canvas c, int x, int y, int w, int h, int fill, int border) {
        if (w <= 2 || h <= 2) {
            c.fill(x, y, x + w, y + h, border != 0 ? border : fill);
            return;
        }
        if (fill != 0) {
            c.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        }
        if (border != 0) {
            c.fill(x + 1, y, x + w - 1, y + 1, border);
            c.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
            c.fill(x, y + 1, x + 1, y + h - 1, border);
            c.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
        } else if (fill != 0) {
            c.fill(x + 1, y, x + w - 1, y + 1, fill);
            c.fill(x + 1, y + h - 1, x + w - 1, y + h, fill);
            c.fill(x, y + 1, x + 1, y + h - 1, fill);
            c.fill(x + w - 1, y + 1, x + w, y + h - 1, fill);
        }
    }

    public static void pane(Canvas c, float x, float y, float w, float h, float r, int fill,
                            int edge) {
        if (fill != 0) {
            c.round(x, y, w, h, r, fill);
        }
        if (edge != 0) {
            c.ring(x, y, w, h, r, c.px(), edge);
        }
    }

    public static void halo(Canvas c, float x, float y, float w, float h, float r, int accent,
                            float strength) {
        if (strength <= 0.01f) {
            return;
        }
        float t = 1.5f;
        c.ring(x - t, y - t, w + t * 2, h + t * 2, r + t, t, fade(alpha(accent, 0x55), strength));
    }

    public static void glow(Canvas c, int x, int y, int w, int h, int argb) {
        box(c, x - 1, y - 1, w + 2, h + 2, 0, argb);
    }

    public static void glyph(Canvas c, int x, int y, String[] rows, int argb) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int at = 0;
            while (at < row.length()) {
                if (row.charAt(at) != '#') {
                    at++;
                    continue;
                }
                int end = at;
                while (end < row.length() && row.charAt(end) == '#') {
                    end++;
                }
                c.fill(x + at, y + r, x + end, y + r + 1, argb);
                at = end;
            }
        }
    }

    public static final String[] ICON_PIN = {"..#..", "..#..", "#####", "..#..", "..#..", "..#.."};

    public static String fit(Canvas c, String text, int maxW) {
        if (text == null) {
            return "";
        }
        if (maxW <= 0) {
            return "";
        }
        if (c.width(text) <= maxW) {
            return text;
        }
        int room = maxW - c.width("…");
        int end = text.length();
        while (end > 0 && c.width(text.substring(0, end)) > room) {
            end = text.offsetByCodePoints(end, -1);
        }
        return text.substring(0, end) + "…";
    }

    public static List<String> wrap(Canvas c, String text, int maxW) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        StringBuilder line = new StringBuilder();
        int lastBreak = -1;
        int at = 0;
        while (at < text.length()) {
            int cp = text.codePointAt(at);
            int len = Character.charCount(cp);
            if (cp == '\n') {
                out.add(line.toString().stripTrailing());
                line.setLength(0);
                lastBreak = -1;
                at += len;
                continue;
            }
            boolean free = wide(cp) && !hangul(cp);
            if (free && line.length() > 0) {
                lastBreak = line.length();
            }
            line.appendCodePoint(cp);
            if (cp == ' ') {
                lastBreak = line.length();
            } else if (free) {
                lastBreak = line.length();
            }
            if (c.width(line.toString()) > maxW && line.length() > len) {
                int cut = lastBreak > 0 && lastBreak < line.length() ? lastBreak
                        : line.length() - len;
                String head = line.substring(0, cut).stripTrailing();
                String tail = line.substring(cut).stripLeading();
                out.add(head);
                line.setLength(0);
                line.append(tail);
                lastBreak = -1;
            }
            at += len;
        }
        if (line.length() > 0) {
            out.add(line.toString().stripTrailing());
        }
        return out;
    }

    private static boolean wide(int cp) {
        return (cp >= 0x2E80 && cp <= 0x9FFF) || (cp >= 0xAC00 && cp <= 0xD7AF)
                || (cp >= 0xF900 && cp <= 0xFAFF) || (cp >= 0xFF00 && cp <= 0xFFEF)
                || (cp >= 0x3000 && cp <= 0x30FF);
    }

    private static boolean hangul(int cp) {
        return cp >= 0xAC00 && cp <= 0xD7AF;
    }

    public static float noise(int x, int y) {
        double k = 0.06711056 * x + 0.00583715 * y;
        double v = 52.9829189 * (k - Math.floor(k));
        return (float) (v - Math.floor(v)) - 0.5f;
    }

    public static float cornerCover(int x, int y, int w, int h, int r) {
        if (r <= 0) {
            return 1f;
        }
        float cx = x < r ? r : (x >= w - r ? w - r : -1);
        float cy = y < r ? r : (y >= h - r ? h - r : -1);
        if (cx < 0 || cy < 0) {
            return 1f;
        }
        float dx = x + 0.5f - cx;
        float dy = y + 0.5f - cy;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        return Math.max(0f, Math.min(1f, r - d + 0.5f));
    }

    public static byte[] fadeMask(int w, int h, int r, float from, float to, float dither) {
        byte[] out = new byte[Math.max(0, w * h)];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float cover = cornerCover(x, y, w, h, r);
                if (cover <= 0f) {
                    continue;
                }
                float a = from + (to - from) * (x + 0.5f) / w;
                float v = a > 0.5f ? a + noise(x, y) * dither : a;
                out[y * w + x] = (byte) Math.round(Math.max(0f, Math.min(255f, v)) * cover);
            }
        }
        return out;
    }

    public static byte[] glowMask(int w, int h, float cx, float cy, float rx, float ry, int fx,
                                  int fy, int fw, int fh, int r, float peak, float dither) {
        byte[] out = new byte[Math.max(0, w * h)];
        if (rx <= 0 || ry <= 0) {
            return out;
        }
        for (int y = 0; y < h; y++) {
            float dy = (y + 0.5f - cy) / ry;
            for (int x = 0; x < w; x++) {
                float dx = (x + 0.5f - cx) / rx;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d >= 1f) {
                    continue;
                }
                float t = 1f - d;
                float a = peak * t * t * (3f - 2f * t) + noise(x + fx, y + fy) * dither;
                out[y * w + x] = (byte) Math.round(Math.max(0f, Math.min(255f, a))
                        * cornerCover(x + fx, y + fy, fw, fh, r));
            }
        }
        return out;
    }

    public static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
