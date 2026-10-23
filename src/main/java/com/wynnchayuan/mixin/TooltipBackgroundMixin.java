package com.wynnchayuan.mixin;

import com.wynnchayuan.render.OriginalTooltipBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 每個 tooltip 畫底色之前：記下原文真正畫在哪裡，見 {@link OriginalTooltipBox}。 */
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipBackgroundMixin {

    @Inject(method = "renderTooltipBackground", at = @At("HEAD"), require = 0)
    private static void wynnchayuan$note(GuiGraphics graphics, int x, int y, int width, int height,
                                         Identifier style, CallbackInfo ci) {
        OriginalTooltipBox.note(graphics, x, y, width, height);
    }
}
