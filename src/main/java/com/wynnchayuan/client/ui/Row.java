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

public final class Row {

    public enum Kind { TOGGLE, SEGMENT, SLIDER, COLOUR, SELECT, ACTION, STATUS }

    public enum Tone { ACCENT, REPLACE, BOTH, OFF, PLAIN }

    public record Option(String label, Tone tone) {}

    public record Choice(String id, String label, String badge, boolean auto) {}

    public final String id;
    public final Kind kind;
    public final Supplier<String> name;
    public final Supplier<String> hint;

    BooleanSupplier on = () -> false;
    Runnable flip = () -> { };
    Supplier<String> onText;

    final List<Option> options = new ArrayList<>();
    IntSupplier selected = () -> 0;
    IntConsumer pick = i -> { };
    Supplier<String> extraLabel;
    Runnable extra;

    int min;
    int max = 100;
    IntSupplier value = () -> 0;
    IntConsumer slide = v -> { };
    IntFunction<String> format = String::valueOf;

    Supplier<String> hex = () -> "#6FA8D8";
    Predicate<String> setHex = s -> false;

    Runnable commit = () -> { };

    Supplier<List<Choice>> choices = List::of;
    Supplier<String> current = () -> "";
    Consumer<String> choose = s -> { };
    BooleanSupplier canApply;
    Runnable apply;
    BooleanSupplier locked = () -> false;
    Supplier<String> busy = () -> null;

    Supplier<String> label = () -> "";
    IntSupplier labelColour;
    BooleanSupplier danger;
    Runnable run = () -> { };
    BooleanSupplier enabled = () -> true;

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

    public Row apply(BooleanSupplier can, Runnable go) {
        this.canApply = can;
        this.apply = go;
        return this;
    }

    public Row locked(BooleanSupplier busyNow) {
        this.locked = busyNow;
        return this;
    }

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

    public Row danger(BooleanSupplier armed) {
        this.danger = armed;
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

    public Row reset(BooleanSupplier atDefault, Runnable toDefault) {
        this.isDefault = atDefault;
        this.reset = toDefault;
        return this;
    }

    public boolean resettable() {
        return reset != null;
    }

    public record Group(Supplier<String> title, boolean tool, List<Row> rows) {}

    public record Tab(String key, Supplier<String> name, Supplier<String> about,
                      Icons.Icon icon, Preview.Scene scene, List<Group> groups) {}
}
