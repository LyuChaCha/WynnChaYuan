package com.wynnchayuan.client;

import com.wynnchayuan.client.ui.Styled;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

record GameLine(FormattedCharSequence text, float width, String plain) implements Styled {

    static GameLine of(FormattedCharSequence text, Font font) {
        StringBuilder out = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            out.appendCodePoint(codePoint);
            return true;
        });
        return new GameLine(text, font.width(text), out.toString());
    }
}
