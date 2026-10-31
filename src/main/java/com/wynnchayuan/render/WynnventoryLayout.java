package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.listener.RenderListener;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.TranslationStore;
import com.wynnchayuan.translate.WynnventoryBridge;
import com.wynntils.core.components.Managers;
import com.wynntils.features.tooltips.TooltipFittingFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WynnventoryLayout {

    private WynnventoryLayout() {
    }

    private static final int GAP = 18;

    private static final int EDGE = 4;

    private static final int PAD = 4;

    private static final int EXTRA_H = 2 + PAD * 2;

    private static final float MIN_SCALE = 0.45f;

    private static boolean drawing;

    public static boolean drawing() {
        return drawing;
    }

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
                            WynnventoryBridge.lines(custom), font));
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
            WynnventoryBox.note(Math.round(spot[0] - PAD * scale), Math.round(spot[1] - PAD * scale),
                    Math.round((pw + PAD * 2) * scale), Math.round((ph + EXTRA_H) * scale),
                    System.nanoTime());
            return true;
        } catch (Throwable t) {
            ErrorDebug.note("WynnventoryLayout#draw", null, t);
            drawing = false;
            return false;
        }
    }

    static float[] anchoredSpot(int screenH, int pw, int ph) {
        float scale = fitScale(ph, screenH);
        float x = EDGE + PAD * scale;
        float y = (screenH - (ph + EXTRA_H) * scale) / 2f + PAD * scale;
        return new float[] {x, y, scale};
    }

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

    public static ClientTooltipPositioner shift(ClientTooltipPositioner delegate, float scale) {
        int[] box = WynnventoryBox.current();
        if (box == null || drawing || OriginalTooltipBox.drawingOwn() || delegate instanceof Shifted) {
            return null;
        }
        ClientTooltipPositioner base = delegate == null ? DefaultTooltipPositioner.INSTANCE : delegate;
        return new Shifted(base, box, scale <= 0 ? 1f : scale);
    }

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
            ErrorDebug.note("WynnventoryLayout#anchored", null, t);
            return false;
        }
    }

    private static Method configGet;
    private static Method settingsGet;
    private static Method anchorGet;

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

    private static int drawnStamp() {
        TranslationStore store = WynnChaYuan.translations();
        CollectorConfig config = WynnChaYuan.config();
        return Objects.hash(store == null ? 0 : store.generation(),
                config == null ? null : config.itemNames(), config == null ? null : config.tooltipMode(),
                RenderListener.shiftHeld());
    }

    private static List<Component> shown(List<Component> vanilla) {
        CollectorConfig config = WynnChaYuan.config();
        if (config == null || config.tooltipMode() != CollectorConfig.TooltipMode.REPLACE) {
            return vanilla;
        }
        return RenderListener.inPlaceLines(vanilla);
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

    static float fitScale(int h, int screenH) {
        float whole = h + EXTRA_H;
        float room = screenH - EDGE * 2;
        return whole > room ? room / whole : 1f;
    }

    private static float wynntilsScale() {
        try {
            var feature = Managers.Feature.getFeatureInstance(TooltipFittingFeature.class);
            return feature.isEnabled() ? feature.universalScale.get() : 1f;
        } catch (Throwable t) {
            ErrorDebug.note("WynnventoryLayout#wynntilsScale", null, t);
            return 1f;
        }
    }
}
