package com.wynnchayuan;

import com.wynnchayuan.capture.UnappliedAudit;
import com.wynnchayuan.client.Credits;
import com.wynnchayuan.client.T;
import com.wynnchayuan.render.DialogueTint;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.FetchedLanguages;
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
import java.util.function.Consumer;

public final class LanguageLayers {

    private LanguageLayers() {}

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
        List<Path> layers = new ArrayList<>();
        String under = fallbackLanguage();
        if (under != null) {
            Path fallback = Languages.dir(WynnChaYuan.configDir, under);
            StarterFiles.installIfEmpty(fallback, under);
            layers.add(fallback);
        }
        Path dir = Languages.dir(WynnChaYuan.configDir, WynnChaYuan.language);
        StarterFiles.installIfEmpty(dir, WynnChaYuan.language);
        layers.add(dir);
        TranslationCache.loadRepairing(WynnChaYuan.translations, layers);
        loadReferenceKeys(layers);
        UnappliedAudit.clear();
    }

    static int fetchCurrentLanguages() {
        String lang = WynnChaYuan.language;
        FetchedLanguages.mark(lang);
        int changed = RemoteSync.fetchInto(
                Languages.dir(WynnChaYuan.configDir, lang), lang);
        String under = Languages.underlay(WynnChaYuan.config.fallbackLanguage(), lang);
        if (under != null) {
            FetchedLanguages.mark(under);
            changed += RemoteSync.fetchInto(
                    Languages.dir(WynnChaYuan.configDir, under), under);
        }
        if (RemoteSync.fetchCredits(WynnChaYuan.configDir)) {
            Credits.reload();
        }
        return changed;
    }

    private static void refreshOncePerSession(String lang) {
        if (WynnChaYuan.config.source() != CollectorConfig.Source.GITHUB
                || !WynnChaYuan.config.autoUpdateTranslations()
                || !FetchedLanguages.firstTime(lang)) {
            return;
        }
        Path dir = Languages.dir(WynnChaYuan.configDir, lang);
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
            Minecraft.getInstance().execute(LanguageLayers::loadLayers);
        } catch (Throwable t) {
            ModLog.warn("同步完排不進主執行緒，下次啟動才會套用：" + t);
        }
    }

    public static String fallbackLanguage() {
        return Languages.underlay(WynnChaYuan.config.fallbackLanguage(), WynnChaYuan.language);
    }

    public static void switchFallback(String lang,
                                      Consumer<String> done) {
        switchFallback(lang, done, null);
    }

    public static void switchFallback(String lang,
                                      Consumer<String> done, RemoteSync.Progress progress) {
        WynnChaYuan.config.setFallbackLanguage(lang);
        String wanted = fallbackLanguage();
        if (wanted != null) {
            TranslationCache.prepare(Languages.dir(WynnChaYuan.configDir, wanted), wanted);
        }
        loadLayers();
        String under = fallbackLanguage();
        if (under == null || WynnChaYuan.config.source() != CollectorConfig.Source.GITHUB) {
            done.accept(T.s("data.fallback.done", WynnChaYuan.translations.size()));
            return;
        }
        Path dir = Languages.dir(WynnChaYuan.configDir, under);
        FetchedLanguages.mark(under);
        Thread worker = new Thread(() -> {
            RemoteSync.fetchInto(dir, under, progress);
            Minecraft.getInstance().execute(() -> {
                loadLayers();
                done.accept(T.s("data.fallback.done", WynnChaYuan.translations.size()));
            });
        }, WynnChaYuan.MOD_ID + "-fallback");
        worker.setDaemon(true);
        worker.start();
    }

    public static final String OFF = Languages.OFF;

    public static void switchLanguage(String lang,
                                      Consumer<String> done) {
        switchLanguage(lang, done, null);
    }

    public static void switchLanguage(String lang,
                                      Consumer<String> done, RemoteSync.Progress progress) {
        WynnChaYuan.config.setLanguage(lang);
        WynnChaYuan.language = Languages.pick(WynnChaYuan.config.language(), gameLanguage());
        DialogueTint.init(WynnChaYuan.configDir.resolve(DialogueTint.FILE), WynnChaYuan.language);
        Path chosen = Languages.dir(WynnChaYuan.configDir, WynnChaYuan.language);
        TranslationCache.prepare(chosen, WynnChaYuan.language);
        loadLayers();
        refreshOncePerSession(WynnChaYuan.language);
        done.accept(T.s("data.language.done", Languages.nativeName(WynnChaYuan.language),
                WynnChaYuan.translations.size()));
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
}
