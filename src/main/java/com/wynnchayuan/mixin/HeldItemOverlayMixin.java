package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Wynntils 取消原版 renderSelectedItemName，改畫自己的 itemText
@Mixin(targets = "com.wynntils.overlays.HeldItemNameOverlay", remap = false)
public abstract class HeldItemOverlayMixin {

    @Shadow(remap = false)
    private StyledText itemText;

    @Inject(method = {"onHeldItemChanged", "onItemRename"}, at = @At("TAIL"), remap = false)
    private void wynnchayuan$translate(CallbackInfo ci) {
        itemText = WynntilsText.heldItemText(itemText);
    }
}
