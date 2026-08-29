package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 譯文可以自己指定顏色：{@code {c1}}、{@code {c:#FF55FF}}、{@code {/}}。
 *
 * <h2>為什麼要有這個功能</h2>
 * 顏色本來是<b>猜</b>出來的：拿原文帶特殊樣式的片段，到譯文裡找同樣的字面。
 * 英文原樣留著的詞找得到，翻成中文的就找不到，那一段只好掉回底色。
 * 整行同色的還能靠位置補救，一行裡混了兩三種顏色的就沒辦法——
 * 實機那張「[Cave Completed] … - Rewards: … +1 Unidentified Helmet」
 * 每一行的顏色都不一樣，譯文出來卻是一片灰。
 *
 * <p>猜不到的時候，唯一知道答案的是譯者。所以讓他直接寫出來。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>{@code {cN}} 真的把原文第 N 個顏色搬過去，粗體之類的裝飾一起搬</li>
 *   <li>同一行裡兩個佔位符各管各的，不會互相污染</li>
 *   <li>{@code {/}} 之後回到底色</li>
 *   <li>編號超出範圍時<b>當作沒寫</b>——顏色不對可以忍，整句變回英文不行</li>
 *   <li>佔位符本身<b>絕對不能</b>漏到畫面上</li>
 * </ol>
 */
public final class ColourPlaceholderTest {

    private static int failures = 0;

    private static final int GREEN = 0x55FF55;    // [Cave Completed]
    private static final int WHITE = 0xFFFFFF;    // 洞窟名稱
    private static final int PINK  = 0xFF55FF;    // - Rewards:
    private static final int GREY  = 0xAAAAAA;    // 內文
    private static final int GOLD  = 0xFFAA00;    // 譯者自己指定的顏色

    /**
     * 實機「洞穴完成」那一塊的骨架：一則訊息、四行、每一行各有自己的顏色。
     *
     * <p>顏色出現的順序就是 {@code {c1}} {@code {c2}} {@code {c3}} 的編號順序，
     * 見 {@code LineTranslator#palette}。
     */
    private static StyledText cave() {
        MutableComponent all = Component.empty();
        all.append(lit("[Cave Completed]", GREEN, false));    // {c1}
        all.append(lit("\n", GREY, false));
        all.append(lit("Grook's Nest", WHITE, true));         // {c2}
        all.append(lit("\n", GREY, false));
        all.append(lit("- ", PINK, false));                   // {c3}
        all.append(lit("Rewards:", GREY, false));             // {c4}
        all.append(lit("\n", GREY, false));
        all.append(lit("- ", PINK, false));
        all.append(lit("Sealed Helmet", GREY, false));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, int colour, boolean bold) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(colour)).withBold(bold));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-colour");
        FlowedDebug.init(dir);

        // 模板就是原文本身：這一塊沒有數值也沒有地名，所以鍵跟畫面上一樣。
        String src = "[Cave Completed]\nGrook's Nest\n- Rewards:\n- Sealed Helmet";
        String dst = "{c1}[洞穴完成]{/}\n{c2}Grook 巢穴{/}\n{c3}- {c4}獎勵：{/}\n"
                   + "{c3}- {c:#FFAA00}封印的頭盔{/}";
        write(dir.resolve("cave.json"), src, dst);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        check("暫用語料讀得進來", store.lookup(src) != null);

        Component hit = LineTranslator.translate(cave(), store);
        check("查得到譯文", hit != null);
        if (hit == null) {
            report();
            return;
        }
        String shown = hit.getString();
        System.out.println("      輸出：" + shown.replace("\n", " ⏎ "));

        // 1. 佔位符不能漏到畫面上。這是最要命的一種壞法：玩家會直接看到 {c1}。
        check("畫面上沒有 {c 佔位符（實際：" + shown.replace("\n", " ⏎ ") + "）",
              !shown.contains("{c") && !shown.contains("{/}"));

        // 2. 每一行各自拿到原文那一行的顏色
        Integer title = colourOf(hit, "洞穴完成");
        check("標題套上原文的綠色（拿到 " + hex(title) + "）",
              title != null && title == GREEN);

        Integer name = colourOf(hit, "巢穴");
        check("洞窟名稱套上原文的白色（拿到 " + hex(name) + "）",
              name != null && name == WHITE);
        check("洞窟名稱連粗體一起搬過來", Boolean.TRUE.equals(boldOf(hit, "巢穴")));

        // 3. 同一行裡兩個佔位符各管各的
        Integer dash = colourOf(hit, "-");
        check("行首的破折號是粉紅（拿到 " + hex(dash) + "）", dash != null && dash == PINK);
        Integer label = colourOf(hit, "獎勵");
        check("同一行的「獎勵」不被粉紅波及（拿到 " + hex(label) + "）",
              label != null && label == GREY);

        // 4. {c:#RRGGBB} 自己指定的顏色
        Integer own = colourOf(hit, "封印的頭盔");
        check("{c:#FFAA00} 指定的金色套得上（拿到 " + hex(own) + "）",
              own != null && own == GOLD);

        // 5. 編號超出範圍時當作沒寫，整句仍然翻得出來
        Path spare = Files.createTempDirectory("wynnchayuan-colour-bad");
        write(spare.resolve("cave.json"), src,
              "{c9}[洞穴完成]{/}\nGrook 巢穴\n- 獎勵：\n- 封印的頭盔");
        TranslationStore lenient = new TranslationStore();
        lenient.loadAll(spare);
        Component odd = LineTranslator.translate(cave(), lenient);
        check("編號超出範圍時整句仍然翻得出來", odd != null);
        check("編號超出範圍時佔位符也不會漏出去",
              odd == null || (!odd.getString().contains("{c9}")
                              && !odd.getString().contains("{/}")));

        report();
    }

    private static void write(Path file, String src, String dst) throws Exception {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty(src, dst);
        Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
    }

    private static String hex(Integer colour) {
        return colour == null ? "null" : String.format("#%06X", colour);
    }

    /**
     * 只看<b>葉節點</b>：上層節點的 getString() 含了所有子節點的字，
     * 拿它去比對一定第一個就中，量到的卻是空的根樣式。
     */
    private static Integer colourOf(Component line, String needle) {
        for (Component part : flatten(line)) {
            if (part.getSiblings().isEmpty() && part.getString().contains(needle)) {
                TextColor colour = part.getStyle().getColor();
                return colour == null ? null : colour.getValue();
            }
        }
        return null;
    }

    private static Boolean boldOf(Component line, String needle) {
        for (Component part : flatten(line)) {
            if (part.getSiblings().isEmpty() && part.getString().contains(needle)) {
                return part.getStyle().isBold();
            }
        }
        return null;
    }

    private static List<Component> flatten(Component root) {
        List<Component> out = new ArrayList<>();
        out.add(root);
        for (Component child : root.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("顏色佔位符：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("顏色佔位符：全部通過");
    }
}
