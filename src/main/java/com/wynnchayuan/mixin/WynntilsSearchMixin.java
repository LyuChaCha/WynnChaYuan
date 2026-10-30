package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.SearchMatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.wynntils.utils.StringUtils", remap = false)
public abstract class WynntilsSearchMixin {

    @Inject(
            method = "partialMatch(Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private static void wynnchayuan$alsoMatchTranslated(
            String content, String query, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (SearchMatch.alsoMatches(content, query)) {
            cir.setReturnValue(true);
        }
    }
}
