package com.wynnchayuan.render;

import java.util.List;

/**
 * 同一段話不能一半中文一半英文。
 *
 * <h2>先前壞在哪</h2>
 * 說明文字是被 tooltip 的<b>寬度</b>折成好幾行的，而語料裡很多段落是一行一條
 * 記下來的——那等於把譯文釘死在翻譯者當時的斷行上。玩家的介面縮放不一樣、
 * 或句子裡的數字多一位數，斷點就跟著跑：有幾行對得上、有幾行對不上，
 * 畫面上就成了中英夾雜的一段。
 *
 * <p>倉庫裡量到 83 條「只有半句、而且沒有整段條目罩著」的譯文。
 *
 * <h2>這裡釘住什麼</h2>
 * 真正的風險是<b>認錯段落</b>：把不相干的兩行當成同一句，就會把翻好的那一行
 * 也退回英文，而畫面上看起來只是「這行忽然沒翻」，完全查不出原因。
 * 所以正反兩個方向都測，尤其是欄位列與標題那些<b>不該</b>被併起來的形狀。
 */
public final class EvenParagraphTest {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== 整段一致 ===");

        // --- 該拉平的 -----------------------------------------------------
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

        // --- 不能動的 -----------------------------------------------------
        same("整段都翻好了就不要動",
             List.of("Tomes are special rewards",
                     "which can buff your character"),
             new boolean[] {true, true});

        same("整段都沒翻也不要動",
             List.of("Tomes are special rewards",
                     "which can buff your character"),
             new boolean[] {false, false});

        // 欄位列各自獨立：大寫開頭，不是接續
        same("屬性欄位不是同一段",
             List.of("Health +120", "Walk Speed +8%"),
             new boolean[] {true, false});

        // 上一行有句點就是收尾了
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

        // 段落只有一行時本來就沒有「一半」可言
        same("單行不受影響", List.of("Loading..."), new boolean[] {false});

        // 三行的段落後面接一個獨立欄位：欄位不能被拖下水
        mixed("後面的欄位列不算在段落裡",
              List.of("Tomes are special rewards",
                      "which can buff your character",
                      "Health +120"),
              new boolean[] {true, false, true},
              new boolean[] {false, false, true});

        // --- 停在冠詞、介系詞上的行 ---------------------------------------
        // 地城鑰匙：後兩行大寫開頭（接的是專有名詞），先前被當成三段，
        // 語料單獨收著的鑰匙名就自己翻成中文，前兩行留英文。
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


        // 首領祭壇的敘述：第二行大寫開頭（專有名詞），第一行結尾是 think——
        // 不是冠詞也不是介系詞。先前兩條判準都不中，第一行自成一段，
        // 於是畫面上一句中文接三句英文（使用者 2026-09-20 的截圖）。
        mixed("散文折行時下一行大寫也是同一段",
              List.of("Most people don't think",
                      "Bovemists revere cows for a",
                      "good reason. The people around",
                      "here know better, and leave",
                      "offerings regularly."),
              new boolean[] {true, false, false, false, false},
              new boolean[] {false, false, false, false, false});

        // 反方向：標題那一行結尾是 ]，下一行是另一件事。
        same("名稱加類別之後是新的一行",
             List.of("Slay Slimes [Mini-Quest]", "Currently in progress"),
             new boolean[] {false, true});

        // 反方向：屬性列沒有句點也沒有冠詞，但帶數字——不能被當成散文。
        same("帶數字的欄位列不算散文",
             List.of("Recommended Combat Lv", "Health +120"),
             new boolean[] {true, false});

        // 反方向：項目符號開頭的是清單的一項，不是句子的續行。
        same("項目符號行不算散文的續行",
             List.of("Converts your emeralds automatically",
                     "- Converts up to Liquid Emeralds"),
             new boolean[] {false, true});

        // 網址翻不了，不能拿它判定「翻了一半」
        same("後面接網址的那一句照樣翻",
             List.of("You can get individual boosts at", "wynncraft.com/store"),
             new boolean[] {true, false});
        check("wynncraft.com/store 是網址", TooltipPanel.isAddress("wynncraft.com/store"));
        check("wynn.gg/rules 是網址", TooltipPanel.isAddress("wynn.gg/rules"));
        check("一般句子不是網址", !TooltipPanel.isAddress("Guild Bank"));
        check("句點結尾的句子不是網址", !TooltipPanel.isAddress("This item has been sealed."));

        // --- 整行只有座標的那一行 -----------------------------------------
        // 洞窟卡的敘述：前兩行語料裡<b>都有</b>，第三行整行只有座標。
        // 座標那一行的模板（{@code [{~}, {~}, -{~}]}）一個字母都沒有，
        // LineTranslator 自己就先擋了，永遠不可能「翻到」。先前它照樣算一票，
        // 整段於是被判成翻了一半 → 整張洞窟卡的敘述退回英文。
        // 使用者回報的「明明語料查得到、畫面還是英文」就是這個。
        same("整行只有座標的那一行不算一票",
             List.of("A deep cave full of scorched",
                     "creatures and earth lies at",
                     "[1603, 155, -5069]"),
             new boolean[] {true, true, false});

        // 迷你任務是同一個形狀，座標帶負號
        same("迷你任務的座標行一樣不算一票",
             List.of("Bring [22 Viscous Slime] to the",
                     "Slaying Post [Combat Lv. 50] at",
                     "[-480, 72, -707]"),
             new boolean[] {true, true, false});

        // 反方向（守門不能被放壞）：座標夾在句子裡，那一行有實字——
        // 它是真的沒翻到，整段照樣要退回英文。
        mixed("含實字的座標行仍然算一票",
              List.of("More than just miners and",
                      "pyrotechnics lurk within the",
                      "old saltpetre mine below the",
                      "Highlands at [-1288, 86, -1319]."),
              new boolean[] {true, true, true, false},
              new boolean[] {false, false, false, false});

        // 反方向：放過座標行不代表其餘幾行不用一致
        mixed("放過座標行不影響其餘幾行的判定",
              List.of("A deep cave full of scorched",
                      "creatures and earth lies at",
                      "[1603, 155, -5069]"),
              new boolean[] {true, false, false},
              new boolean[] {false, false, false});

        // 整段都沒翻（語料真的沒收）時照舊不動
        same("整段都沒翻就不要動（含座標行）",
             List.of("A deep cave full of scorched",
                     "creatures and earth lies at",
                     "[1603, 155, -5069]"),
             new boolean[] {false, false, false});

        check("整行座標沒有字可翻", TooltipPanel.nothingToTranslate("[1603, 155, -5069]"));
        check("帶負號的座標沒有字可翻", TooltipPanel.nothingToTranslate("[-480, 72, -707]"));
        check("句子裡夾著座標算有字",
              !TooltipPanel.nothingToTranslate("Highlands at [-1288, 86, -1319]."));
        check("停在介系詞上的行算有字",
              !TooltipPanel.nothingToTranslate("creatures and earth lies at"));
        check("單位那一個字母也算有字（放寬只認整行沒字母）",
              !TooltipPanel.nothingToTranslate("2m30s"));
        check("中文也算有字", !TooltipPanel.nothingToTranslate("坐標"));

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
        boolean changed = TooltipPanel.evenOut(lines, got);
        check(what + "（有動到）", changed);
        check(what + "（結果 " + show(got) + "）", java.util.Arrays.equals(got, want));
    }

    private static void same(String what, List<String> lines, boolean[] hit) {
        boolean[] got = hit.clone();
        boolean changed = TooltipPanel.evenOut(lines, got);
        check(what + "（結果 " + show(got) + "）",
              !changed && java.util.Arrays.equals(got, hit));
    }

    /**
     * 實機回報的那份地城鑰匙 tooltip，拿出貨的語料整份畫一次。
     *
     * <p>畫面上是「Use this item at the / Forgery to craft a / 虫蚀深坑钥匙」。
     * 整段條目只收了 Decrepit Sewers 那一把，這一把沒有——那是語料的缺口；
     * 這裡釘住的是<b>缺的時候</b>別夾出半句英文接中文名字。
     * 標題那一行是地城名，照團隊決定要翻，不能被一起拉回英文。
     */
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

    /**
     * 技能點數選單的 tooltip（「Upgrade your  Strength skill」那一格），照實機的 layout-debug 組。
     *
     * <h2>實機回報</h2>
     * 就地取代模式下滑過去，error-debug 記到
     * {@code IndexOutOfBoundsException: Index 15 out of bounds for length 15}，
     * 丟在 {@code translateLines} 的 {@code out.set}。
     *
     * <p>說明那四行在語料裡是<b>一整句</b>（gui.json 的扁平條目），譯文比原文少行，
     * 所以 {@code out} 比原文短。之後「同一段不能翻一半」要把某一段退回原文時，
     * 拿的卻是<b>原文的行號</b>去改 {@code out}——前面少了幾行，行號就對不上：
     * 運氣好改錯一行，運氣不好直接超出範圍。
     */
    private static void skillPointTooltip() {
        // 出貨的語料裡「You cannot change your」還是空的，所以那一段兩行都沒翻、不必拉平，
        // 例外躲過去了。但它就躺在 misc.json 等人填——填了第一行、第二行
        // 「skill points at the moment」沒人收，這一段就成了「一半中文」，每個玩家都會踩到。
        // 這裡疊一層只補那一行，把那個狀態釘住。
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
                // 行數不能比原文多：少是整句收短，多就表示同一行被畫了兩次
                check(label + "：行數沒有比原文多（原文 " + tip.size() + "、實際 " + out.size() + "）",
                      out.size() <= tip.size());
                check(label + "：說明那一整句照樣是中文",
                      shown.toString().contains("這項屬性每加一點"));
                if (locked) {
                    // 半句中文接半句英文要整段退回英文，而且退的是<b>這兩行</b>，不是被行號
                    // 錯位之後的別行
                    check(label + "：只翻到一半的那一段整段留英文",
                          shown.toString().contains("You cannot change your")
                                  && shown.toString().contains("skill points at the moment")
                                  && !shown.toString().contains("你目前無法更改你的"));
                }
            }
        }
        // 就地取代那條路：出了任何事都要回傳空的（= 原文不動），不能把例外往外丟
        List<net.minecraft.network.chat.Component> broken = new java.util.ArrayList<>();
        broken.add(null);
        try {
            check("就地取代遇到壞掉的 tooltip 回傳空的（= 原文不動）",
                  TooltipPanel.translateInPlace(broken, store).isEmpty());
        } catch (RuntimeException e) {
            check("就地取代遇到壞掉的 tooltip 不丟例外（實際 " + e + "）", false);
        }
    }

    /** 見 {@link #skillPointTooltip}：layout-debug 裡 Agility 那一份的形狀。 */
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
        rows.add(text("", icon));                          // 分隔線
        rows.add(text("Each point in this skill will", grey));
        rows.add(row(text(skill[1] + " ", pink), text(skill[2], grey)));
        rows.add(row(text(skill[3], grey), text("", icon), text(" " + skill[4], grey),
                     text(skill[5], grey)));
        rows.add(text("you may inflict", grey));
        rows.add(text("", icon));                          // 分隔線
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
