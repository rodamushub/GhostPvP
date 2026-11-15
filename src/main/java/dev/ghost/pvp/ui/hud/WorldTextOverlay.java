package dev.ghost.pvp.ui.hud;

import java.util.ArrayList;
import java.util.List;

/** Thread-safe queue of screen-space strings accumulated during world render, flushed by HudRenderer. */
public final class WorldTextOverlay {

    public record Entry(String text, float x, float y, int color) {}

    private static final List<Entry> pending = new ArrayList<>();

    private WorldTextOverlay() {}

    public static synchronized void queue(String text, float x, float y, int color) {
        pending.add(new Entry(text, x, y, color));
    }

    public static synchronized List<Entry> flushAndGet() {
        List<Entry> copy = new ArrayList<>(pending);
        pending.clear();
        return copy;
    }
}

