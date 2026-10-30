package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BracketAccentTest {

    private static int failures = 0;

    private static final int NAME = 0xFF8C19;
    private static final int TYPE = 0xAAAAAA;

    public static void main(String[] args) throws Exception {
        FlowedDebug.init(Files.createTempDirectory("wynnchayuan"));

        card();
        nameContainsType();
        miniQuest();
        wrappedSpan();
        splitWord();
        spanAcrossRows();
        coordBracketOnOwnRow();
        noBracketsInTranslation();
        reorderedBrackets();
        plurals();
        rules();

        report();
    }

    private static StyledText title(String name, String type) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(name).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(NAME))));
        line.append(Component.literal(type).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(TYPE))));
        return StyledText.fromComponent(line);
    }

    private static TranslationStore corpus(String key, String zh,
                                           String type, String typeZh) throws Exception {
        Path dir = Files.createTempDirectory("bracket-accent");
        Files.writeString(dir.resolve("misc.json"), "{\"" + key + "\": \"" + zh + "\"}");
        Files.writeString(dir.resolve("wynntils.json"),
                "{\"" + type + "\": \"" + typeZh + "\"}");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static void plurals() throws Exception {
        TranslationStore store = corpus(
                "Arcane Anomaly", "奧祕異象", "Dragonling Scale", "龍崽鱗片");

        check("「[{~} Arcane Anomalies]」→ 奧祕異象（-ies 換回 -y）",
              "奧祕異象".equals(
                      LineTranslator.lookupWordCore("[15 Arcane Anomalies]", store)));
        check("「[{~} Dragonling Scales]」→ 龍崽鱗片（剝掉 -s）",
              "龍崽鱗片".equals(
                      LineTranslator.lookupWordCore("[15 Dragonling Scales]", store)));

        TranslationStore ss = corpus("Glass", "玻璃", "Moss", "苔蘚");
        check("「-ss」結尾的不剝（Glass 不會變成 Glas）",
              "玻璃".equals(LineTranslator.lookupWordCore("[3 Glass]", ss)));
        check("查不到的仍然回傳 null（不亂猜）",
              LineTranslator.lookupWordCore("[3 Widgets]", ss) == null);
    }

    private static void card() throws Exception {
        TranslationStore store = corpus(
                "Dragonkin Nest [Cave]", "龍裔巢穴 [洞窟]", "Cave", "洞窟");
        Component one = LineTranslator.translate(
                title("Dragonkin Nest ", "[Cave]"), store);
        check("整條標題查得到譯文", one != null);
        if (one == null) {
            return;
        }
        List<Component> built = List.of(one);
        System.out.println("譯文：" + text(built));
        dump(built);

        check("名稱翻出來了", text(built).contains("龍裔巢穴"));
        check("類型翻出來了", text(built).contains("[洞窟]"));
        check("名稱是橘的（拿到 " + show(colourOf(built, "龍裔巢穴")) + "）",
              is(colourOf(built, "龍裔巢穴"), NAME));
        check("★ 「[洞窟]」是完整的一段（實際分段：" + pieces(built) + "）",
              pieces(built).contains("[洞窟]"));
        check("★ 「[洞窟]」整段是灰的（拿到 " + show(colourOf(built, "[洞窟]")) + "）",
              is(colourOf(built, "[洞窟]"), TYPE));
    }

    private static void nameContainsType() throws Exception {
        TranslationStore store = corpus(
                "Cave of Doom [Cave]", "末日洞窟 [洞窟]", "Cave", "洞窟");
        Component one = LineTranslator.translate(
                title("Cave of Doom ", "[Cave]"), store);
        check("［名稱含類型詞］查得到譯文", one != null);
        if (one == null) {
            return;
        }
        List<Component> built = List.of(one);
        System.out.println("［名稱含類型詞］譯文：" + text(built));
        dump(built);

        check("★［名稱含類型詞］名稱整塊是橘的（實際分段：" + pieces(built)
              + "，拿到 " + show(colourOf(built, "末日洞窟")) + "）",
              is(colourOf(built, "末日洞窟"), NAME));
        check("★［名稱含類型詞］「[洞窟]」是完整的一段", pieces(built).contains("[洞窟]"));
        check("★［名稱含類型詞］「[洞窟]」是灰的（拿到 "
              + show(colourOf(built, "[洞窟]")) + "）",
              is(colourOf(built, "[洞窟]"), TYPE));
    }

    private static void miniQuest() throws Exception {
        TranslationStore store = corpus(
                "Slay Wraiths & Phantasms [Mini-Quest]", "獵殺怨靈與幻影 [迷你任務]",
                "Mini-Quest", "迷你任務");
        Component one = LineTranslator.translate(
                title("Slay Wraiths & Phantasms ", "[Mini-Quest]"), store);
        check("［迷你任務］查得到譯文", one != null);
        if (one == null) {
            return;
        }
        List<Component> built = List.of(one);
        System.out.println("［迷你任務］譯文：" + text(built));
        dump(built);

        check("★［迷你任務］「[迷你任務]」是完整的一段（實際分段：" + pieces(built) + "）",
              pieces(built).contains("[迷你任務]"));
        check("★［迷你任務］「[迷你任務]」是灰的（拿到 "
              + show(colourOf(built, "[迷你任務]")) + "）",
              is(colourOf(built, "[迷你任務]"), TYPE));
        check("［迷你任務］名稱是橘的（拿到 " + show(colourOf(built, "獵殺怨靈與幻影")) + "）",
              is(colourOf(built, "獵殺怨靈與幻影"), NAME));
    }

    private static void coordBracketOnOwnRow() throws Exception {
        List<StyledText> run = List.of(
                line("Bring ", BODY, "[40 Kanderstone Ingots]", ITEM),
                line("or ", BODY, "[40 Kanderstone Gems]", ITEM, " to the", BODY),
                line("Gathering Post ", BODY, "[Mining Lv. 73]", ITEM),
                line("at ", BODY, "[-712, 46, -5553]", COORD));
        String[] flowed = {
            "把 [{~} Kanderstone 錠]",
            "或 [{~} Kanderstone 寶",
            "石] 交到採集站 [採礦等級 {~}]，",
            "座標 [-{~}, {~}, -{~}]",
        };

        List<LineParts> parts = new ArrayList<>();
        List<LineParts.Piece> allRuns = new ArrayList<>();
        for (StyledText row : run) {
            LineParts part = LineParts.of(row);
            parts.add(part);
            allRuns.addAll(part.runs());
        }
        List<LineParts.Piece> accents =
                LineTranslator.bracketAccents(allRuns, flowed, colour(BODY));
        List<Component> built = LineTranslator.rebuildAll(
                flowed, parts, accents, null, null,
                corpus("Kanderstone Ingot", "Kanderstone 錠",
                       "Kanderstone Gem", "Kanderstone 寶石"));
        dump(built);

        String text = flat(built);
        int at = text.indexOf("712");
        check("［座標自成一列］譯文裡有這個座標", at > 0);
        if (at <= 0) {
            return;
        }
        int bracket = text.lastIndexOf('[', at);
        check("★［座標自成一列］座標開頭的「[」是白的，不是散文的灰（拿到 "
              + show(charColour(built, bracket))
              + "，實際分段：" + pieces(built) + "）",
              is(charColour(built, bracket), COORD));
        check("★［座標自成一列］收尾那個「]」也是白的（拿到 "
              + show(charColour(built, text.lastIndexOf(']'))) + "）",
              is(charColour(built, text.lastIndexOf(']')), COORD));
    }

    private static void reorderedBrackets() {
        List<LineParts.Piece> runs = List.of(
                piece("Collect ", BODY),
                piece("[4 Gold Chunks]", ITEM),
                piece(" for Jankan in the mineshaft at ", BODY),
                piece("[605, 80, -4920]", COORD),
                piece(".", BODY));
        String[] translated = {"到 [{~2}, {~3}, -{~4}] 的礦坑替 Jankan 收集 [{~1} 塊金塊]。"};
        Integer coords = null;
        Integer item = null;
        for (LineParts.Piece accent
                : LineTranslator.bracketAccents(runs, translated, colour(BODY))) {
            Integer rgb = accent.style().getColor() == null ? null
                    : accent.style().getColor().getValue();
            if (accent.text().startsWith("[{~2}")) {
                coords = rgb;
            }
            if (accent.text().contains("塊金塊]")) {
                item = rgb;
            }
        }
        check("★［括號換位］座標那一組拿到座標的白（拿到 " + show(coords) + "）",
                is(coords, COORD));
        check("★［括號換位］物品那一組拿到物品的青（拿到 " + show(item) + "）",
                is(item, ITEM));

        check("［括號換位］種類順序一樣時不重排",
                java.util.Arrays.equals(LineTranslator.pairBrackets(
                        List.of("4 Gold Chunks", "605, 80, -4920"),
                        List.of("[{~} 塊金塊]", "[{~}, {~}, -{~}]")), new int[] {0, 1}));
        check("［括號換位］個數對不上時不亂配",
                java.util.Arrays.equals(LineTranslator.pairBrackets(
                        List.of("4 Gold Chunks", "605, 80, -4920"),
                        List.of("[{~}, {~}]", "[{~}, {~}, -{~}]")), new int[] {0, 1}));
        check("［括號換位］三組：同類的照順序配",
                java.util.Arrays.equals(LineTranslator.pairBrackets(
                        List.of("Combat Lv. 88", "4 Hides", "1, 2, -3"),
                        List.of("[{~}, {~}, -{~}]", "[戰鬥等級 {~}]", "[{~} 張皮]")),
                        new int[] {2, 0, 1}));
    }

    private static final char NL = '\n';

    private static final int BODY = 0xAAAAAA;
    private static final int ITEM = 0x00AAAA;
    private static final int COORD = 0xFFFFFF;

    private static Style colour(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }

    private static LineParts.Piece piece(String text, int rgb) {
        return new LineParts.Piece(text, colour(rgb));
    }

    private static void wrappedSpan() {
        List<LineParts.Piece> runs = List.of(
                piece("Bring ", BODY), piece("[20 Malt String]", ITEM), piece(" or", BODY),
                piece("[20 Malt Grains]", ITEM), piece(" to the", BODY),
                piece("Gathering Post at ", BODY), piece("[-1234, 50, -4321]", COORD));
        String[] translated = {
            "把 [{~} 麥芽線] 或 [{~} 麥芽穀粒] 交到採集站，座標 [-{~}, {~}, -{~}]"
        };
        List<String> texts = new ArrayList<>();
        for (LineParts.Piece accent
                : LineTranslator.bracketAccents(runs, translated, colour(BODY))) {
            texts.add(accent.text());
        }
        System.out.println("［跨行括號］登記到的重點段：" + texts);

        check("［跨行括號］整塊 [{~} 麥芽穀粒] 有登記", texts.contains("[{~} 麥芽穀粒]"));
        check("［跨行括號］原本帶空格的那一份還在（沒斷行時靠它）",
              texts.contains(" 麥芽穀粒]"));
        check("★［跨行括號］去掉前導空格的「麥芽穀粒]」也登記了（斷行之後靠它）",
              texts.contains("麥芽穀粒]"));
        check("［跨行括號］座標那組沒有多登記短到會撞的片段（實際：" + texts + "）",
              !texts.contains(", ") && !texts.contains("-"));
    }

    private static void splitWord() {
        String[] flowed = {
            "把 [{~} Kanderstone 錠] 或 [{~} Kanderstone 寶",
            "石] 交到採集站 [採礦等級 {~}]，",
        };
        List<LineParts.Piece> accents = List.of(
                piece("[{~} Kanderstone 寶石]", ITEM),
                piece(" Kanderstone 寶石]", ITEM),
                piece("[{~} Kanderstone 錠]", ITEM),
                piece(" Kanderstone 錠]", ITEM));
        List<String> texts = new ArrayList<>();
        for (LineParts.Piece half
                : LineTranslator.halvesAcrossBreaks(flowed, accents)) {
            texts.add(half.text());
        }
        System.out.println("［詞被切開］補登記的兩半：" + texts);

        check("★［詞被切開］上一行那半登記了", texts.contains(" Kanderstone 寶"));
        check("★［詞被切開］下一行那半登記了（收尾那個 ] 靠它才有色）",
              texts.contains("石]"));
        check("［詞被切開］沒被切到的重點段不補（實際：" + texts + "）",
              !texts.contains(" Kanderstone 錠]"));
        for (String t : texts) {
            check("［詞被切開］補的兩半都不含佔位符（" + t + "）",
                  t.indexOf('{') < 0 && t.indexOf('}') < 0);
        }
    }

    private static void spanAcrossRows() {
        List<LineParts.Piece> runs = List.of(
                piece("Bring ", BODY), piece("[24 Salmon Oil]", ITEM), piece(" or ", BODY),
                piece("[24", ITEM),
                piece("Salmon Meat]", ITEM), piece(" to the Gathering", BODY),
                piece("Post at ", BODY), piece("[-57, 46, -2166]", COORD));
        String[] flowed = {
            "把 [{~} 鮭魚油] 或 [",
            "{~} 鮭魚肉] 交到採集站，",
            "座標 [-{~}, {~}, -{~}]",
        };
        List<String> texts = new ArrayList<>();
        for (LineParts.Piece accent
                : LineTranslator.bracketAccents(runs, flowed, colour(BODY))) {
            texts.add(accent.text());
        }
        System.out.println("［跨行的整組］登記到的重點段：" + texts);

        check("★［跨行的整組］沒有整組放棄（拿到 " + texts.size() + " 段）", !texts.isEmpty());
        check("★［跨行的整組］被切開的那組接回來了", texts.contains("[{~} 鮭魚肉]"));
        check("［跨行的整組］沒被切到的那組照樣在", texts.contains("[{~} 鮭魚油]"));
        for (String t : texts) {
            check("［跨行的整組］登記的字面裡沒有換行（" + t + "）",
                  t.indexOf(NL) < 0);
        }

        String[] moved = LineTranslator.keepAccentsWhole(
                flowed, List.of(piece("[{~} 鮭魚肉]", ITEM)));
        check("★［跨行的整組］行尾那個 [ 被搬到下一行（拿到 「" + moved[0]
              + "」／「" + moved[1] + "」）",
              !moved[0].stripTrailing().endsWith("[")
              && moved[1].startsWith("[{~} 鮭魚肉]"));
    }

    private static void noBracketsInTranslation() throws Exception {
        TranslationStore store = corpus(
                "Dragonkin Nest [Cave]", "龍裔巢穴洞窟", "Cave", "洞窟");
        Component one = LineTranslator.translate(
                title("Dragonkin Nest ", "[Cave]"), store);
        check("［無括號］查得到譯文", one != null);
        if (one == null) {
            return;
        }
        List<Component> built = List.of(one);
        System.out.println("［無括號］譯文：" + text(built));
        dump(built);
        check("［無括號］譯文裡沒有多出來的方括號（實際：" + text(built) + "）",
              text(built).indexOf('[') < 0 && text(built).indexOf(']') < 0);
        check("［無括號］類型詞仍是灰的（拿到 " + show(colourOf(built, "洞窟")) + "）",
              is(colourOf(built, "洞窟"), TYPE));
    }

    private static void rules() {
        wraps("[Cave]", "洞窟", "龍裔巢穴 [洞窟]", "[洞窟]");
        wraps("[Mini-Quest]", "迷你任務", "獵殺蜘蛛 [迷你任務]", "[迷你任務]");
        wraps("[Secret Discovery]", "隱藏探索點", "英雄的起點 [隱藏探索點]", "[隱藏探索點]");
        wraps("[Boss Altar]", "首領祭壇", "腐化祭壇 [首領祭壇]", "[首領祭壇]");
        wraps("[Cave]", "洞窟", "末日洞窟 [洞窟]", "[洞窟]");

        wraps("[24 Fluffy Fur]", "蓬鬆毛皮",
              "把 [{~} 蓬鬆毛皮] 交到討伐告示", "[{~} 蓬鬆毛皮]");
        wraps("[16 Salmon Oil]", "鮭魚油",
              "把 [{~} 鮭魚油] 或 [{~} 鮭魚肉] 交到", "[{~} 鮭魚油]");
        wraps("[24 Fluffy Fur]", "蓬鬆毛皮", "把 [24 蓬鬆毛皮] 交到", "[24 蓬鬆毛皮]");

        wraps("[24 Fluffy Fur]", "蓬鬆毛皮", "把 [上等 蓬鬆毛皮] 交到", null);

        wraps("[Cave]", "[洞窟]", "龍裔巢穴 [洞窟]", null);
        wraps("Nest [Cave]", "巢穴 洞窟", "龍裔巢穴 洞窟", null);
        wraps("[Cave] Lv. 30", "洞窟 30 級", "洞窟 30 級", null);
        wraps("[Cave]", "洞窟", "龍裔巢穴的洞窟", null);
        wraps("[Cave]", "洞窟", "龍裔巢穴 (洞窟)", null);
        wraps("[-1763, 136,", "座標", "座標", null);
        wraps("[]", "空", "[空]", null);
        wraps("[a] [b]", "甲乙", "[甲乙]", null);
        wraps("(Cave)", "洞窟", "龍裔巢穴 (洞窟)", null);
        wraps("[Cave]", "   ", "[   ]", null);
    }

    private static void wraps(String core, String zh, String translated, String want) {
        String got = LineTranslator.wrapLikeSource(core, zh, translated);
        check("「" + core + "」＋「" + zh + "」→ " + (want == null ? "不擴" : want)
              + "（拿到 " + got + "）", java.util.Objects.equals(got, want));
    }

    private static boolean is(Integer colour, int want) {
        return colour != null && colour == want;
    }

    private static String show(Integer c) {
        return c == null ? "null" : "#" + String.format("%06X", c);
    }

    private static String text(List<Component> built) {
        StringBuilder out = new StringBuilder();
        for (Component c : built) {
            out.append(c.getString());
        }
        return out.toString();
    }

    private static StyledText line(Object... pairs) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < pairs.length; i += 2) {
            out.append(Component.literal((String) pairs[i])
                    .withStyle(colour((Integer) pairs[i + 1])));
        }
        return StyledText.fromComponent(out);
    }

    private static String flat(List<Component> built) {
        StringBuilder out = new StringBuilder();
        for (Component c : built) {
            c.visit((style, t) -> {
                out.append(t);
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return out.toString();
    }

    private static Integer charColour(List<Component> built, int index) {
        List<Integer> hit = new ArrayList<>();
        int[] seen = {0};
        for (Component c : built) {
            c.visit((style, t) -> {
                if (hit.isEmpty() && index < seen[0] + t.length()) {
                    hit.add(style.getColor() == null
                            ? null : style.getColor().getValue() & 0xFFFFFF);
                }
                seen[0] += t.length();
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return hit.isEmpty() ? null : hit.get(0);
    }

    private static void dump(List<Component> built) {
        for (Component c : built) {
            StringBuilder row = new StringBuilder("  行：");
            c.visit((style, s) -> {
                row.append('[').append(s).append(' ')
                   .append(style.getColor() == null ? "null" : style.getColor().serialize())
                   .append(']');
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println(row);
        }
    }

    private static List<String> pieces(List<Component> built) {
        List<String> out = new ArrayList<>();
        for (Component c : built) {
            c.visit((style, s) -> {
                out.add(s);
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return out;
    }

    private static Integer colourOf(List<Component> built, String needle) {
        List<Integer> found = new ArrayList<>();
        for (Component c : built) {
            c.visit((style, s) -> {
                if (s.contains(needle) && style.getColor() != null) {
                    found.add(style.getColor().getValue() & 0xFFFFFF);
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "[通過] " : "[失敗] ") + name);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            throw new AssertionError(failures + " 項檢查未通過");
        }
        System.out.println("全部通過");
    }
}
