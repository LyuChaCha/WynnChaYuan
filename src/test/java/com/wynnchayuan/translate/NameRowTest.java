package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynntils.core.text.StyledText;
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
import java.util.Optional;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class NameRowTest {

    private static final String ICON = String.valueOf((char) 0xE000);

    private static final String TAIL = String.valueOf((char) 0xE001);

    public static void main(String[] args) throws Exception {
        nameRowWrap();
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-name-row");
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "w1": {"src": "Warp", "dst": "越相杖", "role": "name"},
                   "w2": {"src": "Oak Wood Dagger", "dst": "橡木匕首", "role": "name"},
                   "w3": {"src": "Bonder", "dst": "", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Withdrawal", "dst": "戒斷", "role": "name"},
                   "a2": {"src": "Redemption", "dst": "救贖", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("ingredient.json"), """
                {"_meta": {"itemNames": true, "gearNames": false},
                 "entries": {
                   "i1": {"src": "Filched Purse", "dst": "扒來的錢包", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"), """
                {"{#}Warp{#}": "{#}越相杖{#}",
                 "{#}{#}{#}{#}{#}Warp [{~}]": "{#}{#}{#}{#}{#}越相杖 [{~}]",
                 "{#}{#}{#}Withdrawal{#}": "{#}{#}{#}Withdrawal{#}",
                 "{#}{#}{#}{#}{#}{#}{#}Withdrawal": "{#}{#}{#}{#}{#}{#}{#}Withdrawal",
                 "{#}Bonder{#}": "{#}Bonder{#}",
                 "{#}{#}{#}{#}{#}Filched Purse": "{#}{#}{#}{#}{#}扒來的錢包",
                 "{#}Redemption": "{#}救贖使命",
                 "{#}Dernic Pickaxe T{~}{#}": "{#}德尼鎬 T{~}{#}",
                 "{#}{#}{#}{#}{#}Dernic Pickaxe T{~}": "{#}{#}{#}{#}{#}德尼鎬 T{~}",
                 "Dernic Pickaxe T{~}": "德尼鎬 T{~}",
                 "{#} Finished buying Dernic Pickaxe T{~}.": "{#} 買好了德尼鎬 T{~}。",
                 "{#}{#}{#}Daily Reward{#}": "{#}{#}{#}每日獎勵{#}"}
                """, StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        same("第 0 行", "Warp", TranslationStore.nameRowInner("{#}Warp{#}"));
        same("已鑑定", "Warp", TranslationStore.nameRowInner("{#}{#}{#}{#}{#}Warp [{~}]"));
        same("三個圖示＋尾巴", "Withdrawal",
                TranslationStore.nameRowInner("{#}{#}{#}Withdrawal{#}"));
        same("七個圖示", "Hero", TranslationStore.nameRowInner("{#}{#}{#}{#}{#}{#}{#}Hero"));
        same("多字名稱", "Oak Wood Dagger",
                TranslationStore.nameRowInner("{#}{#}{#}{#}{#}Oak Wood Dagger [{~}]"));
        same("單一圖示開頭的不是名稱列", null, TranslationStore.nameRowInner("{#}Redemption"));
        same("裸名不是名稱列", null, TranslationStore.nameRowInner("Warp"));
        same("裡面還有佔位符的不是", null, TranslationStore.nameRowInner("{#}{#}{#}Lv. {~}{#}"));
        same("只有圖示的不是", null, TranslationStore.nameRowInner("{#}{#}{#}{#}"));

        store.setNameMode(com.wynnchayuan.ModConfig.ItemNames.BOTH);
        String warp0 = row(ICON, "Warp", TAIL, store);
        same("測試列的模板跟實機語料的鍵同形", "{#}Warp{#}", lastTemplate);
        check("★ 第 0 行附原文（實際 " + plain(warp0) + "）", warp0.contains("越相杖 (Warp)"));
        String warp1 = rolled("Warp", store);
        same("已鑑定那一列也同形", "{#}{#}{#}{#}{#}Warp [{~}]", lastTemplate);
        check("★ 看得見的那一行附原文（實際 " + plain(warp1) + "）",
                warp1.contains("越相杖 (Warp)"));
        String oak = rolled("Oak Wood Dagger", store);
        check("對照組：沒有整列條目的本來就附（實際 " + plain(oak) + "）",
                oak.contains("橡木匕首 (Oak Wood Dagger)"));
        check("原文只附一次（實際 " + plain(warp1) + "）",
                warp1.indexOf("(Warp)") == warp1.lastIndexOf("(Warp)"));

        store.setNameMode(com.wynnchayuan.ModConfig.ItemNames.ON);
        String withdrawal = row(ICON.repeat(3), "Withdrawal", TAIL, store);
        same("三個圖示那一列也同形", "{#}{#}{#}Withdrawal{#}", lastTemplate);
        check("★ 整列條目留英文時，譯名照樣出來（實際 " + plain(withdrawal) + "）",
                withdrawal.contains("戒斷") && !withdrawal.contains("Withdrawal"));
        String warpOn = rolled("Warp", store);
        check("只給譯名的模式不附原文（實際 " + plain(warpOn) + "）",
                warpOn.contains("越相杖") && !warpOn.contains("(Warp)"));

        store.setNameMode(com.wynnchayuan.ModConfig.ItemNames.OFF);
        String off = rolled("Warp", store);
        check("★ 關掉物品名稱時留原文（實際 " + plain(off) + "）",
                off.contains("Warp") && !off.contains("越相杖"));
        store.setNameMode(com.wynnchayuan.ModConfig.ItemNames.ON);

        String purse = row(ICON.repeat(5), "Filched Purse", "", store);
        same("素材那一列也同形", "{#}{#}{#}{#}{#}Filched Purse", lastTemplate);
        check("素材名稱平常是譯名（實際 " + plain(purse) + "）", purse.contains("扒來的錢包"));
        store.setPeekPlainNames(true);
        String peeked = row(ICON.repeat(5), "Filched Purse", "", store);
        check("★ 按住 Shift 時素材名稱是原文（實際 " + plain(peeked) + "）",
                peeked.contains("Filched Purse"));
        store.setPeekPlainNames(false);

        store.setNameMode(com.wynnchayuan.ModConfig.ItemNames.BOTH);
        String purseBoth = row(ICON.repeat(5), "Filched Purse", "", store);
        check("★ 譯名加原文：素材的名稱列附原文（實際 " + plain(purseBoth) + "）",
                purseBoth.contains("扒來的錢包 (Filched Purse)"));
        check("原文只附一次", purseBoth.indexOf("(Filched Purse)")
                == purseBoth.lastIndexOf("(Filched Purse)"));
        store.setPeekPlainNames(true);
        String purseBothPeek = row(ICON.repeat(5), "Filched Purse", "", store);
        check("★ 譯名加原文＋按住 Shift：只剩原文（實際 " + plain(purseBothPeek) + "）",
                purseBothPeek.contains("Filched Purse") && !purseBothPeek.contains("扒來的錢包"));
        store.setPeekPlainNames(false);
        store.setNameMode(com.wynnchayuan.ModConfig.ItemNames.ON);

        String tool = row(ICON.repeat(5), "Dernic Pickaxe T12", "", store);
        same("工具那一列的模板", "{#}{#}{#}{#}{#}Dernic Pickaxe T{~}", lastTemplate);
        check("工具名稱平常是譯名（實際 " + plain(tool) + "）", tool.contains("德尼鎬 T12"));
        String toolHidden = row(ICON, "Dernic Pickaxe T12", TAIL, store);
        check("看不見的第 0 行也是譯名（實際 " + plain(toolHidden) + "）",
                toolHidden.contains("德尼鎬 T12"));
        store.setPeekPlainNames(true);
        String toolPeek = row(ICON.repeat(5), "Dernic Pickaxe T12", "", store);
        check("★ 按住 Shift 時工具名稱是原文（實際 " + plain(toolPeek) + "）",
                toolPeek.contains("Dernic Pickaxe T12") && !toolPeek.contains("德尼鎬"));
        String toolHiddenPeek = row(ICON, "Dernic Pickaxe T12", TAIL, store);
        check("★ 第 0 行也跟著是原文（實際 " + plain(toolHiddenPeek) + "）",
                toolHiddenPeek.contains("Dernic Pickaxe T12") && !toolHiddenPeek.contains("德尼鎬"));
        check("★ 單獨的名稱也是原文", store.lookup("Dernic Pickaxe T{~}") == null);
        check("句子裡提到工具的不受影響（實際 "
                        + store.lookup("{#} Finished buying Dernic Pickaxe T{~}.") + "）",
                "{#} 買好了德尼鎬 T{~}。".equals(
                        store.lookup("{#} Finished buying Dernic Pickaxe T{~}.")));
        store.setPeekPlainNames(false);
        String toolAgain = row(ICON.repeat(5), "Dernic Pickaxe T12", "", store);
        check("放開之後回到譯名（實際 " + plain(toolAgain) + "）", toolAgain.contains("德尼鎬 T12"));
        check("形狀：材質＋工具＋T{~}，前後只能是圖示",
                TranslationStore.isToolName("{#}Golden Rod T{~}{#}")
                        && TranslationStore.isToolName("Stone Axe T{~}")
                        && !TranslationStore.isToolName("- {~}x Golden Pickaxe T{~}")
                        && !TranslationStore.isToolName("{#}Warp{#}")
                        && !TranslationStore.isToolName("Golden Rod"));

        same("Lootrun 使命名不受影響", "{#}救贖使命", store.lookup("{#}Redemption"));
        same("裡面不是物品名稱的整列條目不受影響", "{#}{#}{#}每日獎勵{#}",
                store.lookup("{#}{#}{#}Daily Reward{#}"));

        check("★ 沒翻的名稱那一列不記成缺口",
                store.hasTranslation("{#}{#}{#}{#}{#}Bonder [{~}]"));
        check("翻好的名稱那一列也不是缺口",
                store.hasTranslation("{#}{#}{#}Oak Wood Dagger{#}"));
        check("真的沒收過的整列還是缺口",
                !store.hasTranslation("{#}{#}{#}Never Seen Before{#}"));

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("NameRowTest 全部通過");
        System.exit(0);
    }

    private static String rolled(String name, TranslationStore store) {
        return row(ICON.repeat(5), name + " [58.2%]", "", store);
    }

    private static String row(String before, String text, String after, TranslationStore store) {
        net.minecraft.network.chat.MutableComponent line = Component.empty();
        icons(line, before, 0);
        line.append(Component.literal(text).withStyle(s -> s.withColor(0xAA00AA)));
        icons(line, after, before.length());
        StyledText styled = StyledText.fromComponent(line);
        String template = com.wynnchayuan.capture.LineParts.of(styled).template();
        lastTemplate = template;
        Component out = LineTranslator.translate(styled, store);
        return out == null ? line.getString() : out.getString();
    }

    private static String lastTemplate = "";

    private static void icons(net.minecraft.network.chat.MutableComponent line, String icons,
            int offset) {
        for (int i = 0; i < icons.length(); i++) {
            String font = (offset + i) % 2 == 0
                    ? "minecraft:language/wynncraft" : "minecraft:tooltip/emblem/frame";
            net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY
                    .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                            net.minecraft.resources.Identifier.parse(font)));
            line.append(Component.literal(String.valueOf((char) (0xE000 + offset + i)))
                    .withStyle(style));
        }
    }

    private static String plain(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(
                cp -> sb.appendCodePoint(cp >= 0xE000 && cp <= 0xF8FF ? '#' : cp));
        return sb.toString();
    }

    private static void same(String what, String want, String got) {
        check(what + "（實際 " + got + "）", java.util.Objects.equals(want, got));
    }

    private static void nameRowWrap() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-name-row-wrap");
        Files.createDirectories(dir.resolve("scoped"));
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "w1": {"src": "Glimmerwake", "dst": "微光醒", "role": "name"},
                   "w2": {"src": "Thornbound Moonlit Reliquary", "dst": "棘縛月照聖匣", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scoped").resolve("name.json"), """
                {"_meta": {"scope": "name"},
                 "Shiny": "耀光之{name}"}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(ModConfig.ItemNames.BOTH);

        blockKeyIsNotAName(store);
        LineTranslator.measureForTest = NameRowTest::measure;
        try {
            nameRow(store, "★ Shiny", SHINY, "耀光之微光醒", "(" + SHINY + ")", false);
            nameRow(store, "Shiny 帶記號", SHINY + "֎", "耀光之微光醒", "(" + SHINY + ")", false);
            nameRow(store, "非 Shiny 的長名字", LONG, "棘縛月照聖匣", "(" + LONG + ")", false);
            nameRow(store, "縫黏在名字前面", LONG, "棘縛月照聖匣", "(" + LONG + ")", true);
        } finally {
            LineTranslator.measureForTest = null;
        }
        wrapKeepsBracketsWhole();
    }

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace(id)));
    }

    private static final Style WYNN = font("language/wynncraft")
            .withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style MYTHIC = font("language/wynncraft")
            .withColor(TextColor.fromRgb(0xAA00AA));
    private static final Style BADGE = font("banner/box")
            .withColor(TextColor.fromRgb(0xAA00AA));
    private static final Style BADGE_TEXT = font("banner/box")
            .withColor(TextColor.fromRgb(0x000000));
    private static final Style GREY = font("language/wynncraft")
            .withColor(TextColor.fromRgb(0xAAAAAA));

    private static final String SHINY = "Shiny Glimmerwake";
    private static final String LONG = "Thornbound Moonlit Reliquary";


    private static void blockKeyIsNotAName(TranslationStore store) {
        for (String mark : new String[] {"", "֎"}) {
            String row = "{#}{#}{#}{#}{#}{#}{#}" + SHINY + mark;
            String one = store.lookup(row);
            check("單獨一列的 Shiny 名稱照舊翻（實際 " + one + "）",
                  one != null && one.contains("耀光之微光醒") && one.contains("(" + SHINY + ")"));
            String block = store.lookup(row + "\n{#}{#}{#}{#}{#}{#}{#}");
            check("★ 名稱列＋徽章列的整段鍵查不到（實際 "
                          + (block == null ? null : block.replace("\n", "⏎")) + "）",
                  block == null);
        }
    }

    private static void nameRow(TranslationStore store, String label, String name,
                                String translated, String original, boolean glued) {
        int before = failures;
        List<Component> lines = tooltip(name, glued);
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(lines, store);
        Component badges = lines.get(2);

        int at = -1;
        for (int i = 0; i < out.size(); i++) {
            if (measure(out.get(i)) > 0 && visible(out.get(i)).startsWith(translated)) {
                at = i;
                break;
            }
        }
        check(label + "：看得見的名稱列翻成譯名", at >= 0);
        if (at < 0) {
            dump(out);
            return;
        }
        Component first = out.get(at);
        Component next = at + 1 < out.size() ? out.get(at + 1) : Component.empty();
        check(label + "：名稱列只剩譯名（實際「" + visible(first) + "」）",
              !visible(first).contains("(") && !visible(first).contains(")"));
        check(label + "：★ 下一列整段是「" + original + "」（實際「" + visible(next) + "」）",
              original.equals(visible(next)));
        check(label + "：★ 下一列從名字的起點開始（名字 x=" + textX(first)
                      + "、下一列 x=" + textX(next) + "）",
              textX(first) == textX(next));
        boolean badgesKept = false;
        for (int i = at + 1; i < out.size(); i++) {
            badgesKept |= out.get(i).getString().equals(badges.getString());
        }
        check(label + "：★ 徽章那一列原封不動，沒有字被併進去", badgesKept);
        String broken = null;
        for (Component row : out) {
            String v = visible(row);
            if (v.endsWith("(") || v.startsWith(")")) {
                broken = v;
            }
        }
        check(label + "：★ 沒有一列停在左括號、或從右括號開始（實際「" + broken + "」）",
              broken == null);
        if (failures > before) {
            dump(out);
        }
    }

    private static void wrapKeepsBracketsWhole() {
        java.util.function.ToIntFunction<String> m = s -> measure(Component.literal(s));
        String text = "耀光之微光醒 (" + SHINY + ")";
        int max = m.applyAsInt("耀光之微光醒 (Shiny Glimm");
        String wrapped = TextWrapper.wrapToWidth(text, max, m);
        check("★ 折行不把左括號留在行尾（實際「" + wrapped.replace("\n", "⏎") + "」）",
              wrapped.equals("耀光之微光醒\n(" + SHINY + ")"));
        String balanced = TextWrapper.wrapBalanced(text, max, 2, m);
        boolean clean = true;
        for (String row : balanced.split("\n", -1)) {
            clean &= !row.strip().endsWith("(") && !row.strip().startsWith(")");
        }
        check("★ 平均分配之後也一樣（實際「" + balanced.replace("\n", "⏎") + "」）", clean);
        String closing = TextWrapper.wrapToWidth("微光醒的效果(三層)", m.applyAsInt("微光醒的效果(三層"), m);
        check("右括號不從行首開始（實際「" + closing.replace("\n", "⏎") + "」）",
              !closing.contains("\n)"));
        String spaced = TextWrapper.wrapToWidth("微光醒 (" + SHINY + ") 的效果",
                m.applyAsInt("微光醒 (Shiny Glimm"), m);
        check("括號後面還有字時也一樣（實際「" + spaced.replace("\n", "⏎") + "」）",
              !spaced.contains("(\n") && !spaced.contains("( \n"));
    }

    private static List<Component> tooltip(String name, boolean glued) {
        List<Component> lines = new ArrayList<>();
        MutableComponent hidden = Component.empty();
        hidden.append(lit(cp(0xCF000), font("space")));
        hidden.append(lit(cp(0xE008, 0xD0002), font("space")));
        hidden.append(lit(name, MYTHIC));
        hidden.append(lit(cp(0xCF000), font("space")));
        lines.add(hidden);

        MutableComponent row = Component.empty();
        row.append(lit(cp(0xCFFF0), WYNN));
        row.append(lit(cp(0xE031), font("tooltip/emblem/frame")));
        row.append(lit(cp(0xCFFCF), WYNN));
        row.append(lit(cp(0xE034), font("tooltip/emblem/sprite").withColor(
                TextColor.fromRgb(0x00EB1C))));
        row.append(lit(cp(0xD0005), WYNN));
        row.append(lit(cp(0xE008), font("common")));
        if (glued) {
            row.append(lit(cp(0xD0002) + name, MYTHIC));
        } else {
            row.append(lit(cp(0xD0002), WYNN));
            row.append(lit(name, MYTHIC));
        }
        lines.add(row);

        MutableComponent badges = Component.empty();
        badges.append(lit(cp(0xD0026), WYNN));
        badges.append(lit(cp(0xE060, 0xCFFFF, 0xE03C, 0xCFFFF, 0xE048, 0xE062, 0xCFFDC), BADGE));
        badges.append(lit(cp(0xE00C, 0xE018, 0xD0002), BADGE_TEXT));
        badges.append(lit(cp(0xD0001), WYNN));
        badges.append(lit(cp(0xE060, 0xCFFFF, 0xE041, 0xCFFFF, 0xE034, 0xE062, 0xCFFE6), BADGE));
        badges.append(lit(cp(0xE011, 0xE004, 0xD0002), BADGE_TEXT));
        badges.append(lit(cp(0xD0001), WYNN));
        lines.add(badges);

        lines.add(Component.empty());
        lines.add(lit("Attack Speed: Normal", GREY));
        lines.add(Component.empty());
        lines.add(lit("Class Type    Shaman/Skyseer", GREY));
        lines.add(lit("Combat Level                  93", GREY));
        return lines;
    }

    private static Component lit(String text, Style style) {
        return Component.literal(text).withStyle(style);
    }

    private static String cp(int... cps) {
        StringBuilder sb = new StringBuilder();
        for (int c : cps) {
            sb.appendCodePoint(c);
        }
        return sb.toString();
    }

    private static String visible(Component line) {
        StringBuilder sb = new StringBuilder();
        line.getString().codePoints()
                .filter(c -> !GlyphSplitter.isGlyphCodePoint(c))
                .forEach(sb::appendCodePoint);
        return sb.toString().strip();
    }

    private static int textX(Component line) {
        int x = 0;
        String s = line.getString();
        for (int i = 0; i < s.length(); ) {
            int c = s.codePointAt(i);
            if (!GlyphSplitter.isGlyphCodePoint(c) && !Character.isWhitespace(c)) {
                return x;
            }
            x += advance(c);
            i += Character.charCount(c);
        }
        return Integer.MIN_VALUE;
    }

    static int measure(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            text.codePoints().forEach(c -> w[0] += advance(c));
            return Optional.empty();
        }, Style.EMPTY);
        return Math.max(0, w[0]);
    }

    private static int advance(int c) {
        if (SpaceOffset.isOffset(c)) {
            return SpaceOffset.decode(new String(Character.toChars(c)));
        }
        if (c == 0xCF000) {
            return -4096;
        }
        if (c == 0xE031) {
            return 66;
        }
        if (c == 0xE034) {
            return 16;
        }
        if (c >= 0xE000 && c <= 0xF8FF) {
            return 9;
        }
        if (c >= 0x2E80) {
            return 9;
        }
        return switch (c) {
            case '\n' -> 0;
            case 'i', '.', ':', ',', ';', '!', '\'' -> 2;
            case 'l' -> 3;
            case 't', 'I', ' ', '(', ')' -> 4;
            case 'f', 'k' -> 5;
            default -> 6;
        };
    }

    private static void dump(List<Component> out) {
        for (int i = 0; i < out.size(); i++) {
            StringBuilder sb = new StringBuilder();
            out.get(i).getString().codePoints().forEach(c -> {
                if (GlyphSplitter.isGlyphCodePoint(c)) {
                    sb.append(String.format("<%X>", c));
                } else {
                    sb.appendCodePoint(c);
                }
            });
            System.out.println("    [" + i + "] " + measure(out.get(i)) + "px  " + sb);
        }
    }
}
