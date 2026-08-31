package com.wynnchayuan.translate;

import java.util.Random;

/**
 * 折行不能丟例外。
 *
 * <h2>為什麼要有這條測試</h2>
 * 折行的位置<b>剛好是一個空白</b>時，切點 {@code cut} 就等於當前位置 {@code i}，
 * 而 {@code lineStart} 被設成 {@code cut + 1}——比 {@code i} 還大。接下來的
 * {@code substring(lineStart, i)} 就變成 {@code substring(24, 23)}，當場丟出
 * {@code StringIndexOutOfBoundsException}。
 *
 * <p>那個例外被繪製那層的 {@code catch (Throwable)} 吞掉，結果是<b>整個翻譯面板
 * 都不畫</b>。畫面上看起來像「這件物品沒翻到」，實際上語料好好的躺在那裡。
 * 使用者回報的 Halcyon 就是這樣。
 *
 * <p>所以除了那個確切的形狀，再用亂數跑一輪：任何字串、任何寬度都不該丟例外，
 * 而且折出來的內容去掉換行之後必須跟原文一模一樣。
 */
public final class WrapTest {

    private static int failures = 0;

    /** 一個字元算一單位。真正的量法要 Minecraft 字型，測試環境裡沒有。 */
    private static int measure(String piece) {
        return piece.codePointCount(0, piece.length());
    }

    public static void main(String[] args) {
        // 使用者實際踩到的形狀：換行點正好落在空白上
        check("換行點落在空白上不會丟例外",
                () -> LineTranslator.wrapToWidth(
                        "Blinding Lights: All Ophanim orbs deal 250% damage",
                        8, WrapTest::measure));

        check("寬度只夠一個字",
                () -> LineTranslator.wrapToWidth("a b c d e f g", 1, WrapTest::measure));
        check("整串都是空白",
                () -> LineTranslator.wrapToWidth("          ", 3, WrapTest::measure));
        check("佔位符不會被拆開",
                () -> LineTranslator.wrapToWidth("{~} 格內的友軍 {~} 生命", 4, WrapTest::measure));

        // 避頭尾：標點不能自己站在行首。先前會折成
        //   「…獲得 +5 層 Crystallize」
        //   「，並改為環繞你運行。」   ← 逗號孤零零地開頭
        String orphan = LineTranslator.wrapToWidth(
                "每次命中獲得層數，並改為環繞你運行。", 8, WrapTest::measure);
        boolean starts = false;
        for (String line : orphan.split(String.valueOf(NL))) {
            if (!line.isEmpty() && "，。、；：！？）」".indexOf(line.charAt(0)) >= 0) {
                starts = true;
            }
        }
        report("標點不會被擠到行首（實際：" + show(orphan) + "）", !starts);

        // 正負號跟它的數值是同一塊，不能拆開——先前會折成「增加 +」「5 層數」
        String signed = LineTranslator.wrapToWidth("每次命中增加 +{~} 層數", 7,
                                                   WrapTest::measure);
        report("號不會跟數值分家（實際：" + show(signed) + "）",
                !signed.contains("+" + NL));

        // --- 其他語言 -------------------------------------------------
        // 折行的規則是照中文寫的（中文可以在任何字之間斷）。西、德、法、俄
        // 不行——把單字從中間切開，讀起來就是壞的。這裡釘住那條界線。
        //
        // 判準：每一個單字都塞得下時，折行只能發生在空白上。
        //
        // 比對前把連續空白收成一個。斷點<b>剛好落在空白之後</b>時，那個空白會
        // 留在行尾（看不見，無所謂）；落在別處時則被吃掉。兩種都不算錯，
        // 錯的是單字被切開——收掉空白之後，那才是唯一會讓比對失敗的情況。
        String[] latin = {
            "Erhoeht den Schaden um",          // 德
            "Aumenta el dano magico",          // 西
            "Augmente les degats de sort",     // 法
            "Увеличивает урон заклинаний",     // 俄
        };
        for (String text : latin) {
            String wrapped = LineTranslator.wrapToWidth(text, 12, WrapTest::measure);
            report("不把單字切成兩半：" + text + "（實際：" + show(wrapped) + "）",
                    tidy(wrapped.replace(NL, ' ')).equals(tidy(text)));
        }

        // 諺文跟中日文一樣，可以在音節之間斷（Unicode 的預設行為）。
        // 這裡只要求內容不變——斷在哪由寬度決定。
        String hangul = LineTranslator.wrapToWidth("마법 피해 증가량 상승", 4,
                                                   WrapTest::measure);
        report("諺文折行不會掉字", bare(hangul).equals(bare("마법 피해 증가량 상승")));

        // 一個單字本身就超過整行寬度時，只能從中間切——切壞總比整行滿出去好
        String huge = LineTranslator.wrapToWidth(
                "Bewegungsgeschwindigkeit", 8, WrapTest::measure);
        report("超長單字會被切開而不是滿出去",
                huge.indexOf(NL) > 0 && bare(huge).equals("Bewegungsgeschwindigkeit"));

        // 內容守恆：折行只能動空白，不能吃掉或改動任何一個字。
        //
        // 比對時<b>兩邊的空白全部拿掉</b>。折在空白上時那個空白會被行尾吃掉，
        // 折在中文字之間則不會——所以不能用「換行還原成空白」去比，
        // 那樣兩種情形只能對上一種。
        String text = "Allies within {~} blocks gain {~} of the health you gain";
        for (int px = 1; px <= 60; px++) {
            String wrapped = LineTranslator.wrapToWidth(text, px, WrapTest::measure);
            if (!bare(wrapped).equals(bare(text))) {
                System.out.println("  [FAIL] 寬度 " + px + " 折完內容變了：" + wrapped);
                failures++;
                break;
            }
        }
        check("內容守恆（寬度 1 到 60）", () -> "");

        // 亂數：任何字串、任何寬度都不該丟例外
        Random random = new Random(20260822L);
        String alphabet = " abcXYZ中文的字{~}";
        for (int round = 0; round < 4000; round++) {
            StringBuilder sb = new StringBuilder();
            int length = random.nextInt(60);
            for (int i = 0; i < length; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String input = sb.toString();
            int px = random.nextInt(30);
            try {
                LineTranslator.wrapToWidth(input, px, WrapTest::measure);
            } catch (RuntimeException e) {
                System.out.println("  [FAIL] 亂數第 " + round + " 輪丟例外："
                        + e + "  寬度 " + px + "  輸入 [" + input + "]");
                failures++;
                break;
            }
        }
        check("亂數四千輪都沒丟例外", () -> "");

        placeholderNotSplit();
        glyphNotAtLineEnd();
        oneLine();
        labelBreak();
        balanced();

        System.out.println(failures == 0 ? "Wrap: 全部通過" : "Wrap: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 同樣的行數要排得平均，最後一行不該只剩零頭。
     *
     * <p>使用者回報的畫面：`Pounce: Escape becomes a forward lunge.` 原文兩行，
     * 譯文也是兩行，但折成 11 字 ＋ 2 字——
     *
     * <pre>
     *   ◆ 猛撲: 逃脫變成向前突
     *   進。
     * </pre>
     *
     * <p>貪心折行把第一行塞滿，剩下的全掉到最後一行。中文每個字都能斷，
     * 沒有空白可以退，所以特別明顯。
     */
    private static void balanced() {
        System.out.println("\n  -- 同樣行數要排得平均 --");

        // 三行才輪得到平均分配：兩行的零頭已經被「放得下就一行」接走了
        //（見 oneLine），因為<b>會出現零頭就表示只差一點點放得下</b>。
        //
        // 這裡不寫死字數——寫的是那條性質：同樣的行數，最短的那一行要變長。
        String text = "每次命中使你獲得層數並改為環繞你運行造成傷害";
        String greedy = LineTranslator.wrapToWidth(text, 10, WrapTest::measure);
        String even = LineTranslator.wrapBalanced(text, 10, WrapTest::measure);
        report("這句夠長，真的折成三行以上（" + show(greedy) + "）", rowsOf(greedy).length >= 3);
        report("行數沒變（" + show(even) + "）", rowsOf(even).length == rowsOf(greedy).length);
        report("最後一行不再是零頭（" + shortest(greedy) + " → " + shortest(even) + "）",
                shortest(even) > shortest(greedy));

        // 反面：不能為了平均而把英文單字切開。
        String withWord = "每次命中使你獲得 Crystallize 層數。";
        String tight = LineTranslator.wrapBalanced(withWord, 14, WrapTest::measure);
        boolean split = false;
        for (int at = tight.indexOf(NL); at > 0 && at + 1 < tight.length();
                at = tight.indexOf(NL, at + 1)) {
            // 只看拉丁字母。中文字在 Java 裡也是 isLetter，拿它判斷會把
            // 「…得 ⏎ Crystallize」誤判成切開了單字——中文本來就可以在那裡斷。
            if (latin(tight.charAt(at - 1)) && latin(tight.charAt(at + 1))) {
                split = true;
            }
        }
        report("平均分配沒有把英文單字切開（實際：" + show(tight) + "）", !split);

        // 內容守恆：平均只重排，不能吃字。
        report("字沒有變少", bare(tight).equals(bare(withWord)));
    }

    /**
     * 只差一點就放得下的，讓它留在同一行。
     *
     * <p>Major ID 大多是「名稱: 一句話」，中文比英文緊湊，原文兩行的往往一句
     * 就講完。硬折回兩行只是把一句完整的話剪成兩截——而譯文面板是我們自己畫的，
     * 寬度本來就跟著內容長，不必為了對齊原文的形狀犧牲可讀性。
     *
     * <p>只放寬到 {@code SNUG}（125%）。再寬下去，一句話就能把整份 tooltip
     * 撐得莫名其妙，所以也釘住反面：差太多的還是要折。
     */
    private static void oneLine() {
        System.out.println("\n  -- 放得下就留在同一行 --");

        // measure 數的是「幾個字」。這句 13 個字，寬 11——11 × 125% = 13，剛好放得下。
        String text = "猛撲: 逃脫變成向前突進。";
        String fits = LineTranslator.wrapBalanced(text, 11, WrapTest::measure);
        report("沒有斷行（實際：" + show(fits) + "）", fits.indexOf(NL) < 0);
        report("字沒有變少", bare(fits).equals(bare(text)));

        // 反面：寬 8 的話 8 × 125% = 10，13 個字差太多，該折就得折。
        String tight = LineTranslator.wrapBalanced(text, 8, WrapTest::measure);
        report("差太多的還是要折（實際：" + show(tight) + "）", tight.indexOf(NL) > 0);
    }

    /**
     * 放不下的時候，斷在名稱後面。
     *
     * <p>「{@code 猛撲:}」換行「{@code 逃脫變成向前突進在此}」——斷在冒號這個
     * <b>語意</b>的接縫上，比斷在句子中間好讀。
     *
     * <p>但只在<b>不會多出一行</b>的時候：說明長到要三四行的，名稱獨佔一行
     * 就是白白浪費一行，那還不如平均分配。反面也釘住。
     */
    private static void labelBreak() {
        System.out.println("\n  -- 斷在名稱後面 --");

        // 14 個字、寬 11：11 × 125% = 13 放不下，但名稱後面斷剛好兩行。
        String text = "猛撲: 逃脫變成向前突進在此";
        String out = LineTranslator.wrapBalanced(text, 11, WrapTest::measure);
        String[] rows = rowsOf(out);
        report("折成兩行（實際：" + show(out) + "）", rows.length == 2);
        if (rows.length == 2) {
            report("第一行就是名稱那半", rows[0].equals("猛撲:"));
            report("說明整句在第二行", rows[1].equals("逃脫變成向前突進在此"));
        }
        report("字沒有變少", bare(out).equals(bare(text)));

        // 說明本來就要兩行的話，名稱再獨佔一行就變三行——比貪心多一行，
        // 那是白白浪費一行，不該這樣折。
        //
        // 注意<b>不是</b>「說明長就不斷」：貪心也要三行的時候，斷在名稱後面
        // 同樣是三行，沒有多花，那就照斷（下面第二段）。判準是行數，不是長度。
        String longer = "猛撲: 逃脫變成向前突進並造成傷害";
        String[] wide = rowsOf(LineTranslator.wrapBalanced(longer, 11, WrapTest::measure));
        report("貪心兩行就夠時，不為了斷在名稱後而多花一行（實際："
                + show(String.join(String.valueOf(NL), wide)) + "）",
                wide.length == 2 && !wide[0].equals("猛撲:"));

        // 貪心本來就要三行的，斷在名稱後面也是三行——沒有多花，那就照斷。
        String longest = "猛撲: 逃脫變成向前突進並且造成大量範圍傷害。";
        String[] three = rowsOf(LineTranslator.wrapBalanced(longest, 11, WrapTest::measure));
        report("行數一樣就照斷（實際："
                + show(String.join(String.valueOf(NL), three)) + "）",
                three.length == 3 && three[0].equals("猛撲:"));
        report("剩下那半也排得平均",
                three.length == 3 && Math.abs(three[1].length() - three[2].length()) <= 2);
    }

    /**
     * 圖示不能落在行尾。
     *
     * <p>「{@code 使用 {#} 物品鑑定師}」的 {@code {#}} 是物品鑑定師的圖示，
     * 是它的前綴。使用者回報的畫面是斷在兩者中間——
     *
     * <pre>
     *   此物品的力量已被封印，使用 ◉
     *   物品鑑定師 即可解放其潛能。
     * </pre>
     *
     * <p>這裡不挑特定寬度，而是<b>掃過所有會折行的寬度</b>：只要有一個寬度
     * 讓圖示落在行尾就算失敗。挑一個寬度測的話，換個字數就漏掉了。
     */
    private static void glyphNotAtLineEnd() {
        System.out.println("\n  -- 圖示不能落在行尾 --");

        String text = "此物品的力量已被封印，使用 {#} 物品鑑定師 即可解放其潛能。";
        int bad = 0;
        String worst = null;
        for (int px = 4; px <= 40; px++) {
            String wrapped = LineTranslator.wrapToWidth(text, px, WrapTest::measure);
            for (String row : wrapped.split(String.valueOf(NL), -1)) {
                // strip 過再比。行尾如果是「{#} 」（圖示加一個空白），
                // 直接 endsWith 抓不到——先前的測試就是這樣漏掉真正的形狀的。
                //
                // 整行<b>只有</b>圖示不算失敗：那是寬度窄到放不下「圖示＋一個字」，
                // 沒有東西可以跟它走，往前挪只會挪出一個空行。
                String core = row.stripTrailing();
                if (core.endsWith("{#}") && !core.strip().equals("{#}")) {
                    bad++;
                    if (worst == null) {
                        worst = "寬度 " + px + "：" + show(wrapped);
                    }
                }
            }
        }
        report("沒有任何寬度讓圖示落在行尾"
                + (worst == null ? "" : "（例如 " + worst + "）"), bad == 0);

        // 內容守恆：避尾只是挪斷點，不能吃字
        String once = LineTranslator.wrapToWidth(text, 12, WrapTest::measure);
        report("字沒有變少", bare(once).equals(bare(text)));
    }

    /**
     * 佔位符不能被斷成兩半。
     *
     * <p>使用者回報的 Major ID：
     *
     * <pre>
     *   為你恢復 15%、為友軍恢復 {~2
     *   }，但衝鋒不再造成傷害。
     * </pre>
     *
     * <p>折行本身是照「片段」走的，{@code {~2}} 會被當成一整塊；但避頭
     * （標點不能在行首）是<b>逐字元</b>往回退的——斷點原本落在
     * {@code ，}，退一格就退進了佔位符裡面。
     *
     * <p>而少了右括號就不再是佔位符，填值那一步認不得它，於是畫面上
     * 直接露出 {@code {~2}} 這串字。<b>一個原因，兩個症狀。</b>
     */
    private static void placeholderNotSplit() {
        System.out.println("\n  -- 佔位符不能被斷成兩半 --");

        // 使用者回報的那一句。逗號緊接在佔位符後面，正是會誘發避頭往回退的形狀。
        String text = "為你恢復 {~1}、為友軍恢復 {~2}，但衝鋒不再造成傷害。";
        int bad = 0;
        String worst = null;
        for (int px = 4; px <= 40; px++) {
            String wrapped = LineTranslator.wrapToWidth(text, px, WrapTest::measure);
            for (String row : rowsOf(wrapped)) {
                int open = 0;
                for (int i = 0; i < row.length(); i++) {
                    if (row.charAt(i) == '{') {
                        open++;
                    } else if (row.charAt(i) == '}') {
                        open--;
                    }
                }
                if (open != 0) {
                    bad++;
                    if (worst == null) {
                        worst = "寬度 " + px + "：" + show(wrapped);
                    }
                }
            }
        }
        report("沒有任何寬度把佔位符斷成兩半"
                + (worst == null ? "" : "（例如 " + worst + "）"), bad == 0);

        String once = LineTranslator.wrapToWidth(text, 14, WrapTest::measure);
        report("字沒有變少", bare(once).equals(bare(text)));
    }

    /** 折行結果拆成幾行。 */
    private static String[] rowsOf(String wrapped) {
        return wrapped.split(String.valueOf(NL), -1);
    }

    /** 最短的那一行有幾個字。零頭就是它很小。 */
    private static int shortest(String wrapped) {
        int least = Integer.MAX_VALUE;
        for (String row : rowsOf(wrapped)) {
            least = Math.min(least, row.length());
        }
        return least;
    }

    /** 是不是拉丁字母。中文不算——它本來就可以在任何字之間斷。 */
    private static boolean latin(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /** 連續空白收成一個，首尾去掉。比較折行結果時用。 */
    private static String tidy(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }

    /** 拿掉所有空白，只留下字本身。 */
    private static String bare(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().filter(c -> !Character.isWhitespace(c)).forEach(sb::appendCodePoint);
        return sb.toString();
    }

    private interface Body {
        String run();
    }

    /** 折行用的換行字元。 */
    private static final char NL = '\n';

    /** 把換行換成看得見的符號，失敗訊息才讀得出來折在哪。 */
    private static String show(String wrapped) {
        return wrapped.replace(String.valueOf(NL), " ⏎ ");
    }

    /** 直接斷言一個條件（{@link #check} 只驗「有沒有丟例外」）。 */
    private static void report(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void check(String what, Body body) {
        try {
            body.run();
            System.out.println("  [PASS] " + what);
        } catch (RuntimeException e) {
            System.out.println("  [FAIL] " + what + " —— " + e);
            failures++;
        }
    }
}
