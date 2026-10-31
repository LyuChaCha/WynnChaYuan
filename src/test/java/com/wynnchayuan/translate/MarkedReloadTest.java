package com.wynnchayuan.translate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MarkedReloadTest {

    private static final String MET = "✔ Class Req: Mage/Dark Wizard";
    private static final String UNMET = "✖ Class Req: Mage/Dark Wizard";

    public static void main(String[] args) throws Exception {
        Path tw = Files.createTempDirectory("marked-tw");
        Path ja = Files.createTempDirectory("marked-ja");
        Files.writeString(tw.resolve("gui.json"),
                "{\"" + MET + "\": \"✔ 職業需求：法師/黑暗巫師\"}");
        Files.writeString(ja.resolve("gui.json"),
                "{\"Ability Points\": \"アビリティポイント\"}");

        TranslationStore store = new TranslationStore();
        store.loadAll(tw);
        check("載入繁中後，換了記號的那一行查得到",
                "✖ 職業需求：法師/黑暗巫師".equals(store.lookup(UNMET)));

        store.loadAll(ja);
        check("換成日文後，日文沒有的記號列不再給繁中譯文（✖）", store.lookup(UNMET) == null);
        check("換成日文後，日文沒有的記號列不再給繁中譯文（✔）", store.lookup(MET) == null);
        check("換成日文後，記號列算回缺口", !store.hasTranslation(UNMET));

        store.loadAll(List.of(tw, ja));
        check("疊層時墊底那層的記號列照樣查得到",
                "✖ 職業需求：法師/黑暗巫師".equals(store.lookup(UNMET)));

        System.out.println(failures == 0
                ? "換語言清掉記號索引：全部通過" : "換語言清掉記號索引：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

}
