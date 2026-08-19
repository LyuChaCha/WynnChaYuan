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

        Captured(String src, String role, String domain, String ctx) {
            this.src = src;
            this.role = role;
            this.domain = domain;
            this.ctx = ctx;
        }
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
        String key = hash(template);
        Captured existing = entries.get(key);
        if (existing != null) {
            existing.seen++;
            return false;
        }
        entries.put(key, new Captured(template.strip(), role, domain, ctx));
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

    /** 把目前累積的內容寫到磁碟。沒有變動時直接跳過。 */
    public synchronized void flush() {
        if (!dirty.getAndSet(false)) {
            return;
        }
        JsonObject root = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("count", entries.size());
        meta.addProperty("note", "dst 留空代表尚未翻譯。{#} 是材質包符號，譯文必須原樣保留。");
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
                entries.put(key, GSON.fromJson(saved.get(key), Captured.class));
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
