package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * 這句話是誰說的——直接問對話框本身。
 *
 * <h2>先前的做法與它的問題</h2>
 * 收集端原本用「玩家<b>正在看</b>的那個實體的名牌」當說話者
 * （{@code LookAtTranslator.nearestLabel()}）。那是個間接的猜測，而且通常猜不到：
 * 玩家讀對話框的時候眼睛盯著框，不是盯著 NPC 的頭頂。實測收集到的 13 句對話，
 * {@code ctx} 全都只有 {@code dialogue/King's Recruit}，<b>一句都沒有說話者</b>。
 *
 * <p>沒有說話者，補進來的句子就只能靠人去猜是誰講的、插在哪一段——
 * 而語料裡每一條都有 {@code speaker} 欄位，本來可以直接對上的。
 *
 * <h2>做法</h2>
 * 說話者其實就畫在對話框裡，帶著自己的字型：
 *
 * <pre>
 *   font=hud/dialogue/text/nameplate       text=Tasim
 *   font=hud/dialogue/text/wynncraft/body_0 text=That guard...
 * </pre>
 *
 * 所以認字型就好，不必猜。這份資料是<b>權威的</b>：它就是遊戲畫出來給玩家看的名字。
 *
 * <h2>限制</h2>
 * 有些對話沒有名牌（旁白框、系統敘述），那時回傳 {@code null}，
 * 呼叫端照舊只記任務名稱——沒有比以前差。
 */
public final class DialogueSpeaker {

    /** 名牌的字型。見 {@code DialogueProbe} 裡的字型清單。 */
    private static final String NAMEPLATE_FONT = "hud/dialogue/text/nameplate";

    private DialogueSpeaker() {}

    /**
     * @return 對話框名牌上的名字；沒有名牌就回傳 {@code null}
     */
    public static String of(StyledText text) {
        if (text == null) {
            return null;
        }
        StringBuilder name = new StringBuilder();
        text.getComponent().visit((style, part) -> {
            if (isNameplate(style)) {
                name.append(part);
            }
            return Optional.empty();
        }, Style.EMPTY);
        // 名牌兩側常有排版偏移與框線圖示，跟本文一樣要清掉才拿得到名字
        String out = GlyphSplitter.stripGlyphChars(name.toString()).strip();
        return out.isEmpty() ? null : out;
    }

    /**
     * 字型的判斷刻意跟 {@code DialogueProbe.fontOf} 用同一招：把字型描述轉成字串再找路徑。
     * 1.21.11 的 {@code getFont()} 回傳的是 {@code FontDescription} 而不是 Identifier，
     * 型別在版本之間動過，字串比對是這裡最不會被版本改動打斷的寫法。
     */
    private static boolean isNameplate(Style style) {
        return style != null && style.getFont() != null
                && style.getFont().toString().contains(NAMEPLATE_FONT);
    }
}
