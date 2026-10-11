package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Badges;
import com.wynntils.mc.event.PlayerNametagRenderEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 在名單上的玩家名字前面加一片葉子。
 *
 * <h2>為什麼掛在 Wynntils 的事件上</h2>
 * 玩家名牌是 Wynntils 接管的：它的 {@code CustomNametagRendererFeature} 會把
 * {@link PlayerNametagRenderEvent} 取消掉、自己畫。掛原版的名牌繪製根本走不到。
 *
 * <h2>為什麼是改名牌的字，而且要搶在最前面</h2>
 * 三個模組都想在名字前面放東西，先後是固定的：
 * <ol>
 *   <li><b>WynnMod</b> 包在呼叫名牌繪製的那一行外面，最早跑。它認得這個玩家的話會
 *       把整行<b>重建</b>（階級徽章＋名字），原本接在前面的東西全部不見。</li>
 *   <li>接著才進到名牌繪製，Wynntils 在開頭發這個事件。</li>
 *   <li><b>Wynntils</b> 自己的監聽器（一般優先權）讀這時候的名牌，前面接上它的標記，
 *       取消事件、自己畫；不是 Wynntils 使用者的就放給原版畫。</li>
 * </ol>
 * 所以葉子要接在 2 與 3 之間：用 {@link EventPriority#HIGHEST} 把
 * {@code state.nameTag} 換成「葉子＋原本的名牌」。WynnMod 已經跑完，不會被它重建掉；
 * Wynntils 還沒讀，它畫的和原版畫的都會帶著葉子。排出來是
 * 「Wynntils 標記・葉子・WynnMod 星號・階級・名字」。
 *
 * <p>名牌的狀態每一幀都會重新抽一份，所以改了不會累積；同一幀重發的情況由
 * {@code Badges.startsWithIcon} 擋。
 *
 * <h2>為什麼還留著 receiveCanceled</h2>
 * 這個優先權下通常還沒有人取消，但別的模組也可能用同樣的優先權搶先取消。
 * 取消的意思是「原版不要畫」，不是「誰都不要畫」——Wynntils 接著還是會讀名牌。
 */
public final class BadgeListener {

    /**
     * 從 render state 取出實體 id。
     *
     * <p>{@code AvatarRenderState} 有這個欄位，但事件的型別是共通的
     * {@code EntityRenderState}，所以要判型。取不到就回傳 -1，
     * 呼叫端會退回比對名牌文字。
     */
    private static int entityIdOf(EntityRenderState state) {
        return state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatar
                ? avatar.id : -1;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onPlayerNametag(PlayerNametagRenderEvent event) {
        try {
            EntityRenderState state = event.getEntityRenderState();
            Component decorated = Badges.decorate(state.nameTag, entityIdOf(state));
            if (decorated != null) {
                state.nameTag = decorated;
            }
        } catch (Throwable t) {
            // 這只是裝飾。名牌每幀會跑好幾次，出事絕不能拖垮畫面
            WynnChaYuan.store().noteEvent("render.badgeError");
            com.wynnchayuan.translate.ErrorDebug.note("nametag.badge", null, t);
        }
    }
}
