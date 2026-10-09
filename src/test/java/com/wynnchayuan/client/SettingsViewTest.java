package com.wynnchayuan.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Preview;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.SettingsView;
import com.wynnchayuan.client.ui.Ui;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把設定畫面<b>不開遊戲</b>算出來，量一遍有沒有東西凸出去或疊在一起。
 *
 * <h2>為什麼要有</h2>
 * 舊畫面的版面漂過好幾次，每次都是使用者截圖回報才知道；而這一版多了六種語言，
 * 俄文與西班牙文的選項比中文長一倍以上，靠人一頁一頁看看不完。
 * {@link SettingsView} 不認得遊戲，接一張記憶體裡的畫布就能整個跑——所以這裡把
 * 六種語言 × 五個分類 × 幾種畫面大小全部算一次，每一列都量：
 * <ul>
 *   <li>整列在視窗裡面；</li>
 *   <li>名稱的右緣沒有壓到控制項的左緣；</li>
 *   <li>同一頁的分段、下拉、滑桿要嘛全部在名稱右邊，要嘛全部換到下一列——
 *       不能有的擠有的鬆（使用者 2026-10-08 回報俄文那樣很難看）。</li>
 * </ul>
 *
 * <h2>順便存圖</h2>
 * 算出來的畫面存在 {@code build/ui-preview/}。字的寬度是照原版字型估的
 * （見 {@link AwtCanvas}），所以「放不放得下」可信，字的長相不可信。
 *
 * <h2>這裡的設定表是抄的</h2>
 * 真正的表在 {@code SettingsScreen#buildTabs}，但那個類別要遊戲才載得起來。
 * 這裡照同樣的鍵與同樣的選項再列一次；兩邊對不上的話量出來的就不是實際的畫面，
 * 所以改了那邊的列要回來改這裡。
 */
public final class SettingsViewTest {

    private static int failures = 0;

    private static void must(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("  [FAIL] " + what);
        }
    }

    private static final String[] LANGS = {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "en_us"};
    private static final String[] TABS = {"items", "panel", "dialogue", "world", "data"};

    public static void main(String[] args) throws Exception {
        File out = new File("build/ui-preview");
        out.mkdirs();
        int frames = 0;

        for (String lang : LANGS) {
            LANG = lang;
            AwtCanvas.missing.clear();
            Fake fake = new Fake(lang);
            fontCovers(lang, fake);
            for (int[] size : new int[][] {{640, 360}, {960, 540}, {480, 270}, {427, 240}}) {
                SettingsView view = new SettingsView(fake.tabs(), fake, 0);
                for (int tab = 0; tab < TABS.length; tab++) {
                    view.showTab(tab);
                    AwtCanvas c = render(view, size[0], size[1], 2);
                    String at = lang + " " + TABS[tab] + " " + size[0] + "x" + size[1];
                    check(at, view);
                    frames++;
                    if (size[0] == 640) {
                        ImageIO.write(c.image, "png", new File(out, lang + "-" + TABS[tab] + ".png"));
                    }
                }
            }
            // 矮視窗：示意圖裡的記分板放不進大字底下，會退到 NPC 旁邊。那個排法另外留一張
            SettingsView low = new SettingsView(fake.tabs(), fake, 0);
            low.showTab(3);
            ImageIO.write(render(low, 720, 300, 2).image, "png", new File(out, lang + "-world-short.png"));
        }
        LANG = "zh_tw";
        System.out.println("  [PASS] 算了 " + frames + " 張畫面（" + LANGS.length + " 種語言 × "
                + TABS.length + " 個分類 × 4 種大小）");

        // 繁中在標準大小下每一頁都排得成一列：這是設計的基準，變成兩列代表哪裡的寬度算錯了
        Fake zh = new Fake("zh_tw");
        SettingsView view = new SettingsView(zh.tabs(), zh, 0);
        for (int tab = 0; tab < TABS.length; tab++) {
            view.showTab(tab);
            render(view, 640, 360, 1);
            must("zh_tw " + TABS[tab] + "：標準大小下不必換成兩列", !view.pageWide());
        }
        // 俄文的「世界與聊天」放不下，要整頁換成兩列
        LANG = "ru_ru";
        Fake ru = new Fake("ru_ru");
        view = new SettingsView(ru.tabs(), ru, 3);
        render(view, 640, 360, 1);
        must("ru_ru world：選項太長，整頁換成兩列", view.pageWide());
        LANG = "zh_tw";

        interactions(out);

        // 預覽的物品名：語料在「譯名加原文」時回來的已經帶著原文，預覽自己還會再接一次
        // （使用者 2026-10-09 回報畫成「骨弓 (Bony Bow) (Bony Bow)」）
        must("語料附的原文要先拿掉（認得出來的那種）",
                "骨弓".equals(Preview.bareName("骨弓 (Bony Bow)", "Bony Bow", 2)));
        must("語料認不出來、但結尾剛好是「 (原文)」的也拿掉",
                "骨弓".equals(Preview.bareName("骨弓 (Bony Bow)", "Bony Bow", -1)));
        must("沒附原文的不動", "骨弓".equals(Preview.bareName("骨弓", "Bony Bow", -1)));
        must("譯文本來就帶別的括號的不動",
                "緩慢 (每秒 1.5 次)".equals(Preview.bareName("緩慢 (每秒 1.5 次)", "Slow", -1)));

        // 預覽的記分板：語料的鍵帶佔位符，數字要填對位置（使用者 2026-10-09：記分板的預覽也要做）
        must("沒編號的佔位符照順序填",
                "- Mobs slain: 42/100".equals(Preview.fill("- Mobs slain: {~}/{~}", "42", "100")));
        must("有編號的照編號填（譯文換過語序的那種）",
                "100 之中的 42".equals(Preview.fill("{~2} 之中的 {~1}", "42", "100")));
        must("數字不夠也不會炸、不留佔位符",
                "T2+: 0/0".equals(Preview.fill("T{~1}+: {~2}/{~3}", "2")));
        must("別種大括號不動", "{#}甲 3".equals(Preview.fill("{#}甲 {~}", "3")));
        // 範例句在六個語言的語料裡都要查得到——查不到的語言開了開關預覽也不會變
        for (String lang : LANGS) {
            if ("en_us".equals(lang)) {
                continue;                       // 介面語言，不是譯文語言，沒有語料
            }
            Map<String, String> corpus = corpus(lang);
            for (String key : new String[] {"Daily Objective:", "- Loot Chests T{~}+: {~}/{~}",
                                            "- Mobs slain: {~}/{~}"}) {
                String hit = corpus.get(key);
                must(lang + "：記分板範例「" + key + "」有譯文", hit != null && !hit.isBlank());
                must(lang + "：填完數字沒有殘留佔位符（" + hit + "）",
                        hit == null || !Preview.fill(hit, "2", "3", "5").contains("{~"));
            }
        }

        if (failures > 0) {
            System.out.println("SettingsView: " + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("SettingsView: 全部通過，圖在 " + out.getPath());
    }

    /** 現在用哪個語言的字型畫。 */
    private static String LANG = "zh_tw";

    /** 一個語言出貨的扁平語料（兩種檔案形狀都讀），只給示意圖查範例句用。 */
    private static Map<String, String> corpus(String lang) throws Exception {
        Map<String, String> out = new HashMap<>();
        File dir = new File("src/main/resources/assets/wynnchayuan/translations", lang);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json") && !name.startsWith("_")
                && !name.endsWith("-dialogue.json"));
        if (files == null) {
            return out;
        }
        for (File f : files) {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(
                    java.nio.file.Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("entries") && root.get("entries").isJsonObject()) {
                for (var e : root.getAsJsonObject("entries").entrySet()) {
                    if (!e.getValue().isJsonObject()) {
                        continue;
                    }
                    var o = e.getValue().getAsJsonObject();
                    if (o.has("src") && o.has("dst") && !o.get("dst").getAsString().isBlank()) {
                        out.putIfAbsent(o.get("src").getAsString(), o.get("dst").getAsString());
                    }
                }
            } else {
                for (var e : root.entrySet()) {
                    if (!e.getKey().startsWith("_") && e.getValue().isJsonPrimitive()
                            && !e.getValue().getAsString().isBlank()) {
                        out.putIfAbsent(e.getKey(), e.getValue().getAsString());
                    }
                }
            }
        }
        return out;
    }

    /** 假的時鐘：每畫一幀往前走一點，動畫才會走完。 */
    private static long CLOCK = 1000;

    /** 連畫幾幀：第一幀量出捲動範圍與動畫的起點，後面幾幀讓彈出層淡入、滑塊滑到定位。 */
    private static AwtCanvas render(SettingsView view, int w, int h, int scale) {
        AwtCanvas c = null;
        for (int i = 0; i < 8; i++) {
            c = new AwtCanvas(w, h, scale, LANG);
            c.backdrop();
            view.render(c, w, h, -100, -100, CLOCK += 90);
            c.done();
        }
        return c;
    }

    /**
     * 這個語言的介面字串，隨模組附的字型都畫得出來。
     *
     * <p>字型是裁過的（見 {@code tools/build-ui-fonts.py}），只留用得到的字。
     * 加了介面字串卻沒重裁，新的字會退回原版的點陣字——不會壞，但一行裡混著兩種字。
     * 這裡把整份語言檔掃一遍，缺的字直接列出來。
     */
    private static void fontCovers(String lang, Fake fake) {
        StringBuilder gone = new StringBuilder();
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (String value : fake.strings.values()) {
            value.codePoints().forEach(cp -> {
                if (cp > ' ' && seen.add(cp) && !AwtCanvas.covers(lang, cp)) {
                    gone.appendCodePoint(cp);
                }
            });
        }
        must(lang + "：介面字串的字，字型都有（缺：" + gone + "）——加了字串要重跑 tools/build-ui-fonts.py",
                gone.length() == 0);
    }

    private static void check(String at, SettingsView view) {
        float[] win = view.window();
        List<SettingsView.Placed> rows = view.placed();
        must(at + "：這一頁有畫出列", !rows.isEmpty());
        Boolean wide = null;
        for (SettingsView.Placed p : rows) {
            String row = at + " " + p.id();
            must(row + "：左右在視窗裡", p.x() >= win[0] && p.x() + p.w() <= win[0] + win[2]);
            must(row + "：控制項沒有超出這一列", p.ctrlLeft() >= p.x() && p.ctrlRight() <= p.x() + p.w());
            if (p.wide()) {
                must(row + "：兩列的那種，名稱在自己那一列裡",
                        p.labelRight() <= p.x() + p.w() - 6);
            } else {
                must(row + "：名稱（到 " + p.labelRight() + "）沒有壓到控制項（從 "
                        + p.ctrlLeft() + "）", p.labelRight() <= p.ctrlLeft() - 3);
            }
        }
        for (SettingsView.Placed p : rows) {
            if (STRETCHY.contains(p.id())) {
                if (wide == null) {
                    wide = p.wide();
                }
                must(at + " " + p.id() + "：同一頁的分段、下拉、滑桿要一起換列", wide == p.wide());
            }
        }
    }

    /** 整頁換成兩列時會跟著換的那些列（分段、下拉、滑桿）。 */
    private static final java.util.Set<String> STRETCHY = java.util.Set.of(
            "items.tooltip", "items.names", "items.shot", "panel.anchor", "panel.side", "panel.gap",
            "dialogue.mode", "dialogue.choices", "dialogue.hold", "world.nametag", "world.chat",
            "world.tracker", "data.language", "data.fallback", "data.ui", "data.source");

    /** 點得到、拖得動、彈得出來。 */
    private static void interactions(File out) throws Exception {
        Fake zh = new Fake("zh_tw");
        SettingsView view = new SettingsView(zh.tabs(), zh, 0);
        render(view, 640, 360, 1);

        // 開關：點那一列控制項的右端
        SettingsView.Placed market = find(view, "items.market");
        boolean before = zh.flag("items.market");
        view.mouseDown(market.ctrlRight() - 8, market.y() + market.h() / 2, 0);
        must("點開關會翻過去", zh.flag("items.market") != before);

        // 分段：點最左邊那一格
        SettingsView.Placed names = find(view, "items.names");
        view.mouseDown(names.ctrlLeft() + 6, names.y() + names.h() / 2, 0);
        must("點分段的第一格會選到它", zh.index("items.names") == 0);
        render(view, 640, 360, 1);
        must("改過之後這一頁可以重置（有東西不是預設值）", zh.dirty("items.names"));

        // 滑桿：按住拖到最右邊，放手才存
        view.showTab(1);
        render(view, 640, 360, 1);
        SettingsView.Placed gap = find(view, "panel.gap");
        view.mouseDown(gap.ctrlLeft() + 12, gap.y() + gap.h() / 2, 0);
        view.mouseDrag(gap.ctrlLeft() + 60, gap.y() + gap.h() / 2);
        must("拖滑桿的時候值跟著變", zh.number("panel.gap") != 12);
        must("還沒放手不存檔", zh.commits == 0);
        view.mouseUp();
        must("放手才存檔", zh.commits == 1);

        // 色盤
        SettingsView.Placed theme = find(view, "panel.theme");
        view.mouseDown(theme.ctrlRight() - 40, theme.y() + theme.h() / 2, 0);
        must("點色塊會打開色盤", view.popoverOpen());
        AwtCanvas c = render(view, 640, 360, 3);
        ImageIO.write(c.image, "png", new File(out, "zh_tw-picker.png"));
        view.key(SettingsView.KEY_ESC, false);
        must("Esc 先收色盤，不是關掉整個畫面", !view.popoverOpen());

        // 下拉
        view.showTab(4);
        render(view, 640, 360, 1);
        SettingsView.Placed language = find(view, "data.language");
        view.mouseDown(language.ctrlLeft() + 20, language.y() + language.h() / 2, 0);
        must("點下拉會打開選單", view.popoverOpen());
        c = render(view, 640, 360, 3);
        ImageIO.write(c.image, "png", new File(out, "zh_tw-dropdown.png"));
        view.key(SettingsView.KEY_ESC, false);

        // 搜尋：跨分類找，找不到也不能壞
        view.searchFor("翻譯");
        c = render(view, 640, 360, 3);
        ImageIO.write(c.image, "png", new File(out, "zh_tw-search.png"));
        must("搜尋「翻譯」找得到東西", !view.placed().isEmpty());
        view.searchFor("zzzz");
        render(view, 640, 360, 1);
        must("搜不到的時候沒有列，也不丟例外", view.placed().isEmpty());
        view.searchFor("");

        // 大圖：給人看的
        for (String lang : new String[] {"zh_tw", "ru_ru", "es_es", "ja_jp", "ko_kr", "zh_cn"}) {
            LANG = lang;
            Fake fake = new Fake(lang);
            SettingsView big = new SettingsView(fake.tabs(), fake, 0);
            for (int tab : new int[] {0, 3}) {
                big.showTab(tab);
                c = render(big, 640, 360, 3);
                ImageIO.write(c.image, "png", new File(out, "big-" + lang + "-" + TABS[tab] + ".png"));
            }
        }
        LANG = "zh_tw";
        // 小畫面：介面縮放開到最大、或視窗很小的時候
        for (int[] size : new int[][] {{480, 270}, {427, 240}, {320, 240}}) {
            Fake fake = new Fake("zh_tw");
            SettingsView small = new SettingsView(fake.tabs(), fake, 3);
            c = render(small, size[0], size[1], 3);
            ImageIO.write(c.image, "png", new File(out, "small-" + size[0] + "x" + size[1] + ".png"));
            check("zh_tw world " + size[0] + "x" + size[1], small);
        }
        System.out.println("  [PASS] 點、拖、彈出層、搜尋都走過一遍");
    }

    private static SettingsView.Placed find(SettingsView view, String id) {
        for (SettingsView.Placed p : view.placed()) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        throw new IllegalStateException("畫面上沒有 " + id);
    }

    // ------------------------------------------------------------ 假的設定

    /** 一份假的設定：值存在表裡，字從語言檔讀。 */
    static final class Fake implements SettingsView.Host {
        private final Map<String, String> strings;
        private final Map<String, String> english;
        private final Map<String, Object> values = new HashMap<>();
        private final Map<String, Object> defaults = new HashMap<>();
        int commits;

        private final Map<String, String> corpus;

        Fake(String lang) throws Exception {
            strings = load(lang);
            english = load("en_us");
            corpus = corpus(lang);
        }

        private static Map<String, String> load(String lang) throws Exception {
            try (InputStream in = SettingsViewTest.class.getResourceAsStream(
                    "/assets/wynnchayuan/lang/" + lang + ".json")) {
                return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, String>>() { }.getType());
            }
        }

        boolean flag(String id) {
            return (Boolean) values.get(id);
        }

        int index(String id) {
            return (Integer) values.get(id);
        }

        int number(String id) {
            return (Integer) values.get(id);
        }

        boolean dirty(String id) {
            return !values.get(id).equals(defaults.get(id));
        }

        private void init(String id, Object value) {
            values.putIfAbsent(id, value);
            defaults.putIfAbsent(id, value);
        }

        private Row toggle(String id, boolean on, String onKey) {
            init(id, on);
            Row r = Row.toggle(id, () -> tr(id), () -> tr(id + ".hint"))
                    .on(() -> flag(id)).flip(() -> values.put(id, !flag(id)))
                    .reset(() -> !dirty(id), () -> values.put(id, defaults.get(id)));
            return onKey == null ? r : r.onText(() -> tr(onKey));
        }

        private Row segment(String id, int selected, Row.Tone[] tones, String... keys) {
            init(id, selected);
            // 截圖那一列的說明看有沒有綁鍵，沒有固定的 .hint
            String hintKey = english.containsKey("wynnchayuan." + id + ".hint")
                    ? id + ".hint" : id + ".unbound";
            Row r = Row.segment(id, () -> tr(id), () -> tr(hintKey));
            for (int i = 0; i < keys.length; i++) {
                r.option(keys[i].startsWith("=") ? keys[i].substring(1) : tr(keys[i]), tones[i]);
            }
            return r.selected(() -> index(id)).pick(i -> values.put(id, i))
                    .reset(() -> !dirty(id), () -> values.put(id, defaults.get(id)));
        }

        private Row slider(String id, int lo, int hi, int value, boolean seconds) {
            init(id, value);
            return Row.slider(id, () -> tr(id), () -> tr(id + ".hint")).range(lo, hi)
                    .value(() -> number(id)).slide(v -> values.put(id, v))
                    .format(v -> seconds ? (v == 0 ? tr("unit.forever") : tr("unit.seconds", v))
                                         : v + " px")
                    .commit(() -> commits++)
                    .reset(() -> !dirty(id), () -> values.put(id, defaults.get(id)));
        }

        private Row colour(String id) {
            init(id, "#6FA8D8");
            return Row.colour(id, () -> tr(id), () -> tr(id + ".hint"))
                    .hex(() -> (String) values.get(id))
                    .setHex(hex -> {
                        values.put(id, hex.toUpperCase());
                        return true;
                    })
                    .commit(() -> commits++)
                    .reset(() -> !dirty(id), () -> values.put(id, defaults.get(id)));
        }

        private Row select(String id, boolean apply, String autoKey) {
            init(id, "");
            Row r = Row.select(id, () -> tr(id), () -> tr(id + ".hint"))
                    .choices(() -> {
                        List<Row.Choice> list = new ArrayList<>();
                        list.add(new Row.Choice("", tr(autoKey, "繁體中文"), "A", true));
                        list.add(new Row.Choice("zh_tw", "繁體中文", "繁", false));
                        list.add(new Row.Choice("zh_cn", "简体中文", "简", false));
                        list.add(new Row.Choice("ja_jp", "日本語", "日", false));
                        list.add(new Row.Choice("ko_kr", "한국어", "한", false));
                        list.add(new Row.Choice("ru_ru", "Русский", "RU", false));
                        list.add(new Row.Choice("es_es", "Español", "ES", false));
                        return list;
                    })
                    .current(() -> (String) values.get(id))
                    .choose(v -> values.put(id, v));
            return apply ? r.apply(() -> false, () -> { }) : r;
        }

        private Row action(String id, String labelKey) {
            return Row.action(id, () -> tr(id), () -> tr(id + ".hint")).label(() -> tr(labelKey));
        }

        List<Row.Tab> tabs() {
            Row.Tone a = Row.Tone.ACCENT;
            Row.Tone rp = Row.Tone.REPLACE;
            Row.Tone bo = Row.Tone.BOTH;
            Row.Tone off = Row.Tone.OFF;
            Row.Tone pl = Row.Tone.PLAIN;
            List<Row> items = List.of(
                    segment("items.tooltip", 0, new Row.Tone[] {a, rp, off}, "mode.panel", "mode.replace", "mode.off"),
                    segment("items.names", 2, new Row.Tone[] {a, bo, off}, "mode.on", "items.names.both", "mode.off"),
                    toggle("items.shiftpeek", true, null),
                    toggle("items.market", true, null),
                    segment("items.shot", 0, new Row.Tone[] {a, off}, "mode.hotkey", "mode.off")
                            .extra(() -> tr("button.keybinds"), () -> { }));
            List<Row> place = List.of(
                    segment("panel.anchor", 0, new Row.Tone[] {pl, pl}, "mode.follow", "mode.pinned"),
                    action("panel.place", "button.adjust"),
                    segment("panel.side", 0, new Row.Tone[] {pl, pl, pl}, "mode.auto", "mode.right", "mode.left"),
                    slider("panel.gap", 0, 200, 12, false));
            List<Row> look = List.of(colour("panel.theme"), colour("panel.colour"));
            List<Row> dialogue = List.of(
                    segment("dialogue.mode", 1, new Row.Tone[] {a, rp, off}, "mode.box", "mode.replace", "mode.off"),
                    segment("dialogue.choices", 1, new Row.Tone[] {a, rp, off}, "mode.box", "mode.replace", "mode.off"),
                    slider("dialogue.hold", 0, 30, 6, true),
                    toggle("dialogue.overlays", true, null));
            List<Row> world = List.of(
                    segment("world.nametag", 1, new Row.Tone[] {a, rp, off}, "mode.lookat", "mode.replace", "mode.off")
                            .extra(() -> tr("button.advanced"), () -> { }),
                    segment("world.chat", 1, new Row.Tone[] {rp, bo, off}, "mode.replace", "mode.both", "mode.off"),
                    toggle("world.titles", true, "mode.replace"),
                    toggle("world.bossbar", true, "mode.replace"),
                    segment("world.tracker", 0, new Row.Tone[] {rp, a, off}, "mode.replace", "mode.panel", "mode.off"),
                    toggle("world.objectives", true, "mode.replace"),
                    toggle("world.scoreboard", true, null),
                    toggle("world.helditem", true, null),
                    toggle("world.chatcopy", true, null));
            List<Row> langs = List.of(
                    select("data.language", true, "data.language.auto"),
                    select("data.fallback", true, "data.fallback.auto"),
                    select("data.ui", false, "data.ui.auto"),
                    toggle("data.collect", true, null),
                    toggle("data.collectgui", false, null),
                    toggle("data.debug", false, null));
            List<Row> tools = List.of(
                    segment("data.source", 0, new Row.Tone[] {pl, pl}, "data.source.github", "data.source.local"),
                    toggle("data.autoupdate", false, null),
                    Row.action("data.reload", () -> tr("data.reload"), () -> tr("data.reload.github"))
                            .label(() -> tr("data.reload.fetch")),
                    Row.status("data.version", () -> tr("data.version"), () -> tr("data.version.hint"))
                            .label(() -> tr("data.version.latest")).labelColour(() -> Ui.GREEN),
                    action("data.export", "data.export.button"),
                    action("data.submit", "data.submit.button"));
            List<Row.Tab> tabs = new ArrayList<>();
            // 「模組支援」：Wynntils 那一列永遠在，另外兩列有裝才出現；版面照「都有」的情況量
            List<Row> mods = List.of(
                    toggle("world.wynntils", true, null),
                    toggle("items.wms", true, null),
                    toggle("items.wynnventory", true, null));
            tabs.add(tab("items", Icons.Icon.BOW, Preview.Scene.TOOLTIP,
                    group("group.items", false, items), group("group.mods", false, mods)));
            tabs.add(tab("panel", Icons.Icon.SCROLL, Preview.Scene.PANEL,
                    group("group.place", false, place), group("group.look", false, look)));
            tabs.add(tab("dialogue", Icons.Icon.BUBBLE, Preview.Scene.DIALOGUE, group("group.dialogue", false, dialogue)));
            tabs.add(tab("world", Icons.Icon.COMPASS, Preview.Scene.WORLD, group("group.world", false, world)));
            tabs.add(tab("data", Icons.Icon.BOOK, Preview.Scene.DATA,
                    group("group.lang", false, langs), group("group.tools", true, tools)));
            return tabs;
        }

        private Row.Group group(String key, boolean tool, List<Row> rows) {
            return new Row.Group(() -> tr(key), tool, rows);
        }

        private Row.Tab tab(String key, Icons.Icon icon, Preview.Scene scene, Row.Group... groups) {
            return new Row.Tab(key, () -> tr("tab." + key), () -> tr("tab." + key + ".about"), icon,
                    scene, List.of(groups));
        }

        @Override
        public int accent() {
            return Ui.parseHex((String) values.getOrDefault("panel.theme", "#6FA8D8"), 0xFF6FA8D8);
        }

        @Override
        public int frame() {
            return Ui.parseHex((String) values.getOrDefault("panel.colour", "#6FA8D8"), 0xFF6FA8D8);
        }

        @Override
        public int tone(Row.Tone tone) {
            return switch (tone) {
                case REPLACE -> ModeColours.replace(accent());
                case BOTH -> ModeColours.both(accent());
                case OFF -> 0x40FFFFFF;
                default -> accent();
            };
        }

        @Override
        public String tr(String key, Object... args) {
            String pattern = strings.get("wynnchayuan." + key);
            if (pattern == null) {
                pattern = english.get("wynnchayuan." + key);
            }
            if (pattern == null) {
                failures++;
                System.out.println("  [FAIL] 語言檔沒有 " + key);
                return key;
            }
            for (Object arg : args) {
                pattern = pattern.replaceFirst("%s", java.util.regex.Matcher.quoteReplacement(
                        String.valueOf(arg)));
            }
            return pattern;
        }

        @Override
        public String title() {
            return "WynnChaYuan";
        }

        @Override
        public String tagline() {
            return tr("header.tagline");
        }

        @Override
        public String version() {
            return "v0.2.8_2";
        }

        @Override
        public SettingsView.Status status() {
            return new SettingsView.Status(tr("footer.hint", "55,941"), Ui.TEXT_3);
        }

        @Override
        public boolean hasUpdate() {
            return false;
        }

        @Override
        public boolean blurred() {
            return true;
        }

        @Override
        public void openNotice() { }

        @Override
        public void openUpdates() { }

        @Override
        public void openCredits() { }

        @Override
        public void done() { }

        @Override
        public String clipboard() {
            return "";
        }

        @Override
        public void setClipboard(String text) { }

        @Override
        public void click() { }

        @Override
        public Preview.State preview() {
            Preview.State s = new Preview.State();
            s.loaded = 55941;
            s.loadedLabel = tr("preview.loaded");
            s.facts = new String[][] {
                {tr("data.language"), "繁體中文"}, {tr("data.fallback"), tr("data.fallback.off")},
                {tr("data.source"), tr("data.source.github")},
                {tr("data.version"), tr("data.version.latest"), "version"}};
            s.versionColour = Ui.GREEN;
            // 示意圖用真的譯文畫：俄文、西文比中文長一倍，疊不疊要看它們
            s.translate = text -> {
                String hit = corpus.get(text);
                return hit == null || hit.isBlank() ? text : hit;
            };
            return s;
        }

        @Override
        public void resetDone(String what) { }
    }
}
