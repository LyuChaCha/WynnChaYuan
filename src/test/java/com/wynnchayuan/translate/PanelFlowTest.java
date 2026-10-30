package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PanelFlowTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path corpus = Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT);
        Path extra = Files.createTempDirectory("wcy-panel");
        Files.writeString(extra.resolve("misc.json"),
                "{\"[+{~} Radiance Power]\": \"[+{~} 光輝強度]\"}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(corpus, extra));

        String[] right = {
            "Getting a Boon Type that you",
            "already have will grant +1",
            "Pulls per duplicate. Your",
            "boons will be reduced in",
            "Potency by 10% per duplicate.",
        };
        String whole = whole(right, store);
        check("前提：語料裡有這一整句（整段一次查得到）", whole != null && hasCjk(whole));
        if (whole == null) {
            finish();
            return;
        }

        twoColumns(right, whole, store);
        continuation(right, whole, store);
        untouched(store);
        leftCells(store);
        singleColumn(store);
        boonColours(store);

        finish();
    }

    private static void twoColumns(String[] right, String whole, TranslationStore store) {
        List<StyledText> rows = new ArrayList<>();
        for (String cell : right) {
            rows.add(row(20, "[+5 Radiance Power]", 40, cell));
        }
        Component[] out = LineTranslator.flowPanel(rows, store, null);
        check("★ 五列都換了", out.length == 5 && allNonNull(out));
        if (!allNonNull(out)) {
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (Component one : out) {
            String text = textOf(one);
            check("每一列還是一行（「" + text + "」）", text.indexOf('\n') < 0);
            check("行首的 20px 偏移還在", leadOf(one) == 20);
            check("左欄那一格照舊自己翻（沒被併句吃掉）", text.contains("光輝強度"));
            joined.append(text.replace("[+5 光輝強度]", ""));
        }
        check("★ 右欄五格接回去就是整句的譯文（" + joined + "）",
                squeeze(joined.toString()).equals(squeeze(whole)));
        check("數字填回去了，沒有留下佔位符",
                joined.indexOf("{~") < 0 && joined.indexOf("+1") >= 0 && joined.indexOf("10%") >= 0);
    }

    private static void continuation(String[] right, String whole, TranslationStore store) {
        List<StyledText> rows = new ArrayList<>();
        rows.add(row(20, "[+5 Radiance Power]", 40, right[0]));
        rows.add(row(20, "[+5 Radiance Power]", 40, right[1]));
        rows.add(indented(180, right[2]));
        rows.add(indented(180, right[3]));
        rows.add(indented(180, right[4]));
        Component[] out = LineTranslator.flowPanel(rows, store, null);
        check("★ 只有一格的接續列也接得上", allNonNull(out));
        if (!allNonNull(out)) {
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (Component one : out) {
            joined.append(textOf(one).replace("[+5 光輝強度]", ""));
        }
        check("接回去還是整句（" + joined + "）",
                squeeze(joined.toString()).equals(squeeze(whole)));
        check("接續列的行首偏移還在", leadOf(out[4]) == 180);
    }

    private static void singleColumn(TranslationStore store) {
        String[] lines = {
            "For the rest of your Lootrun,",
            "gain +150 Health (Max x15)",
            "everytime you complete a",
            "Challenge",
        };
        String whole = whole(lines, store);
        check("前提：語料裡有這一整句（Persistent Champion）", whole != null && hasCjk(whole));
        if (whole == null) {
            return;
        }
        List<StyledText> rows = new ArrayList<>();
        rows.add(indented(110, "[Persistent Champion]"));
        int[] leads = {77, 87, 91, 130};
        for (int i = 0; i < lines.length; i++) {
            rows.add(indented(leads[i], lines[i]));
        }
        Component[] out = LineTranslator.flowColumn(rows, store, null);
        check("標題那一列不在句子裡，交還給逐列那條路", out[0] == null);
        boolean all = true;
        for (int i = 1; i < out.length; i++) {
            all &= out[i] != null;
        }
        check("★ 單欄的四列併成一句", all);
        if (!all) {
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 1; i < out.length; i++) {
            String text = textOf(out[i]);
            check("每一列還是一行（「" + text + "」）", text.indexOf('\n') < 0);
            joined.append(text);
        }
        check("★ 接回去就是整句的譯文（" + joined + "）",
                squeeze(joined.toString()).equals(squeeze(whole)));
        check("沒有留下英文半句", joined.indexOf("gain") < 0 && joined.indexOf("everytime") < 0);

        List<StyledText> chat = List.of(indented(89, "Welcome to Wynncraft!"),
                indented(59, "play.wynncraft.com"));
        Component[] none = LineTranslator.flowColumn(chat, store, null);
        check("湊不成句的單欄訊息不碰", none[0] == null && none[1] == null);
    }

    private static void boonColours(TranslationStore store) {
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
        Style white = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
        Style bare = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("default")));

        String[] f1 = {"This time only, immediately", "gain +360 Health (Max x8) for",
                       "every Curse you currently have"};
        check("前提：語料裡有 Madman 這一整句", hasCjk(nullToEmpty(whole(f1, store))));
        List<StyledText> rows = new ArrayList<>();
        rows.add(StyledText.fromComponent(Component.empty().append(offset(52))
                .append(Component.literal("[").setStyle(grey))
                .append(Component.literal("Madman]").setStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(0xFF55FF)).withBold(true)))
                .append(offset(129))));
        rows.add(StyledText.fromComponent(Component.empty().append(offset(14))
                .append(Component.literal("This time only, immediately").setStyle(bare))
                .append(offset(91))));
        rows.add(StyledText.fromComponent(Component.empty().append(offset(2))
                .append(Component.literal("gain ").setStyle(bare))
                .append(Component.literal("+360 Health").setStyle(white))
                .append(Component.literal(" (Max x8) for").setStyle(grey))
                .append(offset(79))));
        rows.add(StyledText.fromComponent(Component.empty().append(offset(-6))
                .append(Component.literal("every Curse you currently have").setStyle(grey))
                .append(offset(71))));
        boonRows("Madman", rows, "360", "Health", store);

        String[] f2 = {"For the rest of this Lootrun,", "gain +2% Loot Quality (Max",
                       "x8) everytime you get a Boon"};
        check("前提：語料裡有 Midas Touch 這一整句", hasCjk(nullToEmpty(whole(f2, store))));
        rows = new ArrayList<>();
        rows.add(StyledText.fromComponent(Component.empty().append(offset(3))
                .append(Component.literal("For the rest of this Lootrun,").setStyle(bare))
                .append(offset(80))));
        rows.add(StyledText.fromComponent(Component.empty().append(offset(12))
                .append(Component.literal("gain ").setStyle(bare))
                .append(Component.literal("+2% Loot Quality").setStyle(white))
                .append(Component.literal(" (Max").setStyle(grey))
                .append(offset(89))));
        rows.add(StyledText.fromComponent(Component.empty().append(offset(3))
                .append(Component.literal("x8) everytime you get a Boon").setStyle(grey))
                .append(offset(80))));
        boonRows("Midas Touch", rows, "2%", "Loot Quality", store);
    }

    private static void boonRows(String name, List<StyledText> rows, String value,
                                 String stat, TranslationStore store) {
        Component[] out = LineTranslator.flowColumn(rows, store, null);
        int first = out[0] == null ? 1 : 0;
        boolean all = true;
        for (int i = first; i < out.length; i++) {
            all &= out[i] != null;
        }
        check(name + "：整句併起來翻了", all && first < out.length);
        if (!all) {
            return;
        }
        List<int[]> chars = new ArrayList<>();
        int[] depth = {0};
        for (int i = first; i < out.length; i++) {
            out[i].visit((style, text) -> {
                if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                    return java.util.Optional.empty();
                }
                TextColor colour = style.getColor();
                text.codePoints().forEach(cp -> {
                    if (cp == '(' || cp == '（') {
                        depth[0]++;
                    }
                    if (!Character.isWhitespace(cp)) {
                        chars.add(new int[] {cp, colour == null ? -1 : colour.getValue(), depth[0]});
                    }
                    if ((cp == ')' || cp == '）') && depth[0] > 0) {
                        depth[0]--;
                    }
                });
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        StringBuilder shown = new StringBuilder();
        for (int[] c : chars) {
            shown.appendCodePoint(c[0]).append(c[1] == 0xAAAAAA ? "灰" : c[1] == 0xFFFFFF ? "白"
                    : c[1] == -1 ? "無" : "?").append(' ');
        }
        System.out.println("      " + name + "：" + shown);
        check(name + "：★ 沒有任何一個字掉成預設色（沒有顏色）",
              chars.stream().noneMatch(c -> c[1] == -1));
        check(name + "：★ 括號外面也有灰色的中文字（條件句是灰的，不是只有「(最多 x8)」）",
              chars.stream().anyMatch(c -> c[2] == 0 && c[1] == 0xAAAAAA && isCjk(c[0])));
        String digits = value.replaceAll("[^0-9]", "");
        check(name + "：數值照原文是白的",
              chars.stream().filter(c -> digits.indexOf(c[0]) >= 0 && c[2] == 0)
                   .allMatch(c -> c[1] == 0xFFFFFF));
        String term = store.lookup(stat);
        StringBuilder plain = new StringBuilder();
        for (int[] c : chars) {
            plain.appendCodePoint(c[0]);
        }
        int at = term == null ? -1 : plain.indexOf(term.replace(" ", ""));
        boolean statWhite = at >= 0;
        for (int i = 0; statWhite && i < term.replace(" ", "").length(); i++) {
            statWhite = chars.get(at + i)[1] == 0xFFFFFF;
        }
        check(name + "：屬性名（" + term + "）照原文是白的", statWhite);
    }

    private static boolean isCjk(int cp) {
        return cp >= 0x4E00 && cp <= 0x9FFF;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static void untouched(TranslationStore store) {
        List<StyledText> rows = List.of(
                row(20, "Zzyzx qwfp", 40, "arst neio"),
                row(20, "Xcvb zxcv", 40, "qwer asdf."));
        Component[] out = LineTranslator.flowPanel(rows, store, null);
        check("湊不成句的面板原樣交還（全部是 null）", out[0] == null && out[1] == null);

        List<StyledText> single = List.of(indented(89, "Welcome to Wynncraft!"),
                indented(59, "play.wynncraft.com"));
        Component[] none = LineTranslator.flowPanel(single, store, null);
        check("不是多欄的訊息不碰", none[0] == null && none[1] == null);
    }

    private static void leftCells(TranslationStore store) {
        String[] right = {"Boons are now 50% more", "effective."};
        String whole = whole(right, store);
        check("前提：語料裡有「Boons are now … effective.」這一整句", whole != null && hasCjk(whole));
        check("前提：詛咒只有帶偏移的整列樣子，單獨一格查不到",
                store.lookup("[+{~} Enemy Damage]") == null
                        && store.lookup("{#}[+{~} Enemy Damage]") != null);
        if (whole == null) {
            return;
        }
        List<StyledText> rows = new ArrayList<>();
        rows.add(row(18, "[+25% Enemy Damage]", 21, right[0]));
        rows.add(row(8, "[+2.5% Enemy Resistance]", 49, right[1]));
        rows.add(trailing(21, "[+25% Enemy Health]", 98));
        Component[] out = LineTranslator.flowPanel(rows, store, null);
        check("右欄併成句子的兩列都換了", out[0] != null && out[1] != null);
        if (out[0] == null || out[1] == null) {
            return;
        }
        String first = textOf(out[0]);
        String second = textOf(out[1]);
        check("★ 第一列的左欄翻了，不是英文（「" + first + "」）",
                !first.contains("Enemy") && first.startsWith("[+25% ") && hasCjk(first.substring(0, 10)));
        check("★ 第二列的左欄也翻了（「" + second + "」）",
                !second.contains("Enemy") && second.startsWith("[+2.5% "));
        check("數字還在，佔位符沒有漏出來", first.contains("50%") || second.contains("50%"));
        check("偏移沒有被吃進字裡：左欄的字後面接著的是右欄的字",
                !first.contains("{#}") && !second.contains("{#}"));
        check("每一列還是一行", first.indexOf('\n') < 0 && second.indexOf('\n') < 0);
        String joined = (first.substring(first.indexOf(']') + 1)
                + second.substring(second.indexOf(']') + 1));
        check("右欄接回去還是整句（" + joined + "）", squeeze(joined).equals(squeeze(whole)));
    }

    private static StyledText trailing(int lead, String text, int tail) {
        MutableComponent out = Component.empty();
        out.append(offset(lead)).append(Component.literal(text)).append(offset(tail));
        return StyledText.fromComponent(out);
    }

    private static String whole(String[] lines, TranslationStore store) {
        List<StyledText> run = new ArrayList<>();
        for (String line : lines) {
            run.add(StyledText.fromString(line));
        }
        List<Component> made = LineTranslator.translateBlock(run, store, new boolean[lines.length]);
        return made == null ? null : TextSplit.join(made).getString();
    }

    private static StyledText row(int lead, String left, int gap, String right) {
        MutableComponent out = Component.empty();
        out.append(offset(lead)).append(Component.literal(left))
           .append(offset(gap)).append(Component.literal(right));
        return StyledText.fromComponent(out);
    }

    private static StyledText indented(int px, String text) {
        MutableComponent out = Component.empty();
        out.append(offset(px)).append(Component.literal(text));
        return StyledText.fromComponent(out);
    }

    private static Component offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .setStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static String textOf(Component line) {
        StringBuilder sb = new StringBuilder();
        line.visit((style, text) -> {
            if (!(SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text))) {
                sb.append(text);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static int leadOf(Component line) {
        int[] px = {0};
        boolean[] done = {false};
        line.visit((style, text) -> {
            if (done[0] || text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                px[0] += SpaceOffset.decode(text);
            } else {
                done[0] = true;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    private static boolean allNonNull(Component[] out) {
        for (Component one : out) {
            if (one == null) {
                return false;
            }
        }
        return true;
    }

    private static String squeeze(String s) {
        return s.replace(" ", "").replace("\n", "");
    }

    private static boolean hasCjk(String s) {
        return s.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void finish() {
        System.out.println(failures == 0 ? "\n面板併句：全部通過"
                : "\n面板併句：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
