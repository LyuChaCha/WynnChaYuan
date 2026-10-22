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

/**
 * 怪物名牌狀態列的倒數秒數（灰的 {@code 12s}）：逐行組出來的名牌，顏色要照原文。
 *
 * <h2>回報（使用者 2026-10-21 的 captured.json）</h2>
 * {@code colours} 裡有 78 句長這樣，說灰色掉了：
 *
 * <pre>
 *   src  Wind-Blessed Soldier {#}{#}\n{#}\n{#} {#} {#} {~}s
 *   dst  受風祝福的士兵 {#}{#}\n{#}\n{#} {#} {#} {~}s
 *   runs #FF5555 Wind-Blessed Soldier | #AAAAAA {~}s
 * </pre>
 *
 * 整塊的鍵語料裡沒有（狀態列隨身上的狀態變），走的是逐行組（{@code labelByLine}）。
 * 照實機的分段重建出來，{@code 12s} 其實是灰的：數值帶著自己的樣式填回去，
 * 緊貼的 {@code s} 跟著數值走。掉色的是<b>診斷</b>——ColourAudit 只把「主色＋貼上的
 * 重點段」算成有出現，數值與單位帶回來的顏色不算，於是每一隻帶倒數的怪都被記一次，
 * 把真的掉了色的句子擠出四百句的名額。
 *
 * <h2>這條測試在盯什麼</h2>
 * <ul>
 *   <li>畫面：名字照翻、倒數的數字與 {@code s} 都是原文的灰；</li>
 *   <li>診斷：這種名牌不再記進 {@code colours}。</li>
 * </ul>
 * 名字的譯文寫在臨時語料裡，不讀正式語料，措辭改了不會紅。
 */
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

    /**
     * 名字（紅）＋兩格血條圖示 ⏎ 一格圖示 ⏎ 三個狀態圖示、倒數（灰）。
     * 血條兩格是不同樣式的片段，模板才是 {@code {#}{#}}。
     */
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

    /** 譯文裡 {@code timer} 那幾個字都是灰的。 */
    private static boolean allGrey(Component out, String timer) {
        if (out == null) {
            return false;
        }
        List<int[]> chars = new ArrayList<>();      // {碼位, 顏色}
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
