package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import net.minecraft.ChatFormatting;
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

/**
 * 未鑑定物品的「力量已被封印」說明：整段攤平查到、我們自己重折之後，要照原文那樣置中。
 *
 * <h2>實機回報（使用者 2026-10-17）</h2>
 * 未鑑定的 Legendary 項鍊（新式外框、繁中）：英文的三行說明是置中的，譯文的兩行
 * 從左緣畫起。layout-debug 的左緣對照：原文 17、21，譯文 0。
 *
 * <p>原文的結構照 majorid-debug 錄到的那幾份（每一行開頭一個白色的偏移，
 * 後面是灰字；第二行中間夾著鑑定師的圖示與粉紫色的名字）：
 *
 * <pre>
 *   §f[偏移 15]§7This item's power has been sealed,
 *   §f[偏移 20]§7an §f[圖示]§7 §#e0b3e6Item Identifier§7 can unlock
 *   §f[偏移 64]§7its potential.
 * </pre>
 *
 * 語料收的是攤平的一句，{@link LineTranslator#translateBlock} 剝掉行首偏移、攤平
 * 查到之後照原文寬度重折。折出來比原文少一行時，先前是「行數不一樣就照原樣出」，
 * 行首偏移已經剝掉了，於是兩行都從 0 畫起。
 *
 * <p>譯文是這裡自己編的，不綁語料的措辭。
 */
public final class SealedCentreTest {

    private static int failures = 0;

    private static final Style SPACE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.withDefaultNamespace("space")));

    private static final String ICON = String.valueOf((char) 0xE000);

    /** 撐住外框的那一列：22 個圖示，假字型每個 9px。 */
    private static final int FRAME = 22 * 9;

    private static final String INDENT = "        ";

    private static final String[] ENGLISH = {
            "This item's power has been sealed,",
            "an " + ICON + " Item Identifier can unlock",
            "its potential."};

    /** 折出來比原文少一行（中文）。 */
    private static final String SHORT = "封印之句甲乙丙丁，使用 {#} 鑑定之人 即可解開它的潛能。";

    /** 折出來跟原文一樣三行（較長的語言）。 */
    private static final String LONG =
            "封印之句甲乙丙丁戊己庚辛壬癸子丑寅卯，請使用 {#} 鑑定之人 才能解開它沉睡的潛能。";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        LineTranslator.measureForTest = AbilityTooltipRenderTest::measure;
        try {
            TranslationStore shortStore = store(SHORT);
            shortStore.setNameMode(CollectorConfig.ItemNames.BOTH);
            fewerRows(shortStore);
            leftAlignedStaysLeft(shortStore);
            guessListUntouched(shortStore);
            TranslationStore longStore = store(LONG);
            longStore.setNameMode(CollectorConfig.ItemNames.BOTH);
            sameRows(longStore);
        } finally {
            LineTranslator.measureForTest = null;
        }
        System.out.println(failures == 0 ? "封印說明的置中：全部通過"
                                         : "封印說明的置中：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static TranslationStore store(String dst) throws Exception {
        Path dir = Files.createTempDirectory("wcy-sealed-centre");
        Files.writeString(dir.resolve("gear-accessory.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Pendant of the Ember", "dst": "甲墜", "role": "name"},
                   "a2": {"src": "Pendant of the Gale", "dst": "乙墜", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"),
                "{\"This item's power has been sealed, an {#} Item Identifier can unlock"
                        + " its potential.\": \"" + dst + "\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    /** ★ 回報的那一種：三行英文折成兩行中文，每一行都要照原文的中心置中。 */
    private static void fewerRows(TranslationStore store) {
        List<Component> out = translate(tooltip(true), store);
        List<Component> rows = sealedRows(out);
        check("前提：譯文折成兩行（實際 " + rows.size() + " 行）", rows.size() == 2);
        centred("少一行", rows);
    }

    /** 折成跟原文一樣的行數也一樣：每一行都要置中，不能因為行首偏移被剝掉而從 0 畫起。 */
    private static void sameRows(TranslationStore store) {
        List<Component> out = translate(tooltip(true), store);
        List<Component> rows = sealedRows(out);
        check("前提：譯文折成三行（實際 " + rows.size() + " 行）", rows.size() == 3);
        centred("同樣行數", rows);
    }

    /** 反面：同一句話原文是靠左的（縮排全是 0），譯文就不能被加上縮排。 */
    private static void leftAlignedStaysLeft(TranslationStore store) {
        List<Component> out = translate(tooltip(false), store);
        List<Component> rows = sealedRows(out);
        check("前提：靠左的那份也翻到了（實際 " + rows.size() + " 行）", !rows.isEmpty());
        for (Component row : rows) {
            check("靠左的原文，譯文照樣從 0 畫起（縮排 " + lead(row) + "）|" + row.getString() + "|",
                    lead(row) == 0);
        }
    }

    /** 底下 Wynntils 的候選清單照舊：縮排、逗號、附原文都不變。 */
    private static void guessListUntouched(TranslationStore store) {
        List<Component> out = translate(tooltip(true), store);
        int last = out.size() - 1;
        same("候選清單第一列", INDENT + "甲墜 (Pendant of the Ember),", out.get(last - 1).getString());
        same("候選清單最後一列：縮排還在", INDENT + "乙墜 (Pendant of the Gale)",
                out.get(last).getString());
    }

    // ---- 檢查 ----

    private static void centred(String what, List<Component> rows) {
        for (Component row : rows) {
            int lead = lead(row);
            int content = AbilityTooltipRenderTest.measure(row) - lead;
            double centre = lead + content / 2.0;
            check(what + "：置中在外框中線 " + FRAME / 2 + "（縮排 " + lead + "、內容 " + content
                            + "、中心 " + centre + "）|" + row.getString() + "|",
                    lead > 0 && Math.abs(centre - FRAME / 2.0) <= 2);
        }
    }

    /** 行首那一串偏移的總寬。 */
    private static int lead(Component line) {
        int[] px = {0};
        boolean[] done = {false};
        line.visit((style, text) -> {
            if (done[0] || text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isOffsetRun(text)) {
                px[0] += SpaceOffset.decode(text);
            } else {
                done[0] = true;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    /** 譯文裡屬於封印說明的那幾行：含「封印之句」到「潛能。」之間。 */
    private static List<Component> sealedRows(List<Component> out) {
        List<Component> rows = new ArrayList<>();
        boolean in = false;
        for (Component row : out) {
            String s = row.getString();
            in |= s.contains("封印之句");
            if (in) {
                rows.add(row);
            }
            if (in && s.contains("潛能。")) {
                break;
            }
        }
        return rows;
    }

    // ---- 組 tooltip ----

    private static List<Component> translate(List<Component> t, TranslationStore store) {
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(t, store);
        return out.isEmpty() ? t : out;
    }

    /**
     * 名稱、撐住外框的分隔列、三行封印說明、空行、Wynntils 的候選清單。
     *
     * @param centred 說明照外框置中（實機）還是全部靠左（反面對照）
     */
    private static List<Component> tooltip(boolean centred) {
        List<Component> t = new ArrayList<>();
        MutableComponent name = Component.empty();
        name.append(Component.literal(ICON).withStyle(font("language/wynncraft")));
        name.append(Component.literal("Unidentified Fixture").withStyle(ChatFormatting.GOLD));
        name.append(Component.literal(ICON).withStyle(font("tooltip/emblem/frame")));
        t.add(name);
        t.add(Component.literal(ICON.repeat(22)).withStyle(font("tooltip/frame")));
        for (int k = 0; k < ENGLISH.length; k++) {
            t.add(sealedRow(k, centred));
        }
        t.add(Component.empty());
        t.add(Component.literal("- ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("可能性：").withStyle(ChatFormatting.GRAY)));
        t.add(Component.empty().append(INDENT)
                .append(Component.literal("Pendant of the Ember").withStyle(ChatFormatting.GOLD))
                .append(","));
        t.add(Component.empty().append(INDENT)
                .append(Component.literal("Pendant of the Gale").withStyle(ChatFormatting.GOLD)));
        return t;
    }

    /** 一行封印說明：白色的行首偏移，接灰字；第二行夾著白色圖示與粉紫色的名字。 */
    private static Component sealedRow(int k, boolean centred) {
        MutableComponent body = Component.empty();
        if (k == 1) {
            body.append(Component.literal("an ").withStyle(ChatFormatting.GRAY));
            body.append(Component.literal(ICON).withStyle(font("common")
                    .withColor(ChatFormatting.WHITE)));
            body.append(Component.literal(" ").withStyle(ChatFormatting.GRAY));
            body.append(Component.literal("Item Identifier").withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6))));
            body.append(Component.literal(" can unlock").withStyle(ChatFormatting.GRAY));
        } else {
            body.append(Component.literal(ENGLISH[k]).withStyle(ChatFormatting.GRAY));
        }
        int lead = centred ? (FRAME - AbilityTooltipRenderTest.measure(body)) / 2 : 0;
        MutableComponent line = Component.empty();
        if (lead > 0) {
            line.append(Component.literal(SpaceOffset.encode(lead))
                    .withStyle(SPACE.withColor(ChatFormatting.WHITE)));
        }
        line.append(body);
        return line;
    }

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse("minecraft:" + id)));
    }

    private static void same(String what, String want, String got) {
        check(what + "（實際 |" + got + "|）", java.util.Objects.equals(want, got));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
