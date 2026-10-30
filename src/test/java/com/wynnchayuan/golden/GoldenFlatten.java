package com.wynnchayuan.golden;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.Optional;

final class GoldenFlatten {

    private GoldenFlatten() {}

    static String of(Component component) {
        if (component == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder();
        component.visit((style, text) -> {
            if (!out.isEmpty()) {
                out.append(" ¦ ");
            }
            out.append(font(style)).append('|')
               .append(colour(style)).append('|')
               .append(decorations(style)).append('|')
               .append(escape(text));
            return Optional.empty();
        }, Style.EMPTY);
        return out.isEmpty() ? "empty" : out.toString();
    }

    static String of(List<Component> lines) {
        if (lines == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder();
        for (Component line : lines) {
            out.append(out.isEmpty() ? "" : " ⏎ ").append(of(line));
        }
        return out.toString();
    }

    static String escape(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\\' -> out.append("\\\\");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String font(Style style) {
        FontDescription font = style.getFont();
        if (font == null || font.equals(FontDescription.DEFAULT)) {
            return "-";
        }
        return font instanceof FontDescription.Resource resource
                ? resource.id().toString() : String.valueOf(font);
    }

    private static String colour(Style style) {
        TextColor colour = style.getColor();
        String base = colour == null ? "-" : colour.serialize();
        Integer shadow = style.getShadowColor();
        return shadow == null ? base : base + "/sc" + Integer.toHexString(shadow);
    }

    private static String decorations(Style style) {
        StringBuilder out = new StringBuilder();
        if (style.isBold()) {
            out.append('b');
        }
        if (style.isItalic()) {
            out.append('i');
        }
        if (style.isUnderlined()) {
            out.append('u');
        }
        if (style.isStrikethrough()) {
            out.append('s');
        }
        if (style.isObfuscated()) {
            out.append('o');
        }
        if (style.getClickEvent() != null) {
            out.append("+click");
        }
        if (style.getHoverEvent() != null) {
            out.append("+hover");
        }
        return out.isEmpty() ? "-" : out.toString();
    }
}
