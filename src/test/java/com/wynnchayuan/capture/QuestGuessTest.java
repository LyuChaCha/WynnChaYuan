package com.wynnchayuan.capture;

public final class QuestGuessTest {

    private static int failures = 0;

    public static void main(String[] args) {
        String quest = "Undersupply";

        check("Master Oshman（在名單上）掛任務名",
              QuestGuess.withQuest(quest, "Master Oshman", true, false));

        check("Marseled（不在名單上）不掛",
              !QuestGuess.withQuest(quest, "Marseled", false, false));

        check("Marseled 那段裡的旁白也不掛",
              !QuestGuess.withQuest(quest, null, null, false));

        check("回到 Master Oshman 又掛上",
              QuestGuess.withQuest(quest, "Master Oshman", true, false));
        check("他那段裡的旁白也掛",
              QuestGuess.withQuest(quest, null, null, false));

        check("點方塊開的日記不掛",
              !QuestGuess.withQuest(quest, null, null, true));

        check("新任務判斷不了就照舊掛",
              QuestGuess.withQuest("Brand New Quest", "Someone", null, false));

        check("沒在追蹤就沒有任務名",
              !QuestGuess.withQuest(null, "Master Oshman", true, false));

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
