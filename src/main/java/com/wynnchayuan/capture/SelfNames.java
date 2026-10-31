package com.wynnchayuan.capture;

import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// 伺服器會把名字轉大寫；暱稱可含空格、與帳號名無關
public final class SelfNames {

    private static final int MIN_NAME = 3;

    private static final int MAX_PREFIX_WORDS = 3;

    private static final int MAX_LEARNED = 256;

    private static final long NAMES_MAX_AGE_NANOS = 1_000_000_000L;

    private static final Set<String> learned = new LinkedHashSet<>();

    private record Names(int player, int version, long at, List<String> all, List<String> usable) {}

    private static volatile Names names;

    private static volatile int version;

    private static final Set<String> UNSET =
            Set.of("not defined", "none", "n/a", "unset", "not set", "-");

    private static final Set<String> COMMON_WORDS = Set.of(
            "the", "and", "you", "your", "yours", "for", "with", "that",
            "this", "what", "who", "whom", "whose", "why", "how", "when",
            "where", "they", "them", "their", "there", "here", "these", "those",
            "not", "but", "all", "any", "one", "two", "now", "new",
            "old", "out", "own", "off", "get", "got", "let", "man",
            "men", "way", "say", "see", "too", "use", "yes", "was",
            "are", "been", "have", "has", "had", "will", "would", "can",
            "could", "should", "may", "him", "his", "her", "hers", "she",
            "its", "our", "ours", "mine", "from", "into", "just", "like",
            "more", "most", "much", "some", "such", "than", "then", "very",
            "well", "were", "only", "over", "same", "take", "come", "know",
            "good", "back", "give", "make", "need", "want", "look", "find",
            "help", "keep", "let's", "yeah", "okay", "hey", "hi", "oh",
            "ah", "no");

    private SelfNames() {}

    public static void learn(String line) {
        if (line == null) {
            return;
        }
        String tag = "Nickname:";
        int at = line.indexOf(tag);
        if (at < 0) {
            return;
        }
        remember(line.substring(at + tag.length()).strip());
    }

    public static final int MIN_EVIDENCE = 1;

    private static final Map<String, Set<String>> proposals = new LinkedHashMap<>();

    public static void propose(String name, String source) {
        if (name == null || source == null) {
            return;
        }
        boolean enough;
        synchronized (learned) {
            Set<String> keys = proposals.computeIfAbsent(name, n -> new LinkedHashSet<>());
            keys.add(source);
            enough = keys.size() >= MIN_EVIDENCE;
            if (proposals.size() > MAX_LEARNED) {
                proposals.remove(proposals.keySet().iterator().next());
            }
        }
        if (enough) {
            remember(name);
        }
    }

    public static void remember(String name) {
        if (name == null) {
            return;
        }
        String bare = name.strip();
        if (bare.length() < MIN_NAME || UNSET.contains(bare.toLowerCase(Locale.ROOT))) {
            return;
        }
        synchronized (learned) {
            if (learned.add(bare)) {
                if (learned.size() > MAX_LEARNED) {
                    learned.remove(learned.iterator().next());
                }
                version++;
            }
        }
    }

    public static List<String> describe() {
        List<String> out = new ArrayList<>();
        synchronized (learned) {
            for (String name : learned) {
                out.add((proposals.containsKey(name) ? "語料反推 " : "角色選單 ")
                        + name.length() + " 個字元");
            }
        }
        String account = accountName();
        if (account != null && account.length() >= MIN_NAME) {
            out.add("帳號名 " + account.length() + " 個字元");
        }
        for (String shown : displayNames()) {
            for (String variant : suffixes(shown)) {
                out.add("顯示名稱 " + variant.length() + " 個字元");
            }
        }
        return out;
    }

    public static List<String> all() {
        return current().all();
    }

    // 每一段文字都要查一次，顯示名稱不能每次現算；換角色會換 LocalPlayer
    private static Names current() {
        Names cached = names;
        int player = System.identityHashCode(localPlayer());
        int seen = version;
        long now = System.nanoTime();
        if (cached != null && cached.player() == player && cached.version() == seen
                && now - cached.at() < NAMES_MAX_AGE_NANOS) {
            return cached;
        }
        Set<String> out;
        synchronized (learned) {
            out = new LinkedHashSet<>(learned);
        }
        addName(out, accountName());
        for (String shown : displayNames()) {
            out.addAll(suffixes(shown));
        }
        List<String> sorted = new ArrayList<>(out);
        sorted.sort((a, b) -> b.length() - a.length());
        List<String> usable = new ArrayList<>(sorted.size());
        for (String name : sorted) {
            if (usable(name)) {
                usable.add(name);
            }
        }
        Names fresh = new Names(player, seen, now, List.copyOf(sorted), List.copyOf(usable));
        names = fresh;
        return fresh;
    }

    private static Object localPlayer() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null ? null : mc.player;
        } catch (Throwable t) {
            ErrorDebug.note("SelfNames#localPlayer", null, t);
            return null;
        }
    }

    public static String find(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        for (String name : current().usable()) {
            int at = indexOfWord(text, name);
            if (at >= 0) {
                return text.substring(at, at + name.length());
            }
        }
        return null;
    }

    static boolean usable(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String core = name.strip().toLowerCase(Locale.ROOT);
        return !UNSET.contains(core) && !COMMON_WORDS.contains(core);
    }

    private static int indexOfWord(String text, String name) {
        int last = text.length() - name.length();
        char first = folded(name.charAt(0));
        for (int at = 0; at <= last; at++) {
            if (folded(text.charAt(at)) != first
                    || !text.regionMatches(true, at, name, 0, name.length())) {
                continue;
            }
            if (at > 0 && Character.isLetterOrDigit(text.charAt(at - 1))) {
                continue;
            }
            int after = at + name.length();
            if (after < text.length() && Character.isLetterOrDigit(text.charAt(after))) {
                continue;
            }
            return at;
        }
        return -1;
    }

    private static char folded(char c) {
        return Character.toLowerCase(Character.toUpperCase(c));
    }

    static List<String> suffixes(String shown) {
        List<String> out = new ArrayList<>();
        if (shown == null) {
            return out;
        }
        String s = shown.strip();
        for (int i = 0; i <= MAX_PREFIX_WORDS && !s.isEmpty(); i++) {
            if (s.length() >= MIN_NAME && usable(s)) {
                out.add(s);
            }
            int space = s.indexOf(' ');
            if (space < 0) {
                break;
            }
            s = s.substring(space + 1).strip();
        }
        return out;
    }

    private static void addName(Set<String> out, String name) {
        if (name != null && name.length() >= MIN_NAME) {
            out.add(name);
        }
    }

    private static String accountName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            String name = mc == null || mc.getUser() == null ? null : mc.getUser().getName();
            return name == null || name.isBlank() ? null : name;
        } catch (Throwable t) {
            ErrorDebug.note("SelfNames#accountName", null, t);
            return null;
        }
    }

    private static List<String> displayNames() {
        List<String> out = new ArrayList<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                return out;
            }
            plain(out, mc.player.getDisplayName());
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info =
                        mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) {
                    plain(out, info.getTabListDisplayName());
                }
            }
        } catch (Throwable t) {
            ErrorDebug.note("SelfNames#displayNames", null, t);
        }
        return out;
    }

    private static void plain(List<String> out, Component component) {
        if (component == null) {
            return;
        }
        String text = component.getString().strip();
        if (!text.isEmpty()) {
            out.add(text);
        }
    }
}
