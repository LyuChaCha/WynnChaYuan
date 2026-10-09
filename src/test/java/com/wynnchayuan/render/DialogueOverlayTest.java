package com.wynnchayuan.render;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 驗證 Dialogue 逐字輸入時的 prefix 翻譯不會洩漏未還原的佔位符。 */
public final class DialogueOverlayTest {

    private static int failures = 0;
    private static final String SOURCE = "Please travel from {p} to {p} tomorrow.";

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-dialogue-test");
        Files.writeString(dir.resolve("dialogue.json"), """
                {
                  "entries": {
                    "test": {
                      "src": "Please travel from {p} to {p} tomorrow.",
                      "dst": "明天從 {p} 前往 {p}。"
                    }
                  }
                }
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        checkPrefix("尚未出現地名時不顯示 raw placeholder",
                "Please travel from", store, null, null);
        checkPrefix("只取得一個地名時仍等待",
                "Please travel from Troms to", store, null, null);
        checkPrefix("取得全部地名後還原 prefix 譯文",
                "Please travel from Troms to Detlas", store,
                "明天從 Troms 前往 Detlas。", SOURCE);
        checkPrefix("完整句與 prefix 顯示一致",
                "Please travel from Troms to Detlas tomorrow.", store,
                "明天從 Troms 前往 Detlas。", SOURCE);

        checkCacheReuse(store);
        checkKeepAlive();
        checkIntoScreen();

        System.out.println(failures == 0 ? "\n全部通過" : "\n失敗 " + failures + " 項");
        System.exit(failures == 0 ? 0 : 1);
    }

    /**
     * 原文還在畫面上的時候，小框不可以自己先淡掉（issue #1081）。
     *
     * <p>停留秒數從譯文最後一次更新起算，而一句話打完之後就不再更新。等玩家按
     * shift 的對話原文會一直留著，小框卻在六秒後淡掉、內容也跟著丟了。
     */
    private static void checkKeepAlive() {
        long now = System.currentTimeMillis();
        long old = now - 60_000;               // 一分鐘前打完的一句話
        int hold = 6000;
        check("前提：一分鐘前更新的小框，照停留秒數早就淡完了",
                Fade.alphaFor(old, hold) == 0f);
        check("★ 在等玩家按 shift：原文還在，小框就還在",
                Fade.alphaFor(DialogueOverlay.keptAlive(old, now, true, true, false), hold) == 1f);
        check("★ 就地取代模式：接手的小框不跟著停留秒數淡掉",
                Fade.alphaFor(DialogueOverlay.keptAlive(old, now, true, false, true), hold) == 1f);
        check("會自己往下講的對話照舊看停留秒數",
                DialogueOverlay.keptAlive(old, now, true, false, false) == old);
        check("小框裡沒東西就不必續",
                DialogueOverlay.keptAlive(old, now, false, true, true) == old);
    }

    /**
     * 存的位置在畫面外時，小框要被拉回來（issue #1081）。
     *
     * <p>回報的人平常用 1920×1009 的視窗（GUI 960×504）把對話小框擺在下方，
     * 錄影時把視窗縮成 1280×720（GUI 640×360）——存的 y 比整個畫面還高，
     * 小框照樣畫，只是畫在看不到的地方。
     */
    private static void checkIntoScreen() {
        int[] at = Boxes.intoScreen(380, 430, 200, 42, 640, 360);
        check("★ 存在畫面外的小框被拉回來（實際 " + at[0] + "," + at[1] + "）",
                at[0] >= 0 && at[0] + 200 <= 640 && at[1] >= 0 && at[1] + 42 <= 360);
        int[] same = Boxes.intoScreen(220, 250, 200, 42, 640, 360);
        check("本來就在畫面裡的不動", same[0] == 220 && same[1] == 250);
        int[] left = Boxes.intoScreen(-300, -50, 200, 42, 640, 360);
        check("往左上跑掉的也拉回來", left[0] >= 0 && left[1] >= 0);
        int[] huge = Boxes.intoScreen(100, 100, 900, 500, 640, 360);
        check("比畫面還大的貼著左上角，至少看得到開頭", huge[0] <= 2 && huge[1] <= 2);
    }

    private static void checkPrefix(String name, String raw, TranslationStore store,
                                    String expected, String expectedSource) {
        StyledText line = StyledText.fromString(raw);
        String template = GlyphSplitter.toTemplate(line);
        DialogueOverlay.LineResult result = DialogueOverlay.translateLine(line, template, store);
        String actual = result == null ? null : result.translated().getString();
        check(name + "（實際：" + actual + "）",
                expected == null ? actual == null : expected.equals(actual));
        String actualSource = result == null ? null : result.source();
        check(name + "－快取原文（實際：" + actualSource + "）",
                expectedSource == null
                        ? actualSource == null
                        : expectedSource.equals(actualSource));
    }

    /** prefix 顯示後，後續逐字輸入與完整句都應沿用同一筆快取。 */
    private static void checkCacheReuse(TranslationStore store) {
        StyledText partial = StyledText.fromString("Please travel from Troms to Detlas");
        String partialTemplate = GlyphSplitter.toTemplate(partial);
        DialogueOverlay.LineResult result =
                DialogueOverlay.translateLine(partial, partialTemplate, store);

        check("prefix 命中後可沿用快取",
                result != null && DialogueOverlay.canReuse(result.source(), partialTemplate));

        String completeTemplate = GlyphSplitter.toTemplate(
                StyledText.fromString("Please travel from Troms to Detlas tomorrow."));
        check("完整句仍沿用同一筆快取",
                result != null && DialogueOverlay.canReuse(result.source(), completeTemplate));

        check("不同 Dialogue 不會沿用舊快取",
                result != null && !DialogueOverlay.canReuse(
                        result.source(), "Please return to {p} immediately."));
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  OK  " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
