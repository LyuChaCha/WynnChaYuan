package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

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
        if (dialogue == null) {
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

            Component translated = LineTranslator.translate(line, store);
            String source = translated == null ? null : template;
            if (translated == null) {
                // NPC 是一個字一個字打出來的，打到一半的句子當然查不到。
                // 只要開頭夠獨特就先把<b>整句</b>譯文顯示出來——先前要等整句
                // 打完才出現，而玩家常常已經按 shift 跳過去了。
                source = store.matchPrefix(template);
                if (source != null) {
                    translated = Component.literal(store.lookupPrefix(template));
                }
            }
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
        if (said != null && !said.isBlank()) {
            lines.add(0, Component.literal(said + "：").withStyle(
                    net.minecraft.network.chat.Style.EMPTY
                            .withColor(SPEAKER_COLOUR).withBold(true)));
        }
        // 診斷：分得出「沒讀到對話」與「讀到了但沒譯文」——
        // 少了這個，畫面上沒東西時完全不知道卡在哪一步
        WynnChaYuan.store().noteEvent(any ? "dialogue.shown" : "dialogue.noMatch");
        current = any ? List.copyOf(lines) : List.of();
        choices = any ? List.copyOf(options) : List.of();
        if (any) {
            lastUpdate = System.currentTimeMillis();
        }
    }

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
        return source != null && source.startsWith(template.strip()) ? source : null;
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

    public static void clear() {
        current = List.of();
        choices = List.of();
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
            drawBox(graphics, mc, lines, x + boxW / 2, y, alpha);
        }
        // 選項擺在對話框正上方，跟遊戲原本的上下關係一致
        if (!options.isEmpty()) {
            int optionH = options.size() * lineHeight + PADDING * 2;
            drawBox(graphics, mc, options, x + boxW / 2,
                    y - optionH - CHOICE_GAP, alpha);
        }
    }

    /** @param centerX 框的水平中心；框寬隨內容變，對齊左緣的話短的那一框會偏掉 */
    private static void drawBox(GuiGraphics graphics, Minecraft mc,
                                List<Component> lines, int centerX, int y, float alpha) {
        int lineHeight = mc.font.lineHeight + 1;
        int boxW = widthOf(mc, lines);
        int boxH = lines.size() * lineHeight + PADDING * 2;
        int x = centerX - boxW / 2;
        Boxes.draw(graphics, x, y, boxW, boxH, alpha);
        int textY = y + PADDING;
        for (Component line : lines) {
            graphics.drawString(mc.font, line, x + PADDING, textY,
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
