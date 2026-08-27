package com.wynnchayuan.render;

import java.util.List;

/**
 * 對話框的位移編碼與重算。
 *
 * <h2>為什麼要釘住這幾個數字</h2>
 * Wynncraft 的對話框是「往左退 N、畫字、再走回原位」拼出來的。換掉文字之後
 * 尾隨的位移<b>必須</b>照新寬度重算，否則後面的名牌、外框、頭像整塊偏掉——
 * 而且偏掉的畫面看起來像「框壞了」，不會有人聯想到是一個減法寫錯。
 *
 * <p>下面三組是從實機錄下來的（dialogue-probe），不是推測：
 * 文字 35px 時尾隨 +81、93px 時 +23、149px 時 -33，前導固定 -116。
 */
public final class DialogueOffsetTest {

    private static int failures = 0;

    public static void main(String[] args) {
        // 位移字元編碼／解碼要對得起來
        check("編碼 -116 再解回來還是 -116",
                DialogueRewriter.offsetOf(DialogueRewriter.offset(-116)) == -116);
        check("正的也一樣",
                DialogueRewriter.offsetOf(DialogueRewriter.offset(81)) == 81);
        check("0 也是合法的位移",
                DialogueRewriter.offsetOf(DialogueRewriter.offset(0)) == 0);

        // 實機資料：前導 -116，尾隨 = -(-116) - 寬度
        check("35px 的文字 → 尾隨 +81", -(-116) - 35 == 81);
        check("93px 的文字 → 尾隨 +23", -(-116) - 93 == 23);
        check("149px 的文字 → 尾隨 -33", -(-116) - 149 == -33);

        // 名牌置中：The Cook（寬 40）是 -60，Enzan（寬 25）是 -52
        check("The Cook 的前導是 -60", DialogueRewriter.nameLead(40) == -60);
        check("Enzan 的前導是 -52", DialogueRewriter.nameLead(25) == -52);

        // 普通文字不能被當成位移字元
        check("英文不是位移", DialogueRewriter.offsetOf("A") == null);
        check("中文不是位移", DialogueRewriter.offsetOf("廚") == null);
        check("兩個字元不是位移", DialogueRewriter.offsetOf("AB") == null);

        // 字型只有在畫不出來的時候才該換掉——換了就丟掉行號（ascent）
        check("英文畫得出來", DialogueRewriter.drawable("Let's hope no more grooks"));
        check("數字符號畫得出來", DialogueRewriter.drawable("+12% (3/5) [80.0%]"));
        check("西班牙文畫得出來", DialogueRewriter.drawable("Aumenta el dano magico"));
        check("法文重音畫得出來", DialogueRewriter.drawable("Augmente les degats"));
        check("中文畫不出來", !DialogueRewriter.drawable("希望別再有"));
        check("中英混排畫不出來", !DialogueRewriter.drawable("希望別再有 grook"));
        check("日文畫不出來", !DialogueRewriter.drawable("グルック"));
        check("諺文畫不出來", !DialogueRewriter.drawable("계속하려면"));
        check("西里爾畫不出來", !DialogueRewriter.drawable("продолжить"));

        // 折行處被吃掉的空格要補回來，否則跨行的台詞永遠查不到
        check("接第一行不補空格",
                DialogueRewriter.joinRow("", "Go on").equals("Go on"));
        check("接下一行補一個空格",
                DialogueRewriter.joinRow("My brother", "keeps sending")
                        .equals(" keeps sending"));
        check("前面已經有空白就不補",
                DialogueRewriter.joinRow("My brother ", "keeps")
                        .equals("keeps"));
        check("後面自己帶空白也不補",
                DialogueRewriter.joinRow("My brother", " keeps")
                        .equals(" keeps"));

        // 譯文跟著原文的進度一個字一個字出來
        check("打完就是全部",
                DialogueRewriter.typedSoFar("希望別再有人闖進來", 20, 20)
                        .equals("希望別再有人闖進來"));
        // 九個字打到一半 → round(9 × 0.5) = 5
        check("打一半就取一半",
                DialogueRewriter.typedSoFar("希望別再有人闖進來", 10, 20)
                        .equals("希望別再有"));
        check("一個字都還沒打就空的",
                DialogueRewriter.typedSoFar("希望別再有人闖進來", 0, 20)
                        .isEmpty());
        // 切在佔位符中間會讓那一行的佔位符數量對不上，整行靜靜不顯示
        check("不切在佔位符中間",
                DialogueRewriter.typedSoFar("你好 {~1} 位客人", 5, 10)
                        .equals("你好 "));
        check("佔位符整個進來了就留著",
                DialogueRewriter.typedSoFar("你好 {~1} 位", 8, 10)
                        .equals("你好 {~1}"));

        // 斷行要照<b>呼叫端算出來的寬度</b>，不能自己寫死。
        //
        // 有頭像的對話文字是從頭像右邊起算的，可用寬度少了 24 像素。先前
        // wrap() 收下寬度卻沒用，一律照最寬的算，中文就滿出框外。
        // 測試環境沒有字型，一個字算 6 像素。
        List<String> narrow = DialogueRewriter.wrap("abcdefgh", 2, null, 24);
        check("窄的時候一行只放得下四個字",
                narrow != null && narrow.get(0).equals("abcd")
                        && narrow.get(1).equals("efgh"));
        List<String> wide = DialogueRewriter.wrap("abcdefgh", 2, null, 48);
        check("寬的時候一行就放得完",
                wide != null && wide.get(0).equals("abcdefgh"));

        System.out.println(failures == 0
                ? "DialogueOffset: 全部通過"
                : "DialogueOffset: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
