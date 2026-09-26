package com.wynnchayuan.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynnchayuan.SafeFiles;
import com.wynnchayuan.capture.LineParts;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 記住每一句對話裡<b>哪幾段是強調色</b>，存成檔案。
 *
 * <h2>為什麼非記不可</h2>
 * Wynncraft 的顏色是跟著打字長出來的：打到 {@code [A} 才送 {@code [A} 的顏色。
 * 而中文比英文短——「不知道那雙靴子跑到哪去了……你聽說過 <b>[</b>」這個方括號，
 * 在中文裡出現在整句的 57%，在英文裡卻要等到 83%。中間那段時間我們手上
 * <b>根本沒有顏色可以貼</b>，只能先畫成底色，等英文打到了才變。
 * 玩家看到的就是「白一下再變色」。
 *
 * <p>所以把看過的顏色記下來。同一句再讀一次，第一幀就有顏色。
 *
 * <h2>為什麼要落地成檔案</h2>
 * 先前只記在記憶體裡，重開遊戲就空了——而玩家正是一邊重開一邊測，
 * 等於完全沒有效果。寫進 {@code config/wynnchayuan/dialogue-colours.json}
 * 之後，一句話這輩子只會白那一次。
 *
 * <p>只記<b>顏色</b>，不記譯文：譯文在語料裡，這裡存的是畫面上量到的東西。
 * 檔案掉了、壞了都只是退回「第一次會白一下」，不影響任何翻譯。
 */
public final class DialogueTint {

    /** 檔名。跟 captured.json、cards.json 放在一起。 */
    public static final String FILE = "dialogue-colours.json";

    /**
     * 記幾句。
     *
     * <p>一次遊玩會經過的台詞是幾百句的量級，而每一條只有幾十個位元組。
     * 上限存在的意義是「檔案不會無限長大」，不是省記憶體。
     */
    private static final int KEEP = 2048;

    /** 最久沒用到的先丟。鍵是整句譯文。 */
    private static final Map<String, List<LineParts.Piece>> SEEN =
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, List<LineParts.Piece>> eldest) {
                    return size() > KEEP;
                }
            };

    private static final AtomicBoolean dirty = new AtomicBoolean(false);

    private static volatile Path file;

    private DialogueTint() {}

    /** 讀進來。檔案不在、讀不懂都只是從空的開始。 */
    public static void init(Path target) {
        file = target;
        JsonObject root = SafeFiles.readObject(target, 4L * 1024 * 1024);
        if (root == null || !root.has("lines")
                || !root.get("lines").isJsonObject()) {
            return;
        }
        synchronized (SEEN) {
            for (Map.Entry<String, JsonElement> each
                    : root.getAsJsonObject("lines").entrySet()) {
                List<LineParts.Piece> pieces = read(each.getValue());
                if (!pieces.isEmpty()) {
                    SEEN.put(each.getKey(), pieces);
                }
            }
        }
    }

    /** 測試用：不落地，只清空。 */
    static void forTest() {
        file = null;
        synchronized (SEEN) {
            SEEN.clear();
        }
        dirty.set(false);
    }

    /**
     * 這一句上次看到的強調色。
     *
     * <p>打字中拿到的是<b>前綴</b>，記下來的是整句，所以不能只比相等。
     *
     * @return 找不到時回傳空的
     */
    public static List<LineParts.Piece> of(String line) {
        if (line == null || line.isEmpty()) {
            return List.of();
        }
        synchronized (SEEN) {
            List<LineParts.Piece> exact = SEEN.get(line);
            if (exact != null) {
                return exact;
            }
            for (Map.Entry<String, List<LineParts.Piece>> each : SEEN.entrySet()) {
                if (each.getKey().startsWith(line)) {
                    return each.getValue();
                }
            }
        }
        return List.of();
    }

    /**
     * 記住這一句的強調色。
     *
     * <p>同一句會在打字途中被記很多次，一次比一次完整。後記的蓋掉先記的——
     * 我們要的就是<b>最完整</b>的那一版。
     */
    public static void learn(String line, List<LineParts.Piece> pieces) {
        if (file == null || line == null || line.isEmpty() || pieces.isEmpty()) {
            return;
        }
        synchronized (SEEN) {
            List<LineParts.Piece> had = SEEN.get(line);
            if (had != null && had.equals(pieces)) {
                return;                    // 一模一樣，不必再寫一次檔
            }
            SEEN.put(line, List.copyOf(pieces));
        }
        dirty.set(true);
    }

    /** 寫檔。跟 captured.json 同一個排程呼叫，見 {@code WynnChaYuan#flushQuietly}。 */
    public static void flush() {
        Path target = file;
        if (target == null || !dirty.getAndSet(false)) {
            return;
        }
        JsonObject lines = new JsonObject();
        synchronized (SEEN) {
            for (Map.Entry<String, List<LineParts.Piece>> each : SEEN.entrySet()) {
                lines.add(each.getKey(), write(each.getValue()));
            }
        }
        JsonObject root = new JsonObject();
        root.addProperty("_note", "對話裡哪幾段是強調色。純粹是畫面上量到的快取，"
                + "刪掉只會讓每一句第一次讀的時候，物品名先白一下再變色。");
        root.add("lines", lines);
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .disableHtmlEscaping().create();
        try {
            SafeFiles.writeAtomically(target, gson.toJson(root));
        } catch (Exception e) {
            dirty.set(true);               // 沒寫成功，下次再試
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------

    private static List<LineParts.Piece> read(JsonElement value) {
        List<LineParts.Piece> out = new ArrayList<>();
        if (!value.isJsonArray()) {
            return out;
        }
        for (JsonElement each : value.getAsJsonArray()) {
            if (!each.isJsonObject()) {
                continue;
            }
            JsonObject piece = each.getAsJsonObject();
            JsonElement text = piece.get("text");
            JsonElement colour = piece.get("colour");
            if (text == null || !text.isJsonPrimitive()) {
                continue;
            }
            TextColor tone = colour == null || !colour.isJsonPrimitive()
                    ? null
                    : TextColor.parseColor(colour.getAsString()).result().orElse(null);
            out.add(new LineParts.Piece(text.getAsString(),
                    Style.EMPTY.withColor(tone)));
        }
        return out;
    }

    private static JsonArray write(List<LineParts.Piece> pieces) {
        JsonArray out = new JsonArray();
        for (LineParts.Piece piece : pieces) {
            JsonObject each = new JsonObject();
            each.addProperty("text", piece.text());
            TextColor tone = piece.style().getColor();
            if (tone != null) {
                each.addProperty("colour", tone.serialize());
            }
            out.add(each);
        }
        return out;
    }
}
