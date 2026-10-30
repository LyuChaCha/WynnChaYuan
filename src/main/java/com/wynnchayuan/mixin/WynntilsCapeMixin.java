package com.wynnchayuan.mixin;

import com.wynnchayuan.cape.Capes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Wynntils 畫披風與取消原版披風都問 shouldRenderCape
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
