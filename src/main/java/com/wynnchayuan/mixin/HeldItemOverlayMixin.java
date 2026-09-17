package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wynntils 的「手持物品名稱」疊層。
 *
 * <h2>為什麼原版那一處不夠</h2>
 * Wynntils 開著這個疊層時會取消原版的 {@code renderSelectedItemName}，改畫自己
 * 記下來的 {@code itemText}。0.2.0_4 只掛在原版上，實機於是完全沒有反應。
 *
 * <p>它只在換手上物品、物品改名時更新那一份字，所以在那兩個時機把字換掉；
 * 每一幀的算繪不必碰。判斷與翻譯在 {@link WynntilsText#heldItemText}。
 */
@Mixin(targets = "com.wynntils.overlays.HeldItemNameOverlay", remap = false)
public abstract class HeldItemOverlayMixin {

    @Shadow(remap = false)
    private StyledText itemText;

    @Inject(method = {"onHeldItemChanged", "onItemRename"}, at = @At("TAIL"), remap = false)
    private void wynnchayuan$translate(CallbackInfo ci) {
        itemText = WynntilsText.heldItemText(itemText);
    }
}
