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

/**
 * 驗證 Python 語料工具與 mod 端算出<b>一模一樣的模板</b>。
 *
 * <p>為什麼需要這個測試：翻譯查表是用模板當鍵。收集／語料端（Python）與
 * 顯示端（Java）各有一份參數化實作，只要規則有一點點差異，遊戲裡就會
 * <b>靜默查不到</b>——譯者明明翻了卻不生效，畫面上完全看不出原因。
 *
 * <p>比對樣本由 {@code corpus/tools/build.py} 的實際輸出產生
 * （{@code src/test/resources/template-fixture.json}），涵蓋含地名、含數字
 * 與純文字三種情況。語料工具改動規則卻忘了同步 mod，這裡就會紅。
 *
 * <h2>比對範圍僅限文字層</h2>
 * 樣本刻意<b>排除含 {@code {#}} 的條目</b>。符號佔位符來自字型中繼資料
 * （{@code font} 屬性），而這裡是拿純字串餵 {@code StyledText.fromString}，
 * 無論如何都重現不出那個結構——留著只會產生假失敗。
 *
 * <p>符號那一半由 {@code TranslateTest} 用實際的 {@code Component} 結構驗證，
 * 兩個測試合起來才覆蓋完整。
 */
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
                if (failures <= 5) {          // 只印前幾筆，避免洗版
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

    /**
     * 正負號<b>不算數字的一部分</b>。
     *
     * <h2>為什麼值得單獨釘住</h2>
     * 從 CDN 產語料的那幾支工具很自然會寫成 {@code [+-]?\d+}——把號一起吃掉。
     * 那樣算出來的鍵是 {@code {~}}，而遊戲送過來的是 {@code +{~}}，
     * 兩邊永遠對不上，<b>而且完全不會報錯</b>：畫面上只是「這條沒翻」。
     *
     * <p>意象的技能說明就是這樣掉的——175 段裡有 156 段從頭到尾沒顯示過，
     * 直到有人截圖問「aspect 怎麼還是英文」才發現。
     */
    private static void signsStayOutside() {
        expect("Ascension lasts +20% longer.", "Ascension lasts +{~} longer.");
        expect("Awakened requires -25 mana.", "Awakened requires -{~} mana.");
        expect("gains 2 arrows", "gains {~} arrows");
        // 千分位與小數照樣算同一個數字
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
