package com.wynnchayuan.capture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 把收集到的缺口匯出成一個檔，讓玩家<b>自己</b>交給翻譯團隊。
 *
 * <h2>為什麼不再自動上傳</h2>
 * 先前的做法是背景定期把缺口 POST 到收集站，端點位址寫在倉庫的設定檔裡、
 * 遠端可改。CurseForge 審核把這件事判定成「擷取到的遊戲文字送往可遠端更改的
 * 伺服器」而拒絕上架——從玩家的角度看，這確實分不出跟外洩有什麼不同。
 * 所以模組現在一個字都不送出去：匯出只是寫一個本機檔案，要不要交、交哪些，
 * 由玩家看過之後自己決定。
 *
 * <h2>為什麼另寫一份，不直接叫人交 captured.json</h2>
 * {@code captured.json} 是給本機自己看的，裡面有公會／隊伍頻道、別人的名牌，
 * 還有「語料收過但沒翻」那一段。匯出檔只放 {@link ShareFilter#shareable} 放行的
 * 缺口——跟當初自動上傳送出去的是同一批，濾網一條都沒少。
 *
 * <p>格式跟 {@code captured.json} 的 {@code entries} 一樣，所以
 * {@code tools/import-captured.py} 直接吃得下。
 */
public final class CorpusExport {

    private CorpusExport() {}

    /**
     * 提交用的 Issue 表單。
     *
     * <p>寫死在 jar 裡、不從任何線上設定讀：這只是一個讓玩家在瀏覽器裡打開的
     * 網址，而且 Minecraft 自己會先跳「確定要開啟連結嗎」。表單本體在
     * {@code .github/ISSUE_TEMPLATE/corpus.yml}，改名時兩邊一起改（有測試釘著）。
     */
    public static final String ISSUE_URL =
            "https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml";

    /** 匯出檔放在設定資料夾底下的這個子資料夾，打開時裡面只有要交的那一個檔。 */
    static final String FOLDER = "export";

    /** 檔名沿用 captured.json：Issue 表單與說明文件講的都是這個名字。 */
    static final String FILE_NAME = "captured.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** 匯出檔的位置。{@code configDir} 是 {@code config/wynnchayuan}。 */
    public static Path file(Path configDir) {
        return configDir.resolve(FOLDER).resolve(FILE_NAME);
    }

    /** 匯出的結果：寫到哪裡、寫了幾條。 */
    public record Result(Path file, int count) {}

    /**
     * 先把 {@code captured.json} 寫到最新，再寫出過濾後的匯出檔。
     *
     * <p>會做檔案 I/O，呼叫端自己決定要不要丟到背景執行緒。
     */
    public static Result write(Path configDir, CaptureStore store,
                               String version, String language) throws IOException {
        // 先落地：玩家按下匯出時，最近三十秒內收的還只在記憶體裡。
        store.flush();
        JsonObject rows = store.rowsJson(ShareFilter::shareable);

        JsonObject meta = new JsonObject();
        meta.addProperty("count", rows.size());
        meta.addProperty("mod", version == null ? "" : version);
        meta.addProperty("lang", language == null ? "" : language);
        meta.addProperty("note", "WynnChaYuan 匯出的未翻譯字串，只含遊戲自己的文字，"
                + "玩家名字與公會／隊伍／喊話／私訊已經濾掉。濾網是啟發式的，"
                + "送出前請自己看一遍，看到別人的名字或私人對話就刪掉那一條。"
                + " Untranslated strings exported by WynnChaYuan. Player names and "
                + "guild/party/shout/private chat are filtered out, but the filter is "
                + "heuristic: look through it and delete anything personal before sending.");

        JsonObject root = new JsonObject();
        root.add("_meta", meta);
        root.add("entries", rows);

        Path out = file(configDir);
        Files.createDirectories(out.getParent());
        Path tmp = out.resolveSibling(FILE_NAME + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        }
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        return new Result(out, rows.size());
    }
}
