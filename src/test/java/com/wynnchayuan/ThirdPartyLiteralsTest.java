package com.wynnchayuan;

import com.wynnchayuan.render.ThirdPartyLiterals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 別的模組拿來判斷遊戲狀態的字串，我們不能翻。
 *
 * <h2>先前壞在哪</h2>
 * WynnMod 的 {@code RaidState.onTitle} 用 {@code title.contains("Raid Completed")}
 * 判斷討伐戰成功。我們把那一行換成「討伐戰完成！」之後它對不上，這一場算不出
 * 結果，上報之後連勝重置——玩家回報打完反而從 1 掉到 0。
 *
 * <p>而且當時只翻了 {@code Raid Completed!}、沒翻 {@code Raid Failed!}，
 * 所以失敗認得出來、成功認不出來，連勝<b>只會往下掉</b>。
 *
 * <h2>這裡釘住什麼</h2>
 * 兩件事：清單上的字要擋得下來（含前後有排版符號與其他文字的情況），
 * 以及<b>沒裝那個模組的人不受影響</b>——那才是「按模組分組」的意義。
 */
public final class ThirdPartyLiteralsTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        // 這台機器上沒有真的裝 WynnMod，load() 會把它整組跳過——
        // 那正是「沒裝的人照常看到中文」該有的行為，先釘住它。
        Path dir = Files.createTempDirectory("wynnchayuan-literals");
        ThirdPartyLiterals.load(dir);
        check("沒裝那個模組時清單是空的（實際 "
                      + ThirdPartyLiterals.activeLiterals() + "）",
              ThirdPartyLiterals.activeLiterals().isEmpty());
        check("清單空的時候什麼都不擋",
              !ThirdPartyLiterals.reserved("Raid Completed!"));

        // 裝了的情況：直接餵清單，測比對本身。
        ThirdPartyLiterals.forTest(List.of("Raid Completed", "Raid Failed"));
        check("擋得下成功", ThirdPartyLiterals.reserved("Raid Completed!"));
        check("擋得下失敗", ThirdPartyLiterals.reserved("Raid Failed!"));

        // 遊戲送過來的那一行前後有排版符號，完全相等永遠不會成立。
        check("前後有別的東西也擋得下來",
              ThirdPartyLiterals.reserved("󰀡 Raid Completed! 󰁅"));

        check("沒在清單上的不擋", !ThirdPartyLiterals.reserved("Raid Experience"));
        check("空字串不擋", !ThirdPartyLiterals.reserved(""));
        check("null 不擋", !ThirdPartyLiterals.reserved(null));

        // 出貨的清單裡真的有那兩句。
        //
        // 這一條不能靠 load() 驗：headless 的測試 JVM 沒有 Fabric 的模組容器，
        // isModLoaded 一律回 false，整組會被跳過——「裝了 WynnMod 會怎樣」
        // 在 CI 上重現不了。所以直接讀出貨的資源檔，釘住內容本身。
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

        // 副本名稱也要在。getRaidFromName(標題) 是用來認「這是哪一場」的，
        // 翻掉的話連 startRaid 都不會觸發——整場都不會被記錄。
        for (String raid : new String[] {
                "Nest of The Grootslangs", "Orphion's Nexus of Light",
                "The Canyon Colossus", "The Nameless Anomaly", "The Wartorn Palace"}) {
            check("副本名稱在清單上：" + raid, wynnmod.contains(raid));
        }

        stats();
        report();
    }

    /**
     * 討伐戰結算的統計數字，WynnMod 也在解——但關鍵詞跟獵殺信標撞名。
     *
     * <p>{@code Reward Pulls} 同時出現在結算與信標面板，兩邊都走聊天。只用
     * 「包含」去擋會把信標的譯文一起擋掉（那是 v1.99.89 才修好的）。分別它們
     * 的是結算那一行的百分比 {@code (30.16%)}。
     *
     * <p>這裡兩個方向都要測：該擋的擋得下來，<b>不該擋的不能誤傷</b>。
     * 只測前者的話，把整個信標面板擋掉也會全過。
     */
    private static void stats() {
        ThirdPartyLiterals.load(java.nio.file.Path.of("does-not-exist"));
        java.util.List<java.util.List<String>> shipped = ThirdPartyLiterals.activeLiterals();
        check("沒裝 WynnMod 時統計那幾條也不生效（實際 " + shipped.size() + " 組）",
              shipped.isEmpty());

        // 直接餵出貨清單裡那幾組，測比對本身。
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

        // ★ 反方向：獵殺信標的面板不能被誤傷。
        check("信標面板不受影響（次數）",
              !ThirdPartyLiterals.reserved("Reward Pulls  next Beacon Effects"));
        check("信標面板不受影響（效力）",
              !ThirdPartyLiterals.reserved("100% Potency  Reward Pulls"));
        check("信標面板不受影響（挑戰場數）",
              !ThirdPartyLiterals.reserved("Reward Pulls  for 10 Challenges"));

        // 討伐戰大廳的每日獎勵面板也不該被擋——那不是 WynnMod 在解的行。
        check("大廳的每日獎勵不受影響",
              !ThirdPartyLiterals.reserved("- Aspect Pulls: 3"));
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
