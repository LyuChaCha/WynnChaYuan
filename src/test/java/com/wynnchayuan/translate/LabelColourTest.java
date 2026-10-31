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

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LabelColourTest {

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int GREEN = 0x55FF55;
    private static final int RED = 0xFF5555;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-label-colour");
        FlowedDebug.init(dir);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Copper\n✔ Ⓑ Mining Lv Min: {~}\n✖ Equipped Tool: Pickaxe",
                "銅\n✔ Ⓑ 挖礦等級下限: {~}\n✖ 裝備工具: 鎬");
        Files.writeString(dir.resolve("npc.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        StyledText label = vein();
        System.out.println("原文");
        for (String row : describe(label)) {
            System.out.println("  " + row);
        }

        Component out = LineTranslator.translate(label, store);
        System.out.println("譯文");
        if (out == null) {
            System.out.println("  （沒翻出來）");
        } else {
            for (String row : describe(StyledText.fromComponent(out))) {
                System.out.println("  " + row);
            }
        }

        Path notes = dir.resolve("majorid-debug.txt");
        if (Files.exists(notes)) {
            System.out.println("診斷");
            for (String row : Files.readAllLines(notes)) {
                System.out.println("  " + row);
            }
        }

        check("名牌翻得出來", out != null);
        if (out != null) {
            check("勾是綠的", colourOf(out, "✔") == GREEN);
            check("叉是紅的", colourOf(out, "✖") == RED);
            check("「挖礦等級下限:」是灰的（實際 "
                            + hex(colourOf(out, "挖礦等級下限")) + "）",
                  colourOf(out, "挖礦等級下限") == GREY);
            check("等級數字是白的（實際 " + hex(colourOf(out, "42")) + "）",
                  colourOf(out, "42") == WHITE);
            check("「裝備工具:」是灰的（實際 "
                            + hex(colourOf(out, "裝備工具")) + "）",
                  colourOf(out, "裝備工具") == GREY);
            check("工具名跟著標籤同色（實際 " + hex(colourOf(out, "鎬")) + "）",
                  colourOf(out, "鎬") == GREY);
        }

        System.out.println(failures == 0 ? "\n名牌顏色：全部通過"
                : "\n名牌顏色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static StyledText vein() {
        MutableComponent all = Component.empty();
        all.append(lit("Copper", WHITE));
        all.append(lit("\n", WHITE));
        all.append(lit("✔ ", GREEN));
        all.append(lit("Ⓑ Mining Lv Min: ", GREY));
        all.append(lit("42", WHITE));
        all.append(lit("\n", WHITE));
        all.append(lit("✖ ", RED));
        all.append(lit("Equipped Tool: ", GREY));
        all.append(lit("Pickaxe", WHITE));
        return StyledText.fromComponent(all);
    }

    private static int colourOf(Component component, String text) {
        int[] found = {-1};
        component.visit((style, piece) -> {
            if (found[0] < 0 && piece.contains(text)) {
                found[0] = style.getColor() == null ? -2 : style.getColor().getValue();
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static List<String> describe(StyledText text) {
        List<String> out = new ArrayList<>();
        text.getComponent().visit((style, piece) -> {
            out.add(hex(style.getColor() == null ? -2 : style.getColor().getValue())
                    + "  「" + piece.replace("\n", "\\n") + "」");
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static String hex(int colour) {
        return switch (colour) {
            case -1 -> "沒有這一段";
            case -2 -> "繼承";
            default -> String.format("#%06X", colour);
        };
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

}
