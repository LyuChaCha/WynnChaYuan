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

        // --- 補償之後的間隔不能把字疊在一起 -----------------------------
        // 正的間隔：中文變長就往回收，但收到 MIN_GAP 為止
        check("譯文變短時把正間隔撐開", LineTranslator.narrowed(80, 118) == 118);
        check("譯文變長時把正間隔收窄", LineTranslator.narrowed(80, 40) == 40);
        check("正間隔不會收到把字黏在一起", LineTranslator.narrowed(80, -30) == 6);

        // 負的間隔是 Wynncraft 自己的排版設計（數值往回貼進標籤尾巴的留白），
        // 不能夾在 MIN_GAP 以上——夾了整行會多出二十幾像素。
        check("負間隔照樣可以撐開（譯文比原文短）",
                LineTranslator.narrowed(-25, -4) == -4);
        check("負間隔可以撐到正的", LineTranslator.narrowed(-25, 12) == 12);
        // 但不能比原文<b>更</b>負。技能樹的 Ice Snake Cost 翻成
        // 「Ice Snake 消耗百分比」之後，數值被往回拉進標籤裡，
        // 畫面上是兩層字疊在一起，讀出來是「消耗百分5%」。
        check("負間隔不會被算得比原文更負 —— 那就是字疊在一起",
                LineTranslator.narrowed(-25, -60) == -25);
        check("負間隔維持原值時剛好等於原文", LineTranslator.narrowed(-25, -25) == -25);

        // --- 別的字型底下的偏移也要認得出來 -----------------------------
        // 素材與坐騎的 tooltip 走 language/wynncraft，那個字型也收了偏移碼位。
        // 只認 space 字型的話，間隔會被當成一般文字、譯文變短時不跟著調整——
        // 數值整排往左跑，坐騎的「右鍵點擊召喚」也失去置中。
        String offsets = new StringBuilder()
                .appendCodePoint(0xCFFD2).appendCodePoint(0xD0064).toString();
        int px = SpaceOffset.decode(offsets);
        Style wynn = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("language/wynncraft")));
        Style spaceFont = SpaceOffset.styleFor(Style.EMPTY);

        check("space 字型的偏移照舊認得（不必量寬度）",
                LineTranslator.isAdjustableSpace(spaceFont, offsets, 0));
        check("別的字型：量出來的寬度等於偏移值，就是偏移（" + px + " px）",
                LineTranslator.isAdjustableSpace(wynn, offsets, px));
        check("別的字型：寬度對不上就不當成偏移 —— 那可能是材質包的圖示",
                !LineTranslator.isAdjustableSpace(wynn, offsets, px + 7));
        check("範圍外的碼位一律不算",
                !LineTranslator.isAdjustableSpace(spaceFont, "Defence", 41));

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

        // 「Class Type␠␠Mage/Dark Wizard」：間隔是字面空格，數值又是文字，
        // 兩個條件都躲過前面的判斷，先前一路回傳 -1 完全沒被補償
        List<Piece> literalGap = List.of(
                Piece.text(" Class Type", Style.EMPTY),
                Piece.text("  ", Style.EMPTY),
                Piece.text("Mage/Dark Wizard", Style.EMPTY));
        check("字面空格當間隔、數值是文字，也要認得出交界",
                LineTranslator.findAlignPoint(literalGap) == 2);

        // 一格空格是詞距不是欄位。這裡仍然會回傳一個位置（數值區起點），
        // 擋下來的是呼叫端的 hasColumnGap ——「補不補」跟「交界在哪」是兩件事，
        // 分開判斷才不會把「找不到交界」和「找到了但不該補」混為一談。
        check("字面空格不足兩格時不算欄位（由 hasColumnGap 擋）",
                !LineTranslator.hasColumnGap(List.of(
                        Piece.text("Emerald Pouch", Style.EMPTY),
                        Piece.text(" ", Style.EMPTY),
                        Piece.text("[Tier 8]", Style.EMPTY)), 1));
        check("兩格以上就算欄位",
                LineTranslator.hasColumnGap(literalGap, 2));

        // 範圍值：「-2414 to -1300」是一個數值。停在 to 的話補償會灌進
        // to 後面，畫面上就成了「-2414 to      -1300」。
        List<Piece> range = List.of(
                Piece.text("Earth Main Attack Damage", Style.EMPTY),
                Piece.space(20, Style.EMPTY),
                Piece.text("-2414", Style.EMPTY),
                Piece.text(" to ", Style.EMPTY),
                Piece.text("-1300", Style.EMPTY));
        check("範圍值不會被從中間拆開",
                LineTranslator.findAlignPoint(range) == 1);

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
