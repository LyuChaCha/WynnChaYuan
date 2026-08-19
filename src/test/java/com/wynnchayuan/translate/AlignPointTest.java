package com.wynnchayuan.translate;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;

import java.util.List;

import static com.wynnchayuan.translate.LineTranslator.Piece;

/**
 * 驗證「差額要撐開哪一個空白」的判斷。
 *
 * <h2>為什麼單獨測這一段</h2>
 * 這裡選錯了，整行的<b>總寬度還是對的</b>——所以量寬度的自我檢查抓不到，
 * 只有肉眼盯著 tooltip 才看得出數值沒有靠到右邊。實際上就是這樣漏掉的：
 * 行尾有一段留白邊距，差額全灌進那裡，數值黏在標籤旁邊、右邊空一大塊。
 *
 * <p>寬度計算需要真的字型（{@code Minecraft.getInstance()}），headless 測不了，
 * 所以這裡只釘「選哪一個」，那也正是出錯的那一步。
 */
public final class AlignPointTest {

    private static int failures = 0;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Piece label = Piece.text("Class Type", Style.EMPTY);
        Piece value = Piece.text("Archer/Hunter", Style.EMPTY);
        Piece gap = Piece.space(80, Style.EMPTY);
        Piece margin = Piece.space(6, Style.EMPTY);

        // 標籤 [對齊空白] 數值 [右邊距] —— 要選中間那個，不是行尾那個
        check("行尾有留白時仍選中間的對齊空白",
                LineTranslator.findAlignPoint(List.of(label, gap, value, margin)) == 1);

        check("只有一個對齊空白時就選它",
                LineTranslator.findAlignPoint(List.of(label, gap, value)) == 1);

        // 置中的行：[空白] 文字 [空白]。撐開行尾那個只會把文字留在左邊
        int centered = LineTranslator.findAlignPoint(List.of(gap, value, margin));
        check("置中行選前導空白", centered == 0);
        check("前導空白被認出是置中（差額要減半）",
                LineTranslator.isLeading(List.of(gap, value, margin), centered));

        check("標籤在前時不算置中",
                !LineTranslator.isLeading(List.of(label, gap, value, margin), 1));

        // 沒有現成空白：從尾端往回找數值區的起點
        check("沒有對齊空白時找數值區起點",
                LineTranslator.findAlignPoint(List.of(
                        Piece.text("Combat Level", Style.EMPTY),
                        Piece.text("114", Style.EMPTY))) == 1);

        check("數值區沒有數字就不算欄位結構",
                LineTranslator.findAlignPoint(List.of(
                        Piece.text("Doom Stone", Style.EMPTY),
                        Piece.text("◆◆◆", Style.EMPTY))) == -1);

        // 空白全在行尾（純留白，後面沒有文字）——沒有東西需要撐開
        check("空白全在行尾時不當成對齊點",
                LineTranslator.findAlignPoint(List.of(
                        Piece.text("Weekly Objectives", Style.EMPTY), margin)) == -1);

        System.out.println(failures == 0
                ? "AlignPoint: 全部通過"
                : "AlignPoint: " + failures + " 項失敗");
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
