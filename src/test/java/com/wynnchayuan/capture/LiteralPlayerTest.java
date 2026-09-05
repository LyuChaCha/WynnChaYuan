package com.wynnchayuan.capture;

import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * 遊戲印出字面的「Player」時，那幾句台詞還是要翻得出來。
 *
 * <h2>怎麼發現的</h2>
 * 使用者回報「新手任務還有缺失的對話」。把那一份 capture 的 81 句沒翻的
 * 逐句對回語料，其中 <b>11 句早就翻好了</b>——它們只差在畫面上那個位置是
 * 字面的 {@code Player} 而不是玩家的名字：
 *
 * <pre>
 *   畫面：Alright! Let's go, Player.
 *   語料：Alright! Let's go, {u}.        （已翻：好！我們走，{u}。）
 * </pre>
 *
 * <p>同一次遊玩、同一個任務裡，別的台詞帶的是真名（{@code Hey, Arclass_!}），
 * 而且那幾句翻得出來——所以不是我們的名字偵測壞了，是 Wynncraft 那幾句
 * 就送字面的「Player」。這是新手任務，每個新玩家都會撞到。
 *
 * <h2>這條測試在盯什麼</h2>
 * 一邊要接得起來，另一邊<b>不能誤傷</b>：語料裡有五條正經的介面字帶著這個字
 * （{@code Player Slot}、{@code Max Player Count}、{@code Looking for a Player...}），
 * 那些不是名字。防線是「只在整行查不到時才改」，而這裡守的是更前面一層：
 * 複數與複合字本來就不該被當成名字。
 */
public final class LiteralPlayerTest {

    private static int failures = 0;

    /** 只有模板的空殼——{@code namingPlayer} 只看模板與 users，其餘用不到。 */
    private static LineParts bare(String template) {
        return new LineParts(template, List.of(), List.of(), List.of(),
                             List.of(), List.of(), Style.EMPTY, List.of());
    }

    public static void main(String[] args) {
        // 新手任務實際遇到的那幾句
        renames("Alright! Let's go, Player.", "Alright! Let's go, {u}.");
        renames("Great job, Player!", "Great job, {u}!");
        renames("Hey, Tasim! Come over here, Player found a way over!",
                "Hey, Tasim! Come over here, {u} found a way over!");
        renames("Player, you take the skeleton in the front.",
                "{u}, you take the skeleton in the front.");

        // 名字要跟著收成片段，不然填回去那個位置會變成空的
        LineParts named = bare("Great job, Player!").namingPlayer();
        report("名字收成了片段（實際 " + named.users().size() + " 個）",
                named.users().size() == 1);
        report("片段的內容就是畫面上那個字（實際 "
                        + (named.users().isEmpty() ? "無" : named.users().get(0).text()) + "）",
                !named.users().isEmpty() && "Player".equals(named.users().get(0).text()));

        // 一句話裡兩個都要收
        LineParts twice = bare("Player, tell Player to wait.").namingPlayer();
        report("兩個都收（實際 " + twice.users().size() + " 個）", twice.users().size() == 2);

        // 反面：這些不是名字
        keeps("Players", "Players online: {~}");
        keeps("複合字", "Playerbase");
        keeps("小寫", "the player must choose");

        // 反面：本來就有 {u} 的行不動——兩種來源混在一起，順序會錯開
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
