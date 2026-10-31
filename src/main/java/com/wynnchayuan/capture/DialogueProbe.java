package com.wynnchayuan.capture;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DialogueProbe {

    private static final int LIMIT = 8;

    private static final int CHOICE_LIMIT = 3;

    private static int written = 0;

    private static int choicesWritten = 0;

    private static volatile boolean hasChoices = false;

    public static void noteHasChoices(boolean value) {
        hasChoices = value;
    }

    private static Path dir;

    private static String lastPlain = "";

    private static int lastRows = -1;

    private static boolean justWrote = false;

    private DialogueProbe() {}

    public static void init(Path configDir) {
        dir = configDir;
    }

    public static void dumpFonts() {
        if (dir == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) {
            return;
        }
        String[] names = {
            "hud/dialogue/text/wynncraft/body_0",
            "hud/dialogue/text/wynncraft/body_1",
            "hud/dialogue/text/wynncraft/body_2",
            "hud/dialogue/text/wynncraft/body_3",
            "hud/dialogue/text/wynncraft/body_4",
            "hud/dialogue/text/wynncraft/body_5",
            "hud/dialogue/text/wynncraft/choice_0",
            "hud/dialogue/text/wynncraft/choice_1",
            "hud/dialogue/text/wynncraft/choice_2",
            "hud/dialogue/text/wynncraft/choice_3",
            "hud/dialogue/text/wynncraft/choice_4",
            "hud/dialogue/text/nameplate",
            "hud/dialogue/text/control",
            "hud/gameplay/default/bottom_middle",
        };
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            sb.append("=== ").append(name).append(" ===")
              .append(System.lineSeparator());
            Identifier id = Identifier.withDefaultNamespace("font/" + name + ".json");
            try {
                var found = mc.getResourceManager().getResource(id);
                if (found.isEmpty()) {
                    sb.append("  （沒有這個檔案）").append(System.lineSeparator());
                } else {
                    try (var in = found.get().open()) {
                        sb.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                sb.append("  讀不到：").append(e).append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }
        try {
            Files.writeString(dir.resolve("font-dump.txt"), sb.toString(),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }

    private static boolean hasBodyText(Component message) {
        boolean[] found = {false};
        message.visit((style, text) -> {
            if (fontOf(style).contains("/body_") && readable(text)) {
                found[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static boolean readable(String text) {
        return text.codePoints().anyMatch(cp ->
                cp >= 0x20 && cp < 0x7F && !Character.isWhitespace(cp));
    }

    public static void miss(Component message) {
        if (dir == null || message == null
                || !WynnChaYuan.config().debugDumps() || !hasBodyText(message)) {
            return;
        }
        String body = bodyOf(message);
        if (body.strip().length() < ENOUGH) {
            return;
        }
        boolean same = !missKey.isEmpty()
                && (body.startsWith(missKey) || missKey.startsWith(body));
        if (!same) {
            if (misses >= MISS_LIMIT) {
                return;
            }
            misses++;
        }
        missKey = body;

        StringBuilder sb = new StringBuilder();
        sb.append("=== 改寫失敗（getString） ===").append(System.lineSeparator())
          .append(message.getString()).append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("=== 逐片段 ===").append(System.lineSeparator());
        int[] index = {0};
        message.visit((style, text) -> {
            sb.append(String.format("  [%02d] font=%-38s color=%-9s text=%s%n",
                    index[0]++, fontOf(style),
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    describe(text)));
            return Optional.empty();
        }, Style.EMPTY);

        try {
            Files.writeString(dir.resolve("dialogue-miss-" + misses + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }

    public static void plain(Component message) {
        if (message == null || hasBodyText(message)) {
            return;
        }
        dumpPlain(message, "actionbar-probe-");
    }

    public static void columns(Component before, Component after, String note) {
        if (dir == null || before == null || !WynnChaYuan.config().debugDumps()) {
            return;
        }
        String body = letters(before);
        if (body.isEmpty() || body.equals(columnsKey)) {
            return;
        }
        if (columnsWritten >= COLUMNS_LIMIT) {
            return;
        }
        columnsKey = body;
        columnsWritten++;

        StringBuilder sb = new StringBuilder();
        dumpSide(sb, "原文", before);
        if (after != null) {
            sb.append(System.lineSeparator());
            dumpSide(sb, "譯文", after);
            sb.append(System.lineSeparator())
              .append("整行寬度：原文 ").append(widthOf(before))
              .append(" px、譯文 ").append(widthOf(after))
              .append(" px（差 ").append(widthOf(after) - widthOf(before))
              .append(" px）").append(System.lineSeparator())
              .append("action bar 是整行置中的，所以這個差的一半就是左右位移。")
              .append(System.lineSeparator());
        }
        if (note != null && !note.isEmpty()) {
            sb.append(System.lineSeparator()).append(note).append(System.lineSeparator());
        }

        try {
            Files.writeString(dir.resolve("actionbar-columns-" + columnsWritten + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }

    private static void dumpSide(StringBuilder sb, String label, Component message) {
        int screen = guiWidth();
        int start = screen < 0 ? Integer.MIN_VALUE : (screen - widthOf(message)) / 2;
        sb.append("=== ").append(label).append("（getString） ===")
          .append(System.lineSeparator())
          .append(message.getString()).append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("畫面寬 ").append(screen)
          .append(" px、整行寬 ").append(widthOf(message))
          .append(" px、起點 x=").append(start)
          .append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("=== ").append(label).append(" 逐片段 ===")
          .append(System.lineSeparator());
        int[] index = {0};
        int[] cursor = {start};
        List<String> offscreen = new ArrayList<>();
        message.visit((style, text) -> {
            int at = cursor[0];
            int w = advance(Component.literal(text).setStyle(style));
            sb.append(String.format(
                    "  [%02d] x=%-6s 寬=%-5s font=%-38s color=%-9s px=%-6s text=%s%n",
                    index[0], at == Integer.MIN_VALUE ? "?" : String.valueOf(at),
                    w == Integer.MIN_VALUE ? "?" : String.valueOf(w), fontOf(style),
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    SpaceOffset.isSpaceFont(style)
                            ? String.valueOf(
                                    SpaceOffset.decode(text))
                            : "-",
                    describe(text)));
            if (at != Integer.MIN_VALUE && at < 0 && !text.isBlank()) {
                offscreen.add(String.format("  [%02d] x=%d  %s  %s",
                        index[0], at, fontOf(style), describe(text)));
            }
            index[0]++;
            if (at != Integer.MIN_VALUE && w != Integer.MIN_VALUE) {
                cursor[0] = at + w;
            }
            return Optional.empty();
        }, Style.EMPTY);
        sb.append(System.lineSeparator())
          .append("--- ").append(label).append(" 畫到畫面外面的片段 ---")
          .append(System.lineSeparator());
        if (offscreen.isEmpty()) {
            sb.append("  （沒有）").append(System.lineSeparator());
        } else {
            for (String line : offscreen) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator());
    }

    private static int advance(Component message) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null || mc.font == null
                    ? Integer.MIN_VALUE : mc.font.width(message);
        } catch (Throwable t) {
            return Integer.MIN_VALUE;
        }
    }

    private static int guiWidth() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null || mc.getWindow() == null
                    ? -1 : mc.getWindow().getGuiScaledWidth();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static int widthOf(Component message) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null || mc.font == null ? -1 : mc.font.width(message);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void dumpPlain(Component message, String prefix) {
        if (dir == null || message == null || !WynnChaYuan.config().debugDumps()) {
            return;
        }
        String body = words(message);
        if (body.isEmpty() || body.equals(plainKey)) {
            return;
        }
        if (plains >= PLAIN_LIMIT) {
            return;
        }
        plainKey = body;
        plains++;

        StringBuilder sb = new StringBuilder();
        sb.append("=== 不是對話的 action bar（getString） ===")
          .append(System.lineSeparator())
          .append(message.getString()).append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("=== 逐片段 ===").append(System.lineSeparator());
        int[] index = {0};
        message.visit((style, text) -> {
            sb.append(String.format("  [%02d] font=%-38s color=%-9s text=%s%n",
                    index[0]++, fontOf(style),
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    describe(text)));
            return Optional.empty();
        }, Style.EMPTY);

        try {
            Files.writeString(dir.resolve(prefix + plains + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }

    public static void tofu(Component before, Component after, String why) {
        if (dir == null || before == null || !WynnChaYuan.config().debugDumps()
                || tofus >= TOFU_LIMIT) {
            return;
        }
        tofus++;

        StringBuilder sb = new StringBuilder();
        sb.append("=== 畫不出來的碼位 ===").append(System.lineSeparator())
          .append("  ").append(why).append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("=== 換之前 ===").append(System.lineSeparator());
        segments(sb, before);
        sb.append(System.lineSeparator())
          .append("=== 差一點就送出去的那一份 ===").append(System.lineSeparator());
        segments(sb, after);

        try {
            Files.writeString(dir.resolve("dialogue-tofu-" + tofus + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }

    private static void segments(StringBuilder sb, Component message) {
        if (message == null) {
            sb.append("  （沒有）").append(System.lineSeparator());
            return;
        }
        int[] index = {0};
        message.visit((style, text) -> {
            sb.append(String.format("  [%02d] font=%-38s color=%-9s text=%s%n",
                    index[0]++, fontOf(style),
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    describe(text)));
            return Optional.empty();
        }, Style.EMPTY);
    }

    private static String words(Component message) {
        StringBuilder sb = new StringBuilder();
        message.visit(text -> {
            if (readable(text)) {
                sb.append(text);
            }
            return Optional.empty();
        });
        String core = sb.toString().strip();
        int letters = 0;
        for (int i = 0; i < core.length(); i++) {
            char c = core.charAt(i);
            if (Character.isDigit(c)) {
                return "";
            }
            if (Character.isLetter(c)) {
                letters++;
            }
        }
        return letters >= MIN_WORDS ? core : "";
    }

    private static String letters(Component message) {
        StringBuilder sb = new StringBuilder();
        message.visit(text -> {
            if (readable(text)) {
                sb.append(text);
            }
            return Optional.empty();
        });
        String core = sb.toString().strip();
        int letters = 0;
        for (int i = 0; i < core.length(); i++) {
            if (Character.isLetter(core.charAt(i))) {
                letters++;
            }
        }
        return letters >= MIN_WORDS ? core : "";
    }

    private static final int MIN_WORDS = 8;

    private static int fonts0 = 0;

    private static final int TOFU_LIMIT = 3;

    private static int tofus = 0;

    private static final int PLAIN_LIMIT = 6;

    private static int plains = 0;

    private static String plainKey = "";

    private static final int COLUMNS_LIMIT = 4;

    private static int columnsWritten = 0;

    private static String columnsKey = "";

    private static final int MISS_LIMIT = 6;

    private static final int ENOUGH = 12;

    private static int misses = 0;
    private static String missKey = "";

    public static void recordChoices(Component message) {
        if (dir == null || !hasChoices || choicesWritten >= CHOICE_LIMIT
                || message == null || !WynnChaYuan.config().debugDumps()) {
            return;
        }
        choicesWritten++;
        StringBuilder sb = new StringBuilder();
        sb.append("=== 有選項的對話：原始 action bar ===")
          .append(System.lineSeparator());
        sb.append(message.getString()).append(System.lineSeparator())
          .append(System.lineSeparator());
        sb.append("=== 逐片段（看選項在哪一個分區）===")
          .append(System.lineSeparator());
        int[] index = {0};
        message.visit((style, text) -> {
            sb.append("  [").append(String.format("%02d", index[0]++)).append("] ")
              .append("font=").append(fontOf(style))
              .append("  text=").append(describe(text))
              .append(System.lineSeparator());
            return Optional.empty();
        }, Style.EMPTY);
        try {
            Files.writeString(dir.resolve("dialogue-choice-probe-"
                                                  + choicesWritten + ".txt"),
                              sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }

    public static void record(Component message) {
        justWrote = false;
        if (dir == null || message == null
                || !WynnChaYuan.config().debugDumps() || !hasBodyText(message)) {
            return;
        }
        String plain = message.getString();
        String body = bodyOf(message);
        int rows = rowsOf(message);
        if (body.length() < lastPlain.length() + 12 && rows == lastRows) {
            return;
        }
        lastPlain = body;
        lastRows = rows;
        written = written % LIMIT + 1;
        if (fonts0 == 0) {
            fonts0 = 1;
            dumpFonts();
        }

        List<String> fonts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("=== 原始（getString） ===").append(System.lineSeparator());
        sb.append(plain).append(System.lineSeparator()).append(System.lineSeparator());

        sb.append("=== 逐片段 ===").append(System.lineSeparator());
        int[] index = {0};
        message.visit((style, text) -> {
            String font = fontOf(style);
            if (!fonts.contains(font)) {
                fonts.add(font);
            }
            sb.append(String.format("  [%02d] font=%-38s color=%-9s text=%s%n",
                    index[0]++, font,
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    describe(text)));
            return Optional.empty();
        }, Style.EMPTY);

        sb.append(System.lineSeparator())
          .append("=== 用到的字型 ===").append(System.lineSeparator());
        for (String font : fonts) {
            sb.append("  ").append(font).append(System.lineSeparator());
        }

        try {
            Files.writeString(dir.resolve("dialogue-probe-" + written + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8);
            justWrote = true;
        } catch (Exception e) {
        }
    }

    public static void after(Component message) {
        if (!justWrote || dir == null || message == null) {
            return;
        }
        justWrote = false;

        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator())
          .append("=== 取代後（getString） ===").append(System.lineSeparator())
          .append(message.getString()).append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("=== 取代後逐片段 ===").append(System.lineSeparator());
        int[] index = {0};
        message.visit((style, text) -> {
            sb.append(String.format("  [%02d] font=%-38s color=%-9s text=%s%n",
                    index[0]++, fontOf(style),
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    describe(text)));
            return Optional.empty();
        }, Style.EMPTY);

        try {
            Files.writeString(dir.resolve("dialogue-probe-" + written + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
        }
    }

    private static String bodyOf(Component message) {
        StringBuilder out = new StringBuilder();
        message.visit((style, text) -> {
            if (fontOf(style).contains("/body_")) {
                out.append(text);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    private static int rowsOf(Component message) {
        int[] rows = {0};
        message.visit((style, text) -> {
            if (fontOf(style).contains("/body_") && readable(text)) {
                rows[0]++;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return rows[0];
    }

    private static String fontOf(Style style) {
        return style.getFont() == null ? "(預設)" : style.getFont().toString();
    }

    private static String describe(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp >= 0x20 && cp < 0x7F) {
                out.append((char) cp);
            } else if (cp == ' ') {
                out.append('␠');
            } else {
                out.append(String.format("<U+%04X>", cp));
            }
        });
        return out.toString();
    }
}
