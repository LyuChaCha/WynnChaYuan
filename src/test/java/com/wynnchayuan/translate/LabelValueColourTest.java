package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 「標籤: 數值」兩半各自的顏色要跟原文一樣。
 *
 * <h2>先前壞在哪</h2>
 * 派對面板與世界清單整片都是這個形狀，而兩半的顏色<b>是不一樣的</b>：
 *
 * <pre>
 *   #00AAAA 「Type: 」   #55FFFF 「Grinding Mobs」
 *   #00AAAA 「World: 」  #55FFFF 「NA12」
 * </pre>
 *
 * 這種行是整行收在語料裡的，走不到「名稱：說明」那條路，於是整行只拿得到一個
 * 主樣式——而主樣式是<b>照字數</b>算的：
 *
 * <pre>
 *   Type:  6 字 &lt; Grinding Mobs 13 字   -&gt; 整行套上數值的顏色，標籤變亮
 *   World: 7 字 &gt; NA12          4 字   -&gt; 整行套上標籤的顏色，數值變暗
 * </pre>
 *
 * 同一個面板上下兩行，一行標籤太亮、一行數值太暗，錯的方向還相反——
 * 純粹看哪半的字比較多。實機回報的顏色錯誤就是這個。
 *
 * <h2>這條測試在盯什麼</h2>
 * 顏色是<b>畫出來</b>才看得到的，光問語料查不查得到沒有用。所以照實機
 * majorid-debug 記下來的顏色把原文重建一次，翻完再把每一段的顏色印出來比對。
 */
public final class LabelValueColourTest {

    private static int failures = 0;

    private static final int LABEL = 0x00AAAA;   // 深青，標籤
    private static final int VALUE = 0x55FFFF;   // 亮青，數值
    private static final int WHITE = 0xFFFFFF;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // 實機 majorid-debug「可用的顏色 186／187」記下來的兩行。
        // 兩半的字數一長一短，先前的錯法剛好相反，所以兩行都要測。
        row(store, "數值比標籤長", new String[] {"Type: ", "Grinding Mobs"},
                new int[] {LABEL, VALUE},
                new String[] {"類型：", "刷怪"}, new int[] {LABEL, VALUE});

        row(store, "標籤比數值長", new String[] {"World: ", "NA12"},
                new int[] {LABEL, VALUE},
                new String[] {"世界: ", "NA", "12"}, new int[] {LABEL, VALUE, VALUE});

        // 反面：本來就對的不能被弄壞。數值走 {~} 佔位符，冒號前面還有個「- 」。
        row(store, "數值是佔位符的照舊",
                new String[] {"- ", "Elapsed Time: ", "00:02"},
                new int[] {LABEL, LABEL, WHITE},
                new String[] {"- 已等待：", "00", ":", "02"},
                new int[] {LABEL, WHITE, WHITE, WHITE});

        // ★ 反面：整行同色的不能被切開。冒號兩邊一樣的顏色本來就沒得分，
        //   硬切只會多登記一段重點段，讓貼樣式那一步挑錯。
        uniform(store, "整行同色不切", new String[] {"Type: ", "Grinding Mobs"},
                new int[] {VALUE, VALUE}, VALUE);

        System.out.println(failures == 0
                ? "標籤數值顏色：全部通過"
                : "標籤數值顏色：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 翻完之後每一段的文字與顏色要剛好是 {@code wantText} / {@code wantColours}。 */
    private static void row(TranslationStore store, String what,
                            String[] parts, int[] colours,
                            String[] wantText, int[] wantColours) {
        List<String> text = new ArrayList<>();
        List<Integer> got = new ArrayList<>();
        dump(store, parts, colours, text, got);

        List<Integer> want = new ArrayList<>();
        for (int c : wantColours) {
            want.add(c);
        }
        report(what + "：" + java.util.Arrays.toString(wantText) + " "
                        + hex(want) + "（實際 " + text + " " + hex(got) + "）",
                text.equals(List.of(wantText)) && got.equals(want));
    }

    /** 整行只有一個顏色時，譯文也整行同一個顏色。 */
    private static void uniform(TranslationStore store, String what,
                                String[] parts, int[] colours, int want) {
        List<String> text = new ArrayList<>();
        List<Integer> got = new ArrayList<>();
        dump(store, parts, colours, text, got);
        boolean ok = !got.isEmpty();
        for (int c : got) {
            ok &= c == want;
        }
        report(what + "：整行都是 #" + Integer.toHexString(want).toUpperCase()
                        + "（實際 " + text + " " + hex(got) + "）", ok);
    }

    private static void dump(TranslationStore store, String[] parts, int[] colours,
                             List<String> text, List<Integer> got) {
        MutableComponent line = Component.empty();
        for (int i = 0; i < parts.length; i++) {
            line.append(Component.literal(parts[i])
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colours[i]))));
        }
        for (Component c : com.wynnchayuan.render.TooltipPanel
                .translateLines(List.of(line), store)) {
            c.visit((style, run) -> {
                text.add(run);
                got.add(style.getColor() == null ? -1 : style.getColor().getValue());
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
    }

    private static String hex(List<Integer> colours) {
        List<String> out = new ArrayList<>(colours.size());
        for (int c : colours) {
            out.add(c < 0 ? "（無色）" : "#" + String.format("%06X", c));
        }
        return out.toString();
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
