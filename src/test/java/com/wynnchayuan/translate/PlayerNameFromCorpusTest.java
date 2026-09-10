package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.SelfNames;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        report("★ 整句夾得出暱稱（實際 " + name(store, FULL) + "）",
               NICK.equals(name(store, FULL)));

        // ★ 打到一半先不認：名字後面那一段還沒打完，看起來像什麼都有可能
        report("★ 打到一半先不認（實際 " + name(store, TYPING) + "）",
               name(store, TYPING) == null);

        // 名字後面才打出一個驚嘆號：邊界還看不準，寧可不認
        String early = "Hey, " + NICK + "!";
        report("★ 證據不夠時不認（實際 " + name(store, early) + "）",
               name(store, early) == null);

        // 沒提到玩家的句子不能生出名字
        report("沒提到玩家就回 null",
               store.playerNameIn("Exit the caravan and talk to the Caravan Driver.")
                       == null);
        report("空字串不會爆", store.playerNameIn("") == null);

        // ★ 實機那一句：打到「…out here, you're」時，語料裡某條 {u} 的鍵
        // 會把半句台詞夾出來當名字。v1.99.172 真的收了它，於是往後每一句
        // 含這幾個字的台詞都翻不出來。
        String halfLine = "If you want to survive out here, you're";
        report("★ 半句台詞不能被當成名字（實際 " + name(store, halfLine) + "）",
               name(store, halfLine) == null);

        // ★ 整份語料掃一次：沒有 {u} 的句子一條都不能被夾出名字。
        //
        // 這一條看的是<b>誤認</b>。反推是在查表之前跑的，所以每一句台詞都會經過
        // 它；只要有一句被夾出「名字」，那個字就會被記成玩家，往後所有句子裡的
        // 它都變成 {u}——比翻不出來糟糕得多。
        // ★ 整份語料<b>逐字模擬</b>掃一次：沒有 {u} 的句子一條都不能湊到兩票。
        //
        // 一定要逐字模擬，不能只拿整句去問。實機那個誤認就是打到
        // 「…out here, you're」那一幀發生的，整句反而夾不出東西——只查整句的
        // 版本掃過三萬條回報「零誤認」，隔天就在遊戲裡炸了。
        Map<String, Set<String>> votes = new HashMap<>();
        for (String key : store.keys()) {
            if (key.contains("{u}") || key.indexOf(10) >= 0) {
                continue;                  // 這些本來就該夾得出東西
            }
            for (int i = 4; i <= key.length(); i++) {
                TranslationStore.Guess guess = store.playerNameIn(key.substring(0, i));
                if (guess != null) {
                    votes.computeIfAbsent(guess.name(), n -> new HashSet<>())
                         .add(guess.source());
                    break;
                }
            }
        }
        List<String> settled = new ArrayList<>();
        for (Map.Entry<String, Set<String>> vote : votes.entrySet()) {
            if (vote.getValue().size() >= SelfNames.MIN_EVIDENCE) {
                settled.add(vote.getKey());
            }
        }
        report("★ 三萬條語料逐字模擬，沒有一個假名字被收（單票猜測 "
                       + votes.size() + " 種，被收 " + settled + "）",
               settled.isEmpty());

        // ★ 一條鍵不夠：要兩條不同的鍵說同一個答案才收。
        // NPC 名字湊不到兩條，玩家的名字則是每一句叫到他的台詞都夾得出來。
        // ★ 整句吻合就夠了：玩家第一次被叫到名字就認得出來，
        // 不必等第二句台詞——King&apos;s Recruit 開頭那幾句全是叫名字的，
        // 拖到第二句才定案，前面就整段留在英文。
        TranslationStore.Guess first = store.playerNameIn(FULL);
        report("整句夾得出來", first != null && NICK.equals(first.name()));
        SelfNames.propose(first.name(), first.source());
        report("★ 一句就收了（實際 " + SelfNames.all() + "）",
               SelfNames.all().contains(NICK));

        String second = "Woah! That sure did the trick, " + NICK + ". Good thinking!";
        TranslationStore.Guess again = store.playerNameIn(second);
        report("別句台詞也夾得出來（實際 " + (again == null ? null : again.name()) + "）",
               again != null && NICK.equals(again.name()));
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

    /** 夾出來的名字，沒有就是 {@code null}。 */
    private static String name(TranslationStore store, String raw) {
        TranslationStore.Guess guess = store.playerNameIn(raw);
        return guess == null ? null : guess.name();
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
