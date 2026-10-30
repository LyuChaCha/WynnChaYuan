package com.wynnchayuan.render;

import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.file.Path;

public final class DialogueTooLongTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT));
        DialogueLayout.widthForTest = DialogueTooLongTest::realWidth;

        String src = "Then leave, and forget the horrors you have witnessed here today...";
        String dst = store.lookup(src);
        check("挑來測的那一句翻得出來（實際 " + dst + "）", dst != null && !dst.isBlank());
        if (dst == null || dst.isBlank()) {
            done();
            return;
        }

        String narrow = DialogueTyping.line(src, store, 1, null, 8);
        check("塞不下時不換（回傳 null）", narrow == null);
        check("★ 塞不下時旗標要立起來，讓小框接手",
              DialogueRewriter.bodyTooLong());

        String roomy = DialogueTyping.line(src, store, 5, null, 232);
        check("夠寬就換得掉（實際 " + roomy + "）", roomy != null);
        check("★ 換得掉的時候旗標不可以立著（不然小框會重複畫一次）",
              !DialogueRewriter.bodyTooLong());

        String unknown = "Qwertyuiop asdfghjkl zxcvbnm, this is not in the corpus.";
        String miss = DialogueTyping.line(unknown, store, 1, null, 8);
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
