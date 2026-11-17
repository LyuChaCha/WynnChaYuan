package com.wynnchayuan.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.translate.ErrorDebug;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;

final class ImageClipboard {

    private ImageClipboard() {}

    static boolean copy(NativeImage source, String where) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new ImageClip(toBuffered(source)), null);
            return true;
        } catch (Throwable t) {
            ErrorDebug.note(where, null, t);
            return false;
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
