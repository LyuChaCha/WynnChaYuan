package com.wynnchayuan.capture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 滑過卡片就把<b>模組實際查表用的那個鍵</b>寫進 {@code cards.json}。
 *
 * <h2>為什麼 captured.json 不夠用</h2>
 * {@link GuiTextCapture} 收的是<b>逐行的碎片</b>：畫面上看到幾行就記幾條。
 * 內容書卡片的敘述卻是被 tooltip 寬度<b>自動斷</b>出來的——
 *
 * <pre>
 *   畫面  Bring [24 Fluffy Fur] to the
 *         Slaying Post [Combat Lv. 88] at
 *         [139, 61, -4399]
 *   查表  Bring [{~} Fluffy Fur] to the Slaying Post [Combat Lv. {~}] at [{~}, {~}, -{~}]
 * </pre>
 *
 * 模組查的是底下那一整句（見 {@code LineTranslator#rejoin}：行與行之間接<b>一個
 * 半形空白</b>）。照逐行的碎片去翻，翻出來的是逐行條目，而逐行條目會蓋掉整段
 * 那條路，畫面上就成了半中半英——這正是 {@code GuiTextCapture#covered} 在擋的事。
 *
 * <p>更糟的是純座標那一行：{@code [139, 61, -4399]} 整行只有數字，收集端當
 * fragment 跳過，於是<b>正負號</b>完全沒留下來。而 {@code [-{~}, {~}, -{~}]}
 * 與 {@code [{~}, {~}, -{~}]} 在語料裡是兩條不同的鍵，猜錯就等於沒翻。
 *
 * <h2>所以鍵從哪裡來</h2>
 * <b>不重新發明查表</b>。{@code TooltipPanel} 從最長試到兩行都落空之後，會把那幾行
 * 的模板接成一個 {@code key} 交給 {@code LineTranslator#noteBlockMiss}——
 * 那就是模組真正拿去查的東西。這裡掛在<b>同一個地方</b>，把同一個 key 多記一份。
 *
 * <p>{@code majorid-debug.txt} 也記這個，但它是散文、而且有名額上限
 * （實機常被登入時的聊天洗光）。這裡沒有上限，只做去重，而且寫成
 * 可以直接併進語料的形狀。
 *
 * <h2>開關</h2>
 * 沿用現成的「收集介面文字」（F6 →{@code collect()} + {@code collectGuiText()}），
 * 不另外加設定鍵。
 */
public final class CardDump {

    public static final String FILE = "cards.json";

    /** 大到不合理的 cards.json 不讀，見 {@code SafeFiles#readObject}。 */
    private static final long MAX_BYTES = 64L << 20;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** 還沒 {@link #init} 過就什麼都不做——測試與「診斷關著」都走這一條。 */
    private static volatile Path file;

    /** 收不收由誰說了算。正式跑是 F6 的開關，測試自己接管。 */
    private static volatile java.util.function.BooleanSupplier enabled = CardDump::configSaysYes;

    private static final Map<String, Card> cards = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> blocked = new ConcurrentHashMap<>();
    private static final AtomicInteger nextSeq = new AtomicInteger();
    private static final AtomicBoolean dirty = new AtomicBoolean();

    /**
     * 上一份處理過的 tooltip，以及它已經記過哪幾段。
     *
     * <h2>為什麼要兩層</h2>
     * tooltip 是<b>每一幀</b>重畫的，而一份 tooltip 裡可能有好幾段各自查不到。
     * 只比「是不是同一份」的話，同一幀的第二段會被第一段擋掉；只比「這一段記過
     * 沒有」的話，{@code seen} 會變成幀數（滑鼠停兩秒就 120），失去意義。
     *
     * <p>所以：換了 tooltip 才清空 {@link #thisHover}，同一份裡每一段各記一次。
     * {@code seen} 於是等於「滑過幾次」，照它排就是最該先翻的那些。
     */
    private static volatile List<Component> lastTooltip = List.of();

    private static final java.util.Set<String> thisHover =
            ConcurrentHashMap.newKeySet();

    private CardDump() {}

    /** 一張卡片上的一整段。欄位刻意與 {@code captured.json} 一致，方便併。 */
    public static final class Card {
        /** 攤平成一句之後的鍵——模組實際拿去查表的就是這個。 */
        public String src;
        /** 留空給譯者填，填完可以直接併進語料。 */
        public String dst = "";
        /** 這張卡是哪一張：tooltip 第一行的模板。 */
        public String card = "";
        /**
         * <b>還沒攤平</b>的逐行原樣，看得出斷行斷在哪裡。
         *
         * <p>攤平之後就看不出來了，而斷行位置正是「為什麼逐行收集會失真」的證據。
         */
        public List<String> lines = List.of();
        public String role = "desc";
        public String domain = "gui";
        public String ctx = "card/block";
        public int seen = 1;
        public int seq;
    }

    /**
     * 開始收。檔案路徑給了才會動，沒給就整支是空轉。
     */
    public static void init(Path target) {
        file = target;
        enabled = CardDump::configSaysYes;
        cards.clear();
        blocked.clear();
        thisHover.clear();
        lastTooltip = List.of();
        nextSeq.set(0);
        dirty.set(false);
        load();
    }

    /** 測試用：指定檔案，並且當成開關已經打開。 */
    public static void forTest(Path target) {
        init(target);
        enabled = () -> true;
    }

    /** 測試用：把狀態清乾淨，下一個案例才不會撞到上一個。 */
    public static void forget() {
        file = null;
        enabled = CardDump::configSaysYes;
        cards.clear();
        blocked.clear();
        thisHover.clear();
        lastTooltip = List.of();
        nextSeq.set(0);
        dirty.set(false);
    }

    private static boolean configSaysYes() {
        CollectorConfig config = WynnChaYuan.config();
        return config != null && config.collect() && config.collectGuiText();
    }

    /**
     * 這一份 tooltip 是玩家頭顱的嗎。
     *
     * <h2>為什麼要別人告訴我們</h2>
     * 帳號名<b>沒有形狀</b>可以認（見 {@code GuiTextCapture#record} 的長篇說明），
     * 但「這一格是玩家頭顱」是確定的事實。只是算繪那一端拿不到 {@code ItemStack}——
     * {@code TooltipPanel#translateLines} 只有幾行文字。
     *
     * <p>{@link GuiTextCapture#record} 本來就在同一個事件裡、而且<b>先</b>跑，
     * 它算 {@code skipTitle} 的時候順手把結果放過來。多寫一份判斷遲早會不一致。
     */
    private static volatile boolean playerHead;

    /** 見 {@link #playerHead}。由 {@link GuiTextCapture} 呼叫。 */
    public static void fromPlayerHead(boolean head) {
        playerHead = head;
    }

    /**
     * 記一段查不到的跨行原文。
     *
     * <p>掛在 {@code TooltipPanel} 呼叫 {@code noteBlockMiss} 的<b>同一個地方</b>，
     * 但拿的<b>不是</b>同一個 key——見 {@link #paragraphKey}。
     *
     * @param tooltip 整份 tooltip（拿來認「還是同一份嗎」，以及整份才看得出來的隱私判斷）
     * @param styled  同一份，已經拆好的樣子
     * @param key     這一<b>段</b>每一行的模板用行分隔符接起來——還沒攤平
     */
    /**
     * 從第 {@code at} 行開始的<b>那一段</b>，每行的模板用行分隔符接起來。
     *
     * <h2>為什麼不能沿用 noteBlockMiss 的 key</h2>
     * 那一個是「從最長試到兩行」的<b>診斷窗格</b>，長度是
     * {@code min(maxBlockLines, 剩下幾行)}。實測語料裡最長的整段條目有 15 行，
     * 所以一張 19 行的迷你任務卡從敘述起點算起，窗格會一路吃到 Wynntils 自己
     * 加在最底下的那兩行中文提示：
     *
     * <pre>
     *   ✔À Recommended Combat Lv: {~} … {#}{#} Click To Track {#} 中鍵點擊在地圖上查看！
     * </pre>
     *
     * 那種鍵含中文，會被底下的隱私那一關整段擋掉——擋掉的正是卡片敘述本身。
     * 就算沒被擋，收到的也是「敘述＋需求＋獎勵＋追蹤提示」黏成一串，不是語料
     * 的鍵，拿到也不能用。
     *
     * <p>語料的整段條目本來就是<b>一個段落</b>，{@code translateBlock} 命中的
     * 也是那種條目，所以這裡自己數到下一個空行為止。空行的認法跟呼叫端的
     * {@code paragraphStart} 一模一樣（剝掉色碼再看 {@code isBlank}），兩邊才
     * 對稱——不然會出現「起點算在這裡、終點算在那裡」的縫。
     *
     * <h2>只收兩行以上</h2>
     * 一行的段落走的是逐行查表那條路，{@code captured.json} 本來就會收，而且
     * 這裡分不出它到底翻到了沒（逐行翻譯發生在這之後），收進來只會混入一堆
     * 其實已經翻好的東西。這個檔的價值就在<b>攤平後的多行鍵</b>——那是
     * {@code captured.json} 給不了的。
     *
     * @return 這一段的 key；不足兩行時回傳 {@code null}
     */
    public static String paragraphKey(List<StyledText> styled, int at) {
        if (styled == null || at < 0 || at >= styled.size()) {
            return null;
        }
        int end = at;
        while (end < styled.size()
                && !styled.get(end).getStringWithoutFormatting().isBlank()) {
            end++;
        }
        if (end - at < 2) {
            return null;                       // 見上：一行的交給 captured.json
        }
        StringBuilder key = new StringBuilder();
        for (int k = at; k < end; k++) {
            if (k > at) {
                key.append(System.lineSeparator());
            }
            key.append(LineParts.of(styled.get(k)).template());
        }
        return key.toString();
    }

    public static void note(List<Component> tooltip, List<StyledText> styled, String key) {
        try {
            if (file == null || key == null || key.isBlank()
                    || tooltip == null || tooltip.isEmpty()
                    || styled == null || styled.isEmpty()
                    || !enabled.getAsBoolean()) {
                return;
            }
            // 換了 tooltip 才重算，見 #thisHover。
            if (!tooltip.equals(lastTooltip)) {
                lastTooltip = List.copyOf(tooltip);
                thisHover.clear();
            }
            if (!thisHover.add(key)) {
                return;                        // 同一份的下一幀，沒有新東西
            }
            record(styled, key);
        } catch (Throwable t) {
            // 這條路跑在 Wynntils／原版的算繪裡。收集絕不能反過來弄壞畫面。
        }
    }

    private static void record(List<StyledText> styled, String key) {
        String src = TranslationStore.normalise(key);
        if (src.isBlank() || !GlyphSplitter.hasLetter(src)) {
            tally("skipped.noLetter");
            return;
        }
        List<String> lines = List.of(key.split("\\R", -1));

        // ---- 隱私。用的全是 GuiTextCapture 那一套，不另外寫一份 ----
        //
        // 整份先攤成模板：「這是不是一張隊伍卡」看的是<b>整份</b>，
        // 逐行看的時候判斷不出來（見 PlayerDataFilter#isPartyCard）。
        List<String> whole = new ArrayList<>(styled.size());
        for (StyledText line : styled) {
            whole.add(GlyphSplitter.isGlyphOnly(line)
                    ? null : GlyphSplitter.toTemplate(line));
        }
        if (PlayerDataFilter.isPartyCard(whole)) {
            tally("blocked.partyCard");
            return;
        }
        String title = titleOf(whole);
        // 玩家頭顱的標題就是帳號名。這裡跟 GuiTextCapture 不一樣：那邊只丟掉
        // 標題那一行照收其餘，我們的每一筆<b>都帶著標題</b>（card 欄），
        // 丟不掉，所以整份不收。
        if (playerHead) {
            tally("blocked.playerHead");
            return;
        }
        // 跨行的片語要拿整段比（開箱廣播會被折行，見 PlayerDataFilter#carriesPlayerData）；
        // looksAccountNamed 只看它拿到的第一行，所以得逐行問。
        //
        // 整段比的時候要先把行分隔符換成純 {@code \n}：那邊的折行比對認的是
        // {@code \n}，而 key 用的是 {@code System.lineSeparator()}——在 Windows 上
        // 多出來的 {@code \r} 會卡在折行處，整則廣播就這樣穿過去。
        String joined = String.join("\n", lines);
        if (suspect(joined) || suspect(title)) {
            tally("blocked.playerData");
            return;
        }
        for (String line : lines) {
            if (suspect(line)) {
                tally("blocked.playerData");
                return;
            }
        }

        Card existing = cards.get(src);
        if (existing != null) {
            existing.seen++;
            // 斷行位置會隨畫面寬度變。留<b>第一次</b>看到的那一份就夠了，
            // 要看的是「有斷行這回事」，不是收集所有斷法。
            dirty.set(true);
            tally("seen.again");
            return;
        }
        Card fresh = new Card();
        fresh.src = src;
        fresh.card = title == null ? "" : title;
        fresh.lines = lines;
        fresh.seq = nextSeq.getAndIncrement();
        cards.put(src, fresh);
        dirty.set(true);
        tally("recorded");
    }

    /** 這一行夾帶了別人的資料嗎。兩道都問，跟 {@link GuiTextCapture} 同一批。 */
    private static boolean suspect(String template) {
        return template != null
                && (PlayerDataFilter.carriesPlayerData(template)
                    || PlayerDataFilter.looksAccountNamed(template));
    }

    /**
     * 這張卡是哪一張。
     *
     * <p>取第一行的模板。但物品名稱其實是<b>兩行</b>——第 0 行寬度是 0，
     * 玩家看不見（見 {@code TooltipPanel}）。所以第 0 行沒有字時往下找一行。
     */
    private static String titleOf(List<String> whole) {
        for (int i = 0; i < whole.size() && i < 2; i++) {
            String line = whole.get(i);
            if (line != null && !line.isBlank() && GlyphSplitter.hasLetter(line)) {
                return line.strip();
            }
        }
        return null;
    }

    private static void tally(String event) {
        blocked.computeIfAbsent(event, k -> new AtomicInteger()).incrementAndGet();
    }

    /** 目前收了幾條。 */
    public static int size() {
        return cards.size();
    }

    /**
     * 寫檔。
     *
     * <p>收集發生在算繪路徑上，所以這裡<b>絕不能</b>被算繪呼叫——
     * 跟 {@code CaptureStore#flush} 一樣交給背景執行緒，30 秒一次。
     */
    public static synchronized void flush() {
        Path target = file;
        if (target == null || !dirty.getAndSet(false)) {
            return;
        }
        try {
            JsonObject root = new JsonObject();
            root.addProperty("_note", NOTE);
            JsonObject meta = new JsonObject();
            meta.addProperty("count", cards.size());
            // 說明只寫一份（在 _note）。這段字不短，兩邊都寫等於每次存檔多存一份。
            JsonObject events = new JsonObject();
            blocked.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> events.addProperty(e.getKey(), e.getValue().get()));
            meta.add("events", events);
            root.add("_meta", meta);
            root.add("entries", rowsJson());
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            com.wynnchayuan.SafeFiles.moveAtomically(tmp, target);
        } catch (Throwable t) {
            dirty.set(true);                   // 沒寫成功，下次再試
            System.err.println("[WynnChaYuan] 寫入失敗 " + target + ": " + t);
        }
    }

    /**
     * 照收集順序寫，不是照雜湊順序。
     *
     * <p>鍵取自卡片標題——同一張卡的幾段自然排在一起，翻的人才看得出上下文。
     */
    private static JsonObject rowsJson() {
        JsonObject rows = new JsonObject();
        List<Card> ordered = cards.values().stream()
                .sorted(java.util.Comparator.comparingInt(c -> c.seq))
                .toList();
        java.util.Set<String> used = new java.util.HashSet<>();
        int n = 0;
        for (Card c : ordered) {
            String base = c.card == null || c.card.isBlank() ? "?" : c.card;
            String key = base + " #" + String.format("%03d", ++n);
            String unique = key;
            for (int k = 2; !used.add(unique); k++) {
                unique = key + "~" + k;
            }
            JsonObject row = new JsonObject();
            row.addProperty("src", c.src);
            row.addProperty("dst", c.dst == null ? "" : c.dst);
            row.addProperty("card", c.card == null ? "" : c.card);
            JsonArray raw = new JsonArray();
            for (String line : c.lines) {
                raw.add(line);
            }
            row.add("lines", raw);
            row.addProperty("role", c.role);
            row.addProperty("domain", c.domain);
            row.addProperty("ctx", c.ctx);
            row.addProperty("seen", c.seen);
            row.addProperty("seq", c.seq);
            rows.add(unique, row);
        }
        return rows;
    }

    /**
     * 讀回上次留下的內容。
     *
     * <p>非讀不可的是 {@code dst}：譯者可能已經在這個檔上填了字，
     * 重開遊戲就被蓋掉的話沒有人敢用它。
     */
    private static void load() {
        Path target = file;
        if (target == null) {
            return;
        }
        JsonObject root = com.wynnchayuan.SafeFiles.readObject(target, MAX_BYTES);
        if (root == null) {
            return;
        }
        JsonElement rows = root.get("entries");
        if (rows == null || !rows.isJsonObject()) {
            return;
        }
        int top = 0;
        for (Map.Entry<String, JsonElement> e : rows.getAsJsonObject().entrySet()) {
            try {
                if (!e.getValue().isJsonObject()) {
                    continue;                  // 壞掉的只跳過那一條
                }
                JsonObject row = e.getValue().getAsJsonObject();
                if (!row.has("src") || row.get("src").getAsString().isBlank()) {
                    continue;
                }
                Card c = new Card();
                c.src = row.get("src").getAsString();
                c.dst = row.has("dst") ? row.get("dst").getAsString() : "";
                c.card = row.has("card") ? row.get("card").getAsString() : "";
                List<String> lines = new ArrayList<>();
                if (row.has("lines") && row.get("lines").isJsonArray()) {
                    for (JsonElement one : row.getAsJsonArray("lines")) {
                        lines.add(one.getAsString());
                    }
                }
                c.lines = List.copyOf(lines);
                c.seen = row.has("seen") ? row.get("seen").getAsInt() : 1;
                c.seq = row.has("seq") ? row.get("seq").getAsInt() : top;
                top = Math.max(top, c.seq + 1);
                cards.put(c.src, c);
            } catch (Throwable t) {
                // 舊版格式、少了欄位：只跳過那一條
            }
        }
        nextSeq.set(top);
    }

    /** 檔頭那段中文說明。寫在檔案裡，拿到檔案的人不必先問怎麼用。 */
    private static final String NOTE =
            "滑鼠滑過內容書卡片（以及任何走 tooltip 的面板）時收下來的。"
            + "src 是模組<b>實際拿去查表</b>的整段鍵——幾行原文攤平成一句，"
            + "行與行之間接一個半形空白。要補翻譯就補這一句，"
            + "不要照 lines 逐行翻：逐行條目會蓋掉整段那條路，畫面上會變成半中半英。"
            + " lines 是還沒攤平的逐行原樣，只是給人看斷行斷在哪裡，不是要翻的東西。"
            + " card 是這一段屬於哪一張卡（tooltip 第一行）。"
            + " dst 留空代表等人翻，填完可以直接併進語料。"
            + " {#} 是材質包符號、{~} 是數值、{p} 是地名、{u} 是玩家名字，譯文都必須原樣保留——"
            + "座標那種 [-{~}, {~}, -{~}] 連正負號都是鍵的一部分，少一個就是另一條。"
            + " seen 是滑過幾次（不是幀數），照它排就是最該先補的。"
            + " 這裡<b>只列查不到的</b>：語料裡整段已經翻好的不會出現。"
            + " 沒有名額上限，只去重；收不收由 F6 的「收集介面文字」決定。";
}
