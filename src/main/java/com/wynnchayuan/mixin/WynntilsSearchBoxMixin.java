package com.wynnchayuan.mixin;

import com.wynnchayuan.render.TypedText;
import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 搜尋框那一半：{@code SearchWidget} 自己畫字，不走父類別。
 *
 * <p>理由與做法見 {@link WynntilsTextBoxMixin}——那邊打
 * {@code TextInputBoxWidget}，這邊打覆寫它的 {@code SearchWidget}。
 * 分成兩個 mixin 類別是因為它們是父子關係：同一個 mixin 套到父子兩邊，
 * 注入進去的私有方法會互相覆蓋。
 *
 * <p>目標類別不在時由 {@link WynntilsGate} 擋掉，遊戲照常開得起來。
 */
@Mixin(targets = "com.wynntils.screens.base.widgets.SearchWidget", remap = false)
public abstract class WynntilsSearchBoxMixin {

    @Inject(method = "doRenderWidget", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$holdRaw(CallbackInfo ci) {
        WynntilsText.holdRawText(TypedText.present(this));
    }

    @Inject(method = "doRenderWidget", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$releaseRaw(CallbackInfo ci) {
        WynntilsText.holdRawText(false);
    }
}
