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

        // 小框要拿掉位移字、但圖示要留著。
        // 實機：dialogue-probe-1 的「Agh!」尾巴接一個 U+D0063，
        // 小框用預設字型畫就變成豆腐方塊（overlay-debug-2）。
        check("尾隨的位移字要拿掉",
                Boxes.dropOffsets("Agh!" + new String(Character.toChars(0xD0063))).equals("Agh!"));
        check("前導的也一樣",
                Boxes.dropOffsets(new String(Character.toChars(0xCFF8C)) + "Agh!").equals("Agh!"));
        check("私用區的圖示不能拿掉",
                Boxes.dropOffsets(" 空手右鍵").equals(" 空手右鍵"));
        // 等級徽章是「圖示 + 位移」交錯拼出來的，那些位移是用來定位圖示的。
        // 實機：majorid-debug 的「填回去的符號 7」——馬的名牌。
        // 1.99.40 一律拿掉位移，徽章就散成「馬 ⬛⬛ LV 1」。
        String badge = new String(Character.toChars(0xE060))
                + new String(Character.toChars(0xCFFFF))
                + new String(Character.toChars(0xE03B));
        check("徽章（圖示夾位移）整段不動",
                Boxes.dropOffsets(badge).equals(badge));
        check("位移加文字還是要拿掉",
                Boxes.dropOffsets("Agh!" + new String(Character.toChars(0xD0063)))
                        .equals("Agh!"));

        check("沒有位移字就原字串回去",
                Boxes.dropOffsets("啊！").equals("啊！"));

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

        // 玩家 ID 不能被切成兩半。前面是頓號、整行一個空白都沒有時，
        // 「退到上一個空白」沒得退，先前就切在 Green_ 和 teaTW 中間。
        List<String> id = DialogueRewriter.wrap("走吧！Tasim、Green_teaTW，來比", 2, null, 84);
        check("玩家 ID 不從中間切（實際：" + id + "）",
                id != null && id.get(0).equals("走吧！Tasim、")
                        && id.get(1).startsWith("Green_teaTW"));

        // 認出來的那一條，要撐得過「名字還在一個字一個字打」的那幾幀。
        //
        // 玩家回報的「中文 → 英文 → 中文」就是這裡：名字要整個打完才會收成
        // {u}，中間幾幀模板對不上，整句掉回英文，名字一打完又跳回中文。
        String says = "I don't suppose you've seen {u} around here?";
        check("整句還沒打完，是同一句",
                DialogueRewriter.within(says, "I don't suppose you've"));
        check("名字打到一半，還是同一句",
                DialogueRewriter.within(says, "I don't suppose you've seen Green_te"));
        check("名字打完收成佔位符，還是同一句",
                DialogueRewriter.within(says, "I don't suppose you've seen {u} around"));
        check("開頭就不一樣的，不是同一句",
                !DialogueRewriter.within(says, "Well met, traveller!"));
        // 岔在語料那條的<b>結尾之後</b> → 畫面上的字比它長，不是它。
        // 任務開始那則後面還接著進度顯示，要讓它往下走 join()。
        check("後面還接了別的東西，就不是同一句",
                !DialogueRewriter.within("New Quest Started: King's Recruit",
                        "New Quest Started: King's Recruit [{~}/{~} ({~}%)]"));
        check("岔開之後長到離譜的，不算",
                !DialogueRewriter.within(says,
                        "I don't suppose you've seen "
                                + "abcdefghijklmnopqrstuvwxyz0123456789"
                                + "abcdefghijklmnopqrstuvwxyz"));

        // 「還在打字」與「打完了」要有不同的態度。沒有現成的訊號，
        // 字停住幾幀就等於停住了。見 DialogueRewriter#settled。
        check("字還在長就不算停下來",
                !DialogueRewriter.settled("Bloc") && !DialogueRewriter.settled("Block"));
        boolean early = false;
        for (int i = 0; i < 5; i++) {
            early |= DialogueRewriter.settled("Block");
        }
        check("字與字之間的空檔不會被誤判成停下來", !early);
        boolean late = false;
        for (int i = 0; i < 4; i++) {
            late |= DialogueRewriter.settled("Block");
        }
        check("停夠久就算停下來了", late);
        check("字又長出來就重新計算", !DialogueRewriter.settled("Block t"));


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
