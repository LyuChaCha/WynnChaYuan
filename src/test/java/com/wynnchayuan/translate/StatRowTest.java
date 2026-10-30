package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StatRowTest {

    private static int failures = 0;

    private static final java.util.regex.Pattern STAT_LABEL =
            java.util.regex.Pattern.compile("[A-Z][A-Za-z]*(?: [A-Za-z]+)*%?");

    private static List<String> labels() throws IOException {
        Path path = Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT, "ui-labels.json");
        JsonObject root = JsonParser.parseString(
                Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> out = new ArrayList<>();
        for (String key : root.keySet()) {
            if (key.startsWith("_") || !STAT_LABEL.matcher(key).matches()
                    || root.get(key).getAsString().isBlank()) {
                continue;
            }
            out.add(key);
        }
        return out;
    }

    public static void main(String[] args) throws IOException {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        starts(store, "Fire Spell Damage {#}+{~} [{~}]", "火屬性法術傷害");
        starts(store, "Water Spell Damage{#}-{~} to -{~}", "水屬性法術傷害");
        starts(store, "Water Spell Damage {#}-{~} [{~}]", "水屬性法術傷害");
        starts(store, "Air Spell Damage {#}+{~} [{~}]", "風屬性法術傷害");
        starts(store, "Elemental Spell Damage {#}+{~} [{~}, {~}]", "元素法術傷害");
        starts(store, "Damage Scale{#} [{~}]", "傷害適性");
        starts(store, "Main Scale{#} [{~}]", "主要適性");
        starts(store, true, "{#} Main Scale{#}[{~}]", "{#} 主要適性");
        starts(store, true, "{#} Damage Scale{#}[{~}]", "{#} 傷害適性");
        starts(store, "Walk Speed {#}+{~} [{~}]", "移動速度");
        starts(store, "Health Regen {#}-{~} [{~}]", "生命回復");
        starts(store, "Teleport Cost {#}-{~} [{~}]", "傳送消耗");

        starts(store, true, "Spell Damage {#}+{~} [{~}]", "法術傷害百分比");
        starts(store, false, "Spell Damage {#}+{~} [{~}]", "法術傷害值");

        truncated(store);
        everyLabel(store);
        ownerLabels(store);
        notAGap(store);
        numberFirst(store);

        keeps(store, "Fire Spell Damage {#}+{~} [{~}]", "{#}+{~} [{~}]");
        keeps(store, "Elemental Spell Damage {#}+{~} ★{~} ⇧{~} ⇩{~}", "★{~} ⇧{~} ⇩{~}");

        TranslationStore zh = new TranslationStore();
        zh.loadAll(List.of(Path.of("src/main/resources/assets/wynnchayuan/translations",
                                   Languages.DEFAULT),
                           corpus("wynnchayuan-connectors", "{~} to {~}", "{~} 到 {~}",
                                  "{~} tier", "{~} 階")));
        contains(zh, "Water Spell Damage{#}-{~} to -{~}", "到");
        contains(zh, "Attack Speed {#}+{~} tier", "階");

        TranslationStore ru = new TranslationStore();
        ru.loadAll(corpus("wynnchayuan-connectors-ru",
                "Water Spell Damage", "Урон заклинаний Воды",
                "Attack Speed", "Скорость атаки",
                "{~} to {~}", "{~} до {~}"));
        equals(ru, "Water Spell Damage{#}-{~} to -{~}", "Урон заклинаний Воды{#}-{~} до -{~}");
        equals(ru, "Attack Speed {#}+{~} tier", "Скорость атаки {#}+{~} tier");

        TranslationStore bare = new TranslationStore();
        bare.loadAll(corpus("wynnchayuan-connectors-none",
                "Water Spell Damage", "水屬性法術傷害"));
        equals(bare, "Water Spell Damage{#}-{~} to -{~}", "水屬性法術傷害{#}-{~} to -{~}");

        nothing(store, "Grants extra courage to everyone nearby by +{~}.");
        nothing(store, "Deal Fire Spell Damage of a very strange kind {~}");
        nothing(store, "Allies within {~} blocks gain {~} of the health you gain");

        System.out.println(failures == 0
                ? "StatRow: 全部通過" : "StatRow: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void ownerLabels(TranslationStore store) {
        owner(store, "PoorChaCha's Mob Totem", "PoorChaCha 的怪物石碑");
        owner(store, "PoorChaCha's Gathering Totem", "PoorChaCha 的採集石碑");
        owner(store, "PoorChaCha’s Mob Totem", "PoorChaCha 的怪物石碑");

        report("後半不認得就不翻（實際："
                        + LineTranslator.lookup("Someone's Unknown Thingy", store, false) + "）",
                LineTranslator.lookup("Someone's Unknown Thingy", store, false) == null);
    }

    private static void owner(TranslationStore store, String row, String want) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」-> 「" + want + "」（實際：" + hit + "）",
                want.equals(hit));
    }

    private static void everyLabel(TranslationStore store) throws IOException {
        java.util.List<String> missed = new java.util.ArrayList<>();
        int checked = 0;
        for (String label : labels()) {
            for (String shape : new String[] {" {#}+{~} [{~}]", "{#}-{~} to -{~}"}) {
                checked++;
                if (LineTranslator.lookup(label + shape, store, false) == null) {
                    missed.add(label + shape);
                }
            }
        }
        report("每一條標籤配上數值都接得回來（掃了 " + checked + " 種，漏掉 "
                + (missed.size() > 6 ? missed.subList(0, 6) + "…" : missed) + "）",
                missed.isEmpty());
    }

    private static void notAGap(TranslationStore store) {
        for (String row : new String[] {
                "Walk Speed {#}+{~} [{~}]",
                "Spell Damage {#}+{~} [{~}]",
                "Health Regen {#}-{~} [{~}]",
                "Water Spell Damage{#}-{~} to -{~}",
                "Attack Speed {#}+{~} tier"}) {
            report("「" + row + "」不會被記成缺口", store.hasTranslation(row));
        }
        report("真的沒翻的還是缺口",
                !store.hasTranslation("Grants extra courage to everyone nearby by +{~}."));
    }

    private static void numberFirst(TranslationStore store) {
        String[][] rows = {
            {"{~} Main Attack Damage {#} [{~}]", "普攻傷害"},
            {"{~} Thunder Damage {#} [{~}]", "雷屬性傷害"},
            {"{~} Main Attack Damage Raw {#} [{~}]", "普攻傷害值"},
            {"{~} Walk Speed {#} [{~}]", "移動速度"},
            {"{~} Exploding {#} [{~}]", "爆炸"},
            {"{~} Earth Damage {#} [{~}]", "地屬性傷害"},
        };
        for (String[] r : rows) {
            String hit = LineTranslator.lookup(r[0], store, false);
            report("數值在前：「" + r[0] + "」-> 含「" + r[1] + "」（實際：" + hit + "）",
                    hit != null && hit.contains(r[1]));
            report("數值在前：「" + r[0] + "」數值原樣在前、括號原樣在後（實際：" + hit + "）",
                    hit != null && hit.startsWith("{~} ") && hit.endsWith("{#} [{~}]"));
            report("數值在前：「" + r[0] + "」不會被記成缺口", store.hasTranslation(r[0]));
        }
        starts(store, true, "{~} Main Attack Damage {#} [{~}]", "{~} 普攻傷害百分比");
        starts(store, false, "{~} Main Attack Damage {#} [{~}]", "{~} 普攻傷害值");

        String seg = LineTranslator.lookup("{~} Walk Speed", store, false);
        report("只有「數值 + 標籤」的片段也翻得出來（實際：" + seg + "）",
                seg != null && seg.contains("移動速度"));

        String gear = LineTranslator.lookup("{~} Guardian", store, false);
        report("數字 + 不是介面標籤的詞不拼（實際：" + gear + "）", gear == null);
        nothing(store, "{~} blocks away from the nearest city {#} [{~}]");

        net.minecraft.network.chat.MutableComponent line =
                net.minecraft.network.chat.Component.empty();
        Object[] parts = {"+22 Main Attack Damage", 0x55FF55, " ", 0xAAAAAA,
                          "", 0xFFFFFF, " [45%]", 0x555555};
        for (int i = 0; i < parts.length; i += 2) {
            line.append(net.minecraft.network.chat.Component.literal((String) parts[i])
                    .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(
                            net.minecraft.network.chat.TextColor.fromRgb((Integer) parts[i + 1]))));
        }
        com.wynntils.core.text.StyledText styled =
                com.wynntils.core.text.StyledText.fromComponent(line);
        net.minecraft.network.chat.Component built = LineTranslator.translate(styled, store);
        String all = built == null ? "" : built.getString();
        report("畫出來標籤是中文（模板 " + com.wynnchayuan.capture.LineParts.of(styled).template()
                        + "，實際：" + all + "）",
                all.contains("普攻傷害") && !all.contains("Main Attack"));
        report("畫出來數值與括號原樣（實際：" + all + "）",
                all.startsWith("+22 ") && all.endsWith("[45%]"));
    }

    private static void truncated(TranslationStore store) {
        String raw = LineTranslator.lookup("Elemental Spell Damage {#}+{~} [{~}]", store, false);
        String pct = LineTranslator.lookup("Elemental Spell Damage {#}+{~} [{~}]", store, true);
        report("前提：完整的標籤查得到，實數與百分比是兩種譯法（實際：" + raw + "／" + pct + "）",
                raw != null && pct != null && !raw.equals(pct));
        String rawLabel = raw == null ? "" : raw.substring(0, raw.indexOf(" {#}"));
        String pctLabel = pct == null ? "" : pct.substring(0, pct.indexOf(" {#}"));

        equals(store, false, "Elemental Spell Da..{#}*+{~} [{~}]", rawLabel + "{#}*+{~} [{~}]");
        equals(store, true, "Elemental Spell Da..{#}*+{~} [{~}]", pctLabel + "{#}*+{~} [{~}]");
        equals(store, true, "Elemental Sp..{#}+{~} [{~}]", pctLabel + "{#}+{~} [{~}]");
        equals(store, true, "Elemental Spell ..{#}+{~} [{~}]", pctLabel + "{#}+{~} [{~}]");
        equals(store, true, "{#} Elemental Spell Da..{#}+{~} [{~}]",
                "{#} " + pctLabel + "{#}+{~} [{~}]");

        java.util.Set<String> bases = new java.util.TreeSet<>();
        try {
            for (String key : labels()) {
                if (key.startsWith("Elemental ")) {
                    bases.add(key.replaceAll("(%| Raw)$", ""));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        report("前提：Elemental 開頭的標籤不只一個（實際：" + bases + "）", bases.size() > 1);
        nothing(store, "Elemental ..{#}+{~} [{~}]");
        nothing(store, "Elem..{#}+{~} [{~}]");
        nothing(store, "Elemental Spell Da..{#}");
        nothing(store, "Elemental Spell Da...");
        nothing(store, "Elemental..");

        String plainRaw = LineTranslator.lookup("Elemental Spell Damage", store, false);
        String plainPct = LineTranslator.lookup("Elemental Spell Damage", store, true);
        report("前提：單獨一段的完整標籤查得到（實際：" + plainRaw + "／" + plainPct + "）",
                plainRaw != null && plainPct != null);
        equals(store, false, "Elemental Spell Da..", plainRaw);
        equals(store, true, "Elemental Spell Da..", plainPct);
        nothing(store, "Completely Made Up La..{#}+{~} [{~}]");
    }

    private static void equals(TranslationStore store, boolean percent, String row, String want) {
        String hit = LineTranslator.lookup(row, store, percent);
        report("「" + row + "」" + (percent ? "（百分比）" : "（實數）")
                + "-> 「" + want + "」（實際：" + hit + "）", want.equals(hit));
    }

    private static void starts(TranslationStore store, boolean percent,
                               String row, String head) {
        String hit = LineTranslator.lookup(row, store, percent);
        report("「" + row + "」" + (percent ? "（百分比）" : "（實數）")
                + "換成「" + head + "」（實際：" + hit + "）",
                hit != null && hit.startsWith(head));
    }

    private static void starts(TranslationStore store, String row, String head) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」的標籤換成「" + head + "」（實際：" + hit + "）",
                hit != null && hit.startsWith(head));
    }

    private static void keeps(TranslationStore store, String row, String tail) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」的數值原樣留著（實際：" + hit + "）",
                hit != null && hit.endsWith(tail));
    }

    private static void contains(TranslationStore store, String row, String want) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」裡有「" + want + "」（實際：" + hit + "）",
                hit != null && hit.contains(want));
    }

    private static void equals(TranslationStore store, String row, String want) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」-> 「" + want + "」（實際：" + hit + "）", want.equals(hit));
    }

    static Path corpus(String name, String... pairs) throws IOException {
        Path dir = Files.createTempDirectory(name);
        JsonObject root = new JsonObject();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            root.addProperty(pairs[i], pairs[i + 1]);
        }
        Files.writeString(dir.resolve("ui-labels.json"), root.toString(), StandardCharsets.UTF_8);
        return dir;
    }

    private static void nothing(TranslationStore store, String sentence) {
        String hit = LineTranslator.lookup(sentence, store, false);
        report("句子不會被當成屬性列：" + sentence + "（實際：" + hit + "）", hit == null);
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
