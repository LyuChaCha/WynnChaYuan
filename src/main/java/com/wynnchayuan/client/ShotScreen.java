package com.wynnchayuan.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 拍完之後跳出來的小面板：看一眼、決定要存檔還是複製到剪貼簿。
 *
 * <h2>為什麼不是按一下就存檔</h2>
 * 先前 F8 直接寫檔，然後在聊天欄留一行字。使用者的回報是「按了沒有任何反應」
 * ——Wynncraft 的聊天欄一直在刷，一行字兩秒就被沖掉了，而檔案存在
 * screenshots 資料夾裡，畫面上什麼都看不到。
 *
 * <p>跳一個面板出來就沒有這個問題：拍到什麼、要拿它做什麼，都在眼前。
 * 而且校稿的人多半是要把圖<b>貼進聊天軟體</b>，複製到剪貼簿比存檔再去翻資料夾
 * 直接得多。
 */
public final class ShotScreen extends Screen {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);

    /** 預覽最多佔畫面的多少。太大反而看不出細節在哪一行。 */
    private static final int MAX_PREVIEW = 260;

    private final NativeImage shot;

    private final String name;

    private final Screen parent;

    private Identifier preview;

    private DynamicTexture texture;

    private Component note = Component.empty();

    public ShotScreen(NativeImage shot, String name, Screen parent) {
        super(Component.literal("譯文截圖"));
        this.shot = shot;
        this.name = name;
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (preview == null) {
            // 貼圖只註冊一次。每次 init（改視窗大小時會再跑）都註冊一張的話，
            // 調整幾次視窗就漏掉幾張貼圖。
            preview = Identifier.fromNamespaceAndPath(
                    WynnChaYuan.MOD_ID, "shot/" + System.nanoTime());
            texture = new DynamicTexture(() -> "wynnchayuan-shot", shot);
            this.minecraft.getTextureManager().register(preview, texture);
        }
        // 圖靠右、按鈕靠左：圖片以 1:1 畫出來才看得清楚，而 1:1 常常很寬，
        // 擺中間就會把按鈕擠到畫面外。左右分開兩邊都放得下。
        int w = 150;
        int x = leftColumn() - w / 2;
        int y = this.height / 2 - 22;

        addRenderableWidget(Button.builder(Component.literal("複製到剪貼簿"),
                b -> copy()).bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("儲存成檔案"),
                b -> save()).bounds(x, y + 24, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("關閉"),
                b -> onClose()).bounds(x, y + 48, w, 20).build());
    }

    /** 左半邊的中線——標題與按鈕都對齊這裡。 */
    private int leftColumn() {
        return Math.max(90, (this.width - previewWidth() - 24) / 2);
    }

    private int previewWidth() {
        return scaled(shot.getWidth());
    }

    private int previewHeight() {
        return scaled(shot.getHeight());
    }

    /**
     * 畫成<b>原本在遊戲裡的大小</b>。
     *
     * <h2>為什麼先前會糊</h2>
     * 截圖是照著<b>畫面像素</b>拍的（GUI 縮放 3 倍就是 3 倍大），先前又把它
     * 等比縮到 260 以內才畫。縮放比例不是整數，每個原始像素被攤到 0.7 個
     * 螢幕像素上，邊緣就全部糊掉了。
     *
     * <p>除以 GUI 縮放倍率就回到 1:1——一個原始像素正好落在一個螢幕像素上，
     * 跟遊戲裡看到的一模一樣銳利。真的放不下才等比縮，而且只縮整數倍。
     */
    private int scaled(int side) {
        int gui = guiScale();
        int fit = side / gui;
        int longest = Math.max(shot.getWidth(), shot.getHeight()) / gui;
        int room = Math.min(MAX_PREVIEW, this.height - 60);
        if (longest <= room) {
            return fit;
        }
        // 只縮整數倍，半個像素的縮放就是先前糊掉的原因
        int step = (longest + room - 1) / room;
        return fit / step;
    }

    private int guiScale() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return 1;
        }
        return Math.max(1, (int) mc.getWindow().getGuiScale());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int w = previewWidth();
        int h = previewHeight();
        int x = this.width - w - 12;
        int y = this.height / 2 - h / 2;

        // 外框跟著設定裡的主題色，跟其他面板同一套
        int accent = WynnChaYuan.config().accentARGB();
        graphics.fill(x - 3, y - 3, x + w + 3, y + h + 3, accent);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        graphics.blit(RenderPipelines.GUI_TEXTURED, preview,
                x, y, 0f, 0f, w, h, w, h);

        int mid = leftColumn();
        graphics.drawCenteredString(this.font, this.title,
                mid, this.height / 2 - 46, accent);
        if (!note.getString().isEmpty()) {
            graphics.drawCenteredString(this.font, note,
                    mid, this.height / 2 + 34, 0xFFFFFF);
        }
    }

    /**
     * 複製到剪貼簿，之後在任何地方 Ctrl+V 就貼得出來。
     *
     * <p>走的是 AWT 的系統剪貼簿。Minecraft 本身沒有「複製圖片」的介面，
     * 它的 {@code keyboardHandler.setClipboard} 只吃文字。
     */
    private void copy() {
        try {
            BufferedImage image = toBuffered(shot);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new ImageClip(image), null);
            note = Component.literal("已複製，可以直接 Ctrl+V 貼上")
                    .withStyle(ChatFormatting.GREEN);
        } catch (Throwable t) {
            // 有些環境沒有可用的剪貼簿（無頭、遠端桌面）。存檔那條路還在，
            // 所以這裡只要說清楚就好，不必當成錯誤。
            note = Component.literal("這個系統上複製不了，請改用「儲存成檔案」")
                    .withStyle(ChatFormatting.RED);
        }
    }

    private void save() {
        try {
            Path dir = this.minecraft.gameDirectory.toPath()
                    .resolve("screenshots").resolve(WynnChaYuan.MOD_ID);
            Files.createDirectories(dir);
            Path file = dir.resolve(name + "_"
                    + LocalDateTime.now().format(STAMP) + ".png");
            shot.writeToFile(file);
            note = Component.literal("已存成 " + file.getFileName())
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.OpenFile(
                                    file.toAbsolutePath().toString())));
        } catch (Exception e) {
            note = Component.literal("存檔失敗：" + e.getMessage())
                    .withStyle(ChatFormatting.RED);
        }
    }

    /** {@link NativeImage} 是 ABGR，AWT 要的是 ARGB，逐點換位。 */
    private static BufferedImage toBuffered(NativeImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                out.setRGB(x, y, source.getPixel(x, y) | 0xFF000000);
            }
        }
        return out;
    }

    @Override
    public void onClose() {
        if (texture != null) {
            this.minecraft.getTextureManager().release(preview);
            texture = null;
        }
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 把一張圖交給系統剪貼簿。 */
    private record ImageClip(BufferedImage image) implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            return image;
        }
    }
}
