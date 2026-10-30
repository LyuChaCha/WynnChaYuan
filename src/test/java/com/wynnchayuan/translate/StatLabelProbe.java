package com.wynnchayuan.translate;

import java.nio.file.Path;

public final class StatLabelProbe {

    private static final String[] LINES = {
        "{#} Total Damage: {~} (of your DPS, per Wisp)",
        "{#}({#}{#}Damage: {~})",
        "{#}({#}{#}Fire: {~})",
        "{#} Total Heal: {~} (of max health)",
        "{#} Area of Effect: {~} Blocks (Circle-Shaped)",
        "{#} Range: {~} Blocks",
    };

    public static void main(String[] args) {
        for (String lang : new String[] {"zh_tw", "zh_cn"}) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of(
                    "src/main/resources/assets/wynnchayuan/translations", lang));
            System.out.println("=== " + lang + "（" + store.size() + " 條）");
            for (String line : LINES) {
                System.out.println("  " + line);
                System.out.println("      整行查：" + show(store.lookup(line)));
                System.out.println("      走完整條路：" + show(
                        TemplateLookup.lookup(line, store, false)));
            }
        }
    }

    private static String show(String text) {
        return text == null ? "（查不到）" : "「" + text + "」";
    }
}
