package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.ColumnAligner.Piece;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TemplateLookup {

    private TemplateLookup() {}

    static String withoutSpacerRows(String template, TranslationStore store) {
        if (template == null) {
            return null;
        }
        Matcher m = SPACER_ROWS.matcher(template);
        if (!m.matches()) {
            return null;
        }
        String body = m.group(1);
        String hit = store.labelLookup(body);
        if (hit == null || hit.isBlank()) {
            hit = lookup(body, store);
        }
        return hit == null || hit.isBlank() ? null : hit + m.group(2);
    }

    private static final Pattern SPACER_ROWS = Pattern.compile(
            "(?s)(.*\\S)((?:\n[ \t]*À[ \tÀ]*)+)");

    static String lookup(String template, TranslationStore store) {
        return lookupTrimmed(template, store, false);
    }

    static String lookup(String template, TranslationStore store, boolean percent) {
        String hit = lookupTrimmed(template, store, percent);
        if (hit == null) {
            hit = statRow(template, store, percent);
        }
        return hit != null ? hit : nameRow(template, store);
    }

    private static String nameRow(String template, TranslationStore store) {
        int start = 0;
        while (start < template.length()
                && Character.isWhitespace(template.charAt(start))) {
            start++;
        }
        int body = start;
        for (int next = bulletEnd(template, body); next > body;
                next = bulletEnd(template, body)) {
            body = next;
        }
        String rest = template.substring(body);
        String head = template.substring(0, body);

        int colon = labelEnd(rest);
        if (colon > 0) {
            String label = rest.substring(0, colon);
            String zhLabel = lookupTrimmed(label, store, false);
            int name = colon;
            while (name < rest.length() && rest.charAt(name) == ' ') {
                name++;
            }
            String tail = rest.substring(name);
            if (zhLabel != null && !zhLabel.isBlank() && !tail.isBlank()) {
                String zhName = TYPED_LABELS.contains(label.strip())
                        ? tail : lookupTrimmed(tail, store, false);
                if ((zhName == null || zhName.isBlank())
                        && NAME_LABELS.contains(label.strip())) {
                    zhName = tail;
                }
                if (zhName != null && !zhName.isBlank()) {
                    return head + zhLabel
                            + reattach(zhLabel, rest.substring(colon, name)) + zhName;
                }
            }
        }
        if (body == start) {
            return null;
        }
        String zh = lookupTrimmed(rest, store, false);
        return zh == null || zh.isBlank() ? null : head + zh;
    }

    private static final Set<String> NAME_LABELS = Set.of("Required Ability:", "Unlocked Ability:",
            "Cave:",
            "Name Contains:");

    private static final Set<String> TYPED_LABELS = Set.of("Name Contains:");

    private static int bulletEnd(String template, int at) {
        if (at >= template.length()) {
            return at;
        }
        int cp = template.codePointAt(at);
        boolean bullet = BULLETS.indexOf(cp) >= 0;
        if (!bullet && !isDecoration(cp)) {
            return at;
        }
        int next = at + Character.charCount(cp);
        int spaced = next;
        while (spaced < template.length() && template.charAt(spaced) == ' ') {
            spaced++;
        }
        if (bullet && spaced == next) {
            return at;
        }
        return spaced;
    }

    private static int labelEnd(String rest) {
        int at = rest.indexOf(':');
        return at > 0 && at + 1 < rest.length() && rest.charAt(at + 1) == ' '
                ? at + 1 : -1;
    }

    private static final String BULLETS = "-–—•*";

    private static String statRow(String template, TranslationStore store, boolean percent) {
        int from = valueTailStart(template);
        if (from <= 0) {
            return null;
        }
        String label = template.substring(0, from);
        if (!GlyphSplitter.hasLetter(label)) {
            return null;
        }
        String tail = template.substring(from);
        String whole = untruncated(label, tail, store);
        if (whole != null) {
            tail = tail.substring(tail.indexOf(TRUNCATED) + TRUNCATED.length());
            label = whole;
            if (tail.isBlank()) {
                String zh = lookupTrimmed(label, store, percent);
                return zh == null || zh.isBlank() ? null : zh + tail;
            }
        }
        if (store.itemNameOnly(label)) {
            if (colonFirst(tail)) {
                return itemAsLabel(label, tail, store);
            }
            if (!countFirst(tail)) {
                return null;
            }
        }
        if (!tail.isEmpty()) {
            String zh;
            try (LookupScope scope = LookupScope.barItemNames(
                    TranslationStore.itemNamesBarred() || !countFirst(tail))) {
                zh = statLabel(label, store, percent);
            }
            if (zh != null) {
                return zh + translateTail(tail, store);
            }
            if (colonFirst(tail)) {
                String own = itemAsLabel(label, tail, store);
                if (own != null) {
                    return own;
                }
            }
        }
        int head = valueHeadEnd(label);
        String lead = label.substring(0, head);
        if (lead.indexOf(GlyphSplitter.NUMBER_PLACEHOLDER) < 0) {
            return null;
        }
        String name = label.substring(head);
        if (tail.isEmpty() && !store.isUiLabel(name.strip())) {
            return null;
        }
        String zh = statLabel(name, store, percent);
        return zh == null ? null : translateTail(lead, store) + zh + translateTail(tail, store);
    }

    // WynnMod 標籤放不下時會砍尾並接上 ..
    private static final String TRUNCATED = "..";

    private static final int MIN_BARE_TRUNCATED = 10;

    private static String untruncated(String label, String tail, TranslationStore store) {
        int dots = 0;
        while (dots < tail.length() && Character.isWhitespace(tail.charAt(dots))) {
            dots++;
        }
        if (!tail.startsWith(TRUNCATED, dots)) {
            return null;
        }
        String after = tail.substring(dots + TRUNCATED.length());
        boolean bare = after.isBlank();
        if (!bare && after.indexOf(GlyphSplitter.NUMBER_PLACEHOLDER) < 0) {
            return null;
        }
        int start = 0;
        while (start < label.length()) {
            if (label.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER, start)) {
                start += GlyphSplitter.GLYPH_PLACEHOLDER.length();
            } else if (Character.isWhitespace(label.charAt(start))) {
                start++;
            } else {
                break;
            }
        }
        String head = label.substring(start) + tail.substring(0, dots);
        if (bare && head.strip().length() < MIN_BARE_TRUNCATED) {
            return null;
        }
        String whole = store.completeUiLabel(head);
        return whole == null ? null : label.substring(0, start) + whole;
    }

    private static boolean countFirst(String tail) {
        String t = tail.stripLeading();
        return !t.isEmpty() && (t.charAt(0) == '[' || t.charAt(0) == '(');
    }

    private static boolean colonFirst(String tail) {
        String t = tail.stripLeading();
        return !t.isEmpty() && ColumnAligner.isTrailingColon(t.charAt(0));
    }

    private static String itemAsLabel(String label, String tail, TranslationStore store) {
        String word = label.strip();
        String own = store.lookupExact(word + ":");
        if (own == null) {
            return null;
        }
        String lead = label.substring(0, label.indexOf(word));
        String t = tail.stripLeading();
        String rest = t.substring(1);
        return lead + own + translateTail(reattach(own, rest), store);
    }

    private static int valueHeadEnd(String label) {
        int at = 0;
        while (at < label.length()) {
            char c = label.charAt(at);
            if (c == '{') {
                int close = label.indexOf('}', at);
                if (close < 0) {
                    break;
                }
                at = close + 1;
            } else if (c != '}' && isValueChar(c)) {
                at++;
            } else {
                break;
            }
        }
        return at;
    }

    private static String statLabel(String label, TranslationStore store, boolean percent) {
        String zh = lookupTrimmed(label + (percent ? "%" : " Raw"), store, false);
        if (zh == null || zh.isBlank()) {
            zh = lookupTrimmed(label, store, percent);
        }
        return zh == null || zh.isBlank() ? null : zh;
    }

    private static int valueTailStart(String template) {
        int at = template.length();
        while (at > 0) {
            char last = template.charAt(at - 1);
            if (last == '}') {
                int open = template.lastIndexOf('{', at - 1);
                if (open < 0) {
                    break;
                }
                at = open;
            } else if (isValueChar(last)) {
                at--;
            } else {
                int word = tailWordStart(template, at);
                if (word < 0) {
                    break;
                }
                at = word;
            }
        }
        return at;
    }

    private static int tailWordStart(String template, int at) {
        int start = at;
        while (start > 0 && Character.isLetter(template.charAt(start - 1))) {
            start--;
        }
        return start < at && CONNECTOR_KEYS.containsKey(template.substring(start, at))
                ? start : -1;
    }

    private static String translateTail(String tail, TranslationStore store) {
        StringBuilder out = new StringBuilder(tail.length());
        int at = 0;
        while (at < tail.length()) {
            if (!Character.isLetter(tail.charAt(at))) {
                out.append(tail.charAt(at));
                at++;
                continue;
            }
            int end = at;
            while (end < tail.length() && Character.isLetter(tail.charAt(end))) {
                end++;
            }
            String word = tail.substring(at, end);
            String local = connector(word, store);
            out.append(local != null ? local : word);
            at = end;
        }
        return out.toString();
    }

    private static boolean isValueChar(char c) {
        return Character.isDigit(c) || Character.isWhitespace(c)
                || "+-±%/[](){}<>,.:~*★⇧⇩⇨❤✦✤✹✽✧'\"".indexOf(c) >= 0;
    }

    private static final Map<String, String> CONNECTOR_KEYS = Map.of(
            "to", "{~} to {~}", "tier", "{~} tier", "tiers", "{~} tier");

    private static final Pattern CONNECTOR_SLOT = Pattern.compile("\\{~\\d*}");

    static String connector(String word, TranslationStore store) {
        String key = CONNECTOR_KEYS.get(word);
        if (key == null || store == null) {
            return null;
        }
        String dst = store.lookup(key);
        if (dst == null || dst.isBlank()) {
            return null;
        }
        String local = CONNECTOR_SLOT.matcher(dst).replaceAll("").strip();
        return local.isEmpty() ? null : local;
    }

    static boolean isConnectorSegment(String body) {
        boolean word = false;
        int at = 0;
        while (at < body.length()) {
            char c = body.charAt(at);
            if (Character.isLetter(c)) {
                int end = at;
                while (end < body.length() && Character.isLetter(body.charAt(end))) {
                    end++;
                }
                if (!CONNECTOR_KEYS.containsKey(body.substring(at, end))) {
                    return false;
                }
                word = true;
                at = end;
            } else if (isValueChar(c)) {
                at++;
            } else {
                return false;
            }
        }
        return word;
    }

    static void translateConnectors(List<Piece> pieces, List<Integer> at,
                                    TranslationStore store) {
        for (int k = at.size() - 1; k >= 0; k--) {
            int index = at.get(k);
            Piece piece = pieces.get(index);
            String raw = piece.text();
            String tail = SpaceOffset.trailingOffsets(raw);
            String body = raw.substring(0, raw.length() - tail.length());
            String local = translateTail(body, store);
            if (local.equals(body)) {
                continue;
            }
            pieces.set(index, Piece.translated(DisplayStyles.literal(local, DisplayStyles.forDisplay(piece.style())),
                                               DisplayStyles.literal(body, piece.style())));
            if (!tail.isEmpty()) {
                pieces.add(index + 1, Piece.space(SpaceOffset.decode(tail),
                        SpaceOffset.styleFor(piece.style())));
            }
        }
    }

    static Component padSlot(Component translated, Component original, Style style) {
        try {
            int want = LineTranslator.widthOf(original);
            int got = LineTranslator.widthOf(translated);
            if (want <= 0 || got <= 0 || want == got) {
                return translated;
            }
            String fill = SpaceOffset.encode(want - got);
            if (fill.isEmpty()) {
                return translated;
            }
            return Component.empty().append(translated).append(
                    Component.literal(fill).setStyle(SpaceOffset.styleFor(style)));
        } catch (Throwable t) {
            ErrorDebug.note("TemplateLookup#padSlot", null, t);
            return translated;
        }
    }

    private static String lookupTrimmed(String template, TranslationStore store,
                                        boolean percent) {
        String exact = withPercent(template, store, percent);
        if (exact != null) {
            int head = 0;
            while (head < template.length()
                    && Character.isWhitespace(template.charAt(head))) {
                head++;
            }
            int tail = template.length();
            while (tail > head && Character.isWhitespace(template.charAt(tail - 1))) {
                tail--;
            }
            return template.substring(0, head) + exact
                    + reattach(exact, template.substring(tail));
        }
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;

        int start = 0;
        while (start < template.length()) {
            if (template.startsWith(glyph, start)) {
                start += glyph.length();
            } else if (Character.isWhitespace(template.charAt(start))) {
                start++;
            } else if (isDecoration(template.codePointAt(start))) {
                start += Character.charCount(template.codePointAt(start));
            } else {
                break;
            }
        }
        int keepColon = trimEnd(template, start, glyph, false);
        int end = trimEnd(template, start, glyph, true);
        if (start == 0 && end == template.length()) {
            return null;
        }
        boolean plate = start == 0 && !template.substring(end).isBlank();
        try (LookupScope scope = LookupScope.barItemNames(
                TranslationStore.itemNamesBarred() || plate)) {
            String hit = null;
            int used = keepColon;
            if (keepColon > end) {
                String withColon = template.substring(start, keepColon);
                hit = withColon.isBlank() ? null : withPercent(withColon, store, percent);
            }
            if (hit == null) {
                used = end;
                String core = template.substring(start, end);
                if (core.isBlank()) {
                    return null;
                }
                hit = withPercent(core, store, percent);
            }
            if (hit == null) {
                return null;
            }
            return template.substring(0, start) + hit + reattach(hit, template.substring(used));
        }
    }

    static int trimEnd(String template, int start, String glyph, boolean colons) {
        int end = template.length();
        while (end > start) {
            if (end >= start + glyph.length() && template.startsWith(glyph, end - glyph.length())) {
                end -= glyph.length();
            } else if (Character.isWhitespace(template.charAt(end - 1))
                    || (colons && ColumnAligner.isTrailingColon(template.charAt(end - 1)))) {
                end--;
            } else {
                break;
            }
        }
        return end;
    }

    static boolean isDecoration(int codePoint) {
        return Character.getType(codePoint) == Character.OTHER_SYMBOL;
    }

    static String reattach(String translated, String suffix) {
        if (translated.isEmpty()) {
            return suffix;
        }
        char last = translated.charAt(translated.length() - 1);
        if (suffix.isBlank()) {
            return isFullWidthPunctuation(last) ? "" : suffix;
        }
        if (ColumnAligner.isTrailingColon(suffix.charAt(0)) && suffix.substring(1).isBlank()
                && ColumnAligner.isTrailingColon(last)) {
            return suffix.substring(1);
        }
        return suffix;
    }

    private static boolean isFullWidthPunctuation(char c) {
        return "：，。、！？；）」』】".indexOf(c) >= 0;
    }

    static String withPercent(String key, TranslationStore store, boolean percent) {
        if (percent) {
            String hit = store.lookup(key.strip() + "%");
            if (hit != null) {
                return hit;
            }
        }
        String hit = store.lookup(key);
        if (hit != null) {
            return hit;
        }
        hit = withQuality(key, store, percent);
        if (hit != null) {
            return hit;
        }
        hit = withMaker(key, store);
        if (hit != null) {
            return hit;
        }
        hit = withOwner(key, store);
        if (hit == null) {
            hit = eventFrame(key, store);
        }
        if (hit == null) {
            hit = iconlessReward(key, store);
        }
        return hit != null ? hit : npcChatLine(key, store);
    }

    private static final String REWARD_ROW = "- +";

    static String iconlessReward(String key, TranslationStore store) {
        if (key == null || store == null) {
            return null;
        }
        String core = key.strip();
        if (!core.startsWith(REWARD_ROW) || core.indexOf('\n') >= 0) {
            return null;
        }
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        String hit = store.lookup(glyph + core);
        if (hit == null || hit.isBlank() || !hit.startsWith(glyph)) {
            return null;
        }
        String rest = hit.substring(glyph.length());
        return rest.isBlank() ? null : rest;
    }

    static String npcChatLine(String template, TranslationStore store) {
        if (template == null || store == null || !template.startsWith(CHAT_COUNTER)) {
            return null;
        }
        Matcher m = TranslationStore.CHAT_LINE.matcher(template.strip());
        if (!m.matches()) {
            return null;
        }
        String speaker = m.group("speaker").strip();
        String line = m.group("line").strip();
        if (speaker.isEmpty() || !LabelTranslator.hasWord(line)
                || speaker.contains(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            return null;
        }
        String said = lookup(line, store);
        if (said == null || said.isBlank()) {
            return null;
        }
        said = oneLine(said.strip());
        String name = speakerName(speaker, store);
        String numberedName = ColumnTemplates.renumber(name, 2);
        String numberedLine = ColumnTemplates.renumber(said, 2 + ColumnTemplates.countNumbers(speaker));
        if (numberedName == null || numberedLine == null) {
            return null;
        }
        return "[{~1}/{~2}] " + numberedName + ": " + numberedLine;
    }

    static final String CHAT_COUNTER = "[{~}/{~}] ";

    private static String speakerName(String speaker, TranslationStore store) {
        String learned = store.chatSpeaker(speaker);
        if (learned == null && !store.isItemName(speaker)) {
            learned = store.lookupExact(speaker);
        }
        if (learned == null || learned.isBlank() || learned.indexOf('\n') >= 0
                || !samePlaceholders(speaker, learned)) {
            return speaker;
        }
        return learned.strip();
    }

    private static final Pattern NUMBERED_SLOT = Pattern.compile("\\{~\\d}");

    private static boolean samePlaceholders(String a, String b) {
        return ColumnTemplates.countNumbers(a) == ColumnTemplates.countNumbers(
                        NUMBERED_SLOT.matcher(ColourTemplate.stripColourTokens(b)).replaceAll("{~}"))
                && count(a, GlyphSplitter.PLACE_PLACEHOLDER) == count(b, GlyphSplitter.PLACE_PLACEHOLDER)
                && count(a, GlyphSplitter.PLAYER_PLACEHOLDER) == count(b, GlyphSplitter.PLAYER_PLACEHOLDER)
                && count(a, GlyphSplitter.GLYPH_PLACEHOLDER) == count(b, GlyphSplitter.GLYPH_PLACEHOLDER);
    }

    private static String oneLine(String text) {
        if (text.indexOf('\n') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        String[] parts = text.split("\n");
        for (String part : parts) {
            String piece = part.strip();
            if (piece.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                char before = out.charAt(out.length() - 1);
                char after = piece.charAt(0);
                if (!isWide(before) && !isWide(after)) {
                    out.append(' ');
                }
            }
            out.append(piece);
        }
        return out.toString();
    }

    private static boolean isWide(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN
                || Character.UnicodeScript.of(c) == Character.UnicodeScript.HIRAGANA
                || Character.UnicodeScript.of(c) == Character.UnicodeScript.KATAKANA
                || (c >= '　' && c <= '〿') || (c >= '＀' && c <= '￯');
    }

    static String countdownBoard(String template, TranslationStore store) {
        if (template == null || store == null || template.indexOf('\n') < 0) {
            return null;
        }
        String[] rows = template.split("\n", -1);
        boolean timer = false;
        boolean words = false;
        for (String row : rows) {
            timer |= TIMER_ROW.matcher(row.strip()).matches();
            words |= LabelTranslator.hasWord(row);
        }
        if (!timer || !words) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int values = 0;
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                out.append(LineTranslator.NEWLINE);
            }
            String row = rows[i];
            String hit;
            if (TIMER_ROW.matcher(row.strip()).matches()) {
                hit = store.labelLookup(row);
                if (hit == null || hit.isBlank()) {
                    hit = row;
                }
            } else if (!LabelTranslator.hasWord(row)) {
                hit = row;
            } else {
                hit = store.labelLookup(row);
                if (hit == null || hit.isBlank()) {
                    hit = lookup(row, store);
                }
                if (hit == null || hit.isBlank() || hit.indexOf('\n') >= 0) {
                    return null;
                }
            }
            String shifted = ColumnTemplates.renumber(ColourTemplate.stripColourTokens(hit), values);
            if (shifted == null) {
                return null;
            }
            values += ColumnTemplates.countNumbers(row);
            out.append(shifted);
        }
        return out.toString();
    }

    private static final Pattern TIMER_ROW = Pattern.compile("\\{~}[dhms](?: \\{~}[dhms])*");

    static String eventFrame(String template, TranslationStore store) {
        if (template == null || store == null || template.indexOf('\n') >= 0
                || !template.contains(EVENT_MARK)) {
            return null;
        }
        Matcher m = EVENT_LINE.matcher(template.strip());
        if (!m.matches()) {
            return null;
        }
        String name = m.group("name").strip();
        if (name.isEmpty() || name.contains(GlyphSplitter.NUMBER_PLACEHOLDER)
                || name.contains(GlyphSplitter.GLYPH_PLACEHOLDER)
                || name.contains(GlyphSplitter.PLAYER_PLACEHOLDER)) {
            return null;
        }
        int lead = count(m.group("lead"), GlyphSplitter.GLYPH_PLACEHOLDER);
        if (lead > 1) {
            return null;
        }
        String key = GlyphSplitter.GLYPH_PLACEHOLDER + " The " + GlyphSplitter.PLACE_PLACEHOLDER
                + " World Event starts in "
                + (m.group("timer").contains("m") ? "{~}m {~}s" : "{~}s")
                + "! (" + GlyphSplitter.NUMBER_PLACEHOLDER + " blocks away) Click to track";
        String frame = store.lookup(key);
        if (frame == null || frame.isBlank()
                || count(frame, GlyphSplitter.PLACE_PLACEHOLDER) != 1) {
            return null;
        }
        if (lead == 0) {
            if (!frame.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                return null;
            }
            frame = frame.substring(GlyphSplitter.GLYPH_PLACEHOLDER.length()).stripLeading();
        }
        return frame.replace(GlyphSplitter.PLACE_PLACEHOLDER, eventName(name, store));
    }

    private static String eventName(String name, TranslationStore store) {
        String hit = store.lookup(name + EVENT_SUFFIX);
        if (hit == null || hit.isBlank()) {
            return name;
        }
        String bare = TRAILING_BRACKET.matcher(hit.strip()).replaceFirst("").strip();
        return bare.isEmpty() || bare.contains("{") ? name : bare;
    }

    private static int count(String text, String token) {
        int n = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + token.length())) {
            n++;
        }
        return n;
    }

    private static final String EVENT_MARK = " World Event starts in ";

    private static final String EVENT_SUFFIX = " [World Event]";

    private static final Pattern TRAILING_BRACKET =
            Pattern.compile("\\s*[\\[［][^\\[\\]［］]*[\\]］]$");

    private static final Pattern EVENT_LINE = Pattern.compile(
            "(?<lead>(?:\\{#}\\s*)*)The (?<name>.+?) World Event starts in "
                    + "(?<timer>\\{~}m\\s*\\{~}s|\\{~}s)!\\s*\\(\\{~} blocks away\\)\\s*Click to track");

    private static String withMaker(String key, TranslationStore store) {
        String core = key.strip();
        for (String prefix : NAME_PREFIXES) {
            if (!core.startsWith(prefix)) {
                continue;
            }
            String name = core.substring(prefix.length()).strip();
            if (!isName(name)) {
                continue;
            }
            String word = store.lookup(prefix.strip());
            if (word != null && !word.isBlank()) {
                return word + " " + name;
            }
        }
        return null;
    }

    static String withOwner(String key, TranslationStore store) {
        String core = key.strip();
        int at = -1;
        for (String mark : OWNER_MARKS) {
            int found = core.indexOf(mark);
            if (found > 0 && (at < 0 || found < at)) {
                at = found;
            }
        }
        if (at <= 0) {
            return null;
        }
        String name = core.substring(0, at);
        if (!isName(name)) {
            return null;
        }
        String whole = core;
        while (whole.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            whole = whole.substring(GlyphSplitter.GLYPH_PLACEHOLDER.length()).strip();
        }
        if (store.lookupExact(whole) != null || store.isItemName(whole)) {
            return null;
        }
        String thing = core.substring(at + 3).strip();
        String zh;
        try (LookupScope scope = LookupScope.barItemNames(true)) {
            zh = thing.isEmpty() ? null : store.lookup(thing);
        }
        if (zh == null || zh.isBlank()) {
            return null;
        }
        String form = store.lookup(OWNER_FORM);
        if (form == null || !form.contains("{owner}") || !form.contains("{thing}")) {
            return null;
        }
        return form.replace("{owner}", name).replace("{thing}", zh);
    }

    private static final String OWNER_FORM = "{owner}'s {thing}";

    private static final String[] OWNER_MARKS = {"'s ", "’s "};

    static boolean isName(String text) {
        if (text.isEmpty() || text.length() > NAME_ROOM) {
            return false;
        }
        String bare = ANY_SLOT.matcher(text).replaceAll("");
        for (int at = 0; at < bare.length(); at++) {
            char c = bare.charAt(at);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_')) {
                return false;
            }
        }
        return true;
    }

    private static final Pattern ANY_SLOT = Pattern.compile("\\{[^}]*\\}");

    private static final int NAME_ROOM = 40;

    private static final String[] NAME_PREFIXES = {"Crafted by "};

    private static String withQuality(String key, TranslationStore store, boolean percent) {
        String core = key.strip();
        for (String prefix : QUALITY_PREFIXES) {
            if (!core.startsWith(prefix)) {
                continue;
            }
            String name = withPercent(core.substring(prefix.length()), store, percent);
            String word = store.lookup(prefix.strip());
            if (name != null && word != null) {
                return word + name;
            }
        }
        return null;
    }

    private static final String[] QUALITY_PREFIXES = {"Perfect ", "Defective "};
}
