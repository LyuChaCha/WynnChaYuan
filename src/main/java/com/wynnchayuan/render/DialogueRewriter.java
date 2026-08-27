package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;

import java.util.ArrayList;
import java.util.List;
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

    /** 對話文字段的字型前綴。{@code body_0}、{@code body_1}……一行一個。 */
    private static final String BODY = "hud/dialogue/text/";

    /** 說話者名字那一段。 */
    private static final String NAMEPLATE = "hud/dialogue/text/nameplate";

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

        // 先就地改，最後才組。名牌是置中的，換掉名字之後<b>前後兩個</b>偏移
        // 都要重算——邊走邊組的話，前面那個已經送出去了，改不到。
        boolean[] swapped = new boolean[texts.size()];
        boolean changed = false;
        for (int i = 1; i + 1 < texts.size(); i++) {
            String font = fontOf(styles.get(i));
            String text = texts.get(i);
            Integer lead = offsetOf(texts.get(i - 1));
            if (!font.contains(BODY) || !readable(text)
                    || lead == null || offsetOf(texts.get(i + 1)) == null) {
                continue;                      // 認不出這個形狀就別動它
            }
            boolean name = font.contains(NAMEPLATE);
            String hit = name ? speaker(text, store) : line(text, store);
            if (hit == null || hit.equals(text)) {
                continue;
            }
            int width = width(hit);
            if (name) {
                // 置中：前後都跟著寬度走
                texts.set(i - 1, offset(nameLead(width)));
                texts.set(i + 1, offset(NAME_CENTRE - width / 2));
            } else {
                // 靠左：前導不動，尾隨補到「走回原位」
                texts.set(i + 1, offset(-lead - width));
            }
            texts.set(i, hit);
            swapped[i] = true;
            changed = true;
        }
        if (!changed) {
            return null;
        }

        MutableComponent out = Component.empty();
        for (int i = 0; i < texts.size(); i++) {
            // 換過的那一段用<b>預設字型</b>畫，中文才出得來；沒換的原樣抄，
            // 包含它原本的字型——框、頭像、按鈕都是靠那些字型畫出來的。
            Style style = swapped[i]
                    ? styles.get(i).withFont(FontDescription.DEFAULT)
                    : styles.get(i);
            out.append(Component.literal(texts.get(i)).withStyle(style));
        }
        return out;
    }

    /** 內文：整句查表，查不到就用開頭比對（NPC 是一個字一個字打出來的）。 */
    private static String line(String text, TranslationStore store) {
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
        return width(hit) <= BODY_LEFT * 2 ? hit : null;
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

    private static int width(String text) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? text.length() * 6 : mc.font.width(text);
    }

    private static boolean readable(String text) {
        return text.codePoints().anyMatch(cp ->
                cp >= 0x20 && cp < 0x2E80 && !Character.isWhitespace(cp));
    }

    private static String fontOf(Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }
}
