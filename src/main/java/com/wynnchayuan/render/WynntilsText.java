package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

/**
 * 換掉 Wynntils <b>自己那幾塊疊層</b>裡的字。
 *
 * <h2>為什麼要動別人畫的東西</h2>
 * 右上角的任務追蹤與每日目標是 Wynntils 的疊層：位置、字型、進度條都是它排的，
 * 我們沒有地方插手。先前的做法是另外畫一個框把譯文擺在旁邊，但那樣同一件事
 * 在畫面上出現兩次，而使用者要的是「那一欄變成中文」。
 *
 * <p>所以多一個「就地取代」模式：在 Wynntils 把字送去畫之前換掉它
 *（見 {@code com.wynnchayuan.mixin}）。位置與樣式完全沿用它的，
 * 我們只換內容。
 *
 * <h2>這個類別為什麼存在</h2>
 * mixin 裡不方便寫邏輯（測不到、出錯也不好查），所以 mixin 只負責轉手，
 * 判斷與翻譯全部在這裡。設定與語料<b>用參數傳</b>，測試才進得來——
 * 靜態的那兩個在測試裡是 null。
 *
 * <p>每一個入口都<b>包在 try/catch 裡</b>：這是別人的算繪流程，
 * 我們丟出例外等於讓別人的疊層畫不出來。
 */
public final class WynntilsText {

    /** 追蹤欄那一塊的類別名。只動這一塊，別的疊層不碰。 */
    static final String TRACKER = "ContentTrackerOverlay";

    private WynntilsText() {}

    /** mixin 的入口；設定與語料從全域拿，順便記一筆診斷。 */
    public static StyledText[] lines(Object overlay, StyledText[] lines) {
        try {
            StyledText[] out = lines(overlay, lines,
                    WynnChaYuan.config(), WynnChaYuan.translations());
            if (out != lines) {
                WynnChaYuan.store().noteEvent("overlay.shown");
            }
            return out;
        } catch (Throwable t) {
            return lines;                      // 別人的算繪流程，不能讓它炸
        }
    }

    /**
     * Wynntils 的文字疊層要畫的那幾行。
     *
     * @param overlay 疊層本身，用來分辨是哪一塊
     * @param lines   它算好的每一行
     * @return 換好的那幾行；不該換或一行都翻不出來時<b>原樣</b>回傳
     */
    static StyledText[] lines(Object overlay, StyledText[] lines,
                              CollectorConfig config, TranslationStore store) {
        if (lines == null || lines.length == 0 || store == null || config == null
                || !tracker(overlay)
                || config.trackerMode() != CollectorConfig.DialogueMode.REPLACE) {
            return lines;
        }
        StyledText[] out = new StyledText[lines.length];
        boolean any = false;
        for (int i = 0; i < lines.length; i++) {
            out[i] = line(lines[i], store);
            any |= out[i] != lines[i];
        }
        return any ? out : lines;
    }

    /** 一行：翻不出來就原樣留著，別的行照樣可能翻得出來。 */
    private static StyledText line(StyledText line, TranslationStore store) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        var translated = LineTranslator.translate(line, store);
        if (translated != null) {
            return StyledText.fromComponent(translated);
        }
        return byPart(line, store);
    }

    /**
     * 整行查不到時，<b>一段一段</b>查。
     *
     * <h2>為什麼需要</h2>
     * 追蹤欄的第一行是 Wynntils 自己拼的：「Quest - Star Thief」——類型一段、
     * 破折號一段、任務名一段。整行當然不在語料裡（那是它拼的，不是遊戲送的），
     * 但<b>每一段都在</b>：「Quest」是介面字串、「Star Thief」是任務名。
     *
     * <p>一段一段換還有一個好處：每一段的顏色原樣留著。Wynntils 用顏色分
     * 「類型」與「名稱」，整行重組會把那個分別弄丟。
     *
     * @return 至少換掉一段時回傳新的；一段都沒換就原樣回去
     */
    private static StyledText byPart(StyledText line, TranslationStore store) {
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean any = false;
        int parts = 0;
        for (com.wynntils.core.text.StyledTextPart part : line) {
            StyledText one = StyledText.fromPart(part);
            parts++;
            if (one.isBlank()) {
                out.append(one.getComponent());
                continue;
            }
            net.minecraft.network.chat.Component done = LineTranslator.translate(one, store);
            if (done == null) {
                // 這一段自己就是「類型 - 名稱」：實機的追蹤欄整行只有一個顏色，
                // Wynntils 送過來就是一整段，上面那道「一段一段查」沒有東西可以拆。
                String split = splitHeader(one.getStringWithoutFormatting(), store);
                if (split != null) {
                    // 字型要換回預設：Wynntils 的標題樣板用 with_font 包成
                    // language/wynncraft，那份字型沒有中文字，照抄就是一排方框。
                    done = net.minecraft.network.chat.Component.literal(split)
                            .withStyle(styleOf(one).withFont(
                                    net.minecraft.network.chat.FontDescription.DEFAULT));
                }
            }
            any |= done != null;
            out.append(done != null ? done : one.getComponent());
        }
        return any ? StyledText.fromComponent(out) : line;
    }

    /**
     * 「Quest – Dearly Departed」這種一段裡的兩半各自查。
     *
     * <h2>實機回報</h2>
     * 0.2.0 的追蹤欄第二行（任務目標）換成中文了，第一行「Quest – Dearly Departed」
     * 還是英文。測試裡那一行是三段不同顏色，一段一段查就過了；實機那一行
     * <b>整行同一個顏色</b>，Wynntils 送來的是一整段，上面那道拆不開。
     *
     * @return 至少一半查得到時回傳換好的字；兩半都查不到回傳 {@code null}
     */
    static String splitHeader(String text, TranslationStore store) {
        for (String dash : DASHES) {
            int at = text.indexOf(dash);
            if (at <= 0) {
                continue;
            }
            String left = text.substring(0, at);
            String right = text.substring(at + dash.length());
            String l = half(left, store);
            String r = half(right, store);
            if (l == null && r == null) {
                return null;
            }
            return (l != null ? l : left) + dash + (r != null ? r : right);
        }
        return null;
    }

    /** 見 {@link #splitHeader}：Wynntils 拼標題用過的幾種破折號，兩邊都有空白。 */
    private static final String[] DASHES = {" - ", " – ", " — "};

    /** 一半：前面的圖示與空白原樣留著，只查中間那段字。 */
    private static String half(String text, TranslationStore store) {
        int start = 0;
        while (start < text.length()
                && (Character.isWhitespace(text.charAt(start))
                    || com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(
                            text.codePointAt(start)))) {
            start += Character.charCount(text.codePointAt(start));
        }
        String core = text.substring(start).strip();
        if (core.isEmpty()) {
            return null;
        }
        String hit = store.lookup(core);
        return hit == null || hit.isBlank() ? null : text.substring(0, start) + hit;
    }

    /** 這一段原本的樣式（顏色、粗體）；換字的時候照抄。 */
    private static net.minecraft.network.chat.Style styleOf(StyledText one) {
        net.minecraft.network.chat.Style[] found = {net.minecraft.network.chat.Style.EMPTY};
        one.getComponent().visit((style, text) -> {
            if (!text.isEmpty()) {
                found[0] = style;
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return found[0];
    }

    /** mixin 的入口；設定與語料從全域拿，順便記一筆診斷。 */
    public static String objective(String text) {
        try {
            String out = objective(text, WynnChaYuan.config(), WynnChaYuan.translations());
            WynnChaYuan.store().noteEvent(
                    out.equals(text) ? "objective.noMatch" : "objective.shown");
            return out;
        } catch (Throwable t) {
            return text;
        }
    }

    /**
     * 每日／公會目標那幾條（「Finish Quests: 2/3」）。
     *
     * <p>Wynntils 把它畫成進度條，字是從 {@code WynnObjective#asObjectiveString}
     * 來的，跟上面的樣板系統無關，所以另外一個入口、另外一個開關。
     *
     * @return 換好的字；不該換或翻不出來時原樣回傳
     */
    static String objective(String text, CollectorConfig config, TranslationStore store) {
        if (text == null || text.isBlank() || store == null || config == null
                || !config.translateObjectives()) {
            return text;
        }
        var translated = LineTranslator.translate(StyledText.fromString(text), store);
        return translated == null ? text : translated.getString();
    }

    /** 這一塊是追蹤欄嗎。用類別名比對，不必把 Wynntils 的型別帶進來。 */
    private static boolean tracker(Object overlay) {
        return overlay != null && TRACKER.equals(overlay.getClass().getSimpleName());
    }
}
