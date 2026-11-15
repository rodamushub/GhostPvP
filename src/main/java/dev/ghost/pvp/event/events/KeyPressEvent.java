package dev.ghost.pvp.event.events;

import dev.ghost.pvp.event.Event;

public final class KeyPressEvent extends Event {
    public final int key;
    public final int action; // GLFW_PRESS / GLFW_RELEASE / GLFW_REPEAT

    public KeyPressEvent(int key, int action) {
        this.key    = key;
        this.action = action;
    }
}

