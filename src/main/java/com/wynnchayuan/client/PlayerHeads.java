package com.wynnchayuan.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 依 Minecraft ID 畫出玩家頭像。
 *
 * <h2>為什麼走原版的管線</h2>
 * 直接抓第三方頭像網站（mc-heads、crafatar 之類）比較短，但那等於把每位
 * 貢獻者的 ID 送給一個我們管不到的服務，而且那個服務哪天收掉，頭像就全沒了。
 *
 * <p>原版本來就有整套：{@link ResolvableProfile} 查 profile、
 * {@code SkinManager} 抓材質並快取到磁碟、{@link PlayerFaceRenderer} 畫臉。
 * 全都是對 Mojang 自己的伺服器，而且玩家本來就在用同一套快取。
 *
 * <h2>失敗就是預設皮膚，不是空白</h2>
 * 離線、查不到、ID 打錯——任何一種情況都退回 Steve/Alex 的頭。名單頁面
 * 不該因為抓不到圖就變成一排破格。
 */
public final class PlayerHeads {

    /** 查過的結果。{@code Supplier} 在材質下載完成前會先給預設皮膚。 */
    private static final Map<String, Supplier<PlayerSkin>> LOOKUPS = new ConcurrentHashMap<>();

    /** 正在查的，避免每一幀都重送一次請求。 */
    private static final Map<String, Boolean> PENDING = new ConcurrentHashMap<>();

    private PlayerHeads() {}

    /**
     * 畫一顆頭。第一次呼叫會在背景開始查，這一幀先畫預設皮膚。
     *
     * @param mcName Minecraft ID；{@code null} 或空字串就不畫
     */
    public static void draw(GuiGraphics g, String mcName, int x, int y, int size) {
        if (mcName == null || mcName.isBlank()) {
            return;
        }
        PlayerFaceRenderer.draw(g, skinOf(mcName), x, y, size);
    }

    private static PlayerSkin skinOf(String mcName) {
        Supplier<PlayerSkin> lookup = LOOKUPS.get(mcName);
        if (lookup != null) {
            return lookup.get();
        }
        request(mcName);
        return DefaultPlayerSkin.getDefaultSkin();
    }

    /** 查一次就好。查詢本身會連線，所以絕不能在繪製執行緒上做。 */
    private static void request(String mcName) {
        if (PENDING.putIfAbsent(mcName, Boolean.TRUE) != null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        try {
            ResolvableProfile.createUnresolved(mcName)
                    .resolveProfile(mc.services().profileResolver())
                    .thenAccept(profile -> {
                        if (profile != null) {
                            LOOKUPS.put(mcName, mc.getSkinManager().createLookup(profile, false));
                        }
                    })
                    .exceptionally(t -> {
                        // 查不到就一直是預設皮膚，不需要吵
                        return null;
                    });
        } catch (Throwable t) {
            // 這只是名單頁面的裝飾，任何情況都不該讓畫面掛掉
        }
    }
}
