package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChatUnwrapTest {

    private static int failures = 0;

    private static final String G = com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER;

    public static void main(String[] args) throws Exception {
        unwrapping();
        lookup();
        System.out.println(failures == 0
                ? "聊天併句：全部通過" : "聊天併句：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void unwrapping() {
        String want = G + " The Karoshi Union World Event starts in {~}m {~}s! "
                + "({~} blocks away) Click to track";
        String[] wrapped = {
            G + " The Karoshi Union World Event starts in {~}m {~}s! ({~}\n"
                    + G + " blocks away) Click to track",
            G + " The Karoshi Union World Event starts in {~}m {~}s! \n"
                    + G + " ({~} blocks away) Click to track",
            G + " The Karoshi Union World Event starts in {~}m {~}s! ({~} blocks\n"
                    + G + " away) Click to track",
        };
        for (String w : wrapped) {
            String got = TranslationStore.unwrap(w);
            report("三種斷法併出同一句（實際：" + got + "）", want.equals(got));
        }
        report("沒斷行的原樣（只壓空白）",
                want.equals(TranslationStore.unwrap(want)));

        String middle = "Can be used to craft " + G + " Boots and " + G + " Leggings";
        report("句子中間的圖示留著（實際：" + TranslationStore.unwrap(middle) + "）",
                middle.equals(TranslationStore.unwrap(middle)));
    }

    private static void lookup() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-unwrap");
        Files.writeString(dir.resolve("misc.json"),
                "{\"The Karoshi Union World Event starts in {~}m {~}s!"
                        + " ({~} blocks away) Click to track\": \""
                        + "過勞工會 世界事件將在 {~1} 分 {~2} 秒後開始！"
                        + "(距離 {~3} 格) 點擊追蹤\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        String[] wrapped = {
            "The Karoshi Union World Event starts in 5m 12s! (300\nblocks away) Click to track",
            "The Karoshi Union World Event starts in 5m 12s! \n(300 blocks away) Click to track",
            "The Karoshi Union World Event starts in 5m 12s! (300 blocks\naway) Click to track",
        };
        for (String w : wrapped) {
            Object hit = LineTranslator.translateChat(StyledText.fromString(w), store);
            String zh = hit == null ? null
                    : ((net.minecraft.network.chat.Component) hit).getString();
            report("斷在第 " + w.indexOf('\n') + " 個字也翻得出來（實際："
                            + (zh == null ? "查不到" : zh.replace("\n", " / ")) + "）",
                    zh != null && zh.contains("過勞工會") && zh.contains("點擊追蹤"));
        }

        Object none = LineTranslator.translateChat(
                StyledText.fromString("Some completely different message\nthat is not in the corpus"),
                store);
        report("語料裡沒有的照樣查不到", none == null);

        glyphPool();
        colours();
    }

    private static void colours() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-colour");
        Files.writeString(dir.resolve("misc.json"),
                "{\"The Necromantic Site World Event starts in {~}m {~}s!"
                        + " ({~} blocks away) Click to track\": \""
                        + "{c1}亡靈法陣 世界事件將在 {~1} 分 {~2} 秒後開始！{/}"
                        + "{c2}(距離 {~3} 格) {/}{c3}點擊追蹤{/}\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        StyledText line = StyledText.fromString(
                "§bThe Necromantic Site World Event starts in 3m 59s! §7(425\n"
                        + "§7blocks away) §d§nClick to track");
        Object hit = LineTranslator.translateChat(line, store);
        if (hit == null) {
            report("有顏色的也翻得出來", false);
            return;
        }
        java.util.List<String> text = new java.util.ArrayList<>();
        java.util.List<String> style = new java.util.ArrayList<>();
        ((net.minecraft.network.chat.Component) hit).visit((s, t) -> {
            text.add(t);
            style.add((s.getColor() == null ? "-" : s.getColor().serialize())
                    + (s.isUnderlined() ? "+底線" : ""));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);

        String joined = String.join("", text);
        report("整句翻出來了（實際：" + joined + "）", joined.contains("點擊追蹤"));

        String click = null;
        for (int i = 0; i < text.size(); i++) {
            if (text.get(i).contains("點擊追蹤")) {
                click = style.get(i);
            }
        }
        report("「點擊追蹤」是粉紅加底線（實際：" + click + "）",
                click != null && click.contains("底線") && !click.startsWith("-"));

        String head = null;
        for (int i = 0; i < text.size(); i++) {
            if (text.get(i).contains("亡靈法陣")) {
                head = style.get(i);
            }
        }
        report("主句沒有被上成粉紅（實際：" + head + "）",
                head != null && !head.equals(click));
    }

    private static void glyphPool() {
        java.util.List<com.wynnchayuan.capture.LineParts.Piece> all = java.util.List.of(
                new com.wynnchayuan.capture.LineParts.Piece("A", null),
                new com.wynnchayuan.capture.LineParts.Piece("B", null),
                new com.wynnchayuan.capture.LineParts.Piece("C", null));
        String template = G + " starts in {~}m {~}s! ({~}\n"
                + G + " blocks away) craft " + G + " Boots";
        java.util.List<com.wynnchayuan.capture.LineParts.Piece> kept =
                TemplateRenderer.unwrappedGlyphs(template, all);
        report("續行的圖示拿掉、其餘留著（實際："
                        + kept.stream().map(com.wynnchayuan.capture.LineParts.Piece::text).toList() + "）",
                kept.size() == 2 && kept.get(0).text().equals("A")
                        && kept.get(1).text().equals("C"));

        java.util.List<com.wynnchayuan.capture.LineParts.Piece> flat = TemplateRenderer.unwrappedGlyphs(
                G + " starts in {~}m! craft " + G + " Boots",
                java.util.List.of(all.get(0), all.get(2)));
        report("沒有換行時一個都不少", flat.size() == 2);
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
