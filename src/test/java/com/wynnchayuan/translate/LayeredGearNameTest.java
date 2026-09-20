package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * 簡中是「繁中墊底、簡中在上」兩層一起載入的。裝備名稱在這條路上要照樣翻。
 *
 * <p>實機回報：0.2.2 測試版切到簡中，物品名稱全部沒翻；繁中正常。
 */
public final class LayeredGearNameTest {

    private static int failures = 0;

    public static void main(String[] args) {
        // 可以指定別的根目錄，例如遊戲裡的快取 config/wynnchayuan/translations
        Path root = Path.of(args.length > 0 ? args[0]
                                            : "src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(root.resolve("zh_tw"), root.resolve("zh_cn")));

        store.setNameMode(CollectorConfig.ItemNames.ON);
        check("開：簡中查得到裝備名（實際 " + store.lookup("Abhorrence") + "）",
              "憎恶之矛".equals(store.lookup("Abhorrence")));
        List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(List.of(
                        net.minecraft.network.chat.Component.literal("Abhorrence"),
                        net.minecraft.network.chat.Component.literal("Abhorrence")), store);
        String shown = out.isEmpty() ? "(沒翻)" : out.get(out.size() - 1).getString();
        check("★ 開：tooltip 名稱那一行畫成簡中（實際 " + shown + "）", "憎恶之矛".equals(shown));

        // ★ 繁中那層的 Mythic 是空的（刻意不翻）；簡中那層的譯名不能被它擋回英文
        check("簡中的 Mythic 不算「還沒翻的裝備名」", !store.isBareGearName("Sunstar"));
        List<net.minecraft.network.chat.Component> myth =
                com.wynnchayuan.render.TooltipPanel.translateLines(List.of(
                        net.minecraft.network.chat.Component.literal("Sunstar"),
                        net.minecraft.network.chat.Component.literal("Sunstar")), store);
        String mythShown = myth.isEmpty() ? "(沒翻)" : myth.get(myth.size() - 1).getString();
        check("★ 簡中 Mythic 的名稱行畫成中文（實際 " + mythShown + "）",
              "太阳与星星圣器".equals(mythShown));

        store.setNameMode(CollectorConfig.ItemNames.BOTH);
        check("譯名加原文（實際 " + store.lookup("Abhorrence") + "）",
              "憎恶之矛 (Abhorrence)".equals(store.lookup("Abhorrence")));

        store.setNameMode(CollectorConfig.ItemNames.OFF);
        check("關：留原文（實際 " + store.lookup("Abhorrence") + "）",
              store.lookup("Abhorrence") == null);

        System.out.println(failures == 0 ? "簡中疊層的裝備名：全部通過"
                                         : "簡中疊層的裝備名：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
