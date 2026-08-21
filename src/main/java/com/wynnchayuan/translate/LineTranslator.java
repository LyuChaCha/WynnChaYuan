package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import net.minecraft.client.Minecraft;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * 把一行原文換成譯文，同時保住顏色與符號。
 *
 * <h2>顏色可以抄，字型不能</h2>
 * 這是整個顯示層最重要的一條規則。自訂字型（{@code common} 那些）的材質裡<b>只有那些
 * 符號的圖，沒有中文字</b>；把中文塞進帶自訂字型的樣式，畫出來就是缺字方框。
 *
 * <p>但顏色、粗體、斜體是渲染屬性，跟字型無關，抄過去完全安全——而且 Wynncraft 的
 * 顏色是有意義的（元素色、稀有度色），丟掉很可惜。所以：
 *
 * <ul>
 *   <li><b>文字</b>：抄顏色與粗斜體，字型強制 {@link FontDescription#DEFAULT}</li>
 *   <li><b>符號</b>：整段原封不動搬回來，<b>連原本的字型一起</b>——不重建，
 *       所以材質包怎麼畫就怎麼畫，不可能破圖</li>
 *   <li><b>數值</b>：填回原本的數字，沿用它自己的樣式（同樣強制預設字型）</li>
 * </ul>
 *
 * <p>佔位符數量對不上就整行放棄，回傳 {@code null} 讓呼叫端顯示灰色原文。
 * 寧可不翻，也不要畫出錯位的東西。
 */
public final class LineTranslator {

    private LineTranslator() {}

    /**
     * 一次翻一整段：把連續好幾行併成<b>一個鍵</b>去查。
     *
     * <h2>為什麼需要這條路</h2>
     * 技能說明的原文本來就是一整段，官方 CDN 給的是含換行的一大串：
     *
     * <pre>
     *   Forms a shield around you that slightly reduces
     *   the damage you take. Being hit will consume
     *   one charge of the shield.
     * </pre>
     *
     * 遊戲畫面上是三行，鍵卻是這三行黏在一起的樣子。逐行查表<b>永遠查不到</b>，
     * 而且失敗得無聲無息——{@code ability.json} 明明填了譯文卻毫無反應，
     * 就是卡在這裡。
     *
     * <p>譯文的行數必須與原文一致（{@code tools/validate.py} 會擋），
     * 於是可以逐行填回原本的排版，對齊照舊由 {@link #realign} 處理。
     *
     * @param centered 與 {@code run} 等長，見 {@link BlockLayout}
     * @return 譯好的那幾行；沒有對應的整段條目時回傳 {@code null}
     */
    public static List<Component> translateBlock(List<StyledText> run,
                                                 TranslationStore store,
                                                 boolean[] centered) {
        if (run.size() < 2) {
            return null;                       // 單行走原本那條路就好
        }
        List<LineParts> parts = new ArrayList<>(run.size());
        StringBuilder key = new StringBuilder();
        for (StyledText line : run) {
            LineParts p = LineParts.of(line);
            if (p.template().isBlank()) {
                // 段落之間的空行不能併進來。攤平查表會把它壓成一個空格，
                // 於是整段連著空行一起被換掉——譯文出來就跟上一段黏在一起了。
                return null;
            }
            parts.add(p);
            if (key.length() > 0) {
                key.append('\n');
            }
            key.append(p.template());
        }
        String template = key.toString();
        if (!GlyphSplitter.hasLetter(template)) {
            return null;
        }
        String translated = store.lookup(template);
        List<LineParts.Piece> extra = List.of();
        boolean flowed = false;
        if (translated == null || translated.isBlank()) {
            // 原文可能是被 tooltip 寬度自動斷行的，斷點跟語料對不上。
            // 把整段攤平成一行再查一次。
            Flowed hit = lookupFlowedParts(template, store);
            translated = hit == null ? null : hit.text();
            flowed = hit != null;
            if (hit != null && hit.label() != null) {
                // 名稱那半在原文裡有自己的顏色（Major ID 的名稱是粉紅的），
                // 而譯文是中文、跟原文對不起來，一般的樣式沿用比對不到。
                // 這裡知道譯出來的名稱長什麼樣，直接當成一個「原樣出現的詞」交上去。
                extra = labelAccent(hit.label(), parts.get(0));
            }
        }
        if (translated == null || translated.isBlank()) {
            return null;
        }
        if (flowed) {
            // 查到的是完整一句，得自己折回原本那幾行的寬度
            translated = wrapToBlock(translated, run);
        }
        String[] dst = translated.split("\n", -1);
        List<Component> built = rebuildAll(dst, parts, extra, store);
        if (built == null) {
            return null;                       // 佔位符對不上，整段放棄
        }
        if (dst.length != run.size()) {
            // 中文比英文緊湊，兩行的句子常常一行就講完。譯文面板是我們自己畫的，
            // 行數不一樣沒關係——只是少了「原本那一行」可以拿來對齊，直接照原樣出。
            return built;
        }
        List<Component> out = new ArrayList<>(run.size());
        for (int i = 0; i < run.size(); i++) {
            out.add(realign(run.get(i), built.get(i), centered[i]));
        }
        return out;
    }

    /**
     * 查一段被自動斷行的長文字。
     *
     * <h2>兩種查法</h2>
     * <ol>
     *   <li><b>整段</b>攤平之後直接查。技能與物品的長敘述屬於這種。</li>
     *   <li><b>「名稱：說明」</b>拆成兩半各查一次。Major ID 長這樣：
     *       {@code Altruism: Allies within 16 blocks gain...}——名稱與說明在語料裡
     *       是<b>兩筆</b>，畫面上卻擠在同一行，整段查永遠查不到。</li>
     * </ol>
     *
     * <p>第二種看起來寬鬆，其實很安全：要誤中的話，冒號兩邊<b>都</b>得剛好是
     * 語料裡的條目；真的都在，那分別翻譯本來就是對的。
     */
    /**
     * @param text 整段譯文
     * @param label 「名稱：說明」的名稱那半；整段查到的情況是 {@code null}
     */
    record Flowed(String text, String label) {}

    static String lookupFlowed(String template, TranslationStore store) {
        Flowed flowed = lookupFlowedParts(template, store);
        return flowed == null ? null : flowed.text();
    }

    static Flowed lookupFlowedParts(String template, TranslationStore store) {
        String whole = store.lookupFlat(template);
        if (whole != null) {
            return new Flowed(whole, null);
        }
        int colon = template.indexOf(": ");
        if (colon <= 0 || colon > MAX_LABEL_LENGTH) {
            return null;
        }
        String head = lookup(template.substring(0, colon), store);
        if (head == null || head.isBlank()) {
            return null;
        }
        String rest = template.substring(colon + 2);
        String tail = store.lookupFlat(rest);
        if (tail == null) {
            tail = store.lookup(rest);
        }
        return tail == null || tail.isBlank()
                ? null : new Flowed(head + ": " + tail, head);
    }

    /**
     * 整段共用的文字樣式：<b>涵蓋字數最多</b>的那一個。
     *
     * <h2>為什麼不能只看第一行</h2>
     * 攤平查表出來的是一整句，重建時整段共用一個樣式。先前取第一行的——
     * 但第一行常常是「名稱：」那半，顏色跟後面的說明不一樣（Major ID 的名稱
     * 是粉紅的、技能說明是綠的），於是整段都被染成名稱的顏色，
     * 說明的顏色就消失了。
     *
     * <p>改成看整段：哪個樣式涵蓋的字最多就用哪個。名稱那半會由
     * {@link #labelAccent} 另外把自己的顏色帶回去。
     */
    private static Style dominantStyle(List<LineParts> parts) {
        java.util.Map<Style, Integer> weight = new java.util.LinkedHashMap<>();
        for (LineParts part : parts) {
            weight.merge(part.textStyle(), part.template().length(), Integer::sum);
        }
        Style best = parts.get(0).textStyle();
        int most = -1;
        for (java.util.Map.Entry<Style, Integer> e : weight.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /**
     * 把譯出來的名稱包成一個「原樣出現的詞」，帶著原文名稱的樣式。
     *
     * <p>拿的是<b>第一個</b>與整行主樣式不同的片段——「名稱：說明」的名稱就在
     * 行首，而它跟後面的說明顏色不同，正是這樣才會被記成 accent。
     * 找不到就回傳空的，那一段照主樣式畫，不會比現在更糟。
     */
    private static List<LineParts.Piece> labelAccent(String label, LineParts first) {
        // 前面的 ✦ 或 {#} 要拿掉再比對。{#} 在重建時是<b>獨立的一個 token</b>，
        // 連著它一起比對永遠對不上，名稱的顏色就這樣掉了。
        String core = label;
        while (!core.isEmpty()) {
            if (core.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                core = core.substring(GlyphSplitter.GLYPH_PLACEHOLDER.length());
            } else if (Character.isWhitespace(core.charAt(0))
                    || isDecoration(core.codePointAt(0))) {
                core = core.substring(Character.charCount(core.codePointAt(0)));
            } else {
                break;
            }
        }
        if (core.isBlank() || first.accents().isEmpty()) {
            return List.of();
        }
        return List.of(new LineParts.Piece(core, first.accents().get(0).style()));
    }

    /** 「名稱：說明」的名稱最長到這裡。再長就不像標題了。 */
    private static final int MAX_LABEL_LENGTH = 40;

    /** 這幾行裡最寬的一行有多寬。折行時當成目標寬度。 */
    private static int widestOf(List<StyledText> run) {
        int widest = 0;
        for (StyledText line : run) {
            widest = Math.max(widest, widthOf(line.getComponent()));
        }
        return widest;
    }

    /**
     * 折成跟原本那一塊<b>一樣的形狀</b>。
     *
     * <h2>為什麼不能只看寬度</h2>
     * 目標寬度是量原文量出來的，而原文用的是 Wynncraft 自己的字型；量不準的話
     * 折出來的行數就會比原本多，整塊往下長，看起來就「跑偏」了。
     *
     * <p>行數是<b>看得出來對不對</b>的：原本四行，折出來就不該超過四行。
     * 超過就把目標寬度放寬一成再試——這樣量得再不準也收得回來。
     */
    private static String wrapToBlock(String text, List<StyledText> run) {
        int width = widestOf(run);
        if (width <= 0) {
            return text;
        }
        String wrapped = wrapToWidth(text, width);
        for (int attempt = 0; attempt < WRAP_RETRIES && lines(wrapped) > run.size(); attempt++) {
            width = width * 11 / 10;
            wrapped = wrapToWidth(text, width);
        }
        return wrapped;
    }

    /** 放寬幾次就放棄。每次一成，五次約多五成，量錯到這個程度另有問題。 */
    private static final int WRAP_RETRIES = 5;

    private static int lines(String text) {
        int n = 1;
        for (int i = text.indexOf(NEWLINE); i >= 0; i = text.indexOf(NEWLINE, i + 1)) {
            n++;
        }
        return n;
    }

    /**
     * 把一整句折成幾行，每行不超過 {@code maxPx}。
     *
     * <h2>為什麼要自己折</h2>
     * 查到的譯文是完整一句，畫面上原本卻是好幾行。直接畫成一行會把面板撐得
     * 比原本的 tooltip 還寬。
     *
     * <p>中文可以在任何字之間斷，英文不行——所以碰到英文單字時退回上一個空白。
     * 佔位符（{@code {~}}）整組不能拆開，拆了就填不回去。
     */
    private static String wrapToWidth(String text, int maxPx) {
        if (maxPx <= 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + 8);
        int lineStart = 0;
        int width = 0;
        int lastSpace = -1;
        int i = 0;
        while (i < text.length()) {
            int close = text.charAt(i) == '{' ? text.indexOf('}', i) : -1;
            int end = close > i
                    ? close + 1                          // 佔位符整組
                    : i + Character.charCount(text.codePointAt(i));
            String piece = text.substring(i, end);
            int pieceWidth = widthOf(Component.literal(piece));
            if (width + pieceWidth > maxPx && i > lineStart) {
                // 退回上一個空白是為了不把<b>英文單字</b>切成兩半。中文可以在
                // 任何字之間斷，退回去只會讓整行提早結束——「✦ 利他主義: 16」
                // 之後就換行、剩下的擠成三行，就是這樣來的。
                int cut = breaksWord(text, i) && lastSpace > lineStart ? lastSpace : i;
                out.append(text, lineStart, cut).append(NEWLINE);
                lineStart = text.charAt(cut) == ' ' ? cut + 1 : cut;
                width = widthOf(Component.literal(text.substring(lineStart, i)));
                lastSpace = -1;
            }
            if (" ".equals(piece)) {
                lastSpace = i;
            }
            width += pieceWidth;
            i = end;
        }
        return out.append(text, lineStart, text.length()).toString();
    }

    /** 在這裡斷行會不會把一個英文單字切成兩半。 */
    private static boolean breaksWord(String text, int at) {
        return at > 0 && at < text.length()
                && isWordChar(text.charAt(at - 1)) && isWordChar(text.charAt(at));
    }

    /** 不能從中間切開的字元：英數與連接號。中文不算。 */
    private static boolean isWordChar(char c) {
        return c < 0x2E80 && (Character.isLetterOrDigit(c) || c == '-' || c == '\'');
    }

    private static final char NEWLINE = '\n';

    /**
     * @return 譯好的一行；查不到翻譯或佔位符對不上時回傳 {@code null}
     */
    public static Component translate(StyledText line, TranslationStore store) {
        return translate(line, store, true);
    }

    /**
     * @param centered 這一行是不是置中的（見 {@link BlockLayout}）。
     *                 靠左的行不能動前導縮排，動了整段就歪掉。
     */
    public static Component translate(StyledText line, TranslationStore store,
                                      boolean centered) {
        Component whole = translateWholeLine(line, store, centered);
        return whole != null ? whole : translateSegments(line, store, centered);
    }

    /**
     * 逐片段替換：保留原本的元件結構，只換掉查得到的文字片段。
     *
     * <h2>為什麼這才是保真的做法</h2>
     * 另一條路（{@link #translateWholeLine}）是把整行拆成模板再重建，格式保不保得住
     * 完全取決於重建得多完美——而實際的 tooltip 一行裡可能混用五、六種字型
     * （{@code space} 排版、{@code banner/box} 外框、{@code tooltip/divider} 分隔線、
     * {@code language/wynncraft} 文字…），任何一點還原不精確就會變成方框或錯位。
     *
     * <p>逐片段替換不重建任何東西：不是文字的片段<b>原封不動抄過去</b>，
     * 連字型、顏色、負寬度空白都保持原樣，所以版面天然就是對的。
     * 只有查得到譯文的那些片段會被換掉，而且只有那些片段需要改成預設字型。
     *
     * <p>代價是無法跨片段調整語序（例如把「Combat Experience +6%」整句重排）。
     * 對「標籤 + 數值」這種結構沒有影響，也正是物品 tooltip 的主要形態。
     */
    private static Component translateSegments(StyledText line, TranslationStore store,
                                               boolean centered) {
        // 先把每個片段翻好，不急著組裝——寬度要等整行都翻完才量得準。
        List<Piece> pieces = new ArrayList<>();
        boolean any = false;
        boolean percent = hasPercentValue(line);

        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();

            // 空白字型底下也可能掛著不是寬度偏移的字元（材質包自訂圖示）。
            // 那種重新編碼會變成別的東西，所以只有整段都是偏移的才當空白。
            if (isAdjustableSpace(style, raw)) {
                pieces.add(Piece.space(SpaceOffset.decode(raw), style));
                continue;
            }
            // 符號、純空白、查不到的文字 —— 一律原封不動，含字型
            if (GlyphSplitter.isGlyphPart(part) || raw.isBlank()) {
                pieces.add(Piece.text(raw, style));
                continue;
            }
            // 欄位間隔有時直接接在標籤後面，同一個片段裡，例如
            // 「Durability + 往回 41 + 往前 145」。那是間隔不是內容——
            // 包在文字片段裡的話補償程式碰不到它，譯文變短數值就往左跑。
            String tail = SpaceOffset.trailingOffsets(raw);
            String body = raw.substring(0, raw.length() - tail.length());

            Component replaced = body.isEmpty()
                    ? null : translateOneSegment(body, style, store, percent);
            if (replaced == null) {
                pieces.add(Piece.text(raw, style));
                continue;
            }
            any = true;
            // rebuild 已經把文字改成預設字型、圖示與偏移保留原字型，
            // 這裡不能再整段重新上色/換字型，否則偏移會失去寬度
            pieces.add(Piece.translated(replaced, literal(body, style)));
            if (!tail.isEmpty()) {
                // 用 space 字型重新編碼：那是保證認得偏移碼位的字型
                pieces.add(Piece.space(SpaceOffset.decode(tail),
                        SpaceOffset.styleFor(style)));
            }
        }
        if (!any) {
            return null;
        }

        // 標籤與數值的交界若還沒有對齊空白，就補一個。
        //
        // 「整行有沒有空白」是不夠的判斷：素材那種行在標籤與第一個數值之間
        // 沒有空白（padding 塞在標籤片段裡），只有兩個數值之間有。看到後面
        // 那個就以為不用補的話，差額會全部灌進第二欄——標籤和第一個數值黏在
        // 一起，第二欄則被推過頭。要看的是<b>交界那個位置</b>本身。
        int boundary = findAlignPoint(pieces);
        if (boundary >= 0 && !pieces.get(boundary).isSpace()
                && hasColumnGap(pieces, boundary)) {
            pieces.add(boundary, Piece.space(0,
                    SpaceOffset.styleFor(pieces.get(boundary).style())));
        }

        List<Piece> aligned = settle(alignColumns(pieces), line);
        // 每一行都記（同一句只記一次，見 LineDebug）。先前只記「有排版空白」的行，
        // 結果真正壞掉的那些——間隔不是用空白字元做的——反而完全看不到。
        LineDebug.pieces("逐片段 " + line.getStringWithoutFormatting(),
                describe(pieces, aligned, line));

        // 只有前導空白的行是「置中／縮排」而不是「標籤 + 數值」。
        // 這種行沒有兩側都有文字的空白，上面那一輪不會動到它，
        // 所以另外量整行、補一半 —— 補滿會把整行推到右邊去。
        int lead = centered ? leadingSpace(aligned) : -1;
        if (lead >= 0) {
            int delta = widthOf(line) - widthOf(assemble(aligned, -1, 0));
            return assemble(aligned, lead, delta / 2);
        }
        return assemble(aligned, -1, 0);
    }

    /**
     * 讓每一個對齊欄後面的內容，落回與原文相同的水平位置。
     *
     * <h2>為什麼要逐欄，不能只調一個</h2>
     * 素材的數值行長這樣：
     *
     * <pre>
     *   Spell Damage  [對齊空白 A]  +60 to  [對齊空白 B]  +75
     * </pre>
     *
     * 兩個空白各自負責一欄：A 讓「最小值」對齊，B 讓「最大值」對齊。
     * 只調其中一個的話，另一欄就會跑掉——而且<b>整行的總寬度仍然是對的</b>，
     * 所以「量整行寬度」那種自我檢查完全看不出問題，只有肉眼盯著才發現
     * 數值黏在標籤旁邊、右邊空一大塊。
     *
     * <p>做法是由左往右累計「翻譯後比原文寬了多少」（drift），每碰到一個
     * 對齊空白就把累計的差額從它身上扣掉，然後歸零。這樣每一欄的起點都會
     * 精準回到原文的位置，有幾欄就補幾次。
     *
     * <p>只補「兩側都有文字」的空白：行尾的留白邊距、行首的縮排都不是欄位
     * 交界，動它們只會讓整行位移。
     */
    private static List<Piece> alignColumns(List<Piece> pieces) {
        List<Piece> out = new ArrayList<>(pieces.size());
        int drift = 0;
        int lastAdjusted = -1;
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (!p.isSpace()) {
                drift += widthOf(p.rendered()) - widthOf(p.orig());
                out.add(p);
                continue;
            }
            if (isAlignSpace(pieces, i)) {
                // 不夾在 0 以上。Wynncraft 自己就在用負間隔——坐騎的
                // 屬性列原文就是「-25px 再 +104px」這種寫法，夾成 0 之後
                // 整行會多出 20 幾像素，八行一起往右跑。
                out.add(Piece.space(narrowed(p.spacePx(), p.spacePx() - drift), p.style()));
                lastAdjusted = i;
                drift = 0;
            } else {
                out.add(p);
            }
        }

        // 最後一欄的<b>右緣</b>也要對回去。
        //
        // 上面那一輪只把「空白之前」的寬度變化補掉，所以數值的<b>起點</b>會落回
        // 原位——但數值本身如果也被翻譯了（{@code Mage/Dark Wizard} →
        // {@code 法師/黑巫師}），它的<b>結尾</b>就短了一截，右緣對不齊。
        //
        // Wynncraft 是把數值靠右排的，所以把剩下的差額一併加回最後一個對齊空白，
        // 整行的總寬度就會跟原文相同，右緣自然重合。
        if (lastAdjusted >= 0 && drift != 0) {
            Piece p = out.get(lastAdjusted);
            out.set(lastAdjusted,
                    Piece.space(narrowed(p.spacePx(), p.spacePx() - drift), p.style()));
        }
        return out;
    }

    /**
     * 收尾：把<b>量出來</b>的殘差補回最後一個對齊欄。
     *
     * <h2>為什麼算完還要量</h2>
     * {@link #alignColumns} 是逐片段累加算出補償量的。只要有<b>任何一項</b>沒算進去
     * ——片段之間的字距、字型換掉造成的寬度差、編碼後空白字元的實際寬度——
     * 整行就會差那麼幾像素到幾十像素，而且完全看不出是哪一段漏了。
     *
     * <p>量整行則是把所有因素一次涵蓋。算對的話這裡的殘差是 0，什麼都不會動；
     * 算漏了就在這裡補回來。這是自我修正，不是另一套演算法。
     *
     * <p>只在有對齊欄時才動——沒有欄位的行（散文、標題）本來就不必守住寬度。
     */
    private static List<Piece> settle(List<Piece> pieces, StyledText original) {
        int last = -1;
        for (int i = 0; i < pieces.size(); i++) {
            if (isAlignSpace(pieces, i)) {
                last = i;
            }
        }
        if (last < 0) {
            return pieces;
        }
        int shortfall = widthOf(original.getComponent())
                - widthOf(assemble(pieces, -1, 0));
        if (shortfall == 0) {
            return pieces;
        }
        List<Piece> out = new ArrayList<>(pieces);
        Piece p = out.get(last);
        // 這裡也要夾住下限。alignColumns 夾過了，但這一步會再動一次同一個空白——
        // 技能樹的「風屬性傷害百分比+15%」就是從這個縫隙漏掉的：譯文比原文寬，
        // shortfall 是負的，間隔被收成 0，字直接貼在一起。
        out.set(last, Piece.space(narrowed(p.spacePx(), p.spacePx() + shortfall),
                                  p.style()));
        return out;
    }

    /** 兩側都有實際文字的空白，才是欄位交界。 */
    static boolean isAlignSpace(List<Piece> pieces, int index) {
        return pieces.get(index).isSpace()
                && !isLeading(pieces, index)
                && hasTextAfter(pieces, index);
    }

    /** 行首的縮排／置中空白；沒有就回傳 -1。 */
    private static int leadingSpace(List<Piece> pieces) {
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).isSpace() && isLeading(pieces, i)
                    && hasTextAfter(pieces, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 找出這一行該在哪裡撐開寬度，也就是「標籤」與「數值」的交界。
     *
     * <p>從尾端往回走，把「看起來像數值」的片段（{@code +372}、{@code [94.0%]}、
     * 純空白、對齊空白）都算進數值區，停在第一個真正的文字片段——那就是交界。
     *
     * <p>回傳的位置<b>可能已經是一個對齊空白</b>（伺服器插好的），那就直接用；
     * 也可能是數值本身，那就得自己補一個。呼叫端看那個位置是不是空白來決定。
     *
     * <p>刻意不做「整行找找看有沒有空白，有就用」——素材那種行在標籤與第一個
     * 數值之間沒有空白，只有兩個數值之間有。看到後面那個就以為不用補的話，
     * 差額會全部灌進第二欄。
     *
     * @return 要撐開的位置索引；沒有欄位結構時回傳 -1
     */
    static int findAlignPoint(List<Piece> pieces) {
        int firstAlign = firstAlignSpace(pieces);
        int valueStart = valueRegionStart(pieces);

        // 現成的空白在數值區<b>之前或正好在交界上</b>，它就是交界，直接用。
        // 「Class Type｜Archer/Hunter」這種數值是文字的行只能靠這條認出來。
        if (firstAlign >= 0 && (valueStart < 0 || firstAlign <= valueStart)) {
            return firstAlign;
        }
        // 現成的空白在數值區裡面（素材的兩欄行就是這樣：標籤後面沒有空白，
        // 只有兩個數值之間有）。那個不是標籤與數值的交界，要在數值區起點補。
        if (valueStart >= 0) {
            return valueStart;
        }
        // 最後一種：間隔是<b>字面空格</b>、數值又是<b>文字</b>，兩個條件都躲過
        // 上面的判斷。{@code Class Type␠␠Mage/Dark Wizard} 就是這樣——整行沒有
        // 排版空白片段，數值區也找不到數字，於是一路回傳 -1，完全沒被補償。
        return literalGapBoundary(pieces);
    }

    /**
     * 以<b>字面空格</b>當間隔的欄位交界。
     *
     * <p>條件是：一整段都是空白的片段，前後都有實際內容，而且夠寬
     * （見 {@link #MIN_COLUMN_GAP}）。一格空格是詞距，不是欄位。
     *
     * <p>回傳空白<b>之後</b>那個位置——補償要加在數值前面，才會把它推到定位。
     */
    private static int literalGapBoundary(List<Piece> pieces) {
        for (int i = pieces.size() - 2; i > 0; i--) {
            String text = pieces.get(i).text();
            if (text.isEmpty() || !text.isBlank() || text.length() < MIN_COLUMN_GAP) {
                continue;
            }
            if (!isLeading(pieces, i) && hasTextAfter(pieces, i)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * 間隔補償之後不能窄到把字黏在一起。
     *
     * <h2>為什麼需要下限</h2>
     * 中文標籤比英文長時，補償算出來是<b>負的</b>——技能樹的
     * {@code Fire Damage: +15%} 翻成「火屬性傷害百分比」之後就變成
     * 「火屬性傷害百分比+15%」，字直接貼在一起，甚至被推出框外。
     *
     * <p>但不能一律夾在 0 以上：Wynncraft <b>自己</b>就在用負間隔，坐騎的屬性列
     * 原文就是「-25px 再 +104px」，夾成 0 之後整行會多出二十幾像素。
     *
     * <p>所以只夾<b>原本是正的</b>那些。原本就是負的照原樣通過——那是排版設計，
     * 不是我們算出來的。對不齊也比黏在一起好讀。
     *
     * @param original 原文的間隔
     * @param adjusted 補償之後的間隔
     */
    private static int narrowed(int original, int adjusted) {
        return original > 0 ? Math.max(MIN_GAP, adjusted) : adjusted;
    }

    /** 見 {@link #narrowed}。一個半形空格的寬度。 */
    private static final int MIN_GAP = 4;

    /** 要幾格空白才算「這是一個對齊欄」而不只是詞與詞之間的間隔。 */
    private static final int MIN_COLUMN_GAP = 2;

    /**
     * 交界處原本就有夠寬的間隔嗎？
     *
     * <h2>為什麼要問這個</h2>
     * {@code Emerald Pouch [Tier 8]} 這種行，「數值」只是接在名稱後面<b>一個空格</b>，
     * 不是右對齊的欄位。把它當欄位補償的話，譯文變短就會把 {@code [Tier 8]}
     * 一路推到原文的右緣去——原本緊跟在名字後面的東西，突然飛到老遠。
     *
     * <p>真正的欄位在原文裡一定有一段明顯的留白（伺服器用它把數值排到定位）。
     * 所以用「間隔有幾格」來分：一格是詞距，兩格以上才是欄位。
     *
     * <p>已經是排版空白字元（{@code minecraft:space}）的情況不會走到這裡——
     * 那種本來就是欄位，呼叫端直接用。
     */
    static boolean hasColumnGap(List<Piece> pieces, int boundary) {
        int spaces = 0;
        // 交界前那一段的尾端空白
        for (int i = boundary - 1; i >= 0; i--) {
            String text = pieces.get(i).text();
            if (text.isEmpty()) {
                continue;
            }
            int n = 0;
            while (n < text.length()
                    && Character.isWhitespace(text.charAt(text.length() - 1 - n))) {
                n++;
            }
            spaces += n;
            if (n < text.length()) {
                break;                         // 不是整段空白，到此為止
            }
        }
        // 交界之後緊接著的整段空白
        for (int i = boundary; i < pieces.size(); i++) {
            String text = pieces.get(i).text();
            if (!text.isEmpty() && text.isBlank()) {
                spaces += text.length();
                continue;
            }
            break;
        }
        if (spaces >= MIN_COLUMN_GAP) {
            return true;
        }
        // 標籤長到剛好把數值頂到欄位上時，中間只會剩一個空格——
        // 「Elemental Spell Damage +372」就是這樣，它跟同一份 tooltip 裡
        // 其他詞條一樣寬，是不折不扣的欄位，卻因為只有一格而被擋掉。
        //
        // 用「數值以正負號開頭」把它跟「Emerald Pouch [Tier 8]」分開：
        // 詞條的數值一定帶正負號，接在名稱後面的標籤則不會。
        return spaces >= 1 && startsWithSign(pieces, boundary);
    }

    private static boolean startsWithSign(List<Piece> pieces, int boundary) {
        if (boundary >= pieces.size()) {
            return false;
        }
        String text = pieces.get(boundary).text().strip();
        return !text.isEmpty() && (text.charAt(0) == '+' || text.charAt(0) == '-');
    }

    /** 把補償前後的每個片段列出來，供診斷用。 */
    private static String describe(List<Piece> before, List<Piece> after,
                                   StyledText original) {
        StringBuilder sb = new StringBuilder();
        // 整行寬度有沒有守住，是「這行對齊對不對」最直接的答案。
        // 逐片段的數字要自己加總才看得出來，加總錯了又很難察覺。
        int want = widthOf(original.getComponent());
        int got = widthOf(assemble(after, -1, 0));
        sb.append(String.format("  整行寬度 %d -> %d%s%n", want, got,
                want == got ? "  (守住)" : "  ← 差 " + (got - want) + " px"));
        for (int i = 0; i < after.size(); i++) {
            Piece b = i < before.size() ? before.get(i) : null;
            Piece a = after.get(i);
            if (a.isSpace()) {
                sb.append(String.format("  [%d] 空白 %s px -> %s px%s%n", i,
                        b == null ? "?" : String.valueOf(b.spacePx()),
                        String.valueOf(a.spacePx()),
                        isAlignSpace(after, i) ? "  (對齊欄)" : "  (不調整)"));
            } else {
                // 顏色也要印。顏色掉了在畫面上很明顯，但先前的診斷完全看不到，
                // 只能靠截圖猜——猜了好幾輪都沒猜中。
                sb.append(String.format("  [%d] 文字 「%s」 <- 「%s」 寬 %d -> %d  色 %s -> %s%n",
                        i, a.text(), a.orig().getString(),
                        widthOf(a.orig()), widthOf(a.rendered()),
                        colourOf(a.orig()), colourOf(a.rendered())));
            }
        }
        return sb.toString();
    }

    /** 這一段的顏色，寫成 {@code #RRGGBB}；沒有指定就是「繼承」。 */
    private static String colourOf(Component component) {
        if (component == null) {
            return "-";
        }
        String[] found = {"繼承"};
        component.visit((style, text) -> {
            if (!text.isEmpty() && style.getColor() != null) {
                found[0] = style.getColor().serialize();
                return java.util.Optional.of(true);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    /** 由前往後第一個「兩側都有文字」的空白。 */
    private static int firstAlignSpace(List<Piece> pieces) {
        for (int i = 0; i < pieces.size(); i++) {
            if (isAlignSpace(pieces, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 數值區從哪裡開始。
     *
     * <p>從尾端往回走，把「看起來像數值」的片段都算進去，停在第一個真正的
     * 文字片段。數值是文字（{@code Archer/Hunter}）時認不出來，回傳 -1，
     * 那種情況靠現成的對齊空白判斷。
     */
    private static int valueRegionStart(List<Piece> pieces) {
        int i = pieces.size() - 1;
        while (i >= 0 && looksLikeValue(pieces.get(i).text())) {
            i--;
        }
        int valueStart = i + 1;
        if (i < 0 || valueStart >= pieces.size()) {
            return -1;                         // 全是數值或全是文字，沒有欄位結構
        }
        // 數值區裡必須真的有數字。否則像「1 x Doom Stone ◆◆◆」這種
        // 以符號結尾的行也會被當成欄位，插進去的空白只會把符號推歪。
        for (int j = valueStart; j < pieces.size(); j++) {
            if (pieces.get(j).text().chars().anyMatch(Character::isDigit)) {
                return valueStart;
            }
        }
        return -1;
    }

    /** 這個位置之後還有沒有實際文字。沒有的話它只是行尾的留白邊距。 */
    private static boolean hasTextAfter(List<Piece> pieces, int index) {
        for (int i = index + 1; i < pieces.size(); i++) {
            if (!pieces.get(i).isSpace() && !pieces.get(i).text().isBlank()) {
                return true;
            }
        }
        return false;
    }

    /** 這個空白之前有沒有任何文字。沒有的話它是縮排／置中用的。 */
    static boolean isLeading(List<Piece> pieces, int index) {
        for (int i = 0; i < index; i++) {
            if (!pieces.get(i).isSpace() && !pieces.get(i).text().isBlank()) {
                return false;
            }
        }
        return true;
    }

    /** 這段文字看起來是數值而不是文案。 */
    private static boolean looksLikeValue(String text) {
        String t = text.strip();
        if (t.isEmpty()) {
            return true;                       // 純空白，算在數值區
        }
        char c = t.charAt(0);
        if (c == '+' || c == '-' || c == '[' || c == '(') {
            return true;
        }
        // 範圍的連接詞。「-2414 to -1300」是<b>一個</b>數值，不是兩欄——
        // 從後面往回找數值區時停在 to，數值區就只剩 -1300，
        // 補償灌進 to 後面，畫面上就成了「-2414 to      -1300」。
        //
        // 只認整段剛好是 to 的。「to sell (」那種後面還接著別的，不算。
        if (t.equalsIgnoreCase("to")) {
            return true;
        }
        // 純數字、百分比、分數這類
        return t.chars().noneMatch(Character::isLetter);
    }

    /**
     * 把片段組回一個 Component。
     *
     * @param adjustIndex 要調整寬度的空白字元索引，{@code -1} 表示都不調
     * @param adjustPx    調整量（像素）
     */
    private static Component assemble(List<Piece> pieces, int adjustIndex, int adjustPx) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (!p.isSpace()) {
                out.append(p.rendered());
                continue;
            }
            int px = p.spacePx() + (i == adjustIndex ? adjustPx : 0);
            String encoded = SpaceOffset.encode(px);
            if (!encoded.isEmpty()) {
                out.append(literal(encoded, p.style()));
            }
        }
        return out;
    }

    /**
     * 尾端的冒號算標點不算內容。
     *
     * <p>遊戲裡的 Major ID 顯示成 {@code "Altruism: "}，但語料的鍵是
     * {@code "Altruism"}——只差一個冒號就查不到。剝掉再查，冒號本身會
     * 原樣接回去（見 {@link #lookup} 的 prefix/suffix）。
     *
     * <p>只在<b>完全比對失敗之後</b>才會走到這裡，所以像 {@code "Rewards:"}
     * 這種鍵本身就帶冒號的條目不受影響。
     */
    private static boolean isTrailingColon(char c) {
        return c == ':' || c == '：';
    }

    /** 整段文字畫出來有多寬（像素）。 */
    private static int widthOf(Component component) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.font == null ? 0 : mc.font.width(component);
    }

    /** 原始行的寬度。 */
    private static int widthOf(StyledText line) {
        return widthOf(line.getComponent());
    }

    /** 組裝前的一個片段：一般文字，或一個帶寬度的對齊空白。 */
    /**
     * 一行拆出來的片段。開放到 package 層級是為了讓
     * {@code AlignPointTest} 測得到 {@link #findAlignPoint}——對齊是這個專案
     * 反覆出問題的地方，而畫面上要靠肉眼比對幾個像素，很難察覺。
     */
    record Piece(Component rendered, Component orig, Style style, int spacePx, boolean isSpace) {

        /**
         * 沒有被翻譯的片段：原封不動，連字型都不動。
         */
        static Piece text(String t, Style s) {
            Component c = literal(t, s);
            return new Piece(c, c, s, 0, false);
        }

        /**
         * 翻譯過的片段。
         *
         * <p>存的是 {@link Component} 而不是字串——這一段裡面可能<b>混著圖示與
         * 排版偏移</b>，它們各自有自己的字型。先前這裡存字串，等於把字型全部
         * 壓掉，再整段套上預設字型；排版偏移字元在預設字型底下<b>沒有寬度</b>，
         * 於是欄位間隔整個消失，數值黏死在標籤旁。
         *
         * <p>原文也存 Component，因為量原始寬度必須用<b>原本的字型</b>——
         * 用譯文的字型去量原文，算出來的差額是錯的。
         */
        static Piece translated(Component rendered, Component orig) {
            return new Piece(rendered, orig, Style.EMPTY, 0, false);
        }

        static Piece space(int px, Style s) {
            return new Piece(null, null, s, px, true);
        }

        /** 純文字，供「這看起來像數值嗎」之類的判斷使用。 */
        String text() {
            return rendered == null ? "" : rendered.getString();
        }
    }

    /**
     * 翻一個文字片段，並保留它原本的前後空白。
     *
     * <p>空白會影響版面（Wynncraft 用它對齊），所以只拿中間的實際內容去查，
     * 查到之後把空白原樣接回。
     */
    /**
     * 這一行的<b>數值</b>是不是百分比。
     *
     * <h2>為什麼要分</h2>
     * 同一個標籤會有兩種形態：{@code Health Regen -605}（實數）與
     * {@code Health Regen -162%}（百分比）。英文靠數值本身就分得出來，
     * 中文卻常常需要不同的說法（「生命回復值」與「生命回復百分比」），
     * 而它們的標籤文字一模一樣，一條譯文沒辦法同時對。
     *
     * <p>所以看數值：以正負號開頭、只有數字的那一段，結尾有沒有 {@code %}。
     * 刻意不看整行有沒有 {@code %}——{@code +282 [53.8%]} 後面那個
     * 是詞條品質，不是數值本身。
     */
    private static boolean hasPercentValue(StyledText line) {
        for (StyledTextPart part : line) {
            String text = part.getString(null, StyleType.NONE).strip();
            if (text.isEmpty() || !VALUE.matcher(text).matches()) {
                continue;
            }
            return text.endsWith("%");
        }
        return false;
    }

    /** 純粹的數值：可帶正負號、千分位、小數，可帶結尾百分號。 */
    private static final java.util.regex.Pattern VALUE =
            java.util.regex.Pattern.compile("[+-]?\\d[\\d,.]*%?");

    /**
     * @param percent 這一行的數值是百分比。會先找「標籤 + {@code %}」的鍵，
     *                找不到才退回一般的鍵——所以沒有特地區分的標籤不受影響。
     */
    private static Component translateOneSegment(String raw, Style style,
                                                 TranslationStore store,
                                                 boolean percent) {
        String core = raw.strip();
        if (core.isEmpty() || !GlyphSplitter.hasLetter(core)) {
            return null;
        }
        int lead = raw.indexOf(core.charAt(0));
        String prefix = raw.substring(0, Math.max(0, lead));
        String suffix = raw.substring(prefix.length() + core.length());

        // 借用整行的機制處理片段內的數值與地名參數化
        StyledText one = StyledText.fromComponent(Component.literal(core).withStyle(style));
        LineParts parts = LineParts.of(one);
        String hit = lookup(parts.template(), store, percent);
        if (hit == null) {
            return null;
        }
        Component rebuilt = rebuild(hit, parts, store);
        if (rebuilt == null) {
            return null;
        }
        // 前後空白用<b>原本的樣式</b>接回去。那些空白也可能是排版偏移，
        // 改成預設字型就沒有寬度了。
        MutableComponent out = Component.empty();
        if (!prefix.isEmpty()) {
            out.append(literal(prefix, style));
        }
        out.append(rebuilt);
        if (!suffix.isEmpty()) {
            out.append(literal(suffix, style));
        }
        return out;
    }

    /** 整行查表。散文與對話用這條路，因為它們需要跨片段重排語序。 */
    private static Component translateWholeLine(StyledText line, TranslationStore store,
                                                boolean centered) {
        LineParts parts = LineParts.of(line);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;                       // 純符號或純數值的行，沒東西可翻
        }
        String translated = lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuild(translated, parts, store);
        if (rebuilt == null) {
            return null;
        }
        Component result = realign(line, rebuilt, centered);
        LineDebug.record(line, result);
        return result;
    }

    /**
     * 把整行重建的結果重新對齊回原文的版面。
     *
     * <h2>為什麼這條路也需要</h2>
     * {@link #rebuild} 會把原本的排版空白<b>原樣填回去</b>——那些偏移是按<b>英文的
     * 寬度</b>算好的。鑑定提示那種置中的三行就是這樣壞掉的：
     *
     * <pre>
     *     This item's power has been sealed,
     *       an ◉ Item Identifier can unlock
     *              its potential.
     * </pre>
     *
     * 中文比英文短，前導偏移卻沒變，整排就往左偏，看起來像沒有置中。
     *
     * <h2>怎麼對回去</h2>
     * 空白在原文與譯文之間是<b>一對一</b>的（{@code rebuild} 依序填回），
     * 所以可以拿它們當錨點，把兩邊切成一樣多段，逐段比寬度：
     *
     * <ul>
     *   <li>行首就是空白 → 這行是置中／縮排，前導偏移補<b>差額的一半</b></li>
     *   <li>其餘的空白 → 兩側都有文字才是欄位交界，補<b>累計的位移</b>，
     *       和 {@link #alignColumns} 同一套邏輯</li>
     * </ul>
     *
     * <p>數量對不上就原樣返回。寧可維持現狀，也不要憑猜測動版面。
     */
    private static Component realign(StyledText original, Component rebuilt,
                                     boolean centered) {
        List<Run> orig = runs(original.getComponent());
        List<Run> made = runs(rebuilt);
        int spaces = countSpaces(made);
        if (spaces == 0 || spaces != countSpaces(orig)) {
            return rebuilt;
        }
        List<Integer> origSeg = segmentWidths(orig);
        List<Integer> madeSeg = segmentWidths(made);

        int[] adjust = new int[spaces];
        boolean leading = centered && origSeg.get(0) == 0 && madeSeg.get(0) == 0;

        if (leading) {
            int delta = sum(origSeg) - sum(madeSeg);
            adjust[0] = delta / 2;             // 置中：補滿會把整行推到右邊
        }
        int drift = 0;
        for (int k = leading ? 1 : 0; k < spaces; k++) {
            drift += madeSeg.get(k) - origSeg.get(k);
            // 兩側都有文字才是欄位交界；行尾的留白邊距不能動
            if (madeSeg.get(k) > 0 && madeSeg.get(k + 1) > 0) {
                adjust[k] = -drift;
                drift = 0;
            }
        }
        return apply(made, adjust);
    }

    /** 一段連續的同型內容：不是排版空白，就是文字。 */
    private record Run(boolean space, int px, Style style, String text) {}

    private static List<Run> runs(Component component) {
        List<Run> out = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.add(isAdjustableSpace(style, text)
                        ? new Run(true, SpaceOffset.decode(text), style, text)
                        : new Run(false, 0, style, text));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /**
     * 這段空白能不能安全地重新編碼。
     *
     * <p>光看字型不夠：{@code minecraft:space} 底下也可能掛著<b>不是</b>寬度偏移的
     * 字元（材質包自己定義的圖示就是這樣）。那種字元重新編碼會變成完全不同的
     * 東西——畫面上是圖示憑空消失或變成方框。
     *
     * <p>判斷交給 {@link SpaceOffset#isOffsetRun}：看每個碼位是不是都落在偏移
     * 範圍內。不是的話就當成普通文字原封不動抄過去。
     */
    private static boolean isAdjustableSpace(Style style, String text) {
        return SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text);
    }

    private static int countSpaces(List<Run> runs) {
        int n = 0;
        for (Run r : runs) {
            if (r.space()) {
                n++;
            }
        }
        return n;
    }

    /** 以空白為界切成幾段，每段的文字寬度。長度固定是「空白數 + 1」。 */
    private static List<Integer> segmentWidths(List<Run> runs) {
        List<Integer> out = new ArrayList<>();
        int width = 0;
        for (Run r : runs) {
            if (r.space()) {
                out.add(width);
                width = 0;
            } else {
                width += widthOf(literal(r.text(), r.style()));
            }
        }
        out.add(width);
        return out;
    }

    private static int sum(List<Integer> values) {
        int total = 0;
        for (int v : values) {
            total += v;
        }
        return total;
    }

    /** 把調整量套回第 n 個空白。 */
    private static Component apply(List<Run> runs, int[] adjust) {
        MutableComponent out = Component.empty();
        int index = 0;
        for (Run r : runs) {
            if (!r.space()) {
                out.append(literal(r.text(), r.style()));
                continue;
            }
            String encoded = SpaceOffset.encode(r.px() + adjust[index++]);
            if (!encoded.isEmpty()) {
                out.append(literal(encoded, r.style()));
            }
        }
        return out;
    }

    /**
     * 查字典，查不到就把首尾的排版符號拿掉再查一次。
     *
     * <p>Wynncraft 會在每一行前後塞排版用的空白字元（{@code minecraft:space}、
     * 外框圖示等），所以實際模板長這樣：
     *
     * <pre>
     *   實際模板  {#}Doom Stone{#}
     *   字典的鍵  Doom Stone          ← 語料來自 CDN 純文字，不含排版符號
     * </pre>
     *
     * 兩者永遠對不上。把首尾的 {@code {#}} 與空白剝掉再查就能命中，
     * 命中後再把剝掉的部分原樣接回去——佔位符數量與位置都不變，
     * 所以 {@link #rebuild} 仍然填得回原本的符號。
     *
     * <p>只剝<b>首尾</b>：句子中間的符號是內容的一部分（例如
     * {@code an {#}Item Identifier can unlock}），剝掉會改變語意。
     */
    private static String lookup(String template, TranslationStore store) {
        return lookup(template, store, false);
    }

    /**
     * @param percent 這一行的數值是百分比，優先找「標籤 + {@code %}」的鍵
     */
    private static String lookup(String template, TranslationStore store, boolean percent) {
        String exact = withPercent(template, store, percent);
        if (exact != null) {
            return exact;
        }
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;

        int start = 0;
        while (start < template.length()) {
            if (template.startsWith(glyph, start)) {
                start += glyph.length();
            } else if (Character.isWhitespace(template.charAt(start))) {
                start++;
            } else if (isDecoration(template.codePointAt(start))) {
                // 行首的裝飾符號，例如 Major ID 的 ✦、技能樹的 ✔。
                // 它們是<b>一般字元</b>不是材質包圖示，所以不會被抽成 {#}，
                // 於是「✦ Altruism」永遠對不上語料裡的「Altruism」。
                start += Character.charCount(template.codePointAt(start));
            } else {
                break;
            }
        }
        int end = template.length();
        while (end > start) {
            if (end >= start + glyph.length() && template.startsWith(glyph, end - glyph.length())) {
                end -= glyph.length();
            } else if (Character.isWhitespace(template.charAt(end - 1))
                    || isTrailingColon(template.charAt(end - 1))) {
                end--;
            } else {
                break;
            }
        }
        if (start == 0 && end == template.length()) {
            return null;                       // 首尾沒有可剝的，不必重查
        }
        String core = template.substring(start, end);
        if (core.isBlank()) {
            return null;
        }
        String hit = withPercent(core, store, percent);
        if (hit == null) {
            return null;
        }
        return template.substring(0, start) + hit + reattach(hit, template.substring(end));
    }

    /**
     * 這個字元是不是純裝飾。
     *
     * <p>只認 Unicode 分類是「其他符號」的：{@code ✦ ✔ ✤ ★}。刻意不含標點與
     * 數學符號——{@code -} 與 {@code +} 出現在真正的鍵開頭（{@code - Converts up to}），
     * 剝掉會讓那些條目查不到。
     */
    private static boolean isDecoration(int codePoint) {
        return Character.getType(codePoint) == Character.OTHER_SYMBOL;
    }

    /**
     * 把剝掉的尾巴接回去，但<b>全形標點後面不接空白</b>。
     *
     * <h2>為什麼</h2>
     * 全形冒號「：」本身就佔一個全形寬，右半邊是留白。原文的
     * {@code "Total Damage: "} 尾端有個半形空格，照樣接回去就變成
     * 「總傷害：␣」——留白疊留白，間隔變成兩倍寬。
     *
     * <p>技能樹整片都是「標籤: 數值」，每一行都多出這麼一塊，看起來就是
     * 到處都有莫名其妙的空格。中文排版本來也就不在全形標點後面加空格。
     */
    private static String reattach(String translated, String suffix) {
        return suffix.isBlank() && !translated.isEmpty()
                && isFullWidthPunctuation(translated.charAt(translated.length() - 1))
                ? "" : suffix;
    }

    /** 自帶留白的全形標點。 */
    private static boolean isFullWidthPunctuation(char c) {
        return "：，。、！？；）」』】".indexOf(c) >= 0;
    }

    /**
     * 先試百分比版本的鍵，再試一般的。
     *
     * <h2>為什麼要獨立一支</h2>
     * 這兩件事必須<b>疊在一起</b>：技能樹的標籤帶冒號（{@code Earth Damage:}），
     * 先前是拿整個模板去接 {@code %}，變成 {@code Earth Damage:%}——那個鍵不存在，
     * 於是退回一般的鍵，百分比的譯法永遠用不到。要先剝掉冒號，再接 {@code %}。
     */
    private static String withPercent(String key, TranslationStore store, boolean percent) {
        if (percent) {
            String hit = store.lookup(key + "%");
            if (hit != null) {
                return hit;
            }
        }
        return store.lookup(key);
    }

    /**
     * 還沒翻譯的行 —— <b>原封不動抄過去</b>。
     *
     * <p>先前這裡會把原文轉成灰色來標示進度，但那等於重建整行，
     * 排版符號的字型與負寬度空白都會跑掉，整個面板的版面就壞了。
     * 格式保真比「看得出哪行沒翻」重要，所以改成原樣複製。
     */
    public static Component untranslated(StyledText line) {
        MutableComponent out = Component.empty();
        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            out.append(literal(raw, ps == null ? Style.EMPTY : ps.getStyle()));
        }
        return out;
    }

    // ------------------------------------------------------------ 內部

    private static Component rebuild(String translated, LineParts parts,
                                     TranslationStore store) {
        List<Component> one = rebuildAll(new String[] {translated}, List.of(parts), List.of(), store);
        return one == null ? null : one.get(0);
    }

    /**
     * 把幾行譯文填回原文的碎片。
     *
     * <h2>為什麼行數可以不一樣</h2>
     * 中文比英文緊湊，原文分成兩行的句子往往一行就講完了。硬要譯者湊出同樣的
     * 行數，斷句會斷在莫名其妙的地方（見 issue #44）。我們的譯文面板是自己畫的，
     * 幾行都沒關係。
     *
     * <p>所以佔位符不是逐行對，而是<b>整段照順序</b>取用：把整段的符號、地名、
     * 數值、玩家名各自併成一個池子，譯文從頭到尾依序消耗。這樣譯者要把
     * {@code {~}} 搬到上一行或下一行都可以，只要整段的數量對得上。
     *
     * @return 譯好的每一行；佔位符數量對不上時回傳 {@code null}
     */
    private static List<Component> rebuildAll(String[] translated, List<LineParts> parts,
                                              List<LineParts.Piece> extraAccents,
                                              TranslationStore store) {
        List<LineParts.Piece> glyphs = new ArrayList<>();
        List<LineParts.Piece> places = new ArrayList<>();
        List<LineParts.Piece> numbers = new ArrayList<>();
        List<LineParts.Piece> users = new ArrayList<>();
        List<LineParts.Piece> accents = new ArrayList<>(extraAccents);
        for (LineParts part : parts) {
            glyphs.addAll(part.glyphs());
            places.addAll(part.places());
            numbers.addAll(part.numbers());
            users.addAll(part.users());
            accents.addAll(part.accents());
        }

        List<List<Token>> lines = new ArrayList<>(translated.length);
        long wantGlyphs = 0;
        long wantPlaces = 0;
        long wantNumbers = 0;
        long wantUsers = 0;
        int numbered = 0;
        for (String line : translated) {
            List<Token> tokens = tokenize(line);
            lines.add(tokens);
            for (Token token : tokens) {
                switch (token.kind()) {
                    case GLYPH -> wantGlyphs++;
                    case PLACE -> wantPlaces++;
                    case NUMBER -> {
                        if (token.index() == 0) {
                            wantNumbers++;      // 照順序取的才算消耗
                        } else {
                            numbered = Math.max(numbered, token.index());
                        }
                    }
                    case USER -> wantUsers++;
                    default -> { }
                }
            }
        }
        // 數值的數量必須剛好——少一個那個數字會憑空消失。
        //
        // 例外是譯文用了 {~1} 這種<b>指名</b>要第幾個的寫法：指名之後重複用、
        // 跳過某一個都合理，只要指到的號碼真的存在。
        boolean numbersOk = numbered == 0
                ? wantNumbers == numbers.size()
                : wantNumbers <= numbers.size() && numbered <= numbers.size();
        if (wantGlyphs != glyphs.size() || wantPlaces != places.size()
                || !numbersOk || wantUsers != users.size()) {
            return null;              // 譯者刪了或多加了佔位符，整段放棄
        }

        Style textStyle = forDisplay(dominantStyle(parts));
        boolean[] usedAccent = new boolean[accents.size()];
        int glyph = 0;
        int place = 0;
        int number = 0;
        int user = 0;

        List<Component> out = new ArrayList<>(lines.size());
        for (List<Token> tokens : lines) {
            MutableComponent line = Component.empty();
            for (Token token : tokens) {
                switch (token.kind()) {
                    case GLYPH -> {
                        // 符號連同原樣式（含自訂字型）整段搬回，這是它顯示得出來的唯一方式
                        LineParts.Piece piece = glyphs.get(glyph++);
                        line.append(Component.literal(piece.text()).withStyle(piece.style()));
                    }
                    case PLACE -> {
                        // 地名是專有名詞，原樣填回，永遠不翻譯
                        LineParts.Piece piece = places.get(place++);
                        line.append(literal(piece.text(), forDisplay(piece.style())));
                    }
                    case NUMBER -> {
                        // 帶編號的直接指名要第幾個，沒編號的照順序取下一個
                        int at = token.index() > 0 ? token.index() - 1 : number++;
                        if (at < 0 || at >= numbers.size()) {
                            return null;
                        }
                        LineParts.Piece piece = numbers.get(at);
                        line.append(literal(piece.text(), forDisplay(piece.style())));
                    }
                    case USER -> {
                        // 玩家名字原樣填回，跟地名一樣是專有名詞
                        LineParts.Piece piece = users.get(user++);
                        line.append(literal(piece.text(), forDisplay(piece.style())));
                    }
                    case TEXT -> appendText(line, token.text(), textStyle,
                                            accents, usedAccent, store);
                }
            }
            out.add(line);
        }
        return out;
    }

    /**
     * 接上一段譯文，並把原文裡帶特殊樣式的詞的樣式貼回去。
     *
     * <h2>為什麼做得到</h2>
     * 技能名、地名這類詞照慣例<b>保持英文</b>——也就是原樣出現在譯文裡。
     * {@code Reduce the Mana cost of Bash.} 翻成「降低 Bash 的魔力消耗。」，
     * 那個 {@code Bash} 還在。原文裡它帶著底線與專屬顏色，比對得到就把
     * 原本的 {@link Style} 搬回去。
     *
     * <p>搬的是樣式<b>物件</b>而不是某個顏色碼，所以非原版的自訂顏色也對——
     * {@code §} 格式碼只表達得出十六個原版顏色，那些技能的顏色根本寫不出來。
     *
     * <h2>對不上的時候</h2>
     * 譯者把那個詞翻成中文了，就比對不到，那一段照主樣式畫——也就是<b>現狀</b>，
     * 不會更糟。每個詞只貼一次：同一個詞出現兩次而原文只有一個帶樣式時，
     * 分不出該貼哪一個，貼錯位置比沒有樣式更糟。
     */
    private static void appendText(MutableComponent out, String text, Style base,
                                   List<LineParts.Piece> accents, boolean[] used,
                                   TranslationStore store) {
        int from = 0;
        while (from < text.length()) {
            int at = -1;
            int which = -1;
            for (int k = 0; k < accents.size(); k++) {
                if (used[k]) {
                    continue;
                }
                int found = text.indexOf(accents.get(k).text(), from);
                if (found < 0) {
                    continue;
                }
                // 位置靠前的優先；同一個位置取比較長的，短詞才不會卡在長詞裡面
                boolean better = at < 0 || found < at
                        || (found == at && accents.get(k).text().length()
                                         > accents.get(which).text().length());
                if (better) {
                    at = found;
                    which = k;
                }
            }
            // 技能名稱：語料裡只翻一次，所有提到它的敘述自動跟著換
            TranslationStore.Term term = store == null ? null : store.findTerm(text, from);
            if (term != null && (at < 0 || term.start() < at)) {
                if (term.start() > from) {
                    out.append(literal(text.substring(from, term.start()), base));
                }
                out.append(literal(term.translation(), base));
                from = term.end();
                continue;
            }
            if (at < 0) {
                out.append(literal(text.substring(from), base));
                return;
            }
            if (at > from) {
                out.append(literal(text.substring(from, at), base));
            }
            LineParts.Piece accent = accents.get(which);
            // 帶樣式的那一段如果剛好是個技能名稱，樣式與譯名兩個都要
            String shown = store == null ? null : store.lookupTerm(accent.text());
            out.append(literal(shown != null ? shown : accent.text(),
                               forDisplay(accent.style())));
            used[which] = true;
            from = at + accent.text().length();
        }
    }

    /** 保留顏色與粗斜體，但把字型換成預設，中文才畫得出來。 */
    private static Style forDisplay(Style style) {
        return (style == null ? Style.EMPTY : style).withFont(FontDescription.DEFAULT);
    }

    private static Style greyed() {
        return Style.EMPTY
                .withColor(ChatFormatting.DARK_GRAY)
                .withFont(FontDescription.DEFAULT);
    }

    private static Component literal(String text, Style style) {
        return Component.literal(text).withStyle(style);
    }

    // ------------------------------------------------------------ 模板切詞

    private enum Kind { TEXT, GLYPH, PLACE, NUMBER, USER }

    /**
     * @param index 指定要原文的第幾個數值（從 1 起）；{@code 0} 表示照順序取下一個
     */
    private record Token(Kind kind, String text, int index) {
        Token(Kind kind, String text) {
            this(kind, text, 0);
        }
    }

    /** {@code {~N}} 的長度：左括號、波浪、一位數字、右括號。 */
    private static final int NUMBERED_LENGTH = 4;

    /**
     * {@code template} 的 {@code at} 位置是不是 {@code {~1}} 這種帶編號的佔位符。
     *
     * @return 編號（1–9）；不是的話回傳 0
     */
    private static int numberedAt(String template, int at) {
        if (at + NUMBERED_LENGTH > template.length()
                || template.charAt(at) != '{' || template.charAt(at + 1) != '~'
                || template.charAt(at + 3) != '}') {
            return 0;
        }
        char digit = template.charAt(at + 2);
        return digit >= '1' && digit <= '9' ? digit - '0' : 0;
    }

    /** 把模板切成「文字 / 符號 / 地名 / 數值 / 玩家名」的序列。 */
    private static List<Token> tokenize(String template) {
        List<Token> out = new java.util.ArrayList<>();
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        String place = GlyphSplitter.PLACE_PLACEHOLDER;
        String number = GlyphSplitter.NUMBER_PLACEHOLDER;
        String user = GlyphSplitter.PLAYER_PLACEHOLDER;
        int i = 0;
        StringBuilder text = new StringBuilder();

        while (i < template.length()) {
            if (template.startsWith(glyph, i)) {
                flush(text, out);
                out.add(new Token(Kind.GLYPH, glyph));
                i += glyph.length();
            } else if (template.startsWith(place, i)) {
                flush(text, out);
                out.add(new Token(Kind.PLACE, place));
                i += place.length();
            } else if (template.startsWith(number, i)) {
                flush(text, out);
                out.add(new Token(Kind.NUMBER, number));
                i += number.length();
            } else if (numberedAt(template, i) > 0) {
                // {~1} {~2}：指定要原文的第幾個數值。中文語序常常跟英文相反，
                // 「+2 to 10%」翻成「最多 10%，最少 +2」就需要調換。
                flush(text, out);
                out.add(new Token(Kind.NUMBER, "", numberedAt(template, i)));
                i += NUMBERED_LENGTH;
            } else if (template.startsWith(user, i)) {
                flush(text, out);
                out.add(new Token(Kind.USER, user));
                i += user.length();
            } else {
                text.append(template.charAt(i++));
            }
        }
        flush(text, out);
        return out;
    }

    private static void flush(StringBuilder text, List<Token> out) {
        if (!text.isEmpty()) {
            out.add(new Token(Kind.TEXT, text.toString()));
            text.setLength(0);
        }
    }
}
