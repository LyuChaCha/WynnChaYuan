package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.DialogueBuffer;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.TranslationStore;
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

public final class DialogueInlineIconTest {

    private static int failures = 0;

    private static final TextColor BODY = TextColor.fromRgb(0xE0D4C0);
    private static final TextColor FIRE = TextColor.fromRgb(0xFF5555);
    private static final TextColor AIR = TextColor.fromRgb(0xFFFFFF);
    private static final TextColor WATER = TextColor.fromRgb(0x55FFFF);
    private static final TextColor EARTH = TextColor.fromRgb(0x00AA00);
    private static final TextColor THUNDER = TextColor.fromRgb(0xFFFF55);

    private static final String KEY =
            "The elements are {#} Fire, {#} Air, {#} Water, {#} Earth , and {#} Thunder.";
    private static final String LEGACY =
            "The elements are  Fire,  Air,  Water,  Earth , and  Thunder.";
    private static final String JA =
            "属性は {#}火{/}、{#}風{/}、{#}水{/}、{#}土{/}、{#}雷{/}があります。";

    private static final String WYNNTILS =
            "The elements are  Fire,  Air,  Water,  Earth , and  Thunder.";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        DialogueRewriter.widthForTest = DialogueInlineIconTest::realWidth;
        setConfig(new CollectorConfig(Files.createTempDirectory("wynnchayuan-icon")
                .resolve("config.json")));

        templates();
        capture();
        spans();
        TranslationStore store = store("\"" + KEY + "\": \"" + JA + "\"");
        replaceWhole(store);
        replaceTyping(store);
        panelWhole(store);
        panelTyping(store);
        legacy();
        markersReplace();
        markersReplaceTyping();
        markersPanel();
        markersPanelTyping();
        markersNeverDrawn();

        System.out.println(failures == 0 ? "\n句中圖示：全部通過"
                : "\n句中圖示：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void templates() {
        DialogueIcons.forget();
        check("句中圖示收成 {#}（實際 " + DialogueIcons.parts(WYNNTILS, null).template() + "）",
                KEY.equals(DialogueIcons.parts(WYNNTILS, null).template()));
        check("舊的算法（LineParts 直接算）仍是兩個空格",
                LEGACY.equals(LineParts.of(StyledText.fromString(WYNNTILS)).template()));
        check("{#} 依序對到五個圖示",
                DialogueIcons.parts(WYNNTILS, null).glyphs().size() == 5
                && "".equals(DialogueIcons.parts(WYNNTILS, null).glyphs().get(4).text()));
        String head = " Fire is strong against Air.";
        check("開頭的圖示新舊一樣",
                DialogueIcons.parts(head, null).template().equals(
                        LineParts.of(StyledText.fromString(head)).template()));
        String plain = "Hey, recruit! You want a refresher on Elements?";
        check("沒有圖示的句子不受影響",
                DialogueIcons.parts(plain, null).template().equals(plain));
        String cursor = "The elements are" + new String(Character.toChars(0xD0010));
        check("打字游標不算句中圖示",
                DialogueIcons.parts(cursor, null).template().equals(
                        LineParts.of(StyledText.fromString(cursor)).template()));
    }

    private static void capture() {
        DialogueBuffer buffer = new DialogueBuffer();
        for (int at = 1; at <= WYNNTILS.length(); at++) {
            String raw = WYNNTILS.substring(0, at);
            buffer.offer(raw, GlyphSplitter.toTemplate(DialogueIcons.line(raw, null)));
        }
        String done = buffer.flush();
        check("收集端逐字打完送出的鍵（實際 " + done + "）", KEY.equals(done));
    }

    private static void spans() {
        List<LineParts.Piece> glyphs = List.of(
                new LineParts.Piece("", Style.EMPTY.withColor(FIRE)),
                new LineParts.Piece("", Style.EMPTY.withColor(AIR)));
        check("{#}…{/} 換成圖示的色碼",
                "{#}{c:#FF5555}火{/}、{#}{c:#FFFFFF}風{/}".equals(
                        DialogueIcons.spans("{#}火{/}、{#}風{/}", glyphs)));
        check("沒寫 {/} 就只有圖示自己",
                "{#}火、{#}風".equals(DialogueIcons.spans("{#}火、{#}風", glyphs)));
        check("{/} 之前又出現 {#} 時，前一個不算、後一個照算",
                "{#}火{#}{c:#FFFFFF}風{/}".equals(DialogueIcons.spans("{#}火{#}風{/}", glyphs)));
    }

    private record Seg(String text, Style style) {}

    private static Style body(int row) {
        return font("hud/dialogue/text/wynncraft/body_" + row);
    }

    private static Style icon(int row) {
        return font("hud/dialogue/text/common/body_" + row);
    }

    private static List<List<Seg>> rows() {
        List<Seg> first = List.of(
                new Seg("The elements are ", body(0).withColor(BODY)),
                new Seg("", icon(0).withColor(FIRE)),
                new Seg(" Fire,", body(0).withColor(FIRE)),
                new Seg(" ", body(0).withColor(BODY)),
                new Seg("", icon(0).withColor(AIR)),
                new Seg(" Air,", body(0).withColor(AIR)),
                new Seg(" ", body(0).withColor(BODY)),
                new Seg("", icon(0).withColor(WATER)),
                new Seg(" Water,", body(0).withColor(WATER)),
                new Seg(" ", body(0).withColor(BODY)),
                new Seg("", icon(0).withColor(EARTH)),
                new Seg(" Earth", body(0).withColor(EARTH)));
        List<Seg> second = List.of(
                new Seg(", and ", body(1).withColor(BODY)),
                new Seg("", icon(1).withColor(THUNDER)),
                new Seg(" Thunder.", body(1).withColor(THUNDER)));
        return List.of(first, second);
    }

    private static int length(List<List<Seg>> rows) {
        int n = 0;
        for (List<Seg> row : rows) {
            for (Seg seg : row) {
                n += seg.text().length();
            }
        }
        return n;
    }

    private static Component message(int typed) {
        return message(rows(), typed);
    }

    private static Component message(List<List<Seg>> rows, int typed) {
        MutableComponent out = Component.empty();
        int left = typed;
        boolean first = true;
        for (List<Seg> row : rows) {
            if (left <= 0) {
                break;
            }
            Style style = row.get(0).style();
            out.append(Component.literal(offset(first ? -116 : -230)).withStyle(style));
            first = false;
            for (Seg seg : row) {
                if (left <= 0) {
                    break;
                }
                String text = seg.text().substring(0, Math.min(left, seg.text().length()));
                left -= text.length();
                out.append(Component.literal(text).withStyle(seg.style()));
            }
        }
        out.append(Component.literal(offset(-40)).withStyle(body(0)));
        return out;
    }

    private static void replaceWhole(TranslationStore store) {
        DialogueRewriter.forget();
        DialogueIcons.forget();
        Component frame = message(length(rows()));
        Component out = null;
        for (int again = 0; again < 12; again++) {
            out = DialogueRewriter.rewrite(frame, store);
        }
        check("就地取代翻得出來", out != null);
        if (out == null) {
            return;
        }
        String text = out.getString();
        check("沒有把 {#}、{/} 原樣畫出來", !text.contains("{"));
        check("★ 五個圖示照原文的順序擺在對的詞前面（實際 " + visible(text) + "）",
                order(text, "火", "風", "水", "土", "雷"));
        List<String> problems = new ArrayList<>();
        out.visit((style, piece) -> {
            piece.codePoints().filter(DialogueIcons::isIcon).forEach(cp -> {
                String path = path(style);
                if (path == null || !path.contains("/common/body_")) {
                    problems.add("圖示用了 " + path);
                }
            });
            return Optional.empty();
        }, Style.EMPTY);
        check("★ 圖示用遊戲自己的 common 字型畫" + (problems.isEmpty() ? "" : problems),
                problems.isEmpty());
        check("★ 火跟著圖示是紅的", FIRE.equals(colourAt(out, "火")));
        check("★ 水跟著圖示是青的", WATER.equals(colourAt(out, "水")));
        check("★ 土跟著圖示是綠的", EARTH.equals(colourAt(out, "土")));
        check("★ 雷跟著圖示是黃的", THUNDER.equals(colourAt(out, "雷")));
        check("圖示自己是它的顏色", FIRE.equals(colourAt(out, "")));
        check("其餘是底色", BODY.equals(colourAt(out, "属性は")));
        check("{/} 之後回到底色", BODY.equals(colourAt(out, "があります")));
    }

    private static void replaceTyping(TranslationStore store) {
        DialogueRewriter.forget();
        DialogueIcons.forget();
        int total = length(rows());
        boolean shown = false;
        boolean flip = false;
        boolean braces = false;
        boolean wrongFont = false;
        String flipAt = null;
        String last = null;
        for (int at = 1; at <= total + 12; at++) {
            Component out = DialogueRewriter.rewrite(message(Math.min(at, total)), store);
            String text = out == null ? null : bodyText(out);
            boolean chinese = text != null && text.codePoints().anyMatch(cp -> cp >= 0x3040);
            if (chinese) {
                shown = true;
            } else if (shown && !flip) {
                flip = true;
                flipAt = "第 " + at + " 幀";
            }
            if (text != null && text.contains("{")) {
                braces = true;
            }
            if (out != null) {
                boolean[] bad = {false};
                out.visit((style, piece) -> {
                    if (DialogueIcons.hasIcon(piece) && piece.codePoints().anyMatch(
                            cp -> cp >= 0xE000 && cp <= 0xE00A)) {
                        String path = path(style);
                        bad[0] |= path == null || !path.contains("/common/body_");
                    }
                    return Optional.empty();
                }, Style.EMPTY);
                wrongFont |= bad[0];
            }
            last = text;
        }
        check("逐字：出過中文就不再掉回英文" + (flipAt == null ? "" : "（" + flipAt + "）"), !flip);
        check("逐字：中文真的出現過", shown);
        check("逐字：任何一幀都沒有畫出 {#}／{/}", !braces);
        check("逐字：圖示一律用 common 字型", !wrongFont);
        check("逐字：最後一幀五個圖示都在（實際 " + visible(last) + "）",
                last != null && order(last, "火", "風", "水", "土",
                        "雷"));
    }

    private static void panelWhole(TranslationStore store) {
        DialogueIcons.forget();
        DialogueIcons.note(message(length(rows())));
        StyledText line = DialogueIcons.line(WYNNTILS, store);
        String template = GlyphSplitter.toTemplate(line);
        check("小框查的是新格式的鍵", KEY.equals(template));
        DialogueOverlay.LineResult result =
                DialogueOverlay.translateLine(line, template, store, true);
        check("小框翻得出來", result != null);
        if (result == null) {
            return;
        }
        Component out = result.translated();
        String text = out.getString();
        check("小框沒有私用區字元（換成聊天那套圖示）", !DialogueIcons.hasIcon(text));
        check("★ 小框的圖示擺在對的詞前面（實際 " + text + "）",
                order(text, "✹火", "❋風", "❉水", "✤土", "✦雷"));
        check("★ 小框：✹ 是紅的", FIRE.equals(colourAt(out, "✹")));
        check("★ 小框：火跟著是紅的", FIRE.equals(colourAt(out, "火")));
        check("★ 小框：水跟著是青的", WATER.equals(colourAt(out, "水")));
        check("★ 小框：雷跟著是黃的", THUNDER.equals(colourAt(out, "雷")));
        check("小框：圖示用預設字型畫（實際 " + path(styleAt(out, "✹")) + "）",
                "default".equals(path(styleAt(out, "✹"))));
        check("小框：{/} 之後不是元素色",
                !THUNDER.equals(colourAt(out, "があります")));
    }

    private static void panelTyping(TranslationStore store) {
        DialogueIcons.forget();
        DialogueIcons.note(message(length(rows())));
        String cachedSource = null;
        String cachedText = null;
        boolean shown = false;
        boolean flip = false;
        boolean junk = false;
        String last = null;
        for (int at = 1; at <= WYNNTILS.length() + 8; at++) {
            String raw = WYNNTILS.substring(0, Math.min(at, WYNNTILS.length()));
            StyledText line = DialogueIcons.line(raw, store);
            String template = GlyphSplitter.toTemplate(line);
            String text;
            if (DialogueOverlay.canReuse(cachedSource, template)) {
                text = cachedText;
            } else {
                DialogueOverlay.LineResult result = DialogueOverlay.translateLine(
                        line, template, store, at > WYNNTILS.length() + 6);
                text = result == null ? null : result.translated().getString();
                if (result != null) {
                    cachedSource = result.source();
                    cachedText = text;
                }
            }
            if (text != null) {
                shown = true;
                junk |= text.contains("{") || DialogueIcons.hasIcon(text);
            } else if (shown) {
                flip = true;
            }
            last = text;
        }
        check("小框逐字：出過中文就不再掉回原文", shown && !flip);
        check("小框逐字：沒有佔位符或私用區字元", !junk);
        check("小框逐字：最後是整句（實際 " + last + "）",
                last != null && order(last, "✹火", "❋風", "❉水", "✤土", "✦雷"));
    }

    private static void legacy() throws Exception {
        TranslationStore old = store("\"" + LEGACY + "\": \"元素有火、風、水、地、雷。\"");
        check("舊格式的鍵退回舊模板",
                LEGACY.equals(DialogueIcons.parts(WYNNTILS, old).template()));
        DialogueRewriter.forget();
        DialogueIcons.forget();
        Component out = null;
        for (int again = 0; again < 12; again++) {
            out = DialogueRewriter.rewrite(message(length(rows())), old);
        }
        check("舊格式：就地取代照舊翻得出來",
                out != null && bodyText(out).contains("元素有火、風、水、地、雷。"));
        DialogueIcons.note(message(length(rows())));
        StyledText line = DialogueIcons.line(WYNNTILS, old);
        DialogueOverlay.LineResult result = DialogueOverlay.translateLine(
                line, GlyphSplitter.toTemplate(line), old, true);
        check("舊格式：小框照舊翻得出來",
                result != null && result.translated().getString().contains("元素有火"));
        check("舊格式：收集端記的也是舊鍵", LEGACY.equals(GlyphSplitter.toTemplate(
                DialogueIcons.line(WYNNTILS, old))));
    }

    private static final TextColor GREEN = TextColor.fromRgb(0x55FF55);
    private static final TextColor BLUE = TextColor.fromRgb(0x5555FF);
    private static final TextColor RED = TextColor.fromRgb(0xFF5555);

    private static List<List<Seg>> sapRows() {
        return List.of(
                List.of(new Seg("\"WARNING: Do NOT, under any circumstances, fill the",
                        body(0).withColor(BODY))),
                List.of(new Seg("tubes with sap doses of ", body(1).withColor(BODY)),
                        new Seg("nine", body(1).withColor(GREEN)),
                        new Seg(", ", body(1).withColor(BODY)),
                        new Seg("six", body(1).withColor(BLUE)),
                        new Seg(", and ", body(1).withColor(BODY)),
                        new Seg("three", body(1).withColor(RED))),
                List.of(new Seg("respectively.\"", body(2).withColor(BODY))));
    }

    private static final String SAP = "\"WARNING: Do NOT, under any circumstances, fill the "
            + "tubes with sap doses of nine, six, and three respectively.\"";

    private static final String SAP_JA = "「警告：いかなる状況においても、チューブにそれぞれ"
            + "{c2}9{/}、{c3}6{/}、{c4}3{/}の量の樹液を{c:gold}絶対に{/}注入して{w1}はなりません。」";

    private static TranslationStore sapStore() throws Exception {
        return store(json(SAP) + ": " + json(SAP_JA));
    }

    private static String json(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void markersReplace() throws Exception {
        TranslationStore store = sapStore();
        DialogueRewriter.forget();
        DialogueIcons.forget();
        Component frame = message(sapRows(), length(sapRows()));
        Component out = null;
        for (int again = 0; again < 12; again++) {
            out = DialogueRewriter.rewrite(frame, store);
        }
        check("標記：就地取代翻得出來", out != null);
        if (out == null) {
            return;
        }
        String text = bodyText(out);
        check("★ 標記：就地取代沒有把 {c2}、{/}、{c:gold} 畫出來（實際 " + text + "）",
                !text.contains("{") && !text.contains("}"));
        check("★ 標記：9 是 {c2}（綠）", GREEN.equals(colourAt(out, "9")));
        check("★ 標記：6 是 {c3}（藍）", BLUE.equals(colourAt(out, "6")));
        check("★ 標記：3 是 {c4}（紅）", RED.equals(colourAt(out, "3")));
        check("標記：{c:gold} 照名稱上色",
                TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GOLD)
                        .equals(colourAt(out, "絶対に")));
        check("標記：{w1} 是第一個有字母的段的顏色（底色）", BODY.equals(colourAt(out, "はなりません")));
        check("標記：{/} 之後回到底色", BODY.equals(colourAt(out, "の量の樹液を")));
    }

    private static void markersReplaceTyping() throws Exception {
        TranslationStore store = sapStore();
        DialogueRewriter.forget();
        DialogueIcons.forget();
        int total = length(sapRows());
        boolean shown = false;
        boolean flip = false;
        String leaked = null;
        Component last = null;
        for (int at = 1; at <= total + 12; at++) {
            Component out = DialogueRewriter.rewrite(message(sapRows(), Math.min(at, total)), store);
            String text = out == null ? null : bodyText(out);
            if (text != null && (text.contains("{") || text.contains("}")) && leaked == null) {
                leaked = "第 " + at + " 幀：" + text;
            }
            boolean chinese = text != null && text.codePoints().anyMatch(cp -> cp >= 0x3040);
            if (chinese) {
                shown = true;
            } else if (shown) {
                flip = true;
            }
            last = out;
        }
        check("標記逐字：任何一幀都沒有畫出標記" + (leaked == null ? "" : "（" + leaked + "）"),
                leaked == null);
        check("標記逐字：出過中文就不再掉回英文", shown && !flip);
        check("標記逐字：最後一幀 9／6／3 的顏色都對", last != null
                && GREEN.equals(colourAt(last, "9")) && BLUE.equals(colourAt(last, "6"))
                && RED.equals(colourAt(last, "3")));
    }

    private static void markersPanel() throws Exception {
        TranslationStore store = sapStore();
        DialogueIcons.forget();
        DialogueIcons.note(message(sapRows(), length(sapRows())));
        StyledText line = DialogueIcons.line(SAP, store);
        DialogueOverlay.LineResult result = DialogueOverlay.translateLine(
                line, GlyphSplitter.toTemplate(line), store, true);
        check("標記：小框翻得出來", result != null);
        if (result == null) {
            return;
        }
        Component out = result.translated();
        String text = out.getString();
        check("標記：小框沒有標記字面（實際 " + text + "）", !text.contains("{"));
        check("★ 標記：小框 9 是綠的", GREEN.equals(colourAt(out, "9")));
        check("★ 標記：小框 6 是藍的", BLUE.equals(colourAt(out, "6")));
        check("★ 標記：小框 3 是紅的", RED.equals(colourAt(out, "3")));
        check("標記：小框 {c:gold}",
                TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GOLD)
                        .equals(colourAt(out, "絶対に")));
    }

    private static void markersPanelTyping() throws Exception {
        TranslationStore store = sapStore();
        DialogueIcons.forget();
        int total = length(sapRows());
        String cachedSource = null;
        Component cached = null;
        int cachedGen = -1;
        boolean shown = false;
        boolean flip = false;
        boolean leaked = false;
        Component last = null;
        for (int at = 1; at <= total + 8; at++) {
            int typed = Math.min(at, total);
            DialogueIcons.note(message(sapRows(), typed));
            String raw = SAP.substring(0, Math.min(typed + rowsBefore(sapRows(), typed), SAP.length()));
            StyledText line = DialogueIcons.line(raw, store);
            String template = GlyphSplitter.toTemplate(line);
            Component out;
            if (DialogueOverlay.canReuse(cachedSource, template)) {
                out = cached;
                if (cachedGen != DialogueIcons.generation()) {
                    Component again = DialogueOverlay.recolour(line, cachedSource, store);
                    if (again != null) {
                        out = again;
                        cached = again;
                    }
                    cachedGen = DialogueIcons.generation();
                }
            } else {
                DialogueOverlay.LineResult result = DialogueOverlay.translateLine(
                        line, template, store, at > total + 6);
                out = result == null ? null : result.translated();
                if (result != null) {
                    cachedSource = result.source();
                    cached = out;
                    cachedGen = DialogueIcons.generation();
                }
            }
            if (out != null) {
                shown = true;
                leaked |= out.getString().contains("{");
            } else if (shown) {
                flip = true;
            }
            last = out;
        }
        check("標記小框逐字：出過中文就不再掉回原文", shown && !flip);
        check("標記小框逐字：沒有標記字面", !leaked);
        check("★ 標記小框逐字：打完之後 3 也變紅（顏色晚到也補得上）", last != null
                && RED.equals(colourAt(last, "3")) && GREEN.equals(colourAt(last, "9")));
    }

    private static int rowsBefore(List<List<Seg>> rows, int typed) {
        int seen = 0;
        int gaps = 0;
        for (int n = 0; n < rows.size(); n++) {
            int len = 0;
            for (Seg seg : rows.get(n)) {
                len += seg.text().length();
            }
            seen += len;
            if (typed > seen && n + 1 < rows.size()) {
                gaps++;
            }
        }
        return gaps;
    }

    private static void markersNeverDrawn() throws Exception {
        String src = "Hello there, traveller.";
        TranslationStore store = store(json(src) + ": "
                + json("{c9}你好{/}，{c:nosuchcolour}旅人{/}{/}。"));
        DialogueRewriter.forget();
        DialogueIcons.forget();
        List<List<Seg>> rows = List.of(List.of(new Seg(src, body(0).withColor(BODY))));
        Component out = null;
        for (int again = 0; again < 12; again++) {
            out = DialogueRewriter.rewrite(message(rows, src.length()), store);
        }
        String text = out == null ? "" : bodyText(out);
        check("標記：對不到的標記也吃掉（實際 " + text + "）",
                out != null && text.contains("你好，旅人。") && !text.contains("{"));

        TranslationStore choices = store(json("Who are you?") + ": " + json("{c2}你{/}是誰？"));
        Style choice = font("hud/dialogue/text/wynncraft/choice_1");
        MutableComponent message = Component.empty();
        message.append(Component.literal(offset(42)).withStyle(choice));
        message.append(Component.literal("Who are you?").withStyle(choice));
        message.append(Component.literal(offset(-73)).withStyle(choice));
        Component picked = DialogueRewriter.rewrite(message, choices);
        String row = picked == null ? "" : picked.getString();
        check("標記：選項列也把標記吃掉（實際 " + visible(row) + "）",
                row.contains("你是誰？") && !row.contains("{"));
    }

    private static TranslationStore store(String entry) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-icon-store");
        Files.writeString(dir.resolve("npc.json"), "{\n " + entry + "\n}\n",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static String bodyText(Component out) {
        StringBuilder text = new StringBuilder();
        out.visit((style, piece) -> {
            String path = path(style);
            if (path != null && path.contains("/body_") || path != null
                    && path.startsWith("dialogue/")) {
                text.append(piece);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return text.toString();
    }

    private static boolean order(String text, String... parts) {
        int from = 0;
        for (String part : parts) {
            int at = text.indexOf(part, from);
            if (at < 0) {
                return false;
            }
            from = at + part.length();
        }
        return true;
    }

    private static TextColor colourAt(Component out, String needle) {
        Style style = styleAt(out, needle);
        return style == null ? null : style.getColor();
    }

    private static Style styleAt(Component out, String needle) {
        Style[] found = {null};
        out.visit((style, piece) -> {
            if (found[0] == null && piece.contains(needle)) {
                found[0] = style;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static String path(Style style) {
        return style != null && style.getFont() instanceof FontDescription.Resource r
                ? r.id().getPath() : null;
    }

    private static String visible(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp >= 0xCF000) {
                return;
            }
            out.append(DialogueIcons.isIcon(cp) ? String.format("<%04X>", cp)
                    : new String(Character.toChars(cp)));
        });
        return out.toString();
    }

    private static String offset(int px) {
        return new String(Character.toChars(0xD0000 + px));
    }

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace(path)));
    }

    private static void setConfig(CollectorConfig config) throws Exception {
        var field = WynnChaYuan.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, config);
    }

    private static int realWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += c >= 0x1100 && c <= 0xFFDC ? 10 : 6;
        }
        return w;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
