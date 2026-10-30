package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Badges;
import com.wynntils.mc.event.PlayerNametagRenderEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

// WynnMod 最先跑且會重建整行，故用 HIGHEST 改 state.nameTag
public final class BadgeListener {

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
            WynnChaYuan.store().noteEvent("render.badgeError");
            com.wynnchayuan.translate.ErrorDebug.note("nametag.badge", null, t);
        }
    }
}
