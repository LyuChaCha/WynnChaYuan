package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChatColumnTest {

    private static int failures = 0;

    private static final int GREY = 0xAAAAAA;

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

        Component half = LineTranslator.translateChat(
                row("Reward Pulls", "Totally Unknown Thing"), store);
        check("只有一欄查得到時仍然翻得出來", half != null);
        if (half != null) {
            String t = half.getString();
            System.out.println("      輸出：" + t);
            check("查得到的那一欄翻了", t.contains("獎勵抽數"));
            check("查不到的那一欄留著英文", t.contains("Totally Unknown Thing"));
        }

        Component none = LineTranslator.translateChat(
                row("Nothing Here", "Nothing There"), store);
        check("一欄都查不到就回傳 null（實際 "
                      + (none == null ? "null" : none.getString()) + "）",
              none == null);

        MutableComponent one = Component.empty();
        one.append(offset(33));
        one.append(lit("Reward Pulls"));
        Component single = LineTranslator.translateChat(
                StyledText.fromComponent(one), store);
        check("單欄的行照常翻", single != null && single.getString().contains("獎勵抽數"));

        colours();
        multiline();
        numbers();
        report();
    }

    private static void numbers() {
        check("第一欄不動（偏移 0）",
              "經過時間：{~1}:{~2}".equals(
                      LineTranslator.renumber("經過時間：{~1}:{~2}", 0)));
        check("指名的往後推",
              "經過時間：{~2}:{~3}".equals(
                      LineTranslator.renumber("經過時間：{~1}:{~2}", 1)));
        check("沒編號的也改寫成指名",
              "共 {~3} 場".equals(LineTranslator.renumber("共 {~} 場", 2)));
        check("混用時忠實保留原本的語意",
              "{~3} / {~4} 共 {~3}".equals(
                      LineTranslator.renumber("{~1} / {~2} 共 {~}", 2)));
        check("沒有數值的譯文原樣回傳",
              "獎勵抽數".equals(LineTranslator.renumber("獎勵抽數", 3)));
        check("超過一位數就回傳 null",
              LineTranslator.renumber("{~2}", 8) == null);
    }

    private static void colours() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-columns-colour");
        FlowedDebug.init(dir);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
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

        check("第一欄不動",
              "{c1}A{/}".equals(ColourTemplate.shiftColours("{c1}A{/}", 0)));
        check("第二欄的 {c1} 推成 {c2}",
              "{c2}A{/}".equals(ColourTemplate.shiftColours("{c1}A{/}", 1)));

        check("欄內重複用同一編號仍是同色",
              "{c3}A{/}{c3}B{/}".equals(
                      ColourTemplate.shiftColours("{c1}A{/}{c1}B{/}", 2)));
        check("欄內兩種顏色的相對關係不變",
              "{c3}A{/}{c4}B{/}".equals(
                      ColourTemplate.shiftColours("{c1}A{/}{c2}B{/}", 2)));

        check("算得出用了幾種顏色",
              ColourTemplate.distinctColours("{c1}A{/}{c1}B{/}{c2}C{/}") == 2);
        check("沒有顏色佔位符就是 0",
              ColourTemplate.distinctColours("沒有顏色") == 0);
    }

    private static void multiline() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-columns-multi");
        FlowedDebug.init(dir);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("{#}Choose a Beacon!", "{#}選擇一個信標！");
        root.addProperty("{#}Walk towards one", "{#}走向其中一個");
        Files.writeString(dir.resolve("lootrun.json"), root.toString(),
                          StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        MutableComponent block = Component.empty();
        block.append(offset(102));
        block.append(lit("Choose a Beacon!"));
        block.append(lit("\n"));
        block.append(offset(58));
        block.append(lit("Walk towards one"));

        Component out = LineTranslator.translateChat(
                StyledText.fromComponent(block), store);
        if (out == null) {
            check("整塊沒有自己的鍵時不硬翻", true);
            return;
        }
        String t = out.getString();
        System.out.println("      輸出：" + t.replace("\n", " ⏎ "));
        check("★ 兩行不能被黏成一行（實際 " + (rows(t)) + " 行）", rows(t) == 2);
    }

    private static int rows(String text) {
        return (int) text.chars().filter(c -> c == '\n').count() + 1;
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
