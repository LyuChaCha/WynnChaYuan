package com.wynnchayuan.render;

import com.google.gson.JsonObject;
import com.wynnchayuan.ModConfig;
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

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DialogueShakeTest {

    private static final String SRC = "The COSMOS bends to my will!! The STARS, the INDESCRIBABLY"
            + " COUNTLESS STARS!!! THE STARS AND THE INKY VOID BETWEEN, THEY WILL STRIKE YOU DOWN!!!!";

    private static final String PLAIN = "宇宙は俺の意志に従う！！星々、言葉では言い表せないほど無数の星々！！！"
            + "星々と、その間にある漆黒の虚空が、お前を打ち倒すのだ！！！！";

    private static final String MARKED = "{c2}宇宙{/}は俺の意志に従う!! {c2}星々{/}、言葉では言い表せないほど"
            + "無数の星々！！！星々と、その間にある漆黒の虚空が、{c2}お前を打ち倒すのだ!!!!{/}";

    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    private static final int ROOM = 208;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        DialogueLayout.widthForTest = DialogueShakeTest::realWidth;
        setConfig(new ModConfig(Files.createTempDirectory("wynnchayuan-shake")
                .resolve("config.json")));
        String was = WynnChaYuan.language();
        setLanguage("ja_jp");
        try {
            palette();
            settle();
            replaceWhole(store(PLAIN), "沒有標記");
            replaceWhole(store(MARKED), "有標記");
            markedColours();
            replaceTyping(store(PLAIN), "沒有標記");
            replaceTyping(store(MARKED), "有標記");
            panelTyping(store(PLAIN), "沒有標記");
            panelTyping(store(MARKED), "有標記");
        } finally {
            setLanguage(was);
        }
        System.out.println(failures == 0 ? "\n晃動的對話：全部通過"
                : "\n晃動的對話：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void palette() {
        DialogueIcons.forget();
        DialogueIcons.note(frame(SRC.length(), 1));
        List<TextColor> first = DialogueIcons.palette();
        DialogueIcons.note(frame(SRC.length(), 2));
        List<TextColor> second = DialogueIcons.palette();
        check("晃動字每字一個色碼也只佔一格（實際 " + first.size() + " 格）", first.size() == 2);
        check("每一 tick 力道不同，{c2} 仍指到晃動那一格",
                second.size() == first.size() && ShaderColours.marker(second.get(1)));
    }

    private static void settle() {
        TextColor shake = shake(200);
        check("晃動色是特效鍵", ShaderColours.marker(shake));
        check("晃動色在 TTF 上的顏色是白色", WHITE.equals(ShaderColours.resting(shake)));
        check("一般顏色原樣", TextColor.fromRgb(0xFFAA00)
                .equals(ShaderColours.resting(TextColor.fromRgb(0xFFAA00))));
        check("斜體青的特效色在 TTF 上是青色", TextColor.fromRgb(0x55FFFF)
                .equals(ShaderColours.resting(TextColor.fromRgb(0x00F01C))));
    }

    private static void replaceWhole(TranslationStore store, String what) {
        DialogueTyping.forget();
        DialogueIcons.forget();
        Component out = null;
        for (int again = 0; again < 12; again++) {
            out = DialogueRewriter.rewrite(frame(SRC.length(), again), store);
        }
        check(what + "：就地取代翻得出來", out != null && hasKana(text(out)));
        if (out == null) {
            return;
        }
        String bad = rawOnTtf(out);
        check(what + "：TTF 畫的字沒有特效色（實際 " + bad + "）", bad == null);
    }

    private static void markedColours() {
        TranslationStore store = store(MARKED);
        DialogueTyping.forget();
        DialogueIcons.forget();
        Component out = null;
        for (int again = 0; again < 12; again++) {
            out = DialogueRewriter.rewrite(frame(SRC.length(), again), store);
        }
        if (out == null) {
            check("有標記：翻得出來", false);
            return;
        }
        List<TextColor> bangs = new ArrayList<>();
        List<TextColor> kana = new ArrayList<>();
        out.visit((style, piece) -> {
            if (piece.contains("!!!!")) {
                bangs.add(style.getColor());
            }
            if (piece.contains("宇宙")) {
                kana.add(style.getColor());
            }
            return Optional.empty();
        }, Style.EMPTY);
        check("有標記：遊戲字型畫的 !!!! 保留晃動色（實際 " + bangs + "）",
                !bangs.isEmpty() && bangs.stream().allMatch(ShaderColours::marker));
        check("有標記：宇宙 是白色不是黃色（實際 " + kana + "）",
                !kana.isEmpty() && kana.stream().allMatch(WHITE::equals));
    }

    private static void replaceTyping(TranslationStore store, String what) {
        DialogueTyping.forget();
        DialogueIcons.forget();
        boolean shown = false;
        String bad = null;
        int gone = 0;
        for (int at = 1; at <= SRC.length() + 12; at++) {
            Component out = DialogueRewriter.rewrite(frame(Math.min(at, SRC.length()), at), store);
            boolean kana = out != null && hasKana(text(out));
            if (shown && !kana && at > SRC.length()) {
                gone++;
            }
            shown |= kana;
            String raw = out == null ? null : rawOnTtf(out);
            if (bad == null && raw != null) {
                bad = "第 " + at + " 幀 " + raw;
            }
        }
        check(what + "：逐字就地取代譯文出現過", shown);
        check(what + "：打完之後譯文不消失（" + gone + " 幀）", gone == 0);
        check(what + "：逐字每一幀 TTF 字都沒有特效色" + (bad == null ? "" : "（" + bad + "）"),
                bad == null);
    }

    private static void panelTyping(TranslationStore store, String what) {
        DialogueIcons.forget();
        boolean shown = false;
        boolean whole = false;
        for (int at = 1; at <= SRC.length(); at++) {
            DialogueIcons.note(frame(at, at));
            String typed = SRC.substring(0, at);
            StyledText line = DialogueIcons.line(typed, store);
            DialogueOverlay.LineResult result = DialogueOverlay.translateLine(line,
                    GlyphSplitter.toTemplate(line), store, at == SRC.length());
            boolean kana = result != null && hasKana(result.translated().getString());
            shown |= kana;
            whole = kana && at == SRC.length();
        }
        check(what + "：小框逐字翻得出來", shown);
        check(what + "：小框打完是整句譯文", whole);
    }

    private static String rawOnTtf(Component out) {
        String[] bad = {null};
        out.visit((style, piece) -> {
            if (bad[0] == null && ShaderColours.marker(style.getColor())
                    && !DialogueFonts.drawable(piece)) {
                bad[0] = piece + " " + style.getColor();
            }
            return Optional.empty();
        }, Style.EMPTY);
        return bad[0];
    }

    private static Component frame(int typed, int tick) {
        List<String> rows = DialogueLayout.wrap(SRC, 5, null, ROOM);
        MutableComponent out = Component.empty();
        int left = typed;
        int pos = 0;
        for (int n = 0; n < rows.size() && left > 0; n++) {
            Style body = font("hud/dialogue/text/wynncraft/body_" + n);
            String row = rows.get(n);
            int start = SRC.indexOf(row, pos);
            pos = start + row.length();
            out.append(Component.literal(offset(-(ROOM - DialogueRewriter.BODY_LEFT)))
                    .withStyle(body));
            int take = Math.min(left, row.length());
            left -= take + (n + 1 < rows.size() ? 1 : 0);
            int width = 0;
            StringBuilder calm = new StringBuilder();
            for (int k = 0; k < take; k++) {
                char c = row.charAt(k);
                width += realWidth(String.valueOf(c));
                if (!shaking(start + k)) {
                    calm.append(c);
                    continue;
                }
                flush(out, calm, body.withColor(WHITE));
                Style shaken = body.withColor(shake(strength(start + k, tick)));
                if (start + k >= SRC.indexOf("STRIKE")) {
                    shaken = shaken.withBold(true);
                }
                out.append(Component.literal(String.valueOf(c)).withStyle(shaken));
            }
            flush(out, calm, body.withColor(WHITE));
            out.append(Component.literal(offset(-width)).withStyle(body));
        }
        return out;
    }

    private static void flush(MutableComponent out, StringBuilder run, Style style) {
        if (!run.isEmpty()) {
            out.append(Component.literal(run.toString()).withStyle(style));
            run.setLength(0);
        }
    }

    private static boolean shaking(int at) {
        int from = at;
        while (from > 0 && Character.isLetter(SRC.charAt(from - 1))) {
            from--;
        }
        int to = at;
        while (to < SRC.length() && Character.isLetter(SRC.charAt(to))) {
            to++;
        }
        String word = SRC.substring(from, to);
        if (word.isEmpty()) {
            return at > 0 && shaking(at - 1) && !Character.isWhitespace(SRC.charAt(at));
        }
        return word.length() > 1 && word.equals(word.toUpperCase());
    }

    private static int strength(int at, int tick) {
        return 64 + Math.floorMod(at * 73 + tick * 151, 192);
    }

    private static TextColor shake(int strength) {
        return TextColor.fromRgb(strength << 16 | 235 << 8 | 24);
    }

    private static TranslationStore store(String dst) {
        try {
            Path dir = Files.createTempDirectory("wynnchayuan-shake-store");
            JsonObject flat = new JsonObject();
            flat.addProperty(SRC, dst);
            Files.writeString(dir.resolve("npc.json"), flat.toString(), StandardCharsets.UTF_8);
            TranslationStore store = new TranslationStore();
            store.loadAll(dir);
            return store;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean hasKana(String text) {
        return text != null && text.codePoints().anyMatch(cp -> cp >= 0x3040 && cp <= 0x30FF
                || cp >= 0x4E00 && cp <= 0x9FFF);
    }

    private static String text(Component out) {
        StringBuilder all = new StringBuilder();
        out.visit((style, piece) -> {
            all.append(piece);
            return Optional.empty();
        }, Style.EMPTY);
        return all.toString();
    }

    private static String offset(int px) {
        return new String(Character.toChars(DialogueRewriter.OFFSET_BASE + px));
    }

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace(path)));
    }

    private static void setConfig(ModConfig config) throws Exception {
        var field = WynnChaYuan.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, config);
    }

    private static void setLanguage(String lang) throws Exception {
        var field = WynnChaYuan.class.getDeclaredField("language");
        field.setAccessible(true);
        field.set(null, lang);
    }

    private static int realWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += c >= 0x1100 && c <= 0xFFDC ? 10 : 6;
        }
        return w;
    }
}
