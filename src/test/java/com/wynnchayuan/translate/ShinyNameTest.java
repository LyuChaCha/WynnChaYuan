package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * 「Shiny Sunstar」：前綴翻成「耀光的」，後面照裝備自己的譯名。
 *
 * <p>使用者指定 Shiny 譯「耀光的」。語料不會每件都收一條「Shiny X」，
 * 前綴放在 {@code scoped/name.json}。
 */
public final class ShinyNameTest {

    private static int failures = 0;

    public static void main(String[] args) {
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");

        TranslationStore tw = new TranslationStore();
        tw.loadAll(List.of(root.resolve("zh_tw")));
        tw.setNameMode(CollectorConfig.ItemNames.ON);
        // 繁中的 Mythic 沒翻：前綴翻、名字留英文，中間空一格
        is(tw, "Shiny Sunstar", "耀光的 Sunstar");
        is(tw, "Shiny Masterwork Az", "耀光的 Masterwork Az");
        // ★ Guardian 另有 Major ID 的「守護者」，不能借來當裝備名
        is(tw, "Shiny Guardian", "耀光的 Guardian");
        // 真的叫 Shiny 的物品照自己的譯名
        is(tw, "Shiny Mask", "閃亮面具");
        // 後面不是裝備名的不動
        is(tw, "Shiny Pebbles of Doom", null);

        List<net.minecraft.network.chat.Component> rows =
                com.wynnchayuan.render.TooltipPanel.translateLines(List.of(
                        net.minecraft.network.chat.Component.literal("Shiny Sunstar"),
                        net.minecraft.network.chat.Component.literal("Shiny Sunstar")), tw);
        String shown = rows.isEmpty() ? "(沒翻)" : rows.get(rows.size() - 1).getString();
        check("★ tooltip 名稱行畫成「耀光的 Sunstar」（實際 " + shown + "）",
              "耀光的 Sunstar".equals(shown));

        TranslationStore cn = new TranslationStore();
        cn.loadAll(List.of(root.resolve("zh_tw"), root.resolve("zh_cn")));
        cn.setNameMode(CollectorConfig.ItemNames.ON);
        is(cn, "Shiny Sunstar", "耀光的太阳与星星");
        String mw = cn.lookup("Shiny Masterwork Apocalypse");
        check("簡中 Shiny Masterwork 也照裝備譯名（實際 " + mw + "）",
              mw != null && mw.startsWith("耀光的") && !mw.contains("Apocalypse"));

        cn.setNameMode(CollectorConfig.ItemNames.BOTH);
        is(cn, "Shiny Sunstar", "耀光的太阳与星星 (Shiny Sunstar)");
        cn.setNameMode(CollectorConfig.ItemNames.OFF);
        is(cn, "Shiny Sunstar", null);

        System.out.println(failures == 0 ? "Shiny 名稱：全部通過" : "Shiny 名稱：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void is(TranslationStore store, String src, String want) {
        String got = store.lookup(src);
        check("「" + src + "」→「" + want + "」（實際 " + got + "）",
              want == null ? got == null : want.equals(got));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
