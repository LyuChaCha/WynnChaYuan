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

    /**
     * 開啟「複製聊天」。
     *
     * <p>預設<b>不綁</b>：F6／F8 是我們自己挑的，再多搶一個鍵對誰都不好。
     * 想用的人到按鍵設定的 WynnChaYuan 那一區綁一個順手的。
     */
    private static KeyMapping copyChatKey;
    private static Path configDir;

    /** 目前使用的譯文語言。見 {@code Languages}。 */
    private static String language = com.wynnchayuan.translate.Languages.DEFAULT;

    /** 收集結果的存放位置。 */
    public static CaptureStore store() {
        return store;
    }

    /** 模組的設定資料夾（{@code config/wynnchayuan}）。F6 的匯出要知道檔案放在哪。 */
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
        // 升級時玩家不會刪這個資料夾，也不該需要刪。先清掉舊版留下的暫存檔與
        // 不再使用的記錄，再讓任何人讀它——見 ConfigFolder。後面每一個讀檔的地方
        // 讀不懂都會把檔案改名放旁邊、用預設值繼續（見 SafeFiles），不會擋住啟動。
        ConfigFolder.tidy(dir);
        com.wynnchayuan.translate.TranslationCache.modVersion = version();
        store = new CaptureStore(dir.resolve("captured.json"));
        config = new CollectorConfig(dir.resolve("config.json"));
        // 滑過卡片時，把模組<b>實際查表用的整段鍵</b>另外記一份。
        //
        // 不跟著 debugDumps 走：這不是診斷檔，是要交出去的語料稿。它自己看
        // 「收集介面文字」那個開關（跟 GuiTextCapture 同一個），所以這裡
        // 無條件備妥路徑就好，關著的時候整支是空轉。見 CardDump。
        com.wynnchayuan.capture.CardDump.init(dir.resolve(
                com.wynnchayuan.capture.CardDump.FILE));
        com.wynnchayuan.render.ThirdPartySections.load(dir);
        com.wynnchayuan.render.ThirdPartyLiterals.load(dir);
        // 診斷檔預設不寫。
        //
        // 每一支寫檔的程式都是「沒 init 過就什麼都不做」，所以只要在這裡
        // 不呼叫 init，整包診斷檔就一個都不會出現——不必在十幾個地方各加
        // 一個判斷，也不會漏掉哪一支。要回報問題的人在 F6 裡打開再重進遊戲。
        //
        // ErrorDebug 不在此列：它只在真的丟例外時寫，是當機紀錄不是洗版，
        // 而那正是最需要留下來的東西。
        if (config.debugDumps()) {
            com.wynnchayuan.render.TooltipDebug.init(dir.resolve("tooltip-debug.json"));
            com.wynnchayuan.render.Boxes.init(dir.resolve("overlay-debug.txt"));
            com.wynnchayuan.translate.LineDebug.init(dir.resolve("line-debug.txt"));
            com.wynnchayuan.translate.LayoutDebug.init(dir.resolve("layout-debug.txt"));
            com.wynnchayuan.translate.FlowedDebug.init(dir);
            com.wynnchayuan.capture.DialogueProbe.init(dir);
        }
        com.wynnchayuan.translate.ErrorDebug.into(dir);
        // 版本說明：先讀 jar 內建那份，背景再換成線上的。見 Releases。
        Releases.init();

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
        // 別的版本寫的快取整包移開、上次沒倒完的補齊。墊底那一層也一樣——見 TranslationCache。
        com.wynnchayuan.translate.TranslationCache.prepare(trDir, language);
        String underneath = fallbackLanguage();
        if (underneath != null) {
            com.wynnchayuan.translate.TranslationCache.prepare(
                    com.wynnchayuan.translate.Languages.dir(dir, underneath), underneath);
        }
        translations = new TranslationStore();
        translations.setNameMode(config.itemNames());
        // captured.json 只該列「還沒翻的」。接上這一條之前它是照單全收——
        // 實機那份 308 條裡有 249 條語料早就翻好了，真正的缺口全被淹掉。
        // 用述詞接而不是把 store 交過去，收集端就不必認識翻譯端。
        store.knowsTranslations(WynnChaYuan::alreadyTranslated);
        // 語料收過、只是還沒翻的不當缺口，但照樣記次數——見 CaptureStore#knowsSources。
        store.knowsSources(WynnChaYuan::alreadyCollected);
        // 打到一半的半句、或提示框只收到第一行——見 CaptureStore#knowsLonger。
        store.knowsLonger(t -> translations.hasLonger(t));
        // 切換語言之後，先前畫出去的譯文會被當成原文收進來——見 OwnOutputs。
        com.wynnchayuan.capture.OwnOutputs.buildAsync();
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
        loadLayers();

        // 先問一句「有沒有新翻譯」，要不要抓由玩家決定。
        //
        // 以前這裡無條件把整個語言的三十幾個檔重抓一遍。翻譯一個月可能只動幾條，
        // 玩家卻每天都在付那個流量與那幾十秒。改成一次請求問 commit
        // （見 RemoteSync#remoteVersion），有新的才在聊天室說一聲。
        //
        // 兩個例外照抓不誤：F6 打開了自動更新，以及<b>從來沒抓過</b>——
        // 新玩家不該先被問一次才有翻譯。
        //
        // 放背景執行緒，不拖慢進遊戲；問不到或抓不到都只是沿用剛剛載入的本機版本。
        if (config.source() == CollectorConfig.Source.GITHUB) {
            Thread sync = new Thread(() -> {
                String remote = RemoteSync.remoteVersion();
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
        flusher.scheduleWithFixedDelay(WynnChaYuan::flushQuietly, 30, 30, TimeUnit.SECONDS);
        // 收集到的東西只留在這一台。要交給翻譯團隊，由玩家在 F6 按「匯出」、
        // 自己看過再附到 Issue——模組本身不把任何字串送出去（見 CorpusExport）。

        // 關遊戲時確保最後一批資料有落地
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            flushQuietly();
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
        loadLayers();
    }

    /**
     * 把目前語言的譯文載入，底下鋪同語族的那一層。
     *
     * <h2>為什麼抽出來</h2>
     * 「要鋪哪幾層」先前寫在啟動流程裡，而 {@link #reloadTranslations} 只讀
     * 自己那一層——按一次「重新載入譯文檔」，墊底的那一層就沒了。
     * 一份邏輯兩個地方寫，遲早會分岔，這次就是。
     *
     * <p>疊的動作交給 {@code TranslationStore#loadAll(List)}：清空只做一次，
     * 然後照順序讀，後面的蓋掉前面的。
     */
    /**
     * 這一句<b>收過了嗎</b>。
     *
     * <h2>收集的缺口與畫面的缺口是兩件事</h2>
     * 簡體玩家看到英文，多半只代表<b>簡體還沒翻</b>——那一句繁體早就收過、
     * 也早就翻好了。把它記成缺口，{@code captured.json} 就會被別人早就收過的
     * 東西塞滿，而真正沒人遇過的句子淹在裡面。
     *
     * <p>所以判準是「{@link Languages#DEFAULT} 有沒有這一條」，不是
     * 「我這一種語言有沒有」。
     *
     * <p>多數情況下畫面那一份就夠用：簡體底下墊著繁體，查得到就不會記。
     * 但<b>輔助語言設成「顯示原文」</b>時那一層不在，整份繁體語料就全變成
     * 「缺口」了——{@link #referenceKeys} 補的正是這個洞。
     *
     * <p>收過但畫面上沒有譯文的，收集端不當缺口，只記次數
     * （見 {@code CaptureStore#knowsSources}）。
     */
    private static boolean alreadyCollected(String template) {
        if (alreadyTranslated(template)) {
            return true;
        }
        java.util.Set<String> keys = referenceKeys;
        return keys != null && template != null && keys.contains(template.strip());
    }

    /** 畫面上這幾層查得到譯文嗎。查得到的連次數都不必記。 */
    private static boolean alreadyTranslated(String template) {
        return translations.hasTranslation(template);
    }

    /**
     * 繁體收過哪些原文。
     *
     * <p>只在繁體<b>沒有</b>鋪在畫面那幾層裡時才建——鋪著的話
     * {@code translations.hasTranslation} 本來就查得到，再存一份是白花記憶體。
     */
    private static volatile java.util.Set<String> referenceKeys;

    private static void loadReferenceKeys(java.util.List<Path> layers) {
        String reference = com.wynnchayuan.translate.Languages.DEFAULT;
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, reference);
        if (layers.contains(dir)) {
            // 這一層已經載進畫面用的 store 了，直接跟它要就好。
            //
            // 這裡原本寫 null，理由是「鋪著的話 hasTranslation 本來就查得到」。
            // 那是錯的：hasTranslation 只認<b>有譯文</b>的，而這份判準要的是
            // 「語料裡有沒有這一條」。還沒翻的那幾千條每次都會被判成「沒有」，
            // 於是繁體玩家每進一次遊戲就把它們重收一遍、重傳一遍——收集站
            // 每天撈回來的幾乎都是早就在倉庫裡的東西。
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
            referenceKeys = null;            // 讀不到就退回舊行為，不要壞掉
        }
    }

    private static void loadLayers() {
        translations.setNameMode(config.itemNames());
        java.util.List<Path> layers = new java.util.ArrayList<>();
        String under = fallbackLanguage();
        if (under != null) {
            Path fallback = com.wynnchayuan.translate.Languages.dir(configDir, under);
            StarterFiles.installIfEmpty(fallback, under);
            layers.add(fallback);
        }
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, language);
        StarterFiles.installIfEmpty(dir, language);   // 被清空的話順手補回來
        layers.add(dir);
        // 讀不懂的檔（上次被關掉時寫到一半）改名放旁邊、從 jar 補回來再載一次
        com.wynnchayuan.translate.TranslationCache.loadRepairing(translations, layers);
        loadReferenceKeys(layers);
    }

    /**
     * 同步完在主執行緒重載。
     *
     * <h2>為什麼不在同步的執行緒上直接載</h2>
     * {@code loadAll} 是先清空再一條一條填回去，而且裡面有幾張表不是執行緒安全的
     * （前綴比對的 TreeMap、介面標籤的 HashSet）。先前是在同步執行緒上直接載，
     * 同一時間主執行緒正在畫 tooltip、對話框、查這幾張表——輕則那一幀查到半空的表，
     * 重則 TreeMap 在走訪途中被改掉而丟例外、甚至卡在迴圈裡。切語言、重新同步那幾條路
     * 早就是「背景抓、主執行緒載」，這裡跟它們一致。
     *
     * <p>只有真的抓到新內容才會走到這裡（見 {@link RemoteSync#fetchInto}），
     * 所以大多數啟動根本不會重載。
     */
    /**
     * 把<b>目前這一疊</b>語言的譯文都抓下來：選定的那一種，加上墊底的那一種。
     *
     * <p>墊底那一種也要跟著更新，而且重載要整疊重載。先前有一版只重載了選定的
     * 那一種：同步一完成，墊底那層就從記憶體裡消失，而它的快取也從來沒有在
     * 啟動時更新過。
     *
     * @return 內容真的有變的檔案數；0 表示不必重新載入
     */
    private static int fetchCurrentLanguages() {
        int changed = RemoteSync.fetchInto(
                com.wynnchayuan.translate.Languages.dir(configDir, language), language);
        String under = fallbackLanguage();
        if (under != null) {
            changed += RemoteSync.fetchInto(
                    com.wynnchayuan.translate.Languages.dir(configDir, under), under);
        }
        return changed;
    }

    private static void reloadOnMainThread() {
        try {
            net.minecraft.client.Minecraft.getInstance().execute(WynnChaYuan::loadLayers);
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] 同步完排不進主執行緒，下次啟動才會套用：" + t);
        }
    }

    /**
     * 寫 captured.json，出錯只記一筆。
     *
     * <p>排程的工作只要丟出一次例外，之後每一次都會被<b>安靜地取消</b>——
     * captured.json 從此不再更新，而且沒有任何訊息。關遊戲時的那一次也一樣：
     * 讓例外從關閉掛鉤裡飛出去沒有任何好處。
     */
    private static void flushQuietly() {
        try {
            store.flush();
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] captured.json 寫入失敗: " + t);
        }
        // 各自一個 try：cards.json 寫不出來不該連帶讓 captured.json 也停掉，
        // 反過來也一樣。見上面「排程丟一次例外就再也不跑」。
        try {
            com.wynnchayuan.capture.CardDump.flush();
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] "
                    + com.wynnchayuan.capture.CardDump.FILE + " 寫入失敗: " + t);
        }
    }

    /**
     * 沒翻到的地方要拿哪一種語言墊底。
     *
     * <p>設定沒指定時照 {@code Languages#fallbackFor} 的自動規則（同語族才墊）；
     * 指名了就照指名的；{@code "off"} 就不墊，沒翻到的地方顯示英文原文。
     *
     * @return 要墊的語言；不墊時回傳 {@code null}
     */
    public static String fallbackLanguage() {
        String chosen = config.fallbackLanguage();
        if (OFF.equals(chosen)) {
            return null;
        }
        if (!chosen.isEmpty()) {
            // 指名自己沒有意義，那會讀同一個資料夾兩次
            return chosen.equals(language) ? null : chosen;
        }
        return com.wynnchayuan.translate.Languages.fallbackFor(language);
    }

    /**
     * 換輔助語言，不必重開遊戲。
     *
     * <p>跟 {@link #switchLanguage} 的差別是<b>主語言沒有變</b>，所以只要
     * 重新鋪層；但墊底那一種可能還沒抓下來過，所以照樣去同步一次。
     */
    public static void switchFallback(String lang,
                                      java.util.function.Consumer<String> done) {
        switchFallback(lang, done, null);
    }

    /**
     * @param progress 下載進度，在背景執行緒上叫；不需要就傳 {@code null}
     */
    public static void switchFallback(String lang,
                                      java.util.function.Consumer<String> done,
                                      RemoteSync.Progress progress) {
        config.setFallbackLanguage(lang);
        loadLayers();
        String under = fallbackLanguage();
        if (under == null || config.source() != CollectorConfig.Source.GITHUB) {
            done.accept(com.wynnchayuan.client.T.s("data.fallback.done",
                    translations.size()));
            return;
        }
        Path dir = com.wynnchayuan.translate.Languages.dir(configDir, under);
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

    /** {@code fallbackLanguage} 設成這個就是「不墊，顯示原文」。 */
    public static final String OFF = "off";

    /**
     * 換一種語言的譯文，不必重開遊戲。
     *
     * <h2>為什麼要能在遊戲裡換</h2>
     * 校稿的人要看的是「這一句在畫面上長什麼樣」。改設定檔再重開遊戲，
     * 每看一句就是一次重開——實際上沒有人會這樣校稿。
     *
     * <p>換完要做三件事：記住選擇、重新鋪層、把新語言的譯文抓下來。
     * 抓取放背景執行緒，抓完再載入一次。
     *
     * @param lang 語言代碼；空字串表示「跟著遊戲語言走」
     */
    public static void switchLanguage(String lang,
                                      java.util.function.Consumer<String> done) {
        switchLanguage(lang, done, null);
    }

    /**
     * @param progress 下載進度，在背景執行緒上叫；不需要就傳 {@code null}
     */
    public static void switchLanguage(String lang,
                                      java.util.function.Consumer<String> done,
                                      RemoteSync.Progress progress) {
        config.setLanguage(lang);
        language = com.wynnchayuan.translate.Languages.pick(
                config.language(), gameLanguage());
        // 切語言<b>不再下載</b>：載入手上已經有的那一份就好。
        //
        // 以前每切一次就把那個語言的三十幾個檔重抓一遍。在遊戲裡對照兩種語言
        // 的人來回切一次就是兩趟完整下載，測試時尤其惱人——而絕大多數時候
        // 那些檔跟上一次抓的一模一樣。
        //
        // 要最新的就按資料頁的「更新翻譯」，或在 F6 打開自動更新。
        // progress 留著不用：呼叫端的簽章不動，將來要接回進度條也還在。
        loadLayers();
        done.accept(com.wynnchayuan.client.T.s("data.language.done",
                com.wynnchayuan.translate.Languages.nativeName(language),
                translations.size()));
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
        Thread worker = new Thread(() -> {
            // 問版本要在抓之前：抓完才問的話，中間剛好有人合併進去，
            // 記下的就是那一個新的，而手上其實是舊的——之後再也不會提示。
            String remote = RemoteSync.remoteVersion();
            fetchCurrentLanguages();
            String result = RemoteSync.lastResult();
            com.wynnchayuan.translate.TranslationUpdate.done(remote);
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
    /** 「跟著遊戲」會挑到哪一種。設定畫面要先算得出來才能顯示。 */
    public static String autoLanguage() {
        return com.wynnchayuan.translate.Languages.pick("", gameLanguage());
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

    /** 目前使用的譯文語言。 */
    public static String language() {
        return language;
    }

    /** F6 開啟設定面板。 */
    /**
     * 按鍵設定裡的分類。
     *
     * <p>原本掛在原版的 {@link KeyMapping.Category#MISC}下。這兩個鍵
     * 確實有註冊成功（shot-debug 記得到 key.keyboard.f8），但「雜項」
     * 裡面已經堆了一堆東西，玩家捲到底也找不到。
     *
     * <p>自己開一個分類，列表裡就會出現一塊寫著
     * 「WynnChaYuan」的標題。名稱走 {@code key.category.wynnchayuan.main}——
     * 這是 {@code Category.label()} 拿 {@code Identifier.toLanguageKey("key.category")}
     * 拼出來的，兩個語言檔都要有這把鑰匙。
     */
    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static void registerKeyBind() {
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.openSettings",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_F6,
                KEY_CATEGORY));
        screenshotKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.screenshot",
                InputConstants.Type.KEYSYM,
                // 預設從 F8 換成 F9：實機回報 F8 按下去沒反應（有東西也綁在
                // 那個鍵上，見 PanelShot#conflict），改綁 F9 才會動。
                org.lwjgl.glfw.GLFW.GLFW_KEY_F9,
                KEY_CATEGORY));
        copyChatKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynnchayuan.copyChat",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,   // 預設不綁，見欄位說明
                KEY_CATEGORY));
        // 畫面開著時 KeyMapping 收不到事件，PanelShot 掛在畫面自己的鍵盤事件上——
        // 把 mapping 交給它，改綁才會跟著生效。
        //
        // 這兩行必須在 screenshotKey <b>指派之後</b>：1.99.2 誤植在上面那個
        // F6 綁定的後面，交出去的是還沒指派的 null，整個模組在啟動時就炸了。
        com.wynnchayuan.render.PanelShot.bind(screenshotKey);
        com.wynnchayuan.render.PanelShot.listen();

        // 警語等載入畫面收掉才跳，見 NoticeScreen#clientTick
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> com.wynnchayuan.client.NoticeScreen.clientTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettingsKey.consumeClick()) {
                client.setScreen(new SettingsScreen());
            }
            while (copyChatKey.consumeClick()) {
                client.setScreen(new com.wynnchayuan.client.ChatCopyScreen());
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
            // 有新版就在聊天室說一次，附下載連結。見 Releases#tellOnce。
            Releases.tellOnce(client);
            // 有新翻譯也說一次，但只說「有」，抓不抓由玩家決定。
            // 見 TranslationUpdate#tellOnce。
            com.wynnchayuan.translate.TranslationUpdate.tellOnce(client);
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
            // 右上那一欄（每日目標、世界事件）走計分板，跟追蹤欄是兩個事件
            WynntilsMod.registerEventListener(new ScoreboardListener());
            WynntilsMod.registerEventListener(new BadgeListener());
            WynntilsMod.registerEventListener(new ChatListener());
            WynntilsMod.registerEventListener(new TitleListener());
            WynntilsMod.registerEventListener(new MarketListener());
            // 市集搜尋打中文換成英文。這是唯一會改寫玩家自己打的字的地方，
            // 觸發條件由 Wynntils 的市集狀態決定，見 MarketListener。
            net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
                    .MODIFY_CHAT.register(MarketListener::rewrite);
            // 打字時在聊天框上方列出候選，對到好幾個物品時讓玩家自己點。
            com.wynnchayuan.client.MarketPicker.register();
            System.out.println("[WynnChaYuan] 已掛上 Wynntils 事件，開始收集");
        } catch (Throwable t) {
            System.err.println("[WynnChaYuan] 掛載失敗，停用收集功能: " + t);
        }
    }
}
