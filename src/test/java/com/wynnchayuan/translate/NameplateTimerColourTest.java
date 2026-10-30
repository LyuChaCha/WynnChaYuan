package com.wynnchayuan.translate;

import com.wynnchayuan.capture.ColourAudit;
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

public final class NameplateTimerColourTest {

    private static int failures = 0;

    private static final int RED = 0xFF5555;
    private static final int GREY = 0xAAAAAA;
    private static final int WHITE = 0xFFFFFF;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-timer");
        FlowedDebug.init(dir);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Wind-Blessed Soldier {#}{#}", "受風祝福的士兵 {#}{#}");
        Files.writeString(dir.resolve("npc.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        for (String timer : new String[] {"12s", "1s"}) {
            StyledText plate = plate(timer);
            String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(plate);
            check("前提：分段照實機（模板 " + template.replace("\n", "⏎") + "）",
                  template.equals("Wind-Blessed Soldier {#}{#}\n{#}\n{#} {#} {#} {~}s"));

            ColourAudit.clear();
            Component replaced = LineTranslator.translateLabel(plate, store);
            check("就地取代：名字照翻（" + shown(replaced) + "）",
                  replaced != null && replaced.getString().contains("受風祝福的士兵"));
            check("★ 就地取代：倒數 " + timer + " 整串是灰的（" + shown(replaced) + "）",
                  allGrey(replaced, timer));
            check("★ 倒數靠數值自己帶回灰色，不記成掉色（colours："
                          + ColourAudit.toJson() + "）",
                  ColourAudit.size() == 0);

            Component looked = LineTranslator.translateFloating(plate, store);
            check("★ 注視小框：倒數 " + timer + " 整串是灰的（" + shown(looked) + "）",
                  allGrey(looked, timer));
        }

        System.out.println(failures == 0
                ? "名牌倒數的顏色：全部通過" : "名牌倒數的顏色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static StyledText plate(String timer) {
        MutableComponent all = Component.empty();
        all.append(lit("Wind-Blessed Soldier", RED));
        all.append(lit(" ", WHITE));
        all.append(lit(glyph(0xE000), WHITE));
        all.append(lit(glyph(0xE001), RED));
        all.append(lit("\n", WHITE));
        all.append(lit(glyph(0xE002), WHITE));
        all.append(lit("\n", WHITE));
        all.append(lit(glyph(0xE003), WHITE));
        all.append(lit(" ", WHITE));
        all.append(lit(glyph(0xE004), WHITE));
        all.append(lit(" ", WHITE));
        all.append(lit(glyph(0xE005), WHITE));
        all.append(lit(" ", WHITE));
        all.append(lit(timer, GREY));
        return StyledText.fromComponent(all);
    }

    private static String glyph(int cp) {
        return new String(Character.toChars(cp));
    }

    private static boolean allGrey(Component out, String timer) {
        if (out == null) {
            return false;
        }
        List<int[]> chars = new ArrayList<>();
        out.visit((style, text) -> {
            TextColor colour = style.getColor();
            text.codePoints().forEach(cp -> chars.add(
                    new int[] {cp, colour == null ? -1 : colour.getValue()}));
            return Optional.empty();
        }, Style.EMPTY);
        StringBuilder plain = new StringBuilder();
        for (int[] c : chars) {
            plain.appendCodePoint(c[0]);
        }
        int at = plain.lastIndexOf(timer);
        if (at < 0) {
            return false;
        }
        int index = plain.codePointCount(0, at);
        for (int i = 0; i < timer.length(); i++) {
            if (chars.get(index + i)[1] != GREY) {
                return false;
            }
        }
        return true;
    }

    private static String shown(Component c) {
        if (c == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        c.visit((style, text) -> {
            String visible = text.codePoints()
                    .filter(cp -> !com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp))
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                             StringBuilder::append)
                    .toString().replace("\n", "⏎");
            if (!visible.isEmpty()) {
                TextColor colour = style.getColor();
                sb.append(visible).append(colour == null ? "[無]"
                        : colour.getValue() == GREY ? "[灰]"
                        : colour.getValue() == RED ? "[紅]" : "[白]");
            }
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
