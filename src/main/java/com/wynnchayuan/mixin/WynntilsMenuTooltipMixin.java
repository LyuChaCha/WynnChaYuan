package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Wynntils 清單畫面右邊那張卡：滑鼠停在某一項時跳出來的說明。
 *
 * <h2>那張卡裡有什麼</h2>
 * 綜合頁面停在一個任務上時，卡上是任務名、「Currently in progress」、
 * 要做什麼、距離、長度、難度、獎勵、「Click To Track」。這些<b>全部是
 * Wynncraft 送來的字</b>，Wynntils 只是重畫一次——它自己的語言檔永遠不會有，
 * 而我們的語料裡早就有了。
 *
 * <h2>為什麼打在 getTooltipLines 的呼叫點</h2>
 * {@code renderTooltip} 只做三件事：跟被停在上面的那個元件要 tooltip、
 * 空的就回去、不空就交給原版畫。把<b>要</b>的那一步接走最單純——
 * 我們拿到的是完整的一份 {@code List<Component>}，換完原樣交回去，
 * 位置與樣式一個位元都不必碰。
 *
 * <p>換好之後走的是跟物品 tooltip 完全同一套（見 {@link WynntilsText#menuTooltip}），
 * 所以整段查得到就用整段、查不到才逐行——跨行的句子只有整段那條路查得到。
 *
 * <p>目標類別不在時整個 mixin 會被 {@link WynntilsGate} 跳過。
 */
@Mixin(targets = "com.wynntils.screens.base.WynntilsListScreen", remap = false)
public abstract class WynntilsMenuTooltipMixin {

    @Redirect(
            method = "renderTooltip",
            at = @At(value = "INVOKE",
                    target = "Lcom/wynntils/screens/base/TooltipProvider;"
                            + "getTooltipLines()Ljava/util/List;"),
            remap = false)
    private List<Component> wynnchayuan$translate(
            com.wynntils.screens.base.TooltipProvider provider) {
        return WynntilsText.menuTooltip(provider.getTooltipLines());
    }
}
