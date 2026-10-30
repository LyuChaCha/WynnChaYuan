package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

// Wynncraft shader 依字型圖集 (0,0) 的標記像素搬頂點
public final class PairedFont {

    private PairedFont() {
    }

    public static final String MARKER = String.valueOf((char) 1);

    public static void warm(FontDescription ours) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.font != null) {
                mc.font.width(net.minecraft.network.chat.Component.literal(MARKER)
                        .setStyle(Style.EMPTY.withFont(ours)));
            }
        } catch (Throwable t) {
        }
    }

    private static final java.util.regex.Pattern SELECTOR_SLOT =
            java.util.regex.Pattern.compile(
                    "hud/selector/default/center_left/([0-2])/"
                            + "(display_name|description)\\b");

    private static final java.util.regex.Pattern BOTTOM_SLOT =
            java.util.regex.Pattern.compile(
                    "hud/(selector|wardrobe|emote)/default/bottom_middle\\b");

    public static String slot(String font) {
        if (font == null) {
            return null;
        }
        if (BOTTOM_SLOT.matcher(font).find()) {
            return "selector_bottom";
        }
        if (font.contains("hud/wardrobe/default/top_middle")) {
            return "wardrobe_top";
        }
        java.util.regex.Matcher m = SELECTOR_SLOT.matcher(font);
        if (!m.find()) {
            return null;
        }
        return ("display_name".equals(m.group(2)) ? "selector_name_"
                                                  : "selector_desc_") + m.group(1);
    }

    private static final java.util.Map<String, Boolean> FONTS =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean shipped(String lang, String name) {
        return FONTS.computeIfAbsent(lang + "/" + name, key -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return false;
            }
            Identifier id = Identifier.fromNamespaceAndPath(
                    WynnChaYuan.MOD_ID, "font/actionbar/" + key + ".json");
            return mc.getResourceManager().getResource(id).isPresent();
        });
    }

    public static FontDescription forStyle(Style style) {
        if (style == null) {
            return null;
        }
        String name = slot(nameOf(style));
        if (name == null) {
            return null;
        }
        String lang = WynnChaYuan.language();
        if (!shipped(lang, name)) {
            return null;
        }
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath(
                WynnChaYuan.MOD_ID, "actionbar/" + lang + "/" + name));
    }

    // /class 每格退回量照英文寬度烘好，hud/selector/default/ 整個不翻
    public static boolean absolutelyPositioned(Style style) {
        return isSelector(style)
                && !nameOf(style).contains("hud/selector/default/bottom_middle");
    }

    public static boolean isSelector(Style style) {
        return nameOf(style).contains("hud/selector/default/");
    }

    public static boolean isHudScreen(Style style) {
        return HUD_SCREEN.matcher(nameOf(style)).find();
    }

    private static final java.util.regex.Pattern HUD_SCREEN =
            java.util.regex.Pattern.compile("hud/(selector|wardrobe|emote)/default/");

    public static boolean isOurs(Style style) {
        return nameOf(style).contains(WynnChaYuan.MOD_ID + ":actionbar/");
    }

    public static String nameOf(Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }
}
