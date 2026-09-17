package com.wynnchayuan.translate;

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
import java.util.ArrayList;
import java.util.List;

/**
 * 登入時的歡迎訊息：交易市場那兩行各有各的顏色。
 *
 * <h2>實機長什麼樣</h2>
 * 有東西賣出、又有坐騎沒飼料時，歡迎訊息底下多兩行：
 *
 * <pre>
 *   §d§l1§r §5item was sold on the Trade Market
 *   §#8f663dand §#bc8f62§l2§#8f663d mounts have §#bc8f62no food§#8f663d in their feeder
 * </pre>
 *
 * 第一行紫、第二行棕。實機回報譯文的第二行整行變成紫色——跟上一行同色。
 */
public final class TradeFeederColourTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;
    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int PINK = 0xFF55FF;
    private static final int PURPLE = 0xAA00AA;
    private static final int BROWN = 0x8F663D;
    private static final int TAN = 0xBC8F62;

    private static final String KEY = "{#}Welcome to Wynncraft!\n{#}play.wynncraft.com -/- wynncraft.com\n\n"
            + "{#}{~} item was sold on the Trade Market\n{#}and {~} mounts have no food in their feeder";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        String[][] cases = {
            {"zh_cn", "{#}欢迎来到 Wynncraft！\n{#}play.wynncraft.com -/- wynncraft.com\n\n"
                    + "{#}交易市场卖出了 {~} 件物品\n{#}另有 {~} 匹坐骑的喂食器里没有食物",
                    "交易市场卖出了", "另有", "喂食器"},
            {"zh_tw", "{#}歡迎來到 Wynncraft！\n{#}play.wynncraft.com -/- wynncraft.com\n\n"
                    + "{#}交易市場上賣出了 {~1} 件物品\n{#}另外有 {~2} 匹坐騎的餵食器沒有食物",
                    "交易市場上賣出了", "另外有", "餵食器"},
        };
        for (String[] c : cases) {
            Path dir = Files.createTempDirectory("wynnchayuan-feeder");
            FlowedDebug.init(dir);
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            root.addProperty(KEY, c[1]);
            Files.writeString(dir.resolve("misc.json"), root.toString(), StandardCharsets.UTF_8);
            TranslationStore store = new TranslationStore();
            store.loadAll(dir);

            System.out.println("== " + c[0]);
            Component out = LineTranslator.translateChat(message(), store, null, false);
            if (out == null) {
                System.out.println("  （沒翻出來）");
            } else {
                for (String row : describe(out)) {
                    System.out.println("  " + row);
                }
            }
            check(c[0] + " 翻得出來", out != null);
            if (out == null) {
                continue;
            }
            check(c[0] + " 第一行的字是紫的（實際 " + hex(colourOf(out, c[2])) + "）",
                  colourOf(out, c[2]) == PURPLE);
            check(c[0] + " 賣出的數量是粉紅的（實際 " + hex(colourOf(out, "1")) + "）",
                  colourOf(out, "1") == PINK);
            check(c[0] + " 第二行開頭是棕的（實際 " + hex(colourOf(out, c[3])) + "）",
                  colourOf(out, c[3]) == BROWN);
            check(c[0] + " 坐騎數量是淺棕的（實際 " + hex(colourOf(out, "2")) + "）",
                  colourOf(out, "2") == TAN);
            check(c[0] + " 第二行後半不是紫的（實際 " + hex(colourOf(out, c[4])) + "）",
                  colourOf(out, c[4]) != PURPLE && colourOf(out, c[4]) != PINK);
        }

        newQuest();

        System.out.println(failures == 0 ? "\n交易市場訊息顏色：全部通過"
                : "\n交易市場訊息顏色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /**
     * 升級時的「+ New Quest [Mini-Quest - Slay Spiders]」：前半淺灰、括號深灰。
     *
     * <p>括號那段比較長，整行的主色於是是深灰；「+ 新任務」要靠語料裡
     * 「+ New Quest」那一條認出來，才拿得回淺灰。實機回報整行變深灰。
     */
    private static void newQuest() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-newquest");
        FlowedDebug.init(dir);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("+ New Quest [Mini-Quest - Slay Spiders]", "+ 新任務 [迷你任務 - 獵殺蜘蛛]");
        root.addProperty("+ New Quest", "+ 新任務");
        Files.writeString(dir.resolve("misc.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        MutableComponent line = Component.empty();
        line.append(lit("+ New Quest ", GREY, false));
        line.append(lit("[Mini-Quest - Slay Spiders]", 0x555555, false));
        Component out = LineTranslator.translateChat(StyledText.fromComponent(line), store);
        System.out.println("== 新任務");
        if (out != null) {
            for (String row : describe(out)) {
                System.out.println("  " + row);
            }
        }
        check("「+ 新任務」是淺灰（實際 " + hex(out == null ? -1 : colourOf(out, "新任務")) + "）",
              out != null && colourOf(out, "新任務") == GREY);
        check("括號是深灰（實際 " + hex(out == null ? -1 : colourOf(out, "獵殺")) + "）",
              out != null && colourOf(out, "獵殺") == 0x555555);
    }

    /** 實機那則訊息，照 majorid-debug 的「聊天對齊」原文拼回來。 */
    private static StyledText message() {
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
        all.append(space(0xD003F));
        all.append(lit("1", PINK, true));
        all.append(lit(" ", null, false));
        all.append(lit("item was sold on the Trade Market", PURPLE, false));
        all.append(lit("\n", null, false));
        all.append(space(0xD002E));
        all.append(lit("and ", BROWN, false));
        all.append(lit("2", TAN, true));
        all.append(lit(" mounts have ", BROWN, false));
        all.append(lit("no food", TAN, false));
        all.append(lit(" in their feeder", BROWN, false));
        return StyledText.fromComponent(all);
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

    /** 含有這段文字的那一片段是什麼顏色；找不到回傳 -1。 */
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

    private static List<String> describe(Component component) {
        List<String> out = new ArrayList<>();
        component.visit((style, piece) -> {
            out.add(hex(style.getColor() == null ? -2 : style.getColor().getValue())
                    + (Boolean.TRUE.equals(style.isBold()) ? " 粗" : "   ")
                    + "  「" + piece.replace("\n", "\\n") + "」");
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
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
