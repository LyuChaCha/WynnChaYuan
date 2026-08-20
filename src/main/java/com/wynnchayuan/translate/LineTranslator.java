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
     * @return 譯好的一行；查不到翻譯或佔位符對不上時回傳 {@code null}
     */
    public static Component translate(StyledText line, TranslationStore store) {
        Component whole = translateWholeLine(line, store);
        return whole != null ? whole : translateSegments(line, store);
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
    private static Component translateSegments(StyledText line, TranslationStore store) {
        // 先把每個片段翻好，不急著組裝——寬度要等整行都翻完才量得準。
        List<Piece> pieces = new ArrayList<>();
        boolean any = false;

        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();

            // 空白字型底下也可能掛著不是寬度偏移的字元（材質包自訂圖示）。
            // 那種重新編碼會變成別的東西，所以只有編解碼繞得回來的才當空白。
            if (SpaceOffset.isSpaceFont(ps) && isAdjustableSpace(style, raw)) {
                pieces.add(Piece.space(SpaceOffset.decode(raw), style));
                continue;
            }
            // 符號、純空白、查不到的文字 —— 一律原封不動，含字型
            if (GlyphSplitter.isGlyphPart(part) || raw.isBlank()) {
                pieces.add(Piece.text(raw, style));
                continue;
            }
            String replaced = translateOneSegment(raw, style, store);
            if (replaced == null) {
                pieces.add(Piece.text(raw, style));
                continue;
            }
            any = true;
            Style display = forDisplay(style);
            pieces.add(Piece.translated(replaced, raw, display));
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

        List<Piece> aligned = alignColumns(pieces);

        // 只有前導空白的行是「置中／縮排」而不是「標籤 + 數值」。
        // 這種行沒有兩側都有文字的空白，上面那一輪不會動到它，
        // 所以另外量整行、補一半 —— 補滿會把整行推到右邊去。
        int lead = leadingSpace(aligned);
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
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (!p.isSpace()) {
                drift += widthOf(literal(p.text(), p.style()))
                        - widthOf(literal(p.origText(), p.style()));
                out.add(p);
                continue;
            }
            if (isAlignSpace(pieces, i)) {
                out.add(Piece.space(p.spacePx() - drift, p.style()));
                drift = 0;
            } else {
                out.add(p);
            }
        }
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
        return valueStart;
    }

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
    private static boolean hasColumnGap(List<Piece> pieces, int boundary) {
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
        return spaces >= MIN_COLUMN_GAP;
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
                out.append(literal(p.text(), p.style()));
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
    record Piece(String text, String origText, Style style, int spacePx, boolean isSpace) {
        /** 沒有被翻譯的片段：原文就是它自己。 */
        static Piece text(String t, Style s) {
            return new Piece(t, t, s, 0, false);
        }

        /** 翻譯過的片段。原文要留著，才算得出這一段寬度變了多少。 */
        static Piece translated(String t, String orig, Style s) {
            return new Piece(t, orig, s, 0, false);
        }

        static Piece space(int px, Style s) {
            return new Piece("", "", s, px, true);
        }
    }

    /**
     * 翻一個文字片段，並保留它原本的前後空白。
     *
     * <p>空白會影響版面（Wynncraft 用它對齊），所以只拿中間的實際內容去查，
     * 查到之後把空白原樣接回。
     */
    private static String translateOneSegment(String raw, Style style, TranslationStore store) {
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
        String hit = lookup(parts.template(), store);
        if (hit == null) {
            return null;
        }
        Component rebuilt = rebuild(hit, parts);
        return rebuilt == null ? null : prefix + rebuilt.getString() + suffix;
    }

    /** 整行查表。散文與對話用這條路，因為它們需要跨片段重排語序。 */
    private static Component translateWholeLine(StyledText line, TranslationStore store) {
        LineParts parts = LineParts.of(line);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;                       // 純符號或純數值的行，沒東西可翻
        }
        String translated = lookup(parts.template(), store);
        if (translated == null || translated.isBlank()) {
            return null;
        }
        Component rebuilt = rebuild(translated, parts);
        if (rebuilt == null) {
            return null;
        }
        Component result = realign(line, rebuilt);
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
    private static Component realign(StyledText original, Component rebuilt) {
        List<Run> orig = runs(original.getComponent());
        List<Run> made = runs(rebuilt);
        int spaces = countSpaces(made);
        if (spaces == 0 || spaces != countSpaces(orig)) {
            return rebuilt;
        }
        List<Integer> origSeg = segmentWidths(orig);
        List<Integer> madeSeg = segmentWidths(made);

        int[] adjust = new int[spaces];
        boolean leading = origSeg.get(0) == 0 && madeSeg.get(0) == 0;

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
     * 字元（材質包自己定義的圖示就是這樣）。那種字元 {@code decode} 會得到 0，
     * 重新編碼就變成完全不同的東西——畫面上是圖示憑空消失或變成方框。
     *
     * <p>所以要求它<b>編解碼能原樣繞回來</b>。繞不回來就當成普通文字原封不動抄過去。
     */
    private static boolean isAdjustableSpace(Style style, String text) {
        return SpaceOffset.isSpaceFont(style)
                && text.equals(SpaceOffset.encode(SpaceOffset.decode(text)));
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
        String exact = store.lookup(template);
        if (exact != null) {
            return exact;
        }
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;

        int start = 0;
        while (true) {
            if (template.startsWith(glyph, start)) {
                start += glyph.length();
            } else if (start < template.length() && Character.isWhitespace(template.charAt(start))) {
                start++;
            } else {
                break;
            }
        }
        int end = template.length();
        while (end > start) {
            if (end >= start + glyph.length() && template.startsWith(glyph, end - glyph.length())) {
                end -= glyph.length();
            } else if (Character.isWhitespace(template.charAt(end - 1))) {
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
        String hit = store.lookup(core);
        return hit == null ? null : template.substring(0, start) + hit + template.substring(end);
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

    private static Component rebuild(String translated, LineParts parts) {
        List<Token> tokens = tokenize(translated);
        long wantGlyphs = tokens.stream().filter(t -> t.kind() == Kind.GLYPH).count();
        long wantPlaces = tokens.stream().filter(t -> t.kind() == Kind.PLACE).count();
        long wantNumbers = tokens.stream().filter(t -> t.kind() == Kind.NUMBER).count();
        if (wantGlyphs != parts.glyphs().size()
                || wantPlaces != parts.places().size()
                || wantNumbers != parts.numbers().size()) {
            return null;              // 譯者刪了或多加了佔位符，放棄這行
        }

        MutableComponent out = Component.empty();
        Style textStyle = forDisplay(parts.textStyle());
        int glyph = 0;
        int place = 0;
        int number = 0;

        for (Token token : tokens) {
            switch (token.kind()) {
                case GLYPH -> {
                    // 符號連同原樣式（含自訂字型）整段搬回，這是它顯示得出來的唯一方式
                    LineParts.Piece p = parts.glyphs().get(glyph++);
                    out.append(Component.literal(p.text()).withStyle(p.style()));
                }
                case PLACE -> {
                    // 地名是專有名詞，原樣填回，永遠不翻譯
                    LineParts.Piece p = parts.places().get(place++);
                    out.append(literal(p.text(), forDisplay(p.style())));
                }
                case NUMBER -> {
                    LineParts.Piece p = parts.numbers().get(number++);
                    out.append(literal(p.text(), forDisplay(p.style())));
                }
                case TEXT -> out.append(literal(token.text(), textStyle));
            }
        }
        return out;
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

    private enum Kind { TEXT, GLYPH, PLACE, NUMBER }

    private record Token(Kind kind, String text) {}

    /** 把模板切成「文字 / 符號佔位 / 數值佔位」的序列。 */
    private static List<Token> tokenize(String template) {
        List<Token> out = new java.util.ArrayList<>();
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        String place = GlyphSplitter.PLACE_PLACEHOLDER;
        String number = GlyphSplitter.NUMBER_PLACEHOLDER;
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
