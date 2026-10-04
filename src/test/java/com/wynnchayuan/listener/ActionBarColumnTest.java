package com.wynnchayuan.listener;

import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code /class} 畫面下方那一行：兩欄擠在<b>同一個片段</b>裡。
 *
 * <h2>實機長什麼樣</h2>
 * 使用者回報「還是英文」，而 {@code captured.json} 收到的整行是：
 *
 * <pre>{@code
 * {#}{#}{#}\u0001AS{~}{#}{#}v{~}_{~}{#}{#}{#} Left-Click to play                      Right-Click to switch{#}{#}\u0001PoorChaCha{#}
 * }</pre>
 *
 * 兩句之間是 <b>22 個普通的 U+0020</b>——不是排版用的空白字型，所以元件不會在
 * 那裡斷片段。逐片段那條路把整串當成一段去查，當然落空；可是語料裡
 * {@code Left-Click to play} 與 {@code Right-Click to switch} 兩句早就各自翻好了
 * （#1010 加的）。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>兩欄都查得到 → 兩欄都換，中間那串空白<b>原樣保留</b>（它是欄距）</li>
 *   <li>只有一欄查得到 → 那一欄換，另一欄留英文，不能整行放棄</li>
 *   <li>一欄都查不到 → 回傳 null，呼叫端顯示原文</li>
 *   <li>單欄的行不受影響（沒有兩個以上連續空白就不切）</li>
 *   <li>單一空白不是欄距：{@code to play} 不能被切成 {@code to} 與 {@code play}</li>
 *   <li>行首行尾的縮排不是欄距，切了會多出空片段</li>
 * </ol>
 */
public final class ActionBarColumnTest {

    private static int failures = 0;

    private static final int GREY = 0xAAAAAA;
    /** 實機數到的欄距：22 個 U+0020。 */
    private static final String GAP = " ".repeat(22);

    private static MutableComponent lit(String text) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GREY)));
    }

    /** 整行只有一個文字片段——實機就是這個形狀，兩欄沒有被拆開。 */
    private static StyledText oneRun(String text) {
        return StyledText.fromComponent(lit(text));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-actionbar");
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Left-Click to play", "左鍵點擊遊玩");
        root.addProperty("Right-Click to switch", "右鍵點擊切換");
        Files.writeString(dir.resolve("misc.json"), root.toString(),
                          StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        // ★ 實機那一行。前面還有一個空白（圖示與文字之間），不能被當成欄距。
        Component both = ActionBarListener.columnSwap(
                oneRun(" Left-Click to play" + GAP + "Right-Click to switch"), store);
        check("兩欄都查得到就都換掉", both != null);
        if (both != null) {
            String t = both.getString();
            System.out.println("      輸出：[" + t + "]");
            check("左欄翻了", t.contains("左鍵點擊遊玩"));
            check("右欄翻了", t.contains("右鍵點擊切換"));
            check("沒有殘留英文", !t.contains("Click"));
            check("欄距原樣保留（22 個空白）", t.contains(GAP));
            check("開頭那個空白還在", t.startsWith(" "));
        }

        // 只有一欄查得到：另一欄留英文，比整行英文好。
        Component half = ActionBarListener.columnSwap(
                oneRun("Left-Click to play" + GAP + "Middle-Click to explode"), store);
        check("只有一欄查得到時仍然換得出來", half != null);
        if (half != null) {
            String t = half.getString();
            System.out.println("      輸出：[" + t + "]");
            check("查得到的那一欄翻了", t.contains("左鍵點擊遊玩"));
            check("查不到的那一欄留著英文", t.contains("Middle-Click to explode"));
        }

        // 一欄都查不到 → null，呼叫端顯示原文。
        Component none = ActionBarListener.columnSwap(
                oneRun("Nothing Here" + GAP + "Nothing There"), store);
        check("一欄都查不到就回傳 null（實際 "
                      + (none == null ? "null" : "[" + none.getString() + "]") + "）",
              none == null);

        // 單欄的行：沒有欄距就不會被切，整行查表那條路已經試過了。
        Component single = ActionBarListener.columnSwap(
                oneRun("Left-Click to play"), store);
        check("單欄的行不走這條路（回傳 null）", single == null);

        // ★ 單一空白不是欄距。`to play` 被切開的話 `Left-Click` 查不到，
        // 整行就只剩半句中文——比沒翻更糟。
        Component spaced = ActionBarListener.columnSwap(
                oneRun("Left-Click  to play"), store);
        check("兩個空白才算欄距，而這一行的兩邊都查不到",
              spaced == null);

        // 行首行尾的縮排不是欄距：切下去會多出空片段，而且那些空白常常是
        // 排版用的，必須原封不動。
        Component indented = ActionBarListener.columnSwap(
                oneRun("    Left-Click to play" + GAP + "Right-Click to switch    "),
                store);
        check("縮排的行也翻得出來", indented != null);
        if (indented != null) {
            String t = indented.getString();
            System.out.println("      輸出：[" + t + "]");
            check("行首縮排原樣保留", t.startsWith("    "));
            check("行尾縮排原樣保留", t.endsWith("    "));
        }

        fonts();
        report();
    }

    /**
     * 六個語言都要有那一份字型，而且位移要對。
     *
     * <h2>為什麼位移錯了看不出來</h2>
     * 字還在、顏色也對，只是畫到別的高度——使用者回報的原話是「字太高了、
     * 沒有對齊」。測試跑在 headless，畫不出畫面，所以只能釘住<b>數字</b>：
     *
     * <p>Wynncraft 把「畫在畫面的哪個高度」烘進字型的 {@code ascent}。
     * {@code hud/selector/default/bottom_middle} 的拉丁字是 {@code -48}，
     * 而我們十一份對話字型量出來的關係是 {@code shift_y = 7 - ascent}
     * （{@code body_0} 34→-27、{@code control} -38→45、{@code nameplate}
     * 50→-43，全部吻合）。所以這裡只能是 {@code 7 - (-48) = 55}。
     *
     * <p>少一份字型比位移錯更糟：{@code FontManager} 對查不到的 id 是拿
     * {@code AllMissingGlyphProvider} 頂上，整列變方框。
     */
    private static void fonts() throws Exception {
        for (String lang : new String[] {"zh_tw", "zh_cn", "ja_jp", "ko_kr",
                                         "ru_ru", "es_es"}) {
            String path = "/assets/wynnchayuan/font/actionbar/" + lang
                    + "/selector_bottom.json";
            try (java.io.InputStream in =
                         ActionBarColumnTest.class.getResourceAsStream(path)) {
                if (in == null) {
                    check(lang + " 有 selector_bottom.json", false);
                    continue;
                }
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                check(lang + " 參照遊戲自己那一份（ASCII 外觀不變）",
                      text.contains("minecraft:hud/selector/default/bottom_middle"));
                check(lang + " 位移是 55（= 7 - (-48)）",
                      text.replaceAll("\\s+", "").contains("\"shift\":[0,55]"));
                check(lang + " 指到自己的 ttf",
                      text.contains("wynnchayuan:fusion"));
            }
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ✔ " : "  ✘ ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println(failures + " 項沒通過");
            System.exit(1);
        }
        System.out.println("action bar 的欄位切分正確。");
    }
}
