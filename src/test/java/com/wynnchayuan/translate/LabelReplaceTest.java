package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LabelReplaceTest {

    private static int failures = 0;

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;

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

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Trickster", "詭術師");
        root.addProperty("{#}Royal Guard\n{#}Loyal to the crown", "{#}皇家守衛\n{#}效忠王室");
        Files.writeString(dir.resolve("label.json"), root.toString(), StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        StyledText partial = label("Trickster", "Confuse with clones");
        Component hit = LineTranslator.translateLabel(partial, store);
        check("整塊查不到就回傳 null（實際："
                      + (hit == null ? "null" : hit.getString().replace("\n", " ⏎ ")) + "）",
              hit == null);

        Component loose = LineTranslator.translate(partial, store);
        check("對照：一般那條路的確會回傳東西（所以先前才會被重排）", loose != null);

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

        com.google.gson.JsonObject plates = new com.google.gson.JsonObject();
        plates.addProperty("Frosted Guard", "霜衛");
        plates.addProperty("Distorted", "扭曲");
        Files.writeString(dir.resolve("npc.json"), plates.toString(),
                          StandardCharsets.UTF_8);
        store = new TranslationStore();
        store.loadAll(dir);

        for (String icons : new String[] {"✦", "✦ ✦", "✦ ✦ ✦"}) {
            StyledText plate = label("Frosted Guard", icons);
            Component out = LineTranslator.translateLabel(plate, store);
            check("狀態圖示「" + icons + "」時名字照樣翻得出來（實際："
                          + (out == null ? "null"
                             : out.getString().replace("\n", " ⏎ ")) + "）",
                  out != null && out.getString().contains("霜衛"));
        }

        StyledText half = label("Frosted Guard", "Unknown Status");
        check("有一行有字卻查不到就整塊不動",
              LineTranslator.translateLabel(half, store) == null);

        for (String status : new String[] {
                "⬤ 12s", "✦ 3 ❄ 5 ⬤ 1s", "❄ 5 ✦ 12", "⬤ 1s"}) {
            StyledText plate = label("Frosted Guard", status);
            Component out = LineTranslator.translateLabel(plate, store);
            check("狀態列「" + status + "」不影響名字（實際："
                          + (out == null ? "null"
                             : out.getString().replace("\n", " ⏎ ")) + "）",
                  out != null && out.getString().contains("霜衛"));
        }

        for (String status : new String[] {"Distorted 12s", "✦ 3 Distorted 5s", "⬤ 1s Distorted 9s"}) {
            StyledText plate = label("Frosted Guard", status);
            Component out = LineTranslator.translateLabel(plate, store);
            String shown = out == null ? "null" : out.getString().replace("\n", " ⏎ ");
            check("狀態列「" + status + "」：名字照翻、狀態詞換掉（實際：" + shown + "）",
                  out != null && shown.contains("霜衛") && shown.contains("扭曲")
                          && !shown.contains("Distorted"));
        }
        check("Distorted 後面還有字就不算狀態列",
              LineTranslator.translateLabel(label("Frosted Guard", "Distorted Memories"), store) == null);

        report();
    }

    private static int rows(Component line) {
        return (int) line.getString().chars().filter(c -> c == '\n').count() + 1;
    }

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
