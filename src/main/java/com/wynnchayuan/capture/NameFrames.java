package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 語料裡帶 {@code {u}} 的句型：拿來認出<b>別人的名字</b>夾在句子哪裡。
 *
 * <h2>為什麼需要</h2>
 * {@code {u}} 原本只認得<b>自己</b>的名字（見 {@link SelfNames}）。別的玩家出現在
 * 句子裡時——隊友給你加了增益、某人死掉的快訊、別人的力量之井——模板裡留著的是
 * 那個人的名字，語料那條 {@code {u} has given you {~} resistance and {~} strength.}
 * 永遠對不上，畫面就是整句英文（使用者 2026-10-17 的截圖）。
 *
 * <p>{@link Broadcasts} 那幾句炸彈廣播是寫死的正規式。這一類卻是<b>無上限的家族</b>：
 * 死亡快訊一種怪一句（見 {@code wynnchayuan-corpus-privacy-leaks}），寫死的話每收一批
 * 語料就要改一次程式。所以句型不寫在程式裡，<b>語料本身就是句型</b>——任何一條帶
 * 一個 {@code {u}} 的鍵都是一個「名字在這裡」的框，翻譯團隊補一條就多認一句。
 *
 * <h2>怎麼認</h2>
 * <ol>
 *   <li>載入時把每條鍵併句（{@link com.wynnchayuan.translate.TranslationStore#unwrap}）、
 *       剝掉行首的圖示，在 {@code {u}} 切成前後兩截；照<b>後半截</b>建索引。</li>
 *   <li>一行字先照平常的方式算出模板，同樣併句、剝行首。從每個位置往後切一刀，
 *       後半截剛好是某個框的後半截、前面又是那個框的前半截，中間那一段就是名字的候選。</li>
 *   <li>候選要像名字（見 {@link #nameLike}），然後回到攤平文字照那個框的形狀定位，
 *       名字的位置交給 {@link LineParts#of(StyledText, Broadcasts.Spans)} 收成 {@code {u}}。
 *       收完的模板必須跟框<b>逐字相同</b>，否則不算。</li>
 * </ol>
 * 名字是照<b>位置</b>收的，不會拿去查表——長得像技能名、地名、怪物名都原樣填回去。
 *
 * <h2>只錨在已知的句子上</h2>
 * 框的其餘部分要整段吻合，名字之外沒有任何彈性；名字本身還要過 {@link #nameLike}。
 * 而且呼叫端只在<b>整句查不到</b>時才來問，語料裡本來就有的句子不會被拆開。
 *
 * <h2>隱私</h2>
 * 認出來的模板<b>就是語料裡的那條鍵</b>（只差行首圖示），收進 {@code captured.json} 的
 * 是框、不是名字。見 {@code CaptureListener#chatTemplate}。
 *
 * <p>不可變，建好之後兩條執行緒（算繪與收集端的背景整理）都可以讀。
 */
public final class NameFrames {

    /** 什麼都不認得的那一份：還沒載入語料時用。 */
    public static final NameFrames EMPTY = new NameFrames(Map.of(), Map.of(), 0);

    /** 一個框：併句、剝掉行首之後在 {@code {u}} 切開的前後兩截。 */
    private record Frame(String head, String tail) {
        int anchor() {
            return head.length() + tail.length();
        }

        String core() {
            return head + GlyphSplitter.PLAYER_PLACEHOLDER + tail;
        }
    }

    /** 後半截 → 框。後半截是空的（名字在句尾）放 {@link #byHead}。 */
    private final Map<String, List<Frame>> byTail;

    /** 名字在句尾的框，照前半截索引。 */
    private final Map<String, List<Frame>> byHead;

    private final int size;

    /** 攤平文字用的正規式，照框快取。兩條執行緒都會用，所以是 ConcurrentHashMap。 */
    private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    private NameFrames(Map<String, List<Frame>> byTail, Map<String, List<Frame>> byHead, int size) {
        this.byTail = byTail;
        this.byHead = byHead;
        this.size = size;
    }

    /** 認得幾個框（併句之後相同的算一個）。 */
    public int size() {
        return size;
    }

    /**
     * 從語料的鍵建出來。
     *
     * <p>只收<b>剛好一個</b> {@code {u}}、而且名字旁邊是實字的鍵——名字緊貼著另一個
     * 佔位符（{@code {u}{~}}）的話，分不出名字在哪裡結束。
     */
    public static NameFrames of(Collection<String> keys) {
        Map<String, List<Frame>> tails = new HashMap<>();
        Map<String, List<Frame>> heads = new HashMap<>();
        Set<String> seen = new java.util.HashSet<>();
        int count = 0;
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            int first = key.indexOf(GlyphSplitter.PLAYER_PLACEHOLDER);
            if (first < 0 || key.indexOf(GlyphSplitter.PLAYER_PLACEHOLDER, first + 1) >= 0) {
                continue;
            }
            String core = core(key);
            int at = core.indexOf(GlyphSplitter.PLAYER_PLACEHOLDER);
            if (at < 0 || !seen.add(core)) {
                continue;
            }
            Frame frame = new Frame(core.substring(0, at),
                                    core.substring(at + GlyphSplitter.PLAYER_PLACEHOLDER.length()));
            if (!anchored(frame)) {
                continue;
            }
            if (frame.tail().isEmpty()) {
                heads.computeIfAbsent(frame.head(), k -> new ArrayList<>(1)).add(frame);
            } else {
                tails.computeIfAbsent(frame.tail(), k -> new ArrayList<>(1)).add(frame);
            }
            count++;
        }
        return new NameFrames(Map.copyOf(tails), Map.copyOf(heads), count);
    }

    /**
     * 這一行是不是某個框，名字在攤平文字的哪裡。
     *
     * @param template 這一行照平常算出來的模板（{@link LineParts#of(StyledText)}），
     *                 呼叫端多半已經算好了，傳進來省一次
     * @return 名字的位置；不是任何一個框就回 {@code null}
     */
    public Broadcasts.Spans find(StyledText line, String template) {
        // 模板裡已經有 {u}（自己的帳號名或暱稱被 SelfNames 收掉了）也要找：
        // Wynntils 把名字改寫成「帳號/暱稱」，畫面上是「Green_teaTW/RichaCha has given
        // you …」，帳號那一半是自己、整個名字卻是一段——模板成了「{u}/RichaCha …」，
        // 整句查不到，先前在這裡直接放棄（實機 2026-10-17 回報）。名字那一段整個重收
        // 成一個 {u}；名字之外如果還有自己的名字，收完的模板會多一個 {u}，對不上框，
        // 下面那一關會擋掉。
        if (line == null || template == null || size == 0) {
            return null;
        }
        List<Frame> candidates = candidates(core(template));
        if (candidates.isEmpty()) {
            return null;
        }
        String flat = LineParts.flat(line);
        for (Frame frame : candidates) {
            Matcher m = pattern(frame).matcher(flat);
            if (!m.matches()) {
                continue;
            }
            int from = m.start("user");
            int to = m.end("user");
            while (from < to && Character.isWhitespace(flat.charAt(from))) {
                from++;
            }
            while (to > from && Character.isWhitespace(flat.charAt(to - 1))) {
                to--;
            }
            if (!nameLike(flat.substring(from, to))) {
                continue;
            }
            Broadcasts.Spans spans = new Broadcasts.Spans(from, to, -1, -1);
            // 收完必須跟框逐字相同：攤平文字的正規式比模板寬鬆（空白、圖示），
            // 這一關把「形狀像、其實不是」的擋掉。
            if (core(LineParts.of(line, spans).template()).equals(frame.core())) {
                return spans;
            }
        }
        return null;
    }

    /**
     * 模板夾得進哪些框，最長的（錨點最多的）排前面。
     *
     * @param core 已經併句、剝掉行首的模板
     */
    private List<Frame> candidates(String core) {
        List<Frame> out = new ArrayList<>(2);
        if (core.isEmpty() || core.length() > MAX_TEMPLATE) {
            return out;
        }
        for (int cut = 1; cut < core.length(); cut++) {
            List<Frame> frames = byTail.get(core.substring(cut));
            if (frames == null) {
                continue;
            }
            for (Frame frame : frames) {
                if (frame.head().length() < cut && core.startsWith(frame.head())
                        && templateNameLike(core.substring(frame.head().length(), cut))) {
                    out.add(frame);
                }
            }
        }
        for (int cut = 1; cut < core.length(); cut++) {
            List<Frame> frames = byHead.get(core.substring(0, cut));
            if (frames == null) {
                continue;
            }
            for (Frame frame : frames) {
                if (templateNameLike(core.substring(cut))) {
                    out.add(frame);
                }
            }
        }
        out.sort((a, b) -> Integer.compare(b.anchor(), a.anchor()));
        return out;
    }

    /** 太長的不是廣播（整段說明文字），不必逐刀切。 */
    private static final int MAX_TEMPLATE = 400;

    /**
     * 併句、壓空白，再剝掉行首的圖示與空白。
     *
     * <p>同一句死亡快訊在聊天是 {@code {#} 名字 …}、在標題是 {@code {#}{#}名字 …}、
     * 在副標題是 {@code 名字 …}——行首長什麼樣子跟句子是哪一句無關。查表那一關
     * 本來就會剝行首（{@code LineTranslator#lookupTrimmed}），認框也照做。
     */
    static String core(String text) {
        String s = com.wynnchayuan.translate.TranslationStore.unwrap(text);
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = 0;
        while (at < s.length()) {
            if (s.startsWith(glyph, at)) {
                at += glyph.length();
            } else if (Character.isWhitespace(s.charAt(at))) {
                at++;
            } else {
                break;
            }
        }
        return s.substring(at);
    }

    /**
     * 框夠不夠格：句子本身要有幾個字（不然什麼都夾得進去），名字旁邊要是實字。
     */
    private static boolean anchored(Frame frame) {
        String literal = (frame.head() + frame.tail()).replaceAll("\\{[^}]*}", "");
        long letters = literal.chars().filter(Character::isLetter).count();
        if (letters < MIN_ANCHOR_LETTERS) {
            return false;
        }
        String tail = frame.tail().stripLeading();
        if (!frame.tail().isEmpty() && (tail.isEmpty() || tail.charAt(0) == '{')) {
            return false;                      // {u}{~}、{u} {#}：名字到哪裡結束分不出來
        }
        String head = frame.head().stripTrailing();
        // 名字在句尾時，前面緊貼的必須是實字（「Thank {u}」）
        return !frame.tail().isEmpty() || (!head.isEmpty() && head.charAt(head.length() - 1) != '}');
    }

    /** 見 {@link #anchored}：「has died」九個字母，「's Bird」四個。再短就什麼都夾得進去。 */
    private static final int MIN_ANCHOR_LETTERS = 4;

    /**
     * 照框的形狀拼一個比對<b>攤平文字</b>的正規式。
     *
     * <p>攤平文字裡沒有圖示（{@link LineParts#flat}），換行跟空白一樣算——伺服器
     * 折在哪裡都對得上。數值照 {@link GlyphSplitter} 的寫法，地名只要求有字。
     */
    private Pattern pattern(Frame frame) {
        return patterns.computeIfAbsent(frame.core(), NameFrames::compile);
    }

    private static Pattern compile(String core) {
        StringBuilder re = new StringBuilder("^\\s*");
        StringBuilder literal = new StringBuilder();
        int at = 0;
        while (at < core.length()) {
            char c = core.charAt(at);
            String token = c == '{' ? placeholderAt(core, at) : null;
            if (token == null && !Character.isWhitespace(c)) {
                literal.append(c);
                at++;
                continue;
            }
            if (!literal.isEmpty()) {
                re.append(Pattern.quote(literal.toString()));
                literal.setLength(0);
            }
            if (token == null) {
                re.append("\\s*");             // 空白（含伺服器的折行）
                at++;
                continue;
            }
            switch (token) {
                case "{u}" -> re.append("(?<user>[^\\n]+?)");
                case "{~}" -> re.append("\\d+(?:[.,]\\d+)*%?");
                case "{p}" -> re.append("[^\\n]+?");
                default -> re.append("\\s*");  // {#}：攤平文字裡沒有圖示
            }
            at += token.length();
        }
        if (!literal.isEmpty()) {
            re.append(Pattern.quote(literal.toString()));
        }
        re.append("\\s*$");
        return Pattern.compile(re.toString());
    }

    /** 這個位置是不是模板佔位符；是就回傳它本身。 */
    private static String placeholderAt(String text, int at) {
        for (String token : TOKENS) {
            if (text.startsWith(token, at)) {
                return token;
            }
        }
        return null;
    }

    private static final String[] TOKENS = {
            GlyphSplitter.PLAYER_PLACEHOLDER, GlyphSplitter.NUMBER_PLACEHOLDER,
            GlyphSplitter.PLACE_PLACEHOLDER, GlyphSplitter.GLYPH_PLACEHOLDER};

    /**
     * 模板裡的那一段像不像名字：名字裡的數字與地名已經被收成 {@code {~}}、{@code {p}}，
     * 那兩種放行，其餘照 {@link #nameLike} 的字元。
     */
    private static boolean templateNameLike(String slice) {
        if (slice.isBlank() || slice.length() > MAX_NAME * 2
                || slice.contains(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            return false;
        }
        // {u} 是被收掉的自己的名字（「{u}/RichaCha」），當成名字的一部分
        String bare = slice.replace(GlyphSplitter.NUMBER_PLACEHOLDER, "0")
                           .replace(GlyphSplitter.PLACE_PLACEHOLDER, "Place")
                           .replace(GlyphSplitter.PLAYER_PLACEHOLDER, "Name");
        return NAME_CHARS.matcher(bare.strip()).matches();
    }

    /**
     * 像不像一個玩家名。
     *
     * <ul>
     *   <li>帳號名是英數與底線；Wynntils 會把暱稱寫成「帳號/暱稱」，暱稱可以有空白。</li>
     *   <li>不收冒號：玩家發言是「名字: 內容」。不收句號、逗號、驚嘆號——那是句子不是名字。</li>
     *   <li>最多三個詞（斜線後面那半另算）：「JC grindeando」「hp giving stick」都過，
     *       一整句話過不了。</li>
     *   <li>沒有斜線時，開頭是 The／Your 這種虛詞的不算——「Your pet has died」
     *       的主詞不是一個人。</li>
     * </ul>
     */
    public static boolean nameLike(String name) {
        if (name == null) {
            return false;
        }
        String s = name.strip();
        if (s.isEmpty() || s.length() > MAX_NAME || !NAME_CHARS.matcher(s).matches()) {
            return false;
        }
        int slash = s.indexOf('/');
        if (slash != s.lastIndexOf('/')) {
            return false;
        }
        String shown = slash < 0 ? s : s.substring(slash + 1).strip();
        String account = slash < 0 ? null : s.substring(0, slash).strip();
        if (shown.isEmpty() || (account != null && (account.isEmpty() || account.contains(" ")))) {
            return false;
        }
        String[] words = shown.split(" +");
        if (words.length > MAX_WORDS) {
            return false;
        }
        return slash >= 0 || !NOT_A_NAME.contains(words[0].toLowerCase(Locale.ROOT));
    }

    /** 見 {@link #nameLike}：英數、底線、空白、斜線、連字號，頭尾要是字。 */
    private static final Pattern NAME_CHARS =
            Pattern.compile("[\\p{L}\\p{N}_](?:[\\p{L}\\p{N}_ /\\-]*[\\p{L}\\p{N}_])?");

    /** 帳號名最多 16 字，加上斜線與暱稱。 */
    private static final int MAX_NAME = 40;

    private static final int MAX_WORDS = 3;

    /** 見 {@link #nameLike}：句子開頭常見、名字開頭不會是的詞。 */
    private static final Set<String> NOT_A_NAME = Set.of(
            "the", "a", "an", "you", "your", "yours", "this", "that", "these", "those",
            "it", "its", "he", "she", "they", "we", "his", "her", "their", "our", "my",
            "there", "here", "everyone", "someone", "nobody", "all", "each");
}
