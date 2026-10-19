package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.TranslationStore;

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
import java.util.Optional;

/**
 * issue #1298 的兩個後續（#1301 之後）。
 *
 * <ul>
 *   <li><b>(a)</b> 就地取代：選項列與 SHIFT 提示列的譯文帶 \n 時，也要像內文那樣攤平
 *       （{@link DialogueRewriter#oneLine}）。#1301 只攤平了內文，這兩列的 \n 仍會畫成方框。</li>
 *   <li><b>(b)</b> 小框的自動折行要懂中文：「，」「。」不能落在行首、英文單字不切開、
 *       強調色跟著字走。先前用的是 {@code font.split}，只認空白。</li>
 * </ul>
 */
public final class DialogueWrapTest {

    private static int failures = 0;

    private static final TextColor BODY = TextColor.fromRgb(0xe0d4c0);
    private static final TextColor ITEM = TextColor.fromRgb(0x55ffff);

    /** 不能出現在行首的字元，跟 {@code DialogueRewriter#CLOSING} 同一份。 */
    private static final String CLOSING = ")]}>」』】》〉）〕］｝”’，。、；：！？%％・…‥";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        DialogueRewriter.widthForTest = DialogueWrapTest::realWidth;

        choiceAndPromptNewline();
        panelPunctuation();
        panelWords();
        panelColours();
        panelShortAndHard();

        System.out.println(failures == 0 ? "\n對話折行：全部通過"
                : "\n對話折行：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // (a) 選項列與提示列
    // ------------------------------------------------------------------

    private static void choiceAndPromptNewline() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-wrap");
        setConfig(new CollectorConfig(dir.resolve("config.json")));
        Files.writeString(dir.resolve("misc.json"), """
                {
                  "Who are you?": "你是\\n誰？",
                  "Have we met before?": "Nos hemos\\nvisto antes?",
                  "to continue": "繼續\\n下去"
                }
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        Style choice1 = font("hud/dialogue/text/wynncraft/choice_1");
        Style choice2 = font("hud/dialogue/text/wynncraft/choice_2");
        Style control = font("hud/dialogue/text/control");
        MutableComponent message = Component.empty();
        row(message, choice1, "Who are you?");
        row(message, choice2, "Have we met before?");
        row(message, control, " to continue");

        Component out = DialogueRewriter.rewrite(message, store);
        String text = out == null ? "" : out.getString();
        check("改寫得出來（實際 " + (out == null ? "null" : "有") + "）", out != null);
        check("★ 選項與提示列裡沒有換行字元", out != null && text.indexOf('\n') < 0);
        check("中文選項的 \\n 直接接起來", text.contains("你是誰？"));
        check("拉丁字之間補一個空白", text.contains("Nos hemos visto antes?"));
        check("提示列的 \\n 也攤平", text.contains("繼續下去"));
    }

    private static void row(MutableComponent message, Style style, String text) {
        message.append(Component.literal(offset(42)).withStyle(style));
        message.append(Component.literal(text).withStyle(style));
        message.append(Component.literal(offset(-73)).withStyle(style));
    }

    private static String offset(int px) {
        return new String(Character.toChars(0xD0000 + px));
    }

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace(path)));
    }

    private static void setConfig(CollectorConfig config) throws Exception {
        var field = WynnChaYuan.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, config);
    }

    // ------------------------------------------------------------------
    // (b) 小框的自動折行
    // ------------------------------------------------------------------

    /** 斷點剛好落在「，」前面：要把前一個字一起帶下去，不讓逗號開頭。 */
    private static void panelPunctuation() {
        // 寬 90px = 9 個中文字；第 10 個字是「，」，照量到哪斷到哪就會落在行首
        String text = "一二三四五六七八九，十甲乙丙丁戊己庚。辛壬癸子丑寅卯辰巳！午未申酉戌亥。";
        List<Component> rows = DialogueOverlay.fold(Component.literal(text), 90);
        boolean head = true;
        boolean fits = true;
        StringBuilder joined = new StringBuilder();
        for (Component row : rows) {
            String s = row.getString();
            if (!s.isEmpty() && CLOSING.indexOf(s.charAt(0)) >= 0) {
                head = false;
                System.out.println("    行首是標點：「" + s + "」");
            }
            fits &= realWidth(s) <= 90;
            joined.append(s);
        }
        check("真的折成好幾行（" + rows.size() + " 行）", rows.size() > 2);
        check("★ 「，」「。」「！」不會落在行首", head);
        check("每一行都放得進框寬", fits);
        check("沒有漏字", joined.toString().equals(text));
    }

    /** 中英混排：英文名字整個留在同一行。 */
    private static void panelWords() {
        String text = "你要是能把那雙靴子帶回來，我就送你 Abysso Galoshes 當作謝禮，好嗎？";
        List<Component> rows = DialogueOverlay.fold(Component.literal(text), 120);
        boolean whole = true;
        for (Component row : rows) {
            String s = row.getString();
            if (s.endsWith("Aby") || s.endsWith("Abys") || s.endsWith("Gal")
                    || s.startsWith("sso") || s.startsWith("oshes")) {
                whole = false;
            }
        }
        StringBuilder joined = new StringBuilder();
        for (Component row : rows) {
            joined.append(row.getString());
        }
        check("英文單字不會被切開", whole);
        check("中英混排沒有漏字（斷在空白的那一格被吃掉）",
              joined.toString().replace(" ", "").equals(text.replace(" ", "")));
    }

    /** 強調色跟著字走，跨行也一樣。 */
    private static void panelColours() {
        MutableComponent line = Component.empty();
        line.append(Component.literal("你要是能把那雙被海盜偷走的靴子 ").withStyle(
                Style.EMPTY.withColor(BODY)));
        line.append(Component.literal("[Abysso Galoshes]").withStyle(
                Style.EMPTY.withColor(ITEM)));
        line.append(Component.literal(" 帶回來，我可以給你報酬！").withStyle(
                Style.EMPTY.withColor(BODY)));
        List<Component> rows = DialogueOverlay.fold(line, 100);
        boolean ok = true;
        StringBuilder name = new StringBuilder();
        for (Component row : rows) {
            // 名字那幾截要是青色、散文那幾截要是底色
            Optional<Boolean> bad = row.visit((style, text) -> {
                boolean inName = text.contains("Abysso") || text.contains("Galoshes");
                if (inName) {
                    name.append(text);
                }
                boolean want = inName ? ITEM.equals(style.getColor())
                        : text.isBlank() || BODY.equals(style.getColor());
                return want ? Optional.empty() : Optional.of(Boolean.TRUE);
            }, Style.EMPTY);
            if (bad.isPresent()) {
                System.out.println("    顏色錯：「" + row.getString() + "」");
            }
            ok &= bad.isEmpty();
        }
        check("真的折成好幾行", rows.size() > 1);
        check("★ 名字那幾截折行之後仍是強調色", ok && name.length() > 0);
    }

    /** 短的不動；譯者寫的 \n 照樣是硬換行。 */
    private static void panelShortAndHard() {
        Component small = Component.literal("你好。");
        List<Component> rows = DialogueOverlay.fold(small, 200);
        check("放得下的一行原樣回傳", rows.size() == 1 && rows.get(0) == small);
        List<Component> lines = Boxes.toLines(Component.literal("第一句。\n第二句。"));
        check("譯文裡的 \\n 仍是硬換行（小框）", lines.size() == 2
                && lines.get(0).getString().equals("第一句。"));
    }

    // ------------------------------------------------------------------

    /** 中日韓一個字 10px，其餘 6px。 */
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
