package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 中文、日文的多行聊天訊息要重新斷行，不能照英文的換行位置斷。
 *
 * <h2>實機回報</h2>
 * <pre>
 *   傳送門湧出充滿憎恨的回音。Wynn 正面臨
 *   湮滅。
 * </pre>
 * 英文在 {@code Wynn faces} 後面折行，譯文照抄同一個位置，「正面臨／湮滅」被拆開。
 *
 * <p>假字型：漢字 9px、其他 6px；排版偏移照解碼值。
 */
public final class ChatReflowTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        LineTranslator.measureForTest = c -> {
            int[] w = {0};
            c.visit((style, text) -> {
                if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                    w[0] += SpaceOffset.decode(text);
                } else {
                    text.codePoints().forEach(cp ->
                            w[0] += LineTranslator.isCjkBreakable(cp) || cp > 0x2FFF ? 9 : 6);
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
            return w[0];
        };
        Path dir = Files.createTempDirectory("wynnchayuan-chat-reflow");
        FlowedDebug.init(dir);

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("{#}Hateful echoes erupt from the Portal. Wynn faces\n{#}Annihilation.",
                "{#}傳送門湧出充滿憎恨的回音。Wynn 正面臨\n{#}湮滅。");
        root.addProperty("{#}Prepare to defend the province at the Corruption Portal\n{#}in {~}m {~}s!",
                "{#}準備到腐敗傳送門保衛行省，\n{#}還有 {~} 分 {~} 秒！");
        root.addProperty("{#}Rewards:\n{#}- +{~} XP\n{#}- +{~} Emeralds",
                "{#}獎勵：\n{#}- +{~} 經驗\n{#}- +{~} 綠寶石");
        Files.writeString(dir.resolve("misc.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Component a = LineTranslator.translateChat(two(
                "Hateful echoes erupt from the Portal. Wynn faces", "Annihilation."), store);
        String sa = text(a);
        System.out.println("      輸出：" + sa.replace("\n", " ⏎ "));
        check("「正面臨湮滅」沒有被拆開", sa.contains("正面臨湮滅"));
        check("併成一行（英文最寬那行放得下）", !sa.contains("\n"));

        Component b = LineTranslator.translateChat(two(
                "Prepare to defend the province at the Corruption Portal", "in 43m 5s!"), store);
        String sb = text(b);
        System.out.println("      輸出：" + sb.replace("\n", " ⏎ "));
        check("第三四句併在同一行", sb.contains("行省，還有 43 分 5 秒！") && !sb.contains("\n"));

        // 清單：前兩行很短，不是折行，不能併起來。
        MutableComponent list = Component.empty();
        list.append(offset(8)).append(Component.literal("Rewards:")).append(Component.literal("\n"));
        list.append(offset(8)).append(Component.literal("- +500 XP")).append(Component.literal("\n"));
        list.append(offset(8)).append(Component.literal("- +20 Emeralds"));
        Component c = LineTranslator.translateChat(StyledText.fromComponent(list), store);
        String sc = text(c);
        System.out.println("      輸出：" + sc.replace("\n", " ⏎ "));
        check("清單照原本的三行", sc.split("\n").length == 3);

        // 很長的一段：寬度上限內要斷在標點後面，而且每一行不超過英文最寬那行。
        String longEn = "The trail through the forest leads you to the central city";
        com.google.gson.JsonObject root2 = new com.google.gson.JsonObject();
        root2.addProperty("{#}" + longEn + "\n{#}of the Wynn Province, where many travellers\n{#}gather.",
                "{#}穿過森林的小徑會帶你前往 Wynn 行省的中心城市，許多旅人\n{#}都聚集在那裡，而且這一段譯文故意寫得比原文更長一些，\n{#}好測試斷行。");
        Path dir2 = Files.createTempDirectory("wynnchayuan-chat-reflow2");
        Files.writeString(dir2.resolve("misc.json"), root2.toString(), StandardCharsets.UTF_8);
        TranslationStore store2 = new TranslationStore();
        store2.loadAll(dir2);
        MutableComponent three = Component.empty();
        three.append(offset(8)).append(Component.literal(longEn)).append(Component.literal("\n"));
        three.append(offset(8)).append(Component.literal("of the Wynn Province, where many travellers"))
             .append(Component.literal("\n"));
        three.append(offset(8)).append(Component.literal("gather."));
        Component d = LineTranslator.translateChat(StyledText.fromComponent(three), store2);
        String sd = text(d);
        System.out.println("      輸出：" + sd.replace("\n", " ⏎ "));
        int max = LineTranslator.measureForTest.applyAsInt(Component.literal(longEn));
        boolean fits = true;
        boolean clean = true;
        for (String line : sd.split("\n")) {
            fits &= LineTranslator.measureForTest.applyAsInt(Component.literal(line)) <= max;
            clean &= !line.isEmpty() && "，。！？、".indexOf(line.charAt(0)) < 0;
        }
        check("每一行都不超過英文最寬那行", fits);
        check("沒有一行是標點開頭", clean);
        check("第一行斷在標點後面", sd.split("\n")[0].endsWith("，"));

        recruit();
        objective();
        rewardIndent();
        report();
    }

    /**
     * 「[Objective Completed]」三行用半形空白墊到中間，三行的中心差了將近 40px。
     * 整則送來、一行一則送來兩條路都要守住各自的中心。
     */
    private static void objective() throws Exception {
        String[] en = {"[Objective Completed]", "Loot Chests T3+", "Click here to claim your rewards!"};
        String[] zh = {"[目標完成]", "戰利品寶箱 T3+", "點這裡領取你的獎勵！"};
        int[] spaces = {13, 15, 5};
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        for (int i = 0; i < en.length; i++) {
            root.addProperty(en[i].replace("T3", "T{~}"), zh[i].replace("T3", "T{~}"));
        }
        root.addProperty(String.join("\n", en).replace("T3", "T{~}"),
                         String.join("\n", zh).replace("T3", "T{~}"));
        Path dir = Files.createTempDirectory("wynnchayuan-chat-objective");
        Files.writeString(dir.resolve("misc.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        MutableComponent all = Component.empty();
        java.util.List<StyledText> each = new java.util.ArrayList<>();
        for (int i = 0; i < en.length; i++) {
            if (i > 0) {
                all.append(Component.literal("\n"));
            }
            String row = " ".repeat(spaces[i]) + en[i];
            all.append(Component.literal(row));
            each.add(StyledText.fromComponent(Component.literal(row)));
        }
        Component out = LineTranslator.translateChat(StyledText.fromComponent(all), store);
        check("整則的目標完成翻得出來", out != null);
        if (out != null) {
            StringBuilder seen = new StringBuilder();
            boolean ok = true;
            for (int i = 0; i < en.length; i++) {
                int want = centreOf(Component.literal(" ".repeat(spaces[i]) + en[i]), 0);
                int got = centreOf(out, i);
                seen.append(want).append("→").append(got).append(' ');
                ok &= Math.abs(want - got) <= 2;
            }
            check("★ 整則送來：每一行守住原文的中心（" + seen.toString().strip() + "）", ok);
        }

        boolean[] centred = LineTranslator.chatCentred(each);
        boolean all3 = centred.length == 3 && centred[0] && centred[1] && centred[2];
        check("★ 一行一則送來：三行都判成置中", all3);
        StringBuilder seen = new StringBuilder();
        boolean ok = all3;
        for (int i = 0; all3 && i < en.length; i++) {
            Component row = LineTranslator.translateChat(each.get(i), store, centred[i], false);
            int want = centreOf(each.get(i).getComponent(), 0);
            int got = row == null ? -999 : centreOf(row, 0);
            seen.append(want).append("→").append(got).append(' ');
            ok &= Math.abs(want - got) <= 2;
        }
        check("★ 一行一則送來：每一行守住原文的中心（" + seen.toString().strip() + "）", ok);
    }

    /**
     * 任務獎勵一行一則、同一個縮排排成一列。譯文比英文寬一點（多了「張」），
     * 聊天視窗還放得下就不能把縮排收掉。
     */
    private static void rewardIndent() {
        Style space = SpaceOffset.styleFor(Style.EMPTY);
        java.util.List<LineTranslator.Run> orig = java.util.List.of(
                new LineTranslator.Run(true, 50, space, SpaceOffset.encode(50)),
                new LineTranslator.Run(false, 0, Style.EMPTY, "- +1 Ragni Teleportation Scroll"));
        java.util.List<LineTranslator.Run> made = java.util.List.of(
                new LineTranslator.Run(true, 50, space, SpaceOffset.encode(50)),
                new LineTranslator.Run(false, 0, Style.EMPTY, "- +1 張 Ragni Teleportation Scroll"));
        int kept = LineTranslator.fitWidth(orig, made, 10, new int[0], 320);
        check("單欄的行：視窗放得下就不收縮排（補=" + kept + "）", kept == 10);
        int tight = LineTranslator.fitWidth(orig, made, 10, new int[0], 0);
        check("不知道視窗多寬時照舊收回（補=" + tight + "）", tight < 10);

        // 五行同一個縮排、寬度各不相同：就算整塊判斷說某一行置中，也要改回靠左。
        String[] rows = {"Rewards:", "- +Access to the Province of Wynn",
                         "- +1 Ragni Teleportation Scroll", "- +35 Experience Points", "- +6 Emeralds"};
        java.util.List<java.util.List<LineTranslator.Run>> list = new java.util.ArrayList<>();
        boolean[] centre = new boolean[rows.length];
        for (int i = 0; i < rows.length; i++) {
            list.add(java.util.List.of(
                    new LineTranslator.Run(true, 50, space, SpaceOffset.encode(50)),
                    new LineTranslator.Run(false, 0, Style.EMPTY, rows[i])));
            centre[i] = i == 2;                // 模擬實機：只有 Ragni 那一行被判成置中
        }
        LineTranslator.sameLeadList(list, centre);
        boolean none = true;
        for (boolean c : centre) {
            none &= !c;
        }
        check("★ 同一個縮排的獎勵清單全部靠左", none);
    }

    /** 第 {@code row} 行實字的中心：最左一個非空白字到最右一個字的中點。 */
    private static int centreOf(Component c, int row) {
        int[] r = {0};
        int[] x = {0};
        int[] left = {-1};
        int[] right = {0};
        c.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                if (r[0] == row) {
                    x[0] += SpaceOffset.decode(text);
                }
                return java.util.Optional.empty();
            }
            for (int k = 0; k < text.length(); ) {
                int cp = text.codePointAt(k);
                k += Character.charCount(cp);
                if (cp == '\n') {
                    r[0]++;
                    continue;
                }
                if (r[0] != row) {
                    continue;
                }
                int w = LineTranslator.measureForTest.applyAsInt(
                        Component.literal(new String(Character.toChars(cp))));
                if (cp != ' ' && left[0] < 0) {
                    left[0] = x[0];
                }
                x[0] += w;
                if (cp != ' ') {
                    right[0] = x[0];
                }
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return (left[0] + right[0]) / 2;
    }

    /** 用縮排墊到中間的招募訊息：四行的中心差幾像素，但都是置中的。 */
    private static void recruit() throws Exception {
        String[] en = {"Enjoying Wynncraft?", "Recruit a friend and both of you will get rewards!",
                       "", "Click here to recruit", "(or type /recruit)"};
        int[] lead = {100, 10, 0, 95, 104};
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("{#}Enjoying Wynncraft?\n{#}Recruit a friend and both of you will get rewards!\n\n"
                        + "{#}Click here to recruit\n{#}(or type /recruit)",
                "{#}喜歡 Wynncraft 嗎？\n{#}邀請朋友一起玩，雙方都能拿到獎勵！\n\n{#}點這裡邀請\n{#}(或輸入 /recruit)");
        Path dir = Files.createTempDirectory("wynnchayuan-chat-recruit");
        Files.writeString(dir.resolve("misc.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        MutableComponent all = Component.empty();
        for (int i = 0; i < en.length; i++) {
            if (i > 0) {
                all.append(Component.literal("\n"));
            }
            if (!en[i].isEmpty()) {
                all.append(offset(lead[i])).append(Component.literal(en[i]));
            }
        }
        Component out = LineTranslator.translateChat(StyledText.fromComponent(all), store);
        check("招募訊息翻得出來", out != null);
        if (out == null) {
            return;
        }
        String[] made = out.getString().split("\n", -1);
        StringBuilder seen = new StringBuilder();
        boolean ok = made.length == en.length;
        for (int i = 0; ok && i < en.length; i++) {
            if (en[i].isEmpty()) {
                continue;
            }
            int want = lead[i] + LineTranslator.measureForTest.applyAsInt(Component.literal(en[i])) / 2;
            int[] lw = leadAndBody(out, i);
            int got = lw[0] + lw[1] / 2;
            seen.append(want).append("→").append(got).append(' ');
            ok &= Math.abs(want - got) <= 2;
        }
        check("★ 每一行照原文的中心置中（原文→譯文 " + seen.toString().strip() + "）", ok);
    }

    /** 第 {@code row} 行的縮排與內容寬度。 */
    private static int[] leadAndBody(Component c, int row) {
        int[] r = {0};
        int[] lead = {0};
        int[] body = {0};
        boolean[] started = {false};
        c.visit((style, text) -> {
            String[] parts = text.split("\n", -1);
            for (int p = 0; p < parts.length; p++) {
                String part = parts[p];
                if (p > 0) {
                    r[0]++;
                    started[0] = false;
                }
                if (r[0] != row || part.isEmpty()) {
                    continue;
                }
                if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(part) && !started[0]) {
                    lead[0] += SpaceOffset.decode(part);
                } else {
                    started[0] = true;
                    body[0] += LineTranslator.measureForTest.applyAsInt(
                            Component.literal(part).withStyle(style));
                }
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return new int[] {lead[0], body[0]};
    }

    private static StyledText two(String first, String second) {
        MutableComponent all = Component.empty();
        all.append(offset(8)).append(Component.literal(first)).append(Component.literal("\n"));
        all.append(offset(8)).append(Component.literal(second));
        return StyledText.fromComponent(all);
    }

    /** 拿掉排版偏移之後的文字。 */
    private static String text(Component c) {
        if (c == null) {
            return "(null)";
        }
        StringBuilder sb = new StringBuilder();
        c.visit((style, t) -> {
            if (!(SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(t))) {
                sb.append(t);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static MutableComponent offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0 ? "中日文重新斷行：全部通過"
                : "中日文重新斷行：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
