package com.wynnchayuan.capture;

import net.minecraft.network.chat.Style;

import java.util.List;

public final class LiteralPlayerTest {

    private static int failures = 0;

    private static LineParts bare(String template) {
        return new LineParts(template, List.of(), List.of(), List.of(),
                             List.of(), List.of(), Style.EMPTY, List.of());
    }

    public static void main(String[] args) {
        renames("Alright! Let's go, Player.", "Alright! Let's go, {u}.");
        renames("Great job, Player!", "Great job, {u}!");
        renames("Hey, Tasim! Come over here, Player found a way over!",
                "Hey, Tasim! Come over here, {u} found a way over!");
        renames("Player, you take the skeleton in the front.",
                "{u}, you take the skeleton in the front.");

        LineParts named = bare("Great job, Player!").namingPlayer();
        report("名字收成了片段（實際 " + named.users().size() + " 個）",
                named.users().size() == 1);
        report("片段的內容就是畫面上那個字（實際 "
                        + (named.users().isEmpty() ? "無" : named.users().get(0).text()) + "）",
                !named.users().isEmpty() && "Player".equals(named.users().get(0).text()));

        LineParts twice = bare("Player, tell Player to wait.").namingPlayer();
        report("兩個都收（實際 " + twice.users().size() + " 個）", twice.users().size() == 2);

        keeps("Players", "Players online: {~}");
        keeps("複合字", "Playerbase");
        keeps("小寫", "the player must choose");

        LineParts mixed = new LineParts("Hey {u}, Player is here.", List.of(), List.of(),
                List.of(), List.of(new LineParts.Piece("Steve", Style.EMPTY)),
                List.of(), Style.EMPTY, List.of());
        report("已經有 {u} 的行不動（實際：" + mixed.namingPlayer().template() + "）",
                "Hey {u}, Player is here.".equals(mixed.namingPlayer().template()));

        System.out.println(failures == 0
                ? "字面 Player：全部通過" : "字面 Player：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void renames(String from, String want) {
        String got = bare(from).namingPlayer().template();
        report("「" + from + "」-> 「" + want + "」（實際：" + got + "）", want.equals(got));
    }

    private static void keeps(String what, String template) {
        String got = bare(template).namingPlayer().template();
        report(what + "不當成名字：" + template + "（實際：" + got + "）", template.equals(got));
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
