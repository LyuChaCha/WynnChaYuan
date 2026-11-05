package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;

import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MountStatRowTest {

    private static final String[] STATS = {
        "Speed", "Acceleration", "Altitude", "Energy", "Handling", "Toughness", "Boost", "Training"
    };

    private static final String[] LANGS = {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"};

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        LineTranslator.measureForTest = c -> c.getString().length() * 6;
        for (String lang : LANGS) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
            store.setAbilityNamesWithOriginal(true);
            for (String stat : STATS) {
                for (boolean max : new boolean[] {false, true}) {
                    Component out = LineTranslator.translate(row(stat, 52, 60, max), store, false);
                    String text = out == null ? "" : out.getString();
                    check(lang + " 坐騎屬性 " + stat + (max ? "（帶上限）" : "") + " 有翻（" + text + "）",
                            !text.isEmpty() && !text.startsWith(stat));
                    check(lang + " 坐騎屬性 " + stat + " 不當成技能名附原文（" + text + "）",
                            !text.contains("(" + stat + ")"));
                    check(lang + " 坐騎屬性 " + stat + " 數值留著", text.contains("52/60"));
                }
            }
        }

        TranslationStore zhTw = new TranslationStore();
        zhTw.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", "zh_tw"));
        zhTw.setAbilityNamesWithOriginal(true);
        Component accel = LineTranslator.translate(row("Acceleration", 60, 60, false), zhTw, false);
        check("zh_tw 坐騎的 Acceleration 是「加速」，不是戰士技能的「動量」",
                accel != null && accel.getString().startsWith("加速"));
        String ability = zhTw.lookup("Acceleration");
        check("戰士技能 Acceleration 照舊附原文（" + ability + "）",
                ability != null && ability.endsWith("(Acceleration)"));

        System.out.println(failures == 0 ? "MountStatRow: 全部通過" : "MountStatRow: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static StyledText row(String stat, int current, int cap, boolean max) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(stat).withStyle(Style.EMPTY.withColor(0xFFFFFF)));
        line.append(Component.literal(SpaceOffset.encode(120 - stat.length() * 6))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY)));
        line.append(Component.literal(String.valueOf(current))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xACFAC6))));
        line.append(Component.literal("/" + cap).withStyle(Style.EMPTY.withColor(0xAAAAAA)));
        if (max) {
            line.append(Component.literal(" (" + (cap + 5) + ")").withStyle(Style.EMPTY.withColor(0x555555)));
        }
        line.append(Component.literal(" ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xACFAC6))));
        line.append(Component.literal("").withStyle(Style.EMPTY.withColor(0x555555)));
        return StyledText.fromComponent(line);
    }
}
