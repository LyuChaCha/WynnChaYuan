package com.wynnchayuan.capture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.ModLog;
import com.wynnchayuan.SafeFiles;
import com.wynnchayuan.render.ThirdPartyLiterals;
import com.wynnchayuan.translate.ErrorDebug;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class CaptureStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path file;
    private final Map<String, Captured> entries = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    private final AtomicInteger nextSeq = new AtomicInteger();

    private final Map<String, AtomicInteger> eventCounts = new ConcurrentHashMap<>();

    public CaptureStore(Path file) {
        this.file = file;
        load();
    }

    public static final class Captured {
        public String src;
        public String dst = "";
        public String role;
        public String domain;
        public String ctx;
        public int seen = 1;

        public String full;

        public int seq;

        public String in;

        public String place;

        Captured(String src, String role, String domain, String ctx, int seq) {
            this.src = src;
            this.role = role;
            this.domain = domain;
            this.ctx = ctx;
            this.seq = seq;
        }
    }

    private volatile Predicate<String> translated = t -> false;

    public void knowsTranslations(Predicate<String> lookup) {
        translated = lookup == null ? t -> false : lookup;
    }

    private volatile Predicate<String> known = t -> false;

    public void knowsSources(Predicate<String> lookup) {
        known = lookup == null ? t -> false : lookup;
    }

    private volatile Predicate<String> longer = t -> false;

    public void knowsLonger(Predicate<String> lookup) {
        longer = lookup == null ? t -> false : lookup;
    }

    private final Map<String, Captured> untranslated = new ConcurrentHashMap<>();

    int untranslatedSeen(String template) {
        Captured c = untranslated.get(hash(template));
        return c == null ? 0 : c.seen;
    }

    public boolean record(String template, String role, String domain, String ctx) {
        return record(template, role, domain, ctx, null);
    }

    public boolean record(String template, String role, String domain, String ctx,
                          String full) {
        return record(template, role, domain, ctx, full, false, null);
    }

    public boolean recordIn(String template, String role, String domain, String ctx, String in) {
        return record(template, role, domain, ctx, null, false, in);
    }

    public boolean isTranslated(String template) {
        return template != null && translated.test(template);
    }

    private volatile Supplier<String> where = () -> null;

    public void knowsWhere(Supplier<String> where) {
        this.where = where == null ? () -> null : where;
    }

    private String whereNow() {
        try {
            String w = where.get();
            return w == null || w.isBlank() ? null : w;
        } catch (Throwable t) {
            ErrorDebug.note("CaptureStore#whereNow", null, t);
            return null;
        }
    }

    public void recordChat(String template, String ctx, long now) {
        recordChatRows(chatRows.offer(template, ctx, now));
    }

    public void settleChat(long now) {
        recordChatRows(chatRows.settle(now));
    }

    private void recordChatRows(List<ChatRows.Row> rows) {
        for (ChatRows.Row row : rows) {
            record(row.template(), "desc", "chat", row.ctx(), null, true, null);
        }
    }

    private final ChatRows chatRows = new ChatRows(
            t -> translated.test(t) || known.test(t),
            t -> longer.test(t));

    private boolean record(String template, String role, String domain, String ctx,
                           String full, boolean wholeLine, String in) {
        if (template == null || template.isBlank() || !GlyphSplitter.hasLetter(template)) {
            return false;
        }
        if (OwnOutputs.isOwn(template)) {
            noteEvent("skipped.ownOutput");
            return false;
        }
        if (translated.test(template)) {
            noteEvent("skipped.translated");
            return false;
        }
        if (ThirdPartyLiterals.reserved(template)) {
            noteEvent("skipped.thirdParty");
            return false;
        }
        if (known.test(template)) {
            untranslated.compute(hash(template), (k, c) -> {
                if (c == null) {
                    return new Captured(template.strip(), role, classify(domain, ctx), ctx, 0);
                }
                c.seen++;
                return c;
            });
            noteEvent("counted.untranslated");
            return false;
        }
        if (!wholeLine && longer.test(template)) {
            noteEvent("skipped.fragment");
            return false;
        }
        String key = hash(template);
        Captured existing = entries.get(key);
        if (existing != null) {
            existing.seen++;
            if (full != null && (existing.full == null
                    || (full.length() > existing.full.length()
                            && full.startsWith(existing.full)))) {
                existing.full = full;
                dirty.set(true);
            }
            return false;
        }
        Captured row = new Captured(template.strip(), role, classify(domain, ctx),
                ctx, nextSeq.getAndIncrement());
        row.full = full;
        row.in = in;
        row.place = whereNow();
        entries.put(key, row);
        dirty.set(true);
        return true;
    }

    private static String classify(String domain, String ctx) {
        if (ctx != null && ctx.startsWith("dialogue")
                && questOf(ctx) == null && speakerOf(ctx) == null) {
            return UNKNOWN;
        }
        return domain;
    }

    private boolean nameFrame(String src) {
        return src.contains(GlyphSplitter.PLAYER_PLACEHOLDER) && known.test(src);
    }

    private static boolean nowFiltered(Captured c) {
        if (PlayerDataFilter.carriesPlayerData(c.src)) {
            return true;
        }
        String ctx = c.ctx == null ? "" : c.ctx;
        if (ctx.startsWith("npc/nametag") || ctx.startsWith("label/floating")) {
            return PlayerDataFilter.looksPlayerNamed(c.src);
        }
        if (ctx.startsWith("gui/title")) {
            return PlayerDataFilter.looksAccountNamed(c.src);
        }
        return false;
    }

    public void noteEvent(String name) {
        eventCounts.computeIfAbsent(name,
                k -> new AtomicInteger()).incrementAndGet();
        dirty.set(true);
    }

    public boolean contains(String template) {
        return entries.containsKey(hash(template));
    }

    synchronized JsonObject rowsJson(Predicate<Captured> keep) {
        JsonObject rows = new JsonObject();
        List<Captured> ordered = entries.values().stream().filter(keep)
                .sorted(Comparator.comparingInt(c -> c.seq)).toList();
        Map<String, Integer> nth = new HashMap<>();
        Set<String> used = new HashSet<>();
        for (Captured c : ordered) {
            String key = readableKey(c, nth);
            String unique = key;
            for (int n = 2; !used.add(unique); n++) {
                unique = key + "~" + n;
            }
            JsonObject row = GSON.toJsonTree(c).getAsJsonObject();
            String quest = questOf(c.ctx);
            if (quest != null) {
                row.addProperty("quest", quest);
                row.addProperty("line", nth.get(quest));
            }
            String who = speakerOf(c.ctx);
            if (who != null) {
                row.addProperty("speaker", who);
            }
            rows.add(unique, row);
        }
        return rows;
    }

    public int size() {
        return entries.size();
    }

    private boolean prune() {
        int before = entries.size();
        entries.values().removeIf(c -> c.dst != null && c.dst.isBlank()
                && c.src != null && (translated.test(c.src) || nowFiltered(c)));
        int gone = before - entries.size();
        if (gone > 0) {
            eventCounts.computeIfAbsent("skipped.translated",
                    k -> new AtomicInteger()).addAndGet(gone);
        }
        boolean moved = false;
        for (var it = entries.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Captured> e = it.next();
            Captured c = e.getValue();
            if (c.src != null && (c.dst == null || c.dst.isBlank()) && known.test(c.src)) {
                it.remove();
                untranslated.merge(e.getKey(), c, (a, b) -> {
                    a.seen += b.seen;
                    return a;
                });
                moved = true;
            }
        }
        int pending = untranslated.size();
        untranslated.values().removeIf(c -> c.src == null
                || translated.test(c.src) || (nowFiltered(c) && !nameFrame(c.src)));
        return gone > 0 || moved || untranslated.size() != pending;
    }

    public synchronized void flush() {
        if (prune()) {
            dirty.set(true);
        }
        if (ColourAudit.takeDirty()) {
            dirty.set(true);
        }
        if (OverflowAudit.takeDirty()) {
            dirty.set(true);
        }
        if (UnappliedAudit.takeDirty()) {
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
                + "seq 是收集順序，任務對話照它排就是原本的先後。"
                + " 任務對話的鍵就是「任務名 #第幾句」，另外 quest／speaker／line "
                + "三欄把同一件事拆開寫，不用自己去解 ctx。"
                + " 認不出來歸屬的（含別的模組的字）集中在 ? 開頭的鍵，"
                + "domain 是 unknown——那是<b>還沒分類</b>，不是一種分類。"
                + " untranslated 是語料收過、但這個語言還沒翻的句子，seen 是看到幾次，"
                + "照多到少排——最常出現的最該先補。這一段不會匯出。"
                + " colours 是<翻出來了、但原文有個顏色沒回到譯文上>的句子："
                + "runs 是原文的顏色分段（#RRGGBB，b 粗體、u 底線），missed 是掉了的"
                + "那幾個顏色。照 runs 的先後數，第一個出現的顏色是 {c1}、第二個是 {c2}，"
                + "在譯文裡寫 {c2}…{/} 就能指定。"
                + " overflow 是<翻出來了、但譯文比原本那一格大>的句子：where 是介面"
                + "（dialogue 對話框、tooltip 物品說明），room 是原本容得下多少、"
                + "need 是譯文要多少，unit 是單位（rows 列、px 像素）。"
                + "對話那一種在畫面上會改由小框顯示。"
                + " unapplied 是<語料有譯文、卻套不上去>的句子，畫面上會留原文："
                + "why 寫的是哪一種佔位符差了幾個（src 原文幾個、dst 譯文幾個）。"
                + "{#} 與 {p} 要跟原文一樣多，{~} 也是（或改用 {~1} {~2} 指名），"
                + "{u} 可以少不能多。這一段每次開遊戲重新記。"
                + " 物品說明與選單：ctx 是 gui/block 的是畫面上折成好幾行的一整段，"
                + "行跟行之間照畫面換行；收進語料時接成一句（用空白接）。"
                + "gui/line 是單獨一行，或條列裡的一項。in 是那份說明的標題（哪一件物品、哪一個選單）。"
                + " place 是收到的時候人在哪裡：討伐戰寫「Raid · 名稱 · 關卡」，"
                + "Lootrun 寫「Lootrun · 地區」，其他寫地區名。同一關的漂浮字照 seq 排就是出現的順序。");
        meta.addProperty("colours", ColourAudit.size());
        meta.addProperty("overflow", OverflowAudit.size());
        meta.addProperty("unapplied", UnappliedAudit.size());
        meta.addProperty("untranslated", untranslated.size());
        JsonObject events = new JsonObject();
        eventCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> events.addProperty(e.getKey(), e.getValue().get()));
        meta.add("events", events);
        JsonArray selves = new JsonArray();
        for (String where : SelfNames.describe()) {
            selves.add(where);
        }
        meta.add("selfNames", selves);
        JsonObject groups = new JsonObject();
        Map<String, Integer> tally = new TreeMap<>();
        for (Captured c : entries.values()) {
            tally.merge(groupOf(c), 1, Integer::sum);
        }
        tally.forEach(groups::addProperty);
        meta.add("groups", groups);
        root.add("_meta", meta);
        root.add("entries", rowsJson(c -> true));
        if (!untranslated.isEmpty()) {
            JsonArray pending = new JsonArray();
            untranslated.values().stream()
                    .sorted(Comparator.comparingInt((Captured c) -> c.seen).reversed())
                    .forEach(c -> {
                        JsonObject row = new JsonObject();
                        row.addProperty("src", c.src);
                        row.addProperty("seen", c.seen);
                        row.addProperty("ctx", c.ctx == null ? "" : c.ctx);
                        String quest = questOf(c.ctx);
                        if (quest != null) {
                            row.addProperty("quest", quest);
                        }
                        String who = speakerOf(c.ctx);
                        if (who != null) {
                            row.addProperty("speaker", who);
                        }
                        pending.add(row);
                    });
            root.add("untranslated", pending);
        }
        if (ColourAudit.size() > 0) {
            root.add("colours", ColourAudit.toJson());
        }
        if (OverflowAudit.size() > 0) {
            root.add("overflow", OverflowAudit.toJson());
        }
        if (UnappliedAudit.size() > 0) {
            root.add("unapplied", UnappliedAudit.toJson());
        }

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            SafeFiles.moveAtomically(tmp, file);
        } catch (IOException e) {
            dirty.set(true);
            ModLog.warn("寫入失敗 " + file + ": " + e.getMessage());
        }
    }

    static final long MAX_BYTES = 32L << 20;

    private void load() {
        JsonObject root = SafeFiles.readObject(file, MAX_BYTES);
        if (root == null) {
            return;
        }
        ColourAudit.load(root.get("colours"));
        OverflowAudit.load(root.get("overflow"));
        JsonElement pending = root.get("untranslated");
        if (pending != null && pending.isJsonArray()) {
            for (var row : pending.getAsJsonArray()) {
                Captured c = row(row);
                if (c != null) {
                    untranslated.put(hash(c.src), c);
                }
            }
        }
        JsonElement saved = root.get("entries");
        if (saved == null || !saved.isJsonObject()) {
            return;
        }
        for (var e : saved.getAsJsonObject().entrySet()) {
            Captured c = row(e.getValue());
            if (c == null) {
                continue;
            }
            entries.put(hash(c.src), c);
            int seq = Math.max(0, c.seq);
            nextSeq.updateAndGet(n -> Math.max(n, seq + 1));
        }
    }

    private static Captured row(JsonElement el) {
        if (el == null || !el.isJsonObject()) {
            return null;
        }
        try {
            Captured c = GSON.fromJson(el, Captured.class);
            if (c == null || c.src == null || c.src.isBlank()) {
                return null;
            }
            if (c.dst == null) {
                c.dst = "";
            }
            c.ctx = cleanCtx(c.ctx);
            return c;
        } catch (RuntimeException e) {
            ErrorDebug.note("CaptureStore#row", null, e);
            return null;
        }
    }

    private static String readableKey(Captured c, Map<String, Integer> nth) {
        String quest = questOf(c.ctx);
        if (quest != null) {
            int n = nth.merge(quest, 1, Integer::sum);
            return String.format("%s #%03d", quest, n);
        }
        return UNKNOWN.equals(c.domain)
                ? String.format("? #%04d", c.seq)
                : String.format("%04d", c.seq);
    }

    public static final String UNKNOWN = "unknown";

    private static String groupOf(Captured c) {
        String quest = questOf(c.ctx);
        if (quest != null) {
            return "quest: " + quest;
        }
        return c.domain == null || c.domain.isBlank() ? UNKNOWN : c.domain;
    }

    private static String speakerOf(String ctx) {
        if (ctx == null || !ctx.startsWith("dialogue")) {
            return null;
        }
        int at = ctx.indexOf('#');
        if (at < 0) {
            return null;
        }
        return CurrentQuest.speakerName(ctx.substring(at + 1));
    }

    static String cleanCtx(String ctx) {
        if (ctx == null || !ctx.startsWith("dialogue")) {
            return ctx;
        }
        int at = ctx.indexOf('#');
        if (at < 0) {
            return ctx;
        }
        String who = CurrentQuest.speakerName(ctx.substring(at + 1));
        String head = ctx.substring(0, at);
        return who == null ? head : head + "#" + who;
    }

    private static String questOf(String ctx) {
        if (ctx == null || !ctx.startsWith("dialogue/")) {
            return null;
        }
        String rest = ctx.substring("dialogue/".length());
        int at = rest.indexOf('#');
        String quest = (at < 0 ? rest : rest.substring(0, at)).strip();
        return quest.isEmpty() ? null : quest;
    }

    static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.strip().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }
}
