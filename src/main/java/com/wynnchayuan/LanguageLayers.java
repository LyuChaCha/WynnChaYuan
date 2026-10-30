package com.wynnchayuan;

import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.RemoteSync;
import com.wynnchayuan.translate.StarterFiles;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.file.Path;

public final class LanguageLayers {

    private LanguageLayers() {}

    public static void reloadTranslations() {
        loadLayers();
    }

    static boolean alreadyCollected(String template) {
        if (alreadyTranslated(template)) {
            return true;
        }
        java.util.Set<String> keys = referenceKeys;
        return keys != null && template != null && keys.contains(template.strip());
    }

    static boolean alreadyTranslated(String template) {
        return WynnChaYuan.translations.hasTranslation(template);
    }

    private static volatile java.util.Set<String> referenceKeys;

    private static void loadReferenceKeys(java.util.List<Path> layers) {
        String reference = com.wynnchayuan.translate.Languages.DEFAULT;
        Path dir = com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, reference);
        if (layers.contains(dir)) {
            referenceKeys = WynnChaYuan.translations.sourceKeys();
            ModLog.info("收集的判準用畫面上這幾層的 "
                    + referenceKeys.size() + " 條原文");
            return;
        }
        try {
            com.wynnchayuan.translate.TranslationStore probe =
                    new com.wynnchayuan.translate.TranslationStore();
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
            WynnChaYuan.translations.loadAll(java.util.List.of());
            com.wynnchayuan.capture.UnappliedAudit.clear();
            return;
        }
        java.util.List<Path> layers = new java.util.ArrayList<>();
        String under = fallbackLanguage();
        if (under != null) {
            Path fallback = com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, under);
            StarterFiles.installIfEmpty(fallback, under);
            layers.add(fallback);
        }
        Path dir = com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, WynnChaYuan.language);
        StarterFiles.installIfEmpty(dir, WynnChaYuan.language);
        layers.add(dir);
        com.wynnchayuan.translate.TranslationCache.loadRepairing(WynnChaYuan.translations, layers);
        loadReferenceKeys(layers);
        com.wynnchayuan.capture.UnappliedAudit.clear();
    }

    static int fetchCurrentLanguages() {
        String lang = WynnChaYuan.language;
        com.wynnchayuan.translate.FetchedLanguages.mark(lang);
        int changed = RemoteSync.fetchInto(
                com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, lang), lang);
        String under = com.wynnchayuan.translate.Languages.underlay(WynnChaYuan.config.fallbackLanguage(), lang);
        if (under != null) {
            com.wynnchayuan.translate.FetchedLanguages.mark(under);
            changed += RemoteSync.fetchInto(
                    com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, under), under);
        }
        if (RemoteSync.fetchCredits(WynnChaYuan.configDir)) {
            com.wynnchayuan.client.Credits.reload();
        }
        return changed;
    }

    private static void refreshOncePerSession(String lang) {
        if (WynnChaYuan.config.source() != CollectorConfig.Source.GITHUB
                || !WynnChaYuan.config.autoUpdateTranslations()
                || !com.wynnchayuan.translate.FetchedLanguages.firstTime(lang)) {
            return;
        }
        Path dir = com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, lang);
        Thread worker = new Thread(() -> {
            int changed = RemoteSync.fetchInto(dir, lang);
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
            net.minecraft.client.Minecraft.getInstance().execute(LanguageLayers::loadLayers);
        } catch (Throwable t) {
            ModLog.warn("同步完排不進主執行緒，下次啟動才會套用：" + t);
        }
    }

    public static String fallbackLanguage() {
        return com.wynnchayuan.translate.Languages.underlay(WynnChaYuan.config.fallbackLanguage(), WynnChaYuan.language);
    }

    public static void switchFallback(String lang,
                                      java.util.function.Consumer<String> done) {
        switchFallback(lang, done, null);
    }

    public static void switchFallback(String lang,
                                      java.util.function.Consumer<String> done,
                                      RemoteSync.Progress progress) {
        WynnChaYuan.config.setFallbackLanguage(lang);
        String wanted = fallbackLanguage();
        if (wanted != null) {
            com.wynnchayuan.translate.TranslationCache.prepare(
                    com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, wanted), wanted);
        }
        loadLayers();
        String under = fallbackLanguage();
        if (under == null || WynnChaYuan.config.source() != CollectorConfig.Source.GITHUB) {
            done.accept(com.wynnchayuan.client.T.s("data.fallback.done",
                    WynnChaYuan.translations.size()));
            return;
        }
        Path dir = com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, under);
        com.wynnchayuan.translate.FetchedLanguages.mark(under);
        Thread worker = new Thread(() -> {
            RemoteSync.fetchInto(dir, under, progress);
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                loadLayers();
                done.accept(com.wynnchayuan.client.T.s("data.fallback.done",
                        WynnChaYuan.translations.size()));
            });
        }, WynnChaYuan.MOD_ID + "-fallback");
        worker.setDaemon(true);
        worker.start();
    }

    public static final String OFF = com.wynnchayuan.translate.Languages.OFF;

    public static void switchLanguage(String lang,
                                      java.util.function.Consumer<String> done) {
        switchLanguage(lang, done, null);
    }

    public static void switchLanguage(String lang,
                                      java.util.function.Consumer<String> done,
                                      RemoteSync.Progress progress) {
        WynnChaYuan.config.setLanguage(lang);
        WynnChaYuan.language = com.wynnchayuan.translate.Languages.pick(
                WynnChaYuan.config.language(), gameLanguage());
        com.wynnchayuan.render.DialogueTint.init(WynnChaYuan.configDir.resolve(
                com.wynnchayuan.render.DialogueTint.FILE), WynnChaYuan.language);
        Path chosen = com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, WynnChaYuan.language);
        com.wynnchayuan.translate.TranslationCache.prepare(chosen, WynnChaYuan.language);
        loadLayers();
        refreshOncePerSession(WynnChaYuan.language);
        done.accept(com.wynnchayuan.client.T.s("data.language.done",
                com.wynnchayuan.translate.Languages.nativeName(WynnChaYuan.language),
                WynnChaYuan.translations.size()));
    }

    public static String autoLanguage() {
        return com.wynnchayuan.translate.Languages.pick("", gameLanguage());
    }

    static void settleAutoLanguage() {
        if (!WynnChaYuan.config.language().isBlank()) {
            return;
        }
        String game = gameLanguage();
        if (!com.wynnchayuan.translate.Languages.known(game)) {
            return;
        }
        String want = com.wynnchayuan.translate.Languages.pick("", game);
        if (want.equals(WynnChaYuan.language)) {
            return;
        }
        WynnChaYuan.language = want;
        Path dir = com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, WynnChaYuan.language);
        com.wynnchayuan.translate.TranslationCache.prepare(dir, WynnChaYuan.language);
        String underneath = fallbackLanguage();
        if (underneath != null) {
            com.wynnchayuan.translate.TranslationCache.prepare(
                    com.wynnchayuan.translate.Languages.dir(WynnChaYuan.configDir, underneath),
                    underneath);
        }
        com.wynnchayuan.render.DialogueTint.init(
                WynnChaYuan.configDir.resolve(com.wynnchayuan.render.DialogueTint.FILE), WynnChaYuan.language);
        loadLayers();
        ModLog.info("跟隨遊戲語言：改用 " + WynnChaYuan.language
                + "（開機時問不到，先用了 " + com.wynnchayuan.translate.Languages.DEFAULT
                + "）");
    }

    static String gameLanguage() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc == null || mc.getLanguageManager() == null
                    ? "" : mc.getLanguageManager().getSelected();
        } catch (Throwable t) {
            ErrorDebug.note("LanguageLayers#gameLanguage", null, t);
            return "";
        }
    }
}
