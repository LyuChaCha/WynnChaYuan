package com.wynnchayuan.client;

import com.wynnchayuan.client.ui.Canvas;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 測試用的畫布：把 {@link Canvas} 的動作畫進一張記憶體裡的圖。
 *
 * <h2>跟遊戲裡差在哪</h2>
 * 形狀是同一套算法（圖示直接用 {@link Icons#mask}），字用的也是<b>同一批字型檔</b>
 * （{@code assets/wynnchayuan/font/ui/*.ttf}），大小與遊戲裡的字型定義一樣是 9。
 * 所以版面寬度可信，字的長相也八九不離十；差別在遊戲是用 FreeType 畫的，
 * 筆畫的粗細與邊緣會有一點點不同。
 *
 * <p>字型檔裡沒有的字，遊戲會退回原版的點陣字；這裡用系統字型代替，並且記在
 * {@link #missing} 裡——測試用它來抓「加了介面字串卻沒重裁字型」。
 */
final class AwtCanvas implements Canvas {

    /** 跟字型定義裡的 size 一樣。 */
    private static final float SIZE = 9f;
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private static final Map<String, Font> FILES = new HashMap<>();
    private static final Map<String, float[]> ADVANCES = new HashMap<>();

    /** 這次畫了哪些字型檔裡沒有的字。 */
    static final Set<Integer> missing = new TreeSet<>();

    final BufferedImage image;
    private final Graphics2D g;
    private final int scale;
    private final Deque<Shape> clips = new ArrayDeque<>();
    private List<Font> chain;
    private final Font fallback = new Font(Font.SANS_SERIF, Font.PLAIN, 9);

    AwtCanvas(int guiW, int guiH, int scale, String lang) {
        this.scale = scale;
        this.image = new BufferedImage(guiW * scale, guiH * scale, BufferedImage.TYPE_INT_ARGB);
        this.g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                           RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        language(lang);
    }

    // ------------------------------------------------------------ 字型

    private static Font file(String name) {
        return FILES.computeIfAbsent(name, n -> {
            try (InputStream in = AwtCanvas.class.getResourceAsStream(
                    "/assets/wynnchayuan/font/ui/" + n + ".ttf")) {
                return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(SIZE);
            } catch (Exception e) {
                throw new IllegalStateException("讀不到字型 " + n + "：" + e);
            }
        });
    }

    /** 跟 tools/build-ui-fonts.py 的 definition() 同一個順序：自己的，再來是別的語言借字用的。 */
    static List<Font> chainFor(String lang) {
        String own = switch (lang == null ? "" : lang) {
            case "zh_tw" -> "tc";
            case "zh_cn" -> "sc";
            case "ja_jp" -> "jp";
            case "ko_kr" -> "kr";
            default -> "latin";
        };
        List<Font> out = new ArrayList<>();
        out.add(file(own));
        out.add(file("sym"));
        for (String other : new String[] {"tc", "sc", "jp", "kr"}) {
            if (!other.equals(own)) {
                out.add(file(other + "_x"));
            }
        }
        return out;
    }

    /** 這個語言的字型鏈畫不畫得出這個字。 */
    static boolean covers(String lang, int cp) {
        for (Font f : chainFor(lang)) {
            if (f.canDisplay(cp)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void language(String lang) {
        this.chain = chainFor(lang);
    }

    private Font fontFor(int cp) {
        for (Font f : chain) {
            if (f.canDisplay(cp)) {
                return f;
            }
        }
        if (cp > ' ') {
            missing.add(cp);
        }
        return fallback;
    }

    private static float advance(Font f, int cp) {
        float[] table = ADVANCES.computeIfAbsent(f.getFontName() + "/" + f.getSize2D(),
                k -> new float[0x10000]);
        if (cp < table.length && table[cp] != 0) {
            return table[cp];
        }
        float adv = (float) f.getStringBounds(new String(Character.toChars(cp)), FRC).getWidth();
        if (cp < table.length) {
            table[cp] = adv == 0 ? 0.0001f : adv;
        }
        return adv;
    }

    @Override
    public int width(String text) {
        if (text == null) {
            return 0;
        }
        float w = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            w += advance(fontFor(cp), cp);
        }
        return (int) Math.ceil(w);
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
        // 遊戲把 TTF 字的基線放在這一行往下 7 的地方，跟原版點陣字同一條線
        float baseline = p(y) + 7f * scale * k;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Font f = fontFor(cp);
            g.setFont(f.deriveFont(f.getSize2D() * scale * k));
            g.drawString(new String(Character.toChars(cp)), cx, baseline);
            cx += advance(f, cp) * scale * k;
        }
    }

    // ------------------------------------------------------------ 形狀

    /** 墊一張假的遊戲畫面：幾塊模糊的顏色，讓半透明的視窗底下有東西。 */
    void backdrop() {
        int w = image.getWidth();
        int h = image.getHeight();
        g.setColor(new Color(0x1B3550));
        g.fillRect(0, 0, w, h);
        java.awt.geom.Point2D centre = new java.awt.geom.Point2D.Float(w * 0.75f, h * 0.4f);
        g.setPaint(new java.awt.RadialGradientPaint(centre, w * 0.35f, new float[] {0f, 1f},
                new Color[] {new Color(0x8C7438), new Color(140, 116, 56, 0)}));
        g.fillRect(0, 0, w, h);
        centre = new java.awt.geom.Point2D.Float(w * 0.25f, h * 0.7f);
        g.setPaint(new java.awt.RadialGradientPaint(centre, w * 0.4f, new float[] {0f, 1f},
                new Color[] {new Color(0x2C5A3E), new Color(44, 90, 62, 0)}));
        g.fillRect(0, 0, w, h);
        // 原版在畫面底下墊的那層暗色
        g.setPaint(new Color(8, 12, 18, 140));
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
    public void head(String minecraftName, float x, float y, float size) {
        // 沒有皮膚可抓：照名字給一個固定的顏色，看得出每個人一格
        int hash = minecraftName == null ? 0 : minecraftName.hashCode();
        g.setColor(new Color(Ui.hsv(Math.floorMod(hash, 360), 0.45f, 0.8f), false));
        g.fill(new Rectangle2D.Float(p(x), p(y), p(x + size) - p(x), p(y + size) - p(y)));
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
        g.dispose();
    }
}
