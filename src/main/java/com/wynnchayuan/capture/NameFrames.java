package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NameFrames {

    public static final NameFrames EMPTY = new NameFrames(Map.of(), Map.of(), 0);

    private record Frame(String head, String tail) {
        int anchor() {
            return head.length() + tail.length();
        }

        String core() {
            return head + GlyphSplitter.PLAYER_PLACEHOLDER + tail;
        }
    }

    private final Map<String, List<Frame>> byTail;

    private final Map<String, List<Frame>> byHead;

    private final int size;

    private final int[] tailLengths;

    private final int[] headLengths;

    private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    private NameFrames(Map<String, List<Frame>> byTail, Map<String, List<Frame>> byHead, int size) {
        this.byTail = byTail;
        this.byHead = byHead;
        this.size = size;
        this.tailLengths = lengths(byTail);
        this.headLengths = lengths(byHead);
    }

    private static int[] lengths(Map<String, List<Frame>> frames) {
        return frames.keySet().stream().mapToInt(String::length).distinct().sorted().toArray();
    }

    public int size() {
        return size;
    }

    public static NameFrames of(Collection<String> keys) {
        Map<String, List<Frame>> tails = new HashMap<>();
        Map<String, List<Frame>> heads = new HashMap<>();
        Set<String> seen = new java.util.HashSet<>();
        int count = 0;
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            int first = key.indexOf(GlyphSplitter.PLAYER_PLACEHOLDER);
            if (first < 0 || key.indexOf(GlyphSplitter.PLAYER_PLACEHOLDER, first + 1) >= 0) {
                continue;
            }
            String core = core(key);
            int at = core.indexOf(GlyphSplitter.PLAYER_PLACEHOLDER);
            if (at < 0 || !seen.add(core)) {
                continue;
            }
            Frame frame = new Frame(core.substring(0, at),
                                    core.substring(at + GlyphSplitter.PLAYER_PLACEHOLDER.length()));
            if (!anchored(frame)) {
                continue;
            }
            if (frame.tail().isEmpty()) {
                heads.computeIfAbsent(frame.head(), k -> new ArrayList<>(1)).add(frame);
            } else {
                tails.computeIfAbsent(frame.tail(), k -> new ArrayList<>(1)).add(frame);
            }
            count++;
        }
        return new NameFrames(Map.copyOf(tails), Map.copyOf(heads), count);
    }

    public Broadcasts.Spans find(StyledText line, String template) {
        if (line == null || template == null || size == 0) {
            return null;
        }
        List<Frame> candidates = candidates(core(template));
        if (candidates.isEmpty()) {
            return null;
        }
        String flat = LineParts.flat(line);
        for (Frame frame : candidates) {
            Matcher m = pattern(frame).matcher(flat);
            if (!m.matches()) {
                continue;
            }
            int from = m.start("user");
            int to = m.end("user");
            while (from < to && Character.isWhitespace(flat.charAt(from))) {
                from++;
            }
            while (to > from && Character.isWhitespace(flat.charAt(to - 1))) {
                to--;
            }
            if (!nameLike(flat.substring(from, to))) {
                continue;
            }
            Broadcasts.Spans spans = new Broadcasts.Spans(from, to, -1, -1);
            if (core(LineParts.of(line, spans).template()).equals(frame.core())) {
                return spans;
            }
        }
        return null;
    }

    private List<Frame> candidates(String core) {
        List<Frame> out = new ArrayList<>(2);
        if (core.isEmpty() || core.length() > MAX_TEMPLATE) {
            return out;
        }
        for (int i = tailLengths.length - 1; i >= 0; i--) {
            int cut = core.length() - tailLengths[i];
            List<Frame> frames = cut < 1 || cut >= core.length() ? null : byTail.get(core.substring(cut));
            if (frames == null) {
                continue;
            }
            for (Frame frame : frames) {
                if (frame.head().length() < cut && core.startsWith(frame.head())
                        && templateNameLike(core.substring(frame.head().length(), cut))) {
                    out.add(frame);
                }
            }
        }
        for (int cut : headLengths) {
            List<Frame> frames = cut < 1 || cut >= core.length() ? null : byHead.get(core.substring(0, cut));
            if (frames == null) {
                continue;
            }
            for (Frame frame : frames) {
                if (templateNameLike(core.substring(cut))) {
                    out.add(frame);
                }
            }
        }
        out.sort((a, b) -> Integer.compare(b.anchor(), a.anchor()));
        return out;
    }

    private static final int MAX_TEMPLATE = 400;

    static String core(String text) {
        String s = com.wynnchayuan.translate.TranslationStore.unwrap(text);
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = 0;
        while (at < s.length()) {
            if (s.startsWith(glyph, at)) {
                at += glyph.length();
            } else if (Character.isWhitespace(s.charAt(at))) {
                at++;
            } else {
                break;
            }
        }
        return s.substring(at);
    }

    private static boolean anchored(Frame frame) {
        String literal = (frame.head() + frame.tail()).replaceAll("\\{[^}]*}", "");
        long letters = literal.chars().filter(Character::isLetter).count();
        if (letters < MIN_ANCHOR_LETTERS) {
            return false;
        }
        String tail = frame.tail().stripLeading();
        if (!frame.tail().isEmpty() && (tail.isEmpty() || tail.charAt(0) == '{')) {
            return false;
        }
        String head = frame.head().stripTrailing();
        return !frame.tail().isEmpty() || (!head.isEmpty() && head.charAt(head.length() - 1) != '}');
    }

    private static final int MIN_ANCHOR_LETTERS = 4;

    private Pattern pattern(Frame frame) {
        return patterns.computeIfAbsent(frame.core(), NameFrames::compile);
    }

    private static Pattern compile(String core) {
        StringBuilder re = new StringBuilder("^\\s*");
        StringBuilder literal = new StringBuilder();
        int at = 0;
        while (at < core.length()) {
            char c = core.charAt(at);
            String token = c == '{' ? placeholderAt(core, at) : null;
            if (token == null && !Character.isWhitespace(c)) {
                literal.append(c);
                at++;
                continue;
            }
            if (!literal.isEmpty()) {
                re.append(Pattern.quote(literal.toString()));
                literal.setLength(0);
            }
            if (token == null) {
                re.append("\\s*");
                at++;
                continue;
            }
            switch (token) {
                case "{u}" -> re.append("(?<user>[^\\n]+?)");
                case "{~}" -> re.append("\\d+(?:[.,]\\d+)*%?");
                case "{p}" -> re.append("[^\\n]+?");
                default -> re.append("\\s*");
            }
            at += token.length();
        }
        if (!literal.isEmpty()) {
            re.append(Pattern.quote(literal.toString()));
        }
        re.append("\\s*$");
        return Pattern.compile(re.toString());
    }

    private static String placeholderAt(String text, int at) {
        for (String token : TOKENS) {
            if (text.startsWith(token, at)) {
                return token;
            }
        }
        return null;
    }

    private static final String[] TOKENS = {
            GlyphSplitter.PLAYER_PLACEHOLDER, GlyphSplitter.NUMBER_PLACEHOLDER,
            GlyphSplitter.PLACE_PLACEHOLDER, GlyphSplitter.GLYPH_PLACEHOLDER};

    private static boolean templateNameLike(String slice) {
        if (slice.isBlank() || slice.length() > MAX_NAME * 2
                || slice.contains(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            return false;
        }
        String bare = slice.replace(GlyphSplitter.NUMBER_PLACEHOLDER, "0")
                           .replace(GlyphSplitter.PLACE_PLACEHOLDER, "Place")
                           .replace(GlyphSplitter.PLAYER_PLACEHOLDER, "Name");
        return NAME_CHARS.matcher(bare.strip()).matches();
    }

    public static boolean nameLike(String name) {
        if (name == null) {
            return false;
        }
        String s = name.strip();
        if (s.isEmpty() || s.length() > MAX_NAME || !NAME_CHARS.matcher(s).matches()) {
            return false;
        }
        int slash = s.indexOf('/');
        if (slash != s.lastIndexOf('/')) {
            return false;
        }
        String shown = slash < 0 ? s : s.substring(slash + 1).strip();
        String account = slash < 0 ? null : s.substring(0, slash).strip();
        if (shown.isEmpty() || (account != null && (account.isEmpty() || account.contains(" ")))) {
            return false;
        }
        String[] words = SPACES.split(shown);
        if (words.length > MAX_WORDS) {
            return false;
        }
        return slash >= 0 || !NOT_A_NAME.contains(words[0].toLowerCase(Locale.ROOT));
    }

    private static final Pattern SPACES = Pattern.compile(" +");

    private static final Pattern NAME_CHARS =
            Pattern.compile("[\\p{L}\\p{N}_](?:[\\p{L}\\p{N}_ /\\-]*[\\p{L}\\p{N}_])?");

    private static final int MAX_NAME = 40;

    private static final int MAX_WORDS = 3;

    private static final Set<String> NOT_A_NAME = Set.of(
            "the", "a", "an", "you", "your", "yours", "this", "that", "these", "those",
            "it", "its", "he", "she", "they", "we", "his", "her", "their", "our", "my",
            "there", "here", "everyone", "someone", "nobody", "all", "each");
}
