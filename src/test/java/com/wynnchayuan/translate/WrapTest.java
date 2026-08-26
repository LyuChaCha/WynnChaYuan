package com.wynnchayuan.translate;

import java.util.Random;

/**
 * 折行不能丟例外。
 *
 * <h2>為什麼要有這條測試</h2>
 * 折行的位置<b>剛好是一個空白</b>時，切點 {@code cut} 就等於當前位置 {@code i}，
 * 而 {@code lineStart} 被設成 {@code cut + 1}——比 {@code i} 還大。接下來的
 * {@code substring(lineStart, i)} 就變成 {@code substring(24, 23)}，當場丟出
 * {@code StringIndexOutOfBoundsException}。
 *
 * <p>那個例外被繪製那層的 {@code catch (Throwable)} 吞掉，結果是<b>整個翻譯面板
 * 都不畫</b>。畫面上看起來像「這件物品沒翻到」，實際上語料好好的躺在那裡。
 * 使用者回報的 Halcyon 就是這樣。
 *
 * <p>所以除了那個確切的形狀，再用亂數跑一輪：任何字串、任何寬度都不該丟例外，
 * 而且折出來的內容去掉換行之後必須跟原文一模一樣。
 */
public final class WrapTest {

    private static int failures = 0;

    /** 一個字元算一單位。真正的量法要 Minecraft 字型，測試環境裡沒有。 */
    private static int measure(String piece) {
        return piece.codePointCount(0, piece.length());
    }

    public static void main(String[] args) {
        // 使用者實際踩到的形狀：換行點正好落在空白上
        check("換行點落在空白上不會丟例外",
                () -> LineTranslator.wrapToWidth(
                        "Blinding Lights: All Ophanim orbs deal 250% damage",
                        8, WrapTest::measure));

        check("寬度只夠一個字",
                () -> LineTranslator.wrapToWidth("a b c d e f g", 1, WrapTest::measure));
        check("整串都是空白",
                () -> LineTranslator.wrapToWidth("          ", 3, WrapTest::measure));
        check("佔位符不會被拆開",
                () -> LineTranslator.wrapToWidth("{~} 格內的友軍 {~} 生命", 4, WrapTest::measure));

        // 避頭尾：標點不能自己站在行首。先前會折成
        //   「…獲得 +5 層 Crystallize」
        //   「，並改為環繞你運行。」   ← 逗號孤零零地開頭
        String orphan = LineTranslator.wrapToWidth(
                "每次命中獲得層數，並改為環繞你運行。", 8, WrapTest::measure);
        boolean starts = false;
        for (String line : orphan.split(String.valueOf(NL))) {
            if (!line.isEmpty() && "，。、；：！？）」".indexOf(line.charAt(0)) >= 0) {
                starts = true;
            }
        }
        report("標點不會被擠到行首（實際：" + show(orphan) + "）", !starts);

        // 正負號跟它的數值是同一塊，不能拆開——先前會折成「增加 +」「5 層數」
        String signed = LineTranslator.wrapToWidth("每次命中增加 +{~} 層數", 7,
                                                   WrapTest::measure);
        report("號不會跟數值分家（實際：" + show(signed) + "）",
                !signed.contains("+" + NL));

        // 內容守恆：折行只能動空白，不能吃掉或改動任何一個字。
        //
        // 比對時<b>兩邊的空白全部拿掉</b>。折在空白上時那個空白會被行尾吃掉，
        // 折在中文字之間則不會——所以不能用「換行還原成空白」去比，
        // 那樣兩種情形只能對上一種。
        String text = "Allies within {~} blocks gain {~} of the health you gain";
        for (int px = 1; px <= 60; px++) {
            String wrapped = LineTranslator.wrapToWidth(text, px, WrapTest::measure);
            if (!bare(wrapped).equals(bare(text))) {
                System.out.println("  [FAIL] 寬度 " + px + " 折完內容變了：" + wrapped);
                failures++;
                break;
            }
        }
        check("內容守恆（寬度 1 到 60）", () -> "");

        // 亂數：任何字串、任何寬度都不該丟例外
        Random random = new Random(20260822L);
        String alphabet = " abcXYZ中文的字{~}";
        for (int round = 0; round < 4000; round++) {
            StringBuilder sb = new StringBuilder();
            int length = random.nextInt(60);
            for (int i = 0; i < length; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String input = sb.toString();
            int px = random.nextInt(30);
            try {
                LineTranslator.wrapToWidth(input, px, WrapTest::measure);
            } catch (RuntimeException e) {
                System.out.println("  [FAIL] 亂數第 " + round + " 輪丟例外："
                        + e + "  寬度 " + px + "  輸入 [" + input + "]");
                failures++;
                break;
            }
        }
        check("亂數四千輪都沒丟例外", () -> "");

        System.out.println(failures == 0 ? "Wrap: 全部通過" : "Wrap: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 拿掉所有空白，只留下字本身。 */
    private static String bare(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().filter(c -> !Character.isWhitespace(c)).forEach(sb::appendCodePoint);
        return sb.toString();
    }

    private interface Body {
        String run();
    }

    /** 折行用的換行字元。 */
    private static final char NL = '\n';

    /** 把換行換成看得見的符號，失敗訊息才讀得出來折在哪。 */
    private static String show(String wrapped) {
        return wrapped.replace(String.valueOf(NL), " ⏎ ");
    }

    /** 直接斷言一個條件（{@link #check} 只驗「有沒有丟例外」）。 */
    private static void report(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void check(String what, Body body) {
        try {
            body.run();
            System.out.println("  [PASS] " + what);
        } catch (RuntimeException e) {
            System.out.println("  [FAIL] " + what + " —— " + e);
            failures++;
        }
    }
}
