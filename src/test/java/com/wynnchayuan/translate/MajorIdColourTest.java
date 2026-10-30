package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MajorIdColourTest {

    private static int failures = 0;

    private static final int NAME_COLOUR = 0xFF55FF;
    private static final int BODY_COLOUR = 0xAAAAAA;
    private static final int MARK_COLOUR = 0xFFFF55;

    private static List<StyledText> block() {
        return block(false);
    }

    private static List<StyledText> block(boolean markSeparate) {
        String[][] rows = {
            {"✦ Altruism: ", "Whenever you gain"},
            {null, "health from your Life Steal"},
            {null, "and Health Regen IDs, allies"},
            {null, "within 16 blocks will receive"},
            {null, "the same amount."},
        };
        List<StyledText> out = new ArrayList<>();
        for (String[] row : rows) {
            MutableComponent line = Component.empty();
            if (row[0] != null) {
                String name = row[0];
                if (markSeparate) {
                    line.append(Component.literal("✦ ").withStyle(
                            Style.EMPTY.withColor(TextColor.fromRgb(MARK_COLOUR))));
                    name = name.substring("✦ ".length());
                }
                line.append(Component.literal(name).withStyle(
                        Style.EMPTY.withColor(TextColor.fromRgb(NAME_COLOUR))));
            }
            line.append(Component.literal(row[1]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
            out.add(StyledText.fromComponent(line));
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        Path dir = java.nio.file.Files.createTempDirectory("wynnchayuan");
        FlowedDebug.init(dir);
        Path debug = dir.resolve("majorid-debug.txt");
        check("診斷檔一開場就存在", java.nio.file.Files.isRegularFile(debug));

        List<StyledText> run = block();
        List<Component> built = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);

        check("整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            report();
            return;
        }
        String all = built.stream().map(Component::getString)
                .reduce("", (a, b) -> a + b);
        check("名稱有翻出來（實際：" + all.substring(0, Math.min(24, all.length())) + "）",
                all.contains("利他主義"));
        Integer colour = colourOf(built, "利他主義");
        check("名稱查得到顏色", colour != null);
        if (colour != null) {
            check("名稱不是說明的顏色（拿到 #" + String.format("%06X", colour) + "）",
                    colour != BODY_COLOUR);
            check("名稱沿用原文名稱的顏色", colour == NAME_COLOUR);
        }
        List<StyledText> split = block(true);
        List<Component> two = LineTranslator.translateBlock(
                split, store, new boolean[split.size()]);
        check("✦ 獨立成段時整段仍查得到", two != null && !two.isEmpty());
        if (two != null && !two.isEmpty()) {
            Integer c = colourOf(two, "利他主義");
            check("✦ 獨立成段時名稱仍保住自己的顏色（拿到 "
                            + (c == null ? "null" : "#" + String.format("%06X", c)) + "）",
                    c != null && c == NAME_COLOUR);
        }
        List<StyledText> longName = new ArrayList<>();
        MutableComponent head = Component.empty();
        head.append(Component.literal("✦ Altruism: ").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(NAME_COLOUR))));
        head.append(Component.literal("Whenever").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
        longName.add(StyledText.fromComponent(head));
        for (String row : new String[] {"you gain health from your Life Steal",
                                        "and Health Regen IDs, allies",
                                        "within 16 blocks will receive",
                                        "the same amount."}) {
            longName.add(StyledText.fromComponent(Component.literal(row).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR)))));
        }
        List<Component> third = LineTranslator.translateBlock(
                longName, store, new boolean[longName.size()]);
        check("名稱比說明長時整段仍查得到", third != null && !third.isEmpty());
        if (third != null && !third.isEmpty()) {
            Integer c = colourOf(third, "利他主義");
            check("名稱比說明長時顏色不會跟說明對調（拿到 "
                            + (c == null ? "null" : "#" + String.format("%06X", c)) + "）",
                    c != null && c == NAME_COLOUR);
        }
        String written = java.nio.file.Files.readString(debug);
        check("跨行翻譯之後診斷檔有內容（" + written.length() + " 字）",
                written.contains("原文第一行的色段"));
        check("診斷檔記下了原文的色段", written.contains("#FF55FF"));
        check("診斷檔記下了每一次呼叫的流水號", written.contains("=== 1 ==="));
        String offset = new StringBuilder().appendCodePoint(0xD0002).toString();
        List<StyledText> real = new ArrayList<>();
        MutableComponent head2 = Component.empty();
        head2.append(Component.literal("").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6))));
        head2.append(Component.literal(offset + "Transcendence: ").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6))));
        head2.append(Component.literal("25% chance for").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
        real.add(StyledText.fromComponent(head2));
        for (String row : new String[] {"spells to cost no mana when", "casted."}) {
            real.add(StyledText.fromComponent(Component.literal(row).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)))));
        }
        List<Component> got = LineTranslator.translateBlock(
                real, store, new boolean[real.size()]);
        check("遊戲實際結構查得到（"
                        + (got == null || got.isEmpty() ? "null" : got.get(0).getString())
                        + "）", got != null && !got.isEmpty());
        if (got != null && !got.isEmpty()) {
            Integer c = colourOf(got, "超凡境界");
            check("遊戲實際結構下名稱保住自己的顏色（拿到 "
                            + (c == null ? "null" : "#" + String.format("%06X", c)) + "）",
                    c != null && c == 0xE0B3E6);
            Integer body = colourOf(got, "機率");
            check("說明那半沒有拿到名稱的顏色（拿到 "
                            + (body == null ? "null" : "#" + String.format("%06X", body))
                            + "）", body != null && body == 0xAAAAAA);
            for (Component one : got) {
                System.out.println("      輸出：" + one.getString());
                for (Component leaf : flatten(one)) {
                    System.out.println("        「" + leaf.getString() + "」 "
                            + (leaf.getStyle().getColor() == null ? "繼承"
                               : leaf.getStyle().getColor().toString()));
                }
            }
        }

        int[] rainbow = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF,
                         0x5555FF, 0xFF55FF, 0xFF5555, 0xFFAA00, 0xFFFF55,
                         0x55FF55, 0x55FFFF, 0x5555FF};
        List<StyledText> shiny = new ArrayList<>();
        MutableComponent head3 = Component.empty();
        String label = "Efflorescence";
        for (int i = 0; i < label.length(); i++) {
            head3.append(Component.literal(String.valueOf(label.charAt(i))).withStyle(
                    Style.EMPTY.withColor(
                            TextColor.fromRgb(rainbow[i % rainbow.length]))));
        }
        head3.append(Component.literal(": Serpent's Garden now centers to").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
        shiny.add(StyledText.fromComponent(head3));
        for (String row : new String[] {
                "where you land with Swan Dive. Damage of Swan Dive,",
                "Serpent's Garden and Jasmine Bloom are distributed",
                "across all elements."}) {
            shiny.add(StyledText.fromComponent(Component.literal(row).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR)))));
        }
        List<Component> painted = LineTranslator.translateBlock(
                shiny, store, new boolean[shiny.size()]);
        check("彩虹名稱的整段查得到", painted != null && !painted.isEmpty());
        if (painted != null && !painted.isEmpty()) {
            String zh = store.lookup("Efflorescence");
            int leaves = 0;
            java.util.LinkedHashSet<Integer> used = new java.util.LinkedHashSet<>();
            for (Component line : painted) {
                for (Component leaf : flatten(line)) {
                    if (!leaf.getString().contains(zh)) {
                        continue;
                    }
                    leaves++;
                    if (leaf.getStyle().getColor() != null) {
                        used.add(leaf.getStyle().getColor().getValue());
                    }
                }
            }
            check("譯名沒有被拆成一個字一段（拆成 " + leaves + " 段）", leaves == 1);
            check("譯名維持單一顏色，沒有被重新上色（用到 " + used.size() + " 個）",
                    used.size() == 1);
            check("用的是原文名稱本來的顏色", rainbowContains(rainbow, used));
            Integer bodyColour = colourOf(painted, tailWord(store.lookup(
                    "Serpent's Garden now centers to where you land with Swan Dive. "
                    + "Damage of Swan Dive, Serpent's Garden and Jasmine Bloom "
                    + "are distributed across all elements.")));
            check("說明那半沒有被染成彩虹（拿到 "
                            + (bodyColour == null ? "null"
                               : "#" + String.format("%06X", bodyColour)) + "）",
                    bodyColour != null && bodyColour == BODY_COLOUR);
        }
        List<StyledText> ordinary = block();
        List<Component> plain = LineTranslator.translateBlock(
                ordinary, store, new boolean[ordinary.size()]);
        check("普通的兩段上色仍然正確",
                plain != null && colourOf(plain, "利他主義") != null
                        && colourOf(plain, "利他主義") == NAME_COLOUR);

        keepWordColour(store);
        noStrayUnderline(store);
        tiedUnderline(store);
        bracketedNote(store);
        translatedTermKeepsColour(store);
        splitNameStillTranslates(store);
        wrappedSevenLines(store);
        skillPanelColours(store);
        report();
    }

    private static void skillPanelColours(TranslationStore store) {
        Style body = Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR));
        Style pink = Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF));
        Style white = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));

        List<StyledText> run = new ArrayList<>();
        run.add(StyledText.fromComponent(
                Component.literal("Each point in this skill will").withStyle(body)));
        MutableComponent second = Component.empty();
        second.append(Component.literal("increase").withStyle(pink));
        second.append(Component.literal(" the chance to dodge").withStyle(body));
        run.add(StyledText.fromComponent(second));
        run.add(StyledText.fromComponent(Component.literal(
                "attacks (90% damage reduction), ").withStyle(body)));
        MutableComponent fourth = Component.empty();
        fourth.append(Component.literal("and increase the ").withStyle(body));
        fourth.append(Component.literal(" Air").withStyle(white));
        fourth.append(Component.literal(" damage").withStyle(body));
        run.add(StyledText.fromComponent(fourth));
        run.add(StyledText.fromComponent(
                Component.literal("you may inflict").withStyle(body)));

        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("敏捷說明整段查得到", out != null && !out.isEmpty());
        if (out == null || out.isEmpty()) {
            return;
        }
        Integer element = colourOf(out, "風屬性");
        check("元素名保住原本的顏色（拿到 "
                        + (element == null ? "null"
                           : "#" + String.format("%06X", element)) + "）",
                element != null && element == 0xFFFFFF);
        Integer emphasis = colourOf(out, "提高");
        check("強調詞保住原本的顏色（拿到 "
                        + (emphasis == null ? "null"
                           : "#" + String.format("%06X", emphasis)) + "）",
                emphasis != null && emphasis == 0xFF55FF);
    }

    private static void splitNameStillTranslates(TranslationStore store) {
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR));
        Style underlined = grey.withUnderlined(true);

        MutableComponent first = Component.empty();
        first.append(Component.literal("Casting ").withStyle(grey));
        first.append(Component.literal("Frozen Tornado").withStyle(underlined));
        first.append(Component.literal(" inside your ").withStyle(grey));
        first.append(Component.literal("Dimensional").withStyle(underlined));

        MutableComponent second = Component.empty();
        second.append(Component.literal("Tear").withStyle(underlined));
        second.append(Component.literal(" infuses it with otherworldly energies,")
                .withStyle(grey));

        List<StyledText> run = List.of(
                StyledText.fromComponent(first),
                StyledText.fromComponent(second),
                StyledText.fromComponent(Component.literal(
                        "turning it into a Time Vortex.").withStyle(grey)));
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("時空渦流那段查得到", out != null && !out.isEmpty());
        if (out == null || out.isEmpty()) {
            return;
        }
        String all = out.stream().map(Component::getString).reduce("", (a, b) -> a + b);
        check("整段確實翻成了中文（實際：" + all.replace("\n", " ") + "）", hasHan(all));
        check("沒有留下半個英文名字",
                !all.contains("Dimensional") || all.contains("Dimensional Tear"));
    }

    private static void translatedTermKeepsColour(TranslationStore store) {
        final int TERM = 0x55FFFF;
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR));
        Style cyan = Style.EMPTY.withColor(TextColor.fromRgb(TERM));

        MutableComponent second = Component.empty();
        second.append(Component.literal("+8 Mana to your ").withStyle(grey));
        second.append(Component.literal("Mana Bank").withStyle(cyan));

        List<StyledText> run = List.of(
                StyledText.fromComponent(Component.literal(
                        "Pyrokinesis will add").withStyle(grey)),
                StyledText.fromComponent(second));
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("語料查得到 Mana Bank 的譯文",
                "魔力儲庫".equals(store.lookup("Mana Bank")));
        check("語料查得到 Unstable 的譯文",
                "失衡".equals(store.lookup("Unstable")));
        check("語料查得到 Riftspawn 的譯文",
                "裂隙之裔".equals(store.lookup("Riftspawn")));
        check("Mana Bank 有登記成可替換的詞",
                "魔力儲庫".equals(store.lookupTerm("Mana Bank")));
        check("詞後面黏著圖示也認得（拿到 "
                        + store.lookupTerm("Mana Bank ✺") + "）",
                "魔力儲庫 ✺".equals(store.lookupTerm("Mana Bank ✺")));
        check("Crystallized 有登記成可替換的詞",
                "結晶化".equals(store.lookupTerm("Crystallized")));
        check("一般的扁平檔不會被當成詞典", store.lookupTerm("Mastery Tomes") == null);
        if (out == null || out.isEmpty()) {
            return;
        }
        Integer colour = colourOf(out, "魔力儲庫");
        if (colour != null) {
            check("譯出來的「魔力儲庫」保住原文的顏色（拿到 #"
                    + String.format("%06X", colour) + "）", colour == TERM);
        }
    }

    private static void bracketedNote(TranslationStore store) {
        final int BODY = 0xAAAAAA;
        final int NOTE = 0x555555;
        Style body = Style.EMPTY.withColor(TextColor.fromRgb(BODY));
        Style note = Style.EMPTY.withColor(TextColor.fromRgb(NOTE));

        MutableComponent third = Component.empty();
        third.append(Component.literal("(When healing others, you can't heal")
                .withStyle(note));

        List<StyledText> run = List.of(
                StyledText.fromComponent(
                        Component.literal("Heals you and nearby allies in").withStyle(body)),
                StyledText.fromComponent(
                        Component.literal("a large area around you.").withStyle(body)),
                StyledText.fromComponent(third),
                StyledText.fromComponent(
                        Component.literal("more than 50% of their max health)")
                                .withStyle(note)));
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("Heal 那段查得到", out != null && !out.isEmpty());
        if (out == null || out.isEmpty()) {
            return;
        }
        Integer bodyColour = colourOf(out, "治療");
        check("正文沒有被染成註解的顏色（拿到 "
                        + (bodyColour == null ? "null"
                           : "#" + String.format("%06X", bodyColour)) + "）",
                bodyColour != null && bodyColour == BODY);
        Integer noteColour = colourOf(out, "無法");
        check("括號註解保住自己的顏色（拿到 "
                        + (noteColour == null ? "null"
                           : "#" + String.format("%06X", noteColour)) + "）",
                noteColour != null && noteColour == NOTE);
    }

    private static void tiedUnderline(TranslationStore store) {
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR));
        Style underlined = grey.withUnderlined(true);
        Style cyan = Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF));
        Style white = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));

        MutableComponent first = Component.empty();
        first.append(Component.literal("Meteor").withStyle(underlined));
        first.append(Component.literal(", ").withStyle(grey));
        first.append(Component.literal("Main Attack").withStyle(underlined));
        first.append(Component.literal(" and ").withStyle(grey));
        first.append(Component.literal("Powder Specials").withStyle(underlined));

        MutableComponent second = Component.empty();
        Style icon = Style.EMPTY.withColor(TextColor.fromRgb(0x7A3CFF))
                .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier.withDefaultNamespace("common")));
        second.append(Component.literal("consume ").withStyle(grey));
        second.append(Component.literal("Unstable ").withStyle(cyan));
        second.append(Component.literal("").withStyle(icon));
        second.append(Component.literal(" to deal ").withStyle(grey));
        second.append(Component.literal("+100%").withStyle(white));
        second.append(Component.literal(" damage.").withStyle(grey));

        List<StyledText> run = List.of(StyledText.fromComponent(first),
                                       StyledText.fromComponent(second));
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("祕術師那段查得到", out != null && !out.isEmpty());
        if (out == null || out.isEmpty()) {
            return;
        }
        check("內文「消耗」沒有底線", underlinedAt(out, "消耗") == Boolean.FALSE);
        check("內文「傷害」沒有底線", underlinedAt(out, "傷害") == Boolean.FALSE);
        check("術語 Meteor（或它的譯名）仍然有底線", underlinedAt(out, "隕石") == Boolean.TRUE
                || underlinedAt(out, "Meteor") == Boolean.TRUE);
    }

    private static Boolean underlinedAt(List<Component> lines, String needle) {
        for (Component line : lines) {
            for (Component leaf : flatten(line)) {
                if (leaf.getString().contains(needle)) {
                    return leaf.getStyle().isUnderlined();
                }
            }
        }
        return null;
    }

    private static void noStrayUnderline(TranslationStore store) {
        Style underlined = Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))
                                      .withUnderlined(true);
        Style plainGrey = Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR));
        MutableComponent second = Component.empty();
        second.append(Component.literal("from ").withStyle(plainGrey));
        second.append(Component.literal("Lightweaver").withStyle(underlined));
        second.append(Component.literal(" by +2.").withStyle(plainGrey));

        List<StyledText> run = List.of(
                StyledText.fromComponent(
                        Component.literal("Increase your Max Orbs").withStyle(plainGrey)),
                StyledText.fromComponent(second));
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("Halo 那段查得到", out != null && !out.isEmpty());
        if (out == null || out.isEmpty()) {
            return;
        }
        int underlinedLeaves = 0;
        int total = 0;
        for (Component line : out) {
            for (Component leaf : flatten(line)) {
                if (leaf.getString().isBlank()) {
                    continue;
                }
                total++;
                if (leaf.getStyle().isUnderlined()) {
                    underlinedLeaves++;
                }
            }
        }
        check("不是整段都畫上底線（" + underlinedLeaves + "/" + total + " 段有底線）",
                total > 0 && underlinedLeaves < total);
        check("該有底線的那個詞仍然有底線",
                underlinedLeaves >= 1);
    }

    private static void wrappedSevenLines(TranslationStore store) {
        String[] rows = {
            "Garden now centers to where",
            "you land with Swan Dive.",
            "Damage of Swan Dive,",
            "Serpent's Garden and Jasmine",
            "Bloom are distributed across",
            "all elements.",
        };
        MutableComponent head = Component.empty();
        head.append(Component.literal("").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6))));
        head.append(Component.literal(
                new StringBuilder().appendCodePoint(0xD0002) + "Efflorescence: ").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6))));
        head.append(Component.literal("Serpent's").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
        List<StyledText> run = new ArrayList<>();
        run.add(StyledText.fromComponent(head));
        for (String row : rows) {
            run.add(StyledText.fromComponent(Component.literal(row).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR)))));
        }
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("斷成七行時整段仍然查得到（"
                        + (out == null || out.isEmpty() ? "沒中"
                           : out.get(0).getString()) + "）",
                out != null && !out.isEmpty());
    }

    private static void keepWordColour(TranslationStore store) {
        final int WORD = 0x55FFFF;
        MutableComponent second = Component.empty();
        second.append(Component.literal("Mana Bank ").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(WORD))));
        second.append(Component.literal("✺").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF))));
        second.append(Component.literal(" by +30.").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));

        List<StyledText> run = List.of(
                StyledText.fromComponent(Component.literal("Increase your maximum")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR)))),
                StyledText.fromComponent(second));
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("技能敘述整段查得到", out != null && !out.isEmpty());
        if (out == null || out.isEmpty()) {
            return;
        }
        Integer word = colourOf(out, "魔力儲庫");
        check("行內最長的那個彩色詞沒有掉色（拿到 "
                        + (word == null ? "null" : "#" + String.format("%06X", word))
                        + "）", word != null && word == WORD);
        boolean bodyKept = false;
        for (Component leaf : flatten(out.get(out.size() - 1))) {
            String text = leaf.getString();
            if (!hasHan(text) || text.contains("魔力儲庫")) {
                continue;
            }
            bodyKept |= leaf.getStyle().getColor() != null
                    && leaf.getStyle().getColor().getValue() == BODY_COLOUR;
        }
        check("其餘敘述仍然是敘述的顏色", bodyKept);
    }

    private static boolean hasHan(String text) {
        return text.codePoints().anyMatch(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    private static boolean rainbowContains(int[] ramp, java.util.Set<Integer> used) {
        for (int colour : used) {
            boolean found = false;
            for (int candidate : ramp) {
                found |= candidate == colour;
            }
            if (!found) {
                return false;
            }
        }
        return !used.isEmpty();
    }

    private static String tailWord(String translated) {
        String t = translated == null ? "" : translated.replaceAll("[。，、！？\\s]+$", "");
        return t.length() <= 3 ? t : t.substring(t.length() - 3);
    }

    private static Integer colourOf(List<Component> lines, String needle) {
        for (Component line : lines) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)) {
                    TextColor colour = part.getStyle().getColor();
                    return colour == null ? null : colour.getValue();
                }
            }
        }
        return null;
    }

    private static List<Component> flatten(Component component) {
        List<Component> out = new ArrayList<>();
        if (component.getSiblings().isEmpty()) {
            out.add(component);
        }
        for (Component child : component.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void stems() {
        System.out.println("\n  -- 複數與所有格的詞幹 --");

        check("複數：Marks -> Mark", "Mark".equals(LineTranslator.stemOf("Marks")));
        check("複數：Mirror Clones -> Mirror Clone",
                "Mirror Clone".equals(LineTranslator.stemOf("Mirror Clones")));
        check("所有格：Multihit's -> Multihit",
                "Multihit".equals(LineTranslator.stemOf("Multihit's")));
        check("全形撇號也認得",
                "Dash".equals(LineTranslator.stemOf("Dash’s")));
        check("ss 結尾不當成複數（Progress）", LineTranslator.stemOf("Progress") == null);
        check("太短的不剝（Was）", LineTranslator.stemOf("Was") == null);
        check("沒有詞尾的回 null", LineTranslator.stemOf("Trick") == null);

        check("譯文裡自成一個詞才算",
                LineTranslator.standsAlone("擁有 2+ 層 Mark 的敵人", "Mark"));
        check("★ 卡在 Marked 裡面的不算",
                !LineTranslator.standsAlone("前置技能: Marked", "Mark"));
        check("句尾也算", LineTranslator.standsAlone("消耗所有 Mark", "Mark"));
        check("被中文夾著也算", LineTranslator.standsAlone("引爆 Trick，造成傷害", "Trick"));
        check("完全沒出現就不算", !LineTranslator.standsAlone("沒有這個詞", "Mark"));
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        stems();
        System.out.println(failures == 0
                ? "MajorIdColour: 全部通過" : "MajorIdColour: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
