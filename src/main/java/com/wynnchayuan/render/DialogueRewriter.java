package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 把譯文寫進 Wynncraft <b>自己那個</b>對話框裡。
 *
 * <h2>為什麼不是自己畫一個框</h2>
 * 先前的就地取代是把整段對話藏掉、另外畫一個框。那永遠不會像——因為
 * Wynncraft 的對話框有木紋外框、名牌、SHIFT 按鈕，有些 NPC 還有頭像，
 * 全部都是那條 action bar 裡的字元。藏掉原文等於把它們一起丟掉。
 *
 * <p>所以改成<b>只換裡面的字</b>，其餘每一個字元原樣抄過去。框、名牌底圖、
 * 頭像、淡入動畫、進度條都留著，因為我們根本沒碰它們。
 *
 * <h2>那段文字長什麼樣</h2>
 * 從實機錄下來的結構（見 {@code dialogue-probe}）：
 *
 * <pre>
 *   [-116px] "Let's hope no more grooks waltz "  [+(116 - 寬度)px]
 *     ↑ 固定                                       ↑ 隨文字長度變
 * </pre>
 *
 * 三次抓到的資料完全吻合：文字 35px 時尾隨 +81、93px 時 +23、149px 時 -33。
 * 也就是「往左退 116、畫字、再往右走回原位」——整段的游標淨位移是 0，
 * 後面的名牌與外框才會落在固定的地方。
 *
 * <p>名牌是<b>置中</b>的，中心固定在游標 -40px：{@code The Cook}（寬 40）
 * 是 -60／+20，{@code Enzan}（寬 25）是 -52／+27，兩個算出來的中心都是 -40。
 *
 * <p>所以換字之後<b>尾隨的偏移必須重算</b>，否則後面的東西整個位移。
 * 這是這件事唯一真正的技術內容，其餘都是抄。
 *
 * <h2>中文怎麼畫得出來</h2>
 * 那組 {@code hud/dialogue/text/...} 字型是 Wynncraft 的資源包字型，
 * 有沒有收中文我們不知道也不該賭。做法是<b>只有文字那一段</b>換成預設字型，
 * 前後的偏移字元留在原本的字型裡——偏移字元本來就不是「字」，
 * 用哪個字型畫都一樣。
 */
public final class DialogueRewriter {

    /**
     * 對話內文的字型片段。{@code body_0}、{@code body_1}……一行一個。
     *
     * <p>認的是 {@code /body_} 而不是 {@code hud/dialogue/text/}——後者<b>也會</b>
     * 命中 {@code text/control}（那行 SHIFT 提示）與 {@code text/nameplate}。
     * 把提示併進整句去查表，鍵就變成「 to continueLet's hope…」，當然查不到，
     * 於是整段都不換——就地取代整個失效就是這樣來的。
     */
    private static final String BODY = "/body_";

    /** 說話者名字那一段。 */
    private static final String NAMEPLATE = "hud/dialogue/text/nameplate";

    /** 「 to continue」那一行。它自己就是一句話，跟內文分開查。 */
    private static final String CONTROL = "hud/dialogue/text/control";

    /**
     * 有附中日韓字型的語言。
     *
     * <p>加一個語言就是「放一個 ttf、產六份 JSON、在這裡多一行」。
     * 沒附字型的語言不會壞掉，只是就地取代那一段會退回預設字型。
     */
    private static final java.util.Set<String> SHIPPED = java.util.Set.of("zh_tw");

    /** 內文的左緣：從游標往左退這麼多。實機量到的固定值。 */
    private static final int BODY_LEFT = 116;

    /** 名牌文字的中心：游標往左 40px。實機用兩個不同長度的名字驗過。 */
    private static final int NAME_CENTRE = 40;

    /** 位移字元的基準碼位。{@code 基準 + n} 代表往右 n px，n 可以是負的。 */
    private static final int OFFSET_BASE = 0xD0000;

    private DialogueRewriter() {}

    /**
     * 換掉對話裡的文字；不是對話、或沒有一段換得掉時回傳 {@code null}。
     *
     * @return 改寫過的訊息，或 {@code null} 表示原樣不動
     */
    public static Component rewrite(Component message, TranslationStore store) {
        if (message == null || store == null) {
            return null;
        }
        List<Style> styles = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        message.visit((style, text) -> {
            styles.add(style);
            texts.add(text);
            return Optional.empty();
        }, Style.EMPTY);

        // 先把所有 body_N 的文字段找出來。
        //
        // Wynncraft 一行一個字型：body_0、body_1……一句話太長就拆成好幾行。
        // 先前每一段各自查表，結果是整句中文全塞進 body_0、body_1 還留著英文的
        // 後半段——版面當然跳掉。要整句一起翻，再<b>攤回同樣的行數</b>，
        // 文字才會落在原文落的地方。
        List<Integer> body = new ArrayList<>();
        for (int i = 1; i + 1 < texts.size(); i++) {
            String font = fontOf(styles.get(i));
            if (font.contains(BODY) && !font.contains(NAMEPLATE)
                    && readable(texts.get(i))
                    && offsetOf(texts.get(i - 1)) != null
                    && offsetOf(texts.get(i + 1)) != null) {
                body.add(i);
            }
        }
        if (body.isEmpty()) {
            return null;
        }

        StringBuilder whole = new StringBuilder();
        for (int at : body) {
            whole.append(texts.get(at));
        }
        boolean changed = false;
        String hit = line(whole.toString(), store, body.size());
        List<String> rows = hit == null ? null : wrap(hit, body.size());
        if (rows != null) {
            changed = true;                    // 攤不進原本的行數就只換提示
        }

        boolean[] swapped = new boolean[texts.size()];
        // 換掉之後仍要留在原字型的前綴（目前只有 SHIFT 的按鈕圖示）
        Map<Integer, String> keep = new java.util.HashMap<>();
        // SHIFT 提示自己就是一句話，跟內文分開查。它跟內文一樣是
        // [偏移][文字][偏移]，換完一樣要重算尾隨的偏移。
        for (int i = 1; i + 1 < texts.size(); i++) {
            if (!fontOf(styles.get(i)).contains(CONTROL) || !readable(texts.get(i))) {
                continue;
            }
            Integer lead = offsetOf(texts.get(i - 1));
            if (lead == null || offsetOf(texts.get(i + 1)) == null) {
                continue;
            }
            // SHIFT 那個按鈕圖示是<b>混在同一段文字裡</b>的，不是獨立片段：
            // 這一段長得像「<U+E000> to continue」。整段拿去查表就是
            // " to continue"，而語料裡是 "to continue"，永遠對不上。
            //
            // 所以把開頭的圖示與空白剝掉再查，換完再原樣接回去。
            String raw = texts.get(i);
            int at = 0;
            while (at < raw.length() && !readable(raw.substring(at, at + 1))) {
                at++;
            }
            String prefix = raw.substring(0, at);
            String tip = store.lookup(raw.substring(at).strip());
            if (tip == null || tip.isBlank()) {
                continue;
            }
            // 圖示要留在<b>原本的字型</b>裡：那個 SHIFT 按鈕是它畫出來的，
            // 換成預設字型就變成缺字。所以只有譯文那一截換字型，
            // 組裝時再把兩截接起來（見下面的 keep）。
            keep.put(i, prefix);
            texts.set(i, tip);
            int total = width(Component.literal(prefix).withStyle(styles.get(i)))
                    + width(tip, styles.get(i));
            texts.set(i + 1, offset(-lead - total));
            swapped[i] = true;
            changed = true;
        }
        if (rows != null) {
            for (int n = 0; n < body.size(); n++) {
                int at = body.get(n);
                int lead = offsetOf(texts.get(at - 1));
                texts.set(at, rows.get(n));
                texts.set(at + 1,
                        offset(-lead - width(rows.get(n), styles.get(at))));
                swapped[at] = true;
            }
        }
        if (!changed) {
            return null;
        }

        MutableComponent out = Component.empty();
        for (int i = 0; i < texts.size(); i++) {
            // 換過的那一段用<b>預設字型</b>畫，中文才出得來；沒換的原樣抄，
            // 包含它原本的字型——框、頭像、按鈕都是靠那些字型畫出來的。
            String prefix = keep.get(i);
            if (prefix != null && !prefix.isEmpty()) {
                // 圖示照原樣、原字型；只有後面的譯文換成預設字型
                out.append(Component.literal(prefix).withStyle(styles.get(i)));
            }
            // 換過的那一段，只有<b>畫不出來</b>的時候才改字型。
            //
            // Wynncraft 的對話字型把行號烘進了 ascent（body_0 是 34、body_1
            // 是 22、control 是 -38，預設字型是 7），換成預設就等於丟掉高度。
            // 譯文如果本來就畫得出來——西班牙文、法文、德文那些只用到拉丁字母
            // 的語言——就<b>不要碰字型</b>，位置跟原文一模一樣。
            Style style = styles.get(i);
            if (swapped[i] && !drawable(texts.get(i))) {
                FontDescription pair = paired(fontOf(styles.get(i)));
                // 配不到就退回預設字型：位置會掉，但至少看得到字
                style = style.withFont(
                        pair == null ? FontDescription.DEFAULT : pair);
            }
            out.append(Component.literal(texts.get(i)).withStyle(style));
        }
        return out;
    }

    /**
     * 把譯文攤成正好 {@code rows} 行，每一行都塞得進框裡。
     *
     * <p>行數<b>必須</b>跟原文一樣：多了畫不下（框的高度是伺服器決定的，
     * 我們動不了），少了文字會往上擠，看起來像浮在框裡。寧可不換。
     *
     * @return 每一行的內容；攤不進去時回傳 {@code null}
     */
    static List<String> wrap(String text, int rows) {
        int limit = BODY_LEFT * 2;
        List<String> out = new ArrayList<>(rows);
        int at = 0;
        for (int row = 0; row < rows; row++) {
            if (at >= text.length()) {
                out.add("");                   // 中文比較短，後面幾行留空
                continue;
            }
            int end = at;
            int last = at;
            while (end < text.length()
                    && width(text.substring(at, end + 1)) <= limit) {
                end++;
                if (end < text.length() && text.charAt(end) == ' ') {
                    last = end;                // 記著最後一個可以斷的空白
                }
            }
            if (end >= text.length()) {
                out.add(text.substring(at));
                at = text.length();
                continue;
            }
            // 英文單字不從中間切；中文可以在任何字之間斷
            int cut = last > at && isLatin(text.charAt(end)) ? last : end;
            out.add(text.substring(at, cut));
            at = cut < text.length() && text.charAt(cut) == ' ' ? cut + 1 : cut;
        }
        return at >= text.length() ? out : null;
    }

    private static boolean isLatin(char c) {
        return c < 0x2E80 && Character.isLetterOrDigit(c);
    }

    /**
     * 內文：整句查表，查不到就用開頭比對（NPC 是一個字一個字打出來的）。
     *
     * <p>{@code rows} 是原文佔了幾行。長度上限要乘上行數——先前這裡寫死
     * 一行的寬度，等於<b>只要譯文超過一行就整句不翻</b>，兩行以上的台詞
     * 全部留在英文。{@link #wrap} 本來就是為了攤成多行才寫的，
     * 攤不進去它會回 {@code null}，所以這裡不必再擋一次。
     */
    private static String line(String text, TranslationStore store, int rows) {
        String hit = store.lookup(text.strip());
        if (hit == null) {
            String full = store.matchPrefix(text.strip());
            hit = full == null ? null : store.lookup(full);
        }
        if (hit == null || hit.isBlank()) {
            return null;
        }
        // 塞不進框裡就不換。中文通常比英文短，真的塞不下時，
        // 讓玩家看見完整的英文，比看見被切掉一半的中文好。
        return width(hit) <= BODY_LEFT * 2 * rows ? hit : null;
    }

    /** 名牌：說話者的名字，語料裡本來就有（npc.json）。 */
    private static String speaker(String text, TranslationStore store) {
        String hit = store.lookup(text.strip());
        return hit == null || hit.isBlank() ? null : hit;
    }

    /**
     * 名牌是置中的，所以前導也要跟著重算——這裡回傳新的前導值。
     *
     * <p>內文是靠左的，前導固定不用動；名牌不是。分開處理，
     * 不然名字一換長度就整塊偏掉。
     */
    static int nameLead(int width) {
        return -(NAME_CENTRE + width / 2);
    }

    /** 這一段是不是單純一個位移字元；是的話回傳位移的像素數。 */
    static Integer offsetOf(String text) {
        if (text.codePointCount(0, text.length()) != 1) {
            return null;
        }
        int cp = text.codePointAt(0);
        // 位移字元的範圍，見 GlyphSplitter#isGlyphCodePoint
        if (cp < 0xCF000 || cp > 0xD1000) {
            return null;
        }
        return cp - OFFSET_BASE;
    }

    /** 把像素數編回位移字元。 */
    static String offset(int px) {
        return new String(Character.toChars(OFFSET_BASE + px));
    }

    /**
     * 這一段文字<b>實際畫出來</b>會有多寬。
     *
     * <p>不能一律拿預設字型去量：中日韓走的是我們自己那套 Cubic 11，
     * 字寬跟預設的和 Wynncraft 的都不一樣。量錯了尾隨偏移就補錯，
     * 後面整塊會跟著偏。
     */
    private static int width(String text, Style original) {
        if (drawable(text)) {
            return width(Component.literal(text).withStyle(original));
        }
        FontDescription pair = paired(fontOf(original));
        return width(Component.literal(text).withStyle(
                pair == null ? original.withFont(FontDescription.DEFAULT)
                        : original.withFont(pair)));
    }

    private static int width(String text) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? text.length() * 6 : mc.font.width(text);
    }

    /** 帶樣式量寬度——圖示的寬度要照它<b>原本的字型</b>算，不是預設字型。 */
    private static int width(Component text) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? 0 : mc.font.width(text);
    }

    /**
     * Wynncraft 的對話字型畫不畫得出這一串。
     *
     * <p>字元集直接來自它自己的字型定義（{@code font-dump.txt}）：
     * {@code wynncraft.png} 是可見的 ASCII，{@code wynncraft_latin.png}
     * 補上帶重音的拉丁字母。兩張表以外的字（中日韓、西里爾、諺文）
     * 畫出來是空框，那才需要換字型——代價是位置會掉。
     */
    static boolean drawable(String text) {
        return text.codePoints().allMatch(cp ->
                (cp >= 0x20 && cp < 0x7F) || cp == 0x2014
                        || "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏàáâãäåæçèéêëìíîï".indexOf(cp) >= 0);
    }

    private static boolean readable(String text) {
        return text.codePoints().anyMatch(cp ->
                cp >= 0x20 && cp < 0x2E80 && !Character.isWhitespace(cp));
    }

    /**
     * 同一行、但畫得出中日韓的字型。
     *
     * <h2>為什麼非得自己做一套</h2>
     * Wynncraft 把「畫在第幾行」烘進了字型的 {@code ascent}（`body_0` 是 34、
     * `body_1` 是 22、`control` 是 -38，而 {@code minecraft:default} 是 7）。
     * 整條對話是<b>一個</b> action bar 字串，位移字元只能左右移不能上下移——
     * 換成預設字型就等於把行號丟掉，內文往下掉、SHIFT 提示往上跳進框裡被蓋住。
     *
     * <p>所以每一行各做一份字型：ASCII 直接 {@code reference} Wynncraft 自己那份
     * （外觀與高度完全不變），中日韓走 Cubic 11 並用 {@code shift} 補回高度差。
     * MC 的 ttf provider 把 shift 交給 FreeType 時取了負號（{@code deltaY = -shiftY}），
     * 而 FreeType 的 +y 朝上，所以 <b>shift 的 y 是正數往下</b>；
     * 又因為它乘上 oversample 之後才送出去，單位就是最終螢幕像素。
     * 位移量因此正好是 {@code -(ascent - 7)}。
     *
     * @return 對應的字型；這一段不是對話文字就回傳 {@code null}
     */
    private static FontDescription paired(String font) {
        String name = null;
        if (font.contains(BODY)) {
            int at = font.lastIndexOf(BODY);
            name = "body_" + font.substring(at + BODY.length());
        } else if (font.contains(CONTROL)) {
            name = "control";
        } else if (font.contains(NAMEPLATE)) {
            name = "nameplate";
        }
        if (name == null) {
            return null;
        }
        // 字型 id 後面常常還跟著 class 的 toString 尾巴，只留合法的部分
        StringBuilder clean = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                clean.append(Character.toLowerCase(c));
            } else {
                break;
            }
        }
        if (clean.isEmpty()) {
            return null;
        }
        // 字型是<b>按語言</b>分的。同一個碼位在不同地區的寫法不一樣
        //（「骨」「角」「直」的內部筆畫，中文與日文就不同），所以 Ark Pixel
        // 才拆成 zh_tw／zh_cn／ja／ko。塞同一份給所有語言，等於讓某些語言
        // 讀到別的地區的字形。
        //
        // 只為<b>真的附了字型</b>的語言配對；其他語言回 null，交給呼叫端退回
        // 預設字型——位置會掉，但總比整段空白好。之後某個語言開始翻對話時，
        // 就只是「多一個 ttf + 多六個 JSON」，這裡不用再動。
        String lang = WynnChaYuan.language();
        if (!SHIPPED.contains(lang)) {
            return null;
        }
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath(
                WynnChaYuan.MOD_ID, "dialogue/" + lang + "/" + clean));
    }

    private static String fontOf(Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }
}
