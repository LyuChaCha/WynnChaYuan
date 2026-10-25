package com.wynnchayuan.render;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * 診斷：每一句台詞「打字之前」拿得到什麼，以及多久才認出來。只寫檔，不影響畫面。
 *
 * <h2>為什麼要有</h2>
 * 使用者提議「給台詞編號、照編號出來」。查過 Wynntils 與探針：伺服器是<b>逐字</b>送的
 * （每一則 action bar 只帶到目前打到的地方），沒有整句先到。但對話框裡有一條
 * {@code hud/dialogue/effect/progress} 的進度條（U+E000…U+E0DF，224 格），已知的一筆
 * 「Hey, …」停在 U+E095（約 2/3），看起來像「這段對話第幾句／共幾句」——如果是，
 * 就是現成的編號。還不能確定，也不知道換句的第一幀它是不是還停在上一句的值。
 * 這份檔一句一行記下來，下一輪拿來驗證、建對照表：
 *
 * <pre>
 *   時間 | 進度(第一幀→最後) | 名牌 | 追蹤任務 | 框的樣式 | 頭像 | 打了幾字、幾毫秒（每秒幾字）
 *        | 認出來：第幾個字、幾毫秒 / 放英文：第幾個字、幾毫秒 | 原文（玩家名換成 {u}）
 * </pre>
 *
 * <p>只在 F6 的診斷開著時寫（跟其他探針一樣），最多 {@link #LIMIT} 句。
 */
public final class DialogueStartLog {

    private static final int LIMIT = 600;
    private static final String PROGRESS = "hud/dialogue/effect/progress";
    private static final String BODY = "/body_";

    private static Path file;
    private static int written;

    // 目前這一句
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
            // 刪不掉就接著寫
        }
    }

    // ------------------------------------------------------------------
    // 逐幀：每一則訊息畫出去多少字（test3「小框閃一次」「逐字忽快忽慢」要的資料）
    // ------------------------------------------------------------------

    private static Path framesFile;
    private static int frameLines;
    private static final int FRAME_LIMIT = 6000;
    private static final long T0 = System.currentTimeMillis();
    private static String lastRow = "";

    /**
     * 一幀：就地取代送出去幾個中文字（-1 是留英文、0 是先空著），或小框畫了幾個字、幾列。
     * 跟上一幀完全一樣就不記（字停下來之後每 tick 都會再送一次）。
     *
     * <pre>
     *   毫秒 | 模式 | 原文已打字數 | 畫出的中文字數 | 小框列數
     * </pre>
     */
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

    /** 每一則對話框的原始 action bar（還沒被任何人改過）。 */
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
            speaker = com.wynnchayuan.capture.DialogueSpeaker.current();
            quest = com.wynnchayuan.capture.CurrentQuest.get();
            style = look[0];
            portrait = look[1];
            outcome = "";
        }
        text = now;
        lastFrame = t;
        frames++;
        lastProgress = progress[0];
    }

    /** 就地取代認出這一句的那一幀。 */
    static void recognised(int chars, long ms) {
        if (outcome.isEmpty()) {
            outcome = "認出 第" + chars + "字 " + ms + "ms";
        }
    }

    /** 就地取代放棄、改放英文的那一幀。 */
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
        String self = com.wynnchayuan.capture.SelfNames.find(shown);
        if (self != null) {
            shown = shown.replace(self, "{u}");
        }
        String line = String.join(" | ",
                java.time.LocalTime.now().withNano(0).toString(),
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
            file = null;                       // 寫不了就別再試
        }
    }

    private static String hex(int progress) {
        return progress < 0 ? "-" : String.format("E0%02X", progress);
    }

    /** 把排版偏移與圖示拿掉，只留看得見的文字。 */
    private static String letters(String raw) {
        StringBuilder out = new StringBuilder();
        raw.codePoints().filter(cp -> !com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)
                        && !com.wynnchayuan.translate.SpaceOffset.isOffset(cp))
                .forEach(out::appendCodePoint);
        return out.toString().strip();
    }

    private static String fontOf(Style style) {
        return style == null || style.getFont() == null ? "" : style.getFont().toString();
    }
}
