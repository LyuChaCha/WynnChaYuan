package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 置中判斷的中間值。
 *
 * <h2>為什麼要自己一個檔</h2>
 * 這段輸出先前是掛在 {@link LineDebug} 上的，而<b>連續三次</b>回報回來的檔案裡
 * 一筆都沒有——共用緩衝區、共用去重集合、共用額度、還綁著收集開關，
 * 任何一環出問題都會讓它靜默消失，而且從外面看不出是哪一環。
 *
 * <p>所以拆開：自己的檔案、自己的額度、自己的去重、不看任何開關，
 * 而且整段包在 try 裡——診斷寫不出來絕不能反過來弄壞畫面。
 *
 * <p>寫的是「未鑑定物品的說明有沒有被判成置中」這件事。判斷失敗時畫面上
 * 只看得到「沒有跟著置中」，看不出是分段切錯、寬度量錯、還是算式差了幾像素——
 * 三種的修法完全不同。
 */
public final class LayoutDebug {

    /**
     * 最多寫幾份 tooltip 的詳細判斷。
     *
     * <p>12 太少了：使用者回報坐騎那份沒有詳細判斷，而檔案裡「· 看過 Wyvern Flute」
     * 出現了八次——額度早就被前面的素材袋、住宅選單那些用光。玩家要滑到想查的東西
     * 之前，難免先滑過一堆別的。
     */
    private static final int LIMIT = 40;

    private static Path file;
    private static int written = 0;
    private static final Set<String> seen = new HashSet<>();


    private LayoutDebug() {}

    public static void init(Path path) {
        file = path;
        flowedSeen = 0;
        written = 0;
        failures = 0;
        seen.clear();
        // 改成<b>附加寫入</b>，而且每一次 record 都立刻落地。
        //
        // 先前是把內容累積在一個 StringBuilder 裡、每次重寫整份檔案。使用者連續
        // 兩版回報「這個檔只有檔頭」——而那正是要拿來查坐騎置中的唯一依據。
        // 累積式寫法一旦中途有任何一步沒走到，整份就停在初始狀態，
        // 而且分不出是「沒被呼叫」還是「算到一半掛掉」。附加寫入沒有這個模式：
        // 有呼叫就一定留得下一行。majorid-debug.txt 用的就是這套，一直都可靠。
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    "# 置中判斷。每一行的縮排、內容寬度，以及置中時該有的縮排。"
                    + System.lineSeparator()
                    + "# 三個數字對得起來卻判成靠左，就是分段切錯了。"
                    + System.lineSeparator()
                    + "# 「· 看過」是每一份 tooltip 的足跡；完全沒有這種行，"
                    + "就代表判斷根本沒被呼叫到。"
                    + System.lineSeparator() + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (Throwable t) {
            file = null;                       // 這裡寫不出來就是真的不能寫
        }
    }

    /**
     * 一份 tooltip 的判斷結果。同一份只寫一次。
     *
     * <h2>為什麼要拆成三段各自 try</h2>
     * 先前整支包在<b>一個</b> try 裡：{@code BlockLayout.explain} 一丟例外，
     * 連掛號與寫檔都跳過了，檔案就永遠停在只有檔頭的樣子。使用者回報
     * 「layout-debug.txt 是空的」，而那正是要用來查坐騎置中的唯一依據——
     * 診斷自己失敗卻不留痕跡，比沒有診斷更誤導。
     *
     * <p>現在：掛號一定會做、寫檔一定會做，中間算不出來就<b>把例外寫進檔案</b>。
     */
    public static void record(List<Component> lines, boolean[] centered) {
        if (file == null || lines == null || lines.isEmpty()) {
            return;
        }
        String key;
        try {
            key = lines.get(0).getString() + "/" + lines.size();
        } catch (Throwable t) {
            return;                            // 連名字都取不到，沒東西可記
        }
        boolean detail = written < LIMIT && seen.add(key);
        StringBuilder sb = new StringBuilder();
        // 每一份都留一行足跡，<b>就算不詳細記</b>。這樣「沒被呼叫到」與
        // 「呼叫了但被去重擋掉」永遠分得出來。
        sb.append("· 看過 ").append(oneLine(key)).append(System.lineSeparator());
        if (detail) {
            written++;
            try {
                String explained = BlockLayout.explain(lines, centered);
                sb.append("=== ").append(written).append(" · ").append(oneLine(key))
                  .append(" ===").append(System.lineSeparator())
                  .append(explained).append(System.lineSeparator());
                System.out.println("[WynnChaYuan] 版面判斷" + System.lineSeparator()
                        + explained);
            } catch (Throwable t) {
                failures++;
                sb.append("=== ").append(written).append(" · ").append(oneLine(key))
                  .append(" ===").append(System.lineSeparator())
                  .append("  這一份算不出來：").append(t)
                  .append(System.lineSeparator()).append(System.lineSeparator());
            }
        }
        try {
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable t) {
            failures++;
        }
    }

    /** 診斷失敗過幾次。寫在檔頭，才知道「內容很少」是不是因為一直寫失敗。 */
    private static int failures = 0;

    private static String oneLine(String text) {
        String flat = text.replace('\n', ' ').strip();
        return flat.length() > 30 ? flat.substring(0, 30) + "…" : flat;
    }

    /** 記過的跨行條目數。夠看出問題就好，不是要當日誌。 */
    private static int flowedSeen = 0;

    private static final int FLOWED_LIMIT = 12;

    /**
     * 記一次<b>跨行查表</b>的結果。
     *
     * <h2>為什麼專門記這個</h2>
     * Major ID 與技能敘述走的都是這條路，而「名稱的顏色不見了／跟說明對調了」
     * 我已經修錯兩次——每次都是照畫面猜原文的色段長怎樣，然後猜錯。
     *
     * <p>所以把現場整個寫下來：原文第一行<b>每一段的文字與顏色</b>、
     * 查到的譯名、以及程式最後挑中的樣式。三者擺在一起，錯在哪一步一眼就看得到。
     */
    public static void flowed(java.util.List<com.wynntils.core.text.StyledText> run,
                              String label, net.minecraft.network.chat.Style chosen) {
        if (file == null || run == null || run.isEmpty() || flowedSeen >= FLOWED_LIMIT) {
            return;
        }
        flowedSeen++;
        try {
            writeFlowed(run, label, chosen);
        } catch (Throwable t) {
            failures++;                        // 診斷絕不能反過來弄壞畫面
        }
    }

    private static void writeFlowed(java.util.List<com.wynntils.core.text.StyledText> run,
                                    String label, net.minecraft.network.chat.Style chosen)
            throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 跨行查表 ===").append(System.lineSeparator());
        sb.append("  譯名：").append(label == null ? "（整段命中，沒有拆名稱）" : label)
          .append(System.lineSeparator());
        sb.append("  挑中的樣式：").append(describe(chosen)).append(System.lineSeparator());
        sb.append("  原文第一行的色段：").append(System.lineSeparator());
        int i = 0;
        for (com.wynntils.core.text.StyledTextPart part : run.get(0)) {
            String raw = part.getString(null, com.wynntils.core.text.type.StyleType.NONE);
            com.wynntils.core.text.PartStyle ps = part.getPartStyle();
            sb.append("    [").append(i++).append("] ")
              .append(describe(ps == null ? null : ps.getStyle()))
              .append("  「").append(raw).append("」").append(System.lineSeparator());
        }
        for (com.wynntils.core.text.StyledText line : run) {
            sb.append("  原文：").append(line.getString()).append(System.lineSeparator());
        }
        System.out.println("[WynnChaYuan] 跨行查表" + System.lineSeparator() + sb);
        // 這裡也改成附加。先前是重寫整份檔案，而 record 已經改成附加了——
        // 兩種寫法混在同一個檔上，後寫的那個會把前面附加的內容整個蓋掉。
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    private static String describe(net.minecraft.network.chat.Style style) {
        if (style == null) {
            return "（無）";
        }
        net.minecraft.network.chat.TextColor colour = style.getColor();
        return colour == null ? "繼承"
                : String.format("#%06X", colour.getValue() & 0xFFFFFF);
    }
}
