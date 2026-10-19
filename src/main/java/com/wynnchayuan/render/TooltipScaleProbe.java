package com.wynnchayuan.render;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;

import java.lang.reflect.Field;

/**
 * Wynntils 的「tooltip 縮放」把太高的 tooltip 縮小時換上的定位方式（{@code ScaledTooltipPositioner}），
 * 它縮了多少。讀它的私有欄位；不是那一種、或讀不到，就當沒縮（1）。
 * 給 {@link WynnventoryLayout#shift} 換算畫面座標用。
 */
public final class TooltipScaleProbe {

    private TooltipScaleProbe() {
    }

    private static Field factor;
    private static boolean broken;

    public static float scaleOf(ClientTooltipPositioner positioner) {
        if (positioner == null || broken
                || !positioner.getClass().getSimpleName().equals("ScaledTooltipPositioner")) {
            return 1f;
        }
        try {
            if (factor == null) {
                Field f = positioner.getClass().getDeclaredField("scaleFactor");
                f.setAccessible(true);
                factor = f;
            }
            return factor.getFloat(positioner);
        } catch (Throwable t) {
            broken = true;
            return 1f;
        }
    }

    /** Wynntils 的「tooltip 縮放」有沒有開；讀不到就當開著（交給上面那個 mixin 處理）。 */
    public static boolean fittingEnabled() {
        try {
            return com.wynntils.core.components.Managers.Feature.getFeatureInstance(
                    com.wynntils.features.tooltips.TooltipFittingFeature.class).isEnabled();
        } catch (Throwable t) {
            return true;
        }
    }
}
