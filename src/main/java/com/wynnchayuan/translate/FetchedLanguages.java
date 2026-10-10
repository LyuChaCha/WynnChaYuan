package com.wynnchayuan.translate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 這一次開遊戲以來，哪幾種語言的譯文已經跟 GitHub 對過了。
 *
 * <h2>實機回報（2026-10-10）：「切換語言之後物品翻譯都會失效」</h2>
 * 切語言是刻意<b>不下載</b>的（來回對照兩種語言的人不該每切一次就重抓三十幾個檔），
 * 載入的是那個語言資料夾裡手上有的那一份。問題是那一份可以很舊：背景同步只抓
 * 「目前這一疊」語言，平常沒在用的語言，快取就停在最後一次用它的那一天。
 * 回報的那一台，繁體的快取是兩個版本以前的——切過去之後，這兩天才補進去的
 * 裝備名稱一個都沒有，看起來就是「物品翻譯失效」，重開遊戲、等同步跑完才會好。
 *
 * <p>所以切到一個語言時，這一次開遊戲還沒對過的就在背景對一次（有變才重載），
 * 對過的不再抓。來回切的成本從「每切一次抓一次」變成「每種語言一次」，
 * 而第一次切過去看到的不會再是幾天前的東西。
 */
public final class FetchedLanguages {

    private FetchedLanguages() {}

    private static final Set<String> DONE = ConcurrentHashMap.newKeySet();

    /** 記下這一種已經對過（或正在對）。 */
    public static void mark(String lang) {
        if (lang != null && !lang.isBlank()) {
            DONE.add(lang);
        }
    }

    /**
     * 這一種是不是<b>第一次</b>被問到。是的話順手記下，所以同時問兩次只有一邊拿到 true。
     */
    public static boolean firstTime(String lang) {
        return lang != null && !lang.isBlank() && DONE.add(lang);
    }

    /** 給測試用。 */
    static void reset() {
        DONE.clear();
    }
}
