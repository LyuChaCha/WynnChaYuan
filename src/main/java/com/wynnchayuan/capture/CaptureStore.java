package com.wynnchayuan.capture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 收集到的字串暫存於記憶體，定期批次寫入磁碟。
 *
 * <p>收集發生在渲染路徑上（每畫一次物品說明就可能觸發一次），所以絕不能在
 * 事件處理裡做檔案 I/O。這裡的做法是：事件只往 {@link ConcurrentHashMap} 塞資料，
 * 實際落地交給 {@link #flush()}，由背景執行緒或登出時呼叫。
 *
 * <p>寫檔採「先寫暫存檔再原子搬移」，避免遊戲當掉時留下半截 JSON 檔。
 */
public final class CaptureStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path file;
    private final Map<String, Captured> entries = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    /** 收集順序的流水號。見 {@link Captured#seq}。 */
    private final java.util.concurrent.atomic.AtomicInteger nextSeq =
            new java.util.concurrent.atomic.AtomicInteger();

    /**
     * 各種事件的觸發次數，會寫進 _meta.events。
     *
     * <p>沒收集到東西時，光看空的 entries 無法分辨是「事件沒發」還是「發了但被過濾掉」。
     * 這個計數器讓 captured.json 自己回答這個問題。
     */
    private final Map<String, java.util.concurrent.atomic.AtomicInteger> eventCounts =
            new ConcurrentHashMap<>();

    public CaptureStore(Path file) {
        this.file = file;
        load();
    }

    /** 一筆收集到的字串。欄位刻意與 corpus/workspace 的格式一致，方便合併。 */
    public static final class Captured {
        public String src;      // 模板化後的原文，{#} 代表材質包符號
        public String dst = ""; // 待譯者填寫
        public String role;     // name / desc / stat / text
        public String domain;   // quest / npc / ui / chat
        public String ctx;      // 出現位置
        public int seen = 1;    // 遇到次數，用來排優先度

        /**
         * 第幾個被收集到。
         *
         * <h2>為什麼要記順序</h2>
         * 條目是用雜湊當鍵的，所以檔案裡的順序等於隨機。對<b>任務對話</b>來說
         * 這是致命的：同一段對話的十幾句話會散在檔案各處，譯者看不出上下文，
         * 也不知道誰接誰——「他說了什麼」和「他在回答什麼」是兩回事。
         *
         * <p>記下收集順序之後，合併工具就能照原始順序輸出，同一段對話自然
         * 排在一起。
         */
        public int seq;

        Captured(String src, String role, String domain, String ctx, int seq) {
            this.src = src;
            this.role = role;
            this.domain = domain;
            this.ctx = ctx;
            this.seq = seq;
        }
    }

    /**
     * 已經有譯文的字串就不必收。見 {@link #record}。
     *
     * <p>用述詞而不是直接拿 {@code TranslationStore}，是為了讓這一條測得到，
     * 也讓收集端不必認識翻譯端。
     */
    private volatile java.util.function.Predicate<String> translated = t -> false;

    /**
     * 告訴收集端「哪些字串已經有譯文了」。
     *
     * <h2>為什麼一定要接上</h2>
     * {@code captured.json} 的用途是<b>還沒翻的清單</b>——檔頭自己就寫著
     * 「dst 留空代表尚未翻譯」。但先前它照單全收：看到什麼記什麼，
     * 語料裡明明已經有譯文的也照記，{@code dst} 一樣留空。
     *
     * <p>實機那一份 308 條裡有 <b>249 條</b>語料早就翻好了——
     * 「露營車駕駛」「建立角色」「{@code - 職業: 薩滿}」全在裡面。
     * 也就是說那個檔案有八成是雜訊，真正的缺口反而淹在裡面找不到。
     * 使用者回報「gap list 不可信」講的就是這件事。
     *
     * <p>接上之後，{@code captured.json} 就真的只剩<b>沒翻的</b>那些。
     */
    public void knowsTranslations(java.util.function.Predicate<String> lookup) {
        translated = lookup == null ? t -> false : lookup;
    }

    /**
     * 記錄一段文字。已存在則只累加計數。
     *
     * @return 是否為新字串
     */
    public boolean record(String template, String role, String domain, String ctx) {
        if (template == null || template.isBlank() || !GlyphSplitter.hasLetter(template)) {
            return false;   // 沒有字母 = 純符號或純數字，不值得記錄
        }
        if (translated.test(template)) {
            // 已經翻好了，這不是缺口。記個數就好——數字留著，
            // 「明明有譯文卻還是英文」那種問題才看得出來。
            noteEvent("skipped.translated");
            return false;
        }
        String key = hash(template);
        Captured existing = entries.get(key);
        if (existing != null) {
            existing.seen++;
            return false;
        }
        entries.put(key, new Captured(template.strip(), role, domain, ctx,
                nextSeq.getAndIncrement()));
        dirty.set(true);
        return true;
    }

    /** 記下某個事件發生過一次（不論最後有沒有記錄內容）。 */
    public void noteEvent(String name) {
        eventCounts.computeIfAbsent(name,
                k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
        dirty.set(true);
    }

    public boolean contains(String template) {
        return entries.containsKey(hash(template));
    }

    public int size() {
        return entries.size();
    }

    /**
     * 丟掉「已經有譯文、而且沒有人動過」的舊條目。
     *
     * <p>{@link #record} 從現在起不收已翻好的，但<b>上一版收進來的</b>還躺在
     * 檔案裡。它們才是雜訊的大宗（實機那份 308 條裡佔了 249 條）。
     *
     * <p>{@code dst} 有東西的一律不動——那是有人打的字，不管語料裡有沒有。
     *
     * @return 有沒有真的丟掉東西
     */
    private boolean prune() {
        int before = entries.size();
        entries.values().removeIf(c -> c.dst != null && c.dst.isBlank()
                && c.src != null && translated.test(c.src));
        int gone = before - entries.size();
        if (gone > 0) {
            eventCounts.computeIfAbsent("skipped.translated",
                    k -> new java.util.concurrent.atomic.AtomicInteger()).addAndGet(gone);
        }
        return gone > 0;
    }

    /** 把目前累積的內容寫到磁碟。沒有變動時直接跳過。 */
    public synchronized void flush() {
        if (prune()) {
            dirty.set(true);
        }
        if (!dirty.getAndSet(false)) {
            return;
        }
        JsonObject root = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("count", entries.size());
        meta.addProperty("note", "這裡只列<還沒有譯文>的字串——語料裡已經翻好的"
                + "不會出現在這裡（被略過幾次看 events 的 skipped.translated）。"
                + "dst 留空代表等人翻。{#} 是材質包符號、{~} 是數值、"
                + "{p} 是地名、{u} 是玩家名字，譯文都必須原樣保留。"
                + "seq 是收集順序，任務對話照它排就是原本的先後。");
        JsonObject events = new JsonObject();
        eventCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> events.addProperty(e.getKey(), e.getValue().get()));
        meta.add("events", events);
        root.add("_meta", meta);
        root.add("entries", GSON.toJsonTree(entries));

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            dirty.set(true);   // 沒寫成功，下次再試
            System.err.println("[WynnChaYuan] 寫入失敗 " + file + ": " + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject saved = root.getAsJsonObject("entries");
            if (saved == null) {
                return;
            }
            for (String key : saved.keySet()) {
                Captured c = GSON.fromJson(saved.get(key), Captured.class);
                entries.put(key, c);
                // 接續上次的流水號，重開遊戲之後收到的才會排在後面
                nextSeq.updateAndGet(n -> Math.max(n, c.seq + 1));
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 讀取失敗，將重新開始 " + file + ": " + e.getMessage());
        }
    }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.strip().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }
}
