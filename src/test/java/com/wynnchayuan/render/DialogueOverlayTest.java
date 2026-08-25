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

        System.out.println(failures == 0 ? "\n全部通過" : "\n失敗 " + failures + " 項");
        System.exit(failures == 0 ? 0 : 1);
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
