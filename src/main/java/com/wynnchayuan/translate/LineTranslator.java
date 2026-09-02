package com.wynnchayuan.translate;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.PlaceNames;
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
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

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
        List<LineParts.Piece> places = null;
        List<LineParts.Piece> glyphs = null;
        boolean flowed = false;
        if (translated == null || translated.isBlank()) {
            // 原文可能是被 tooltip 寬度自動斷行的，斷點跟語料對不上。
            // 把整段攤平成一行再查一次。
            Flowed hit = lookupFlowedParts(template, store);
            if (hit == null) {
                // 還是查不到的話，多半是<b>斷行本身</b>留下的痕跡。見 rejoin。
                Rejoined rejoined = rejoin(parts, dominantStyle(parts));
                if (rejoined != null) {
                    hit = lookupFlowedParts(rejoined.template(), store);
                    if (hit != null) {
                        places = rejoined.places();
                        glyphs = rejoined.glyphs();
                    }
                }
            }
            translated = hit == null ? null : hit.text();
            flowed = hit != null;
            if (hit != null && hit.label() != null) {
                // 名稱那半在原文裡有自己的顏色（Major ID 的名稱是粉紅的），
                // 而譯文是中文、跟原文對不起來，一般的樣式沿用比對不到。
                // 這裡知道譯出來的名稱長什麼樣，直接當成一個「原樣出現的詞」交上去。
                extra = labelAccent(hit.label(), run.get(0));
            }
        }
        if (translated == null || translated.isBlank()) {
            // 這裡<b>不</b>記診斷。呼叫端會從最長試到兩行，每一個長度都記一次的話，
            // 一份八行的素材清單就吃掉十九個名額，真正想看的那一段永遠排不進來。
            // 改由 TooltipPanel 在「所有長度都失敗」之後記一次。
            return null;
        }
        // 記在這裡，<b>不管走的是哪一條路</b>。先前只記「拆名稱」那條，
        // 而 Major ID 常常是整段一次命中——於是要查的那一種偏偏沒被記下來，
        // 使用者回報「這個檔案根本沒生成」。
        FlowedDebug.note(run, extra.isEmpty() ? null : extra.get(0).text(),
                         labelStyleOf(run.get(0)), dominantStyle(parts));
        if (needsWrap(translated, run.size(), flowed)) {
            translated = wrapToBlock(translated, run);
        }
        String[] dst = translated.split("\n", -1);
        List<Component> built = rebuildAll(dst, parts, extra, glyphs, places, store);
        if (built == null) {
            return null;                       // 佔位符對不上，整段放棄
        }
        if (dst.length != run.size()) {
            // 中文比英文緊湊，兩行的句子常常一行就講完。譯文面板是我們自己畫的，
            // 行數不一樣沒關係——只是少了「原本那一行」可以拿來對齊，直接照原樣出。
            List<Component> plain = new ArrayList<>(built.size());
            for (Component one : built) {
                plain.add(unslant(one));
            }
            return plain;
        }
        List<Component> out = new ArrayList<>(run.size());
        for (int i = 0; i < run.size(); i++) {
            out.add(unslant(realign(run.get(i), built.get(i), centered[i])));
        }
        return out;
    }

    /**
     * 記一段「整輪都查不到」的跨行原文。
     *
     * <p>呼叫端會把連續幾行併起來、從最長試到兩行。全部落空之後才記這一筆——
     * 見 {@code TooltipPanel} 裡的說明。
     */
    public static void noteBlockMiss(String template, TranslationStore store) {
        FlowedDebug.miss(template, store);
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

    /**
     * 重新併回一句的結果。
     *
     * @param glyphs 剝掉行首排版偏移之後<b>剩下</b>的符號，順序照舊
     * @param places 跨行認出來的地名
     */
    record Rejoined(String template, List<LineParts.Piece> glyphs,
                    List<LineParts.Piece> places) {}

    /**
     * 把幾行原文<b>當成一句話</b>重新組一次，去掉斷行本身留下的痕跡。
     *
     * <h2>斷行會留下兩種痕跡</h2>
     * <ol>
     *   <li><b>每一行開頭的排版偏移。</b>置中的段落，每一行前面都有一個
     *       用來推位置的隱形字元，抽成模板就是行首的 {@code {#}}：
     *       <pre>
     *         {#}As a flashing star shot through
     *         {#}the night sky, a sole observer
     *       </pre>
     *       攤平之後鍵就成了「{@code {#}As a flashing star… {#}the night sky…}」，
     *       而語料裡是乾淨的一句話。{@code {#}} 的數量取決於斷成幾行，
     *       也就是取決於<b>畫面寬度</b>——那是排版，不是內容。</li>
     *   <li><b>被切成兩半的地名。</b>地名是在 {@link LineParts#of} 裡逐行比對的：
     *       <pre>
     *         the peak of the Tower of
     *         Ascension, is completely hollow.
     *       </pre>
     *       兩行各自都不含完整地名，模板裡就留著原樣英文；語料裡存的是
     *       {@code {p}}。</li>
     * </ol>
     *
     * <p>兩種痕跡都會讓鍵對不上，而且是<b>各自獨立</b>的——裝備的背景敘述整批
     * 不生效就是它們疊在一起，缺一個修都沒用。這裡一次處理掉：剝掉行首偏移、
     * 併成一行、再認一次地名。
     *
     * <p>剝掉的偏移<b>不能留在符號池裡</b>，否則譯文裡的 {@code {#}} 數量對不上，
     * 整段會被判定為佔位符不符而放棄。所以回傳剩下的那些讓呼叫端換掉整個池子。
     * 置中本身不受影響——那是 {@link #realign} 依 {@code centered} 另外算的。
     *
     * @return 兩種痕跡都沒有時回傳 {@code null}，呼叫端就不必白查一次
     */
    private static Rejoined rejoin(List<LineParts> parts, Style style) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        StringBuilder joined = new StringBuilder();
        List<LineParts.Piece> kept = new ArrayList<>();
        boolean trimmed = false;
        for (LineParts part : parts) {
            String line = part.template();
            int at = 0;
            int lead = 0;
            while (line.startsWith(glyph, at)) {
                at += glyph.length();
                lead++;
            }
            if (at >= line.length()) {
                // 整行都是符號——那是<b>分隔線</b>（lore 上下那條 ◆—◆），是內容，
                // 不是縮排。剝掉的話這一行會變成空的，於是整段連分隔線一起被
                // 當成同一句話吃掉，譯文出來就少了那條線。
                at = 0;
                lead = 0;
            }
            if (lead > 0) {
                trimmed = true;
            }
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(line, at, line.length());
            List<LineParts.Piece> own = part.glyphs();
            kept.addAll(own.subList(Math.min(lead, own.size()), own.size()));
        }

        String text = joined.toString();
        List<LineParts.Piece> found = new ArrayList<>();
        java.util.regex.Matcher place = PlaceNames.matcher(text);
        if (place != null) {
            StringBuilder out = new StringBuilder();
            int from = 0;
            while (place.find()) {
                out.append(text, from, place.start())
                   .append(GlyphSplitter.PLACE_PLACEHOLDER);
                found.add(new LineParts.Piece(place.group(), style));
                from = place.end();
            }
            if (!found.isEmpty()) {
                text = out.append(text.substring(from)).toString();
            }
        }
        return trimmed || !found.isEmpty()
                ? new Rejoined(text, List.copyOf(kept), List.copyOf(found)) : null;
    }

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
        String head = labelHead(template.substring(0, colon + 1), store);
        if (head == null) {
            return null;
        }
        String rest = template.substring(colon + 2);
        String tail = store.lookupFlat(rest);
        if (tail == null) {
            tail = store.lookup(rest);
        }
        return tail == null || tail.isBlank()
                ? null : new Flowed(joinLabel(head, tail), head);
    }

    /**
     * 名稱那半的譯文。<b>連冒號一起查</b>。
     *
     * <h2>為什麼冒號不能先切掉</h2>
     * 這些標籤在語料裡的正規形式是<b>帶冒號</b>的（{@code "Range:"}、
     * {@code "Total Damage:"}），冒號跟著譯文走——見 {@link #lookup} 裡
     * 「把冒號留著」那段：同一個面板才不會半形全形混用。
     *
     * <p>先前是切在冒號<b>前面</b>再查，於是只有同時也收了無冒號版的標籤
     * （{@code "Duration"}）查得到，其餘一律落空——技能面板的「施放範圍」、
     * 「總傷害」整行掉回英文就是這樣來的。
     *
     * <p>{@link #lookup} 查不到帶冒號的版本時本來就會再剝掉冒號重查，
     * 所以多帶一個冒號進去只會多命中、不會少。無冒號的那次是保險：
     * 標籤本身以別的標點結尾時（罕見）才輪得到。
     *
     * @param label 名稱那半，<b>含</b>結尾的冒號
     */
    static String labelHead(String label, TranslationStore store) {
        String hit = lookup(label, store);
        if (hit == null || hit.isBlank()) {
            hit = lookup(label.substring(0, label.length() - 1), store);
        }
        return hit == null || hit.isBlank() ? null : hit;
    }

    /**
     * 名稱與說明接起來，不要接出<b>兩個</b>冒號。
     *
     * <p>名稱現在可能自帶冒號了（{@code "施放範圍:"}），那是語料刻意的形式，
     * 見 {@link #labelHead}。
     */
    private static String joinLabel(String head, String tail) {
        String core = head.stripTrailing();
        return endsWithColon(core) ? core + " " + tail : head + ": " + tail;
    }

    /** 這段文字是不是以冒號收尾（半形或全形都算）。 */
    private static boolean endsWithColon(String text) {
        return !text.isEmpty() && isTrailingColon(text.charAt(text.length() - 1));
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
            // 逐<b>段</b>累計，不是逐行。
            //
            // 先前是「每一行的主樣式拿走整行的份量」。那等於讓一行裡最長的那一段
            // 代表整行——技能樹的
            //
            //   Increase your Max Orbs        ← 整行灰色，22 字
            //   from Lightweaver by +2.       ← Lightweaver 帶底線、11 字，
            //                                    但比 from(5) 與 by +2.(7) 都長
            //
            // 第二行的主樣式於是變成「底線」，還帶著整行 25 字的份量，
            // 壓過第一行的 22 字——整段譯文就全部畫上了底線。
            //
            // 照實際字數累計就沒這問題：灰 22+5+7=34、底線 11，灰勝。
            //
            // 累計時<b>不看裝飾</b>，見 #undecorated；
            // 括號裡的註解也不算，見 #noteStyleOf。
            for (LineParts.Piece run : part.runs()) {
                if (isNote(run.text())) {
                    continue;
                }
                weight.merge(undecorated(run.style()), run.text().length(), Integer::sum);
            }
        }
        Style best = undecorated(parts.get(0).textStyle());
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
     * 照括號把文字切成「正文」與「註解」兩種，各自用自己的顏色送去 {@link #appendText}。
     *
     * <p>括號在中英文裡都是註解的記號，所以切點在譯文上一樣成立。
     * {@code depth} 跨行帶著走——註解常常被 tooltip 寬度切成兩行，
     * 左括號在這一行、右括號在下一行。
     *
     * @param note  註解的顏色；{@code null} 表示原文的註解跟正文同色，不必分開
     * @param depth 單元素陣列，當作可變的「現在在不在括號裡」
     */
    private static void appendNoting(MutableComponent out, String text,
                                     Style base, Style note, boolean[] depth,
                                     List<LineParts.Piece> accents, boolean[] used,
                                     TranslationStore store) {
        if (note == null) {
            appendText(out, text, base, accents, used, store);
            return;
        }
        int from = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean opens = !depth[0] && (c == '(' || c == '（');
            boolean closes = depth[0] && (c == ')' || c == '）');
            if (!opens && !closes) {
                continue;
            }
            // 左括號自己算註解的一部分，右括號也是——切點在括號的外側
            int cut = opens ? i : i + 1;
            if (cut > from) {
                appendText(out, text.substring(from, cut),
                           depth[0] ? note : base, accents, used, store);
            }
            from = cut;
            depth[0] = opens;
        }
        if (from < text.length()) {
            appendText(out, text.substring(from), depth[0] ? note : base,
                       accents, used, store);
        }
    }

    /**
     * 這一段是不是<b>括號裡的註解</b>。
     *
     * <h2>為什麼註解不能參與底色的統計</h2>
     * 技能樹的 {@code Heal}：
     *
     * <pre>
     *   Heals you and nearby allies in
     *   a large area around you.                ← 正文，亮灰，54 字
     *   (When healing others, you can't heal
     *   more than 50% of their max health)      ← 括號註解，暗灰，70 字
     * </pre>
     *
     * <p>照字數算，<b>註解比正文長</b>——於是整段譯文被染成註解的暗灰，
     * 正文那兩行也跟著暗掉。但註解依定義是附帶說明，不是這一段的主要聲音。
     *
     * <p>括號在中英文裡都是註解的記號，所以這個判斷跨語言成立——
     * 譯文那半也認得出哪裡是註解（見 {@link #noteStyleOf} 的用法）。
     */
    private static boolean isNote(String text) {
        String core = text.strip();
        return core.startsWith("(") || core.startsWith("（");
    }

    /**
     * 括號註解自己的顏色；沒有註解、或註解跟正文同色時回傳 {@code null}。
     *
     * <p>不參與底色統計之後還得把顏色還回去，否則註解會變成正文的亮色——
     * 那是把錯誤換一個方向，不是修好。
     */
    private static Style noteStyleOf(List<LineParts.Piece> runs, Style base) {
        for (LineParts.Piece run : runs) {
            if (isNote(run.text())) {
                Style style = undecorated(run.style());
                return style.equals(base) ? null : forDisplay(run.style());
            }
        }
        return null;
    }

    /**
     * 拿掉底線、粗體、斜體那些<b>裝飾</b>，只留顏色與字型。
     *
     * <h2>為什麼段落的底色不能帶裝飾</h2>
     * 底線是標在<b>特定術語</b>上的：Wynncraft 用 {@code §n} 標技能名。
     * 它依定義就是例外，而例外不該變成整段的底色。
     *
     * <p>沒拿掉之前，秘術師那段是這樣算的：
     *
     * <pre>
     *   Meteor, Pyrokinesis and Powder Specials      ← 三個底線詞共 32 字
     *   consume Unstable ⚡ to deal +100% damage.     ← 灰字共 32 字
     * </pre>
     *
     * 32 比 32，<b>平手</b>——而平手時先遇到的勝出，第一段剛好是 {@code Meteor}
     * 那個底線詞，於是整段譯文全部畫上了底線。
     *
     * <p>把裝飾拿掉之後，「灰＋底線」與「灰」併成同一個，灰以 64 字獨贏，
     * 平手這件事根本不會發生。而 {@code Meteor} 的底線仍然回得來——
     * 它跟底色不同，會被收成重點段（見 {@code LineParts#accentsAgainst}）。
     */
    private static Style undecorated(Style style) {
        return (style == null ? Style.EMPTY : style)
                .withUnderlined(false)
                .withBold(false)
                .withItalic(false)
                .withStrikethrough(false)
                .withObfuscated(false);
    }

    /**
     * 複數與所有格：原文寫 {@code Marks}，譯文寫 {@code Mark}。
     *
     * <h2>為什麼會對不上</h2>
     * 顏色是拿原文的<b>字面</b>到譯文裡找的。譯者保留英文專有名詞時，
     * 中文沒有複數，自然寫單數——{@code 擁有 2+ 層 Mark}。於是重點段
     * {@code Marks} 在譯文裡一個字都對不上，那一段就掉回底色。
     * 所有格也一樣：{@code Multihit's} 對 {@code Multihit}。
     *
     * <h2>為什麼要這麼小心</h2>
     * 貼樣式那一步用的是 {@code indexOf}，<b>沒有詞界</b>。詞幹 {@code Mark}
     * 會中在 {@code Marked} 裡面，把技能名的前四個字母染成別的顏色。
     *
     * <p>所以不憑空登記，兩個條件都要成立：完整形式在譯文裡<b>找不到</b>
     * （找得到就用它，輪不到詞幹），而詞幹在譯文裡<b>自成一個詞</b>。
     */
    private static void addStem(List<LineParts.Piece> out, String core,
                                Style style, String translated) {
        String stem = stemOf(core);
        if (stem == null || translated == null || translated.contains(core)
                || !standsAlone(translated, stem)) {
            return;
        }
        out.add(new LineParts.Piece(stem, style));
    }

    /** 去掉結尾的所有格或複數；不像有詞尾就回傳 {@code null}。 */
    static String stemOf(String text) {
        for (String tail : new String[] {"’s", "'s"}) {
            if (text.endsWith(tail) && text.length() > tail.length() + 1) {
                return text.substring(0, text.length() - tail.length());
            }
        }
        // ss 結尾的多半不是複數（Progress、Address），去掉會變成別的字
        return text.length() > MIN_STEM && text.endsWith("s") && !text.endsWith("ss")
                ? text.substring(0, text.length() - 1) : null;
    }

    /** {@code word} 在 {@code text} 裡有沒有<b>自成一個詞</b>地出現過。 */
    static boolean standsAlone(String text, String word) {
        for (int at = text.indexOf(word); at >= 0; at = text.indexOf(word, at + 1)) {
            int after = at + word.length();
            boolean left = at == 0 || !isWordChar(text.charAt(at - 1));
            boolean right = after >= text.length() || !isWordChar(text.charAt(after));
            if (left && right) {
                return true;
            }
        }
        return false;
    }

    /** 見 {@link #stemOf}：三個字母以下的詞尾去掉之後不成詞。 */
    private static final int MIN_STEM = 4;

    /**
     * 把譯出來的名稱包成一個「原樣出現的詞」，帶著原文名稱的樣式。
     *
     * <p>拿的是<b>第一個</b>與整行主樣式不同的片段——「名稱：說明」的名稱就在
     * 行首，而它跟後面的說明顏色不同，正是這樣才會被記成 accent。
     * 找不到就回傳空的，那一段照主樣式畫，不會比現在更糟。
     */
    private static List<LineParts.Piece> labelAccent(String label, StyledText firstLine) {
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
        if (core.isBlank()) {
            return List.of();
        }
        // 直接從<b>原文第一行</b>取第一段有字母的樣式——那一段就是名稱本身。
        //
        // 先前是從 accents() 裡挑，而 accents() 收的是「跟整行主要樣式不同」的段。
        // 名稱如果比同一行露出的說明還長，主要樣式就變成名稱，被判為「不同」的
        // 反而是說明——於是名稱套上說明的顏色、說明套上名稱的顏色，
        // 畫面上看起來就是<b>兩邊顏色對調</b>。
        Style style = labelStyleOf(firstLine);
        if (style == null) {
            return List.of();
        }
        // 冒號也算名稱的一部分。原文的「Transcendence:」連冒號都是名稱的顏色，
        // 只把名字上色的話冒號會落到說明那半，看起來就是「顏色接不起來」。
        // 兩種都登記：帶冒號的比較長，比對時會優先中。
        // 名稱可能已經自帶冒號（見 #labelHead），再加一個就變成「範圍::」，
        // 那一條永遠比對不到。先剝再加，兩種形式都還是各登記一次。
        String bare = endsWithColon(core) ? core.substring(0, core.length() - 1) : core;
        return List.of(new LineParts.Piece(bare + ":", style),
                       new LineParts.Piece(core, style));
    }

    /**
     * 名稱那一段的樣式：原文第一行<b>第一段有字母的</b>。
     *
     * <p>抽成一支是為了讓診斷跟正式路徑用同一份判斷。兩邊各寫一次的話，
     * 診斷會說「挑到粉紅色」而畫面上是白的，然後就開始懷疑人生。
     */
    static Style labelStyleOf(StyledText firstLine) {
        for (StyledTextPart part : firstLine) {
            String raw = part.getString(null, StyleType.NONE);
            if (!GlyphSplitter.hasLetter(raw)) {
                continue;                      // ✦ 那類純符號的段跳過
            }
            PartStyle ps = part.getPartStyle();
            return ps == null ? Style.EMPTY : ps.getStyle();
        }
        return null;
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
    /**
     * 這一段譯文要不要自己折行。
     *
     * <h2>兩種都得折</h2>
     * <ul>
     *   <li><b>攤平查到的</b>本來就是完整一句，沒有換行可言。</li>
     *   <li><b>精準命中的</b>也可能比原文少行——譯者常把三行的英文寫成一句中文，
     *       那在語料裡就是沒有換行的一整句。先前只折前者，
     *       於是後者在畫面上是一條長到衝出面板的行。</li>
     * </ul>
     *
     * <p>行數<b>一樣</b>的不折：那是譯者自己排好的形狀，動了反而更糟。
     * 比原文<b>多</b>的也不折——{@code wrapToBlock} 只會讓它更長。
     */
    static boolean needsWrap(String translated, int originalLines, boolean flowed) {
        return flowed || lines(translated) < originalLines;
    }

    private static String wrapToBlock(String text, List<StyledText> run) {
        int width = widestOf(run);
        if (width <= 0) {
            return text;
        }
        return wrapBalanced(text, width, run.size(),
                piece -> widthOf(Component.literal(piece)));
    }

    /**
     * 折行，量法可以換掉。<b>測試用</b>——正式的量法要 Minecraft 的字型，
     * 測試環境裡沒有，所有寬度都會量成 0，後面幾條規則就永遠不會被觸發。
     */
    static String wrapBalanced(String text, int maxPx, ToIntFunction<String> measure) {
        return wrapBalanced(text, maxPx, Integer.MAX_VALUE, measure);
    }

    /**
     * 把一句譯文折成好看的形狀。
     *
     * <h2>三條規則，由好到將就</h2>
     * <ol>
     *   <li><b>放得下就一行。</b>Major ID 大多是「名稱: 一句話」，中文比英文緊湊，
     *       原文兩行的往往一句就講完。硬折回兩行只是把一句完整的話剪成兩截。
     *       見 {@link #SNUG}。</li>
     *   <li><b>不然就在名稱後面斷。</b>「{@code ◆ 猛撲: }」換行「{@code 逃脫變成向前突進。}」——
     *       斷在冒號這個<b>語意</b>的接縫上，比斷在句子中間好讀。只在不會多出
     *       一行的時候才這樣做：說明長到要三四行的，名稱獨佔一行就太浪費了。</li>
     *   <li><b>都不行才貪心折行＋平均分配。</b>見 {@link #balance}。</li>
     * </ol>
     *
     * @param rows 原文有幾行；折出來不該比它多，超過就把寬度放寬再試
     */
    static String wrapBalanced(String text, int maxPx, int rows,
                               ToIntFunction<String> measure) {
        int width = maxPx;
        String wrapped = wrapToWidth(text, width, measure);
        for (int attempt = 0; attempt < WRAP_RETRIES && lines(wrapped) > rows; attempt++) {
            width = width * 11 / 10;
            wrapped = wrapToWidth(text, width, measure);
        }
        // ① 只差一點就放得下的，讓它留在同一行。
        //
        // 只在<b>剛好兩行</b>的時候放寬：三行以上的說明本來就長，為了它把整個
        // 面板撐寬四分之一不划算，而且也不會因此變成一行。
        if (lines(wrapped) == 2 && measure.applyAsInt(text) <= width * SNUG / 100) {
            return text;
        }
        // ② 斷在名稱後面。
        String head = labelBreak(text, width, rows, measure);
        if (head != null && lines(head) <= lines(wrapped)) {
            return head;
        }
        return balance(text, wrapped, width, measure);
    }

    /**
     * 試著把「名稱: 」單獨留在第一行。
     *
     * <p>會多出一行就不值得——{@code lines()} 由呼叫端比。折不出冒號、
     * 或名稱長到不像名稱的，回傳 {@code null} 表示這條路不通。
     */
    private static String labelBreak(String text, int width, int rows,
                                     ToIntFunction<String> measure) {
        int colon = text.indexOf(": ");
        if (colon <= 0 || colon > MAX_LABEL_LENGTH || text.indexOf(NEWLINE) >= 0) {
            return null;
        }
        String label = text.substring(0, colon + 1);
        if (measure.applyAsInt(label) > width) {
            return null;                        // 名稱自己就放不下，白做
        }
        // 剩下那半自己也要排得平均。名稱獨佔一行已經很短了，說明再折成
        // 「滿的一行 ＋ 零頭」，三行就會長短長，比不斷在名稱後面還醜。
        String body = text.substring(colon + 2);
        String rest = wrapToWidth(body, width, measure);
        rest = balance(body, rest, width, measure);
        return 1 + lines(rest) > rows ? null : label + NEWLINE + rest;
    }

    /**
     * 一行最多可以比原文的最寬那一行寬多少（百分比）。
     *
     * <p>譯文面板是我們自己畫的，寬度本來就跟著內容長——所以「差一點放不下」
     * 不必真的折成兩行。這個數字是<b>撐寬面板</b>與<b>把一句話剪成兩截</b>
     * 之間的取捨：太小就沒效果，太大則整份 tooltip 會被一句話撐得莫名其妙。
     */
    private static final int SNUG = 125;

    /**
     * 同樣的行數，把字排得平均一點。
     *
     * <h2>為什麼要做</h2>
     * 貪心折行會把第一行塞到再也塞不下為止，剩下的全掉到最後一行：
     *
     * <pre>
     *   ◆ 猛撲: 逃脫變成向前突
     *   進。                     ← 最後一行只剩兩個字
     * </pre>
     *
     * <p>英文的單字之間有空白可以退，看起來還好；中文每個字都能斷，
     * 於是第一行被塞滿、最後一行只剩零頭，還常常把一個詞切成兩半。
     *
     * <h2>做法</h2>
     * 行數<b>固定</b>的前提下，可用寬度越窄，每一行就被迫越接近那個寬度，
     * 最後一行的零頭自然被前面幾行讓出來的字補滿。所以把寬度一路往下收，
     * 收到再收就會多一行為止，取最後一個還是同樣行數的結果。
     *
     * <p><b>行數一個字都不動。</b>那是原文決定的、面板高度也照它算，
     * 這裡只重排同樣的字，不會讓任何東西溢出或縮排跑掉。
     *
     * @param wrapped 已經折好的結果；收不窄就原樣回傳
     */
    private static String balance(String text, String wrapped, int width,
                                  ToIntFunction<String> measure) {
        int rows = lines(wrapped);
        if (rows < 2) {
            return wrapped;                     // 一行沒得平均
        }
        String best = wrapped;
        int at = width;
        for (int step = 0; step < BALANCE_STEPS; step++) {
            int narrower = at * 19 / 20;        // 每次收 5%
            if (narrower <= 0 || narrower == at) {
                break;
            }
            String tighter = wrapToWidth(text, narrower, measure);
            if (lines(tighter) != rows) {
                break;                          // 再收就會多一行，停在上一個
            }
            if (splitsWord(tighter) && !splitsWord(wrapped)) {
                break;                          // 見 splitsWord：寧可不平均
            }
            best = tighter;
            at = narrower;
        }
        return best;
    }

    /**
     * 這個折行結果有沒有把英文單字切成兩半。
     *
     * <p>收窄行寬會讓某些行短到<b>放不下一整個英文單字</b>——{@code breaksWord}
     * 想退回上一個空白，但那一行根本沒有空白可退，於是 {@code Crystallize}
     * 被切成 {@code Crystalliz} 加 {@code e}。原本的貪心折行不會這樣，
     * 因為行寬夠。
     *
     * <p>所以平均分配每收窄一次都要檢查一遍：只要比原本多切開一個單字就停手。
     * 最後一行短一點還讀得懂，單字被劈成兩半就不行了。
     */
    private static boolean splitsWord(String wrapped) {
        for (int at = wrapped.indexOf(NEWLINE); at > 0 && at + 1 < wrapped.length();
                at = wrapped.indexOf(NEWLINE, at + 1)) {
            if (isWordChar(wrapped.charAt(at - 1)) && isWordChar(wrapped.charAt(at + 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 平均分配最多收幾次。每次 5%，二十次約收到原本的三成六——
     * 早在那之前就會多出一行而停下來，這個上限只是保證不會跑太久。
     */
    private static final int BALANCE_STEPS = 20;

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
        return wrapToWidth(text, maxPx, piece -> widthOf(Component.literal(piece)));
    }

    /**
     * @param measure 量一小段字有多寬。獨立成參數是為了讓測試跑得起來——
     *                正式的量法要 Minecraft 的字型，測試環境裡沒有，
     *                所有寬度都會量成 0，折行的邏輯就永遠不會被觸發。
     */
    static String wrapToWidth(String text, int maxPx, ToIntFunction<String> measure) {
        if (maxPx <= 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + 8);
        int lineStart = 0;
        int width = 0;
        int lastSpace = -1;
        int i = 0;
        while (i < text.length()) {
            // 正負號跟後面的佔位符是<b>同一個東西</b>，中間不能斷。
            // 先前 `+{~}` 會被拆成 `+` 與 `{~}` 兩塊，於是行尾留一個孤零零的
            // 「+」、數字掉到下一行——畫面上是「增加 +」換行「5 層數」。
            int at = i;
            if ((text.charAt(i) == '+' || text.charAt(i) == '-')
                    && i + 1 < text.length() && text.charAt(i + 1) == '{') {
                at = i + 1;
            }
            int close = text.charAt(at) == '{' ? text.indexOf('}', at) : -1;
            int end = close > at
                    ? close + 1                          // （帶號的）佔位符整組
                    : i + Character.charCount(text.codePointAt(i));
            String piece = text.substring(i, end);
            int pieceWidth = measure.applyAsInt(piece);
            if (width + pieceWidth > maxPx && i > lineStart) {
                // 退回上一個空白是為了不把<b>英文單字</b>切成兩半。中文可以在
                // 任何字之間斷，退回去只會讓整行提早結束——「✦ 利他主義: 16」
                // 之後就換行、剩下的擠成三行，就是這樣來的。
                int cut = breaksWord(text, i) && lastSpace > lineStart ? lastSpace : i;
                cut = avoidOrphan(text, cut, lineStart);
                out.append(text, lineStart, cut).append(NEWLINE);
                boolean atSpace = text.charAt(cut) == ' ';
                lineStart = atSpace ? cut + 1 : cut;
                lastSpace = -1;
                if (atSpace && cut == i) {
                    // 換行的位置<b>剛好就是這個空白</b>：它被行尾吃掉了，
                    // 不屬於新的一行。先前這裡直接算 substring(lineStart, i)，
                    // 而 lineStart 已經是 i + 1，於是 substring(24, 23) 當場丟出
                    // StringIndexOutOfBounds——被 catch 吞掉之後，整個翻譯面板
                    // 就不見了，看起來像「這件物品沒翻到」。
                    width = 0;
                    i = end;
                    continue;
                }
                width = measure.applyAsInt(text.substring(lineStart, i));
            }
            if (" ".equals(piece)) {
                lastSpace = i;
            }
            width += pieceWidth;
            i = end;
        }
        return out.append(text, lineStart, text.length()).toString();
    }

    /**
     * 中文排版的<b>避頭尾</b>：某些字不能出現在行首。
     *
     * <h2>畫面上長什麼樣</h2>
     * 句號、逗號、右括號被擠到下一行的開頭，看起來像上一句沒寫完、
     * 下一行憑空多了一個標點：
     *
     * <pre>
     *   每次命中使你獲得 +5 層 Crystallize
     *   ，並改為環繞你運行。          ← 逗號孤零零地開頭
     * </pre>
     *
     * <p>百分比與單位也算：{@code 250} 留在行尾、{@code %} 掉到下一行，
     * 讀起來是斷開的兩個東西。
     *
     * <p>做法是把斷點往前挪一個字，讓那個標點跟著上一行走。挪之後如果整行
     * 就沒東西了（行首本身就是標點）就放棄——寧可難看也不要無限迴圈。
     */
    private static int avoidOrphan(String text, int cut, int lineStart) {
        int at = cut;
        while (at > lineStart + 1 && at < text.length() && cannotStartLine(text.charAt(at))) {
            at--;
        }
        at = keepGlyphWithWord(text, at, lineStart);
        int outside = outsidePlaceholder(text, at, lineStart);
        if (outside != at) {
            // 退進佔位符裡面了。退得到它前面就退；退不了（佔位符就從行首開始）
            // 就<b>放棄這次避頭</b>，斷回原來的位置。標點落在行首只是難看，
            // 佔位符被切成兩半是真的壞掉——填值那一步認不得它，會整串印出來。
            at = outside > lineStart ? outside : cut;
        }
        return at > lineStart ? at : cut;
    }

    /**
     * 斷點不能落在佔位符<b>裡面</b>。
     *
     * <h2>畫面上長什麼樣</h2>
     * 使用者回報的 Major ID：
     *
     * <pre>
     *   為你恢復 15%、為友軍恢復 {~2
     *   }，但衝鋒不再造成傷害。
     * </pre>
     *
     * <p>兩個症狀是<b>同一個原因</b>。折行本身是照「片段」走的，{@code {~2}}
     * 會被當成一整塊；但 {@link #cannotStartLine} 的避頭是<b>逐字元</b>往回退的
     * ——斷點原本落在 {@code ，}（不能在行首），退一格就退進了佔位符裡面。
     *
     * <p>而 {@code {~2} 少了右括號就不再是佔位符（見 {@link #numberedAt}），
     * 填值那一步認不得它，只好當一般文字印出來。所以畫面上既斷錯行、
     * 又露出了 {@code {~2}} 這串字。
     *
     * <p>做法：退完之後如果人在 {@code &#123;} 與 {@code &#125;} 之間，
     * 就退到那個 {@code &#123;} 上，讓整個佔位符跟著下一行走。
     */
    private static int outsidePlaceholder(String text, int cut, int lineStart) {
        int open = text.lastIndexOf('{', cut - 1);
        if (open < lineStart) {
            return cut;
        }
        int close = text.indexOf('}', open);
        // close < cut 表示那個 { 早就收掉了，cut 不在它裡面。
        // 這裡只回報「在不在裡面、裡面的起點在哪」，要不要退由呼叫端決定——
        // 起點剛好是行首時退過去會生出一個空行，那時候該做的是放棄避頭。
        return close >= cut ? open : cut;
    }

    /**
     * 圖示不能落在<b>行尾</b>。
     *
     * <h2>畫面上長什麼樣</h2>
     * 「{@code 使用 {#} 物品鑑定師}」——那個 {@code {#}} 是物品鑑定師的圖示，
     * 是它的前綴。斷在兩者中間就成了：
     *
     * <pre>
     *   此物品的力量已被封印，使用 ◉
     *   物品鑑定師 即可解放其潛能。   ← 圖示孤零零留在上一行行尾
     * </pre>
     *
     * <p>這是 {@link #cannotStartLine} 的鏡像：那個管「不能在行首」的標點，
     * 這個管「不能在行尾」的圖示。做法一樣是把斷點往前挪，讓圖示跟著下一行走。
     * 圖示前面那個空白也一起挪——它是用來隔開圖示與前一個詞的，
     * 留在行尾只是多一格看不見的寬度。
     */
    private static int keepGlyphWithWord(String text, int cut, int lineStart) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = cut;
        while (true) {
            // <b>先</b>退過空白再看圖示。斷點落在圖示<b>後面那個空白之後</b>
            // （「使用 {#} 物品鑑定師」斷在「物」），緊鄰 cut 的是空白不是圖示——
            // 先前少了這一步，這個最常見的形狀反而沒被接住。
            int back = at;
            while (back > lineStart && text.charAt(back - 1) == ' ') {
                back--;
            }
            if (back - glyph.length() < lineStart
                    || !text.startsWith(glyph, back - glyph.length())) {
                return at > lineStart ? at : cut;
            }
            at = back - glyph.length();
        }
    }

    /** 不能出現在行首的字元。全形標點、收尾符號、百分比與單位。 */
    private static boolean cannotStartLine(char c) {
        return "，。、；：！？）」』】〉》%‰°′″…・".indexOf(c) >= 0;
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
        return translate(line, store, centered, false);
    }

    /**
     * @param leftAligned 這份 tooltip 的第二欄是<b>靠左</b>排的（見
     *                    {@link #columnsAreLeftAligned}）。預設 {@code false}
     *                    ——物品 tooltip 的數值是靠右的，那是絕大多數。
     */
    public static Component translate(StyledText line, TranslationStore store,
                                      boolean centered, boolean leftAligned) {
        Component whole = translateWholeLine(line, store, centered, leftAligned);
        if (whole != null) {
            return unslant(whole);
        }
        // 多行標籤（怪物名牌）整塊查不到時，逐行查——見 translatePerLine。
        Component perLine = translatePerLine(line, store, centered);
        return unslant(perLine != null ? perLine
                                       : translateSegments(line, store, centered, leftAligned));
    }

    /**
     * 用已經查到的譯文重建目前原文裡的參數。
     *
     * <p>Dialogue 逐字輸入時，prefix lookup 會先認出完整句，但地名或玩家名稱
     * 可能還沒打出來。只有譯文需要的佔位符都已經能從目前內容取得時才回傳結果；
     * 否則回傳 {@code null}，避免把字面上的 {@code {p}}、{@code {u}} 放進快取。
     *
     * @return 重建完成的譯文；佔位符尚未齊全或數量不符時回傳 {@code null}
     */
    public static Component translateKnown(StyledText line, String translated,
                                           TranslationStore store) {
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuild(translated, LineParts.of(line), store);
        return rebuilt == null ? null : unslant(realign(line, rebuilt, true));
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
                                               boolean centered, boolean leftAligned) {
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

        List<Piece> aligned = settle(alignColumns(pieces, leftAligned), line);
        // 每一行都記（同一句只記一次，見 LineDebug）。先前只記「有排版空白」的行，
        // 結果真正壞掉的那些——間隔不是用空白字元做的——反而完全看不到。
        LineDebug.pieces("逐片段 " + line.getStringWithoutFormatting(),
                describe(pieces, aligned, line));

        // 置中的行如果有<b>兩欄以上</b>，每一欄各自置中，不是右對齊。
        // 這一步從還沒補償過的 pieces 重算，見 {@link #recenterColumns}。
        if (centered && textGroups(pieces).size() >= 2) {
            return assemble(recenterColumns(pieces), -1, 0);
        }

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
     * 這份 tooltip 的第二欄是<b>靠左</b>排的嗎？
     *
     * <h2>為什麼一行看不出來</h2>
     * 兩種排版在單獨一行裡長得一模一樣，都是「文字 + 排版空白 + 文字」：
     *
     * <pre>
     *   Health Regen           +37%             ← 數值靠右，各行的右緣對齊
     *   +4 Ability Points      /Switch Command  ← 第二欄靠左，各行的起點對齊
     * </pre>
     *
     * 差別只有<b>看好幾行</b>才顯現：前者每一行的右緣落在同一個 x，
     * 後者每一行第二欄的起點落在同一個 x。
     *
     * <p>這件事決定補償的方向。{@link #alignColumns} 預設把譯文縮水的部分補回
     * 最後一段間隔，讓右緣回到原位——對數值是對的，對階級特權清單那種靠左的
     * 第二欄卻是災難：譯得越短就被推得越右，八行各歪一個量。
     *
     * <p>判斷靠量原文：每一行第二欄的起點與整行的右緣各收一份，起點比右緣
     * <b>更一致</b>就是靠左排。分不出來時回傳 {@code false}——物品 tooltip 的
     * 數值是靠右的，那是絕大多數。
     */
    public static boolean columnsAreLeftAligned(List<StyledText> lines) {
        if (lines == null || lines.size() < MIN_COLUMN_ROWS) {
            return false;
        }
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        for (StyledText line : lines) {
            int[] both = secondColumn(line);
            if (both != null) {
                starts.add(both[0]);
                ends.add(both[1]);
            }
        }
        if (starts.size() < MIN_COLUMN_ROWS) {
            return false;
        }
        return spread(starts) < spread(ends);
    }

    /** 要有這麼多「兩欄的行」才判斷得出來。兩行是巧合，三行才是排版。 */
    private static final int MIN_COLUMN_ROWS = 3;

    /**
     * 這一行第二欄的起點與整行的右緣（像素）。
     *
     * @return {@code {起點, 右緣}}；不是兩欄的行回傳 {@code null}
     */
    private static int[] secondColumn(StyledText line) {
        int x = 0;
        int start = -1;
        boolean sawGap = false;
        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();
            if (isAdjustableSpace(style, raw)) {
                int px = SpaceOffset.decode(raw);
                // 夠寬的間隔才是欄位交界；一兩像素只是字距
                if (px >= COLUMN_GAP_PX && start < 0 && x > 0) {
                    sawGap = true;
                }
                x += px;
                continue;
            }
            if (sawGap && start < 0 && !raw.isBlank()) {
                start = x;
            }
            x += widthOf(literal(raw, style));
        }
        return start < 0 ? null : new int[] {start, x};
    }

    /** 一段間隔要這麼寬才算欄位交界，而不只是字距。 */
    private static final int COLUMN_GAP_PX = 8;

    /** 最大減最小。越小代表這些位置越一致。 */
    static int spread(List<Integer> values) {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int v : values) {
            low = Math.min(low, v);
            high = Math.max(high, v);
        }
        return high - low;
    }

    /**
     * 一行裡的「文字群組」：連續的非空白片段算一組，中間的排版空白是欄與欄的間隔。
     *
     * @return 每一組的 {@code {起, 迄(不含)}}
     */
    static List<int[]> textGroups(List<Piece> pieces) {
        List<int[]> groups = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).isSpace()) {
                if (start >= 0) {
                    groups.add(new int[] {start, i});
                    start = -1;
                }
            } else if (start < 0) {
                start = i;
            }
        }
        if (start >= 0) {
            groups.add(new int[] {start, pieces.size()});
        }
        return groups;
    }

    /**
     * 置中的多欄行：讓每一欄<b>各自</b>回到原本的中心，而不是回到原本的左緣或右緣。
     *
     * <h2>為什麼右對齊那一套在這裡是錯的</h2>
     * 技能點數面板的那一列，原文是
     *
     * <pre>
     *   [2px] 83 points [15px][30px][15px] 84 points
     * </pre>
     *
     * 15 + 30 + 15 不是隨便湊的：那是「左欄右邊留 15、中間空 30、右欄左邊留 15」，
     * 也就是<b>兩欄各自置中</b>。{@link #alignColumns} 會把左欄的縮水補進間隔，
     * 讓右欄的<b>起點</b>回到原位；接著「最後一欄的右緣也要對回去」那一步又把
     * 右欄的縮水加到<b>同一個</b>間隔上——同一段間隔被補了兩次，右欄整個往右推。
     * 畫面上就是「83 點」還在原位、「84 點」卻擠到右邊去。
     *
     * <p>那一套是為「標籤靠左、數值靠右」寫的，對這種行不適用。這裡改成量出
     * 原文每一欄的中心，再反推間隔，讓譯文的每一欄壓在同一個中心上。單欄的
     * 置中行結果與舊做法相同（就是把差額對半分），所以只有多欄行走這條路。
     *
     * <h2>間隔不收到負的</h2>
     * 譯文比原文寬時，理想的間隔可能是負的——那會讓兩欄疊在一起。寧可讓它
     * 往右偏也不要疊字，所以夾在 0 以上，與 {@link #narrowed} 同一個取捨。
     */
    static List<Piece> recenterColumns(List<Piece> pieces) {
        List<int[]> groups = textGroups(pieces);
        int[] wantStart = new int[groups.size()];
        int[] newWidth = new int[groups.size()];

        int x = 0;
        int g = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (p.isSpace()) {
                x += p.spacePx();
                continue;
            }
            if (g < groups.size() && groups.get(g)[0] == i) {
                int origWidth = 0;
                int width = 0;
                for (int k = groups.get(g)[0]; k < groups.get(g)[1]; k++) {
                    origWidth += widthOf(pieces.get(k).orig());
                    width += widthOf(pieces.get(k).rendered());
                }
                newWidth[g] = width;
                // 中心對中心：起點 = 原本的中心 - 譯文寬度的一半
                wantStart[g] = x + (origWidth - width) / 2;
                x += origWidth;
                g++;
            }
        }

        List<Piece> out = new ArrayList<>(pieces.size());
        int cursor = 0;
        g = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (!p.isSpace()) {
                out.add(p);
                if (g < groups.size() && groups.get(g)[1] == i + 1) {
                    cursor += newWidth[g];
                    g++;
                }
                continue;
            }
            // 一段間隔可能由好幾個偏移字元組成（15 + 30 + 15）。整段的寬度
            // 一次算在最後一個上面，前面的收成 0——畫面上是同一個位置，
            // 但只有一個地方需要調整，不會重複補償。
            boolean lastOfRun = i + 1 >= pieces.size() || !pieces.get(i + 1).isSpace();
            if (!lastOfRun) {
                out.add(Piece.space(0, p.style()));
                continue;
            }
            if (g >= groups.size()) {
                // 行尾的留白：譯文不需要它撐寬度，原樣留著即可
                out.add(p);
                continue;
            }
            int gap = Math.max(0, wantStart[g] - cursor);
            out.add(Piece.space(gap, p.style()));
            cursor += gap;
        }
        return out;
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
    static List<Piece> alignColumns(List<Piece> pieces, boolean leftAligned) {
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
            if (isAlignSpace(pieces, i) && !isBacktrack(p)) {
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
        // ……但只有在這個空白<b>前面真的有標籤</b>時才成立。
        //
        // 書卷的標題長這樣：[圖示][偏移][圖示][偏移][書卷名][ 60.0%]。
        // 那些偏移兩側都是圖示，會被當成欄位交界，而唯一會變短的是<b>後面</b>
        // 的書卷名——把差額補進它前面的空白，等於把整個標題改成靠右對齊，
        // 中文短了多少就往右推多少。沒有 [60.0%] 的書卷剛好沒有這個空白，
        // 所以只有一部分書卷歪掉，看起來像隨機發生。
        if (lastAdjusted >= 0 && drift != 0 && !leftAligned
                && labelled(out, lastAdjusted)) {
            Piece p = out.get(lastAdjusted);
            out.set(lastAdjusted,
                    Piece.space(narrowed(p.spacePx(), p.spacePx() - drift), p.style()));
        }
        return out;
    }

    /**
     * 這個偏移是「往回退」而不是欄距嗎。
     *
     * <h2>原料袋的星星</h2>
     * Wynncraft 的星級是<b>疊</b>出來的：先畫三顆灰色的空星，再用一個負偏移
     * 退回起點，把彩色的實星疊上去。原料袋那一行的原文是
     *
     * <pre>
     *   「1 x 」「Ripe Aureate Fruit」「 」「灰星×3」「-23px」「彩星×3」
     * </pre>
     *
     * <p>那個 -23 的值是照<b>灰星的寬度</b>挑的，跟前面的名字有多長無關。
     * 譯名從 92px 縮到 36px 之後，補償邏輯為了守住整行寬度把它改成 +33，
     * 於是彩星不再疊在灰星上，而是被推到行尾——畫面上就是「一行有兩組星星」。
     *
     * <p>所以負偏移一律原樣保留，累積的差額留給後面第一個正的欄距去吸收；
     * 整行都沒有正欄距時就讓它短一點。行短一點沒人看得出來，
     * 疊字散開是一眼就看到的。
     */
    private static boolean isBacktrack(Piece space) {
        return space.spacePx() < 0;
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
            if (isAlignSpace(pieces, i) && !isBacktrack(pieces.get(i))
                    && labelled(pieces, i)) {
                last = i;      // 負偏移是疊字用的，見 isBacktrack
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

    /**
     * 這個空白前面有沒有<b>真正的文字</b>——不是圖示，也不是偏移。
     *
     * <p>「把殘差補回最後一個對齊欄」這件事，前提是那個欄位左邊有一個
     * 不會動的標籤（{@code Health}、{@code Class:}），右邊是靠右排的數值。
     * 前面只有圖示的話，這一行根本沒有欄位可言——它是標題，補下去只會
     * 把整行往右推。
     *
     * <p>圖示與偏移都落在私人使用區，不是字母也不是數字，所以用
     * {@link Character#isLetterOrDigit} 就分得出來。
     */
    static boolean labelled(List<Piece> pieces, int index) {
        for (int i = 0; i < index && i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (p.isSpace()) {
                continue;
            }
            String text = p.orig() == null ? null : p.orig().getString();
            for (int k = 0; text != null && k < text.length(); k++) {
                if (Character.isLetterOrDigit(text.charAt(k))) {
                    return true;
                }
            }
        }
        return false;
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
     * <p>所以只夾<b>原本是正的</b>那些。原本就是負的不夾在 MIN_GAP 以上——
     * 那是排版設計，不是我們算出來的。對不齊也比黏在一起好讀。
     *
     * <h2>但負間隔也不能<b>更</b>負</h2>
     * 先前負間隔是原樣通過的，於是中文標籤比英文長的時候，補償會把它算得
     * 更負——技能樹的 {@code Ice Snake Cost -6%} 翻成「Ice Snake 消耗百分比」
     * 之後，數值被往回拉進標籤裡，畫面上是<b>兩層字疊在一起</b>，
     * 使用者看到的是「消耗百分5%」這種讀不出來的東西。
     *
     * <p>負間隔的用意是讓數值往回貼進標籤<b>尾巴的留白</b>，那個留白是照英文
     * 算的；中文沒有那段留白，再往回就是壓到字上。所以補償只准把間隔放寬，
     * 不准收得比原文還緊。譯文變短時照常補償（那是往寬的方向），
     * 變長時最多維持原本的間隔——數值會往右偏，但至少讀得出來。
     *
     * @param original 原文的間隔
     * @param adjusted 補償之後的間隔
     */
    static int narrowed(int original, int adjusted) {
        return original > 0 ? Math.max(MIN_GAP, adjusted)
                            : Math.max(original, adjusted);
    }

    /**
     * 見 {@link #narrowed}。
     *
     * <p>本來是 4（一個半形空格）。技能樹的標籤改用半形冒號之後，冒號本身只有
     * 2px 寬、右邊也沒有全形字自帶的留白，4px 讀起來就像數值黏在冒號上。
     */
    private static final int MIN_GAP = 6;

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
        // 只剩一個空格時，看標籤<b>有沒有帶冒號</b>。
        //
        // 這是兩個介面實際的差別，不是猜的：
        //   技能樹  `Fire Damage: +15%`   —— 數值緊跟在標籤後面，各行不對齊
        //   物品欄  `Main Attack Damage -17%` —— 數值排在同一欄，靠留白頂過去
        //
        // 帶冒號的當成欄位去補償，中文標籤一短就生出一大塊空白，
        // 間隔跟沒翻譯時一樣寬。不帶冒號的則相反：那是真正的欄位，
        // `Main Attack Damage` 是整份 tooltip 最長的標籤，剛好把數值頂到定位，
        // 中間只剩一個空格——不補償它就會脫離其他行的欄位。
        return spaces >= 1 && startsWithSign(pieces, boundary)
                && !labelEndsWithColon(pieces, boundary);
    }

    /** 交界之前那段文字是不是以冒號收尾。見 {@link #hasColumnGap}。 */
    private static boolean labelEndsWithColon(List<Piece> pieces, int boundary) {
        for (int i = boundary - 1; i >= 0; i--) {
            String text = pieces.get(i).text().strip();
            if (text.isEmpty()) {
                continue;
            }
            return isTrailingColon(text.charAt(text.length() - 1));
        }
        return false;
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
            if (text.endsWith("%")) {
                return true;
            }
            // 這裡<b>不能</b>就這樣回傳 false。未鑑定的裝備寫的是範圍
            // 「-39 to -21%」，百分號掛在<b>後面</b>那個數字上；碰到第一個
            // 數字就下結論的話，整行會被當成 raw，於是「生命回復百分比」
            // 退成「生命回復」——標籤跟實際數值對不起來。
        }
        return false;
    }

    /**
     * 純粹的數值：可帶正負號、千分位、小數，可帶結尾百分號。
     *
     * <p>也接受「{@code a to b}」這種範圍——未鑑定的裝備整段是同一個片段，
     * 不放進來的話它連數值都不算，百分比判斷等於沒看到它。
     */
    private static final java.util.regex.Pattern VALUE =
            java.util.regex.Pattern.compile(
                    "[+-]?\\d[\\d,.]*%?(?:\\s+to\\s+[+-]?\\d[\\d,.]*%?)?");

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
        // 全形標點自帶右側留白，後面不再接原文的半形空格——留白疊留白，
        // 間隔就會變成兩倍。技能樹整片都是「標籤: 數值」，每一行都多這麼一塊。
        String tail = reattach(rebuilt.getString(), suffix);
        if (!tail.isEmpty()) {
            out.append(literal(tail, style));
        }
        return out;
    }

    /** 整行查表。散文與對話用這條路，因為它們需要跨片段重排語序。 */
    private static Component translateWholeLine(StyledText line, TranslationStore store,
                                                boolean centered, boolean leftAligned) {
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
        Component result = realign(line, rebuilt, centered, leftAligned);
        LineDebug.record(line, result);
        return result;
    }

    /**
     * 多行標籤：整塊查不到時，逐行查。
     *
     * <h2>為什麼需要</h2>
     * 怪物名牌是<b>單一個含換行的 StyledText</b>——名字、等級、血條全在同一個鍵裡
     * （{@code "Sylphid Gatekeeper {#}{#}\n{#} {#} {#}"}）。於是同一隻怪只要多出
     * 一行（血條、任務提示、{@code "to dock"}），就變成另一個鍵，得再翻一次。
     * 玩家看到的是「這個角度有翻、那個角度沒翻」。
     *
     * <p>tooltip 沒有這個問題，因為它走 {@link #translateBlock}，那是吃<b>已經
     * 分好行的 list</b>；名牌只有一個 StyledText，走不到那條路。
     *
     * <h2>為什麼接得回去</h2>
     * 靠 {@link #rebuildAll} 既有的設計：<b>佔位符不是逐行對，而是整段照順序
     * 取用</b>。查不到的那幾行原樣留著，佔位符的順序就沒有被打亂，接起來的
     * 字串仍然對得上原文的碎片。
     *
     * <p>只在<b>有換行、而且至少一行查得到</b>時才回傳結果。單行進來立刻
     * {@code null}，走原本的路——所以 tooltip 完全不受影響。
     *
     * @return 逐行填好的一行；沒有換行、或一行都查不到時回傳 {@code null}
     */
    private static Component translatePerLine(StyledText line, TranslationStore store,
                                              boolean centered) {
        LineParts parts = LineParts.of(line);
        String template = parts.template();
        if (template.indexOf(NEWLINE) < 0) {
            return null;                       // 單行沒有「逐行」可言
        }
        String[] rows = template.split("\n", -1);
        StringBuilder joined = new StringBuilder(template.length());
        boolean any = false;
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                joined.append(NEWLINE);
            }
            String row = rows[i];
            // 純佔位符的行（血條就是 {#} {#} {#}）沒東西可查，直接留著。
            String hit = GlyphSplitter.hasLetter(row) ? lookup(row, store) : null;
            if (hit != null && !hit.isBlank()) {
                joined.append(hit);
                any = true;
            } else {
                joined.append(row);            // 查不到就留原文那一行
            }
        }
        if (!any) {
            return null;                       // 一行都沒命中，讓後面的路去試
        }
        Component rebuilt = rebuild(joined.toString(), parts, store);
        if (rebuilt == null) {
            return null;                       // 佔位符對不上就放棄，不硬塞
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
    /**
     * 沒有「欄」可言的呼叫端用這個：對話、逐行名牌、聊天。右緣補償不適用，
     * 傳 {@code leftAligned=true} 等於維持原本「只保欄位起點」的行為。
     */
    private static Component realign(StyledText original, Component rebuilt,
                                     boolean centered) {
        return realign(original, rebuilt, centered, true);
    }

    private static Component realign(StyledText original, Component rebuilt,
                                     boolean centered, boolean leftAligned) {
        List<Run> orig = splitGaps(runs(original.getComponent()));
        List<Run> made = splitGaps(runs(rebuilt));
        // 多行的要一行一行重算——每一行有自己的置中縮排。見 #realignRows。
        StringBuilder log = null;
        if (rows(orig) > 1 || rows(made) > 1) {
            log = new StringBuilder();
            log.append("  原文行數：").append(rows(orig))
               .append("  譯文行數：").append(rows(made))
               .append("  置中：").append(centered)
               .append(System.lineSeparator());
            Component perRow = realignRows(orig, made, centered, log);
            if (perRow != null) {
                FlowedDebug.rows(original.getString(), log.toString());
                return perRow;
            }
            // 逐行對不上就<b>什麼都不要動</b>。
            //
            // 下面那一段是為 tooltip 的「欄位交界」寫的：整段只有一個空白時，
            // 它會把<b>整段</b>的寬度差全部加到那一個空白上。一行的 tooltip 這樣
            // 是對的，多行的聊天訊息這樣是災難——實機錄到的 [Cave Completed]
            // 就是這樣壞的（診斷檔「逐行對齊 9」）：
            //
            // <pre>
            //   原文行數：8  譯文行數：6  逐行重算：放棄
            //   原文段寬=[750, 101]  譯文段寬=[528, 45]  補正=[222]
            // </pre>
            //
            // 那個 +222px 加在最後一行的定位空白上，「未鑑定頭盔」就被推到
            // 螢幕最右邊去了。中文比英文短是<b>整段</b>加起來的差，不該由
            // 某一行的某一個空白獨自吸收。
            log.append("  逐行重算：放棄；多行的不套整段那一路，原樣返回")
               .append(System.lineSeparator());
            FlowedDebug.rows(original.getString(), log.toString());
            return rebuilt;
        }
        int spaces = countSpaces(made);
        if (spaces == 0 || spaces != countSpaces(orig)) {
            if (log != null) {
                log.append("  整段：空白數 原文=").append(countSpaces(orig))
                   .append(" 譯文=").append(spaces)
                   .append("，對不上就原樣返回")
                   .append(System.lineSeparator());
                FlowedDebug.rows(original.getString(), log.toString());
            }
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
        int lastAdjusted = -1;
        int[] gapPx = gapPixels(made);
        for (int k = leading ? 1 : 0; k < spaces; k++) {
            drift += madeSeg.get(k) - origSeg.get(k);
            // 兩側都有文字才是欄位交界；行尾的留白邊距不能動。
            // 圖示疊字用的負偏移也不能動，見 #overlayGap。
            if (madeSeg.get(k) > 0 && madeSeg.get(k + 1) > 0
                    && !overlayGap(made, k, gapPx[k])) {
                adjust[k] = -drift;
                drift = 0;
                lastAdjusted = k;
            }
        }

        // 最後一欄的<b>右緣</b>也要對回去。
        //
        // 上面那一輪只補「間隔之前」的寬度變化，所以數值的<b>起點</b>會落回原位
        // ——但數值本身如果也翻譯了（{@code Mage/Dark Wizard} → 「法師/闇導士」），
        // 它的<b>結尾</b>就短了一截，而 Wynncraft 是把數值靠右排的，右緣就對不齊。
        //
        // 這一步 {@link #alignColumns}（逐片段那條路）早就有了，整行查表這條路
        // 一直沒有——同一份 tooltip 裡兩條路各走各的，於是同一個症狀反覆出現：
        // 補償沒跑時數值往左，只補了標籤時數值又像被推到右邊。
        //
        // 條件跟 alignColumns 那邊一樣：只有第二欄靠右（{@code !leftAligned}）、
        // 而且那個間隔<b>前面真的有標籤</b>時才做。前面沒有標籤的間隔是圖示之間的
        // 排版，把差額補進去等於把整行改成靠右對齊。
        int tail = madeSeg.get(spaces) - origSeg.get(spaces);
        if (!leftAligned && lastAdjusted >= 0 && tail != 0
                && labelledRun(made, lastAdjusted)) {
            adjust[lastAdjusted] -= tail;
        }

        // 單行的也要記。先前只有多行的會寫診斷，而<b>欄位錯位一直是單行的問題</b>
        // ——實機回報了三次「職業類型那一行跑掉」，每一次都沒有數字可看，
        // 只能靠讀 line-debug 的片段反推。
        if (log == null) {
            log = new StringBuilder();
        }
        log.append("  整段：").append(describeRow(origSeg, madeSeg, adjust, leading))
           .append("  右緣補正=").append(-tail)
           .append(leftAligned ? "（靠左，不套用）" : "")
           .append(System.lineSeparator());
        FlowedDebug.rows(original.getString(), log.toString());
        return apply(made, adjust);
    }

    /** 診斷用的一行摘要：兩邊的段寬與算出來的補正。 */
    private static String describeRow(List<Integer> origSeg, List<Integer> madeSeg,
                                      int[] adjust, boolean leading) {
        return "前導=" + leading + " 原文段寬=" + origSeg + " 譯文段寬=" + madeSeg
                + " 補正=" + java.util.Arrays.toString(adjust);
    }

    /** 一段連續的同型內容：不是排版空白，就是文字。 */
    /**
     * 逐行重算置中縮排。
     *
     * <h2>為什麼不能整段一起算</h2>
     * 聊天的系統訊息是<b>多行的</b>，而每一行前面都有一個
     * 自己的置中縮排——實機錄到的歡迎訊息（診斷檔
     * 「填回去的符號 2」）就是三個：
     *
     * <pre>U+D0059  U+D003B  U+D003C   font=minecraft:space</pre>
     *
     * <p>那些數字是照<b>英文寬度</b>算出來的。原樣填回去之後，
     * 中文那一块每行寬度都不一樣，整块就歪了。
     *
     * <p>舊的 {@code realign} 把整段當一行，只修得到第一個縮排，
     * 剩下的會走到「欄位交界」那個分支——而那是為 tooltip 寫的。
     *
     * @return 重算後的整段；兩邊行數對不上就回傳 {@code null}，讓呼叫端走舊路
     */
    private static Component realignRows(List<Run> orig, List<Run> made,
                                         boolean centered, StringBuilder log) {
        List<List<Run>> origRows = splitRows(orig);
        List<List<Run>> madeRows = splitRows(made);
        // 首尾的空行不算數。
        //
        // 語料的鍵是<b>去掉首尾空白</b>之後的樣子（見 TranslationStore#lookup），
        // 而遊戲送來的訊息前後常常多幾個空行。實機錄到的 [Cave Completed]
        // 原文八行、譯文六行，差的就是那兩行——中間那六行是一一對得上的。
        // 硬要求行數完全相同，這種訊息一輩子對不齊。
        int[] keepOrig = solidRows(origRows);
        int[] keepMade = solidRows(madeRows);
        if (keepOrig[1] - keepOrig[0] != keepMade[1] - keepMade[0]) {
            if (log != null) {
                log.append("  去掉首尾空行之後：原文 ")
                   .append(keepOrig[1] - keepOrig[0]).append(" 行、譯文 ")
                   .append(keepMade[1] - keepMade[0]).append(" 行，還是對不上")
                   .append(System.lineSeparator());
            }
            return null;
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < madeRows.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(NL));
            }
            if (i < keepMade[0] || i >= keepMade[1]) {
                out.append(apply(madeRows.get(i), new int[0]));   // 空行，原樣
                continue;
            }
            if (log != null) {
                log.append("  [").append(i).append("] ");
            }
            out.append(realignRow(origRows.get(keepOrig[0] + i - keepMade[0]),
                                  madeRows.get(i), centered, log));
        }
        return out;
    }

    /**
     * 首尾的空行不算；回傳 {@code [起, 迄)}。見 {@link #realignRows}。
     *
     * <h2>「空」的定義要跟語料的鍵一致</h2>
     * 先前只認<b>完全沒有片段</b>的行。但語料的鍵是 {@code strip()} 過的
     * （{@link LineParts#of} 一次、{@code TranslationStore} 再一次），
     * 而 {@code strip()} 會把<b>只有空白</b>的首尾行也一起去掉。
     *
     * <p>兩邊的定義不一致，「洞穴完成」那一塊就永遠對不齊——實機診斷寫得很清楚：
     *
     * <pre>
     *   === 聊天對齊 11 ===（沒翻到：行數對不上，原樣返回不動排版）
     *     去掉首尾空行之後：原文 7 行、譯文 6 行
     * </pre>
     *
     * 多出來的那一行是訊息開頭的 {@code "  "}——兩個空白，不是空的，
     * 所以沒被去掉；而它在語料的鍵裡早就被 {@code strip()} 掉了，
     * 譯文根本不可能有那一行。
     *
     * <p>所以這裡也改看「有沒有實字」：空白、排版偏移都算空。
     */
    private static int[] solidRows(List<List<Run>> rows) {
        int from = 0;
        int to = rows.size();
        while (from < to && blankRow(rows.get(from))) {
            from++;
        }
        while (to > from && blankRow(rows.get(to - 1))) {
            to--;
        }
        return new int[] {from, to};
    }

    /** 這一行有沒有實字。排版空白與偏移都不算。 */
    private static boolean blankRow(List<Run> row) {
        return rowText(row).isBlank();
    }

    /** 一行的重算；跟舊 {@code realign} 內層同一套邏輯。 */
    private static Component realignRow(List<Run> orig, List<Run> made,
                                        boolean centered, StringBuilder log) {
        int spaces = countSpaces(made);
        if (spaces == 0 || spaces != countSpaces(orig)) {
            if (log != null) {
                log.append("不動：空白數 原文=").append(countSpaces(orig))
                   .append(" 譯文=").append(spaces)
                   .append("  譯文=「").append(rowText(made)).append("」")
                   .append(System.lineSeparator());
            }
            return apply(made, new int[0]);
        }
        List<Integer> origSeg = segmentWidths(orig);
        List<Integer> madeSeg = segmentWidths(made);
        int[] adjust = new int[spaces];
        boolean leading = centered && origSeg.get(0) == 0 && madeSeg.get(0) == 0;
        if (leading) {
            adjust[0] = (sum(origSeg) - sum(madeSeg)) / 2;
        }
        int drift = 0;
        int[] gapPx = gapPixels(made);
        for (int k = leading ? 1 : 0; k < spaces; k++) {
            drift += madeSeg.get(k) - origSeg.get(k);
            if (madeSeg.get(k) > 0 && madeSeg.get(k + 1) > 0
                    && !overlayGap(made, k, gapPx[k])) {
                adjust[k] = -drift;
                drift = 0;
            }
        }
        if (log != null) {
            log.append(describeRow(origSeg, madeSeg, adjust, leading))
               .append("  譯文=「").append(rowText(made)).append("」")
               .append(System.lineSeparator());
        }
        return apply(made, adjust);
    }

    /**
     * 聊天訊息的譯文。
     *
     * <h2>為什麼聊天要自己一條路</h2>
     * 聊天訊息的縮排跟 tooltip 是<b>兩件事</b>。tooltip 是一個寬度自己算出來的框，
     * 置中就是「相對於這一份 tooltip 最寬那行置中」；聊天訊息是 Wynncraft 依
     * <b>聊天視窗</b>的固定寬度排好的，同一塊裡有置中的標題，也有靠左的清單。
     *
     * <p>先前整條聊天路徑寫死 {@code centered = true}——每一行都被當成置中的，
     * 於是靠左的獎勵清單全部被往右推了半個寬度差。實機那張「任務完成」的圖裡，
     * 譯文的「獎勵:」比原文的「Rewards:」右邊一截，就是這樣來的。
     *
     * <p>這裡改成<b>先問過整塊</b>（見 {@link BlockLayout}）：整塊都吻合置中的算式
     * 才逐行重新置中，否則一律沿用原文那一行的左緣。沿用左緣的結果是譯文那塊
     * 跟原文那塊<b>形狀一樣</b>，那正是回報要的。
     *
     * <h2>順便修好只用空白縮排的那種</h2>
     * 「[Cave Completed]」那塊的縮排是<b>真的空白字元</b>，不是排版偏移，
     * 所以 {@code realignRows} 的「空白數對不上就不動」把它整個放掉了。
     * 更糟的是語料的鍵會被 {@code strip()} 掉頭尾空白（見
     * {@link LineParts#of} 與 {@code TranslationStore}），第一行的縮排連
     * 寫都寫不進譯文——畫面上就是「[洞穴完成]」孤零零貼在最左邊。
     *
     * <p>{@link #leadWidth} 把真空白也算成縮排，缺多少就在行首補一個偏移，
     * 語料裡寫不寫得下都不影響。
     *
     * @param centred 已經知道這一行是不是置中的就傳進來（整塊逐行查表那條路
     *                會先算好）；{@code null} 表示由這裡自己判斷
     * @return 譯好的訊息；查不到或佔位符對不上時回傳 {@code null}
     */
    public static Component translateChat(StyledText message, TranslationStore store) {
        return translateChat(message, store, null);
    }

    public static Component translateChat(StyledText message, TranslationStore store,
                                          Boolean centred) {
        LineParts parts = LineParts.of(message);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        // 「沒翻到」跟「翻了但沒對齊」是兩種病。先前查不到就安靜回傳 null，
        // 於是診斷檔裡那一塊完全不存在，看起來像沒被呼叫到——分不出是哪一種。
        String translated = lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            // 整行查不到時，多欄的行改成<b>一欄一欄查</b>。見 #byColumn。
            translated = byColumn(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            FlowedDebug.chatRows(message.getString(), "  鍵：" + parts.template(),
                                 "語料裡查不到這一塊");
            return null;
        }
        Component rebuilt = rebuild(translated, parts, store);
        if (rebuilt == null) {
            FlowedDebug.chatRows(message.getString(), "  譯文：" + translated,
                                 "佔位符數量對不上，整塊放棄");
            return null;
        }
        return unslant(realignChat(message, rebuilt, centred));
    }

    /**
     * 多欄的行：整行查不到就<b>一欄一欄</b>查，再接回去。
     *
     * <h2>為什麼需要</h2>
     * 獵殺信標的選單是兩欄併排，而兩欄的內容是<b>各自獨立</b>抽出來的：
     *
     * <pre>
     *   {#}Purple Beacon{#}Blue Beacon
     *   {#}Empower next Beacon{#}+{~} Beacon Choice
     * </pre>
     *
     * 聊天走的是「只認整行的鍵」那條路，所以每一種左右配對都要在語料裡各列一條。
     * 12 種信標顏色配上十幾種效果，組合是上百種——語料裡已經硬列了 <b>728 條</b>
     * 成對條目，玩家還是三天兩頭遇到沒翻到的。
     *
     * <p>但那些欄位<b>單獨</b>的譯文其實都有（{@code {#}Empower next Beacon}、
     * {@code {#}Reward Pulls}）。一欄一欄查就能組出任何配對，組合爆炸就消失了。
     *
     * <h2>安全性</h2>
     * 只用語料裡<b>已經有的</b>單欄譯文，不自己拆句子、不自己編。查不到的那一欄
     * 原樣留著英文。所以最壞的情況是「一半中文一半英文」——而那本來就是現在
     * 查不到整行時的樣子（整行都是英文），不會更糟。
     *
     * <p>至少要有一欄查到才回傳，否則交還給呼叫端當作「查不到」。
     *
     * @return 接好的譯文；不是多欄、或一欄都沒查到時回傳 {@code null}
     */
    private static String byColumn(String template, TranslationStore store) {
        // ★ 只處理<b>單獨一行</b>。欄位是一行之內的概念，跨行切會把行黏在一起。
        //
        // ChatBlock 會先把整塊接成一個含換行的字串再查一次。那時候切出來的段
        // 長這樣：「{#}Choose a Beacon!⏎」——而 lookup 會 strip() 掉尾端的換行，
        // 於是查得到、換行卻不見了，兩行就併成一行。實機畫面上「選擇一個信標！」
        // 與「走向其中一個即可開始挑戰」擠在同一行，整塊版面跟著垮掉。
        if (template.indexOf(NEWLINE) >= 0) {
            return null;
        }
        List<String> parts = splitColumns(template);
        if (parts.size() < 2) {
            return null;                       // 不是多欄的行
        }
        StringBuilder out = new StringBuilder();
        int hits = 0;
        int colours = 0;                       // 前面幾欄一共用掉幾個顏色
        int values = 0;                        // 前面幾欄一共有幾個數值
        for (String part : parts) {
            String hit = GlyphSplitter.hasLetter(part) ? lookup(part, store) : null;
            if (hit != null && !hit.isBlank()) {
                String shifted = renumber(shiftColours(hit, colours), values);
                if (shifted == null) {
                    return null;               // 編號會超過一位數，整條讓開
                }
                colours += distinctColours(hit);
                out.append(shifted);
                hits++;
            } else {
                out.append(part);              // 這一欄沒有譯文，原樣留著
            }
            // ★ 不管這一欄有沒有譯文都要累加——填值走的是<b>原文</b>那一行的
            //   數值表，沒翻到的欄一樣佔著位置。
            values += countNumbers(part);
        }
        return hits > 0 ? out.toString() : null;
    }

    /** 這一段裡有幾個 {@code {~}}。 */
    private static int countNumbers(String template) {
        String mark = GlyphSplitter.NUMBER_PLACEHOLDER;
        int n = 0;
        for (int at = template.indexOf(mark); at >= 0;
                at = template.indexOf(mark, at + mark.length())) {
            n++;
        }
        return n;
    }

    private static final java.util.regex.Pattern NUMBER_ANY =
            java.util.regex.Pattern.compile("\\{~([1-9])?}");

    /**
     * 把一欄的譯文從「這一欄的第幾個數值」改寫成「整行的第幾個」。
     *
     * <h2>為什麼要這一步</h2>
     * {@code {~N}} 指的是<b>原文的第 N 個數值</b>，而填值時走的是整行的數值表
     * （見填值迴圈的 {@code NUMBER} 那一支）。一欄一欄查到的譯文是照<b>那一欄</b>
     * 編號的，直接接起來就會錯位。
     *
     * <p>實機回報的就是這個：Lootrun 結算是兩欄一行，
     * 「{@code 45 Reward Pulls｜Time Elapsed: 12:39}」。右欄單獨查到的譯文是
     * 「經過時間：{@code {~1}:{~2}}」，接起來之後 {@code {~1}} 指到整行的第一個
     * 數值——左欄的 45。畫面上就成了「經過時間：45:12」。
     *
     * <h2>連沒編號的也一起改寫</h2>
     * 沒編號的 {@code {~}} 是照出現順序取，用的是<b>另一個</b>計數器
     * （帶編號的不會讓它前進）。兩種寫法混在不同欄裡接起來一樣會亂，
     * 所以偏移不為零時一律改寫成指名的形式，讓結果不依賴那個計數器。
     *
     * @return 改寫後的譯文；編號會超過一位數（{@code {~10}} 填不回去）時回傳
     *         {@code null}，呼叫端應該整條讓開
     */
    static String renumber(String text, int offset) {
        if (offset == 0 || text == null || text.indexOf('{') < 0) {
            return text;
        }
        java.util.regex.Matcher m = NUMBER_ANY.matcher(text);
        StringBuilder out = new StringBuilder();
        int at = 0;
        int plain = 0;
        while (m.find()) {
            int want = m.group(1) != null
                    ? offset + Integer.parseInt(m.group(1))
                    : offset + ++plain;
            if (want > 9) {
                return null;
            }
            out.append(text, at, m.start()).append("{~").append(want).append('}');
            at = m.end();
        }
        return at == 0 ? text : out.append(text.substring(at)).toString();
    }

    /**
     * 把一欄的顏色佔位符往後推 {@code offset} 個。
     *
     * <h2>為什麼要推</h2>
     * {@code {cN}} 指的是「這一行<b>第 N 個</b>出現的顏色」，所以編號跟欄位在
     * 哪一欄有關：同一個「紫色信標」在左欄是 {@code {c1}}、在右欄是 {@code {c2}}。
     * 語料裡因此同一欄存了兩種寫法——那正是先前只能一對一對硬列的原因之一。
     *
     * <p>單欄的條目沒辦法知道自己會被放到第幾欄，所以一律寫 {@code {c1}}，
     * 接的時候再按前面幾欄用掉的顏色數往後推。
     *
     * <h2>為什麼是逐欄推，不是接好之後整串重編</h2>
     * 整串重編分不出「同一欄裡重複用 {@code {c1}}」（該保持同色）與「兩欄各自
     * 寫 {@code {c1}}」（該是不同色）——兩者在接好的字串裡長得一模一樣。
     * 逐欄推就沒有這個歧義：欄內的相對關係原封不動，欄與欄之間才錯開。
     */
    static String shiftColours(String text, int offset) {
        if (offset == 0) {
            return text;
        }
        java.util.regex.Matcher m = COLOUR_TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (m.find()) {
            out.append(text, at, m.start())
               .append("{c").append(Integer.parseInt(m.group(1)) + offset).append('}');
            at = m.end();
        }
        return at == 0 ? text : out.append(text.substring(at)).toString();
    }

    /** 這一段用到幾種不同的顏色。見 {@link #shiftColours}。 */
    static int distinctColours(String text) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.regex.Matcher m = COLOUR_TOKEN.matcher(text);
        while (m.find()) {
            seen.add(m.group(1));
        }
        return seen.size();
    }

    private static final java.util.regex.Pattern COLOUR_TOKEN =
            java.util.regex.Pattern.compile("\\{c(\\d+)}");

    /**
     * 以排版符號為界把一行切成幾欄。
     *
     * <p>每一欄都<b>帶著自己前面那個 {@code {#}}</b>——語料裡的單欄條目就是那個
     * 形狀（{@code {#}Reward Pulls}），不帶的話查不到。
     *
     * <p>只切 {@code {#}}。{@code {~}} 數值、{@code {p}} 地名是欄位<b>內部</b>的
     * 東西，切開就湊不回去了。
     */
    private static List<String> splitColumns(String template) {
        String mark = GlyphSplitter.GLYPH_PLACEHOLDER;
        List<String> out = new ArrayList<>();
        int at = template.indexOf(mark);
        if (at > 0) {
            out.add(template.substring(0, at));   // 第一個符號之前也算一欄
        }
        while (at >= 0) {
            int next = template.indexOf(mark, at + mark.length());
            out.add(next < 0 ? template.substring(at) : template.substring(at, next));
            at = next;
        }
        return out;
    }

    /**
     * 漂浮名牌的譯文。只認<b>整塊</b>的鍵，而且完全不碰排版。
     *
     * <h2>為什麼名牌不能走一般那條路</h2>
     * 一般的 {@link #translate} 查不到整行時會退到 {@link #translateSegments}——
     * 逐片段替換，然後跑 {@link #alignColumns}、{@link #recenterColumns}、
     * 前導置中補償。那一整套是為 <b>tooltip</b> 寫的：tooltip 是「標籤 + 數值」
     * 兩欄的表格，欄要對齊。
     *
     * <p>漂浮名牌不是表格。它浮在 3D 世界裡，位置由遊戲整塊算好，
     * 裡面的偏移是 Wynncraft 自己排的圖示與間隔。把欄位對齊那一套套上去，
     * 等於<b>拿尺去量一張沒有欄的紙</b>——空白被重新編碼、前導被補上半個寬度差，
     * 整塊就歪了。而且只要有<b>任何一個</b>片段查得到（哪怕是句子裡的一個詞），
     * 這條路就會回傳非 null，於是連「幾乎沒翻到」的名牌也被重排一次。
     *
     * <p>使用者回報的「翻譯讓原始 Wynncraft UI 錯位」正是這個：職業選擇的
     * 三個全英文標籤互相疊在一起、蓋住圖示——那些字我們根本沒翻，
     * 卻被排版邏輯動過。
     *
     * <h2>所以這裡怎麼做</h2>
     * <ul>
     *   <li>只查<b>整塊</b>的鍵。查不到就回傳 {@code null}，原文原封不動。</li>
     *   <li><b>不重新對齊。</b>符號與偏移由 {@link #rebuild} 原樣填回，
     *       Wynncraft 怎麼排就怎麼排。</li>
     * </ul>
     *
     * <p>代價是有些名牌會從「翻到一半」變成「完全沒翻」。這符合這個專案
     * 一開始就寫下的規則：寧可不翻，也不要畫出錯位的東西——何況錯位的是
     * <b>遊戲原本的畫面</b>，那比我們自己的面板嚴重得多。
     */
    public static Component translateLabel(StyledText label, TranslationStore store) {
        LineParts parts = LineParts.of(label);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        String translated = lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            translated = labelByLine(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuild(translated, parts, store);
        return rebuilt == null ? null : unslant(rebuilt);
    }

    /**
     * 整塊查不到時的退路：<b>逐行</b>查，純符號的那幾行原樣留著。
     *
     * <h2>為什麼需要</h2>
     * 怪物名牌是「名字 + 血條」一行、「狀態圖示」一行：
     * <pre>
     *   Frosted Guard {#}{#}
     *   {#} {#} {#}
     * </pre>
     * 第二行那排圖示會隨著身上的狀態增減——給牠一個緩速就多一個圖示，
     * 整塊的鍵跟著變，於是<b>名字瞬間跳回英文</b>。實機回報的就是這個。
     *
     * <p>要為每種狀態組合各建一個鍵是不可能的：那是排列組合。能建的只有
     * 「名字那一行」一個鍵，其餘的行原樣放行。
     *
     * <h2>為什麼這樣不會弄壞版面</h2>
     * {@link #translateLabel} 之所以只認整塊，是怕退到逐片段替換之後跑
     * tooltip 那套欄位對齊，把遊戲自己排好的漂浮標籤弄歪。
     *
     * <p>這裡不走那條路：純符號的行是<b>逐字元原樣</b>抄回去的，
     * 有字的行則是整行查表——兩種都不會經過重新對齊。
     *
     * <p>而且要求<b>每一個有字的行都查得到</b>，有一行查不到就整塊放棄。
     * 半中半英的名牌比全英文的更難看，也更難查是哪裡出的問題。
     */
    /**
     * 這一行有沒有<b>字</b>——連續兩個以上的字母。
     *
     * <h2>為什麼不能用「有沒有字母」</h2>
     * 名牌的狀態列長這樣：{@code ⬤ 12s}、{@code {#} 3 ❄ 5 ⬤ 1s}——那個 {@code s}
     * 是秒的單位，不是字。用「有沒有字母」判斷會把整列當成需要翻譯的文字，
     * 查不到就整塊放棄，於是<b>NPC 一中緩速、名字就跳回英文</b>。實機回報的正是這個。
     *
     * <p>要求連續兩個字母，單位字母（s、m、k）就落在外面，而真正的字
     * （{@code Dwarven Trader}）一定進得來。
     */
    private static boolean hasWord(String text) {
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c) && !isHan(c)) {
                if (++run >= 2) {
                    return true;
                }
            } else {
                run = 0;
            }
        }
        return false;
    }

    private static String labelByLine(String template, TranslationStore store) {
        if (template.indexOf(NEWLINE) < 0) {
            return null;                       // 單行的話整塊就是那一行，沒有退路可言
        }
        String[] rows = template.split("\n", -1);
        StringBuilder out = new StringBuilder();
        boolean any = false;
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                out.append(NEWLINE);
            }
            String row = rows[i];
            if (!hasWord(row)) {
                out.append(row);               // 沒有字的行：一個位元都不動
                continue;
            }
            String hit = lookup(row, store);
            if (hit == null || hit.isBlank()) {
                return null;                   // 有字卻查不到，整塊放棄
            }
            out.append(hit);
            any = true;
        }
        return any ? out.toString() : null;
    }

    /**
     * 一塊聊天訊息裡哪幾行是置中的。整塊逐行查表時，呼叫端先算好再一行一行傳進來。
     */
    public static boolean[] chatCentred(List<StyledText> rows) {
        List<Component> lines = new ArrayList<>(rows.size());
        for (StyledText row : rows) {
            lines.add(row.getComponent());
        }
        return BlockLayout.centered(lines);
    }

    private static Component realignChat(StyledText original, Component rebuilt,
                                         Boolean centred) {
        List<List<Run>> origRows = splitRows(runs(original.getComponent()));
        List<List<Run>> madeRows = splitRows(runs(rebuilt));
        int[] keepOrig = solidRows(origRows);
        int[] keepMade = solidRows(madeRows);
        if (keepOrig[1] - keepOrig[0] != keepMade[1] - keepMade[0]) {
            FlowedDebug.chatRows(original.getString(),
                    "  去掉首尾空行之後：原文 " + (keepOrig[1] - keepOrig[0])
                    + " 行、譯文 " + (keepMade[1] - keepMade[0]) + " 行",
                    "行數對不上，原樣返回不動排版");
            return rebuilt;                    // 行數對不上就什麼都別動
        }
        boolean[] centre = centred == null ? centredRows(origRows) : null;
        // 這一塊是不是「兩欄併排的面板」。見 #columnPanel。
        boolean panel = columnPanel(origRows);
        StringBuilder log = new StringBuilder();
        MutableComponent out = Component.empty();
        for (int i = 0; i < madeRows.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(NL));
            }
            List<Run> made = madeRows.get(i);
            if (i < keepMade[0] || i >= keepMade[1]) {
                out.append(apply(made, new int[countSpaces(made)]));
                continue;                      // 首尾的空行，原樣
            }
            int at = keepOrig[0] + i - keepMade[0];
            out.append(chatRow(origRows.get(at), made,
                               centred == null ? centre[at] : centred, panel, log));
        }
        FlowedDebug.chatRows(original.getString(), log.toString(), null);
        return out;
    }

    /** 拆好的原文各行，交給 {@link BlockLayout} 判斷置中。 */
    private static boolean[] centredRows(List<List<Run>> rows) {
        List<Component> lines = new ArrayList<>(rows.size());
        for (List<Run> row : rows) {
            lines.add(apply(row, new int[countSpaces(row)]));
        }
        return BlockLayout.centered(lines);
    }

    /**
     * 把一行譯文挪到原文那一行該在的位置。
     *
     * <ul>
     *   <li><b>置中</b>：中文比較短，縮排補上寬度差的一半，中線就對回去了</li>
     *   <li><b>靠左</b>：縮排直接對齊原文那一行的左緣</li>
     * </ul>
     *
     * <p>補的是行首多加一個偏移字元，不去改譯者自己寫的空白——差多少補多少，
     * 負的也補得出來（見 {@link SpaceOffset#encode}）。
     */
    /**
     * 這一塊是<b>兩欄併排的面板</b>嗎——只要有一行是兩欄就算。
     *
     * <h2>為什麼要問這個</h2>
     * 獵殺信標的面板裡混著兩種行：兩欄的（「白色信標｜黃色信標」）與單欄的
     * （「本次 Lootrun」「點擊此處重抽」）。兩欄的走逐欄置中，看起來是對的；
     * 單欄的照抄原文的左緣，而原文其實是<b>置中</b>的——中文短了，就往左偏。
     *
     * <p>本來該由 {@link BlockLayout#centered} 判斷置中，但它是拿<b>整塊</b>最寬
     * 的一行當基準的，而聊天塊常常混進不相干的訊息：實機診斷檔裡，信標面板的行
     * 都是 200～290px，卻跟一則 1439px 的歡迎訊息攢在同一塊，基準整個被拉走，
     * 單欄的行於是全被判成靠左。
     *
     * <p>但「這一塊裡有兩欄的行」本身就是很強的訊號：那是一個排版過的面板，
     * 裡面的單欄行也是照面板置中的。不必知道整塊多寬也判斷得出來。
     */
    private static boolean columnPanel(List<List<Run>> rows) {
        for (List<Run> row : rows) {
            if (columns(segmentWidths(row)) >= 2) {
                return true;
            }
        }
        return false;
    }

    private static Component chatRow(List<Run> orig, List<Run> made,
                                     boolean centred, boolean panel,
                                     StringBuilder log) {
        int leadOrig = leadWidth(orig);
        int leadMade = leadWidth(made);
        int bodyOrig = rowWidth(orig) - leadOrig;
        int bodyMade = rowWidth(made) - leadMade;
        // 從第 0 欄開始的那一行不可能是置中的，不管判斷怎麼說。
        //
        // 沒有這一道，單獨一則沒有縮排的訊息（「[You are now entering Ragni]」）
        // 會被補上半個寬度差，整行往右跑——那是<b>新加</b>的歪法，
        // 舊路在沒有前導空白時本來就什麼都不做。
        // 多欄的行不整行置中——那是 columnDrift 在做的，而且是<b>每欄各自</b>
        // 置中。兩邊都做的話整行會位移兩次。
        // 面板裡的單欄行也照原文的中心擺——原文是置中的，照抄左緣會往左偏。
        // 見 #columnPanel。
        boolean single = columns(segmentWidths(orig)) < 2;
        boolean centre = single && leadOrig > 0 && (centred || panel);
        int target = centre ? leadOrig + (bodyOrig - bodyMade) / 2 : leadOrig;
        int pad = target - leadMade;
        log.append("  ").append(centre ? "置中" : "靠左")
           .append(" 原文縮排=").append(leadOrig).append(" 內容=").append(bodyOrig)
           .append("  譯文縮排=").append(leadMade).append(" 內容=").append(bodyMade)
           .append("  補=").append(pad)
           .append("  譯文=").append(rowText(made))
           .append(System.lineSeparator());
        MutableComponent row = Component.empty();
        String encoded = SpaceOffset.encode(pad);
        if (!encoded.isEmpty()) {
            row.append(literal(encoded, SpaceOffset.styleFor(Style.EMPTY)));
        }
        int[] columns = columnPad(orig, made);
        for (int px : columns) {
            if (px != 0) {
                log.append("        欄距補正=")
                   .append(java.util.Arrays.toString(columns))
                   .append(System.lineSeparator());
                break;
            }
        }
        row.append(apply(made, columns));
        return row;
    }

    /**
     * 欄與欄之間的間隔，要跟著譯文的寬度走。
     *
     * <h2>畫面上是什麼樣子</h2>
     * 獵殺信標的選單是兩欄併排的，原文一行長這樣：
     *
     * <pre>
     *   [縮排 33px]Orange Beacon[間隔 69px]Yellow Beacon
     * </pre>
     *
     * 那個 69px 是伺服器照<b>英文</b>的寬度算好的，讓右欄落在該落的位置。
     * 譯文把 {@code Orange Beacon}（90px）換成「橘色信標」（40px）之後，間隔卻
     * 原樣搬過來——右欄的起點往左跑了 50px，整欄疊到左欄的說明文字上面。
     *
     * <p>{@link #chatRow} 本來只調行首的縮排，管的是<b>整行</b>的左緣；行<b>內</b>
     * 的欄距沒有人管。這裡補上：每個間隔各自吸收它左邊那一段縮水了多少，
     * 欄的起點就回到原文的位置。和 {@link #alignColumns} 是同一套邏輯，只是那邊
     * 走的是 tooltip 的逐片段路徑，聊天訊息走不到。
     *
     * <p>第一個間隔不動。它是行首的縮排，{@link #chatRow} 的 {@code pad} 已經在
     * 管了——兩邊都補的話整行會位移兩次。
     *
     * <p>負的間隔也不動：那是疊字用的，理由見 {@link #isBacktrack}。它左邊累積的
     * 差額留給後面第一個正的間隔去吸收。
     *
     * <p>間隔數量對不上就整行放棄。對不上代表譯文的排版結構跟原文不同，
     * 這時候「第幾個間隔」配不起來，硬補只會補到別的地方去。
     */
    private static int[] columnPad(List<Run> orig, List<Run> made) {
        int spaces = countSpaces(made);
        if (countSpaces(orig) != spaces) {
            return new int[spaces];
        }
        int[] px = new int[spaces];
        int index = 0;
        for (Run r : made) {
            if (r.space()) {
                px[index++] = r.px();
            }
        }
        return columnDrift(segmentWidths(orig), segmentWidths(made), px);
    }

    /**
     * {@link #columnPad} 的算術部分。
     *
     * <p>分出來是為了測得到。寬度要有字型才量得出來，headless 的
     * {@link #widthOf} 一律回 0，所以吃 {@code Run} 的那一層在測試裡永遠算出
     * 全零——真正會出錯的加減法反而一行都沒被蓋到。這一層吃的是量好的數字。
     *
     * @param from     原文每一欄的文字寬度，長度是「間隔數 + 1」
     * @param to       譯文每一欄的文字寬度，長度同上
     * @param spacePx  譯文每一個間隔現在的寬度
     */
    static int[] columnDrift(List<Integer> from, List<Integer> to, int[] spacePx) {
        int[] adjust = new int[spacePx.length];
        if (columns(from) >= 2) {
            return centreColumns(from, to, spacePx);
        }
        int drift = 0;
        for (int i = 1; i < spacePx.length; i++) {
            drift += to.get(i) - from.get(i);
            if (spacePx[i] >= 0) {
                adjust[i] = -drift;
                drift = 0;
            }
        }
        return adjust;
    }

    /** 這一行有幾個真的有內容的欄。 */
    private static int columns(List<Integer> widths) {
        int n = 0;
        for (int w : widths) {
            if (w > 0) {
                n++;
            }
        }
        return n;
    }

    /**
     * 多欄的行：每一欄各自<b>置中</b>在原文那一欄的位置上。
     *
     * <h2>畫面上是什麼樣子</h2>
     * 獵殺信標的選單是兩欄併排，而且每一欄自己是置中的：
     *
     * <pre>
     *     Purple Beacon              Blue Beacon
     *    +2 Curse, +2 End         Choose a Boon at
     *      Reward Pulls              100% Potency
     * </pre>
     *
     * 伺服器是靠<b>每行不同的縮排</b>做到的（實測是 34、34、45 px）。我們照抄
     * 那個縮排、字卻變窄了，於是每一欄都往左偏，而且偏的量各行不同——畫面上
     * 就是三行參差不齊。
     *
     * <h2>規則</h2>
     * 令 {@code d[j]} 是第 j 欄縮水了多少（原文寬 - 譯文寬）。要讓每一欄的
     * <b>中心</b>都留在原處，第 k 個間隔要補的量是
     *
     * <pre>
     *   adjust[k] = (d[k] + d[k+1]) / 2
     * </pre>
     *
     * 也就是<b>各吸收左右兩欄縮水的一半</b>。推導：第 j 欄的起點要往右移
     * {@code d[j]/2}，把前面所有間隔的變化累加起來相減就得到上式。
     *
     * <p>只有兩欄以上才這樣做。單欄的行沒有「欄」可言，照舊靠左——那種行
     * 整行置中與否是 {@link #chatRow} 在管的。
     */
    private static int[] centreColumns(List<Integer> from, List<Integer> to, int[] spacePx) {
        int[] adjust = new int[spacePx.length];
        for (int k = 0; k < spacePx.length; k++) {
            if (spacePx[k] < 0) {
                continue;                  // 疊字用的負偏移不動，見 isBacktrack
            }
            int before = from.get(k) - to.get(k);
            int after = k + 1 < from.size() ? from.get(k + 1) - to.get(k + 1) : 0;
            adjust[k] = (before + after) / 2;
        }
        return adjust;
    }

    /**
     * 這一行開頭的縮排有多寬。
     *
     * <p>排版偏移與<b>真的空白字元</b>都算。只認偏移的話，「[Cave Completed]」
     * 那種整塊用空白排版的訊息會被當成完全沒有縮排。
     *
     * <p>空白可能跟文字黏在同一段裡（{@code "        Grook's Nest"}），
     * 所以要看到段<b>裡面</b>去，碰到第一個實字就停。
     */
    static int leadWidth(List<Run> row) {
        int px = 0;
        for (Run run : row) {
            if (run.space()) {
                px += run.px();
                continue;
            }
            String text = run.text();
            int n = 0;
            while (n < text.length() && Character.isWhitespace(text.charAt(n))) {
                n++;
            }
            px += widthOf(literal(text.substring(0, n), run.style()));
            if (n < text.length()) {
                return px;                     // 碰到實字就停
            }
        }
        return px;
    }

    /** 這一行連縮排在內總共多寬。 */
    private static int rowWidth(List<Run> row) {
        int px = 0;
        for (Run run : row) {
            px += run.space() ? run.px() : widthOf(literal(run.text(), run.style()));
        }
        return px;
    }

    /** 一行裡的實字，診斷用。排版空白不進去，不然滿眼都是看不懂的碼位。 */
    private static String rowText(List<Run> runs) {
        StringBuilder sb = new StringBuilder();
        for (Run r : runs) {
            if (!r.space()) {
                sb.append(r.text());
            }
        }
        return sb.toString();
    }

    /** 拆成一行一組。換行本身不進任何一組。 */
    private static List<List<Run>> splitRows(List<Run> runs) {
        List<List<Run>> out = new ArrayList<>();
        List<Run> row = new ArrayList<>();
        for (Run r : runs) {
            if (r.space() || r.text().indexOf('\n') < 0) {
                row.add(r);
                continue;
            }
            String[] parts = r.text().split(NL, -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    out.add(row);
                    row = new ArrayList<>();
                }
                if (!parts[i].isEmpty()) {
                    row.add(new Run(false, 0, r.style(), parts[i]));
                }
            }
        }
        out.add(row);
        return out;
    }

    private static int rows(List<Run> runs) {
        return splitRows(runs).size();
    }

    record Run(boolean space, int px, Style style, String text) {}

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
        return isAdjustableSpace(style, text, widthOf(literal(text, style)));
    }

    /**
     * 見上。這一支多收一個「量出來的寬度」，是為了讓判斷本身測得到——
     * 量寬度需要真的字型，headless 測不了，但<b>判斷</b>才是出錯的那一步。
     *
     * <h2>為什麼不能只認 space 字型</h2>
     * 先前的條件是「字型必須是 {@code minecraft:space}」。但素材與坐騎的 tooltip
     * 走的是 {@code minecraft:language/wynncraft}，而<b>那個字型也收了偏移碼位</b>——
     * 同樣是寬度偏移，只因為掛在別的字型底下就被當成一般文字：
     *
     * <pre>
     *   󏿿󐀁󐀂Defence󏿒󐁤+1 to +2
     *   [1] 「防禦」 &lt;- 「Defence」  寬 41 -&gt; 18   ← 標籤縮了 23px
     *   [2] 「󏿒󐁤」                  寬 54 -&gt; 54   ← 間隔沒跟著調整
     * </pre>
     *
     * <p>數值就整排往左跑了 23px。坐騎的「右鍵點擊召喚」原本靠前導偏移置中，
     * 也是同一件事——偏移沒被認出來，譯文變短之後整行就偏左了。
     *
     * <h2>放寬之後怎麼確定沒認錯</h2>
     * 量出來的寬度<b>剛好等於</b>解碼出來的偏移值，就證明它在那個字型底下確實是
     * 寬度偏移，換成 space 字型重新編碼不會改變版面。材質包若在同一段碼位畫了
     * 圖示，寬度對不上，這裡就不會誤判。
     *
     * @param measured 這一段在它自己的字型底下量出來的寬度
     */
    static boolean isAdjustableSpace(Style style, String text, int measured) {
        if (!SpaceOffset.isOffsetRun(text)) {
            return false;
        }
        return SpaceOffset.isSpaceFont(style) || measured == SpaceOffset.decode(text);
    }

    /**
     * 這個間隔前面有沒有<b>真正的標籤</b>——含字母或數字的文字。
     *
     * <h2>為什麼要問這個</h2>
     * 右緣補償只在「標籤 + 靠右的數值」這種行才成立。角色資訊的詞條列長這樣：
     *
     * <pre>
     *   – ✤ Strength: -70
     *       ↑ 這個間隔前面只有破折號跟屬性圖示，沒有標籤
     * </pre>
     *
     * 那不是欄位交界，是圖示與內文之間的排版。把數值縮水的差額補進去，等於
     * <b>把標籤往右推</b>——而且每一行推的量不同（每個屬性名縮水的幅度不一樣），
     * 於是整排參差不齊。實機回報的「角色資訊跑版」就是這個。
     *
     * <p>{@link #alignColumns}（逐片段那條路）早就有這一道（{@link #labelled}）；
     * 我在 {@link #realign} 這邊誤用了<b>寬度</b>判斷——圖示的寬度也大於零，
     * 所以條件永遠成立。
     *
     * <p>方塊字算字母，所以已經翻成中文的標籤照樣認得出來。
     */
    /**
     * 這個間隔是不是<b>圖示疊字</b>，而不是欄位交界。
     *
     * <h2>畫面上是什麼樣子</h2>
     * 玩家名牌右邊的等級膠囊是<b>疊出來</b>的：先畫一整顆膠囊底圖，再用一個
     * 負偏移把游標拉回膠囊左端，然後把「LV 95」的圖示字畫在底圖上面。
     *
     * <pre>
     *   font=banner/pill  text=<U+E060>…<U+E062><U+CFFE2>   ← 膠囊底圖 + 往回 30px
     *   font=banner/pill  text=<U+E00B><U+E015> <U+E029><U+E025><U+D0002>   ← 疊在上面的「LV 95」
     * </pre>
     *
     * <p>{@link #realign} 看到的卻是「兩段都有寬度的間隔」，也就是它認定的欄位
     * 交界。「Kandon-Beda Recruit」翻成「Kandon-Beda 新兵」窄了 18px，補償就把
     * 這個 -30 改成 -12——等級的字整個從膠囊上滑出去，畫面上變成膠囊跟數字
     * 分開的兩塊。實機截圖回報的就是這個。
     *
     * <h2>怎麼分辨</h2>
     * 兩個條件同時成立才算疊字：間隔<b>是負的</b>（把游標往回拉，才有東西可以
     * 疊上去），而且它<b>後面那一段沒有實字</b>，只有造字區的圖示碼位。
     *
     * <p>欄位交界不會兩者兼具——真正的欄位間隔是把右欄往<b>後</b>推，而右欄
     * 是要讀的文字（{@code Mage/Dark Wizard}、{@code +1 to +2}）。所以這個判斷
     * 不會把該補的欄位擋掉。
     */
    static boolean overlayGap(List<Run> runs, int gap, int px) {
        return px < 0 && !textAfterGap(runs, gap);
    }

    /** 第 {@code gap} 個間隔<b>後面</b>那一段有沒有實字。造字區的圖示不算。 */
    static boolean textAfterGap(List<Run> runs, int gap) {
        int seen = 0;
        boolean after = false;
        for (Run r : runs) {
            if (r.space()) {
                if (after) {
                    return false;          // 走到下一個間隔了，這一段沒有實字
                }
                if (seen++ == gap) {
                    after = true;
                }
                continue;
            }
            if (!after) {
                continue;
            }
            String text = r.text();
            for (int i = 0; text != null && i < text.length(); i++) {
                if (Character.isLetterOrDigit(text.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 每個間隔的偏移量，順序跟 {@link #countSpaces} 數出來的一樣。 */
    private static int[] gapPixels(List<Run> runs) {
        int[] out = new int[countSpaces(runs)];
        int n = 0;
        for (Run r : runs) {
            if (r.space()) {
                out[n++] = r.px();
            }
        }
        return out;
    }

    static boolean labelledRun(List<Run> runs, int gap) {
        int seen = 0;
        for (Run r : runs) {
            if (r.space()) {
                if (seen++ == gap) {
                    return false;              // 走到這個間隔了，前面沒有標籤
                }
                continue;
            }
            String text = r.text();
            for (int i = 0; text != null && i < text.length(); i++) {
                if (Character.isLetterOrDigit(text.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 把「文字尾端黏著欄位間隔」的片段拆成兩段。
     *
     * <h2>畫面上是什麼樣子</h2>
     * 物品的需求列在遊戲裡是一整段：
     *
     * <pre>
     *   font=language/wynncraft  text=␠Class␠Type<U+CFFC4><U+D004C>
     *   font=language/wynncraft  text=Mage/Dark␠Wizard
     * </pre>
     *
     * 尾端那兩個字元是把右欄推到右緣用的<b>欄位間隔</b>，只是沒有獨立成一個片段。
     *
     * <p>{@link #realign} 的第一道關卡是「原文與譯文的間隔數量要一樣」，
     * 而 {@link #isAdjustableSpace} 是對<b>整個片段</b>判斷的——
     * {@code ␠Class␠Type󏿄󐁌} 整段不是純偏移，所以原文算出 0 個間隔；
     * 重建之後偏移被拆成自己一段，譯文算出 1 個。數量對不上，整行原樣返回，
     * <b>補償完全沒有跑</b>。
     *
     * <p>於是「職業類型」比 {@code Class Type} 窄了十幾像素，右欄就跟著往左跑；
     * 而同一份 tooltip 裡走逐片段那條路的行有補償、走整行查表的沒有——
     * 一份物品說明裡兩種對齊方式並存，實機回報的就是這個。
     *
     * <h2>為什麼拆了就安全</h2>
     * 原文與譯文<b>用同一支函式</b>拆，數量自然還是對得上；拆出來的兩段文字與
     * 樣式都沒變，{@link #apply} 重新組回去跟原本是同一件東西。
     *
     * <p>只認尾端，而且要求那一段<b>單獨拿去量的寬度剛好等於解碼出來的偏移值</b>
     * （{@link #isAdjustableSpace}）。材質包若在同一段碼位畫了圖示，寬度對不上，
     * 這裡就不會誤拆。整段都是偏移的不用拆——那本來就已經是一個間隔了。
     */
    private static List<Run> splitGaps(List<Run> runs) {
        return splitGaps(runs, LineTranslator::isAdjustableSpace);
    }

    /**
     * 見上。這一支多收一個「這段算不算間隔」的判斷，是為了讓拆法本身測得到——
     * 判斷要量寬度，而量寬度需要真的字型，headless 測不了；但<b>拆或不拆</b>
     * 才是出錯的那一步。
     */
    static List<Run> splitGaps(List<Run> runs,
                               java.util.function.BiPredicate<Style, String> isGap) {
        List<Run> out = new ArrayList<>(runs.size() + 2);
        boolean split = false;
        for (Run r : runs) {
            String tail = r.space() ? "" : SpaceOffset.trailingOffsets(r.text());
            if (tail.isEmpty() || tail.length() == r.text().length()
                    || !isGap.test(r.style(), tail)) {
                out.add(r);
                continue;
            }
            String head = r.text().substring(0, r.text().length() - tail.length());
            out.add(new Run(false, 0, r.style(), head));
            out.add(new Run(true, SpaceOffset.decode(tail), r.style(), tail));
            split = true;
        }
        return split ? out : runs;
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
    static String lookup(String template, TranslationStore store, boolean percent) {
        String exact = withPercent(template, store, percent);
        if (exact != null) {
            // `store.lookup` 會把鍵 strip 過再查，命中時<b>原文首尾的空白就消失了</b>——
            // 「Ability Points: 1」於是變成「技能點數:1」，數字黏在冒號上。
            // 前面剝掉的原樣補回去，後面交給 reattach（全形標點自帶留白）。
            int head = 0;
            while (head < template.length()
                    && Character.isWhitespace(template.charAt(head))) {
                head++;
            }
            int tail = template.length();
            while (tail > head && Character.isWhitespace(template.charAt(tail - 1))) {
                tail--;
            }
            return template.substring(0, head) + exact
                    + reattach(exact, template.substring(tail));
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
        // 尾巴分兩段剝。<b>先只剝空白與圖示、把冒號留著</b>——技能樹的鍵本來
        // 就帶冒號（{@code "Damage:"}），冒號跟空白一起剝掉就永遠對不到那個鍵，
        // 會退回 ui-labels 的無冒號版，接回原文的半形「: 」。同一個面板裡
        // 於是「持續時間：」與「傷害: 」並存，兩種冒號、兩種間距。
        int keepColon = trimEnd(template, start, glyph, false);
        int end = trimEnd(template, start, glyph, true);
        if (start == 0 && end == template.length()) {
            return null;                       // 首尾沒有可剝的，不必重查
        }
        String hit = null;
        int used = keepColon;
        if (keepColon > end) {                 // 尾端真的有冒號可留
            String withColon = template.substring(start, keepColon);
            hit = withColon.isBlank() ? null : withPercent(withColon, store, percent);
        }
        if (hit == null) {
            used = end;
            String core = template.substring(start, end);
            if (core.isBlank()) {
                return null;
            }
            hit = withPercent(core, store, percent);
        }
        if (hit == null) {
            return null;
        }
        return template.substring(0, start) + hit + reattach(hit, template.substring(used));
    }

    /**
     * 從尾端往回剝，回報剝到哪裡。
     *
     * @param colons 連冒號一起剝。{@code false} 時只剝空白與圖示佔位符
     */
    private static int trimEnd(String template, int start, String glyph, boolean colons) {
        int end = template.length();
        while (end > start) {
            if (end >= start + glyph.length() && template.startsWith(glyph, end - glyph.length())) {
                end -= glyph.length();
            } else if (Character.isWhitespace(template.charAt(end - 1))
                    || (colons && isTrailingColon(template.charAt(end - 1)))) {
                end--;
            } else {
                break;
            }
        }
        return end;
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
        if (translated.isEmpty()) {
            return suffix;
        }
        char last = translated.charAt(translated.length() - 1);
        if (suffix.isBlank()) {
            return isFullWidthPunctuation(last) ? "" : suffix;
        }
        // 尾巴是半形冒號，而譯文自己已經帶了冒號——別再補第二個。
        if (isTrailingColon(suffix.charAt(0)) && suffix.substring(1).isBlank()
                && isTrailingColon(last)) {
            return suffix.substring(1);
        }
        return suffix;
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
            // 要先 strip 再接 %。模板尾端常常有一個空格，直接接會變成
            // 「Fire Damage: %」——那個鍵不存在，於是<b>靜默</b>退回非百分比的
            // 譯法，畫面上「+15%」的那一行被標成「火屬性傷害」。看起來只是
            // 翻得不夠好，其實是查錯鍵了。
            String hit = store.lookup(key.strip() + "%");
            if (hit != null) {
                return hit;
            }
        }
        String hit = store.lookup(key);
        if (hit != null) {
            return hit;
        }
        hit = withQuality(key, store, percent);
        return hit != null ? hit : withMaker(key, store);
    }

    /**
     * 「Crafted by {@code <玩家名>}」這種「固定開頭 + 一個名字」的行。
     *
     * <h2>為什麼名字不能收進語料</h2>
     * 那是<b>別的玩家</b>的名字。收進去等於把某個人的 ID 寫死在譯文檔裡，
     * 而且一個名字一條，永遠收不完。擷取那一關本來就會擋掉夾帶玩家名的行
     * （見 {@code PlayerDataFilter}），所以這種句子從來沒進過語料——
     * 玩家看到的「自製物品的『Crafted by』一直是英文」就是這個。
     *
     * <p>只收開頭，名字原樣接回去。開頭查不到就整行不翻，免得中英夾雜。
     */
    private static String withMaker(String key, TranslationStore store) {
        String core = key.strip();
        for (String prefix : NAME_PREFIXES) {
            if (!core.startsWith(prefix)) {
                continue;
            }
            String name = core.substring(prefix.length()).strip();
            if (!isName(name)) {
                continue;
            }
            String word = store.lookup(prefix.strip());
            if (word != null && !word.isBlank()) {
                return word + " " + name;
            }
        }
        return null;
    }

    /**
     * Minecraft 的 ID：英數與底線。
     *
     * <p>要先把佔位符拿掉再看。名字裡的<b>數字</b>在參數化那一關已經被收成
     * {@code {~}}——「3N0K1」進到這裡是「{~}N{~}K{~}」。只認英數的話，
     * 帶數字的 ID 一律被擋下，畫面上就是「有的人翻得出來、有的翻不出來」。
     * 佔位符原樣留著，後面 {@code fill} 會把真正的數字填回去。
     */
    static boolean isName(String text) {
        if (text.isEmpty() || text.length() > NAME_ROOM) {
            return false;
        }
        String bare = text.replaceAll("\\{[^}]*\\}", "");
        return bare.chars().allMatch(c ->
                (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9') || c == '_');
    }

    /** ID 最長 16 個字；收成佔位符之後會變長，所以放寬到這個數。 */
    private static final int NAME_ROOM = 40;

    /** 見 {@link #withMaker}。譯文放在 {@code misc.json}，翻譯團隊可以改。 */
    private static final String[] NAME_PREFIXES = {"Crafted by "};

    /**
     * 詞條全部滾到最高或最低時，遊戲會在名稱前面加一個品質詞。
     *
     * <h2>症狀</h2>
     * {@code Ephemeral Tome of Mysticism II} 在語料裡，但畫面上是
     * {@code Perfect Ephemeral Tome of Mysticism II}——多出來的那個字讓整個名稱
     * 查不到，看起來就是「這件物品的翻譯整個消失了」。100% 的物品才會遇到，
     * 所以平常翻不出來，一拿到滿滾的東西就中。
     *
     * <p>剝掉前綴查名稱，再把前綴的譯文接回去。兩邊都要查得到才算數——
     * 只查到一半就寧可整行不翻，免得中英混在一起。
     */
    private static String withQuality(String key, TranslationStore store, boolean percent) {
        String core = key.strip();
        for (String prefix : QUALITY_PREFIXES) {
            if (!core.startsWith(prefix)) {
                continue;
            }
            String name = withPercent(core.substring(prefix.length()), store, percent);
            String word = store.lookup(prefix.strip());
            if (name != null && word != null) {
                return word + name;
            }
        }
        return null;
    }

    /** 見 {@link #withQuality}。譯文放在 {@code ui-labels.json}，翻譯團隊可以改。 */
    private static final String[] QUALITY_PREFIXES = {"Perfect ", "Defective "};

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
        return rebuildAll(translated, parts, extraAccents, null, null, store);
    }

    /**
     * 「整行同色」的行，連同它的<b>譯文</b>一起收成重點段。
     *
     * <h2>為什麼需要</h2>
     * 上色是拿原文的<b>字面</b>到譯文裡找。一個詞查得到譯文
     * （見 {@link #withTranslations}）還好，但整句就不行了：
     * {@code Welcome to Wynncraft!} 不是語料的鍵（鍵是整段四行），
     * lookup、lookupTerm、lookupWordCore 三條路全落空。於是原文是金色粗體、
     * 譯文掉成一片灰色——一亮一暗擺在一起，一看就知道下面那行是外掛貼的。
     *
     * <p>但這種情況根本不需要查表——<b>位置就是答案</b>：
     * 原文第 i 行從頭到尾只有一個顏色，譯文第 i 行就是那個顏色。
     * 把「譯文那一行」當成一個重點段交給既有的貼樣式機制，不必另外開一條路。
     *
     * <h2>何時不做</h2>
     * 行數對不上就不做——中文比英文緊湊，兩行的句子常常一行就講完，
     * 此時「第 i 行」兩邊指的不是同一件事，比對下去只會上錯色。
     * 一行裡混了幾種顏色的也不做，那是 {@code accents} 本來就在管的事。
     */
    private static List<LineParts.Piece> wholeLineAccents(
            List<LineParts> parts, List<LineParts.Piece> allRuns,
            String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        String template = parts.size() == 1 ? parts.get(0).template() : null;
        List<RowStyle> source = template == null
                ? perPartStyles(parts) : uniformStyles(allRuns, template);
        String[] dst = String.join(NL, translated).split(NL, -1);
        if (source.size() != dst.length) {
            return List.of();
        }
        List<LineParts.Piece> out = new ArrayList<>();
        for (int i = 0; i < dst.length; i++) {
            String text = PLACEHOLDER.matcher(dst[i]).replaceAll("").strip();
            if (!hasContent(text)) {
                continue;
            }
            Style only = source.get(i).only();
            if (only == null) {
                only = fallback(source.get(i).dominant(), known, dst[i]);
            }
            if (only == null || java.util.Objects.equals(only, blockStyle)
                    || covered(known, text, only)) {
                continue;         // 見 #covered：重複登記只會讓貼樣式那一步挑錯
            }
            out.add(new LineParts.Piece(text, only));
        }
        return out;
    }

    /**
     * 混色的那一行退而求其次：整行套上<b>多數色</b>。
     *
     * <h2>為什麼要有這一步</h2>
     * 「{~} mounts have no food in their feeder」原文是兩種棕色交錯的：
     *
     * <pre>
     *   #8F663D 「mounts have」   #BC8F62 「no food」   #8F663D 「in their feeder」
     * </pre>
     *
     * 三段都是句子中間的片語，翻成中文之後<b>一段都對不上字面</b>
     * （診斷檔 majorid-debug 裡那三行「譯文裡找不到」）。混色的行不登記，
     * 於是整行掉回底色——原文一片棕、譯文一片灰，兩行擺在一起就穿幫了。
     *
     * <h2>為什麼可以放心貼</h2>
     * 只在這一行<b>一個重點段都貼不上</b>時才做。貼得上的話那些片語各自有
     * 自己的顏色，整行套一個色反而會把它們蓋掉——而且重點段是從
     * 位置 0 起算優先的（見 {@link #appendText}），整行那一條一定先被選中。
     *
     * @return 要套的顏色；不該套時回傳 {@code null}
     */
    private static Style fallback(Style dominant, List<LineParts.Piece> known, String row) {
        if (dominant == null) {
            return null;
        }
        for (LineParts.Piece piece : known) {
            if (!piece.text().isBlank() && row.contains(piece.text())) {
                return null;               // 這一行有貼得上的重點段，讓它們去貼
            }
        }
        return dominant;
    }

    /**
     * 一行原文的顏色狀況。
     *
     * @param only     整行只有這一個顏色；混了幾種就是 {@code null}
     * @param dominant 佔最多字的那個顏色。見 {@link #fallback}
     */
    private record RowStyle(Style only, Style dominant) {}

    private static final String NL = "\n";

    /** 譯文裡的 {@code {#}}、{@code {~}}、{@code {p}}、{@code {u}} 之類。 */
    private static final java.util.regex.Pattern PLACEHOLDER =
            java.util.regex.Pattern.compile("\\{[^}]*\\}");

    /**
     * 每一行原文的唯一樣式；那一行混了幾種就是 {@code null}。
     *
     * <h2>為什麼要靠模板數行</h2>
     * {@code LineParts.of} 只收「不是空白」的片段，所以純換行的那一段
     * <b>整個被丟掉了</b>——光看 {@code runs} 根本不知道行在哪裡斷。
     * 模板留著換行，就拿它當尺：一行一行量過去，同時把 {@code runs} 依序消耗掉。
     *
     * <p>量的是<b>非空白字元數</b>。模板裡的文字就是各片段的文字接起來的，
     * 只是中間多了 {@code {#}} 這類佔位符與換行；把佔位符去掉、只數實字，
     * 兩邊就對得起來。
     */
    private static List<RowStyle> uniformStyles(List<LineParts.Piece> runs, String template) {
        List<RowStyle> out = new ArrayList<>();
        int at = 0;                        // 走到第幾個 run
        int eaten = 0;                     // 那個 run 已經用掉幾個實字
        for (String line : template.split(NL, -1)) {
            int need = solidCount(PLACEHOLDER.matcher(line).replaceAll(""));
            Style only = null;
            boolean mixed = false;
            boolean seen = false;
            Tally tally = new Tally();
            while (need > 0 && at < runs.size()) {
                LineParts.Piece run = runs.get(at);
                int have = solidCount(run.text()) - eaten;
                if (have <= 0) {           // 這個 run 沒有實字，跳過
                    at++;
                    eaten = 0;
                    continue;
                }
                if (!seen) {
                    only = run.style();
                    seen = true;
                } else if (!java.util.Objects.equals(only, run.style())) {
                    mixed = true;
                }
                int take = Math.min(have, need);
                tally.add(run.style(), take);
                need -= take;
                if (take == have) {
                    at++;
                    eaten = 0;
                } else {
                    eaten += take;
                }
            }
            out.add(new RowStyle(mixed || !seen ? null : only, tally.top()));
        }
        return out;
    }

    /** 一行裡每個顏色各佔幾個實字。見 {@link #fallback}。 */
    private static final class Tally {
        private final java.util.Map<Style, Integer> counts = new java.util.LinkedHashMap<>();

        void add(Style style, int solid) {
            if (style != null && solid > 0) {
                counts.merge(style, solid, Integer::sum);
            }
        }

        /** 佔最多字的那個；平手時取先出現的（也就是行首那個）。 */
        Style top() {
            Style best = null;
            int most = 0;
            for (java.util.Map.Entry<Style, Integer> e : counts.entrySet()) {
                if (e.getValue() > most) {
                    most = e.getValue();
                    best = e.getKey();
                }
            }
            return best;
        }
    }

    /** 非空白、非圖示的字元數。 */
    private static int solidCount(String text) {
        return (int) text.codePoints().filter(cp -> !Character.isWhitespace(cp)
                && !com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)).count();
    }

    /** tooltip 那一路：本來就一行一個 {@link LineParts}，直接看每一份自己的片段。 */
    private static List<RowStyle> perPartStyles(List<LineParts> parts) {
        List<RowStyle> out = new ArrayList<>();
        for (LineParts part : parts) {
            Style only = null;
            boolean mixed = false;
            boolean seen = false;
            Tally tally = new Tally();
            for (LineParts.Piece run : part.runs()) {
                if (!hasContent(run.text())) {
                    continue;
                }
                tally.add(run.style(), solidCount(run.text()));
                if (!seen) {
                    only = run.style();
                    seen = true;
                } else if (!java.util.Objects.equals(only, run.style())) {
                    mixed = true;
                }
            }
            out.add(new RowStyle(mixed || !seen ? null : only, tally.top()));
        }
        return out;
    }

    /**
     * 這一行已經有人登記過了嗎。
     *
     * <h2>為什麼要擋</h2>
     * {@link #withTranslations} 已經把查得到譯文的詞都登記了一份。
     * 整行只有一個詞的時候（「建立角色」「素材袋」這種標題），
     * 這邊會再登記一次<b>一模一樣的東西</b>。
     *
     * <p>貼樣式那一步是「在譯文裡找重點段的字面」，每個重點段只能用一次；
     * 兩條一模一樣的只有第一條會被用到，第二條就成了
     * {@code ★在譯文裡卻沒貼上}——診斷檔裡那一排星號就是這樣來的。
     * 更麻煩的是多余的長串會跟真正該貼的重點段互投位置。
     */
    private static boolean covered(List<LineParts.Piece> known, String text, Style style) {
        for (LineParts.Piece piece : known) {
            if (piece.text().equals(text)
                    && java.util.Objects.equals(piece.style(), style)) {
                return true;
            }
        }
        return false;
    }

    /** 這一段有沒有真正的字（非空白、非圖示）。 */
    private static boolean hasContent(String text) {
        return text.codePoints().anyMatch(cp -> !Character.isWhitespace(cp)
                && !com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp));
    }

    /**
     * @param overrideGlyphs 不是 {@code null} 就<b>取代</b>逐行收來的符號池
     * @param overridePlaces 不是 {@code null} 就<b>取代</b>逐行認出來的地名池
     *                       （見 {@link #rejoin}：整段重組過的話，兩個池子都得跟著換，
     *                       否則譯文裡的佔位符數量對不上，整段會被放棄）
     */
    private static List<Component> rebuildAll(String[] translated, List<LineParts> parts,
                                              List<LineParts.Piece> extraAccents,
                                              List<LineParts.Piece> overrideGlyphs,
                                              List<LineParts.Piece> overridePlaces,
                                              TranslationStore store) {
        List<LineParts.Piece> glyphs = new ArrayList<>();
        List<LineParts.Piece> places = new ArrayList<>();
        List<LineParts.Piece> numbers = new ArrayList<>();
        List<LineParts.Piece> users = new ArrayList<>();
        List<LineParts.Piece> accents = new ArrayList<>(extraAccents);
        // 重點段要拿<b>整段</b>的主樣式重挑一次，不能沿用每一行各自挑好的。
        // 見 LineParts#accentsAgainst：一段被 tooltip 寬度切成好幾行之後，
        // 每一行的主樣式各自不同，某一行裡最長的那一段會在自己那行被當成
        // 「就是主樣式」而丟掉——畫面上就是 Mana Bank 的藍色不見了、
        // Major ID 的敘述整段套上了標題的顏色。
        Style blockStyle = dominantStyle(parts);
        List<LineParts.Piece> allRuns = new ArrayList<>();
        for (LineParts part : parts) {
            glyphs.addAll(part.glyphs());
            places.addAll(part.places());
            numbers.addAll(part.numbers());
            users.addAll(part.users());
            allRuns.addAll(part.runs());
        }
        if (overridePlaces != null) {
            // 整段重組出來的那一份是<b>依序</b>掃的，逐行那份看得到的它都看得到，
            // 還多了被斷行切開的那些。兩份混在一起會重複，直接換掉。
            places = new ArrayList<>(overridePlaces);
        }
        if (overrideGlyphs != null) {
            // 行首的排版偏移已經在重組時剝掉了，池子也要跟著少那幾個。
            glyphs = new ArrayList<>(overrideGlyphs);
        }
        accents.addAll(LineParts.accentsAgainst(allRuns, blockStyle));
        // 譯文接成一整串再傳：詞幹要不要登記得看它在譯文裡有沒有自成一個詞，
        // 而換行不是詞的一部分（見 #addStem）。
        accents = withTranslations(accents, String.join(String.valueOf(NEWLINE), translated), store);
        // 整行同色的那幾行，直接拿譯文那一行當重點段。見 #wholeLineAccents。
        accents.addAll(wholeLineAccents(parts, allRuns, translated, blockStyle, accents));

        // 斷行不要把一個重點詞切成兩半，否則它的顏色會整個掉。見 keepAccentsWhole。
        String[] flowed = keepAccentsWhole(translated, accents);

        List<List<Token>> lines = new ArrayList<>(flowed.length);
        long wantGlyphs = 0;
        long wantPlaces = 0;
        long wantNumbers = 0;
        long wantUsers = 0;
        int numbered = 0;
        for (String line : flowed) {
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

        Style textStyle = forDisplay(blockStyle);
        // 遊戲自己的符號留在<b>原本的字型</b>裡。
        //
        // 上面那一行把字型換成預設，中文才畫得出來。代價是 Wynncraft 自己那組
        // 字型裡的特製字形一起沒了——{@code ❤} 在它們的字型裡是紅色實心的心，
        // 換成預設字型就退回 Unicode 的通用字形，畫面上是白色空心的。
        //
        // 實機回報：戰鬥資訊面板「生命」那行的愛心是白的，而下面「有效生命」
        // 那行是紅的。差別在後者走的是逐片段替換，符號那一段根本沒被動過。
        // 同一份面板裡同一個符號兩種樣子，一眼就看得出來。
        //
        // 空白字型（純粹用來推像素的那種）不能留——拿它畫字會是一堆亂碼。
        //
        // 用<b>原文裡那個符號自己的樣式</b>，不是整段的主樣式。戰鬥資訊面板的
        // 「❤ Health: 12,958/14,175」裡，愛心是紅的、數字是白的，而主樣式取的是
        // 佔多數的那個——拿主樣式去畫愛心，畫出來就是白的。實機回報的正是這個。
        Style symbolStyle = leadingSymbolStyle(allRuns);
        if (symbolStyle == null) {
            symbolStyle = SpaceOffset.isSpaceFont(blockStyle) ? textStyle : blockStyle;
        }
        // 括號註解在原文裡是另一個顏色。它不參與底色統計（見 #isNote），
        // 所以要在這裡把顏色還回去，否則註解會變成正文的亮色。
        Style noteStyle = noteStyleOf(allRuns, blockStyle);
        // 註解常常被 tooltip 寬度切成兩行——左括號在這一行、右括號在下一行，
        // 所以這個狀態要跨行帶著走。
        boolean[] inNote = {false};
        boolean[] usedAccent = new boolean[accents.size()];
        int glyph = 0;
        int place = 0;
        int number = 0;
        int user = 0;

        // 譯者自己指定的顏色可以挑哪些。見 #colourToken。
        List<Style> palette = palette(allRuns);
        FlowedDebug.palette(palette, allRuns, flowed);

        // 譯者指定的顏色管到 {/} 或下一個 {cN} 為止，<b>可以跨行</b>。
        //
        // 這裡原本每一行都會收掉，理由是「忘了寫 {/} 只會影響那一行」。
        // 但翻譯團隊第一次用就踩到這個限制：一句話被 tooltip 切成兩行，
        // 在第一行開色段、想一路染到第二行，結果第二行整個掉回底色，
        // 而寫在第二行的 {/} 也變成空操作。跨行才是譯者預期的行為，
        // 也跟上面 inNote 的處理一致——註解同樣會被切成兩行。
        //
        // 「忘了寫 {/}」改由 validate.py 擋下：色段開到整段結尾還沒關就報錯。
        // 從「默默染錯」變成「合併前擋下來」，比靠行尾自動收掉可靠。
        Style forced = null;
        List<Component> out = new ArrayList<>(lines.size());
        for (List<Token> tokens : lines) {
            MutableComponent line = Component.empty();
            Style justFilled = null;          // 剛填回去的那個佔位符的樣式
            // 剛填回去的是不是<b>數值</b>。單位後綴（s、m、stx）要跟著數值
            // 走顏色，見 #appendHugging 的說明。
            boolean afterNumber = false;
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                switch (token.kind()) {
                    case COLOR -> forced = colourOf(token, palette, textStyle);
                    case GLYPH -> {
                        // 符號連同原樣式（含自訂字型）整段搬回，這是它顯示得出來的唯一方式
                        LineParts.Piece piece = glyphs.get(glyph++);
                        line.append(Component.literal(piece.text()).withStyle(piece.style()));
                        justFilled = piece.style();
                        afterNumber = false;
                    }
                    case PLACE -> {
                        // 地名是專有名詞，原樣填回，永遠不翻譯
                        LineParts.Piece piece = places.get(place++);
                        line.append(literal(piece.text(), forDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = false;
                    }
                    case NUMBER -> {
                        // 帶編號的直接指名要第幾個，沒編號的照順序取下一個
                        int at = token.index() > 0 ? token.index() - 1 : number++;
                        if (at < 0 || at >= numbers.size()) {
                            return null;
                        }
                        LineParts.Piece piece = numbers.get(at);
                        line.append(literal(piece.text(), forDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = true;
                    }
                    case USER -> {
                        // 玩家名字原樣填回，跟地名一樣是專有名詞
                        LineParts.Piece piece = users.get(user++);
                        line.append(literal(piece.text(), forDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = false;
                    }
                    case TEXT -> {
                        if (forced != null) {
                            // 譯者已經講明這一段要什麼顏色，就不要再猜了——
                            // 重點段比對、括號註解、底色統計全部讓開。
                            line.append(literal(token.text(), forced));
                        } else {
                            Style before = i > 0 ? justFilled : null;
                            Style after = i + 1 < tokens.size()
                                    ? peekStyle(tokens.get(i + 1), glyphs, places,
                                                numbers, users, glyph, place, number, user)
                                    : null;
                            appendHugging(line, token.text(), textStyle, symbolStyle,
                                          noteStyle, inNote, accents, usedAccent,
                                          store, before, after, afterNumber);
                        }
                        justFilled = null;
                        afterNumber = false;
                    }
                }
            }
            out.add(line);
        }
        // 哪幾個重點段真的貼上去了。沒有這一欄，「某個詞沒有顏色」就只能用猜的。
        List<String> texts = new ArrayList<>(accents.size());
        List<Style> styles = new ArrayList<>(accents.size());
        for (LineParts.Piece accent : accents) {
            texts.add(accent.text());
            styles.add(accent.style());
        }
        FlowedDebug.accents(texts, styles, usedAccent, flowed);
        // 填回去的符號是什麼——見 FlowedDebug#glyphs（聲明了為什麼要這一欄）
        FlowedDebug.glyphs(glyphs, flowed);
        return out;
    }

    /**
     * 下一個佔位符會填回什麼樣式——還沒填，先看一眼。
     *
     * <p>沒編號的佔位符是照順序取的，所以「下一個」就是各自計數器現在指到的那一個。
     */
    private static Style peekStyle(Token next,
                                   List<LineParts.Piece> glyphs, List<LineParts.Piece> places,
                                   List<LineParts.Piece> numbers, List<LineParts.Piece> users,
                                   int glyph, int place, int number, int user) {
        return switch (next.kind()) {
            case GLYPH -> at(glyphs, glyph);
            case PLACE -> at(places, place);
            case NUMBER -> at(numbers, next.index() > 0 ? next.index() - 1 : number);
            case USER -> at(users, user);
            case TEXT, COLOR -> null;      // 顏色佔位符不填東西，沒有樣式可看
        };
    }

    private static Style at(List<LineParts.Piece> pool, int index) {
        return index >= 0 && index < pool.size() ? pool.get(index).style() : null;
    }

    /**
     * 緊貼著佔位符的標點，跟著那個佔位符走。
     *
     * <h2>畫面上是什麼樣子</h2>
     * 專業那一行的原文是 {@code - Ⓔ Lv. 120 Scribing§8 [66.24%]}——中括號與百分比
     * <b>同屬一個暗灰色片段</b>。譯文裡數值是佔位符，會連同自己的樣式填回去，
     * 但 {@code [} 與 {@code ]} 是譯文自己的字，畫的是整行的主樣式（比較亮的灰）。
     * 於是括號比它包住的數字亮一階，跟原文對不起來。技能點數的 {@code 0/50}
     * 中間那條斜線也是同一回事。
     *
     * <p>規則很單純：一段文字<b>開頭</b>緊貼著前一個佔位符的標點，用前一個佔位符
     * 的樣式；<b>結尾</b>緊貼著下一個佔位符的標點，用下一個的。中間那段照舊。
     * 「緊貼」是字面意思——中間有空白就不算，那是詞距不是黏著。
     *
     * <p>圓括號不算在內：它們是註解的界線（見 {@link #appendNoting}），
     * 交給註解那一套處理，這裡動它只會把註解的顏色弄亂。
     */
    /**
     * 整段都是標點時，那段標點該跟<b>後面</b>的佔位符走嗎。
     *
     * <p>遊戲送 {@code ✦ Available Points: 0/50} 是三段：
     * {@code 「✦ Available Points: 」「0」「/50」}——斜線跟它<b>右邊</b>的數字
     * 同屬一段。跟左邊走的話，譯文那條斜線會拿到前一個數字的樣式，
     * 畫面上就是它跟原文顏色不同。
     *
     * <p>只有「整段都是標點」才換邊。{@code 抄寫 [} 這種前面還有文字的，
     * 開頭那截仍歸前一段——那才是它視覺上依附的地方。
     */
    static boolean leadTakesNext(String text, int lead, Style after) {
        return after != null && lead == text.length();
    }

    private static void appendHugging(MutableComponent out, String text,
                                      Style base, Style symbol, Style note, boolean[] depth,
                                      List<LineParts.Piece> accents, boolean[] used,
                                      TranslationStore store, Style before, Style after,
                                      boolean afterNumber) {
        int lead = 0;
        if (before != null) {
            while (lead < text.length() && hugs(text.charAt(lead))) {
                lead++;
            }
        }
        int tail = text.length();
        if (after != null) {
            while (tail > lead && hugs(text.charAt(tail - 1))) {
                tail--;
            }
        }
        if (lead > 0) {
            // 整段都是標點、而且兩邊都有佔位符時，跟<b>後面</b>那個走。
            //
            // 遊戲送 `✦ Available Points: 0/50` 是三段：
            //   「✦ Available Points: 」「0」「/50」
            // 也就是斜線跟它<b>右邊</b>的數字同屬一段。跟左邊走的話，
            // 譯文那條斜線會拿到前一個數字的樣式——畫面上就是它跟原文顏色不同。
            Style owner = leadTakesNext(text, lead, after) ? after : before;
            out.append(literal(text.substring(0, lead), forDisplay(owner)));
        }
        // 單位後綴跟著前面那個數值走顏色。
        //
        // 畫面上是什麼樣子：生命竊取那一列原文寫 `+445/3s`，整串都是綠的——
        // 遊戲把數字與單位放在同一段。譯文的 `s` 是<b>文字</b>，拿到的是整行的
        // 正文顏色（標籤那個色），於是 `+445/3` 綠、`s` 白，一眼就看得出來。
        //
        // 上面那一段只黏標點（見 #hugs 明確排除字母），所以碰不到 `s`。
        //
        // 判準收得很緊：必須<b>緊接在數值後面</b>（lead == 0，中間連標點都沒有）、
        // 必須是三個以內的 ASCII 小寫字母（s、m、h、stx）、而且後面不能再接字母
        // ——再接字母那是一個單字，不是單位。中文不是 ASCII，所以「{~} 秒」
        // 這種寫法完全不受影響。
        int unit = before == null ? 0 : unitLength(text, afterNumber, lead, tail);
        if (unit > 0) {
            out.append(literal(text.substring(0, unit), forDisplay(before)));
            lead = unit;
        }
        // 遊戲自己的符號（❤ ✦ ⬤⋯）用原本的字型畫，見 #rebuild 那邊的說明。
        // 只剝<b>開頭</b>那一串：符號在原文裡幾乎都掛在標籤前面，
        // 而剝得越少，誤把該換字型的東西留在舊字型的機會也越少。
        int mid = lead;
        while (mid < tail && isPictograph(text.charAt(mid))) {
            mid++;
        }
        if (mid > lead) {
            out.append(literal(text.substring(lead, mid), symbol));
            lead = mid;
        }
        if (tail > lead) {
            appendNoting(out, text.substring(lead, tail), base, note,
                         depth, accents, used, store);
        }
        if (tail < text.length()) {
            out.append(literal(text.substring(tail), forDisplay(after)));
        }
    }

    /**
     * 單位後綴最多幾個字母。
     *
     * <p>{@code s}、{@code m}、{@code h} 是一個，{@code stx}（綠寶石堆疊單位）是三個。
     * 再長就不像單位了，寧可少黏。
     */
    private static final int MAX_UNIT = 3;

    /**
     * 原文開頭那個符號自己的樣式。
     *
     * <p>符號在原文裡通常自成一段（顏色與字型都跟正文不同），而整段的「主樣式」
     * 取的是佔多數的那一種。拿主樣式去畫符號，顏色就跟原文對不上——戰鬥資訊面板的
     * 愛心變成白色就是這樣來的。
     *
     * <p>只認<b>開頭</b>：符號幾乎都掛在標籤前面，而認得越少，把不該套的樣式
     * 套上去的機會也越少。開頭不是符號就回傳 null，由呼叫端退回主樣式。
     */
    private static Style leadingSymbolStyle(List<LineParts.Piece> runs) {
        for (LineParts.Piece run : runs) {
            String text = run.text();
            if (text == null || text.isEmpty()) {
                continue;
            }
            return isPictograph(text.charAt(0)) ? run.style() : null;
        }
        return null;
    }

    /** 單位用的字母：只認 ASCII 小寫。大寫與中文都不是單位。 */
    private static boolean isUnitLetter(char c) {
        return c >= 'a' && c <= 'z';
    }

    /**
     * 開頭有幾個字元是「緊貼在數值後面的單位」。
     *
     * <p>抽出來是為了測得到：黏不黏的<b>判斷</b>不需要字型，而那正是會出錯的一步。
     *
     * @param afterNumber 前一個佔位符是不是數值
     * @param lead        標點已經吃掉幾個字元；不是 0 就表示中間隔著東西，不算緊貼
     * @param tail        這一段可以動的範圍右界
     * @return 單位的長度；不是單位時回傳 0
     */
    static int unitLength(String text, boolean afterNumber, int lead, int tail) {
        if (!afterNumber || lead != 0 || text.isEmpty()) {
            return 0;
        }
        int unit = 0;
        while (unit < tail && unit < MAX_UNIT && isUnitLetter(text.charAt(unit))) {
            unit++;
        }
        if (unit == 0) {
            return 0;
        }
        // 後面還接著字母的話，那是一個單字不是單位
        return unit >= text.length() || !Character.isLetter(text.charAt(unit)) ? unit : 0;
    }

    /** 會黏在佔位符身上的標點：不是字、不是數字、不是方塊字，也不是空白或圓括號。 */
    static boolean hugs(char c) {
        if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || isHan(c)) {
            return false;
        }
        return c != '(' && c != ')' && c != '（' && c != '）';
    }

    /**
     * 斷行不要把一個重點詞切成兩半。
     *
     * <h2>為什麼會切到</h2>
     * 重點段的樣式是靠<b>字面比對</b>貼回去的（見 {@link #appendText}），而比對是
     * 逐行做的。譯文被面板寬度斷開之後，「地屬性」可能是「地」留在上一行、
     * 「屬性」跑到下一行——兩行各自都找不到「地屬性」，那個綠色就整個掉了。
     * 畫面上是圖示還是綠的（它走符號池那條路，樣式本來就跟著走），
     * 名稱卻是灰的。
     *
     * <p>做法是把被切開的那幾個字<b>往下一行搬</b>。下一行因此寬一點點，
     * 面板會跟著寬一兩個字——比一個詞半綠半灰好。
     *
     * <p>只搬<b>不含佔位符</b>的片段：佔位符是照順序取用的，搬動它會讓池子錯位。
     */
    static String[] keepAccentsWhole(String[] lines, List<LineParts.Piece> accents) {
        if (lines.length < 2 || accents.isEmpty()) {
            return lines;
        }
        String[] out = lines.clone();
        for (int i = 0; i + 1 < out.length; i++) {
            int move = splitAcross(out[i], out[i + 1], accents);
            if (move > 0) {
                // 詞搬下去之後，行尾若只剩一個圖示，圖示也要跟著走。
                //
                // 實機回報：「一件強大的神器，可透過 {#}」換行「物品升級師 將…」
                // ——{#} 是「物品升級師」前面那個圖示，兩者本來是一體的。
                // 折行本身有 keepGlyphWithWord 顧著，但這裡是<b>折完之後</b>
                // 又把詞搬走，圖示就這樣被留在行尾。
                //
                // 認得出來的形狀是「行尾是圖示，後面只有空白」。
                move += trailingGlyph(out[i], out[i].length() - move);
                String moved = out[i].substring(out[i].length() - move);
                out[i] = out[i].substring(0, out[i].length() - move);
                out[i + 1] = moved + out[i + 1];
            }
        }
        return out;
    }

    /**
     * 搬走 {@code from} 之後，行尾還剩下的圖示有多長（含它後面的空白）。
     *
     * <p>「{@code …可透過 {#} }」搬走「物品升級師」之後只剩「{@code …可透過 {#} }」
     * ——圖示孤零零掛在行尾，而它本來是下一個詞的一部分。回傳要<b>多搬</b>幾個字。
     *
     * <p>圖示前面若還有東西（正常情況），只搬圖示與它後面的空白；整行搬完會空掉
     * 的話就不搬——寧可圖示在行尾，也不要多出一個空行。
     */
    private static int trailingGlyph(String head, int from) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = from;
        while (at > 0 && head.charAt(at - 1) == ' ') {
            at--;
        }
        if (at - glyph.length() <= 0 || !head.startsWith(glyph, at - glyph.length())) {
            return 0;                          // 行尾不是圖示，或整行只有圖示
        }
        return from - (at - glyph.length());
    }

    /** 上一行結尾有幾個字是下一行開頭那個重點詞的一部分；沒有就是 0。 */
    private static int splitAcross(String head, String tail,
                                   List<LineParts.Piece> accents) {
        if (head.isEmpty() || tail.isEmpty()) {
            return 0;
        }
        for (LineParts.Piece accent : accents) {
            String word = accent.text();
            if (word.length() < 2 || word.length() > MAX_REFLOW) {
                continue;
            }
            for (int cut = 1; cut < word.length(); cut++) {
                String left = word.substring(0, cut);
                if (!head.endsWith(left) || !tail.startsWith(word.substring(cut))) {
                    continue;
                }
                if (left.indexOf('{') >= 0 || left.indexOf('}') >= 0) {
                    break;                    // 佔位符不能搬，搬了池子就錯位
                }
                return cut;
            }
        }
        return 0;
    }

    /** 為了不切斷重點詞，最多把這麼長的詞搬到下一行。再長就不值得重排了。 */
    private static final int MAX_REFLOW = 12;

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
            // 同一個位置時取<b>比較長</b>的那一個——這跟重點段彼此之間本來就用的
            // 規則一致，先前只有重點段互比時套用，跟詞典比的時候卻是重點段無條件勝。
            //
            // 後果：`Dimensional Tear` 在原文被 tooltip 寬度拆成兩行，第一行只剩
            // `Dimensional`，那一段成了重點段。它跟詞典裡的 `Dimensional Tear`
            // 從同一個位置開始，於是短的贏——譯文就卡著半個英文名字。
            boolean longer = term != null && at >= 0 && term.start() == at
                    && term.end() - term.start() > accents.get(which).text().length();
            if (term != null && (at < 0 || term.start() < at || longer)) {
                if (term.start() > from) {
                    String before = text.substring(from, term.start());
                    // 英文詞前面那個半形空格，換成中文之後就多餘了。
                    // 語料寫的是「Bash 的作用範圍」——空格是給英文用的；
                    // Bash 換成「重擊」以後留著它，畫面上是「重擊 的作用範圍」。
                    if (dropsSpaceBefore(before, term.translation())) {
                        before = before.substring(0, before.length() - 1);
                    }
                    out.append(literal(before, base));
                }
                // 這個位置如果<b>同時</b>有一個帶樣式的片段，樣式要留住。
                // 先前一律用 base 畫，於是 `Heal` 與 `Arcane Transfer` 的底線
                // 在替換的當下就沒了——原文有底線、譯文沒有。
                Style style = base;
                for (int k = 0; k < accents.size(); k++) {
                    if (used[k]) {
                        continue;
                    }
                    String accentText = accents.get(k).text();
                    if (text.startsWith(accentText, term.start())
                            && term.start() + accentText.length() <= term.end()) {
                        style = forDisplay(accents.get(k).style());
                        used[k] = true;
                        break;
                    }
                }
                out.append(literal(term.translation(), style));
                from = term.end();
                if (dropsSpaceAfter(term.translation(), text, from)) {
                    from++;
                }
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

    /**
     * 帶樣式的片段，連同<b>它的譯文</b>一起收。
     *
     * <h2>為什麼需要</h2>
     * 樣式是拿原文的字面去譯文裡找的：{@code Meteor} 保留英文，所以找得到，
     * 底線就跟著在。但 {@code Main Attack} 翻成「普攻」之後，字面對不上，
     * <b>底線與顏色就整個掉了</b>——畫面上原文有底線、譯文沒有。
     *
     * <p>所以每個帶樣式的片段都再登記一份「譯文版」，樣式沿用原本那個。
     * 兩份都留著：原文版負責沒被翻的情況，譯文版負責翻了的情況。
     */
    private static List<LineParts.Piece> withTranslations(List<LineParts.Piece> accents,
                                                          String translated,
                                                          TranslationStore store) {
        if (store == null || accents.isEmpty()) {
            return accents;
        }
        List<LineParts.Piece> out = new ArrayList<>(accents);
        for (LineParts.Piece accent : accents) {
            String core = accent.text().strip();
            if (core.isEmpty()) {
                continue;
            }
            addStem(out, core, accent.style(), translated);
            String zh = store.lookup(core);
            if (zh == null || zh.isBlank()) {
                // 詞典裡的詞也算——而且它認得「詞 + 尾巴的圖示」那種色段，
                // 見 TranslationStore#lookupTerm
                zh = store.lookupTerm(core);
            }
            if (zh == null || zh.isBlank()) {
                zh = lookupWordCore(core, store);
            }
            if (zh != null && !zh.isBlank() && !zh.equals(core)) {
                out.add(new LineParts.Piece(zh, accent.style()));
            }
        }
        return out;
    }

    /**
     * 剝掉色段<b>前後</b>的圖示與標點再查一次。
     *
     * <h2>為什麼需要這一步</h2>
     * 屬性名稱在畫面上是「圖示 + 名稱」，而且<b>圖示與名稱同屬一個色段</b>：
     * 力量說明的最後一段原文是 {@code §2<U+E001> Earth}，深綠色從圖示一路蓋到
     * {@code Earth}。{@link String#strip()} 只去空白，去不掉那個圖示碼位，
     * 於是查表的鍵是 {@code "<U+E001> Earth"}——查不到，譯文那邊的「地屬性」
     * 就完全沒有顏色，只剩圖示是綠的（圖示走的是另一條路：它在譯文裡是
     * {@code {#}}，由字形池原樣填回，顏色自然還在）。
     *
     * <p>{@link TranslationStore#lookupTerm} 只處理<b>尾巴</b>的圖示，因為它是
     * 為「名稱 + 圖示」那種色段寫的。這裡把兩頭都剝掉，補上前導圖示那一種。
     *
     * <p>只回傳<b>核心詞</b>的譯文，不把剝掉的圖示接回去——譯文裡的圖示是獨立
     * 片段，接回去反而對不上字面。
     */
    static String lookupWordCore(String text, TranslationStore store) {
        int from = 0;
        int to = text.length();
        while (from < to && !Character.isLetterOrDigit(text.charAt(from))) {
            from++;
        }
        while (to > from && !Character.isLetterOrDigit(text.charAt(to - 1))) {
            to--;
        }
        if (from == 0 && to == text.length()) {
            return null;                       // 沒東西可剝，上面已經查過了
        }
        String core = text.substring(from, to).strip();
        if (core.isEmpty()) {
            return null;
        }
        String zh = store.lookup(core);
        return zh == null || zh.isBlank() ? store.lookupTerm(core) : zh;
    }

    /**
     * 換上中文之後，前面那個半形空格該不該一起吃掉。
     *
     * <p>條件是三個都成立：前面剛好以一個半形空格結尾、空格前是中日韓文字、
     * 換上去的譯名以中日韓文字開頭。只要有一邊是英文，那個空格就仍然需要
     * ——「gains 重擊」拿掉空格會黏成一團。
     */
    static boolean dropsSpaceBefore(String before, String translation) {
        return before.length() >= 2
                && before.charAt(before.length() - 1) == ' '
                && isHan(before.charAt(before.length() - 2))
                && !translation.isEmpty() && isHan(translation.charAt(0));
    }

    /** 同上，看的是譯名<b>後面</b>那個空格。 */
    static boolean dropsSpaceAfter(String translation, String text, int at) {
        return !translation.isEmpty() && isHan(translation.charAt(translation.length() - 1))
                && at + 1 < text.length()
                && text.charAt(at) == ' '
                && isHan(text.charAt(at + 1));
    }

    /**
     * 遊戲自己的符號，例如 {@code ❤ ✦ ⬤ ❁ ✺}。
     *
     * <p>判準是「非 ASCII、不是漢字、也不是字母或數字」。這些字元在 Wynncraft
     * 那組字型裡是特製字形，換成預設字型會退回 Unicode 的通用字形——顏色與形狀
     * 都不一樣。字母數字排除掉，是因為那些本來就該跟著正文走。
     */
    private static boolean isPictograph(char c) {
        return c > 0x7F && !isHan(c) && !Character.isLetterOrDigit(c)
                && !Character.isWhitespace(c);
    }

    private static boolean isHan(char c) {
        Character.UnicodeScript script = Character.UnicodeScript.of(c);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || (c >= '＀' && c <= '￯')      // 全形標點
                || (c >= '　' && c <= '〿');
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

    /**
     * 最後一道防線：顏色佔位符絕對不能出現在畫面上。
     *
     * <h2>為什麼需要</h2>
     * 實機回報的畫面是「使用這件物品進入討伐戰{/」換行「}，或製作腐化地城鑰匙」
     * ——{@code {/}} 被原樣印出來，還被換行切成兩半。
     *
     * <p>{@code {cN}} 與 {@code {/}} 是<b>我們自己</b>的標記，正常情況下
     * {@link #tokenize} 會在重建時把它們吃掉。但語料裡的譯文會經過好幾條路徑
     * （逐片段替換、前綴組合、名牌⋯⋯），只要有一條沒走到 tokenize，那些標記
     * 就會一路帶到畫面上。
     *
     * <p>{@link #literal} 是所有文字變成 {@code Component} 的共同出口，擋在這裡
     * 涵蓋得最完整。走過 tokenize 的文字本來就沒有這些標記，這一步是空操作。
     *
     * <p>這是<b>防線</b>不是修正：真正該做的是讓每條路徑都處理顏色。但那些標記
     * 印在畫面上是玩家一眼就看到的壞掉，而少一層顏色只是不夠漂亮——先擋住。
     */
    static String stripColourTokens(String text) {
        if (text == null || text.indexOf('{') < 0) {
            return text;                       // 絕大多數的字根本沒有大括號
        }
        String out = text;
        if (out.contains(COLOR_END)) {
            out = out.replace(COLOR_END, "");
        }
        return COLOUR_ANY.matcher(out).replaceAll("");
    }

    /**
     * 任何形式的顏色佔位符：{@code {c1}}、{@code {c:#FF55FF}}。
     *
     * <p>跟 {@link #COLOUR_TOKEN} 分開是因為那個要抓編號來重編，只認數字那種；
     * 這個只負責清掉，寧可抓得寬。
     */
    private static final java.util.regex.Pattern COLOUR_ANY =
            java.util.regex.Pattern.compile("\\{c[^}]*}");

    private static Component literal(String text, Style style) {
        text = stripColourTokens(text);
        Style base = upright(text, style);
        // 我們把字型換掉了，斜體就跟著丟。
        //
        // 斜體在 Minecraft 裡是繪製時的<b>剪切</b>，不是另一套字形。Wynncraft
        // 自己那組點陣字被剪切之後幾乎看不出來，換成預設字型就整個歪出去——
        // 使用者看到的「同一行字，原文是正的、譯文是斜的」就是這樣來的
        // （Corkian Augments）。
        //
        // 原本就用預設字型的片段不動：那些的斜體是遊戲真的想要的效果，
        // 我們照抄才對。
        if (style != null && style.getFont() != null
                && !FontDescription.DEFAULT.equals(style.getFont())) {
            base = base.withItalic(false);
        }
        return text.indexOf(SECTION) < 0
                ? Component.literal(text).withStyle(base)
                : coloured(text, base);
    }

    /** Minecraft 的格式碼前綴。 */
    private static final char SECTION = '§';

    /**
     * 讓譯文自己帶 {@code §} 格式碼。
     *
     * <h2>為什麼要支援</h2>
     * 大部分時候譯文的顏色是從原文<b>搬</b>過來的（見 {@link #appendText}），
     * 那對「原文有色、譯文照抄」的情況剛好。但有些地方是中文<b>自己</b>需要
     * 強調——原文沒有對應的色段，搬不過來。翻譯團隊要嘛放棄排版，
     * 要嘛得請人改程式。
     *
     * <p>支援 {@code §} 之後，那種需求在譯文檔裡就解決得掉：
     * {@code "§c警告§r：這會消耗你的魂"}。這是 Minecraft 自己的寫法，
     * 譯者本來就熟。
     *
     * <h2>底色仍然是這一段的樣式</h2>
     * {@code §} 只<b>覆蓋</b>它管到的那一截，沒被覆蓋的部分照樣用原本的樣式
     * ——所以在技能樹那種「顏色來自原文」的地方，不寫 {@code §} 就完全不受影響，
     * 寫了才會蓋掉。{@code §r} 回到這一段原本的樣式，而不是回到全白。
     */
    static Component coloured(String text, Style base) {
        MutableComponent out = Component.empty();
        Style now = base;
        int from = 0;
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != SECTION) {
                continue;
            }
            ChatFormatting code = ChatFormatting.getByCode(
                    Character.toLowerCase(text.charAt(i + 1)));
            if (code == null) {
                continue;                      // §後面不是格式碼，當成普通文字
            }
            if (i > from) {
                out.append(Component.literal(text.substring(from, i)).withStyle(now));
            }
            now = code == ChatFormatting.RESET ? base : now.applyFormat(code);
            i++;                               // 跳過格式碼本身
            from = i + 1;
        }
        if (from < text.length()) {
            out.append(Component.literal(text.substring(from)).withStyle(now));
        }
        return out;
    }

    /**
     * 方塊字不跟著原文一起斜。
     *
     * <h2>為什麼</h2>
     * Minecraft 的斜體不是另一套字型，是<b>把字形往右推一個剪切</b>。
     * 拉丁字母本來就有傾斜的字形設計，剪切之後看起來還算正常；方塊字沒有，
     * 剪切出來的是糊成一團的斜方塊——筆畫互相穿插，相鄰兩個字還會疊到，
     * 窄一點的介面根本讀不出來。
     *
     * <p>而 GUI 的標題幾乎<b>全部</b>是斜的：Minecraft 只要物品有自訂名稱就自動
     * 加上斜體，Wynncraft 沒有特地關掉。原文是拉丁字母所以沒人在意，
     * 一換成中文就整排糊掉。
     *
     * <p>所以只丟掉斜體，顏色與粗體照抄——那兩個對方塊字沒有副作用。
     * 判斷看的是<b>這一段文字</b>而不是整行：同一行裡的英文片段（技能名、
     * 裝備名）該斜還是斜，跟原文一致。
     */
    private static Style upright(String text, Style style) {
        Style base = style == null ? Style.EMPTY : style;
        // 斜體一定要<b>寫死</b>，不能留成「沒設定」——沒設定的會繼承父層，
        // 而 GUI 的物品標題本身是斜的。先前只在有方塊字時才關掉，
        // 純英文的段落於是把父層的斜體繼承下來，畫面上就是
        // 「原文不斜、我們重建出來的那份是斜的」（例如 Corkian Augments）。
        return base.withItalic(base.isItalic() && !hasHan(text));
    }

    /**
     * 有中文的那一行，<b>整行</b>都不斜。
     *
     * <h2>為什麼要整行一起看</h2>
     * {@link #upright} 是逐段判斷的：這一段有方塊字就拿掉斜體，沒有就留著。
     * 單獨看每一段都對，合起來就壞了——技能樹的標題只有中間那個名字被翻譯：
     *
     * <pre>
     *   原文  Unlock Cheaper Totem ability      ← 整行同一個樣式
     *   譯文  Unlock 節約．圖騰 ability          ← 英文那兩截還斜著，中文是正的
     * </pre>
     *
     * 一行裡半斜半正，比整行斜還醒目。使用者回報的「技能樹還是有斜體問題」
     * 就是這個樣子。
     *
     * <p>所以在最後<b>整行</b>再看一次：只要這一行出現方塊字，斜體一律拿掉。
     * 純英文的行不動——那些的斜體是遊戲真的想要的效果，我們照抄才對。
     *
     * <p>順便把樣式攤平成明確的值。{@code visit} 會把繼承來的樣式解出來，
     * 於是「沒設定」不再繼承到別人的父層——那正是斜體最容易漏進來的縫。
     */
    /**
     * 整塊拉正：不管這一行有沒有方塊字，斜體一律拿掉。
     *
     * <h2>為什麼要整塊做</h2>
     * {@link #unslant} 只拉正<b>含方塊字的那一行</b>，理由見 UprightTest：
     * 中文被 Minecraft 的斜體剪切會糊成一團。但這樣一來，同一份 tooltip 裡
     * 翻好的行是正的、還沒翻的英文行還是斜的——實機看到的就是整份參差不齊。
     *
     * <p>MC 只要物品有自訂名稱就自動加斜體，Wynncraft 沒有關掉，所以幾乎每一份
     * 物品說明都會遇到。翻譯團隊選的是整塊拉正：中文不糊，整份也一致；
     * 代價是還沒翻的英文行跟原版遊戲不同（原版是斜的）。
     */
    public static Component unslantAll(Component line) {
        if (line == null) {
            return null;
        }
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.append(Component.literal(text).withStyle(style.withItalic(false)));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    static Component unslant(Component line) {
        if (line == null || !hasHan(line.getString())) {
            return line;
        }
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.append(Component.literal(text).withStyle(style.withItalic(false)));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** 這段文字裡有沒有方塊字。全形標點不算——單獨出現時剪切不礙事。 */
    private static boolean hasHan(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    // ------------------------------------------------------------ 模板切詞

    private enum Kind { TEXT, GLYPH, PLACE, NUMBER, USER, COLOR }

    /**
     * @param index 數值：指定要原文的第幾個（從 1 起），{@code 0} 表示照順序取下一個。
     *              顏色：原文調色盤的第幾個（從 1 起），{@code 0} 是 {@code {/}}
     *              收尾，{@code -1} 是 {@code {c:…}} 自己指定的顏色
     */
    private record Token(Kind kind, String text, int index) {
        Token(Kind kind, String text) {
            this(kind, text, 0);
        }
    }

    /** {@code {/}}：顏色到此為止，回到這一段原本的樣式。 */
    static final String COLOR_END = "{/}";

    /**
     * 譯文裡的顏色佔位符。
     *
     * <h2>為什麼需要</h2>
     * 顏色本來是<b>猜</b>出來的：拿原文裡帶特殊樣式的片段，到譯文裡找同樣的字面
     * （見 {@link #appendText}）。英文原樣留著的詞（技能名、地名）找得到，
     * 翻成中文的就找不到——那一段只好掉回底色。整行同色的還能靠位置補救
     * （見 {@link #wholeLineAccents}），一行裡混了兩三種顏色的就沒辦法：
     * 「{@code [Cave Completed] … - Rewards: … +1 Unidentified Helmet}」
     * 每一行的顏色都不一樣，譯文出來卻是一片灰。
     *
     * <p>猜不到的時候，唯一知道答案的是<b>譯者</b>。所以讓他直接寫出來：
     *
     * <pre>
     *   "{c1}[洞穴完成]{/}"                 ← 用原文的第 1 個顏色
     *   "{c2}獎勵{/}：{c3}未鑑定頭盔{/}"    ← 一行裡兩個顏色，就兩個佔位符
     *   "{c:#FF55FF}警告{/}"                ← 原文沒有對應色段時自己指定
     * </pre>
     *
     * <h2>編號怎麼來的</h2>
     * {@code {cN}} 指的是<b>這一條原文</b>用到的第 N 個顏色，依第一次出現的順序
     * 編號（見 {@link #palette}）。編號而不是寫死色碼，好處是搬的是樣式<b>物件</b>
     * 本身——粗體、底線、Wynncraft 自己那些非原版的色碼都一起帶過去，
     * 而且遊戲改調色盤時譯文自動跟著改。
     *
     * <p>哪一個編號是哪一個顏色，看診斷檔 {@code majorid-debug.txt} 的
     * 「可用的顏色」那一段。
     *
     * <h2>寫錯了會怎樣</h2>
     * 編號超出範圍就<b>當作沒寫</b>，那一段照舊走猜的那條路。整條譯文不會因此
     * 消失——顏色不對比整句變回英文好。
     *
     * @return 這個位置的顏色佔位符；不是的話回傳 {@code null}
     */
    private static Token colourToken(String template, int at) {
        if (template.startsWith(COLOR_END, at)) {
            return new Token(Kind.COLOR, COLOR_END, 0);
        }
        if (at + 3 > template.length()
                || template.charAt(at) != '{' || template.charAt(at + 1) != 'c') {
            return null;
        }
        int end = template.indexOf('}', at + 2);
        if (end < 0) {
            return null;
        }
        String body = template.substring(at + 2, end);
        String whole = template.substring(at, end + 1);
        if (body.length() == 1 && body.charAt(0) >= '1' && body.charAt(0) <= '9') {
            return new Token(Kind.COLOR, whole, body.charAt(0) - '0');
        }
        if (body.length() > 1 && body.charAt(0) == ':') {
            return new Token(Kind.COLOR, whole, -1);
        }
        return null;
    }

    /**
     * 原文用到的顏色，依<b>第一次出現</b>的順序編號。{@code {c1}} 就是這裡的第一個。
     *
     * <p>只看有實字的片段：純空白、純排版符號沒有「顏色」可言，收進來只會讓
     * 編號跟譯者在畫面上看到的對不起來。
     */
    static List<Style> palette(List<LineParts.Piece> runs) {
        List<Style> out = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            if (!hasContent(run.text())) {
                continue;
            }
            Style style = run.style() == null ? Style.EMPTY : run.style();
            if (!out.contains(style)) {
                out.add(style);
            }
        }
        return out;
    }

    /**
     * 一個顏色佔位符要套的樣式；套不出來就回傳 {@code null}（照舊走猜的那條路）。
     *
     * @param base 這一段原本的樣式，{@code {c:…}} 只換顏色、其餘沿用
     */
    private static Style colourOf(Token token, List<Style> palette, Style base) {
        if (token.index() == 0) {
            return null;                       // {/}：回到底色
        }
        if (token.index() > 0) {
            return token.index() <= palette.size()
                    ? forDisplay(palette.get(token.index() - 1)) : null;
        }
        String spec = token.text().substring(3, token.text().length() - 1).strip();
        if (spec.startsWith("#")) {
            try {
                return base.withColor(TextColor.fromRgb(
                        Integer.parseInt(spec.substring(1), 16)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        ChatFormatting named = ChatFormatting.getByName(spec);
        return named == null ? null : base.applyFormat(named);
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
            } else if (colourToken(template, i) != null) {
                // {c1} {c:#FF55FF} {/}：譯者自己指定顏色。見 #colourToken。
                Token colour = colourToken(template, i);
                flush(text, out);
                out.add(colour);
                i += colour.text().length();
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
