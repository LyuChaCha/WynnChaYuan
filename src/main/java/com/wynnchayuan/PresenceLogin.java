package com.wynnchayuan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class PresenceLogin {

    private PresenceLogin() {}

    static final String LOGIN_PREFIX = "wynnchayuan.com login\n";

    static byte[] loginMessage(String challenge, String uuid) {
        return (LOGIN_PREFIX + challenge + "\n" + uuid).getBytes(StandardCharsets.UTF_8);
    }

    static byte[] signLogin(PrivateKey key, String challenge, String uuid)
            throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(loginMessage(challenge, uuid));
        return signer.sign();
    }

    static String loginBody(String uuid, long expiresAt, byte[] publicKey, byte[] keySignature,
                            String challenge, byte[] signature,
                            String texturesValue, String texturesSignature) {
        Base64.Encoder b64 = Base64.getEncoder();
        JsonObject o = new JsonObject();
        o.addProperty("uuid", uuid);
        o.addProperty("expiresAt", expiresAt);
        o.addProperty("publicKey", b64.encodeToString(publicKey));
        o.addProperty("keySignature", b64.encodeToString(keySignature));
        o.addProperty("challenge", challenge);
        o.addProperty("signature", b64.encodeToString(signature));
        JsonObject textures = new JsonObject();
        textures.addProperty("value", texturesValue);
        textures.addProperty("signature", texturesSignature);
        o.add("textures", textures);
        return o.toString();
    }

    // Mojang 擋 Cloudflare Worker 的請求（403），改用玩家金鑰簽名
    static boolean login() throws Exception {
        if (Presence.token != null) {
            return true;
        }
        String problem;
        try {
            problem = proveAccount();
        } catch (Exception e) {
            ErrorDebug.note("presence.login", null, e);
            problem = e.toString();
        }
        if (problem == null) {
            return true;
        }
        Presence.loginFailedAt = System.currentTimeMillis();
        if (!problem.equals(lastProblem)) {
            lastProblem = problem;
            ModLog.info("登記站登入沒有成功：" + problem);
        }
        return false;
    }

    private static volatile String lastProblem;

    private static final long KEY_WAIT_SECONDS = 20;

    private static String proveAccount() throws Exception {
        Minecraft mc = Minecraft.getInstance();
        UUID id = mc.getUser().getProfileId();
        if (id == null) {
            return "沒有 UUID";
        }
        Optional<ProfileKeyPair> pair = mc.getProfileKeyPairManager().prepareKeyPair()
                        .get(KEY_WAIT_SECONDS, TimeUnit.SECONDS);
        if (pair == null || pair.isEmpty()) {
            return "拿不到 Mojang 發的玩家金鑰（離線帳號，或是 Mojang 暫時連不上）";
        }
        ProfilePublicKey.Data certificate = pair.get().publicKey().data();
        Property textures = signedTextures(mc, id);
        if (textures == null) {
            return "拿不到 Mojang 簽過的個人檔案";
        }
        HttpResponse<byte[]> asked = Presence.send(HttpRequest.newBuilder(URI.create(Presence.API + "/challenge"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)), false);
        if (asked.statusCode() != 200) {
            return "登記站沒有出題（" + asked.statusCode() + "）";
        }
        String challenge = JsonParser.parseString(new String(asked.body(), StandardCharsets.UTF_8))
                .getAsJsonObject().get("challenge").getAsString();
        String uuid = id.toString().replace("-", "");
        String body = loginBody(uuid, certificate.expiresAt().toEpochMilli(),
                certificate.key().getEncoded(), certificate.keySignature(), challenge,
                signLogin(pair.get().privateKey(), challenge, uuid),
                textures.value(), textures.signature());
        HttpResponse<byte[]> response = Presence.send(HttpRequest.newBuilder(URI.create(Presence.API + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)), false);
        String answer = new String(response.body(), StandardCharsets.UTF_8);
        if (response.statusCode() != 200) {
            return "登記站回 " + response.statusCode() + " "
                    + (answer.length() > 120 ? answer.substring(0, 120) : answer);
        }
        Presence.token = JsonParser.parseString(answer).getAsJsonObject().get("token").getAsString();
        Presence.member = Presence.memberOf(answer);
        lastProblem = null;
        return null;
    }

    private static Property signedTextures(Minecraft mc, UUID id) {
        Property found = signedTextures(mc.getGameProfile());
        if (found == null) {
            ProfileResult fetched = mc.services().sessionService().fetchProfile(id, true);
            found = fetched == null ? null : signedTextures(fetched.profile());
        }
        return found;
    }

    private static Property signedTextures(GameProfile profile) {
        if (profile == null || profile.properties() == null) {
            return null;
        }
        for (Property property : profile.properties().get("textures")) {
            if (property.hasSignature() && property.value() != null) {
                return property;
            }
        }
        return null;
    }
}
