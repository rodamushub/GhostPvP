package dev.ghost.pvp.event.events;

import dev.ghost.pvp.event.Cancellable;
import dev.ghost.pvp.event.Event;
import net.minecraft.network.protocol.Packet;

public final class PacketEvent extends Event implements Cancellable {

    public enum Direction { SEND, RECEIVE }

    public final Direction    direction;
    public final Packet<?>    packet;
    private      boolean      cancelled;

    public PacketEvent(Direction direction, Packet<?> packet) {
        this.direction = direction;
        this.packet    = packet;
    }

    @Override public boolean isCancelled()              { return cancelled; }
    @Override public void    setCancelled(boolean b)    { this.cancelled = b; }
}

