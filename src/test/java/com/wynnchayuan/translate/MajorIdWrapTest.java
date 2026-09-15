package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * 每一條 Major ID，照遊戲的樣子斷行之後，<b>說明</b>都要翻得出來。
 *
 * <h2>為什麼要整份語料跑一遍</h2>
 * 使用者回報神話弓 Air In A Can 的底部是
 *
 * <pre>
 *   ◆ 自由跑者: When your sprint
 *   bar is under 30% full,
 *   increase your sprint speed by
 *   +150%.
 * </pre>
 *
 * 名稱翻了、說明整段英文，而 {@code major-id.json} 裡那一句的原文一字不差。
 * 先前那次同樣症狀是語料過期；這次不是——Fabled 物品的說明<b>續行開頭多了
 * 一串 {@code À}</b>（Wynncraft 的縮排字元，讓續行對齊在「◆ 」後面），疊出兩個破口：
 *
 * <ol>
 *   <li>跨行查表只剝得掉行首的<b>圖示</b>，剝不掉以文字形式出現的縮排，
 *       攤平的鍵夾著「ÀÀ」，永遠對不上語料。</li>
 *   <li>「同一段不能翻一半」那道守門沒擋住：續行開頭是 À 不是小寫，認不出是同一句；
 *       而且只有縮排的片段會查到語料裡的「ÀÀ」，續行被算成「翻好了」。</li>
 * </ol>
 *
 * <p>之前的測試都是一條一條手捏的，捏的人沒想到的形狀就測不到。所以這裡拿
 * 語料裡<b>每一條</b>說明，照 Wynncraft 的寬度自己斷行、兩種排版
 * （一般的、帶縮排的）、兩種量法（無字型、有字型會重新折行）各走一次真正的
 * {@code TooltipPanel#translateLines}。
 *
 * <h2>Wynncraft 的斷行寬度</h2>
 * 伺服器端斷行，跟玩家的字型無關，所以按<b>字數</b>算。從實機錄到的斷行反推
 * （Transcendence、Efflorescence、Blinding Lights、Wavebreak、Freerunner）：
 * 一行最多 29 個字，名稱與冒號算在內、行首的圖示不算；句尾的標點可以超出
 * （{@code gains 3 less mana from Meteor.} 是 30 個字仍在同一行，而
 * {@code Damage of Swan Dive, Serpent's} 同樣 30 個字卻被折開）。
 */
public final class MajorIdWrapTest {

    private static int failures = 0;

    private static PrintStream out;

    /** 見類別說明「Wynncraft 的斷行寬度」。 */
    private static final int WYNN_ROW_CHARS = 29;

    private static final Style PINK = Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6));
    private static final Style GREY = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
    private static final Style ICON = Style.EMPTY.withFont(
            new net.minecraft.network.chat.FontDescription.Resource(
                    net.minecraft.resources.Identifier.withDefaultNamespace("common")));

    /** Fabled 物品續行開頭的縮排。在 Wynncraft 的字型裡 À 是一格固定寬度的空白。 */
    private static final String INDENT = "ÀÀ";

    /** 名稱前面那顆項目符號（材質包圖示）。 */
    private static final String BULLET = "";

    /** 說明裡的圖示（元素、資源符號）。 */
    private static final String ICON_CHAR = "";

    /**
     * 目前已知會失敗、而且<b>原因不在程式</b>的說明，以語料的鍵列出。每一條都要寫明原因。
     * 空的最好——要加的時候就是希望有人看一眼的時候。
     */
    private static final Set<String> KNOWN = Set.of();

    private static final Path MAJOR_ID = Path.of(
            "src/main/resources/assets/wynnchayuan/translations",
            Languages.DEFAULT, "major-id.json");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build"));
        out = new PrintStream(new java.io.FileOutputStream("build/majorid-wrap.txt"),
                true, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        airInACan(store);
        halfParagraphStaysEnglish(store);
        corpus(store);

        say(failures == 0 ? "MajorIdWrap: 全部通過" : "MajorIdWrap: " + failures + " 項失敗");
        out.close();
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 使用者回報的那一份，一行一行照抄。 */
    private static void airInACan(TranslationStore store) {
        say("-- Air In A Can / Freerunner --");
        String[] rows = {"When your sprint", "bar is under 30% full,",
                         "increase your sprint speed by", "+150%."};
        for (String indent : new String[] {"", INDENT}) {
            List<Component> tip = tooltip(majorIdRows("Freerunner", rows, indent));
            String all = render(tip, store, null);
            String label = indent.isEmpty() ? "續行沒有縮排" : "續行帶 À 縮排";
            check(label + "：名稱翻了（實際：" + oneLine(all) + "）", all.contains("自由跑者"));
            check(label + "：說明翻了", all.contains("衝刺條"));
            check(label + "：沒有留下英文說明", !all.contains("When your sprint")
                    && !all.contains("bar is under") && !all.contains("increase your sprint"));
            check(label + "：數值填回去了", all.contains("30%") && all.contains("+150%"));
            // 會重新折行的量法：行數不能比原文多
            String wrapped = render(tip, store, MajorIdWrapTest::width);
            check(label + "（重新折行）：說明翻了（實際：" + oneLine(wrapped) + "）",
                    wrapped.contains("衝刺條") && !wrapped.contains("When your sprint"));
            check(label + "（重新折行）：行數沒有比原文多",
                    rowsOf(tip, store, MajorIdWrapTest::width) <= tip.size());
        }
        // 只剩縮排的片段不算翻譯。語料裡有整行只剩「À」的條目，查得到「ÀÀ」——
        // 那樣整行會被算成翻好了，譯文卻跟原文一模一樣。
        Component indented = majorIdRows("Freerunner", rows, INDENT).get(1);
        check("只有縮排的片段不會被當成翻好了",
                LineTranslator.translate(StyledText.fromComponent(indented), store, false, false)
                        == null);
    }

    /**
     * 反面：說明<b>查不到</b>時，整段要留英文，不能只剩名稱是中文。
     *
     * <p>「同一段不能一半中文一半英文」是靠「下一行小寫開頭就是同一句」認段落的。
     * 續行開頭是 {@code À} 的話它不是小寫，段落就斷成一行一段——使用者看到的
     * 「自由跑者: When your sprint」正是這樣漏過去的。
     */
    private static void halfParagraphStaysEnglish(TranslationStore store) {
        say("-- 查不到的說明整段留英文 --");
        String[] rows = {"When your sprint", "bar is under 30% empty,",
                         "increase your sprint speed by", "+150%."};
        String all = render(tooltip(majorIdRows("Freerunner", rows, INDENT)), store, null);
        check("不會只把名稱換成中文（實際：" + oneLine(all) + "）", !all.contains("自由跑者"));
    }

    /**
     * @param key    說明在語料裡的鍵
     * @param src    說明的原文（模板）
     * @param sample 填好數值與圖示、畫面上會出現的那一句
     */
    private record Pair(String name, String nameDst, String key, String src,
                        String sample, String descDst) {}

    private static void corpus(TranslationStore store) throws Exception {
        say("-- 整份 major-id.json --");
        JsonObject entries = JsonParser.parseString(
                Files.readString(MAJOR_ID, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("entries");
        List<String[]> names = new ArrayList<>();
        List<Pair> pairs = new ArrayList<>();
        List<JsonObject> descs = new ArrayList<>();
        List<String> descKeys = new ArrayList<>();
        for (String key : entries.keySet()) {
            JsonObject e = entries.getAsJsonObject(key);
            String role = e.has("role") ? e.get("role").getAsString() : "";
            if ("name".equals(role)) {
                names.add(new String[] {e.get("src").getAsString(),
                        e.has("dst") ? e.get("dst").getAsString() : ""});
            } else if ("desc".equals(role)) {
                descs.add(e);
                descKeys.add(key);
            }
        }
        // 語料裡名稱與說明是兩串，<b>沒有</b>記誰配誰（說明的鍵被重新編號過，
        // 名稱比說明多一條）。這裡照順序輪流借一個名稱來組第一行——名稱只影響
        // 第一行有多長，查表時名稱與說明本來就是各自查的，配錯不影響結果。
        // 真正的一對（Freerunner）由上面的 airInACan 另外驗。
        for (int i = 0; i < descs.size(); i++) {
            JsonObject e = descs.get(i);
            String[] name = names.get(i % names.size());
            String src = e.get("src").getAsString();
            String dst = e.has("dst") ? e.get("dst").getAsString() : "";
            String raw = e.has("_raw") ? e.get("_raw").getAsString() : null;
            if (name[1].isBlank() || dst.isBlank()) {
                continue;
            }
            pairs.add(new Pair(name[0], name[1], descKeys.get(i), src, sample(raw, src), dst));
        }
        say("  名稱 " + names.size() + " 條、有譯文的說明 " + pairs.size() + " 條");

        String[] layouts = {"", INDENT};
        List<ToIntFunction<Component>> measures = new ArrayList<>();
        measures.add(null);
        measures.add(MajorIdWrapTest::width);
        for (String indent : layouts) {
            for (ToIntFunction<Component> measure : measures) {
                String mode = (indent.isEmpty() ? "一般" : "À 縮排")
                        + (measure == null ? "" : "＋重新折行");
                List<String> failed = new ArrayList<>();
                int unexpected = 0;
                for (Pair p : pairs) {
                    List<String> rows = wrap(p.name(), p.sample());
                    List<Component> tip = tooltip(
                            majorIdRows(p.name(), rows.toArray(String[]::new), indent));
                    String why = verdict(p, rows, render(tip, store, measure), store);
                    if (why == null) {
                        continue;
                    }
                    boolean known = KNOWN.contains(p.key());
                    failed.add((known ? "(已知) " : "") + p.key() + "（借名 " + p.name() + "）：" + why);
                    if (!known) {
                        unexpected++;
                    }
                }
                say("  [" + mode + "] " + (pairs.size() - failed.size()) + "/" + pairs.size()
                        + " 條說明翻得出來");
                for (String f : failed) {
                    say("      " + f);
                }
                check("[" + mode + "] 沒有預期外的失敗（" + unexpected + " 條）", unexpected == 0);
            }
        }
    }

    /**
     * 畫面上會出現的那一句。
     *
     * <p>有 {@code _raw} 就用它，數值才是真的——但 {@code _raw} 不一定可靠：
     * 有的把圖示寫成字面的「{#}」，有的整個漏掉圖示。所以換好圖示之後模板化一次，
     * 跟語料的 src 對不上就退回拿 src 自己填。
     */
    private static String sample(String raw, String src) {
        if (raw != null) {
            String filled = raw.replace("{#}", ICON_CHAR);
            if (template(filled).equals(src)) {
                return filled;
            }
        }
        return src.replace("{~}", "12").replace("{#}", ICON_CHAR);
    }

    /** @return 失敗的原因；翻好了回傳 {@code null} */
    private static String verdict(Pair p, List<String> rows, String all, TranslationStore store) {
        // 重新折行會把一串方塊字切在兩行之間，比對前先把換行拿掉
        String joined = all.replace("\n", "");
        String longest = longestHan(strip(p.descDst()));
        boolean nameOk = joined.contains(strip(p.nameDst()));
        boolean descOk = longest.length() < 2 || joined.contains(longest);
        boolean english = false;
        for (int i = 1; i < rows.size(); i++) {
            String row = GlyphSplitter.stripGlyphChars(rows.get(i)).strip();
            if (row.chars().filter(Character::isLetter).count() >= 6 && all.contains(row)) {
                english = true;
            }
        }
        if (nameOk && descOk && !english) {
            return null;
        }
        String reason = store.lookupFlat(template(p.sample())) == null
                ? "語料查不到這句說明（" + template(p.sample()) + "）" : "程式路徑";
        return (nameOk ? "" : "名稱沒翻 ") + (descOk ? "" : "說明沒翻 ")
                + (english ? "留著英文續行 " : "") + "— " + reason + "｜輸出：" + oneLine(all);
    }

    /** 模板化，跟語料的 src 同一個形狀。 */
    private static String template(String text) {
        return com.wynnchayuan.capture.LineParts.of(StyledText.fromComponent(parts(text, GREY)))
                .template();
    }

    // ------------------------------------------------------------------ 組 tooltip

    /**
     * 照 Wynncraft 的寬度把「名稱: 說明」斷行。
     *
     * @return 每一行的文字；第一行<b>不含</b>名稱，名稱由 {@link #majorIdRows} 接上
     */
    static List<String> wrap(String name, String desc) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        int prefix = name.length() + 2;               // 「Name: 」
        for (String word : desc.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String tried = row.length() == 0 ? word : row + " " + word;
            int budget = WYNN_ROW_CHARS - (rows.isEmpty() ? prefix : 0);
            if (row.length() > 0 && counted(tried) > budget) {
                rows.add(row.toString());
                row.setLength(0);
                row.append(word);
            } else {
                row.setLength(0);
                row.append(tried);
            }
        }
        rows.add(row.toString());
        return rows;
    }

    /** 算字數：圖示不算，句尾的標點可以超出。 */
    private static int counted(String row) {
        String core = row.replaceAll("[.,]+$", "");
        return (int) core.codePoints().filter(cp -> !GlyphSplitter.isGlyphCodePoint(cp)).count();
    }

    /**
     * 遊戲送來的 Major ID 那幾行：第一行是「圖示＋偏移＋粉紅的名稱＋灰色的說明」，
     * 續行是灰色的說明，Fabled 物品的續行前面多一串 {@code À}。
     */
    private static List<Component> majorIdRows(String name, String[] rows, String indent) {
        List<Component> out = new ArrayList<>();
        MutableComponent first = Component.empty();
        first.append(Component.literal(BULLET).withStyle(ICON.withColor(PINK.getColor())));
        first.append(Component.literal(SpaceOffset.encode(2)).withStyle(SpaceOffset.styleFor(PINK)));
        first.append(Component.literal(name + ": ").withStyle(PINK));
        first.append(parts(rows[0], GREY));
        out.add(first);
        for (int i = 1; i < rows.length; i++) {
            MutableComponent row = Component.empty();
            if (!indent.isEmpty()) {
                row.append(Component.literal(indent).withStyle(GREY));
            }
            row.append(parts(rows[i], GREY));
            out.add(row);
        }
        return out;
    }

    /** 文字切成片段：私用區字元當圖示（自訂字型），其餘是文字。 */
    private static MutableComponent parts(String text, Style style) {
        MutableComponent line = Component.empty();
        StringBuilder run = new StringBuilder();
        boolean glyph = false;
        for (int cp : text.codePoints().toArray()) {
            boolean g = GlyphSplitter.isGlyphCodePoint(cp);
            if (run.length() > 0 && g != glyph) {
                line.append(Component.literal(run.toString()).withStyle(glyph ? ICON : style));
                run.setLength(0);
            }
            glyph = g;
            run.appendCodePoint(cp);
        }
        if (run.length() > 0) {
            line.append(Component.literal(run.toString()).withStyle(glyph ? ICON : style));
        }
        return line;
    }

    /** Major ID 放進一份物品 tooltip 裡：前後有別的行、中間隔著空行。 */
    private static List<Component> tooltip(List<Component> majorId) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal("Sample Bow").withStyle(PINK));
        tip.add(Component.literal(""));
        tip.addAll(majorId);
        tip.add(Component.literal(""));
        tip.add(Component.literal("Fabled Item").withStyle(PINK));
        return tip;
    }

    // ------------------------------------------------------------------ 跑一次

    private static String render(List<Component> tip, TranslationStore store,
                                 ToIntFunction<Component> measure) {
        LineTranslator.measureForTest = measure;
        try {
            List<Component> got = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
            if (got.isEmpty()) {
                got = tip;                             // 一行都沒翻 = 整份原文
            }
            StringBuilder sb = new StringBuilder();
            for (Component c : got) {
                sb.append(c.getString()).append('\n');
            }
            return sb.toString();
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

    private static int rowsOf(List<Component> tip, TranslationStore store,
                              ToIntFunction<Component> measure) {
        LineTranslator.measureForTest = measure;
        try {
            List<Component> got = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
            return got.isEmpty() ? tip.size() : got.size();
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

    /** 粗略的 Minecraft 字寬：空白 4、方塊字 12、其他 6；À 縮排算一格空白。 */
    private static int width(Component c) {
        int px = 0;
        for (int cp : c.getString().codePoints().toArray()) {
            px += cp == ' ' || cp == 'À' ? 4 : cp >= 0x2E80 ? 12 : 6;
        }
        return px;
    }

    // ------------------------------------------------------------------ 小工具

    /** 拿掉 {~1}、{c1}、{/} 那些記號。 */
    private static String strip(String text) {
        return text.replaceAll("\\{[^}]*\\}", "");
    }

    /** 最長的一串方塊字。拿它當「說明有翻到」的標記，不必釘特定用詞。 */
    private static String longestHan(String text) {
        String best = "";
        StringBuilder run = new StringBuilder();
        for (int cp : text.codePoints().toArray()) {
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                run.appendCodePoint(cp);
            } else {
                if (run.length() > best.length()) {
                    best = run.toString();
                }
                run.setLength(0);
            }
        }
        return run.length() > best.length() ? run.toString() : best;
    }

    private static String oneLine(String text) {
        String one = text.replace('\n', '|');
        return one.length() > 90 ? one.substring(0, 90) + "…" : one;
    }

    private static void say(String line) {
        System.out.println(line);
        out.println(line);
    }

    private static void check(String what, boolean ok) {
        say("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
