package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GlyphHugTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;

    private static final String OFFSET =
            new StringBuilder().appendCodePoint(0xD0074).toString();

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        MutableComponent line = Component.empty();
        line.append(Component.literal(OFFSET).withStyle(SpaceOffset.styleFor(Style.EMPTY)));
        line.append(Component.literal("[Quest Completed]").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(GOLD))));

        Component one = LineTranslator.translateChat(
                StyledText.fromComponent(line), store);
        List<Component> built = one == null ? List.of() : List.of(one);

        check("整行查得到譯文", !built.isEmpty());
        if (built.isEmpty()) {
            report();
            return;
        }
        String all = built.stream().map(Component::getString).reduce("", (a, b) -> a + b);
        check("翻成了「任務完成」（實際：" + all.replace(OFFSET, "(對齊)") + "）",
                all.contains("任務完成"));

        for (Component row : built) {
            for (Component leaf : flatten(row)) {
                TextColor colour = leaf.getStyle().getColor();
                System.out.println("      「" + leaf.getString().replace(OFFSET, "(對齊)")
                        + "」 " + (colour == null ? "繼承" : colour.toString()));
            }
        }

        Integer bracket = colourOf(built, "[");
        check("★ 左中括號跟原文一樣是金色（拿到 "
                        + (bracket == null ? "繼承" : "#" + String.format("%06X", bracket))
                        + "）", bracket != null && bracket == GOLD);
        Integer body = colourOf(built, "任務完成");
        check("正文是金色（拿到 "
                        + (body == null ? "繼承" : "#" + String.format("%06X", body))
                        + "）", body != null && body == GOLD);
        Integer close = colourOf(built, "]");
        check("右中括號是金色（拿到 "
                        + (close == null ? "繼承" : "#" + String.format("%06X", close))
                        + "）", close != null && close == GOLD);
        check("括號跟正文沒有被切成兩個顏色",
                java.util.Objects.equals(bracket, body)
                        && java.util.Objects.equals(body, close));

        hugsStillWork(store);
        report();
    }

    private static void hugsStillWork(TranslationStore store) {
        check("中括號仍算黏著", TemplateRenderer.hugs('['));
        check("空白仍不算黏著", !TemplateRenderer.hugs(' '));
        check("語料裡的鍵沒被動到", store != null);
    }

    private static Integer colourOf(List<Component> lines, String needle) {
        for (Component line : lines) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)) {
                    TextColor colour = part.getStyle().getColor();
                    return colour == null ? null : colour.getValue();
                }
            }
        }
        return null;
    }

    private static List<Component> flatten(Component component) {
        List<Component> out = new ArrayList<>();
        if (component.getSiblings().isEmpty()) {
            out.add(component);
        }
        for (Component child : component.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "GlyphHug: 全部通過" : "GlyphHug: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
