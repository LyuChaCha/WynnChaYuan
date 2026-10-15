package com.wynnchayuan.capture;

import com.wynntils.core.components.Models;
import com.wynntils.models.lootrun.type.LootrunningState;
import com.wynntils.models.raid.type.RaidInfo;
import com.wynntils.models.raid.type.RaidRoomInfo;
import com.wynntils.models.territories.profile.TerritoryProfile;

import net.minecraft.client.Minecraft;

/**
 * 收到一句話的時候人在哪裡，寫進 captured.json 的 {@code where}。
 *
 * <p>漂浮字最需要：「Stand here to defend the elder」單看不知道是哪個討伐戰的
 * 哪一關，譯者只能猜語氣與用詞。有了「Raid · Nest of the Grootslangs · 第二關的名字」，
 * 同一關的幾句會排在一起，也知道要照討伐戰的用詞翻。
 *
 * <p>三種，照這個順序取第一個有的：
 * <ul>
 *   <li>討伐戰：{@code Raid · 名稱 · 房間}（房間是 Wynntils 從記分板讀到的名字）</li>
 *   <li>Lootrun：{@code Lootrun · 所在地區}</li>
 *   <li>其他：所在地區（公會領地的名字，Wynntils 的領地資料）</li>
 * </ul>
 *
 * <p>地區名是公開的地圖資料，不是玩家資料，匯出時可以留著。
 * 任何一步出錯都回 {@code null}：這只是給人看的附註，不能讓收集失敗。
 */
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
