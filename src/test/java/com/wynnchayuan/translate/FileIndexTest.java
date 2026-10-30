package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FileIndexTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wcy-index");

        check("資料夾裡沒有清單就回空的（不套退路）",
                FileIndex.inDirectory(dir).isEmpty());

        write(dir, "{\"files\": [\"ui-labels.json\", \"brand-new-area.json\"]}");
        List<String> remote = FileIndex.inDirectory(dir);
        check("讀得到遠端清單裡的新檔名",
                remote.contains("brand-new-area.json"));
        check("清單濾掉底線開頭的檔（_index.json 自己不在裡面）",
                remote.stream().noneMatch(n -> n.startsWith("_")));

        java.util.LinkedHashSet<String> merged =
                new java.util.LinkedHashSet<>(FileIndex.bundled("zh_tw"));
        int bundled = merged.size();
        merged.addAll(remote);
        check("合併後新檔進得來", merged.contains("brand-new-area.json"));
        check("合併後內建的檔一個都沒少", merged.size() >= bundled);
        check("內建清單本來就沒有這個新檔（確認測試有意義）",
                !FileIndex.bundled("zh_tw").contains("brand-new-area.json"));

        write(dir, "<html>404</html>");
        java.util.LinkedHashSet<String> safe =
                new java.util.LinkedHashSet<>(FileIndex.bundled("zh_tw"));
        safe.addAll(FileIndex.inDirectory(dir));
        check("遠端清單壞掉時內建那份仍然保底", safe.size() >= bundled);

        write(dir, "{\"files\": [\"ok.json\", \"../../evil.json\", \"/etc/evil.json\","
                + " \"C:\\\\evil.json\", \"ability/mage.json\"]}");
        List<String> guarded = FileIndex.inDirectory(dir);
        check("★ 帶 .. 或絕對路徑的名字不收，一般的照收（實際 " + guarded + "）",
                guarded.contains("ok.json") && guarded.contains("ability/mage.json")
                        && guarded.size() == 2);

        report();
    }

    private static void write(Path dir, String json) throws Exception {
        Files.write(dir.resolve("_index.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("譯文清單：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("譯文清單：全部通過");
    }
}
