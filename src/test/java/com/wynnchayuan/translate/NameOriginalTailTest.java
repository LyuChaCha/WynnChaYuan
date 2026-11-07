package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;

import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class NameOriginalTailTest {

    public static void main(String[] args) {
        TranslationStore tw = new TranslationStore();
        tw.loadAll(List.of(Path.of("src/main/resources/assets/wynnchayuan/translations/zh_tw")));
        String sunstar = tw.lookup("Sunstar");
        String defective = tw.lookup("Defective");
        check("前提：Sunstar 有譯名（" + sunstar + "）", sunstar != null && !sunstar.equals("Sunstar"));
        check("前提：Defective 有譯名（" + defective + "）", defective != null);

        tw.setNameMode(ModConfig.ItemNames.BOTH);
        String shiny = tw.lookup("Shiny Sunstar [{~}]");
        check("★ 譯名加原文：原文括號後面跟 [ 之間有空白（實際 " + shiny + "）",
              shiny != null && shiny.endsWith(" (Shiny Sunstar) [{~}]"));
        String flawed = TemplateLookup.lookup("Defective Shiny Sunstar [{~}]", tw);
        check("★ 附上的原文保留 Defective（實際 " + flawed + "）",
              flawed != null && flawed.contains("(Defective Shiny Sunstar)")
                      && flawed.startsWith(defective));
        String plain = TemplateLookup.lookup("Defective Sunstar", tw);
        check("★ 不是 Shiny 的也一樣（實際 " + plain + "）",
              plain != null && plain.contains("(Defective Sunstar)"));

        tw.setNameMode(ModConfig.ItemNames.ON);
        String shown = tw.lookup("Shiny Sunstar [{~}]");
        check("只顯示譯名：括號照原文留一格空白（實際 " + shown + "）",
              shown != null && shown.endsWith(" [{~}]") && !shown.contains("(Shiny"));
        String flawedOn = TemplateLookup.lookup("Defective Shiny Sunstar [{~}]", tw);
        check("只顯示譯名：沒有附原文（實際 " + flawedOn + "）",
              flawedOn != null && !flawedOn.contains("("));

        check("括號裡不是那個名字時不動", "名 (Other)".equals(
                TemplateLookup.qualityInOriginal("名 (Other)", "Defective ", "Shiny Sunstar")));

        System.out.println(failures == 0 ? "附上的原文：全部通過" : "附上的原文：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
