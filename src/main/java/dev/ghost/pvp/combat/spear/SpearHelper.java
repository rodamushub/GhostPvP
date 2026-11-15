package dev.ghost.pvp.combat.spear;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;

/**
 * SpearHelper — miscellaneous spear QoL:
 *   - Auto-charge: holds right-click for full charge before releasing
 *   - Throw indicator: shows predicted spear arc via RenderWorldEvent (see SpearArcRenderer)
 *   - Cooldown display: shows remaining cooldown on HUD
 */
public final class SpearHelper extends Module {

    private final Setting<Boolean> autoCharge    = addBool("Auto Charge", "Holds charge before releasing", true);
    private final Setting<Integer> chargeTimeTicks = addInt("Charge Ticks", "Ticks to hold for full charge", 20, 5, 40);

    private int chargeTick  = 0;
    private boolean charging = false;

    public SpearHelper() {
        super("SpearHelper", "Spear charge automation and throw assist", Category.SPEAR);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame() || !autoCharge.getValue()) return;
        var player = Globals.player();
        var options = Globals.mc.options;

        // Detect when player starts using a spear
        boolean holdingSpear = player.getMainHandItem().getItem()
            .getDescriptionId().contains("spear");

        if (!holdingSpear) { chargeTick = 0; charging = false; return; }

        if (!charging) {
            options.keyUse.setDown(true);
            charging = true;
            chargeTick = 0;
        } else {
            chargeTick++;
            if (chargeTick >= chargeTimeTicks.getValue()) {
                options.keyUse.setDown(false);
                charging = false;
                chargeTick = 0;
            }
        }
    }
}

