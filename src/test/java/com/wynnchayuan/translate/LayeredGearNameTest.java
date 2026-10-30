package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.file.Path;
import java.util.List;

public final class LayeredGearNameTest {

    private static int failures = 0;

    public static void main(String[] args) {
        Path root = Path.of(args.length > 0 ? args[0]
                                            : "src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(root.resolve("zh_cn"), root.resolve("zh_tw")));

        TranslationStore cnOnly = new TranslationStore();
        cnOnly.loadAll(List.of(root.resolve("zh_tw")));
        cnOnly.setNameMode(CollectorConfig.ItemNames.ON);
        String want = cnOnly.lookup("Abhorrence");

        store.setNameMode(CollectorConfig.ItemNames.ON);
        check("上層本來就有 Abhorrence 的譯名（實際 " + want + "）",
              want != null && !want.equals("Abhorrence"));
        check("開：查得到上層的裝備名（實際 " + store.lookup("Abhorrence") + "）",
              want != null && want.equals(store.lookup("Abhorrence")));
        List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(List.of(
                        net.minecraft.network.chat.Component.literal("Abhorrence"),
                        net.minecraft.network.chat.Component.literal("Abhorrence"),
                        banner()), store);
        String shown = out.isEmpty() ? "(沒翻)" : out.get(1).getString();
        check("★ 開：tooltip 名稱那一行畫成上層的譯名（實際 " + shown + "）",
              want != null && want.equals(shown));

        check("上層的 Mythic 不算「還沒翻的裝備名」", !store.isBareGearName("Sunstar"));
        List<net.minecraft.network.chat.Component> myth =
                com.wynnchayuan.render.TooltipPanel.translateLines(List.of(
                        net.minecraft.network.chat.Component.literal("Sunstar"),
                        net.minecraft.network.chat.Component.literal("Sunstar"),
                        banner()), store);
        String mythShown = myth.isEmpty() ? "(沒翻)" : myth.get(1).getString();
        String mythName = store.lookup("Sunstar");
        check("上層的 Mythic 有譯名（實際 " + mythName + "）",
              mythName != null && !mythName.equals("Sunstar")
                      && mythName.codePoints().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF));
        check("★ 上層 Mythic 的名稱行畫成中文（實際 " + mythShown + "）",
              mythName != null && mythName.equals(mythShown));

        store.setNameMode(CollectorConfig.ItemNames.BOTH);
        check("譯名加原文（實際 " + store.lookup("Abhorrence") + "）",
              want != null && (want + " (Abhorrence)").equals(store.lookup("Abhorrence")));

        store.setNameMode(CollectorConfig.ItemNames.OFF);
        check("關：留原文（實際 " + store.lookup("Abhorrence") + "）",
              store.lookup("Abhorrence") == null);

        System.out.println(failures == 0 ? "疊層的裝備名：全部通過"
                                         : "疊層的裝備名：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static net.minecraft.network.chat.Component banner() {
        net.minecraft.network.chat.MutableComponent row =
                net.minecraft.network.chat.Component.empty();
        int[] colours = {0xAA00AA, 0x000000, 0xE0B3E6};
        int[] glyphs = {0xE060, 0xE03C, 0xE048};
        for (int k = 0; k < glyphs.length; k++) {
            int c = colours[k];
            row.append(net.minecraft.network.chat.Component.literal(
                    new String(Character.toChars(glyphs[k]))).withStyle(st -> st.withColor(c)));
        }
        return row;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
