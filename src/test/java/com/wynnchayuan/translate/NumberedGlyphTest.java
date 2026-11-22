package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class NumberedGlyphTest {

    private static final String EARTH = "";
    private static final String WATER = "";

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-numbered-glyph");
        String src = "{#} Earth and {#} Water damage";
        Files.writeString(dir.resolve("misc.json"), "{\n"
                + "  \"" + src + "\": \"{#2} 水與 {#1} 土的傷害\",\n"
                + "  \"{#} Air and {#} Fire\": \"{#} 風與 {#} 火\"\n"
                + "}\n", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Component swapped = LineTranslator.translate(
                line(EARTH, " Earth and ", WATER, " Water damage"), store);
        String shown = swapped == null ? null : swapped.getString();
        check("帶編號的 {#} 照編號填圖示（實際 " + shown + "）",
              (WATER + " 水與 " + EARTH + " 土的傷害").equals(shown));
        check("畫面上沒有 {#", shown != null && !shown.contains("{#"));

        Component plain = LineTranslator.translate(
                line(EARTH, " Air and ", WATER, " Fire"), store);
        String kept = plain == null ? null : plain.getString();
        check("沒編號的照順序填（實際 " + kept + "）", (EARTH + " 風與 " + WATER + " 火").equals(kept));

        System.out.println(failures == 0 ? "\n帶編號的圖示：全部通過"
                : "\n帶編號的圖示：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static StyledText line(String... pieces) {
        MutableComponent all = Component.empty();
        for (String piece : pieces) {
            all.append(Component.literal(piece));
        }
        return StyledText.fromComponent(all);
    }
}
