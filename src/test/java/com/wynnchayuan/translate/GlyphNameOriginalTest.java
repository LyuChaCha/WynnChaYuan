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

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class GlyphNameOriginalTest {

    private static final String ICON = String.valueOf((char) 0xE000);

    private static final String TAIL = String.valueOf((char) 0xE001);

    private static final String TOME = "Arsonist's Tome of Allegiance";

    private static final String WRAPPED = "{#}" + TOME + "{#}";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-glyph-name");
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
        Files.writeString(dir.resolve("ability-terms.json"),
                "{\"Allegiance\": \"效忠\", \"Tome\": \"書\"}", StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(CollectorConfig.ItemNames.BOTH);
        UnappliedAudit.clear();

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

        String swapped = TermSubstitution.termsWithin(shown, store);
        check("★ 折行前換詞不動附的原文（實際 " + swapped + "）",
                swapped == null || swapped.contains("(" + TOME + ")"));
        check("「 (」的位置找得到（實際 " + store.appendedOriginalAt(shown) + "）",
                store.appendedOriginalAt(shown) == shown.indexOf(" ("));
        String body = TermSubstitution.termsWithin("Tome of Allegiance grants power", store);
        check("一般內文照樣換詞（實際 " + body + "）",
                body != null && body.contains("效忠") && body.contains("書"));

        ToIntFunction<String> measure = GlyphNameOriginalTest::cells;
        String broken = TermSubstitution.breakBeforeOriginal(shown, cells(WRAPPED), store, measure);
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

        store.setNameMode(CollectorConfig.ItemNames.ON);
        same("只給譯名：不附原文", ICON + "戀火效忠書卷" + TAIL, text(ICON, TOME, TAIL, store));
        store.setNameMode(CollectorConfig.ItemNames.OFF);
        same("關掉物品名稱：留原文", ICON + TOME + TAIL, text(ICON, TOME, TAIL, store));
        store.setNameMode(CollectorConfig.ItemNames.BOTH);

        same("一般的名字照舊", "潮湧戰鬥精通書卷 II (Abyssal Tome of Combat Mastery II)",
                store.lookup("Abyssal Tome of Combat Mastery II"));

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

        same("查表：帶地名的名字不附原文", "{p} 旗幟", store.lookup("{p} Banner"));
        String banner = text(ICON.repeat(5), "Detlas Banner", "", store);
        same("測試列真的被認成地名", "{#}{#}{#}{#}{#}{p} Banner", lastTemplate);
        check("★ 名字帶地名的也翻得出來（實際 " + plain(banner) + "）",
                banner.endsWith("Detlas 旗幟"));
        check("這幾種都沒有被記成「翻了卻沒顯示」（實際 " + UnappliedAudit.toJson() + "）",
                UnappliedAudit.size() == 0);

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

    private static void realCorpus() throws Exception {
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");
        String[] tomes = {TOME, "Dragon's Tome of Mysticism III"};
        try (var langs = Files.list(root)) {
            for (Path lang : langs.filter(Files::isDirectory).sorted().toList()) {
                if (!Files.exists(lang.resolve("tome.json"))) {
                    continue;
                }
                Path dir = Files.createTempDirectory("wynnchayuan-glyph-name-real");
                Files.copy(lang.resolve("tome.json"), dir.resolve("tome.json"));
                Files.copy(lang.resolve("misc.json"), dir.resolve("misc.json"));
                TranslationStore store = new TranslationStore();
                store.loadAll(dir);
                for (String tome : tomes) {
                    store.setNameMode(CollectorConfig.ItemNames.ON);
                    String name = store.lookup(tome);
                    if (name == null && untranslated(lang.resolve("tome.json"), tome)) {
                        continue;
                    }
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

    private static boolean untranslated(Path file, String src) throws Exception {
        com.google.gson.JsonObject entries = com.google.gson.JsonParser.parseString(
                Files.readString(file, java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("entries");
        for (String key : entries.keySet()) {
            com.google.gson.JsonObject e = entries.getAsJsonObject(key);
            if (src.equals(e.get("src").getAsString())) {
                return e.get("dst").getAsString().isBlank();
            }
        }
        return false;
    }

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

    private static Component line(String before, String name, String after,
            TranslationStore store) {
        StyledText styled = StyledText.fromComponent(source(before, name, after));
        lastTemplate = com.wynnchayuan.capture.LineParts.of(styled).template();
        return LineTranslator.translate(styled, store);
    }

    private static String lastTemplate = "";

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

}
