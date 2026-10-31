package com.wynnchayuan;

import com.wynnchayuan.render.ThirdPartySections;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ThirdPartySectionsTest {

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("wynnchayuan");
        java.nio.file.Files.writeString(dir.resolve("third-party-sections.json"),
                "{\"sections\": [\"NORI\", \"WYNNPOOL\"]}");
        ThirdPartySections.load(dir);

        check("移除兩個相鄰區塊連同前後分隔線",
                strip(List.of(
                        "Epoch",
                        "897 DPS",
                        "",
                        "NORI",
                        "Ascend Scale [66.6%]",
                        "WYNNPOOL",
                        "Main Scale [74.5%]",
                        "",
                        "Class Type Archer/Hunter"))
                        .equals(List.of("Epoch", "897 DPS", "Class Type Archer/Hunter")));

        check("標籤前後夾著圖示仍然認得出來",
                strip(List.of("A", "󰀀NORI󰀀", "Ascend Scale", "", "B"))
                        .equals(List.of("A", "B")));

        check("小寫標籤一樣認得",
                strip(List.of("A", "wynnpool", "Main Scale", "", "B"))
                        .equals(List.of("A", "B")));

        List<Component> untouched = of(List.of("Epoch", "897 DPS", "", "Class Type"));
        check("沒東西要刪就原樣回傳（不多配一份清單）",
                ThirdPartySections.strip(untouched) == untouched);

        check("只是含有標籤字樣的敘述不會被誤刪",
                strip(List.of("Nori's Blessing", "+10 Health", "", "Class Type"))
                        .equals(List.of("Nori's Blessing", "+10 Health", "", "Class Type")));

        List<String> noEnd = new ArrayList<>(List.of("Epoch", "NORI"));
        for (int i = 0; i < 30; i++) {
            noEnd.add("裝備資訊 " + i);
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

}
