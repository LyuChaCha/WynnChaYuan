package com.wynnchayuan.client;

import com.wynnchayuan.client.ui.Row;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

import java.util.ArrayList;
import java.util.List;

final class BadgeTab {

    private final SettingsScreen screen;

    BadgeTab(SettingsScreen screen) {
        this.screen = screen;
    }

    List<Row> badge() {
        List<Row> rows = new ArrayList<>();
        String self = com.wynnchayuan.render.Badges.selfName();
        List<com.wynnchayuan.render.Badges.Role> mine =
                com.wynnchayuan.render.Badges.registered(self);
        if (mine.isEmpty()) {
            rows.add(Row.status("badge.none", SettingsScreen.t("badge.none"), SettingsScreen.t("badge.none.hint"))
                    .label(SettingsScreen.t("badge.none.label"))
                    .labelColour(() -> com.wynnchayuan.render.Colors.opaque(
                            com.wynnchayuan.render.Badges.INSTALLED)));
            rows.add(Row.action("badge.contribute", SettingsScreen.t("badge.contribute"), SettingsScreen.t("badge.contribute.hint"))
                    .label(SettingsScreen.t("badge.contribute.button"))
                    .run(() -> ConfirmLinkScreen.confirmLinkNow(screen, ModsTab.CONTRIBUTE_URL)));
            rows.add(Row.action("badge.sponsor", SettingsScreen.t("badge.sponsor"), SettingsScreen.t("badge.sponsor.hint"))
                    .label(SettingsScreen.t("badge.sponsor.button"))
                    .run(() -> ConfirmLinkScreen.confirmLinkNow(screen, ModsTab.SPONSOR_URL)));
            return rows;
        }
        for (com.wynnchayuan.render.Badges.Role role : mine) {
            if (mine.size() > 1) {
                java.util.function.BooleanSupplier shown =
                        () -> SettingsScreen.cfg().badgeRoleShown(role.id(), !role.hidden());
                rows.add(Row.toggle("badge.role." + role.id(),
                                () -> T.s("badge.role", role.name()), SettingsScreen.t("badge.role.hint"))
                        .on(shown)
                        .flip(() -> {
                            if (!shown.getAsBoolean() || badgesShown(mine) > 1) {
                                SettingsScreen.cfg().setBadgeRole(role.id(), !shown.getAsBoolean());
                            }
                        })
                        .reset(() -> shown.getAsBoolean() == !role.hidden(),
                                () -> SettingsScreen.cfg().setBadgeRole(role.id(), !role.hidden())));
            }
            String registered = String.format("#%06X", role.color() & 0xFFFFFF);
            rows.add(Row.colour("badge.colour." + role.id(),
                            () -> T.s("badge.colour", role.name()), SettingsScreen.t("badge.colour.hint"))
                    .hex(() -> {
                        String own = SettingsScreen.cfg().badgeColour(role.id());
                        return own == null ? registered : own;
                    })
                    .setHex(hex -> SettingsScreen.cfg().setBadgeColourLive(role.id(), hex))
                    .commit(SettingsScreen.cfg()::saveIfDirty)
                    .reset(() -> SettingsScreen.cfg().badgeColour(role.id()) == null,
                            () -> SettingsScreen.cfg().clearBadgeColour(role.id())));
            if (com.wynnchayuan.render.Badges.rainbowAllowed(role)) {
                java.util.function.BooleanSupplier rainbow =
                        () -> SettingsScreen.cfg().badgeRainbow(role.rainbow());
                rows.add(Row.toggle("badge.rainbow", SettingsScreen.t("badge.rainbow"), SettingsScreen.t("badge.rainbow.hint"))
                        .on(rainbow)
                        .flip(() -> SettingsScreen.cfg().setBadgeRainbow(!rainbow.getAsBoolean()))
                        .reset(() -> rainbow.getAsBoolean() == role.rainbow(),
                                () -> SettingsScreen.cfg().setBadgeRainbow(role.rainbow())));
            }
        }
        return rows;
    }

    private int badgesShown(List<com.wynnchayuan.render.Badges.Role> mine) {
        int n = 0;
        for (com.wynnchayuan.render.Badges.Role role : mine) {
            if (SettingsScreen.cfg().badgeRoleShown(role.id(), !role.hidden())) {
                n++;
            }
        }
        return n;
    }
}
