package dev.ghost.pvp.combat.movement;

import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;

/**
 * NoJumpDelay — removes the 10-tick delay between jumps.
 * Applied via mixin in JumpMixin (sets jumpDelay = 0 each tick).
 */
public final class NoJumpDelay extends Module {
    public NoJumpDelay() { super("NoJumpDelay", "Removes delay between jumps", Category.MOVEMENT); }
}

