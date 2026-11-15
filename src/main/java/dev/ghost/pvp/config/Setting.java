package dev.ghost.pvp.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Setting<T> {

    private final String          name;
    private final String          description;
    private final T               defaultValue;
    private       T               value;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    public Setting(String name, String description, T defaultValue) {
        this.name         = name;
        this.description  = description;
        this.defaultValue = defaultValue;
        this.value        = defaultValue;
    }

    public T    getValue()        { return value; }
    public T    getDefault()      { return defaultValue; }
    public String getName()       { return name; }
    public String getDescription(){ return description; }

    public void setValue(T v) {
        this.value = v;
        listeners.forEach(l -> l.accept(v));
    }

    public void onChange(Consumer<T> listener) { listeners.add(listener); }

    public void reset() { setValue(defaultValue); }

    // ── Typed subclasses for slider bounds ────────────────────────────────────

    public static class IntSetting extends Setting<Integer> {
        public final int min, max;
        public IntSetting(String name, String desc, int def, int min, int max) {
            super(name, desc, def);
            this.min = min;
            this.max = max;
        }
        @Override public void setValue(Integer v) {
            super.setValue(Math.max(min, Math.min(max, v)));
        }
    }

    public static class DoubleSetting extends Setting<Double> {
        public final double min, max;
        public DoubleSetting(String name, String desc, double def, double min, double max) {
            super(name, desc, def);
            this.min = min;
            this.max = max;
        }
        @Override public void setValue(Double v) {
            super.setValue(Math.max(min, Math.min(max, v)));
        }
    }
}

