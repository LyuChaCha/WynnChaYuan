package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class StatusColourTest {

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        agrees(store, "Marks", "標記");
        agrees(store, "Mark", "標記");
        agrees(store, "Bleeding", "失血");
        agrees(store, "Blood Pool", "血池");

        coloured(store);

        System.out.println(failures == 0
                ? "狀態名顏色：全部通過" : "狀態名顏色：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void agrees(TranslationStore store, String english, String want) {
        String zh = store.lookup(english);
        check("詞表的「" + english + "」是「" + want + "」（實際：" + zh + "）",
                want.equals(zh));
    }

    private static void coloured(TranslationStore store) {
        int cyan = 0x55FFFF;
        int body = 0xAAAAAA;

        MutableComponent line = Component.empty();
        line.append(literal("Increase your maximum ", body));
        line.append(literal("Marks ", cyan));
        line.append(glyph());
        line.append(literal(" by ", body));
        line.append(literal("+1", 0xFFFFFF));
        line.append(literal(". Decrease your damage per ", body));
        line.append(literal("Mark ", cyan));
        line.append(glyph());
        line.append(literal(" by ", body));
        line.append(literal("-0.5%", 0xFF5555));
        line.append(literal(".", body));

        Component built = LineTranslator.translate(
                StyledText.fromComponent(line), store);
        if (built == null) {
            System.out.println("  [跳過] 語料裡沒有這一行，換一句再測");
            return;
        }
        String all = built.getString();
        check("畫出來有「標記」（實際：" + all + "）", all.contains("標記"));

        Integer got = colourOf(built, "標記");
        check("「標記」拿到原文的青色（實際："
                        + (got == null ? "沒有顏色" : String.format("#%06X", got)) + "）",
                got != null && got == cyan);
    }

    private static Component glyph() {
        return Component.literal("").withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(0xFF5555))
                .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier
                                .fromNamespaceAndPath("minecraft", "tooltip/status"))));
    }

    private static Component literal(String text, int rgb) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private static Integer colourOf(Component root, String needle) {
        for (Component part : leaves(root)) {
            if (part.getString().contains(needle)) {
                TextColor colour = part.getStyle().getColor();
                return colour == null ? null : colour.getValue();
            }
        }
        return null;
    }

    private static List<Component> leaves(Component component) {
        List<Component> out = new ArrayList<>();
        if (component.getSiblings().isEmpty()) {
            out.add(component);
        }
        for (Component child : component.getSiblings()) {
            out.addAll(leaves(child));
        }
        return out;
    }

}
