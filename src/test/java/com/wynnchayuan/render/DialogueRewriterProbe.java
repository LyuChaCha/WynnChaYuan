package com.wynnchayuan.render;

public final class DialogueRewriterProbe {

    private DialogueRewriterProbe() {}

    public static int rowsNeeded(String text, int rows) {
        return DialogueRewriter.rowsNeeded(text, rows);
    }

    public static boolean fits(String text, int rows) {
        return rows > 0 && DialogueRewriter.wrap(text, rows) != null;
    }
}
