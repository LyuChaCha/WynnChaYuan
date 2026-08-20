package com.wynnchayuan.translate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 驗證譯文表怎麼認「跨行條目」與「物品名稱」。
 *
 * <p>這兩件事都曾經<b>無聲失效</b>——譯文明明載入了、數量也對，遊戲裡就是不生效，
 * 從畫面上完全看不出原因。所以它們值得各有一條測試盯著。
 */
public final class TranslationStoreTest {

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("wynnchayuan-store");
        try {
            write(dir, "ability.json", """
                {
                 "_meta": { "domain": "ability", "itemNames": false },
                 "entries": {
                  "a1": { "src": "Drastically increase the\nspeed of your Meteor ability.",
                          "dst": "大幅增加 Meteor 技能的\n速度。", "role": "desc" },
                  "a2": { "src": "Shooting Star", "dst": "流星", "role": "name" }
                 }
                }""");
            write(dir, "gear-weapon.json", """
                {
                 "_meta": { "domain": "gear-weapon" },
                 "entries": {
                  "g1": { "src": "Idol", "dst": "偶像", "role": "name" }
                 }
                }""");

            TranslationStore store = new TranslationStore();
            store.loadAll(dir);

            // 跨行條目：技能說明的原文本來就是一整段，逐行查表永遠對不上。
            // 呼叫端要靠 maxBlockLines 才知道該把幾行併起來試。
            check("認得出最長的跨行條目有兩行", store.maxBlockLines() == 2);
            check("整段查得到",
                    "大幅增加 Meteor 技能的\n速度。".equals(store.lookup(
                            "Drastically increase the\nspeed of your Meteor ability.")));

            // 「翻譯物品名稱」只該管裝備，不該連技能名稱一起關掉——
            // 技能名稱跟 wiki、交易市場毫無關係。
            store.setTranslateNames(false);
            check("關掉物品名稱後，裝備名稱不翻", store.lookup("Idol") == null);
            check("關掉物品名稱後，技能名稱照翻", "流星".equals(store.lookup("Shooting Star")));

            store.setTranslateNames(true);
            check("開啟後裝備名稱恢復", "偶像".equals(store.lookup("Idol")));
        } finally {
            delete(dir);
        }

        System.out.println(failures == 0
                ? "TranslationStore: 全部通過" : "TranslationStore: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void write(Path dir, String name, String body) throws IOException {
        Files.writeString(dir.resolve(name), body, StandardCharsets.UTF_8);
    }

    private static void delete(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            for (Path p : files.toList()) {
                Files.deleteIfExists(p);
            }
        }
        Files.deleteIfExists(dir);
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
