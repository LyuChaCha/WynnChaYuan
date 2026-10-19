package com.wynnchayuan.render;

/**
 * Wynnventory 側邊框的擺法（{@link WynnventoryLayout#place}）。
 *
 * <p>實機回報（2026-10-19）：未鑑定物品的說明框寬到左右都放不下價格框，價格框被夾回畫面裡，
 * 整個壓在說明上。畫面 773×984（GUI 座標），說明框從 x=105 到 725。
 */
public final class WynnventoryLayoutTest {

    private static int failures = 0;

    public static void main(String[] args) {
        // ★ 截圖那一幕：說明框照中文量是 620 寬（照英文量窄得多，它以為右邊沒位置、左邊也不夠，
        // 夾回畫面就壓在說明上）。照實際寬度算，右邊放得下就放右邊，絕不壓在說明上。
        float[] spot = WynnventoryLayout.place(1100, 984, 105, 380, 620, 310, 900);
        check("★ 照實際寬度擺得下（實際 " + (spot == null ? "null" : "x=" + spot[0]) + "）",
              spot != null);
        if (spot != null) {
            float right = spot[0] + 310 * spot[2];
            check("★ 不壓在說明框上（框在 " + spot[0] + "–" + right + "，說明框 105–725）",
                  right <= 105 || spot[0] >= 725);
            check("不超出畫面", spot[0] >= 0 && right <= 1100);
        }
        // 兩邊都不夠、但縮一點就放得下：挑大的那邊縮小
        float[] squeezed = WynnventoryLayout.place(1000, 984, 105, 380, 620, 310, 400);
        check("兩邊都放不下時縮小（實際 " + (squeezed == null ? "null" : squeezed[2]) + "）",
              squeezed != null && squeezed[2] < 1f
                      && squeezed[0] >= 725 && squeezed[0] + 310 * squeezed[2] <= 1000);

        // 右邊放得下：放右邊、不縮
        float[] roomy = WynnventoryLayout.place(1000, 600, 100, 50, 300, 200, 200);
        check("右邊放得下就放右邊、原尺寸（實際 x=" + roomy[0] + " 縮放 " + roomy[2] + "）",
              roomy[0] == 100 + 300 + 18 && roomy[2] == 1f);

        // 右邊放不下、左邊放得下：放左邊
        float[] lefty = WynnventoryLayout.place(1000, 600, 500, 50, 400, 200, 200);
        check("右邊放不下就放左邊（實際 x=" + lefty[0] + "）", lefty[0] == 500 - 18 - 200);

        // 太高：照原本的規則縮，y 也要在畫面裡
        float[] tall = WynnventoryLayout.place(1000, 300, 100, 50, 300, 200, 600);
        check("太高就縮（實際 " + tall[2] + "）", tall[2] < 1f);
        check("縮了之後底部不出畫面", tall[1] + 600 * tall[2] <= 300);

        // 空間小到字看不清：不接手
        check("空間太小就交回它原本的方法",
              WynnventoryLayout.place(400, 600, 30, 50, 340, 300, 100) == null);

        if (failures > 0) {
            System.out.println("Wynnventory 側邊框：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("Wynnventory 側邊框：全部通過");
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            System.out.println("  [ok] " + what);
        } else {
            System.out.println("  [FAIL] " + what);
            failures++;
        }
    }
}
