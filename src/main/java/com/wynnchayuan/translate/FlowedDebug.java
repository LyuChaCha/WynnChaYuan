package com.wynnchayuan.translate;

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

/**
 * 跨行查表的現場：Major ID 與技能敘述。
 *
 * <h2>為什麼要自己一個檔</h2>
 * 這段診斷原本寄生在 {@code LayoutDebug} 裡，共用同一個緩衝、同一份額度、
 * 同一個「出事就停」的開關。結果使用者照指示操作了三次，回報回來的
 * {@code layout-debug.txt} 一次有內容、一次只有檔頭、一次連 record 的內容都沒有——
 * 到底是沒被呼叫、還是被別的東西擦掉，完全分不出來。
 *
 * <p>所以獨立出來：自己的檔案、自己的額度、<b>附加寫入</b>不重寫，
 * 而且每一次呼叫都先記一行流水號。這樣「有沒有被呼叫到」本身就是可觀察的。
 */
public final class FlowedDebug {

    private static final String FILE = "majorid-debug.txt";

    /** 最多記幾筆。夠看出問題，又不會把玩家的資料夾塞爆。 */
    private static final int LIMIT = 30;

    private static Path file;
    private static int seen = 0;

    private FlowedDebug() {}

    public static void init(Path configDir) {
        file = configDir.resolve(FILE);
        seen = 0;
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

    /**
     * 記一次跨行翻譯。
     *
     * @param label  查到的譯名，整段命中時是 null
     * @param chosen 程式挑中要套在譯名上的樣式
     */
    public static void note(List<StyledText> run, String label, Style chosen) {
        if (file == null || run == null || run.isEmpty() || seen >= LIMIT) {
            return;
        }
        seen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(seen).append(" ===").append(System.lineSeparator());
            sb.append("  譯名：")
              .append(label == null ? "（整段命中，沒有拆出名稱）" : label)
              .append(System.lineSeparator());
            sb.append("  挑中的樣式：").append(describe(chosen))
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
            System.out.println("[WynnChaYuan] 跨行查表 #" + seen);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面。不把 file 設成 null——
            // 那等於一次失敗就永久關閉，而使用者只會看到「檔案沒生成」。
        }
    }

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
}
