package com.wynnchayuan.capture;

import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ReferenceGateTest {

    private static int failures = 0;

    private static final Path ROOT =
            Path.of("src/main/resources/assets/wynnchayuan/translations");

    public static void main(String[] args) throws Exception {
        TranslationStore tw = new TranslationStore();
        tw.loadAll(ROOT.resolve(Languages.DEFAULT));

        TranslationStore cnOnly = new TranslationStore();
        cnOnly.loadAll(ROOT.resolve("zh_cn"));

        String picked = pick(tw, cnOnly);
        if (picked == null) {
            picked = pick(tw, new TranslationStore());
            cnOnly = new TranslationStore();
            cnOnly.loadAll(com.wynnchayuan.CorpusGap.without(ROOT.resolve("zh_cn"), picked));
        }
        final TranslationStore alone = cnOnly;
        check("找得到一句繁體有、簡體沒翻的台詞（" + picked + "）", picked != null);
        final String KNOWN = picked == null ? "" : picked;
        check("繁體本來就有這一句（" + tw.size() + " 條）", tw.hasTranslation(KNOWN));

        var keys = tw.sourceKeys();
        check("★ 拿得到繁體收過哪些原文（" + keys.size() + " 條）",
                keys.size() > 1000 && keys.contains(KNOWN));
        check("★ 只鋪簡體時畫面上查不到（輔助語言＝顯示原文的情境）",
                !alone.hasTranslation(KNOWN));

        Path dir = Files.createTempDirectory("wynnchayuan-refgate");
        CaptureStore store = new CaptureStore(dir.resolve("captured.json"));

        store.knowsTranslations(alone::hasTranslation);
        boolean recordedBefore = store.record(KNOWN, "desc", "gui", "gui/line");

        CaptureStore fixed = new CaptureStore(dir.resolve("fixed.json"));
        fixed.knowsTranslations(t -> alone.hasTranslation(t)
                || (t != null && keys.contains(t.strip())));
        boolean recordedAfter = fixed.record(KNOWN, "desc", "gui", "gui/line");

        check("舊行為會把它記成缺口（這一條紅了表示情境沒設對）", recordedBefore);
        check("★ 繁體已經有的句子不再記成缺口", !recordedAfter);

        String gap = "WynnChaYuan reference gate: nobody has ever seen this line";
        check("★ 繁體也沒有的句子照樣記得下來",
                fixed.record(gap, "desc", "gui", "gui/line"));

        report();
    }

    private static String pick(TranslationStore tw, TranslationStore other) {
        for (String key : new java.util.TreeSet<>(tw.sourceKeys())) {
            if (key.length() >= 20 && Character.isLetter(key.charAt(0)) && key.indexOf(' ') > 0
                    && key.indexOf('{') < 0 && key.indexOf('\n') < 0
                    && tw.hasTranslation(key) && !other.hasTranslation(key)) {
                return key;
            }
        }
        return null;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "收集的判準：全部通過"
                : "收集的判準：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
