package com.wynnchayuan.render;

import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.List;

/**
 * boss bar 開頭的怪物名要跟頭上的名牌一樣翻出來。
 *
 * <p>實機回報：名牌是「支气管体」「凝块」，boss bar 還是
 * 「Bronchial - 113k❤ - Weak Dam Def」。名字與血量在同一個顏色段，
 * 名牌語料的鍵又帶著等級膠囊的 {@code {#}{#}}。
 */
public final class BossBarNameTest {

    private static int failures = 0;

    public static void main(String[] args) {
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(root.resolve("zh_tw"), root.resolve("zh_cn")));

        Style red = Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555));
        Style aqua = Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF));
        MutableComponent bar = Component.empty();
        bar.append(Component.literal("Bronchial - 113k❤ - ").withStyle(red));
        bar.append(Component.literal("Weak").withStyle(aqua));

        Component out = WynntilsText.bossBarName(bar, store);
        String shown = out == null ? "(沒翻)" : out.getString();
        check("★ 名字換成名牌的譯名（實際 " + shown + "）",
              shown.startsWith("支气管体 - 113k❤ - "));
        check("名字後面的血量與狀態照原樣", shown.endsWith("113k❤ - Weak"));
        String[] colour = {null};
        if (out != null) {
            out.visit((style, text) -> {
                if (text.contains("支气管体")) {
                    colour[0] = String.valueOf(style.getColor());
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        check("名字那段保留原本的顏色（實際 " + colour[0] + "）",
              String.valueOf(red.getColor()).equals(colour[0]));

        MutableComponent grume = Component.empty();
        grume.append(Component.literal("Grume - 37.7k❤ - ").withStyle(red));
        Component g = WynntilsText.bossBarName(grume, store);
        check("Grume → 凝块（實際 " + (g == null ? "(沒翻)" : g.getString()) + "）",
              g != null && g.getString().startsWith("凝块 - "));

        // 右邊的屬性克制：Weak／Dam／Def 只在 boss bar 換，前面的圖示與顏色不動
        MutableComponent status = Component.empty();
        status.append(Component.literal("Grume - 37.7k❤ - ").withStyle(red));
        status.append(Component.literal("Weak ").withStyle(aqua));
        status.append(Component.literal("Dam Def").withStyle(red));
        Component w = WynntilsText.bossBarWords(status, store);
        String ws = w == null ? "(沒翻)" : w.getString();
        check("★ 屬性克制換成易伤／增伤／防护（實際 " + ws + "）",
              ws.equals("Grume - 37.7k❤ - 易伤 增伤 防护"));
        check("一般語料不受影響：Def 不會在別處變成防护",
              !"防护".equals(store.lookup("Def")));
        check("查不到的名字回 null",
              WynntilsText.bossBarName(Component.literal("Zzqxv - 5❤"), store) == null);
        check("沒有「 - 」的標題不動",
              WynntilsText.bossBarName(Component.literal("Bronchial"), store) == null);

        System.out.println(failures == 0 ? "boss bar 怪物名：全部通過"
                                         : "boss bar 怪物名：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
