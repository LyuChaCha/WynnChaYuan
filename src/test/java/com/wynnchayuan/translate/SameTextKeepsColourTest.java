package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SameTextKeepsColourTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wcy-same");
        Files.writeString(dir.resolve("gui.json"),
                "{\"{~}\\u00b2 ({~}stx {~}\\u00bc\\u00b2)\": \"{~}\\u00b2 ({~}stx {~}\\u00bc\\u00b2)\","
                + " \"Emerald Pouch\": \"\\u7da0\\u5bf6\\u77f3\\u888b\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

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

        Component real = LineTranslator.translate(
                StyledText.fromComponent(Component.literal("Emerald Pouch")), store);
        check("真的有譯文的照常翻（" + (real == null ? "null" : real.getString()) + "）",
                real != null && real.getString().equals("綠寶石袋"));

        System.out.println(failures == 0 ? "\n同字不動色：全部通過"
                : "\n同字不動色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

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
