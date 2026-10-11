package com.wynnchayuan.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 聊天是<b>一列一則</b>送來的——收集時要把同一句話的幾列接回去看。
 *
 * <h2>實機回報（issue #1124）</h2>
 * 發現地區的說明是伺服器照聊天欄寬度<b>先折好行</b>、一列一則訊息送來的：
 *
 * <pre>
 *   Area Discovered: …… (+7000 XP)
 *   A shortage of water has caused this forest to exist in      ← 沒收
 *   an eternal autumn. The poorest region in Fruma, its         ← 收了
 *   inhabitants manage an economy of their own based on         ← 收了
 *   bartering, allowing them a better quality of life.          ← 收了
 * </pre>
 *
 * 整句早就在語料裡、畫面上也翻出來了（翻譯那一邊是等整塊跳完再接成一句查的，
 * 見 {@code ChatBlock}）。但收集是<b>每一列進來就各自記</b>：第一列剛好是整句的
 * 開頭，被「語料裡有更長的」那一關擋掉（{@link CaptureStore#knowsLonger}）；後面
 * 幾列不是任何條目的開頭，於是被當成缺口匯出——三條永遠用不到的半句。
 *
 * <h2>做法</h2>
 * 一列進來，如果它是語料某一條的<b>開頭</b>，先扣著不記。下一列進來時接上去看：
 * <ul>
 *   <li>接起來還是某一條的開頭 → 繼續扣；</li>
 *   <li>接起來<b>就是</b>語料的一條 → 這幾列是那一句折出來的，全部不記；</li>
 *   <li>接不上 → 扣著的那幾列不是折行，各自照常記。</li>
 * </ul>
 * 接的方式兩種都試：折行的句子用空白接（{@code translate/TranslationStore#unwrap}
 * 同一套），整塊的條目（「[Quest Completed]⏎任務名⏎Rewards:」）用換行接。
 *
 * <h2>順便補上的洞</h2>
 * 扣著的列<b>沒有</b>被接下去時，它就是完整的一列，不是打到一半的半句——所以放行
 * 時不再過「語料裡有更長的」那一關。先前「{@code - +Access to the {p}}」這種獎勵列
 * 因為語料有「{@code - +Access to the {p} Dungeon}」而被當成半句丟掉：畫面上是
 * 英文，缺口清單裡卻沒有它。
 *
 * <p>這個類別只管判斷，不碰檔案也不碰 Minecraft，測試直接餵字串就好。
 */
public final class ChatRows {

    /** 扣著的列隔這麼久沒有下一列，就當它自己是一則。同一塊訊息是同一個 tick 送完的。 */
    static final long WINDOW_MS = 1000;

    /** 一句話最多折成幾列。再多就不是「一句話」，別把整個聊天欄扣住。 */
    static final int MAX_ROWS = 24;

    /** 一列聊天：模板與它的出處（{@code chat/INFO}）。 */
    public record Row(String template, String ctx) {}

    private final Predicate<String> whole;
    private final Predicate<String> longer;

    private final List<Row> held = new ArrayList<>();
    private String run;
    private long last;

    /**
     * @param whole  語料裡有這一條嗎（翻了沒都算）
     * @param longer 語料裡還有更長的原文以這一段開頭嗎
     */
    public ChatRows(Predicate<String> whole, Predicate<String> longer) {
        this.whole = whole;
        this.longer = longer;
    }

    /**
     * 收一列。
     *
     * @return 現在可以記的列（照原本的先後）。被扣著的、以及確定是折行的不在裡面
     */
    public synchronized List<Row> offer(String template, String ctx, long now) {
        List<Row> out = new ArrayList<>();
        if (template == null || template.isBlank()) {
            return out;
        }
        String row = template.strip();
        if (run != null && (now - last > WINDOW_MS || held.size() >= MAX_ROWS)) {
            release(out);
        }
        if (run != null) {
            for (String joined : List.of(run + " " + continuation(row), run + "\n" + row)) {
                boolean more = longer.test(joined);
                if (!more && !whole.test(joined)) {
                    continue;
                }
                if (more) {
                    run = joined;
                    held.add(new Row(row, ctx));
                    last = now;
                } else {
                    clear();                   // 整句接完了，這幾列一列都不必記
                }
                return out;
            }
            release(out);                      // 接不上：扣著的不是折行
        }
        if (longer.test(row)) {
            run = row;
            held.add(new Row(row, ctx));
            last = now;
        } else {
            out.add(new Row(row, ctx));
        }
        return out;
    }

    /**
     * 扣著的列等夠久了就放行。由計時器呼叫——最後一列後面不一定還有下一則訊息。
     */
    public synchronized List<Row> settle(long now) {
        List<Row> out = new ArrayList<>();
        if (run != null && now - last > WINDOW_MS) {
            release(out);
        }
        return out;
    }

    /** 現在扣著幾列，測試用。 */
    synchronized int holding() {
        return held.size();
    }

    /**
     * 放行扣著的列。接到這裡剛好是語料的一條的話（後面沒有再接下去），
     * 那幾列一樣是它折出來的，不必記。
     */
    private void release(List<Row> out) {
        if (!whole.test(run)) {
            out.addAll(held);
        }
        clear();
    }

    private void clear() {
        held.clear();
        run = null;
    }

    /**
     * 續行開頭的頻道圖示與空白不算句子的一部分。
     *
     * <p>伺服器折行時每一列開頭都會再掛一次圖示，語料的整句裡沒有它——
     * 跟 {@code TranslationStore#unwrap} 拿掉的是同一個東西。
     */
    private static String continuation(String row) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = 0;
        while (at < row.length()) {
            if (row.startsWith(glyph, at)) {
                at += glyph.length();
            } else if (Character.isWhitespace(row.charAt(at))) {
                at++;
            } else {
                break;
            }
        }
        return row.substring(at);
    }
}
