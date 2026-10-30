package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PartyPanelTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        List<String> panel = List.of(
                "Looking for a party",
                "For The Wartorn Palace",
                "Status:",
                "- Region: AS, NA",
                "- Time estimate: Fast",
                "(keep 1 Ek Rune in your",
                "inventory to find a party",
                "faster)",
                "Click to leave the queue");

        List<Component> lines = new ArrayList<>(panel.size());
        for (String line : panel) {
            lines.add(Component.literal(line));
        }
        List<Component> out = com.wynnchayuan.render.TooltipPanel
                .translateLines(lines, store);
        report("整份都有翻到（實際 " + out.size() + " 行）", !out.isEmpty());

        StringBuilder all = new StringBuilder();
        for (Component c : out) {
            all.append(c.getString()).append('\n');
        }
        String zh = all.toString();
        System.out.println(zh.stripTrailing().indent(4));

        for (String english : new String[] {"Looking for a party", "Status:",
                                            "Region:", "Time estimate:",
                                            "keep", "inventory", "faster",
                                            "Click to leave the queue"}) {
            report("「" + english + "」不該再出現", !zh.contains(english));
        }

        report("沒有逐行條目蓋掉整段",
                TemplateLookup.lookup("inventory to find a party", store, false) == null);

        lootrunBlocks(store);

        System.out.println(failures == 0
                ? "面板整段：全部通過" : "面板整段：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void lootrunBlocks(TranslationStore store) {
        block(store, "詛咒加傷", new String[] {
                "For the rest of this Lootrun,",
                "gain +100% Damage (Max x5)",
                "everytime you get a Curse"}, "傷害");
        String beacon = "Once you have been offered a\nBlue or Purple Beacon more\n"
                + "than {~} times this Lootrun,\ngain +{~} {#}Defence";
        report("信標加防：模板查得到（實際 " + store.lookup(beacon) + "）",
                store.lookup(beacon) != null && store.lookup(beacon).contains("信標"));
        block(store, "寶箱寶物品質", new String[] {
                "For the rest of this Lootrun,",
                "gain +5% Loot Quality (Max",
                "x10) for every 3 items",
                "offered to you from a Chest"}, "寶物品質");
        block(store, "挑戰加生命", new String[] {
                "Once you reach 10 Challenges",
                "completed during your",
                "Lootrun, gain +500 Health"}, "挑戰");
        block(store, "獻祭儀式", new String[] {
                "After finishing a Challenge,",
                "consume 1 Pull to gain +2",
                "Challenges."}, "消耗");
        block(store, "開始條件", new String[] {
                "In order to start a",
                "Lootrun you need to",
                "complete the following",
                "requirements"}, "條件");
        block(store, "領取獎勵", new String[] {
                "Complete the objective",
                "to receive the rewards"}, "領取");
        block(store, "確認投降", new String[] {
                "Click again to confirm",
                "surrendering"}, "投降");
        lootrunEnd(store);
    }

    private static void lootrunEnd(TranslationStore store) {
        row(store, "Yellow Beacon", "黃色信標");
        row(store, "Dark Grey Beacon", "深灰信標");
        row(store, "Crimson Beacon", "緋紅信標");
        row(store, "Obscured Beacon", "晦暗信標");
        row(store, "Offered: {~}", "出現次數");
        row(store, "Chosen: {~}/{~}", "選取次數");
        row(store, "Decay: {~} Challenges", "衰減");
        row(store, "Choices: +{~}", "信標選項");
        row(store, "Knife Edge", "刀鋒");
        row(store, "Thrill Seeker", "逐險者");

        block(store, "詛咒減半", new String[] {
                "Curses are now half as",
                "effective."}, "減半");
        block(store, "逐險者說明", new String[] {
                "Gain +1 Pull per Red Beacon",
                "Challenge completed. This",
                "amount is increased by +1 per",
                "3 challenges completed (max",
                "+5). This boost is reset upon",
                "taking a Green Beacon."}, "紅色信標");
        block(store, "獻祭獎勵說明", new String[] {
                "Sacrificing your rewards",
                "will add a percentage of your",
                "pulls into your next lootrun"}, "獻祭");
        block(store, "獻祭越多保留越多", new String[] {
                "A higher amount of sacrifices",
                "increases the amount of pulls",
                "saved for your next run"}, "保留");
        block(store, "關閉寶箱", new String[] {
                "By closing your chest",
                "inside of it will be lost"}, "寶箱");
    }

    private static void row(TranslationStore store, String src, String want) {
        String zh = TemplateLookup.lookup(src, store, false);
        report("「" + src + "」→「" + want + "」（實際 " + zh + "）",
                zh != null && zh.contains(want));
    }

    private static void block(TranslationStore store, String what,
                              String[] lines, String want) {
        List<Component> in = new ArrayList<>(lines.length);
        for (String line : lines) {
            in.add(Component.literal(line));
        }
        StringBuilder zh = new StringBuilder();
        for (Component c : com.wynnchayuan.render.TooltipPanel.translateLines(in, store)) {
            zh.append(c.getString()).append('\n');
        }
        String out = zh.toString();
        boolean ok = out.contains(want) && !out.contains(lines[0]);
        report(what + "：整段翻出來（實際 " + out.replace('\n', '/').stripTrailing() + "）", ok);
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
