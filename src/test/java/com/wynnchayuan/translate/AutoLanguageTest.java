package com.wynnchayuan.translate;

public final class AutoLanguageTest {

    private static int failures = 0;

    public static void main(String[] args) {
        eq("選了韓文就是韓文", "ko_kr", Languages.pick("ko_kr", "ja_jp"));
        eq("選了繁中就是繁中", "zh_tw", Languages.pick("zh_tw", "ko_kr"));

        eq("跟隨韓文", "ko_kr", Languages.pick("", "ko_kr"));
        eq("跟隨日文", "ja_jp", Languages.pick("", "ja_jp"));
        eq("跟隨簡中", "zh_cn", Languages.pick("", "zh_cn"));
        eq("沒打包的語言退回預設", Languages.DEFAULT, Languages.pick("", "fr_fr"));
        eq("大小寫與連字號都要認得", "ko_kr", Languages.pick("", "KO-KR"));

        report("空字串不算問到", !Languages.known(""));
        report("null 不算問到", !Languages.known(null));
        report("沒打包的語言也不算", !Languages.known("fr_fr"));
        report("韓文算問到", Languages.known("ko_kr"));
        report("KO-KR 也算", Languages.known("KO-KR"));
        report("繁中算問到", Languages.known(Languages.DEFAULT));

        System.out.println(failures == 0
                ? "跟隨遊戲語言：全部通過"
                : "跟隨遊戲語言：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void eq(String what, String want, String got) {
        report(what + "（要 " + want + "，實際 " + got + "）", want.equals(got));
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
