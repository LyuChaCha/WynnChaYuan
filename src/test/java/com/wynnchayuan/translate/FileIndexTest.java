package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 譯文清單：repo 上新增的譯文檔要真的被同步下來。
 *
 * <h2>先前壞在哪</h2>
 * {@code RemoteSync} 決定「要下載哪些檔案」時，讀的是
 * {@code FileIndex.bundled()}——也就是<b>打包在 jar 裡</b>的 {@code _index.json}。
 * 程式碼裡有一行註解寫著「先更新清單，才知道還有哪些新檔案」，但那份剛抓下來的
 * 清單從頭到尾沒有被讀過：清單在迴圈開始前就從 jar 算好了，而且還被快取起來。
 *
 * <p>後果是譯文<b>新增檔案</b>時，GitHub 上明明有，玩家卻要等下一次發版才拿得到。
 * 這正好抵銷掉這支同步存在的理由——編輯既有檔案不必發版，新增檔案卻要。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>{@code inDirectory} 只看那個資料夾，讀不到就回空清單——<b>不套內建退路</b>。
 *       套了退路的話，「遠端比內建多出哪些」就永遠算不出來。</li>
 *   <li>資料夾裡的清單有新檔名時，那個檔名要出現在最後的下載清單裡。</li>
 *   <li>遠端清單壞掉或缺席時，內建那份仍然保底，不會突然變成零個檔。</li>
 * </ol>
 */
public final class FileIndexTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wcy-index");

        // 1. 沒有 _index.json：空清單，不是內建退路
        check("資料夾裡沒有清單就回空的（不套退路）",
                FileIndex.inDirectory(dir).isEmpty());

        // 2. 遠端清單帶進新檔
        write(dir, "{\"files\": [\"ui-labels.json\", \"brand-new-area.json\"]}");
        List<String> remote = FileIndex.inDirectory(dir);
        check("讀得到遠端清單裡的新檔名",
                remote.contains("brand-new-area.json"));
        check("清單濾掉底線開頭的檔（_index.json 自己不在裡面）",
                remote.stream().noneMatch(n -> n.startsWith("_")));

        // 3. 合併之後，新檔與內建的檔案都在
        java.util.LinkedHashSet<String> merged =
                new java.util.LinkedHashSet<>(FileIndex.bundled("zh_tw"));
        int bundled = merged.size();
        merged.addAll(remote);
        check("合併後新檔進得來", merged.contains("brand-new-area.json"));
        check("合併後內建的檔一個都沒少", merged.size() >= bundled);
        check("內建清單本來就沒有這個新檔（確認測試有意義）",
                !FileIndex.bundled("zh_tw").contains("brand-new-area.json"));

        // 4. 遠端清單是壞的 JSON：不能讓下載清單變成空的
        write(dir, "<html>404</html>");
        java.util.LinkedHashSet<String> safe =
                new java.util.LinkedHashSet<>(FileIndex.bundled("zh_tw"));
        safe.addAll(FileIndex.inDirectory(dir));
        check("遠端清單壞掉時內建那份仍然保底", safe.size() >= bundled);

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
