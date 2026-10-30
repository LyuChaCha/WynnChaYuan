package com.wynnchayuan.translate;

public final class LookupScope implements AutoCloseable {

    private static final Flag NAME_LINE = new Flag(TranslationStore.NAME_LINE);
    private static final Flag LOOTRUN_TITLE = new Flag(TranslationStore.LOOTRUN_TITLE);
    private static final Flag HOLD_APPENDED = new Flag(TranslationStore.HOLD_APPENDED);
    private static final Flag BAR_ITEMS = new Flag(TranslationStore.BAR_ITEMS);

    private final ThreadLocal<Boolean> flag;
    private final boolean restored;

    private LookupScope(ThreadLocal<Boolean> flag, boolean restored) {
        this.flag = flag;
        this.restored = restored;
    }

    public static LookupScope nameLine(boolean on) {
        return NAME_LINE.enter(on);
    }

    public static LookupScope lootrunTitle(boolean on) {
        return LOOTRUN_TITLE.enter(on);
    }

    public static LookupScope holdAppendedOriginal(boolean on) {
        return HOLD_APPENDED.enter(on);
    }

    public static LookupScope barItemNames(boolean on) {
        return BAR_ITEMS.enter(on);
    }

    public void set(boolean on) {
        flag.set(on);
    }

    @Override
    public void close() {
        flag.set(restored);
    }

    // 查表在算繪路徑上：兩個還原值各留一個實例，進出不配置物件
    private static final class Flag {
        private final ThreadLocal<Boolean> value;
        private final LookupScope backToOff;
        private final LookupScope backToOn;

        Flag(ThreadLocal<Boolean> value) {
            this.value = value;
            this.backToOff = new LookupScope(value, false);
            this.backToOn = new LookupScope(value, true);
        }

        LookupScope enter(boolean on) {
            LookupScope scope = value.get() ? backToOn : backToOff;
            value.set(on);
            return scope;
        }
    }
}
