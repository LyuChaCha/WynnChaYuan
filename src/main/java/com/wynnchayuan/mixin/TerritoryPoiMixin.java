package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.wynntils.services.map.pois.TerritoryPoi", remap = false)
public abstract class TerritoryPoiMixin {

    @Inject(method = "renderAt", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$hold(CallbackInfo ci) {
        WynntilsText.holdRawText(true);
    }

    @Inject(method = "renderAt", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$release(CallbackInfo ci) {
        WynntilsText.holdRawText(false);
    }
}
