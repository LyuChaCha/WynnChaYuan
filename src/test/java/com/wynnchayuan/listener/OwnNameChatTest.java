package com.wynnchayuan.listener;

import com.google.gson.JsonObject;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.SelfNames;
import com.wynnchayuan.capture.UnappliedAudit;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 有譯文卻套不上去的句子：不提玩家名字的譯文要畫得出來，真的對不上的要記下來。
 *
 * <h2>實機回報（issue #1158）</h2>
 * Cinfras 的告示板在任務完成後多一句：
 *
 * <pre>
 *   Note: We would like to thank yool141 for all the help they brought us. …
 * </pre>
 *
 * 譯者補了譯文，畫面上還是英文，而 {@code captured.json} 裡那一句也不見了——
 * 收集端問的是「語料有沒有這一條」，顯示端卻因為佔位符對不上整句放棄，
 * 於是這一句哪裡都看不到。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>譯文照規矩寫 {@code {u}}：名字填回去；</li>
 *   <li>★ 譯文<b>沒寫</b> {@code {u}}（不提名字、或把名字直接寫出來）：照樣畫得出來；</li>
 *   <li>★ 真的對不上的（多一個 {@code {u}}、少一個數值）：畫面留原文，
 *       但 {@code unapplied} 記得到是哪一句、差在哪；</li>
 *   <li>語料根本沒有的句子不記——那是缺口，歸 {@code entries} 管；</li>
 *   <li>帶著別人名字的原文不記，這份檔案是要交出去的。</li>
 * </ul>
 */
public final class OwnNameChatTest {

    private static int failures = 0;

    private static final String NAME = "yool141";
    private static final String TAIL = " for all the help they brought us."
            + " We believe they're the one who helped. We're actually not sure. Thanks anyway!";
    private static final String NOTE = "Note: We would like to thank {u}" + TAIL;
    /** 實機那一則的樣子：整句灰色斜體，名字粗體。 */
    private static final String SHOWN = "§7§oNote: We would like to thank §l" + NAME + "§7§o" + TAIL;

    public static void main(String[] args) throws Exception {
        SelfNames.learn("- Nickname: " + NAME);
        StyledText message = StyledText.fromString(SHOWN);
        String template = GlyphSplitter.toTemplate(message);
        check("名字收成 {u}，模板就是語料的鍵（實際 " + template + "）", NOTE.equals(template));

        // ---- 照規矩寫 ----
        TranslationStore store = store(NOTE, "注: {u} さんのご協力に感謝します。");
        String got = chat(message, store);
        check("譯文有 {u}：名字填回去（實際 " + got + "）",
                ("注: " + NAME + " さんのご協力に感謝します。").equals(got));
        check("套得上的不記", UnappliedAudit.size() == 0);

        // ---- ★ 譯文沒寫 {u} ----
        store = store(NOTE, "注: 皆さんのご協力に感謝します。");
        got = chat(message, store);
        check("★ 譯文不提名字也畫得出來（實際 " + got + "）",
                "注: 皆さんのご協力に感謝します。".equals(got));
        store = store(NOTE, "注: " + NAME + " さんのご協力に感謝します。");
        got = chat(message, store);
        check("★ 譯文把名字直接寫出來也畫得出來（實際 " + got + "）",
                ("注: " + NAME + " さんのご協力に感謝します。").equals(got));
        check("這兩種都不算套不上", UnappliedAudit.size() == 0);

        // ---- ★ 真的對不上：留原文，但要記得到 ----
        UnappliedAudit.clear();
        store = store(NOTE, "注: {u} さん、{u} さんに感謝します。");
        check("語料有這一條，所以收集端不會把它當缺口", store.hasTranslation(template));
        check("多一個 {u}：填不出來，留原文", chat(message, store) == null);
        check("★ 記下來了（實際 " + UnappliedAudit.size() + " 條）", UnappliedAudit.size() == 1);
        JsonObject row = UnappliedAudit.size() == 1
                ? UnappliedAudit.toJson().get(0).getAsJsonObject() : new JsonObject();
        check("★ 記的是原文、譯文、差在哪（實際 " + row + "）",
                row.has("src") && NOTE.equals(row.get("src").getAsString())
                        && row.get("dst").getAsString().startsWith("注: {u} さん、{u}")
                        && "{u}: src 1, dst 2".equals(row.get("why").getAsString()));
        check("有變動要存檔", UnappliedAudit.takeDirty());
        chat(message, store);
        chat(message, store);
        check("同一句再來幾次還是一條、也不必再存", UnappliedAudit.size() == 1
                && !UnappliedAudit.takeDirty());

        store = store("You have {~} emeralds left.", "綠寶石不多了。");
        StyledText emeralds = StyledText.fromString("§7You have 12 emeralds left.");
        check("少一個數值：留原文", chat(emeralds, store) == null);
        check("★ 也記下來（實際 " + UnappliedAudit.toJson() + "）", UnappliedAudit.size() == 2
                && "{~}: src 1, dst 0".equals(UnappliedAudit.toJson().get(1)
                        .getAsJsonObject().get("why").getAsString()));

        // ---- 不該記的 ----
        int before = UnappliedAudit.size();
        check("語料沒有的句子翻不出來",
                chat(StyledText.fromString("§7Nothing in the corpus says this 7 times."), store) == null);
        check("沒有譯文的不記——那是缺口，不是套不上", UnappliedAudit.size() == before);
        // 「X has thrown a …」的主詞是別人的名字，這份檔案要交出去
        check("★ 帶著別人名字的原文不記", UnappliedAudit.audit(
                "SomePlayer has thrown a {~} Bomb", "某人丟了炸彈") == null);

        // ---- 判準本身：跟 rebuildAll 同一套 ----
        check("對得上的回 null", UnappliedAudit.why("{#} Go to {p} in {~}s", "{#} {~} 秒內到 {p}") == null);
        check("少一個圖示（實際 " + UnappliedAudit.why("{#} Hi {#}", "{#} 嗨") + "）",
                "{#}: src 2, dst 1".equals(UnappliedAudit.why("{#} Hi {#}", "{#} 嗨")));
        check("多一個地名", "{p}: src 1, dst 2".equals(UnappliedAudit.why("Go to {p}", "去 {p} 與 {p}")));
        check("名字可以少", UnappliedAudit.why("Hey, {u}!", "嘿！") == null);
        check("指名的數值可以重排、可以重複",
                UnappliedAudit.why("{~} of {~}", "共 {~2} 個裡的 {~1} 個（{~1}）") == null);
        check("指名指到不存在的（實際 " + UnappliedAudit.why("{~} of {~}", "{~3} 個") + "）",
                UnappliedAudit.why("{~} of {~}", "{~3} 個") != null);
        check("兩種都差的話兩種都寫", "{#}: src 1, dst 0; {~}: src 1, dst 0".equals(
                UnappliedAudit.why("{#} {~} left", "沒了")));

        // ---- 譯文重新載入：改好的不該留著 ----
        UnappliedAudit.clear();
        check("清掉之後是空的，而且要存檔", UnappliedAudit.size() == 0 && UnappliedAudit.takeDirty());
        store = store(NOTE, "注: {u} さん、{u} さんに感謝します。");
        chat(message, store);
        check("清掉之後同一句會重新問", UnappliedAudit.size() == 1);
        UnappliedAudit.clear();

        System.out.println(failures == 0
                ? "套不上去的譯文：全部通過" : "套不上去的譯文：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 就地取代模式走的那兩步：先聊天專用的，查不到再退回通用的。見 ChatListener#replaceInPlace。 */
    private static String chat(StyledText message, TranslationStore store) {
        Component hit = LineTranslator.translateChat(message, store);
        if (hit == null) {
            hit = LineTranslator.translate(message, store);
        }
        return hit == null ? null : hit.getString();
    }

    private static TranslationStore store(String src, String dst) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-unapplied");
        JsonObject one = new JsonObject();
        one.addProperty(src, dst);
        Files.writeString(dir.resolve("misc.json"), one.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
