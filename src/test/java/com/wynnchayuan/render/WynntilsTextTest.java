package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 就地取代 Wynntils 疊層裡的字。
 *
 * <h2>為什麼要有這一份</h2>
 * 真正動手的是 mixin，而 mixin 只有在遊戲裡才跑得到。所以判斷與翻譯全部
 * 留在 {@link WynntilsText}，這裡直接呼叫它——關掉開關時一個字都不准動、
 * 打開時該翻的要翻、翻不出來的要<b>原樣</b>回去（換成空字串就是把
 * 別人的疊層弄壞）。
 */
public final class WynntilsTextTest {

    private static int failures = 0;

    /** 名字就是判斷依據（見 {@link WynntilsText#TRACKER}），所以照抄一個。 */
    private static final class ContentTrackerOverlay { }

    /** 別的疊層不准碰。 */
    private static final class InfoBoxOverlay { }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT));
        CollectorConfig config = new CollectorConfig(
                Files.createTempDirectory("wcy-overlay").resolve("config.json"));

        tracker(config, store);
        objectives(config, store);

        System.out.println(failures == 0 ? "\n就地取代：全部通過"
                : "\n就地取代：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void tracker(CollectorConfig config, TranslationStore store) {
        StyledText[] lines = {
            StyledText.fromString("Quest"),
            StyledText.fromString("Find the meteor's crash site."),
        };
        Object tracker = new ContentTrackerOverlay();

        while (config.trackerMode() != CollectorConfig.DialogueMode.REPLACE) {
            config.cycleTrackerMode();
        }
        StyledText[] out = WynntilsText.lines(tracker, lines, config, store);
        check("追蹤欄第一行翻得出來（實際 " + out[0].getString() + "）",
              !out[0].getString().equals("Quest"));
        check("行數不變", out.length == lines.length);

        // 追蹤欄的第一行是 Wynntils 拼的：「Quest - <任務名>」。整行不在語料裡，
        // 但每一段都在——類型是介面字串、任務名在 quest-name.json。
        StyledText composed = StyledText.fromString(
                "§b§lQuest§7 - §fStar Thief");
        StyledText[] header = WynntilsText.lines(
                tracker, new StyledText[] {composed}, config, store);
        String shown = header[0].getStringWithoutFormatting();
        check("「Quest - 任務名」兩段都換掉（實際 " + shown + "）",
              shown.contains("任務") && shown.contains("竊星者"));
        check("顏色沒有被抹掉", header[0].getString().contains("§b"));

        StyledText[] other = WynntilsText.lines(new InfoBoxOverlay(), lines, config, store);
        check("別的疊層原樣不動", other == lines);

        while (config.trackerMode() != CollectorConfig.DialogueMode.PANEL) {
            config.cycleTrackerMode();
        }
        check("面板模式下不動 Wynntils 的字",
              WynntilsText.lines(tracker, lines, config, store) == lines);

        while (config.trackerMode() != CollectorConfig.DialogueMode.OFF) {
            config.cycleTrackerMode();
        }
        check("關掉時不動 Wynntils 的字",
              WynntilsText.lines(tracker, lines, config, store) == lines);

        check("沒有設定或語料時原樣回去",
              WynntilsText.lines(tracker, lines, null, store) == lines
                      && WynntilsText.lines(tracker, lines, config, null) == lines);
    }

    private static void objectives(CollectorConfig config, TranslationStore store) {
        if (!config.translateObjectives()) {
            config.toggleObjectives();
        }
        String done = WynntilsText.objective("Slay Mobs: 12/100", config, store);
        check("目標那一條翻得出來（實際 " + done + "）", done.contains("擊殺怪物"));
        check("數字照抄", done.contains("12") && done.contains("100"));

        String odd = "Qwertyuiop: 1/2";
        check("翻不出來的原樣回去",
              WynntilsText.objective(odd, config, store).equals(odd));

        config.toggleObjectives();
        check("關掉時原樣回去",
              WynntilsText.objective("Slay Mobs: 12/100", config, store)
                      .equals("Slay Mobs: 12/100"));
        config.toggleObjectives();
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
