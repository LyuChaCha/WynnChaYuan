package com.wynnchayuan.render;

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

        WynnventoryBox.note(318, 96, 80, 70, now);
        check("★ 面板原本在右邊、跟價格框疊到：改放物品說明左邊（實際 " + place(RIGHT, now) + "）",
                place(RIGHT, now) == LEFT);
        check("面板本來就在左邊、沒疊到：原樣", place(LEFT, now) == LEFT);
        check("★ 價格框是很久以前記的（游標早就移開了）：不讓",
                place(RIGHT, now + 500_000_000L) == RIGHT);
        check("高度錯開、沒疊到：原樣",
                WynnventoryBox.place(RIGHT, 300, W, H, LEFT, RIGHT, SCREEN, GAP, now) == RIGHT);

        WynnventoryBox.note(118, 96, 80, 70, now);
        int beyond = WynnventoryBox.place(112, Y, W, H, -102, 112, SCREEN, GAP, now);
        check("★ 左邊放不下：改放價格框的右邊（實際 " + beyond + "）", beyond == 118 + 80 + GAP);

        WynnventoryBox.note(118, 96, 80, 70, now);
        int stuck = WynnventoryBox.place(112, Y, W, H, -102, 112, 260, GAP, now);
        check("★ 都放不下：留在原位（實際 " + stuck + "）", stuck == 112);

        WynnventoryBox.note(110, 96, 80, 70, now);
        check("★ 價格框在左邊、面板也在左邊：改放右邊（實際 " + place(LEFT, now) + "）",
                place(LEFT, now) == RIGHT);
        int further = WynnventoryBox.place(LEFT, Y, W, H, LEFT, RIGHT, 380, GAP, now);
        check("★ 右邊放不下：放到價格框的左邊（實際 " + further + "）", further == 110 - GAP - W);

        WynnventoryBox.note(290, 96, 80, 70, now);
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
