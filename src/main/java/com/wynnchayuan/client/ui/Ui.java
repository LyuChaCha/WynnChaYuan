package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 設定畫面的色票與小工具：圓角框、像素圖示、折行、顏色運算、緩動。
 *
 * <p>全部是純函式，只碰 {@link Canvas}——測試裡可以直接呼叫。
 */
public final class Ui {

    private Ui() {}

    // ------------------------------------------------------------ 色票
    //
    // 霧面玻璃：視窗本身半透明，底下是遊戲被模糊過的畫面。所以每一層都不是
    // 「一個顏色」，而是「往上疊一點白」或「往下壓一點黑」——卡片比視窗亮一點、
    // 輸入框比視窗暗一點，換到哪一種背景上都分得出層次。
    // 重點色是使用者的「風格顏色」，不在這裡；符文金只留給「這裡有東西要看」。

    /** 視窗底色。底下有模糊時用這個。 */
    public static final int WINDOW = 0xB8181F2A;
    /** 玩家把「選單背景模糊」關掉時用這個：沒有模糊，半透明的底會讓字很難讀。 */
    public static final int WINDOW_SOLID = 0xF0161C26;
    public static final int WIN_EDGE = 0x29FFFFFF;
    public static final int RAIL = 0x0AFFFFFF;
    public static final int CARD = 0x0EFFFFFF;
    public static final int FIELD = 0x4D000000;
    public static final int RAISED = 0x16FFFFFF;
    public static final int BORDER = 0x26FFFFFF;
    public static final int LINE = 0x17FFFFFF;
    /** 彈出來的東西（色盤、下拉、說明）要蓋住底下的字，所以幾乎不透明。 */
    public static final int POP = 0xFC1B2330;
    /** 預覽裡那張示意圖的底：它在演遊戲畫面，不跟著玻璃透明。 */
    public static final int SCENE = 0xFF0C1219;
    public static final int KNOB_OFF = 0xFF7B8694;
    public static final int TRACK_OFF = 0x33FFFFFF;

    public static final int TEXT = 0xFFF2F5F9;
    public static final int TEXT_2 = 0xFFD3DAE3;
    public static final int TEXT_3 = 0xFFAEB8C5;
    public static final int HINT = 0xFF8D98A6;
    public static final int FAINT = 0xFF7B8694;
    /** 壓在重點色上面的字：重點色都偏亮，深色字才讀得清楚。 */
    public static final int ON_ACCENT = 0xFF0B1420;

    public static final int GOLD = 0xFFE0BC6E;
    public static final int GREEN = 0xFF7BC47F;
    public static final int AMBER = 0xFFE0B354;
    public static final int RED = 0xFFE0706B;

    /** 圓角：控制項、卡片、視窗。單位是 GUI 像素。 */
    public static final float R1 = 4.5f;
    public static final float R2 = 7f;
    public static final float R3 = 9f;

    // ------------------------------------------------------------ 顏色運算

    /** 換掉 alpha（0–255），RGB 不動。 */
    public static int alpha(int argb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (argb & 0xFFFFFF);
    }

    /** alpha 乘上一個係數，做淡入淡出。 */
    public static int fade(int argb, float k) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, k)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** 兩個顏色之間取一點；{@code t} 是 0 回 {@code a}、1 回 {@code b}。 */
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

    /** {@code #RRGGBB}；格式不對回 {@code fallback}。 */
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

    /** 色相 0–360、飽和與明度 0–1。 */
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

    /** @return {@code {色相, 飽和, 明度}} */
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

    // ------------------------------------------------------------ 緩動

    /**
     * 往目標靠近一步，跟幀率無關。
     *
     * <p>不用「每幀走固定比例」：那樣 144 Hz 的螢幕動畫會比 60 Hz 快一倍多。
     * 指數衰減照經過的時間算，哪一種更新率走出來的曲線都一樣。
     *
     * @param tauMs 時間常數，大約是走完六成所需的毫秒數
     */
    public static float ease(float current, float target, float dtMs, float tauMs) {
        if (Math.abs(target - current) < 0.002f) {
            return target;
        }
        float k = 1f - (float) Math.exp(-Math.max(0f, dtMs) / tauMs);
        return current + (target - current) * k;
    }

    // ------------------------------------------------------------ 形狀

    /**
     * 圓角框。{@code fill} 只能畫矩形，所以「四角各缺一格」來暗示圓角——
     * 這個尺寸下遠看夠像，而且每一個像素都落在格子上，不會糊。
     *
     * @param border 傳 0 就不畫框線
     */
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

    /**
     * 玻璃風的一塊：圓角底色加一圈髮絲線。{@code edge} 傳 0 就不畫線。
     *
     * <p>線寬是螢幕上的一個點，不是一個 GUI 像素——介面縮放是 3 的時候後者有
     * 三個點粗，看起來就不是玻璃的邊，是一條框。
     */
    public static void pane(Canvas c, float x, float y, float w, float h, float r, int fill,
                            int edge) {
        if (fill != 0) {
            c.round(x, y, w, h, r, fill);
        }
        if (edge != 0) {
            c.ring(x, y, w, h, r, c.px(), edge);
        }
    }

    /** 滑鼠指著、或正在用的控制項：外面多一圈重點色的光暈。 */
    public static void halo(Canvas c, float x, float y, float w, float h, float r, int accent,
                            float strength) {
        if (strength <= 0.01f) {
            return;
        }
        float t = 1.5f;
        c.ring(x - t, y - t, w + t * 2, h + t * 2, r + t, t, fade(alpha(accent, 0x55), strength));
    }

    /** 框外面再套一圈淡淡的光——滑鼠指著、或鍵盤焦點在上面的時候。 */
    public static void glow(Canvas c, int x, int y, int w, int h, int argb) {
        box(c, x - 1, y - 1, w + 2, h + 2, 0, argb);
    }

    /**
     * 像素圖示。每個字串是一列，{@code #} 是要塗的格子。
     *
     * <p>不用貼圖：這幾個圖示只有七八像素見方，而且要跟著主題色變色。
     * 連續的格子併成一個矩形再畫，一個圖示大約十次 {@code fill}。
     */
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

    // ------------------------------------------------------------ 文字

    /** 放不下就截斷補「…」。凸出格子外比截斷難看得多。 */
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

    /**
     * 折行。
     *
     * <p>中日文沒有空白，哪裡都能斷；拉丁字與西里爾字要在空白處斷，不然一個單字
     * 被劈成兩半。所以規則是：遇到空白記下「這裡可以斷」，遇到中日韓的字則
     * 前後都可以斷；一行滿了就退回最近一個可以斷的地方。
     */
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
            if (wide(cp) && line.length() > 0) {
                lastBreak = line.length();
            }
            line.appendCodePoint(cp);
            if (cp == ' ') {
                lastBreak = line.length();
            } else if (wide(cp)) {
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

    /** 中日韓的字（含全形標點）：前後都可以折行。 */
    private static boolean wide(int cp) {
        return (cp >= 0x2E80 && cp <= 0x9FFF) || (cp >= 0xAC00 && cp <= 0xD7AF)
                || (cp >= 0xF900 && cp <= 0xFAFF) || (cp >= 0xFF00 && cp <= 0xFFEF)
                || (cp >= 0x3000 && cp <= 0x30FF);
    }

    /** 點在不在這個矩形裡。 */
    public static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
