package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PageNames {

    private PageNames() {}

    private static final Pattern TITLE = Pattern.compile(
            "^(.+) \\[(Quest|Mini-Quest|Cave|Secret Discovery|World Discovery"
                    + "|Territorial Discovery|Boss Altar|Lootrun Camp|Dungeon|Raid|World Event|Time Trial)]$");

    public static String nameOfTitle(String text) {
        if (text == null || text.indexOf('\n') >= 0) {
            return null;
        }
        Matcher m = TITLE.matcher(text);
        return m.matches() ? m.group(1) : null;
    }

    public static List<Component> titles(List<Component> original, List<Component> translated,
                                         CollectorConfig.ItemNames mode) {
        if (original == null || original.isEmpty()
                || translated == null || translated.isEmpty()) {
            return translated;
        }
        Component first = original.get(0);
        String name = first == null ? null : nameOfTitle(first.getString());
        if (name == null) {
            return translated;
        }
        if (mode == null) {
            mode = CollectorConfig.ItemNames.ON;
        }
        Component changed = title(first, translated.get(0), name, mode);
        boolean below = mode == CollectorConfig.ItemNames.BOTH && changed != null
                && !titleName(changed.getString()).equals(name);
        if (changed == translated.get(0) && !below) {
            return translated;
        }
        List<Component> out = new ArrayList<>(translated);
        out.set(0, changed);
        if (below) {
            MutableComponent row = Component.empty();
            copy(row, first, 0, name.length());
            out.add(1, row);
        }
        return out;
    }

    private static String titleName(String shown) {
        int cut = shown.lastIndexOf(" [");
        return cut > 0 && shown.endsWith("]") ? shown.substring(0, cut) : shown;
    }

    static Component title(Component original, Component translated, String name,
                           CollectorConfig.ItemNames mode) {
        if (translated == null) {
            return null;
        }
        String shown = translated.getString();
        int cut = shown.lastIndexOf(" [");
        if (cut <= 0 || !shown.endsWith("]")) {
            return translated;
        }
        String local = shown.substring(0, cut);
        if (local.equals(name)) {
            return translated;
        }
        MutableComponent out = Component.empty();
        if (mode == CollectorConfig.ItemNames.OFF) {
            copy(out, original, 0, name.length());
        } else {
            net.minecraft.network.chat.TextColor colour = nameColour(original, name.length());
            int[] at = {0};
            boolean[] same = {true};
            translated.visit((style, part) -> {
                int end = Math.min(at[0] + part.length(), cut);
                if (end > at[0]) {
                    Style painted = colour == null ? style : style.withColor(colour);
                    same[0] &= painted.equals(style);
                    out.append(Component.literal(part.substring(0, end - at[0])).withStyle(painted));
                }
                at[0] += part.length();
                return Optional.empty();
            }, Style.EMPTY);
            if (same[0]) {
                return translated;
            }
        }
        copy(out, translated, cut, shown.length());
        return out;
    }

    private static net.minecraft.network.chat.TextColor nameColour(Component line, int length) {
        java.util.Map<net.minecraft.network.chat.TextColor, Integer> votes = new java.util.LinkedHashMap<>();
        int[] at = {0};
        line.visit((style, part) -> {
            for (int i = 0; i < part.length() && at[0] + i < length; i++) {
                char c = part.charAt(i);
                if (Character.isLetterOrDigit(c) && !(c >= 0xE000 && c <= 0xF8FF)
                        && style.getColor() != null) {
                    votes.merge(style.getColor(), 1, Integer::sum);
                }
            }
            at[0] += part.length();
            return Optional.empty();
        }, Style.EMPTY);
        net.minecraft.network.chat.TextColor best = null;
        int most = 0;
        for (java.util.Map.Entry<net.minecraft.network.chat.TextColor, Integer> e : votes.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private static void copy(MutableComponent out, Component line, int from, int to) {
        int[] at = {0};
        line.visit((style, part) -> {
            int end = at[0] + part.length();
            int a = Math.max(at[0], from);
            int b = Math.min(end, to);
            if (a < b) {
                out.append(Component.literal(part.substring(a - at[0], b - at[0])).withStyle(style));
            }
            at[0] = end;
            return Optional.empty();
        }, Style.EMPTY);
    }

    public static Component bare(Component original, Component translated,
                                 CollectorConfig.ItemNames mode) {
        if (translated == null || mode == null || mode == CollectorConfig.ItemNames.ON) {
            return translated;
        }
        if (mode == CollectorConfig.ItemNames.OFF) {
            return null;
        }
        String name = original.getString();
        String shown = translated.getString();
        if (shown.equals(name) || shown.endsWith("(" + name + ")")) {
            return translated;
        }
        return splice(translated, shown.length(), shown.length(), " (" + name + ")");
    }

    static Component splice(Component line, int from, int to, String text) {
        List<Style> styles = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        line.visit((style, part) -> {
            if (!part.isEmpty()) {
                styles.add(style);
                texts.add(part);
            }
            return Optional.empty();
        }, Style.EMPTY);

        int probe = from > 0 ? from - 1 : 0;
        Style inserted = styles.isEmpty() ? Style.EMPTY : styles.get(styles.size() - 1);
        int at = 0;
        for (int i = 0; i < texts.size(); i++) {
            if (probe < at + texts.get(i).length()) {
                inserted = styles.get(i);
                break;
            }
            at += texts.get(i).length();
        }

        MutableComponent out = Component.empty();
        at = 0;
        for (int i = 0; i < texts.size(); i++) {
            String part = texts.get(i);
            int end = at + part.length();
            int headEnd = Math.min(end, from);
            if (headEnd > at) {
                out.append(Component.literal(part.substring(0, headEnd - at))
                        .withStyle(styles.get(i)));
            }
            at = end;
        }
        out.append(Component.literal(text).withStyle(inserted));
        at = 0;
        for (int i = 0; i < texts.size(); i++) {
            String part = texts.get(i);
            int end = at + part.length();
            int tailStart = Math.max(at, to);
            if (tailStart < end) {
                out.append(Component.literal(part.substring(tailStart - at))
                        .withStyle(styles.get(i)));
            }
            at = end;
        }
        return out;
    }
}
