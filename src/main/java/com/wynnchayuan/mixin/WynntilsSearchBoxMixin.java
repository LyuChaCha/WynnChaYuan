package com.wynnchayuan.mixin;

import com.wynnchayuan.render.TypedText;
import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// SearchWidget 覆寫 doRenderWidget 不呼叫 super
@Mixin(targets = "com.wynntils.screens.base.widgets.SearchWidget", remap = false)
public abstract class WynntilsSearchBoxMixin {

    @Inject(method = "doRenderWidget", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$holdRaw(CallbackInfo ci) {
        WynntilsText.holdRawText(TypedText.present(this));
    }

    @Inject(method = "doRenderWidget", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$releaseRaw(CallbackInfo ci) {
        WynntilsText.holdRawText(false);
    }
}
