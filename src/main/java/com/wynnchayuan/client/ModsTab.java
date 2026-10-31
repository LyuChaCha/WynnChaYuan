package com.wynnchayuan.client;

import com.wynnchayuan.NetworkGate;
import com.wynnchayuan.Presence;
import com.wynnchayuan.PresenceAccount;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.cape.Capes;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.render.Badges;
import com.wynnchayuan.render.WynnModTooltip;
import com.wynnchayuan.translate.WmsBridge;
import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ModsTab {

    private final SettingsScreen screen;

    ModsTab(SettingsScreen screen) {
        this.screen = screen;
    }

    List<Row> mods() {
        List<Row> rows = new ArrayList<>();
        rows.add(SettingsScreen.toggle("world.wynntils", true, SettingsScreen.cfg()::wynntilsUi, SettingsScreen.cfg()::toggleWynntilsUi));
        if (WmsBridge.installed()) {
            rows.add(SettingsScreen.toggle("items.wms", true, SettingsScreen.cfg()::wynnMarketSearch,
                    SettingsScreen.cfg()::toggleWynnMarketSearch));
        }
        if (WynnventoryBridge.installed()) {
            rows.add(SettingsScreen.toggle("items.wynnventory", true, SettingsScreen.cfg()::wynnventory, SettingsScreen.cfg()::toggleWynnventory));
        }
        if (WynnModTooltip.installed()) {
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
        String self = Badges.selfName();
        UUID me = screen.client() == null ? null : screen.client().getUser().getProfileId();
        if (!SettingsScreen.cfg().ownCape() || Capes.hasCape(self, me)) {
            rows.add(SettingsScreen.toggle("fun.owncape", true, SettingsScreen.cfg()::ownCape, SettingsScreen.cfg()::toggleOwnCape));
        } else {
            rows.add(Row.status("fun.nocape", SettingsScreen.t("fun.nocape"), SettingsScreen.t("fun.nocape.hint"))
                    .label(SettingsScreen.t("fun.nocape.label")));
        }
        if (!NetworkGate.registry()) {
            rows.add(Row.status("fun.offline", SettingsScreen.t("fun.offline"), SettingsScreen.t("fun.offline.hint"))
                    .label(SettingsScreen.t("fun.offline.label")));
            rows.add(Row.action("fun.gallery", SettingsScreen.t("fun.gallery"), SettingsScreen.t("fun.gallery.hint"))
                    .label(SettingsScreen.t("fun.gallery.button"))
                    .run(() -> ConfirmLinkScreen.confirmLinkNow(screen,
                            Presence.GALLERY_URL)));
            return rows;
        }
        rows.add(Row.action("fun.upload", SettingsScreen.t("fun.upload"), SettingsScreen.t("fun.upload.hint"))
                .label(SettingsScreen.t("fun.upload.button"))
                .run(() -> PresenceAccount.site("/account", true,
                        url -> Minecraft.getInstance().execute(
                                () -> ConfirmLinkScreen.confirmLinkNow(screen, url)))));
        rows.add(Row.action("fun.remove", SettingsScreen.t("fun.remove"), SettingsScreen.t("fun.remove.hint"))
                .label(SettingsScreen.t("fun.remove.button"))
                .run(() -> PresenceAccount.remove(screen.privacyTab::removed)));
        rows.add(Row.action("fun.gallery", SettingsScreen.t("fun.gallery"), SettingsScreen.t("fun.gallery.hint"))
                .label(SettingsScreen.t("fun.gallery.button"))
                .run(() -> PresenceAccount.gallery(true, url -> Minecraft.getInstance().execute(
                                () -> ConfirmLinkScreen.confirmLinkNow(screen, url)))));
        return rows;
    }
}
