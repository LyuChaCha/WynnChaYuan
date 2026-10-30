package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.wynnchayuan.translate.ErrorDebug;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OverflowAudit {

    private OverflowAudit() {}

    static final int LIMIT = 300;

    public static final String DIALOGUE = "dialogue";
    public static final String TOOLTIP = "tooltip";

    public static final String ROWS_UNIT = "rows";
    public static final String PX_UNIT = "px";

    static final int MIN_PX = 4;

    public record Row(String where, String src, String dst, int room, int need, String unit) {}

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();
    private static volatile boolean dirty;

    public static void note(String where, String src, String dst, int room, int need,
                            String unit) {
        try {
            if (need <= room && size() == 0) {
                return;
            }
            String id = id(where, src);
            Row row = audit(where, src, dst, room, need, unit);
            synchronized (ROWS) {
                if (row == null) {
                    if (id != null && !ROWS.isEmpty() && ROWS.remove(id) != null) {
                        dirty = true;
                    }
                    return;
                }
                Row was = ROWS.get(id);
                if (row.equals(was)) {
                    return;
                }
                if (was == null && ROWS.size() >= LIMIT) {
                    return;
                }
                ROWS.put(id, row);
            }
            dirty = true;
        } catch (Throwable t) {
            ErrorDebug.note("OverflowAudit#note", null, t);
        }
    }

    static Row audit(String where, String src, String dst, int room, int need, String unit) {
        if (where == null || src == null || dst == null || unit == null) {
            return null;
        }
        String key = clean(src);
        if (key.isBlank() || dst.isBlank() || room <= 0 || need <= room) {
            return null;
        }
        if (PX_UNIT.equals(unit) && need - room < MIN_PX) {
            return null;
        }
        if (PlayerDataFilter.carriesPlayerData(key)) {
            return null;
        }
        return new Row(where, key, clean(dst), room, need, unit);
    }

    private static String id(String where, String src) {
        return where == null || src == null ? null : where + '\u0000' + clean(src);
    }

    private static String clean(String text) {
        return GlyphSplitter.parametrizeNumbers(GlyphSplitter.stripGlyphChars(text)).strip();
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
                o.addProperty("where", row.where());
                o.addProperty("src", row.src());
                o.addProperty("dst", row.dst());
                o.addProperty("room", row.room());
                o.addProperty("need", row.need());
                o.addProperty("unit", row.unit());
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
                    Row row = new Row(o.get("where").getAsString(), o.get("src").getAsString(),
                            o.get("dst").getAsString(), o.get("room").getAsInt(),
                            o.get("need").getAsInt(), o.get("unit").getAsString());
                    if (!row.src().isBlank() && ROWS.size() < LIMIT) {
                        ROWS.putIfAbsent(row.where() + '\u0000' + row.src(), row);
                    }
                } catch (RuntimeException e) {
                    ErrorDebug.note("OverflowAudit#load", null, e);
                }
            }
        }
    }

    public static void clear() {
        synchronized (ROWS) {
            if (!ROWS.isEmpty()) {
                ROWS.clear();
                dirty = true;
            }
        }
    }
}
