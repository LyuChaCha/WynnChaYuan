package com.wynnchayuan;

import com.wynnchayuan.capture.OwnOutputs;
import com.wynnchayuan.capture.UnappliedAudit;
import com.wynnchayuan.client.Credits;
import com.wynnchayuan.client.T;
import com.wynnchayuan.render.DialogueTint;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LanguagePack;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.RemoteSync;
import com.wynnchayuan.translate.StarterFiles;
import com.wynnchayuan.translate.TranslationCache;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class LanguageLayers {

    private LanguageLayers() {}

    public record Outcome(String text, boolean ok) {}

    public static void reloadTranslations() {
        loadLayers();
    }

    static boolean alreadyCollected(String template) {
        if (alreadyTranslated(template)) {
            return true;
        }
        Set<String> keys = referenceKeys;
        return keys != null && template != null && keys.contains(template.strip());
    }

    static boolean alreadyTranslated(String template) {
        return WynnChaYuan.translations.hasTranslation(template);
    }

    private static volatile Set<String> referenceKeys;

    private static void loadReferenceKeys(List<Path> layers) {
        String reference = Languages.DEFAULT;
        Path dir = Languages.dir(WynnChaYuan.configDir, reference);
        if (layers.contains(dir)) {
            referenceKeys = WynnChaYuan.translations.sourceKeys();
            ModLog.info("收集的判準用畫面上這幾層的 "
                    + referenceKeys.size() + " 條原文");
            return;
        }
        try {
            TranslationStore probe = new TranslationStore();
            probe.loadAll(dir);
            referenceKeys = probe.sourceKeys();
            ModLog.info("收集的判準用 " + reference
                    + " 的 " + referenceKeys.size() + " 條原文");
        } catch (Exception e) {
            ErrorDebug.note("LanguageLayers#loadReferenceKeys", null, e);
            referenceKeys = null;
        }
    }

    static void loadLayers() {
        WynnChaYuan.translations.setNameMode(WynnChaYuan.config.itemNames());
        WynnChaYuan.translations.setPageNameMode(WynnChaYuan.config.pageNames());
        WynnChaYuan.translations.setAbilityNamesWithOriginal(WynnChaYuan.config.abilityNamesOriginal());
        if (!WynnChaYuan.config.translationsEnabled()) {
            WynnChaYuan.translations.loadAll(List.of());
            UnappliedAudit.clear();
            return;
        }
        String under = fallbackLanguage();
        if (under != null) {
            StarterFiles.installIfEmpty(Languages.dir(WynnChaYuan.configDir, under), under);
        }
        StarterFiles.installIfEmpty(
                Languages.dir(WynnChaYuan.configDir, WynnChaYuan.language), WynnChaYuan.language);
        List<Path> layers = new ArrayList<>(
                LanguagePack.layers(WynnChaYuan.configDir, WynnChaYuan.language, under));
        TranslationCache.loadRepairing(WynnChaYuan.translations, layers);
        loadReferenceKeys(layers);
        UnappliedAudit.clear();
    }

    static int fetchCurrentLanguages() {
        String lang = WynnChaYuan.language;
        markFetched(lang);
        int changed = syncLanguage(lang);
        String under = Languages.underlay(WynnChaYuan.config.fallbackLanguage(), lang);
        if (under != null) {
            markFetched(under);
            changed += syncLanguage(under);
        }
        if (RemoteSync.fetchCredits(WynnChaYuan.configDir)) {
            Credits.reload();
        }
        return changed;
    }

    static boolean fetchMissingLanguages() {
        String lang = WynnChaYuan.language;
        boolean got = false;
        for (String one : new String[] {lang,
                Languages.underlay(WynnChaYuan.config.fallbackLanguage(), lang)}) {
            if (one != null && LanguagePack.needsDownload(WynnChaYuan.configDir, one)) {
                markFetched(one);
                got |= syncLanguage(one) > 0;
            }
        }
        return got;
    }

    private static int syncLanguage(String lang) {
        if (!LanguagePack.needsDownload(WynnChaYuan.configDir, lang)) {
            return RemoteSync.fetchInto(Languages.dir(WynnChaYuan.configDir, lang), lang);
        }
        if (downloadBlocked() != null) {
            return 0;
        }
        LanguagePack.Phase got = LanguagePack.download(
                WynnChaYuan.configDir, lang, RemoteSync::fetchComplete, null);
        if (got != LanguagePack.Phase.READY) {
            return 0;
        }
        OwnOutputs.buildAsync(WynnChaYuan.configDir);
        return 1;
    }

    public static String downloadBlocked() {
        if (WynnChaYuan.config.source() != ModConfig.Source.GITHUB) {
            return "data.language.local";
        }
        if (!NetworkGate.online(WynnChaYuan.config)) {
            return "data.language.offline";
        }
        return null;
    }

    private static void fetchMissing(String lang, RemoteSync.Progress progress,
                                     Consumer<Outcome> done) {
        String name = Languages.nativeName(lang);
        String blocked = downloadBlocked();
        if (blocked != null) {
            done.accept(new Outcome(T.s(blocked, name), false));
            return;
        }
        markFetched(lang);
        Thread worker = new Thread(() -> {
            LanguagePack.Phase got = LanguagePack.download(
                    WynnChaYuan.configDir, lang, RemoteSync::fetchComplete, progress);
            ModLog.info("下載 " + lang + "：" + got + "，" + RemoteSync.lastResult());
            Minecraft.getInstance().execute(() -> {
                if (got == LanguagePack.Phase.READY) {
                    loadLayers();
                    OwnOutputs.buildAsync(WynnChaYuan.configDir);
                    done.accept(new Outcome(T.s("data.language.downloaded", name,
                            WynnChaYuan.translations.size()), true));
                } else if (got == LanguagePack.Phase.DOWNLOADING) {
                    LanguagePack.Status at = LanguagePack.status(lang);
                    done.accept(new Outcome(T.s("data.language.downloading",
                            (at == null ? 0 : at.percent()) + "%"), true));
                } else {
                    done.accept(new Outcome(T.s("data.language.download_failed", name), false));
                }
            });
        }, WynnChaYuan.MOD_ID + "-download");
        worker.setDaemon(true);
        worker.start();
    }

    private static void refreshOncePerSession(String lang) {
        if (WynnChaYuan.config.source() != ModConfig.Source.GITHUB
                || !WynnChaYuan.config.autoUpdateTranslations()
                || !firstFetch(lang)) {
            return;
        }
        Thread worker = new Thread(() -> {
            int changed = syncLanguage(lang);
            ModLog.info("切到 " + lang + "：" + RemoteSync.lastResult());
            if (changed > 0) {
                reloadOnMainThread();
            }
        }, WynnChaYuan.MOD_ID + "-switch-sync");
        worker.setDaemon(true);
        worker.start();
    }

    static void reloadOnMainThread() {
        try {
            Minecraft.getInstance().execute(LanguageLayers::loadLayers);
        } catch (Throwable t) {
            ModLog.warn("同步完排不進主執行緒，下次啟動才會套用：" + t);
        }
    }

    public static String fallbackLanguage() {
        return Languages.underlay(WynnChaYuan.config.fallbackLanguage(), WynnChaYuan.language);
    }

    public static void switchFallback(String lang,
                                      Consumer<Outcome> done) {
        switchFallback(lang, done, null);
    }

    public static void switchFallback(String lang,
                                      Consumer<Outcome> done, RemoteSync.Progress progress) {
        WynnChaYuan.config.setFallbackLanguage(lang);
        String wanted = fallbackLanguage();
        if (wanted != null) {
            TranslationCache.prepare(Languages.dir(WynnChaYuan.configDir, wanted), wanted);
        }
        loadLayers();
        String under = fallbackLanguage();
        if (under != null && LanguagePack.needsDownload(WynnChaYuan.configDir, under)) {
            fetchMissing(under, progress, done);
            return;
        }
        if (under == null || WynnChaYuan.config.source() != ModConfig.Source.GITHUB) {
            done.accept(new Outcome(T.s("data.fallback.done", WynnChaYuan.translations.size()), true));
            return;
        }
        Path dir = Languages.dir(WynnChaYuan.configDir, under);
        markFetched(under);
        Thread worker = new Thread(() -> {
            RemoteSync.fetchInto(dir, under, progress);
            Minecraft.getInstance().execute(() -> {
                loadLayers();
                done.accept(new Outcome(
                        T.s("data.fallback.done", WynnChaYuan.translations.size()), true));
            });
        }, WynnChaYuan.MOD_ID + "-fallback");
        worker.setDaemon(true);
        worker.start();
    }

    public static final String OFF = Languages.OFF;

    public static void switchLanguage(String lang,
                                      Consumer<Outcome> done) {
        switchLanguage(lang, done, null);
    }

    public static void switchLanguage(String lang,
                                      Consumer<Outcome> done, RemoteSync.Progress progress) {
        WynnChaYuan.config.setLanguage(lang);
        WynnChaYuan.language = Languages.pick(WynnChaYuan.config.language(), gameLanguage());
        DialogueTint.init(WynnChaYuan.configDir.resolve(DialogueTint.FILE), WynnChaYuan.language);
        Path chosen = Languages.dir(WynnChaYuan.configDir, WynnChaYuan.language);
        TranslationCache.prepare(chosen, WynnChaYuan.language);
        loadLayers();
        if (WynnChaYuan.store() != null) {
            WynnChaYuan.store().useLanguage(WynnChaYuan.language);
        }
        if (LanguagePack.needsDownload(WynnChaYuan.configDir, WynnChaYuan.language)) {
            fetchMissing(WynnChaYuan.language, progress, done);
            return;
        }
        refreshOncePerSession(WynnChaYuan.language);
        done.accept(new Outcome(T.s("data.language.done", Languages.nativeName(WynnChaYuan.language),
                WynnChaYuan.translations.size()), true));
    }

    public static String autoLanguage() {
        return Languages.pick("", gameLanguage());
    }

    static void settleAutoLanguage() {
        if (!WynnChaYuan.config.language().isBlank()) {
            return;
        }
        String game = gameLanguage();
        if (!Languages.known(game)) {
            return;
        }
        String want = Languages.pick("", game);
        if (want.equals(WynnChaYuan.language)) {
            return;
        }
        WynnChaYuan.language = want;
        Path dir = Languages.dir(WynnChaYuan.configDir, WynnChaYuan.language);
        TranslationCache.prepare(dir, WynnChaYuan.language);
        String underneath = fallbackLanguage();
        if (underneath != null) {
            TranslationCache.prepare(Languages.dir(WynnChaYuan.configDir, underneath), underneath);
        }
        DialogueTint.init(WynnChaYuan.configDir.resolve(DialogueTint.FILE), WynnChaYuan.language);
        loadLayers();
        ModLog.info("跟隨遊戲語言：改用 " + WynnChaYuan.language
                + "（開機時問不到，先用了 " + Languages.DEFAULT
                + "）");
    }

    static String gameLanguage() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null || mc.getLanguageManager() == null
                    ? "" : mc.getLanguageManager().getSelected();
        } catch (Throwable t) {
            ErrorDebug.note("LanguageLayers#gameLanguage", null, t);
            return "";
        }
    }

    private static final Set<String> FETCHED = ConcurrentHashMap.newKeySet();

    public static void markFetched(String lang) {
        if (lang != null && !lang.isBlank()) {
            FETCHED.add(lang);
        }
    }

    public static boolean firstFetch(String lang) {
        return lang != null && !lang.isBlank() && FETCHED.add(lang);
    }

    public static void forgetFetched() {
        FETCHED.clear();
    }
}
