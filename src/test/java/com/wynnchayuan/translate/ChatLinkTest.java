package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.Optional;

public final class ChatLinkTest {

    private static int failures = 0;

    private static final String ICON = "";

    private static final int RED = 0xFF0000;

    private static final ClickEvent LINK =
            new ClickEvent.RunCommand("/abilitytree update");

    private static StyledText outdated() {
        MutableComponent all = Component.empty();
        all.append(lit(ICON, false, null));
        all.append(lit("Your ability tree is outdated, click ", false, null));
        all.append(lit("here", true, LINK));
        all.append(lit(" to update.", false, null));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, boolean underline, ClickEvent click) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(RED))
                .withUnderlined(underline);
        if (click != null) {
            style = style.withClickEvent(click);
        }
        return Component.literal(text).withStyle(style);
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                              Languages.DEFAULT));
        FlowedDebug.init(java.nio.file.Files.createTempDirectory("wynnchayuan"));

        Component hit = LineTranslator.translate(outdated(), store);
        check("查得到譯文", hit != null);
        if (hit == null) {
            report();
            return;
        }
        System.out.println("      輸出：" + hit.getString());

        String shown = hit.getString();
        check("翻出來了（實際 " + shown + "）", shown.contains("技能樹"));

        Style link = styleOf(hit, "這裡");
        check("「這裡」查得到樣式", link != null);
        check("「這裡」帶著原文的 ClickEvent（實際 "
                      + (link == null ? "null" : String.valueOf(link.getClickEvent())) + "）",
              link != null && LINK.equals(link.getClickEvent()));
        check("「這裡」跟原文一樣有底線", link != null && link.isUnderlined());

        Style body = styleOf(hit, "更新");
        check("句子其餘的部分沒有底線", body != null && !body.isUnderlined());
        check("句子其餘的部分沒有 ClickEvent", body != null && body.getClickEvent() == null);
        check("句子其餘的部分仍是原文的紅色",
              body != null && body.getColor() != null
                      && body.getColor().getValue() == RED);

        fallback();
        report();
    }

    private static void fallback() throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                              Languages.DEFAULT));

        MutableComponent all = Component.empty();
        all.append(lit("Click ", false, null));
        all.append(lit("here to join", false, LINK));
        Component hit = LineTranslator.translate(StyledText.fromComponent(all), store);
        check("「Click here to join」查得到譯文", hit != null);
        if (hit == null) {
            return;
        }
        Style style = styleOf(hit, "加入");
        check("語料沒標 {cN} 時，整行照樣掛上原文的 ClickEvent（實際 "
                      + (style == null ? "null" : String.valueOf(style.getClickEvent())) + "）",
              style != null && LINK.equals(style.getClickEvent()));

        MutableComponent two = Component.empty();
        two.append(lit("Click ", false, new ClickEvent.RunCommand("/yes")));
        two.append(lit("here to decline", false, new ClickEvent.RunCommand("/no")));
        Component plain = Component.literal("點這裡");
        Component kept = LineTranslator.keepClick(StyledText.fromComponent(two), plain);
        check("一行有兩個不同的連結時不亂套（實際 "
                      + String.valueOf(styleOf(kept, "點這裡").getClickEvent()) + "）",
              styleOf(kept, "點這裡").getClickEvent() == null);

        MutableComponent same = Component.empty();
        same.append(lit("Click ", false, LINK));
        same.append(lit("here", false, LINK));
        Component one = LineTranslator.keepClick(StyledText.fromComponent(same), plain);
        check("同一個連結分成好幾段時算一種",
              LINK.equals(styleOf(one, "點這裡").getClickEvent()));
    }

    private static Style styleOf(Component line, String needle) {
        Style[] found = {null};
        line.visit((style, text) -> {
            if (found[0] == null && text.contains(needle)) {
                found[0] = style;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("聊天連結：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("聊天連結：全部通過");
    }
}
