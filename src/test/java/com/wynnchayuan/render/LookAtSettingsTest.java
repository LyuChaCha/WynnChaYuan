package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LookAtSettingsTest {

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        LookAtTranslator.dropStale(store);

        set("lastShown", Component.literal("舊譯文"));
        LookAtTranslator.dropStale(store);
        check("設定沒變，小框留著", get("lastShown") != null);

        store.setNameMode(ModConfig.ItemNames.BOTH);
        LookAtTranslator.dropStale(store);
        check("★ 換物品名稱設定，小框不再畫舊的譯文", get("lastShown") == null);

        set("lastShown", Component.literal("舊譯文"));
        store.loadAll(List.of());
        LookAtTranslator.dropStale(store);
        check("★ 語料重載，小框不再畫舊的譯文", get("lastShown") == null);

        System.out.println(failures == 0 ? "LookAtSettings: 全部通過"
                : "LookAtSettings: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void set(String field, Object value) throws Exception {
        var f = LookAtTranslator.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static Object get(String field) throws Exception {
        var f = LookAtTranslator.class.getDeclaredField(field);
        f.setAccessible(true);
        return f.get(null);
    }
}
