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

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
