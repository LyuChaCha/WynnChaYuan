package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.WynnventoryBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.wynnventory.gui.widget.WynnventoryButton", remap = false)
public abstract class WynnventoryButtonMixin {

    @Inject(method = "getLabel", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void wynnchayuan$label(CallbackInfoReturnable<String> cir) {
        String label = cir.getReturnValue();
        String shown = WynnventoryBridge.word(label);
        if (shown != null && !shown.equals(label)) {
            cir.setReturnValue(shown);
        }
    }
}
