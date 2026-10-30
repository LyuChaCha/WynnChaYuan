package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NameplateLineTest {

    private static int failures = 0;

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;

    private static StyledText plate(String name, String hint) {
        MutableComponent all = Component.empty();
        all.append(lit(name, WHITE));
        all.append(lit("\n", GREY));
        all.append(lit(hint, GREY));
        return StyledText.fromComponent(all);
    }

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

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Teleporter", "傳送點");
        root.addProperty("Sylphid Gatekeeper", "西爾菲德守門人");
        Files.writeString(dir.resolve("npc.json"), root.toString(), StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        StyledText twoParts = plate("Teleporter", "to dock");
        Component a = LineTranslator.translate(twoParts, store);
        show("兩片段名牌", a);
        check("名字翻得出來", a != null && a.getString().contains("傳送點"));
        check("查不到的那一行原樣留著",
              a != null && a.getString().contains("to dock"));
        check("還是兩行", a != null && rows(a) == 2);

        StyledText single = onePiece("Sylphid Gatekeeper\nGuarding the gate");
        Component b = LineTranslator.translate(single, store);
        show("單片段名牌", b);
        check("單片段時名字也翻得出來",
              b != null && b.getString().contains("西爾菲德守門人"));
        check("單片段時查不到的那一行也留著",
              b != null && b.getString().contains("Guarding the gate"));

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
