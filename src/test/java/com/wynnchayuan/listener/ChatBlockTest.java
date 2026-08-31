package com.wynnchayuan.listener;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

/**
 * 一次跳好幾行的伺服器訊息，譯文要等它跳完再一起出來。
 *
 * <h2>為什麼值得一條測試</h2>
 * 這件事只有「時機」，沒有輸出可以比對——譯文早送一拍就是中英交錯，
 * 晚送一秒就是玩家以為沒翻。而時機出錯在畫面上看起來跟「翻譯壞掉」
 * 一模一樣，回報回來的截圖分不出是哪一種。
 */
public final class ChatBlockTest {

    private static int failures = 0;

    private static StyledText line(String text) {
        return StyledText.fromComponent(Component.literal(text));
    }

    public static void main(String[] args) {
        ChatBlock.clear();
        check("一開始什麼都沒攢", ChatBlock.size() == 0);
        check("沒東西攢的時候不會送", !ChatBlock.ready(Long.MAX_VALUE));

        long now = 1_000_000L;
        ChatBlock.queue(line("[Quest Completed]"), Component.literal("[任務完成]"));
        ChatBlock.arrivedAt(now);
        check("收到一行就攢起來", ChatBlock.size() == 1);
        check("剛收到不送", !ChatBlock.ready(now));
        check("差一點也不送", !ChatBlock.ready(now + 199));
        check("安靜夠久才送", ChatBlock.ready(now + 200));

        // 整塊是同一個封包送來的，行與行之間不到一個 tick——
        // 每收到一行就要重新計時，不然第一行滿兩百毫秒就先跑掉了。
        ChatBlock.queue(line("King's Recruit"), Component.literal("國王的新兵"));
        ChatBlock.arrivedAt(now + 150);
        check("又來一行就重新計時", !ChatBlock.ready(now + 200));
        check("攢的是兩行", ChatBlock.size() == 2);
        check("從最後一行起算滿了才送", ChatBlock.ready(now + 350));

        // 查不到譯文的那幾行也要攢：它們是這一塊的一部分，
        // 少了它們整塊就接不回去，也就查不到整塊的那一條。
        ChatBlock.queue(line("- Rewards:"), null);
        check("查不到譯文的行也算一行", ChatBlock.size() == 3);

        // 聊天在洗版時不能無限攢下去
        ChatBlock.clear();
        for (int i = 0; i < 30; i++) {
            ChatBlock.queue(line("row " + i), Component.literal("第 " + i + " 行"));
        }
        check("洗版時不會無限攢（實際 " + ChatBlock.size() + " 行）",
                ChatBlock.size() > 0 && ChatBlock.size() <= 24);

        ChatBlock.clear();
        check("清掉之後是空的", ChatBlock.size() == 0);

        // ★ 一行都沒翻到的那一塊，一個字都不能送。
        //
        // 查不到譯文的行改成「原樣帶上」之後，這一塊就永遠有東西可送了。
        // 照送的話等於把伺服器的訊息原封不動再貼一遍——而原文並沒有被取消，
        // 畫面上就是每則訊息出現兩遍（實機回報：聊天欄一直洗頻）。
        check("有翻到就送", ChatBlock.worthSending(true, true));
        check("一行都沒翻到就不送", !ChatBlock.worthSending(true, false));
        check("沒東西可送當然不送", !ChatBlock.worthSending(false, false));

        System.out.println(failures == 0
                ? "ChatBlock: 全部通過" : "ChatBlock: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
