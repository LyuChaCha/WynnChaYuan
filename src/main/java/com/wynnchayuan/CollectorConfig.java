package com.wynnchayuan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 使用者設定，開關狀態會記住。
 *
 * <p>刻意做得極簡：只有幾個布林值，直接讀寫一個小 JSON，
 * 不引入設定框架，也不依賴 ModMenu／YACL——少一個依賴就少一個壞掉的理由。
 */
public final class CollectorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;

    /** 是否顯示翻譯面板。用快捷鍵切換，關掉之後仍然照常收集。 */
    private boolean showPanel = true;

    /** 是否收集未翻譯字串。 */
    private boolean collect = true;

    /**
     * 譯文從哪裡來。
     *
     * <p>{@code GITHUB}：進遊戲時從 repo 同步，所有人看到同一份最新翻譯。
     * {@code LOCAL}：只用 config 目錄下的檔案，給自己試譯或離線使用。
     */
    private Source source = Source.GITHUB;

    /** GITHUB 以遠端為準（本機仍可覆蓋）；LOCAL 完全只看本機檔案。 */
    public enum Source { GITHUB, LOCAL }

    /**
     * 是否翻譯 NPC 頭頂名牌。
     *
     * <p>名牌是唯一就地替換原文的地方（浮在 3D 世界裡，沒辦法開側欄），
     * 所以獨立一個開關，不想動原文的人可以只關這項。
     */
    private boolean translateNametags = true;

    /**
     * 名牌翻譯的呈現方式。
     *
     * <p>預設 {@code LOOK_AT}：原文名牌不動，注視時另外跳一個小框。
     * 就地取代會讓畫面上再也看不到原文，跟只認得英文名的老玩家就對不上話。
     */
    private NametagMode nametagMode = NametagMode.LOOK_AT;

    /** OFF 不翻；LOOK_AT 注視時顯示小框；REPLACE 直接取代名牌文字。 */
    public enum NametagMode { OFF, LOOK_AT, REPLACE }

    /** tooltip 面板放在原本 tooltip 的哪一邊。 */
    private PanelSide panelSide = PanelSide.AUTO;

    /** 面板位置選項。AUTO 會依畫面空間自動決定。 */
    public enum PanelSide { AUTO, RIGHT, LEFT }

    /**
     * 是否翻譯物品名稱。
     *
     * <p>裝備名稱多半是專有名詞，翻了反而對不上社群討論與 wiki，
     * 所以獨立一個開關，預設不翻。
     */
    private boolean translateItemNames = false;

    /** 面板與原 tooltip 之間的間距（像素）。 */
    private int panelGap = 12;

    /** 面板要跟著滑鼠，還是固定在畫面上某處。 */
    private PanelAnchor panelAnchor = PanelAnchor.FOLLOW;

    /** FOLLOW 跟著 tooltip 走；FIXED 固定在 fixedX/fixedY，可按住 ALT 拖曳。 */
    public enum PanelAnchor { FOLLOW, FIXED }

    private int fixedX = 20;
    private int fixedY = 20;

    public CollectorConfig(Path file) {
        this.file = file;
        load();
    }

    public boolean showPanel() {
        return showPanel;
    }

    public boolean collect() {
        return collect;
    }

    public boolean translateNametags() {
        return translateNametags;
    }

    public boolean translateItemNames() {
        return translateItemNames;
    }

    public boolean toggleItemNames() {
        translateItemNames = !translateItemNames;
        save();
        return translateItemNames;
    }

    public int panelGap() {
        return panelGap;
    }

    /** 在 4/8/12/16/24 之間輪替。 */
    public int cyclePanelGap() {
        int[] steps = {4, 8, 12, 16, 24};
        int idx = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == panelGap) {
                idx = i;
                break;
            }
        }
        panelGap = steps[(idx + 1) % steps.length];
        save();
        return panelGap;
    }

    public PanelAnchor panelAnchor() {
        return panelAnchor;
    }

    public PanelAnchor togglePanelAnchor() {
        panelAnchor = panelAnchor == PanelAnchor.FOLLOW
                ? PanelAnchor.FIXED : PanelAnchor.FOLLOW;
        save();
        return panelAnchor;
    }

    public int fixedX() {
        return fixedX;
    }

    public int fixedY() {
        return fixedY;
    }

    /** 拖曳時呼叫。存檔有節流，不會每幀寫磁碟。 */
    public void setFixedPos(int x, int y) {
        if (fixedX == x && fixedY == y) {
            return;
        }
        fixedX = x;
        fixedY = y;
        dirty = true;
    }

    /** 把拖曳期間累積的變更寫入磁碟。 */
    public void saveIfDirty() {
        if (dirty) {
            dirty = false;
            save();
        }
    }

    private boolean dirty = false;

    public boolean toggleNametags() {
        translateNametags = !translateNametags;
        save();
        return translateNametags;
    }

    public NametagMode nametagMode() {
        return nametagMode;
    }

    /** 在 關閉 → 注視顯示 → 就地取代 之間輪替。 */
    public NametagMode cycleNametagMode() {
        NametagMode[] all = NametagMode.values();
        nametagMode = all[(nametagMode.ordinal() + 1) % all.length];
        save();
        return nametagMode;
    }

    public PanelSide panelSide() {
        return panelSide;
    }

    /** 依序在 AUTO → RIGHT → LEFT 之間輪替。 */
    public PanelSide cyclePanelSide() {
        PanelSide[] all = PanelSide.values();
        panelSide = all[(panelSide.ordinal() + 1) % all.length];
        save();
        return panelSide;
    }

    public Source source() {
        return source;
    }

    public Source toggleSource() {
        source = source == Source.GITHUB ? Source.LOCAL : Source.GITHUB;
        save();
        return source;
    }

    public boolean toggleCollect() {
        collect = !collect;
        save();
        return collect;
    }

    /** 切換面板顯示，並立刻存檔。 */
    public boolean togglePanel() {
        showPanel = !showPanel;
        save();
        return showPanel;
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            if (o.has("showPanel")) {
                showPanel = o.get("showPanel").getAsBoolean();
            }
            if (o.has("collect")) {
                collect = o.get("collect").getAsBoolean();
            }
            if (o.has("source")) {
                source = Source.valueOf(o.get("source").getAsString());
            }
            if (o.has("translateNametags")) {
                translateNametags = o.get("translateNametags").getAsBoolean();
            }
            if (o.has("nametagMode")) {
                nametagMode = NametagMode.valueOf(o.get("nametagMode").getAsString());
            }
            if (o.has("panelSide")) {
                panelSide = PanelSide.valueOf(o.get("panelSide").getAsString());
            }
            if (o.has("translateItemNames")) {
                translateItemNames = o.get("translateItemNames").getAsBoolean();
            }
            if (o.has("panelGap")) {
                panelGap = o.get("panelGap").getAsInt();
            }
            if (o.has("panelAnchor")) {
                panelAnchor = PanelAnchor.valueOf(o.get("panelAnchor").getAsString());
            }
            if (o.has("fixedX")) {
                fixedX = o.get("fixedX").getAsInt();
            }
            if (o.has("fixedY")) {
                fixedY = o.get("fixedY").getAsInt();
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 設定讀取失敗，使用預設值: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            o.addProperty("showPanel", showPanel);
            o.addProperty("collect", collect);
            o.addProperty("source", source.name());
            o.addProperty("translateNametags", translateNametags);
            o.addProperty("nametagMode", nametagMode.name());
            o.addProperty("panelSide", panelSide.name());
            o.addProperty("translateItemNames", translateItemNames);
            o.addProperty("panelGap", panelGap);
            o.addProperty("panelAnchor", panelAnchor.name());
            o.addProperty("fixedX", fixedX);
            o.addProperty("fixedY", fixedY);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(o, w);
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 設定寫入失敗: " + e.getMessage());
        }
    }
}
