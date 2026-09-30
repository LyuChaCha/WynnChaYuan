package com.wynnchayuan.render;

import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.file.Path;

/**
 * 譯文塞不進對話框時，改讓小框接手，而不是整句退回英文。
 *
 * <h2>回報（yool141，#902）</h2>
 * 「部分译文过长，导致部分内容会切换回英文（如果将任务翻译设置在另一个框中，
 * 就能正确翻译）。」
 *
 * <h2>為什麼就地取代放不下</h2>
 * 就地取代只能寫進 Wynncraft <b>已經送來</b>的 {@code body_N} 那幾行，而框的高度
 * 也是照那個行數畫出來的——憑空多畫一行會穿出框外。所以「譯文比原文多一行」
 * 在就地取代這條路上沒有解。
 *
 * <p>量過各語言之後（見 {@link DialogueFitLangTest}）才知道這件事有多大：
 * 繁中在標準框寬下一條都不會中，但西文有 843 條、有頭像的窄框下更是 3,996 條，
 * 接近兩成的台詞。那些先前全部退回英文。
 *
 * <h2>這支釘住什麼</h2>
 * <ul>
 *   <li>翻得出來、但攤不進行數 → 回傳 {@code null}<b>而且</b>旗標立起來。</li>
 *   <li>同一句給夠寬的框 → 換得掉，旗標<b>不能</b>立起來。</li>
 *   <li><b>查不到譯文</b>的那一句 → 旗標也不能立起來。這一條最重要：
 *       旗標一立，小框就會畫；沒有譯文卻讓小框接手，畫面上會冒出一塊空的、
 *       或者上一句留下來的東西。</li>
 * </ul>
 */
public final class DialogueTooLongTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT));
        DialogueRewriter.widthForTest = DialogueTooLongTest::realWidth;

        // 語料裡真的有的一句，連譯文一起拿出來——不寫死措辭，術語統一的 PR
        // 才不會把這支弄紅（見 tests-dont-hardcode-wording）。
        String src = "Then leave, and forget the horrors you have witnessed here today...";
        String dst = store.lookup(src);
        check("挑來測的那一句翻得出來（實際 " + dst + "）", dst != null && !dst.isBlank());
        if (dst == null || dst.isBlank()) {
            done();
            return;
        }

        // 一行、而且窄到連一個字都放不下 → 一定攤不進去
        String narrow = DialogueRewriter.line(src, store, 1, null, 8);
        check("塞不下時不換（回傳 null）", narrow == null);
        check("★ 塞不下時旗標要立起來，讓小框接手",
              DialogueRewriter.bodyTooLong());

        // 同一句給足夠的行數與寬度 → 換得掉，旗標要是關的
        String roomy = DialogueRewriter.line(src, store, 5, null, 232);
        check("夠寬就換得掉（實際 " + roomy + "）", roomy != null);
        check("★ 換得掉的時候旗標不可以立著（不然小框會重複畫一次）",
              !DialogueRewriter.bodyTooLong());

        // ★ 查不到譯文的那一句：旗標絕不能立起來
        String unknown = "Qwertyuiop asdfghjkl zxcvbnm, this is not in the corpus.";
        String miss = DialogueRewriter.line(unknown, store, 1, null, 8);
        check("查不到的原樣不動（回傳 null）", miss == null);
        check("★ 查不到譯文時旗標不可以立起來",
              !DialogueRewriter.bodyTooLong());

        done();
    }

    private static void done() {
        System.out.println(failures == 0 ? "\n對話塞不下改走小框：全部通過"
                                         : "\n對話塞不下改走小框：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 中日韓一個字 10px，其餘照 Wynncraft 的對話字型算 6px。 */
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
