package dev.ghost.pvp.combat.movement;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.MoveEvent;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;

/**
 * Speed — increases horizontal movement speed.
 *
 * LEGIT   — slight boost via sprint + jump cadence, hard to detect
 * STRAFE  — boosts strafe speed each tick by multiplier
 */
public final class Speed extends Module {

    public enum SpeedMode { LEGIT, STRAFE }

    private final Setting<SpeedMode> mode       = addEnum("Mode", "Speed method", SpeedMode.LEGIT);
    private final Setting<Double>    multiplier  = addDouble("Multiplier", "Speed multiplier", 1.3, 1.0, 3.0);

    public Speed() { super("Speed", "Increases movement speed", Category.MOVEMENT); }

    @Override protected void registerListeners() {
        listen(TickEvent.class, this::onTick);
        listen(MoveEvent.class, this::onMove);
    }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        if (mode.getValue() != SpeedMode.LEGIT) return;
        var player = Globals.player();
        // Legit: jump each time we land to maintain boost
        if (player.onGround() && player.isSprinting()) {
            player.jumpFromGround();
        }
    }

    private void onMove(MoveEvent e) {
        if (!Globals.inGame()) return;
        if (mode.getValue() != SpeedMode.STRAFE) return;
        var player = Globals.player();
        if (!player.onGround()) return;
        double m = multiplier.getValue();
        e.x *= m;
        e.z *= m;
    }
}

