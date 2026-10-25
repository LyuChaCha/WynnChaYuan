package com.wynnchayuan.render;

import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.DialogueSpeaker;
import com.wynnchayuan.capture.SelfNames;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 量表：NPC 台詞逐字打出來時，<b>第一個中文出現之前，畫面上已經打了幾個英文字元</b>。
 *
 * <p>不是合併關卡（數字隨語料而動），是給人看的報告：
 * {@code gradle runDialogueLeadAudit [-Plang=zh_cn] [-Pevery=3]}。
 *
 * <h2>量哪幾種情境</h2>
 * <ul>
 *   <li><b>單句</b>：每一句各自打（上一句是什麼不知道），追蹤／沒追蹤任務各一輪。</li>
 *   <li><b>連續</b>：同一個任務的台詞照語料的順序一句接一句打——實機的對話本來就是
 *       這樣進來的。名牌（說話者）照語料的 speaker 填。</li>
 * </ul>
 * 另外量「貼錯」：任何一幀的中文<b>不是這一句自己譯文的開頭</b>就算一句。
 * 正確答案取「這一句單獨打完、停下來」那一幀的結果。
 */
public final class DialogueLeadAudit {

    private static final int WIDTH = 232;
    private static final int STILL_FRAMES = 12;

    /** 模擬用的玩家名。Wynncraft 在對話裡送的是全大寫。 */
    private static final String PLAYER = "STEVEN";

    record Line(String quest, String raw, String speaker, String group) {}

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        String lang = System.getProperty("wcy.lang", Languages.DEFAULT);
        int every = Integer.parseInt(System.getProperty("wcy.every", "1"));
        SelfNames.remember(PLAYER);

        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");
        List<Path> layers = new ArrayList<>();
        String base = Languages.fallbackFor(lang);
        if (base != null && !base.equals(lang)) {
            layers.add(root.resolve(base));
        }
        layers.add(root.resolve(lang));
        TranslationStore store = new TranslationStore();
        store.loadAll(layers);

        Map<String, List<Line>> stories = new LinkedHashMap<>();
        read(root.resolve(lang).resolve("quest-dialogue.json"), "quest", stories);
        read(root.resolve(lang).resolve("secret-dialogue.json"), "secret", stories);
        List<String> names = new ArrayList<>(stories.keySet());
        Map<String, List<Line>> picked = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i += every) {
            picked.put(names.get(i), stories.get(names.get(i)));
        }
        List<Line> flat = flat(root.resolve(lang).resolve("quest.json"), every);

        String only = System.getProperty("wcy.quest", "");
        if (!only.isEmpty()) {
            perLine(stories.get(only), store);
            System.exit(0);
        }
        int total = picked.values().stream().mapToInt(List::size).sum();
        System.out.println("語言 " + lang + "：故事 " + picked.size() + " 個、台詞 " + total
                + " 句、未歸屬台詞 " + flat.size() + " 句");

        // 正確答案：單獨打完、停下來那一幀
        Map<String, String> answer = new java.util.HashMap<>();
        Map<String, String> panelAnswer = new java.util.HashMap<>();
        for (List<Line> story : picked.values()) {
            for (Line l : story) {
                CurrentQuest.set(l.quest());
                speak(null);
                DialogueRewriter.forget();
                DialogueForecast.forgetAll();
                answer.put(l.raw(), last(typeOut(l.raw(), store)));
                panelAnswer.put(l.raw(), last(panelFrames(l.raw(), store)));
            }
        }

        for (boolean panel : new boolean[] {false, true}) {
            String path = panel ? "小框" : "就地取代";
            Map<String, String> want = panel ? panelAnswer : answer;
            for (boolean tracked : new boolean[] {true, false}) {
                String t = tracked ? "追蹤任務" : "沒追蹤";
                run(path + "・單句・" + t, picked, store, want, panel, tracked, false, false);
                run(path + "・連續・" + t + "・無名牌", picked, store, want, panel, tracked, true, false);
                run(path + "・連續・" + t + "・有名牌", picked, store, want, panel, tracked, true, true);
            }
        }
        // 沒歸到任務的台詞：沒有任務、沒有順序可言
        Stats s = new Stats("就地取代・未歸屬台詞（quest.json）");
        for (Line l : flat) {
            CurrentQuest.set(null);
            speak(null);
            DialogueRewriter.forget();
            DialogueForecast.forgetAll();
            String want = last(typeOut(l.raw(), store));
            DialogueRewriter.forget();
            s.add(typeOut(l.raw(), store), l.raw().length(), want);
        }
        s.print();
        System.exit(0);
    }

    private static void run(String title, Map<String, List<Line>> stories, TranslationStore store,
                            Map<String, String> want, boolean panel, boolean tracked,
                            boolean chained, boolean nameplate) {
        String only = System.getProperty("wcy.only", "");
        if (!only.isEmpty() && !java.util.Arrays.stream(only.split(","))
                .allMatch(title::contains)) {
            return;
        }
        Map<String, Stats> byGroup = new LinkedHashMap<>();
        for (List<Line> story : stories.values()) {
            DialogueRewriter.forget();
            DialogueForecast.forgetAll();
            panelCache = new String[3];
            for (Line l : story) {
                if (!chained) {
                    DialogueRewriter.forget();
                    DialogueForecast.forgetAll();
                    panelCache = new String[3];
                }
                CurrentQuest.set(tracked ? l.quest() : null);
                speak(nameplate ? l.speaker() : null);
                List<String> frames = panel ? panelFrames(l.raw(), store) : typeOut(l.raw(), store);
                byGroup.computeIfAbsent(l.group(), g -> new Stats(title + "・" + g))
                        .add(frames, l.raw().length(), want.get(l.raw()));
            }
        }
        byGroup.values().forEach(Stats::print);
    }

    /**
     * 一個任務逐句列出：沒追蹤任務、有名牌、一句接一句，預測關／開各打幾個英文字元。
     * 給實機測試挑句子用：{@code -Pquest="King's Recruit"}。
     */
    private static void perLine(List<Line> story, TranslationStore store) {
        if (story == null) {
            System.out.println("沒有這個任務");
            return;
        }
        int[][] leads = new int[2][story.size()];
        for (int mode = 0; mode < 2; mode++) {
            DialogueForecast.enabled = mode == 1;
            DialogueRewriter.forget();
            DialogueForecast.forgetAll();
            for (int i = 0; i < story.size(); i++) {
                Line l = story.get(i);
                CurrentQuest.set(null);
                speak(l.speaker());
                List<String> frames = typeOut(l.raw(), store);
                int first = -1;
                for (int f = 0; f < frames.size(); f++) {
                    if (frames.get(f) != null) {
                        first = f;
                        break;
                    }
                }
                leads[mode][i] = first < 0 ? -1 : Math.min(first, l.raw().length());
            }
        }
        DialogueForecast.enabled = true;
        for (int i = 0; i < story.size(); i++) {
            Line l = story.get(i);
            System.out.printf("%3d  %3s → %3s  [%s] %s%n", i, leads[0][i], leads[1][i],
                    l.speaker(), l.raw().length() > 70 ? l.raw().substring(0, 70) : l.raw());
        }
    }

    /** 名牌。測試用的入口，實機由 ActionBarListener 從原始訊息讀。 */
    static void speak(String name) {
        DialogueSpeaker.noteName(name);
    }

    // ------------------------------------------------------------------

    static final class Stats {
        final String title;
        final List<Integer> leads = new ArrayList<>();
        final List<Integer> blanks = new ArrayList<>();
        int english;
        int never;
        int wrong;
        int flips;
        final List<String> wrongSamples = new ArrayList<>();

        Stats(String title) {
            this.title = title;
        }

        void add(List<String> frames, int length, String want) {
            // 空字串是「認出來之前先空著」（DialogueRewriter#holds）：不是中文，也不是英文
            int first = -1;
            for (int i = 0; i < frames.size(); i++) {
                if (frames.get(i) != null && !frames.get(i).isEmpty()) {
                    first = i;
                    break;
                }
            }
            int englishChars = 0;
            int blankChars = 0;
            for (int i = 0; i < (first < 0 ? Math.min(length, frames.size()) : Math.min(first, length)); i++) {
                if (frames.get(i) == null) {
                    englishChars++;
                } else {
                    blankChars++;
                }
            }
            if (first < 0) {
                never++;
            } else {
                leads.add(englishChars);
                blanks.add(blankChars);
                if (englishChars > 0) {
                    english++;
                }
            }
            boolean shown = false;
            boolean bad = false;
            for (String f : frames) {
                if (f != null && f.isEmpty()) {
                    continue;
                }
                if (f == null) {
                    if (shown) {
                        flips++;
                        shown = false;
                        break;
                    }
                    continue;
                }
                shown = true;
                if (want == null ? true : !core(want).startsWith(core(f))) {
                    bad = true;
                }
            }
            if (bad) {
                wrong++;
                String dump = System.getProperty("wcy.dump", "");
                if (!dump.isEmpty()) {
                    try {
                        java.nio.file.Files.writeString(java.nio.file.Path.of(dump),
                                title + '\t' + want + '\t' + frames + '\n',
                                java.nio.charset.StandardCharsets.UTF_8,
                                java.nio.file.StandardOpenOption.CREATE,
                                java.nio.file.StandardOpenOption.APPEND);
                    } catch (java.io.IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                }
                if (wrongSamples.size() < 5) {
                    wrongSamples.add(want + "  ←  " + frames);
                }
            }
        }

        /** 只比譯文自己的字：名字、地名、數字是逐字填進去的，打到一半時本來就不一樣。 */
        static String core(String text) {
            StringBuilder out = new StringBuilder();
            text.codePoints().filter(cp -> cp > 0x7F && !Character.isWhitespace(cp))
                    .forEach(out::appendCodePoint);
            return out.toString();
        }

        void print() {
            List<Integer> v = new ArrayList<>(leads);
            Collections.sort(v);
            int n = v.size();
            double mean = v.stream().mapToInt(Integer::intValue).average().orElse(0);
            long zero = v.stream().filter(x -> x == 0).count();
            List<Integer> b = new ArrayList<>(blanks);
            Collections.sort(b);
            double blank = b.stream().mapToInt(Integer::intValue).average().orElse(0) * MS_PER_CHAR;
            int blankP90 = b.isEmpty() ? 0 : b.get((int) (b.size() * 0.9)) * MS_PER_CHAR;
            int blankMax = b.isEmpty() ? 0 : b.get(b.size() - 1) * MS_PER_CHAR;
            System.out.printf("%-40s 句數 %5d  英文字元 平均 %5.2f 中位 %3d P90 %3d  沒露英文 %5.1f%%"
                            + "  空白ms 平均 %3.0f P90 %3d 最長 %3d  沒翻出來 %d  中→英 %d  貼錯 %d%n",
                    title, n, mean, n == 0 ? 0 : v.get(n / 2), n == 0 ? 0 : v.get((int) (n * 0.9)),
                    n == 0 ? 0 : 100.0 * zero / n, blank, blankP90, blankMax, never, flips, wrong);
            if (Boolean.getBoolean("wcy.samples")) {
                for (String w : wrongSamples) {
                    System.out.println("    [錯] " + w);
                }
            }
        }
    }

    // ------------------------------------------------------------------

    static List<String> typeOut(String src, TranslationStore store) {
        List<String> frames = new ArrayList<>();
        for (int at = 1; at <= src.length(); at++) {
            tick();
            frames.add(frame(src.substring(0, at), store));
        }
        for (int again = 0; again < STILL_FRAMES; again++) {
            tick();
            frames.add(frame(src, store));
        }
        return frames;
    }

    /** 一個字多少毫秒。實機打字大約每秒二三十個字；-PmsPerChar= 可以換。 */
    static final int MS_PER_CHAR = Integer.getInteger("wcy.msPerChar", 40);

    private static long fakeNow = 1_000_000L;

    /** 逐字模擬的時鐘：一幀推進一個字的時間（就地取代「先空著」的上限照它算）。 */
    private static void tick() {
        fakeNow += MS_PER_CHAR;
        DialogueRewriter.holdClock = () -> fakeNow;
    }

    private static String frame(String raw, TranslationStore store) {
        try {
            return DialogueRewriter.line(raw, store, rowsFor(raw), null, WIDTH);
        } catch (RuntimeException e) {
            return "！" + e;
        }
    }

    private static String[] panelCache = new String[3];

    /** 小框的快取倒掉（換一段對話）。 */
    static void resetPanel() {
        panelCache = new String[3];
    }

    static List<String> panelFrames(String src, TranslationStore store) {
        List<String> frames = new ArrayList<>();
        for (int at = 1; at <= src.length(); at++) {
            frames.add(panelFrame(src.substring(0, at), store, false));
        }
        for (int again = 0; again < STILL_FRAMES; again++) {
            frames.add(panelFrame(src, store, again >= 6));
        }
        return frames;
    }

    private static String panelFrame(String raw, TranslationStore store, boolean steady) {
        com.wynntils.core.text.StyledText line = DialogueIcons.line(raw, store);
        String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
        // 跟 DialogueOverlay#setCurrent 一樣：同一句（原文往後長）才沿用
        boolean same = DialogueOverlay.continues(panelCache[2], raw);
        panelCache[2] = raw;
        if (same && DialogueOverlay.canReuse(panelCache[0], template)) {
            DialogueOverlay.noteHeard(store, panelCache[0], template);
            return panelCache[1];
        }
        DialogueOverlay.LineResult result =
                DialogueOverlay.translateLine(line, template, store, steady);
        if (result == null) {
            panelCache[0] = null;
            return null;
        }
        DialogueOverlay.noteHeard(store, result.source(), template);
        panelCache[0] = result.source();
        panelCache[1] = result.translated().getString();
        return panelCache[1];
    }

    private static int rowsFor(String raw) {
        for (int n = 1; n <= 5; n++) {
            if (DialogueRewriter.wrap(raw, n, null, WIDTH) != null) {
                return n;
            }
        }
        return 5;
    }

    static String last(List<String> frames) {
        return frames.isEmpty() ? null : frames.get(frames.size() - 1);
    }

    // ------------------------------------------------------------------

    /**
     * 照語料順序讀一個 bundle，一個故事一串。
     *
     * <p>只收台詞（{@code kind} 是 dialogue）。同一句台詞收了 wiki 與實機兩條的，
     * 實機只會送其中一條——wiki 那條旁邊兩格內有一條實機收的、用字重疊過半，就跳過它。
     */
    static void read(Path file, String group, Map<String, List<Line>> out)
            throws Exception {
        if (!Files.isRegularFile(file)) {
            return;
        }
        com.google.gson.JsonObject entries = com.google.gson.JsonParser
                .parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("entries");
        Map<String, List<com.google.gson.JsonObject>> raw = new LinkedHashMap<>();
        for (String key : entries.keySet()) {
            com.google.gson.JsonObject e = entries.getAsJsonObject(key);
            String kind = e.has("kind") ? e.get("kind").getAsString() : "dialogue";
            String quest = e.has("quest") ? e.get("quest").getAsString().strip() : "";
            if (!"dialogue".equals(kind) || quest.isEmpty() || !e.has("src")) {
                continue;
            }
            raw.computeIfAbsent(quest, q -> new ArrayList<>()).add(e);
        }
        for (Map.Entry<String, List<com.google.gson.JsonObject>> story : raw.entrySet()) {
            List<com.google.gson.JsonObject> ls = story.getValue();
            List<Line> lines = new ArrayList<>();
            for (int i = 0; i < ls.size(); i++) {
                com.google.gson.JsonObject e = ls.get(i);
                String src = e.get("src").getAsString().strip();
                if ("wiki".equals(str(e, "source")) && hasCapturedTwin(ls, i)) {
                    continue;
                }
                String shown = fill(src);
                if (shown == null) {
                    continue;
                }
                lines.add(new Line(story.getKey(), shown, emptyToNull(str(e, "speaker")), group));
            }
            out.put(story.getKey(), lines);
        }
    }

    private static boolean hasCapturedTwin(List<com.google.gson.JsonObject> ls, int i) {
        Set<String> me = words(ls.get(i).get("src").getAsString());
        for (int j = Math.max(0, i - 2); j <= Math.min(ls.size() - 1, i + 2); j++) {
            if (j == i || !str(ls.get(j), "source").startsWith("captured")) {
                continue;
            }
            Set<String> other = words(ls.get(j).get("src").getAsString());
            Set<String> both = new HashSet<>(me);
            both.retainAll(other);
            Set<String> any = new HashSet<>(me);
            any.addAll(other);
            if (!any.isEmpty() && both.size() * 2 >= any.size()) {
                return true;
            }
        }
        return false;
    }

    private static List<Line> flat(Path file, int every) throws Exception {
        List<Line> out = new ArrayList<>();
        com.google.gson.JsonObject root = com.google.gson.JsonParser
                .parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        int i = 0;
        for (String key : root.keySet()) {
            if (key.startsWith("_") || !root.get(key).isJsonPrimitive()) {
                continue;
            }
            if (i++ % every != 0) {
                continue;
            }
            String shown = fill(key.strip());
            if (shown != null) {
                out.add(new Line(null, shown, null, "flat"));
            }
        }
        return out;
    }

    /** 佔位符換回畫面上會出現的樣子；圖示與顏色那類還原不回去的整句跳過。 */
    private static String fill(String src) {
        if (src.isEmpty() || src.contains("{#}") || src.contains("\n")) {
            return null;
        }
        String shown = src.replaceAll("\\{~\\d*\\}", "3")
                .replace("{u}", PLAYER)
                .replace("{p}", "Ragni");
        return shown.contains("{") ? null : shown;
    }

    private static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        for (String w : text.toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    private static String str(com.google.gson.JsonObject e, String key) {
        return e.has(key) && e.get(key).isJsonPrimitive() ? e.get(key).getAsString() : "";
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
