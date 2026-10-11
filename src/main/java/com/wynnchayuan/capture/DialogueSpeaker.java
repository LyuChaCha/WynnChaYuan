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
        return text == null ? null : of(text.getComponent());
    }

    /**
     * 最近一次 action bar 原始訊息上的名牌。
     *
     * <h2>為什麼要有（issue #1138）</h2>
     * {@code NpcDialogueEvent#getDialogueText()} 是 Wynntils 從 action bar 拆出來的
     * <b>內文那一段</b>，名牌不在裡面——實機收集到的對話一句說話者都沒有，
     * 畫面上卻明明寫著「Espren Mother」。名牌只在原始訊息裡
     * （{@code DialogueProbe} 錄到的 {@code hud/dialogue/text/nameplate} 那幾片），
     * 所以在原始訊息進來時先讀下來，對話事件再來拿。
     */
    public static void noteRaw(net.minecraft.network.chat.Component message) {
        if (message == null) {
            current = null;
            currentText = "";
            return;
        }
        current = of(message);
        currentText = letters(message.getString());
    }

    /**
     * 畫面上這個對話框的名牌——但只在它<b>就是這一句</b>的對話框時。
     *
     * <h2>為什麼要比對內文</h2>
     * 換句的那一刻，原始訊息已經是下一句（名牌換成下一個人），Wynntils 卻還會用
     * <b>上一句</b>的文字補發幾個事件。直接拿名牌的話，上一句就被記成下一個人說的——
     * 實機錄到母子對話整段錯開一句。所以名牌只給內文對得上的那一句。
     *
     * @param dialogueText 對話事件帶的文字
     * @return 名牌；對不上或沒有名牌就回傳 {@code null}
     */
    public static String currentFor(String dialogueText) {
        String name = current;
        if (name == null || dialogueText == null) {
            return null;
        }
        String said = letters(dialogueText);
        return !said.isEmpty() && currentText.contains(said) ? name : null;
    }

    /**
     * 名牌原文只留<b>名字</b>：第一列，剝掉圖示與排版偏移。
     *
     * <p>NPC 頭頂的名牌底下還有一列等級牌（全是自訂字型的圖示），
     * 語料裡有些 speaker 也還帶著那一列。
     *
     * @return 乾淨的名字；什麼都不剩就回傳 {@code null}
     */
    public static String cleanName(String raw) {
        if (raw == null) {
            return null;
        }
        String first = raw.strip().split("\n", 2)[0];
        return named(GlyphSplitter.stripGlyphChars(first).strip());
    }

    /** 只留字母與數字：原始訊息裡夾滿排版偏移與圖示，比對時不能算進去。 */
    private static String letters(String s) {
        StringBuilder out = new StringBuilder(s.length());
        s.codePoints().filter(Character::isLetterOrDigit)
                .filter(cp -> cp < 0xE000 || (cp > 0xF8FF && cp < 0xF0000))
                .forEach(out::appendCodePoint);
        return out.toString();
    }

    private static volatile String current;
    private static volatile String currentText = "";

    private static String of(net.minecraft.network.chat.Component component) {
        StringBuilder name = new StringBuilder();
        component.visit((style, part) -> {
            if (isNameplate(style)) {
                name.append(part);
            }
            return Optional.empty();
        }, Style.EMPTY);
        // 名牌兩側常有排版偏移與框線圖示，跟本文一樣要清掉才拿得到名字
        return named(GlyphSplitter.stripGlyphChars(name.toString()).strip());
    }

    /**
     * 不像名字的就不算名字：至少要有兩個 A–Z 字母。
     *
     * <h2>為什麼（實機，Tunnel Trouble）</h2>
     * 有些對話框的名牌位置只放一個 {@code À}，那是 Wynncraft 拿來佔位的字
     * （漂浮字的 Seaskipper 也是 {@code §0À}）。它在名牌字型裡是普通的字
     * （{@code wynncraft_latin.png}），從字型看不出是佔位，所以不能只擋 À 一個——
     * 下次換成 Á、Ï 一樣會被記成說話者，還會因為不在任務的說話者名單上，
     * 把整句的任務名一起拿掉。NPC 的名字都是英文，兩個字母擋不到真名
     * （Bob、Espren Guard、Álvaro），擋得掉只有佔位字、符號、數字的名牌——只有問號的「???」除外。
     */
    /** 只有問號（可能帶附加符號、空白）的名牌。 */
    private static final java.util.regex.Pattern MYSTERY =
            java.util.regex.Pattern.compile("[?\\p{M}\\s]*\\?[?\\p{M}\\s]*");

    private static String named(String text) {
        if (text == null) {
            return null;
        }
        long letters = text.chars().filter(c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')).count();
        if (letters >= 2) {
            return text;
        }
        // 「???」是遊戲刻意藏名字的角色，語料裡有好幾個，譯者要知道是同一個謎樣人物
        return MYSTERY.matcher(text).matches() ? text : null;
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
