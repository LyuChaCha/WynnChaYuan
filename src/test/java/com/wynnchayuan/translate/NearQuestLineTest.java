package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * wiki 抄來的台詞跟實機差一兩個字時，同一個任務裡還認得出是哪一句。
 *
 * <p>實機：「You're searching a [Mythic Everlasting Pufferfish]? …」，
 * 語料：「You're searching for a …」。
 */
public final class NearQuestLineTest {

    private static int failures = 0;

    private static final String PUFFER = "You're searching for a [Mythic Everlasting Pufferfish]? "
            + "Hmm I heard that name before, my friend, Strato, told me about it years ago. "
            + "If I remember correctly it is infinite, you can feed every single person in all "
            + "the provinces from this single fish.";
    private static final String OTHER = "However, we don't have to fight over the fish in the lake, "
            + "we just need a [Mythic Everlasting Pufferfish], that would be enough, nothing else.";
    private static final String LOST = "Oh you look a bit lost, let me help you, I've lived here "
            + "for a long time, I know every corner and every snowflake.";

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-near");
        com.google.gson.JsonObject entries = new com.google.gson.JsonObject();
        add(entries, "U#1", PUFFER, "你在找 [Mythic Everlasting Pufferfish]？");
        add(entries, "U#2", OTHER, "我們只需要一條 [Mythic Everlasting Pufferfish]。");
        add(entries, "U#3", LOST, "你看起來有點迷路。");
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.add("entries", entries);
        Files.writeString(dir.resolve("quest-dialogue.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        String game = PUFFER.replace("searching for a", "searching a");
        check("整句少一個字：認得出是同一句", PUFFER.equals(store.nearQuestLine(game, "Underice")));
        String typing = game.substring(0, game.indexOf("told"));
        check("打字打到一半也認得出來", PUFFER.equals(store.nearQuestLine(typing, "Underice")));
        check("不是目前的任務就不找", store.nearQuestLine(game, "Other Quest") == null);
        check("太短不猜", store.nearQuestLine("You're searching a thing?", "Underice") == null);
        String unrelated = "The weather is quite cold today and the lake has frozen over completely, "
                + "so nobody can fish here anymore.";
        check("不相干的句子不硬配", store.nearQuestLine(unrelated, "Underice") == null);
        String changed = "Oh you look very lost, let me guide you, I have lived here "
                + "for ages, I know every street and every snowflake.";
        check("差太多字就不配", store.nearQuestLine(changed, "Underice") == null);

        System.out.println(failures == 0 ? "wiki 措辭差異：全部通過" : "wiki 措辭差異：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void add(com.google.gson.JsonObject entries, String key, String src, String dst) {
        com.google.gson.JsonObject e = new com.google.gson.JsonObject();
        e.addProperty("src", src);
        e.addProperty("dst", dst);
        e.addProperty("role", "desc");
        e.addProperty("kind", "dialogue");
        e.addProperty("quest", "Underice");
        e.addProperty("source", "wiki");
        entries.add(key, e);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
