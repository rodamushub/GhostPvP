package dev.ghost.pvp.event.events;

import dev.ghost.pvp.event.Cancellable;
import dev.ghost.pvp.event.Event;
import net.minecraft.world.entity.Entity;

public final class AttackEvent extends Event implements Cancellable {
    public final Entity target;
    private      boolean cancelled;

    public AttackEvent(Entity target) { this.target = target; }

    @Override public boolean isCancelled()           { return cancelled; }
    @Override public void    setCancelled(boolean b) { this.cancelled = b; }
}

