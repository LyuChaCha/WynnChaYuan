package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Row;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

import java.util.ArrayList;
import java.util.List;

final class ModsTab {

    private final SettingsScreen screen;

    ModsTab(SettingsScreen screen) {
        this.screen = screen;
    }

    List<Row> mods() {
        List<Row> rows = new ArrayList<>();
        rows.add(SettingsScreen.toggle("world.wynntils", true, SettingsScreen.cfg()::wynntilsUi, SettingsScreen.cfg()::toggleWynntilsUi));
        if (com.wynnchayuan.translate.WmsBridge.installed()) {
            rows.add(SettingsScreen.toggle("items.wms", true, SettingsScreen.cfg()::wynnMarketSearch,
                    SettingsScreen.cfg()::toggleWynnMarketSearch));
        }
        if (com.wynnchayuan.translate.WynnventoryBridge.installed()) {
            rows.add(SettingsScreen.toggle("items.wynnventory", true, SettingsScreen.cfg()::wynnventory, SettingsScreen.cfg()::toggleWynnventory));
        }
        if (com.wynnchayuan.render.WynnModTooltip.installed()) {
            rows.add(SettingsScreen.toggle("items.wynnmod", true, SettingsScreen.cfg()::wynnmodTooltip,
                    SettingsScreen.cfg()::toggleWynnmodTooltip));
        }
        return rows;
    }

    static final String CONTRIBUTE_URL =
            "https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md";

    static final String SPONSOR_URL =
            "https://github.com/LyuChaCha/WynnChaYuan#%E8%B4%8A%E5%8A%A9";

    List<Row> fun() {
        List<Row> rows = new ArrayList<>();
        rows.add(SettingsScreen.toggle("fun.capes", true, SettingsScreen.cfg()::capes, SettingsScreen.cfg()::toggleCapes));
        String self = com.wynnchayuan.render.Badges.selfName();
        java.util.UUID me = screen.client() == null ? null : screen.client().getUser().getProfileId();
        if (!SettingsScreen.cfg().ownCape() || com.wynnchayuan.cape.Capes.hasCape(self, me)) {
            rows.add(SettingsScreen.toggle("fun.owncape", true, SettingsScreen.cfg()::ownCape, SettingsScreen.cfg()::toggleOwnCape));
        } else {
            rows.add(Row.status("fun.nocape", SettingsScreen.t("fun.nocape"), SettingsScreen.t("fun.nocape.hint"))
                    .label(SettingsScreen.t("fun.nocape.label")));
        }
        if (!com.wynnchayuan.NetworkGate.registry()) {
            rows.add(Row.status("fun.offline", SettingsScreen.t("fun.offline"), SettingsScreen.t("fun.offline.hint"))
                    .label(SettingsScreen.t("fun.offline.label")));
            rows.add(Row.action("fun.gallery", SettingsScreen.t("fun.gallery"), SettingsScreen.t("fun.gallery.hint"))
                    .label(SettingsScreen.t("fun.gallery.button"))
                    .run(() -> ConfirmLinkScreen.confirmLinkNow(screen,
                            com.wynnchayuan.Presence.GALLERY_URL)));
            return rows;
        }
        rows.add(Row.action("fun.upload", SettingsScreen.t("fun.upload"), SettingsScreen.t("fun.upload.hint"))
                .label(SettingsScreen.t("fun.upload.button"))
                .run(() -> com.wynnchayuan.PresenceAccount.site("/account", true,
                        url -> net.minecraft.client.Minecraft.getInstance().execute(
                                () -> ConfirmLinkScreen.confirmLinkNow(screen, url)))));
        rows.add(Row.action("fun.remove", SettingsScreen.t("fun.remove"), SettingsScreen.t("fun.remove.hint"))
                .label(SettingsScreen.t("fun.remove.button"))
                .run(() -> com.wynnchayuan.PresenceAccount.remove(screen.privacyTab::removed)));
        rows.add(Row.action("fun.gallery", SettingsScreen.t("fun.gallery"), SettingsScreen.t("fun.gallery.hint"))
                .label(SettingsScreen.t("fun.gallery.button"))
                .run(() -> com.wynnchayuan.PresenceAccount.gallery(true,
                        url -> net.minecraft.client.Minecraft.getInstance().execute(
                                () -> ConfirmLinkScreen.confirmLinkNow(screen, url)))));
        return rows;
    }
}
