package com.wynnchayuan.translate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.NameFrames;
import com.wynnchayuan.capture.PlayerDataFilter;
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
 * 夾著<b>別人名字</b>的句子，照語料裡帶 {@code {u}} 的句型翻（見 {@link NameFrames}）。
 *
 * <h2>先前是什麼樣子</h2>
 * {@code {u}} 只認得自己的名字。隊友給的增益（「Green_teaTW/RichaCha has given you 29%
 * resistance and 8% strength.」）、別人的死亡快訊、別人的力量之井，語料裡明明有
 * {@code {u}} 那一條，畫面上還是整句英文（使用者 2026-10-17 的截圖）。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>聊天、標題、名牌三條路都認得；名字從<b>位置</b>收，帳號/暱稱、多詞暱稱、
 *       底線、數字、長得像技能名的都原樣填回，名字裡的數字不會跟句子的數值搶順序；</li>
 *   <li>名字在句中、句尾、所有格前面都認得；伺服器折過行的也認得；</li>
 *   <li>不是名字的不能被當成名字：「Your pet」、一整句話、帶冒號的發言；
 *       語料本來就有整句的鍵時，整句那條優先；</li>
 *   <li>收集端記的是 {@code {u}} 那一份，名字不進檔案；那一份也不會被濾網的片語當成個資清掉。</li>
 * </ol>
 *
 * <p>語料是測試自己寫的，措辭跟實際語料無關。訊息照實機 log 的分段組。
 */
public final class NameFrameTest {

    private static int failures = 0;

    private static final int AQUA = 0x55FFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int RED = 0xD6401E;
    private static final int ORANGE = 0xFA7F63;
    private static final int GOLD = 0xFFAA00;

    private static final String ICON = "󟿼󟿿󟿾";
    private static final String ICON_NEXT = "󟿼󠀆";

    private static final String BUFF = "{u} has given you {~} resistance and {~} strength.";
    private static final String SPEED = "{u} gave you +{~} minutes speed boost.";

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-nameframe");
        FlowedDebug.init(dir);
        TranslationStore store = corpus(dir.resolve("corpus"));

        frames(store);
        buffs(store);
        deaths(store);
        titles(store);
        nameplates(store);
        notNames(store);
        privacy(store, dir);
        realCorpus();
        // 這一段會記下「自己的名字」（靜態的），放在最後，不影響上面幾段
        ownNameInsideName(store);
        report();
    }

    /**
     * 實機回報（2026-10-17，test jar 8）：「Green_teaTW/RichaCha has given you 29% resistance
     * and 8% strength.」整句英文。
     *
     * <p>log 裡伺服器送的是 {@code &b&o&<1>RichaCha&7 has given you &b29% resistance&7 and
     * &b8% strength&7.}——暱稱是青色斜體、掛著滑鼠提示（{@code &<1>} 是 Wynntils 記 hover
     * 的寫法），Wynntils 再把它改寫成「帳號/暱稱」。帳號那一半是<b>玩家自己</b>，
     * SelfNames 先把它收成 {@code {u}}，模板成了「{u}/RichaCha has given you …」：整句查不到，
     * 而句型那一關看到模板裡已經有 {u} 就放棄了。
     */
    private static void ownNameInsideName(TranslationStore store) {
        System.out.println("=== 名字裡有自己的帳號名 ===");
        com.wynnchayuan.capture.SelfNames.remember("Green_teaTW");
        Style hover = Style.EMPTY.withColor(TextColor.fromRgb(AQUA)).withItalic(true)
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                        Component.literal("Green_teaTW")));
        // Wynntils 改寫後：帳號/ 與暱稱兩段，都帶著同一個 hover
        MutableComponent rewritten = Component.empty();
        rewritten.append(Component.literal("Green_teaTW/").withStyle(hover));
        rewritten.append(Component.literal("RichaCha").withStyle(hover));
        StyledText line = buff(rewritten);
        check("重現：模板裡帳號已經是 {u}（實際 "
                      + LineParts.of(line).template() + "）",
              LineParts.of(line).template().startsWith("{u}/RichaCha"));
        Component hit = LineTranslator.translateChat(line, store);
        String got = hit == null ? null : hit.getString();
        check("★ 「帳號/暱稱」而帳號是自己：照樣翻（實際 " + got + "）",
              "Green_teaTW/RichaCha 賦予你 29% 甲盾與 8% 乙力。".equals(got));
        // 實機回報（10-19，test jar 9）：翻出來了，可是名字變黃、其餘整句同一個顏色
        check("★ 帳號那一半是名字的顏色（實際 " + hex(colourOf(hit, "Green_teaTW")) + "）",
              hit != null && colourOf(hit, "Green_teaTW") == AQUA);
        check("★ 暱稱是名字的顏色（實際 " + hex(colourOf(hit, "RichaCha")) + "）",
              hit != null && colourOf(hit, "RichaCha") == AQUA);
        check("★ 句子是灰的（實際 " + hex(colourOf(hit, "賦予你")) + "）",
              hit != null && colourOf(hit, "賦予你") == GREY);
        check("★ 數值是青的（實際 " + hex(colourOf(hit, "29%")) + "）",
              hit != null && colourOf(hit, "29%") == AQUA);
        check("★ 「與」是灰的（實際 " + hex(colourOf(hit, "與")) + "）",
              hit != null && colourOf(hit, "與") == GREY);
        check("★ 聊天的守門也認得", LineTranslator.nameFramed(line, store) != null);
        String captured = com.wynnchayuan.listener.CaptureProbe.chatTemplate(line, store);
        check("★ 收集端記的是 {u} 那一份（實際 " + captured + "）", BUFF.equals(captured));

        // 伺服器原本那一段：只有暱稱，數值那一段是「&b 8% strength」（空白在數值段裡）
        com.wynnchayuan.capture.SelfNames.remember("RichaCha");
        MutableComponent raw = Component.empty();
        raw.append(Component.literal("RichaCha").withStyle(hover));
        raw.append(lit(" has given you ", GREY));
        raw.append(lit("25% resistance", AQUA));
        raw.append(lit(" and", GREY));
        raw.append(lit(" 8% strength", AQUA));
        raw.append(lit(".", GREY));
        Component own = LineTranslator.translateChat(StyledText.fromComponent(raw), store);
        String ownText = own == null ? null : own.getString();
        check("自己的暱稱單獨一段也翻（實際 " + ownText + "）",
              "RichaCha 賦予你 25% 甲盾與 8% 乙力。".equals(ownText));
        check("名字之外又出現自己的名字：不能把兩個名字併成一個",
              LineTranslator.translateChat(death("", "Bob", " was killed by RichaCha"), store) == null
                      || !LineTranslator.translateChat(death("", "Bob", " was killed by RichaCha"), store)
                              .getString().contains("被收拾了"));
    }

    /**
     * 七個語言的實際語料都有這幾個句型：翻得出來、名字原樣、英文句型不見了。
     * 只看結構，不看措辭。
     */
    private static void realCorpus() {
        System.out.println("=== 實際語料 ===");
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr")) {
            TranslationStore real = new TranslationStore();
            real.loadAll(base.resolve(lang));
            String[][] cases = {
                {"增益", "has given you"},
                {"單一增益", "has given you"},
                {"加速", "speed boost"},
                {"治療", "gave you"},
                {"死亡快訊", "was minced"},
                {"死亡快訊（沒有頻道圖示）", "was incinerated"},
            };
            StyledText[] lines = {
                buff(lit("Green_teaTW/", AQUA), nick("No Dmg Guardian", AQUA)),
                StyledText.fromComponent(Component.empty()
                        .append(nick("Rabbit_flower", AQUA)).append(lit(" has given you ", GREY))
                        .append(lit("8% strength", AQUA)).append(lit(".", GREY))),
                speed("woody123_is_god/radiance morph"),
                StyledText.fromComponent(Component.empty()
                        .append(nick("Bash", AQUA)).append(lit(" gave you ", GREY))
                        .append(lit("[+5273 ❤]", RED))),
                death("", "Kasyu_pwq", " was minced to bits by the Grootslang."),
                StyledText.fromComponent(Component.empty()
                        .append(lit("JC grindeando", ORANGE)).append(lit(" was incinerated in an instant.", RED))),
            };
            String[] names = {"No Dmg Guardian", "Rabbit_flower", "woody123_is_god/radiance morph",
                              "Bash", "Kasyu_pwq", "JC grindeando"};
            for (int i = 0; i < lines.length; i++) {
                Component hit = LineTranslator.translateChat(lines[i], real);
                String got = hit == null ? null : hit.getString();
                check(lang + "：" + cases[i][0] + "（實際 " + got + "）",
                      got != null && got.indexOf(names[i]) >= 0
                              && got.indexOf(names[i]) == got.lastIndexOf(names[i])
                              && !got.contains(cases[i][1]) && !got.contains("{"));
            }
            // 標題：{#}{#}名字 has been slain
            MutableComponent title = Component.empty();
            title.append(icon(ICON, GOLD));
            title.append(lit("Elaina Lover has been slain", GOLD));
            Component slain = LineTranslator.translate(StyledText.fromComponent(title), real);
            String got = slain == null ? null : slain.getString();
            check(lang + "：死亡快訊的標題（實際 " + got + "）",
                  got != null && got.contains("Elaina Lover") && !got.contains("slain"));
            // 名牌：某人的力量之井
            Component well = LineTranslator.translateLabel(
                    StyledText.fromString("Green_teaTW/RichaCha's Well of Power"), real);
            got = well == null ? null : well.getString();
            check(lang + "：別人的力量之井（實際 " + got + "）",
                  got != null && got.contains("Green_teaTW/RichaCha") && !got.contains("'s Well"));
        }
    }

    // ------------------------------------------------------------ 語料

    private static TranslationStore corpus(Path dir) throws Exception {
        Files.createDirectories(dir);
        JsonObject misc = new JsonObject();
        misc.addProperty(BUFF, "{u}{c:gray} 賦予你 {c:aqua}{~1} 甲盾{c:gray}與 {c:aqua}{~2} 乙力{c:gray}。");
        misc.addProperty("{u} has given you {~} strength.", "{u} 賦予你 {~} 乙力。");
        // 名字排在數值後面：名字裡的數字要是混進 {~}，這一條就會填錯
        misc.addProperty(SPEED, "+{~} 分鐘疾行，來自 {u}。");
        misc.addProperty("{#} {u} was minced to bits by the Grootslang.", "{#} {u} 被巨蛇剁成碎塊。");
        misc.addProperty("{u} has been slain", "{u} 倒地不起");
        misc.addProperty("{#} Unfathomable rancor gushed from {u}.", "{#} 怨念自 {u} 身上湧出。");
        misc.addProperty("{#} {u}'s soul burst into flames.", "{#} {u} 的魂魄燃了起來。");
        misc.addProperty("{#} Not even ash remains of {u}", "{#} {u} 連灰燼都不剩");
        misc.addProperty("{#} {u} was annuled from reality by The Nameless.",
                         "{#} {u} 被無名者自現實中抹消。");
        // 同一句話：伺服器折好行的那條（兩行譯文）排在沒折的那條前面
        misc.addProperty("{#} {u} was removed from the physical plane by\n{#} Octo Beast",
                         "{#} {u} 被八足獸\n{#} 從物質界抹去");
        misc.addProperty("{#} {u} was removed from the physical plane by Octo Beast",
                         "{#} {u} 被八足獸從物質界抹去");
        misc.addProperty("{#} {u} was gunned down by Armored Tank A{~}-L{~}.",
                         "{#} {u} 遭 A{~}-L{~} 型戰車掃射。");
        misc.addProperty("{#} {u} has died", "{#} {u} 斷氣了");
        misc.addProperty("{#} {u} was killed", "{#} {u} 被收拾了");
        misc.addProperty("{#} Guard Captain was killed", "{#} 衛隊長遭到殺害");
        // 句型夠不夠格：名字緊貼著數值、或句子太短的不收
        misc.addProperty("{u}{~} points", "{u}{~} 分");
        misc.addProperty("{u} ok", "{u} 好");
        // 還沒翻的句型：收集端要靠它把名字換成 {u}
        misc.addProperty("{#} {u} was swallowed up by the Black Hole.", "");
        // 名字長得像這些也不能被換掉
        misc.addProperty("Bash", "猛擊（不該出現）");
        Files.writeString(dir.resolve("misc.json"), misc.toString(), StandardCharsets.UTF_8);

        JsonObject label = new JsonObject();
        label.addProperty("{u}'s Well of Power", "{u} 的力之泉");
        label.addProperty("{u}'s Bird", "{u} 的小鳥");
        Files.writeString(dir.resolve("label.json"), label.toString(), StandardCharsets.UTF_8);

        JsonObject terms = new JsonObject();
        terms.addProperty("Bash", "猛擊");
        terms.addProperty("Meteor", "隕石術");
        Files.writeString(dir.resolve("ability-terms.json"), terms.toString(), StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    // ------------------------------------------------------------ 訊息

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static MutableComponent nick(String text, int colour) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(colour)).withItalic(true));
    }

    private static MutableComponent icon(String glyphs, int colour) {
        return Component.literal(glyphs).withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(colour))
                .withFont(new FontDescription.Resource(Identifier.parse("minecraft:chat/prefix"))));
    }

    /** 隊友給的增益，照 log 的分段：名字（斜體）、灰色句子、數值那兩段是青色。 */
    private static StyledText buff(MutableComponent... name) {
        MutableComponent all = Component.empty();
        for (MutableComponent part : name) {
            all.append(part);
        }
        all.append(lit(" has given you ", GREY));
        all.append(lit("29% resistance", AQUA));
        all.append(lit(" and ", GREY));
        all.append(lit("8% strength", AQUA));
        all.append(lit(".", GREY));
        return StyledText.fromComponent(all);
    }

    private static StyledText speed(String name) {
        MutableComponent all = Component.empty();
        all.append(lit(name, AQUA));
        all.append(lit(" gave you ", GREY));
        all.append(lit("+2 minutes", AQUA));
        all.append(lit(" speed boost.", GREY));
        return StyledText.fromComponent(all);
    }

    /** 聊天的死亡快訊：頻道圖示、名字自己一個顏色、句子是紅的。 */
    private static StyledText death(String before, String name, String after) {
        MutableComponent all = Component.empty();
        all.append(icon(ICON, RED));
        all.append(lit(" " + before, RED));
        all.append(lit(name, ORANGE));
        all.append(lit(after, RED));
        return StyledText.fromComponent(all);
    }

    // ------------------------------------------------------------ 檢查

    /** 句型本身：哪些鍵收、名字像不像。 */
    private static void frames(TranslationStore store) {
        System.out.println("=== 句型 ===");
        NameFrames frames = store.nameFrames();
        check("語料的 {u} 鍵建成了句型（實際 " + frames.size() + " 個）", frames.size() >= 10);
        check("{u}{~} 這種分不出名字到哪裡的不收",
              frames.find(StyledText.fromString("Bob12 points"), "Bob{~} points") == null);
        check("太短的句子不收（{u} ok）",
              frames.find(StyledText.fromString("Bob ok"), "Bob ok") == null);

        for (String name : List.of("Green_teaTW/RichaCha", "JC grindeando", "No Dmg Guardian",
                "woody123_is_god/radiance morph", "xX_Pro123_Xx", "1234567", "Bash", "Ab")) {
            check("像名字：" + name, NameFrames.nameLike(name));
        }
        for (String name : List.of("Your pet", "The guard", "Someone: Bob", "Bob.", "a/b/c",
                "one two three four", "", "   ")) {
            check("不像名字：「" + name + "」", !NameFrames.nameLike(name));
        }
    }

    private static void buffs(TranslationStore store) {
        System.out.println("=== 隊友給的增益 ===");
        // ★ 使用者截圖那一句：帳號/暱稱，暱稱斜體
        Component hit = LineTranslator.translateChat(
                buff(lit("Green_teaTW/", AQUA), nick("RichaCha", AQUA)), store);
        String text = hit == null ? null : hit.getString();
        check("★ 帳號/暱稱給的增益翻了（實際 " + text + "）",
              "Green_teaTW/RichaCha 賦予你 29% 甲盾與 8% 乙力。".equals(text));
        check("名字留著它自己的顏色", hit != null && colourOf(hit, "RichaCha") == AQUA);
        check("數值保留原本的顏色", hit != null && colourOf(hit, "29%") == AQUA);
        check("句子是灰的（實際 " + hex(colourOf(hit, "賦予你")) + "）", hit != null && colourOf(hit, "賦予你") == GREY);
        check("「與」是灰的（實際 " + hex(colourOf(hit, "與")) + "）", hit != null && colourOf(hit, "與") == GREY);
        check("譯文沒有殘留的佔位符", text != null && !text.contains("{"));

        for (String name : List.of("No Dmg Guardian", "Bash", "Meteor", "Rabbit_flower", "1234567")) {
            Component one = LineTranslator.translateChat(buff(nick(name, AQUA)), store);
            String got = one == null ? null : one.getString();
            check("「" + name + "」原樣填回（實際 " + got + "）",
                  got != null && got.equals(name + " 賦予你 29% 甲盾與 8% 乙力。"));
        }

        // ★ 名字裡有數字、句型把名字排在數值後面：數字不能跟 +2 搶位置
        Component fast = LineTranslator.translateChat(speed("woody123_is_god/radiance morph"), store);
        String got = fast == null ? null : fast.getString();
        check("★ 名字裡的數字不會被當成數值（實際 " + got + "）",
              "+2 分鐘疾行，來自 woody123_is_god/radiance morph。".equals(got));

        // 一般翻譯那一支（就地取代退到它）也認得
        Component general = LineTranslator.translate(speed("Rabbit_flower"), store);
        check("一般翻譯那一支也認得（實際 " + (general == null ? null : general.getString()) + "）",
              general != null && general.getString().equals("+2 分鐘疾行，來自 Rabbit_flower。"));
    }

    private static void deaths(TranslationStore store) {
        System.out.println("=== 死亡快訊 ===");
        String[][] cases = {
            {"", "Kasyu_pwq", " was minced to bits by the Grootslang.", " Kasyu_pwq 被巨蛇剁成碎塊。"},
            {"Unfathomable rancor gushed from ", "JC grindeando", ".", " 怨念自 JC grindeando 身上湧出。"},
            {"", "Bob", "'s soul burst into flames.", " Bob 的魂魄燃了起來。"},
            {"Not even ash remains of ", "Ab_9", "", " Ab_9 連灰燼都不剩"},
            {"", "Tank9", " was gunned down by Armored Tank A1-L4.", " Tank9 遭 A1-L4 型戰車掃射。"},
        };
        for (String[] c : cases) {
            Component hit = LineTranslator.translateChat(death(c[0], c[1], c[2]), store);
            String got = hit == null ? null : hit.getString();
            check("★ 「" + c[0] + "<" + c[1] + ">" + c[2] + "」（實際 " + got + "）",
                  got != null && got.endsWith(c[3]) && got.indexOf(c[1]) == got.lastIndexOf(c[1]));
        }
        Component minced = LineTranslator.translateChat(
                death("", "Kasyu_pwq", " was minced to bits by the Grootslang."), store);
        check("名字是它自己的顏色", minced != null && colourOf(minced, "Kasyu_pwq") == ORANGE);

        // ★ 伺服器折過行：語料只有一條沒折的
        MutableComponent wrapped = Component.empty();
        wrapped.append(icon(ICON, RED));
        wrapped.append(lit(" ", RED));
        wrapped.append(lit("Elaina Lover", ORANGE));
        wrapped.append(lit(" was annuled from reality by The\n", RED));
        wrapped.append(icon(ICON_NEXT, RED));
        wrapped.append(lit(" Nameless.", RED));
        Component hit = LineTranslator.translateChat(StyledText.fromComponent(wrapped), store);
        String got = hit == null ? null : hit.getString();
        check("★ 折過行的死亡快訊也翻了（實際 " + got + "）",
              got != null && got.contains("Elaina Lover 被無名者自現實中抹消。"));

        // ★ 語料裡同一句有折好行的與沒折的兩條，伺服器卻折在第三個地方：
        //   併句索引要拿沒折的那條，折好行的那條帶著續行圖示，填回去數量對不上
        MutableComponent elsewhere = Component.empty();
        elsewhere.append(icon(ICON, RED));
        elsewhere.append(lit(" ", RED));
        elsewhere.append(lit("Bob", ORANGE));
        elsewhere.append(lit(" was removed from the physical plane by Octo\n", RED));
        elsewhere.append(icon(ICON_NEXT, RED));
        elsewhere.append(lit(" Beast", RED));
        Component third = LineTranslator.translateChat(StyledText.fromComponent(elsewhere), store);
        String thirdText = third == null ? null : third.getString();
        check("★ 折在第三個地方也翻得出來（實際 " + thirdText + "）",
              thirdText != null && thirdText.contains("Bob 被八足獸從物質界抹去"));
        MutableComponent own = Component.empty();
        own.append(icon(ICON, RED));
        own.append(lit(" ", RED));
        own.append(lit("Bob", ORANGE));
        own.append(lit(" was removed from the physical plane by\n", RED));
        own.append(icon(ICON_NEXT, RED));
        own.append(lit(" Octo Beast", RED));
        Component exact = LineTranslator.translateChat(StyledText.fromComponent(own), store);
        String exactText = exact == null ? null : exact.getString();
        check("折在語料那個位置的照舊用那一條（實際 " + exactText + "）",
              exactText != null && exactText.contains("被八足獸\n") && exactText.contains("從物質界抹去"));
    }

    private static void titles(TranslationStore store) {
        System.out.println("=== 標題 ===");
        // 標題是「{#}{#}名字 has been slain」，語料只有沒圖示的那一條
        MutableComponent title = Component.empty();
        title.append(icon(ICON, GOLD));
        title.append(icon(ICON_NEXT, GOLD));
        title.append(lit("Rabbit_flower has been slain", GOLD));
        StyledText line = StyledText.fromComponent(title);
        LineParts framed = LineTranslator.nameFramed(line, store);
        check("★ 標題認得出句型（實際 " + (framed == null ? null : framed.template()) + "）",
              framed != null && framed.template().endsWith("{u} has been slain")
                      && "Rabbit_flower".equals(framed.users().get(0).text()));
        Component hit = framed == null ? null : LineTranslator.translateFramed(line, framed, store);
        String got = hit == null ? null : hit.getString();
        check("★ 標題翻了，名字原樣（實際 " + got + "）",
              got != null && got.endsWith("Rabbit_flower 倒地不起"));
        Component general = LineTranslator.translate(line, store);
        check("一般翻譯那一支也一樣（實際 " + (general == null ? null : general.getString()) + "）",
              general != null && general.getString().endsWith("Rabbit_flower 倒地不起"));
    }

    private static void nameplates(TranslationStore store) {
        System.out.println("=== 名牌 ===");
        for (String[] c : new String[][] {
                {"Bob_12's Well of Power", "Bob_12 的力之泉"},
                {"No Dmg Guardian's Bird", "No Dmg Guardian 的小鳥"},
                {"Green_teaTW/RichaCha's Bird", "Green_teaTW/RichaCha 的小鳥"}}) {
            Component hit = LineTranslator.translateLabel(StyledText.fromString(c[0]), store);
            String got = hit == null ? null : hit.getString();
            check("★ 名牌「" + c[0] + "」（實際 " + got + "）", c[1].equals(got));
        }
    }

    private static void notNames(TranslationStore store) {
        System.out.println("=== 不是名字的不碰 ===");
        check("「Your pet has died」不是某人死了",
              LineTranslator.translateChat(death("", "Your pet", " has died"), store) == null);
        check("一整句話不是名字",
              LineTranslator.translateChat(death("", "Ancient guardian of forest", " has died"), store)
                      == null);
        check("帶冒號的是玩家發言",
              LineTranslator.translateChat(death("", "Someone: Bob", " has died"), store) == null);
        Component guard = LineTranslator.translateChat(death("", "Guard Captain", " was killed"), store);
        check("★ 語料有整句的鍵時整句那條優先（實際 " + (guard == null ? null : guard.getString()) + "）",
              guard != null && guard.getString().contains("衛隊長遭到殺害"));
        check("一般句子不受影響",
              LineTranslator.nameFramed(StyledText.fromString("Welcome to Wynncraft!"), store) == null);
        check("句型沒翻的那一個語言：整句不動（不會只翻一半）",
              LineTranslator.translateChat(death("", "Bob", " was swallowed up by the Black Hole."), store)
                      == null);
    }

    private static void privacy(TranslationStore store, Path dir) throws Exception {
        System.out.println("=== 收集端 ===");
        StyledText line = buff(lit("Green_teaTW/", AQUA), nick("RichaCha", AQUA));
        String framed = com.wynnchayuan.listener.CaptureProbe.chatTemplate(line, store);
        check("★ 收的是 {u} 那一份（實際 " + framed + "）", BUFF.equals(framed));
        check("收的模板裡沒有名字", framed != null && !framed.contains("RichaCha")
                && !framed.contains("Green_teaTW"));
        check("沒有語料可認時照舊整句擋下",
              com.wynnchayuan.listener.CaptureProbe.chatTemplate(line, null) == null);
        String untranslated = com.wynnchayuan.listener.CaptureProbe.chatTemplate(
                death("", "Bob_77", " was swallowed up by the Black Hole."), store);
        check("★ 還沒翻的句型也收成 {u}（實際 " + untranslated + "）",
              "{#} {u} was swallowed up by the Black Hole.".equals(untranslated));

        LineParts title = LineTranslator.nameFramed(StyledText.fromString("Bob_77 has been slain"), store);
        check("標題那條路收到的也沒有名字",
              title != null && !title.template().contains("Bob_77"));

        // 送加速的那一句，沒有句型可認的時候也不能把名字收進來
        check("★ 送加速的句子算夾帶玩家名", PlayerDataFilter.carriesPlayerData(
                "woody{~}_is_god/radiance morph gave you +{~} minutes speed boost."));

        // ★ 收進「沒翻」清單的 {u} 句型，整理時不能被濾網的片語當成個資刪掉
        Path file = dir.resolve("captured.json");
        CaptureStore capture = new CaptureStore(file);
        capture.knowsSources(t -> t.strip().equals(BUFF));
        capture.record(BUFF, "desc", "chat", "chat/INFO");
        capture.flush();
        boolean kept = false;
        JsonObject root = JsonParser.parseString(
                Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        if (root.has("untranslated")) {
            JsonArray rows = root.getAsJsonArray("untranslated");
            for (JsonElement row : rows) {
                kept |= BUFF.equals(row.getAsJsonObject().get("src").getAsString());
            }
        }
        check("★ 沒翻的 {u} 句型留在清單裡（濾網的片語不算個資）", kept);
    }

    // ------------------------------------------------------------ 工具

    private static String hex(int colour) {
        return colour < 0 ? "無" : String.format("#%06X", colour);
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
            System.out.println("名字句型：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("名字句型：全部通過");
    }
}
