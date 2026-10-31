package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// 名單照 Wynntils 4.2.8 的 MissionType 與 TrialType
public final class LootrunNames {

    private LootrunNames() {}

    static final Set<String> NAMES = Set.of(
            "Cleansing Greed", "Materialism", "Hoarder", "Jester's Trick", "Interest Scheme",
            "Orphion's Grace", "Opal Offering", "Gourmand", "Route Indigo", "Ostinato",
            "Porphyrophobia", "Radiant Hunter", "Equilibrium", "Inner Peace", "Optimism",
            "High Spirits", "Beleza Pura", "Backup Beat", "Requiem", "Stasis", "Chronokinesis",
            "Thrill Seeker", "Knife Edge", "King's Court", "High Roller", "Redemption",
            "Sacrificial Ritual", "Complete Chaos",
            "Cleansing Ritual",
            "Balancing Act",
            "Adrenaline Junkie", "Dying Light", "Monochromokopia", "Chronotrigger", "All In",
            "Gambling Beast", "Hubris", "Lights Out", "Side Hustle", "Treasury Bill",
            "Ultimate Sacrifice", "Warmth Devourer", "Imperitia");

    public static Set<String> names() {
        return NAMES;
    }

    public static boolean isName(String text) {
        return text != null && NAMES.contains(text.strip());
    }

    private static final String ICON = "{#}";
    private static final char NEWLINE = (char) 10;
    private static final String COLOUR_END = "{/}";

    public static String apply(String template, String hit, ModConfig.ItemNames mode) {
        if (template == null || hit == null || mode == null
                || mode == ModConfig.ItemNames.ON || !mentionsName(template)) {
            return hit;
        }
        List<String> src = split(template);
        List<String> dst = split(hit);
        if (src.size() != dst.size()) {
            return hit;
        }
        StringBuilder out = new StringBuilder(hit.length() + 24);
        boolean changed = false;
        for (int i = 0; i < src.size(); i++) {
            String s = src.get(i);
            String d = dst.get(i);
            if (i % 2 == 1) {
                if (!s.equals(d)) {
                    return hit;
                }
                out.append(d);
                continue;
            }
            String name = nameOf(s);
            String fixed = name == null ? null : rewrite(d, name, mode);
            if (fixed != null) {
                out.append(fixed);
                changed = true;
            } else {
                out.append(d);
            }
        }
        return changed ? out.toString() : hit;
    }

    static List<int[]> spans(String text) {
        List<int[]> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        for (String name : NAMES) {
            int at = text.indexOf(name);
            while (at >= 0) {
                int end = at + name.length();
                boolean left = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
                boolean right = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
                if (left && right) {
                    out.add(new int[] {at, end});
                }
                at = text.indexOf(name, at + 1);
            }
        }
        return out;
    }

    private static boolean mentionsName(String template) {
        for (String name : NAMES) {
            if (template.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static String nameOf(String cell) {
        String core = cell.strip();
        if (core.endsWith(":")) {
            core = core.substring(0, core.length() - 1).strip();
        }
        return NAMES.contains(core) ? core : null;
    }

    private static String rewrite(String cell, String name, ModConfig.ItemNames mode) {
        String core = cell.strip();
        if (core.isEmpty() || core.contains("(" + name + ")")) {
            return null;
        }
        int lead = cell.indexOf(core);
        String before = cell.substring(0, lead);
        String after = cell.substring(lead + core.length());
        String colon = "";
        if (core.endsWith(":") || core.endsWith("：")) {
            colon = core.substring(core.length() - 1);
            core = core.substring(0, core.length() - 1).stripTrailing();
        }
        String close = "";
        if (core.endsWith(COLOUR_END)) {
            close = COLOUR_END;
            core = core.substring(0, core.length() - COLOUR_END.length());
        }
        if (core.isEmpty() || core.equals(name)) {
            return null;
        }
        String shown;
        if (mode == ModConfig.ItemNames.OFF) {
            int text = 0;
            while (core.startsWith("{c", text)) {
                int end = core.indexOf('}', text);
                if (end < 0) {
                    break;
                }
                text = end + 1;
            }
            shown = core.substring(0, text) + name;
        } else {
            shown = core + " (" + name + ")";
        }
        return before + shown + close + colon + after;
    }

    static List<String> split(String text) {
        List<String> out = new ArrayList<>();
        int from = 0;
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == NEWLINE) {
                out.add(text.substring(from, i));
                out.add(String.valueOf(NEWLINE));
                from = ++i;
            } else if (text.startsWith(ICON, i)) {
                out.add(text.substring(from, i));
                out.add(ICON);
                i += ICON.length();
                from = i;
            } else {
                i++;
            }
        }
        out.add(text.substring(from));
        return out;
    }
}
