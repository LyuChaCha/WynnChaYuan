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

public final class CorpusExport {

    private CorpusExport() {}

    public static final String ISSUE_URL =
            "https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml";

    static final String FOLDER = "export";

    static final String FILE_NAME = "captured.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static Path file(Path configDir) {
        return configDir.resolve(FOLDER).resolve(FILE_NAME);
    }

    public record Result(Path file, int count) {}

    public static Result write(Path configDir, CaptureStore store,
                               String version, String language) throws IOException {
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
        if (ColourAudit.size() > 0) {
            root.add("colours", ColourAudit.toJson());
        }
        if (OverflowAudit.size() > 0) {
            root.add("overflow", OverflowAudit.toJson());
        }
        if (UnappliedAudit.size() > 0) {
            root.add("unapplied", UnappliedAudit.toJson());
        }

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
