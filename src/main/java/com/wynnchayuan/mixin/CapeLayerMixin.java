package com.wynnchayuan.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynnchayuan.cape.Capes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 畫完要換回：鞘翅層沒貼圖時會拿披風那張畫
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {

    private static final String SUBMIT = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V";

    @Unique
    private AvatarRenderState wynnchayuan$state;
    @Unique
    private PlayerSkin wynnchayuan$skin;
    @Unique
    private boolean wynnchayuan$shown;

    @Inject(method = SUBMIT, at = @At("HEAD"))
    private void wynnchayuan$wear(PoseStack pose, SubmitNodeCollector nodes, int light,
                                  AvatarRenderState state, float yRot, float xRot,
                                  CallbackInfo ci) {
        PlayerSkin before = Capes.swap(state);
        if (before != null) {
            wynnchayuan$state = state;
            wynnchayuan$skin = before;
            wynnchayuan$shown = state.showCape;
            state.showCape = true;
        }
    }

    @Inject(method = SUBMIT, at = @At("RETURN"))
    private void wynnchayuan$restore(PoseStack pose, SubmitNodeCollector nodes, int light,
                                     AvatarRenderState state, float yRot, float xRot,
                                     CallbackInfo ci) {
        if (wynnchayuan$state == state && wynnchayuan$skin != null) {
            state.skin = wynnchayuan$skin;
            state.showCape = wynnchayuan$shown;
        }
        wynnchayuan$state = null;
        wynnchayuan$skin = null;
    }
}
