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

/**
 * 聊天形式的 NPC 台詞（{@code [1/3] Obsessor: You need to come with us!}）拆成三段組
 * （見 {@link LineTranslator#npcChatLine}）。
 *
 * <h2>先前是什麼樣子</h2>
 * 台詞本身在任務檔裡有譯文（對話框裡就是翻好的），可是聊天多了計數與說話者，
 * 查的是整句鍵——語料沒有那一條就整句英文。capture 一次收了六十三句這種，
 * 一半的台詞任務檔裡早就有了。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>台詞照任務檔的譯文，說話者照既有聊天鍵的叫法（沒有就查同名鍵、再沒有就原文），
 *       計數原樣，三段的顏色照原文；</li>
 *   <li>說話者帶數值時，數值各就各位；</li>
 *   <li>整句鍵照樣優先；台詞查不到就整句不動；不是這種句子的不碰；</li>
 *   <li>收集端問「翻了沒」時這種句子算翻了；</li>
 *   <li>七個語言的實際語料：任務檔裡有台詞的那幾句都翻得出來。</li>
 * </ol>
 *
 * <p>語料是測試自己寫的，措辭跟實際語料無關。
 */
public final class NpcChatLineTest {

    private static int failures = 0;

    private static final int GREY = 0xAAAAAA;
    private static final int DARK_GREEN = 0x00AA00;
    private static final int GREEN = 0x55FF55;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-npc-chat");
        FlowedDebug.init(dir);
        TranslationStore store = corpus(dir.resolve("corpus"));

        composes(store);
        guards(store);
        realCorpus();
        report();
    }

    private static TranslationStore corpus(Path dir) throws Exception {
        Files.createDirectories(dir);
        // 任務檔：台詞的鍵就是台詞本身
        JsonObject entries = new JsonObject();
        entry(entries, "T#1", "You need to come with us!", "測：你得跟我們走！", "Obsessor");
        entry(entries, "T#2", "I was made to be angry. I can't stop myself...", "測：我生來就憤怒。", "Big boss Mech-DM2");
        entry(entries, "T#3", "Begin, the Air Division!", "測：風之部隊，開始！", "Qira");
        entry(entries, "T#4", "Keep fighting!", "測：繼續打！", "Jesp");
        JsonObject quest = new JsonObject();
        quest.add("entries", entries);
        Files.writeString(dir.resolve("quest-dialogue.json"), quest.toString(), StandardCharsets.UTF_8);

        JsonObject misc = new JsonObject();
        // 既有的整句聊天鍵：說話者的叫法從這裡學
        misc.addProperty("[{~}/{~}] Obsessor: It's the only way to make us feel whole!",
                "[{~}/{~}] 測執念者: 測：這是唯一的辦法！");
        misc.addProperty("[{~}/{~}] Obsessor: YOU STAY OUT OF THIS!!", "[{~}/{~}] 測執念者: 測：你別插手！！");
        // 整句鍵：照樣優先
        misc.addProperty("[{~}/{~}] Qira: Begin, the Air Division!", "[{~}/{~}] 測琪拉: 整句鍵：風之部隊！");
        // 同名的 NPC 名牌
        misc.addProperty("Jesp", "測傑斯普");
        Files.writeString(dir.resolve("misc.json"), misc.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static void entry(JsonObject into, String id, String src, String dst, String speaker) {
        JsonObject e = new JsonObject();
        e.addProperty("src", src);
        e.addProperty("dst", dst);
        e.addProperty("role", "desc");
        e.addProperty("kind", "dialogue");
        e.addProperty("quest", "Test Quest");
        e.addProperty("speaker", speaker);
        into.add(id, e);
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    /** 照 log 的分段：{@code &7[1/3] &2Obsessor: &aYou need to come with us!} */
    private static StyledText chat(String counter, String speaker, String line) {
        return StyledText.fromComponent(Component.empty()
                .append(lit(counter + " ", GREY))
                .append(lit(speaker + ": ", DARK_GREEN))
                .append(lit(line, GREEN)));
    }

    private static void composes(TranslationStore store) {
        System.out.println("=== 拆成三段組 ===");
        Component out = LineTranslator.translateChat(chat("[2/5]", "Obsessor", "You need to come with us!"), store);
        String got = out == null ? null : out.getString();
        check("★ 台詞照任務檔、說話者照既有聊天鍵（實際 " + got + "）",
              "[2/5] 測執念者: 測：你得跟我們走！".equals(got));
        check("計數是灰色", out != null && colourOf(out, "[") == GREY && colourOf(out, "/") == GREY);
        check("說話者是深綠色", out != null && colourOf(out, "測執念者") == DARK_GREEN);
        check("台詞是綠色", out != null && colourOf(out, "跟我們走") == GREEN);

        Component named = LineTranslator.translateChat(chat("[1/2]", "Jesp", "Keep fighting!"), store);
        got = named == null ? null : named.getString();
        check("★ 沒有聊天鍵的說話者：照同名的鍵（實際 " + got + "）", "[1/2] 測傑斯普: 測：繼續打！".equals(got));

        Component mech = LineTranslator.translateChat(
                chat("[3/4]", "Big boss Mech-DM2", "I was made to be angry. I can't stop myself..."), store);
        got = mech == null ? null : mech.getString();
        check("★ 說話者帶數值、而且查不到叫法：原文留著，數值各就各位（實際 " + got + "）",
              "[3/4] Big boss Mech-DM2: 測：我生來就憤怒。".equals(got));

        Component general = LineTranslator.translate(chat("[2/5]", "Obsessor", "You need to come with us!"), store);
        check("一般翻譯那一支也認得（實際 " + (general == null ? null : general.getString()) + "）",
              general != null && general.getString().contains("測：你得跟我們走！"));

        check("★ 收集端：這種句子算翻了",
              store.hasTranslation("[{~}/{~}] Obsessor: You need to come with us!"));
    }

    private static void guards(TranslationStore store) {
        System.out.println("=== 不該動的不動 ===");
        Component whole = LineTranslator.translateChat(chat("[1/1]", "Qira", "Begin, the Air Division!"), store);
        check("★ 整句鍵照樣優先（實際 " + (whole == null ? null : whole.getString()) + "）",
              whole != null && whole.getString().contains("整句鍵：風之部隊！"));
        check("台詞查不到：整句不動", LineTranslator.translateChat(
                chat("[1/3]", "Obsessor", "This line is nowhere in the corpus."), store) == null);
        check("台詞查不到：收集端照樣記成缺口",
              !store.hasTranslation("[{~}/{~}] Obsessor: This line is nowhere in the corpus."));
        check("沒有計數的不碰（證據裡沒有這種）",
              LineTranslator.npcChatLine("Obsessor: You need to come with us!", store) == null);
        check("說話者是空的不碰", LineTranslator.npcChatLine("[{~}/{~}] : You need to come with us!", store) == null);
        check("計數之後不是「名字: 台詞」的不碰",
              LineTranslator.npcChatLine("[{~}/{~}] Crystals Destroyed", store) == null);
        check("多行的不碰", LineTranslator.npcChatLine(
                "[{~}/{~}] Obsessor: You need to come with us!\nPress SHIFT to continue", store) == null);
        check("沒有東西的不碰", LineTranslator.npcChatLine(null, store) == null
                && LineTranslator.npcChatLine("", store) == null);
    }

    /** 七個語言：capture 裡的這幾句，台詞都在任務檔裡。 */
    private static void realCorpus() {
        System.out.println("=== 實際語料 ===");
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        String[][] lines = {
                {"Qira", "Begin, the Thunder Division! Strike quickly and move on, for those within will warp "
                        + "your very mind should you stray for too long!"},
                {"Nettik", "I'm sorry it ended up like this, you should've just ignored that old man."},
                {"Obsessor", "You need to come with us!"},
                {"Scientist Ynnos", "*Ahem* Welcome, everyone."},
                {"Axelus", "Ah! Don't let them get too close!"},
        };
        for (String lang : new String[] {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"}) {
            TranslationStore real = new TranslationStore();
            real.loadAll(base.resolve(lang));
            for (String[] l : lines) {
                Component out = LineTranslator.translateChat(chat("[1/3]", l[0], l[1]), real);
                String got = out == null ? null : out.getString();
                String head = l[1].substring(0, Math.min(12, l[1].length()));
                check(lang + "：" + l[0] + "（實際 " + got + "）",
                      got != null && got.startsWith("[1/3] ") && !got.contains(head) && !got.contains("{"));
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
            System.out.println("聊天形式的 NPC 台詞：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("聊天形式的 NPC 台詞：全部通過");
    }
}
