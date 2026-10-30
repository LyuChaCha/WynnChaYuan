package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;

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
        store.knowsWhere(() -> "Raid · Nest of the Grootslangs · Slime Gathering");
        store.recordIn("Stand here to\ndefend the elder", "desc", "gui", "gui/block", "Elder's Request");
        store.knowsWhere(() -> { throw new IllegalStateException("還沒進世界"); });
        store.record("Teleporter", "desc", "label", "label/floating");
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
        JsonObject block = rows.entrySet().stream()
                .filter(e -> e.getValue().getAsJsonObject().get("src").getAsString().startsWith("Stand here"))
                .map(e -> e.getValue().getAsJsonObject()).findFirst().orElse(null);
        check("★ 介面字帶著是哪一份 tooltip 的（in）",
                block != null && "Elder's Request".equals(block.get("in").getAsString()));
        check("★ 帶著收到的時候在哪裡（place）",
                block != null && block.get("place").getAsString().startsWith("Raid · Nest"));
        JsonObject tele = rows.entrySet().stream()
                .filter(e -> "Teleporter".equals(e.getValue().getAsJsonObject().get("src").getAsString()))
                .map(e -> e.getValue().getAsJsonObject()).findFirst().orElse(null);
        check("問地點出錯：照收，只是沒有 place", tele != null && !tele.has("place"));
        check("沒有的欄位不寫出來（對話沒有 in）", !first.has("in"));
        check("第二句的 line 是 2",
                rows.getAsJsonObject("King's Recruit #002")
                        .get("line").getAsInt() == 2);

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

        JsonObject groups = root.getAsJsonObject("_meta")
                .getAsJsonObject("groups");
        check("★ _meta.groups 一眼看得出這份檔案裡有什麼",
                groups.has("quest: King's Recruit")
                        && groups.get("quest: King's Recruit").getAsInt() == 2);
        check("不明的那一堆也列在 groups 裡",
                groups.has(CaptureStore.UNKNOWN));

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
