package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Wynntils 從更新封包讀 boss bar 英文，不經過這裡
@Mixin(BossHealthOverlay.class)
public abstract class BossBarNameMixin {

    @Redirect(method = "render",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/components/LerpingBossEvent;getName()Lnet/minecraft/network/chat/Component;"))
    private Component wynnchayuan$translate(LerpingBossEvent event) {
        return WynntilsText.bossBar(event.getName());
    }
}
