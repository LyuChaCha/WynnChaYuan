package com.wynnchayuan;

import com.wynnchayuan.PresenceAccount;
import com.wynnchayuan.translate.RemoteSync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class FirstRunTest {

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

    private static ModConfig fresh() throws Exception {
        return new ModConfig(Files.createTempDirectory("wcy-first").resolve("config.json"));
    }

    private static ModConfig withConsent(ModConfig.Consent consent) throws Exception {
        ModConfig c = fresh();
        c.setConsent(consent);
        return c;
    }

    private static void freshInstall() throws Exception {
        ModConfig c = fresh();
        check("新安裝：物品翻譯預設關閉", c.tooltipMode() == ModConfig.TooltipMode.OFF);
        check("新安裝：物品名稱維持原文", c.itemNames() == ModConfig.ItemNames.OFF);
        check("新安裝：還沒回答同意、還沒選設定，首次啟動畫面要跳",
                c.consent() == ModConfig.Consent.UNKNOWN && !c.setupDone()
                        && c.needsFirstRun() && !c.existedBefore());
        check("新安裝：連線一律關著", !NetworkGate.online(c) && !NetworkGate.registry(c));
    }

    private static String display(ModConfig c) {
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
        ModConfig c = new ModConfig(file);
        c.applyRecommended();
        check("推薦設定：對話與選項就地取代",
                c.dialogueMode() == ModConfig.DialogueMode.REPLACE
                        && c.choiceMode() == ModConfig.DialogueMode.REPLACE);
        check("推薦設定：聊天原文加譯文", c.chatMode() == ModConfig.ChatMode.BOTH);
        check("推薦設定：名牌就地取代", c.nametagMode() == ModConfig.NametagMode.REPLACE);
        check("推薦設定：物品翻譯關閉、物品名稱原文",
                c.tooltipMode() == ModConfig.TooltipMode.OFF
                        && c.itemNames() == ModConfig.ItemNames.OFF);
        check("推薦設定：追蹤欄就地取代、記分板與目標翻譯",
                c.trackerMode() == ModConfig.DialogueMode.REPLACE
                        && c.translateScoreboard() && c.translateObjectives());
        check("推薦設定：大字、Boss 血條、Wynntils 介面、總開關都開",
                c.translateTitles() && c.translateBossBar() && c.wynntilsUi()
                        && c.translationsEnabled() && c.showOverlays());
        check("推薦設定不碰版面設定", c.panelGap() == 40);
        check("推薦設定就是新安裝的預設值", display(c).equals(display(fresh())));
        check("推薦設定寫進檔案", display(new ModConfig(file)).equals(display(c)));

        ModConfig custom = fresh();
        custom.setDialogueModes(ModConfig.DialogueMode.PANEL);
        custom.setChatMode(ModConfig.ChatMode.OFF);
        custom.setNametagMode(ModConfig.NametagMode.LOOK_AT);
        custom.setTooltipMode(ModConfig.TooltipMode.REPLACE);
        custom.setItemNames(ModConfig.ItemNames.BOTH);
        custom.setTrackerMode(ModConfig.DialogueMode.OFF);
        custom.setTranslateScoreboard(false);
        custom.setTranslateTitles(false);
        custom.setTranslateBossBar(false);
        custom.setWynntilsUi(false);
        check("自訂：每一項都照選的",
                custom.dialogueMode() == ModConfig.DialogueMode.PANEL
                        && custom.choiceMode() == ModConfig.DialogueMode.PANEL
                        && custom.chatMode() == ModConfig.ChatMode.OFF
                        && custom.nametagMode() == ModConfig.NametagMode.LOOK_AT
                        && custom.tooltipMode() == ModConfig.TooltipMode.REPLACE
                        && custom.itemNames() == ModConfig.ItemNames.BOTH
                        && custom.trackerMode() == ModConfig.DialogueMode.OFF
                        && !custom.translateScoreboard() && !custom.translateTitles()
                        && !custom.translateBossBar() && !custom.wynntilsUi());
    }

    private static void upgradeShowsOnce() throws Exception {
        Path dir = Files.createTempDirectory("wcy-upgrade");
        Path file = dir.resolve("config.json");
        Files.writeString(file, """
                {"tooltipMode": "REPLACE", "itemNames": "ON", "chatMode": "OFF",
                 "notifiedVersion": "1.0.4", "oldKeysReleased": true}""");
        ModConfig old = new ModConfig(file);
        check("★ 升級上來（沒有同意欄位）：畫面要跳", old.needsFirstRun() && old.existedBefore());
        check("★ 升級上來：原本的翻譯設定一個都沒變",
                old.tooltipMode() == ModConfig.TooltipMode.REPLACE
                        && old.itemNames() == ModConfig.ItemNames.ON
                        && old.chatMode() == ModConfig.ChatMode.OFF);
        old.setConsent(ModConfig.Consent.DECLINED);
        check("只回答了同意、還沒選設定（按 Esc 關掉）：下次還要跳",
                new ModConfig(file).needsFirstRun());
        old.markSetupDone();
        ModConfig again = new ModConfig(file);
        check("★ 回答過、選過「保留目前的設定」：之後不再跳",
                !again.needsFirstRun() && again.consent() == ModConfig.Consent.DECLINED);
        check("★ 保留目前的設定：設定照舊",
                again.tooltipMode() == ModConfig.TooltipMode.REPLACE
                        && again.itemNames() == ModConfig.ItemNames.ON
                        && again.chatMode() == ModConfig.ChatMode.OFF);
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
        NetworkGate.start(withConsent(ModConfig.Consent.UNKNOWN), unknown);
        check("★ 還沒回答同意：更新檢查、譯文同步都不開始", unknown.calls.isEmpty());

        NetworkGate.reset();
        Recorder declined = new Recorder();
        NetworkGate.start(withConsent(ModConfig.Consent.DECLINED), declined);
        check("★ 不同意：更新檢查、譯文同步都不開始", declined.calls.isEmpty());
        check("不同意：報到、披風、葉子也不連",
                !NetworkGate.registry(withConsent(ModConfig.Consent.DECLINED)));

        NetworkGate.reset();
        Recorder agreed = new Recorder();
        ModConfig yes = withConsent(ModConfig.Consent.AGREED);
        NetworkGate.start(yes, agreed);
        check("★ 同意：更新檢查與譯文同步都開始（實際 " + agreed.calls + "）",
                agreed.calls.equals(List.of(NetworkGate.UPDATE_CHECK, NetworkGate.TRANSLATION_SYNC)));
        check("同意：報到、披風、葉子可以連", NetworkGate.online(yes) && NetworkGate.registry(yes));
        Recorder twice = new Recorder();
        NetworkGate.start(yes, twice);
        check("同一次開遊戲只開始一次（在畫面上按同意之後不會再重複）", twice.calls.isEmpty());

        NetworkGate.reset();
        Recorder later = new Recorder();
        ModConfig changing = withConsent(ModConfig.Consent.UNKNOWN);
        NetworkGate.start(changing, later);
        changing.setConsent(ModConfig.Consent.AGREED);
        NetworkGate.start(changing, later);
        check("★ 啟動時還沒回答、之後在畫面上按同意：當下開始",
                later.calls.equals(List.of(NetworkGate.UPDATE_CHECK, NetworkGate.TRANSLATION_SYNC)));

        NetworkGate.reset();
        Recorder local = new Recorder();
        ModConfig offline = withConsent(ModConfig.Consent.AGREED);
        offline.toggleSource();
        NetworkGate.start(offline, local);
        check("同意但譯文來源是本機：只檢查更新", local.calls.equals(List.of(NetworkGate.UPDATE_CHECK)));
        NetworkGate.reset();
    }

    private static void servicesRefuseWithoutConsent() throws Exception {
        ModConfig before = null;
        NetworkGate.use(withConsent(ModConfig.Consent.UNKNOWN));
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
                    PresenceAccount.capeBytes("0123456789abcdef01234567", 3) == null);
            PresenceAccount.LinkResult[] link = {null};
            PresenceAccount.link("ABCDEFGH", r -> link[0] = r);
            check("★ 沒同意：/wcy link 不送", link[0] == PresenceAccount.LinkResult.OFF);
            PresenceAccount.Result[] removed = {null};
            PresenceAccount.remove(r -> removed[0] = r);
            check("★ 沒同意：移除披風不送", removed[0] == PresenceAccount.Result.PRIVACY);
            check("★ 沒同意：不畫（也不下載）任何披風",
                    com.wynnchayuan.cape.Capes.textureFor("Someone", UUID.randomUUID(), false,
                            withConsent(ModConfig.Consent.DECLINED), 0) == null);
            ModConfig optedOut = withConsent(ModConfig.Consent.AGREED);
            optedOut.setDataOptOut(true);
            check("★ 選了不上傳資料：也不畫（不下載）披風",
                    com.wynnchayuan.cape.Capes.textureFor("Someone", UUID.randomUUID(), false,
                            optedOut, 0) == null);
            NetworkGate.use(optedOut);
            PresenceAccount.LinkResult[] link2 = {null};
            PresenceAccount.link("ABCDEFGH", r -> link2[0] = r);
            check("★ 選了不上傳資料：/wcy link 也不送", link2[0] == PresenceAccount.LinkResult.OFF);
        } finally {
            NetworkGate.use(before);
        }
    }

    private static void optOut() throws Exception {
        Path file = Files.createTempDirectory("wcy-optout").resolve("config.json");
        ModConfig c = new ModConfig(file);
        c.setConsent(ModConfig.Consent.AGREED);
        c.setDataOptOut(true);
        ModConfig reopened = new ModConfig(file);
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
        NetworkGate.start(new ModConfig(file), done);
        check("★ 刪除成功之後不再送，開關維持開著",
                !done.calls.contains(NetworkGate.DELETE_DATA) && new ModConfig(file).dataOptOut());

        reopened.setDataOptOut(false);
        ModConfig off = new ModConfig(file);
        check("關掉開關：不再有待送的刪除，報到恢復",
                !off.dataOptOut() && !off.dataDeletePending() && NetworkGate.registry(off));

        Files.writeString(file, "{\"privacyConsent\": \"AGREED\", \"setupDone\": true,"
                + " \"dataDeletePending\": true}");
        check("手改的設定檔：沒開開關的待刪除不算", !new ModConfig(file).dataDeletePending());

        Path declinedFile = Files.createTempDirectory("wcy-optout2").resolve("config.json");
        ModConfig declined = new ModConfig(declinedFile);
        declined.setConsent(ModConfig.Consent.DECLINED);
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
                PresenceAccount.deleteResultOf(200, "{\"ok\":true}") == PresenceAccount.DeleteResult.DONE);
        check("刪除：200 但 ok:false 不算",
                PresenceAccount.deleteResultOf(200, "{\"ok\":false}") == PresenceAccount.DeleteResult.FAILED);
        check("刪除：200 但不是 JSON 不算",
                PresenceAccount.deleteResultOf(200, "<html>") == PresenceAccount.DeleteResult.FAILED);
        check("刪除：ok 是字串不算",
                PresenceAccount.deleteResultOf(200, "{\"ok\":\"true\"}") == PresenceAccount.DeleteResult.FAILED);
        check("刪除：404（登記站還沒有這個端點）",
                PresenceAccount.deleteResultOf(404, "") == PresenceAccount.DeleteResult.NOT_FOUND);
        check("刪除：401 是登入問題",
                PresenceAccount.deleteResultOf(401, "{}") == PresenceAccount.DeleteResult.LOGIN_FAILED);
        check("刪除：500 失敗", PresenceAccount.deleteResultOf(500, "") == PresenceAccount.DeleteResult.FAILED);
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

}
