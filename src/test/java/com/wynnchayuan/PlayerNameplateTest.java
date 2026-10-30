package com.wynnchayuan;

import com.wynnchayuan.capture.PlayerDataFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PlayerNameplateTest {

    private static int failures = 0;

    private static final String[] LEAKED = {
        "nunot's Totem",
        "Noxy_OwO's Rubber Duck",
        "epoch",
        "oily femboy choker",
        "HellRevenger",
        "NexusRolly Love",
        "heal kitty {#}[Lv {~}]",
        "coca cola {#}[{~}/{~}]",
        "{#} PoorChaCha has placed a mob totem in {p} at {~}, {~},",
        "{#} You are inside of PoorChaCha's mob totem. Get your own",
    };

    private static final String[] KEEP = {
        "Grootslang Wyrmling",
        "Voracious Octiped",
        "Shelled Grub",
        "Mysterious Merchant",
        "Corrupted Goliath",
        "Bob the Zombie",
        "Zombie Pigman",
        "Orphion's Grace",
        "Grook's Nest Rewards",
        "{~}k",
        "{~}m {#}",
        "on the Trade Market!",
        "untradable, and quest items",
    };

    public static void main(String[] args) throws Exception {
        for (String leak : LEAKED) {
            check("擋得下：" + leak, PlayerDataFilter.looksPlayerNamed(leak));
        }
        for (String keep : KEEP) {
            check("不誤擋：" + keep, !PlayerDataFilter.looksPlayerNamed(keep));
        }

        partyCards();

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
        check("擋得下增益選擇（帶隊友 ID）",
              com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(
                      "{#} Watari has chosen the Elder III buff!"));
        check("擋得下隊伍名",
              com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(
                      "gobert_rl's Party"));
        check("擋得下隊長",
              com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(
                      "Leader: TheTankZone__"));

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
                "{#} Sleepy toaster has gotten a Gray Wybel from their\n{#} crate!\n"
                        + "{#} Get your own at wynncraft.com/store or type /store",
                "{#} Sleepy toaster Combat Experience Bomb has expired! \n"
                        + "{#} Get your own bombs at wynncraft.com/store",
                "Toaster Alleyway\nby Very Sleepy Toasters",
                "- Name Contains: Ripe Aureate Fruit",
                "Sleepy Toaster has Scarlet Megabat to thank for their death",
                "Toaster got a new skull piercing from T-{~} Overseer",
                "Toaster{~} was sniped by T-{~} Overseer",
                "{#} toaster{~} was de-animated by the powers of\n{#} Encroaching Tunneler",
                "{#}Crafted by Bunnub",
                "{#}Crafted by creeper{~}",
                "{#} HiSlIgHt_ has reconnected!",
                "Weeeeeeeeeezard was minced to bits by the Grootslang.",
                "{#}{#}Weeeeeeeeeezard began to glow and then faded into nothing.",
                "Linger{~}_ has been crystallized!",
                "YuChaYuan\n< Season {~} - Platinum >",
                "YuChaYuan\n< Radiant >",
                "{#} Party Finder: Hey MorphCascade, over here! Join the queue"}) {
            check("擋得下討伐戰死亡／治療訊息：" + death.replace("\n", "⏎"),
                  com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(death));
        }

        for (String who : new String[] {
                "{#}{#}PoorChaCha", "{#}RichaCha", "{#}ChangJenChief",
                "{#}PeeYan Hunter", "YuChaYuan [YCY]", "- [{~}] YuanYoIn",
                "heal_kitty"}) {
            check("擋得下介面裡的帳號名：" + who,
                  com.wynnchayuan.capture.PlayerDataFilter.looksAccountNamed(who));
        }

        for (String guild : new String[] {
                "- RabbitHouse [Maya]", "- Blank [BLK]", "- TimeCity [tmxt]",
                "- Uchouten Tea House [THTS]", "- Death Star [LIVE]",
                "- Chemdah [CHEM]", "NexusRolly Love [ikun]"}) {
            check("擋得下公會清單：" + guild,
                  com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(guild));
        }

        for (String row : new String[] {
                "- [||{~}||] PoorChaC [{~}]",
                "- [||{~}{~}||] {~}tao_ [{~}]",
                "- {~}jimmy",
                "{~}ay_joker → PoorChaCha: {~} Emeralds",
                "PoorChaCha → {~}yue: a Guild Tome",
                "→ PoorChaCha {~}x {p} Teleportation Scroll [{~}/{~}] (Everyone)",
                "→ RRRRAcher [AS{~}/Hunter]"}) {
            check("擋得下隊伍列／交易列：" + row,
                  com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(row));
        }

        for (String amount : new String[] {
                "- {~}x Pine Plank", "- {~}x Corkian Amplifier III",
                "- {~}m Guild Experience", "- {~}x {#}{#}Cumulonimbus"}) {
            check("不誤擋數量列：" + amount,
                  !com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(amount));
        }

        for (String ui : new String[] {
                "[+2800 ❤] Potions of Healing [9/30]",
                "Willow [67.5%]", "Emerald Pouch [Tier 8]",
                "- Slay Mobs: 12/50", "Rewards [Weekly Objective]"}) {
            check("不誤擋介面文字（公會那條）：" + ui,
                  !com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(ui));
        }

        for (String ui : new String[] {
                "to confirm", "offering better chances", "Reward Sacrifices",
                "Active Boosts", "Unlock Rewards", "Emerald Block",
                "You will be in permanent hunted mode (PvP on)",
                "UnderworldÀÀÀCrypt Key", "Broken DecrepitÀÀÀSewers Key"}) {
            check("不誤擋介面文字：" + ui,
                  !com.wynnchayuan.capture.PlayerDataFilter.looksAccountNamed(ui));
        }

        for (String item : new String[] {
                "Famished's Gambit", "Harvester's Tome of Mysticism III",
                "Major's Badge", "Heroine's Blessing: Devout",
                "Orphion's Nexus of Light", "Bob's Tear"}) {
            check("不誤擋物品名：" + item,
                  !com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(item));
        }

        for (String leak : new String[] {
                "ChangJenChief has died",
                "{#}{#}ChangJenChief has died",
                "{~}jimmy's Totem of Tales",
                "{#} {#}JC grindeando has thrown a",
                "{#} {#}Thank JC grindeando",
                "{#} JC grindeando Loot Bomb has expired! "
                        + "\n{#} Get your own bombs at wynncraft.com/store"}) {
            check("擋得下：" + leak.replace("\n", "⏎"),
                  com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(leak));
        }

        for (String keep : new String[] {
                "{#} You are gaining effects from a Totem of Tales!",
                "{#} You did not enter the event radius in time",
                "{#} Your guild is already attacking this territory!",
                "The king has died in his sleep.",
                "Thank you for your help!",
                "{#} The Lonely Islet World Event starts in {~}m {~}s! ({~}"
                        + "\n{#} blocks away) Click to track",
                "Swashbuckling Brawl ({~}m {~}s left)"}) {
            check("不誤擋：" + keep.replace("\n", "⏎"),
                  !com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(keep));
        }

        List<String> accepted = List.of("manage your island", "repair items");
        blocked.removeAll(accepted);
        check("人工翻過的語料沒有新的誤擋（實際 " + blocked + "）", blocked.isEmpty());

        report();
    }

    private static void partyCards() {
        List<String> card = List.of(
                "Batcave", "World: NA{~}", "✖ This party's world is full");
        check("認得出隊伍卡", PlayerDataFilter.isPartyCard(card));

        check("素材袋不是隊伍卡", !PlayerDataFilter.isPartyCard(
                List.of("Packed Crafter Bag [{~}/{~}]", "Contains one of several")));
        check("技能節點不是隊伍卡", !PlayerDataFilter.isPartyCard(
                List.of("Vacuokinesis", "Your Main Attack is imbued with vacuum,")));
        check("空的不是隊伍卡", !PlayerDataFilter.isPartyCard(List.of()));
        check("null 不是隊伍卡", !PlayerDataFilter.isPartyCard(null));

        check("世界那一行不算玩家資料",
                !PlayerDataFilter.carriesPlayerData("World: NA{~}"));
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
