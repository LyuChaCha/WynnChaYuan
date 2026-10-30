package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.SearchMatch;
import com.wynntils.services.itemfilter.type.ItemSearchQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 打 itemNameMatches 不打 matches：在 RETURN 分不出哪半沒過
@Mixin(targets = "com.wynntils.services.itemfilter.ItemFilterService", remap = false)
public abstract class WynntilsItemSearchMixin {

    @Inject(
            method = "itemNameMatches",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0)
    private void wynnchayuan$alsoMatchTranslatedName(
            ItemSearchQuery searchQuery, String itemName,
            CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (SearchMatch.nameAlsoMatches(itemName, searchQuery.plainTextTokens())) {
            cir.setReturnValue(true);
        }
    }
}
