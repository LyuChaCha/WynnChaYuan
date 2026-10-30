package com.wynnchayuan;

import com.wynnchayuan.translate.RemoteSync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FirstRunTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        freshInstall();
        recommendedPreset();
        upgradeShowsOnce();
        consentGatesServices();
        servicesRefuseWithoutConsent();
        optOut();
        deleteAnswers();
        selfCapeCache();
        if (failures > 0) {
            System.out.println("首次啟動：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("首次啟動：全部通過");
    }

    private static CollectorConfig fresh() throws Exception {
        return new CollectorConfig(Files.createTempDirectory("wcy-first").resolve("config.json"));
    }

    private static CollectorConfig withConsent(CollectorConfig.Consent consent) throws Exception {
        CollectorConfig c = fresh();
        c.setConsent(consent);
        return c;
    }

    private static void freshInstall() throws Exception {
        CollectorConfig c = fresh();
        check("新安裝：物品翻譯預設關閉", c.tooltipMode() == CollectorConfig.TooltipMode.OFF);
        check("新安裝：物品名稱維持原文", c.itemNames() == CollectorConfig.ItemNames.OFF);
        check("新安裝：還沒回答同意、還沒選設定，首次啟動畫面要跳",
                c.consent() == CollectorConfig.Consent.UNKNOWN && !c.setupDone()
                        && c.needsFirstRun() && !c.existedBefore());
        check("新安裝：連線一律關著", !NetworkGate.online(c) && !NetworkGate.registry(c));
    }

    private static String display(CollectorConfig c) {
        return String.join(",", List.of(
                String.valueOf(c.translationsEnabled()), c.dialogueMode().name(),
                c.choiceMode().name(), c.chatMode().name(), c.nametagMode().name(),
                c.tooltipMode().name(), c.itemNames().name(), c.pageNames().name(),
                c.trackerMode().name(), String.valueOf(c.translateScoreboard()),
                String.valueOf(c.translateObjectives()), String.valueOf(c.translateTitles()),
                String.valueOf(c.translateBossBar()), String.valueOf(c.translateHeldItem()),
                String.valueOf(c.wynntilsUi()), String.valueOf(c.showOverlays())));
    }

    private static void recommendedPreset() throws Exception {
        Path dir = Files.createTempDirectory("wcy-preset");
        Path file = dir.resolve("config.json");
        Files.writeString(file, """
                {"tooltipMode": "PANEL", "itemNames": "BOTH", "dialogueMode": "OFF",
                 "choiceMode": "PANEL", "chatMode": "OFF", "nametagMode": "LOOK_AT",
                 "trackerMode": "OFF", "translateScoreboard": false, "translateTitles": false,
                 "translateBossBar": false, "wynntilsUi": false, "translationsEnabled": false,
                 "showOverlays": false, "pageNames": "OFF", "panelGap": 40}""");
        CollectorConfig c = new CollectorConfig(file);
        c.applyRecommended();
        check("推薦設定：對話與選項就地取代",
                c.dialogueMode() == CollectorConfig.DialogueMode.REPLACE
                        && c.choiceMode() == CollectorConfig.DialogueMode.REPLACE);
        check("推薦設定：聊天原文加譯文", c.chatMode() == CollectorConfig.ChatMode.BOTH);
        check("推薦設定：名牌就地取代", c.nametagMode() == CollectorConfig.NametagMode.REPLACE);
        check("推薦設定：物品翻譯關閉、物品名稱原文",
                c.tooltipMode() == CollectorConfig.TooltipMode.OFF
                        && c.itemNames() == CollectorConfig.ItemNames.OFF);
        check("推薦設定：追蹤欄就地取代、記分板與目標翻譯",
                c.trackerMode() == CollectorConfig.DialogueMode.REPLACE
                        && c.translateScoreboard() && c.translateObjectives());
        check("推薦設定：大字、Boss 血條、Wynntils 介面、總開關都開",
                c.translateTitles() && c.translateBossBar() && c.wynntilsUi()
                        && c.translationsEnabled() && c.showOverlays());
        check("推薦設定不碰版面設定", c.panelGap() == 40);
        check("推薦設定就是新安裝的預設值", display(c).equals(display(fresh())));
        check("推薦設定寫進檔案", display(new CollectorConfig(file)).equals(display(c)));

        CollectorConfig custom = fresh();
        custom.setDialogueModes(CollectorConfig.DialogueMode.PANEL);
        custom.setChatMode(CollectorConfig.ChatMode.OFF);
        custom.setNametagMode(CollectorConfig.NametagMode.LOOK_AT);
        custom.setTooltipMode(CollectorConfig.TooltipMode.REPLACE);
        custom.setItemNames(CollectorConfig.ItemNames.BOTH);
        custom.setTrackerMode(CollectorConfig.DialogueMode.OFF);
        custom.setTranslateScoreboard(false);
        custom.setTranslateTitles(false);
        custom.setTranslateBossBar(false);
        custom.setWynntilsUi(false);
        check("自訂：每一項都照選的",
                custom.dialogueMode() == CollectorConfig.DialogueMode.PANEL
                        && custom.choiceMode() == CollectorConfig.DialogueMode.PANEL
                        && custom.chatMode() == CollectorConfig.ChatMode.OFF
                        && custom.nametagMode() == CollectorConfig.NametagMode.LOOK_AT
                        && custom.tooltipMode() == CollectorConfig.TooltipMode.REPLACE
                        && custom.itemNames() == CollectorConfig.ItemNames.BOTH
                        && custom.trackerMode() == CollectorConfig.DialogueMode.OFF
                        && !custom.translateScoreboard() && !custom.translateTitles()
                        && !custom.translateBossBar() && !custom.wynntilsUi());
    }

    private static void upgradeShowsOnce() throws Exception {
        Path dir = Files.createTempDirectory("wcy-upgrade");
        Path file = dir.resolve("config.json");
        Files.writeString(file, """
                {"tooltipMode": "REPLACE", "itemNames": "ON", "chatMode": "OFF",
                 "notifiedVersion": "1.0.4", "oldKeysReleased": true}""");
        CollectorConfig old = new CollectorConfig(file);
        check("★ 升級上來（沒有同意欄位）：畫面要跳", old.needsFirstRun() && old.existedBefore());
        check("★ 升級上來：原本的翻譯設定一個都沒變",
                old.tooltipMode() == CollectorConfig.TooltipMode.REPLACE
                        && old.itemNames() == CollectorConfig.ItemNames.ON
                        && old.chatMode() == CollectorConfig.ChatMode.OFF);
        old.setConsent(CollectorConfig.Consent.DECLINED);
        check("只回答了同意、還沒選設定（按 Esc 關掉）：下次還要跳",
                new CollectorConfig(file).needsFirstRun());
        old.markSetupDone();
        CollectorConfig again = new CollectorConfig(file);
        check("★ 回答過、選過「保留目前的設定」：之後不再跳",
                !again.needsFirstRun() && again.consent() == CollectorConfig.Consent.DECLINED);
        check("★ 保留目前的設定：設定照舊",
                again.tooltipMode() == CollectorConfig.TooltipMode.REPLACE
                        && again.itemNames() == CollectorConfig.ItemNames.ON
                        && again.chatMode() == CollectorConfig.ChatMode.OFF);
    }

    private static final class Recorder implements NetworkGate.Services {
        final List<String> calls = new ArrayList<>();

        @Override
        public void updateCheck() {
            calls.add(NetworkGate.UPDATE_CHECK);
        }

        @Override
        public void translationSync() {
            calls.add(NetworkGate.TRANSLATION_SYNC);
        }

        @Override
        public void deleteData() {
            calls.add(NetworkGate.DELETE_DATA);
        }
    }

    private static void consentGatesServices() throws Exception {
        NetworkGate.reset();
        Recorder unknown = new Recorder();
        NetworkGate.start(withConsent(CollectorConfig.Consent.UNKNOWN), unknown);
        check("★ 還沒回答同意：更新檢查、譯文同步都不開始", unknown.calls.isEmpty());

        NetworkGate.reset();
        Recorder declined = new Recorder();
        NetworkGate.start(withConsent(CollectorConfig.Consent.DECLINED), declined);
        check("★ 不同意：更新檢查、譯文同步都不開始", declined.calls.isEmpty());
        check("不同意：報到、披風、葉子也不連",
                !NetworkGate.registry(withConsent(CollectorConfig.Consent.DECLINED)));

        NetworkGate.reset();
        Recorder agreed = new Recorder();
        CollectorConfig yes = withConsent(CollectorConfig.Consent.AGREED);
        NetworkGate.start(yes, agreed);
        check("★ 同意：更新檢查與譯文同步都開始（實際 " + agreed.calls + "）",
                agreed.calls.equals(List.of(NetworkGate.UPDATE_CHECK, NetworkGate.TRANSLATION_SYNC)));
        check("同意：報到、披風、葉子可以連", NetworkGate.online(yes) && NetworkGate.registry(yes));
        Recorder twice = new Recorder();
        NetworkGate.start(yes, twice);
        check("同一次開遊戲只開始一次（在畫面上按同意之後不會再重複）", twice.calls.isEmpty());

        NetworkGate.reset();
        Recorder later = new Recorder();
        CollectorConfig changing = withConsent(CollectorConfig.Consent.UNKNOWN);
        NetworkGate.start(changing, later);
        changing.setConsent(CollectorConfig.Consent.AGREED);
        NetworkGate.start(changing, later);
        check("★ 啟動時還沒回答、之後在畫面上按同意：當下開始",
                later.calls.equals(List.of(NetworkGate.UPDATE_CHECK, NetworkGate.TRANSLATION_SYNC)));

        NetworkGate.reset();
        Recorder local = new Recorder();
        CollectorConfig offline = withConsent(CollectorConfig.Consent.AGREED);
        offline.toggleSource();
        NetworkGate.start(offline, local);
        check("同意但譯文來源是本機：只檢查更新", local.calls.equals(List.of(NetworkGate.UPDATE_CHECK)));
        NetworkGate.reset();
    }

    private static void servicesRefuseWithoutConsent() throws Exception {
        CollectorConfig before = null;
        NetworkGate.use(withConsent(CollectorConfig.Consent.UNKNOWN));
        try {
            Path cache = Files.createTempDirectory("wcy-gate").resolve("translations/zh_tw");
            long t0 = System.nanoTime();
            check("★ 沒同意：RemoteSync 不問版本", RemoteSync.remoteVersion() == null);
            check("★ 沒同意：RemoteSync 不抓譯文（連資料夾都不建）",
                    RemoteSync.fetchInto(cache, "zh_tw") == 0 && !Files.exists(cache));
            check("★ 沒同意：RemoteSync 不抓名單",
                    !RemoteSync.fetchCredits(cache.getParent()));
            check("沒同意：當場就回來，沒有去連線（" + (System.nanoTime() - t0) / 1_000_000 + " ms）",
                    System.nanoTime() - t0 < 2_000_000_000L);
            check("★ 沒同意：不能遊戲內更新", !SelfUpdate.available());
            check("★ 沒同意：不下載別人的披風",
                    Presence.capeBytes("0123456789abcdef01234567", 3) == null);
            Presence.LinkResult[] link = {null};
            Presence.link("ABCDEFGH", r -> link[0] = r);
            check("★ 沒同意：/wcy link 不送", link[0] == Presence.LinkResult.OFF);
            Presence.Result[] removed = {null};
            Presence.remove(r -> removed[0] = r);
            check("★ 沒同意：移除披風不送", removed[0] == Presence.Result.PRIVACY);
            check("★ 沒同意：不畫（也不下載）任何披風",
                    com.wynnchayuan.cape.Capes.textureFor("Someone", UUID.randomUUID(), false,
                            withConsent(CollectorConfig.Consent.DECLINED), 0) == null);
            CollectorConfig optedOut = withConsent(CollectorConfig.Consent.AGREED);
            optedOut.setDataOptOut(true);
            check("★ 選了不上傳資料：也不畫（不下載）披風",
                    com.wynnchayuan.cape.Capes.textureFor("Someone", UUID.randomUUID(), false,
                            optedOut, 0) == null);
            NetworkGate.use(optedOut);
            Presence.LinkResult[] link2 = {null};
            Presence.link("ABCDEFGH", r -> link2[0] = r);
            check("★ 選了不上傳資料：/wcy link 也不送", link2[0] == Presence.LinkResult.OFF);
        } finally {
            NetworkGate.use(before);
        }
    }

    private static void optOut() throws Exception {
        Path file = Files.createTempDirectory("wcy-optout").resolve("config.json");
        CollectorConfig c = new CollectorConfig(file);
        c.setConsent(CollectorConfig.Consent.AGREED);
        c.setDataOptOut(true);
        CollectorConfig reopened = new CollectorConfig(file);
        check("打開「不上傳我的資料」：記住開關，刪除請求等著送",
                reopened.dataOptOut() && reopened.dataDeletePending());
        check("打開之後報到、披風、葉子都不連，其他連線照常",
                !NetworkGate.registry(reopened) && NetworkGate.online(reopened));

        NetworkGate.reset();
        Recorder retry = new Recorder();
        NetworkGate.start(reopened, retry);
        check("★ 上次沒刪成功：下次啟動再送一次刪除",
                retry.calls.contains(NetworkGate.DELETE_DATA));
        Recorder again = new Recorder();
        NetworkGate.start(reopened, again);
        check("同一次開遊戲只送一次", again.calls.isEmpty());

        reopened.dataDeleted();
        NetworkGate.reset();
        Recorder done = new Recorder();
        NetworkGate.start(new CollectorConfig(file), done);
        check("★ 刪除成功之後不再送，開關維持開著",
                !done.calls.contains(NetworkGate.DELETE_DATA) && new CollectorConfig(file).dataOptOut());

        reopened.setDataOptOut(false);
        CollectorConfig off = new CollectorConfig(file);
        check("關掉開關：不再有待送的刪除，報到恢復",
                !off.dataOptOut() && !off.dataDeletePending() && NetworkGate.registry(off));

        Files.writeString(file, "{\"privacyConsent\": \"AGREED\", \"setupDone\": true,"
                + " \"dataDeletePending\": true}");
        check("手改的設定檔：沒開開關的待刪除不算", !new CollectorConfig(file).dataDeletePending());

        Path declinedFile = Files.createTempDirectory("wcy-optout2").resolve("config.json");
        CollectorConfig declined = new CollectorConfig(declinedFile);
        declined.setConsent(CollectorConfig.Consent.DECLINED);
        declined.setDataOptOut(true);
        NetworkGate.reset();
        Recorder privacy = new Recorder();
        NetworkGate.start(declined, privacy);
        check("不同意連線、但要求刪除資料：只送刪除那一個請求",
                privacy.calls.equals(List.of(NetworkGate.DELETE_DATA)));
        NetworkGate.reset();
    }

    private static void deleteAnswers() {
        check("刪除：200 而且 ok:true 才算成功",
                Presence.deleteResultOf(200, "{\"ok\":true}") == Presence.DeleteResult.DONE);
        check("刪除：200 但 ok:false 不算",
                Presence.deleteResultOf(200, "{\"ok\":false}") == Presence.DeleteResult.FAILED);
        check("刪除：200 但不是 JSON 不算",
                Presence.deleteResultOf(200, "<html>") == Presence.DeleteResult.FAILED);
        check("刪除：ok 是字串不算",
                Presence.deleteResultOf(200, "{\"ok\":\"true\"}") == Presence.DeleteResult.FAILED);
        check("刪除：404（登記站還沒有這個端點）",
                Presence.deleteResultOf(404, "") == Presence.DeleteResult.NOT_FOUND);
        check("刪除：401 是登入問題",
                Presence.deleteResultOf(401, "{}") == Presence.DeleteResult.LOGIN_FAILED);
        check("刪除：500 失敗", Presence.deleteResultOf(500, "") == Presence.DeleteResult.FAILED);
    }

    private static void selfCapeCache() throws Exception {
        Path cache = Files.createTempDirectory("wcy-capecache");
        String mine = "0123456789abcdef01234567";
        Files.writeString(cache.resolve("up-" + mine + "-3"), "x");
        Files.writeString(cache.resolve("up-" + mine + "-4"), "x");
        Files.writeString(cache.resolve("up-fedcba9876543210fedcba98-3"), "x");
        Files.writeString(cache.resolve("dev.png"), "x");
        int removed = com.wynnchayuan.cape.Capes.deleteUploaded(cache, mine);
        check("自己上傳的披風快取刪掉（" + removed + " 個），別人的與名單上的留著",
                removed == 2 && !Files.exists(cache.resolve("up-" + mine + "-3"))
                        && Files.exists(cache.resolve("up-fedcba9876543210fedcba98-3"))
                        && Files.exists(cache.resolve("dev.png")));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
