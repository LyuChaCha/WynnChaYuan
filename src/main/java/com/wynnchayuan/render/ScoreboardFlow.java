package com.wynnchayuan.render;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TextSplit;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScoreboardFlow {

    private ScoreboardFlow() {}

    static final int MAX_RUN = 6;

    public record Plan(List<Component> panel, Map<String, StyledText> rows, boolean any,
                       int missing) {}

    public static Plan plan(List<StyledText> content, TranslationStore store) {
        return plan(content, store, Set.of());
    }

    public static Plan plan(List<StyledText> content, TranslationStore store,
                            Set<String> kept) {
        List<StyledText> live = new ArrayList<>();
        if (content != null) {
            for (StyledText row : content) {
                if (row != null && !GlyphSplitter.isGlyphOnly(row)
                        && !GlyphSplitter.toTemplate(row).isBlank()) {
                    live.add(row);
                }
            }
        }
        List<Component> panel = new ArrayList<>();
        Map<String, StyledText> rows = new LinkedHashMap<>();
        boolean any = false;
        int missing = 0;
        int i = 0;
        while (i < live.size()) {
            if (kept.contains(key(live.get(i)))) {
                panel.add(LineTranslator.untranslated(live.get(i)));
                i++;
                continue;
            }
            int stop = Math.min(live.size(), i + MAX_RUN);
            for (int k = i + 1; k < stop; k++) {
                if (kept.contains(key(live.get(k)))) {
                    stop = k;
                    break;
                }
            }
            int end = -1;
            Component whole = null;
            for (int j = stop - 1; j > i; j--) {
                whole = sentence(live.subList(i, j + 1), store);
                if (whole != null) {
                    end = j;
                    break;
                }
            }
            if (whole != null) {
                int count = end - i + 1;
                List<Component> parts = TextSplit.lines(whole);
                if (parts.size() == count) {
                    panel.addAll(parts);
                } else {
                    whole = oneLine(whole);
                    panel.add(whole);
                    parts = split(whole, count);
                }
                for (int k = 0; k <= end - i; k++) {
                    rows.put(key(live.get(i + k)), styled(parts.get(k)));
                }
                any = true;
                i = end + 1;
                continue;
            }
            StyledText row = live.get(i);
            Component one = LineTranslator.translate(row, store);
            if (one != null) {
                one = oneLine(one);
                rows.put(key(row), styled(one));
                any = true;
            } else {
                missing++;
            }
            panel.add(one != null ? one : LineTranslator.untranslated(row));
            i++;
        }
        return new Plan(panel, rows, any, missing);
    }

    static final String PARTY_PART = "PartyScoreboardPart";

    private static final Pattern PARTY_HEADER =
            Pattern.compile("^\\s*Party:\\s*\\[Lv\\. ?\\d+]\\s*$");

    private static final Pattern NAME_TOKEN = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private static final Pattern BRACKETS = Pattern.compile("\\[[^\\]]*]");

    public static Set<String> keptRows(String part, StyledText header,
                                       List<StyledText> content, Collection<String> names) {
        if (content == null || content.isEmpty()) {
            return Set.of();
        }
        Set<String> kept = new HashSet<>();
        if (isParty(part, header)) {
            for (StyledText row : content) {
                kept.add(key(row));
            }
            return kept;
        }
        Set<String> known = new HashSet<>();
        if (names != null) {
            for (String name : names) {
                if (name != null && NAME_TOKEN.matcher(name).matches()) {
                    known.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (known.isEmpty()) {
            return Set.of();
        }
        for (StyledText row : content) {
            String plain = key(row);
            for (String word : words(plain)) {
                if (known.contains(word.toLowerCase(Locale.ROOT))) {
                    kept.add(plain);
                    break;
                }
            }
        }
        return kept;
    }

    public static boolean isParty(String part, StyledText header) {
        if (part != null && part.contains(PARTY_PART)) {
            return true;
        }
        return header != null && PARTY_HEADER.matcher(key(header)).matches();
    }

    public static String partyName(StyledText row) {
        List<String> found = words(BRACKETS.matcher(key(row)).replaceAll(" "));
        return found.isEmpty() ? null : found.get(found.size() - 1);
    }

    private static List<String> words(String plain) {
        List<String> out = new ArrayList<>();
        Matcher m = NAME_TOKEN.matcher(plain);
        while (m.find()) {
            boolean whole = (m.start() == 0 || !isNameChar(plain.charAt(m.start() - 1)))
                    && (m.end() == plain.length() || !isNameChar(plain.charAt(m.end())));
            if (whole) {
                out.add(m.group());
            }
        }
        return out;
    }

    private static boolean isNameChar(char c) {
        return c == '_' || (c < 128 && Character.isLetterOrDigit(c));
    }

    public static String key(StyledText row) {
        return row == null ? "" : row.getStringWithoutFormatting();
    }

    private static StyledText styled(Component part) {
        return part == null || part.getString().isEmpty()
                ? StyledText.EMPTY : StyledText.fromComponent(part);
    }

    static Component sentence(List<StyledText> run, TranslationStore store) {
        StyledText joined = StyledText.join(" ", run);
        String template = GlyphSplitter.toTemplate(joined);
        if (template.isBlank() || store.lookup(template) == null) {
            return null;
        }
        return LineTranslator.translate(joined, store);
    }

    public static Component oneLine(Component text) {
        return TextSplit.oneLine(text);
    }

    static List<Component> split(Component whole, int rows) {
        return TextSplit.split(whole, rows);
    }
}
