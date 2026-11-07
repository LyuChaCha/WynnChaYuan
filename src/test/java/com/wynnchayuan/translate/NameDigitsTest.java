package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.wynnchayuan.capture.CaptureStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class NameDigitsTest {

    private static final int AQUA = 0x55FFFF;
    private static final int GREY = 0xAAAAAA;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-name-digits");
        FlowedDebug.init(dir);
        TranslationStore store = corpus(dir.resolve("corpus"));

        twoRowPlates(store);
        reorderedNumbers(store);
        capture(store, dir);
        shippedCorpus();

        System.out.println(failures == 0 ? "名字帶數字：全部通過" : "名字帶數字：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void twoRowPlates(TranslationStore store) {
        System.out.println("=== 兩列的漂浮字 ===");
        for (String name : List.of("Bobby", "12bobby", "bobby12", "Bob_7x", "4ab5")) {
            Component hit = LineTranslator.translateLabel(well(name, "30s"), store);
            String got = hit == null ? null : hit.getString();
            check("★ 「" + name + "」的力量之井翻了（實際 " + got + "）",
                  got != null && got.startsWith(name + " WELL-OF")
                          && got.indexOf(name) == got.lastIndexOf(name) && got.endsWith("[30s]"));
            check("名字還是名字的顏色（" + name + "）", hit != null && colourOf(hit, name) == AQUA);
        }
        Component single = LineTranslator.translateLabel(StyledText.fromComponent(
                Component.empty().append(lit("bobby12", AQUA)).append(lit("'s Well of Power", GREY))),
                store);
        check("一列的照舊翻（實際 " + (single == null ? null : single.getString()) + "）",
              single != null && single.getString().equals("bobby12 WELL-OF"));
    }

    private static void reorderedNumbers(TranslationStore store) {
        System.out.println("=== 譯文把數值搬到名字前面 ===");
        Component hit = LineTranslator.translateLabel(StyledText.fromComponent(Component.empty()
                .append(lit("bobby12", AQUA)).append(lit(" keeps 3 hearts", GREY))
                .append(lit("\n[30s]", GREY))), store);
        String got = hit == null ? null : hit.getString();
        check("★ 名字的數字不會被填到別的數值上（實際 " + got + "）",
              got == null || (got.contains("bobby12") && got.contains("3 HEARTS")));
        Component plain = LineTranslator.translateLabel(StyledText.fromComponent(Component.empty()
                .append(lit("bobby", AQUA)).append(lit(" keeps 3 hearts", GREY))
                .append(lit("\n[30s]", GREY))), store);
        check("名字沒有數字時照翻（實際 " + (plain == null ? null : plain.getString()) + "）",
              plain != null && plain.getString().startsWith("3 HEARTS bobby"));
    }

    private static void capture(TranslationStore store, Path dir) throws Exception {
        System.out.println("=== 收集端 ===");
        for (String name : List.of("12bobby", "bobby12")) {
            String framed = LineTranslator.framedRows(well(name, "30s"), store);
            check("★ 收的是 {u} 那一份（" + name + "：" + framed + "）",
                  "{u}'s Well of Power\n[{~}s]".equals(framed));
        }
        check("一列的不歸這裡管", LineTranslator.framedRows(
                StyledText.fromString("bobby12's Well of Power"), store) == null);
        check("沒有名字的兩列不動", LineTranslator.framedRows(
                StyledText.fromString("Iron Ore\n[30s]"), store) == null);

        Path file = dir.resolve("captured.json");
        JsonObject entries = new JsonObject();
        entries.add("0001", row("{~}bobby's Well of Power", "label/floating"));
        entries.add("0002", row("bobby{~}'s Well of Power", "label/floating"));
        entries.add("0003", row("more Chest Loot from BOBBY{~} [{~}m]", "label/bossbar"));
        entries.add("0004", row("Bobby was wrapped up by a spider.", "title"));
        entries.add("0005", row("Iron Ore", "label/floating"));
        entries.add("0006", row("Carl was wrapped up by a spider.", "gui/line"));
        JsonObject root = new JsonObject();
        root.add("entries", entries);
        Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        CaptureStore captured = new CaptureStore(file);
        captured.knowsPlayerFrames(t -> store.nameFrames().framesName(t));
        captured.flush();
        check("★ 舊檔裡帶名字的力量之井清掉了",
              !captured.contains("{~}bobby's Well of Power")
                      && !captured.contains("bobby{~}'s Well of Power"));
        check("★ 舊檔裡帶名字的 boss bar 清掉了",
              !captured.contains("more Chest Loot from BOBBY{~} [{~}m]"));
        check("★ 舊檔裡帶名字的死亡標題清掉了",
              !captured.contains("Bobby was wrapped up by a spider."));
        check("一般的字留著", captured.contains("Iron Ore"));
        check("物品說明那條路本來就不認名字，留著",
              captured.contains("Carl was wrapped up by a spider.") && captured.size() == 2);
    }

    private static void shippedCorpus() {
        System.out.println("=== 出貨語料 ===");
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations/zh_tw");
        TranslationStore real = new TranslationStore();
        real.loadAll(base);
        for (String name : List.of("12bobby", "bobby12")) {
            Component hit = LineTranslator.translateLabel(well(name, "30s"), real);
            String got = hit == null ? null : hit.getString();
            check("★ zh_tw：「" + name + "」兩列的力量之井（實際 " + got + "）",
                  got != null && got.contains(name) && !got.contains("Well of Power"));
        }
    }

    private static TranslationStore corpus(Path dir) throws Exception {
        Files.createDirectories(dir);
        JsonObject label = new JsonObject();
        label.addProperty("{u}'s Well of Power", "{u} WELL-OF");
        label.addProperty("{u} keeps {~} hearts", "{~} HEARTS {u}");
        label.addProperty("more Chest Loot from {u} [{~}m]", "MORE LOOT {u} [{~}m]");
        label.addProperty("{u} was wrapped up by a spider.", "{u} WRAPPED");
        label.addProperty("Iron Ore", "");
        Files.writeString(dir.resolve("label.json"), label.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static JsonObject row(String src, String ctx) {
        JsonObject o = new JsonObject();
        o.addProperty("src", src);
        o.addProperty("dst", "");
        o.addProperty("role", "name");
        o.addProperty("domain", "label");
        o.addProperty("ctx", ctx);
        o.addProperty("seen", 3);
        return o;
    }

    private static StyledText well(String name, String timer) {
        MutableComponent all = Component.empty();
        all.append(lit(name, AQUA));
        all.append(lit("'s Well of Power", GREY));
        all.append(lit("\n", GREY));
        all.append(lit("[" + timer + "]", GREY));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static int colourOf(Component c, String text) {
        int[] found = {-1};
        StringBuilder flat = new StringBuilder();
        java.util.List<Integer> colours = new java.util.ArrayList<>();
        c.visit((style, s) -> {
            TextColor colour = style.getColor();
            for (int i = 0; i < s.length(); i++) {
                flat.append(s.charAt(i));
                colours.add(colour == null ? -1 : colour.getValue());
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        int at = flat.indexOf(text);
        if (at >= 0) {
            found[0] = colours.get(at);
        }
        return found[0];
    }
}
