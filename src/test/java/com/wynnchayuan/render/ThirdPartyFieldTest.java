package com.wynnchayuan.render;

import com.wynnchayuan.ThirdPartyField;
import com.wynnchayuan.translate.WmsBridge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ThirdPartyFieldTest {

    private static int failures = 0;

    private static class Base {
        protected int x = 7;
    }

    private static final class Widget extends Base {
        private String label = "hello";
        private final String fixed = "same";
    }

    private static final class Overlay {
        private Object itemText = "not styled text";
    }

    private static final class RenamedItem {
        private final String title = "Bony Bow";
    }

    public static void main(String[] args) throws Exception {
        Widget widget = new Widget();

        ThirdPartyField label = new ThirdPartyField("label");
        check("讀得到私有欄位", "hello".equals(label.get(widget)));
        check("寫得進私有欄位", label.set(widget, "bye") && "bye".equals(label.get(widget)));

        ThirdPartyField inherited = new ThirdPartyField("x");
        check("讀得到父類別的欄位", inherited.getInt(widget, -1) == 7);

        ThirdPartyField gone = new ThirdPartyField("itemText");
        check("欄位不見時回 null，不丟例外", gone.get(widget) == null);
        check("欄位不見時寫入回 false", !gone.set(widget, "x"));
        check("欄位不見時數值用預設", gone.getInt(widget, 42) == 42);
        check("欄位不見時回報不可用", !gone.available(widget));

        check("型別不對時回預設，不丟例外", label.getInt(widget, 9) == 9);
        check("寫不進 final 字串欄位時不丟例外", failsQuietly(new ThirdPartyField("fixed"), widget));

        ThirdPartyField shared = new ThirdPartyField("x");
        check("換一個型別會重新找欄位",
                shared.getInt(widget, -1) == 7 && shared.getInt(new Overlay(), -1) == -1);

        check("owner 是 null 時不丟例外", label.get(null) == null && !label.set(null, "x"));

        WynntilsText.translateHeldItem(new Widget());
        Overlay overlay = new Overlay();
        WynntilsText.translateHeldItem(overlay);
        check("Wynntils 欄位型別變了就不動它", "not styled text".equals(overlay.itemText));

        check("WynnMarketSearch 的 name 不見時搜尋照常不命中",
                !WmsBridge.matchesItem(new RenamedItem(), "骨弓"));
        WmsRows.draw(null, new Widget());
        check("WynnMarketSearch 列的欄位不見時不丟例外", true);

        List<String> shadowed = shadowUsers();
        check("mixin 不再用 @Shadow 碰第三方模組（實際 " + shadowed + "）", shadowed.isEmpty());

        System.out.println(failures == 0 ? "ThirdPartyField: 全部通過"
                : "ThirdPartyField: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static boolean failsQuietly(ThirdPartyField field, Object owner) {
        field.set(owner, "other");
        return true;
    }

    private static List<String> shadowUsers() throws Exception {
        try (Stream<Path> files = Files.list(Path.of("src/main/java/com/wynnchayuan/mixin"))) {
            return files.filter(ThirdPartyFieldTest::usesShadow)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static boolean usesShadow(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).contains("@Shadow");
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
