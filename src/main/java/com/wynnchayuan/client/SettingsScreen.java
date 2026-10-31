package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.LanguageLayers;
import com.wynnchayuan.Releases;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Preview;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.SettingsView;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.Languages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class SettingsScreen extends Screen {

    static int lastTab = 0;

    private final Screen parent;

    final ModsTab modsTab = new ModsTab(this);
    final PrivacyTab privacyTab = new PrivacyTab(this);
    final BadgeTab badgeTab = new BadgeTab(this);
    final DisplayTabs displayTabs = new DisplayTabs(this);
    final LanguageTab languageTab = new LanguageTab(this);
    final ToolsTab toolsTab = new ToolsTab(this);
    SettingsView view;

    private String status = "";
    private int statusColour = Ui.TEXT_3;
    private long statusAt = 0;
    private static final long STATUS_MS = 4000;

    public SettingsScreen() {
        this(null);
    }

    public SettingsScreen(Screen parent) {
        super(Component.literal("WynnChaYuan"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (view == null) {
            view = new SettingsView(buildTabs(), new Host(), lastTab);
        }
    }

    Minecraft client() {
        return this.minecraft;
    }

    static CollectorConfig cfg() {
        return WynnChaYuan.config();
    }

    void say(String text, int colour) {
        status = text;
        statusColour = colour;
        statusAt = System.currentTimeMillis();
    }

    static Supplier<String> t(String key) {
        return () -> T.s(key);
    }

    static Row toggle(String key, boolean byDefault,
                              BooleanSupplier get, Runnable flip) {
        return Row.toggle(key, t(key), t(key + ".hint")).on(get).flip(flip)
                .reset(() -> get.getAsBoolean() == byDefault, () -> {
                    if (get.getAsBoolean() != byDefault) {
                        flip.run();
                    }
                });
    }

    static <E extends Enum<E>> void cycleTo(Supplier<E> get, Runnable next, E want) {
        for (int i = 0; i < want.getDeclaringClass().getEnumConstants().length
                && get.get() != want; i++) {
            next.run();
        }
    }

    List<Row.Tab> buildTabs() {
        List<Row.Tab> tabs = new ArrayList<>();
        tabs.add(new Row.Tab("items", t("tab.items"), t("tab.items.about"), Icons.Icon.BOW,
                Preview.Scene.TOOLTIP, List.of(
                        new Row.Group(t("group.items"), false, displayTabs.items()),
                        new Row.Group(t("group.mods"), false, modsTab.mods()))));
        tabs.add(new Row.Tab("panel", t("tab.panel"), t("tab.panel.about"), Icons.Icon.SCROLL,
                Preview.Scene.PANEL, List.of(
                        new Row.Group(t("group.place"), false, displayTabs.place()),
                        new Row.Group(t("group.look"), false, displayTabs.look()))));
        tabs.add(new Row.Tab("dialogue", t("tab.dialogue"), t("tab.dialogue.about"),
                Icons.Icon.BUBBLE, Preview.Scene.DIALOGUE,
                List.of(new Row.Group(t("group.dialogue"), false, displayTabs.dialogue()))));
        tabs.add(new Row.Tab("world", t("tab.world"), t("tab.world.about"), Icons.Icon.COMPASS,
                Preview.Scene.WORLD, List.of(new Row.Group(t("group.world"), false, displayTabs.world()))));
        tabs.add(new Row.Tab("data", t("tab.data"), t("tab.data.about"), Icons.Icon.BOOK,
                Preview.Scene.DATA, List.of(
                        new Row.Group(t("group.lang"), false, languageTab.languages()),
                        new Row.Group(t("group.tools"), true, toolsTab.tools()))));
        tabs.add(new Row.Tab("badge", t("tab.badge"), t("tab.badge.about"), Icons.Icon.LEAF,
                Preview.Scene.CAPE, List.of(
                        new Row.Group(t("group.badge"), false, badgeTab.badge()),
                        new Row.Group(t("group.cape"), false, modsTab.fun()),
                        new Row.Group(t("group.privacy"), false, privacyTab.privacy()),
                        new Row.Group(t("group.perf"), false, List.of(
                                toggle("perf.highlight", true, cfg()::wynntilsFps,
                                       cfg()::toggleWynntilsFps))))));
        return tabs;
    }

    void rebuild() {
        float at = view.scrollPosition();
        lastTab = view.tab();
        view = new SettingsView(buildTabs(), new Host(), lastTab);
        view.restoreScroll(at);
    }

    final class Host implements SettingsView.Host {

        @Override
        public int accent() {
            return cfg().themeARGB();
        }

        @Override
        public int frame() {
            return cfg().accentARGB();
        }

        @Override
        public int tone(Row.Tone tone) {
            int accent = cfg().themeARGB();
            return switch (tone) {
                case REPLACE -> ModeColours.replace(accent);
                case BOTH -> ModeColours.both(accent);
                case OFF -> 0x40FFFFFF;
                default -> accent;
            };
        }

        @Override
        public String tr(String key, Object... args) {
            return T.s(key, args);
        }

        @Override
        public String title() {
            return "WynnChaYuan";
        }

        @Override
        public String tagline() {
            return T.s("header.tagline");
        }

        @Override
        public String version() {
            return "v" + WynnChaYuan.version();
        }

        @Override
        public SettingsView.Status status() {
            if (!status.isEmpty()
                    && (toolsTab.fetching || System.currentTimeMillis() - statusAt < STATUS_MS)) {
                return new SettingsView.Status(status, statusColour);
            }
            int size = WynnChaYuan.translations().size();
            return new SettingsView.Status(T.s("footer.hint", String.format("%,d", size)),
                    size > 0 ? Ui.TEXT_3 : Ui.RED);
        }

        @Override
        public boolean hasUpdate() {
            return Releases.newer() != null;
        }

        @Override
        public boolean blurred() {
            Minecraft mc = Minecraft.getInstance();
            return mc.options.getMenuBackgroundBlurriness() > 0;
        }

        @Override
        public void openNotice() {
            SettingsScreen.this.minecraft.setScreen(new NoticeScreen(SettingsScreen.this));
        }

        @Override
        public void openUpdates() {
            SettingsScreen.this.minecraft.setScreen(new ReleaseNotesScreen(SettingsScreen.this));
        }

        @Override
        public void openCredits() {
            SettingsScreen.this.minecraft.setScreen(new CreditsScreen(SettingsScreen.this));
        }

        @Override
        public void done() {
            if (languageTab.pendingLanguage != null) {
                languageTab.applyLanguage();
            } else if (languageTab.pendingFallback != null) {
                languageTab.applyFallback();
            }
            onClose();
        }

        @Override
        public String clipboard() {
            return Minecraft.getInstance().keyboardHandler.getClipboard();
        }

        @Override
        public void setClipboard(String text) {
            Minecraft.getInstance().keyboardHandler.setClipboard(text);
        }

        @Override
        public void click() {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }

        @Override
        public Preview.State preview() {
            return previewState();
        }

        @Override
        public void resetDone(String what) {
            say(T.s("reset.done", what), cfg().themeARGB());
        }
    }

    private final Preview.State previewState = new Preview.State();

    private Preview.State previewState() {
        Preview.State s = previewState;
        CollectorConfig c = cfg();
        s.tooltipMode = c.tooltipMode().ordinal();
        s.names = c.itemNames().ordinal();
        s.anchorFixed = c.panelAnchor() == CollectorConfig.PanelAnchor.FIXED;
        s.side = c.panelSide().ordinal();
        s.gap = c.panelGap();
        s.dialogueMode = c.dialogueMode().ordinal();
        s.choiceMode = c.choiceMode().ordinal();
        s.holdSeconds = DisplayTabs.holdSeconds();
        s.overlays = c.showOverlays();
        s.nametag = c.nametagMode().ordinal();
        s.chat = c.chatMode().ordinal();
        s.titles = c.translateTitles();
        s.bossbar = c.translateBossBar();
        s.tracker = switch (c.trackerMode()) {
            case REPLACE -> 0;
            case PANEL -> 1;
            case OFF -> 2;
        };
        s.objectives = c.translateObjectives();
        s.scoreboard = c.translateScoreboard();
        s.heldItem = c.translateHeldItem();
        s.loaded = WynnChaYuan.translations().size();
        s.loadedLabel = T.s("preview.loaded");
        s.panelOffNote = T.s("preview.panel.off");
        String lang = c.language();
        String under = LanguageLayers.fallbackLanguage();
        s.facts = new String[][] {
            {T.s("data.language"), Languages.nativeName(
                    lang.isEmpty() ? LanguageLayers.autoLanguage() : lang)},
            {T.s("data.fallback"), under == null ? T.s("data.fallback.off")
                    : Languages.nativeName(under)},
            {T.s("data.source"), T.s(ToolsTab.github() ? "data.source.github" : "data.source.local")},
            {T.s("data.version"), T.s(ToolsTab.VERSION_KEY.get(toolsTab.versionState())), "version"},
        };
        s.versionColour = switch (toolsTab.versionState()) {
            case LATEST -> Ui.GREEN;
            case BEHIND -> Ui.AMBER;
            case UNKNOWN -> Ui.RED;
            default -> Ui.TEXT_2;
        };
        s.translate = SettingsScreen::previewLookup;
        return s;
    }

    static String previewLookup(String text) {
        var store = WynnChaYuan.translations();
        String hit = store.lookup(text);
        if (hit == null || hit.isBlank()) {
            return text;
        }
        return Preview.bareName(hit, text, store.appendedOriginalAt(hit));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int guiScale = (int) mc.getWindow().getGuiScale();
        GuiCanvas canvas = new GuiCanvas(g, this.font, guiScale);
        view.beneathOverlays(() -> {
            float[] model = view.spot("preview.model");
            if (model != null && mc.player != null) {
                canvas.outside(() -> {
                    try {
                        drawPlayer(g, mc, model, view.modelYaw(), view.modelPitch());
                    } catch (Throwable t) {
                        ErrorDebug.note("settings.model", null, t);
                    }
                });
            }
        });
        canvas.begin();
        try {
            double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                    / Math.max(1, mc.getWindow().getScreenWidth());
            double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                    / Math.max(1, mc.getWindow().getScreenHeight());
            view.render(canvas, this.width, this.height, mx, my, Util.getMillis());
        } finally {
            canvas.end();
        }
    }

    private static void drawPlayer(GuiGraphics g, Minecraft mc, float[] box, float yaw,
                                   float pitch) {
        LivingEntity player = mc.player;
        int x1 = Math.round(box[0]);
        int y1 = Math.round(box[1]);
        int x2 = Math.round(box[0] + box[2]);
        int y2 = Math.round(box[1] + box[3]) - 14;
        if (x2 - x1 < 20 || y2 - y1 < 30) {
            return;
        }
        EntityRenderState state =
                mc.getEntityRenderDispatcher().getRenderer(player).createRenderState(player, 1.0f);
        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        if (state instanceof LivingEntityRenderState
                living) {
            living.bodyRot = 180.0f + yaw;
            living.yRot = 0;
            living.xRot = 0;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0f;
        }
        if (state instanceof AvatarRenderState avatar) {
            avatar.capeFlap = 0;
            avatar.capeLean = 6;
            avatar.capeLean2 = 0;
        }
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(pitch * (float) (Math.PI / 180.0));
        pose.mul(camera);
        float size = Math.min((y2 - y1) / 2.3f, (x2 - x1) / 1.5f);
        Vector3f lift = new Vector3f(0, state.boundingBoxHeight / 2.0f, 0);
        g.submitEntityRenderState(state, size, lift, pose, camera, x1, y1, x2, y2);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return view.mouseDown(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return view.mouseDrag(event.x(), event.y());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return view.mouseUp();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        return view.scroll(dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // GLFW 修飾鍵位元：2 是 Ctrl、8 是 Super（mac 的 Cmd）
        boolean ctrl = (event.modifiers() & (2 | 8)) != 0;
        if (view.key(event.key(), ctrl)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return view.typed(event.codepointAsString());
    }

    @Override
    public void onClose() {
        cfg().saveIfDirty();
        lastTab = view == null ? lastTab : view.tab();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        cfg().saveIfDirty();
        if (view != null) {
            lastTab = view.tab();
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
