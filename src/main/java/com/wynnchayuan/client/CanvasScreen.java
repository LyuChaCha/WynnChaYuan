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

/**
 * 用 {@link Surface} 畫的畫面共用的那一層：把遊戲的滑鼠鍵盤事件轉過去，
 * 把畫布接上，關掉時回到上一個畫面。
 *
 * <p>畫面本身長什麼樣、點哪裡會怎樣，全部在各自的 {@code *View}，那邊不認得遊戲。
 */
abstract class CanvasScreen extends Screen {

    protected final Screen parent;

    protected CanvasScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** 這個畫面要畫的東西。 */
    protected abstract Surface view();

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        GuiCanvas canvas = new GuiCanvas(g, this.font, mc.getWindow().getGuiScale());
        canvas.begin();
        try {
            // 滑鼠座標給的是整數的 GUI 像素；問視窗可以拿到小數，拖東西才不會一格一格跳
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
        // GLFW 的修飾鍵位元：2 是 Ctrl、8 是 Super（mac 的 Cmd）
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

    /** 每個畫面都要跟遊戲問的那幾件事。 */
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
