package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.WmsBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WynnMarketSearch 的搜尋框：打譯名也找得到。
 *
 * <p>那個模組問每一件物品「符不符合搜尋字」，這裡在它判斷之前先看一眼譯名——
 * 譯名對得上就直接答「符合」，對不上就不插手，照它原本的規則。
 * 判斷本身在 {@link WmsBridge}。
 *
 * <p>{@link Pseudo}：那個模組是選裝的，編譯時沒有它的類別。沒裝的時候
 * {@code WynntilsGate} 會把這個 mixin 整個跳過。
 * {@code remap = false}：目標是別的模組自己的類別與方法，不是原版的，名稱不會被改寫。
 */
@Pseudo
@Mixin(targets = "me.a0g.api.WynnItem", remap = false)
public abstract class WmsItemSearchMixin {

    @Shadow(remap = false)
    @Final
    private String name;

    @Inject(method = "matchesSearch", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private void wynnchayuan$translatedName(String query, CallbackInfoReturnable<Boolean> cir) {
        if (WmsBridge.matches(name, query)) {
            cir.setReturnValue(true);
        }
    }
}
