package com.wynnchayuan;

import com.wynnchayuan.PresenceLogin;
import com.wynnchayuan.PresenceAccount;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.cape.CapeRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public final class PresenceTest {

    private static int failures = 0;

    private static final UUID ME = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID GHOST = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    public static void main(String[] args) throws Exception {
        String id = Presence.idOf(ME);
        check("代碼是 24 個小寫十六進位字元（實際 " + id + "）",
                id != null && id.matches("[0-9a-f]{24}"));
        check("★ 演算法跟登記站一樣（改了的話誰都看不到誰）",
                "3dd509c0fcb112e29c04ee34".equals(id) && id.equals(expected()));
        check("不同的人不同的代碼", !id.equals(Presence.idOf(OTHER)));
        check("沒有 UUID 就沒有代碼", Presence.idOf(null) == null);
        check("★ 代碼裡看不出 UUID", !id.contains("aaaaaaaa"));

        java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        java.security.KeyPair keys = generator.generateKeyPair();
        String uuid = ME.toString().replace("-", "");
        String challenge = "1791800000000." + "ab".repeat(16) + "." + "cd".repeat(32);
        check("★ 簽的那串字綁住網域、這一題、這個帳號",
                new String(PresenceLogin.loginMessage(challenge, uuid), java.nio.charset.StandardCharsets.UTF_8)
                        .equals("wynnchayuan.com login\n" + challenge + "\n" + uuid));
        byte[] signature = PresenceLogin.signLogin(keys.getPrivate(), challenge, uuid);
        java.security.Signature verifier = java.security.Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keys.getPublic());
        verifier.update(PresenceLogin.loginMessage(challenge, uuid));
        check("★ 簽名用公鑰驗得過（SHA256withRSA，登記站用同一種驗）", verifier.verify(signature));
        verifier.initVerify(keys.getPublic());
        verifier.update(PresenceLogin.loginMessage(challenge, OTHER.toString().replace("-", "")));
        check("換一個 UUID 就驗不過", !verifier.verify(signature));
        byte[] keySignature = {1, 2, 3, 4};
        String sent = PresenceLogin.loginBody(uuid, 1791900000000L, keys.getPublic().getEncoded(),
                keySignature, challenge, signature, "dGV4dHVyZXM=", "c2lnbmVk");
        JsonObject login = JsonParser.parseString(sent).getAsJsonObject();
        check("★ 登入送的就是這七欄（實際 " + login.keySet() + "）",
                login.keySet().equals(Set.of("uuid", "expiresAt", "publicKey", "keySignature",
                        "challenge", "signature", "textures"))
                        && login.getAsJsonObject("textures").keySet().equals(Set.of("value", "signature")));
        java.util.Base64.Decoder b64 = java.util.Base64.getDecoder();
        check("公鑰、兩份簽章是 base64，內容原樣", uuid.equals(login.get("uuid").getAsString())
                && login.get("expiresAt").getAsLong() == 1791900000000L
                && Arrays.equals(b64.decode(login.get("publicKey").getAsString()), keys.getPublic().getEncoded())
                && Arrays.equals(b64.decode(login.get("keySignature").getAsString()), keySignature)
                && Arrays.equals(b64.decode(login.get("signature").getAsString()), signature)
                && challenge.equals(login.get("challenge").getAsString()));
        String privateKey = java.util.Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded());
        check("★ 私鑰不在送出去的內容裡", !sent.contains(privateKey.substring(0, 64))
                && !sent.contains(privateKey.substring(privateKey.length() - 64)));
        JsonObject linkSent = JsonParser.parseString(Presence.linkBody("ABCD2345")).getAsJsonObject();
        check("★ 綁定只有一欄：code（實際 " + linkSent.keySet() + "）",
                linkSent.keySet().equals(Set.of("code")) && "ABCD2345".equals(linkSent.get("code").getAsString()));
        check("綁定碼不計較大小寫與前後空白", "ABCD2345".equals(PresenceAccount.linkCode(" abcd2345 ")));
        check("★ 不像綁定碼的不送（太短、有 0／O／1／I、有怪字元）", PresenceAccount.linkCode("ABC") == null
                && PresenceAccount.linkCode("ABCD0345") == null && PresenceAccount.linkCode("ABCDO345") == null
                && PresenceAccount.linkCode("ABCD1345") == null && PresenceAccount.linkCode("ABCDI345") == null
                && PresenceAccount.linkCode("ABCD-345") == null && PresenceAccount.linkCode(null) == null);
        check("登記站的回答對得上結果", PresenceAccount.linkResultOf(200) == PresenceAccount.LinkResult.DONE
                && PresenceAccount.linkResultOf(404) == PresenceAccount.LinkResult.UNKNOWN_CODE
                && PresenceAccount.linkResultOf(400) == PresenceAccount.LinkResult.BAD_CODE
                && PresenceAccount.linkResultOf(401) == PresenceAccount.LinkResult.LOGIN_FAILED
                && PresenceAccount.linkResultOf(500) == PresenceAccount.LinkResult.FAILED);
        check("★ 一般玩家 1 MB、成員 20 MB", CapeRules.limitFor(false) == 1024 * 1024
                && CapeRules.limitFor(true) == 20 * 1024 * 1024);
        byte[] pngHead = new byte[64];
        System.arraycopy(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, 0, pngHead, 0, 8);
        System.arraycopy("IHDR".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, pngHead, 12, 4);
        pngHead[19] = (byte) 176;
        pngHead[23] = (byte) 136;
        int[] measured = CapeRules.size(pngHead);
        check("★ 量得出 PNG 的尺寸（上傳完要說給玩家聽）", measured != null && measured[0] == 176 && measured[1] == 136);
        byte[] gifHead = new byte[32];
        System.arraycopy("GIF89a".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, gifHead, 0, 6);
        gifHead[6] = 88;
        gifHead[8] = 68;
        measured = CapeRules.size(gifHead);
        check("量得出 GIF 的尺寸", measured != null && measured[0] == 88 && measured[1] == 68);
        check("不是圖的量不出來", CapeRules.size(new byte[40]) == null && CapeRules.size(null) == null);
        JsonObject hello = JsonParser.parseString(
                Presence.helloBody(true, true, java.util.List.of("developers", "sponsors"))).getAsJsonObject();
        check("★ 報到只有三欄：披風開關與徽章的選擇（實際 " + hello.keySet() + "）",
                hello.keySet().equals(Set.of("cape", "rainbow", "show"))
                        && hello.get("rainbow").getAsBoolean()
                        && hello.getAsJsonArray("show").size() == 2
                        && "developers".equals(hello.getAsJsonArray("show").get(0).getAsString()));
        check("不在名單上的人沒有身分可挑：不送 show",
                JsonParser.parseString(Presence.helloBody(true, false, null)).getAsJsonObject()
                        .keySet().equals(Set.of("cape", "rainbow")));
        String mine = Presence.idOf(ME);
        String theirs = Presence.idOf(OTHER);
        Presence.load(Presence.parse("{\"t\":1,\"users\":[\"" + mine + "\",\"" + theirs
                + "\"],\"capes\":{},\"hidden\":[],\"rainbow\":[\"" + theirs + "\",\"ffffffffffffffffffffffff\"],"
                + "\"show\":{\"" + theirs + "\":\"developers,sponsors\",\"" + mine + "\":\"Dev!,contributors\"}}"));
        JsonObject yes = new JsonObject();
        yes.addProperty("token", "x");
        yes.addProperty("member", true);
        JsonObject text = new JsonObject();
        text.addProperty("token", "x");
        text.addProperty("member", "true");
        JsonObject old = new JsonObject();
        old.addProperty("token", "x");
        check("★ 登入的回覆有 member:true 才算有待遇；舊的登記站沒這一欄、寫成字串、不是 JSON 都當沒有",
                Presence.memberOf(yes.toString()) && !Presence.memberOf(old.toString())
                        && !Presence.memberOf(text.toString()) && !Presence.memberOf("oops")
                        && !Presence.memberOf(null));
        Presence.BadgeChoice picked = Presence.badgeChoice(OTHER);
        check("★ 名單上讀得到別人開了彩色、挑了哪幾片", picked.rainbow()
                && java.util.List.of("developers", "sponsors").equals(picked.shown()));
        check("怪的身分代號不收", java.util.List.of("contributors").equals(Presence.badgeChoice(ME).shown())
                && !Presence.badgeChoice(ME).rainbow());
        check("不在名單上的人沒有選擇（名單外的代碼開彩色不算）",
                Presence.badgeChoice(GHOST) == Presence.BadgeChoice.NONE
                        && Presence.badgeChoice(null) == Presence.BadgeChoice.NONE);
        Presence.load(Presence.parse("{\"t\":1,\"users\":[\"" + theirs + "\"],\"capes\":{},\"hidden\":[]}"));
        check("舊格式的名單（沒有那兩欄）照樣讀得進來", Presence.installed(OTHER)
                && Presence.badgeChoice(OTHER) == Presence.BadgeChoice.NONE);
        Presence.load(null);
        check("報到的披風開關照送", hello.get("cape").getAsBoolean()
                        && !JsonParser.parseString(Presence.helloBody(false, false, null)).getAsJsonObject()
                                .get("cape").getAsBoolean());

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

        check("200 是成功", PresenceAccount.resultOf(200, "{\"ok\":true}") == PresenceAccount.Result.DONE);
        check("登記站說的每一種理由都認得",
                PresenceAccount.resultOf(400, "{\"why\":\"bad size\"}") == PresenceAccount.Result.BAD_SIZE
                        && PresenceAccount.resultOf(413, "{\"why\":\"too large\"}") == PresenceAccount.Result.TOO_LARGE
                        && PresenceAccount.resultOf(403, "{\"why\":\"not listed\"}") == PresenceAccount.Result.NOT_LISTED
                        && PresenceAccount.resultOf(403, "{\"why\":\"banned\"}") == PresenceAccount.Result.BANNED
                        && PresenceAccount.resultOf(403, "{\"why\":\"gif members only\"}")
                                == PresenceAccount.Result.GIF_MEMBERS_ONLY
                        && PresenceAccount.resultOf(403, "{\"why\":\"uploads off\"}") == PresenceAccount.Result.OFF
                        && PresenceAccount.resultOf(429, "{\"why\":\"slow down\"}") == PresenceAccount.Result.SLOW_DOWN
                        && PresenceAccount.resultOf(400, "{\"why\":\"too many frames\"}")
                                == PresenceAccount.Result.TOO_MANY_FRAMES);
        check("不認得的、不是 JSON 的都是「沒有成功」",
                PresenceAccount.resultOf(502, "<html>Bad Gateway</html>") == PresenceAccount.Result.FAILED
                        && PresenceAccount.resultOf(400, "{\"why\":\"???\"}") == PresenceAccount.Result.FAILED);

        String link = Presence.BASE + "/capes#t=web.abc";
        check("登記站回的登入網址照用", link.equals(
                PresenceAccount.signedInLink(200, "{\"ok\":true,\"url\":\"" + link + "\"}")));
        check("★ 指到別的地方的網址不開", PresenceAccount.signedInLink(200,
                "{\"url\":\"https://example.com/capes#t=web.abc\"}") == null
                && PresenceAccount.signedInLink(200, "{\"url\":\"" + Presence.BASE + ".evil.test/capes#t=x\"}")
                        == null);
        check("沒有通行證（401）、不是 JSON：沒有登入網址", PresenceAccount.signedInLink(401, "{}") == null
                && PresenceAccount.signedInLink(200, "<html>") == null);
        check("★ F6 的上傳打開帳號頁，通行證照帶", (Presence.BASE + "/account#t=web.abc").equals(
                PresenceAccount.onPage(link, "/account")) && link.equals(PresenceAccount.onPage(link, "/capes")));
        check("沒有登入網址就沒有帳號頁的登入網址", PresenceAccount.onPage(null, "/account") == null);
        check("網站是 wynnchayuan.com", "https://wynnchayuan.com".equals(Presence.BASE));

        byte[] gif = Files.readAllBytes(Path.of("src/test/resources/cape/template.gif"));
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
        check("★ PNG：範本的七種尺寸都可以，最大 1056×816（實際 " + CapeRules.SIZES.length + " 種）",
                CapeRules.SIZES.length == 7 && CapeRules.check(png(1056, 816)) == null
                        && Arrays.stream(CapeRules.SIZES)
                .allMatch(size -> CapeRules.check(png(size[0], size[1])) == null));
        check("★ PNG：別的尺寸不行", CapeRules.check(png(100, 100)) == CapeRules.Problem.BAD_SIZE
                && CapeRules.check(png(66, 51)) == CapeRules.Problem.BAD_SIZE
                && CapeRules.check(png(990, 765)) == CapeRules.Problem.BAD_SIZE
                && CapeRules.check(png(1408, 1088)) == CapeRules.Problem.BAD_SIZE);
        byte[] wide = gif.clone();
        wide[6] = 23;
        check("GIF：別的尺寸不行", CapeRules.check(wide) == CapeRules.Problem.BAD_SIZE);

        System.out.println(failures == 0 ? "登記站：全部通過" : "登記站：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String expected() throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(
                ("wynnchayuan:" + "a".repeat(32)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            out.append(String.format("%02x", hash[i]));
        }
        return out.toString();
    }

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
