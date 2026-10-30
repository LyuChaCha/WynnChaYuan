package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.wynntils.features.inventory.ItemTextOverlayFeature", remap = false)
public abstract class ItemTextOverlayMixin {

    @Inject(method = "drawTextOverlay", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$hold(CallbackInfo ci) {
        WynntilsText.holdRawText(true);
    }

    @Inject(method = "drawTextOverlay", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$release(CallbackInfo ci) {
        WynntilsText.holdRawText(false);
    }
}
