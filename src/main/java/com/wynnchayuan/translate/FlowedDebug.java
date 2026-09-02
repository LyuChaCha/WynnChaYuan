package com.wynnchayuan.translate;

import com.wynntils.core.text.PartStyle;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * 跨行查表的現場：Major ID 與技能敘述。
 *
 * <h2>為什麼要自己一個檔</h2>
 * 這段診斷原本寄生在 {@code LayoutDebug} 裡，共用同一個緩衝、同一份額度、
 * 同一個「出事就停」的開關。結果使用者照指示操作了三次，回報回來的
 * {@code layout-debug.txt} 一次有內容、一次只有檔頭、一次連 record 的內容都沒有——
 * 到底是沒被呼叫、還是被別的東西擦掉，完全分不出來。
 *
 * <p>所以獨立出來：自己的檔案、自己的額度、<b>附加寫入</b>不重寫，
 * 而且每一次呼叫都先記一行流水號。這樣「有沒有被呼叫到」本身就是可觀察的。
 */
public final class FlowedDebug {

    private static final String FILE = "majorid-debug.txt";

    /** 最多記幾筆。夠看出問題，又不會把玩家的資料夾塞爆。 */
    private static final int LIMIT = 30;

    private static Path file;
    private static int seen = 0;

    /**
     * 已經記過的內容。
     *
     * <h2>為什麼要去重</h2>
     * tooltip 是<b>每一幀</b>重翻一次的。滑鼠停在一件裝備上兩秒，同一段就被記了
     * 三十次——額度用完，而使用者真正想看的那個 tooltip 一筆都沒進來。
     * 實測回報回來的檔案裡，30 筆全是同一個 Major ID、20 筆全是同一個綠寶石袋。
     *
     * <p>同樣的內容記一次就夠：要看的是「有哪些情況」，不是「發生幾次」。
     */
    private static final java.util.Set<String> already =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private FlowedDebug() {}

    public static void init(Path configDir) {
        file = configDir.resolve(FILE);
        seen = 0;
        accentSeen = 0;
        rowSeen = 0;
        missed = 0;
        prose = 0;
        labelled = 0;
        choicesSeen = 0;
        already.clear();
        try {
            Files.createDirectories(configDir);
            Files.writeString(file,
                    "# 跨行查表的現場（Major ID、技能敘述）。" + System.lineSeparator()
                    + "# 每一次跨行翻譯都會記一筆，含原文每一段的顏色與程式挑中的樣式。"
                    + System.lineSeparator()
                    + "# 檔案只有這兩行，就代表跨行查表<b>一次都沒被呼叫到</b>。"
                    + System.lineSeparator() + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (Throwable t) {
            file = null;
        }
    }

    /**
     * 記一次跨行翻譯。
     *
     * @param label      查到的譯名，整段命中時是 null
     * @param chosen     要套在<b>譯名</b>上的樣式。整段命中（label 是 null）時
     *                   這一欄沒有用到——先前沒寫清楚，看的人會拿它去解釋
     *                   整段的顏色，然後查錯方向。
     * @param blockStyle 整段內文的底色。<b>這一欄才是</b>決定敘述長什麼樣的那個。
     */
    public static void note(List<StyledText> run, String label, Style chosen,
                            Style blockStyle) {
        if (file == null || run == null || run.isEmpty() || seen >= LIMIT) {
            return;
        }
        StringBuilder id = new StringBuilder("note");
        for (StyledText one : run) {
            id.append(one.getString());
        }
        if (!already.add(id.toString())) {
            return;                            // 每一幀都會重翻一次，見 #already
        }
        seen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(seen).append(" ===").append(System.lineSeparator());
            sb.append("  譯名：")
              .append(label == null ? "（整段命中，沒有拆出名稱）" : label)
              .append(System.lineSeparator());
            sb.append("  譯名的樣式：").append(describe(chosen))
              .append(label == null ? "（整段命中，這一欄沒用到）" : "")
              .append(System.lineSeparator());
            sb.append("  整段內文的底色：").append(describe(blockStyle))
              .append(System.lineSeparator());
            sb.append("  原文第一行的色段：").append(System.lineSeparator());
            int i = 0;
            for (StyledTextPart part : run.get(0)) {
                PartStyle ps = part.getPartStyle();
                sb.append("    [").append(i++).append("] ")
                  .append(describe(ps == null ? null : ps.getStyle()))
                  .append("  「").append(part.getString(null, StyleType.NONE))
                  .append("」").append(System.lineSeparator());
            }
            for (StyledText line : run) {
                sb.append("  原文：").append(line.getString())
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("[WynnChaYuan] 跨行查表 #" + seen);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面。不把 file 設成 null——
            // 那等於一次失敗就永久關閉，而使用者只會看到「檔案沒生成」。
        }
    }

    /**
     * 重點段有哪些、哪幾個真的貼上去了。
     *
     * <h2>為什麼需要這一欄</h2>
     * 「譯文裡某個詞沒有顏色」有<b>三個</b>可能：那一段根本沒被收成重點段、
     * 收了但查不到中文的說法、或是查到了卻在譯文裡比對不到（被斷行切開、
     * 被更長的詞卡住）。三者在畫面上長得一模一樣，只能用猜的——
     * 「地屬性沒有顏色」就這樣猜了兩輪。
     *
     * <p>這裡把三者分開寫：每一段的文字、它的樣式、以及最後有沒有被用掉。
     * 「在譯文裡卻沒貼上」就是比對那一步的問題，跟查表無關。
     */
    public static void accents(List<String> texts, List<Style> styles,
                               boolean[] used, String[] translated) {
        if (file == null || texts == null || texts.isEmpty()
                || accentSeen >= ACCENT_LIMIT) {
            return;
        }
        StringBuilder id = new StringBuilder("accents");
        for (String line : translated) {
            id.append(line);
        }
        if (!already.add(id.toString())) {
            return;                            // 每一幀都會重翻一次，見 #already
        }
        accentSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 重點段 ").append(accentSeen).append(" ===")
              .append(System.lineSeparator());
            for (String line : translated) {
                sb.append("  譯文：").append(line).append(System.lineSeparator());
            }
            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);
                boolean hit = used != null && i < used.length && used[i];
                boolean present = false;
                for (String line : translated) {
                    present |= line.contains(text);
                }
                sb.append("    [").append(i).append("] ")
                  .append(describe(i < styles.size() ? styles.get(i) : null))
                  .append("  「").append(text).append("」")
                  .append(hit ? "  已貼上"
                              : (present ? "  ★在譯文裡卻沒貼上" : "  譯文裡找不到"))
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }

    /**
     * 填回去的符號到底是什麼。
     *
     * <h2>為什麼需要這一欄</h2>
     * 聊天訊息的譯文偶爾會在 {@code {#}} 的位置炸出一個大空隙——
     * 「- +1 個　　　　　　未鑑定頭盔」那種。{@code {#}} 是連同<b>原本的字型與
     * 寬度</b>整段搬回去的，如果其中一個其實是排版位移字元，它在英文那邊剛好，
     * 換成中文之後前後的字寬不同，那個位移就把後面的東西推歪了。
     *
     * <p>先前的診斷只記得到重點段與查表結果，<b>看不到符號本身</b>，
     * 所以這個假設一直驗不了。這裡把每一個填回去的符號連同碼位與字型寫出來：
     * 私用區的是真圖示，未分配的第 13 平面就是位移。
     */
    public static void glyphs(List<LineParts.Piece> pool, String[] translated) {
        if (file == null || pool == null || pool.isEmpty()
                || glyphSeen >= GLYPH_LIMIT) {
            return;
        }
        StringBuilder id = new StringBuilder("glyphs");
        for (String line : translated) {
            id.append(line);
        }
        if (!already.add(id.toString())) {
            return;
        }
        boolean interesting = false;
        for (LineParts.Piece piece : pool) {
            interesting |= piece.text().codePoints().anyMatch(FlowedDebug::isOffset);
        }
        if (!interesting) {
            return;                            // 全是真圖示，沒什麼好看的
        }
        glyphSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 填回去的符號 ").append(glyphSeen).append(" ===")
              .append(System.lineSeparator());
            for (String line : translated) {
                sb.append("  譯文：").append(line).append(System.lineSeparator());
            }
            for (int i = 0; i < pool.size(); i++) {
                LineParts.Piece piece = pool.get(i);
                sb.append("    [").append(i).append("] ");
                piece.text().codePoints().forEach(cp -> sb
                        .append(String.format("U+%04X", cp))
                        .append(isOffset(cp) ? "(位移)" : "(圖示)")
                        .append(' '));
                sb.append(" font=")
                  .append(piece.style() == null ? "-" : piece.style().getFont())
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }

    /**
     * 多行區塊重新對齊的現場。
     *
     * <h2>為什麼需要</h2>
     * 聊天的系統訊息是好幾行擠在<b>一則</b>訊息裡，每一行前面各有一個
     * 照英文寬度算好的置中縮排。{@code realignRows} 就是為了逐行重算而寫的，
     * 但它在兩邊行數對不上時會<b>安靜地</b>退回舊路——而舊路只修得到第一個縮排。
     *
     * <p>回報回來的畫面正是「只有第一行歪掉、其餘各行原封不動」，
     * 跟退回舊路的症狀一模一樣；但到底是行數對不上、還是空白數對不上、
     * 又或者根本沒進到這裡，現有的診斷一個都答不出來。這裡把那三件事寫清楚。
     *
     * @param body 呼叫端排好的內容，見 {@code LineTranslator#realign}
     */
    public static void rows(String id, String body) {
        if (file == null || body == null || rowSeen >= ROW_LIMIT
                || !already.add("rows" + id)) {
            return;
        }
        rowSeen++;
        try {
            Files.writeString(file,
                    "=== 逐行對齊 " + rowSeen + " ===" + System.lineSeparator()
                    + body + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }

    /**
     * 這一條原文用得到的顏色，連同編號。
     *
     * <h2>為什麼一定要有</h2>
     * 譯文可以用 {@code {c1}}、{@code {c2}} 指定顏色（見
     * {@code LineTranslator#colourToken}）。但「第 1 個顏色是哪一個」是程式
     * 依原文片段的先後算出來的，譯者<b>看不到</b>——沒有這一欄，那個功能就只能
     * 靠試誤，等於沒有。
     *
     * <p>所以每翻一條就把調色盤寫出來：編號、色碼、粗體底線之類的裝飾，
     * 再附上譯文本身好對照是哪一條。
     */
    public static void palette(List<Style> colours, List<LineParts.Piece> runs,
                               String[] translated) {
        if (file == null || colours == null || colours.size() < 2
                || paletteSeen >= PALETTE_LIMIT) {
            return;                            // 只有一個顏色的沒什麼好挑
        }
        StringBuilder id = new StringBuilder("palette");
        for (String line : translated) {
            id.append(line);
        }
        if (!already.add(id.toString())) {
            return;
        }
        paletteSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 可用的顏色 ").append(paletteSeen).append(" ===")
              .append(System.lineSeparator());
            for (String line : translated) {
                sb.append("  譯文：").append(line).append(System.lineSeparator());
            }
            for (int i = 0; i < colours.size(); i++) {
                sb.append("    {c").append(i + 1).append("}  ")
                  .append(String.format("%-22s", describe(colours.get(i))))
                  .append("原文：").append(sample(colours.get(i), runs))
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }

    /**
     * 這個顏色在原文裡第一次出現時，那一段字長什麼樣。
     *
     * <h2>為什麼一定要附上</h2>
     * 只印色碼的話，譯者手上是「{c2} 是 #FFFFFF」——那還是得回頭猜哪一句是白的。
     * 附上原文之後就成了「{c2} 是 Grook{@code '}s Nest 那個白」，直接對得起來。
     */
    private static String sample(Style colour, List<LineParts.Piece> runs) {
        if (runs == null) {
            return "（不明）";
        }
        for (LineParts.Piece run : runs) {
            Style style = run.style() == null ? Style.EMPTY : run.style();
            if (!style.equals(colour)) {
                continue;
            }
            String text = run.text().strip();
            return text.length() > SAMPLE_LENGTH
                    ? "「" + text.substring(0, SAMPLE_LENGTH) + "…」" : "「" + text + "」";
        }
        return "（找不到）";
    }

    /** 原文樣本印這麼長就夠認得出是哪一句了。 */
    private static final int SAMPLE_LENGTH = 28;

    private static int paletteSeen = 0;

    /**
     * 調色盤那一欄的額度，跟其他欄分開算。
     *
     * <p>先前是 20，全被角色選單那一頁吃光——真正想看的聊天區塊排不進來。
     * 放寬到 60 之後又踩到同一件事：登入時的公告、每日獎勵、坐騎提醒一口氣
     * 把額度用完，等玩家進到討伐戰、想查一條招牌訊息的顏色，這一欄早就滿了。
     * 調色盤一條只佔幾行，值不了那個省。
     */
    private static final int PALETTE_LIMIT = 250;

    /**
     * 聊天訊息的逐行對齊，<b>額度跟名牌分開算</b>。
     *
     * <h2>為什麼要分</h2>
     * 兩者共用一份額度時，名牌永遠贏——漂浮名牌<b>每一個都是兩行</b>，
     * 走過一片草原就是幾十筆。實機回報的檔案裡 40 筆有 35 筆是名牌，
     * 而且每一筆都是「不動：空白數 原文=0 譯文=0」這種沒有資訊量的紀錄；
     * 真正想查的「洞穴完成」那一塊排在後面，一次都沒被寫進去。
     *
     * <p>額度先前從 12 加到 40 也沒用，因為名牌的數量本來就沒有上限。
     * 分開算才治本。
     *
     * @param miss 查不到譯文的原因；有值時代表<b>根本沒翻到</b>，
     *             那跟「翻了但沒對齊」是兩種病，混在一起就查不出來
     */
    public static void chatRows(String id, String body, String miss) {
        if (file == null || chatSeen >= CHAT_ROW_LIMIT
                || !already.add("chatRows" + id)) {
            return;
        }
        chatSeen++;
        try {
            Files.writeString(file,
                    "=== 聊天對齊 " + chatSeen + " ==="
                    + (miss == null ? "" : "（沒翻到：" + miss + "）")
                    + System.lineSeparator()
                    + "  原文：" + id.replace("\n", " ⏎ ") + System.lineSeparator()
                    + (body == null ? "" : body) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }

    private static int chatSeen = 0;

    /** 聊天訊息本來就不多，二十筆足夠涵蓋一輪任務。 */
    private static final int CHAT_ROW_LIMIT = 20;

    private static int rowSeen = 0;

    /**
     * 逐行對齊記幾筆。
     *
     * <p>先前是 12，而漂浮字的名牌<b>每一個都是兩行</b>——走過一片草原就把
     * 額度吃光了。使用者回報「洞穴完成還是沒對齊」，可是診斷檔裡連那一塊
     * 都沒出現，因為前面十二筆全是「Grook」「Tasim」的名牌。
     */
    private static final int ROW_LIMIT = 40;

    /** 未分配的第 13 平面＝排版位移；私用區＝真圖示。見 GlyphSplitter。 */
    private static boolean isOffset(int cp) {
        return com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)
                && !com.wynnchayuan.capture.GlyphSplitter.isPrivateUse(cp);
    }

    private static final int GLYPH_LIMIT = 12;

    private static int glyphSeen = 0;

    /** 重點段那一欄的額度，跟跨行查表分開算。 */
    private static final int ACCENT_LIMIT = 20;

    private static int accentSeen = 0;

    private static String describe(Style style) {
        if (style == null) {
            return "（無樣式）";
        }
        TextColor colour = style.getColor();
        String base = colour == null ? "繼承"
                : String.format("#%06X", colour.getValue() & 0xFFFFFF);
        return base + (style.isUnderlined() ? " 底線" : "")
                + (style.isBold() ? " 粗體" : "");
    }

    /**
     * 查不到的最多記幾筆。
     *
     * <h2>為什麼要分兩份名額</h2>
     * 一份 tooltip 裡的<b>詞條清單</b>（{@code Health {#}-{~} [{~}]} 那些）也會走
     * 跨行查表，而它們本來就不該有跨行條目——一件裝備就灌進十幾筆，
     * 把名額整個佔滿。連續兩次收到使用者的診斷檔，真正要查的那一段都因此排不進來。
     *
     * <p>所以「看起來像句子的」另外給一份名額。判斷看的是有沒有
     * {@code ". "} 或 {@code ": "}——會被自動斷行的長敘述與「名稱：說明」都有，
     * 詞條清單兩個都沒有。
     */
    private static final int MISS_LIMIT = 24;

    /** 見 {@link #MISS_LIMIT}。 */
    private static final int PROSE_MISS_LIMIT = 24;

    /**
     * 「名稱：說明」形狀的再另外給一份名額。
     *
     * <h2>為什麼要第三個桶子</h2>
     * Major ID 一份 tooltip 只有一兩段，卻跟幾十段技能敘述搶同一份名額。
     * 連續三次拿到的診斷檔，要查的那一段都因為名額被吃光而排不進來——
     * 每一次都得再請使用者重跑一遍遊戲。
     */
    private static final int LABELLED_MISS_LIMIT = 12;

    private static int missed = 0;
    private static int prose = 0;
    private static int labelled = 0;

    /** 這一段看起來是句子，而不是一串詞條。 */
    private static boolean looksLikeProse(String template) {
        String flat = TranslationStore.normalise(template);
        return flat.contains(". ") || flat.contains(": ");
    }

    /** 第一行長得像「名稱：說明」。 */
    private static boolean looksLabelled(String template) {
        int end = template.indexOf('\n');
        String first = end < 0 ? template : template.substring(0, end);
        int colon = first.indexOf(": ");
        return colon > 0 && colon <= 40;
    }

    /**
     * 「名稱：說明」那條路是<b>哪一半</b>失敗的。
     *
     * <h2>為什麼要記這個</h2>
     * 只知道「整段查不到」沒有用：名稱那半查不到、說明那半查不到、
     * 或兩半都查得到卻拼不起來——三種的修法完全不同，
     * 而先前的診斷三種長得一模一樣。把中間結果原樣印出來就分得開了。
     */
    private static void explain(StringBuilder sb, String template,
                                TranslationStore store) {
        String nl = System.lineSeparator();
        int end = template.indexOf('\n');
        String first = end < 0 ? template : template.substring(0, end);
        int colon = first.indexOf(": ");
        if (colon <= 0) {
            return;
        }
        String head = template.substring(0, colon);
        String rest = template.substring(colon + 2);
        sb.append("  -- 拆成「名稱：說明」之後 --").append(nl);
        sb.append("     名稱＝[").append(head).append(']').append(nl);
        sb.append("     名稱查表＝")
          .append(LineTranslator.lookup(head, store, false)).append(nl);
        sb.append("     說明的鍵＝[").append(TranslationStore.normalise(rest))
          .append(']').append(nl);
        sb.append("     說明查表＝").append(store.lookupFlat(rest)).append(nl);
        sb.append("     整段攤平查表＝").append(store.lookupFlat(template)).append(nl);
    }

    /**
     * 記一段<b>查不到</b>的跨行原文。
     *
     * <h2>為什麼非記不可</h2>
     * 先前只記成功的。於是檔案裡看不到 Major ID 時有兩種可能：跨行查表<b>根本沒被
     * 呼叫到</b>，或者呼叫了但查不到——這兩件事的修法完全不同，而診斷分不出來。
     * 連查不到也記下來，看一眼就知道是哪一種：檔案裡有這一段就是查了沒中
     * （語料的問題），完全沒有就是連進都沒進來（程式的問題）。
     *
     * <p>順便把攤平後的鍵原樣印出來，直接拿去跟譯文檔的 {@code src} 對，
     * 差一個空格還是差一個標點一眼就看得出來。
     */
    public static void miss(String template, TranslationStore store) {
        if (file == null || template == null) {
            return;
        }
        // 「名稱：說明」的走自己那份名額，而且多記「哪一半失敗」。
        boolean label = looksLabelled(template);
        boolean sentence = !label && looksLikeProse(template);
        int limit = label ? LABELLED_MISS_LIMIT
                : sentence ? PROSE_MISS_LIMIT : MISS_LIMIT;
        int used = label ? labelled : sentence ? prose : missed;
        if (used >= limit) {
            return;
        }
        if (!already.add("miss" + template)) {
            return;
        }
        int nth = label ? ++labelled : sentence ? ++prose : ++missed;
        String kind = label ? "（名稱：說明）" : sentence ? "（像句子）" : "（詞條）";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 查不到 ").append(kind)
              .append(nth).append(" ===")
              .append(System.lineSeparator());
            sb.append("  攤平後的鍵：").append(TranslationStore.normalise(template))
              .append(System.lineSeparator());
            for (String line : template.split("\\R", -1)) {
                sb.append("    原文：").append(line).append(System.lineSeparator());
            }
            if (label && store != null) {
                explain(sb, template, store);
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }

    /** 有選項的對話最多記幾筆。 */
    private static final int CHOICE_LIMIT = 6;

    private static int choicesSeen = 0;

    /**
     * 記一段<b>有選項</b>的對話原文。
     *
     * <p>遊戲把選項畫在對話框上面另一個框裡，但事件只給我們一整串文字——
     * 哪幾行是 NPC 說的、哪幾行是玩家可以選的，從事件本身看不出來。
     * 先把原始結構記下來，照真實資料實作才不會又猜錯。
     */
    public static void noteChoices(String text) {
        if (file == null || text == null || choicesSeen >= CHOICE_LIMIT
                || !already.add("choices" + text)) {
            return;
        }
        choicesSeen++;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 有選項的對話 ").append(choicesSeen).append(" ===")
              .append(System.lineSeparator());
            int i = 0;
            for (String line : text.split("\\R", -1)) {
                sb.append("  [").append(i++).append("] ").append(line)
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // 診斷絕不能反過來弄壞畫面
        }
    }
}
