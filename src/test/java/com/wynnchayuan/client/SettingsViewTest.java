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

public final class SettingsViewTest {

    private static int failures = 0;

    private static void must(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("  [FAIL] " + what);
        }
    }

    private static final String[] LANGS = {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr", "en_us"};
    private static final String[] TABS = {"items", "panel", "dialogue", "world", "data", "badge"};

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
            SettingsView low = new SettingsView(fake.tabs(), fake, 0);
            low.showTab(3);
            ImageIO.write(render(low, 720, 300, 2).image, "png", new File(out, lang + "-world-short.png"));
        }
        LANG = "zh_tw";
        System.out.println("  [PASS] 算了 " + frames + " 張畫面（" + LANGS.length + " 種語言 × "
                + TABS.length + " 個分類 × 4 種大小）");

        Fake zh = new Fake("zh_tw");
        SettingsView view = new SettingsView(zh.tabs(), zh, 0);
        for (int tab = 0; tab < TABS.length; tab++) {
            view.showTab(tab);
            render(view, 640, 360, 1);
            must("zh_tw " + TABS[tab] + "：標準大小下不必換成兩列", !view.pageWide());
        }
        LANG = "ru_ru";
        Fake ru = new Fake("ru_ru");
        view = new SettingsView(ru.tabs(), ru, 3);
        render(view, 640, 360, 1);
        must("ru_ru world：選項太長，整頁換成兩列", view.pageWide());
        LANG = "zh_tw";

        interactions(out);
        for (String lang : new String[] {"zh_tw", "en_us"}) {
            privacyShot(out, lang, null);
            privacyShot(out, lang, PRIVACY_HOVER);
            privacyShot(out, lang, null, true, false);
            privacyShot(out, lang, null, false, true);
        }

        must("語料附的原文要先拿掉（認得出來的那種）",
                "骨弓".equals(Preview.bareName("骨弓 (Bony Bow)", "Bony Bow", 2)));
        must("語料認不出來、但結尾剛好是「 (原文)」的也拿掉",
                "骨弓".equals(Preview.bareName("骨弓 (Bony Bow)", "Bony Bow", -1)));
        must("沒附原文的不動", "骨弓".equals(Preview.bareName("骨弓", "Bony Bow", -1)));
        must("譯文本來就帶別的括號的不動",
                "緩慢 (每秒 1.5 次)".equals(Preview.bareName("緩慢 (每秒 1.5 次)", "Slow", -1)));

        must("沒編號的佔位符照順序填",
                "- Mobs slain: 42/100".equals(Preview.fill("- Mobs slain: {~}/{~}", "42", "100")));
        must("有編號的照編號填（譯文換過語序的那種）",
                "100 之中的 42".equals(Preview.fill("{~2} 之中的 {~1}", "42", "100")));
        must("數字不夠也不會炸、不留佔位符",
                "T2+: 0/0".equals(Preview.fill("T{~1}+: {~2}/{~3}", "2")));
        must("別種大括號不動", "{#}甲 3".equals(Preview.fill("{#}甲 {~}", "3")));
        for (String lang : LANGS) {
            if ("en_us".equals(lang)) {
                continue;
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

    private static String LANG = "zh_tw";

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

    private static long CLOCK = 1000;

    static String PRIVACY_HOVER = "privacy.delete";

    private static void privacyShot(File out, String lang, String hover) throws Exception {
        privacyShot(out, lang, hover, false, false);
    }

    private static void privacyShot(File out, String lang, String hover, boolean armed,
                                    boolean deleted) throws Exception {
        String was = LANG;
        LANG = lang;
        Fake fake = new Fake(lang);
        fake.armed = armed;
        fake.deleted = deleted;
        List<Row.Tab> tabs = fake.tabs();
        SettingsView view = new SettingsView(tabs, fake, tabs.size() - 1);
        view.showTab(tabs.size() - 1);
        render(view, 640, 360, 2);
        for (int i = 0; i < 30; i++) {
            view.scroll(-1);
        }
        render(view, 640, 360, 2);
        double mx = -100;
        double my = -100;
        if (hover != null) {
            for (SettingsView.Placed p : view.placed()) {
                if (p.id().equals(hover)) {
                    mx = p.x() + 20;
                    my = p.y() + p.h() / 2;
                }
            }
        }
        AwtCanvas c = null;
        for (int i = 0; i < 10; i++) {
            c = new AwtCanvas(640, 360, 2, lang);
            c.backdrop();
            view.render(c, 640, 360, mx, my, CLOCK += 90);
            c.done();
        }
        ImageIO.write(c.image, "png", new File(out, lang + "-badge-privacy"
                + (hover == null ? "" : "-hover") + (armed ? "-armed" : "")
                + (deleted ? "-deleted" : "") + ".png"));
        for (SettingsView.Placed p : view.placed()) {
            must(lang + " " + p.id() + "：在視窗裡", p.x() + p.w() <= view.window()[0] + view.window()[2]);
        }
        LANG = was;
    }

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

    private static final java.util.Set<String> STRETCHY = java.util.Set.of(
            "items.tooltip", "items.names", "items.pages", "items.shot", "panel.anchor", "panel.side", "panel.gap",
            "dialogue.mode", "dialogue.choices", "dialogue.hold", "world.nametag", "world.chat",
            "world.tracker", "data.language", "data.fallback", "data.ui", "data.source");

    private static void interactions(File out) throws Exception {
        Fake zh = new Fake("zh_tw");
        SettingsView view = new SettingsView(zh.tabs(), zh, 0);
        render(view, 640, 360, 1);

        SettingsView.Placed market = find(view, "items.market");
        boolean before = zh.flag("items.market");
        view.mouseDown(market.ctrlRight() - 8, market.y() + market.h() / 2, 0);
        must("點開關會翻過去", zh.flag("items.market") != before);

        SettingsView.Placed names = find(view, "items.names");
        view.mouseDown(names.ctrlLeft() + 6, names.y() + names.h() / 2, 0);
        must("點分段的第一格會選到它", zh.index("items.names") == 0);
        render(view, 640, 360, 1);
        must("改過之後這一頁可以重置（有東西不是預設值）", zh.dirty("items.names"));

        view.showTab(1);
        render(view, 640, 360, 1);
        SettingsView.Placed gap = find(view, "panel.gap");
        view.mouseDown(gap.ctrlLeft() + 12, gap.y() + gap.h() / 2, 0);
        view.mouseDrag(gap.ctrlLeft() + 60, gap.y() + gap.h() / 2);
        must("拖滑桿的時候值跟著變", zh.number("panel.gap") != 12);
        must("還沒放手不存檔", zh.commits == 0);
        view.mouseUp();
        must("放手才存檔", zh.commits == 1);

        SettingsView.Placed theme = find(view, "panel.theme");
        view.mouseDown(theme.ctrlRight() - 40, theme.y() + theme.h() / 2, 0);
        must("點色塊會打開色盤", view.popoverOpen());
        AwtCanvas c = render(view, 640, 360, 3);
        ImageIO.write(c.image, "png", new File(out, "zh_tw-picker.png"));
        view.key(SettingsView.KEY_ESC, false);
        must("Esc 先收色盤，不是關掉整個畫面", !view.popoverOpen());

        view.showTab(4);
        render(view, 640, 360, 1);
        SettingsView.Placed language = find(view, "data.language");
        view.mouseDown(language.ctrlLeft() + 20, language.y() + language.h() / 2, 0);
        must("點下拉會打開選單", view.popoverOpen());
        c = render(view, 640, 360, 3);
        ImageIO.write(c.image, "png", new File(out, "zh_tw-dropdown.png"));
        view.key(SettingsView.KEY_ESC, false);

        view.searchFor("翻譯");
        c = render(view, 640, 360, 3);
        ImageIO.write(c.image, "png", new File(out, "zh_tw-search.png"));
        must("搜尋「翻譯」找得到東西", !view.placed().isEmpty());
        view.searchFor("zzzz");
        render(view, 640, 360, 1);
        must("搜不到的時候沒有列，也不丟例外", view.placed().isEmpty());
        view.searchFor("");

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
                    segment("items.pages", 0, new Row.Tone[] {a, bo, off}, "mode.on", "items.names.both", "items.pages.off"),
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
            List<Row> mods = List.of(
                    toggle("world.wynntils", true, null),
                    toggle("items.wms", true, null),
                    toggle("items.wynnventory", true, null),
                    toggle("items.wynnmod", true, null));
            tabs.add(tab("items", Icons.Icon.BOW, Preview.Scene.TOOLTIP,
                    group("group.items", false, items), group("group.mods", false, mods)));
            tabs.add(tab("panel", Icons.Icon.SCROLL, Preview.Scene.PANEL,
                    group("group.place", false, place), group("group.look", false, look)));
            tabs.add(tab("dialogue", Icons.Icon.BUBBLE, Preview.Scene.DIALOGUE, group("group.dialogue", false, dialogue)));
            tabs.add(tab("world", Icons.Icon.COMPASS, Preview.Scene.WORLD, group("group.world", false, world)));
            tabs.add(tab("data", Icons.Icon.BOOK, Preview.Scene.DATA,
                    group("group.lang", false, langs), group("group.tools", true, tools)));
            tabs.add(badgeTab());
            return tabs;
        }

        boolean deleted;
        boolean armed;

        Row.Tab badgeTab() {
            List<Row> badge = List.of(
                    Row.status("badge.none", () -> tr("badge.none"), () -> tr("badge.none.hint"))
                            .label(() -> tr("badge.none.label")),
                    action("badge.contribute", "badge.contribute.button"),
                    action("badge.sponsor", "badge.sponsor.button"));
            List<Row> cape = List.of(
                    toggle("fun.capes", true, null),
                    Row.status("fun.nocape", () -> tr("fun.nocape"), () -> tr("fun.nocape.hint"))
                            .label(() -> tr("fun.nocape.label")),
                    action("fun.upload", "fun.upload.button"),
                    action("fun.remove", "fun.remove.button"),
                    action("fun.gallery", "fun.gallery.button"));
            List<Row> privacy = new ArrayList<>();
            privacy.add(Row.toggle("privacy.online", () -> tr("privacy.online"),
                    () -> tr("privacy.online.hint")).on(() -> true));
            privacy.add(action("privacy.policy", "fun.gallery.button"));
            if (deleted) {
                privacy.add(Row.status("privacy.delete", () -> tr("privacy.delete"),
                                () -> tr("privacy.delete.hint"))
                        .label(() -> tr("privacy.delete.state.done")).labelColour(() -> Ui.GREEN));
                privacy.add(action("privacy.resume", "privacy.resume.button"));
            } else {
                privacy.add(Row.action("privacy.delete", () -> tr("privacy.delete"),
                                () -> tr("privacy.delete.hint"))
                        .label(() -> tr(armed ? "privacy.delete.confirm" : "privacy.delete.button"))
                        .danger(() -> armed).run(() -> armed = true));
            }
            List<Row> perf = List.of(toggle("perf.highlight", true, null));
            return tab("badge", Icons.Icon.LEAF, Preview.Scene.CAPE,
                    group("group.badge", false, badge), group("group.cape", false, cape),
                    group("group.privacy", false, privacy), group("group.perf", false, perf));
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
