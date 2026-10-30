package com.wynnchayuan.capture;

import com.wynnchayuan.translate.ErrorDebug;
import com.wynntils.core.components.Models;
import com.wynntils.models.lootrun.type.LootrunningState;
import com.wynntils.models.raid.type.RaidInfo;
import com.wynntils.models.raid.type.RaidRoomInfo;
import com.wynntils.models.territories.profile.TerritoryProfile;

import net.minecraft.client.Minecraft;

public final class Whereabouts {

    private Whereabouts() {}

    public static String now() {
        try {
            String raid = raid();
            if (raid != null) {
                return raid;
            }
            String area = area();
            if (Models.Lootrun.getState() != null
                    && Models.Lootrun.getState() != LootrunningState.NOT_RUNNING) {
                return area == null ? "Lootrun" : "Lootrun · " + area;
            }
            return area;
        } catch (Throwable t) {
            ErrorDebug.note("Whereabouts#now", null, t);
            return null;
        }
    }

    private static String raid() {
        RaidInfo raid = Models.Raid.getCurrentRaid();
        if (raid == null || raid.getRaidKind() == null) {
            return null;
        }
        String name = "Raid · " + raid.getRaidKind().getRaidName();
        RaidRoomInfo room = raid.getCurrentRoom();
        String roomName = room == null ? null : room.getRoomName();
        return roomName == null || roomName.isBlank() ? name : name + " · " + roomName.strip();
    }

    private static String area() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return null;
        }
        TerritoryProfile here = Models.Territory.getTerritoryProfileForPosition(mc.player.position());
        return here == null ? null : here.getName();
    }
}
