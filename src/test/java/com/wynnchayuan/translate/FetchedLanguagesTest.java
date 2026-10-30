package com.wynnchayuan.translate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public final class FetchedLanguagesTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        FetchedLanguages.reset();
        check("第一次切到某個語言要對", FetchedLanguages.firstTime("zh_tw"));
        check("★ 同一個語言第二次不再對", !FetchedLanguages.firstTime("zh_tw"));
        check("換一個語言又是第一次", FetchedLanguages.firstTime("tr_tr"));

        FetchedLanguages.reset();
        FetchedLanguages.mark("zh_cn");
        check("★ 啟動時背景同步抓過的語言，切過去不再對", !FetchedLanguages.firstTime("zh_cn"));

        check("空的語言代碼不算", !FetchedLanguages.firstTime("") && !FetchedLanguages.firstTime(null));

        FetchedLanguages.reset();
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
                if (FetchedLanguages.firstTime("ja_jp")) {
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

        System.out.println(failures == 0
                ? "FetchedLanguages: 全部通過" : "FetchedLanguages: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
