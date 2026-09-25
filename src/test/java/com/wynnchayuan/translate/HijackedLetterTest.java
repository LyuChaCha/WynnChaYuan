package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import com.wynntils.core.text.StyledText;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code Á} 在 Wynncraft 不是字母，是全螢幕黑幕。
 *
 * <h2>實機回報</h2>
 * 西班牙文玩家在伺服器上發現新區域時<b>整個畫面變全黑</b>，而聊天那一行的
 * 第一個字不見了：
 *
 * <pre>
 *   應該是  Área descubierta: Farmers Settlement (+25 XP)
 *   畫出來  rea descubierta: Farmers Settlement (+25 XP)
 * </pre>
 *
 * <p>資源包裡 {@code minecraft:default} 引用的 {@code deprecated} 字型把
 * U+00C1 指到 {@code font/screen/static/fade.png}——他們的淡出黑幕。
 * 玩家看到的不是缺字，是我們請遊戲畫了一張蓋住整個畫面的黑圖。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>一般路徑（聊天、tooltip、HUD）畫出來不可以有 {@code Á}；</li>
 *   <li>Wynncraft 的<b>對話字型</b>自己帶了拉丁重音字母、而且沒有引用
 *       {@code deprecated}，那裡要原樣留著；</li>
 *   <li>原文本來就有的那個字元不能被動到——那是遊戲自己要的圖。</li>
 * </ul>
 */
public final class HijackedLetterTest {

    private static int failures = 0;

    /** 聊天那一行的字型：引用了 deprecated，Á 在這裡是黑幕。 */
    private static final Style CHAT = Style.EMPTY.withFont(
            new FontDescription.Resource(
                    Identifier.withDefaultNamespace("language/wynncraft")));

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-hijacked");
        Files.writeString(dir.resolve("discovery.json"), """
                {
                  "Area Discovered": "Área descubierta",
                  "Open the door": "ÁBRETE"
                }
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        chatLine(store);
        dialogueLine(store);
        originalKept(store);

        System.out.println(failures == 0
                ? "HijackedLetterTest 全部通過" : failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** ★ 回報的那一行：畫出來絕對不能帶著 Á。 */
    private static void chatLine(TranslationStore store) {
        String shown = shown("Area Discovered", CHAT, store);
        check("聊天那一行換掉了（實際 " + shown + "）",
              shown.startsWith("Area descubierta"));
        check("整行一個 Á 都不剩", shown.indexOf('Á') < 0);
    }

    /**
     * 對話框不受影響。
     *
     * <p>對話走的是 {@code DialogueRewriter}，不經過畫字的那一步：那邊的規則是
     * 「譯文<b>畫得出來</b>就完全不碰字型」，而 Wynncraft 的對話字型自己帶了一張
     * {@code wynncraft_latin.png}（沒有引用 {@code deprecated}），{@code Á}
     * 在那裡是正常的字母。所以西班牙文的「ÁBRETE」在對話框裡照樣有重音——
     * 前提是<b>語料裡還留著它</b>，這一條就是釘住這件事。
     */
    private static void dialogueLine(TranslationStore store) {
        check("語料本身留著重音（實際 " + store.lookup("Open the door") + "）",
              "ÁBRETE".equals(store.lookup("Open the door")));
    }

    /**
     * 原文本來就有的不能動。
     *
     * <p>Wynncraft 自己就用這個字元畫黑幕，查不到譯文時照原樣送回去是<b>對的</b>。
     * 一律換掉的話，那張黑幕就再也放不出來。
     */
    private static void originalKept(TranslationStore store) {
        Component out = LineTranslator.translate(
                StyledText.fromComponent(Component.literal("Á").withStyle(CHAT)),
                store);
        check("查不到譯文就不碰（實際 " + (out == null ? "沒翻" : out.getString()) + "）",
              out == null || "Á".equals(out.getString()));
    }

    private static String shown(String text, Style style, TranslationStore store) {
        Component out = LineTranslator.translate(
                StyledText.fromComponent(Component.literal(text).withStyle(style)),
                store);
        return out == null ? "" : out.getString();
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
