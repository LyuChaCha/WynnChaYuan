package com.wynnchayuan.golden;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GoldenSamples {

    record Sample(String origin, StyledText text) {}

    private static final String CHAT_MARK = "[CHAT/INFO] ";
    private static final Pattern LOG_CODE =
            Pattern.compile("&(?=[0-9a-fk-orA-FK-OR]|\\{|#[0-9a-fA-F]{8})");
    private static final String LINE_MARK = "原文: ";
    private static final Pattern LINE_PART = Pattern.compile(
            "^\\s+font=\\S*\\[id=([^]]+)] px=\\S*\\s+斜([XV]) 粗([XV]) 底([XV]) text=(.*)$");
    private static final Pattern CODE_POINT = Pattern.compile("<U\\+([0-9A-F]{4,6})>");

    private GoldenSamples() {}

    static List<Sample> read(List<Path> files) throws IOException {
        List<Sample> out = new ArrayList<>();
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            String name = file.getFileName().toString();
            if (name.endsWith(".log")) {
                chat(name, lines, out);
            } else {
                lineDebug(name, lines, out);
            }
        }
        return out;
    }

    private static void chat(String name, List<String> lines, List<Sample> out) {
        int row = 0;
        for (String line : lines) {
            row++;
            int at = line.indexOf(CHAT_MARK);
            if (at < 0) {
                continue;
            }
            String coded = line.substring(at + CHAT_MARK.length());
            if (!coded.isBlank()) {
                String formatted = LOG_CODE.matcher(coded).replaceAll("§");
                out.add(new Sample(name + ":" + row, StyledText.fromString(formatted)));
            }
        }
    }

    private static void lineDebug(String name, List<String> lines, List<Sample> out) {
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).startsWith(LINE_MARK)) {
                continue;
            }
            MutableComponent root = Component.empty();
            int j = i + 1;
            for (; j < lines.size(); j++) {
                Matcher part = LINE_PART.matcher(lines.get(j));
                if (!part.matches()) {
                    break;
                }
                Style style = Style.EMPTY
                        .withFont(new FontDescription.Resource(Identifier.parse(part.group(1))))
                        .withItalic("V".equals(part.group(2)))
                        .withBold("V".equals(part.group(3)))
                        .withUnderlined("V".equals(part.group(4)));
                root.append(Component.literal(decode(part.group(5))).withStyle(style));
            }
            if (j > i + 1) {
                out.add(new Sample(name + ":" + (i + 1), StyledText.fromComponent(root)));
            }
            i = j - 1;
        }
    }

    private static String decode(String shown) {
        Matcher m = CODE_POINT.matcher(shown.replace('␠', ' '));
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(
                    Character.toString(Integer.parseInt(m.group(1), 16))));
        }
        m.appendTail(out);
        return out.toString();
    }
}
