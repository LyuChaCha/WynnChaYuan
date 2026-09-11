package com.wynnchayuan.capture;

import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 收集的缺口，判準是<b>繁體</b>有沒有，不是「我這一種語言有沒有」。
 *
 * <h2>使用者怎麼說的</h2>
 * 「繁體中文若有缺失才補進，否則其他語言看到英文只是沒翻譯並沒收集」。
 *
 * <p>簡體玩家看到英文，多半只代表簡體還沒翻——那一句繁體早就收過、也早就
 * 翻好了。把它記成缺口，{@code captured.json} 就會被別人早就收過的東西塞滿，
 * 而真正沒人遇過的句子淹在裡面。
 *
 * <h2>什麼時候會出事</h2>
 * 平常不會：簡體底下墊著繁體，查得到就不會記。出事的是<b>輔助語言設成
 * 「顯示原文」</b>那一刻——繁體那一層不在了，於是<b>整份繁體語料</b>
 * 一句一句變成「缺口」。
 *
 * <p>所以這條測試把兩件事分開釘：畫面查不到（因為沒鋪那一層）是一回事，
 * 收集要不要記是另一回事。
 */
public final class ReferenceGateTest {

    private static int failures = 0;

    private static final Path ROOT =
            Path.of("src/main/resources/assets/wynnchayuan/translations");

    /**
     * 一句繁體翻好了、<b>簡體還沒翻</b>的台詞。
     *
     * <p>兩邊都有的句子測不出東西：簡體自己就查得到，舊行為也不會記。
     * 要挑的正是「繁體有、簡體沒有」那一種——那才是被誤記成缺口的那一批。
     */
    private static final String KNOWN =
            "Good luck in there, recruits! You're gonna need it...";

    public static void main(String[] args) throws Exception {
        TranslationStore tw = new TranslationStore();
        tw.loadAll(ROOT.resolve(Languages.DEFAULT));
        check("繁體本來就有這一句（" + tw.size() + " 條）", tw.hasTranslation(KNOWN));

        var keys = tw.sourceKeys();
        check("★ 拿得到繁體收過哪些原文（" + keys.size() + " 條）",
                keys.size() > 1000 && keys.contains(KNOWN));

        // ---- 模擬「輔助語言＝顯示原文」：畫面上只鋪簡體 ----
        TranslationStore alone = new TranslationStore();
        alone.loadAll(ROOT.resolve("zh_cn"));
        check("★ 只鋪簡體時畫面上查不到（輔助語言＝顯示原文的情境）",
                !alone.hasTranslation(KNOWN));

        Path dir = Files.createTempDirectory("wynnchayuan-refgate");
        CaptureStore store = new CaptureStore(dir.resolve("captured.json"));

        // 舊行為：判準是「畫面那一份」。繁體有、簡體沒有的句子會被當成缺口。
        store.knowsTranslations(alone::hasTranslation);
        boolean recordedBefore = store.record(KNOWN, "desc", "gui", "gui/line");

        CaptureStore fixed = new CaptureStore(dir.resolve("fixed.json"));
        // 新行為：畫面那一份查不到時，再問一次繁體收過沒有。
        fixed.knowsTranslations(t -> alone.hasTranslation(t)
                || (t != null && keys.contains(t.strip())));
        boolean recordedAfter = fixed.record(KNOWN, "desc", "gui", "gui/line");

        check("舊行為會把它記成缺口（這一條紅了表示情境沒設對）", recordedBefore);
        check("★ 繁體已經有的句子不再記成缺口", !recordedAfter);

        // ---- 反方向：繁體<b>也</b>沒有的才是真缺口 ----
        String gap = "WynnChaYuan reference gate: nobody has ever seen this line";
        check("★ 繁體也沒有的句子照樣記得下來",
                fixed.record(gap, "desc", "gui", "gui/line"));

        report();
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
