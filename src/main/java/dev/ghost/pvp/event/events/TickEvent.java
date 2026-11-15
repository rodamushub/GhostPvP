package dev.ghost.pvp.event.events;

import dev.ghost.pvp.event.Event;
import net.minecraft.client.Minecraft;

public final class TickEvent extends Event {
    public final Minecraft client;
    public TickEvent(Minecraft client) { this.client = client; }
}

