package com.wynnchayuan.translate;

import java.nio.file.Path;

public final class MajorIdTest {

    private static int failures = 0;

    private static final String AS_RENDERED =
            "✦ Altruism: Whenever you gain\n"
            + "health from your Life Steal\n"
            + "and Health Regen IDs, allies\n"
            + "within {~} blocks will receive\n"
            + "the same amount.";

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        check("語料本身讀得到名稱", "利他主義".equals(store.lookup("Altruism")));

        String flowed = LineTranslator.lookupFlowed(AS_RENDERED, store);
        check("整段（含 ✦ 與自動斷行）查得到", flowed != null);
        if (flowed != null) {
            check("名稱有翻到", flowed.contains("利他主義"));
            check("說明有翻到", flowed.contains("格內的友軍"));
            check("佔位符數量守住", numberSlots(flowed) == numberSlots(AS_RENDERED));
        }

        String blinding = LineTranslator.lookupFlowed(BLINDING, store);
        check("帶兩個偏移字元的整段也查得到（實際：" + shorten(blinding) + "）",
                blinding != null);
        if (blinding != null) {
            check("名稱有翻到", blinding.contains("眩目之光"));
        }

        check("不認得的名稱不會硬翻",
                LineTranslator.lookupFlowed(
                        "✦ Nonexistent: Allies within {~} blocks gain {~} of the "
                        + "health you gain from Health Regen and Life Steal.", store) == null);

        System.out.println(failures == 0
                ? "MajorId: 全部通過" : "MajorId: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static final String BLINDING =
            "{#}{#}Blinding Lights: All Ophanim\n"
            + "orbs deal {~} damage, gain\n"
            + "+{~} Crystallize per hit, and\n"
            + "instead orbit around you.\n"
            + "Using Main Attack causes all\n"
            + "orbs to travel faster.";

    private static String shorten(String text) {
        if (text == null) {
            return "null";
        }
        String one = text.replace('\n', ' ');
        return one.length() > 40 ? one.substring(0, 40) + "…" : one;
    }

    private static int numberSlots(String text) {
        return text.split("\\{~\\d?\\}", -1).length - 1;
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
