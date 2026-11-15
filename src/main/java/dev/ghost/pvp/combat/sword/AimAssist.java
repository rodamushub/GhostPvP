package dev.ghost.pvp.combat.sword;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * AimAssist / SilentAim — softly pulls crosshair toward nearest enemy.
 *
 * AimAssist: visible rotation change (legit-ish).
 * SilentAim: rotates for attack packet only, resets after.
 */
public final class AimAssist extends Module {

    public enum AimMode { ASSIST, SILENT }

    private final Setting<AimMode> mode       = addEnum("Mode", "Rotation mode", AimMode.ASSIST);
    private final Setting<Double>  range      = addDouble("Range", "Max target range", 6.0, 1.0, 10.0);
    private final Setting<Double>  strength   = addDouble("Strength", "Snap fraction per tick (0-1)", 0.15, 0.01, 1.0);
    private final Setting<Double>  fov        = addDouble("FOV", "Max angle from look for activation", 60.0, 5.0, 180.0);

    private float savedYaw, savedPitch;
    private boolean rotated = false;

    public AimAssist() {
        super("AimAssist", "Soft aim assist / silent aim toward players", Category.COMBAT);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        // Restore silent aim rotation after last tick
        if (rotated && mode.getValue() == AimMode.SILENT) {
            player.setYRot(savedYaw);
            player.setXRot(savedPitch);
            rotated = false;
        }

        Entity target = findTarget(player);
        if (target == null) return;

        if (mode.getValue() == AimMode.SILENT) {
            savedYaw   = player.getYRot();
            savedPitch = player.getXRot();
            rotated    = true;
        }

        snapToward(player, target);
    }

    private void snapToward(net.minecraft.client.player.LocalPlayer player, Entity target) {
        Vec3 diff  = target.getEyePosition().subtract(player.getEyePosition());
        double yaw = Math.toDegrees(Math.atan2(-diff.x, diff.z));
        double pitch = Math.toDegrees(-Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));

        float dYaw   = (float)(yaw - player.getYRot());
        float dPitch = (float)(pitch - player.getXRot());

        float s = (float) strength.getValue().doubleValue();
        player.setYRot(player.getYRot() + dYaw * s);
        player.setXRot(player.getXRot() + dPitch * s);
    }

    private Entity findTarget(Player self) {
        double r2 = range.getValue() * range.getValue();
        double halfFov = fov.getValue() / 2.0;
        Entity best = null; double bd = Double.MAX_VALUE;
        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == self) continue;
            if (dev.ghost.pvp.GhostClient.get().getFriendManager().isFriend(p.getName().getString())) continue;
            double d = self.distanceToSqr(en);
            if (d > r2) continue;
            Vec3 look = self.getLookAngle();
            Vec3 diff = en.position().subtract(self.getEyePosition()).normalize();
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, look.dot(diff)))));
            if (angle > halfFov) continue;
            if (d < bd) { best = en; bd = d; }
        }
        return best;
    }
}

