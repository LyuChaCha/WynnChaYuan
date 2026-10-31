package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ShopSignTest {

    private static final int AQUA = 0x55FFFF;
    private static final int WHITE = 0xFFFFFF;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-shop-sign");
        Files.writeString(dir.resolve("misc.json"), """
                {"{owner}'s {thing}": "{thing} ({owner})", "Shop": "Dükkân", "Wild Rabbit": "Yaban Tavşanı",
                 "Controlled by {u} [Lv. {~}]": "Kontrol eden: {u} [Sv. {~}]",
                 "Click for Options": "Seçenekler için tıkla"}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        String sign = plain(LineTranslator.translateLabel(sign("Abberdine", "Also try Look Outside!"), store));
        check("★攤位名翻出來（實際 " + sign + "）", sign.startsWith("Dükkân (Abberdine)"));
        check("★攤主的招牌原樣留著", sign.endsWith("\nAlso try Look Outside!"));

        String digits = plain(LineTranslator.translateLabel(sign("Bocchi5101", "Please help us recycle these"), store));
        check("名字帶數字也一樣（實際 " + digits + "）",
                digits.startsWith("Dükkân (Bocchi5101)") && digits.endsWith("recycle these"));

        String letter = plain(LineTranslator.translateLabel(sign("MilesC1", "e"), store));
        check("招牌只有一個字母的照舊（實際 " + letter + "）", letter.equals("Dükkân (MilesC1)\ne"));

        MutableComponent flag = Component.empty()
                .append(Component.literal("Detlas\nControlled by "))
                .append(Component.literal("Polish Hussars").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AQUA))))
                .append(Component.literal(" [Lv. 84]\n< Master of the Sands >\nClick for Options"));
        String banner = plain(LineTranslator.translateLabel(StyledText.fromComponent(flag), store));
        check("★領地旗幟翻出來，公會名稱原樣（實際 " + banner + "）",
                "Detlas\nKontrol eden: Polish Hussars [Sv. 84]\n< Master of the Sands >\nSeçenekler için tıkla"
                        .equals(banner));

        Component mob = LineTranslator.translateLabel(StyledText.fromComponent(
                Component.literal("Wild Rabbit\nSomething nobody wrote down")), store);
        check("別的漂浮字照舊整塊放棄（實際 " + plain(mob) + "）", mob == null);

        System.out.println(failures == 0 ? "攤位招牌：全部通過" : "攤位招牌：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static StyledText sign(String owner, String slogan) {
        MutableComponent line = Component.empty()
                .append(Component.literal(owner).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AQUA))))
                .append(Component.literal("'s Shop\n" + slogan)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(WHITE))));
        return StyledText.fromComponent(line);
    }

    private static String plain(Component c) {
        return c == null ? null : c.getString();
    }

}
