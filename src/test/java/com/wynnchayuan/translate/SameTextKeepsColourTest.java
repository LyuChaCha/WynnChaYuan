package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 譯文跟原文一字不差的那些條目，顏色不可以被動到。
 *
 * <h2>實機回報</h2>
 * 綠寶石袋的數量那一行是 {@code 472,667² (1stx 51.40¼²)}。裡面沒有字要翻，
 * 所以語料那一條的譯文<b>等於</b>原文——「刻意留原樣」的寫法。
 *
 * <p>可是重建會把整行拆成片段再接回去，而顏色是<b>猜</b>的（見
 * {@link LineTranslator#colourToken}）。結果文字一個字都沒變，貨幣圖示卻從灰色
 * 變成跟數字一樣的橘色。
 *
 * <p>這種條目重建<b>一點好處都沒有</b>：文字必定相同，能改變的只有樣式，
 * 而改變樣式在這裡一律是壞事。
 *
 * <h2>兩個容易搞錯的地方</h2>
 * 比的是<b>算繪出來的字</b>而不是模板：模板一樣不代表結果一樣，{@code {~}}
 * 會填回實際數值。
 *
 * <p>而且兩邊都要是<b>純文字</b>。{@code StyledText#getString} 帶著格式碼與
 * 片段標記，拿它跟 {@code Component#getString} 比永遠不相等——第一版就是這樣
 * 寫的，守門看起來加了，其實一次都沒生效。
 */
public final class SameTextKeepsColourTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wcy-same");
        // 譯文 == 原文：語料裡「刻意留原樣」的那種寫法
        Files.writeString(dir.resolve("gui.json"),
                "{\"{~}\\u00b2 ({~}stx {~}\\u00bc\\u00b2)\": \"{~}\\u00b2 ({~}stx {~}\\u00bc\\u00b2)\","
                + " \"Emerald Pouch\": \"\\u7da0\\u5bf6\\u77f3\\u888b\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        // 原文：數字是橘的、貨幣圖示與括號那一段是灰的——兩個不同的顏色段
        Component line = Component.empty()
                .append(Component.literal("472,667²")
                        .withStyle(Style.EMPTY.withColor(0xFFAA00)))
                .append(Component.literal(" (1stx 51.40¼²)")
                        .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
        StyledText original = StyledText.fromComponent(line);

        Component out = LineTranslator.translate(original, store);
        check("翻得出來（或至少不是 null）", out != null);
        if (out != null) {
            check("字一個都沒變（" + out.getString() + "）",
                    out.getString().equals(line.getString()));
            check("顏色分段跟原文一模一樣 —— 圖示不會跟著數字變橘",
                    colours(out).equals(colours(line)));
            if (!colours(out).equals(colours(line))) {
                System.out.println("       原文 " + colours(line));
                System.out.println("       譯文 " + colours(out));
            }
        }

        // 反面：真的有翻的照樣要翻，這一道不能把整條路擋掉
        Component real = LineTranslator.translate(
                StyledText.fromComponent(Component.literal("Emerald Pouch")), store);
        check("真的有譯文的照常翻（" + (real == null ? "null" : real.getString()) + "）",
                real != null && real.getString().equals("綠寶石袋"));

        System.out.println(failures == 0 ? "\n同字不動色：全部通過"
                : "\n同字不動色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 每一段的「文字→顏色」，用來比對樣式有沒有被動到。 */
    private static String colours(Component c) {
        StringBuilder out = new StringBuilder();
        c.visit((style, text) -> {
            out.append('[').append(text).append('|')
               .append(style.getColor() == null ? "-" : style.getColor().serialize())
               .append(']');
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
