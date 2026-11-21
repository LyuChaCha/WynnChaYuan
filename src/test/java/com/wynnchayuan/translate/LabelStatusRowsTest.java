package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LabelStatusRowsTest {

    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", "zh_tw"));

        String mushroom = shown(store, "\nKill 1 Energy Mushroom\nto create a way out", "\n[0/1]");
        check("末尾的 [0/1] 不擋翻譯（實際 " + mushroom + "）",
              mushroom.contains("擊殺 1 個 Energy Mushroom") && mushroom.contains("以開出一條出路"));
        check("[0/1] 原樣留在最後一行", mushroom.endsWith("\n[0/1]"));

        String panel = shown(store, "\n \nKill 1 Royal Mechanic\nto access the\ncontrol panel", "\n[0/1]");
        check("同類的懸浮字也換得到（實際 " + panel + "）",
              panel.contains("皇家技師") && panel.endsWith("\n[0/1]"));

        String plain = shown(store, "\nKill 1 Energy Mushroom\nto create a way out", "");
        check("沒有進度列時照舊翻", plain.contains("以開出一條出路"));

        String miss = shown(store, "\nQwertyuiop Zxcv\nasdfgh jkl", "\n[0/1]");
        check("查不到的仍然原樣", miss.contains("Qwertyuiop"));

        System.out.println(failures == 0 ? "\n懸浮字的進度列：全部通過"
                : "\n懸浮字的進度列：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static String shown(TranslationStore store, String body, String status) {
        Style banner = Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.parse("minecraft:banner/pill")));
        MutableComponent c = Component.empty()
                .append(Component.literal("").withStyle(banner))
                .append(Component.literal("").withStyle(banner.withColor(0xFF5555)))
                .append(Component.literal(body));
        if (!status.isEmpty()) {
            c.append(Component.literal(status).withStyle(Style.EMPTY.withColor(0x55FF55)));
        }
        Component out = LineTranslator.translateLabel(StyledText.fromComponent(c), store);
        return out == null ? StyledText.fromComponent(c).getString() : out.getString();
    }
}
