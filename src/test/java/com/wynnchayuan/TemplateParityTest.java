package com.wynnchayuan;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class TemplateParityTest {

    private static int failures = 0;
    private static int checked = 0;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        JsonArray cases;
        try (InputStream in = TemplateParityTest.class
                .getResourceAsStream("/template-fixture.json")) {
            if (in == null) {
                System.out.println("找不到比對樣本，略過");
                return;
            }
            cases = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        }

        for (JsonElement el : cases) {
            JsonObject c = el.getAsJsonObject();
            String input = c.get("in").getAsString();
            String expected = c.get("out").getAsString();

            String actual = LineParts.of(StyledText.fromString(input)).template();
            checked++;
            if (!expected.equals(actual)) {
                failures++;
                if (failures <= 5) {
                    System.out.println("  [FAIL] 模板不一致");
                    System.out.println("      原文    " + input);
                    System.out.println("      Python  " + expected);
                    System.out.println("      Java    " + actual);
                }
            }
        }

        signsStayOutside();

        System.out.printf("  比對 %d 筆，%d 筆不一致%n", checked, failures);
        System.out.println(failures == 0
                ? "\n全部通過"
                : "\n失敗 " + failures + " 項");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void signsStayOutside() {
        expect("Ascension lasts +20% longer.", "Ascension lasts +{~} longer.");
        expect("Awakened requires -25 mana.", "Awakened requires -{~} mana.");
        expect("gains 2 arrows", "gains {~} arrows");
        expect("Worth 1,250.5%", "Worth {~}");
    }

    private static void expect(String input, String want) {
        String got = LineParts.of(StyledText.fromString(input)).template();
        checked++;
        if (!want.equals(got)) {
            failures++;
            System.out.println("  [FAIL] 正負號的規則不一致");
            System.out.println("      原文  " + input);
            System.out.println("      應為  " + want);
            System.out.println("      實際  " + got);
        }
    }
}
