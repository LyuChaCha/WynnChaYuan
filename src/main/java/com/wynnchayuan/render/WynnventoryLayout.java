package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

/**
 * Wynnventory 的側邊框（價格、意象清單）放在哪裡、縮多小，以及物品說明怎麼讓開它。
 *
 * <h2>回報（2026-10-19）</h2>
 * 未鑑定物品的「可能性」清單很長，價格框跟著很長，整個疊到物品說明上。原因有三：
 * <ol>
 *   <li>它照<b>英文</b>的物品說明算說明框多寬（它的事件比我們先跑，拿到的還是原文），
 *       畫面上卻是我們就地換好、附了原文名字的中文，寬得多。</li>
 *   <li>它用固定位置畫自己的框，可是 Wynntils 的「tooltip 縮放」把<b>所有</b>太高的 tooltip
 *       都改成自己的位置與縮放（它只在別人沒指定位置時讓開，而那個事件根本拿不到別人指定的位置），
 *       價格框於是跑到滑鼠旁邊、上半截出了畫面。</li>
 *   <li>使用者開著它的「固定在左側」（anchorTooltips）：框貼在畫面左邊，而游標在左半邊的時候
 *       物品說明也在那裡，兩個疊在一起。</li>
 * </ol>
 *
 * <h2>做法</h2>
 * <ul>
 *   <li>它的 {@code RenderUtils.drawTooltip} 由 {@link #draw} 接手（mixin 擋在開頭），
 *       畫的那一刻掛著 {@link #drawing}，Wynntils 的縮放那一步看到就跳過
 *      （{@code WynntilsFittingMixin}），位置與縮放照這裡算的。</li>
 *   <li>固定在左側：貼畫面左邊、上下置中，太高就整個縮到剛好塞進畫面。</li>
 *   <li>沒固定：照<b>實際畫出來的</b>說明框寬度挑邊，右邊優先、再左邊，都放不下就挑大的那邊縮小。</li>
 *   <li>物品說明（它在價格框之後才定位）蓋到價格框時往右讓開，見 {@link #shift}。</li>
 * </ul>
 */
public final class WynnventoryLayout {

    private WynnventoryLayout() {
    }

    /** 它的價格框與說明框之間的距離。 */
    private static final int GAP = 18;

    /** 離畫面邊緣至少留這麼多。 */
    private static final int EDGE = 4;

    /** 原版 tooltip 的底色與框線比文字區多出來的量（上下左右各一份）。 */
    private static final int PAD = 4;

    /** 第一列（標題）下面原版多空的 2 px，加上上下兩份 PAD。 */
    private static final int EXTRA_H = 2 + PAD * 2;

    private static final float MIN_SCALE = 0.45f;

    /** 正在畫它的框：Wynntils 的 tooltip 縮放這時候不要插手。 */
    private static boolean drawing;

    public static boolean drawing() {
        return drawing;
    }

    /**
     * 接手畫一個側邊框。
     *
     * @param vanilla 物品原本的說明（它拿到的是原文）
     * @param custom  它要畫的那幾行
     * @return 畫了就回 {@code true}；回 {@code false} 交給它原本的方法
     */
    public static boolean draw(GuiGraphics graphics, int mouseX, int mouseY,
                               List<Component> vanilla, List<Component> custom) {
        try {
            if (graphics == null || custom == null || custom.isEmpty() || vanilla == null) {
                return false;
            }
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            Prepared prepared = lastPrice != null && lastPrice.same(custom) ? lastPrice
                    : (lastPrice = prepare(custom,
                            com.wynnchayuan.translate.WynnventoryBridge.lines(custom), font));
            List<ClientTooltipComponent> price = prepared.comps();
            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();
            int pw = prepared.w();
            int ph = prepared.h();

            float[] spot;
            if (anchored()) {
                spot = anchoredSpot(screenH, pw, ph);
            } else {
                int stamp = drawnStamp();
                Prepared drawn = lastDrawn != null && lastDrawn.same(vanilla, stamp) ? lastDrawn
                        : (lastDrawn = prepare(vanilla, stamp, shown(vanilla), font));
                int vw = drawn.w();
                int vh = drawn.h();
                float fit = Math.min(wynntilsScale(), fitScale(vh, screenH));
                int svw = (int) (vw * fit);
                int svh = (int) (vh * fit);
                Vector2ic at = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                        screenW, screenH, mouseX, mouseY, svw, svh);
                spot = place(screenW, screenH, at.x(), at.y(), svw, pw, ph);
            }
            if (spot == null) {
                return false;
            }
            float scale = spot[2];
            final int fx = Math.round(spot[0] / scale);
            final int fy = Math.round(spot[1] / scale);
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            drawing = true;
            try {
                graphics.renderTooltip(font, price, fx, fy,
                        (sw, sh, mx, my, w, h) -> new Vector2i(fx, fy), null);
            } finally {
                drawing = false;
                graphics.pose().popMatrix();
            }
            // 物品說明與「另開面板」的譯文面板都要讓開它，見 #shift 與 WynnventoryBox
            WynnventoryBox.note(Math.round(spot[0] - PAD * scale), Math.round(spot[1] - PAD * scale),
                    Math.round((pw + PAD * 2) * scale), Math.round((ph + EXTRA_H) * scale),
                    System.nanoTime());
            return true;
        } catch (Throwable t) {
            drawing = false;
            return false;
        }
    }

    /**
     * 固定在左側：貼畫面左邊、上下置中，太高就縮。
     *
     * @return {文字區左上角 x, y, 縮放}（畫面座標）
     */
    static float[] anchoredSpot(int screenH, int pw, int ph) {
        float scale = fitScale(ph, screenH);
        float x = EDGE + PAD * scale;
        float y = (screenH - (ph + EXTRA_H) * scale) / 2f + PAD * scale;
        return new float[] {x, y, scale};
    }

    /**
     * 沒固定時放哪、縮多少。
     *
     * @param vx  說明框左上角（畫面座標）
     * @param svw 說明框實際的寬度（已乘上縮放）
     * @return {文字區左上角 x, y, 縮放}（畫面座標）；縮得比 {@link #MIN_SCALE} 還小時回 {@code null}
     */
    static float[] place(int screenW, int screenH, int vx, int vy, int svw, int pw, int ph) {
        int right = screenW - EDGE - (vx + svw + GAP);
        int left = vx - GAP - EDGE;
        int boxW = pw + PAD * 2;
        float scale = fitScale(ph, screenH);
        boolean onRight;
        if (boxW * scale <= right) {
            onRight = true;
        } else if (boxW * scale <= left) {
            onRight = false;
        } else {
            onRight = right >= left;
            scale = Math.min(scale, Math.max(right, left) / (float) boxW);
        }
        if (scale < MIN_SCALE) {
            return null;
        }
        float x = onRight ? vx + svw + GAP + PAD * scale : vx - GAP - (pw + PAD) * scale;
        float top = EDGE + PAD * scale;
        float bottom = screenH - EDGE - (ph + EXTRA_H - PAD) * scale;
        float y = Math.max(top, Math.min(vy, bottom));
        return new float[] {x, y, scale};
    }

    /**
     * 物品說明要定位了：蓋到這一幀它的框的話，往右挪到框的右邊（挪得下才挪）。
     *
     * @param delegate 原本要用的定位方式（Wynntils 的縮放或原版）；{@code null} 是原版
     * @param scale    那個定位方式畫的時候的縮放（Wynntils 縮了就不是 1）
     * @return 要換上的定位方式；不必換時回 {@code null}
     */
    public static ClientTooltipPositioner shift(ClientTooltipPositioner delegate, float scale) {
        int[] box = WynnventoryBox.current();
        if (box == null || drawing || delegate instanceof Shifted) {
            return null;
        }
        ClientTooltipPositioner base = delegate == null ? DefaultTooltipPositioner.INSTANCE : delegate;
        return new Shifted(base, box, scale <= 0 ? 1f : scale);
    }

    /** 見 {@link #shift}。 */
    private record Shifted(ClientTooltipPositioner base, int[] box, float scale)
            implements ClientTooltipPositioner {

        @Override
        public Vector2ic positionTooltip(int screenW, int screenH, int mouseX, int mouseY,
                                         int w, int h) {
            Vector2ic at = base.positionTooltip(screenW, screenH, mouseX, mouseY, w, h);
            float[] moved = WynnventoryLayout.moved(at.x() * scale, at.y() * scale, w * scale, h * scale,
                    box, screenW, scale);
            return moved == null ? at : new Vector2i(Math.round(moved[0] / scale), at.y());
        }
    }

    /**
     * 物品說明（文字區左上角 x, y、寬高，畫面座標）蓋到框的話挪到哪。
     *
     * @return {新的 x}；不必挪或挪不下時回 {@code null}
     */
    static float[] moved(float x, float y, float w, float h, int[] box, int screenW, float scale) {
        float pad = PAD * scale;
        boolean hits = x - pad < box[0] + box[2] && box[0] < x + w + pad
                && y - pad < box[1] + box[3] && box[1] < y + h + pad;
        if (!hits) {
            return null;
        }
        float nx = box[0] + box[2] + 2 + pad;
        if (nx + w + pad > screenW - EDGE) {
            return null;
        }
        return new float[] {nx};
    }

    /** 它的設定「固定在左側」。讀不到就當沒開。 */
    private static boolean anchored() {
        try {
            if (anchorGet == null) {
                Class<?> config = Class.forName("com.wynnventory.core.config.ModConfig");
                configGet = config.getMethod("getInstance");
                settingsGet = config.getMethod("getTooltipSettings");
                anchorGet = settingsGet.getReturnType().getMethod("isAnchorTooltips");
            }
            Object settings = settingsGet.invoke(configGet.invoke(null));
            return Boolean.TRUE.equals(anchorGet.invoke(settings));
        } catch (Throwable t) {
            return false;
        }
    }

    private static java.lang.reflect.Method configGet;
    private static java.lang.reflect.Method settingsGet;
    private static java.lang.reflect.Method anchorGet;

    /**
     * 一份框的元件與寬高，照<b>原本那份字</b>記住。
     *
     * <p>指著同一件物品的每一幀，它交過來的都是同一份字：轉成元件、量寬高、翻譯
     * 每幀都重做的話，光這一步就佔渲染取樣的一成（2026-10-19 的 JFR）。
     */
    private record Prepared(List<Component> key, int stamp, List<ClientTooltipComponent> comps,
                            int w, int h) {
        boolean same(List<Component> other) {
            return same(other, 0);
        }

        boolean same(List<Component> other, int otherStamp) {
            return stamp == otherStamp && (key == other || key.equals(other));
        }
    }

    private static Prepared lastPrice;
    private static Prepared lastDrawn;

    private static Prepared prepare(List<Component> key, List<Component> lines, Font font) {
        return prepare(key, 0, lines, font);
    }

    private static Prepared prepare(List<Component> key, int stamp, List<Component> lines, Font font) {
        List<ClientTooltipComponent> comps = components(lines);
        return new Prepared(List.copyOf(key), stamp, comps, width(comps, font), height(comps, font));
    }

    /** 說明框換成什麼樣子取決於語料版本、名稱模式與 Shift：換了就重量一次。 */
    private static int drawnStamp() {
        com.wynnchayuan.translate.TranslationStore store = WynnChaYuan.translations();
        CollectorConfig config = WynnChaYuan.config();
        return java.util.Objects.hash(store == null ? 0 : store.generation(),
                config == null ? null : config.itemNames(), config == null ? null : config.tooltipMode(),
                com.wynnchayuan.listener.RenderListener.shiftHeld());
    }

    /** 就地取代模式下，畫面上的說明是我們換好的那一份。 */
    private static List<Component> shown(List<Component> vanilla) {
        CollectorConfig config = WynnChaYuan.config();
        if (config == null || config.tooltipMode() != CollectorConfig.TooltipMode.REPLACE) {
            return vanilla;
        }
        return com.wynnchayuan.listener.RenderListener.inPlaceLines(vanilla);
    }

    private static List<ClientTooltipComponent> components(List<Component> lines) {
        List<ClientTooltipComponent> out = new ArrayList<>(lines.size());
        for (Component line : lines) {
            out.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        }
        return out;
    }

    private static int width(List<ClientTooltipComponent> lines, Font font) {
        int w = 0;
        for (ClientTooltipComponent c : lines) {
            w = Math.max(w, c.getWidth(font));
        }
        return w;
    }

    private static int height(List<ClientTooltipComponent> lines, Font font) {
        int h = 0;
        for (ClientTooltipComponent c : lines) {
            h += c.getHeight(font);
        }
        return h;
    }

    /** 整個框（含底色、框線與標題下的空隙）比畫面高就縮到剛好塞進去，上下各留 {@link #EDGE}。 */
    static float fitScale(int h, int screenH) {
        float whole = h + EXTRA_H;
        float room = screenH - EDGE * 2;
        return whole > room ? room / whole : 1f;
    }

    /** Wynntils「tooltip 縮放」的設定；讀不到就當 1。 */
    private static float wynntilsScale() {
        try {
            var feature = com.wynntils.core.components.Managers.Feature.getFeatureInstance(
                    com.wynntils.features.tooltips.TooltipFittingFeature.class);
            return feature.isEnabled() ? feature.universalScale.get() : 1f;
        } catch (Throwable t) {
            return 1f;
        }
    }
}
