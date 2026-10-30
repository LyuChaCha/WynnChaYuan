package com.wynnchayuan.golden;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.render.DialogueGoldenFrames;
import com.wynnchayuan.translate.BlockLayout;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public final class TranslationGoldenDump {

    private static final int DIALOGUE_LINES = 400;

    private final BufferedWriter out;

    private TranslationGoldenDump(BufferedWriter out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: TranslationGoldenDump <output> <translations-root> [sample files...]");
            System.exit(2);
        }
        Path output = Path.of(args[0]);
        Path root = Path.of(args[1]);
        List<Path> samples = Arrays.stream(args, 2, args.length)
                .filter(s -> !s.isBlank())
                .map(Path::of)
                .filter(Files::isRegularFile)
                .toList();

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        long started = System.nanoTime();
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            TranslationGoldenDump dump = new TranslationGoldenDump(writer);
            List<GoldenSamples.Sample> sampled = GoldenSamples.read(samples);
            for (String lang : GoldenCorpus.languages(root)) {
                dump.language(lang, root.resolve(lang), sampled);
            }
        }
        System.out.printf("golden dump: %s (%.1f s)%n", output,
                (System.nanoTime() - started) / 1e9);
        System.exit(0);
    }

    private void language(String lang, Path dir, List<GoldenSamples.Sample> samples)
            throws Exception {
        useLanguage(lang);
        CurrentQuest.set(null);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        int cases = 0;
        for (String src : GoldenCorpus.sources(dir)) {
            StyledText text = GoldenCorpus.styled(src);
            String id = lang + "\tsrc\t" + GoldenFlatten.escape(src);
            lookups(id, text, store);
            entryPoints(id, text, store);
            if (src.indexOf('\n') >= 0) {
                block(id, text, store);
            }
            cases++;
        }
        for (GoldenSamples.Sample sample : samples) {
            String id = lang + "\tsample\t" + sample.origin();
            emit(id, "input", GoldenFlatten.of(sample.text().getComponent()));
            lookups(id, sample.text(), store);
            entryPoints(id, sample.text(), store);
        }
        for (GoldenCorpus.DialogueLine line : GoldenCorpus.dialogue(dir, DIALOGUE_LINES)) {
            CurrentQuest.set(line.quest());
            String id = lang + "\tdialogue\t" + line.key();
            emit(id, "replace", changes(DialogueGoldenFrames.inPlace(line.shown(), store)));
            emit(id, "panel", changes(DialogueGoldenFrames.panel(
                    line.shown(), store, GoldenFlatten::of)));
        }
        CurrentQuest.set(null);
        System.out.printf("  %s: %d sources, %d samples%n", lang, cases, samples.size());
    }

    private void lookups(String id, StyledText text, TranslationStore store) throws IOException {
        String template = GlyphSplitter.toTemplate(text);
        emit(id, "lookup", guarded(() -> GoldenFlatten.escape(store.lookup(template))));
        emit(id, "has", guarded(() -> String.valueOf(store.hasTranslation(template))));
    }

    private void entryPoints(String id, StyledText text, TranslationStore store)
            throws IOException {
        emit(id, "translate", component(() -> LineTranslator.translate(text, store)));
        emit(id, "chat", component(() -> LineTranslator.translateChat(text, store)));
        emit(id, "label", component(() -> LineTranslator.translateLabel(text, store)));
        emit(id, "floating", component(() -> LineTranslator.translateFloating(text, store)));
    }

    private void block(String id, StyledText text, TranslationStore store) throws IOException {
        List<StyledText> rows = List.of(text.split("\n"));
        List<Component> components = new ArrayList<>(rows.size());
        for (StyledText row : rows) {
            components.add(row.getComponent());
        }
        emit(id, "block", guarded(() -> GoldenFlatten.of(LineTranslator.translateBlock(
                rows, store, BlockLayout.centered(components)))));
    }

    private void emit(String id, String kind, String result) throws IOException {
        out.write(id);
        out.write('\t');
        out.write(kind);
        out.write('\t');
        out.write(result);
        out.write('\n');
    }

    private static String component(Supplier<Component> call) {
        return guarded(() -> GoldenFlatten.of(call.get()));
    }

    private static String guarded(Supplier<String> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            return "!" + e.getClass().getName();
        }
    }

    private static String changes(List<String> frames) {
        StringBuilder out = new StringBuilder();
        String was = "";
        for (int at = 0; at < frames.size(); at++) {
            String now = frames.get(at) == null ? "null" : GoldenFlatten.escape(frames.get(at));
            if (!now.equals(was)) {
                out.append(out.isEmpty() ? "" : " ‖ ").append(at + 1).append(':').append(now);
                was = now;
            }
        }
        return out.toString();
    }

    private static void useLanguage(String lang) throws ReflectiveOperationException {
        Field field = WynnChaYuan.class.getDeclaredField("language");
        field.setAccessible(true);
        field.set(null, lang);
    }
}
