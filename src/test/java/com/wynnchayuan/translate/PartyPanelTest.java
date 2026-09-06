package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 排隊找隊伍那一面板整片都要是中文。
 *
 * <h2>實機回報</h2>
 * 面板上九行只有兩行翻出來，其餘全是英文：
 *
 * <pre>
 *   Looking for a party        For 烽火王宮      ← 只有副本名被詞表換掉
 *   Status:                    - Time estimate: 快速
 *   - Region: AS, NA           (keep 1 Ek Rune in your ...
 * </pre>
 *
 * <p>討伐戰的排隊面板早就收在 {@code raid.json} 了，但找隊伍那一份是<b>另一組
 * 字串</b>：{@code Time estimate} 的 e 是小寫、地區是「AS, NA」兩個一起、
 * 底下還多了三行提示。一個字不一樣就整條對不到。
 *
 * <h2>這條測試在盯什麼</h2>
 * 那三行提示是 tooltip 照寬度切開的<b>一句話</b>，語料收的是接起來的整句。
 * 逐行去問永遠問不到，所以整段那條路一斷，畫面上就會剩三行英文——
 * 而且逐行補回去只會補出半中半英（見 {@code GuiBlockGapTest}）。
 * 這裡把「接得起來」釘住。
 */
public final class PartyPanelTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // 實機那一份，照畫面上的行序
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

        // ★ 一個英文字母都不該剩。副本名（烽火王宮）與符文名（Ek Rune）除外——
        //   符文名照團隊慣例留原文，所以只挑幾個必須消失的字來問。
        for (String english : new String[] {"Looking for a party", "Status:",
                                            "Region:", "Time estimate:",
                                            "keep", "inventory", "faster",
                                            "Click to leave the queue"}) {
            report("「" + english + "」不該再出現", !zh.contains(english));
        }

        // ★ 那三行提示是接起來查的，隨便挑一行單獨問要查不到——
        //   查得到就代表有人補了逐行條目，那會蓋掉整段那條路。
        report("沒有逐行條目蓋掉整段",
                LineTranslator.lookup("inventory to find a party", store, false) == null);

        System.out.println(failures == 0
                ? "排隊面板：全部通過" : "排隊面板：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
