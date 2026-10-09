package com.wynnchayuan.translate;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wynnventory（交易市場價格、獎勵池）的文字也翻。
 *
 * <h2>它的字從哪裡來</h2>
 * 兩條路，所以這裡也是兩塊：
 * <ol>
 *   <li><b>語言檔的鍵</b>——價格框的標籤（{@code Lowest: }）、獎勵畫面的按鈕、設定畫面、
 *       聊天提示、按鍵名稱，全部是 {@code Component.translatable("…wynnventory…")}。
 *       它只附英文一份語言檔，而我們的譯文語言又跟遊戲語言無關（多數人遊戲開英文），
 *       所以不能靠「替它補一份語言檔」。做法是把遊戲的 {@link Language} 包一層
 *       （{@link #wrap}）：帶 {@code wynnventory} 的鍵先照它自己的英文查我們的表。</li>
 *   <li><b>寫死的字</b>——價格框第一行的物品名、{@code No data yet.}、預測價格底下
 *       一項一項的數值名、獎勵池的意象清單。這些在它畫價格框之前改
 *       （{@link #lines}，掛在 {@code RenderUtils#drawTooltip} 上）。</li>
 * </ol>
 *
 * <h2>按住 Shift 看原文</h2>
 * 跟物品名稱同一個開關（「按住 Shift 暫時換成另一種」）。語言那一層有個麻煩：
 * {@code TranslatableContents} 會把查到的字記在自己身上，認的是「{@link Language}
 * 還是不是同一個物件」。所以開關一變（{@link #tick}），就重新包一個<b>新的</b>物件塞回去
 * （{@link #refresh}），所有畫面上的字下一次畫的時候自己重查——不必去找它們。
 *
 * <h2>沒裝的人</h2>
 * {@link #installed} 是假的時候三個入口都原樣放行：不包語言、不掛 tick、
 * 設定畫面也不出現那一列。
 *
 * <p>譯文在 {@code scoped/wynnventory.json}，鍵是 Wynnventory 的<b>英文原句</b>
 * （不是它的語言鍵——那樣語料檔才跟其他檔一個樣，validate 也照常檢查）。
 */
public final class WynnventoryBridge {

    private WynnventoryBridge() {}

    /** {@code scoped/wynnventory.json}，見 {@link FileIndex#SCOPED}。 */
    public static final String SCOPE = "wynnventory";

    private static volatile Boolean installed;

    /**
     * 現在要不要翻：開關開著、而且沒有按住 Shift。
     *
     * <p>語言那一層可能在別的執行緒被問到（資源重載），所以不在那裡讀鍵盤與設定，
     * 只讀這個旗子；旗子由 {@link #tick} 在主執行緒更新。
     */
    private static volatile boolean active;

    private static boolean ticked;
    private static int seenGeneration;

    /** 有沒有裝 Wynnventory。 */
    public static boolean installed() {
        Boolean known = installed;
        if (known == null) {
            boolean found;
            try {
                found = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("wynnventory");
            } catch (Throwable t) {
                found = false;                 // 測試環境沒有 loader
            }
            installed = known = found;
        }
        return known;
    }

    /** 給測試用。 */
    static void pretend(boolean isInstalled, boolean isActive) {
        installed = isInstalled;
        active = isActive;
    }

    public static boolean active() {
        return active;
    }

    // ------------------------------------------------------------ 每一格

    /**
     * 每個 client tick 叫一次：開關、Shift、語料有沒有換過。有變就讓畫面上的字重查。
     *
     * @param enabled    設定裡的「Wynnventory 翻譯」
     * @param peeking    正按著 Shift 看原文（已經算進「按住 Shift」那個開關）
     * @param generation 語料的版本，見 {@code TranslationStore#generation}——換譯文語言、
     *                   背景同步更新都會動到它
     */
    public static void tick(boolean enabled, boolean peeking, int generation) {
        if (!installed()) {
            return;
        }
        boolean now = enabled && !peeking;
        if (ticked && now == active && generation == seenGeneration) {
            return;
        }
        ticked = true;
        active = now;
        seenGeneration = generation;
        refresh();
    }

    /** 換一個新的外殼，讓所有 {@code TranslatableContents} 下一次自己重查。 */
    private static void refresh() {
        try {
            Language.inject(wrap(Language.getInstance()));
        } catch (Throwable t) {
            // 換不了就算了：字會停在上一種狀態，不影響別的
        }
    }

    // ------------------------------------------------------------ 語言那一層

    /**
     * 把遊戲的語言包一層。已經包過的先剝掉再包，回來的一定是<b>新的</b>物件。
     * 沒裝 Wynnventory 的時候原樣回去。
     */
    public static Language wrap(Language language) {
        if (language == null || !installed()) {
            return language;
        }
        Language base = language instanceof Wrapped w ? w.inner : language;
        return new Wrapped(base);
    }

    /** 這個鍵是不是 Wynnventory 的。它的鍵都把模組名夾在中間或開頭。 */
    static boolean ours(String key) {
        return key != null && key.contains("wynnventory");
    }

    /**
     * 語言鍵查到的英文 → 譯文；不翻的時候原樣回去。
     *
     * @param base 遊戲自己查到的字。Wynnventory 只有英文語言檔，所以不管遊戲開哪個語言
     *             這裡都是英文；萬一有人用資源包替它翻過，查不到我們的表，照他的顯示
     */
    static String lang(String key, String base) {
        if (!active || base == null || !ours(key)) {
            return base;
        }
        TranslationStore store;
        try {
            store = WynnChaYuan.translations();
        } catch (Throwable t) {
            return base;
        }
        String hit = text(store, base);
        return hit == null ? base : hit;
    }

    /**
     * Wynnventory 的一句英文 → 譯文；沒有就回 {@code null}。
     *
     * <p>頭尾的空白照原文接回去：價格框的標籤是「{@code Lowest: }」，後面直接接價格，
     * 而語料載入時頭尾空白都會被修掉。
     */
    public static String text(TranslationStore store, String english) {
        if (store == null || english == null) {
            return null;
        }
        String core = english.strip();
        if (core.isEmpty()) {
            return null;
        }
        String hit = store.scopedLookup(SCOPE, core);
        if (hit == null || hit.isBlank() || hit.equals(core)) {
            return null;
        }
        int lead = english.indexOf(core);
        return english.substring(0, lead) + hit + english.substring(lead + core.length());
    }

    /** 外殼：Wynnventory 的鍵多查一次，其餘原樣轉給裡面那個。 */
    private static final class Wrapped extends Language {
        private final Language inner;

        private Wrapped(Language inner) {
            this.inner = inner;
        }

        @Override
        public String getOrDefault(String key, String defaultValue) {
            String base = inner.getOrDefault(key, defaultValue);
            return ours(key) ? lang(key, base) : base;
        }

        @Override
        public boolean has(String key) {
            return inner.has(key);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return inner.isDefaultRightToLeft();
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return inner.getVisualOrder(text);
        }
    }

    // ------------------------------------------------------------ 獎勵畫面與通知裡寫死的字

    /**
     * 獎勵畫面上的一塊標籤（Aspects、Tomes、Mythic、Filters…）→ 譯文；
     * 不翻、或表裡沒有的回 {@code null}，那一塊照它原本的畫。
     *
     * <p>只查 Wynnventory 自己的表，不查一般語料：獎勵池的名字是縮寫（NOTG、TCC）
     * 與地區名（Sky Islands），照這個專案的規矩留英文，不能讓一般語料碰巧翻掉。
     */
    public static Component label(Component text) {
        if (!active || text == null) {
            return null;
        }
        String hit = text(store(), text.getString());
        return hit == null ? null : Component.literal(hit).withStyle(text.getStyle());
    }

    /** 篩選鈕的名字。不翻或查不到就原樣回去。 */
    public static String word(String english) {
        if (!active || english == null || english.isEmpty()) {
            return english;
        }
        String hit = text(store(), english);
        return hit == null ? english : hit;
    }

    private static final java.util.regex.Pattern TOAST_FOUND = java.util.regex.Pattern.compile(
            "^((?:\u00a7.)*)(.+?)((?:\u00a7.)*) in (.+)$");
    private static final java.util.regex.Pattern TOAST_MORE = java.util.regex.Pattern.compile(
            "^(\\d+) more\\.\\.\\.$");

    /**
     * 「找到收藏的物品」通知底下那一行。兩種：
     * 「{@code <顏色碼><物品><顏色碼> in <獎勵池>}」與「{@code N more...}」。
     * 物品名照一般語料（跟著「翻譯物品名稱」），獎勵池的名字不動。
     */
    public static Component toast(Component description) {
        if (!active || description == null) {
            return description;
        }
        try {
            String done = toast(store(), description.getString());
            return done == null ? description
                    : Component.literal(done).withStyle(description.getStyle());
        } catch (Throwable t) {
            return description;
        }
    }

    /** 見 {@link #toast(Component)}；沒有東西可換就回 {@code null}。 */
    static String toast(TranslationStore store, String line) {
        if (store == null || line == null) {
            return null;
        }
        java.util.regex.Matcher more = TOAST_MORE.matcher(line);
        if (more.matches()) {
            String frame = text(store, "%s more...");
            return frame == null ? null : frame.replace("%s", more.group(1));
        }
        java.util.regex.Matcher found = TOAST_FOUND.matcher(line);
        if (!found.matches()) {
            return null;
        }
        String name = store.lookup(found.group(2));
        String frame = text(store, "%s in %s");
        if ((name == null || name.isBlank()) && frame == null) {
            return null;
        }
        String item = found.group(1) + (name == null || name.isBlank() ? found.group(2) : name)
                + found.group(3);
        String shape = frame == null ? "%s in %s" : frame;
        int first = shape.indexOf("%s");
        int second = shape.indexOf("%s", first + 2);
        if (first < 0 || second < 0) {
            return null;
        }
        return shape.substring(0, first) + item + shape.substring(first + 2, second)
                + found.group(4) + shape.substring(second + 2);
    }

    private static TranslationStore store() {
        try {
            return WynnChaYuan.translations();
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------ 價格框裡寫死的字

    /**
     * 價格框（與意象清單）要畫的那幾行。不翻的時候<b>原樣</b>回去（同一個物件）。
     *
     * <p>標籤那一半已經由語言那一層換好了，這裡只管寫死的字：物品名、
     * {@code No data yet.}、數值名、意象名。
     */
    public static List<Component> lines(List<Component> lines) {
        if (!active || lines == null || lines.isEmpty()) {
            return lines;
        }
        TranslationStore store;
        try {
            store = WynnChaYuan.translations();
        } catch (Throwable t) {
            return lines;
        }
        return lines(store, lines);
    }

    /** 見 {@link #lines(List)}；測試直接叫這個。 */
    static List<Component> lines(TranslationStore store, List<Component> lines) {
        if (store == null) {
            return lines;
        }
        List<Component> out = null;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            Component done = line == null ? null : line(store, line);
            if (done != line && out == null) {
                out = new ArrayList<>(lines);
            }
            if (out != null) {
                out.set(i, done);
            }
        }
        return out == null ? lines : out;
    }

    private record Piece(Style style, String text) {}

    /** 一行：照樣式切成一段一段，認得的那幾段換掉，樣式原封不動。沒有東西可換就回原物件。 */
    static Component line(TranslationStore store, Component line) {
        List<Piece> pieces = new ArrayList<>();
        line.visit((style, text) -> {
            pieces.add(new Piece(style, text));
            return Optional.empty();
        }, Style.EMPTY);
        boolean changed = false;
        MutableComponent out = Component.empty();
        for (Piece p : pieces) {
            String done = piece(store, p.text());
            if (done != null) {
                changed = true;
            }
            out.append(Component.literal(done == null ? p.text() : done).withStyle(p.style()));
        }
        return changed ? out : line;
    }

    /**
     * 一段字 → 譯文；不該動或查不到就回 {@code null}。
     *
     * <p>先整段查 Wynnventory 自己的表（{@code No data yet.}）。查不到再當成名字查一般語料：
     * 物品名、數值名、意象名。當名字查之前先把開頭不是字母的東西留在原地——意象清單
     * 每一行是「{@code • <職業圖示> 名字}」。
     *
     * <p>不碰的：沒有拉丁字母的（已經是譯文、或只是「{@code : }」），以及價格
     * （「{@code 12.5 LE}」「{@code +3 EB}」——數字開頭）。
     */
    static String piece(TranslationStore store, String text) {
        if (text == null) {
            return null;
        }
        String own = text(store, text);
        if (own != null) {
            return own;
        }
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                return null;                       // 數字先出現：是價格或百分比
            }
            if (latinLetter(c)) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return null;
        }
        String body = text.substring(start).stripTrailing();
        if (body.isEmpty()) {
            return null;
        }
        String hit = store.lookup(body);
        if (hit == null || hit.isBlank() || hit.equals(body)) {
            return null;
        }
        return text.substring(0, start) + hit + text.substring(start + body.length());
    }

    private static boolean latinLetter(char c) {
        return c < 0x250 && Character.isLetter(c);
    }
}
