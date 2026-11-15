package dev.ghost.pvp.combat.sword;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.AttackEvent;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;

/**
 * W-Tap — briefly releases forward key on hit to reset sprint momentum.
 * S-Tap mode: taps backward key instead (pulls away slightly for spacing).
 */
public final class WTap extends Module {

    public enum TapMode { WTAP, STRAP }

    private final Setting<TapMode>  mode       = addEnum("Mode", "W or S tap on attack", TapMode.WTAP);
    private final Setting<Integer>  holdTicks  = addInt("Hold Ticks", "Ticks to release key", 1, 1, 5);

    private int tapTick = -1;

    public WTap() { super("W-Tap", "Sprint-reset tap on attack for knockback advantage", Category.COMBAT); }

    @Override protected void registerListeners() {
        listen(AttackEvent.class, this::onAttack);
        listen(TickEvent.class, this::onTick);
    }

    private void onAttack(AttackEvent e) {
        if (!Globals.inGame()) return;
        tapTick = 0;
        if (mode.getValue() == TapMode.WTAP) Globals.mc.options.keyUp.setDown(false);
        else Globals.mc.options.keyDown.setDown(true);
    }

    private void onTick(TickEvent e) {
        if (tapTick < 0) return;
        tapTick++;
        if (tapTick >= holdTicks.getValue()) {
            Globals.mc.options.keyUp.setDown(true);
            Globals.mc.options.keyDown.setDown(false);
            tapTick = -1;
        }
    }
}

