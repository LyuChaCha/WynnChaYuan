package com.wynnchayuan.render;

import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.SelfNames;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 預測下一句（{@link DialogueForecast}）：逐字模擬，兩條對話路徑都跑。
 *
 * <h2>釘住什麼</h2>
 * <ul>
 *   <li>上一句認出來、名牌對得上、而且沒有別句可能時，下一句<b>第一個字</b>就是中文。</li>
 *   <li>玩家跳著對話、NPC 重講前面那句、名牌不是語料標的那個人：都<b>不准</b>貼出別句的中文。</li>
 *   <li>猜的那句之後岔開了（那句話不在語料裡）：岔開的那一幀起退回英文，不會一路錯下去。</li>
 *   <li>沒追蹤任務時，上一句所在的任務照樣能拿來縮小範圍（4 個字，跟追蹤欄同一個門檻）。</li>
 *   <li>小框換到下一句時，不准沿用上一句的譯文（舊句子剛好以新句子開頭那種）。</li>
 *   <li>真語料一句接一句模擬：預測開著的時候貼錯的句數不比關著多，第一個中文出現得更早。</li>
 * </ul>
 */
public final class DialogueForecastTest {

    private static int failures = 0;
    private static final int WIDTH = 232;

    private static final String QUEST = "Harbour Tale";
    private static final String[][] LINES = {
        // speaker, src, dst
        {"Tasim", "Welcome aboard, recruit.", "歡迎上船，新兵。"},
        // 實機收的那種：語料沒標說話者。名牌比對不到它，只有「整個任務裡還有別句」那一道擋得住
        {"", "Get the rope and help me tie this down.", "去拿繩子來幫我綁好。"},
        {"Aledar", "Quickly now, the storm is coming!", "快點，暴風雨要來了！"},
        {"Tasim", "Good work. Now head below deck.", "幹得好。現在下甲板去。"},
        {"Tasim", "Wait, did you hear that?", "等等，你聽到了嗎？"},
        {"Tasim", "Get some rest before we land.", "靠岸前先休息一下。"},
    };

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        TranslationStore store = miniStore();
        predicted(store);
        skipped(store);
        repeated(store);
        unknownLine(store);
        wrongSpeaker(store);
        noNameplate(store);
        stale(store);
        anchorQuest(store);
        panel(store);
        panelNextLine(store);
        sharedShortLine();
        otherQuestTwin();
        lookalikes();

        corpus();

        DialogueForecast.now = System::currentTimeMillis;
        DialogueForecast.enabled = true;
        System.out.println(failures == 0 ? "\n預測下一句：全部通過"
                : "\n預測下一句：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------

    /** 上一句認出來、名牌對得上：下一句第一個字就是中文。 */
    private static void predicted(TranslationStore store) {
        begin();
        say(store, 1, "Tasim");
        List<String> frames = type(store, LINES[2][1], "Aledar");
        check("下一句第一個字就是中文（實際 " + frames.get(0) + "）",
              frames.get(0) != null && LINES[2][2].startsWith(frames.get(0)));
        check("一路都是這句自己的譯文", ownLine(frames, LINES[2][2]));
        check("講完是整句譯文", LINES[2][2].equals(last(frames)));
        // 對照：沒有預測時，沒追蹤任務要打到全庫唯一（12 個字）才認得出來
        DialogueForecast.enabled = false;
        begin();
        say(store, 1, "Tasim");
        List<String> without = type(store, LINES[2][1], "Aledar");
        DialogueForecast.enabled = true;
        check("沒有預測時第一個字還是英文（對照組，實際 " + without.get(0) + "）",
              without.get(0) == null);
    }

    /** 玩家跳過一句：真正打出來的是後面那句。兩句同一個字母開頭時要等它分開。 */
    private static void skipped(TranslationStore store) {
        begin();
        say(store, 1, "Tasim");
        List<String> frames = type(store, LINES[5][1], "Tasim");   // 跳過 #2、#3、#4
        check("跳著對話不會貼出別句（" + frames.subList(0, 4) + "）",
              ownLine(frames, LINES[5][2]));
        check("「G」同時對得上兩句時先不猜", frames.get(0) == null);
        // 「Get 」也還對得上 #1（整個任務裡的另一句），要到「Get s」才分得開
        check("跟任務裡別句分開之前不猜", frames.get(3) == null);
        check("分開之後（Get s）就認得出來", frames.get(4) != null);
    }

    /**
     * NPC 重講<b>前面</b>那句提醒（玩家講完又回頭找他）。上一句之後的那幾句裡
     * 只有 #5 以 Get 開頭，但整個任務裡 #1 也是——那就不能猜。#1 語料沒標說話者，
     * 所以擋住它的是「整個任務」那一道，不是「同一個人講過的話」那一道。
     */
    private static void repeated(TranslationStore store) {
        begin();
        say(store, 4, "Tasim");
        List<String> frames = type(store, LINES[1][1], "Tasim");
        check("NPC 重講前面那句時不貼出後面那句（" + frames.subList(0, 4) + "）",
              ownLine(frames, LINES[1][2]));
        check("講完仍然翻得出來", LINES[1][2].equals(last(frames)));
    }

    /**
     * 語料裡沒有的一句，開頭跟預測的那句一樣。這是唯一猜得錯的情形；
     * 岔開的那一幀起一定要退回英文。
     */
    private static void unknownLine(TranslationStore store) {
        begin();
        say(store, 4, "Tasim");
        String odd = "Get some water, all of you!";
        List<String> frames = type(store, odd, "Tasim");
        int split = 9;                                 // 「Get some w」岔開
        boolean back = true;
        for (int i = split; i < frames.size(); i++) {
            back &= frames.get(i) == null;
        }
        check("岔開之後退回英文（" + frames.subList(0, 12) + "）", back);
    }

    /** 名牌不是語料標的那個人：不猜。 */
    private static void wrongSpeaker(TranslationStore store) {
        begin();
        say(store, 1, "Tasim");
        List<String> frames = type(store, LINES[2][1], "Harbour Villager");
        check("名牌對不上時不猜（實際 " + frames.get(0) + "）", frames.get(0) == null);
    }

    /** 沒有名牌（旁白框）：第一層不猜。 */
    private static void noNameplate(TranslationStore store) {
        begin();
        say(store, 1, "Tasim");
        List<String> frames = type(store, LINES[2][1], null);
        check("沒有名牌時不猜（實際 " + frames.get(0) + "）", frames.get(0) == null);
    }

    /** 上一句是很久以前的事：不當成同一段對話。 */
    private static void stale(TranslationStore store) {
        begin();
        long[] clock = {1_000_000L};
        DialogueForecast.now = () -> clock[0];
        say(store, 1, "Tasim");
        clock[0] += DialogueForecast.FRESH_MS + 1;
        List<String> frames = type(store, LINES[2][1], "Aledar");
        DialogueForecast.now = System::currentTimeMillis;
        check("隔太久不沿用上一句（實際 " + frames.get(0) + "）", frames.get(0) == null);
    }

    /** 沒追蹤任務、沒有名牌：上一句所在的任務照樣縮小範圍（4 個字，跟追蹤欄一樣）。 */
    private static void anchorQuest(TranslationStore store) {
        begin();
        say(store, 2, "Aledar");
        List<String> frames = type(store, LINES[3][1], null);
        int first = firstShown(frames);
        check("沒追蹤任務也在第 4 個字認出來（實際第 " + (first + 1) + " 個字）",
              first >= 0 && first <= 3);
        check("一路都是這句自己的譯文", ownLine(frames, LINES[3][2]));
    }

    /** 小框那條路：同樣第一個字就認得出來。 */
    private static void panel(TranslationStore store) {
        begin();
        panelSay(store, 1, "Tasim");
        DialogueLeadAudit.speak("Aledar");
        List<String> frames = DialogueLeadAudit.panelFrames(LINES[2][1], store);
        check("小框：下一句第一個字就是中文（實際 " + frames.get(0) + "）",
              LINES[2][2].equals(frames.get(0)));
        check("小框：一路都是這句自己的譯文", ownLine(frames, LINES[2][2]));
    }

    /**
     * 小框換到下一句：上一句「Good work…」，下一句打到「G」——舊的那條原文剛好以
     * 「G」開頭，先前就照舊擺著上一句的譯文。
     */
    private static void panelNextLine(TranslationStore store) {
        begin();
        DialogueForecast.enabled = false;              // 只看沿用那一道
        DialogueLeadAudit.speak(null);
        DialogueLeadAudit.panelFrames(LINES[3][1], store);
        List<String> frames = DialogueLeadAudit.panelFrames(LINES[5][1], store);
        DialogueForecast.enabled = true;
        check("小框：換句時不沿用上一句的譯文（實際 " + frames.get(0) + "）",
              frames.get(0) == null || LINES[5][2].equals(frames.get(0)));
    }

    /**
     * 「Alright!」這種短句好幾個任務都有。它打完會先貼上自己的譯文（句子講完了），
     * 接著同一句又打下去變成「Alright! Let's see…」。先前把「Alright!」當成上一句、
     * 隨便挑了第一個有它的任務當範圍，下一個字就貼出那個任務的「Alright! Let's get to
     * work…」（真語料逐字模擬抓到的）。
     */
    private static void sharedShortLine() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-forecast-short");
        Files.writeString(dir.resolve("quest-dialogue.json"), "{\"entries\":{"
                + entry("A#0", "Festival", "Mayor", "Alright!", "好！") + ","
                + entry("A#1", "Festival", "Mayor", "Alright! Let's get to work, everyone.",
                        "好了！大家開工吧。") + ","
                + entry("B#0", "Duel", "Rival", "Alright!", "好！") + ","
                + entry("B#1", "Duel", "Rival", "Alright! Let's see who is stronger!",
                        "好！來看看誰比較強！")
                + "}}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        begin();
        List<String> frames = type(store, "Alright! Let's see who is stronger!", null);
        check("短句好幾個任務都有時，不拿它當上一句去猜（" + frames.subList(7, 14) + "）",
              ownLine(frames, "好！來看看誰比較強！"));
    }

    /**
     * 限定任務的前綴比對命中 wiki 那條時，會去找「同一句話的校訂版」換過去
     * （{@code TranslationStore#curatedRival}）。短句的用字重疊率很容易過門檻：
     * 「Who are you!?」被換成<b>別的任務</b>的「Who are you now?」，整句貼上
     * 「現在的你是誰？」（真語料逐字模擬抓到的，追蹤任務時就會發生）。
     */
    private static void otherQuestTwin() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-forecast-twin");
        Files.writeString(dir.resolve("quest-dialogue.json"), "{\"entries\":{"
                + entry("M#0", "Missing", "Inspector", "Halt!", "站住！", "wiki") + ","
                + entry("M#1", "Missing", "Inspector", "Who are you!?", "你是誰！？", "wiki") + ","
                + entry("P#0", "Point", "Player?", "Who are you now?", "現在的你是誰？", "captured")
                + "}}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        begin();
        CurrentQuest.set("Missing");
        List<String> frames = type(store, "Who are you!?", "Inspector");
        CurrentQuest.set(null);
        check("不把這一句換成別的任務裡相像的那句（" + frames.subList(3, 8) + "）",
              ownLine(frames, "你是誰！？"));
    }

    /**
     * 追蹤任務時的前綴比對（4 個字、任務裡唯一）也會挑錯，兩種都是真語料逐字模擬抓到的：
     * <ul>
     *   <li>玩家名打到一半：「...S」時「...{u}, can we talk…」還不算以它開頭，
     *       唯一的候選變成「...S-S-Syndra?」。</li>
     *   <li>短句被當成同一句的另一個版本：「What brings you here, child.」換成
     *       quest.json 裡的「What brings you here?」。</li>
     * </ul>
     */
    private static void lookalikes() throws Exception {
        SelfNames.remember("STEVEN");
        Path dir = Files.createTempDirectory("wynnchayuan-forecast-alike");
        Files.writeString(dir.resolve("quest-dialogue.json"), "{\"entries\":{"
                + entry("E#0", "Echo", "Syndra", "...S-S-Syndra?", "……S、S、Syndra？") + ","
                + entry("E#1", "Echo", "Syndra", "...{u}, can we talk outside?",
                        "……{u}，我們可以到外面談談嗎？") + ","
                + entry("E#2", "Echo", "Citizen", "What brings you here, child.",
                        "是什麼風把你吹來的，孩子。", "wiki")
                + "}}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("quest.json"),
                "{\"What brings you here?\": \"你怎麼會來這裡？\"}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        begin();
        CurrentQuest.set("Echo");
        List<String> name = type(store, "...STEVEN, can we talk outside?", "Syndra");
        begin();
        CurrentQuest.set("Echo");
        List<String> child = type(store, "What brings you here, child.", "Citizen");
        CurrentQuest.set(null);
        check("玩家名打到一半時不貼出別句（" + name.subList(3, 8) + "）",
              ownLine(name, "……STEVEN，我們可以到外面談談嗎？"));
        check("短句不當成同一句的另一個版本（" + child.subList(4, 9) + "）",
              ownLine(child, "是什麼風把你吹來的，孩子。"));
    }

    // ------------------------------------------------------------------
    // 真語料：一句接一句
    // ------------------------------------------------------------------

    /**
     * 照語料順序把整段任務對話一句接一句打出來，名牌照語料的 speaker，
     * 預測開與關各跑一次，兩條路都跑。
     *
     * <p>「貼錯」：任何一幀的中文不是這一句自己譯文的開頭。預測開著時不能比關著多——
     * 多出來的每一句都是預測猜錯貼上去的。
     */
    private static void corpus() throws Exception {
        SelfNames.remember("STEVEN");
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        store.loadAll(root.resolve(Languages.DEFAULT));
        Map<String, List<DialogueLeadAudit.Line>> stories = new LinkedHashMap<>();
        DialogueLeadAudit.read(root.resolve(Languages.DEFAULT).resolve("quest-dialogue.json"),
                "quest", stories);
        List<List<DialogueLeadAudit.Line>> picked = new ArrayList<>();
        int i = 0;
        for (List<DialogueLeadAudit.Line> story : stories.values()) {
            if (i++ % 6 == 0) {
                picked.add(story);
            }
        }
        // 正確答案：單獨打完、停下來那一幀
        Map<String, String> want = new java.util.HashMap<>();
        Map<String, String> wantPanel = new java.util.HashMap<>();
        for (List<DialogueLeadAudit.Line> story : picked) {
            for (DialogueLeadAudit.Line l : story) {
                CurrentQuest.set(l.quest());
                DialogueLeadAudit.speak(null);
                begin();
                want.put(l.raw(), DialogueLeadAudit.last(DialogueLeadAudit.typeOut(l.raw(), store)));
                DialogueLeadAudit.resetPanel();
                wantPanel.put(l.raw(),
                        DialogueLeadAudit.last(DialogueLeadAudit.panelFrames(l.raw(), store)));
            }
        }
        for (boolean panel : new boolean[] {false, true}) {
            String path = panel ? "小框" : "就地取代";
            int[] off = chain(picked, store, panel ? wantPanel : want, panel, false);
            int[] on = chain(picked, store, panel ? wantPanel : want, panel, true);
            System.out.printf("  %s：%d 句；第一個中文之前的英文字元 合計 %d → %d；貼錯 %d → %d%n",
                    path, on[2], off[0], on[0], off[1], on[1]);
            check(path + "：預測開著時貼錯的句數不增加（" + off[1] + " → " + on[1] + "）",
                  on[1] <= off[1]);
            check(path + "：預測讓中文出現得更早（" + off[0] + " → " + on[0] + "）",
                  on[0] < off[0]);
        }
    }

    /** @return {英文字元合計, 貼錯句數, 句數}。沒追蹤任務（預測最用得上的情境）。 */
    private static int[] chain(List<List<DialogueLeadAudit.Line>> stories, TranslationStore store,
                               Map<String, String> want, boolean panel, boolean forecast) {
        DialogueForecast.enabled = forecast;
        int lead = 0;
        int wrong = 0;
        int lines = 0;
        for (List<DialogueLeadAudit.Line> story : stories) {
            begin();
            for (DialogueLeadAudit.Line l : story) {
                CurrentQuest.set(null);
                DialogueLeadAudit.speak(l.speaker());
                List<String> frames = panel ? DialogueLeadAudit.panelFrames(l.raw(), store)
                        : DialogueLeadAudit.typeOut(l.raw(), store);
                int first = firstShown(frames);
                lead += first < 0 ? l.raw().length() : Math.min(first, l.raw().length());
                String answer = want.get(l.raw());
                for (String f : frames) {
                    if (f != null && (answer == null
                            || !DialogueLeadAudit.Stats.core(answer).startsWith(
                                    DialogueLeadAudit.Stats.core(f)))) {
                        wrong++;
                        break;
                    }
                }
                lines++;
            }
        }
        DialogueForecast.enabled = true;
        return new int[] {lead, wrong, lines};
    }

    // ------------------------------------------------------------------

    private static TranslationStore miniStore() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-forecast");
        StringBuilder json = new StringBuilder("{\"entries\":{");
        for (int i = 0; i < LINES.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(entry(QUEST + "#" + i, QUEST, LINES[i][0], LINES[i][1], LINES[i][2]));
        }
        // 另一個任務裡同一個 Tasim 也講過以 Q 開頭的話；#2 是 Aledar 講的，名牌分得開
        json.append(',').append(entry("Other#0", "Other Tale", "Tasim",
                "Quiet! Someone is coming.", "安靜！有人來了。"));
        json.append("}}");
        Files.writeString(dir.resolve("quest-dialogue.json"), json, StandardCharsets.UTF_8);
        // 還沒歸到任務的台詞
        Files.writeString(dir.resolve("quest.json"), """
                {
                  "Hello there, traveller.": "你好啊，旅人。",
                  "What a lovely day it is.": "今天天氣真好。"
                }
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static String entry(String key, String quest, String speaker, String src, String dst) {
        return entry(key, quest, speaker, src, dst, "captured");
    }

    private static String entry(String key, String quest, String speaker, String src, String dst,
                                String source) {
        return "\"" + key + "\":{\"src\":\"" + src + "\",\"dst\":\"" + dst
                + "\",\"kind\":\"dialogue\",\"quest\":\"" + quest + "\",\"speaker\":\""
                + speaker + "\",\"source\":\"" + source + "\"}";
    }

    /** 換一段對話：兩條路的狀態都清掉，沒追蹤任務。 */
    private static void begin() {
        DialogueRewriter.forget();
        DialogueForecast.forgetAll();
        DialogueLeadAudit.resetPanel();
        CurrentQuest.set(null);
    }

    /** 就地取代：把第 {@code n} 句整句打完（當成上一句）。 */
    private static void say(TranslationStore store, int n, String speaker) {
        type(store, LINES[n][1], speaker);
    }

    private static void panelSay(TranslationStore store, int n, String speaker) {
        DialogueLeadAudit.speak(speaker);
        DialogueLeadAudit.panelFrames(LINES[n][1], store);
    }

    private static List<String> type(TranslationStore store, String src, String speaker) {
        DialogueLeadAudit.speak(speaker);
        return DialogueLeadAudit.typeOut(src, store);
    }

    /** 每一幀的中文都是 {@code dst} 的開頭（或還沒出現）。 */
    private static boolean ownLine(List<String> frames, String dst) {
        for (String f : frames) {
            if (f != null && !dst.startsWith(f)) {
                return false;
            }
        }
        return true;
    }

    private static int firstShown(List<String> frames) {
        for (int i = 0; i < frames.size(); i++) {
            if (frames.get(i) != null) {
                return i;
            }
        }
        return -1;
    }

    private static String last(List<String> frames) {
        return frames.isEmpty() ? null : frames.get(frames.size() - 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
