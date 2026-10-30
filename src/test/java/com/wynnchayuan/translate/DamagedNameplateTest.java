package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DamagedNameplateTest {

    private static int failures = 0;

    private static final int RED = 0xFF5555;
    private static final int GREY = 0xAAAAAA;
    private static final int WHITE = 0xFFFFFF;
    private static final int GOLD = 0xFFAA00;
    private static final int DARK_GREY = 0x555555;
    private static final int AQUA = 0x55FFFF;
    private static final int PURPLE = 0xAA00AA;

    private static final Style PLATE_FONT = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.withDefaultNamespace("nameplate/default")));

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-damaged-plate");
        FlowedDebug.init(dir);

        com.google.gson.JsonObject npc = new com.google.gson.JsonObject();
        npc.addProperty("Ashen Wisp {#}{#}", "灰燼幽光 {#}{#}");
        npc.addProperty("Mossback Golem {#}{#}", "苔背魔像 {#}{#}");
        Files.writeString(dir.resolve("npc.json"), npc.toString(), StandardCharsets.UTF_8);
        com.google.gson.JsonObject misc = new com.google.gson.JsonObject();
        misc.addProperty("Ashen Wisp", "Ashen Wisp");
        misc.addProperty("Aelumia Safe Zone", "Aelumia 安全區");
        Files.writeString(dir.resolve("misc.json"), misc.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        String fresh = firstRow(LineTranslator.translateLabel(plate("Mossback Golem", null), store));
        check("前提：沒被打的名牌翻得出來（" + fresh + "）",
              fresh != null && !fresh.startsWith("Mossback"));
        StyledText mantles = plate("Mossback Golem", Status.MANTLES);
        check("前提：模板照實機（" + GlyphSplitter.toTemplate(mantles).replace("\n", "⏎") + "）",
              GlyphSplitter.toTemplate(mantles)
                      .equals("Mossback Golem {#}{#}\n{#}{#}\n{#} {~} ☠ {~}k Mantles"));
        Component hit = LineTranslator.translateLabel(mantles, store);
        check("★ 效果列帶字（Mantles）：名字照翻、跟沒被打時一樣（" + shown(hit) + "）",
              hit != null && firstRow(hit).equals(fresh));
        check("★ 效果列帶字：血條與效果列逐字元原樣（碼位、字型、顏色）",
              hit != null && belowName(hit).equals(belowName(mantles.getComponent())));

        StyledText timers = plate("Mossback Golem", Status.TIMERS);
        Component timed = LineTranslator.translateLabel(timers, store);
        check("只有圖示與倒數的效果列：名字照翻（" + shown(timed) + "）",
              timed != null && firstRow(timed).equals(fresh));
        check("只有圖示與倒數的效果列：底下逐字元原樣",
              timed != null && belowName(timed).equals(belowName(timers.getComponent())));

        Component eliteFresh = LineTranslator.translateLabel(elite("Mossback Golem", false), store);
        String eliteName = row(eliteFresh, 1);
        check("前提：沒被打的精英名牌翻得出來（" + shown(eliteFresh) + "）",
              eliteName != null && eliteName.equals(fresh));
        StyledText eliteHit = elite("Mossback Golem", true);
        check("前提：精英名牌的模板（" + GlyphSplitter.toTemplate(eliteHit).replace("\n", "⏎") + "）",
              GlyphSplitter.toTemplate(eliteHit)
                      .equals("{#}\nMossback Golem {#}{#}\n{#}{#} {~}k\n{#} {~} ☠ {~}k Mantles"));
        Component eliteOut = LineTranslator.translateLabel(eliteHit, store);
        check("★ 被打的精英怪：名字照翻、跟沒被打時一樣（" + shown(eliteOut) + "）",
              eliteOut != null && eliteName != null && eliteName.equals(row(eliteOut, 1)));
        check("★ 被打的精英怪：標記列逐字元原樣",
              eliteOut != null && row(eliteOut, 0) != null
                      && aboveName(eliteOut).equals(aboveName(eliteHit.getComponent())));
        check("★ 被打的精英怪：血條與效果列逐字元原樣",
              eliteOut != null && belowRow(eliteOut, 2).equals(belowRow(eliteHit.getComponent(), 2)));

        String wisp = firstRow(LineTranslator.translateLabel(plate("Ashen Wisp", null), store));
        check("前提：沒被打的 Ashen Wisp 翻得出來（" + wisp + "）",
              wisp != null && !wisp.startsWith("Ashen"));
        StyledText bar = plate("Ashen Wisp", Status.NONE);
        Component barred = LineTranslator.translateLabel(bar, store);
        check("★ 只多一列血條：名字不能被單獨那條（譯文是原文）搶走（" + shown(barred) + "）",
              barred != null && firstRow(barred).equals(wisp));
        check("只多一列血條：血條逐字元原樣",
              barred != null && belowName(barred).equals(belowName(bar.getComponent())));

        check("告示看板（圖示列＋名稱＋空列＋查不到的一列）仍然整塊不動",
              LineTranslator.translateLabel(sign(), store) == null);
        check("名字下面直接接一列查不到的字（沒有血條）仍然整塊不動",
              LineTranslator.translateLabel(twoRows("Mossback Golem", "Guarding the ruins"),
                                            store) == null);
        check("名字查不到的怪物名牌照樣不翻",
              LineTranslator.translateLabel(plate("Unknown Beast", Status.MANTLES), store) == null);

        realCorpus();

        System.out.println(failures == 0
                ? "被打的名牌：全部通過" : "被打的名牌：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void realCorpus() throws Exception {
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(root.resolve("zh_tw")));
        com.google.gson.JsonObject npc = com.google.gson.JsonParser.parseString(
                Files.readString(root.resolve("zh_tw/npc.json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
        int checked = 0;
        List<String> bad = new ArrayList<>();
        for (String key : npc.keySet()) {
            if (!key.endsWith(" {#}{#}") || key.indexOf('\n') >= 0
                    || key.indexOf('{') != key.length() - 6) {
                continue;
            }
            String name = key.substring(0, key.length() - 7);
            if (!GlyphSplitter.hasWord(name) || npc.get(key).getAsString().isBlank()) {
                continue;
            }
            String fresh = firstRow(LineTranslator.translateLabel(plate(name, null), store));
            if (fresh == null) {
                continue;
            }
            checked++;
            String eliteGot = row(LineTranslator.translateLabel(elite(name, true), store), 1);
            if (!fresh.equals(eliteGot)) {
                bad.add(name + " [精英] 沒被打「" + fresh + "」被打「" + eliteGot + "」");
            }
            for (Status status : new Status[] {Status.NONE, Status.TIMERS, Status.MANTLES}) {
                String got = firstRow(LineTranslator.translateLabel(plate(name, status), store));
                if (!fresh.equals(got)) {
                    bad.add(name + " [" + status + "] 沒被打「" + fresh + "」被打「" + got + "」");
                }
            }
        }
        for (int i = 0; i < Math.min(8, bad.size()); i++) {
            System.out.println("      " + bad.get(i));
        }
        check("★ 正式語料 " + checked + " 隻怪：被打之後名字都跟沒被打時一樣（不一樣的 "
                      + bad.size() + " 處）",
              checked > 1000 && bad.isEmpty());
    }

    private enum Status { NONE, TIMERS, MANTLES }

    private static Style font(String path, int colour) {
        return Style.EMPTY.withColor(TextColor.fromRgb(colour)).withFont(
                new FontDescription.Resource(Identifier.withDefaultNamespace(path)));
    }

    private static StyledText elite(String name, boolean damaged) {
        MutableComponent all = Component.empty();
        all.append(Component.literal("").withStyle(font("nameplate/gold_radiant", WHITE)));
        all.append(lit("\n", WHITE));
        all.append(lit(name, GOLD));
        all.append(lit(" ", GOLD));
        all.append(Component.literal("").withStyle(font("nameplate/default_radiant", GOLD)));
        all.append(Component.literal("").withStyle(font("nameplate/default_radiant", DARK_GREY)));
        if (damaged) {
            all.append(lit("\n", WHITE));
            all.append(Component.literal("").withStyle(font("nameplate/gold", RED)));
            all.append(Component.literal("").withStyle(font("nameplate/gold", DARK_GREY)));
            all.append(lit(" 48.2k", GREY));
            all.append(lit("\n", WHITE));
            all.append(lit("", AQUA));
            all.append(lit(" 2", GREY));
            all.append(lit(" ☠", PURPLE));
            all.append(lit(" 1.2k", GREY));
            all.append(lit(" Mantles", GREY));
        }
        return StyledText.fromComponent(all);
    }

    private static String row(Component c, int index) {
        if (c == null) {
            return null;
        }
        String[] rows = c.getString().split("\n", -1);
        return index < rows.length ? rows[index].strip() : null;
    }

    private static List<String> aboveName(Component c) {
        List<String> all = chars(c);
        int nl = all.indexOf("⏎");
        return nl < 0 ? all : all.subList(0, nl);
    }

    private static List<String> belowRow(Component c, int from) {
        List<String> all = chars(c);
        int seen = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).equals("⏎") && ++seen == from) {
                return all.subList(i, all.size());
            }
        }
        return List.of();
    }

    private static List<String> chars(Component c) {
        List<String> out = new ArrayList<>();
        c.visit((style, text) -> {
            text.codePoints().forEach(cp -> out.add(cp == '\n' ? "⏎"
                    : cp == ' ' ? "␠|" + style.getFont() + "|" + style.isBold()
                    : Integer.toHexString(cp) + "|" + style.getFont() + "|"
                            + style.getColor() + "|" + style.isBold()));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static StyledText plate(String name, Status status) {
        MutableComponent all = Component.empty();
        all.append(lit(name, RED));
        all.append(lit(" ", RED));
        all.append(glyphs("", GOLD));
        all.append(glyphs("", DARK_GREY));
        if (status != null) {
            all.append(lit("\n", WHITE));
            all.append(glyphs("", RED));
            all.append(glyphs("", DARK_GREY));
        }
        if (status == Status.TIMERS) {
            all.append(lit("\n", WHITE));
            all.append(lit("", AQUA));
            all.append(lit(" 12s", GREY));
            all.append(lit(" ⬤", 0xFFFF55));
            all.append(lit(" 3s", GREY));
        } else if (status == Status.MANTLES) {
            all.append(lit("\n", WHITE));
            all.append(lit("", AQUA));
            all.append(lit(" 2", GREY));
            all.append(lit(" ☠", PURPLE));
            all.append(lit(" 1.2k", GREY));
            all.append(lit(" Mantles", GREY));
        }
        return StyledText.fromComponent(all);
    }

    private static StyledText sign() {
        MutableComponent all = Component.empty();
        all.append(glyphs("", GOLD));
        all.append(glyphs("", DARK_GREY));
        all.append(lit("\nAelumia Safe Zone\n \n", WHITE));
        all.append(glyphs("", GOLD));
        all.append(lit(" Toward the Unknown Gate ", GREY));
        all.append(glyphs("", DARK_GREY));
        return StyledText.fromComponent(all);
    }

    private static StyledText twoRows(String name, String second) {
        MutableComponent all = Component.empty();
        all.append(lit(name, RED));
        all.append(lit(" ", RED));
        all.append(glyphs("", GOLD));
        all.append(glyphs("", DARK_GREY));
        all.append(lit("\n" + second, GREY));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static MutableComponent glyphs(String text, int colour) {
        return Component.literal(text).withStyle(PLATE_FONT.withColor(TextColor.fromRgb(colour)));
    }

    private static String firstRow(Component c) {
        return c == null ? null : c.getString().split("\n", -1)[0].strip();
    }

    private static List<String> belowName(Component c) {
        return belowRow(c, 1);
    }

    private static String shown(Component c) {
        return c == null ? "null" : c.getString().replace("\n", "⏎");
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
