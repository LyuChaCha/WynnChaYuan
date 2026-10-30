package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynnventoryPriceCache;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "com.wynnventory.feature.tooltip.price.PriceTooltipFactory", remap = false)
public abstract class WynnventoryPriceCacheMixin {

    @Inject(method = "getPriceTooltip", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private void wynnchayuan$cached(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> hit = WynnventoryPriceCache.get(stack);
        if (hit != null) {
            cir.setReturnValue(hit);
        }
    }

    @Inject(method = "getPriceTooltip", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$remember(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        WynnventoryPriceCache.put(stack, cir.getReturnValue());
    }
}
