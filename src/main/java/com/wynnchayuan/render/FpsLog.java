package com.wynnchayuan.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 診斷用：每秒的 FPS 連同當時開著哪個畫面、效能開關的狀態，寫進 {@code debug/fps.txt}。
 *
 * <h2>為什麼</h2>
 * 改 Wynntils 效能（見 {@link HighlightModelCache}）要拿數字比：同一個容器、開關開與關各站一段時間。
 * 叫使用者盯著 F3 抄數字不準，所以記下來我自己讀。
 *
 * <p>每換一次「畫面＋開關」就收一段，寫一行摘要（平均、最低、幾秒）。前 3 秒不算——
 * 剛開容器時要載材質、解析物品，那幾秒的數字不代表平常。
 *
 * <p>只有開著診斷檔（{@code debugDumps}）時才寫。數字是 Minecraft 自己每秒算一次的
 * {@code Minecraft#getFps}，跟 F3 左上角同一個。
 */
public final class FpsLog {

    private FpsLog() {
    }

    private static final int WARMUP = 3;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static Path file;
    private static long nextSample;

    private static String segment;
    private static int seconds;
    private static long sum;
    private static int min = Integer.MAX_VALUE;

    public static void init(Path debugDir) {
        file = debugDir.resolve("fps.txt");
        append("=== 開遊戲 " + java.time.LocalDateTime.now().withNano(0) + " ===");
    }

    /** 每個 client tick 叫一次；一秒取樣一次。 */
    public static void tick(Minecraft mc) {
        if (file == null || mc == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextSample) {
            return;
        }
        nextSample = now + 1000;
        int fps = mc.getFps();
        String where = describe(mc.screen);
        String key = where + " | 加速=" + (boostOn() ? "開" : "關");
        if (!key.equals(segment)) {
            flush();
            segment = key;
            seconds = 0;
            sum = 0;
            min = Integer.MAX_VALUE;
        }
        seconds++;
        if (seconds > WARMUP) {
            sum += fps;
            min = Math.min(min, fps);
        }
        append(LocalTime.now().format(TIME) + "  " + fps + " fps  " + key);
    }

    private static void flush() {
        int counted = seconds - WARMUP;
        if (segment == null || counted < 3) {
            return;
        }
        append(String.format("  ── 摘要：平均 %.1f fps、最低 %d、%d 秒｜%s",
                sum / (double) counted, min, counted, segment));
    }

    private static String describe(Screen screen) {
        if (screen == null) {
            return "沒開畫面";
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString()
                .replaceAll("[\\p{Cntrl}\\p{Co}\\x{E000}-\\x{F8FF}\\x{F0000}-\\x{10FFFF}]", "").strip();
        if (title.length() > 40) {
            title = title.substring(0, 40);
        }
        return screen.getClass().getSimpleName() + (title.isEmpty() ? "" : "「" + title + "」");
    }

    private static boolean boostOn() {
        com.wynnchayuan.CollectorConfig config = com.wynnchayuan.WynnChaYuan.config();
        return config == null || config.wynntilsFps();
    }

    private static void append(String line) {
        try {
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            file = null;                       // 寫不了就不寫了，不影響遊戲
        }
    }
}
