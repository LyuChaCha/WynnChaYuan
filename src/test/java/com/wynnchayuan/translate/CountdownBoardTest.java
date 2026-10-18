package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 商店的倒數看板一列一列組（見 {@link LineTranslator#countdownBoard}）。
 *
 * <h2>先前是什麼樣子</h2>
 * 寶箱、股份、階級、炸彈的優惠牌子，倒數格式隨剩餘時間變（天時分秒的任意組合），
 * 附加的幾列隨活動變。語料一種組合一條整塊的鍵，capture 一次又收了三十六種沒見過的。
 * 就地取代那一支會退到逐行，可是倒數列原樣留英文；注視時才顯示的小框那一支整塊放棄。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>沒收過的組合：每一列照自己的鍵翻，倒數列照漂浮字專用的時間格式，數值各就各位；</li>
 *   <li>兩支都認得（就地取代、注視時的小框），每一列的顏色照原文那一列；</li>
 *   <li>整塊的鍵照樣優先；有一列查不到就整塊不動；沒有倒數列的牌子不碰；</li>
 *   <li>收集端問「翻了沒」時這種組合算翻了；</li>
 *   <li>七個語言的實際語料：capture 裡那幾種組合都翻得出來，不剩英文。</li>
 * </ol>
 *
 * <p>語料是測試自己寫的，措辭跟實際語料無關；實際語料那一段只看結構（沒有英文、數值都在）。
 */
public final class CountdownBoardTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;
    private static final int WHITE = 0xFFFFFF;
    private static final int GREEN = 0x55FF55;
    private static final int GREY = 0xAAAAAA;
    private static final int AQUA = 0x55FFFF;
    private static final int YELLOW = 0xFFFF55;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-countdown-board");
        FlowedDebug.init(dir);
        TranslationStore store = corpus(dir.resolve("corpus"));

        composes(store);
        guards(store);
        realCorpus();
        report();
    }

    private static TranslationStore corpus(Path dir) throws Exception {
        Files.createDirectories(dir.resolve("scoped"));
        JsonObject misc = new JsonObject();
        misc.addProperty("Crate Offer", "測寶箱特惠");
        misc.addProperty("{~} OFF Crates", "測寶箱打 {~} 折");
        misc.addProperty("({~}x Regular and higher)", "(測 {~}x 普通起)");
        misc.addProperty("Extra Bonus:", "測加碼:");
        misc.addProperty("Get VIP Rank FREE", "測送 VIP");
        misc.addProperty("Bomb Offer", "測炸彈特惠");
        misc.addProperty("{~} OFF Bombs", "測炸彈打 {~} 折");
        // 整塊的舊鍵：照樣優先
        misc.addProperty("Bomb Offer\n{~}h {~}m\n\n{~} OFF Bombs", "整塊舊鍵\n{~1} 小時 {~2} 分\n\n炸彈 {~3}");
        Files.writeString(dir.resolve("misc.json"), misc.toString(), StandardCharsets.UTF_8);
        JsonObject label = new JsonObject();
        label.addProperty("{~}d {~}h {~}m {~}s", "{~} 日 {~} 時 {~} 分 {~} 秒");
        label.addProperty("{~}h {~}m {~}s", "{~} 時 {~} 分 {~} 秒");
        label.addProperty("{~}d {~}s", "{~} 日 {~} 秒");
        label.addProperty("{~}s", "{~} 秒");
        Files.writeString(dir.resolve("scoped/label.json"), label.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    /** 實機的樣子：每一列一種顏色，列與列之間是換行。 */
    private static StyledText board(String[] rows, int[] colours) {
        MutableComponent all = Component.empty();
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                all.append(Component.literal("\n"));
            }
            if (!rows[i].isEmpty()) {
                all.append(lit(rows[i], colours[i]));
            }
        }
        return StyledText.fromComponent(all);
    }

    private static StyledText crateBoard(String timer) {
        return board(new String[] {"Crate Offer", timer, "", "30% OFF Crates", "(5x Regular and higher)",
                                   "", "Extra Bonus:", "Get VIP Rank FREE"},
                     new int[] {GOLD, WHITE, WHITE, GREEN, GREY, WHITE, AQUA, YELLOW});
    }

    private static void composes(TranslationStore store) {
        System.out.println("=== 沒收過的組合 ===");
        StyledText crate = crateBoard("3h 5m 2s");
        for (int path = 0; path < 2; path++) {
            String name = path == 0 ? "就地取代" : "注視小框";
            Component out = path == 0 ? LineTranslator.translateLabel(crate, store)
                                      : LineTranslator.translateFloating(crate, store);
            String got = out == null ? null : out.getString().replace("\n", "⏎");
            check("★ " + name + "：每一列都翻了（實際 " + got + "）",
                  got != null && got.startsWith("測寶箱特惠⏎3 時 5 分 2 秒⏎⏎測寶箱打 30% 折")
                          && got.contains("(測 5x 普通起)") && got.endsWith("測加碼:⏎測送 VIP"));
            check(name + "：沒剩英文", got != null && !got.contains("Offer") && !got.contains("FREE"));
            check(name + "：標題是金色", out != null && colourOf(out, "測寶箱特惠") == GOLD);
            check(name + "：倒數是白色", out != null && colourOf(out, "時") == WHITE);
            check(name + "：折扣列是綠色", out != null && colourOf(out, "測寶箱打") == GREEN);
            check(name + "：括號列是灰色", out != null && colourOf(out, "普通起") == GREY);
            check(name + "：贈品列是黃色", out != null && colourOf(out, "測送") == YELLOW);
        }
        // 另一種倒數：只剩天與秒（capture 看過 59 次）
        Component days = LineTranslator.translateLabel(board(
                new String[] {"Bomb Offer", "2d 9s", "", "15% OFF Bombs"},
                new int[] {GOLD, WHITE, WHITE, GREEN}), store);
        String got = days == null ? null : days.getString().replace("\n", "⏎");
        check("★ 天與秒的倒數（實際 " + got + "）",
              "測炸彈特惠⏎2 日 9 秒⏎⏎測炸彈打 15% 折".equals(got));
        // 倒數那一列是空的
        Component empty = LineTranslator.translateLabel(board(
                new String[] {"Bomb Offer", "", "", "15% OFF Bombs"},
                new int[] {GOLD, WHITE, WHITE, GREEN}), store);
        check("倒數列是空的：沒有倒數就不算看板，交給逐行那條路（實際 "
                      + (empty == null ? null : empty.getString().replace("\n", "⏎")) + "）",
              empty != null && empty.getString().equals("測炸彈特惠\n\n\n測炸彈打 15% 折"));
        // 收集端
        check("★ 收集端：這種組合算翻了", store.hasTranslation(
                "Crate Offer\n{~}h {~}m {~}s\n\n{~} OFF Crates\n({~}x Regular and higher)\n\nExtra Bonus:\nGet VIP Rank FREE"));
    }

    private static void guards(TranslationStore store) {
        System.out.println("=== 不該動的不動 ===");
        Component old = LineTranslator.translateLabel(board(
                new String[] {"Bomb Offer", "4h 7m", "", "15% OFF Bombs"},
                new int[] {GOLD, WHITE, WHITE, GREEN}), store);
        check("★ 整塊的舊鍵照樣優先（實際 " + (old == null ? null : old.getString().replace("\n", "⏎")) + "）",
              old != null && old.getString().startsWith("整塊舊鍵\n4 小時 7 分"));
        StyledText unknownRow = board(new String[] {"Crate Offer", "3h 5m 2s", "", "Get a Mystery Thing FREE"},
                                      new int[] {GOLD, WHITE, WHITE, YELLOW});
        check("有一列查不到：整塊不組", LineTranslator.countdownBoard(
                "Crate Offer\n{~}h {~}m {~}s\n\nGet a Mystery Thing FREE", store) == null);
        Component partial = LineTranslator.translateLabel(unknownRow, store);
        check("有一列查不到：就地取代整塊不動", partial == null);
        check("沒有倒數列的牌子不碰", LineTranslator.countdownBoard(
                "Crate Offer\n\n{~} OFF Crates", store) == null);
        check("單行不碰", LineTranslator.countdownBoard("{~}h {~}m {~}s", store) == null);
        check("只有倒數、沒有字的不碰", LineTranslator.countdownBoard("{~}h\n{~}s", store) == null);
        check("沒有東西的不碰", LineTranslator.countdownBoard(null, store) == null
                && LineTranslator.countdownBoard("", store) == null);
        check("時間格式的短鍵不會漏到一般語料（聊天、tooltip 的計時不受影響）",
              store.lookup("{~}h {~}m {~}s") == null);
    }

    /** 七個語言：capture 裡收到、而語料沒有整塊鍵的那幾種組合。 */
    private static void realCorpus() {
        System.out.println("=== 實際語料 ===");
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        String[][] boards = {
                {"Crate Offer", "4h 11m 52s", "", "30% OFF Crates", "(5x Regular and higher)", "", "Extra Bonus:",
                 "Get VIP Rank FREE"},
                {"Crate Offer", "2d 6m 3s", "", "30% OFF Crates", "(5x Regular and higher)", "", "Extra Bonus:",
                 "Get VIP Rank FREE"},
                {"Share Offer", "1d 2h 3m 4s", "", "20% OFF Shares", "(10x and higher)", "", "Extra Bonus:",
                 "Get VIP Rank FREE"},
                {"Share Offer", "7h 8s", "", "20% OFF Shares", "(10x and higher)", "", "Extra Bonus:",
                 "Get VIP Rank FREE"},
                {"Rank Offer", "5h 6m 7s", "", "25% OFF Ranks", "", "Extra Bonus:", "Get 3 Crates FREE"},
                {"Rank Offer", "9h", "", "25% OFF Ranks", "", "Extra Bonus:", "Get 3 Crates FREE"},
                {"Bomb Offer", "6m 40s", "", "10% OFF Bombs"},
                {"Bomb Offer", "2d 9s", "", "10% OFF Bombs"},
                {"Bomb Offer", "45s", "", "10% OFF Bombs"},
        };
        Pattern english = Pattern.compile("Offer|FREE|Extra Bonus|higher|Get |\\b\\d+[dhms]\\b");
        for (String lang : new String[] {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"}) {
            TranslationStore real = new TranslationStore();
            real.loadAll(base.resolve(lang));
            for (String[] rows : boards) {
                int[] colours = new int[rows.length];
                java.util.Arrays.fill(colours, WHITE);
                StyledText label = board(rows, colours);
                for (int path = 0; path < 2; path++) {
                    Component out = path == 0 ? LineTranslator.translateLabel(label, real)
                                              : LineTranslator.translateFloating(label, real);
                    String got = out == null ? null : out.getString().replace("\n", "⏎");
                    String want = rows[1].replaceAll("[^0-9 ]", "").strip();
                    boolean numbers = got != null;
                    for (String n : want.split(" ")) {
                        numbers &= got != null && got.contains(n);
                    }
                    check(lang + (path == 0 ? " 就地取代：" : " 注視小框：") + rows[0] + " " + rows[1]
                                  + "（實際 " + got + "）",
                          got != null && !english.matcher(got).find() && numbers && !got.contains("{")
                                  && got.split("⏎", -1).length == rows.length);
                }
            }
        }
    }

    private static int colourOf(Component whole, String piece) {
        int[] found = {-1};
        whole.visit((style, text) -> {
            if (found[0] < 0 && text.contains(piece) && style.getColor() != null) {
                found[0] = style.getColor().getValue();
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            System.out.println("  [PASS] " + what);
        } else {
            System.out.println("  [FAIL] " + what);
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("商店倒數看板：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("商店倒數看板：全部通過");
    }
}
