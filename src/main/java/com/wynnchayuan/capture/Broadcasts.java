package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 伺服器廣播裡<b>別人的名字</b>在哪裡。
 *
 * <h2>為什麼需要</h2>
 * 「某某人丟了炸彈」這一類廣播，句子是伺服器的文案，主詞是別的玩家：
 *
 * <pre>
 *   {#} I_Rexpz_I has thrown a Combat Experience Bomb on NA1      炸彈鈴（跨世界）
 *   {#} {#}Kasyu_pwq has thrown a                                 同一個世界裡有人丟
 *   {#} Kasyu_pwq Chest Loot Bomb has expired!                    到期
 *   {#} {#}Thank Kasyu_pwq                                        道謝那一列
 * </pre>
 *
 * 先前這幾句一律原樣放過：{@link PlayerDataFilter} 看到別人的名字就擋，
 * 而 {@code {u}} 只認得<b>自己</b>的名字（{@link SelfNames}）。畫面上於是
 * 整塊中文裡夾著一句「Kasyu_pwq has thrown a」（使用者 2026-10-10 回報）。
 *
 * <h2>做法</h2>
 * 句型是固定的，所以名字在哪裡<b>算得出來</b>。這裡只回答「名字從哪到哪」，
 * {@link LineParts#of(StyledText, Spans)} 照那個位置把名字收成 {@code {u}}——
 * 之後查表、還原都是既有的路：名字是佔位符，照原樣（連樣式）填回去，
 * 不會經過語料、詞表、地名表任何一關。語料那邊一條「{@code {#} {u} has thrown a
 * Combat Experience Bomb on {~}}」所有人通用。
 *
 * <p>世界名（{@code NA1}、{@code EU17}）整個收成一個 {@code {~}}：只把數字抽掉的話
 * 鍵是 {@code NA{~}}，每多一個區域就多一輪鍵。
 *
 * <h2>只認得窄</h2>
 * <ul>
 *   <li>名字裡不能有冒號——玩家發言的格式是「名字: 內容」，有人在聊天打出
 *       一模一樣的句子時不會被當成廣播。呼叫端另外只收伺服器發的訊息。</li>
 *   <li>到期那一句的炸彈種類是寫死的清單：名字與種類之間只隔一個空白，而暱稱
 *       本身可以有空白，不靠清單分不出名字到哪裡結束。認不得的種類就不動。</li>
 *   <li>「Thank you for your purchase!」不是道謝那一列。</li>
 * </ul>
 * 認不出來的照舊原樣放過；這個類別不負責擋東西，收集端那一關
 * （{@link PlayerDataFilter}）完全沒動——這幾句一樣不會進 {@code captured.json}。
 *
 * <h2>Wynntils 自己也讀炸彈鈴</h2>
 * 它的 {@code BombModel} 掛的是 {@code ChatMessageEvent.Match}，在改寫
 * （{@code Edit}）之前就讀完了，所以翻掉畫面上那一句不影響它的炸彈清單。
 */
public final class Broadcasts {

    private Broadcasts() {}

    /**
     * 名字與世界名在{@linkplain LineParts#flat 攤平文字}裡的位置。
     * 沒有世界名的句型，後兩個是 {@code -1}。
     */
    public record Spans(int userStart, int userEnd, int worldStart, int worldEnd) {}

    /** 名字：同一行、沒有冒號、不要太長。取最短的那一段（後面接著的句型會把它卡住）。 */
    private static final String NAME = "(?<user>[^\\n:]{1,40}?)";

    /** 炸彈的種類，一到四個首字大寫的詞：Combat Experience、Loot Chest、World Event。 */
    private static final String BOMB = "[A-Z][A-Za-z]*(?: [A-Z][A-Za-z]*){0,3}";

    /**
     * 到期那一句認得的種類。長的排前面：{@code Loot Chest} 要先於 {@code Loot}。
     * 到期用的名字跟丟出去時不完全一樣（{@code Chest Loot}），兩種寫法都列。
     */
    private static final String KNOWN = "(?:Combat Experience|Profession Experience|Profession Speed"
            + "|Combat XP|Profession XP|Scroll Charge|World Event|Loot Chest|Chest Loot"
            + "|Dungeon|Party|Item|Loot)";

    private static final List<Pattern> FRAMES = List.of(
            // 炸彈鈴。伺服器會在「on」前後折行，折點後面是續行的圖示（攤平之後不見了）。
            Pattern.compile("^\\s*" + NAME + " has thrown an? " + BOMB + " Bomb\\s+on\\s+"
                    + "(?<world>[A-Za-z]{1,6}\\d{0,4})\\s*$"),
            // 同一個世界裡有人丟：短的種類接在同一列，長的折到下一列（那一列另有條目）。
            Pattern.compile("^\\s*" + NAME + " has thrown an?(?: " + BOMB + " Bomb\\.?)?\\s*$"),
            // 到期。後面接著「Get your own bombs at …」。
            Pattern.compile("^\\s*" + NAME + " " + KNOWN + " Bomb has expired![\\s\\S]*$"),
            // 道謝那一列（點了會向丟炸彈的人道謝）。
            Pattern.compile("^\\s*Thank (?!you\\b)(?<user>[^\\n:!.]{1,40}?)\\s*$"));

    /**
     * 這則訊息是不是認得的廣播句型。
     *
     * @return 名字（與世界名）的位置；不是就回 {@code null}
     */
    public static Spans find(StyledText message) {
        return message == null ? null : spans(LineParts.flat(message));
    }

    /** 見 {@link #find}。拆出來是為了不必湊一個 {@link StyledText} 也測得到句型。 */
    public static Spans spans(String flat) {
        if (flat == null || flat.isBlank()) {
            return null;
        }
        for (Pattern frame : FRAMES) {
            Matcher m = frame.matcher(flat);
            if (!m.matches()) {
                continue;
            }
            int from = m.start("user");
            int to = m.end("user");
            if (flat.substring(from, to).isBlank()) {
                continue;
            }
            int worldFrom = -1;
            int worldTo = -1;
            if (frame.pattern().contains("(?<world>")) {
                worldFrom = m.start("world");
                worldTo = m.end("world");
            }
            return new Spans(from, to, worldFrom, worldTo);
        }
        return null;
    }

    /**
     * 這則訊息的模板與碎片：認得句型就把名字收成 {@code {u}}，不認得就是平常那一份。
     */
    public static LineParts parts(StyledText message) {
        Spans spans = find(message);
        return spans == null ? LineParts.of(message) : LineParts.of(message, spans);
    }
}
