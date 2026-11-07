package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class TowerBarTest {

    private static final String KEY = "[{u}] {p} Tower - ❤ {~} ({~}) - ☠ {~}-{~} ({~}x)";

    private static final String TAG = "QXZW";

    private static final String PLACE = "Detlas";

    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        StyledText bar = bar();
        check("前提：地名認得出來（模板 " + LineParts.of(bar).template() + "）",
              LineParts.of(bar).template().equals("[" + TAG + "] {p} Tower - ❤ {~} ({~}) - ☠ {~}-{~} ({~}x)"));

        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(base.resolve(lang));
            String dst = store.lookup(KEY);
            check(lang + "：語料有公會塔的 boss bar（" + dst + "）",
                  dst != null && !dst.isBlank() && dst.startsWith("[{u}] "));
            LineParts framed = LineTranslator.nameFramed(bar, store);
            check("★ " + lang + "：公會標籤收成 {u}（實際 "
                          + (framed == null ? null : framed.template()) + "）",
                  framed != null && KEY.equals(framed.template()));
            Component shown = LineTranslator.translate(bar, store);
            String got = shown == null ? null : shown.getString();
            check("★ " + lang + "：整條翻了，標籤、地名與數值原樣（實際 " + got + "）",
                  got != null && got.startsWith("[" + TAG + "] ") && got.contains(PLACE)
                          && got.contains("250000") && got.contains("(1.5x)")
                          && got.indexOf(TAG) == got.lastIndexOf(TAG) && !got.contains(" Tower - "));
        }

        System.out.println(failures == 0 ? "公會塔 boss bar：全部通過" : "公會塔 boss bar：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static StyledText bar() {
        MutableComponent all = Component.empty();
        all.append(lit("[" + TAG + "] ", 0x55FFFF));
        all.append(lit(PLACE + " Tower", 0xFFAA00));
        all.append(lit(" - ", 0x555555));
        all.append(lit("❤ 250000 (50%)", 0xFF5555));
        all.append(lit(" - ", 0x555555));
        all.append(lit("☠ 1200-1800 (1.5x)", 0xAAAAAA));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }
}
