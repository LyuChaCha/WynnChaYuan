package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class SelfNamesTest {

    private static int failures = 0;

    private static final String NICK = "0 QUEST WYNNCHAYUAN";

    public static void main(String[] args) {
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

        String after = template("Hey, " + NICK + "! Are you alright in there?");
        report("★ 暱稱抽成 {u}（實際 " + after + "）",
               "Hey, {u}! Are you alright in there?".equals(after));

        LineParts parts = LineParts.of(StyledText.fromComponent(
                Component.literal("Hey, " + NICK + "! Are you alright in there?")));
        String got = parts.users().isEmpty() ? null : parts.users().get(0).text();
        report("★ 抽出來的就是暱稱本身（實際 " + got + "）", NICK.equals(got));

        List<String> suffixes = SelfNames.suffixes("VIP " + NICK);
        report("整串在清單裡", suffixes.contains("VIP " + NICK));
        report("剝掉階級前綴之後也在清單裡（實際 " + suffixes + "）",
               suffixes.contains(NICK));

        SelfNames.learn("- Nickname: ab");
        report("★ 太短的名字不收（實際 " + SelfNames.all() + "）",
               !SelfNames.all().contains("ab"));

        SelfNames.learn("§6- Nickname: Not Defined");
        report("★ 「Not Defined」不收（實際 " + SelfNames.all() + "）",
               !SelfNames.all().contains("Not Defined"));

        SelfNames.learn("- Nickname: Wynnchayuan");
        String shouted = "Hey, WYNNCHAYUAN! Are you alright in there? "
                       + "It looks like we've hit something.";
        report("★ 大小寫不同也找得到（實際 " + SelfNames.find(shouted) + "）",
               "WYNNCHAYUAN".equals(SelfNames.find(shouted)));
        report("★ 大寫的名字抽成 {u}（實際 " + template(shouted) + "）",
               ("Hey, {u}! Are you alright in there? "
                + "It looks like we've hit something.").equals(template(shouted)));

        SelfNames.learn("- Nickname: Ash");
        report("★ 名字不會夾在單字中間命中（實際 "
                       + SelfNames.find("You see a bright flash of light.") + "）",
               SelfNames.find("You see a bright flash of light.") == null);
        report("整個詞出現時照樣找得到",
               "ASH".equals(SelfNames.find("Hey, ASH! Over here.")));

        report("★ 剝前綴不會剝出英文常用字（實際 "
                       + SelfNames.suffixes("Iamnot what") + "）",
               !SelfNames.suffixes("Iamnot what").contains("what"));
        report("整串還是收得到", SelfNames.suffixes("Iamnot what").contains("Iamnot what"));
        report("★ 常用字當不了名字", !SelfNames.usable("what")
                && !SelfNames.usable("They") && !SelfNames.usable("the"));
        report("正常的名字照收", SelfNames.usable("Arcla") && SelfNames.usable("Watari"));

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
