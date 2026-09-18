package com.wynnchayuan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 替「某一句別的語言還沒翻」的測試<b>自己造</b>一個缺口。
 *
 * <p>先前那幾條測試是從語料裡當場挑一句「繁體有、簡體沒翻」的——語言補齊之後
 * （2026-09-18 各語言都補到跟繁體一樣多），這樣的句子一條都不剩，測試就架不起
 * 情境而整條紅掉。缺口是測試要的<b>情境</b>，不該靠語料剛好還沒補完。
 *
 * <p>做法：把那個語言的資料夾整份複製到暫存目錄，只把 {@code key} 那一條的譯文清空。
 */
public final class CorpusGap {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private CorpusGap() {}

    /** 複製 {@code dir}，把原文是 {@code key} 的譯文全部清空；回傳複本的位置。 */
    public static Path without(Path dir, String key) throws IOException {
        Path out = Files.createTempDirectory("wynnchayuan-gap");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        for (Path file : files) {
            Path target = out.resolve(dir.relativize(file).toString());
            Files.createDirectories(target.getParent());
            if (!file.toString().endsWith(".json")) {
                Files.copy(file, target);
                continue;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (!text.contains(jsonText(key))) {
                Files.writeString(target, text, StandardCharsets.UTF_8);
                continue;
            }
            JsonElement root = JsonParser.parseString(text);
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("entries") && obj.get("entries").isJsonObject()) {
                    for (var e : obj.getAsJsonObject("entries").entrySet()) {
                        JsonObject entry = e.getValue().getAsJsonObject();
                        if (entry.has("src") && key.equals(entry.get("src").getAsString())) {
                            entry.addProperty("dst", "");
                        }
                    }
                } else if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                    obj.addProperty(key, "");
                }
            }
            Files.writeString(target, GSON.toJson(root), StandardCharsets.UTF_8);
        }
        return out;
    }

    /** {@code key} 寫進 JSON 字串之後的樣子，用來先粗篩要不要解析這個檔。 */
    private static String jsonText(String key) {
        String quoted = GSON.toJson(key);
        return quoted.substring(1, quoted.length() - 1);
    }
}
