package com.wynnchayuan.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HotPathCacheTest {

    private static int failures = 0;

    // 類別簡名要跟 WynntilsText.TRACKER 一樣
    private static final class ContentTrackerOverlay { }

    private static final Path ROOT = Path.of("src/main/resources/assets/wynnchayuan/translations");

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path tmp = Files.createTempDirectory("wcy-hotpath");
        CollectorConfig config = new CollectorConfig(tmp.resolve("config.json"));
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(ROOT.resolve(Languages.DEFAULT)));
        CaptureStore captured = new CaptureStore(tmp.resolve("captured.json"));
        captured.knowsTranslations(store::hasTranslation);
        captured.knowsSources(store::hasTranslation);
        captured.knowsLonger(store::hasLonger);
        set("config", config);
        set("translations", store);
        set("store", captured);

        tooltips(store);
        tracker(config, store, captured);
        nameplates(config, store, captured);
        nameSetting(config, store);

        System.out.println(failures == 0 ? "\n算繪端加速：全部通過"
                : "\n算繪端加速：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static final Pattern NUMBER = Pattern.compile("\\{~\\d*}");
    private static final Pattern OTHER = Pattern.compile("\\{[^}]*}");

    private static void tooltips(TranslationStore store) throws Exception {
        List<List<String>> blocks = new ArrayList<>();
        List<String> singles = new ArrayList<>();
        for (String file : List.of("ability.json", "major-id.json", "gui.json", "misc.json",
                "gear-weapon.json", "gear-armour.json", "charm.json", "tome.json", "aspect-desc.json",
                "lootrun.json", "raid.json", "cave.json", "discovery.json", "ui-labels.json")) {
            Path path = ROOT.resolve(Languages.DEFAULT).resolve(file);
            if (!Files.exists(path)) {
                continue;
            }
            int n = 0;
            for (var e : JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                    .getAsJsonObject().entrySet()) {
                if (e.getKey().startsWith("_") || !e.getValue().isJsonPrimitive()) {
                    continue;
                }
                String filled = fill(e.getKey(), n++);
                if (filled == null) {
                    continue;
                }
                if (filled.indexOf('\n') >= 0) {
                    blocks.add(List.of(filled.split("\n", -1)));
                } else if (singles.size() < 4000) {
                    singles.add(filled);
                }
            }
        }
        report("組 tooltip 的材料（整段 " + blocks.size() + "、單行 " + singles.size() + "）",
                blocks.size() > 500 && singles.size() > 500);

        int total = 0;
        int translated = 0;
        int differ = 0;
        String example = null;
        CollectorConfig.ItemNames[] modes = {CollectorConfig.ItemNames.OFF,
                CollectorConfig.ItemNames.ON, CollectorConfig.ItemNames.BOTH};
        int b = 0;
        int s = 0;
        while (b < blocks.size() && total < 900) {
            List<Component> tip = new ArrayList<>();
            tip.add(line(singles.get(s++ % singles.size()), 0xFFAA00));
            tip.add(line("", 0xAAAAAA));
            int want = 6 + total % 20;
            while (tip.size() < want && b < blocks.size()) {
                for (String row : blocks.get(b++)) {
                    tip.add(line(row, 0xAAAAAA));
                }
                if (total % 3 == 0) {
                    tip.add(line("", 0xAAAAAA));
                }
                tip.add(line(singles.get(s++ % singles.size()), 0x55FF55));
            }
            store.setNameMode(modes[total % modes.length]);
            List<Component> reused = TooltipTranslator.translateLines(tip, store, true);
            List<Component> fresh = TooltipTranslator.translateLines(tip, store, false);
            total++;
            if (!reused.isEmpty()) {
                translated++;
            }
            if (!reused.equals(fresh) || !text(reused).equals(text(fresh))) {
                differ++;
                if (example == null) {
                    example = text(tip) + " → 只切一次 " + text(reused) + " / 每次重切 " + text(fresh);
                }
            }
        }
        store.setNameMode(CollectorConfig.ItemNames.OFF);
        report("真的有翻到東西（" + translated + "/" + total + " 份）", translated > total / 3);
        report("★ tooltip 每行只切一次，跟每次重切的結果逐行相同（" + total + " 份，不同 " + differ
                + (example == null ? "" : "，例如 " + example) + "）", differ == 0);
    }

    private static String fill(String key, int seed) {
        Matcher m = NUMBER.matcher(key);
        StringBuilder sb = new StringBuilder();
        int k = 0;
        while (m.find()) {
            m.appendReplacement(sb, String.valueOf(3 + (seed * 7 + k++ * 13) % 97));
        }
        m.appendTail(sb);
        String out = sb.toString()
                .replace("{#}", "")
                .replace("{p}", "Ragni")
                .replace("{u}", "Steve");
        return OTHER.matcher(out).find() ? null : out;
    }

    private static Component line(String text, int rgb) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(rgb));
    }

    private static String text(List<Component> lines) {
        StringBuilder sb = new StringBuilder();
        for (Component c : lines) {
            sb.append(c.getString()).append(" | ").append(c).append('\n');
        }
        return sb.toString();
    }

    private static void tracker(CollectorConfig config, TranslationStore store,
                                CaptureStore captured) throws Exception {
        Method line = WynntilsText.class.getDeclaredMethod("line", StyledText.class,
                TranslationStore.class);
        line.setAccessible(true);
        Object overlay = new ContentTrackerOverlay();

        List<StyledText[]> panels = new ArrayList<>();
        int n = 0;
        for (var e : JsonParser.parseString(Files.readString(
                ROOT.resolve(Languages.DEFAULT).resolve("quest-name.json"), StandardCharsets.UTF_8))
                .getAsJsonObject().entrySet()) {
            if (e.getKey().startsWith("_") || e.getKey().contains("{")) {
                continue;
            }
            panels.add(new StyledText[] {
                    StyledText.fromString("§6§lQuest§r§7 - §e§l" + e.getKey()),
                    StyledText.fromString("§fTalk to Hans in the Ragni outskirts at [-" + (100 + n) + ", 45, -678]"),
                    StyledText.fromString("§7(Optional) Bring " + (n % 9 + 1) + " shiny pebbles to the curious child"),
            });
            if (++n >= 120) {
                break;
            }
        }
        for (String gear : List.of("Halcyon", "Warp", "Divzer", "Idol", "Ignis", "Grandmother")) {
            panels.add(new StyledText[] {StyledText.fromString(gear)});
        }
        for (String plain : List.of("Quest - Star Thief", "Mini-Quest - Gather Wheat", "Find the meteor's crash site.",
                "Speak to the Elder", "Rewards:", "Daily Objectives:")) {
            panels.add(new StyledText[] {StyledText.fromString(plain)});
        }

        CollectorConfig.ItemNames[] pageModes = {CollectorConfig.ItemNames.ON,
                CollectorConfig.ItemNames.BOTH, CollectorConfig.ItemNames.OFF};
        int compared = 0;
        int differ = 0;
        int translated = 0;
        String example = null;
        Map<Integer, java.util.Set<String>> perPanel = new java.util.HashMap<>();
        String[] phases = {"一開始", "第二、三幀（命中快取）", "名稱模式 BOTH", "按住 Shift",
                "名稱模式 OFF", "內容名稱 BOTH", "內容名稱 OFF", "換成簡中", "換回繁中"};
        List<StyledText[]> small = new ArrayList<>(panels.subList(0, 3));
        small.addAll(panels.subList(panels.size() - 12, panels.size()));
        for (int phase = 0; phase < phases.length; phase++) {
            switch (phase) {
                case 2 -> store.setNameMode(CollectorConfig.ItemNames.BOTH);
                case 3 -> store.setPeekPlainNames(true);
                case 4 -> {
                    store.setPeekPlainNames(false);
                    store.setNameMode(CollectorConfig.ItemNames.OFF);
                }
                case 5 -> store.setPageNameMode(pageModes[1]);
                case 6 -> store.setPageNameMode(pageModes[2]);
                case 7 -> {
                    store.setPageNameMode(pageModes[0]);
                    store.loadAll(List.of(ROOT.resolve("zh_cn")));
                }
                case 8 -> store.loadAll(List.of(ROOT.resolve(Languages.DEFAULT)));
                default -> { }
            }
            int frames = phase == 1 ? 2 : 1;
            for (int f = 0; f < frames; f++) {
                List<StyledText[]> these = phase < 2 ? panels : small;
                for (int p = 0; p < these.size(); p++) {
                    StyledText[] panel = these.get(p);
                    StyledText[] out = WynntilsText.lines(overlay, panel, config, store);
                    StringBuilder sig = new StringBuilder(phase + ":");
                    for (int i = 0; i < panel.length; i++) {
                        StyledText want = (StyledText) line.invoke(null, panel[i], store);
                        StyledText got = out[i];
                        compared++;
                        boolean same = want == panel[i] ? got == panel[i] : want.equals(got)
                                && want.getString().equals(got.getString());
                        if (want != panel[i]) {
                            translated++;
                        }
                        if (!same) {
                            differ++;
                            if (example == null) {
                                example = phases[phase] + "：" + panel[i].getString() + " 應為 "
                                        + want.getString() + " 實際 " + (got == null ? null : got.getString());
                            }
                        }
                        sig.append(got == null ? "" : got.getString()).append('|');
                    }
                    perPanel.computeIfAbsent(System.identityHashCode(panel), k -> new java.util.HashSet<>())
                            .add(sig.substring(sig.indexOf(":") + 1));
                }
            }
        }
        report("追蹤欄真的有翻到（" + translated + " 行次）", translated > 100);
        long changing = perPanel.values().stream().filter(v -> v.size() > 1).count();
        report("換設定之後確實有幾行會換結果（測試有在測東西：" + changing + " 組）", changing > 0);
        report("★ 追蹤欄每一幀、每次換設定之後，都跟當場重翻逐行相同（" + compared + " 行次，不同 "
                + differ + (example == null ? "" : "，例如 " + example) + "）", differ == 0);

        StyledText[] english = {
                StyledText.fromString("§fWalk carefully through the misty swamp near the old mill"),
                StyledText.fromString("§7(Optional) Bring the curious child some shiny pebbles"),
        };
        for (int f = 0; f < 50; f++) {
            WynntilsText.lines(overlay, english);
        }
        int[] seen = seen(captured, GlyphSplitter.toTemplate(english[0]));
        report("★ 沒譯文的追蹤欄行收進語料，而且只收一次（收到 " + seen[0] + " 條，seen=" + seen[1] + "）",
                seen[0] == 1 && seen[1] == 1);
    }

    private static void nameSetting(CollectorConfig config, TranslationStore store) throws Exception {
        int gearNames = 0;
        boolean followed = true;
        for (var e : JsonParser.parseString(Files.readString(
                ROOT.resolve(Languages.DEFAULT).resolve("gear-weapon.json"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("entries").entrySet()) {
            var entry = e.getValue().getAsJsonObject();
            String name = entry.get("src").getAsString();
            if (!"name".equals(entry.get("role").getAsString()) || name.contains("{")
                    || gearNames >= 50) {
                continue;
            }
            Component plate = Component.literal(name);
            String on = WynntilsNames.entityName(plate, config, store).getString();
            store.setTranslateNames(false);
            String off = WynntilsNames.entityName(plate, config, store).getString();
            store.setTranslateNames(true);
            if (!on.equals(name)) {
                gearNames++;
                followed &= off.equals(name)
                        && WynntilsNames.entityName(plate, config, store).getString().equals(on);
            }
        }
        report("★ 切換物品名翻譯，名牌馬上跟著變（" + gearNames + " 個裝備名）",
                gearNames > 0 && followed);
    }

    private static void nameplates(CollectorConfig config, TranslationStore store,
                                   CaptureStore captured) throws Exception {
        Method byPart = WynntilsText.class.getDeclaredMethod("byPart", StyledText.class,
                TranslationStore.class);
        byPart.setAccessible(true);
        Map<?, ?> names = map("NAMES");
        Map<?, ?> indicators = map("INDICATORS");

        List<Component> visible = new ArrayList<>();
        int n = 0;
        for (String file : List.of("label.json", "npc.json")) {
            for (var e : JsonParser.parseString(Files.readString(
                    ROOT.resolve(Languages.DEFAULT).resolve(file), StandardCharsets.UTF_8))
                    .getAsJsonObject().entrySet()) {
                String filled = e.getKey().startsWith("_") ? null : fill(e.getKey(), n);
                if (filled == null || filled.isBlank()) {
                    continue;
                }
                MutableComponent c = Component.literal(filled).withStyle(Style.EMPTY.withColor(0xFFAA00));
                if (n % 3 == 0) {
                    c.append(Component.literal(" [Lv. " + (n % 90 + 1) + "]")
                            .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
                }
                visible.add(c);
                if (++n >= 300) {
                    break;
                }
            }
        }
        visible.add(Component.literal("Wandering Stranger Without A Translation"));
        visible.add(Component.literal("-1234 ✤").withStyle(Style.EMPTY.withColor(0xFF5555)));
        visible.add(Component.literal("Critical").withStyle(Style.EMPTY.withColor(0xFF5555)));
        visible.add(Component.literal(""));
        visible.add(Component.literal("   "));

        String[] phases = {"第一次", "第二幀（命中）", "打了兩百幀之後", "換成簡中", "換回繁中"};
        int compared = 0;
        int differ = 0;
        int translated = 0;
        String example = null;
        for (int phase = 0; phase < phases.length; phase++) {
            if (phase == 2) {
                int fresh = 0;
                for (int frame = 0; frame < 200; frame++) {
                    for (Component c : visible.subList(0, 60)) {
                        WynntilsNames.entityName(c, config, store);
                    }
                    for (int k = 0; k < 20; k++) {
                        WynntilsNames.entityName(Component.literal("-" + (fresh * 7919 % 99999) + " ✤")
                                .withStyle(Style.EMPTY.withColor(0xFF5555)), config, store);
                        fresh++;
                    }
                    for (int k = 0; k < 5; k++) {
                        WynntilsNames.entityName(Component.literal("Passerby " + (frame * 5 + k)), config, store);
                    }
                }
                int kept = 0;
                for (Component c : visible.subList(0, 60)) {
                    if (names.containsKey(c) || indicators.containsKey(c)) {
                        kept++;
                    }
                }
                report("★ 打了兩百幀（四千個傷害數字、一千個路人）之後，畫面上的 60 個名牌一個都沒被擠掉（剩 "
                        + kept + "）", kept == 60);
                report("傷害數字有上限，沒有無限長大（" + indicators.size() + "）", indicators.size() <= 256);
            }
            if (phase == 3) {
                store.loadAll(List.of(ROOT.resolve("zh_cn")));
            }
            if (phase == 4) {
                store.loadAll(List.of(ROOT.resolve(Languages.DEFAULT)));
            }
            for (Component c : visible) {
                Component got = WynntilsNames.entityName(c, config, store);
                Component want = oldEntityName(c, store, byPart);
                compared++;
                if (want != c) {
                    translated++;
                }
                boolean same = want == c ? got == c : want.equals(got);
                if (!same) {
                    differ++;
                    if (example == null) {
                        example = phases[phase] + "：" + c.getString() + " 應為 " + want.getString()
                                + " 實際 " + got.getString();
                    }
                }
            }
        }
        report("名牌真的有翻到（" + translated + " 次）", translated > 50);
        report("★ 名牌跟原本的算法逐條相同（" + compared + " 次，不同 " + differ
                + (example == null ? "" : "，例如 " + example) + "）", differ == 0);

        Component normal = visible.get(0);
        WynntilsNames.entityName(normal, config, store);
        report("一般名牌要登記，登記的就是它的原文",
                StyledText.fromComponent(normal).equals(original(names, indicators, normal)));
        for (Component skip : visible.subList(visible.size() - 4, visible.size())) {
            WynntilsNames.entityName(skip, config, store);
            report("戰鬥數字、純圖示、空白不登記（" + skip.getString().strip() + "）",
                    original(names, indicators, skip) == null);
        }

        var mode = CollectorConfig.class.getDeclaredField("nametagMode");
        mode.setAccessible(true);
        Object was = mode.get(config);
        mode.set(config, CollectorConfig.NametagMode.LOOK_AT);
        report("注視小框模式下原樣回傳", WynntilsNames.entityName(normal, config, store) == normal);
        mode.set(config, was);

        Component stranger = Component.literal("Totally Unknown Wanderer Of The Mists");
        WynntilsNames.entityName(stranger, config, store);
        names.clear();
        indicators.clear();
        WynntilsNames.entityName(stranger, config, store);
        int[] seen = seen(captured, "Totally Unknown Wanderer Of The Mists");
        report("★ 沒譯文的名牌只收一次（收到 " + seen[0] + " 條，seen=" + seen[1] + "）",
                seen[0] == 1 && seen[1] == 1);
    }

    private static Component oldEntityName(Component name, TranslationStore store, Method byPart)
            throws Exception {
        StyledText text = StyledText.fromComponent(name);
        Component floating = LineTranslator.translateFloating(text, store);
        StyledText shown = floating != null ? StyledText.fromComponent(floating)
                : (StyledText) byPart.invoke(null, text, store);
        return shown == text ? name : shown.getComponent();
    }

    private static StyledText original(Map<?, ?> names, Map<?, ?> indicators, Component key)
            throws Exception {
        Object entry = names.containsKey(key) ? names.get(key) : indicators.get(key);
        if (entry == null) {
            throw new IllegalStateException("沒進快取：" + key.getString());
        }
        var accessor = entry.getClass().getDeclaredMethod("original");
        accessor.setAccessible(true);
        return (StyledText) accessor.invoke(entry);
    }

    private static Map<?, ?> map(String field) throws Exception {
        var f = WynntilsNames.class.getDeclaredField(field);
        f.setAccessible(true);
        return (Map<?, ?>) f.get(null);
    }

    private static int[] seen(CaptureStore captured, String src) throws Exception {
        var f = CaptureStore.class.getDeclaredField("entries");
        f.setAccessible(true);
        int count = 0;
        int seen = -1;
        for (Object o : ((Map<?, ?>) f.get(captured)).values()) {
            CaptureStore.Captured c = (CaptureStore.Captured) o;
            if (src.strip().equals(c.src)) {
                count++;
                if (seen < 0) {
                    seen = c.seen;
                }
            }
        }
        return new int[] {count, seen};
    }

    private static void set(String field, Object value) throws Exception {
        var f = WynnChaYuan.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static void report(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
