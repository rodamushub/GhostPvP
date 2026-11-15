package dev.ghost.pvp.combat.movement;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/**
 * WindChargeMacro — auto-uses wind charges for height gain or escape.
 *
 * BURST  — fires burst of wind charges in sequence for max height
 * ESCAPE — uses when taking damage to gain vertical distance
 * AUTO   — uses based on configurable height threshold
 */
public final class WindChargeMacro extends Module {

    public enum WindMode { BURST, ESCAPE, AUTO }

    private final Setting<WindMode> mode          = addEnum("Mode", "Wind Charge usage mode", WindMode.AUTO);
    private final Setting<Double>   heightTarget  = addDouble("Height Target", "Fire when below this Y above ground", 3.0, 0.5, 15.0);
    private final Setting<Integer>  burstCount    = addInt("Burst Count", "Number of charges per burst", 2, 1, 5);
    private final Setting<Integer>  delayMs       = addInt("Delay", "ms between burst shots", 50, 0, 200);

    private long lastUse   = 0;
    private int  shotCount = 0;

    public WindChargeMacro() { super("WindChargeMacro", "Automatic wind charge usage for movement", Category.MOVEMENT); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();
        long now   = System.currentTimeMillis();
        if (now - lastUse < delayMs.getValue()) return;

        int slot = findWindCharge(player);
        if (slot == -1) return;

        switch (mode.getValue()) {
            case AUTO -> {
                double groundDist = player.getY() - Globals.world()
                    .getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        player.blockPosition()).getY();
                if (groundDist < heightTarget.getValue()) fire(player, slot, now);
            }
            case BURST -> {
                if (shotCount < burstCount.getValue()) { fire(player, slot, now); shotCount++; }
                else shotCount = 0;
            }
            case ESCAPE -> {
                // Triggered externally via onDamage — just check a flag
                if (player.hurtTime > 0) fire(player, slot, now);
            }
        }
    }

    private void fire(net.minecraft.client.player.LocalPlayer player, int slot, long now) {
        int prev = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        Globals.mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.getInventory().setSelectedSlot(prev);
        lastUse = now;
    }

    private int findWindCharge(net.minecraft.client.player.LocalPlayer p) {
        for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).is(Items.WIND_CHARGE)) return i;
        return -1;
    }
}

