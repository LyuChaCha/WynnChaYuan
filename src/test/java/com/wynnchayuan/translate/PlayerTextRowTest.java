package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
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
import java.util.Optional;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class PlayerTextRowTest {

    private static final int AQUA = 0x00AAAA;
    private static final int GREY = 0xAAAAAA;
    private static final int WHITE = 0xFFFFFF;
    private static final int PINK = 0xFF55FF;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-player-row");
        FlowedDebug.init(dir);
        JsonObject label = new JsonObject();
        label.addProperty("Teleporter", "PORTAL");
        label.addProperty("Glow Heart", "GLOW-HEART");
        label.addProperty("Blacksmith", "SMITH");
        label.addProperty("Bank\nopen to all", "BANK\nOPEN");
        Files.writeString(dir.resolve("label.json"), label.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Component sign = LineTranslator.translateLabel(teleporter("my study"), store);
        String got = text(sign);
        check("★ 傳送立牌：第一列翻、目的地原樣（實際 " + got + "）",
              "PORTAL\nto my study".equals(got));
        check("★ 目的地前面的 to 還是灰的", colourOf(sign, "to ") == GREY);
        check("★ 玩家取的名字還是白的", colourOf(sign, "my study") == WHITE);
        check("第一列照原本的顏色", colourOf(sign, "PORTAL") == AQUA);

        Component heart = LineTranslator.translateLabel(two("Glow Heart", PINK, "good tree", GREY), store);
        check("★ 改過名字的物品：第一列翻、第二列原樣（實際 " + text(heart) + "）",
              "GLOW-HEART\ngood tree".equals(text(heart)));

        check("第二列跟第一列同色：整塊不動",
              LineTranslator.translateLabel(two("Blacksmith", AQUA, "repairs gear here", AQUA), store)
                      == null);
        check("第二列大寫開頭：整塊不動（像遊戲自己的字）",
              LineTranslator.translateLabel(two("Blacksmith", AQUA, "Repairs Gear", GREY), store) == null);
        MutableComponent three = Component.empty();
        three.append(lit("Teleporter", AQUA)).append(lit("\nto ", GREY)).append(lit("my study", WHITE))
                .append(lit("\nfloor two", GREY));
        check("三列的：整塊不動",
              LineTranslator.translateLabel(StyledText.fromComponent(three), store) == null);
        check("整塊有條目時照整塊翻",
              "BANK\nOPEN".equals(text(LineTranslator.translateLabel(
                      two("Bank", AQUA, "open to all", GREY), store))));

        TranslationStore real = new TranslationStore();
        real.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations/zh_tw"));
        String row0 = real.lookup("Teleporter");
        Component shipped = LineTranslator.translateLabel(teleporter("my study"), real);
        check("前提：zh_tw 有 Teleporter 的譯名（" + row0 + "）", row0 != null && !row0.isBlank());
        check("★ zh_tw：傳送立牌第一列照語料翻（實際 " + text(shipped) + "）",
              row0 != null && (row0.strip() + "\nto my study").equals(text(shipped)));

        System.out.println(failures == 0 ? "玩家自己打的第二列：全部通過"
                : "玩家自己打的第二列：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static StyledText teleporter(String room) {
        MutableComponent all = Component.empty();
        all.append(lit("Teleporter", AQUA).withStyle(s -> s.withBold(true)));
        all.append(lit("\nto ", GREY));
        all.append(lit(room, WHITE));
        return StyledText.fromComponent(all);
    }

    private static StyledText two(String top, int topColour, String below, int belowColour) {
        return StyledText.fromComponent(Component.empty().append(lit(top, topColour))
                .append(lit("\n" + below, belowColour)));
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static String text(Component c) {
        return c == null ? null : c.getString();
    }

    private static int colourOf(Component c, String piece) {
        if (c == null) {
            return -2;
        }
        StringBuilder flat = new StringBuilder();
        List<Integer> colours = new ArrayList<>();
        c.visit((style, s) -> {
            TextColor colour = style.getColor();
            for (int i = 0; i < s.length(); i++) {
                flat.append(s.charAt(i));
                colours.add(colour == null ? -1 : colour.getValue());
            }
            return Optional.empty();
        }, Style.EMPTY);
        int at = flat.indexOf(piece);
        return at < 0 ? -2 : colours.get(at);
    }
}
