package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 實體頭上的自訂名稱：隱形盔甲座疊成的浮空字。
 *
 * <p>Wynncraft 大部分的浮空字已經改用 TextDisplay（走 Wynntils 的名牌事件），
 * 但還有一些是舊式做法——討伐戰的「Void Altar／Give your soul, and enter the altar.」
 * 就是兩個疊起來的盔甲座。這種字先前完全沒經過模組。
 *
 * <p>只換<b>畫出去</b>的那一份，實體本身的名字不動。玩家不換——那是別人的 ID。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityNameMixin {

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void wynnchayuan$translate(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name == null || entity instanceof Player) {
            return;
        }
        Component shown = WynntilsText.entityName(name);
        if (shown != name) {
            cir.setReturnValue(shown);
        }
    }
}
