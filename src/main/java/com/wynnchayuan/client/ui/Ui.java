package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class Ui {

    private Ui() {}

    public static final int WINDOW = 0xB8181F2A;
    public static final int WINDOW_SOLID = 0xF0161C26;
    public static final int WIN_EDGE = 0x29FFFFFF;
    public static final int RAIL = 0x0AFFFFFF;
    public static final int CARD = 0x0EFFFFFF;
    public static final int FIELD = 0x4D000000;
    public static final int RAISED = 0x16FFFFFF;
    public static final int BORDER = 0x26FFFFFF;
    public static final int LINE = 0x17FFFFFF;
    public static final int POP = 0xFC1B2330;
    public static final int SCENE = 0xFF0C1219;
    public static final int KNOB_OFF = 0xFF7B8694;
    public static final int TRACK_OFF = 0x33FFFFFF;

    public static final int TEXT = 0xFFF2F5F9;
    public static final int TEXT_2 = 0xFFD3DAE3;
    public static final int TEXT_3 = 0xFFAEB8C5;
    public static final int HINT = 0xFF8D98A6;
    public static final int FAINT = 0xFF7B8694;
    public static final int ON_ACCENT = 0xFF0B1420;

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

    public static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
