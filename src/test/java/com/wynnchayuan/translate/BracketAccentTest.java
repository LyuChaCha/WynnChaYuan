package com.wynnchayuan.translate;

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
        noBracketsInTranslation();
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
