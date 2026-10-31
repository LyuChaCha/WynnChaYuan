package com.wynnchayuan.golden;

import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.ToIntFunction;

final class GoldenWidth {

    private GoldenWidth() {}

    static void install() throws ReflectiveOperationException {
        Field field = LineTranslator.class.getDeclaredField("measureForTest");
        field.setAccessible(true);
        field.set(null, (ToIntFunction<Component>) GoldenWidth::of);
    }

    static int of(Component component) {
        int[] width = {0};
        component.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                width[0] += SpaceOffset.decode(text);
                return Optional.empty();
            }
            for (int at = 0; at < text.length(); ) {
                int cp = text.codePointAt(at);
                at += Character.charCount(cp);
                width[0] += advance(cp);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return width[0];
    }

    private static int advance(int cp) {
        if (cp == '\n' || (cp >= 0xE000 && cp <= 0xF8FF)) {
            return 0;
        }
        return cp >= 0x2E80 ? 9 : 6;
    }
}
