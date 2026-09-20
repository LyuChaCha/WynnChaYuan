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

    /** mixin 的入口：Wynntils 任務指引標記底下那行字。見 {@code WaypointTextMixin}。 */
    public static String marker(String text) {
        try {
            return marker(text, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            return text;
        }
    }

    /**
     * 任務指引的標記文字（通常就是任務名）。
     *
     * <p>跟追蹤欄同一個開關：追蹤欄關掉翻譯的人，指引也照原文。
     * 每一幀每個標記都會問一次，所以記住上一次的答案——語料換了就整份清掉。
     *
     * @return 翻好的字；不該換或翻不出來時原樣回傳
     */
    static String marker(String text, CollectorConfig config, TranslationStore store) {
        if (text == null || text.isBlank() || store == null || config == null
                || config.trackerMode() == CollectorConfig.DialogueMode.OFF) {
            return text;
        }
        synchronized (MARKERS) {
            if (markerStore != store) {
                MARKERS.clear();
                markerStore = store;
            }
            String hit = MARKERS.get(text);
            if (hit == null) {
                var translated = LineTranslator.translate(StyledText.fromString(text), store);
                hit = translated == null ? text : StyledText.fromComponent(translated).getString();
                if (MARKERS.size() > 256) {
                    MARKERS.clear();
                }
                MARKERS.put(text, hit);
            }
            return hit;
        }
    }

    private static final java.util.Map<String, String> MARKERS = new java.util.HashMap<>();
    private static TranslationStore markerStore;

    /** mixin 的入口：快捷列上方那行手持物品名稱。見 {@code HeldItemNameMixin}。 */
    public static net.minecraft.network.chat.Component heldItemName(
            net.minecraft.network.chat.Component name) {
        try {
            return heldItemName(name, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            return name;
        }
    }

    /**
     * 手持物品名稱。F6 開關預設關閉（名稱慣例留英文）。
     *
     * @return 翻好的名稱；關掉或翻不出來時原樣回傳
     */
    static net.minecraft.network.chat.Component heldItemName(
            net.minecraft.network.chat.Component name, CollectorConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null || !config.translateHeldItem()) {
            return name;
        }
        var translated = LineTranslator.translate(StyledText.fromComponent(name), store);
        return translated == null ? name : translated;
    }

    /** mixin 的入口：畫面頂端的 boss bar 標題。見 {@code BossBarNameMixin}。 */
    public static net.minecraft.network.chat.Component bossBar(
            net.minecraft.network.chat.Component name) {
        try {
            return bossBar(name, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            return name;
        }
    }

    /**
     * boss bar 標題：攻擊生物時掛上的「Horse - 47❤」這種。
     *
     * <p>整行查不到時一段一段查——名字與血量是不同顏色的兩段，名字就是名牌語料裡那一條。
     * 跟名牌翻譯同一個開關。每一幀都會畫，結果照原字記下；血量一直在變，記滿就清。
     *
     * @return 翻好的標題；關掉或翻不出來時原樣回傳
     */
    static net.minecraft.network.chat.Component bossBar(
            net.minecraft.network.chat.Component name, CollectorConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null || !config.translateNametags()) {
            return name;
        }
        if (store != barStore || BARS.size() > 256) {
            BARS.clear();
            barStore = store;
        }
        net.minecraft.network.chat.Component hit = BARS.get(name);
        if (hit == null) {
            StyledText text = StyledText.fromComponent(name);
            StyledText shown = line(text, store);
            hit = shown == text ? name : shown.getComponent();
            // 逐段那條路可能翻了別段（狀態詞），名字仍是英文，所以不論如何都再換一次
            net.minecraft.network.chat.Component named = bossBarName(hit, store);
            if (named != null) {
                hit = named;
            }
            net.minecraft.network.chat.Component worded = bossBarWords(hit, store);
            if (worded != null) {
                hit = worded;
            }
            BARS.put(name, hit);
            // 畫面上是英文時，先分清楚是「根本沒走到這裡」還是「走到了但查不到」：
            // captured.json 的 events 裡有沒有 bossbar.* 就知道。查不到的收進 capture。
            var captured = WynnChaYuan.store();
            if (captured != null) {
                captured.noteEvent(hit == name ? "bossbar.noMatch" : "bossbar.shown");
            }
            if (hit == name && config.collect()) {
                collectName(text, "label/bossbar");
            }
        }
        return hit;
    }

    /**
     * 只換 boss bar 開頭的怪物名：「Bronchial - 113k❤ - Weak Dam Def」。
     *
     * <h2>為什麼逐段查不到</h2>
     * Wynncraft 把「名字 - 血量❤ - 」放在<b>同一個</b>顏色段裡，逐段查表拿到的是
     * 整串，永遠對不上。名牌語料又是「Bronchial {#}{#}」——後面兩個是等級膠囊
     * 的圖示——所以只拿名字精確查也查不到。實機回報：頭上名牌是「支气管体」，
     * boss bar 還是 Bronchial。
     *
     * <p>先精確查名字，查不到再查名牌的鍵、拿掉尾巴的圖示。譯文裡還留著
     * 佔位符的不用，寧可留英文。名字跨了顏色段就放棄。
     *
     * @return 換好名字的標題；查不到回傳 {@code null}
     */
    static net.minecraft.network.chat.Component bossBarName(
            net.minecraft.network.chat.Component bar, TranslationStore store) {
        String plain = bar.getString();
        int cut = plain.indexOf(" - ");
        if (cut <= 0) {
            return null;
        }
        String head = plain.substring(0, cut).strip();
        if (head.isEmpty()) {
            return null;
        }
        String dst = store.lookup(head);
        if (dst == null) {
            String plate = store.lookup(head + " {#}{#}");
            if (plate != null && plate.endsWith("{#}{#}")) {
                dst = plate.substring(0, plate.length() - "{#}{#}".length()).strip();
            }
        }
        if (dst == null || dst.isEmpty() || dst.contains("{")) {
            return null;
        }
        String translated = dst;
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean[] done = {false};
        bar.visit((style, text) -> {
            int at = done[0] ? -1 : text.indexOf(head);
            if (at >= 0) {
                out.append(net.minecraft.network.chat.Component.literal(
                        text.substring(0, at) + translated
                                + text.substring(at + head.length())).withStyle(style));
                done[0] = true;
            } else if (!text.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(text).withStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return done[0] ? out : null;
    }

    /**
     * boss bar 右邊的屬性克制：{@code Weak}（易傷）、{@code Dam}（增傷）、{@code Def}（防護）。
     *
     * <p>這幾個字放進一般語料會換到別的地方去（Def、Dam 在介面上另有意思），
     * 所以譯法放在 {@code scoped/bossbar.json}，只在 boss bar 用。
     * 逐個英文單字查，前後的圖示、空白與顏色都不動。
     *
     * @return 換好的標題；一個字都沒換到回傳 {@code null}
     */
    static net.minecraft.network.chat.Component bossBarWords(
            net.minecraft.network.chat.Component bar, TranslationStore store) {
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean[] changed = {false};
        bar.visit((style, text) -> {
            if (text.isEmpty()) {
                return java.util.Optional.empty();
            }
            java.util.regex.Matcher m = WORD.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String dst = store.scopedLookup("bossbar", m.group());
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                        dst != null ? dst : m.group()));
                changed[0] |= dst != null;
            }
            m.appendTail(sb);
            out.append(net.minecraft.network.chat.Component.literal(sb.toString())
                    .withStyle(style));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return changed[0] ? out : null;
    }

    private static final java.util.regex.Pattern WORD =
            java.util.regex.Pattern.compile("[A-Za-z]+");

    private static final java.util.Map<net.minecraft.network.chat.Component,
            net.minecraft.network.chat.Component> BARS = new java.util.HashMap<>();
    private static TranslationStore barStore;

    /**
     * mixin 的入口：Wynntils 通知框（畫面中下方帶暗底的那一行）。
     * 見 {@code NotificationMixin}。
     *
     * <p>Wynntils 會把某些系統訊息攔下來改畫在這個框裡，那些訊息就不會再走聊天事件——
     * 所以先前既沒翻、也沒收進 capture。聊天翻譯關掉時不動。
     */
    public static StyledText notification(StyledText text) {
        try {
            CollectorConfig config = WynnChaYuan.config();
            TranslationStore store = WynnChaYuan.translations();
            if (text == null || text.isEmpty() || config == null || store == null
                    || config.chatMode() == CollectorConfig.ChatMode.OFF) {
                return text;
            }
            StyledText shown = line(text, store);
            if (shown == text && config.collect()
                    && !com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(text)) {
                String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(text);
                var captured = WynnChaYuan.store();
                if (captured != null && !template.isBlank()
                        && !com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)) {
                    captured.record(template, "desc", "chat", "chat/notification");
                }
            }
            return shown;
        } catch (Throwable t) {
            return text;
        }
    }

    /** mixin 的入口：實體頭上的自訂名稱。見 {@code EntityNameMixin}。 */
    public static net.minecraft.network.chat.Component entityName(
            net.minecraft.network.chat.Component name) {
        try {
            return entityName(name, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            return name;
        }
    }

    /**
     * 實體的自訂名稱：隱形盔甲座疊出來的那種浮空字（討伐戰的「Void Altar」
     * 與下面那行提示）。
     *
     * <p>這種字不是 TextDisplay，Wynntils 的名牌事件收不到，先前完全沒經過模組——
     * 沒翻、也沒收進 capture。跟名牌翻譯同一個開關。
     *
     * <p>每一幀都會畫，結果照原字記下；查不到的只在第一次看到時收進 capture。
     *
     * @return 翻好的名稱；關掉或翻不出來時原樣回傳
     */
    static net.minecraft.network.chat.Component entityName(
            net.minecraft.network.chat.Component name, CollectorConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null || !config.translateNametags()) {
            return name;
        }
        if (store != nameStore || NAMES.size() > 512) {
            NAMES.clear();
            nameStore = store;
        }
        net.minecraft.network.chat.Component hit = NAMES.get(name);
        if (hit == null) {
            StyledText text = StyledText.fromComponent(name);
            // 漂浮字那一支：專用譯法優先（見 TranslationStore#labelLookup），整塊查得到時
            // 照原文的分段上色（見 LineTranslator#labelColours）。先前只有查到專用譯法才走它，
            // 其餘的走一般那條路，寶箱上的字就整行一個顏色。都查不到才照舊逐段查。
            net.minecraft.network.chat.Component floating =
                    LineTranslator.translateFloating(text, store);
            StyledText shown = floating != null
                    ? StyledText.fromComponent(floating)
                    : byPart(text, store);
            hit = shown == text ? name : shown.getComponent();
            NAMES.put(name, hit);
            if (hit == name && config.collect()) {
                collectName(text, "label/floating");
            }
        }
        return hit;
    }

    /** 沒譯文的浮空字收進 capture，濾網跟 TextDisplay 那條路一樣。 */
    private static void collectName(StyledText text, String ctx) {
        if (text.isEmpty() || com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(text)
                || com.wynnchayuan.capture.CombatText.isIndicator(text)) {
            return;
        }
        String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(text);
        if (template.isBlank() || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(template)
                || com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)
                || com.wynnchayuan.capture.PlayerDataFilter.looksPlayerNamed(template)
                // 寵物與坐騎的名字帶著主人的 ID（「Tomzd{~}'s Bird」），
                // 數字被抽掉之後字形判準認不出來。見 #mentionsOnlinePlayerLoose。
                || com.wynnchayuan.capture.PlayerDataFilter.mentionsOnlinePlayerLoose(template)) {
            return;
        }
        var captured = WynnChaYuan.store();
        if (captured != null) {
            captured.record(template, "name", "label", ctx);
        }
    }

    private static final java.util.Map<net.minecraft.network.chat.Component,
            net.minecraft.network.chat.Component> NAMES = new java.util.HashMap<>();
    private static TranslationStore nameStore;

    /** mixin 的入口：Wynntils「手持物品名稱」疊層記下的那份字。見 {@code HeldItemOverlayMixin}。 */
    public static StyledText heldItemText(StyledText text) {
        try {
            if (text == null || text.isEmpty()) {
                return text;
            }
            net.minecraft.network.chat.Component shown = heldItemName(text.getComponent(),
                    WynnChaYuan.config(), WynnChaYuan.translations());
            return shown == text.getComponent() ? text : StyledText.fromComponent(shown);
        } catch (Throwable t) {
            return text;
        }
    }

    /** 這一塊是追蹤欄嗎。用類別名比對，不必把 Wynntils 的型別帶進來。 */
    private static boolean tracker(Object overlay) {
        return overlay != null && TRACKER.equals(overlay.getClass().getSimpleName());
    }
}
