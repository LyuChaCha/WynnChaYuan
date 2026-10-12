package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.capture.UnappliedAudit;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 「譯名 + 原文」附的原文<b>不能照抄鍵裡的佔位符</b>。
 *
 * <h2>實機回報（2026-10-12）</h2>
 * 使用者的 {@code captured.json}，「翻了卻沒顯示」那一節：
 *
 * <pre>
 *   src  {#}Arsonist's Tome of Allegiance{#}
 *   dst  {#}戀火效忠書卷{#} ({#}Arsonist's Tome of Allegiance{#})
 *   why  {#}: src 2, dst 4
 * </pre>
 *
 * <p>{@code tome.json} 有兩條名稱的鍵本身帶著圖示。查表把鍵整個接在譯名後面，
 * 圖示多了一倍，{@code LineTranslator#rebuildAll} 判定佔位符對不上、整條放棄——
 * 開了「譯名 + 原文」反而連譯名都沒有。名字裡帶數值或地名的裝備
 * （{@code H-{~} Micro Defibrillator}、{@code {p} Banner}）是同一個毛病。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>鍵帶圖示的名字：譯得出來、圖示一個不多、原文不帶圖示；</li>
 *   <li>附的原文還是認得出來——詞表不換它、太寬時挪得到下一行
 *       （括號裡的字已經不是語料的鍵了，認不出來這兩件事會一起壞）；</li>
 *   <li>鍵帶數值的名字：原文裡是<b>同一個</b>數字，不多消耗；</li>
 *   <li>鍵帶地名的名字：至少譯得出來；</li>
 *   <li>實機語料的那兩本書卷，名稱的兩種外框在七個語言都附得上原文。</li>
 * </ul>
 */
public final class GlyphNameOriginalTest {

    private static int failures = 0;

    /** 測試用的圖示字元（私用區，會被當成 {@code {#}}）。 */
    private static final String ICON = String.valueOf((char) 0xE000);

    private static final String TAIL = String.valueOf((char) 0xE001);

    private static final String TOME = "Arsonist's Tome of Allegiance";

    private static final String WRAPPED = "{#}" + TOME + "{#}";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-glyph-name");
        // ★ 回報的那一條，照使用者 captured.json 記到的樣子：鍵本身帶圖示。
        Files.writeString(dir.resolve("tome.json"), """
                {"_meta": {"itemNames": true, "gearNames": true},
                 "entries": {
                   "t1": {"src": "{#}Arsonist's Tome of Allegiance{#}",
                          "dst": "{#}戀火效忠書卷{#}", "role": "name"},
                   "t2": {"src": "Abyssal Tome of Combat Mastery II",
                          "dst": "潮湧戰鬥精通書卷 II", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gear-accessory.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "H-{~} Micro Defibrillator", "dst": "H-{~} 微型除顫器",
                          "role": "name"},
                   "a2": {"src": "{p} Banner", "dst": "{p} 旗幟", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        // 括號裡的英文剛好都是詞表裡的詞——沒認出那是附的原文就會被換掉。
        Files.writeString(dir.resolve("ability-terms.json"),
                "{\"Allegiance\": \"效忠\", \"Tome\": \"書\"}", StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(CollectorConfig.ItemNames.BOTH);
        UnappliedAudit.clear();

        // ---- ★ 回報的那一種：鍵帶圖示 ----
        String shown = store.lookup(WRAPPED);
        same("查表：原文不帶圖示，接在結尾的圖示之前",
                "{#}戀火效忠書卷 (" + TOME + "){#}", shown);
        check("★ 佔位符跟原文對得上（" + UnappliedAudit.why(WRAPPED, shown) + "）",
                UnappliedAudit.why(WRAPPED, shown) == null);

        Component built = line(ICON, TOME, TAIL, store);
        same("測試列的模板跟回報的鍵同形", WRAPPED, lastTemplate);
        String row0 = built == null ? null : built.getString();
        check("★ 名稱翻出來了、後面附著原文（實際 " + plain(row0) + "）",
                row0 != null && row0.contains("戀火效忠書卷 (" + TOME + ")"));
        check("圖示一個不多、留在原本的兩頭（實際 " + plain(row0) + "）",
                row0 != null && row0.startsWith(ICON) && row0.endsWith(TAIL)
                        && glyphs(row0) == 2);
        check("★ 沒有被記成「翻了卻沒顯示」（實際 " + UnappliedAudit.toJson() + "）",
                UnappliedAudit.size() == 0);

        // ---- ★ 附的原文還認得出來：詞表不換 ----
        String swapped = LineTranslator.termsWithin(shown, store);
        check("★ 折行前換詞不動附的原文（實際 " + swapped + "）",
                swapped == null || swapped.contains("(" + TOME + ")"));
        check("「 (」的位置找得到（實際 " + store.appendedOriginalAt(shown) + "）",
                store.appendedOriginalAt(shown) == shown.indexOf(" ("));
        // 反方向：同一批詞在一般內文照換，否則「把詞表關掉」也會過。
        String body = LineTranslator.termsWithin("Tome of Allegiance grants power", store);
        check("一般內文照樣換詞（實際 " + body + "）",
                body != null && body.contains("效忠") && body.contains("書"));

        // ---- ★ 太寬時原文挪得到下一行 ----
        ToIntFunction<String> measure = GlyphNameOriginalTest::cells;
        String broken = LineTranslator.breakBeforeOriginal(shown, cells(WRAPPED), store, measure);
        same("折行前那一刀：結尾的圖示留在名字那一行",
                "{#}戀火效忠書卷{#}\n(" + TOME + ")", broken);
        if (built != null) {
            List<Component> original = List.of(source(ICON, TOME, TAIL));
            NameWrap.Split split = NameWrap.split(original, List.of(built), new boolean[] {false},
                    store, c -> cells(c.getString()));
            List<Component> out = split.translated();
            check("★ 畫好之後 NameWrap 也拆得開（實際 " + out.size() + " 行）", out.size() == 2);
            if (out.size() == 2) {
                check("名字那一行只剩譯名與圖示（實際 " + plain(out.get(0).getString()) + "）",
                        (ICON + "戀火效忠書卷" + TAIL).equals(out.get(0).getString()));
                check("原文自己一行（實際 " + plain(out.get(1).getString()) + "）",
                        out.get(1).getString().endsWith("(" + TOME + ")")
                                && !out.get(1).getString().contains("戀火"));
            }
        }

        // ---- 另外兩種模式照舊 ----
        store.setNameMode(CollectorConfig.ItemNames.ON);
        same("只給譯名：不附原文", ICON + "戀火效忠書卷" + TAIL, text(ICON, TOME, TAIL, store));
        store.setNameMode(CollectorConfig.ItemNames.OFF);
        same("關掉物品名稱：留原文", ICON + TOME + TAIL, text(ICON, TOME, TAIL, store));
        store.setNameMode(CollectorConfig.ItemNames.BOTH);

        // ---- 對照組：鍵不帶佔位符的名字一個字都沒變 ----
        same("一般的名字照舊", "潮湧戰鬥精通書卷 II (Abyssal Tome of Combat Mastery II)",
                store.lookup("Abyssal Tome of Combat Mastery II"));

        // ---- ★ 鍵帶數值：同一個數字再用一次 ----
        String num = store.lookup("H-{~} Micro Defibrillator");
        same("查表：原文裡的數值改成指名的", "H-{~} 微型除顫器 (H-{~1} Micro Defibrillator)", num);
        String rolled = text(ICON.repeat(5), "H-209 Micro Defibrillator [58.2%]", "", store);
        check("★ 名字帶數值的也翻得出來，原文是同一個數字、鑑定度沒被拿走（實際 "
                        + plain(rolled) + "）",
                rolled.endsWith("H-209 微型除顫器 (H-209 Micro Defibrillator) [58.2%]"));
        check("折行前認得出來（實際 " + store.appendedOriginalAt(num) + "）",
                store.appendedOriginalAt(num) == num.indexOf(" ("));
        String drawn = "H-209 微型除顫器 (H-209 Micro Defibrillator) [58.2%]";
        int[] span = store.appendedOriginalSpan(drawn);
        check("畫好之後也認得出來", span != null && span[0] == drawn.indexOf(" (")
                && span[1] == drawn.indexOf(')') + 1);
        same("兩個數值各指各的", "A-{~1} B-{~2} Thing",
                TranslationStore.originalOf("A-{~} B-{~} Thing"));

        // ---- ★ 鍵帶地名：沒有指名的寫法，只給譯名 ----
        same("查表：帶地名的名字不附原文", "{p} 旗幟", store.lookup("{p} Banner"));
        String banner = text(ICON.repeat(5), "Detlas Banner", "", store);
        same("測試列真的被認成地名", "{#}{#}{#}{#}{#}{p} Banner", lastTemplate);
        check("★ 名字帶地名的也翻得出來（實際 " + plain(banner) + "）",
                banner.endsWith("Detlas 旗幟"));
        check("這幾種都沒有被記成「翻了卻沒顯示」（實際 " + UnappliedAudit.toJson() + "）",
                UnappliedAudit.size() == 0);

        // ---- 不是附原文的括號，照舊不認 ----
        check("敘述本來就帶的括號不算", store.appendedOriginalAt("緩慢 (每秒 1.5 次)") < 0
                && store.appendedOriginalAt("{#}緩慢 (Slow){#}") < 0
                && store.appendedOriginalSpan("H-209 微型除顫器 (H-209 Other Thing)") == null);

        realCorpus();

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("GlyphNameOriginalTest 全部通過");
        System.exit(0);
    }

    /**
     * ★ 實機語料：那兩本書卷的名稱，兩種外框都要附得上原文。
     *
     * <p>書卷的名稱是兩行，看不見的第 0 行是 {@code {#}名稱{#}}、看得見的那一行是
     * {@code {#}{#}{#}名稱{#}}。語料的鍵寫成第 0 行的樣子時，看得見的那一行逐片段
     * 查的是裸名、查不到——不管哪個模式，畫面上都是英文。鍵要跟其他書卷一樣是裸名。
     *
     * <p>譯名從語料讀，不寫死：這裡守的是「附得上」，不是措辭。
     */
    private static void realCorpus() throws Exception {
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");
        String[] tomes = {TOME, "Dragon's Tome of Mysticism III"};
        try (var langs = Files.list(root)) {
            for (Path lang : langs.filter(Files::isDirectory).sorted().toList()) {
                if (!Files.exists(lang.resolve("tome.json"))) {
                    continue;
                }
                // 只載這兩個檔就夠：名稱在 tome.json，整列條目只可能在 misc.json
                Path dir = Files.createTempDirectory("wynnchayuan-glyph-name-real");
                Files.copy(lang.resolve("tome.json"), dir.resolve("tome.json"));
                Files.copy(lang.resolve("misc.json"), dir.resolve("misc.json"));
                TranslationStore store = new TranslationStore();
                store.loadAll(dir);
                for (String tome : tomes) {
                    store.setNameMode(CollectorConfig.ItemNames.ON);
                    String name = store.lookup(tome);
                    String who = lang.getFileName() + " " + tome;
                    check(who + "：鍵是裸名（實際 " + name + "）",
                            name != null && !name.equals(tome) && name.indexOf('{') < 0);
                    store.setNameMode(CollectorConfig.ItemNames.BOTH);
                    String want = name + " (" + tome + ")";
                    String hidden = text(ICON, tome, TAIL, store);
                    check(who + "：第 0 行（實際 " + plain(hidden) + "）", hidden.contains(want));
                    String visible = text(ICON.repeat(3), tome, TAIL, store);
                    same(who + "：看得見那一行的外框", "{#}{#}{#}" + tome + "{#}", lastTemplate);
                    check(who + "：★ 看得見的那一行（實際 " + plain(visible) + "）",
                            visible.contains(want) && glyphs(visible) == 4);
                }
            }
        }
    }

    /** 假字型：一個字一格，中日文兩格。正式的要 Minecraft 的字型，測試環境量出來全是 0。 */
    private static int cells(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += s.charAt(i) > 0x2E80 && s.charAt(i) < 0xE000 ? 2 : 1;
        }
        return w;
    }

    private static long glyphs(String text) {
        return text.codePoints().filter(cp -> cp >= 0xE000 && cp <= 0xF8FF).count();
    }

    private static String text(String before, String name, String after, TranslationStore store) {
        Component out = line(before, name, after, store);
        return out == null ? source(before, name, after).getString() : out.getString();
    }

    /** 翻好的那一列；沒翻到回 {@code null}。 */
    private static Component line(String before, String name, String after,
            TranslationStore store) {
        StyledText styled = StyledText.fromComponent(source(before, name, after));
        lastTemplate = com.wynnchayuan.capture.LineParts.of(styled).template();
        return LineTranslator.translate(styled, store);
    }

    /** 上一列實際算出來的模板，拿來確認測試列跟實機的鍵同形。 */
    private static String lastTemplate = "";

    /**
     * 照實機的分段組一列：每個圖示各自一段、字型交錯，所以幾個圖示就是幾個
     * {@code {#}}。見 {@code NameRowTest#row}。
     */
    private static Component source(String before, String name, String after) {
        net.minecraft.network.chat.MutableComponent line = Component.empty();
        icons(line, before, 0);
        line.append(Component.literal(name).withStyle(s -> s.withColor(0xAA00AA)));
        icons(line, after, before.length());
        return line;
    }

    private static void icons(net.minecraft.network.chat.MutableComponent line, String icons,
            int offset) {
        for (int i = 0; i < icons.length(); i++) {
            String font = (offset + i) % 2 == 0
                    ? "minecraft:language/wynncraft" : "minecraft:tooltip/emblem/frame";
            net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY
                    .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                            net.minecraft.resources.Identifier.parse(font)));
            line.append(Component.literal(icons.substring(i, i + 1)).withStyle(style));
        }
    }

    /** 圖示字元印出來是亂碼，換成 # 好讀。 */
    private static String plain(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(
                cp -> sb.appendCodePoint(cp >= 0xE000 && cp <= 0xF8FF ? '#' : cp));
        return sb.toString();
    }

    private static void same(String what, String want, String got) {
        check(what + "（實際 " + plain(got) + "）", java.util.Objects.equals(want, got));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
