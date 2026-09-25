package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品格角落那幾個字是 Wynntils 自己算出來的<b>簡稱</b>，不要翻。
 *
 * <h2>回報</h2>
 * 傳送卷軸的格子上，Wynntils 會寫一小段字標示目的地。實機上那段字被換成了
 * 「傳送」——因為 {@code ability/mage.json} 裡真的有一條 {@code "Teleport": "傳送"}
 * （法師的技能就叫 Teleport）。
 *
 * <h2>為什麼不是改語料</h2>
 * 那一條是對的：法師的技能確實該翻成「傳送」。錯的是它被套到了<b>別的東西</b>上。
 * 同一個格子上還有翡翠袋的等級、採集工具的等級、放大器的等級——全都是 Wynntils
 * 照物品資料拼出來的兩三個字，本來就不是遊戲的文案。一條一條躲躲不完，
 * 而且躲掉就等於把技能名也毀了。
 *
 * <p>所以擋的是<b>那個位置</b>：{@code ItemTextOverlayFeature#drawTextOverlay}
 * 進去到出來之間，Wynntils 畫面上的字一律原樣放行。那支方法最後轉進的是
 * {@code FontRenderer.renderText}，正是 {@code WynntilsFontMixin} 打的那一支
 * ——不擋就一定會被換掉。
 *
 * <p>旗標的用法與理由跟 {@link TerritoryPoiMixin} 同一套：布林而不是計數，
 * 卡住了下一格自己會重設。
 *
 * <p>目標類別不在時整個 mixin 會被 {@link WynntilsGate} 跳過。
 */
@Mixin(targets = "com.wynntils.features.inventory.ItemTextOverlayFeature", remap = false)
public abstract class ItemTextOverlayMixin {

    @Inject(method = "drawTextOverlay", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$hold(CallbackInfo ci) {
        WynntilsText.holdRawText(true);
    }

    @Inject(method = "drawTextOverlay", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$release(CallbackInfo ci) {
        WynntilsText.holdRawText(false);
    }
}
