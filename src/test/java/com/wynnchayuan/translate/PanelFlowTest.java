package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 兩欄併排的面板：同一欄折成好幾列的一句話，併起來翻、再切回原本的列。
 *
 * <h2>實機回報（2026-10-08）</h2>
 * 「Lootrun……語句不通順」。信標面板的敘述是伺服器折好行、兩欄併排送來的，
 * 一列一列查只查得到半句（「{#}信標，於」「{#} (最多」），而且左右任意配對，
 * 語料裡硬列了八百多條成對的半句。整句其實語料裡本來就有（選單上同一段敘述是
 * 整段收的），所以把同一欄連續幾格接起來查整句。
 *
 * <h2>這條測試在盯什麼</h2>
 * <ul>
 *   <li>併得起來：五格是一句話的那一欄，接回去是整句的譯文，數字填回去了；</li>
 *   <li>列數不變、每一列還是一行、行首與欄距的偏移都還在；</li>
 *   <li>另一欄不受影響（各自是完整的一格，照舊自己翻）；</li>
 *   <li>只有一格的接續列也接得上——不管它其實是哪一欄；</li>
 *   <li>湊不成句的不動：不可以因為併句就把別的格子吃掉。</li>
 * </ul>
 * 欄距補多少是照字寬算的，測試環境沒有字型，那一段只有實機看得出來。
 *
 * <p>整句那一條用的是語料裡真的那一條；措辭不寫死（術語統一的 PR 會改它），
 * 只跟「整段一次查」的結果比。
 */
public final class PanelFlowTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path corpus = Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT);
        // 左欄那幾格的譯文疊一層上去：這條測試看的是右欄，左欄只要「有東西可換」
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

        finish();
    }

    /** 兩欄都有字的五列：右欄是一句話。 */
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

    /** 左欄只有兩列，右欄的後三列是「只有一格」的接續列。 */
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

    /** 湊不成句的面板：一列都不該被動到。 */
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

    /** 同一段敘述整段一次查的結果（選單上那條路），接成一句。 */
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

    /** 這一列的實字（排版偏移不算）。 */
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
