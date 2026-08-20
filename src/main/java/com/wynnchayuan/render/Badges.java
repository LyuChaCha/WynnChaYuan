package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.Credits;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 在開發者、贊助者、貢獻者的名牌上方加一行標記。
 *
 * <h2>只有裝了這個模組的人看得到</h2>
 * 這是純客戶端的繪製，伺服器完全不知情，被標記的人自己也看不到（除非他也裝了）。
 * 所以它的意義是「模組使用者之間互相認得出來」，不是對全服公告。
 *
 * <p>名單就是 {@code credits.json}——同一份資料同時餵給關於頁與這裡。
 * 加人只要送一個 PR，不必改程式，也不必重新發版（譯文同步時會一起更新）。
 *
 * <h2>怎麼認出是誰</h2>
 * 名牌上的文字不一定等於帳號名稱：Wynncraft 會加上階級前綴與顏色，
 * Wynntils 也可能再改一手。所以是拿名單裡的 ID 去<b>名牌文字裡找</b>，
 * 而且要求前後是詞的邊界——用「包含」的話，{@code Bob} 會誤中 {@code Bobby}。
 */
public final class Badges {

    /**
     * 標記要比名牌高多少（世界座標）。
     *
     * <p>抓兩行的高度而不是一行：Wynntils 自己也會在名牌上下加東西
     * （donator 標記、公會標籤），只抬一行會跟它疊在同一個位置，
     * 後畫的那個會把先畫的蓋掉。
     */
    public static final double HEIGHT = 0.62;

    private static final String PREFIX = "WynnChaYuan ";

    /** 一個人的身分。可能有好幾個。 */
    public record Role(String name, int color) {}

    private static Map<String, List<Role>> byName;
    private static Map<String, Pattern> patterns;

    private Badges() {}

    /** 名單第一次用到時才建，之後重複使用。 */
    private static void ensureLoaded() {
        if (byName != null) {
            return;
        }
        Map<String, List<Role>> roles = new LinkedHashMap<>();
        Map<String, Pattern> pats = new LinkedHashMap<>();
        for (Credits.Section section : Credits.sections()) {
            for (Credits.Member member : section.members()) {
                if (!member.hasHead()) {
                    continue;              // 沒有遊戲 ID 的（資料來源那類）不用標
                }
                String key = member.mc().toLowerCase(Locale.ROOT);
                roles.computeIfAbsent(key, k -> new ArrayList<>())
                     .add(new Role(section.role(), section.color()));
                pats.computeIfAbsent(key, k -> Pattern.compile(
                        "(?i)\\b" + Pattern.quote(member.mc()) + "\\b"));
            }
        }
        byName = roles;
        patterns = pats;
    }

    /**
     * 這個名牌該不該加標記，要加什麼。
     *
     * @param nameTag  名牌上的文字，可能是 {@code null}
     * @param entityId 這個名牌屬於哪個實體；{@code < 0} 表示不知道
     * @return 要畫的那一行；不是名單上的人就回傳 {@code null}
     */
    public static Component forNameTag(Component nameTag, int entityId) {
        if (!WynnChaYuan.config().showBadges()) {
            return null;
        }
        ensureLoaded();
        if (byName.isEmpty()) {
            return null;
        }
        // 先問實體本身。名牌上的文字不可靠——Wynntils 會整個換掉它
        // （階級前綴、donator 標記、公會標籤都是它加的），甚至可能清空。
        // 帳號名稱則是這個玩家的事實，誰都改不了。
        List<Role> byProfile = rolesOf(accountName(entityId));
        if (byProfile != null) {
            return build(byProfile);
        }
        if (nameTag == null) {
            return null;
        }
        // 退路：名牌文字裡找得到名單上的 ID
        String plain = nameTag.getString();
        for (Map.Entry<String, Pattern> e : patterns.entrySet()) {
            if (e.getValue().matcher(plain).find()) {
                return build(byName.get(e.getKey()));
            }
        }
        return null;
    }

    private static List<Role> rolesOf(String accountName) {
        return accountName == null
                ? null : byName.get(accountName.toLowerCase(Locale.ROOT));
    }

    /** 這個實體是哪個玩家的帳號名稱；不是玩家就回傳 {@code null}。 */
    private static String accountName(int entityId) {
        if (entityId < 0) {
            return null;
        }
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.level == null) {
                return null;
            }
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(entityId);
            return entity instanceof net.minecraft.world.entity.player.Player player
                    ? player.getGameProfile().name() : null;
        } catch (Throwable t) {
            return null;   // 這只是裝飾，查不到就退回看名牌文字
        }
    }

    /**
     * 組出標記文字。
     *
     * <p>一個身分就用那個身分的顏色；多個身分則<b>逐字漸層</b>掃過所有顏色。
     * 三種身分都有的人給整段彩虹——那是刻意的稀有感，不是隨便挑的效果。
     */
    private static Component build(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        if (WynnChaYuan.config().badgeStyle() == CollectorConfig.BadgeStyle.PRIMARY
                || roles.size() == 1) {
            Role first = roles.get(0);
            return Component.literal(PREFIX + first.name())
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(first.color())));
        }
        String text = PREFIX + joinNames(roles);
        return roles.size() >= 3 ? rainbow(text) : gradient(text, roles);
    }

    private static String joinNames(List<Role> roles) {
        StringBuilder sb = new StringBuilder();
        for (Role r : roles) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(r.name());
        }
        return sb.toString();
    }

    /** 逐字在各身分的顏色之間內插。 */
    private static Component gradient(String text, List<Role> roles) {
        MutableComponent out = Component.empty();
        int n = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            double t = (double) i / n * (roles.size() - 1);
            int index = Math.min((int) t, roles.size() - 2);
            int color = lerp(roles.get(index).color(),
                             roles.get(index + 1).color(), t - index);
            out.append(charWith(text.charAt(i), color));
        }
        return out;
    }

    /** 整段掃過色相環。三種身分都有的人專用。 */
    private static Component rainbow(String text) {
        MutableComponent out = Component.empty();
        int n = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            out.append(charWith(text.charAt(i), hsv((float) i / n, 0.75f, 1.0f)));
        }
        return out;
    }

    private static Component charWith(char c, int color) {
        return Component.literal(String.valueOf(c))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    private static int lerp(int from, int to, double t) {
        int r = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** 自己算而不是用 {@code Mth.hsvToRgb}，因為那支在不同版本間搬過家。 */
    private static int hsv(float h, float s, float v) {
        int sector = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        int p = (int) (v * (1 - s) * 255);
        int q = (int) (v * (1 - f * s) * 255);
        int t = (int) (v * (1 - (1 - f) * s) * 255);
        int full = (int) (v * 255);
        return switch (sector) {
            case 0 -> (full << 16) | (t << 8) | p;
            case 1 -> (q << 16) | (full << 8) | p;
            case 2 -> (p << 16) | (full << 8) | t;
            case 3 -> (p << 16) | (q << 8) | full;
            case 4 -> (t << 16) | (p << 8) | full;
            default -> (full << 16) | (p << 8) | q;
        };
    }
}
