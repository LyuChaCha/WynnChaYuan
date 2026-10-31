package com.wynnchayuan.listener;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ActionBarColumnTest {

    private static int failures = 0;

    private static final int GREY = 0xAAAAAA;
    private static final String GAP = " ".repeat(22);

    private static MutableComponent lit(String text) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GREY)));
    }

    private static StyledText oneRun(String text) {
        return StyledText.fromComponent(lit(text));
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("wcy-column");
        for (var field : java.util.Map.of(
                "config", (Object) new ModConfig(tmp.resolve("config.json")),
                "store", new CaptureStore(tmp.resolve("captured.json"))).entrySet()) {
            java.lang.reflect.Field held = WynnChaYuan.class.getDeclaredField(field.getKey());
            held.setAccessible(true);
            held.set(null, field.getValue());
        }
        Path dir = Files.createTempDirectory("wynnchayuan-actionbar");
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Left-Click to play", "左鍵點擊遊玩");
        root.addProperty("Right-Click to switch", "右鍵點擊切換");
        Files.writeString(dir.resolve("misc.json"), root.toString(),
                          StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

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

        Component half = ActionBarListener.columnSwap(
                oneRun("Left-Click to play" + GAP + "Middle-Click to explode"), store);
        check("只有一欄查得到時仍然換得出來", half != null);
        if (half != null) {
            String t = half.getString();
            System.out.println("      輸出：[" + t + "]");
            check("查得到的那一欄翻了", t.contains("左鍵點擊遊玩"));
            check("查不到的那一欄留著英文", t.contains("Middle-Click to explode"));
        }

        Component none = ActionBarListener.columnSwap(
                oneRun("Nothing Here" + GAP + "Nothing There"), store);
        check("一欄都查不到就回傳 null（實際 "
                      + (none == null ? "null" : "[" + none.getString() + "]") + "）",
              none == null);

        Component single = ActionBarListener.columnSwap(
                oneRun("Left-Click to play"), store);
        check("單欄的行不走這條路（回傳 null）", single == null);

        Component spaced = ActionBarListener.columnSwap(
                oneRun("Left-Click  to play"), store);
        check("兩個空白才算欄距，而這一行的兩邊都查不到",
              spaced == null);

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
