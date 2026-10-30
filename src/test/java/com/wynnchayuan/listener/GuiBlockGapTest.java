package com.wynnchayuan.listener;

import java.util.List;

public final class GuiBlockGapTest {

    private static int failures = 0;

    private static boolean known(List<String> lines) {
        String joined = String.join(" ", lines);
        return joined.equals("Contains one of several exclusive ingredients in high quantities.")
                || String.join("\n", lines).equals("Increases your maximum orbs\n"
                        + "from Ophanim by +{~} and\nreduces their damage.");
    }

    public static void main(String[] args) {
        List<String> bag = java.util.Arrays.asList(
                "Packed Crafter Bag [{~}/{~}]",
                null,
                "Contains one of several",
                "exclusive ingredients in",
                "high quantities.",
                null,
                "Rare Item");
        boolean[] out = GuiTextCapture.covered(bag, GuiBlockGapTest::known);
        report("標題不算在段落裡", !out[0]);
        report("整段翻好的三行都不列成缺口", out[2] && out[3] && out[4]);
        report("後面那行不受影響", !out[6]);

        List<String> node = java.util.Arrays.asList(
                "Divination",
                null,
                "Increases your maximum orbs",
                "from Ophanim by +{~} and",
                "reduces their damage.");
        boolean[] two = GuiTextCapture.covered(node, GuiBlockGapTest::known);
        report("換行接法也認得", two[2] && two[3] && two[4]);
        report("技能名還是要收", !two[0]);

        List<String> unknown = java.util.Arrays.asList(
                "Quickly find parties", "for raids and others!");
        boolean[] none = GuiTextCapture.covered(unknown, GuiBlockGapTest::known);
        report("整段查不到就照舊列成缺口", !none[0] && !none[1]);

        List<String> single = java.util.Arrays.asList("Contains one of several");
        report("單行段落不處理",
                !GuiTextCapture.covered(single, GuiBlockGapTest::known)[0]);

        mountFood();
        oneEntryPerParagraph();

        System.out.println(failures == 0
                ? "整段缺口：全部通過" : "整段缺口：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void oneEntryPerParagraph() {
        List<String> totem = java.util.Arrays.asList(
                "Mana Totem",
                null,
                "Summons a totem that",
                "restores {~} Mana to all",
                "nearby players every {~}s.",
                "",
                "- {~}x Mydium",
                "- {~}x Copper Ingot",
                "",
                "Owner: Steve_123");
        boolean[] none = new boolean[totem.size()];
        java.util.function.Predicate<String> listItems = t -> t.startsWith("- {~}x Copper");
        java.util.function.Predicate<String> personal = t -> t.startsWith("Owner:");
        List<String[]> rows = GuiTextCapture.rows(totem, none, 1, listItems, personal);

        report("折成三行的一句收成一條 gui/block，照畫面換行",
                rows.stream().anyMatch(r -> "gui/block".equals(r[1]) && r[0].equals(
                        "Summons a totem that\nrestores {~} Mana to all\nnearby players every {~}s.")));
        report("半句不再各自一條", rows.stream().noneMatch(r -> "restores {~} Mana to all".equals(r[0])));
        report("條列裡有一項自己就有譯文：整段照舊逐行收",
                rows.stream().anyMatch(r -> "- {~}x Mydium".equals(r[0]) && "gui/line".equals(r[1]))
                        && rows.stream().noneMatch(r -> r[0].contains("Mydium\n")));
        report("帶玩家資料的那一段整段不收，只記次數",
                rows.stream().noneMatch(r -> r[0].contains("Steve_123"))
                        && rows.stream().anyMatch(r -> "gui.blocked.playerData".equals(r[0]) && r[1] == null));
        report("標題不在這裡收（從標題下一行開始）",
                rows.stream().noneMatch(r -> "Mana Totem".equals(r[0])));

        boolean[] done = new boolean[totem.size()];
        done[2] = done[3] = done[4] = true;
        List<String[]> covered = GuiTextCapture.rows(totem, done, 1, listItems, personal);
        report("整段翻好了：不收，只記次數",
                covered.stream().noneMatch(r -> r[0].contains("Summons"))
                        && covered.stream().anyMatch(r -> "gui.skipped.inBlock".equals(r[0])));
    }

    private static void mountFood() {
        com.wynnchayuan.translate.TranslationStore store =
                new com.wynnchayuan.translate.TranslationStore();
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("wynnchayuan-mount-food");
            java.nio.file.Files.writeString(dir.resolve("material.json"), """
                    {"_meta": {"itemNames": true, "gearNames": false},
                     "entries": {"m1": {
                       "src": "Feed to your Mount to upgrade its Acceleration, Toughness and Speed\\nCan be used to purchase blocks in Housing, or to craft {#} Spears, {#} Daggers, {#} Bows, {#} Reliks & {#} Wands",
                       "dst": "餵給坐騎可提升加速、韌性與速度\\n可用來購買房屋方塊，或製作 {#} 長矛、{#} 匕首、{#} 弓、{#} 聖物與 {#} 法杖",
                       "role": "desc"}}}
                    """, java.nio.charset.StandardCharsets.UTF_8);
            store.loadAll(dir);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        List<String> food = java.util.Arrays.asList(
                "{#}Bonder{#}",
                null,
                "{#}Feed to your Mount to upgrade its",
                "{#}Acceleration, Toughness and Speed",
                "{#}Can be used to purchase blocks in",
                "{#}Housing, or to craft {#} Spears, {#}",
                "{#}Daggers, {#} Bows, {#} Reliks & {#} Wands");
        boolean[] out = GuiTextCapture.covered(food, lines -> GuiTextCapture.whole(lines, store));
        report("★ 行首掛著偏移、斷行又不同的整段，每一行都不列成缺口",
                out[2] && out[3] && out[4] && out[5] && out[6]);
        report("標題照舊要收", !out[0]);

        List<String> other = java.util.Arrays.asList(
                "{#}Feed to your Mount to upgrade", "{#}something nobody translated");
        boolean[] none = GuiTextCapture.covered(other, lines -> GuiTextCapture.whole(lines, store));
        report("剝掉偏移也查不到的照舊列成缺口", !none[0] && !none[1]);
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
