package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Wynncraft 的暱稱也要抽成 {@code {u}}。
 *
 * <h2>實機回報</h2>
 * 「有暱稱的情況會導致翻譯失效，例如我叫 0 Quest Wynnchayuan」。對話框送來的是
 *
 * <pre>
 *   Hey, 0 QUEST WYNNCHAYUAN! Are you alright in there?
 * </pre>
 *
 * <p>而 {@code LineParts} 只認得 Minecraft <b>帳號名</b>——暱稱跟帳號名毫無關係，
 * 可以有空格、可以數字開頭。於是 {@code {u}} 沒有命中，接著數字比對把開頭那個
 * {@code 0} 抽成 {@code {~}}：
 *
 * <pre>
 *   Hey, {~} QUEST WYNNCHAYUAN! Are you alright in there?
 * </pre>
 *
 * 語料的鍵是「{@code Hey, {u}! …}」，永遠對不上。這不是一句話的問題——
 * <b>每一句提到玩家名字的台詞</b>都會這樣，而 capture 裡也確實躺著這個壞掉的鍵。
 *
 * <h2>釘住什麼</h2>
 * 學到暱稱之後，模板要抽成 {@code {u}}，而且抽出來的那一段要<b>就是暱稱</b>
 * ——多吃或少吃一個字都會讓鍵對不上。
 */
public final class SelfNamesTest {

    private static int failures = 0;

    private static final String NICK = "0 QUEST WYNNCHAYUAN";

    public static void main(String[] args) {
        // 沒學到之前：認不出來，開頭的 0 會被當成數值
        String before = template("Hey, " + NICK + "! Are you alright in there?");
        report("★ 學到之前，暱稱抽不出來（實際 " + before + "）",
               !before.contains("{u}"));

        SelfNames.learn("- Nickname: " + NICK);

        List<String> all = SelfNames.all();
        report("學到的清單裡有暱稱（實際 " + all + "）", all.contains(NICK));
        report("find 找得到（實際 " + SelfNames.find("Hey, " + NICK + "!") + "）",
               NICK.equals(SelfNames.find("Hey, " + NICK + "!")));
        report("沒提到的句子不會誤中",
               SelfNames.find("Hey, traveller! Are you alright in there?") == null);

        // ★ 學到之後：模板要跟語料的鍵長得一樣
        String after = template("Hey, " + NICK + "! Are you alright in there?");
        report("★ 暱稱抽成 {u}（實際 " + after + "）",
               "Hey, {u}! Are you alright in there?".equals(after));

        // 抽出來的那一段要就是暱稱，不能多吃「Hey, 」或少吃開頭的 0
        LineParts parts = LineParts.of(StyledText.fromComponent(
                Component.literal("Hey, " + NICK + "! Are you alright in there?")));
        String got = parts.users().isEmpty() ? null : parts.users().get(0).text();
        report("★ 抽出來的就是暱稱本身（實際 " + got + "）", NICK.equals(got));

        // 階級前綴：顯示名稱可能是「VIP 0 QUEST WYNNCHAYUAN」，剝掉開頭的詞
        List<String> suffixes = SelfNames.suffixes("VIP " + NICK);
        report("整串在清單裡", suffixes.contains("VIP " + NICK));
        report("剝掉階級前綴之後也在清單裡（實際 " + suffixes + "）",
               suffixes.contains(NICK));

        // 太短的不收——一兩個字元在句子裡到處都是
        SelfNames.learn("- Nickname: ab");
        report("★ 太短的名字不收（實際 " + SelfNames.all() + "）",
               !SelfNames.all().contains("ab"));

        System.out.println(failures == 0
                ? "暱稱：全部通過" : "暱稱：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String template(String text) {
        return LineParts.of(StyledText.fromComponent(Component.literal(text))).template();
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
