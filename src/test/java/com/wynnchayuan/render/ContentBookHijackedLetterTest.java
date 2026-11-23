package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.ModConfig.ItemNames;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ContentBookHijackedLetterTest {

    private static final Style NAME = Style.EMPTY
            .withFont(new FontDescription.Resource(
                    Identifier.withDefaultNamespace("language/wynncraft")))
            .withColor(TextColor.fromRgb(0xFF8C19));

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wcy-content-book-letter");
        Files.writeString(dir.resolve("discovery-name.json"), """
                {
                  "Water of the Past": "Água do passado"
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"), """
                {
                  "Water of the Past [Secret Discovery]": "Água do passado [Descoberta secreta]"
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("quest-name.json"), """
                {
                  "Emissaries of Light": "Ánh Sáng Sứ Giả"
                }
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setPageNameMode(ItemNames.ON);
        ModConfig config = new ModConfig(
                Files.createTempDirectory("wcy-content-book-cfg").resolve("config.json"));

        WynntilsScreenText.contentScreen = () -> true;
        try {
            for (String name : new String[] {"Water of the Past", "Emissaries of Light"}) {
                String shown = WynntilsScreenText.screenText(
                        StyledText.fromComponent(Component.literal(name).withStyle(NAME)),
                        config, store).getStringWithoutFormatting();
                check("★ 內容書清單「" + name + "」換成譯名（實際 " + shown + "）",
                        !shown.equals(name));
                check("★ 內容書清單「" + name + "」沒有黑幕字 Á（實際 " + shown + "）",
                        shown.indexOf('Á') < 0);
            }
        } finally {
            WynntilsScreenText.contentScreen = () -> false;
        }

        System.out.println(failures == 0
                ? "ContentBookHijackedLetterTest 全部通過" : failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
