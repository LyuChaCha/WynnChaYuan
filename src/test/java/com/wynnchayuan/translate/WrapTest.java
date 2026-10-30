package com.wynnchayuan.translate;

import java.util.Random;

public final class WrapTest {

    private static int failures = 0;

    private static int measure(String piece) {
        return piece.codePointCount(0, piece.length());
    }

    private static int realistic(String piece) {
        int px = 0;
        for (int i = 0; i < piece.length(); ) {
            int cp = piece.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == ' ') {
                px += 4;
            } else if (cp >= 0x3000) {
                px += 9;
            } else {
                px += 5;
            }
        }
        return piece.startsWith("{") && piece.endsWith("}") ? 8 : px;
    }

    public static void main(String[] args) {
        check("換行點落在空白上不會丟例外",
                () -> TextWrapper.wrapToWidth(
                        "Blinding Lights: All Ophanim orbs deal 250% damage",
                        8, WrapTest::measure));

        check("寬度只夠一個字",
                () -> TextWrapper.wrapToWidth("a b c d e f g", 1, WrapTest::measure));
        check("整串都是空白",
                () -> TextWrapper.wrapToWidth("          ", 3, WrapTest::measure));
        check("佔位符不會被拆開",
                () -> TextWrapper.wrapToWidth("{~} 格內的友軍 {~} 生命", 4, WrapTest::measure));

        String orphan = TextWrapper.wrapToWidth(
                "每次命中獲得層數，並改為環繞你運行。", 8, WrapTest::measure);
        boolean starts = false;
        for (String line : orphan.split(String.valueOf(NL))) {
            if (!line.isEmpty() && "，。、；：！？）」".indexOf(line.charAt(0)) >= 0) {
                starts = true;
            }
        }
        report("標點不會被擠到行首（實際：" + show(orphan) + "）", !starts);

        String signed = TextWrapper.wrapToWidth("每次命中增加 +{~} 層數", 7,
                                                   WrapTest::measure);
        report("號不會跟數值分家（實際：" + show(signed) + "）",
                !signed.contains("+" + NL));

        String[] latin = {
            "Erhoeht den Schaden um",
            "Aumenta el dano magico",
            "Augmente les degats de sort",
            "Увеличивает урон заклинаний",
        };
        for (String text : latin) {
            String wrapped = TextWrapper.wrapToWidth(text, 12, WrapTest::measure);
            report("不把單字切成兩半：" + text + "（實際：" + show(wrapped) + "）",
                    tidy(wrapped.replace(NL, ' ')).equals(tidy(text)));
        }

        String hangul = TextWrapper.wrapToWidth("마법 피해 증가량 상승", 4,
                                                   WrapTest::measure);
        report("諺文折行不會掉字", bare(hangul).equals(bare("마법 피해 증가량 상승")));

        String huge = TextWrapper.wrapToWidth(
                "Bewegungsgeschwindigkeit", 8, WrapTest::measure);
        report("超長單字會被切開而不是滿出去",
                huge.indexOf(NL) > 0 && bare(huge).equals("Bewegungsgeschwindigkeit"));

        String text = "Allies within {~} blocks gain {~} of the health you gain";
        for (int px = 1; px <= 60; px++) {
            String wrapped = TextWrapper.wrapToWidth(text, px, WrapTest::measure);
            if (!bare(wrapped).equals(bare(text))) {
                System.out.println("  [FAIL] 寬度 " + px + " 折完內容變了：" + wrapped);
                failures++;
                break;
            }
        }
        check("內容守恆（寬度 1 到 60）", () -> "");

        Random random = new Random(20260822L);
        String alphabet = " abcXYZ中文的字{~}";
        for (int round = 0; round < 4000; round++) {
            StringBuilder sb = new StringBuilder();
            int length = random.nextInt(60);
            for (int i = 0; i < length; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String input = sb.toString();
            int px = random.nextInt(30);
            try {
                TextWrapper.wrapToWidth(input, px, WrapTest::measure);
            } catch (RuntimeException e) {
                System.out.println("  [FAIL] 亂數第 " + round + " 輪丟例外："
                        + e + "  寬度 " + px + "  輸入 [" + input + "]");
                failures++;
                break;
            }
        }
        check("亂數四千輪都沒丟例外", () -> "");

        clauseBreaks();
        placeholderNotSplit();
        colourTokensNeverShown();
        glyphNotAtLineEnd();
        oneLine();
        labelBreak();
        balanced();
        balanceKeepsClause();
        valueWithLabel();
        latinRunsWhole();
        rangesWhole();

        System.out.println(failures == 0 ? "Wrap: 全部通過" : "Wrap: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void clauseBreaks() {
        String text = "所有 奧法尼姆 光球造成 250% 傷害，每次命中使你獲得 +5 層 結晶化，"
                + "並改為環繞你運行。使用普攻時，所有光球的移動速度都會加快。";
        String wrapped = TextWrapper.wrapToWidth(text, 150, WrapTest::realistic);
        String[] rows = wrapped.split(String.valueOf(NL));
        System.out.println("  折出來的樣子：");
        for (String row : rows) {
            System.out.println("    |" + row + "|");
        }
        boolean clean = true;
        for (int i = 0; i < rows.length - 1; i++) {
            char last = rows[i].charAt(rows[i].length() - 1);
            if ("，。、；：！？".indexOf(last) < 0) {
                clean = false;
            }
        }
        report("每一行都收在標點上（實際 " + rows.length + " 行）", clean);
        report("內容沒有變", bare(wrapped).equals(bare(text)));

        String early = TextWrapper.wrapToWidth("好，這是一句很長很長很長很長的話", 150,
                                                  WrapTest::realistic);
        report("太早的標點不斷（實際：" + show(early) + "）", !early.startsWith("好，" + NL));
    }

    private static void balanced() {
        System.out.println("\n  -- 同樣行數要排得平均 --");

        String text = "每次命中使你獲得層數並改為環繞你運行造成傷害";
        String greedy = TextWrapper.wrapToWidth(text, 10, WrapTest::measure);
        String even = TextWrapper.wrapBalanced(text, 10, WrapTest::measure);
        report("這句夠長，真的折成三行以上（" + show(greedy) + "）", rowsOf(greedy).length >= 3);
        report("行數沒變（" + show(even) + "）", rowsOf(even).length == rowsOf(greedy).length);
        report("最後一行不再是零頭（" + shortest(greedy) + " → " + shortest(even) + "）",
                shortest(even) > shortest(greedy));

        String withWord = "每次命中使你獲得 Crystallize 層數。";
        String tight = TextWrapper.wrapBalanced(withWord, 14, WrapTest::measure);
        boolean split = false;
        for (int at = tight.indexOf(NL); at > 0 && at + 1 < tight.length();
                at = tight.indexOf(NL, at + 1)) {
            if (latin(tight.charAt(at - 1)) && latin(tight.charAt(at + 1))) {
                split = true;
            }
        }
        report("平均分配沒有把英文單字切開（實際：" + show(tight) + "）", !split);

        report("字沒有變少", bare(tight).equals(bare(withWord)));
    }

    private static void balanceKeepsClause() {
        System.out.println("\n  -- 平均分配不丟掉標點上的斷點 --");
        String text = "本次剩餘的整段期間，每提供一個信標就增加一點傷害";
        String out = TextWrapper.wrapBalanced(text, 12, WrapTest::measure);
        String[] rows = rowsOf(out);
        report("第一行收在逗號上（實際：" + show(out) + "）",
               rows.length == 3 && rows[0].endsWith("，"));
        report("字沒有變少", bare(out).equals(bare(text)));
    }

    private static void valueWithLabel() {
        System.out.println("\n  -- 數值跟屬性名不分家 --");
        String[][] cases = {
            {"本次 Lootrun 剩餘期間，每提供一個信標就 +{~1} 元素傷害 (最多 x{~2})", "+{~1} 元素傷害"},
            {"本次 Lootrun 剩餘期間，每提供一個信標就 +{~1} {#}防禦 (上限 x{~2})", "+{~1} {#}防禦"},
            {"本次 Lootrun 剩餘期間，寶箱每提供 {~3} 件物品就 +{~1} 寶物品質 (最多 x{~2})",
             "+{~1} 寶物品質"},
            {"本次 Lootrun 出現藍色或紫色信標超過 {~} 次之後，元素傷害 +{~}", "元素傷害 +{~}"},
            {"このルートランで青か紫のビーコンが {~} 回より多く提示されると、属性ダメージ +{~}",
             "属性ダメージ +{~}"},
            {"每次命中回復 -{~}/5s 生命，並使周圍友軍獲得 x{~} 倍傷害", "-{~}/5s 生命"},
            {"Когда в этом Lootrun вам предложат маяк более {~} раз, получите +{~} к стихийному урону",
             "+{~} к стихийному"},
        };
        for (String[] c : cases) {
            String text = c[0];
            String unit = c[1];
            int bad = 0;
            String first = null;
            boolean lost = false;
            for (int px = additive(unit); px <= 320; px++) {
                String wrapped = TextWrapper.wrapBalanced(text, px, WrapTest::additive);
                if (!bare(wrapped).equals(bare(text))) {
                    lost = true;
                }
                boolean kept = false;
                for (String row : rowsOf(wrapped)) {
                    kept |= row.contains(unit);
                }
                if (!kept) {
                    bad++;
                    if (first == null) {
                        first = "寬度 " + px + "：" + show(wrapped);
                    }
                }
            }
            report("「" + unit + "」沒有被拆開" + (first == null ? "" : "（例如 " + first + "）"),
                   bad == 0);
            report("「" + unit + "」那句沒有掉字", !lost);
        }
    }

    private static int additive(String piece) {
        int px = 0;
        for (int i = 0; i < piece.length(); ) {
            int cp = piece.codePointAt(i);
            i += Character.charCount(cp);
            px += cp == ' ' ? 4 : cp >= 0x2E80 ? 9 : 5;
        }
        return px;
    }

    private static void oneLine() {
        System.out.println("\n  -- 放得下就留在同一行 --");

        String text = "猛撲: 逃脫變成向前突進。";
        String fits = TextWrapper.wrapBalanced(text, 11, WrapTest::measure);
        report("沒有斷行（實際：" + show(fits) + "）", fits.indexOf(NL) < 0);
        report("字沒有變少", bare(fits).equals(bare(text)));

        String tight = TextWrapper.wrapBalanced(text, 8, WrapTest::measure);
        report("差太多的還是要折（實際：" + show(tight) + "）", tight.indexOf(NL) > 0);
    }

    private static void labelBreak() {
        System.out.println("\n  -- 斷在名稱後面 --");

        String text = "猛撲: 逃脫變成向前突進在此";
        String out = TextWrapper.wrapBalanced(text, 11, WrapTest::measure);
        String[] rows = rowsOf(out);
        report("折成兩行（實際：" + show(out) + "）", rows.length == 2);
        if (rows.length == 2) {
            report("第一行就是名稱那半", rows[0].equals("猛撲:"));
            report("說明整句在第二行", rows[1].equals("逃脫變成向前突進在此"));
        }
        report("字沒有變少", bare(out).equals(bare(text)));

        String longer = "猛撲: 逃脫變成向前突進並造成傷害";
        String[] wide = rowsOf(TextWrapper.wrapBalanced(longer, 11, WrapTest::measure));
        report("貪心兩行就夠時，不為了斷在名稱後而多花一行（實際："
                + show(String.join(String.valueOf(NL), wide)) + "）",
                wide.length == 2 && !wide[0].equals("猛撲:"));

        String longest = "猛撲: 逃脫變成向前突進並且造成大量範圍傷害。";
        String[] three = rowsOf(TextWrapper.wrapBalanced(longest, 11, WrapTest::measure));
        report("行數一樣就照斷（實際："
                + show(String.join(String.valueOf(NL), three)) + "）",
                three.length == 3 && three[0].equals("猛撲:"));
        report("剩下那半也排得平均",
                three.length == 3 && Math.abs(three[1].length() - three[2].length()) <= 2);
    }

    private static void glyphNotAtLineEnd() {
        System.out.println("\n  -- 圖示不能落在行尾 --");

        String text = "此物品的力量已被封印，使用 {#} 物品鑑定師 即可解放其潛能。";
        int bad = 0;
        String worst = null;
        for (int px = 4; px <= 40; px++) {
            String wrapped = TextWrapper.wrapToWidth(text, px, WrapTest::measure);
            for (String row : wrapped.split(String.valueOf(NL), -1)) {
                String core = row.stripTrailing();
                if (core.endsWith("{#}") && !core.strip().equals("{#}")) {
                    bad++;
                    if (worst == null) {
                        worst = "寬度 " + px + "：" + show(wrapped);
                    }
                }
            }
        }
        report("沒有任何寬度讓圖示落在行尾"
                + (worst == null ? "" : "（例如 " + worst + "）"), bad == 0);

        String once = TextWrapper.wrapToWidth(text, 12, WrapTest::measure);
        report("字沒有變少", bare(once).equals(bare(text)));

        String upgrade = "一件強大的神器，可透過 {#} 物品升級師 將物品提升至全新的境界。";
        int wide = 0;
        String first = null;
        for (int px = 4; px <= 120; px++) {
            String wrapped = TextWrapper.wrapToWidth(upgrade, px, WrapTest::measure);
            for (String row : wrapped.split(String.valueOf(NL), -1)) {
                String core = row.stripTrailing();
                if (core.endsWith("{#}") && !core.strip().equals("{#}")) {
                    wide++;
                    if (first == null) {
                        first = "寬度 " + px + "：" + show(wrapped);
                    }
                }
            }
        }
        report("寬一點的版面也不會讓圖示落在行尾"
                + (first == null ? "" : "（例如 " + first + "）"), wide == 0);

        int bal = 0;
        String balWorst = null;
        for (int px = 8; px <= 320; px++) {
            for (int rows = 2; rows <= 4; rows++) {
                String wrapped = TextWrapper.wrapBalanced(
                        upgrade, px, rows, WrapTest::realistic);
                for (String row : wrapped.split(String.valueOf(NL), -1)) {
                    String core = row.stripTrailing();
                    if (core.endsWith("{#}") && !core.strip().equals("{#}")) {
                        bal++;
                        if (balWorst == null) {
                            balWorst = "寬度 " + px + "、" + rows + " 行："
                                    + show(wrapped);
                        }
                    }
                }
            }
        }
        report("平均分配之後圖示也不能落在行尾"
                + (balWorst == null ? "" : "（例如 " + balWorst + "）"), bal == 0);

        java.util.List<com.wynnchayuan.capture.LineParts.Piece> accents =
                java.util.List.of(new com.wynnchayuan.capture.LineParts.Piece(
                        "物品升級師", net.minecraft.network.chat.Style.EMPTY));
        String[] split = {"一件強大的神器，可透過 {#} 物品升級", "師 將物品提升至全新的境界。"};
        String[] kept = TemplateRenderer.keepAccentsWhole(split, accents);
        System.out.println("      搬完：" + String.join(" ⏎ ", kept));
        report("重點詞搬下去之後，圖示不會被留在行尾",
               !kept[0].stripTrailing().endsWith("{#}"));
        report("重點詞真的整個到了下一行", kept[1].contains("物品升級師"));
        report("字沒有變少",
               bare(String.join("", kept)).equals(bare(String.join("", split))));
    }

    private static void placeholderNotSplit() {
        System.out.println("\n  -- 佔位符不能被斷成兩半 --");

        String text = "為你恢復 {~1}、為友軍恢復 {~2}，但衝鋒不再造成傷害。";
        int bad = 0;
        String worst = null;
        for (int px = 4; px <= 40; px++) {
            String wrapped = TextWrapper.wrapToWidth(text, px, WrapTest::measure);
            for (String row : rowsOf(wrapped)) {
                int open = 0;
                for (int i = 0; i < row.length(); i++) {
                    if (row.charAt(i) == '{') {
                        open++;
                    } else if (row.charAt(i) == '}') {
                        open--;
                    }
                }
                if (open != 0) {
                    bad++;
                    if (worst == null) {
                        worst = "寬度 " + px + "：" + show(wrapped);
                    }
                }
            }
        }
        report("沒有任何寬度把佔位符斷成兩半"
                + (worst == null ? "" : "（例如 " + worst + "）"), bad == 0);

        String once = TextWrapper.wrapToWidth(text, 14, WrapTest::measure);
        report("字沒有變少", bare(once).equals(bare(text)));

        String colour = "使用這件物品進入{c2}討伐戰{/}，或製作{c2}腐化地城鑰匙{/}";
        int split = 0;
        String first = null;
        for (int px = 4; px <= 60; px++) {
            String wrapped = TextWrapper.wrapToWidth(colour, px, WrapTest::measure);
            for (String row : rowsOf(wrapped)) {
                int open = 0;
                for (int i = 0; i < row.length(); i++) {
                    if (row.charAt(i) == '{') {
                        open++;
                    } else if (row.charAt(i) == '}') {
                        open--;
                    }
                }
                if (open != 0) {
                    split++;
                    if (first == null) {
                        first = "寬度 " + px + "：" + show(wrapped);
                    }
                }
            }
        }
        report("顏色佔位符不會被斷成兩半"
                + (first == null ? "" : "（例如 " + first + "）"), split == 0);
    }

    private static void colourTokensNeverShown() {
        System.out.println("\n  -- 顏色佔位符不能印到畫面上 --");
        report("清掉編號形式",
               "使用這件物品進入討伐戰，或製作腐化地城鑰匙".equals(
                       ColourTemplate.stripColourTokens(
                               "使用這件物品進入{c2}討伐戰{/}，或製作{c2}腐化地城鑰匙{/}")));
        report("清掉自訂色形式",
               "警告".equals(ColourTemplate.stripColourTokens("{c:#FF55FF}警告{/}")));
        report("沒有大括號的原樣返回",
               "一般文字".equals(ColourTemplate.stripColourTokens("一般文字")));
        report("不會誤清別的佔位符",
               "{#}{~} 場挑戰".equals(
                       ColourTemplate.stripColourTokens("{#}{c1}{~} 場挑戰{/}")));
        report("null 不會炸", ColourTemplate.stripColourTokens(null) == null);
    }

    private static void latinRunsWhole() {
        System.out.println("\n  -- 英文名稱整串不斷開 --");
        String text = "Arrow Shield 的層數固定為 {~1}，但傷害加倍。Arrow Wall 改為讓 "
                + "Arrow Shield 的傷害變為三倍，並使 Mask of the Lunatic 的彈藥加倍。";
        int longest = "Mask of the Lunatic".length();
        int bad = 0;
        String first = null;
        boolean lost = false;
        for (int px = longest; px <= 80; px++) {
            for (String wrapped : new String[] {
                    TextWrapper.wrapToWidth(text, px, WrapTest::measure),
                    TextWrapper.wrapBalanced(text, px, WrapTest::measure)}) {
                lost |= !bare(wrapped).equals(bare(text));
                String[] rows = rowsOf(wrapped);
                for (int i = 0; i + 1 < rows.length; i++) {
                    String a = rows[i].stripTrailing();
                    String b = rows[i + 1].stripLeading();
                    if (!a.isEmpty() && !b.isEmpty()
                            && latin(a.charAt(a.length() - 1)) && latin(b.charAt(0))) {
                        bad++;
                        if (first == null) {
                            first = "寬度 " + px + "：" + show(wrapped);
                        }
                    }
                }
            }
        }
        report("放得下的英文名稱不會被斷在兩行之間"
                + (first == null ? "" : "（例如 " + first + "）"), bad == 0);
        report("字沒有變少", !lost);

        String english = TextWrapper.wrapToWidth(
                "Arrow Shield is set to two charges but deals doubled damage", 20,
                WrapTest::measure);
        report("整句英文照樣在空白上折行（實際：" + show(english) + "）",
                rowsOf(english).length >= 3);
    }

    private static String[] rowsOf(String wrapped) {
        return wrapped.split(String.valueOf(NL), -1);
    }

    private static int shortest(String wrapped) {
        int least = Integer.MAX_VALUE;
        for (String row : rowsOf(wrapped)) {
            least = Math.min(least, row.length());
        }
        return least;
    }

    private static boolean latin(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static String tidy(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }

    private static String bare(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().filter(c -> !Character.isWhitespace(c)).forEach(sb::appendCodePoint);
        return sb.toString();
    }

    private interface Body {
        String run();
    }

    private static final char NL = '\n';

    private static void rangesWhole() {
        String[] texts = {
            "甲乙丙丁每提供 {~} 件，就隨機獲得 +{~}-{~} 子丑、+{~}-{~} 寅卯、+{~}-{~} 辰巳或 +{~}-{~} 秒。",
            "甲乙丙丁每提供 3 件，就隨機獲得 +3-3 子丑、+1-1 寅卯、+1-2 辰巳或 +60-60 秒。",
        };
        java.util.regex.Pattern splitRange = java.util.regex.Pattern.compile(
                "(?:\\d|\\{~\\d*})%?" + NL + "[-+]?(?:\\d|\\{~)"
                + "|[-+]" + NL + "(?:\\d|\\{~)"
                + "|(?:\\d|})%?[-+]" + NL);
        int bad = 0;
        int wrapped = 0;
        String example = null;
        for (String text : texts) {
            for (int px = 45; px <= 160; px++) {
                for (TextWrapper.Keep keep : TextWrapper.Keep.values()) {
                    String out = TextWrapper.wrapToWidth(text, px, WrapTest::additive, keep);
                    if (out.indexOf(NL) >= 0) {
                        wrapped++;
                    }
                    if (splitRange.matcher(out).find()) {
                        bad++;
                        if (example == null) {
                            example = keep + " " + px + "px：" + show(out);
                        }
                    }
                }
                String balanced = TextWrapper.wrapBalanced(text, px, 4, WrapTest::additive);
                if (splitRange.matcher(balanced).find()) {
                    bad++;
                    if (example == null) {
                        example = "balanced " + px + "px：" + show(balanced);
                    }
                }
            }
        }
        report("這兩句真的有被折行（" + wrapped + " 種）", wrapped > 100);
        report("★ 數值範圍一次都沒有被拆開（" + bad + " 次" + (example == null ? "" : "，例如 " + example) + "）",
               bad == 0);
        check("塞不下的範圍不會丟例外",
                () -> TextWrapper.wrapToWidth("+{~}-{~}", 2, WrapTest::additive));
    }

    private static String show(String wrapped) {
        return wrapped.replace(String.valueOf(NL), " ⏎ ");
    }

    private static void report(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void check(String what, Body body) {
        try {
            body.run();
            System.out.println("  [PASS] " + what);
        } catch (RuntimeException e) {
            System.out.println("  [FAIL] " + what + " —— " + e);
            failures++;
        }
    }
}
