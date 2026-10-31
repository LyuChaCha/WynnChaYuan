package com.wynnchayuan.client;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.LanguageLayers;
import com.wynnchayuan.ModNetwork;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.CorpusExport;
import com.wynnchayuan.capture.OwnOutputs;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.translate.RemoteSync;
import com.wynnchayuan.translate.TranslationUpdate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ToolsTab {

    private final SettingsScreen screen;

    ToolsTab(SettingsScreen screen) {
        this.screen = screen;
    }

    boolean fetching = false;

    private boolean checkingVersion;

    static boolean github() {
        return SettingsScreen.cfg().source() == ModConfig.Source.GITHUB;
    }

    List<Row> tools() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.segment("data.source", SettingsScreen.t("data.source"), SettingsScreen.t("data.source.hint"))
                .option(T.s("data.source.github"), Row.Tone.PLAIN)
                .option(T.s("data.source.local"), Row.Tone.PLAIN)
                .selected(() -> SettingsScreen.cfg().source().ordinal())
                .pick(i -> SettingsScreen.cycleTo(SettingsScreen.cfg()::source, SettingsScreen.cfg()::toggleSource,
                        ModConfig.Source.values()[i]))
                .reset(ToolsTab::github, () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::source, SettingsScreen.cfg()::toggleSource,
                        ModConfig.Source.GITHUB)));
        rows.add(SettingsScreen.toggle("data.autoupdate", false, SettingsScreen.cfg()::autoUpdateTranslations,
                SettingsScreen.cfg()::toggleAutoUpdateTranslations));
        rows.add(Row.action("data.reload", SettingsScreen.t("data.reload"),
                        () -> T.s(github() ? "data.reload.github" : "data.reload.local"))
                .label(() -> T.s(github() ? "data.reload.fetch" : "data.reload.reread"))
                .enabled(() -> !fetching)
                .run(this::reload));
        rows.add(Row.status("data.version", SettingsScreen.t("data.version"), this::versionHint)
                .label(() -> T.s(VERSION_KEY.get(versionState())))
                .labelColour(() -> switch (versionState()) {
                    case LATEST -> Ui.GREEN;
                    case BEHIND -> Ui.AMBER;
                    case UNKNOWN -> Ui.RED;
                    default -> Ui.TEXT_2;
                })
                .run(this::recheckVersion));
        rows.add(Row.action("data.export", SettingsScreen.t("data.export"), SettingsScreen.t("data.export.hint"))
                .label(SettingsScreen.t("data.export.button"))
                .run(this::exportCorpus));
        rows.add(Row.action("data.submit", SettingsScreen.t("data.submit"), SettingsScreen.t("data.submit.hint"))
                .label(SettingsScreen.t("data.submit.button"))
                .run(() -> ConfirmLinkScreen.confirmLinkNow(screen, CorpusExport.ISSUE_URL)));
        return rows;
    }

    TranslationUpdate.State versionState() {
        return TranslationUpdate.verdict(github(), checkingVersion, TranslationUpdate.asked(),
                SettingsScreen.cfg().syncedTranslations(), TranslationUpdate.seen());
    }

    static final Map<TranslationUpdate.State, String> VERSION_KEY = Map.of(
                    TranslationUpdate.State.LOCAL, "data.version.local",
                    TranslationUpdate.State.CHECKING, "data.version.checking",
                    TranslationUpdate.State.LATEST, "data.version.latest",
                    TranslationUpdate.State.BEHIND, "data.version.behind",
                    TranslationUpdate.State.UNKNOWN, "data.version.unknown");

    private static String shortVersion(String sha, String date) {
        return TranslationUpdate.label(sha, date, T.s("data.version.bundled"));
    }

    private String versionHint() {
        String local = shortVersion(SettingsScreen.cfg().syncedTranslations(), SettingsScreen.cfg().syncedTranslationsDate());
        return switch (versionState()) {
            case LOCAL -> T.s("data.version.local.hint");
            case CHECKING -> T.s("data.version.checking");
            case LATEST -> T.s("data.version.latest.hint", local);
            case BEHIND -> T.s("data.version.behind.hint", local,
                    shortVersion(TranslationUpdate.seen(), RemoteSync.lastRemoteDate()));
            case UNKNOWN -> T.s("data.version.unknown.hint", local);
        };
    }

    private void recheckVersion() {
        if (checkingVersion) {
            return;
        }
        if (!github()) {
            screen.say(T.s("data.version.local.hint"), Ui.TEXT_3);
            return;
        }
        checkingVersion = true;
        ModNetwork.recheckTranslationVersion(() -> {
            checkingVersion = false;
            screen.say(versionHint(),
                    versionState() == TranslationUpdate.State.LATEST ? Ui.GREEN : Ui.AMBER);
        });
    }

    private void reload() {
        if (github()) {
            screen.say(T.s("status.fetching"), Ui.TEXT_3);
            fetching = true;
            ModNetwork.resyncTranslations(result -> {
                fetching = false;
                report(result, WynnChaYuan.translations().size() > 0);
            });
            return;
        }
        LanguageLayers.reloadTranslations();
        report(WynnChaYuan.translations().lastResult(), WynnChaYuan.translations().size() > 0);
    }

    private void report(String result, boolean ok) {
        screen.say((ok ? "✔ " : "✘ ") + result, ok ? Ui.GREEN : Ui.RED);
        if (screen.client() != null && screen.client().player != null) {
            Component line = Component.literal("[WynnChaYuan] " + result)
                    .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
            OwnOutputs.note(line);
            screen.client().player.displayClientMessage(line, false);
        }
    }

    private void exportCorpus() {
        CaptureStore store = WynnChaYuan.store();
        Path dir = WynnChaYuan.configDir();
        if (store == null || dir == null) {
            return;
        }
        screen.say(T.s("data.export.working"), Ui.TEXT_3);
        Minecraft client = Minecraft.getInstance();
        Thread worker = new Thread(() -> {
            try {
                CorpusExport.Result result = CorpusExport.write(
                        dir, store, WynnChaYuan.version(), WynnChaYuan.language());
                client.execute(() -> {
                    if (result.count() == 0) {
                        screen.say(T.s("data.export.empty"), Ui.TEXT_3);
                    } else {
                        screen.say(T.s("data.export.done", result.count()), Ui.GREEN);
                    }
                    Util.getPlatform().openPath(result.file().getParent());
                });
            } catch (Exception e) {
                client.execute(() -> screen.say(T.s("data.export.failed", String.valueOf(e.getMessage())),
                        Ui.RED));
            }
        }, WynnChaYuan.MOD_ID + "-export");
        worker.setDaemon(true);
        worker.start();
    }
}
