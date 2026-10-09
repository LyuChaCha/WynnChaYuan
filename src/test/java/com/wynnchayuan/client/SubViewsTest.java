package com.wynnchayuan.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.wynnchayuan.client.ui.CreditsView;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.NotesView;
import com.wynnchayuan.client.ui.NoticeView;
import com.wynnchayuan.client.ui.PositionView;
import com.wynnchayuan.client.ui.Preview;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.SettingsView;
import com.wynnchayuan.client.ui.Shell;
import com.wynnchayuan.client.ui.Surface;
import com.wynnchayuan.client.ui.Ui;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 設定畫面以外的那幾張：使用須知、更新說明、貢獻者、調整面板位置、名牌進階。
 *
 * <h2>為什麼要有</h2>
 * 設定畫面改版之後，這幾張還是舊的樣子，使用者一打開就看得出是兩套
 * （2026-10-08 回報）。改成同一套畫法之後，跟 {@link SettingsViewTest} 一樣
 * 不開遊戲就能整張算出來——所以每種語言、幾種畫面大小都算一次，量：
 * <ul>
 *   <li>每個按鈕、開關、分段都在畫面裡面，沒有被擠到外面去；</li>
 *   <li>同一列的按鈕沒有疊在一起（俄文與西班牙文的字最長）；</li>
 *   <li>畫得到的每一個字，隨模組附的字型都有。</li>
 * </ul>
 * 再點幾下最要緊的：拖一個框、存檔、全部回到預設、換說明的語言、勾「不再顯示」。
 *
 * <p>算出來的圖一樣存在 {@code build/ui-preview/}。
 */
public final class SubViewsTest {

    private static int failures = 0;

    private static void must(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("  [FAIL] " + what);
        }
    }

    private static final String[] LANGS = {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "en_us"};
    private static final int[][] SIZES = {{640, 360}, {960, 540}, {480, 270}, {427, 240}};

    private static long CLOCK = 1000;

    public static void main(String[] args) throws Exception {
        File out = new File("build/ui-preview");
        out.mkdirs();
        int frames = 0;

        for (String lang : LANGS) {
            AwtCanvas.missing.clear();
            FakeShell shell = new FakeShell(lang);
            for (int[] size : SIZES) {
                Map<String, Surface> views = new LinkedHashMap<>();
                views.put("notice", new NoticeView(shell, () -> false, () -> { }, () -> { }));
                views.put("notes", new NotesView(shell, new FakeNotes(shell, false)));
                views.put("notes-newer", new NotesView(shell, new FakeNotes(shell, true)));
                views.put("credits", new CreditsView(shell, new FakeCredits(shell)));
                views.put("position", new PositionView(shell, new FakePlaces(shell)));
                views.put("nametag", nametag(lang));
                for (var one : views.entrySet()) {
                    AwtCanvas c = render(one.getValue(), lang, size[0], size[1], 2, -100, -100);
                    String at = lang + " " + one.getKey() + " " + size[0] + "x" + size[1];
                    inside(at, one.getValue(), size[0], size[1]);
                    apart(at, one.getValue());
                    frames++;
                    if (size[0] == 640 || (size[0] == 427 && lang.equals("ru_ru"))) {
                        ImageIO.write(c.image, "png", new File(out,
                                lang + "-" + one.getKey() + (size[0] == 640 ? "" : "-small") + ".png"));
                    }
                }
            }
            StringBuilder gone = new StringBuilder();
            for (int cp : AwtCanvas.missing) {
                gone.appendCodePoint(cp);
            }
            must(lang + "：這幾張畫到的字，附的字型都有（缺「" + gone + "」，重跑 tools/build-ui-fonts.py）",
                    gone.length() == 0);
        }
        System.out.println("  [PASS] 算了 " + frames + " 張畫面（" + LANGS.length + " 種語言 × 6 張 × "
                + SIZES.length + " 種大小）");

        position(out);
        notes();
        notice();
        credits();

        if (failures > 0) {
            System.out.println("SubViews: " + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("SubViews: 全部通過，圖在 " + out.getPath());
    }

    private static AwtCanvas render(Surface view, String lang, int w, int h, int scale,
                                    double mx, double my) {
        AwtCanvas c = null;
        for (int i = 0; i < 8; i++) {
            c = new AwtCanvas(w, h, scale, lang);
            c.backdrop();
            view.render(c, w, h, mx, my, CLOCK += 90);
            c.done();
        }
        return c;
    }

    /** 每個叫得出名字的控制項都在畫面裡面。 */
    private static void inside(String at, Surface view, int w, int h) {
        for (var spot : view.spots().entrySet()) {
            float[] r = spot.getValue();
            if (spot.getKey().startsWith("box:") || spot.getKey().startsWith("grip:")) {
                continue;   // 框本來就可以拖到貼邊
            }
            must(at + "：" + spot.getKey() + " 凸出畫面（" + r[0] + "," + r[1] + " "
                            + r[2] + "×" + r[3] + "）",
                    r[2] <= 0 || (r[0] >= -0.5f && r[1] >= -0.5f
                            && r[0] + r[2] <= w + 0.5f && r[1] + r[3] <= h + 0.5f));
        }
    }

    /** 按鈕彼此不重疊。 */
    private static void apart(String at, Surface view) {
        List<Map.Entry<String, float[]>> buttons = new ArrayList<>();
        for (var spot : view.spots().entrySet()) {
            if (spot.getKey().startsWith("b:") && spot.getValue()[2] > 0) {
                buttons.add(spot);
            }
        }
        for (int i = 0; i < buttons.size(); i++) {
            for (int j = i + 1; j < buttons.size(); j++) {
                float[] a = buttons.get(i).getValue();
                float[] b = buttons.get(j).getValue();
                boolean overlap = a[0] < b[0] + b[2] - 0.5f && b[0] < a[0] + a[2] - 0.5f
                        && a[1] < b[1] + b[3] - 0.5f && b[1] < a[1] + a[3] - 0.5f;
                must(at + "：" + buttons.get(i).getKey() + " 與 " + buttons.get(j).getKey()
                        + " 疊在一起", !overlap);
            }
        }
    }

    private static void press(Surface view, String key) {
        float[] r = view.spot(key);
        must("找得到 " + key, r != null);
        if (r != null) {
            view.mouseDown(r[0] + r[2] / 2, r[1] + r[3] / 2, 0);
            view.mouseUp();
        }
    }

    // ------------------------------------------------------------ 調整面板位置

    private static void position(File out) throws Exception {
        FakeShell shell = new FakeShell("zh_tw");
        FakePlaces places = new FakePlaces(shell);
        PositionView view = new PositionView(shell, places);
        render(view, "zh_tw", 640, 360, 1, -100, -100);

        float[] box = view.spot("box:TOOLTIP");
        must("翻譯面板那個框畫出來了", box != null);
        if (box == null) {
            return;
        }
        // 區塊含框上面那條 13px 的名稱，所以框本身的 y 是區塊的 y + 13
        must("沒擺過的框放在預設位置，而且不會壓到頂列", box[0] == 20 && box[1] + 13 >= 56);
        // 預設位置下任務追蹤那個框疊在它左半邊上面，所以抓右半邊
        double gx = box[0] + 135;
        double gy = box[1] + 60;
        view.mouseDown(gx, gy, 0);
        view.mouseDrag(gx + 100, gy + 50);
        AwtCanvas c = render(view, "zh_tw", 640, 360, 2, gx + 100, gy + 50);
        ImageIO.write(c.image, "png", new File(out, "zh_tw-position-drag.png"));
        view.mouseUp();
        render(view, "zh_tw", 640, 360, 1, -100, -100);
        float[] moved = view.spot("box:TOOLTIP");
        must("拖了之後框跟著走（" + moved[0] + "," + moved[1] + "）",
                Math.abs(moved[0] - (box[0] + 100)) < 1.5f
                        && Math.abs(moved[1] - (box[1] + 50)) < 1.5f);
        must("還沒按儲存，什麼都不寫", places.stored.isEmpty() && places.commits == 0);

        press(view, "b:save");
        must("按儲存：五個框都寫進去，存一次檔", places.stored.size() == 5 && places.commits == 1);
        int[] tooltip = places.stored.get("TOOLTIP");
        must("寫進去的是拖過之後的位置", tooltip != null && tooltip[0] == 120);
        int[] dialogue = places.stored.get("DIALOGUE");
        must("置中的框存的是中心的 x（畫面一半）", dialogue != null && dialogue[0] == 320);
        must("沒拉過大小的框不寫大小", tooltip != null && tooltip[2] == -1);

        // 拉大小：任務追蹤的右下角
        render(view, "zh_tw", 640, 360, 1, -100, -100);
        float[] grip = view.spot("grip:TRACKER");
        must("能改大小的框右下角有把手", grip != null);
        must("照內容長的框沒有把手", view.spot("grip:TOOLTIP") == null);
        if (grip != null) {
            view.mouseDown(grip[0] + 5, grip[1] + 5, 0);
            view.mouseDrag(grip[0] + 45, grip[1] + 25);
            view.mouseUp();
            render(view, "zh_tw", 640, 360, 1, -100, -100);
            press(view, "b:save");
            int[] tracker = places.stored.get("TRACKER");
            must("拉過大小的框寫了新的寬高（" + tracker[2] + "×" + tracker[3] + "）",
                    tracker[2] == 152 && tracker[3] == 82);
        }

        // 全部回到預設
        render(view, "zh_tw", 640, 360, 1, -100, -100);
        press(view, "b:all");
        render(view, "zh_tw", 640, 360, 1, -100, -100);
        float[] back = view.spot("box:TOOLTIP");
        must("全部回到預設：框回到原來的地方", back[0] == box[0] && back[1] == box[1]);
        press(view, "b:save");
        must("回到預設之後大小也清掉", places.stored.get("TRACKER")[2] == -1);

        int before = places.closes;
        press(view, "b:cancel");
        must("取消會關掉畫面", places.closes == before + 1);

        // 存過的位置，下次打開要在同一個地方
        places.saved.put("TOOLTIP", new PositionView.Saved(300, 120, null, null));
        places.saved.put("DIALOGUE", new PositionView.Saved(320, 200, 260, 60));
        PositionView again = new PositionView(shell, places);
        render(again, "zh_tw", 640, 360, 1, -100, -100);
        float[] t = again.spot("box:TOOLTIP");
        float[] d = again.spot("box:DIALOGUE");
        must("存過的位置打開時原樣還原", t[0] == 300 && t[1] + 13 == 120);
        must("置中的框存的是中心：畫出來要往左退半個寬度（" + d[0] + "，寬 " + d[2] + "）",
                d[0] == 320 - 130 && d[2] == 260);
    }

    // ------------------------------------------------------------ 更新說明

    private static void notes() throws Exception {
        FakeShell shell = new FakeShell("ja_jp");
        FakeNotes host = new FakeNotes(shell, true);
        NotesView view = new NotesView(shell, host);
        render(view, "ja_jp", 640, 360, 1, -100, -100);
        must("介面是日文、沒有日文說明：預設給英文", host.language() == 0);
        press(view, "c:lang:1");
        must("點第二格換成繁中", host.language() == 1);
        render(view, "ja_jp", 640, 360, 1, -100, -100);
        must("有新版的時候有下載鈕", view.spot("b:download") != null);
        press(view, "b:download");
        must("下載鈕會叫到外面", host.downloads == 1);
        press(view, "b:back");
        must("返回會關掉畫面", host.closes == 1);

        NotesView plain = new NotesView(shell, new FakeNotes(shell, false));
        render(plain, "ja_jp", 640, 360, 1, -100, -100);
        must("沒有新版就沒有下載鈕", plain.spot("b:download") == null);
    }

    // ------------------------------------------------------------ 使用須知

    private static void notice() throws Exception {
        FakeShell shell = new FakeShell("zh_tw");
        boolean[] dismissed = {false};
        int[] closes = {0};
        NoticeView view = new NoticeView(shell, () -> dismissed[0],
                () -> dismissed[0] = !dismissed[0], () -> closes[0]++);
        render(view, "zh_tw", 640, 360, 1, -100, -100);
        press(view, "t:dismiss");
        must("勾「不再自動顯示」", dismissed[0]);
        view.mouseDown(5, 5, 0);
        must("點卡片外面不會關（要讀過才走）", closes[0] == 0);
        press(view, "b:ok");
        must("按「知道了」才關", closes[0] == 1);
        must("Enter 也能關", view.key(Surface.KEY_ENTER, false) && closes[0] == 2);
    }

    // ------------------------------------------------------------ 貢獻者

    private static void credits() throws Exception {
        FakeShell shell = new FakeShell("zh_tw");
        FakeCredits host = new FakeCredits(shell);
        CreditsView view = new CreditsView(shell, host);
        render(view, "zh_tw", 640, 360, 1, -100, -100);
        press(view, "b:badge");
        must("徽章開關點得到", host.toggles == 1);
        press(view, "b:back");
        must("返回會關掉畫面", host.closes == 1);
    }

    // ------------------------------------------------------------ 名牌進階

    /** 跟 {@code NametagScreen#rows} 同一張表：一個分段、三條滑桿。 */
    private static SettingsView nametag(String lang) throws Exception {
        SettingsViewTest.Fake fake = new SettingsViewTest.Fake(lang);
        int[] mode = {1};
        int[] hold = {1};
        int[] range = {6};
        int[] angle = {6};
        List<Row> rows = List.of(
                Row.segment("nametag.mode", () -> fake.tr("world.nametag"),
                                () -> fake.tr("nametag.desc.replace"))
                        .option(fake.tr("mode.lookat"), Row.Tone.ACCENT)
                        .option(fake.tr("mode.replace"), Row.Tone.REPLACE)
                        .option(fake.tr("mode.off"), Row.Tone.OFF)
                        .selected(() -> mode[0]).pick(i -> mode[0] = i)
                        .reset(() -> mode[0] == 1, () -> mode[0] = 1),
                Row.slider("nametag.hold", () -> fake.tr("nametag.hold"), () -> fake.tr("nametag.about1"))
                        .range(0, 15).value(() -> hold[0]).slide(v -> hold[0] = v)
                        .format(v -> v == 0 ? fake.tr("unit.forever") : fake.tr("unit.seconds", v))
                        .commit(() -> { }).reset(() -> hold[0] == 1, () -> hold[0] = 1),
                Row.slider("nametag.range", () -> fake.tr("nametag.range"), () -> fake.tr("nametag.about2"))
                        .range(2, 64).value(() -> range[0]).slide(v -> range[0] = v)
                        .format(String::valueOf)
                        .commit(() -> { }).reset(() -> range[0] == 6, () -> range[0] = 6),
                Row.slider("nametag.angle", () -> fake.tr("nametag.angle"), () -> fake.tr("nametag.about2"))
                        .range(1, 45).value(() -> angle[0]).slide(v -> angle[0] = v)
                        .format(v -> v + "°")
                        .commit(() -> { }).reset(() -> angle[0] == 6, () -> angle[0] = 6));
        return new SettingsView(List.of(new Row.Tab("nametag", () -> fake.tr("nametag.title"),
                () -> fake.tr("nametag.about1"), Icons.Icon.COMPASS, Preview.Scene.WORLD,
                List.of(new Row.Group(() -> fake.tr("world.nametag"), false, rows)))), fake, 0, true);
    }

    // ------------------------------------------------------------ 假的外殼與資料

    static final class FakeShell implements Shell {
        private final Map<String, String> strings;
        private final Map<String, String> english;
        private final String lang;

        FakeShell(String lang) throws Exception {
            this.lang = lang;
            strings = load(lang);
            english = load("en_us");
        }

        private static Map<String, String> load(String lang) throws Exception {
            try (InputStream in = SubViewsTest.class.getResourceAsStream(
                    "/assets/wynnchayuan/lang/" + lang + ".json")) {
                return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, String>>() { }.getType());
            }
        }

        @Override
        public int accent() {
            return 0xFF6FA8D8;
        }

        @Override
        public int frame() {
            return 0xFF6FA8D8;
        }

        @Override
        public boolean blurred() {
            return true;
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
        public String language() {
            return lang;
        }

        @Override
        public void click() { }
    }

    /** 更新說明：讀倉庫根目錄那份真的 version.json，字數與語言都是實際的。 */
    private static final class FakeNotes implements NotesView.Host {
        private static final String[] CODES = {"en_us", "zh_tw", "zh_cn"};
        private final JsonObject notes;
        private final boolean newer;
        private final FakeShell shell;
        private int chosen = -1;
        int downloads;
        int closes;

        FakeNotes(FakeShell shell, boolean newer) throws Exception {
            this.shell = shell;
            this.newer = newer;
            JsonObject root = new Gson().fromJson(
                    Files.readString(Path.of("version.json"), StandardCharsets.UTF_8), JsonObject.class);
            notes = root.getAsJsonObject("notes");
        }

        @Override
        public List<String> versions() {
            return new ArrayList<>(notes.keySet());
        }

        @Override
        public NotesView.Note notes(String version, String lang) {
            JsonObject one = notes.getAsJsonObject(version);
            if (one == null) {
                return null;
            }
            JsonObject in = one.has(lang) ? one.getAsJsonObject(lang) : one;
            List<String> items = new ArrayList<>();
            if (in.has("items")) {
                for (JsonElement e : in.getAsJsonArray("items")) {
                    items.add(e.getAsString());
                }
            }
            return new NotesView.Note(in.has("headline") ? in.get("headline").getAsString() : "", items);
        }

        @Override
        public String running() {
            // 有新版的那一種：假裝手上是第二新的
            List<String> all = versions();
            return newer && all.size() > 1 ? all.get(1) : all.get(0);
        }

        @Override
        public String newer() {
            return newer ? versions().get(0) : null;
        }

        @Override
        public String[] languages() {
            return CODES;
        }

        @Override
        public int language() {
            if (chosen >= 0) {
                return chosen;
            }
            for (int i = 0; i < CODES.length; i++) {
                if (CODES[i].equals(shell.language())) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public void pickLanguage(int index) {
            chosen = index;
        }

        @Override
        public void download() {
            downloads++;
        }

        @Override
        public void close() {
            closes++;
        }
    }

    /** 貢獻者：讀真的 credits.json，人數與名字長度都是實際的。 */
    private static final class FakeCredits implements CreditsView.Host {
        private final FakeShell shell;
        private final List<CreditsView.Section> sections = new ArrayList<>();
        private boolean badges = true;
        int toggles;
        int closes;

        FakeCredits(FakeShell shell) throws Exception {
            this.shell = shell;
            JsonObject root;
            try (InputStream in = SubViewsTest.class.getResourceAsStream(
                    "/assets/wynnchayuan/credits.json")) {
                root = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                        JsonObject.class);
            }
            for (JsonElement s : root.getAsJsonArray("sections")) {
                JsonObject section = s.getAsJsonObject();
                List<CreditsView.Member> members = new ArrayList<>();
                for (JsonElement m : section.getAsJsonArray("members")) {
                    JsonObject member = m.getAsJsonObject();
                    String name = member.has("key") ? shell.tr(member.get("key").getAsString())
                            : member.get("name").getAsString();
                    members.add(new CreditsView.Member(name,
                            member.has("mc") ? member.get("mc").getAsString() : null));
                }
                String role = section.has("key") ? shell.tr(section.get("key").getAsString())
                        : section.get("role").getAsString();
                int colour = 0xFF000000 | Integer.parseInt(
                        section.get("color").getAsString().substring(1), 16);
                sections.add(new CreditsView.Section(role, colour, members));
            }
        }

        @Override
        public List<CreditsView.Section> sections() {
            return sections;
        }

        @Override
        public String title() {
            return shell.tr("credits.title", "WynnChaYuan");
        }

        @Override
        public String version() {
            return "0.2.8_2";
        }

        @Override
        public String badgeLabel() {
            return shell.tr("credits.badges", shell.tr(badges ? "mode.on" : "mode.off"));
        }

        @Override
        public void toggleBadges() {
            badges = !badges;
            toggles++;
        }

        @Override
        public String styleLabel() {
            return shell.tr("credits.badges.style", shell.tr("credits.badges.all"));
        }

        @Override
        public void cycleStyle() { }

        @Override
        public void close() {
            closes++;
        }
    }

    /** 五個框：跟 {@code PositionScreen#specs} 同樣的大小與預設位置。 */
    private static final class FakePlaces implements PositionView.Host {
        private final FakeShell shell;
        final Map<String, PositionView.Saved> saved = new HashMap<>();
        /** 存進去的 {@code {x, y, w, h}}；沒寫大小的是 -1。 */
        final Map<String, int[]> stored = new LinkedHashMap<>();
        int commits;
        int closes;

        FakePlaces(FakeShell shell) {
            this.shell = shell;
        }

        private PositionView.Line line(String key, int colour) {
            return new PositionView.Line(shell.tr(key), colour);
        }

        @Override
        public List<PositionView.Spec> boxes() {
            List<PositionView.Spec> out = new ArrayList<>();
            out.add(new PositionView.Spec("TOOLTIP", shell.tr("pos.box.tooltip"), 150, 90,
                    false, 90, 640, false, List.of(line("pos.demo.bow", 0xFFB48EDE),
                            line("pos.demo.dps", Ui.TEXT), line("pos.demo.level", Ui.TEXT),
                            line("pos.demo.steal", Ui.TEXT))));
            out.add(new PositionView.Spec("TRACKER", shell.tr("pos.box.tracker"), 112, 62,
                    true, 100, 640, false, List.of(line("pos.demo.tracker", 0xFFF5C56B),
                            line("pos.demo.goto", Ui.TEXT), line("pos.demo.slay", Ui.TEXT))));
            out.add(new PositionView.Spec("DIALOGUE", shell.tr("pos.box.dialogue"), 200, 42,
                    true, 100, 640, true, List.of(line("pos.demo.cook", 0xFF8FD694),
                            line("pos.demo.line", Ui.TEXT))));
            out.add(new PositionView.Spec("CHOICES", shell.tr("pos.box.choices"), 150, 34,
                    true, 100, 420, true, List.of(line("pos.demo.choice1", Ui.TEXT),
                            line("pos.demo.choice2", Ui.TEXT), line("pos.demo.choice3", Ui.TEXT))));
            out.add(new PositionView.Spec("NAMETAG", shell.tr("pos.box.nametag"), 96, 22,
                    false, 100, 640, true, List.of(line("pos.demo.cook", 0xFF8FD694))));
            return out;
        }

        @Override
        public PositionView.Saved saved(String id) {
            return saved.getOrDefault(id, new PositionView.Saved(null, null, null, null));
        }

        @Override
        public int[] defaultPosition(String id, int w, int h, int screenW, int screenH) {
            return switch (id) {
                case "TOOLTIP" -> new int[] {20, 20};
                case "TRACKER" -> new int[] {8, 40};
                case "DIALOGUE" -> new int[] {(screenW - w) / 2, screenH - 60 - h};
                case "NAMETAG" -> new int[] {(screenW - w) / 2, screenH / 2 + 16};
                default -> new int[] {screenW - w - 8, screenH * 5 / 8};
            };
        }

        @Override
        public void store(String id, int x, int y, Integer w, Integer h) {
            stored.put(id, new int[] {x, y, w == null ? -1 : w, h == null ? -1 : h});
        }

        @Override
        public void commit() {
            commits++;
        }

        @Override
        public void close() {
            closes++;
        }
    }
}
