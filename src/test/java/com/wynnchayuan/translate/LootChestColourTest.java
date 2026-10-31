package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LootChestColourTest {

    private static final int GREY = 0xAAAAAA;
    private static final int WHITE = 0xFFFFFF;
    private static final int DARK = 0x555555;
    private static final int PINK = 0xFF55FF;
    private static final int PURPLE = 0xAA00AA;
    private static final int AQUA = 0x55FFFF;
    private static final int TEAL = 0x00AAAA;
    private static final int RED = 0xFF5555;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path root = Path.of(args.length > 0 ? args[0]
                                            : "src/main/resources/assets/wynnchayuan/translations");
        FlowedDebug.init(Files.createTempDirectory("wynnchayuan-loot-chest"));
        TranslationStore cn = new TranslationStore();
        cn.loadAll(List.of(root.resolve("zh_tw"), root.resolve("zh_cn")));
        TranslationStore tw = new TranslationStore();
        tw.loadAll(List.of(root.resolve("zh_tw")));

        StyledText plain = styled(
                lit("Loot Chest ", GREY), lit("[", GREY), lit("✫", WHITE),
                lit("✫✫✫", DARK), lit("]", GREY));
        for (Entry e : entries(plain, cn)) {
            Component out = e.out();
            check(e.path() + "：一級寶箱翻得出來", out != null);
            if (out == null) {
                continue;
            }
            check(e.path() + "：字面與原文同一個格式（實際 " + out.getString() + "）",
                  "战利品宝箱 [✫✫✫✫]".equals(out.getString()));
            expect(e.path(), out, "战利品宝箱", GREY, false);
            expect(e.path(), out, "[", GREY, false);
            expect(e.path(), out, "]", GREY, false);
            expectStars(e.path(), out, new int[] {WHITE, DARK, DARK, DARK});
        }

        StyledText locked3 = styled(
                lit("Locked ", PINK), lit("Loot Chest [", PURPLE), lit("✫✫✫", PINK),
                lit("✫", DARK), lit("]", PURPLE), lit("\n", PURPLE),
                bold("SLAY!", RED), lit(" ", GREY), lit("Defeat a ", GREY), lit("Grume", WHITE));
        for (Entry e : entries(locked3, cn)) {
            Component out = e.out();
            check(e.path() + "：上鎖的三級寶箱翻得出來", out != null);
            if (out == null) {
                continue;
            }
            check(e.path() + "：兩行、字面照語料（實際 " + out.getString().replace("\n", "⏎") + "）",
                  "上锁的战利品宝箱 [✫✫✫✫]\n击杀！击败 凝块".equals(out.getString()));
            expect(e.path(), out, "上锁的", PINK, false);
            expect(e.path(), out, "战利品宝箱", PURPLE, false);
            expect(e.path(), out, "[", PURPLE, false);
            expect(e.path(), out, "]", PURPLE, false);
            expectStars(e.path(), out, new int[] {PINK, PINK, PINK, DARK});
            expect(e.path(), out, "击杀！", RED, true);
            expect(e.path(), out, "击败", GREY, false);
            expect(e.path(), out, "凝块", WHITE, false);
        }

        StyledText locked4 = styled(
                lit("Locked ", AQUA), lit("Loot Chest [", TEAL), lit("✫✫✫✫", AQUA),
                lit("]", TEAL), lit("\n", TEAL),
                bold("SLAY!", RED), lit(" Defeat a ", GREY), lit("Grume", WHITE));
        for (Entry e : entries(locked4, cn)) {
            Component out = e.out();
            check(e.path() + "：上鎖的四級寶箱翻得出來", out != null);
            if (out == null) {
                continue;
            }
            expect(e.path(), out, "上锁的", AQUA, false);
            expect(e.path(), out, "战利品宝箱", TEAL, false);
            expectStars(e.path(), out, new int[] {AQUA, AQUA, AQUA, AQUA});
            expect(e.path(), out, "击杀！", RED, true);
            expect(e.path(), out, "击败", GREY, false);
            expect(e.path(), out, "凝块", WHITE, false);
        }

        StyledText mobs = styled(
                lit("Locked ", PINK), lit("Loot Chest [", PURPLE), lit("✫✫✫", PINK),
                lit("✫", DARK), lit("]", PURPLE), lit("\n", PURPLE),
                bold("SLAY!", RED), lit(" Defeat ", GREY), lit("3/5", WHITE),
                lit(" Mobs", GREY));
        for (Entry e : entries(mobs, cn)) {
            Component out = e.out();
            check(e.path() + "：帶數值的寶箱翻得出來", out != null);
            if (out == null) {
                continue;
            }
            check(e.path() + "：數值填回去（實際 " + out.getString().replace("\n", "⏎") + "）",
                  out.getString().endsWith("击败 3/5 只 怪物"));
            expect(e.path(), out, "上锁的", PINK, false);
            expect(e.path(), out, "击杀！", RED, true);
            expect(e.path(), out, "3", WHITE, false);
        }

        StyledText seal = styled(
                lit("Locked ", PINK), lit("Loot Chest [", PURPLE), lit("✫✫✫", PINK),
                lit("✫", DARK), lit("]", PURPLE), lit("\n", PURPLE),
                bold("SLAY!", RED), lit(" Defeat a ", GREY), lit("Voltaic Seal", WHITE));
        for (Entry e : entries(seal, tw)) {
            Component out = e.out();
            check(e.path() + "：繁中上鎖寶箱翻得出來", out != null);
            if (out == null) {
                continue;
            }
            expect(e.path(), out, "上鎖的", PINK, false);
            expect(e.path(), out, "戰利品寶箱", PURPLE, false);
            expectStars(e.path(), out, new int[] {PINK, PINK, PINK, DARK});
            expect(e.path(), out, "擊殺！", RED, true);
            expect(e.path(), out, "擊敗", GREY, false);
            expect(e.path(), out, "[", PURPLE, false);
            expect(e.path(), out, "]", PURPLE, false);
        }

        StyledText guardian = styled(
                lit("Locked ", PINK), lit("Loot Chest [", PURPLE), lit("✫✫✫✫", PINK),
                lit("]", PURPLE), lit("\n", PURPLE),
                bold("SLAY!", RED), lit(" Defeat a ", GREY), lit("Sanctuary Guardian", WHITE));
        for (TranslationStore store : List.of(tw, cn)) {
            for (Entry e : entries(guardian, store)) {
                Component out = e.out();
                check("★ " + e.path() + "：聖域守護者的寶箱翻得出來", out != null);
                if (out == null) {
                    continue;
                }
                expect("★ " + e.path(), out, "[", PURPLE, false);
                expect("★ " + e.path(), out, "]", PURPLE, false);
                expectStars("★ " + e.path(), out, new int[] {PINK, PINK, PINK, PINK});
            }
        }

        StyledText spaced = StyledText.fromString("\u00a7fStand here to\n\u00a7fdefend the elder\n\u00c0");
        for (Entry e : entries(spaced, tw)) {
            String got = e.out() == null ? "" : e.out().getString();
            check("★ " + e.path() + "：結尾多一列 À 也查得到兩行的條目（實際 " + got.replace("\n", "⏎") + "）",
                    got.contains("保護長老") && got.endsWith("\n\u00c0"));
        }

        System.out.println(failures == 0 ? "\n寶箱漂浮字的顏色：全部通過"
                : "\n寶箱漂浮字的顏色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private record Entry(String path, Component out) {}

    private static List<Entry> entries(StyledText label, TranslationStore store) {
        System.out.println("原文");
        for (String row : describe(label.getComponent())) {
            System.out.println("  " + row);
        }
        List<Entry> out = new ArrayList<>();
        out.add(new Entry("translateLabel", LineTranslator.translateLabel(label, store)));
        out.add(new Entry("translateFloating", LineTranslator.translateFloating(label, store)));
        for (Entry e : out) {
            System.out.println(e.path());
            if (e.out() == null) {
                System.out.println("  （沒翻出來）");
            } else {
                for (String row : describe(e.out())) {
                    System.out.println("  " + row);
                }
            }
        }
        return out;
    }

    private static void expect(String path, Component out, String text, int colour,
                               boolean bold) {
        List<Style> styles = stylesOf(out, text, 0);
        boolean ok = styles != null;
        String seen = "找不到";
        if (styles != null) {
            StringBuilder sb = new StringBuilder();
            for (Style s : styles) {
                ok &= colour(s) == colour && s.isBold() == bold;
                sb.append(hex(colour(s))).append(s.isBold() ? "粗" : "").append(' ');
            }
            seen = sb.toString().strip();
        }
        check(path + "：「" + text + "」是 " + hex(colour) + (bold ? " 粗體" : "")
              + "（實際 " + seen + "）", ok);
    }

    private static void expectStars(String path, Component out, int[] colours) {
        List<Style> styles = stylesOf(out, "✫✫✫✫", 0);
        boolean ok = styles != null && styles.size() == colours.length;
        StringBuilder want = new StringBuilder();
        StringBuilder seen = new StringBuilder();
        for (int i = 0; i < colours.length; i++) {
            want.append(hex(colours[i])).append(' ');
            if (styles != null && i < styles.size()) {
                seen.append(hex(colour(styles.get(i)))).append(' ');
                ok &= colour(styles.get(i)) == colours[i];
            }
        }
        check(path + "：星星 " + want.toString().strip()
              + "（實際 " + (styles == null ? "找不到" : seen.toString().strip()) + "）", ok);
    }

    private static List<Style> stylesOf(Component out, String text, int from) {
        StringBuilder plain = new StringBuilder();
        List<Style> at = new ArrayList<>();
        out.visit((style, piece) -> {
            for (int i = 0; i < piece.length(); i++) {
                plain.append(piece.charAt(i));
                at.add(style);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        int found = plain.indexOf(text, from);
        return found < 0 ? null : at.subList(found, found + text.length());
    }

    private static int colour(Style style) {
        return style.getColor() == null ? -2 : style.getColor().getValue();
    }

    private static List<String> describe(Component text) {
        List<String> out = new ArrayList<>();
        text.visit((style, piece) -> {
            out.add(hex(colour(style)) + (style.isBold() ? " 粗" : "")
                    + "  「" + piece.replace("\n", "\\n") + "」");
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static String hex(int colour) {
        return colour == -2 ? "繼承" : String.format("#%06X", colour);
    }

    private static StyledText styled(MutableComponent... parts) {
        MutableComponent all = Component.empty();
        for (MutableComponent part : parts) {
            all.append(part);
        }
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static MutableComponent bold(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)).withBold(true));
    }

}
