package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DialogueNumberTest {

    private static final int STILL_EVENTS = 12;
    private static final int WIDTH = 232;
    private static final int ROWS = 3;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-dialogue-number");
        set("config", new CollectorConfig(dir.resolve("config.json")));
        set("store", new CaptureStore(dir.resolve("captured.json")));
        TranslationStore store = store(dir);
        CurrentQuest.set(null);

        canReuseRule();
        both("You need 22 gold to enter.", "你需要 22 金幣才能進入。", store);
        both("You need 2 gold to enter.", "你需要 2 金幣才能進入。", store);
        both("The gate costs 1,250 gold.", "這道門要 1,250 金幣。", store);
        both("Bring me 15 wood and 200 stone.", "帶給我 15 木材和 200 石頭。", store);
        both("We leave in 30", "我們 30 後出發", store);
        both("Chance: 1.5", "機率：1.5", store);

        System.out.println(failures == 0 ? "DialogueNumber: 全部通過"
                : "DialogueNumber: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void canReuseRule() {
        String source = "You need {~} gold to enter.";
        String typed = "You need {~}";
        check("數字還在長就不沿用",
                !DialogueOverlay.canReuse(source, typed, "You need 2", "You need 22"));
        check("數字打完後下一幀也要重算一次",
                !DialogueOverlay.canReuse(source, "You need {~} ", "You need 22", "You need 22 "));
        check("小數點後面的數字也算同一個數字",
                !DialogueOverlay.canReuse(source, typed, "You need 1.", "You need 1.5"));
        check("文字沒變就沿用", DialogueOverlay.canReuse(source, typed, "You need 22", "You need 22"));
        check("一般打字照舊沿用",
                DialogueOverlay.canReuse(source, "You need {~} go", "You need 22 g", "You need 22 go"));
        check("換了一句不沿用",
                !DialogueOverlay.canReuse(source, typed, "Another line", "You need 22"));
    }

    private static void both(String line, String expected, TranslationStore store) {
        check("小框：" + line, expected.equals(panel(line, store)));
        check("就地取代：" + line, expected.equals(inPlace(line, store)));
    }

    private static String panel(String line, TranslationStore store) {
        DialogueOverlay.resetForTest();
        for (int at = 1; at <= line.length(); at++) {
            DialogueOverlay.setCurrent(StyledText.fromString(line.substring(0, at)), store);
        }
        for (int again = 0; again < STILL_EVENTS; again++) {
            DialogueOverlay.setCurrent(StyledText.fromString(line), store);
        }
        return text(DialogueOverlay.currentForTest());
    }

    private static String inPlace(String line, TranslationStore store) {
        DialogueTyping.forget();
        String out = null;
        for (int at = 1; at <= line.length(); at++) {
            out = DialogueTyping.line(line.substring(0, at), store, ROWS, null, WIDTH);
        }
        for (int again = 0; again < STILL_EVENTS; again++) {
            out = DialogueTyping.line(line, store, ROWS, null, WIDTH);
        }
        return out;
    }

    private static String text(List<Component> rows) {
        StringBuilder out = new StringBuilder();
        for (Component row : rows) {
            out.append(row.getString());
        }
        return out.toString();
    }

    private static TranslationStore store(Path dir) throws Exception {
        Path corpus = Files.createDirectories(dir.resolve("corpus"));
        Files.writeString(corpus.resolve("dialogue.json"), """
                {
                  "You need {~} gold to enter.": "你需要 {~} 金幣才能進入。",
                  "The gate costs {~} gold.": "這道門要 {~} 金幣。",
                  "Bring me {~} wood and {~} stone.": "帶給我 {~1} 木材和 {~2} 石頭。",
                  "We leave in {~}": "我們 {~} 後出發",
                  "Chance: {~}": "機率：{~}"
                }
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(corpus);
        return store;
    }

    private static void set(String field, Object value) throws Exception {
        var f = WynnChaYuan.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }

}
