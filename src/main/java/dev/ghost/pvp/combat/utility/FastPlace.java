package dev.ghost.pvp.combat.utility;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;

/**
 * FastPlace — reduces the item use cooldown to allow placing blocks faster.
 * Applied via ClientPlayerInteractionManagerMixin (sets blockHitDelay to 0).
 * FastEXP: also covers throwing XP bottles rapidly.
 */
public final class FastPlace extends Module {

    public final Setting<Boolean> fastExp = addBool("Fast EXP", "Also apply to XP bottles", true);

    public FastPlace() { super("FastPlace", "Removes placement delay for blocks and items", Category.UTILITY); }
}

