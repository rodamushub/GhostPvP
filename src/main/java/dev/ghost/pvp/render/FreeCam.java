package dev.ghost.pvp.render;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.phys.Vec3;

/**
 * FreeCam — detaches the camera from the player for scouting.
 * Player position is frozen server-side; camera floats freely.
 * Speed controlled via config. Disable snaps camera back.
 */
public final class FreeCam extends Module {

    private final Setting<Double> speed = addDouble("Speed", "Camera movement speed", 0.5, 0.1, 5.0);

    private Vec3    savedPos;
    private float   savedYaw, savedPitch;

    public FreeCam() { super("FreeCam", "Detached spectator-style camera", Category.RENDER); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    @Override protected void onEnable() {
        if (!Globals.inGame()) { setEnabled(false); return; }
        var player  = Globals.player();
        savedPos    = player.position();
        savedYaw    = player.getYRot();
        savedPitch  = player.getXRot();
        player.setNoGravity(true);
    }

    @Override protected void onDisable() {
        if (!Globals.inGame() || savedPos == null) return;
        var player = Globals.player();
        player.setPos(savedPos.x, savedPos.y, savedPos.z);

        double dx = 0, dy = 0, dz = 0;
        float yawRad = (float) Math.toRadians(player.getYRot());

        if (options.keyUp.isDown())    { dx -= Math.sin(yawRad) * spd; dz += Math.cos(yawRad) * spd; }
        if (options.keyDown.isDown())  { dx += Math.sin(yawRad) * spd; dz -= Math.cos(yawRad) * spd; }
        if (options.keyRight.isDown()) { dx += Math.cos(yawRad) * spd; dz += Math.sin(yawRad) * spd; }
        if (options.keyLeft.isDown())  { dx -= Math.cos(yawRad) * spd; dz -= Math.sin(yawRad) * spd; }
        if (options.keyJump.isDown())  dy += spd;
        if (options.keyShift.isDown()) dy -= spd;

        player.setDeltaMovement(0, 0, 0);
        player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(dx, dy, dz));
    }
}

