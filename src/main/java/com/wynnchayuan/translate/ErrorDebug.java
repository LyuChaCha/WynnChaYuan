package com.wynnchayuan.translate;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 把被吞掉的例外寫出來。
 *
 * <h2>為什麼需要這個檔</h2>
 * 繪製相關的程式碼都包在 {@code catch (Throwable)} 裡——那是對的，一個算繪
 * 例外不該把整個畫面拖垮。但先前只記了一個<b>次數</b>，於是使用者的
 * {@code captured.json} 裡躺著「render.tooltipError 1398」，而我們完全不知道
 * 那 1398 次是什麼。畫面上的症狀只是「翻譯面板沒出現」，看起來像沒翻到。
 *
 * <p>所以出事要留下現場。同一種例外只記<b>一次</b>（用類別加最上面幾層堆疊
 * 認），否則每幀都丟的話瞬間就把檔案灌爆。
 */
public final class ErrorDebug {

    private static final String FILE = "error-debug.txt";

    /** 最多記幾種不同的例外。夠看出問題就好，不是要當日誌。 */
    private static final int LIMIT = 20;

    private static final Set<String> seen = new LinkedHashSet<>();

    private static Path dir;

    private ErrorDebug() {}

    public static void into(Path configDir) {
        dir = configDir;
    }

    /**
     * 記一次例外。
     *
     * @param where 出事的地方，例如 {@code tooltip.panel}
     * @param note  當下在處理什麼（第一行文字之類），沒有就給 null
     */
    public static void note(String where, String note, Throwable t) {
        if (dir == null || t == null) {
            return;
        }
        try {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            String key = where + "|" + fingerprint(trace);
            synchronized (seen) {
                if (seen.size() >= LIMIT || !seen.add(key)) {
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(where).append(" ===").append(System.lineSeparator());
            if (note != null && !note.isBlank()) {
                sb.append("處理中：").append(note).append(System.lineSeparator());
            }
            sb.append(trace).append(System.lineSeparator());
            Files.writeString(dir.resolve(FILE), sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ignored) {
            // 診斷本身絕不能再丟一次
        }
    }

    /** 例外類別加最上面三層，用來判斷「這個我已經記過了」。 */
    private static String fingerprint(String trace) {
        String[] lines = trace.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length && i < 4; i++) {
            sb.append(lines[i].strip()).append('\n');
        }
        return sb.toString();
    }
}
