package com.wynnchayuan;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 遊戲內更新：下載回來的檔要驗、關遊戲時蓋到舊檔上、沒套用成功的下次看得到。
 *
 * <p>真的去 GitHub 下載那一段這裡不跑（測試不連網）；這裡釘住的是下載前後的每一道判斷——
 * 網址怎麼拼、什麼樣的檔才肯拿去蓋掉 mods 裡那個、蓋的方式、蓋不成的時候留下什麼。
 * 這幾道錯了的後果是玩家的模組壞掉，或是被換成別的東西。
 */
public final class SelfUpdateTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("wcy-selfupdate");

        // ---- 網址：寫死的 GitHub Releases，只有版本號是變數 ----
        String url = SelfUpdate.assetUrl("1.0.1_1");
        check("網址是這個倉庫的 Releases（實際 " + url + "）",
                url.equals("https://github.com/LyuChaCha/WynnChaYuan/releases/download/"
                        + "v1.0.1_1/wynnchayuan-1.0.1_1.jar"));
        check("一般的版本號過得了", SelfUpdate.plausibleVersion("1.0.1_1")
                && SelfUpdate.plausibleVersion("1.2.0") && SelfUpdate.plausibleVersion("0.9.0-beta.2"));
        // 版本號來自線上的 version.json；它要拿去拼網址與檔名，怪東西一律不收
        for (String bad : new String[] {null, "", "1.0/../../evil", "1.0?x=1", "1.0#a", "..",
                "1.0 1", "a".repeat(41), "1.0%2f", "1.0\\x"}) {
            check("★ 怪版本號不收（" + bad + "）", !SelfUpdate.plausibleVersion(bad));
        }
        check("下載放在設定資料夾底下的 update，不放 mods",
                SelfUpdate.holding(tmp, "1.2.0").equals(tmp.resolve("update").resolve("wynnchayuan-1.2.0.jar")));

        // ---- 什麼樣的檔才算「這個模組的那一版」 ----
        Path good = jar(tmp.resolve("good.jar"), "wynnchayuan", "1.2.0", 4000);
        check("模組 ID 與版本都對的 jar 過得了", SelfUpdate.isRelease(good, "1.2.0"));
        check("★ 版本不對的不收", !SelfUpdate.isRelease(good, "1.2.1"));
        check("★ 別的模組不收",
                !SelfUpdate.isRelease(jar(tmp.resolve("other.jar"), "wynntils", "1.2.0", 100), "1.2.0"));
        Path page = tmp.resolve("page.jar");
        Files.writeString(page, "<html><body>Not Found</body></html>");
        check("★ 錯誤頁面（不是 zip）不收", !SelfUpdate.isRelease(page, "1.2.0"));
        byte[] whole = Files.readAllBytes(good);
        Path half = tmp.resolve("half.jar");
        Files.write(half, Arrays.copyOf(whole, whole.length / 2));
        check("★ 下載到一半的檔不收", !SelfUpdate.isRelease(half, "1.2.0"));
        Path bare = tmp.resolve("bare.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(bare))) {
            zip.putNextEntry(new ZipEntry("readme.txt"));
            zip.write("x".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        check("★ 沒有 fabric.mod.json 的 zip 不收", !SelfUpdate.isRelease(bare, "1.2.0"));
        check("不存在的檔不收", !SelfUpdate.isRelease(tmp.resolve("nope.jar"), "1.2.0"));

        // ---- 套用：內容寫進舊檔的位置，檔名不變 ----
        Path mods = Files.createDirectories(tmp.resolve("mods"));
        Path old = jar(mods.resolve("wynnchayuan-1.0.1_1.jar"), "wynnchayuan", "1.0.1_1", 9000);
        Path fresh = SelfUpdate.holding(tmp, "1.2.0");
        SelfUpdate.copy(good, fresh);
        check("套用成功", SelfUpdate.apply(fresh, old));
        check("★ 舊檔的位置現在是新版（比舊檔短也不留尾巴）",
                Arrays.equals(Files.readAllBytes(old), whole) && SelfUpdate.isRelease(old, "1.2.0"));
        check("mods 裡還是只有那一個檔", count(mods) == 1);
        check("暫放的那一份刪掉了", !Files.exists(fresh));
        check("沒有東西可套用時回 false，舊檔不動",
                !SelfUpdate.apply(fresh, old) && SelfUpdate.isRelease(old, "1.2.0"));
        // 舊檔不見了（玩家自己換過）：不要憑空在 mods 裡生一個檔出來
        SelfUpdate.copy(good, fresh);
        Path gone = mods.resolve("wynnchayuan-gone.jar");
        check("★ 舊檔不在就不套用，也不新建", !SelfUpdate.apply(fresh, gone) && !Files.exists(gone));
        check("套用不成時新檔留著", Files.exists(fresh));

        // ---- 下次開遊戲：沒套用成功的看得到，套用過的清乾淨 ----
        Path found = SelfUpdate.leftover(tmp, "1.0.1_1");
        check("★ 沒套用成功的新版下次看得到（實際 " + found + "）", fresh.equals(found));
        check("已經在跑那一版的話，留下的檔清掉",
                SelfUpdate.leftover(tmp, "1.2.0") == null && !Files.exists(fresh));
        Path dir = tmp.resolve("update");
        Files.write(dir.resolve("wynnchayuan-1.3.0.jar.part"), new byte[10]);
        Files.write(dir.resolve("wynnchayuan-1.3.0.jar"), Arrays.copyOf(whole, 100));
        SelfUpdate.copy(good, dir.resolve("wynnchayuan-9.9.9.jar"));       // 檔名與內容的版本對不上
        check("半截的、壞的、名實不符的都清掉，不當成新版",
                SelfUpdate.leftover(tmp, "1.0.1_1") == null && count(dir) == 0);
        check("沒有 update 資料夾也沒事", SelfUpdate.leftover(tmp.resolve("nowhere"), "1.0.1_1") == null);

        // ---- 還沒按之前 ----
        SelfUpdate.reset();
        check("一開始是 IDLE、沒有進度、沒有目標", SelfUpdate.state() == SelfUpdate.State.IDLE
                && SelfUpdate.percent() == -1 && SelfUpdate.target() == null);
        SelfUpdate.applyPending();
        check("沒下載過的話，關遊戲時什麼都不動", SelfUpdate.isRelease(old, "1.2.0") && count(mods) == 1);
        SelfUpdate.start("1.0/../../evil");
        check("★ 怪版本號根本不會開始下載", SelfUpdate.state() == SelfUpdate.State.IDLE);

        if (failures > 0) {
            System.out.println(failures + " 項沒過");
            System.exit(1);
        }
        System.out.println("全部通過");
    }

    /** 做一個像模組的 jar：fabric.mod.json 加一塊填充（讓新舊兩版長度不同）。 */
    private static Path jar(Path path, String id, String version, int padding) throws Exception {
        Files.createDirectories(path.getParent());
        try (OutputStream out = Files.newOutputStream(path);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("fabric.mod.json"));
            zip.write(("{\"schemaVersion\":1,\"id\":\"" + id + "\",\"version\":\"" + version + "\"}")
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("padding.bin"));
            byte[] filler = new byte[padding];
            new java.util.Random(padding).nextBytes(filler);
            zip.write(filler);
            zip.closeEntry();
        }
        return path;
    }

    private static long count(Path dir) throws Exception {
        try (var files = Files.list(dir)) {
            return files.count();
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ✓ " : "  ✗ ") + what);
        if (!ok) {
            failures++;
        }
    }
}
