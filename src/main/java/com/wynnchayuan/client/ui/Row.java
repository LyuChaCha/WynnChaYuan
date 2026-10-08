package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 一列設定：名稱、說明與控制項綁在同一個物件上。
 *
 * <h2>為什麼全部是「怎麼讀、怎麼寫」的函式</h2>
 * 這一層不認得 {@code CollectorConfig}。每一列只知道「現在的值去哪裡問、改了去哪裡
 * 講」，所以畫面（{@link SettingsView}）可以在測試裡接一張假的表來跑，而接到真的
 * 設定時，值永遠是<b>當下</b>問到的——不會有畫面上一個值、設定檔裡另一個值的時候。
 *
 * <p>建法是一串流暢呼叫，在 {@code SettingsScreen} 裡一列一行：
 * <pre>
 *   Row.toggle("items.market", name, hint).on(cfg::marketSearch).flip(cfg::toggleMarketSearch)
 * </pre>
 */
public final class Row {

    public enum Kind { TOGGLE, SEGMENT, SLIDER, COLOUR, SELECT, ACTION, STATUS }

    /**
     * 分段控制器的一格是哪一種顯示方式，決定選到時滑塊是什麼顏色。
     *
     * <p>沿用舊畫面的做法（見 {@code ModeColours}）：另開的是主題色、就地取代是它的
     * 補色、原文加譯文在中間、關閉是灰色。掃一眼顏色就知道每一項是哪一種，
     * 不必逐列讀字。{@code PLAIN} 是沒有「開關」之分的選擇（跟隨滑鼠／固定位置）。
     */
    public enum Tone { ACCENT, REPLACE, BOTH, OFF, PLAIN }

    /** 分段控制器的一格。 */
    public record Option(String label, Tone tone) {}

    /** 下拉選單的一項。{@code auto} 是「跟著某某」那一種，徽章用金色。 */
    public record Choice(String id, String label, String badge, boolean auto) {}

    public final String id;
    public final Kind kind;
    public final Supplier<String> name;
    public final Supplier<String> hint;

    // 開關
    BooleanSupplier on = () -> false;
    Runnable flip = () -> { };
    Supplier<String> onText;

    // 分段
    final List<Option> options = new ArrayList<>();
    IntSupplier selected = () -> 0;
    IntConsumer pick = i -> { };
    Supplier<String> extraLabel;
    Runnable extra;

    // 滑桿
    int min;
    int max = 100;
    IntSupplier value = () -> 0;
    IntConsumer slide = v -> { };
    IntFunction<String> format = String::valueOf;

    // 顏色
    Supplier<String> hex = () -> "#6FA8D8";
    Predicate<String> setHex = s -> false;

    // 滑桿與顏色共用：拖完放手時叫一次（存檔）
    Runnable commit = () -> { };

    // 下拉
    Supplier<List<Choice>> choices = List::of;
    Supplier<String> current = () -> "";
    Consumer<String> choose = s -> { };
    BooleanSupplier canApply;
    Runnable apply;
    BooleanSupplier locked = () -> false;
    Supplier<String> busy = () -> null;

    // 動作與狀態
    Supplier<String> label = () -> "";
    IntSupplier labelColour;
    Runnable run = () -> { };
    BooleanSupplier enabled = () -> true;

    // 重置
    Runnable reset;
    BooleanSupplier isDefault = () -> true;

    private Row(String id, Kind kind, Supplier<String> name, Supplier<String> hint) {
        this.id = id;
        this.kind = kind;
        this.name = name;
        this.hint = hint;
    }

    public static Row toggle(String id, Supplier<String> name, Supplier<String> hint) {
        return new Row(id, Kind.TOGGLE, name, hint);
    }

    public static Row segment(String id, Supplier<String> name, Supplier<String> hint) {
        return new Row(id, Kind.SEGMENT, name, hint);
    }

    public static Row slider(String id, Supplier<String> name, Supplier<String> hint) {
        return new Row(id, Kind.SLIDER, name, hint);
    }

    public static Row colour(String id, Supplier<String> name, Supplier<String> hint) {
        return new Row(id, Kind.COLOUR, name, hint);
    }

    public static Row select(String id, Supplier<String> name, Supplier<String> hint) {
        return new Row(id, Kind.SELECT, name, hint);
    }

    public static Row action(String id, Supplier<String> name, Supplier<String> hint) {
        return new Row(id, Kind.ACTION, name, hint);
    }

    public static Row status(String id, Supplier<String> name, Supplier<String> hint) {
        return new Row(id, Kind.STATUS, name, hint);
    }

    public Row on(BooleanSupplier get) {
        this.on = get;
        return this;
    }

    public Row flip(Runnable toggle) {
        this.flip = toggle;
        return this;
    }

    /** 開著的時候旁邊那兩三個字；不給就是「開啟」。 */
    public Row onText(Supplier<String> text) {
        this.onText = text;
        return this;
    }

    public Row option(String text, Tone tone) {
        options.add(new Option(text, tone));
        return this;
    }

    public Row selected(IntSupplier index) {
        this.selected = index;
        return this;
    }

    public Row pick(IntConsumer set) {
        this.pick = set;
        return this;
    }

    /** 分段右邊再帶一顆小按鈕（進階…）。 */
    public Row extra(Supplier<String> text, Runnable go) {
        this.extraLabel = text;
        this.extra = go;
        return this;
    }

    public Row range(int lo, int hi) {
        this.min = lo;
        this.max = hi;
        return this;
    }

    public Row value(IntSupplier get) {
        this.value = get;
        return this;
    }

    /** 拖曳途中每變一格就叫一次——即時預覽靠的就是這個。 */
    public Row slide(IntConsumer live) {
        this.slide = live;
        return this;
    }

    public Row format(IntFunction<String> show) {
        this.format = show;
        return this;
    }

    public Row hex(Supplier<String> get) {
        this.hex = get;
        return this;
    }

    /** @param live 收 {@code #RRGGBB}，格式不對回 {@code false} */
    public Row setHex(Predicate<String> live) {
        this.setHex = live;
        return this;
    }

    public Row commit(Runnable save) {
        this.commit = save;
        return this;
    }

    public Row choices(Supplier<List<Choice>> list) {
        this.choices = list;
        return this;
    }

    public Row current(Supplier<String> id) {
        this.current = id;
        return this;
    }

    public Row choose(Consumer<String> set) {
        this.choose = set;
        return this;
    }

    /** 選了不會馬上生效、要再按一次「套用」的那種（換語言要下載整份譯文）。 */
    public Row apply(BooleanSupplier can, Runnable go) {
        this.canApply = can;
        this.apply = go;
        return this;
    }

    public Row locked(BooleanSupplier busyNow) {
        this.locked = busyNow;
        return this;
    }

    /** 忙的時候顯示在選單上的字（下載中 3/31）；{@code null} 就照常顯示選到的那一項。 */
    public Row busy(Supplier<String> text) {
        this.busy = text;
        return this;
    }

    public Row label(Supplier<String> text) {
        this.label = text;
        return this;
    }

    public Row labelColour(IntSupplier argb) {
        this.labelColour = argb;
        return this;
    }

    public Row run(Runnable go) {
        this.run = go;
        return this;
    }

    public Row enabled(BooleanSupplier can) {
        this.enabled = can;
        return this;
    }

    /** 這一列有預設值可以回去。沒叫這個的列（動作、狀態）不會出現在「重置本頁」裡。 */
    public Row reset(BooleanSupplier atDefault, Runnable toDefault) {
        this.isDefault = atDefault;
        this.reset = toDefault;
        return this;
    }

    public boolean resettable() {
        return reset != null;
    }

    /** 一組設定，畫成一張卡片。{@code tool} 是「按了就做一件事」的那一組，框線用主題色。 */
    public record Group(Supplier<String> title, boolean tool, List<Row> rows) {}

    /** 一個分類。{@code scene} 決定右邊的即時預覽畫哪一種示意。 */
    public record Tab(String key, Supplier<String> name, Supplier<String> about,
                      Icons.Icon icon, Preview.Scene scene, List<Group> groups) {}
}
