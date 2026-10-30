package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class UprightTest {

    private static int failures = 0;

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        Style title = Style.EMPTY
                .withColor(TextColor.fromRgb(0xFFFFFF))
                .withItalic(true)
                .withBold(true);
        Component out = LineTranslator.translate(
                StyledText.fromComponent(
                        Component.literal("Your Islands").withStyle(title)),
                store, false);
        check("標題翻得出來", out != null && out.getString().contains("你的島嶼"));
        if (out != null) {
            check("中文不再是斜的", !italicOf(out, "你的島嶼"));
            check("粗體照舊——只丟斜體", boldOf(out, "你的島嶼"));
        }

        MutableComponent mixed = Component.empty();
        mixed.append(Component.literal("Ragni Citizen").withStyle(title));
        Component both = LineTranslator.translate(
                StyledText.fromComponent(mixed), store, false);
        check("混排的那一行翻得出來",
                both != null && both.getString().contains("市民"));
        if (both != null) {
            check("中文那半不斜", !italicOf(both, "市民"));
            check("英文那半跟著一起拉正（同一行不能半斜半正）", !italicOf(both, "Ragni"));
        }

        Component english = Component.literal("Untradable").withStyle(title);
        check("純英文行本身仍然是斜的（確認測試有意義）",
              italicOf(english, "Untradable"));
        Component flat = LineTranslator.unslantAll(english);
        check("整塊拉正時純英文行也拉正", !italicOf(flat, "Untradable"));

        Component han = Component.literal("無法交易").withStyle(title);
        check("中文照樣拉正", !italicOf(LineTranslator.unslantAll(han), "無法交易"));
        check("粗體不受影響——只丟斜體",
              boldOf(LineTranslator.unslantAll(han), "無法交易"));

        System.out.println(failures == 0
                ? "Upright: 全部通過" : "Upright: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static boolean italicOf(Component out, String needle) {
        Component part = find(out, needle);
        return part != null && part.getStyle().isItalic();
    }

    private static boolean boldOf(Component out, String needle) {
        Component part = find(out, needle);
        return part != null && part.getStyle().isBold();
    }

    private static Component find(Component out, String needle) {
        for (Component part : flatten(out)) {
            if (part.getContents().toString().contains(needle)) {
                return part;
            }
        }
        return null;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        for (Component child : c.getSiblings()) {
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
}
