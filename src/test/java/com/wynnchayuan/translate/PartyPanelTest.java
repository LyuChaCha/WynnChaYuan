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

        lootrunBlocks(store);

        System.out.println(failures == 0
                ? "面板整段：全部通過" : "面板整段：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * Lootrun 的信標與挑戰說明也是照寬度切開的整段。
     *
     * <p>跟排隊面板同一個道理，只是切得更碎（三到四行）。這些段落在
     * {@code misc.json} / {@code lootrun.json} 裡本來就躺著一批<b>逐行</b>的
     * 空條目——那是照 capture 補出來的形狀。逐行填下去就會半中半英，
     * 所以整段收一條、逐行那些留空（空的不會載入，不會蓋掉整段）。
     */
    private static void lootrunBlocks(TranslationStore store) {
        block(store, "詛咒加傷", new String[] {
                "For the rest of this Lootrun,",
                "gain +100% Damage (Max x5)",
                "everytime you get a Curse"}, "詛咒");
        // 這一段最後一行帶著材質包圖示（「gain +{~} {#}Defence」），
        // 而 Component.literal 造不出真的圖示字元——照上面那樣畫一次會假失敗。
        // 改成直接問模板，至少釘住鍵的形狀沒被改壞。
        String beacon = "Once you have been offered a\nBlue or Purple Beacon more\n"
                + "than {~} times this Lootrun,\ngain +{~} {#}Defence";
        report("信標加防：模板查得到（實際 " + store.lookup(beacon) + "）",
                store.lookup(beacon) != null && store.lookup(beacon).contains("信標"));
        // 詞用 GLOSSARY.md 的「寶物品質」。這條測試就是拿來擋詞表漂移的——
        // 語料裡先前兩種譯法並存過（寶物品質／戰利品品質），統一之後這裡
        // 一起釘住，下次再分岔會馬上失敗。
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
                "Challenges."}, "抽數");
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

    /**
     * Lootrun 跑完之後那幾面：結算統計、獎勵寶箱、信標次數。
     *
     * <h2>為什麼單獨列一塊</h2>
     * 這幾面只有<b>跑完一整趟</b>才看得到，所以一直沒人收。實機 capture 一次
     * 就吐出四十幾條缺口，形狀分成三種：面板標題（{@code Yellow Beacon}）、
     * 統計列（{@code Offered: {~}}）、照寬度切碎的說明（三到六行）。
     * 三種各釘一條，下次改動哪一種都會被抓到。
     */
    private static void lootrunEnd(TranslationStore store) {
        // 信標名稱：雙欄的那些早就翻好了（「{#}黃色信標{#}藍色信標」），
        // 單獨當標題出現的那一份卻一直是空的。顏色詞要跟雙欄那份一致。
        row(store, "Yellow Beacon", "黃色信標");
        row(store, "Dark Grey Beacon", "深灰信標");
        row(store, "Crimson Beacon", "緋紅信標");
        row(store, "Obscured Beacon", "晦暗信標");
        // 統計列
        row(store, "Offered: {~}", "提供次數");
        row(store, "Chosen: {~}/{~}", "選取次數");
        row(store, "Decay: {~} Challenges", "衰減");
        row(store, "Choices: +{~}", "信標選項");
        // 使命名稱照團隊的語調——兩個字的意象詞（救贖、停滯、天賜、機緣）
        row(store, "Knife Edge", "刀鋒");
        row(store, "Thrill Seeker", "逐險者");

        block(store, "詛咒減半", new String[] {
                "Curses are now half as",
                "effective."}, "詛咒");
        block(store, "逐險者說明", new String[] {
                "Gain +1 Pull per Red Beacon",
                "Challenge completed. This",
                "amount is increased by +1 per",
                "3 challenges completed (max",
                "+5). This boost is reset upon",
                "taking a Green Beacon."}, "紅色信標");
        block(store, "捨棄獎勵說明", new String[] {
                "Sacrificing your rewards",
                "will add a percentage of your",
                "pulls into your next lootrun"}, "捨棄");
        block(store, "捨棄越多保留越多", new String[] {
                "A higher amount of sacrifices",
                "increases the amount of pulls",
                "saved for your next run"}, "抽數");
        block(store, "關閉寶箱", new String[] {
                "By closing your chest",
                "inside of it will be lost"}, "寶箱");
    }

    /** 單獨一行的條目：查得到，而且用的是講好的詞。 */
    private static void row(TranslationStore store, String src, String want) {
        String zh = LineTranslator.lookup(src, store, false);
        report("「" + src + "」→「" + want + "」（實際 " + zh + "）",
                zh != null && zh.contains(want));
    }

    /** 整段要翻出來，而且不能剩下英文的原句。 */
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
