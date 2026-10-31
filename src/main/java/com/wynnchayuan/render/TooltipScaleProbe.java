package com.wynnchayuan.render;

import com.wynntils.core.components.Managers;
import com.wynntils.features.tooltips.TooltipFittingFeature;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;

import java.lang.reflect.Field;

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

    public static boolean fittingEnabled() {
        try {
            return Managers.Feature.getFeatureInstance(TooltipFittingFeature.class).isEnabled();
        } catch (Throwable t) {
            return true;
        }
    }
}
