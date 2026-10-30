package com.wynnchayuan.translate;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class NameplateProbe {

    private static final String FILE = "nameplate-miss.txt";

    private static final int LIMIT = 40;

    private static final int SEEN_LIMIT = 4096;

    private static Path file;
    private static int written;
    private static final java.util.Set<String> seen = new java.util.HashSet<>();
    private static final StringBuilder buffer = new StringBuilder();

    private NameplateProbe() {}

    public static void init(Path debugDir) {
        file = debugDir.resolve(FILE);
        buffer.append("# WynnChaYuan v").append(WynnChaYuan.version())
              .append("　多行名牌：名字那一列自己翻得出來，整塊卻沒翻（或名字翻得不一樣）")
              .append(System.lineSeparator())
              .append("# 每一段：字型、顏色、文字（看不見的碼位寫成 U+XXXX，換行寫成 ⏎）")
              .append(System.lineSeparator()).append(System.lineSeparator());
    }

    public static void check(String path, StyledText label, Component result,
                             TranslationStore store) {
        if (file == null || label == null || store == null || written >= LIMIT
                || !WynnChaYuan.config().debugDumps()) {
            return;
        }
        try {
            String plain = label.getStringWithoutFormatting();
            if (plain.indexOf('\n') < 0) {
                return;
            }
            String template = LineParts.of(label).template();
            synchronized (seen) {
                if (seen.size() >= SEEN_LIMIT || !seen.add(path + '\0' + template)) {
                    return;
                }
            }
            StyledText[] rows = label.split("\n");
            if (rows.length == 0) {
                return;
            }
            Component alone = LineTranslator.translateLabel(rows[0], store);
            if (alone == null) {
                return;
            }
            String name = alone.getString().strip();
            String got = result == null ? null : result.getString().split("\n", -1)[0].strip();
            if (got != null && got.equals(name)) {
                return;
            }
            synchronized (buffer) {
                if (written >= LIMIT) {
                    return;
                }
                written++;
                String nl = System.lineSeparator();
                buffer.append("=== ").append(written).append(" · ").append(path)
                      .append(result == null ? " · 整塊沒翻" : " · 名字翻得不一樣").append(" ===").append(nl)
                      .append("模板: ").append(template.replace("\n", "⏎")).append(nl)
                      .append("第一列模板: ").append(LineParts.of(rows[0]).template()).append(nl)
                      .append("第一列單獨的譯文: ").append(name).append(nl)
                      .append("整塊的譯文: ")
                      .append(result == null ? "(null)" : result.getString().replace("\n", "⏎"))
                      .append(nl)
                      .append("列數（照 StyledText 切）: ").append(rows.length).append(nl);
                dump(label.getComponent());
                buffer.append(nl);
                Files.writeString(file, buffer.toString(), StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
        }
    }

    private static void dump(Component component) {
        int[] index = {0};
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                buffer.append(String.format("  [%2d] font=%-32s color=%-8s %s text=%s%n",
                        index[0]++,
                        style.getFont() == null ? "(null)" : style.getFont().toString(),
                        style.getColor() == null ? "(null)" : style.getColor().serialize(),
                        (style.isBold() ? "粗" : "") + (style.isItalic() ? "斜" : ""),
                        describe(text)));
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    private static String describe(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp == '\n') {
                sb.append('⏎');
            } else if (cp == ' ') {
                sb.append('␠');
            } else if (Character.isISOControl(cp) || cp > 0xFFFF
                    || Character.getType(cp) == Character.PRIVATE_USE
                    || Character.getType(cp) == Character.UNASSIGNED
                    || Character.getType(cp) == Character.FORMAT) {
                sb.append(String.format("<U+%04X>", cp));
            } else {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString();
    }
}
