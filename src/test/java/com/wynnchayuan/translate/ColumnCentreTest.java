package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.translate.LineTranslator.Piece;

/**
 * 置中多欄行、圖示色段的查表、以及譯名前後多餘空格。
 *
 * <h2>為什麼這三件事放在一起</h2>
 * 它們都是「畫面上看得出來、程式卻一聲不吭」的那一類：
 *
 * <ul>
 *   <li>技能點數面板的兩欄各自置中，被當成「標籤靠左、數值靠右」補償，
 *       右邊那一欄整個往右擠。整行總寬度仍然是對的，量寬度看不出來。</li>
 *   <li>力量說明最後一段的色段是 {@code §2<圖示> Earth}——圖示與名稱同一段。
 *       {@code strip()} 去不掉圖示，查表落空，譯文的「地屬性」就整個沒有顏色，
 *       只剩圖示是綠的。</li>
 *   <li>語料寫「Bash 的作用範圍」，那個空格是給英文用的。{@code Bash} 被詞典
 *       換成中文之後空格還在，畫面上是「重擊 的作用範圍」。</li>
 * </ul>
 *
 * <p>寬度需要真的字型（{@code Minecraft.getInstance()}），headless 量不到，
 * 所以置中那一段只釘<b>分組規則</b>——欄位分錯組的話補償一定跟著錯。
 */
public final class ColumnCentreTest {

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        groups();
        spacing();
        glyphPrefixedAccent();
        wrapKeepsAccentsWhole();
        huggingPunctuation();
        sectionCodes();
        gluedGaps();

        System.out.println(failures == 0
                ? "ColumnCentreTest 全部通過"
                : "ColumnCentreTest 失敗 " + failures + " 項");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 欄位間隔黏在標籤後面同一個片段裡時，要拆得出來。
     *
     * <h2>先前壞在哪</h2>
     * 物品的需求列遊戲送過來是這樣的（{@code Class Type} 與間隔同一段）：
     *
     * <pre>
     *   ␠Class␠Type<U+CFFC4><U+D004C>    ← 標籤 + 把右欄推到右緣的間隔
     *   Mage/Dark␠Wizard                  ← 右欄
     * </pre>
     *
     * {@code realign} 的第一道關卡是「原文與譯文的間隔數量要一樣」。整段不是純
     * 偏移，所以原文算出 <b>0</b> 個間隔；重建之後偏移被拆成自己一段，譯文算出
     * <b>1</b> 個。數量對不上，整行原樣返回，補償根本沒有跑——
     * 「職業類型」比 {@code Class Type} 窄，右欄就往左跑了。
     *
     * <p>而同一份 tooltip 裡走逐片段那條路的行<b>有</b>補償，走整行查表的沒有，
     * 於是一份物品說明裡兩種對齊方式並存。
     *
     * <h2>這裡釘住什麼</h2>
     * 拆或不拆的<b>判斷</b>。寬度需要真的字型、headless 量不到，所以把「這段算不算
     * 間隔」注入進去——出錯的那一步是拆法，不是量法。
     */
    private static void gluedGaps() {
        Style s = Style.EMPTY;
        // 測試用的判準：只看是不是整段都是偏移碼位，不量寬度。
        java.util.function.BiPredicate<Style, String> isGap =
                (style, text) -> SpaceOffset.isOffsetRun(text);
        String gap = SpaceOffset.encode(60);

        List<LineTranslator.Run> glued = List.of(
                new LineTranslator.Run(false, 0, s, " Class Type" + gap),
                new LineTranslator.Run(false, 0, s, "Mage/Dark Wizard"));
        List<LineTranslator.Run> split = LineTranslator.splitGaps(glued, isGap);
        check("黏在一起的間隔拆得出來（實際 " + split.size() + " 段）",
              split.size() == 3);
        check("拆出來的中間那段是間隔", split.size() == 3 && split.get(1).space());
        check("標籤本身沒有被動到",
              split.size() == 3 && " Class Type".equals(split.get(0).text()));
        check("間隔的寬度解得對（實際 "
                      + (split.size() == 3 ? split.get(1).px() : -1) + "）",
              split.size() == 3 && split.get(1).px() == 60);

        // ★ 反方向一：已經是獨立一段的間隔不用再拆。
        List<LineTranslator.Run> already = List.of(
                new LineTranslator.Run(false, 0, s, "Class Type"),
                new LineTranslator.Run(true, 60, s, gap));
        check("已經獨立的間隔不重複拆",
              LineTranslator.splitGaps(already, isGap).size() == 2);

        // ★ 反方向二：判準說不是間隔就不能拆。材質包會在同一段碼位畫圖示，
        //   那種東西拆出來重新編碼會變成完全不同的字。
        check("判準否決時不拆",
              LineTranslator.splitGaps(glued, (style, text) -> false).size() == 2);

        // ★ 反方向三：整段都是偏移的本來就是一個間隔，不該拆成「空字串 + 間隔」。
        List<LineTranslator.Run> pure = List.of(
                new LineTranslator.Run(false, 0, s, gap));
        check("整段都是偏移時不拆",
              LineTranslator.splitGaps(pure, isGap).size() == 1);
    }

    /** 一行拆成幾個「欄」。 */
    private static void groups() {
        Style s = Style.EMPTY;

        // 技能點數那一列：[2px] 83 points [15px][30px][15px] 84 points
        // 中間那三個偏移是<b>同一段</b>間隔，不是三欄。
        List<Piece> row = List.of(
                Piece.space(2, s),
                Piece.text("83 points", s),
                Piece.space(15, s), Piece.space(30, s), Piece.space(15, s),
                Piece.text("84 points", s));
        List<int[]> g = LineTranslator.textGroups(row);
        check("連續的偏移算同一段間隔，兩欄就是兩組", g.size() == 2);
        check("第一欄落在文字上", g.get(0)[0] == 1 && g.get(0)[1] == 2);
        check("第二欄落在最後那段文字上", g.get(1)[0] == 5 && g.get(1)[1] == 6);

        // 單欄的置中行走舊路（差額對半分），不能被誤判成多欄
        List<Piece> single = List.of(
                Piece.space(10, s),
                Piece.text("* Modified by your gear (+23)", s));
        check("只有前導空白的置中行是一欄", LineTranslator.textGroups(single).size() == 1);

        // 同一欄裡混著圖示與文字（各自是片段）也只算一欄
        List<Piece> mixed = List.of(
                Piece.space(6, s),
                Piece.text("", s), Piece.text(" Earth", s),
                Piece.space(40, s),
                Piece.text("+12", s));
        check("同一欄裡的圖示與文字不會被拆成兩欄",
                LineTranslator.textGroups(mixed).size() == 2);

        // 重排之後片段數不變（只有空白的寬度會變），組裝端才不會錯位
        check("重排不會增減片段",
                LineTranslator.recenterColumns(row).size() == row.size());
    }

    /** 譯名換上中文之後，原本給英文用的空格該不該留。 */
    private static void spacing() {
        check("中文 + 空格 + 換成中文的譯名 → 空格要吃掉",
                LineTranslator.dropsSpaceBefore("提高 ", "重擊"));
        check("英文後面的空格要留著 —— 拿掉會黏成一團",
                !LineTranslator.dropsSpaceBefore("gains ", "重擊"));
        check("譯名還是英文時空格照留",
                !LineTranslator.dropsSpaceBefore("提高 ", "Bash"));
        check("沒有空格就沒事", !LineTranslator.dropsSpaceBefore("提高", "重擊"));

        check("中文譯名後面接空格再接中文 → 吃掉",
                LineTranslator.dropsSpaceAfter("重擊", "重擊 的作用範圍", 2));
        check("後面接的是英文就留著",
                !LineTranslator.dropsSpaceAfter("重擊", "重擊 gains", 2));
        check("行尾沒有東西可以黏，不動",
                !LineTranslator.dropsSpaceAfter("重擊", "重擊 ", 2));

        // 吃掉空格是<b>中日文</b>的規則：方塊字之間本來就不寫空格。
        // 西、德、法、俄、韓都靠空格分詞，吃掉就是把兩個字黏成一個。
        check("德文不吃空格", !LineTranslator.dropsSpaceBefore("mehr ", "Schaden"));
        check("西班牙文不吃空格", !LineTranslator.dropsSpaceBefore("del ", "hechizo"));
        check("俄文不吃空格", !LineTranslator.dropsSpaceBefore("урон ", "заклинаний"));
        check("諺文不吃空格 —— 韓文是靠空格分詞的",
                !LineTranslator.dropsSpaceBefore("마법 ", "피해"));
        check("諺文後面的空格也要留著",
                !LineTranslator.dropsSpaceAfter("피해", "피해 증가", 2));
    }

    /** 色段前面掛著圖示時，剝掉再查。 */
    private static void glyphPrefixedAccent() throws IOException {
        Path dir = Files.createTempDirectory("wynnchayuan-accent");
        write(dir, "ui-labels.json", """
            {
             "Earth": "地屬性",
             "Main Attack": "普攻"
            }""");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        // 力量說明的最後一段：圖示與名稱同屬一個深綠色段
        String segment = " Earth";
        check("直接查帶圖示的色段會落空（這就是原本的狀況）",
                store.lookup(segment) == null);
        check("剝掉前後的圖示與空白之後查得到",
                "地屬性".equals(LineTranslator.lookupWordCore(segment, store)));
        check("圖示在後面也認得",
                "普攻".equals(LineTranslator.lookupWordCore("Main Attack ", store)));
        check("沒有東西可剝就回傳 null，交給前面的查法",
                LineTranslator.lookupWordCore("Earth", store) == null);
        check("剝完查不到就是查不到",
                LineTranslator.lookupWordCore(" Nonsense", store) == null);
    }

    /** 斷行不要把一個重點詞切成兩半。 */
    private static void wrapKeepsAccentsWhole() {
        Style green = Style.EMPTY;
        List<LineParts.Piece> accents = List.of(new LineParts.Piece("地屬性", green));

        // 力量說明的實況：面板寬度把「地屬性」切成「地」＋「屬性」，
        // 兩行各自都找不到整個詞，那個綠色就整個掉了
        String[] split = {"並提高你能造成的 {#} 地", "屬性傷害"};
        String[] fixed = LineTranslator.keepAccentsWhole(split, accents);
        // 圖示要跟著詞一起搬。
        //
        // 這裡原本釘的是「{#} 留在上一行」。實機回報證明那是錯的：
        // 「一件強大的神器，可透過 {#}」換行「物品升級師 將…」——圖示是後面
        // 那個詞的圖示，兩者本來是一體的，拆開之後圖示孤零零掛在行尾。
        //
        // 折行本身有 keepGlyphWithWord 顧著，但這裡是<b>折完之後</b>又把詞搬走，
        // 所以要在搬的時候一併把圖示帶下去。
        check("被切開的重點詞會整個搬到下一行，圖示跟著走（實際 "
                        + fixed[0] + " ⏎ " + fixed[1] + "）",
                fixed[0].equals("並提高你能造成的 ") && fixed[1].equals("{#} 地屬性傷害"));

        // 沒切到的不要動 —— 重排行是有代價的（面板會變寬）
        String[] whole = {"並提高你能造成的 {#} 地屬性", "傷害"};
        check("沒被切到就原樣不動",
                LineTranslator.keepAccentsWhole(whole, accents)[0].endsWith("地屬性"));

        // 佔位符是照順序取用的，搬動它會讓符號池整個錯位
        String[] withToken = {"造成的 {#}", "地屬性傷害"};
        List<LineParts.Piece> odd = List.of(new LineParts.Piece("{#} 地", green));
        check("含佔位符的片段不搬",
                LineTranslator.keepAccentsWhole(withToken, odd)[0].endsWith("{#}"));

        check("只有一行時什麼都不做",
                LineTranslator.keepAccentsWhole(new String[] {"地屬性傷害"}, accents).length == 1);
    }

    /** 緊貼佔位符的標點跟著它走。 */
    private static void huggingPunctuation() {
        // 專業那一行：原文 [66.24%] 整段是暗灰的，譯文的中括號卻畫成整行的主樣式
        check("中括號緊貼數值，算黏著", LineTranslator.hugs('['));
        check("斜線緊貼數值，算黏著", LineTranslator.hugs('/'));
        check("減號緊貼數值，算黏著", LineTranslator.hugs('-'));
        check("空白不算 —— 那是詞距不是黏著", !LineTranslator.hugs(' '));
        check("方塊字不算", !LineTranslator.hugs('傷'));
        check("英文字不算", !LineTranslator.hugs('A'));
        check("數字不算", !LineTranslator.hugs('7'));
        // 圓括號是註解的界線，交給註解那一套處理
        check("左圓括號不算 —— 那是註解的界線", !LineTranslator.hugs('('));
        check("右圓括號不算", !LineTranslator.hugs(')'));

        // 兩個數值中間的斜線：原文的 0/50 是「0」＋「/50」兩段，
        // 斜線跟右邊走。跟左邊走的話畫面上它的顏色會跟原文不同。
        Style next = Style.EMPTY;
        check("整段都是標點時跟後面那個佔位符走",
                LineTranslator.leadTakesNext("/", 1, next));
        check("後面沒有佔位符就留給前面",
                !LineTranslator.leadTakesNext("/", 1, null));
        check("前面還有文字時開頭那截仍歸前一段",
                !LineTranslator.leadTakesNext(" 抄寫 [", 1, next));

        unitSuffix();
    }

    /**
     * 緊貼在數值後面的單位要跟著<b>數值</b>走顏色。
     *
     * <h2>先前壞在哪</h2>
     * 生命竊取那一列原文寫 {@code +445/3s}，整串都是綠的——遊戲把數字與單位放在
     * 同一段。譯文的 {@code s} 是<b>文字</b>，拿到的是整行的正文顏色（標籤那個色），
     * 於是 {@code +445/3} 綠、{@code s} 白，一眼就看得出來。
     *
     * <p>上面那一套只黏標點（{@link LineTranslator#hugs} 明確排除字母），碰不到它。
     *
     * <h2>判準要收緊</h2>
     * 收太寬會把譯文開頭的英文整個染成數值的顏色。所以三道：緊貼（中間連標點都
     * 沒有）、三個以內的 ASCII 小寫字母、後面不能再接字母。
     */
    private static void unitSuffix() {
        int n = 8;
        check("s 緊貼數值時算單位",
              LineTranslator.unitLength("s [37.8%]", true, 0, n) == 1);
        check("stx 也算（綠寶石的堆疊單位）",
              LineTranslator.unitLength("stx", true, 0, 3) == 3);
        check("整段就是單位時也認得",
              LineTranslator.unitLength("s", true, 0, 1) == 1);

        // ★ 反方向。
        check("前一個不是數值就不算",
              LineTranslator.unitLength("s [37.8%]", false, 0, n) == 0);
        check("中間隔著標點就不算緊貼",
              LineTranslator.unitLength("s [37.8%]", true, 1, n) == 0);
        check("後面還接著字母的是單字不是單位",
              LineTranslator.unitLength("seconds", true, 0, 7) == 0);
        check("超過三個字母不算",
              LineTranslator.unitLength("abcd ", true, 0, 5) == 0);
        check("大寫不算", LineTranslator.unitLength("S [1]", true, 0, n) == 0);
        check("中文不算——「{~} 秒」這種寫法不受影響",
              LineTranslator.unitLength(" 秒", true, 0, 2) == 0);
        check("空白開頭不算",
              LineTranslator.unitLength(" x Doom Stone", true, 0, 13) == 0);
    }

    /** 譯文自己帶 {@code §} 格式碼。 */
    private static void sectionCodes() {
        Style base = Style.EMPTY.withColor(net.minecraft.network.chat.TextColor
                .fromRgb(0xAAAAAA));

        check("沒有 § 時原樣輸出",
                "純文字".equals(LineTranslator.coloured("純文字", base).getString()));

        // §c 只管到它後面那一截，前面照樣是這一段原本的樣式
        java.util.List<Component> parts =
                flatten(LineTranslator.coloured("警告§c危險", base));
        int grey = 0;
        int red = 0;
        for (Component leaf : parts) {
            Integer colour = leaf.getStyle().getColor() == null
                    ? null : leaf.getStyle().getColor().getValue();
            if (colour != null && colour == 0xAAAAAA) {
                grey++;
            }
            if (colour != null && colour == 0xFF5555) {
                red++;
            }
        }
        check("§ 之前沿用這一段原本的樣式", grey >= 1);
        check("§c 之後變成紅色", red >= 1);
        check("格式碼本身不會被印出來",
                !LineTranslator.coloured("警告§c危險", base)
                        .getString().contains("§"));

        // §r 回到「這一段原本的樣式」，不是回到全白——否則技能樹那種
        // 顏色來自原文的地方，一寫 §r 就把原本的顏色洗掉了
        java.util.List<Component> reset =
                flatten(LineTranslator.coloured("§c紅§r回來", base));
        Integer last = reset.get(reset.size() - 1).getStyle().getColor() == null
                ? null : reset.get(reset.size() - 1).getStyle().getColor().getValue();
        check("§r 回到這一段原本的樣式（拿到 "
                        + (last == null ? "null" : String.format("#%06X", last)) + "）",
                last != null && last == 0xAAAAAA);

        // § 後面不是格式碼就當普通文字，不要吃掉它
        check("§ 後面不是格式碼時原樣留著",
                LineTranslator.coloured("100§z", base).getString().contains("§z"));
    }

    private static java.util.List<Component> flatten(Component c) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        c.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.add(Component.literal(text).withStyle(style));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static void write(Path dir, String name, String body) throws IOException {
        Files.write(dir.resolve(name), body.getBytes(StandardCharsets.UTF_8));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  OK  " : "  失敗 ") + what);
        if (!ok) {
            failures++;
        }
    }
}
