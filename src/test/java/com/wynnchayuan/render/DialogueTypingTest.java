package com.wynnchayuan.render;

import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 逐字模擬：NPC 一個字一個字講出一句話的時候，畫面上到底出現什麼。
 *
 * <h2>為什麼非得逐字模擬</h2>
 * 拿整句去查表只驗得到「這句翻不翻得出來」。玩家回報的毛病全部發生在
 * <b>中間那幾十幀</b>——一句 60 個字的台詞會以 60 種長度進來，每一種都要
 * 各自決定畫中文還是英文。只測頭尾等於整段沒測，issue #735 就是這樣漏掉的。
 *
 * <p>還要補上<b>字停下來之後</b>那幾幀：action bar 每 tick 都會重送同一段文字
 *（見 {@code ActionBarListener#onGameInfoRewrite}），玩家真正看到的是那幾幀的
 * 結果，不是最後一個字進來的那一幀。
 *
 * <h2>釘住什麼</h2>
 * <ul>
 *   <li>一句話裡的譯文只會<b>往前長</b>，不會換成別的字（issue #735 的「跳一下英文」）。</li>
 *   <li>認出來之後不准在同一句裡掉回英文。</li>
 *   <li>講到一半被打斷的句子，已經出來的那半句中文要留著。</li>
 *   <li>打到一半的殘句不准被當成語料裡<b>另一條</b>短句貼上去。</li>
 * </ul>
 */
public final class DialogueTypingTest {

    private static int failures = 0;

    /** 模擬用的行寬。量寬度時 Minecraft 不在，一個字算 6px（見 measure）。 */
    private static final int WIDTH = 232;

    /** 模擬一句要跑幾十幀，全庫兩萬多句跑起來太久；取前面這麼多句。 */
    private static final int SAMPLE = 1500;

    /** 字停下來之後還會再收到幾幀同樣的 action bar。要比 SETTLE_FRAMES 多。 */
    private static final int STILL_FRAMES = 12;

    /** 模擬時要不要假裝玩家正在追蹤這句台詞所屬的任務。 */
    private static final boolean QUEST_SCOPE =
            !"0".equals(System.getProperty("wcy.questScope", "1"));

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        fragments();
        composite();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT));
        corpus(store);
        panel(store);

        System.out.println(failures == 0 ? "\n逐字模擬：全部通過"
                : "\n逐字模擬：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // 針對性的小語料：釘住的是行為，不會隨著語料增減而飄
    // ------------------------------------------------------------------

    /**
     * 打到一半的殘句不准被當成<b>另一條</b>短句貼上去。
     *
     * <h2>issue #735 的第一個成因</h2>
     * 「Not even death saw the end of my misfortunes.」打到第二個字是「No」，
     * 而「No」自己就是語料裡的一條。先前
     * {@link DialogueRewriter#line} 的整句查表有
     * {@code TranslationStore#hasLonger} 擋著，往下的 {@code join} 卻沒有，
     * 於是同一份語料從後門放行：畫面上「不」閃一下，再掉回英文，
     * 等到第六個字左右才換成正確的譯文。
     */
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

    /**
     * 拼出來的訊息（一句話 + 一個名字）照樣要翻，而且中間不准閃回英文。
     *
     * <p>任務開始那則就是這個形狀。語料裡有「New Quest Started:」也有任務名，
     * 就是沒有黏在一起的那一條——{@link DialogueRewriter} 的 {@code join} 是
     * 為它寫的。這裡順便釘住：任務名打到一半的那十幾幀要沿用上一幀的譯文，
     * 不可以掉回英文（那正是玩家看到的「翻譯閃一下不見」）。
     */
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

    /** 只有這幾條的倉庫：驗的是行為，不受語料增減影響。 */
    private static TranslationStore miniStore(String json) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-typing");
        Files.writeString(dir.resolve("misc.json"), json, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    // ------------------------------------------------------------------
    // 真語料：一次跑一千多句，抓的是「整批會不會有例外」
    // ------------------------------------------------------------------

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
                    // 整句幾十幀印出來沒人看得完，只留<b>換掉的那一刀</b>
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
        // 「中文換成另一段中文」不在這裡斷言。那幾句是<b>語料</b>裡同一段台詞收了
        // 兩條（wiki 抄的與照實機校訂的，差在標點或一兩個字），前綴比對會先挑到
        // 校訂版、整句打完才換回本尊——決定權在
        // {@code TranslationStore#settle}，不是這條就地取代的路。數字會隨語料
        // 增減而動，釘死只會讓語料 PR 無辜地紅掉；印出來讓人看得到就夠了。

        interrupted(store, lines);
        tail(store, lines);
    }

    /**
     * 講完之後那幾幀。
     *
     * <p>最後一個字進來時畫面並不會停——action bar 每 tick 都會再送一次同樣的
     * 文字。玩家真正盯著看的就是這幾幀：句子已經講完、字不會再動了，
     * 畫面上的譯文<b>也不該再動</b>。
     *
     * <p>先前這裡沒有測。實機回報「句子收尾那一下會變」——最後一個字
     *（往往是句點或逗號）進來之後，譯文先出一個版本，過幾幀又換成另一個。
     */
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
                continue;                      // 這句從頭到尾沒翻出來，不是這裡的事
            }
            boolean moved = false;
            for (String frame : still) {
                if (!settledAt.equals(frame)) {
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

    /**
     * 講到一半被打斷：已經出來的那半句中文要留著，不可以整段掉回英文。
     *
     * <p>玩家回報「語音講到一半斷掉，整段話就變成英文」。斷在哪裡都要驗——
     * 斷得早的那幾句<b>本來就</b>認不出是哪一條（證據不夠），那不算退步，
     * 所以只看「已經認出來過」的句子。
     */
    private static void interrupted(TranslationStore store, List<String[]> lines) {
        for (int share : new int[] {2, 3, 5}) {           // 斷在五分之二、三分之一…
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
                    continue;              // 斷得太早，本來就認不出來
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

    // ------------------------------------------------------------------

    /**
     * 一個字一個字餵進 {@link DialogueRewriter#line}，收下每一幀的結果。
     *
     * @param upto 打到第幾個字就停住（等於整句長度就是講完）
     */
    private static List<String> typeOut(String src, TranslationStore store, int upto) {
        DialogueRewriter.forget();
        List<String> frames = new ArrayList<>();
        int stop = Math.max(1, Math.min(upto, src.length()));
        for (int at = 1; at <= stop; at++) {
            frames.add(frame(src.substring(0, at), store));
        }
        // 字停下來之後畫面沒有停——action bar 每 tick 都會重送同一段文字。
        // 「停下來就可以定案」（見 DialogueRewriter#settled）只有在這幾幀才成立。
        String raw = src.substring(0, stop);
        for (int again = 0; again < STILL_FRAMES; again++) {
            frames.add(frame(raw, store));
        }
        return frames;
    }

    private static String frame(String raw, TranslationStore store) {
        try {
            return DialogueRewriter.line(raw, store, rowsFor(raw), null, WIDTH);
        } catch (RuntimeException e) {
            return "！" + e;
        }
    }

    /** 兩項都要：不在中英之間跳，也不把中文換成別段中文。 */
    private static boolean steady(List<String> frames) {
        return noFlip(frames) && noSwap(frames);
    }

    /**
     * 出過中文之後就不准再變回英文。
     *
     * <p>這是玩家真正看得見的那件事（issue #735）：一句話講到一半，
     * 已經是中文的字忽然又變回英文。前面幾幀還沒認出來、留著英文是可以的，
     * 出來過再縮回去不行。
     */
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

    /** 畫出來的中文只准<b>往前長</b>，不准換成另一段中文。 */
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

    // ------------------------------------------------------------------
    // 另一條路：翻譯畫在自己的小框裡（dialogueMode=面板）
    // ------------------------------------------------------------------

    /**
     * 小框模式的逐字模擬。
     *
     * <h2>為什麼要分開測</h2>
     * 就地取代（{@link DialogueRewriter}）與小框（{@link DialogueOverlay}）是
     * <b>兩份</b>程式，各自決定這一幀要畫什麼。issue #735 只修了前者，
     * 玩家換成小框模式就又看到閃爍——同一個毛病，另一條路。
     *
     * <p>這裡照 {@code DialogueOverlay#setCurrent} 的樣子模擬：先問快取
     *（{@code canReuse}），問不到才重算，重算不出來就擺原文。
     */
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

    /** 照 {@code DialogueOverlay#setCurrent} 的流程跑一句話的每一幀。 */
    private static List<String> panelFrames(String src, TranslationStore store) {
        List<String> frames = new ArrayList<>();
        String[] cache = new String[2];            // [0] 認出來的原文 [1] 畫上去的譯文
        for (int at = 1; at <= src.length(); at++) {
            frames.add(panelFrame(src.substring(0, at), store, cache, false));
        }
        for (int again = 0; again < STILL_FRAMES; again++) {
            // 字停下來之後對話事件照樣會進來，第六次起算「講完了」
            frames.add(panelFrame(src, store, cache, again >= 6));
        }
        return frames;
    }

    private static String panelFrame(String raw, TranslationStore store,
                                     String[] cache, boolean steady) {
        com.wynntils.core.text.StyledText line =
                com.wynntils.core.text.StyledText.fromString(raw);
        String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
        if (DialogueOverlay.canReuse(cache[0], template)) {
            return cache[1];
        }
        DialogueOverlay.LineResult result =
                DialogueOverlay.translateLine(line, template, store, steady);
        if (result == null) {
            return null;                           // 認不出來：小框擺原文
        }
        cache[0] = result.source();
        cache[1] = result.translated().getString();
        return cache[1];
    }

    /** 第一次「不是往前長」的那一刀：換掉之前是什麼、之後變成什麼。 */
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

    /** 失敗時印出來的樣子：只留變化的那幾幀，不然一句就是幾十欄。 */
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

    /** 語料裡的台詞。只取不含佔位符的：模擬不出玩家名，帶了反而測到別的東西。 */
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
