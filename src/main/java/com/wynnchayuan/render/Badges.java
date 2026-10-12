package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.Credits;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 在開發者、贊助者、貢獻者的名字前面加一片葉子。
 *
 * <h2>為什麼是圖示，不是一行字</h2>
 * 先前是在名牌上方多畫一行「WynnChaYuan 開發者」。那一行字要跟著介面語言翻，
 * 而且 Wynntils、WynnMod 都已經把自己的標記放在<b>名字前面</b>——同一個位置放
 * 一個小圖示、用顏色分身分，掃一眼就認得出來，也不必翻（使用者 2026-10-11）。
 *
 * <p>顏色：開發者金、贊助者青藍、貢獻者綠，各自可以在 {@code credits.json} 登記
 * 自己的顏色；開發者另外可以登記成會變色的彩色。只是裝了模組、不在名單上的人是灰色。
 *
 * <h2>只有裝了這個模組的人看得到</h2>
 * 這是純客戶端的繪製，伺服器完全不知情。名單就是 {@code credits.json}，
 * 同步譯文時會一起更新，加人、改顏色都不必重新發版。
 *
 * <h2>「誰裝了模組」目前只認得自己</h2>
 * 模組不對外送任何資料，所以別人有沒有裝，這一端無從得知；灰色葉子只會出現在
 * 自己的名牌上。要讓它出現在別人身上，得有一個回報「我在線上」的服務——那是
 * 另一個決定，這裡先把位置留好（{@link #isInstalled}）。
 *
 * <h2>怎麼認出是誰</h2>
 * 名牌上的文字不一定等於帳號名稱：Wynncraft 會加上階級前綴與顏色，WynnMod 會把
 * 整行重建。所以先問實體本身的帳號名稱，問不到才拿名單裡的 ID 去名牌文字裡找，
 * 而且要求前後是詞的邊界——用「包含」的話，{@code Bob} 會誤中 {@code Bobby}。
 */
public final class Badges {

    /** 葉子在 {@code wynnchayuan:nametag} 字型裡的碼位。 */
    public static final String ICON = "";

    /** 只是裝了模組、不在名單上的人。 */
    public static final int INSTALLED = 0xA8A8A8;

    static final FontDescription FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID, "nametag"));

    /** 彩色模式轉一圈要多久、分成幾格。 */
    private static final long RAINBOW_MS = 3000;
    private static final int RAINBOW_STEPS = 60;

    /**
     * 一個人的一種身分。
     *
     * @param id      分區的代號（{@code developers}、{@code sponsors}、{@code contributors}）
     * @param name    畫面上的稱呼，照介面語言
     * @param color   葉子的顏色：登記了就用登記的，沒有就用分區的
     * @param rainbow 登記成彩色
     * @param hidden  登記成不展示（多重身分的人挑著放）
     */
    public record Role(String id, String name, int color, boolean rainbow, boolean hidden) {}

    /**
     * 名單建好的查表。整份一起換，讀的人拿到的永遠是完整的一份。
     *
     * <p>不用鎖：名牌每幀都在畫（繪製執行緒），名單是同步的背景執行緒換的。
     * 兩邊各拿一把鎖互等的話，畫面會整個卡住。
     */
    private record Table(Map<String, List<Role>> byName, Map<String, Pattern> patterns) {}

    private static volatile Table table;

    private Badges() {}

    /** 名單換了（同步抓到新的 {@code credits.json}）。 */
    public static void reload() {
        table = null;
    }

    /** 名單第一次用到時才建，之後重複使用。 */
    private static Table table() {
        Table now = table;
        if (now == null) {
            now = build(Credits.sections());
            table = now;
        }
        return now;
    }

    /** 照這份名單建表。平常是 {@code credits.json}，測試會塞自己的。 */
    static void load(List<Credits.Section> sections) {
        table = build(sections);
    }

    private static Table build(List<Credits.Section> sections) {
        Map<String, List<Role>> roles = new LinkedHashMap<>();
        Map<String, Pattern> pats = new LinkedHashMap<>();
        for (Credits.Section section : sections) {
            for (Credits.Member member : section.members()) {
                if (!member.hasHead()) {
                    continue;              // 沒有遊戲 ID 的（資料來源那類）不用標
                }
                String key = member.mc().toLowerCase(Locale.ROOT);
                roles.computeIfAbsent(key, k -> new ArrayList<>()).add(new Role(
                        section.id(), section.role(),
                        member.colour() != 0 ? member.colour() : section.color(),
                        member.rainbow(), member.hidden()));
                pats.computeIfAbsent(key, k -> Pattern.compile(
                        "(?i)\\b" + Pattern.quote(member.mc()) + "\\b"));
            }
        }
        return new Table(roles, pats);
    }

    /** 這個帳號在名單上的所有身分，連登記成不展示的；不在名單上就是空的。 */
    public static List<Role> registered(String account) {
        List<Role> roles = account == null
                ? null : table().byName().get(account.toLowerCase(Locale.ROOT));
        return roles == null ? List.of() : roles;
    }

    /**
     * 這個帳號要畫哪幾片葉子。
     *
     * <p>別人照名單上登記的；<b>自己</b>再套一層本機的選擇（設定畫面的「我的徽章」），
     * 這樣改了馬上看得到。本機的選擇不會傳出去，別人看到的仍然是登記的那一份。
     *
     * <p>名單上的人<b>至少有一片</b>，不會是空的。
     *
     * @param account 帳號名稱
     * @param self    自己的帳號名稱；不知道就傳 {@code null}
     */
    public static List<Role> shown(String account, String self, CollectorConfig config) {
        boolean mine = account != null && account.equalsIgnoreCase(self);
        List<Role> all = new ArrayList<>();
        List<Role> out = new ArrayList<>();
        for (Role role : registered(account)) {
            boolean rainbow = mine ? config.badgeRainbow(role.rainbow()) && rainbowAllowed(role)
                    : role.rainbow();
            Role resolved = new Role(role.id(), role.name(),
                    mine ? ownColour(config, role) : role.color(), rainbow, false);
            all.add(resolved);
            if (mine ? config.badgeRoleShown(role.id(), !role.hidden()) : !role.hidden()) {
                out.add(resolved);
            }
        }
        // 最少留一片：徽章是「這個人是誰」，不能整個關掉（使用者 2026-10-12）。
        // 多重身分的人可以挑要放哪幾片，全關掉的話留名單上的第一個。
        if (out.isEmpty() && !all.isEmpty()) {
            out.add(all.get(0));
        }
        return out;
    }

    /** 自己替這個身分選的顏色；沒選（或設定檔被改壞）就用名單上登記的。 */
    private static int ownColour(CollectorConfig config, Role role) {
        String hex = config.badgeColour(role.id());
        if (hex != null) {
            try {
                return Integer.parseInt(hex.replace("#", "").strip(), 16) & 0xFFFFFF;
            } catch (NumberFormatException e) {
                // 落到下面
            }
        }
        return role.color();
    }

    /** 彩色模式是開發者的；其他身分就算設定檔被手改也不給。 */
    public static boolean rainbowAllowed(Role role) {
        return "developers".equals(role.id());
    }

    /**
     * 這個帳號是不是裝了模組。
     *
     * <p>現在只認得自己。見類別說明。
     */
    static boolean isInstalled(String account, String self) {
        return account != null && account.equalsIgnoreCase(self);
    }

    /**
     * 把葉子接到名牌前面。
     *
     * @param nameTag  名牌上的文字，可能是 {@code null}
     * @param entityId 這個名牌屬於哪個實體；{@code < 0} 表示不知道
     * @return 加了葉子的名牌；不必加就回傳 {@code null}
     */
    public static Component decorate(Component nameTag, int entityId) {
        return decorate(nameTag, accountName(entityId), selfName(), WynnChaYuan.config(),
                System.currentTimeMillis());
    }

    /** 同上，但所有外部狀態都從參數來——測試用。 */
    static Component decorate(Component nameTag, String account, String self,
                              CollectorConfig config, long now) {
        if (nameTag == null || startsWithIcon(nameTag)) {
            return null;
        }
        String who = account != null ? account : accountIn(nameTag.getString());
        List<Role> roles = shown(who, self, config);
        MutableComponent icons = Component.empty();
        if (!roles.isEmpty()) {
            for (Role role : roles) {
                icons.append(icon(role.rainbow() ? rainbowAt(now) : role.color()));
            }
        } else if (isInstalled(who, self)) {
            icons.append(icon(INSTALLED));
        } else {
            return null;
        }
        return Component.empty().append(icons).append(" ").append(nameTag);
    }

    /** 一片葉子。 */
    static Component icon(int rgb) {
        return Component.literal(ICON).withStyle(
                Style.EMPTY.withFont(FONT).withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
    }

    /**
     * 名牌是不是已經加過葉子了。
     *
     * <p>事件一幀可能發不只一次；沒有這一關，葉子會越接越多。看的是<b>字型</b>
     * 而不是字元——私用區的碼位 Wynncraft 自己也在用。
     */
    static boolean startsWithIcon(Component nameTag) {
        Optional<Boolean> first = nameTag.visit((style, text) -> text.isEmpty()
                ? Optional.empty()
                : Optional.of(text.startsWith(ICON) && FONT.equals(style.getFont())), Style.EMPTY);
        return first.orElse(false);
    }

    /**
     * 彩色模式這一刻的顏色。
     *
     * <p>切成固定的幾格，不是每幀算一個新顏色：WynnMod 拿名牌的內容當快取的鍵，
     * 顏色連續變化會讓它每幀都算一份新的。
     */
    static int rainbowAt(long now) {
        long step = Math.floorMod(now, RAINBOW_MS) * RAINBOW_STEPS / RAINBOW_MS;
        return hsv((float) step / RAINBOW_STEPS, 0.7f, 1.0f);
    }

    /** 名牌文字裡找得到名單上的哪個 ID；找不到就回傳 {@code null}。 */
    private static String accountIn(String plain) {
        for (Map.Entry<String, Pattern> e : table().patterns().entrySet()) {
            if (e.getValue().matcher(plain).find()) {
                return e.getKey();
            }
        }
        return null;
    }

    /** 自己的帳號名稱；還沒登入（或在測試裡）就回傳 {@code null}。 */
    public static String selfName() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc == null ? null : mc.getUser().getName();
        } catch (Throwable t) {
            return null;
        }
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
