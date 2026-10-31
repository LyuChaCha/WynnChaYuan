package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class ColourAudit {

    private ColourAudit() {}

    static final int LIMIT = 400;

    public record Run(String colour, String text) {}

    public record Row(String src, List<String> dst, List<Run> runs, List<String> missed) {}

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();

    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private static final int SEEN_LIMIT = 4096;
    private static volatile boolean dirty;

    private static final Pattern MARKER = Pattern.compile("\\{(?:c[^}]*|w\\d|/)}");

    public static void note(List<String> src, String[] dst, List<LineParts.Piece> runs,
                            List<LineParts.Piece> accents, boolean[] used, Style dominant) {
        try {
            if (src == null || dst == null) {
                return;
            }
            String id = String.join("\n", src) + '\u0000' + String.join("\n", dst);
            if (!SEEN.add(id)) {
                return;
            }
            if (SEEN.size() > SEEN_LIMIT) {
                SEEN.clear();
            }
            Row row = audit(src, dst, runs, accents, used, dominant);
            if (row == null) {
                if (src != null) {
                    synchronized (ROWS) {
                        if (!ROWS.isEmpty() && ROWS.remove(String.join("\n", src)) != null) {
                            dirty = true;
                        }
                    }
                }
                return;
            }
            synchronized (ROWS) {
                if (ROWS.containsKey(row.src()) || ROWS.size() >= LIMIT) {
                    return;
                }
                ROWS.put(row.src(), row);
            }
            dirty = true;
        } catch (Throwable t) {
            ErrorDebug.note("ColourAudit#note", null, t);
        }
    }

    static Row audit(List<String> src, String[] dst, List<LineParts.Piece> runs,
                     List<LineParts.Piece> accents, boolean[] used, Style dominant) {
        if (src == null || dst == null || runs == null || accents == null) {
            return null;
        }
        String key = String.join("\n", src);
        if (key.isBlank() || key.contains(GlyphSplitter.PLAYER_PLACEHOLDER)
                || PlayerDataFilter.carriesPlayerData(key)) {
            return null;
        }
        for (String line : dst) {
            if (line != null && MARKER.matcher(line).find()) {
                return null;
            }
        }
        Set<String> present = new LinkedHashSet<>();
        List<Run> shown = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            String text = clean(run.text());
            if (text.isBlank()) {
                continue;
            }
            String colour = describe(run.style());
            shown.add(new Run(colour, text));
            if (hasLetter(withoutNumbers(text))) {
                present.add(colour);
            }
        }
        if (present.size() < 2) {
            return null;
        }
        Set<String> placed = new LinkedHashSet<>();
        placed.add(describe(dominant));
        for (int i = 0; i < accents.size(); i++) {
            if (used != null && i < used.length && used[i]) {
                placed.add(describe(accents.get(i).style()));
            }
        }
        List<String> missed = new ArrayList<>();
        for (String colour : present) {
            if (!placed.contains(colour)) {
                missed.add(colour);
            }
        }
        if (missed.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>(dst.length);
        for (String line : dst) {
            lines.add(line == null ? "" : line);
        }
        return new Row(key, lines, merge(shown), missed);
    }

    private static List<Run> merge(List<Run> runs) {
        List<Run> out = new ArrayList<>();
        for (Run run : runs) {
            if (!out.isEmpty() && out.get(out.size() - 1).colour().equals(run.colour())) {
                Run last = out.remove(out.size() - 1);
                out.add(new Run(last.colour(), last.text() + run.text()));
            } else {
                out.add(run);
            }
        }
        return out;
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return GlyphSplitter.parametrizeNumbers(GlyphSplitter.stripGlyphChars(text));
    }

    static String withoutNumbers(String text) {
        return NUMBER_UNIT.matcher(text).replaceAll("");
    }

    private static final Pattern NUMBER_UNIT =
            Pattern.compile("\\{~\\}(?:[a-z]{1,3}(?![A-Za-z]))?");

    private static boolean hasLetter(String text) {
        return text.codePoints().anyMatch(Character::isLetter);
    }

    static String describe(Style style) {
        if (style == null) {
            return "inherit";
        }
        TextColor colour = style.getColor();
        String base = colour == null ? "inherit"
                : String.format("#%06X", colour.getValue() & 0xFFFFFF);
        return base + (style.isBold() ? "b" : "") + (style.isUnderlined() ? "u" : "");
    }

    public static boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    public static int size() {
        synchronized (ROWS) {
            return ROWS.size();
        }
    }

    public static JsonArray toJson() {
        JsonArray out = new JsonArray();
        synchronized (ROWS) {
            for (Row row : ROWS.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("src", row.src());
                JsonArray dst = new JsonArray();
                row.dst().forEach(dst::add);
                o.add("dst", dst);
                JsonArray runs = new JsonArray();
                for (Run run : row.runs()) {
                    JsonArray one = new JsonArray();
                    one.add(run.colour());
                    one.add(run.text());
                    runs.add(one);
                }
                o.add("runs", runs);
                JsonArray missed = new JsonArray();
                row.missed().forEach(missed::add);
                o.add("missed", missed);
                out.add(o);
            }
        }
        return out;
    }

    public static void load(JsonElement saved) {
        if (saved == null || !saved.isJsonArray()) {
            return;
        }
        synchronized (ROWS) {
            for (JsonElement el : saved.getAsJsonArray()) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    String src = o.get("src").getAsString();
                    List<String> dst = new ArrayList<>();
                    o.getAsJsonArray("dst").forEach(e -> dst.add(e.getAsString()));
                    List<Run> runs = new ArrayList<>();
                    for (JsonElement r : o.getAsJsonArray("runs")) {
                        JsonArray one = r.getAsJsonArray();
                        runs.add(new Run(one.get(0).getAsString(), one.get(1).getAsString()));
                    }
                    List<String> missed = new ArrayList<>();
                    o.getAsJsonArray("missed").forEach(e -> missed.add(e.getAsString()));
                    if (!src.isBlank() && ROWS.size() < LIMIT) {
                        ROWS.putIfAbsent(src, new Row(src, dst, runs, missed));
                    }
                } catch (RuntimeException e) {
                    ErrorDebug.note("ColourAudit#load", null, e);
                }
            }
        }
    }

    public static void clear() {
        SEEN.clear();
        synchronized (ROWS) {
            if (!ROWS.isEmpty()) {
                ROWS.clear();
                dirty = true;
            }
        }
    }
}
