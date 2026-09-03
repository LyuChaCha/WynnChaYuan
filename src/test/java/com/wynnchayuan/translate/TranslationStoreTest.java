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
                  "a6": { "src": "Guardian", "dst": "守護者", "role": "name" },
                  "a7": { "src": "Guardian Angels", "dst": "守護天使", "role": "name" },
                  "a8": { "src": "Mask", "dst": "假面", "role": "name" },
                  "a9": { "src": "Mask of the Lunatic", "dst": "赤狂假面", "role": "name" },
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

            // 素材：是道具、但<b>不是裝備</b>。名稱該照翻，也不該被拿去替換別的句子。
            write(dir, "ingredient.json", """
                {
                 "_meta": { "domain": "ingredient", "itemNames": true, "gearNames": false },
                 "entries": {
                  "i1": { "src": "Dark Matter", "dst": "暗物質", "role": "name" }
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

            // 採集點的名牌與裝備需求列，每一行前面掛一個「符合／不符合」的記號。
            // 記號取決於玩家當下的等級與工具，是狀態不是內容——語料只存一種，
            // 另一種靠 lookupMarked 換記號查回來。
            write(dir, "gather.json", """
                {
                 "Blossom\n✖ Ⓒ Woodcutting Lv Min: {~}\n✖ Equipped Tool: Axe":
                     "花木\n✖ Ⓒ 伐木等級下限: {~}\n✖ 裝備工具: 斧頭",
                 "✖ Ability Points: {~}": "✖ 能力點數: {~}"
                }""");

            TranslationStore store = new TranslationStore();
            store.loadAll(dir);

            // 記號換一下就該查得到，而且回來的譯文要帶<b>呼叫端自己</b>那幾個記號。
            check("等級夠了的採集點也翻得出來（拿到：" + store.lookup(
                            "Blossom\n✔ Ⓒ Woodcutting Lv Min: {~}\n✖ Equipped Tool: Axe") + "）",
                    ("花木\n✔ Ⓒ 伐木等級下限: {~}\n✖ 裝備工具: 斧頭").equals(store.lookup(
                            "Blossom\n✔ Ⓒ Woodcutting Lv Min: {~}\n✖ Equipped Tool: Axe")));
            check("裝備需求列的記號也換得回來",
                    "✔ 能力點數: {~}".equals(store.lookup("✔ Ability Points: {~}")));
            check("原本那一種照舊",
                    "✖ 能力點數: {~}".equals(store.lookup("✖ Ability Points: {~}")));
            // 收集端也要知道這不是缺口，否則 captured.json 會一直把它列成沒翻。
            check("換記號查得到的不算缺口",
                    store.hasTranslation("✔ Ability Points: {~}"));
            check("沒有記號的句子不受影響", "短句".equals(store.lookup("Short one")));

            // 跨行條目：技能說明的原文本來就是一整段，逐行查表永遠對不上。
            // 呼叫端要靠 maxBlockLines 才知道該把幾行併起來試。
            check("認得出最長的跨行條目有兩行", store.maxBlockLines() >= 2);
            check("整段查得到",
                    "大幅增加 Meteor 技能的\n速度。".equals(store.lookup(
                            "Drastically increase the\nspeed of your Meteor ability.")));

            // 「翻譯物品名稱」只該管裝備，不該連技能名稱或素材一起關掉——
            // 技能名稱跟 wiki、交易市場毫無關係，素材也是。
            store.setTranslateNames(false);
            check("關掉物品名稱後，裝備名稱不翻", store.lookup("Idol") == null);
            check("關掉物品名稱後，技能名稱照翻", "流星".equals(store.lookup("Shooting Star")));
            check("關掉物品名稱後，素材名稱照翻",
                    "暗物質".equals(store.lookup("Dark Matter")));

            store.setTranslateNames(true);
            check("開啟後裝備名稱恢復", "偶像".equals(store.lookup("Idol")));

            // 素材名稱同時要滿足<b>兩件事</b>，這正是把旗標拆成兩個的理由：
            // 不受開關管（上面那條），而且不會被拿去替換別的句子（這條）。
            // 共用一個旗標時，怎麼填都只顧得到一半。
            check("素材名稱不會被當成可替換的詞塞進別的句子",
                    store.findTerm("You found a Dark Matter today.", 0) == null);

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

            // 詞表裡有不少名稱是另一個名稱的開頭。先前比中就回，而字數是從少
            // 往多試的——短的永遠贏，畫面上是「守護者Angels」「假面 of the
            // Lunatic」。語料裡兩邊都翻好了，玩家看到的卻是半個英文名字。
            TranslationStore.Term longer =
                    store.findTerm("Guardian Angels 增加 +2 發彈藥。", 0);
            check("同一個起點取最長的名稱",
                    longer != null && "守護天使".equals(longer.translation()));
            check("最長的那個範圍也要對",
                    longer != null && longer.start() == 0 && longer.end() == 15);

            // 三個字以上的也一樣，而且中間夾著小寫的 of / the
            TranslationStore.Term mask =
                    store.findTerm("穿戴 Mask of the Lunatic 時", 0);
            check("跨多個字的名稱不被開頭那個蓋掉",
                    mask != null && "赤狂假面".equals(mask.translation()));

            // 反面：長的接不上去時，短的還是要照常換
            TranslationStore.Term shortOne =
                    store.findTerm("Guardian 的護盾", 0);
            check("接不到長的就用短的",
                    shortOne != null && "守護者".equals(shortOne.translation()));

            // 反面：小寫的一般字不能被當成技能名稱換掉
            check("小寫的同一個字不換", store.findTerm("you bash the ground", 0) == null);
            // 反面：名稱是別的字的一部分時不算
            check("夾在字中間的不算", store.findTerm("Bashful grin", 0) == null);

            // 中文不用空格分詞，緊貼著寫是正常的中文。Java 認為漢字是字母，
            // 於是「降低Bash的魔力消耗」裡的 Bash 兩邊都被當成同一個詞的一部分，
            // 詞表整個不觸發——語料裡有 77 條長這樣，差別只在有沒有留空格。
            TranslationStore.Term glued =
                    store.findTerm("降低Bash的魔力消耗。", 0);
            check("前面緊貼著漢字也找得到",
                    glued != null && "重擊".equals(glued.translation()));
            check("找到的範圍剛好是名稱",
                    glued != null && glued.start() == 2 && glued.end() == 6);
            TranslationStore.Term tail =
                    store.findTerm("使用 Bash時", 0);
            check("後面緊貼著漢字也找得到",
                    tail != null && "重擊".equals(tail.translation()));

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
            // 六成剛好會把這兩個<b>不同任務的不同目標</b>收進來——門檻訂在六成時
            // 畫面上「進入 [-」會閃一下變成「進入 [1234」。見 SAME_LINE_PERCENT。
            check("兩個不同任務的目標句不算同一句",
                    !TranslationStore.sameLine(
                            "Enter the castle at [-{~}, {~}, -{~}]",
                            "Enter the vault at [{~}, {~}, -{~}]"));

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

        itemNamesAreNotSubstitutableTerms(real);
        ingredientNamesIgnoreTheGearSwitch(real);
    }

    /**
     * 素材名稱<b>不受</b> F6「翻譯物品名稱」開關影響。
     *
     * <h2>先前壞在哪</h2>
     * {@code _meta.itemNames} 一個旗標同時被拿來答兩個問題：「要不要進可替換的詞表」
     * 與「受不受裝備開關管」。六個道具檔標成 {@code false}，原意是後者，
     * 但那個值同時觸發了前者，於是道具名變成可替換的詞（v1.99.71 的災情）。
     *
     * <p>v1.99.71 把它們改成 {@code true} 修好了替換，卻讓素材名稱一起落進
     * 裝備開關底下——而那個開關<b>預設是關的</b>。玩家打開素材袋，
     * 標題與說明都是中文，<b>九個素材名稱全是英文</b>，看起來就像翻譯憑空消失。
     *
     * <p>{@code ingredient.json} 的 {@code _meta.note} 從一開始就寫著
     * 「名稱不受『翻譯物品名稱』開關影響」——需求一直都在，只是沒有地方能表達它。
     * 現在由 {@code _meta.gearNames} 單獨回答第二個問題。
     *
     * <p>下面用的是玩家回報那張截圖裡的素材，逐個拿出貨語料驗。
     */
    private static void ingredientNamesIgnoreTheGearSwitch(TranslationStore real) {
        real.setTranslateNames(false);         // 開關預設就是關的
        String[][] pouch = {
            {"Ripe Aureate Fruit", "熟成金果"},
            {"Doom Stone", "厄運之石"},
            {"Infected Mass", "感染的團塊"},
            {"Dragon Aura", "龍之靈氣"},
            {"Tenebrous Plasma", "幽暗電漿"},
            {"Demonic Blood", "惡魔之血"},
            {"Thick Vines", "厚實藤蔓"},
        };
        for (String[] row : pouch) {
            String got = real.lookup(row[0]);
            check("實際語料：關著開關時素材「" + row[0] + "」照翻（拿到 " + got + "）",
                    row[1].equals(got));
        }
        // 對照組：裝備名稱必須<b>還是</b>被開關關掉，否則等於把 v1.99.71 之前的
        // 問題換個方向再犯一次。
        check("實際語料：關著開關時裝備名稱仍然不翻",
                real.lookup("Idol") == null);
    }

    /**
     * 道具名稱<b>不可以</b>進「可替換的詞」表。
     *
     * <h2>先前壞在哪</h2>
     * {@code _meta.itemNames: false} 那條分支是為<b>技能名稱與 Major ID</b> 設計的——
     * 那些名稱確實會出現在別的敘述裡（「提升 Meteor 的傷害」），所以要能被替換。
     *
     * <p>但 {@code ingredient.json}、{@code material.json}、{@code tome.json}、
     * {@code aspect.json}、{@code charm.json} 也被標成 {@code false}，
     * 於是 <b>1370 個道具名變成可替換的詞</b>，會蓋到任何剛好含有同樣字串的文字上。
     *
     * <p>使用者回報的症狀就是這個：素材 {@code Dark Matter}（暗物質）與一件<b>盔甲</b>
     * 同名，素材的譯名被貼到了盔甲上；{@code Charred Bone}（焦黑的骨）與一把<b>武器</b>
     * 同名，情況一樣。裝備名稱是刻意保留英文的（要對得上 wiki 與交易市場），
     * 卻被素材檔的譯名劫走。
     *
     * <p>順帶一提，這也讓 F6 的「翻譯物品名稱」開關對素材<b>無效</b>——
     * 走 terms 那條路不受那個開關管。
     */
    private static void itemNamesAreNotSubstitutableTerms(TranslationStore real) {
        // 這兩個名稱在語料裡同時是素材、也是裝備。裝備那份刻意沒有譯文。
        for (String name : new String[] {"Dark Matter", "Charred Bone"}) {
            String sentence = "You found a " + name + " today.";
            TranslationStore.Term hit = real.findTerm(sentence, 0);
            String matched = hit == null ? null
                    : sentence.substring(hit.start(), hit.end());
            boolean clean = !name.equals(matched);
            check("實際語料：「" + name + "」不會被當成可替換的詞塞進別的句子"
                    + (clean ? "" : "（被換成了「" + hit.translation() + "」）"), clean);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
