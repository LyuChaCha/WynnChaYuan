package com.wynnchayuan.capture;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 一行遊戲文字拆解後的樣子：查詢用的模板，加上還原時要填回去的碎片。
 *
 * <p>翻譯的來回是這樣走的：
 *
 * <pre>
 *   原文  ◆Troms Citizen — Total Damage: 400%
 *   模板  {#}{p} Citizen — Total Damage: {~}      ← 拿這個去查字典
 *   查到  {#}{p} 市民 — 總傷害: {~}
 *   還原  ◆Troms 市民 — 總傷害: 400%              ← 符號、地名、數值填回原位
 * </pre>
 *
 * <p>三種佔位符都<b>連同各自的樣式一起記下來</b>：符號要帶原本的自訂字型才顯示得出來，
 * 地名與數值常常是另一個顏色。
 *
 * <h2>這是模板的唯一產生處</h2>
 * 收集端與顯示端<b>必須</b>算出一模一樣的模板，否則查表會靜默落空。
 * 所以 {@link GlyphSplitter#toTemplate} 也是轉呼叫這裡，不另外實作一份。
 */
public record LineParts(
        String template,
        List<Piece> glyphs,
        List<Piece> places,
        List<Piece> numbers,
        /** 玩家自己的名字。參數化之後一條譯文所有人通用，見 GlyphSplitter#PLAYER_PLACEHOLDER。 */
        List<Piece> users,
        /**
         * 樣式與整行不同的文字片段，例如被畫上底線、單獨換色的技能名。
         *
         * <p>重建譯文時整行套用同一個 {@code textStyle}，行內的樣式變化會被抹平——
         * {@code Reduce the Mana cost of Bash.} 裡的 {@code Bash} 原本帶底線與專屬
         * 顏色，譯文出來就成了一片平的。
         *
         * <p>技能名、地名這類詞照慣例<b>保持英文</b>，也就是原樣出現在譯文裡，
         * 所以不必請譯者標記——比對得到就把原本的 {@link Style} 貼回去。
         * 這樣連非原版的自訂顏色都對，因為搬的是樣式物件本身而不是某個顏色碼。
         */
        List<Piece> accents,
        Style textStyle,

        /**
         * 這一行的<b>每一段</b>文字，含它自己的樣式，還沒篩過。
         *
         * <p>{@link #accents} 是篩過的結果，篩的基準是這一行自己的主樣式。
         * 跨行重建時基準會變（見 {@link #accentsAgainst}），那時候需要原始的清單
         * 重挑一次——只留篩過的結果就挑不回來了。
         */
        List<Piece> runs) {

    /** 一個要填回去的碎片：文字加上它原本的樣式。 */
    public record Piece(String text, Style style) {}

    /**
     * 遊戲有時候<b>真的印出「Player」這四個字</b>，而不是玩家的名字。
     *
     * <h2>畫面上是什麼樣子</h2>
     * 新手任務 King's Recruit 的幾句台詞，Wynncraft 送過來就是字面的
     * {@code "Alright! Let's go, Player."}、{@code "Great job, Player!"}——
     * 同一個任務裡別的台詞卻帶著真名（{@code "Hey, Arclass_!"}），
     * 同一次遊玩、同一段劇情，兩種都有。
     *
     * <p>語料裡那幾句<b>早就翻好了</b>，鍵是 {@code "Alright! Let's go, {u}."}。
     * 只因為畫面上是「Player」而不是名字，模板對不起來，於是整句掉回英文——
     * 而且是新手任務，每個新玩家都會撞到。使用者回報的正是這個。
     *
     * <p>做法是把那個字當成玩家名收成 {@code {u}}，並且補一個內容就是
     * 「Player」的片段——填回去畫面上還是「Player」，跟遊戲一致。
     *
     * <h2>兩道限制</h2>
     * <ul>
     *   <li>只在<b>整行查不到</b>時才走這條（呼叫端負責）。語料裡有五條
     *       正經的介面字帶著這個字（{@code Player Slot}、{@code Max Player Count}、
     *       {@code Looking for a Player...}），它們自己查得到，走不到這裡。</li>
     *   <li>本來就有 {@code {u}} 的行不動。混在一起的話兩種來源的順序會錯開，
     *       名字就填到別的位置去了。</li>
     * </ul>
     */
    public LineParts namingPlayer() {
        if (!users.isEmpty() || !LITERAL_PLAYER.matcher(template).find()) {
            return this;
        }
        Style style = runs.isEmpty() || runs.get(0).style() == null
                ? Style.EMPTY : runs.get(0).style();
        List<Piece> named = new ArrayList<>(users);
        StringBuilder out = new StringBuilder();
        Matcher m = LITERAL_PLAYER.matcher(template);
        int from = 0;
        while (m.find()) {
            out.append(template, from, m.start())
               .append(GlyphSplitter.PLAYER_PLACEHOLDER);
            named.add(new Piece(LITERAL_NAME, style));
            from = m.end();
        }
        out.append(template.substring(from));
        return new LineParts(out.toString(), glyphs, places, numbers,
                             named, accents, textStyle, runs);
    }

    /** 見 {@link #namingPlayer}：整個字才算，{@code Players}、{@code Playerbase} 不算。 */
    private static final java.util.regex.Pattern LITERAL_PLAYER =
            java.util.regex.Pattern.compile("\\bPlayer\\b");

    private static final String LITERAL_NAME = "Player";

    /**
     * 拆解一行。
     *
     * <p>逐個 part 走，才能把每個碎片的樣式對應到它所在的片段——
     * 先把整行拼成字串再套正規表示式的話，樣式資訊就丟掉了。
     */
    /**
     * 這段文字開頭（或結尾）連續的符號字元。
     *
     * <p>只認符號碼位，空白不算——空白是內容的一部分，抽掉會讓
     * {@code "Tailoring "} 變成 {@code "Tailoring"}，兩者的語料鍵不同。
     */
    /**
     * 這段文字開頭連續的符號字元。
     *
     * <p>只認符號碼位，空白不算——空白是內容的一部分，抽掉會讓
     * {@code "Tailoring "} 變成 {@code "Tailoring"}，兩者的語料鍵不同。
     */
    /** 本機玩家的帳號名稱；取不到就回傳 null。 */
    private static String localPlayerName() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            String name = mc == null || mc.getUser() == null ? null : mc.getUser().getName();
            return name == null || name.isBlank() ? null : name;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String leadingGlyphs(String raw) {
        int n = 0;
        while (n < raw.length()) {
            int cp = raw.codePointAt(n);
            if (!GlyphSplitter.isGlyphCodePoint(cp)) {
                break;
            }
            n += Character.charCount(cp);
        }
        return raw.substring(0, n);
    }

    /**
     * 結尾連續的符號字元。
     *
     * @param floor 不要越過這個位置——開頭已經抽走的部分不能再被抽一次
     */
    private static String trailingGlyphs(String raw, int floor) {
        int end = raw.length();
        while (end > floor) {
            int cp = raw.codePointBefore(end);
            if (!GlyphSplitter.isGlyphCodePoint(cp)) {
                break;
            }
            end -= Character.charCount(cp);
        }
        return raw.substring(end);
    }

    public static LineParts of(StyledText line) {
        StringBuilder tmpl = new StringBuilder();
        List<Piece> glyphs = new ArrayList<>();
        List<Piece> places = new ArrayList<>();
        List<Piece> numbers = new ArrayList<>();
        List<Piece> users = new ArrayList<>();
        List<Piece> runs = new ArrayList<>();

        StringBuilder glyphRun = new StringBuilder();
        Style glyphRunStyle = null;

        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();

            if (GlyphSplitter.isGlyphPart(part)) {
                // 只合併「樣式相同」的連續符號。
                // 一行裡常常混用多種字型（space 排版 / banner/box 外框 /
                // tooltip/divider 分隔線…），全部併成一段只會套用第一個字型，
                // 其餘字元找不到對應字形就變成方框。
                if (!glyphRun.isEmpty() && !java.util.Objects.equals(glyphRunStyle, style)) {
                    glyphs.add(new Piece(glyphRun.toString(), glyphRunStyle));
                    glyphRun.setLength(0);
                }
                if (glyphRun.isEmpty()) {
                    glyphRunStyle = style;
                    tmpl.append(GlyphSplitter.GLYPH_PLACEHOLDER);
                }
                glyphRun.append(raw);
                continue;
            }
            if (!glyphRun.isEmpty()) {
                glyphs.add(new Piece(glyphRun.toString(), glyphRunStyle));
                glyphRun.setLength(0);
            }

            // 文字片段的<b>頭尾</b>可能黏著排版偏移字元，例如
            // {@code "Jeweling" + U+D0004 + U+D0034}。那些是排版寬度，
            // 先前被 stripGlyphChars 直接丟掉——譯文一換上去，間隔就整個消失。
            //
            // 抽成 {@code {#}} 交給 glyphs 保管，還原時才填得回去。
            // 只處理頭尾：夾在句子中間的圖示是內容的一部分，本來就不該進語料的鍵，
            // 而且抽出來會讓 {@code {#}} 卡在模板中間，既有的譯文全部對不上。
            String lead = leadingGlyphs(raw);
            // 從 lead 之後才往回找，整段都是符號時兩邊才不會重疊
            String tail = trailingGlyphs(raw, lead.length());
            String body = raw.substring(lead.length(), raw.length() - tail.length());

            if (!lead.isEmpty()) {
                tmpl.append(GlyphSplitter.GLYPH_PLACEHOLDER);
                glyphs.add(new Piece(lead, style));
            }
            String text = GlyphSplitter.stripGlyphChars(body);
            if (!text.isBlank()) {
                runs.add(new Piece(text, style));
            }
            appendParametrized(text, style, tmpl, places, numbers, users);
            if (!tail.isEmpty()) {
                tmpl.append(GlyphSplitter.GLYPH_PLACEHOLDER);
                glyphs.add(new Piece(tail, style));
            }
        }
        if (!glyphRun.isEmpty()) {
            glyphs.add(new Piece(glyphRun.toString(), glyphRunStyle));
        }

        Style textStyle = mainStyle(runs);
        return new LineParts(
                tmpl.toString().strip(),
                List.copyOf(glyphs),
                List.copyOf(places),
                List.copyOf(numbers),
                List.copyOf(users),
                accents(runs, textStyle),
                textStyle,
                List.copyOf(runs));
    }

    /**
     * 重新挑一次重點段，這次拿<b>別的</b>主樣式來比。
     *
     * <h2>為什麼跨行時要重挑</h2>
     * {@link #accents} 是拿<b>那一行自己的</b>主樣式篩的。單獨看一行沒問題，
     * 但一整段被 tooltip 寬度切成好幾行之後，每一行的「主樣式」各自不同：
     *
     * <pre>
     *   Increase your maximum          ← 整行灰色，灰是主樣式
     *   Mana Bank ◈ by +30.            ← 「Mana Bank」比後面長，藍變成主樣式
     * </pre>
     *
     * <p>於是藍色那一段在自己那行被判定為「跟主樣式相同」而丟掉，
     * 譯文重建時沒有東西可以把藍色貼回去——畫面上 Mana Bank 變成白的。
     * 同一件事也發生在 Major ID：名稱比同一行露出的說明長時，說明那半的灰色
     * 被丟掉，整段敘述就套上了標題的顏色。
     *
     * <p>所以跨行重建時改用<b>整段</b>的主樣式重挑一次。
     */
    public static List<Piece> accentsAgainst(List<Piece> runs, Style textStyle) {
        return accents(runs, textStyle);
    }

    /**
     * 整行文字的主樣式：<b>涵蓋字數最多</b>的那一個。
     *
     * <p>先前取的是「第一段真正文字」。句子以帶樣式的詞開頭時那就整個反了——
     * {@code Bash deals damage} 會讓整行都被畫上底線，剩下的才是例外。
     */
    private static Style mainStyle(List<Piece> runs) {
        Style best = null;
        int bestLen = -1;
        for (Piece candidate : runs) {
            int len = 0;
            for (Piece other : runs) {
                if (java.util.Objects.equals(candidate.style(), other.style())) {
                    len += other.text().length();
                }
            }
            if (len > bestLen) {
                bestLen = len;
                best = candidate.style();
            }
        }
        return best == null ? Style.EMPTY : best;
    }

    /**
     * 樣式與主樣式不同、而且<b>認得出來</b>的片段。
     *
     * <p>太短的不收：一兩個字元在譯文裡到處都比對得到，貼錯位置比沒有樣式更糟。
     * 純標點、純數字也不收——數字走 {@code {~}} 那條路，本來就會連樣式一起填回。
     */
    private static List<Piece> accents(List<Piece> runs, Style textStyle) {
        List<Piece> out = new ArrayList<>();
        for (Piece run : runs) {
            String text = run.text().strip();
            if (java.util.Objects.equals(run.style(), textStyle) || text.isEmpty()) {
                continue;
            }
            // 一般的詞要夠長才收——短的在譯文裡到處都比對得到，貼錯位置更糟。
            // 但<b>符號</b>不一樣：「✖」只有一個字元，卻是整行唯一的紅色，
            // 而且它原樣留在譯文裡，比對不會出錯。先前被長度擋掉，
            // 於是 ✖ 跟後面的標籤併成一段，紅色就沒了。
            boolean word = text.length() >= MIN_ACCENT_LENGTH && GlyphSplitter.hasLetter(text);
            if (word || isSymbolRun(text)) {
                out.add(new Piece(text, run.style()));
            }
        }
        return List.copyOf(out);
    }

    /**
     * 整段都是符號（沒有字母也沒有數字），例如 {@code ✖}、{@code ✦}、{@code ⚔}。
     *
     * <p><b>成對的括號不算。</b>括號自己不帶意義，它的顏色只有連同被包住的
     * 內容一起才成立；而被包住的那半是要翻譯的，翻完就比對不到、貼不回去。
     * 於是一整組只剩兩個有顏色的括號——使用者回報的
     * 「{@code [+3 獎勵抽數]} 裡只有 {@code [} 是綠色的」就是這樣來的。
     * 整組都沒有顏色，比只剩括號有顏色好。
     *
     * <p>{@code ✖}、{@code ✔}、{@code ⚔} 這些不受影響——那些單獨出現就是
     * 一個完整的狀態，本來就是這條規則要救的對象。
     */
    private static boolean isSymbolRun(String text) {
        boolean meaningful = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                return false;
            }
            if (BRACKETS.indexOf(c) < 0) {
                meaningful = true;
            }
        }
        return meaningful;
    }

    /** 見 {@link #isSymbolRun}：這些自己不成一個意思，不單獨收成重點段。 */
    private static final String BRACKETS = "[]()<>{}【】（）「」";

    /** 見 {@link #accents}。三個字元大約是最短的有意義單字。 */
    private static final int MIN_ACCENT_LENGTH = 3;

    /**
     * 把一段文字裡的地名與數字換成佔位符，同時記下原文與樣式。
     *
     * <p>地名先比對：它可能含數字（少見但存在），先換成 {@code {p}} 就不會被拆開。
     */
    private static void appendParametrized(String text, Style style, StringBuilder tmpl,
                                           List<Piece> places, List<Piece> numbers,
                                           List<Piece> users) {
        // 玩家名字最先抽掉。它可能長得像地名或含數字，先處理才不會被拆散。
        String self = localPlayerName();
        if (self != null) {
            int at = text.indexOf(self);
            if (at >= 0) {
                appendParametrized(text.substring(0, at), style, tmpl, places, numbers, users);
                tmpl.append(GlyphSplitter.PLAYER_PLACEHOLDER);
                users.add(new Piece(self, style));
                appendParametrized(text.substring(at + self.length()),
                        style, tmpl, places, numbers, users);
                return;
            }
        }
        Matcher place = PlaceNames.matcher(text);
        int from = 0;
        if (place != null) {
            while (place.find()) {
                appendNumbers(text.substring(from, place.start()), style, tmpl, numbers);
                tmpl.append(GlyphSplitter.PLACE_PLACEHOLDER);
                places.add(new Piece(place.group(), style));
                from = place.end();
            }
        }
        appendNumbers(text.substring(from), style, tmpl, numbers);
    }

    private static void appendNumbers(String text, Style style,
                                      StringBuilder tmpl, List<Piece> numbers) {
        Matcher m = GlyphSplitter.numberMatcher(text);
        int from = 0;
        while (m.find()) {
            tmpl.append(text, from, m.start());
            tmpl.append(GlyphSplitter.NUMBER_PLACEHOLDER);
            numbers.add(new Piece(m.group(), style));
            from = m.end();
        }
        tmpl.append(text, from, text.length());
    }
}
