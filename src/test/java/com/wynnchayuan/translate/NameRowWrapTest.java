package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.capture.GlyphSplitter;
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

/**
 * 「譯名 + 原文」的名稱列拆成兩列之後，第二列要整段是「(原文)」、從名字的起點開始。
 *
 * <h2>實機回報（使用者 2026-10-17）</h2>
 * 新式外框的 Mythic 法器 {@code Shiny Transfiguration}，名稱列畫成：
 *
 * <pre>
 *   [圖示]🔒溯光的化形 (
 *   Shiny Transfiguration)  [MYTHIC] [RELIK]   ← 從最左邊開始，蓋在物品圖示上
 * </pre>
 *
 * 兩件事：斷在「(」後面；第二列貼著左緣、還把徽章那一列往右推。
 *
 * <h2>原因</h2>
 * <ol>
 *   <li>整段查表（{@link LineTranslator#translateBlock}）拿「名稱列＋徽章列」去查，
 *       Shiny 的認法剝圖示時把換行當空白 strip 掉，兩列被當成一個名字認了。
 *       譯文只剩一列，整段那條路就照兩列重折：後半截落到徽章那一列的行首。</li>
 *   <li>重折時 {@code keepLatinRunWhole} 把整串英文挪到下一行，挪到括號<b>後面</b>，
 *       括號自己留在行尾。</li>
 * </ol>
 *
 * <h2>這裡照實機的片段結構重建</h2>
 * 依 {@code tooltip-partial-5.json}（新式外框：位移、外框、位移、徽記、位移、名字，
 * 每一段各自一個字型）與 {@code layout-debug.txt} 的第 0 列（{@code 󏀀<鎖>󐀂名字󏀀}）。
 * 名字與譯名都是虛構的，不跟語料的措辭綁在一起。
 */
public final class NameRowWrapTest {

    private static int failures = 0;

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

    public static void main(String[] args) throws Exception {
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
        store.setNameMode(CollectorConfig.ItemNames.BOTH);

        blockKeyIsNotAName(store);
        LineTranslator.measureForTest = NameRowWrapTest::measure;
        try {
            // 回報的那一種：layout-debug 的第 0 列是「󏀀<鎖>󐀂Shiny Transfiguration󏀀」，沒有記號
            nameRow(store, "★ Shiny", SHINY, "耀光之微光醒", "(" + SHINY + ")", false);
            // 名字結尾黏著 Shiny 記號的那一種（語料的 {#}Shiny Fatal֎{#}）
            nameRow(store, "Shiny 帶記號", SHINY + "֎", "耀光之微光醒", "(" + SHINY + ")", false);
            nameRow(store, "非 Shiny 的長名字", LONG, "棘縛月照聖匣", "(" + LONG + ")", false);
            // 名字前面那 2px 的縫跟名字同一段送來（「󐀂Brainwash」那種）
            nameRow(store, "縫黏在名字前面", LONG, "棘縛月照聖匣", "(" + LONG + ")", true);
        } finally {
            LineTranslator.measureForTest = null;
        }
        wrapKeepsBracketsWhole();

        System.out.println(failures == 0
                ? "名稱列的拆行：全部通過" : "名稱列的拆行：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /**
     * ★ 跨列的鍵不是名字。整段查表問的是「名稱列＋徽章列」，不能被 Shiny 的認法接走。
     */
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

    /**
     * 整份 tooltip 走一遍：名稱列拆成兩列，第二列從名字的起點開始，徽章列原封不動。
     *
     * @param glued 名字前面的 2px 縫跟名字放在同一段
     */
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

    /**
     * 折行本身：括號跟它括住的字在同一列。
     *
     * <p>{@code keepLatinRunWhole} 會把整串英文挪到下一列，挪到的位置是那串英文的
     * 開頭——括號後面。這裡直接問折行，寬度剛好讓斷點落在英文名字中間。
     */
    private static void wrapKeepsBracketsWhole() {
        java.util.function.ToIntFunction<String> m = s -> measure(Component.literal(s));
        String text = "耀光之微光醒 (" + SHINY + ")";
        // 「耀光之微光醒 (Shiny」放得下、加上後面那個字就放不下
        int max = m.applyAsInt("耀光之微光醒 (Shiny Glimm");
        String wrapped = LineTranslator.wrapToWidth(text, max, m);
        check("★ 折行不把左括號留在行尾（實際「" + wrapped.replace("\n", "⏎") + "」）",
              wrapped.equals("耀光之微光醒\n(" + SHINY + ")"));
        String balanced = LineTranslator.wrapBalanced(text, max, 2, m);
        boolean clean = true;
        for (String row : balanced.split("\n", -1)) {
            clean &= !row.strip().endsWith("(") && !row.strip().startsWith(")");
        }
        check("★ 平均分配之後也一樣（實際「" + balanced.replace("\n", "⏎") + "」）", clean);
        // 右括號那一半本來就擋（避頭），一起守著
        String closing = LineTranslator.wrapToWidth("微光醒的效果(三層)", m.applyAsInt("微光醒的效果(三層"), m);
        check("右括號不從行首開始（實際「" + closing.replace("\n", "⏎") + "」）",
              !closing.contains("\n)"));
        String spaced = LineTranslator.wrapToWidth("微光醒 (" + SHINY + ") 的效果",
                m.applyAsInt("微光醒 (Shiny Glimm"), m);
        check("括號後面還有字時也一樣（實際「" + spaced.replace("\n", "⏎") + "」）",
              !spaced.contains("(\n") && !spaced.contains("( \n"));
    }

    /**
     * 照實機的片段結構組一份 Mythic 法器的 tooltip。
     *
     * <ul>
     *   <li>第 0 列：{@code 󏀀<鎖>󐀂名字󏀀}，寬度 0（layout-debug 的「內容 0 px」）。</li>
     *   <li>第 1 列：位移、外框、位移、徽記、位移、鎖、2px 的縫、名字——每一段各自的字型。</li>
     *   <li>第 2 列：往右 38px，然後 MYTHIC、RELIK 兩塊徽章（底圖、字、1px 的縫）。</li>
     * </ul>
     */
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

    /** 剝掉圖示與位移之後看得見的字。 */
    private static String visible(Component line) {
        StringBuilder sb = new StringBuilder();
        line.getString().codePoints()
                .filter(c -> !GlyphSplitter.isGlyphCodePoint(c))
                .forEach(sb::appendCodePoint);
        return sb.toString().strip();
    }

    /** 第一個看得見的字畫在第幾個像素。 */
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

    /**
     * 假字型。圖示寬度照 {@code IngredientTooltipTest}（從 layout-debug 倒推的那一份）：
     * 外框 66、徽記 16，名字從第 22px 開始；鎖 9px、後面 2px 的縫。
     *
     * <p>偏移碼位不管掛在哪個字型底下都算寬度。第 0 列的 {@code U+CF000} 把整列推出畫面，
     * 量出來是 0——跟實機 layout-debug 一樣，NameWrap 因此不碰那一列。
     */
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

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
