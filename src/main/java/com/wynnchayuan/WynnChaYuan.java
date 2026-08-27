package com.wynnchayuan;

import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.listener.ActionBarListener;
import com.wynnchayuan.listener.BadgeListener;
import com.wynnchayuan.listener.CaptureListener;
import com.wynnchayuan.listener.RenderListener;
import com.wynnchayuan.client.SettingsScreen;
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

/**
 * 收集 Wynncraft 遊戲內尚未翻譯的字串。
 *
 * <p>裝備與技能可以從官方 CDN 完整離線取得（見 corpus/ 的工具），
 * 這個 mod 只補齊靜態資料涵蓋不到的部分：任務對話、NPC 名牌、介面字串。
 */
public final class WynnChaYuan implements ClientModInitializer {

    public static final String MOD_ID = "wynnchayuan";
    public static final String MOD_NAME = "WynnChaYuan";

    /** 版本號取自 fabric.mod.json，不必兩個地方各寫一次。 */
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

    /** 把目前的翻譯面板拍成一張圖，給校稿用。 */
    private static KeyMapping screenshotKey;
    private static Path configDir;

    /** 目前使用的譯文語言。見 {@code Languages}。 */
    private static String language = com.wynnchayuan.translate.Languages.DEFAULT;

    /** 收集結果的存放位置。 */
    public static CaptureStore store() {
        return store;
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
        store = new CaptureStore(dir.resolve("captured.json"));
        config = new CollectorConfig(dir.resolve("config.json"));
        com.wynnchayuan.render.TooltipDebug.init(dir.resolve("tooltip-debug.json"));
        com.wynnchayuan.render.Boxes.init(dir.resolve("overlay-debug.txt"));
        com.wynnchayuan.render.ThirdPartySections.load(dir);
        com.wynnchayuan.translate.LineDebug.init(dir.resolve("line-debug.txt"));
        com.wynnchayuan.translate.LayoutDebug.init(dir.resolve("layout-debug.txt"));
        com.wynnchayuan.translate.FlowedDebug.init(dir);
        com.wynnchayuan.capture.DialogueProbe.init(dir);
        com.wynnchayuan.translate.ErrorDebug.into(dir);

        // 譯文放在 config/wynnchayuan/translations/ 下，格式與 corpus/workspace 相同，
        // 所以離線語料與遊戲內收集的內容可以直接混放。
        // 第一次啟動時把內建的工作檔倒出來，玩家才有東西可以翻
        // 譯文按語言分層（translations/<lang>/），見 Languages。
        // 舊版是平的，第一次跑到新版時搬進 zh_tw/——不搬的話使用者自己翻的
        // 東西會突然全部失效，而且看不出原因。
        language = com.wynnchayuan.translate.Languages.pick(
                config.language(), gameLanguage());
        com.wynnchayuan.translate.Languages.migrateFlat(dir, language);
        Path trDir = com.wynnchayuan.translate.Languages.dir(dir, language);
        StarterFiles.installIfEmpty(trDir, language);
        translations = new TranslationStore();
        translations.setTranslateNames(config.translateItemNames());
        // 同語族的語言先鋪一層當底，再把選定的那一種疊上去。
        //
        // 新語言是從 zh_tw 複製出來、dst 全部清空的骨架，剛開張時一條譯文
        // 都沒有。少了這一層，簡體中文的玩家會在 zh_cn/ 建好的那一刻，
        // 從「看得到繁體」變成「什麼都看不到」——多一種語言反而害了他。
        //
        // 只在同語族之內墊，見 Languages#fallbackFor：日文與俄文的玩家
        // 不該因為那邊還沒翻就突然看到滿畫面中文。
        //
        // 疊得起來是因為載入端本來就會跳過空的 dst（見 TranslationStore），
        // 而後載入的會蓋掉先載入的。於是每一條各自回退。
        String under = com.wynnchayuan.translate.Languages.fallbackFor(language);
        if (under != null) {
            Path fallback = com.wynnchayuan.translate.Languages.dir(dir, under);
            StarterFiles.installIfEmpty(fallback, under);
            translations.loadAll(fallback);
        }
        translations.loadAll(trDir);

        // 從 GitHub 同步最新譯文。放背景執行緒，不拖慢進遊戲；
        // 抓不到就沿用剛剛載入的本機版本。
        if (config.source() == CollectorConfig.Source.GITHUB) {
            Thread sync = new Thread(() -> {
                if (RemoteSync.fetchInto(trDir, language) > 0) {
                    translations.loadAll(trDir);
                }
                System.out.println("[WynnChaYuan] " + RemoteSync.lastResult());
            }, MOD_ID + "-sync");
            sync.setDaemon(true);
            sync.start();
        }

        // 註冊必須等到 CLIENT_STARTED，不能在這裡直接做。
        //
        // Wynntils 的 event bus 是在 WynntilsMod.init() 裡才建立的，而那發生在
        // client entrypoint 之後——在 onInitializeClient 呼叫 registerEventListener
        // 會直接吃到 NullPointerException 並讓遊戲開不起來。
        // WynnScribe 也是這樣處理的（見其 WynnscribeFabric）。
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerWithWynntils());

        // 按鍵綁定壞掉不該把整個遊戲擋在門外。1.99.2 就是在這裡丟了一個
        // NullPointerException，玩家連主畫面都進不去——而少的只是一個截圖鍵。
        try {
            registerKeyBind();
        } catch (Throwable t) {
            System.out.println("[" + MOD_NAME + "] 按鍵註冊失敗，其餘功能照常："
                    + t);
        }

        // tooltip 面板要在整個畫面畫完之後才畫，否則會被原始 tooltip 蓋掉。
        // Wynntils 的 ItemTooltipRenderEvent.Post 從來沒被發送過，用不了，
        // 所以改掛 Fabric 的螢幕事件。
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            ScreenEvents.afterRender(screen).register(
                    (s, graphics, mx, my, delta) ->
                            RenderListener.renderAfterScreen(graphics, mx, my));
            // 畫面開著時的按鍵要另外接。
            //
            // KeyMapping.consumeClick() 只有在<b>沒有畫面</b>時才會有事件——
            // Minecraft 的鍵位佇列是在 handleKeybinds() 裡餵的，而那只在
            // screen == null 時跑。所以「開著背包、滑鼠停在物品上按 F8」
            // 永遠不會被讀到，而那正好是最需要拍照的時刻。
            // 畫面開著時的 F8 由 PanelShot 直接讀鍵盤——見 pollWhileScreenOpen。
            // 先前試過 ScreenKeyboardEvents，實測沒有生效。
        });
        HudElementRegistry.addLast(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "dialogue"),
                (graphics, tickCounter) -> RenderListener.renderHud(graphics));

        // 收集發生在渲染路徑上，寫檔一律交給背景執行緒，避免卡住遊戲主迴圈
        flusher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, MOD_ID + "-flush");
            t.setDaemon(true);
            return t;
        });
        // 每秒檢查對話是否已經打完（打字停住夠久就送出），寫檔仍維持 30 秒一次
        flusher.scheduleWithFixedDelay(WynnChaYuan::tick, 1, 1, TimeUnit.SECONDS);
        flusher.scheduleWithFixedDelay(store::flush, 30, 30, TimeUnit.SECONDS);

        // 關遊戲時確保最後一批資料有落地
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            store.flush();
            System.out.println("[WynnChaYuan] 已收集 " + store.size() + " 條字串");
        }, MOD_ID + "-shutdown"));

        System.out.println("[WynnChaYuan] 就緒，輸出於 " + dir.resolve("captured.json"));
        System.out.println("[WynnChaYuan] 譯文 " + translations.size() + " 條（"
                + translations.loadedFiles() + " 個檔案），F6 開啟設定");
        System.out.println("[WynnChaYuan] 地名清單 "
                + com.wynnchayuan.capture.PlaceNames.size() + " 筆（不翻譯，原樣保留）");
    }

    /**
     * 重新載入譯文檔，不用重開遊戲。
     *
     * <p>只讀本機檔案。譯文來源設在 GitHub 時，這樣按下去只會把同一份舊快取
     * 再讀一次——所以另外有 {@link #resyncTranslations}。
     */
    public static void reloadTranslations() {
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, language);
        StarterFiles.installIfEmpty(dir, language);   // 被清空的話順手補回來
        translations.setTranslateNames(config.translateItemNames());
        translations.loadAll(dir);
    }

    /**
     * 重新從 GitHub 抓一次譯文，抓完再載入。
     *
     * <p>譯者在 GitHub 上改完之後，需要進遊戲對照才能確認翻得對不對。
     * 沒有這個按鈕就得重開遊戲，來回一趟的成本高到沒人會認真校對。
     *
     * <p>網路動作放在背景執行緒，按下去畫面不會卡住；抓完再回到主執行緒載入。
     *
     * @param done 完成後在主執行緒呼叫，參數是要顯示給使用者的結果
     */
    public static void resyncTranslations(java.util.function.Consumer<String> done) {
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, language);
        Thread worker = new Thread(() -> {
            RemoteSync.fetchInto(dir, language);
            String result = RemoteSync.lastResult();
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                reloadTranslations();
                done.accept(result + "，共 " + translations.size() + " 條");
            });
        }, MOD_ID + "-resync");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 遊戲本身的語系代碼（{@code zh_tw}、{@code ja_jp}…）。
     *
     * <p>取不到就回傳空字串，交給 {@code Languages#pick} 退回預設——
     * 這個方法在遊戲還沒初始化完時也可能被呼叫到。
     */
    private static String gameLanguage() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc == null || mc.getLanguageManager() == null
                    ? "" : mc.getLanguageManager().getSelected();
        } catch (Throwable t) {
            return "";
        }
    }

    /** 目前使用的譯文語言。 */
    public static String language() {
        return language;
    }

    /** F6 開啟設定面板。 */
    private static void registerKeyBind() {
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.openSettings",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_F6,
                KeyMapping.Category.MISC));
        screenshotKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.screenshot",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_F8,
                KeyMapping.Category.MISC));
        // 畫面開著時 KeyMapping 收不到事件，PanelShot 掛在畫面自己的鍵盤事件上——
        // 把 mapping 交給它，改綁才會跟著生效。
        //
        // 這兩行必須在 screenshotKey <b>指派之後</b>：1.99.2 誤植在上面那個
        // F6 綁定的後面，交出去的是還沒指派的 null，整個模組在啟動時就炸了。
        com.wynnchayuan.render.PanelShot.bind(screenshotKey);
        com.wynnchayuan.render.PanelShot.listen();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettingsKey.consumeClick()) {
                client.setScreen(new SettingsScreen());
            }
            // 截圖的按鍵在 tick 裡只記一個旗標，真正拍是在下一次繪製<b>之後</b>。
            // tick 的時候這一幀還沒畫完，當場拍會拍到上一幀，
            // 而上一幀常常正好是滑鼠剛移到物品上、面板還沒出現的那一幀。
            while (screenshotKey.consumeClick()) {
                com.wynnchayuan.render.PanelShot.request();
            }
            // 真正的拍照在這裡——tick 跑在兩幀之間，那時畫面裡才是
            // 上一幀完整合成後的結果。見 PanelShot#tick。
            com.wynnchayuan.render.PanelShot.tick();
        });
    }

    /** 把打完但還沒送出的對話收進 store。 */
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

    /**
     * 掛上 Wynntils 的事件匯流排。
     *
     * <p>整段包在 try/catch 裡是刻意的：這個 mod 只是輔助工具，
     * 就算因為 Wynntils 改版而註冊失敗，也絕對不該讓玩家的遊戲開不起來。
     * 失敗就安靜地不收集，其他功能照常。
     */
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
            WynntilsMod.registerEventListener(new BadgeListener());
            System.out.println("[WynnChaYuan] 已掛上 Wynntils 事件，開始收集");
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] 掛載失敗，停用收集功能: " + t);
        }
    }
}
