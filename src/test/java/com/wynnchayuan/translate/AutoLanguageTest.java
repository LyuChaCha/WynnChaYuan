package com.wynnchayuan.translate;

import com.wynnchayuan.LanguageLayers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class AutoLanguageTest {

    public static void main(String[] args) throws Exception {
        fetchedLanguages();
        eq("選了韓文就是韓文", "ko_kr", Languages.pick("ko_kr", "ja_jp"));
        eq("選了繁中就是繁中", "zh_tw", Languages.pick("zh_tw", "ko_kr"));

        eq("跟隨韓文", "ko_kr", Languages.pick("", "ko_kr"));
        eq("跟隨日文", "ja_jp", Languages.pick("", "ja_jp"));
        eq("跟隨簡中", "zh_cn", Languages.pick("", "zh_cn"));
        eq("沒打包的語言退回預設", Languages.DEFAULT, Languages.pick("", "fr_fr"));
        eq("大小寫與連字號都要認得", "ko_kr", Languages.pick("", "KO-KR"));

        check("空字串不算問到", !Languages.known(""));
        check("null 不算問到", !Languages.known(null));
        check("沒打包的語言也不算", !Languages.known("fr_fr"));
        check("韓文算問到", Languages.known("ko_kr"));
        check("KO-KR 也算", Languages.known("KO-KR"));
        check("繁中算問到", Languages.known(Languages.DEFAULT));

        System.out.println(failures == 0
                ? "跟隨遊戲語言：全部通過"
                : "跟隨遊戲語言：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void eq(String what, String want, String got) {
        check(what + "（要 " + want + "，實際 " + got + "）", want.equals(got));
    }

    private static void fetchedLanguages() throws Exception {
        LanguageLayers.forgetFetched();
        check("第一次切到某個語言要對", LanguageLayers.firstFetch("zh_tw"));
        check("★ 同一個語言第二次不再對", !LanguageLayers.firstFetch("zh_tw"));
        check("換一個語言又是第一次", LanguageLayers.firstFetch("tr_tr"));

        LanguageLayers.forgetFetched();
        LanguageLayers.markFetched("zh_cn");
        check("★ 啟動時背景同步抓過的語言，切過去不再對", !LanguageLayers.firstFetch("zh_cn"));

        check("空的語言代碼不算", !LanguageLayers.firstFetch("") && !LanguageLayers.firstFetch(null));

        LanguageLayers.forgetFetched();
        AtomicInteger winners = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (LanguageLayers.firstFetch("ja_jp")) {
                    winners.incrementAndGet();
                }
            });
            threads[i].start();
        }
        go.countDown();
        for (Thread t : threads) {
            t.join();
        }
        check("★ 同時問八次只有一邊拿到「第一次」（實際 " + winners.get() + "）", winners.get() == 1);
    }
}
