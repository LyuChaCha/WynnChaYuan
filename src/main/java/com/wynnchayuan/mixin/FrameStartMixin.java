package com.wynnchayuan.mixin;

import com.wynnchayuan.render.ComponentMemo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 每一幀的開頭：物品資料的一幀快取換新的幀號，見 {@link ComponentMemo}。 */
@Mixin(GameRenderer.class)
public abstract class FrameStartMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void wynnchayuan$frame(DeltaTracker tracker, boolean tick, CallbackInfo ci) {
        ComponentMemo.nextFrame();
    }
}
