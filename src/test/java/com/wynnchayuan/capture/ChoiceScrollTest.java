package com.wynnchayuan.capture;

import java.util.ArrayList;
import java.util.List;

public final class ChoiceScrollTest {

    private static int failures = 0;

    // 實機量到的跑馬燈視窗寬度
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

    private static void shortOptionIsNotReported() {
        ChoiceScroll.reset();
        check("短選項不回報整句", ChoiceScroll.feed(0, "Who are you?") == null);
        check("短選項的 full() 是 null", ChoiceScroll.full(0) == null);
        check("短選項的 first() 還是它自己",
              "Who are you?".equals(ChoiceScroll.first(0)));
    }

    private static void unrelatedOptionRestarts() {
        ChoiceScroll.reset();
        ChoiceScroll.feed(0, "How come one of the pedestal");
        ChoiceScroll.feed(0, "Nothing to say, goodbye.");
        check("接不上就重新開始",
              "Nothing to say, goodbye.".equals(ChoiceScroll.first(0)),
              "Nothing to say, goodbye.", ChoiceScroll.first(0));
    }

    private static void coincidenceDoesNotMerge() {
        check("三個字的巧合不接",
              ChoiceScroll.merge("I want the map", "map is over there") == null);
        check("重疊剛好到門檻就接",
              "abcdefghijklmnopXYZ".equals(
                      ChoiceScroll.merge("abcdefghijklmnop", "efghijklmnopXYZ")));
    }

    private static void wrapCompletesUnpunctuated() {
        ChoiceScroll.reset();
        String sentence = "Tell me more about the Nemract graveyard problem";
        String got = scroll(1, sentence, 1);
        check("沒有句尾標點也接得回來", sentence.equals(got), sentence, got);
    }

    private static void earlyPeriodIsCorrectedLater() {
        ChoiceScroll.reset();
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

    private static void secondCycleKeepsTheFirstWindow() {
        ChoiceScroll.reset();
        String sentence = "How come one of the pedestals is already glowing "
                + "when nobody has touched it?";
        String firstWindow = sentence.substring(0, WINDOW).strip();

        scroll(3, sentence, 1);
        scroll(3, sentence, 1);
        scroll(3, sentence, 1);

        check("第二、三輪之後 first() 還是第一格",
              firstWindow.equals(ChoiceScroll.first(3)),
              firstWindow, ChoiceScroll.first(3));
        check("三輪之後整句沒有被拼壞", sentence.equals(ChoiceScroll.full(3)),
              sentence, ChoiceScroll.full(3));

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

    private static List<String> windows(String sentence, int step) {
        List<String> out = new ArrayList<>();
        if (sentence.length() <= WINDOW) {
            out.add(sentence);
            return out;
        }
        for (int i = 0; i + WINDOW <= sentence.length(); i += step) {
            out.add(sentence.substring(i, i + WINDOW).strip());
        }
        String tail = sentence.substring(sentence.length() - WINDOW).strip();
        if (!out.get(out.size() - 1).equals(tail)) {
            out.add(tail);
        }
        out.add(sentence.substring(0, WINDOW).strip());
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
