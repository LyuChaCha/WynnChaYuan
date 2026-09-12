package com.wynnchayuan.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 對話字型的位移值必須跟 Wynncraft 的 ascent 對得上。
 *
 * <h2>為什麼要釘住</h2>
 * Wynncraft 把「畫在第幾行」烘進了字型的 {@code ascent}。我們自己那六份字型
 * 靠 {@code shift} 把中日韓補回同一個高度，位移量正好是 {@code -(ascent - 7)}
 * ——7 是 {@code minecraft:default} 的 ascent。
 *
 * <p>這些數字來自伺服器資源包（見玩家回報的 {@code font-dump.txt}），
 * 不是算出來的。有人手改 JSON 卻改錯一個，畫面上只會看到「字有點偏」，
 * 追起來又是好幾輪。所以在這裡對死。
 *
 * <p>方向也一併釘住：MC 的 ttf provider 把 shift 交給 FreeType 時取了負號
 * （{@code deltaY = -shiftY}），而 FreeType 的 +y 朝上，所以正數往下。
 */
public final class DialogueFontTest {

    /** {@code minecraft:default} 的 ascent。位移都是相對它算的。 */
    private static final int DEFAULT_ASCENT = 7;

    /** 每一份字型對應的 Wynncraft ascent，抄自 font-dump.txt。 */
    private static final Map<String, Integer> ASCENT = new LinkedHashMap<>();

    // 這一份要<b>收齊</b>，不是收「目前剛好做了的那幾份」。
    //
    // 對話框是五行（body_0…body_4），選項是<b>從 0 起算</b>的四列
    // （choice_0…choice_3）。先前這裡只列到 body_3、choice 一列都沒列，
    // 於是 body_4 與 choice_0 兩份根本沒做，也一直沒人發現。
    //
    // 少一份的下場不是「字有點偏」，是那一整列<b>變成方框</b>：Minecraft 對
    // 查不到的字型 id 是拿 AllMissingGlyphProvider 頂上，每一個字都畫成框。
    // 所以話一長到第五行、選項一滿四個，玩家就看到一排方框。
    static {
        ASCENT.put("body_0", 34);
        ASCENT.put("body_1", 22);
        ASCENT.put("body_2", 10);
        ASCENT.put("body_3", -2);
        ASCENT.put("body_4", -14);
        ASCENT.put("choice_0", 110);
        ASCENT.put("choice_1", 97);
        ASCENT.put("choice_2", 84);
        ASCENT.put("choice_3", 71);
        ASCENT.put("nameplate", 50);
        ASCENT.put("control", -38);
    }

    /**
     * 要檢查哪幾種語言。
     *
     * <h2>為什麼不是寫死的名單</h2>
     * 先前這裡是 {@code { "zh_tw" }}。簡體的字型檔早就放進去了，這一行沒跟上
     * ——於是<b>簡體的字型從來沒被檢查過</b>，而 {@code DialogueRewriter} 裡
     * 另一份同樣停在 zh_tw 的名單也沒人發現。實機的症狀是簡體玩家的
     * 「SHIFT 繼續」那一行被畫到畫面外（control 的位移是 -38，預設字型是 7）。
     *
     * <p>改成問 {@code _languages.json}：會打包出去的語言就會有人用對話框，
     * 有人用就要檢查。加一個語言不必回來改這裡。
     */
    private static String[] langs() throws IOException {
        Path manifest = Path.of("src/main/resources/assets/wynnchayuan"
                + "/translations/_languages.json");
        String json = Files.readString(manifest, StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"languages\"\\s*:\\s*\\[([^]]*)]").matcher(json);
        if (!m.find()) {
            return new String[0];
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.regex.Matcher one = java.util.regex.Pattern
                .compile("\"([a-z]{2}_[a-z]{2})\"").matcher(m.group(1));
        while (one.find()) {
            out.add(one.group(1));
        }
        return out.toArray(new String[0]);
    }

    private static final Path ROOT = Path.of(
            "src/main/resources/assets/wynnchayuan/font");

    private static int failures = 0;

    private DialogueFontTest() {}

    public static void main(String[] args) throws IOException {
        System.out.println("=== 對話字型 ===");

        check("授權檔在（SIL OFL 隨模組散布必須附上）",
                Files.isRegularFile(ROOT.resolve("ofl-fusion.txt")));

        String[] langs = langs();
        check("讀得到會打包的語言：" + java.util.Arrays.toString(langs),
                langs.length > 0);

        for (String lang : langs) {
            // 覆蓋率表是「缺字就不就地取代」的依據，掉了會讓方框跑到畫面上
            Path cover = ROOT.resolve("dialogue").resolve(lang)
                    .resolve("coverage.txt");
            check(lang + " 的覆蓋率表在", Files.isRegularFile(cover));
            if (Files.isRegularFile(cover)) {
                long lines = Files.readAllLines(cover).stream()
                        .filter(l -> !l.isBlank() && !l.startsWith("#"))
                        .count();
                check(lang + " 覆蓋率表不是空的（" + lines + " 段）", lines > 100);
            }
            checkLanguage(lang);
        }

        System.out.println(failures == 0
                ? "DialogueFont: 全部通過"
                : "DialogueFont: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void checkLanguage(String lang) throws IOException {
        Path dir = ROOT.resolve("dialogue").resolve(lang);
        for (Map.Entry<String, Integer> row : ASCENT.entrySet()) {
            String name = row.getKey();
            Path file = dir.resolve(name + ".json");
            if (!Files.isRegularFile(file)) {
                check(name + ".json 存在", false);
                continue;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8)
                    .replaceAll("\\s+", "");

            check(name + " 先參照 Wynncraft 自己的字型（ASCII 外觀與高度不能變）",
                    json.contains("\"type\":\"reference\""));
            // ttf 的檔名<b>按語言不同</b>（zh_hant 與 zh_hans 是兩份字型：
            // 同一個碼位的內部筆畫不一樣）。所以不比對固定檔名，
            // 而是把 JSON 裡指到的那一份撈出來，確認它真的在。
            java.util.regex.Matcher ref = java.util.regex.Pattern
                    .compile("\"file\":\"wynnchayuan:([^\"]+)\"").matcher(json);
            check(name + " 指名了一份中日韓 ttf", ref.find());
            if (ref.reset().find()) {
                check(name + " 指到的 " + ref.group(1) + " 真的在",
                        Files.isRegularFile(ROOT.resolve(ref.group(1))));
            }
            // Fusion Pixel 10px 的 unitsPerEm 是 1000，capHeight 700、
            // ascender 1100、descender -300——全是 100 的倍數，也就是
            // 「一個設計像素 = 100 單位、一個 em = 10 像素」。size 不是 10 的
            // 倍數的話，每個設計像素分不到整數個螢幕像素，FreeType 只能用灰階
            // 抗鋸齒補，字就糊了——Cubic 11 填 size 11 那一版就是踩到這個。
            check(name + " size 是 10 的倍數（Fusion Pixel 的設計格）",
                    json.matches(".*\"size\":(10|20|30),.*"));
            check(name + " oversample 是 1（點陣字再降採樣只會更糊）",
                    json.contains("\"oversample\":1"));
            // reference 必須排在 ttf 前面：MC 是前面的 provider 優先，
            // 顛倒過來 ASCII 就會被 Cubic 11 接手，外觀跟原文對不上
            check(name + " reference 排在 ttf 前面",
                    json.indexOf("\"reference\"") < json.indexOf("\"ttf\""));

            int want = -(row.getValue() - DEFAULT_ASCENT);
            check(name + " 位移是 " + want + "（ascent " + row.getValue() + " − 7）",
                    json.contains("\"shift\":[0," + want + "]"));
        }

    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
