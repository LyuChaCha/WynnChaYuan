package com.wynnchayuan.translate;

import com.wynnchayuan.WynnChaYuan;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class LineDebug {

    private static final int LIMIT = 120;

    private static Path file;
    private static int written = 0;
    private static int layouts = 0;
    private static int held = 0;

    private static final int HELD_QUOTA = 10;

    private static final String HELD = "(守住)";

    private static final int LAYOUT_LIMIT = 8;
    private static StringBuilder buffer = new StringBuilder();

    private static final Set<String> seen = new HashSet<>();

    private LineDebug() {}

    public static void init(Path path) {
        file = path;
        buffer.append("# WynnChaYuan v").append(WynnChaYuan.version())
              .append("　收集語料：")
              .append(WynnChaYuan.config().collect() ? "開" : "關")
              .append(System.lineSeparator()).append(System.lineSeparator());
    }

    public static void pieces(String header, String detail) {
        if (file == null || written >= LIMIT || !WynnChaYuan.config().debugDumps()) {
            return;
        }
        boolean fine = detail.contains(HELD);
        if (fine && held >= HELD_QUOTA) {
            return;
        }
        if (!seen.add(header)) {
            return;
        }
        if (fine) {
            held++;
        }
        written++;
        buffer.append("=== ").append(written).append(" · ").append(header)
              .append(" ===").append(System.lineSeparator())
              .append(detail).append(System.lineSeparator());
        flush();
    }

    public static void record(StyledText original, Component translated) {
        if (file == null || written >= LIMIT || translated == null
                || !WynnChaYuan.config().debugDumps()
                || !seen.add(original.getStringWithoutFormatting())) {
            return;
        }
        written++;
        buffer.append("=== ").append(written).append(" ===")
              .append(System.lineSeparator());
        dump("原文", original.getComponent());
        dump("譯文", translated);
        buffer.append(System.lineSeparator());
        flush();
    }

    private static void dump(String label, Component component) {
        buffer.append(label).append(": ").append(component.getString())
              .append(System.lineSeparator());
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                buffer.append(String.format("   font=%-28s px=%-6s %s text=%s%n",
                        style.getFont() == null ? "(null)" : style.getFont().toString(),
                        SpaceOffset.isSpaceFont(style)
                                ? String.valueOf(SpaceOffset.decode(text)) : "-",
                        decorations(style),
                        describe(text)));
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    private static String decorations(Style style) {
        return "斜" + (style.isItalic() ? "V" : "X")
                + " 粗" + (style.isBold() ? "V" : "X")
                + " 底" + (style.isUnderlined() ? "V" : "X");
    }

    private static String describe(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp == ' ') {
                sb.append('␠');
            } else if (Character.isISOControl(cp) || cp > 0xFFFF
                    || Character.getType(cp) == Character.PRIVATE_USE
                    || Character.getType(cp) == Character.UNASSIGNED) {
                sb.append(String.format("<U+%04X>", cp));
            } else {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString();
    }

    private static void flush() {
        try {
            Files.writeString(file, buffer.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }
}
