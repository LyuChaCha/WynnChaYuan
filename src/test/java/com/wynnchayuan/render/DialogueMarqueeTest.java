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

/**
 * 就地取代時塞不下的那一句：最後一列跑馬燈，不再讓原文冒出來。
 *
 * <h2>先前</h2>
 * 譯文比原文多一列時，{@link DialogueRewriter#line} 打完那一幀回 {@code null}，
 * 畫面在講完的那一刻變回英文（後來改成讓小框接手，原文仍然留在框裡）。
 *
 * <h2>這支釘住什麼</h2>
 * <ul>
 *   <li>逐字打完、字停下來之後，每一幀都還是譯文，旗標不會讓小框接手。</li>
 *   <li>跑馬燈的節奏：停在開頭 → 一次最多移一個字往左捲 → 停在結尾 → 跳回開頭。</li>
 *   <li>每一幀畫出去的那一段都放得進框寬；捲完一輪每個字都出現過。</li>
 *   <li>前面幾列不動，只有最後一列在捲。</li>
 *   <li>名字捲到一半、只剩半截在框裡，那半截照樣是強調色。</li>
 *   <li>塞得下的句子不捲（只在真的放不下時才用）。</li>
 *   <li>譯文帶 \n（#1298／#1301 攤平之後）照樣捲，列裡不會出現 \n。</li>
 * </ul>
 */
public final class DialogueMarqueeTest {

    private static int failures = 0;

    /** 模擬一幀的間隔：action bar 每 tick 送一次。 */
    private static final long TICK = 50;

    /** 字停下來之後再收幾幀同樣的 action bar。要比 SETTLE_FRAMES 多。 */
    private static final int STILL_FRAMES = 12;

    private static final TextColor BODY = TextColor.fromRgb(0xe0d4c0);
    private static final TextColor ITEM = TextColor.fromRgb(0x55ffff);

    private static long now = 1_000_000;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        DialogueRewriter.widthForTest = DialogueMarqueeTest::realWidth;
        DialogueRewriter.clock = () -> now;

        rhythm();
        typedThenScrolls();
        firstRowsStay();
        accentInWindow();
        fitsDoesNotScroll();
        newline();
        restartsPerLine();

        // 原文太長、遊戲自己在捲的那種（對話選項）
        choiceFollowsGameScroll();
        shortChoiceStays();
        bodyAndChoiceTogether();
        panelHoldsMidScroll();

        System.out.println(failures == 0 ? "\n對話跑馬燈：全部通過"
                : "\n對話跑馬燈：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------

    /** 一輪的節奏，直接問 {@link Marquee#window}。 */
    private static void rhythm() {
        String row = "一二三四五六七八九十甲乙丙丁戊己庚辛壬癸";   // 20 字 = 200px
        int room = 100;
        int over = realWidth(row) - room;
        long scroll = over * 1000L / Marquee.SPEED;
        long cycle = Marquee.HOLD_START + scroll + Marquee.HOLD_END;

        int[] first = Marquee.window(row, room, DialogueMarqueeTest::realWidth, 0);
        check("一開始停在開頭（實際從第 " + first[0] + " 字）", first[0] == 0);
        int[] held = Marquee.window(row, room, DialogueMarqueeTest::realWidth,
                Marquee.HOLD_START - 1);
        check("開頭停滿 HOLD_START 才捲", held[0] == 0);

        int last = 0;
        boolean smooth = true;
        boolean fits = true;
        boolean[] seen = new boolean[row.length()];
        for (long t = 0; t < cycle; t += TICK) {
            int[] w = Marquee.window(row, room, DialogueMarqueeTest::realWidth, t);
            if (w[0] < last || w[0] - last > 1) {
                smooth = false;            // 往回跳，或一幀跳了不只一個字
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

        int[] end = Marquee.window(row, room, DialogueMarqueeTest::realWidth,
                Marquee.HOLD_START + scroll);
        check("捲到結尾（實際迄於 " + end[1] + "／" + row.length() + "）",
              end[1] == row.length());
        int[] endHeld = Marquee.window(row, room, DialogueMarqueeTest::realWidth, cycle - 1);
        check("結尾停滿 HOLD_END", endHeld[1] == row.length());
        int[] again = Marquee.window(row, room, DialogueMarqueeTest::realWidth, cycle);
        check("一輪之後跳回開頭重來", again[0] == 0);
    }

    /**
     * 逐字打一句塞不下的台詞，再收幾幀停住的 action bar——每一幀都要是譯文。
     *
     * <p>這一條在改之前會紅：打完那一幀 {@code line} 回 {@code null}（原文冒出來），
     * 而且旗標立起來讓小框接手。
     */
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

        // 字停下來之後再放幾秒，看那一列有沒有在框裡捲
        String full = dst;
        boolean fits = true;
        boolean newline = false;
        int moved = 0;
        String before = null;
        StringBuilder union = new StringBuilder();
        for (int tick = 0; tick < 200; tick++) {
            now += TICK;
            String again = DialogueRewriter.line(src, store, 1, null, room);
            DialogueRewriter.Layout laid = again == null ? null
                    : DialogueRewriter.layout(again, 1, null, room, now);
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

    /** 兩列的框：第一列照常、不動，只有第二列在捲。 */
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
            DialogueRewriter.Layout laid = DialogueRewriter.layout(text, 2, null, room, now);
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

    /** 名字捲到一半：框裡那半截照樣是強調色。 */
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
            DialogueRewriter.Layout laid = DialogueRewriter.layout(text, 1, null, room, now);
            if (laid == null) {
                check("塞不下的那一句排得出跑馬燈", false);
                return;
            }
            int[] starts = new int[laid.rows().size()];
            List<LineParts.Piece> inked = DialogueRewriter.paint(
                    DialogueRewriter.flatten(laid.rows(), starts),
                    Style.EMPTY.withColor(BODY), tint, new boolean[tint.size()]);
            List<LineParts.Piece> row = DialogueRewriter.cutRow(laid, inked, starts, 0);
            StringBuilder joined = new StringBuilder();
            for (LineParts.Piece piece : row) {
                joined.append(piece.text());
            }
            text_ok &= joined.toString().equals(laid.shown(0));
            // 視窗裡屬於名字的那幾個字都要是青色
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
                half = true;               // 確實測到了「只剩半截」的那幾幀
            }
        }
        check("畫出去的幾截接起來就是那一幀的視窗", text_ok);
        check("測得到名字被框邊切開的那幾幀", half);
        check("★ 名字捲到一半，框裡那半截仍是強調色", coloured);
    }

    /** 塞得下的句子不捲，跟先前一模一樣。 */
    private static void fitsDoesNotScroll() {
        String text = "老燈塔看守人說暴風雨會過去。";
        DialogueRewriter.Layout laid = DialogueRewriter.layout(text, 1, null, 232, now);
        check("塞得下就不捲", laid != null && !laid.scrolling()
                && laid.shown(0).equals(DialogueRewriter.wrap(text, 1, null, 232).get(0)));
    }

    /** 譯文帶 \n：攤平（#1301）之後太長的照樣捲，列裡沒有 \n。 */
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
            String again = DialogueRewriter.line(src, store, 1, null, room);
            DialogueRewriter.Layout laid = again == null ? null
                    : DialogueRewriter.layout(again, 1, null, room, now);
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

    /** 換了一句就從開頭重來，不接著上一句的進度。 */
    private static void restartsPerLine() {
        String a = "一二三四五六七八九十甲乙丙丁戊己庚辛壬癸";
        String b = "子丑寅卯辰巳午未申酉戌亥一二三四五六七八";
        for (int i = 0; i < 100; i++) {
            now += TICK;
            DialogueRewriter.layout(a, 1, null, 100, now);
        }
        now += TICK;
        DialogueRewriter.Layout laid = DialogueRewriter.layout(b, 1, null, 100, now);
        check("換一句就從開頭開始", laid != null && laid.offset(0) == 0);
    }

    // ------------------------------------------------------------------
    // 原文太長：遊戲自己讓選項跑馬燈，每個 tick 只送 28 個字元的一格
    // ------------------------------------------------------------------

    /** 實機回報（2026-09-18）的那一個選項，與遊戲一格的字數。 */
    private static final String LONG_CHOICE = "Do you remember anything from before you joined?";
    private static final int WINDOW = 28;

    /**
     * 遊戲送來的格子：停在第一格幾個 tick、一個 tick 往左一格、捲到底停一下、
     * 跳回第一格再來一輪。照 {@code MarqueeTest} 的切法（頭尾的空白會被 strip）。
     */
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

    /**
     * 照 {@code DialogueRewriter#rewrite} 選項那一段的做法跑一格：先查表，
     * 查不到就問 {@link Marquee#follow}，再決定這一幀畫譯文的哪一段。
     *
     * @return 這一幀選項列畫出來的字；留英文時回 {@code null}
     */
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
        int[] cut = DialogueRewriter.choiceWindow(row, pick, realWidth(window),
                DialogueMarqueeTest::realWidth, now);
        return pick.substring(cut[0], cut[1]);
    }

    /**
     * 原文在捲，譯文也要在框裡捲：每一格都是譯文、每一格都放得進框、整句都看得到。
     *
     * <p>先前選項列貼的是<b>整句</b>譯文，不管它比框寬多少——中文一長就穿出選項框。
     */
    private static void choiceFollowsGameScroll() throws Exception {
        String first = LONG_CHOICE.substring(0, WINDOW).strip();
        String dst = "你還記得自己加入我們之前發生過的任何事情嗎？一點點也好。";
        TranslationStore store = miniStore(first, dst);
        Marquee.clear();
        int room = Marquee.choiceRoom(0, DialogueMarqueeTest::realWidth);
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

    /** 原文很短的選項：譯文比原文長一點但放得進框，不捲。 */
    private static void shortChoiceStays() {
        String pick = "……你在說什麼？";
        int[] cut = DialogueRewriter.choiceWindow("choice_3", pick, realWidth("...What?"),
                DialogueMarqueeTest::realWidth, now);
        check("短選項放得下就不捲", cut[0] == 0 && cut[1] == pick.length());
    }

    /**
     * 內文最後一列與選項同時在捲：兩邊各有各的時鐘。
     *
     * <p>共用一個時鐘的話，每一幀都會被另一列當成「換了一列」歸零，誰都捲不動。
     */
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
            DialogueRewriter.Layout laid = DialogueRewriter.layout(body, 1, null, 100, now);
            int[] cut = DialogueRewriter.choiceWindow("choice_0", pick, 160,
                    DialogueMarqueeTest::realWidth, now);
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

    /**
     * 小框那條路：捲到大寫開頭的那一格不能被當成一組新選項重翻。
     *
     * <p>{@code DialogueOverlay#scrolling} 只認小寫開頭；「Peloros work hard…」那種
     * 大寫開頭的中段視窗會漏過去，查不到就把那一列換成英文，下一格又變回中文。
     */
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

    // ------------------------------------------------------------------

    /** 一個字一個字餵進 {@link DialogueRewriter#line}，打完再收幾幀停住的。 */
    private static List<String> typeOut(String src, TranslationStore store, int rows, int room) {
        DialogueRewriter.forget();
        Marquee.clear();
        List<String> frames = new ArrayList<>();
        for (int at = 1; at <= src.length(); at++) {
            now += TICK;
            frames.add(DialogueRewriter.line(src.substring(0, at), store, rows, null, room));
        }
        for (int again = 0; again < STILL_FRAMES; again++) {
            now += TICK;
            frames.add(DialogueRewriter.line(src, store, rows, null, room));
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

    /** 中日韓一個字 10px，其餘照 Wynncraft 的對話字型算 6px。 */
    private static int realWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += c >= 0x1100 && c <= 0xFFDC ? 10 : 6;
        }
        return w;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
