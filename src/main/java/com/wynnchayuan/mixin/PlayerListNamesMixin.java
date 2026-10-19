package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wynntils 自訂玩家清單（按 Tab）裡的名字，不要翻。
 *
 * <h2>回報（2026-10-19）</h2>
 * 清單裡一位玩家叫 {@code Mark64118}，畫面上成了「標記64118」：數字收成 {@code {~}}
 * 之後剩下的 {@code Mark} 對上了技能詞表的 {@code "Mark": "標記"}。
 *
 * <p>跟 {@link TerritoryPoiMixin} 是同一回事：名字是玩家自己取的，跟語料撞名只是遲早的事，
 * 擋的是<b>那個位置</b>。從 {@code renderPlayerNames} 進去到出來，Wynntils 畫的字一律原樣放行。
 * 這份清單是伺服器的玩家列表，裡面沒有要翻的遊戲文案。
 */
@Mixin(targets = "com.wynntils.overlays.CustomPlayerListOverlay", remap = false)
public abstract class PlayerListNamesMixin {

    @Inject(method = "renderPlayerNames", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$hold(CallbackInfo ci) {
        WynntilsText.holdRawText(true);
    }

    @Inject(method = "renderPlayerNames", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$release(CallbackInfo ci) {
        WynntilsText.holdRawText(false);
    }
}
