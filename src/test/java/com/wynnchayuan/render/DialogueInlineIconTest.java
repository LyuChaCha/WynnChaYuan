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

/**
 * 句中夾著元素圖示的對話（PR #1435，Detlas 士兵）兩條路都翻得出來、圖示在對的位置、
 * 顏色跟著對應的詞。見 {@link DialogueIcons}。
 *
 * <pre>
 *   The elements are ✹ Fire, ❋ Air, ❉ Water, ✤ Earth, and ✦ Thunder.
 *                    紅紅紅紅  白白白   青青青青   綠綠綠綠       黃黃黃黃黃
 * </pre>
 *
 * <p>照 memory 的教訓，兩條路都<b>逐字</b>模擬：打字途中每一幀都會跑一次，
 * 只拿整句測抓不到「打到圖示那一幀掉回英文」這一類。
 */
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

    /** Wynntils 給小框與收集端的純文字：圖示是對話字型的私用區字元，列與列之間一個空白。 */
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

        System.out.println(failures == 0 ? "\n句中圖示：全部通過"
                : "\n句中圖示：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ 模板

    private static void templates() {
        DialogueIcons.forget();
        check("句中圖示收成 {#}（實際 " + DialogueIcons.parts(WYNNTILS, null).template() + "）",
                KEY.equals(DialogueIcons.parts(WYNNTILS, null).template()));
        check("舊的算法（LineParts 直接算）仍是兩個空格",
                LEGACY.equals(LineParts.of(StyledText.fromString(WYNNTILS)).template()));
        check("{#} 依序對到五個圖示",
                DialogueIcons.parts(WYNNTILS, null).glyphs().size() == 5
                && "".equals(DialogueIcons.parts(WYNNTILS, null).glyphs().get(4).text()));
        // 頭尾的圖示本來就收成 {#}，新舊算法要一樣，既有的鍵才不會變
        String head = " Fire is strong against Air.";
        check("開頭的圖示新舊一樣",
                DialogueIcons.parts(head, null).template().equals(
                        LineParts.of(StyledText.fromString(head)).template()));
        String plain = "Hey, recruit! You want a refresher on Elements?";
        check("沒有圖示的句子不受影響",
                DialogueIcons.parts(plain, null).template().equals(plain));
        // 第 13 平面的排版位移（打字游標）不收：那是頭尾規則的事
        String cursor = "The elements are" + new String(Character.toChars(0xD0010));
        check("打字游標不算句中圖示",
                DialogueIcons.parts(cursor, null).template().equals(
                        LineParts.of(StyledText.fromString(cursor)).template()));
    }

    /** 收集端逐字餵進緩衝區，送出來的鍵要跟語料寫的一樣（不然玩家收到的鍵對不上）。 */
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

    // ------------------------------------------------------------------ 就地取代

    /** 一段文字與它的樣式。 */
    private record Seg(String text, Style style) {}

    private static Style body(int row) {
        return font("hud/dialogue/text/wynncraft/body_" + row);
    }

    private static Style icon(int row) {
        return font("hud/dialogue/text/common/body_" + row);
    }

    /** 實機的兩列：第一列到 Earth 為止，「, and」被折到第二列。 */
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

    /** 打到第 {@code typed} 個字元那一幀的 action bar：[前導][列][位移][列][尾隨]。 */
    private static Component message(int typed) {
        MutableComponent out = Component.empty();
        int left = typed;
        boolean first = true;
        for (List<Seg> row : rows()) {
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

    /**
     * 逐字打出來：出過中文之後不准變回英文、不准畫出佔位符、圖示永遠用 common 字型。
     */
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

    // ------------------------------------------------------------------ 小框

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

    /** 小框也逐字：跟 DialogueOverlay#setCurrent 一樣先問快取，問不到才重算。 */
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

    // ------------------------------------------------------------------ 舊格式

    /** 語料只有舊格式（兩個空格）的句子：照舊翻得出來，只是沒有圖示。 */
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
        // 收集端：語料認得舊格式就記舊格式，不另外生一條新格式的鍵
        check("舊格式：收集端記的也是舊鍵", LEGACY.equals(GlyphSplitter.toTemplate(
                DialogueIcons.line(WYNNTILS, old))));
    }

    // ------------------------------------------------------------------

    private static TranslationStore store(String entry) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-icon-store");
        Files.writeString(dir.resolve("npc.json"), "{\n " + entry + "\n}\n",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    /** 對話內文那幾段（body／common 字型）的字，位移與其他東西不算。 */
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
                return;                            // 位移字元不印
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

    /** 中日韓一個字 10px，其餘 6px（圖示也算 10px，跟實機的 9px 差不多）。 */
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
