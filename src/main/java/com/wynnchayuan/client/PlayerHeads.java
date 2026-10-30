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

public final class PlayerHeads {

    private static final Map<String, Supplier<PlayerSkin>> LOOKUPS = new ConcurrentHashMap<>();

    private static final Map<String, Boolean> PENDING = new ConcurrentHashMap<>();

    private PlayerHeads() {}

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
                        return null;
                    });
        } catch (Throwable t) {
        }
    }
}
