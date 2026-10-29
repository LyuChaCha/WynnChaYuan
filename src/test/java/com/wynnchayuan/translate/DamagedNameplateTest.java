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

/**
 * 怪物被打之後，頭上的名字要照樣是譯文——血條與狀態列原樣留著。
 *
 * <h2>回報（1.0.4，名牌＝就地取代）</h2>
 * 「怪物頭上的名牌有翻，一打下去（血條出現、身上掛了效果）名字就變回英文。」
 * 被打的怪物名牌是一塊三列的字：
 * <pre>
 *   Stone Golem {#}{#}          ← 名字＋等級膠囊
 *   {#}                         ← 血條（nameplate 字型的私用區字元）
 *   ❈ {~} {#} {~} Mantles       ← 身上的效果
 * </pre>
 *
 * <h2>壞在哪（兩條路）</h2>
 * <ol>
 *   <li><b>效果列有字。</b>逐行退路只認 {@code Distorted} 是狀態詞，其餘帶字的效果
 *       （語料裡就有 {@code Mantles}）被當成「有字卻查不到」，整塊放棄，名字一起變回英文。</li>
 *   <li><b>只多一列血條。</b>整塊的鍵查不到時，整塊查表把尾巴的血條與等級膠囊一路剝到
 *       只剩名字再查；語料裡若剛好有一條<b>單獨的</b>名字（別的介面收的、譯文就是原文，
 *       正式語料的 {@code Orange Cybel} 就是），整塊就「查到了」原文。</li>
 * </ol>
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>被打之後第一列跟沒被打時<b>一模一樣</b>（不寫死譯文措辭，拿沒被打的那塊當答案）；</li>
 *   <li>血條與效果列逐字元原樣：碼位、字型、顏色都不動；</li>
 *   <li>反方向：告示看板查不到的列仍然整塊放棄，不能半中半英；</li>
 *   <li>正式語料逐隻跑一遍：每一隻翻得出來的怪，被打之後名字都不能變。</li>
 * </ul>
 */
public final class DamagedNameplateTest {

    private static int failures = 0;

    private static final int RED = 0xFF5555;
    private static final int GREY = 0xAAAAAA;
    private static final int WHITE = 0xFFFFFF;
    private static final int GOLD = 0xFFAA00;
    private static final int DARK_GREY = 0x555555;
    private static final int AQUA = 0x55FFFF;
    private static final int PURPLE = 0xAA00AA;

    /** 名牌的字型：等級膠囊與血條都是 {@code minecraft:nameplate/default} 的私用區字元。 */
    private static final Style PLATE_FONT = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.withDefaultNamespace("nameplate/default")));

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-damaged-plate");
        FlowedDebug.init(dir);

        // 名字的譯文只寫在名牌的鍵上，跟正式語料一樣；另外放一條單獨的名字，譯文是原文。
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

        // --- 1. 效果列帶字 -----------------------------------------------
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

        // --- 1b. 精英怪（維護者補充：出事的是精英怪）-------------------------
        // 名字上面一列純圖示的標記、名牌字型換成 radiant／gold 那幾套、大血條旁邊帶數值，
        // 被打之後的效果列帶字。
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

        // --- 2. 只多一列血條，語料裡另有一條單獨的名字 ------------------------
        String wisp = firstRow(LineTranslator.translateLabel(plate("Ashen Wisp", null), store));
        check("前提：沒被打的 Ashen Wisp 翻得出來（" + wisp + "）",
              wisp != null && !wisp.startsWith("Ashen"));
        StyledText bar = plate("Ashen Wisp", Status.NONE);
        Component barred = LineTranslator.translateLabel(bar, store);
        check("★ 只多一列血條：名字不能被單獨那條（譯文是原文）搶走（" + shown(barred) + "）",
              barred != null && firstRow(barred).equals(wisp));
        check("只多一列血條：血條逐字元原樣",
              barred != null && belowName(barred).equals(belowName(bar.getComponent())));

        // --- 3. 反方向：不是怪物名牌的，查不到的列照樣整塊放棄 -----------------
        check("告示看板（圖示列＋名稱＋空列＋查不到的一列）仍然整塊不動",
              LineTranslator.translateLabel(sign(), store) == null);
        check("名字下面直接接一列查不到的字（沒有血條）仍然整塊不動",
              LineTranslator.translateLabel(twoRows("Mossback Golem", "Guarding the ruins"),
                                            store) == null);
        check("名字查不到的怪物名牌照樣不翻",
              LineTranslator.translateLabel(plate("Unknown Beast", Status.MANTLES), store) == null);

        // --- 4. 正式語料逐隻跑 -------------------------------------------
        realCorpus();

        System.out.println(failures == 0
                ? "被打的名牌：全部通過" : "被打的名牌：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 正式語料的每一隻怪：被打之後（只多血條、血條＋帶字的效果）名字都跟沒被打時一樣。 */
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

    /**
     * 精英怪的名牌：名字上面一列純圖示的標記（{@code nameplate/gold_radiant} 的 phase 字元），
     * 等級膠囊用 {@code nameplate/default_radiant}；被打之後是 {@code nameplate/gold} 的大血條
     * （{@code U+E015/E017/E035/E037}，資源包裡那一格是 16 像素高）加上血量數值，再接帶字的效果列。
     */
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

    /** 第幾列（去掉頭尾空白）；沒有那一列就是 {@code null}。 */
    private static String row(Component c, int index) {
        if (c == null) {
            return null;
        }
        String[] rows = c.getString().split("\n", -1);
        return index < rows.length ? rows[index].strip() : null;
    }

    /** 第一個換行之前的每一個字元。 */
    private static List<String> aboveName(Component c) {
        List<String> all = chars(c);
        int nl = all.indexOf("⏎");
        return nl < 0 ? all : all.subList(0, nl);
    }

    /** 第 {@code from} 列（從 0 算）開始的每一個字元。 */
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

    /** 每一個字元：碼位、字型、顏色、粗體（換行寫成 ⏎，空白不看顏色）。 */
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

    /**
     * 照實機的分段組一塊怪物名牌：名字（紅）＋空白＋等級膠囊（兩段不同色的名牌字型字元）；
     * 被打之後換行接血條（滿的一段、空的一段），再換行接效果列。
     *
     * @param status {@code null} 是沒被打的名牌（只有名字那一列）
     */
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

    /** 告示看板：圖示列、名稱、空列、一列語料沒有的字。 */
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

    /** 第一個換行之後的每一個字元（含那個換行）。 */
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
