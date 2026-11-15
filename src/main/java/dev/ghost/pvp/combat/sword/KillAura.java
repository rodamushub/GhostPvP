package dev.ghost.pvp.combat.sword;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * KillAura / AttackAura
 *
 * Modes:
 *   NORMAL — attacks every tick within range
 *   LEGIT  — respects cooldown, FOV limit, smooth rotation, human-like delays
 *   SILENT — attacks without rotating player (packet-level)
 *
 * Target priority: closest / lowest health / crosshair-nearest.
 */
public final class KillAura extends Module {

    public enum KAMode { NORMAL, LEGIT, SILENT }
    public enum TargetPriority { CLOSEST, LOWEST_HP, CROSSHAIR }

    private final Setting<KAMode>        mode         = addEnum("Mode", "Aura mode", KAMode.LEGIT);
    private final Setting<TargetPriority>priority     = addEnum("Priority", "Target selection", TargetPriority.CLOSEST);
    private final Setting<Double>        range        = addDouble("Range", "Attack range", 3.5, 1.0, 6.0);
    private final Setting<Double>        fov          = addDouble("FOV", "Max angle from crosshair (legit)", 90.0, 10.0, 360.0);
    private final Setting<Boolean>       onlyFullCd   = addBool("Full Cooldown", "Only attack on full cooldown (legit)", true);
    private final Setting<Boolean>       rotateSmooth = addBool("Smooth Rotation", "Smooth look toward target", true);
    private final Setting<Double>        rotateSpeed  = addDouble("Rotate Speed", "Max degrees per tick (legit)", 30.0, 1.0, 180.0);
    private final Setting<Integer>       cpsMin       = addInt("CPS Min", "Min clicks per second", 8, 1, 20);
    private final Setting<Integer>       cpsMax       = addInt("CPS Max", "Max clicks per second", 14, 1, 20);
    private final Setting<Boolean>       throughWalls = addBool("Through Walls", "Attack even without LOS", false);

    private long lastAttack = 0;
    private final Random rng = new Random();

    public KillAura() {
        super("KillAura", "Automated melee attacks with legit/silent modes", Category.COMBAT);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        Entity target = findTarget(player);
        if (target == null) return;

        // Cooldown gate (legit mode)
        if (mode.getValue() == KAMode.LEGIT && onlyFullCd.getValue()) {
            if (player.getAttackStrengthScale(0) < 0.99f) return;
        }

        // CPS gate
        int cps = cpsMin.getValue() + rng.nextInt(Math.max(1, cpsMax.getValue() - cpsMin.getValue()));
        long minDelay = 1000L / cps;
        long now = System.currentTimeMillis();
        if (now - lastAttack < minDelay) return;

        // Rotation (legit + normal)
        if (mode.getValue() != KAMode.SILENT) {
            if (rotateSmooth.getValue()) smoothRotate(player, target);
            else snapRotate(player, target);
        }

        Globals.mc.gameMode.attack(player, target);
        lastAttack = now;
    }

    private Entity findTarget(Player self) {
        double r2 = range.getValue() * range.getValue();
        Entity best = null;
        double bestScore = Double.MAX_VALUE;

        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == self) continue;
            if (dev.ghost.pvp.GhostClient.get().getFriendManager().isFriend(p.getName().getString())) continue;

            double d = self.distanceToSqr(en);
            if (d > r2) continue;

            if (!throughWalls.getValue() && !self.hasLineOfSight(en)) continue;

            if (mode.getValue() == KAMode.LEGIT) {
                // FOV check
                if (angleTo(self, en) > fov.getValue() / 2.0) continue;
            }

            double score = switch (priority.getValue()) {
                case CLOSEST    -> d;
                case LOWEST_HP  -> (en instanceof net.minecraft.world.entity.LivingEntity le)
                                   ? le.getHealth() : d;
                case CROSSHAIR  -> angleTo(self, en);
            };

            if (score < bestScore) { best = en; bestScore = score; }
        }
        return best;
    }

    private double angleTo(Player self, Entity target) {
        Vec3 look = self.getLookAngle();
        Vec3 diff = target.position().subtract(self.getEyePosition()).normalize();
        double dot = look.dot(diff);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }

    private void smoothRotate(net.minecraft.client.player.LocalPlayer player, Entity target) {
        Vec3 diff  = target.getEyePosition().subtract(player.getEyePosition());
        double yaw = Math.toDegrees(Math.atan2(-diff.x, diff.z));
        double pitch = Math.toDegrees(-Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));

        float dYaw   = (float)((yaw - player.getYRot()) % 360);
        float dPitch = (float)(pitch - player.getXRot());

        float speed = rotateSpeed.getValue().floatValue();
        player.setYRot(player.getYRot() + Math.max(-speed, Math.min(speed, dYaw)));
        player.setXRot(player.getXRot() + Math.max(-speed, Math.min(speed, dPitch)));
    }

    private void snapRotate(net.minecraft.client.player.LocalPlayer player, Entity target) {
        Vec3 diff = target.getEyePosition().subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-diff.x, diff.z)));
        player.setXRot((float) Math.toDegrees(-Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));
    }
}

