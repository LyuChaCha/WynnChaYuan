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

public final class ShotScreen extends Screen {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);

    private final NativeImage shot;

    private final String name;

    private final Screen parent;

    private Identifier preview;

    private DynamicTexture texture;

    private Component note = Component.empty();

    public ShotScreen(NativeImage shot, String name, Screen parent) {
        super(T.c("shot.title"));
        this.shot = shot;
        this.name = name;
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (preview == null) {
            preview = Identifier.fromNamespaceAndPath(
                    WynnChaYuan.MOD_ID, "shot/" + System.nanoTime());
            texture = new DynamicTexture(() -> "wynnchayuan-shot", shot);
            this.minecraft.getTextureManager().register(preview, texture);
        }
        int w = 150;
        int x = leftColumn() - w / 2;
        int y = this.height / 2 - 22;

        addRenderableWidget(Button.builder(T.c("shot.clipboard"),
                b -> copy()).bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(T.c("shot.save"),
                b -> save()).bounds(x, y + 24, w, 20).build());
        addRenderableWidget(Button.builder(T.c("button.close"),
                b -> onClose()).bounds(x, y + 48, w, 20).build());
    }

    private int leftColumn() {
        return Math.max(90, (this.width - previewWidth() - 24) / 2);
    }

    private int previewWidth() {
        return scaled(shot.getWidth());
    }

    private int previewHeight() {
        return scaled(shot.getHeight());
    }

    private int divisor() {
        int roomW = Math.max(60, this.width - 210);
        int roomH = Math.max(60, this.height - 20);
        int k = 1;
        while (shot.getWidth() / k > roomW || shot.getHeight() / k > roomH) {
            k++;
        }
        return k;
    }

    private int scaled(int side) {
        return Math.max(1, side / divisor());
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int w = previewWidth();
        int h = previewHeight();
        int x = this.width - w - 12;
        int y = this.height / 2 - h / 2;

        int accent = WynnChaYuan.config().themeARGB();
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

    private void copy() {
        try {
            BufferedImage image = toBuffered(shot);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new ImageClip(image), null);
            note = T.c("shot.copied")
                    .withStyle(ChatFormatting.GREEN);
        } catch (Throwable t) {
            note = T.c("shot.nocopy")
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
            note = com.wynnchayuan.render.PanelShot.saved(file);
            com.wynnchayuan.render.PanelShot.announce(note);
        } catch (Exception e) {
            note = T.c("shot.failed", e.getMessage())
                    .withStyle(ChatFormatting.RED);
        }
    }

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
