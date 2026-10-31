package com.wynnchayuan.translate;

import com.wynnchayuan.capture.DialogueSpeaker;
import com.wynnchayuan.capture.GlyphSplitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import static com.wynnchayuan.translate.TranslationStore.respell;
import static com.wynnchayuan.translate.TranslationStore.sameLine;
import static com.wynnchayuan.translate.TranslationStore.slotFits;
import static com.wynnchayuan.translate.TranslationStore.unplacedRivals;
import static com.wynnchayuan.translate.TranslationStore.words;

final class QuestDialogueIndex {

    private final TranslationIndex translations;

    QuestDialogueIndex(TranslationIndex translations) {
        this.translations = translations;
    }

    String speakerOf(String template) {
        return template == null ? null : speakers.get(template.strip());
    }

    final Map<String, String> speakers = new ConcurrentHashMap<>();

    Boolean inCast(String quest, String speaker) {
        if (quest == null || speaker == null || speaker.isBlank()) {
            return null;
        }
        Set<String> cast = questCast.get(GlyphSplitter.stripGlyphChars(quest).strip());
        if (cast == null || cast.isEmpty()) {
            return null;
        }
        return cast.contains(speaker.strip());
    }

    static String questName(String quest) {
        if (quest == null) {
            return null;
        }
        String q = quest.strip();
        return q.endsWith(" (Quest)") ? q.substring(0, q.length() - " (Quest)".length()).strip() : q;
    }

    String questOf(String template) {
        return template == null ? null : questOfLine.get(template.strip());
    }

    final Map<String, Set<String>> questCast = new ConcurrentHashMap<>();

    final Map<String, String> questOfLine = new ConcurrentHashMap<>();

    String lookupPrefix(String partial) {
        String source = matchPrefix(partial);
        return source == null ? null : prefixIndex.get(source);
    }

    String speakerOfPrefix(String partial) {
        String source = matchPrefix(partial);
        return source == null ? null : speakers.get(source);
    }

    String matchPrefix(String partial) {
        return matchPrefix(partial, partial == null ? 0 : partial.strip().length());
    }

    String matchPrefix(String partial, int evidence) {
        return matchPrefix(partial, evidence, null);
    }

    String matchPrefix(String partial, int evidence, String quest) {
        if (partial == null) {
            return null;
        }
        String key = partial.strip();
        if (key.isEmpty()) {
            return null;
        }
        if (quest != null && !quest.isBlank() && key.length() >= MIN_QUEST_PREFIX) {
            TreeMap<String, String> scoped = byQuest.get(
                    GlyphSplitter.stripGlyphChars(quest).strip());
            if (scoped != null) {
                String hit = unique(scoped, key);
                if (hit != null) {
                    String better = curatedRival(hit, key);
                    String pick = better != null ? better : hit;
                    if (aloneInQuest(pick, key, GlyphSplitter.stripGlyphChars(quest).strip())) {
                        return pick;
                    }
                }
            }
        }
        if (evidence < MIN_PREFIX_LENGTH) {
            return null;
        }
        return unique(prefixIndex, key);
    }

    String nearQuestLine(String typed, String quest) {
        if (typed == null || quest == null || quest.isBlank()) {
            return null;
        }
        TreeMap<String, String> scoped = byQuest.get(GlyphSplitter.stripGlyphChars(quest).strip());
        if (scoped == null) {
            return null;
        }
        NearQuery last = lastNear;
        if (last != null && last.typed().equals(typed) && last.quest().equals(quest)) {
            return last.result();
        }
        String result = nearQuestLineUncached(typed, scoped);
        lastNear = new NearQuery(typed, quest, result);
        return result;
    }

    private String nearQuestLineUncached(String typed, TreeMap<String, String> scoped) {
        String[] said = nearWords(typed);
        if (said.length < NEAR_MIN_WORDS) {
            return null;
        }
        int allowed = Math.max(1, said.length / 8);
        String best = null;
        int bestCost = Integer.MAX_VALUE;
        int second = Integer.MAX_VALUE;
        for (String src : scoped.keySet()) {
            String[] words = nearWordCache.computeIfAbsent(src, QuestDialogueIndex::nearWords);
            int cost = Integer.MAX_VALUE;
            for (int take = Math.max(1, said.length - allowed);
                 take <= Math.min(words.length, said.length + allowed); take++) {
                cost = Math.min(cost, wordDistance(said, words, take));
            }
            if (cost < bestCost) {
                second = bestCost;
                bestCost = cost;
                best = src;
            } else if (cost < second) {
                second = cost;
            }
        }
        if (best == null || bestCost > allowed || second - bestCost < 2) {
            return null;
        }
        return best;
    }

    record SeqLine(String src, String speaker, boolean wiki) {}

    private final Map<String, List<SeqLine>> questSeq = new ConcurrentHashMap<>();

    private final Map<String, Map<String, List<Integer>>> questSeqAt = new ConcurrentHashMap<>();

    private final Set<String> seqKeys = ConcurrentHashMap.newKeySet();

    private final Map<String, Set<String>> seqQuestOf = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> bySpeaker = new ConcurrentHashMap<>();

    final Set<String> unplacedLines = ConcurrentHashMap.newKeySet();

    void noteSequence(String entryKey, String src, String quest, String kind,
                              String speaker, boolean wiki) {
        String who = speakerKey(speaker);
        if (!who.isEmpty()) {
            bySpeaker.computeIfAbsent(who, k -> ConcurrentHashMap.newKeySet()).add(src);
        }
        if (quest == null || quest.isBlank()) {
            return;
        }
        if (kind != null && !kind.isBlank() && !"dialogue".equals(kind)) {
            return;
        }
        String q = quest.strip();
        if (!seqKeys.add(q + '\0' + entryKey)) {
            return;
        }
        List<SeqLine> seq = questSeq.computeIfAbsent(q,
                k -> new CopyOnWriteArrayList<>());
        questSeqAt.computeIfAbsent(q, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(src, k -> new CopyOnWriteArrayList<>()).add(seq.size());
        seq.add(new SeqLine(src, who, wiki));
        seqQuestOf.computeIfAbsent(src, k -> ConcurrentHashMap.newKeySet()).add(q);
    }

    static String speakerKey(String raw) {
        String name = DialogueSpeaker.cleanName(raw);
        if (name == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if (Character.isLetterOrDigit(c) || c == '?') {
                out.append(c);
            }
        }
        return out.toString();
    }

    String sequenceQuestOf(String src, String... prefer) {
        if (src == null) {
            return null;
        }
        String key = src.strip();
        Set<String> homes = seqQuestOf.get(key);
        if (homes == null || homes.isEmpty()) {
            return null;
        }
        for (String p : prefer) {
            if (p != null) {
                String q = GlyphSplitter.stripGlyphChars(p).strip();
                if (homes.contains(q)) {
                    return q;
                }
            }
        }
        return homes.size() == 1 ? homes.iterator().next() : null;
    }

    int sequenceIndex(String quest, String src, int after) {
        if (quest == null || src == null) {
            return -1;
        }
        Map<String, List<Integer>> at = questSeqAt.get(quest);
        List<Integer> spots = at == null ? null : at.get(src.strip());
        if (spots == null || spots.isEmpty()) {
            return -1;
        }
        for (int spot : spots) {
            if (spot > after) {
                return spot;
            }
        }
        return spots.get(0);
    }

    String forecast(String typed, String quest, int after, int window, String speaker,
                           int floor, BiPredicate<String, String> fits) {
        if (typed == null || quest == null || speaker == null) {
            return null;
        }
        String key = typed.strip();
        String who = speakerKey(speaker);
        if (key.isEmpty() || key.length() < floor || who.isEmpty()) {
            return null;
        }
        return forecastUncached(key, GlyphSplitter.stripGlyphChars(quest).strip(), after,
                window, who, fits, true);
    }

    String forecastBySpeaker(String typed, String quest, String speaker, int floor,
                                    BiPredicate<String, String> fits) {
        if (typed == null || quest == null || speaker == null) {
            return null;
        }
        String key = typed.strip();
        String who = speakerKey(speaker);
        if (key.isEmpty() || key.length() < floor || who.isEmpty()) {
            return null;
        }
        return forecastUncached(key, GlyphSplitter.stripGlyphChars(quest).strip(), -1, 0, who,
                fits, false);
    }

    String forecastNext(String typed, String quest, int after, int window, String speaker,
                               int floor, BiPredicate<String, String> fits) {
        if (typed == null || quest == null || after < 0) {
            return null;
        }
        String key = typed.strip();
        if (key.length() < floor) {
            return null;
        }
        List<SeqLine> seq = questSeq.get(GlyphSplitter.stripGlyphChars(quest).strip());
        if (seq == null) {
            return null;
        }
        String who = speaker == null ? "" : speakerKey(speaker);
        List<SeqLine> group = new ArrayList<>();
        for (int i = after + 1; i < Math.min(seq.size(), after + 1 + window); i++) {
            SeqLine line = seq.get(i);
            if (!who.isEmpty() && !line.speaker().isEmpty() && !line.speaker().equals(who)) {
                continue;
            }
            if (!looseFit(line.src(), key)) {
                continue;
            }
            if (!group.isEmpty() && !group.get(0).src().equals(line.src())
                    && !versionOf(group.get(0).src(), line.src())) {
                return null;
            }
            group.add(line);
        }
        String pick = null;
        for (SeqLine line : group) {
            String dst = translations.entries.get(line.src());
            if (dst == null || dst.isBlank() || !fits.test(line.src(), key)) {
                continue;
            }
            if (pick == null || (!line.wiki() && fromWiki.contains(pick))) {
                pick = line.src();
            }
        }
        return pick;
    }

    private String forecastUncached(String key, String quest, int after, int window, String who,
                                    BiPredicate<String, String> fits, boolean elsewhere) {
        List<SeqLine> seq = questSeq.get(quest);
        if (seq == null) {
            return null;
        }
        int from = after >= 0 ? after + 1 : 0;
        int to = after >= 0 ? Math.min(seq.size(), from + window) : seq.size();
        List<SeqLine> group = new ArrayList<>();
        for (int i = from; i < to; i++) {
            SeqLine line = seq.get(i);
            if (!line.speaker().isEmpty() && !line.speaker().equals(who)) {
                continue;
            }
            if (!looseFit(line.src(), key)) {
                continue;
            }
            if (!group.isEmpty() && !group.get(0).src().equals(line.src())
                    && !versionOf(group.get(0).src(), line.src())) {
                return null;
            }
            group.add(line);
        }
        if (group.isEmpty()) {
            return null;
        }
        boolean vouched = false;
        for (SeqLine line : group) {
            vouched |= line.speaker().equals(who);
        }
        if (!vouched) {
            return null;
        }
        String pick = null;
        for (SeqLine line : group) {
            String dst = translations.entries.get(line.src());
            if (dst == null || dst.isBlank() || !fits.test(line.src(), key)) {
                continue;
            }
            if (pick == null || (!line.wiki() && fromWiki.contains(pick))) {
                pick = line.src();
            }
        }
        if (pick == null) {
            return null;
        }
        for (SeqLine line : seq) {
            if ((line.speaker().isEmpty() || line.speaker().equals(who))
                    && rival(pick, line.src(), key)) {
                return null;
            }
        }
        if (!elsewhere) {
            return pick;
        }
        Set<String> theirs = bySpeaker.get(who);
        if (theirs != null) {
            for (String src : theirs) {
                if (rival(pick, src, key)) {
                    return null;
                }
            }
        }
        for (String src : unplacedRivals ? unplacedLines : Set.<String>of()) {
            if (rival(pick, src, key)) {
                return null;
            }
        }
        return pick;
    }

    String explainForecast(String typed, String quest, int after, int window,
                                  String speaker, BiPredicate<String, String> fits) {
        String who = speakerKey(speaker);
        if (who.isEmpty()) {
            return "沒有名牌";
        }
        String key = typed.strip();
        List<SeqLine> seq = questSeq.get(quest == null ? "" : quest);
        if (seq == null) {
            return "沒有任務";
        }
        int from = after >= 0 ? after + 1 : 0;
        int to = after >= 0 ? Math.min(seq.size(), from + window) : seq.size();
        List<SeqLine> group = new ArrayList<>();
        for (int i = from; i < to; i++) {
            SeqLine line = seq.get(i);
            if (!line.speaker().isEmpty() && !line.speaker().equals(who)) {
                continue;
            }
            if (!looseFit(line.src(), key)) {
                continue;
            }
            if (!group.isEmpty() && !group.get(0).src().equals(line.src())
                    && !versionOf(group.get(0).src(), line.src())) {
                return "後面幾句裡有兩句同開頭";
            }
            group.add(line);
        }
        if (group.isEmpty()) {
            return "不在後面幾句裡";
        }
        boolean vouched = false;
        String pick = null;
        for (SeqLine line : group) {
            vouched |= line.speaker().equals(who);
            String dst = translations.entries.get(line.src());
            if (dst != null && !dst.isBlank() && fits.test(line.src(), key)) {
                pick = line.src();
            }
        }
        if (!vouched) {
            return "語料沒標說話者";
        }
        if (pick == null) {
            return "沒有譯文";
        }
        for (SeqLine line : seq) {
            if ((line.speaker().isEmpty() || line.speaker().equals(who))
                    && rival(pick, line.src(), key)) {
                return line.src().startsWith(key) ? "任務裡別句同開頭" : "任務裡別句岔在佔位符";
            }
        }
        Set<String> theirs = bySpeaker.get(who);
        if (theirs != null) {
            for (String src : theirs) {
                if (rival(pick, src, key)) {
                    return "同一人別的任務有句同開頭";
                }
            }
        }
        for (String src : unplacedLines) {
            if (rival(pick, src, key)) {
                return "未歸屬台詞同開頭";
            }
        }
        return "可以";
    }

    String explainQuest(String typed, String quest) {
        String key = typed.strip();
        if (quest == null) {
            return "沒有任務";
        }
        String q = GlyphSplitter.stripGlyphChars(quest).strip();
        if (key.length() < MIN_QUEST_PREFIX) {
            return "不到4字";
        }
        TreeMap<String, String> scoped = byQuest.get(q);
        if (scoped == null) {
            return "沒有任務";
        }
        String hit = unique(scoped, key);
        if (hit == null) {
            Map.Entry<String, String> first = scoped.ceilingEntry(key);
            return first == null || !first.getKey().startsWith(key) ? "任務裡沒有這句"
                    : "任務裡多句同開頭";
        }
        String better = curatedRival(hit, key);
        String pick = better != null ? better : hit;
        List<SeqLine> seq = questSeq.get(q);
        if (seq != null) {
            for (SeqLine line : seq) {
                if (rival(pick, line.src(), key)) {
                    if (!line.src().startsWith(key)) {
                        return "任務裡別句岔在佔位符";
                    }
                    String dst = translations.entries.get(line.src());
                    return dst == null || dst.isBlank() ? "任務裡沒翻的句同開頭" : "任務裡短版本同開頭";
                }
            }
        }
        return "可以";
    }

    private boolean aloneInQuest(String pick, String key, String quest) {
        List<SeqLine> seq = questSeq.get(quest);
        if (seq == null) {
            return true;
        }
        for (SeqLine line : seq) {
            if (rival(pick, line.src(), key)) {
                return false;
            }
        }
        return true;
    }

    private boolean rival(String pick, String src, String key) {
        return !src.equals(pick) && looseFit(src, key) && !versionOf(pick, src);
    }

    static boolean looseFit(String src, String typed) {
        if (src.startsWith(typed) || slotFits(src, typed)) {
            return true;
        }
        String other = respell(typed);
        return !other.equals(typed) && src.startsWith(other);
    }

    private static final int NEAR_MIN_WORDS = 8;

    private record NearQuery(String typed, String quest, String result) {}

    private volatile NearQuery lastNear;

    private final Map<String, String[]> nearWordCache =
            new ConcurrentHashMap<>();

    private static final Pattern NEAR_SPACE = Pattern.compile("\\s+");

    private static final Pattern NEAR_EDGES =
            Pattern.compile("^[\\p{Punct}…“”‘’]+|[\\p{Punct}…“”‘’]+$");

    private static String[] nearWords(String text) {
        String[] raw = NEAR_SPACE.split(text.strip());
        List<String> out = new ArrayList<>(raw.length);
        for (String w : raw) {
            String t = NEAR_EDGES.matcher(w.toLowerCase(Locale.ROOT)).replaceAll("");
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out.toArray(new String[0]);
    }

    private static int wordDistance(String[] a, String[] b, int take) {
        int[] prev = new int[take + 1];
        int[] cur = new int[take + 1];
        for (int j = 0; j <= take; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length; i++) {
            cur[0] = i;
            for (int j = 1; j <= take; j++) {
                int sub = prev[j - 1] + (a[i - 1].equals(b[j - 1]) ? 0 : 1);
                cur[j] = Math.min(sub, Math.min(prev[j] + 1, cur[j - 1] + 1));
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[take];
    }

    private String unique(TreeMap<String, String> index, String key) {
        Map.Entry<String, String> first = index.ceilingEntry(key);
        if (first == null || !first.getKey().startsWith(key)) {
            String other = respell(key);
            if (other.equals(key)) {
                return null;
            }
            first = index.ceilingEntry(other);
            if (first == null || !first.getKey().startsWith(other)) {
                return null;
            }
            key = other;
        }
        String next = index.higherKey(first.getKey());
        if (next != null && next.startsWith(key)) {
            return settle(index, key, first.getKey());
        }
        return first.getKey();
    }

    private String settle(TreeMap<String, String> index, String key, String first) {
        String curated = null;
        int scanned = 0;
        int best = -1;
        for (Map.Entry<String, String> e : index.tailMap(key).entrySet()) {
            if (!e.getKey().startsWith(key)) {
                break;
            }
            if (++scanned > RIVAL_SCAN || !sameLine(first, e.getKey())) {
                return null;
            }
            best = Math.max(best, translations.layerOf(e.getKey()));
            if (!fromWiki.contains(e.getKey())) {
                if (curated != null) {
                    return null;
                }
                curated = e.getKey();
            }
        }
        return curated != null && translations.layerOf(curated) < best ? null : curated;
    }

    static final int MIN_PREFIX_LENGTH = 12;

    // 實測限定任務後平均第 4 個字就分得出
    private static final int MIN_QUEST_PREFIX = 4;

    private String curatedRival(String wikiHit, String key) {
        if (!fromWiki.contains(wikiHit)) {
            return null;
        }
        int scanned = 0;
        for (Map.Entry<String, String> e : prefixIndex.tailMap(key).entrySet()) {
            if (!e.getKey().startsWith(key) || ++scanned > RIVAL_SCAN) {
                break;
            }
            if (fromWiki.contains(e.getKey()) || e.getKey().equals(wikiHit)) {
                continue;
            }
            String home = questOfLine.get(wikiHit);
            String theirs = questOfLine.get(e.getKey());
            if (home != null && theirs != null && !home.equals(theirs)) {
                continue;
            }
            if (versionOf(wikiHit, e.getKey())
                    && translations.layerOf(e.getKey()) >= translations.layerOf(wikiHit)) {
                return e.getKey();
            }
        }
        return null;
    }

    static boolean versionOf(String a, String b) {
        return words(a).size() >= VERSION_WORDS && words(b).size() >= VERSION_WORDS
                && sameLine(a, b);
    }

    private static final int VERSION_WORDS = 6;

    private static final int RIVAL_SCAN = 16;

    final Set<String> fromWiki = ConcurrentHashMap.newKeySet();

    boolean hasLonger(String key) {
        if (key == null) {
            return false;
        }
        String bare = key.strip();
        if (bare.isEmpty()) {
            return false;
        }
        String next = ordered.higher(bare);
        return next != null && next.startsWith(bare);
    }

    boolean couldStillMatch(String typed) {
        if (typed == null) {
            return false;
        }
        String key = typed.strip();
        if (key.isEmpty()) {
            return true;
        }
        if (startsSome(key)) {
            return true;
        }
        String other = respell(key);
        if (!other.equals(key) && startsSome(other)) {
            return true;
        }
        for (int cut = key.length() - 1; cut >= 0; cut--) {
            String before = key.substring(0, cut);
            for (String slot : SLOTS) {
                String head = before + slot;
                int scanned = 0;
                for (String src : ordered.tailSet(head)) {
                    if (!src.startsWith(head) || ++scanned > 64) {
                        break;
                    }
                    if (slotFits(src, key)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static final String[] SLOTS = {"{u}", "{p}", "{~", "{#}"};

    private boolean startsSome(String key) {
        String next = ordered.ceiling(key);
        return next != null && next.startsWith(key);
    }

    final ConcurrentSkipListSet<String> ordered = new ConcurrentSkipListSet<>();

    final TreeMap<String, String> prefixIndex = new TreeMap<>();

    final Map<String, TreeMap<String, String>> byQuest = new HashMap<>();
}
