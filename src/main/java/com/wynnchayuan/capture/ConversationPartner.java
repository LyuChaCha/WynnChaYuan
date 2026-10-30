package com.wynnchayuan.capture;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ConversationPartner {

    static final long START_MS = 4_000;

    static final long IDLE_MS = 20_000;

    private static volatile String name;
    private static volatile long clickedAt;
    private static volatile long lastLineAt;
    private static volatile boolean talking;

    private ConversationPartner() {}

    public static void clicked(Entity entity) {
        String raw = com.wynnchayuan.render.LookAtTranslator.labelAbove(entity);
        String found = DialogueSpeaker.cleanName(raw);
        if (found != null && isRealPlayer(entity, found)) {
            note("partner.reject.player");
            found = null;
        }
        if (found != null && (PlayerDataFilter.carriesPlayerData(found)
                || SelfNames.all().contains(found))) {
            note("partner.reject.filter");
            found = null;
        }
        note(found == null ? "partner.unset" : "partner.set");
        name = found;
        blockAt = 0;
        clickedAt = System.currentTimeMillis();
        talking = false;
    }

    public static void blockClicked() {
        clear();
        blockAt = System.currentTimeMillis();
    }

    public static boolean fromBlock() {
        return name == null && System.currentTimeMillis() - blockAt <= IDLE_MS;
    }

    private static volatile long blockAt;

    public static void clear() {
        if (name != null) {
            note("partner.cleared");
        }
        name = null;
        talking = false;
    }

    public static String forLine() {
        String current = name;
        if (current == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        boolean expired = talking
                ? now - lastLineAt > IDLE_MS
                : now - clickedAt > START_MS;
        if (expired) {
            note("partner.expired");
            clear();
            return null;
        }
        if (!talking) {
            note("partner.used");
        }
        talking = true;
        lastLineAt = now;
        return current;
    }

    private static void note(String event) {
        try {
            com.wynnchayuan.WynnChaYuan.store().noteEvent(event);
        } catch (Throwable ignored) {
        }
    }

    // Wynncraft 的 NPC 也是玩家實體，tab 名單也有假條目
    private static boolean isRealPlayer(Entity entity, String label) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        try {
            String account = player.getGameProfile().name();
            if (account == null || account.isBlank()) {
                return false;
            }
            String a = account.strip().toLowerCase(java.util.Locale.ROOT);
            String l = label.toLowerCase(java.util.Locale.ROOT);
            return l.equals(a) || (a.length() >= 3 && l.contains(a));
        } catch (Throwable t) {
            return true;
        }
    }
}
