package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Wynnventory 的「找到收藏的物品」通知：標題走語言檔，底下那行
 * 「{@code <物品> in <獎勵池>}」「{@code N more...}」是它自己拼的。
 */
@Pseudo
@Mixin(targets = "com.wynnventory.feature.FavouriteNotifyFeature", remap = false)
public abstract class WynnventoryToastMixin {

    @ModifyVariable(method = "showToast", at = @At("HEAD"), argsOnly = true, remap = false,
                    require = 0)
    private static Component wynnchayuan$toast(Component description) {
        return WynnventoryBridge.toast(description);
    }
}
