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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        boolean layoutRow = false;
        for (StyledText line : run) {
            LineParts p = LineParts.of(line);
            if (p.template().isBlank()) {
                // 段落之間的空行不能併進來。攤平查表會把它壓成一個空格，
                // 於是整段連著空行一起被換掉——譯文出來就跟上一段黏在一起了。
                return null;
            }
            // 純排版的行（分隔線、只有圖示與偏移的那種）也是同一件事，只是
            // 它的模板<b>不是空的</b>（「{#}{#}」），所以躲過上面那一關。見 layoutRow。
            // 只看「拿掉圖示之後還剩不剩東西」：Major ID 敘述斷行後，最後一行常常
            // 只剩「{#}+{~}.」，沒有字母但是內容，不能當成分隔線。
            layoutRow |= p.template()
                    .replace(GlyphSplitter.GLYPH_PLACEHOLDER, "").isBlank();
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
        if ((translated == null || translated.isBlank()) && layoutRow) {
            // 攤平查表這條路<b>只能</b>給「被 tooltip 寬度折斷的一整段」用：
            // 它把換行壓成空格，等於假設每一行都是同一句話的一部分。
            //
            // 這段裡有純排版的行時那個假設就不成立，而後果不是「查不到」，
            // 是<b>查到別的東西</b>：Shiny 物品的統計列上面正好是一條分隔線，
            //
            // <pre>
            //   [n  ] {#}{#}                                ← 分隔線
            //   [n+1] {#} Boss Altars Won{#}{~} {#}{#}{#}    ← 統計列
            // </pre>
            //
            // 攤平之後剛好對上語料裡那條「{#}{#} Boss Altars Won…」——那條本身
            // 就是從攤平過的 capture 收進來的。命中之後兩行被併成一行：分隔線
            // 不見了，而它的縮排偏移接到了標籤前面，整列被推到面板中間。
            // 使用者回報的「應該在兩條橫線中間，卻獨立出一行」就是這個。
            //
            // 整段收在語料裡的那種（{@code raid.json} 有幾條第一行就是 {#}{#}）
            // 走的是上面那次<b>精確</b>查表，不受這一關影響。
            return null;
        }
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
            // 技能名要在<b>折行之前</b>、對整句換掉。
            //
            // 語料裡說明刻意留著英文技能名，畫的時候才由詞表換（見 appendText）。先前是
            // 先折行、再逐行換詞，兩件事都壞：
            //
            //   1. 折行量的是還帶著英文名的譯文。「Malicious Mockery」比「譏世弄人」寬得多，
            //      斷點全算在錯的位置，換完只剩「譏世弄人使」一小截自己佔一行。
            //   2. 多字的名稱被斷在兩行之間，逐行找詞時兩行各只有半個名字：「Last」「Laugh」
            //      都不是詞而留英文；「Guardian」剛好是另一個詞（Major ID 名稱「守護者」），
            //      畫面上就成了「焚化守護者」換行「Angels」。
            //
            // 整句先換好，折行看到的就是最後要畫的字；rebuildAll 裡逐行那一次找不到英文名，
            // 什麼都不會做。名稱原本的樣式由 withTranslations 登記的「譯文版」重點段接手。
            // 見 MajorIdTermWrapTest。
            String swapped = termsWithin(translated, store);
            if (swapped != null) {
                translated = swapped;
            }
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
     * @param places 整段的地名，<b>逐行認出來的與跨行認出來的併在一起</b>，
     *               照出現順序排。見 {@link #rejoin} 的「地名池不能只裝跨行那些」
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
        List<LineParts.Piece> named = new ArrayList<>();
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
            // 圖示後面還可能接著一串 À——那是 Wynncraft 的<b>縮排字元</b>，
            // 以一般文字的樣子出現，所以上面剝圖示那一步剝不到。
            //
            // Fabled 物品的 Major ID 就長這樣：續行前面墊兩格，對齊在「◆ 」後面。
            //
            // <pre>
            //   {#}{#}Freerunner: When your sprint
            //   ÀÀbar is under {~} full,
            //   ÀÀincrease your sprint speed by
            //   ÀÀ+{~}.
            // </pre>
            //
            // 沒剝掉的話攤平的鍵夾著「ÀÀ」，跟語料裡乾淨的那一句永遠對不上。
            // 使用者回報 Air In A Can 只有名稱是中文、說明整段英文，就是這個。
            // 縮排跟行首偏移一樣是排版，不是內容；它不在符號池裡，剝掉不必動池子。
            // 整行只剩縮排的不剝，理由同上面的分隔線。
            int indent = TranslationStore.indentOf(line.substring(at));
            if (indent > 0 && at + indent < line.length()) {
                at += indent;
                trimmed = true;
            }
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(line, at, line.length());
            List<LineParts.Piece> own = part.glyphs();
            kept.addAll(own.subList(Math.min(lead, own.size()), own.size()));
            named.addAll(part.places());
        }

        String text = joined.toString();
        // 池子要裝<b>整段所有</b>的地名，不能只裝這裡新認出來的。
        //
        // 逐行那一輪（LineParts#of）已經把認得出來的換成了 {p}，所以下面這個
        // 掃描看到的是 {p}，不是地名本身——它<b>看不到</b>逐行認掉的那些。
        // 先前直接拿新認到的那幾個當整段的池子，於是逐行認掉的地名憑空消失：
        // 譯文裡的 {p} 沒有東西可以填，rebuildAll 判定佔位符不符、整段放棄。
        // 畫面上就是第一行中文、其餘英文，而且「置中又含地名」的整段敘述
        // 全部中招（Grootslang 之巢、光之領域那幾段）。
        //
        // 兩份都是照出現順序排的，交錯併起來就是整段的順序。
        List<LineParts.Piece> merged = new ArrayList<>();
        boolean crossed = false;                 // 有被斷行切開、這裡才認出來的
        java.util.regex.Matcher place = PlaceNames.matcher(text);
        if (place != null) {
            StringBuilder out = new StringBuilder();
            int from = 0;
            int taken = 0;
            while (place.find()) {
                taken = carry(text, from, place.start(), named, taken, merged);
                out.append(text, from, place.start())
                   .append(GlyphSplitter.PLACE_PLACEHOLDER);
                merged.add(new LineParts.Piece(place.group(), style));
                crossed = true;
                from = place.end();
            }
            if (crossed) {
                carry(text, from, text.length(), named, taken, merged);
                text = out.append(text.substring(from)).toString();
            }
        }
        if (!crossed) {
            merged = named;                      // 沒有跨行的，池子就是逐行那份
        }
        return trimmed || crossed
                ? new Rejoined(text, List.copyOf(kept), List.copyOf(merged)) : null;
    }

    /**
     * 把某一段模板裡<b>已經是</b> {@code {p}} 的那幾個地名依序搬進新的池子。
     *
     * <p>見 {@link #rejoin}：跨行認出來的地名要插在正確的位置，而它的前後
     * 都可能有逐行就認掉的地名。這裡按 {@code {p}} 的出現次數從舊池子取用，
     * 兩邊的順序就對得起來。
     *
     * @param taken 舊池子已經用掉幾個
     * @return 用掉的總數
     */
    private static int carry(String text, int from, int to,
                             List<LineParts.Piece> named, int taken,
                             List<LineParts.Piece> merged) {
        String mark = GlyphSplitter.PLACE_PLACEHOLDER;
        int at = text.indexOf(mark, from);
        while (at >= 0 && at < to) {
            if (taken < named.size()) {
                merged.add(named.get(taken++));
            }
            at = text.indexOf(mark, at + mark.length());
        }
        return taken;
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
     *
     * <h2>多數色不算佔位符</h2>
     * 跟 {@link #perPartStyles} 是<b>同一件事</b>，只是發生在「整段命中」這條路上。
     * 數值、地名、玩家名是從原文抽出去、填回時<b>自己帶著原樣式</b>回來的
     * （見 {@link LineParts}），所以不能讓它們決定周圍的散文是什麼顏色。
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
     * <p>整段一次命中，於是整段的散文都畫這裡挑出來的底色。照實字數算：
     * 灰的 {@code Bring}、{@code to the}、{@code Slaying Post}、{@code at} 共 23 個，
     * 青的兩個方括號共 26 個——青贏，畫面上整段散文變成青色。
     *
     * <p>那 26 個裡有 4 個是<b>數值本身</b>（{@code 24}、{@code 88}），而數值早就
     * 另外保管、會自己帶青色回來。扣掉之後青的剩 22、灰的 23，灰贏——
     * 跟肉眼看到的一致。白的座標同理：14 個實字裡 9 個是數值，扣完只剩
     * {@code [,,-]} 5 個。
     *
     * <p>順帶一提，數的是<b>實字</b>（{@link #solidCount}）而不是字串長度。
     * 先前數的是長度，空白也算一份：上面那段是灰 29 對青 30，青只贏在一個空格上。
     * 扣佔位符的份量本來就是照實字算的，兩把尺要一樣才扣得準。
     */
    private static Style dominantStyle(List<LineParts> parts) {
        // 跟 #perPartStyles 用同一個 Tally：併掉裝飾、平手取先出現的，
        // 都是那邊已經談過的事，不要再長出第二套算法。
        Tally tally = new Tally();
        // 方括號裡的字不投票，見 #proseCount。深度跨行帶著走：括號常常被
        // tooltip 寬度切成兩行，左括號在這一行、右括號在下一行。
        int[] depth = {0};
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
                tally.add(run.style(), proseCount(run.text(), depth));
            }
        }
        // 抽出去的佔位符不算數，見上。符號（{#}）不必扣——solidCount 本來就不數，
        // 而且 LineParts.of 也不會把純符號的片段收進 runs。
        for (LineParts part : parts) {
            for (List<LineParts.Piece> pool
                    : List.of(part.numbers(), part.places(), part.users())) {
                for (LineParts.Piece piece : pool) {
                    tally.remove(piece.style(), solidCount(piece.text()));
                }
            }
        }
        // 扣完可能一個都不剩（整段就只有一個座標那種）。那就退回第一行的樣式，
        // 跟先前「一筆都沒累計到」的做法一樣——總得畫個顏色。
        Style top = tally.top();
        return undecorated(top == null ? parts.get(0).textStyle() : top);
    }

    /**
     * 方括號<b>外面</b>的實字有幾個。
     *
     * <h2>為什麼方括號裡的字不能投票</h2>
     * 底色要的是「散文是什麼顏色」。而方括號在這些卡片上就是重點記號——
     * 裡面那一段本來就<b>該</b>是另一個顏色，它出現多長跟散文無關。
     *
     * <p>只扣數值還不夠。實機那張 Arcane Anomalies 的迷你任務卡：
     *
     * <pre>
     *   §7Bring §3[15 Arcane Anomalies]§7 to
     *   §7the Slaying Post §3[Combat Lv.
     *   §375]§7 at §f[-677, 46, -4948]
     * </pre>
     *
     * <p>扣掉數值之後灰的還有 24 個（{@code Bring}、{@code to}、
     * {@code the Slaying Post}、{@code at}），青的 28 個——青贏，整段散文
     * 被畫成青色。名字長一點的卡片就會這樣，同一份診斷檔裡 30 段有 8 段中招，
     * 全是這個形狀。
     *
     * <p>扣掉括號裡的之後青只剩 {@code 75]} 的那一個右括號，灰穩穩地贏。
     *
     * <h2>不是「那個顏色一律不投票」</h2>
     * 扣的是<b>括號裡的字</b>，不是「跟括號同色的那一段」。青色的字只要落在
     * 括號外面照樣算數——見 {@code BlockProseColourTest} 的「青色多數」那一組：
     * {@code and [88 Soft Fur] right now} 整段是青的，括號外的
     * {@code and}、{@code right now} 共 11 個實字，仍然贏過灰的 {@code Get}。
     *
     * @param depth 單元素陣列，當作可變的「現在在不在方括號裡」
     */
    /**
     * 跳過前 {@code skip} 個實字之後，接下來 {@code take} 個實字裡有幾個在方括號外面。
     *
     * <p>{@link #proseCount} 的切片版：{@link #uniformStyles} 是照實字數把 run
     * 切給每一行的，切點落在 run 中間，所以不能整個 run 一起算。
     *
     * <p>{@code skip}／{@code take} 數的是<b>實字</b>（跟 {@link #solidCount}
     * 同一把尺，方括號本身也算一個），回傳的才是括號外面的那幾個。
     */
    private static int proseAmong(String text, int skip, int take, int[] depth) {
        int seen = 0;
        int kept = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            boolean bracket = cp == '[' || cp == ']';
            if (cp == '[') {
                depth[0]++;
            } else if (cp == ']' && depth[0] > 0) {
                depth[0]--;
            }
            if (Character.isWhitespace(cp)
                    || com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)) {
                continue;
            }
            seen++;
            if (seen <= skip) {
                continue;
            }
            if (seen > skip + take) {
                break;
            }
            if (!bracket && depth[0] == 0) {
                kept++;
            }
        }
        return kept;
    }

    private static int proseCount(String text, int[] depth) {
        int solid = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '[') {
                depth[0]++;
                continue;
            }
            if (cp == ']') {
                if (depth[0] > 0) {
                    depth[0]--;
                }
                continue;
            }
            if (depth[0] > 0 || Character.isWhitespace(cp)
                    || com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)) {
                continue;
            }
            solid++;
        }
        return solid;
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
        String wrapped = wrapBalanced(text, maxPx, rows, measure, true);
        if (rows == Integer.MAX_VALUE || widestRow(wrapped, measure) <= maxPx) {
            return wrapped;
        }
        // 有行數上限（折回原文那一塊）時，寬度也不該超過原文。
        //
        // clauseAhead 為了把近在眼前的逗號收進來，會讓一行多撐兩成。賜福改譯成
        // 「本次 Lootrun 剩餘期間，每出現一個信標，獲得 +{~1} 傷害 (最多 x{~2})」之後，
        // 第一行就撐成「本次 Lootrun 剩餘期間，每出現一個信標，」——183px，原文最寬才 157px，
        // 下一行卻短了一截。平均分配救不回來：每一次收窄，那一行都照比例再撐兩成。
        //
        // 不撐也放得進原文的行數，就用不撐的那一份；放不進才照舊撐寬。
        String inside = wrapBalanced(text, maxPx, rows, measure, false);
        return lines(inside) <= rows && widestRow(inside, measure) < widestRow(wrapped, measure)
                ? inside : wrapped;
    }

    /** @param reach 要不要為了把標點收進來讓一行超出寬度，見 {@link #clauseAhead} */
    private static String wrapBalanced(String text, int maxPx, int rows,
                                       ToIntFunction<String> measure, boolean reach) {
        // 先照最嚴的「不拆開」規則折（見 Keep），行數超過原文才一級一級放鬆，
        // 三級都超過才放寬寬度。順序是刻意的：數值跟屬性名分家只是難讀，
        // 面板比原文高、比原文寬卻是整份 tooltip 跟著變形。
        int width = maxPx;
        Keep keep = Keep.NONE;
        String wrapped = null;
        for (int attempt = 0; ; attempt++) {
            for (Keep level : Keep.values()) {
                String tried = wrapToWidth(text, width, measure, level, reach);
                if (lines(tried) <= rows) {
                    keep = level;
                    wrapped = tried;
                    break;
                }
            }
            if (wrapped != null || attempt >= WRAP_RETRIES) {
                break;
            }
            width = width * 11 / 10;
        }
        if (wrapped == null) {
            wrapped = wrapToWidth(text, width, measure, Keep.NONE, reach);
        }
        // ① 只差一點就放得下的，讓它留在同一行。
        //
        // 只在<b>剛好兩行</b>的時候放寬：三行以上的說明本來就長，為了它把整個
        // 面板撐寬四分之一不划算，而且也不會因此變成一行。
        if (lines(wrapped) == 2 && measure.applyAsInt(text) <= width * SNUG / 100) {
            return text;
        }
        // ② 斷在名稱後面。
        String head = labelBreak(text, width, rows, measure, keep, reach);
        if (head != null && lines(head) <= lines(wrapped)) {
            return head;
        }
        return balance(text, wrapped, width, measure, keep, reach);
    }

    /**
     * 試著把「名稱: 」單獨留在第一行。
     *
     * <p>會多出一行就不值得——{@code lines()} 由呼叫端比。折不出冒號、
     * 或名稱長到不像名稱的，回傳 {@code null} 表示這條路不通。
     */
    private static String labelBreak(String text, int width, int rows,
                                     ToIntFunction<String> measure, Keep keep,
                                     boolean reach) {
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
        String rest = wrapToWidth(body, width, measure, keep, reach);
        rest = balance(body, rest, width, measure, keep, reach);
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
                                  ToIntFunction<String> measure, Keep keep, boolean reach) {
        int rows = lines(wrapped);
        if (rows < 2) {
            return wrapped;                     // 一行沒得平均
        }
        String best = wrapped;
        int bestCost = wrapCost(text, wrapped, keep);
        int at = width;
        for (int step = 0; step < BALANCE_STEPS; step++) {
            int narrower = at * 19 / 20;        // 每次收 5%
            if (narrower <= 0 || narrower == at) {
                break;
            }
            String tighter = wrapToWidth(text, narrower, measure, keep, reach);
            if (lines(tighter) != rows) {
                break;                          // 再收就會多一行，停在上一個
            }
            if (splitsWord(tighter) && !splitsWord(wrapped)) {
                break;                          // 見 splitsWord：寧可不平均
            }
            at = narrower;
            // 行數一樣時先比「斷得好不好」，同分才取比較窄（比較平均）的。
            // 先前一律取最窄的：收到逗號放不進上一行時，斷點就掉進詞中間——
            // 「本次剩餘的整段期」換行「間，每提供一個信」，行數沒變所以照收。
            // 不 break：再收窄一點，斷點可能又回到標點上。
            int cost = wrapCost(text, tighter, keep);
            if (cost <= bestCost) {
                best = tighter;
                bestCost = cost;
            }
        }
        return best;
    }

    /**
     * 平均分配拿來比較的分數：拆開一組數值＋屬性名很重（見 {@link #unitsSplit}），
     * 其餘照 {@link #breakCost}。
     *
     * <h2>為什麼拆開要另外算</h2>
     * 折行本身不會拆開放得下一行的那一組（見 {@link #keepUnitWhole}），但平均分配會把寬度
     * 一路收窄——收到那一組從行首開始都放不下時，只好從中間斷。行數沒變、斷點也都在空白上，
     * 單看 {@link #breakCost} 分不出來，於是「+{~} к стихийному」剛好放得下的寬度，
     * 平均完變成「+{~} к」換行「стихийному」。
     */
    private static int wrapCost(String text, String wrapped, Keep keep) {
        return unitsSplit(text, wrapped, keep) * 10 + breakCost(wrapped);
    }

    /**
     * 折行結果拆開了幾組 {@link #valueUnits}（每一層都算）。
     *
     * <p>折行只插入換行、斷在空白上時吃掉那個空白，所以可以跟原文逐字對回去，
     * 得到每個換行在原文裡的位置。
     */
    static int unitsSplit(String text, String wrapped, Keep keep) {
        List<List<int[]>> units = valueUnits(text, keep);
        if (units.isEmpty()) {
            return 0;
        }
        int count = 0;
        int t = 0;
        for (int w = 0; w < wrapped.length() && t <= text.length(); w++) {
            if (wrapped.charAt(w) != NEWLINE || (t < text.length() && text.charAt(t) == NEWLINE)) {
                t++;
                continue;
            }
            for (List<int[]> layer : units) {
                for (int[] unit : layer) {
                    if (unit[0] < t && t < unit[1]) {
                        count++;
                    }
                }
            }
            // 斷在空白上時那個空白被行尾吃掉了，原文這邊跳過它
            if (t < text.length() && text.charAt(t) == ' '
                    && (w + 1 >= wrapped.length() || wrapped.charAt(w + 1) != ' '
                        || (t + 1 < text.length() && text.charAt(t + 1) == ' '))) {
                t++;
            }
        }
        return count;
    }

    /**
     * 一個折行結果的斷點有多難讀，越小越好。見 {@link #balance}。
     *
     * <ul>
     *   <li>斷在標點後面（，、。；：或半形的 , ; :）：0——那是句子本來的接縫</li>
     *   <li>一般的斷點（詞中間、空白上）：1</li>
     *   <li>下一行從左括號開始：2——括號註解多半在補充上一行最後那個數值</li>
     *   <li>下一行從收尾的標點開始：4——避頭本來就擋，這裡是保險</li>
     * </ul>
     *
     * <p>拉丁字母的譯文每個斷點都在空白上、全部是 1，所以對它們而言跟先前一樣取最窄的；
     * 只有碰到逗號才會多一個選擇。
     */
    static int breakCost(String wrapped) {
        int cost = 0;
        for (int at = wrapped.indexOf(NEWLINE); at >= 0; at = wrapped.indexOf(NEWLINE, at + 1)) {
            int prev = at;
            while (prev > 0 && wrapped.charAt(prev - 1) == ' ') {
                prev--;
            }
            int next = at + 1;
            while (next < wrapped.length() && wrapped.charAt(next) == ' ') {
                next++;
            }
            if (prev == 0 || next >= wrapped.length() || wrapped.charAt(prev - 1) == NEWLINE
                    || wrapped.charAt(next) == NEWLINE) {
                continue;                       // 空行不是斷點
            }
            char before = wrapped.charAt(prev - 1);
            char after = wrapped.charAt(next);
            if (cannotStartLine(after)) {
                cost += 4;
            } else if (after == '(' || after == '（') {
                cost += 2;
            } else if (CLAUSE_END.indexOf(before) < 0 && ",;:".indexOf(before) < 0) {
                cost += 1;
            }
        }
        return cost;
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

    /** 折好的結果裡最寬的那一行。 */
    private static int widestRow(String wrapped, ToIntFunction<String> measure) {
        int widest = 0;
        for (String row : wrapped.split(NL, -1)) {
            widest = Math.max(widest, measure.applyAsInt(row));
        }
        return widest;
    }

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
        return wrapToWidth(text, maxPx, measure, Keep.NOTE);
    }

    /** @param keep 哪些東西不能拆到兩行，見 {@link Keep} */
    static String wrapToWidth(String text, int maxPx, ToIntFunction<String> measure,
                              Keep keep) {
        return wrapToWidth(text, maxPx, measure, keep, true);
    }

    /** @param reach 要不要為了把標點收進來讓一行超出寬度，見 {@link #clauseAhead} */
    private static String wrapToWidth(String text, int maxPx, ToIntFunction<String> measure,
                                      Keep keep, boolean reach) {
        if (maxPx <= 0) {
            return text;
        }
        List<List<int[]>> units = valueUnits(text, keep);
        StringBuilder out = new StringBuilder(text.length() + 8);
        int lineStart = 0;
        int width = 0;
        int lastSpace = -1;
        int lastClause = -1;               // 這一行裡最後一個標點的<b>後面</b>
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
                int clause = clauseBreak(text, lineStart, i, lastClause, maxPx, measure,
                                         reach);
                if (clause > lineStart) {
                    cut = clause;
                }
                cut = avoidOrphan(text, cut, lineStart);
                cut = keepUnitWhole(text, units, cut, lineStart);
                // 中文句子裡留著的英文名稱整串跟著走，見 keepLatinRunWhole。
                // 挪完之後名稱前面若是圖示，圖示也要一起到下一行。
                int run = keepLatinRunWhole(text, cut, lineStart);
                if (run != cut) {
                    cut = keepGlyphWithWord(text, run, lineStart);
                }
                out.append(text, lineStart, cut).append(NEWLINE);
                boolean atSpace = cut < text.length() && text.charAt(cut) == ' ';
                lineStart = atSpace ? cut + 1 : cut;
                lastSpace = -1;
                lastClause = -1;
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
                if (lineStart > i) {
                    // 斷點被<b>往前</b>挪到目前這一塊的後面（見 clauseAhead）。
                    // 那幾個字已經寫進上一行了，新的一行從斷點重新開始數，
                    // 不能再回頭量 substring(lineStart, i)——那是一段負長度。
                    width = 0;
                    i = lineStart;
                    continue;
                }
                width = measure.applyAsInt(text.substring(lineStart, i));
                // 斷點被往回挪過（見 keepUnitWhole、keepGlyphWithWord）時，新的一行
                // 已經吃進了幾個字，裡面的空白與標點要重新記。先前一律歸零，
                // 下一次斷行就找不到剛才那個空白，只好把單字切開：
                // 「+{~} к стихийном」換行「у урону」。
                for (int k = lineStart; k < i; k++) {
                    if (text.charAt(k) == ' ') {
                        lastSpace = k;
                    } else if (CLAUSE_END.indexOf(text.charAt(k)) >= 0) {
                        lastClause = k + 1;
                    }
                }
            }
            if (" ".equals(piece)) {
                lastSpace = i;
            }
            if (piece.length() == 1 && CLAUSE_END.indexOf(piece.charAt(0)) >= 0) {
                lastClause = end;              // 標點跟著上一行走，接縫在它後面
            }
            width += pieceWidth;
            i = end;
        }
        return out.append(text, lineStart, text.length()).toString();
    }

    /**
     * 中文在<b>標點</b>處斷，比在剛好塞滿的地方斷好讀。
     *
     * <h2>畫面上長什麼樣</h2>
     * 折行本來是量到哪斷到哪，而中文任何兩個字之間都可以斷，於是斷點常常
     * 落在詞的中間——使用者回報的 Major ID：
     *
     * <pre>
     *   眩目之光: 所有 奧法尼姆
     *   光球造成 250% 傷害，每次命中使
     *   你獲得 +5 層 結晶化，
     *   並改為環繞你運行。使用普攻時，所
     *   有光球的移動速度都會加快。
     * </pre>
     *
     * 「奧法尼姆／光球」被切開、「所／有光球」被切開。字都在，但要回頭讀一次
     * 才知道在講什麼。
     *
     * <p>改成優先斷在逗號、句號這些<b>語意的接縫</b>上：
     *
     * <pre>
     *   眩目之光: 所有 奧法尼姆 光球造成 250% 傷害，
     *   每次命中使你獲得 +5 層 結晶化，
     *   並改為環繞你運行。
     *   使用普攻時，所有光球的移動速度都會加快。
     * </pre>
     *
     * <h2>為什麼要有「至少多滿」這個條件</h2>
     * 不設條件的話，一行只填了三分之一也會在標點處斷，整段變成細細長長的
     * 一條——行數暴增，而行數超過原文時外層會把寬度放寬重折
     * （見 {@link #wrapBalanced}），等於白折一輪。
     *
     * <p>{@link #CLAUSE_FILL} 是那條界線：夠滿才值得為了好讀提早收尾。
     *
     * <h2>為什麼只認全形標點</h2>
     * 半形的 {@code ,} 與 {@code .} 在西班牙文、德文、俄文的譯文裡到處都是，
     * 認了它們就等於在那些語言裡改用「逗號折行」——而拉丁字母本來就靠空白
     * 斷，不需要這條。全形標點只有中日韓會用到。
     *
     * @param lastClause 這一行裡最後一個標點的後一個位置；沒有就是 -1
     * @return 該斷的位置；這一行不適合在標點處斷時回傳 {@code lineStart}
     */
    private static int clauseBreak(String text, int lineStart, int at, int lastClause,
                                   int maxPx, ToIntFunction<String> measure, boolean reach) {
        // reach 關掉時不往前找：那一步會讓這一行超出寬度，見 wrapBalanced
        int ahead = reach ? clauseAhead(text, lineStart, at, maxPx, measure) : lineStart;
        if (ahead > lineStart) {
            return ahead;
        }
        if (lastClause <= lineStart || lastClause >= at) {
            return lineStart;
        }
        int filled = measure.applyAsInt(text.substring(lineStart, lastClause));
        return filled * 100 >= maxPx * CLAUSE_FILL ? lastClause : lineStart;
    }

    /**
     * 標點<b>就在前面一點點</b>時，讓這一行多撐一下把它收進來。
     *
     * <h2>為什麼往前找也要找</h2>
     * 往回找標點只有在「這一行裡本來就有標點」時才有東西可斷。實機那條
     * 「眩目之光」的第一行整句都沒有標點，逗號剛好落在寬度限制的<b>後面</b>
     * 兩個字——往回找一無所獲，於是還是斷在「傷／害」中間。
     *
     * <p>目標寬度本來就是量原文量出來的估計值（見 {@link #wrapToBlock}），
     * 而寬一點只是面板寬一點，高度不會變；反過來，斷在詞中間是每一行都要
     * 回頭讀一次。所以寧可多撐 {@link #CLAUSE_REACH}%。
     *
     * <p>只找<b>一小段</b>：找太遠就變成整段不折了。
     */
    private static int clauseAhead(String text, int lineStart, int at,
                                   int maxPx, ToIntFunction<String> measure) {
        int limit = Math.min(text.length(), at + CLAUSE_LOOKAHEAD);
        for (int i = at; i < limit; i++) {
            if (CLAUSE_END.indexOf(text.charAt(i)) < 0) {
                continue;
            }
            int end = i + 1;
            if (end >= text.length()) {
                return lineStart;              // 收在句尾等於沒斷，只會多一個空行
            }
            int wide = measure.applyAsInt(text.substring(lineStart, end));
            return wide * 100 <= maxPx * (100 + CLAUSE_REACH) ? end : lineStart;
        }
        return lineStart;
    }

    /** 見 {@link #clauseBreak}：這些標點後面是一個乾淨的接縫。 */
    private static final String CLAUSE_END = "，。、；：！？》」』）】…";

    /** 見 {@link #clauseBreak}：在標點處收尾之前，這一行至少要有這麼滿（百分比）。 */
    private static final int CLAUSE_FILL = 60;

    /** 見 {@link #clauseAhead}：為了把標點收進來，這一行最多可以超出幾 %。 */
    private static final int CLAUSE_REACH = 20;

    /** 見 {@link #clauseAhead}：往前找標點最多找幾個字元。 */
    private static final int CLAUSE_LOOKAHEAD = 6;

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
        at = keepValueWithGlyph(text, at, lineStart);
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

    /**
     * 「數值 空白 圖示」是同一個東西，不能斷在數值與圖示中間。
     *
     * <h2>畫面上長什麼樣</h2>
     * Lootrun 賜福 Heavensent 的譯文是一整句「……每提供一個信標就 +{~1} {#}防禦 (上限 x{~2})」，
     * 折回原文的三行時斷點落在圖示前面：
     *
     * <pre>
     *   每提供一個信標就 +2
     *   ✤防禦 (上限 x15)        ← 屬性圖示跑到行首，跟 +2 分家
     * </pre>
     *
     * <p>原文的「+2 ✤Defence」是一組：數值屬於那個屬性，圖示是屬性名的前綴。
     * {@link #keepGlyphWithWord} 只管圖示不留在行尾，所以它讓圖示跟著後面的詞走——
     * 但前面那個數值被留在上一行。這裡把斷點再往前挪到（帶號的）數值前面，
     * 三樣東西一起到下一行。
     *
     * <p>只認「數值佔位符、空白、圖示」緊挨著的形狀。「使用 {#} 物品鑑定師」前面是字，
     * 不受影響；挪完整行會空掉時放棄，照原本的斷點。
     */
    private static int keepValueWithGlyph(String text, int cut, int lineStart) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        if (!text.startsWith(glyph, cut)) {
            return cut;
        }
        int back = cut;
        while (back > lineStart && text.charAt(back - 1) == ' ') {
            back--;
        }
        if (back == cut || back <= lineStart || text.charAt(back - 1) != '}') {
            return cut;                        // 圖示前面沒有空白，或空白前面不是佔位符
        }
        int open = text.lastIndexOf('{', back - 1);
        if (open < lineStart || !text.substring(open, back).matches("\\{~\\d*\\}")) {
            return cut;                        // 是地名、玩家名或另一個圖示，不是數值
        }
        if (open > lineStart && (text.charAt(open - 1) == '+' || text.charAt(open - 1) == '-')) {
            open--;                            // 正負號跟數值是同一個東西，見 wrapToWidth
        }
        return open > lineStart ? open : cut;
    }

    /**
     * 折行時哪些東西不能拆到兩行。由嚴到鬆排，{@link #wrapBalanced} 依序試，
     * 行數超過原文才退一級。
     *
     * <h2>畫面上長什麼樣</h2>
     * 玩家回報 Lootrun 賜福 Heavensent「排版有點怪」：
     *
     * <pre>
     *   本次 Lootrun 剩餘期間，
     *   每提供一個信標就 +4%
     *   元素傷害 (最多 x15)          ← 加的是什麼要到下一行才知道
     * </pre>
     *
     * 「+4% 元素傷害」是一件事，「(最多 x15)」是在補充它。折行只看寬度，而中文每個字
     * 都能斷，斷點就落在數值與屬性名中間，括號註解也跟著離開了它的數值。
     * {@link #keepValueWithGlyph} 只接得住「數值 圖示」那一種，這一條沒有圖示。
     */
    enum Keep {
        /** 數值＋屬性名，連同緊跟在後的短括號註解：「+4% 元素傷害 (最多 x15)」 */
        NOTE,
        /** 只保數值＋屬性名：「+4% 元素傷害」 */
        VALUE,
        /** 只看寬度的舊斷法。行數怎樣都超過原文時的最後手段 */
        NONE
    }

    /** 見 {@link #labelAfter}：中日文的屬性名最多黏幾個字。再長就是句子，不是名稱。 */
    private static final int LABEL_MAX = 6;

    /** 見 {@link #noteAfter}：括號註解最長幾個字元還算「短註解」。 */
    private static final int NOTE_MAX = 16;

    /**
     * 找出譯文裡「數值跟它的屬性名」那幾段，折行時不能從中間斷。
     *
     * <h2>怎麼認</h2>
     * <ul>
     *   <li><b>數值</b>：{@code {~N}}，或帶正負號、倍率 x、百分比的數字；後面可以黏
     *       {@code /5s}、{@code s} 這類單位。裸的數字不算——那多半只是句子裡的一個字。</li>
     *   <li><b>屬性名</b>：數值後面（隔一個空白、可以先有一個 {@code {#}} 圖示）的那個詞。
     *       中日文取連續的字，最多 {@link #LABEL_MAX} 個；拉丁與西里爾字母取一個單字，
     *       太短的（俄文的「к」）是介系詞，再多帶一個。</li>
     *   <li>後面沒有詞可黏時（「元素傷害 +{~}」、日文的「属性ダメージ +{~}」），黏<b>前面</b>
     *       那個詞——但前面那串太長就是句子不是名稱，不黏。</li>
     * </ul>
     *
     * @return 由外到內的幾層區間 {@code [start, end)}。{@link Keep#NOTE} 有兩層：
     *         連註解的、只有數值＋屬性名的。外層從行首開始就放不下時退到內層，
     *         見 {@link #keepUnitWhole}
     */
    static List<List<int[]>> valueUnits(String text, Keep keep) {
        if (keep == Keep.NONE) {
            return List.of();
        }
        List<int[]> values = new ArrayList<>();
        List<int[]> notes = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end = valueEnd(text, i);
            if (end <= i) {
                i++;
                continue;
            }
            int start = i;
            int stop = labelAfter(text, end);
            if (stop == end) {
                start = labelBefore(text, i);
            }
            values.add(new int[] {start, stop});
            notes.add(new int[] {start, noteAfter(text, stop)});
            i = end;
        }
        return keep == Keep.VALUE ? List.of(merged(values))
                                  : List.of(merged(notes), merged(values));
    }

    /** 重疊的區間併成一段。「+{~1} 元素傷害」與「(最多 x{~2})」連起來就是一整組。 */
    private static List<int[]> merged(List<int[]> spans) {
        spans.sort(java.util.Comparator.comparingInt(s -> s[0]));
        List<int[]> out = new ArrayList<>();
        for (int[] span : spans) {
            int[] last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && span[0] < last[1]) {
                last[1] = Math.max(last[1], span[1]);
            } else {
                out.add(new int[] {span[0], span[1]});
            }
        }
        return out;
    }

    /**
     * 斷點落在一組數值＋屬性名<b>裡面</b>時，挪到那一組的前面，整組到下一行。
     *
     * <p>那一組從行首開始就放不下的話，挪了也沒用（只會生出空行），改看內層那一組——
     * 「+4% 元素傷害 (最多 x15)」整組塞不下，至少「+4% 元素傷害」不要拆。
     */
    private static int keepUnitWhole(String text, List<List<int[]>> units, int cut,
                                     int lineStart) {
        for (List<int[]> layer : units) {
            for (int[] unit : layer) {
                if (unit[0] < cut && cut < unit[1]) {
                    if (unit[0] > lineStart) {
                        // 「使用 {#} +{~}」的圖示也不能被留在行尾
                        return keepGlyphWithWord(text, unit[0], lineStart);
                    }
                    break;
                }
            }
        }
        return cut;
    }

    /**
     * 中文句子裡留著的<b>英文名稱</b>（幾個單字連在一起）不能從中間斷開。
     *
     * <h2>畫面上長什麼樣</h2>
     * 語料刻意把技能名、人名留英文（詞表換不到的就一直是英文）。先前只保護
     * <b>一個</b>英文單字不被切成兩半（{@link #breaksWord}），單字之間的空白照樣是合法斷點：
     *
     * <pre>
     *   鮮血共鳴與 Eldritch
     *   Call 的傳送將會變得更高更遠。     ← 名字被拆成兩行，讀起來像兩個東西
     * </pre>
     *
     * <p>做法是斷點落在一串英文<b>中間的空白</b>上時，挪到那串英文的前面，整串到下一行。
     *
     * <h2>什麼時候不挪</h2>
     * <ul>
     *   <li>整句沒有中日韓文字——西、德、法、俄文的譯文每個字都是英文字母，
     *       整句會變成一串，那樣就不斷行了。</li>
     *   <li>那串英文從行首開始就放不下——挪了只會生出空行，只好照舊在空白上斷。</li>
     * </ul>
     *
     * @return 挪過的斷點；不必挪時原樣回傳
     */
    static int keepLatinRunWhole(String text, int cut, int lineStart) {
        if (cut <= lineStart || cut >= text.length() || !hasCjkLetter(text)) {
            return cut;
        }
        int left = cut;
        while (left > lineStart && text.charAt(left - 1) == ' ') {
            left--;
        }
        int right = cut;
        while (right < text.length() && text.charAt(right) == ' ') {
            right++;
        }
        // 兩邊都得是英數字，而且中間剛好隔一個空白。沒有空白是單字本身太長，交給 breaksWord；
        // 隔好幾個空白多半是排版用的間距，不是同一個名字。
        if (left <= lineStart || right >= text.length() || right - left != 1
                || !isWordChar(text.charAt(left - 1)) || !isWordChar(text.charAt(right))) {
            return cut;
        }
        int start = left;
        while (start > lineStart) {
            char c = text.charAt(start - 1);
            if (isWordChar(c)) {
                start--;
            } else if (c == ' ' && start - 2 >= lineStart && isWordChar(text.charAt(start - 2))) {
                start--;                        // 單一空白隔開的上一個字，還是同一串
            } else {
                break;
            }
        }
        if (start <= lineStart) {
            return cut;                         // 從行首開始就放不下
        }
        // 名稱前面那個空白是給英文用的，斷在它上面，不要留在行尾
        return text.charAt(start - 1) == ' ' && start - 1 > lineStart ? start - 1 : start;
    }

    /** 有沒有中日韓文字。見 {@link #keepLatinRunWhole}。 */
    private static boolean hasCjkLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (isCjkLetter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** 從 {@code i} 開始的數值到哪裡結束；這裡不是數值時回傳 -1。見 {@link #valueUnits}。 */
    private static int valueEnd(String text, int i) {
        int n = text.length();
        char c = text.charAt(i);
        boolean signed = false;
        int j = i;
        if (c == '+' || c == '-' || c == '−' || c == 'x' || c == '×') {
            // x 與 - 緊貼在字母數字後面的話，是單字的一部分（box、10-20），不是號
            if (c != '+' && i > 0 && isWordChar(text.charAt(i - 1))) {
                return -1;
            }
            signed = true;
            j++;
        }
        int k = numberEnd(text, j);
        if (k <= j) {
            return -1;
        }
        boolean placeholder = text.charAt(j) == '{';
        if (k < n && (text.charAt(k) == '%' || text.charAt(k) == '‰')) {
            k++;
            signed = true;                     // 「250%」本身就是一個量
        }
        if (!signed && !placeholder) {
            return -1;
        }
        if (k < n && text.charAt(k) == '/') {
            // 每單位時間：「/5s」「/s」
            int per = numberEnd(text, k + 1);
            int from = per > k + 1 ? per : k + 1;
            int letters = unitLetters(text, from);
            if (letters > k + 1) {
                k = letters;
            }
        }
        return unitLetters(text, k);          // 緊貼的單位：「3s」「{~}m」
    }

    /** {@code from} 開始的單位字母（見 {@link #unitLength}）到哪裡；不是單位就是 {@code from}。 */
    private static int unitLetters(String text, int from) {
        int k = from;
        while (k < text.length() && k - from < MAX_UNIT && isUnitLetter(text.charAt(k))) {
            k++;
        }
        return k < text.length() && Character.isLetter(text.charAt(k)) ? from : k;
    }

    /** {@code {~N}} 或阿拉伯數字（可以帶小數點、千分位）到哪裡；不是數字回傳 -1。 */
    private static int numberEnd(String text, int j) {
        int n = text.length();
        if (j >= n) {
            return -1;
        }
        if (text.startsWith("{~", j)) {
            int close = text.indexOf('}', j);
            if (close < 0) {
                return -1;
            }
            for (int d = j + 2; d < close; d++) {
                if (text.charAt(d) < '0' || text.charAt(d) > '9') {
                    return -1;                 // {~a} 不是數值佔位符
                }
            }
            return close + 1;
        }
        int k = j;
        while (k < n && text.charAt(k) >= '0' && text.charAt(k) <= '9') {
            k++;
            if (k + 1 < n && (text.charAt(k) == '.' || text.charAt(k) == ',')
                    && text.charAt(k + 1) >= '0' && text.charAt(k + 1) <= '9') {
                k++;
            }
        }
        return k > j ? k : -1;
    }

    /** 數值後面那個屬性名的結尾；後面沒有名稱時回傳 {@code end}（有圖示就是圖示之後）。 */
    private static int labelAfter(String text, int end) {
        int n = text.length();
        int j = end;
        if (j < n && text.charAt(j) == ' ') {
            j++;
        }
        int glyphEnd = -1;
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        if (text.startsWith(glyph, j)) {
            j += glyph.length();
            glyphEnd = j;
            if (j < n && text.charAt(j) == ' ') {
                j++;
            }
        }
        if (j < n && isCjkLetter(text.charAt(j))) {
            int k = j;
            while (k < n && k - j < LABEL_MAX && isCjkLetter(text.charAt(k))) {
                k++;
            }
            return k;
        }
        if (j < n && isLatinLetter(text.charAt(j))) {
            int k = wordEndAt(text, j);
            if (k - j <= 2 && k + 1 < n && text.charAt(k) == ' '
                    && isLatinLetter(text.charAt(k + 1))) {
                k = wordEndAt(text, k + 1);    // 「к стихийному」：介系詞後面才是名稱
            }
            return k;
        }
        return glyphEnd > 0 ? glyphEnd : end;
    }

    /**
     * 數值前面那個屬性名的起點；前面不是名稱時回傳 {@code start}。
     *
     * <p>名稱前面緊貼的左括號也算進來——「(最多 x15)」拆成「(」與「最多 x15)」的話，
     * 上一行就收在一個孤零零的左括號上。
     */
    private static int labelBefore(String text, int start) {
        int j = start;
        if (j > 0 && text.charAt(j - 1) == ' ') {
            j--;
        }
        int k = j;
        if (k > 0 && isCjkLetter(text.charAt(k - 1))) {
            while (k > 0 && isCjkLetter(text.charAt(k - 1))) {
                k--;
            }
            if (j - k > LABEL_MAX) {
                return start;                  // 「每提供一個信標就 +{~}」前面是整句話
            }
            String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
            if (k >= glyph.length() && text.startsWith(glyph, k - glyph.length())) {
                k -= glyph.length();           // 「{#}防御 +{~}」
            }
        } else if (k > 0 && isLatinLetter(text.charAt(k - 1))) {
            while (k > 0 && isWordChar(text.charAt(k - 1))) {
                k--;
            }
        } else {
            return start;
        }
        if (k > 0 && (text.charAt(k - 1) == '(' || text.charAt(k - 1) == '（')) {
            k--;
        }
        return k;
    }

    /** 緊跟在 {@code stop} 後面的短括號註解的結尾；沒有就回傳 {@code stop}。 */
    private static int noteAfter(String text, int stop) {
        int j = stop;
        if (j < text.length() && text.charAt(j) == ' ') {
            j++;
        }
        if (j >= text.length() || (text.charAt(j) != '(' && text.charAt(j) != '（')) {
            return stop;
        }
        int close = text.indexOf(text.charAt(j) == '(' ? ')' : '）', j);
        return close > j && close - j <= NOTE_MAX ? close + 1 : stop;
    }

    private static int wordEndAt(String text, int from) {
        int k = from;
        while (k < text.length() && isWordChar(text.charAt(k))) {
            k++;
        }
        return k;
    }

    /** 中日韓的「字」：漢字、假名（含長音符「ー」）、諺文。標點不算。 */
    private static boolean isCjkLetter(char c) {
        return c >= 0x2E80 && Character.isLetter(c);
    }

    /** 拉丁、希臘、西里爾字母。 */
    private static boolean isLatinLetter(char c) {
        return c < 0x2E80 && Character.isLetter(c);
    }

    /**
     * 不能出現在行首的字元。全形標點、收尾符號、百分比與單位。
     *
     * <p>半形的右括號也算：「(最多 x15」換行「)」是實際折得出來的形狀。
     */
    private static boolean cannotStartLine(char c) {
        return "，。、；：！？）」』】〉》%‰°′″…・)]".indexOf(c) >= 0;
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
            return keepClick(line, unslant(dropIconSpaces(whole)));
        }
        // 多行標籤（怪物名牌）整塊查不到時，逐行查——見 translatePerLine。
        Component perLine = translatePerLine(line, store, centered);
        return keepClick(line, unslant(dropIconSpaces(perLine != null ? perLine
                                       : translateSegments(line, store, centered, leftAligned))));
    }

    /**
     * 原文可以點，譯文卻點不動時的最後一道防線。
     *
     * <h2>實機回報</h2>
     * 「{@code Your ability tree is outdated, click here to update.}」裡的
     * {@code here} 掛著 {@code ClickEvent}，點下去才會更新。譯文整段是一塊，
     * 底線沒了、<b>也點不動</b>。
     *
     * <p>正規的做法是在譯文裡把可點的那幾個字標成 {@code {c2}}——{@code {cN}}
     * 搬的是原文那一段的<b>整個樣式物件</b>，{@code ClickEvent} 就住在裡面。
     * 但那要一條一條標，而遊戲隨時會新增這種訊息；沒標到的那些，玩家看到的是
     * 一句<b>沒有作用的</b>中文，比留著英文還糟。
     *
     * <p>所以這裡補一層：原文整行只有<b>一種</b> {@code ClickEvent}、而譯文
     * 一個都沒有時，把它套到整行上。整行可點跟原文只有那個詞可點不完全一樣，
     * 但點得到總比點不到好。
     *
     * <h2>為什麼要限定「只有一種」</h2>
     * 一行裡有兩個不同的連結（「接受 / 拒絕」那種）時，套哪一個都是錯的——
     * 錯的連結比沒有連結危險得多，那種情況一律不碰，交給 {@code {cN}}。
     */
    static Component keepClick(StyledText original, Component translated) {
        if (original == null || translated == null) {
            return translated;
        }
        ClickEvent only = soleClick(original.getComponent());
        if (only == null || hasClick(translated)) {
            return translated;
        }
        return translated.copy().withStyle(s -> s.withClickEvent(only));
    }

    /** 整行就這一種 {@link ClickEvent} 時回傳它；沒有或不只一種時回傳 {@code null}。 */
    private static ClickEvent soleClick(Component line) {
        java.util.List<ClickEvent> found = new java.util.ArrayList<>();
        line.visit((style, text) -> {
            ClickEvent click = style.getClickEvent();
            if (click != null && !found.contains(click)) {
                found.add(click);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found.size() == 1 ? found.get(0) : null;
    }

    private static boolean hasClick(Component line) {
        Boolean[] any = {Boolean.FALSE};
        line.visit((style, text) -> {
            if (style.getClickEvent() != null) {
                any[0] = Boolean.TRUE;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return any[0];
    }

    /**
     * 拿掉第一個實字之前、<b>只有空白而且套著圖示字型</b>的片段。
     *
     * <h2>實機</h2>
     * 屬性列原文是 {@code <圖示>[+2]'Agility '[+151]'+35'}——空白在標籤<b>後面</b>。
     * 翻出來畫的卻是 {@code <圖示>[+2]' '(tooltip/attribute/sprite)'敏捷'…}：那個空白
     * 跑到標籤前面，還沾上圖示的字型，「敏捷」「远程反伤」就被推開十幾像素
     * （tooltip-partial 記得很清楚）。原文在那個位置沒有任何東西，拿掉只會對回原文。
     *
     * <p>只動空白字元，排版偏移（私人使用區的字元）不是空白，不受影響；
     * 預設字型與 Wynncraft 文字字型裡的空白也照留——那是真的排版。
     */
    static Component dropIconSpaces(Component line) {
        if (line == null) {
            return null;
        }
        boolean[] dropped = {false};
        boolean[] seenText = {false};
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            if (text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (!seenText[0] && text.isBlank() && iconFont(style)) {
                dropped[0] = true;
                return java.util.Optional.empty();
            }
            if (text.codePoints().anyMatch(Character::isLetterOrDigit)
                    && !SpaceOffset.isSpaceFont(style) && !iconFont(style)) {
                seenText[0] = true;
            }
            out.append(Component.literal(text).withStyle(style));
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return dropped[0] ? out : line;
    }

    /** 圖示用的字型：不是預設、不是 Wynncraft 的文字字型，也不是排版偏移字型。 */
    private static boolean iconFont(Style style) {
        if (!(style.getFont() instanceof net.minecraft.network.chat.FontDescription.Resource r)
                || r.id() == null) {
            return false;
        }
        String path = r.id().getPath();
        return !path.equals("default") && !path.equals("uniform") && !path.equals("space")
                && !path.equals("language/wynncraft");
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
        // 第一輪查不到、但整段只有數值與範圍小字（「 to 」「-60 tier」）的片段。
        // 要等整行看完、確定有標籤翻成功了才動它們，見下面第二輪。
        List<Integer> connectorAt = new ArrayList<>();

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
            // 反過來也有：間隔掛在<b>後面</b>那一欄的片段開頭，跟著它的顏色。
            // 商城階級說明的「[間隔]★[間隔]Super Priority Queue」就是這樣，
            // 不拆出來的話這一列根本數不到第二欄，見 #splitOffsets。
            String lead = SpaceOffset.leadingOffsets(body);
            if (lead.length() == body.length() || !isAdjustableSpace(style, lead)) {
                lead = "";
            }
            body = body.substring(lead.length());
            if (!lead.isEmpty()) {
                pieces.add(Piece.space(SpaceOffset.decode(lead), SpaceOffset.styleFor(style)));
            }

            Component replaced = body.isEmpty()
                    ? null : translateOneSegment(body, style, store, percent);
            if (replaced == null) {
                if (!body.isEmpty() && isConnectorSegment(body)) {
                    connectorAt.add(pieces.size());
                }
                pieces.add(Piece.text(raw.substring(lead.length()), style));
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
            // 沒有任何標籤翻成功：句子裡零星的「to」不是範圍，整行保持原文。
            return null;
        }
        translateConnectors(pieces, connectorAt, store);

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

        // 靠左的清單不能拿「整行寬度」來收尾：那等於把最後一欄改成靠右，
        // 商城階級說明的「住宅特權」「商人攤位」就是這樣被推到最右邊的。
        List<Piece> columns = alignColumns(pieces, leftAligned);
        List<Piece> aligned = leftAligned ? columns : settle(columns, line);
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
     * <b>更一致</b>就是靠左排。
     *
     * <h2>「看不出來」就是「沒有欄」</h2>
     * 兩欄的行湊不滿 {@value #MIN_COLUMN_ROWS} 行時，這份 tooltip 根本
     * <b>沒有</b>靠右的數值欄——右緣對齊是一群行一起對出來的，一行自己對不出
     * 任何東西。先前這種情況回傳 {@code false}（＝當成靠右），於是那唯一一行
     * 也吃到「把譯文縮水的部分補回最後一段間隔」。
     *
     * <p>洞穴卡的獎勵列就是這樣歪的：
     *
     * <pre>
     *   - +1 [2px] Theatre Cane   →   - +1 [40px] 劇場手杖
     * </pre>
     *
     * 那個 2px 只是圖示與名稱之間的縫，不是欄距；譯名短了 38px，全部補進去，
     * 手杖就被推到行尾去了。整份卡片只有這一行有間隔，沒有第二行可以印證
     * 「右緣該在哪」——這種時候不補，比補錯好。
     */
    public static boolean columnsAreLeftAligned(List<StyledText> lines) {
        if (lines == null || lines.size() < MIN_COLUMN_ROWS) {
            return true;                       // 沒有欄可言，見上
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
            return true;                       // 同上
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
        // 要看「前面有沒有字」，不是 x > 0：行首的縮排常常是兩個偏移
        // （10px + 12px），第二個的 x 已經大於 0，會被誤認成欄界，
        // 第一欄就被當成第二欄量了。
        boolean sawText = false;
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
                if (px >= COLUMN_GAP_PX && start < 0 && sawText) {
                    sawGap = true;
                }
                x += px;
                continue;
            }
            if (sawGap && start < 0 && !raw.isBlank()) {
                start = x;
            }
            if (!raw.isBlank()) {
                sawText = true;
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
        // 原本就比 MIN_GAP 窄的（★ 旁邊 2～3px 的微調）不能被撐寬，
        // 不然沒有要補償的間隔也會自己變大。
        return original > 0 ? Math.max(Math.min(MIN_GAP, original), adjusted)
                            : Math.max(original, adjusted);
    }

    /**
     * 見 {@link #narrowed}。
     *
     * <p>本來是 4（一個半形空格）。技能樹的標籤改用半形冒號之後，冒號本身只有
     * 2px 寬、右邊也沒有全形字自帶的留白，4px 讀起來就像數值黏在冒號上。
     */
    static final int MIN_GAP = 6;

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
        if (measureForTest != null) {
            return measureForTest.applyAsInt(component);
        }
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.font == null ? 0 : mc.font.width(component);
    }

    /**
     * 測試用的量法；{@code null} 表示照常問字型。
     *
     * <p>headless 沒有字型，{@link #widthOf} 一律是 0——聊天那一整條對齊（欄界、
     * 置中、整行寬度）因此在測試裡全部退化成「什麼都不做」，只剩
     * {@link #chatColumnPad} 那一小段算式測得到。Lootrun 結算那種整行的回報
     * （欄位錯開、整行太寬被聊天折到下一行）得從 {@code translateChat} 一路量到底
     * 才看得出來。
     */
    static ToIntFunction<Component> measureForTest;

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
        // 只有 À 縮排的片段不是內容。
        //
        // À 是字母，躲得過上面那一關；而語料裡剛好有整行只剩縮排的條目
        // （raid.json 那幾條結尾的「À」），於是 lookup("ÀÀ") 查到「ÀÀ」——
        // 這一行就被算成「翻好了」，譯文卻跟原文一字不差。
        //
        // 算錯的代價在 TooltipPanel#evenOut：它靠「每一行有沒有翻到」判斷同一段
        // 是不是翻了一半。Fabled 物品的 Major ID 續行都是「ÀÀ說明…」，三行全被算成
        // 翻好了，只剩第一行自己換掉名稱的那一半沒被收回——畫面上就是
        // 「自由跑者: When your sprint」接三行英文。
        if (TranslationStore.indentOf(core) == core.length()) {
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
        if (leftAligned) {
            Component left = realignLeft(orig, made);
            if (left != null) {
                FlowedDebug.rows(original.getString(), "  靠左：每一欄的起點對回原文");
                return left;
            }
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

    /**
     * 靠左的兩欄：每一段文字的<b>起點</b>都要落回原文的位置。
     *
     * <h2>為什麼另外寫</h2>
     * 商城的階級說明（CHAMPION／HERO+／HERO）是一格一格的清單，第二欄全部
     * 從同一個 x 開始。原本那條路有兩個地方對不上它：
     *
     * <ul>
     *   <li>行首的縮排是<b>兩個</b>偏移（10px + 12px），重建後併成一個 22px，
     *       兩邊的間隔數對不上，整行原樣返回、完全沒補償；</li>
     *   <li>★ 前面的間隔只有 5～6px，不到欄界的門檻，「每日寶箱 第 2 階」
     *       縮短多少，後面的 ★ 就往左跑多少。</li>
     * </ul>
     *
     * 靠左的清單不需要分辨欄界：兩側都有東西的間隔，全部照原文的座標重算，
     * 前後的內容就一格一格對齊了。
     *
     * @return 對好的一行；兩邊的間隔數對不上時回傳 {@code null}，走原本的路
     */
    private static Component realignLeft(List<Run> orig, List<Run> made) {
        List<Run> o = mergeSpaces(orig);
        List<Run> m = mergeSpaces(made);
        int gaps = 0;
        for (Run r : o) {
            if (r.space()) {
                gaps++;
            }
        }
        int madeGaps = 0;
        for (Run r : m) {
            if (r.space()) {
                madeGaps++;
            }
        }
        if (gaps == 0 || gaps != madeGaps) {
            return null;
        }
        // 原文每個間隔「之後」的 x 座標
        int[] target = new int[gaps];
        int x = 0;
        int g = 0;
        for (Run r : o) {
            x += r.space() ? r.px() : widthOf(literal(r.text(), r.style()));
            if (r.space()) {
                target[g++] = x;
            }
        }
        MutableComponent out = Component.empty();
        x = 0;
        g = 0;
        boolean textBefore = false;
        for (Run r : m) {
            if (!r.space()) {
                out.append(literal(r.text(), r.style()));
                x += widthOf(literal(r.text(), r.style()));
                textBefore = true;
                continue;
            }
            int px = r.px();
            // 行首縮排、疊字用的負偏移都照原樣
            if (textBefore && px >= 0) {
                px = Math.max(Math.min(px, 1), target[g] - x);
            }
            g++;
            String encoded = SpaceOffset.encode(px);
            if (!encoded.isEmpty()) {
                out.append(literal(encoded, r.style()));
            }
            x += px;
        }
        return out;
    }

    /** 相鄰的偏移併成一段，寬度相加。 */
    static List<Run> mergeSpaces(List<Run> runs) {
        List<Run> out = new ArrayList<>(runs.size());
        for (Run r : runs) {
            if (r.space() && !out.isEmpty() && out.get(out.size() - 1).space()) {
                Run prev = out.remove(out.size() - 1);
                out.add(new Run(true, prev.px() + r.px(), prev.style(),
                        prev.text() + r.text()));
            } else {
                out.add(r);
            }
        }
        return out;
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
        return translateChat(message, store, centred, false);
    }

    /**
     * @param inPanel 呼叫端已經知道這一行屬於一塊「兩欄併排的面板」。
     *                見 {@link #chatPanel}
     */
    public static Component translateChat(StyledText message, TranslationStore store,
                                          Boolean centred, boolean inPanel) {
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
        List<LineParts.Piece> glyphs = null;
        if (translated == null || translated.isBlank()) {
            // 還是查不到：把斷好的行併成一句再查一次。見 #unwrapped。
            translated = store.lookupUnwrapped(parts.template());
            if (translated != null && !translated.isBlank()) {
                glyphs = unwrappedGlyphs(parts.template(), parts.glyphs());
            }
        }
        if (translated == null || translated.isBlank()) {
            FlowedDebug.chatRows(message.getString(), "  鍵：" + parts.template(),
                                 "語料裡查不到這一塊");
            return null;
        }
        Component rebuilt = glyphs == null
                ? rebuild(translated, parts, store)
                : rebuild(translated, parts, glyphs, store);
        if (rebuilt == null) {
            FlowedDebug.chatRows(message.getString(), "  譯文：" + translated,
                                 "佔位符數量對不上，整塊放棄");
            return null;
        }
        rebuilt = rowColours(message, rebuilt, dominantStyle(List.of(parts)));
        return unslant(realignChat(message, rebuilt, centred, inPanel));
    }

    /**
     * 多行訊息裡，<b>原文根本沒用到主色</b>的那幾行，改用那一行自己的顏色。
     *
     * <h2>實機回報</h2>
     * 登入的歡迎訊息底下有交易市場兩行：
     * <pre>
     *   §d§l1§r §5item was sold on the Trade Market
     *   §#8f663dand §#bc8f62§l2§#8f663d mounts have §#bc8f62no food§#8f663d in their feeder
     * </pre>
     * 整塊的主色是第一行的紫色，譯文每一行的正文都畫主色，
     * 於是第二行「另有 2 匹坐騎……」整行變紫，跟原文的棕色對不起來。
     *
     * <h2>為什麼只動「沒用到主色」的行</h2>
     * 一行裡只要出現過主色，譯文那一行畫主色就有根據；換成那一行最多的顏色，
     * 反而可能把一個很長的名字的顏色染到整行。只有主色在原文那一行
     * <b>一個字都沒有</b>時，畫主色才一定是錯的。
     *
     * @return 行數對不上或不需要動時原樣回傳
     */
    static Component rowColours(StyledText original, Component rebuilt, Style blockStyle) {
        TextColor block = blockStyle == null ? null : blockStyle.getColor();
        if (block == null) {
            return rebuilt;
        }
        List<List<Run>> origRows = splitRows(runs(original.getComponent()));
        List<List<Run>> madeRows = splitRows(runs(rebuilt));
        int[] keepOrig = solidRows(origRows);
        int[] keepMade = solidRows(madeRows);
        int count = keepOrig[1] - keepOrig[0];
        if (count < 2 || count != keepMade[1] - keepMade[0]) {
            return rebuilt;
        }
        TextColor[] rowColour = new TextColor[count];
        boolean any = false;
        for (int k = 0; k < count; k++) {
            java.util.Map<TextColor, Integer> weight = new java.util.LinkedHashMap<>();
            boolean usesBlock = false;
            for (Run run : origRows.get(keepOrig[0] + k)) {
                TextColor colour = run.style() == null ? null : run.style().getColor();
                if (run.space() || colour == null || !hasContent(run.text())) {
                    continue;
                }
                if (colour.equals(block)) {
                    usesBlock = true;
                    break;
                }
                weight.merge(colour, run.text().strip().length(), Integer::sum);
            }
            if (usesBlock || weight.isEmpty()) {
                continue;
            }
            rowColour[k] = java.util.Collections.max(weight.entrySet(),
                    java.util.Map.Entry.comparingByValue()).getKey();
            any = true;
        }
        if (!any) {
            return rebuilt;
        }
        MutableComponent out = Component.empty();
        for (int r = 0; r < madeRows.size(); r++) {
            if (r > 0) {
                out.append(Component.literal("\n"));
            }
            int k = r - keepMade[0];
            TextColor colour = k >= 0 && k < count ? rowColour[k] : null;
            for (Run run : madeRows.get(r)) {
                Style style = run.style();
                if (colour != null && !run.space() && style != null
                        && block.equals(style.getColor())) {
                    style = style.withColor(colour);
                }
                out.append(Component.literal(run.text()).withStyle(style));
            }
        }
        return out;
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
    /**
     * 漂浮字：先查漂浮字專用的譯法，查不到再走一般的 {@link #translate}。
     * 見 {@link TranslationStore#labelLookup}。
     */
    public static Component translateFloating(StyledText label, TranslationStore store) {
        LineParts parts = LineParts.of(label);
        String scoped = store.labelLookup(parts.template());
        if (scoped != null && !scoped.isBlank()) {
            Component rebuilt = rebuildLabel(label, scoped, parts, store);
            if (rebuilt != null) {
                return unslant(rebuilt);
            }
        }
        // 整塊查得到的漂浮字照 translateWholeLine 同一套走，只多一步把原文的分段顏色
        // 帶進譯文（見 #labelColours）。查不到才交給一般那條路，行為跟先前一樣。
        Component whole = floatingWhole(label, parts, store);
        if (whole != null) {
            return unslant(dropIconSpaces(whole));
        }
        return translate(label, store);
    }

    /** 見 {@link #translateFloating}：跟 {@link #translateWholeLine} 一樣，只是重建時帶著分段顏色。 */
    private static Component floatingWhole(StyledText label, LineParts parts,
                                           TranslationStore store) {
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        String translated = lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuildLabel(label, translated, parts, store);
        if (rebuilt == null) {
            return null;
        }
        Component result = realign(label, rebuilt, true, false);
        LineDebug.record(label, result);
        return result;
    }

    /** 漂浮字的重建：先照原文的分段插上顏色，插了反而重建不出來就退回沒插的。 */
    private static Component rebuildLabel(StyledText label, String translated,
                                          LineParts parts, TranslationStore store) {
        String coloured = labelColours(label, parts, translated, store);
        Component rebuilt = rebuild(coloured, parts, store);
        if (rebuilt == null && !coloured.equals(translated)) {
            rebuilt = rebuild(translated, parts, store);
        }
        return rebuilt;
    }

    public static Component translateLabel(StyledText label, TranslationStore store) {
        LineParts parts = LineParts.of(label);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        // 漂浮字專用的譯法優先，見 TranslationStore#labelLookup
        String translated = store.labelLookup(parts.template());
        if (translated == null || translated.isBlank()) {
            translated = lookup(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            translated = labelByLine(parts.template(), store);
        }
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuildLabel(label, translated, parts, store);
        return rebuilt == null ? null : unslant(rebuilt);
    }

    /**
     * 漂浮字的譯文，照原文<b>每一段的顏色</b>插上 {@code {cN}}。
     *
     * <h2>實機回報</h2>
     * 寶箱上的字「loot chest 上的飄浮字 格式與顏色與原文不同」：
     *
     * <pre>
     *   §dLocked §5Loot Chest [§d✫✫✫§8✫§5]
     *   §c§lSLAY! §7Defeat a §fGrume
     * </pre>
     *
     * 譯出來「上鎖的」是寶箱名的深紫、第二行「擊殺！擊敗 凝塊」整行一個灰。
     *
     * <h2>為什麼一般的上色補不回來</h2>
     * 一般的上色（{@link #rebuildAll} 的重點段）是拿原文的<b>字面</b>或它的
     * <b>單獨譯名</b>到譯文裡找。這塊牌子上的每一段都找不到：
     * <ul>
     *   <li>「Locked」「Defeat a」在語料裡沒有單獨的條目；</li>
     *   <li>「SLAY!」有，但譯文是「{@code {c2}}擊殺！」——帶著色碼，字面對不上；</li>
     *   <li>怪物名只以名牌的形狀存在（「Grume {#}{#}」），單獨查不到。</li>
     * </ul>
     * 沒有佔位符可以切段（見 {@link #segmentAccents}），整行只剩多數色。
     * 而語料裡寫死 {@code {cN}} 也不行：星星的分段隨寶箱等級變（一級 1+3、三級 3+1、
     * 四級 4+0），{@code {cN}} 的編號跟著變。
     *
     * <h2>做法</h2>
     * 漂浮字是遊戲整塊排好的，<b>一行對一行</b>。行數相同時，每一行把原文的顏色段
     * 對到譯文上，錨點由可靠到不可靠：
     * <ol>
     *   <li>原文某一段<b>連同後面（或前面）幾段</b>整串查得到，而且那串的譯文剛好是
     *       譯文這一行的結尾（或開頭）——「Loot Chest [✫✫✫✫]」就是一條現成的條目，
     *       於是「上鎖的」一定是「Locked」。</li>
     *   <li>某一段的字面原樣出現在譯文裡（星星、括號、留英文的名字），
     *       或它自己的譯名出現在譯文裡。</li>
     *   <li>剩下夾在錨點之間、而且只有一段原文的那截譯文，就是那一段的。</li>
     *   <li>夾著好幾段時，譯文用空白分成<b>同樣多塊</b>才照順序配（「擊敗 凝塊」），
     *       否則不猜。</li>
     * </ol>
     * 對上的地方插 {@code {cN}}，對不上的地方插 {@code {/}} 交回一般的上色。
     * 數值、符號這些佔位符本來就帶著自己的樣式填回去，不受 {@code {cN}} 影響。
     *
     * <h2>何時不做</h2>
     * 譯者自己寫了色碼（尊重譯者）、行數對不上、這一行原文只有一個顏色、
     * 顏色超過九種（{@code {cN}} 只有一位數）。
     *
     * @return 插好顏色的譯文；不適用時原樣回傳
     */
    static String labelColours(StyledText label, LineParts parts, String translated,
                               TranslationStore store) {
        if (label == null || translated == null || store == null
                || LABEL_COLOUR_TOKEN.matcher(translated).find()) {
            return translated;
        }
        List<List<LabelRun>> rows = labelRows(label);
        String[] dst = translated.split(NL, -1);
        if (rows.size() != dst.length) {
            return translated;
        }
        List<Style> palette = palette(parts.runs());
        StringBuilder out = new StringBuilder(translated.length() + 16);
        boolean any = false;
        for (int i = 0; i < dst.length; i++) {
            if (i > 0) {
                out.append(NL);
            }
            List<LabelRun> row = rows.get(i);
            if (distinctStyles(row) < 2) {
                out.append(dst[i]);
                continue;
            }
            List<int[]> regions = new ArrayList<>();
            List<Style> styles = new ArrayList<>();
            alignLabel(row, 0, row.size(), dst[i], 0, dst[i].length(), regions, styles, store);
            String coloured = paint(dst[i], regions, styles, palette);
            any |= !coloured.equals(dst[i]);
            out.append(coloured);
        }
        return any ? out.toString() : translated;
    }

    /** 譯者已經寫了的色碼：{@code {c1}}、{@code {c:#hex}}、{@code {w1}}、{@code {/}}。 */
    private static final java.util.regex.Pattern LABEL_COLOUR_TOKEN =
            java.util.regex.Pattern.compile("\\{(?:c[^}]*|w\\d|/)}");

    /** 原文一行裡的一段：文字（含它後面的空白）與它的樣式。 */
    private record LabelRun(String text, Style style) {}

    /**
     * 原文照換行切成一行一行，每一行是依顏色切開的幾段。
     *
     * <p>直接走 StyledText 的片段、用跟 {@link LineParts#of} 同一個樣式物件——
     * {@code {cN}} 指的是 {@link #palette} 裡的第幾個，樣式物件不同就對不上編號。
     * {@code LineParts} 的 runs 不能用：純換行的片段被它丟掉了，看不出行在哪裡斷。
     * 圖示片段不算（譯文裡是 {@code {#}}，連同自己的樣式填回去）。
     */
    private static List<List<LabelRun>> labelRows(StyledText label) {
        List<List<LabelRun>> rows = new ArrayList<>();
        List<LabelRun> row = new ArrayList<>();
        for (StyledTextPart part : label) {
            if (GlyphSplitter.isGlyphPart(part)) {
                continue;
            }
            String raw = part.getString(null, StyleType.NONE);
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();
            String[] pieces = raw.split(NL, -1);
            for (int p = 0; p < pieces.length; p++) {
                if (p > 0) {
                    rows.add(row);
                    row = new ArrayList<>();
                }
                String text = GlyphSplitter.stripGlyphChars(pieces[p]);
                if (text.isEmpty()) {
                    continue;
                }
                LabelRun last = row.isEmpty() ? null : row.get(row.size() - 1);
                if (last != null && (!hasContent(text)
                        || java.util.Objects.equals(last.style(), style))) {
                    // 純空白黏到前一段；同色的相鄰片段併成一段
                    row.set(row.size() - 1, new LabelRun(last.text() + text, last.style()));
                } else if (hasContent(text)) {
                    row.add(new LabelRun(text, style));
                }
            }
        }
        rows.add(row);
        return rows;
    }

    private static int distinctStyles(List<LabelRun> row) {
        java.util.Set<Style> seen = new java.util.HashSet<>();
        for (LabelRun run : row) {
            seen.add(run.style());
        }
        return seen.size();
    }

    /**
     * 把原文 {@code runs[lo, hi)} 對到譯文 {@code dst[x, y)}，對上的區間記進 {@code regions}。
     * 錨點的先後見 {@link #labelColours}。
     */
    private static void alignLabel(List<LabelRun> runs, int lo, int hi, String dst, int x, int y,
                                   List<int[]> regions, List<Style> styles,
                                   TranslationStore store) {
        while (x < y && Character.isWhitespace(dst.charAt(x))) {
            x++;
        }
        while (y > x && Character.isWhitespace(dst.charAt(y - 1))) {
            y--;
        }
        if (lo >= hi || x >= y) {
            return;
        }
        if (hi - lo == 1) {
            regions.add(new int[] {x, y});
            styles.add(runs.get(lo).style());
            return;
        }
        String window = dst.substring(x, y);
        // 1. 後面幾段（或前面幾段）整串查得到，而且剛好是這一截的結尾（或開頭）
        for (int k = lo + 1; k < hi; k++) {
            String hit = labelLookup(joined(runs, k, hi), store);
            if (hit != null && hit.length() < window.length() && window.endsWith(hit)) {
                int cut = y - hit.length();
                alignLabel(runs, lo, k, dst, x, cut, regions, styles, store);
                alignLabel(runs, k, hi, dst, cut, y, regions, styles, store);
                return;
            }
        }
        for (int k = hi - 1; k > lo; k--) {
            String hit = labelLookup(joined(runs, lo, k), store);
            if (hit != null && hit.length() < window.length() && window.startsWith(hit)) {
                int cut = x + hit.length();
                alignLabel(runs, lo, k, dst, x, cut, regions, styles, store);
                alignLabel(runs, k, hi, dst, cut, y, regions, styles, store);
                return;
            }
        }
        // 2. 單獨一段：字面原樣出現，或它的譯名出現——照順序往後找，不回頭
        List<int[]> anchors = new ArrayList<>();       // {第幾段, 起, 訖}
        int cursor = x;
        for (int j = lo; j < hi; j++) {
            int[] at = findRun(runs.get(j).text().strip(), dst, cursor, y, store);
            if (at != null) {
                anchors.add(new int[] {j, at[0], at[1]});
                cursor = at[1];
            }
        }
        if (anchors.isEmpty()) {
            // 4. 沒有錨點：空白切出來的塊數跟段數一樣才照順序配
            List<int[]> chunks = new ArrayList<>();
            int i = x;
            while (i < y) {
                while (i < y && Character.isWhitespace(dst.charAt(i))) {
                    i++;
                }
                int start = i;
                while (i < y && !Character.isWhitespace(dst.charAt(i))) {
                    i++;
                }
                if (i > start) {
                    chunks.add(new int[] {start, i});
                }
            }
            if (chunks.size() == hi - lo) {
                for (int k = 0; k < chunks.size(); k++) {
                    regions.add(chunks.get(k));
                    styles.add(runs.get(lo + k).style());
                }
            }
            return;
        }
        // 3. 錨點之間的空檔遞迴下去：只夾一段原文的，整截就是那一段
        int prevRun = lo;
        int prevEnd = x;
        for (int[] anchor : anchors) {
            alignLabel(runs, prevRun, anchor[0], dst, prevEnd, anchor[1], regions, styles, store);
            regions.add(new int[] {anchor[1], anchor[2]});
            styles.add(runs.get(anchor[0]).style());
            prevRun = anchor[0] + 1;
            prevEnd = anchor[2];
        }
        alignLabel(runs, prevRun, hi, dst, prevEnd, y, regions, styles, store);
    }

    private static String joined(List<LabelRun> runs, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(runs.get(i).text());
        }
        return sb.toString().strip();
    }

    /**
     * 原文一段在譯文 {@code dst[from, to)} 裡的位置：先找字面，再找它的譯名。
     *
     * <p>有字母的字面要<b>自成一個詞</b>才算——「a」不能算在「Grook」裡面。
     *
     * @return {@code {起, 訖}}；找不到回傳 {@code null}
     */
    private static int[] findRun(String core, String dst, int from, int to,
                                 TranslationStore store) {
        if (!hasContent(core)) {
            return null;
        }
        int at = dst.indexOf(core, from);
        while (at >= 0 && at + core.length() <= to) {
            boolean letters = core.codePoints().anyMatch(Character::isLetter);
            int after = at + core.length();
            boolean whole = !letters
                    || ((at == 0 || !isWordChar(dst.charAt(at - 1)))
                        && (after >= dst.length() || !isWordChar(dst.charAt(after))));
            if (whole) {
                return new int[] {at, after};
            }
            at = dst.indexOf(core, at + 1);
        }
        String hit = labelLookup(core, store);
        if (hit != null) {
            int found = dst.indexOf(hit, from);
            if (found >= 0 && found + hit.length() <= to) {
                return new int[] {found, found + hit.length()};
            }
        }
        return null;
    }

    /**
     * 一段原文（或幾段接起來）的譯名，整理成可以在譯文裡找字面的樣子。
     *
     * <p>先照一般的鍵查，再查詞表、剝掉頭尾符號查；都查不到再用<b>名牌的形狀</b>查
     * （「Grume {#}{#}」——怪物名在語料裡只以名牌存在，後面兩個是等級膠囊的圖示，
     * boss bar 也是這樣查的，見 {@code WynntilsText#bossBarName}）。
     * 譯文裡的色碼拿掉（「{@code {c2}}擊殺！」只留「擊殺！」），名牌尾巴的圖示也拿掉。
     *
     * @return 整理好的譯名；查不到或沒有實字時回傳 {@code null}
     */
    private static String labelLookup(String text, TranslationStore store) {
        if (!GlyphSplitter.hasLetter(text)) {
            return null;
        }
        String template = GlyphSplitter.toTemplate(StyledText.fromString(text));
        String hit = lookup(template, store);
        if (hit == null || hit.isBlank()) {
            hit = store.lookupTerm(template);
        }
        if (hit == null || hit.isBlank()) {
            hit = lookupWordCore(template, store);
        }
        if (hit == null || hit.isBlank()) {
            String plate = store.lookup(template + " " + GlyphSplitter.GLYPH_PLACEHOLDER
                                        + GlyphSplitter.GLYPH_PLACEHOLDER);
            String tail = GlyphSplitter.GLYPH_PLACEHOLDER + GlyphSplitter.GLYPH_PLACEHOLDER;
            if (plate != null && plate.strip().endsWith(tail)) {
                hit = plate.strip();
                hit = hit.substring(0, hit.length() - tail.length());
            }
        }
        if (hit == null) {
            return null;
        }
        hit = LABEL_COLOUR_TOKEN.matcher(hit).replaceAll("").strip();
        return hasContent(hit) ? hit : null;
    }

    /**
     * 照對好的區間在譯文這一行插上 {@code {cN}}；區間之外插 {@code {/}}，
     * 那一截交回一般的上色。行尾一定收掉，色碼不會染到下一行。
     */
    private static String paint(String row, List<int[]> regions, List<Style> styles,
                                List<Style> palette) {
        Integer[] order = new Integer[regions.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, java.util.Comparator.comparingInt(i -> regions.get(i)[0]));
        StringBuilder out = new StringBuilder(row.length() + 16);
        int at = 0;
        int current = 0;                               // 0 表示沒有強制顏色
        for (int i : order) {
            int[] region = regions.get(i);
            int slot = palette.indexOf(styles.get(i)) + 1;
            if (region[0] < at || slot < 1 || slot > 9) {
                continue;                              // 重疊或編號寫不出來，不貼
            }
            if (region[0] > at) {
                String gap = row.substring(at, region[0]);
                if (current != 0 && hasContent(gap)) {
                    out.append(COLOR_END);
                    current = 0;
                }
                out.append(gap);
            }
            if (slot != current) {
                out.append("{c").append(slot).append('}');
                current = slot;
            }
            out.append(row, region[0], region[1]);
            at = region[1];
        }
        if (at < row.length()) {
            String rest = row.substring(at);
            if (current != 0 && hasContent(rest)) {
                out.append(COLOR_END);
                current = 0;
            }
            out.append(rest);
        }
        if (current != 0) {
            out.append(COLOR_END);
        }
        return out.toString();
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
        boolean[] centred = BlockLayout.centered(lines);
        // 一行一則送來的（「[Objective Completed]」那三行）也要看空白墊出來的置中，
        // 跟整則送來時走 realignChat 的判斷一致。見 #spacePadded。
        List<List<Run>> runRows = new ArrayList<>(rows.size());
        for (StyledText row : rows) {
            List<List<Run>> split = splitRows(runs(row.getComponent()));
            if (split.size() != 1) {
                return centred;                // 一則裡有好幾行，對不上逐行的索引
            }
            runRows.add(split.get(0));
        }
        sharedCentre(runRows, centred);
        spacePadded(runRows, centred);
        sameLeadList(runRows, centred);
        return centred;
    }

    /**
     * 這一塊聊天訊息是不是「兩欄併排的面板」。整塊逐行查表時先算好再傳進來。
     *
     * <h2>為什麼不能一行一行問</h2>
     * 獵殺信標的面板是<b>一行一則訊息</b>送過來的。一則裡只有一行時，
     * {@link #columnPanel} 看不到旁邊那幾行，只好說「不是面板」——
     * 於是同一塊裡的多欄行照欄置中、單欄的接續行卻靠左不動，兩者就錯開。
     *
     * <p>實機回報「Lootrun 有時候不對齊」就是這個：紅色信標那一欄往右移了
     * 16px（中文短了一半的補償），而它折下來的
     * 「{@code no Time Bonus for}」「{@code completing them.}」原地不動。
     *
     * <p>{@link #chatCentred} 也接不住這種行：它問的是「這一行在<b>整塊</b>裡
     * 置不置中」，而欄位的接續行是置中在<b>自己那一欄</b>上，不是整塊。
     */
    public static boolean chatPanel(List<StyledText> rows) {
        for (StyledText row : rows) {
            for (List<Run> line : splitRows(runs(row.getComponent()))) {
                if (spacedColumns(line) >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 這一行有幾段實字被<b>正的排版偏移</b>隔開。
     *
     * <p>跟 {@link #columns} 問的是同一件事，但<b>不看字寬</b>——偏移的寬度是
     * 從碼位解出來的，不必問字型。{@link #chatPanel} 用這一支，
     * 因為判斷「是不是面板」不需要知道每一欄多寬，而且測試環境沒有字型。
     */
    private static int spacedColumns(List<Run> row) {
        int columns = 0;
        boolean text = false;
        for (Run run : row) {
            if (run.space()) {
                if (text && run.px() > 0) {
                    columns++;                 // 一段實字結束在一個間隔上
                    text = false;
                }
            } else if (hasContent(run.text())) {
                text = true;
            }
        }
        return text ? columns + 1 : columns;
    }

    private static Component realignChat(StyledText original, Component rebuilt,
                                         Boolean centred) {
        return realignChat(original, rebuilt, centred, false);
    }

    private static Component realignChat(StyledText original, Component rebuilt,
                                         Boolean centred, boolean inPanel) {
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
        if (centre != null) {
            sharedCentre(origRows, centre);
            spacePadded(origRows, centre);
            sameLeadList(origRows, centre);
        }
        // 這一塊是不是「兩欄併排的面板」。見 #columnPanel。
        //
        // 呼叫端說了算優先：信標面板是一行一則訊息送來的，這裡看到的
        // origRows 只有那一行，自己判斷永遠是 false。見 #chatPanel。
        boolean panel = inPanel || columnPanel(origRows);
        // 中文、日文：照英文的斷行位置斷會斷在句子中間，整段接起來重新斷。見 #reflowCjk。
        boolean anyCentre = Boolean.TRUE.equals(centred);
        if (centre != null) {
            for (boolean c : centre) {
                anyCentre |= c;
            }
        }
        if (!panel && !anyCentre) {
            Component flowed = reflowCjk(origRows, madeRows);
            if (flowed != null) {
                FlowedDebug.chatRows(original.getString(), "  中日文重新斷行", null);
                return flowed;
            }
        }
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

    /**
     * 中文、日文的多行聊天訊息：整段接起來，照英文最寬那一行的寬度重新斷行。
     *
     * <h2>實機回報</h2>
     * <pre>
     *   傳送門湧出充滿憎恨的回音。Wynn 正面臨
     *   湮滅。
     * </pre>
     * 譯文照英文的換行位置斷（{@code Wynn faces\nAnnihilation.}），中文短得多，
     * 第一行沒滿就斷，「正面臨／湮滅」被拆在兩行。這種譯文在語料裡成千上萬條，
     * 一條一條改不完，所以在畫的時候重新斷。
     *
     * <h2>怎麼斷</h2>
     * <ul>
     *   <li>每行的行首符號（訊息圖示、續行縮排）照留：第一行用第一行的，
     *       其餘用第二行的</li>
     *   <li>寬度上限是英文內容最寬那一行</li>
     *   <li>優先斷在「，。！？」之後；找不到就斷在兩個漢字、假名之間；
     *       英文單字、數字、韓文單字中間不斷；標點不放在行首</li>
     * </ul>
     *
     * <h2>什麼時候不動</h2>
     * 只有英文本來就是「一段話折成幾行」時才做：除了最後一行，每一行都要接近
     * 最寬那一行（清單式的短行不算），而且譯文裡要有漢字或假名。行中間有排版
     * 空白（分欄）、首尾有空行的都不碰。
     *
     * @return 重新斷好的整段；不該動時回傳 {@code null}
     */
    static Component reflowCjk(List<List<Run>> origRows, List<List<Run>> madeRows) {
        int n = madeRows.size();
        if (n < 2 || origRows.size() != n) {
            return null;
        }
        int[] origBody = new int[n];
        int max = 0;
        for (int i = 0; i < n; i++) {
            List<Run> row = origRows.get(i);
            int lead = prefixEnd(row);
            if (lead < 0) {
                return null;
            }
            origBody[i] = runsWidth(row.subList(lead, row.size()));
            max = Math.max(max, origBody[i]);
        }
        if (max <= 0) {
            return null;
        }
        for (int i = 0; i < n - 1; i++) {
            if (origBody[i] * 10 < max * 6) {
                return null;                   // 短行：清單或刻意分行，不是折行
            }
        }
        List<List<Run>> prefixes = new ArrayList<>(n);
        List<Run> body = new ArrayList<>();
        boolean cjk = false;
        for (int i = 0; i < n; i++) {
            List<Run> row = madeRows.get(i);
            int lead = prefixEnd(row);
            if (lead < 0) {
                return null;
            }
            prefixes.add(row.subList(0, lead));
            List<Run> content = row.subList(lead, row.size());
            for (Run r : content) {
                if (r.space()) {
                    return null;               // 行中間有排版空白：分欄，不碰
                }
                cjk |= r.text().codePoints().anyMatch(LineTranslator::isCjkBreakable);
            }
            appendJoined(body, content);
        }
        if (!cjk || body.isEmpty()) {
            return null;
        }
        List<List<Run>> lines = wrapRuns(body, max);
        MutableComponent out = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(NL));
            }
            List<Run> prefix = prefixes.get(Math.min(i, 1));
            for (Run r : prefix) {
                out.append(literal(r.space() ? SpaceOffset.encode(r.px()) : r.text(), r.style()));
            }
            for (Run r : lines.get(i)) {
                out.append(literal(r.text(), r.style()));
            }
        }
        return out;
    }

    /** 行首符號（圖示、縮排、空白）到哪裡為止；整行都是符號時回傳 {@code -1}。 */
    private static int prefixEnd(List<Run> row) {
        for (int i = 0; i < row.size(); i++) {
            Run r = row.get(i);
            if (r.space()) {
                continue;
            }
            if (r.text().codePoints().anyMatch(cp -> Character.isLetterOrDigit(cp)
                    && !GlyphSplitter.isGlyphCodePoint(cp))) {
                // 同一段裡前面的空白一起算進行首
                return i;
            }
        }
        return -1;
    }

    private static int runsWidth(List<Run> runs) {
        int w = 0;
        for (Run r : runs) {
            w += r.space() ? r.px() : widthOf(literal(r.text(), r.style()));
        }
        return w;
    }

    /** 把一行內容接到整段後面：去掉接縫的空白，兩邊都是英數時留一個空格。 */
    private static void appendJoined(List<Run> body, List<Run> content) {
        List<Run> trimmed = new ArrayList<>(content);
        while (!trimmed.isEmpty()) {
            Run first = trimmed.get(0);
            String t = first.text().stripLeading();
            if (!t.isEmpty()) {
                trimmed.set(0, new Run(false, 0, first.style(), t));
                break;
            }
            trimmed.remove(0);
        }
        if (trimmed.isEmpty()) {
            return;
        }
        if (!body.isEmpty()) {
            Run last = body.get(body.size() - 1);
            String t = last.text().stripTrailing();
            body.set(body.size() - 1, new Run(false, 0, last.style(), t));
            int a = t.isEmpty() ? ' ' : t.codePointBefore(t.length());
            int b = trimmed.get(0).text().codePointAt(0);
            if (isWordChar(a) && isWordChar(b)) {
                body.add(new Run(false, 0, last.style(), " "));
            }
        }
        body.addAll(trimmed);
    }

    /** 英數、韓文：單字中間不能斷，接縫要補空格。 */
    private static boolean isWordChar(int cp) {
        return (Character.isLetterOrDigit(cp) && !isCjkBreakable(cp))
                || "%+-/.,".indexOf(cp) >= 0 && cp != ',';
    }

    /** 漢字與假名：任兩個之間都可以斷。韓文不算（韓文以空白分詞）。 */
    static boolean isCjkBreakable(int cp) {
        Character.UnicodeScript s = Character.UnicodeScript.of(cp);
        return s == Character.UnicodeScript.HAN || s == Character.UnicodeScript.HIRAGANA
                || s == Character.UnicodeScript.KATAKANA;
    }

    private static final String BREAK_AFTER = "，。！？；：、）」』…,.!?;:";
    private static final String NO_LINE_START = "，。！？；：、）」』…ー,.!?;:%）)";

    /** 照寬度上限斷行，回傳每一行的段落。 */
    private static List<List<Run>> wrapRuns(List<Run> body, int max) {
        // 攤成一個字一格
        List<int[]> cps = new ArrayList<>();          // {codepoint, run index}
        for (int k = 0; k < body.size(); k++) {
            int kk = k;
            body.get(k).text().codePoints().forEach(cp -> cps.add(new int[] {cp, kk}));
        }
        int[] w = new int[cps.size()];
        for (int i = 0; i < cps.size(); i++) {
            w[i] = widthOf(literal(new String(Character.toChars(cps.get(i)[0])),
                    body.get(cps.get(i)[1]).style()));
        }
        List<int[]> ranges = new ArrayList<>();       // [from, to)
        int start = 0;
        while (start < cps.size()) {
            int x = 0;
            int end = start;
            while (end < cps.size() && x + w[end] <= max) {
                x += w[end];
                end++;
            }
            if (end >= cps.size()) {
                ranges.add(new int[] {start, cps.size()});
                break;
            }
            // 往回找斷點：先找標點之後（不短於一半），再找可斷的字間
            int cut = -1;
            int acc = x;
            for (int i = end; i > start; i--) {
                if (canBreakBefore(cps, i) && BREAK_AFTER.indexOf(cps.get(i - 1)[0]) >= 0
                        && acc * 2 >= max) {
                    cut = i;
                    break;
                }
                acc -= w[i - 1];
            }
            if (cut < 0) {
                for (int i = end; i > start; i--) {
                    if (canBreakBefore(cps, i)) {
                        cut = i;
                        break;
                    }
                }
            }
            if (cut <= start) {
                cut = Math.max(end, start + 1);        // 斷不了就硬斷
            }
            ranges.add(new int[] {start, cut});
            start = cut;
            while (start < cps.size() && cps.get(start)[0] == ' ') {
                start++;                               // 行首的空格吃掉
            }
        }
        List<List<Run>> out = new ArrayList<>();
        for (int[] r : ranges) {
            List<Run> line = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            int run = -1;
            for (int i = r[0]; i < r[1]; i++) {
                int ri = cps.get(i)[1];
                if (ri != run && sb.length() > 0) {
                    line.add(new Run(false, 0, body.get(run).style(), sb.toString()));
                    sb.setLength(0);
                }
                run = ri;
                sb.appendCodePoint(cps.get(i)[0]);
            }
            if (sb.length() > 0) {
                String t = sb.toString().stripTrailing();
                if (!t.isEmpty()) {
                    line.add(new Run(false, 0, body.get(run).style(), t));
                }
            }
            out.add(line);
        }
        return out;
    }

    /** 第 {@code i} 個字前面可以斷嗎。 */
    private static boolean canBreakBefore(List<int[]> cps, int i) {
        int a = cps.get(i - 1)[0];
        int b = cps.get(i)[0];
        if (NO_LINE_START.indexOf(b) >= 0) {
            return false;
        }
        if (a == ' ' || b == ' ') {
            return true;
        }
        if (BREAK_AFTER.indexOf(a) >= 0 && !isWordChar(b)) {
            return true;
        }
        if (BREAK_AFTER.indexOf(a) >= 0 && (a > 0x2FFF)) {
            return true;                               // 全形標點之後什麼都能接
        }
        return isCjkBreakable(a) || isCjkBreakable(b)
                ? !(isWordChar(a) && isWordChar(b)) : false;
    }

    /**
     * 聊天裡用空白墊出來的置中：幾行都有縮排、縮排長短不一，中心卻落在差不多的位置。
     *
     * <h2>實機回報</h2>
     * <pre>
     *            Enjoying Wynncraft?
     *    Recruit a friend and both of you will get rewards!
     *           Click here to recruit
     *            (or type /recruit)
     * </pre>
     * 伺服器用空白把每一行墊到聊天視窗中間，但寬度是它自己估的，四行的中心
     * 落在 130～151px 之間，不是同一條線。{@link BlockLayout#centered} 拿最寬那行
     * 當基準，那一行自己也有縮排，於是整塊被判成靠左，中文變短後就往左偏。
     *
     * <p>這裡另外看：有縮排的行至少兩行、縮排差了一截（不是清單那種同一個縮排），
     * 而且每一行的中心都在中位數 ±16px 以內，就把那幾行當成置中。
     */
    private static void sharedCentre(List<List<Run>> rows, boolean[] centre) {
        List<Integer> idx = new ArrayList<>();
        List<Integer> mids = new ArrayList<>();
        int minLead = Integer.MAX_VALUE;
        int maxLead = 0;
        for (int i = 0; i < rows.size(); i++) {
            List<Run> row = rows.get(i);
            int lead = leadWidth(row);
            int body = rowWidth(row) - lead;
            if (lead <= 0 || body <= 0) {
                continue;
            }
            if (columns(chatSegmentWidths(row, LineTranslator::runWidth)) >= 2) {
                return;                                // 分欄的面板另有規則
            }
            idx.add(i);
            mids.add(lead + body / 2);
            minLead = Math.min(minLead, lead);
            maxLead = Math.max(maxLead, lead);
        }
        if (idx.size() < 2 || maxLead - minLead <= 8) {
            return;
        }
        List<Integer> sorted = new ArrayList<>(mids);
        java.util.Collections.sort(sorted);
        int median = sorted.get(sorted.size() / 2);
        for (int m : mids) {
            if (Math.abs(m - median) > 16) {
                return;
            }
        }
        for (int i : idx) {
            centre[i] = true;
        }
    }

    /**
     * 用一長串半形空白墊出來的行，是伺服器在置中。
     *
     * <h2>實機回報</h2>
     * <pre>
     *            [Objective Completed]
     *              Loot Chests T3+
     *        Click here to claim your rewards!
     * </pre>
     * 三行的中心差了將近 40px（伺服器估的寬度不準，底線那一行尤其歪），
     * {@link #sharedCentre} 的 ±16px 收不進來，{@link BlockLayout#centered} 也拿最寬
     * 那行當基準判成靠左——中文變短之後，每行都貼著英文的左緣。
     *
     * <p>Wynncraft 的清單縮排用的是偏移字元（任務獎勵的「- +35 經驗值」），
     * 不是空白；前面墊了五個以上的半形空白，就只會是置中。每一行守住自己的中心，
     * 不去管它們彼此對不對齊——原文本來就沒對齊。
     */
    static void spacePadded(List<List<Run>> rows, boolean[] centre) {
        for (int i = 0; i < rows.size(); i++) {
            List<Run> row = rows.get(i);
            if (plainSpaceLead(row) >= 5
                    && columns(chatSegmentWidths(row, LineTranslator::runWidth)) < 2) {
                centre[i] = true;
            }
        }
    }

    /**
     * 縮排一樣、內容寬度卻不一樣的幾行，是靠左的清單，不可能是置中。
     *
     * <h2>實機回報</h2>
     * <pre>
     *   Rewards:
     *   - +Access to the Province of Wynn
     *   - +1 Ragni Teleportation Scroll      ← 只有這行被判成置中
     *   - +35 Experience Points
     * </pre>
     * 任務獎勵一行一則，五行同一個縮排。{@link BlockLayout#centered} 拿整塊最寬的
     * 一行比，Ragni 那一行的寬度剛好湊得上「置中該有的縮排」，就被判成置中；
     * 譯文多了「張」、寬了 13px，整行往左挪了一半，跟上下幾行錯開。
     *
     * <p>置中的行縮排由內容寬度決定：縮排一樣，內容就該一樣寬。兩行縮排相同、
     * 寬度差了一截，那個縮排就是清單的縮排，同一組全部改回靠左。
     */
    static void sameLeadList(List<List<Run>> rows, boolean[] centre) {
        int n = rows.size();
        int[] lead = new int[n];
        int[] body = new int[n];
        for (int i = 0; i < n; i++) {
            lead[i] = leadWidth(rows.get(i));
            body[i] = rowWidth(rows.get(i)) - lead[i];
        }
        boolean[] list = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (lead[i] <= 0 || body[i] <= 0) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                if (body[j] > 0 && Math.abs(lead[i] - lead[j]) <= 1
                        && Math.abs(body[i] - body[j]) > 6) {
                    list[i] = true;
                    list[j] = true;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (list[i]) {
                centre[i] = false;
            }
        }
    }

    /** 行首有幾個半形空白；碰到偏移字元或實字就停，偏移字元開頭的算 0。 */
    private static int plainSpaceLead(List<Run> row) {
        int n = 0;
        for (Run run : row) {
            if (run.space()) {
                return n == 0 ? 0 : n;
            }
            String text = run.text();
            for (int k = 0; k < text.length(); k++) {
                char c = text.charAt(k);
                if (c != ' ') {
                    return n;
                }
                n++;
            }
        }
        return 0;                                // 整行都是空白
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
            if (columns(chatSegmentWidths(row, LineTranslator::runWidth)) >= 2) {
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
        boolean single = columns(chatSegmentWidths(orig, LineTranslator::runWidth)) < 2;
        boolean centre = single && leadOrig > 0 && (centred || panel);
        int target = centre ? leadOrig + (bodyOrig - bodyMade) / 2 : leadOrig;
        int pad = target - leadMade;
        int[] columns = columnPad(orig, made);
        int fitted = fitWidth(orig, made, pad, columns);
        log.append("  ").append(centre ? "置中" : "靠左")
           .append(" 原文縮排=").append(leadOrig).append(" 內容=").append(bodyOrig)
           .append("  譯文縮排=").append(leadMade).append(" 內容=").append(bodyMade)
           .append("  補=").append(pad)
           .append(fitted != pad ? "（收寬後 " + fitted + "）" : "")
           .append("  譯文=").append(rowText(made))
           .append(System.lineSeparator());
        pad = fitted;
        MutableComponent row = Component.empty();
        String encoded = SpaceOffset.encode(pad);
        if (!encoded.isEmpty()) {
            row.append(literal(encoded, SpaceOffset.styleFor(Style.EMPTY)));
        }
        for (int px : columns) {
            if (px != 0) {
                log.append("        欄距補正=")
                   .append(java.util.Arrays.toString(columns))
                   .append(System.lineSeparator());
                break;
            }
        }
        row.append(applyChat(made, columns));
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
        return chatColumnPad(orig, made, LineTranslator::runWidth);
    }

    /**
     * 補正之後，這一行<b>不能比原文寬</b>；超出的部分從最右邊的欄距開始收回來。
     *
     * <h2>為什麼要收</h2>
     * 聊天視窗的寬度是固定的，放不下的行原版會從最後一個空白折到下一行最左邊。
     * Lootrun 結算那幾行本來就排到快滿（「300 Lootrun Experience ｜
     * Challenges Completed: 5」寬 294px，聊天預設 320px），任何一點多出來的寬度
     * 都會把右欄整段甩到下一行——issue #719 的「经验」孤零零掉到下一行最左邊。
     *
     * <p>{@link #centreColumns} 讓每一欄守住自己的中心。譯文比英文<b>窄</b>時整行
     * 只會變短；但譯文比英文<b>寬</b>時（俄文「наградных вытягиваний」），右欄的
     * 右緣就會跑出原文的右緣。這時候寧可右欄往左靠一點、跟上下幾行差幾像素，
     * 也不能讓整行被折斷——折斷之後兩欄連在哪一行都分不清楚。
     *
     * <p>收的順序是由右往左：先收最後一個欄距（左欄的位置不動），但至少留一個
     * 空白的寬度（{@link #MIN_CELL_GAP}），兩欄才不會黏在一起；還不夠才收行首的縮排。
     * 行首的縮排可以收到 0。
     *
     * @param columns 每個欄界的補正，<b>會被就地改掉</b>
     * @return 收過之後的行首補正
     */
    private static int fitWidth(List<Run> orig, List<Run> made, int pad, int[] columns) {
        return fitWidth(orig, made, pad, columns, chatWidth());
    }

    /**
     * 見上。單欄的行另外放寬到聊天視窗的寬度。
     *
     * <h2>實機回報</h2>
     * 任務完成的獎勵清單一行一則訊息送來，每行用同一個縮排排成一列。
     * 「+1 Ragni Teleportation Scroll」譯成「+1 張 Ragni Teleportation Scroll」
     * 多了 13px，照「不能比原文寬」的規則把縮排收掉 13px，那一行就比上下
     * 幾行往左凸出去。
     *
     * <p>「不能比原文寬」是為了兩欄的 Lootrun 結算不被折行（issue #719）。
     * 單欄的行只有一段字，真正的上限是聊天視窗；比原文寬一點、視窗還放得下，
     * 就該守住原文的縮排。
     *
     * @param window 聊天視窗的寬度；0 表示不知道，照原文的寬度收
     */
    static int fitWidth(List<Run> orig, List<Run> made, int pad, int[] columns, int window) {
        int limit = rowWidth(orig);
        if (window > limit
                && columns(chatSegmentWidths(orig, LineTranslator::runWidth)) < 2) {
            limit = window;
        }
        int over = rowWidth(made) + pad - limit;
        for (int px : columns) {
            over += px;
        }
        if (over <= 0) {
            return pad;
        }
        boolean[] gaps = chatGaps(made);
        int k = columns.length;
        for (int i = made.size() - 1; i >= 0 && over > 0; i--) {
            if (!gaps[i]) {
                continue;
            }
            k--;
            if (k < 0) {
                break;
            }
            int floor = contentBefore(made, i) ? MIN_CELL_GAP : 0;
            int room = made.get(i).px() + columns[k] - floor;
            if (room <= 0) {
                continue;                      // 已經貼著了，或是疊字用的負偏移
            }
            int take = Math.min(room, over);
            columns[k] -= take;
            over -= take;
        }
        if (over > 0 && pad > 0) {
            pad -= Math.min(pad, over);
        }
        return pad;
    }

    /** 聊天視窗現在多寬；拿不到（測試、還沒進遊戲）就回 0。 */
    private static int chatWidth() {
        try {
            return net.minecraft.client.gui.components.ChatComponent.getWidth(
                    net.minecraft.client.Minecraft.getInstance().options.chatWidth().get());
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 見 {@link #fitWidth}：收欄距時兩欄之間至少留這麼寬，大約一個半形空白。 */
    private static final int MIN_CELL_GAP = 4;

    /** 這個位置前面有沒有實字。沒有的話它是行首的縮排，不是欄與欄之間。 */
    private static boolean contentBefore(List<Run> runs, int at) {
        for (int i = at - 1; i >= 0; i--) {
            if (!runs.get(i).space() && hasContent(runs.get(i).text())) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@link #columnPad}，寬度的量法可以換掉。<b>測試用</b>——headless 沒有字型，
     * {@link #widthOf} 一律是 0，欄界認不認得出來這件事就永遠測不到。
     * 欄界用的是聊天專用的 {@link #chatGaps}。
     */
    static int[] chatColumnPad(List<Run> orig, List<Run> made, ToIntFunction<Run> width) {
        boolean[] madeGaps = chatGaps(made);
        int spaces = count(madeGaps);
        if (count(chatGaps(orig)) != spaces) {
            return new int[spaces];
        }
        int[] px = new int[spaces];
        int index = 0;
        for (int i = 0; i < made.size(); i++) {
            if (madeGaps[i]) {
                px[index++] = made.get(i).px();
            }
        }
        return columnDrift(chatSegmentWidths(orig, width), chatSegmentWidths(made, width), px);
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
            if (isColumnGap(r)) {
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
            if (isColumnGap(r)) {
                out[n++] = r.px();
            }
        }
        return out;
    }

    static boolean labelledRun(List<Run> runs, int gap) {
        int seen = 0;
        for (Run r : runs) {
            if (isColumnGap(r)) {
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
            List<Run> pieces = r.space() ? null : splitOffsets(r, isGap);
            if (pieces == null) {
                out.add(r);
                continue;
            }
            out.addAll(pieces);
            split = true;
        }
        return split ? out : runs;
    }

    /**
     * 把一段文字裡的偏移全部拆成獨立的間隔——不只行尾那一段。
     *
     * <h2>為什麼行尾不夠</h2>
     * 商城的階級說明是兩欄：
     *
     * <pre>
     *   +16 Market Slots [間隔]★[間隔]Super Priority Queue
     *   +10 Character Slots [間隔]Beta Access
     * </pre>
     *
     * 間隔跟著<b>後面</b>那段的顏色（★ 的顏色），或者整列同一個顏色、間隔夾在
     * 文字中間。兩種都不在行尾，先前只剝行尾的做法一個都拆不到——診斷檔裡
     * 這幾列都只數到行首那一個間隔（{@code 原文段寬=[0, 235]}），第二欄完全
     * 沒補償，照各列中文縮短的量各自往左偏，排出來參差不齊。
     *
     * @return 拆好的幾段；沒有可拆的、或整段本來就是偏移時回傳 {@code null}
     */
    private static List<Run> splitOffsets(Run r,
                                          java.util.function.BiPredicate<Style, String> isGap) {
        String text = r.text();
        List<Run> out = new ArrayList<>(3);
        boolean any = false;
        int from = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            if (!SpaceOffset.isOffset(cp)) {
                i += Character.charCount(cp);
                continue;
            }
            int end = i;
            while (end < text.length() && SpaceOffset.isOffset(text.codePointAt(end))) {
                end += Character.charCount(text.codePointAt(end));
            }
            String gap = text.substring(i, end);
            // 整段都是偏移的本來就是一個間隔，不拆成「空字串 + 間隔」
            if (gap.length() < text.length() && isGap.test(r.style(), gap)) {
                if (i > from) {
                    out.add(new Run(false, 0, r.style(), text.substring(from, i)));
                }
                out.add(new Run(true, SpaceOffset.decode(gap), r.style(), gap));
                from = end;
                any = true;
            }
            i = end;
        }
        if (!any) {
            return null;
        }
        if (from < text.length()) {
            out.add(new Run(false, 0, r.style(), text.substring(from)));
        }
        return out;
    }

    /**
     * 這個間隔是<b>欄與欄之間</b>的嗎。
     *
     * <h2>為什麼要有門檻</h2>
     * 材質包用寬度偏移做兩件完全不同的事：一種是把兩欄推開（獵殺信標的選單
     * 是 69px），另一種是把圖示往旁邊挪幾像素對齊文字。兩種都是偏移字元，
     * 分不出來的話後者也會被當成欄界——洞穴完成的獎勵那一行就是這樣：
     *
     * <pre>
     *   - +1 {@literal 🔒}Unidentified Helmet
     * </pre>
     *
     * 圖示前那個 2px 的微調被數成第二欄，整塊訊息於是被當成「兩欄面板」。
     * 面板裡的單欄行會照原文的中心擺，於是上面兩行獎勵各自被推開不同的距離，
     * 破折號對不齊；而這一行自己又被補了 17px 的欄距，圖示跟文字中間裂了一道縫。
     *
     * <p>真正的欄界動輒數十像素（信標是 69px，素材 tooltip 把數值往回拉 12px），
     * 對齊圖示的微調則是個位數。以八像素為界——大約一個半字元寬。
     */
    static final int MIN_GAP_PX = 8;

    static boolean isColumnGap(Run r) {
        return r.space() && Math.abs(r.px()) >= MIN_GAP_PX;
    }

    /**
     * 聊天這條路的欄界：{@link #isColumnGap} 認得的，加上「行首或文字後面、
     * 緊接著文字」的小偏移。
     *
     * <h2>為什麼聊天要另外判斷</h2>
     * 獵殺信標面板兩欄的<b>中心</b>是伺服器固定好的（約 77px 與 232px），名稱越長，
     * 縮排與欄距就越小。璀璨信標的名稱最長，實機收到的是
     *
     * <pre>
     *   &lt;+0&gt;Vibrant Dark Grey Beacon&lt;+7&gt;Vibrant Rainbow Beacon
     *   &lt;+7&gt;Vibrant Crimson Beacon&lt;+23&gt;…
     *   &lt;+2&gt;Vibrant Obscured Beacon&lt;+4&gt;Vibrant Obscured Beacon
     * </pre>
     *
     * 全都低於 {@link #MIN_GAP_PX}。欄界認不出來，兩個中文名就擠在一起、或整行往左偏
     * ——玩家回報「璀璨的信標歪得很嚴重」。一般信標的名稱短，偏移都在 8px 以上，碰不到。
     *
     * <p>門檻本來要擋的是圖示前的微調（{@code - +1 <2px>🔒Unidentified Helmet}）。
     * 那種偏移後面接的是圖示、不是字母，「後面緊接著有字母的文字」這一條照樣擋得住。
     * tooltip 的 {@code realign} 也共用 {@link #isColumnGap}，所以不去動它。
     */
    static boolean[] chatGaps(List<Run> runs) {
        boolean[] out = new boolean[runs.size()];
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (!r.space()) {
                continue;
            }
            if (isColumnGap(r)) {
                out[i] = true;
                continue;
            }
            if (r.px() < 0) {
                continue;                      // 疊字用的負偏移，見 isBacktrack
            }
            out[i] = textBefore(runs, i) && startsWithLetter(runs, i + 1);
        }
        return out;
    }

    /** 往前第一段實字是有字母的，或前面根本沒有實字（行首）。 */
    private static boolean textBefore(List<Run> runs, int at) {
        for (int i = at - 1; i >= 0; i--) {
            Run r = runs.get(i);
            if (!r.space()) {
                return GlyphSplitter.hasLetter(r.text());
            }
        }
        return true;
    }

    /** 往後第一段實字是以字母開頭的。 */
    private static boolean startsWithLetter(List<Run> runs, int from) {
        for (int i = from; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (r.space()) {
                continue;
            }
            String text = r.text().stripLeading();
            return !text.isEmpty() && Character.isLetter(text.codePointAt(0));
        }
        return false;
    }

    private static int count(boolean[] flags) {
        int n = 0;
        for (boolean f : flags) {
            if (f) {
                n++;
            }
        }
        return n;
    }

    /** 一段文字畫出來多寬。 */
    static int runWidth(Run r) {
        return widthOf(literal(r.text(), r.style()));
    }

    /** {@link #segmentWidths} 的聊天版，欄界見 {@link #chatGaps}。 */
    static List<Integer> chatSegmentWidths(List<Run> runs, ToIntFunction<Run> width) {
        boolean[] gaps = chatGaps(runs);
        List<Integer> out = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < runs.size(); i++) {
            if (gaps[i]) {
                out.add(total);
                total = 0;
            } else if (!runs.get(i).space()) {
                total += width.applyAsInt(runs.get(i));
            }
        }
        out.add(total);
        return out;
    }

    /** {@link #apply} 的聊天版，欄界見 {@link #chatGaps}。 */
    private static Component applyChat(List<Run> runs, int[] adjust) {
        boolean[] gaps = chatGaps(runs);
        MutableComponent out = Component.empty();
        int index = 0;
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (!gaps[i]) {
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

    private static int countSpaces(List<Run> runs) {
        int n = 0;
        for (Run r : runs) {
            if (isColumnGap(r)) {
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
            if (isColumnGap(r)) {
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
            if (!isColumnGap(r)) {
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
        // 這個沒帶 percent 的版本<b>不走屬性列那條路</b>。屬性列要看數值有沒有
        // 百分號才知道該挑哪一個標籤（生命回復 vs 生命回復百分比），而百分號
        // 這時候常常已經被吃進 {~} 裡了——只有呼叫端算得出來。這裡硬猜的話，
        // 「Health Regen -20%」會被標成非百分比的譯法，看起來只是翻得不夠好，
        // 其實是查錯鍵了。帶 percent 的那個版本才試。
        return lookupTrimmed(template, store, false);
    }

    /**
     * @param percent 這一行的數值是百分比，優先找「標籤 + {@code %}」的鍵
     */
    static String lookup(String template, TranslationStore store, boolean percent) {
        String hit = lookupTrimmed(template, store, percent);
        if (hit == null) {
            hit = statRow(template, store, percent);
        }
        return hit != null ? hit : nameRow(template, store);
    }

    /**
     * 技能樹的「項目符號 + 標籤 + 名稱」整行。
     *
     * <h2>實機長相</h2>
     * 法師技能樹的節點說明裡有這幾種行：
     *
     * <pre>
     *   ✔ Required Ability: Heal
     *   ✖ Required Ability: Dimensional Tear
     *   - Psychokinesis
     *   - Meteor Shower
     * </pre>
     *
     * <h2>為什麼不能靠整行條目收</h2>
     * 跟 {@link #statRow} 同一個道理：<b>五個職業約兩百多個技能 × 三種形狀</b>，
     * 而整行條目一種只收一個。上一版手動補了五組 {@code Required Ability:}，
     * 這次 capture 又冒出八組沒收到的——每補一批就再冒一批，收不完。
     * 而且 {@code misc.json} 裡已經躺著一批 dst 留空的同形狀條目，
     * 正是「一行一條」補不動的證據。
     *
     * <h2>做法</h2>
     * 兩邊本來就都有：標籤（{@code Required Ability:} → 前置技能:）在
     * {@code ability-labels.json}，技能名（{@code Heal} → 治療）在
     * {@code ability/} 底下五個職業檔裡，共一千兩百多條。所以不查整行，
     * 改成把行首的項目符號剝掉、冒號前後<b>各自查、再原樣組回去</b>——
     * 跟 {@link #statRow} 只查標籤是同一招。一條標籤涵蓋五個職業。
     *
     * <h2>為什麼很安全</h2>
     * 切開後<b>每一段都必須自己命中語料</b>，只要有一段查不到就整條放棄，
     * 不會把一般句子拼成半中半英。唯一的例外是 {@link #NAME_LABELS}
     * 那幾個<b>後面必定接技能名</b>的標籤：Wynncraft 改版新增的技能還沒進
     * 語料時，至少把標籤翻出來，剩半個技能名是原文——那本來就是專有名詞。
     *
     * <p>而且這條路排在整行查與 {@link #statRow} 後面，真正收在語料裡的整行
     * （翻譯團隊手寫的那六組、{@code - Converts up to} 那種）永遠先命中。
     *
     * @return 譯好的整行；切不開、或查不到標籤時回傳 {@code null}
     */
    private static String nameRow(String template, TranslationStore store) {
        int start = 0;
        while (start < template.length()
                && Character.isWhitespace(template.charAt(start))) {
            start++;
        }
        int body = start;
        for (int next = bulletEnd(template, body); next > body;
                next = bulletEnd(template, body)) {
            body = next;
        }
        String rest = template.substring(body);
        String head = template.substring(0, body);

        int colon = labelEnd(rest);
        if (colon > 0) {
            String label = rest.substring(0, colon);
            String zhLabel = lookupTrimmed(label, store, false);
            int name = colon;
            while (name < rest.length() && rest.charAt(name) == ' ') {
                name++;
            }
            String tail = rest.substring(name);
            if (zhLabel != null && !zhLabel.isBlank() && !tail.isBlank()) {
                // 玩家自己打的字查到了也不換：畫面上要看到的是自己搜了什麼。
                String zhName = TYPED_LABELS.contains(label.strip())
                        ? tail : lookupTrimmed(tail, store, false);
                if ((zhName == null || zhName.isBlank())
                        && NAME_LABELS.contains(label.strip())) {
                    zhName = tail;             // 還沒進語料的新技能，名字留原文
                }
                if (zhName != null && !zhName.isBlank()) {
                    // 中間的空白交給 reattach：譯文收在全形冒號時不再補半形
                    // 空格，否則「前置技能：␣Ophanim」會留白疊留白。
                    return head + zhLabel
                            + reattach(zhLabel, rest.substring(colon, name)) + zhName;
                }
            }
        }
        if (body == start) {
            return null;                       // 行首沒剝掉東西，跟整行查是同一件事
        }
        String zh = lookupTrimmed(rest, store, false);
        return zh == null || zh.isBlank() ? null : head + zh;
    }

    /**
     * 見 {@link #nameRow}：這幾個標籤的冒號後面<b>必定是技能名</b>，
     * 查不到譯文時可以原樣留著——技能名是專有名詞，留原文玩家看得懂，
     * 整行英文才是問題。改版新增的技能進語料之前就靠這一條撐著。
     *
     * <p>刻意用<b>英文原文</b>當條件而不是譯文：原文各語言共用，
     * 這條路對 {@code ja_jp}、{@code de_de} 一樣成立。
     */
    private static final java.util.Set<String> NAME_LABELS = java.util.Set.of(
            "Required Ability:", "Unlocked Ability:",
            // Lootrun 的洞窟清單「✔ Cave: Eyeball Gauntlet」十二條，
            // 冒號後面是洞窟的專有名詞，跟技能名一樣留原文。
            "Cave:",
            // 市集的篩選說明「- Name Contains: a」，冒號後面是玩家自己打的搜尋字，
            // 語料裡不可能有，只翻標籤。
            "Name Contains:");

    /**
     * 見 {@link #nameRow}：冒號後面是<b>玩家自己打的字</b>，一律原樣留著，
     * 就算剛好查得到譯文也不換——「- Name Contains: Insulators」要讓玩家看到
     * 自己搜的是 Insulators，換成「絕緣器」就對不上輸入框裡的字了。
     */
    private static final java.util.Set<String> TYPED_LABELS = java.util.Set.of(
            "Name Contains:");

    /**
     * 行首那個項目符號到哪裡結束（含它後面的空白）。
     *
     * <p>{@link #isDecoration} 刻意不含 {@code -}——那是因為
     * {@code "- Converts up to"} 這種<b>整條收在語料裡</b>的鍵就是這樣開頭的，
     * 在整行查的時候剝掉它會查不到。但這裡跑在整行查<b>失敗之後</b>，
     * 剝了才有機會，所以連 {@code -} 一起認。要求它後面有空白，
     * 不然 {@code "-20%"} 的負號也會被當成項目符號。
     *
     * @return 符號後面的位置；{@code at} 不是項目符號時回傳 {@code at}
     */
    private static int bulletEnd(String template, int at) {
        if (at >= template.length()) {
            return at;
        }
        int cp = template.codePointAt(at);
        boolean bullet = BULLETS.indexOf(cp) >= 0;
        if (!bullet && !isDecoration(cp)) {
            return at;
        }
        int next = at + Character.charCount(cp);
        int spaced = next;
        while (spaced < template.length() && template.charAt(spaced) == ' ') {
            spaced++;
        }
        if (bullet && spaced == next) {
            return at;                         // 「-20%」的負號，不是項目符號
        }
        return spaced;
    }

    /**
     * 冒號在哪裡——只認<b>後面跟著空白</b>的那個，
     * {@code "Ability: Heal"} 才算，{@code "12:30"} 不算。
     *
     * @return 冒號的下一個位置；沒有可切的冒號時回傳 -1
     */
    private static int labelEnd(String rest) {
        int at = rest.indexOf(':');
        return at > 0 && at + 1 < rest.length() && rest.charAt(at + 1) == ' '
                ? at + 1 : -1;
    }

    /** 見 {@link #bulletEnd}：這些字元自己不是內容，只是行首的項目符號。 */
    private static final String BULLETS = "-–—•*";

    /**
     * 裝備面板的「標籤 + 數值」整行。
     *
     * <h2>為什麼不能靠整行條目收</h2>
     * 同一個「法術傷害」在實機上長這些樣子：
     *
     * <pre>
     *   Fire Spell Damage {#}+{~} [{~}]
     *   Water Spell Damage{#}-{~} to -{~}
     *   Spell Damage {#}+{~} [{~}, {~}]
     *   Elemental Spell Damage {#}+{~} ★{~} ⇧{~} ⇩{~}
     *   [{~}] Spell Damage
     * </pre>
     *
     * <b>七種元素 × 正負 × 五六種數值格式</b>，而整行條目一種只收一個。
     * 收不完——每補一批，下一份 capture 又冒出新的排列。
     *
     * <h2>做法</h2>
     * 標籤本來就都在 {@code ui-labels.json} 裡（288 條，含
     * {@code Fire Spell Damage}、{@code Damage Scale} 這些）。
     * 所以不查整行，改成把行尾<b>純數值的那一段</b>切掉、只查前面的標籤，
     * 數值原樣接回去。一條標籤就涵蓋所有排列。
     *
     * <h2>為什麼很安全</h2>
     * 尾巴必須<b>整段都是數值</b>——佔位符、數字、正負號、括號、箭頭這些。
     * 只要還有一個英文字，就不是屬性列而是句子，這條路直接放棄。例外只有
     * {@link #TAIL_WORDS} 那兩個字，它們本來就只出現在數值中間。
     *
     * @return 譯好的整行；不是屬性列、或標籤查不到時回傳 {@code null}
     */
    private static String statRow(String template, TranslationStore store, boolean percent) {
        int from = valueTailStart(template);
        if (from <= 0) {
            return null;                       // 整行都是數值
        }
        String label = template.substring(0, from);
        if (!GlyphSplitter.hasLetter(label)) {
            return null;
        }
        String tail = template.substring(from);
        if (!tail.isEmpty()) {
            String zh = statLabel(label, store, percent);
            if (zh != null) {
                return zh + translateTail(tail, store);
            }
        }
        // 數值在<b>前面</b>的那一種：
        //
        //   {~} Main Attack Damage {#} [{~}]
        //   {~} Walk Speed {#} [{~}]
        //
        // 上面那一刀是從行尾往回切的，切到「Damage」就停，剩下的「{~} Main Attack
        // Damage」當然不是標籤——於是這幾種排列一條都翻不出來，capture 裡每種都
        // 被記了十來次缺口。標籤明明都在 ui-labels.json 裡。
        //
        // 所以行首那段純數值也切掉再查一次，數值原樣接回前面。
        int head = valueHeadEnd(label);
        String lead = label.substring(0, head);
        if (lead.indexOf(GlyphSplitter.NUMBER_PLACEHOLDER) < 0) {
            return null;                       // 前後都沒有數值，不是屬性列
        }
        String name = label.substring(head);
        // 只有前面有數值、後面什麼都沒有的（逐片段那條路常常切成「+22 Main Attack
        // Damage」一段），標籤必須<b>真的是介面標籤</b>。不然任何「數字 + 語料裡的詞」
        // 都會被拼起來——「2 Guardian」就成了「2 守護者」。
        if (tail.isEmpty() && !store.isUiLabel(name.strip())) {
            return null;
        }
        String zh = statLabel(name, store, percent);
        return zh == null ? null : translateTail(lead, store) + zh + translateTail(tail, store);
    }

    /**
     * 行首那段純數值到哪裡為止。佔位符整組跳過，數值字元一個一個走。
     * 見 {@link #statRow}「數值在前面」那一段。
     */
    private static int valueHeadEnd(String label) {
        int at = 0;
        while (at < label.length()) {
            char c = label.charAt(at);
            if (c == '{') {
                int close = label.indexOf('}', at);
                if (close < 0) {
                    break;
                }
                at = close + 1;
            } else if (c != '}' && isValueChar(c)) {
                at++;
            } else {
                break;
            }
        }
        return at;
    }

    /** 屬性標籤的譯名，百分比／實數分開挑。見 {@link #statRow}。 */
    private static String statLabel(String label, TranslationStore store, boolean percent) {
        // 同一個屬性有三種標籤，畫面上是<b>三個不同的東西</b>：
        //
        //   Spell Damage +12%   -> 法術傷害百分比   （Spell Damage%）
        //   Spell Damage +230   -> 法術傷害值       （Spell Damage Raw）
        //   Spell Damage        -> 法術傷害         （沒有特地區分時）
        //
        // 實機截圖裡這兩行上下相鄰，都標成「法術傷害」的話玩家分不出哪個是
        // 百分比、哪個是實數——而那正是他要看的差別。
        //
        // 百分比與否只有呼叫端算得出來：模板裡的百分號常常已經被吃進 {~} 了。
        String zh = lookupTrimmed(label + (percent ? "%" : " Raw"), store, false);
        if (zh == null || zh.isBlank()) {
            zh = lookupTrimmed(label, store, percent);
        }
        return zh == null || zh.isBlank() ? null : zh;
    }

    /**
     * 行尾那段純數值從哪裡開始。
     *
     * <p>從尾巴往回走：佔位符整組跳過，數值字元一個一個退，
     * {@link #TAIL_WORDS} 裡的小字也算數值的一部分。遇到別的就停。
     *
     * @return 數值段的起點；整行都不是數值時回傳 {@code template.length()}
     */
    private static int valueTailStart(String template) {
        int at = template.length();
        while (at > 0) {
            char last = template.charAt(at - 1);
            if (last == '}') {
                int open = template.lastIndexOf('{', at - 1);
                if (open < 0) {
                    break;                     // 沒頭沒尾的括號，別猜
                }
                at = open;
            } else if (isValueChar(last)) {
                at--;
            } else {
                int word = tailWordStart(template, at);
                if (word < 0) {
                    break;
                }
                at = word;
            }
        }
        return at;
    }

    /** {@code at} 前面那個字如果是尾巴允許的小字，回傳它的起點，否則 -1。 */
    private static int tailWordStart(String template, int at) {
        int start = at;
        while (start > 0 && Character.isLetter(template.charAt(start - 1))) {
            start--;
        }
        return start < at && CONNECTOR_KEYS.containsKey(template.substring(start, at))
                ? start : -1;
    }

    /**
     * 數值段裡的小字也要換掉，不然「+1 tier」會留一個英文在中文中間。
     *
     * <p>換成什麼由語料決定（見 {@link #connector}），查不到就留英文。
     * 字前後的空白原樣保留。
     */
    private static String translateTail(String tail, TranslationStore store) {
        StringBuilder out = new StringBuilder(tail.length());
        int at = 0;
        while (at < tail.length()) {
            if (!Character.isLetter(tail.charAt(at))) {
                out.append(tail.charAt(at));
                at++;
                continue;
            }
            int end = at;
            while (end < tail.length() && Character.isLetter(tail.charAt(end))) {
                end++;
            }
            String word = tail.substring(at, end);
            String local = connector(word, store);
            out.append(local != null ? local : word);
            at = end;
        }
        return out.toString();
    }

    /** 見 {@link #statRow}：這些字元自己不是內容，只是數值的一部分。 */
    private static boolean isValueChar(char c) {
        return Character.isDigit(c) || Character.isWhitespace(c)
                || "+-±%/[](){}<>,.:~*★⇧⇩⇨❤✦✤✹✽✧'\"".indexOf(c) >= 0;
    }

    /**
     * 數值段裡唯一允許出現的英文字，以及它們在語料裡的連接詞鍵。
     *
     * <p>{@code to} 是區間（{@code +100 to +200}）、{@code tier} 是攻速階級
     * （{@code +1 tier}）。兩個都只出現在數值中間，不會讓句子被誤判成屬性列。
     *
     * <h2>為什麼不寫死譯文</h2>
     * 先前這裡直接對應「到」「階」。那是繁中的字，而屬性列這條路每個語言都會走——
     * 日文、俄文、韓文的標籤查到之後，數值中間照樣被塞進中文。
     * 店家本身不知道自己是哪個語言，所以不在程式裡分語言，改查語料：
     * 每個語言的 {@code ui-labels.json} 自己寫連接詞，簡體沒寫時照疊層規則退到繁體
     * （見 {@code Languages#fallbackFor}），其他語言沒寫就留英文。
     */
    private static final java.util.Map<String, String> CONNECTOR_KEYS = java.util.Map.of(
            "to", "{~} to {~}", "tier", "{~} tier", "tiers", "{~} tier");

    /** 連接詞譯文裡的數值佔位符，{@code {~}} 或照順序指名的 {@code {~1}}。 */
    private static final java.util.regex.Pattern CONNECTOR_SLOT =
            java.util.regex.Pattern.compile("\\{~\\d*}");

    /**
     * 範圍／單位小字在目前語言的說法：語料 {@code "{~} to {~}": "{~} 到 {~}"}
     * 拿掉佔位符、去掉前後空白，得到「到」。
     *
     * @return 語料沒有這個鍵、或譯文是空的時回傳 {@code null}——呼叫端留英文
     */
    static String connector(String word, TranslationStore store) {
        String key = CONNECTOR_KEYS.get(word);
        if (key == null || store == null) {
            return null;
        }
        String dst = store.lookup(key);
        if (dst == null || dst.isBlank()) {
            return null;
        }
        String local = CONNECTOR_SLOT.matcher(dst).replaceAll("").strip();
        return local.isEmpty() ? null : local;
    }

    /**
     * 這一段是不是只有數值與範圍小字：{@code " to "}、{@code "-60 tier"}。
     *
     * <p>至少要有一個 {@link #CONNECTOR_KEYS} 裡的字；有別的英文字就是句子，不算。
     */
    private static boolean isConnectorSegment(String body) {
        boolean word = false;
        int at = 0;
        while (at < body.length()) {
            char c = body.charAt(at);
            if (Character.isLetter(c)) {
                int end = at;
                while (end < body.length() && Character.isLetter(body.charAt(end))) {
                    end++;
                }
                if (!CONNECTOR_KEYS.containsKey(body.substring(at, end))) {
                    return false;
                }
                word = true;
                at = end;
            } else if (isValueChar(c)) {
                at++;
            } else {
                return false;
            }
        }
        return word;
    }

    /**
     * {@link #translateSegments} 的第二輪：標籤翻成功的行，數值之間獨立成段的
     * 「 to 」「-60 tier」也換成目前語言的連接詞。
     *
     * <h2>為什麼第一輪收不到</h2>
     * 未鑑定裝備的範圍是好幾個<b>不同顏色</b>的元件：{@code +2%}、{@code  to }、
     * {@code +9%}。單獨一段「to」查不到鍵，也不是屬性列（前面沒有標籤），
     * 於是原樣抄過去，畫面變成「移動速度 +2% to +9%」。
     *
     * <p>顏色照原段，字型換成預設——跟 {@link #rebuild} 對文字的處理一樣，
     * 否則中文會用 Wynncraft 的字型畫成方框。
     */
    private static void translateConnectors(List<Piece> pieces, List<Integer> at,
                                            TranslationStore store) {
        // 從後面往前換：補回來的排版偏移會插一個片段，前面的位置才不會跑掉
        for (int k = at.size() - 1; k >= 0; k--) {
            int index = at.get(k);
            Piece piece = pieces.get(index);
            String raw = piece.text();
            String tail = SpaceOffset.trailingOffsets(raw);
            String body = raw.substring(0, raw.length() - tail.length());
            String local = translateTail(body, store);
            if (local.equals(body)) {
                continue;                      // 語料沒有這個語言的連接詞，留英文
            }
            pieces.set(index, Piece.translated(literal(local, forDisplay(piece.style())),
                                               literal(body, piece.style())));
            if (!tail.isEmpty()) {
                pieces.add(index + 1, Piece.space(SpaceOffset.decode(tail),
                        SpaceOffset.styleFor(piece.style())));
            }
        }
    }

    /** 見 {@link #lookup}：先剝首尾再查，這是原本那條路。 */
    private static String lookupTrimmed(String template, TranslationStore store,
                                        boolean percent) {
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
        if (hit != null) {
            return hit;
        }
        hit = withMaker(key, store);
        return hit != null ? hit : withOwner(key, store);
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
     * 「{@code <玩家名>'s <東西>}」這種<b>名字在前</b>的漂浮字。
     *
     * <h2>畫面上長什麼樣</h2>
     * 石碑放下去之後，頭上浮著：
     *
     * <pre>
     *   PoorChaCha's Mob Totem
     *   ⌛ 4m 56s
     * </pre>
     *
     * 名字是別人的（也可能是自己的），永遠不會進語料——擷取那一關就擋掉了
     * （見 {@code PlayerDataFilter}）。所以整行查不到，畫面上一直是英文。
     *
     * <p>{@link #withMaker} 處理的是「固定開頭 + 名字」（{@code Crafted by X}），
     * 這裡是<b>反過來</b>的那一半：名字在前、東西在後。
     *
     * <h2>為什麼不會亂認</h2>
     * 兩道關卡都得過：前半要像 Minecraft 的 ID（見 {@link #isName}），
     * 後半要<b>剛好是語料裡的一個鍵</b>。設定裡的所有格（{@code Orphion's Grace}）
     * 整條本來就查得到，根本輪不到這裡；真的輪到了，「Grace」也不是語料的鍵。
     */
    private static String withOwner(String key, TranslationStore store) {
        String core = key.strip();
        int at = -1;
        for (String mark : OWNER_MARKS) {
            int found = core.indexOf(mark);
            if (found > 0 && (at < 0 || found < at)) {
                at = found;
            }
        }
        if (at <= 0) {
            return null;
        }
        String name = core.substring(0, at);
        if (!isName(name)) {
            return null;
        }
        String thing = core.substring(at + 3).strip();
        String zh = thing.isEmpty() ? null : store.lookup(thing);
        return zh == null || zh.isBlank() ? null : name + " 的" + zh;
    }

    /** 見 {@link #withOwner}：所有格的兩種撇號，材質包兩種都出現過。 */
    private static final String[] OWNER_MARKS = {"'s ", "’s "};

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

    /** 同上，但自己指定符號池——併成一句之後續行的圖示要拿掉。見 {@link #unwrappedGlyphs}。 */
    private static Component rebuild(String translated, LineParts parts,
                                     List<LineParts.Piece> glyphs, TranslationStore store) {
        List<Component> one = rebuildAll(new String[] {translated}, List.of(parts),
                                         List.of(), glyphs, null, store);
        return one == null ? null : one.get(0);
    }

    /**
     * 併成一句之後還剩下哪些符號。
     *
     * <h2>為什麼池子要跟著少</h2>
     * {@link TranslationStore#unwrap} 把換行與<b>續行的行首圖示</b>一起拿掉了，
     * 而語料那條是照併起來的樣子寫的——它要的 {@code {#}} 比原文少一個。
     * 池子沒跟著少的話 {@link #rebuildAll} 會判定「佔位符數量對不上」整條放棄
     * （那個檢查是嚴格相等的，本來就該嚴格）。
     *
     * <p>只拿掉<b>緊接在換行後面</b>的那幾個。句子中間的圖示是內容
     * （{@code craft {#} Boots}），拿掉會讓譯文少一個圖示。
     * 判斷方式跟 {@code unwrap} 逐字對齊：換行之後連續的空白與圖示都算行首，
     * 遇到第一個實字就結束。
     */
    static List<LineParts.Piece> unwrappedGlyphs(
            String template, List<LineParts.Piece> glyphs) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        List<LineParts.Piece> kept = new ArrayList<>();
        int at = 0;
        int index = 0;
        boolean lineStart = false;
        while (at < template.length() && index < glyphs.size()) {
            if (template.startsWith(glyph, at)) {
                if (!lineStart) {
                    kept.add(glyphs.get(index));
                }
                index++;
                at += glyph.length();
                continue;
            }
            char c = template.charAt(at);
            if (c == '\n') {
                lineStart = true;
            } else if (!Character.isWhitespace(c)) {
                lineStart = false;
            }
            at++;
        }
        while (index < glyphs.size()) {
            kept.add(glyphs.get(index++));
        }
        return kept;
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
    /**
     * 方括號對方括號，照出現順序配。
     *
     * <h2>為什麼要有這一條</h2>
     * 方括號在這些卡片上是重點記號，括號裡那一段有自己的顏色。要把顏色貼回
     * 譯文，既有的路是<b>查表</b>：把括號裡的詞查出中文，再拿字面去譯文裡找。
     * 查不到就整塊掉回底色——畫面上是「{@code [} 有色、名字沒色」的半彩。
     *
     * <p>查不到很常見，而且理由都不是「該翻沒翻」：
     *
     * <ul>
     *   <li>{@code [Mini-Quest - Slay Spiders]}——語料收的是<b>整行</b>
     *       （{@code + New Quest [Mini-Quest - Slay Spiders]}），括號那半
     *       自己沒有條目。</li>
     *   <li>{@code [Combat Lv. 88]}——等級是這一次的數字，不可能進語料。</li>
     *   <li>{@code [-677, 46, -4948]}——座標同理。</li>
     * </ul>
     *
     * <p>但這幾種<b>位置就是答案</b>：原文有幾個方括號段，譯文照樣寫了幾個，
     * 而且順序一樣——語料的譯文本來就照著原文的括號結構寫。第 i 個對第 i 個，
     * 不必查表。
     *
     * <h2>何時不做</h2>
     * 個數對不上就不做：那表示譯文改寫了括號結構，照順序配會配到別的東西上。
     * 某一段橫跨了兩種顏色也整組不做——那一段的顏色本來就有歧義，猜錯比不猜糟。
     * 被 tooltip 寬度切成兩行的括號（{@code [Combat Lv.} ＋ {@code 88]}）不算
     * 歧義，只要那幾段同色就算一段。
     */
    static List<LineParts.Piece> bracketAccents(
            List<LineParts.Piece> allRuns, String[] translated, Style blockStyle) {
        List<Style> source = new ArrayList<>();
        Style open = null;
        boolean mixed = false;
        int depth = 0;
        for (LineParts.Piece run : allRuns) {
            String text = run.text();
            if (depth > 0 && !sameColour(open, run.style())) {
                mixed = true;                  // 跨行的那一段換了顏色
            }
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '[') {
                    if (depth == 0) {
                        open = run.style();
                        mixed = false;
                    }
                    depth++;
                } else if (c == ']' && depth > 0) {
                    depth--;
                    if (depth == 0) {
                        if (mixed) {
                            return List.of();  // 見上：有歧義就整組不做
                        }
                        source.add(open);
                        open = null;
                    }
                }
            }
        }
        if (source.isEmpty() || depth != 0) {
            return List.of();                  // 沒有括號，或有一個沒收尾
        }
        List<String> spans = squareSpans(String.join(NL, translated));
        if (spans.size() != source.size()) {
            return List.of();
        }
        List<LineParts.Piece> out = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            Style style = source.get(i);
            if (style == null || sameColour(style, blockStyle) || spans.get(i).isBlank()) {
                continue;                      // 跟底色同色的不必貼
            }
            out.add(new LineParts.Piece(spans.get(i), style));
            // 括號裡夾著佔位符時，整塊貼不上去：畫的時候佔位符是自己一個
            // 片段，整塊的字面在畫面上從來不連續（{@code [{~} 蓬鬆毛皮]} 只有
            // 開頭那個 {@code [} 對得上）。實機那張迷你任務卡就是這樣變成
            // 「{@code [} 青、名字灰」的。
            //
            // 所以把佔位符切開的那幾段<b>各自</b>登記一次。只登記<b>帶實字</b>
            // 的那幾段：座標切出來的 {@code [-}、{@code , } 太短又到處都有，
            // 貼上去只會貼到別的地方。
            for (String piece : PLACEHOLDER.split(spans.get(i), -1)) {
                if (piece.length() >= 2 && hasLetter(piece)) {
                    out.add(new LineParts.Piece(piece, style));
                    // 收尾那一段再登記一份<b>去掉前導空白</b>的。
                    //
                    // 面板寬度把 `[{~} 麥芽穀粒]` 斷在數值後面時，那個空格是
                    // 斷行點、會被吃掉：上一行留 `…[{~}`，下一行從
                    // `麥芽穀粒]` 開始。帶空格的那一份於是兩行都對不上，
                    // 而名字本身另有一條（物品名查表查得到）——所以畫面上是
                    // 「麥芽穀粒」有色、後面那個 `]` 掉回底色。使用者回報的
                    // 正是這個。
                    //
                    // 只對 `]` 收尾的那一段做：它是被斷行孤立出來的那一半，
                    // 而且帶著括號夠獨特。中間那種兩頭都是空格的片段不動，
                    // 剝掉空白之後太容易貼到散文裡的同名詞上。
                    String bare = piece.strip();
                    if (piece.endsWith("]") && !bare.equals(piece)
                            && bare.length() >= 2 && hasLetter(bare)) {
                        out.add(new LineParts.Piece(bare, style));
                    }
                }
            }
        }
        return out;
    }

    /** 這一段裡有沒有字母或方塊字——標點與數字不算。 */
    private static boolean hasLetter(String text) {
        return text.codePoints().anyMatch(Character::isLetter);
    }

    /**
     * 譯文裡最外層的那幾個 {@code [...]}，照出現順序。
     *
     * <h2>被斷行切開的也要算</h2>
     * 先前跨行就整組放棄。但這裡拿到的譯文<b>已經照面板寬度折過</b>
     * （見 {@code #wrapToBlock}），一張迷你任務卡三、四行，括號落在折行處是
     * 常態而不是例外——放棄等於整張卡的括號全部沒有顏色。實機回報的採集站那張
     * 卡就是這樣：「把 {@code [24 鮭魚油]} 或 {@code [}」換行「{@code 24 鮭魚肉]}」，
     * 兩組括號都只剩底色。
     *
     * <p>改成把斷行<b>接回來</b>再收。位置本來就不是這條路在用的東西——
     * 貼樣式是拿字面去找（見 {@code #appendText}），而接回來的字面正好是
     * {@code keepAccentsWhole} 要的：它靠「上一行結尾 ＋ 下一行開頭」認出被切開的
     * 詞，再把前半搬到下一行。搬不動的還有 {@code #halvesAcrossBreaks} 接著。
     */
    private static List<String> squareSpans(String text) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == ']' && depth > 0) {
                depth--;
                if (depth == 0) {
                    out.add(text.substring(start, i + 1)
                                .replace(String.valueOf(NEWLINE), ""));
                }
            }
        }
        return depth == 0 ? out : List.of();
    }

    /** 兩個樣式是不是同一個顏色（不看裝飾，跟 {@link #dominantStyle} 同一把尺）。 */
    private static boolean sameColour(Style one, Style other) {
        if (one == null || other == null) {
            return one == other;
        }
        return undecorated(one).equals(undecorated(other));
    }

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
     * 「標籤: 數值」兩半各自的顏色。
     *
     * <h2>先前壞在哪</h2>
     * 派對面板與世界清單整片都是這個形狀，而兩半的顏色<b>是不一樣的</b>：
     *
     * <pre>
     *   #00AAAA 「Type: 」   #55FFFF 「Grinding Mobs」
     *   #00AAAA 「World: 」  #55FFFF 「NA12」
     * </pre>
     *
     * 這種行是<b>整行</b>收在語料裡的（{@code "Type: Grinding Mobs": "類型：刷怪"}），
     * 走不到 {@link #labelAccent} 那條「名稱：說明」的路，於是整行只拿得到
     * 一個 {@link #dominantStyle}——而主樣式是<b>照字數</b>算的：
     *
     * <pre>
     *   Type:  6 字 &lt; Grinding Mobs 13 字   -&gt; 整行套上數值的顏色，標籤變亮
     *   World: 7 字 &gt; NA12          4 字   -&gt; 整行套上標籤的顏色，數值變暗
     * </pre>
     *
     * 同一個面板上下兩行，一行標籤太亮、一行數值太暗，而且錯的方向還相反——
     * 純粹看哪半的字比較多。使用者回報的顏色錯誤就是這個。
     *
     * <h2>做法</h2>
     * 原文在冒號那裡換色的話，譯文也照冒號切兩半，各自貼回原本那半的顏色。
     * 交給既有的重點段機制去貼，不必另外開一條上色的路。
     *
     * <h2>何時不做</h2>
     * 冒號兩邊<b>同色</b>時不做（那本來就沒得分）、原文只有一行時才做
     * （多行的話「哪一半」指的不是同一件事）、譯文沒有冒號時不做。
     * 跟主樣式相同的那一半也不登記——它本來就會拿到那個顏色，
     * 重複登記只會讓貼樣式那一步挑錯（見 {@link #covered}）。
     */
    private static List<LineParts.Piece> labelValueAccents(
            List<LineParts> parts, String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        if (parts.size() != 1 || translated.length != 1) {
            return List.of();
        }
        // 一「份」不等於一「行」。
        //
        // 名牌是<b>一份含換行</b>的文字（「Copper\n✔ Ⓑ Mining Lv Min: {~}\n
        // ✖ Equipped Tool: Pickaxe」），parts 與 translated 都只有一個，
        // 上面那道關卡因此攔不住它。於是冒號那一刀切在第<b>二</b>行的冒號上，
        // 「數值」那一半成了「{~}\n✖ 裝備工具: 鎬」——整個第三行被登記成數值的
        // 白色，紅色的叉與灰色的標籤全被蓋掉。玩家看到的是採集點名牌只有中間
        // 那一行有顏色，而顏色正是那塊牌子的資訊（綠勾＝等級夠、紅叉＝工具不對）。
        //
        // 「哪一半」本來就只在單行的情況下說得通，所以有換行就不做。
        if (translated[0].indexOf(NEWLINE) >= 0
                || parts.get(0).template().indexOf(NEWLINE) >= 0) {
            return List.of();
        }
        Style label = null;
        Style value = null;
        for (LineParts.Piece run : parts.get(0).runs()) {
            if (isNote(run.text())) {
                continue;
            }
            if (label == null) {
                int colon = run.text().indexOf(':');
                if (colon < 0) {
                    continue;                  // 冒號前面的「- 」那類，跳過
                }
                if (hasContent(run.text().substring(colon + 1))) {
                    return List.of();          // 兩半同色，沒得分
                }
                label = undecorated(run.style());
            } else if (hasContent(run.text())) {
                value = undecorated(run.style());
                break;
            }
        }
        if (label == null || value == null || label.equals(value)) {
            return List.of();
        }
        int at = translated[0].indexOf(':');
        int wide = translated[0].indexOf('：');
        if (at < 0 || (wide >= 0 && wide < at)) {
            at = wide;                         // 譯文的冒號常常是全形的
        }
        if (at < 0) {
            return List.of();
        }
        List<LineParts.Piece> out = new ArrayList<>();
        add(out, translated[0].substring(0, at + 1), label, blockStyle, known);
        add(out, translated[0].substring(at + 1), value, blockStyle, known);
        return out;
    }

    /**
     * 佔位符之間的每一段，各自沿用原文<b>對應那一段</b>的顏色。
     *
     * <h2>先前壞在哪</h2>
     * 技能的「Total Damage: 2400% (of your DPS, Attack)」是三個顏色：標籤淺灰、
     * 數值白、括號深灰。{@link #labelValueAccents} 照冒號切兩半，後半整段套上
     * 數值的白色，括號就跟著變亮。「grant 2048 Emeralds」的 grant 是粉紅、
     * 其餘是白；混色的行只拿得到多數色，譯文的「給予」就成了白的。
     *
     * <h2>做法</h2>
     * 佔位符（{@code {~}}、{@code {#}}、{@code {p}}、{@code {u}}）在原文與譯文裡
     * 是同一組錨點。兩邊照錨點切開，第 k 段對第 k 段：原文那一段只有一個顏色，
     * 譯文那一段就貼上那個顏色。
     *
     * <h2>何時不做</h2>
     * 行數不同、佔位符的種類或順序不同（譯者搬動了數值）、某一段一邊有字一邊沒字、
     * 譯文自己寫了色碼——這些情況第 k 段指的不是同一件事，照舊交給原本的規則。
     * 原文每一段都同色的行也不做，那本來就沒有東西要分。
     *
     * @return 沒有任何一行適用時回傳 {@code null}；適用但不必多登記時回傳空的
     */
    private static List<LineParts.Piece> segmentAccents(
            List<LineParts> parts, String[] translated, Style blockStyle,
            List<LineParts.Piece> known) {
        String[] dst = String.join(NL, translated).split(NL, -1);
        if (dst.length != parts.size()) {
            return null;
        }
        List<LineParts.Piece> out = new ArrayList<>();
        boolean applied = false;
        StringBuilder earlier = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            List<Segment> source = segments(parts.get(i).template());
            List<Segment> target = segments(dst[i]);
            List<List<LineParts.Piece>> styles = source == null
                    ? null : segmentStyles(source, parts.get(i).runs());
            if (styles == null || target == null || !sameShape(source, target)
                    || usableStyles(source, target, styles) < 2) {
                earlier.append(dst[i]).append(NL);
                continue;
            }
            applied = true;
            for (int k = 0; k < target.size(); k++) {
                String core = target.get(k).text().strip();
                // 前面已經出現過同樣的字面，貼樣式那一步會先貼到前面去，不登記
                StringBuilder before = new StringBuilder(earlier);
                // 譯者在這一段裡多加了字時，顏色只給原文那個名稱。見 #nameInside。
                String name = nameInside(source.get(k).text().strip(), styles.get(k),
                                         core, known);
                List<LineParts.Piece> pieces = name != null
                        ? List.of(new LineParts.Piece(name, styles.get(k).get(0).style()))
                        : assign(source.get(k).text().strip(), styles.get(k), core);
                for (LineParts.Piece piece : pieces) {
                    if (hasContent(piece.text()) && before.indexOf(piece.text()) < 0) {
                        add(out, piece.text(), piece.style(), blockStyle, known);
                    }
                    before.append(piece.text());
                }
                earlier.append(target.get(k).text());
            }
            earlier.append(NL);
        }
        return applied ? out : null;
    }

    /**
     * 原文這一段只有一個顏色、譯文那一段卻多了別的字時，找出譯文裡真正對應原文的那個名稱。
     *
     * <h2>0.1.9_4 實機回報</h2>
     * 法師技能 Diffraction（晶化蔓延）：「Ophanim also applies +2 Crystallized {#}.」，
     * 水藍色的只有 Crystallized。譯文「Ophanim 也會施加 {~} 層 Crystallized {#}.」照佔位符切段，
     * {@code {~}} 與 {@code {#}} 之間那一段是「 層 Crystallized 」——整段登記成水藍色，
     * 譯者加的量詞「層」就跟著變藍，畫面上是「2 層結晶化」四個字都是藍的。
     *
     * <p>原文那一段<b>就是</b>名稱本身（或它的譯名，見 {@link #withTranslations}）而且在
     * 譯文那一段裡找得到時，只登記那個名稱；其餘的字照正文的顏色。找不到（譯者把整段
     * 重寫了）才照舊整段上色——那時分不出哪幾個字對應原文。
     *
     * @return 要上色的名稱；不適用時回傳 {@code null}
     */
    private static String nameInside(String source, List<LineParts.Piece> runs, String target,
                                     List<LineParts.Piece> known) {
        if (runs.size() != 1 || target.equals(source)) {
            return null;
        }
        Style style = runs.get(0).style();
        List<String> names = new ArrayList<>();
        names.add(source);
        for (LineParts.Piece piece : known) {
            if (java.util.Objects.equals(piece.style(), style)) {
                names.add(piece.text().strip());
            }
        }
        String best = null;
        for (String name : names) {
            if (hasContent(name) && !target.equals(name) && target.contains(name)
                    && (best == null || name.length() > best.length())) {
                best = name;
            }
        }
        return best;
    }

    /** 被佔位符切開的一段文字，連同緊接在它後面的佔位符種類（最後一段是 {@code null}）。 */
    private record Segment(String text, String anchor) {}

    /**
     * 照佔位符切段。
     *
     * @return 遇到色碼或認不得的佔位符、或 {@code {~N}} 不是照 1、2、3 的順序時回傳 {@code null}
     */
    private static List<Segment> segments(String line) {
        List<Segment> out = new ArrayList<>();
        java.util.regex.Matcher m = PLACEHOLDER.matcher(line);
        int from = 0;
        int plainNumbers = 0;
        int nextIndex = 1;
        while (m.find()) {
            String token = m.group();
            String anchor;
            if (token.equals(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                anchor = "#";
            } else if (token.equals(GlyphSplitter.PLACE_PLACEHOLDER)) {
                anchor = "p";
            } else if (token.equals(GlyphSplitter.PLAYER_PLACEHOLDER)) {
                anchor = "u";
            } else if (token.equals("{~}")) {
                anchor = "~";
                plainNumbers++;
            } else if (token.matches("\\{~\\d+\\}")) {
                // 指名第幾個的寫法，只接受照順序寫的——換過順序就對不上段落了
                if (Integer.parseInt(token.substring(2, token.length() - 1)) != nextIndex++) {
                    return null;
                }
                anchor = "~";
            } else {
                return null;
            }
            out.add(new Segment(line.substring(from, m.start()), anchor));
            from = m.end();
        }
        if (plainNumbers > 0 && nextIndex > 1) {
            return null;
        }
        out.add(new Segment(line.substring(from), null));
        return out;
    }

    /** 兩邊的佔位符順序相同，而且每一段「有沒有字」也相同。 */
    private static boolean sameShape(List<Segment> source, List<Segment> target) {
        if (source.size() != target.size()) {
            return false;
        }
        for (int k = 0; k < source.size(); k++) {
            if (!java.util.Objects.equals(source.get(k).anchor(), target.get(k).anchor())
                    || hasContent(source.get(k).text()) != hasContent(target.get(k).text())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 原文每一段依顏色切開的小段；整段同色就只有一個，空的段落是空清單。
     *
     * <p>模板裡的文字就是各片段接起來、數值與地名換成佔位符的樣子，
     * 所以每一段都能依序在片段的原文裡找到。
     *
     * @return 某一段找不到時回傳 {@code null}
     */
    private static List<List<LineParts.Piece>> segmentStyles(List<Segment> source,
                                                              List<LineParts.Piece> runs) {
        StringBuilder plain = new StringBuilder();
        List<Style> styleAt = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            for (int c = 0; c < run.text().length(); c++) {
                plain.append(run.text().charAt(c));
                styleAt.add(run.style());
            }
        }
        List<List<LineParts.Piece>> out = new ArrayList<>(source.size());
        int cursor = 0;
        for (Segment segment : source) {
            String core = segment.text().strip();
            if (core.isEmpty()) {
                out.add(List.of());
                continue;
            }
            int at = plain.indexOf(core, cursor);
            if (at < 0) {
                return null;
            }
            List<LineParts.Piece> pieces = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            Style current = null;
            for (int c = at; c < at + core.length(); c++) {
                char ch = plain.charAt(c);
                if (!Character.isWhitespace(ch)) {
                    Style style = styleAt.get(c);
                    if (current != null && !java.util.Objects.equals(current, style)) {
                        pieces.add(new LineParts.Piece(text.toString().strip(), current));
                        text.setLength(0);
                    }
                    current = style;
                }
                text.append(ch);
            }
            pieces.add(new LineParts.Piece(text.toString().strip(), current));
            out.add(pieces);
            cursor = at + core.length();
        }
        return out;
    }

    /**
     * 這一行真正貼得上的顏色有幾種。
     *
     * <p>整段同色、譯文那段有字的算；混色的段落只有譯文<b>照抄原文</b>時才算。
     * 少於兩種就沒有東西要分，交回原本的規則——「Type: Grinding Mobs」
     * 這種整行翻掉、沒有佔位符的行，冒號那一刀才輪得到。
     */
    private static int usableStyles(List<Segment> source, List<Segment> target,
                                    List<List<LineParts.Piece>> styles) {
        java.util.Set<Style> distinct = new java.util.HashSet<>();
        for (int k = 0; k < target.size(); k++) {
            for (LineParts.Piece piece : assign(source.get(k).text().strip(), styles.get(k),
                                                target.get(k).text().strip())) {
                distinct.add(piece.style());
            }
        }
        return distinct.size();
    }

    /**
     * 譯文的某一段該貼哪些顏色。
     *
     * <ul>
     *   <li>原文那段同色：譯文整段貼那個顏色。</li>
     *   <li>混色但譯文照抄原文（「✖ Lv.」）：逐段照原文貼。</li>
     *   <li>混色、翻掉了，但兩邊的<b>標點一模一樣</b>（「Expired - Sold」→「已過期 - 已售出」）：
     *       照標點切開，第 i 塊對第 i 塊。</li>
     * </ul>
     *
     * @param runs 原文那段依顏色切開的小段，見 {@link #segmentStyles}
     * @return 空清單代表這一段對不上
     */
    private static List<LineParts.Piece> assign(String source, List<LineParts.Piece> runs,
                                                String target) {
        if (runs.size() == 1) {
            return hasContent(target) ? List.of(new LineParts.Piece(target, runs.get(0).style()))
                                      : List.of();
        }
        if (runs.size() < 2) {
            return List.of();
        }
        if (target.equals(source)) {
            return runs;
        }
        return punctuationSplit(source, runs, target);
    }

    /** 見 {@link #assign}：兩邊照同樣的標點切開、逐塊對色。對不上就回傳空清單。 */
    private static List<LineParts.Piece> punctuationSplit(String source,
                                                          List<LineParts.Piece> runs,
                                                          String target) {
        // 把小段放回原文的位置，得到每個字元的顏色
        Style[] at = new Style[source.length()];
        int cursor = 0;
        for (LineParts.Piece run : runs) {
            int found = source.indexOf(run.text(), cursor);
            if (found < 0) {
                return List.of();
            }
            java.util.Arrays.fill(at, found, found + run.text().length(), run.style());
            cursor = found + run.text().length();
        }
        List<int[]> ours = punctuationSpans(source);
        List<int[]> theirs = punctuationSpans(target);
        if (!samePunctuation(source, ours, target, theirs)) {
            // 標點對不上時只拿括號當錨點再試一次。
            //
            // 技能面板的「Area of Effect: 7 Blocks (Circle-Shaped)」：原文括號裡的
            // 連字號也算標點，譯文「格 (圆形)」沒有，數量一不同就整行放棄，
            // 退回「冒號後面整段是數值色」——括號跟著變白，本來是灰的。
            // 括號內的逗號被換成全形、俄文多一個縮寫點，也是同一回事。
            ours = bracketSpans(source);
            theirs = bracketSpans(target);
            if (!samePunctuation(source, ours, target, theirs)) {
                return List.of();
            }
        }
        List<LineParts.Piece> out = new ArrayList<>();
        int from = 0;
        int dstFrom = 0;
        for (int i = 0; i <= ours.size(); i++) {
            int to = i < ours.size() ? ours.get(i)[0] : source.length();
            int dstTo = i < theirs.size() ? theirs.get(i)[0] : target.length();
            String piece = target.substring(dstFrom, dstTo).strip();
            if (hasContent(source.substring(from, to)) != hasContent(piece)) {
                return List.of();              // 一邊有字一邊沒有，切法不是同一回事
            }
            Style style = uniformStyle(source, at, from, to);
            if (style != null && hasContent(piece)) {
                out.add(new LineParts.Piece(piece, style));
            }
            if (i < ours.size()) {
                Style mark = uniformStyle(source, at, ours.get(i)[0], ours.get(i)[1]);
                if (mark != null) {
                    out.add(new LineParts.Piece(
                            source.substring(ours.get(i)[0], ours.get(i)[1]), mark));
                }
                from = ours.get(i)[1];
                dstFrom = theirs.get(i)[1];
            }
        }
        return out;
    }

    /** 連續的標點符號（不是字母、數字、空白）各自的起訖位置。 */
    private static List<int[]> punctuationSpans(String text) {
        List<int[]> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int start = i;
            while (i < text.length() && !Character.isLetterOrDigit(text.charAt(i))
                    && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            out.add(new int[] {start, i});
        }
        return out;
    }

    /** 兩邊的標點段數量相同、而且逐組相同；全形括號當成半形比。 */
    private static boolean samePunctuation(String source, List<int[]> ours,
                                           String target, List<int[]> theirs) {
        if (ours.isEmpty() || ours.size() != theirs.size()) {
            return false;
        }
        for (int i = 0; i < ours.size(); i++) {
            String a = halfWidthBrackets(source.substring(ours.get(i)[0], ours.get(i)[1]));
            String b = halfWidthBrackets(target.substring(theirs.get(i)[0], theirs.get(i)[1]));
            if (!a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    /** 每一個括號字元自成一段；其他標點不算。 */
    private static List<int[]> bracketSpans(String text) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(' || c == ')' || c == '（' || c == '）') {
                out.add(new int[] {i, i + 1});
            }
        }
        return out;
    }

    private static String halfWidthBrackets(String text) {
        return text.replace('（', '(').replace('）', ')');
    }

    /** 這一段實字的顏色都一樣的話回傳那個顏色，否則 {@code null}。 */
    private static Style uniformStyle(String text, Style[] at, int from, int to) {
        Style only = null;
        for (int c = from; c < to; c++) {
            if (Character.isWhitespace(text.charAt(c))) {
                continue;
            }
            if (at[c] == null || (only != null && !java.util.Objects.equals(only, at[c]))) {
                return null;
            }
            only = at[c];
        }
        return only;
    }

    /** 見 {@link #labelValueAccents}：剝掉佔位符之後還有字才登記。 */
    private static void add(List<LineParts.Piece> out, String text, Style style,
                            Style blockStyle, List<LineParts.Piece> known) {
        String bare = PLACEHOLDER.matcher(text).replaceAll("").strip();
        if (!hasContent(bare) || java.util.Objects.equals(style, blockStyle)
                || covered(known, bare, style)) {
            return;
        }
        out.add(new LineParts.Piece(bare, style));
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
        // 方括號裡的字不投票，跟 #dominantStyle 同一把尺——兩邊用不同的尺
        // 就會打架：整段的底色挑了散文那色，這裡的 dominant 卻挑了括號那色，
        // 於是 #wholeLineAccents 拿括號那色把<b>整行</b>蓋掉。實機那條
        // 「+ New Quest [Mini-Quest - Slay Spiders]」整行變深灰就是這樣來的。
        int[] depth = {0};
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
                // 從這個 run 的開頭重讀，所以深度得從<b>進這個 run 時</b>算起。
                int[] local = {depth[0]};
                tally.add(run.style(), proseAmong(run.text(), eaten, take, local));
                need -= take;
                if (take == have) {
                    depth[0] = local[0];   // 整個 run 讀完了，深度帶到下一個
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

    /**
     * 一行裡每個顏色各佔幾個實字。見 {@link #fallback}。
     *
     * <h2>累計時不看底線、粗體這些裝飾</h2>
     * 0.1.9_4 實機回報：法師技能 Arcane Speed 的第二行原文是
     * 「casting <u>Heal</u> or <u>Arcane Transfer</u>.」——底線的技能名 18 個字，
     * 灰色的正文只有 10 個字。照樣式分開算，「灰＋底線」就成了這一行的多數色，
     * {@link #fallback} 把它整行套到譯文的第二行，畫面上是
     * 「使用<u>治療</u>和<u>祕法回流</u>」換行「<u>額外獲得移動速度。</u>」。
     *
     * <p>裝飾標的是<b>詞</b>（技能名、重點詞），從來不是整行的底色；而且譯文是我們自己
     * 折回原文行數的，「第 i 行」兩邊本來就不是同一段字。所以跟 {@link #dominantStyle}
     * 一樣併成同一個顏色來數，贏的時候回傳<b>沒有裝飾的那一個</b>原樣式。
     */
    private static final class Tally {
        private final java.util.Map<Style, Integer> counts = new java.util.LinkedHashMap<>();
        /** 每個顏色實際要回傳的樣式：有沒裝飾的就用它 */
        private final java.util.Map<Style, Style> shown = new java.util.HashMap<>();

        void add(Style style, int solid) {
            if (style != null && solid > 0) {
                Style key = undecorated(style);
                counts.merge(key, solid, Integer::sum);
                Style seen = shown.get(key);
                if (seen == null || (hasDecoration(seen) && !hasDecoration(style))) {
                    shown.put(key, style);
                }
            }
        }

        /** 這幾個實字不算數；扣到零就整個拿掉。見 {@link LineTranslator#perPartStyles}。 */
        void remove(Style style, int solid) {
            if (style == null || solid <= 0) {
                return;
            }
            Style key = undecorated(style);
            Integer have = counts.get(key);
            if (have == null) {
                return;
            }
            if (have <= solid) {
                counts.remove(key);
                shown.remove(key);
            } else {
                counts.put(key, have - solid);
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
            return best == null ? null : shown.get(best);
        }

        private static boolean hasDecoration(Style style) {
            return style.isUnderlined() || style.isBold() || style.isItalic()
                    || style.isStrikethrough() || style.isObfuscated();
        }
    }

    /** 非空白、非圖示的字元數。 */
    private static int solidCount(String text) {
        return (int) text.codePoints().filter(cp -> !Character.isWhitespace(cp)
                && !com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)).count();
    }

    /**
     * tooltip 那一路：本來就一行一個 {@link LineParts}，直接看每一份自己的片段。
     *
     * <h2>多數色不算佔位符</h2>
     * 數值、地名、玩家名是<b>從原文抽出來、原樣填回去</b>的（見 {@link LineParts}），
     * 而且各自帶著自己的樣式回來。它們在譯文裡想搬到哪一行就搬到哪一行，
     * 所以不能讓它們決定「這一行的字是什麼顏色」。
     *
     * <h2>實機回報（內容書「The Missing Piece」任務卡）</h2>
     * 原文兩行，座標是白的、說明是灰的：
     *
     * <pre>
     *   §7Pick up your post in the Post
     *   §7Office at §f[-2156, 30, -944]
     * </pre>
     *
     * 中文把座標搬到句子中間（「到 [-{~}, {~}, -{~}] 的郵局領取你的郵件。」），
     * 折回兩行之後第二行是「的郵局領取你的郵件。」——整句<b>灰色</b>的那一半。
     *
     * <p>原文第二行混了兩個顏色，{@link #fallback} 於是退而求其次拿多數色：
     * 灰的「Office at」只有 9 個實字，白的「[-2156, 30, -944]」有 15 個，白贏。
     * 畫面上就是座標後面的說明整段變白——使用者回報的正是這個。
     *
     * <p>那 15 個字裡有 9 個是數值本身，而數值早就另外保管、會自己帶白色回來。
     * 扣掉之後白的只剩 {@code [-,,-]} 6 個，灰的 9 個贏——跟肉眼看到的一致。
     * 這也讓這條路跟 {@link #uniformStyles} 一致：那邊是照模板去掉佔位符之後
     * 的實字數在量的，本來就不含數值。
     */
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
            // 抽出去的佔位符不算數，見上。符號（{#}）不必扣——solidCount 本來就不數。
            for (List<LineParts.Piece> pool
                    : List.of(part.numbers(), part.places(), part.users())) {
                for (LineParts.Piece piece : pool) {
                    tally.remove(piece.style(), solidCount(piece.text()));
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
        // 佔位符之間的每一段各自沿用原文那一段的顏色。見 #segmentAccents。
        // 對得上的話它比「標籤: 數值」切得細，冒號那一刀就不必再切——
        // 那一刀會把數值後面的括號註解一起染成數值的顏色。
        List<LineParts.Piece> segmented = segmentAccents(parts, translated, blockStyle, accents);
        if (segmented != null) {
            accents.addAll(segmented);
        } else {
            // 「標籤: 數值」兩半各自的顏色。見 #labelValueAccents。
            accents.addAll(labelValueAccents(parts, translated, blockStyle, accents));
        }
        // 整行同色的那幾行，直接拿譯文那一行當重點段。見 #wholeLineAccents。
        accents.addAll(wholeLineAccents(parts, allRuns, translated, blockStyle, accents));
        // 方括號對方括號，照順序配。查表查不到的那幾種（等級、座標、
        // 只收了整行的那種）靠這一條拿回顏色。見 #bracketAccents。
        accents.addAll(bracketAccents(allRuns, translated, blockStyle));

        // 斷行不要把一個重點詞切成兩半，否則它的顏色會整個掉。見 keepAccentsWhole。
        String[] flowed = keepAccentsWhole(translated, accents);
        // 搬不動的那些，兩半各自登記一份。見 halvesAcrossBreaks。
        accents.addAll(halvesAcrossBreaks(flowed, accents));

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
        List<Style> words = wordPalette(allRuns);
        // 進度條的顏色是<b>資料</b>，見 #bars。
        List<Bar> bars = bars(allRuns);
        boolean[] usedBar = new boolean[bars.size()];
        FlowedDebug.palette(palette, allRuns, flowed);

        // 譯者指定的顏色管到 {/} 或下一個 {cN} 為止，<b>可以跨行</b>。
        //
        // 這裡原本每一行都會收掉，理由是「忘了寫 {/} 只會影響那一行」。
        // 但翻譯團隊第一次用就踩到這個限制：一句話被 tooltip 切成兩行，
        // 在第一行開色段、想一路染到第二行，結果第二行整個掉回底色，
        // 而寫在第二行的 {/} 也變成空操作。跨行才是譯者預期的行為，
        // 也跟上面 inNote 的處理一致——註解同樣會被切成兩行。
        //
        // 註：這裡曾經寫著「忘了寫 {/} 由 validate.py 擋下」——那是錯的。
        // validate <b>刻意不報</b>色段沒關：一路染到整段結尾是正當寫法，
        // 而且作用範圍止於這一條，不會外溢到別的條目（見 validate.py 那段註解）。
        //
        // 真正會出事的是「該在中間收、卻一路染到結尾」，例如
        // {@code {w2}地屬性{/}傷害} 少了 {@code {/}}，「傷害」就跟著變成元素色。
        // 那要看原文的顏色分段才判斷得出來，語料裡沒有那份資料，靜態檢查不到。
        // 2026-09-12 有人照著舊註解去找那條不存在的檢查。
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
                    case COLOR -> forced = colourOf(token, palette, words, textStyle);
                    case GLYPH -> {
                        // 符號連同原樣式（含自訂字型）整段搬回，這是它顯示得出來的唯一方式
                        LineParts.Piece piece = glyphs.get(glyph++);
                        line.append(Component.literal(piece.text()).withStyle(piece.style()));
                        justFilled = null;    // 見 #appendHugging：標點不跟符號走
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
                        line.append(literal(piece.text(), numberDisplay(piece.style())));
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
                        // 正負號跟著後面那個<b>放大的</b>數值走字型。
                        //
                        // 原文的「+4,250 Health」整串數字（含 + 號）都在
                        // offset/wynncraft_quad 這種放大字型裡。譯文把數值拆成
                        // {~}，+ 號留在文字那半，於是只有它縮回一般大小——
                        // 實機回報「+ 是小的」。
                        String text = token.text();
                        Style bigNext = i + 1 < tokens.size()
                                && tokens.get(i + 1).kind() == Kind.NUMBER
                                ? displayFont(peekStyle(tokens.get(i + 1), glyphs, places,
                                              numbers, users, glyph, place, number, user))
                                : null;
                        if (bigNext != null && !text.isEmpty()
                                && (text.endsWith("+") || text.endsWith("-"))) {
                            String head = text.substring(0, text.length() - 1);
                            if (!head.isEmpty()) {
                                appendHugging(line, head, textStyle, symbolStyle,
                                              noteStyle, inNote, accents, usedAccent,
                                              store, i > 0 ? justFilled : null, null,
                                              afterNumber);
                            }
                            line.append(literal(text.substring(text.length() - 1), bigNext));
                            justFilled = null;
                            afterNumber = false;
                            break;
                        }
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
                            // 進度條先攔下來，剩下的兩頭照舊走猜的那條路。
                            int[] span = barSpan(token.text());
                            Bar bar = span == null ? null
                                    : takeBar(bars, usedBar, token.text().charAt(span[0]),
                                              span[1] - span[0]);
                            if (bar == null) {
                                appendHugging(line, token.text(), textStyle, symbolStyle,
                                              noteStyle, inNote, accents, usedAccent,
                                              store, before, after, afterNumber);
                            } else {
                                String head = token.text().substring(0, span[0]);
                                String tail = token.text().substring(span[1]);
                                if (!head.isEmpty()) {
                                    appendHugging(line, head, textStyle, symbolStyle,
                                                  noteStyle, inNote, accents, usedAccent,
                                                  store, before, null, afterNumber);
                                }
                                appendBar(line, bar);
                                if (!tail.isEmpty()) {
                                    appendHugging(line, tail, textStyle, symbolStyle,
                                                  noteStyle, inNote, accents, usedAccent,
                                                  store, null, after, false);
                                }
                            }
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
            case GLYPH -> null;            // 見 #appendHugging：標點不跟符號走
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
     *
     * <h2>{@code {#}} 不算「前一個佔位符」</h2>
     * 黏著的前提是「原文裡那個標點跟佔位符同屬一個片段」——數值、地名、玩家名
     * 都是從一段文字裡挖出來的，挖出來之前確實跟旁邊的標點同色。符號不是：
     * {@code GlyphSplitter} 一定會把它切成自己的片段，所以標點<b>從來沒有</b>
     * 跟符號同過一段，黏過去只是把符號那一段的顏色平白搬給標點。
     *
     * <p>實機回報的是任務完成的橫幅：原文 {@code 󐁴§6[Quest Completed]} 整串金色，
     * 但那個排版符號自己不帶顏色（{@code §6} 在它<b>後面</b>才開始）。譯文
     * {@code {#}[任務完成]} 的左中括號緊貼著 {@code {#}}，於是拿到符號那一段的
     * 白色，畫面上就是「白色的 [ 配金色的字」。
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
        // 兩個佔位符中間整段都是標點空白、而且兩邊同色時，整段跟著那個顏色。
        //
        // 座標就是這個形狀：原文 `[-1621, 50, -4664]` 整串白色，譯文寫成
        // `[-{~}, {~}, -{~}]`，中間那兩段是「, 」。逗號會黏在前一個數值上
        // （見 #hugs），但空白不黏——「中間有空白就不算黏著」那條規則管的是
        // 詞距，而這裡整段都不是詞。結果是座標裡兩個空白掉回散文的灰色，
        // 畫面上白色的座標中間夾著兩格灰。
        //
        // 兩邊同色才做：顏色不同的時候這一段該歸誰本來就有歧義，交給底下
        // 逐邊黏的規則處理。
        if (before != null && after != null && sameColour(before, after)
                && !hasLetter(text)) {
            out.append(literal(text, forDisplay(before)));
            return;
        }
        int lead = 0;
        if (before != null) {
            while (lead < text.length() && hugs(text.charAt(lead))
                    && !hasOwnColour(text, lead, accents, used, before)) {
                lead++;
            }
        }
        int tail = text.length();
        if (after != null) {
            while (tail > lead && hugs(text.charAt(tail - 1))
                    && !hasOwnColour(text, tail - 1, accents, used, after)) {
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
            // 這個符號在原文裡自己是一段、而且登記成重點段時，顏色用它自己的。
            //
            // 市集那一列：原文的兩顆綠寶石各自比前面的數字暗一階（白配灰、
            // 亮青配暗青），而符號那一段的樣式整行只有一個，套下去兩顆會變成
            // 同一個顏色。字型仍然沿用符號那一段的——要換的只有顏色。
            Style paint = symbol;
            int own = exactAccent(text.substring(lead, mid), accents, used);
            if (own >= 0) {
                used[own] = true;
                Style mine = accents.get(own).style();
                paint = mine == null || paint == null ? forDisplay(mine)
                        : paint.withColor(mine.getColor());
            }
            out.append(literal(text.substring(lead, mid), paint));
            lead = mid;
        }
        // 結尾那個符號跟著<b>後面</b>那個佔位符走。
        //
        // 市集那一列的原文是 `✮ 4,218` 一整段青色——星號跟它右邊的數字同屬一段，
        // 中間那個空格也是那一段的。譯文寫 `✮ {~}`，數值被抽出去之後那條字面
        // 對不上，星號就掉回底色，畫面上是「金色的星配青色的數字」。
        //
        // 「中間有空白就不算黏著」那條規則（見 #hugs 的說明）管的是標點；
        // 遊戲的符號不一樣，它跟它標註的那個值之間本來就留著一格。
        int back = tail;
        if (after != null) {
            int end = tail;
            while (end > lead && text.charAt(end - 1) == ' ') {
                end--;
            }
            int start = end;
            while (start > lead && isPictograph(text.charAt(start - 1))) {
                start--;
            }
            // 符號自己有一段顏色的不搬——那一段等一下會自己貼上去。
            if (start < end && exactAccent(text.substring(start, end), accents, used) < 0) {
                back = start;
            }
        }
        if (back > lead) {
            appendNoting(out, text.substring(lead, back), base, note,
                         depth, accents, used, store);
        }
        if (back < tail) {
            out.append(literal(text.substring(back, tail), forDisplay(after)));
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

    /** 字面剛好就是 {@code word} 的那個還沒用掉的重點段；沒有就是 -1。 */
    private static int exactAccent(String word, List<LineParts.Piece> accents,
                                   boolean[] used) {
        for (int k = 0; k < accents.size(); k++) {
            if (!used[k] && accents.get(k).text().equals(word)) {
                return k;
            }
        }
        return -1;
    }

    /**
     * 這個位置的標點<b>自己就有</b>顏色嗎。
     *
     * <h2>為什麼要問</h2>
     * 黏著的前提是「原文裡那個標點跟佔位符同屬一個片段」。重點段的存在正好
     * 說明相反的事：原文<b>另外</b>給了它一段自己的樣式，黏過去等於把它抹掉。
     *
     * <p>實機回報的是市集那一列。原文
     * {@code 4,300² ✮ 4,218² (1¼² 1²½ 58²) each} 切成
     * {@code 「4,300」白「²」灰「✮ 4,218」青「²」暗青「(…)」深灰}——
     * 綠寶石那個符號比它前面的數字暗一階，兩顆都是。譯文的
     * {@code {~}²} 讓 {@code ²} 緊貼著數值，於是它拿到數值的顏色，
     * 診斷檔記的是「{@code ²} ★在譯文裡卻沒貼上」兩次。
     *
     * <p>顏色一樣的不算——那種黏不黏都畫得出同一個結果，少繞一圈。
     */
    private static boolean hasOwnColour(String text, int at,
                                        List<LineParts.Piece> accents, boolean[] used,
                                        Style side) {
        for (int k = 0; k < accents.size(); k++) {
            if (used[k]) {
                continue;
            }
            String word = accents.get(k).text();
            if (!word.isEmpty() && text.startsWith(word, at)
                    && !sameColour(accents.get(k).style(), side)) {
                return true;
            }
        }
        return false;
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
                // 圖示前面若是「+{~} 」，那個數值也是這個屬性的，一起搬。
                //
                // 實機回報的 Heavensent：折行斷在「+{~1} {#}防／禦」，這裡把「防」
                // 連同圖示搬下去，數值卻留在上一行——「每提供一個信標就 +2」換行
                // 「✤防禦 (上限 x15)」。見 keepValueWithGlyph。
                move = out[i].length()
                        - keepValueWithGlyph(out[i], out[i].length() - move, 0);
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

    /**
     * 搬不動的重點段，被斷行切開的<b>兩半各自</b>登記一份。
     *
     * <h2>為什麼還需要這一條</h2>
     * {@link #keepAccentsWhole} 只搬得動十來個字以內的詞，
     * 而且含佔位符的那一半一律不搬。搬不動的就留在原地被切成兩半，兩行各自都
     * 對不上整段的字面——整塊掉回底色。
     *
     * <p>實機回報的是採集站那張卡：{@code [{~} Kanderstone 寶石]} 十七個字，
     * 斷行落在「寶」「石」之間。要搬得動得把「{@code  Kanderstone 寶}」十五個
     * 字整串挪到下一行，那會把面板撐寬一大截，不值得——但顏色不能就這樣掉。
     *
     * <p>所以改成認這一刀：上一行結尾是 {@code left}、下一行開頭是
     * {@code right} 時，兩半各登記一次，畫的時候各貼各的。
     *
     * <h2>何時不登記</h2>
     * 兩頭都至少要兩個字元、而且要帶實字——單一個字元到處都是，貼上去會貼到
     * 別的地方去。含佔位符的那一半也不登記：畫的時候佔位符自成一個片段，
     * 那一串字面在畫面上從來不連續。
     */
    static List<LineParts.Piece> halvesAcrossBreaks(
            String[] flowed, List<LineParts.Piece> accents) {
        List<LineParts.Piece> out = new ArrayList<>();
        if (flowed.length < 2) {
            return out;
        }
        for (LineParts.Piece accent : accents) {
            String word = accent.text();
            if (word.length() < 4) {
                continue;                      // 切開之後兩半都太短
            }
            for (int i = 0; i + 1 < flowed.length; i++) {
                int cut = cutBetween(flowed[i], flowed[i + 1], word);
                if (cut <= 0) {
                    continue;
                }
                addHalf(out, word.substring(0, cut), accent.style());
                addHalf(out, word.substring(cut), accent.style());
                break;
            }
        }
        return out;
    }

    /**
     * {@code word} 被這兩行切在第幾個字元；沒被切開就是 0。
     *
     * <p>切在第一個字元也算：{@code [{~} 鮭魚肉]} 斷在 {@code [} 後面時，
     * 左半只有一個字元登記不了，右半照樣得救。要不要登記交給
     * {@link #addHalf} 判斷。
     */
    private static int cutBetween(String head, String tail, String word) {
        for (int cut = 1; cut < word.length(); cut++) {
            if (head.endsWith(word.substring(0, cut))
                    && tail.startsWith(word.substring(cut))) {
                return cut;
            }
        }
        return 0;
    }

    private static void addHalf(List<LineParts.Piece> out, String half, Style style) {
        if (half.length() >= 2 && half.indexOf('{') < 0 && half.indexOf('}') < 0
                && hasLetter(half)) {
            out.add(new LineParts.Piece(half, style));
        }
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
            //
            // <b>一樣長也算詞典贏。</b>技能名稱在原文裡本來就帶著自己的樣式
            // （`Bash` 是灰色加底線），所以它同時是重點段、也是詞典裡的詞——
            // 同一個位置、同樣長。用「嚴格比較長」的話重點段勝，而重點段做的事
            // 是<b>原樣貼回英文</b>，於是技能名永遠換不掉：意象敘述上是
            // 「Bash 的技能範圍增加」，而對照表寫的是「重擊」。使用者回報的正是這個。
            //
            // 樣式不會因此丟掉——底下那段會把同一個位置的重點段樣式套到譯文上。
            boolean longer = term != null && at >= 0 && term.start() == at
                    && term.end() - term.start() >= accents.get(which).text().length();
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
            if (shown == null && store != null) {
                // 重點段不一定<b>就是</b>那個詞，可能只是包著它。見 #termsWithin。
                shown = termsWithin(accent.text(), store);
            }
            out.append(literal(shown != null ? shown : accent.text(),
                               forDisplay(accent.style())));
            used[which] = true;
            from = at + accent.text().length();
        }
    }

    /**
     * 把一段重點段<b>裡面</b>的技能名稱換掉，樣式由呼叫端照舊套上。
     *
     * <h2>實機回報</h2>
     * 法師技能 Diffraction 的敘述畫出來是「奧法尼姆也會施加 2 層 Crystallized」，
     * 藍色的 Crystallized 留著英文，而詞表裡明明有「結晶化」，上一行也換掉了。
     *
     * <h2>怎麼漏的</h2>
     * 這一行是整行命中語料的（「Ophanim 也會施加 {~} 層 Crystallized {#}.」），
     * 上色靠 {@link #segmentAccents}：原文與譯文照佔位符切段，第 k 段貼第 k 段的顏色。
     * {@code {~}} 與 {@code {#}} 之間那一段在譯文裡是「 層 Crystallized 」，
     * 於是登記進來的重點段是<b>包著</b>技能名的一整截，而不是那個詞本身。
     *
     * <p>{@link #appendText} 裡重點段比詞表先開始（位置較前），走的是重點段那條路；
     * 那條路只問「整段是不是一個詞」，不是就原樣貼回英文。上一行換得掉，
     * 是因為那一段剛好就只有「Crystallized」。
     *
     * @return 換過的文字；裡面沒有任何詞表裡的名稱時回傳 {@code null}
     */
    static String termsWithin(String text, TranslationStore store) {
        StringBuilder out = new StringBuilder();
        int from = 0;
        boolean any = false;
        TranslationStore.Term term;
        while (from < text.length() && (term = store.findTerm(text, from)) != null) {
            String before = text.substring(from, term.start());
            // 跟 appendText 同一套空格規則：「層 結晶化」的半形空格是給英文用的
            if (dropsSpaceBefore(before, term.translation())) {
                before = before.substring(0, before.length() - 1);
            }
            out.append(before).append(term.translation());
            from = term.end();
            if (dropsSpaceAfter(term.translation(), text, from)) {
                from++;
            }
            any = true;
        }
        return any ? out.append(text.substring(Math.min(from, text.length()))).toString()
                   : null;
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
                // 原文那一段整塊被方括號包起來的話，括號也是它的顏色。見 #wrapLikeSource。
                String wrapped = wrapLikeSource(core, zh, translated);
                out.add(new LineParts.Piece(wrapped != null ? wrapped : zh, accent.style()));
            }
        }
        return out;
    }

    /**
     * 原文的色段<b>整塊</b>被方括號包起來，而譯文裡查到的是去掉括號的內層時，
     * 把重點段往外擴到括號。
     *
     * <h2>實機回報（內容書右邊那張卡的標題）</h2>
     * 原文那一行是兩個顏色，名稱橘、類型灰，而灰的那一段<b>含方括號</b>：
     *
     * <pre>
     *   #FF8C19 「Dragonkin Nest 」
     *   #AAAAAA 「[Cave]」
     * </pre>
     *
     * 語料寫的是純文字（{@code "Dragonkin Nest [Cave]": "龍裔巢穴 [洞窟]"}），
     * 沒有色碼，所以灰色是靠字面比對貼回去的。{@code [Cave]} 查表查不到，
     * {@link #lookupWordCore} 剝掉標點之後查到的是 {@code Cave} → 「洞窟」，
     * 於是只有中間兩個字變灰，兩個方括號留在名稱的橘色裡——畫出去是
     * {@code ["龍裔巢穴 ", "[", "洞窟", "]"]} 四段。使用者回報的就是這個。
     *
     * <p>名稱的譯文<b>自己就含有</b>類型詞時更明顯：字面比對挑的是第一個，
     * 「末日洞窟 [洞窟]」的灰色貼到了<b>名稱裡</b>的「洞窟」，後面真正該灰的
     * 那一個反而留著名稱的橘色。擴到括號之後字面唯一，這種挑錯位置也跟著沒了。
     *
     * <h2>為什麼這條規則站得住腳</h2>
     * 括號是<b>原文那一段自己的一部分</b>（原文的分段就是這樣切的），而譯文
     * 照原樣寫了一對括號把同一個詞包起來——兩邊指的是同一塊東西，顏色自然一致。
     * 語料裡 630 條這種標題（{@code Cave} 199、{@code Quest} 110、
     * {@code Mini-Quest} 85、{@code Secret Discovery} 23、{@code Dungeon} 19、
     * {@code Boss Altar} 14、{@code World Discovery} 12 …）的譯文全是
     * {@code 譯名 [類型譯名]}，六個語言都一樣。
     *
     * <h2>何時不擴</h2>
     * <ul>
     *   <li>原文那一段<b>不是</b>整塊括起來的——括號左右還有別的字時，擴出去會
     *       吃掉不屬於這個顏色的字。只認開頭是 {@code [}、結尾是 {@code ]}
     *       而且中間沒有另一層括號的。</li>
     *   <li>譯文本來就<b>自己帶括號</b>（查到的譯文裡已經有 {@code []}）——
     *       再包一層會變成 {@code [[洞窟]]}，那在譯文裡根本找不到。</li>
     *   <li>譯文裡<b>沒有</b>照原文寫這對括號（改寫成別的說法、或換成圓括號）
     *       ——找不到就照舊，只貼內層那個詞，不會比現在更糟。</li>
     * </ul>
     * 只處理方括號：圓括號與大括號在這份語料裡不是這個用法，而 {@code {} }
     * 還是佔位符的符號。
     *
     * @return 擴出去之後的字面；不適用時回傳 {@code null}
     */
    static String wrapLikeSource(String core, String zh, String translated) {
        if (core == null || zh == null || translated == null
                || core.length() < 3 || zh.isBlank()) {
            return null;
        }
        if (core.charAt(0) != '[' || core.charAt(core.length() - 1) != ']') {
            return null;                       // 不是整塊被括起來的
        }
        String inner = core.substring(1, core.length() - 1);
        if (inner.isBlank() || inner.indexOf('[') >= 0 || inner.indexOf(']') >= 0) {
            return null;                       // 巢狀或空的括號，看不出該擴到哪一層
        }
        if (zh.indexOf('[') >= 0 || zh.indexOf(']') >= 0) {
            return null;                       // 譯文本來就自己帶括號
        }
        String wrapped = "[" + zh + "]";
        if (translated.contains(wrapped)) {
            return wrapped;
        }
        return countedWrap(zh, translated);
    }

    /**
     * 括號裡除了那個詞還有一個<b>數量</b>時的版本。
     *
     * <h2>實機回報（迷你任務卡的敘述）</h2>
     * 原文那一段是「青色的整塊方括號」，而括號裡是數量加物品名：
     *
     * <pre>
     *   §7Bring §3[24 Fluffy Fur]§7 to the Slaying Post §3[Combat Lv. 88]§7 at
     * </pre>
     *
     * 譯文是「把 {@code [{~} 蓬鬆毛皮]} 交到討伐告示 {@code [戰鬥等級 {~}]}」。
     * 上面那一路找的是 {@code [蓬鬆毛皮]}，而譯文裡是 {@code [{~} 蓬鬆毛皮]}
     * ——中間隔著數量，找不到。結果只有 {@code [24} 是青的，
     * {@code 蓬鬆毛皮]} 掉回底色，一個方括號半青半灰。
     *
     * <h2>條件一樣要窄</h2>
     * 只認「括號裡除了那個詞，剩下的全是數量」——數字、{@code {~}}、空白。
     * 剩下的只要有一個實字（{@code [Combat Lv. 88]} 那種<b>整塊</b>另外查得到
     * 譯文的，走的是上面那一路）就不擴，免得把兩個詞的括號整塊吃掉。
     *
     * <p>找<b>第一個</b>吻合的括號就停。同一行出現兩個「數量 + 同一個詞」的
     * 括號在這份語料裡不存在；真出現了也只是少上一個色，不會上錯。
     *
     * @return 連括號與數量一起的那一整塊；找不到時回傳 {@code null}
     */
    private static String countedWrap(String zh, String translated) {
        int at = 0;
        while ((at = translated.indexOf('[', at)) >= 0) {
            int close = translated.indexOf(']', at + 1);
            if (close < 0) {
                return null;
            }
            String inner = translated.substring(at + 1, close);
            if (inner.endsWith(zh)
                    && onlyCount(inner.substring(0, inner.length() - zh.length()))) {
                return translated.substring(at, close + 1);
            }
            at = close + 1;
        }
        return null;
    }

    /** 只剩數量：數字、{@code {~}}、空白。空的不算——那是上面 exact 那一路的事。 */
    private static boolean onlyCount(String lead) {
        if (lead.isBlank()) {
            return false;
        }
        String bare = lead.replace(com.wynnchayuan.capture.GlyphSplitter.NUMBER_PLACEHOLDER, "")
                          .replace(" ", "");
        for (int i = 0; i < bare.length(); i++) {
            if (!Character.isDigit(bare.charAt(i))) {
                return false;
            }
        }
        return true;
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
        if (zh == null || zh.isBlank()) {
            zh = store.lookupTerm(core);
        }
        return zh == null || zh.isBlank() ? withoutCount(core, store) : zh;
    }

    /**
     * 開頭那個<b>數量</b>剝掉再查一次。
     *
     * <p>迷你任務的色段是整塊的 {@code [24 Fluffy Fur]}，剝掉前後標點之後仍然是
     * 「24 Fluffy Fur」——語料裡的鍵是物品名本身，數量是<b>這一次</b>的數字，
     * 不可能進語料。不剝的話這一段完全查不到，譯文裡的物品名一個色都沒有。
     *
     * <p>只剝<b>開頭</b>、而且後面必須還有實字。剝完剩數字的（{@code [139]}）
     * 不查——那是數值，本來就由佔位符自己帶樣式回來。
     */
    private static String withoutCount(String core, TranslationStore store) {
        int at = 0;
        while (at < core.length() && Character.isDigit(core.charAt(at))) {
            at++;
        }
        if (at == 0 || at >= core.length() || core.charAt(at) != ' ') {
            return null;                       // 開頭沒有數量，或整塊都是數字
        }
        String rest = core.substring(at + 1).strip();
        if (rest.isEmpty() || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(rest)) {
            return null;
        }
        String zh = store.lookup(rest);
        if (zh == null || zh.isBlank()) {
            zh = store.lookupTerm(rest);
        }
        return zh == null || zh.isBlank() ? asSingular(rest, store) : zh;
    }

    /**
     * 複數變回單數再查一次。
     *
     * <h2>為什麼需要</h2>
     * 卡片上寫的是<b>這一次要幾個</b>，所以物品名是複數：{@code [15 Arcane
     * Anomalies]}、{@code [20 Void Essences]}、{@code - +5 Saltpetres}。
     * 而語料收的是物品本身，鍵永遠是單數（{@code Arcane Anomaly}）。
     *
     * <p>查不到的後果不是「沒翻到」而已——方括號那一段查不到譯文就拿不到
     * 重點色，整塊掉回底色，畫面上變成「{@code [} 青、名字灰」的半青半灰。
     * 拿實機那幾張卡對過，七個查不到的複數裡這一步救回六個，剩下那個
     * （{@code Light Wood}）本來就是單數、語料真的沒有。
     *
     * <h2>只做這三條</h2>
     * {@code -ies → -y}、{@code -es → }、{@code -s → }。英文的不規則複數不管：
     * 猜錯了頂多查不到，跟現在一樣；猜對了才有收穫。{@code -ss} 結尾的不剝
     * （{@code Glass}、{@code Moss}）。
     */
    private static String asSingular(String plural, TranslationStore store) {
        List<String> tries = new ArrayList<>(2);
        if (plural.endsWith("ies") && plural.length() > 3) {
            tries.add(plural.substring(0, plural.length() - 3) + "y");
        } else if (plural.endsWith("es") && plural.length() > 2) {
            // 「-es」可能是 -e 加 s（Scales），也可能是整個 -es（Anomalies 已在上面）。
            // 兩種都試，先試短的那一種。
            tries.add(plural.substring(0, plural.length() - 2));
            tries.add(plural.substring(0, plural.length() - 1));
        } else if (plural.endsWith("s") && !plural.endsWith("ss")
                && plural.length() > 1) {
            tries.add(plural.substring(0, plural.length() - 1));
        }
        for (String one : tries) {
            String zh = store.lookup(one);
            if (zh == null || zh.isBlank()) {
                zh = store.lookupTerm(one);
            }
            if (zh != null && !zh.isBlank()) {
                return zh;
            }
        }
        return null;
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

    /**
     * 填回數值用的樣式：原本是 {@code offset/…} 這類<b>顯示字型</b>時保留原字型。
     *
     * <p>武器的「370 DPS」，數字用的是 {@code offset/wynncraft_quad/12}——放大的字。
     * 一律換成預設字型的話數字就縮回一般大小（實機回報「上方 dps 文字很大」那行
     * 翻完變小）。那種字型本來就只拿來畫數字，照原樣畫就是原文的樣子。
     */
    private static Style numberDisplay(Style style) {
        Style big = displayFont(style);
        return big != null ? big : forDisplay(style);
    }

    /** 放大數字用的字型（{@code offset/…}）；不是那種字型時回傳 {@code null}。 */
    private static Style displayFont(Style style) {
        return style != null && style.getFont() instanceof FontDescription.Resource r
                && r.id() != null && r.id().getPath().startsWith("offset/") ? style : null;
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
        boolean word = at + 3 <= template.length()
                && template.charAt(at) == '{' && template.charAt(at + 1) == 'w';
        if (!word && (at + 3 > template.length()
                || template.charAt(at) != '{' || template.charAt(at + 1) != 'c')) {
            return null;
        }
        int end = template.indexOf('}', at + 2);
        if (end < 0) {
            return null;
        }
        String body = template.substring(at + 2, end);
        String whole = template.substring(at, end + 1);
        if (body.length() == 1 && body.charAt(0) >= '1' && body.charAt(0) <= '9') {
            int slot = body.charAt(0) - '0';
            return new Token(Kind.COLOR, whole, word ? slot + WORD_SLOT : slot);
        }
        if (word) {
            return null;                       // {w:…} 沒有這種寫法
        }
        if (body.length() > 1 && body.charAt(0) == ':') {
            return new Token(Kind.COLOR, whole, -1);
        }
        return null;
    }

    /**
     * 原文裡一整條同一個符號重複的進度條，連同它<b>每一格的顏色</b>。
     *
     * @param sign   組成這條的那個符號
     * @param styles 每一格各自的樣式，長度就是這條有幾格
     */
    record Bar(char sign, List<Style> styles) { }

    /** 幾格以上才算一條進度條。兩格的重複符號多半只是標點。 */
    private static final int MIN_BAR = 3;

    /**
     * 把原文裡的進度條連同顏色收起來。
     *
     * <h2>為什麼需要這個</h2>
     * 意象的層級那一行，原文長這樣：
     *
     * <pre>
     *   Tier I  &gt;&gt;&gt;&gt;&gt;&gt;  &gt;&gt;&gt;&gt;  Tier II  [9/14]
     *   灰      綠          暗灰      洋紅     白
     * </pre>
     *
     * 綠的有幾格<b>就是進度本身</b>——9/14 就是九綠五灰。那條箭頭不是裝飾，
     * 是玩家真正在看的東西。
     *
     * <p>而譯文裡它是一整段文字（{@code 「 >>>>>>>>>> 」}），走的是猜顏色那條路，
     * 猜出來整條同一個色，進度就沒了。{@code {cN}} / {@code {wN}} 也救不了：
     * 那是一段一個顏色，而這裡要的是一格一個顏色。
     *
     * <p>做法跟 {@code {#}} 一樣——原樣搬回來。只要譯文裡那條跟原文<b>同一個符號、
     * 同樣長</b>，就把原文每一格的顏色照抄；長度對不上就不猜，照舊。
     */
    static List<Bar> bars(List<LineParts.Piece> runs) {
        List<Bar> out = new ArrayList<>();
        char sign = 0;
        List<Style> styles = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            Style style = run.style() == null ? Style.EMPTY : run.style();
            for (int i = 0; i < run.text().length(); i++) {
                char c = run.text().charAt(i);
                if (c == sign) {
                    styles.add(style);
                    continue;
                }
                if (styles.size() >= MIN_BAR) {
                    out.add(new Bar(sign, List.copyOf(styles)));
                }
                styles.clear();
                sign = isBarSign(c) ? c : 0;
                if (sign != 0) {
                    styles.add(style);
                }
            }
        }
        if (styles.size() >= MIN_BAR) {
            out.add(new Bar(sign, List.copyOf(styles)));
        }
        return out;
    }

    /**
     * 這個字元能不能組成進度條。
     *
     * <p>字母與數字不算——那是字不是條。空白也不算，不然一整片縮排會被當成一條。
     * 代理對（{@code {#}} 那些私用區符號）同樣排除：它們走 GLYPH 那條路填回去，
     * 本來就帶著自己的樣式。
     */
    private static boolean isBarSign(char c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)
                && !Character.isSurrogate(c);
    }

    /** 這段文字裡第一條進度條的起訖（{@code {起, 訖}}），沒有就回 {@code null}。 */
    static int[] barSpan(String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (!isBarSign(c)) {
                i++;
                continue;
            }
            int j = i;
            while (j < text.length() && text.charAt(j) == c) {
                j++;
            }
            if (j - i >= MIN_BAR) {
                return new int[] {i, j};
            }
            i = j;
        }
        return null;
    }

    /**
     * 領一條還沒用過、對得上的原文進度條。
     *
     * <p>同一行有兩條的時候照出現順序配，用過的不再配第二次——不然第二條會
     * 拿到第一條的顏色。
     */
    private static Bar takeBar(List<Bar> bars, boolean[] used, char sign, int length) {
        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            if (!used[i] && bar.sign() == sign && bar.styles().size() == length) {
                used[i] = true;
                return bar;
            }
        }
        return null;
    }

    /** 把一條進度條畫回去，顏色相同的相鄰格併成一段。 */
    private static void appendBar(MutableComponent line, Bar bar) {
        int at = 0;
        while (at < bar.styles().size()) {
            Style style = bar.styles().get(at);
            int end = at;
            while (end < bar.styles().size() && bar.styles().get(end).equals(style)) {
                end++;
            }
            line.append(literal(String.valueOf(bar.sign()).repeat(end - at),
                                forDisplay(style)));
            at = end;
        }
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
     * 只看<b>有字母</b>的片段的顏色，同樣依第一次出現的順序編號。{@code {w1}} 是第一個。
     *
     * <h2>為什麼要有第二套編號</h2>
     * {@link #palette} 是照<b>所有</b>有內容的片段編的，而有些行裡夾著純符號的
     * 片段——意象的層級進度就是：
     *
     * <pre>
     *   Tier I  &gt;&gt;&gt;&gt;&gt;  &gt;&gt;&gt;&gt;&gt;  Tier II  [2/4]
     *   灰      綠        暗灰     洋紅     白
     * </pre>
     *
     * 綠箭頭的數量<b>隨進度變動</b>：進度 0 的時候只有一段暗灰，滿的時候只有
     * 一段綠。於是「下一層」那一段在 palette 裡一下是第 4 個、一下是第 3 個——
     * 譯文裡寫死 {@code {c4}} 或 {@code {c3}} 都會有一半的時候落空，落空就當作
     * 沒寫，畫面上整行變成灰的。使用者回報了兩次。
     *
     * <p>{@code {wN}} 只數有字母的片段：箭頭與 {@code [2/4]} 都不算，所以
     * {@code {w1}} 永遠是目前層級、{@code {w2}} 永遠是下一層級，跟進度無關。
     *
     * <p>刻意<b>另開</b>一套而不是改 {@link #palette}：語料裡已經有八百多條在用
     * {@code {cN}}，改編號規則會把它們全部挪位。
     */
    static List<Style> wordPalette(List<LineParts.Piece> runs) {
        List<Style> out = new ArrayList<>();
        for (LineParts.Piece run : runs) {
            if (!GlyphSplitter.hasLetter(run.text())) {
                continue;
            }
            Style style = run.style() == null ? Style.EMPTY : run.style();
            if (!out.contains(style)) {
                out.add(style);
            }
        }
        return out;
    }

    /** 見 {@link #wordPalette}：{@code {wN}} 的編號從這裡起跳，跟 {@code {cN}} 分開。 */
    private static final int WORD_SLOT = 100;

    /**
     * 一個顏色佔位符要套的樣式；套不出來就回傳 {@code null}（照舊走猜的那條路）。
     *
     * @param base 這一段原本的樣式，{@code {c:…}} 只換顏色、其餘沿用
     */
    private static Style colourOf(Token token, List<Style> palette,
                                  List<Style> words, Style base) {
        if (token.index() == 0) {
            return null;                       // {/}：回到底色
        }
        if (token.index() > WORD_SLOT) {
            int slot = token.index() - WORD_SLOT;
            return slot <= words.size() ? forDisplay(words.get(slot - 1)) : null;
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
