package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 多欄的聊天訊息：整行查不到就一欄一欄查。
 *
 * <h2>先前壞在哪</h2>
 * 獵殺信標的選單是兩欄併排，而兩欄的內容是<b>各自獨立</b>抽出來的。聊天走的是
 * 「只認整行的鍵」那條路，所以每一種左右配對都要在語料裡各列一條——12 種信標
 * 顏色配上十幾種效果是上百種組合。
 *
 * <p>語料裡因此硬列了 <b>728 條</b>成對條目，玩家還是三天兩頭遇到沒翻到的：
 * 這件事在回報裡出現過四次。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>兩欄都有單欄譯文 → 兩欄都翻出來（組合爆炸消失）</li>
 *   <li>只有一欄有 → 那一欄翻、另一欄<b>留著英文</b>，不能整行放棄</li>
 *   <li>一欄都沒有 → 回傳 null，交還給呼叫端當「查不到」</li>
 *   <li>單欄的行不受影響</li>
 * </ol>
 *
 * <p>第二點是重點：最壞情況要是「一半中文一半英文」，那本來就是查不到整行時
 * 的樣子（整行英文），不會更糟。
 */
public final class ChatColumnTest {

    private static int failures = 0;

    private static final int GREY = 0xAAAAAA;

    /** 兩欄併排：偏移、文字、偏移、文字——實機信標選單就是這個形狀。 */
    private static StyledText row(String left, String right) {
        MutableComponent all = Component.empty();
        all.append(offset(33));
        all.append(lit(left));
        all.append(offset(69));
        all.append(lit(right));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static MutableComponent lit(String text) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GREY)));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-columns");
        FlowedDebug.init(dir);

        // 語料裡<b>只有單欄</b>的條目，一條配對都沒有——這正是重點。
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("{#}Purple Beacon", "{#}紫色信標");
        root.addProperty("{#}Blue Beacon", "{#}藍色信標");
        root.addProperty("{#}Reward Pulls", "{#}獎勵抽數");
        Files.writeString(dir.resolve("lootrun.json"), root.toString(),
                          StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Component both = LineTranslator.translateChat(row("Purple Beacon", "Blue Beacon"),
                                                     store);
        check("兩欄都有單欄譯文就都翻出來", both != null);
        if (both != null) {
            String t = both.getString();
            System.out.println("      輸出：" + t);
            check("左欄翻了", t.contains("紫色信標"));
            check("右欄翻了", t.contains("藍色信標"));
            check("沒有殘留英文", !t.contains("Beacon"));
        }

        // ★ 只有一欄查得到：那一欄翻，另一欄留英文，不能整行放棄。
        Component half = LineTranslator.translateChat(
                row("Reward Pulls", "Totally Unknown Thing"), store);
        check("只有一欄查得到時仍然翻得出來", half != null);
        if (half != null) {
            String t = half.getString();
            System.out.println("      輸出：" + t);
            check("查得到的那一欄翻了", t.contains("獎勵抽數"));
            check("查不到的那一欄留著英文", t.contains("Totally Unknown Thing"));
        }

        // 一欄都查不到 → 當作查不到，讓呼叫端顯示原文。
        Component none = LineTranslator.translateChat(
                row("Nothing Here", "Nothing There"), store);
        check("一欄都查不到就回傳 null（實際 "
                      + (none == null ? "null" : none.getString()) + "）",
              none == null);

        // 單欄的行不受影響：查不到就是查不到，不會被拆。
        MutableComponent one = Component.empty();
        one.append(offset(33));
        one.append(lit("Reward Pulls"));
        Component single = LineTranslator.translateChat(
                StyledText.fromComponent(one), store);
        check("單欄的行照常翻", single != null && single.getString().contains("獎勵抽數"));

        colours();
        report();
    }

    /**
     * 顏色佔位符要按接好之後的<b>出現順序</b>重編。
     *
     * <h2>為什麼</h2>
     * {@code {cN}} 指的是「這一行第 N 個出現的顏色」，所以編號跟欄位在哪一欄
     * 有關：同一個「紫色信標」在左欄是 {@code {c1}}、在右欄是 {@code {c2}}。
     * 語料裡因此同一欄存了兩種寫法——那正是先前只能一對一對硬列的原因之一。
     *
     * <p>單欄的條目沒辦法知道自己會被放到第幾欄，所以一律寫 {@code {c1}}，
     * 接起來之後重編。沒有這一步，右欄會拿到左欄的顏色——那正是 v1.99.89
     * 修過的「黃色信標變成橘色」。
     */
    private static void colours() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-columns-colour");
        FlowedDebug.init(dir);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        // 兩條都寫 {c1}：單欄條目就是這個形狀。
        root.addProperty("{#}Purple Beacon", "{#}{c1}紫色信標{/}");
        root.addProperty("{#}Pink Beacon", "{#}{c1}粉紅信標{/}");
        Files.writeString(dir.resolve("lootrun.json"), root.toString(),
                          StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Component out = LineTranslator.translateChat(
                row("Purple Beacon", "Pink Beacon"), store);
        check("兩欄都寫 {c1} 時仍翻得出來", out != null);
        if (out != null) {
            System.out.println("      輸出：" + out.getString());
            check("兩欄都翻了", out.getString().contains("紫色信標")
                    && out.getString().contains("粉紅信標"));
        }

        // 位移本身。第一欄不動，後面的欄按前面用掉的顏色數往後推。
        check("第一欄不動",
              "{c1}A{/}".equals(LineTranslator.shiftColours("{c1}A{/}", 0)));
        check("第二欄的 {c1} 推成 {c2}",
              "{c2}A{/}".equals(LineTranslator.shiftColours("{c1}A{/}", 1)));

        // ★ 欄<b>內</b>重複用同一個編號要保持同色——這是「逐欄推」才做得到的，
        // 接好之後整串重編會把它拆成兩個顏色。
        check("欄內重複用同一編號仍是同色",
              "{c3}A{/}{c3}B{/}".equals(
                      LineTranslator.shiftColours("{c1}A{/}{c1}B{/}", 2)));
        check("欄內兩種顏色的相對關係不變",
              "{c3}A{/}{c4}B{/}".equals(
                      LineTranslator.shiftColours("{c1}A{/}{c2}B{/}", 2)));

        check("算得出用了幾種顏色",
              LineTranslator.distinctColours("{c1}A{/}{c1}B{/}{c2}C{/}") == 2);
        check("沒有顏色佔位符就是 0",
              LineTranslator.distinctColours("沒有顏色") == 0);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("多欄聊天：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("多欄聊天：全部通過");
    }
}
