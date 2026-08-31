package com.wynnchayuan;

import com.wynnchayuan.capture.PlayerDataFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家自己取的名字不能進共享語料。
 *
 * <h2>先前壞在哪</h2>
 * 過濾器只擋得住<b>自己</b>的帳號名（{@code {u}} 佔位符也只認自己），別人的一律
 * 穿過去。而且名牌有兩條收集路徑，其中 {@code npc/name} 那條<b>完全沒過濾</b>。
 *
 * <p>一份貢獻者回傳的語料裡因此混進了：隊友技能生成的召喚物
 * （{@code Critar's Totem}、{@code Noxy_OwO's Rubber Duck}）、玩家自己命名的
 * 寵物與飾品（{@code epoch}、{@code oily femboy choker}），以及<b>整批其他玩家
 * 的角色名牌</b>（{@code heal kitty [Lv 106]} 這種）。
 *
 * <h2>這裡釘住什麼</h2>
 * 這條規則真正的風險是<b>誤擋</b>——擋太兇會把正當的怪物名字一起濾掉，而且
 * 不會有任何錯誤訊息，只會安靜地少收東西。所以兩個方向都測，而且誤擋那一邊
 * 是拿<b>真實語料</b>當對照：現有的 npc.json 是人工翻過的，裡面的名字全部都
 * 應該通得過。
 */
public final class PlayerNameplateTest {

    private static int failures = 0;

    /** 真實回報裡漏進來的，全部都要擋下來。 */
    private static final String[] LEAKED = {
        "nunot's Totem",
        "Noxy_OwO's Rubber Duck",
        "epoch",
        "oily femboy choker",
        "HellRevenger",
        "NexusRolly Love",
        "heal kitty {#}[Lv {~}]",
        "coca cola {#}[{~}/{~}]",
    };

    /** 正當的遊戲內容，一個都不能擋。 */
    private static final String[] KEEP = {
        "Grootslang Wyrmling",
        "Voracious Octiped",
        "Shelled Grub",
        "Mysterious Merchant",
        "Corrupted Goliath",
        "Bob the Zombie",
        "Zombie Pigman",
        "Orphion's Grace",          // 設定裡的所有格，不是玩家的
        "Grook's Nest Rewards",
        "{~}k",                     // 血條的單位，剝掉佔位符只剩一個字母
        "{~}m {#}",
        "on the Trade Market!",     // 多行提示被折出來的續行
        "untradable, and quest items",
    };

    public static void main(String[] args) throws Exception {
        for (String leak : LEAKED) {
            check("擋得下：" + leak, PlayerDataFilter.looksPlayerNamed(leak));
        }
        for (String keep : KEEP) {
            check("不誤擋：" + keep, !PlayerDataFilter.looksPlayerNamed(keep));
        }

        // ★ 拿真實語料當對照。npc.json 是人工翻過的，裡面的名字都是遊戲內容。
        //
        // 只測上面那幾個手寫的例子是不夠的——真正的風險是某個沒想到的命名
        // 形狀被規則掃到，而那種東西只有整份語料掃過才看得見。
        Path npc = Path.of("src/main/resources/assets/wynnchayuan/translations",
                           "zh_tw/npc.json");
        if (!Files.isRegularFile(npc)) {
            System.out.println("  [略過] 找不到 npc.json，跳過真實語料對照");
            report();
            return;
        }
        com.google.gson.JsonObject root = com.google.gson.JsonParser
                .parseString(Files.readString(npc)).getAsJsonObject();
        List<String> blocked = new ArrayList<>();
        int total = 0;
        for (String key : root.keySet()) {
            if (key.startsWith("_")) {
                continue;
            }
            total++;
            if (PlayerDataFilter.looksPlayerNamed(key)) {
                blocked.add(key.split("\n", 2)[0]);
            }
        }
        System.out.println("      真實語料 " + total + " 條，會被擋 " + blocked.size() + " 條");
        for (String b : blocked) {
            System.out.println("        · " + b);
        }
        // 已知且可接受的誤擋。
        //
        // 這兩條是多行提示被折出來的續行，結構上跟「oily femboy choker」
        // 一模一樣（開頭小寫、兩三個字、不帶句讀），分不出來。
        //
        // 接受它們，是因為<b>擋掉它們沒有任何損失</b>：兩者的整段都另外收了
        // （「Blacksmith⏎Sell, scrap, and⏎repair items」），而按「逐行 vs 整段」
        // 的規矩，整段本來就才是該用的鍵——只翻其中一行會夾出半中半英。
        //
        // 清單寫死在這裡是刻意的：將來有人放寬規則、造成<b>新的</b>誤擋，
        // 這條就會紅，而那正是需要有人看一眼的時候。
        List<String> accepted = List.of("manage your island", "repair items");
        blocked.removeAll(accepted);
        check("人工翻過的語料沒有新的誤擋（實際 " + blocked + "）", blocked.isEmpty());

        report();
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("玩家名牌：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("玩家名牌：全部通過");
    }
}
