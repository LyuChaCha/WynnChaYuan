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
        carp(store);
        oakLogs(store);
    }

    /** 採集鯉魚 II：說明折成三列，每一列開頭都是說明本文。 */
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

    /**
     * 採集橡木原木：折行的位置不一樣，<b>第一列只剩說明本文、一個方括號都沒有</b>。
     *
     * <h2>實機回報</h2>
     * 「到採集告示繳交」那一列是青色的，應該跟後面兩列一樣是灰的。鯉魚那張卡
     * 第一列折進了「[30 鯉魚油]」所以看不出來——同一段文字，只因為折在別的地方
     * 就變色，那是色段貼回去的時候錯位了。
     */
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
        // 方括號裡的道具與座標有自己的顏色，說明本文該是灰的。折在哪裡都一樣。
        //
        // 找的字<b>不要帶術語</b>。這裡本來寫「到採集站繳交」，Gathering Post 的
        // 譯名統一成「採集告示」之後整句就找不到，測試會紅在「找不到」而不是
        // 顏色不對——那是測試自己壞了，不是程式壞了。「繳交」「座標」是句子的
        // 骨架，換譯名也還在。
        for (String prose : new String[] {"繳交", "座標"}) {
            Integer colour = colourOf(out, prose);
            check("★ " + what + "「" + prose + "」是灰的（實際 "
                          + (colour == null ? "找不到" : String.format("#%06X", colour)) + "）",
                  colour != null && colour == GREY);
        }
    }

    /** 含有這段文字的那個片段是什麼顏色。 */
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
