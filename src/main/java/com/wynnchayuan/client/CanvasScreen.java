package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Shell;
import com.wynnchayuan.client.ui.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

abstract class CanvasScreen extends Screen {

    protected final Screen parent;

    protected CanvasScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected abstract Surface view();

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        GuiCanvas canvas = new GuiCanvas(g, this.font, mc.getWindow().getGuiScale());
        canvas.begin();
        try {
            double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                    / Math.max(1, mc.getWindow().getScreenWidth());
            double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                    / Math.max(1, mc.getWindow().getScreenHeight());
            view().render(canvas, this.width, this.height, mx, my, Util.getMillis());
        } finally {
            canvas.end();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return view().mouseDown(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return view().mouseDrag(event.x(), event.y());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return view().mouseUp();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        return view().scroll(dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // GLFW 修飾鍵位元：2 是 Ctrl、8 是 Super（mac 的 Cmd）
        boolean ctrl = (event.modifiers() & (2 | 8)) != 0;
        if (view().key(event.key(), ctrl)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return view().typed(event.codepointAsString());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static final Shell SHELL = new Shell() {
        @Override
        public int accent() {
            return WynnChaYuan.config().themeARGB();
        }

        @Override
        public int frame() {
            return WynnChaYuan.config().accentARGB();
        }

        @Override
        public boolean blurred() {
            return Minecraft.getInstance().options.getMenuBackgroundBlurriness() > 0;
        }

        @Override
        public String tr(String key, Object... args) {
            return T.s(key, args);
        }

        @Override
        public String language() {
            String pinned = T.pinnedLanguage();
            return pinned != null ? pinned
                    : Minecraft.getInstance().getLanguageManager().getSelected();
        }

        @Override
        public void click() {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    };
}
