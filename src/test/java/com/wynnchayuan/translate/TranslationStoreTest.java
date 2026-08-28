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
                 "Short one": "短句",
                 "{u}, why don't you do the honors? Go ahead and open that gate.":
                     "{u}，不如由你來吧？去把那道門打開。"
                }""");

            // 子目錄要寫進 _index.json 才讀得到——Files.list 不遞迴。
            write(dir, "_index.json", """
                {"files": ["quest/king-s-recruit.json", "quest/queen-s-recruit.json"]}""");

            // 任務檔：每一條都標了它屬於哪個任務。
            // 兩個任務都有一句 "Hey, {u}!" 開頭，全庫看就是撞句；
            // 分開看則各自唯一。這正是實機上卡住翻譯的形狀。
            write(dir, "quest/king-s-recruit.json", """
                {
                 "_meta": {"quest": "King's Recruit"},
                 "entries": {
                  "King's Recruit#002": {
                   "src": "Sound off, {u}! Are you hurt? The cart hit a boulder.",
                   "dst": "喂，{u}！你受傷了嗎？馬車撞到石頭了。",
                   "quest": "King's Recruit",
                   "source": "wiki"
                  },
                  "King's Recruit#003": {
                   "src": "Hey, {u}! You alright in there? Looks like we hit something.",
                   "dst": "嘿，{u}！你在裡面還好嗎？我們好像撞到什麼東西了。",
                   "quest": "King's Recruit",
                   "source": "wiki"
                  }
                 }
                }""");
            write(dir, "quest/queen-s-recruit.json", """
                {
                 "_meta": {"quest": "Queen's Recruit"},
                 "entries": {
                  "Queen's Recruit#002": {
                   "src": "Sound off, {u}! Are you ready? The ship leaves at dawn.",
                   "dst": "喂，{u}！準備好了嗎？船天一亮就開。",
                   "quest": "Queen's Recruit"
                  }
                 }
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

            // 語料是美式拼法（honors），遊戲裡是英式（honours）。差一個字母，
            // 語料裡八百多條句子在畫面上一條都對不上。
            check("英式拼法查得到美式的語料",
                    "{u}，不如由你來吧？去把那道門打開。".equals(store.lookup(
                            "{u}, why don't you do the honours? "
                                    + "Go ahead and open that gate.")));
            check("打到一半也認得英式拼法",
                    store.matchPrefix("{u}, why don't you do the honours?") != null);
            // 反面：拼法換過去還是查不到的，不要硬掰
            check("換了拼法還是查不到就算了",
                    store.lookup("What colour is the honour?") == null);

            // 卡住翻譯的不是長度門檻，是<b>撞句</b>。
            //
            // 實機（dialogue-probe）：「Hey, Green_teaTW」打到 16 個字還是英文，
            // 一直到「Hey, Green_teaTW! Are you al」28 個字才徽然跳成中文。
            // 長度門檻早就過了，是因為好幾句都以它開頭、分不出是哪一句。
            String half = "Sound off, {u}! Are you";
            check("兩個任務都有這個開頭，全庫查分不出來",
                    store.matchPrefix(half, 40) == null);
            check("知道是哪個任務就分得出來",
                    "Sound off, {u}! Are you hurt? The cart hit a boulder."
                            .equals(store.matchPrefix(half, 40, "King's Recruit")));
            check("換一個任務就是另一句",
                    "Sound off, {u}! Are you ready? The ship leaves at dawn."
                            .equals(store.matchPrefix(half, 40, "Queen's Recruit")));
            // 範圍窄到一個任務之後，連長度門檻都不必等——
            // 這就是「一開始就出來」的意思。
            check("任務內連幾個字就夠",
                    "Sound off, {u}! Are you hurt? The cart hit a boulder."
                            .equals(store.matchPrefix("Sound off, {u}! Are you h", 5,
                                                      "King's Recruit")));
            // 但任務內也不能亂猜：這個開頭那個任務根本沒有
            check("任務內沒有的開頭不會硬提一句",
                    store.matchPrefix("Completely unrelated opening line", 40,
                                      "King's Recruit") == null);
            // 不知道任務時行為跟以前一模一樣
            check("不知道任務時還是走舊的那一條",
                    store.matchPrefix(half, 40, null) == null);
            check("任務名字對不上就當作不知道",
                    store.matchPrefix(half, 40, "No Such Quest") == null);

            // wiki 抄來的台詞會跟遊戲裡實際跑的字不一樣（官方改過詞、wiki 還沒跟上）。
            // 兩個版本都在庫裡時，逐字打字會先貼 wiki 版、多打幾個字再換成校訂版——
            // 實機錄到的就是「嘿，」講到一半自己變成「喂，」。見 #curatedRival。
            check("同一句有校訂版時，一開始就用校訂版",
                    "Hey, {u}! Are you alright in there? It looks like we've hit something."
                            .equals(store.matchPrefix("Hey, {u}", 8, "King's Recruit")));
            check("多打幾個字之後還是同一條，不會換",
                    "Hey, {u}! Are you alright in there? It looks like we've hit something."
                            .equals(store.matchPrefix("Hey, {u}! Are you al", 20,
                                                      "King's Recruit")));
            // 反面：沒有校訂版時，wiki 那條照樣要用——這條路不能把任務索引整個廢掉
            check("沒有校訂版時 wiki 那條照用",
                    "Sound off, {u}! Are you hurt? The cart hit a boulder."
                            .equals(store.matchPrefix("Sound off, {u}! Are you h", 5,
                                                      "King's Recruit")));

            // 「同一句話」是看用字重疊，不是看共同前綴——上面那兩條在
            // 「Hey, {u}! 」之後就立刻岔開了。
            check("改過幾個詞算同一句",
                    TranslationStore.sameLine(
                            "Hey, {u}! You alright in there? Looks like we hit something.",
                            "Hey, {u}! Are you alright in there? "
                                    + "It looks like we've hit something."));
            check("只是開頭一樣不算同一句",
                    !TranslationStore.sameLine(
                            "Sound off, {u}! Are you hurt? The cart hit a boulder.",
                            "Sound off, {u}! Are you ready? The ship leaves at dawn."));
            check("空句子不算", !TranslationStore.sameLine("", "anything at all"));

            // 打到一半的那半句本身剛好也是語料裡的另一條時，不能馬上定案——
            // 後面還有更長的候選，就代表現在貼上去等一下要改口。
            check("後面還有更長的就要說有", store.hasLonger("Hey, {u}"));
            check("整段一模一樣但沒有更長的就說沒有",
                    !store.hasLonger("Hey, {u}! Are you alright in there? "
                            + "It looks like we've hit something."));
            check("完全不相干的開頭也是沒有", store.hasLonger("Zzzz nothing here") == false);
            check("短句沒有更長的候選時不受影響", !store.hasLonger("Short one"));
            realCorpus();
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
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());   // quest/ 這種子目錄
        Files.writeString(file, body, StandardCharsets.UTF_8);
    }

    private static void delete(Path dir) throws IOException {
        // 得遞迴——quest/ 子目錄裡還有檔，先前直接 deleteIfExists
        // 會丟 DirectoryNotEmptyException。
        try (var files = Files.list(dir)) {
            for (Path p : files.toList()) {
                if (Files.isDirectory(p)) {
                    delete(p);
                } else {
                    Files.deleteIfExists(p);
                }
            }
        }
        Files.deleteIfExists(dir);
    }

    /**
     * 拿<b>真正出貨的語料</b>再驗一次同一件事。
     *
     * <h2>為什麼要多這一段</h2>
     * 上面那些用的是臨時造的小語料，證明的是程式邏輯。但這個 bug 的成因有一半
     * 在<b>資料</b>裡：wiki 抄來的舊版與人工校訂的新版同時存在，而校訂版沒有
     * quest 欄位、看不見於任務索引。臨時語料再怎麼造也不會發現真語料哪天
     * 又多出第三個版本——那時候 {@code settle} 會退回 {@code null}，
     * 畫面上又變回「講到一半才跳成中文」，而所有單元測試依然全綠。
     */
    private static void realCorpus() {
        Path corpus = Path.of("src/main/resources/assets/wynnchayuan/translations",
                              Languages.DEFAULT);
        if (!Files.isDirectory(corpus)) {
            return;                            // 不在原始碼樹裡跑就跳過
        }
        TranslationStore real = new TranslationStore();
        real.loadAll(corpus);
        String live = "Hey, {u}! Are you alright in there? "
                + "It looks like we've hit something.";
        String first = real.matchPrefix("Hey, {u}", 16, "King's Recruit");
        check("實際語料：打到「Hey, {u}」就認得出是哪一句（拿到 " + first + "）",
                live.equals(first));
        String later = real.matchPrefix("Hey, {u}! Are you al", 28, "King's Recruit");
        check("實際語料：多打幾個字還是同一條（拿到 " + later + "）",
                live.equals(later));

        // 實際語料裡這幾條是逐字模擬抓出來的：打到一半剛好命中別的檔案裡的短詞。
        check("實際語料：打到「Block」時後面還有更長的候選",
                real.hasLonger("Block"));
        check("實際語料：打到「Bring」時後面還有更長的候選",
                real.hasLonger("Bring"));
        check("實際語料：打到「...」時後面還有更長的候選", real.hasLonger("..."));
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
