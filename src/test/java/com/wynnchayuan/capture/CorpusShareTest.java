package com.wynnchayuan.capture;

/**
 * 哪些收集到的字串可以分享出去。
 *
 * <h2>為什麼這條測試特別重要</h2>
 * 其他測試錯了，畫面會怪；這一條錯了，是<b>別人的名字被推上公開倉庫</b>，
 * 而那種東西一旦進去就洗不掉。所以這裡釘的不只是「該過的有過」，
 * 更是「該擋的一條都沒漏」。
 *
 * <p>濾網一共三道：模組端（{@link CorpusUpload#shareable}，就是這裡測的）、
 * 收集站（{@code tools/collector/worker.js} 的 {@code acceptable}）、
 * 以及進倉庫前的 {@code tools/import-captured.py}。三道都是獨立寫的，
 * 刻意重複——只要有一道擋得住就不會外流。
 */
public final class CorpusShareTest {

    private static int failures = 0;

    private static CaptureStore.Captured one(String src, String ctx) {
        return new CaptureStore.Captured(src, "desc", "quest", ctx, 0);
    }

    public static void main(String[] args) {
        yes("任務對話", "Good luck in there, recruits!", "dialogue/King's Recruit");
        yes("介面文字", "Available Points", "gui/line");
        yes("物品說明", "This item's power has been sealed.", "tooltip/lore");
        yes("伺服器公告", "{#} Found a bug? Use /bug to report it!", "chat/INFO");
        yes("NPC 名牌", "Otium", "label/floating");
        yes("NPC 名牌帶地名", "{p}'s King", "label/floating");

        // ---- 別人打的字：整類不送 ----
        no("公會頻道", "anyone want to do a raid", "chat/GUILD");
        no("隊伍頻道", "im at the bank", "chat/PARTY");
        no("喊話", "WTS mythic cheap", "chat/SHOUT");
        no("私訊", "hey are you there", "chat/PRIVATE");
        no("沒標明來源的聊天", "something", "chat/");

        // ---- 夾帶個資 ----
        no("公會標籤", "- Cloud Tavern [CTRN]", "gui/line");
        no("領地控制", "Controlled by Paladins United", "label/floating");
        no("製作者署名", "Crafted by SomeOne", "tooltip/lore");
        no("帳號名形狀的名牌", "Green_teaTW", "label/floating");
        no("駝峰帳號名的名牌", "PoorChaCha", "npc/nametag");

        // ---- 形狀不對 ----
        no("空字串", "", "dialogue/X");
        no("純符號", "{#}{#}", "gui/line");
        no("太長", "x".repeat(601) + " word", "dialogue/X");

        // ---- 自己的名字（含暱稱）----
        SelfNames.remember("WYNNCHAYUAN");
        no("自己的暱稱出現在對話裡",
                "Hey, WYNNCHAYUAN! Are you alright in there?", "dialogue/X");
        no("大小寫不同也要擋",
                "Great job, Wynnchayuan!", "dialogue/X");
        yes("別句照樣可以送", "Great job, recruit!", "dialogue/X");

        report();
    }

    private static void yes(String what, String src, String ctx) {
        check("可以分享：" + what, CorpusUpload.shareable(one(src, ctx)));
    }

    private static void no(String what, String src, String ctx) {
        check("★ 不可分享：" + what, !CorpusUpload.shareable(one(src, ctx)));
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "CorpusShare: 全部通過" : "CorpusShare: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
