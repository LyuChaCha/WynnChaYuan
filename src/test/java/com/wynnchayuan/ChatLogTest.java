package com.wynnchayuan;

import com.wynnchayuan.capture.ChatLog;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 「複製聊天」的緩衝區。
 *
 * <p>釘住兩件事：容量到了要丟最舊的（不是最新的），
 * 以及複製出來的內容要跟著「聊天訊息」的顯示模式走——
 * 畫面上看到什麼，複製起來就該是什麼。
 */
public final class ChatLogTest {

    private static int failures = 0;

    public static void main(String[] args) {
        ChatLog.clear();
        check("一開始是空的", ChatLog.size() == 0);

        ChatLog.add(lit("Hello"), lit("哈囉"));
        ChatLog.add(lit("World"), null);
        check("記了兩則", ChatLog.size() == 2);

        List<ChatLog.Entry> recent = ChatLog.recent();
        check("由新到舊", recent.get(0).original().getString().equals("World"));
        check("舊的排後面", recent.get(1).original().getString().equals("Hello"));

        // 空白的不收——分隔線、純排版符號那種複製了也沒用
        ChatLog.add(lit("   "), lit("有譯文但原文是空的"));
        ChatLog.add(null, lit("原文是 null"));
        check("空白與 null 都不收", ChatLog.size() == 2);

        // 顯示模式：畫面上看到什麼，複製起來就是什麼
        ChatLog.Entry both = new ChatLog.Entry(lit("Hello"), lit("哈囉"));
        check("只顯示原文就複製原文",
                both.forMode(false, false).equals("Hello"));
        check("就地取代就複製譯文",
                both.forMode(true, false).equals("哈囉"));
        check("兩份都顯示就兩份都複製",
                both.forMode(false, true).equals("Hello\n哈囉"));

        // 沒有譯文的（別人講的話、查不到的）永遠只給原文，
        // 不能因為模式是「就地取代」就複製出一個空字串
        ChatLog.Entry only = new ChatLog.Entry(lit("Player says hi"), null);
        check("沒譯文時就地取代仍給原文",
                only.forMode(true, false).equals("Player says hi"));
        check("沒譯文時兩份模式也只給原文",
                only.forMode(false, true).equals("Player says hi"));
        ChatLog.Entry blank = new ChatLog.Entry(lit("Player says hi"), lit("   "));
        check("譯文只有空白也當成沒有",
                blank.forMode(true, false).equals("Player says hi"));

        // 容量：滿了要丟最舊的
        ChatLog.clear();
        for (int i = 0; i < ChatLog.CAPACITY + 50; i++) {
            ChatLog.add(lit("line " + i), null);
        }
        check("不會超過容量", ChatLog.size() == ChatLog.CAPACITY);
        List<ChatLog.Entry> full = ChatLog.recent();
        check("留下的是最新的",
                full.get(0).original().getString().equals("line " + (ChatLog.CAPACITY + 49)));
        check("丟掉的是最舊的",
                full.get(full.size() - 1).original().getString().equals("line 50"));

        ChatLog.clear();
        check("清得掉", ChatLog.size() == 0);

        System.out.println(failures == 0
                ? "ChatLog: 全部通過" : "ChatLog: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static Component lit(String text) {
        return Component.literal(text);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
