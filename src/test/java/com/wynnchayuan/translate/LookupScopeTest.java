package com.wynnchayuan.translate;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LookupScopeTest {

    public static void main(String[] args) throws Exception {
        nesting();
        afterException();
        reuseWithinScope();
        separateThreads();
        System.out.println(failures == 0 ? "LookupScope: 全部通過" : "LookupScope: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void nesting() {
        try (LookupScope outer = LookupScope.barItemNames(true)) {
            check("外層進去後是開的", TranslationStore.itemNamesBarred());
            try (LookupScope inner = LookupScope.barItemNames(false)) {
                check("內層改成關", !TranslationStore.itemNamesBarred());
            }
            check("內層結束回到外層的值", TranslationStore.itemNamesBarred());
        }
        check("外層結束回到預設", !TranslationStore.itemNamesBarred());
    }

    private static void afterException() {
        try (LookupScope scope = LookupScope.nameLine(true)) {
            throw new IllegalStateException();
        } catch (IllegalStateException expected) {
            check("丟例外也會還原", !TranslationStore.NAME_LINE.get());
        }
    }

    private static void reuseWithinScope() {
        try (LookupScope scope = LookupScope.holdAppendedOriginal(false)) {
            scope.set(true);
            check("逐行切換有效", TranslationStore.HOLD_APPENDED.get());
        }
        check("逐行切換之後照樣還原", !TranslationStore.HOLD_APPENDED.get());
        check("進出不配置物件", LookupScope.lootrunTitle(false) == LookupScope.lootrunTitle(false));
    }

    private static void separateThreads() throws Exception {
        try (LookupScope scope = LookupScope.barItemNames(true)) {
            boolean[] seen = new boolean[1];
            Thread other = new Thread(() -> seen[0] = TranslationStore.itemNamesBarred());
            other.start();
            other.join();
            check("旗標不跨執行緒", !seen[0]);
        }
    }

}
