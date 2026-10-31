package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LanguagePickTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        check("沒指定就跟著遊戲走",
                "zh_cn".equals(Languages.pick("", "zh_cn")));
        check("指名了就用指名的",
                "zh_cn".equals(Languages.pick("zh_cn", "zh_tw")));
        check("★ 指名的語言就算 jar 裡沒打包也照用"
                        + "（譯文是啟動時從 GitHub 抓的，不靠 jar）",
                "ja_jp".equals(Languages.pick("ja_jp", "zh_tw")));
        check("沒指定、遊戲語言也沒打包，退回預設",
                Languages.DEFAULT.equals(Languages.pick("", "de_de")));
        check("前後空白不算",
                "zh_cn".equals(Languages.pick("  zh_cn  ", "zh_tw")));

        check("jar 裡打包了簡體中文", Languages.bundled().contains("zh_cn"));
        check("繁體中文一定在（它是回退的底）",
                Languages.bundled().contains(Languages.DEFAULT));
        check("★ 簡體沒指定輔助語言就不墊繁體",
                Languages.underlay("", "zh_cn") == null);
        check("繁體沒指定輔助語言也不墊",
                Languages.underlay("", "zh_tw") == null);
        check("日文不拿中文墊底",
                Languages.underlay("", "ja_jp") == null);
        check("off 就不墊", Languages.underlay(Languages.OFF, "zh_cn") == null);
        check("指名了就照指名的",
                "ja_jp".equals(Languages.underlay("ja_jp", "zh_cn")));
        check("指名自己不墊", Languages.underlay("zh_cn", "zh_cn") == null);

        Path dir = Files.createTempDirectory("wynnchayuan-lang");
        Path file = dir.resolve("config.json");
        ModConfig first = new ModConfig(file);
        check("預設是空的（跟著遊戲走）", first.language().isEmpty());
        first.setLanguage("zh_cn");
        check("設完就讀得到", "zh_cn".equals(first.language()));

        ModConfig again = new ModConfig(file);
        check("★ 重開之後還在（這一條本來是壞的）",
                "zh_cn".equals(again.language()));

        again.setLanguage("");
        check("清回空字串也存得住",
                new ModConfig(file).language().isEmpty());

        check("輔助語言預設是自動（空字串）",
                again.fallbackLanguage().isEmpty());
        again.setFallbackLanguage("off");
        check("★ 選了『顯示原文』之後存得住",
                "off".equals(new ModConfig(file).fallbackLanguage()));
        again.setFallbackLanguage("zh_tw");
        check("指名一種語言也存得住",
                "zh_tw".equals(new ModConfig(file).fallbackLanguage()));
        again.setFallbackLanguage("");
        check("清回自動也存得住",
                new ModConfig(file).fallbackLanguage().isEmpty());

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
