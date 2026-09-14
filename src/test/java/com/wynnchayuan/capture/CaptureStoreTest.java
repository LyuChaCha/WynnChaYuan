package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code captured.json} 只該列<b>還沒有譯文</b>的字串。
 *
 * <h2>先前壞在哪</h2>
 * 檔頭自己寫著「dst 留空代表尚未翻譯」，但收集端照單全收：看到什麼記什麼，
 * 語料裡明明已經有譯文的也照記，{@code dst} 一樣留空。
 *
 * <p>實機回報的那一份 308 條裡有 <b>249 條</b>語料早就翻好了——
 * 「露營車駕駛」「建立角色」「- 職業: 薩滿」全在裡面。八成是雜訊，
 * 真正的缺口反而找不到。使用者說「那個檔案不能當待辦清單」，就是這件事。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>已經有譯文的不收，而且要記在 events 裡（數字留著才看得出
 *       「明明有譯文卻還是英文」）</li>
 *   <li>上一版收進來的舊條目，flush 的時候要清掉</li>
 *   <li><b>有人打過字的一律不動</b>——那是別人的工，不管語料裡有沒有</li>
 * </ol>
 */
public final class CaptureStoreTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        newOnesAreSkipped();
        staleOnesArePruned();
        humanWorkSurvives();
        untranslatedAreCounted();
        report();
    }

    /**
     * 語料收過、只是還沒翻的：不當缺口，但要記次數。
     *
     * <p>先前這種句子一律略過、連次數都沒留，翻譯的人不知道哪幾句最常被看到。
     */
    private static void untranslatedAreCounted() throws Exception {
        Path file = Files.createTempDirectory("wynnchayuan-cap").resolve("captured.json");
        CaptureStore store = new CaptureStore(file);
        store.knowsTranslations(t -> t.equals("Caravan Driver"));
        store.knowsSources(t -> t.equals("Caravan Driver") || t.equals("Halt, recruits!"));

        String ctx = "dialogue/King's Recruit#Guard";
        check("語料收過的不當成缺口", !store.record("Halt, recruits!", "desc", "quest", ctx));
        store.record("Halt, recruits!", "desc", "quest", ctx);
        check("有譯文的照樣略過", !store.record("Caravan Driver", "name", "label", "label/floating"));
        check("語料沒收過的照收", store.record("Brand new line", "desc", "quest", "dialogue/x"));
        check("缺口清單只有真正的新句子（實際 " + store.size() + " 條）", store.size() == 1);
        check("沒翻的那句記了兩次", store.untranslatedSeen("Halt, recruits!") == 2);
        check("有譯文的不會跑進沒翻清單", store.untranslatedSeen("Caravan Driver") == 0);

        store.flush();
        JsonObject root = read(file);
        check("檔案裡有 untranslated 一段",
                root.has("untranslated") && root.getAsJsonArray("untranslated").size() == 1);
        JsonObject row = root.getAsJsonArray("untranslated").get(0).getAsJsonObject();
        check("那一段記了次數與任務", row.get("seen").getAsInt() == 2
                && "King's Recruit".equals(row.get("quest").getAsString()));

        CaptureStore again = new CaptureStore(file);
        check("重開遊戲次數還在", again.untranslatedSeen("Halt, recruits!") == 2);

        // 舊版當成缺口收進來、後來語料收了（還沒翻）的，要搬到沒翻清單。
        Path oldFile = Files.createTempDirectory("wynnchayuan-cap").resolve("captured.json");
        CaptureStore old = new CaptureStore(oldFile);
        old.record("Halt, recruits!", "desc", "quest", "dialogue/x");
        old.flush();
        CaptureStore moved = new CaptureStore(oldFile);
        moved.knowsSources(t -> t.equals("Halt, recruits!"));
        moved.flush();
        check("搬走之後缺口清單是空的（實際 " + moved.size() + " 條）", moved.size() == 0);
        check("搬到沒翻清單", moved.untranslatedSeen("Halt, recruits!") == 1);
    }

    private static void newOnesAreSkipped() throws Exception {
        Path file = Files.createTempDirectory("wynnchayuan-cap").resolve("captured.json");
        CaptureStore store = new CaptureStore(file);
        store.knowsTranslations(t -> t.equals("Caravan Driver"));

        check("沒譯文的照收", store.record("Halt, recruits!", "desc", "quest", "dialogue/x"));
        check("有譯文的不收", !store.record("Caravan Driver", "name", "label", "label/floating"));
        check("只剩沒譯文的那一條（實際 " + store.size() + " 條）", store.size() == 1);

        store.flush();
        JsonObject root = read(file);
        JsonObject events = root.getAsJsonObject("_meta").getAsJsonObject("events");
        check("略過的次數有記下來",
              events.has("skipped.translated")
              && events.get("skipped.translated").getAsInt() == 1);
    }

    private static void staleOnesArePruned() throws Exception {
        Path file = Files.createTempDirectory("wynnchayuan-cap").resolve("captured.json");

        // 先照舊版的樣子收一輪：那時候還不知道哪些已經有譯文，兩條都收了。
        CaptureStore old = new CaptureStore(file);
        old.record("Caravan Driver", "name", "label", "label/floating");
        old.record("Halt, recruits!", "desc", "quest", "dialogue/x");
        old.flush();

        // 換成接上譯文的版本再開一次。
        CaptureStore store = new CaptureStore(file);
        check("兩條都讀進來了（實際 " + store.size() + " 條）", store.size() == 2);
        store.knowsTranslations(t -> t.equals("Caravan Driver"));
        store.flush();

        check("已經翻好的那條被清掉（實際 " + store.size() + " 條）", store.size() == 1);
        check("還沒翻的那條留著", store.contains("Halt, recruits!"));
        check("已經翻好的那條不在了", !store.contains("Caravan Driver"));

        JsonObject root = read(file);
        check("檔案裡也只剩一條", root.getAsJsonObject("entries").size() == 1);
    }

    private static void humanWorkSurvives() throws Exception {
        Path file = Files.createTempDirectory("wynnchayuan-cap").resolve("captured.json");
        CaptureStore old = new CaptureStore(file);
        old.record("Caravan Driver", "name", "label", "label/floating");
        old.flush();

        // 有人在這個檔裡打了譯文。就算語料裡也有一份，也不能替他丟掉。
        JsonObject root = read(file);
        JsonObject saved = root.getAsJsonObject("entries");
        String key = saved.keySet().iterator().next();
        saved.getAsJsonObject(key).addProperty("dst", "露營車駕駛");
        write(file, root.toString());

        CaptureStore store = new CaptureStore(file);
        store.knowsTranslations(t -> true);
        store.flush();
        check("有人打過字的不會被清掉", store.contains("Caravan Driver"));
    }

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static JsonObject read(Path file) throws Exception {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("收集清單：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("收集清單：全部通過");
    }
}
