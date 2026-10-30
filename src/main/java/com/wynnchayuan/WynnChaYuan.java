package com.wynnchayuan;

import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.listener.ActionBarListener;
import com.wynnchayuan.listener.BadgeListener;
import com.wynnchayuan.listener.ChatListener;
import com.wynnchayuan.listener.MarketListener;
import com.wynnchayuan.listener.TitleListener;
import com.wynnchayuan.listener.CaptureListener;
import com.wynnchayuan.listener.RenderListener;
import com.wynnchayuan.client.SettingsScreen;
import com.wynnchayuan.listener.ScoreboardListener;
import com.wynnchayuan.listener.TrackerListener;
import com.wynnchayuan.translate.RemoteSync;
import com.wynnchayuan.translate.StarterFiles;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.WynntilsMod;
import net.fabricmc.api.ClientModInitializer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class WynnChaYuan implements ClientModInitializer {

    public static final String MOD_ID = "wynnchayuan";
    public static final String MOD_NAME = "WynnChaYuan";

    public static String version() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }

    private static CaptureStore store;
    private static CaptureListener listener;
    private static ScheduledExecutorService flusher;
    private static CollectorConfig config;
    private static TranslationStore translations;
    private static KeyMapping openSettingsKey;

    private static KeyMapping screenshotKey;

    private static KeyMapping copyChatKey;
    private static Path configDir;

    private static String language = com.wynnchayuan.translate.Languages.DEFAULT;

    public static CaptureStore store() {
        return store;
    }

    public static Path configDir() {
        return configDir;
    }

    public static CollectorConfig config() {
        return config;
    }

    public static TranslationStore translations() {
        return translations;
    }

    @Override
    public void onInitializeClient() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        configDir = dir;
        ConfigFolder.tidy(dir);
        com.wynnchayuan.translate.TranslationCache.modVersion = version();
        store = new CaptureStore(dir.resolve("captured.json"));
        config = new CollectorConfig(dir.resolve("config.json"));
        NetworkGate.use(config);
        com.wynnchayuan.capture.CardDump.init(dir.resolve(
                com.wynnchayuan.capture.CardDump.FILE));
        com.wynnchayuan.render.DialogueTint.init(dir.resolve(
                com.wynnchayuan.render.DialogueTint.FILE), language);
        com.wynnchayuan.render.ThirdPartySections.load(dir);
        com.wynnchayuan.translate.TranslationUpdate.registerCommand();
        com.wynnchayuan.render.ThirdPartyLiterals.load(dir);
        if (config.debugDumps()) {
            Path debug = ConfigFolder.debugDir(dir);
            com.wynnchayuan.render.TooltipDebug.init(debug.resolve("tooltip-debug.json"));
            com.wynnchayuan.render.Boxes.init(debug.resolve("overlay-debug.txt"));
            com.wynnchayuan.translate.LineDebug.init(debug.resolve("line-debug.txt"));
            com.wynnchayuan.translate.NameplateProbe.init(debug);
            com.wynnchayuan.translate.LayoutDebug.init(debug.resolve("layout-debug.txt"));
            com.wynnchayuan.translate.FlowedDebug.init(debug);
            com.wynnchayuan.capture.DialogueProbe.init(debug);
            com.wynnchayuan.render.DialogueStartLog.init(debug);
            com.wynnchayuan.render.FpsLog.init(debug);
        }
        com.wynnchayuan.translate.ErrorDebug.into(dir);
        Releases.init();

        language = com.wynnchayuan.translate.Languages.pick(
                config.language(), gameLanguage());
        com.wynnchayuan.translate.Languages.migrateFlat(dir, language);
        Path trDir = com.wynnchayuan.translate.Languages.dir(dir, language);
        com.wynnchayuan.translate.TranslationCache.syncsFromGitHub =
                config.source() == CollectorConfig.Source.GITHUB;
        com.wynnchayuan.translate.TranslationCache.prepare(trDir, language);
        String underneath = fallbackLanguage();
        if (underneath != null) {
            com.wynnchayuan.translate.TranslationCache.prepare(
                    com.wynnchayuan.translate.Languages.dir(dir, underneath), underneath);
        }
        translations = new TranslationStore();
        translations.setNameMode(config.itemNames());
        translations.setPageNameMode(config.pageNames());
        translations.setAbilityNamesWithOriginal(config.abilityNamesOriginal());
        store.knowsTranslations(WynnChaYuan::alreadyTranslated);
        store.knowsSources(WynnChaYuan::alreadyCollected);
        store.knowsWhere(com.wynnchayuan.capture.Whereabouts::now);
        store.knowsLonger(t -> translations.hasLonger(t));
        com.wynnchayuan.capture.OwnOutputs.buildAsync();
        loadLayers();

        // CLIENT_STARTED 之前，跟隨遊戲語言的玩家語言還沒定

        // Wynntils event bus 在 WynntilsMod.init() 才建立
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerWithWynntils());
        ClientLifecycleEvents.CLIENT_STARTED.register(WynnChaYuan::releaseOldDefaultKeys);

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            settleAutoLanguage();
            startNetworked();
        });

        try {
            registerKeyBind();
        } catch (Throwable t) {
            System.out.println("[" + MOD_NAME + "] 按鍵註冊失敗，其餘功能照常："
                    + t);
        }
        try {
            registerCommand();
        } catch (Throwable t) {
            System.out.println("[" + MOD_NAME + "] 指令註冊失敗，其餘功能照常：" + t);
        }
        try {
            net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register(
                    (player, world, hand, entity, hit) -> {
                        if (world.isClientSide()) {
                            com.wynnchayuan.capture.ConversationPartner.clicked(entity);
                        }
                        return net.minecraft.world.InteractionResult.PASS;
                    });
            net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register(
                    (player, world, hand, hit) -> {
                        if (world.isClientSide()) {
                            com.wynnchayuan.capture.ConversationPartner.blockClicked();
                        }
                        return net.minecraft.world.InteractionResult.PASS;
                    });
        } catch (Throwable t) {
            System.out.println("[" + MOD_NAME + "] 點擊監聽註冊失敗，說話者只看名牌：" + t);
        }

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            ScreenEvents.afterRender(screen).register(
                    (s, graphics, mx, my, delta) ->
                            RenderListener.renderAfterScreen(graphics, mx, my));
            // KeyMapping.consumeClick() 只在沒有畫面時有事件
        });
        HudElementRegistry.addLast(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "dialogue"),
                (graphics, tickCounter) -> RenderListener.renderHud(graphics));

        flusher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, MOD_ID + "-flush");
            t.setDaemon(true);
            return t;
        });
        flusher.scheduleWithFixedDelay(WynnChaYuan::tick, 1, 1, TimeUnit.SECONDS);
        flusher.scheduleWithFixedDelay(WynnChaYuan::flushQuietly, 30, 30, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            flushQuietly();
            System.out.println("[WynnChaYuan] 已收集 " + store.size() + " 條字串");
            SelfUpdate.applyPending();
        }, MOD_ID + "-shutdown"));

        System.out.println("[WynnChaYuan] 就緒，輸出於 " + dir.resolve("captured.json"));
        System.out.println("[WynnChaYuan] 譯文 " + translations.size() + " 條（"
                + translations.loadedFiles() + " 個檔案），/wcy 開啟設定");
        System.out.println("[WynnChaYuan] 地名清單 "
                + com.wynnchayuan.capture.PlaceNames.size() + " 筆（不翻譯，原樣保留）");
    }

    public static void reloadTranslations() {
        loadLayers();
    }

    private static boolean alreadyCollected(String template) {
        if (alreadyTranslated(template)) {
            return true;
        }
        java.util.Set<String> keys = referenceKeys;
        return keys != null && template != null && keys.contains(template.strip());
    }

    private static boolean alreadyTranslated(String template) {
        return translations.hasTranslation(template);
    }

    private static volatile java.util.Set<String> referenceKeys;

    private static void loadReferenceKeys(java.util.List<Path> layers) {
        String reference = com.wynnchayuan.translate.Languages.DEFAULT;
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, reference);
        if (layers.contains(dir)) {
            referenceKeys = translations.sourceKeys();
            System.out.println("[WynnChaYuan] 收集的判準用畫面上這幾層的 "
                    + referenceKeys.size() + " 條原文");
            return;
        }
        try {
            com.wynnchayuan.translate.TranslationStore probe =
                    new com.wynnchayuan.translate.TranslationStore();
            probe.loadAll(dir);
            referenceKeys = probe.sourceKeys();
            System.out.println("[WynnChaYuan] 收集的判準用 " + reference
                    + " 的 " + referenceKeys.size() + " 條原文");
        } catch (Exception e) {
            referenceKeys = null;
        }
    }

    private static void loadLayers() {
        translations.setNameMode(config.itemNames());
        translations.setPageNameMode(config.pageNames());
        translations.setAbilityNamesWithOriginal(config.abilityNamesOriginal());
        if (!config.translationsEnabled()) {
            translations.loadAll(java.util.List.of());
            com.wynnchayuan.capture.UnappliedAudit.clear();
            return;
        }
        java.util.List<Path> layers = new java.util.ArrayList<>();
        String under = fallbackLanguage();
        if (under != null) {
            Path fallback = com.wynnchayuan.translate.Languages.dir(configDir, under);
            StarterFiles.installIfEmpty(fallback, under);
            layers.add(fallback);
        }
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, language);
        StarterFiles.installIfEmpty(dir, language);
        layers.add(dir);
        com.wynnchayuan.translate.TranslationCache.loadRepairing(translations, layers);
        loadReferenceKeys(layers);
        com.wynnchayuan.capture.UnappliedAudit.clear();
    }

    private static int fetchCurrentLanguages() {
        com.wynnchayuan.translate.FetchedLanguages.mark(language);
        int changed = RemoteSync.fetchInto(
                com.wynnchayuan.translate.Languages.dir(configDir, language), language);
        String under = fallbackLanguage();
        if (under != null) {
            com.wynnchayuan.translate.FetchedLanguages.mark(under);
            changed += RemoteSync.fetchInto(
                    com.wynnchayuan.translate.Languages.dir(configDir, under), under);
        }
        if (RemoteSync.fetchCredits(configDir)) {
            com.wynnchayuan.client.Credits.reload();
        }
        return changed;
    }

    private static void refreshOncePerSession(String lang) {
        if (config.source() != CollectorConfig.Source.GITHUB
                || !config.autoUpdateTranslations()
                || !com.wynnchayuan.translate.FetchedLanguages.firstTime(lang)) {
            return;
        }
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, lang);
        Thread worker = new Thread(() -> {
            int changed = RemoteSync.fetchInto(dir, lang);
            System.out.println("[" + MOD_NAME + "] 切到 " + lang + "：" + RemoteSync.lastResult());
            if (changed > 0) {
                reloadOnMainThread();
            }
        }, MOD_ID + "-switch-sync");
        worker.setDaemon(true);
        worker.start();
    }

    private static void reloadOnMainThread() {
        try {
            net.minecraft.client.Minecraft.getInstance().execute(WynnChaYuan::loadLayers);
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] 同步完排不進主執行緒，下次啟動才會套用：" + t);
        }
    }

    private static void flushQuietly() {
        try {
            store.flush();
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] captured.json 寫入失敗: " + t);
        }
        try {
            com.wynnchayuan.capture.CardDump.flush();
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] "
                    + com.wynnchayuan.capture.CardDump.FILE + " 寫入失敗: " + t);
        }
        try {
            com.wynnchayuan.render.DialogueTint.flush();
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] "
                    + com.wynnchayuan.render.DialogueTint.FILE + " 寫入失敗: " + t);
        }
    }

    public static String fallbackLanguage() {
        return com.wynnchayuan.translate.Languages.underlay(config.fallbackLanguage(), language);
    }

    public static void switchFallback(String lang,
                                      java.util.function.Consumer<String> done) {
        switchFallback(lang, done, null);
    }

    public static void switchFallback(String lang,
                                      java.util.function.Consumer<String> done,
                                      RemoteSync.Progress progress) {
        config.setFallbackLanguage(lang);
        String wanted = fallbackLanguage();
        if (wanted != null) {
            com.wynnchayuan.translate.TranslationCache.prepare(
                    com.wynnchayuan.translate.Languages.dir(configDir, wanted), wanted);
        }
        loadLayers();
        String under = fallbackLanguage();
        if (under == null || config.source() != CollectorConfig.Source.GITHUB) {
            done.accept(com.wynnchayuan.client.T.s("data.fallback.done",
                    translations.size()));
            return;
        }
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, under);
        com.wynnchayuan.translate.FetchedLanguages.mark(under);
        Thread worker = new Thread(() -> {
            RemoteSync.fetchInto(dir, under, progress);
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                loadLayers();
                done.accept(com.wynnchayuan.client.T.s("data.fallback.done",
                        translations.size()));
            });
        }, MOD_ID + "-fallback");
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
        config.setLanguage(lang);
        language = com.wynnchayuan.translate.Languages.pick(
                config.language(), gameLanguage());
        com.wynnchayuan.render.DialogueTint.init(configDir.resolve(
                com.wynnchayuan.render.DialogueTint.FILE), language);
        Path chosen = com.wynnchayuan.translate.Languages.dir(configDir, language);
        com.wynnchayuan.translate.TranslationCache.prepare(chosen, language);
        loadLayers();
        refreshOncePerSession(language);
        done.accept(com.wynnchayuan.client.T.s("data.language.done",
                com.wynnchayuan.translate.Languages.nativeName(language),
                translations.size()));
    }


    public static void resyncTranslations(java.util.function.Consumer<String> done) {
        Thread worker = new Thread(() -> {
            String remote = RemoteSync.remoteVersion();
            fetchCurrentLanguages();
            String result = RemoteSync.lastResult();
            com.wynnchayuan.translate.TranslationUpdate.done(remote);
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                reloadTranslations();
                done.accept(com.wynnchayuan.client.T.s("status.sync_total", result, translations.size()));
            });
        }, MOD_ID + "-resync");
        worker.setDaemon(true);
        worker.start();
    }

    public static void recheckTranslationVersion(Runnable done) {
        Thread worker = new Thread(() -> {
            String remote = RemoteSync.remoteVersion();
            com.wynnchayuan.translate.TranslationUpdate.checked(remote);
            if (remote != null && !remote.equals(config.syncedTranslations())) {
                com.wynnchayuan.translate.TranslationUpdate.found(remote);
            }
            net.minecraft.client.Minecraft.getInstance().execute(done);
        }, MOD_ID + "-version");
        worker.setDaemon(true);
        worker.start();
    }

    public static String autoLanguage() {
        return com.wynnchayuan.translate.Languages.pick("", gameLanguage());
    }

    private static void startBackgroundSync() {
        if (config.source() != CollectorConfig.Source.GITHUB) {
            return;
        }
        Thread sync = new Thread(() -> {
            String remote = RemoteSync.remoteVersion();
            com.wynnchayuan.translate.TranslationUpdate.checked(remote);
            boolean first = config.syncedTranslations().isBlank();
            if (!first && !config.autoUpdateTranslations()) {
                if (remote != null && !remote.equals(config.syncedTranslations())) {
                    com.wynnchayuan.translate.TranslationUpdate.found(remote);
                }
                return;
            }
            int changed = fetchCurrentLanguages();
            com.wynnchayuan.translate.TranslationUpdate.done(remote);
            if (changed > 0) {
                reloadOnMainThread();
            }
            System.out.println("[WynnChaYuan] " + RemoteSync.lastResult());
        }, MOD_ID + "-sync");
        sync.setDaemon(true);
        sync.start();
    }

    public static void startNetworked() {
        NetworkGate.start(config, new NetworkGate.Services() {
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
        config.setConsent(agreed ? CollectorConfig.Consent.AGREED : CollectorConfig.Consent.DECLINED);
        if (agreed) {
            startNetworked();
        } else {
            Presence.forget();
        }
    }

    public static void setDataOptOut(boolean on,
                                     java.util.function.Consumer<Presence.DeleteResult> done) {
        config.setDataOptOut(on);
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
            if (result == Presence.DeleteResult.DONE && config.dataOptOut()) {
                config.dataDeleted();
            }
            System.out.println("[" + MOD_NAME + "] data deletion: " + result);
            if (done != null) {
                net.minecraft.client.Minecraft.getInstance().execute(() -> done.accept(result));
            }
        });
    }

    private static void settleAutoLanguage() {
        if (!config.language().isBlank()) {
            return;
        }
        String game = gameLanguage();
        if (!com.wynnchayuan.translate.Languages.known(game)) {
            return;
        }
        String want = com.wynnchayuan.translate.Languages.pick("", game);
        if (want.equals(language)) {
            return;
        }
        language = want;
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, language);
        com.wynnchayuan.translate.TranslationCache.prepare(dir, language);
        String underneath = fallbackLanguage();
        if (underneath != null) {
            com.wynnchayuan.translate.TranslationCache.prepare(
                    com.wynnchayuan.translate.Languages.dir(configDir, underneath),
                    underneath);
        }
        com.wynnchayuan.render.DialogueTint.init(
                configDir.resolve(com.wynnchayuan.render.DialogueTint.FILE), language);
        loadLayers();
        System.out.println("[" + MOD_NAME + "] 跟隨遊戲語言：改用 " + language
                + "（開機時問不到，先用了 " + com.wynnchayuan.translate.Languages.DEFAULT
                + "）");
    }

    private static String gameLanguage() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc == null || mc.getLanguageManager() == null
                    ? "" : mc.getLanguageManager().getSelected();
        } catch (Throwable t) {
            return "";
        }
    }

    public static String language() {
        return language;
    }

    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static void registerKeyBind() {
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.openSettings",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));
        screenshotKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.screenshot",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));
        copyChatKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.copyChat",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));
        com.wynnchayuan.render.PanelShot.bind(screenshotKey);
        com.wynnchayuan.render.PanelShot.listen();

        if (com.wynnchayuan.translate.WynnventoryBridge.installed()) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                CollectorConfig c = config();
                TranslationStore store = translations();
                if (c == null || store == null) {
                    return;
                }
                com.wynnchayuan.translate.WynnventoryBridge.tick(c.wynnventory(),
                        c.shiftPeekNames() && com.wynnchayuan.listener.RenderListener.shiftHeld(),
                        store.generation());
            });
        }

        ClientTickEvents.END_CLIENT_TICK.register(com.wynnchayuan.render.FpsLog::tick);

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> com.wynnchayuan.client.FirstRunScreen.clientTick());
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> com.wynnchayuan.client.NoticeScreen.clientTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettingsKey.consumeClick()) {
                client.setScreen(new SettingsScreen());
            }
            if (tellKeysReleased && client.player != null) {
                tellKeysReleased = false;
                Component line = com.wynnchayuan.client.T.c("chat.keys.released")
                        .withStyle(net.minecraft.ChatFormatting.AQUA);
                com.wynnchayuan.capture.OwnOutputs.note(line);
                client.player.displayClientMessage(line, false);
            }
            if (openSettingsNextTick) {
                openSettingsNextTick = false;
                client.setScreen(new SettingsScreen());
            }
            while (copyChatKey.consumeClick()) {
                client.setScreen(new com.wynnchayuan.client.ChatCopyScreen());
            }
            while (screenshotKey.consumeClick()) {
                com.wynnchayuan.render.PanelShot.request();
            }
            com.wynnchayuan.render.PanelShot.tick();
            Releases.tellOnce(client);
            if (client.player != null && client.getCurrentServer() != null
                    && client.getCurrentServer().ip.toLowerCase(java.util.Locale.ROOT)
                            .contains("wynncraft")) {
                Presence.tick(true, config.ownCape(),
                        com.wynnchayuan.render.Badges.ownRainbow(config),
                        com.wynnchayuan.render.Badges.ownShown(config));
            }
            SelfUpdate.tellLeftover(client);
            SelfUpdate.announceProgress(client);
            com.wynnchayuan.translate.TranslationUpdate.tellOnce(client);
        });
    }

    private static volatile boolean tellKeysReleased = false;

    private static void releaseOldDefaultKeys(net.minecraft.client.Minecraft client) {
        try {
            if (config == null || config.oldKeysReleased() || openSettingsKey == null
                    || screenshotKey == null) {
                return;
            }
            boolean cleared = unbindIf(openSettingsKey, org.lwjgl.glfw.GLFW.GLFW_KEY_F6);
            cleared |= unbindIf(screenshotKey, org.lwjgl.glfw.GLFW.GLFW_KEY_F9);
            cleared |= unbindIf(screenshotKey, org.lwjgl.glfw.GLFW.GLFW_KEY_F8);
            if (cleared) {
                KeyMapping.resetMapping();
                client.options.save();
                tellKeysReleased = true;
            }
            config.markOldKeysReleased();
        } catch (Throwable t) {
            System.out.println("[" + MOD_NAME + "] 清舊的預設鍵時出錯，按鍵維持原樣：" + t);
        }
    }

    private static boolean unbindIf(KeyMapping mapping, int key) {
        InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(mapping);
        if (bound == null || bound.getType() != InputConstants.Type.KEYSYM
                || bound.getValue() != key) {
            return false;
        }
        mapping.setKey(InputConstants.UNKNOWN);
        return true;
    }

    private static volatile boolean openSettingsNextTick = false;

    private static void registerCommand() {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess) -> {
                    for (String name : new String[] {"wynnchayuan", "wcy"}) {
                        dispatcher.register(
                                net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                        .literal(name)
                                        .executes(context -> {
                                            openSettingsNextTick = true;
                                            return 1;
                                        })
                                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                .literal("update")
                                                .executes(context -> {
                                                    SelfUpdate.startFromChat(
                                                            net.minecraft.client.Minecraft.getInstance());
                                                    return 1;
                                                }))
                                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                .literal("link")
                                                .executes(context -> {
                                                    say("chat.link.usage");
                                                    return 1;
                                                })
                                                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                        .argument("code", com.mojang.brigadier.arguments
                                                                .StringArgumentType.word())
                                                        .executes(context -> {
                                                            linkAccount(com.mojang.brigadier.arguments
                                                                    .StringArgumentType.getString(context, "code"));
                                                            return 1;
                                                        }))));
                    }
                });
    }

    private static void linkAccount(String code) {
        say("chat.link.working");
        Presence.link(code, result -> net.minecraft.client.Minecraft.getInstance().execute(() -> say(switch (result) {
            case DONE -> "chat.link.done";
            case BAD_CODE -> "chat.link.bad";
            case UNKNOWN_CODE -> "chat.link.unknown";
            case LOGIN_FAILED -> "chat.link.login";
            case FAILED -> "chat.link.failed";
            case OFF -> "chat.link.off";
        })));
    }

    private static void say(String key) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        net.minecraft.network.chat.Component line = com.wynnchayuan.client.T.c(key);
        com.wynnchayuan.capture.OwnOutputs.note(line);
        client.player.displayClientMessage(line, false);
    }

    private static void tick() {
        CaptureListener l = listener;
        if (l != null) {
            try {
                l.flushSettled();
            } catch (Throwable t) {
                System.err.println("[WynnChaYuan] flushSettled 失敗: " + t);
            }
        }
    }

    private static void registerWithWynntils() {
        if (!FabricLoader.getInstance().isModLoaded("wynntils")) {
            System.out.println("[WynnChaYuan] 找不到 Wynntils，停用收集功能");
            return;
        }
        try {
            listener = new CaptureListener();
            WynntilsMod.registerEventListener(listener);
            WynntilsMod.registerEventListener(new RenderListener());
            WynntilsMod.registerEventListener(new ActionBarListener());
            WynntilsMod.registerEventListener(new TrackerListener());
            WynntilsMod.registerEventListener(new ScoreboardListener());
            WynntilsMod.registerEventListener(new BadgeListener());
            WynntilsMod.registerEventListener(new ChatListener());
            WynntilsMod.registerEventListener(new TitleListener());
            WynntilsMod.registerEventListener(new MarketListener());
            net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
                    .MODIFY_CHAT.register(MarketListener::rewrite);
            com.wynnchayuan.client.MarketPicker.register();
            System.out.println("[WynnChaYuan] 已掛上 Wynntils 事件，開始收集");
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] 掛載失敗，停用收集功能: " + t);
        }
    }
}
