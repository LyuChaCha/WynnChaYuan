package com.wynnchayuan;

import com.wynnchayuan.cape.Capes;
import com.wynnchayuan.client.T;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.RemoteSync;
import com.wynnchayuan.translate.TranslationUpdate;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public final class ModNetwork {

    private ModNetwork() {}

    public static void resyncTranslations(Consumer<String> done) {
        Thread worker = new Thread(() -> {
            String remote = RemoteSync.remoteVersion();
            LanguageLayers.fetchCurrentLanguages();
            String result = RemoteSync.lastResult();
            TranslationUpdate.done(remote);
            Minecraft.getInstance().execute(() -> {
                LanguageLayers.reloadTranslations();
                done.accept(T.s("status.sync_total", result, WynnChaYuan.translations.size()));
            });
        }, WynnChaYuan.MOD_ID + "-resync");
        worker.setDaemon(true);
        worker.start();
    }

    public static void recheckTranslationVersion(Runnable done) {
        Thread worker = new Thread(() -> {
            String remote = RemoteSync.remoteVersion();
            TranslationUpdate.checked(remote);
            if (remote != null && !remote.equals(WynnChaYuan.config.syncedTranslations())) {
                TranslationUpdate.found(remote);
            }
            Minecraft.getInstance().execute(done);
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
            TranslationUpdate.checked(remote);
            boolean first = WynnChaYuan.config.syncedTranslations().isBlank();
            if (!first && !WynnChaYuan.config.autoUpdateTranslations()) {
                if (remote != null && !remote.equals(WynnChaYuan.config.syncedTranslations())) {
                    TranslationUpdate.found(remote);
                }
                return;
            }
            int changed = LanguageLayers.fetchCurrentLanguages();
            TranslationUpdate.done(remote);
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
            PresenceAccount.forget();
        }
    }

    public static void setDataOptOut(boolean on,
                                     Consumer<PresenceAccount.DeleteResult> done) {
        WynnChaYuan.config.setDataOptOut(on);
        if (!on) {
            return;
        }
        PresenceAccount.forget();
        try {
            Capes.forgetSelf(Minecraft.getInstance().getUser().getProfileId());
        } catch (Throwable t) {
            ErrorDebug.note("privacy.capes", null, t);
        }
        deleteServerData(done);
    }

    private static void deleteServerData(Consumer<PresenceAccount.DeleteResult> done) {
        PresenceAccount.deleteMyData(result -> {
            if (result == PresenceAccount.DeleteResult.DONE && WynnChaYuan.config.dataOptOut()) {
                WynnChaYuan.config.dataDeleted();
            }
            ModLog.info("刪資料：" + result);
            if (done != null) {
                Minecraft.getInstance().execute(() -> done.accept(result));
            }
        });
    }
}
