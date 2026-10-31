package com.wynnchayuan.render;

import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.DialogueSpeaker;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.SelfNames;
import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.util.Optional;

public final class DialogueStartLog {

    private static final int LIMIT = 600;
    private static final String PROGRESS = "hud/dialogue/effect/progress";
    private static final String BODY = "/body_";

    private static Path file;
    private static int written;

    private static String text = "";
    private static long start;
    private static long lastFrame;
    private static int frames;
    private static int firstProgress = -1;
    private static int lastProgress = -1;
    private static String speaker;
    private static String quest;
    private static String style;
    private static String portrait;
    private static String outcome = "";

    private DialogueStartLog() {}

    public static void init(Path debugDir) {
        file = debugDir.resolve("dialogue-start.txt");
        framesFile = debugDir.resolve("dialogue-frames.txt");
        written = 0;
        frameLines = 0;
        try {
            Files.deleteIfExists(file);
            Files.deleteIfExists(framesFile);
        } catch (Exception ignored) {
        }
    }

    private static Path framesFile;
    private static int frameLines;
    private static final int FRAME_LIMIT = 6000;
    private static final long T0 = System.currentTimeMillis();
    private static String lastRow = "";

    static void frame(String mode, int raw, int shown) {
        frameRow(mode + " | " + raw + " | " + shown + " | -");
    }

    static void panelFrame(int raw, int chars, int rows) {
        frameRow("小框 | " + raw + " | " + chars + " | " + rows);
    }

    private static void frameRow(String row) {
        if (framesFile == null || frameLines >= FRAME_LIMIT || row.equals(lastRow)) {
            return;
        }
        lastRow = row;
        try {
            Files.writeString(framesFile, (System.currentTimeMillis() - T0) + " | " + row + '\n',
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            frameLines++;
        } catch (Exception ignored) {
            framesFile = null;
        }
    }

    public static void frame(Component message) {
        if (file == null || message == null || written >= LIMIT) {
            return;
        }
        StringBuilder body = new StringBuilder();
        int[] progress = {-1};
        String[] look = {null, null};
        message.visit((s, part) -> {
            String font = fontOf(s);
            if (font.contains(BODY)) {
                body.append(part);
            } else if (font.contains(PROGRESS)) {
                part.codePoints().filter(cp -> cp >= 0xE000 && cp <= 0xE0FF)
                        .forEach(cp -> progress[0] = cp - 0xE000);
            } else if (font.contains("hud/dialogue/style/") && look[0] == null) {
                String rest = font.substring(font.indexOf("hud/dialogue/style/") + 19);
                look[0] = rest.contains("/") ? rest.substring(0, rest.indexOf('/')) : rest;
            } else if (font.contains("hud/dialogue/portrait/") && look[1] == null) {
                look[1] = font.substring(font.indexOf("hud/dialogue/portrait/") + 22)
                        .replace("]", "");
            }
            return Optional.empty();
        }, Style.EMPTY);
        String now = letters(body.toString());
        if (now.isEmpty()) {
            return;
        }
        long t = System.currentTimeMillis();
        if (text.isEmpty() || !now.startsWith(text)) {
            flush();
            start = t;
            frames = 0;
            firstProgress = progress[0];
            speaker = DialogueSpeaker.current();
            quest = CurrentQuest.get();
            style = look[0];
            portrait = look[1];
            outcome = "";
        }
        text = now;
        lastFrame = t;
        frames++;
        lastProgress = progress[0];
    }

    static void recognised(int chars, long ms) {
        if (outcome.isEmpty()) {
            outcome = "認出 第" + chars + "字 " + ms + "ms";
        }
    }

    static void gaveUp(int chars, long ms) {
        if (outcome.isEmpty()) {
            outcome = "放英文 第" + chars + "字 " + ms + "ms";
        }
    }

    private static void flush() {
        if (text.isEmpty() || file == null || written >= LIMIT) {
            return;
        }
        long span = Math.max(0, lastFrame - start);
        String rate = span <= 0 ? "?" : String.format("%.1f", text.length() * 1000.0 / span);
        String shown = text;
        String self = SelfNames.find(shown);
        if (self != null) {
            shown = shown.replace(self, "{u}");
        }
        String line = String.join(" | ",
                LocalTime.now().withNano(0).toString(),
                hex(firstProgress) + "→" + hex(lastProgress),
                String.valueOf(speaker), String.valueOf(quest),
                String.valueOf(style), String.valueOf(portrait),
                text.length() + "字 " + span + "ms " + frames + "幀 每秒" + rate + "字",
                outcome.isEmpty() ? "-" : outcome,
                shown) + "\n";
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            written++;
        } catch (Exception ignored) {
            file = null;
        }
    }

    private static String hex(int progress) {
        return progress < 0 ? "-" : String.format("E0%02X", progress);
    }

    private static String letters(String raw) {
        StringBuilder out = new StringBuilder();
        raw.codePoints().filter(cp -> !GlyphSplitter.isGlyphCodePoint(cp)
                        && !SpaceOffset.isOffset(cp)).forEach(out::appendCodePoint);
        return out.toString().strip();
    }

    private static String fontOf(Style style) {
        return style == null || style.getFont() == null ? "" : style.getFont().toString();
    }
}
