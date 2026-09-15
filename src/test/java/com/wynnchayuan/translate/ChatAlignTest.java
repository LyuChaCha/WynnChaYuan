package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天訊息的譯文要跟原文那一塊<b>形狀一樣</b>。
 *
 * <h2>先前壞在哪</h2>
 * 整條聊天路徑寫死「這一行是置中的」。tooltip 那邊這樣講得通——一份 tooltip
 * 的置中是相對於它自己最寬那行算的；聊天不是，Wynncraft 是照<b>聊天視窗</b>
 * 的固定寬度排的，同一塊裡有置中的標題，也有靠左的獎勵清單。
 *
 * <p>結果靠左的每一行都被往右推了半個寬度差。實機那張「任務完成」的圖裡，
 * 譯文的「獎勵:」比原文的「Rewards:」右邊一截，就是這樣來的。
 *
 * <h2>這裡釘住什麼</h2>
 * 靠左的行，譯文的縮排要<b>剛好等於</b>原文那一行的縮排——不多不少。
 * 中文比英文短，但短的是右邊，左緣不該動。
 *
 * <p>量的是排版偏移字元解出來的像素值，那個不需要字型（見 {@link SpaceOffset}），
 * headless 也量得準。真空白字元的寬度要有字型才量得出來，所以那一種
 * （「[Cave Completed]」那塊）這裡測不到，只能靠實機看。
 */
public final class ChatAlignTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;
    private static final int PINK = 0xFF55FF;
    private static final int GREY = 0xAAAAAA;

    /** 三行都各有自己的前導偏移，數字照英文寬度算好。 */
    private static final int[] LEADS = {96, 40, 8};

    /**
     * 實機「任務完成」那一塊的骨架。
     *
     * <p>縮排是 {@code minecraft:space} 的偏移字元，在語料的模板裡就是行首那個
     * {@code {#}}——重建時原樣填回去，所以譯文一開始的縮排跟原文一樣。
     */
    private static StyledText quest() {
        MutableComponent all = Component.empty();
        all.append(offset(LEADS[0]));
        all.append(lit("[Quest Completed]", GOLD));
        all.append(lit("\n", GREY));
        all.append(offset(LEADS[1]));
        all.append(lit("Rewards:", PINK));
        all.append(lit("\n", GREY));
        all.append(offset(LEADS[2]));
        all.append(lit("- +Access to the Province of Wynn", GREY));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-chat-align");
        FlowedDebug.init(dir);

        String src = "{#}[Quest Completed]\n{#}Rewards:\n{#}- +Access to the Province of Wynn";
        String dst = "{#}[任務完成]\n{#}獎勵:\n{#}- +取得進入 Wynn 行省的資格";
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty(src, dst);
        Files.writeString(dir.resolve("quest.json"), root.toString(), StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Component hit = LineTranslator.translateChat(quest(), store);
        check("整塊查得到譯文", hit != null);
        if (hit == null) {
            report();
            return;
        }
        System.out.println("      輸出：" + hit.getString().replace("\n", " ⏎ "));
        check("三行都還在", rows(hit) == 3);
        check("內容有翻出來", hit.getString().contains("任務完成")
                && hit.getString().contains("獎勵"));

        int[] made = leads(hit);
        for (int i = 0; i < LEADS.length && i < made.length; i++) {
            check("第 " + i + " 行的縮排跟原文一樣（原文 " + LEADS[i]
                          + "、譯文 " + made[i] + "）", made[i] == LEADS[i]);
        }
        check("量到三行的縮排（實際 " + made.length + " 行）", made.length == 3);

        // 原文開頭多一行「只有空白」時，行數仍然要對得起來。
        //
        // 語料的鍵是 strip() 過的，只有空白的首行早就被去掉了，譯文不可能有那一行。
        // 先前 solidRows 只認「完全沒有片段」的行，於是實機的「洞穴完成」永遠是
        // 「原文 7 行、譯文 6 行」對不上，整塊排版原樣返回——診斷檔的
        // 「聊天對齊 11」寫得清清楚楚。
        MutableComponent padded = Component.empty();
        padded.append(lit("  ", GREY));            // 只有空白的第一行
        padded.append(lit("\n", GREY));
        padded.append(quest().getComponent().copy());
        padded.append(lit("\n", GREY));           // 結尾的空行
        Component trimmed = LineTranslator.translateChat(
                StyledText.fromComponent(padded), store);
        check("原文首行只有空白時仍然翻得出來", trimmed != null);
        if (trimmed != null) {
            int[] kept = leads(trimmed);
            check("三行的縮排仍然跟原文一樣（實際 "
                          + java.util.Arrays.toString(kept) + "）",
                  kept.length >= 3 && kept[0] == LEADS[0]
                          && kept[1] == LEADS[1] && kept[2] == LEADS[2]);
        }

        // 明說「這一行是置中的」時才重新置中。行數與內容不能因此走樣。
        Component centred = LineTranslator.translateChat(quest(), store, Boolean.TRUE);
        check("指定置中時仍翻得出來", centred != null);
        check("指定置中時行數不變", centred == null || rows(centred) == 3);

        columns();
        panelSingles();
        vibrantColumns();
        try {
            lootrunSummary();
        } finally {
            LineTranslator.measureForTest = null;
        }
        report();
    }

    private static final int AQUA = 0x55FFFF;

    /** 英文那兩欄的中心。量自 issue #719 的截圖（GUI 縮放 2）：左欄約 81、右欄約 234。 */
    private static final int LEFT_CENTRE = 81;
    private static final int RIGHT_CENTRE = 234;

    /** 原版聊天字型的字寬，夠用來比較「原文 vs 譯文」就好。 */
    private static int advance(int cp) {
        if (cp >= 0x2E80) {
            return 9;
        }
        return switch (cp) {
            case 'i', '.', ':', ',', ';', '!', '\'' -> 2;
            case 'l' -> 3;
            case 't', 'I', ' ' -> 4;
            case 'f', 'k' -> 5;
            default -> 6;
        };
    }

    private static int measure(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                w[0] += SpaceOffset.decode(text);
            } else {
                text.codePoints().forEach(cp -> w[0] += cp == '\n' ? 0 : advance(cp));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }

    /**
     * Lootrun 結算的一行：左欄「數字 + 名稱」、右欄「標籤 + 數字」，兩欄各自置中。
     * 形狀照 Wynntils LootrunModel 的正則：{@code §.(\d+)§7 Reward Rerolls§r}、
     * {@code §7Mobs Killed: §.(\d+)}。
     */
    private static StyledText lootrunRow(String num, String rest, String label, String value) {
        int left = measure(Component.literal(num + rest));
        int right = measure(Component.literal(label + value));
        int lead = LEFT_CENTRE - left / 2;
        int gap = RIGHT_CENTRE - right / 2 - (lead + left);
        MutableComponent all = Component.empty();
        all.append(offset(lead));
        all.append(lit(num, AQUA));
        all.append(lit(rest, GREY));
        all.append(offset(gap));
        all.append(lit(label, GREY));
        all.append(lit(value, AQUA));
        return StyledText.fromComponent(all);
    }

    /** 一行裡被偏移隔開的每一欄：{起點, 終點}。 */
    private static List<int[]> cells(Component line) {
        List<int[]> out = new ArrayList<>();
        int x = 0;
        int[] open = null;
        for (Component part : flatten(line)) {
            if (!part.getSiblings().isEmpty() || part.getString().isEmpty()) {
                continue;
            }
            String text = part.getString();
            if (SpaceOffset.isSpaceFont(part.getStyle()) && SpaceOffset.isOffsetRun(text)) {
                x += SpaceOffset.decode(text);
                open = null;
                continue;
            }
            int w = measure(part);
            if (open == null) {
                open = new int[] {x, x + w};
                out.add(open);
            } else {
                open[1] = x + w;
            }
            x += w;
        }
        return out;
    }

    /**
     * issue #719：Lootrun 結算面板，兩欄一行，四行。
     *
     * <h2>回報的畫面</h2>
     * 「3 次奖励重抽｜击杀怪物数: 104」的右欄比上下幾行右偏一截；
     * 「300 点 Lootrun｜经验」整行太寬，「经验」被聊天折到下一行最左邊。
     *
     * <p>0.1.9_3 的就地取代模式走的是 tooltip 那一支：整行置中補一次、欄距再補
     * 一次、右緣又補一次。拿截圖量到的位置去套那套算式，兩行的左緣（58、63）與
     * 右緣（293、336——超過聊天的 320）都對得上，所以病因就是那一支。
     * 現在就地取代先走聊天專用的 {@code translateChat}（每欄守自己的中心），
     * 這裡把整行從查表量到底，確保那兩種症狀不會回來。
     *
     * <h2>釘住什麼</h2>
     * 不管整行翻好、只翻一欄、還是退回通用那一支（語料只有片段譯文時）：
     * <ul>
     *   <li>每一欄的中心跟英文那幾行在同一個位置（上下對得齊）；</li>
     *   <li>整行不比原文寬（不會被聊天折斷）。譯文比英文寬的語言（俄文）
     *       寧可右欄往左靠，也不能超出去，見 {@code LineTranslator#fitWidth}。</li>
     * </ul>
     *
     * <p>「{#}{~} Reward Pulls{#}Time Elapsed: {~}:{~}」那一行在裝了 WynnMod 時
     * 整行留英文（「Time Elapsed:」是它在解的字，見 ThirdPartyLiterals），
     * 這裡量的是沒裝時的樣子。
     */
    private static void lootrunSummary() {
        LineTranslator.measureForTest = ChatAlignTest::measure;
        String[][] rows = {
            {"46", " Reward Pulls", "Time Elapsed: ", "05:00"},
            {"3", " Reward Rerolls", "Mobs Killed: ", "104"},
            {"0", " Reward Sacrifices", "Chests Open: ", "5"},
            {"300", " Lootrun Experience", "Challenges Completed: ", "5"},
        };
        for (String lang : new String[] {"zh_cn", "zh_tw", "ru_ru", "ja_jp", "partial"}) {
            TranslationStore store = new TranslationStore();
            if (lang.equals("partial")) {
                // 兩欄都只有「片段」譯文、沒有整行也沒有單欄條目：退回通用那一支時的樣子
                try {
                    Path dir = Files.createTempDirectory("wynnchayuan-lootrun-partial");
                    Files.writeString(dir.resolve("gui.json"),
                            "{\"Reward Sacrifices\": \"奖励舍弃\", \"Chests Open:\": \"开启宝箱数:\","
                            + " \"Mobs Killed:\": \"击杀怪物数:\", \"Reward Rerolls\": \"奖励重抽\"}",
                            StandardCharsets.UTF_8);
                    store.loadAll(dir);
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
            }
            for (String[] r : rows) {
                StyledText orig = lootrunRow(r[0], r[1], r[2], r[3]);
                Component made = LineTranslator.translateChat(orig, store);
                Component fallback = made != null ? null : LineTranslator.translate(orig, store);
                Component shown = made != null ? made : fallback;
                // 就地取代的順序：先聊天那一支，查不到才退回通用的（見 ChatListener）
                if (shown != null) {
                    String what = "[" + lang + "] " + r[1].strip();
                    List<int[]> was = cells(orig.getComponent());
                    List<int[]> now = cells(shown);
                    check(what + "：還是兩欄（實際 " + now.size() + "）", now.size() == 2);
                    if (now.size() == 2) {
                        // 俄文的右欄比英文寬，收寬之後右欄的中心會往左移，只驗左欄
                        boolean wider = now.get(1)[1] - now.get(1)[0] > was.get(1)[1] - was.get(1)[0];
                        check(what + "：左欄中心跟英文一樣（" + describe(now) + "）",
                              Math.abs(centre(now.get(0)) - LEFT_CENTRE) <= 1);
                        check(what + "：右欄中心跟英文一樣（" + describe(now) + "）",
                              wider || Math.abs(centre(now.get(1)) - RIGHT_CENTRE) <= 1);
                        check(what + "：兩欄沒有黏在一起（" + describe(now) + "）",
                              now.get(1)[0] - now.get(0)[1] >= 4);
                    }
                    check(what + "：★ 整行不比原文寬（原文 " + measure(orig.getComponent())
                                  + "、譯文 " + measure(shown) + "）",
                          measure(shown) <= measure(orig.getComponent()));
                }
                if (lang.equals("zh_cn") && made != null) {
                    // 雙語模式是整塊攢起來、帶著「面板」旗標一行一行重譯的（見 ChatBlock）
                    Component stacked = LineTranslator.translateChat(orig, store, Boolean.FALSE, true);
                    check("[zh_cn 整塊] " + r[1].strip() + "：欄位置跟單則一樣",
                          stacked != null && describe(cells(stacked)).equals(describe(cells(made))));
                }
                System.out.println("      [" + lang + "] " + r[1].strip() + " 原文欄 "
                        + describe(cells(orig.getComponent())) + " 寬 "
                        + measure(orig.getComponent())
                        + (shown == null ? "  （沒翻）"
                           : "  " + (made != null ? "chat" : "tooltip") + " 譯文欄 "
                             + describe(cells(shown)) + " 寬 " + measure(shown)
                             + " 「" + shown.getString().replaceAll("[\\x{CF000}-\\x{D1000}]", "|") + "」"));
            }
        }
    }

    private static int centre(int[] cell) {
        return (cell[0] + cell[1]) / 2;
    }

    private static String describe(List<int[]> cells) {
        StringBuilder sb = new StringBuilder();
        for (int[] c : cells) {
            sb.append('[').append(c[0]).append('-').append(c[1])
              .append(" 中心 ").append((c[0] + c[1]) / 2).append(']');
        }
        return sb.toString();
    }

    /**
     * 璀璨信標的名稱列：欄界偏移小於 8px 也要認得出來。
     *
     * <p>偏移取自實機 log：兩欄中心固定，名稱越長偏移越小，
     * {@code <+0>Vibrant Dark Grey Beacon<+7>Vibrant Rainbow Beacon}。
     * 先前 {@code MIN_GAP_PX} 把這些都當成「不是欄界」，整行沒對齊。
     */
    private static void vibrantColumns() {
        java.util.Map<String, Integer> px = java.util.Map.of(
                "Vibrant Dark Grey Beacon", 155, "Vibrant Rainbow Beacon", 140,
                "Vibrant Crimson Beacon", 150, "Vibrant Aqua Beacon", 128,
                "璀璨深灰信標", 57, "璀璨彩虹信標", 57, "璀璨緋紅信標", 57, "璀璨水藍信標", 57,
                "- +1 ", 22, "🔒Unidentified Helmet", 120);
        java.util.function.ToIntFunction<LineTranslator.Run> width =
                r -> r.space() ? 0 : px.getOrDefault(r.text(), 0);

        List<LineTranslator.Run> darkOrig = List.of(gap(0), word("Vibrant Dark Grey Beacon"),
                                                    gap(7), word("Vibrant Rainbow Beacon"));
        List<LineTranslator.Run> darkMade = List.of(gap(0), word("璀璨深灰信標"),
                                                    gap(7), word("璀璨彩虹信標"));
        centresKept("縮排 0、欄距 7", darkOrig, darkMade, width);

        List<LineTranslator.Run> crimsonOrig = List.of(gap(7), word("Vibrant Crimson Beacon"),
                                                       gap(23), word("Vibrant Aqua Beacon"));
        List<LineTranslator.Run> crimsonMade = List.of(gap(7), word("璀璨緋紅信標"),
                                                       gap(23), word("璀璨水藍信標"));
        centresKept("縮排 7、欄距 23", crimsonOrig, crimsonMade, width);

        // 反例：圖示前的 2px 微調仍然不算欄界。
        List<LineTranslator.Run> helmet = List.of(word("- +1 "), gap(2),
                                                  word("🔒Unidentified Helmet"));
        int gaps = 0;
        for (boolean g : LineTranslator.chatGaps(helmet)) {
            gaps += g ? 1 : 0;
        }
        check("圖示前的 2px 微調不算欄界（實際 " + gaps + " 個）", gaps == 0);
    }

    private static void centresKept(String what, List<LineTranslator.Run> orig,
                                    List<LineTranslator.Run> made,
                                    java.util.function.ToIntFunction<LineTranslator.Run> width) {
        int[] adjust = LineTranslator.chatColumnPad(orig, made, width);
        check(what + "：認得兩個欄界（實際 " + adjust.length + " 個）", adjust.length == 2);
        List<Integer> before = centres(orig, new int[adjust.length], width);
        List<Integer> after = centres(made, adjust, width);
        boolean kept = before.size() == 2 && after.size() == 2;
        for (int i = 0; kept && i < 2; i++) {
            kept = Math.abs(before.get(i) - after.get(i)) <= 1;
        }
        check(what + "：兩欄的中心不動（原文 " + before + "、譯文 " + after + "）", kept);
    }

    /** 套上欄距補正之後，每一段實字的中心在哪。 */
    private static List<Integer> centres(List<LineTranslator.Run> row, int[] adjust,
                                         java.util.function.ToIntFunction<LineTranslator.Run> width) {
        boolean[] gaps = LineTranslator.chatGaps(row);
        List<Integer> out = new ArrayList<>();
        int pos = 0;
        int index = 0;
        for (int i = 0; i < row.size(); i++) {
            LineTranslator.Run r = row.get(i);
            if (r.space()) {
                pos += r.px() + (gaps[i] && index < adjust.length ? adjust[index++] : 0);
                continue;
            }
            int w = width.applyAsInt(r);
            out.add(pos + w / 2);
            pos += w;
        }
        return out;
    }

    private static LineTranslator.Run gap(int px) {
        return new LineTranslator.Run(true, px, Style.EMPTY, SpaceOffset.encode(px));
    }

    private static LineTranslator.Run word(String text) {
        return new LineTranslator.Run(false, 0, Style.EMPTY, text);
    }

    /**
     * 欄距要跟著譯文的寬度走。見 {@link LineTranslator#columnDrift}。
     *
     * <p>數字取自實機診斷檔的「聊天對齊 7」：信標選單一行是
     * {@code [縮排 33px]Orange Beacon[間隔 69px]Yellow Beacon}，兩段英文
     * 分別是 90px 與 82px，譯成「橘色信標」「黃色信標」之後各剩 40px。
     */
    private static void columns() {
        // 譯文比原文寬的時候要反過來收窄，否則右欄會被推出去。
        int[] wide = LineTranslator.columnDrift(
                List.of(0, 40), List.of(0, 64), new int[] {10, 30});
        check("譯文變寬時欄距收窄（實際 " + wide[1] + "）", wide[1] == -24);

        // 負的間隔是疊字用的，一律原樣；差額留給後面第一個正的間隔。
        int[] stacked = LineTranslator.columnDrift(
                List.of(0, 90, 20, 20), List.of(0, 40, 20, 20),
                new int[] {33, -23, 40});
        check("負間隔不動（實際 " + stacked[1] + "）", stacked[1] == 0);
        // 「把差額往後遞延」是<b>單欄</b>那條路的行為，目的是守住整行寬度。
        // 多欄改成各自置中之後要守的是每一欄的中心，不是整行寬度，所以
        // 後面的間隔只吸收它自己左右兩欄的縮水——這裡兩欄都沒縮，就是 0。
        check("多欄不遞延差額（實際 " + stacked[2] + "）", stacked[2] == 0);

        // ★ 兩欄以上：每一欄各自置中，不是把第二欄靠左貼回原位。
        //
        // 數字取自實機的信標選單：兩欄的英文各 90px 與 82px，譯成「紫色信標」
        // 「藍色信標」之後各剩 40px。原文的縮排 34px 是伺服器照英文算好、
        // 讓那一欄置中的，照抄過來字就會往左偏。
        int[] two = LineTranslator.columnDrift(
                List.of(0, 90, 82), List.of(0, 40, 40), new int[] {34, 69});
        check("第一欄吸收自己縮水的一半（實際 " + two[0] + "）", two[0] == 25);
        check("間隔吸收左右各一半（實際 " + two[1] + "）", two[1] == 46);

        // 兩欄置中之後，每一欄的中心都要留在原處——這才是「對齊」的定義。
        int[] widths = {90, 82};
        int[] made = {40, 40};
        int[] gaps = {34, 69};
        int origPos = 0;
        int madePos = 0;
        for (int i = 0; i < 2; i++) {
            origPos += gaps[i];
            madePos += gaps[i] + two[i];
            int origCentre = origPos + widths[i] / 2;
            int madeCentre = madePos + made[i] / 2;
            check("第 " + (i + 1) + " 欄的中心不動（原文 " + origCentre
                          + "、譯文 " + madeCentre + "）",
                  Math.abs(origCentre - madeCentre) <= 1);
            origPos += widths[i];
            madePos += made[i];
        }

        // 只有縮排、沒有第二欄的行完全不受影響。
        int[] one = LineTranslator.columnDrift(
                List.of(0, 107), List.of(0, 38), new int[] {101});
        check("單欄的行不動（實際 " + java.util.Arrays.toString(one) + "）",
              one.length == 1 && one[0] == 0);
    }

    /**
     * 面板裡的單欄行也要照原文的中心擺。
     *
     * <h2>先前壞在哪</h2>
     * 獵殺信標的面板裡混著兩欄的行（「白色信標｜黃色信標」）與單欄的行
     * （「本次 Lootrun」「點擊此處重抽」）。兩欄的走逐欄置中、看起來是對的；
     * 單欄的照抄原文左緣，而原文其實是<b>置中</b>的，中文短了就往左偏。
     *
     * <p>照理說 BlockLayout 會判斷置中，但它拿<b>整塊</b>最寬的一行當基準，而
     * 聊天塊常常混進不相干的訊息——實機診斷檔裡，信標面板的行都是 200～290px，
     * 卻跟一則 1439px 的歡迎訊息攢在同一塊，基準被拉走，單欄行全判成靠左。
     *
     * <p>這裡驗的是<b>算式</b>：原文的中心在哪，譯文的中心就該在哪。
     */
    private static void panelSingles() {
        // 原文：縮排 105、內容 98 → 中心 154。譯文內容 54 → 縮排該是 154-27=127。
        int origLead = 105;
        int origBody = 98;
        int madeBody = 54;
        int want = origLead + (origBody - madeBody) / 2;
        check("單欄行的中心要跟原文一樣（該縮排 " + want + "）", want == 127);

        // 兩欄的行不走這條：它們逐欄置中，見 columnDrift。
        int[] two = LineTranslator.columnDrift(
                List.of(0, 90, 82), List.of(0, 40, 40), new int[] {34, 69});
        check("兩欄的行仍然逐欄置中（實際 " + two[0] + "/" + two[1] + "）",
              two[0] == 25 && two[1] == 46);
    }

    private static int rows(Component line) {
        return (int) line.getString().chars().filter(c -> c == '\n').count() + 1;
    }

    /** 每一行開頭那段偏移字元解出來的寬度。 */
    private static int[] leads(Component line) {
        List<Integer> out = new ArrayList<>();
        int px = 0;
        boolean counting = true;
        for (Component part : flatten(line)) {
            if (!part.getSiblings().isEmpty()) {
                continue;                      // 上層節點，字會重複算
            }
            String text = part.getString();
            if (text.isEmpty()) {
                continue;
            }
            if (text.indexOf('\n') >= 0) {
                out.add(px);
                px = 0;
                counting = true;
                continue;
            }
            if (!counting) {
                continue;
            }
            if (SpaceOffset.isOffsetRun(text)) {
                px += SpaceOffset.decode(text);
            } else {
                counting = false;              // 碰到實字，這一行的縮排數完了
            }
        }
        out.add(px);
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = out.get(i);
        }
        return arr;
    }

    private static List<Component> flatten(Component root) {
        List<Component> out = new ArrayList<>();
        out.add(root);
        for (Component child : root.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("聊天對齊：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("聊天對齊：全部通過");
    }
}
