package dev.ghost.pvp.combat.spear;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * AutoLunge / LungeAssist
 *
 * Tick-perfect lunge: detects when the player is in mid-air (falling/jumping)
 * and the target is within lunge range, then triggers the spear throw automatically
 * on the optimal tick (peak of jump or falling phase).
 *
 * Optimal window: player.getDeltaMovement().y < 0 (descending) for maximum lunge height.
 */
public final class AutoLunge extends Module {

    private final Setting<Double>  targetRange = addDouble("Target Range", "Max target distance for auto-lunge", 6.0, 1.0, 10.0);
    private final Setting<Double>  minHeight   = addDouble("Min Fall Height", "Minimum fall velocity to trigger", 0.1, 0.0, 1.0);
    private final Setting<Boolean> aimAssist   = addBool("Aim Assist", "Snap look toward target on lunge", true);

    public AutoLunge() {
        super("AutoLunge", "Tick-perfect auto lunge on falling phase", Category.SPEAR);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        // Only lunge when descending
        double vy = player.getDeltaMovement().y;
        if (vy > -minHeight.getValue()) return;
        if (player.onGround()) return;

        // Find nearest target
        Entity target = findTarget(player);
        if (target == null) return;

        // Optionally face target
        if (aimAssist.getValue()) faceTarget(player, target);

        // Use main hand (spear must be held — caller responsibility to switch via AttributeSwap)
        Globals.mc.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private Entity findTarget(Player self) {
        double r2 = targetRange.getValue() * targetRange.getValue();
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == self) continue;
            if (dev.ghost.pvp.GhostClient.get().getFriendManager().isFriend(p.getName().getString())) continue;
            double d = self.distanceToSqr(en);
            if (d < r2 && d < bestDist) { best = en; bestDist = d; }
        }
        return best;
    }

    private void faceTarget(net.minecraft.client.player.LocalPlayer player, Entity target) {
        Vec3 diff = target.position().subtract(player.getEyePosition());
        double yaw   = Math.toDegrees(Math.atan2(-diff.x, diff.z));
        double pitch = Math.toDegrees(-Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
        player.setYRot((float) yaw);
        player.setXRot((float) pitch);
    }
}

