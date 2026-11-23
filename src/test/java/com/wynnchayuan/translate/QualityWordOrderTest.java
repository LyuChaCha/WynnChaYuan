package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class QualityWordOrderTest {

    private static final String ROOT = "src/main/resources/assets/wynnchayuan/translations";
    private static final List<String> TEMPLATED = List.of(
            "ko_kr", "es_es", "pt_br", "ru_ru", "th_th", "tr_tr", "vi_vn", "de_de", "fr_fr");
    private static final List<String> GLUED = List.of("zh_tw", "zh_cn", "ja_jp");
    private static final List<String> QUALITIES = List.of("Perfect", "Defective");

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-quality");
        Files.writeString(dir.resolve("misc.json"), """
                {"Perfect": "Perfeito", "Defective": "Defeituoso",
                 "Perfect {item}": "{item} perfeito", "Defective {item}": "{item} defeituoso",
                 "Soft Helmet": "Capacete macio", "Shiny Sunstar [{~}]": "Sol brilhante [{~}]"}
                """, StandardCharsets.UTF_8);
        TranslationStore fake = new TranslationStore();
        fake.loadAll(dir);
        String perfect = TemplateLookup.lookup("Perfect Soft Helmet", fake);
        check("★ 品質詞照範本放到名字後面（實際 " + perfect + "）", "Capacete macio perfeito".equals(perfect));
        String shiny = TemplateLookup.lookup("Defective Shiny Sunstar [{~}]", fake);
        check("★ 名字後面的計數留在最後（實際 " + shiny + "）", "Sol brilhante defeituoso [{~}]".equals(shiny));

        for (String lang : TEMPLATED) {
            TranslationStore store = load(lang);
            for (String quality : QUALITIES) {
                String form = store.lookup(quality + " {item}");
                check(lang + "：有「" + quality + " {item}」範本（" + form + "）",
                        form != null && form.contains("{item}") && !form.contains("{item}" + store.lookup(quality)));
                String name = store.lookup("Soft Helmet");
                if (name == null || form == null) {
                    continue;
                }
                String shown = TemplateLookup.lookup(quality + " Soft Helmet", store);
                check(lang + "：★ " + quality + " Soft Helmet 照範本組（實際 " + shown + "）",
                        form.replace("{item}", name).equals(shown));
            }
        }

        for (String lang : GLUED) {
            TranslationStore store = load(lang);
            for (String quality : QUALITIES) {
                String word = store.lookup(quality);
                String form = store.lookup(quality + " {item}");
                check(lang + "：沒有範本或範本就是品質詞接名字（" + form + "）",
                        form == null || form.equals(word + "{item}"));
                String name = store.lookup("Soft Helmet");
                if (name == null) {
                    continue;
                }
                String shown = TemplateLookup.lookup(quality + " Soft Helmet", store);
                check(lang + "：" + quality + " 照舊直接接在名字前面（實際 " + shown + "）",
                        (word + name).equals(shown));
            }
        }

        TranslationStore pt = load("pt_br");
        pt.setNameMode(ModConfig.ItemNames.BOTH);
        String both = TemplateLookup.lookup("Perfect Soft Helmet", pt);
        check("★ 譯名加原文：品質詞在譯名後面、原文帶 Perfect（實際 " + both + "）",
                "Capacete macio perfeito (Perfect Soft Helmet)".equals(both));
        TranslationStore tw = load("zh_tw");
        tw.setNameMode(ModConfig.ItemNames.BOTH);
        String twBoth = TemplateLookup.lookup("Perfect Soft Helmet", tw);
        check("繁中譯名加原文照舊（實際 " + twBoth + "）",
                (tw.lookup("Perfect") + tw.lookup("Soft Helmet").replace("(Soft Helmet)", "(Perfect Soft Helmet)"))
                        .equals(twBoth));

        System.out.println(failures == 0 ? "品質詞語序：全部通過" : "品質詞語序：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static TranslationStore load(String lang) {
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(Path.of(ROOT, lang)));
        return store;
    }
}
