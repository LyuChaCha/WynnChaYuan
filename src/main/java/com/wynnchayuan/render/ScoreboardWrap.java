package com.wynnchayuan.render;

import com.wynnchayuan.translate.ErrorDebug;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Wynntils ScoreboardOverlay 照英文算框寬，用反射換欄位
public final class ScoreboardWrap {

    private ScoreboardWrap() {
    }

    // 一列 9px，與 Wynntils 算 contentHeight 相同
    private static final float LINE_HEIGHT = 9f;

    private static final float MIN_WIDTH = 60f;

    private static final String OVERLAY = "com.wynntils.overlays.ScoreboardOverlay";

    private static Field lines;
    private static Field longest;
    private static Field height;
    private static boolean broken;

    private static List<StyledText> lastTranslated = List.of();
    private static float lastWidth = -1;
    private static float lastRoom = -1;
    private static List<StyledText> lastWrapped = List.of();
    private static float lastLongest;

    private static Object swapped;
    private static Object savedLines;
    private static float savedLongest;
    private static float savedHeight;

    @SuppressWarnings("unchecked")
    public static void begin(Object overlay) {
        swapped = null;
        if (broken || overlay == null || !fields(overlay)) {
            return;
        }
        try {
            List<StyledText> source = (List<StyledText>) lines.get(overlay);
            float width = longest.getFloat(overlay);
            float room = Math.max(width, roomOf(overlay));
            if (source == null || source.isEmpty()) {
                return;
            }
            List<StyledText> translated = new ArrayList<>(source.size());
            for (StyledText row : source) {
                translated.add(WynntilsText.screenText(row));
            }
            if (!sameRows(translated, lastTranslated) || width != lastWidth || room != lastRoom) {
                StringSplitter splitter = Minecraft.getInstance().font.getSplitter();
                List<StyledText> wrapped = new ArrayList<>();
                float widest = 0;
                for (StyledText row : translated) {
                    for (StyledText piece : wrap(row, room, splitter)) {
                        wrapped.add(piece);
                        widest = Math.max(widest, splitter.stringWidth(piece.getComponent()));
                    }
                }
                lastTranslated = translated;
                lastWidth = width;
                lastRoom = room;
                lastWrapped = wrapped;
                lastLongest = Math.max(width, widest);
            }
            savedLines = source;
            savedLongest = width;
            savedHeight = height.getFloat(overlay);
            lines.set(overlay, lastWrapped);
            longest.setFloat(overlay, lastLongest);
            height.setFloat(overlay, lastWrapped.size() * LINE_HEIGHT);
            swapped = overlay;
            WynntilsText.holdRawText(true);
        } catch (Throwable t) {
            ErrorDebug.note("ScoreboardWrap#begin", null, t);
            broken = true;
        }
    }

    public static void end(Object overlay) {
        WynntilsText.holdRawText(false);
        if (swapped == null || swapped != overlay) {
            return;
        }
        try {
            lines.set(overlay, savedLines);
            longest.setFloat(overlay, savedLongest);
            height.setFloat(overlay, savedHeight);
        } catch (Throwable t) {
            ErrorDebug.note("ScoreboardWrap#end", null, t);
            broken = true;
        }
        swapped = null;
    }

    static List<StyledText> wrap(StyledText row, float width, StringSplitter splitter) {
        Component whole = row.getComponent();
        float limit = Math.max(width, MIN_WIDTH);
        if (splitter.stringWidth(whole) <= limit + 0.5f) {
            return List.of(row);
        }
        String plain = row.getStringWithoutFormatting();
        String indent = plain.startsWith("- ") || plain.startsWith("• ") ? "  " : "";
        int paren = plain.endsWith(")") ? plain.lastIndexOf(" (") : -1;
        if (paren > 0) {
            List<StyledText> pair = List.of(
                    StyledText.fromComponent(slice(whole, 0, paren)),
                    StyledText.fromComponent(Component.empty()
                            .append(Component.literal(indent))
                            .append(slice(whole, paren + 1, plain.length()))));
            if (pair.stream().allMatch(p -> splitter.stringWidth(p.getComponent()) <= limit + 0.5f)) {
                return pair;
            }
        }
        List<FormattedText> parts = splitter.splitLines(whole, (int) limit, Style.EMPTY);
        if (parts.size() < 2) {
            return List.of(row);
        }
        List<StyledText> out = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            MutableComponent line = Component.empty();
            boolean[] leading = {i > 0};
            parts.get(i).visit((style, text) -> {
                String piece = text;
                if (leading[0]) {
                    piece = piece.stripLeading();
                    if (piece.isEmpty()) {
                        return Optional.empty();
                    }
                    leading[0] = false;
                    line.append(Component.literal(indent).withStyle(style));
                }
                line.append(Component.literal(piece).withStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            out.add(StyledText.fromComponent(line));
        }
        return out;
    }

    private static Component slice(Component whole, int from, int to) {
        MutableComponent out = Component.empty();
        int[] at = {0};
        whole.visit((style, text) -> {
            int start = at[0];
            int end = start + text.length();
            at[0] = end;
            int a = Math.max(from, start);
            int b = Math.min(to, end);
            if (a < b) {
                out.append(Component.literal(text.substring(a - start, b - start)).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static java.lang.reflect.Method sizeGetter;
    private static Field scaleField;
    private static java.lang.reflect.Method scaleGet;

    private static float roomOf(Object overlay) {
        try {
            if (sizeGetter == null) {
                Class<?> type = overlay.getClass();
                while (type != null && !OVERLAY.equals(type.getName())) {
                    type = type.getSuperclass();
                }
                if (type == null) {
                    return 0;
                }
                Field scale = type.getDeclaredField("fontScale");
                scale.setAccessible(true);
                scaleField = scale;
                scaleGet = scale.getType().getMethod("get");
                sizeGetter = type.getMethod("getWidth");
            }
            float overlayWidth = (Float) sizeGetter.invoke(overlay);
            float scale = (Float) scaleGet.invoke(scaleField.get(overlay));
            return scale > 0 ? overlayWidth / scale : overlayWidth;
        } catch (Throwable t) {
            ErrorDebug.note("ScoreboardWrap#roomOf", null, t);
            return 0;
        }
    }

    private static boolean sameRows(List<StyledText> a, List<StyledText> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean fields(Object overlay) {
        if (lines != null) {
            return true;
        }
        try {
            Class<?> type = overlay.getClass();
            while (type != null && !OVERLAY.equals(type.getName())) {
                type = type.getSuperclass();
            }
            if (type == null) {
                broken = true;
                return false;
            }
            Field l = type.getDeclaredField("linesToRender");
            Field w = type.getDeclaredField("longestLineWidth");
            Field h = type.getDeclaredField("contentHeight");
            l.setAccessible(true);
            w.setAccessible(true);
            h.setAccessible(true);
            lines = l;
            longest = w;
            height = h;
            return true;
        } catch (Throwable t) {
            ErrorDebug.note("ScoreboardWrap#fields", null, t);
            broken = true;
            return false;
        }
    }
}
