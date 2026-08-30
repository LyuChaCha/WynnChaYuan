package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 從<b>未經處理的</b> action bar 裡把玩家可選的回答抽出來。
 *
 * <h2>為什麼非得從這一手資料抽</h2>
 * Wynntils 的 {@code NpcDialogueEvent} 只帶 NPC 那一句。它的
 * {@code DialogueSegment} 只有三個方法——{@code getDialogueText()}、
 * {@code requiresShift()}、{@code hasChoices()}——<b>選項的文字不在裡面</b>，
 * 事件只告訴我們「有選項」這件事。所以走 Wynntils 那條路永遠拿不到。
 *
 * <h2>選項在哪裡（實機錄到的，不是推測）</h2>
 * 原始 action bar 是按<b>字型</b>分區的，每一個選項自己一個字型：
 * <pre>
 *   font=…/text/wynncraft/choice_1  text=Are you headed someplace?
 *   font=…/text/wynncraft/choice_3  text=Just saying hello.
 *   font=…/text/wynncraft/choice_2  text=What's life like here?
 * </pre>
 * 注意送過來的<b>順序不是編號的順序</b>（上面是 1、3、2），所以要照字型名字裡的
 * 編號排，不能照出現的先後。
 *
 * <h2>兩個先前踩過的坑</h2>
 * <ul>
 *   <li>認的是 {@code /choice_}（<b>底線</b>）。畫框線的字型叫
 *       {@code style/default/choice}，沒有底線——收進來會混一堆排版字元。</li>
 *   <li>選項<b>沒有號碼</b>。舊的 {@code looksLikeChoice} 靠「1.」「[1]」認選項，
 *       而真實資料裡三個選項一個號碼都沒有，所以那個判斷永遠不會成立。</li>
 * </ul>
 *
 * <p>另外，實測選項<b>從第一幀就是完整的</b>——NPC 那句還在逐字打的時候，
 * 三個選項已經全在了。所以這裡不必等、也不必處理打到一半的選項。
 */
public final class DialogueChoices {

    /** 選項文字的字型前綴。底線是關鍵，見類別說明。 */
    private static final String CHOICE = "/choice_";

    private DialogueChoices() {}

    /**
     * 把選項照編號順序抽出來。
     *
     * @param message 未經清理的 action bar 訊息
     * @return 選項文字，照字型名字裡的編號排；沒有選項時回傳空的清單
     */
    public static List<String> of(Component message) {
        if (message == null) {
            return List.of();
        }
        // TreeMap：照編號排，而不是照送過來的先後（實機錄到的順序是 1、3、2）。
        Map<Integer, StringBuilder> byIndex = new TreeMap<>();
        message.visit((style, text) -> {
            String font = fontOf(style);
            int at = font.indexOf(CHOICE);
            if (at < 0 || !readable(text)) {
                return Optional.empty();
            }
            Integer index = numberAfter(font, at + CHOICE.length());
            if (index == null) {
                return Optional.empty();
            }
            // 同一個選項可能被切成好幾段（句中換色時 Wynncraft 就會這樣送），
            // 所以是往後接，不是覆蓋。
            byIndex.computeIfAbsent(index, k -> new StringBuilder()).append(text);
            return Optional.empty();
        }, Style.EMPTY);

        List<String> out = new ArrayList<>(byIndex.size());
        for (StringBuilder each : byIndex.values()) {
            String text = each.toString().strip();
            if (!text.isEmpty()) {
                out.add(text);
            }
        }
        return List.copyOf(out);
    }

    /** 從 {@code at} 開始讀一串數字；讀不到回傳 {@code null}。 */
    private static Integer numberAfter(String font, int at) {
        int stop = at;
        while (stop < font.length() && Character.isDigit(font.charAt(stop))) {
            stop++;
        }
        if (stop == at) {
            return null;
        }
        try {
            return Integer.parseInt(font.substring(at, stop));
        } catch (NumberFormatException e) {
            return null;                        // 編號長到滿出 int，當作沒有
        }
    }

    /** 有沒有非排版、非圖示的字。與 {@code DialogueProbe} 同一套判斷。 */
    private static boolean readable(String text) {
        return text != null && text.codePoints().anyMatch(cp ->
                cp >= 0x20 && cp < 0x7F && !Character.isWhitespace(cp));
    }

    private static String fontOf(Style style) {
        return style.getFont() == null ? "" : style.getFont().toString();
    }
}
