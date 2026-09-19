package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.List;

/**
 * 武器 tooltip 的「370 DPS」：數字用大字型、黃色；DPS 用一般字型、白色。
 *
 * <p>實機回報：翻出來的「370 每秒伤害」數字變小、「每秒伤害」跟著變成數字的顏色。
 * 數字要留在原本的大字型，「每秒伤害」要拿 DPS 那段的顏色。
 */
public final class DpsRowTest {

    private static int failures = 0;

    static final Style NUMBER = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55))
            .withFont(new FontDescription.Resource(
                    Identifier.withDefaultNamespace("offset/wynncraft_quad/12")));
    static final Style LABEL = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))
            .withFont(new FontDescription.Resource(
                    Identifier.withDefaultNamespace("language/wynncraft")));

    public static void main(String[] args) {
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(root.resolve("zh_tw"), root.resolve("zh_cn")));

        MutableComponent row = Component.empty();
        row.append(Component.literal("370").withStyle(NUMBER));
        row.append(Component.literal(" DPS").withStyle(LABEL));
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(
                List.of(Component.literal("Divzer"), Component.literal("Divzer"), row), store);
        Component shown = out.get(out.size() - 1);

        Style[] num = {null};
        Style[] word = {null};
        StringBuilder dump = new StringBuilder();
        shown.visit((style, text) -> {
            dump.append("[").append(text).append(" ").append(style.getColor())
                .append(" ").append(style.getFont()).append("]");
            if (text.contains("370")) {
                num[0] = style;
            }
            if (text.contains("每秒")) {
                word[0] = style;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        System.out.println("  畫出：" + dump);
        check("有翻出來（實際 " + shown.getString() + "）", shown.getString().contains("每秒伤害"));
        check("★ 數字留在原本的大字型",
              num[0] != null && NUMBER.getFont().equals(num[0].getFont()));
        check("數字仍是黃色", num[0] != null && NUMBER.getColor().equals(num[0].getColor()));
        check("★「每秒伤害」是 DPS 那段的白色",
              word[0] != null && LABEL.getColor().equals(word[0].getColor()));

        System.out.println(failures == 0 ? "DPS 那一行：全部通過" : "DPS 那一行：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
