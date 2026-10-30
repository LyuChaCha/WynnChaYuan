package com.wynnchayuan.translate;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Wynnventory 只附英文語言檔，所以包一層 Language
public final class WynnventoryBridge {

    private WynnventoryBridge() {}

    public static final String SCOPE = "wynnventory";

    private static volatile Boolean installed;

    private static volatile boolean active;

    private static boolean ticked;
    private static int seenGeneration;

    public static boolean installed() {
        Boolean known = installed;
        if (known == null) {
            boolean found;
            try {
                found = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("wynnventory");
            } catch (Throwable t) {
                ErrorDebug.note("WynnventoryBridge#installed", null, t);
                found = false;
            }
            installed = known = found;
        }
        return known;
    }

    static void pretend(boolean isInstalled, boolean isActive) {
        installed = isInstalled;
        active = isActive;
    }

    public static boolean active() {
        return active;
    }

    public static void tick(boolean enabled, boolean peeking, int generation) {
        if (!installed()) {
            return;
        }
        boolean now = enabled && !peeking;
        if (ticked && now == active && generation == seenGeneration) {
            return;
        }
        ticked = true;
        active = now;
        seenGeneration = generation;
        refresh();
    }

    private static void refresh() {
        try {
            Language.inject(wrap(Language.getInstance()));
        } catch (Throwable t) {
            ErrorDebug.note("wynnventory.refresh", null, t);
        }
    }

    // TranslatableContents 靠 Language 物件身分決定要不要重查
    public static Language wrap(Language language) {
        if (language == null || !installed()) {
            return language;
        }
        Language base = language instanceof Wrapped w ? w.inner : language;
        return new Wrapped(base);
    }

    static boolean ours(String key) {
        return key != null && key.contains("wynnventory");
    }

    static String lang(String key, String base) {
        if (!active || base == null || !ours(key)) {
            return base;
        }
        try {
            String hit = text(WynnChaYuan.translations(), base);
            return hit == null ? base : hit;
        } catch (Throwable t) {
            ErrorDebug.note("wynnventory.lang", key, t);
            return base;
        }
    }

    public static String text(TranslationStore store, String english) {
        if (store == null || english == null) {
            return null;
        }
        String core = english.strip();
        if (core.isEmpty()) {
            return null;
        }
        String hit = store.scopedLookup(SCOPE, core);
        if (hit == null || hit.isBlank() || hit.equals(core)) {
            return null;
        }
        int lead = english.indexOf(core);
        return english.substring(0, lead) + hit + english.substring(lead + core.length());
    }

    private static final class Wrapped extends Language {
        private final Language inner;

        private Wrapped(Language inner) {
            this.inner = inner;
        }

        @Override
        public String getOrDefault(String key, String defaultValue) {
            String base = inner.getOrDefault(key, defaultValue);
            return ours(key) ? lang(key, base) : base;
        }

        @Override
        public boolean has(String key) {
            return inner.has(key);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return inner.isDefaultRightToLeft();
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return inner.getVisualOrder(text);
        }
    }

    public static Component label(Component text) {
        if (!active || text == null) {
            return null;
        }
        try {
            String hit = text(store(), text.getString());
            return hit == null ? null : Component.literal(hit).withStyle(text.getStyle());
        } catch (Throwable t) {
            ErrorDebug.note("wynnventory.label", null, t);
            return null;
        }
    }

    public static String word(String english) {
        if (!active || english == null || english.isEmpty()) {
            return english;
        }
        try {
            String hit = text(store(), english);
            return hit == null ? english : hit;
        } catch (Throwable t) {
            ErrorDebug.note("wynnventory.word", english, t);
            return english;
        }
    }

    private static final java.util.regex.Pattern TOAST_FOUND = java.util.regex.Pattern.compile(
            "^((?:\u00a7.)*)(.+?)((?:\u00a7.)*) in (.+)$");
    private static final java.util.regex.Pattern TOAST_MORE = java.util.regex.Pattern.compile(
            "^(\\d+) more\\.\\.\\.$");

    public static Component toast(Component description) {
        if (!active || description == null) {
            return description;
        }
        try {
            String done = toast(store(), description.getString());
            return done == null ? description
                    : Component.literal(done).withStyle(description.getStyle());
        } catch (Throwable t) {
            ErrorDebug.note("wynnventory.toast", null, t);
            return description;
        }
    }

    static String toast(TranslationStore store, String line) {
        if (store == null || line == null) {
            return null;
        }
        java.util.regex.Matcher more = TOAST_MORE.matcher(line);
        if (more.matches()) {
            String frame = text(store, "%s more...");
            return frame == null ? null : frame.replace("%s", more.group(1));
        }
        java.util.regex.Matcher found = TOAST_FOUND.matcher(line);
        if (!found.matches()) {
            return null;
        }
        String name = shownName(store, found.group(2));
        String frame = text(store, "%s in %s");
        if ((name == null || name.isBlank()) && frame == null) {
            return null;
        }
        String item = found.group(1) + (name == null || name.isBlank() ? found.group(2) : name)
                + found.group(3);
        String shape = frame == null ? "%s in %s" : frame;
        int first = shape.indexOf("%s");
        int second = shape.indexOf("%s", first + 2);
        if (first < 0 || second < 0) {
            return null;
        }
        return shape.substring(0, first) + item + shape.substring(first + 2, second)
                + found.group(4) + shape.substring(second + 2);
    }

    private static TranslationStore store() {
        try {
            return WynnChaYuan.translations();
        } catch (Throwable t) {
            ErrorDebug.note("WynnventoryBridge#store", null, t);
            return null;
        }
    }

    public static List<Component> lines(List<Component> lines) {
        if (!active || lines == null || lines.isEmpty()) {
            return lines;
        }
        try {
            return lines(WynnChaYuan.translations(), lines);
        } catch (Throwable t) {
            ErrorDebug.note("wynnventory.lines", null, t);
            return lines;
        }
    }

    static List<Component> lines(TranslationStore store, List<Component> lines) {
        if (store == null) {
            return lines;
        }
        List<Component> out = null;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            List<Component> rows = line == null ? null : rows(store, line);
            boolean same = rows == null || (rows.size() == 1 && rows.get(0) == line);
            if (!same && out == null) {
                out = new ArrayList<>(lines.subList(0, i));
            }
            if (out != null) {
                if (rows == null) {
                    out.add(line);
                } else {
                    out.addAll(rows);
                }
            }
        }
        return out == null ? lines : out;
    }

    private static final String ORIGINAL_INDENT = "     ";

    static String indentFor(String before) {
        if (before.isEmpty()) {
            return "";
        }
        return before.isBlank() ? before : ORIGINAL_INDENT;
    }

    private static int leadOf(String text) {
        int at = 0;
        while (at < text.length() && !Character.isLetterOrDigit(text.codePointAt(at))) {
            at += Character.charCount(text.codePointAt(at));
        }
        return at;
    }

    static List<Component> rows(TranslationStore store, Component line) {
        List<Piece> pieces = new ArrayList<>();
        line.visit((style, text) -> {
            pieces.add(new Piece(style, text));
            return Optional.empty();
        }, Style.EMPTY);
        boolean changed = false;
        MutableComponent out = Component.empty();
        Component original = null;
        for (Piece p : pieces) {
            String done = piece(store, p.text());
            if (done == null) {
                out.append(Component.literal(p.text()).withStyle(p.style()));
                continue;
            }
            changed = true;
            String english = englishOf(p.text());
            String tail = english == null ? null : " (" + english + ")";
            int at = tail == null ? -1 : done.lastIndexOf(tail);
            if (original == null && at > 0 && done.substring(at + tail.length()).isBlank()) {
                String before = out.getString() + done.substring(0, Math.min(at, leadOf(done)));
                out.append(Component.literal(done.substring(0, at)
                        + done.substring(at + tail.length())).withStyle(p.style()));
                original = Component.literal(indentFor(before) + tail.strip()).withStyle(p.style());
            } else {
                out.append(Component.literal(done).withStyle(p.style()));
            }
        }
        if (!changed) {
            return List.of(line);
        }
        return original == null ? List.of(out) : List.of(out, original);
    }

    private static String englishOf(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (latinLetter(text.charAt(i))) {
                String body = text.substring(i).stripTrailing();
                return body.isEmpty() ? null : body;
            }
        }
        return null;
    }

    private record Piece(Style style, String text) {}

    static Component line(TranslationStore store, Component line) {
        List<Piece> pieces = new ArrayList<>();
        line.visit((style, text) -> {
            pieces.add(new Piece(style, text));
            return Optional.empty();
        }, Style.EMPTY);
        boolean changed = false;
        MutableComponent out = Component.empty();
        for (Piece p : pieces) {
            String done = piece(store, p.text());
            if (done != null) {
                changed = true;
            }
            out.append(Component.literal(done == null ? p.text() : done).withStyle(p.style()));
        }
        return changed ? out : line;
    }

    static String piece(TranslationStore store, String text) {
        if (text == null) {
            return null;
        }
        String own = text(store, text);
        if (own != null) {
            return own;
        }
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                return null;
            }
            if (latinLetter(c)) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return null;
        }
        String body = text.substring(start).stripTrailing();
        if (body.isEmpty()) {
            return null;
        }
        String hit = shownName(store, body);
        if (hit == null || hit.isBlank() || hit.equals(body)) {
            return null;
        }
        return text.substring(0, start) + hit + text.substring(start + body.length());
    }

    static String shownName(TranslationStore store, String text) {
        if (!store.isItemName(text)) {
            return store.lookup(text);
        }
        if (store.isBareGearName(text) && !(store.translatesNames() && store.gearOwnName(text) != null)) {
            return null;
        }
        try (LookupScope hold = LookupScope.holdAppendedOriginal(false);
             LookupScope nameLine = LookupScope.nameLine(true)) {
            return store.lookup(text);
        }
    }

    private static boolean latinLetter(char c) {
        return c < 0x250 && Character.isLetter(c);
    }
}
