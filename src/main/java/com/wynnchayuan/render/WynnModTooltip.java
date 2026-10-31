package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.listener.RenderListener;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.SpaceOffset;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WynnModTooltip {

    private WynnModTooltip() {}

    private static volatile Boolean installed;

    public static boolean installed() {
        Boolean known = installed;
        if (known == null) {
            installed = known = FabricLoader.getInstance().isModLoaded("wynnmod");
        }
        return known;
    }

    public static List<Component> joinSameStyle(List<Component> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (Component line : lines) {
            out.add(line == null ? null : joinSameStyle(line));
        }
        return out;
    }

    private static Component joinSameStyle(Component line) {
        List<Style> styles = new ArrayList<>();
        List<StringBuilder> texts = new ArrayList<>();
        boolean[] joined = {false};
        line.visit((style, text) -> {
            if (text.isEmpty()) {
                return Optional.empty();
            }
            int last = styles.size() - 1;
            if (last >= 0 && plain(text) && plain(texts.get(last))
                    && (sameLook(styles.get(last), style)
                        || cutMark(text))) {
                texts.get(last).append(text);
                joined[0] = true;
            } else {
                styles.add(style);
                texts.add(new StringBuilder(text));
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (!joined[0]) {
            return line;
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < styles.size(); i++) {
            out.append(Component.literal(texts.get(i).toString()).withStyle(styles.get(i)));
        }
        return out;
    }

    // Style#equals 把沒設與設成預設值視為不同
    static boolean sameLook(Style a, Style b) {
        return a.getFont().equals(b.getFont())
                && colourOf(a) == colourOf(b)
                && a.isBold() == b.isBold()
                && a.isItalic() == b.isItalic()
                && a.isUnderlined() == b.isUnderlined()
                && a.isStrikethrough() == b.isStrikethrough()
                && a.isObfuscated() == b.isObfuscated()
                && Objects.equals(a.getShadowColor(), b.getShadowColor())
                && Objects.equals(a.getClickEvent(), b.getClickEvent())
                && Objects.equals(a.getHoverEvent(), b.getHoverEvent())
                && Objects.equals(a.getInsertion(), b.getInsertion());
    }

    private static boolean cutMark(String text) {
        return "..".equals(text);
    }

    private static int colourOf(Style style) {
        return style.getColor() == null ? 0xFFFFFF : style.getColor().getValue();
    }

    private static boolean plain(CharSequence text) {
        return text.codePoints().noneMatch(cp ->
                (cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0xF0000
                        || SpaceOffset.isOffset(cp));
    }

    public static List<Component> lines(List<Component> text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            ModConfig config = WynnChaYuan.config();
            if (config == null || !config.wynnmodTooltip()
                    || config.tooltipMode() != ModConfig.TooltipMode.REPLACE) {
                return text;
            }
            List<Component> out = RenderListener.translateForeign(joinSameStyle(text));
            if (out.isEmpty()) {
                TooltipDebug.dumpForeign("wynnmod", text, null);
                return text;
            }
            TooltipDebug.dumpForeign("wynnmod", text, out);
            WynnChaYuan.store().noteEvent("tooltip.wynnmod");
            return out;
        } catch (Throwable t) {
            try {
                WynnChaYuan.store().noteEvent("render.wynnmodError");
                ErrorDebug.note("tooltip.wynnmod", text.get(0).getString(), t);
            } catch (Throwable failure) {
                ErrorDebug.note("WynnModTooltip#lines", null, failure);
            }
            return text;
        }
    }
}
