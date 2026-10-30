package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ErrorDebugTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-error-debug");
        ErrorDebug.note("ErrorDebugTest#early", null, new IllegalStateException("還沒有資料夾"));
        ErrorDebug.into(dir);
        for (int i = 0; i < 5; i++) {
            ErrorDebug.note("ErrorDebugTest#loop", "第 " + i + " 次", new IllegalStateException("同一個錯"));
        }
        ErrorDebug.note("ErrorDebugTest#loop", null, new IllegalArgumentException("另一種例外"));
        ErrorDebug.note("ErrorDebugTest#null", null, null);

        String written = Files.readString(dir.resolve("error-debug.txt"), StandardCharsets.UTF_8);
        check("同一處同一種例外只寫一次", count(written, "同一個錯") == 1);
        check("另一種例外另外寫一筆", written.contains("另一種例外"));
        check("記下處理中的內容", written.contains("處理中：第 0 次"));
        check("沒有資料夾時不寫", !written.contains("還沒有資料夾"));
        System.out.println(failures == 0 ? "ErrorDebug: 全部通過" : "ErrorDebug: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static int count(String text, String word) {
        int n = 0;
        for (int at = text.indexOf(word); at >= 0; at = text.indexOf(word, at + 1)) {
            n++;
        }
        return n;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
