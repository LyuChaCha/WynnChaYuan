package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 畫面頂端 boss bar 的標題（攻擊生物時的「Horse - 47❤」）。
 *
 * <p>只換<b>畫出去</b>的那一份：Wynntils 從更新封包讀 boss bar 的英文字，
 * 那條路不經過這裡，照樣讀得到原文。寬度與置中照譯文算。
 *
 * <p>跟名牌翻譯同一個 F6 開關，見 {@link WynntilsText#bossBar}。
 */
@Mixin(BossHealthOverlay.class)
public abstract class BossBarNameMixin {

    @Redirect(method = "render",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/components/LerpingBossEvent;getName()Lnet/minecraft/network/chat/Component;"))
    private Component wynnchayuan$translate(LerpingBossEvent event) {
        return WynntilsText.bossBar(event.getName());
    }
}
