package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.WynnventoryBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wynnventory 篩選鈕的名字（Mythic、Fabled…）：它的提示是「Toggle filtering for %s」，
 * 句子本身走語言檔，填進去的那個名字是寫死的。
 */
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
