package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DiscoveryNameTest {

    private static final Path FILE = Path.of(
            "src/main/resources/assets/wynnchayuan/translations",
            Languages.DEFAULT, "discovery-name.json");

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        JsonObject root;
        try (BufferedReader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        }

        int checked = 0;
        int viaTemplate = 0;
        int titles = 0;
        for (String name : root.keySet()) {
            if (name.equals("_meta") || name.indexOf('{') >= 0) {
                continue;
            }
            if (name.endsWith("]") && name.contains(" [")) {
                titles++;
                continue;
            }
            checked++;
            String template = com.wynnchayuan.capture.LineParts
                    .of(StyledText.fromComponent(Component.literal(name))).template();
            if (!template.equals(name)) {
                viaTemplate++;
            }
            String zh = TemplateLookup.lookup(template, store, false);
            check("「" + name + "」查得到（模板 " + template + " → " + zh + "）",
                   zh != null && !zh.isBlank());
        }

        check("★ 檢查了 124 個名稱（實際 " + checked + "）", checked == 124);
        check("★ 其中有 17 個是靠地名／數字模板命中的（實際 " + viaTemplate + "）",
               viaTemplate == 17);

        check("★ 也收了「名稱 [類型]」的整行條目（實際 " + titles + " 條）",
               titles >= 100);

        for (String[] pair : new String[][] {
                {"Ragni's Secret Library", "祕密圖書館"}, {"Historical Maltic", "往昔"},
                {"Bak'al's Destruction 2", "毀滅"}, {"The Twains' Downfall", "Twain"},
                {"Wynn Plains Monument", "紀念碑"}, {"Aldwell Library", "圖書館"}}) {
            String template = com.wynnchayuan.capture.LineParts
                    .of(StyledText.fromComponent(Component.literal(pair[0]))).template();
            String zh = TemplateLookup.lookup(template, store, false);
            check("「" + pair[0] + "」譯文含「" + pair[1] + "」（實際 " + zh + "）",
                   zh != null && zh.contains(pair[1]));
        }

        descriptions(store);

        System.out.println(failures == 0
                ? "探索點：全部通過" : "探索點：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void descriptions(TranslationStore store) throws Exception {
        Path file = Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT, "discovery.json");
        JsonObject root;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        }
        int dead = 0;
        for (String key : root.keySet()) {
            if (key.startsWith("_") || key.indexOf('{') >= 0) {
                continue;
            }
            String template = com.wynnchayuan.capture.LineParts
                    .of(StyledText.fromComponent(Component.literal(key))).template();
            if (TemplateLookup.lookup(template, store, false) == null) {
                dead++;
                check("敘述查不到：" + template.substring(0, Math.min(90, template.length())),
                       false);
            }
        }
        check("★ 沒有查不到的敘述（實際 " + dead + " 條）", dead == 0);
    }

}
