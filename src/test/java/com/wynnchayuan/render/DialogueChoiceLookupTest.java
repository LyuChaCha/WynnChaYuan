package com.wynnchayuan.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DialogueChoiceLookupTest {

    private static final String ROOT = "src/main/resources/assets/wynnchayuan/translations";

    private static final List<String> LANGUAGES = List.of(
            "zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr");

    private static final List<String> SONGS = List.of(
            "Status", "Bullfrog's Bellow", "Whisper", "Day In, Day Out");

    private static final Style CHOICE = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.withDefaultNamespace("hud/dialogue/text/wynncraft/choice_1")));

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-choice");
        var field = WynnChaYuan.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, new ModConfig(dir.resolve("config.json")));

        otherCategories(dir);
        for (String lang : LANGUAGES) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of(ROOT, lang));
            store.setNameMode(ModConfig.ItemNames.BOTH);
            for (String song : SONGS) {
                check(lang + "：★ 就地取代：「" + song + "」照原文", replaced(song, store) == null);
                check(lang + "：★ 小框：「" + song + "」照原文", panel(song, store).equals(song));
            }
            dialogueLines(lang, store);
        }

        System.out.println(failures == 0 ? "對話選項查表：全部通過"
                : "對話選項查表：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void otherCategories(Path dir) throws Exception {
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"itemNames": true, "gearNames": true},
                 "entries": {"a": {"src": "Sharp Song", "dst": "銳歌", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("ui-labels.json"), """
                {"Back": "返回", "Long enough label here": "夠長的標籤"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("dialogue-choice.json"), """
                {"entries": {"a": {"src": "Hello there, my friend.", "dst": "你好。", "kind": "dialogue"},
                             "b": {"src": "Back again", "dst": "又回來了", "kind": "objective"}}}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("quest.json"), """
                {"Yes!": "好！"}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(ModConfig.ItemNames.BOTH);

        check("裝備名不算對話", store.lookupChoice("Sharp Song") == null);
        check("介面字不算對話", store.lookupChoice("Back") == null);
        check("任務目標不算對話", store.lookupChoice("Back again") == null);
        check("整列比對不靠鬆比對撞介面字", store.lookupChoice("long enough label here!") == null);
        check("對話選項查得到", store.lookupChoice("Hello there, my friend.") != null);
        check("前後空白不影響", store.lookupChoice(" Hello there, my friend. ") != null);
        check("標點差一點照樣查得到", store.lookupChoice("hello there, my friend") != null);
        check("還沒歸任務的台詞算對話", store.lookupChoice("Yes!") != null);
        check("就地取代翻得出對話選項", replaced("Hello there, my friend.", store) != null);
        check("小框翻得出對話選項", !panel("Hello there, my friend.", store).equals("Hello there, my friend."));
        check("裝備名就地取代不換", replaced("Sharp Song", store) == null);
        check("裝備名小框不換", panel("Sharp Song", store).equals("Sharp Song"));
    }

    private static void dialogueLines(String lang, TranslationStore store) throws Exception {
        int total = 0;
        int lost = 0;
        for (String src : translated(Path.of(ROOT, lang, "dialogue-choice.json"))) {
            total++;
            String line = store.lookupChoice(src);
            if (line == null || !line.equals(store.lookupExact(src))) {
                lost++;
                System.out.println("    查不到：「" + src + "」");
            }
        }
        check(lang + "：選項語料照舊查得到（" + total + " 條，掉 " + lost + "）",
                total > 0 && lost == 0);
    }

    private static List<String> translated(Path file) throws Exception {
        List<String> out = new ArrayList<>();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject entries = JsonParser.parseReader(in).getAsJsonObject()
                    .getAsJsonObject("entries");
            for (var entry : entries.entrySet()) {
                JsonElement dst = entry.getValue().getAsJsonObject().get("dst");
                if (dst != null && !dst.getAsString().isBlank()) {
                    out.add(entry.getValue().getAsJsonObject().get("src").getAsString());
                }
            }
        }
        return out;
    }

    private static Component replaced(String row, TranslationStore store) {
        Marquee.clear();
        MutableComponent message = Component.empty();
        message.append(Component.literal(offset(42)).withStyle(CHOICE));
        message.append(Component.literal(row).withStyle(CHOICE));
        message.append(Component.literal(offset(-73)).withStyle(CHOICE));
        return DialogueRewriter.rewrite(message, store);
    }

    private static String panel(String row, TranslationStore store) {
        DialogueOverlay.rawChoices = List.of(row);
        StringBuilder text = new StringBuilder();
        for (List<Component> option : DialogueOverlay.translateChoices(store)) {
            for (Component line : option) {
                text.append(line.getString());
            }
        }
        DialogueOverlay.rawChoices = List.of();
        return text.toString();
    }

    private static String offset(int px) {
        return new String(Character.toChars(0xD0000 + px));
    }
}
