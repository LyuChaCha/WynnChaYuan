package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ChatBlock {

    private ChatBlock() {}

    private static final long IDLE_MS = 200;

    private static final int MAX_ROWS = 24;

    private record Row(StyledText original, Component translated, boolean keep) {}

    private static final List<Row> pending = new ArrayList<>();
    private static long last;

    public static synchronized void queue(StyledText original, Component translated) {
        if (pending.size() >= MAX_ROWS) {
            flush();
        }
        pending.add(new Row(original, translated, false));
        last = System.currentTimeMillis();
    }

    private static boolean held = false;

    public static synchronized void hold(StyledText original, boolean keep) {
        long now = System.currentTimeMillis();
        if (pending.size() >= MAX_ROWS || ready(now)) {
            flush();
        }
        if (!held || pending.isEmpty()) {
            heldSince = now;
        }
        pending.add(new Row(original, null, keep));
        held = true;
        last = now;
    }

    private static final long HOLD_MAX_MS = 600;

    private static long heldSince;

    static synchronized void heldAt(long when) {
        heldSince = when;
    }

    public static synchronized boolean holding() {
        return held && !pending.isEmpty();
    }

    public static synchronized void tick() {
        if (ready(System.currentTimeMillis())) {
            flush();
        }
    }

    public static synchronized void flushPending() {
        if (!pending.isEmpty()) {
            flush();
        }
    }

    static synchronized boolean ready(long now) {
        if (pending.isEmpty()) {
            return false;
        }
        return now - last >= IDLE_MS || (held && now - heldSince >= HOLD_MAX_MS);
    }

    static synchronized int size() {
        return pending.size();
    }

    static synchronized void arrivedAt(long when) {
        last = when;
    }

    public static synchronized void clear() {
        pending.clear();
        held = false;
        mine.clear();
        sentAt.clear();
    }

    private static final java.util.Set<String> mine =
            java.util.Collections.newSetFromMap(
                    new java.util.LinkedHashMap<>() {
                        @Override
                        protected boolean removeEldestEntry(
                                java.util.Map.Entry<String, Boolean> eldest) {
                            return size() > MINE_MEMORY;
                        }
                    });

    private static final int MINE_MEMORY = 32;

    // displayClientMessage 會同步觸發 Wynntils 的 Match 與 Edit
    private static boolean sending = false;

    private static Component sendingWhole;

    // 用戶端訊息會被 Wynntils「提及」把自己名字染黃
    public static synchronized Component sendingNow() {
        return sending ? sendingWhole : null;
    }

    private static final long MINE_TTL_MS = 5_000;

    private static final java.util.Map<String, Long> sentAt = new java.util.HashMap<>();

    public static synchronized boolean isOurs(StyledText message) {
        if (message == null) {
            return false;
        }
        return sending || isOurs(message.getStringWithoutFormatting());
    }

    static synchronized boolean isOurs(String plain) {
        if (plain == null) {
            return false;
        }
        String key = bare(plain);
        if (!mine.contains(key)) {
            return false;
        }
        Long when = sentAt.get(key);
        return when != null && System.currentTimeMillis() - when <= MINE_TTL_MS;
    }

    private static String bare(String text) {
        return text.indexOf('\u00a7') < 0 ? text : STYLE_CODE.matcher(text).replaceAll("");
    }

    private static final java.util.regex.Pattern STYLE_CODE =
            java.util.regex.Pattern.compile("\u00a7(?:#[0-9a-fA-F]{8}|\\{[^}]*}|\\[[^\\]]*]|.)");

    static synchronized void remember(Component sent) {
        String key = bare(sent.getString());
        mine.add(key);
        sentAt.put(key, System.currentTimeMillis());
        sentAt.keySet().retainAll(mine);
    }

    private static void flush() {
        List<Row> rows = new ArrayList<>(pending);
        boolean wasHeld = held;
        pending.clear();
        held = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        Component whole = compose(rows, wasHeld, WynnChaYuan.translations());
        if (whole != null) {
            remember(whole);
            sending = true;
            sendingWhole = whole;
            try {
                mc.player.displayClientMessage(whole, false);
            } finally {
                sending = false;
                sendingWhole = null;
            }
        }
    }

    static Component compose(List<Row> rows, boolean wasHeld,
                             com.wynnchayuan.translate.TranslationStore store) {
        Component whole = rows.size() > 1 ? asBlock(rows, store) : null;
        if (whole == null) {
            whole = stacked(rows, wasHeld, store);
        }
        return whole;
    }

    static synchronized Component preview(com.wynnchayuan.translate.TranslationStore store) {
        return compose(new ArrayList<>(pending), held, store);
    }

    private static Component asBlock(List<Row> rows,
                                     com.wynnchayuan.translate.TranslationStore store) {
        try {
            net.minecraft.network.chat.MutableComponent joined = Component.empty();
            for (int i = 0; i < rows.size(); i++) {
                if (i > 0) {
                    joined.append(Component.literal("\n"));
                }
                joined.append(rows.get(i).original().getComponent());
            }
            return LineTranslator.translateChat(StyledText.fromComponent(joined),
                                                store);
        } catch (Throwable t) {
            ErrorDebug.note("ChatBlock#asBlock", null, t);
            return null;
        }
    }

    private static Component stacked(List<Row> rows, boolean wasHeld,
                                     com.wynnchayuan.translate.TranslationStore store) {
        boolean[] centred = null;
        boolean panel = false;
        if (rows.size() > 1) {
            List<StyledText> originals = new ArrayList<>(rows.size());
            for (Row row : rows) {
                originals.add(row.original());
            }
            try {
                centred = LineTranslator.chatCentred(originals);
                panel = LineTranslator.chatPanel(originals);
            } catch (Throwable t) {
                ErrorDebug.note("ChatBlock#stacked", null, t);
                centred = null;
                panel = false;
            }
        }
        Component[] flowed = null;
        if (panel) {
            try {
                List<StyledText> originals = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    originals.add(row.original());
                }
                flowed = LineTranslator.flowPanel(originals, store, centred);
            } catch (Throwable t) {
                ErrorDebug.note("ChatBlock#stacked", null, t);
                flowed = null;
            }
        } else if (rows.size() > 1) {
            try {
                List<StyledText> originals = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    originals.add(row.original());
                }
                flowed = LineTranslator.flowColumn(originals, store, centred);
            } catch (Throwable t) {
                ErrorDebug.note("ChatBlock#stacked", null, t);
                flowed = null;
            }
        }
        Component[] lines = new Component[rows.size()];
        boolean[] hit = new boolean[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            Component line = centred == null ? rows.get(i).translated() : null;
            boolean keep = rows.get(i).keep();
            if (!keep && flowed != null && flowed[i] != null) {
                line = flowed[i];
            }
            if (line == null && !keep) {
                try {
                    line = LineTranslator.translateChat(rows.get(i).original(),
                                                        store,
                                                        centred == null ? null : centred[i],
                                                        panel);
                } catch (Throwable t) {
                    ErrorDebug.note("ChatBlock#stacked", null, t);
                    line = null;
                }
            }
            if (line == null) {
                line = rows.get(i).translated();
            }
            if (line == null && wasHeld && !keep) {
                try {
                    line = LineTranslator.translate(rows.get(i).original(), store);
                } catch (Throwable t) {
                    ErrorDebug.note("ChatBlock#stacked", null, t);
                    line = null;
                }
            }
            if (line == null) {
                line = rows.get(i).original().getComponent();
            } else {
                hit[i] = true;
            }
            lines[i] = line;
        }
        boolean[] carry = wasHeld ? null : carried(multiLine(rows), hit);
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        boolean any = false;
        boolean translated = false;
        for (int i = 0; i < rows.size(); i++) {
            if (carry != null && !carry[i]) {
                continue;
            }
            translated |= hit[i];
            if (any) {
                out.append(Component.literal("\n"));
            }
            out.append(lines[i]);
            any = true;
        }
        return (wasHeld ? any : worthSending(any, translated)) ? out : null;
    }

    private static boolean[] multiLine(List<Row> rows) {
        boolean[] out = new boolean[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            String text = rows.get(i).original().getStringWithoutFormatting();
            out[i] = text != null && text.strip().indexOf('\n') >= 0;
        }
        return out;
    }

    static boolean[] carried(boolean[] multiLine, boolean[] hit) {
        boolean[] out = new boolean[hit.length];
        int i = 0;
        while (i < hit.length) {
            int end = i + 1;
            if (!multiLine[i]) {
                while (end < hit.length && !multiLine[end]) {
                    end++;
                }
            }
            boolean any = false;
            for (int k = i; k < end; k++) {
                any |= hit[k];
            }
            for (int k = i; k < end; k++) {
                out[k] = any;
            }
            i = end;
        }
        return out;
    }

    static boolean worthSending(boolean any, boolean translated) {
        return any && translated;
    }
}
