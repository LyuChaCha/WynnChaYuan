package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 譯文比英文寬時，整份 tooltip 要一起撐開。
 *
 * <h2>實機回報</h2>
 * 俄文是第一個<b>比英文長</b>的語言。逐行對齊只為「譯文比較短」設計過，
 * line-debug 記到的是：
 *
 * <pre>
 *   Health Regen  間隔 34 -&gt; 6   整行 165 -&gt; 196   ← 凸出去
 *   Mana Regen    間隔 65 -&gt; 29  整行 207 -&gt; 207   ← 守住
 *   an Item Identifier can unlock  整行 172 -&gt; 243，縮排 13 沒動
 * </pre>
 *
 * 放得下的守住舊右緣、放不下的各自凸出，數值欄參差不齊；置中的行則跑出外框。
 *
 * <h2>這條測試怎麼量</h2>
 * 寬度要有字型才量得出來，headless 沒有。所以跟 {@code BlockLayoutTest} 一樣注入
 * 一個假字型：一般字一律 6px，space 字型的偏移照解碼值。原文照遊戲送來的形狀
 * 組（文字片段 + {@code minecraft:space} 的偏移字元），譯文照逐行對齊之後
 * 會長成的樣子組，再交給撐寬那一步。
 */
public final class TooltipWidenTest {

    private static int failures = 0;

    private static final int CHAR = 6;

    /** 假字型：一般字 6px，space 字型底下的偏移照解碼值。 */
    private static final ToIntFunction<Component> WIDTH = c -> {
        int[] w = {0};
        c.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                w[0] += SpaceOffset.decode(text);
            } else {
                w[0] += text.codePointCount(0, text.length()) * CHAR;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return w[0];
    };

    public static void main(String[] args) {
        statBlock();
        centredBlock();
        shorterLanguage();
        shrunkBox();
        noFont();

        System.out.println(failures == 0
                ? "寬譯文撐開：全部通過" : "寬譯文撐開：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 原文的欄位右緣。 */
    private static final int EDGE = 140;

    /**
     * 屬性區塊：其中一行的俄文標籤比英文的間隔還寬。
     *
     * <p>撐開之後每一條靠右的行都要收在同一個 x；圖示列、疊字列、分隔線、
     * 物品名稱都不能動。
     */
    private static void statBlock() {
        List<Component> orig = new ArrayList<>();
        List<Component> made = new ArrayList<>();

        orig.add(line("Waist Apron"));                                    // 0 名稱
        made.add(line("Waist Apron"));
        orig.add(line(""));                                   // 1 分隔線
        made.add(line(""));
        // 2 生命回復：俄文標籤 120px，英文的間隔只剩 44px，放不下
        orig.add(row("Health Regen", "+20%"));
        made.add(shrunk("Health Regen", "Регенерация здоровья", "+20%"));
        // 3 魔力回復：放得下
        orig.add(row("Mana Regen", "+5/5s"));
        made.add(shrunk("Mana Regen", "Регенерация маны", "+5/5s"));
        // 4 走路速度：放得下
        orig.add(row("Walk Speed", "-12%"));
        made.add(shrunk("Walk Speed", "Скорость ходьбы", "-12%"));
        // 5 沒翻到的行：原文的複本，照樣靠右到共同右緣
        orig.add(row("Loot Bonus", "+12%"));
        made.add(row("Loot Bonus", "+12%"));
        // 6 圖示列：間隔前面只有圖示，不是欄位
        orig.add(parts("", EDGE - 6 - 18, "+30"));
        made.add(parts("", EDGE - 6 - 18, "+30"));
        // 7 疊字列：往回退的偏移後面只有星星圖示
        orig.add(parts("Stars", -18, ""));
        made.add(parts("Звёзды", -18, ""));

        boolean[] centred = new boolean[orig.size()];
        report("前提：生命回復那行逐行對齊之後凸出右緣（實際 "
                        + WIDTH.applyAsInt(made.get(2)) + "px，右緣 " + EDGE + "px）",
                WIDTH.applyAsInt(made.get(2)) > EDGE);

        List<Component> out = TooltipWiden.fit(orig, made, centred, false, WIDTH);

        int want = Math.max(EDGE, CHAR * "Регенерация здоровья".length()
                + LineTranslator.MIN_GAP + CHAR * "+20%".length());
        List<Integer> ends = new ArrayList<>();
        for (int i = 2; i <= 5; i++) {
            ends.add(WIDTH.applyAsInt(out.get(i)));
        }
        report("★ 靠右的四行收在同一個右緣 " + want + "px（實際 " + ends + "）",
                ends.equals(List.of(want, want, want, want)));
        report("放不下的那行間隔不小於最小間隔（實際 " + gapOf(out.get(2)) + "px）",
                gapOf(out.get(2)) >= LineTranslator.MIN_GAP);
        report("數值還在（實際「" + out.get(3).getString().replaceAll("[\\x{C0000}-\\x{DFFFF}]", "")
                        + "」）",
                out.get(3).getString().contains("Регенерация маны")
                        && out.get(3).getString().endsWith("+5/5s"));
        report("名稱、分隔線、圖示列、疊字列都沒動",
                out.get(0) == made.get(0) && out.get(1) == made.get(1)
                        && out.get(6) == made.get(6) && out.get(7) == made.get(7));

        // 反面：第二欄靠左的 tooltip（階級特權那種）整份都不能動。
        List<Component> left = TooltipWiden.fit(orig, made, centred, true, WIDTH);
        report("第二欄靠左時一行都不動", left == made);
    }

    /**
     * 未鑑定物品那三行置中的說明，中間那行的俄文比英文寬。
     *
     * <p>逐行那一步把縮排算成負的（-21px）。撐開之後每一行照新的寬度重新置中，
     * 縮排不能是負的；頂著外框右緣的屬性行也要跟著收到新的右緣。
     */
    private static void centredBlock() {
        String[] en = {"This item's power has been sealed,",
                       "an  Item Identifier can unlock",
                       "its potential."};
        String[] ru = {"Сила этого предмета запечатана,",
                       "но  Идентификатор предметов раскроет её",
                       "потенциал."};
        int frame = CHAR * en[0].length();

        List<Component> orig = new ArrayList<>();
        List<Component> made = new ArrayList<>();
        orig.add(line("Waist Apron"));
        made.add(line("Waist Apron"));
        // 頂著外框右緣的屬性行
        orig.add(parts("Health", frame - 36 - 18, "+24"));
        made.add(parts("Здоровье", frame - 48 - 18, "+24"));
        for (int k = 0; k < en.length; k++) {
            int enW = CHAR * en[k].length();
            int ruW = CHAR * ru[k].length();
            int enLead = (frame - enW) / 2;
            orig.add(indented(enLead, en[k]));
            // 逐行那一步：縮排補差額的一半，放不下時就成了負的
            made.add(indented(enLead + (enW - ruW) / 2, ru[k]));
        }
        boolean[] centred = {false, false, true, true, true};

        report("前提：中間那行逐行對齊之後縮排是負的（實際 " + leadOf(made.get(3)) + "px）",
                leadOf(made.get(3)) < 0);

        List<Component> out = TooltipWiden.fit(orig, made, centred, false, WIDTH);

        int wide = 0;
        for (String s : ru) {
            wide = Math.max(wide, CHAR * s.length());
        }
        boolean ok = true;
        StringBuilder seen = new StringBuilder();
        for (int k = 0; k < ru.length; k++) {
            Component c = out.get(2 + k);
            int lead = leadOf(c);
            int content = WIDTH.applyAsInt(c) - lead;
            ok &= lead >= 0 && lead == (wide - content) / 2;
            seen.append(lead).append('/').append(content).append(' ');
        }
        report("★ 三行照新寬度 " + wide + "px 重新置中、縮排不為負（縮排/內容 "
                + seen.toString().strip() + "）", ok);
        report("最寬那行的縮排是 0（實際 " + leadOf(out.get(3)) + "px）", leadOf(out.get(3)) == 0);
        report("頂著外框的屬性行跟著收到 " + wide + "px（實際 "
                        + WIDTH.applyAsInt(out.get(1)) + "px）",
                WIDTH.applyAsInt(out.get(1)) == wide);
        report("名稱沒動", out.get(0) == made.get(0));
    }

    /**
     * 中文：每一行都比英文短，逐行對齊已經把右緣守住了。
     *
     * <p>這一步必須<b>原封不動</b>——連物件都是同一個。
     */
    private static void shorterLanguage() {
        List<Component> orig = new ArrayList<>();
        List<Component> made = new ArrayList<>();
        orig.add(line("Waist Apron"));
        made.add(line("腰間圍裙"));
        orig.add(row("Health Regen", "+20%"));
        made.add(shrunk("Health Regen", "生命回復", "+20%"));
        orig.add(row("Mana Regen", "+5/5s"));
        made.add(shrunk("Mana Regen", "魔力回復", "+5/5s"));

        String[] en = {"This item's power has been sealed,",
                       "an  Item Identifier can unlock",
                       "its potential."};
        String[] zh = {"這件物品的力量被封印了，", " 鑑定師可以解開", "它的潛力。"};
        int frame = CHAR * en[0].length();
        // 頂著外框右緣的屬性行：中文照樣頂著，框不會縮
        orig.add(parts("Health", frame - 36 - 18, "+24"));
        made.add(parts("生命", frame - 12 - 18, "+24"));
        for (int k = 0; k < en.length; k++) {
            int enW = CHAR * en[k].length();
            int zhW = CHAR * zh[k].length();
            int enLead = (frame - enW) / 2;
            orig.add(indented(enLead, en[k]));
            made.add(indented(enLead + (enW - zhW) / 2, zh[k]));
        }
        boolean[] centred = {false, false, false, false, true, true, true};

        List<Component> out = TooltipWiden.fit(orig, made, centred, false, WIDTH);
        boolean same = out == made;
        for (int i = 0; i < made.size() && same; i++) {
            same = out.get(i) == made.get(i);
        }
        report("★ 中文每行都放得下：整份原封不動（同一份清單、同一批物件）", same);
    }

    /**
     * aspect：沒有欄位行撐住外框，全是敘述。中文每行都比英文短，框跟著縮窄，
     * 置中的「四阶 [MAX]」卻還照英文的寬度置中——實機看起來整行偏右。
     *
     * <p>要照縮窄後的寬度（最寬的那行）重新置中；靠左的行不能動。
     */
    private static void shrunkBox() {
        String[] en = {"Tier IV [MAX]", "Increases the damage of Arrow Bomb"};
        String[] zh = {"四阶 [MAX]", "提高箭矢炸弹的伤害"};
        int frame = CHAR * en[1].length();
        List<Component> orig = new ArrayList<>();
        List<Component> made = new ArrayList<>();
        orig.add(indented((frame - CHAR * en[0].length()) / 2, en[0]));
        int enW = CHAR * en[0].length();
        int zhW = CHAR * zh[0].length();
        made.add(indented((frame - enW) / 2 + (enW - zhW) / 2, zh[0]));
        orig.add(line(en[1]));
        made.add(line(zh[1]));
        boolean[] centred = {true, false};

        List<Component> out = TooltipWiden.fit(orig, made, centred, false, WIDTH);
        int box = CHAR * zh[1].length();
        int want = (box - zhW) / 2;
        report("★ 框縮到 " + box + "px 時置中行照新寬度置中（縮排 "
                        + leadOf(made.get(0)) + " -> " + leadOf(out.get(0)) + "，應為 " + want + "）",
                leadOf(out.get(0)) == want);
        report("靠左的行沒動", out.get(1) == made.get(1));
    }

    /** 沒有字型時量不到寬度，什麼都判斷不了，也就什麼都不做。 */
    private static void noFont() {
        List<Component> orig = List.of(row("Health Regen", "+20%"));
        List<Component> made = List.of(shrunk("Health Regen", "Регенерация здоровья", "+20%"));
        report("量不到寬度時原封不動",
                TooltipWiden.fit(orig, made, new boolean[1], false, c -> 0) == made);
    }

    // ------------------------------------------------------------ 組行

    private static Component line(String text) {
        return Component.empty().append(Component.literal(text));
    }

    /** 「標籤 + 偏移 + 數值」，右緣收在 {@link #EDGE}。 */
    private static Component row(String label, String value) {
        return parts(label, EDGE - CHAR * label.length() - CHAR * value.length(), value);
    }

    /**
     * 逐行對齊之後的譯文：間隔補回標籤短掉的部分，放不下時收在最小間隔
     * （跟 {@link LineTranslator#narrowed} 同一套）。
     */
    private static Component shrunk(String en, String translated, String value) {
        int gap = EDGE - CHAR * en.length() - CHAR * value.length();
        int adjusted = gap - (CHAR * translated.length() - CHAR * en.length());
        return parts(translated, LineTranslator.narrowed(gap, adjusted), value);
    }

    private static Component parts(String before, int px, String after) {
        MutableComponent out = Component.empty();
        out.append(Component.literal(before)).append(offset(px)).append(Component.literal(after));
        return out;
    }

    private static Component indented(int px, String text) {
        MutableComponent out = Component.empty();
        out.append(offset(px)).append(Component.literal(text));
        return out;
    }

    private static Component offset(int px) {
        return Component.literal(SpaceOffset.encode(px)).setStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    /** 行首的偏移總寬。 */
    private static int leadOf(Component line) {
        int[] px = {0};
        boolean[] done = {false};
        line.visit((style, text) -> {
            if (done[0] || text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                px[0] += SpaceOffset.decode(text);
            } else {
                done[0] = true;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    /** 文字之間那段偏移的總寬。 */
    private static int gapOf(Component line) {
        int[] px = {0};
        boolean[] text = {false};
        line.visit((style, run) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(run)) {
                if (text[0]) {
                    px[0] += SpaceOffset.decode(run);
                }
            } else if (!run.isEmpty()) {
                text[0] = true;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
