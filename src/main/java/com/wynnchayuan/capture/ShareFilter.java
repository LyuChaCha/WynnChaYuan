package com.wynnchayuan.capture;

public final class ShareFilter {

    private ShareFilter() {}

    static final int MAX_LEN = 600;

    public static boolean shareable(CaptureStore.Captured c) {
        if (c == null || c.src == null || c.src.isBlank() || c.src.length() > MAX_LEN) {
            return false;
        }
        if (!GlyphSplitter.hasLetter(c.src)) {
            return false;
        }
        String ctx = c.ctx == null ? "" : c.ctx;
        boolean ok = ctx.startsWith("dialogue/") || ctx.startsWith("gui/")
                || ctx.startsWith("tooltip/") || ctx.startsWith("npc/")
                || ctx.startsWith("label/") || ctx.equals("chat/INFO");
        if (!ok) {
            return false;
        }
        if (PlayerDataFilter.carriesPlayerData(c.src)) {
            return false;
        }
        if ((ctx.startsWith("npc/") || ctx.startsWith("label/"))
                && PlayerDataFilter.looksPlayerNamed(c.src)) {
            return false;
        }
        if (ctx.startsWith("gui/title") && PlayerDataFilter.looksAccountNamed(c.src)) {
            return false;
        }
        return SelfNames.find(c.src) == null;
    }
}
