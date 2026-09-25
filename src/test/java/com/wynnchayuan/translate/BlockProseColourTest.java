package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 整段一次命中時，<b>散文</b>的底色不可以被抽出去的數值投票投走。
 *
 * <h2>實機回報（內容書迷你任務卡）</h2>
 * 原文三行，散文是灰的、方括號裡的物品與等級是青的、座標是白的：
 *
 * <pre>
 *   §7Bring §3[24 Fluffy Fur]§7 to the
 *   §7Slaying Post §3[Combat Lv. 88]§7 at
 *   §f[139, 61, -4399]
 * </pre>
 *
 * 畫面上整段散文卻是<b>青</b>的。
 *
 * <h2>怎麼壞的</h2>
 * 整段的底色是 {@code dominantStyle} 挑的：哪個顏色蓋到的字最多就用哪個。
 * 灰的實字 23 個（{@code Bring}、{@code to the}、{@code Slaying Post}、{@code at}），
 * 青的 26 個（兩個方括號）——青贏。
 *
 * <p>但青的那 26 個裡有 4 個是<b>數值本身</b>（{@code 24}、{@code 88}），
 * 而數值是抽出去另外保管、填回時自己帶著青色回來的；它在譯文裡搬到哪裡都可以，
 * 不該由它決定周圍的散文是什麼顏色。扣掉之後青 22、灰 23，灰贏。
 *
 * <p>這跟 {@link ObjectiveTailColourTest} 釘的是<b>同一類</b>問題的另一個位置：
 * 那邊修的是逐行的 {@code perPartStyles}，這邊是整段的 {@code dominantStyle}。
 *
 * <h2>為什麼要看診斷檔</h2>
 * 「整段內文的底色」這一欄就是 {@code dominantStyle} 的輸出，
 * 使用者回報時貼的也正是它（{@code majorid-debug.txt} 的 {@code === N ===} 區塊）。
 * 直接斷言那一行，測的就是根因本身，不是它在畫面上的某一個影子。
 */
public final class BlockProseColourTest {

    private static int failures = 0;

    private static final int BODY = 0xAAAAAA;   // 灰：散文
    private static final int ITEM = 0x00AAAA;   // 青：方括號裡的物品與等級
    private static final int COORD = 0xFFFFFF;  // 白：座標

    public static void main(String[] args) throws Exception {
        realCard();
        longName();
        coordOnItsOwnRow();
        tealReallyWins();
        report();
    }

    /**
     * 座標整串（連同開頭那個 {@code [}）都是白的。
     *
     * <h2>實機回報（採集站的迷你任務卡）</h2>
     * <pre>
     *   石] 交到采集站 [采矿等级 73]，
     *   坐标 [-712, 46, -5553]
     * </pre>
     * 座標的數字是白的，開頭那個 {@code [} 卻是散文的灰。
     *
     * <p>這一列的字面是「坐标 [-」接數值，前面沒有別的數值可以黏——
     * {@code appendHugging} 得從尾巴往回把 {@code [-} 黏到後面那個數值上。
     * 中間隔著一格空白，而「中間有空白就不算黏著」那條規則管的是詞距，
     * 不是座標開頭的這一格。
     */
    private static void coordOnItsOwnRow() throws Exception {
        String flat = "Bring [{~} Kanderstone Ingots] or [{~} Kanderstone Gems] "
                + "to the Gathering Post [Mining Lv. {~}] at [-{~}, {~}, -{~}]";
        String dst = "把 [{~} Kanderstone 錠] 或 [{~} Kanderstone 寶石] "
                + "交到採集站 [採礦等級 {~}]，座標 [-{~}, {~}, -{~}]";
        List<StyledText> run = List.of(
                line("Bring ", BODY, "[40 Kanderstone Ingots]", ITEM, " or", BODY),
                line("[40 Kanderstone Gems]", ITEM, " to the", BODY),
                line("Gathering Post ", BODY, "[Mining Lv. 73]", ITEM, " at", BODY),
                line("[-712, 46, -5553]", COORD));

        Path debug = Files.createTempDirectory("block-prose-coord");
        FlowedDebug.init(debug);
        List<Component> built = translate(run, flat, dst);
        check("［座標自成一列］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        dump("［座標自成一列］", built);

        String text = flatText(built);
        int at = text.indexOf("712");
        check("［座標自成一列］譯文裡有這個座標", at > 0);
        if (at <= 0) {
            return;
        }
        int bracket = text.lastIndexOf('[', at);
        Integer open = charColour(built, bracket);
        check("★［座標自成一列］座標開頭的「[」是白的，不是散文的灰（拿到 "
                + show(open) + "，實際分段：" + pieces(built) + "）",
                open != null && open == COORD);
        Integer minus = charColour(built, bracket + 1);
        check("★［座標自成一列］「[」後面那個負號也是白的（拿到 " + show(minus) + "）",
                minus != null && minus == COORD);
    }

    /**
     * 名字長一點的那張卡。{@code realCard} 的灰 23 對青 22 只差一個實字——
     * 換一張名字長的就翻盤了，而實機正是這樣：
     *
     * <pre>
     *   §7Bring §3[15 Arcane Anomalies]§7 to
     *   §7the Slaying Post §3[Combat Lv.
     *   §375]§7 at §f[-677, 46, -4948]
     * </pre>
     *
     * <p>扣掉數值之後灰 24、青 28，底色被挑成青的，整段散文在畫面上變成青色。
     * 使用者回報的第一張截圖就是這張卡；同一份 majorid-debug.txt 裡 30 段
     * 有 8 段中招，全是這個形狀。見 {@code LineTranslator#proseCount}。
     */
    private static void longName() throws Exception {
        String flat = "Bring [{~} Arcane Anomalies] to the Slaying Post "
                + "[Combat Lv. {~}] at [-{~}, {~}, -{~}]";
        String dst = "把 [{~} 奧祕異象] 交到擊殺告示 [戰鬥等級 {~}]，座標 [-{~}, {~}, -{~}]";
        List<StyledText> run = List.of(
                line("Bring ", BODY, "[15 Arcane Anomalies]", ITEM, " to", BODY),
                line("the Slaying Post ", BODY, "[Combat Lv.", ITEM),
                line("75]", ITEM, " at ", BODY, "[-677, 46, -4948]", COORD));

        Path debug = Files.createTempDirectory("block-prose-long");
        FlowedDebug.init(debug);
        List<Component> built = translate(run, flat, dst);
        check("［長名稱］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        dump("［長名稱］", built);

        String colour = blockColour(debug);
        check("★［長名稱］整段內文的底色是灰的（拿到 " + colour + "）",
                "#AAAAAA".equals(colour));

        Integer prose = colourOf(built, "交到擊殺告示");
        check("★［長名稱］散文是灰的、不是青的（拿到 " + show(prose) + "）",
                prose != null && prose == BODY);
        Integer coord = colourOf(built, "677");
        check("［長名稱］座標仍是原文的白（拿到 " + show(coord) + "）",
                coord != null && coord == COORD);
        // 原文 `[-677, 46, -4948]` 整串白色，中間那兩個「, 」也是。
        // 逗號會黏在前一個數值上，空白不黏——先前這兩格掉回散文的灰。
        // 見 {@code LineTranslator#appendHugging} 開頭那一段。
        Integer gap = colourOf(built, ", ");
        check("★［長名稱］座標裡的「, 」也是白的，不是散文的灰（拿到 "
                + show(gap) + "，實際分段：" + pieces(built) + "）",
                gap != null && gap == COORD);
    }

    /**
     * 實機那三行：灰 23 個實字、青 26 個（扣掉數值剩 22）、白 14 個（扣掉剩 4）。
     * 底色必須是灰。
     */
    private static void realCard() throws Exception {
        String flat = "Bring [{~} Fluffy Fur] to the Slaying Post "
                + "[Combat Lv. {~}] at [{~}, {~}, -{~}]";
        String dst = "把 [{~} 蓬鬆毛皮] 交到擊殺告示 [戰鬥等級 {~}]，座標 [{~}, {~}, -{~}]";
        List<StyledText> run = List.of(
                line("Bring ", BODY, "[24 Fluffy Fur]", ITEM, " to the", BODY),
                line("Slaying Post ", BODY, "[Combat Lv. 88]", ITEM, " at", BODY),
                line("[139, 61, -4399]", COORD));

        Path debug = Files.createTempDirectory("block-prose-card");
        FlowedDebug.init(debug);
        List<Component> built = translate(run, flat, dst);
        check("［任務卡］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        dump("［任務卡］", built);

        String colour = blockColour(debug);
        check("［任務卡］診斷檔記到了這一段（拿到 " + colour + "）", colour != null);
        check("［任務卡］整段內文的底色是灰的（拿到 " + colour + "）",
                "#AAAAAA".equals(colour));

        Integer prose = colourOf(built, "交到擊殺告示");
        check("［任務卡］散文是灰的（拿到 " + show(prose) + "）",
                prose != null && prose == BODY);
        Integer coord = colourOf(built, "139");
        check("［任務卡］座標仍是原文的白（拿到 " + show(coord) + "）",
                coord != null && coord == COORD);
    }

    /**
     * 反過來那一半：青的實字<b>扣完數值還是</b>比灰的多，底色就該是青。
     *
     * <p>沒有這一條，把「數值不投票」寫成「方括號那色一律不投票」也會通過——
     * 那是把錯誤換一個方向。
     */
    private static void tealReallyWins() throws Exception {
        // 灰只有 Get（3 個實字）；青有 [24 Fluffy Fur]（扣掉 24 剩 11）
        // 與 and [88 Soft Fur] right now（扣掉 88 剩 20），共 31 個。
        String flat = "Get [{~} Fluffy Fur] and [{~} Soft Fur] right now";
        String dst = "取得 [{~} 蓬鬆毛皮] 與 [{~} 柔軟毛皮]，就是現在";
        List<StyledText> run = List.of(
                line("Get ", BODY, "[24 Fluffy Fur]", ITEM),
                line("and [88 Soft Fur] right now", ITEM));

        Path debug = Files.createTempDirectory("block-prose-teal");
        FlowedDebug.init(debug);
        List<Component> built = translate(run, flat, dst);
        check("［青色多數］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        dump("［青色多數］", built);

        String colour = blockColour(debug);
        check("［青色多數］診斷檔記到了這一段（拿到 " + colour + "）", colour != null);
        check("［青色多數］整段內文的底色仍是青的（拿到 " + colour + "）",
                "#00AAAA".equals(colour));

        Integer prose = colourOf(built, "就是現在");
        check("［青色多數］散文是青的（拿到 " + show(prose) + "）",
                prose != null && prose == ITEM);
    }

    /**
     * 語料只收<b>攤平成一行</b>的那一份——實機就是這樣（斷點是 tooltip 寬度決定的，
     * 跟語料對不上），走的也正是診斷檔在記的那條「整段命中」的路。
     */
    private static List<Component> translate(List<StyledText> run, String src, String dst,
                                             String... alsoKeyThenValue)
            throws Exception {
        Path dir = Files.createTempDirectory("block-prose");
        StringBuilder json = new StringBuilder("{\"" + src + "\": \"" + dst + "\"");
        for (int i = 0; i < alsoKeyThenValue.length; i += 2) {
            json.append(", \"").append(alsoKeyThenValue[i])
                .append("\": \"").append(alsoKeyThenValue[i + 1]).append('"');
        }
        Files.writeString(dir.resolve("quest.json"),
                json.append('}').toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return LineTranslator.translateBlock(run, store, new boolean[run.size()]);
    }

    /** 診斷檔裡「整段內文的底色」那一欄，也就是 {@code dominantStyle} 的輸出。 */
    private static String blockColour(Path dir) throws Exception {
        Path file = dir.resolve("majorid-debug.txt");
        if (!Files.exists(file)) {
            return null;
        }
        String mark = "整段內文的底色：";
        for (String row : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            int at = row.indexOf(mark);
            if (at >= 0) {
                return row.substring(at + mark.length()).strip();
            }
        }
        return null;
    }

    private static StyledText line(Object... pairs) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < pairs.length; i += 2) {
            out.append(Component.literal((String) pairs[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) pairs[i + 1]))));
        }
        return StyledText.fromComponent(out);
    }

    /** 畫出去的每一段文字，照順序。 */
    private static List<String> pieces(List<Component> built) {
        List<String> out = new ArrayList<>();
        for (Component c : built) {
            c.visit((style, t) -> {
                out.add(t);
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return out;
    }

    /** 畫出去的整串文字（所有段接起來）。 */
    private static String flatText(List<Component> built) {
        StringBuilder out = new StringBuilder();
        for (Component c : built) {
            c.visit((style, t) -> {
                out.append(t);
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return out.toString();
    }

    /** {@link #flatText} 裡第 {@code index} 個字的顏色。 */
    private static Integer charColour(List<Component> built, int index) {
        List<Integer> hit = new ArrayList<>();
        int[] seen = {0};
        for (Component c : built) {
            c.visit((style, t) -> {
                if (hit.isEmpty() && index < seen[0] + t.length()) {
                    hit.add(style.getColor() == null ? null : style.getColor().getValue() & 0xFFFFFF);
                }
                seen[0] += t.length();
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return hit.isEmpty() ? null : hit.get(0);
    }

    private static String show(Integer c) {
        return c == null ? "null" : "#" + String.format("%06X", c);
    }

    private static void dump(String tag, List<Component> built) {
        for (Component c : built) {
            StringBuilder row = new StringBuilder(tag + "行：");
            c.visit((style, s) -> {
                row.append('[').append(s).append(' ')
                   .append(style.getColor() == null ? "null" : style.getColor().serialize())
                   .append(']');
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println(row);
        }
    }

    /** 含有 {@code needle} 的那一段的顏色。 */
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
