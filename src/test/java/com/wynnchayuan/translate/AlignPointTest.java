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
 * <p>素材的數值行有<b>兩個</b>對齊欄（最小值一欄、最大值一欄），只補其中一個
 * 的話另一欄就會跑掉——而且整行總寬度仍然是對的，所以量寬度也看不出來。
 * 這裡把「哪些空白算欄位交界」釘住。
 *
 * <p>寬度計算需要真的字型（{@code Minecraft.getInstance()}），headless 測不了，
 * 所以這裡只釘判斷規則，那也正是出錯的那一步。
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

        // --- 哪些空白算欄位交界 -----------------------------------------
        // 標籤 [對齊空白] 數值 [右邊距]
        List<Piece> labelValue = List.of(label, gap, value, margin);
        check("標籤與數值之間的空白算欄位交界",
                LineTranslator.isAlignSpace(labelValue, 1));
        check("行尾的留白邊距不算 —— 差額灌進那裡的話數值會黏在標籤旁",
                !LineTranslator.isAlignSpace(labelValue, 3));

        // 素材那種兩欄的行：標籤 [A] +60 to [B] +75
        // 只補其中一欄的話另一欄會跑掉，而整行總寬度仍然是對的
        List<Piece> twoColumns = List.of(
                Piece.text("Spell Damage", Style.EMPTY),
                Piece.space(60, Style.EMPTY),
                Piece.text("+60 to", Style.EMPTY),
                Piece.space(24, Style.EMPTY),
                Piece.text("+75", Style.EMPTY));
        check("兩欄的行兩個空白都算交界（第一欄）",
                LineTranslator.isAlignSpace(twoColumns, 1));
        check("兩欄的行兩個空白都算交界（第二欄）",
                LineTranslator.isAlignSpace(twoColumns, 3));

        // --- 置中／縮排 ---------------------------------------------------
        List<Piece> centered = List.of(gap, value, margin);
        check("前導空白不算欄位交界（動它會讓整行位移）",
                !LineTranslator.isAlignSpace(centered, 0));
        check("前導空白被認出是置中（差額要減半）",
                LineTranslator.isLeading(centered, 0));
        check("標籤在前時不算置中",
                !LineTranslator.isLeading(labelValue, 1));

        // --- 交界找在哪 ---------------------------------------------------
        // 有現成對齊空白時，交界就落在那個空白上（呼叫端看到是空白就不補）
        check("有現成對齊空白時交界落在空白上",
                LineTranslator.findAlignPoint(labelValue) == 1
                        && labelValue.get(1).isSpace());

        // 素材那種行：標籤與第一個數值之間沒有空白，只有兩個數值之間有。
        // 交界必須落在「第一個數值」上，呼叫端才會補進去；落在後面那個空白
        // 的話，差額會全部灌進第二欄，標籤和第一個數值就黏在一起。
        List<Piece> gapOnlyLater = List.of(
                Piece.text("Combat Experience   ", Style.EMPTY),
                Piece.text("+2% to", Style.EMPTY),
                Piece.space(20, Style.EMPTY),
                Piece.text("+7%", Style.EMPTY));
        int boundary = LineTranslator.findAlignPoint(gapOnlyLater);
        check("標籤後沒有空白時交界落在第一個數值（不是後面那個空白）",
                boundary == 1 && !gapOnlyLater.get(1).isSpace());

        check("沒有對齊空白時找數值區起點",
                LineTranslator.findAlignPoint(List.of(
                        Piece.text("Combat Level", Style.EMPTY),
                        Piece.text("114", Style.EMPTY))) == 1);

        check("數值區沒有數字就不算欄位結構",
                LineTranslator.findAlignPoint(List.of(
                        Piece.text("Doom Stone", Style.EMPTY),
                        Piece.text("◆◆◆", Style.EMPTY))) == -1);

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
