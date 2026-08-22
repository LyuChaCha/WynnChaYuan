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
 * 方塊字不跟著原文一起斜。
 *
 * <h2>畫面上長什麼樣</h2>
 * Minecraft 的斜體是<b>把字形往右推一個剪切</b>，不是另一套字型。拉丁字母本來就有
 * 傾斜的字形設計，剪切之後還算好看；方塊字沒有，剪切出來的是糊成一團的斜方塊——
 * 筆畫互相穿插，相鄰兩個字會疊到。
 *
 * <p>而 GUI 的標題幾乎全部是斜的：Minecraft 只要物品有自訂名稱就自動加上斜體，
 * Wynncraft 沒有特地關掉。原文是拉丁字母所以沒人在意，一換成中文就整排糊掉。
 *
 * <p>要留意的是<b>診斷檔看起來完全正常</b>：原文與譯文都印 {@code 斜V}，
 * 兩邊一致。問題不在「抄錯了」，而在同一個屬性套在兩種文字上的效果差太多。
 */
public final class UprightTest {

    private static int failures = 0;

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations"));

        // GUI 標題：Minecraft 自動加上的斜體 + Wynncraft 自己的粗體
        Style title = Style.EMPTY
                .withColor(TextColor.fromRgb(0xFFFFFF))
                .withItalic(true)
                .withBold(true);
        Component out = LineTranslator.translate(
                StyledText.fromComponent(
                        Component.literal("Your Islands").withStyle(title)),
                store, false);
        check("標題翻得出來", out != null && out.getString().contains("你的島嶼"));
        if (out != null) {
            check("中文不再是斜的", !italicOf(out, "你的島嶼"));
            check("粗體照舊——只丟斜體", boldOf(out, "你的島嶼"));
        }

        // 同一行裡的英文片段該斜還是斜，跟原文一致。判斷是<b>逐片段</b>的，
        // 不是整行一起關掉。
        MutableComponent mixed = Component.empty();
        mixed.append(Component.literal("Ragni Citizen").withStyle(title));
        Component both = LineTranslator.translate(
                StyledText.fromComponent(mixed), store, false);
        check("混排的那一行翻得出來",
                both != null && both.getString().contains("市民"));
        if (both != null) {
            check("中文那半不斜", !italicOf(both, "市民"));
            check("英文那半仍然照原文斜", italicOf(both, "Ragni"));
        }

        System.out.println(failures == 0
                ? "Upright: 全部通過" : "Upright: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static boolean italicOf(Component out, String needle) {
        Component part = find(out, needle);
        return part != null && part.getStyle().isItalic();
    }

    private static boolean boldOf(Component out, String needle) {
        Component part = find(out, needle);
        return part != null && part.getStyle().isBold();
    }

    private static Component find(Component out, String needle) {
        for (Component part : flatten(out)) {
            if (part.getContents().toString().contains(needle)) {
                return part;
            }
        }
        return null;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        for (Component child : c.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
