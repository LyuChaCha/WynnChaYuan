package com.wynnchayuan.translate;

import com.wynnchayuan.ModLog;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;

public final class LayoutDebug {

    private static final int LIMIT = 40;

    private static Path file;
    private static int written = 0;
    private static final Set<String> seen = new HashSet<>();


    private LayoutDebug() {}

    public static void init(Path path) {
        file = path;
        flowedSeen = 0;
        drawnWritten = 0;
        drawnSeen.clear();
        written = 0;
        failures = 0;
        seen.clear();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    "# 置中判斷。每一行的縮排、內容寬度，以及置中時該有的縮排。"
                    + System.lineSeparator()
                    + "# 三個數字對得起來卻判成靠左，就是分段切錯了。"
                    + System.lineSeparator()
                    + "# 「· 看過」是每一份 tooltip 的足跡；完全沒有這種行，"
                    + "就代表判斷根本沒被呼叫到。"
                    + System.lineSeparator() + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (Throwable t) {
            file = null;
        }
    }

    public static void record(List<Component> lines, boolean[] centered) {
        if (file == null || lines == null || lines.isEmpty()) {
            return;
        }
        String key;
        try {
            key = lines.get(0).getString() + "/" + lines.size();
        } catch (Throwable t) {
            return;
        }
        boolean detail = written < LIMIT && seen.add(key);
        StringBuilder sb = new StringBuilder();
        sb.append("· 看過 ").append(oneLine(key)).append(System.lineSeparator());
        if (detail) {
            written++;
            try {
                String explained = BlockLayout.explain(lines, centered);
                sb.append("=== ").append(written).append(" · ").append(oneLine(key))
                  .append(" ===").append(System.lineSeparator())
                  .append(explained).append(System.lineSeparator());
                ModLog.info("版面判斷" + System.lineSeparator()
                        + explained);
            } catch (Throwable t) {
                failures++;
                sb.append("=== ").append(written).append(" · ").append(oneLine(key))
                  .append(" ===").append(System.lineSeparator())
                  .append("  這一份算不出來：").append(t)
                  .append(System.lineSeparator()).append(System.lineSeparator());
            }
        }
        try {
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            failures++;
        }
    }

    private static final int DRAWN_LIMIT = 60;

    private static final int DRAWN_SEEN_LIMIT = 80;

    private static int drawnWritten = 0;

    private static final Set<String> drawnSeen = new HashSet<>();

    public static void drawn(String stage, List<Component> original, List<Component> made,
                             ToIntFunction<Component> width) {
        if (file == null || original == null || made == null || width == null
                || original.isEmpty()) {
            return;
        }
        try {
            String key = stage + "/" + original.get(0).getString() + "/" + original.size();
            boolean fresh = drawnSeen.size() < DRAWN_SEEN_LIMIT && drawnSeen.add(key);
            if (!fresh) {
                return;
            }
            String text = compareLeads(stage, original, made, width);
            if (text == null) {
                {
                    Files.writeString(file,
                            "· 左緣對照（" + stage + "）" + oneLine(original.get(0).getString())
                            + "：" + Math.min(original.size(), made.size())
                            + " 行都跟原文對得上" + System.lineSeparator(),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
                return;
            }
            if (drawnWritten >= DRAWN_LIMIT) {
                return;
            }
            drawnWritten++;
            Files.writeString(file, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            failures++;
        }
    }

    private static String compareLeads(String stage, List<Component> original,
                                       List<Component> made,
                                       ToIntFunction<Component> width) {
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        int n = Math.min(original.size(), made.size());
        int shift = original.size() - made.size();
        for (int i = 0; i < n; i++) {
            int a = leadOf(original.get(i), width);
            int b = leadOf(made.get(i), width);
            if (a == Integer.MIN_VALUE || b == Integer.MIN_VALUE || a == b) {
                continue;
            }
            int j = i - shift;
            if (shift != 0 && j >= 0 && j < made.size()
                    && leadOf(made.get(j), width) == a) {
                continue;
            }
            any = true;
            sb.append(String.format("  [%2d] 原文左緣 %4d  譯文左緣 %4d  差 %+d  %s%n",
                    i, a, b, b - a, oneLine(made.get(i).getString())));
        }
        if (!any) {
            return null;
        }
        return "=== 左緣對照（" + stage + "）· " + oneLine(original.get(0).getString())
                + "\u3000原文 " + original.size() + " 行、譯文 " + made.size() + " 行 ==="
                + System.lineSeparator() + sb + System.lineSeparator();
    }

    private static int leadOf(Component line,
                              ToIntFunction<Component> width) {
        int[] x = {0};
        int[] found = {Integer.MIN_VALUE};
        line.visit((style, text) -> {
            if (found[0] != Integer.MIN_VALUE) {
                return Optional.empty();
            }
            boolean glyphOnly = !text.isBlank() && text.codePoints().allMatch(
                    cp -> GlyphSplitter.isGlyphCodePoint(cp));
            if (text.isBlank() || glyphOnly) {
                x[0] += width.applyAsInt(
                        Component.literal(text).setStyle(style));
                return Optional.empty();
            }
            found[0] = x[0];
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static int failures = 0;

    private static String oneLine(String text) {
        String flat = text.replace('\n', ' ').strip();
        return flat.length() > 30 ? flat.substring(0, 30) + "…" : flat;
    }

    private static int flowedSeen = 0;

    private static final int FLOWED_LIMIT = 12;

    public static void flowed(List<StyledText> run, String label, Style chosen) {
        if (file == null || run == null || run.isEmpty() || flowedSeen >= FLOWED_LIMIT) {
            return;
        }
        flowedSeen++;
        try {
            writeFlowed(run, label, chosen);
        } catch (Throwable t) {
            failures++;
        }
    }

    private static void writeFlowed(List<StyledText> run, String label, Style chosen)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 跨行查表 ===").append(System.lineSeparator());
        sb.append("  譯名：").append(label == null ? "（整段命中，沒有拆名稱）" : label)
          .append(System.lineSeparator());
        sb.append("  挑中的樣式：").append(describe(chosen)).append(System.lineSeparator());
        sb.append("  原文第一行的色段：").append(System.lineSeparator());
        int i = 0;
        for (StyledTextPart part : run.get(0)) {
            String raw = part.getString(null, StyleType.NONE);
            PartStyle ps = part.getPartStyle();
            sb.append("    [").append(i++).append("] ")
              .append(describe(ps == null ? null : ps.getStyle()))
              .append("  「").append(raw).append("」").append(System.lineSeparator());
        }
        for (StyledText line : run) {
            sb.append("  原文：").append(line.getString()).append(System.lineSeparator());
        }
        ModLog.info("跨行查表" + System.lineSeparator() + sb);
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String describe(Style style) {
        if (style == null) {
            return "（無）";
        }
        TextColor colour = style.getColor();
        return colour == null ? "繼承"
                : String.format("#%06X", colour.getValue() & 0xFFFFFF);
    }
}
