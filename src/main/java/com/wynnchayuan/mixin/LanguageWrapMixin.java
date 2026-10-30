package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Language.class)
public abstract class LanguageWrapMixin {

    @ModifyVariable(method = "inject", at = @At("HEAD"), argsOnly = true)
    private static Language wynnchayuan$wrap(Language language) {
        return WynnventoryBridge.wrap(language);
    }
}
