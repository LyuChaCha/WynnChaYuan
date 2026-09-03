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
        // ★ 翻譯團隊跑一場討伐戰回傳的語料裡漏掉的那幾類。
        //
        // 「Watari has chosen the Elder III buff!」一場就收進 80 條——前面那個是
        // 隊友的 ID。隊伍搜尋則是每一支隊伍兩條（名稱與隊長）。
        check("擋得下增益選擇（帶隊友 ID）",
              com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(
                      "{#} Watari has chosen the Elder III buff!"));
        check("擋得下隊伍名",
              com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(
                      "gobert_rl's Party"));
        check("擋得下隊長",
              com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(
                      "Leader: TheTankZone__"));

        // 討伐戰的死亡與治療訊息。句子是遊戲的模板，主詞永遠是隊友的 ID。
        for (String death : new String[] {
                "{#} Watari was purified by Orphion.",
                "{#} King lost their color to The Parasite.",
                "{#} Watari was drained of their life by The Parasite.",
                "{#} KFA was crushed between the Wyrmling's jaws.",
                "{#} Meow Meow's existence was redacted by The Nameless.",
                "Watari had their existence effaced by The Nameless.",
                "Watari passed away",
                "King gave you [+{~} ❤]",
                "IHateRaid has given you {~} resistance and {~} strength.",
                "{#} Yorikatsu{~} has been overtaken! Keep attacking The"
                        + " {#} Parasite to save them!",
                "{#} userUwU has gotten a Steampunk Spear from their crate!",
                "{#}Crafted by Bunnub",
                "{#}Crafted by creeper{~}",
                "{#} HiSlIgHt_ has reconnected!",
                "{#} Party Finder: Hey MorphCascade, over here! Join the queue"}) {
            check("擋得下討伐戰死亡／治療訊息：" + death.replace("\n", "⏎"),
                  com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(death));
        }

        // 介面裡整格就是一個帳號名的情況（隊伍、好友、公會清單的玩家頭顱）。
        // 片語比對抓不到這些，實機的 captured.json 就混進了六個。
        for (String who : new String[] {
                "{#}{#}PoorChaCha", "{#}RichaCha", "{#}ChangJenChief",
                "{#}PeeYan Hunter", "YuChaYuan [YCY]", "- [{~}] YuanYoIn",
                "heal_kitty"}) {
            check("擋得下介面裡的帳號名：" + who,
                  com.wynnchayuan.capture.PlayerDataFilter.looksAccountNamed(who));
        }

        // 公會清單的一列。公會名是玩家取的，什麼樣子都有——靠名字的形狀
        // （底線、駝峰）幾乎全部擋不住，實機開一次公會選單就漏了七個。
        // 認得出來的是<b>那一列的形狀</b>：結尾是方括號包起來的二到四碼標籤。
        for (String guild : new String[] {
                "- RabbitHouse [Maya]", "- Blank [BLK]", "- TimeCity [tmxt]",
                "- Uchouten Tea House [THTS]", "- Death Star [LIVE]",
                "- Chemdah [CHEM]", "NexusRolly Love [ikun]"}) {
            check("擋得下公會清單：" + guild,
                  com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(guild));
        }

        // ★ 反方向：長得有點像、但不是公會清單的那些。
        //
        // 這條規則只認「整行就是一個名字加標籤」。方括號裡有空白、斜線或
        // 百分比的都不算，遊戲自己的介面字才不會被掃到。
        for (String ui : new String[] {
                "[+2800 ❤] Potions of Healing [9/30]",
                "Willow [67.5%]", "Emerald Pouch [Tier 8]",
                "- Slay Mobs: 12/50", "Rewards [Weekly Objective]"}) {
            check("不誤擋介面文字（公會那條）：" + ui,
                  !com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(ui));
        }

        // ★ 反方向：介面文字不能被這條規則掃到。判準比名牌那支嚴，
        //   因為介面滿是被折出來的小寫續行，寬判準量過去會誤擋 62 條。
        for (String ui : new String[] {
                "to confirm", "offering better chances", "Reward Sacrifices",
                "Active Boosts", "Unlock Rewards", "Emerald Block",
                "You will be in permanent hunted mode (PvP on)",
                // 地城鑰匙中間那幾個 À 是對齊字元，不是駝峰。
                "UnderworldÀÀÀCrypt Key", "Broken DecrepitÀÀÀSewers Key"}) {
            check("不誤擋介面文字：" + ui,
                  !com.wynnchayuan.capture.PlayerDataFilter.looksAccountNamed(ui));
        }

        // ★ 反方向：同樣帶所有格的<b>物品名</b>一個都不能誤擋。
        //
        // 只測「擋得下來」的話，把所有 X's Y 擋死也會全過——而語料裡那種形狀的
        // 物品名多得是。
        for (String item : new String[] {
                "Famished's Gambit", "Harvester's Tome of Mysticism III",
                "Major's Badge", "Heroine's Blessing: Devout",
                "Orphion's Nexus of Light", "Bob's Tear"}) {
            check("不誤擋物品名：" + item,
                  !com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(item));
        }

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
