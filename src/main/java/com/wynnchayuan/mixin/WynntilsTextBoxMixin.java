package com.wynnchayuan.mixin;

import com.wynnchayuan.render.TypedText;
import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家自己打進去的字，不要翻。
 *
 * <h2>實機回報</h2>
 * 在銀行的搜尋框打 {@code Abysso Galoshes}，框裡顯示的卻是「深淵雨靴」。
 * 游標還停在原本那串英文的寬度上，所以字的後面空一大截——
 * 看起來像輸入法出了問題。
 *
 * <p>成因是 Wynntils 的輸入框也走它自己的 {@code FontRenderer}
 *（見 {@code WynntilsFontMixin}）。那一道不分青紅皂白，看到語料裡有的字就換，
 * 而玩家剛好打出了一個裝備名稱。
 *
 * <h2>為什麼打在 doRenderWidget 上</h2>
 * {@code TextInputBoxWidget} 是 Wynntils <b>每一個</b>文字輸入框的祖先：
 * 搜尋框、路徑點名稱、設定值⋯⋯而 {@code doRenderWidget} 是它畫字的那一支。
 *
 * <p>{@code SearchWidget} 覆寫了它（改成畫三段：游標前、選取、游標後）
 * 而且<b>沒有</b>呼叫 {@code super}，所以那一邊要另外打，見
 * {@link WynntilsSearchBoxMixin}。兩道打完，{@code ItemSearchWidget}、
 * {@code ListSearchWidget} 那幾個子類別自動都有——它們沒有再覆寫這一支。
 *
 * <p>目標類別不在時由 {@link WynntilsGate} 擋掉，遊戲照常開得起來。
 */
@Mixin(targets = "com.wynntils.screens.base.widgets.TextInputBoxWidget", remap = false)
public abstract class WynntilsTextBoxMixin {

    @Inject(method = "doRenderWidget", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$holdRaw(CallbackInfo ci) {
        WynntilsText.holdRawText(TypedText.present(this));
    }

    @Inject(method = "doRenderWidget", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$releaseRaw(CallbackInfo ci) {
        WynntilsText.holdRawText(false);
    }
}
