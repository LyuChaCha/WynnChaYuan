package com.wynnchayuan.translate;

import com.wynnchayuan.capture.Broadcasts;
import com.wynnchayuan.capture.LineParts;
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
 * 「某某人丟了炸彈」這類廣播：句子要翻，<b>名字一個字都不能動</b>。
 *
 * <h2>先前是什麼樣子</h2>
 * 夾著別人名字的伺服器訊息一律原樣放過，所以炸彈鈴整句是英文，同一個世界裡
 * 有人丟炸彈時是「整塊中文裡夾一句 {@code Kasyu_pwq has thrown a}」
 * （使用者 2026-10-10 回報，並要求「務必不要翻譯到玩家」）。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>四種句型都認得，而且名字是從<b>位置</b>收的——名字長得像技能、地名、
 *       炸彈種類、帶數字、帶空白、跨兩個片段，都原樣填回去；</li>
 *   <li>伺服器折行的炸彈鈴（折在 on 前後）跟沒折的是同一條語料；</li>
 *   <li>不是廣播的東西不能被當成廣播：玩家發言、Thank you for your purchase；</li>
 *   <li>認不得的炸彈種類整句不動，不會只翻一半；</li>
 *   <li>六個語言的實際語料都組得出來，名字照樣不動。</li>
 * </ol>
 *
 * <p>訊息的片段照實機 log 的分段組：圖示、名字、種類、世界名各自一段，顏色不同。
 */
public final class BroadcastTest {

    private static int failures = 0;

    private static final int BELL = 0xFDDD5C;
    private static final int BELL_HI = 0xF3E6B2;
    private static final int GREEN = 0xA0C84B;
    private static final int GOLD = 0xFFD750;
    private static final int GREY = 0xAAAAAA;

    /** 行首的頻道圖示（私用區字元，自訂字型）。 */
    private static final String ICON = "";
    private static final String ICON_NEXT = "";

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static MutableComponent icon(String glyphs, int colour) {
        return Component.literal(glyphs).withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(colour))
                .withFont(new FontDescription.Resource(Identifier.parse("minecraft:chat/prefix"))));
    }

    private static MutableComponent offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    /** 炸彈鈴。{@code wrap}：0 不折、1 折在 on 前面、2 折在 on 後面。 */
    private static StyledText bell(String user, String article, String kind, String world, int wrap) {
        MutableComponent all = Component.empty();
        all.append(icon(ICON, BELL));
        all.append(lit(" " + user + " has thrown " + article + " ", BELL));
        all.append(lit(kind + " Bomb", BELL_HI));
        if (wrap == 1) {
            all.append(lit("\n", BELL));
            all.append(icon(ICON_NEXT, BELL));
            all.append(lit(" on ", BELL));
        } else if (wrap == 2) {
            all.append(lit(" on \n", BELL));
            all.append(icon(ICON_NEXT, BELL));
            all.append(lit(" ", BELL));
        } else {
            all.append(lit(" on ", BELL));
        }
        all.append(Component.literal(world).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BELL_HI)).withUnderlined(true)));
        return StyledText.fromComponent(all);
    }

    /** 同一個世界裡有人丟：置中的一列，名字自己一個顏色。 */
    private static StyledText local(String user, String rest) {
        MutableComponent all = Component.empty();
        all.append(icon(ICON_NEXT, GREEN));
        all.append(lit(" ", GREEN));
        all.append(offset(23));
        all.append(lit(user, GOLD));
        all.append(lit(" has thrown a" + rest, GREY));
        return StyledText.fromComponent(all);
    }

    private static StyledText expired(String user, String kind) {
        MutableComponent all = Component.empty();
        all.append(icon(ICON, GREEN));
        all.append(lit(" ", GREEN));
        all.append(lit(user, GOLD));
        all.append(lit(" " + kind + " Bomb has expired! \n", GREEN));
        all.append(icon(ICON_NEXT, GREEN));
        all.append(lit(" Get your own bombs at wynncraft.com/store", GREEN));
        return StyledText.fromComponent(all);
    }

    private static StyledText thank(String user) {
        MutableComponent all = Component.empty();
        all.append(icon(ICON_NEXT, GREEN));
        all.append(lit(" ", GREEN));
        all.append(offset(103));
        all.append(Component.literal("Thank " + user).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(GREY)).withUnderlined(true)));
        return StyledText.fromComponent(all);
    }

    /** 長得像別的東西的名字。每一個都必須原樣出現在譯文裡。 */
    private static final List<String> NAMES = List.of(
            "I_Rexpz_I", "GlitchedSouls", "xX_Pro123_Xx", "1234567", "JC grindeando",
            "Meteor", "Bash", "Totem", "Detlas", "Ragni", "Loot", "Combat Experience",
            "Dungeon", "Bomb_Guy", "Thank", "on NA1", "Emerald", "Guardian", "Wynn");

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-broadcast");
        FlowedDebug.init(dir);

        frames();
        ownCorpus(dir);
        realCorpus();
        report();
    }

    /** 句型本身：認得該認的，不認不該認的。 */
    private static void frames() {
        check("炸彈鈴認得", Broadcasts.spans(" I_Rexpz_I has thrown a Combat Experience Bomb on NA1") != null);
        check("折在 on 前面也認得",
              Broadcasts.spans(" dungeonmaster322 has thrown a Combat Experience Bomb\n on NA4") != null);
        check("折在 on 後面也認得",
              Broadcasts.spans(" threefourteen has thrown a Profession Speed Bomb on \n NA2") != null);
        check("an 開頭的種類", Broadcasts.spans(" someone has thrown an Item Bomb on EU3") != null);
        check("同世界、種類折到下一列", Broadcasts.spans(" Kasyu_pwq has thrown a") != null);
        check("同世界、種類接在同一列",
              Broadcasts.spans(" Gutballeee has thrown a Profession Speed Bomb.") != null);
        check("到期", Broadcasts.spans(" Kasyu_pwq Chest Loot Bomb has expired! \n"
                + " Get your own bombs at wynncraft.com/store") != null);
        check("道謝那一列", Broadcasts.spans(" Thank Kasyu_pwq") != null);

        Broadcasts.Spans s = Broadcasts.spans(" JC grindeando has thrown a Loot Chest Bomb on EU17");
        check("帶空白的暱稱整個算名字", s != null
                && " JC grindeando has thrown a Loot Chest Bomb on EU17"
                        .substring(s.userStart(), s.userEnd()).equals("JC grindeando"));
        check("世界名整個收起來", s != null
                && " JC grindeando has thrown a Loot Chest Bomb on EU17"
                        .substring(s.worldStart(), s.worldEnd()).equals("EU17"));
        Broadcasts.Spans e = Broadcasts.spans(" Epic Loot Dungeon Bomb has expired! \n x");
        check("到期：暱稱結尾剛好是另一種炸彈的名字，名字要取到真正的種類之前（實際 "
                      + (e == null ? "null" : " Epic Loot Dungeon Bomb has expired! \n x"
                              .substring(e.userStart(), e.userEnd())) + "）",
              e != null && e.userEnd() - e.userStart() == "Epic Loot".length());

        // ★ 不是廣播的
        check("玩家發言不算（名字後面有冒號）",
              Broadcasts.spans(" Someone: I_Rexpz_I has thrown a Combat Experience Bomb on NA1") == null);
        check("Thank you for your purchase! 不是道謝那一列",
              Broadcasts.spans(" Thank you for your purchase!") == null);
        check("by viewers like you. Thank you. 也不是",
              Broadcasts.spans(" by viewers like you. Thank you.") == null);
        check("到期：認不得的種類不動",
              Broadcasts.spans(" Someone Mystery Bomb has expired! \n x") == null);
        check("一般句子不算", Broadcasts.spans(" Welcome to Wynncraft!") == null);
        check("空的不算", Broadcasts.spans("   ") == null && Broadcasts.spans(null) == null);
    }

    /** 自己寫的小語料：措辭跟實際語料無關，只看機制。 */
    private static void ownCorpus(Path dir) throws Exception {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("{#} {u} has thrown a Combat Experience Bomb on {~}",
                         "{#} {u}{c1} 於 {~} 擲出{c2}甲種炸彈");
        root.addProperty("{#} {u} has thrown a Profession Speed Bomb on {~}",
                         "{#} {u}{c1} 於 {~} 擲出{c2}乙種炸彈");
        root.addProperty("{#} {#}{u} has thrown a", "{#} {#}{u} 擲出了");
        root.addProperty("{#} {#}{u} has thrown a Profession Speed Bomb.",
                         "{#} {#}{u} 擲出了乙種炸彈。");
        root.addProperty("{#} {u} Chest Loot Bomb has expired! \n"
                + "{#} Get your own bombs at wynncraft.com/store",
                         "{#} {u} 的丙種炸彈沒了！\n{#} 自己去 wynncraft.com/store 買");
        root.addProperty("{#} {#}Thank {u}", "{#} {#}謝謝 {u}");
        // 名字長得像這些也不能被換掉
        root.addProperty("Combat Experience", "戰鬥經驗");
        root.addProperty("Dungeon", "地城");
        root.addProperty("Loot", "寶物");
        root.addProperty("Thank", "感謝");
        Files.writeString(dir.resolve("misc.json"), root.toString(), StandardCharsets.UTF_8);
        com.google.gson.JsonObject terms = new com.google.gson.JsonObject();
        terms.addProperty("Meteor", "隕石術");
        terms.addProperty("Bash", "猛擊");
        terms.addProperty("Totem", "圖騰");
        terms.addProperty("Guardian", "守護者");
        Files.writeString(dir.resolve("ability-terms.json"), terms.toString(),
                          StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        for (String name : NAMES) {
            every(name, store, "小語料", List.of("擲出", "的丙種炸彈沒了", "謝謝"));
        }

        // 模板：名字是 {u}、世界名是一個 {~}，折行的那兩種多一個續行圖示
        check("模板（實際 " + Broadcasts.parts(bell("A_b", "a", "Combat Experience", "NA1", 0)).template() + "）",
              "{#} {u} has thrown a Combat Experience Bomb on {~}".equals(
                      Broadcasts.parts(bell("A_b", "a", "Combat Experience", "NA1", 0)).template()));
        LineParts spaced = Broadcasts.parts(bell("12 cats 9", "a", "Combat Experience", "AS22", 0));
        check("名字裡的數字不會被抽成 {~}（實際 " + spaced.template() + "）",
              spaced.numbers().size() == 1 && "AS22".equals(spaced.numbers().get(0).text())
                      && spaced.users().size() == 1
                      && "12 cats 9".equals(spaced.users().get(0).text()));

        // ★ 名字跨兩個片段（Wynntils 把暱稱改寫成「帳號名/暱稱」，後半斜體）
        MutableComponent split = Component.empty();
        split.append(icon(ICON_NEXT, GREEN));
        split.append(lit(" ", GREEN));
        split.append(offset(17));
        split.append(lit("ImagineKami/", GOLD));
        split.append(Component.literal("Elaina Lover").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(GOLD)).withItalic(true)));
        split.append(lit(" has thrown a Profession Speed Bomb.", GREY));
        Component two = LineTranslator.translateChat(StyledText.fromComponent(split), store);
        check("名字跨兩個片段也翻得出來（實際 " + (two == null ? "null" : two.getString()) + "）",
              two != null && two.getString().contains("ImagineKami/Elaina Lover 擲出了乙種炸彈。"));

        // 顏色：炸彈名照 {c2} 是淺色那一個，名字留著它自己的顏色
        Component belled = LineTranslator.translateChat(
                bell("I_Rexpz_I", "a", "Combat Experience", "NA1", 0), store);
        if (belled != null) {
            StringBuilder runs = new StringBuilder();
            belled.visit((style, text) -> {
                runs.append("[").append(style.getColor() == null ? "-" : style.getColor().serialize())
                    .append(style.isUnderlined() ? "u" : "").append(" ").append(text).append("]");
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println("      分段：" + runs);
        }
        check("炸彈名是原文第二個顏色", belled != null && colourOf(belled, "甲種炸彈") == BELL_HI);
        check("句子本身是第一個顏色", belled != null && colourOf(belled, "擲出") == BELL);
        Component here = LineTranslator.translateChat(local("Kasyu_pwq", ""), store);
        check("同世界那一列：名字還是金色", here != null && colourOf(here, "Kasyu_pwq") == GOLD);
        check("譯文沒有殘留的顏色記號", belled != null && !belled.getString().contains("{"));

        // ★ 認不得的炸彈種類：整句不動（回 null），不能只翻一半
        Component unknown = LineTranslator.translateChat(
                bell("I_Rexpz_I", "a", "Mystery Flavour", "NA1", 0), store);
        check("語料沒有的種類整句不動（實際 " + (unknown == null ? "null" : unknown.getString()) + "）",
              unknown == null);

        // 收集端那一關沒有因此放行：這幾句照樣不進 captured.json
        check("收集端照舊擋下炸彈鈴", PlayerDataFilter.carriesPlayerData(
                com.wynnchayuan.capture.GlyphSplitter.toTemplate(
                        bell("I_Rexpz_I", "a", "Combat Experience", "NA1", 0))));
        check("收集端照舊擋下道謝那一列", PlayerDataFilter.carriesPlayerData(
                com.wynnchayuan.capture.GlyphSplitter.toTemplate(thank("Kasyu_pwq"))));
        check("收集端擋下死亡的大字標題", PlayerDataFilter.carriesPlayerData(
                "PoorChaCha was butchered by Dead Lumberjack."));
    }

    /** 六個語言的實際語料：每一種句型都組得出來，名字不動、沒有英文句子殘留。 */
    private static void realCorpus() {
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(base.resolve(lang));
            for (String name : NAMES) {
                every(name, store, lang, List.of());
            }
            for (String kind : List.of("Combat Experience", "Profession Experience",
                    "Profession Speed", "Loot", "Loot Chest", "Dungeon")) {
                Component hit = LineTranslator.translateChat(
                        bell("I_Rexpz_I", "a", kind, "EU17", 0), store);
                check(lang + "：炸彈鈴 " + kind + "（實際 "
                              + (hit == null ? "null" : hit.getString()) + "）",
                      hit != null && hit.getString().contains("I_Rexpz_I")
                              && hit.getString().contains("EU17")
                              && !hit.getString().contains("has thrown")
                              && !hit.getString().contains("{"));
            }
        }
    }

    /** 同一個名字走過每一種句型：翻得出來、名字原樣出現一次、英文句型不見了。 */
    private static void every(String name, TranslationStore store, String tag, List<String> want) {
        List<StyledText> rows = List.of(
                bell(name, "a", "Combat Experience", "NA1", 0),
                bell(name, "a", "Combat Experience", "NA1", 1),
                bell(name, "a", "Profession Speed", "EU17", 2),
                local(name, ""),
                local(name, " Profession Speed Bomb."),
                expired(name, "Chest Loot"),
                thank(name));
        String[] what = {"炸彈鈴", "炸彈鈴（折在 on 前）", "炸彈鈴（折在 on 後）", "同世界",
                "同世界（整句）", "到期", "道謝"};
        for (int i = 0; i < rows.size(); i++) {
            Component hit = LineTranslator.translateChat(rows.get(i), store);
            String text = hit == null ? null : hit.getString();
            boolean ok = text != null && once(text, name)
                    && !text.contains("has thrown") && !text.contains("has expired")
                    && !text.contains("{");
            if (ok && !want.isEmpty()) {
                boolean any = false;
                for (String w : want) {
                    any |= text.contains(w);
                }
                ok = any;
            }
            if (!ok) {
                check(tag + "：「" + name + "」" + what[i] + "（實際 " + text + "）", false);
            }
        }
        check(tag + "：「" + name + "」七種句型都翻了、名字原樣", true);
    }

    /** 名字原樣出現，而且句子裡沒有多出第二份（被別的規則又貼了一次）。 */
    private static boolean once(String text, String name) {
        int at = text.indexOf(name);
        return at >= 0 && text.indexOf(name, at + name.length()) < 0;
    }

    /** 譯文裡某一段字的顏色；找不到回 -1。 */
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
            System.out.println("廣播句型：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("廣播句型：全部通過");
    }
}
