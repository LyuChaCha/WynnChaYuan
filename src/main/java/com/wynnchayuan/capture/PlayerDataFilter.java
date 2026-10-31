package com.wynnchayuan.capture;

import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.regex.Pattern;

public final class PlayerDataFilter {

    private static final List<String> MARKERS = List.of(
            " shouts:",
            "has logged into server",
            "has left the game",
            " left the game",
            "'s friends (",
            "[Server: ",
            "Congratulations to",
            " has joined your party",
            " has invited you",
            " sent you a friend request",
            " from their crate!",
            " has thrown a",
            " Loot Bomb has expired",
            " Bomb has expired",
            "'s Totem of Tales",
            "\u2019s Totem of Tales",
            "'s Mob Totem",
            "’s Mob Totem",
            " is visiting this island",
            " into your island",
            " had their existence effaced",
            " is preparing to descend",
            " became another casualty of War",
            " was entirely corrupted",
            " was reduced to ashes",
            " was instantly evaporated",
            " ceased to be anything human",
            " was buried beneath earth",
            " made a fatal mistake",
            "Cleansing fire melted ",
            " fell victim to ",
            " was clobbered by ",
            " was bashed into paste by ",
            " was butchered by ",
            " was impaled by ",
            " was silenced by ",
            " was stomped by ",
            " was weaker than ",
            "took a shortcut to death's door",
            "There's only ashes where ",
            "-- oops, other way around",
            " failed to evade ",
            " was shot down by ",
            " was de-animated by ",
            " became swiss cheese",
            " to thank for their death",
            " got a new skull piercing",
            " is now combat level",
            "'s HQ",
            "’s HQ",
            "DM Owner",
            "DM Chiefs",
            " has placed a mob totem",
            " has placed a gathering totem",
            " mob totem. Get your own",
            " gathering totem. Get your own",
            " of your XP to ",
            " has just logged in",
            " has just logged out",
            " is now online",
            " is now offline",
            "Controlled by ",
            " has chosen the ",
            " gave you [+",
            " has given you ",
            " gave you +",
            "Party Finder: Hey ",
            "Name Contains: "
    );

    private static final Pattern RAID_DEATH = Pattern.compile(
            "(^|(?:\\{#}\\s*)+)\\S+ (passed away"
                    + "|was purified by"
                    + "|was drained of"
                    + "|lost their color to"
                    + "|was crushed b"
                    + "|had their existence effaced"
                    + "|has been overtaken"
                    + "|has been crystallized"
                    + "|was minced to bits by"
                    + "|began to glow and then faded"
                    + "|has reconnected"
                    + "|has died)"
                    + "|['’]s existence was redacted",
            Pattern.MULTILINE);

    private static final Pattern THANK_SOMEONE =
            Pattern.compile("(?m)^(?:\\{#}\\s*)+Thank ");

    private static final Pattern ACCOUNT_LEAD =
            Pattern.compile("(?m)^(?:\\{#}\\s*)*[A-Za-z]{2,}\\{~}\\s+[a-z]");

    private static final Pattern PARTY_ROW = Pattern.compile(
            "(?m)^\\s*-\\s*(\\[\\|+|\\{~}[A-Za-z_][A-Za-z0-9_]*\\s*$)");

    private static final Pattern TRADE_LINE =
            Pattern.compile("(?m)^[^\\n]*→[^\\n]*\\{[#~pu]\\d?}[^\\n]*$"
                    + "|(?m)^[^\\n]*\\{[#~pu]\\d?}[^\\n]*→[^\\n]*$");

    private static final Pattern UNWRAP =
            Pattern.compile("[ \\t]*\\n(?:[ \\t]*\\{#}[ \\t]*)*");

    private static final Pattern COORDS =
            Pattern.compile("\\[-?\\d+,\\s*-?\\d+,\\s*-?\\d+]");

    private static final Pattern PLAYER_SHOP =
            Pattern.compile("['’]s Shop(\\n|$)");

    private static final Pattern PARTY_OWNER =
            Pattern.compile("['’]s Party(\\n|$)|^Leader: ", Pattern.MULTILINE);

    private static final Pattern GUILD_TAG = Pattern.compile(
            "^\\s*-?\\s*[^\\[\\]\\n]*\\S[^\\[\\]\\n]*"
                    + "\\[(?!(?:Cave|Raid)])[A-Za-z0-9]{2,4}]\\s*$",
            Pattern.MULTILINE);

    public static boolean isPartyCard(java.util.List<String> templates) {
        if (templates == null) {
            return false;
        }
        for (String line : templates) {
            if (line != null && PARTY_WORLD.matcher(line.strip()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static final Pattern PARTY_WORLD =
            Pattern.compile("^World: [A-Z]{2}\\{~}$");

    private static final Pattern GUILD_HOLOGRAM =
            Pattern.compile("(?m)^\\s*<[^<>\\n]{1,40}>\\s*$");

    private static final Pattern CRAFTED_BY =
            Pattern.compile("(?m)^(?:\\{#\\})*\\s*(?:Crafted )?by \\S+\\s*$");

    private static final Pattern SIGNED_BY =
            Pattern.compile("(?m)^(?:\\{#\\})*\\s*by [A-Z]");

    private static final Pattern XP_SHARE_TARGET =
            Pattern.compile("Combat XP]\\s*\\n\\s*\\[");

    private static final Pattern CJK = Pattern.compile(
            "[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff]");

    private PlayerDataFilter() {}

    public static boolean carriesPlayerData(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String unwrapped = text.indexOf('\n') < 0 ? text : UNWRAP.matcher(text).replaceAll(" ");
        boolean wrapped = !unwrapped.equals(text);
        for (String marker : MARKERS) {
            if (text.contains(marker) || (wrapped && unwrapped.contains(marker))) {
                return true;
            }
        }
        if (COORDS.matcher(text).find() || PLAYER_SHOP.matcher(text).find()
                || PARTY_OWNER.matcher(text).find()
                || GUILD_TAG.matcher(text).find()
                || RAID_DEATH.matcher(text).find()
                || CRAFTED_BY.matcher(text).find()
                || SIGNED_BY.matcher(text).find()
                || ACCOUNT_LEAD.matcher(text).find()
                || XP_SHARE_TARGET.matcher(text).find()
                || GUILD_HOLOGRAM.matcher(text).find()
                || PARTY_ROW.matcher(text).find()
                || TRADE_LINE.matcher(text).find()
                || THANK_SOMEONE.matcher(text).find()
                || CJK.matcher(text).find()) {
            return true;
        }
        if (SelfNames.find(text) != null) {
            return true;
        }
        return mentionsOnlinePlayer(text);
    }

    private static final Pattern SLOT = Pattern.compile("\\{[#~pu]\\d?}");

    private static final Pattern BRACKETED = Pattern.compile("\\[[^\\]]*\\]?");

    private static final Pattern SPACES = Pattern.compile("\\s+");

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private static String firstRowName(String template) {
        int row = template.indexOf('\n');
        String first = row < 0 ? template : template.substring(0, row);
        return SLOT.matcher(first).replaceAll(" ").trim();
    }

    private static final int MIN_ONLINE_NAME = 4;

    private static boolean mentionsOnlinePlayer(String text) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getConnection() == null) {
                return false;
            }
            for (net.minecraft.client.multiplayer.PlayerInfo info
                    : mc.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().name();
                if (name != null && name.length() >= MIN_ONLINE_NAME
                        && text.contains(name)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            ErrorDebug.note("PlayerDataFilter#mentionsOnlinePlayer", null, t);
        }
        return false;
    }

    // 地城鑰匙名中間有 À 對齊字元，大寫限 ASCII
    public static boolean looksAccountNamed(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        String name = firstRowName(template);
        if (name.length() < MIN_PLATE_NAME) {
            return false;
        }
        char tail = name.charAt(name.length() - 1);
        if (tail == '.' || tail == '!' || tail == '?' || tail == ',' || tail == ':') {
            return false;
        }
        String[] words = SPACES.split(BRACKETED.matcher(name).replaceAll(" ").trim());
        if (words.length > MAX_PLATE_WORDS) {
            return false;
        }
        for (String word : words) {
            if (word.indexOf('_') >= 0) {
                return true;
            }
            for (int i = 1; i < word.length(); i++) {
                char c = word.charAt(i);
                char prev = word.charAt(i - 1);
                if (c >= 'A' && c <= 'Z' && prev >= 'a' && prev <= 'z') {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean mentionsOnlinePlayerLoose(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        java.util.List<String> names = onlineNames();
        if (names.isEmpty()) {
            return false;
        }
        java.util.regex.Matcher m = WORDS.matcher(template);
        while (m.find()) {
            String token = m.group();
            if (token.length() < MIN_NAME_MATCH) {
                continue;
            }
            for (String name : names) {
                if (name.length() < MIN_NAME_MATCH) {
                    continue;
                }
                if (name.regionMatches(true, 0, token, 0, Math.min(name.length(), token.length()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static java.util.List<String> onlineNames() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getConnection() == null) {
                return List.of();
            }
            java.util.List<String> out = new java.util.ArrayList<>();
            for (var entry : mc.getConnection().getListedOnlinePlayers()) {
                String name = entry.getProfile().name();
                if (name != null && !name.isBlank()) {
                    out.add(DIGITS.matcher(name).replaceAll(""));
                }
            }
            return out;
        } catch (Throwable t) {
            ErrorDebug.note("PlayerDataFilter#onlineNames", null, t);
            return List.of();
        }
    }

    private static final int MIN_NAME_MATCH = 4;

    private static final Pattern WORDS = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public static boolean looksPlayerNamed(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        String name = firstRowName(template);
        if (name.length() < MIN_PLATE_NAME) {
            return false;
        }
        String bare = BRACKETED.matcher(name).replaceAll(" ").trim();
        char last = name.charAt(name.length() - 1);
        boolean plateShaped = SPACES.split(bare).length <= MAX_PLATE_WORDS_HARD
                && last != '.' && last != '!' && last != '?';
        for (String word : SPACES.split(name)) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.indexOf('_') >= 0) {
                return true;
            }
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (plateShaped && Character.isDigit(c)) {
                    return true;
                }
                if (i > 0 && Character.isUpperCase(c)
                        && Character.isLowerCase(word.charAt(i - 1))) {
                    return true;
                }
            }
        }

        if (!Character.isLowerCase(name.charAt(0))) {
            return false;
        }
        char tail = name.charAt(name.length() - 1);
        if (tail == '.' || tail == '!' || tail == '?' || tail == ',' || tail == ':') {
            return false;
        }
        String[] words = SPACES.split(BRACKETED.matcher(name).replaceAll(" ").trim());
        boolean real = false;
        for (String word : words) {
            if (word.length() >= 2) {
                real = true;
                break;
            }
        }
        return real && words.length <= MAX_PLATE_WORDS;
    }

    private static final int MIN_PLATE_NAME = 3;

    private static final int MAX_PLATE_WORDS = 3;

    private static final int MAX_PLATE_WORDS_HARD = 6;

    private static String localPlayerName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null || mc.getUser() == null ? null : mc.getUser().getName();
        } catch (Throwable t) {
            ErrorDebug.note("PlayerDataFilter#localPlayerName", null, t);
            return null;
        }
    }
}
