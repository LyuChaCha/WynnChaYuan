package com.wynnchayuan.render;

/** 讓別的套件的測試問得到 {@link DialogueRewriter} 的折行結果。 */
public final class DialogueRewriterProbe {

    private DialogueRewriterProbe() {}

    public static int rowsNeeded(String text, int rows) {
        return DialogueRewriter.rowsNeeded(text, rows);
    }

    public static boolean fits(String text, int rows) {
        return rows > 0 && DialogueRewriter.wrap(text, rows) != null;
    }
}
