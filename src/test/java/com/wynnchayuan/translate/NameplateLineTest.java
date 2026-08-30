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
 * 多行名牌：整塊查不到時，應該<b>逐行</b>查。
 *
 * <h2>先前壞在哪</h2>
 * 怪物名牌是<b>單一個含換行的 StyledText</b>——名字、等級、血條全在同一個鍵裡
 * （{@code "Teleporter\ndock"}、{@code "Sylphid Gatekeeper {#}{#}\n{#} {#} {#}"}）。
 * 於是同一隻怪只要多出一行，就變成另一個鍵，得再翻一次。
 *
 * <p>實際的語料裡，{@code npc.json} 有 388 條多行的鍵，其中 281 條沒翻——
 * 而它們絕大多數只是<b>同一個名字的不同變體</b>。使用者看到的是「這個角度
 * 有翻、那個角度沒翻」。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>名字有單行的譯文時，多行變體也要翻得出來——<b>不需要為每個變體各建一個鍵</b></li>
 *   <li>查不到的那一行原樣留著，不能被吃掉</li>
 *   <li>一行都查不到時不硬翻</li>
 * </ol>
 *
 * <p>第三點是與 {@link LabelReplaceTest} 的分工：那邊管<b>就地替換</b>的標籤
 * （{@code translateLabel}，整塊查不到就一個位元都不動）；這裡管注視框走的
 * {@code translate}。兩條路的取捨不同，因為前者會動到遊戲自己排好的版面。
 */
public final class NameplateLineTest {

    private static int failures = 0;

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;

    /** 名字一行、提示一行，兩行不同色——真實名牌就是這個形狀。 */
    private static StyledText plate(String name, String hint) {
        MutableComponent all = Component.empty();
        all.append(lit(name, WHITE));
        all.append(lit("\n", GREY));
        all.append(lit(hint, GREY));
        return StyledText.fromComponent(all);
    }

    /** 整塊只有一個片段——名字與提示同色時就是這樣，逐片段替換救不了。 */
    private static StyledText onePiece(String text) {
        return StyledText.fromComponent(lit(text, WHITE));
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-nameplate");
        FlowedDebug.init(dir);

        // 語料裡<b>只有名字</b>，沒有任何多行的鍵——這正是實際的狀況。
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Teleporter", "傳送點");
        root.addProperty("Sylphid Gatekeeper", "西爾菲德守門人");
        Files.writeString(dir.resolve("npc.json"), root.toString(), StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        // 1. 兩個片段的名牌
        StyledText twoParts = plate("Teleporter", "to dock");
        Component a = LineTranslator.translate(twoParts, store);
        show("兩片段名牌", a);
        check("名字翻得出來", a != null && a.getString().contains("傳送點"));
        check("查不到的那一行原樣留著",
              a != null && a.getString().contains("to dock"));
        check("還是兩行", a != null && rows(a) == 2);

        // 2. 整塊只有一個片段 —— 逐片段替換在這裡救不了，只有逐行能救
        StyledText single = onePiece("Sylphid Gatekeeper\nGuarding the gate");
        Component b = LineTranslator.translate(single, store);
        show("單片段名牌", b);
        check("單片段時名字也翻得出來",
              b != null && b.getString().contains("西爾菲德守門人"));
        check("單片段時查不到的那一行也留著",
              b != null && b.getString().contains("Guarding the gate"));

        // 3. 技能樹的「Unlock X ability」——外層那兩個詞是<b>獨立片段</b>，
        //    所以補上 Unlock 與 ability 兩條就能覆蓋全部技能，
        //    不必為一百八十個技能各建一條整行的鍵。
        com.google.gson.JsonObject labels = new com.google.gson.JsonObject();
        labels.addProperty("Unlock", "解鎖");
        labels.addProperty("ability", "技能");
        labels.addProperty("Implosion", "內爆");
        Files.writeString(dir.resolve("ability-labels.json"), labels.toString(),
                          StandardCharsets.UTF_8);
        TranslationStore withLabels = new TranslationStore();
        withLabels.loadAll(dir);

        MutableComponent unlock = Component.empty();
        unlock.append(lit("Unlock ", GREY));
        unlock.append(lit("Implosion", 0xFFAA00));
        unlock.append(lit(" ability", GREY));
        Component d3 = LineTranslator.translate(StyledText.fromComponent(unlock), withLabels);
        show("Unlock X ability", d3);
        check("Unlock 有翻", d3 != null && d3.getString().contains("解鎖"));
        check("技能名有翻", d3 != null && d3.getString().contains("內爆"));
        check("ability 有翻", d3 != null && d3.getString().contains("技能"));
        check("沒有殘留英文",
              d3 != null && !d3.getString().contains("Unlock")
                      && !d3.getString().contains("ability"));

        // 4. 一行都查不到 —— 不硬翻
        StyledText miss = onePiece("Unknown Thing\nsome hint");
        Component c = LineTranslator.translate(miss, store);
        check("一行都查不到時回傳 null（實際："
                      + (c == null ? "null" : c.getString().replace("\n", " ⏎ ")) + "）",
              c == null);

        report();
    }

    private static void show(String what, Component c) {
        System.out.println("      " + what + "：「"
                                   + (c == null ? "null" : c.getString().replace("\n", " ⏎ "))
                                   + "」");
    }

    private static int rows(Component line) {
        return (int) line.getString().chars().filter(ch -> ch == '\n').count() + 1;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures == 0) {
            System.out.println("名牌逐行：全部通過");
        } else {
            System.out.println("名牌逐行：" + failures + " 項失敗");
            System.exit(1);
        }
    }
}
