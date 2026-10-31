package com.wynnchayuan.render;

import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MarqueeTest {

    public static void main(String[] args) throws Exception {
        dialogueMarquee();
        System.out.println("=== 選項跑馬燈 ===");
        Marquee.clear();

        String full = "Do you remember anything from before you joined?";
        int width = 28;
        Marquee.remember("choice_0", full.substring(0, width).strip(), "你還記得以前的事嗎？");
        boolean all = true;
        for (int s = 1; s + width <= full.length(); s++) {
            String window = full.substring(s, s + width).strip();
            if (!"你還記得以前的事嗎？".equals(Marquee.follow("choice_0", window))) {
                System.out.println("    沒接上：「" + window + "」");
                all = false;
            }
        }
        check("★ 捲到最後一格都沿用第一格的譯文", all);

        Marquee.clear();
        Marquee.remember("choice_3", "Aren't you scared of what's", "你不怕外面的世界嗎？");
        check("★ 「ren't you scared of what's o」接得上",
               "你不怕外面的世界嗎？".equals(
                       Marquee.follow("choice_3", "ren't you scared of what's o")));

        check("別的列不借用這一列的譯文",
               Marquee.follow("choice_1", "ren't you scared of what's o") == null);

        Marquee.clear();
        Marquee.remember("choice_0", "What are you going to do now", "你接下來打算怎麼辦？");
        check("不相干的一格不沿用",
               Marquee.follow("choice_0", "Tell me about the Wynn Province") == null);
        check("只重疊幾個字母不算",
               !Marquee.continues("abcdefgh", "cdefXYZ"));
        check("同一格重送照樣沿用",
               "你接下來打算怎麼辦？".equals(
                       Marquee.follow("choice_0", "What are you going to do now")));

        System.out.println(failures == 0 ? "選項跑馬燈：全部通過" : "選項跑馬燈：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void dialogueMarquee() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        DialogueLayout.widthForTest = MarqueeTest::realWidth;
        DialogueLayout.clock = () -> now;

        rhythm();
        typedThenScrolls();
        firstRowsStay();
        accentInWindow();
        fitsDoesNotScroll();
        newline();
        restartsPerLine();

        choiceFollowsGameScroll();
        shortChoiceStays();
        bodyAndChoiceTogether();
        panelHoldsMidScroll();
    }

    private static final long TICK = 50;

    private static final int STILL_FRAMES = 12;

    private static final TextColor BODY = TextColor.fromRgb(0xe0d4c0);
    private static final TextColor ITEM = TextColor.fromRgb(0x55ffff);

    private static long now = 1_000_000;


    private static void rhythm() {
        String row = "一二三四五六七八九十甲乙丙丁戊己庚辛壬癸";
        int room = 100;
        int over = realWidth(row) - room;
        long scroll = over * 1000L / Marquee.SPEED;
        long cycle = Marquee.HOLD_START + scroll + Marquee.HOLD_END;

        int[] first = Marquee.window(row, room, MarqueeTest::realWidth, 0);
        check("一開始停在開頭（實際從第 " + first[0] + " 字）", first[0] == 0);
        int[] held = Marquee.window(row, room, MarqueeTest::realWidth,
                Marquee.HOLD_START - 1);
        check("開頭停滿 HOLD_START 才捲", held[0] == 0);

        int last = 0;
        boolean smooth = true;
        boolean fits = true;
        boolean[] seen = new boolean[row.length()];
        for (long t = 0; t < cycle; t += TICK) {
            int[] w = Marquee.window(row, room, MarqueeTest::realWidth, t);
            if (w[0] < last || w[0] - last > 1) {
                smooth = false;
            }
            last = w[0];
            if (realWidth(row.substring(w[0], w[1])) > room) {
                fits = false;
            }
            for (int i = w[0]; i < w[1]; i++) {
                seen[i] = true;
            }
        }
        check("捲動時一幀最多往左移一個字、不會倒退", smooth);
        check("每一幀畫出去的那一段都放得進框寬", fits);
        boolean all = true;
        for (boolean s : seen) {
            all &= s;
        }
        check("捲完一輪每個字都出現過", all);

        int[] end = Marquee.window(row, room, MarqueeTest::realWidth,
                Marquee.HOLD_START + scroll);
        check("捲到結尾（實際迄於 " + end[1] + "／" + row.length() + "）",
              end[1] == row.length());
        int[] endHeld = Marquee.window(row, room, MarqueeTest::realWidth, cycle - 1);
        check("結尾停滿 HOLD_END", endHeld[1] == row.length());
        int[] again = Marquee.window(row, room, MarqueeTest::realWidth, cycle);
        check("一輪之後跳回開頭重來", again[0] == 0);
    }

    private static void typedThenScrolls() throws Exception {
        String src = "The old lighthouse keeper told me the storm would pass by morning.";
        String dst = "老燈塔看守人跟我說，這場暴風雨到了早上就會過去，叫我別擔心船的事。";
        TranslationStore store = miniStore(src, dst);
        int room = 120;
        List<String> frames = typeOut(src, store, 1, room);

        boolean shown = false;
        boolean fellBack = false;
        for (String frame : frames) {
            if (frame == null && shown) {
                fellBack = true;
            }
            shown |= frame != null;
        }
        check("認出來之後不會掉回英文（實際最後一幀 " + frames.get(frames.size() - 1) + "）",
              shown && !fellBack);
        check("講完之後是整句譯文", dst.equals(frames.get(frames.size() - 1)));
        check("★ 小框不接手（譯文留在對話框裡）", !DialogueRewriter.bodyTooLong());

        String full = dst;
        boolean fits = true;
        boolean newline = false;
        int moved = 0;
        String before = null;
        StringBuilder union = new StringBuilder();
        for (int tick = 0; tick < 200; tick++) {
            now += TICK;
            String again = DialogueTyping.line(src, store, 1, null, room);
            DialogueLayout.Layout laid = again == null ? null
                    : DialogueLayout.layout(again, 1, null, room, now);
            if (laid == null) {
                fits = false;
                break;
            }
            String row = laid.shown(0);
            fits &= realWidth(row) <= room;
            newline |= row.indexOf('\n') >= 0;
            if (before != null && !row.equals(before)) {
                moved++;
            }
            before = row;
            union.append(row);
        }
        check("每一幀那一列都放得進框寬", fits);
        check("那一列真的在捲（換了 " + moved + " 次）", moved > 5);
        boolean every = true;
        for (char c : full.toCharArray()) {
            every &= union.indexOf(String.valueOf(c)) >= 0;
        }
        check("捲過之後每個字都看得到", every);
        check("列裡沒有換行字元", !newline);
    }

    private static void firstRowsStay() {
        String text = "老燈塔看守人跟我說，這場暴風雨到了早上就會過去，叫我別擔心船的事，"
                + "也別急著出海，等天亮了再說。";
        int room = 120;
        String head = null;
        boolean still = true;
        int distinct = 0;
        String prev = null;
        for (int tick = 0; tick < 200; tick++) {
            now += TICK;
            DialogueLayout.Layout laid = DialogueLayout.layout(text, 2, null, room, now);
            if (laid == null) {
                check("兩列的框排得出來", false);
                return;
            }
            if (head == null) {
                head = laid.shown(0);
            }
            still &= head.equals(laid.shown(0)) && laid.offset(0) == 0;
            if (!laid.shown(1).equals(prev)) {
                distinct++;
            }
            prev = laid.shown(1);
        }
        check("第一列不動（" + head + "）", still && realWidth(head) <= room);
        check("第二列在捲", distinct > 5);
    }

    private static void accentInWindow() {
        String text = "你要是能把那雙被海盜偷走的靴子帶回來，我就給你 [Abysso Galoshes] 當謝禮！";
        int room = 100;
        List<LineParts.Piece> tint = List.of(new LineParts.Piece(
                "[Abysso Galoshes]", Style.EMPTY.withColor(ITEM)));
        String name = "[Abysso Galoshes]";
        boolean coloured = true;
        boolean half = false;
        boolean text_ok = true;
        long t0 = now;
        for (long t = 0; t < 30_000; t += TICK) {
            now = t0 + t;
            DialogueLayout.Layout laid = DialogueLayout.layout(text, 1, null, room, now);
            if (laid == null) {
                check("塞不下的那一句排得出跑馬燈", false);
                return;
            }
            int[] starts = new int[laid.rows().size()];
            List<LineParts.Piece> inked = DialogueAccents.paint(
                    DialogueAccents.flatten(laid.rows(), starts),
                    Style.EMPTY.withColor(BODY), tint, new boolean[tint.size()]);
            List<LineParts.Piece> row = DialogueLayout.cutRow(laid, inked, starts, 0);
            StringBuilder joined = new StringBuilder();
            for (LineParts.Piece piece : row) {
                joined.append(piece.text());
            }
            text_ok &= joined.toString().equals(laid.shown(0));
            int nameAt = text.indexOf(name) - laid.offset(0);
            int at = 0;
            for (LineParts.Piece piece : row) {
                for (int i = 0; i < piece.text().length(); i++, at++) {
                    boolean inName = at >= nameAt && at < nameAt + name.length();
                    if (inName && !ITEM.equals(piece.style().getColor())) {
                        coloured = false;
                    }
                }
            }
            String shown = laid.shown(0);
            if (!shown.contains(name) && (shown.contains("Galoshes]") || shown.contains("[Abysso"))) {
                half = true;
            }
        }
        check("畫出去的幾截接起來就是那一幀的視窗", text_ok);
        check("測得到名字被框邊切開的那幾幀", half);
        check("★ 名字捲到一半，框裡那半截仍是強調色", coloured);
    }

    private static void fitsDoesNotScroll() {
        String text = "老燈塔看守人說暴風雨會過去。";
        DialogueLayout.Layout laid = DialogueLayout.layout(text, 1, null, 232, now);
        check("塞得下就不捲", laid != null && !laid.scrolling()
                && laid.shown(0).equals(DialogueLayout.wrap(text, 1, null, 232).get(0)));
    }

    private static void newline() throws Exception {
        String src = "Keep your lantern lit and your boots dry, the marsh does not forgive.";
        String dst = "把你的燈籠點著，靴子保持乾燥。\n這片沼澤可不會原諒任何一個粗心的旅人。";
        TranslationStore store = miniStore(src, dst);
        int room = 120;
        List<String> frames = typeOut(src, store, 1, room);
        String last = frames.get(frames.size() - 1);
        check("帶 \\n 的譯文打完是譯文（實際 " + last + "）", last != null);
        boolean clean = true;
        boolean fits = true;
        for (int tick = 0; tick < 200; tick++) {
            now += TICK;
            String again = DialogueTyping.line(src, store, 1, null, room);
            DialogueLayout.Layout laid = again == null ? null
                    : DialogueLayout.layout(again, 1, null, room, now);
            if (laid == null) {
                fits = false;
                break;
            }
            for (String row : laid.visible()) {
                clean &= row.indexOf('\n') < 0 && row.indexOf('\r') < 0;
                fits &= realWidth(row) <= room;
            }
        }
        check("帶 \\n 的譯文捲動時列裡沒有換行字元", clean);
        check("帶 \\n 的譯文每一幀都放得進框寬", fits);
        check("帶 \\n 的譯文也不讓小框接手", !DialogueRewriter.bodyTooLong());
    }

    private static void restartsPerLine() {
        String a = "一二三四五六七八九十甲乙丙丁戊己庚辛壬癸";
        String b = "子丑寅卯辰巳午未申酉戌亥一二三四五六七八";
        for (int i = 0; i < 100; i++) {
            now += TICK;
            DialogueLayout.layout(a, 1, null, 100, now);
        }
        now += TICK;
        DialogueLayout.Layout laid = DialogueLayout.layout(b, 1, null, 100, now);
        check("換一句就從開頭開始", laid != null && laid.offset(0) == 0);
    }

    private static final String LONG_CHOICE = "Do you remember anything from before you joined?";
    private static final int WINDOW = 28;

    private static List<String> gameWindows(String full, int rounds) {
        List<String> out = new ArrayList<>();
        for (int round = 0; round < rounds; round++) {
            for (int hold = 0; hold < 14; hold++) {
                out.add(full.substring(0, WINDOW).strip());
            }
            for (int s = 1; s + WINDOW <= full.length(); s++) {
                out.add(full.substring(s, s + WINDOW).strip());
            }
            for (int hold = 0; hold < 14; hold++) {
                out.add(full.substring(full.length() - WINDOW).strip());
            }
        }
        return out;
    }

    private static String choiceFrame(String row, String window, TranslationStore store) {
        String pick = store.lookup(window);
        if (pick != null && !pick.isBlank()) {
            Marquee.remember(row, window, pick);
        } else {
            pick = Marquee.follow(row, window);
        }
        if (pick == null) {
            return null;
        }
        int[] cut = DialogueLayout.choiceWindow(row, pick, realWidth(window),
                MarqueeTest::realWidth, now);
        return pick.substring(cut[0], cut[1]);
    }

    private static void choiceFollowsGameScroll() throws Exception {
        String first = LONG_CHOICE.substring(0, WINDOW).strip();
        String dst = "你還記得自己加入我們之前發生過的任何事情嗎？一點點也好。";
        TranslationStore store = miniStore(first, dst);
        Marquee.clear();
        int room = Marquee.choiceRoom(0, MarqueeTest::realWidth);
        boolean english = false;
        boolean fits = true;
        int moved = 0;
        String prev = null;
        StringBuilder union = new StringBuilder();
        for (String window : gameWindows(LONG_CHOICE, 3)) {
            now += TICK;
            String shown = choiceFrame("choice_0", window, store);
            if (shown == null) {
                english = true;
                continue;
            }
            fits &= realWidth(shown) <= room;
            if (prev != null && !shown.equals(prev)) {
                moved++;
            }
            prev = shown;
            union.append(shown);
        }
        check("★ 原文捲動的每一格都是譯文，沒有一格掉回英文", !english);
        check("★ 選項的譯文每一幀都放得進選項框（框寬 " + room + "px）", fits);
        check("選項的譯文真的在捲（換了 " + moved + " 次）", moved > 5);
        boolean every = true;
        for (char c : dst.toCharArray()) {
            every &= union.indexOf(String.valueOf(c)) >= 0;
        }
        check("捲過之後整句選項譯文都看得到", every);
    }

    private static void shortChoiceStays() {
        String pick = "……你在說什麼？";
        int[] cut = DialogueLayout.choiceWindow("choice_3", pick, realWidth("...What?"),
                MarqueeTest::realWidth, now);
        check("短選項放得下就不捲", cut[0] == 0 && cut[1] == pick.length());
    }

    private static void bodyAndChoiceTogether() {
        Marquee.clear();
        String body = "一二三四五六七八九十甲乙丙丁戊己庚辛壬癸";
        String pick = "你還記得自己加入我們之前發生過的任何事情嗎？一點點也好。";
        int bodyMoved = 0;
        int choiceMoved = 0;
        String lastBody = null;
        String lastChoice = null;
        for (int tick = 0; tick < 200; tick++) {
            now += TICK;
            DialogueLayout.Layout laid = DialogueLayout.layout(body, 1, null, 100, now);
            int[] cut = DialogueLayout.choiceWindow("choice_0", pick, 160,
                    MarqueeTest::realWidth, now);
            String b = laid.shown(0);
            String c = pick.substring(cut[0], cut[1]);
            bodyMoved += lastBody != null && !b.equals(lastBody) ? 1 : 0;
            choiceMoved += lastChoice != null && !c.equals(lastChoice) ? 1 : 0;
            lastBody = b;
            lastChoice = c;
        }
        check("內文與選項同時捲：內文有在動（" + bodyMoved + "）", bodyMoved > 5);
        check("內文與選項同時捲：選項有在動（" + choiceMoved + "）", choiceMoved > 5);
    }

    private static void panelHoldsMidScroll() {
        String full = "Aren't you scared of what Peloros work hard to keep out there?";
        List<String> windows = gameWindows(full, 1);
        boolean held = true;
        String before = null;
        for (String window : windows) {
            List<String> cur = List.of("Just saying hello", window);
            if (before != null && !before.equals(window)) {
                List<String> was = List.of("Just saying hello", before);
                if (!window.equals(windows.get(0))
                        && !DialogueOverlay.sameScroll(was, cur)) {
                    held = false;
                    System.out.println("    沒接上：「" + before + "」→「" + window + "」");
                }
            }
            before = window;
        }
        check("★ 小框：捲動中的每一格都沿用同一組選項（含大寫開頭的那一格）", held);
        check("小框：真的換了一組選項不算捲動",
              !DialogueOverlay.sameScroll(List.of("Just saying hello", "Who are you?"),
                      List.of("Tell me about Ragni", "Goodbye")));
    }

    private static List<String> typeOut(String src, TranslationStore store, int rows, int room) {
        DialogueTyping.forget();
        Marquee.clear();
        List<String> frames = new ArrayList<>();
        for (int at = 1; at <= src.length(); at++) {
            now += TICK;
            frames.add(DialogueTyping.line(src.substring(0, at), store, rows, null, room));
        }
        for (int again = 0; again < STILL_FRAMES; again++) {
            now += TICK;
            frames.add(DialogueTyping.line(src, store, rows, null, room));
        }
        return frames;
    }

    private static TranslationStore miniStore(String src, String dst) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-marquee");
        String json = "{\n  " + quote(src) + ": " + quote(dst) + "\n}\n";
        Files.writeString(dir.resolve("misc.json"), json, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static int realWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += c >= 0x1100 && c <= 0xFFDC ? 10 : 6;
        }
        return w;
    }
}
