package com.wynnchayuan.mixin;

import com.wynnchayuan.render.HighlightModelCache;
import com.wynntils.mc.event.DataComponentGetEvent;
import net.minecraft.world.item.component.CustomModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wynntils 物品高亮每次讀模型資料都重算一次，改成照物件快取。
 * 為什麼、怎麼算，見 {@link HighlightModelCache}；F6「功能」可以關掉。
 */
@Mixin(targets = "com.wynntils.features.inventory.ItemHighlightFeature", remap = false)
public abstract class ItemHighlightSpeedMixin {

    @Inject(method = "onGetModelData", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private void wynnchayuan$cached(DataComponentGetEvent.CustomModelData event, CallbackInfo ci) {
        CustomModelData original = event.getOriginalValue();
        CustomModelData value = HighlightModelCache.lookup(this, original);
        if (value == null) {
            return;                            // 停用或對不上：照 Wynntils 原本的方法
        }
        if (value != original) {
            event.setValue(value);
        }
        ci.cancel();
    }
}
