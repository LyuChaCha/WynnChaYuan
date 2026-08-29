package com.wynnchayuan.capture;

/**
 * 逐字打出來的對話要收斂成<b>完整</b>的一句，不能送出半截。
 *
 * <h2>先前壞在哪</h2>
 * 緩衝區拿<b>模板</b>比前綴，判斷「新內容是不是舊內容的延伸」。問題是模板在
 * 打字過程中<b>不是單調遞增的</b>——參數化要比對到完整的詞才會命中：
 *
 * <pre>
 *   "Hey, Green_teaT"    名字還沒打完 → 比不到 → 模板是字面
 *   "Hey, Green_teaTea"  名字打完了   → 比到了 → 模板變成 "Hey, {u}"
 * </pre>
 *
 * {@code "Hey, {u}"} 不是 {@code "Hey, Green_teaT"} 的延伸，於是被判定成
 * 「換了新的一句」，半截的那份被送進 {@code captured.json}。
 *
 * <p>實測使用者的 {@code captured.json}：13 句對話有 12 句是半截的，
 * 而且斷點都落在名字或地名的邊界上——
 * {@code "Of course, the King of Ragn"}（Ragni）、{@code "[+{~} Ragn"}、
 * {@code "Hey, Green_teaT"}。玩家名字外流也是同一個原因：
 * {@code {u}} 沒生效，正是因為送出的那份還沒打完整。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>打字途中一律不送出</li>
 *   <li>模板中途從字面翻成佔位符時<b>不能</b>誤判成新句子（回歸測試）</li>
 *   <li>對話框重新換行、字串中間多出空白時也不能誤判</li>
 *   <li>真的換了一句才送出，而且送出的是<b>參數化過</b>的那一份</li>
 * </ol>
 */
public final class DialogueBufferTest {

    private static int failures = 0;

    public static void main(String[] args) {
        typingNeverLeaks();
        placeholderFlipIsNotANewSentence();
        rewrapIsNotANewSentence();
        newSentenceReleasesTheParametrizedOne();
        report();
    }

    /** 打字途中每一幀都不該送出東西。 */
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

    /**
     * 回歸：模板從字面翻成佔位符的那一幀。
     *
     * <p>原文一路遞增（{@code Green_teaT} → {@code Green_teaTea}），
     * 但模板在最後一刻整個換了形狀。比對原文就不會被騙。
     */
    private static void placeholderFlipIsNotANewSentence() {
        DialogueBuffer b = new DialogueBuffer();
        check("名字打到一半不送出",
                b.offer("Hey, Green_teaT", "Hey, Green_teaT") == null);
        check("名字打完、模板翻成 {u} —— 仍然不送出",
                b.offer("Hey, Green_teaTea", "Hey, {u}") == null);
        check("留下來的是參數化過的那一份", "Hey, {u}".equals(b.flush()));

        // 地名同理：Ragn -> Ragni -> {p}
        DialogueBuffer c = new DialogueBuffer();
        c.offer("the King of Ragn", "the King of Ragn");
        check("地名翻成 {p} 也不算新句子",
                c.offer("the King of Ragni", "the King of {p}") == null);
        check("留下來的是 {p} 那一份", "the King of {p}".equals(c.flush()));
    }

    /** 對話框放不下就重新換行，字串中間會多出空白與排版偏移。 */
    private static void rewrapIsNotANewSentence() {
        DialogueBuffer b = new DialogueBuffer();
        b.offer("We should probably talk to them", "We should probably talk to them");
        boolean quiet = b.offer("We should probably talk\nto them if we want to pass",
                                "We should probably talk\nto them if we want to pass") == null;
        check("換行不算換句子", quiet);
        check("換行之後留下的是完整那句",
                "We should probably talk\nto them if we want to pass".equals(b.flush()));
    }

    /** 真的換了一句才送出上一句。 */
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
