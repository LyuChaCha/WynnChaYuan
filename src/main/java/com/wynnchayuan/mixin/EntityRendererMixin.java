package com.wynnchayuan.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Badges;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在名單上的玩家名牌上方多畫一行標記。
 *
 * <h2>為什麼是這裡，而且只加一次呼叫</h2>
 * 原版的名牌只有一行，要多一行就得自己畫。但完全自己畫要處理面向相機、
 * 深度、背景底色、縮放與距離淡出——那些原版都做好了。
 *
 * <p>所以這裡不重寫任何東西：在原版畫完之後，用<b>同一支</b>
 * {@link SubmitNodeCollector#submitNameTag} 再送一次，只把附著點往上抬一行、
 * 文字換成標記。行為與原版名牌完全一致，版本更新時也不容易壞。
 *
 * <p>這是整個模組唯一的 mixin。其他地方都走 Wynntils 的事件或 Fabric 的
 * 生命週期，因為那些不會因為 Minecraft 內部重構而無聲失效——名牌渲染沒有
 * 對應的事件，只能用這條路。
 *
 * <p>注入點是 {@code TAIL}：原版畫完才輪到我們，不會蓋掉原本的名字。
 * 而且原版在 {@code nameTag == null} 時直接 return，那種情況我們也跟著不畫。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // require = 0：注入點對不上時安靜地不生效。標記只是裝飾，
    // 不值得讓玩家的遊戲開不起來——整個模組都照這條規則。
    @Inject(method = "submitNameTag", at = @At("TAIL"), require = 0)
    private void wynnchayuan$contributorBadge(EntityRenderState state,
                                              PoseStack pose,
                                              SubmitNodeCollector collector,
                                              CameraRenderState camera,
                                              CallbackInfo ci) {
        try {
            Component badge = Badges.forNameTag(state.nameTag);
            if (badge == null) {
                return;
            }
            collector.submitNameTag(
                    pose,
                    state.nameTagAttachment.add(0.0, Badges.HEIGHT, 0.0),
                    0,
                    badge,
                    !state.isDiscrete,
                    state.lightCoords,
                    state.distanceToCameraSq,
                    camera);
        } catch (Throwable t) {
            // 這只是裝飾。名牌渲染每幀跑好幾次，出事的話絕不能拖垮畫面
            WynnChaYuan.store().noteEvent("render.badgeError");
        }
    }
}
