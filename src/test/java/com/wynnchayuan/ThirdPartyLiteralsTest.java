package com.wynnchayuan;

import com.wynnchayuan.render.ThirdPartyLiterals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ThirdPartyLiteralsTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-literals");
        ThirdPartyLiterals.load(dir);
        check("沒裝那個模組時清單是空的（實際 "
                      + ThirdPartyLiterals.activeLiterals() + "）",
              ThirdPartyLiterals.activeLiterals().isEmpty());
        check("清單空的時候什麼都不擋",
              !ThirdPartyLiterals.reserved("Raid Completed!"));

        ThirdPartyLiterals.forTest(List.of("Raid Completed", "Raid Failed"));
        check("擋得下成功", ThirdPartyLiterals.reserved("Raid Completed!"));
        check("擋得下失敗", ThirdPartyLiterals.reserved("Raid Failed!"));

        check("前後有別的東西也擋得下來",
              ThirdPartyLiterals.reserved("󰀡 Raid Completed! 󰁅"));

        check("沒在清單上的不擋", !ThirdPartyLiterals.reserved("Raid Experience"));
        check("空字串不擋", !ThirdPartyLiterals.reserved(""));
        check("null 不擋", !ThirdPartyLiterals.reserved(null));

        // headless 沒有 Fabric 模組容器，isModLoaded 一律 false
        String shipped;
        try (java.io.InputStream in = ThirdPartyLiterals.class.getResourceAsStream(
                "/assets/wynnchayuan/third-party-literals.json")) {
            check("出貨的清單讀得到", in != null);
            shipped = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        com.google.gson.JsonObject root =
                com.google.gson.JsonParser.parseString(shipped).getAsJsonObject();
        check("清單按模組 ID 分組（有 wynnmod 這一組）", root.has("wynnmod"));
        String wynnmod = root.has("wynnmod") ? root.get("wynnmod").toString() : "";
        check("WynnMod 判斷成功的那句在清單上", wynnmod.contains("Raid Completed"));
        check("判斷失敗的那句也在（少一邊連勝就只會往下掉）",
              wynnmod.contains("Raid Failed"));

        for (String raid : new String[] {
                "Nest of The Grootslangs", "Orphion's Nexus of Light",
                "The Canyon Colossus", "The Nameless Anomaly", "The Wartorn Palace"}) {
            check("副本名稱在清單上：" + raid, wynnmod.contains(raid));
        }

        check("開場邀請那句在清單上", wynnmod.contains("would like to start"));
        check("增益選擇那句在清單上", wynnmod.contains("has chosen the"));

        String prompt = "Type the item name or type 'cancel' to cancel:";
        check("清單有 wms 這一組", root.has("wms"));
        check("市集搜尋的那句提示在 wms 那一組",
              root.has("wms") && root.get("wms").toString().contains(prompt));
        ThirdPartyLiterals.forTest(List.of(prompt));
        check("遊戲送來的那一則（前後帶圖示、一共三行）擋得下來",
              ThirdPartyLiterals.reserved("󏿼 \n Type the item name or type 'cancel' to cancel:\n󏿼"));
        check("市集的其他訊息不受影響",
              !ThirdPartyLiterals.reserved(" You moved and your chat input was canceled."));
        com.google.gson.JsonObject ui = com.google.gson.JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/assets/wynnchayuan/translations/zh_tw/quest-ui.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        boolean inCorpus = false;
        for (String key : ui.keySet()) {
            inCorpus |= key.contains(prompt) && !ui.get(key).getAsString().isBlank();
        }
        check("那句提示就是語料裡翻過的那一條", inCorpus);

        stats();
        chat();
        readsChatFirst(root);
        report();
    }

    private static void stats() {
        ThirdPartyLiterals.load(java.nio.file.Path.of("does-not-exist"));
        java.util.List<java.util.List<String>> shipped = ThirdPartyLiterals.activeLiterals();
        check("沒裝 WynnMod 時統計那幾條也不生效（實際 " + shipped.size() + " 組）",
              shipped.isEmpty());

        ThirdPartyLiterals.forTestGroups(java.util.List.of(
                java.util.List.of("Time Elapsed:"),
                java.util.List.of("Hover for more"),
                java.util.List.of("Raid Experience", "%)"),
                java.util.List.of("Reward Pulls", "%)"),
                java.util.List.of("Aspect Pulls", "%)")));

        check("擋得下結算的時間", ThirdPartyLiterals.reserved("Time Elapsed: 04:33"));
        check("擋得下結算的戰鬥經驗",
              ThirdPartyLiterals.reserved("Hover for more  100k Combat Experience"));
        check("擋得下結算的獎勵抽數",
              ThirdPartyLiterals.reserved("DMG 2.9M/9.7M (30.16%)  39 Reward Pulls"));
        check("擋得下結算的意象抽數",
              ThirdPartyLiterals.reserved("DEF 25.4K/81K (31.33%)  12 Aspect Pulls"));
        check("擋得下結算的討伐戰經驗",
              ThirdPartyLiterals.reserved("HEAL 144K/1M (13.13%)  210 Raid Experience"));

        check("信標面板不受影響（次數）",
              !ThirdPartyLiterals.reserved("Reward Pulls  next Beacon Effects"));
        check("信標面板不受影響（效力）",
              !ThirdPartyLiterals.reserved("100% Potency  Reward Pulls"));
        check("信標面板不受影響（挑戰場數）",
              !ThirdPartyLiterals.reserved("Reward Pulls  for 10 Challenges"));

        check("大廳的每日獎勵不受影響",
              !ThirdPartyLiterals.reserved("- Aspect Pulls: 3"));
    }

    private static void readsChatFirst(com.google.gson.JsonObject root) {
        String probe = "com.wynnmod.mixin.events.S2CPacketEvents$ChatMsg";
        check("★ 出貨的清單有帶條件的那一組，條件是 WynnMod 封包層的聊天事件",
              root.has("wynnmod@unless:" + probe));
        String[] rows = {
            "DMG 64.9M/293M (22.13%)  55 Reward Pulls",
            "DEF 69.4K/323K (21.49%)  16 Aspect Pulls",
            "HEAL 93.5K/44.8M (0.21%)  400 Raid Experience",
            "Hover for more  10m Combat Experience",
            "87 Reward Pulls  Time Elapsed: 19:01",
        };
        String[] titles = {"Raid Completed!", "Raid Failed!", "Lootrun Completed!",
            "Orphion's Nexus of Light", "Prepare to Lootrun"};

        ThirdPartyLiterals.forTestGroups(ThirdPartyLiterals.groups(root,
                id -> id.equals("wynnmod"), probe::equals));
        for (String row : rows) {
            check("★ 新版 WynnMod：結算這一列不留英文（" + row + "）",
                  !ThirdPartyLiterals.reserved(row));
        }
        for (String title : titles) {
            check("★ 新版 WynnMod：標題照舊留英文（" + title + "）",
                  ThirdPartyLiterals.reserved(title));
        }
        check("開場邀請照舊留英文", ThirdPartyLiterals.reserved(
                "Watari would like to start Nest of the Grootslangs!"));

        ThirdPartyLiterals.forTestGroups(ThirdPartyLiterals.groups(root,
                id -> id.equals("wynnmod"), name -> false));
        for (String row : rows) {
            check("舊版 WynnMod：結算這一列照舊留英文（" + row + "）",
                  ThirdPartyLiterals.reserved(row));
        }
        check("舊版 WynnMod：標題也留", ThirdPartyLiterals.reserved("Raid Completed!"));
        check("舊版 WynnMod：信標面板不受影響",
              !ThirdPartyLiterals.reserved("Reward Pulls  next Beacon Effects"));

        check("沒裝 WynnMod：什麼都不留", ThirdPartyLiterals.groups(root,
                id -> false, name -> false).isEmpty());
        check("只裝了別的模組（wms）：WynnMod 那兩組都不生效",
              ThirdPartyLiterals.groups(root, id -> id.equals("wms"), name -> true).size() == 1);
        check("清單是空的也不出事", ThirdPartyLiterals.groups(null, id -> true, name -> true).isEmpty());
        ThirdPartyLiterals.forTestGroups(java.util.List.of());
    }

    private static void chat() {
        ThirdPartyLiterals.forTestGroups(java.util.List.of(
                java.util.List.of("would like to start"),
                java.util.List.of("has chosen the", "buff!")));

        check("擋得下開場邀請", ThirdPartyLiterals.reserved(
                "Watari would like to start Nest of the Grootslangs!"));
        check("擋得下增益選擇", ThirdPartyLiterals.reserved(
                "Watari has chosen the Elder III buff!"));

        check("任務對話不受影響（沒有 buff!）",
              !ThirdPartyLiterals.reserved(
                      "The Council has chosen the path of silence."));
        check("增益面板本身不受影響",
              !ThirdPartyLiterals.reserved("Choose a buff or go to the exit"));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("第三方字串：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("第三方字串：全部通過");
    }
}
