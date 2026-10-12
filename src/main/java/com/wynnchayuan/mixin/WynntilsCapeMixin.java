package com.wynnchayuan.mixin;

import com.wynnchayuan.cape.Capes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 穿著 WynnChaYuan 披風的人，Wynntils 的披風讓開。
 *
 * <p>Wynntils 的披風不是換貼圖，是它自己另外一層；而且它那一層要畫的時候，會把原版的
 * 披風層取消掉。兩件事問的都是 {@code CosmeticsService#shouldRenderCape}——這裡讓它對
 * 我們的人回「不畫」，它那一層就不畫、原版那一層也不會被取消。
 *
 * <p>只管披風。{@code elytra} 是 true 的那一次問的是鞘翅的外觀，不關我們的事。
 */
@Mixin(targets = "com.wynntils.services.cosmetics.CosmeticsService", remap = false)
public abstract class WynntilsCapeMixin {

    @Inject(method = "shouldRenderCape", at = @At("HEAD"), cancellable = true, remap = false)
    private void wynnchayuan$yield(Player player, boolean elytra,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (!elytra && Capes.overrides(player)) {
            cir.setReturnValue(false);
        }
    }
}
