package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 漂浮字專用的譯法：解謎地板上的 Back 是「向後」，介面上的 Back 仍是「返回」。
 */
public final class ScopedLabelTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-scoped");
        Files.writeString(dir.resolve("gui.json"), "{\"Back\": \"返回\"}", StandardCharsets.UTF_8);
        Files.createDirectories(dir.resolve("scoped"));
        Files.writeString(dir.resolve("scoped/label.json"),
                "{\"_meta\": {\"scope\": \"label\"}, \"Back\": \"向後\", \"Left\": \"向左\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        check("一般查表：Back 還是返回", "返回".equals(store.lookup("Back")));
        check("scoped 的字不進一般語料", store.lookup("Left") == null);
        check("漂浮字專用：Back 是向後", "向後".equals(store.labelLookup("Back")));
        Component label = LineTranslator.translateLabel(StyledText.fromString("Back"), store);
        check("★ 地板上的 Back 畫成向後", label != null && "向後".equals(label.getString()));
        Component left = LineTranslator.translateLabel(StyledText.fromString("Left"), store);
        check("地板上的 Left 畫成向左", left != null && "向左".equals(left.getString()));

        System.out.println(failures == 0 ? "漂浮字專用譯法：全部通過" : "漂浮字專用譯法：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
