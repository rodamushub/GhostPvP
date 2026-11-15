package dev.ghost.pvp.combat.movement;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;

/**
 * AutoJumpReset — jumps the moment the player lands to maintain continuous
 * sprint momentum. Essential for crystal PvP positioning.
 */
public final class AutoJumpReset extends Module {

    private final Setting<Boolean> onlyMoving = addBool("Only Moving", "Only jump when holding a movement key", true);

    private boolean wasAirborne = false;

    public AutoJumpReset() { super("AutoJumpReset", "Auto-jumps on landing to maintain momentum", Category.MOVEMENT); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player  = Globals.player();
        var options = Globals.mc.options;

        boolean movingKey = options.keyUp.isDown() || options.keyDown.isDown()
                         || options.keyLeft.isDown() || options.keyRight.isDown();

        if (onlyMoving.getValue() && !movingKey) { wasAirborne = false; return; }

        boolean onGround = player.onGround();

        if (!onGround) {
            wasAirborne = true;
        } else if (wasAirborne) {
            wasAirborne = false;
            if (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly) {
                player.jumpFromGround();
            }
        }
    }
}

