package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CaptureStoreTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        newOnesAreSkipped();
        staleOnesArePruned();
        humanWorkSurvives();
        untranslatedAreCounted();
        fragmentsAreSkipped();
        report();
    }

    private static void fragmentsAreSkipped() throws Exception {
        Path file = Files.createTempDirectory("wynnchayuan-frag").resolve("captured.json");
        CaptureStore store = new CaptureStore(file);
        String whole = "Go on, I don't have time for you. My brother keeps sending"
                + " me people all the time.";
        store.knowsLonger(t -> whole.startsWith(t) && !whole.equals(t));

        check("半截的不收",
              !store.record("Go on, I don't have time for you. My brother",
                            "desc", "quest", "dialogue/x"));
        check("整句照收", store.record(whole, "desc", "quest", "dialogue/x"));
        check("不相干的句子照收",
              store.record("Brand new line", "desc", "quest", "dialogue/x"));
        check("缺口清單只有那兩條（實際 " + store.size() + " 條）", store.size() == 2);
    }

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

        CaptureStore old = new CaptureStore(file);
        old.record("Caravan Driver", "name", "label", "label/floating");
        old.record("Halt, recruits!", "desc", "quest", "dialogue/x");
        old.flush();

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
