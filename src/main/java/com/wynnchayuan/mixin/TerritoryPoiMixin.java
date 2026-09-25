package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 公會戰地圖上那一格一格的領地標籤，不要翻。
 *
 * <h2>回報</h2>
 * 公會戰地圖整片都是彩色方框，每一格上面寫著佔領它的公會縮寫：
 * {@code Maya}、{@code PUN}、{@code ETKW}⋯⋯。其中一格的公會叫 {@code Fox}，
 * 畫面上被換成了「狐狸」——因為 {@code npc.json} 裡真的有一條
 * {@code "Fox": "狐狸"}（遊戲裡確實有叫 Fox 的 NPC）。
 *
 * <h2>為什麼不是改語料</h2>
 * 公會名是<b>玩家自己取的</b>，什麼字都有可能：{@code Fox}、{@code Blank}、
 * {@code Maya}。跟語料裡任何一個詞撞名都只是遲早的事，一條一條躲躲不完。
 * 而且這些名字本來就不該翻——它是別人的公會，不是遊戲的文案。
 *
 * <p>所以擋的是<b>那個位置</b>：這一格標籤是 {@code TerritoryPoi} 畫的，
 * 從它進去到它出來，Wynntils 畫面上的字一律原樣放行。
 *
 * <h2>為什麼用旗標而不是計數</h2>
 * {@code renderAt} 不會自己套自己，一個布林就夠。用布林還有一個好處：
 * 萬一它中途丟例外、旗標卡在「不要翻」，<b>下一格</b>進來就會把它重設，
 * 自己會好。計數器則會一路累加，再也回不來。
 *
 * <p>目標類別不在時整個 mixin 會被 {@link WynntilsGate} 跳過。
 */
@Mixin(targets = "com.wynntils.services.map.pois.TerritoryPoi", remap = false)
public abstract class TerritoryPoiMixin {

    @Inject(method = "renderAt", at = @At("HEAD"), remap = false, require = 0)
    private void wynnchayuan$hold(CallbackInfo ci) {
        WynntilsText.holdRawText(true);
    }

    @Inject(method = "renderAt", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$release(CallbackInfo ci) {
        WynntilsText.holdRawText(false);
    }
}
