package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ShinyNameTest {

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
        String guardian = tw.gearOwnName("Guardian");
        check("★ Guardian 有自己的裝備名，而且跟 Major ID 的不一樣（實際 " + guardian + "）",
              guardian != null && !guardian.equals(tw.lookup("Guardian")));
        is(tw, "Shiny Guardian", twShiny + guardian);
        String bare = bareGearName(root.resolve("zh_tw"), tw);
        check("找得到一件還沒有譯名的裝備（沒有的話下一條什麼都沒驗）", bare != null);
        if (bare != null) {
            is(tw, "Shiny " + bare, twShiny + " " + bare);
        }
        is(tw, "{#}Shiny Masterwork Az֎{#}", "{#}" + twShiny + tw.lookup("Masterwork Az") + "֎{#}");
        is(tw, "{#}{#}{#}Shiny Sunstar{#}", "{#}{#}{#}" + twShiny + sunstarTw + "{#}");
        String mwFatal = tw.lookup("Masterwork Fatal");
        check("繁中的 Masterwork Fatal 有譯名（實際 " + mwFatal + "）",
              mwFatal != null && !mwFatal.contains("Fatal"));
        is(tw, "Shiny Mask", "閃亮面具");
        is(tw, "Shiny Pebbles of Doom", null);

        List<net.minecraft.network.chat.Component> rows =
                com.wynnchayuan.render.TooltipPanel.translateLines(List.of(
                        net.minecraft.network.chat.Component.literal("Shiny Sunstar"),
                        net.minecraft.network.chat.Component.literal("Shiny Sunstar")), tw);
        String shown = rows.isEmpty() ? "(沒翻)" : rows.get(rows.size() - 1).getString();
        check("★ tooltip 名稱行畫成前綴＋譯名（實際 " + shown + "）",
              (twShiny + sunstarTw).equals(shown));

        TranslationStore cn = new TranslationStore();
        cn.loadAll(List.of(root.resolve("zh_cn")));
        cn.setNameMode(CollectorConfig.ItemNames.ON);
        if (cn.lookup("Sunstar") == null || pattern(root.resolve("zh_cn")).isEmpty()) {
            check("簡中沒有 Sunstar 或 Shiny 的譯名時留原文（實際 " + cn.lookup("Shiny Sunstar") + "）",
                  cn.lookup("Shiny Sunstar") == null);
            System.out.println(failures == 0 ? "Shiny 名稱：全部通過" : "Shiny 名稱：" + failures + " 項失敗");
            System.exit(failures == 0 ? 0 : 1);
        }
        String cnShiny = prefix(root.resolve("zh_cn"));
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

    private static String prefix(Path lang) throws Exception {
        String pattern = pattern(lang);
        return pattern.substring(0, pattern.indexOf("{name}"));
    }

    private static String pattern(Path lang) throws Exception {
        return com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
                lang.resolve("scoped/name.json"), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject().get("Shiny").getAsString();
    }

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

}
