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
 * 漂浮名牌是<b>就地替換</b>的，所以絕對不能動到遊戲自己排好的版面。
 *
 * <h2>先前壞在哪</h2>
 * 名牌走的是一般的 {@code translate}：查不到整行就退到逐片段替換，然後跑
 * {@code alignColumns}、{@code recenterColumns}、前導置中補償。那一整套是為
 * <b>tooltip</b> 寫的——tooltip 是「標籤 + 數值」的兩欄表格，欄要對齊。
 *
 * <p>漂浮名牌不是表格，它浮在 3D 世界裡，裡面的偏移是 Wynncraft 自己排的。
 * 更糟的是只要<b>任何一個</b>片段查得到（哪怕只是句子裡的一個詞），
 * 那條路就回傳非 null，於是連幾乎沒翻到的名牌也被重排一次。
 *
 * <p>使用者回報「翻譯讓原始 Wynncraft UI 錯位」：職業選擇的三個<b>全英文</b>
 * 標籤互相疊在一起、蓋住圖示。那些字我們根本沒翻，卻被排版邏輯動過。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>整塊查不到就回傳 {@code null}——原文一個位元都不動</li>
 *   <li>查得到時，遊戲自己的排版偏移<b>原封不動</b>填回去</li>
 * </ol>
 */
public final class LabelReplaceTest {

    private static int failures = 0;

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;

    /** Wynncraft 排好的兩行標籤：偏移 + 名稱 ⏎ 偏移 + 標語。 */
    private static StyledText label(String name, String tagline) {
        MutableComponent all = Component.empty();
        all.append(offset(48));
        all.append(lit(name, WHITE));
        all.append(lit("\n", GREY));
        all.append(offset(16));
        all.append(lit(tagline, GREY));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-label");
        FlowedDebug.init(dir);

        // 語料裡只有「Trickster」這個詞，整塊的鍵沒有。
        // 一般那條路會靠這個詞回傳「翻到一點點」，然後把整塊重排。
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Trickster", "詭術師");
        root.addProperty("{#}Royal Guard\n{#}Loyal to the crown", "{#}皇家守衛\n{#}效忠王室");
        Files.writeString(dir.resolve("label.json"), root.toString(), StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        // 1. 整塊查不到 —— 一個位元都不能動
        StyledText partial = label("Trickster", "Confuse with clones");
        Component hit = LineTranslator.translateLabel(partial, store);
        check("整塊查不到就回傳 null（實際："
                      + (hit == null ? "null" : hit.getString().replace("\n", " ⏎ ")) + "）",
              hit == null);

        // 一般那條路確實會動它 —— 這就是先前壞掉的原因，順便釘住這個對照
        Component loose = LineTranslator.translate(partial, store);
        check("對照：一般那條路的確會回傳東西（所以先前才會被重排）", loose != null);

        // 2. 整塊查得到 —— 遊戲自己的偏移原封不動
        StyledText whole = label("Royal Guard", "Loyal to the crown");
        Component done = LineTranslator.translateLabel(whole, store);
        check("整塊查得到就翻得出來", done != null);
        if (done != null) {
            String shown = done.getString();
            System.out.println("      輸出：" + shown.replace("\n", " ⏎ "));
            check("內容有翻出來", shown.contains("皇家守衛") && shown.contains("效忠王室"));
            check("兩行都還在", rows(done) == 2);
            int[] made = leads(done);
            check("第一行的偏移原封不動（原文 48、譯文 "
                          + (made.length > 0 ? made[0] : -1) + "）",
                  made.length > 0 && made[0] == 48);
            check("第二行的偏移原封不動（原文 16、譯文 "
                          + (made.length > 1 ? made[1] : -1) + "）",
                  made.length > 1 && made[1] == 16);
        }

        report();
    }

    private static int rows(Component line) {
        return (int) line.getString().chars().filter(c -> c == '\n').count() + 1;
    }

    /** 每一行開頭那段偏移字元解出來的寬度。 */
    private static int[] leads(Component line) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        int px = 0;
        boolean counting = true;
        for (Component part : flatten(line)) {
            if (!part.getSiblings().isEmpty()) {
                continue;
            }
            String text = part.getString();
            if (text.isEmpty()) {
                continue;
            }
            if (text.indexOf('\n') >= 0) {
                out.add(px);
                px = 0;
                counting = true;
                continue;
            }
            if (!counting) {
                continue;
            }
            if (SpaceOffset.isOffsetRun(text)) {
                px += SpaceOffset.decode(text);
            } else {
                counting = false;
            }
        }
        out.add(px);
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = out.get(i);
        }
        return arr;
    }

    private static java.util.List<Component> flatten(Component root) {
        java.util.List<Component> out = new java.util.ArrayList<>();
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
            System.out.println("名牌就地替換：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("名牌就地替換：全部通過");
    }
}
