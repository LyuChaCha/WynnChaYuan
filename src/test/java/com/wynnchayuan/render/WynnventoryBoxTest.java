package com.wynnchayuan.render;

/**
 * 譯文面板讓開 Wynnventory 的價格框。
 *
 * <p>畫面 480 寬。物品說明在 x=200、寬 100（200–300）；面板寬 90、間距 12：
 * 放右邊是 312，放左邊是 98。
 */
public final class WynnventoryBoxTest {

    private static int failures = 0;

    private static final int LEFT = 98;
    private static final int RIGHT = 312;
    private static final int W = 90;
    private static final int H = 60;
    private static final int Y = 100;
    private static final int GAP = 12;
    private static final int SCREEN = 480;

    public static void main(String[] args) {
        long now = 1_000_000_000L;

        WynnventoryBox.forget();
        check("沒有價格框：原樣", place(RIGHT, now) == RIGHT);

        // 價格框在物品說明右邊（它的預設）：318 起、寬 80
        WynnventoryBox.note(318, 96, 80, 70, now);
        check("★ 面板原本在右邊、跟價格框疊到：改放物品說明左邊（實際 " + place(RIGHT, now) + "）",
                place(RIGHT, now) == LEFT);
        check("面板本來就在左邊、沒疊到：原樣", place(LEFT, now) == LEFT);
        check("★ 價格框是很久以前記的（游標早就移開了）：不讓",
                place(RIGHT, now + 500_000_000L) == RIGHT);
        check("高度錯開、沒疊到：原樣",
                WynnventoryBox.place(RIGHT, 300, W, H, LEFT, RIGHT, SCREEN, GAP, now) == RIGHT);

        // 游標在畫面左側：物品說明左邊放不下（負的），只好放到價格框的外側
        WynnventoryBox.note(118, 96, 80, 70, now);      // 物品說明 0–100，價格框 118–198
        int beyond = WynnventoryBox.place(112, Y, W, H, -102, 112, SCREEN, GAP, now);
        check("★ 左邊放不下：改放價格框的右邊（實際 " + beyond + "）", beyond == 118 + 80 + GAP);

        // 兩邊都放不下：留在原位，不要跑出畫面、也不要蓋到物品說明
        WynnventoryBox.note(118, 96, 80, 70, now);
        int stuck = WynnventoryBox.place(112, Y, W, H, -102, 112, 260, GAP, now);
        check("★ 都放不下：留在原位（實際 " + stuck + "）", stuck == 112);

        // 價格框翻到物品說明左邊（右邊沒位置時它會這樣），面板也在左邊
        WynnventoryBox.note(110, 96, 80, 70, now);      // 110–190，物品說明 200–300
        check("★ 價格框在左邊、面板也在左邊：改放右邊（實際 " + place(LEFT, now) + "）",
                place(LEFT, now) == RIGHT);
        // 右邊也放不下（畫面只有 380）：放到價格框的更左邊
        int further = WynnventoryBox.place(LEFT, Y, W, H, LEFT, RIGHT, 380, GAP, now);
        check("★ 右邊放不下：放到價格框的左邊（實際 " + further + "）", further == 110 - GAP - W);

        // 它被固定在畫面邊上，剛好壓在物品說明的範圍裡：沒有「外側」，只試另一邊
        WynnventoryBox.note(290, 96, 80, 70, now);      // 跨在物品說明的右緣上
        check("價格框跨在物品說明上：只試另一邊（實際 " + place(RIGHT, now) + "）",
                place(RIGHT, now) == LEFT);

        WynnventoryBox.forget();
        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("WynnventoryBoxTest 全部通過");
    }

    private static int place(int x, long now) {
        return WynnventoryBox.place(x, Y, W, H, LEFT, RIGHT, SCREEN, GAP, now);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
