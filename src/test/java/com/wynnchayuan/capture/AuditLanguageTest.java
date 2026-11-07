package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class AuditLanguageTest {

    private static final String OVERFLOW = "[{\"where\":\"tooltip\",\"src\":\"Bring the ore to the forge\","
            + "\"dst\":\"鉱石を鍛冶場へ持っていこう\",\"room\":160,\"need\":173,\"unit\":\"px\"}]";

    public static void main(String[] args) throws Exception {
        Path file = write("{\"_meta\":{\"language\":\"ja_jp\"},\"overflow\":" + OVERFLOW + "}");
        OverflowAudit.clear();
        CaptureStore store = new CaptureStore(file);
        check("前提：讀回上次的過長記錄", OverflowAudit.size() == 1);
        store.useLanguage("ja_jp");
        check("同一個語言：記錄留著", OverflowAudit.size() == 1);
        store.useLanguage("zh_tw");
        check("★ 換了語言：上一個語言的過長記錄清掉", OverflowAudit.size() == 0);
        store.flush();
        JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        check("★ 存檔記下現在是哪個語言", "zh_tw".equals(
                saved.getAsJsonObject("_meta").get("language").getAsString()));
        check("★ 存檔裡沒有上一個語言的記錄", !saved.has("overflow"));

        Path old = write("{\"overflow\":" + OVERFLOW + "}");
        OverflowAudit.clear();
        CaptureStore legacy = new CaptureStore(old);
        legacy.useLanguage("zh_tw");
        check("舊檔沒記語言：不知道是哪一個，留著", OverflowAudit.size() == 1);
        legacy.useLanguage("ru_ru");
        check("★ 之後再換語言就清掉", OverflowAudit.size() == 0);

        OverflowAudit.clear();
        ColourAudit.clear();
        System.out.println(failures == 0 ? "換語言的診斷記錄：全部通過"
                : "換語言的診斷記錄：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static Path write(String json) throws Exception {
        Path file = Files.createTempDirectory("wynnchayuan-audit-lang").resolve("captured.json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }
}
