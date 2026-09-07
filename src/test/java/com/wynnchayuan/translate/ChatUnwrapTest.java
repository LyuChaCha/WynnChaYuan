package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 聊天訊息斷在哪裡都要查得到。
 *
 * <h2>先前壞在哪</h2>
 * 世界事件的通知是伺服器<b>先斷好行</b>送過來的，每一行開頭還掛著頻道圖示：
 *
 * <pre>
 *   {#} The Karoshi Union World Event starts in {~}m {~}s! ({~}
 *   {#} blocks away) Click to track
 * </pre>
 *
 * 斷在哪裡取決於<b>事件名多長</b>加上玩家自己設定的聊天欄寬度。語料裡現成的
 * 三條就有三種斷法：
 *
 * <pre>
 *   …{~}s! ({~}\n{#} blocks away) Click to track
 *   …{~}s! \n{#} ({~} blocks away) Click to track
 *   …{~}s! ({~} blocks\n{#} away) Click to track
 * </pre>
 *
 * 於是七十個世界事件要收的不是七十條，是「事件 × 斷點」——而斷點算不出來。
 *
 * <p>{@code flat} 那份索引接不住：它只壓空白，續行的 {@code {#}} 會留在原地，
 * 而它的位置正好隨斷點移動。所以另外收一份把續行圖示也拿掉的。
 *
 * <h2>這條測試在盯什麼</h2>
 * 語料只收<b>一條併起來的</b>，三種斷法都要查得到。反面同樣重要：
 * 句子中間的圖示不能被當成續行拿掉，那會讓譯文少一個圖示。
 */
public final class ChatUnwrapTest {

    private static int failures = 0;

    /** 材質包圖示的模板佔位符。 */
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

    /** 併句本身：三種斷法要併出同一個鍵。 */
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

        // ★ 句子中間的圖示是<b>內容</b>，不能拿掉——拿掉譯文就少一個圖示，
        //   rebuild 會判定佔位符數量對不上而整條放棄。
        String middle = "Can be used to craft " + G + " Boots and " + G + " Leggings";
        report("句子中間的圖示留著（實際：" + TranslationStore.unwrap(middle) + "）",
                middle.equals(TranslationStore.unwrap(middle)));
    }

    /**
     * 端到端：語料只收一條，三種斷法都要翻得出來。
     *
     * <p>這裡刻意<b>不放圖示</b>。混在一般文字裡的材質包字元會被
     * {@code GlyphSplitter#stripGlyphChars} 清掉而不是抽成 {@code {#}}，
     * 用 {@code StyledText.fromString} 造不出實機那種「圖示自成一段」的結構。
     * 符號池那一半另外單獨測，見 {@link #glyphPool}。
     */
    private static void lookup() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-unwrap");
        // 鍵是併起來的一句，dst 也是一句
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

        // 反面：語料裡沒有的還是要回 null，不能因為併句就亂配
        Object none = LineTranslator.translateChat(
                StyledText.fromString("Some completely different message\nthat is not in the corpus"),
                store);
        report("語料裡沒有的照樣查不到", none == null);

        glyphPool();
        colours();
    }

    /**
     * 併句之後每一段還是要拿到原文那一段的顏色。
     *
     * <h2>實機回報</h2>
     * 世界事件通知的「{@code Click to track}」原文是粉紅色加底線，譯文的
     * 「點擊追蹤」卻掉回整行的主色、也沒有底線。
     *
     * <p>原因是貼樣式那一步是<b>比對字面</b>的——譯文是中文，跟
     * {@code Click to track} 對不上，於是那一段拿不到自己的顏色。
     *
     * <p>語料本來就有顏色佔位符可以直接指定（{@code {c1}}…{@code {/}}），
     * 世界事件那七十條都改成這樣寫。這裡釘住 {@code {cN}} 的編號確實對應到
     * 原文<b>由左到右第 N 種樣式</b>——編號錯掉的話顏色會整組對調，
     * 那比沒有顏色更難看出問題。
     */
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

        // 實機那一行的三種樣式：主色、灰色的距離、粉紅加底線的「點擊追蹤」
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

        // ★ 「點擊追蹤」要拿到粉紅加底線，不是整行的主色
        String click = null;
        for (int i = 0; i < text.size(); i++) {
            if (text.get(i).contains("點擊追蹤")) {
                click = style.get(i);
            }
        }
        report("「點擊追蹤」是粉紅加底線（實際：" + click + "）",
                click != null && click.contains("底線") && !click.startsWith("-"));

        // ★ 反面：主句那一段不能也被上成粉紅
        String head = null;
        for (int i = 0; i < text.size(); i++) {
            if (text.get(i).contains("亡靈法陣")) {
                head = style.get(i);
            }
        }
        report("主句沒有被上成粉紅（實際：" + head + "）",
                head != null && !head.equals(click));
    }

    /**
     * 符號池要跟著少掉續行的圖示。
     *
     * <p>{@link TranslationStore#unwrap} 把續行的行首圖示拿掉了，語料那條是照
     * 併起來的樣子寫的，要的 {@code {#}} 就少一個。池子沒跟著少的話
     * {@code rebuildAll} 會判定佔位符數量對不上而整條放棄。
     */
    private static void glyphPool() {
        java.util.List<com.wynnchayuan.capture.LineParts.Piece> all = java.util.List.of(
                new com.wynnchayuan.capture.LineParts.Piece("A", null),     // 行首的頻道圖示
                new com.wynnchayuan.capture.LineParts.Piece("B", null),     // 續行的行首圖示 —— 要拿掉
                new com.wynnchayuan.capture.LineParts.Piece("C", null));    // 句子中間的圖示 —— 要留著
        String template = G + " starts in {~}m {~}s! ({~}\n"
                + G + " blocks away) craft " + G + " Boots";
        java.util.List<com.wynnchayuan.capture.LineParts.Piece> kept =
                LineTranslator.unwrappedGlyphs(template, all);
        report("續行的圖示拿掉、其餘留著（實際："
                        + kept.stream().map(com.wynnchayuan.capture.LineParts.Piece::text).toList() + "）",
                kept.size() == 2 && kept.get(0).text().equals("A")
                        && kept.get(1).text().equals("C"));

        // 沒有換行時一個都不該少
        java.util.List<com.wynnchayuan.capture.LineParts.Piece> flat = LineTranslator.unwrappedGlyphs(
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
