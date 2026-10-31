package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.ModNetwork;
import com.wynnchayuan.Presence;
import com.wynnchayuan.PresenceAccount;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PrivacyTab {

    private final SettingsScreen screen;

    PrivacyTab(SettingsScreen screen) {
        this.screen = screen;
    }

    private static final long ARM_MS = 6000;

    private long armedAt;

    private boolean deleting;

    private boolean armed() {
        return System.currentTimeMillis() - armedAt < ARM_MS;
    }

    private void requestDelete() {
        deleting = true;
        ModNetwork.setDataOptOut(true, this::deleted);
        screen.say(T.s("privacy.deleting"), Ui.TEXT_3);
        screen.rebuild();
    }

    List<Row> privacy() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.toggle("privacy.online", SettingsScreen.t("privacy.online"), SettingsScreen.t("privacy.online.hint"))
                .on(() -> SettingsScreen.cfg().consent() == CollectorConfig.Consent.AGREED)
                .flip(() -> {
                    boolean agree = SettingsScreen.cfg().consent() != CollectorConfig.Consent.AGREED;
                    ModNetwork.setConsent(agree);
                    screen.say(T.s(agree ? "privacy.online.on" : "privacy.online.off"), Ui.GREEN);
                    screen.rebuild();
                }));
        rows.add(Row.action("privacy.policy", SettingsScreen.t("privacy.policy"), SettingsScreen.t("privacy.policy.hint"))
                .label(SettingsScreen.t("fun.gallery.button"))
                .run(() -> ConfirmLinkScreen.confirmLinkNow(screen,
                        Presence.PRIVACY_URL)));
        if (!SettingsScreen.cfg().dataOptOut()) {
            rows.add(Row.action("privacy.delete", SettingsScreen.t("privacy.delete"), SettingsScreen.t("privacy.delete.hint"))
                    .label(() -> T.s(armed() ? "privacy.delete.confirm" : "privacy.delete.button"))
                    .danger(this::armed)
                    .run(() -> {
                        if (armed()) {
                            armedAt = 0;
                            requestDelete();
                        } else {
                            armedAt = System.currentTimeMillis();
                            screen.say(T.s("privacy.delete.armed"), Ui.RED);
                        }
                    }));
            return rows;
        }
        rows.add(Row.status("privacy.delete", SettingsScreen.t("privacy.delete"), SettingsScreen.t("privacy.delete.hint"))
                .label(() -> T.s(deleting ? "privacy.delete.state.working"
                        : SettingsScreen.cfg().dataDeletePending() ? "privacy.delete.state.retry"
                        : "privacy.delete.state.done"))
                .labelColour(() -> deleting ? Ui.TEXT_3
                        : SettingsScreen.cfg().dataDeletePending() ? Ui.AMBER : Ui.GREEN)
                .run(() -> {
                    if (!deleting && SettingsScreen.cfg().dataDeletePending()) {
                        requestDelete();
                    }
                }));
        rows.add(Row.action("privacy.resume", SettingsScreen.t("privacy.resume"), SettingsScreen.t("privacy.resume.hint"))
                .label(SettingsScreen.t("privacy.resume.button"))
                .enabled(() -> !deleting)
                .run(() -> {
                    ModNetwork.setDataOptOut(false, null);
                    screen.say(T.s("privacy.resumed"), Ui.GREEN);
                    screen.rebuild();
                }));
        return rows;
    }

    private void deleted(PresenceAccount.DeleteResult result) {
        deleting = false;
        if (result == PresenceAccount.DeleteResult.DONE) {
            screen.say(T.s("privacy.deleted"), Ui.GREEN);
        } else {
            screen.say(T.s("privacy.delete.later"), Ui.AMBER);
        }
    }

    void removed(PresenceAccount.Result result) {
        Minecraft.getInstance().execute(() -> {
            if (result == PresenceAccount.Result.DONE) {
                screen.say(T.s("fun.remove.done"), Ui.GREEN);
            } else {
                screen.say(T.s("fun.upload." + result.name().toLowerCase(Locale.ROOT)), Ui.RED);
            }
        });
    }
}
