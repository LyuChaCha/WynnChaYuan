package com.wynnchayuan.capture;

public final class ShareFilterTest {

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

        no("公會頻道", "anyone want to do a raid", "chat/GUILD");
        no("隊伍頻道", "im at the bank", "chat/PARTY");
        no("喊話", "WTS mythic cheap", "chat/SHOUT");
        no("私訊", "hey are you there", "chat/PRIVATE");
        no("沒標明來源的聊天", "something", "chat/");

        no("公會標籤", "- Cloud Tavern [CTRN]", "gui/line");
        no("領地控制", "Controlled by Paladins United", "label/floating");
        no("製作者署名", "Crafted by SomeOne", "tooltip/lore");
        no("帳號名形狀的名牌", "Green_teaTW", "label/floating");
        no("駝峰帳號名的名牌", "PoorChaCha", "npc/nametag");

        no("空字串", "", "dialogue/X");
        no("純符號", "{#}{#}", "gui/line");
        no("太長", "x".repeat(601) + " word", "dialogue/X");

        SelfNames.remember("WYNNCHAYUAN");
        no("自己的暱稱出現在對話裡",
                "Hey, WYNNCHAYUAN! Are you alright in there?", "dialogue/X");
        no("大小寫不同也要擋",
                "Great job, Wynnchayuan!", "dialogue/X");
        yes("別句照樣可以收", "Great job, recruit!", "dialogue/X");

        report();
    }

    private static void yes(String what, String src, String ctx) {
        check("可以交出去：" + what, ShareFilter.shareable(one(src, ctx)));
    }

    private static void no(String what, String src, String ctx) {
        check("★ 不可交出去：" + what, !ShareFilter.shareable(one(src, ctx)));
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "ShareFilter: 全部通過" : "ShareFilter: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
