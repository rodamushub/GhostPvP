package dev.ghost.pvp.combat.utility;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/**
 * AutoSwitch — switches to the optimal weapon when a target is in range.
 * Priority: Crystal > Mace (airborne) > Sword > Axe
 */
public final class AutoSwitch extends Module {

    private final Setting<Double> range      = addDouble("Range", "Target range to trigger switch", 4.0, 1.0, 8.0);
    private final Setting<Boolean> macePriority = addBool("Mace Priority", "Prefer mace when falling", true);

    public AutoSwitch() { super("AutoSwitch", "Automatically equips best weapon for the situation", Category.UTILITY); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        // Find nearest enemy
        Player target = null;
        double r2 = range.getValue() * range.getValue();
        double bd = Double.MAX_VALUE;
        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == player) continue;
            double d = player.distanceToSqr(p);
            if (d < r2 && d < bd) { target = p; bd = d; }
        }
        if (target == null) return;

        boolean falling = player.getDeltaMovement().y < -0.3;

        // Pick best slot
        int best = -1;
        int bestPriority = -1;

        for (int i = 0; i < 9; i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            int p = getWeaponPriority(stack, falling);
            if (p > bestPriority) { bestPriority = p; best = i; }
        }

        if (best != -1) player.getInventory().setSelectedSlot(best);
    }

    private int getWeaponPriority(ItemStack stack, boolean falling) {
        Item item = stack.getItem();
        if (item == Items.END_CRYSTAL) return 10;
        if (item == Items.MACE && falling) return 9;
        if (item == Items.MACE) return 7;
        if (item instanceof SwordItem) return 8;
        if (item instanceof AxeItem) return 6;
        return 0;
    }
}

