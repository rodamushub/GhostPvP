package dev.ghost.pvp.combat.sword;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;

/**
 * Reach — extends attack range.
 * Applied via ClientPlayerInteractionManagerMixin which reads reach.getValue().
 */
public final class Reach extends Module {

    public final Setting<Double> reach;

    public Reach() {
        super("Reach", "Extends melee attack range", Category.COMBAT);
        reach = addDouble("Reach", "Attack range (vanilla = 3.0)", 3.5, 3.0, 6.0);
    }
}

