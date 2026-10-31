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
import com.wynnchayuan.translate.ErrorDebug;
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
    private static volatile CaptureListener listener;
    private static ScheduledExecutorService flusher;
    static CollectorConfig config;
    static TranslationStore translations;
    static KeyMapping openSettingsKey;

    static KeyMapping screenshotKey;

    static KeyMapping copyChatKey;
    static Path configDir;

    static volatile String language = com.wynnchayuan.translate.Languages.DEFAULT;

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
                config.language(), LanguageLayers.gameLanguage());
        com.wynnchayuan.translate.Languages.migrateFlat(dir, language);
        Path trDir = com.wynnchayuan.translate.Languages.dir(dir, language);
        com.wynnchayuan.translate.TranslationCache.syncsFromGitHub =
                config.source() == CollectorConfig.Source.GITHUB;
        com.wynnchayuan.translate.TranslationCache.prepare(trDir, language);
        String underneath = LanguageLayers.fallbackLanguage();
        if (underneath != null) {
            com.wynnchayuan.translate.TranslationCache.prepare(
                    com.wynnchayuan.translate.Languages.dir(dir, underneath), underneath);
        }
        translations = new TranslationStore();
        translations.setNameMode(config.itemNames());
        translations.setPageNameMode(config.pageNames());
        translations.setAbilityNamesWithOriginal(config.abilityNamesOriginal());
        store.knowsTranslations(LanguageLayers::alreadyTranslated);
        store.knowsSources(LanguageLayers::alreadyCollected);
        store.knowsWhere(com.wynnchayuan.capture.Whereabouts::now);
        store.knowsLonger(t -> translations.hasLonger(t));
        com.wynnchayuan.capture.OwnOutputs.buildAsync();
        LanguageLayers.loadLayers();

        // CLIENT_STARTED 之前，跟隨遊戲語言的玩家語言還沒定

        // Wynntils event bus 在 WynntilsMod.init() 才建立
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerWithWynntils());
        ClientLifecycleEvents.CLIENT_STARTED.register(ModKeys::releaseOldDefaultKeys);

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            LanguageLayers.settleAutoLanguage();
            ModNetwork.startNetworked();
        });

        try {
            ModKeys.registerKeyBind();
        } catch (Throwable t) {
            ModLog.info("按鍵註冊失敗，其餘功能照常："
                    + t);
        }
        try {
            ModCommands.registerCommand();
        } catch (Throwable t) {
            ModLog.info("指令註冊失敗，其餘功能照常：" + t);
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
            ModLog.info("點擊監聽註冊失敗，說話者只看名牌：" + t);
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
            ModLog.info("已收集 " + store.size() + " 條字串");
            SelfUpdate.applyPending();
        }, MOD_ID + "-shutdown"));

        ModLog.info("就緒，輸出於 " + dir.resolve("captured.json"));
        ModLog.info("譯文 " + translations.size() + " 條（"
                + translations.loadedFiles() + " 個檔案），/wcy 開啟設定");
        ModLog.info("地名清單 "
                + com.wynnchayuan.capture.PlaceNames.size() + " 筆（不翻譯，原樣保留）");
    }

    private static void flushQuietly() {
        try {
            store.flush();
        } catch (Throwable t) {
            ModLog.warn("captured.json 寫入失敗: " + t);
        }
        try {
            com.wynnchayuan.capture.CardDump.flush();
        } catch (Throwable t) {
            ModLog.warn(""
                    + com.wynnchayuan.capture.CardDump.FILE + " 寫入失敗: " + t);
        }
        try {
            com.wynnchayuan.render.DialogueTint.flush();
        } catch (Throwable t) {
            ModLog.warn(""
                    + com.wynnchayuan.render.DialogueTint.FILE + " 寫入失敗: " + t);
        }
    }

    public static String language() {
        return language;
    }

    private static void tick() {
        CaptureListener l = listener;
        if (l != null) {
            try {
                l.flushSettled();
            } catch (Throwable t) {
                ModLog.warn("flushSettled 失敗: " + t);
            }
        }
    }

    private static void registerWithWynntils() {
        if (!FabricLoader.getInstance().isModLoaded("wynntils")) {
            ModLog.info("找不到 Wynntils，停用收集功能");
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
            ModLog.info("已掛上 Wynntils 事件，開始收集");
        } catch (Throwable t) {
            ModLog.warn("掛載失敗，停用收集功能: " + t);
        }
    }
}
