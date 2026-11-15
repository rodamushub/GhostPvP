package dev.ghost.pvp.event.events;

import dev.ghost.pvp.event.Cancellable;
import dev.ghost.pvp.event.Event;
import net.minecraft.world.phys.Vec3;

public final class MoveEvent extends Event implements Cancellable {
    public double x, y, z;
    private boolean cancelled;

    public MoveEvent(Vec3 motion) {
        this.x = motion.x;
        this.y = motion.y;
        this.z = motion.z;
    }

    public Vec3 asVec3() { return new Vec3(x, y, z); }

    @Override public boolean isCancelled()           { return cancelled; }
    @Override public void    setCancelled(boolean b) { this.cancelled = b; }
}

