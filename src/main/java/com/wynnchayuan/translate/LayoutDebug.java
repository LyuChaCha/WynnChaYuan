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
        drawnWritten = 0;
        drawnSeen.clear();
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

    /** 左緣對照最多記幾份<b>有問題的</b>。 */
    private static final int DRAWN_LIMIT = 60;

    /** 足跡最多記幾行。足跡是一份一行，不會像詳細那樣長。 */
    private static final int DRAWN_SEEN_LIMIT = 80;

    private static int drawnWritten = 0;

    private static final Set<String> drawnSeen = new HashSet<>();

    /**
     * 一份 tooltip 畫出去之前，每一行的<b>左緣</b>有沒有跟原文對上。
     *
     * <h2>為什麼需要這個</h2>
     * 實機回報素材的需求列「防禦需求」比「耐久度」往右縮了十幾像素，而照
     * {@code layout-debug} 重建出來的那一份，四列標籤全部落在 0——
     * 我們這條路算出來是對的，那十幾像素是後面某一步加上去的。
     * 「翻完」與「撐寬後」各記一次，就知道是哪一步。
     *
     * <p>只記<b>有行對不上</b>的那幾份：全部對得上的沒什麼好看，
     * 記下來只會把檔案塞滿、把真正有問題的那份擠掉。
     *
     * @param stage 這是哪一步之後量的
     */
    public static void drawn(String stage, List<Component> original, List<Component> made,
                             java.util.function.ToIntFunction<Component> width) {
        if (file == null || original == null || made == null || width == null
                || original.isEmpty()) {
            return;
        }
        try {
            // 對得上的也要留一行足跡。沒有足跡的話「量過、每一行都對」與
            // 「根本沒量到」在檔案裡長得一模一樣——而這兩件事的下一步完全相反。
            // 見 [[wynnchayuan-palette-log-on-success]]。
            String key = stage + "/" + original.get(0).getString() + "/" + original.size();
            boolean fresh = drawnSeen.size() < DRAWN_SEEN_LIMIT && drawnSeen.add(key);
            if (!fresh) {
                return;                        // 同一份滑過幾十次，別把名額吃光
            }
            String text = compareLeads(stage, original, made, width);
            if (text == null) {
                {
                    Files.writeString(file,
                            "· 左緣對照（" + stage + "）" + oneLine(original.get(0).getString())
                            + "：" + Math.min(original.size(), made.size())
                            + " 行都跟原文對得上" + System.lineSeparator(),
                            StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.APPEND);
                }
                return;
            }
            if (drawnWritten >= DRAWN_LIMIT) {
                return;
            }
            drawnWritten++;
            Files.writeString(file, text, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable t) {
            failures++;                        // 診斷絕不能反過來弄壞畫面
        }
    }

    /** @return 有行對不上時的報告；全部對得上回傳 {@code null} */
    private static String compareLeads(String stage, List<Component> original,
                                       List<Component> made,
                                       java.util.function.ToIntFunction<Component> width) {
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        int n = Math.min(original.size(), made.size());
        // 說明段併成一句時譯文會少幾行，後面的行整段往前移。照索引硬配就會
        // 把「第 8 行對到第 7 行」報成歪掉——實機第一份 dump 裡兩張卡都是這樣的
        // 假警報。所以同一個索引與位移過的索引<b>兩邊都對不上</b>才算數。
        int shift = original.size() - made.size();
        for (int i = 0; i < n; i++) {
            int a = leadOf(original.get(i), width);
            int b = leadOf(made.get(i), width);
            if (a == Integer.MIN_VALUE || b == Integer.MIN_VALUE || a == b) {
                continue;
            }
            int j = i - shift;
            if (shift != 0 && j >= 0 && j < made.size()
                    && leadOf(made.get(j), width) == a) {
                continue;                      // 位移之後對得上，不是歪掉
            }
            any = true;
            sb.append(String.format("  [%2d] 原文左緣 %4d  譯文左緣 %4d  差 %+d  %s%n",
                    i, a, b, b - a, oneLine(made.get(i).getString())));
        }
        if (!any) {
            return null;
        }
        return "=== 左緣對照（" + stage + "）· " + oneLine(original.get(0).getString())
                + "\u3000原文 " + original.size() + " 行、譯文 " + made.size() + " 行 ==="
                + System.lineSeparator() + sb + System.lineSeparator();
    }

    /**
     * 第一個實字畫在第幾個像素。
     *
     * <p>偏移字元與圖示都只佔寬度、不算「字」——欄位對齊就是靠它們排出來的，
     * 把圖示當成字的話每一行的左緣都會量成 0，什麼都看不出來。
     *
     * @return 整行都沒有字時回傳 {@link Integer#MIN_VALUE}
     */
    private static int leadOf(Component line,
                              java.util.function.ToIntFunction<Component> width) {
        int[] x = {0};
        int[] found = {Integer.MIN_VALUE};
        line.visit((style, text) -> {
            if (found[0] != Integer.MIN_VALUE) {
                return java.util.Optional.empty();
            }
            boolean glyphOnly = !text.isBlank() && text.codePoints().allMatch(
                    cp -> com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp));
            if (text.isBlank() || glyphOnly) {
                x[0] += width.applyAsInt(
                        net.minecraft.network.chat.Component.literal(text).setStyle(style));
                return java.util.Optional.empty();
            }
            found[0] = x[0];
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return found[0];
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
