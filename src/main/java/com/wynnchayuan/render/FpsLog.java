package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
        append("=== 開遊戲 " + LocalDateTime.now().withNano(0) + " ===");
    }

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
        ModConfig config = WynnChaYuan.config();
        return config == null || config.wynntilsFps();
    }

    private static void append(String line) {
        try {
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            file = null;
        }
    }
}
