package com.wynnchayuan;

import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.RemoteSync;

public final class ModNetwork {

    private ModNetwork() {}

    public static void resyncTranslations(java.util.function.Consumer<String> done) {
        Thread worker = new Thread(() -> {
            String remote = RemoteSync.remoteVersion();
            LanguageLayers.fetchCurrentLanguages();
            String result = RemoteSync.lastResult();
            com.wynnchayuan.translate.TranslationUpdate.done(remote);
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                LanguageLayers.reloadTranslations();
                done.accept(com.wynnchayuan.client.T.s("status.sync_total", result, WynnChaYuan.translations.size()));
            });
        }, WynnChaYuan.MOD_ID + "-resync");
        worker.setDaemon(true);
        worker.start();
    }

    public static void recheckTranslationVersion(Runnable done) {
        Thread worker = new Thread(() -> {
            String remote = RemoteSync.remoteVersion();
            com.wynnchayuan.translate.TranslationUpdate.checked(remote);
            if (remote != null && !remote.equals(WynnChaYuan.config.syncedTranslations())) {
                com.wynnchayuan.translate.TranslationUpdate.found(remote);
            }
            net.minecraft.client.Minecraft.getInstance().execute(done);
        }, WynnChaYuan.MOD_ID + "-version");
        worker.setDaemon(true);
        worker.start();
    }

    private static void startBackgroundSync() {
        if (WynnChaYuan.config.source() != CollectorConfig.Source.GITHUB) {
            return;
        }
        Thread sync = new Thread(() -> {
            String remote = RemoteSync.remoteVersion();
            com.wynnchayuan.translate.TranslationUpdate.checked(remote);
            boolean first = WynnChaYuan.config.syncedTranslations().isBlank();
            if (!first && !WynnChaYuan.config.autoUpdateTranslations()) {
                if (remote != null && !remote.equals(WynnChaYuan.config.syncedTranslations())) {
                    com.wynnchayuan.translate.TranslationUpdate.found(remote);
                }
                return;
            }
            int changed = LanguageLayers.fetchCurrentLanguages();
            com.wynnchayuan.translate.TranslationUpdate.done(remote);
            if (changed > 0) {
                LanguageLayers.reloadOnMainThread();
            }
            ModLog.info(RemoteSync.lastResult());
        }, WynnChaYuan.MOD_ID + "-sync");
        sync.setDaemon(true);
        sync.start();
    }

    public static void startNetworked() {
        NetworkGate.start(WynnChaYuan.config, new NetworkGate.Services() {
            @Override
            public void updateCheck() {
                Releases.fetchLatest();
            }

            @Override
            public void translationSync() {
                startBackgroundSync();
            }

            @Override
            public void deleteData() {
                deleteServerData(null);
            }
        });
    }

    public static void setConsent(boolean agreed) {
        WynnChaYuan.config.setConsent(agreed ? CollectorConfig.Consent.AGREED : CollectorConfig.Consent.DECLINED);
        if (agreed) {
            startNetworked();
        } else {
            Presence.forget();
        }
    }

    public static void setDataOptOut(boolean on,
                                     java.util.function.Consumer<Presence.DeleteResult> done) {
        WynnChaYuan.config.setDataOptOut(on);
        if (!on) {
            return;
        }
        Presence.forget();
        try {
            com.wynnchayuan.cape.Capes.forgetSelf(
                    net.minecraft.client.Minecraft.getInstance().getUser().getProfileId());
        } catch (Throwable t) {
            com.wynnchayuan.translate.ErrorDebug.note("privacy.capes", null, t);
        }
        deleteServerData(done);
    }

    private static void deleteServerData(java.util.function.Consumer<Presence.DeleteResult> done) {
        Presence.deleteMyData(result -> {
            if (result == Presence.DeleteResult.DONE && WynnChaYuan.config.dataOptOut()) {
                WynnChaYuan.config.dataDeleted();
            }
            ModLog.info("data deletion: " + result);
            if (done != null) {
                net.minecraft.client.Minecraft.getInstance().execute(() -> done.accept(result));
            }
        });
    }
}
