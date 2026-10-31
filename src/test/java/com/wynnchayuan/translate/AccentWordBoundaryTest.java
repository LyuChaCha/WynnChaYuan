package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class AccentWordBoundaryTest {

    private static final int BODY = 0xAAAAAA;
    private static final int VALUE = 0xFFFFFF;
    private static final int STAT = 0xFFAA00;

    public static void main(String[] args) throws Exception {
        FlowedDebug.init(Files.createTempDirectory("wynnchayuan"));

        lootrunNotSplit();
        cjkAccentStillPastes();
        wholeEnglishWordStillPastes();

        System.out.println(failures == 0
                ? "重點段詞界：全部通過" : "重點段詞界：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void lootrunNotSplit() throws Exception {
        String key = "Lootrun, gain +{~} Loot";
        TranslationStore store = corpus(key, "這趟 Lootrun 獲得 +{~} 寶物加成");

        StyledText styled = line("Lootrun, gain ", BODY, "+20%", VALUE,
                                 " ", BODY, "Loot", STAT);
        check("★ 模板就是語料的鍵（實際："
               + com.wynnchayuan.capture.LineParts.of(styled).template() + "）",
               key.equals(com.wynnchayuan.capture.LineParts.of(styled).template()));

        Component built = LineTranslator.translate(styled, store);
        check("翻得出來", built != null);
        if (built == null) {
            return;
        }
        List<String> parts = pieces(built);
        System.out.println("  畫出去的分段：" + parts);

        boolean whole = false;
        for (String p : parts) {
            if (p.contains("Lootrun")) {
                whole = true;
            }
        }
        check("★ Lootrun 七個字母在同一段裡，沒有被切成 Loot + run（分段："
               + parts + "）", whole);
        check("整句畫出來還是完整的（實際：" + built.getString() + "）",
               built.getString().contains("Lootrun"));
    }

    private static void cjkAccentStillPastes() throws Exception {
        TranslationStore store = corpus("Gain +{~} Loot now", "現在獲得 +{~} 寶物加成");
        StyledText styled = line("Gain ", BODY, "+20%", VALUE,
                                 " Loot now", STAT);
        Component built = LineTranslator.translate(styled, store);
        check("［中文重點段］翻得出來", built != null);
        if (built == null) {
            return;
        }
        System.out.println("  畫出去的分段：" + pieces(built));
        check("★［中文重點段］「寶物加成」還是拿得到橘色（實際："
               + show(colourOf(built, "寶物加成")) + "）",
               Integer.valueOf(STAT).equals(colourOf(built, "寶物加成")));
    }

    private static void wholeEnglishWordStillPastes() throws Exception {
        TranslationStore store = corpus("Reduce the cost of Bash", "降低 Bash 的消耗");
        StyledText styled = line("Reduce the cost of ", BODY, "Bash", STAT);
        Component built = LineTranslator.translate(styled, store);
        check("［獨立英文詞］翻得出來", built != null);
        if (built == null) {
            return;
        }
        System.out.println("  畫出去的分段：" + pieces(built));
        check("★［獨立英文詞］Bash 還是拿得到橘色（實際："
               + show(colourOf(built, "Bash")) + "）",
               Integer.valueOf(STAT).equals(colourOf(built, "Bash")));
    }

    private static StyledText line(Object... parts) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.length; i += 2) {
            out.append(Component.literal((String) parts[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) parts[i + 1]))));
        }
        return StyledText.fromComponent(out);
    }

    private static TranslationStore corpus(String key, String zh) throws Exception {
        Path dir = Files.createTempDirectory("accent-word");
        Files.writeString(dir.resolve("misc.json"),
                "{\"" + key + "\": \"" + zh + "\"}");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static List<String> pieces(Component built) {
        List<String> out = new ArrayList<>();
        built.visit((style, s) -> {
            out.add(s);
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static Integer colourOf(Component built, String needle) {
        List<Integer> found = new ArrayList<>();
        built.visit((style, s) -> {
            if (s.contains(needle) && style.getColor() != null) {
                found.add(style.getColor().getValue() & 0xFFFFFF);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found.isEmpty() ? null : found.get(0);
    }

    private static String show(Integer rgb) {
        return rgb == null ? "沒拿到（多半是被拆成兩段）" : String.format("#%06X", rgb);
    }

}
