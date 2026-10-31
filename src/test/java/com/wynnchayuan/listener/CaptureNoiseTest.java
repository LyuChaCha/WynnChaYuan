package com.wynnchayuan.listener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.translate.FlowedDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class CaptureNoiseTest {

    public static void main(String[] args) throws Exception {
        speakerIsNameOnly();
        ownEchoIsNotCaptured();
        wrappedRowsAreNotCaptured();
        wholeRowsAreStillCaptured();
        if (failures > 0) {
            System.out.println("收集雜訊：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("收集雜訊：全部通過");
    }

    private static String plate() {
        int[] cps = {0xE060, 0xCFFFF, 0xE03D, 0xCFFFF, 0xE03F, 0xCFFFF, 0xE032, 0xCFFFF,
                     0xE062, 0xCFFEC, 0xE00D, 0xE00F, 0xE002, 0xD0002};
        StringBuilder sb = new StringBuilder();
        for (int cp : cps) {
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }

    private static boolean clean(String text) {
        return text != null && text.codePoints().noneMatch(cp -> cp == '\n' || cp >= 0xE000);
    }

    private static void speakerIsNameOnly() throws Exception {
        System.out.println("=== 說話者只留名字 ===");
        String label = "Old Drunk\n" + plate();
        String inline = "Espren Citizen " + plate();

        CurrentQuest.set("A New Beginning");
        String ctx = CurrentQuest.tag("dialogue", label);
        check("★ ctx 只有名字（實際 " + show(ctx) + "）",
              "dialogue/A New Beginning#Old Drunk".equals(ctx));
        check("★ 沒有換行的那一種也一樣（實際 " + show(CurrentQuest.tag("dialogue", inline)) + "）",
              "dialogue/A New Beginning#Espren Citizen".equals(CurrentQuest.tag("dialogue", inline)));
        check("選項那條路也一樣",
              "dialogue/choices/A New Beginning#Old Drunk"
                      .equals(CurrentQuest.tag("dialogue/choices", label)));
        check("乾淨的名字不動",
              "dialogue/A New Beginning#Aledar".equals(CurrentQuest.tag("dialogue", "Aledar")));
        check("名字上面還有一列圖示：拿得到名字",
              "dialogue/A New Beginning#Zeph"
                      .equals(CurrentQuest.tag("dialogue", plate() + "\nZeph\n" + plate())));
        check("整塊都是圖示：當成沒有說話者",
              "dialogue/A New Beginning".equals(CurrentQuest.tag("dialogue", plate())));

        Path dir = Files.createTempDirectory("wynnchayuan-noise");
        Path file = dir.resolve("captured.json");
        CaptureStore store = new CaptureStore(file);
        store.record("Hic! Leave me be, I've nothing left to trade.", "desc", "quest", ctx);
        store.flush();
        JsonObject row = firstEntry(file);
        check("★ 匯出的 speaker 是 Old Drunk（實際 " + show(text(row, "speaker")) + "）",
              "Old Drunk".equals(text(row, "speaker")));
        check("任務名沒被影響", "A New Beginning".equals(text(row, "quest")));
        check("ctx 裡沒有換行與圖示", clean(text(row, "ctx")));
        CurrentQuest.set(null);

        Path old = dir.resolve("old.json");
        JsonObject entry = new JsonObject();
        entry.addProperty("src", "You're not from around here, are you?");
        entry.addProperty("dst", "");
        entry.addProperty("role", "desc");
        entry.addProperty("domain", "quest");
        entry.addProperty("ctx", "dialogue/choices/The Cursed One#Syndra\n" + plate());
        entry.addProperty("seen", 1);
        entry.addProperty("seq", 0);
        JsonObject entries = new JsonObject();
        entries.add("choices/The Cursed One #001", entry);
        JsonObject root = new JsonObject();
        root.add("entries", entries);
        Files.writeString(old, root.toString(), StandardCharsets.UTF_8);
        CaptureStore reloaded = new CaptureStore(old);
        reloaded.record("Something new", "desc", "chat", "chat/INFO");
        reloaded.flush();
        JsonObject kept = firstEntry(old);
        check("★ 舊檔讀回來：speaker 是 Syndra（實際 " + show(text(kept, "speaker")) + "）",
              "Syndra".equals(text(kept, "speaker")));
        check("舊檔讀回來：ctx 也清乾淨了（實際 " + show(text(kept, "ctx")) + "）",
              "dialogue/choices/The Cursed One#Syndra".equals(text(kept, "ctx")));
        check("舊檔讀回來：任務名還在", "choices/The Cursed One".equals(text(kept, "quest")));
    }

    private static StyledText row(String text) {
        return StyledText.fromComponent(Component.literal(text));
    }

    private static void ownEchoIsNotCaptured() throws Exception {
        System.out.println("=== 自己重送的譯文不是原文 ===");
        Path dir = Files.createTempDirectory("wynnchayuan-echo");
        FlowedDebug.init(dir);
        JsonObject corpus = new JsonObject();
        corpus.addProperty("[Quest Completed]", "[Görev Tamamlandı]");
        corpus.addProperty("A Journey Home", "Eve Yolculuk");
        corpus.addProperty("Rewards:", "Ödüller:");
        corpus.addProperty("- +{~} Experience Points", "- +{~} Deneyim Puanı");
        corpus.addProperty("- +{~} Emeralds", "- +{~} Zümrüt");
        corpus.addProperty("The fog is lifting.", "Sis kalkıyor.");
        Files.writeString(dir.resolve("misc.json"), corpus.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        String gap = "- +Access to the Outer Reaches";
        List<StyledText> block = List.of(
                row("[Quest Completed]"), row("A Journey Home"), row("Rewards:"),
                row("- +7000 Experience Points"), row("- +64 Emeralds"), row("            " + gap));

        List<String> notes = new ArrayList<>();
        List<String> captured = new ArrayList<>();
        ChatBlock.clear();
        for (StyledText one : block) {
            String template = CaptureListener.chatTemplate(one, notes::add);
            if (template != null) {
                captured.add(template.strip());
            }
            ChatBlock.queue(one, LineTranslator.translateChat(one, store));
        }
        check("沒翻的那一列在第一次進來時就收到了（實際 " + captured + "）",
              captured.contains(gap));

        Component sent = ChatBlock.preview(store);
        check("整塊的譯文送得出來", sent != null);
        if (sent == null) {
            ChatBlock.clear();
            return;
        }
        String shown = sent.getString();
        check("前提：送出去的那一則是譯文夾著沒翻的原文",
              shown.contains("[Görev Tamamlandı]") && shown.contains("Eve Yolculuk")
                      && shown.contains(gap));
        StyledText echo = StyledText.fromComponent(sent);
        ChatBlock.clear();
        check("前提：不知道是自己送的話，它會被收成一條「原文」",
              CaptureListener.chatTemplate(echo, notes::add) != null);
        notes.clear();
        ChatBlock.remember(sent);
        check("★ 自己送的那一則不收", CaptureListener.chatTemplate(echo, notes::add) == null);
        check("略過的原因記下來了（實際 " + notes + "）",
              notes.contains("chat.skipped.ownOutput"));

        Component event = Component.literal(
                " Arachnid Pusu Dünya Etkinliği 4dk 12sn sonra başlıyor! (312\n"
                + " blok uzakta) Takip için tıkla");
        ChatBlock.remember(event);
        check("★ 整則都是譯文的也不收",
              CaptureListener.chatTemplate(StyledText.fromComponent(event), notes::add) == null);
        Component banner = Component.literal(
                "Welcome to Wynncraft!\nplay.wynncraft.com -/- wynncraft.com\nSis kalkıyor.");
        ChatBlock.remember(banner);
        check("★ 橫幅黏著譯文的那一則也不收",
              CaptureListener.chatTemplate(StyledText.fromComponent(banner), notes::add) == null);

        check("伺服器送來的原文照收",
              "The guards are holding you in place.".equals(CaptureListener.chatTemplate(
                      row("The guards are holding you in place."), notes::add)));
        check("純符號的照舊不收", CaptureListener.chatTemplate(row(" "), notes::add) == null);
        ChatBlock.clear();
    }

    private static final String TITLE = "Area Discovered: {p} (+{~} XP)";

    private static final List<String> AUTUMN = List.of(
            "A shortage of water has caused this forest to exist in",
            "an eternal autumn. The poorest region in Fruma, its",
            "inhabitants manage an economy of their own based on",
            "bartering, allowing them a better quality of life.");

    private static final List<String> BOG = List.of(
            "The waters flowing down from the highlands have",
            "stagnated. The lack of drainage to the ocean has caused",
            "a freshwater swamp to slowly develop in this corner of",
            "the forest. Spooky stories about what lies within the",
            "bog have led to it being an unfavourable place to live.");

    private static CaptureStore storeKnowing(Set<String> keys) throws Exception {
        Path file = Files.createTempDirectory("wynnchayuan-rows").resolve("captured.json");
        CaptureStore store = new CaptureStore(file);
        store.knowsTranslations(t -> keys.contains(t.strip()));
        store.knowsSources(t -> keys.contains(t.strip()));
        store.knowsLonger(t -> {
            String bare = t.strip();
            return keys.stream().anyMatch(k -> k.length() > bare.length() && k.startsWith(bare));
        });
        return store;
    }

    private static void feed(CaptureStore store, List<String> rows, long at) {
        for (String one : rows) {
            store.recordChat(one, "chat/INFO", at);
        }
        store.settleChat(at + 60_000);
    }

    private static List<String> with(String first, List<String> rest) {
        List<String> out = new ArrayList<>();
        out.add(first);
        out.addAll(rest);
        return out;
    }

    private static void wrappedRowsAreNotCaptured() throws Exception {
        System.out.println("=== 折行的句子不收後半截 ===");
        String autumn = String.join(" ", AUTUMN);
        String bog = String.join(" ", BOG);

        Set<String> corpus = new LinkedHashSet<>(List.of(TITLE, autumn, TITLE + "\n" + autumn));
        CaptureStore before = storeKnowing(corpus);
        before.record(TITLE, "desc", "chat", "chat/INFO");
        for (String one : AUTUMN) {
            before.record(one, "desc", "chat", "chat/INFO");
        }
        check("重現：逐列各自記會收到三條半句（實際 " + before.size() + " 條）",
              before.size() == 3 && before.contains(AUTUMN.get(1)) && before.contains(AUTUMN.get(3)));

        CaptureStore store = storeKnowing(corpus);
        feed(store, with(TITLE, AUTUMN), 1_000);
        check("★ 整句在語料裡：一列都不收（實際 " + store.size() + " 條）", store.size() == 0);

        CaptureStore blockOnly = storeKnowing(Set.of(TITLE, TITLE + "\n" + bog));
        feed(blockOnly, with(TITLE, BOG), 1_000);
        check("★ 只有「標題＋整句」那一條也接得起來（實際 " + blockOnly.size() + " 條）",
              blockOnly.size() == 0);

        CaptureStore sentenceOnly = storeKnowing(Set.of(TITLE, bog));
        feed(sentenceOnly, with(TITLE, BOG), 1_000);
        check("★ 只有整句那一條也接得起來（實際 " + sentenceOnly.size() + " 條）",
              sentenceOnly.size() == 0);

        String event = "{#} The Encroaching Blaze World Event starts in {~}s! ({~} blocks away) Click to track";
        CaptureStore iconed = storeKnowing(Set.of(event));
        feed(iconed, List.of("{#} The Encroaching Blaze World Event starts in {~}s! ({~}",
                             "{#} blocks away) Click to track"), 1_000);
        check("續行開頭的圖示不算句子的一部分（實際 " + iconed.size() + " 條）", iconed.size() == 0);

        CaptureStore unknown = storeKnowing(Set.of(TITLE));
        feed(unknown, with(TITLE, AUTUMN), 1_000);
        check("語料沒有的句子四列都收（實際 " + unknown.size() + " 條）",
              unknown.size() == 4 && unknown.contains(AUTUMN.get(0)));

        CaptureStore drifted = storeKnowing(corpus);
        feed(drifted, List.of(TITLE, AUTUMN.get(0), AUTUMN.get(1),
                              "inhabitants barter for what little they need."), 1_000);
        check("後半句對不上：扣著的列一起放出來（實際 " + drifted.size() + " 條）",
              drifted.size() == 3 && drifted.contains(AUTUMN.get(0))
                      && drifted.contains(AUTUMN.get(1)));
    }

    private static void wholeRowsAreStillCaptured() throws Exception {
        System.out.println("=== 完整的一列照收 ===");
        Set<String> corpus = Set.of("[Quest Completed]", "Rewards:",
                "- +{~} Experience Points", "- +Access to the {p} Dungeon");
        String gap = "- +Access to the {p}";

        CaptureStore before = storeKnowing(corpus);
        before.record(gap, "desc", "chat", "chat/INFO");
        check("重現：先前被當成半句丟掉", before.size() == 0);

        CaptureStore store = storeKnowing(corpus);
        long t = 5_000;
        for (String one : List.of("[Quest Completed]", "Rewards:", "- +{~} Experience Points", gap)) {
            store.recordChat(one, "chat/INFO", t);
        }
        check("後面還可能有續行：先扣著", !store.contains(gap));
        store.recordChat("+ New Quest [Undersupply]", "chat/INFO", t);
        check("★ 下一列接不上：那一列是完整的，收", store.contains(gap));
        check("後面那一列也照收", store.contains("+ New Quest [Undersupply]"));
        check("翻好的那幾列沒有混進來（實際 " + store.size() + " 條）", store.size() == 2);

        CaptureStore last = storeKnowing(corpus);
        last.recordChat(gap, "chat/INFO", t);
        last.settleChat(t + 100);
        check("還沒等夠久：繼續扣著", !last.contains(gap));
        last.settleChat(t + 5_000);
        check("★ 等夠久了：放行", last.contains(gap));

        CaptureStore typed = storeKnowing(corpus);
        check("對話打到一半的照舊不收",
              !typed.record(gap, "desc", "quest", "dialogue/x"));
    }

    private static JsonObject firstEntry(Path file) throws Exception {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject entries = JsonParser.parseReader(r).getAsJsonObject()
                    .getAsJsonObject("entries");
            return entries.entrySet().iterator().next().getValue().getAsJsonObject();
        }
    }

    private static String text(JsonObject row, String field) {
        return row.has(field) ? row.get(field).getAsString() : null;
    }

    private static String show(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp == '\n') {
                sb.append("⏎");
            } else if (cp >= 0xE000) {
                sb.append(String.format("<%X>", cp));
            } else {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString();
    }

}
