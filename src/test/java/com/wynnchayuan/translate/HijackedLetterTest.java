package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import com.wynntils.core.text.StyledText;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class HijackedLetterTest {

    private static TranslationStore STORE;

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
        STORE = store;

        chatLine(store);
        corpusKeepsAccent(dir);
        originalKept(store);
        contentBookNames(store);

        System.out.println(failures == 0
                ? "HijackedLetterTest 全部通過" : failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void chatLine(TranslationStore store) {
        String shown = shown("Area Discovered", CHAT, store);
        check("聊天那一行換掉了（實際 " + shown + "）",
              shown.startsWith("Area descubierta"));
        check("整行一個 Á 都不剩", shown.indexOf('Á') < 0);
    }

    private static void corpusKeepsAccent(Path dir) throws Exception {
        String raw = Files.readString(dir.resolve("discovery.json"),
                                      StandardCharsets.UTF_8);
        check("語料檔本身留著重音", raw.contains("ÁBRETE"));
        check("查出來的已經是安全的（實際 " + STORE.lookup("Open the door") + "）",
              "ABRETE".equals(STORE.lookup("Open the door")));
    }

    private static void originalKept(TranslationStore store) {
        Component out = LineTranslator.translate(
                StyledText.fromComponent(Component.literal("Á").withStyle(CHAT)),
                store);
        check("查不到譯文就不碰（實際 " + (out == null ? "沒翻" : out.getString()) + "）",
              out == null || "Á".equals(out.getString()));
    }

    private static void contentBookNames(TranslationStore store) {
        String card = store.pageTitleName("Water of the Past");
        check("卡片標題拿到的名字也換掉了（實際 " + card + "）", "Agua do passado".equals(card));
        String quest = store.pageTitleName("Emissaries of Light");
        check("任務名稱表拿到的名字也換掉了（實際 " + quest + "）", "Anh Sáng Sứ Giả".equals(quest));
    }

    private static String shown(String text, Style style, TranslationStore store) {
        Component out = LineTranslator.translate(
                StyledText.fromComponent(Component.literal(text).withStyle(style)),
                store);
        return out == null ? "" : out.getString();
    }

}
