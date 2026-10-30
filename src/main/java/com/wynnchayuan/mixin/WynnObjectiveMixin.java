package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.wynntils.models.objectives.WynnObjective", remap = false)
public abstract class WynnObjectiveMixin {

    @Inject(method = "asObjectiveString", at = @At("RETURN"), cancellable = true,
            remap = false)
    private void wynnchayuan$translate(CallbackInfoReturnable<String> hit) {
        hit.setReturnValue(WynntilsText.objective(hit.getReturnValue()));
    }
}
