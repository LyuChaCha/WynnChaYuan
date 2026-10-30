package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ChatAlignTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;
    private static final int PINK = 0xFF55FF;
    private static final int GREY = 0xAAAAAA;

    private static final int[] LEADS = {96, 40, 8};

    private static StyledText quest() {
        MutableComponent all = Component.empty();
        all.append(offset(LEADS[0]));
        all.append(lit("[Quest Completed]", GOLD));
        all.append(lit("\n", GREY));
        all.append(offset(LEADS[1]));
        all.append(lit("Rewards:", PINK));
        all.append(lit("\n", GREY));
        all.append(offset(LEADS[2]));
        all.append(lit("- +Access to the Province of Wynn", GREY));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-chat-align");
        FlowedDebug.init(dir);

        String src = "{#}[Quest Completed]\n{#}Rewards:\n{#}- +Access to the Province of Wynn";
        String dst = "{#}[任務完成]\n{#}獎勵:\n{#}- +取得進入 Wynn 行省的資格";
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty(src, dst);
        Files.writeString(dir.resolve("quest.json"), root.toString(), StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Component hit = LineTranslator.translateChat(quest(), store);
        check("整塊查得到譯文", hit != null);
        if (hit == null) {
            report();
            return;
        }
        System.out.println("      輸出：" + hit.getString().replace("\n", " ⏎ "));
        check("三行都還在", rows(hit) == 3);
        check("內容有翻出來", hit.getString().contains("任務完成")
                && hit.getString().contains("獎勵"));

        int[] made = leads(hit);
        for (int i = 0; i < LEADS.length && i < made.length; i++) {
            check("第 " + i + " 行的縮排跟原文一樣（原文 " + LEADS[i]
                          + "、譯文 " + made[i] + "）", made[i] == LEADS[i]);
        }
        check("量到三行的縮排（實際 " + made.length + " 行）", made.length == 3);

        MutableComponent padded = Component.empty();
        padded.append(lit("  ", GREY));
        padded.append(lit("\n", GREY));
        padded.append(quest().getComponent().copy());
        padded.append(lit("\n", GREY));
        Component trimmed = LineTranslator.translateChat(
                StyledText.fromComponent(padded), store);
        check("原文首行只有空白時仍然翻得出來", trimmed != null);
        if (trimmed != null) {
            int[] kept = leads(trimmed);
            check("三行的縮排仍然跟原文一樣（實際 "
                          + java.util.Arrays.toString(kept) + "）",
                  kept.length >= 3 && kept[0] == LEADS[0]
                          && kept[1] == LEADS[1] && kept[2] == LEADS[2]);
        }

        Component centred = LineTranslator.translateChat(quest(), store, Boolean.TRUE);
        check("指定置中時仍翻得出來", centred != null);
        check("指定置中時行數不變", centred == null || rows(centred) == 3);

        columns();
        panelSingles();
        vibrantColumns();
        tooltipColumns();
        try {
            lootrunSummary();
            singleRowCentre();
        } finally {
            LineTranslator.measureForTest = null;
        }
        report();
    }

    private static final int AQUA = 0x55FFFF;

    private static final int LEFT_CENTRE = 81;
    private static final int RIGHT_CENTRE = 234;

    private static int advance(int cp) {
        if (cp >= 0x2E80) {
            return 9;
        }
        return switch (cp) {
            case 'i', '.', ':', ',', ';', '!', '\'' -> 2;
            case 'l' -> 3;
            case 't', 'I', ' ' -> 4;
            case 'f', 'k' -> 5;
            default -> 6;
        };
    }

    private static int measure(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                w[0] += SpaceOffset.decode(text);
            } else {
                text.codePoints().forEach(cp -> w[0] += cp == '\n' ? 0 : advance(cp));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }

    private static StyledText lootrunRow(String num, String rest, String label, String value) {
        int left = measure(Component.literal(num + rest));
        int right = measure(Component.literal(label + value));
        int lead = LEFT_CENTRE - left / 2;
        int gap = RIGHT_CENTRE - right / 2 - (lead + left);
        MutableComponent all = Component.empty();
        all.append(offset(lead));
        all.append(lit(num, AQUA));
        all.append(lit(rest, GREY));
        all.append(offset(gap));
        all.append(lit(label, GREY));
        all.append(lit(value, AQUA));
        return StyledText.fromComponent(all);
    }

    private static List<int[]> cells(Component line) {
        List<int[]> out = new ArrayList<>();
        int x = 0;
        int[] open = null;
        for (Component part : flatten(line)) {
            if (!part.getSiblings().isEmpty() || part.getString().isEmpty()) {
                continue;
            }
            String text = part.getString();
            if (SpaceOffset.isSpaceFont(part.getStyle()) && SpaceOffset.isOffsetRun(text)) {
                x += SpaceOffset.decode(text);
                open = null;
                continue;
            }
            int w = measure(part);
            if (open == null) {
                open = new int[] {x, x + w};
                out.add(open);
            } else {
                open[1] = x + w;
            }
            x += w;
        }
        return out;
    }

    private static void lootrunSummary() {
        LineTranslator.measureForTest = ChatAlignTest::measure;
        String[][] rows = {
            {"46", " Reward Pulls", "Time Elapsed: ", "05:00"},
            {"3", " Reward Rerolls", "Mobs Killed: ", "104"},
            {"0", " Reward Sacrifices", "Chests Open: ", "5"},
            {"300", " Lootrun Experience", "Challenges Completed: ", "5"},
        };
        for (String lang : new String[] {"zh_cn", "zh_tw", "ru_ru", "ja_jp", "partial"}) {
            TranslationStore store = new TranslationStore();
            if (lang.equals("partial")) {
                try {
                    Path dir = Files.createTempDirectory("wynnchayuan-lootrun-partial");
                    Files.writeString(dir.resolve("gui.json"),
                            "{\"Reward Sacrifices\": \"奖励舍弃\", \"Chests Open:\": \"开启宝箱数:\","
                            + " \"Mobs Killed:\": \"击杀怪物数:\", \"Reward Rerolls\": \"奖励重抽\"}",
                            StandardCharsets.UTF_8);
                    store.loadAll(dir);
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
            }
            for (String[] r : rows) {
                StyledText orig = lootrunRow(r[0], r[1], r[2], r[3]);
                Component made = LineTranslator.translateChat(orig, store);
                Component fallback = made != null ? null : LineTranslator.translate(orig, store);
                Component shown = made != null ? made : fallback;
                if (shown != null) {
                    String what = "[" + lang + "] " + r[1].strip();
                    List<int[]> was = cells(orig.getComponent());
                    List<int[]> now = cells(shown);
                    check(what + "：還是兩欄（實際 " + now.size() + "）", now.size() == 2);
                    if (now.size() == 2) {
                        boolean wider = now.get(1)[1] - now.get(1)[0] > was.get(1)[1] - was.get(1)[0];
                        check(what + "：左欄中心跟英文一樣（" + describe(now) + "）",
                              Math.abs(centre(now.get(0)) - LEFT_CENTRE) <= 1);
                        check(what + "：右欄中心跟英文一樣（" + describe(now) + "）",
                              wider || Math.abs(centre(now.get(1)) - RIGHT_CENTRE) <= 1);
                        check(what + "：兩欄沒有黏在一起（" + describe(now) + "）",
                              now.get(1)[0] - now.get(0)[1] >= 4);
                    }
                    check(what + "：★ 整行不比原文寬（原文 " + measure(orig.getComponent())
                                  + "、譯文 " + measure(shown) + "）",
                          measure(shown) <= measure(orig.getComponent()));
                }
                if (lang.equals("zh_cn") && made != null) {
                    Component stacked = LineTranslator.translateChat(orig, store, Boolean.FALSE, true);
                    check("[zh_cn 整塊] " + r[1].strip() + "：欄位置跟單則一樣",
                          stacked != null && describe(cells(stacked)).equals(describe(cells(made))));
                }
                System.out.println("      [" + lang + "] " + r[1].strip() + " 原文欄 "
                        + describe(cells(orig.getComponent())) + " 寬 "
                        + measure(orig.getComponent())
                        + (shown == null ? "  （沒翻）"
                           : "  " + (made != null ? "chat" : "tooltip") + " 譯文欄 "
                             + describe(cells(shown)) + " 寬 " + measure(shown)
                             + " 「" + shown.getString().replaceAll("[\\x{CF000}-\\x{D1000}]", "|") + "」"));
            }
        }
    }

    private static int centre(int[] cell) {
        return (cell[0] + cell[1]) / 2;
    }

    private static String describe(List<int[]> cells) {
        StringBuilder sb = new StringBuilder();
        for (int[] c : cells) {
            sb.append('[').append(c[0]).append('-').append(c[1])
              .append(" 中心 ").append((c[0] + c[1]) / 2).append(']');
        }
        return sb.toString();
    }

    private static void vibrantColumns() {
        java.util.Map<String, Integer> px = java.util.Map.of(
                "Vibrant Dark Grey Beacon", 155, "Vibrant Rainbow Beacon", 140,
                "Vibrant Crimson Beacon", 150, "Vibrant Aqua Beacon", 128,
                "璀璨深灰信標", 57, "璀璨彩虹信標", 57, "璀璨緋紅信標", 57, "璀璨水藍信標", 57,
                "- +1 ", 22, "🔒Unidentified Helmet", 120);
        java.util.function.ToIntFunction<SpaceRuns.Run> width =
                r -> r.space() ? 0 : px.getOrDefault(r.text(), 0);

        List<SpaceRuns.Run> darkOrig = List.of(gap(0), word("Vibrant Dark Grey Beacon"),
                                                    gap(7), word("Vibrant Rainbow Beacon"));
        List<SpaceRuns.Run> darkMade = List.of(gap(0), word("璀璨深灰信標"),
                                                    gap(7), word("璀璨彩虹信標"));
        centresKept("縮排 0、欄距 7", darkOrig, darkMade, width);

        List<SpaceRuns.Run> crimsonOrig = List.of(gap(7), word("Vibrant Crimson Beacon"),
                                                       gap(23), word("Vibrant Aqua Beacon"));
        List<SpaceRuns.Run> crimsonMade = List.of(gap(7), word("璀璨緋紅信標"),
                                                       gap(23), word("璀璨水藍信標"));
        centresKept("縮排 7、欄距 23", crimsonOrig, crimsonMade, width);

        List<SpaceRuns.Run> helmet = List.of(word("- +1 "), gap(2),
                                                  word("🔒Unidentified Helmet"));
        int gaps = 0;
        for (boolean g : SpaceRuns.chatGaps(helmet)) {
            gaps += g ? 1 : 0;
        }
        check("圖示前的 2px 微調不算欄界（實際 " + gaps + " 個）", gaps == 0);
    }

    private static final String ICON = new String(Character.toChars(0xE000));

    private static void tooltipColumns() {
        List<SpaceRuns.Run> classType = List.of(
                word(ICON + " Class Type"), gap(4), word("Mage/Dark Wizard"));
        check("職業類型那一行認得 4px 的欄界", gapCount(classType) == 1);

        List<SpaceRuns.Run> made = List.of(
                word(ICON + " 職業類型"), gap(4), word("法師/闇導士"));
        check("譯文那一行也認得（兩邊數量要一樣才會補償）", gapCount(made) == 1);
        check("補償看得到標籤", SpaceRuns.labelledRun(made, 0));

        List<SpaceRuns.Run> helmet = List.of(word("- +1 "), gap(2),
                                                  word(ICON + "Unidentified Helmet"));
        check("圖示前的 2px 微調不算欄界", gapCount(helmet) == 0);

        List<SpaceRuns.Run> reward = List.of(gap(4), word("Rewards:"));
        check("行首的縮排不算欄界", gapCount(reward) == 0);

        List<SpaceRuns.Run> overlay = List.of(word("Bonder"), gap(-4), word(ICON));
        check("往回拉的負偏移不算欄界", gapCount(overlay) == 0);
    }

    private static int gapCount(List<SpaceRuns.Run> runs) {
        int n = 0;
        for (boolean g : SpaceRuns.tooltipGaps(runs)) {
            n += g ? 1 : 0;
        }
        return n;
    }

    private static void centresKept(String what, List<SpaceRuns.Run> orig,
                                    List<SpaceRuns.Run> made,
                                    java.util.function.ToIntFunction<SpaceRuns.Run> width) {
        int[] adjust = LineTranslator.chatColumnPad(orig, made, width);
        check(what + "：認得兩個欄界（實際 " + adjust.length + " 個）", adjust.length == 2);
        List<Integer> before = centres(orig, new int[adjust.length], width);
        List<Integer> after = centres(made, adjust, width);
        boolean kept = before.size() == 2 && after.size() == 2;
        for (int i = 0; kept && i < 2; i++) {
            kept = Math.abs(before.get(i) - after.get(i)) <= 1;
        }
        check(what + "：兩欄的中心不動（原文 " + before + "、譯文 " + after + "）", kept);
    }

    private static List<Integer> centres(List<SpaceRuns.Run> row, int[] adjust,
                                         java.util.function.ToIntFunction<SpaceRuns.Run> width) {
        boolean[] gaps = SpaceRuns.chatGaps(row);
        List<Integer> out = new ArrayList<>();
        int pos = 0;
        int index = 0;
        for (int i = 0; i < row.size(); i++) {
            SpaceRuns.Run r = row.get(i);
            if (r.space()) {
                pos += r.px() + (gaps[i] && index < adjust.length ? adjust[index++] : 0);
                continue;
            }
            int w = width.applyAsInt(r);
            out.add(pos + w / 2);
            pos += w;
        }
        return out;
    }

    private static SpaceRuns.Run gap(int px) {
        return new SpaceRuns.Run(true, px, Style.EMPTY, SpaceOffset.encode(px));
    }

    private static SpaceRuns.Run word(String text) {
        return new SpaceRuns.Run(false, 0, Style.EMPTY, text);
    }

    private static void columns() {
        int[] wide = LineTranslator.columnDrift(
                List.of(0, 40), List.of(0, 64), new int[] {10, 30});
        check("譯文變寬時欄距收窄（實際 " + wide[1] + "）", wide[1] == -24);

        int[] stacked = LineTranslator.columnDrift(
                List.of(0, 90, 20, 20), List.of(0, 40, 20, 20),
                new int[] {33, -23, 40});
        check("負間隔不動（實際 " + stacked[1] + "）", stacked[1] == 0);
        check("多欄不遞延差額（實際 " + stacked[2] + "）", stacked[2] == 0);

        int[] two = LineTranslator.columnDrift(
                List.of(0, 90, 82), List.of(0, 40, 40), new int[] {34, 69});
        check("第一欄吸收自己縮水的一半（實際 " + two[0] + "）", two[0] == 25);
        check("間隔吸收左右各一半（實際 " + two[1] + "）", two[1] == 46);

        int[] widths = {90, 82};
        int[] made = {40, 40};
        int[] gaps = {34, 69};
        int origPos = 0;
        int madePos = 0;
        for (int i = 0; i < 2; i++) {
            origPos += gaps[i];
            madePos += gaps[i] + two[i];
            int origCentre = origPos + widths[i] / 2;
            int madeCentre = madePos + made[i] / 2;
            check("第 " + (i + 1) + " 欄的中心不動（原文 " + origCentre
                          + "、譯文 " + madeCentre + "）",
                  Math.abs(origCentre - madeCentre) <= 1);
            origPos += widths[i];
            madePos += made[i];
        }

        int[] one = LineTranslator.columnDrift(
                List.of(0, 107), List.of(0, 38), new int[] {101});
        check("單欄的行不動（實際 " + java.util.Arrays.toString(one) + "）",
              one.length == 1 && one[0] == 0);
    }

    private static void singleRowCentre() throws Exception {
        System.out.println("=== 單獨一則的置中行 ===");
        LineTranslator.measureForTest = ChatAlignTest::measure;
        Path dir = Files.createTempDirectory("wynnchayuan-chat-centre");
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("{#}Challenge Completed", "{#}挑戰完成");
        root.addProperty("{#}Next beacons will appear soon!", "{#}下一批信標即將出現！");
        root.addProperty("{#}[+{~} Reward Pull]", "{#}[+{~} 獎勵抽取次數]");
        root.addProperty("{#}Mission Started", "{#}使命開始");
        root.addProperty("{#}- +Access to the Province of Wynn", "{#}- +取得進入 Wynn 行省的資格");
        Files.writeString(dir.resolve("misc.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        for (String text : new String[] {"Challenge Completed", "Next beacons will appear soon!",
                "[+1 Reward Pull]", "Mission Started"}) {
            int body = measure(Component.literal(text));
            int lead = (309 - body) / 2 - (text.startsWith("Mission") ? 6 : 0);
            MutableComponent row = Component.empty();
            row.append(offset(lead)).append(lit(text, GREY));
            Component hit = LineTranslator.translateChat(StyledText.fromComponent(row), store);
            check(text + "：查得到譯文", hit != null);
            if (hit == null) {
                continue;
            }
            int madeLead = leads(hit)[0];
            int madeBody = measure(hit) - madeLead;
            int centreX2 = 2 * madeLead + madeBody;
            check("★ " + text + "：譯文的中心跟原文同一個位置（原文縮排 " + lead + "、內容 " + body
                          + "；譯文縮排 " + madeLead + "、內容 " + madeBody + "）",
                  Math.abs(centreX2 - (2 * lead + body)) <= 2 && madeLead != lead);
        }

        String item = "- +Access to the Province of Wynn";
        MutableComponent left = Component.empty();
        left.append(offset(8)).append(lit(item, GREY));
        Component kept = LineTranslator.translateChat(StyledText.fromComponent(left), store);
        check("靠左的清單列查得到譯文", kept != null);
        if (kept != null) {
            check("★ 靠左的清單列縮排照舊（實際 " + leads(kept)[0] + "）", leads(kept)[0] == 8);
        }

        String full = "- +Access to the Province of Wynn";
        int pad = (309 - 2 * 8 - measure(Component.literal(full))) / measure(Component.literal("x"));
        check("前提：湊得出一列寬到中心落在置中線上的靠左句子", pad > 0);
        String wide = full + "x".repeat(Math.max(0, pad));
        com.google.gson.JsonObject more = new com.google.gson.JsonObject();
        more.addProperty("{#}" + wide, "{#}- +取得進入 Wynn 行省的資格");
        Path dir2 = Files.createTempDirectory("wynnchayuan-chat-centre-wide");
        Files.writeString(dir2.resolve("misc.json"), more.toString(), StandardCharsets.UTF_8);
        TranslationStore store2 = new TranslationStore();
        store2.loadAll(dir2);
        MutableComponent row = Component.empty();
        row.append(offset(8)).append(lit(wide, GREY));
        Component same = LineTranslator.translateChat(StyledText.fromComponent(row), store2);
        check("寫滿一行的句子查得到譯文", same != null);
        if (same != null) {
            check("★ 寫滿一行的靠左句子縮排照舊（實際 " + leads(same)[0] + "）",
                  leads(same)[0] == 8);
        }
    }

    private static void panelSingles() {
        int origLead = 105;
        int origBody = 98;
        int madeBody = 54;
        int want = origLead + (origBody - madeBody) / 2;
        check("單欄行的中心要跟原文一樣（該縮排 " + want + "）", want == 127);

        int[] two = LineTranslator.columnDrift(
                List.of(0, 90, 82), List.of(0, 40, 40), new int[] {34, 69});
        check("兩欄的行仍然逐欄置中（實際 " + two[0] + "/" + two[1] + "）",
              two[0] == 25 && two[1] == 46);
    }

    private static int rows(Component line) {
        return (int) line.getString().chars().filter(c -> c == '\n').count() + 1;
    }

    private static int[] leads(Component line) {
        List<Integer> out = new ArrayList<>();
        int px = 0;
        boolean counting = true;
        for (Component part : flatten(line)) {
            if (!part.getSiblings().isEmpty()) {
                continue;
            }
            String text = part.getString();
            if (text.isEmpty()) {
                continue;
            }
            if (text.indexOf('\n') >= 0) {
                out.add(px);
                px = 0;
                counting = true;
                continue;
            }
            if (!counting) {
                continue;
            }
            if (SpaceOffset.isOffsetRun(text)) {
                px += SpaceOffset.decode(text);
            } else {
                counting = false;
            }
        }
        out.add(px);
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = out.get(i);
        }
        return arr;
    }

    private static List<Component> flatten(Component root) {
        List<Component> out = new ArrayList<>();
        out.add(root);
        for (Component child : root.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("聊天對齊：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("聊天對齊：全部通過");
    }
}
