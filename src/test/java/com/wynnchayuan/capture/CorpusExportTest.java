package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * F6「匯出未翻譯字串」寫出來的檔案。
 *
 * <h2>為什麼要測</h2>
 * 自動上傳拿掉之後，玩家交來的就是這個檔，而且是公開附在 Issue 上的。
 * 這裡釘住四件事：
 *
 * <ol>
 *   <li>檔案放在哪（跟本機的 captured.json 分開）</li>
 *   <li>公會頻道與別人的名字有沒有濾掉；本機檔案沒有被動到</li>
 *   <li>網址裡的 Issue 表單真的存在</li>
 *   <li><b>模組原始碼裡沒有任何把資料送出去的 HTTP 呼叫</b>，連出去的網址全是寫死的
 *       那幾個主機——CurseForge 就是因為這件事退件的，不能哪天又悄悄長回來</li>
 * </ol>
 */
public final class CorpusExportTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        where();
        filtered();
        issueForm();
        noUpload();
        report();
    }

    private static void where() {
        Path dir = Path.of("config", "wynnchayuan");
        Path file = CorpusExport.file(dir);
        check("匯出檔在 config/wynnchayuan/export/captured.json（實際 " + file + "）",
                file.equals(dir.resolve("export").resolve("captured.json")));
        check("匯出檔跟本機的 captured.json 不是同一個檔",
                !file.equals(dir.resolve("captured.json")));
    }

    private static void filtered() throws Exception {
        Path dir = Files.createTempDirectory("wcy-export");
        CaptureStore store = new CaptureStore(dir.resolve("captured.json"));
        String dialogue = "Good luck in there, recruits!";
        String gui = "Available Points";
        String guild = "anyone want to do a raid";
        String territory = "Controlled by Paladins United";
        store.record(dialogue, "desc", "quest", "dialogue/King's Recruit#Guard");
        store.record(gui, "text", "ui", "gui/line");
        store.record(guild, "text", "chat", "chat/GUILD");
        store.record(territory, "name", "label", "label/floating");

        CorpusExport.Result result = CorpusExport.write(dir, store, "0.1.9_5", "zh_tw");
        check("寫到 export/captured.json",
                result.file().equals(CorpusExport.file(dir))
                        && Files.isRegularFile(result.file()));

        JsonObject root = parse(result.file());
        JsonObject entries = root.getAsJsonObject("entries");
        List<String> srcs = srcs(entries);
        check("任務對話有匯出", srcs.contains(dialogue));
        check("介面文字有匯出", srcs.contains(gui));
        check("★ 公會頻道沒有匯出", !srcs.contains(guild));
        check("★ 公會領地（別人的公會名）沒有匯出", !srcs.contains(territory));
        check("count 跟實際條數一致（實際 " + entries.size() + " 條）",
                result.count() == entries.size()
                        && root.getAsJsonObject("_meta").get("count").getAsInt()
                                == entries.size());
        check("_meta 記了模組版本與語言",
                root.getAsJsonObject("_meta").get("mod").getAsString().equals("0.1.9_5")
                        && root.getAsJsonObject("_meta").get("lang").getAsString()
                                .equals("zh_tw"));
        check("對話那一條帶著任務名（import-captured.py 靠它分檔）",
                entries.entrySet().stream().anyMatch(e -> e.getValue().getAsJsonObject()
                        .has("quest")));

        Path local = dir.resolve("captured.json");
        check("匯出前本機的 captured.json 先寫到最新", Files.isRegularFile(local));
        check("本機的 captured.json 照樣全收（匯出不刪東西）",
                Files.isRegularFile(local)
                        && srcs(parse(local).getAsJsonObject("entries")).contains(guild));
        check("沒留下暫存檔",
                !Files.exists(result.file().resolveSibling("captured.json.tmp")));
    }

    private static void issueForm() {
        String url = CorpusExport.ISSUE_URL;
        check("Issue 網址指向這個倉庫的新 Issue 表單",
                url.startsWith("https://github.com/LyuChaCha/WynnChaYuan/issues/new?"));
        int at = url.indexOf("template=");
        String template = at < 0 ? "" : url.substring(at + "template=".length());
        check("網址裡的表單 .github/ISSUE_TEMPLATE/" + template
                        + " 真的存在（改名要兩邊一起改）",
                !template.isEmpty()
                        && Files.isRegularFile(Path.of(".github", "ISSUE_TEMPLATE", template)));
    }

    /**
     * 模組原始碼裡沒有上傳，而且連出去的主機只有寫死的那幾個。
     *
     * <p>下載（GET）照常允許：譯文同步與更新檢查要用。擋的是「把東西送出去」的寫法。
     */
    private static void noUpload() throws Exception {
        Pattern send = Pattern.compile(
                "BodyPublishers|\\.POST\\(|\\.PUT\\(|HttpURLConnection|setDoOutput");
        Pattern host = Pattern.compile("\"https?://([^/\"]+)");
        // api.github.com 是唯讀的：問「譯文最後一次改動是哪一個 commit」，
        // 一次 GET、不帶任何本機資料。見 RemoteSync#remoteVersion。
        Set<String> allowed = Set.of(
                "raw.githubusercontent.com", "cdn.jsdelivr.net", "github.com",
                "api.github.com");
        List<String> senders = new ArrayList<>();
        Set<String> hosts = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (send.matcher(text).find()) {
                    senders.add(file.getFileName().toString());
                }
                Matcher m = host.matcher(text);
                while (m.find()) {
                    hosts.add(m.group(1));
                }
            }
        }
        check("★ 模組原始碼裡沒有任何把資料送出去的 HTTP 呼叫"
                        + (senders.isEmpty() ? "" : "：" + senders),
                senders.isEmpty());
        check("掃到了寫死的網址（確認掃描有意義）：" + hosts, !hosts.isEmpty());
        check("★ 寫死的網址只有 GitHub 與 jsDelivr",
                allowed.containsAll(hosts));
        check("CorpusUpload 不在了", !Files.exists(
                Path.of("src/main/java/com/wynnchayuan/capture/CorpusUpload.java")));
    }

    private static JsonObject parse(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static List<String> srcs(JsonObject entries) {
        List<String> out = new ArrayList<>();
        for (var e : entries.entrySet()) {
            out.add(e.getValue().getAsJsonObject().get("src").getAsString());
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "CorpusExport: 全部通過" : "CorpusExport: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
