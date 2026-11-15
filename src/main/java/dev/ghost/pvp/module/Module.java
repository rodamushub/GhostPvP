package dev.ghost.pvp.module;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.Event;
import dev.ghost.pvp.event.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Module {

    // ── Identity ───────────────────────────────────────────────────────────────
    private final String   name;
    private final String   description;
    private final Category category;

    // ── State ──────────────────────────────────────────────────────────────────
    private boolean enabled;
    private int     keybind = -1; // GLFW key code; -1 = unbound

    // ── Settings ───────────────────────────────────────────────────────────────
    protected final List<Setting<?>> settings = new ArrayList<>();

    // ── Internal ───────────────────────────────────────────────────────────────
    /** Registered listeners so we can remove them on disable. */
    private final List<ListenerHandle<?, ?>> handles = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name        = name;
        this.description = description;
        this.category    = category;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    protected void onEnable()  {}
    protected void onDisable() {}

    public final void toggle() { setEnabled(!enabled); }

    public final void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (enabled) {
            registerListeners();
            onEnable();
        } else {
            onDisable();
            unregisterListeners();
        }
    }

    /** Subclasses override this to call {@link #listen} for every event they care about. */
    protected void registerListeners() {}

    private void unregisterListeners() {
        EventBus bus = GhostClient.get().getEventBus();
        for (ListenerHandle<?, ?> h : handles) h.unsubscribe(bus);
        handles.clear();
    }

    @SuppressWarnings("unchecked")
    protected <T extends Event> void listen(Class<T> type, Consumer<T> handler) {
        GhostClient.get().getEventBus().subscribe(type, handler);
        handles.add(new ListenerHandle<>(type, (Consumer<Event>) handler));
    }

    // ── Settings factory helpers ───────────────────────────────────────────────

    protected Setting<Boolean> addBool(String name, String desc, boolean def) {
        Setting<Boolean> s = new Setting<>(name, desc, def);
        settings.add(s);
        return s;
    }

    protected Setting<Integer> addInt(String name, String desc, int def, int min, int max) {
        Setting<Integer> s = new Setting.IntSetting(name, desc, def, min, max);
        settings.add(s);
        return s;
    }

    protected Setting<Double> addDouble(String name, String desc, double def, double min, double max) {
        Setting<Double> s = new Setting.DoubleSetting(name, desc, def, min, max);
        settings.add(s);
        return s;
    }

    protected <E extends Enum<E>> Setting<E> addEnum(String name, String desc, E def) {
        Setting<E> s = new Setting<>(name, desc, def);
        settings.add(s);
        return s;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public String          getName()        { return name; }
    public String          getDescription() { return description; }
    public Category        getCategory()    { return category; }
    public boolean         isEnabled()      { return enabled; }
    public int             getKeybind()     { return keybind; }
    public void            setKeybind(int k){ this.keybind = k; }
    public List<Setting<?>> getSettings()  { return settings; }

    // ── Inner ──────────────────────────────────────────────────────────────────

    private record ListenerHandle<T extends Event, E extends Event>(
        Class<T> type, Consumer<E> handler
    ) {
        @SuppressWarnings("unchecked")
        void unsubscribe(EventBus bus) {
            bus.unsubscribe((Class<E>) type, handler);
        }
    }
}

