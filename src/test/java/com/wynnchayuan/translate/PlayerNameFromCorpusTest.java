package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.SelfNames;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/**
 * 從語料反推玩家的暱稱。
 *
 * <h2>實機回報</h2>
 * 「只要用 nickname 翻譯就會失效」。查了兩輪：
 *
 * <ol>
 *   <li>伺服器把名字轉大寫送出來（{@code Hey, WYNNCHAYUAN!}），照原樣比對落空
 *       ——已經改成不分大小寫。</li>
 *   <li>可是<b>根本沒學到暱稱</b>。診斷檔裡 {@code selfNames} 只有一筆 11 字元，
 *       那是帳號名 {@code Green_teaTW}；暱稱 {@code WYNNCHAYUAN} 不在任何一個
 *       Minecraft API 裡，玩家清單與實體的顯示名稱都拿不到。唯一有的來源是角色
 *       選單那一行「{@code - Nickname:}」——要玩家自己去開。</li>
 * </ol>
 *
 * <h2>釘住什麼</h2>
 * 語料自己就知道答案：鍵是「{@code Hey, {u}! Are you alright in there?…}」，
 * 畫面上是「{@code Hey, WYNNCHAYUAN! Are you alright in ther}」，前後都對得上，
 * 中間夾的就是名字。這一份釘住「夾得出來」、「證據不夠時寧可不認」，
 * 以及認出來之後模板真的會變成 {@code {u}}。
 */
public final class PlayerNameFromCorpusTest {

    private static int failures = 0;

    private static final String NICK = "WYNNCHAYUAN";

    /** 實機那一句，以及它逐字打到一半的樣子。 */
    private static final String FULL =
            "Hey, " + NICK + "! Are you alright in there? "
            + "It looks like we've hit something.";
    private static final String TYPING = "Hey, " + NICK + "! Are you alright in ther";

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                              "zh_tw"));

        // ★ 整句：夾出來的要就是名字，不能多吃驚嘆號、也不能少吃字母
        report("★ 整句夾得出暱稱（實際 " + store.playerNameIn(FULL) + "）",
               NICK.equals(store.playerNameIn(FULL)));

        // ★ 打到一半也要認得——對話框是一個字一個字長出來的
        report("★ 打到一半也夾得出來（實際 " + store.playerNameIn(TYPING) + "）",
               NICK.equals(store.playerNameIn(TYPING)));

        // 名字後面才打出一個驚嘆號：邊界還看不準，寧可不認
        String early = "Hey, " + NICK + "!";
        report("★ 證據不夠時不認（實際 " + store.playerNameIn(early) + "）",
               store.playerNameIn(early) == null);

        // 沒提到玩家的句子不能生出名字
        report("沒提到玩家就回 null",
               store.playerNameIn("Exit the caravan and talk to the Caravan Driver.")
                       == null);
        report("空字串不會爆", store.playerNameIn("") == null);

        // ★ 整份語料掃一次：沒有 {u} 的句子一條都不能被夾出名字。
        //
        // 這一條看的是<b>誤認</b>。反推是在查表之前跑的，所以每一句台詞都會經過
        // 它；只要有一句被夾出「名字」，那個字就會被記成玩家，往後所有句子裡的
        // 它都變成 {u}——比翻不出來糟糕得多。
        int wrong = 0;
        String worst = null;
        for (String key : store.keys()) {
            if (key.contains("{u}")) {
                continue;                  // 這些本來就該夾得出東西
            }
            String name = store.playerNameIn(key);
            if (name != null) {
                wrong++;
                worst = key + " → " + name;
            }
        }
        report("★ 三萬條語料沒有一句被誤認（誤認 " + wrong + " 條"
                       + (worst == null ? "" : "，例如 " + worst) + "）",
               wrong == 0);

        // ★ 認出來之後，模板要跟語料的鍵長得一樣
        SelfNames.remember(store.playerNameIn(TYPING));
        String template = LineParts.of(StyledText.fromComponent(
                Component.literal(FULL))).template();
        report("★ 之後模板抽成 {u}（實際 " + template + "）",
               ("Hey, {u}! Are you alright in there? "
                + "It looks like we've hit something.").equals(template));

        // 而且查得到——這一句語料裡本來就翻好了，先前只是對不上
        report("★ 查得到譯文（實際 " + store.lookup(template) + "）",
               store.lookup(template) != null);

        System.out.println(failures == 0
                ? "語料反推暱稱：全部通過"
                : "語料反推暱稱：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
