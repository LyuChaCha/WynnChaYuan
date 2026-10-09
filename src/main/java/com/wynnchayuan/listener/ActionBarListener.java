package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.DialogueProbe;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.DialogueRewriter;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.event.ActionBarRenderEvent;
import com.wynntils.mc.event.SystemMessageEvent;
import com.wynntils.models.dialogue.actionbar.segments.DialogueSegment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 就地取代模式下，把遊戲自己那段對話文字藏掉。
 *
 * <h2>對話其實是 action bar</h2>
 * Wynncraft 的 NPC 對話不是聊天訊息，也不是什麼獨立的 GUI——它整塊塞在
 * <b>action bar</b> 裡，用自訂字型與位移符號排成畫面下方那個框。Wynntils 把它
 * 認成一個 {@link DialogueSegment}，跟血量、魔力、座標那些段落並排。
 *
 * <p>所以要「就地取代」，不必去攔繪製、不必寫 mixin：在
 * {@link ActionBarRenderEvent} 上把那一段停用，Wynntils 的
 * {@code removeDisabledSegments} 會在送去畫之前把它從字串裡剪掉，
 * 其他段落原封不動。譯文再由 {@link DialogueOverlay} 畫在它空出來的位置上。
 *
 * <h2>沒有譯文就不藏</h2>
 * 藏原文之前一定要先確定<b>有東西可以擺上去</b>。查不到譯文卻把原文剪掉，
 * 玩家就是對著一片空白按 shift——比沒翻譯糟得多。
 *
 * <h2>順便解決了「對話什麼時候結束」</h2>
 * 這個事件在 action bar 每次更新時都會發，內容裡有沒有 {@link DialogueSegment}
 * 就是對話還在不在的直接答案。先前只能靠「多久沒更新就隱藏」的計時器猜，
 * 因為 Wynntils 的 {@code Ended} 事件每打一個字就發一次，根本不是結束。
 */
public final class ActionBarListener {

    /**
     * 對話消失後再等幾次更新才收掉譯文。
     *
     * <p>Wynncraft 在兩句之間會有幾幀沒有對話段落。收得太急，每換一句話
     * 譯文就會閃一下。
     */
    private static final int GRACE = 3;

    private int missing = 0;

    /**
     * 上一次繪製時 action bar 裡有沒有對話段落。
     *
     * <p>{@link #onPlainActionBar} 拿它分流：有對話的那條路歸
     * {@link #onGameInfoRewrite}，沒有的才是大廳與職業選擇畫面的滑鼠提示。
     */
    private volatile boolean inDialogue = false;

    /**
     * action bar 的原始訊息，還沒被任何人動過。
     *
     * <p>{@link ActionBarRenderEvent} 拿到的是已經拆成段落的結果，而
     * {@link com.wynntils.models.dialogue.event.NpcDialogueEvent} 拿到的是
     * Wynntils 清理過的純文字——對話框那組自訂字型的資訊在那兩步都沒了。
     * 要弄清楚「保留原本的框、只換裡面的字」做不做得到，只能看這一手資料。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameInfo(SystemMessageEvent.GameInfoReceivedEvent event) {
        DialogueProbe.record(event.getMessage());
        // 有選項的對話另外留格子錄——不然八格會被沒有選項的用光，
        // 而最需要看的偏偏是有選項的那一種。見 DialogueProbe#recordChoices。
        DialogueProbe.recordChoices(event.getMessage());
        // 選項的文字只在這一手資料裡看得到——Wynntils 的對話事件只帶 NPC 那一句。
        // 見 DialogueChoices 的說明（含實機錄到的字型分區）。
        java.util.List<String> picks =
                com.wynnchayuan.capture.DialogueChoices.of(event.getMessage());
        DialogueOverlay.noteChoices(picks,
                com.wynnchayuan.capture.DialogueChoices.selected(event.getMessage()));
        // 每一幀都餵：太長的選項是一格一格捲過來的，整句只能這樣接回來。
        // 見 ChoiceScroll。
        if (picks.isEmpty()) {
            com.wynnchayuan.capture.ChoiceScroll.reset();
        } else {
            for (int i = 0; i < picks.size(); i++) {
                String stitched = com.wynnchayuan.capture.ChoiceScroll.feed(i, picks.get(i));
                if (stitched != null) {
                    noteFull(i, stitched);
                }
            }
        }
        collect(picks);
    }

    /** 上一次收過的選項。action bar 每幀都發，不擋就會一直重複記。 */
    private java.util.List<String> lastPicks = java.util.List.of();
    /** 還在動、等它停下來的那一組選項。見 {@link #collect}。 */
    private java.util.List<String> settling = java.util.List.of();
    private long settlingSince;

    /**
     * 選項要<b>連續穩定這麼久</b>才收。
     *
     * <p>放太短擋不住跑馬燈，放太長會漏掉一閃而過的對話。對話框至少會停留幾秒，
     * 而跑馬燈是每幾個 tick 就換一格，中間隔著好幾個數量級。
     */
    private static final long SETTLE_MS = 700;

    /**
     * 把選項收進語料。
     *
     * <h2>為什麼要另外收</h2>
     * {@code CaptureListener#record} 記的是 Wynntils 給的對話文字，而那裡面
     * <b>只有 NPC 那一句</b>。選項是我們自己從原始 action bar 抽出來的，
     * 從來沒有進過收集流程——所以它們不會出現在 `captured.json`，
     * 也就永遠不會有人翻。畫面上看得到、語料裡卻沒有，等於只做了一半。
     */
    private void collect(java.util.List<String> picks) {
        if (picks.isEmpty() || !WynnChaYuan.config().collect()) {
            settling = java.util.List.of();
            return;
        }
        // ★ 等選項<b>停下來</b>再收。
        //
        // 太長的選項 Wynncraft 會做成跑馬燈，一格一格往左捲。先前這裡只擋
        // 「跟上一次完全相同」，於是每一格都是新字串、每一格都收一條——
        // 實機回報：四個選項的對話收出了 56 條，而且每一條都是切一半的視窗
        //（「mber anything from before yo」「ber anything from before you」）。
        //
        // 跑馬燈捲動中的那幾格永遠不會停，所以永遠不會從這裡被收——這是對的：
        // 那幾格都是從單字中間切開的殘句，翻了也對不上。
        //
        // 但<b>第一格</b>不一樣：它會在開始捲之前停著，而它就是算繪端查表用的鍵。
        // 所以第一格照收，整句由 noteFull 另外補上。見 ChoiceScroll。
        if (!picks.equals(settling)) {
            settling = picks;
            settlingSince = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - settlingSince < SETTLE_MS
                || picks.equals(lastPicks)) {
            return;
        }
        lastPicks = picks;
        for (int i = 0; i < picks.size(); i++) {
            String pick = picks.get(i);
            if (com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(pick)) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
                continue;
            }
            // 第二道：漏網的殘句幾乎都是從字中間切開的。真正的選項是完整的
            // 句子，一律大寫或符號開頭。
            if (looksClipped(pick)) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.clipped");
                continue;
            }
            // 第三道：跑馬燈捲到<b>中間</b>停下來的那一格。
            //
            // 捲動中的視窗多半是小寫開頭，上面那一道就擋掉了；但停在字首剛好是
            // 大寫的那一格（「Peloros work hard…」）就會漏過來。
            //
            // 判準不用長度也不用標點——{@link com.wynnchayuan.capture.ChoiceScroll}
            // 手上有這一列的實際狀態：接得上前面累積的那幾格，就表示這一格是
            // 捲到一半的；沒在捲的選項（含剛好 27、28 個字元又沒有句尾標點的那種）
            // 自己就是第一格，不會被誤擋。
            if (!pick.equals(com.wynnchayuan.capture.ChoiceScroll.first(i))) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.midScroll");
                continue;
            }
            record(pick, com.wynnchayuan.capture.ChoiceScroll.full(i));
        }
    }

    /**
     * 這一列的整句拼好了：把它補到<b>第一格</b>那一條上面去。
     *
     * <p>收的時候（第一格停著不動那 700 毫秒）還沒開始捲，整句當然還沒有。
     * 所以要等拼完再回來補一次——{@code CaptureStore#record} 認得這件事。
     *
     * <p>這裡刻意<b>不看</b>{@link #collect} 那個「停穩 700 毫秒」的判斷：整句都
     * 接回來了就表示這一列確實是一個捲動中的選項，不是雜訊。有些跑馬燈在第一格
     * 停不到 700 毫秒，先前就是這樣整條漏掉的。
     */
    private void noteFull(int row, String stitched) {
        if (!WynnChaYuan.config().collect()) {
            return;
        }
        String firstWindow = com.wynnchayuan.capture.ChoiceScroll.first(row);
        if (firstWindow == null || looksClipped(firstWindow)) {
            return;
        }
        record(firstWindow, stitched);
    }

    /**
     * 把一條選項收進語料。
     *
     * @param pick 語料的鍵：畫面上那一格（跑馬燈的話是第一格）
     * @param full 接回來的整句，沒有就傳 {@code null}。見 {@code ChoiceScroll}
     */
    private void record(String pick, String full) {
        if (com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(pick)) {
            WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
            return;
        }
        // 整句也要過一次守門：它是好幾格接起來的，任何一格夾帶玩家資料都不能進去。
        if (full != null
                && com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(full)) {
            WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
            full = null;
        }
        com.wynntils.core.text.StyledText line =
                com.wynntils.core.text.StyledText.fromString(pick);
        String template = full == null ? null
                : com.wynnchayuan.capture.GlyphSplitter.toTemplate(
                        com.wynntils.core.text.StyledText.fromString(full));
        WynnChaYuan.store().record(
                com.wynnchayuan.capture.GlyphSplitter.toTemplate(line),
                "desc", "quest",
                com.wynnchayuan.capture.CurrentQuest.tag("dialogue/choice", null),
                template);
    }

    /**
     * 這一句是不是被跑馬燈從<b>字中間</b>切開的殘句。
     *
     * <p>Wynncraft 的選項都是寫成句子的，一律大寫字母或符號開頭
     *（{@code Just saying hello}、{@code Who are you?}）。捲動出來的視窗
     * 則是從單字中間切開的（{@code mber anything from before yo}）。
     *
     * <p>這是<b>第二道</b>：主要靠 {@link #collect} 的穩定判斷擋住。這一道只
     * 負責漏網的，所以寧可判準簡單也不要複雜到自己出錯。
     */
    static boolean looksClipped(String pick) {
        if (pick == null || pick.isEmpty()) {
            return false;
        }
        char first = pick.charAt(0);
        if (first >= 'a' && first <= 'z') {
            return true;
        }
        // 切在單字<b>中間</b>的視窗也可能是符號開頭：「'm not going to help you」
        // 就是從 I'm 的撇號切開的。沒有任何選項是這樣開頭的。
        return first == '\'' || first == ',' || first == '.' || first == ';'
                || first == ':' || first == '!' || first == '?';
    }

    /**
     * 就地取代：把譯文寫進 Wynncraft <b>自己那個</b>對話框裡。
     *
     * <p>優先權最低，所以 Wynntils 已經處理完才輪到我們——它那邊的段落解析
     * 讀的是原始英文，先被我們換成中文的話會整個認不出來
     * （自動翻頁那類功能就跟著壞掉）。
     *
     * <p>換不動就什麼都不做，原文照樣顯示。這比「藏掉原文卻補不上譯文」好，
     * 也比自己畫一個框好——框、名牌、頭像都在那條訊息裡，動它們就沒了。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGameInfoRewrite(SystemMessageEvent.GameInfoReceivedEvent event) {
        // 內文與選項是兩個開關，任一邊要就地取代就得進來——
        // 到底換哪幾段由 DialogueRewriter 自己再判斷一次。
        if (WynnChaYuan.config().dialogueMode() != CollectorConfig.DialogueMode.REPLACE
                && WynnChaYuan.config().choiceMode() != CollectorConfig.DialogueMode.REPLACE) {
            return;
        }
        try {
            var swapped = DialogueRewriter.rewrite(
                    event.getMessage(), WynnChaYuan.translations());
            if (swapped != null) {
                event.setMessage(swapped);
                DialogueProbe.after(swapped);
            } else {
                DialogueProbe.miss(event.getMessage());
                // 不是對話的那些（大廳的滑鼠提示之類）miss 收不到，見 plain
                DialogueProbe.plain(event.getMessage());
            }
        } catch (Throwable t) {
            // action bar 每 tick 都會走這裡，出錯絕不能讓遊戲停下來
            WynnChaYuan.store().noteEvent("dialogue.rewriteError");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onActionBarRender(ActionBarRenderEvent event) {
        boolean present = false;
        for (ActionBarSegment segment : event.getSegments()) {
            if (segment instanceof DialogueSegment) {
                present = true;
                break;
            }
        }

        inDialogue = present;
        if (!present) {
            if (++missing >= GRACE && DialogueOverlay.hasContent()) {
                DialogueOverlay.clear();
            }
            return;
        }
        missing = 0;
        // 原文還在畫面上：等玩家按 shift 的對話，譯文小框不可以自己先淡掉（#1081）
        DialogueOverlay.stillOnScreen();

        // 這裡不再藏原文。就地取代改成<b>改寫</b>那條訊息的內容
        // （見 onGameInfoRewrite）——藏掉的話，框、名牌、頭像會一起不見，
        // 那正是先前怎麼調都不像的原因。
    }

    /**
     * 不是對話的那幾行 action bar——大廳與 {@code /class} 畫面下方的滑鼠提示。
     *
     * <h2>為什麼要另外一條路</h2>
     * {@link #onGameInfoRewrite} 只處理對話，而且整支被「對話就地取代」那兩個
     * 開關擋著。職業選擇畫面下方那行
     * （<em>Left-Click to play　Right-Click to switch</em>）同樣塞在 action bar 裡，
     * 但它不是 {@code DialogueSegment}，所以以前既不會被收進語料、也不會被翻。
     *
     * <h2>怎麼擋住血量魔力那條</h2>
     * 正常遊玩時 action bar 裝的是血量、魔力、座標——那一條<b>一個拉丁字母都沒有</b>
     * （數字加上自訂字型的符號），所以 {@link GlyphSplitter#isGlyphOnly} 就擋掉了。
     * 再加上 {@link com.wynnchayuan.render.ThirdPartyLiterals#reserved}（別的模組
     * 靠英文判斷狀態的那些）與 {@link PlayerDataFilter}（夾帶玩家名的）兩道。
     *
     * <p>查不到譯文就什麼都不做，原文照舊——跟 title 那條路一樣。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlainActionBar(SystemMessageEvent.GameInfoReceivedEvent event) {
        if (inDialogue) {
            return;                       // 對話那條路在 onGameInfoRewrite
        }
        try {
            net.minecraft.network.chat.Component swapped =
                    plainSwap(event.getMessage());
            if (swapped != null) {
                event.setMessage(swapped);
            }
        } catch (Throwable t) {
            // 每 tick 都會走這裡，出錯絕不能讓遊戲停下來
            WynnChaYuan.store().noteEvent("actionbar.plainError");
        }
    }

    /**
     * @return 譯好的那一行；不該動或查不到時回傳 {@code null}（原文照舊）
     */
    private static net.minecraft.network.chat.Component plainSwap(
            net.minecraft.network.chat.Component original) {
        if (original == null) {
            return null;
        }
        com.wynntils.core.text.StyledText line =
                com.wynntils.core.text.StyledText.fromComponent(original);
        if (line.isEmpty()
                || com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(line)) {
            return null;                  // 血量魔力那條沒有字母，不碰
        }
        if (com.wynnchayuan.render.ThirdPartyLiterals.reserved(
                line.getStringWithoutFormatting())) {
            return null;
        }
        String template =
                com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
        if (com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)) {
            return null;
        }
        net.minecraft.network.chat.Component hit;
        if (hasSelectorSlot(line)) {
            // /class 整個畫面是一行 action bar，十幾個定位格的跳／退回位移全在
            // 裡面。LineTranslator 會把那些位移當成可調的欄距重排，整行 39 px
            // 變 1714 px、全部畫到畫面外（見 PairedFont#isSelector）。這種行
            // 只走 columnSwap：翻到的格子換字型補白，其餘逐片段原樣抄。
            hit = columnSwap(line, WynnChaYuan.translations());
        } else {
            hit = com.wynnchayuan.translate.LineTranslator.translate(
                    line, WynnChaYuan.translations());
            if (hit == null) {
                // 整行與逐片段都落空：再試一次「照空白切欄」，見 columnSwap
                hit = columnSwap(line, WynnChaYuan.translations());
            }
        }
        // Wynncraft 自己的定位 bug 也在這裡修：/class 第一個原型圖示站錯錨點
        // （見 SelectorRealign）。翻不翻都要修，所以不能縮在 hit != null 底下。
        com.wynnchayuan.render.SelectorRealign.Result fixed =
                com.wynnchayuan.render.SelectorRealign.apply(hit != null ? hit : original);
        net.minecraft.network.chat.Component out =
                hit != null || fixed.changed() ? fixed.out() : null;
        // 換成功的也要錄。action bar 是整行置中的，換完變窄就整行左移，
        // 而這條路以前只在<b>失敗</b>時才留診斷，於是永遠拿不到資料。
        // 矯正的結果一起寫進去：原文側與譯文側的 x 一比就知道有沒有拉回來。
        DialogueProbe.columns(original, out, fixed.report());
        if (hit != null) {
            leftovers(line, hit);
            return out;
        }
        // 真的查不到才收。先收再翻的話，<b>已經翻好</b>的這一行每一幀都會被
        // 記成缺口——實機第一份 captured.json 的 seen 就衝到 1700。
        if (WynnChaYuan.config().collect()) {
            // 這一行以前沒有任何地方收，語料才補得起來
            WynnChaYuan.store().record(template, "name", "actionbar", "actionbar");
        }
        return out;
    }

    /**
     * 欄與欄之間那一長串空白，兩邊都要有字才算。
     *
     * <p>{@code (?<=\S)} 與 {@code (?=\S)} 擋掉行首行尾的縮排——那種不是欄距，
     * 切下去只會多出一個空片段。
     */
    private static final java.util.regex.Pattern COLUMN_GAP =
            java.util.regex.Pattern.compile("(?<=\\S) {2,}(?=\\S)");

    /**
     * 同一個片段裡的兩欄，各自查表。
     *
     * <h2>為什麼逐片段那條路不夠</h2>
     * {@code LineTranslator} 是照<b>元件的片段</b>切的，而 {@code /class} 畫面
     * 下方那一行實機送來是<b>一個片段</b>：
     *
     * <pre>{@code  Left-Click to play                      Right-Click to switch}</pre>
     *
     * 中間那 22 個是普通的 U+0020，不是排版用的空白字型，所以片段不會在那裡斷。
     * 整串拿去查表當然落空，可是兩句各自早就翻好了。
     *
     * <h2>做法</h2>
     * 只在整行與逐片段都落空之後才走這裡，而且只動「含欄距的文字片段」：圖示、
     * 純空白、沒有欄距的片段原封不動抄過去，連字型與負寬度空白都不碰。欄距本身
     * 也照原樣留著——它用的是原片段的樣式，換成預設字型就沒有寬度了。
     *
     * <p>有一欄查到就算數，其餘保持原文：混著翻比整行英文好，而且兩欄之間本來
     * 就是各自獨立的提示。
     *
     * <p><b>已知的不完美</b>：中文比英文短，欄距照抄的話右邊那一欄會往左移幾像素。
     * action bar 是整行置中的，所以看起來仍然是置中的一行；真要補償得先知道那行
     * 的圖示是不是用絕對位移擺的，而那要等實機的 {@code actionbar-columns-*.txt}
     * （上面那支探針）。
     *
     * @return 換好的那一行；沒有任何一欄查得到時回傳 {@code null}
     */
    static net.minecraft.network.chat.Component columnSwap(
            com.wynntils.core.text.StyledText line,
            com.wynnchayuan.translate.TranslationStore store) {
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean any = false;
        for (com.wynntils.core.text.StyledTextPart part : line) {
            String raw = part.getString(null, com.wynntils.core.text.type.StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            com.wynntils.core.text.PartStyle ps = part.getPartStyle();
            net.minecraft.network.chat.Style style =
                    ps == null ? net.minecraft.network.chat.Style.EMPTY : ps.getStyle();
            net.minecraft.network.chat.FontDescription ours =
                    com.wynnchayuan.render.PairedFont.forStyle(style);
            if (ours != null) {
                com.wynnchayuan.render.PairedFont.warm(ours);
            }
            if (ours != null && com.wynnchayuan.render.PairedFont.MARKER.equals(raw)) {
                // 名字前面單獨那個 U+0001 是定位標記，要用<b>我們的</b>字型畫，
                // 後面的譯文才會被 shader 搬到該去的位置；見 PairedFont 的說明。
                // 要排在 isGlyphPart 前面：控制字元會被當成符號原樣抄走。
                out.append(literal(raw, style.withFont(ours)));
                continue;
            }
            if (com.wynnchayuan.capture.GlyphSplitter.isGlyphPart(part)
                    || raw.isBlank()) {
                out.append(literal(raw, style));
                continue;
            }
            if (!COLUMN_GAP.matcher(raw).find()) {
                // /class 的定位格：一格一句，沒有欄距。有我們配對字型的才翻
                // （原型名、簡介）；沒有的（Create a Character 在 top_middle）
                // 留英文——換成預設字型會把烘在 ascent 裡的高度丟掉。
                if (ours != null && com.wynnchayuan.capture.GlyphSplitter.hasLetter(raw)) {
                    any |= column(out, raw, style, store);
                } else {
                    out.append(literal(raw, style));
                }
                continue;
            }
            net.minecraft.network.chat.MutableComponent rebuilt =
                    net.minecraft.network.chat.Component.empty();
            java.util.regex.Matcher m = COLUMN_GAP.matcher(raw);
            boolean swapped = false;
            int at = 0;
            while (m.find()) {
                swapped |= column(rebuilt, raw.substring(at, m.start()), style, store);
                rebuilt.append(literal(m.group(), style));   // 欄距照原樣
                at = m.end();
            }
            swapped |= column(rebuilt, raw.substring(at), style, store);
            if (!swapped) {
                out.append(literal(raw, style));
                continue;
            }
            any = true;
            out.append(rebuilt);
        }
        return any ? out : null;
    }

    /**
     * 原型名那一格專用的譯名（{@code scoped/archetype.json}）。
     *
     * <h2>為什麼不能走一般語料</h2>
     * {@code Sharpshooter} 與 {@code Acrobat} 的裸名在 {@code gear-weapon.json} 是
     * <b>武器名</b>（{@code ctx: weapon/weapon}）。zh_tw 碰巧兩邊同字看不出來，
     * zh_cn 是「神射弓」「杂技弓」——弓的名字跑到職業選單上。一般語料一個原文只能
     * 有一種譯法（validate 的 {@code check_duplicates} 會擋），所以另外放，
     * 跟 {@code scoped/label.json} 的 {@code Back} 同一個道理。
     *
     * <p>回傳的片段刻意用<b>預設字型</b>：{@link #refont} 只換非自訂字型的那些，
     * 保留原本的 selector 字型會讓它整段跳過，中日韓字就畫不出來。
     *
     * @return 這一格不是原型名、或查不到時回 {@code null}
     */
    private static net.minecraft.network.chat.Component archetypeName(
            String chunk,
            net.minecraft.network.chat.Style style,
            com.wynnchayuan.translate.TranslationStore store) {
        String slot = com.wynnchayuan.render.PairedFont.slot(fontOf(style));
        if (slot == null || !slot.startsWith("selector_name_")) {
            return null;
        }
        String dst = store.scopedLookup("archetype", chunk);
        if (dst == null || dst.isBlank()) {
            return null;
        }
        return literal(dst, style.withFont(
                net.minecraft.network.chat.FontDescription.DEFAULT));
    }

    /** 這一行是不是 {@code /class}／角色選擇那個 HUD（見 {@link PairedFont#isSelector}）。 */
    private static boolean hasSelectorSlot(com.wynntils.core.text.StyledText line) {
        for (com.wynntils.core.text.StyledTextPart part : line) {
            com.wynntils.core.text.PartStyle ps = part.getPartStyle();
            if (ps != null && com.wynnchayuan.render.PairedFont.isSelector(ps.getStyle())) {
                return true;
            }
        }
        return false;
    }

    /** @return 這一欄有沒有換成譯文 */
    private static boolean column(net.minecraft.network.chat.MutableComponent out,
                                  String chunk,
                                  net.minecraft.network.chat.Style style,
                                  com.wynnchayuan.translate.TranslationStore store) {
        net.minecraft.network.chat.Component done = archetypeName(chunk, style, store);
        if (done == null) {
            done = com.wynnchayuan.translate.LineTranslator.translateChunk(
                    chunk, style, store);
        }
        if (done == null) {
            out.append(literal(chunk, style));
            collect(chunk);
            return false;
        }
        // 畫不出來就別換——方框比英文糟。跟對話那條路同一個守門。
        if (!com.wynnchayuan.render.DialogueRewriter.renderable(done.getString())) {
            out.append(literal(chunk, style));
            return false;
        }
        net.minecraft.network.chat.Component swapped = refont(done, style);
        // 簡介的鍵帶著 U+0001，譯文沒有——語料把它當成原文的一部分丟掉了。
        // 它是定位標記不是內容，而且必須是這一份字型<b>第一個</b>畫的字
        // （見 PairedFont 的說明），所以用我們的字型補回譯文最前面。
        if (chunk.startsWith(com.wynnchayuan.render.PairedFont.MARKER)
                && !swapped.getString().startsWith(com.wynnchayuan.render.PairedFont.MARKER)) {
            net.minecraft.network.chat.FontDescription ours =
                    com.wynnchayuan.render.PairedFont.forStyle(style);
            if (ours != null) {
                swapped = net.minecraft.network.chat.Component.empty()
                        .append(literal(com.wynnchayuan.render.PairedFont.MARKER,
                                        style.withFont(ours)))
                        .append(swapped);
            }
        }
        out.append(swapped);
        // 這一欄變窄多少就補回多少。
        //
        // action bar 是<b>整行置中</b>的，所以任何寬度變化都會讓整行左右跑掉
        // ——差的一半就是位移。/class 的下方提示列三欄一起翻完，整行少了
        // 152px，畫面往旁邊移了 76px，左邊的字被切掉、右邊的角色卡被推出
        // 畫面（使用者 2026-10-05 的截圖）。
        //
        // 這段先前被關掉過一次，當時的症狀其實是別的原因（Create a Character
        // 在 top_middle 被翻掉）造成的，關它並沒有解決問題，只是把真正的
        // 補償也一起拿走了。
        // 補在<b>這一欄自己後面</b>，下一個元件才會落回原位。
        //
        // 堆到行尾只修好整行總寬：排在這一欄<b>後面</b>的元件前面少了那幾十
        // 像素，會整批往左移（實機 152px，使用者 2026-10-05 的「整體靠左偏移」）。
        // 補在這裡則游標在離開這一欄時就已經回到原文的位置。
        int gap = owed(chunk, style, swapped);
        if (gap != 0) {
            String fill = com.wynnchayuan.translate.SpaceOffset.encode(gap);
            if (!fill.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(fill)
                        .setStyle(com.wynnchayuan.translate.SpaceOffset.styleFor(style)));
            }
        }
        return true;
    }

    /**
     * 整行只翻到一部分時，把<b>還是原文</b>的那幾段也收起來。
     *
     * <h2>為什麼</h2>
     * {@code /class} 整個畫面是<b>一條</b> action bar 字串，十幾個 HUD 元件都在
     * 裡面。只要其中一段查得到（實機是下方那行的
     * {@code Left-Click to Select}），整行就被當成「翻好了」，於是另外十幾段
     * ——職業原型的名字與簡介——既沒翻、也<b>永遠不會進 captured.json</b>。
     * 2026-10-05 要補那幾句時，六份診斷檔加 capture 裡一個字都找不到，就是
     * 卡在這裡。
     *
     * <h2>怎麼判斷「這一段沒翻到」</h2>
     * 譯文裡還<b>原封不動</b>出現同一串文字就算沒翻。這招不必知道翻譯那條路
     * 內部怎麼切片段，也就不會因為切法改了而默默失效。
     */
    private static void leftovers(com.wynntils.core.text.StyledText line,
                                  net.minecraft.network.chat.Component hit) {
        try {
            if (!WynnChaYuan.config().collect() || hit == null) {
                return;
            }
            String after = hit.getString();
            for (com.wynntils.core.text.StyledTextPart part : line) {
                if (com.wynnchayuan.capture.GlyphSplitter.isGlyphPart(part)) {
                    continue;
                }
                String raw = part.getString(
                        null, com.wynntils.core.text.type.StyleType.NONE);
                if (raw == null || raw.strip().length() < MIN_LEFTOVER
                        || !after.contains(raw)) {
                    continue;        // 不見了就是翻掉了
                }
                collect(raw);
            }
        } catch (Throwable t) {
            // 收集絕不能讓 action bar 掛掉
        }
    }

    /** 比這短的片段收進來只是雜訊：版本號、世界編號、單一個字母。 */
    private static final int MIN_LEFTOVER = 6;

    /**
     * 收下查不到的那一欄。
     *
     * <h2>為什麼不能只靠整行那一次</h2>
     * {@code plainSwap} 只在<b>整行都查不到</b>時才 record。{@code /class} 下方
     * 那一行三欄裡第一欄（{@code Left-Click to Select}）語料裡早就有，
     * {@code columnSwap} 因此回傳非 null，於是整行被當成「翻好了」——
     * 另外兩欄（{@code Scroll up/down to browse}、{@code Right-Click to return}）
     * 既沒翻、也<b>永遠不會出現在 captured.json</b>，補語料的人根本不知道它存在。
     * 使用者 2026-10-05 回報「class 頁面要翻一些語料」時，那幾句在六份診斷檔裡
     * 一個字都找不到，就是卡在這裡。
     *
     * <p>收的是模板（數字換成 {@code {~}}），跟其他收集點同一個形狀；
     * 玩家資料照樣先濾一遍。
     */
    private static void collect(String chunk) {
        try {
            if (!WynnChaYuan.config().collect() || chunk == null || chunk.isBlank()) {
                return;
            }
            String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(
                    com.wynntils.core.text.StyledText.fromString(chunk.strip()));
            if (template.isBlank()
                    || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(template)
                    || com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)
                    || com.wynnchayuan.capture.PlayerDataFilter.looksPlayerNamed(template)) {
                return;
            }
            WynnChaYuan.store().record(template, "name", "actionbar", "actionbar/column");
        } catch (Throwable t) {
            // 收集絕不能讓 action bar 掛掉
        }
    }

    /**
     * 補回這一欄被換掉之後少掉（或多出來）的像素。
     *
     * <h2>目前沒有接上，為什麼</h2>
     * 數據上它是對的：{@code actionbar-columns-2.txt} 量到整行淨寬原文 39 px、
     * 譯文 39 px，第二三欄確實回到原本的位置。可是實機看起來更糟
     * （使用者 2026-10-05 回報）。在拿到「補與不補」的對照截圖之前不要接回來
     * ——版面這種事 headless 驗不了（[[wynnchayuan-tooltip-widen-untested]]）。
     *
     * <p>診斷檔也顯示這條路比原本想的複雜：{@code /class} 整個畫面是<b>一條</b>
     * action bar 字串，十幾個 HUD 元件各自用
     * {@code [負位移] 文字 [負位移]} 夾著，而且每個元件有自己的字型
     * （{@code center_left/0/display_name}、{@code .../description}、
     * {@code bottom_middle}…）。在元件中間插東西只會動到<b>同一個元件裡</b>
     * 後面那幾欄，不會動到別的元件——這點已經由淨寬相同證實。
     *
     * <h2>為什麼非補不可</h2>
     * action bar 是<b>整行置中</b>的：Minecraft 量完整行寬度才決定左邊界。
     * {@code /class} 下方那行有三欄，只有查得到的那幾欄會換成中文，中文又比
     * 英文窄，於是整行變窄、左邊界往右跑，後面每一欄跟著位移——嚴重時整行
     * 被推出畫面（使用者 2026-10-05 回報的「class 的內容會跑掉」）。
     *
     * <p>欄與欄之間的空白是<b>原片段的樣式</b>照抄的（見 {@link #columnSwap}），
     * 所以補償不能靠改空白：那會連帶改到字型。改成在換好的那一欄後面補一個
     * {@code minecraft:space} 的寬度偏移字元——它只佔寬度、不畫任何東西，
     * 也不影響前後片段的字型與高度。
     *
     * <p>量不到寬度（headless 測試、畫面還沒開）就什麼都不做：位置會跑，
     * 但那是這一版之前的行為，不會更糟。
     */
    private static int owed(String chunk,
                            net.minecraft.network.chat.Style style,
                            net.minecraft.network.chat.Component swapped) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.font == null) {
                return 0;
            }
            int was = mc.font.width(literal(chunk, style));
            int now = mc.font.width(swapped);
            if (was <= 0 || now <= 0) {
                return 0;
            }
            return was - now;
        } catch (Throwable t) {
            return 0;        // 量不到就不補，絕不能讓 action bar 掛掉
        }
    }

    /**
     * 把換好的那一欄改用我們自己的字型。
     *
     * <h2>為什麼不能用預設字型</h2>
     * Wynncraft 把「畫在畫面的哪個高度」烘進了字型的 {@code ascent}：
     * {@code hud/selector/default/bottom_middle} 的拉丁字是 <b>-48</b>，而
     * {@code minecraft:default} 是 <b>7</b>。整行是<b>一個</b> action bar 字串，
     * 位移字元只能左右移不能上下移，所以換成預設字型就等於把高度丟掉——
     * 實機看到的正是「字跑到滑鼠圖示上面一大截」。
     *
     * <p>做法跟對話框那十一份字型一模一樣：ASCII 直接
     * {@code reference} Wynncraft 自己那一份（外觀與高度完全不變），
     * 中日韓走 Fusion Pixel 並用 {@code shift} 補回高度差。量過的關係是
     * {@code shift_y = 7 - ascent}（對話框那十一份全部吻合），所以這裡是
     * {@code 7 - (-48) = 55}。
     *
     * <p>配不到或沒附那一份就原樣回傳：位置會掉，但字看得見——
     * 那是上一版的行為，不會更糟。
     */
    private static net.minecraft.network.chat.Component refont(
            net.minecraft.network.chat.Component translated,
            net.minecraft.network.chat.Style original) {
        String name = pairedFont(fontOf(original));
        if (name == null) {
            return translated;
        }
        String lang = WynnChaYuan.language();
        if (!fontShipped(lang, name)) {
            return translated;
        }
        net.minecraft.network.chat.FontDescription ours =
                new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                WynnChaYuan.MOD_ID, "actionbar/" + lang + "/" + name));
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        translated.visit((style, text) -> {
            // 只動「重建時改成預設字型」的那些；圖示片段的字型不能碰
            net.minecraft.network.chat.Style use =
                    com.wynnchayuan.capture.GlyphSplitter.isCustomFont(style.getFont())
                            ? style : style.withFont(ours);
            out.append(net.minecraft.network.chat.Component.literal(text).setStyle(use));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out;
    }

    /** 這一段原本的字型對應到我們哪一份；邏輯與說明都在 {@link PairedFont}。 */
    private static String pairedFont(String font) {
        return com.wynnchayuan.render.PairedFont.slot(font);
    }

    private static boolean fontShipped(String lang, String name) {
        return com.wynnchayuan.render.PairedFont.shipped(lang, name);
    }

    private static String fontOf(net.minecraft.network.chat.Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }

    private static net.minecraft.network.chat.Component literal(
            String text, net.minecraft.network.chat.Style style) {
        return net.minecraft.network.chat.Component.literal(text).setStyle(style);
    }
}
