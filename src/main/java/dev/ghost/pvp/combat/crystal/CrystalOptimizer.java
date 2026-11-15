package dev.ghost.pvp.combat.crystal;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/**
 * CrystalOptimizer — removes stale crystals that aren't hitting any target
 * to free up placement spots. Runs at reduced frequency.
 */
public final class CrystalOptimizer extends Module {

    private final Setting<Double>  range   = addDouble("Range", "Remove crystals outside this radius", 5.0, 1.0, 8.0);
    private final Setting<Integer> delay   = addInt("Delay", "Ticks between cleanup runs", 5, 1, 20);

    private int tick = 0;

    public CrystalOptimizer() {
        super("CrystalOptimizer", "Removes stale crystals not contributing to combat", Category.CRYSTAL);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        if (++tick < delay.getValue()) return;
        tick = 0;

        var player = Globals.player();
        var world  = Globals.world();
        double r2  = range.getValue() * range.getValue();

        // Remove crystals with no nearby enemy target contribution
        for (var entity : world.getEntities()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (player.distanceToSqr(crystal) > r2) continue;
            // Check if any enemy is in blast range of this crystal
            boolean useful = world.getEntities().stream()
                .filter(en -> en instanceof net.minecraft.world.entity.player.Player && en != player)
                .anyMatch(en -> crystal.distanceTo(en) < 7.0);
            if (!useful) {
                Globals.mc.gameMode.attack(player, crystal);
            }
        }
    }
}

