package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 任務對話的翻譯小框。
 *
 * <p>畫在畫面下緣、原對話文字的<b>上方</b>，兩邊都看得到——原文不動，
 * 譯文另外一塊，不影響原本的閱讀。
 *
 * <p>目前的內容由 {@link #setCurrent} 從對話事件餵進來，
 * 因為對話文字是走 action bar 的，不像 tooltip 那樣每幀都拿得到。
 */
public final class DialogueOverlay {

    /** 距畫面底部的高度，避開原本的對話文字與快捷列。 */
    private static final int BOTTOM_MARGIN = 70;

    private static final int PADDING = 4;

    private static volatile List<Component> current = List.of();

    /**
     * 玩家可以選的回答，畫在對話框<b>上面</b>另一塊。
     *
     * <p>遊戲本來就把選項與 NPC 的話分成兩塊畫。譯文如果全部擠進同一框，
     * 玩家得自己分辨哪幾行是別人說的、哪幾行是自己等一下要按的號碼——
     * 那正是原本畫面已經替他分好的事。
     */
    private static volatile List<List<Component>> choices = List.of();

    /**
     * 玩家目前選到第幾個選項（1 起算）；0 代表認不出來。
     *
     * <p>認不出來就<b>不標</b>——寧可沒有反白，也不要標錯一列害玩家按錯。
     * 來源見 {@link com.wynnchayuan.capture.DialogueChoices#selected}。
     */
    private static volatile int picked = 0;

    /**
     * 這一段對話的選項<b>原文</b>，由 {@link #noteChoices} 從原始 action bar 餵進來。
     *
     * <h2>為什麼要另外存一份</h2>
     * 選項的文字<b>不在</b> Wynntils 的對話事件裡（那邊只有 NPC 那一句），
     * 只在未經處理的 action bar 上。而 {@link #setCurrent} 是由對話事件驅動的，
     * 兩邊是不同的事件、不同的時機——所以原始那一手先存下來，
     * 等 setCurrent 跑的時候再一起翻。
     *
     * <p>實測選項<b>從第一幀就是完整的</b>（NPC 那句還在逐字打的時候就全在了），
     * 所以這裡不會收到打到一半的選項。
     */
    private static volatile List<String> rawChoices = List.of();

    /**
     * 現在是誰在說話。獨立存放，不再直接混進 {@link #current}。
     *
     * <p>就地取代要把名字畫在對話框上緣的名牌裡（像遊戲原本那樣），
     * 混在內文行裡就只是「第一行剛好是名字」，做不出名牌。
     */
    private static volatile String speaker = null;

    /** 這一段話要不要按 shift 才會繼續。就地取代時要畫出那個提示。 */
    private static volatile boolean needsShift = false;

    /** 每一行譯文是從哪一條原文來的，見 {@link #settledSource}。 */
    private static volatile List<String> settledFor = List.of();

    /** 每一行已經畫好的譯文，索引與 {@link #settledFor} 對齊。 */
    private static volatile List<List<Component>> shown = List.of();

    /**
     * 最後一次收到對話內容的時間。
     *
     * <p>沒有這個的話框會永遠留在畫面上——對話事件只在對話進行中發，
     * 走開之後就再也沒有東西來清它。改用「多久沒更新就隱藏」，
     * 不必依賴一個其實不可靠的「對話結束」事件。
     */
    private static volatile long lastUpdate = 0;

    /** 說話者那一行的顏色。刻意跟內文分開，一眼就看得出誰在講。 */
    private static final net.minecraft.network.chat.TextColor SPEAKER_COLOUR =
            net.minecraft.network.chat.TextColor.fromRgb(0xFFD966);

    private DialogueOverlay() {}

    /**
     * 這一段話要不要按 shift 才繼續。
     *
     * <p>由 {@link com.wynnchayuan.listener.CaptureListener} 從對話事件轉手。
     * 就地取代把原文整段藏掉，連那行提示一起藏了，得自己補回來——
     * 少了它，玩家不知道這段話還沒講完。
     */
    public static void setShiftPrompt(boolean required) {
        needsShift = required;
    }

    /**
     * 記下這一段對話的選項原文。
     *
     * <p>由 {@link com.wynnchayuan.listener.ActionBarListener} 從<b>未經處理的</b>
     * action bar 餵進來——選項只有在那一手資料裡才看得到，見
     * {@link com.wynnchayuan.capture.DialogueChoices}。
     *
     * <p>空的清單也要收：對話從「有選項」換到「沒有選項」時，
     * 得把上一段的選項清掉，否則會黏在畫面上。
     */
    public static void noteChoices(List<String> raw, int selected) {
        rawChoices = raw == null ? List.of() : List.copyOf(raw);
        picked = selected;
    }

    /** 對話內容變了就更新這裡；傳 null 或空的代表對話結束。 */
    public static void setCurrent(StyledText dialogue, TranslationStore store) {
        setCurrent(dialogue, store, false);
    }

    /**
     * @param hasChoices 這一段有多個可選的回答。遊戲把選項畫在對話框<b>上面</b>
     *                   另一個框裡，我們的譯文如果全部擠在同一塊，玩家分不出
     *                   哪幾行是 NPC 說的、哪幾行是自己可以選的。
     */
    public static void setCurrent(StyledText dialogue, TranslationStore store,
                                  boolean hasChoices) {
        if (hasChoices && dialogue != null) {
            // 選項對話的實際結構還沒定案，先把原文記下來，下一輪照真實資料實作
            com.wynnchayuan.translate.FlowedDebug.noteChoices(dialogue.getString());
        }
        // 內文關掉<b>不代表</b>選項也關掉——那是兩個開關。先前是同一個，
        // 於是「內文關、選項留面板」這個組合根本畫不出東西。
        boolean bodyOff = WynnChaYuan.config().dialogueMode()
                == CollectorConfig.DialogueMode.OFF;
        boolean choiceOff = WynnChaYuan.config().choiceMode()
                == CollectorConfig.DialogueMode.OFF;
        if (dialogue == null || (bodyOff && choiceOff)) {
            clear();                           // 快取也要跟著倒掉，否則下一段對話
            return;                            // 會拿上一段的結果去比對開頭
        }
        List<Component> lines = new ArrayList<>();
        // 選項不從這段文字裡分——它們根本不在這裡。
        //
        // 先前是靠 looksLikeChoice 找「1.」「[1]」那種號碼開頭的行，前提是選項
        // 混在同一段文字裡、而且帶號碼。實機資料把兩個前提都推翻了：
        // Wynntils 給的文字<b>只有 NPC 那一句</b>，而真正的選項
        //（「Are you headed someplace?」）<b>一個號碼都沒有</b>。
        // 所以那個判斷從來沒有成立過——這正是選項一直沒被翻譯的原因。
        //
        // 改成用 noteChoices 從原始 action bar 抽出來的那一份。
        List<List<Component>> options = choiceOff ? List.of() : translateChoices(store);
        // any 只管<b>本文</b>有沒有翻出來。查不到的那幾行也會被擺進 lines，
        // 全靠這個旗標攔著不顯示——把選項也算進來，英文原文就會漏上面板。
        boolean any = false;
        String said = null;
        String[] raws = dialogue.getString().split("\n");
        List<String> sources = new ArrayList<>(raws.length);
        List<List<Component>> rendered = new ArrayList<>(raws.length);
        for (int i = 0; i < raws.length; i++) {
            StyledText line = StyledText.fromString(raws[i]);
            String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
            List<Component> target = lines;

            // 還在打字：這一行是我們已經認出的那句話的開頭，那就別再算一次。
            // 見 #settledSource 說明——重算才是閃爍的來源。
            String settled = settledSource(i, template);
            if (settled != null) {
                sources.add(settled);
                rendered.add(shown.get(i));
                target.addAll(shown.get(i));
                any = true;
                if (said == null) {
                    said = speakerFor(store, settled);
                }
                continue;
            }

            LineResult result = translateLine(line, template, store);
            Component translated = result == null ? null : result.translated();
            String source = result == null ? null : result.source();
            if (translated != null) {
                any = true;
                if (said == null) {
                    said = speakerFor(store, template);
                }
                List<Component> box = Boxes.toLines(translated);
                sources.add(source);
                rendered.add(box);
                target.addAll(box);
            } else {
                // 認不出來的照原樣擺著，但<b>不</b>記進快取——下一個字進來時
                // 還要再試一次，這一行隨時可能變成認得出來的
                sources.add(null);
                rendered.add(List.of());
                target.addAll(Boxes.toLines(LineTranslator.untranslated(line)));
            }
        }
        // List.copyOf 不收 null，而認不出來的那幾行<b>就是</b>放 null。
        //
        // 於是只要對話裡有一句翻不出來，這裡就丟 NullPointerException，
        // 整個 setCurrent 在後面那幾行之前就被打斷——面板不會更新、
        // 說話者不會換，畫面上看起來像「這句沒翻譯」，實際上是整段沒跑完。
        // 日誌裡那幾千筆例外也是從這裡來的。
        settledFor = java.util.Collections.unmodifiableList(new ArrayList<>(sources));
        shown = List.copyOf(rendered);
        // 誰在說話。同一個場景裡好幾個 NPC 輪流講的時候，光看文字認不出來。
        // 語料裡本來就有這個欄位，只是先前沒被帶到畫面上。
        speaker = said != null && !said.isBlank() ? said : null;
        // 診斷：分得出「沒讀到對話」與「讀到了但沒譯文」——
        // 少了這個，畫面上沒東西時完全不知道卡在哪一步
        WynnChaYuan.store().noteEvent(any ? "dialogue.shown" : "dialogue.noMatch");
        current = any && !bodyOff ? List.copyOf(lines) : List.of();
        // 選項獨立於本文：NPC 那句查不到的時候，選項照樣要能顯示——
        // 玩家等一下就得從裡面挑一個。
        choices = options;
        if ((any && !bodyOff) || !options.isEmpty()) {
            lastUpdate = System.currentTimeMillis();
        }
    }

    /** 翻譯目前打到的這一行；抽成獨立入口，讓逐字輸入的 prefix 行為可以驗證。 */
    static LineResult translateLine(StyledText line, String template, TranslationStore store) {
        Component translated = LineTranslator.translate(line, store);
        String source = translated == null ? null : template;
        if (translated == null) {
            // NPC 是一個字一個字打出來的，打到一半的句子當然查不到。
            // 只要開頭夠獨特就先把<b>整句</b>譯文顯示出來——先前要等整句
            // 打完才出現，而玩家常常已經按 shift 跳過去了。
            // 卡住翻譯的不是長度門檻而是撞句。帶上目前追蹤的任務，
            // 範圍從幾萬句窄到一百多句，第一個字就分得出來了。
            source = store.matchPrefix(template, template.strip().length(),
                                       com.wynnchayuan.capture.CurrentQuest.get());
            if (source != null) {
                // prefix 認得出完整句，不代表目前已經打出所有參數。交回翻譯器
                // 還原地名／玩家名；參數尚未齊全就先等，不能把 raw {p}/{u}
                // 放進 shown，否則 settledSource 會一路沿用到整句完成。
                translated = LineTranslator.translateKnown(
                        line, store.lookup(source), store);
            }
        }
        return translated == null ? null : new LineResult(source, translated);
    }

    record LineResult(String source, Component translated) {}

    /**
     * 這一行是不是「剛才那句、只是又長了幾個字」。
     *
     * <h2>閃爍是怎麼來的</h2>
     * NPC 打字的每一個字都會發一次事件，同一句話因此會以幾十種長度進來。
     * 先前每一次都重算：半句話查不到 → 整框清空 → 下一個字剛好湊夠開頭 →
     * 又冒出來。一句話打完的過程中，框就這樣閃了十幾次。
     *
     * <p>認出來的那條原文記著，只要新進來的還是它的開頭，就直接沿用上一次的結果。
     * 一整句打完的過程中只算一次，畫面從頭到尾是穩的。
     *
     * @return 沿用的原文；這一行是新的、得重算時回傳 {@code null}
     */
    private static String settledSource(int index, String template) {
        List<String> sources = settledFor;
        List<List<Component>> boxes = shown;
        if (index >= sources.size() || index >= boxes.size()) {
            return null;
        }
        String source = sources.get(index);
        return canReuse(source, template) ? source : null;
    }

    /** 已認出的完整原文是不是仍包含目前打到的這個開頭。 */
    static boolean canReuse(String source, String template) {
        return source != null && template != null && source.startsWith(template.strip());
    }

    /**
     * 把 {@link #noteChoices} 收到的選項翻成中文。
     *
     * <p>每一個選項<b>各自查表</b>，不像 NPC 的台詞那樣整段併起來——它們是彼此
     * 獨立的句子，併起來查一定查不到。
     *
     * <p>查不到的照原文擺著。選項是玩家等一下要按的東西，<b>少一個都不行</b>：
     * 只翻出兩個、第三個整個消失，比三個都是英文糟得多。
     */
    static List<List<Component>> translateChoices(TranslationStore store) {
        List<String> raw = rawChoices;
        if (raw.isEmpty()) {
            return List.of();
        }
        // <b>一個選項一組</b>，不要攤平。
        //
        // 先前是攤成一串 Component，一個選項折成兩行之後就分不出哪兩行屬於
        // 同一個選項——那樣既畫不出分隔線，也標不出選到哪一個。
        List<List<Component>> out = new ArrayList<>(raw.size());
        for (String each : raw) {
            StyledText line = StyledText.fromString(each);
            Component hit = LineTranslator.translate(line, store);
            // 查不到就擺原文——選項不能少，見上面的說明
            out.add(List.copyOf(Boxes.toLines(
                    hit != null ? hit : LineTranslator.untranslated(line))));
        }
        return List.copyOf(out);
    }

    private static String speakerFor(TranslationStore store, String template) {
        String said = store.speakerOf(template);
        return said != null ? said : store.speakerOfPrefix(template);
    }

    /**
     * 現在有沒有畫得出來的譯文。
     *
     * <p>{@link com.wynnchayuan.listener.ActionBarListener} 用它決定能不能把
     * 原文藏掉——沒有譯文還藏，玩家就是對著空白按 shift。
     */
    public static boolean hasContent() {
        return !current.isEmpty() || !choices.isEmpty();
    }

    public static void clear() {
        current = List.of();
        choices = List.of();
        rawChoices = List.of();
        picked = 0;
        speaker = null;
        needsShift = false;
        settledFor = List.of();
        shown = List.of();
        lastUpdate = 0;
    }

    /** 每幀呼叫。沒有內容時什麼都不畫。 */
    public static void render(GuiGraphics graphics) {
        List<Component> lines = current;
        List<List<Component>> options = choices;
        if (lines.isEmpty() && options.isEmpty()) {
            return;
        }
        // 兩種模式的選項都走 drawChoices，位置才會一致。
        // 停留時間到了之後淡出，而不是啪一聲不見。NPC 講下一句時
        // lastUpdate 會被推後，透明度自然回到全滿——換內容不會有淡出。
        float alpha = Fade.alphaFor(lastUpdate, WynnChaYuan.config().dialogueHoldMs());
        if (alpha <= 0f) {
            current = List.of();               // 淡完了才真的清掉
            choices = List.of();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (WynnChaYuan.config().dialogueMode() == CollectorConfig.DialogueMode.REPLACE) {
            // 就地取代已經把譯文寫進遊戲自己的對話框了（見 DialogueRewriter），
            // 這裡再畫一塊就是同一句話出現兩次。
            //
            // 選項只在它自己的開關是「面板」時才畫。就地取代模式下遊戲自己的
            // 選項框已經是中文了（見 DialogueRewriter），這裡再畫一次就是
            // 同一組選項出現兩次。
            if (!options.isEmpty() && choicesInPanel()) {
                drawChoices(graphics, mc, options, alpha);
            }
            return;
        }
        // 小框模式：說話者仍然當成內文的第一行，跟先前一樣
        if (speaker != null && !lines.isEmpty()) {
            List<Component> withName = new ArrayList<>(lines.size() + 1);
            withName.add(speakerLine(speaker));
            withName.addAll(lines);
            lines = withName;
        }
        int lineHeight = mc.font.lineHeight + 1;
        int boxH = lines.isEmpty() ? 0 : lines.size() * lineHeight + PADDING * 2;
        int boxW = widthOf(mc, lines);

        // 玩家自己擺過就照他的，否則用預設錨點（下方置中）
        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - BOTTOM_MARGIN - boxH;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.DIALOGUE)) {
            // 存的是水平中心 —— 框寬隨對話長短變，對齊左緣的話短句會偏左
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.DIALOGUE) - boxW / 2;
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.DIALOGUE);
        }

        if (!lines.isEmpty()) {
            draw(graphics, mc, lines, x + boxW / 2, y, alpha, true);
        }
        // 選項也走 drawChoices，兩種模式的位置與樣式才會一致
        if (!options.isEmpty() && choicesInPanel()) {
            drawChoices(graphics, mc, options, alpha);
        }
    }

    /** 選項要不要畫在我們的面板上。見 {@link CollectorConfig#choiceMode()}。 */
    private static boolean choicesInPanel() {
        return WynnChaYuan.config().choiceMode() == CollectorConfig.DialogueMode.PANEL;
    }

    /**
     * 就地取代：畫一個真正的對話框，不是把字裸著擺在原文空出來的位置。
     *
     * <h2>為什麼要自己畫框</h2>
     * 原文那個框是 Wynncraft 用自訂字型與位移符號拼出來的，字一換掉就散了，
     * 沒辦法只換內容、保留外框。所以原文整段藏掉之後，框得由我們自己補：
     * 固定寬度的框、上緣一塊寫著說話者的名牌、下面一行 shift 提示。
     *
     * <p>寬度<b>固定</b>而不是隨字數變。NPC 是一個字一個字打出來的，寬度跟著
     * 內容跑的話，整句話打完的過程中框會一路長大，看起來像在抽搐。
     */
    private static void drawInPlace(GuiGraphics graphics, Minecraft mc,
                                    List<Component> lines, List<Component> options,
                                    float alpha) {
        int boxW = Math.max(MIN_BOX_W, Math.min(MAX_BOX_W,
                graphics.guiWidth() * 5 / 8));
        int inner = boxW - PADDING * 2 - 2;
        int lineHeight = mc.font.lineHeight + 1;

        List<FormattedCharSequence> body = wrapAll(mc, lines, inner);
        List<FormattedCharSequence> picks = wrapAll(mc, options, inner);

        int bodyH = body.isEmpty() ? 0 : body.size() * lineHeight + PADDING * 2;
        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - IN_PLACE_MARGIN - bodyH;

        if (!body.isEmpty()) {
            Boxes.draw(graphics, x, y, boxW, bodyH, alpha);
            int textY = y + PADDING;
            for (FormattedCharSequence line : body) {
                graphics.drawString(mc.font, line, x + PADDING + 1, textY,
                        Colors.fade(Colors.TEXT, alpha));
                textY += lineHeight;
            }
            // 名牌貼在框的上緣左側，跟遊戲原本的擺法一致
            if (speaker != null) {
                int nameW = mc.font.width(speaker) + PADDING * 2 + 2;
                int nameH = mc.font.lineHeight + PADDING;
                Boxes.draw(graphics, x, y - nameH + 1, nameW, nameH, alpha);
                graphics.drawString(mc.font, speaker, x + PADDING + 1,
                        y - nameH + PADDING / 2 + 2,
                        Colors.fade(SPEAKER_COLOUR.getValue() | 0xFF000000, alpha));
            }
            // 「SHIFT 以繼續」：原文那行提示也一起被藏掉了，要補回來，
            // 否則玩家不知道這段話還沒完
            if (needsShift) {
                int hintW = mc.font.width(SHIFT_HINT);
                graphics.drawString(mc.font, SHIFT_HINT,
                        x + (boxW - hintW) / 2, y + bodyH + 3,
                        Colors.fade(Colors.TEXT, alpha * 0.75f));
            }
        }

        if (!picks.isEmpty()) {
            int pickH = picks.size() * lineHeight + PADDING * 2;
            int pickY = y - pickH - CHOICE_GAP - (speaker == null ? 0 : mc.font.lineHeight);
            Boxes.draw(graphics, x, pickY, boxW, pickH, alpha);
            int textY = pickY + PADDING;
            for (FormattedCharSequence line : picks) {
                graphics.drawString(mc.font, line, x + PADDING + 1, textY,
                        Colors.fade(Colors.TEXT, alpha));
                textY += lineHeight;
            }
        }
    }

    /**
     * 選項單獨畫一塊，貼在畫面<b>右側</b>。
     *
     * <h2>為什麼不跟本文擺在一起</h2>
     * 先前選項是置中畫在對話框<b>正上方</b>的。就地取代模式底下 NPC 那句已經寫進
     * 遊戲自己的框了，本文是空的，於是 {@code bodyH} 為 0——選項框正好落在對話框
     * 的位置上，把它整個蓋掉（回報的畫面就是這樣）。
     *
     * <p>遊戲自己的選項清單在<b>右上角</b>。譯文貼到右邊，才是真的「出現在原文旁邊」，
     * 兩邊可以對著看。
     *
     * <p>位置也接進了 {@code PositionScreen}：使用者拖過就用他拖的，
     * 沒拖過就用右側這個預設錨點。
     */
    private static void drawChoices(GuiGraphics graphics, Minecraft mc,
                                    List<List<Component>> options, float alpha) {
        int boxW = Math.max(MIN_CHOICE_W, Math.min(MAX_CHOICE_W,
                graphics.guiWidth() / 4));
        int inner = boxW - PADDING * 2 - MARKER_W - 2;
        int lineHeight = mc.font.lineHeight + 1;

        // 先把每個選項各自折好，才知道整塊多高，也才畫得出分隔線
        List<List<FormattedCharSequence>> rows = new ArrayList<>(options.size());
        int textLines = 0;
        for (List<Component> option : options) {
            List<FormattedCharSequence> wrapped = wrapAll(mc, option, inner);
            if (wrapped.isEmpty()) {
                continue;
            }
            rows.add(wrapped);
            textLines += wrapped.size();
        }
        if (rows.isEmpty()) {
            return;
        }
        int boxH = textLines * lineHeight + PADDING * 2
                + (rows.size() - 1) * (ROW_GAP * 2 + 1);

        int x = graphics.guiWidth() - boxW - CHOICE_EDGE;
        int y = graphics.guiHeight() * 5 / 8;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.CHOICES)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.CHOICES)
                    - boxW / 2;
            // y 存的是<b>上緣</b>，跟對話框與名牌同一套。
            //
            // 先前這裡減了半個框高，而拖曳畫面存進去的是上緣——於是玩家排好的
            // 位置，進遊戲會往上跳半個框。四個框裡只有這一個是這樣算的。
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.CHOICES);
        }
        // 拖到畫面外就拉回來，不然框會整個看不見
        x = Math.max(2, Math.min(graphics.guiWidth() - boxW - 2, x));
        y = Math.max(2, Math.min(graphics.guiHeight() - boxH - 2, y));

        Boxes.draw(graphics, x, y, boxW, boxH, alpha);

        int textY = y + PADDING;
        for (int i = 0; i < rows.size(); i++) {
            List<FormattedCharSequence> option = rows.get(i);
            int rowH = option.size() * lineHeight;
            boolean here = picked == i + 1;      // picked 是 1 起算，0 代表認不出來

            if (here) {
                // 底色加亮：讓「選到哪一個」一眼看得出來，不必去數第幾行
                graphics.fill(x + 1, textY - ROW_GAP + 1, x + boxW - 1,
                              textY + rowH + ROW_GAP - 1,
                              Colors.fade(SELECTED_BG, alpha));
                graphics.drawString(mc.font, MARKER, x + PADDING,
                        textY + (rowH - mc.font.lineHeight) / 2,
                        Colors.fade(Colors.HIGHLIGHT, alpha));
            }
            int colour = here ? Colors.HIGHLIGHT : Colors.TEXT;
            int lineY = textY;
            for (FormattedCharSequence line : option) {
                graphics.drawString(mc.font, line, x + PADDING + MARKER_W, lineY,
                        Colors.fade(colour, alpha));
                lineY += lineHeight;
            }
            textY += rowH;

            // 選項之間畫一條虛線。實線太重，會跟外框搶；虛線只是分隔，不搶眼。
            if (i < rows.size() - 1) {
                int sepY = textY + ROW_GAP;
                for (int dx = x + PADDING; dx < x + boxW - PADDING;
                        dx += DASH + DASH_GAP) {
                    int end = Math.min(dx + DASH, x + boxW - PADDING);
                    graphics.fill(dx, sepY, end, sepY + 1,
                                  Colors.fade(Colors.FAINT, alpha));
                }
                textY += ROW_GAP * 2 + 1;
            }
        }
    }

    /** 選到的那一列的底色。比框底再亮一點，但不能亮到蓋掉文字。 */
    private static final int SELECTED_BG = 0x40FFD24A;

    /** 選到的那一列左邊的箭頭，跟遊戲自己的選單一致。 */
    private static final String MARKER = "\u25B6";

    /** 留給箭頭的寬度。每一列都留，選到與否文字才不會左右跳。 */
    private static final int MARKER_W = 8;

    /** 選項與分隔線之間的上下留白。 */
    private static final int ROW_GAP = 2;

    /** 虛線的實心段與空隙長度。 */
    private static final int DASH = 3;

    private static final int DASH_GAP = 2;

    /** 選項框的寬度上下限。選項通常是短句，比對話框窄。 */
    private static final int MIN_CHOICE_W = 120;

    private static final int MAX_CHOICE_W = 260;

    /** 選項框距畫面右緣的間距。 */
    private static final int CHOICE_EDGE = 8;

    /** 把每一行折到框寬以內；樣式由 {@code font.split} 保留。 */
    private static List<FormattedCharSequence> wrapAll(Minecraft mc,
                                                       List<Component> lines, int width) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component line : lines) {
            out.addAll(mc.font.split(line, Math.max(16, width)));
        }
        return out;
    }

    private static Component speakerLine(String said) {
        return Component.literal(said + "：").withStyle(
                net.minecraft.network.chat.Style.EMPTY
                        .withColor(SPEAKER_COLOUR).withBold(true));
    }

    /** 對話框的寬度上下限。太窄一句話折成六行，太寬眼睛得橫掃整個螢幕。 */
    private static final int MIN_BOX_W = 240;

    private static final int MAX_BOX_W = 420;

    private static final String SHIFT_HINT = "SHIFT 以繼續";

    /**
     * 就地取代時譯文距畫面底部的高度。
     *
     * <p>比 {@link #BOTTOM_MARGIN} 低——那個值是為了<b>避開</b>原本的對話文字，
     * 這裡則是要站到它原本的位置上。
     */
    private static final int IN_PLACE_MARGIN = 55;

    /** @param centerX 框的水平中心；框寬隨內容變，對齊左緣的話短的那一框會偏掉 */
    private static void draw(GuiGraphics graphics, Minecraft mc, List<Component> lines,
                             int centerX, int y, float alpha, boolean withBox) {
        int lineHeight = mc.font.lineHeight + 1;
        int boxW = widthOf(mc, lines);
        int boxH = lines.size() * lineHeight + PADDING * 2;
        int x = centerX - boxW / 2;
        if (withBox) {
            Boxes.draw(graphics, x, y, boxW, boxH, alpha);
        }
        int textY = y + PADDING;
        for (Component line : lines) {
            // 沒有底框時每一行各自置中——遊戲原本的對話就是置中的，
            // 靠左對齊會讓譯文看起來像另外貼上去的東西。
            int lineX = withBox
                    ? x + PADDING
                    : centerX - mc.font.width(line) / 2;
            graphics.drawString(mc.font, line, lineX, textY,
                    Colors.fade(Colors.TEXT, alpha));
            textY += lineHeight;
        }
    }

    private static int widthOf(Minecraft mc, List<Component> lines) {
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, mc.font.width(line));
        }
        return width + PADDING * 2;
    }

    /** 兩塊之間留一點空隙，才看得出來是兩件事。 */
    private static final int CHOICE_GAP = 3;
}
