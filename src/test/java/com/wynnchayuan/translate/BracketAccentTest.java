package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 原文的色段<b>整塊</b>被方括號包起來時，譯文的方括號要跟著上色。
 *
 * <h2>實機回報（Wynntils 內容書右邊那張卡的標題）</h2>
 * 卡片標題是「名稱 + 類型」兩個顏色，而類型那一段<b>含方括號</b>：
 *
 * <pre>
 *   #FF8C19 「Theatre Royal 」       名稱，橘色
 *   #AAAAAA 「[Cave]」               類型，灰色
 * </pre>
 *
 * 使用者回報「[洞窟] 應該是灰色」——畫面上它不是。診斷檔
 * {@code tooltip-partial-6.json} 看得到實際畫出去的分段：原文是
 * {@code ["Dragonkin Nest ", "[Cave]"]} 兩段，譯文卻被拆成
 * {@code ["龍裔巢穴 ", "[", "洞窟", "]"]} 四段。
 *
 * <h2>怎麼壞的</h2>
 * 語料的條目是純文字、沒有 {@code {cN}}，所以灰色是拿原文色段的<b>字面</b>到
 * 譯文裡找位置貼回去的（見 {@code appendText}）。{@code [Cave]} 查表查不到，
 * {@link LineTranslator#lookupWordCore} 剝掉標點之後查到的是 {@code Cave}
 * →「洞窟」，於是登記進去的重點段是<b>沒有括號</b>的「洞窟」兩個字，
 * 括號留在名稱那一段的顏色裡。
 *
 * <p>畫面上看得出來的有兩種：
 * <ul>
 *   <li>「獵殺怨靈與幻影 {@code [}迷你任務{@code ]}」——兩個括號是名稱的橘色，
 *       中間四個字才是灰的。</li>
 *   <li>「末日洞窟 [洞窟]」——名稱<b>自己就含有</b>類型詞，而字面比對挑的是
 *       <b>第一個</b>，於是灰色貼到了名稱裡的「洞窟」，真正該灰的那個還是橘的。
 *       這一種錯得更明顯：看起來像名稱中間隨機兩個字變色。</li>
 * </ul>
 *
 * <h2>修法</h2>
 * {@link LineTranslator#wrapLikeSource}：原文那一段本身就是 {@code [...]}
 * 包起來的，而譯文裡找到的是去掉括號的內層時，把重點段<b>往外擴到括號</b>。
 * 擴出去之後字面唯一，名稱裡的同名詞也不會再被挑中。
 * {@link #rules()} 釘住它不會擴過頭。
 */
public final class BracketAccentTest {

    private static int failures = 0;

    private static final int NAME = 0xFF8C19;   // 橘：名稱
    private static final int TYPE = 0xAAAAAA;   // 灰：類型

    public static void main(String[] args) throws Exception {
        FlowedDebug.init(Files.createTempDirectory("wynnchayuan"));

        card();
        nameContainsType();
        miniQuest();
        wrappedSpan();
        splitWord();
        noBracketsInTranslation();
        plurals();
        rules();

        report();
    }

    /**
     * 遊戲送來的樣子：名稱一段、{@code [類型]} 整塊另一段。
     *
     * @param name 名稱那一段，含它後面那個空格
     * @param type 類型那一段，含兩個方括號
     */
    private static StyledText title(String name, String type) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(name).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(NAME))));
        line.append(Component.literal(type).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(TYPE))));
        return StyledText.fromComponent(line);
    }

    /**
     * 語料另外寫一份（不讀 {@code misc.json}），這樣別人改語料不會讓這幾條測試
     * 莫名其妙地紅燈或綠燈。內容跟實機的那幾條一模一樣：整條標題有譯文，
     * 類型詞自己也有一條。
     */
    private static TranslationStore corpus(String key, String zh,
                                           String type, String typeZh) throws Exception {
        Path dir = Files.createTempDirectory("bracket-accent");
        Files.writeString(dir.resolve("misc.json"), "{\"" + key + "\": \"" + zh + "\"}");
        Files.writeString(dir.resolve("wynntils.json"),
                "{\"" + type + "\": \"" + typeZh + "\"}");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    /**
     * 卡片寫複數、語料收單數。
     *
     * <p>迷你任務的方括號寫的是「這一次要幾個」，所以物品名是複數
     * （{@code [15 Arcane Anomalies]}）；語料收的是物品本身，鍵永遠是單數
     * （{@code Arcane Anomaly}）。查不到就拿不到重點色，那一塊會掉回底色。
     *
     * <p>拿實機那幾張卡的物品名對過，七個查不到的複數裡這一步救回六個。
     * 見 {@code LineTranslator#asSingular}。
     */
    private static void plurals() throws Exception {
        TranslationStore store = corpus(
                "Arcane Anomaly", "奧祕異象", "Dragonling Scale", "龍崽鱗片");

        check("「[{~} Arcane Anomalies]」→ 奧祕異象（-ies 換回 -y）",
              "奧祕異象".equals(
                      LineTranslator.lookupWordCore("[15 Arcane Anomalies]", store)));
        check("「[{~} Dragonling Scales]」→ 龍崽鱗片（剝掉 -s）",
              "龍崽鱗片".equals(
                      LineTranslator.lookupWordCore("[15 Dragonling Scales]", store)));

        TranslationStore ss = corpus("Glass", "玻璃", "Moss", "苔蘚");
        check("「-ss」結尾的不剝（Glass 不會變成 Glas）",
              "玻璃".equals(LineTranslator.lookupWordCore("[3 Glass]", ss)));
        check("查不到的仍然回傳 null（不亂猜）",
              LineTranslator.lookupWordCore("[3 Widgets]", ss) == null);
    }

    /** 使用者回報的那張卡：「[洞窟]」三個字元是同一段、而且是灰的。 */
    private static void card() throws Exception {
        TranslationStore store = corpus(
                "Dragonkin Nest [Cave]", "龍裔巢穴 [洞窟]", "Cave", "洞窟");
        Component one = LineTranslator.translate(
                title("Dragonkin Nest ", "[Cave]"), store);
        check("整條標題查得到譯文", one != null);
        if (one == null) {
            return;
        }
        List<Component> built = List.of(one);
        System.out.println("譯文：" + text(built));
        dump(built);

        check("名稱翻出來了", text(built).contains("龍裔巢穴"));
        check("類型翻出來了", text(built).contains("[洞窟]"));
        check("名稱是橘的（拿到 " + show(colourOf(built, "龍裔巢穴")) + "）",
              is(colourOf(built, "龍裔巢穴"), NAME));
        // 括號連著類型詞是同一段——原文就是這樣切的，譯文也該這樣。
        check("★ 「[洞窟]」是完整的一段（實際分段：" + pieces(built) + "）",
              pieces(built).contains("[洞窟]"));
        check("★ 「[洞窟]」整段是灰的（拿到 " + show(colourOf(built, "[洞窟]")) + "）",
              is(colourOf(built, "[洞窟]"), TYPE));
    }

    /**
     * 名稱的譯文<b>自己就含有</b>類型詞：「末日洞窟 [洞窟]」。
     *
     * <p>字面比對挑的是第一個，所以沒有括號的「洞窟」會貼到名稱裡去——
     * 畫面上是「末日」橘、「洞窟」灰，後面真正的 [洞窟] 反而是橘的。
     * 語料裡這種名稱不少：Cave of Doom、Cave of the Seasons、Cave of Sand…
     */
    private static void nameContainsType() throws Exception {
        TranslationStore store = corpus(
                "Cave of Doom [Cave]", "末日洞窟 [洞窟]", "Cave", "洞窟");
        Component one = LineTranslator.translate(
                title("Cave of Doom ", "[Cave]"), store);
        check("［名稱含類型詞］查得到譯文", one != null);
        if (one == null) {
            return;
        }
        List<Component> built = List.of(one);
        System.out.println("［名稱含類型詞］譯文：" + text(built));
        dump(built);

        check("★［名稱含類型詞］名稱整塊是橘的（實際分段：" + pieces(built)
              + "，拿到 " + show(colourOf(built, "末日洞窟")) + "）",
              is(colourOf(built, "末日洞窟"), NAME));
        check("★［名稱含類型詞］「[洞窟]」是完整的一段", pieces(built).contains("[洞窟]"));
        check("★［名稱含類型詞］「[洞窟]」是灰的（拿到 "
              + show(colourOf(built, "[洞窟]")) + "）",
              is(colourOf(built, "[洞窟]"), TYPE));
    }

    /** 迷你任務那一種：整塊 {@code [迷你任務]} 要是灰的，兩個括號不可以留在橘色裡。 */
    private static void miniQuest() throws Exception {
        TranslationStore store = corpus(
                "Slay Wraiths & Phantasms [Mini-Quest]", "獵殺怨靈與幻影 [迷你任務]",
                "Mini-Quest", "迷你任務");
        Component one = LineTranslator.translate(
                title("Slay Wraiths & Phantasms ", "[Mini-Quest]"), store);
        check("［迷你任務］查得到譯文", one != null);
        if (one == null) {
            return;
        }
        List<Component> built = List.of(one);
        System.out.println("［迷你任務］譯文：" + text(built));
        dump(built);

        check("★［迷你任務］「[迷你任務]」是完整的一段（實際分段：" + pieces(built) + "）",
              pieces(built).contains("[迷你任務]"));
        check("★［迷你任務］「[迷你任務]」是灰的（拿到 "
              + show(colourOf(built, "[迷你任務]")) + "）",
              is(colourOf(built, "[迷你任務]"), TYPE));
        check("［迷你任務］名稱是橘的（拿到 " + show(colourOf(built, "獵殺怨靈與幻影")) + "）",
              is(colourOf(built, "獵殺怨靈與幻影"), NAME));
    }

    private static final int BODY = 0xAAAAAA;   // 灰：散文
    private static final int ITEM = 0x00AAAA;   // 青：括號裡的物品
    private static final int COORD = 0xFFFFFF;  // 白：座標

    private static Style colour(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }

    private static LineParts.Piece piece(String text, int rgb) {
        return new LineParts.Piece(text, colour(rgb));
    }

    /**
     * 面板寬度把 {@code [{~} 麥芽穀粒]} 斷在數值後面。
     *
     * <h2>實機回報</h2>
     * 「把 {@code [20 麥芽線]} 或 {@code [20}」換行「麥芽穀粒{@code ]} 交到採集站，」
     * ——名字是青的，緊跟在後面的那個 {@code ]} 卻掉回灰色。
     *
     * <h2>怎麼壞的</h2>
     * 斷行點就是數值後面那個空格，而空格會被<b>吃掉</b>：上一行結尾是
     * {@code …[{~}}、下一行開頭是 {@code 麥芽穀粒]}。
     * {@link LineTranslator#bracketAccents} 為佔位符切出來的那幾段各登記一次，
     * 而收尾那一段是 {@code " 麥芽穀粒]"}——<b>帶著前導空格</b>，於是斷行之後
     * 兩行都對不上。{@code keepAccentsWhole} 也搬不動它：要搬的那一半含佔位符。
     * 剩下能貼的只有物品名查表拿到的「麥芽穀粒」四個字，{@code ]} 沒人管。
     *
     * <h2>為什麼測登記、不測畫面</h2>
     * 斷行是照<b>面板寬度</b>折的，而測試環境沒有真的字型，折不出實機那一刀。
     * 登記進去之後誰勝出是既有規則：{@code appendText} 同一個位置取<b>比較長</b>
     * 的那一段，所以「麥芽穀粒{@code ]}」會壓過查表來的「麥芽穀粒」。
     */
    private static void wrappedSpan() {
        // 實機那張卡的三行，照原樣切段
        List<LineParts.Piece> runs = List.of(
                piece("Bring ", BODY), piece("[20 Malt String]", ITEM), piece(" or", BODY),
                piece("[20 Malt Grains]", ITEM), piece(" to the", BODY),
                piece("Gathering Post at ", BODY), piece("[-1234, 50, -4321]", COORD));
        String[] translated = {
            "把 [{~} 麥芽線] 或 [{~} 麥芽穀粒] 交到採集站，座標 [-{~}, {~}, -{~}]"
        };
        List<String> texts = new ArrayList<>();
        for (LineParts.Piece accent
                : LineTranslator.bracketAccents(runs, translated, colour(BODY))) {
            texts.add(accent.text());
        }
        System.out.println("［跨行括號］登記到的重點段：" + texts);

        check("［跨行括號］整塊 [{~} 麥芽穀粒] 有登記", texts.contains("[{~} 麥芽穀粒]"));
        check("［跨行括號］原本帶空格的那一份還在（沒斷行時靠它）",
              texts.contains(" 麥芽穀粒]"));
        check("★［跨行括號］去掉前導空格的「麥芽穀粒]」也登記了（斷行之後靠它）",
              texts.contains("麥芽穀粒]"));
        // 座標那組不可以跟著剝：「, 」「-」到處都有，貼上去會貼到別的地方
        check("［跨行括號］座標那組沒有多登記短到會撞的片段（實際：" + texts + "）",
              !texts.contains(", ") && !texts.contains("-"));
    }

    /**
     * 斷行落在<b>詞中間</b>：「…Kanderstone 寶」換行「石]」。
     *
     * <h2>實機回報</h2>
     * 採集站那張卡：「把 {@code [32 Kanderstone 錠]} 或 {@code [32 Kanderstone 寶}」
     * 換行「石{@code ]} 交到採集站 {@code [採礦等級 71]}，」——下半截掉回底色。
     *
     * <h2>為什麼 keepAccentsWhole 救不了</h2>
     * 它最多搬十二個字，而這裡要搬的是「{@code  Kanderstone 寶}」十五個字；
     * 真搬下去會把面板撐寬一大截。{@link LineTranslator#halvesAcrossBreaks}
     * 改成認這一刀，兩半各自登記。
     */
    private static void splitWord() {
        String[] flowed = {
            "把 [{~} Kanderstone 錠] 或 [{~} Kanderstone 寶",
            "石] 交到採集站 [採礦等級 {~}]，",
        };
        List<LineParts.Piece> accents = List.of(
                piece("[{~} Kanderstone 寶石]", ITEM),
                piece(" Kanderstone 寶石]", ITEM),
                piece("[{~} Kanderstone 錠]", ITEM),
                piece(" Kanderstone 錠]", ITEM));
        List<String> texts = new ArrayList<>();
        for (LineParts.Piece half
                : LineTranslator.halvesAcrossBreaks(flowed, accents)) {
            texts.add(half.text());
        }
        System.out.println("［詞被切開］補登記的兩半：" + texts);

        check("★［詞被切開］上一行那半登記了", texts.contains(" Kanderstone 寶"));
        check("★［詞被切開］下一行那半登記了（收尾那個 ] 靠它才有色）",
              texts.contains("石]"));
        // 沒被切到的那一段不該多出東西來
        check("［詞被切開］沒被切到的重點段不補（實際：" + texts + "）",
              !texts.contains(" Kanderstone 錠]"));
        // 含佔位符的那一半不登記：畫的時候它在畫面上不連續
        for (String t : texts) {
            check("［詞被切開］補的兩半都不含佔位符（" + t + "）",
                  t.indexOf('{') < 0 && t.indexOf('}') < 0);
        }
    }

    /**
     * 譯文<b>沒有</b>照原文寫方括號時，維持原本的行為：內層那個詞照樣上色，
     * 不可以憑空把附近的字括進來。
     */
    private static void noBracketsInTranslation() throws Exception {
        TranslationStore store = corpus(
                "Dragonkin Nest [Cave]", "龍裔巢穴洞窟", "Cave", "洞窟");
        Component one = LineTranslator.translate(
                title("Dragonkin Nest ", "[Cave]"), store);
        check("［無括號］查得到譯文", one != null);
        if (one == null) {
            return;
        }
        List<Component> built = List.of(one);
        System.out.println("［無括號］譯文：" + text(built));
        dump(built);
        check("［無括號］譯文裡沒有多出來的方括號（實際：" + text(built) + "）",
              text(built).indexOf('[') < 0 && text(built).indexOf(']') < 0);
        check("［無括號］類型詞仍是灰的（拿到 " + show(colourOf(built, "洞窟")) + "）",
              is(colourOf(built, "洞窟"), TYPE));
    }

    /**
     * 往外擴的規則本身：該擴的擴、不該擴的一個都不能擴。
     *
     * <p>語料裡 630 條這種標題（{@code 名稱 [文字類型]}）全是同一個樣子，
     * 六個語言都一樣：{@code Cave} 199、{@code Quest} 110、{@code Mini-Quest} 85、
     * {@code Secret Discovery} 23、{@code Dungeon} 19、{@code Boss Altar} 14、
     * {@code World Discovery} 12 …，譯文一律寫成 {@code 譯名 [類型譯名]}。
     */
    private static void rules() {
        // 該擴的：原文整塊被括起來，譯文裡是去掉括號的內層。
        wraps("[Cave]", "洞窟", "龍裔巢穴 [洞窟]", "[洞窟]");
        wraps("[Mini-Quest]", "迷你任務", "獵殺蜘蛛 [迷你任務]", "[迷你任務]");
        wraps("[Secret Discovery]", "隱藏探索點", "英雄的起點 [隱藏探索點]", "[隱藏探索點]");
        wraps("[Boss Altar]", "首領祭壇", "腐化祭壇 [首領祭壇]", "[首領祭壇]");
        // 名稱自己含有類型詞也照擴——擴出去之後字面唯一，才不會貼到名稱裡去。
        wraps("[Cave]", "洞窟", "末日洞窟 [洞窟]", "[洞窟]");

        // 括號裡除了那個詞還有一個<b>數量</b>。迷你任務的敘述全長這樣；
        // 不連數量一起擴的話方括號會半青半灰（[24 青、蓬鬆毛皮] 掉回底色）。
        wraps("[24 Fluffy Fur]", "蓬鬆毛皮",
              "把 [{~} 蓬鬆毛皮] 交到討伐告示", "[{~} 蓬鬆毛皮]");
        wraps("[16 Salmon Oil]", "鮭魚油",
              "把 [{~} 鮭魚油] 或 [{~} 鮭魚肉] 交到", "[{~} 鮭魚油]");
        // 數量已經填回真正的數字時一樣認得
        wraps("[24 Fluffy Fur]", "蓬鬆毛皮", "把 [24 蓬鬆毛皮] 交到", "[24 蓬鬆毛皮]");

        // 數量以外還有實字的不擴——免得把兩個詞的括號整塊吃掉
        wraps("[24 Fluffy Fur]", "蓬鬆毛皮", "把 [上等 蓬鬆毛皮] 交到", null);

        // 不該擴的：
        wraps("[Cave]", "[洞窟]", "龍裔巢穴 [洞窟]", null);       // 譯文本來就帶括號
        wraps("Nest [Cave]", "巢穴 洞窟", "龍裔巢穴 洞窟", null);   // 括號左邊還有字
        wraps("[Cave] Lv. 30", "洞窟 30 級", "洞窟 30 級", null);   // 括號右邊還有字
        wraps("[Cave]", "洞窟", "龍裔巢穴的洞窟", null);            // 譯文沒寫括號
        wraps("[Cave]", "洞窟", "龍裔巢穴 (洞窟)", null);           // 譯文換成圓括號
        wraps("[-1763, 136,", "座標", "座標", null);                // 座標被寬度切斷
        wraps("[]", "空", "[空]", null);                           // 括號裡沒東西
        wraps("[a] [b]", "甲乙", "[甲乙]", null);                   // 兩組括號，看不出擴到哪
        wraps("(Cave)", "洞窟", "龍裔巢穴 (洞窟)", null);           // 只處理方括號
        wraps("[Cave]", "   ", "[   ]", null);                     // 譯文是空白
    }

    private static void wraps(String core, String zh, String translated, String want) {
        String got = LineTranslator.wrapLikeSource(core, zh, translated);
        check("「" + core + "」＋「" + zh + "」→ " + (want == null ? "不擴" : want)
              + "（拿到 " + got + "）", java.util.Objects.equals(got, want));
    }

    private static boolean is(Integer colour, int want) {
        return colour != null && colour == want;
    }

    private static String show(Integer c) {
        return c == null ? "null" : "#" + String.format("%06X", c);
    }

    private static String text(List<Component> built) {
        StringBuilder out = new StringBuilder();
        for (Component c : built) {
            out.append(c.getString());
        }
        return out.toString();
    }

    private static void dump(List<Component> built) {
        for (Component c : built) {
            StringBuilder row = new StringBuilder("  行：");
            c.visit((style, s) -> {
                row.append('[').append(s).append(' ')
                   .append(style.getColor() == null ? "null" : style.getColor().serialize())
                   .append(']');
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println(row);
        }
    }

    /** 畫出去的每一段文字，照順序。 */
    private static List<String> pieces(List<Component> built) {
        List<String> out = new ArrayList<>();
        for (Component c : built) {
            c.visit((style, s) -> {
                out.add(s);
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return out;
    }

    /**
     * 含有 {@code needle} 的<b>第一</b>段的顏色。
     *
     * <p>找不到<b>一整段</b>裝得下 {@code needle} 的就回傳 {@code null}——那表示
     * 那幾個字被拆到了兩段以上，而拆開本身就是這條測試要擋的事。
     */
    private static Integer colourOf(List<Component> built, String needle) {
        List<Integer> found = new ArrayList<>();
        for (Component c : built) {
            c.visit((style, s) -> {
                if (s.contains(needle) && style.getColor() != null) {
                    found.add(style.getColor().getValue() & 0xFFFFFF);
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "[通過] " : "[失敗] ") + name);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            throw new AssertionError(failures + " 項檢查未通過");
        }
        System.out.println("全部通過");
    }
}
