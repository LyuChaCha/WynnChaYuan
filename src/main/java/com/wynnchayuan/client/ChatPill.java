package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class ChatPill {

    private ChatPill() {}

    private static final String LABEL = "WYNNCHAYUAN";

    private static final FontDescription FONT =
            new FontDescription.Resource(Identifier.withDefaultNamespace("banner/pill"));

    private static final int LEFT_EDGE = 0xE060;
    private static final int RIGHT_EDGE = 0xE062;
    private static final int BACK_A = 0xE030;
    private static final int FRONT_A = 0xE000;

    private static final int EDGE_WIDTH = 2;
    private static final int LETTER_WIDTH = 6;
    private static final int GAP = 6;

    private static final int INNER = LABEL.length() * LETTER_WIDTH;

    static final int WIDTH = INNER + EDGE_WIDTH + GAP;

    private static final int[] STOPS = {0x3A9F4C, 0x5FC063, 0xA2DF86};

    private static final int INK = 0x0C1A10;

    private static final Component PILL = pill();

    private static final Component TEXT = text();

    public static Component prefixed(Font font, Component line) {
        return prefixed(font.width(PILL), line);
    }

    static Component prefixed(int measured, Component line) {
        return Component.empty().append(prefix(measured)).append(line);
    }

    // 資源包沒載入時整組字型都是缺字框，寬度對不上
    static Component prefix(int measured) {
        return measured == WIDTH ? PILL : TEXT;
    }

    private static Component pill() {
        int count = LABEL.length() + 2;
        String step = SpaceOffset.encode(-1);
        MutableComponent out = Component.empty();
        out.append(back(glyph(LEFT_EDGE) + step, 0, count));
        StringBuilder front = new StringBuilder();
        for (int i = 0; i < LABEL.length(); i++) {
            int letter = LABEL.charAt(i) - 'A';
            out.append(back(glyph(BACK_A + letter) + step, i + 1, count));
            front.append(glyph(FRONT_A + letter));
        }
        out.append(back(glyph(RIGHT_EDGE) + SpaceOffset.encode(-INNER - EDGE_WIDTH),
                count - 1, count));
        Style ink = Style.EMPTY.withFont(FONT).withColor(INK).withoutShadow();
        out.append(Component.literal(front.toString()).setStyle(ink));
        out.append(Component.literal(SpaceOffset.encode(GAP)).setStyle(ink));
        return out;
    }

    private static Component back(String text, int index, int count) {
        return Component.literal(text)
                .setStyle(Style.EMPTY.withFont(FONT).withColor(colour(index, count)));
    }

    private static String glyph(int codePoint) {
        return String.valueOf((char) codePoint);
    }

    private static Component text() {
        String name = WynnChaYuan.MOD_NAME;
        MutableComponent out = Component.empty();
        out.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY));
        for (int i = 0; i < name.length(); i++) {
            out.append(Component.literal(name.substring(i, i + 1))
                    .setStyle(Style.EMPTY.withColor(colour(i, name.length()))));
        }
        out.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    static int colour(int index, int count) {
        double at = (double) index / (count - 1) * (STOPS.length - 1);
        int stop = Math.min((int) at, STOPS.length - 2);
        double part = at - stop;
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int from = STOPS[stop] >> shift & 0xFF;
            int to = STOPS[stop + 1] >> shift & 0xFF;
            out |= (int) Math.round(from + (to - from) * part) << shift;
        }
        return out;
    }
}
