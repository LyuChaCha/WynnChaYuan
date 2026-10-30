package com.wynnchayuan.capture;

public final class DialogueBufferTest {

    private static int failures = 0;

    public static void main(String[] args) {
        typingNeverLeaks();
        placeholderFlipIsNotANewSentence();
        rewrapIsNotANewSentence();
        newSentenceReleasesTheParametrizedOne();
        report();
    }

    private static void typingNeverLeaks() {
        DialogueBuffer b = new DialogueBuffer();
        String[] frames = {"H-h", "H-hey", "H-hey, h", "H-hey, human!"};
        boolean leaked = false;
        for (String f : frames) {
            if (b.offer(f, f) != null) {
                leaked = true;
            }
        }
        check("打字途中不送出任何東西", !leaked);
        check("結束時送出的是完整那句", "H-hey, human!".equals(b.flush()));
    }

    private static void placeholderFlipIsNotANewSentence() {
        DialogueBuffer b = new DialogueBuffer();
        check("名字打到一半不送出",
                b.offer("Hey, Green_teaT", "Hey, Green_teaT") == null);
        check("名字打完、模板翻成 {u} —— 仍然不送出",
                b.offer("Hey, Green_teaTea", "Hey, {u}") == null);
        check("留下來的是參數化過的那一份", "Hey, {u}".equals(b.flush()));

        DialogueBuffer c = new DialogueBuffer();
        c.offer("the King of Ragn", "the King of Ragn");
        check("地名翻成 {p} 也不算新句子",
                c.offer("the King of Ragni", "the King of {p}") == null);
        check("留下來的是 {p} 那一份", "the King of {p}".equals(c.flush()));
    }

    private static void rewrapIsNotANewSentence() {
        DialogueBuffer b = new DialogueBuffer();
        b.offer("We should probably talk to them", "We should probably talk to them");
        boolean quiet = b.offer("We should probably talk\nto them if we want to pass",
                                "We should probably talk\nto them if we want to pass") == null;
        check("換行不算換句子", quiet);
        check("換行之後留下的是完整那句",
                "We should probably talk\nto them if we want to pass".equals(b.flush()));
    }

    private static void newSentenceReleasesTheParametrizedOne() {
        DialogueBuffer b = new DialogueBuffer();
        b.offer("Hey, Green_teaT", "Hey, Green_teaT");
        b.offer("Hey, Green_teaTea", "Hey, {u}");
        String done = b.offer("Follow me to Ragni", "Follow me to {p}");
        check("換句時送出上一句", "Hey, {u}".equals(done));
        check("新的那句留在緩衝區", "Follow me to {p}".equals(b.flush()));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("對話緩衝：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("對話緩衝：全部通過");
    }
}
