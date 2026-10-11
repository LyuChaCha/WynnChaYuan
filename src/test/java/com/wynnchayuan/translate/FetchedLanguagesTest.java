package com.wynnchayuan.translate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 切語言時「這一次開遊戲對過了沒」的那張表。
 *
 * <p>實機回報（2026-10-10）：切到平常沒在用的語言，載入的是好幾個版本以前的快取，
 * 這兩天補的裝備名稱一個都沒有。修法是每種語言每次開遊戲在背景對一次——
 * 這裡守的是「只對一次」：少了會回到舊快取，多了就是每切一次抓三十幾個檔。
 */
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

        // 設定畫面連點兩下：兩條路同時問，只能有一邊去抓
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
