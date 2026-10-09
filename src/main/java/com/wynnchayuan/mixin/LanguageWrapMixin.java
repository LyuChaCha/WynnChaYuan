package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 遊戲換語言物件的時候（進遊戲、重載資源、改語言），替它包上 Wynnventory 那一層。
 *
 * <p>沒裝 Wynnventory 的時候 {@link WynnventoryBridge#wrap} 原樣放行，這裡等於沒有。
 * 為什麼要包、包了做什麼，見 {@link WynnventoryBridge}。
 */
@Mixin(Language.class)
public abstract class LanguageWrapMixin {

    @ModifyVariable(method = "inject", at = @At("HEAD"), argsOnly = true)
    private static Language wynnchayuan$wrap(Language language) {
        return WynnventoryBridge.wrap(language);
    }
}
