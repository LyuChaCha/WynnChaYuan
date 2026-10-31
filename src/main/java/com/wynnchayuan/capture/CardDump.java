package com.wynnchayuan.capture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynnchayuan.ModConfig;
import com.wynnchayuan.ModLog;
import com.wynnchayuan.SafeFiles;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public final class CardDump {

    public static final String FILE = "cards.json";

    private static final long MAX_BYTES = 64L << 20;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static volatile Path file;

    private static volatile BooleanSupplier enabled = CardDump::configSaysYes;

    private static final Map<String, Card> cards = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> blocked = new ConcurrentHashMap<>();
    private static final AtomicInteger nextSeq = new AtomicInteger();
    private static final AtomicBoolean dirty = new AtomicBoolean();

    private static volatile List<Component> lastTooltip = List.of();

    private static final Set<String> thisHover = ConcurrentHashMap.newKeySet();

    private CardDump() {}

    public static final class Card {
        public String src;
        public String dst = "";
        public String card = "";
        public List<String> lines = List.of();
        public String role = "desc";
        public String domain = "gui";
        public String ctx = "card/block";
        public int seen = 1;
        public int seq;
    }

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

    public static void forTest(Path target) {
        init(target);
        enabled = () -> true;
    }

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
        ModConfig config = WynnChaYuan.config();
        return config != null && config.collect() && config.collectGuiText();
    }

    private static volatile boolean playerHead;

    public static void fromPlayerHead(boolean head) {
        playerHead = head;
    }

    public static String paragraphKey(List<StyledText> styled, int at) {
        if (styled == null || at <= 0 || at >= styled.size()) {
            return null;
        }
        int end = at;
        while (end < styled.size()
                && !styled.get(end).getStringWithoutFormatting().isBlank()) {
            end++;
        }
        if (end - at < 2) {
            return null;
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

    private static boolean partlyPerLine(String key, TranslationStore store) {
        if (store == null) {
            return false;
        }
        boolean any = false;
        boolean all = true;
        for (String line : key.split("\\R", -1)) {
            String one = line.strip();
            if (one.isEmpty()) {
                continue;
            }
            String zh = store.lookup(one);
            if (zh != null && !zh.isBlank()) {
                any = true;
            } else {
                all = false;
            }
        }
        return any && !all;
    }

    public static void note(List<Component> tooltip, List<StyledText> styled, String key,
                            TranslationStore store) {
        try {
            if (file == null || key == null || key.isBlank()
                    || tooltip == null || tooltip.isEmpty()
                    || styled == null || styled.isEmpty()
                    || !enabled.getAsBoolean()) {
                return;
            }
            if (partlyPerLine(key, store)) {
                tally("skipped.perLine");
                return;
            }
            if (!tooltip.equals(lastTooltip)) {
                lastTooltip = List.copyOf(tooltip);
                thisHover.clear();
            }
            if (!thisHover.add(key)) {
                return;
            }
            record(styled, key);
        } catch (Throwable t) {
            ErrorDebug.note("CardDump#note", null, t);
        }
    }

    private static void record(List<StyledText> styled, String key) {
        String src = TranslationStore.normalise(key);
        if (src.isBlank() || !GlyphSplitter.hasLetter(src)) {
            tally("skipped.noLetter");
            return;
        }
        List<String> lines = List.of(key.split("\\R", -1));

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
        if (playerHead) {
            tally("blocked.playerHead");
            return;
        }
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

    private static boolean suspect(String template) {
        return template != null
                && (PlayerDataFilter.carriesPlayerData(template)
                    || PlayerDataFilter.looksAccountNamed(template));
    }

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

    public static int size() {
        return cards.size();
    }

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
            SafeFiles.moveAtomically(tmp, target);
        } catch (Throwable t) {
            dirty.set(true);
            ModLog.warn("寫入失敗 " + target + ": " + t);
        }
    }

    private static JsonObject rowsJson() {
        JsonObject rows = new JsonObject();
        List<Card> ordered = cards.values().stream()
                .sorted(Comparator.comparingInt(c -> c.seq)).toList();
        Set<String> used = new HashSet<>();
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

    private static void load() {
        Path target = file;
        if (target == null) {
            return;
        }
        JsonObject root = SafeFiles.readObject(target, MAX_BYTES);
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
                    continue;
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
                ErrorDebug.note("CardDump#load", null, t);
            }
        }
        nextSeq.set(top);
    }

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
