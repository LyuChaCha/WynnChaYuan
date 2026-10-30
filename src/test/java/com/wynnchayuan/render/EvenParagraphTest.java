package com.wynnchayuan.render;

import java.util.List;

public final class EvenParagraphTest {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== 整段一致 ===");

        mixed("折成三行的說明只翻到前兩行",
              List.of("Tomes are special rewards",
                      "which can buff your character",
                      "and abilities in many ways"),
              new boolean[] {true, true, false},
              new boolean[] {false, false, false});

        mixed("只有中間那行沒翻到",
              List.of("Search and filter through",
                      "all available market orders",
                      "by the item level range"),
              new boolean[] {true, false, true},
              new boolean[] {false, false, false});

        same("整段都翻好了就不要動",
             List.of("Tomes are special rewards",
                     "which can buff your character"),
             new boolean[] {true, true});

        same("整段都沒翻也不要動",
             List.of("Tomes are special rewards",
                     "which can buff your character"),
             new boolean[] {false, false});

        same("屬性欄位不是同一段",
             List.of("Health +120", "Walk Speed +8%"),
             new boolean[] {true, false});

        same("句點之後是新的一句",
             List.of("This item has been sealed.",
                     "unlock it with an Identifier"),
             new boolean[] {true, false});

        same("冒號結尾是標題，不是句子",
             List.of("Weekly Objectives:",
                     "complete three lootruns"),
             new boolean[] {true, false});

        same("空行不會把兩段黏起來",
             List.of("Tomes are special rewards", "", "and abilities look"),
             new boolean[] {true, false, false});

        same("單行不受影響", List.of("Loading..."), new boolean[] {false});

        mixed("後面的欄位列不算在段落裡",
              List.of("Tomes are special rewards",
                      "which can buff your character",
                      "Health +120"),
              new boolean[] {true, false, true},
              new boolean[] {false, false, true});

        mixed("停在冠詞上，下一行大寫也是同一句",
              List.of("Use this item at the",
                      "Forgery to craft a",
                      "Infested Pit Key"),
              new boolean[] {false, false, true},
              new boolean[] {false, false, false});

        same("上一行停在名稱上就是另一段",
             List.of("Infested Pit Key", "Use this item at the"),
             new boolean[] {true, false});

        same("單字的標籤不算停在介系詞上",
             List.of("To", "Health +120"),
             new boolean[] {true, false});


        mixed("散文折行時下一行大寫也是同一段",
              List.of("Most people don't think",
                      "Bovemists revere cows for a",
                      "good reason. The people around",
                      "here know better, and leave",
                      "offerings regularly."),
              new boolean[] {true, false, false, false, false},
              new boolean[] {false, false, false, false, false});

        same("名稱加類別之後是新的一行",
             List.of("Slay Slimes [Mini-Quest]", "Currently in progress"),
             new boolean[] {false, true});

        same("帶數字的欄位列不算散文",
             List.of("Recommended Combat Lv", "Health +120"),
             new boolean[] {true, false});

        same("項目符號行不算散文的續行",
             List.of("Converts your emeralds automatically",
                     "- Converts up to Liquid Emeralds"),
             new boolean[] {false, true});

        same("後面接網址的那一句照樣翻",
             List.of("You can get individual boosts at", "wynncraft.com/store"),
             new boolean[] {true, false});
        check("wynncraft.com/store 是網址", TooltipShape.isAddress("wynncraft.com/store"));
        check("wynn.gg/rules 是網址", TooltipShape.isAddress("wynn.gg/rules"));
        check("一般句子不是網址", !TooltipShape.isAddress("Guild Bank"));
        check("句點結尾的句子不是網址", !TooltipShape.isAddress("This item has been sealed."));

        same("整行只有座標的那一行不算一票",
             List.of("A deep cave full of scorched",
                     "creatures and earth lies at",
                     "[1603, 155, -5069]"),
             new boolean[] {true, true, false});

        same("迷你任務的座標行一樣不算一票",
             List.of("Bring [22 Viscous Slime] to the",
                     "Slaying Post [Combat Lv. 50] at",
                     "[-480, 72, -707]"),
             new boolean[] {true, true, false});

        mixed("含實字的座標行仍然算一票",
              List.of("More than just miners and",
                      "pyrotechnics lurk within the",
                      "old saltpetre mine below the",
                      "Highlands at [-1288, 86, -1319]."),
              new boolean[] {true, true, true, false},
              new boolean[] {false, false, false, false});

        mixed("放過座標行不影響其餘幾行的判定",
              List.of("A deep cave full of scorched",
                      "creatures and earth lies at",
                      "[1603, 155, -5069]"),
              new boolean[] {true, false, false},
              new boolean[] {false, false, false});

        same("整段都沒翻就不要動（含座標行）",
             List.of("A deep cave full of scorched",
                     "creatures and earth lies at",
                     "[1603, 155, -5069]"),
             new boolean[] {false, false, false});

        check("整行座標沒有字可翻", TooltipShape.nothingToTranslate("[1603, 155, -5069]"));
        check("帶負號的座標沒有字可翻", TooltipShape.nothingToTranslate("[-480, 72, -707]"));
        check("句子裡夾著座標算有字",
              !TooltipShape.nothingToTranslate("Highlands at [-1288, 86, -1319]."));
        check("停在介系詞上的行算有字",
              !TooltipShape.nothingToTranslate("creatures and earth lies at"));
        check("單位那一個字母也算有字（放寬只認整行沒字母）",
              !TooltipShape.nothingToTranslate("2m30s"));
        check("中文也算有字", !TooltipShape.nothingToTranslate("坐標"));

        keyTooltip();
        skillPointTooltip();

        System.out.println(failures == 0
                ? "整段一致：全部通過"
                : "整段一致：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void mixed(String what, List<String> lines,
                              boolean[] hit, boolean[] want) {
        boolean[] got = hit.clone();
        boolean changed = TooltipShape.evenOut(lines, got);
        check(what + "（有動到）", changed);
        check(what + "（結果 " + show(got) + "）", java.util.Arrays.equals(got, want));
    }

    private static void same(String what, List<String> lines, boolean[] hit) {
        boolean[] got = hit.clone();
        boolean changed = TooltipShape.evenOut(lines, got);
        check(what + "（結果 " + show(got) + "）",
              !changed && java.util.Arrays.equals(got, hit));
    }

    private static void keyTooltip() {
        com.wynnchayuan.translate.TranslationStore store =
                new com.wynnchayuan.translate.TranslationStore();
        store.loadAll(java.nio.file.Path.of(
                "src/main/resources/assets/wynnchayuan/translations",
                com.wynnchayuan.translate.Languages.DEFAULT));
        check("★ 鑰匙名單獨查得到（不然下面測的是空氣）",
              store.lookup("Infested Pit Key") != null);
        check("★ 整段條目確實沒收（收了就該整段翻，這條測試要跟著改）",
              store.lookup("Use this item at the\nForgery to craft a\nInfested Pit Key") == null
                      && store.lookupFlat("Use this item at the Forgery to craft a Infested Pit Key")
                              == null);

        List<net.minecraft.network.chat.Component> out = TooltipPanel.translateLines(List.of(
                net.minecraft.network.chat.Component.literal("Infested Pit Key"),
                net.minecraft.network.chat.Component.literal("Use this item at the"),
                net.minecraft.network.chat.Component.literal("Forgery to craft a"),
                net.minecraft.network.chat.Component.literal("Infested Pit Key")), store);
        check("鑰匙 tooltip 照樣畫得出來（實際 " + out.size() + " 行）", out.size() == 4);
        if (out.size() != 4) {
            return;
        }
        check("標題的地城名照翻（實際 " + out.get(0).getString() + "）",
              !out.get(0).getString().equals("Infested Pit Key"));
        check("★ 句子裡的鑰匙名跟著整句留英文（實際 " + out.get(3).getString() + "）",
              out.get(3).getString().equals("Infested Pit Key"));
    }

    private static void skillPointTooltip() {
        java.nio.file.Path layer = java.nio.file.Path.of("build", "test-layers", "skill-point");
        try {
            java.nio.file.Files.createDirectories(layer);
            java.nio.file.Files.writeString(layer.resolve("misc.json"),
                    "{\"You cannot change your\": \"你目前無法更改你的\"}",
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            check("建得出測試用的語料層（" + e + "）", false);
            return;
        }
        com.wynnchayuan.translate.TranslationStore store =
                new com.wynnchayuan.translate.TranslationStore();
        store.loadAll(List.of(java.nio.file.Path.of(
                "src/main/resources/assets/wynnchayuan/translations",
                com.wynnchayuan.translate.Languages.DEFAULT), layer));
        check("★ 疊上去的那一行查得到（不然下面測的是空氣）",
              "你目前無法更改你的".equals(store.lookup("You cannot change your")));

        for (String[] skill : new String[][] {
                {"Strength", "increase", "any damage you deal,", "and increase the ", "Earth", " damage"},
                {"Agility", "increase", "the chance to dodge attacks (90% damage reduction),",
                        "and increase the ", "Air", " damage"}}) {
            for (boolean locked : new boolean[] {true, false}) {
                List<net.minecraft.network.chat.Component> tip = skillRows(skill, locked);
                String label = skill[0] + (locked ? "（不能改點）" : "（可以改點）");
                List<net.minecraft.network.chat.Component> out;
                try {
                    out = TooltipPanel.translateLines(tip, store);
                } catch (RuntimeException e) {
                    check(label + "：不會丟例外（實際 " + e + "）", false);
                    continue;
                }
                check(label + "：不會丟例外", true);
                StringBuilder shown = new StringBuilder();
                for (net.minecraft.network.chat.Component c : out) {
                    shown.append(c.getString()).append(" | ");
                }
                check(label + "：標題照翻（實際 " + shown + "）",
                      out.isEmpty() || !shown.toString().contains("Upgrade your"));
                check(label + "：行數沒有比原文多（原文 " + tip.size() + "、實際 " + out.size() + "）",
                      out.size() <= tip.size());
                check(label + "：說明那一整句照樣是中文",
                      shown.toString().contains("這項屬性每加一點"));
                if (locked) {
                    check(label + "：只翻到一半的那一段整段留英文",
                          shown.toString().contains("You cannot change your")
                                  && shown.toString().contains("skill points at the moment")
                                  && !shown.toString().contains("你目前無法更改你的"));
                }
            }
        }
        List<net.minecraft.network.chat.Component> broken = new java.util.ArrayList<>();
        broken.add(null);
        try {
            check("就地取代遇到壞掉的 tooltip 回傳空的（= 原文不動）",
                  TooltipPanel.translateInPlace(broken, store).isEmpty());
        } catch (RuntimeException e) {
            check("就地取代遇到壞掉的 tooltip 不丟例外（實際 " + e + "）", false);
        }
    }

    private static List<net.minecraft.network.chat.Component> skillRows(String[] skill,
                                                                        boolean locked) {
        net.minecraft.network.chat.Style grey = net.minecraft.network.chat.Style.EMPTY
                .withColor(net.minecraft.network.chat.TextColor.fromRgb(0xAAAAAA));
        net.minecraft.network.chat.Style pink = net.minecraft.network.chat.Style.EMPTY
                .withColor(net.minecraft.network.chat.TextColor.fromRgb(0xFF55FF));
        net.minecraft.network.chat.Style red = net.minecraft.network.chat.Style.EMPTY
                .withColor(net.minecraft.network.chat.TextColor.fromRgb(0xFF5555));
        net.minecraft.network.chat.Style icon = net.minecraft.network.chat.Style.EMPTY.withFont(
                new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier.withDefaultNamespace("common")));
        List<net.minecraft.network.chat.Component> rows = new java.util.ArrayList<>();
        rows.add(row(offset(15, grey), text("Upgrade your ", grey), text("", icon),
                     text(" " + skill[0], grey), text(" skill", grey)));
        rows.add(text(" ".repeat(42), grey));
        rows.add(row(offset(18, grey), text("Now", grey), offset(31, grey), offset(30, grey),
                     offset(28, grey), text("Next", grey)));
        rows.add(row(offset(15, grey), text("57.8%", grey), offset(28, grey), text(">>>>>>", grey),
                     offset(28, grey), text("58.3%", grey)));
        rows.add(row(offset(5, grey), text("70 points", grey), offset(18, grey), offset(30, grey),
                     offset(18, grey), text("71 points", grey)));
        rows.add(row(offset(10, grey), text("* Modified by your gear (+24)", grey)));
        rows.add(text("", icon));
        rows.add(text("Each point in this skill will", grey));
        rows.add(row(text(skill[1] + " ", pink), text(skill[2], grey)));
        rows.add(row(text(skill[3], grey), text("", icon), text(" " + skill[4], grey),
                     text(skill[5], grey)));
        rows.add(text("you may inflict", grey));
        rows.add(text("", icon));
        if (locked) {
            rows.add(text("You cannot change your", red));
            rows.add(text("skill points at the moment", red));
        } else {
            rows.add(row(text("", icon), text(" Left-Click to add 1 point", grey)));
            rows.add(row(text("", icon), text(" Right-Click to remove 1 point", grey)));
            rows.add(row(text("", icon), text(" Hold Shift to modify by 5", grey)));
        }
        return rows;
    }

    private static net.minecraft.network.chat.MutableComponent text(
            String text, net.minecraft.network.chat.Style style) {
        return net.minecraft.network.chat.Component.literal(text).withStyle(style);
    }

    private static net.minecraft.network.chat.MutableComponent offset(
            int px, net.minecraft.network.chat.Style style) {
        return text(com.wynnchayuan.translate.SpaceOffset.encode(px),
                    com.wynnchayuan.translate.SpaceOffset.styleFor(style));
    }

    private static net.minecraft.network.chat.Component row(
            net.minecraft.network.chat.Component... parts) {
        net.minecraft.network.chat.MutableComponent line =
                net.minecraft.network.chat.Component.empty();
        for (net.minecraft.network.chat.Component part : parts) {
            line.append(part);
        }
        return line;
    }

    private static String show(boolean[] a) {
        StringBuilder sb = new StringBuilder();
        for (boolean b : a) {
            sb.append(b ? '中' : '英');
        }
        return sb.toString();
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
