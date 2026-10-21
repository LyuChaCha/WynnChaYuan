package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 交易市場的攤位招牌：「X's Shop」底下是攤主自己打的一行字。
 *
 * <h2>實機回報（2026-10-21）</h2>
 * 一整排攤位只有一攤翻得出來（「Dükkânı (MilesC1)」，底下那行是「e」），
 * 其餘「Abberdine's Shop / Also try Look Outside!」整面都是英文。
 *
 * <p>逐行翻的那條路規定「有一行查不到就整塊放棄」，而攤主打的字永遠不會進語料；
 * 「e」只有一個字母不算字，那一攤才剛好過關。第一行是「某人的東西」時，
 * 後面查不到的行原樣留著。別的漂浮字照舊整塊放棄。
 */
public final class ShopSignTest {

    private static int failures = 0;

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

        // 領地的旗幟（實機 2026-10-21）：公會名稱夾在句子裡、底下是公會自己的稱號
        MutableComponent flag = Component.empty()
                .append(Component.literal("Detlas\nControlled by "))
                .append(Component.literal("Polish Hussars").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AQUA))))
                .append(Component.literal(" [Lv. 84]\n< Master of the Sands >\nClick for Options"));
        String banner = plain(LineTranslator.translateLabel(StyledText.fromComponent(flag), store));
        check("★領地旗幟翻出來，公會名稱原樣（實際 " + banner + "）",
                "Detlas\nKontrol eden: Polish Hussars [Sv. 84]\n< Master of the Sands >\nSeçenekler için tıkla"
                        .equals(banner));

        // 對照組：不是「某人的東西」的漂浮字，查不到的那一行照舊讓整塊放棄
        Component mob = LineTranslator.translateLabel(StyledText.fromComponent(
                Component.literal("Wild Rabbit\nSomething nobody wrote down")), store);
        check("別的漂浮字照舊整塊放棄（實際 " + plain(mob) + "）", mob == null);

        System.out.println(failures == 0 ? "攤位招牌：全部通過" : "攤位招牌：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 實機的樣子：名字是青色，「's Shop」是白色，招牌另起一行。 */
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

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
