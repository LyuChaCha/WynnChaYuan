package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天訊息的譯文要跟原文那一塊<b>形狀一樣</b>。
 *
 * <h2>先前壞在哪</h2>
 * 整條聊天路徑寫死「這一行是置中的」。tooltip 那邊這樣講得通——一份 tooltip
 * 的置中是相對於它自己最寬那行算的；聊天不是，Wynncraft 是照<b>聊天視窗</b>
 * 的固定寬度排的，同一塊裡有置中的標題，也有靠左的獎勵清單。
 *
 * <p>結果靠左的每一行都被往右推了半個寬度差。實機那張「任務完成」的圖裡，
 * 譯文的「獎勵:」比原文的「Rewards:」右邊一截，就是這樣來的。
 *
 * <h2>這裡釘住什麼</h2>
 * 靠左的行，譯文的縮排要<b>剛好等於</b>原文那一行的縮排——不多不少。
 * 中文比英文短，但短的是右邊，左緣不該動。
 *
 * <p>量的是排版偏移字元解出來的像素值，那個不需要字型（見 {@link SpaceOffset}），
 * headless 也量得準。真空白字元的寬度要有字型才量得出來，所以那一種
 * （「[Cave Completed]」那塊）這裡測不到，只能靠實機看。
 */
public final class ChatAlignTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;
    private static final int PINK = 0xFF55FF;
    private static final int GREY = 0xAAAAAA;

    /** 三行都各有自己的前導偏移，數字照英文寬度算好。 */
    private static final int[] LEADS = {96, 40, 8};

    /**
     * 實機「任務完成」那一塊的骨架。
     *
     * <p>縮排是 {@code minecraft:space} 的偏移字元，在語料的模板裡就是行首那個
     * {@code {#}}——重建時原樣填回去，所以譯文一開始的縮排跟原文一樣。
     */
    private static StyledText quest() {
        MutableComponent all = Component.empty();
        all.append(offset(LEADS[0]));
        all.append(lit("[Quest Completed]", GOLD));
        all.append(lit("\n", GREY));
        all.append(offset(LEADS[1]));
        all.append(lit("Rewards:", PINK));
        all.append(lit("\n", GREY));
        all.append(offset(LEADS[2]));
        all.append(lit("- +Access to the Province of Wynn", GREY));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-chat-align");
        FlowedDebug.init(dir);

        String src = "{#}[Quest Completed]\n{#}Rewards:\n{#}- +Access to the Province of Wynn";
        String dst = "{#}[任務完成]\n{#}獎勵:\n{#}- +取得進入 Wynn 行省的資格";
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty(src, dst);
        Files.writeString(dir.resolve("quest.json"), root.toString(), StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Component hit = LineTranslator.translateChat(quest(), store);
        check("整塊查得到譯文", hit != null);
        if (hit == null) {
            report();
            return;
        }
        System.out.println("      輸出：" + hit.getString().replace("\n", " ⏎ "));
        check("三行都還在", rows(hit) == 3);
        check("內容有翻出來", hit.getString().contains("任務完成")
                && hit.getString().contains("獎勵"));

        int[] made = leads(hit);
        for (int i = 0; i < LEADS.length && i < made.length; i++) {
            check("第 " + i + " 行的縮排跟原文一樣（原文 " + LEADS[i]
                          + "、譯文 " + made[i] + "）", made[i] == LEADS[i]);
        }
        check("量到三行的縮排（實際 " + made.length + " 行）", made.length == 3);

        // 原文開頭多一行「只有空白」時，行數仍然要對得起來。
        //
        // 語料的鍵是 strip() 過的，只有空白的首行早就被去掉了，譯文不可能有那一行。
        // 先前 solidRows 只認「完全沒有片段」的行，於是實機的「洞穴完成」永遠是
        // 「原文 7 行、譯文 6 行」對不上，整塊排版原樣返回——診斷檔的
        // 「聊天對齊 11」寫得清清楚楚。
        MutableComponent padded = Component.empty();
        padded.append(lit("  ", GREY));            // 只有空白的第一行
        padded.append(lit("\n", GREY));
        padded.append(quest().getComponent().copy());
        padded.append(lit("\n", GREY));           // 結尾的空行
        Component trimmed = LineTranslator.translateChat(
                StyledText.fromComponent(padded), store);
        check("原文首行只有空白時仍然翻得出來", trimmed != null);
        if (trimmed != null) {
            int[] kept = leads(trimmed);
            check("三行的縮排仍然跟原文一樣（實際 "
                          + java.util.Arrays.toString(kept) + "）",
                  kept.length >= 3 && kept[0] == LEADS[0]
                          && kept[1] == LEADS[1] && kept[2] == LEADS[2]);
        }

        // 明說「這一行是置中的」時才重新置中。行數與內容不能因此走樣。
        Component centred = LineTranslator.translateChat(quest(), store, Boolean.TRUE);
        check("指定置中時仍翻得出來", centred != null);
        check("指定置中時行數不變", centred == null || rows(centred) == 3);

        columns();
        report();
    }

    /**
     * 欄距要跟著譯文的寬度走。見 {@link LineTranslator#columnDrift}。
     *
     * <p>數字取自實機診斷檔的「聊天對齊 7」：信標選單一行是
     * {@code [縮排 33px]Orange Beacon[間隔 69px]Yellow Beacon}，兩段英文
     * 分別是 90px 與 82px，譯成「橘色信標」「黃色信標」之後各剩 40px。
     */
    private static void columns() {
        // 第 0 個間隔是行首縮排，chatRow 的 pad 在管，這裡不能動。
        int[] two = LineTranslator.columnDrift(
                List.of(0, 90, 82), List.of(0, 40, 40), new int[] {33, 69});
        check("行首的縮排不動（實際 " + two[0] + "）", two[0] == 0);
        check("欄距吸收左欄縮水的 50px（實際 " + two[1] + "）", two[1] == 50);

        // 譯文比原文寬的時候要反過來收窄，否則右欄會被推出去。
        int[] wide = LineTranslator.columnDrift(
                List.of(0, 40), List.of(0, 64), new int[] {10, 30});
        check("譯文變寬時欄距收窄（實際 " + wide[1] + "）", wide[1] == -24);

        // 負的間隔是疊字用的，一律原樣；差額留給後面第一個正的間隔。
        int[] stacked = LineTranslator.columnDrift(
                List.of(0, 90, 20, 20), List.of(0, 40, 20, 20),
                new int[] {33, -23, 40});
        check("負間隔不動（實際 " + stacked[1] + "）", stacked[1] == 0);
        check("差額留到後面的正間隔（實際 " + stacked[2] + "）", stacked[2] == 50);

        // 只有縮排、沒有第二欄的行完全不受影響。
        int[] one = LineTranslator.columnDrift(
                List.of(0, 107), List.of(0, 38), new int[] {101});
        check("單欄的行不動（實際 " + java.util.Arrays.toString(one) + "）",
              one.length == 1 && one[0] == 0);
    }

    private static int rows(Component line) {
        return (int) line.getString().chars().filter(c -> c == '\n').count() + 1;
    }

    /** 每一行開頭那段偏移字元解出來的寬度。 */
    private static int[] leads(Component line) {
        List<Integer> out = new ArrayList<>();
        int px = 0;
        boolean counting = true;
        for (Component part : flatten(line)) {
            if (!part.getSiblings().isEmpty()) {
                continue;                      // 上層節點，字會重複算
            }
            String text = part.getString();
            if (text.isEmpty()) {
                continue;
            }
            if (text.indexOf('\n') >= 0) {
                out.add(px);
                px = 0;
                counting = true;
                continue;
            }
            if (!counting) {
                continue;
            }
            if (SpaceOffset.isOffsetRun(text)) {
                px += SpaceOffset.decode(text);
            } else {
                counting = false;              // 碰到實字，這一行的縮排數完了
            }
        }
        out.add(px);
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = out.get(i);
        }
        return arr;
    }

    private static List<Component> flatten(Component root) {
        List<Component> out = new ArrayList<>();
        out.add(root);
        for (Component child : root.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("聊天對齊：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("聊天對齊：全部通過");
    }
}
