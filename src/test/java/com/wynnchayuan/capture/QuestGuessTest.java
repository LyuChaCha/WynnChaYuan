package com.wynnchayuan.capture;

/**
 * 對話要不要掛追蹤中的任務名（issue #1138）。
 *
 * <p>情境照實機那一趟：追著 Undersupply，順路跟 Marseled 聊天、跑完 Solace in Pain
 * （日記是右鍵點方塊開的），中間也跟 Undersupply 自己的 Master Oshman 講話。
 */
public final class QuestGuessTest {

    private static int failures = 0;

    public static void main(String[] args) {
        String quest = "Undersupply";

        // 1. 任務自己的人
        check("Master Oshman（在名單上）掛任務名",
              QuestGuess.withQuest(quest, "Master Oshman", true, false));

        // 2. 不在名單上的 NPC
        check("Marseled（不在名單上）不掛",
              !QuestGuess.withQuest(quest, "Marseled", false, false));

        // 3. 同一段對話裡沒有名牌的句子，跟著前一句
        check("Marseled 那段裡的旁白也不掛",
              !QuestGuess.withQuest(quest, null, null, false));

        // 4. 回到任務的人，之後的旁白跟著掛
        check("回到 Master Oshman 又掛上",
              QuestGuess.withQuest(quest, "Master Oshman", true, false));
        check("他那段裡的旁白也掛",
              QuestGuess.withQuest(quest, null, null, false));

        // 5. 右鍵點方塊開的日記：不是剛才那個 NPC 的話
        check("點方塊開的日記不掛",
              !QuestGuess.withQuest(quest, null, null, true));

        // 6. 語料裡沒有這個任務的名單（新任務）：判斷不了，照舊掛
        check("新任務判斷不了就照舊掛",
              QuestGuess.withQuest("Brand New Quest", "Someone", null, false));

        // 7. 沒在追蹤任務
        check("沒在追蹤就沒有任務名",
              !QuestGuess.withQuest(null, "Master Oshman", true, false));

        // 8. 隔太久的旁白不沿用前一段的判斷
        QuestGuess.withQuest(quest, "Marseled", false, false);
        QuestGuess.reset();
        check("重設之後沒有說話者的句子照舊掛",
              QuestGuess.withQuest(quest, null, null, false));

        report();
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures == 0) {
            System.out.println("任務歸屬：全部通過");
        } else {
            System.out.println("任務歸屬：" + failures + " 項失敗");
            System.exit(1);
        }
    }
}
