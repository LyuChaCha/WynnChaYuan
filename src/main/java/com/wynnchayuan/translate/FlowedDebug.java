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
            // 走的是單色還是彩虹。顏色不對時這一行決定了要往哪邊查：
            // 判成彩虹卻不該是，跟挑錯單色，是兩個完全不同的問題。
            List<Style> ramp = LineTranslator.labelRamp(run.get(0));
            sb.append("  上色方式：")
              .append(ramp == null ? "單色" : "彩虹（" + ramp.size() + " 個顏色）")
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

    /** 查不到的最多記幾筆。 */
    private static final int MISS_LIMIT = 20;

    private static int missed = 0;

    /**
     * 記一段<b>查不到</b>的跨行原文。
     *
     * <h2>為什麼非記不可</h2>
     * 先前只記成功的。於是檔案裡看不到 Major ID 時有兩種可能：跨行查表<b>根本沒被
     * 呼叫到</b>，或者呼叫了但查不到——這兩件事的修法完全不同，而診斷分不出來。
     * 連查不到也記下來，看一眼就知道是哪一種：檔案裡有這一段就是查了沒中
     * （語料的問題），完全沒有就是連進都沒進來（程式的問題）。
     *
     * <p>順便把攤平後的鍵原樣印出來，直接拿去跟譯文檔的 {@code src} 對，
     * 差一個空格還是差一個標點一眼就看得出來。
     */
    public static void miss(String template) {
        if (file == null || template == null || missed >= MISS_LIMIT) {
            return;
        }
        missed++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 查不到 ").append(missed).append(" ===")
              .append(System.lineSeparator());
            sb.append("  攤平後的鍵：").append(TranslationStore.normalise(template))
              .append(System.lineSeparator());
            for (String line : template.split("\\R", -1)) {
                sb.append("    原文：").append(line).append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }

    /** 有選項的對話最多記幾筆。 */
    private static final int CHOICE_LIMIT = 6;

    private static int choicesSeen = 0;

    /**
     * 記一段<b>有選項</b>的對話原文。
     *
     * <p>遊戲把選項畫在對話框上面另一個框裡，但事件只給我們一整串文字——
     * 哪幾行是 NPC 說的、哪幾行是玩家可以選的，從事件本身看不出來。
     * 先把原始結構記下來，照真實資料實作才不會又猜錯。
     */
    public static void noteChoices(String text) {
        if (file == null || text == null || choicesSeen >= CHOICE_LIMIT) {
            return;
        }
        choicesSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 有選項的對話 ").append(choicesSeen).append(" ===")
              .append(System.lineSeparator());
            int i = 0;
            for (String line : text.split("\\R", -1)) {
                sb.append("  [").append(i++).append("] ").append(line)
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }
}
