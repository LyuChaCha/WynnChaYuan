package com.wynnchayuan.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Canvas;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 把 {@link Canvas} 的繪製動作畫到遊戲畫面上。
 *
 * <h2>為什麼先把座標放大</h2>
 * 遊戲的介面座標是「GUI 像素」，介面縮放是 3 的時候一個 GUI 像素是螢幕上三個點。
 * 圓角與髮絲線要畫到那三個點的精度，所以這裡一開始就把座標系縮小成<b>螢幕上的點</b>
 * （{@link #begin}），之後每個動作把傳進來的 GUI 像素乘回去、取整數再畫。
 * 字還是照原本的大小畫——畫字的時候再把座標系放大回去。
 *
 * <h2>圓角怎麼來的</h2>
 * {@code GuiGraphics} 只會畫矩形與貼圖。圓角是先算一張白色圓形的遮罩
 * （{@link Icons#disc}），當貼圖註冊起來，畫的時候四個角各貼一個象限、染成要的顏色，
 * 中間用矩形補滿。遮罩照實際要畫的點數算，一對一貼上去，所以邊是平滑的。
 * 同一個半徑只算一次。
 */
final class GuiCanvas implements Canvas {

    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID, "textures/gui/icon.png");

    /** 遮罩貼圖。鍵是「哪一種形狀＋尺寸」，整個遊戲期間共用。 */
    private static final Map<String, Identifier> MASKS = new HashMap<>();
    private static final Map<String, DynamicTexture> LIVE = new HashMap<>();
    private static int serial;

    private final GuiGraphics g;
    private final Font font;
    private final int scale;
    private String lang;

    GuiCanvas(GuiGraphics g, Font font, int guiScale) {
        this.g = g;
        this.font = font;
        this.scale = Math.max(1, guiScale);
        this.lang = family(T.pinnedLanguage() != null ? T.pinnedLanguage()
                : Minecraft.getInstance().getLanguageManager().getSelected());
    }

    // ------------------------------------------------------------ 字型
    //
    // 設定畫面用隨模組附的 Noto Sans，不用原版的點陣字（見 tools/build-ui-fonts.py）。
    // 字型定義照「語言」與「一個字點畫成螢幕上幾個點」各一份：TTF 字是先畫成點陣再
    // 貼上去的，畫的解析度跟實際貼出來的大小一比一時邊緣才乾淨。

    /** 有哪幾種倍率的字型定義。跟 build-ui-fonts.py 的 DOTS 要一致。 */
    private static final int[] DOTS = {2, 3, 4, 6};
    private static final java.util.Map<String, Style> STYLES = new HashMap<>();

    /** 這個語言用哪一組字型定義；不是中日韓的都用拉丁那一組。 */
    private static String family(String code) {
        if (code == null) {
            return "latin";
        }
        return switch (code) {
            case "zh_tw", "zh_hk", "lzh" -> "zh_tw";
            case "zh_cn" -> "zh_cn";
            case "ja_jp" -> "ja_jp";
            case "ko_kr" -> "ko_kr";
            default -> "latin";
        };
    }

    /** 要畫成每個字點 {@code dots} 個螢幕點時，用哪一份定義：剛好的那份，沒有就挑大一級的。 */
    private Style style(int dots) {
        int pick = DOTS[DOTS.length - 1];
        for (int d : DOTS) {
            if (d >= dots) {
                pick = d;
                break;
            }
        }
        String key = lang + "_" + pick;
        return STYLES.computeIfAbsent(key, k -> Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID, "ui/" + k))));
    }

    @Override
    public void language(String code) {
        this.lang = family(code);
    }

    @Override
    public void head(String minecraftName, float x, float y, float size) {
        PlayerHeads.draw(g, minecraftName, p(x), p(y), Math.max(8, p(size)));
    }

    /** 把座標系換成螢幕上的點。畫完要叫 {@link #end}。 */
    void begin() {
        g.pose().pushMatrix();
        g.pose().scale(1f / scale, 1f / scale);
    }

    void end() {
        g.pose().popMatrix();
    }

    private int p(float v) {
        return Math.round(v * scale);
    }

    @Override
    public float px() {
        return 1f / scale;
    }

    @Override
    public void fill(float x0, float y0, float x1, float y1, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        int ax = p(x0);
        int ay = p(y0);
        int bx = p(x1);
        int by = p(y1);
        if (bx > ax && by > ay) {
            g.fill(ax, ay, bx, by, argb);
        }
    }

    @Override
    public void round(float x, float y, float w, float h, float r, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        int ax = p(x);
        int ay = p(y);
        int bx = p(x + w);
        int by = p(y + h);
        int pw = bx - ax;
        int ph = by - ay;
        if (pw <= 0 || ph <= 0) {
            return;
        }
        int rr = Math.min(p(r), Math.min(pw, ph) / 2);
        if (rr < 2) {
            g.fill(ax, ay, bx, by, argb);
            return;
        }
        Identifier tex = mask("d" + rr, rr * 2, rr * 2, () -> Icons.disc(rr, 0));
        corners(tex, ax, ay, bx, by, rr, argb);
        g.fill(ax + rr, ay, bx - rr, by, argb);
        if (by - rr > ay + rr) {
            g.fill(ax, ay + rr, ax + rr, by - rr, argb);
            g.fill(bx - rr, ay + rr, bx, by - rr, argb);
        }
    }

    @Override
    public void ring(float x, float y, float w, float h, float r, float t, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        int ax = p(x);
        int ay = p(y);
        int bx = p(x + w);
        int by = p(y + h);
        int pw = bx - ax;
        int ph = by - ay;
        int tt = Math.max(1, p(t));
        if (pw <= tt * 2 || ph <= tt * 2) {
            if (pw > 0 && ph > 0) {
                g.fill(ax, ay, bx, by, argb);
            }
            return;
        }
        int rr = Math.min(p(r), Math.min(pw, ph) / 2);
        if (rr <= tt) {
            rr = 0;
        } else {
            int radius = rr;
            Identifier tex = mask("r" + rr + "/" + tt, rr * 2, rr * 2,
                                  () -> Icons.disc(radius, radius - tt));
            corners(tex, ax, ay, bx, by, rr, argb);
        }
        g.fill(ax + rr, ay, bx - rr, ay + tt, argb);
        g.fill(ax + rr, by - tt, bx - rr, by, argb);
        int top = ay + Math.max(rr, tt);
        int bottom = by - Math.max(rr, tt);
        if (bottom > top) {
            g.fill(ax, top, ax + tt, bottom, argb);
            g.fill(bx - tt, top, bx, bottom, argb);
        }
    }

    /** 四個角各貼圓形遮罩的一個象限。 */
    private void corners(Identifier tex, int ax, int ay, int bx, int by, int r, int argb) {
        int size = r * 2;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, ax, ay, 0f, 0f, r, r, size, size, argb);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, bx - r, ay, (float) r, 0f, r, r, size, size, argb);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, ax, by - r, 0f, (float) r, r, r, size, size, argb);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, bx - r, by - r, (float) r, (float) r, r, r,
               size, size, argb);
    }

    @Override
    public void icon(Icons.Icon icon, float x, float y, float size, int argb) {
        int s = Math.max(4, p(size));
        Identifier tex = mask("i" + icon.name() + s, s, s, () -> Icons.mask(icon, s));
        g.blit(RenderPipelines.GUI_TEXTURED, tex, p(x), p(y), 0f, 0f, s, s, s, s, argb);
    }

    @Override
    public void text(String text, float x, float y, int argb) {
        draw(text, x, y, argb, scale);
    }

    @Override
    public void text(String text, float x, float y, int argb, float wanted) {
        draw(text, x, y, argb, Math.max(1, Math.round(scale * wanted)));
    }

    @Override
    public float scale(float wanted) {
        return Math.max(1, Math.round(scale * wanted)) / (float) scale;
    }

    private void draw(String text, float x, float y, int argb, int dots) {
        if (text == null || text.isEmpty() || (argb >>> 24) < 4) {
            return;                            // 透明到這樣就是看不見，原版也是不畫
        }
        g.pose().pushMatrix();
        g.pose().translate(p(x), p(y));
        g.pose().scale(dots, dots);
        g.drawString(font, Component.literal(text).withStyle(style(dots)), 0, 0, argb, false);
        g.pose().popMatrix();
    }

    @Override
    public int width(String text) {
        return text == null || text.isEmpty() ? 0
                : font.width(Component.literal(text).withStyle(style(scale)));
    }

    @Override
    public void clip(float x0, float y0, float x1, float y1) {
        g.enableScissor(p(x0), p(y0), Math.max(p(x0), p(x1)), Math.max(p(y0), p(y1)));
    }

    @Override
    public void unclip() {
        g.disableScissor();
    }

    @Override
    public void logo(float x, float y, float size) {
        int s = Math.max(8, p(size));
        Identifier tex = MASKS.get("logo" + s);
        if (tex == null) {
            tex = shrunkLogo(s);
            MASKS.put("logo" + s, tex);
        }
        if (tex == ICON) {
            g.blit(RenderPipelines.GUI_TEXTURED, ICON, p(x), p(y), 0f, 0f, s, s, 256, 256, 256, 256);
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, p(x), p(y), 0f, 0f, s, s, s, s);
        }
    }

    /**
     * 把圖示縮成要畫的大小。
     *
     * <p>原圖 256×256。直接叫遊戲縮的話它是「跳著取點」，縮到幾十個點會鋸齒；
     * 這裡把每個目的點涵蓋到的原圖範圍平均起來，縮一次存起來。
     * 讀不到原圖就退回直接貼——頂多鋸齒，不會沒有圖示。
     */
    private static Identifier shrunkLogo(int size) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(ICON);
             NativeImage src = NativeImage.read(in)) {
            int sw = src.getWidth();
            int sh = src.getHeight();
            NativeImage out = new NativeImage(size, size, false);
            for (int y = 0; y < size; y++) {
                int y0 = y * sh / size;
                int y1 = Math.max(y0 + 1, (y + 1) * sh / size);
                for (int x = 0; x < size; x++) {
                    int x0 = x * sw / size;
                    int x1 = Math.max(x0 + 1, (x + 1) * sw / size);
                    long a = 0;
                    long r = 0;
                    long gr = 0;
                    long b = 0;
                    for (int yy = y0; yy < y1; yy++) {
                        for (int xx = x0; xx < x1; xx++) {
                            int c = src.getPixel(xx, yy);
                            int ca = c >>> 24;
                            a += ca;
                            r += ((c >> 16) & 0xFF) * ca;
                            gr += ((c >> 8) & 0xFF) * ca;
                            b += (c & 0xFF) * ca;
                        }
                    }
                    int n = (x1 - x0) * (y1 - y0);
                    int alpha = (int) (a / n);
                    int colour = a == 0 ? 0
                            : ((int) (r / a) << 16) | ((int) (gr / a) << 8) | (int) (b / a);
                    out.setPixel(x, y, (alpha << 24) | colour);
                }
            }
            return register("logo" + size, out);
        } catch (Exception e) {
            return ICON;
        }
    }

    @Override
    public void svSquare(float x, float y, float w, float h, float hue) {
        int pw = Math.max(2, p(x + w) - p(x));
        int ph = Math.max(2, p(y + h) - p(y));
        int step = Math.round(hue * 2);        // 半度一格：拖色相條時不必每一幀都重算
        Identifier tex = live("sv", pw, ph, step, image -> {
            for (int yy = 0; yy < ph; yy++) {
                float v = 1f - yy / (float) (ph - 1);
                for (int xx = 0; xx < pw; xx++) {
                    image.setPixel(xx, yy, Ui.hsv(step / 2f, xx / (float) (pw - 1), v));
                }
            }
        });
        g.blit(RenderPipelines.GUI_TEXTURED, tex, p(x), p(y), 0f, 0f, pw, ph, pw, ph);
    }

    @Override
    public void hueBar(float x, float y, float w, float h) {
        int pw = Math.max(2, p(x + w) - p(x));
        int ph = Math.max(1, p(y + h) - p(y));
        Identifier tex = live("hue", pw, ph, 0, image -> {
            for (int xx = 0; xx < pw; xx++) {
                int c = Ui.hsv(360f * xx / pw, 1f, 1f);
                for (int yy = 0; yy < ph; yy++) {
                    image.setPixel(xx, yy, c);
                }
            }
        });
        g.blit(RenderPipelines.GUI_TEXTURED, tex, p(x), p(y), 0f, 0f, pw, ph, pw, ph);
    }

    // ------------------------------------------------------------ 貼圖

    private interface MaskSource {
        byte[] get();
    }

    /** 白色、只有透明度的遮罩貼圖；畫的時候再染色。 */
    private static Identifier mask(String key, int w, int h, MaskSource source) {
        Identifier id = MASKS.get(key);
        if (id != null) {
            return id;
        }
        byte[] cover = source.get();
        NativeImage image = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setPixel(x, y, ((cover[y * w + x] & 0xFF) << 24) | 0xFFFFFF);
            }
        }
        id = register(key, image);
        MASKS.put(key, id);
        return id;
    }

    private static Identifier register(String key, NativeImage image) {
        Identifier id = Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID, "ui/" + (serial++));
        DynamicTexture texture = new DynamicTexture(() -> "wynnchayuan-ui-" + key, image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    private interface Painter {
        void paint(NativeImage image);
    }

    private static final Map<String, int[]> LIVE_STATE = new HashMap<>();

    /**
     * 內容會變的貼圖（色盤那一塊）。尺寸沒變就在原地重畫、重新上傳；
     * 尺寸變了（改了視窗大小或介面縮放）才丟掉重做。
     */
    private static Identifier live(String key, int w, int h, int version, Painter painter) {
        int[] state = LIVE_STATE.get(key);
        DynamicTexture texture = LIVE.get(key);
        Identifier id = MASKS.get("live:" + key);
        if (texture == null || state == null || state[0] != w || state[1] != h) {
            if (id != null) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
            NativeImage image = new NativeImage(w, h, false);
            painter.paint(image);
            id = Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID, "ui/" + (serial++));
            texture = new DynamicTexture(() -> "wynnchayuan-ui-" + key, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            LIVE.put(key, texture);
            MASKS.put("live:" + key, id);
            LIVE_STATE.put(key, new int[] {w, h, version});
            return id;
        }
        if (state[2] != version) {
            NativeImage image = texture.getPixels();
            if (image != null) {
                painter.paint(image);
                texture.upload();
            }
            state[2] = version;
        }
        return id;
    }
}
