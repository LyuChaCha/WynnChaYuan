package com.wynnchayuan;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.PlaceNames;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 驗證翻譯來回：模板抽取 → 查表 → 還原，以及最重要的「顏色抄、字型不抄」規則。
 */
public final class TranslateTest {

    private static int failures = 0;

    /** Wynncraft 用來畫材質包符號的字型。 */
    private static final FontDescription COMMON =
            new FontDescription.Resource(Identifier.withDefaultNamespace("common"));

    /** 一個材質包圖示字元。 */
    private static final String GLYPH = new String(Character.toChars(0xE007));

    /** 排版用的空白字型（Wynncraft 用它在每行前後塞對齊字元）。 */
    private static final FontDescription SPACE =
            new FontDescription.Resource(Identifier.withDefaultNamespace("space"));

    /** Wynntils 自己的文字字型，數值也用它渲染。 */
    private static final FontDescription WYNNTILS_LANG =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("wynntils", "language"));

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-test");

        plainLine(dir);
        glyphLine(dir);
        placeLine(dir);
        mismatch(dir);

        System.out.println(failures == 0 ? "\n全部通過" : "\n失敗 " + failures + " 項");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 純文字加數值。 */
    private static void plainLine(Path dir) throws Exception {
        System.out.println("  -- 純文字 --");
        StyledText line = StyledText.fromString("§7Total Damage: §f400%");
        LineParts parts = LineParts.of(line);
        check("數值被抽成佔位符", "Total Damage: {~}".equals(parts.template()));
        check("數值本身被記下來",
                parts.numbers().size() == 1 && "400%".equals(parts.numbers().get(0).text()));

        write(dir, "plain.json", "{\"Total Damage: {~}\": \"總傷害: {~}\"}");
        TranslationStore store = load(dir);

        Component out = LineTranslator.translate(line, store);
        check("翻譯成功", out != null);
        if (out != null) {
            String plain = out.getString();
            check("譯文含中文", plain.contains("總傷害"));
            check("數值填回原位", plain.contains("400%"));
            check("佔位符沒有殘留", !plain.contains("{~}") && !plain.contains("{#}"));
        }
    }

    /**
     * 帶材質包符號的行——這是整個設計的重點。
     *
     * <p>照遊戲實際送來的結構組：符號自己一個片段、帶 {@code common} 字型，
     * 文字與數值各自獨立。
     */
    private static void glyphLine(Path dir) throws Exception {
        System.out.println("\n  -- 含材質包符號 --");
        Component raw = Component.empty()
                .append(Component.literal(GLYPH).withStyle(s -> s.withFont(COMMON)))
                .append(Component.literal(" Mana Cost: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("50").withStyle(ChatFormatting.WHITE));
        StyledText line = StyledText.fromComponent(raw);

        LineParts parts = LineParts.of(line);
        check("符號被抽成佔位符", parts.template().startsWith("{#}"));
        check("符號原文被記下來",
                parts.glyphs().size() == 1 && GLYPH.equals(parts.glyphs().get(0).text()));
        check("模板裡沒有 PUA 字元（譯者看不到，就不會誤刪）",
                parts.template().codePoints().noneMatch(cp -> cp >= 0xE000 && cp <= 0xF8FF));

        write(dir, "glyph.json",
                "{\"" + parts.template() + "\": \"{#} 魔力消耗: {~}\"}");
        TranslationStore store = load(dir);

        Component out = LineTranslator.translate(line, store);
        check("含符號的行翻譯成功", out != null);
        if (out == null) {
            return;
        }
        check("符號原樣保留", out.getString().contains(GLYPH));
        check("中文有出現", out.getString().contains("魔力消耗"));
        check("數值填回", out.getString().contains("50"));

        // 核心規則的兩面
        check("中文片段用預設字型（否則畫出來是方框）", cjkUsesDefaultFont(out));
        check("符號片段保留原本的自訂字型（否則畫不出圖示）", glyphKeepsFont(out));

        customFontText(dir);
        layoutGlyphs(dir);
        glyphsInsideText(dir);
        numericInTextFont(dir);
        formatFidelity(dir);
    }

    /**
     * 格式保真：不是文字的片段必須原封不動，含字型。
     *
     * <p>實際 tooltip 一行會混用多種字型（space 排版、banner/box 外框…）。
     * 先前把連續符號合併成一段、只套用第一個字型，其餘字元就變成方框。
     */
    private static void formatFidelity(Path dir) throws Exception {
        System.out.println();
        System.out.println("  -- 格式保真 --");
        FontDescription box =
                new FontDescription.Resource(Identifier.withDefaultNamespace("banner/box"));

        // 相鄰但字型不同的兩個符號
        Component raw = Component.empty()
                .append(Component.literal(GLYPH).withStyle(s -> s.withFont(SPACE)))
                .append(Component.literal(GLYPH).withStyle(s -> s.withFont(box)))
                .append(Component.literal("Durability").withStyle(s -> s.withFont(WYNNTILS_LANG)));
        StyledText line = StyledText.fromComponent(raw);

        LineParts parts = LineParts.of(line);
        check("不同字型的符號不會被併成一段", parts.glyphs().size() == 2);
        check("第一個符號保留 space 字型", SPACE.equals(parts.glyphs().get(0).style().getFont()));
        check("第二個符號保留 banner/box 字型", box.equals(parts.glyphs().get(1).style().getFont()));

        // 逐片段替換：只有翻到的片段改字型，其餘完全不動
        write(dir, "fidelity.json", "{\"Durability\": \"耐久度\"}");
        Component out = LineTranslator.translate(line, load(dir));
        check("翻得出來", out != null);
        if (out != null) {
            check("符號字型原樣保留", fontsPreserved(out, box));
            check("譯文用預設字型", cjkUsesDefaultFont(out));
        }

        // 完全沒翻到的行要原封不動（含字型）
        Component untouched = LineTranslator.untranslated(line);
        check("未翻譯的行保留符號字型", fontsPreserved(untouched, box));
    }

    /** 檢查輸出裡仍存在帶指定字型的片段。 */
    private static boolean fontsPreserved(Component c, FontDescription want) {
        boolean[] found = {false};
        c.visit((style, text) -> {
            if (want.equals(style.getFont())) {
                found[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    /**
     * 首尾的排版符號要能剝掉再查字典。
     *
     * <p>取自實際 tooltip：物品名稱前後各有一個 minecraft:space 的排版字元，
     * 所以模板是 {@code {#}Doom Stone{#}}，而語料的鍵是純文字 {@code Doom Stone}。
     */
    /**
     * 排版偏移黏在<b>文字片段尾端</b>的情形。
     *
     * <p>實際 tooltip 裡的樣子：{@code "Jeweling" + U+D0004 + U+D0034}——
     * 名稱與排版寬度在同一個片段。先前那些字元被直接丟掉，譯文一換上去
     * 間隔就整個消失，數值黏死在標籤旁邊。素材 tooltip 的
     * 「耐久度」「持續時間」「配方清單」全是這樣壞掉的。
     */
    private static void glyphsInsideText(Path dir) throws Exception {
        System.out.println();
        System.out.println("  -- 排版偏移黏在文字裡 --");
        String offsets = new String(Character.toChars(0xD0004))
                + new String(Character.toChars(0xD0034));
        Component raw = Component.literal("Jeweling" + offsets);
        StyledText line = StyledText.fromComponent(raw);

        LineParts parts = LineParts.of(line);
        check("尾端偏移被抽成佔位符（而不是丟掉）",
                "Jeweling{#}".equals(parts.template()));
        check("偏移原文被保管起來",
                parts.glyphs().size() == 1 && offsets.equals(parts.glyphs().get(0).text()));

        write(dir, "inside.json", "{\"Jeweling\": \"珠寶\"}");
        Component out = LineTranslator.translate(line, load(dir));
        check("查得到譯文", out != null);
        if (out != null) {
            check("譯文正確", out.getString().contains("珠寶"));
            check("排版偏移有填回去 —— 少了它欄位間隔就整個消失",
                    out.getString().endsWith(offsets));
        }
    }

    private static void layoutGlyphs(Path dir) throws Exception {
        System.out.println();
        System.out.println("  -- 首尾排版符號 --");
        Component raw = Component.empty()
                .append(Component.literal(GLYPH).withStyle(s -> s.withFont(SPACE)))
                .append(Component.literal("Waist Apron"))
                .append(Component.literal(GLYPH).withStyle(s -> s.withFont(SPACE)));
        StyledText line = StyledText.fromComponent(raw);

        check("模板含首尾符號", "{#}Waist Apron{#}".equals(LineParts.of(line).template()));

        // 字典只有純文字的鍵 —— 這正是語料的樣子
        write(dir, "layout.json", "{\"Waist Apron\": \"腰圍圍裙\"}");
        Component out = LineTranslator.translate(line, load(dir));
        check("剝掉首尾符號後查得到", out != null);
        if (out != null) {
            check("譯文正確", out.getString().contains("腰圍圍裙"));
            check("首尾符號有填回去",
                    out.getString().startsWith(GLYPH) && out.getString().endsWith(GLYPH));
        }

        // 句中的符號不能剝 —— 那是內容的一部分
        Component mid = Component.empty()
                .append(Component.literal("an "))
                .append(Component.literal(GLYPH).withStyle(s -> s.withFont(SPACE)))
                .append(Component.literal(" Item Identifier"));
        check("句中符號保留在模板裡",
                LineParts.of(StyledText.fromComponent(mid)).template().contains("{#}"));
    }

    /**
     * 數值用文字字型渲染時不能當成圖示。
     *
     * <p>實際資料：{@code +2} 帶 {@code wynntils:language} 字型。只看「有沒有字母」
     * 會把它判成圖示，模板變成 {@code Thorns {#} to {#}} 而不是 {@code {~}}。
     */
    private static void numericInTextFont(Path dir) {
        System.out.println();
        System.out.println("  -- 文字字型下的數值 --");
        Component raw = Component.empty()
                .append(Component.literal("Thorns "))
                .append(Component.literal("+2").withStyle(s -> s.withFont(WYNNTILS_LANG)))
                .append(Component.literal(" to "))
                .append(Component.literal("10%").withStyle(s -> s.withFont(WYNNTILS_LANG)));
        String tmpl = LineParts.of(StyledText.fromComponent(raw)).template();
        // 正負號保留在模板裡，與 Python 語料工具的 parametrize 一致
        check("數值變成 {~} 而不是 {#}", "Thorns +{~} to {~}".equals(tmpl));
    }

    /**
     * 帶自訂字型的<b>可讀文字</b>必須照常翻譯。
     *
     * <p>Wynncraft 對一般文字也套用自訂字型（例如 ascii）。舊規則「帶自訂字型
     * 就當圖示」會把整個 tooltip 每一行都丟掉，翻譯一條也對不上——
     * 實測 34 份 tooltip 全部 noMatch 就是這個原因。
     */
    private static void customFontText(Path dir) throws Exception {
        System.out.println();
        System.out.println("  -- 帶自訂字型的文字 --");
        Component raw = Component.empty()
                .append(Component.literal("Doom Stone").withStyle(s -> s.withFont(COMMON)));
        StyledText line = StyledText.fromComponent(raw);

        check("帶自訂字型的可讀文字不是 glyphOnly", !GlyphSplitter.isGlyphOnly(line));
        check("模板保留原文", "Doom Stone".equals(LineParts.of(line).template()));

        write(dir, "font.json", "{\"Doom Stone\": \"末日之石\"}");
        Component out = LineTranslator.translate(line, load(dir));
        check("翻得出來", out != null && out.getString().contains("末日之石"));

        // 同一個字型、內容換成純符號 —— 這種才是圖示
        Component icon = Component.empty()
                .append(Component.literal(GLYPH).withStyle(s -> s.withFont(COMMON)));
        check("同字型但純符號仍判定為圖示",
                GlyphSplitter.isGlyphOnly(StyledText.fromComponent(icon)));
    }

    /**
     * 地名參數化——專有名詞永遠不翻譯，還原時原樣填回。
     *
     * <p>順帶解決去重：137 個地名 × 每種句型，收斂成一條。
     */
    private static void placeLine(Path dir) throws Exception {
        System.out.println();
        System.out.println("  -- 地名 --");
        check("地名清單有載入", PlaceNames.size() > 100);

        StyledText troms = StyledText.fromString("Troms Citizen");
        StyledText detlas = StyledText.fromString("Detlas Citizen");
        check("不同地名收斂成同一個模板",
                LineParts.of(troms).template().equals(LineParts.of(detlas).template()));
        check("模板長成 {p} Citizen",
                "{p} Citizen".equals(LineParts.of(troms).template()));

        write(dir, "place.json", "{\"{p} Citizen\": \"{p} 市民\"}");
        TranslationStore store = load(dir);

        Component a = LineTranslator.translate(troms, store);
        Component b = LineTranslator.translate(detlas, store);
        check("Troms 這句翻得出來", a != null);
        check("Detlas 這句用同一條譯文也翻得出來", b != null);
        if (a != null && b != null) {
            check("地名原樣保留（Troms）", "Troms 市民".equals(a.getString()));
            check("地名原樣保留（Detlas）", "Detlas 市民".equals(b.getString()));
        }

        // 邊界：不該把 Ragnite 裡的 Ragni 當成地名
        check("不會誤判詞中的地名",
                !LineParts.of(StyledText.fromString("Ragnite Ore")).template().contains("{p}"));
        // 大小寫敏感：普通名詞 swamp 不該被當成地名 Swamp
        check("小寫普通名詞不算地名",
                !LineParts.of(StyledText.fromString("cross the swamp")).template().contains("{p}"));
    }

    /** 佔位符數量不符時必須整行放棄。 */
    private static void mismatch(Path dir) throws Exception {
        System.out.println("\n  -- 佔位符不符 --");
        Files.list(dir).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (Exception ignored) {
                // 清理失敗不影響驗證
            }
        });
        write(dir, "bad.json", "{\"Total Damage: {~}\": \"總傷害\"}");
        TranslationStore store = load(dir);
        StyledText line = StyledText.fromString("§7Total Damage: §f400%");
        check("少了佔位符就放棄整行（寧可不翻也不要錯位）",
                LineTranslator.translate(line, store) == null);
    }

    // ------------------------------------------------------------ 檢查工具

    /** 含中日韓字元的片段是否都用預設字型。 */
    private static boolean cjkUsesDefaultFont(Component c) {
        boolean[] ok = {true};
        c.visit((style, text) -> {
            boolean hasCjk = text.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
            if (hasCjk && !isDefaultFont(style.getFont())) {
                ok[0] = false;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return ok[0];
    }

    /** 符號片段是否仍帶著原本的自訂字型。 */
    private static boolean glyphKeepsFont(Component c) {
        boolean[] ok = {false};
        c.visit((style, text) -> {
            if (text.contains(GLYPH) && COMMON.equals(style.getFont())) {
                ok[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return ok[0];
    }

    private static boolean isDefaultFont(FontDescription font) {
        return font == null || FontDescription.DEFAULT.equals(font);
    }

    private static void write(Path dir, String name, String json) throws Exception {
        Files.writeString(dir.resolve(name), json, StandardCharsets.UTF_8);
    }

    private static TranslationStore load(Path dir) {
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static void check(String name, boolean pass) {
        System.out.printf("  [%s] %s%n", pass ? "PASS" : "FAIL", name);
        if (!pass) {
            failures++;
        }
    }
}
