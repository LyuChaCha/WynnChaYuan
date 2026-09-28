package com.wynnchayuan.listener;

/**
 * 右上那一欄哪幾段該抄進我們的追蹤欄面板。
 *
 * <h2>實機回報（2026-09-28）</h2>
 * 「Lootrun 應該只待在旁邊記分板，但是追蹤欄也出現了一份 lootrun 翻譯。」
 *
 * <p>Lootrun 那一段有四、五行（{@code Lootrun: ／選擇一個信標！／- 剩餘時間: 05:28
 * ／- 挑戰: 1/12}），一進 Lootrun 就把面板上半部的追蹤任務擠掉，而同一份內容
 * 遊戲自己的記分板與 Wynntils 的 Lootrun 疊層上都看得到。
 *
 * <h2>這條測試在盯什麼</h2>
 * 決定寫在類別<b>簡名</b>上，而 Wynntils 改版換了類別名這一段就會默默恢復成
 * 「照收」。所以連反面一起釘：認不出來的段落照收（寧可多一段，也不要整欄
 * 憑空少東西），而每日目標那幾段不可以被順手一起擋掉。
 */
public final class ScoreboardMirrorTest {

    private static int failures = 0;

    public static void main(String[] args) {
        // 不抄：追蹤中的任務（TrackerListener 畫在同一個框的上半部）
        check("追蹤任務那一段不抄", !ScoreboardListener.mirrors(
                "ActivityTrackerScoreboardPart"));
        // 不抄：Lootrun（記分板本來就在）
        check("★ Lootrun 那一段不抄", !ScoreboardListener.mirrors(
                "LootrunScoreboardPart"));

        // 照抄：這幾段在畫面上沒有第二個地方看得到譯文
        for (String part : new String[] {"DailyObjectiveScoreboardPart",
                                         "GuildObjectiveScoreboardPart",
                                         "PartyScoreboardPart",
                                         "RaidScoreboardPart",
                                         "WarScoreboardPart",
                                         "GuildAttackScoreboardPart"}) {
            check(part + " 照抄", ScoreboardListener.mirrors(part));
        }

        // 認不出來的照抄：Wynntils 新增一段時，寧可多一段也不要整欄少東西
        check("認不出來的類別名照抄", ScoreboardListener.mirrors("SomethingNewPart"));
        check("拿不到類別名時照抄", ScoreboardListener.mirrors(null));

        System.out.println(failures == 0
                ? "記分板分段：全部通過" : "記分板分段：" + failures + " 項失敗");
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
