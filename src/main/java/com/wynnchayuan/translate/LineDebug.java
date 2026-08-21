package com.wynnchayuan.translate;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import com.wynntils.core.text.StyledText;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 把「有翻到」的那些行的片段結構寫出來。
 *
 * <h2>為什麼需要這個</h2>
 * 對齊出問題時，畫面上只看得到「歪掉」，看不出是哪一種歪：是空白沒被認出來、
 * 認出來了但補錯量、還是那行根本走了另一條路。前後試了好幾版，每次都得靠
 * 截圖反推，而截圖看不到字型與碼位。
 *
 * <p>已經有 {@code TooltipDebug} 負責<b>沒翻到</b>的行；這支負責<b>翻到了但
 * 版面怪怪的</b>那些，兩者的用途剛好互補。
 *
 * <p>只在「收集未翻譯字串」開著時寫，而且只寫前幾行——一般玩家的 config
 * 資料夾不該多出一堆檔案。
 */
public final class LineDebug {

    private static final int LIMIT = 40;

    private static Path file;
    private static int written = 0;
    private static int layouts = 0;

    /** 版面判斷自己的額度，見 {@link #layout}。 */
    private static final int LAYOUT_LIMIT = 8;
    private static StringBuilder buffer = new StringBuilder();

    /**
     * 已經記過的句子。
     *
     * <p>沒有這個的話，額度會被<b>同一行</b>洗光——tooltip 每幀都會重畫，
     * 滑鼠停在某個東西上兩秒就寫滿 25 筆一模一樣的內容。前兩次收到的診斷檔
     * 就是這樣，25 筆全是「點擊前往你的住宅」，真正要看的裝備行一筆都沒有。
     */
    private static final java.util.Set<String> seen = new java.util.HashSet<>();

    private LineDebug() {}

    public static void init(Path path) {
        file = path;
        // 版本寫在最前面。看回報的檔案時，第一個要排除的可能就是「他跑的是舊版」——
        // 譯文是從 GitHub 同步的，所以光看譯文內容分辨不出 jar 的新舊。
        buffer.append("# WynnChaYuan v").append(WynnChaYuan.version())
              .append("　收集語料：")
              .append(WynnChaYuan.config().collect() ? "開" : "關")
              .append(System.lineSeparator()).append(System.lineSeparator());
    }

    /**
     * 記錄逐片段那條路的中間狀態。
     *
     * <p>只看最後的 Component 不夠：間隔「完全消失」與「補太少」在畫面上
     * 都是歪掉，但成因完全不同——前者是空白被編碼成空字串，後者是差額算錯。
     * 要分辨就得看到<b>每個空白補償前後的像素值</b>。
     */
    public static void pieces(String header, String detail) {
        if (file == null || written >= LIMIT || !WynnChaYuan.config().collect()
                || !seen.add(header)) {
            return;
        }
        written++;
        buffer.append("=== ").append(written).append(" · ").append(header)
              .append(" ===").append(System.lineSeparator())
              .append(detail).append(System.lineSeparator());
        flush();
    }

    /** 記一行。原文與譯文的片段並排，才看得出哪一段變了、變多少。 */
    public static void record(StyledText original, Component translated) {
        if (file == null || written >= LIMIT || translated == null
                || !WynnChaYuan.config().collect()
                || !seen.add(original.getStringWithoutFormatting())) {
            return;
        }
        written++;
        buffer.append("=== ").append(written).append(" ===")
              .append(System.lineSeparator());
        dump("原文", original.getComponent());
        dump("譯文", translated);
        buffer.append(System.lineSeparator());
        flush();
    }

    private static void dump(String label, Component component) {
        buffer.append(label).append(": ").append(component.getString())
              .append(System.lineSeparator());
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                buffer.append(String.format("   font=%-28s px=%-6s text=%s%n",
                        style.getFont() == null ? "(null)" : style.getFont().toString(),
                        SpaceOffset.isSpaceFont(style)
                                ? String.valueOf(SpaceOffset.decode(text)) : "-",
                        describe(text)));
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    /** 把看不見的碼位寫成 U+XXXX，否則檔案裡就是一片空白，什麼都看不出來。 */
    private static String describe(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp == ' ') {
                sb.append('␠');
            } else if (Character.isISOControl(cp) || cp > 0xFFFF
                    || Character.getType(cp) == Character.PRIVATE_USE
                    || Character.getType(cp) == Character.UNASSIGNED) {
                sb.append(String.format("<U+%04X>", cp));
            } else {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString();
    }

    private static void flush() {
        try {
            Files.writeString(file, buffer.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 診斷寫不出來就算了，不要影響遊戲
        }
    }
}
