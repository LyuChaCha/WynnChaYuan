package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

/**
 * Wynnventory 的側邊框（價格、意象清單）放在哪裡、縮多小。
 *
 * <h2>回報（2026-10-19）</h2>
 * 未鑑定物品的「可能性」清單很長，價格框跟著很長；它整個疊到物品說明上，蓋住一半的字。
 * 兩個原因：
 * <ol>
 *   <li>它照<b>英文</b>的物品說明算說明框有多寬（它的事件優先序比我們高，拿到的還是原文），
 *       可是畫面上是我們就地換好的中文——附了原文的名字更寬。它以為旁邊有位置，其實沒有。</li>
 *   <li>左右兩邊都放不下時，它只把座標夾回畫面裡，於是直接壓在說明框上。</li>
 * </ol>
 *
 * <h2>做法</h2>
 * 它的 {@code RenderUtils.drawTooltip} 由這裡接手（mixin 擋在開頭）：
 * <ul>
 *   <li>說明框的寬度照<b>實際畫出來的</b>那一份算（就地取代模式用我們的譯文，
 *       見 {@code RenderListener#inPlaceLines}），縮放照 Wynntils 的「tooltip 縮放」。</li>
 *   <li>右邊放得下就放右邊，不然左邊；兩邊都放不下，挑空間大的那一邊，整個框縮小到剛好塞進去
 *      （太高時照它原本的規則也縮）。</li>
 *   <li>畫法跟它一樣：同一個原版 tooltip 畫法、同樣隔 18 px。</li>
 * </ul>
 * 縮到比 {@link #MIN_SCALE} 還小就不接手，交回它原本的方法——那時候字已經小得看不清楚了。
 */
public final class WynnventoryLayout {

    private WynnventoryLayout() {
    }

    /** 它的價格框與說明框之間的距離。 */
    private static final int GAP = 18;

    /** 離畫面邊緣至少留這麼多。 */
    private static final int EDGE = 4;

    private static final float MIN_SCALE = 0.45f;

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
            List<ClientTooltipComponent> drawn = components(shown(vanilla));
            List<ClientTooltipComponent> price = components(
                    com.wynnchayuan.translate.WynnventoryBridge.lines(custom));
            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            // 說明框：跟它一樣照 Wynntils 的縮放與「太高就縮」算出畫在哪
            int vw = width(drawn, font);
            int vh = height(drawn, font);
            float fit = Math.min(wynntilsScale(), heightScale(vh, screenH));
            int svw = (int) (vw * fit);
            int svh = (int) (vh * fit);
            Vector2ic at = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                    screenW, screenH, mouseX, mouseY, svw, svh);

            int pw = width(price, font);
            int ph = height(price, font);
            float[] spot = place(screenW, screenH, at.x(), at.y(), svw, pw, ph);
            if (spot == null) {
                return false;
            }
            int x = Math.round(spot[0]);
            int y = Math.round(spot[1]);
            float scale = spot[2];

            final int fx = Math.round(x / scale);
            final int fy = Math.round(y / scale);
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.renderTooltip(font, price, fx, fy,
                    (sw, sh, mx, my, w, h) -> new Vector2i(fx, fy), null);
            graphics.pose().popMatrix();
            // 我們「另開面板」的譯文面板要讓開它，見 WynnventoryBox
            WynnventoryBox.note(x - 4, y - 4, Math.round(pw * scale) + 8,
                    Math.round(ph * scale) + 8, System.nanoTime());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 側邊框放哪、縮多少。
     *
     * @param vx  說明框左上角（畫面座標）
     * @param svw 說明框實際的寬度（已乘上縮放）
     * @return {x, y, 縮放}（畫面座標）；縮得比 {@link #MIN_SCALE} 還小時回 {@code null}
     */
    static float[] place(int screenW, int screenH, int vx, int vy, int svw, int pw, int ph) {
        int right = screenW - EDGE - (vx + svw + GAP);
        int left = vx - GAP - EDGE;
        float scale = heightScale(ph, screenH);
        boolean onRight;
        if (pw * scale <= right) {
            onRight = true;
        } else if (pw * scale <= left) {
            onRight = false;
        } else {
            onRight = right >= left;
            scale = Math.min(scale, Math.max(right, left) / (float) pw);
        }
        if (scale < MIN_SCALE) {
            return null;
        }
        float x = onRight ? vx + svw + GAP : vx - GAP - pw * scale;
        // 頂端至少留 6（跟它一樣）；縮到剛好塞滿畫面高度時，頂端那 6 讓給底部
        float bottom = Math.max(0, screenH - ph * scale - EDGE);
        float y = Math.clamp(vy, Math.min(6, bottom), bottom);
        return new float[] {x, y, scale};
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

    /** 跟它一樣：比畫面高就整個縮小。 */
    private static float heightScale(int h, int screenH) {
        return h + 10 > screenH ? screenH / (float) (h + 10) : 1f;
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
