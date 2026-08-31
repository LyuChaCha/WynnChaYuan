package com.wynnchayuan;

import com.wynnchayuan.render.ThirdPartySections;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 驗證第三方區塊的濾除規則。
 *
 * <p>這條規則會<b>刪掉</b>玩家看得到的行，出錯的代價比一般顯示問題高：
 * 刪錯了畫面上不會有任何跡象，只是東西默默不見。所以連「不該刪的時候
 * 真的沒刪」也一起測。
 *
 * <p>與其他測試一樣寫成 main，沿用 Loom 重映射過的 classpath。
 */
public final class ThirdPartySectionsTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        // 自己寫一份清單再載入，<b>不要靠出貨的預設值</b>。
        //
        // 預設值是政策、會變：當初預設濾掉 NORI 與 WYNNPOOL，2026-08-30 因為
        // 實機證據改成預設不濾。這份測試驗的是「濾的機制對不對」，跟預設濾誰
        // 無關——綁在一起的話，改政策就會弄紅一堆跟政策無關的斷言。
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("wynnchayuan");
        java.nio.file.Files.writeString(dir.resolve("third-party-sections.json"),
                "{\"sections\": [\"NORI\", \"WYNNPOOL\"]}");
        ThirdPartySections.load(dir);

        // --- 正常情形 -------------------------------------------------------
        check("移除兩個相鄰區塊連同前後分隔線",
                strip(List.of(
                        "Epoch",
                        "897 DPS",
                        "",                       // 區塊前的分隔線
                        "NORI",
                        "Ascend Scale [66.6%]",
                        "WYNNPOOL",
                        "Main Scale [74.5%]",
                        "",                       // 區塊結尾
                        "Class Type Archer/Hunter"))
                        .equals(List.of("Epoch", "897 DPS", "Class Type Archer/Hunter")));

        // Wynncraft 的標籤常被排版字元夾住，比對前要先剝掉圖示
        check("標籤前後夾著圖示仍然認得出來",
                strip(List.of("A", "󰀀NORI󰀀", "Ascend Scale", "", "B"))
                        .equals(List.of("A", "B")));

        check("小寫標籤一樣認得",
                strip(List.of("A", "wynnpool", "Main Scale", "", "B"))
                        .equals(List.of("A", "B")));

        // --- 不該動的情形 ---------------------------------------------------
        List<Component> untouched = of(List.of("Epoch", "897 DPS", "", "Class Type"));
        check("沒東西要刪就原樣回傳（不多配一份清單）",
                ThirdPartySections.strip(untouched) == untouched);

        // 「用包含比對」會在這裡出事：這是裝備敘述，不是區塊標題
        check("只是含有標籤字樣的敘述不會被誤刪",
                strip(List.of("Nori's Blessing", "+10 Health", "", "Class Type"))
                        .equals(List.of("Nori's Blessing", "+10 Health", "", "Class Type")));

        // --- 止血上限 -------------------------------------------------------
        List<String> noEnd = new ArrayList<>(List.of("Epoch", "NORI"));
        for (int i = 0; i < 30; i++) {
            noEnd.add("裝備資訊 " + i);        // 全部都有可讀文字，永遠碰不到分隔線
        }
        List<String> capped = strip(noEnd);
        check("區塊沒有結尾分隔線時不會把後面整份吃光（剩 " + capped.size() + " 行）",
                capped.size() > 15 && capped.get(0).equals("Epoch")
                        && !capped.contains("NORI"));

        System.out.println(failures == 0
                ? "ThirdPartySections: 全部通過"
                : "ThirdPartySections: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static List<Component> of(List<String> lines) {
        List<Component> out = new ArrayList<>();
        for (String line : lines) {
            out.add(Component.literal(line));
        }
        return out;
    }

    private static List<String> strip(List<String> lines) {
        return ThirdPartySections.strip(of(lines)).stream()
                .map(Component::getString).toList();
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
