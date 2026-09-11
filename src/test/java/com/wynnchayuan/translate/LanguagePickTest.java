package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 要用哪一種語言的譯文，以及那個選擇存不存得住。
 *
 * <h2>為什麼值得一條測試</h2>
 * {@code language} 這個設定原本是<b>死的</b>：欄位在、javadoc 在、getter 在，
 * 但存檔與讀檔都沒有它，所以怎麼改都沒有用。這種漏掉不會有任何錯誤訊息——
 * 改了設定檔、重開遊戲、畫面沒變，看起來就像「這個設定沒有效果」。
 *
 * <p>它的用途是校稿：譯者的 Minecraft 是繁中，但要看簡中翻得對不對，
 * 總不能為了看一眼就把整個遊戲切成簡體再切回來。
 */
public final class LanguagePickTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        // ---- 挑語言 ----
        check("沒指定就跟著遊戲走",
                "zh_cn".equals(Languages.pick("", "zh_cn")));
        check("指名了就用指名的",
                "zh_cn".equals(Languages.pick("zh_cn", "zh_tw")));
        check("★ 指名的語言就算 jar 裡沒打包也照用"
                        + "（譯文是啟動時從 GitHub 抓的，不靠 jar）",
                "ja_jp".equals(Languages.pick("ja_jp", "zh_tw")));
        check("沒指定、遊戲語言也沒打包，退回預設",
                Languages.DEFAULT.equals(Languages.pick("", "ru_ru")));
        check("前後空白不算",
                "zh_cn".equals(Languages.pick("  zh_cn  ", "zh_tw")));

        // ---- 簡中要能被選到 ----
        check("jar 裡打包了簡體中文", Languages.bundled().contains("zh_cn"));
        check("繁體中文一定在（它是回退的底）",
                Languages.bundled().contains(Languages.DEFAULT));
        check("簡體沒翻到的地方拿繁體墊底",
                Languages.DEFAULT.equals(Languages.fallbackFor("zh_cn")));
        check("日文不拿中文墊底——沒要求就滿畫面中文比留原文糟",
                Languages.fallbackFor("ja_jp") == null);

        // ---- 存得住嗎 ----
        Path dir = Files.createTempDirectory("wynnchayuan-lang");
        Path file = dir.resolve("config.json");
        CollectorConfig first = new CollectorConfig(file);
        check("預設是空的（跟著遊戲走）", first.language().isEmpty());
        first.setLanguage("zh_cn");
        check("設完就讀得到", "zh_cn".equals(first.language()));

        CollectorConfig again = new CollectorConfig(file);
        check("★ 重開之後還在（這一條本來是壞的）",
                "zh_cn".equals(again.language()));

        again.setLanguage("");
        check("清回空字串也存得住",
                new CollectorConfig(file).language().isEmpty());

        // ---- 輔助語言 ----
        check("輔助語言預設是自動（空字串）",
                again.fallbackLanguage().isEmpty());
        again.setFallbackLanguage("off");
        check("★ 選了『顯示原文』之後存得住",
                "off".equals(new CollectorConfig(file).fallbackLanguage()));
        again.setFallbackLanguage("zh_tw");
        check("指名一種語言也存得住",
                "zh_tw".equals(new CollectorConfig(file).fallbackLanguage()));
        again.setFallbackLanguage("");
        check("清回自動也存得住",
                new CollectorConfig(file).fallbackLanguage().isEmpty());

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
                ? "LanguagePick: 全部通過" : "LanguagePick: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
