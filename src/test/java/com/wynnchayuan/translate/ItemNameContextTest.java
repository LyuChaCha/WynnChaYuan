package com.wynnchayuan.translate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.CollectorConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ItemNameContextTest {

    private static final String ROOT = "src/main/resources/assets/wynnchayuan/translations";

    public static void main(String[] args) throws Exception {
        for (String lang : List.of("zh_tw", "ja_jp")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of(ROOT, lang));
            store.setNameMode(CollectorConfig.ItemNames.BOTH);
            List<String> names = pick(store, lang);
            check(lang + "：挑得到夠多只有物品在用的名字（" + names.size() + "）", names.size() >= 20);

            int plate = 0, bar = 0, range = 0, owner = 0, quality = 0, colon = 0;
            int list = 0, bullet = 0, count = 0, bare = 0;
            StringBuilder wrong = new StringBuilder();
            for (String name : names) {
                plate += leak(store, name + " {#}{#}", name, wrong);
                bar += leak(store, name + " - {~}❤", name, wrong);
                range += leak(store, name + " to {p}", name, wrong);
                owner += leak(store, "Somebody's " + name, name, wrong);
                quality += leak(store, "Defective " + name + " {#}{#}", name, wrong);
                colon += leak(store, name + ":", name, wrong);
                list += shown(store, "{#}{#}" + name, name);
                bullet += shown(store, "- " + name, name);
                count += shown(store, name + " [{~}/{~}]", name);
                bare += shown(store, name, name);
            }
            int n = names.size();
            check(lang + "：★ 名牌（名字 + 圖示）不會變成物品（" + plate + " 個漏掉）" + wrong, plate == 0);
            check(lang + "：★ 血條（名字 - 血量）不會變成物品（" + bar + "）", bar == 0);
            check(lang + "：★ 「名字 to 地名」不會變成物品（" + range + "）", range == 0);
            check(lang + "：★ 「某人的 名字」不會變成物品（" + owner + "）", owner == 0);
            check(lang + "：★ 「品質詞 名字 + 圖示」不會變成物品（" + quality + "）", quality == 0);
            check(lang + "：「名字:」不會變成物品（" + colon + "）", colon == 0);
            check(lang + "：圖示開頭的清單照舊翻（" + list + "/" + n + "）", list == n);
            check(lang + "：項目符號開頭的清單照舊翻（" + bullet + "/" + n + "）", bullet == n);
            check(lang + "：「名字 [數量]」照舊翻（" + count + "/" + n + "）", count == n);
            check(lang + "：單獨一個名字照舊翻（" + bare + "/" + n + "）", bare == n);
            check(lang + "：旗標用完有放下", !TranslationStore.itemNamesBarred());
            otherThreads(store, names, lang);
        }

        System.out.println(failures == 0
                ? "ItemNameContext: 全部通過" : "ItemNameContext: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static int leak(TranslationStore store, String template, String name,
                            StringBuilder wrong) {
        String hit = TemplateLookup.lookup(template, store, false);
        if (hit == null || !hit.contains("(" + name + ")")) {
            return 0;
        }
        if (wrong.length() < 300) {
            wrong.append("  ").append(template).append(" -> ").append(hit);
        }
        return 1;
    }

    private static int shown(TranslationStore store, String template, String name) {
        String hit = TemplateLookup.lookup(template, store, false);
        return hit != null && hit.contains("(" + name + ")") ? 1 : 0;
    }

    private static List<String> pick(TranslationStore store, String lang) throws Exception {
        List<String> out = new ArrayList<>();
        for (String file : List.of("gear-accessory.json", "gear-weapon.json", "gear-armour.json")) {
            Path path = Path.of(ROOT, lang, file);
            JsonObject root = JsonParser.parseString(
                    Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("entries").entrySet()) {
                JsonObject entry = e.getValue().getAsJsonObject();
                String src = entry.get("src").getAsString();
                String dst = entry.has("dst") ? entry.get("dst").getAsString() : "";
                if (dst.isBlank() || dst.equals(src) || !src.matches("[A-Z][a-z]{3,}")) {
                    continue;
                }
                if (!store.itemNameOnly(src)) {
                    continue;
                }
                boolean keyed = false;
                for (String shape : List.of(src + " {#}{#}", src + " - {~}❤", src + " to {p}",
                        src + ":", "Somebody's " + src, "Defective " + src + " {#}{#}")) {
                    keyed |= store.lookupExact(shape) != null;
                }
                if (!keyed) {
                    out.add(src);
                }
                if (out.size() >= 50) {
                    return out;
                }
            }
        }
        return out;
    }

    private static void otherThreads(TranslationStore store, List<String> names, String lang)
            throws Exception {
        String name = names.get(0);
        java.util.concurrent.CountDownLatch held = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        Thread holder = new Thread(() -> {
            try (LookupScope scope = LookupScope.barItemNames(true)) {
                held.countDown();
                release.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "holder");
        holder.start();
        held.await();
        boolean mine = store.lookup(name) != null && !TranslationStore.itemNamesBarred();
        release.countDown();
        holder.join();
        check(lang + "：★ 別條執行緒掛著旗標，這一條的物品名照舊翻", mine);

        java.util.concurrent.atomic.AtomicBoolean stop =
                new java.util.concurrent.atomic.AtomicBoolean();
        Thread pruner = new Thread(() -> {
            while (!stop.get()) {
                for (String n : names) {
                    store.hasTranslation(n + " {#}{#}");
                    store.hasTranslation(n + " - {~}❤");
                    store.hasTranslation("Somebody's " + n);
                }
            }
        }, "pruner");
        pruner.start();
        int lost = 0;
        long until = System.nanoTime() + 1_500_000_000L;
        while (System.nanoTime() < until) {
            for (String n : names) {
                TemplateLookup.lookup(n + " {#}{#}", store, false);
                TemplateLookup.lookup(n + " to {p}", store, false);
                if (store.lookup(n) == null) {
                    lost++;
                }
            }
        }
        stop.set(true);
        pruner.join();
        check(lang + "：★ 背景整理與算繪一起查，物品名一次都沒有掉（" + lost + " 次）", lost == 0);
        check(lang + "：★ 兩條一起跑完，旗標是放下的", !TranslationStore.itemNamesBarred());
        check(lang + "：★ 跑完之後物品名照舊翻", store.lookup(name) != null);
    }

}
