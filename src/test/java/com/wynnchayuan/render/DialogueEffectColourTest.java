package com.wynnchayuan.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
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

public final class DialogueEffectColourTest {

    private static int failures = 0;

    private static final TextColor SHAKE = TextColor.fromRgb(0x0AEB28);
    private static final TextColor FADED_SHAKE = TextColor.fromRgb(0x00F018);
    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);
    private static final TextColor GOLD = TextColor.fromRgb(0xFFAA00);

    private static final String KEY = "Off the Rails#731";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        DialogueLayout.widthForTest = DialogueEffectColourTest::realWidth;
        setConfig(new CollectorConfig(Files.createTempDirectory("wynnchayuan-effect")
                .resolve("config.json")));

        String[] line = corpusLine("ja_jp");
        String src = line[0];
        String dst = line[1];
        check("語料那一句沒有顏色標記", !DialogueIcons.hasColourMarks(dst));
        TranslationStore store = store(src, dst);
        int split = src.indexOf(' ') + 1;
        String head = src.substring(0, split);
        String tail = src.substring(split);

        keys();
        replaceWhole(store, head, tail, null, SHAKE, "晃動、底色沒設");
        replaceWhole(store, head, tail, null, FADED_SHAKE, "淡出晃動、底色沒設");
        replaceWhole(store, head, tail, WHITE, SHAKE, "晃動、底色白");
        replaceTyping(store, head, tail, SHAKE);
        allEffect(store, src);
        realAccentStays();
        panel(store, head, tail, SHAKE);

        System.out.println(failures == 0 ? "\n對話特效色：全部通過"
                : "\n對話特效色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void keys() {
        check("晃動的色碼是特效鍵", DialogueAccents.shaderKey(SHAKE));
        check("淡出晃動的色碼是特效鍵", DialogueAccents.shaderKey(FADED_SHAKE));
        check("白色不是特效鍵", !DialogueAccents.shaderKey(WHITE));
        check("金色不是特效鍵", !DialogueAccents.shaderKey(GOLD));
        check("綠色 §a 不是特效鍵", !DialogueAccents.shaderKey(TextColor.fromRgb(0x55FF55)));
        check("沒設顏色不是特效鍵", !DialogueAccents.shaderKey(null));
    }

    private static void replaceWhole(TranslationStore store, String head, String tail,
                                     TextColor base, TextColor effect, String what) {
        DialogueTyping.forget();
        DialogueIcons.forget();
        Component frame = message(head, tail, base, effect, head.length() + tail.length());
        Component out = null;
        for (int again = 0; again < 12; again++) {
            out = DialogueRewriter.rewrite(frame, store);
        }
        check(what + "：翻得出來", out != null && hasKana(bodyText(out)));
        if (out == null) {
            return;
        }
        List<TextColor> seen = kanaColours(out);
        check(what + "：譯文沒有任何一個字是特效色（實際 " + seen + "）",
                !seen.isEmpty() && seen.stream().noneMatch(DialogueAccents::shaderKey));
        check(what + "：譯文是這一行的底色（實際 " + seen + "）",
                seen.stream().allMatch(c -> java.util.Objects.equals(c, base)));
    }

    private static void replaceTyping(TranslationStore store, String head, String tail,
                                      TextColor effect) {
        DialogueTyping.forget();
        DialogueIcons.forget();
        int total = head.length() + tail.length();
        boolean shown = false;
        String bad = null;
        for (int at = 1; at <= total + 12; at++) {
            Component out = DialogueRewriter.rewrite(
                    message(head, tail, null, effect, Math.min(at, total)), store);
            if (out == null || !hasKana(bodyText(out))) {
                continue;
            }
            shown = true;
            if (bad == null && kanaColours(out).stream().anyMatch(DialogueAccents::shaderKey)) {
                bad = "第 " + at + " 幀";
            }
        }
        check("逐字：譯文出現過", shown);
        check("逐字：每一幀的譯文都沒有特效色" + (bad == null ? "" : "（" + bad + "）"),
                bad == null);
    }

    private static void allEffect(TranslationStore store, String src) {
        DialogueTyping.forget();
        DialogueIcons.forget();
        Component frame = message("", src, null, SHAKE, src.length());
        Component out = null;
        for (int again = 0; again < 12; again++) {
            out = DialogueRewriter.rewrite(frame, store);
        }
        check("整句都是特效：翻得出來", out != null && hasKana(bodyText(out)));
        if (out != null) {
            List<TextColor> seen = kanaColours(out);
            check("整句都是特效：譯文不畫成特效色（實際 " + seen + "）",
                    seen.stream().noneMatch(DialogueAccents::shaderKey));
        }
    }

    private static void realAccentStays() {
        List<String> texts = new ArrayList<>(List.of("Find the ", "Pearl", " for me"));
        List<Style> styles = List.of(Style.EMPTY.withColor(WHITE),
                Style.EMPTY.withColor(GOLD), Style.EMPTY.withColor(WHITE));
        TextColor tone = DialogueAccents.tone(texts, styles, List.of(0), List.of(2));
        check("一般的強調色照舊：底色是散文那色", WHITE.equals(tone));
        check("一般的強調色照舊：Pearl 仍是強調",
                DialogueAccents.accents(texts, styles, List.of(0), List.of(2), tone)
                        .stream().anyMatch(p -> p.text().equals("Pearl")
                                && GOLD.equals(p.style().getColor())));
        List<String> shaking = new ArrayList<>(List.of("Run ", "RUN RUN RUN"));
        List<Style> shook = List.of(Style.EMPTY.withColor(WHITE), Style.EMPTY.withColor(SHAKE));
        TextColor tone2 = DialogueAccents.tone(shaking, shook, List.of(0), List.of(1));
        check("特效段比較長也不當底色", WHITE.equals(tone2));
        check("特效段不當強調色貼回譯文",
                DialogueAccents.accents(shaking, shook, List.of(0), List.of(1), tone2).isEmpty());
    }

    private static void panel(TranslationStore store, String head, String tail,
                              TextColor effect) {
        DialogueIcons.forget();
        String src = head + tail;
        DialogueIcons.note(message(head, tail, null, effect, src.length()));
        StyledText line = DialogueIcons.line(src, store);
        String template = GlyphSplitter.toTemplate(line);
        DialogueOverlay.LineResult result =
                DialogueOverlay.translateLine(line, template, store, true);
        check("小框：翻得出來", result != null && hasKana(result.translated().getString()));
        if (result != null) {
            List<TextColor> seen = kanaColours(result.translated());
            check("小框：譯文沒有特效色（實際 " + seen + "）",
                    seen.stream().noneMatch(DialogueAccents::shaderKey));
        }
    }

    private static Component message(String head, String tail, TextColor base,
                                     TextColor effect, int typed) {
        Style body = font("hud/dialogue/text/wynncraft/body_0");
        MutableComponent out = Component.empty();
        out.append(Component.literal(offset(-116)).withStyle(body));
        int left = typed;
        String a = head.substring(0, Math.min(left, head.length()));
        left -= a.length();
        if (!a.isEmpty()) {
            out.append(Component.literal(a).withStyle(base == null ? body : body.withColor(base)));
        }
        String b = tail.substring(0, Math.max(0, Math.min(left, tail.length())));
        if (!b.isEmpty()) {
            out.append(Component.literal(b).withStyle(body.withColor(effect)));
        }
        out.append(Component.literal(offset(-40)).withStyle(body));
        return out;
    }

    private static String[] corpusLine(String lang) throws Exception {
        Path file = Path.of("src/main/resources/assets/wynnchayuan/translations/" + lang
                + "/quest/off-the-rails.json");
        JsonObject all = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject entry = all.getAsJsonObject("entries").getAsJsonObject(KEY);
        return new String[] {entry.get("src").getAsString(), entry.get("dst").getAsString()};
    }

    private static TranslationStore store(String src, String dst) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-effect-store");
        JsonObject flat = new JsonObject();
        flat.addProperty(src, dst);
        Files.writeString(dir.resolve("npc.json"), flat.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static boolean hasKana(String text) {
        return text != null && text.codePoints().anyMatch(cp -> cp >= 0x3040 && cp <= 0x30FF);
    }

    private static List<TextColor> kanaColours(Component out) {
        List<TextColor> seen = new ArrayList<>();
        out.visit((style, piece) -> {
            if (hasKana(piece)) {
                seen.add(style.getColor());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return seen;
    }

    private static String bodyText(Component out) {
        StringBuilder text = new StringBuilder();
        out.visit((style, piece) -> {
            text.append(piece);
            return Optional.empty();
        }, Style.EMPTY);
        return text.toString();
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
