package dev.ghost.pvp.combat.movement;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;

/** Sprint — forces sprint regardless of hunger or block/use state. */
public final class Sprint extends Module {

    public enum SprintMode { NORMAL, LEGIT, ALWAYS }

    private final Setting<SprintMode> mode = addEnum("Mode", "Sprint behaviour", SprintMode.ALWAYS);

    public Sprint() { super("Sprint", "Keeps sprint active always", Category.MOVEMENT); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        switch (mode.getValue()) {
            case ALWAYS -> player.setSprinting(true);
            case LEGIT  -> {
                if (!player.isBlocking() && !player.isUsingItem()) player.setSprinting(true);
            }
            case NORMAL -> {
                if (player.getFoodData().getFoodLevel() > 6) player.setSprinting(true);
            }
        }
    }
}

