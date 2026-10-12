package com.wynnchayuan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.cape.CapeRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * 登記站：送出去的東西只有登入、披風開關與玩家自己選的圖；名單讀得對；上傳之前先驗得出問題。
 *
 * <h2>為什麼要釘</h2>
 * 這是整個模組唯一往外送資料的地方。{@code CorpusExportTest} 管的是「只有這一個檔會送、
 * 只送到這一個網址」，這裡管的是<b>送什麼</b>：哪天有人順手多加一欄（世界、版本、
 * 登入憑證……），這裡要紅。
 */
public final class PresenceTest {

    private static int failures = 0;

    private static final UUID ME = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID GHOST = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    public static void main(String[] args) throws Exception {
        // ---- 代碼 ----
        String id = Presence.idOf(ME);
        check("代碼是 24 個小寫十六進位字元（實際 " + id + "）",
                id != null && id.matches("[0-9a-f]{24}"));
        // SHA-256("wynnchayuan:" + 32 個 a) 的前 12 個位元組——登記站的 publicId 是同一條算式，
        // tools/presence/worker.test.mjs 用同一個 UUID。兩邊算的不一樣就互相看不到。
        check("★ 演算法跟登記站一樣（改了的話誰都看不到誰）",
                "3dd509c0fcb112e29c04ee34".equals(id) && id.equals(expected()));
        check("不同的人不同的代碼", !id.equals(Presence.idOf(OTHER)));
        check("沒有 UUID 就沒有代碼", Presence.idOf(null) == null);
        check("★ 代碼裡看不出 UUID", !id.contains("aaaaaaaa"));

        // ---- ★ 送出去的東西 ----
        String serverId = Presence.newServerId();
        check("給 Mojang 對的那串字是 40 個十六進位字元，每次都不一樣",
                serverId.matches("[0-9a-f]{40}") && !serverId.equals(Presence.newServerId()));
        JsonObject login = JsonParser.parseString(Presence.loginBody("Green_teaTW", serverId))
                .getAsJsonObject();
        check("★ 登入只有兩欄：name 與 serverId（實際 " + login.keySet() + "）",
                login.keySet().equals(Set.of("name", "serverId"))
                        && "Green_teaTW".equals(login.get("name").getAsString())
                        && serverId.equals(login.get("serverId").getAsString()));
        JsonObject hello = JsonParser.parseString(Presence.helloBody(true)).getAsJsonObject();
        check("★ 報到只有一欄：cape（實際 " + hello.keySet() + "）",
                hello.keySet().equals(Set.of("cape")) && hello.get("cape").getAsBoolean()
                        && !JsonParser.parseString(Presence.helloBody(false)).getAsJsonObject()
                                .get("cape").getAsBoolean());

        // ---- 名單 ----
        String other = Presence.idOf(OTHER);
        String ghost = Presence.idOf(GHOST);
        Presence.Roster roster = Presence.parse("{\"t\":1,\"users\":[\"" + id + "\",\"" + other
                + "\",\"not-an-id\"],\"capes\":{\"" + id + "\":1700000000000,\"" + ghost
                + "\":5},\"hidden\":[\"" + other + "\",\"" + ghost + "\"]}");
        check("名單讀得出來，格式不對的代碼丟掉", roster != null && roster.users().size() == 2);
        check("★ 不在 users 裡的人就算列在 capes／hidden 也不算", roster != null
                && roster.capes().keySet().equals(Set.of(id)) && roster.hidden().equals(Set.of(other)));
        check("不是 JSON、不是物件的整份不要", Presence.parse("<html>") == null
                && Presence.parse("[]") == null);
        check("少了欄位就是空的名單，不是出錯", Presence.parse("{}") != null
                && Presence.parse("{}").users().isEmpty());

        check("還沒抓到名單：誰都不算有裝", !Presence.installed(ME));
        Presence.load(roster);
        check("★ 名單上的人算有裝", Presence.installed(ME) && Presence.installed(OTHER));
        check("不在名單上的人不算", !Presence.installed(GHOST) && !Presence.installed(null));
        check("★ 上傳的披風照名單上的版本", Presence.capeVersion(ME) == 1700000000000L
                && Presence.capeVersion(OTHER) == 0 && Presence.capeVersion(GHOST) == 0);
        check("★ 關掉披風的人照名單", Presence.capeHidden(OTHER) && !Presence.capeHidden(ME));
        Presence.load(null);
        check("清掉之後回到空的", !Presence.installed(ME) && Presence.capeVersion(ME) == 0);

        // ---- 登記站回的理由 ----
        check("200 是成功", Presence.resultOf(200, "{\"ok\":true}") == Presence.Result.DONE);
        check("登記站說的每一種理由都認得",
                Presence.resultOf(400, "{\"why\":\"bad size\"}") == Presence.Result.BAD_SIZE
                        && Presence.resultOf(413, "{\"why\":\"too large\"}") == Presence.Result.TOO_LARGE
                        && Presence.resultOf(403, "{\"why\":\"not listed\"}") == Presence.Result.NOT_LISTED
                        && Presence.resultOf(403, "{\"why\":\"banned\"}") == Presence.Result.BANNED
                        && Presence.resultOf(403, "{\"why\":\"uploads off\"}") == Presence.Result.OFF
                        && Presence.resultOf(429, "{\"why\":\"slow down\"}") == Presence.Result.SLOW_DOWN
                        && Presence.resultOf(400, "{\"why\":\"too many frames\"}")
                                == Presence.Result.TOO_MANY_FRAMES);
        check("不認得的、不是 JSON 的都是「沒有成功」",
                Presence.resultOf(502, "<html>Bad Gateway</html>") == Presence.Result.FAILED
                        && Presence.resultOf(400, "{\"why\":\"???\"}") == Presence.Result.FAILED);

        // ---- 上傳之前先驗 ----
        byte[] gif = Files.readAllBytes(Path.of("src/test/resources/cape/template.gif"));   // 22×17，三格
        check("照範本畫的 GIF 可以上傳", CapeRules.check(gif) == null);
        check("★ 只畫一面的圖不能上傳——要照範本", CapeRules.check(Files.readAllBytes(
                Path.of("src/test/resources/cape/anim.gif"))) == CapeRules.Problem.BAD_SIZE
                && CapeRules.check(png(40, 64)) == CapeRules.Problem.BAD_SIZE);
        check("★ 沒裁掉空白的完整版型也不能上傳", CapeRules.check(png(64, 32)) == CapeRules.Problem.BAD_SIZE
                && CapeRules.check(png(128, 64)) == CapeRules.Problem.BAD_SIZE);
        check("上限是 20 MB", CapeRules.MAX_BYTES == 20 * 1024 * 1024);
        check("★ 下載到一半的 GIF 不行", CapeRules.check(Arrays.copyOf(gif, gif.length - 40))
                == CapeRules.Problem.BAD_FORMAT);
        check("★ 太大的檔不行", CapeRules.check(new byte[CapeRules.MAX_BYTES + 1])
                == CapeRules.Problem.TOO_LARGE);
        check("不是圖的不行", CapeRules.check("<html>".getBytes()) == CapeRules.Problem.BAD_FORMAT
                && CapeRules.check(null) == CapeRules.Problem.BAD_FORMAT);
        check("PNG：範本的四種尺寸都可以（實際 " + CapeRules.SIZES.length + " 種）",
                CapeRules.SIZES.length == 4 && Arrays.stream(CapeRules.SIZES)
                .allMatch(size -> CapeRules.check(png(size[0], size[1])) == null));
        check("★ PNG：別的尺寸不行", CapeRules.check(png(100, 100)) == CapeRules.Problem.BAD_SIZE
                && CapeRules.check(png(66, 51)) == CapeRules.Problem.BAD_SIZE
                && CapeRules.check(png(352, 272)) == CapeRules.Problem.BAD_SIZE);
        byte[] wide = gif.clone();
        wide[6] = 23;                                   // 寬改成 23
        check("GIF：別的尺寸不行", CapeRules.check(wide) == CapeRules.Problem.BAD_SIZE);

        System.out.println(failures == 0 ? "登記站：全部通過" : "登記站：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 自己算一次，跟寫死的那個值互相對照（哪一邊抄錯都看得出來）。 */
    private static String expected() throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(
                ("wynnchayuan:" + "a".repeat(32)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            out.append(String.format("%02x", hash[i]));
        }
        return out.toString();
    }

    /** 只有檔頭的 PNG：簽章加 IHDR 的寬高，夠驗尺寸用。 */
    private static byte[] png(int width, int height) {
        byte[] bytes = new byte[64];
        int[] head = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
        for (int i = 0; i < head.length; i++) {
            bytes[i] = (byte) head[i];
        }
        bytes[16] = (byte) (width >>> 24);
        bytes[17] = (byte) (width >>> 16);
        bytes[18] = (byte) (width >>> 8);
        bytes[19] = (byte) width;
        bytes[20] = (byte) (height >>> 24);
        bytes[21] = (byte) (height >>> 16);
        bytes[22] = (byte) (height >>> 8);
        bytes[23] = (byte) height;
        return bytes;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
