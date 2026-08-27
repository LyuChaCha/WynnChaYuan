package com.wynnchayuan.translate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 譯文按語言分層：{@code translations/<lang>/…}。
 *
 * <h2>為什麼要分</h2>
 * 原本所有譯文都平放在 {@code translations/} 底下，等於整個專案只能有一種語言。
 * 要讓別人翻成日文、西班牙文，就得有地方放——而且<b>各語言的檔案必須彼此獨立</b>，
 * 否則兩個語言的譯者會在同一個檔案上撞在一起，每個 PR 都要解衝突。
 *
 * <h2>為什麼每個語言都自己帶一份原文</h2>
 * 另一種做法是原文只存一份、各語言只存「編號 → 譯文」。那樣省空間，
 * 但這個專案的譯者是<b>用 GitHub 網頁編輯器改檔的</b>（分支名 {@code *-patch-N}
 * 就是網頁編輯出來的）。一個打開來只有雜湊和中文、看不到英文原文的檔案，
 * 在網頁上根本沒辦法翻。所以原文跟著譯文一起放。
 *
 * <p>代價是 repo 每多一種語言就多一份原文。玩家端不受影響：
 * {@link RemoteSync} 只抓使用者選的那一種。
 *
 * <h2>舊版的扁平目錄</h2>
 * 已經裝過舊版的人，{@code translations/} 底下是平的。第一次跑到新版時
 * 把那些檔案搬進 {@code translations/zh_tw/}——直接無視的話，
 * 使用者自己翻的東西會突然全部失效，而且看不出原因。
 */
public final class Languages {

    /** 專案的主要語言，也是找不到對應語言時的退路。 */
    public static final String DEFAULT = "zh_tw";

    /** jar 裡的資源前綴。 */
    private static final String ASSETS = "/assets/wynnchayuan/translations/";

    private Languages() {}

    /** 這個語言的譯文資料夾。 */
    public static Path dir(Path configDir, String lang) {
        return configDir.resolve("translations").resolve(normalise(lang));
    }

    /** 這個語言在 jar 裡的資源前綴，結尾帶斜線。 */
    public static String resource(String lang) {
        return ASSETS + normalise(lang) + "/";
    }

    /** 這個語言的相對路徑，給 {@link RemoteSync} 組 URL 用。 */
    public static String path(String lang) {
        return normalise(lang) + "/";
    }

    /**
     * jar 裡打包了哪些語言。
     *
     * <p>讀 {@code translations/_languages.json}——那是一份純清單，
     * 由 {@code tools/update-docs.py} 依實際資料夾產生，不必手動維護。
     */
    public static List<String> bundled() {
        try (InputStream in = Languages.class.getResourceAsStream(
                ASSETS + "_languages.json")) {
            if (in == null) {
                return List.of(DEFAULT);
            }
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            List<String> out = new ArrayList<>();
            for (com.google.gson.JsonElement el
                    : root.getAsJsonObject().getAsJsonArray("languages")) {
                out.add(el.getAsString());
            }
            return out.isEmpty() ? List.of(DEFAULT) : List.copyOf(out);
        } catch (Exception e) {
            return List.of(DEFAULT);
        }
    }

    /**
     * 把舊版的扁平目錄搬進語言資料夾。
     *
     * <p>只在<b>語言資料夾還不存在、而扁平目錄有 .json</b> 時做一次。
     * 搬而不是複製：留著兩份的話，使用者改了外面那份卻沒反應，
     * 那是最難查的一種問題。
     *
     * @return 搬了幾個檔案；不需要搬時回傳 0
     */
    public static int migrateFlat(Path configDir, String lang) {
        Path root = configDir.resolve("translations");
        Path target = dir(configDir, lang);
        try {
            if (!Files.isDirectory(root) || Files.exists(target)) {
                return 0;
            }
            List<Path> loose = new ArrayList<>();
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(Files::isRegularFile)
                     .filter(p -> p.getFileName().toString().endsWith(".json"))
                     .forEach(loose::add);
            }
            if (loose.isEmpty()) {
                return 0;
            }
            Files.createDirectories(target);
            int moved = 0;
            for (Path from : loose) {
                Path to = target.resolve(root.relativize(from));
                Files.createDirectories(to.getParent());
                Files.move(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                moved++;
            }
            // 搬完之後空掉的子資料夾（quest/、ability/）也一併清掉
            try (Stream<Path> rest = Files.walk(root)) {
                rest.sorted(java.util.Comparator.reverseOrder())
                    .filter(p -> !p.equals(root) && Files.isDirectory(p))
                    .forEach(p -> {
                        try (Stream<Path> inside = Files.list(p)) {
                            if (inside.findAny().isEmpty()) {
                                Files.delete(p);
                            }
                        } catch (Exception ignored) {
                            // 刪不掉就留著，不影響功能
                        }
                    });
            }
            System.out.println("[WynnChaYuan] 舊版的譯文已搬進 " + lang + "/（"
                    + moved + " 個檔案）");
            return moved;
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 搬移舊版譯文失敗：" + e.getMessage());
            return 0;
        }
    }

    /**
     * 設定裡沒指定時要用哪一種語言。
     *
     * <p>跟著<b>遊戲本身的語言</b>走，jar 裡沒有那一種就退回
     * {@link #DEFAULT}——這樣日文玩家裝好就是日文，不必先去翻設定。
     *
     * @param configured 設定檔裡寫的；空字串表示「跟著遊戲走」
     */
    public static String pick(String configured, String gameLanguage) {
        if (configured != null && !configured.isBlank()) {
            return normalise(configured);
        }
        String game = normalise(gameLanguage);
        return bundled().contains(game) ? game : DEFAULT;
    }

    /**
     * 這一種語言還沒翻到的地方，要不要拿另一種語言頂上；要的話拿哪一種。
     *
     * <h2>只在同一個語族之內回退</h2>
     * 新語言都是從 {@link #DEFAULT} 複製、把譯文清空的骨架，所以剛開張時
     * 一條都沒有。簡體中文的玩家在這種時候看到繁體是<b>好事</b>——讀得懂，
     * 而且比看英文原文近得多。
     *
     * <p>但日文或俄文的玩家不是。把繁體頂上去，他們會在完全沒有要求的情況下
     * 突然看到滿畫面中文，那比「維持英文原文」糟得多——原文至少是他們
     * 本來就在讀的東西。
     *
     * <p>所以比的是底線<b>前面</b>那一段：{@code zh_cn} 與 {@code zh_tw}
     * 同族，會回退；{@code ja_jp}、{@code ru_ru} 不同族，不回退，
     * 沒翻到的地方就保持原文。
     *
     * @return 要墊在底下的語言；不需要墊的話回傳 {@code null}
     */
    public static String fallbackFor(String lang) {
        String clean = normalise(lang);
        if (clean.equals(DEFAULT)) {
            return null;                       // 它自己就是底
        }
        return base(clean).equals(base(DEFAULT)) ? DEFAULT : null;
    }

    /** {@code zh_tw} 的語族是 {@code zh}。沒有底線就是整串。 */
    private static String base(String lang) {
        int cut = lang.indexOf('_');
        return cut < 0 ? lang : lang.substring(0, cut);
    }

    /** {@code zh-TW}、{@code ZH_TW} 都當成 {@code zh_tw}。 */
    private static String normalise(String lang) {
        if (lang == null || lang.isBlank()) {
            return DEFAULT;
        }
        String clean = lang.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        // 路徑安全：只准字母、數字與底線，擋掉 `../` 那種寫法
        return clean.matches("[a-z0-9_]+") ? clean : DEFAULT;
    }
}
