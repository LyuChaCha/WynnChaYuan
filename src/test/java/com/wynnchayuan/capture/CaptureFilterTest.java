package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class CaptureFilterTest {

    public static void main(String[] args) throws Exception {
        templateRules();
        sceneRules();
        gluedDigits();
        throughTheStore();
        staleEntries();
        shippedCorpus();

        System.out.println(failures == 0 ? "收集雜訊：全部通過" : "收集雜訊：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void templateRules() {
        System.out.println("=== 只看字串的規則 ===");
        noisy("黑幕字", "Á", "title");
        noisy("黑幕字前後有圖示", "{#}Á{#}", "title");
        noisy("領地 boss bar", "{p} {#}{#}", "label/bossbar");
        noisy("公會標籤開頭的公告", "{#} [ABCD] was victorious!", "chat/INFO");
        noisy("公會標籤開頭的看板", "[XYZ] is hosting a\nraid tonight", "gui/block");
        noisy("公會的歡迎標題", "Welcome to Some Folks Guild :{~}", "title");
        noisy("傳送立牌", "Teleporter\nto my room", "label/floating");
        noisy("收到東西的通知", "← someone{~} {~}x Liquid Emerald (Everyone)", "chat/notification");
        noisy("寄東西的通知", "Alice → Bob: an Aspect", "chat/notification");

        quiet("一般的字", "Welcome to Wynncraft!", "title");
        quiet("名牌上的方向字", "← Move Left", "label/floating");
        quiet("地名 boss bar 只在 boss bar 擋", "{p} {#}{#}", "label/floating");
        quiet("驚嘆號不是公會標籤", "[!] Tower Aura", "title");
        quiet("傳送點單獨一列照收", "Teleporter", "label/floating");
        quiet("名字裡的 Á", "Ábrete", "title");
        quiet("賣東西的通知", "Sold {~} item for {~}²", "chat/notification");
    }

    private static void sceneRules() {
        System.out.println("=== 要看人在哪裡的規則 ===");
        check("★ 在住宅島上的漂浮字不收",
              CaptureNoise.sceneReason("good tree", "label/floating", true, () -> null) != null);
        check("★ 在住宅島上的標題不收",
              CaptureNoise.sceneReason("Make Sure Upvote", "title", true, () -> null) != null);
        check("住宅島上的物品說明照收",
              !CaptureNoise.dependsOnScene("gui/line"));
        check("★ 城裡的「名字 {#}{#}」是寵物",
              CaptureNoise.sceneReason("Fluffy {#}{#}", "label/floating", false, () -> "Detlas") != null);
        check("城外的「名字 {#}{#}」是怪物，照收",
              CaptureNoise.sceneReason("Fluffy {#}{#}", "label/floating", false,
                      () -> "Lootrun · Sky Falls") == null);
        check("不知道在哪裡的照收",
              CaptureNoise.sceneReason("Fluffy {#}{#}", "label/floating", false, () -> null) == null);
        check("城裡兩列的名牌照收（不是寵物的樣子）",
              CaptureNoise.sceneReason("Fluffy {#}{#}\n{#}\n{#} {~}s", "label/floating", false,
                      () -> "Detlas") == null);
        check("城裡的 NPC 名牌照收",
              CaptureNoise.sceneReason("Fluffy {#}{#}", "npc/nametag", false, () -> "Detlas") == null);
    }

    private static void gluedDigits() {
        System.out.println("=== 名字黏著數字 ===");
        for (String plate : List.of("Fluffy{~} {#}{#}", "zoey{~}'s Well of Power")) {
            check("★ 像玩家取的名字：" + plate, PlayerDataFilter.looksPlayerNamed(plate));
        }
        for (String plate : List.of("Steam Mech-DM{~} {#}{#}", "Armored Tank A{~}-L{~} {#}{#}",
                "F{~}t{~}-Mech {#}{#}", "Creeper King {~} {#}{#}", "Rusted Sledge-Bot {~}k {#}{#}")) {
            check("遊戲自己的怪：" + plate, !PlayerDataFilter.looksPlayerNamed(plate));
        }
    }

    private static void throughTheStore() throws Exception {
        System.out.println("=== 經過收集端 ===");
        Path file = Files.createTempDirectory("wynnchayuan-noise").resolve("captured.json");
        CaptureStore store = new CaptureStore(file);
        boolean[] housing = {false};
        String[] place = {"Detlas"};
        store.knowsHousing(() -> housing[0]);
        store.knowsWhere(() -> place[0]);

        check("★ 黑幕字不進 captured", !store.record("Á", "desc", "title", "title"));
        check("★ 城裡的寵物不進 captured",
              !store.record("Fluffy {#}{#}", "name", "label", "label/floating"));
        place[0] = "Nivla Woods";
        check("城外同一個名字照收", store.record("Fluffy {#}{#}", "name", "label", "label/floating"));
        housing[0] = true;
        check("★ 住宅島上的漂浮字不進 captured",
              !store.record("good tree", "name", "label", "label/floating"));
        check("住宅島上的物品說明照收", store.record("A fine old tree", "desc", "gui", "gui/line"));
        check("只收了兩條（實際 " + store.size() + "）", store.size() == 2);
        store.flush();
        JsonObject events = JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                .getAsJsonObject("_meta").getAsJsonObject("events");
        check("擋下來的有記在 events", events.has("skipped.blackout") && events.has("skipped.pet")
                && events.has("skipped.housing"));
    }

    private static void staleEntries() throws Exception {
        System.out.println("=== 舊檔裡已經收進來的 ===");
        Path file = Files.createTempDirectory("wynnchayuan-noise-old").resolve("captured.json");
        JsonObject entries = new JsonObject();
        entries.add("0001", row("Á", "title", null));
        entries.add("0002", row("Teleporter\nto my room", "label/floating", null));
        entries.add("0003", row("Fluffy {#}{#}", "label/floating", "Thesead"));
        entries.add("0004", row("Patch {#}{#}", "label/floating", null));
        entries.add("0005", row("Alice → Bob: an Aspect", "chat/notification", null));
        entries.add("0006", row("Bunny{~} {#}{#}", "label/floating", null));
        JsonObject root = new JsonObject();
        root.add("entries", entries);
        root.add("untranslated", JsonParser.parseString(
                "[{\"src\":\"{p} {#}{#}\",\"seen\":9,\"ctx\":\"label/bossbar\"}]"));
        Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        CaptureStore store = new CaptureStore(file);
        store.flush();
        List<String> left = new ArrayList<>();
        for (String src : List.of("Á", "Teleporter\nto my room", "Alice → Bob: an Aspect",
                "Bunny{~} {#}{#}")) {
            if (store.contains(src)) {
                left.add(src);
            }
        }
        check("★ 舊的雜訊清掉了（還在的：" + left + "）", left.isEmpty());
        check("不知道在哪裡收到的名牌留著", store.contains("Patch {#}{#}"));
        JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        check("★ 領地 boss bar 從沒翻清單清掉了", !saved.has("untranslated")
                || saved.getAsJsonArray("untranslated").isEmpty());
    }

    private static void shippedCorpus() throws Exception {
        System.out.println("=== 出貨語料不會被誤擋 ===");
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations/zh_tw");
        List<String> hit = new ArrayList<>();
        for (String name : List.of("npc.json", "label.json", "misc.json", "gui.json", "raid.json")) {
            JsonObject o = JsonParser.parseString(Files.readString(base.resolve(name)))
                    .getAsJsonObject();
            for (String key : o.keySet()) {
                for (String ctx : List.of("title", "label/floating", "chat/INFO", "gui/block")) {
                    if (CaptureNoise.reason(key, ctx) != null) {
                        hit.add(name + ": " + key);
                    }
                }
                if ((name.equals("npc.json") || name.equals("label.json"))
                        && PlayerDataFilter.looksPlayerNamed(key)
                        && !PlayerDataFilter.looksPlayerNamed(key.replaceAll("[a-z]{2}\\{~}", "ab"))) {
                    hit.add(name + "（黏著數字）: " + key);
                }
            }
        }
        check("★ 語料裡沒有一條被新規則擋下（被擋的：" + hit + "）", hit.isEmpty());
    }

    private static void noisy(String what, String template, String ctx) {
        check("★ 不收：" + what, CaptureNoise.reason(template, ctx) != null);
    }

    private static void quiet(String what, String template, String ctx) {
        check("照收：" + what, CaptureNoise.reason(template, ctx) == null);
    }

    private static JsonObject row(String src, String ctx, String place) {
        JsonObject o = new JsonObject();
        o.addProperty("src", src);
        o.addProperty("dst", "");
        o.addProperty("role", "name");
        o.addProperty("domain", "label");
        o.addProperty("ctx", ctx);
        o.addProperty("seen", 2);
        if (place != null) {
            o.addProperty("place", place);
        }
        return o;
    }
}
