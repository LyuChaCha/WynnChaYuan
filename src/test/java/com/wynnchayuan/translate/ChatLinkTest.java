package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.Optional;

/**
 * 聊天訊息裡<b>可以點的那個詞</b>，譯文也要能點。
 *
 * <h2>畫面上長什麼樣</h2>
 * 「{@code Your ability tree is outdated, click here to update.}」整句紅字，
 * 中間的 {@code here} 加了底線，並且掛著一個 {@code ClickEvent}——點下去才會更新。
 *
 * <p>譯文先前是一整段沒有分段的字：底線沒了，<b>點也點不動</b>。
 * 使用者回報「點擊更新格式不正確 也沒辦法點」講的就是這個。
 *
 * <h2>為什麼不必改程式</h2>
 * {@code {cN}} 搬的是原文第 N 段的<b>樣式物件</b>，而 {@code ClickEvent} 跟底線
 * 一樣就住在 {@link Style} 裡——{@code LineTranslator#forDisplay} 只換字型，
 * 其餘原樣帶過去。所以譯文寫成「點{@code {c2}}這裡{@code {c1}}更新。」就夠了。
 *
 * <p>這件事只有跑過才知道，所以釘在這裡：將來若有人讓上色那條路改成「只抄顏色」，
 * 這個測試會紅，而那正是需要有人看一眼的時候。
 */
public final class ChatLinkTest {

    private static int failures = 0;

    /** 行首那個符號。語料裡是 {@code {#}}，實際送來的是私用區碼位。 */
    private static final String ICON = "";

    private static final int RED = 0xFF0000;

    /** 點下去會開的網址，取自實機那一則訊息的形狀。 */
    private static final ClickEvent LINK =
            new ClickEvent.RunCommand("/abilitytree update");

    /**
     * 遊戲送來的樣子，取自使用者回報的 majorid-debug.txt「可用的顏色 77」：
     * {@code {c1}} 是紅色的長句，{@code {c2}} 是紅色<b>帶底線</b>的 {@code here}。
     */
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

        // ★ 真正要釘的兩件事：可以點、看得出可以點。
        Style link = styleOf(hit, "這裡");
        check("「這裡」查得到樣式", link != null);
        check("「這裡」帶著原文的 ClickEvent（實際 "
                      + (link == null ? "null" : String.valueOf(link.getClickEvent())) + "）",
              link != null && LINK.equals(link.getClickEvent()));
        check("「這裡」跟原文一樣有底線", link != null && link.isUnderlined());

        // ★ 反方向：底線只能加在那個詞上，整句畫底線就是另一種壞法。
        Style body = styleOf(hit, "更新");
        check("句子其餘的部分沒有底線", body != null && !body.isUnderlined());
        check("句子其餘的部分沒有 ClickEvent", body != null && body.getClickEvent() == null);
        check("句子其餘的部分仍是原文的紅色",
              body != null && body.getColor() != null
                      && body.getColor().getValue() == RED);

        fallback();
        report();
    }

    /**
     * 沒標 {@code {cN}} 的那些，至少整行要點得動。
     *
     * <h2>為什麼需要這一層</h2>
     * 遊戲隨時會新增帶連結的訊息，語料不可能每一條都標到。沒標到的那些，
     * 譯文是一句<b>按了沒反應</b>的中文——比留著英文還糟，因為玩家不知道
     * 那是模組造成的。見 {@code LineTranslator#keepClick}。
     */
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

        // ★ 反方向：一行有兩個不同的連結時一律不碰——套錯連結比沒有連結危險。
        //
        // 直接叫 keepClick，因為要釘的正是「這一層有沒有出手」：走完整條翻譯流程的話，
        // 譯文可能本來就從某一段帶了連結過來，看不出這一層做了什麼。
        MutableComponent two = Component.empty();
        two.append(lit("Click ", false, new ClickEvent.RunCommand("/yes")));
        two.append(lit("here to decline", false, new ClickEvent.RunCommand("/no")));
        Component plain = Component.literal("點這裡");
        Component kept = LineTranslator.keepClick(StyledText.fromComponent(two), plain);
        check("一行有兩個不同的連結時不亂套（實際 "
                      + String.valueOf(styleOf(kept, "點這裡").getClickEvent()) + "）",
              styleOf(kept, "點這裡").getClickEvent() == null);

        // ★ 同一個連結在好幾段上重複，算一種，照樣補得上去。
        MutableComponent same = Component.empty();
        same.append(lit("Click ", false, LINK));
        same.append(lit("here", false, LINK));
        Component one = LineTranslator.keepClick(StyledText.fromComponent(same), plain);
        check("同一個連結分成好幾段時算一種",
              LINK.equals(styleOf(one, "點這裡").getClickEvent()));
    }

    /** 譯文裡含有這段字的那一段的樣式。 */
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
