package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 所有小框共用的外觀。
 *
 * <p>集中在一處，改主題色時三個小框（對話、追蹤、名牌）會一起跟著變，
 * 不必記得三個地方都要改。
 */
public final class Boxes {

    private Boxes() {}

    public static void draw(GuiGraphics g, int x, int y, int w, int h) {
        draw(g, x, y, w, h, 1.0f);
    }

    /**
     * 同上，但整個框（含邊線）乘上一個透明度，用來做淡出。
     *
     * <p>底色與邊線要一起淡，只淡其中一個的話收尾那幾幀會看到一個
     * 沒有內容的空框，比直接消失還怪。
     */
    public static void draw(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        int bg = Colors.fade(WynnChaYuan.config().backgroundARGB(), alpha);
        int border = Colors.fade(WynnChaYuan.config().accentARGB(), alpha);
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    /**
     * 把一段譯文整理成可以放進小框的幾行。
     *
     * <h2>為什麼要丟掉排版字元</h2>
     * 遊戲原本的文字裡夾著大量 {@code minecraft:space} 的對齊偏移，那是為了
     * <b>原本的版面</b>算好的——3D 名牌置中、tooltip 的欄位對齊。
     *
     * <p>但這裡是我們自己畫的框，版面完全不同。把偏移原樣搬過來會把文字推到框外，
     * 看起來就是「框有出現但裡面是空的」。
     *
     * <p>tooltip 面板則相反：它在鏡像原本的版面，那邊必須保留偏移。
     * 兩種情境不能共用同一套處理。
     *
     * <p>整行都是圖示、一個可讀字都沒有的行也會丟掉，理由同上：那是原本版面的
     * 裝飾（名牌底圖的 {@code banner/pill}、血條），我們自己畫了背景，
     * 再疊一張別人的底圖只會蓋掉譯文。
     *
     * <p>圖示<b>混在文字裡</b>的行則整行保留——那種圖示是內容的一部分
     * （物品符號、元素圖示），丟掉會讓句子少一塊。
     *
     * <p>換行也在這裡拆開——{@code drawString} 不會自己斷行，
     * 留著只會讓好幾行擠成一團。
     */
    public static List<Component> toLines(Component source) {
        if (source == null) {
            return List.of();
        }
        List<Style> styles = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        source.visit((style, text) -> {
            styles.add(style);
            texts.add(text);
            return Optional.empty();
        }, Style.EMPTY);

        List<Component> lines = new ArrayList<>();
        MutableComponent current = Component.empty();
        boolean hasContent = false;   // 這一行有沒有「可讀」的字，純圖示不算

        for (int i = 0; i < texts.size(); i++) {
            Style style = styles.get(i);
            String text = texts.get(i);
            if (text.isEmpty() || SpaceOffset.isSpaceFont(style)) {
                continue;                      // 排版偏移，這個框不需要
            }
            String[] chunks = text.split("\n", -1);
            for (int c = 0; c < chunks.length; c++) {
                if (c > 0) {
                    if (hasContent) {
                        lines.add(current);
                    }
                    current = Component.empty();
                    hasContent = false;
                }
                String chunk = dropOffsets(chunks[c]);
                if (!chunk.isEmpty()) {
                    current.append(Component.literal(chunk).withStyle(style));
                    if (hasReadable(chunk)) {
                        hasContent = true;
                    }
                }
            }
        }
        if (hasContent) {
            lines.add(current);
        }
        dump(source, texts, styles, lines);
        return lines;
    }

    /**
     * 拿掉只是拿來排版的位移字元。
     *
     * <p>對話框裡這些字是用 body 字型畫的（不是 space 字型），
     * {@link SpaceOffset#isSpaceFont} 篩不掉；到了小框這邊改用預設字型畫，
     * 它們就變成一個個豆腐方塊。
     *
     * <p><b>不能整批拿掉圖示</b>——滑鼠、屬性符號那些是真的要畫出來的。
     * 兩者的區別在 Unicode 屬性：圖示在私用區，位移字在未分配的第 13 平面。
     *
     * <h2>但也不能見到位移就拿</h2>
     * 等級徽章這種東西是<b>圖示與位移交錯</b>拼出來的。
     * 實機錄到的「馬」名牌（診斷檔「填回去的符號 7」）：
     *
     * <pre>U+E060(圖示) U+CFFFF(位移) U+E03B(圖示) U+CFFFF(位移) …  font=banner/pill</pre>
     *
     * <p>那些位移是<b>用來定位圖示的</b>，拿掉徽章就散了——
     * 使用者回報的「馬 ⬛⬛ LV 1」就是這樣來的，而且是 1.99.40
     * 為了修豆腐方塊改出來的。
     *
     * <p>所以規則是：<b>一段裡面有私用區圖示，就整段別動</b>。
     * 純位移（或位移加文字）那種才是排版用的，拿掉才對。
     */
    static String dropOffsets(String text) {
        if (text.codePoints().noneMatch(Boxes::isOffset)) {
            return text;                       // 絕大多數行沒有，不要白白重建字串
        }
        if (text.codePoints().anyMatch(GlyphSplitter::isPrivateUse)) {
            return text;                       // 見下面的說明：這些位移是用來定位圖示的
        }
        StringBuilder kept = new StringBuilder(text.length());
        text.codePoints().filter(cp -> !isOffset(cp)).forEach(kept::appendCodePoint);
        return kept.toString();
    }

    private static boolean isOffset(int cp) {
        return GlyphSplitter.isGlyphCodePoint(cp) && !GlyphSplitter.isPrivateUse(cp);
    }

    /** 有沒有非圖示、非空白的字。整行都是圖示的話這裡會是 false。 */
    private static boolean hasReadable(String text) {
        return text.codePoints().anyMatch(cp ->
                !Character.isWhitespace(cp) && !GlyphSplitter.isGlyphCodePoint(cp));
    }

    /**
     * 把整理過程寫出來，供對照。只寫前幾次就停。
     *
     * <p>小框空白時光看畫面完全查不出是哪一步——是沒有行、行是空的、
     * 還是畫出來看不見。這裡把三者分開記錄。
     */
    private static int dumped = 0;

    private static void dump(Component source, List<String> texts,
                             List<Style> styles, List<Component> lines) {
        // 只在收集模式下寫，一般玩家的 config 資料夾不需要多這幾個檔
        if (dumped >= 3 || debugFile == null || !WynnChaYuan.config().debugDumps()) {
            return;
        }
        dumped++;
        StringBuilder sb = new StringBuilder();
        sb.append("=== 原始 ===").append(System.lineSeparator());
        sb.append("getString: ").append(source.getString()).append(System.lineSeparator());

        sb.append(System.lineSeparator())
          .append("=== visit 拆出來的片段 ===").append(System.lineSeparator());
        for (int i = 0; i < texts.size(); i++) {
            Style st = styles.get(i);
            sb.append(String.format("  [%d] font=%s color=%s len=%d text=%s%n",
                    i,
                    st.getFont() == null ? "(null)" : st.getFont().toString(),
                    st.getColor() == null ? "(null)" : st.getColor().toString(),
                    texts.get(i).length(),
                    texts.get(i).replace("\n", "\\n")));
        }

        sb.append(System.lineSeparator())
          .append("=== 整理後的行 ===").append(System.lineSeparator());
        sb.append("行數: ").append(lines.size()).append(System.lineSeparator());
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            sb.append(String.format("  [%d] len=%d %s%n",
                    i, line.getString().length(), line.getString()));
        }
        try {
            java.nio.file.Files.writeString(
                    debugFile.resolveSibling("overlay-debug-" + dumped + ".txt"),
                    sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 診斷寫不出來就算了，不要影響遊戲
        }
    }

    private static java.nio.file.Path debugFile;

    public static void init(java.nio.file.Path path) {
        debugFile = path;
    }
}
