package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 譯文把句尾的座標搬到句中之後，座標<b>後面</b>那段字不可以繼承座標的顏色。
 *
 * <h2>實機回報（內容書「The Missing Piece」任務卡）</h2>
 * 原文兩行、兩個顏色：
 *
 * <pre>
 *   §7Pick up your post in the Post
 *   §7Office at §f[-2156, 30, -944]
 * </pre>
 *
 * 譯文把座標搬到句子中間：「到 [-{~}, {~}, -{~}] 的郵局領取你的郵件。」，
 * 折回兩行之後第二行是「的郵局領取你的郵件。」——整句<b>灰</b>的那一半。
 * 畫面上它卻是白的。
 *
 * <h2>怎麼壞的</h2>
 * 原文第二行混了兩個顏色，{@code wholeLineAccents} 退而求其次拿「多數色」貼上
 * 譯文的同一行。多數色是照實字數算的：灰的 {@code Office at} 9 個字，
 * 白的 {@code [-2156, 30, -944]} 15 個字——白贏。
 *
 * <p>但那 15 個字裡有 9 個是<b>數值本身</b>，而數值是抽出去另外保管、會自己帶著
 * 白色填回來的；它在譯文裡搬到哪一行都可以，不該由它決定整行的字是什麼顏色。
 * 見 {@code perPartStyles}。
 */
public final class ObjectiveTailColourTest {

    private static int failures = 0;

    private static final int BODY = 0xAAAAAA;   // 灰：說明
    private static final int COORD = 0xFFFFFF;  // 白：座標

    private static final String LINE_ONE = "Pick up your post in the Post";
    private static final String LINE_TWO_TEXT = "Office at ";
    private static final String COORDS = "[-2156, 30, -944]";

    private static Component line(Object... pairs) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < pairs.length; i += 2) {
            out.append(Component.literal((String) pairs[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) pairs[i + 1]))));
        }
        return out;
    }

    /** 遊戲送來的樣子：句尾的座標自成一個白色的段，其餘是灰的。 */
    private static List<StyledText> block() {
        return List.of(
                StyledText.fromComponent(line(LINE_ONE, BODY)),
                StyledText.fromComponent(line(LINE_TWO_TEXT, BODY, COORDS, COORD)));
    }

    public static void main(String[] args) throws Exception {
        FlowedDebug.init(java.nio.file.Files.createTempDirectory("wynnchayuan"));

        oneLine();
        twoLines();

        report();
    }

    /**
     * 譯文一行就講完（測試環境量不到字寬，不會折行）——這是原本就對的那一半，
     * 釘住它才看得出修正沒有把它弄壞。
     */
    private static void oneLine() throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        List<StyledText> run = block();
        List<Component> built = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("［一行］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        verify("［一行］", built);
    }

    /**
     * 譯文折成兩行——實機就是這樣（原文寬度只容得下這麼多）。
     *
     * <p>測試環境量不到字寬，{@code wrapToBlock} 折不出東西來，所以直接餵一條
     * <b>本來就兩行</b>的譯文：行數與原文相同，走的是同一段程式。
     */
    private static void twoLines() throws Exception {
        Path dir = java.nio.file.Files.createTempDirectory("objective-tail");
        String src = LINE_ONE + "\\n" + LINE_TWO_TEXT.strip() + " [-{~}, {~}, -{~}]";
        String dst = "到 [-{~}, {~}, -{~}]\\n的郵局領取你的郵件。";
        java.nio.file.Files.writeString(dir.resolve("quest.json"),
                "{\"" + src + "\": \"" + dst + "\"}");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        List<StyledText> run = block();
        List<Component> built = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("［兩行］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        check("［兩行］譯文真的是兩行（實際 " + built.size() + " 行）", built.size() == 2);
        verify("［兩行］", built);
    }

    private static void verify(String tag, List<Component> built) {
        String all = text(built);
        System.out.println(tag + "譯文：" + all.replace("\n", "⏎"));
        dump(built);
        check(tag + "座標填回來了（實際：" + all + "）", all.contains(COORDS));
        check(tag + "後半翻出來了（實際：" + all + "）", all.contains("領取你的郵件"));

        Integer coord = colourOf(built, "2156");
        check(tag + "座標是原文的白（拿到 " + show(coord) + "）",
                coord != null && coord == COORD);
        Integer tail = colourOf(built, "郵局");
        check(tag + "座標後面的說明仍是灰的（拿到 " + show(tail) + "）",
                tail != null && tail == BODY);
        Integer head = colourOf(built, "到");
        check(tag + "座標前面的字也是灰的（拿到 " + show(head) + "）",
                head != null && head == BODY);
    }

    private static String show(Integer c) {
        return c == null ? "null" : "#" + String.format("%06X", c);
    }

    private static String text(List<Component> built) {
        StringBuilder out = new StringBuilder();
        for (Component c : built) {
            out.append(c.getString());
        }
        return out.toString();
    }

    private static void dump(List<Component> built) {
        for (Component c : built) {
            StringBuilder row = new StringBuilder("  行：");
            c.visit((style, s) -> {
                row.append('[').append(s).append(' ')
                   .append(style.getColor() == null ? "null" : style.getColor().serialize())
                   .append(']');
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println(row);
        }
    }

    /** 含有 {@code needle} 的那一段的顏色。 */
    private static Integer colourOf(List<Component> built, String needle) {
        List<Integer> found = new ArrayList<>();
        for (Component c : built) {
            c.visit((style, s) -> {
                if (s.contains(needle) && style.getColor() != null) {
                    found.add(style.getColor().getValue() & 0xFFFFFF);
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "[通過] " : "[失敗] ") + name);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            throw new AssertionError(failures + " 項檢查未通過");
        }
        System.out.println("全部通過");
    }
}
