package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.Credits;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class Badges {

    public static final String ICON = "";

    public static final int INSTALLED = 0xA8A8A8;

    static final FontDescription FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID, "nametag"));

    private static final long RAINBOW_MS = 3000;
    private static final int RAINBOW_STEPS = 60;

    public record Role(String id, String name, int color, boolean rainbow, boolean hidden) {}

    private record Table(Map<String, List<Role>> byName, Map<String, Pattern> patterns) {}

    private static volatile Table table;

    private Badges() {}

    public static void reload() {
        table = null;
    }

    private static Table table() {
        Table now = table;
        if (now == null) {
            now = build(Credits.sections());
            table = now;
        }
        return now;
    }

    static void load(List<Credits.Section> sections) {
        table = build(sections);
    }

    private static Table build(List<Credits.Section> sections) {
        Map<String, List<Role>> roles = new LinkedHashMap<>();
        Map<String, Pattern> pats = new LinkedHashMap<>();
        for (Credits.Section section : sections) {
            for (Credits.Member member : section.members()) {
                if (!member.hasHead()) {
                    continue;
                }
                String key = member.mc().toLowerCase(Locale.ROOT);
                roles.computeIfAbsent(key, k -> new ArrayList<>()).add(new Role(
                        section.id(), section.role(),
                        member.colour() != 0 ? member.colour() : section.color(),
                        member.rainbow(), member.hidden()));
                pats.computeIfAbsent(key, k -> Pattern.compile(
                        "(?i)\\b" + Pattern.quote(member.mc()) + "\\b"));
            }
        }
        return new Table(roles, pats);
    }

    public static List<Role> registered(String account) {
        List<Role> roles = account == null
                ? null : table().byName().get(account.toLowerCase(Locale.ROOT));
        return roles == null ? List.of() : roles;
    }

    public static List<Role> shown(String account, String self, CollectorConfig config) {
        return shown(account, self, config, com.wynnchayuan.Presence.BadgeChoice.NONE);
    }

    public static List<Role> shown(String account, String self, CollectorConfig config,
                                   com.wynnchayuan.Presence.BadgeChoice theirs) {
        boolean mine = account != null && account.equalsIgnoreCase(self);
        List<Role> all = new ArrayList<>();
        List<Role> out = new ArrayList<>();
        for (Role role : registered(account)) {
            boolean rainbow = mine ? config.badgeRainbow(role.rainbow()) && rainbowAllowed(role)
                    : role.rainbow() || (theirs.rainbow() && rainbowAllowed(role));
            Role resolved = new Role(role.id(), role.name(),
                    mine ? ownColour(config, role) : role.color(), rainbow, false);
            all.add(resolved);
            boolean visible = mine ? config.badgeRoleShown(role.id(), !role.hidden())
                    : theirs.shown() != null ? theirs.shown().contains(role.id()) : !role.hidden();
            if (visible) {
                out.add(resolved);
            }
        }
        if (out.isEmpty() && !all.isEmpty()) {
            out.add(all.get(0));
        }
        return out;
    }

    private static int ownColour(CollectorConfig config, Role role) {
        String hex = config.badgeColour(role.id());
        if (hex != null) {
            try {
                return Integer.parseInt(hex.replace("#", "").strip(), 16) & 0xFFFFFF;
            } catch (NumberFormatException e) {
            }
        }
        return role.color();
    }

    public static boolean ownRainbow(CollectorConfig config) {
        for (Role role : registered(selfName())) {
            if (rainbowAllowed(role) && config.badgeRainbow(role.rainbow())) {
                return true;
            }
        }
        return false;
    }

    public static List<String> ownShown(CollectorConfig config) {
        List<Role> mine = registered(selfName());
        if (mine.isEmpty()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (Role role : mine) {
            if (config.badgeRoleShown(role.id(), !role.hidden())) {
                out.add(role.id());
            }
        }
        if (out.isEmpty()) {
            out.add(mine.get(0).id());
        }
        return out;
    }

    public static boolean rainbowAllowed(Role role) {
        return "developers".equals(role.id());
    }

    static boolean isInstalled(String account, String self) {
        return account != null && account.equalsIgnoreCase(self);
    }

    public static Component decorate(Component nameTag, int entityId) {
        java.util.UUID uuid = uuidOf(entityId);
        return decorate(nameTag, accountName(entityId), selfName(), WynnChaYuan.config(),
                System.currentTimeMillis(), com.wynnchayuan.Presence.installed(uuid),
                com.wynnchayuan.Presence.badgeChoice(uuid));
    }

    static Component decorate(Component nameTag, String account, String self,
                              CollectorConfig config, long now) {
        return decorate(nameTag, account, self, config, now, false);
    }

    static Component decorate(Component nameTag, String account, String self,
                              CollectorConfig config, long now, boolean onRoster) {
        return decorate(nameTag, account, self, config, now, onRoster,
                com.wynnchayuan.Presence.BadgeChoice.NONE);
    }

    static Component decorate(Component nameTag, String account, String self,
                              CollectorConfig config, long now, boolean onRoster,
                              com.wynnchayuan.Presence.BadgeChoice theirs) {
        if (nameTag == null || startsWithIcon(nameTag)) {
            return null;
        }
        String who = account != null ? account : accountIn(nameTag.getString());
        List<Role> roles = shown(who, self, config, theirs);
        MutableComponent icons = Component.empty();
        if (!roles.isEmpty()) {
            for (Role role : roles) {
                icons.append(icon(role.rainbow() ? rainbowAt(now) : role.color()));
            }
        } else if (onRoster || isInstalled(who, self)) {
            icons.append(icon(INSTALLED));
        } else {
            return null;
        }
        return Component.empty().append(icons).append(" ").append(nameTag);
    }

    static Component icon(int rgb) {
        return Component.literal(ICON).withStyle(
                Style.EMPTY.withFont(FONT).withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
    }

    static boolean startsWithIcon(Component nameTag) {
        Optional<Boolean> first = nameTag.visit((style, text) -> text.isEmpty()
                ? Optional.empty()
                : Optional.of(text.startsWith(ICON) && FONT.equals(style.getFont())), Style.EMPTY);
        return first.orElse(false);
    }

    // WynnMod 以名牌內容當快取鍵，連續變色會每幀重算
    static int rainbowAt(long now) {
        long step = Math.floorMod(now, RAINBOW_MS) * RAINBOW_STEPS / RAINBOW_MS;
        return hsv((float) step / RAINBOW_STEPS, 0.7f, 1.0f);
    }

    private static String accountIn(String plain) {
        for (Map.Entry<String, Pattern> e : table().patterns().entrySet()) {
            if (e.getValue().matcher(plain).find()) {
                return e.getKey();
            }
        }
        return null;
    }

    public static String selfName() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc == null ? null : mc.getUser().getName();
        } catch (Throwable t) {
            ErrorDebug.note("Badges#selfName", null, t);
            return null;
        }
    }

    private static java.util.UUID uuidOf(int entityId) {
        if (entityId < 0) {
            return null;
        }
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.world.entity.Entity entity =
                    mc == null || mc.level == null ? null : mc.level.getEntity(entityId);
            return entity instanceof net.minecraft.world.entity.player.Player player
                    ? player.getUUID() : null;
        } catch (Throwable t) {
            ErrorDebug.note("Badges#uuidOf", null, t);
            return null;
        }
    }

    private static String accountName(int entityId) {
        if (entityId < 0) {
            return null;
        }
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.level == null) {
                return null;
            }
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(entityId);
            return entity instanceof net.minecraft.world.entity.player.Player player
                    ? player.getGameProfile().name() : null;
        } catch (Throwable t) {
            ErrorDebug.note("Badges#accountName", null, t);
            return null;
        }
    }

    private static int hsv(float h, float s, float v) {
        int sector = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        int p = (int) (v * (1 - s) * 255);
        int q = (int) (v * (1 - f * s) * 255);
        int t = (int) (v * (1 - (1 - f) * s) * 255);
        int full = (int) (v * 255);
        return switch (sector) {
            case 0 -> (full << 16) | (t << 8) | p;
            case 1 -> (q << 16) | (full << 8) | p;
            case 2 -> (p << 16) | (full << 8) | t;
            case 3 -> (p << 16) | (q << 8) | full;
            case 4 -> (t << 16) | (p << 8) | full;
            default -> (full << 16) | (p << 8) | q;
        };
    }
}
