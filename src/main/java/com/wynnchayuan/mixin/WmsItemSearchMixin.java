package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.WmsBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.a0g.api.WynnItem", remap = false)
public abstract class WmsItemSearchMixin {

    @Shadow(remap = false)
    @Final
    private String name;

    @Inject(method = "matchesSearch", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private void wynnchayuan$translatedName(String query, CallbackInfoReturnable<Boolean> cir) {
        if (WmsBridge.matches(name, query)) {
            cir.setReturnValue(true);
        }
    }
}
