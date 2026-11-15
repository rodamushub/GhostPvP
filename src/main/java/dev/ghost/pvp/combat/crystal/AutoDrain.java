package dev.ghost.pvp.combat.crystal;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * AutoDrain — targets players below a health threshold to drain their totems
 * by forcing rapid crystal detonations. Prioritizes lowest-health enemy.
 */
public final class AutoDrain extends Module {

    private final Setting<Double>  healthThreshold = addDouble("Health Threshold", "Target if HP ≤ this", 8.0, 0.0, 20.0);
    private final Setting<Double>  range           = addDouble("Range", "Target range", 8.0, 1.0, 16.0);
    private final Setting<Integer> burstDelay      = addInt("Burst Delay", "ms between drain hits", 30, 0, 200);

    private long lastHit = 0;

    public AutoDrain() {
        super("AutoDrain", "Forces totem pops on low-health targets", Category.CRYSTAL);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();
        var world  = Globals.world();

        Player target = null;
        float  lowestHp = Float.MAX_VALUE;
        double r2 = range.getValue() * range.getValue();

        for (var entity : world.getEntities()) {
            if (!(entity instanceof Player p) || p == player) continue;
            if (dev.ghost.pvp.GhostClient.get().getFriendManager().isFriend(p.getName().getString())) continue;
            if (p.getHealth() <= healthThreshold.getValue() && player.distanceToSqr(p) < r2) {
                if (p.getHealth() < lowestHp) {
                    lowestHp = p.getHealth();
                    target   = p;
                }
            }
        }

        if (target == null) return;
        long now = System.currentTimeMillis();
        if (now - lastHit < burstDelay.getValue()) return;

        // Attack all crystals near the target
        final Player finalTarget = target;
        for (var entity : world.getEntities()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (crystal.distanceTo(finalTarget) < 6.0 && player.distanceTo(crystal) < 6.0) {
                Globals.mc.gameMode.attack(player, crystal);
                lastHit = now;
                break;
            }
        }
    }
}

