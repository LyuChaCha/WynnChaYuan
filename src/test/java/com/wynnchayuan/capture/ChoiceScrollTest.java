package com.wynnchayuan.capture;

import java.util.ArrayList;
import java.util.List;

/**
 * 跑馬燈選項要能接回整句。
 *
 * <h2>為什麼要模擬捲動</h2>
 * 直接餵整句進去一定會過，但那不是實機發生的事：實機每個 tick 只送一個
 * <b>固定寬度的視窗</b>過來，整句從來不會完整出現。所以這裡照著那個樣子
 * 一格一格餵——視窗寬度、每格捲幾個字、頭尾的空白被 strip 掉，都照實機。
 *
 * <p>只拿整句去測會漏掉整類 bug（對齊、重疊、空白），那是這個專案踩過的。
 */
public final class ChoiceScrollTest {

    private static int failures = 0;

    /** 實機量到的視窗寬度。 */
    private static final int WINDOW = 28;

    public static void main(String[] args) {
        fullSentenceIsStitched();
        realFieldWindows();
        shortOptionIsNotReported();
        unrelatedOptionRestarts();
        coincidenceDoesNotMerge();
        wrapCompletesUnpunctuated();
        earlyPeriodIsCorrectedLater();
        secondCycleKeepsTheFirstWindow();

        if (failures > 0) {
            System.out.println("跑馬燈接合：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("跑馬燈接合：全部通過");
    }

    /** 主線：一句 150 字元的選項捲一輪，要接回原句。 */
    private static void fullSentenceIsStitched() {
        ChoiceScroll.reset();
        String sentence = "Don't underestimate our strength, we have been "
                + "training for this our whole lives and we will not back down now.";
        String got = scroll(0, sentence, 1);

        check("接回整句", sentence.equals(got), sentence, got);
        check("鍵留第一格",
              sentence.substring(0, WINDOW).equals(ChoiceScroll.first(0)),
              sentence.substring(0, WINDOW), ChoiceScroll.first(0));
        check("full() 拿得到整句", sentence.equals(ChoiceScroll.full(0)));
    }

    /**
     * 實機錄到的三格。
     *
     * <p>2026-09-18 的回報裡連續出現這三格（前後還有別的，只有這三格留下來）。
     * 接起來要剛好只多出兩個字，不能重複「anything from before you」。
     */
    private static void realFieldWindows() {
        String a = "mber anything from before yo";
        String b = "ber anything from before you";
        String merged = ChoiceScroll.merge(a, b);
        check("實機的兩格接起來", "mber anything from before you".equals(merged),
              "mber anything from before you", merged);

        String c = "er anything from before you ";
        String again = ChoiceScroll.merge(merged, c.strip());
        check("第三格接上去", "mber anything from before you".equals(again),
              "mber anything from before you", again);
    }

    /** 沒在捲的選項：整句就是第一格，不該回報「接好的整句」。 */
    private static void shortOptionIsNotReported() {
        ChoiceScroll.reset();
        check("短選項不回報整句", ChoiceScroll.feed(0, "Who are you?") == null);
        check("短選項的 full() 是 null", ChoiceScroll.full(0) == null);
        check("短選項的 first() 還是它自己",
              "Who are you?".equals(ChoiceScroll.first(0)));
    }

    /** 換成另一個選項（接不上）就從頭累積，不能把兩句黏在一起。 */
    private static void unrelatedOptionRestarts() {
        ChoiceScroll.reset();
        ChoiceScroll.feed(0, "How come one of the pedestal");
        ChoiceScroll.feed(0, "Nothing to say, goodbye.");
        check("接不上就重新開始",
              "Nothing to say, goodbye.".equals(ChoiceScroll.first(0)),
              "Nothing to say, goodbye.", ChoiceScroll.first(0));
    }

    /** 幾個字母的巧合不算接得上——接錯比接不到糟。 */
    private static void coincidenceDoesNotMerge() {
        check("三個字的巧合不接",
              ChoiceScroll.merge("I want the map", "map is over there") == null);
        check("重疊剛好到門檻就接",
              "abcdefghijklmnopXYZ".equals(
                      ChoiceScroll.merge("abcdefghijklmnop", "efghijklmnopXYZ")));
    }

    /** 沒有句尾標點的長選項：靠捲回第一格判定拼完。 */
    private static void wrapCompletesUnpunctuated() {
        ChoiceScroll.reset();
        String sentence = "Tell me more about the Nemract graveyard problem";
        String got = scroll(1, sentence, 1);
        check("沒有句尾標點也接得回來", sentence.equals(got), sentence, got);
    }

    /**
     * 句子中間的句點會讓判定提早成立。後面繼續捲進來的字要能再回報一次，
     * 而且新的那一份是舊的<b>前綴延伸</b>——收集端靠這件事決定要不要換掉。
     */
    private static void earlyPeriodIsCorrectedLater() {
        ChoiceScroll.reset();
        // 句點刻意放在第一格之後（第 40 個字元附近）：落在第一格裡面的話
        // 一開始就成立，測不到「提早判定」。
        String sentence = "Before we go any further, I spoke to Mr. Bob "
                + "about the missing shipment.";
        List<String> reports = new ArrayList<>();
        for (String w : windows(sentence, 1)) {
            String r = ChoiceScroll.feed(2, w);
            if (r != null) {
                reports.add(r);
            }
        }
        check("提早判定之後還會再回報", reports.size() >= 2,
              ">=2", String.valueOf(reports.size()));
        boolean prefixes = true;
        for (int i = 1; i < reports.size(); i++) {
            if (!reports.get(i).startsWith(reports.get(i - 1))) {
                prefixes = false;
            }
        }
        check("每一次回報都是前一次的延伸", prefixes);
        check("最後一次就是整句",
              !reports.isEmpty() && sentence.equals(reports.get(reports.size() - 1)),
              sentence, reports.isEmpty() ? "(無)" : reports.get(reports.size() - 1));
    }

    /**
     * 跑馬燈會一直捲。第二輪不能把 {@link ChoiceScroll#first} 換成第二格——
     * 收集端拿它當「第一格」的鍵，換掉就等於把一條從單字中間切開的字串
     * 收進語料，那正是這次要修的毛病。
     */
    private static void secondCycleKeepsTheFirstWindow() {
        ChoiceScroll.reset();
        String sentence = "How come one of the pedestals is already glowing "
                + "when nobody has touched it?";
        String firstWindow = sentence.substring(0, WINDOW).strip();

        scroll(3, sentence, 1);
        scroll(3, sentence, 1);            // 第二輪
        scroll(3, sentence, 1);            // 第三輪

        check("第二、三輪之後 first() 還是第一格",
              firstWindow.equals(ChoiceScroll.first(3)),
              firstWindow, ChoiceScroll.first(3));
        check("三輪之後整句沒有被拼壞", sentence.equals(ChoiceScroll.full(3)),
              sentence, ChoiceScroll.full(3));

        // ★ 要在一輪的<b>中途</b>問。捲完整一輪的最後一格剛好是第一格，
        // 那一格自己就會把狀態接回去，中途壞掉的話在輪尾看不出來。
        ChoiceScroll.reset();
        List<String> cycle = windows(sentence, 1);
        for (String w : cycle) {
            ChoiceScroll.feed(4, w);
        }
        for (int i = 0; i < 6 && i < cycle.size(); i++) {
            ChoiceScroll.feed(4, cycle.get(i));
        }
        check("第二輪捲到一半 first() 也還是第一格",
              firstWindow.equals(ChoiceScroll.first(4)),
              firstWindow, ChoiceScroll.first(4));
        check("第二輪捲到一半 full() 還是整句",
              sentence.equals(ChoiceScroll.full(4)),
              sentence, ChoiceScroll.full(4));
    }

    /**
     * 照實機的樣子把一句話捲一輪餵進去。
     *
     * @return 回報出來的最後一句
     */
    private static String scroll(int row, String sentence, int step) {
        String last = null;
        for (String w : windows(sentence, step)) {
            String r = ChoiceScroll.feed(row, w);
            if (r != null) {
                last = r;
            }
        }
        return last;
    }

    /**
     * 一句話捲過去會看到的每一格。
     *
     * <p>照實機：固定寬度、每格往左捲 {@code step} 個字、頭尾空白被 strip 掉，
     * 捲到尾巴之後<b>回到第一格</b>（那正是「拼完了」的信號之一）。
     */
    private static List<String> windows(String sentence, int step) {
        List<String> out = new ArrayList<>();
        if (sentence.length() <= WINDOW) {
            out.add(sentence);
            return out;
        }
        for (int i = 0; i + WINDOW <= sentence.length(); i += step) {
            out.add(sentence.substring(i, i + WINDOW).strip());
        }
        // 捲到尾巴還差幾個字的話，最後一格是貼齊尾巴的那一格
        String tail = sentence.substring(sentence.length() - WINDOW).strip();
        if (!out.get(out.size() - 1).equals(tail)) {
            out.add(tail);
        }
        out.add(sentence.substring(0, WINDOW).strip());   // 捲回開頭
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void check(String what, boolean ok, String want, String got) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            System.out.println("         想要：" + want);
            System.out.println("         拿到：" + got);
            failures++;
        }
    }
}
