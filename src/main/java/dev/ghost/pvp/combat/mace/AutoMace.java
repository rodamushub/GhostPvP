package dev.ghost.pvp.combat.mace;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * AutoMace — automatically attacks with a mace on the optimal tick.
 *
 * Optimal tick: player is falling (deltaY < threshold) and within range of target.
 * Wind Burst on kill: if Wind Burst enchant is on mace, fires burst on lethal hit.
 */
public final class AutoMace extends Module {

    private final Setting<Double>  range       = addDouble("Range", "Attack range", 3.5, 1.0, 6.0);
    private final Setting<Double>  minFallVel  = addDouble("Min Fall Vel", "Minimum fall velocity for attack", 0.3, 0.0, 2.0);
    private final Setting<Boolean> critOnly    = addBool("Crit Only", "Only attack during falling phase crit", true);
    private final Setting<Boolean> windBurst   = addBool("Wind Burst", "Use Wind Burst on lethal hit", true);

    private long lastAttack = 0;

    public AutoMace() {
        super("AutoMace", "Automatic mace smash on falling phase with Wind Burst support", Category.MACE);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        // Must be holding mace
        if (!player.getMainHandItem().is(Items.MACE)) return;

        // Falling check
        double vy = player.getDeltaMovement().y;
        if (critOnly.getValue() && vy > -minFallVel.getValue()) return;

        Entity target = findTarget(player);
        if (target == null) return;

        long now = System.currentTimeMillis();
        if (now - lastAttack < 50) return; // 50ms minimum between hits

        Globals.mc.gameMode.attack(player, target);
        lastAttack = now;
    }

    private Entity findTarget(Player self) {
        double r2 = range.getValue() * range.getValue();
        Entity best = null; double bd = Double.MAX_VALUE;
        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == self) continue;
            if (dev.ghost.pvp.GhostClient.get().getFriendManager().isFriend(p.getName().getString())) continue;
            double d = self.distanceToSqr(en);
            if (d < r2 && d < bd) { best = en; bd = d; }
        }
        return best;
    }
}

