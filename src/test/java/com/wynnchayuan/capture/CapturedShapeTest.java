package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code captured.json} 的分類看得懂嗎。
 *
 * <h2>這份檔案是給人讀的</h2>
 * 譯者拿到的是幾百句斷了脈絡的台詞。要翻得對，他得知道三件事：
 * <b>哪個任務、誰講的、第幾句</b>。這三件事收集的當下都知道，
 * 所以應該各自一欄寫出來——要他自己去解
 * {@code "dialogue/King's Recruit#Aledar"} 這種機器字串不合理。
 *
 * <h2>認不出來就說認不出來</h2>
 * 既沒有任務名也沒有說話者的對話，硬歸在 {@code quest} 底下是在騙人：
 * 看起來有分類，實際上沒有。塞錯的分類比沒有分類更難發現。
 */
public final class CapturedShapeTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-captured");
        Path file = dir.resolve("captured.json");
        CaptureStore store = new CaptureStore(file);

        store.record("Good luck in there, recruits!", "desc", "quest",
                "dialogue/King's Recruit#Aledar");
        store.record("You made it. Well done.", "desc", "quest",
                "dialogue/King's Recruit#Aledar");
        store.record("Fine weather today.", "desc", "quest", "dialogue");
        store.record("Available Points", "desc", "gui", "gui/line");
        store.flush();

        JsonObject root = JsonParser.parseString(
                Files.readString(file)).getAsJsonObject();
        JsonObject rows = root.getAsJsonObject("entries");

        check("任務對話的鍵就是「任務名 #第幾句」",
                rows.has("King's Recruit #001") && rows.has("King's Recruit #002"));

        JsonObject first = rows.getAsJsonObject("King's Recruit #001");
        check("★ 有 quest 欄（不用自己解 ctx）",
                first.has("quest") && "King's Recruit".equals(
                        first.get("quest").getAsString()));
        check("★ 有 speaker 欄（誰講的）",
                first.has("speaker") && "Aledar".equals(
                        first.get("speaker").getAsString()));
        check("★ 有 line 欄（第幾句）",
                first.has("line") && first.get("line").getAsInt() == 1);
        check("第二句的 line 是 2",
                rows.getAsJsonObject("King's Recruit #002")
                        .get("line").getAsInt() == 2);

        // ---- 認不出來的那一堆 ----
        String orphan = rows.keySet().stream()
                .filter(k -> k.startsWith("?")).findFirst().orElse(null);
        check("★ 沒有任務也沒有說話者的對話，鍵用 ? 開頭", orphan != null);
        if (orphan != null) {
            JsonObject one = rows.getAsJsonObject(orphan);
            check("★ 它的 domain 是 unknown，不是假裝成 quest",
                    CaptureStore.UNKNOWN.equals(one.get("domain").getAsString()));
            check("它沒有 quest 欄——沒有就是沒有，不要編一個",
                    !one.has("quest"));
        }

        JsonObject gui = null;
        for (String key : rows.keySet()) {
            JsonObject one = rows.getAsJsonObject(key);
            if ("Available Points".equals(one.get("src").getAsString())) {
                gui = one;
            }
        }
        check("介面文字照舊是 gui，沒有被當成不明",
                gui != null && "gui".equals(gui.get("domain").getAsString()));

        // ---- 群組統計 ----
        JsonObject groups = root.getAsJsonObject("_meta")
                .getAsJsonObject("groups");
        check("★ _meta.groups 一眼看得出這份檔案裡有什麼",
                groups.has("quest: King's Recruit")
                        && groups.get("quest: King's Recruit").getAsInt() == 2);
        check("不明的那一堆也列在 groups 裡",
                groups.has(CaptureStore.UNKNOWN));

        // ---- 第三方模組的字串不收 ----
        com.wynnchayuan.render.ThirdPartyLiterals.forTest(
                java.util.List.of("Raid Completed"));
        CaptureStore other = new CaptureStore(dir.resolve("second.json"));
        check("★ 別的模組拿來判斷狀態的句子不收（翻掉會害它算錯）",
                !other.record("Raid Completed", "name", "gui", "gui/title"));
        check("別的句子照收",
                other.record("Raid Failed differently", "name", "gui", "gui/title"));

        report();
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "CapturedShape: 全部通過"
                : "CapturedShape: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
