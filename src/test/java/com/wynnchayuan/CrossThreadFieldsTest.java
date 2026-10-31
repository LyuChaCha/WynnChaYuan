package com.wynnchayuan;

import java.lang.reflect.Modifier;
import java.util.List;

public final class CrossThreadFieldsTest {

    private static final List<String> CONFIG_FIELDS = List.of(
            "collect",
            "language", "uiLanguage", "fallbackLanguage",
            "syncedTranslations", "syncedTranslationsDate", "autoUpdateTranslations");

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        for (String field : CONFIG_FIELDS) {
            check("ModConfig." + field, ModConfig.class, field);
        }
        check("WynnChaYuan.listener", WynnChaYuan.class, "listener");
        check("WynnChaYuan.language", WynnChaYuan.class, "language");
        System.out.println(failures == 0 ? "CrossThreadFields: 全部通過"
                : "CrossThreadFields: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, Class<?> owner, String field) throws Exception {
        boolean ok = Modifier.isVolatile(owner.getDeclaredField(field).getModifiers());
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
