package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * 「Shiny Sunstar」：前綴照 {@code scoped/name.json} 翻，後面照裝備自己的譯名。
 *
 * <p>前綴叫什麼、每一件叫什麼都是語料的事（繁中 2026-10-12 從「耀光的」改成「溯光的」，
 * 同一天 Mythic 也有了譯名），這裡不寫死——守的是「前綴接上裝備自己的名字」這個接法。
 * 語料不會每件都收一條「Shiny X」，前綴放在 {@code scoped/name.json}。
 */
public final class ShinyNameTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");

        TranslationStore tw = new TranslationStore();
        tw.loadAll(List.of(root.resolve("zh_tw")));
        tw.setNameMode(CollectorConfig.ItemNames.ON);
        String twShiny = prefix(root.resolve("zh_tw"));
        check("繁中有 Shiny 的前綴（實際 " + twShiny + "）", !twShiny.isEmpty());
        String sunstarTw = tw.lookup("Sunstar");
        check("繁中的 Sunstar 有譯名（實際 " + sunstarTw + "）",
              sunstarTw != null && !sunstarTw.equals("Sunstar"));
        is(tw, "Shiny Sunstar", twShiny + sunstarTw);
        is(tw, "Shiny Masterwork Az", twShiny + tw.lookup("Masterwork Az"));
        // ★ Guardian 另有 Major ID 的「守護者」，不能借來當裝備名——要用它自己的名字
        String guardian = tw.gearOwnName("Guardian");
        check("★ Guardian 有自己的裝備名，而且跟 Major ID 的不一樣（實際 " + guardian + "）",
              guardian != null && !guardian.equals(tw.lookup("Guardian")));
        is(tw, "Shiny Guardian", twShiny + guardian);
        // 還沒有譯名的裝備（純造字的名字）：前綴翻、名字留英文，中間空一格
        String bare = bareGearName(root.resolve("zh_tw"), tw);
        check("找得到一件還沒有譯名的裝備（沒有的話下一條什麼都沒驗）", bare != null);
        if (bare != null) {
            is(tw, "Shiny " + bare, twShiny + " " + bare);
        }
        // 真的叫 Shiny 的物品照自己的譯名
        is(tw, "Shiny Mask", "閃亮面具");
        // 後面不是裝備名的不動
        is(tw, "Shiny Pebbles of Doom", null);

        List<net.minecraft.network.chat.Component> rows =
                com.wynnchayuan.render.TooltipPanel.translateLines(List.of(
                        net.minecraft.network.chat.Component.literal("Shiny Sunstar"),
                        net.minecraft.network.chat.Component.literal("Shiny Sunstar")), tw);
        String shown = rows.isEmpty() ? "(沒翻)" : rows.get(rows.size() - 1).getString();
        check("★ tooltip 名稱行畫成前綴＋譯名（實際 " + shown + "）",
              (twShiny + sunstarTw).equals(shown));

        TranslationStore cn = new TranslationStore();
        cn.loadAll(List.of(root.resolve("zh_tw"), root.resolve("zh_cn")));
        cn.setNameMode(CollectorConfig.ItemNames.ON);
        String cnShiny = prefix(root.resolve("zh_cn"));
        // 「Sunstar 叫什麼」是語料的事，不是這支測試的事——這裡守的是
        // 「Shiny 這個前綴要接上裝備自己的譯名」。先前寫死「太阳与星星圣器」，
        // 2026-09-29 改譯名的時候就整支紅了，而它跟前綴一點關係都沒有。
        String sunstar = cn.lookup("Sunstar");
        check("簡中的 Sunstar 有譯名（實際 " + sunstar + "）",
              sunstar != null && !sunstar.equals("Sunstar"));
        is(cn, "Shiny Sunstar", cnShiny + sunstar);
        String mw = cn.lookup("Shiny Masterwork Apocalypse");
        check("簡中 Shiny Masterwork 也照裝備譯名（實際 " + mw + "）",
              mw != null && mw.startsWith(cnShiny) && !mw.contains("Apocalypse"));

        cn.setNameMode(CollectorConfig.ItemNames.BOTH);
        is(cn, "Shiny Sunstar", cnShiny + sunstar + " (Shiny Sunstar)");
        cn.setNameMode(CollectorConfig.ItemNames.OFF);
        is(cn, "Shiny Sunstar", null);

        System.out.println(failures == 0 ? "Shiny 名稱：全部通過" : "Shiny 名稱：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 這個語言的 Shiny 前綴：{@code scoped/name.json} 裡 {@code {name}} 前面那一段。 */
    private static String prefix(Path lang) throws Exception {
        String pattern = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
                lang.resolve("scoped/name.json"), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject().get("Shiny").getAsString();
        return pattern.substring(0, pattern.indexOf("{name}"));
    }

    /** 一件還沒有譯名的裝備（名稱條目的 dst 是空的，而且查表也認它是沒翻的裝備名）。 */
    private static String bareGearName(Path lang, TranslationStore store) throws Exception {
        com.google.gson.JsonObject entries = com.google.gson.JsonParser.parseString(
                java.nio.file.Files.readString(lang.resolve("gear-weapon.json"),
                        java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("entries");
        for (String key : entries.keySet()) {
            com.google.gson.JsonObject e = entries.getAsJsonObject(key);
            if ("name".equals(e.get("role").getAsString()) && e.get("dst").getAsString().isBlank()
                    && store.isBareGearName(e.get("src").getAsString())) {
                return e.get("src").getAsString();
            }
        }
        return null;
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
