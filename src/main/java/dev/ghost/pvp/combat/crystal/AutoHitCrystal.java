package dev.ghost.pvp.combat.crystal;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/**
 * AutoHitCrystal (DoubleTap) — immediately attacks a crystal on the same tick it spawns.
 * Hooks into entity-add events via tick scan for newly spawned crystals.
 */
public final class AutoHitCrystal extends Module {

    private final Setting<Double>  range       = addDouble("Range", "Max crystal distance to hit", 5.0, 1.0, 7.0);
    private final Setting<Integer> delayTicks  = addInt("Delay Ticks", "Ticks after spawn before hit", 0, 0, 5);

    private final java.util.Map<EndCrystal, Integer> pending = new java.util.LinkedHashMap<>();

    public AutoHitCrystal() {
        super("AutoHitCrystal", "Double-tap: hits crystals immediately on spawn", Category.CRYSTAL);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();
        var world  = Globals.world();
        double r2  = range.getValue() * range.getValue();

        // Track new crystals
        for (var entity : world.getEntities()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (player.distanceToSqr(crystal) > r2) continue;
            pending.putIfAbsent(crystal, 0);
        }

        // Age and fire
        pending.entrySet().removeIf(entry -> {
            EndCrystal crystal = entry.getKey();
            if (!crystal.isAlive()) return true;
            int age = entry.getValue() + 1;
            if (age >= delayTicks.getValue()) {
                Globals.mc.gameMode.attack(player, crystal);
                return true;
            }
            entry.setValue(age);
            return false;
        });
    }
}

