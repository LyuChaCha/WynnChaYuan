package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MiniQuestCardTest {

    private static final int GREY = 0xAAAAAA;
    private static final int AQUA = 0x00AAAA;
    private static final int WHITE = 0xFFFFFF;

    private static Style of(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }

    public static void main(String[] args) {
        Path root = Path.of(args.length > 0 ? args[0]
                                            : "src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        List<Path> dirs = new ArrayList<>();
        dirs.add(root.resolve("zh_tw"));
        store.loadAll(dirs);
        store.setNameMode(ModConfig.ItemNames.ON);
        LineTranslator.measureForTest = MiniQuestCardTest::measure;
        try {
            run(store);
        } finally {
            LineTranslator.measureForTest = null;
        }
        System.out.println(failures == 0
                ? "迷你任務卡片的說明段：全部通過"
                : "迷你任務卡片的說明段：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void run(TranslationStore store) {
        carp(store);
        oakLogs(store);
    }

    private static void carp(TranslationStore store) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Gather Carp II ").withStyle(of(0xB38FAD))
                .append(Component.literal("[Mini-Quest]").withStyle(of(GREY))));
        lines.add(Component.literal("Currently in progress").withStyle(of(0x55FF55)));
        lines.add(Component.literal(" ").withStyle(of(GREY)));
        lines.add(row("Bring ", GREY, "[30 Carp Oil]", AQUA, " or ", GREY, "[30 Carp", AQUA));
        lines.add(row("Meat]", AQUA, " to the Gathering Post at", GREY));
        lines.add(row("[1094, 43, -1356]", WHITE));
        check3("採集鯉魚 II", store, lines);
    }

    private static void oakLogs(TranslationStore store) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Gather Oak Logs ").withStyle(of(0xB38FAD))
                .append(Component.literal("[Mini-Quest]").withStyle(of(GREY))));
        lines.add(Component.literal("Currently in progress").withStyle(of(0x55FF55)));
        lines.add(Component.literal(" ").withStyle(of(GREY)));
        lines.add(row("Bring ", GREY, "[12 Oak Wood]", AQUA, " or ", GREY, "[12 Oak", AQUA));
        lines.add(row("Paper]", AQUA, " to the Gathering Post at", GREY));
        lines.add(row("[-169, 71, -1572]", WHITE));
        check3("採集橡木原木", store, lines);
    }

    private static void check3(String what, TranslationStore store, List<Component> lines) {
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(lines, store);
        System.out.println("== " + what + "（原文 " + lines.size() + " 行，譯文 " + out.size() + " 行）");
        for (Component line : out) {
            System.out.println("  " + describe(line));
        }
        for (String prose : new String[] {"繳交", "座標"}) {
            Integer colour = colourOf(out, prose);
            check("★ " + what + "「" + prose + "」是灰的（實際 "
                          + (colour == null ? "找不到" : String.format("#%06X", colour)) + "）",
                  colour != null && colour == GREY);
        }
    }

    private static Integer colourOf(List<Component> lines, String needle) {
        Integer[] found = {null};
        for (Component line : lines) {
            line.visit((style, text) -> {
                if (found[0] == null && text.contains(needle)) {
                    TextColor colour = style.getColor();
                    found[0] = colour == null ? -1 : (colour.getValue() & 0xFFFFFF);
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return found[0];
    }

    static int measure(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                w[0] += SpaceOffset.decode(text);
            } else {
                text.codePoints().forEach(cp -> w[0] += advance(cp));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }

    private static int advance(int cp) {
        if (SpaceOffset.isOffset(cp)) {
            return SpaceOffset.decode(new String(Character.toChars(cp)));
        }
        return switch (cp) {
            case '\n' -> 0;
            case 'i', '.', ':', ',', ';', '!', '\'', '[', ']' -> 2;
            case 'l' -> 3;
            case 't', 'I', ' ' -> 4;
            case 'f', 'k' -> 5;
            default -> cp >= 0x2E80 ? 9 : 6;
        };
    }

    private static Component row(Object... parts) {
        MutableComponent c = Component.empty();
        for (int i = 0; i < parts.length; i += 2) {
            c.append(Component.literal((String) parts[i])
                    .withStyle(of((Integer) parts[i + 1])));
        }
        return c;
    }

    private static String plain(Component line) {
        return line.getString().replaceAll("[\\x{D0000}-\\x{DFFFF}\\x{CF000}-\\x{CFFFF}]", "");
    }

    private static String describe(Component line) {
        StringBuilder sb = new StringBuilder();
        line.visit((style, text) -> {
            TextColor colour = style.getColor();
            sb.append('[')
              .append(colour == null ? "繼承" : String.format("#%06X", colour.getValue() & 0xFFFFFF))
              .append(' ')
              .append(text.replaceAll("[\\x{D0000}-\\x{DFFFF}\\x{CF000}-\\x{CFFFF}]", "·"))
              .append(']');
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

}
