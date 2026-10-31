package com.wynnchayuan.mixin;

import com.wynnchayuan.render.ComponentMemo;
import com.wynnchayuan.render.OriginalTooltipBox;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class FrameStartMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void wynnchayuan$frame(DeltaTracker tracker, boolean tick, CallbackInfo ci) {
        ComponentMemo.nextFrame();
        OriginalTooltipBox.nextFrame();
    }
}
