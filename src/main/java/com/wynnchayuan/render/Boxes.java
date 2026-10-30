package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Boxes {

    private Boxes() {}

    public static void draw(GuiGraphics g, int x, int y, int w, int h) {
        draw(g, x, y, w, h, 1.0f);
    }

    static final int EDGE = 2;

    public static int[] intoScreen(int x, int y, int w, int h, int screenW, int screenH) {
        int maxX = Math.max(EDGE, screenW - w - EDGE);
        int maxY = Math.max(EDGE, screenH - h - EDGE);
        return new int[] {Math.max(EDGE, Math.min(maxX, x)), Math.max(EDGE, Math.min(maxY, y))};
    }

    public static void draw(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        int bg = Colors.fade(WynnChaYuan.config().backgroundARGB(), alpha);
        int border = Colors.fade(WynnChaYuan.config().accentARGB(), alpha);
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    public static List<Component> toLines(Component source) {
        if (source == null) {
            return List.of();
        }
        List<Style> styles = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        source.visit((style, text) -> {
            styles.add(style);
            texts.add(text);
            return Optional.empty();
        }, Style.EMPTY);

        List<Component> lines = new ArrayList<>();
        MutableComponent current = Component.empty();
        boolean hasContent = false;
        boolean[] glyphLine = glyphLines(texts, styles);
        int line = 0;

        for (int i = 0; i < texts.size(); i++) {
            Style style = styles.get(i);
            String text = texts.get(i);
            if (text.isEmpty() || SpaceOffset.isSpaceFont(style)) {
                continue;
            }
            String[] chunks = text.split("\n", -1);
            for (int c = 0; c < chunks.length; c++) {
                if (c > 0) {
                    if (hasContent) {
                        lines.add(current);
                    }
                    current = Component.empty();
                    hasContent = false;
                    line++;
                }
                String chunk = glyphLine[line] ? chunks[c] : dropOffsets(chunks[c]);
                if (!chunk.isEmpty()) {
                    current.append(Component.literal(chunk).withStyle(style));
                    if (hasReadable(chunk)) {
                        hasContent = true;
                    }
                }
            }
        }
        if (hasContent) {
            lines.add(current);
        }
        dump(source, texts, styles, lines);
        return lines;
    }

    private static boolean[] glyphLines(List<String> texts, List<Style> styles) {
        List<Boolean> seen = new ArrayList<>();
        boolean glyph = false;
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text.isEmpty() || SpaceOffset.isSpaceFont(styles.get(i))) {
                continue;
            }
            String[] chunks = text.split("\n", -1);
            for (int c = 0; c < chunks.length; c++) {
                if (c > 0) {
                    seen.add(glyph);
                    glyph = false;
                }
                if (chunks[c].codePoints().anyMatch(GlyphSplitter::isPrivateUse)) {
                    glyph = true;
                }
            }
        }
        seen.add(glyph);
        boolean[] out = new boolean[seen.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = seen.get(i);
        }
        return out;
    }

    static String dropOffsets(String text) {
        if (text.codePoints().noneMatch(Boxes::isOffset)) {
            return text;
        }
        if (text.codePoints().anyMatch(GlyphSplitter::isPrivateUse)) {
            return text;
        }
        StringBuilder kept = new StringBuilder(text.length());
        text.codePoints().filter(cp -> !isOffset(cp)).forEach(kept::appendCodePoint);
        return kept.toString();
    }

    private static boolean isOffset(int cp) {
        return GlyphSplitter.isGlyphCodePoint(cp) && !GlyphSplitter.isPrivateUse(cp);
    }

    private static boolean hasReadable(String text) {
        return text.codePoints().anyMatch(cp ->
                !Character.isWhitespace(cp) && !GlyphSplitter.isGlyphCodePoint(cp));
    }

    private static int dumped = 0;

    private static void dump(Component source, List<String> texts,
                             List<Style> styles, List<Component> lines) {
        if (dumped >= 3 || debugFile == null || !WynnChaYuan.config().debugDumps()) {
            return;
        }
        dumped++;
        StringBuilder sb = new StringBuilder();
        sb.append("=== 原始 ===").append(System.lineSeparator());
        sb.append("getString: ").append(source.getString()).append(System.lineSeparator());

        sb.append(System.lineSeparator())
          .append("=== visit 拆出來的片段 ===").append(System.lineSeparator());
        for (int i = 0; i < texts.size(); i++) {
            Style st = styles.get(i);
            sb.append(String.format("  [%d] font=%s color=%s len=%d text=%s%n",
                    i,
                    st.getFont() == null ? "(null)" : st.getFont().toString(),
                    st.getColor() == null ? "(null)" : st.getColor().toString(),
                    texts.get(i).length(),
                    texts.get(i).replace("\n", "\\n")));
        }

        sb.append(System.lineSeparator())
          .append("=== 整理後的行 ===").append(System.lineSeparator());
        sb.append("行數: ").append(lines.size()).append(System.lineSeparator());
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            sb.append(String.format("  [%d] len=%d %s%n",
                    i, line.getString().length(), line.getString()));
        }
        try {
            java.nio.file.Files.writeString(
                    debugFile.resolveSibling("overlay-debug-" + dumped + ".txt"),
                    sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }

    private static java.nio.file.Path debugFile;

    public static void init(java.nio.file.Path path) {
        debugFile = path;
    }
}
