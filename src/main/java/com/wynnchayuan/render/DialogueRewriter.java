package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
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
     * 選項那幾列的字型。認的是 {@code /choice_}（有底線）——
     * {@code style/default/choice} 是畫外框的，沒有底線，不能混進來。
     */
    private static final String CHOICE = "/choice_";

    /**
     * 有附中日韓字型的語言。
     *
     * <p>加一個語言就是「放一個 ttf、產六份 JSON、在這裡多一行」。
     * 沒附字型的語言不會壞掉，只是就地取代那一段會退回預設字型。
     */
    private static final java.util.Set<String> SHIPPED = java.util.Set.of("zh_tw");

    /** 每個語言那套字型畫得出來的碼位區間，第一次用到才讀。 */
    private static final Map<String, int[]> COVERAGE = new java.util.HashMap<>();

    /** 哪幾份對話字型真的打包進來了；見 {@link #fontMissing}。 */
    private static final Map<String, Boolean> FONTS = new java.util.HashMap<>();

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
        //
        // 一行不一定只有一段。句子裡有顏色強調時，Wynncraft 會把它切開送過來：
        // 「Go ahead and 」「open that gate」「.」——中間那幾段的鄰居是<b>文字</b>，
        // 不是位移字元。先前的判斷要求左右兩側都是位移，於是這種行三段全不合格，
        // 整行對改寫器來說等於不存在，永遠留在英文（玩家回報的「只翻了第一行」）。
        //
        // 改成收「兩個位移之間、連續的文字段」當作一行，頭尾各記一份。
        // 代價是整行只剩一個顏色（取第一段的）——句中的強調色會沒了，
        // 但看得懂的中文比有顏色的英文重要。
        List<Integer> body = new ArrayList<>();     // 每一行的第一段
        List<Integer> ends = new ArrayList<>();     // 每一行的最後一段
        for (int i = 1; i + 1 < texts.size(); i++) {
            if (offsetOf(texts.get(i - 1)) == null) {
                continue;                           // 這裡不是一行的開頭
            }
            int stop = i;
            while (stop < texts.size() && offsetOf(texts.get(stop)) == null) {
                stop++;                             // 一路吃到下一個位移字元
            }
            if (stop == i || stop >= texts.size()) {
                continue;                           // 沒有文字，或收不到結尾的位移
            }
            int last = stop - 1;
            if (!bodyRow(texts, styles, i, last)) {
                continue;
            }
            body.add(i);
            ends.add(last);
            i = last;                               // 這一行收完了，跳過去
        }
        // body 是空的也要往下走：任務開始那類訊息沒有台詞行，只有底下的
        // SHIFT 提示。先前在這裡直接 return，於是那種訊息的「to continue」
        // 永遠翻不出來——而它是玩家最常看到的一句。

        StringBuilder whole = new StringBuilder();
        for (int n = 0; n < body.size(); n++) {
            whole.append(joinRow(whole.toString(),
                    rowText(texts, body.get(n), ends.get(n))));
        }
        boolean changed = false;
        // 內文與選項是<b>兩個</b>開關。呼叫端只要有一邊是就地取代就會進來，
        // 所以這裡得各自再確認一次，不然關掉的那一邊也會被換掉。
        var mode = com.wynnchayuan.CollectorConfig.DialogueMode.REPLACE;
        boolean doBody = com.wynnchayuan.WynnChaYuan.config().dialogueMode() == mode;
        boolean doChoice = com.wynnchayuan.WynnChaYuan.config().choiceMode() == mode;
        // 一行放得下多少：兩邊都要用同一個數字。
        //
        // 先前 wrap() 自己寫死 232，而這裡算的是照實際前導偏移得出的寬度——
        // 有頭像的對話文字是從頭像右邊起算的，可用寬度少了 24 像素，於是
        // 「整句塞不塞得下」判斷用窄的、真正斷行時用寬的，中文就滿出框外。
        int room = rowWidth(texts, body);
        String hit = body.isEmpty() || !doBody ? null
                : line(whole.toString(), store, body.size(),
                        styles.get(body.get(0)), room);
        List<String> rows = hit == null
                ? null : wrap(hit, body.size(), styles.get(body.get(0)), room);
        // 有一行配不到中文字型就整段不換。少一份字型的下場不是「位置歪掉」
        // 而是<b>那一整行變成方框</b>（見 fontMissing），跟缺字一樣糟。
        for (int n = 0; rows != null && n < body.size(); n++) {
            if (fontMissing(fontOf(styles.get(body.get(n))))) {
                rows = null;
            }
        }
        if (rows != null) {
            changed = true;                    // 攤不進原本的行數就只換提示
        }

        boolean[] swapped = new boolean[texts.size()];
        // 換掉之後仍要留在原字型的前綴（目前只有 SHIFT 的按鈕圖示）
        Map<Integer, String> keep = new java.util.HashMap<>();
        // SHIFT 提示自己就是一句話，跟內文分開查。它跟內文一樣是
        // [偏移][文字][偏移]，換完一樣要重算尾隨的偏移。
        for (int i = 1; doBody && i + 1 < texts.size(); i++) {
            if (!fontOf(styles.get(i)).contains(CONTROL) || !readable(texts.get(i))) {
                continue;
            }
            Integer lead = offsetOf(texts.get(i - 1));
            Integer trail = offsetOf(texts.get(i + 1));
            if (lead == null || trail == null) {
                continue;
            }
            int was = width(texts.get(i), styles.get(i));
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
            if (tip == null || tip.isBlank() || !renderable(tip)) {
                continue;
            }
            if (!drawable(tip) && fontMissing(fontOf(styles.get(i)))) {
                continue;                       // 沒有這一份中文字型，換了是一排方框
            }
            // 圖示要留在<b>原本的字型</b>裡：那個 SHIFT 按鈕是它畫出來的，
            // 換成預設字型就變成缺字。所以只有譯文那一截換字型，
            // 組裝時再把兩截接起來（見下面的 keep）。
            int total = width(Component.literal(prefix).withStyle(styles.get(i)))
                    + width(tip, styles.get(i));
            String back = offset(trail + was - total);        // 同上：只補長度差
            if (back == null) {
                continue;                       // 補不回去就別動這一段，見 offset
            }
            keep.put(i, prefix);
            texts.set(i, tip);
            texts.set(i + 1, back);
            swapped[i] = true;
            changed = true;
        }
        // 選項那幾列。形狀跟上面的 SHIFT 提示一樣是 [偏移][文字][偏移]，
        // 差別是<b>沒有前置圖示</b>——整段就是選項本身，直接查表就好。
        //
        // 每一列有自己的字型（choice_1／2／3），行號烘在 ascent 裡（97／84／71），
        // 所以換完要配對應的中文字型，不然三列會疊在一起。見 paired()。
        for (int i = 1; doChoice && i + 1 < texts.size(); i++) {
            if (!fontOf(styles.get(i)).contains(CHOICE) || !readable(texts.get(i))) {
                continue;
            }
            Integer lead = offsetOf(texts.get(i - 1));
            Integer trail = offsetOf(texts.get(i + 1));
            if (lead == null || trail == null) {
                continue;
            }
            String pick = store.lookup(texts.get(i).strip());
            if (pick == null || pick.isBlank() || !renderable(pick)) {
                continue;                       // 查不到就留英文，不要換一半
            }
            if (!drawable(pick) && fontMissing(fontOf(styles.get(i)))) {
                continue;                       // 沒有這一列的中文字型，換了是一排方框
            }
            int was = width(texts.get(i), styles.get(i));
            // 跟內文一樣只補長度差：這一列縮短多少就還回去多少，
            // 後面幾列與外框的位置才不會被推走。
            String back = offset(trail + was - width(pick, styles.get(i)));
            if (back == null) {
                continue;                       // 補不回去就別動這一列，見 offset
            }
            texts.set(i, pick);
            texts.set(i + 1, back);
            swapped[i] = true;
            changed = true;
        }
        if (rows != null) {
            for (int n = 0; n < body.size(); n++) {
                int at = body.get(n);
                // 只把偏移「補上長度差」，不要自己算一個新的。
                //
                // 兩行的訊息長這樣：[前導][第一行][前導][第二行][尾隨]。
                // 也就是說對第一行而言，at + 1 是<b>第二行的前導</b>，不是尾隨偏移。
                // 先前每一行都照「淨位移歸零」重算 at + 1，等於把第二行的前導蓋掉，
                // 後面的頭像與外框就整個被推走——一行的時候剛好沒事，字數多到
                // 換行才歪，正是「跳出一定字數就把框位移」。
                //
                // 改成沿用原本的偏移再加上這一行縮短了多少，原文的版面就原樣保住，
                // 不管 at + 1 是下一行的前導還是整段的尾隨都成立。
                //
                // 一行可能有好幾段（顏色強調），所以要看的是<b>最後一段</b>的
                // 後面那個偏移，寬度也要把整行加起來算。
                int end = ends.get(n);
                Integer after = offsetOf(texts.get(end + 1));
                if (after == null) {
                    continue;
                }
                int was = 0;
                for (int i = at; i <= end; i++) {
                    was += width(texts.get(i), styles.get(i));
                }
                int shrink = was - width(rows.get(n), styles.get(at));
                String back = offset(after + shrink);
                if (back == null) {
                    continue;                      // 補不回去就別動這一行，見 offset
                }
                texts.set(at, rows.get(n));
                for (int i = at + 1; i <= end; i++) {
                    texts.set(i, "");              // 整行併到第一段，其餘清空
                    swapped[i] = true;
                }
                texts.set(end + 1, back);
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
        return wrap(text, rows, null, BODY_LEFT * 2);
    }

    /**
     * @param style 這一行實際會用的樣式。<b>一定要傳</b>——量寬度得用真正會畫出來的
     *              字型，中日韓走的是我們自己那套（一個字 10 像素），拿預設字型
     *              （9 像素）去量會少算一成，於是每一行都塞得比框還寬，畫出來就溢出。
     */
    static List<String> wrap(String text, int rows, Style style, int limit) {
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
                    && measure(text.substring(at, end + 1), style) <= limit) {
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
            // 英文單字不從中間切；中文可以在任何字之間斷。
            //
            // 退到「上一個空白」是不夠的：玩家 ID 前面常常是頓號或驚嘆號，
            // 整行一個空白都沒有，於是 Green_teaTW 被切成 Green_ 和 teaTW。
            // 改成退到<b>這個字自己的開頭</b>，前面有沒有空白都一樣。
            int cut = end;
            if (isWord(text.charAt(end))) {
                int back = end;
                while (back > at && isWord(text.charAt(back - 1))) {
                    back--;
                }
                if (back > at) {
                    cut = back;
                } else if (last > at) {
                    cut = last;                // 整行就是一個長字，只好斷在空白
                }
            }
            out.add(text.substring(at, cut).stripTrailing());
            at = cut < text.length() && text.charAt(cut) == ' ' ? cut + 1 : cut;
        }
        return at >= text.length() ? out : null;
    }

    private static boolean isLatin(char c) {
        return c < 0x2E80 && Character.isLetterOrDigit(c);
    }

    /** 不能從中間切開的一整個字。底線算在裡面：玩家 ID 幾乎都有。 */
    private static boolean isWord(char c) {
        return isLatin(c) || c == '_';
    }

    /**
     * 譯文打到哪裡了。
     *
     * <p>原文打了幾成，譯文就出幾成。這樣譯文的出現節奏跟原文一致，
     * 看起來就像 Wynncraft 自己在打字。
     *
     * <p>不會切在佔位符中間——{@code {~1}} 被切成 {@code {~} 之後，
     * 那一行的佔位符數量就跟原文對不上，整行會靜靜失效。
     */
    static String typedSoFar(String full, int typed, int total) {
        if (total <= 0 || typed >= total) {
            return full;
        }
        int take = (int) Math.round(full.length() * (double) typed / total);
        take = Math.max(0, Math.min(full.length(), take));
        int brace = full.lastIndexOf('{', Math.max(0, take - 1));
        if (brace >= 0 && full.indexOf('}', brace) >= take) {
            take = brace;
        }
        return full.substring(0, take);
    }

    /**
     * 把譯文裡的佔位符換回原文的實際值。
     *
     * <p>{@code {~1}} 這種帶編號的寫法指名要原文的第幾個數值——中文語序常常
     * 跟英文相反，沒有編號就沒辦法把數字擺到正確的位置。
     */
    static String fill(String translated, LineParts parts) {
        StringBuilder out = new StringBuilder();
        int place = 0;
        int user = 0;
        int number = 0;
        for (int i = 0; i < translated.length(); ) {
            if (translated.startsWith("{p}", i)) {
                out.append(pick(parts.places(), place++));
                i += 3;
            } else if (translated.startsWith("{u}", i)) {
                out.append(pick(parts.users(), user++));
                i += 3;
            } else if (translated.startsWith("{~", i)
                    && translated.indexOf('}', i) > 0) {
                int close = translated.indexOf('}', i);
                String index = translated.substring(i + 2, close);
                int at = number;
                if (!index.isEmpty()) {
                    try {
                        at = Integer.parseInt(index) - 1;
                    } catch (NumberFormatException e) {
                        at = number;               // 不是數字就當作沒編號
                    }
                } else {
                    number++;
                }
                out.append(pick(parts.numbers(), at));
                i = close + 1;
            } else {
                out.append(translated.charAt(i++));
            }
        }
        return out.toString();
    }

    private static String pick(List<LineParts.Piece> pieces, int at) {
        return at >= 0 && at < pieces.size() ? pieces.get(at).text() : "";
    }

    /**
     * 這一行放得下多寬。
     *
     * <p>不能寫死：有頭像的對話，文字是<b>從頭像右邊</b>開始的——前導偏移
     * 從 −116 變成 −92，整整少了 24 像素。拿 −116 去算就會以為還有空間，
     * 於是中文疊到頭像上（玩家回報的「任務翻譯錯位」）。
     * 右邊界是固定的，所以寬度就是「實際前導的絕對值 + 右邊界」。
     */
    /**
     * 拆成幾截各自查表，每一截都查得到才算數。
     *
     * <p>任務開始那則訊息就是拼出來的：「New Quest Started:」是一句、
     * 「King's Recruit」是任務名、「[0/150 (0.0%)]」是進度。語料裡三樣<b>都有</b>，
     * 就是沒有黏在一起的那一條——而任務有一百多個，一條條收根本收不完。
     * 打字打到一半時前綴還對得上，任務名一出來就整句掉回英文，
     * 畫面上看起來就是「翻譯翻到一半變英文」。
     *
     * <p>每一截都從<b>最長</b>的開始試，並且要求整句被吃完，湊不滿就整個不換——
     * 半句中文半句英文比整句英文糟糕得多。
     *
     * @return 接起來的譯文，或 {@code null} 表示這樣拆也不成
     */
    private static String join(String text, TranslationStore store) {
        StringBuilder out = new StringBuilder();
        String rest = text.strip();
        for (int part = 0; part < PARTS && !rest.isEmpty(); part++) {
            String head = null;
            String piece = null;
            for (int at = rest.length(); at > 0; at--) {
                if (at < rest.length() && rest.charAt(at) != ' ') {
                    continue;                  // 只在空白處切
                }
                head = rest.substring(0, at).strip();
                piece = plain(head) ? head : store.lookup(head);
                if (piece != null && !piece.isBlank()) {
                    break;
                }
                piece = null;
            }
            if (piece == null) {
                return null;
            }
            if (out.length() > 0 && gap(out.charAt(out.length() - 1), piece.charAt(0))) {
                out.append(' ');
            }
            out.append(piece);
            rest = rest.substring(head.length()).strip();
        }
        return rest.isEmpty() && out.length() > 0 ? out.toString() : null;
    }

    /**
     * 畫面上的字停下來了嗎。
     *
     * <h2>為什麼要問</h2>
     * 「還在打字」與「打完了」對同一段文字要有不同的態度：還在打的時候，
     * 現在看到的這半句隨時會變長，貿然定案等一下就得改口（見 {@link #line}）；
     * 打完了就沒有這個顧慮，該貼就貼。
     *
     * <p>沒有現成的「打完了」訊號，但字停住幾幀就等於停住了。打字大約
     * 每秒二三十個字，字與字之間頂多隔兩三幀；連續幾幀一模一樣就不是
     * 打字的空檔，是真的停了。
     */
    static boolean settled(String raw) {
        if (raw.equals(lastRaw)) {
            still++;
        } else {
            lastRaw = raw;
            still = 0;
        }
        return still >= SETTLE_FRAMES;
    }

    private static String lastRaw = "";
    private static int still = 0;

    /**
     * 停幾幀才算停下來。
     *
     * <p>六幀在 60fps 下是十分之一秒——比字與字之間的空檔長，
     * 又短到玩家感覺不出來。真的猜錯了頂多退回舊行為，不會更糟。
     */
    private static final int SETTLE_FRAMES = 6;

    /** 上一幀畫面上打到哪（<b>原始</b>文字）。見 {@link #line}。 */
    private static String said = "";

    /** 上一幀認出來的是語料裡的哪一條。 */
    private static String spoken;

    /**
     * 認出來的那一條還罩得住這一幀嗎。
     *
     * <p>打字打到人名、地名或數值<b>中間</b>時，模板會暫時對不上：畫面上是
     * 「…have you seen Green_te」，語料裡是「…have you seen {u}」——要整個名字
     * 打完才會收成佔位符。單看 {@code startsWith} 的話這中間幾幀全部落空，
     * 玩家看到的就是「中文 → 英文 → 中文」。
     *
     * <p>所以岔開是允許的，但只允許岔在<b>語料那條的裡面</b>。岔開的位置
     * 剛好是語料的結尾，代表畫面上的字已經比那條長了——那就不是同一句
     * （任務開始那則後面還接著進度顯示就是這樣），要讓它往下走 {@link #join}。
     */
    static boolean within(String source, String typed) {
        if (source.startsWith(typed)) {
            return true;
        }
        int room = Math.min(source.length(), typed.length());
        int agreed = 0;
        while (agreed < room && source.charAt(agreed) == typed.charAt(agreed)) {
            agreed++;
        }
        return agreed >= AGREED && agreed < source.length()
                && typed.length() <= source.length() + NAME_ROOM;
    }

    /** 岔開之前至少要對上這麼多，才算「還是同一句」。 */
    private static final int AGREED = 12;

    /** 一個還沒收成佔位符的名字，最多比佔位符本身長這麼多。 */
    private static final int NAME_ROOM = 24;

    /** 最多拆成幾截。再多就不是「拼出來的訊息」，是硬湊了。 */
    private static final int PARTS = 4;

    /**
     * 沒有字要翻的一截：數字、括號、佔位符。
     *
     * <p>像「[0/150 (0.0%)]」這種進度顯示，語料裡不會有、也不該有，
     * 但它擋在中間會讓整句拼不起來。原樣留著就好。
     */
    private static boolean plain(String text) {
        String bare = text.replaceAll("\\{[^}]*\\}", "");
        for (int i = 0; i < bare.length(); i++) {
            char c = bare.charAt(i);
            if (Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    /** 兩截之間要不要空白：接的是英文才要，中文之間不要。 */
    private static boolean gap(char left, char right) {
        return (isLatin(right) || isLatin(left)) && left != ' ' && right != ' ';
    }

    /**
     * 這幾段是不是同一行的內文。
     *
     * <p>整段都要是 body 字型（名牌另外處理），而且至少有一段是看得懂的文字——
     * 純粹由排版字元組成的那幾段不算一行。
     */
    private static boolean bodyRow(List<String> texts, List<Style> styles,
            int from, int to) {
        boolean any = false;
        for (int i = from; i <= to; i++) {
            String font = fontOf(styles.get(i));
            if (!font.contains(BODY) || font.contains(NAMEPLATE)) {
                return false;
            }
            any |= readable(texts.get(i));
        }
        return any;
    }

    /** 一行的完整文字——顏色強調會把它切成好幾段。見 {@link #bodyRow}。 */
    private static String rowText(List<String> texts, int from, int to) {
        StringBuilder out = new StringBuilder();
        for (int i = from; i <= to; i++) {
            out.append(texts.get(i));
        }
        return out.toString();
    }

    private static int rowWidth(List<String> texts, List<Integer> body) {
        if (body.isEmpty()) {
            return BODY_LEFT * 2;
        }
        Integer lead = offsetOf(texts.get(body.get(0) - 1));
        if (lead == null || lead >= 0) {
            return BODY_LEFT * 2;
        }
        return -lead + BODY_LEFT;
    }

    /**
     * 接下一行時要補回被吃掉的空格。
     *
     * <p>Wynncraft 是在<b>空白處</b>折行的，而那個空白不留在任何一行裡：
     * body_0 是「…My brother」、body_1 是「keeps sending…」。直接相接會變成
     * 「My brotherkeeps」，語料裡永遠查不到——NPC 只要講到需要換行的長度，
     * 整句就翻不出來。玩家看到的「多講幾句就斷掉」正是這個。
     *
     * @return 這一行要接上去的內容（必要時前面補一個空格）
     */
    static String joinRow(String sofar, String row) {
        if (sofar.isEmpty() || sofar.endsWith(" ") || row.startsWith(" ")) {
            return row;
        }
        return " " + row;
    }

    /**
     * 內文：整句查表，查不到就用開頭比對（NPC 是一個字一個字打出來的）。
     *
     * <p>{@code rows} 是原文佔了幾行。長度上限要乘上行數——先前這裡寫死
     * 一行的寬度，等於<b>只要譯文超過一行就整句不翻</b>，兩行以上的台詞
     * 全部留在英文。{@link #wrap} 本來就是為了攤成多行才寫的，
     * 攤不進去它會回 {@code null}，所以這裡不必再擋一次。
     */
    private static String line(String text, TranslationStore store, int rows,
            Style style, int width) {
        // 先參數化再查表。
        //
        // 語料裡的鍵是「Hey, {u}! Are you alright…」，而畫面上是玩家的真名。
        // 先前這裡拿<b>原始文字</b>直接查，凡是句子裡有玩家名、地名或數字的
        // 一律查不到——側邊面板翻得出來、就地取代翻不出來，差別就在這一步。
        // 用的是跟語料同一支參數化程式，兩邊算出來的模板才會一樣。
        String raw = text.strip();
        LineParts parts = LineParts.of(StyledText.fromString(raw));
        // 遊戲有時候真的印出「Player」這四個字，而不是玩家的名字——新手任務
        // 就有好幾句。那幾句語料裡早就翻好了（鍵是 {u}），只是模板對不起來。
        // 見 LineParts#namingPlayer；只在整行查不到時才改，介面上那幾條正經的
        // 「Player Slot」「Looking for a Player...」自己查得到，走不到這裡。
        if (store.lookup(parts.template().strip()) == null) {
            parts = parts.namingPlayer();
        }
        String typed = parts.template().strip();
        String source = typed;
        String hit = store.lookup(typed);
        if (hit != null && !settled(raw) && store.hasLonger(typed)) {
            // 打到一半的那半句，本身剛好也是語料裡的另一條。
            //
            // 逐字模擬全部 5749 句台詞，這樣的情形有 105 句：打到「Block」先貼上
            // 技能表的「格擋」、打到「Bring」先貼上任務目標的「攜帶」、
            // 打到「...」先貼上「……」，一兩幀之後整句打完又被換掉。
            // 玩家看到的就是中文閃一下變成別的字。
            //
            // 後面還有更長的候選、而畫面上的字還在長，就先不要定案——
            // 讓它往下走前綴比對，那條路本來就是為「還沒打完」寫的。
            // 沒有更長的候選時不受影響：「Agh!」那種短句照樣立刻換。
            hit = null;
        }
        if (hit == null) {
            // 這一句上一幀就認出來了，繼續用同一條。
            //
            // 逐字打字時 matchPrefix 要求「只有一條語料以這個開頭」。打到一半
            // 剛好有第二條也是同樣開頭時，它會回 null，畫面就掉回英文；再多打
            // 幾個字岔開了又跳回中文——玩家看到的「講到一半忽然變英文又變回來」
            // 就是這個。既然上一幀已經確定是哪一句，中間這幾幀沒理由再問一次。
            //
            // 只在<b>還是同一句話</b>時沿用。「同一句」看的是<b>原始文字</b>
            // 有沒有繼續長出去——原文是一個字一個字加上去的，這個判斷永遠成立；
            // 拿參數化後的模板來比反而會在打到人名中間時自己斷掉（見 within）。
            if (spoken != null && !said.isEmpty() && raw.startsWith(said)
                    && within(spoken, typed)) {
                source = spoken;
                hit = store.lookup(spoken);
            }
        }
        if (hit == null) {
            // 門檻要看<b>畫面上已經打出多少字</b>，不是模板有多長。
            //
            // 玩家名被 {u} 收掉之後模板會短一大截：畫面上打出
            // 「Hey, Green_teaTW」十六個字，模板卻只有「Hey, {u}」八個字，
            // 卡在門檻底下查不到，於是開頭那一小段先閃出英文才跳成中文。
            source = store.matchPrefix(typed, raw.length(),
                                       com.wynnchayuan.capture.CurrentQuest.get());
            hit = source == null ? null : store.lookup(source);
        }
        if (hit != null && source != null) {
            said = raw;                // 記住這一幀畫面上打到哪
            spoken = source;           // 以及它是語料裡的哪一條
        } else if (!raw.startsWith(said)) {
            said = "";                 // 換句話了，別讓舊的那條黏著
            spoken = null;
        }
        if (hit == null) {
            // 「一句話 + 一個名字」：任務開始那則訊息就是這樣拼出來的。
            //
            // 語料裡有「New Quest Started:」也有「King's Recruit」，就是沒有
            // 兩個黏在一起的那一條——而任務名有一百多個，一條條收根本收不完。
            // 打字打到一半時前綴還對得上，名字一出來就整句掉回英文，
            // 看起來就是「翻譯翻到一半變英文」。
            hit = join(typed, store);
            source = hit == null ? null : typed;
        }
        if (hit == null || hit.isBlank()) {
            return null;
        }
        // 有字畫不出來就整段不換——一句話裡插幾個方框，比整句留著英文糟糕得多。
        if (!renderable(hit)) {
            return null;
        }
        hit = fill(hit, parts);            // 佔位符換回真名、地名與數值
        int limit = width * rows;
        if (source.equals(typed)) {
            // 講完了。塞不進框裡就不換——中文通常比英文短，真的塞不下時，
            // 讓玩家看見完整的英文，比看見被切掉一半的中文好。
            return measure(hit, style) <= limit ? hit : null;
        }
        // 還在逐字打字。譯文也照同樣的進度一個字一個字出來，看起來就跟原文一樣。
        //
        // 先前這裡是拿<b>完整</b>譯文去比對「塞不塞得下目前這幾行」——而打到一半時
        // 通常只有一行，完整譯文要兩行，於是整句退回英文，要等最後一行出現才
        // 忽然跳成中文。玩家看到的「講到一半翻譯失效」就是這個。
        String part = typedSoFar(hit, typed.length(), source.length());
        while (!part.isEmpty() && measure(part, style) > limit) {
            part = typedSoFar(part, part.length() - 1, part.length());
        }
        return part.isEmpty() ? null : part;
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

    /**
     * 把像素數編回位移字元；編不出來時回傳 {@code null}。
     *
     * <h2>為什麼一定要擋範圍</h2>
     * 位移字元只有 {@code U+CF000}–{@code U+D1000} 這一段有定義（見
     * {@link #offsetOf} 解碼時的檢查），也就是 ±4096 像素。超出去的碼位
     * <b>資源包裡根本沒有那個字</b>，Minecraft 只好畫成缺字的方框——
     * 而位移字元常常是<b>一整串</b>的，於是畫面上就出現一長排方框。
     *
     * <p>先前這裡沒有檢查，只有解碼那一端有。補正值是「原本的偏移 + 這一段
     * 縮短了多少」算出來的，長句子、或量錯寬度的時候就可能衝出去。
     *
     * <p>回傳 {@code null} 的時候呼叫端一律<b>放棄那一段的替換</b>：
     * 留著英文原文至少版面是對的，比推歪加一排方框好得多。
     */
    static String offset(int px) {
        int cp = OFFSET_BASE + px;
        if (cp < 0xCF000 || cp > 0xD1000) {
            return null;
        }
        return new String(Character.toChars(cp));
    }

    /**
     * 這一段文字<b>實際畫出來</b>會有多寬。
     *
     * <p>不能一律拿預設字型去量：中日韓走的是我們自己那套 Cubic 11，
     * 字寬跟預設的和 Wynncraft 的都不一樣。量錯了尾隨偏移就補錯，
     * 後面整塊會跟著偏。
     */
    /** 傳得到樣式就照樣式量，傳不到才退回預設字型。 */
    private static int measure(String text, Style style) {
        return style == null ? width(text) : width(text, style);
    }

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
    /**
     * 那一套字型畫不畫得出這一串。
     *
     * <h2>為什麼要擋</h2>
     * 點陣字是一個字一個字畫出來的，沒有哪一套是全的——Cubic 11 收了兩萬多個
     * 漢字裡的九千多個。收不到的字畫出來是<b>方框</b>，而一句話裡插幾個方框
     * 比整句留著英文糟糕得多。
     *
     * <p>所以缺一個字就整段不換。區間表由 {@code tools/font-coverage.py}
     * 從字型檔本身抽出來，跟著字型一起放進 jar，換字型時一起重產。
     */
    /** 這一串在對話框裡畫不畫得出來——Wynncraft 自己那份或我們補的那套，有一個能畫就算。 */
    static boolean renderable(String text) {
        return drawable(text) || covered(text, WynnChaYuan.language());
    }

    static boolean covered(String text, String lang) {
        int[] ranges = coverage(lang);
        if (ranges.length == 0) {
            return true;                       // 讀不到就不擋，維持原本的行為
        }
        return text.codePoints().allMatch(cp -> {
            if (cp < 0x80) {
                return true;
            }
            for (int i = 0; i < ranges.length; i += 2) {
                if (cp >= ranges[i] && cp <= ranges[i + 1]) {
                    return true;
                }
            }
            return false;
        });
    }

    private static int[] coverage(String lang) {
        int[] known = COVERAGE.get(lang);
        if (known != null) {
            return known;
        }
        List<Integer> flat = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        try {
            var id = Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID,
                    "font/dialogue/" + lang + "/coverage.txt");
            var found = mc.getResourceManager().getResource(id);
            if (found.isPresent()) {
                try (var in = found.get().openAsReader()) {
                    for (String line : in.lines().toList()) {
                        if (line.isBlank() || line.startsWith("#")) {
                            continue;
                        }
                        int dash = line.indexOf('-');
                        flat.add(Integer.parseInt(line.substring(0, dash), 16));
                        flat.add(Integer.parseInt(line.substring(dash + 1), 16));
                    }
                }
            }
        } catch (Exception e) {
            // 讀不到就當作沒有限制，維持原本的行為
        }
        int[] out = new int[flat.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = flat.get(i);
        }
        COVERAGE.put(lang, out);
        return out;
    }

    /**
     * 這一段該配的中文字型<b>本來就該有，卻沒打包進來</b>。
     *
     * <h2>為什麼要跟 paired() 分開問</h2>
     * {@link #paired} 回 {@code null} 有兩種意思，後果差很多：
     * <ul>
     *   <li>這個語言<b>沒附字型</b>（日文、韓文、俄文……）——退回預設字型，
     *       位置會掉，但字看得見。</li>
     *   <li>這個語言附了字型，<b>就是少了這一份</b>——那退回去的會是一個
     *       Minecraft 根本沒有的字型 id，而 {@code FontManager} 對查不到的 id
     *       是拿 {@code AllMissingGlyphProvider} 頂上，也就是
     *       <b>每一個字都畫成方框</b>。</li>
     * </ul>
     *
     * <p>後者正是對話框有五行（{@code body_0}…{@code body_4}）、選項從
     * {@code choice_0} 起算，而我們只做到 {@code body_3} 與 {@code choice_1}
     * 時發生的事：話一長到第五行、選項一滿四個，那一列就整排方框。
     * 缺字型跟缺字一樣，寧可整段留著英文。
     */
    static boolean fontMissing(String font) {
        String lang = WynnChaYuan.language();
        if (!SHIPPED.contains(lang)) {
            return false;                  // 本來就沒附，走既有的「退回預設字型」
        }
        String name = pairedName(font);
        return name != null && !fontShipped(lang, name);
    }

    /** 這一份字型檔真的在 jar 裡嗎。問一次就記起來，每一幀都查太貴。 */
    private static boolean fontShipped(String lang, String name) {
        return FONTS.computeIfAbsent(lang + "/" + name, key -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return true;               // 測試環境沒有資源管理員，不擋
            }
            var id = Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID,
                    "font/dialogue/" + key + ".json");
            return mc.getResourceManager().getResource(id).isPresent();
        });
    }

    /** 這一段原本的字型對應到我們哪一份中文字型；只有檔名，沒有命名空間。 */
    static String pairedName(String font) {
        String name = null;
        if (font.contains(BODY)) {
            int at = font.lastIndexOf(BODY);
            name = "body_" + font.substring(at + BODY.length());
        } else if (font.contains(CHOICE)) {
            int at = font.lastIndexOf(CHOICE);
            name = "choice_" + font.substring(at + CHOICE.length());
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
        return clean.isEmpty() ? null : clean.toString();
    }

    private static FontDescription paired(String font) {
        String clean = pairedName(font);
        if (clean == null) {
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
