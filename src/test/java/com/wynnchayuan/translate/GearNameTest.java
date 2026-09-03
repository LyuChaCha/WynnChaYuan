package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 裝備名是專有名詞，別的領域不能替它翻譯。
 *
 * <h2>先前壞在哪</h2>
 * 實機回報：神話矛「Guardian」的名稱被翻成「守護者」。那個譯名來自
 * {@code major-id.json}——Wynncraft 真的有一個叫 Guardian 的 Major ID。
 *
 * <p>武器那一筆在 {@code gear-weapon.json} 裡 {@code dst} 是空的，而空譯文
 * 根本不會載入，於是沒有任何地方知道「Guardian 是裝備名」，Major ID 的譯名
 * 就順理成章地蓋上去了。
 *
 * <p>全庫掃過去這種撞名有 <b>50 組</b>：{@code Curse}、{@code Fissure}、
 * {@code Plague}、{@code Momentum}⋯⋯所以不能靠一條一條排除。
 *
 * <h2>這裡釘住什麼</h2>
 * 兩個方向。裝備名沒有自己的譯文時擋下來；<b>有</b>自己的譯文時照常翻——
 * 那是翻譯團隊刻意翻的。只測前者的話，把裝備名全部擋死也會過。
 */
public final class GearNameTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-gear");

        // 裝備檔：一個沒翻、一個翻好了。gearNames 旗標跟出貨的檔案一致。
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Guardian", "dst": "", "role": "name"},
                   "a2": {"src": "Idol", "dst": "神像", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);

        // 別的領域剛好有同名的詞。
        Files.writeString(dir.resolve("major-id.json"),
                "{\"Guardian\": \"守護者\", \"Idol\": \"偶像\"}", StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        check("★ 認得出「還沒翻的裝備名」", store.isBareGearName("Guardian"));

        // ★ 但查表本身<b>不能</b>擋掉——同一個字在別的位置可能完全正當。
        //
        // 第一版是在 lookup 裡擋的，結果把介面標籤一起擋死了：有一把裝備叫
        // Reflection，屬性列的「遠程反傷」也就翻不出來，同一份 tooltip 裡
        // 其他標籤都是中文、只有那一行是英文。實機回報的就是這個。
        check("查表不受影響（實際 " + store.lookup("Guardian") + "）",
              "守護者".equals(store.lookup("Guardian")));

        // 反方向：裝備自己翻好的照常用，而且用的是<b>裝備的</b>譯名。
        String idol = store.lookup("Idol");
        check("翻好的裝備名照常翻（實際 " + idol + "）",
              "神像".equals(idol));

        // ★ 介面標籤跟裝備撞名時，標籤照常翻——這是回報的 Reflection 那一案。
        Files.writeString(dir.resolve("ui-labels.json"),
                "{\"Reflection\": \"遠程反傷\"}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {"b1": {"src": "Reflection", "dst": "", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        store = new TranslationStore();
        store.loadAll(dir);
        check("撞名的介面標籤照常翻（實際 " + store.lookup("Reflection") + "）",
              "遠程反傷".equals(store.lookup("Reflection")));
        check("但它仍然被認得出是還沒翻的裝備名",
              store.isBareGearName("Reflection"));

        // 沒撞名的一般詞不受影響。
        Files.writeString(dir.resolve("misc.json"),
                "{\"Fissure\": \"地裂\"}", StandardCharsets.UTF_8);
        store = new TranslationStore();
        store.loadAll(dir);
        check("不是裝備名的照常翻（實際 " + store.lookup("Fissure") + "）",
              "地裂".equals(store.lookup("Fissure")));

        decorations();
        twoLineName();
        report();
    }

    /**
     * Wynntils 掛在名稱後面的註記要先剝掉才比對得到。
     *
     * <p>實機回報：就地取代模式下靴子 {@code Willow} 被翻成「柳木」
     * （{@code npc.json} 裡的柳木是那棵樹）。守門明明裝了，卻沒擋住——
     * 因為畫面上那一行是 {@code Willow [67.5%]}，模板成了
     * {@code Willow [{~}%]}，跟裝備名清單裡的 {@code Willow} 對不上。
     *
     * <p>反方向一樣要測：名字本身不能被剝掉。
     */
    private static void decorations() {
        check("剝掉鑑定百分比",
              "Willow".equals(com.wynnchayuan.render.TooltipPanel
                      .bareName("Willow [{~}%]")));
        check("剝掉多個註記",
              "Willow".equals(com.wynnchayuan.render.TooltipPanel
                      .bareName("Willow [{~}%] [✔]")));
        check("沒有註記時原樣回傳",
              "Idol".equals(com.wynnchayuan.render.TooltipPanel.bareName("Idol")));
        check("符號佔位符一併剝掉",
              "Idol".equals(com.wynnchayuan.render.TooltipPanel
                      .bareName("{#}{#}Idol{#}")));
        // ★ 整行都是括號的話那不是名字，不能剝成空字串——剝成空的會讓
        //   isBareGearName("") 去比對，而空字串誰都不是。
        check("整行都是括號時不剝",
              "[{~}%]".equals(com.wynnchayuan.render.TooltipPanel
                      .bareName("[{~}%]")));
    }

    /**
     * 名稱其實是兩行：寬度 0 的標記行，加上玩家看得到的那一行。
     *
     * <p>實機 layout-debug：
     *
     * <pre>
     *   [0] 內容   0 px  󏀀Immolation󏀀
     *   [1] 內容 125 px  󏿰󏿏󐀅Immolation [66.3%]
     * </pre>
     *
     * <p>守門只擋第 0 行等於擋了看不見的那一行，第 1 行照樣被詞彙表換掉——
     * 神話矛 Guardian 拿到同名 Major ID 的「守護者」就是這樣。修法靠的是
     * 「兩行剝出同一個名字」，這裡把那個前提釘住。
     */
    private static void twoLineName() {
        check("標記行與顯示行剝出同一個名字",
              com.wynnchayuan.render.TooltipPanel.bareName("{#}Guardian{#}")
                      .equals(com.wynnchayuan.render.TooltipPanel
                              .bareName("{#}{#}{#}{#}{#}Guardian [{~}]")));
        check("沒有鑑定百分比的也一樣（素材）",
              com.wynnchayuan.render.TooltipPanel.bareName("{#}Radioactive Soil{#}")
                      .equals(com.wynnchayuan.render.TooltipPanel
                              .bareName("{#}{#}{#}{#}{#}Radioactive Soil")));
        // 反例：只有一行的物品，下一行是別的東西，不能被誤擋
        check("下一行不是同一個名字就不擋",
              !com.wynnchayuan.render.TooltipPanel.bareName("{#} Earth Powder V")
                      .equals(com.wynnchayuan.render.TooltipPanel
                              .bareName("Tier {~} [■■■■■■■]")));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("裝備名：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("裝備名：全部通過");
    }
}
