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
                  "a2": { "src": "Shooting Star", "dst": "流星", "role": "name" },
                  "a4": { "src": "Bash", "dst": "重擊", "role": "name" },
                  "a5": { "src": "ÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀ Knockback Immunity to Allies", "dst": "ÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀ 給友軍擊退免疫", "role": "desc" },
                  "a3": { "src": "Allies within {~} blocks gain {~} of the health you gain from Health Regen and Life Steal.",
                          "dst": "{~} 格內的友軍會獲得你從生命回復與生命竊取所得的 {~} 生命", "role": "desc" }
                 }
                }""");
            write(dir, "gear-weapon.json", """
                {
                 "_meta": { "domain": "gear-weapon" },
                 "entries": {
                  "g1": { "src": "Idol", "dst": "偶像", "role": "name" }
                 }
                }""");

            // 台詞語料是<b>扁平格式</b>（原文當鍵），跟上面的 workspace 格式不同。
            write(dir, "quest.json", """
                {
                 "Hey, {u}! Are you alright in there? It looks like we've hit something.":
                     "喂，{u}！你在裡面還好嗎？我們好像撞到什麼東西了。",
                 "Short one": "短句"
                }""");

            TranslationStore store = new TranslationStore();
            store.loadAll(dir);

            // 跨行條目：技能說明的原文本來就是一整段，逐行查表永遠對不上。
            // 呼叫端要靠 maxBlockLines 才知道該把幾行併起來試。
            check("認得出最長的跨行條目有兩行", store.maxBlockLines() >= 2);
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

            // 長敘述在遊戲裡是依畫面寬度自動斷行的，斷點取決於玩家的設定；
            // 語料存的是完整一句。差別只在空白，所以正規化之後就對得上。
            String wrapped = "Allies within {~} blocks gain {~} of the\n"
                    + "health you gain from Health Regen and Life Steal.";
            check("斷行位置不同也查得到",
                    store.lookupFlat(wrapped) != null);
            check("正規化不會誤中短詞", store.lookupFlat("Idol") == null);

            // 這種條目在語料裡是「一行」，maxBlockLines 對它一無所知——
            // 呼叫端要靠它決定把幾行併起來試，回報 1 的話就永遠不會試。
            check("有攤平索引時，掃描行數要放大", store.maxBlockLines() >= 4);

            // 技能說明裡到處都在提別的技能。翻一次，所有敘述跟著換——
            // 這是翻譯團隊要的「Bash 改一次就好」，但用名稱不用座標代號。
            TranslationStore.Term term =
                    store.findTerm("Bash will hit a second time at a farther range.", 0);
            check("句子裡的技能名稱找得到", term != null && "重擊".equals(term.translation()));
            check("找到的位置正確", term != null && term.start() == 0 && term.end() == 4);

            check("整段剛好是名稱時也查得到", "重擊".equals(store.lookupTerm("Bash")));

            // 反面：小寫的一般字不能被當成技能名稱換掉
            check("小寫的同一個字不換", store.findTerm("you bash the ground", 0) == null);
            // 反面：名稱是別的字的一部分時不算
            check("夾在字中間的不算", store.findTerm("Bashful grin", 0) == null);

            // 縮排幾格是<b>排版</b>：取決於畫面寬度與同一組細項裡最長的那一行。
            // 語料（CDN）存的是 40 格，遊戲實際送出來的是 4 格或 42 格，字面上永遠
            // 不相等——`Knockback Immunity to Allies` 就是這樣一直維持英文的。
            String wide = String.valueOf('À').repeat(40);
            check("縮排格數不同也查得到",
                    store.lookup(wide + " Knockback Immunity to Allies") != null);
            String narrow = String.valueOf('À').repeat(4);
            String indented = store.lookup(narrow + " Knockback Immunity to Allies");
            check("查到的譯文（實際：" + indented + "）",
                    (narrow + " 給友軍擊退免疫").equals(indented));
            check("接回去的是呼叫端自己的縮排，不是語料裡的那一串",
                    indented != null && !indented.startsWith(wide));
            // 反面：完全沒有縮排的句子不該靠這條路徑中
            check("沒縮排的不會誤中",
                    store.lookup("Knockback Immunity to Allies") == null);

            // 逐字打字：畫面上只打到一半時，靠前綴找出這是哪一句。
            //
            // 這條路徑曾經對<b>整個台詞語料</b>無聲失效——扁平格式的檔案沒有寫進
            // 前綴索引，於是整句打完才查得到，中途一律落空。畫面上看到的是
            // 「英文一字一字跑完，才忽然整句跳成中文」，而譯文數量完全正常，
            // 從載入紀錄上一點都看不出來。
            check("打到一半就找得到是哪一句",
                    "Hey, {u}! Are you alright in there? It looks like we've hit something."
                            .equals(store.matchPrefix("Hey, {u}! Are you alright in there? It l")));
            check("找到之後查得到譯文",
                    store.lookup(store.matchPrefix(
                            "Hey, {u}! Are you alright in there? It l")) != null);
            // 反面：太短的前綴分不出是哪一句，寧可不換也不要換錯
            check("前綴太短就不猜", store.matchPrefix("Hey, {u}!") == null);
            // 但門檻要看畫面上<b>實際打出來</b>幾個字：玩家名被 {u} 收掉之後
            // 模板短一大截，拿模板長度當門檻，開頭那一小段會先閃出英文。
            check("模板短但實際打了夠多字就查得到",
                    "Hey, {u}! Are you alright in there? It looks like we've hit something."
                            .equals(store.matchPrefix("Hey, {u}!", "Hey, Green_teaTW!".length())));
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
