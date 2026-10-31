package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;
import com.wynntils.core.text.StyledText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ItemGuessListTest {

    private static final String ICON = String.valueOf((char) 0xE000);

    private static final String INDENT = "        ";

    private static final String[] NAMES = {"Pendant of the Ember", "Pendant of the Gale",
            "Pendant of the Squall", "Pendant of the Brine"};

    private static final String[] SHOWN = {"甲墜", "乙墜", "丙墜", "丁墜"};

    private static final String BARE = "Gloomshard";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wcy-item-guess");
        Files.writeString(dir.resolve("gear-accessory.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Pendant of the Ember", "dst": "甲墜", "role": "name"},
                   "a2": {"src": "Pendant of the Gale", "dst": "乙墜", "role": "name"},
                   "a3": {"src": "Pendant of the Squall", "dst": "丙墜", "role": "name"},
                   "a4": {"src": "Pendant of the Brine", "dst": "丁墜", "role": "name"},
                   "a5": {"src": "Gloomshard", "dst": "", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"), """
                {"This item's power has been sealed,": "此物品已封印，",
                 "Gloomshard": "技能名"}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        LineTranslator.measureForTest = AbilityTooltipRenderTest::measure;
        try {
            store.setNameMode(ModConfig.ItemNames.BOTH);
            oneNamePerRow(store);
            twoNamesPerRow(store);
            bareGearStaysEnglish(store);
            notAList(store);
            store.setNameMode(ModConfig.ItemNames.ON);
            translationOnly(store);
            store.setNameMode(ModConfig.ItemNames.OFF);
            namesOff(store);
        } finally {
            LineTranslator.measureForTest = null;
        }
        System.out.println(failures == 0 ? "物品猜測的候選清單：全部通過"
                                         : "物品猜測的候選清單：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void oneNamePerRow(TranslationStore store) {
        List<Component> t = tooltip(NAMES, 1);
        List<Component> out = translate(t, store);
        int first = t.size() - NAMES.length;
        for (int k = 0; k < NAMES.length; k++) {
            String want = INDENT + SHOWN[k] + " (" + NAMES[k] + ")"
                    + (k < NAMES.length - 1 ? "," : "");
            same("★ 第 " + (k + 1) + " 個名字：附原文、縮排與逗號照原文",
                    want, row(out, first + k));
        }
        Component last = out.get(first + NAMES.length - 1);
        check("★ 最後一列的縮排還在（實際 " + show(last) + "）",
                last.getString().startsWith(INDENT));
        check("名字照原文的顏色（實際 " + colourOf(out.get(first), SHOWN[0]) + "）",
                ChatFormatting.AQUA.getColor().equals(colourOf(out.get(first), SHOWN[0])));
        check("收藏的那一件照樣有底線", underlined(out.get(first + 1), SHOWN[1])
                && !underlined(out.get(first), SHOWN[0]));
        same("標題與等級列不動", t.get(first - 1).getString(), row(out, first - 1));
    }

    private static void twoNamesPerRow(TranslationStore store) {
        List<Component> t = tooltip(NAMES, 2);
        List<Component> out = translate(t, store);
        int first = t.size() - 2;
        same("★ 一列兩個：每一格都附原文",
                INDENT + SHOWN[0] + " (" + NAMES[0] + "), " + SHOWN[1] + " (" + NAMES[1] + "),",
                row(out, first));
        same("★ 最後一列：縮排還在",
                INDENT + SHOWN[2] + " (" + NAMES[2] + "), " + SHOWN[3] + " (" + NAMES[3] + ")",
                row(out, first + 1));
    }

    private static void bareGearStaysEnglish(TranslationStore store) {
        String[] names = {NAMES[0], BARE, NAMES[3]};
        List<Component> t = tooltip(names, 1);
        List<Component> out = translate(t, store);
        int first = t.size() - names.length;
        same("★ 還沒翻的裝備名留原文", INDENT + BARE + ",", row(out, first + 1));
        same("★ 旁邊翻好的照翻", INDENT + SHOWN[0] + " (" + NAMES[0] + "),", row(out, first));
        same("最後一列照翻", INDENT + SHOWN[3] + " (" + NAMES[3] + ")", row(out, first + 2));
        List<Component> two = translate(tooltip(new String[] {BARE, NAMES[1]}, 2), store);
        same("★ 同一列裡也是逐格判斷",
                INDENT + BARE + ", " + SHOWN[1] + " (" + NAMES[1] + ")",
                row(two, two.size() - 1));
    }

    private static void notAList(TranslationStore store) {
        check("名字後面接著別的字：不是清單列",
                GuessListRow.of(styled(INDENT + NAMES[0] + " is here"), store) == null);
        check("沒有縮排：不是清單列（敘述裡單獨一行的同名字照舊不附原文）",
                GuessListRow.of(styled(NAMES[0]), store) == null);
        check("有一格不是物品名稱：不是清單列",
                GuessListRow.of(styled(INDENT + NAMES[0] + ", Something Else"), store) == null);
        List<Component> t = new ArrayList<>();
        t.add(nameRow("Badge Fixture"));
        t.add(Component.empty());
        t.add(Component.literal("This item's power has been sealed,"));
        t.add(Component.literal(NAMES[0]));
        List<Component> out = translate(t, store);
        same("敘述裡的同名字不附原文", SHOWN[0], row(out, 3));
    }

    private static void translationOnly(TranslationStore store) {
        List<Component> t = tooltip(NAMES, 1);
        List<Component> out = translate(t, store);
        int first = t.size() - NAMES.length;
        same("只給譯名", INDENT + SHOWN[0] + ",", row(out, first));
        same("★ 只給譯名：最後一列的縮排還在", INDENT + SHOWN[3], row(out, first + 3));
    }

    private static void namesOff(TranslationStore store) {
        List<Component> t = tooltip(NAMES, 1);
        List<Component> out = translate(t, store);
        int first = t.size() - NAMES.length;
        for (int k = 0; k < NAMES.length; k++) {
            same("關掉物品名稱：第 " + (k + 1) + " 列留原文", t.get(first + k).getString(),
                    row(out, first + k));
        }
    }

    private static List<Component> translate(List<Component> t, TranslationStore store) {
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(t, store);
        if (out.isEmpty()) {
            return t;
        }
        check("譯文跟原文一樣多行（" + t.size() + " → " + out.size() + "）",
                out.size() == t.size());
        return out;
    }

    private static List<Component> tooltip(String[] names, int perRow) {
        List<Component> t = new ArrayList<>();
        t.add(nameRow("Unidentified Fixture"));
        t.add(Component.literal("This item's power has been sealed,")
                .withStyle(ChatFormatting.GRAY));
        t.add(Component.empty());
        t.addAll(guess(names, perRow));
        return t;
    }

    private static Component nameRow(String name) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(ICON).withStyle(Style.EMPTY.withFont(
                new FontDescription.Resource(Identifier.parse("minecraft:language/wynncraft")))));
        line.append(Component.literal(name).withStyle(ChatFormatting.AQUA));
        line.append(Component.literal(ICON).withStyle(Style.EMPTY.withFont(
                new FontDescription.Resource(Identifier.parse("minecraft:tooltip/emblem/frame")))));
        return line;
    }

    private static List<Component> guess(String[] names, int perRow) {
        List<Component> out = new ArrayList<>();
        out.add(Component.literal("- ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("可能性：").withStyle(ChatFormatting.GRAY)));
        MutableComponent level = Component.literal("    ");
        level.append(Component.literal("- ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("等級 88").withStyle(ChatFormatting.GRAY)));
        level.append(Component.literal(" [").append(Component.literal("408 ²")
                .withStyle(ChatFormatting.GREEN)).append("]").withStyle(ChatFormatting.GRAY));
        level.append(StyledText.fromString("§7: ").getComponent());
        MutableComponent line = level;
        for (int i = 0; i < names.length; i++) {
            MutableComponent name = Component.literal(names[i]).withStyle(ChatFormatting.AQUA);
            if (i == 1) {
                name.withStyle(ChatFormatting.UNDERLINE);
            }
            if (i % perRow == 0) {
                if (i != 0) {
                    line.append(",");
                }
                out.add(line);
                line = Component.empty().append(INDENT).append(name);
            } else {
                line.append(Component.literal(", ")).append(name);
            }
        }
        out.add(line);
        return out;
    }

    private static StyledText styled(String text) {
        return StyledText.fromComponent(Component.literal(text));
    }

    private static String row(List<Component> out, int k) {
        return k < out.size() ? out.get(k).getString() : null;
    }

    private static Integer colourOf(Component line, String text) {
        Integer[] found = {null};
        line.visit((style, s) -> {
            if (s.contains(text)) {
                TextColor c = style.getColor();
                found[0] = c == null ? null : c.getValue();
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static boolean underlined(Component line, String text) {
        boolean[] found = {false};
        line.visit((style, s) -> {
            if (s.contains(text)) {
                found[0] = style.isUnderlined();
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static String show(Component c) {
        return "|" + c.getString() + "|";
    }

    private static void same(String what, String want, String got) {
        check(what + "（實際 |" + got + "|）", java.util.Objects.equals(want, got));
    }

}
