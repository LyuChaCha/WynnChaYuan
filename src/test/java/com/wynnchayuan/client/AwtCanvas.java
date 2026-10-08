package com.wynnchayuan.client;

import com.wynnchayuan.client.ui.Canvas;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 測試用的畫布：把 {@link Canvas} 的動作畫進一張記憶體裡的圖。
 *
 * <h2>跟遊戲裡差在哪</h2>
 * 形狀是同一套算法（圖示直接用 {@link Icons#mask}），差的是<b>字</b>：這裡沒有遊戲的
 * 字型，所以每個字的寬度是照原版字型<b>估</b>的（英數多半 6、中日韓 9），字本身用
 * 系統字型塞進那一格裡。版面是照寬度排的，所以「放不放得下」的結論跟遊戲裡一致；
 * 字的長相不一樣，這張圖不是用來看字漂不漂亮的。
 */
final class AwtCanvas implements Canvas {

    final BufferedImage image;
    private final Graphics2D g;
    private final int scale;
    private final Deque<Shape> clips = new ArrayDeque<>();
    private final Font latin;
    private final Font wide;

    AwtCanvas(int guiW, int guiH, int scale) {
        this.scale = scale;
        this.image = new BufferedImage(guiW * scale, guiH * scale, BufferedImage.TYPE_INT_ARGB);
        this.g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        this.latin = new Font(Font.SANS_SERIF, Font.BOLD, Math.round(7.4f * scale));
        this.wide = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(8f * scale));
    }

    /** 墊一張假的遊戲畫面：幾塊顏色，讓半透明的視窗底下有東西。 */
    void backdrop() {
        int w = image.getWidth();
        int h = image.getHeight();
        g.setColor(new Color(0x1B3550));
        g.fillRect(0, 0, w, h / 2);
        g.setColor(new Color(0x2C3A2E));
        g.fillRect(0, h / 2, w, h / 2);
        g.setColor(new Color(0x8C7438));
        g.fillRect(w * 7 / 10, h / 6, w / 4, h / 2);
        g.setColor(new Color(0x4A3A2A));
        g.fillRect(w / 12, h / 4, w / 4, h / 2);
        g.setColor(new Color(0x55606A));
        g.fillRect(w / 3, h / 2, w / 3, h / 3);
        // 原版在畫面底下墊的那層暗色
        g.setColor(new Color(8, 12, 18, 150));
        g.fillRect(0, 0, w, h);
    }

    private static Color colour(int argb) {
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24);
    }

    private float p(float v) {
        return Math.round(v * scale);
    }

    @Override
    public float px() {
        return 1f / scale;
    }

    @Override
    public void fill(float x0, float y0, float x1, float y1, int argb) {
        if ((argb >>> 24) == 0 || p(x1) <= p(x0) || p(y1) <= p(y0)) {
            return;
        }
        g.setColor(colour(argb));
        g.fill(new Rectangle2D.Float(p(x0), p(y0), p(x1) - p(x0), p(y1) - p(y0)));
    }

    private Shape shape(float x, float y, float w, float h, float r) {
        float ax = p(x);
        float ay = p(y);
        float pw = p(x + w) - ax;
        float ph = p(y + h) - ay;
        float rr = Math.min(p(r), Math.min(pw, ph) / 2f);
        return new RoundRectangle2D.Float(ax, ay, pw, ph, rr * 2, rr * 2);
    }

    @Override
    public void round(float x, float y, float w, float h, float r, int argb) {
        if ((argb >>> 24) == 0 || w <= 0 || h <= 0) {
            return;
        }
        g.setColor(colour(argb));
        g.fill(shape(x, y, w, h, r));
    }

    @Override
    public void ring(float x, float y, float w, float h, float r, float t, int argb) {
        if ((argb >>> 24) == 0 || w <= 0 || h <= 0) {
            return;
        }
        float tt = Math.max(1, Math.round(t * scale)) / (float) scale;
        Area outer = new Area(shape(x, y, w, h, r));
        if (w > tt * 2 && h > tt * 2) {
            outer.subtract(new Area(shape(x + tt, y + tt, w - tt * 2, h - tt * 2,
                                          Math.max(0, r - tt))));
        }
        g.setColor(colour(argb));
        g.fill(outer);
    }

    @Override
    public void icon(Icons.Icon icon, float x, float y, float size, int argb) {
        int s = Math.max(4, Math.round(size * scale));
        byte[] mask = Icons.mask(icon, s);
        int ox = Math.round(x * scale);
        int oy = Math.round(y * scale);
        int base = argb >>> 24;
        for (int yy = 0; yy < s; yy++) {
            for (int xx = 0; xx < s; xx++) {
                int a = (mask[yy * s + xx] & 0xFF) * base / 255;
                if (a == 0) {
                    continue;
                }
                g.setColor(new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, a));
                g.fillRect(ox + xx, oy + yy, 1, 1);
            }
        }
    }

    @Override
    public void text(String text, float x, float y, int argb) {
        draw(text, x, y, argb, 1f);
    }

    @Override
    public void text(String text, float x, float y, int argb, float wanted) {
        draw(text, x, y, argb, scale(wanted));
    }

    @Override
    public float scale(float wanted) {
        return Math.max(1, Math.round(scale * wanted)) / (float) scale;
    }

    private void draw(String text, float x, float y, int argb, float k) {
        if (text == null || text.isEmpty() || (argb >>> 24) < 4) {
            return;
        }
        g.setColor(colour(argb));
        float cx = p(x);
        float baseline = p(y) + 7f * scale * k;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            int adv = advance(cp);
            Font f = (wide(cp) ? wide : latin);
            f = f.deriveFont(f.getSize2D() * k);
            String s = new String(Character.toChars(cp));
            if (!f.canDisplay(cp)) {
                f = wide.deriveFont(wide.getSize2D() * k);
            }
            g.setFont(f);
            // 塞進估出來的那一格：系統字型比較寬的字（M、W）壓窄，不讓它吃到下一個字
            float real = (float) g.getFontMetrics().stringWidth(s);
            float cell = (adv - 1) * scale * k;
            if (real > cell && real > 0) {
                var old = g.getTransform();
                g.translate(cx, baseline);
                g.scale(cell / real, 1);
                g.drawString(s, 0, 0);
                g.setTransform(old);
            } else {
                g.drawString(s, cx + (cell - real) / 2f, baseline);
            }
            cx += adv * scale * k;
        }
    }

    @Override
    public int width(String text) {
        if (text == null) {
            return 0;
        }
        int w = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            w += advance(cp);
        }
        return w;
    }

    private static boolean wide(int cp) {
        return (cp >= 0x2E80 && cp <= 0x9FFF) || (cp >= 0xAC00 && cp <= 0xD7AF)
                || (cp >= 0xF900 && cp <= 0xFAFF) || (cp >= 0xFF00 && cp <= 0xFFEF)
                || (cp >= 0x3000 && cp <= 0x30FF);
    }

    /** 原版字型裡這個字佔幾個 GUI 像素（含字與字之間那一格）。估的，見類別說明。 */
    static int advance(int cp) {
        if (wide(cp)) {
            return 9;
        }
        return switch (cp) {
            case ' ' -> 4;
            case 'i', '!', '.', ',', ':', ';', '\'', '|' -> 2;
            case 'l', '`' -> 3;
            case 't', 'I', '[', ']', '(', ')', '{', '}', '"', '*' -> 4;
            case 'f', 'k', '<', '>' -> 5;
            case '@', '~' -> 7;
            case '…' -> 8;
            case '·' -> 3;
            case '—', '→', '✔', '✘', '●' -> 8;
            case '«', '»' -> 6;
            default -> 6;
        };
    }

    @Override
    public void clip(float x0, float y0, float x1, float y1) {
        clips.push(g.getClip() == null ? new Rectangle2D.Float(0, 0, image.getWidth(),
                                                                image.getHeight()) : g.getClip());
        Area next = new Area(clips.peek());
        next.intersect(new Area(new Rectangle2D.Float(p(x0), p(y0), Math.max(0, p(x1) - p(x0)),
                                                      Math.max(0, p(y1) - p(y0)))));
        g.setClip(next);
    }

    @Override
    public void unclip() {
        g.setClip(clips.pop());
    }

    @Override
    public void logo(float x, float y, float size) {
        // 沒有貼圖管理員：畫一塊主題色的圓角方塊佔位，位置與大小是對的
        g.setColor(new Color(0x6FA8D8));
        g.fill(shape(x, y, size, size, size / 5f));
        g.setColor(new Color(0x0B1420));
        g.fill(shape(x + size * 0.28f, y + size * 0.28f, size * 0.44f, size * 0.44f, size / 10f));
    }

    @Override
    public void svSquare(float x, float y, float w, float h, float hue) {
        int pw = Math.round(p(x + w) - p(x));
        int ph = Math.round(p(y + h) - p(y));
        for (int yy = 0; yy < ph; yy++) {
            for (int xx = 0; xx < pw; xx++) {
                g.setColor(new Color(Ui.hsv(hue, xx / (float) (pw - 1),
                                            1f - yy / (float) (ph - 1)), false));
                g.fillRect(Math.round(p(x)) + xx, Math.round(p(y)) + yy, 1, 1);
            }
        }
    }

    @Override
    public void hueBar(float x, float y, float w, float h) {
        int pw = Math.round(p(x + w) - p(x));
        int ph = Math.round(p(y + h) - p(y));
        for (int xx = 0; xx < pw; xx++) {
            g.setColor(new Color(Ui.hsv(360f * xx / pw, 1f, 1f), false));
            g.fillRect(Math.round(p(x)) + xx, Math.round(p(y)), 1, ph);
        }
    }

    void done() {
        g.setComposite(AlphaComposite.SrcOver);
        g.dispose();
    }
}
