package com.wynnchayuan.listener;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.DiscoveryBlock;
import com.wynnchayuan.translate.FlowedDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.SpaceOffset;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DiscoveryBlockTest {

    private static final Path TRANSLATIONS = Path.of("src/main/resources/assets/wynnchayuan/translations");

    private static final String[] LANGUAGES = {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"};

    private static final String ISSUE_SENTENCE = "Between Realms of physicality and influence lies the"
            + " vast nothingness of the void. Some islands have broken from the lands and drift"
            + " in this space, seeking purpose.";

    private static final String[] ISSUE_ROWS = {
        "Between Realms of physicality and influence lies the",
        "vast nothingness of the void. Some islands have broken",
        "from the lands and drift in this space, seeking",
        "purpose."};

    private static int advance(int cp) {
        if (cp == '\n') {
            return 0;
        }
        if (cp >= 0x1100 && cp != 0x2019) {
            return 9;
        }
        return switch (cp) {
            case '!', ',', '.', ':', ';', 'i', '|' -> 2;
            case '\'', '`', 'l' -> 3;
            case ' ', 'I', 't', '[', ']' -> 4;
            case 'f', 'k', '(', ')', '<', '>', '"', '*' -> 5;
            default -> 6;
        };
    }

    private static int measure(Component component) {
        int[] width = {0};
        component.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                width[0] += SpaceOffset.decode(text);
            } else {
                text.codePoints().forEach(cp -> width[0] += advance(cp));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return width[0];
    }

    private static int plainWidth(String text) {
        return measure(Component.literal(text));
    }

    private static MutableComponent padded(int width) {
        int spaces = Math.max(0, Math.round((150 - width / 2f) / 4.75f));
        return Component.literal(" ".repeat(spaces));
    }

    private static StyledText prose(String text) {
        MutableComponent row = padded(plainWidth(text));
        row.append(Component.literal(text).withStyle(ChatFormatting.GRAY));
        return StyledText.fromComponent(row);
    }

    private static StyledText head(String label, String name, String xp) {
        MutableComponent row = padded(plainWidth(label + name + xp));
        boolean secret = label.startsWith("Secret");
        row.append(Component.literal(label).withStyle(secret ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY));
        row.append(Component.literal(name).withStyle(secret ? ChatFormatting.AQUA : ChatFormatting.WHITE));
        row.append(Component.literal(xp).withStyle(ChatFormatting.LIGHT_PURPLE));
        return StyledText.fromComponent(row);
    }

    private static StyledText title(String text) {
        MutableComponent row = padded(plainWidth(text));
        row.append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        return StyledText.fromComponent(row);
    }

    private static StyledText blank() {
        return StyledText.fromComponent(Component.literal(" "));
    }

    private static List<StyledText> issueBlock() {
        List<StyledText> rows = new ArrayList<>();
        rows.add(head("Secret Discovery: ", "Space Between Realities", " (+1500000 XP)"));
        rows.add(title("Silent Expanse [8/9]"));
        rows.add(blank());
        for (String row : ISSUE_ROWS) {
            rows.add(prose(row));
        }
        return rows;
    }

    private static List<String> wrapped(String sentence) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : sentence.split(" ")) {
            if (row.length() > 0 && plainWidth(row + " " + word) > 295) {
                rows.add(row.toString());
                row.setLength(0);
            }
            if (row.length() > 0) {
                row.append(' ');
            }
            row.append(word);
        }
        rows.add(row.toString());
        return rows;
    }

    private static Component held(List<StyledText> rows, TranslationStore store) {
        ChatBlock.clear();
        for (StyledText row : rows) {
            ChatBlock.hold(row, false);
        }
        Component out = ChatBlock.preview(store);
        ChatBlock.clear();
        return out;
    }

    private static Component queued(List<StyledText> rows, TranslationStore store) {
        ChatBlock.clear();
        for (StyledText row : rows) {
            if (!GlyphSplitter.isGlyphOnly(row)) {
                ChatBlock.queue(row, null);
            }
        }
        Component out = ChatBlock.preview(store);
        ChatBlock.clear();
        return out;
    }

    private static List<Component> shown(Component whole) {
        List<Component> rows = new ArrayList<>();
        MutableComponent[] row = {Component.empty()};
        whole.visit((style, text) -> {
            String[] parts = text.split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    rows.add(row[0]);
                    row[0] = Component.empty();
                }
                if (!parts[i].isEmpty()) {
                    row[0].append(Component.literal(parts[i]).withStyle(style));
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        rows.add(row[0]);
        return rows;
    }

    private static int lead(Component row) {
        int[] px = {0};
        boolean[] done = {false};
        row.visit((style, text) -> {
            if (done[0]) {
                return Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                px[0] += SpaceOffset.decode(text);
                return Optional.empty();
            }
            int n = 0;
            while (n < text.length() && text.charAt(n) == ' ') {
                n++;
            }
            px[0] += n * 4;
            done[0] = n < text.length();
            return Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    private static boolean onCentreLine(Component row) {
        int lead = lead(row);
        int body = measure(row) - lead;
        return body >= 309 ? lead == 0 : Math.abs(2 * lead + body - 309) <= 2;
    }

    private static String visible(Component row) {
        StringBuilder out = new StringBuilder();
        row.visit((style, text) -> {
            if (!SpaceOffset.isSpaceFont(style)) {
                out.append(text);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString().strip();
    }

    private static JsonObject corpus(String language) throws Exception {
        return JsonParser.parseString(Files.readString(
                TRANSLATIONS.resolve(language).resolve("discovery.json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static TranslationStore real(String language) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(TRANSLATIONS.resolve(language));
        return store;
    }

    private static TranslationStore synthetic(JsonObject root) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-discovery");
        Files.writeString(dir.resolve("discovery.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    public static void main(String[] args) throws Exception {
        Field width = LineTranslator.class.getDeclaredField("measureForTest");
        width.setAccessible(true);
        width.set(null, (ToIntFunction<Component>) DiscoveryBlockTest::measure);
        FlowedDebug.init(Files.createTempDirectory("wynnchayuan-discovery"));

        holdStarts();
        parsing();
        issueShape();
        rowCounts();
        missKeepsEnglish();
        capture();
        everyEntry();

        System.out.println(failures == 0 ? "探索點段落：全部通過" : "探索點段落：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void holdStarts() {
        check("★ 探索點標頭會開始攔（就地取代才併得了句）",
              ChatListener.opensHold(head("Secret Discovery: ", "Necklace Chamber", " (+1700000 XP)")));
        check("★ 地區探索點標頭也會",
              ChatListener.opensHold(head("Area Discovered: ", "The Mistwoods", " (+1000000 XP)")));
        check("內文列不會自己開始攔", !ChatListener.opensHold(prose("purpose.")));
        check("其他單欄訊息不攔", !ChatListener.opensHold(prose("[You are now entering Aldwell]")));
    }

    private static void parsing() {
        List<StyledText> rows = new ArrayList<>();
        rows.add(head("Area Discovered: ", "Fort Torann", " (+510000 XP)"));
        rows.add(blank());
        rows.add(prose("Behind the Royal Gate lies the largest military"));
        rows.add(prose("must pass through it."));
        rows.add(StyledText.fromComponent(Component.literal("[You are now entering Fort Torann]")
                .withStyle(ChatFormatting.GRAY)));
        List<DiscoveryBlock.Block> blocks = DiscoveryBlock.blocks(rows);
        check("一個標頭一塊", blocks.size() == 1);
        check("內文是空行後面那兩列（靠左的進城提示不算）",
              blocks.get(0).from() == 2 && blocks.get(0).to() == 4);

        List<StyledText> noProse = List.of(head("Area Discovered: ", "Espren", " (+7000 XP)"), blank(),
                StyledText.fromComponent(Component.literal("[You are now entering Espren]")
                        .withStyle(ChatFormatting.GRAY)));
        check("★ 只有標頭沒有內文（進城提示不當內文）",
              !DiscoveryBlock.blocks(noProse).get(0).hasProse());

        List<StyledText> mixed = new ArrayList<>(rows.subList(0, 3));
        mixed.add(title("Silent Expanse's Secret is somewhere around here, or so they say."));
        check("★ 顏色不同的列不算進內文", DiscoveryBlock.blocks(mixed).get(0).to() == 3);

        List<StyledText> two = new ArrayList<>(issueBlock());
        two.addAll(issueBlock());
        check("連著兩塊各算各的", DiscoveryBlock.blocks(two).size() == 2);
    }

    private static void issueShape() throws Exception {
        for (String language : LANGUAGES) {
            TranslationStore store = real(language);
            String want = corpus(language).get(ISSUE_SENTENCE).getAsString();
            for (boolean replace : new boolean[] {true, false}) {
                String mode = language + (replace ? " 就地取代" : " 原文加譯文");
                Component out = replace ? held(issueBlock(), store) : queued(issueBlock(), store);
                check(mode + "：#1588 那一塊送得出來", out != null);
                if (out == null) {
                    continue;
                }
                List<Component> rows = shown(out);
                int first = replace ? 3 : 2;
                StringBuilder prose = new StringBuilder();
                boolean centred = true;
                boolean fits = true;
                boolean grey = true;
                for (Component row : rows.subList(first, rows.size())) {
                    prose.append(visible(row)).append(' ');
                    centred &= onCentreLine(row);
                    fits &= measure(row) - lead(row) <= 300;
                    grey &= colourIs(row, ChatFormatting.GRAY);
                }
                String flat = prose.toString().replace(" ", "");
                check("★ " + mode + "：譯文是整句（實際 " + prose + "）",
                      flat.equals(want.replace(" ", "")));
                check("★ " + mode + "：沒有英文殘留", !out.getString().contains("nothingness")
                        && !out.getString().contains("purpose."));
                check("★ " + mode + "：每一列都在置中線上", centred);
                check(mode + "：每一列塞得下", fits);
                check(mode + "：內文照原文灰色", grey);
                check("★ " + mode + "：標題列留著而且在置中線上（實際 " + visible(rows.get(1)) + "）",
                      visible(rows.get(1)).contains("[8/9]") && onCentreLine(rows.get(1)));
                check("★ " + mode + "：標頭在置中線上", onCentreLine(rows.get(0)));
                check("★ " + mode + "：標頭翻出來（實際 " + visible(rows.get(0)) + "）",
                      !visible(rows.get(0)).startsWith("Secret Discovery:"));
                check(mode + "：標頭、標題、內文各自一列（有 " + rows.size() + " 列）", rows.size() > first);
            }
        }
    }

    private static void rowCounts() throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty(ISSUE_SENTENCE, "短短一句。");
        Component shorter = held(issueBlock(), synthetic(root));
        List<Component> rows = shown(shorter);
        check("★ 譯文短就少幾列，不留空列（實際 " + rows.size() + " 列）", rows.size() == 4);
        check("短譯文在置中線上", onCentreLine(rows.get(3)));

        root = new JsonObject();
        root.addProperty(ISSUE_SENTENCE, ("這是一句比原文長很多的譯文，用來確認列數會自己變多，"
                + "而且每一列都不會超過聊天視窗。").repeat(6));
        Component longer = held(issueBlock(), synthetic(root));
        rows = shown(longer);
        boolean fits = true;
        boolean centred = true;
        for (Component row : rows.subList(3, rows.size())) {
            fits &= measure(row) - lead(row) <= 300;
            centred &= onCentreLine(row);
        }
        check("★ 譯文長就多幾列（實際 " + (rows.size() - 3) + " 列內文）", rows.size() - 3 > ISSUE_ROWS.length);
        check("★ 多出來的列也塞得下", fits);
        check("多出來的列也在置中線上", centred);

        root = new JsonObject();
        root.addProperty("Long ago, the Avos hid away a powerful necklace. Near {p}, too.",
                         "很久以前，Avos 在 {p} 附近藏起了項鍊。");
        List<StyledText> block = new ArrayList<>();
        block.add(head("Secret Discovery: ", "Necklace Chamber", " (+1700000 XP)"));
        block.add(blank());
        block.add(prose("Long ago, the Avos hid away a powerful necklace. Near"));
        block.add(prose("Ragni, too."));
        Component named = held(block, synthetic(root));
        check("★ 地名佔位符跟著整句換（實際 " + named.getString() + "）", named.getString().contains("Ragni"));

        root = new JsonObject();
        root.addProperty("Area Discovered: The {p} (+{~} XP)\nA dense fog covers this vast forest.",
                         "發現: {p} (+{~} XP)\n濃霧籠罩這片森林。");
        block = new ArrayList<>();
        block.add(head("Area Discovered: ", "The Mistwoods", " (+1000000 XP)"));
        block.add(blank());
        block.add(prose("A dense fog covers this vast forest."));
        Component two = held(block, synthetic(root));
        check("★ 只有含換行的條目也查得到（實際 " + two.getString() + "）",
              two.getString().contains("濃霧籠罩這片森林"));
        check("含換行的條目只取內文那一列", !two.getString().contains("發現:"));
    }

    private static void missKeepsEnglish() throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty("purpose.", "目的。");
        root.addProperty("Between Realms of physicality and influence lies the", "半句。");
        Component out = held(issueBlock(), synthetic(root));
        check("★ 查不到整句就整段留英文，不逐列翻（實際 " + out.getString() + "）",
              out.getString().contains("purpose.") && !out.getString().contains("目的")
                      && !out.getString().contains("半句"));
    }

    private static void capture() throws Exception {
        TranslationStore empty = synthetic(new JsonObject());
        CaptureStore store = new CaptureStore(
                Files.createTempDirectory("wynnchayuan-discovery").resolve("captured.json"));
        DiscoveryRows rows = new DiscoveryRows();
        List<StyledText> block = issueBlock();
        long now = 1000;
        for (StyledText row : block) {
            if (!rows.offer(row, "chat/INFO", now++, store, empty)) {
                store.recordChat(CaptureListener.chatTemplate(row, s -> {}, empty), "chat/INFO", now);
            }
        }
        rows.settle(now + DiscoveryRows.QUIET_MS + 1, store, empty);
        store.settleChat(now + 5000);
        store.flush();
        JsonObject entries = JsonParser.parseReader(Files.newBufferedReader(
                captureFile(store))).getAsJsonObject().getAsJsonObject("entries");
        List<String> sources = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : entries.entrySet()) {
            sources.add(e.getValue().getAsJsonObject().get("src").getAsString());
        }
        check("★ 沒翻到的段落收成一整句（實際 " + sources + "）", sources.contains(ISSUE_SENTENCE));
        for (String row : ISSUE_ROWS) {
            check("★ 不收半句：" + row, !sources.contains(row));
        }
        for (JsonElement e : entries.asMap().values()) {
            JsonObject entry = e.getAsJsonObject();
            if (ISSUE_SENTENCE.equals(entry.get("src").getAsString())) {
                check("整句記著屬於哪個探索點", entry.get("in").getAsString().contains("Space Between Realities"));
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty(ISSUE_SENTENCE, "整句譯文。");
        TranslationStore known = synthetic(root);
        CaptureStore second = new CaptureStore(
                Files.createTempDirectory("wynnchayuan-discovery").resolve("captured.json"));
        DiscoveryRows others = new DiscoveryRows();
        now = 1000;
        for (StyledText row : block.subList(1, block.size())) {
            check("標頭之前的列不收著", !others.offer(row, "chat/INFO", now++, second, known));
        }
        others.offer(block.get(0), "chat/INFO", now++, second, known);
        for (StyledText row : block.subList(1, block.size())) {
            check("標頭之後的列先收著", others.offer(row, "chat/INFO", now++, second, known));
        }
        others.settle(now + DiscoveryRows.QUIET_MS + 1, second, known);
        check("★ 整句有譯文就不收", second.size() == 0);

        CaptureStore third = new CaptureStore(
                Files.createTempDirectory("wynnchayuan-discovery").resolve("captured.json"));
        DiscoveryRows late = new DiscoveryRows();
        late.offer(block.get(0), "chat/INFO", 1000, third, empty);
        check("安靜之後的列不算這一塊",
              !late.offer(prose("purpose."), "chat/INFO", 1000 + DiscoveryRows.QUIET_MS + 1, third, empty));
    }

    private static Path captureFile(CaptureStore store) throws Exception {
        Field file = CaptureStore.class.getDeclaredField("file");
        file.setAccessible(true);
        return (Path) file.get(store);
    }

    private static boolean colourIs(Component row, ChatFormatting colour) {
        boolean[] ok = {true};
        row.visit((style, text) -> {
            if (!text.isBlank() && !SpaceOffset.isSpaceFont(style)
                    && (style.getColor() == null || style.getColor().getValue() != colour.getColor())) {
                ok[0] = false;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return ok[0];
    }

    private static String filled(String text) {
        return text.replaceAll("\\{~\\d*}", "3").replaceAll("\\{p}", "Ragni");
    }

    private static void everyEntry() throws Exception {
        for (String language : LANGUAGES) {
            TranslationStore store = real(language);
            int single = 0;
            int multi = 0;
            List<String> missed = new ArrayList<>();
            List<String> crooked = new ArrayList<>();
            for (Map.Entry<String, JsonElement> e : corpus(language).entrySet()) {
                String key = e.getKey();
                if (key.startsWith("_") || key.contains("??") || e.getValue().getAsString().isBlank()) {
                    continue;
                }
                int newline = key.indexOf('\n');
                String sentence = filled(newline < 0 ? key : key.substring(newline + 1));
                String label = newline < 0 ? "Secret Discovery: Some Name (+100 XP)" : filled(key.substring(0, newline));
                int colon = label.indexOf(": ") + 2;
                List<StyledText> block = new ArrayList<>();
                block.add(head(label.substring(0, colon), label.substring(colon, label.lastIndexOf(" (")),
                               label.substring(label.lastIndexOf(" ("))));
                block.add(blank());
                List<String> wrapped = wrapped(sentence);
                for (String row : wrapped) {
                    block.add(prose(row));
                }
                Component out = held(block, store);
                boolean hit = out != null
                        && !out.getString().contains(sentence.substring(0, Math.min(24, sentence.length())));
                if (newline < 0) {
                    single += hit ? 1 : 0;
                } else {
                    multi += hit ? 1 : 0;
                }
                if (!hit) {
                    missed.add(key);
                    continue;
                }
                List<Component> rows = shown(out);
                for (Component row : rows.subList(2, rows.size())) {
                    if (!onCentreLine(row) || measure(row) - lead(row) > 300) {
                        crooked.add(key);
                        break;
                    }
                }
            }
            check("★ " + language + "：每一條都查得到（單行 " + single + "、含換行 " + multi
                    + "；沒中 " + missed.size() + "：" + (missed.isEmpty() ? "" : missed.get(0)) + "）",
                  missed.isEmpty());
            check("★ " + language + "：每一條都在置中線上而且塞得下（歪的 " + crooked.size() + "）",
                  crooked.isEmpty());
        }
    }
}
