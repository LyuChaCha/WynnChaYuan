package com.wynnchayuan.translate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

            write(dir, "ingredient.json", """
                {
                 "_meta": { "domain": "ingredient", "itemNames": true, "gearNames": false },
                 "entries": {
                  "i1": { "src": "Dark Matter", "dst": "暗物質", "role": "name" }
                 }
                }""");

            write(dir, "quest.json", """
                {
                 "Hey, {u}! Are you alright in there? It looks like we've hit something.":
                     "喂，{u}！你在裡面還好嗎？我們好像撞到什麼東西了。",
                 "Short one": "短句",
                 "{u}, why don't you do the honors? Go ahead and open that gate.":
                     "{u}，不如由你來吧？去把那道門打開。"
                }""");

            write(dir, "_index.json", """
                {"files": ["quest/king-s-recruit.json", "quest/queen-s-recruit.json", "quest/tower-of-ascension-quest.json"]}""");

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

            write(dir, "quest/tower-of-ascension-quest.json", """
                {
                 "_meta": {"quest": "Tower of Ascension (Quest)"},
                 "entries": {
                  "Tower of Ascension (Quest)#003": {
                   "src": "Have you got what it takes to conquer the tower?",
                   "dst": "你有本事征服這座塔嗎？",
                   "quest": "Tower of Ascension (Quest)",
                   "speaker": "Old Man"
                  }
                 }
                }""");

            write(dir, "gather.json", """
                {
                 "Blossom\n✖ Ⓒ Woodcutting Lv Min: {~}\n✖ Equipped Tool: Axe":
                     "花木\n✖ Ⓒ 伐木等級下限: {~}\n✖ 裝備工具: 斧頭",
                 "✖ Ability Points: {~}": "✖ 能力點數: {~}"
                }""");

            TranslationStore store = new TranslationStore();
            store.loadAll(dir);

            check("等級夠了的採集點也翻得出來（拿到：" + store.lookup(
                            "Blossom\n✔ Ⓒ Woodcutting Lv Min: {~}\n✖ Equipped Tool: Axe") + "）",
                    ("花木\n✔ Ⓒ 伐木等級下限: {~}\n✖ 裝備工具: 斧頭").equals(store.lookup(
                            "Blossom\n✔ Ⓒ Woodcutting Lv Min: {~}\n✖ Equipped Tool: Axe")));
            check("裝備需求列的記號也換得回來",
                    "✔ 能力點數: {~}".equals(store.lookup("✔ Ability Points: {~}")));
            check("原本那一種照舊",
                    "✖ 能力點數: {~}".equals(store.lookup("✖ Ability Points: {~}")));
            check("換記號查得到的不算缺口",
                    store.hasTranslation("✔ Ability Points: {~}"));
            check("沒有記號的句子不受影響", "短句".equals(store.lookup("Short one")));

            check("認得出最長的跨行條目有兩行", store.maxBlockLines() >= 2);
            check("整段查得到",
                    "大幅增加 Meteor 技能的\n速度。".equals(store.lookup(
                            "Drastically increase the\nspeed of your Meteor ability.")));

            store.setTranslateNames(false);
            check("關掉物品名稱後，裝備名稱不翻", store.lookup("Idol") == null);
            check("關掉物品名稱後，技能名稱照翻", "流星".equals(store.lookup("Shooting Star")));
            check("關掉物品名稱後，素材名稱照翻",
                    "暗物質".equals(store.lookup("Dark Matter")));

            store.setTranslateNames(true);
            check("開啟後裝備名稱恢復", "偶像".equals(store.lookup("Idol")));

            check("素材名稱不會被當成可替換的詞塞進別的句子",
                    store.findTerm("You found a Dark Matter today.", 0) == null);

            String wrapped = "Allies within {~} blocks gain {~} of the\n"
                    + "health you gain from Health Regen and Life Steal.";
            check("斷行位置不同也查得到",
                    store.lookupFlat(wrapped) != null);
            check("正規化不會誤中短詞", store.lookupFlat("Idol") == null);

            check("有攤平索引時，掃描行數要放大", store.maxBlockLines() >= 4);

            TranslationStore.Term term =
                    store.findTerm("Bash will hit a second time at a farther range.", 0);
            check("句子裡的技能名稱找得到", term != null && "重擊".equals(term.translation()));
            check("找到的位置正確", term != null && term.start() == 0 && term.end() == 4);

            check("整段剛好是名稱時也查得到", "重擊".equals(store.lookupTerm("Bash")));

            TranslationStore.Term longer =
                    store.findTerm("Guardian Angels 增加 +2 發彈藥。", 0);
            check("同一個起點取最長的名稱",
                    longer != null && "守護天使".equals(longer.translation()));
            check("最長的那個範圍也要對",
                    longer != null && longer.start() == 0 && longer.end() == 15);

            TranslationStore.Term mask =
                    store.findTerm("穿戴 Mask of the Lunatic 時", 0);
            check("跨多個字的名稱不被開頭那個蓋掉",
                    mask != null && "赤狂假面".equals(mask.translation()));

            TranslationStore.Term shortOne =
                    store.findTerm("Guardian 的護盾", 0);
            check("接不到長的就用短的",
                    shortOne != null && "守護者".equals(shortOne.translation()));

            check("小寫的同一個字不換", store.findTerm("you bash the ground", 0) == null);
            check("夾在字中間的不算", store.findTerm("Bashful grin", 0) == null);

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

            String wide = String.valueOf('À').repeat(40);
            check("縮排格數不同也查得到",
                    store.lookup(wide + " Knockback Immunity to Allies") != null);
            String narrow = String.valueOf('À').repeat(4);
            String indented = store.lookup(narrow + " Knockback Immunity to Allies");
            check("查到的譯文（實際：" + indented + "）",
                    (narrow + " 給友軍擊退免疫").equals(indented));
            check("接回去的是呼叫端自己的縮排，不是語料裡的那一串",
                    indented != null && !indented.startsWith(wide));
            check("沒縮排的不會誤中",
                    store.lookup("Knockback Immunity to Allies") == null);

            check("打到一半就找得到是哪一句",
                    "Hey, {u}! Are you alright in there? It looks like we've hit something."
                            .equals(store.matchPrefix("Hey, {u}! Are you alright in there? It l")));
            check("找到之後查得到譯文",
                    store.lookup(store.matchPrefix(
                            "Hey, {u}! Are you alright in there? It l")) != null);
            check("前綴太短就不猜", store.matchPrefix("Hey, {u}!") == null);
            check("模板短但實際打了夠多字就查得到",
                    "Hey, {u}! Are you alright in there? It looks like we've hit something."
                            .equals(store.matchPrefix("Hey, {u}!", "Hey, Green_teaTW!".length())));

            check("英式拼法查得到美式的語料",
                    "{u}，不如由你來吧？去把那道門打開。".equals(store.lookup(
                            "{u}, why don't you do the honours? "
                                    + "Go ahead and open that gate.")));
            check("打到一半也認得英式拼法",
                    store.matchPrefix("{u}, why don't you do the honours?") != null);
            check("換了拼法還是查不到就算了",
                    store.lookup("What colour is the honour?") == null);

            String half = "Sound off, {u}! Are you";
            check("兩個任務都有這個開頭，全庫查分不出來",
                    store.matchPrefix(half, 40) == null);
            check("知道是哪個任務就分得出來",
                    "Sound off, {u}! Are you hurt? The cart hit a boulder."
                            .equals(store.matchPrefix(half, 40, "King's Recruit")));
            check("換一個任務就是另一句",
                    "Sound off, {u}! Are you ready? The ship leaves at dawn."
                            .equals(store.matchPrefix(half, 40, "Queen's Recruit")));
            check("任務內連幾個字就夠",
                    "Sound off, {u}! Are you hurt? The cart hit a boulder."
                            .equals(store.matchPrefix("Sound off, {u}! Are you h", 5,
                                                      "King's Recruit")));
            check("任務內沒有的開頭不會硬提一句",
                    store.matchPrefix("Completely unrelated opening line", 40,
                                      "King's Recruit") == null);
            check("不知道任務時還是走舊的那一條",
                    store.matchPrefix(half, 40, null) == null);
            check("任務名字對不上就當作不知道",
                    store.matchPrefix(half, 40, "No Such Quest") == null);
            check("★ 任務檔帶 (Quest) 後綴：用追蹤欄的名字也查得到",
                    "Have you got what it takes to conquer the tower?"
                            .equals(store.matchPrefix("Have you got what", 5, "Tower of Ascension")));
            check("★ 這句屬於哪個任務，回的是追蹤欄的名字",
                    "Tower of Ascension".equals(store.questOf("Have you got what it takes to conquer the tower?")));
            check("★ 說話者名單也認得",
                    Boolean.TRUE.equals(store.inCast("Tower of Ascension", "Old Man")));

            check("同一句有校訂版時，一開始就用校訂版",
                    "Hey, {u}! Are you alright in there? It looks like we've hit something."
                            .equals(store.matchPrefix("Hey, {u}", 8, "King's Recruit")));
            check("多打幾個字之後還是同一條，不會換",
                    "Hey, {u}! Are you alright in there? It looks like we've hit something."
                            .equals(store.matchPrefix("Hey, {u}! Are you al", 20,
                                                      "King's Recruit")));
            check("沒有校訂版時 wiki 那條照用",
                    "Sound off, {u}! Are you hurt? The cart hit a boulder."
                            .equals(store.matchPrefix("Sound off, {u}! Are you h", 5,
                                                      "King's Recruit")));

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
            check("兩個不同任務的目標句不算同一句",
                    !TranslationStore.sameLine(
                            "Enter the castle at [-{~}, {~}, -{~}]",
                            "Enter the vault at [{~}, {~}, -{~}]"));

            check("後面還有更長的就要說有", store.hasLonger("Hey, {u}"));
            check("整段一模一樣但沒有更長的就說沒有",
                    !store.hasLonger("Hey, {u}! Are you alright in there? "
                            + "It looks like we've hit something."));
            check("完全不相干的開頭也是沒有", store.hasLonger("Zzzz nothing here") == false);
            check("短句沒有更長的候選時不受影響", !store.hasLonger("Short one"));
            realCorpus();
            looseMatching();
        } finally {
            delete(dir);
        }

        System.out.println(failures == 0
                ? "TranslationStore: 全部通過" : "TranslationStore: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void looseMatching() throws IOException {
        Path dir = Files.createTempDirectory("wynnchayuan-loose");
        try {
            write(dir, "quest.json", """
                {
                 "It's time for me to show the Growth Accelerator to the Corkians.":
                     "該把生長加速器拿去給 Corkus 人看了。",
                 "Well... I suppose that settles it, {u}.": "嗯……看來就這麼定了，{u}。",
                 "No, it isn't over. Not yet!": "不，還沒結束。還沒！",
                 "No, it isn't over. Not yet?": "不，還沒結束嗎？還沒？",
                 "Short line.": "短句。"
                }""");
            TranslationStore store = new TranslationStore();
            store.loadAll(dir);

            String accelerator = "該把生長加速器拿去給 Corkus 人看了。";
            check("彎引號、少了句號、多了空白也查得到", accelerator.equals(store.lookup(
                    "It’s time for me to show the  Growth Accelerator to the Corkians")));
            check("大小寫不同也查得到", accelerator.equals(store.lookup(
                    "IT'S TIME FOR ME TO SHOW THE GROWTH ACCELERATOR TO THE CORKIANS.")));
            check("… 與 ... 視為同一個", "嗯……看來就這麼定了，{u}。".equals(
                    store.lookup("Well… I suppose that settles it, {u}.")));
            check("鬆化後撞在一起、譯文又不同的不亂貼",
                    store.lookup("No, it isn't over. Not yet") == null);
            check("精確比對仍然優先", "不，還沒結束嗎？還沒？".equals(
                    store.lookup("No, it isn't over. Not yet?")));
            check("太短的不走寬鬆比對", store.lookup("short line") == null);
            check("只差標點的不記成缺口", store.hasTranslation(
                    "It's time for me to show the Growth Accelerator to the Corkians!"));
            check("不同的句子還是查不到",
                    store.lookup("It's time for me to show nothing to anyone.") == null);
        } finally {
            delete(dir);
        }
    }

    private static void write(Path dir, String name, String body) throws IOException {
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, body, StandardCharsets.UTF_8);
    }

    private static void delete(Path dir) throws IOException {
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

    private static void realCorpus() {
        Path corpus = Path.of("src/main/resources/assets/wynnchayuan/translations",
                              Languages.DEFAULT);
        if (!Files.isDirectory(corpus)) {
            return;
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

        check("實際語料：打到「Block」時後面還有更長的候選",
                real.hasLonger("Block"));
        check("實際語料：打到「Bring」時後面還有更長的候選",
                real.hasLonger("Bring"));
        check("實際語料：打到「...」時後面還有更長的候選", real.hasLonger("..."));

        itemNamesAreNotSubstitutableTerms(real);
        ingredientNamesIgnoreTheGearSwitch(real);
        buildPartNamesFollowTheGearSwitch(real);
    }

    private static void buildPartNamesFollowTheGearSwitch(TranslationStore real) {
        String[] parts = {
            "Abyssal Tome of Combat Mastery II",
            "Charm of the Corruption",
            "Aspect of Battlement Fortification",
        };
        real.setTranslateNames(false);
        for (String name : parts) {
            check("實際語料：關著開關時「" + name + "」留原文",
                    real.lookup(name) == null);
        }
        real.setTranslateNames(true);
        for (String name : parts) {
            String got = real.lookup(name);
            check("實際語料：打開開關時「" + name + "」照翻（拿到 " + got + "）",
                    got != null && !got.isBlank() && !got.equals(name));
        }
        real.setTranslateNames(false);
        check("實際語料：素材「厄運之石」仍然不受開關影響",
                "厄運之石".equals(real.lookup("Doom Stone")));
    }

    private static void ingredientNamesIgnoreTheGearSwitch(TranslationStore real) {
        real.setTranslateNames(false);
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
        check("實際語料：關著開關時裝備名稱仍然不翻",
                real.lookup("Idol") == null);
    }

    private static void itemNamesAreNotSubstitutableTerms(TranslationStore real) {
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
