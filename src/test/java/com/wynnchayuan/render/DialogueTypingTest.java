package com.wynnchayuan.render;

import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DialogueTypingTest {

    private static int failures = 0;

    private static final int WIDTH = 232;

    private static final int SAMPLE = 1500;

    private static final int STILL_FRAMES = 12;

    private static final boolean QUEST_SCOPE =
            !"0".equals(System.getProperty("wcy.questScope", "1"));

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        fragments();
        composite();
        diverged();
        glyphs();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT));
        corpus(store);
        placeholders(store);
        panel(store);

        System.out.println(failures == 0 ? "\n逐字模擬：全部通過"
                : "\n逐字模擬：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void fragments() throws Exception {
        TranslationStore store = miniStore("""
                {
                  "No": "不",
                  "Wait.": "等等。",
                  "Not even death saw the end of my misfortunes.":
                      "就連死亡也沒能終結我的不幸。"
                }
                """);
        CurrentQuest.set(null);
        String src = "Not even death saw the end of my misfortunes.";
        List<String> frames = typeOut(src, store, src.length());

        check("殘句「No」不會被貼上語料裡另一條「不」（實際 "
                        + frames.get(1) + "）",
              !"不".equals(frames.get(1)));
        check("整句打完仍然翻得出來（實際 " + frames.get(frames.size() - 1) + "）",
              "就連死亡也沒能終結我的不幸。".equals(frames.get(frames.size() - 1)));
        check("這一句從頭到尾沒有中英來回跳", steady(frames));
    }

    private static void composite() throws Exception {
        TranslationStore store = miniStore("""
                {
                  "New Quest Started:": "新任務開始：",
                  "King's Recruit": "國王的新兵"
                }
                """);
        CurrentQuest.set(null);
        String src = "New Quest Started: King's Recruit";
        List<String> frames = typeOut(src, store, src.length());

        check("拼出來的整句翻得出來（實際 " + frames.get(frames.size() - 1) + "）",
              "新任務開始：國王的新兵".equals(frames.get(frames.size() - 1)));
        check("任務名打到一半不會掉回英文", steady(frames));
    }

    private static void diverged() throws Exception {
        TranslationStore store = miniStore("""
                {
                  "You tell the group of kids your experiences of strange creatures, even stranger men as well as a hazy memory about... a coconut?":
                      "你向那群孩子講述自己遇過的奇怪生物、更奇怪的人，還有一段模糊的記憶……跟椰子有關？"
                }
                """);
        CurrentQuest.set(null);
        String src = "You tell them about the War of the Realms. Of Light and Dark and War.";
        List<String> frames = typeOut(src, store, src.length());
        int split = "You tell them".length() - 1;
        boolean stuck = false;
        for (int at = split; at < frames.size(); at++) {
            stuck |= frames.get(at) != null;
        }
        check("岔開之後不會停在別句的半句譯文上（實際最後一幀 "
                        + frames.get(frames.size() - 1) + "）", !stuck);
    }

    private static void glyphs() {
        com.wynnchayuan.capture.LineParts parts = com.wynnchayuan.capture.LineParts.of(
                com.wynntils.core.text.StyledText.fromString("Agility, now"));
        String out = DialogueRewriter.fill("{#} 敏捷嘛，那是給愛冒險的人的。", parts);
        check("對話框不會印出「{#}」（實際 " + out + "）",
              !out.contains("{#}") && out.startsWith("敏捷"));
        String middle = DialogueRewriter.fill("點 {#} 敏捷", parts);
        check("句中的圖示一樣拿掉（實際 " + middle + "）", !middle.contains("{#}"));
    }

    private static TranslationStore miniStore(String json) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-typing");
        Files.writeString(dir.resolve("misc.json"), json, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static void corpus(TranslationStore store) throws Exception {
        List<String[]> lines = dialogueLines();
        System.out.println("=== 逐字模擬 " + lines.size() + " 句台詞"
                + (QUEST_SCOPE ? "（有追蹤任務）" : "（沒追蹤任務）") + " ===");

        int flipped = 0;
        int swapped = 0;
        int lost = 0;
        int never = 0;
        List<String> worst = new ArrayList<>();
        List<String> rivals = new ArrayList<>();
        for (String[] row : lines) {
            CurrentQuest.set(QUEST_SCOPE ? row[2] : null);
            List<String> frames = typeOut(row[0], store, row[0].length());
            if (!noFlip(frames)) {
                flipped++;
                if (worst.size() < 6) {
                    worst.add(row[0] + "\n        " + shorten(frames));
                }
            }
            if (!noSwap(frames)) {
                swapped++;
                if (rivals.size() < 12) {
                    rivals.add(swapPoint(frames) + "\n        （" + row[0] + "）");
                }
            }
            if (last(frames) == null && frames.stream().anyMatch(f -> f != null)) {
                lost++;
            }
            if (frames.stream().allMatch(f -> f == null)) {
                never++;
            }
        }
        System.out.println("  中→英→中：" + flipped);
        System.out.println("  認出來之後整句掉回英文：" + lost);
        System.out.println("  從頭到尾沒翻出來：" + never);
        System.out.println("  中文換成另一段中文：" + swapped
                + "（語料裡同一句有兩條記錄，見下）");
        for (String s : worst) {
            System.out.println("   [跳] " + s);
        }
        for (String s : rivals) {
            System.out.println("   [換] " + s);
        }
        check("同一句話裡不會在中英之間來回跳（實際 " + flipped + " 句）", flipped == 0);
        check("認出來之後不會整句掉回英文（實際 " + lost + " 句）", lost == 0);

        interrupted(store, lines);
        tail(store, lines);
    }

    private static void tail(TranslationStore store, List<String[]> lines) {
        int churn = 0;
        int wrong = 0;
        int slow = 0;
        List<String> shaky = new ArrayList<>();
        for (String[] row : lines) {
            CurrentQuest.set(QUEST_SCOPE ? row[2] : null);
            List<String> frames = typeOut(row[0], store, row[0].length());
            List<String> still = frames.subList(row[0].length(), frames.size());
            String settledAt = last(frames);
            if (settledAt == null) {
                continue;
            }
            boolean moved = false;
            for (String frame : still) {
                if (!settledAt.equals(frame) && (frame == null || !settledAt.startsWith(frame))) {
                    moved = true;
                }
            }
            if (moved) {
                churn++;
                if (shaky.size() < 8) {
                    shaky.add(row[0] + "\n        " + shorten(still));
                }
            }
            if (!settledAt.equals(row[1])) {
                wrong++;
            }
            if (!moved && !settledAt.equals(frames.get(row[0].length() - 1))) {
                slow++;
            }
        }
        System.out.println("  講完之後譯文還在變：" + churn);
        System.out.println("  講完之後的譯文不是這句的譯文：" + wrong);
        System.out.println("  最後一個字那一幀還沒定案：" + slow);
        for (String s : shaky) {
            System.out.println("   [尾] " + s);
        }
        check("講完之後譯文不會再變（實際 " + churn + " 句）", churn == 0);
    }

    private static void interrupted(TranslationStore store, List<String[]> lines) {
        for (int share : new int[] {2, 3, 5}) {
            int lost = 0;
            int kept = 0;
            for (String[] row : lines) {
                String src = row[0];
                if (src.length() < 30) {
                    continue;
                }
                CurrentQuest.set(QUEST_SCOPE ? row[2] : null);
                List<String> frames = typeOut(src, store, src.length() / share);
                if (frames.stream().allMatch(f -> f == null)) {
                    continue;
                }
                if (last(frames) == null) {
                    lost++;
                } else {
                    kept++;
                }
            }
            System.out.println("  斷在 1/" + share + "：留著中文 " + kept
                    + "，掉回英文 " + lost);
            check("斷在 1/" + share + " 的句子不會掉回英文（實際 " + lost + " 句）",
                  lost == 0);
        }
    }

    private static List<String> typeOut(String src, TranslationStore store, int upto) {
        DialogueRewriter.forget();
        List<String> frames = new ArrayList<>();
        int stop = Math.max(1, Math.min(upto, src.length()));
        for (int at = 1; at <= stop; at++) {
            frames.add(frame(src.substring(0, at), store));
        }
        String raw = src.substring(0, stop);
        for (int again = 0; again < STILL_FRAMES; again++) {
            frames.add(frame(raw, store));
        }
        return frames;
    }

    private static String frame(String raw, TranslationStore store) {
        try {
            String out = DialogueRewriter.line(raw, store, rowsFor(raw), null, WIDTH);
            return out == null || out.isEmpty() ? null : out;
        } catch (RuntimeException e) {
            return "！" + e;
        }
    }

    private static boolean steady(List<String> frames) {
        return noFlip(frames) && noSwap(frames);
    }

    private static boolean noFlip(List<String> frames) {
        boolean shown = false;
        for (String frame : frames) {
            if (frame == null) {
                if (shown) {
                    return false;
                }
            } else {
                shown = true;
            }
        }
        return true;
    }

    private static boolean noSwap(List<String> frames) {
        String grown = null;
        for (String frame : frames) {
            if (frame == null) {
                continue;
            }
            if (grown != null && !frame.startsWith(grown)) {
                return false;
            }
            grown = frame;
        }
        return true;
    }

    private static void panel(TranslationStore store) throws Exception {
        List<String[]> lines = dialogueLines();
        System.out.println("=== 小框模式 " + lines.size() + " 句台詞 ===");
        int flipped = 0;
        int swapped = 0;
        int never = 0;
        List<String> worst = new ArrayList<>();
        for (String[] row : lines) {
            CurrentQuest.set(QUEST_SCOPE ? row[2] : null);
            List<String> frames = panelFrames(row[0], store);
            if (!noFlip(frames)) {
                flipped++;
                if (worst.size() < 6) {
                    worst.add(swapPoint(frames) + "\n        （" + row[0] + "）");
                }
            }
            if (!noSwap(frames)) {
                swapped++;
            }
            if (frames.stream().allMatch(f -> f == null)) {
                never++;
            }
        }
        System.out.println("  中→英→中：" + flipped);
        System.out.println("  中文換成另一段中文：" + swapped);
        System.out.println("  從頭到尾沒翻出來：" + never);
        for (String s : worst) {
            System.out.println("   [框] " + s);
        }
        check("小框模式不會在中英之間來回跳（實際 " + flipped + " 句）", flipped == 0);
        check("小框模式講完之後翻得出來（實際 " + never + " 句沒翻出來）",
              never <= lines.size() / 100);
    }

    private static List<String> panelFrames(String src, TranslationStore store) {
        List<String> frames = new ArrayList<>();
        String[] cache = new String[2];
        for (int at = 1; at <= src.length(); at++) {
            frames.add(panelFrame(src.substring(0, at), store, cache, false));
        }
        for (int again = 0; again < STILL_FRAMES; again++) {
            frames.add(panelFrame(src, store, cache, again >= 6));
        }
        return frames;
    }

    private static String panelFrame(String raw, TranslationStore store,
                                     String[] cache, boolean steady) {
        com.wynntils.core.text.StyledText line = DialogueIcons.line(raw, store);
        String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
        if (DialogueOverlay.canReuse(cache[0], template)) {
            return cache[1];
        }
        DialogueOverlay.LineResult result =
                DialogueOverlay.translateLine(line, template, store, steady);
        if (result == null) {
            return null;
        }
        cache[0] = result.source();
        cache[1] = result.translated().getString();
        return cache[1];
    }

    private static String swapPoint(List<String> frames) {
        String grown = null;
        for (int at = 0; at < frames.size(); at++) {
            String frame = frames.get(at);
            if (frame == null) {
                continue;
            }
            if (grown != null && !frame.startsWith(grown)) {
                return "第 " + (at + 1) + " 幀　" + grown + "　→　" + frame;
            }
            grown = frame;
        }
        return "";
    }

    private static String last(List<String> frames) {
        return frames.isEmpty() ? null : frames.get(frames.size() - 1);
    }

    private static String shorten(List<String> frames) {
        StringBuilder out = new StringBuilder();
        String was = "";
        for (int at = 0; at < frames.size(); at++) {
            String now = frames.get(at) == null ? "-" : frames.get(at);
            if (!now.equals(was)) {
                out.append(at + 1).append(':').append(now).append(" | ");
                was = now;
            }
        }
        return out.toString();
    }

    private static int rowsFor(String raw) {
        for (int n = 1; n <= 5; n++) {
            if (DialogueRewriter.wrap(raw, n, null, WIDTH) != null) {
                return n;
            }
        }
        return 5;
    }

    private static void placeholders(TranslationStore store) throws Exception {
        List<String[]> lines = filledLines();
        System.out.println("=== 逐字模擬 " + lines.size() + " 句帶佔位符的台詞 ===");

        int boxes = 0;
        int never = 0;
        List<String> bad = new ArrayList<>();
        for (String[] row : lines) {
            CurrentQuest.set(QUEST_SCOPE ? row[2] : null);
            List<String> frames = typeOut(row[0], store, row[0].length());
            for (String frame : frames) {
                if (frame != null && !DialogueRewriter.renderable(frame)) {
                    boxes++;
                    if (bad.size() < 6) {
                        bad.add(row[0] + " → " + frame);
                    }
                    break;
                }
            }
            if (frames.stream().allMatch(f -> f == null)) {
                never++;
            }
        }
        System.out.println("  從頭到尾沒翻出來：" + never);
        for (String s : bad) {
            System.out.println("   [框] " + s);
        }
        check("填回佔位符之後不會出現畫不出來的字（實際 " + boxes + " 句）", boxes == 0);
    }

    private static List<String[]> filledLines() throws Exception {
        List<String[]> out = new ArrayList<>();
        Path file = Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT, "quest-dialogue.json");
        com.google.gson.JsonObject root = com.google.gson.JsonParser
                .parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
        com.google.gson.JsonObject entries = root.getAsJsonObject("entries");
        for (String key : entries.keySet()) {
            com.google.gson.JsonObject e = entries.getAsJsonObject(key);
            if (!e.has("src") || !e.has("dst")) {
                continue;
            }
            String src = e.get("src").getAsString().strip();
            if (src.isEmpty() || !src.contains("{") || src.contains("{#}")) {
                continue;
            }
            String shown = src.replaceAll("\\{~\\d*\\}", "3")
                    .replace("{u}", "Steve")
                    .replace("{p}", "Ragni");
            if (shown.contains("{") || shown.length() < 8) {
                continue;
            }
            out.add(new String[] {shown, e.get("dst").getAsString().strip(),
                    e.has("quest") ? e.get("quest").getAsString() : null});
            if (out.size() >= SAMPLE) {
                break;
            }
        }
        return out;
    }

    private static List<String[]> dialogueLines() throws Exception {
        List<String[]> out = new ArrayList<>();
        Path file = Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT, "quest-dialogue.json");
        com.google.gson.JsonObject root = com.google.gson.JsonParser
                .parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
        com.google.gson.JsonObject entries = root.getAsJsonObject("entries");
        for (String key : entries.keySet()) {
            com.google.gson.JsonObject e = entries.getAsJsonObject(key);
            if (!e.has("src") || !e.has("dst")) {
                continue;
            }
            String src = e.get("src").getAsString().strip();
            String dst = e.get("dst").getAsString().strip();
            if (src.isEmpty() || dst.isEmpty() || src.contains("{") || src.length() < 8) {
                continue;
            }
            out.add(new String[] {src, dst,
                    e.has("quest") ? e.get("quest").getAsString() : null});
            if (out.size() >= SAMPLE) {
                break;
            }
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
