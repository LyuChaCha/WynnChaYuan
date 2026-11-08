package com.wynnchayuan.render;

import com.wynnchayuan.capture.LineParts;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ShaderColours {

    private ShaderColours() {}

    private static final int MOVEMENT = 235;

    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    static boolean marker(TextColor colour) {
        if (colour == null) {
            return false;
        }
        int rgb = colour.getValue();
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        boolean movement = g == MOVEMENT && b % 4 == 0 && b <= 72;
        boolean effect = r < 4 && g >= 240 && g < 244 && b < 40;
        return movement || effect;
    }

    static boolean same(TextColor a, TextColor b) {
        if (Objects.equals(a, b)) {
            return true;
        }
        return marker(a) && marker(b) && kind(a) == kind(b);
    }

    private static int kind(TextColor colour) {
        int rgb = colour.getValue();
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        return g == MOVEMENT ? MOVEMENT << 8 | b : 0xF000 | b / 4;
    }

    static TextColor resting(TextColor colour) {
        if (!marker(colour)) {
            return colour;
        }
        int rgb = colour.getValue();
        if ((rgb >> 8 & 0xFF) == MOVEMENT) {
            return WHITE;
        }
        return switch ((rgb & 0xFF) / 4) {
            case 1 -> TextColor.fromRgb(0xF56217);
            case 2 -> TextColor.fromRgb(0x5AF082);
            case 3 -> TextColor.fromRgb(0xC83232);
            case 4 -> TextColor.fromRgb(0x8A0303);
            case 5 -> TextColor.fromRgb(0xA0C84B);
            case 6 -> TextColor.fromRgb(0x4D4D4D);
            case 7 -> TextColor.fromRgb(0x55FFFF);
            case 8, 9 -> TextColor.fromRgb(0xC6C6C6);
            default -> WHITE;
        };
    }

    static List<LineParts.Piece> settle(String text, Style worn, String rowFont) {
        TextColor colour = worn.getColor();
        if (!marker(colour) || DialogueFonts.drawable(text, rowFont)) {
            return List.of(new LineParts.Piece(text, worn));
        }
        Style calm = worn.withColor(resting(colour));
        List<LineParts.Piece> out = new ArrayList<>();
        int from = 0;
        boolean bitmap = false;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            boolean drawn = DialogueFonts.drawable(new String(Character.toChars(cp)), rowFont);
            if (i > from && drawn != bitmap) {
                out.add(new LineParts.Piece(text.substring(from, i), bitmap ? worn : calm));
                from = i;
            }
            bitmap = drawn;
            i += Character.charCount(cp);
        }
        if (from < text.length()) {
            out.add(new LineParts.Piece(text.substring(from), bitmap ? worn : calm));
        }
        return out;
    }
}
