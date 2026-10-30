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

@Mixin(EntityRenderer.class)
public abstract class EntityNameMixin {

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void wynnchayuan$translate(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name == null || entity instanceof Player) {
            return;
        }
        Component shown = WynntilsText.entityName(name, entity);
        if (shown != name) {
            cir.setReturnValue(shown);
        }
    }
}
