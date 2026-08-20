package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Badges;
import com.wynntils.mc.event.PlayerNametagRenderEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 在名單上的玩家名牌上方多畫一行標記。
 *
 * <h2>為什麼不是掛原版的名牌繪製</h2>
 * 一開始是用 mixin 掛在原版 {@code EntityRenderer#submitNameTag} 的尾端，
 * 結果完全不會被呼叫——<b>Wynntils 接管了玩家名牌</b>。它的
 * {@code CustomNametagRendererFeature} 會把
 * {@link PlayerNametagRenderEvent} 取消掉、自己畫（donator 標記、公會標籤都是
 * 這樣來的），原版那條路根本走不到尾端。
 *
 * <p>所以改成訂閱同一個事件。事件本身<b>無論如何都會發</b>，被不被取消是
 * Wynntils 的功能在決定的，跟我們無關——我們只是在它旁邊多畫一行。
 * 這也順便把整個模組唯一的 mixin 拿掉了。
 *
 * <p>用 {@link EventPriority#LOWEST}：等 Wynntils 決定完自己要怎麼畫，
 * 我們最後才加上去。
 */
public final class BadgeListener {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerNametag(PlayerNametagRenderEvent event) {
        try {
            EntityRenderState state = event.getEntityRenderState();
            Component badge = Badges.forNameTag(state.nameTag);
            if (badge == null) {
                return;
            }
            // 用 Wynntils 給的同一個 collector 送一次名牌，只把附著點往上抬一行。
            // 面向相機、深度、背景、距離淡出全部沿用原版行為。
            event.getSubmitNodeCollector().submitNameTag(
                    event.getPoseStack(),
                    state.nameTagAttachment.add(0.0, Badges.HEIGHT, 0.0),
                    0,
                    badge,
                    !state.isDiscrete,
                    state.lightCoords,
                    state.distanceToCameraSq,
                    event.getCameraRenderState());
        } catch (Throwable t) {
            // 這只是裝飾。名牌每幀會跑好幾次，出事絕不能拖垮畫面
            WynnChaYuan.store().noteEvent("render.badgeError");
        }
    }
}
