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

public final class DialogueForecastTest {

    private static int failures = 0;
    private static final int WIDTH = 232;

    private static final String QUEST = "Harbour Tale";
    private static final String[][] LINES = {
        {"Tasim", "Welcome aboard, recruit.", "歡迎上船，新兵。"},
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
        holdBlank(store);
        holdOnScreen(store);
        nameFirst();
        midway(store);
        panelFlicker(store);
        pacing();
        skip();

        corpus();

        DialogueForecast.now = System::currentTimeMillis;
        DialogueForecast.enabled = true;
        System.out.println(failures == 0 ? "\n預測下一句：全部通過"
                : "\n預測下一句：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void predicted(TranslationStore store) {
        begin();
        say(store, 1, "Tasim");
        List<String> frames = type(store, LINES[2][1], "Aledar");
        check("下一句第一個字就是中文（實際 " + frames.get(0) + "）",
              frames.get(0) != null && LINES[2][2].startsWith(frames.get(0)));
        check("一路都是這句自己的譯文", ownLine(frames, LINES[2][2]));
        check("講完是整句譯文", LINES[2][2].equals(last(frames)));
        DialogueForecast.enabled = false;
        begin();
        say(store, 1, "Tasim");
        List<String> without = type(store, LINES[2][1], "Aledar");
        DialogueForecast.enabled = true;
        check("沒有預測時第一個字還是英文（對照組，實際 " + without.get(0) + "）",
              blank(without.get(0)));
    }

    private static void skipped(TranslationStore store) {
        begin();
        say(store, 1, "Tasim");
        List<String> frames = type(store, LINES[5][1], "Tasim");
        check("跳著對話不會貼出別句（" + frames.subList(0, 4) + "）",
              ownLine(frames, LINES[5][2]));
        check("「G」同時對得上兩句時先不猜", blank(frames.get(0)));
        check("跟任務裡別句分開之前不猜", blank(frames.get(3)));
        check("分開之後（Get s）就認得出來", !blank(frames.get(4)));
    }

    private static void repeated(TranslationStore store) {
        begin();
        say(store, 4, "Tasim");
        List<String> frames = type(store, LINES[1][1], "Tasim");
        check("NPC 重講前面那句時不貼出後面那句（" + frames.subList(0, 4) + "）",
              ownLine(frames, LINES[1][2]));
        check("講完仍然翻得出來", LINES[1][2].equals(last(frames)));
    }

    private static void unknownLine(TranslationStore store) {
        begin();
        say(store, 4, "Tasim");
        String odd = "Get some water, all of you!";
        List<String> frames = type(store, odd, "Tasim");
        int split = 9;
        boolean back = true;
        for (int i = split; i < frames.size(); i++) {
            back &= frames.get(i) == null;
        }
        check("岔開之後退回英文（" + frames.subList(0, 12) + "）", back);
    }

    private static void wrongSpeaker(TranslationStore store) {
        begin();
        say(store, 1, "Tasim");
        List<String> frames = type(store, LINES[2][1], "Harbour Villager");
        check("名牌對不上時不猜（實際 " + frames.get(0) + "）", blank(frames.get(0)));
    }

    private static void noNameplate(TranslationStore store) {
        begin();
        say(store, 1, "Tasim");
        List<String> frames = type(store, LINES[2][1], null);
        check("沒有名牌時不猜（實際 " + frames.get(0) + "）", blank(frames.get(0)));
    }

    private static void stale(TranslationStore store) {
        begin();
        long[] clock = {1_000_000L};
        DialogueForecast.now = () -> clock[0];
        say(store, 1, "Tasim");
        clock[0] += DialogueForecast.FRESH_MS + 1;
        List<String> frames = type(store, LINES[2][1], "Aledar");
        DialogueForecast.now = System::currentTimeMillis;
        check("隔太久不沿用上一句（實際 " + frames.get(0) + "）", blank(frames.get(0)));
    }

    private static void anchorQuest(TranslationStore store) {
        begin();
        say(store, 2, "Aledar");
        List<String> frames = type(store, LINES[3][1], null);
        int first = firstShown(frames);
        check("沒追蹤任務也在第 4 個字認出來（實際第 " + (first + 1) + " 個字）",
              first >= 0 && first <= 3);
        check("一路都是這句自己的譯文", ownLine(frames, LINES[3][2]));
    }

    private static void panel(TranslationStore store) {
        begin();
        panelSay(store, 1, "Tasim");
        DialogueLeadAudit.speak("Aledar");
        List<String> frames = DialogueLeadAudit.panelFrames(LINES[2][1], store);
        check("小框：下一句第一個字就是中文（實際 " + frames.get(0) + "）",
              LINES[2][2].equals(frames.get(0)));
        check("小框：一路都是這句自己的譯文", ownLine(frames, LINES[2][2]));
    }

    private static void panelNextLine(TranslationStore store) {
        begin();
        DialogueForecast.enabled = false;
        DialogueLeadAudit.speak(null);
        DialogueLeadAudit.panelFrames(LINES[3][1], store);
        List<String> frames = DialogueLeadAudit.panelFrames(LINES[5][1], store);
        DialogueForecast.enabled = true;
        check("小框：換句時不沿用上一句的譯文（實際 " + frames.get(0) + "）",
              frames.get(0) == null || LINES[5][2].equals(frames.get(0)));
    }

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

    private static void holdBlank(TranslationStore store) {
        begin();
        DialogueLeadAudit.speak(null);
        CurrentQuest.set(QUEST);
        List<String> known = DialogueLeadAudit.typeOut(LINES[0][1], store);
        CurrentQuest.set(null);
        boolean english = false;
        int first = firstShown(known);
        for (int i = 0; i < first; i++) {
            english |= known.get(i) == null;
        }
        check("認出來之前先空著（第一幀 " + quote(known.get(0)) + "）", "".equals(known.get(0)));
        check("中文出來之前一個英文字都沒露（第 " + (first + 1) + " 個字出中文）",
              first >= 0 && !english);

        begin();
        List<String> stranger = DialogueLeadAudit.typeOut("Zebras are lovely today.", store);
        check("語料裡沒有任何一句以它開頭：英文立刻出來（第一幀 " + quote(stranger.get(0)) + "）",
              stranger.get(0) == null);

        begin();
        String odd = "Welcome to the jungle, friend.";
        List<String> parted = DialogueLeadAudit.typeOut(odd, store);
        check("岔開、語料裡再也對不上之後英文就出來（第 9 個字 " + quote(parted.get(8)) + "）",
              parted.get(8) == null);

        begin();
        List<String> stopped = DialogueLeadAudit.typeOut("Welc", store);
        check("字停下來了還認不出來：放英文（最後一幀 " + quote(last(stopped)) + "）",
              last(stopped) == null);

        begin();
        List<String> slow = DialogueLeadAudit.typeOut(LINES[0][1], store);
        int cap = (int) (DialogueTyping.HOLD_MS / DialogueLeadAudit.MS_PER_CHAR) + 1;
        check("空白不超過 " + DialogueTyping.HOLD_MS + " 毫秒（第 " + (cap + 1) + " 個字 "
                        + quote(slow.get(cap)) + "）", slow.get(cap) == null);
        boolean noBlankAfter = true;
        for (int i = cap; i < slow.size(); i++) {
            noBlankAfter &= !"".equals(slow.get(i));
        }
        check("放了英文之後不再蓋回空白", noBlankAfter);
    }

    private static void holdOnScreen(TranslationStore store) throws Exception {
        var field = com.wynnchayuan.WynnChaYuan.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, new com.wynnchayuan.CollectorConfig(
                Files.createTempDirectory("wynnchayuan-forecast-cfg").resolve("config.json")));
        begin();
        net.minecraft.network.chat.Style body = net.minecraft.network.chat.Style.EMPTY.withFont(
                new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier.withDefaultNamespace(
                                "hud/dialogue/text/wynncraft/body_0")));
        net.minecraft.network.chat.MutableComponent message =
                net.minecraft.network.chat.Component.empty();
        message.append(net.minecraft.network.chat.Component.literal(
                new String(Character.toChars(0xD0000 - 116))).withStyle(body));
        message.append(net.minecraft.network.chat.Component.literal("We").withStyle(body));
        message.append(net.minecraft.network.chat.Component.literal(
                new String(Character.toChars(0xD0000 + 100))).withStyle(body));
        net.minecraft.network.chat.Component out = DialogueRewriter.rewrite(message, store);
        String shown = out == null ? "We" : out.getString();
        check("框裡畫成空白，沒有英文（實際 " + (out == null ? "沒改寫" : "改寫了")
                + "、含 We：" + shown.contains("We") + "）", out != null && !shown.contains("We"));
    }

    private static void nameFirst() throws Exception {
        SelfNames.remember("STEVEN");
        Path dir = Files.createTempDirectory("wynnchayuan-forecast-name");
        StringBuilder json = new StringBuilder("{\"entries\":{");
        for (int i = 0; i < 80; i++) {
            json.append(entry("I#" + i, "Icons", "", "{#} Icon line number " + i + ".",
                    "圖示第 " + i + " 句。")).append(',');
        }
        json.append(entry("N#0", "Gate", "Aledar", "{u}, why don't you do the honours?",
                "{u}，要不要由你來開門？"));
        json.append("}}");
        Files.writeString(dir.resolve("quest-dialogue.json"), json, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        begin();
        List<String> frames = type(store, "STEVEN, why don't you do the honours?", null);
        check("玩家名開頭的句子打到名字中間時還空著（第 3 個字 " + quote(frames.get(2)) + "）",
              "".equals(frames.get(2)));
    }

    private static void midway(TranslationStore store) {
        begin();
        com.wynnchayuan.capture.DialogueProgress.settle(100);
        say(store, 4, "Tasim");
        List<String> mid = type(store, LINES[5][1], "Tasim");
        check("對話講到一半：第 2 個字就認出下一句（" + mid.subList(0, 3) + "）",
              !blank(mid.get(1)) && ownLine(mid, LINES[5][2]));

        begin();
        com.wynnchayuan.capture.DialogueProgress.settle(com.wynnchayuan.capture.DialogueProgress.FULL);
        say(store, 4, "Tasim");
        List<String> full = type(store, LINES[5][1], "Tasim");
        check("對話講完了（滿格）：照舊等到跟別句分開（" + full.subList(0, 5) + "）",
              blank(full.get(3)));
        com.wynnchayuan.capture.DialogueProgress.settle(-1);
    }

    private static void panelFlicker(TranslationStore store) throws Exception {
        var cfg = com.wynnchayuan.WynnChaYuan.class.getDeclaredField("config");
        cfg.setAccessible(true);
        Path tmp = Files.createTempDirectory("wynnchayuan-forecast-panel");
        cfg.set(null, new com.wynnchayuan.CollectorConfig(tmp.resolve("config.json")));
        var st = com.wynnchayuan.WynnChaYuan.class.getDeclaredField("store");
        st.setAccessible(true);
        st.set(null, new com.wynnchayuan.capture.CaptureStore(tmp.resolve("captured.json")));

        for (int variant = 0; variant < 4; variant++) {
            boolean named = variant % 2 == 0;
            tail = variant >= 2;
            begin();
            DialogueOverlay.resetForTest();
            String a = LINES[3][1];
            String b = LINES[2][1];
            DialogueLeadAudit.speak("Tasim");
            for (int i = 1; i <= a.length(); i++) {
                panelEvent(a.substring(0, i), store);
            }
            for (int i = 0; i < 8; i++) {
                panelEvent(a, store);
            }
            String old = text(DialogueOverlay.currentForTest());
            DialogueLeadAudit.speak(named ? "Aledar" : null);
            List<String> seen = new ArrayList<>();
            panelEvent(a, store);
            seen.add(text(DialogueOverlay.currentForTest()));
            for (int i = 1; i <= b.length(); i++) {
                panelEvent(b.substring(0, i), store);
                seen.add(text(DialogueOverlay.currentForTest()));
                if (i == 2) {
                    panelEvent(a, store);
                    seen.add(text(DialogueOverlay.currentForTest()));
                }
            }
            int hides = 0;
            int stale = 0;
            List<String> shown = new ArrayList<>();
            boolean started = false;
            for (int i = 1; i < seen.size(); i++) {
                String now = seen.get(i);
                if (now.isEmpty()) {
                    hides++;
                } else if (now.equals(old)) {
                    stale++;
                } else if (!now.isBlank() && (shown.isEmpty() || !shown.get(shown.size() - 1).equals(now))) {
                    shown.add(now);
                }
            }
            String label = (named ? "（有名牌，會預測" : "（沒名牌，先空著")
                    + (tail ? "、結尾帶排版偏移）" : "）");
            check("小框換句時框不會收掉再冒出來" + label + "（收掉 " + hides + " 次）", hides == 0);
            check("小框換句後不再出現上一句的譯文" + label + "（" + stale + " 次）", stale == 0);
            check("小框這一句的譯文只出現一次、沒有換來換去" + label + "（" + shown + "）",
                  shown.size() == 1 && shown.get(0).contains(LINES[2][2]));
        }
        tail = false;
    }

    private static void pacing() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-forecast-pace");
        Files.writeString(dir.resolve("quest.json"), "{"
                + "\"Please go to the old mill by the river and fetch me {~} sacks of grain.\": "
                + "\"請幫我拿 {~} 袋穀子來，去河邊那座老磨坊就找得到，拜託你了。\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        begin();
        List<String> frames = type(store,
                "Please go to the old mill by the river and fetch me 12 sacks of grain.", null);
        int prev = -1;
        int worst = 0;
        boolean back = false;
        for (String f : frames) {
            if (blank(f)) {
                continue;
            }
            int len = f.codePointCount(0, f.length());
            if (prev >= 0) {
                worst = Math.max(worst, len - prev);
                back |= len < prev;
            }
            prev = len;
        }
        check("中文一幀最多長 " + DialogueTyping.MAX_STEP + " 個字（實際最多 " + worst + "）",
              worst <= DialogueTyping.MAX_STEP);
        check("中文不倒退", !back);
        check("最後補成整句", "請幫我拿 12 袋穀子來，去河邊那座老磨坊就找得到，拜託你了。".equals(last(frames)));
    }

    private static boolean tail;

    private static void skip() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-forecast-skip");
        Files.writeString(dir.resolve("quest.json"), "{"
                + "\"Please go to the old mill by the river and fetch me {~} sacks of grain.\": "
                + "\"請幫我拿 {~} 袋穀子來，去河邊那座老磨坊就找得到，拜託你了。\","
                + "\"Do you like what I've done with the palace? And my most precious collection...\": "
                + "\"你喜歡我把宮殿佈置成這樣嗎？還有我最珍貴的收藏……\","
                + "\"Hold on now\": \"等一下\","
                + "\"Hold on now, wait for me.\": \"等一下，等等我。\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        String grain = "Please go to the old mill by the river and fetch me 12 sacks of grain.";
        String grainZh = "請幫我拿 12 袋穀子來，去河邊那座老磨坊就找得到，拜託你了。";
        String palace = "Do you like what I've done with the palace? And my most precious collection...";
        String palaceZh = "你喜歡我把宮殿佈置成這樣嗎？還有我最珍貴的收藏……";
        Object[][] cases = {
            {"一開始就跳", palace, palaceZh, 3},
            {"半句跳", palace, palaceZh, palace.length() / 2},
            {"數字出來之後跳", grain, grainZh, grain.indexOf("12 sacks") + 9},
            {"第一句第一幀就是整句", palace, palaceZh, 0},
            {"短句、又是另一句的開頭", "Hold on now", "等一下", 0},
        };
        for (Object[] c : cases) {
            String name = (String) c[0];
            String src = (String) c[1];
            String want = (String) c[2];
            int typed = (Integer) c[3];
            begin();
            DialogueLeadAudit.speak(null);
            long[] clock = {5_000_000L};
            DialogueTyping.holdClock = () -> clock[0];
            String out = null;
            for (int i = 1; i <= typed; i++) {
                clock[0] += 40;
                DialogueTyping.line(src.substring(0, i), store, 3, null, 232);
            }
            clock[0] += 40;
            out = DialogueTyping.line(src, store, 3, null, 232);
            check("跳過（" + name + "）：就地取代那一則就是整句（實際 " + quote(out) + "）",
                  want.equals(out));
            begin();
            DialogueOverlay.resetForTest();
            for (int i = 1; i <= typed; i++) {
                panelEvent(src.substring(0, i), store);
            }
            panelEvent(src, store);
            String panel = text(DialogueOverlay.currentForTest());
            check("跳過（" + name + "）：小框那一則就是整句（實際 " + panel + "）",
                  panel.contains(want));
        }
        DialogueTyping.holdClock = System::currentTimeMillis;
    }

    private static void panelEvent(String text, TranslationStore store) {
        String shown = tail ? text + new String(Character.toChars(0xD0000 + 80 - text.length()))
                : text;
        DialogueOverlay.setCurrent(com.wynntils.core.text.StyledText.fromString(shown), store);
    }

    private static String text(List<net.minecraft.network.chat.Component> rows) {
        StringBuilder out = new StringBuilder();
        for (net.minecraft.network.chat.Component row : rows) {
            out.append(row.getString());
        }
        return out.toString();
    }

    private static String quote(String frame) {
        return frame == null ? "英文" : frame.isEmpty() ? "空白" : frame;
    }

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

    private static int[] chain(List<List<DialogueLeadAudit.Line>> stories, TranslationStore store,
                               Map<String, String> want, boolean panel, boolean forecast) {
        DialogueForecast.enabled = forecast;
        int lead = 0;
        int wrong = 0;
        int lines = 0;
        for (List<DialogueLeadAudit.Line> story : stories) {
            begin();
            for (int li = 0; li < story.size(); li++) {
                DialogueLeadAudit.Line l = story.get(li);
                boolean lastOfTalk = li + 1 >= story.size()
                        || !story.get(li + 1).stage().equals(l.stage());
                com.wynnchayuan.capture.DialogueProgress.settle(
                        lastOfTalk ? com.wynnchayuan.capture.DialogueProgress.FULL : 100);
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

    private static TranslationStore miniStore() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-forecast");
        StringBuilder json = new StringBuilder("{\"entries\":{");
        for (int i = 0; i < LINES.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(entry(QUEST + "#" + i, QUEST, LINES[i][0], LINES[i][1], LINES[i][2]));
        }
        json.append(',').append(entry("Other#0", "Other Tale", "Tasim",
                "Quiet! Someone is coming.", "安靜！有人來了。"));
        json.append("}}");
        Files.writeString(dir.resolve("quest-dialogue.json"), json, StandardCharsets.UTF_8);
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

    private static void begin() {
        com.wynnchayuan.capture.DialogueProgress.settle(-1);
        DialogueTyping.forget();
        DialogueForecast.forgetAll();
        DialogueLeadAudit.resetPanel();
        CurrentQuest.set(null);
    }

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

    private static boolean ownLine(List<String> frames, String dst) {
        for (String f : frames) {
            if (f != null && !dst.startsWith(f)) {
                return false;
            }
        }
        return true;
    }

    private static boolean blank(String frame) {
        return frame == null || frame.isEmpty();
    }

    private static int firstShown(List<String> frames) {
        for (int i = 0; i < frames.size(); i++) {
            if (!blank(frames.get(i))) {
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
