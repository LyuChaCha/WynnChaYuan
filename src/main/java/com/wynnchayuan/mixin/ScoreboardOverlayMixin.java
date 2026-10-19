package com.wynnchayuan.mixin;

import com.wynnchayuan.render.ScoreboardWrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wynntils 記分板疊層：畫之前換成譯文並把太寬的列折開，畫完放回原文。
 * 為什麼、怎麼折，見 {@link ScoreboardWrap}。
 */
@Mixin(targets = "com.wynntils.overlays.ScoreboardOverlay", remap = false)
public abstract class ScoreboardOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$wrap(CallbackInfo ci) {
        ScoreboardWrap.begin(this);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$restore(CallbackInfo ci) {
        ScoreboardWrap.end(this);
    }
}
