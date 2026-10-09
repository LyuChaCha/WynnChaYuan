package com.wynnchayuan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.StarterFiles;
import com.wynnchayuan.translate.TranslationCache;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 留著舊版的 {@code config/wynnchayuan} 升級，遊戲也要開得起來。
 *
 * <h2>回報長什麼樣</h2>
 * 玩家測過舊版、升上新版<b>沒刪</b>設定資料夾（語料收集開著），遊戲偶爾開不起來；
 * 刪掉資料夾就好了。那個資料夾裡有：還帶著已移除欄位 {@code shareCaptures} 的
 * {@code config.json}、{@code captured.json}、舊版的分享記錄 {@code shared.json}、
 * 一包診斷檔，以及從 GitHub 同步下來的譯文快取。
 *
 * <h2>這裡釘住什麼</h2>
 * 每一種「舊的、壞的、寫到一半的」檔都要：照樣建得起來、用預設值（或補回內建的），
 * 並且把壞檔<b>改名放旁邊</b>而不是留在原地等著被蓋掉。
 *
 * <p>最後一段拿整個資料夾照啟動順序跑一次：一份用內建資源拼出來的「舊版資料夾」
 * 一定跑；指定 {@code -PplayerFolder=} 時再拿真實玩家資料夾的副本跑一次
 * （那份含玩家的字，不進 repo）。
 */
public final class UpgradeSafetyTest {

    private static int failures = 0;

    /** 玩家那份 config.json 的欄位（只有設定值，沒有玩家的字）。 */
    private static final String PLAYER_CONFIG = """
            {
              "tooltipMode": "REPLACE",
              "showOverlays": true,
              "collect": true,
              "shareCaptures": true,
              "notifiedVersion": "",
              "language": "",
              "fallbackLanguage": "",
              "source": "GITHUB",
              "debugDumps": true,
              "collectGuiText": true,
              "translateNametags": true,
              "nametagMode": "REPLACE",
              "panelSide": "AUTO",
              "translateItemNames": true,
              "panelGap": 12,
              "accentColor": "#6FA8D8",
              "dialogueHoldMs": 6000,
              "dialogueMode": "REPLACE",
              "choiceMode": "REPLACE",
              "chatMode": "BOTH",
              "translateTitles": true,
              "chatCopy": true,
              "shotMode": "KEY",
              "nametagHoldMs": 1500,
              "panelAnchor": "FOLLOW",
              "overlayPos": {},
              "overlaySize": {},
              "showBadges": true,
              "badgeStyle": "GRADIENT",
              "nametagRange": 6.0,
              "nametagAngle": 6.0,
              "fixedX": 20,
              "fixedY": 20
            }""";

    public static void main(String[] args) throws Exception {
        truncatedConfig();
        emptyOrZeroedConfig();
        oldSchemaConfig();
        playerShapedConfig();
        truncatedCaptured();
        oldSchemaCaptured();
        obsoleteFilesAreRemoved();
        brokenCopiesAreCapped();
        settingsThatUsedToReset();
        themeColourFollowsOldBorder();
        downloadValidation();
        partialStarterInstall();
        otherLanguageIsNotSwallowed();
        obsoleteSyncedFilesAreRemoved();
        newerCacheFormatIsSetAside();
        staleCacheRefreshedOnUpgrade();
        editedCacheSurvivesUpgrade();
        localOnlyCacheUntouchedOnUpgrade();
        brokenCacheFileIsRestored();
        oldVersionFolder();
        playerFolder();
        report();
    }

    // ── config.json ────────────────────────────────────────────────────

    private static void truncatedConfig() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-cfg");
        Path file = dir.resolve("config.json");
        String bad = "{\n  \"tooltipMode\": \"REPLACE\",\n  \"collect\": fal";
        Files.writeString(file, bad);

        CollectorConfig c = new CollectorConfig(file);
        check("半截的 config.json：建得起來，全部用預設值",
                c.tooltipMode() == CollectorConfig.TooltipMode.PANEL && c.collect()
                        && c.source() == CollectorConfig.Source.GITHUB);
        List<Path> aside = brokenCopies(dir, "config.json");
        check("半截的 config.json 改名放旁邊，原位空出來",
                aside.size() == 1 && !Files.exists(file));
        check("放旁邊的那份內容原封不動（要救可以救）",
                aside.size() == 1 && Files.readString(aside.get(0)).equals(bad));

        c.toggleCollect();                     // 會存檔
        check("之後存得出一份讀得懂的設定檔", !new CollectorConfig(file).collect()
                && brokenCopies(dir, "config.json").size() == 1);
        check("存檔沒有留下 config.json.tmp", !Files.exists(dir.resolve("config.json.tmp")));
    }

    private static void emptyOrZeroedConfig() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-cfg0");
        Path file = dir.resolve("config.json");
        Files.writeString(file, "");
        CollectorConfig empty = new CollectorConfig(file);
        check("空的 config.json：預設值、改名放旁邊",
                empty.tooltipMode() == CollectorConfig.TooltipMode.PANEL
                        && brokenCopies(dir, "config.json").size() == 1);

        // NTFS 上寫到一半斷電，檔案常常是正確長度但內容全是 0
        Files.write(file, new byte[64]);
        new CollectorConfig(file);
        check("整份是 0 位元組的 config.json：一樣改名放旁邊",
                brokenCopies(dir, "config.json").size() == 2 && !Files.exists(file));

        // 用記事本以 Big5 另存
        Files.write(file, "{\"language\": \"繁中\"}".getBytes("Big5"));
        new CollectorConfig(file);
        check("不是 UTF-8 的 config.json：一樣改名放旁邊",
                brokenCopies(dir, "config.json").size() == 3);
    }

    private static void oldSchemaConfig() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-cfgold");
        Path file = dir.resolve("config.json");
        Files.writeString(file, """
                {"shareCaptures": true, "showPanel": false, "collect": false,
                 "tooltipMode": "SIDEBAR", "panelGap": "wide", "language": null,
                 "nametagRange": 1e9, "dialogueHoldMs": -5, "chatMode": "both",
                 "overlayPos": {"DIALOGUE": [1, "x"], "TRACKER": [5, 6], "NAMETAG": "oops"},
                 "overlaySize": [], "badgeStyle": 3, "fixedX": 40, "debugDumps": "true"}""");
        CollectorConfig c = new CollectorConfig(file);
        check("型別不對的欄位不會連累後面的欄位（collect=false、fixedX=40 都讀到）",
                !c.collect() && c.fixedX() == 40);
        check("不認得的 tooltipMode 退回舊欄位 showPanel=false 的換算（OFF）",
                c.tooltipMode() == CollectorConfig.TooltipMode.OFF && !c.showOverlays());
        check("\"wide\" 的 panelGap、null 的 language、數字的 badgeStyle 用預設",
                c.panelGap() == 12 && c.language().isEmpty()
                        && c.badgeStyle() == CollectorConfig.BadgeStyle.GRADIENT);
        check("極端數值夾回範圍（名牌距離 64、對話停留 = 持續顯示）",
                c.nametagRange() == 64.0 && c.dialogueHoldMs() == Integer.MAX_VALUE);
        check("小寫的列舉值、字串的布林值也認得", c.chatMode() == CollectorConfig.ChatMode.BOTH
                && c.debugDumps());
        check("小框位置：讀得懂的留著、讀不懂的那一個用預設",
                c.hasOverlayPos(CollectorConfig.Overlay.TRACKER)
                        && c.overlayX(CollectorConfig.Overlay.TRACKER) == 5
                        && !c.hasOverlayPos(CollectorConfig.Overlay.DIALOGUE)
                        && !c.hasOverlayPos(CollectorConfig.Overlay.NAMETAG));
        check("JSON 本身是好的，不改名放旁邊", brokenCopies(dir, "config.json").isEmpty());
        JsonObject rewritten = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        check("讀完就重寫：shareCaptures、showPanel 從檔案裡消失",
                !rewritten.has("shareCaptures") && !rewritten.has("showPanel"));
        CollectorConfig again = new CollectorConfig(file);
        check("重寫後的設定再讀一次，值沒有跑掉",
                !again.collect() && again.tooltipMode() == CollectorConfig.TooltipMode.OFF
                        && again.fixedX() == 40);
    }

    private static void playerShapedConfig() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-cfgplayer");
        Path file = dir.resolve("config.json");
        Files.writeString(file, PLAYER_CONFIG);
        CollectorConfig c = new CollectorConfig(file);
        check("玩家那份設定照樣讀得懂", c.tooltipMode() == CollectorConfig.TooltipMode.REPLACE
                && c.debugDumps() && c.collectGuiText()
                && c.itemNames() == CollectorConfig.ItemNames.ON
                && c.source() == CollectorConfig.Source.GITHUB);
        JsonObject expected = JsonParser.parseString(PLAYER_CONFIG).getAsJsonObject();
        expected.remove("shareCaptures");
        // 這兩欄先前從來沒被寫出去過（見 settingsThatUsedToReset），舊設定檔裡不會有，
        // 重寫之後會帶著預設值出現。
        expected.addProperty("uiLanguage", "");
        expected.addProperty("marketSearch", true);
        // 0.2.0 加的：右上那一欄怎麼翻、目標進度條要不要翻。
        // 舊設定檔沒有，重寫時補上預設值。
        expected.addProperty("trackerMode", "REPLACE");
        expected.addProperty("translateObjectives", true);
        // 1.0.0：記分板的開關。舊設定檔沒有，預設開著（跟原本的行為一樣）。
        expected.addProperty("translateScoreboard", true);
        expected.addProperty("translateHeldItem", true);
        // 0.2.2：物品名稱從開／關改成三段。舊的 true 換成「只顯示譯名」。
        expected.remove("translateItemNames");
        expected.addProperty("itemNames", "ON");
        // 0.2.2：「跟別人講話請用原文」的警語。升級上來的人還沒看過，所以是 false。
        expected.addProperty("noticeDismissed", false);
        // 0.2.3：Wynntils 自己那幾個畫面要不要換成中文。舊設定檔沒有，補上預設值。
        expected.addProperty("wynntilsUi", true);
        // 0.2.3：按住 Shift 暫時看另一種物品名稱。舊設定檔沒有，補上預設值。
        expected.addProperty("shiftPeekNames", true);
        // 0.2.3：翻譯改成「先問有沒有新的」。上一次抓到哪一版、要不要自己抓。
        expected.addProperty("syncedTranslations", "");
        expected.addProperty("autoUpdateTranslations", false);
        // 0.2.3_1：boss bar 自己的開關（issue #825）。舊設定檔沒有，補上預設值。
        expected.addProperty("translateBossBar", true);
        // 0.2.3_1：translateNametags 拿掉了。它沒有接到 F6 卻擋著算繪，正是 #825
        // 的病根；浮空字改成跟「名牌與漂浮字」那一列走。
        expected.remove("translateNametags");
        // 0.2.6：F6 的譯文版本改印日期，不再印 commit 短碼（使用者說那看起來像
        // 亂碼）。日期跟 SHA 一起記在設定檔裡，舊設定檔沒有這一欄，補上空字串
        // ——空的代表不知道日期，下一次問版本時會補回來，
        // 見 TranslationUpdate#adoptDate。
        expected.addProperty("syncedTranslationsDate", "");
        // 設定畫面改版：畫面自己的主題色（themeColor）從框線顏色拆出來。舊設定檔沒有
        // 這一欄，補上的是<b>原本的框線顏色</b>——升級之後畫面的顏色不能自己變。
        expected.addProperty("themeColor", expected.has("accentColor")
                ? expected.get("accentColor").getAsString() : "#6FA8D8");
        // 同一版：舊的預設鍵（F6、F9）清過了沒。舊設定檔沒有，補上「還沒」——
        // 清的動作在遊戲啟動完之後才做，見 WynnChaYuan#releaseOldDefaultKeys。
        expected.addProperty("oldKeysReleased", false);
        JsonObject rewritten = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        check("重寫後少了 shareCaptures、多了補寫的那幾欄，其他每一欄都一樣",
                expected.equals(rewritten));
    }

    /**
     * F6 選的介面語言與市集搜尋開關要記得住。
     *
     * <h2>先前壞在哪</h2>
     * {@code setUiLanguage} 與 {@code toggleMarketSearch} 都有呼叫 {@code save()}，
     * 但 {@code save} 沒寫這兩欄、{@code load} 也沒讀——設定檔裡從來沒有它們，
     * 於是每次重開遊戲都回到預設。日文、韓文的介面檔早就翻好了，選了卻留不住。
     */
    /**
     * 設定畫面的主題色是從框線顏色拆出來的。
     *
     * <p>把框線調成別的顏色的玩家，升級之後設定畫面要<b>維持他原本看到的顏色</b>，
     * 不能自己跳回預設的藍。之後兩個各自改、各自存。
     */
    private static void themeColourFollowsOldBorder() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-theme");
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{ \"accentColor\": \"#C0FFEE\" }");
        CollectorConfig old = new CollectorConfig(file);
        check("★ 舊設定檔只有框線顏色：主題色沿用它", "#C0FFEE".equals(old.themeColor()));
        check("改主題色不動框線", old.setThemeColor("#112233")
                && "#C0FFEE".equals(old.accentColor()));
        check("改框線不動主題色", old.setAccentColor("#445566")
                && "#112233".equals(old.themeColor()));
        CollectorConfig again = new CollectorConfig(file);
        check("重開之後兩個顏色各自還在", "#112233".equals(again.themeColor())
                && "#445566".equals(again.accentColor()));
        check("拖色盤時的寫法不合格式就不收", !again.setThemeColorLive("藍色")
                && "#112233".equals(again.themeColor()));
    }

    private static void settingsThatUsedToReset() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-keep");
        Path file = dir.resolve("config.json");
        CollectorConfig first = new CollectorConfig(file);
        first.setUiLanguage("ja_jp");
        boolean market = first.toggleMarketSearch();   // 預設開著，切成關
        check("市集搜尋預設是開的，切過之後是關的", !market);

        CollectorConfig reopened = new CollectorConfig(file);
        check("★ 重開之後介面語言還是 ja_jp（先前會變回跟著譯文語言）",
                "ja_jp".equals(reopened.uiLanguage()));
        check("★ 重開之後市集搜尋還是關的（先前會變回開啟）", !reopened.marketSearch());

        // 手寫的設定檔也讀得到
        Files.writeString(file, "{\"uiLanguage\": \" ko_kr \", \"marketSearch\": false}");
        CollectorConfig handWritten = new CollectorConfig(file);
        check("手寫的設定檔讀得到這兩欄，前後空白照樣去掉",
                "ko_kr".equals(handWritten.uiLanguage()) && !handWritten.marketSearch());

        // 型別不對時跟別的欄位一樣：只有那一欄回預設
        Files.writeString(file, "{\"uiLanguage\": 5, \"marketSearch\": \"nope\", \"collect\": false}");
        CollectorConfig wrongTypes = new CollectorConfig(file);
        check("型別不對的那兩欄用預設，別的欄位照讀",
                wrongTypes.uiLanguage().isEmpty() && wrongTypes.marketSearch()
                        && !wrongTypes.collect());
    }

    // ── captured.json ──────────────────────────────────────────────────

    private static void truncatedCaptured() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-cap");
        Path file = dir.resolve("captured.json");
        CaptureStore first = new CaptureStore(file);
        for (int i = 0; i < 20; i++) {
            first.record("Some line number " + i + " that nobody translated", "desc", "quest",
                    "dialogue/Quest#Npc");
        }
        first.flush();
        byte[] whole = Files.readAllBytes(file);
        byte[] half = java.util.Arrays.copyOf(whole, whole.length / 2);
        Files.write(file, half);

        CaptureStore store = new CaptureStore(file);
        check("半截的 captured.json：建得起來，從空的開始", store.size() == 0);
        List<Path> aside = brokenCopies(dir, "captured.json");
        check("半截的 captured.json 改名放旁邊，內容原封不動",
                aside.size() == 1 && java.util.Arrays.equals(Files.readAllBytes(aside.get(0)), half));
        store.record("A brand new line after the crash", "desc", "quest", "dialogue/x");
        store.flush();
        check("之後照常寫出讀得懂的 captured.json，放旁邊那份沒有被蓋掉",
                SafeFiles.parsesAsObject(file) && Files.exists(aside.get(0))
                        && new CaptureStore(file).size() == 1);
    }

    private static void oldSchemaCaptured() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-capold");
        Path file = dir.resolve("captured.json");
        Files.writeString(file, """
                {"entries": {
                   "0001": {"src": "Old line one", "role": "desc", "domain": "quest", "ctx": "dialogue/Q#N"},
                   "0002": null,
                   "0003": {"dst": "no source"},
                   "0004": "garbage",
                   "0005": {"src": "Old line two", "seq": 7, "seen": 3, "dst": null}},
                 "untranslated": [null, 5, {"src": "Pending line", "seen": 2}]}""");
        CaptureStore store = new CaptureStore(file);
        check("舊格式的 captured.json：讀得懂的兩條留著，壞的幾條跳過", store.size() == 2);
        check("JSON 本身是好的，不改名放旁邊", brokenCopies(dir, "captured.json").isEmpty());
        store.record("Recorded after upgrade", "desc", "quest", "dialogue/x");
        store.flush();
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        JsonObject rows = root.getAsJsonObject("entries");
        boolean dstFilled = rows.entrySet().stream().allMatch(e ->
                e.getValue().getAsJsonObject().has("dst")
                        && e.getValue().getAsJsonObject().get("dst").isJsonPrimitive());
        int newSeq = rows.entrySet().stream()
                .map(e -> e.getValue().getAsJsonObject())
                .filter(r -> r.get("src").getAsString().equals("Recorded after upgrade"))
                .mapToInt(r -> r.get("seq").getAsInt()).findFirst().orElse(-1);
        check("舊紀錄沒有 dst 的補成空字串", dstFilled);
        check("流水號接在舊紀錄後面（實際 " + newSeq + "）", newSeq == 8);
        check("沒翻清單讀得懂的那一條留著",
                root.has("untranslated") && root.getAsJsonArray("untranslated").size() == 1);
    }

    // ── 舊版留下的檔 ────────────────────────────────────────────────────

    private static void obsoleteFilesAreRemoved() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-tidy");
        Files.writeString(dir.resolve("shared.json"),
                "{\"note\":\"已經分享過的字串雜湊\",\"greeted\":true,\"hashes\":[\"eea66f504b32\"]}");
        Files.writeString(dir.resolve("config.json.tmp"), "{\"tooltip");
        Files.writeString(dir.resolve("captured.json.tmp"), "{\"entries\": {");
        Files.writeString(dir.resolve("captured.json"), "{}");
        Files.writeString(dir.resolve("third-party-literals.json"), "{}");
        Files.writeString(dir.resolve("error-debug.txt"), "trace");
        Path zh = Files.createDirectories(dir.resolve("translations/zh_tw/ability"));
        Files.writeString(zh.getParent().resolve("npc.json.tmp"), "{\"ent");
        Files.writeString(zh.resolve("mage.json.tmp"), "{\"ent");
        Files.writeString(zh.getParent().resolve("npc.json"), "{}");

        int removed = ConfigFolder.tidy(dir);
        check("清掉 shared.json 與四個暫存檔（實際 " + removed + " 個）", removed == 5
                && !Files.exists(dir.resolve("shared.json"))
                && !Files.exists(dir.resolve("config.json.tmp"))
                && !Files.exists(zh.resolve("mage.json.tmp")));
        check("玩家的檔、診斷檔、譯文一個都沒動",
                Files.exists(dir.resolve("captured.json"))
                        && Files.exists(dir.resolve("third-party-literals.json"))
                        && Files.exists(dir.resolve("error-debug.txt"))
                        && Files.exists(zh.getParent().resolve("npc.json")));
        check("資料夾不存在（第一次安裝）時什麼都不做",
                ConfigFolder.tidy(dir.resolve("nope")) == 0);
    }

    private static void brokenCopiesAreCapped() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-cap3");
        Path file = dir.resolve("x.json");
        for (int i = 0; i < 5; i++) {
            Files.writeString(file, "{" + i);
            SafeFiles.setAside(file, "測試");
        }
        check("同一個檔的壞檔最多留 " + SafeFiles.KEEP_BROKEN + " 份",
                brokenCopies(dir, "x.json").size() == SafeFiles.KEEP_BROKEN);
    }

    private static void downloadValidation() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-dl");
        check("完整的 JSON 物件才算下載成功", accepts(dir, "{\"a\": {\"b\": [1, 2]}}"));
        check("連線中途斷掉的半截檔不算（先前只看開頭的「{」就放行）",
                !accepts(dir, "{\"entries\": {\"x\": {\"src\": \"a\""));
        check("錯誤頁面不算", !accepts(dir, "<html>404</html>"));
        check("物件後面還有東西不算", !accepts(dir, "{}garbage"));
        check("空檔不算", !accepts(dir, ""));
    }

    private static boolean accepts(Path dir, String body) throws Exception {
        Path f = dir.resolve("download.json.tmp");
        Files.writeString(f, body);
        return SafeFiles.parsesAsObject(f);
    }

    // ── 譯文快取 ────────────────────────────────────────────────────────

    private static void partialStarterInstall() throws Exception {
        Path root = Files.createTempDirectory("wcy-up-starter");
        try {
            Path dir = Files.createDirectories(root.resolve("translations/zh_tw"));
            // 第一次倒工作檔時被關掉：只倒出一個檔，清單還沒寫
            Files.writeString(dir.resolve("ui-labels.json"), "{\"Custom\": \"自訂\"}");
            int written = StarterFiles.installIfEmpty(dir, "zh_tw");
            check("上次沒倒完（有譯文檔、沒有清單）：把缺的補上（實際 " + written + " 個）",
                    written > 10 && Files.exists(dir.resolve("npc.json"))
                            && Files.exists(dir.resolve("_index.json")));
            check("已經在的檔一個都不蓋",
                    Files.readString(dir.resolve("ui-labels.json")).contains("自訂"));
            check("補齊之後不會再倒第二次", StarterFiles.installIfEmpty(dir, "zh_tw") == 0);

            Path onlyStamp = Files.createDirectories(root.resolve("translations/zh_cn"));
            Files.writeString(onlyStamp.resolve("_cache.json"), "{\"format\": 1}");
            check("只有底線開頭的記錄檔的資料夾等於空的，照樣倒工作檔",
                    StarterFiles.installIfEmpty(onlyStamp, "zh_tw") > 10);
        } finally {
            deleteTree(root);
        }
    }

    private static void otherLanguageIsNotSwallowed() throws Exception {
        Path config = Files.createTempDirectory("wcy-up-flat");
        Path zh = Files.createDirectories(config.resolve("translations/zh_tw/ability"));
        Files.writeString(zh.getParent().resolve("npc.json"), "{}");
        Files.writeString(zh.resolve("mage.json"), "{}");
        int moved = Languages.migrateFlat(config, "zh_cn");
        check("★ 切到還沒有資料夾的語言時，別的語言的快取不會被當成扁平檔搬走（實際搬了 "
                        + moved + " 個）",
                moved == 0 && Files.exists(zh.resolve("mage.json"))
                        && !Files.exists(config.resolve("translations/zh_cn/zh_tw")));

        Path legacy = Files.createTempDirectory("wcy-up-flat2");
        Path flat = Files.createDirectories(legacy.resolve("translations/ability"));
        Files.writeString(flat.getParent().resolve("npc.json"), "{}");
        Files.writeString(flat.resolve("mage.json"), "{}");
        check("真正舊版的扁平目錄照樣搬進語言資料夾",
                Languages.migrateFlat(legacy, "zh_tw") == 2
                        && Files.exists(legacy.resolve("translations/zh_tw/ability/mage.json")));
    }

    private static void obsoleteSyncedFilesAreRemoved() throws Exception {
        Path dir = Files.createDirectories(
                Files.createTempDirectory("wcy-up-stamp").resolve("translations/zh_tw"));
        for (String name : List.of("kept.json", "gone.json", "later.json", "mine.json")) {
            Files.writeString(dir.resolve(name), "{}");
        }
        List<String> first = List.of("kept.json", "gone.json", "later.json");
        TranslationCache.record(dir, first, first, true);
        int removed = TranslationCache.record(dir, List.of("kept.json", "later.json"),
                List.of("kept.json", "later.json"), true);
        check("repo 上已經不列的同步檔會刪掉（實際 " + removed + " 個）",
                removed == 1 && !Files.exists(dir.resolve("gone.json")));
        check("使用者自己放的檔（從來不是同步來的）不刪", Files.exists(dir.resolve("mine.json")));
        int offline = TranslationCache.record(dir, List.of("kept.json"), List.of("kept.json"), false);
        check("遠端清單這次沒抓到時一個都不刪",
                offline == 0 && Files.exists(dir.resolve("later.json")));
        JsonObject stamp = JsonParser.parseString(
                Files.readString(dir.resolve("_cache.json"))).getAsJsonObject();
        check("戳記寫著格式版本", stamp.get("format").getAsInt() == TranslationCache.FORMAT);
    }

    /** 0.2.0_4 實機回報：舊版同步下來的快取蓋住了新版 jar 內建的譯文。 */
    private static void staleCacheRefreshedOnUpgrade() throws Exception {
        Path config = Files.createTempDirectory("wcy-up-stale");
        String before = TranslationCache.modVersion;
        try {
            Path dir = Languages.dir(config, "zh_tw");
            StarterFiles.installIfEmpty(dir, "zh_tw");
            Files.writeString(dir.resolve("misc.json"), "{\"Win Dungeons\": \"WCY_STALE_MARKER\"}");
            Files.writeString(dir.resolve("mine.json"), "{\"Mine\": \"我的\"}");
            Files.writeString(dir.resolve("_cache.json"),
                    "{\"format\": 1, \"mod\": \"0.0.1\", \"files\": [\"misc.json\"]}");
            TranslationCache.modVersion = "9.9.9";
            TranslationCache.prepare(dir, "zh_tw");
            String misc = Files.readString(dir.resolve("misc.json"));
            check("換版本後同步來的舊檔換回內建的", !misc.contains("WCY_STALE_MARKER") && misc.length() > 1000);
            check("自己放的檔不動", Files.readString(dir.resolve("mine.json")).contains("我的"));
            check("戳記的版本改成這一版",
                    Files.readString(dir.resolve("_cache.json")).contains("9.9.9"));
            Files.writeString(dir.resolve("misc.json"), "{\"Win Dungeons\": \"同步來的新譯文\"}");
            TranslationCache.prepare(dir, "zh_tw");
            check("同一版再啟動不重做（同步來的檔留著）",
                    Files.readString(dir.resolve("misc.json")).contains("同步來的新譯文"));
        } finally {
            TranslationCache.modVersion = before;
            deleteTree(config);
        }
    }

    /**
     * #979 實機回報：自己改過的譯文檔，升一次版就被 jar 內建的那份整份蓋掉。
     *
     * <p>戳記記著同步那一刻的雜湊，所以「原封不動」與「有人改過」分得出來：
     * 前者該換成新版 jar 的（那是 0.2.0_4 那個回報），後者不能碰。
     */
    private static void editedCacheSurvivesUpgrade() throws Exception {
        Path config = Files.createTempDirectory("wcy-up-edited");
        String before = TranslationCache.modVersion;
        try {
            Path dir = Languages.dir(config, "zh_tw");
            StarterFiles.installIfEmpty(dir, "zh_tw");
            // 同步下來的兩個檔，戳記連雜湊一起記
            Files.writeString(dir.resolve("misc.json"), "{\"Win Dungeons\": \"同步來的\"}");
            Files.writeString(dir.resolve("npc.json"), "{\"Aledar\": \"同步來的\"}");
            List<String> synced = List.of("misc.json", "npc.json");
            TranslationCache.record(dir, synced, synced, true);
            JsonObject stamp = JsonParser.parseString(
                    Files.readString(dir.resolve("_cache.json"))).getAsJsonObject();
            check("戳記記下了同步那一刻的雜湊",
                    stamp.has("hashes") && stamp.getAsJsonObject("hashes").has("misc.json"));
            // 之後譯者自己改了其中一個
            Files.writeString(dir.resolve("misc.json"), "{\"Win Dungeons\": \"我自己改的\"}");

            TranslationCache.modVersion = "9.9.9";
            TranslationCache.prepare(dir, "zh_tw");
            check("自己改過的檔升版後還在",
                    Files.readString(dir.resolve("misc.json")).contains("我自己改的"));
            String npc = Files.readString(dir.resolve("npc.json"));
            check("沒動過的檔照舊換回內建的",
                    !npc.contains("同步來的") && npc.length() > 1000);

            // 改過的那個檔，新的戳記要記著它現在的內容，不然下一次升版又被判成「沒動過」
            Files.writeString(dir.resolve("npc.json"), "{\"Aledar\": \"又是同步來的\"}");
            TranslationCache.modVersion = "9.9.10";
            TranslationCache.prepare(dir, "zh_tw");
            check("再升一版，自己改的還是沒被碰",
                    Files.readString(dir.resolve("misc.json")).contains("我自己改的"));
        } finally {
            TranslationCache.modVersion = before;
            deleteTree(config);
        }
    }

    /** #979：切到「只用本機檔案」的人，升版時整個快取資料夾不該被動。 */
    private static void localOnlyCacheUntouchedOnUpgrade() throws Exception {
        Path config = Files.createTempDirectory("wcy-up-local");
        String before = TranslationCache.modVersion;
        boolean syncing = TranslationCache.syncsFromGitHub;
        try {
            Path dir = Languages.dir(config, "zh_tw");
            StarterFiles.installIfEmpty(dir, "zh_tw");
            Files.writeString(dir.resolve("misc.json"), "{\"Win Dungeons\": \"我自己翻的\"}");
            // 以前同步過，所以戳記裡有這個檔名（0.2.7 以前的戳記，沒有雜湊）
            Files.writeString(dir.resolve("_cache.json"),
                    "{\"format\": 1, \"mod\": \"0.0.1\", \"files\": [\"misc.json\"]}");
            TranslationCache.modVersion = "9.9.9";
            TranslationCache.syncsFromGitHub = false;
            TranslationCache.prepare(dir, "zh_tw");
            check("只用本機檔案時，升版不換任何同步來的檔",
                    Files.readString(dir.resolve("misc.json")).contains("我自己翻的"));
            check("戳記也不動（切回 GitHub 時還認得出來）",
                    Files.readString(dir.resolve("_cache.json")).contains("0.0.1"));
        } finally {
            TranslationCache.modVersion = before;
            TranslationCache.syncsFromGitHub = syncing;
            deleteTree(config);
        }
    }

    private static void newerCacheFormatIsSetAside() throws Exception {
        Path config = Files.createTempDirectory("wcy-up-format");
        try {
            Path dir = Languages.dir(config, "zh_tw");
            StarterFiles.installIfEmpty(dir, "zh_tw");
            Files.writeString(dir.resolve("mine.json"), "{\"Mine\": \"我的\"}");

            Files.writeString(dir.resolve("_cache.json"), "{\"format\": 1}");
            check("格式相同的快取照用", !TranslationCache.check(dir) && Files.exists(dir));

            Files.writeString(dir.resolve("_cache.json"), "{\"format");
            check("戳記壞了：當成沒有戳記，快取照用，戳記改名放旁邊",
                    !TranslationCache.check(dir) && Files.exists(dir.resolve("npc.json"))
                            && brokenCopies(dir, "_cache.json").size() == 1);

            Files.writeString(dir.resolve("_cache.json"),
                    "{\"format\": 99, \"mod\": \"9.9.9\", \"files\": []}");
            check("別的格式寫的快取整包移開", TranslationCache.check(dir) && !Files.exists(dir));
            Path old;
            try (Stream<Path> kept = Files.list(config.resolve("translations.old"))) {
                old = kept.findFirst().orElse(null);
            }
            check("移開的快取留在 translations.old/，自己放的檔還在",
                    old != null && Files.exists(old.resolve("mine.json")));
            int written = TranslationCache.prepare(dir, "zh_tw");
            check("移開之後從內建重新倒一份（實際 " + written + " 個）", written > 10);
        } finally {
            deleteTree(config);
        }
    }

    private static void brokenCacheFileIsRestored() throws Exception {
        Path config = Files.createTempDirectory("wcy-up-repair");
        try {
            Path dir = Languages.dir(config, "zh_tw");
            StarterFiles.installIfEmpty(dir, "zh_tw");
            TranslationStore fresh = new TranslationStore();
            fresh.loadAll(List.of(dir));
            int full = fresh.size();

            byte[] npc = Files.readAllBytes(dir.resolve("npc.json"));
            Files.write(dir.resolve("npc.json"), java.util.Arrays.copyOf(npc, npc.length / 2));
            Files.writeString(dir.resolve("quest-name.json"), "");

            TranslationStore store = new TranslationStore();
            int broken = TranslationCache.loadRepairing(store, List.of(dir));
            check("半截的 npc.json 與空的 quest-name.json 都被認出來（實際 " + broken + " 個）",
                    broken == 2);
            check("從內建補回來再載一次，條數跟完好時一樣（" + store.size() + " / " + full + "）",
                    store.size() == full && full > 10000);
            check("補回來的檔是完整的 JSON", SafeFiles.parsesAsObject(dir.resolve("npc.json"))
                    && SafeFiles.parsesAsObject(dir.resolve("quest-name.json")));
            check("半截的那份改名放旁邊", brokenCopies(dir, "npc.json").size() == 1);
            check("補回之後 brokenFiles 是空的", store.brokenFiles().isEmpty());
        } finally {
            deleteTree(config);
        }
    }

    // ── 整個資料夾照啟動順序跑一次 ─────────────────────────────────────

    /** 啟動時讀檔的結果。 */
    private record Started(CollectorConfig config, CaptureStore store,
                           TranslationStore translations, int broken, long millis) {}

    /**
     * 照 {@code WynnChaYuan#onInitializeClient} 的順序呼叫同一批讀檔的方法。
     *
     * <p>那個類別本身載不進測試（靜態欄位會去註冊按鍵分類），所以這裡照抄順序，
     * 呼叫的是完全相同的方法。
     */
    private static Started startUp(Path dir) {
        long t0 = System.nanoTime();
        ConfigFolder.tidy(dir);
        CaptureStore store = new CaptureStore(dir.resolve("captured.json"));
        CollectorConfig config = new CollectorConfig(dir.resolve("config.json"));
        String lang = Languages.pick(config.language(), "");
        Languages.migrateFlat(dir, lang);
        Path trDir = Languages.dir(dir, lang);
        TranslationCache.prepare(trDir, lang);
        List<Path> layers = new ArrayList<>();
        String under = config.fallbackLanguage().isEmpty()
                ? Languages.fallbackFor(lang) : null;
        if (under != null) {
            Path f = Languages.dir(dir, under);
            TranslationCache.prepare(f, under);
            layers.add(f);
        }
        layers.add(trDir);
        TranslationStore translations = new TranslationStore();
        int broken = TranslationCache.loadRepairing(translations, layers);
        store.knowsTranslations(translations::hasTranslation);
        store.flush();
        return new Started(config, store, translations, broken,
                (System.nanoTime() - t0) / 1_000_000);
    }

    /** 用內建資源拼出一份「測過舊版、留著沒刪」的資料夾，一定跑。 */
    private static void oldVersionFolder() throws Exception {
        Path dir = Files.createTempDirectory("wcy-up-old").resolve("wynnchayuan");
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("config.json"), PLAYER_CONFIG);
            Files.writeString(dir.resolve("shared.json"),
                    "{\"note\":\"已經分享過的字串雜湊\",\"greeted\":true,\"hashes\":[\"eea66f504b32\"]}");
            CaptureStore old = new CaptureStore(dir.resolve("captured.json"));
            old.record("A line nobody has translated yet", "desc", "quest", "dialogue/Q#N");
            old.flush();
            Files.writeString(dir.resolve("captured.json.tmp"), "{\"entries\": {");
            Files.writeString(dir.resolve("layout-debug.txt"), "# 置中判斷");
            Path tr = Languages.dir(dir, "zh_tw");
            StarterFiles.installIfEmpty(tr, "zh_tw");
            byte[] quest = Files.readAllBytes(tr.resolve("quest.json"));
            Files.write(tr.resolve("quest.json"), java.util.Arrays.copyOf(quest, quest.length / 3));
            Files.writeString(tr.resolve("quest-dialogue.json.tmp"), "{\"entr");

            Started s = startUp(dir);
            System.out.println("  （舊版資料夾啟動讀檔 " + s.millis() + " ms，譯文 "
                    + s.translations().size() + " 條）");
            check("舊版資料夾：設定照讀、收集照讀", s.config().debugDumps()
                    && s.config().source() == CollectorConfig.Source.GITHUB && s.store().size() == 1);
            check("舊版資料夾：shared.json 與暫存檔清掉，診斷檔留著",
                    !Files.exists(dir.resolve("shared.json"))
                            && !Files.exists(dir.resolve("captured.json.tmp"))
                            && !Files.exists(tr.resolve("quest-dialogue.json.tmp"))
                            && Files.exists(dir.resolve("layout-debug.txt")));
            check("舊版資料夾：半截的 quest.json 補回來，譯文照常載入（壞檔 " + s.broken() + " 個）",
                    s.broken() == 1 && SafeFiles.parsesAsObject(tr.resolve("quest.json"))
                            && s.translations().size() > 10000);
            check("舊版資料夾：config.json 裡的 shareCaptures 清掉了",
                    !Files.readString(dir.resolve("config.json")).contains("shareCaptures"));
        } finally {
            deleteTree(dir.getParent());
        }
    }

    /** 指定 -PplayerFolder= 時，拿真實玩家資料夾的副本跑一次。 */
    private static void playerFolder() throws Exception {
        String given = System.getProperty("wcy.playerFolder", "");
        if (given.isBlank()) {
            System.out.println("  [SKIP] 沒有指定 -PplayerFolder，略過真實玩家資料夾");
            return;
        }
        Path source = Path.of(given);
        if (!Files.isDirectory(source)) {
            check("-PplayerFolder 指到的資料夾存在（" + given + "）", false);
            return;
        }
        Path dir = Files.createTempDirectory("wcy-up-player").resolve("wynnchayuan");
        try {
            copyTree(source, dir);
            boolean hadShared = Files.exists(dir.resolve("shared.json"));
            Started s = startUp(dir);
            System.out.println("  （玩家資料夾啟動讀檔 " + s.millis() + " ms，譯文 "
                    + s.translations().size() + " 條，缺口 " + s.store().size() + " 條，壞檔 "
                    + s.broken() + " 個）");
            check("玩家資料夾：譯文照常載入", s.translations().size() > 10000);
            check("玩家資料夾：設定讀得懂、沒有被改名放旁邊",
                    brokenCopies(dir, "config.json").isEmpty()
                            && s.config().source() == CollectorConfig.Source.GITHUB);
            check("玩家資料夾：captured.json 讀得懂、沒有被改名放旁邊",
                    brokenCopies(dir, "captured.json").isEmpty()
                            && SafeFiles.parsesAsObject(dir.resolve("captured.json")));
            if (hadShared) {
                check("玩家資料夾：舊的 shared.json 清掉了", !Files.exists(dir.resolve("shared.json")));
            }
            check("玩家資料夾：config.json 不再帶著 shareCaptures",
                    !Files.readString(dir.resolve("config.json")).contains("shareCaptures"));
        } finally {
            deleteTree(dir.getParent());       // 副本裡有玩家的字，跑完就刪
        }
    }

    // ── 工具 ────────────────────────────────────────────────────────────

    private static List<Path> brokenCopies(Path dir, String name) throws Exception {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith(name + SafeFiles.BROKEN))
                        .sorted().toList();
        }
    }

    private static void copyTree(Path from, Path to) throws Exception {
        try (Stream<Path> all = Files.walk(from)) {
            for (Path p : all.toList()) {
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(p, target);
                }
            }
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> all = Files.walk(root)) {
            for (Path p : all.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        } catch (Exception ignored) {
            // 暫存資料夾刪不乾淨不影響結果
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("升級安全：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("升級安全：全部通過");
    }
}
