package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "com.wynnventory.feature.FavouriteNotifyFeature", remap = false)
public abstract class WynnventoryToastMixin {

    @ModifyVariable(method = "showToast", at = @At("HEAD"), argsOnly = true, remap = false,
                    require = 0)
    private static Component wynnchayuan$toast(Component description) {
        return WynnventoryBridge.toast(description);
    }
}
