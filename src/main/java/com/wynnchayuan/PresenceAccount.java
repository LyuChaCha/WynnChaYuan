package com.wynnchayuan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.translate.ErrorDebug;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class PresenceAccount {

    private PresenceAccount() {}

    public enum LinkResult {
        DONE, BAD_CODE, UNKNOWN_CODE, LOGIN_FAILED, FAILED, OFF
    }

    private static final java.util.regex.Pattern LINK_CODE =
            java.util.regex.Pattern.compile("[A-HJ-NP-Z2-9]{8}");

    static String linkCode(String typed) {
        if (typed == null) {
            return null;
        }
        String code = typed.strip().toUpperCase(java.util.Locale.ROOT);
        return LINK_CODE.matcher(code).matches() ? code : null;
    }

    static LinkResult linkResultOf(int status) {
        return switch (status) {
            case 200 -> LinkResult.DONE;
            case 400 -> LinkResult.BAD_CODE;
            case 404 -> LinkResult.UNKNOWN_CODE;
            case 401 -> LinkResult.LOGIN_FAILED;
            default -> LinkResult.FAILED;
        };
    }

    public static void link(String typed, Consumer<LinkResult> done) {
        if (!NetworkGate.registry()) {
            done.accept(LinkResult.OFF);
            return;
        }
        String code = linkCode(typed);
        if (code == null) {
            done.accept(LinkResult.BAD_CODE);
            return;
        }
        Presence.WORKER.execute(() -> {
            LinkResult result;
            try {
                if (!PresenceLogin.login()) {
                    result = LinkResult.LOGIN_FAILED;
                } else {
                    HttpResponse<byte[]> response = Presence.send(HttpRequest.newBuilder(
                            URI.create(Presence.API + "/link"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(Presence.linkBody(code),
                                    StandardCharsets.UTF_8)), true);
                    result = linkResultOf(response.statusCode());
                }
            } catch (Throwable t) {
                com.wynnchayuan.translate.ErrorDebug.note("presence.link", null, t);
                result = LinkResult.FAILED;
            }
            done.accept(result);
        });
    }

    public enum Result {
        DONE, TOO_LARGE, BAD_SIZE, BAD_FORMAT, TOO_MANY_FRAMES, NOT_LISTED, GIF_MEMBERS_ONLY,
        BANNED, OFF,
        SLOW_DOWN, LOGIN_FAILED, FAILED, PRIVACY
    }

    static Result resultOf(int status, String body) {
        if (status == 200) {
            return Result.DONE;
        }
        String why = "";
        try {
            why = JsonParser.parseString(body).getAsJsonObject().get("why").getAsString();
        } catch (Exception e) {
            ErrorDebug.note("PresenceAccount#resultOf", null, e);
        }
        return switch (why) {
            case "too large" -> Result.TOO_LARGE;
            case "bad size" -> Result.BAD_SIZE;
            case "bad format" -> Result.BAD_FORMAT;
            case "too many frames" -> Result.TOO_MANY_FRAMES;
            case "not listed" -> Result.NOT_LISTED;
            case "gif members only" -> Result.GIF_MEMBERS_ONLY;
            case "banned" -> Result.BANNED;
            case "uploads off" -> Result.OFF;
            case "slow down" -> Result.SLOW_DOWN;
            case "login first" -> Result.LOGIN_FAILED;
            default -> Result.FAILED;
        };
    }

    public static void remove(Consumer<Result> done) {
        if (!NetworkGate.registry()) {
            done.accept(Result.PRIVACY);
            return;
        }
        Presence.WORKER.execute(() -> done.accept(change(HttpRequest.newBuilder(
                URI.create(Presence.API + "/cape")).DELETE())));
    }

    private static Result change(HttpRequest.Builder request) {
        try {
            if (!PresenceLogin.login()) {
                return Result.LOGIN_FAILED;
            }
            HttpResponse<byte[]> response = Presence.send(request, true);
            Result result = resultOf(response.statusCode(),
                    new String(response.body(), StandardCharsets.UTF_8));
            if (result == Result.DONE) {
                Presence.refresh();
            }
            return result;
        } catch (Throwable t) {
            com.wynnchayuan.translate.ErrorDebug.note("presence.cape", null, t);
            return Result.FAILED;
        }
    }

    public static void gallery(boolean signIn, Consumer<String> open) {
        site("/capes", signIn, open);
    }

    public static void site(String page, boolean signIn, Consumer<String> open) {
        Presence.WORKER.execute(() -> {
            String url = Presence.BASE + page;
            if (signIn && NetworkGate.registry()) {
                try {
                    if (PresenceLogin.login()) {
                        HttpResponse<byte[]> response = Presence.send(HttpRequest.newBuilder(
                                URI.create(Presence.API + "/web-link"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString("{}")), true);
                        String link = onPage(signedInLink(response.statusCode(),
                                new String(response.body(), StandardCharsets.UTF_8)), page);
                        if (link != null) {
                            url = link;
                        }
                    }
                } catch (Throwable t) {
                    com.wynnchayuan.translate.ErrorDebug.note("presence.gallery", null, t);
                }
            }
            open.accept(url);
        });
    }

    static String onPage(String link, String page) {
        String gallery = Presence.BASE + "/capes";
        if (link == null || !link.startsWith(gallery + "#")) {
            return null;
        }
        return Presence.BASE + page + link.substring(gallery.length());
    }

    static String signedInLink(int status, String body) {
        if (status != 200) {
            return null;
        }
        try {
            String url = JsonParser.parseString(body).getAsJsonObject().get("url").getAsString();
            return url.startsWith(Presence.BASE + "/capes#t=") ? url : null;
        } catch (Exception e) {
            ErrorDebug.note("PresenceAccount#signedInLink", null, e);
            return null;
        }
    }

    public static byte[] capeBytes(String id, long version) {
        if (!Presence.isId(id) || !NetworkGate.registry()) {
            return null;
        }
        try {
            HttpResponse<byte[]> response = Presence.send(HttpRequest.newBuilder(
                    URI.create(Presence.API + "/cape/" + id + "?v=" + version)).GET(), false);
            return response.statusCode() == 200 && response.body().length <= Presence.MAX_CAPE_BYTES
                    ? response.body() : null;
        } catch (Exception e) {
            ErrorDebug.note("PresenceAccount#capeBytes", null, e);
            return null;
        }
    }

    public enum DeleteResult {
        DONE, NOT_FOUND, LOGIN_FAILED, FAILED
    }

    static DeleteResult deleteResultOf(int status, String body) {
        if (status == 404) {
            return DeleteResult.NOT_FOUND;
        }
        if (status == 401) {
            return DeleteResult.LOGIN_FAILED;
        }
        if (status != 200) {
            return DeleteResult.FAILED;
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            return root.has("ok") && root.get("ok").isJsonPrimitive()
                    && root.get("ok").getAsJsonPrimitive().isBoolean()
                    && root.get("ok").getAsBoolean() ? DeleteResult.DONE : DeleteResult.FAILED;
        } catch (Exception e) {
            ErrorDebug.note("PresenceAccount#deleteResultOf", null, e);
            return DeleteResult.FAILED;
        }
    }

    public static void deleteMyData(Consumer<DeleteResult> done) {
        Presence.WORKER.execute(() -> {
            DeleteResult result;
            try {
                boolean hadToken = Presence.token != null;
                result = askToDelete();
                if (result == DeleteResult.LOGIN_FAILED && hadToken) {
                    result = askToDelete();
                }
            } catch (Throwable t) {
                com.wynnchayuan.translate.ErrorDebug.note("presence.delete", null, t);
                result = DeleteResult.FAILED;
            }
            if (result == DeleteResult.DONE) {
                forget();
            }
            done.accept(result);
        });
    }

    private static DeleteResult askToDelete() throws Exception {
        if (!PresenceLogin.login()) {
            return DeleteResult.LOGIN_FAILED;
        }
        HttpResponse<byte[]> response = Presence.send(HttpRequest.newBuilder(URI.create(Presence.API + "/me/delete"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)), true);
        return deleteResultOf(response.statusCode(),
                new String(response.body(), StandardCharsets.UTF_8));
    }

    public static void forget() {
        Presence.token = null;
        Presence.member = false;
        Presence.lastHello = 0;
        Presence.lastSaid = null;
        Presence.lastList = 0;
        Presence.roster = Presence.Roster.EMPTY;
    }
}
