package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 登入的歡迎訊息：整則的鍵查不到時一段一段組（{@link LineTranslator#welcomeBanner}）。
 *
 * <h2>實機回報（2026-10-17）</h2>
 * 歡迎訊息底下的幾行是看當下狀況拼的（交易市場賣出／買進、坐騎吃完飼料／飼料槽空了、單複數），
 * 語料一種組合一條整則的鍵。這一次登入是
 *
 * <pre>
 *   1 item was sold on the Trade Market
 *   and 2 Mounts finished eating while you were offline
 * </pre>
 *
 * 語料沒有這個組合，整則留英文。訊息的分段與顏色照遊戲 log 的 {@code [CHAT/INFO]} 抄的。
 */
public final class WelcomeBannerTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;
    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int PINK = 0xFF55FF;
    private static final int PURPLE = 0xAA00AA;
    private static final int BROWN = 0x8F663D;
    private static final int TAN = 0xBC8F62;

    private static final String HEAD = "{#}Welcome to Wynncraft!\n{#}play.wynncraft.com -/- wynncraft.com";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        FlowedDebug.init(Files.createTempDirectory("wynnchayuan-welcome"));

        fixture();
        realCorpus();
        colours();

        System.out.println(failures == 0 ? "\n歡迎訊息逐段組：全部通過"
                : "\n歡迎訊息逐段組：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 假語料：只有舊的整則鍵與逐行鍵，沒有這一次的組合。 */
    private static void fixture() throws Exception {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        // 舊的整則鍵（別的組合），長得跟語料一樣
        root.addProperty(HEAD + "\n\n{#}{~} item was sold on the Trade Market\n{#}and {~} mounts have no food in their feeder",
                "{#}歡迎來到 Wynncraft！\n{#}play.wynncraft.com -/- wynncraft.com\n\n"
                        + "{#}交易市場上賣出了 {~1} 件物品\n{#}另外有 {~2} 匹坐騎的餵食器沒有食物");
        root.addProperty(HEAD, "{#}歡迎來到 Wynncraft！\n{#}play.wynncraft.com -/- wynncraft.com");
        root.addProperty("{#}{~} item was sold on the Trade Market", "{#}交易市場上賣出了 {~} 件物品");
        root.addProperty("{#}and {~} Mounts finished eating while you were offline",
                "{#}另外有 {~} 匹坐騎在你離線期間吃完了食物");
        // 不是歡迎訊息、可是每一行都有鍵的另一則：不能被逐行組
        root.addProperty("{#}Some other notice", "{#}別的公告");
        root.addProperty("{#}and {~} more lines", "{#}還有 {~} 行");
        Path dir = Files.createTempDirectory("wynnchayuan-welcome-fixture");
        Files.writeString(dir.resolve("misc.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        System.out.println("== 假語料：賣出 1 件＋2 匹坐騎吃完了食物");
        Component out = LineTranslator.translateChat(soldAndFed(1, 2), store);
        print(out);
        check("★ 整則沒有鍵也翻得出來", out != null);
        if (out != null) {
            int rows = rowCount(out.getString());
            int orig = rowCount(soldAndFed(1, 2).getComponent().getString());
            check("行數沒變（實際 " + rows + "，原文 " + orig + "）", rows == orig);
            String all = visible(out.getString());
            check("標頭翻了", all.contains("歡迎來到 Wynncraft！"));
            check("賣出那一行，數字在對的位置", all.contains("交易市場上賣出了 1 件物品"));
            check("★ 「and」那一行接在後面讀得通，數字是第二個",
                  all.contains("另外有 2 匹坐騎在你離線期間吃完了食物"));
            check("沒有留英文", !all.contains("item") && !all.contains("Mounts") && !all.contains("Welcome"));
            check("賣出的數量是粉紅的（實際 " + hex(colourOf(out, "1")) + "）", colourOf(out, "1") == PINK);
            check("賣出那一行的字是紫的（實際 " + hex(colourOf(out, "交易市場")) + "）",
                  colourOf(out, "交易市場") == PURPLE);
            check("坐騎數量是淺棕的（實際 " + hex(colourOf(out, "2")) + "）", colourOf(out, "2") == TAN);
            check("坐騎那一行是棕的（實際 " + hex(colourOf(out, "另外有")) + "）",
                  colourOf(out, "另外有") == BROWN);
            check("每一行開頭的排版偏移還在", glyphRows(out) >= 4);
        }

        // ★ 有一行查不到就整則放棄，不翻出半中半英
        Component partial = LineTranslator.translateChat(soldAndFed(1, 2, "and 2 Pets are hungry"), store);
        check("★ 有一行查不到：整則不翻（實際 "
              + (partial == null ? "null" : visible(partial.getString())) + "）", partial == null);

        // 不是歡迎訊息的多行訊息，每一行都有鍵也不逐行組
        MutableComponent other = Component.empty();
        other.append(space(0xD0010)).append(lit("Some other notice", WHITE, false));
        other.append(lit("\n", null, false));
        other.append(space(0xD0012)).append(lit("and 3 more lines", WHITE, false));
        check("不是歡迎訊息的不逐行組",
              LineTranslator.translateChat(StyledText.fromComponent(other), store) == null);
    }

    /** 實際語料：七個語言都要翻得出這一次的組合與另外幾種沒有整則鍵的組合。 */
    private static void realCorpus() {
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        List<String> english = List.of("Welcome", "item", "Trade Market", "Mounts", "mounts",
                                       "feeder", "offline", "sold as well");
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(base.resolve(lang));
            List<StyledText> cases = List.of(
                    soldAndFed(1, 2),
                    soldAndFed(3, 2, "and 2 Mounts finished eating while you were offline",
                               "items were sold"),
                    banner(new String[] {"2 items were bought on the Trade Market",
                                         "and 1 mount has no food in their feeder"}),
                    banner(new String[] {"4 items were bought on the Trade Market",
                                         "and 1 item was sold as well",
                                         "and 2 Mounts finished eating while you were offline"}),
                    banner(new String[] {"2 Mounts finished eating while you were offline"}));
            for (StyledText msg : cases) {
                Component out = LineTranslator.translateChat(msg, store);
                String all = out == null ? "" : visible(out.getString());
                if ("zh_tw".equals(lang) || "ru_ru".equals(lang)) {
                    System.out.println("   " + lang + " | " + all.replace("\n", " / "));
                }
                boolean left = false;
                for (String word : english) {
                    left |= all.contains(word);
                }
                String src = visible(msg.getComponent().getString()).replace("\n", " / ");
                check(lang + "：翻得出來、沒留英文 —— " + src, out != null && !left);
                if (out != null) {
                    check(lang + "：行數沒變 —— " + src,
                          rowCount(out.getString()) == rowCount(msg.getComponent().getString()));
                }
            }
        }
    }

    /**
     * ★ 每一行的顏色要跟原文那一行一樣（實機回報，test5）。
     *
     * <p>「2 items were sold」那一段用的是整則的舊鍵當前綴，它帶著 {@code {cN}}；
     * 接在後面的「and 2 Mounts…」沒有，結果整行沿用上一行的紫色，只有數字是對的。
     * 單數那種（前綴沒有 {@code {cN}}）測不出來，所以這裡用複數。
     * 原文的顏色照 log 的 {@code [CHAT/INFO]}。
     */
    private static void colours() {
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        // 實機截圖那一則
        StyledText shot = soldAndFed(2, 2, "and 2 Mounts finished eating while you were offline",
                                     "items were sold");
        // 另一種：買進、也賣出、坐騎吃完了
        MutableComponent bought = header();
        bought.append(space(0xD0038));
        bought.append(lit("4", PINK, true));
        bought.append(lit(" ", null, false));
        bought.append(lit("items were bought on the Trade Market", PURPLE, false));
        bought.append(lit("\n", null, false));
        bought.append(space(0xD0050));
        bought.append(lit("and ", PURPLE, false));
        bought.append(lit("1", PINK, true));
        bought.append(lit(" item was sold as well", PURPLE, false));
        bought.append(lit("\n", null, false));
        bought.append(space(0xD0019));
        bought.append(lit("and ", BROWN, false));
        bought.append(lit("2", TAN, true));
        bought.append(lit(" Mounts finished eating while you were offline", BROWN, false));
        StyledText boughtMsg = StyledText.fromComponent(bought);

        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(base.resolve(lang));
            rowColours(lang + " 截圖那一則", LineTranslator.translateChat(shot, store),
                       new int[][] {{PURPLE, PINK}, {BROWN, TAN}});
            rowColours(lang + " 買進＋賣出＋坐騎", LineTranslator.translateChat(boughtMsg, store),
                       new int[][] {{PURPLE, PINK}, {PURPLE, PINK}, {BROWN, TAN}});
        }
    }

    /**
     * 標頭與內文每一行的顏色。
     *
     * @param body 內文每一行 {字的顏色, 數字的顏色}；數字一律要粗體
     */
    private static void rowColours(String name, Component out, int[][] body) {
        check(name + "：翻得出來", out != null);
        if (out == null) {
            return;
        }
        List<List<Object[]>> rows = new java.util.ArrayList<>();
        rows.add(new java.util.ArrayList<>());
        out.visit((style, piece) -> {
            for (int k = 0; k < piece.length(); ) {
                int cp = piece.codePointAt(k);
                if (cp == '\n') {
                    rows.add(new java.util.ArrayList<>());
                } else {
                    rows.get(rows.size() - 1).add(new Object[] {cp, style});
                }
                k += Character.charCount(cp);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        while (!rows.isEmpty() && rows.get(0).isEmpty()) {
            rows.remove(0);
        }
        check(name + "：行數（實際 " + rows.size() + "）", rows.size() == 3 + body.length);
        if (rows.size() != 3 + body.length) {
            return;
        }
        // 標頭：每一個字都是金色粗體（中日韓的字也是）
        String header = describeRow(rows.get(0), GOLD, -1, true);
        check(name + "：標頭整行金色粗體" + header, header.isEmpty());
        // 網址那一行：白、「-/-」灰
        boolean url = true;
        for (Object[] c : rows.get(1)) {
            int cp = (Integer) c[0];
            int colour = colourValue((Style) c[1]);
            if (cp == '/' || (cp == '-')) {
                url &= colour == GREY;
            } else if (Character.isLetter(cp)) {
                url &= colour == WHITE;
            }
        }
        check(name + "：網址那一行白、-/- 灰", url);
        for (int r = 0; r < body.length; r++) {
            String bad = describeRow(rows.get(3 + r), body[r][0], body[r][1], false);
            check(name + "：內文第 " + (r + 1) + " 行的字 " + hex(body[r][0]) + "、數字 "
                  + hex(body[r][1]) + " 粗體" + bad, bad.isEmpty());
        }
    }

    /**
     * 一行裡顏色不對的字。
     *
     * @param number 數字的顏色；{@code -1} 表示這一行沒有數字要分開看
     * @param allBold 每一個字都要粗體
     * @return 不對的字列出來；全對時是空字串
     */
    private static String describeRow(List<Object[]> row, int text, int number, boolean allBold) {
        StringBuilder bad = new StringBuilder();
        for (Object[] c : row) {
            int cp = (Integer) c[0];
            Style style = (Style) c[1];
            if (GlyphSplitter.isGlyphCodePoint(cp) || Character.isWhitespace(cp)) {
                continue;
            }
            boolean digit = Character.isDigit(cp) && number >= 0;
            int want = digit ? number : text;
            boolean bold = Boolean.TRUE.equals(style.isBold());
            boolean ok = colourValue(style) == want && (!digit || bold) && (!allBold || bold);
            if (!ok && bad.length() < 200) {
                bad.append(" 「").append(Character.toString(cp)).append("」")
                   .append(hex(colourValue(style))).append(bold ? "粗" : "");
            }
        }
        return bad.length() == 0 ? "" : "（不對：" + bad + "）";
    }

    private static int colourValue(Style style) {
        return style.getColor() == null ? -2 : style.getColor().getValue();
    }

    private static StyledText soldAndFed(int sold, int mounts) {
        return soldAndFed(sold, mounts,
                "and " + mounts + " Mounts finished eating while you were offline");
    }

    private static StyledText soldAndFed(int sold, int mounts, String second) {
        return soldAndFed(sold, mounts, second, sold == 1 ? "item was sold" : "items were sold");
    }

    /** 實機那則訊息（log 的 {@code [CHAT/INFO]}）：標頭、空行、賣出、坐騎。 */
    private static StyledText soldAndFed(int sold, int mounts, String second, String verb) {
        MutableComponent all = header();
        all.append(space(0xD003F));
        all.append(lit(String.valueOf(sold), PINK, true));
        all.append(lit(" ", null, false));
        all.append(lit(verb + " on the Trade Market", PURPLE, false));
        all.append(lit("\n", null, false));
        all.append(space(0xD0019));
        String count = String.valueOf(mounts);
        int at = second.indexOf(count);
        all.append(lit(second.substring(0, at), BROWN, false));
        all.append(lit(count, TAN, true));
        all.append(lit(second.substring(at + count.length()), BROWN, false));
        return StyledText.fromComponent(all);
    }

    /** 標頭＋幾行內文，內文每一行一個顏色、數字另一個顏色。 */
    private static StyledText banner(String[] body) {
        MutableComponent all = header();
        for (int i = 0; i < body.length; i++) {
            if (i > 0) {
                all.append(lit("\n", null, false));
            }
            all.append(space(0xD0020 + i));
            String row = body[i];
            int digit = 0;
            while (digit < row.length() && !Character.isDigit(row.charAt(digit))) {
                digit++;
            }
            int stop = digit;
            while (stop < row.length() && Character.isDigit(row.charAt(stop))) {
                stop++;
            }
            if (digit > 0) {
                all.append(lit(row.substring(0, digit), BROWN, false));
            }
            all.append(lit(row.substring(digit, stop), TAN, true));
            all.append(lit(row.substring(stop), BROWN, false));
        }
        return StyledText.fromComponent(all);
    }

    private static MutableComponent header() {
        MutableComponent all = Component.empty();
        all.append(lit("\n", null, false));
        all.append(space(0xD0059));
        all.append(lit("Welcome to Wynncraft!", GOLD, true));
        all.append(lit("\n", null, false));
        all.append(space(0xD003B));
        all.append(lit("play.wynncraft.com ", WHITE, false));
        all.append(lit("-/- ", GREY, false));
        all.append(lit("wynncraft.com", WHITE, false));
        all.append(lit("\n\n", null, false));
        return all;
    }

    private static Component space(int codePoint) {
        return Component.literal(Character.toString(codePoint)).withStyle(Style.EMPTY.withFont(
                new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "space"))));
    }

    private static Component lit(String text, Integer colour, boolean bold) {
        Style style = Style.EMPTY;
        if (colour != null) {
            style = style.withColor(TextColor.fromRgb(colour));
        }
        if (bold) {
            style = style.withBold(true);
        }
        return Component.literal(text).withStyle(style);
    }

    /**
     * 行數，不算開頭的空行。原文第一行是空的；整則的舊鍵翻出來也一向沒有那一行，
     * 這裡比的是內容的行數。
     */
    private static int rowCount(String text) {
        int start = 0;
        while (start < text.length() && text.charAt(start) == '\n') {
            start++;
        }
        return text.substring(start).split("\n", -1).length;
    }

    /** 有幾行帶著排版偏移字元。 */
    private static int glyphRows(Component out) {
        int n = 0;
        for (String row : out.getString().split("\n", -1)) {
            if (!row.equals(GlyphSplitter.stripGlyphChars(row))) {
                n++;
            }
        }
        return n;
    }

    private static int colourOf(Component component, String text) {
        int[] found = {-1};
        component.visit((style, piece) -> {
            if (found[0] < 0 && piece.contains(text)) {
                found[0] = style.getColor() == null ? -2 : style.getColor().getValue();
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static String visible(String text) {
        StringBuilder out = new StringBuilder();
        String[] rows = text.split("\n", -1);
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(GlyphSplitter.stripGlyphChars(rows[i]).strip());
        }
        return out.toString();
    }

    private static void print(Component out) {
        if (out == null) {
            System.out.println("  （沒翻出來）");
            return;
        }
        for (String row : visible(out.getString()).split("\n", -1)) {
            System.out.println("  | " + row);
        }
    }

    private static String hex(int colour) {
        return switch (colour) {
            case -1 -> "沒有這一段";
            case -2 -> "無色";
            default -> String.format("#%06X", colour);
        };
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
