package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
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

        // --- 負偏移是疊字，不是欄距 -------------------------------------
        // 原料袋的星級：灰色空星畫完之後用 -23px 退回起點，把彩色實星疊上去。
        // 那個值是照灰星的寬度挑的，跟前面的名字多長無關；被當成欄距改寫掉的話，
        // 彩星會被推到行尾，畫面上變成「一行有兩組星星」。
        Piece grey = Piece.text("★★★", Style.EMPTY);
        Piece back = Piece.space(-23, Style.EMPTY);
        Piece lit = Piece.text("★★★", Style.EMPTY);
        List<Piece> stars = List.of(Piece.text("1 x ", Style.EMPTY),
                Piece.text("Ripe Aureate Fruit", Style.EMPTY),
                grey, back, lit);
        check("負偏移不會被當成可調整的欄距",
                LineTranslator.alignColumns(stars, false).get(3).spacePx() == -23);
        check("正的欄距照常可以調整",
                LineTranslator.isAlignSpace(labelValue, 1));

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

        // --- 沒有欄的 tooltip 不該吃到右緣補償 ---------------------------
        // 右緣對齊是<b>一群行</b>一起對出來的。兩欄的行湊不滿三行時，這份
        // tooltip 根本沒有靠右的數值欄，卻仍然會被「把譯文縮水的部分補回
        // 最後一段間隔」處理到。洞穴卡的獎勵列就是這樣歪的：
        // 「- +1 [2px] Theatre Cane」的那個 2px 只是圖示與名稱之間的縫，
        // 譯名短了 38px 全部灌進去，手杖被推到行尾。
        check("空的一份：沒有欄，不補右緣",
                LineTranslator.columnsAreLeftAligned(List.of()));
        check("null 不會炸", LineTranslator.columnsAreLeftAligned(null));
        check("行數不足三行：沒有欄，不補右緣",
                LineTranslator.columnsAreLeftAligned(List.of(
                        com.wynntils.core.text.StyledText.fromString("Theatre Royal [Cave]"),
                        com.wynntils.core.text.StyledText.fromString("Can be explored"))));
        check("行數夠但沒有一行有欄距：一樣不補",
                LineTranslator.columnsAreLeftAligned(List.of(
                        com.wynntils.core.text.StyledText.fromString("Rewards:"),
                        com.wynntils.core.text.StyledText.fromString("- +7500000 XP"),
                        com.wynntils.core.text.StyledText.fromString("- +Various Items"))));

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

        // 書卷標題：[圖示][偏移][圖示][偏移][書卷名]。那些偏移兩側都是圖示，
        // 會被當成欄位交界，而唯一會變短的是<b>後面</b>的書卷名——把差額補進
        // 它前面的空白，等於把整個標題改成靠右對齊，中文短多少就往右推多少。
        java.util.List<LineTranslator.Piece> title = java.util.List.of(
                LineTranslator.Piece.text("", Style.EMPTY),      // 圖示
                LineTranslator.Piece.space(5, Style.EMPTY),
                LineTranslator.Piece.text("Dragon's Tome", Style.EMPTY));
        check("前面只有圖示，不算欄位交界",
                !LineTranslator.labelled(title, 1));
        java.util.List<LineTranslator.Piece> stat = java.util.List.of(
                LineTranslator.Piece.text("Health", Style.EMPTY),
                LineTranslator.Piece.space(40, Style.EMPTY),
                LineTranslator.Piece.text("+390", Style.EMPTY));
        check("前面有標籤，是欄位交界",
                LineTranslator.labelled(stat, 1));

        // 自製物品的「Crafted by <玩家名>」。名字裡的<b>數字</b>在參數化那一關
        // 已經被收成 {~}，所以判斷要先把佔位符拿掉——只認英數的話，帶數字的 ID
        // 一律被擋下，畫面上就是「有的人翻得出來、有的翻不出來」。
        check("純字母的 ID", LineTranslator.isName("Xikys"));
        check("帶底線的 ID", LineTranslator.isName("Green_teaTW"));
        check("數字被收成佔位符的 ID", LineTranslator.isName("{~}N{~}K{~}"));
        check("整串都是數字的 ID", LineTranslator.isName("{~}"));
        check("一句話不是 ID", !LineTranslator.isName("the legendary ice mage"));
        check("空的不是 ID", !LineTranslator.isName(""));

        gearRequirements();

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

    /**
     * 武器的需求欄：數值的<b>右緣</b>要留在原處。
     *
     * <h2>回報了四次的那一個</h2>
     * 橡木弓的「職業類型」翻成中文之後停在原本的起點，右邊空一大塊，看起來像
     * 置中；同一份 tooltip 的「戰鬥等級 1」卻是對的——因為那個 1 沒有被翻譯，
     * 寬度沒變。
     *
     * <p>{@link LineTranslator#realign} 本來就會補右緣，可是它要
     * {@code leftAligned == false} 才做，而 {@link LineTranslator#columnsAreLeftAligned}
     * 對<b>每一份物品 tooltip</b> 都回答「靠左」：
     *
     * <ul>
     *   <li>{@code secondColumn} 先前逐 {@code StyledTextPart} 走，而 Wynncraft 把
     *       欄距的兩個偏移接在標籤<b>同一個片段</b>的尾巴，所以一個欄界都沒認出來；</li>
     *   <li>就算認出來了，門檻是「三行才算排版」，而武器只有職業類型與戰鬥等級
     *       兩行有欄距。</li>
     * </ul>
     *
     * <h2>這裡怎麼量</h2>
     * 照 line-debug「=== 9 ===」的片段結構重建那兩行，偏移值改用本檔的量尺算
     * （半形 6、全形 9、圖示 0），讓「退回標籤起點、再跳到數值欄」的關係跟實機
     * 一致。兩行的右緣都落在 149px：
     *
     * <pre>
     *   sprite[-1]  " Class Type"[66]   [-66 +72]  "Archer/Hunter"[78]   → 149
     *   sprite[-1]  " Combat Level"[78] [-78 +144] "1"[6]                → 149
     * </pre>
     *
     * 整行的總寬就是右緣（數值後面沒有東西了），所以「譯文總寬 == 原文總寬」
     * 就是「右緣沒跑掉」。沒修之前職業類型那一行量到 113——短了 36px，正是
     * 截圖上那一塊空白。
     */
    private static void gearRequirements() {
        LineTranslator.measureForTest = AlignPointTest::gearWidth;
        try {
            StyledText classType = requirementRow(" Class Type", -66, 72, "Archer/Hunter");
            StyledText combatLevel = requirementRow(" Combat Level", -78, 144, "1");
            List<StyledText> rows = List.of(classType, combatLevel);

            check("原文兩行的右緣本來就對齊（量尺沒歪）",
                    gearWidth(classType.getComponent()) == 149
                            && gearWidth(combatLevel.getComponent()) == 149);
            check("兩行有欄距的需求列＝靠右，不是靠左",
                    !LineTranslator.columnsAreLeftAligned(rows));

            TranslationStore store = new TranslationStore();
            store.loadAll(java.nio.file.Path.of(
                    "src/main/resources/assets/wynnchayuan/translations", "zh_tw"));
            boolean left = LineTranslator.columnsAreLeftAligned(rows);
            for (StyledText row : rows) {
                net.minecraft.network.chat.Component out =
                        LineTranslator.translate(row, store, false, left);
                String plain = out == null ? "(沒翻到)" : out.getString();
                int was = gearWidth(row.getComponent());
                int now = out == null ? -1 : gearWidth(out);
                check("需求列的右緣對回原文：" + plain.replace(" ", "␠")
                                + "  " + was + " -> " + now,
                        now == was);
            }
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

    /** line-debug「=== 9 ===」的片段結構：圖示、標籤＋兩個偏移、數值。 */
    private static StyledText requirementRow(String label, int rewind, int column,
                                             String value) {
        Style sprite = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace(
                        "tooltip/requirement/sprite")));
        Style wynn = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("language/wynncraft")));
        net.minecraft.network.chat.MutableComponent row =
                net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal(
                        new String(Character.toChars(0xE006)) + SpaceOffset.encode(-1))
                        .withStyle(sprite))
                .append(net.minecraft.network.chat.Component.literal(
                        label + SpaceOffset.encode(rewind) + SpaceOffset.encode(column))
                        .withStyle(wynn))
                .append(net.minecraft.network.chat.Component.literal(value).withStyle(wynn));
        return StyledText.fromComponent(row);
    }

    /**
     * 量尺：半形 6、全形 9、圖示 0、偏移碼位照面值。
     *
     * <p>偏移要<b>逐碼位</b>認，不能像 {@code ChatAlignTest} 那樣只認整段都是
     * 偏移的片段——這裡要量的正是「標籤後面接著偏移」的混合片段。
     */
    private static int gearWidth(net.minecraft.network.chat.Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            int i = 0;
            while (i < text.length()) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                if (SpaceOffset.isOffset(cp)) {
                    w[0] += SpaceOffset.decode(new String(Character.toChars(cp)));
                } else if (cp >= 0xE000 && cp <= 0xF8FF) {
                    w[0] += 0;                 // 圖示本身不佔位，位置靠前後的偏移
                } else if (cp >= 0x2E80) {
                    w[0] += 9;                 // 全形
                } else {
                    w[0] += 6;
                }
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }
}
