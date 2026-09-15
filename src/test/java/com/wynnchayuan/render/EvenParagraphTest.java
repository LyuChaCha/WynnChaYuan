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

        keyTooltip();

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
