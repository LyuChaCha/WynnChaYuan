package com.wynnchayuan.render;

import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

import java.util.List;

final class TooltipShape {

    private TooltipShape() {}

    static boolean evenOut(List<String> plain, boolean[] hit) {
        boolean changed = false;
        int i = 0;
        while (i < hit.length) {
            int end = i + 1;
            while (end < hit.length && continues(plain.get(end - 1), plain.get(end))) {
                end++;
            }
            if (end - i > 1) {
                boolean some = false;
                boolean all = true;
                for (int k = i; k < end; k++) {
                    if (isAddress(plain.get(k)) || nothingToTranslate(plain.get(k))) {
                        continue;
                    }
                    some |= hit[k];
                    all &= hit[k];
                }
                if (some && !all) {
                    for (int k = i; k < end; k++) {
                        hit[k] = false;
                    }
                    changed = true;
                }
            }
            i = end;
        }
        return changed;
    }

    static boolean isAddress(String line) {
        return line != null && ADDRESS.matcher(line.strip()).matches();
    }

    private static final java.util.regex.Pattern ADDRESS = java.util.regex.Pattern.compile(
            "(?:https?://)?[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+(?:/\\S*)?");

    static boolean nothingToTranslate(String line) {
        return line != null
                && !com.wynnchayuan.capture.GlyphSplitter.hasLetter(line);
    }

    static String visibleText(StyledText line) {
        StringBuilder out = new StringBuilder();
        for (com.wynntils.core.text.StyledTextPart part : line) {
            com.wynntils.core.text.PartStyle ps = part.getPartStyle();
            if (ps != null && isInvisibleFont(ps.getStyle())) {
                continue;
            }
            out.append(part.getString(null, com.wynntils.core.text.type.StyleType.NONE));
        }
        return out.toString();
    }

    private static boolean isInvisibleFont(net.minecraft.network.chat.Style style) {
        if (style == null) {
            return false;
        }
        net.minecraft.network.chat.FontDescription font = style.getFont();
        return font instanceof net.minecraft.network.chat.FontDescription.Resource resource
                && resource.id() != null && "invisible".equals(resource.id().getPath());
    }

    private static boolean continues(String prev, String line) {
        if (prev.isEmpty() || line.isEmpty()) {
            return false;
        }
        char end = prev.charAt(prev.length() - 1);
        if (end == '.' || end == '!' || end == '?' || end == ':' || end == ';') {
            return false;
        }
        char start = line.charAt(0);
        return start >= 'a' && start <= 'z' || endsMidPhrase(prev) || prose(prev, line);
    }

    private static boolean prose(String prev, String line) {
        char end = prev.charAt(prev.length() - 1);
        return (Character.isLetter(end) || end == ',')
                && hasLowerWord(prev)
                && proseLine(prev) && proseLine(line);
    }

    private static boolean hasLowerWord(String line) {
        for (int i = 1; i < line.length(); i++) {
            if (line.charAt(i - 1) == ' ' && Character.isLowerCase(line.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean proseLine(String line) {
        if (!Character.isLetter(line.charAt(0))) {
            return false;
        }
        if (line.indexOf(' ') < 0 || line.indexOf(':') >= 0) {
            return false;
        }
        for (int i = 0; i < line.length(); i++) {
            if (Character.isDigit(line.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean endsMidPhrase(String prev) {
        int space = prev.lastIndexOf(' ');
        if (space <= 0) {
            return false;
        }
        return LINK_WORDS.contains(prev.substring(space + 1).toLowerCase(java.util.Locale.ROOT));
    }

    private static final java.util.Set<String> LINK_WORDS = java.util.Set.of(
            "a", "an", "the", "of", "to", "at", "for", "with", "from", "into",
            "by", "in", "on", "and", "or", "your");

    static boolean isAbilityNode(List<StyledText> styled) {
        return anyMarkers(styled, PANEL_MARKERS);
    }

    static boolean isLootrunCard(List<StyledText> styled) {
        return anyMarkers(styled, LOOTRUN_CARD_MARKERS);
    }

    private static final String[][] LOOTRUN_CARD_MARKERS = {
        {"Objective:", "Reward:"},
        {"Trial:", "Reward:"},
    };

    private static boolean anyMarkers(List<StyledText> styled, String[][] groups) {
        StringBuilder all = new StringBuilder();
        for (StyledText line : styled) {
            all.append(line.getStringWithoutFormatting()).append('\n');
        }
        String text = all.toString();
        for (String[] markers : groups) {
            boolean every = true;
            for (String marker : markers) {
                every &= text.contains(marker);
            }
            if (every) {
                return true;
            }
        }
        return false;
    }

    private static final String[][] PANEL_MARKERS = {
        {"Ability Points:"},
        {"Blocked by:"},
        {"Blocked by another ability"},
        {"Click to unlock this ability"},
        {"Objective:", "Reward:"},
        {"Trial:", "Reward:"},
        {"Dynamic Boon", "Click to choose!"},
        {"Static Boon", "Click to choose!"},
    };

    static boolean blockedGearName(StyledText line, TranslationStore store) {
        String bare = bareName(com.wynnchayuan.capture.LineParts.of(line).template());
        int at = 0;
        while (at < bare.length() && BULLETS.indexOf(bare.charAt(at)) >= 0) {
            at++;
        }
        return store.isBareGearName(bare.substring(at).strip())
                || guessList(line.getStringWithoutFormatting(), store);
    }

    static boolean guessList(String plain, TranslationStore store) {
        String text = com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(plain).strip();
        int at = 0;
        while (at < text.length() && (BULLETS.indexOf(text.charAt(at)) >= 0
                || Character.isWhitespace(text.charAt(at)))) {
            at++;
        }
        java.util.regex.Matcher row = GUESS_ROW.matcher(text.substring(at));
        if (!row.matches()) {
            return false;
        }
        for (String name : row.group(1).split(",")) {
            if (store.isBareGearName(name.strip())) {
                return true;
            }
        }
        return false;
    }

    private static final java.util.regex.Pattern GUESS_ROW = java.util.regex.Pattern.compile(
            "[^\\d:\\[\\]]{1,16} \\d+(?: \\[[^\\]]*\\])?: (.+)");

    private static final String BULLETS = "-–—•*";

    private static final java.util.regex.Pattern CRAFTED_BY =
            java.util.regex.Pattern.compile("(?:^|[^A-Za-z])Crafted by \\S");

    static boolean crafted(List<StyledText> lines) {
        for (StyledText line : lines) {
            if (line != null && CRAFTED_BY.matcher(line.getStringWithoutFormatting()).find()) {
                return true;
            }
        }
        return false;
    }

    static boolean itemShaped(List<StyledText> styled) {
        int n = styled.size();
        if (n == 0) {
            return false;
        }
        if (TranslationStore.nameRowInner(
                com.wynnchayuan.capture.LineParts.of(styled.get(0)).template()) != null) {
            return true;
        }
        for (int k = 1; k < Math.min(n, BANNER_ROWS + 1); k++) {
            String t = com.wynnchayuan.capture.LineParts.of(styled.get(k)).template();
            if (bannerRow(t)) {
                return true;
            }
        }
        for (StyledText line : styled) {
            if (line != null && ITEM_ROW.matcher(line.getStringWithoutFormatting()).find()) {
                return true;
            }
        }
        return false;
    }

    private static final int BANNER_ROWS = 4;

    static boolean bannerRow(String template) {
        String icon = com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER;
        String rest = template.replace(icon, "");
        if (!rest.isBlank()) {
            return false;
        }
        int icons = (template.length() - rest.length()) / icon.length();
        return icons >= 3;
    }

    private static final java.util.regex.Pattern ITEM_ROW = java.util.regex.Pattern.compile(
            "Combat Level|Combat Lv\\. Min|Class Type|Class Req|\\d DPS\\b"
                    + "|(?:^|[^A-Za-z])Crafted by \\S");

    static String bareName(String template) {
        String s = template
                .replace(com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER, "")
                .strip();
        while (s.endsWith("]")) {
            int open = s.lastIndexOf('[');
            if (open <= 0) {
                break;
            }
            s = s.substring(0, open).strip();
        }
        return s;
    }
}
