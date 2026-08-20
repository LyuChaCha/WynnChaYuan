package com.wynnchayuan;

import com.wynnchayuan.translate.SpaceOffset;

/**
 * 驗證排版偏移的判斷與編解碼。
 *
 * <p>這一段錯了，畫面上的表現是「欄位間隔憑空消失」或「圖示變方框」——
 * 兩者都完全看不出跟空白字元有關，前後追了好幾版才定位到。
 */
public final class SpaceOffsetTest {

    private static final int ZERO = 0xD0000;

    private static int failures = 0;

    private static String cp(int offset) {
        return new String(Character.toChars(ZERO + offset));
    }

    public static void main(String[] args) {
        // --- 認得出來的 ---------------------------------------------------
        check("正偏移", SpaceOffset.isOffsetRun(cp(104)));
        check("負偏移（Wynncraft 實際會用，例如 U+CFFE7）",
                SpaceOffset.isOffsetRun(cp(-25)));
        check("0 像素偏移 —— 未鑑定物品第一行的置中就卡在這裡",
                SpaceOffset.isOffsetRun(cp(0)));
        check("多個字元組成的偏移 —— 配方那種一行兩欄的間隔",
                SpaceOffset.isOffsetRun(cp(40) + cp(-6) + cp(12)));

        // --- 不該誤認的 ---------------------------------------------------
        check("空字串不算", !SpaceOffset.isOffsetRun(""));
        check("普通文字不算", !SpaceOffset.isOffsetRun("Health"));
        check("範圍外的碼位不算（材質包自訂圖示）",
                !SpaceOffset.isOffsetRun(""));
        check("一半是偏移一半是圖示，整段都不算",
                !SpaceOffset.isOffsetRun(cp(40) + ""));

        // --- 編解碼 -------------------------------------------------------
        check("解正偏移", SpaceOffset.decode(cp(104)) == 104);
        check("解負偏移", SpaceOffset.decode(cp(-25)) == -25);
        check("多字元會加總", SpaceOffset.decode(cp(40) + cp(-6)) == 34);
        check("編負偏移不再是空字串 —— 先前這裡等於把整個間隔刪掉",
                !SpaceOffset.encode(-25).isEmpty());
        check("編回去解得出同一個值", SpaceOffset.decode(SpaceOffset.encode(-25)) == -25);
        check("0 仍然不必畫", SpaceOffset.encode(0).isEmpty());

        System.out.println(failures == 0
                ? "SpaceOffset: 全部通過" : "SpaceOffset: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
