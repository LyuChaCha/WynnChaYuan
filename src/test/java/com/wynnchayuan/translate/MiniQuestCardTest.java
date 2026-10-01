package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 迷你任務卡片的說明段：底色是灰的，只有道具與座標有自己的顏色。
 *
 * <h2>實機回報</h2>
 * 「採集鯉魚 II」那張卡，說明的<b>第一列</b>顏色不對——應該跟後面兩列一樣是灰的。
 * 英文那三列長這樣（{@code majorid-debug} 的「可用的顏色 34」）：
 *
 * <pre>
 *   §7Bring §3[30 Carp Oil]§7 or §3[30 Carp
 *   §3Meat]§7 to the Gathering Post at
 *   §f[1094, 43, -1356]
 * </pre>
 *
 * 整段命中之後重新折行成三列中文，色段是照<b>文字比對</b>貼回去的：
 * 灰的「Bring」在譯文裡根本不存在，貼不上；貼不上的那一段就該留底色（灰），
 * 不能拿別段的顏色去補。
 */
public final class MiniQuestCardTest {

    private static int failures = 0;

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
        store.setNameMode(CollectorConfig.ItemNames.ON);
        // 要量得到寬度，說明段才會像實機那樣折成三列——顏色是<b>折完之後</b>
        // 才貼回每一列的，不折就測不到那一步。
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
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Gather Carp II ").withStyle(of(0xB38FAD))
                .append(Component.literal("[Mini-Quest]").withStyle(of(GREY))));
        lines.add(Component.literal("Currently in progress").withStyle(of(0x55FF55)));
        lines.add(Component.literal(" ").withStyle(of(GREY)));
        lines.add(row("Bring ", GREY, "[30 Carp Oil]", AQUA, " or ", GREY, "[30 Carp", AQUA));
        lines.add(row("Meat]", AQUA, " to the Gathering Post at", GREY));
        lines.add(row("[1094, 43, -1356]", WHITE));

        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(lines, store);
        System.out.println("== 採集鯉魚 II（原文 " + lines.size() + " 行，譯文 " + out.size() + " 行）");
        for (Component line : out) {
            System.out.println("  " + describe(line));
        }

        // 說明段是輸出的最後三列（前面是標題、狀態、分隔）。
        List<Component> body = out.subList(Math.max(0, out.size() - 3), out.size());
        for (Component line : body) {
            Integer lead = leadColour(line);
            check("★「" + plain(line) + "」開頭是灰的（實際 "
                          + (lead == null ? "空的" : String.format("#%06X", lead)) + "）",
                  lead != null && lead == GREY);
        }
    }

    /**
     * 這一列<b>開頭</b>那段字的顏色。
     *
     * <p>使用者回報的是「第一排的顏色不對」——折行之後每一列的開頭都是說明本文，
     * 底色該是灰的。方括號裡的道具與座標有自己的顏色，不看。
     */
    private static Integer leadColour(Component line) {
        Integer[] found = {null};
        line.visit((style, text) -> {
            if (found[0] != null || text.isBlank()) {
                return java.util.Optional.empty();
            }
            TextColor colour = style.getColor();
            found[0] = colour == null ? -1 : (colour.getValue() & 0xFFFFFF);
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    /** 照 layout-debug 反推的假字型：中日文 9px，英數大多 6px。 */
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

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
