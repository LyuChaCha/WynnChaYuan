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

/**
 * 物品的名字只在<b>物品會出現的位置</b>才算數。
 *
 * <h2>實機回報（2026-10-10）</h2>
 * 傳送點上的漂浮字「Return to Detlas」畫成「回归之戒 (Return) 至 Detlas」。
 * {@code Return} 是一枚戒指的名字；「名字 + 數值尾巴」那條路把它當成屬性標籤查到了。
 * 拿手上五萬多條實機字串掃過一遍，同一類還有血條（{@code Frog - 1200❤}）、
 * 怪物名牌（{@code Fatal {#}{#}}、{@code Defective Bolt {#}{#}}）與「某人的東西」
 * （{@code Grook's Nest}）。
 *
 * <h2>怎麼測</h2>
 * 名字<b>從語料裡挑</b>，不寫死：之後把「Fatal」這隻怪補進 {@code npc.json}，
 * 寫死的測試就會無條件通過、再也守不住東西。挑的是「只有物品在用、而且套進這幾種
 * 形狀之後語料沒有整條收」的裝備名，每種語言各挑一批。
 */
public final class ItemNameContextTest {

    private static int failures = 0;

    private static final String ROOT = "src/main/resources/assets/wynnchayuan/translations";

    public static void main(String[] args) throws Exception {
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of(ROOT, lang));
            store.setNameMode(CollectorConfig.ItemNames.BOTH);
            List<String> names = pick(store, lang);
            check(lang + "：挑得到夠多只有物品在用的名字（" + names.size() + "）", names.size() >= 20);

            int plate = 0, bar = 0, range = 0, owner = 0, quality = 0, colon = 0;
            int list = 0, bullet = 0, count = 0, bare = 0;
            StringBuilder wrong = new StringBuilder();
            for (String name : names) {
                // ---- 不是物品的位置：一律不能換成那件物品
                plate += leak(store, name + " {#}{#}", name, wrong);
                bar += leak(store, name + " - {~}❤", name, wrong);
                range += leak(store, name + " to {p}", name, wrong);
                owner += leak(store, "Somebody's " + name, name, wrong);
                quality += leak(store, "Defective " + name + " {#}{#}", name, wrong);
                colon += leak(store, name + ":", name, wrong);
                // ---- 物品會出現的位置：照舊翻
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

    /** 拼出來的結果帶著「(名字)」——物品名稱才有的寫法——就算漏了一個。 */
    private static int leak(TranslationStore store, String template, String name,
                            StringBuilder wrong) {
        String hit = LineTranslator.lookup(template, store, false);
        if (hit == null || !hit.contains("(" + name + ")")) {
            return 0;
        }
        if (wrong.length() < 300) {
            wrong.append("  ").append(template).append(" -> ").append(hit);
        }
        return 1;
    }

    private static int shown(TranslationStore store, String template, String name) {
        String hit = LineTranslator.lookup(template, store, false);
        return hit != null && hit.contains("(" + name + ")") ? 1 : 0;
    }

    /**
     * 只有物品在用、譯文跟原文不一樣、而且這幾種形狀語料都沒有整條收的裝備名。
     * 單字的優先（一般的字最容易撞名），取前五十個。
     */
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

    /**
     * 別條執行緒也在查表時，這一條的物品名不能受影響。
     *
     * <h2>實機回報（2026-10-10，1.0.1）：「切換語言之後物品翻譯都會失效」</h2>
     * 「這個位置不是物品」原本是一個全域旗標。收集端每 30 秒在背景執行緒整理
     * {@code captured.json}，每一條都問 {@code hasTranslation}，那條路同樣會
     * 掛上再還原這個旗標；跟算繪那一條交錯時，旗標被「還原」成對方掛上的值，
     * 從此卡在開著——所有只有物品在用的名字都查不到，重開遊戲才會好。
     *
     * <p>第一段是必現的寫法：別條執行緒掛著旗標不放，這一條照樣要翻得到。
     * 第二段是實機那個情境：兩條一起跑，跑完旗標要是放下的。
     */
    private static void otherThreads(TranslationStore store, List<String> names, String lang)
            throws Exception {
        String name = names.get(0);
        java.util.concurrent.CountDownLatch held = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        Thread holder = new Thread(() -> {
            TranslationStore.barItemNames(true);
            held.countDown();
            try {
                release.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                TranslationStore.barItemNames(false);
            }
        }, "holder");
        holder.start();
        held.await();
        boolean mine = store.lookup(name) != null && !TranslationStore.itemNamesBarred();
        release.countDown();
        holder.join();
        check(lang + "：★ 別條執行緒掛著旗標，這一條的物品名照舊翻", mine);

        // 背景那一條：收集端整理時問的就是 hasTranslation（名牌、血條、某人的東西）
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
                LineTranslator.lookup(n + " {#}{#}", store, false);
                LineTranslator.lookup(n + " to {p}", store, false);
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

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
