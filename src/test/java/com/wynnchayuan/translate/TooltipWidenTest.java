package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class TooltipWidenTest {

    private static int failures = 0;

    private static final int CHAR = 6;

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
        skillPoints();
        noFont();

        System.out.println(failures == 0
                ? "寬譯文撐開：全部通過" : "寬譯文撐開：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static final int EDGE = 140;

    private static void statBlock() {
        List<Component> orig = new ArrayList<>();
        List<Component> made = new ArrayList<>();

        orig.add(line("Waist Apron"));
        made.add(line("Waist Apron"));
        orig.add(line(""));
        made.add(line(""));
        orig.add(row("Health Regen", "+20%"));
        made.add(shrunk("Health Regen", "Регенерация здоровья", "+20%"));
        orig.add(row("Mana Regen", "+5/5s"));
        made.add(shrunk("Mana Regen", "Регенерация маны", "+5/5s"));
        orig.add(row("Walk Speed", "-12%"));
        made.add(shrunk("Walk Speed", "Скорость ходьбы", "-12%"));
        orig.add(row("Loot Bonus", "+12%"));
        made.add(row("Loot Bonus", "+12%"));
        orig.add(parts("", EDGE - 6 - 18, "+30"));
        made.add(parts("", EDGE - 6 - 18, "+30"));
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

        List<Component> left = TooltipWiden.fit(orig, made, centred, true, WIDTH);
        report("第二欄靠左時一行都不動", left == made);
    }

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
        orig.add(parts("Health", frame - 36 - 18, "+24"));
        made.add(parts("Здоровье", frame - 48 - 18, "+24"));
        for (int k = 0; k < en.length; k++) {
            int enW = CHAR * en[k].length();
            int ruW = CHAR * ru[k].length();
            int enLead = (frame - enW) / 2;
            orig.add(indented(enLead, en[k]));
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

    private static void skillPoints() {
        int left = 40;
        int right = 160;
        int arrows = 90;
        List<Component> orig = new ArrayList<>();
        List<Component> made = new ArrayList<>();
        orig.add(centredRow(new String[] {"Current", ">>>>>>", "Next"},
                            new int[] {left, arrows + 18, right}, false));
        made.add(startRow(new String[] {"目前", ">>>>>>", "下一級"},
                          starts(orig.get(0))));
        orig.add(centredRow(new String[] {"48.8%", "49.2%"}, new int[] {left, right}, false));
        made.add(orig.get(1));
        orig.add(centredRow(new String[] {"60 points", "61 points"},
                            new int[] {left, right}, false));
        made.add(startRow(new String[] {"60 點", "61 點"}, starts(orig.get(2))));
        boolean[] centred = new boolean[orig.size()];

        List<Component> out = TooltipWiden.fit(orig, made, centred, false, WIDTH);

        int[] head = centres(out.get(0), 3);
        int[] pts = centres(out.get(2), 2);
        report("★ 「目前」與「60 點」照原文的中心 " + left + " 擺（實際 "
                        + head[0] + "、" + pts[0] + "）",
                Math.abs(head[0] - left) <= 1 && Math.abs(pts[0] - left) <= 1);
        report("★ 「下一級」與「61 點」照原文的中心 " + right + " 擺（實際 "
                        + head[2] + "、" + pts[1] + "）",
                Math.abs(head[2] - right) <= 1 && Math.abs(pts[1] - right) <= 1);
        report("沒翻的百分比那一行沒動", out.get(1) == made.get(1));

        List<Component> origLong = new ArrayList<>(orig);
        origLong.add(line("Each point in this skill will"));
        origLong.add(line("increase all damage you deal"));
        origLong.add(line("and your Earth damage"));
        List<Component> madeShort = new ArrayList<>(made);
        madeShort.add(line("這項屬性每加一點，都會提高"));
        madeShort.add(line("你造成的所有傷害與地屬性傷害"));
        List<Component> out2 = TooltipWiden.fit(origLong, madeShort,
                new boolean[origLong.size()], false, WIDTH);
        int[] pts2 = centres(out2.get(2), 2);
        report("★ 說明少一行時，「60 點」「61 點」照樣擺回中心（實際 "
                        + pts2[0] + "、" + pts2[1] + "）",
                Math.abs(pts2[0] - left) <= 1 && Math.abs(pts2[1] - right) <= 1);
        report("說明那兩行沒動", out2.get(3) == madeShort.get(3) && out2.get(4) == madeShort.get(4));
    }

    private static Component centredRow(String[] texts, int[] centre, boolean unused) {
        int[] start = new int[texts.length];
        for (int k = 0; k < texts.length; k++) {
            start[k] = centre[k] - CHAR * texts[k].length() / 2;
        }
        return startRow(texts, start);
    }

    private static Component startRow(String[] texts, int[] start) {
        MutableComponent out = Component.empty();
        int x = 0;
        for (int k = 0; k < texts.length; k++) {
            out.append(offset(start[k] - x));
            out.append(Component.literal(texts[k]));
            x = start[k] + CHAR * texts[k].length();
        }
        return out;
    }

    private static int[] starts(Component row) {
        List<Integer> s = new ArrayList<>();
        int[] x = {0};
        boolean[] inText = {false};
        row.visit((style, text) -> {
            if (text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                x[0] += SpaceOffset.decode(text);
                inText[0] = false;
            } else {
                if (!inText[0]) {
                    s.add(x[0]);
                }
                inText[0] = true;
                x[0] += text.codePointCount(0, text.length()) * CHAR;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] centres(Component row, int count) {
        int[] out = new int[count];
        int[] x = {0};
        int[] k = {-1};
        int[] w = new int[count];
        int[] st = new int[count];
        boolean[] inText = {false};
        row.visit((style, text) -> {
            if (text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                x[0] += SpaceOffset.decode(text);
                inText[0] = false;
            } else {
                int tw = text.codePointCount(0, text.length()) * CHAR;
                if (!inText[0]) {
                    k[0]++;
                    if (k[0] < count) {
                        st[k[0]] = x[0];
                    }
                }
                if (k[0] < count) {
                    w[k[0]] += tw;
                }
                inText[0] = true;
                x[0] += tw;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        for (int i = 0; i < count; i++) {
            out[i] = st[i] + w[i] / 2;
        }
        return out;
    }

    private static void noFont() {
        List<Component> orig = List.of(row("Health Regen", "+20%"));
        List<Component> made = List.of(shrunk("Health Regen", "Регенерация здоровья", "+20%"));
        report("量不到寬度時原封不動",
                TooltipWiden.fit(orig, made, new boolean[1], false, c -> 0) == made);
    }

    private static Component line(String text) {
        return Component.empty().append(Component.literal(text));
    }

    private static Component row(String label, String value) {
        return parts(label, EDGE - CHAR * label.length() - CHAR * value.length(), value);
    }

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
