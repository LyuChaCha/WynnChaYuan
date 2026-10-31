package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DialogueFonts {

    private DialogueFonts() {}

    // hud/dialogue/text/ 也會命中 control 與 nameplate
    static final String BODY = "/body_";

    static final String NAMEPLATE = "hud/dialogue/text/nameplate";

    static final String CONTROL = "hud/dialogue/text/control";

    static final String CHOICE = "/choice_";

    private static boolean hasDialogueFonts(String lang) {
        return fontShipped(lang, "body_0");
    }

    private static final Map<String, int[]> COVERAGE = new HashMap<>();

    private static final Map<String, Boolean> FONTS = new HashMap<>();

    static String unpaintable(String text, Style style) {
        String font = fontOf(style);
        boolean ours = font.startsWith(WynnChaYuan.MOD_ID + ":");
        if (!ours && !font.startsWith("minecraft:hud/dialogue/text/")) {
            return null;
        }
        String lang = WynnChaYuan.language();
        StringBuilder bad = new StringBuilder();
        text.codePoints().forEach(cp -> {
            String one = new String(Character.toChars(cp));
            if (!(ours ? covered(one, lang) : drawable(one, font))) {
                bad.append(String.format("U+%04X ", cp));
            }
        });
        return bad.length() == 0 ? null
                : bad.toString().strip() + "  字型=" + font + "  文字=" + text;
    }

    static boolean drawable(String text) {
        return text.codePoints().allMatch(cp ->
                plain(cp) || "ÀÁÂÃÄÅÆÈÉÊËÌÍÎÏàáâãäåæèéêëìíîï".indexOf(cp) >= 0);
    }

    static boolean drawable(String text, String font) {
        if (font != null && font.contains(CONTROL)) {
            return text.codePoints().allMatch(DialogueFonts::plain);
        }
        return drawable(text);
    }

    private static boolean plain(int cp) {
        return (cp >= 0x20 && cp < 0x7F) || cp == 0x2014;
    }

    static boolean readable(String text) {
        return text.codePoints().anyMatch(cp ->
                cp >= 0x20 && cp < 0x2E80 && !Character.isWhitespace(cp));
    }

    static boolean renderable(String text) {
        return drawable(text) || covered(text, WynnChaYuan.language());
    }

    static boolean covered(String text, String lang) {
        int[] ranges = coverage(lang);
        if (ranges.length == 0) {
            return true;
        }
        return text.codePoints().allMatch(cp -> {
            if (cp < 0x80) {
                return true;
            }
            for (int i = 0; i < ranges.length; i += 2) {
                if (cp >= ranges[i] && cp <= ranges[i + 1]) {
                    return true;
                }
            }
            return false;
        });
    }

    private static int[] coverage(String lang) {
        int[] known = COVERAGE.get(lang);
        if (known != null) {
            return known;
        }
        List<Integer> flat = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        try {
            var id = Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID,
                    "font/dialogue/" + lang + "/coverage.txt");
            var found = mc.getResourceManager().getResource(id);
            if (found.isPresent()) {
                try (var in = found.get().openAsReader()) {
                    for (String line : in.lines().toList()) {
                        if (line.isBlank() || line.startsWith("#")) {
                            continue;
                        }
                        int dash = line.indexOf('-');
                        flat.add(Integer.parseInt(line.substring(0, dash), 16));
                        flat.add(Integer.parseInt(line.substring(dash + 1), 16));
                    }
                }
            }
        } catch (Exception e) {
            ErrorDebug.note("DialogueFonts#coverage", null, e);
        }
        int[] out = new int[flat.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = flat.get(i);
        }
        COVERAGE.put(lang, out);
        return out;
    }

    // 缺一份字型時 FontManager 用 AllMissingGlyphProvider，整列方框
    static boolean fontMissing(String font) {
        String lang = WynnChaYuan.language();
        if (!hasDialogueFonts(lang)) {
            return false;
        }
        String name = pairedName(font);
        return name != null && !fontShipped(lang, name);
    }

    private static boolean fontShipped(String lang, String name) {
        return FONTS.computeIfAbsent(lang + "/" + name, key -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return true;
            }
            var id = Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID,
                    "font/dialogue/" + key + ".json");
            return mc.getResourceManager().getResource(id).isPresent();
        });
    }

    static String pairedName(String font) {
        String name = null;
        if (font.contains(BODY)) {
            int at = font.lastIndexOf(BODY);
            name = "body_" + font.substring(at + BODY.length());
        } else if (font.contains(CHOICE)) {
            int at = font.lastIndexOf(CHOICE);
            name = "choice_" + font.substring(at + CHOICE.length());
        } else if (font.contains(CONTROL)) {
            name = "control";
        } else if (font.contains(NAMEPLATE)) {
            name = "nameplate";
        }
        if (name == null) {
            return null;
        }
        StringBuilder clean = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                clean.append(Character.toLowerCase(c));
            } else {
                break;
            }
        }
        return clean.isEmpty() ? null : clean.toString();
    }

    static FontDescription paired(String font) {
        String clean = pairedName(font);
        if (clean == null) {
            return null;
        }
        String lang = WynnChaYuan.language();
        if (!hasDialogueFonts(lang)) {
            return null;
        }
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath(
                WynnChaYuan.MOD_ID, "dialogue/" + lang + "/" + clean));
    }

    static String fontOf(Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }
}
