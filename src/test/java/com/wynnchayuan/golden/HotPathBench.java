package com.wynnchayuan.golden;

import com.sun.management.ThreadMXBean;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.capture.SelfNames;
import com.wynnchayuan.render.DialogueGoldenFrames;
import com.wynnchayuan.render.HotPathFrames;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class HotPathBench {

    private static final String LANG = "zh_tw";
    private static final int LINES = 2000;
    private static final int TOOLTIPS = 200;
    private static final int DIALOGUE_LINES = 30;
    private static final int ON_SCREEN = 40;
    private static final long ROUND_NANOS = 400_000_000L;
    private static final int ROUNDS = 5;

    private static final ThreadMXBean THREADS = (ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static volatile Object sink;

    private record Result(String name, double nanos, double bytes) {}

    private final TranslationStore store;
    private final CollectorConfig config;
    private final List<Result> results = new ArrayList<>();
    private boolean warming;

    private List<StyledText> hits;
    private List<StyledText> misses;
    private List<StyledText> real;
    private List<List<Component>> tooltips;
    private List<GoldenCorpus.DialogueLine> dialogue;

    private HotPathBench(TranslationStore store, CollectorConfig config) {
        this.store = store;
        this.config = config;
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        List<Path> files = Arrays.stream(args, 1, args.length)
                .filter(s -> !s.isBlank())
                .map(Path::of)
                .filter(Files::isRegularFile)
                .toList();

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field language = WynnChaYuan.class.getDeclaredField("language");
        language.setAccessible(true);
        language.set(null, LANG);
        GoldenWidth.install();
        SelfNames.remember("Nickname");

        Path dir = root.resolve(LANG);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        CollectorConfig config = new CollectorConfig(
                Files.createTempDirectory("wcy-bench").resolve("config.json"));

        HotPathBench bench = new HotPathBench(store, config);
        bench.inputs(dir, files);
        bench.warming = true;
        bench.all();
        bench.all();
        bench.warming = false;
        bench.all();

        System.out.printf("%-22s %12s %12s%n", "path", "ns/op", "bytes/op");
        for (Result r : bench.results) {
            System.out.printf("%-22s %12.0f %12.0f%n", r.name(), r.nanos(), r.bytes());
        }
        System.exit(0);
    }

    private void inputs(Path dir, List<Path> files) throws Exception {
        List<String> sources = new ArrayList<>(GoldenCorpus.sources(dir));
        List<StyledText> corpus = new ArrayList<>();
        int stride = Math.max(1, sources.size() / LINES);
        for (int at = 0; at < sources.size(); at += stride) {
            if (sources.get(at).indexOf('\n') < 0) {
                corpus.add(GoldenCorpus.styled(sources.get(at)));
            }
        }
        tooltips = new ArrayList<>();
        int blockStride = Math.max(1, sources.size() / (TOOLTIPS * 4));
        for (int at = 0; at < sources.size() && tooltips.size() < TOOLTIPS; at += blockStride) {
            int next = at;
            while (next < sources.size() && sources.get(next).indexOf('\n') < 0) {
                next++;
            }
            if (next == sources.size()) {
                break;
            }
            List<Component> rows = new ArrayList<>();
            for (StyledText row : GoldenCorpus.styled(sources.get(next)).split("\n")) {
                rows.add(row.getComponent());
            }
            tooltips.add(rows);
            at = next;
        }

        real = new ArrayList<>();
        for (GoldenSamples.Sample sample : GoldenSamples.read(files)) {
            real.add(sample.text());
        }
        hits = new ArrayList<>(corpus);
        misses = new ArrayList<>();
        for (StyledText text : real) {
            (store.hasTranslation(GlyphSplitter.toTemplate(text)) ? hits : misses).add(text);
        }
        for (StyledText text : corpus) {
            misses.add(GoldenCorpus.styled("[Guild] Steve: " + text.getStringWithoutFormatting()));
        }
        dialogue = GoldenCorpus.dialogue(dir, DIALOGUE_LINES);
        System.out.printf("inputs: %d hits, %d misses, %d real samples, %d tooltips, %d dialogue%n",
                hits.size(), misses.size(), real.size(), tooltips.size(), dialogue.size());
    }

    private void all() {
        List<String> hitKeys = hits.stream().map(GlyphSplitter::toTemplate).toList();
        List<String> missKeys = misses.stream().map(GlyphSplitter::toTemplate).toList();

        each("template", hits, t -> sink = GlyphSplitter.toTemplate(t));
        each("lineParts", hits, t -> sink = LineParts.of(t));
        each("playerData", hitKeys, k -> sink = PlayerDataFilter.carriesPlayerData(k));
        each("lookup.hit", hitKeys, k -> sink = store.lookup(k));
        each("lookup.miss", missKeys, k -> sink = store.lookup(k));
        each("translate.hit", hits, t -> sink = LineTranslator.translate(t, store));
        each("translate.miss", misses, t -> sink = LineTranslator.translate(t, store));
        each("chat.hit", hits, t -> sink = LineTranslator.translateChat(t, store));
        each("chat.miss", misses, t -> sink = LineTranslator.translateChat(t, store));
        each("label.hit", hits, t -> sink = LineTranslator.translateLabel(t, store));
        each("label.miss", misses, t -> sink = LineTranslator.translateLabel(t, store));
        each("floating.hit", hits, t -> sink = LineTranslator.translateFloating(t, store));
        each("floating.miss", misses, t -> sink = LineTranslator.translateFloating(t, store));
        if (!real.isEmpty()) {
            each("translate.real", real, t -> sink = LineTranslator.translate(t, store));
            each("chat.real", real, t -> sink = LineTranslator.translateChat(t, store));
        }
        each("tooltip.first", tooltips, rows -> sink = HotPathFrames.tooltip(rows, store));
        each("tooltip.cached", tooltips.subList(0, Math.min(8, tooltips.size())),
                rows -> sink = HotPathFrames.tooltipCached(rows, store));
        List<StyledText> screen = hits.subList(0, Math.min(ON_SCREEN, hits.size()));
        each("screenText.cached", screen, t -> sink = HotPathFrames.screenText(t, config, store));
        HotPathFrames.scoreboardShows(hits.subList(hits.size() - ON_SCREEN, hits.size()));
        each("screenText.board", screen, t -> sink = HotPathFrames.screenText(t, config, store));
        HotPathFrames.scoreboardShows(List.of());
        List<Component> names = new ArrayList<>();
        for (StyledText text : screen) {
            names.add(text.getComponent());
        }
        each("nameplate.cached", names, n -> sink = HotPathFrames.nameplate(n, config, store));
        frames("dialogue.typing", line -> DialogueGoldenFrames.inPlace(line.shown(), store));
        frames("dialogue.panel", line -> DialogueGoldenFrames.panel(
                line.shown(), store, Component::getString));
    }

    private <T> void each(String name, List<T> inputs, Consumer<T> call) {
        time(name, inputs.size(), () -> {
            for (T input : inputs) {
                call.accept(input);
            }
        });
    }

    private void frames(String name, Function<GoldenCorpus.DialogueLine, List<String>> call) {
        int total = 0;
        for (GoldenCorpus.DialogueLine line : dialogue) {
            total += line.shown().length() + 12;
        }
        time(name, total, () -> {
            for (GoldenCorpus.DialogueLine line : dialogue) {
                sink = call.apply(line);
            }
        });
    }

    private void time(String name, int ops, Runnable pass) {
        long id = Thread.currentThread().threadId();
        double[] nanos = new double[ROUNDS];
        double[] bytes = new double[ROUNDS];
        for (int round = 0; round < ROUNDS; round++) {
            long done = 0;
            long allocated = THREADS.getThreadAllocatedBytes(id);
            long started = System.nanoTime();
            long elapsed;
            do {
                pass.run();
                done += ops;
                elapsed = System.nanoTime() - started;
            } while (elapsed < ROUND_NANOS);
            bytes[round] = (double) (THREADS.getThreadAllocatedBytes(id) - allocated) / done;
            nanos[round] = (double) elapsed / done;
        }
        if (!warming) {
            Arrays.sort(nanos);
            Arrays.sort(bytes);
            results.add(new Result(name, nanos[ROUNDS / 2], bytes[ROUNDS / 2]));
        }
    }
}
