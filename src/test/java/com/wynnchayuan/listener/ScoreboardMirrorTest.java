package com.wynnchayuan.listener;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ScoreboardMirrorTest {

    public static void main(String[] args) {
        check("追蹤任務那一段不抄", !ScoreboardListener.mirrors(
                "ActivityTrackerScoreboardPart"));
        check("★ Lootrun 那一段不抄", !ScoreboardListener.mirrors(
                "LootrunScoreboardPart"));

        for (String part : new String[] {"DailyObjectiveScoreboardPart",
                                         "GuildObjectiveScoreboardPart",
                                         "PartyScoreboardPart",
                                         "RaidScoreboardPart",
                                         "WarScoreboardPart",
                                         "GuildAttackScoreboardPart"}) {
            check(part + " 照抄", ScoreboardListener.mirrors(part));
        }

        check("認不出來的類別名照抄", ScoreboardListener.mirrors("SomethingNewPart"));
        check("拿不到類別名時照抄", ScoreboardListener.mirrors(null));

        extras();

        System.out.println(failures == 0
                ? "記分板分段：全部通過" : "記分板分段：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void extras() {
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("wcy-extras");
            com.wynnchayuan.CollectorConfig on =
                    new com.wynnchayuan.CollectorConfig(dir.resolve("on.json"));
            check("預設就是開著 Wynntils 介面", on.wynntilsUi());
            check("★ 開著時不補那幾段（不然隊伍清單會出現兩次）",
                  !com.wynnchayuan.render.TrackerOverlay.showsExtras(on));

            java.nio.file.Path off = dir.resolve("off.json");
            java.nio.file.Files.writeString(off, "{\"wynntilsUi\": false}");
            com.wynnchayuan.CollectorConfig closed =
                    new com.wynnchayuan.CollectorConfig(off);
            check("關掉之後讀得到 false", !closed.wynntilsUi());
            check("★ 關掉時照補（沒有別人會翻那一欄）",
                  com.wynnchayuan.render.TrackerOverlay.showsExtras(closed));

            check("拿不到設定時不補", !com.wynnchayuan.render.TrackerOverlay.showsExtras(null));
        } catch (Exception e) {
            check("測試自己跑得起來（" + e + "）", false);
        }
    }

}
