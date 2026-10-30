package com.wynnchayuan.mixin;

import com.wynnchayuan.render.HighlightModelCache;
import com.wynntils.mc.event.DataComponentGetEvent;
import net.minecraft.world.item.component.CustomModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.wynntils.features.inventory.ItemHighlightFeature", remap = false)
public abstract class ItemHighlightSpeedMixin {

    @Inject(method = "onGetModelData", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private void wynnchayuan$cached(DataComponentGetEvent.CustomModelData event, CallbackInfo ci) {
        CustomModelData original = event.getOriginalValue();
        CustomModelData value = HighlightModelCache.lookup(this, original);
        if (value == null) {
            return;
        }
        if (value != original) {
            event.setValue(value);
        }
        ci.cancel();
    }
}
