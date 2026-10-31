package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class IngredientTooltipTest {

    private static final Style WHITE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style GREY = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
    private static final Style RED = Style.EMPTY.withColor(TextColor.fromRgb(0xFAACAC));

    private static final Style WYNN = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))
            .withFont(new FontDescription.Resource(
                    Identifier.withDefaultNamespace("language/wynncraft")));

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace(id)));
    }

    public static void main(String[] args) {
        Path root = Path.of(args.length > 0 ? args[0]
                                            : "src/main/resources/assets/wynnchayuan/translations");
        LineTranslator.measureForTest = IngredientTooltipTest::measure;
        try {
            TranslationStore store = new TranslationStore();
            List<Path> dirs = new ArrayList<>();
            dirs.add(root.resolve("zh_tw"));
            store.loadAll(dirs);
            store.setNameMode(ModConfig.ItemNames.OFF);
            run(store);
        } finally {
            LineTranslator.measureForTest = null;
        }
        centreFalseMatch();
        System.out.println(failures == 0
                ? "素材 tooltip 需求列：全部通過"
                : "素材 tooltip 需求列：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static List<Component> claw() {
        List<Component> lines = new ArrayList<>();
        lines.add(glyphs("󰀀", "Claw of Demise", "󰀀"));
        MutableComponent name = Component.empty();
        name.append(off(-16));
        name.append(Component.literal("").withStyle(font("tooltip/emblem/frame")));
        name.append(off(-49));
        name.append(Component.literal("").withStyle(font("tooltip/emblem/sprite")));
        name.append(off(5));
        name.append(Component.literal("Claw of Demise").withStyle(WHITE));
        lines.add(name);
        lines.add(band(38, ""));
        lines.add(Component.empty());
        lines.add(lead(2, "100 Crafting Level"));
        lines.add(lead(2, "Can be used in recipes for"));
        lines.add(professions());
        lines.add(divider(24));
        lines.add(statRow("Durability", -41, 141, "-165"));
        lines.add(statRow("Min. Defence", -54, 144, "+35"));
        lines.add(Component.empty());
        lines.add(iconStatRow("Defence", -46, 123, "-3 to -5"));
        lines.add(statRow("Fire Damage", -52, 99, "+23% to +26%"));
        lines.add(statRow("Health Regen", -56, 102, "-15% to -20%"));
        lines.add(divider(24));
        lines.add(band(0, ""));
        lines.add(Component.literal("Grants an effectiveness multiplier").withStyle(GREY));
        lines.add(Component.literal("to nearby ingredients when used in").withStyle(GREY));
        lines.add(Component.literal("a Crafted Item recipe").withStyle(GREY));
        lines.add(divider(22));
        for (String[] cells : new String[][] {
                {"-65%", "-65%", "-65%"},
                {"-65%", "0%", "-65%"},
                {"0%", null, "0%"},
                {"-65%", "0%", "-65%"},
                {"-65%", "-65%", "-65%"}}) {
            lines.add(grid(cells));
        }
        lines.add(Component.empty());
        return lines;
    }

    private static void run(TranslationStore store) {
        List<Component> lines = claw();
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(lines, store);
        boolean[] flags = BlockLayout.centered(lines, IngredientTooltipTest::measure);
        StringBuilder f = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            f.append(i).append(flags[i] ? "=\u7f6e\u4e2d " : "=\u9760\u5de6 ");
        }
        System.out.println("  \u7f6e\u4e2d\u5224\u65b7 " + f);
        int frame = 0;
        for (Component line : lines) {
            frame = Math.max(frame, measure(line));
        }
        System.out.println("  \u6574\u4efd\u6700\u5bec " + frame);
        for (int i = 0; i < lines.size(); i++) {
            System.out.println("  [" + i + "] \u539f " + measure(lines.get(i))
                    + (i < out.size() ? "  \u8b6f " + measure(out.get(i)) : "")
                    + "  " + clean(lines.get(i).getString()));
        }
        System.out.println("== 殞命之爪（原文 " + lines.size() + " 行，譯文 " + out.size() + " 行）");
        for (int i = 0; i < lines.size() && i < out.size(); i++) {
            String plain = lines.get(i).getString();
            if (!plain.startsWith("Durability") && !plain.startsWith("Min. Defence")
                    && !plain.startsWith("Fire Damage") && !plain.startsWith("Health Regen")) {
                continue;
            }
            int orig = firstTextX(lines.get(i));
            int made = firstTextX(out.get(i));
            System.out.println("  " + clean(out.get(i).getString())
                    + "  標籤起點 原文=" + orig + " 譯文=" + made);
            check("「" + plain.replaceAll("[^A-Za-z. ]", "").strip()
                          + "」標籤從原文的起點開始（原文 " + orig + "、譯文 " + made + "）",
                  made == orig);
        }
    }

    private static Component glyphs(String before, String text, String after) {
        MutableComponent c = Component.empty();
        c.append(Component.literal(before).withStyle(font("space")));
        c.append(Component.literal(text).withStyle(WHITE));
        c.append(Component.literal(after).withStyle(font("space")));
        return c;
    }

    private static Component lead(int px, String text) {
        MutableComponent c = Component.empty();
        c.append(off(px));
        c.append(Component.literal(text).withStyle(GREY));
        return c;
    }

    private static Component band(int px, String glyph) {
        MutableComponent c = Component.empty();
        if (px != 0) {
            c.append(off(px));
        }
        c.append(Component.literal(glyph).withStyle(font("banner/box")));
        return c;
    }

    private static Component divider(int px) {
        MutableComponent c = Component.empty();
        if (px != 0) {
            c.append(off(px));
        }
        c.append(Component.literal("").withStyle(font("tooltip/divider")));
        return c;
    }

    private static Component professions() {
        MutableComponent c = Component.empty();
        c.append(off(2));
        c.append(Component.literal("").withStyle(font("tooltip/profession/sprite")));
        c.append(off(-1));
        c.append(Component.literal(" Weaponsmithing").withStyle(GREY));
        c.append(off(4));
        c.append(Component.literal("").withStyle(font("tooltip/profession/sprite")));
        c.append(off(-2));
        c.append(Component.literal(" Woodworking").withStyle(GREY));
        return c;
    }

    private static Component statRow(String label, int back, int jump, String value) {
        MutableComponent c = Component.empty();
        c.append(Component.literal(label + SpaceOffset.encode(back) + SpaceOffset.encode(jump))
                .withStyle(WYNN));
        c.append(Component.literal(value).withStyle(RED.withFont(WYNN.getFont())));
        return c;
    }

    private static Component iconStatRow(String label, int back, int jump, String value) {
        MutableComponent c = Component.empty();
        c.append(off(-1));
        c.append(Component.literal("").withStyle(font("tooltip/attribute/sprite")));
        c.append(off(1));
        c.append(off(2));
        c.append(Component.literal(label + SpaceOffset.encode(back) + SpaceOffset.encode(jump))
                .withStyle(WYNN));
        c.append(Component.literal(value).withStyle(RED.withFont(WYNN.getFont())));
        return c;
    }

    private static Component grid(String[] cells) {
        MutableComponent c = Component.empty();
        c.append(off(26));
        for (String cell : cells) {
            c.append(off(6));
            if (cell == null) {
                c.append(Component.literal("").withStyle(font("tooltip/effectiveness")));
            } else {
                c.append(Component.literal(cell).withStyle(RED));
            }
            c.append(off(6));
        }
        return c;
    }

    private static Component off(int px) {
        return Component.literal(SpaceOffset.encode(px)).withStyle(SpaceOffset.styleFor(WHITE));
    }

    private static String clean(String s) {
        return s.replaceAll("[\\x{D0000}-\\x{DFFFF}\\x{CF000}-\\x{CFFFF}]", "·");
    }

    private static int firstTextX(Component c) {
        int[] x = {0};
        int[] found = {Integer.MIN_VALUE};
        c.visit((style, text) -> {
            if (found[0] != Integer.MIN_VALUE) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                x[0] += SpaceOffset.decode(text);
            } else if (!text.isBlank() && text.codePoints().allMatch(
                    cp -> cp >= 0xE000 && cp <= 0xF8FF)) {
                x[0] += measure(Component.literal(text));
            } else if (!text.isBlank()) {
                int lead = 0;
                while (lead < text.length() && text.charAt(lead) == ' ') {
                    lead++;
                }
                found[0] = x[0] + lead * 4;
            } else {
                x[0] += text.length() * 4;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static void centreFalseMatch() {
        List<Component> orig = new ArrayList<>();
        List<Component> made = new ArrayList<>();
        orig.add(twoCol(0, 47, 100, 24));
        orig.add(twoCol(0, 61, 90, 18));
        orig.add(twoCol(11, 41, 77, 49));
        orig.add(twoCol(0, 54, 47, 72));
        made.add(twoCol(0, 27, 120, 24));
        made.add(twoCol(0, 36, 115, 18));
        made.add(twoCol(11, 18, 100, 49));
        made.add(twoCol(0, 72, 29, 72));

        List<Component> out = com.wynnchayuan.translate.TooltipWiden.centreColumns(
                orig, made, IngredientTooltipTest::measure);
        for (int i = 0; i < out.size(); i++) {
            int want = firstTextX(made.get(i));
            int got = firstTextX(out.get(i));
            check("★ 靠左的需求列第 " + i + " 列沒有被當成置中欄推開"
                          + "（補償前 " + want + "、補償後 " + got + "）",
                  want == got);
        }
    }

    private static Component twoCol(int lead, int label, int gap, int value) {
        MutableComponent c = Component.empty();
        if (lead != 0) {
            c.append(off(lead));
        }
        c.append(Component.literal(fill(label)).withStyle(WHITE));
        c.append(off(gap));
        c.append(Component.literal(fill(value)).withStyle(RED));
        return c;
    }

    private static String fill(int px) {
        StringBuilder sb = new StringBuilder();
        int wide = px / 6;
        int left = px - wide * 6;
        if (left == 1 && wide > 0) {
            wide--;
            left = 7;
        }
        for (int i = 0; i < wide; i++) {
            sb.append('a');
        }
        for (int[] pair : new int[][] {{5, 'f'}, {4, 't'}, {3, 'l'}, {2, 'i'}}) {
            while (left >= pair[0]) {
                sb.append((char) pair[1]);
                left -= pair[0];
            }
        }
        if (left != 0) {
            throw new IllegalArgumentException("湊不出 " + px + " px");
        }
        return sb.toString();
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
            case 0xCF000 -> 0;
            case 0xE031 -> 66;
            case 0xE034 -> 16;
            case 0xE000 -> 112;
            case 0xE00A, 0xE00B -> 16;
            case 0xE013 -> 9;
            case 0xE002 -> 12;
            case 0xF000 -> 115;
            case 0xE100 -> 85;
            case 0xE101 -> 80;
            case 'i', '.', ':', ',', ';', '!', '\'' -> 2;
            case 'l' -> 3;
            case 't', 'I', ' ' -> 4;
            case 'f', 'k' -> 5;
            default -> cp >= 0x2E80 ? 9 : 6;
        };
    }

}
