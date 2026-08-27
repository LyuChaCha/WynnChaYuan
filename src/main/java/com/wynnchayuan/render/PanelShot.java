package com.wynnchayuan.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 把翻譯面板單獨拍成一張圖。
 *
 * <h2>做這個是為了校稿</h2>
 * 譯文在遊戲裡長什麼樣，跟它在 JSON 裡長什麼樣是兩回事：欄位有沒有對齊、
 * 顏色有沒有掉、有沒有被擠到換行、標點是不是跑到行首。這些全都只有畫出來
 * 才看得見，而翻譯的人多半不會為了確認一行字專程進遊戲跑到那個 NPC 面前。
 *
 * <p>所以拍下來。一張圖貼進討論串，比十句「那個地方好像怪怪的」有用。
 *
 * <h2>為什麼是裁切而不是重畫</h2>
 * 重畫一次面板到離屏緩衝區聽起來比較乾淨，實際上會拍到<b>另一次</b>繪製的
 * 結果——字型圖集、動態顏色、淡入淡出的透明度都可能跟畫面上那一幀不同。
 * 於是圖裡的東西跟使用者當下看到的對不起來，那正好毀掉這個功能的用途。
 *
 * <p>直接從已經畫好的畫面上裁，拍到的就是他看到的那一幀。
 */
public final class PanelShot {

    /** 圖存在哪。放在 screenshots 底下自己的資料夾，不跟一般截圖混在一起。 */
    private static final String FOLDER = "wynnchayuan";

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);

    /**
     * 上一次畫出來的面板在哪、有多大（GUI 座標）。
     *
     * <p>由繪製端每幀更新。沒有這個就只能拍整個畫面，而整個畫面裡有九成
     * 是跟譯文無關的東西。
     */
    private static volatile int lastX;
    private static volatile int lastY;
    private static volatile int lastW;
    private static volatile int lastH;
    private static volatile String lastName = "panel";

    /** 這一幀有沒有畫過面板。沒畫過就沒有東西好拍。 */
    private static volatile long lastFrame = 0;

    /**
     * 按了快捷鍵，等這一幀畫完就拍。
     *
     * <p>按鍵是在 tick 裡讀到的，那時候這一幀還沒畫。當場拍會拍到<b>上一幀</b>，
     * 而上一幀常常正好是滑鼠剛移到物品上、面板還沒出現的那一幀——
     * 於是使用者按了鍵，拿到一張空白的圖。
     */
    private static volatile boolean pending = false;

    /** 已經自動拍過哪些內容。同一件物品看十次不必拍十張。 */
    private static final java.util.Set<String> seen =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 自動模式一場遊戲最多拍幾張。
     *
     * <p>沒有上限的話，一趟商城逛下來就是幾百張圖躺在硬碟裡。這個功能是拿來
     * 校稿的，不是拿來備份整個遊戲的。
     */
    private static final int AUTO_LIMIT = 200;

    private static int autoTaken = 0;

    private PanelShot() {}

    public static void request() {
        if (WynnChaYuan.config().shotMode() != com.wynnchayuan.CollectorConfig
                .ShotMode.OFF) {
            pending = true;
        }
    }

    /**
     * 每幀在所有繪製之後呼叫。
     *
     * <p>手動請求與自動拍都收在這裡——兩邊都必須等畫完才拍，
     * 分成兩條路只會有一條記得這件事。
     */
    public static void afterRender() {
        if (!pending || !ready()) {
            pending = false;
            return;
        }
        pending = false;
        capture();
    }

    /**
     * 自動模式：這一份譯文是第一次看到就拍一張。
     *
     * <p>{@code key} 是內容本身而不是位置或時間——同一件物品在不同地方看到
     * 是同一份譯文，不需要第二張。
     */
    public static void auto(String key) {
        if (WynnChaYuan.config().shotMode() != com.wynnchayuan.CollectorConfig
                .ShotMode.AUTO) {
            return;
        }
        if (key == null || key.isBlank() || autoTaken >= AUTO_LIMIT
                || !seen.add(key)) {
            return;
        }
        autoTaken++;
        pending = true;
    }

    /** 繪製端呼叫：記下這一幀面板的位置與大小。 */
    public static void note(int x, int y, int w, int h, String name) {
        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;
        if (name != null && !name.isBlank()) {
            lastName = name;
        }
        lastFrame = System.currentTimeMillis();
    }

    /** 現在有沒有面板可以拍。 */
    public static boolean ready() {
        return lastW > 0 && lastH > 0
                && System.currentTimeMillis() - lastFrame < 500;
    }

    /**
     * 拍下目前的面板。
     *
     * <p>要在<b>繪製之後</b>呼叫，這一幀才畫得完整。抓畫面是非同步的
     * （GPU 讀回來要時間），所以裁切與存檔都在回呼裡做。
     */
    public static void capture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !ready()) {
            tell(Component.literal("現在沒有翻譯面板可以拍。")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        // GUI 座標與實際像素差一個縮放倍率。用整數倍率會在 125% 這種
        // 設定下差幾個像素，邊框就被切掉一條。
        double scale = mc.getWindow().getGuiScale();
        int px = (int) Math.floor(lastX * scale);
        int py = (int) Math.floor(lastY * scale);
        int pw = (int) Math.ceil(lastW * scale);
        int ph = (int) Math.ceil(lastH * scale);
        String name = lastName;

        Screenshot.takeScreenshot(mc.getMainRenderTarget(), full -> {
            try (NativeImage image = full) {
                save(crop(image, px, py, pw, ph), name);
            } catch (Exception e) {
                tell(Component.literal("截圖失敗：" + e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        });
    }

    /**
     * 從整張畫面裁出面板那一塊。
     *
     * <p>邊界一律夾住。面板可能有一部分在畫面外（貼著邊緣的時候），
     * 不夾的話 {@code getPixel} 會直接丟例外，而那個例外會在回呼裡
     * 被吞掉——使用者按了鍵，什麼都沒發生，也沒有任何訊息。
     */
    private static NativeImage crop(NativeImage full, int x, int y, int w, int h) {
        int left = Math.max(0, Math.min(x, full.getWidth() - 1));
        int top = Math.max(0, Math.min(y, full.getHeight() - 1));
        int right = Math.max(left + 1, Math.min(x + w, full.getWidth()));
        int bottom = Math.max(top + 1, Math.min(y + h, full.getHeight()));

        NativeImage out = new NativeImage(right - left, bottom - top, false);
        for (int row = top; row < bottom; row++) {
            for (int col = left; col < right; col++) {
                out.setPixel(col - left, row - top, full.getPixel(col, row));
            }
        }
        return out;
    }

    private static void save(NativeImage image, String name) {
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage shot = image) {
            Path dir = mc.gameDirectory.toPath()
                    .resolve(Screenshot.SCREENSHOT_DIR).resolve(FOLDER);
            Files.createDirectories(dir);
            Path file = dir.resolve(safe(name) + "_"
                    + LocalDateTime.now().format(STAMP) + ".png");
            shot.writeToFile(file);
            tell(Component.literal("已存下譯文截圖：")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(file.getFileName().toString())
                            .withStyle(Style.EMPTY
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent.OpenFile(
                                            file.toAbsolutePath().toString())))));
        } catch (Exception e) {
            tell(Component.literal("截圖存檔失敗：" + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    /** 檔名裡不能有的字元一律換掉——物品名稱裡什麼都有可能。 */
    private static String safe(String name) {
        StringBuilder out = new StringBuilder();
        name.codePoints().forEach(cp -> {
            if (cp == '/' || cp == '\\' || cp == ':' || cp == '*' || cp == '?'
                    || cp == '"' || cp == '<' || cp == '>' || cp == '|'
                    || Character.isISOControl(cp)) {
                out.append('_');
            } else {
                out.appendCodePoint(cp);
            }
        });
        String clean = out.toString().strip();
        if (clean.isEmpty()) {
            clean = "panel";
        }
        return clean.length() > 60 ? clean.substring(0, 60) : clean;
    }

    /**
     * 說一聲。
     *
     * <p>一定要丟回<b>主執行緒</b>。抓畫面是非同步的，回呼跑在算繪執行緒上，
     * 在那裡呼叫 {@code displayClientMessage} 不會顯示——使用者按了鍵、
     * 檔案也存好了，畫面上卻什麼都沒發生。第一版就是這樣。
     *
     * <p>順便發一個快門聲。Wynncraft 的聊天欄常常在刷，一行字很容易被沖掉，
     * 但聲音不會。
     */
    private static void tell(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(() -> say(mc, message));
        }
    }

    private static void say(Minecraft mc, Component message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(WynnChaYuan.MOD_NAME)
                                    .withStyle(ChatFormatting.AQUA))
                            .append(Component.literal("] ")
                                    .withStyle(ChatFormatting.DARK_GRAY))
                            .append(message), false);
            mc.player.playSound(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1f);
        }
    }
}
