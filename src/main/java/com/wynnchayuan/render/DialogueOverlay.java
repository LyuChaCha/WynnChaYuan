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
    private static volatile List<Component> choices = List.of();

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
        if (dialogue == null
                || WynnChaYuan.config().dialogueMode()
                        == CollectorConfig.DialogueMode.OFF) {
            clear();                           // 快取也要跟著倒掉，否則下一段對話
            return;                            // 會拿上一段的結果去比對開頭
        }
        List<Component> lines = new ArrayList<>();
        List<Component> options = new ArrayList<>();
        boolean any = false;
        String said = null;
        String[] raws = dialogue.getString().split("\n");
        List<String> sources = new ArrayList<>(raws.length);
        List<List<Component>> rendered = new ArrayList<>(raws.length);
        for (int i = 0; i < raws.length; i++) {
            StyledText line = StyledText.fromString(raws[i]);
            String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
            List<Component> target = hasChoices && looksLikeChoice(raws[i]) ? options : lines;

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
        settledFor = List.copyOf(sources);
        shown = List.copyOf(rendered);
        // 誰在說話。同一個場景裡好幾個 NPC 輪流講的時候，光看文字認不出來。
        // 語料裡本來就有這個欄位，只是先前沒被帶到畫面上。
        speaker = said != null && !said.isBlank() ? said : null;
        // 診斷：分得出「沒讀到對話」與「讀到了但沒譯文」——
        // 少了這個，畫面上沒東西時完全不知道卡在哪一步
        WynnChaYuan.store().noteEvent(any ? "dialogue.shown" : "dialogue.noMatch");
        current = any ? List.copyOf(lines) : List.of();
        choices = any ? List.copyOf(options) : List.of();
        if (any) {
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
            source = store.matchPrefix(template);
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
     * 這一行是玩家可以選的回答嗎。
     *
     * <p>事件只給我們一整串文字，哪幾行是 NPC 說的、哪幾行是選項，從事件本身
     * 看不出來——但遊戲要玩家按號碼，所以選項一定帶著號碼。認號碼就夠了。
     *
     * <p>認不出來的時候<b>不</b>硬分：整段照舊擺進對話框，跟先前一樣。
     * 分錯堆比不分還糟，玩家會以為 NPC 在講他自己的台詞。
     */
    private static boolean looksLikeChoice(String raw) {
        String text = StyledText.fromString(raw).getString().strip();
        int i = 0;
        while (i < text.length() && !Character.isLetterOrDigit(text.charAt(i))) {
            i++;                               // 跳過前面的符號與位移字元
        }
        if (i >= text.length() || !Character.isDigit(text.charAt(i))) {
            return false;
        }
        while (i < text.length() && Character.isDigit(text.charAt(i))) {
            i++;
        }
        // 號碼後面得有個分隔（「1.」「[1]」「1)」），純數字開頭的句子不算
        return i < text.length() && ".)]:-」".indexOf(text.charAt(i)) >= 0;
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
        speaker = null;
        needsShift = false;
        settledFor = List.of();
        shown = List.of();
        lastUpdate = 0;
    }

    /** 每幀呼叫。沒有內容時什麼都不畫。 */
    public static void render(GuiGraphics graphics) {
        List<Component> lines = current;
        List<Component> options = choices;
        if (lines.isEmpty() && options.isEmpty()) {
            return;
        }
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
            // 選項還是要畫：那是另一個框，改寫還沒處理到。
            if (!options.isEmpty()) {
                drawInPlace(graphics, mc, List.of(), options, alpha);
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
        // 選項擺在對話框正上方，跟遊戲原本的上下關係一致
        if (!options.isEmpty()) {
            int optionH = options.size() * lineHeight + PADDING * 2;
            draw(graphics, mc, options, x + boxW / 2,
                    y - optionH - CHOICE_GAP, alpha, true);
        }
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
