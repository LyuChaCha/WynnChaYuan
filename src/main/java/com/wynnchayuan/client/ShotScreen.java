package com.wynnchayuan.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.ChatFormatting;
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
        int y = this.height / 2 + previewHeight() / 2 + 12;
        int w = 150;
        int x = this.width / 2 - w - 4;

        addRenderableWidget(Button.builder(Component.literal("複製到剪貼簿"),
                b -> copy()).bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("儲存成檔案"),
                b -> save()).bounds(this.width / 2 + 4, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("關閉"),
                b -> onClose())
                .bounds(this.width / 2 - 50, y + 24, 100, 20).build());
    }

    private int previewWidth() {
        return scaled(shot.getWidth());
    }

    private int previewHeight() {
        return scaled(shot.getHeight());
    }

    /** 等比縮到放得下為止。放大則不做——放大的點陣圖只會更難看清楚。 */
    private int scaled(int side) {
        int longest = Math.max(shot.getWidth(), shot.getHeight());
        return longest <= MAX_PREVIEW ? side : side * MAX_PREVIEW / longest;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int w = previewWidth();
        int h = previewHeight();
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - h / 2;
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
        graphics.blit(RenderPipelines.GUI_TEXTURED, preview,
                x, y, 0f, 0f, w, h, w, h);
        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, y - 24, 0xFFFFFF);
        if (!note.getString().isEmpty()) {
            graphics.drawCenteredString(this.font, note,
                    this.width / 2, y + h + 66, 0xFFFFFF);
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
