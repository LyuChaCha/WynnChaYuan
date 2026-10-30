package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GearNameTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-gear");

        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Guardian", "dst": "", "role": "name"},
                   "a2": {"src": "Idol", "dst": "神像", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("major-id.json"),
                "{\"Guardian\": \"守護者\", \"Idol\": \"偶像\"}", StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        check("★ 認得出「還沒翻的裝備名」", store.isBareGearName("Guardian"));

        check("查表不受影響（實際 " + store.lookup("Guardian") + "）",
              "守護者".equals(store.lookup("Guardian")));

        String idol = store.lookup("Idol");
        check("翻好的裝備名照常翻（實際 " + idol + "）",
              "神像".equals(idol));

        Files.writeString(dir.resolve("ui-labels.json"),
                "{\"Reflection\": \"遠程反傷\"}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {"b1": {"src": "Reflection", "dst": "", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        store = new TranslationStore();
        store.loadAll(dir);
        check("撞名的介面標籤照常翻（實際 " + store.lookup("Reflection") + "）",
              "遠程反傷".equals(store.lookup("Reflection")));
        check("但它仍然被認得出是還沒翻的裝備名",
              store.isBareGearName("Reflection"));

        Files.writeString(dir.resolve("misc.json"),
                "{\"Fissure\": \"地裂\"}", StandardCharsets.UTF_8);
        store = new TranslationStore();
        store.loadAll(dir);
        check("不是裝備名的照常翻（實際 " + store.lookup("Fissure") + "）",
              "地裂".equals(store.lookup("Fissure")));

        nameModes();
        badgeInDescription();
        decorations();
        twoLineName();
        abilityNodeTitles();
        report();
    }

    private static void nameModes() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-gear-modes");
        Files.writeString(dir.resolve("gear-accessory.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Diffraction", "dst": "晶化蔓延", "role": "name"},
                   "a2": {"src": "Smithy", "dst": "鐵匠鋪", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.createDirectories(dir.resolve("ability"));
        Files.writeString(dir.resolve("ability/mage.json"), """
                {"_meta": {"itemNames": false},
                 "entries": {"m1": {"src": "Diffraction", "dst": "晶化蔓延", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.OFF);
        check("關：只屬於裝備的名字不翻（實際 " + store.lookup("Smithy") + "）",
              store.lookup("Smithy") == null);
        check("★ 關：撞名的技能譯名照常查得到（實際 " + store.lookup("Diffraction") + "）",
              "晶化蔓延".equals(store.lookup("Diffraction")));
        check("★ 關：名稱那一行的守門認得翻好的裝備名", store.isBareGearName("Diffraction")
              && store.isBareGearName("Smithy"));

        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.ON);
        check("開：裝備名照翻（實際 " + store.lookup("Smithy") + "）",
              "鐵匠鋪".equals(store.lookup("Smithy")));
        check("開：守門不擋翻好的裝備名", !store.isBareGearName("Diffraction")
              && !store.isBareGearName("Smithy"));

        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.BOTH);
        check("譯名加原文（實際 " + store.lookup("Smithy") + "）",
              "鐵匠鋪 (Smithy)".equals(store.lookup("Smithy")));
        check("撞名的技能標題不加原文（實際 " + store.lookup("Diffraction") + "）",
              "晶化蔓延".equals(store.lookup("Diffraction")));

        try (LookupScope scope = LookupScope.holdAppendedOriginal(true)) {
            check("★ 敘述那幾行只給譯名（實際 " + store.lookup("Smithy") + "）",
                  "鐵匠鋪".equals(store.lookup("Smithy")));
        }
        check("放開之後名稱那一行照樣附原文（實際 " + store.lookup("Smithy") + "）",
              "鐵匠鋪 (Smithy)".equals(store.lookup("Smithy")));
    }

    private static void badgeInDescription() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-badge");
        Files.writeString(dir.resolve("gear-accessory.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {"a1": {"src": "Snowflake", "dst": "雪花", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"),
                "{\"Guild Badge\": \"公會徽章\"}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.BOTH);

        String body = joined(store, "Guild Badge", "",
                             "Unlocks the Guild Badge", "Snowflake");
        check("★ 敘述裡的徽章名只給譯名（實際 " + body.replace(System.lineSeparator(), " / ") + "）",
              body.contains("雪花") && !body.contains("(Snowflake)"));

        String name = joined(store, "Snowflake", "Snowflake", "Combat Lv. Min: 1");
        check("名稱那一行照樣附原文（實際 " + name.replace(System.lineSeparator(), " / ") + "）",
              name.contains("雪花 (Snowflake)"));
    }

    private static String joined(TranslationStore store, String... rows) {
        java.util.List<net.minecraft.network.chat.Component> tip =
                new java.util.ArrayList<>();
        for (String row : rows) {
            tip.add(net.minecraft.network.chat.Component.literal(row));
        }
        StringBuilder all = new StringBuilder();
        for (net.minecraft.network.chat.Component line
                : com.wynnchayuan.render.TooltipPanel.translateLines(tip, store)) {
            all.append(line.getString()).append(System.lineSeparator());
        }
        return all.toString();
    }

    private static void abilityNodeTitles() throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.OFF);

        for (String[] pair : new String[][] {
                {"Diffraction", "晶化蔓延"}, {"Gleam", "耀光"}, {"Paradox", "悖論"},
                {"Sunshower", "太陽雨"}, {"Harmony", "調和"}, {"Echo", "複演"}}) {
            check("★「" + pair[0] + "」是還沒翻的裝備名（守門會對它出手）",
                  store.isBareGearName(pair[0]));
            check("「" + pair[0] + "」查得到技能譯名（實際 "
                            + store.lookup(pair[0]) + "）",
                  pair[1].equals(store.lookup(pair[0])));
        }

        titleBecomes(store, "Diffraction", "晶化蔓延", "✖ Ability Points: 1");
        titleBecomes(store, "Gleam", "耀光", "✖ Ability Points: 1");
        titleBecomes(store, "Paradox", "悖論", "✖ Ability Points: 2");

        for (String[] pair : new String[][] {
                {"Redemption", "救贖"}, {"Stasis", "停滯"},
                {"Heavensent", "天賜"}, {"Serendipity", "機緣"}}) {
            check("★ 使命「" + pair[0] + "」也是還沒翻的裝備名（守門會對它出手）",
                  store.isBareGearName(pair[0]));
            missionTitle(store, pair[0], pair[1]);
        }
        missionTitle(store, "Ostinato", "頑固音型");

        boonTitle(store, "Heavensent", "天賜", "Dynamic Boon");
        boonTitle(store, "Heavensent", "天賜", "Static Boon");

        titleStays(store, "Diffraction", "Fire Spell Damage +100");
        titleStays(store, "Gleam", "Fire Spell Damage +100");

        loreLines(store);
    }

    private static void loreLines(TranslationStore store) {
        java.util.List<net.minecraft.network.chat.Component> gear =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal("Ashen Conscience"),
                                net.minecraft.network.chat.Component.literal(
                                        "- Converts up to Liquid Emeralds"),
                                net.minecraft.network.chat.Component.literal("- Guardian"),
                                net.minecraft.network.chat.Component.literal("- Tempest"),
                                net.minecraft.network.chat.Component.literal(
                                        "Left-Click to view contents")),
                        store);
        check("★ 清單那份 tooltip 有翻到東西（不然下面幾項是廢話）", gear.size() == 5);
        String bullet = gear.size() < 2 ? "" : gear.get(1).getString();
        check("一般的項目符號行照翻（實際 " + bullet + "）", bullet.contains("液態綠寶石"));
        for (int row : new int[] {2, 3}) {
            String line = gear.size() <= row ? "" : gear.get(row).getString();
            check("★ 清單裡的裝備名留原文（實際 " + line + "）",
                  line.startsWith("- ") && line.equals(line.replaceAll("[一-鿿]", "")));
        }

        java.util.List<net.minecraft.network.chat.Component> node =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal("Gleam"),
                                net.minecraft.network.chat.Component.literal(
                                        "Unlocking will block:"),
                                net.minecraft.network.chat.Component.literal("- Paradox"),
                                net.minecraft.network.chat.Component.literal("- Diffraction"),
                                net.minecraft.network.chat.Component.literal(
                                        "✔ Ability Points: 2")),
                        store);
        String blocked = node.size() < 3 ? "" : node.get(2).getString();
        check("★ 技能樹的封鎖清單照樣翻（實際 " + blocked + "）", blocked.contains("悖論"));

        guessLines(store);
    }

    private static void guessLines(TranslationStore store) {
        check("★ Bloodlust 是還沒翻的裝備名（守門會對它出手）",
              store.isBareGearName("Bloodlust"));
        check("★ Bloodlust 查得到同名技能的譯名（不然下面測的是空氣，實際 "
                        + store.lookup("Bloodlust") + "）",
              store.lookup("Bloodlust") != null);

        java.util.List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal("Unidentified Boots"),
                                net.minecraft.network.chat.Component.literal("86-90 Level Range"),
                                net.minecraft.network.chat.Component.literal(""),
                                net.minecraft.network.chat.Component.literal("- Possibilities: "),
                                guessRow("Lv. 87", "Bloodlust"),
                                guessRow("等級 87", "Guardian"),
                                guessRow("Lv. 88", "Sempiternal, Statue"),
                                net.minecraft.network.chat.Component.literal("- Streak: 0")),
                        store);
        check("★ 候選清單那份 tooltip 有翻到東西（實際 " + out.size() + " 行）",
              out.size() == 8);
        if (out.size() != 8) {
            return;
        }
        check("等級區間照翻（實際 " + out.get(1).getString() + "）",
              out.get(1).getString().contains("等級區間"));
        check("★ 英文前綴那一行留著裝備名（實際 " + out.get(4).getString() + "）",
              out.get(4).getString().endsWith(": Bloodlust"));
        check("★ Wynntils 中文前綴那一行也留著（實際 " + out.get(5).getString() + "）",
              out.get(5).getString().endsWith(": Guardian"));
        check("沒撞名的那一行本來就是原文（實際 " + out.get(6).getString() + "）",
              out.get(6).getString().endsWith(": Sempiternal, Statue"));
        check("同一份裡的一般清單行照翻（實際 " + out.get(7).getString() + "）",
              out.get(7).getString().contains("連續"));
    }

    private static net.minecraft.network.chat.Component guessRow(String level, String names) {
        net.minecraft.network.chat.MutableComponent row =
                net.minecraft.network.chat.Component.empty();
        Object[] parts = {"    ", 0xFFFFFF, "- ", 0x55FF55, level, 0xAAAAAA, " [", 0xAAAAAA,
                          "113 ²", 0x55FF55, "]", 0xAAAAAA, ": ", 0xAAAAAA, names, 0xFF55FF};
        for (int i = 0; i < parts.length; i += 2) {
            row.append(net.minecraft.network.chat.Component.literal((String) parts[i])
                    .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(
                            net.minecraft.network.chat.TextColor.fromRgb((Integer) parts[i + 1]))));
        }
        return row;
    }

    private static void titleBecomes(TranslationStore store, String name,
                                     String want, String points) {
        java.util.List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(points)),
                        store);
        String line0 = out.isEmpty() ? "" : out.get(0).getString();
        String line1 = out.size() < 2 ? "" : out.get(1).getString();
        check("技能樹「" + name + "」-> 「" + want + "」（實際 "
                        + line0 + " / " + line1 + "）",
              want.equals(line0) && want.equals(line1));
    }

    private static void missionTitle(TranslationStore store, String name, String want) {
        java.util.List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal("Objective:"),
                                net.minecraft.network.chat.Component.literal("Open 12 Chests"),
                                net.minecraft.network.chat.Component.literal("Reward:")),
                        store);
        String line0 = out.isEmpty() ? "" : out.get(0).getString();
        check("使命「" + name + "」-> 「" + want + "」（實際 " + line0 + "）",
              want.equals(line0));
    }

    private static void boonTitle(TranslationStore store, String name, String want,
                                  String kind) {
        java.util.List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(" "),
                                net.minecraft.network.chat.Component.literal(kind),
                                net.minecraft.network.chat.Component.literal(" "),
                                net.minecraft.network.chat.Component.literal("Click to choose!")),
                        store);
        String line0 = out.isEmpty() ? "" : out.get(0).getString();
        check("賜福「" + name + "」(" + kind + ") -> 「" + want + "」（實際 " + line0 + "）",
              want.equals(line0));
    }

    private static void titleStays(TranslationStore store, String name, String gearLine) {
        java.util.List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(gearLine)),
                        store);
        check("★ 裝備「" + name + "」那份 tooltip 有翻到東西（不然下一項是廢話）",
              out.size() == 3);
        String line0 = out.isEmpty() ? "" : out.get(0).getString();
        String line1 = out.size() < 2 ? "" : out.get(1).getString();
        check("★ 裝備「" + name + "」仍然留原文（實際 " + line0 + " / " + line1 + "）",
              name.equals(line0) && name.equals(line1));
    }

    private static void decorations() {
        check("剝掉鑑定百分比",
              "Willow".equals(com.wynnchayuan.render.TooltipPanel
                      .bareName("Willow [{~}%]")));
        check("剝掉多個註記",
              "Willow".equals(com.wynnchayuan.render.TooltipPanel
                      .bareName("Willow [{~}%] [✔]")));
        check("沒有註記時原樣回傳",
              "Idol".equals(com.wynnchayuan.render.TooltipPanel.bareName("Idol")));
        check("符號佔位符一併剝掉",
              "Idol".equals(com.wynnchayuan.render.TooltipPanel
                      .bareName("{#}{#}Idol{#}")));
        check("整行都是括號時不剝",
              "[{~}%]".equals(com.wynnchayuan.render.TooltipPanel
                      .bareName("[{~}%]")));
    }

    private static void twoLineName() {
        check("標記行與顯示行剝出同一個名字",
              com.wynnchayuan.render.TooltipPanel.bareName("{#}Guardian{#}")
                      .equals(com.wynnchayuan.render.TooltipPanel
                              .bareName("{#}{#}{#}{#}{#}Guardian [{~}]")));
        check("沒有鑑定百分比的也一樣（素材）",
              com.wynnchayuan.render.TooltipPanel.bareName("{#}Radioactive Soil{#}")
                      .equals(com.wynnchayuan.render.TooltipPanel
                              .bareName("{#}{#}{#}{#}{#}Radioactive Soil")));
        check("下一行不是同一個名字就不擋",
              !com.wynnchayuan.render.TooltipPanel.bareName("{#} Earth Powder V")
                      .equals(com.wynnchayuan.render.TooltipPanel
                              .bareName("Tier {~} [■■■■■■■]")));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("裝備名：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("裝備名：全部通過");
    }
}
