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
        abilityNodeTitles();
        report();
    }

    /**
     * 技能樹的節點標題撞到「還沒翻的裝備名」時不能被守門擋住。
     *
     * <h2>實機回報</h2>
     * 法師技能樹的 {@code Diffraction}／{@code Gleam}／{@code Paradox}
     * 標題一直是英文，但 {@code ability/mage.json} 裡明明寫著
     * 「晶化蔓延」「耀光」「悖論」，capture 也抓不到這幾行——
     * {@code hasTranslation} 說得出譯文，是<b>畫的時候</b>被擋掉的。
     *
     * <p>原因是守門看的是名字不是面板：Wynncraft 剛好也有叫
     * {@code Diffraction} 的飾品、叫 {@code Gleam} 與 {@code Paradox} 的防具，
     * 三件都還沒翻，於是三個名字都在 {@code gearNameKeys} 裡。
     * 全庫掃過去這種撞名有 <b>25 組</b>，五個職業都有，不能一條一條排除。
     *
     * <h2>怎麼分</h2>
     * 技能樹的每個節點都有一行 {@code Ability Points:}，裝備永遠沒有。
     * 這裡兩個方向都測：技能樹的標題要翻出來，裝備的標題仍然要擋住——
     * 只測前者的話，把守門整個拿掉也會過。
     */
    private static void abilityNodeTitles() throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // 前提：這幾個字確實同時是「還沒翻的裝備名」與「翻好的技能名」。
        // 哪天翻譯團隊把那幾件裝備翻了，這裡會變成假通過，所以一起釘住。
        for (String[] pair : new String[][] {
                {"Diffraction", "晶化蔓延"}, {"Gleam", "耀光"}, {"Paradox", "悖論"},
                {"Sunshower", "太陽雨"}, {"Harmony", "調和"}, {"Echo", "複演"}}) {
            check("★「" + pair[0] + "」是還沒翻的裝備名（守門會對它出手）",
                  store.isBareGearName(pair[0]));
            check("「" + pair[0] + "」查得到技能譯名（實際 "
                            + store.lookup(pair[0]) + "）",
                  pair[1].equals(store.lookup(pair[0])));
        }

        // 技能樹的節點：標題兩行都要翻出來
        titleBecomes(store, "Diffraction", "晶化蔓延", "✖ Ability Points: 1");
        titleBecomes(store, "Gleam", "耀光", "✖ Ability Points: 1");
        titleBecomes(store, "Paradox", "悖論", "✖ Ability Points: 2");

        // ★ Lootrun 的使命是<b>同一個 bug 的第二次</b>：四個使命名稱都有同名而且
        //   還沒翻的裝備，於是標題被守門擋住。使命面板用「Objective: + Reward:」
        //   兩行認，單看一行太鬆。
        for (String[] pair : new String[][] {
                {"Redemption", "救贖"}, {"Stasis", "停滯"},
                {"Heavensent", "天賜"}, {"Serendipity", "機緣"}}) {
            check("★ 使命「" + pair[0] + "」也是還沒翻的裝備名（守門會對它出手）",
                  store.isBareGearName(pair[0]));
            missionTitle(store, pair[0], pair[1]);
        }
        // Ostinato 沒有同名裝備，守門本來就碰不到它——它先前是空條目而已。
        // 一起測是為了讓五個使命的標題都有人看著。
        missionTitle(store, "Ostinato", "頑固音型");

        // ★ 反方向：真的是裝備時照樣擋住。裝備沒有 Ability Points 那一行。
        //
        // 第三行故意挑一條<b>翻得出來的</b>屬性列：translateLines 整份都沒翻到
        // 的時候回傳空清單，那樣「名字沒被翻」就變成廢話，擋不擋得住都會過。
        titleStays(store, "Diffraction", "Fire Spell Damage +100");
        titleStays(store, "Gleam", "Fire Spell Damage +100");

        loreLines(store);
    }

    /**
     * 撞名的裝備名不是只出現在名稱那一行。
     *
     * <h2>實機回報</h2>
     * 「裝備撞名的問題還是有，會出現在箱子裡的 lore」。守門本來只看第 0、1 行，
     * 而套裝的成員清單、寶箱的獎勵預覽、鑄造材料，都是「項目符號 + 裝備名」
     * <b>單獨佔一行</b>，落在守門外面——實測 33 個裝備名撞名，其中 23 個
     * 不管裸的還是帶項目符號，都會被同名的技能／Major ID 譯文頂掉。
     *
     * <h2>兩邊都要測</h2>
     * 同樣是「- Paradox」，在裝備 tooltip 裡是套裝成員（留原文），在技能樹裡
     * 是「解鎖後將封鎖」的技能（要翻）。分辨靠的是面板，不是那一行本身——
     * 這正是先前那兩次 bug 的教訓。
     */
    private static void loreLines(TranslationStore store) {
        // 裝備 tooltip：成員名留原文，一般的項目符號行照翻
        java.util.List<net.minecraft.network.chat.Component> gear =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal("Ashen Conscience"),
                                net.minecraft.network.chat.Component.literal(
                                        "- Converts up to Liquid Emeralds"),
                                net.minecraft.network.chat.Component.literal("- Guardian"),
                                net.minecraft.network.chat.Component.literal("- Tempest"),
                                net.minecraft.network.chat.Component.literal(
                                        "Left-Click to view contents")),
                        store);
        check("★ 清單那份 tooltip 有翻到東西（不然下面幾項是廢話）", gear.size() == 5);
        String bullet = gear.size() < 2 ? "" : gear.get(1).getString();
        check("一般的項目符號行照翻（實際 " + bullet + "）", bullet.contains("液態綠寶石"));
        for (int row : new int[] {2, 3}) {
            String line = gear.size() <= row ? "" : gear.get(row).getString();
            check("★ 清單裡的裝備名留原文（實際 " + line + "）",
                  line.startsWith("- ") && line.equals(line.replaceAll("[一-鿿]", "")));
        }

        // 技能樹：同樣的行要翻出來
        java.util.List<net.minecraft.network.chat.Component> node =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal("Gleam"),
                                net.minecraft.network.chat.Component.literal(
                                        "Unlocking will block:"),
                                net.minecraft.network.chat.Component.literal("- Paradox"),
                                net.minecraft.network.chat.Component.literal("- Diffraction"),
                                net.minecraft.network.chat.Component.literal(
                                        "✔ Ability Points: 2")),
                        store);
        String blocked = node.size() < 3 ? "" : node.get(2).getString();
        check("★ 技能樹的封鎖清單照樣翻（實際 " + blocked + "）", blocked.contains("悖論"));
    }

    /** 技能樹節點的標題兩行都換成中文。 */
    private static void titleBecomes(TranslationStore store, String name,
                                     String want, String points) {
        java.util.List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(points)),
                        store);
        String line0 = out.isEmpty() ? "" : out.get(0).getString();
        String line1 = out.size() < 2 ? "" : out.get(1).getString();
        check("技能樹「" + name + "」-> 「" + want + "」（實際 "
                        + line0 + " / " + line1 + "）",
              want.equals(line0) && want.equals(line1));
    }

    /** Lootrun 使命面板的標題要翻出來。用「Objective: + Reward:」認出面板。 */
    private static void missionTitle(TranslationStore store, String name, String want) {
        java.util.List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal("Objective:"),
                                net.minecraft.network.chat.Component.literal("Open 12 Chests"),
                                net.minecraft.network.chat.Component.literal("Reward:")),
                        store);
        String line0 = out.isEmpty() ? "" : out.get(0).getString();
        check("使命「" + name + "」-> 「" + want + "」（實際 " + line0 + "）",
              want.equals(line0));
    }

    /** 同一個名字掛在裝備上時，標題仍然留原文。 */
    private static void titleStays(TranslationStore store, String name, String gearLine) {
        java.util.List<net.minecraft.network.chat.Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(
                        java.util.List.of(
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(name),
                                net.minecraft.network.chat.Component.literal(gearLine)),
                        store);
        check("★ 裝備「" + name + "」那份 tooltip 有翻到東西（不然下一項是廢話）",
              out.size() == 3);
        String line0 = out.isEmpty() ? "" : out.get(0).getString();
        String line1 = out.size() < 2 ? "" : out.get(1).getString();
        check("★ 裝備「" + name + "」仍然留原文（實際 " + line0 + " / " + line1 + "）",
              name.equals(line0) && name.equals(line1));
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
