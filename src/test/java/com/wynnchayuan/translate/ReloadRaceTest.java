package com.wynnchayuan.translate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ReloadRaceTest {

    private static final int FILLER = 4000;
    private static final int RELOADS = 120;
    private static final int READERS = 4;

    private static final String EXACT = "The ferryman will not wait for latecomers tonight.";
    private static final String FLAT = "The lighthouse keeper has not been seen since the storm rolled in from the west.";
    private static final String FLAT_SHOWN = "The lighthouse keeper has not been seen\nsince the storm rolled in from the west.";
    private static final String MARKED = "✔ Combat Lv. Min: 105";
    private static final String MARKED_SHOWN = "✖ Combat Lv. Min: 105";
    private static final String PREFIX = "The harbourmaster keeps a ledger of every ship that docks at Selchar.";

    public static void main(String[] args) throws Exception {
        Path tw = language("race-tw", "甲");
        Path ja = language("race-ja", "乙");

        TranslationStore store = new TranslationStore();
        store.loadAll(tw);

        Map<String, Set<String>> allowed = Map.of(
                EXACT, Set.of("甲 渡船", "乙 渡船"),
                FLAT_SHOWN, Set.of("甲 燈塔", "乙 燈塔"),
                MARKED_SHOWN, Set.of("✖ 甲 戰鬥等級", "✖ 乙 戰鬥等級"));
        Set<String> prefixAllowed = Set.of("甲 港務長", "乙 港務長");

        AtomicBoolean stop = new AtomicBoolean();
        AtomicLong probes = new AtomicLong();
        Map<String, Integer> seen = new ConcurrentHashMap<>();
        CountDownLatch done = new CountDownLatch(READERS);
        for (int t = 0; t < READERS; t++) {
            Thread reader = new Thread(() -> {
                try {
                    while (!stop.get()) {
                        for (Map.Entry<String, Set<String>> probe : allowed.entrySet()) {
                            String got = probe.getKey().equals(FLAT_SHOWN)
                                    ? store.lookupFlat(probe.getKey())
                                    : store.lookup(probe.getKey());
                            if (got == null || !probe.getValue().contains(got)) {
                                seen.merge(probe.getKey() + " -> " + got, 1, Integer::sum);
                            }
                            if (!probe.getKey().equals(FLAT_SHOWN) && !store.hasTranslation(probe.getKey())) {
                                seen.merge("hasTranslation " + probe.getKey() + " -> false", 1, Integer::sum);
                            }
                        }
                        String partial = store.lookupPrefix(PREFIX.substring(0, 40));
                        if (partial == null || !prefixAllowed.contains(partial)) {
                            seen.merge("lookupPrefix -> " + partial, 1, Integer::sum);
                        }
                        probes.incrementAndGet();
                    }
                } catch (Throwable e) {
                    seen.merge("exception " + e, 1, Integer::sum);
                } finally {
                    done.countDown();
                }
            }, "reload-race-reader-" + t);
            reader.setDaemon(true);
            reader.start();
        }

        int lastGeneration = store.generation();
        boolean generationsRise = true;
        for (int i = 0; i < RELOADS; i++) {
            store.loadAll(i % 2 == 0 ? ja : tw);
            int now = store.generation();
            generationsRise &= now > lastGeneration;
            lastGeneration = now;
        }
        stop.set(true);
        boolean finished = done.await(30, TimeUnit.SECONDS);

        check("讀取執行緒都收得回來（沒有卡死）", finished);
        check("重載期間查了不少次（" + probes.get() + " 輪）", probes.get() > RELOADS);
        check("每次重載 generation 都往前", generationsRise);
        seen.forEach((what, n) -> System.out.println("    " + n + " × " + what));
        check("重載期間每一次查都是完整的舊語言或新語言", seen.isEmpty());

        store.loadAll(tw);
        check("載完之後是最後載的那個語言", "甲 渡船".equals(store.lookup(EXACT)));
        store.loadAll(List.of(tw, ja));
        check("疊層照樣是上面那層贏", "乙 渡船".equals(store.lookup(EXACT)));

        System.out.println(failures == 0
                ? "重載不留半套狀態：全部通過" : "重載不留半套狀態：" + failures + " 項失敗");
        System.exit(failures > 0 ? 1 : 0);
    }

    private static Path language(String name, String mark) throws Exception {
        Path dir = Files.createTempDirectory(name);
        StringBuilder quest = new StringBuilder("{");
        quest.append(json(EXACT)).append(':').append(json(mark + " 渡船")).append(',');
        quest.append(json(FLAT)).append(':').append(json(mark + " 燈塔")).append(',');
        quest.append(json(MARKED)).append(':').append(json("✔ " + mark + " 戰鬥等級")).append(',');
        quest.append(json(PREFIX)).append(':').append(json(mark + " 港務長"));
        for (int i = 0; i < FILLER; i++) {
            quest.append(',').append(json("Filler line number " + i + " that only pads the corpus out."))
                 .append(':').append(json(mark + " 填充 " + i));
        }
        quest.append('}');
        Files.writeString(dir.resolve("quest.json"), quest.toString());
        return dir;
    }

    private static String json(String text) {
        return new com.google.gson.JsonPrimitive(text).toString();
    }

}
