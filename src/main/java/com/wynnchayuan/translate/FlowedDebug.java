package com.wynnchayuan.translate;

import com.wynnchayuan.ModLog;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FlowedDebug {

    private static final String FILE = "majorid-debug.txt";

    private static final int LIMIT = 30;

    private static Path file;
    private static int seen = 0;

    private static final Set<String> already = ConcurrentHashMap.newKeySet();

    private FlowedDebug() {}

    public static void init(Path configDir) {
        file = configDir.resolve(FILE);
        seen = 0;
        accentSeen = 0;
        rowSeen = 0;
        missed = 0;
        prose = 0;
        labelled = 0;
        choicesSeen = 0;
        already.clear();
        try {
            Files.createDirectories(configDir);
            Files.writeString(file,
                    "# 跨行查表的現場（Major ID、技能敘述）。" + System.lineSeparator()
                    + "# 每一次跨行翻譯都會記一筆，含原文每一段的顏色與程式挑中的樣式。"
                    + System.lineSeparator()
                    + "# 檔案只有這兩行，就代表跨行查表<b>一次都沒被呼叫到</b>。"
                    + System.lineSeparator() + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (Throwable t) {
            file = null;
        }
    }

    public static void note(List<StyledText> run, String label, Style chosen,
                            Style blockStyle) {
        if (file == null || run == null || run.isEmpty() || seen >= LIMIT) {
            return;
        }
        StringBuilder id = new StringBuilder("note");
        for (StyledText one : run) {
            id.append(one.getString());
        }
        if (!already.add(id.toString())) {
            return;
        }
        seen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(seen).append(" ===").append(System.lineSeparator());
            sb.append("  譯名：")
              .append(label == null ? "（整段命中，沒有拆出名稱）" : label)
              .append(System.lineSeparator());
            sb.append("  譯名的樣式：").append(describe(chosen))
              .append(label == null ? "（整段命中，這一欄沒用到）" : "")
              .append(System.lineSeparator());
            sb.append("  整段內文的底色：").append(describe(blockStyle))
              .append(System.lineSeparator());
            sb.append("  原文第一行的色段：").append(System.lineSeparator());
            int i = 0;
            for (StyledTextPart part : run.get(0)) {
                PartStyle ps = part.getPartStyle();
                sb.append("    [").append(i++).append("] ")
                  .append(describe(ps == null ? null : ps.getStyle()))
                  .append("  「").append(part.getString(null, StyleType.NONE))
                  .append("」").append(System.lineSeparator());
            }
            for (StyledText line : run) {
                sb.append("  原文：").append(line.getString())
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            ModLog.info("跨行查表 #" + seen);
        } catch (Throwable t) {
        }
    }

    public static void accents(List<String> texts, List<Style> styles,
                               boolean[] used, String[] translated) {
        if (file == null || texts == null || texts.isEmpty()
                || accentSeen >= ACCENT_LIMIT) {
            return;
        }
        StringBuilder id = new StringBuilder("accents");
        for (String line : translated) {
            id.append(line);
        }
        if (!already.add(id.toString())) {
            return;
        }
        accentSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 重點段 ").append(accentSeen).append(" ===")
              .append(System.lineSeparator());
            for (String line : translated) {
                sb.append("  譯文：").append(line).append(System.lineSeparator());
            }
            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);
                boolean hit = used != null && i < used.length && used[i];
                boolean present = false;
                for (String line : translated) {
                    present |= line.contains(text);
                }
                sb.append("    [").append(i).append("] ")
                  .append(describe(i < styles.size() ? styles.get(i) : null))
                  .append("  「").append(text).append("」")
                  .append(hit ? "  已貼上"
                              : (present ? "  ★在譯文裡卻沒貼上" : "  譯文裡找不到"))
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
        }
    }

    public static void glyphs(List<LineParts.Piece> pool, String[] translated) {
        if (file == null || pool == null || pool.isEmpty()
                || glyphSeen >= GLYPH_LIMIT) {
            return;
        }
        StringBuilder id = new StringBuilder("glyphs");
        for (String line : translated) {
            id.append(line);
        }
        if (!already.add(id.toString())) {
            return;
        }
        boolean interesting = false;
        for (LineParts.Piece piece : pool) {
            interesting |= piece.text().codePoints().anyMatch(FlowedDebug::isOffset);
        }
        if (!interesting) {
            return;
        }
        glyphSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 填回去的符號 ").append(glyphSeen).append(" ===")
              .append(System.lineSeparator());
            for (String line : translated) {
                sb.append("  譯文：").append(line).append(System.lineSeparator());
            }
            for (int i = 0; i < pool.size(); i++) {
                LineParts.Piece piece = pool.get(i);
                sb.append("    [").append(i).append("] ");
                piece.text().codePoints().forEach(cp -> sb
                        .append(String.format("U+%04X", cp))
                        .append(isOffset(cp) ? "(位移)" : "(圖示)")
                        .append(' '));
                sb.append(" font=")
                  .append(piece.style() == null ? "-" : piece.style().getFont())
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
        }
    }

    public static void rows(String id, String body) {
        if (file == null || body == null || rowSeen >= ROW_LIMIT
                || !already.add("rows" + id)) {
            return;
        }
        rowSeen++;
        try {
            Files.writeString(file,
                    "=== 逐行對齊 " + rowSeen + " ===" + System.lineSeparator()
                    + body + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
        }
    }

    public static void palette(List<Style> colours, List<LineParts.Piece> runs,
                               String[] translated) {
        if (file == null || colours == null || colours.size() < 2
                || paletteSeen >= PALETTE_LIMIT) {
            return;
        }
        StringBuilder id = new StringBuilder("palette");
        for (String line : translated) {
            id.append(line);
        }
        if (!already.add(id.toString())) {
            return;
        }
        paletteSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 可用的顏色 ").append(paletteSeen).append(" ===")
              .append(System.lineSeparator());
            for (String line : translated) {
                sb.append("  譯文：").append(line).append(System.lineSeparator());
            }
            for (int i = 0; i < colours.size(); i++) {
                sb.append("    {c").append(i + 1).append("}  ")
                  .append(String.format("%-22s", describe(colours.get(i))))
                  .append("原文：").append(sample(colours.get(i), runs))
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
        }
    }

    private static String sample(Style colour, List<LineParts.Piece> runs) {
        if (runs == null) {
            return "（不明）";
        }
        for (LineParts.Piece run : runs) {
            Style style = run.style() == null ? Style.EMPTY : run.style();
            if (!style.equals(colour)) {
                continue;
            }
            String text = run.text().strip();
            return text.length() > SAMPLE_LENGTH
                    ? "「" + text.substring(0, SAMPLE_LENGTH) + "…」" : "「" + text + "」";
        }
        return "（找不到）";
    }

    private static final int SAMPLE_LENGTH = 28;

    private static int paletteSeen = 0;

    private static final int PALETTE_LIMIT = 250;

    public static void chatRows(String id, String body, String miss) {
        if (file == null || chatSeen >= CHAT_ROW_LIMIT
                || !already.add("chatRows" + id)) {
            return;
        }
        chatSeen++;
        try {
            Files.writeString(file,
                    "=== 聊天對齊 " + chatSeen + " ==="
                    + (miss == null ? "" : "（沒翻到：" + miss + "）")
                    + System.lineSeparator()
                    + "  原文：" + id.replace("\n", " ⏎ ") + System.lineSeparator()
                    + (body == null ? "" : body) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
        }
    }

    private static int chatSeen = 0;

    private static final int CHAT_ROW_LIMIT = 20;

    private static int rowSeen = 0;

    private static final int ROW_LIMIT = 40;

    private static boolean isOffset(int cp) {
        return GlyphSplitter.isGlyphCodePoint(cp)
                && !GlyphSplitter.isPrivateUse(cp);
    }

    private static final int GLYPH_LIMIT = 12;

    private static int glyphSeen = 0;

    private static final int ACCENT_LIMIT = 20;

    private static int accentSeen = 0;

    private static String describe(Style style) {
        if (style == null) {
            return "（無樣式）";
        }
        TextColor colour = style.getColor();
        String base = colour == null ? "繼承"
                : String.format("#%06X", colour.getValue() & 0xFFFFFF);
        return base + (style.isUnderlined() ? " 底線" : "")
                + (style.isBold() ? " 粗體" : "");
    }

    private static final int MISS_LIMIT = 24;

    private static final int PROSE_MISS_LIMIT = 24;

    private static final int LABELLED_MISS_LIMIT = 12;

    private static int missed = 0;
    private static int prose = 0;
    private static int labelled = 0;

    private static boolean looksLikeProse(String template) {
        String flat = TranslationStore.normalise(template);
        return flat.contains(". ") || flat.contains(": ");
    }

    private static boolean looksLabelled(String template) {
        int end = template.indexOf('\n');
        String first = end < 0 ? template : template.substring(0, end);
        int colon = first.indexOf(": ");
        return colon > 0 && colon <= 40;
    }

    private static void explain(StringBuilder sb, String template,
                                TranslationStore store) {
        String nl = System.lineSeparator();
        int end = template.indexOf('\n');
        String first = end < 0 ? template : template.substring(0, end);
        int colon = first.indexOf(": ");
        if (colon <= 0) {
            return;
        }
        String head = template.substring(0, colon);
        String rest = template.substring(colon + 2);
        sb.append("  -- 拆成「名稱：說明」之後 --").append(nl);
        sb.append("     名稱＝[").append(head).append(']').append(nl);
        sb.append("     名稱查表＝")
          .append(TemplateLookup.lookup(head, store, false)).append(nl);
        sb.append("     說明的鍵＝[").append(TranslationStore.normalise(rest))
          .append(']').append(nl);
        sb.append("     說明查表＝").append(store.lookupFlat(rest)).append(nl);
        sb.append("     整段攤平查表＝").append(store.lookupFlat(template)).append(nl);
    }

    public static void miss(String template, TranslationStore store) {
        if (file == null || template == null) {
            return;
        }
        boolean label = looksLabelled(template);
        boolean sentence = !label && looksLikeProse(template);
        int limit = label ? LABELLED_MISS_LIMIT
                : sentence ? PROSE_MISS_LIMIT : MISS_LIMIT;
        int used = label ? labelled : sentence ? prose : missed;
        if (used >= limit) {
            return;
        }
        if (!already.add("miss" + template)) {
            return;
        }
        int nth = label ? ++labelled : sentence ? ++prose : ++missed;
        String kind = label ? "（名稱：說明）" : sentence ? "（像句子）" : "（詞條）";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 查不到 ").append(kind)
              .append(nth).append(" ===")
              .append(System.lineSeparator());
            sb.append("  攤平後的鍵：").append(TranslationStore.normalise(template))
              .append(System.lineSeparator());
            for (String line : template.split("\\R", -1)) {
                sb.append("    原文：").append(line).append(System.lineSeparator());
            }
            if (label && store != null) {
                explain(sb, template, store);
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
        }
    }

    private static final int CHOICE_LIMIT = 6;

    private static int choicesSeen = 0;

    public static void noteChoices(String text) {
        if (file == null || text == null || choicesSeen >= CHOICE_LIMIT
                || !already.add("choices" + text)) {
            return;
        }
        choicesSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 有選項的對話 ").append(choicesSeen).append(" ===")
              .append(System.lineSeparator());
            int i = 0;
            for (String line : text.split("\\R", -1)) {
                sb.append("  [").append(i++).append("] ").append(line)
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
        }
    }
}
